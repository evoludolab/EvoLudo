#!/bin/bash

EVOLUDO_PUBLIC="EvoLudo" ;
EVOLUDO_CORE_HOME="EvoLudoCore" ;
EVOLUDO_JRE_HOME="EvoLudoJRE" ;
EVOLUDO_GWT_HOME="EvoLudoGWT" ;
EVOLUDO_TEST_HOME="EvoLudoTest" ;
EVOLUDO_TEST_TEST="$EVOLUDO_TEST_HOME/tests" ;
EVOLUDO_DIST="dist" ;
EVOLUDO_API="$EVOLUDO_DIST/api" ;
EVOLUDO_WAR="$EVOLUDO_DIST/war" ;
EVOLUDO_SH="scripts"

### find most recently changed src file (skip hidden files)
LATEST_JAVA=`find . -type f -name '*.java' -print0 | \
			xargs -0 stat -f "%m %N" | \
			sort -rn | head -1 | cut -f2- -d" "` ;
LATEST_GWT=`find ${EVOLUDO_GWT_HOME}/target/EvoLudoGWT* \
			-type f -name 'evoludoweb.nocache.js'` ;
LATEST_JAR=`find ${EVOLUDO_JRE_HOME}/target/ \
			-type f -name 'EvoLudo.*.jar' -print0 | \
			xargs -0 stat -f "%m %N" | \
			sort -rn | head -1 | cut -f2- -d" "` ;
LATEST_DOC=`find ${EVOLUDO_API}/ \
			-type f -name '*.html' -print0 | \
			xargs -0 stat -f "%m %N" | \
			sort -rn | head -1 | cut -f2- -d" "` ;

if [ -d ${EVOLUDO_DIST} ]
    then 
    echo "Distribution directory exists - aborting!" ;
    exit 1 ;
fi

# start by running tests
${EVOLUDO_SH}/runtests.sh

passed=$? ;

if [ $passed -ne 0 ]
    then 
    echo "EvoLudo test(s) failed - aborting!" ;
    exit $passed ;
fi

# build API documentation (if needed) - places javadoc in docs/api
# note: some say compile goal is necessary to resolve cross-module dependencies of the javadoc but seems ok now
if [[ ! -d ${EVOLUDO_API} || ( -d ${EVOLUDO_API} && ${LATEST_JAVA} -nt ${LATEST_DOC} ) ]]; then
	echo "Building public API documentation..." ;
	mvn -P public,-private javadoc:aggregate ;
else
	echo "Building public API documentation skipped..."
fi

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
cp -a "$EVOLUDO_GWT_HOME"/src/main/webapp/* dist/war/ ;

# copy JRE executable
echo "Copying EvoLudo JRE executable..." ;
cp -a "$EVOLUDO_JRE_HOME"/target/EvoLudo.*.jar dist/ ;

# copy API documentation
echo "Copying EvoLudo API documentation..." ;
cp -a target/site/apidocs dist/api

echo "Building EvoLudo distribution done!" ;
