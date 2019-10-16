# **[EvoLudo](https://www.evoludo.org>)**
Evolutionary Dynamics Simulation Toolkit

## Installation of the EvoLudo developer framework

### Requirements
1. Java compiler: Install <i>[Java SDK8](http://www.oracle.com/technetwork/java/javase/downloads/index.html)</i> or better.
2. Developer environment: Install <i>[eclipse](https://www.eclipse.org/downloads/eclipse-packages/)</i> IDE, Java EE version recommended.
3. JavaScript converter: Launch <i>eclipse</i> and open `Eclipse Marketplace...` in the Help-Menu. Search for `GWT` (<i>[Google Web Toolkit](http://www.gwtproject.org)</i>) and install `GWT Eclipse Plugin` (version 3.0.0 at time of writing). Make sure that the installation includes `GWT 2.8.0` or better.
4. Version control system: Launch <i>eclipse</i> and open `Eclipse Marketplace...` in the Help-Menu. Search for `EGit` and install the `EGit - git Integration for Eclipse` (version 5.5.5 at time of writing).

*Note:* `GWT` is only required for running EvoLudo in a web browser. For compiling `java` applications the `ant` build system is required (see below).

### Recommended additional software
Installation of further useful command line tools is recommended. For macOS, [macports](https://www.macports.org) is an easy package manager (start by installing [macports](https://www.macports.org/install.php) for your version of the OS; note that this in turn requires the installation of Apple's Xcode and it's command line tools from the AppStore).
1. `git` command line tool: `port install git`.<br/>
*Purpose:* during the build process a call to `git describe` retrieves the current version information.
2. `ant` build system: `port install apache-ant`.<br/>
*Purpose:* required to build `java` applications for running simulations.


### Installation
Download EvoLudo source code from [GitHub](https://github.com/evoludolab/EvoLudo):
  1. Launch *eclipse*.
  2. Create new project (`File > New > Other...`).
  3. Search for `git` and select `Git Repository`
  4. Choose location/directory for local repository
  5. ***scratch*** set remote to `https://github.com/evoludolab/EvoLudo.git`.

Alternatively, use the `GitHub Desktop` application to download the EvoLudo source code and import the local copy of the repository into *eclipse*.

*Note:* If *eclipse* reports compilation errors, just ignore them for now. This is addressed with the following finishing touches.

### Finishing touches
1. Make sure `GWT 2.8.0` (or better) is the default version.<br/>
Launch *eclipse*. Open `Eclipse > Preferences...` and search for `GWT`. Under `GWT Settings` check version 2.8.0 or higher.
2. Make sure compiler compliance level is set to 1.8.<br/>
Launch *eclipse*. Open `Eclipse > Preferences...` and search for `compiler compliance`. Under `Java Compiler` select compiler compliance level 1.8 (as of GWT version 2.8.2 newer levels cannot be handled).
3. Open EvoLudo properties (`Project > Properties`) and select `Builders` then select `Git version` and press `Edit...`. On the `Main` tab make sure the `Location` points to the `git` executable. The default is `/usr/bin/git`.
4. Open a Terminal and change to the root directory of EvoLudo source code. On \*nix/macOS systems execute `touch src/org/evoludo/simulator/web/theme/git.version` and on Windows find another way to simply create an empty file at the above location.
5. Verify that the paths in the configuration file `ant/<OS name>.properties` are correctly set:
  1. `git.exe` needs to point to the `git` executable.
  2. `java.runtime` must point to the `java` runtime library (usually `rt.jar`).<br/>
  *Note:* this is only important for compiling the EvoLudo source into a `java` application, both for building simulations and the interactive GUI, but is not needed for GWT development.

### Debugging
For newer versions of `java` (Java 9+) the communication between *Google Chrome* and *eclipse* is unfortunately broken (see [sdbg issues](https://github.com/sdbg/sdbg/issues/161). Fortunately a workaround exists adapted from the aforementioned source:
1. Download the archive [com.github.sdbg.releng.p2-1.0.10.qualifier.zip](https://lorax.math.ub.ca/EvoLudo/com.github.sdbg.releng.p2-1.0.10.qualifier.zip).<br/>
The archive was built according to the above workaround (the download solely saves the trouble of compile the entire sdbg sources).<br/>
*Note:* make sure the browser does not unzip the archive after downloading.
2. Install the archive in *eclipse*.<br/>
Open `Help > Install New Software...`, click `Add...` then `Archive...` and select the above archive. Name it something like `SDBG (java 9+)`, click `Add`, check `Source Map Debugger` and click `Finish`.


## Development

### Compile project:
1. Launch *eclipse*.
2. From the Debug or Run configurations select `EvoLudo - GWT Development WebServer` to compile the project and launch the development web server.
3. Open a browser and point it to [http://127.0.0.1:8888/TestEvoLudoLabs.html](http://127.0.0.1:8888/TestEvoLudoLabs.html) to launch the web interface of EvoLudo.
4. After making changes to the code in *eclipse*, simply reload the browser page to trigger a recompile of the EvoLudo source code.<br/>
*Note:* due to the size of the project, the first build may take fairly long but subsequent recompiles are significantly faster.
