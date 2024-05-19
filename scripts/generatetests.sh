#!/bin/bash

EVOLUDO_TEST_HOME="EvoLudoTest" ;
EVOLUDO_TEST_TEST="$EVOLUDO_TEST_HOME/tests" ;

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
