# **[EvoLudo](https://www.evoludo.org)**
*Evolutionary Dynamics Simulation Toolkit*

Visit the growing collection of interactive online tutorials at [www.evoludo.org](https://www.evoludo.org) to see EvoLudo in action and to explore game theory and the fascinating spatio-temporal dynamics arising from evolutionary processes.

EvoLudo ([*ludo:*](http://en.wiktionary.org/wiki/ludo) Latin for "I play" or Italian for "game") is the engine behind numerous scientific research articles since 2001 (including in *Nature*, *Science* and *Proc. Natl. Acad. Sci. USA*). A [selection of articles](https://wiki.evoludo.org/index.php?title=Research) with summaries (and a growing number accompanied by interactive labs) is available, as well as a [complete list](https://www.math.ubc.ca/~hauert/). *EvoLudo* provides an interactive way to confirm the reported results and invites further exploration.

## Installation of the EvoLudo developer framework

### Requirements
1. Download EvoLudo: Obtain the source code or releases at https://github.com/evoludolab/EvoLudo.
2. Java compiler: Install *Java SDK 8* or better (*Java SDK 21* at the time of writing).
3. Developer tools: Install the `maven`, `ant` and `make` build tools.

### Compilation
1. **Important:** run `mvn clean` first to download and install required software dependencies into your local repository.
2. `ant EvoLudo` compiles the `java` application for running generic *EvoLudo* simulations. For customised simulations, see `src/org/evoludo/jre/simulator/exec/`.
3. `mvn gwt:compile` compiles the `java` code into a JavaScript web application. The compiled code is in the `target/EvoLudo-x.x.x/evoludoweb` directory. Alternatively, `ant gwt-build` does the same but places the compiled code into `war/evoludoweb` directory. Note, the `war` directory includes several `.(x)html` files that may be useful to test (and rapid development of) the web application.

## Development
1. The configuration files in the *EvoLudo* repository are designed to support development using `VSCode`.
2. `mvn gwt:devmode` translates the java code into JavaScript and launches a development server at `localhost`. The code is located in the `target/gwt/devmode/war` directory. Copy the desired `.(x)html` test files from the `war` directory. Point your browser to `http://localhost:8888/<yourfile>.html` and start developing your own *EvoLudo* features.
3. The debug/run targets in `VSCode` support step-by-step debugging of both java code as well as code running in your browser.
4. `ant docs` or `make docs` generates the API documentation for the *EvoLudo* simulation toolkit in `docs/api`. *Note:* currently the javadoc generation uses <i>Java SDK 11</i> as the last version to support frames. Documents of the latest tagged version are also available at https://www.evoludo.org/docs/api

### Contribute to EvoLudo
Pull requests by anyone are most welcome!

## History
Unmaintained instructions for developing *EvoLudo* using the [*eclipse* IDE](docs/installation/ECLIPSE.md) are available for reference.

## Acknowledgments
EvoLudo relies on other open source projects:
1. [MersenneTwister](http://www.math.sci.hiroshima-u.ac.jp/~m-mat/MT/emt.html) and, more specifically, the *java* port of MersenneTwister by [Jean Luke](https://cs.gmu.edu/~sean/research/) with further adaptations to make it *GWT* compliant.
2. [Parallax](https://thothbot.github.io) version 1.6 for the 3D rendering of population structures using WebGL. Moreover, dust off your red-cyan glasses to explore the structures in real 3D (see context menu).
3. [Canvas2Svg.js](https://gliffy.github.io/canvas2svg/) to export and save EvoLudo graphics.

A big thank you to the respective researchers and developers.
