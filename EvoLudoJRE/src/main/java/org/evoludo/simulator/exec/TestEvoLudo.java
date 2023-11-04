//
// EvoLudo Project
//
// Copyright 2020 Christoph Hauert
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//	http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//
// For publications in any form, you are kindly requested to attribute the
// author and project as follows:
//
//	Hauert, Christoph (<year>) EvoLudo Project, http://www.evoludo.org
//			(doi: <doi>[, <version>])
//
//	<doi>:	digital object identifier of the downloaded release (or the
//			most recent release if downloaded from github.com),
//	<year>:	year of release (or download), and
//	[, <version>]: optional version number (as reported in output header
//			or GUI console) to simplify replication of reported results.
//
// The formatting may be adjusted to comply with publisher requirements.
//

package org.evoludo.simulator.exec;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.evoludo.simulator.EvoLudoJRE;
import org.evoludo.simulator.models.Model;
import org.evoludo.util.Plist;
import org.evoludo.util.PlistParser;

/**
 *
 * @author Christoph Hauert
 */
public class TestEvoLudo implements Model.MilestoneListener {

	/**
	 * Pointer to engine. Engine has EvoLudoJRE class but do not rely on JRE
	 * specifics.
	 */
	EvoLudoJRE engine;

	/**
	 * Logger for keeping track of and reporting events and issues.
	 */
	Logger logger;

	File testsDir; // directory to containing test or to store generated tests
	File reportsDir; // directory to store reports of failed tests
	File testFile; // test file
	File generator; // directory with generator scripts
	File exportDir; // (sub)directory for exporting generated reference files
	boolean performTest;
	boolean testRunning;
	boolean useCompression = false;
	boolean dumpMinor = false;
	boolean verbose = false;

	public TestEvoLudo() {
		engine = new EvoLudoJRE();
		// applications remove --export option because it does not make sense
		engine.isApplication = false;
		logger = engine.getLogger();
		// register as Model.MilestoneListener to receive milestone notifications
		engine.addMilestoneListener(this);
	}

	public void generate() {
		if (generator.isDirectory()) {
			File[] gens = generator.listFiles(new FilenameFilter() {
				@Override
				public boolean accept(File dir, String name) {
					return name.endsWith(".clo");
				}
			});
			for (File f : gens)
				generate(f);
		} else {
			generate(generator);
		}
	}

