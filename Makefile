# path to relevant executables
# javadoc of active java installation
#JAVADOC = $(shell which javadoc)
# jdk-11 provides last javadoc that supports frames
JAVADOC = /Library/Java/JavaVirtualMachines/jdk-11.0.2.jdk/Contents/Home/bin/javadoc


# top level directories of EvoLudo
EVOLUDO_HOME = $(CURDIR)
EVOLUDO_SRC = $(CURDIR)/src
EVOLUDO_BUILD = $(CURDIR)/build
EVOLUDO_DIST = $(CURDIR)/dist
EVOLUDO_DOC = $(CURDIR)/docs/api

GIT_COMMIT_ID = 'git.commit.id.describe'
GIT_BUILD_TIME = 'git.build.time'
VERSION = $(shell git describe --tags --dirty='*')
DATE = $(shell git show -s --date='format:%B %d, %Y @ %H:%m %z' --format='%cd')

.PHONY: all test clean docs

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
		-classpath ./lib/gwt-user-XHTML-2.10.0.jar:./lib/parallax-gwt-1.6.jar:./lib/mtj-1.0.4.jar:./lib/freehep-graphicsio-svg-2.4.jar:./lib/freehep-graphics2d-2.4.jar:./lib/freehep-graphicsio-ps-2.4.jar:./lib/freehep-graphicsio-pdf-2.4.jar \
		-link https://docs.oracle.com/en/java/javase/19/docs/api \
		-link https://www.gwtproject.org/javadoc/latest \
		-link https://thothbot.github.io/parallax/docs/1.6 \
		-Xmaxerrs 100 -Xmaxwarns 0 \
		--allow-script-in-comments \
		-header "<script src='https://polyfill.io/v3/polyfill.min.js?features=es6'></script><script id='MathJax-script' async src='https://cdn.jsdelivr.net/npm/mathjax@3/es5/tex-mml-chtml.js'></script>" \
		-footer "<span style='display:inline-block; padding:14px 0;'>$(DATE)&nbsp;$(VERSION)</span>" \
		--frames \
		org.evoludo.geom\
		org.evoludo.graphics\
		org.evoludo.simulator\
		org.evoludo.ui\
		org.evoludo.graphics\
		org.evoludo.simulator\
		org.evoludo.simulator.exec\
		org.evoludo.simulator\
		org.evoludo.simulator.models\
		org.evoludo.simulator.modules\
		org.evoludo.simulator.views\
		org.evoludo.util ;

docs-clean :
	rm -rf $(EVOLUDO_DOC)

$(EVOLUDO_BUILD)/TestEvoLudo.jar :
	ant test

test-build : $(EVOLUDO_BUILD)/TestEvoLudo.jar

# provide --reports option to store failed test reports
test-generate : test-build
	java -jar $(EVOLUDO_BUILD)/TestEvoLudo.jar \
		--tests $(EVOLUDO_HOME)/test/references \
		--generate $(EVOLUDO_HOME)/test/generators \
		--reports $(EVOLUDO_HOME)/test/reports \
		--compress

test : test-build
	java -jar $(EVOLUDO_BUILD)/TestEvoLudo.jar \
		--tests $(EVOLUDO_HOME)/test/references/current \
		--reports $(EVOLUDO_HOME)/test/reports

test-clean :
	ant clean ;
	rm -rf $(EVOLUDO_BUILD)/TestEvoLudo.jar

sims :
	ant EvoLudo

gwt :
	ant gwt-build

clean : docs-clean test-clean dist-clean

dist-init :
	mkdir -p $(EVOLUDO_DIST)/jar
	mkdir -p $(EVOLUDO_DIST)/war
	mkdir -p $(EVOLUDO_DIST)/doc
	mkdir -p $(EVOLUDO_DIST)/reports

dist-test : test-build
	java -jar $(EVOLUDO_BUILD)/TestEvoLudo.jar \
		--tests $(EVOLUDO_HOME)/test/references/current \
		--reports $(EVOLUDO_DIST)/reports

dist-clean :
	rm -rf $(EVOLUDO_DIST)

dist : clean dist-init docs gwt sims dist-test
	mv $(EVOLUDO_DOC) $(EVOLUDO_DIST)/doc
	mv $(EVOLUDO_BUILD)/*.jar $(EVOLUDO_DIST)/jar
	mv $(EVOLUDO_HOME)/war/evoludoweb $(EVOLUDO_DIST)/war
	cp -a $(EVOLUDO_HOME)/war/*.*html $(EVOLUDO_DIST)/war
