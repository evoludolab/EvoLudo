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
	echo "  --generate                Generate reference tests or update failing"
	echo "                            ones after confirmation (conflicts with --gui)"
	echo "  -h, --help                Show this help screen"
	echo "  --headful, --headed [<v>] Show the browser window (implies --gui);"
	echo "                            <v> optional view index; defaults to first"
	echo "  --gui [<v>]               Run test through GUI using playwright"
	echo "                            (requires server, see 'mvn gwt:devmode')"
	echo "                            <v> optional view index; defaults to console"
	echo "  --jobs <n>                Run <n> module tests in parallel (default:"
	echo "                            auto-sized from processor count, system"
	echo "                            load and free memory; --headed defaults to 1)"
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
HEADED=false
GUI_VIEW=
USER_JOBS=
TEST_FILES=()
PASSTHROUGH_ARGS=()
GUI_ARGS=()
while [[ $# -gt 0 ]]; do
	arg="$1"
	case "$arg" in
		--ref|--references|--report|--reports|--export|--exports|--timeout|--snap|--jobs)
			if [[ $# -lt 2 ]]; then
				echo "Missing value for option '$arg' - aborting!"
				exit 1
			fi
			case "$arg" in
				--ref|--references) REFERENCES_DIR="$2" ;;
				--report|--reports) REPORTS_DIR="$2" ;;
				--export|--exports) EXPORTS_DIR="$2" ;;
				--timeout|--snap) GUI_ARGS+=("$arg" "$2") ;;
				--jobs) USER_JOBS="$2" ;;
			esac
			shift 2
			;;
		--gui)
			GUI=true
			# optional numeric view index, e.g. '--gui 1' - see GUI_VIEW below
			if [[ $# -ge 2 && "$2" =~ ^[0-9]+$ ]]; then
				GUI_VIEW="$2"
				shift
			fi
			shift
			;;
		--generate)
			GENERATE=true
			shift
			;;
		--headed|--headful)
			GUI=true
			GUI_ARGS+=("--headed")
			HEADED=true
			# optional numeric view index, same as for '--gui' above
			if [[ $# -ge 2 && "$2" =~ ^[0-9]+$ ]]; then
				GUI_VIEW="$2"
				shift
			fi
			shift
			;;
		--verb)
			GUI_ARGS+=("$arg")
			PASSTHROUGH_ARGS+=("$arg")
			shift
			;;
		-*)
			PASSTHROUGH_ARGS+=("$arg")
			shift
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
			shift
			;;
	esac
done

# select the initial view for every test in the browser, see EvoLudoWeb's
# --view option: an explicit index (from '--gui <v>' or '--headful <v>')
# always wins; otherwise headless runs default to the console view - it
# requires no layouting of networks and so is by far the fastest, whereas
# headful runs default to the module's own default (first) view, since a
# visible browser is presumably meant to be looked at. Console is always the
# last entry in the view list regardless of module, so an index well beyond
# any module's view count reliably clamps to it.
if [[ -n "$GUI_VIEW" ]]; then
	GUI_ARGS+=("--view" "$GUI_VIEW")
elif [[ "$HEADED" == false ]]; then
	GUI_ARGS+=("--view" "999")
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
	local tmpout content wstart wend name
	wstart=$(date +%s)
	# $testfile isn't a parameter - both call sites happen to loop over a
	# variable of that name, and bash's dynamic scoping picks it up here
	name="$(basename "${testfile:-unknown}" .clo)"
	tmpout="$(mktemp)"
	java -jar "$LATEST_TEST_JAR" "$@" 2>&1 | tee "$tmpout" || true
	teststatus=${PIPESTATUS[0]}
	wend=$(date +%s)
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
	printf 'Completed %s.clo @ %s   Total: %s\n' \
		"$name" "$(format_duration $((wend - wstart)))" "$(format_duration $((wend - RUN_START)))"
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

# scratch space for the parallel workers' logs/tallies (not reports - those go
# straight into REPORTS_DIR, see the note by its 'mkdir -p' below); removed on
# exit, whether clean, failing, or interrupted
WORKDIR="$(mktemp -d)"
trap 'rm -rf "$WORKDIR"' EXIT

