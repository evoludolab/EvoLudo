#!/bin/bash

EVOLUDO_PUBLIC="EvoLudo" ;
EVOLUDO_CORE_HOME="EvoLudoCore" ;
EVOLUDO_JRE_HOME="EvoLudoJRE" ;
EVOLUDO_GWT_HOME="EvoLudoGWT" ;
EVOLUDO_DEV_HOME="EvoLudoGWTDev" ;
EVOLUDO_TEST_HOME="EvoLudoTest" ;
EVOLUDO_TEST_TEST="$EVOLUDO_TEST_HOME/tests" ;
EVOLUDO_API="dist/api" ;
EVOLUDO_WAR="dist/war" ;
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

# build project - if needed
if [[ ${LATEST_JAVA} -nt ${LATEST_GWT} || ${LATEST_JAVA} -nt ${LATEST_JAR} ]]; then
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

java -jar "$EVOLUDO_TEST_HOME"/target/EvoLudoTest.*.jar \
    	--tests "$EVOLUDO_TEST_TEST/generators" \
    	--references "$EVOLUDO_TEST_TEST/references" \
		--reports "$EVOLUDO_TEST_TEST/reports" \
		"$@"
