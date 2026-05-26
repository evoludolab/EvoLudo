#!/bin/bash

#
# EvoLudo Project
#
# Copyright 2010-2026 Christoph Hauert
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

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "${SCRIPT_DIR}/.."
source "${SCRIPT_DIR}/vars.sh"

MAVEN_PUBLIC_ARGS=(-P public,-private)

warn_multiple_gwt_artifacts() {
	local count=0
	local candidate
	if [ ! -d "${EVOLUDO_GWT_HOME}/target" ]; then
		return
	fi
	while IFS= read -r -d '' candidate; do
		count=$((count + 1))
	done < <(find "${EVOLUDO_GWT_HOME}/target" -type f \
		-path "${EVOLUDO_GWT_HOME}/target/EvoLudoGWT*/evoludoweb/evoludoweb.nocache.js" -print0)
	if [ "$count" -gt 1 ]; then
		echo "Warning: multiple EvoLudo GWT artifacts found; using most recent: ${LATEST_GWT}" >&2
		find "${EVOLUDO_GWT_HOME}/target" -type f \
			-path "${EVOLUDO_GWT_HOME}/target/EvoLudoGWT*/evoludoweb/evoludoweb.nocache.js" -print >&2
	fi
}

warn_multiple_jars() {
	local count=0
	local candidate
	if [ ! -d "${EVOLUDO_JRE_HOME}/target" ]; then
		return
	fi
	while IFS= read -r -d '' candidate; do
		count=$((count + 1))
	done < <(find "${EVOLUDO_JRE_HOME}/target" -maxdepth 1 -type f -name 'EvoLudo.*.jar' -print0)
	if [ "$count" -gt 1 ]; then
		echo "Warning: multiple EvoLudo JRE executables found; using most recent: ${LATEST_JAR}" >&2
		find "${EVOLUDO_JRE_HOME}/target" -maxdepth 1 -type f -name 'EvoLudo.*.jar' -print >&2
	fi
}

warn_release_state() {
	local describe
	local warning=0
	describe=$(git describe --tags --long --dirty --always 2>/dev/null || true)
	if [ -z "$describe" ]; then
		return
	fi
	if [[ "$describe" == *-dirty ]]; then
		echo "Warning: HEAD has uncommitted changes: ${describe}" >&2
		warning=1
	fi
	if [[ "$describe" =~ ^.+-([0-9]+)-g[0-9a-f]+(-dirty)?$ && "${BASH_REMATCH[1]}" -gt 0 ]]; then
		echo "Warning: HEAD is ${BASH_REMATCH[1]} commit(s) past the latest tag: ${describe}" >&2
		warning=1
	fi
	if [ "$warning" -ne 0 ]; then
		read -p "Proceed despite release-state warning(s)? [yes|No] " -n 1 -r || REPLY=""
		echo
		if [[ ! $REPLY =~ ^[Yy]$ ]]; then
			echo "Aborting builddist."
			exit 1
		fi
	fi
}

echo "Building EvoLudo distribution..."
warn_release_state

if [ -d ${EVOLUDO_DIST} ]; then
    read -p "Distribution directory exists - skip or rebuild? [Skip|rebuild] " -n 1 -r
	echo # (optional) move to a new line
	if [[ $REPLY =~ ^[Ss]$ ]]; then
		echo "Skipping distribution!"
		exit 0
	else
		echo "Re-building EvoLudo distribution..."
		rm -rf ${EVOLUDO_DIST}
	fi
fi

read -p "Run tests? [Yes|no] " -n 1 -r
echo # (optional) move to a new line
if [[ ! $REPLY =~ ^[Nn]$ ]]; then
	${EVOLUDO_SH}/runtests.sh || {
		status=$?
		echo "EvoLudo test(s) failed - aborting!"
		exit $status
	}
fi

# create distribution directory
mkdir -p dist/war

# building GWT app (if needed)
LATEST_GWT=$(latest_gwt)
if [[ -z "${LATEST_GWT}" || ! "${LATEST_GWT}" -nt "${LATEST_JAVA}" ]]; then
	echo "Building EvoLudo GWT application..."
	mvn "${MAVEN_PUBLIC_ARGS[@]}" -pl EvoLudoGWT -am clean install || {
		status=$?
		echo "Building EvoLudo GWT files failed - aborting!"
		exit $status
	}
	LATEST_GWT=$(latest_gwt)
fi

if [ -z "${LATEST_GWT}" ]; then
	echo "EvoLudo GWT files missing - aborting!"
	exit 1
fi
warn_multiple_gwt_artifacts
GWT_APP_DIR=$(dirname "$LATEST_GWT")

# copy GWT files
echo "Copying EvoLudo GWT files..."
cp -a "$GWT_APP_DIR" dist/war/
cp -a "$EVOLUDO_GWT_HOME"/src/main/webapp/* dist/war/

# building JRE executable (if needed)
LATEST_JAR=$(latest_jar)
if [[ -z "${LATEST_JAR}" || ! "${LATEST_JAR}" -nt "${LATEST_JAVA}" ]]; then
	echo "Building EvoLudo JRE executable..."
	mvn "${MAVEN_PUBLIC_ARGS[@]}" -pl EvoLudoJRE -am clean install || {
		status=$?
		echo "Building EvoLudo JRE files failed - aborting!"
		exit $status
	}
	LATEST_JAR=$(latest_jar)
fi

if [ -z "${LATEST_JAR}" ]; then
	echo "EvoLudo JRE executable missing - aborting!"
	exit 1
fi
warn_multiple_jars

# copy JRE executable
echo "Copying EvoLudo JRE executable..."
cp -a "$LATEST_JAR" dist/

# build API documentation if the cached docs are missing or stale
LATEST_DOC=$(latest_doc)
if [[ -z "${LATEST_DOC}" || ! "${LATEST_DOC}" -nt "${LATEST_JAVA}" ]]; then
	echo "Building public API documentation..."
	mvn "${MAVEN_PUBLIC_ARGS[@]}" javadoc:aggregate || {
		status=$?
		echo "Building javadoc failed - aborting!"
		exit $status
	}
	LATEST_DOC=$(latest_doc)
else
	echo "Building public API documentation skipped..."
fi

if [ -z "${LATEST_DOC}" ] || [ ! -d "${EVOLUDO_API_TARGET}" ]; then
	echo "EvoLudo API documentation missing - aborting!"
	exit 1
fi

# copy API documentation
echo "Copying EvoLudo API documentation..."
cp -a "${EVOLUDO_API_TARGET}" dist/api

echo "Building EvoLudo distribution done!"
