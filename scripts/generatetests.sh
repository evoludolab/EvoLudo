#!/bin/bash

EVOLUDO_TEST_HOME="EvoLudoTest" ;
EVOLUDO_TEST_TEST="$EVOLUDO_TEST_HOME/tests" ;

nreportsbefore=$((`ls | wc -l`)) ;

java -jar "$EVOLUDO_TEST_HOME"/target/EvoLudoTest.*.jar \
		--tests "$EVOLUDO_TEST_TEST/references" \
		--generate "$EVOLUDO_TEST_TEST/generators" \
		--reports "$EVOLUDO_TEST_TEST/reports" \
		--compress

nreportsafter=$((`ls | wc -l`)) ;

if (( $nreportsbefore == $nreportsafter )) 
	# all tests successfully generated
	then
		pushd "$EVOLUDO_TEST_TEST/references" ;
		# find newest directory - must be the one generated for the set of tests
		NEWSET=$(ls -td ./*/ | head -1) ;
		echo "new '$NEWSET': all tests successfully generated. setting current reference to new set of tests." ;
		rm ./current ;
		ln -s "./$NEWSET" ./current ;
		popd ;
	else
		echo "some tests had issues. keeping current set of reference tests." ;
fi
