# path to relevant executables
JAVADOC = $(shell which javadoc)

# top level directories of EvoLudo
EVOLUDO_HOME = $(CURDIR)
EVOLUDO_DOC = $(CURDIR)/docs
EVOLUDO_SRC = $(CURDIR)/src
EVOLUDO_BUILD = $(CURDIR)/build

# targets
all:

# note: to include all errors/warnings add: -Xmaxerrs 0 -Xmaxwarns 0

# at least for now, place docs in build directory. may graduate to a top level
# location once EvoLudo is public and if documentation is served from github.
docs:
	mkdir -p $(EVOLUDO_BUILD)/$(EVOLUDO_DOC) ;
	javadoc -d $(EVOLUDO_BUILD)/$(EVOLUDO_DOC) -sourcepath $(EVOLUDO_SRC) \
		-classpath ../Google/releases/gwt-2.8.2.XHTML/gwt-user.jar:./lib/parallax-1.6.jar:./lib/mtj-0.9.14.jar:./lib/freehep-graphicsio-svg-2.4.jar:./lib/freehep-graphics2d-2.4.jar:./lib/freehep-graphicsio-ps-2.4.jar:./lib/freehep-graphicsio-pdf-2.4.jar \
-Xmaxerrs 0 -Xmaxwarns 0 \
		-subpackages ec.util:org.evoludo.gwt:org.evoludo.jre:org.evoludo.simulator:org.evoludo.util ;

clean:
	rm -rf $(EVOLUDO_BUILD)/$(EVOLUDO_DOC)
