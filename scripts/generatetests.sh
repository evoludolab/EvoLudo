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

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "${SCRIPT_DIR}/vars.sh"

# make scripts fail fast on errors
set -euo pipefail
echo "Preparing to generate EvoLudo tests..."

# build project - if needed
if [[ ${LATEST_JAVA} -nt ${LATEST_TEST_JAR} ]]; then
	echo "Building EvoLudo test suite..."
	mvn -pl EvoLudoTest -am clean install
	LATEST_TEST_JAR=$(latest_test_jar)
else
	echo "EvoLudo test suite up to date."
fi

passed=$?

if [ $passed -ne 0 ]; then
	echo "Building EvoLudo test suite failed - aborting!"
	exit $passed
fi

nreportsbefore=$(($(ls "$EVOLUDO_TEST_TEST/reports" | wc -l)))

if [ -z "${LATEST_TEST_JAR}" ]; then
	echo "EvoLudoTest jar missing - aborting!"
	exit 1
fi

java -jar "$LATEST_TEST_JAR" \
	--references "$EVOLUDO_TEST_TEST/references" \
	--generate "$EVOLUDO_TEST_TEST/generators" \
	--reports "$EVOLUDO_TEST_TEST/reports" \
	--compress \
	"$@"
passed=$?

# System.exit(-1) in java translates to 255
if [ $passed -ne 0 ]; then
	echo "EvoLudo test generation failed - aborting!"
	exit $passed
fi

nreportsafter=$(($(ls "$EVOLUDO_TEST_TEST/reports" | wc -l)))

if (($nreportsbefore == $nreportsafter)); then # all tests successfully generated
	pushd "$EVOLUDO_TEST_TEST/references" >/dev/null
	# find newest directory - must be the one generated for the set of tests
	NEWSET=$(ls -td ./*/ | head -1)
	echo "All tests for set '$NEWSET' successfully generated."
	popd >/dev/null
else
	echo "Some tests had issues."
	echo "Keeping current set of reference tests."
fi
