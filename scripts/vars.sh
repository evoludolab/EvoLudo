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
	find . -type f -name '*.java' -print0 |
		while IFS= read -r -d '' file; do
			stat_mtime_name "$file"
		done |
		sort -rn | cut -f2- -d" " | sed -n '1p'
}

latest_gwt() {
	latest_file "${EVOLUDO_GWT_HOME}/target" 'evoludoweb.nocache.js'
}

latest_jar() {
	latest_file "${EVOLUDO_JRE_HOME}/target" 'EvoLudo.*.jar'
}

latest_doc() {
	latest_file "${EVOLUDO_API}" '*.html'
}

latest_test_jar() {
	latest_file "${EVOLUDO_TEST_HOME}/target" 'EvoLudoTest.*.jar'
}

LATEST_JAVA=$(latest_java)
LATEST_GWT=$(latest_gwt)
LATEST_JAR=$(latest_jar)
LATEST_DOC=$(latest_doc)
LATEST_TEST_JAR=$(latest_test_jar)
