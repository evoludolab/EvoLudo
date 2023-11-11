#!/bin/bash

EVOLUDO_GWT_HOME="EvoLudoGWT" ;
EVOLUDO_JRE_HOME="EvoLudoJRE" ;
EVOLUDO_DEV_HOME="EvoLudoDev" ;

EVOLUDO_TEST_HOME="EvoLudoTest" ;
EVOLUDO_TEST_TEST="$EVOLUDO_TEST_HOME/tests" ;

# build project
mvn clean install ;

# run tests
java -jar "$EVOLUDO_TEST_HOME"/target/EvoLudoTest.*.jar \
		--tests "$EVOLUDO_TEST_TEST/references/current" \
		--reports "$EVOLUDO_TEST_TEST/reports"

passed=$? ;

if [ $passed -ne 0 ]
    then "EvoLudo test\(s\) failed - aborting!" ;
    exit $passed ;
fi

# build API documentation - places javadoc in docs/api
mvn javadoc:aggregate ;

# create distribution directory
mkdir -p dist/war ;

# copy GWT files
cp -a "$EVOLUDO_GWT_HOME"/target/EvoLudoGWT*/evoludoweb dist/war/ ;
cp -a "$EVOLUDO_DEV_HOME"/src/main/webapp/* dist/war/ ;

# copy JRE executable
cp -a "$EVOLUDO_JRE_HOME"/target/EvoLudo.*.jar dist/ ;

# copy API documentation
cp -a docs/api dist/
