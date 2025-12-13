#!/bin/bash

#
# EvoLudo Project
#
# Copyright 2010-2025 Christoph Hauert
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#	http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
# For publications in any form, you are kindly requested to attribute the
# author and project as follows:
#
#	Hauert, Christoph (<year>) EvoLudo Project, https://www.evoludo.org
#			(doi: 10.5281/zenodo.14591549 [, <version>])
#
#	<year>:    year of release (or download), and
#	<version>: optional version number (as reported in output header
#			or GUI console) to simplify replication of reported results.
#
# The formatting may be adjusted to comply with publisher requirements.
#

EVOLUDO_PUBLIC="EvoLudo"
EVOLUDO_CORE_HOME="EvoLudoCore"
EVOLUDO_JRE_HOME="EvoLudoJRE"
EVOLUDO_GWT_HOME="EvoLudoGWT"
EVOLUDO_TEST_HOME="EvoLudoTest"
EVOLUDO_TEST_TEST="$EVOLUDO_TEST_HOME/tests"
EVOLUDO_DIST="dist"
EVOLUDO_API="$EVOLUDO_DIST/api"
EVOLUDO_WAR="$EVOLUDO_DIST/war"
EVOLUDO_SH="scripts"

### find most recently changed src file (skip hidden files)
LATEST_JAVA=$(find . -type f -name '*.java' -print0 |
	xargs -0 stat -f "%m %N" |
	sort -rn | head -1 | cut -f2- -d" ")
LATEST_GWT=$(find ${EVOLUDO_GWT_HOME}/target/EvoLudoGWT* \
	-type f -name 'evoludoweb.nocache.js')
LATEST_JAR=$(find ${EVOLUDO_JRE_HOME}/target/ \
	-type f -name 'EvoLudo.*.jar' -print0 |
	xargs -0 stat -f "%m %N" |
	sort -rn | head -1 | cut -f2- -d" ")
LATEST_DOC=$(find ${EVOLUDO_API}/ \
	-type f -name '*.html' -print0 |
	xargs -0 stat -f "%m %N" |
	sort -rn | head -1 | cut -f2- -d" ")

if [ -d ${EVOLUDO_DIST} ]; then
	echo "Distribution directory exists - aborting!"
	exit 1
fi

read -p "Run tests? [Yes|no] " -n 1 -r
echo # (optional) move to a new line
if [[ ! $REPLY =~ ^[Nn]$ ]]; then
	${EVOLUDO_SH}/runtests.sh
	passed=$?

	if [ $passed -ne 0 ]; then
		echo "EvoLudo test(s) failed - aborting!"
		exit $passed
	fi
fi

# build API documentation (if needed) - places javadoc in docs/api
# note: some say compile goal is necessary to resolve cross-module dependencies of the javadoc but seems ok now
if [[ ! -d ${EVOLUDO_API} || (-d ${EVOLUDO_API} && ${LATEST_JAVA} -nt ${LATEST_DOC}) ]]; then
	echo "Building public API documentation..."
	mvn -P public,-private javadoc:aggregate
	passed=$?

	if [ $passed -ne 0 ]; then
		echo "Building javadoc failed - aborting!"
		exit $passed
	fi
else
	echo "Building public API documentation skipped..."
fi

# create distribution directory
mkdir -p dist/war

# building GWT app (if needed)
if [[ ${LATEST_JAVA} -nt ${LATEST_GWT} ]]; then
	echo "Building EvoLudo GWT application..."
	mvn -pl EvoLudoGWT -am install
	passed=$?
	if [ $passed -ne 0 ]; then
		echo "Building EvoLudo GWT files failed - aborting!"
		exit $passed
	fi
fi

# copy GWT files
echo "Copying EvoLudo GWT files..."
cp -a "$EVOLUDO_GWT_HOME"/target/EvoLudoGWT*/evoludoweb dist/war/
cp -a "$EVOLUDO_GWT_HOME"/src/main/webapp/* dist/war/

# building JRE executable (if needed)
if [[ ${LATEST_JAVA} -nt ${LATEST_GWT} ]]; then
	echo "Building EvoLudo JRE executable..."
	mvn -pl EvoLudoJRE -am install
	passed=$?
	if [ $passed -ne 0 ]; then
		echo "Building EvoLudo JRE files failed - aborting!"
		exit $passed
	fi
fi

# copy JRE executable
echo "Copying EvoLudo JRE executable..."
cp -a "$EVOLUDO_JRE_HOME"/target/EvoLudo.*.jar dist/

# copy API documentation
echo "Copying EvoLudo API documentation..."
cp -a target/site/apidocs dist/api

echo "Building EvoLudo distribution done!"
