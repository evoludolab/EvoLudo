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

LATEST_JAVA=$(latest_java)
LATEST_GWT=$(latest_gwt)
LATEST_JAR=$(latest_jar)
LATEST_DOC=$(latest_doc)
LATEST_TEST_JAR=$(latest_test_jar)