# wall-clock start of the actual test run (build time excluded); stashed in a
# file so the worker processes runtests-worker.sh spawns - each a separate
# bash invocation with its own $SECONDS - can compute the same running total
RUN_START=$(date +%s)
echo "$RUN_START" >"$WORKDIR/start_epoch"

print_total_elapsed() {
	printf "${COLOR_TITLE}Total elapsed: %s${COLOR_RESET}\n" \
		"$(format_duration $(($(date +%s) - RUN_START)))"
}

# Reset the shared started/completed/total_modules counters a parallel batch's
# workers use (via runtests-worker.sh) to report "Tasks: N pending, N in
# process, N completed" whenever one of them clocks in or out, and print the
# beginning-of-batch state (everything still pending) right away.
init_task_counters() {
	local count=$1
	echo 0 >"$WORKDIR/started"
	echo 0 >"$WORKDIR/completed"
	echo "$count" >"$WORKDIR/total_modules"
	printf 'Tasks: %d pending, 0 in process, 0 completed\n' "$count"
}

# every worker JVM/node process, run in parallel or not, is handed the same
# REPORTS_DIR/REFERENCES_DIR; TestEvoLudo no longer creates or deletes the
# reports directory itself (it used to, but that raced when several JVMs
# shared one reports directory - see TestEvoLudo#run() and #parse()), so
# creating it once up front, before any worker starts, is now runtests.sh's
# job
mkdir -p "$REPORTS_DIR"

# stash the passthrough option arrays as NUL-delimited files once, since they
# can't cross the process boundary 'xargs' spawns runtests-worker.sh across -
# see runtests-worker.sh for the read-back side of this
printf '%s\0' ${PASSTHROUGH_ARGS[@]+"${PASSTHROUGH_ARGS[@]}"} >"$WORKDIR/passthrough-java.args"
printf '%s\0' ${GUI_ARGS[@]+"${GUI_ARGS[@]}"} >"$WORKDIR/passthrough-gui.args"

# Measured per-worker memory footprints (peak RSS, rounded up for headroom):
# ~1.3GB for a JRE test JVM and ~750MB for playwright (chrome-headless-shell,
# gpu-process, network utility, and renderer). check against available memory
# to avoid swapping.
JRE_JOB_MEM_MB=1536
GUI_JOB_MEM_MB=1024

# Determine how many modules to run in parallel: an explicit '--jobs' always
# wins and is trusted as-is (only clamped to the number of modules); otherwise
# start from one less than the number of available processors, then back off
# further for whichever of these three is tightest:
#   - load:   an already-busy system has fewer idle cores to hand a new
#             worker (measured to matter: a background game running
#             materially changed parallel-vs-serial timings)
#   - memory: 'mem_per_job_mb' workers' worth must fit in free+inactive
#             memory, or the OS falls back to compressing/decompressing and
#             swapping pages - real kernel-mode work that shows up as system
#             time, confirmed via vm_stat sampling and page-reclaim counts
#             during a 10-way parallel run vs. a serial one
#   - hard_cap: an optional caller-imposed ceiling (e.g. a headed --gui run,
#             which stays at 1 so there's exactly one window to watch)
# ...and never more workers than there are modules to run.
effective_jobs() {
	local count=$1 mem_per_job_mb=${2:-0} hard_cap=${3:-0}
	local jobs cpu load1min load_jobs avail_kb mem_jobs
	if [[ -n "$USER_JOBS" ]]; then
		jobs=$USER_JOBS
	else
		cpu=$(num_processors)
		jobs=$cpu
		((jobs > 1)) && jobs=$((jobs - 1))

		load1min=$(current_load1min)
		load_jobs=$(awk -v c="$cpu" -v l="$load1min" \
			'BEGIN { j = c - l; if (j < 1) j = 1; printf "%d", j }')
		((load_jobs < jobs)) && jobs=$load_jobs

		if ((mem_per_job_mb > 0)); then
			avail_kb=$(available_memory_kb)
			if ((avail_kb > 0)); then
				mem_jobs=$((avail_kb / (mem_per_job_mb * 1024)))
				((mem_jobs < 1)) && mem_jobs=1
				((mem_jobs < jobs)) && jobs=$mem_jobs
			fi
		fi

		((hard_cap > 0 && jobs > hard_cap)) && jobs=$hard_cap
	fi
	((jobs > count)) && jobs=$count
	((jobs < 1)) && jobs=1
	echo "$jobs"
}

