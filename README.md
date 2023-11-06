# **[EvoLudo](https://www.evoludo.org)**
*Evolutionary Dynamics Simulation Toolkit*

Visit the growing collection of interactive online tutorials at [www.evoludo.org](https://www.evoludo.org) to see *EvoLudo* in action and to explore game theory and the fascinating spatio-temporal dynamics arising from evolutionary processes.

*EvoLudo* ([*ludo:*](http://en.wiktionary.org/wiki/ludo) Latin for "I play" or Italian for "game") is the engine behind numerous scientific research articles since 2001 (including in *Nature*, *Science* and *Proc. Natl. Acad. Sci. USA*). A [selection of articles](https://wiki.evoludo.org/index.php?title=Research) with summaries (and a growing number accompanied by interactive labs) is available, as well as a [complete list](https://www.math.ubc.ca/~hauert/). *EvoLudo* provides an interactive way to confirm the reported results and invites further exploration.

## Installation of the *EvoLudo* framework

### Requirements
1. Java compiler: Install *Java SDK 8* or better (*Java SDK 21* at the time of writing).
2. Developer tools: Install the [`maven`](https://maven.apache.org) and [`git`](https://git-scm.com/downloads).
3. Additional tools:
   - [`ant`](https://ant.apache.org) for compiling customized simulation modules.
   - [`make`](https://www.gnu.org/software/make/) for assembling javadocs, html and JavaScript components.<br>

   > [!NOTE]
   > the dependency on `ant` and `make` will be replaced by `maven` in the future.

### Download
The *EvoLudo* simulation toolkit is available on [github](https://github.com/evoludolab/EvoLudo). Obtain the source code or release at https://github.com/evoludolab/EvoLudo. For example, executing `git clone https://github.com/evoludolab/EvoLudo` downloads the latest source code. Visit https://github.com/evoludolab/EvoLudo/releases for the current list of releases.


## Quick start
> [!IMPORTANT]
> first execute `mvn clean` to download and install required software dependencies into your local maven repository. This includes those from remote repositories as well as those provided by *EvoLudo*.

The *EvoLudo* project consists of three modules:
<dl>
<dt>EvoLudoCore
<dd>The core jave code of *EvoLudo* shared by GWT as well as JRE. This is the backend that deals with the numerical integration of differential equations (ordinary, stochasti, or partial) as well as individual based simulation.
<dt>EvoLudoGWT
<dd>The GWT specific code of *EvoLudo*. This includes all the GUI components for visualization in the web browser.
<dt>EvoLudoJRE
<dd>The JRE specific code of *EvoLudo*. This includes customized as well as generic java simulations. For examples of customized simulations, see `EvoLudoJRE/src/main/org/evoludo/simulator/exec/`. In addition it also includes all the GUI components when running as a java application. <br>

   > [!NOTE]
   > at this point the GUI components are maintained but not further developed in favour of the GWT counterpart, which is also significantly richer in features. 
</dl>

Simply execute `mvn clean install` to compile all three modules. Most notably this results in:
<dl>
<dt>EvoLudoCore/target/EvoLudoCore-<&ZeroWidthSpace;version>(.jar)</dt>
<dd>The compiled shared code of <i>EvoLudo</i>.</dd>
<dt>EvoLudoGWT/target/EvoLudoGWT-<&ZeroWidthSpace;version>(.war)</dt>
<dd>The compiled GWT specific code of <i>EvoLudo</i>. In particular, this includes the <tt>evoludoweb</tt> directory with the comiled JavaScript code ready for running in the web browser.<br>

   > [!NOTE]
   > This is work in progress. Eventually this should include all GWT files necessary for developing, debugging or deploying the <tt>evoludoweb</tt> app.</dd>
<dt>EvoLudoJRE/target/EvoLudoJRE-<&ZeroWidthSpace;version>(.war)</dt>
<dd>The JRE specific code of <i>EvoLudo</i>. <br>

   > [!NOTE]
   > This is work in progress. <tt>maven</tt> does not yet produce the <tt>EvoLudo.jar</tt> file but places all GWT components here... Currently the <tt>EvoLudo.jar</tt> can only be produced by executing <tt>ant</tt> and is then located in <tt>build/EvoLudo.jar</tt>.</dd>
</dl>

## Research
Explorations in the web browser are very useful but have significant limitations. Most importantly execution speed is of the essence for running simulations but also access to data. The *EvoLudo* project overs different modes for running simulations depending on how the <tt>build/EvoLudo.jar</tt> is launched.

> [!WARNING]
> Documentation incomplete.

<!-- Next, there are two options to choose:

1. Build a `jar` executable to run simulations with `java`:<br>
Execute `ant EvoLudo` to compile and build the `java` application for running generic *EvoLudo* simulations. Depending on the command line options this runs with or without a GUI. For customised simulations, see examples in `src/org/evoludo/jre/simulator/exec/`.
2. Translate the `java` code into `JavaScript` to run with a GUI in a web browser:<br>
Execute `mvn gwt:compile` to convert the `java` code into a `JavaScript` web application. The compiled code is in the `target/EvoLudo-x.x.x/evoludoweb` directory. The `evoludoweb` directory then needs to be copied to a webserver together with `.(x)html` files that load the *EvoLudo* web application (see `war` directory and section 'Developement').<br>
Alternatively, executing `ant gwt-build` does the same but places the compiled code into `war/evoludoweb` directory. The `war` directory includes several `.(x)html` files that may be useful to test (and for the rapid development of) the web application. This means you can open one of them, e.g. `TestEvoLudoLabs.html` and the freshly compiled *EvoLudo* web application is loaded ready to satisfy your evolutionary curiosity. -->

## Development
1. The configuration files in the *EvoLudo* repository are designed to support development using [`VSCode`](https://code.visualstudio.com).
2. `mvn gwt:devmode` translates the java code into `JavaScript` and launches a development server at `localhost`. The code is located in the `target/gwt/devmode/war` directory. Copy all files from the `EvoLudoJRE/srcmain/webapp` directory. Point your browser to `http://localhost:8888/<yourfile>.html` and start developing your own *EvoLudo* features.<br>

   > [!NOTE]
   > This work in progress. The steps and directories are likely to change. For now, the first launch of `mvn gwt:devmode` generates an empty `target/gwt/devmode/war` directory. Cancel the `maven` build, copy the `EvoLudoJRE/srcmain/webapp` directory and execute `mvn gwt:devmode` again. Now Jetty can serve the html files and the `evoludoweb` GWT application.

   > [!NOTE]
   > Unsurprisingly `localhost` is only reachable from the local machine. If, instead, you would like to test the *EvoLudo* web application on another device (for example to test touch features on a tablet), add the option `-Dgwt.server=<server>` to the command line where `<server>` is the DNS name or IP address of the machine that runs the development server. Now the server is reachable from the outside world at `http://<server>:8888/<yourfile>.html`.

3. The debug/run targets in `VSCode` support step-by-step debugging of both java code as well as code running in your browser. For example, the configuration `JRE EvoLudo` launches `EvoLudo.jar` with a set of options specified in `launch.json`. Similarly, the configuration `GWT EvoLudo (10.0.0.10)` launches the webserver and assumes that the local machine has the IP address `10.0.0.10` and listens on port `8888` for incoming connections. On port `9876` listens the source map provider linking the compiled and obfuscated `JavaScript` code to the actual source code files.

4. The API documentation for the *EvoLudo* simulation toolkit is automatically generated from the source files by executing `mvn site` and placed in `target/site/apidocs`. <br>

   > [!NOTE]
   > 1. The javadoc generation is configured to use <i>Java SDK 11</i>, which is the last version to support frames when generating the documentation. You may need to adjust the `<javadocExecutable>` in the `pom.xml` to fit your development setup and `java` version and possibly remove the `--frames` option from the `<additionalJOption>`. 
   > 2. Documentation of the latest tagged version is available at https://www.evoludo.org/docs/api. 

### Contribute to *EvoLudo*
Pull requests by anyone are most welcome!

## History
Unmaintained instructions for developing *EvoLudo* using the [*eclipse* IDE](docs/installation/ECLIPSE.md) are available for reference.

## Acknowledgments
EvoLudo relies on other open source projects:
1. [`MersenneTwister`](http://www.math.sci.hiroshima-u.ac.jp/~m-mat/MT/emt.html) and, more specifically, the *java* port of MersenneTwister by [Jean Luke](https://cs.gmu.edu/~sean/research/) with further adaptations to make it *GWT* compliant.
2. [`Parallax`](https://thothbot.github.io) version 1.6 for the 3D rendering of population structures using WebGL. Moreover, dust off your red-cyan glasses to explore the structures in real 3D (see context menu).
3. [`Canvas2Svg.js`](https://gliffy.github.io/canvas2svg/) to export and save *EvoLudo* graphics.

A big thank you to the respective researchers and developers.