	protected void generate(File clo) {
		String filename = clo.getName();
		exportDir = new File(testsDir.getPath() + File.separator + filename.substring(0, filename.lastIndexOf(".clo")));
		if (!exportDir.exists() && !exportDir.mkdir()) {
			logWarning("Generating: failed to make directory '" + exportDir.getPath() + "' - skipped.");
			return;
		}
		engine.setExportDir(exportDir);
		String list;
		try {
			list = new String(Files.readAllBytes(clo.toPath()));
		} catch (IOException e) {
			logWarning("Generating: failed to read '" + clo.getName() + "' - skipped.");
			return;
		}
		String[] tests = list.split("\n");
		int nTest = 0;
		for (String test : tests) {
			// skip empty lines or comments (lines starting with '#')
			if (test.strip().length() < 1 || test.startsWith("#"))
				continue;
			// check if test result file already exists
			String export;
			int exportIdx = test.indexOf("--export");
			if (exportIdx < 0) {
				export = exportDir.getName() + "-" + (++nTest) + ".plist";
				test = test + " --export " + export;
			} else {
				exportIdx += "--export".length();
				int exportEnd = test.indexOf("--", exportIdx);
				if (exportEnd < 0)
					exportEnd = test.length();
				export = test.substring(exportIdx, exportEnd).strip();
			}
			if (new File(exportDir.getPath() + File.separator + export).exists()
					|| new File(exportDir.getPath() + File.separator + export + ".zip").exists()) {
				logWarning("Generating: reference file '" + export + "' exists - skipping!");
				continue;
			}
			engine.unloadModule();
			// set command line options of test to generate
			// prepend --seed 0 to ensure fixed seed; append --delay 0 to run at full speed.
			// note, later instances of repeated options take precedence.
			engine.setCLO("--seed 0 " + test + " --delay 0");
			System.out.println("Generating: '" + export + "'...");
			if (!engine.parseCLO()) {
				logWarning("Generating: parsing issues of command line arguments - review!");
				nTestWarnings++;
			}
			engine.setSuspended(true);
			engine.modelReset();
			// if --generation 0 then reset cleared the --run flag, i.e. engine
			// no longer in suspended mode and needs no nudge to run.
			testRunning = engine.isSuspended();
			if (!testRunning) {
				// still need to export current state
				engine.exportState();
			}
			else {
				// wait for test case generation to finish
				while (testRunning) {
					synchronized (this) {
						engine.run();
//						System.out.println("Generating: engine running, waiting to finish...");
						try {
							wait();
						} catch (InterruptedException e) {
							logMessage("Generating: resuming.");
							break;
						}
					}
				}
			}
			// compress output
			String input = exportDir.getPath() + File.separator + export;
			if (useCompression) {
				try {
					FileInputStream fis = new FileInputStream(input);
					try {
						ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(input + ".zip"));
						zos.putNextEntry(new ZipEntry(export));
						int length;
						byte[] buffer = new byte[1024];
						while ((length = fis.read(buffer)) > 0) {
							zos.write(buffer, 0, length);
						}
						zos.finish();
						zos.close();
						// delete uncompressed file
						new File(input).delete();
					} catch (Exception e) {
						logError("failed to compress to " + input + ".zip.");
						nTestFailures++;
					} finally {
						fis.close();
					}
				} catch (Exception e) {
					logError("failed to open input " + input + ".");
					nTestFailures++;
				}
				input += ".zip";
			}
			// rename export file to include model name for easier access of reports
			Path source = new File(input).toPath();
			String destname = source.getFileName().toString();
			int ext = destname.lastIndexOf(".plist");
			// if export file name doesn't have '.plist' extension simply append model name
			Path dest;
			if (ext < 0)
				dest = source.resolveSibling(destname + "-" + engine.getModel().getModelType().getKey());
			else
				dest = source.resolveSibling(
						destname.substring(0, ext) + "-" + engine.getModel().getModelType().getKey()
								+ destname.substring(ext));
			try {
				Files.move(source, dest);
			} catch (IOException e) {
				logError("failed to rename " + input + " to " + dest);
				nTestFailures++;
			}
			// check test
			test(dest.toFile());
			// need to reset export directory in case a test failed and a report was written
			engine.setExportDir(exportDir);
		}
	}

	public synchronized void generateDone() {
//		System.out.println("Engine finished.");
		engine.dumpEnd();
		testRunning = false;
		notify();
	}

	int nTests, nTestFailures, nTestMinor, nTestWarnings;

	public void test(File dir) {
		if (dir.isDirectory()) {
			File[] tests = dir.listFiles();
			Arrays.sort(tests);
			for (File test : tests)
				test(test);
			return;
		}
		// arg is file
		String parent = (dir.getParentFile() != null ? dir.getParentFile().getName() + File.separator : "");
		System.out.println("Testing: '" + parent + dir.getName() + "'...");
		Plist reference = engine.readPlist(dir.getAbsolutePath());
		if (reference == null)
			return;
		engine.unloadModule();
		String clo = (String) reference.get("CLO");
		engine.setCLO(clo);
		if (!engine.parseCLO(true)) {
			logError("Testing: parsing issues of command line arguments - skipping!");
			nTestFailures++;
			nTests++;
			return;
		}
		engine.setSuspended(true);
		engine.modelReset();
		// if --generation 0 then reset cleared the --run flag, i.e. engine
		// no longer in suspended mode and needs no nudge to run.
		testRunning = engine.isSuspended();
		// wait for test case generation to finish
		while (testRunning) {
			synchronized (this) {
				engine.run();
//				System.out.println("Testing: engine running, waiting to finish...");
				try {
					wait();
				} catch (InterruptedException e) {
					break;
				}
			}
		}
		nTests++;
		System.out.println("Testing: analyzing differences...");
		// create 'plist' of current state and compare to 'reference'
		if (verbose)
			reference.verbose();
		Plist replicate = PlistParser.parse(engine.encodeState());
		// ignore some plist entries
		int nIssues = reference.diff(replicate, Arrays.asList("Export date", "Version", "CLO"));
		// check option strings only if some tests failed
		if (reference.getNMajor() > 0) {
			String rclo = (String) reference.get("CLO");
			String mclo = (String) replicate.get("CLO");
			if (!rclo.equals(mclo)) {
				logWarning("CLO strings differ (me: " + mclo + ", ref: " + rclo + ")");
				nTestWarnings++;
			}
		}
		int nMinor = reference.getNMinor();
		String msg;
		ConsoleColors color;
		if (nIssues == 0) {
			// no diffs found
			msg = "passed!";
			color = ConsoleColors.GREEN;
		} else {
			if (nIssues == nMinor) {
				// only minor diffs found
				msg = "found " + nMinor + " minor differences (likely numerical rounding)";
				color = ConsoleColors.YELLOW;
			} else {
				// at least some serious diffs found
				msg = "found " + nIssues + " differences for options '" + clo + "'";
				if (nMinor > 0)
					msg += " (with " + nMinor + " minor numerical)";
				msg += " - review!";
				color = ConsoleColors.RED;
			}
		}
		msg = color + "Testing " + parent + dir.getName() + " " + msg + ConsoleColors.RESET;
		switch(color) {
			case GREEN:
				logOk(msg);
				break;
			case YELLOW:
				logWarning(msg);
				break;
			case RED:
				logError(msg);
				break;
			default:
				logMessage(msg);
		}
		if (nIssues > 0) {
			engine.setExportDir(reportsDir);
			if (nIssues == nMinor) {
				nTestMinor++;
				if (dumpMinor) {
					String path = dir.getName();
					// replace .plist or .plist.zip extension
					engine.exportState(path.substring(0, path.lastIndexOf(".plist")) + "-minor.plist");
				}
			} else {
				nTestFailures++;
				String path = dir.getName();
				engine.exportState(path.substring(0, path.lastIndexOf(".plist")) + "-failed.plist");
			}
		}
	}

	public synchronized void testDone() {
//		System.out.println("Engine finished.");
		testRunning = false;
		notify();
	}

	@Override
	public synchronized void modelStopped() {
		// generation or testing finished
		if (performTest)
			testDone();
		else
			generateDone();
	}

	public void run() {
		nTests = 0;
		nTestFailures = 0;
		nTestMinor = 0;
		nTestWarnings = 0;
		if (performTest) {
			test(testsDir);
			logTitle("Tests completed.");
		} else {
			generate();
			logTitle("Generating completed.");
		}
		if (nTestWarnings > 0)
			logWarning(nTestWarnings + " out of " + nTests + " tests had warnings.");
		if (nTestMinor > 0)
			logWarning(nTestMinor + " out of " + nTests
					+ " tests had minor (likely numerical) issues.");
		if (nTestFailures > 0)
			logError(nTestFailures + " out of " + nTests + " tests had errors.");
		if (nTests != nTestFailures)
			logOk((nTests - nTestFailures) + " out of " + nTests
					+ " tests successfully passed.");
		if (nTestFailures > 0 || nTestMinor > 0 || nTestWarnings > 0)
			logTitle("Review issues carefully!");
	}

	public void parse(String[] args) {
		// test suite accepts several arguments:
		// --generate <filename>: read option sets from file and generate test cases
		// --tests <directory>: directory for storing/retrieving test cases
		// --reports <directory>: directory for storing failed test reports
		// --compress: compress generated test files
		// --verb: verbose mode
		// --minor: dump differences for minor failures as well
		// --help, -h or no arguments: help screen
		reportsDir = null;
		int nArgs = args.length;
		if (nArgs == 0)
			help();
		for (int i = 0; i < nArgs; i++) {
			String arg = args[i];
			// print help and exit
			if (arg.startsWith("-h") || arg.startsWith("--help"))
				help();
			// compress generated test files
			if (arg.startsWith("--comp")) {
				useCompression = true;
				continue;
			}
			// generate test cases from file or directory
			if (arg.startsWith("--generate")) {
				if (i + 1 == nArgs) {
					logError("generate: filename missing.");
					engine.exit(1);
				}
				// ask for confirmation to proceed
				Scanner scanner = new Scanner(System.in);
				System.out.print("Do you really want to generate new reference data (yes/no): ");
				String confirmation = scanner.next();
				scanner.close();
				if (!confirmation.toLowerCase().equals("yes")) {
					logError("Generation of reference data aborted.");
					engine.exit(0);
				}
				// open file or directory
				arg = args[++i];
				generator = FileSystems.getDefault().getPath(arg).toFile();
				if (!generator.canRead()) {
					logError("generate: failed to read '" + generator.getPath() + "'");
					// e.printStackTrace(); // for debugging
					engine.exit(1);
				}
				continue;
			}
			// directory for storing/retrieving tests
			if (arg.startsWith("--test")) {
				if (i + 1 == nArgs) {
					logError("test: file/directory name missing.");
					engine.exit(1);
				}
				// check directory
				arg = args[++i];
				Path tests = FileSystems.getDefault().getPath(arg);
				if (!tests.toFile().exists()) {
					logError("test: file/directory '" + arg + "' not found.");
					engine.exit(1);
				}
				testsDir = tests.toFile();
				continue;
			}
			// directory for storing failed test reports
			if (arg.startsWith("--report")) {
				if (i + 1 == nArgs) {
					logError("report: file/directory name missing.");
					engine.exit(1);
				}
				// check directory
				arg = args[++i];
				Path reports = FileSystems.getDefault().getPath(arg);
				reportsDir = new File(reports.toFile().getPath() + File.separator + engine.getGit());
				continue;
			}
			// dump differences for minor failures
			if (arg.startsWith("--minor")) {
				dumpMinor = true;
				continue;
			}
			// verbose mode
			if (arg.startsWith("--verb")) {
				verbose = true;
				continue;
			}
		}
		if (reportsDir == null) {
			if (testsDir.isDirectory()) {
				reportsDir = new File(testsDir.getPath() + File.separator + engine.getGit());
				if (!reportsDir.exists() && !reportsDir.mkdir()) {
					logError("failed to create directory '" + reportsDir.getPath() + "' for reports.");
					engine.exit(1);
				}
			} else
				reportsDir = new File(".");
		}
		if (generator != null) {
			// for generating tests, generator file/directory must be readable and the tests
			// directory must exist and must be writable
			if (generator != null && !generator.canRead()) {
				logError("directory with generator scripts is not readable.");
				engine.exit(1);
			}
			if (testsDir == null) {
				logError("directory for generated tests missing - use --tests <directory>.");
				engine.exit(1);
			}
			if (!testsDir.canWrite()) {
				logError("directory for generated tests is not writable.");
				engine.exit(1);
			}
			testsDir = new File(testsDir.getPath() + File.separator + engine.getGit());
			if (!testsDir.exists() && !testsDir.mkdir()) {
				logError("failed to create directory '" + testsDir.getPath() + "' for tests.");
				engine.exit(1);
			}
		} else {
			// for performing test(s) either test directory must exist and must be readable
			// or test file must be provided and must be readable
			if (testFile != null && testsDir != null) {
				logError("specify either --test <file> or <dir>.");
				engine.exit(1);
			}
			if (testFile != null && !testFile.canRead()) {
				logError("test file '" + testFile.getPath() + "' not readable.");
				engine.exit(1);
			}
			if (testsDir != null && !testsDir.canRead()) {
				logError("test directory '" + testsDir.getPath() + "' not readable.");
				engine.exit(1);
			}
		}
		// for generating and testing, the directory for reports must exist and be writeable
		if (reportsDir != null && !reportsDir.exists()) {
			if (!reportsDir.mkdirs()) {
				logError("failed to create directory '" + reportsDir.getPath() + "' for reports.");
				engine.exit(1);
			}
		}
		if (reportsDir != null && !reportsDir.canWrite()) {
			logError("report directory '" + reportsDir.getPath() + "' not writable.");
			engine.exit(1);
		}
		performTest = !(generator != null);
	}

	public void help() {
		System.out.println("Usage: java -jar ./build/TestEvoLudo.jar \n" + //
				"       --generate <directory|file>: generate test files from option sets\n" + //
				"       --tests <directory>: directory for storing/retrieving test cases\n" + //
				"       --reports  <directory>: directory to store reports of failed tests" + //
				"       --compress: compress generated test files\n" + //
				"       --minor: dump differences for minor failures\n" + //
				"       --verb: verbose mode\n" + //
				"       --help, -h or no arguments: this help screen");
		engine.exit(0);
	}

	/**
	 * Entry point for test routines of EvoLudo.
	 * 
	 * @param args the string of command line arguments
	 */
	public static void main(final String[] args) {
		TestEvoLudo testSuite = new TestEvoLudo();
		testSuite.parse(args);
		testSuite.run();
		testSuite.engine.exit(0);
	}

	public void logMessage(String msg) {
		if(logger.isLoggable(Level.INFO))
			logger.info(msg);
		else
			System.out.println(msg);
	}

	public void logTitle(String msg) {
		if(logger.isLoggable(Level.INFO))
			logger.info(ConsoleColors.BLACK_BOLD + msg + ConsoleColors.RESET);
		else
			System.out.println(ConsoleColors.BLACK_BOLD + msg + ConsoleColors.RESET);
	}

	public void logOk(String msg) {
		if(logger.isLoggable(Level.INFO))
			logger.info(ConsoleColors.GREEN + msg + ConsoleColors.RESET);
		else
			System.out.println(ConsoleColors.GREEN + msg + ConsoleColors.RESET);
	}

	public void logWarning(String msg) {
		if(logger.isLoggable(Level.WARNING))
			logger.warning(ConsoleColors.YELLOW + msg + ConsoleColors.RESET);
		else
			System.out.println(ConsoleColors.YELLOW + "WARNING: " + msg + ConsoleColors.RESET);
	}

	public void logError(String msg) {
		if(logger.isLoggable(Level.SEVERE))
			logger.severe(ConsoleColors.RED + msg + ConsoleColors.RESET);
		else
			System.out.println(ConsoleColors.RED + "ERROR: " + msg + ConsoleColors.RESET);
	}

	enum ConsoleColors {
		// Color end string, color reset
		RESET("\033[0m"),

		// Regular Colors. Normal color, no bold, background color etc.
		BLACK("\033[0;30m"), // BLACK
		RED("\033[0;31m"), // RED
		GREEN("\033[0;32m"), // GREEN
		YELLOW("\033[0;33m"), // YELLOW
		BLUE("\033[0;34m"), // BLUE
		MAGENTA("\033[0;35m"), // MAGENTA
		CYAN("\033[0;36m"), // CYAN
		WHITE("\033[0;37m"), // WHITE

		// Bold
		BLACK_BOLD("\033[1;30m"), // BLACK
		RED_BOLD("\033[1;31m"), // RED
		GREEN_BOLD("\033[1;32m"), // GREEN
		YELLOW_BOLD("\033[1;33m"), // YELLOW
		BLUE_BOLD("\033[1;34m"), // BLUE
		MAGENTA_BOLD("\033[1;35m"), // MAGENTA
		CYAN_BOLD("\033[1;36m"), // CYAN
		WHITE_BOLD("\033[1;37m"), // WHITE

		// Underline
		BLACK_UNDERLINED("\033[4;30m"), // BLACK
		RED_UNDERLINED("\033[4;31m"), // RED
		GREEN_UNDERLINED("\033[4;32m"), // GREEN
		YELLOW_UNDERLINED("\033[4;33m"), // YELLOW
		BLUE_UNDERLINED("\033[4;34m"), // BLUE
		MAGENTA_UNDERLINED("\033[4;35m"), // MAGENTA
		CYAN_UNDERLINED("\033[4;36m"), // CYAN
		WHITE_UNDERLINED("\033[4;37m"), // WHITE

		// Background
		BLACK_BACKGROUND("\033[40m"), // BLACK
		RED_BACKGROUND("\033[41m"), // RED
		GREEN_BACKGROUND("\033[42m"), // GREEN
		YELLOW_BACKGROUND("\033[43m"), // YELLOW
		BLUE_BACKGROUND("\033[44m"), // BLUE
		MAGENTA_BACKGROUND("\033[45m"), // MAGENTA
		CYAN_BACKGROUND("\033[46m"), // CYAN
		WHITE_BACKGROUND("\033[47m"), // WHITE

		// High Intensity
		BLACK_BRIGHT("\033[0;90m"), // BLACK
		RED_BRIGHT("\033[0;91m"), // RED
		GREEN_BRIGHT("\033[0;92m"), // GREEN
		YELLOW_BRIGHT("\033[0;93m"), // YELLOW
		BLUE_BRIGHT("\033[0;94m"), // BLUE
		MAGENTA_BRIGHT("\033[0;95m"), // MAGENTA
		CYAN_BRIGHT("\033[0;96m"), // CYAN
		WHITE_BRIGHT("\033[0;97m"), // WHITE

		// Bold High Intensity
		BLACK_BOLD_BRIGHT("\033[1;90m"), // BLACK
		RED_BOLD_BRIGHT("\033[1;91m"), // RED
		GREEN_BOLD_BRIGHT("\033[1;92m"), // GREEN
		YELLOW_BOLD_BRIGHT("\033[1;93m"), // YELLOW
		BLUE_BOLD_BRIGHT("\033[1;94m"), // BLUE
		MAGENTA_BOLD_BRIGHT("\033[1;95m"), // MAGENTA
		CYAN_BOLD_BRIGHT("\033[1;96m"), // CYAN
		WHITE_BOLD_BRIGHT("\033[1;97m"), // WHITE

		// High Intensity backgrounds
		BLACK_BACKGROUND_BRIGHT("\033[0;100m"), // BLACK
		RED_BACKGROUND_BRIGHT("\033[0;101m"), // RED
		GREEN_BACKGROUND_BRIGHT("\033[0;102m"), // GREEN
		YELLOW_BACKGROUND_BRIGHT("\033[0;103m"), // YELLOW
		BLUE_BACKGROUND_BRIGHT("\033[0;104m"), // BLUE
		MAGENTA_BACKGROUND_BRIGHT("\033[0;105m"), // MAGENTA
		CYAN_BACKGROUND_BRIGHT("\033[0;106m"), // CYAN
		WHITE_BACKGROUND_BRIGHT("\033[0;107m"); // WHITE

		private final String code;

		ConsoleColors(String code) {
			this.code = code;
		}

		@Override
		public String toString() {
			return code;
		}
	}
}
