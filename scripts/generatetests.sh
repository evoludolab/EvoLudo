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

nreportsbefore=$((`ls | wc -l`)) ;

java -jar "$EVOLUDO_TEST_HOME"/target/EvoLudoTest.*.jar \
		--references "$EVOLUDO_TEST_TEST/references" \
		--generate "$EVOLUDO_TEST_TEST/generators" \
		--reports "$EVOLUDO_TEST_TEST/reports" \
		--compress ;

passed=$? ;

# System.exit(-1) in java translates to 255
if [ $passed -ne 0 ]
	then
    echo "EvoLudo test generation failed - aborting!" ;
	exit ;
fi

nreportsafter=$((`ls | wc -l`)) ;

if (( $nreportsbefore == $nreportsafter ))
	# all tests successfully generated
	then
		pushd "$EVOLUDO_TEST_TEST/references" > /dev/null ;
		# find newest directory - must be the one generated for the set of tests
		NEWSET=$(ls -td ./*/ | head -1) ;
		echo "All tests for set '$NEWSET' successfully generated." ;
		echo "Setting current reference to new set of tests." ;
		rm ./current ;
		ln -s "$NEWSET" current ;
		popd > /dev/null ;
	else
		echo "Some tests had issues." ;
		echo "Keeping current set of reference tests." ;
fi
