# path to relevant executables
# javadoc of active java installation
#JAVADOC = $(shell which javadoc)
# jdk-11 provides last javadoc that supports frames
JAVADOC = /Library/Java/JavaVirtualMachines/jdk-11.0.7.jdk/Contents/Home/bin/javadoc


# top level directories of EvoLudo
EVOLUDO_HOME = $(CURDIR)
EVOLUDO_SRC = $(CURDIR)/src
EVOLUDO_BUILD = $(CURDIR)/build
EVOLUDO_DOC = $(CURDIR)/docs
# at least for now, place docs in build directory. may graduate to a top level
# location once EvoLudo is public and if documentation is served from github.
#EVOLUDO_DOC = $(EVOLUDO_BUILD)/docs

VERSION = $(shell cat $(EVOLUDO_SRC)/org/evoludo/simulator/git.version)
DATE = $(shell date +'%B %d, %Y')

.PHONY: all test clean

# targets
all:

# notes: 
# - to include all errors/warnings add: -Xmaxerrs 0 -Xmaxwarns 0
# - no whitespace allowed in list of subpackages
# - only one -header/-footer option permissible (last takes precedence)
# - frames option requires javadoc from JDK 11 or earlier
# - in eclipse just run Project > Generate Javadoc... for entire EvoLudo project

docs :
	mkdir -p $(EVOLUDO_DOC) ;
	$(JAVADOC) -d $(EVOLUDO_DOC) \
		-sourcepath $(EVOLUDO_SRC) \
		-doctitle "EvoLudo: in silico evolution" \
		-windowtitle "EvoLudo: in silico evolution" \
		-classpath ../Google/releases/gwt-2.10.0.XHTML/gwt-user.jar:./lib/parallax-1.6.jar:./lib/mtj-1.0.4.jar:./lib/freehep-graphicsio-svg-2.4.jar:./lib/freehep-graphics2d-2.4.jar:./lib/freehep-graphicsio-ps-2.4.jar:./lib/freehep-graphicsio-pdf-2.4.jar \
		-link https://docs.oracle.com/en/java/javase/19/docs/api \
		-link https://www.gwtproject.org/javadoc/latest \
		-link https://thothbot.github.io/parallax/docs/1.6 \
		-Xmaxerrs 100 -Xmaxwarns 0 \
		--allow-script-in-comments \
		-header "<script src='https://polyfill.io/v3/polyfill.min.js?features=es6'></script><script id='MathJax-script' async src='https://cdn.jsdelivr.net/npm/mathjax@3/es5/tex-mml-chtml.js'></script>" \
		-footer "<span style='display:inline-block; padding:14px 0;'>$(DATE)&nbsp;$(VERSION)</span>" \
		--frames \
		org.evoludo.geom\
		org.evoludo.gwt.graphics\
		org.evoludo.gwt.simulator\
		org.evoludo.gwt.ui\
		org.evoludo.jre.graphics\
		org.evoludo.jre.simulator\
		org.evoludo.jre.simulator.exec\
		org.evoludo.simulator\
		org.evoludo.simulator.models\
		org.evoludo.simulator.modules\
		org.evoludo.simulator.views\
		org.evoludo.util ;

$(EVOLUDO_BUILD)/applets/TestEvoLudo.jar :
	ant test

build-test : $(EVOLUDO_BUILD)/applets/TestEvoLudo.jar

test-generate : build-test
	java -jar $(EVOLUDO_BUILD)/applets/TestEvoLudo.jar --tests $(EVOLUDO_HOME)/test/references --generate $(EVOLUDO_HOME)/test/generators --compress

test : build-test
	java -jar $(EVOLUDO_BUILD)/applets/TestEvoLudo.jar --tests $(EVOLUDO_HOME)/test/references/current --reports $(EVOLUDO_HOME)/test/reports

clean-docs :
	rm -rf $(EVOLUDO_DOC)

clean-test :
	ant clean ;
	rm -rf $(EVOLUDO_BUILD)/applets/TestEvoLudo.jar

clean : clean-doc clean-test
