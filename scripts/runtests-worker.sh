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

#
# Run a single EvoLudo test module and stash its result for the caller. Called
# once per module by runtests.sh's 'xargs -P' worker pool. Full output is
# captured to '<workdir>/logs/<module>.log' and a tally to
# '<workdir>/tally/<module>.tally' for runtests.sh to print/fold in original
# module order once every worker is done (interleaving-free, reproducible).
#
# What prints live is just a one-line progress trail (Starting/Elapsed/Total/
# Tasks) built from shared 'started'/'completed' counters every worker in the
# pool increments. Those counters need the 'mkdir'-based lock below (portable
# flock stand-in, unavailable on macOS): concurrent unlocked increments lose
# updates (verified - 30 racing increments collapsed to 1), not because the
# short print lines themselves would garble.
#
# Usage: runtests-worker.sh --mode <jre-run|jre-check|gui-export>
#          --jar <jar> | --node-script <js> --url <url>
#          --references <dir> [--reports <dir>] [--exports <dir>]
#          --workdir <dir> --passthrough-file <file> --testfile <file>
#

print_help() {
	echo "Usage: $0 --mode <jre-run|jre-check|gui-export> [options] --testfile <file>"
	echo
	echo "Run one EvoLudo test module, capture its output and tally, for use as an"
	echo "'xargs -P' worker invoked by runtests.sh. Not meant to be run directly."
}

MODE=
JAR=
NODE_SCRIPT=
URL=
REFERENCES_DIR=
REPORTS_DIR=
EXPORTS_DIR=
WORKDIR=
PASSTHROUGH_FILE=
TESTFILE=

while [[ $# -gt 0 ]]; do
	case "$1" in
		-h|--help) print_help; exit 0 ;;
		--mode) MODE="$2"; shift 2 ;;
		--jar) JAR="$2"; shift 2 ;;
		--node-script) NODE_SCRIPT="$2"; shift 2 ;;
		--url) URL="$2"; shift 2 ;;
		--references) REFERENCES_DIR="$2"; shift 2 ;;
		--reports) REPORTS_DIR="$2"; shift 2 ;;
		--exports) EXPORTS_DIR="$2"; shift 2 ;;
		--workdir) WORKDIR="$2"; shift 2 ;;
		--passthrough-file) PASSTHROUGH_FILE="$2"; shift 2 ;;
		--testfile) TESTFILE="$2"; shift 2 ;;
		*) echo "runtests-worker.sh: unknown option '$1' - aborting!" >&2; exit 1 ;;
	esac
done

if [[ -z "$MODE" || -z "$TESTFILE" || -z "$WORKDIR" ]]; then
	echo "runtests-worker.sh: --mode, --workdir and --testfile are required - aborting!" >&2
	exit 1
fi

name="$(basename "$TESTFILE" .clo)"
mkdir -p "$WORKDIR/logs" "$WORKDIR/tally"
logfile="$WORKDIR/logs/$name.log"
tallyfile="$WORKDIR/tally/$name.tally"

# arrays can't cross the process boundary xargs spawns this script across, so
# passthrough CLI options (e.g. '--verb', '--timeout 60') are handed over as a
# NUL-delimited file instead, mirroring the find -print0 / read -d '' idiom
# already used in vars.sh
PASSTHROUGH=()
if [[ -n "$PASSTHROUGH_FILE" && -s "$PASSTHROUGH_FILE" ]]; then
	while IFS= read -r -d '' arg; do
		PASSTHROUGH+=("$arg")
	done < "$PASSTHROUGH_FILE"
fi

wstart=$(date +%s)
start_elapsed=0
[[ -s "$WORKDIR/start_epoch" ]] && start_elapsed=$((wstart - $(<"$WORKDIR/start_epoch")))

LOCKDIR="$WORKDIR/print.lock"
until mkdir "$LOCKDIR" 2>/dev/null; do
	sleep 0.05
done
trap 'rmdir "$LOCKDIR" 2>/dev/null' EXIT
n_started=1
[[ -s "$WORKDIR/started" ]] && n_started=$(($(<"$WORKDIR/started") + 1))
echo "$n_started" >"$WORKDIR/started"
n_completed=0
[[ -s "$WORKDIR/completed" ]] && n_completed=$(<"$WORKDIR/completed")
n_total=$n_started
[[ -s "$WORKDIR/total_modules" ]] && n_total=$(<"$WORKDIR/total_modules")
echo "Starting $name.clo @ $(format_duration "$start_elapsed")"
printf 'Tasks: %d pending, %d in process, %d completed\n' \
	"$((n_total - n_started))" "$((n_started - n_completed))" "$n_completed"
