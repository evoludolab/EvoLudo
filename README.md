# **[EvoLudo](https://www.evoludo.org)**
*Evolutionary Dynamics Simulation Toolkit*

In order to see EvoLudo in action, visit the growing collection of interactive online tutorials at [www.evoludo.org](https://www.evoludo.org), which showcase the fascinating spatio-temporal dynamics arising from evolutionary processes.

## Installation of the EvoLudo developer framework
In order to set up the EvoLudo developer framework a number of steps are required (as detailed below). The proper setup is crucial but fortunately is required only once.

### Requirements
1. Java compiler: Install <i>[Java SDK8](http://www.oracle.com/technetwork/java/javase/downloads/index.html)</i> or better (Java SE 13 at the time of writing).
2. Developer environment: Install <i>[eclipse](https://www.eclipse.org/downloads/eclipse-packages/)</i> IDE, Java EE edition recommended.
3. JavaScript converter: Launch <i>eclipse</i> and open `Eclipse Marketplace...` in the Help-Menu. Search for `GWT` (<i>[Google Web Toolkit](http://www.gwtproject.org)</i>) and install `GWT Eclipse Plugin` (version 3.0.0 at time of writing).
4. Version control system: Launch <i>eclipse</i> and open `Eclipse Marketplace...` in the Help-Menu. Search for `EGit` and install the `EGit - git Integration for Eclipse` (version 5.5.5 at time of writing, included with *eclipse* Java EE edition).

*Note:* `GWT` is only required for running EvoLudo in a web browser. For compiling `java` applications the `ant` build system is required (see below).

### Recommended additional software
Installation of further useful command line tools is recommended. For macOS, [macports](https://www.macports.org) is an easy package manager (start by installing [macports](https://www.macports.org/install.php) for your version of the OS; note that this in turn requires the installation of Apple's Xcode and it's command line tools from the AppStore).
1. `git` command line tool: `port install git`.<br/>
*Purpose:* during the build process a call to `git describe` retrieves the current version information.
2. `ant` build system: `port install apache-ant`.<br/>
*Purpose:* required to build `java` applications for running simulations.


### Installation
Import EvoLudo project into *eclipse*:
1. Launch *eclipse*. If this is the first time, choose/create a folder as the *eclipse* workspace.
2. Open `File > Import...`, search for `git` and select the `Projects from Git` import wizard.
3. Select `Clone URI` and enter `https://github.com/evoludolab/EvoLudo.git` in the URI field.
4. Ensure the `master` branch is checked; all others can be unchecked.
5. Choose local directory for `git` repository, e.g. create new directory `EvoLudo` in *eclipse* workspace.
6. Wait until the `EvoLudo` repository is downloaded.
7. Select `Import existing Eclipse projects` (default) and click `Finish`.

*Note:* If *eclipse* reports compilation errors, just ignore them for now. The  finishing touches below should address all of them.

Alternatively, either manually download the EvoLudo source code from [GitHub](https://github.com/evoludolab/EvoLudo) or use the `GitHub Desktop` application to download the EvoLudo source code and import the local copy of the repository into *eclipse*.

### Finishing touches
1. Make sure `GWT 2.8.2` (or better) is the default version.<br/>
Launch *eclipse*. Open `Eclipse > Preferences...` and search for `GWT`. Under `GWT Settings` check version 2.8.2 or higher. Note, currently the `GWT Eclipse Plugin` only includes `GWT 2.8.1`. In order to add a newer GWT version:
    1. Download the newest `GWT SDK` from the [GWT Project](http://www.gwtproject.org/download.html).
    2. Unzip the archive and copy the folder to a convenient location, e.g. the *eclipse* workspace.
    3. In the *eclipse* `GWT Settings` click `Add...` and choose the folder with the downloaded GWT version.
    4. Select `GWT 2.8.2` (or better) as the default.
2. Make sure compiler compliance level is set to 1.8.<br/>
Launch *eclipse*. Open `Eclipse > Preferences...` and search for `compiler compliance`. Under `Java Compiler` select compiler compliance level 1.8 (as of GWT version 2.8.2 newer levels cannot be handled) and rebuild the project.
3. Create an empty file at `src/org/evoludo/simulator/web/theme/git.version` (during the build process the git version is stored here).<br/>
Open a terminal and change to the root directory of EvoLudo. On \*nix/macOS systems execute `touch src/org/evoludo/simulator/web/theme/git.version` and on Windows find another way to create the file at the above location.
4. (Optional) Use *Google Chrome* as the default web browser for development (facilitates debugging). <br/>
If *Google Chrome* is not the default web browser of the system, open `Eclipse > Preferences...` and search for `web browser`. Under `Web Browser` select `Use external browser` and select *Google Chrome* in the list or click `New...` to add it to the list and select it.
5. (Optional) Open EvoLudo properties (`Project > Properties`) and select `Builders` then select `Git version` and press `Edit...`. On the `Main` tab make sure the `Location` points to the `git` executable. The default is `/usr/bin/git`.
6. (Optional) Verify that the paths in the configuration file `ant/<OS name>.properties` are correctly set, where `<OS name>` refers to the OS of the machine:
    1. `git.exe` needs to point to the `git` executable.
    2. `java.runtime` must point to the `java` runtime library (usually `rt.jar`).<br/>
*Note:* `java.runtime` is only important for compiling the EvoLudo source into a `java` application (simulations as well as the interactive `java` GUI) but is not needed for GWT development.
7. Restart *eclipse*.

### Known issues:
1. The built-in Jetty web server of the `GWT Eclipse Plugin` throws an error on launch: <br/>
``[ERROR] jreLeakPrevention.gcDaemonFail
java.lang.ClassNotFoundException: sun.misc.GC``. Fortunately this is inconsequential and can be safely ignored.
2. For newer versions of `java` (Java 9+) the communication between *Google Chrome* and *eclipse* is unfortunately broken (see [sdbg issues](https://github.com/sdbg/sdbg/issues/161). The following steps implement a shortcut for the workaround listed in aforementioned source (*optional, not required for development*):
    1. Download the archive [com.github.sdbg.releng.p2-1.0.10.qualifier.zip](https://lorax.math.ub.ca/EvoLudo/com.github.sdbg.releng.p2-1.0.10.qualifier.zip).<br/>
The archive was built according to the  workaround (the above download solely saves the trouble of compiling the entire sdbg sources).<br/>
*Note:* make sure the browser does not unzip the archive after downloading.
    2. Install the archive in *eclipse*.<br/>
Open `Help > Install New Software...`, click `Add...` then `Archive...` and select the above archive. Name it something like `SDBG (java 9+)`, click `Add`, check `Source Map Debugger` and click `Finish`.


## Development

### Extend EvoLudo:
The `GWT Eclipse Plugin` provides a development WebServer, which enables rapid development, test and debug cycles.
1. Launch *eclipse*.
2. From the Debug or Run configurations select `EvoLudo - GWT Development WebServer` to compile the project and launch the development web server.<br/>
*Note:* may not be available before completing all steps above, including restarting *eclipse*.
3. From the Debug or Run configurations select `EvoLudo - GWT Debug with Chrome` or open a browser and point it to [http://127.0.0.1:8888/TestEvoLudoLabs.html](http://127.0.0.1:8888/TestEvoLudoLabs.html) to launch the web interface of EvoLudo.<br/>
*Note:* may not be available if *Google Chrome* is not installed or unknown to *eclipse*.
4. After making changes to the code in *eclipse*, simply reload the browser page to trigger a recompile of the EvoLudo source code.<br/>
*Note:* due to the size of the project, the first build may take fairly long but subsequent recompiles are significantly faster.

### Debug EvoLudo:
Fortunately step-by-step debugging can be largely avoided thanks to the efficient developer mode of GWT. However, if the need arises breakpoints and step-by-step debugging is possible both in *Google Chrome* (or other browsers) as well as in *eclipse*.<br/>
*ToDo...*

### Compile EvoLudo:
The GWT compiler converts the `java` source code into platform and browser independent JavaScript.
1. Launch *eclipse*.
2. Right-click on EvoLudo in `Project Explorer`, select the `GWT > Compile` menu and click `Compile`.
3. The compiled JavaScript is located in `war/evoludoweb`. For examples on how to embed an interactive EvoLudo lab on a web page, see `war/TestEvoLudoLabs.html`.
