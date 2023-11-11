# [*EvoLudo*](https://www.evoludo.org)
***Evolutionary Dynamics Simulation Toolkit***

Visit the growing collection of interactive online tutorials at [www.evoludo.org](https://www.evoludo.org) to see *EvoLudo* in action and to explore game theory and the fascinating spatio-temporal dynamics arising from evolutionary processes.

*EvoLudo* ([*ludo:*](http://en.wiktionary.org/wiki/ludo) Latin for "I play" or Italian for "game") is the engine behind numerous scientific research articles since 2001 (including in *Nature*, *Science* and *Proc. Natl. Acad. Sci. USA*). A [selection of articles](https://wiki.evoludo.org/index.php?title=Research) with summaries (and a growing number accompanied by interactive labs) is available, as well as a [complete list](https://www.math.ubc.ca/~hauert/). *EvoLudo* provides an interactive way to confirm the reported results and invites further exploration.


## Installation of the *EvoLudo* framework

### Requirements
1. Java compiler: Install *Java SDK 8* or better (*Java SDK 21* at the time of writing).
2. Developer tools: Install the [*maven*](https://maven.apache.org) and [*git*](https://git-scm.com/downloads).

### Download
The *EvoLudo* simulation toolkit is available on [github](https://github.com/evoludolab/EvoLudo). Obtain the source code or release at https://github.com/evoludolab/EvoLudo. For example, executing `git clone https://github.com/evoludolab/EvoLudo` downloads the latest source code. Visit https://github.com/evoludolab/EvoLudo/releases for the current list of releases.


## Quick start
Start exploring the fascinating world of evolutionary dynamics and spatio-temporal patterns by setting up your own *EvoLudo* environment in three easy steps:

> [!IMPORTANT]
> In order to initialize your *EvoLudo* development environment execute `mvn clean` to download and install all required software dependencies into your local maven repository. This includes those from remote repositories as well as those provided by *EvoLudo*. This is only needed for the initial setup.

1. Compile all modules with `mvn clean install`.
2. Execute `mvn gwt:devmode` to launch a local [*Jetty*](https://github.com/jetty) webserver.

   > [!NOTE]
   > All warnings that are displayed in the terminal are issued by *Jetty* and can be savely ignored but should exclusively be used for development and never for deployment.
4. Click *Launch Default Browser* and try out the different *EvoLudo* modules.


## Development
With the [Quick start](#quick-start) steps everything is already setup for development too. This section covers a few more tips to get started for adding your own features to *EvoLudo*.

1. The configuration files in the *EvoLudo* repository are designed to support development using [*VSCode*](https://code.visualstudio.com) but should be easy to adjust to your favourite IDE.

2. Step 2. in the [Quick start](#quick-start) above translates the java code into *JavaScript* using the [GWT](https://www.gwtproject.org) toolkit. Step 3. launches a development server at *localhost*. Unsurprisingly *localhost* is only reachable from the local machine. If, instead, you would like to test the *EvoLudo* web application on another device (for example to test touch features on a tablet), add the option `-Dgwt.server=<server>` to the command line where `<server>` is the DNS name or IP address of the machine that runs the development server. Now the server is reachable from the outside world at `http://<server>:8888/<yourfile>.html`. On port *9876* listens the source map provider linking the compiled and obfuscated `JavaScript` code to the actual source code files.

3. For development three modes are available:
   
     1. Quick and dirty: Make changes to the source code and simply reload the page in the browser. This compiles the code as needed and shows the result. Note that the first compilation takes a little longer. Compilation fails if the changes include errors. To help resolve the issues a link is provided which shows the stack trace on a separate page.

     2. Provide some feedback: Add `GWT.log(String)` lines to the code to print out whatever is of interest. The output is displayed in the console of the browser (cmd-alt-c on the Mac).

     3. Step-by-step debugging (for *VSCode*): Open the *Run and debug* tab and launch the *GWT EvoLudo (localhost)* configuration. This will launch the selected startup URL in the Google Chrome browser (this configuration can be changed in going to *Add configuration...*). Using `GWT.log(String)` continues to work but the result is also shown in the console of VSCode. Moreover, in VSCode you can set breakpoints and do all the step-by-step fun stuff of debugging. This is actually all quite amazing (and simple) considering that in the background JavaScript is running in the browser but the breakpoints and step-by-step execution is in the java source file. Almost magically this even works for obfuscated JavaScript code that is completely unreadable by itself. If you make changes to the code, just reload the web page and there they are.

5. The debug targets in *VSCode* not only support step-by-step debugging of *GWT* code but also of traditional *java* code. Most notably the *java* code is multi-threaded preventing the GUI from potentially becoming unresponsive and is leveraged for a tremendous speed increase for PDE models.  For example, the configuration *JRE EvoLudo* launches the `EvoLudo.jar` with a set of options specified in `launch.json`.

6. It is recommended to use the web interface as much as possible during development because of the rapid workflow. 

    > [!IMPORTANT]
    > Always keep in mind that the shared *java* code in the `EvoLudoCore` module *must* be agnostic to special features of the * GWT* or of *JRE*. *GWT* specific code resides in the `EvoLudoGWT` module while *JRE* specific variants are in the `EvoLudoJRE` module

7. The API documentation for the *EvoLudo* simulation toolkit is automatically generated from the source files by executing `mvn javadoc:aggregate` and placed in `docs/api`.

   > [!NOTE]
   > 1. The javadoc generation is configured to use *Java SDK 11*, which is the last version to support frames when generating the documentation. You may need to adjust the `<javadocExecutable>` in the `pom.xml` to fit your development setup and `java` version and possibly remove the `--frames` option from the `<additionalJOption>`. 
   > 2. Documentation of the latest tagged version is available at https://www.evoludo.org/docs/api. 

8. A number of useful shell scripts are located in the `script` folder. Most notably, `builddist.sh` builds all modules, runs consistency tests and if they all pass assembles all parts of the *EvoLudo* toolkit in the `dist` folder (including the API documentation).

### Contribute to *EvoLudo*
Pull requests by anyone are most welcome!


## *EvoLudo* modules overview
The *EvoLudo* project consists of six modules:
1. ***EvoLudoCore:***<br>
The core *java* code of *EvoLudo* shared by *GWT* as well as *JRE*. This is the backend that deals with the numerical integration of differential equations (ordinary, stochastic, or partial) as well as individual based simulation and must be agnostic of  *GWT* or *JRE* specifics.

2. ***EvoLudoGWT:***<br>
The *GWT* specific code of *EvoLudo*. Most notably this includes all the GUI components for visualizations in the web browser as well as handling the asynchronous scheduling of tasks in the browser.

3. ***EvoLudoDev:***<br>
Handles the *Jetty* server for development using a web browser and keeps the `(x)html` files for loading the *GWT* application.

4. ***EvoLudoJRE:***<br>
The *JRE* specific code of *EvoLudo*. This provides the basis for generic as well as customized *java* simulations. In addition it also includes all the GUI components when running as an old fashioned *java* application. <br>

   > [!NOTE]
   > at this point the GUI components are maintained but not further developed in favour of the GWT counterpart, which is also significantly richer in features. 

   For examples of customized simulations, see module `EvoLudoSims`.

5. ***EvoLudoSims:***<br>
Handles customized *java* simulations. The simulations are kept in `EvoLudoJRE/src/main/org/evoludo/simulator/exec/`. By default the `simTBT.jar` executable is generated. Other executable can be generated using the option `-Devoludo.sim=<simulation>` where `<simulation>` denotes the class name of the simulation.

6. ***EvoLudoTest:***<br>
Test suite for the different *EvoLudo* modules in `EvoLudoCore/src/main/java/org/evoludo/simulator/modules` (not to be confused with the *maven* modules). All tests must always pass. There are two main reasons that test may legitimately fail: first, if the names or parsing of command line options changed, one or several module tests may fail; second, after fundamental changes (extensions or bugfixes) to the *EvoLudo* models the tests of those models likely fail. In either case the changes must be carefully checked and once approved a new set of tests needs to be generated.


## *EvoLudo* features
1. Reproducible simulations using the command line option `--seed[=<seed>]` where `<seed>` is an arbitrary integer number (defaults to zero)
2. The *GWT* application produces *identical* output as the *java* simulations. In fact, the state of any *EvoLudo* module/model can be exported in one framework and continued in the other again with *identical* results.

> [!WARNING]
> Incomplete.


## Research
Explorations in the web browser are very useful but have significant limitations. Most importantly execution speed is of the essence for running simulations but also access to data. The *EvoLudo* project overs different modes for running simulations depending on how the <tt>build/EvoLudo.jar</tt> is launched.

> [!WARNING]
> Incomplete.

<!-- ## History
Unmaintained instructions for developing *EvoLudo* using the [*eclipse* IDE](docs/installation/ECLIPSE.md) are available for reference.-->

## Acknowledgments
EvoLudo relies on other open source projects:
1. [`MersenneTwister`](http://www.math.sci.hiroshima-u.ac.jp/~m-mat/MT/emt.html) and, more specifically, the *java* port of MersenneTwister by [Jean Luke](https://cs.gmu.edu/~sean/research/) with further adaptations to make it *GWT* compliant.
2. [`Parallax`](https://thothbot.github.io) version 1.6 for the 3D rendering of population structures using WebGL. Moreover, dust off your red-cyan glasses to explore the structures in real 3D (see context menu).
3. [`Canvas2Svg.js`](https://gliffy.github.io/canvas2svg/) to export and save *EvoLudo* graphics.

A big thank you to the respective researchers and developers.
