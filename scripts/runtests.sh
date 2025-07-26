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

EVOLUDO_PUBLIC="EvoLudo" ;
EVOLUDO_CORE_HOME="EvoLudoCore" ;
EVOLUDO_JRE_HOME="EvoLudoJRE" ;
EVOLUDO_GWT_HOME="EvoLudoGWT" ;
EVOLUDO_TEST_HOME="EvoLudoTest" ;
EVOLUDO_TEST_TEST="$EVOLUDO_TEST_HOME/tests" ;
EVOLUDO_API="dist/api" ;
EVOLUDO_WAR="dist/war" ;
EVOLUDO_SH="scripts"

### find most recently changed src file (skip hidden files)
LATEST_JAVA=`find . -type f -name '*.java' -print0 | \
			xargs -0 stat -f "%m %N" | \
			sort -rn | head -1 | cut -f2- -d" "` ;
LATEST_TEST_JAR=`find ${EVOLUDO_TEST_HOME}/target/ \
			-type f -name 'EvoLudo*.jar' -print0 | \
			xargs -0 stat -f "%m %N" | \
			sort -rn | head -1 | cut -f2- -d" "` ;

# build project - if needed
if [[ ${LATEST_JAVA} -nt ${LATEST_TEST_JAR} ]]; then
	echo "Building EvoLudo test suite..." ;
	mvn -pl EvoLudoTest -am clean install ;
else
	echo "EvoLudo test suite up to date."
fi

passed=$? ;

if [ $passed -ne 0 ]
	then 
	echo "Building EvoLudo test suite failed - aborting!" ;
	exit $passed ;
fi

java -jar "$EVOLUDO_TEST_HOME"/target/EvoLudoTest.*.jar \
    	--tests "$EVOLUDO_TEST_TEST/generators" \
    	--references "$EVOLUDO_TEST_TEST/references" \
		--reports "$EVOLUDO_TEST_TEST/reports" \
		"$@"
