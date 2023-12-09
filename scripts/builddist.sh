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
		--tests "$EVOLUDO_TEST_TEST/generators" \
		--references "$EVOLUDO_TEST_TEST/references/current" \
		--reports "$EVOLUDO_TEST_TEST/reports"

passed=$? ;

if [ $passed -ne 0 ]
    then 
    echo "EvoLudo test\(s\) failed - aborting!" ;
    exit $passed ;
fi

# build API documentation - places javadoc in docs/api
echo "Building API documentation..." ;
mvn javadoc:aggregate ;

# create distribution directory
mkdir -p dist/war ;

# copy GWT files
echo "Copying GWT files..." ;
cp -a "$EVOLUDO_GWT_HOME"/target/EvoLudoGWT*/evoludoweb dist/war/ ;
cp -a "$EVOLUDO_DEV_HOME"/src/main/webapp/* dist/war/ ;

# copy JRE executable
echo "Copying JRE executable..." ;
cp -a "$EVOLUDO_JRE_HOME"/target/EvoLudo.*.jar dist/ ;

# copy API documentation
echo "Copying API documentation..." ;
cp -a docs/api dist/

echo "Building EvoLudo distribution done!" ;
