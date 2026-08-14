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
	echo "Run or generate EvoLudo tests. If no TEST names are given, all generator"
	echo ".clo files in EvoLudoTest/tests/generators are used."
	echo
	echo "TEST names without a path are resolved in EvoLudoTest/tests/generators,"
	echo "and the .clo extension is optional."
	echo
	echo "Options are passed to the EvoLudo test jar:"
	echo "  --exports <directory>     Exported states (requires --gui)"
	echo "  --generate                Generate reference test cases instead of"
	echo "                            running tests, replacing any existing"
	echo "                            reference with a mismatching SHA (prompts"
	echo "                            for confirmation); cannot be combined with"
	echo "                            --gui"
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
GENERATE=false
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
		--generate)
			GENERATE=true
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

if [[ "$GUI" == true && "$GENERATE" == true ]]; then
	echo "Options --gui and --generate cannot be combined - aborting!"
	exit 1
fi

# ANSI colors matching org.evoludo.simulator.exec.TestEvoLudo's ConsoleColors,
# used to keep the aggregate summary visually consistent with its per-module
# reports.
COLOR_GREEN='\033[0;32m'
COLOR_YELLOW='\033[0;33m'
COLOR_RED='\033[0;31m'
COLOR_TITLE='\033[1;30m'
COLOR_RESET='\033[0m'

# Run one 'java -jar $LATEST_TEST_JAR "$@"' test invocation, streaming its
# output live while also tallying counts into the modules_checked/total_*
# variables (initialized by the caller) for the aggregate summary printed by
# print_aggregate_summary. Each module runs in its own JVM, so its counts are
# otherwise lost once that process exits and the final report only ever
# reflects the last module checked, not the entire run. Sets $teststatus to
# the java process' exit status.
run_and_tally() {
	local tmpout content
	tmpout="$(mktemp)"
	java -jar "$LATEST_TEST_JAR" "$@" 2>&1 | tee "$tmpout" || true
	teststatus=${PIPESTATUS[0]}
	modules_checked=$((modules_checked + 1))
	content="$(<"$tmpout")"
	rm -f "$tmpout"
	# nTests is echoed as the denominator in every "X out of Y ..." summary
	# line; prefer the "successfully passed" line, but that line is skipped
	# when every single test in the module failed, so fall back to the
	# "had errors" line's denominator in that case.
	if [[ $content =~ ([0-9]+)\ out\ of\ ([0-9]+)\ tests\ successfully\ passed ]]; then
		total_passed=$((total_passed + ${BASH_REMATCH[1]}))
		total_tests=$((total_tests + ${BASH_REMATCH[2]}))
	elif [[ $content =~ ([0-9]+)\ out\ of\ ([0-9]+)\ tests\ had\ errors ]]; then
		total_tests=$((total_tests + ${BASH_REMATCH[2]}))
	fi
	if [[ $content =~ ([0-9]+)\ out\ of\ [0-9]+\ tests\ had\ warnings ]]; then
		total_warnings=$((total_warnings + ${BASH_REMATCH[1]}))
	fi
	if [[ $content =~ ([0-9]+)\ out\ of\ [0-9]+\ tests\ had\ minor ]]; then
		total_minor=$((total_minor + ${BASH_REMATCH[1]}))
	fi
	if [[ $content =~ ([0-9]+)\ out\ of\ [0-9]+\ tests\ had\ errors ]]; then
		total_failures=$((total_failures + ${BASH_REMATCH[1]}))
	fi
}

# Print the aggregate summary across all modules_checked. Only called when
# more than one module was actually checked - a single module already prints
# its own complete summary.
print_aggregate_summary() {
	echo
	if ((modules_skipped > 0)); then
		printf "${COLOR_TITLE}Overall test summary (%d modules checked, %d skipped):${COLOR_RESET}\n" \
			"$modules_checked" "$modules_skipped"
	else
		printf "${COLOR_TITLE}Overall test summary (%d modules checked):${COLOR_RESET}\n" \
			"$modules_checked"
	fi
	if ((total_warnings > 0)); then
		printf "${COLOR_YELLOW}WARNING: %d out of %d tests had warnings.${COLOR_RESET}\n" \
			"$total_warnings" "$total_tests"
	fi
	if ((total_minor > 0)); then
		printf "${COLOR_YELLOW}WARNING: %d out of %d tests had minor (likely numerical) issues.${COLOR_RESET}\n" \
			"$total_minor" "$total_tests"
	fi
	if ((total_failures > 0)); then
		printf "${COLOR_RED}ERROR: %d out of %d tests had errors.${COLOR_RESET}\n" \
			"$total_failures" "$total_tests"
	fi
	if ((total_tests != total_failures)); then
		printf "${COLOR_GREEN}%d out of %d tests successfully passed.${COLOR_RESET}\n" \
			"$total_passed" "$total_tests"
	fi
	if ((total_failures > 0 || total_minor > 0 || total_warnings > 0)); then
		printf "${COLOR_TITLE}Review issues carefully!${COLOR_RESET}\n"
	fi
}