rmdir "$LOCKDIR" 2>/dev/null
trap - EXIT

status=0
parse_tally=false
case "$MODE" in
	jre-run)
		java -jar "$JAR" --tests "$TESTFILE" --references "$REFERENCES_DIR" \
			--reports "$REPORTS_DIR" "${PASSTHROUGH[@]}" \
			>"$logfile" 2>&1 || status=$?
		parse_tally=true
		;;
	jre-check)
		java -jar "$JAR" --gui --tests "$EXPORTS_DIR/$name" --references "$REFERENCES_DIR" \
			--reports "$REPORTS_DIR" "${PASSTHROUGH[@]}" \
			>"$logfile" 2>&1 || status=$?
		parse_tally=true
		;;
	gui-export)
		node "$NODE_SCRIPT" --url "$URL" --exports "$EXPORTS_DIR" --references "$REFERENCES_DIR" \
			"${PASSTHROUGH[@]}" "$TESTFILE" \
			>"$logfile" 2>&1 || status=$?
		;;
	*)
		echo "runtests-worker.sh: unknown mode '$MODE' - aborting!" >&2
		exit 1
		;;
esac

{
	echo "STATUS=$status"
	if [[ "$parse_tally" == true ]]; then
		content="$(<"$logfile")"
		# nTests is echoed as the denominator in every "X out of Y ..." summary
		# line; prefer the "successfully passed" line, but that line is skipped
		# when every single test in the module failed, so fall back to the
		# "had errors" line's denominator in that case - mirrors runtests.sh's
		# (pre-parallel) run_and_tally
		if [[ $content =~ ([0-9]+)\ out\ of\ ([0-9]+)\ tests\ successfully\ passed ]]; then
			echo "PASSED=${BASH_REMATCH[1]}"
			echo "TESTS=${BASH_REMATCH[2]}"
		elif [[ $content =~ ([0-9]+)\ out\ of\ ([0-9]+)\ tests\ had\ errors ]]; then
			echo "TESTS=${BASH_REMATCH[2]}"
		fi
		if [[ $content =~ ([0-9]+)\ out\ of\ [0-9]+\ tests\ had\ warnings ]]; then
			echo "WARNINGS=${BASH_REMATCH[1]}"
		fi
		if [[ $content =~ ([0-9]+)\ out\ of\ [0-9]+\ tests\ had\ minor ]]; then
			echo "MINOR=${BASH_REMATCH[1]}"
		fi
		if [[ $content =~ ([0-9]+)\ out\ of\ [0-9]+\ tests\ had\ errors ]]; then
			echo "FAILURES=${BASH_REMATCH[1]}"
		fi
	fi
} >"$tallyfile"

wend=$(date +%s)
worker_elapsed=$((wend - wstart))
total_elapsed=$worker_elapsed
if [[ -s "$WORKDIR/start_epoch" ]]; then
	total_elapsed=$((wend - $(<"$WORKDIR/start_epoch")))
fi

# clock out: bump the shared 'completed' counter and report it, under the
# same lock (and for the same lost-update reason) as the clock-in above. The
# module's own full result is left in '$logfile' for runtests.sh to print
# later, in original module order, once every worker has finished.
LOCKDIR="$WORKDIR/print.lock"
until mkdir "$LOCKDIR" 2>/dev/null; do
	sleep 0.05
done
trap 'rmdir "$LOCKDIR" 2>/dev/null' EXIT
n_completed=1
[[ -s "$WORKDIR/completed" ]] && n_completed=$(($(<"$WORKDIR/completed") + 1))
echo "$n_completed" >"$WORKDIR/completed"
n_started=$n_completed
[[ -s "$WORKDIR/started" ]] && n_started=$(<"$WORKDIR/started")
n_total=$n_started
[[ -s "$WORKDIR/total_modules" ]] && n_total=$(<"$WORKDIR/total_modules")
printf 'Completed %s.clo @ %s   Total: %s\n' \
	"$name" "$(format_duration "$worker_elapsed")" "$(format_duration "$total_elapsed")"
printf 'Tasks: %d pending, %d in process, %d completed\n' \
	"$((n_total - n_started))" "$((n_started - n_completed))" "$n_completed"
rmdir "$LOCKDIR" 2>/dev/null
trap - EXIT

# always exit 0: a failing test module must not stop 'xargs' from dispatching
# the remaining modules; the real per-module status lives in the tally file
exit 0
