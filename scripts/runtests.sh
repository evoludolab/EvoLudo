#!/bin/bash

EVOLUDO_TEST_HOME="EvoLudoTest" ;
EVOLUDO_TEST_TEST="$EVOLUDO_TEST_HOME/tests" ;

java -jar "$EVOLUDO_TEST_HOME"/target/EvoLudoTest.*.jar \
    	--references "$EVOLUDO_TEST_TEST/references/current" \
		--reports "$EVOLUDO_TEST_TEST/reports" \
		"$@"
