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

print_help() {
	echo "Usage: $0 [options] [TEST[.clo] ...]"
	echo
	echo "Run EvoLudo tests. If no TEST names are given, all generator .clo"
	echo "files in EvoLudoTest/tests/generators are run."
	echo
	echo "TEST names without a path are resolved in EvoLudoTest/tests/generators,"
	echo "and the .clo extension is optional."
	echo
	echo "Options are passed to the EvoLudo test jar:"
	echo "  --references <directory>  Directory for reference test cases"
	echo "  --reports <directory>     Directory for failed test reports"
	echo "  --minor                   Dump differences for minor failures"
	echo "  --verb                    Verbose mode"
	echo "  --nosha                   Disable SHA checks"
	echo "  -h, --help                Show this help screen"
}

for arg in "$@"; do
	case "$arg" in
		-h|--help)
			print_help
			exit 0
			;;
	esac
done

source "${SCRIPT_DIR}/vars.sh"

echo "Preparing to run EvoLudo tests..."

# build project - if needed
if [[ ${LATEST_JAVA} -nt ${LATEST_TEST_JAR} ]]; then
	echo "Building EvoLudo test suite..."
	mvn -pl EvoLudoTest -am clean install || {
		status=$?
		echo "Building EvoLudo test suite failed - aborting!"
		exit $status
	}
	LATEST_TEST_JAR=$(latest_test_jar)
else
	echo "EvoLudo test suite up to date."
fi

if [ -z "${LATEST_TEST_JAR}" ]; then
	echo "EvoLudoTest jar missing - aborting!"
	exit 1
fi

TEST_FILES=()
PASSTHROUGH_ARGS=()
EXPECT_VALUE=false
LAST_OPTION=
for arg in "$@"; do
	if [[ "$EXPECT_VALUE" == true ]]; then
		PASSTHROUGH_ARGS+=("$arg")
		EXPECT_VALUE=false
		LAST_OPTION=
		continue
	fi
	case "$arg" in
		--generate|--ref|--references|--report|--reports|--test|--tests)
			PASSTHROUGH_ARGS+=("$arg")
			EXPECT_VALUE=true
			LAST_OPTION="$arg"
			;;
		-*)
			PASSTHROUGH_ARGS+=("$arg")
			;;
		*)
			testfile="$arg"
			if [[ "$testfile" != *.clo ]]; then
				testfile="${testfile}.clo"
			fi
			if [[ "$testfile" != /* && "$testfile" != */* ]]; then
				testfile="$EVOLUDO_TEST_TEST/generators/$testfile"
			fi
			if [ ! -f "$testfile" ]; then
				echo "EvoLudo test file '$testfile' not found - aborting!"
				exit 1
			fi
			TEST_FILES+=("$testfile")
			;;
	esac
done

if [[ "$EXPECT_VALUE" == true ]]; then
	echo "Missing value for option '$LAST_OPTION' - aborting!"
	exit 1
fi

if ((${#TEST_FILES[@]} == 0)); then
	if ((${#PASSTHROUGH_ARGS[@]} == 0)); then
		java -jar "$LATEST_TEST_JAR" \
			--tests "$EVOLUDO_TEST_TEST/generators" \
			--references "$EVOLUDO_TEST_TEST/references" \
			--reports "$EVOLUDO_TEST_TEST/reports"
	else
		java -jar "$LATEST_TEST_JAR" \
			--tests "$EVOLUDO_TEST_TEST/generators" \
			--references "$EVOLUDO_TEST_TEST/references" \
			--reports "$EVOLUDO_TEST_TEST/reports" \
			"${PASSTHROUGH_ARGS[@]}"
	fi
	exit $?
fi

status=0
for testfile in "${TEST_FILES[@]}"; do
	echo "Running EvoLudo test file '$testfile'..."
	teststatus=0
	if ((${#PASSTHROUGH_ARGS[@]} == 0)); then
		java -jar "$LATEST_TEST_JAR" \
			--tests "$testfile" \
			--references "$EVOLUDO_TEST_TEST/references" \
			--reports "$EVOLUDO_TEST_TEST/reports" || teststatus=$?
	else
		java -jar "$LATEST_TEST_JAR" \
			--tests "$testfile" \
			--references "$EVOLUDO_TEST_TEST/references" \
			--reports "$EVOLUDO_TEST_TEST/reports" \
			"${PASSTHROUGH_ARGS[@]}" || teststatus=$?
	fi
	if ((teststatus != 0)); then
		status=$teststatus
	fi
done
exit $status
