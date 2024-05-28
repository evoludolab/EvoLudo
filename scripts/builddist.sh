#!/bin/bash

EVOLUDO_PUBLIC="EvoLudo" ;
EVOLUDO_JRE_HOME="EvoLudoJRE" ;
EVOLUDO_GWT_HOME="EvoLudoGWT" ;
EVOLUDO_DEV_HOME="EvoLudoGWTDev" ;
EVOLUDO_TEST_HOME="EvoLudoTest" ;
EVOLUDO_TEST_TEST="$EVOLUDO_TEST_HOME/tests" ;

# build project
echo "Building EvoLudo project..." ;
mvn clean install ;

# run tests
echo "Running EvoLudo tests..." ;
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
# note: the compile goal is necessary to resolve cross-module dependencies of the javadoc
echo "Building public API documentation..." ;
mvn -P public compile javadoc:aggregate ;

passed=$? ;

if [ $passed -ne 0 ]
    then 
    echo "Building javadoc failed - aborting!" ;
    exit $passed ;
fi

# create distribution directory
mkdir -p dist/war ;

# copy GWT files
echo "Copying EvoLudo GWT files..." ;
cp -a "$EVOLUDO_GWT_HOME"/target/EvoLudoGWT*/evoludoweb dist/war/ ;
cp -a "$EVOLUDO_DEV_HOME"/src/main/webapp/* dist/war/ ;

# copy JRE executable
echo "Copying EvoLudo JRE executable..." ;
cp -a "$EVOLUDO_JRE_HOME"/target/EvoLudo.*.jar dist/ ;

# copy API documentation
echo "Copying EvoLudo API documentation..." ;
cp -a target/site/apidocs dist/api

echo "Building EvoLudo distribution done!" ;
