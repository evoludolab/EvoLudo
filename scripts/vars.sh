#!/bin/bash

# Shared variables and helpers for EvoLudo scripts.

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

latest_file() {
	local root=$1
	local pattern=$2
	if [ ! -d "${root}" ]; then
		return 0
	fi
	find "${root}" -type f -name "${pattern}" -print0 |
		xargs -0 -r stat -f "%m %N" |
		sort -rn | head -1 | cut -f2- -d" "
}

latest_java() {
	find . -type f -name '*.java' -print0 |
		xargs -0 -r stat -f "%m %N" |
		sort -rn | head -1 | cut -f2- -d" "
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
