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
	echo "  --exports <directory>     Exported states (requires --gui)"
	echo "  -h, --help                Show this help screen"
	echo "  --headful, --headed       Show the browser window (requires --gui)"
	echo "  --gui                     Run test through GUI using playwright"
	echo "                            (requires development server, see"
	echo "                            'mvn gwt:devmode')"
	echo "  --minor                   Dump differences for minor failures"
	echo "  --nosha                   Disable SHA checks"
	echo "  --snap <seconds>          Timeout for laying out networks (default 20)"
	echo "  --references <directory>  Reference test directory"
	echo "  --reports <directory>     Failed test reports directory"
	echo "  --timeout <seconds>       Timeout per test case (default 300)"
	echo "  --verb                    Verbose mode"
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

REFERENCES_DIR="$EVOLUDO_TEST_TEST/references"
REPORTS_DIR="$EVOLUDO_TEST_TEST/reports"
EXPORTS_DIR="$EVOLUDO_TEST_TEST/exports"
GUI=false
TEST_FILES=()
PASSTHROUGH_ARGS=()
GUI_ARGS=()
EXPECT_VALUE=false
LAST_OPTION=
for arg in "$@"; do
	if [[ "$EXPECT_VALUE" == true ]]; then
		case "$LAST_OPTION" in
			--ref|--references) REFERENCES_DIR="$arg" ;;
			--report|--reports) REPORTS_DIR="$arg" ;;
			--export|--exports) EXPORTS_DIR="$arg" ;;
			*) GUI_ARGS+=("$LAST_OPTION" "$arg") ;;
		esac
		EXPECT_VALUE=false
		LAST_OPTION=
		continue
	fi
	case "$arg" in
		--ref|--references|--report|--reports|--export|--exports|--timeout|--snap)
			EXPECT_VALUE=true
			LAST_OPTION="$arg"
			;;
		--gui)
			GUI=true
			;;
		--headed|--headful)
			GUI_ARGS+=("--headed")
			;;
		--verb)
			GUI_ARGS+=("$arg")
			PASSTHROUGH_ARGS+=("$arg")
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

echo "Preparing to run EvoLudo tests..."

# build project - if needed. note in GUI mode 'clean' would kill the
# development server and is deliberately omitted.
if [[ ${LATEST_JAVA} -nt ${LATEST_TEST_JAR} ]]; then
	echo "Building EvoLudo test suite..."
	if [[ "$GUI" == true ]]; then
		MAVEN_GOALS=(install)
	else
		MAVEN_GOALS=(clean install)
	fi
	mvn -pl EvoLudoTest -am "${MAVEN_GOALS[@]}" || {
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

if [[ "$GUI" == false ]]; then
	# run the tests in the JRE
	if ((${#TEST_FILES[@]} == 0)); then
		java -jar "$LATEST_TEST_JAR" \
			--tests "$EVOLUDO_TEST_TEST/generators" \
			--references "$REFERENCES_DIR" \
			--reports "$REPORTS_DIR" \
			${PASSTHROUGH_ARGS[@]+"${PASSTHROUGH_ARGS[@]}"}
		exit $?
	fi

	status=0
	for testfile in "${TEST_FILES[@]}"; do
		echo "Running EvoLudo test file '$testfile'..."
		teststatus=0
		java -jar "$LATEST_TEST_JAR" \
			--tests "$testfile" \
			--references "$REFERENCES_DIR" \
			--reports "$REPORTS_DIR" \
			${PASSTHROUGH_ARGS[@]+"${PASSTHROUGH_ARGS[@]}"} || teststatus=$?
		if ((teststatus != 0)); then
			status=$teststatus
		fi
	done
	exit $status
fi

#
# run the tests in the GUI: the option sets are fed to the EvoLudo lab in a
# browser and the states exported by the GUI are compared to the very same
# references. JRE and GWT must return the same results.
#

# testing the GUI only makes sense with a running development server
port_open() {
	(exec 3<>"/dev/tcp/localhost/$1") 2>/dev/null
}

JETTY_PORT=$(sed -n 's:.*<evoludo\.jettyPort>\([0-9]*\)</evoludo\.jettyPort>.*:\1:p' pom.xml | head -1)
if [ -z "${JETTY_PORT}" ]; then
	echo "Failed to determine the jetty port from pom.xml - aborting!"
	exit 1
fi
EVOLUDO_URL="http://localhost:${JETTY_PORT}"
if ! port_open "${JETTY_PORT}" || ! port_open 9876; then
	echo "No EvoLudo development server on ${EVOLUDO_URL} (code server on 9876)."
	echo "Start it with 'mvn gwt:devmode' and try again - aborting!"
	exit 1
fi

if ! command -v node >/dev/null 2>&1; then
	echo "node not found - aborting!"
	exit 1
fi
NODE_PATH="$(npm root -g)"
export NODE_PATH
if ! node -e "require('playwright')" >/dev/null 2>&1; then
	echo "playwright not found - install it with 'npm install -g playwright' and"
	echo "'npx playwright install chromium' - aborting!"
	exit 1
fi

if ((${#TEST_FILES[@]} == 0)); then
	for testfile in "$EVOLUDO_TEST_TEST"/generators/*.clo; do
		TEST_FILES+=("$testfile")
	done
fi

status=0

# run the tests in the browser and save the states exported by the GUI
echo "Running EvoLudo tests in the GUI on ${EVOLUDO_URL}..."
node "${SCRIPT_DIR}/guitests.js" \
	--url "$EVOLUDO_URL" \
	--exports "$EXPORTS_DIR" \
	--references "$REFERENCES_DIR" \
	${GUI_ARGS[@]+"${GUI_ARGS[@]}"} \
	"${TEST_FILES[@]}" || status=$?

# compare the exported states to the references
for testfile in "${TEST_FILES[@]}"; do
	name="$(basename "$testfile" .clo)"
	if [ ! -d "$EXPORTS_DIR/$name" ]; then
		echo "No states exported for '$testfile' - skipped."
		status=1
		continue
	fi
	echo "Checking EvoLudo GUI test file '$testfile'..."
	teststatus=0
	java -jar "$LATEST_TEST_JAR" \
		--gui \
		--tests "$EXPORTS_DIR/$name" \
		--references "$REFERENCES_DIR" \
		--reports "$REPORTS_DIR" \
		${PASSTHROUGH_ARGS[@]+"${PASSTHROUGH_ARGS[@]}"} || teststatus=$?
	if ((teststatus != 0)); then
		status=$teststatus
	fi
done
exit $status
