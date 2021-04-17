# path to relevant executables
JAVADOC = $(shell which javadoc)

# top level directories of EvoLudo
EVOLUDO_HOME = $(CURDIR)
EVOLUDO_SRC = $(CURDIR)/src
EVOLUDO_BUILD = $(CURDIR)/build
# at least for now, place docs in build directory. may graduate to a top level
# location once EvoLudo is public and if documentation is served from github.
#EVOLUDO_DOC = $(CURDIR)/docs
EVOLUDO_DOC = $(EVOLUDO_BUILD)/docs

.PHONY: all test clean

# targets
all:

# note: to include all errors/warnings add: -Xmaxerrs 0 -Xmaxwarns 0

docs :
	mkdir -p $(EVOLUDO_DOC) ;
	javadoc -d $(EVOLUDO_DOC) -sourcepath $(EVOLUDO_SRC) \
		-classpath ../Google/releases/gwt-2.8.2.XHTML/gwt-user.jar:./lib/parallax-1.6.jar:./lib/mtj-0.9.14.jar:./lib/freehep-graphicsio-svg-2.4.jar:./lib/freehep-graphics2d-2.4.jar:./lib/freehep-graphicsio-ps-2.4.jar:./lib/freehep-graphicsio-pdf-2.4.jar \
		-Xmaxerrs 0 -Xmaxwarns 0 \
		-subpackages org.evoludo.gwt.graphics:\
					 org.evoludo.gwt.simulator:\
					 org.evoludo.gwt.ui:\
					 org.evoludo.jre:\
					 org.evoludo.jre.graphics:\
					 org.evoludo.jre.simulator:\
					 org.evoludo.simulator:\
					 org.evoludo.simulator.models:\
					 org.evoludo.simulator.modules:\
					 org.evoludo.simulator.views:\
					 org.evoludo.util ;

$(EVOLUDO_BUILD)/applets/TestEvoLudo.jar :
	ant test

build-test : $(EVOLUDO_BUILD)/applets/TestEvoLudo.jar

test-generate : build-test
	java -jar $(EVOLUDO_BUILD)/applets/TestEvoLudo.jar --tests $(EVOLUDO_HOME)/test/references --generate $(EVOLUDO_HOME)/test/generators --compress

test : build-test
	java -jar $(EVOLUDO_BUILD)/applets/TestEvoLudo.jar --tests $(EVOLUDO_HOME)/test/references

clean-doc :
	rm -rf $(EVOLUDO_BUILD)/$(EVOLUDO_DOC)

clean-test :
	rm -rf $(EVOLUDO_BUILD)/applets/TestEvoLudo.jar

clean : clean-doc clean-test