# Run 'modules' (generator .clo files) in parallel through runtests-worker.sh,
# one 'jre-run' or 'jre-check' worker per module, then fold each module's
# results into modules_checked/total_* (initialized by the caller) exactly as
# run_and_tally does for a single module, so print_aggregate_summary works
# unchanged. Each worker prints its own progress trail (Starting/Elapsed/
# Total/Tasks lines) live as it clocks in and out - see runtests-worker.sh -
# but its full result is only printed here, in original module order, once
# every worker has finished, so full per-module output is never interleaved
# and runs stay reproducible/diffable.
run_parallel_and_tally() {
	local mode=$1 jobs=$2
	shift 2
	local modules=("$@")
	init_task_counters "${#modules[@]}"
	printf '%s\0' "${modules[@]}" | xargs -0 -P "$jobs" -I{} "$SCRIPT_DIR/runtests-worker.sh" \
		--mode "$mode" --jar "$LATEST_TEST_JAR" --references "$REFERENCES_DIR" \
		--reports "$REPORTS_DIR" --exports "$EXPORTS_DIR" --workdir "$WORKDIR" \
		--passthrough-file "$WORKDIR/passthrough-java.args" --testfile {}
	local testfile name logfile tallyfile key value
	local teststatus tests passed warnings minor failures
	for testfile in "${modules[@]}"; do
		name="$(basename "$testfile" .clo)"
		echo "Running EvoLudo test file '$testfile'..."
		logfile="$WORKDIR/logs/$name.log"
		[[ -f "$logfile" ]] && cat "$logfile"
		tallyfile="$WORKDIR/tally/$name.tally"
		teststatus=1
		tests=0
		passed=0
		warnings=0
		minor=0
		failures=0
		if [[ -f "$tallyfile" ]]; then
			while IFS='=' read -r key value; do
				case "$key" in
					STATUS) teststatus=$value ;;
					TESTS) tests=$value ;;
					PASSED) passed=$value ;;
					WARNINGS) warnings=$value ;;
					MINOR) minor=$value ;;
					FAILURES) failures=$value ;;
				esac
			done <"$tallyfile"
		else
			echo "No result reported for '$testfile' - treating as failed."
		fi
		modules_checked=$((modules_checked + 1))
		total_tests=$((total_tests + tests))
		total_passed=$((total_passed + passed))
		total_warnings=$((total_warnings + warnings))
		total_minor=$((total_minor + minor))
		total_failures=$((total_failures + failures))
		if ((teststatus != 0)); then
			status=$teststatus
		fi
	done
}