if [[ "$GENERATE" == true ]]; then
	echo "Preparing to generate EvoLudo tests..."
else
	echo "Preparing to run EvoLudo tests..."
fi

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

if [[ "$GENERATE" == true ]]; then
	# generate reference test cases from the generator .clo files; the java
	# process itself prompts for confirmation before overwriting references
	if ((${#TEST_FILES[@]} == 0)); then
		java -jar "$LATEST_TEST_JAR" \
			--generate "$EVOLUDO_TEST_TEST/generators" \
			--references "$REFERENCES_DIR" \
			--reports "$REPORTS_DIR" \
			--compress \
			${PASSTHROUGH_ARGS[@]+"${PASSTHROUGH_ARGS[@]}"}
		exit $?
	fi

	status=0
	for testfile in "${TEST_FILES[@]}"; do
		echo "Generating EvoLudo test file '$testfile'..."
		teststatus=0
		java -jar "$LATEST_TEST_JAR" \
			--generate "$testfile" \
			--references "$REFERENCES_DIR" \
			--reports "$REPORTS_DIR" \
			--compress \
			${PASSTHROUGH_ARGS[@]+"${PASSTHROUGH_ARGS[@]}"} || teststatus=$?
		if ((teststatus != 0)); then
			status=$teststatus
		fi
	done
	exit $status
fi

status=0
modules_checked=0
modules_skipped=0
total_tests=0
total_passed=0
total_warnings=0
total_minor=0
total_failures=0

if [[ "$GUI" == false ]]; then
	# run the tests in the JRE, one module (generator file) per invocation, so
	# that each gets its own summary alongside the aggregate over all of them
	if ((${#TEST_FILES[@]} == 0)); then
		for testfile in "$EVOLUDO_TEST_TEST"/generators/*.clo; do
			TEST_FILES+=("$testfile")
		done
	fi

	for testfile in "${TEST_FILES[@]}"; do
		echo "Running EvoLudo test file '$testfile'..."
		run_and_tally \
			--tests "$testfile" \
			--references "$REFERENCES_DIR" \
			--reports "$REPORTS_DIR" \
			${PASSTHROUGH_ARGS[@]+"${PASSTHROUGH_ARGS[@]}"}
		if ((teststatus != 0)); then
			status=$teststatus
		fi
	done
	if ((modules_checked > 1)); then
		print_aggregate_summary
	fi
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

# run the tests in the browser and save the states exported by the GUI
echo "Running EvoLudo tests in the GUI on ${EVOLUDO_URL}..."
node "${SCRIPT_DIR}/guitests.js" \
	--url "$EVOLUDO_URL" \
	--exports "$EXPORTS_DIR" \
	--references "$REFERENCES_DIR" \
	${GUI_ARGS[@]+"${GUI_ARGS[@]}"} \
	"${TEST_FILES[@]}" || status=$?

# compare the exported states to the references, one module per invocation,
# tallying totals across all modules checked
for testfile in "${TEST_FILES[@]}"; do
	name="$(basename "$testfile" .clo)"
	if [ ! -d "$EXPORTS_DIR/$name" ]; then
		echo "No states exported for '$testfile' - skipped."
		status=1
		modules_skipped=$((modules_skipped + 1))
		continue
	fi
	echo "Checking EvoLudo GUI test file '$testfile'..."
	run_and_tally \
		--gui \
		--tests "$EXPORTS_DIR/$name" \
		--references "$REFERENCES_DIR" \
		--reports "$REPORTS_DIR" \
		${PASSTHROUGH_ARGS[@]+"${PASSTHROUGH_ARGS[@]}"}
	if ((teststatus != 0)); then
		status=$teststatus
	fi
done
if ((modules_checked > 1)); then
	print_aggregate_summary
fi
exit $status
