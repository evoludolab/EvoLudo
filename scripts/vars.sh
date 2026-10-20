#!/bin/bash

# Shared variables and helpers for EvoLudo scripts.

# make scripts fail fast on errors
set -euo pipefail

EVOLUDO_PUBLIC="EvoLudo"
EVOLUDO_CORE_HOME="EvoLudoCore"
EVOLUDO_JRE_HOME="EvoLudoJRE"
EVOLUDO_GWT_HOME="EvoLudoGWT"
EVOLUDO_TEST_HOME="EvoLudoTest"
EVOLUDO_TEST_TEST="${EVOLUDO_TEST_HOME}/tests"
EVOLUDO_DIST="dist"
EVOLUDO_API="${EVOLUDO_DIST}/api"
EVOLUDO_API_TARGET="target/site/apidocs"
EVOLUDO_WAR="${EVOLUDO_DIST}/war"
EVOLUDO_SH="scripts"
EVOLUDO_GITEXPORTER_JSON="config/gitexporter.json"
EVOLUDO_GITEXPORTER_FETCH_JSON="config/gitexporter.fetch.json"

if stat --version >/dev/null 2>&1; then
	STAT_MTIME_ARGS=(-c "%Y %n")
else
	STAT_MTIME_ARGS=(-f "%m %N")
fi

stat_mtime_name() {
	stat "${STAT_MTIME_ARGS[@]}" "$@"
}

latest_file() {
	local root=$1
	local pattern=$2
	if [ ! -d "${root}" ]; then
		return 0
	fi
	find "${root}" -type f -name "${pattern}" -print0 |
		while IFS= read -r -d '' file; do
			stat_mtime_name "$file"
		done |
		sort -rn | cut -f2- -d" " | sed -n '1p'
}

latest_java() {
	find . \( -name .git -o -name target -o -name dist \) -prune -o -type f -name '*.java' -print0 |
		while IFS= read -r -d '' file; do
			stat_mtime_name "$file"
		done |
		sort -rn | cut -f2- -d" " | sed -n '1p'
}

latest_gwt() {
	if [ ! -d "${EVOLUDO_GWT_HOME}/target" ]; then
		return 0
	fi
	find "${EVOLUDO_GWT_HOME}/target" -type f \
		-path "${EVOLUDO_GWT_HOME}/target/EvoLudoGWT*/evoludoweb/evoludoweb.nocache.js" -print0 |
		while IFS= read -r -d '' file; do
			stat_mtime_name "$file"
		done |
		sort -rn | cut -f2- -d" " | sed -n '1p'
}

latest_jar() {
	if [ ! -d "${EVOLUDO_JRE_HOME}/target" ]; then
		return 0
	fi
	find "${EVOLUDO_JRE_HOME}/target" -maxdepth 1 -type f -name 'EvoLudo.*.jar' -print0 |
		while IFS= read -r -d '' file; do
			stat_mtime_name "$file"
		done |
		sort -rn | cut -f2- -d" " | sed -n '1p'
}

latest_doc() {
	latest_file "${EVOLUDO_API_TARGET}" '*.html'
}

latest_test_jar() {
	latest_file "${EVOLUDO_TEST_HOME}/target" 'EvoLudoTest.*.jar'
}

format_duration() {
	local total=$1 h m s
	h=$((total / 3600))
	m=$(((total % 3600) / 60))
	s=$((total % 60))
	printf '%d:%02d:%02d' "$h" "$m" "$s"
}
# exported so runtests-worker.sh - a separate bash process xargs spawns, not
# sourced - can call it too without duplicating the definition
export -f format_duration

num_processors() {
	if command -v nproc >/dev/null 2>&1; then
		nproc
	elif command -v getconf >/dev/null 2>&1 && getconf _NPROCESSORS_ONLN >/dev/null 2>&1; then
		getconf _NPROCESSORS_ONLN
	elif command -v sysctl >/dev/null 2>&1; then
		sysctl -n hw.ncpu
	else
		echo 1
	fi
}

# 1-minute load average, e.g. '2.35' - used to size worker pools down when the
# system is already busy (measured to matter: a background game running
# inflated an otherwise-clean parallel-vs-serial comparison)
current_load1min() {
	if [[ -r /proc/loadavg ]]; then
		awk '{print $1}' /proc/loadavg
	elif command -v sysctl >/dev/null 2>&1; then
		sysctl -n vm.loadavg 2>/dev/null | awk '{print $2}'
	else
		echo 0
	fi
}

# free + inactive memory in KiB (inactive pages are reclaimable on demand, so
# counting them - not just free - avoids under-estimating headroom on a
# machine that just isn't under memory pressure). Echoes 0, meaning "unknown",
# if neither source is available, so callers can skip the memory-based cap
# rather than divide by a bogus number.
available_memory_kb() {
	if [[ -r /proc/meminfo ]]; then
		awk '/MemAvailable/ {print $2; exit}' /proc/meminfo
	elif command -v vm_stat >/dev/null 2>&1 && command -v sysctl >/dev/null 2>&1; then
		local page_size
		page_size=$(sysctl -n hw.pagesize 2>/dev/null || echo 4096)
		vm_stat | awk -v ps="$page_size" '
			/Pages free/ { free = $3 }
			/Pages inactive/ { inactive = $3 }
			END {
				gsub(/\./, "", free); gsub(/\./, "", inactive)
				printf "%d", (free + inactive) * ps / 1024
			}'
	else
		echo 0
	fi
}

LATEST_JAVA=$(latest_java)
LATEST_GWT=$(latest_gwt)
LATEST_JAR=$(latest_jar)
LATEST_DOC=$(latest_doc)
LATEST_TEST_JAR=$(latest_test_jar)