# Run 'modules' in parallel through runtests-worker.sh's 'gui-export' mode,
# one playwright instance per module. Unlike run_parallel_and_tally, exports
# aren't tallied against modules_checked/total_* here - only the subsequent
# GUI-check pass (run through run_parallel_and_tally like the JRE tests) is,
# matching today's sequential GUI flow where the export pass only ever sets
# $status. Each worker prints its own progress trail (Starting/Elapsed/Total/
# Tasks lines) live as it clocks in and out - see runtests-worker.sh - but its
# export log is only printed here, in original module order, once every
# worker has finished, so full per-module output is never interleaved.
run_parallel_export() {
	local jobs=$1
	shift
	local modules=("$@")
	init_task_counters "${#modules[@]}"
	printf '%s\0' "${modules[@]}" | xargs -0 -P "$jobs" -I{} "$SCRIPT_DIR/runtests-worker.sh" \
		--mode gui-export --node-script "$SCRIPT_DIR/guitests.js" --url "$EVOLUDO_URL" \
		--references "$REFERENCES_DIR" --exports "$EXPORTS_DIR" --workdir "$WORKDIR" \
		--passthrough-file "$WORKDIR/passthrough-gui.args" --testfile {}
	local testfile name logfile tallyfile key value teststatus
	for testfile in "${modules[@]}"; do
		name="$(basename "$testfile" .clo)"
		logfile="$WORKDIR/logs/$name.log"
		[[ -f "$logfile" ]] && cat "$logfile"
		tallyfile="$WORKDIR/tally/$name.tally"
		teststatus=1
		if [[ -f "$tallyfile" ]]; then
			while IFS='=' read -r key value; do
				[[ "$key" == STATUS ]] && teststatus=$value
			done <"$tallyfile"
		fi
		if ((teststatus != 0)); then
			status=$teststatus
		fi
	done
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

	if ((${#TEST_FILES[@]} > 1)); then
		JOBS=$(effective_jobs ${#TEST_FILES[@]} "$JRE_JOB_MEM_MB")
	else
		JOBS=1
	fi
	if ((JOBS > 1)); then
		echo "Running ${#TEST_FILES[@]} EvoLudo test files using $JOBS parallel workers..."
		run_parallel_and_tally jre-run "$JOBS" "${TEST_FILES[@]}"
	else
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
	fi
	if ((modules_checked > 1)); then
		print_aggregate_summary
	fi
	print_total_elapsed
	exit $status
fi

#
# run the tests in the GUI: the option sets are fed to the EvoLudo lab in a
# browser and the states exported by the GUI are compared to the very same
# references. JRE and GWT must return the same results.
#

# testing the GUI only makes sense with a running development server; prefer
# 'nc', which resolves 'localhost' the same way curl/browsers do (trying both
# IPv4 and IPv6) - bash's own '/dev/tcp' can fail to find a port that's only
# listening on the IPv6 loopback address, which is how the jetty dev server
# binds by default on some systems
port_open() {
	if command -v nc >/dev/null 2>&1; then
		nc -z -w 2 localhost "$1" 2>/dev/null
	else
		(exec 3<>"/dev/tcp/localhost/$1") 2>/dev/null
	fi
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

# run the tests in the browser and save the states exported by the GUI; each
# worker is a full headless browser instance, sized down the same way the JRE
# workers are (load + memory, see effective_jobs), and a headed run - meant
# to be watched - is hard-capped to a single browser unless '--jobs' overrides
echo "Running EvoLudo tests in the GUI on ${EVOLUDO_URL}..."
if ((${#TEST_FILES[@]} > 1)); then
	GUI_JOBS_CAP=0
	[[ "$HEADED" == true ]] && GUI_JOBS_CAP=1
	JOBS=$(effective_jobs ${#TEST_FILES[@]} "$GUI_JOB_MEM_MB" "$GUI_JOBS_CAP")
else
	JOBS=1
fi
if ((JOBS > 1)); then
	echo "Using $JOBS parallel playwright workers..."
	run_parallel_export "$JOBS" "${TEST_FILES[@]}"
else
	wstart=$(date +%s)
	node "${SCRIPT_DIR}/guitests.js" \
		--url "$EVOLUDO_URL" \
		--exports "$EXPORTS_DIR" \
		--references "$REFERENCES_DIR" \
		${GUI_ARGS[@]+"${GUI_ARGS[@]}"} \
		"${TEST_FILES[@]}" || status=$?
	wend=$(date +%s)
	if ((${#TEST_FILES[@]} == 1)); then
		label="$(basename "${TEST_FILES[0]}" .clo).clo"
	else
		label="${#TEST_FILES[@]} files"
	fi
	printf 'Completed %s @ %s   Total: %s\n' "$label" \
		"$(format_duration $((wend - wstart)))" "$(format_duration $((wend - RUN_START)))"
fi

# compare the exported states to the references, one module per invocation,
# tallying totals across all modules checked
CHECK_FILES=()
for testfile in "${TEST_FILES[@]}"; do
	name="$(basename "$testfile" .clo)"
	if [ ! -d "$EXPORTS_DIR/$name" ]; then
		echo "No states exported for '$testfile' - skipped."
		status=1
		modules_skipped=$((modules_skipped + 1))
		continue
	fi
	CHECK_FILES+=("$testfile")
done

if ((${#CHECK_FILES[@]} > 1)); then
	JOBS=$(effective_jobs ${#CHECK_FILES[@]} "$JRE_JOB_MEM_MB")
else
	JOBS=1
fi
if ((JOBS > 1)); then
	echo "Checking ${#CHECK_FILES[@]} EvoLudo GUI test files using $JOBS parallel workers..."
	run_parallel_and_tally jre-check "$JOBS" "${CHECK_FILES[@]}"
elif ((${#CHECK_FILES[@]} == 1)); then
	testfile="${CHECK_FILES[0]}"
	name="$(basename "$testfile" .clo)"
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
fi
if ((modules_checked > 1)); then
	print_aggregate_summary
fi
exit $status
