#!/bin/bash

EVOLUDO_PUBLIC="EvoLudo" ;
EVOLUDO_CORE_HOME="EvoLudoCore" ;
EVOLUDO_JRE_HOME="EvoLudoJRE" ;
EVOLUDO_GWT_HOME="EvoLudoGWT" ;
EVOLUDO_DEV_HOME="EvoLudoGWTDev" ;
EVOLUDO_TEST_HOME="EvoLudoTest" ;
EVOLUDO_TEST_TEST="$EVOLUDO_TEST_HOME/tests" ;

### find most recently changed src file (skip hidden files)
LATEST_JAVA=`find ${EVOLUDO_GWT_HOME}/src/main/ ${EVOLUDO_CORE_HOME}/src/main/ \
			-type f -not -name '.*'  -not -name '*.properties' -print0 | \
			xargs -0 stat -f "%m %N" | \
			sort -rn | head -1 | cut -f2- -d" "` ;
LATEST_GWT=`find ${EVOLUDO_GWT_HOME}/target/EvoLudoGWT* \
			-type f -name 'evoludoweb.nocache.js'` ;

# build project - if needed
if [[ ${LATEST_JAVA} -nt ${LATEST_GWT} ]]; then
	echo "Building EvoLudo project..." ;
	mvn clean install ;
else
	echo "Building EvoLudo project skipped..."
fi

passed=$? ;

if [ $passed -ne 0 ]
	then 
	echo "Building EvoLudo project failed - aborting!" ;
	exit $passed ;
fi

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
# note: some say compile goal is necessary to resolve cross-module dependencies of the javadoc but seems ok now
echo "Building public API documentation..." ;
mvn -P public,-private javadoc:aggregate ;

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
