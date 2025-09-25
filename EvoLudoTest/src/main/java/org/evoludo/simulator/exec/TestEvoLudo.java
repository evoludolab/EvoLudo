//
// EvoLudo Project
//
// Copyright 2010-2025 Christoph Hauert
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
//	Hauert, Christoph (<year>) EvoLudo Project, https://www.evoludo.org
//			(doi: 10.5281/zenodo.14591549 [, <version>])
//
//	<year>:    year of release (or download), and
//	<version>: optional version number (as reported in output header
//			or GUI console) to simplify replication of reported results.
//
// The formatting may be adjusted to comply with publisher requirements.
//

package org.evoludo.simulator.exec;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.evoludo.simulator.EvoLudoJRE;
import org.evoludo.simulator.models.MilestoneListener;
import org.evoludo.util.Plist;
import org.evoludo.util.PlistParser;

/**
 * TestEvoLudo is a test suite for EvoLudo. It generates test cases from a
 * generator file or directory and compares the results with reference files.
 * The test suite can be run in two modes: (1) generating test cases from a
 * generator file or directory, and (2) testing the generated or existing test
 * cases. The test suite accepts the following command line options:
 * <ul>
 * <li>{@code --generate <filename>}: read option sets from file and generate
 * test cases
 * <li>{@code --tests <directory>}: directory for storing/retrieving test cases
 * (defaults to references)
 * <li>{@code --references <directory>}: directory for storing/retrieving test
 * cases
 * <li>{@code --reports <directory>}: directory for storing failed test reports
 * <li>{@code --compress}: compress generated test files
 * <li>{@code --minor}: dump differences for minor failures
 * <li>{@code --verb}: verbose mode
 * <li>{@code --help}, {@code -h} or no arguments: this help screen
 * </ul>
 * 
 * @author Christoph Hauert
 */
public class TestEvoLudo implements MilestoneListener {

	/**
	 * List of keys to exclude from test case comparisons.
	 */
	static final Collection<String> SHA_EXCLUDE = Arrays.asList("Export date", "Version", "JavaVersion", "CLO");

	/**
	 * The prefix to mark the beginning of the SHA hash in the filename.
	 */
	static final String SHA_PREFIX = "-SHA";

	/**
	 * Pointer to engine. Engine has EvoLudoJRE class but do not rely on JRE
	 * specifics.
	 */
	EvoLudoJRE engine;

	/**
	 * Logger for keeping track of and reporting events and issues.
	 */
	Logger logger;

	/**
	 * Directory containing tests or for storing generated tests.
	 */
	File testsDir;

	/**
	 * Directory for storing reports of failed tests.
	 */
	File reportsDir;

	/**
	 * Directory with reference results.
	 */
	File referencesDir;

	/**
	 * Directory with generator scripts for tests.
	 */
	File generator;

	/**
	 * The flag to indicate whether to perform tests or generate test cases.
	 */
	boolean performTest;

	/**
	 * The flag to indicate whether tests are running.
	 */
	boolean isRunning;

	/**
	 * The flag to indicate whether to use compression for the generated test files.
	 */
	boolean useCompression = false;

	/**
	 * The flag to indicate whether to dump differences for minor failures.
	 */
	boolean dumpMinor = false;

	/**
	 * The flag to indicate whether to run in verbose mode.
	 */
	boolean verbose = false;

	/**
	 * The flag to indicate whether to skip SHA checks.
	 */
	boolean skipSHA = false;

	/**
	 * The total number of tests.
	 */
	int nTests;

	/**
	 * The number of failed tests.
	 */
	int nTestFailures;

	/**
	 * The number of tests failing with minor errors.
	 */
	int nTestMinor;

	/**
	 * The number of tests with warnings.
	 */
	int nTestWarnings;

	/**
	 * Constructor for TestEvoLudo.
	 */
	public TestEvoLudo() {
		engine = new EvoLudoJRE();
		logger = engine.getLogger();
		// register as Model.MilestoneListener to receive milestone notifications
		engine.addMilestoneListener(this);
	}

	/**
	 * Generate test cases from generator file or directory.
	 */
	public void generate() {
		if (generator.isDirectory()) {
			File[] gens = generator.listFiles((dir, name) -> name.endsWith(".clo"));
			for (File f : gens)
				generate(f);
		} else {
			generate(generator);
		}
	}

	/**
	 * Generate test cases from generator file {@code clo}.
	 * 
	 * @param clo the generator file
	 */
	protected void generate(File clo) {
		String filename = clo.getName();
		// strip extension
		filename = filename.substring(0, filename.lastIndexOf(".clo"));
		File exportDir = new File(
				referencesDir.getPath() + File.separator + filename);
		if (!exportDir.exists() && !exportDir.mkdir()) {
			logWarning("Generating: failed to make directory '" + exportDir.getPath() + "' - skipped.");
			return;
		}
		int nTest = 0;
		try {
			Scanner scanner = new Scanner(clo);
			while (scanner.hasNextLine()) {
				String cloLine = scanner.nextLine().trim();
				// skip comments and empty lines
				if (cloLine.isEmpty() || cloLine.startsWith("#"))
					continue;
				// run module
				if (!runModule("Testing", cloLine)) {
					nTest++;
					continue;
				}
				// check result against references
				String result = engine.encodeState();
				Plist plist = PlistParser.parse(result);
				String dest = generateBasename(cloLine, ++nTest);
				File ref = checkReference(referencesDir, plist, dest);
				if (ref != null && ref != referencesDir)
					// check passed
					continue;
				if (ref == null) {
					// copy failed test to reports directory
					try {
						File src = search(referencesDir, dest);
						Path dst = new File(reportsDir.getPath() + File.separator +
								dest + "-old.plist" +
								(src.getName().endsWith(".zip") ? ".zip" : "")).toPath();
						Files.move(src.toPath(), dst);
					} catch (FileAlreadyExistsException e) {
						logError("file '" + reportsDir + File.separator + dest + "' already exists.");
						continue;
					} catch (IOException e) {
						logError("failed to move '" + referencesDir + File.separator + dest
								+ "' to '" + reportsDir + File.separator + dest + "'.");
						continue;
					}
				}
				dest += SHA_PREFIX + sha256(plist, SHA_EXCLUDE) + ".plist";
				// save new reference
				if (useCompression) {
					// with compression
					ref = exportDir.toPath().resolve(dest + ".zip").toFile();
					try {
						ZipOutputStream zos = new ZipOutputStream(
								new FileOutputStream(ref));
						zos.putNextEntry(new ZipEntry(dest));
						zos.write(result.getBytes());
						zos.finish();
						zos.close();
					} catch (FileNotFoundException e) {
						logError("failed to open '" + dest + "' for writing.");
						nTestFailures++;
					} catch (IOException e) {
						logError("failed to write to '" + dest + "'.");
						nTestFailures++;
					}
				} else {
					// no compression
					ref = exportDir.toPath().resolve(dest).toFile();
					try {
						FileOutputStream fos = new FileOutputStream(ref);
						fos.write(result.getBytes());
						fos.close();
					} catch (FileNotFoundException e) {
						logError("failed to open '" + dest + "' for writing.");
						nTestFailures++;
					} catch (IOException e) {
						logError("failed to write to '" + dest + "'.");
						nTestFailures++;
					}
				}
				String name = ref.getName();
				int idx = name.indexOf(SHA_PREFIX);
				logOk(name.substring(0, idx) + ": test generated/updated.");
				// check test
				test(ref);
			}
			scanner.close();
		} catch (FileNotFoundException fnfe) {
			logWarning("file '" + clo + "' not found!");
		}
	}

	/**
	 * Test all files in directory {@code dir}. This directory can either contain
	 * test files or files for generating them. In either case the test output is
	 * compared to the reference files.
	 * 
	 * @param dir the directory with test files
	 */
	public void test(File dir) {
		if (dir.isDirectory()) {
			File[] tests = dir.listFiles();
			Arrays.sort(tests);
			for (File test : tests)
				test(test);
			return;
		}
		// dir is file
		String parent = (dir.getParentFile() != null ? dir.getParentFile().getName() + File.separator : "");
		// test plist or clo files
		String filename = dir.getName();
		String ext = filename.substring(filename.lastIndexOf('.'));
		if (ext.equals(".plist") || ext.equals(".zip")) {
			String name = dir.getName();
			int idx = name.indexOf(SHA_PREFIX);
			logMessage("Testing: '" + parent + name.substring(0, idx) + "'...");
			Plist reference = engine.readPlist(dir.getAbsolutePath());
			if (reference == null)
				return;
			String clo = (String) reference.get("CLO");
			// run module
			if (runModule("Testing", clo)) {
				Plist result = PlistParser.parse(engine.encodeState());
				compareRuns(dir, reference, result);
			}
		} else if (ext.equals(".clo")) {
			// derive reference from clo string
			int nTest = 0;
			try {
				Scanner scanner = new Scanner(dir);
				while (scanner.hasNextLine()) {
					String clo = scanner.nextLine().trim();
					// skip comments and empty lines
					if (clo.isEmpty() || clo.startsWith("#"))
						continue;
					// run module
					if (!runModule("Testing", clo)) {
						nTest++;
						continue;
					}
					// check result against references
					Plist result = PlistParser.parse(engine.encodeState());
					String refname = generateBasename(clo, ++nTest);
					if (checkReference(referencesDir, result, refname) == referencesDir)
						logWarning("reference file '" + refname + "' not found - generate references first!");
				}
				scanner.close();
			} catch (FileNotFoundException fnfe) {
				logWarning("file '" + dir + "' not found!");
			}
		} else {
			// unknown extension
		}
	}

	public static String sha256(Plist plist, Collection<String> exclude) {
		HashMap<String, Object> original = new HashMap<>(plist);
		for (String key : exclude) {
			if (plist.containsKey(key)) {
				original.put(key, plist.get(key));
				plist.replace(key, null);
			}
		}
		String sha = "";
		try {
			sha = hashSHA256(plist);
		} catch (IOException | NoSuchAlgorithmException e) {
			e.printStackTrace();
		} finally {
			// restore original state
			plist.putAll(original);
		}
		return sha;
	}

	public static String hashSHA256(Object obj) throws IOException, NoSuchAlgorithmException {
		try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
				ObjectOutputStream oos = new ObjectOutputStream(bos)) {
			oos.writeObject(obj);
			oos.flush();
			byte[] objectBytes = bos.toByteArray();

			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] shaHash = digest.digest(objectBytes);
			StringBuilder sb = new StringBuilder();
			for (byte b : shaHash) {
				sb.append(String.format("%02x", b));
			}
			return sb.toString();
		}
	}

	/**
	 * Generate the base name for tests from command line options {@code clo} and
	 * index {@code idx}.
	 * 
	 * @param clo the command line options
	 * @param idx the index of the test
	 * @return the base filename
	 */
	private String generateBasename(String clo, int idx) {
		String module = engine.getModule().getKey();
		String model = engine.getModel().getType().getKey();
		int exportIdx = clo.indexOf("--export");
		if (exportIdx < 0)
			return module + "-" + idx + "-" + model;

		exportIdx += "--export".length();
		int exportEnd = clo.indexOf("--", exportIdx);
		if (exportEnd < 0)
			exportEnd = clo.length();
		String name = clo.substring(exportIdx, exportEnd).strip();
		name = name.replace(".plist", "").replace(".zip", "");
		return name;
	}

	/**
	 * Strip export option from command line options {@code clo}.
	 * 
	 * @param clo the command line options
	 * @return the stripped command line options
	 */
	private String stripExport(String clo) {
		int expidx;
		while ((expidx = clo.indexOf("--export")) >= 0) {
			String stripped = clo.substring(0, expidx).trim();
			int expendidx = clo.indexOf("--", expidx + "--export".length());
			if (expendidx >= 0)
				stripped += " " + clo.substring(expendidx);
			clo = stripped;
		}
		return clo;
	}

	/**
	 * Load and run module with parameters {@code clo}. The options {@code --seed 0}
	 * is prepended and {@code --delay 0} is appended to {@code clo}. Because for
	 * options that are specified multiple times the latter takes precedence, this
	 * ensures that the results are reproducible with potentially custom seeds and
	 * also run at full speed. Returns {@code false} if the module didn't run and
	 * hence no export was generated, even if requested with the {@code --export}
	 * option. This happens for example with {@code --timestop 0}.
	 * 
	 * @param task the task that is running (generating or testing)
	 * @param clo  the command line options for running the module
	 * @return {@code true} if the module ran successfully
	 */
	private boolean runModule(String task, String clo) {
		engine.unloadModule();
		// set command line options of test to generate
		// prepend --seed 0 to ensure fixed seed; append --delay 0 to run at full speed.
		// note, later instances of repeated options take precedence.
		engine.setCLO("--seed 0 " + stripExport(clo) + " --delay 0");
		int issues = engine.parseCLO();
		if (issues > 0) {
			logError(task + ": " + issues + " parsing issues with command line arguments - review!");
			nTestFailures++;
			nTests++;
			return false;
		}
		engine.setSuspended(true);
		// reset/run module
		engine.modelReset();
		// if --generation 0 then reset cleared the --run flag, i.e. engine
		// no longer in suspended mode and needs no nudge to save state.
		isRunning = engine.isSuspended();
		// wait for test case generation to finish
		while (isRunning) {
			synchronized (this) {
				engine.run();
				// logMessage(task + ": engine running, waiting to finish...");
				try {
					wait();
				} catch (InterruptedException e) {
					logMessage(task + ": resuming.");
					break;
				}
			}
		}
		engine.writeFooter();
		return true;
	}

	/**
	 * Compare reference output {@code reference} with the output of the test
	 * {@code replicate} and generate a report if differences are found. The
	 * reference is stored in {@code refname}. The method returns {@code true} if
	 * the test passed.
	 * 
	 * @param refname   the name of the reference file
	 * @param reference the reference {@code Plist}
	 * @param replicate the replicate {@code Plist}
	 * @return {@code true} if the test passed
	 */
	private boolean compareRuns(File refname, Plist reference, Plist replicate) {
		nTests++;
		logMessage("Testing: analyzing differences...");
		// create 'plist' of current state and compare to 'reference'
		if (verbose)
			replicate.verbose();
		// ignore some plist entries
		int nIssues = replicate.diff(reference, SHA_EXCLUDE);
		// check option strings only if some tests failed
		if (replicate.getNMajor() > 0) {
			String rclo = (String) reference.get("CLO");
			String mclo = (String) replicate.get("CLO");
			if (!rclo.equals(mclo)) {
				logWarning("CLO strings differ!\n me: " + mclo + "\nref: " + rclo + ")");
				nTestWarnings++;
			}
		}
		int nMinor = replicate.getNMinor();
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
				if (replicate.failfast()) {
					msg = "failed";
				} else {
					msg = "found " + nIssues + " differences for options '" + replicate.get("CLO") + "'";
					if (nMinor > 0)
						msg += " (with " + nMinor + " minor numerical)";
				}
				msg += " - review!";
				color = ConsoleColors.RED;
			}
		}
		String name = refname.getName();
		int idx = name.indexOf(SHA_PREFIX);
		msg = color + "Testing " + name.substring(0, idx) + " " + msg;
		switch (color) {
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
		if (nIssues == 0)
			return true;
		if (replicate.failfast())
			return false;
		String javaref = (String) reference.get("JavaVersion");
		String javarep = (String) replicate.get("JavaVersion");
		if (javaref != null && !javaref.equals(javarep))
			logWarning("JRE versions differ (me: " + javarep + ", ref: " + javaref + ")");
		String ext;
		String report = reportsDir.getAbsolutePath() + File.separator + refname.getName();
		if (nIssues == nMinor) {
			nTestMinor++;
			if (!dumpMinor)
				return false;
			ext = "-minor.plist";
		} else {
			nTestFailures++;
			ext = "-failed.plist";
		}
		engine.exportState(report.substring(0, report.lastIndexOf(".plist")) + ext);
		return false;
	}

	/**
	 * Search for reference file with the name base {@code refbasename} (no SHA, no
	 * extensions) in directory {@code references}. If {@code refbasename} is found
	 * verify the results in the plist {@code result}.
	 * 
	 * @param references  the directory with reference files
	 * @param result      the {@code Plist} with the results
	 * @param refbasename the base name of the reference file (if it exists)
	 * @return if verification successful return {@code File} pointing to reference;
	 *         if unsuccesssful return {@code null}; and if reference file not found
	 *         return {@code references}
	 */
	private File checkReference(File references, Plist result, String refbasename) {
		File ref = search(references, refbasename);
		if (ref == null) {
			// no reference file found
			return references;
		}
		String name = ref.getName();
		int idx = name.indexOf(SHA_PREFIX);
		if (!skipSHA || idx < 0) {
			// skip SHA check or no SHA in name
			String shaReference = name.substring(idx + SHA_PREFIX.length()).replace(".zip", "").replace(".plist", "");
			String shaResult = sha256(result, SHA_EXCLUDE);
			if (shaResult.equals(shaReference)) {
				nTests++;
				logOk("Testing: " + name.substring(0, idx) + " SHA match!");
				return ref;
			}
			logWarning("Testing: " + name.substring(0, idx) + " SHA mismatch! - comparing plists...");
		}
		// compare plists
		Plist reference;
		try {
			reference = engine.readPlist(ref.getAbsolutePath());
		} catch (Exception e) {
			// failed to read reference file
			nTests++;
			nTestFailures++;
			logError("Testing: failed to read reference file '" + ref.getAbsolutePath() + "'.");
			return null;
		}
		if (compareRuns(ref, reference, result))
			return ref;
		return null;
	}

	/**
	 * Recursively search for a file whose name starts with {@code search} in
	 * directory {@code file}.
	 * 
	 * @param file   the directory to search
	 * @param search the name of the file to search for
	 * @return the file if found, otherwise {@code null}
	 */
	private static File search(File file, String search) {
		if (file.isDirectory()) {
			for (File f : file.listFiles()) {
				File hit = search(f, search);
				if (hit != null)
					return hit;
			}
		}
		String name = file.getName();
		int idx = name.indexOf(SHA_PREFIX);
		if (idx >= 0)
			name = name.substring(0, idx);
		if (name.equals(search))
			return file;
		return null;
	}

	@Override
	public synchronized void modelStopped() {
		isRunning = false;
		notify();
	}

	/**
	 * Generate or run all tests.
	 * 
	 * @return {@code true} if successful
	 */
	public boolean run() {
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
		// remove reports directory if empty
		reportsDir.delete();
		return (nTestFailures == 0);
	}

	/**
	 * Parse the command line options.
	 * 
	 * @param args the command line options
	 */
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
				System.out.print("Do you really want to generate new reference data (yes/No): ");
				String confirmation = scanner.nextLine();
				scanner.close();
				if (!confirmation.equalsIgnoreCase("yes")) {
					logError("Generation of reference data aborted.");
					engine.exit(-1);
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
			// directory for retrieving references
			if (arg.startsWith("--ref")) {
				if (i + 1 == nArgs) {
					logError("references: references file/directory name missing.");
					engine.exit(1);
				}
				// check directory
				arg = args[++i];
				Path refs = FileSystems.getDefault().getPath(arg);
				if (!refs.toFile().exists()) {
					logError("references: file/directory '" + arg + "' not found.");
					engine.exit(1);
				}
				referencesDir = refs.toFile();
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
				reportsDir = new File(reports.toFile().getPath());
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
			// skip SHA checks
			if (arg.startsWith("--nosha")) {
				skipSHA = true;
			}
		}
		if (testsDir == null)
			testsDir = referencesDir;
		if (referencesDir == null)
			referencesDir = testsDir;
		if (reportsDir == null) {
			if (testsDir.isDirectory()) {
				reportsDir = new File(testsDir.getPath());
				if (!reportsDir.exists() && !reportsDir.mkdir()) {
					logError("failed to create directory '" + reportsDir.getPath() + "' for reports.");
					engine.exit(1);
				}
			} else
				reportsDir = new File(".");
		}
		if (generator != null) {
			// for generating reference tests, generator file/directory must be readable and
			// the reference directory must exist and must be writable
			if (generator != null && !generator.canRead()) {
				logError("directory with generator scripts is not readable.");
				engine.exit(1);
			}
			if (referencesDir == null) {
				logError("directory for generated references missing - use --references <directory>.");
				engine.exit(1);
			}
			if (!referencesDir.canWrite()) {
				logError("directory for generated references is not writable.");
				engine.exit(1);
			}
		} else {
			// for testing the directory to the reference files must exist and be readable
			if (referencesDir == null) {
				logError("no references found. use --references <file|dir>.");
				engine.exit(1);
			}
			if (!referencesDir.canRead()) {
				logError("reference directory '" + referencesDir.getPath() + "' not readable.");
				engine.exit(1);
			}
			if (testsDir == null) {
				logError("no tests found. use --tests <file|dir> or --references <file|dir>.");
				engine.exit(1);
			}
			if (!testsDir.canRead()) {
				logError("test/reference directory '" + testsDir.getPath() + "' not readable.");
				engine.exit(1);
			}
		}
		// for generating and testing, the directory for reports must exist and be
		// writeable
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
		performTest = (generator == null);
	}

	/**
	 * Print help screen.
	 */
	public void help() {
		System.out.println(
				"EvoLudo tests: " + engine.getVersion() + //
						"\nUsage: java -jar TestEvoLudo.jar with options\n" + //
						"       --generate <directory|file>: generate test files from option sets\n" + //
						"       --tests <directory|file>: test files (defaults to references)\n" + //
						"       --references <directory>: directory for storing/retrieving test cases\n" + //
						"       --reports  <directory>: directory to store reports of failed tests\n" + //
						"       --compress: compress generated test files\n" + //
						"       --minor: dump differences for minor failures\n" + //
						"       --verb: verbose mode\n" + //
						"       --nosha: disable SHA checks\n" + //
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
		boolean success = testSuite.run();
		testSuite.engine.exit(success ? 0 : 2);
	}

	/**
	 * Log message to console or logger.
	 * 
	 * @param msg the message to log
	 */
	public void logMessage(String msg) {
		if (logger.isLoggable(Level.INFO))
			logger.info(msg);
		else
			System.out.println(msg);
	}

	/**
	 * Log bold message to console or logger.
	 * 
	 * @param msg the message to log
	 */
	public void logTitle(String msg) {
		if (logger.isLoggable(Level.INFO))
			logger.info(ConsoleColors.BLACK_BOLD + msg + ConsoleColors.RESET);
		else
			System.out.println(ConsoleColors.BLACK_BOLD + msg + ConsoleColors.RESET);
	}

	/**
	 * Log message in green color to console or logger.
	 * 
	 * @param msg the message to log
	 */
	public void logOk(String msg) {
		if (logger.isLoggable(Level.INFO))
			logger.info(ConsoleColors.GREEN + msg + ConsoleColors.RESET);
		else
			System.out.println(ConsoleColors.GREEN + msg + ConsoleColors.RESET);
	}

	/**
	 * Log warning message in yellow color to console or logger.
	 * 
	 * @param msg the warning to log
	 */
	public void logWarning(String msg) {
		if (logger.isLoggable(Level.WARNING))
			logger.warning(ConsoleColors.YELLOW + msg + ConsoleColors.RESET);
		else
			System.out.println(ConsoleColors.YELLOW + "WARNING: " + msg + ConsoleColors.RESET);
	}

	/**
	 * Log error message in red color to console or logger.
	 * 
	 * @param msg the error to log
	 */
	public void logError(String msg) {
		if (logger.isLoggable(Level.SEVERE))
			logger.severe(ConsoleColors.RED + msg + ConsoleColors.RESET);
		else
			System.out.println(ConsoleColors.RED + "ERROR: " + msg + ConsoleColors.RESET);
	}

	/**
	 * The control codes for changing the style of the console output.
	 */
	enum ConsoleColors {

		/**
		 * Reset all styling.
		 */
		RESET("\033[0m"),

		/**
		 * Black color. Regular font, no background.
		 */
		BLACK("\033[0;30m"),

		/**
		 * Red color. Regular font, no background.
		 */
		RED("\033[0;31m"),

		/**
		 * Green color. Regular font, no background.
		 */
		GREEN("\033[0;32m"),

		/**
		 * Yellow color. Regular font, no background.
		 */
		YELLOW("\033[0;33m"),

		/**
		 * Blue color. Regular font, no background.
		 */
		BLUE("\033[0;34m"),

		/**
		 * Magenta color. Regular font, no background.
		 */
		MAGENTA("\033[0;35m"),

		/**
		 * Cyan color. Regular font, no background.
		 */
		CYAN("\033[0;36m"),

		/**
		 * White color. Regular font, no background.
		 */
		WHITE("\033[0;37m"),

		/**
		 * Black color. Bold font, no background.
		 */
		BLACK_BOLD("\033[1;30m"),

		/**
		 * Red color. Bold font, no background.
		 */
		RED_BOLD("\033[1;31m"),

		/**
		 * Green color. Bold font, no background.
		 */
		GREEN_BOLD("\033[1;32m"),

		/**
		 * Yellow color. Bold font, no background.
		 */
		YELLOW_BOLD("\033[1;33m"),

		/**
		 * Blue color. Bold font, no background.
		 */
		BLUE_BOLD("\033[1;34m"),

		/**
		 * Magenta color. Bold font, no background.
		 */
		MAGENTA_BOLD("\033[1;35m"),

		/**
		 * Cyan color. Bold font, no background.
		 */
		CYAN_BOLD("\033[1;36m"),

		/**
		 * White color. Bold font, no background.
		 */
		WHITE_BOLD("\033[1;37m"),

		/**
		 * Black color. Underlined, no background.
		 */
		BLACK_UNDERLINED("\033[4;30m"),

		/**
		 * Red color. Underlined, no background.
		 */
		RED_UNDERLINED("\033[4;31m"),

		/**
		 * Green color. Underlined, no background.
		 */
		GREEN_UNDERLINED("\033[4;32m"),

		/**
		 * Yellow color. Underlined, no background.
		 */
		YELLOW_UNDERLINED("\033[4;33m"),

		/**
		 * Blue color. Underlined, no background.
		 */
		BLUE_UNDERLINED("\033[4;34m"),

		/**
		 * Magenta color. Underlined, no background.
		 */
		MAGENTA_UNDERLINED("\033[4;35m"),

		/**
		 * Cyan color. Underlined, no background.
		 */
		CYAN_UNDERLINED("\033[4;36m"),

		/**
		 * White color. Underlined, no background.
		 */
		WHITE_UNDERLINED("\033[4;37m"),

		/**
		 * Black background color.
		 */
		BLACK_BACKGROUND("\033[40m"),

		/**
		 * Red background color.
		 */
		RED_BACKGROUND("\033[41m"),

		/**
		 * Green background color.
		 */
		GREEN_BACKGROUND("\033[42m"),

		/**
		 * Yellow background color.
		 */
		YELLOW_BACKGROUND("\033[43m"),

		/**
		 * Blue background color.
		 */
		BLUE_BACKGROUND("\033[44m"),

		/**
		 * Magenta background color.
		 */
		MAGENTA_BACKGROUND("\033[45m"),

		/**
		 * Cyan background color.
		 */
		CYAN_BACKGROUND("\033[46m"),

		/**
		 * White background color.
		 */
		WHITE_BACKGROUND("\033[47m"),

		/**
		 * Black high intensity color. Regular font, no background.
		 */
		BLACK_BRIGHT("\033[0;90m"),

		/**
		 * Red high intensity color. Regular font, no background.
		 */
		RED_BRIGHT("\033[0;91m"),

		/**
		 * Green high intensity color. Regular font, no background.
		 */
		GREEN_BRIGHT("\033[0;92m"),

		/**
		 * Yellow high intensity color. Regular font, no background.
		 */
		YELLOW_BRIGHT("\033[0;93m"),

		/**
		 * Blue high intensity color. Regular font, no background.
		 */
		BLUE_BRIGHT("\033[0;94m"),

		/**
		 * Magenta high intensity color. Regular font, no background.
		 */
		MAGENTA_BRIGHT("\033[0;95m"),

		/**
		 * Cyan high intensity color. Regular font, no background.
		 */
		CYAN_BRIGHT("\033[0;96m"),

		/**
		 * White high intensity color. Regular font, no background.
		 */
		WHITE_BRIGHT("\033[0;97m"),

		/**
		 * Black high intensity color. Bold font, no background.
		 */
		BLACK_BOLD_BRIGHT("\033[1;90m"),

		/**
		 * Red high intensity color. Bold font, no background.
		 */
		RED_BOLD_BRIGHT("\033[1;91m"),

		/**
		 * Green high intensity color. Bold font, no background.
		 */
		GREEN_BOLD_BRIGHT("\033[1;92m"),

		/**
		 * Yellow high intensity color. Bold font, no background.
		 */
		YELLOW_BOLD_BRIGHT("\033[1;93m"),

		/**
		 * Blue high intensity color. Bold font, no background.
		 */
		BLUE_BOLD_BRIGHT("\033[1;94m"),

		/**
		 * Magenta high intensity color. Bold font, no background.
		 */
		MAGENTA_BOLD_BRIGHT("\033[1;95m"),

		/**
		 * Cyan high intensity color. Bold font, no background.
		 */
		CYAN_BOLD_BRIGHT("\033[1;96m"),

		/**
		 * White high intensity color. Bold font, no background.
		 */
		WHITE_BOLD_BRIGHT("\033[1;97m"),

		/**
		 * Black high intensity background.
		 */
		BLACK_BACKGROUND_BRIGHT("\033[0;100m"),

		/**
		 * Red high intensity background.
		 */
		RED_BACKGROUND_BRIGHT("\033[0;101m"),

		/**
		 * Green high intensity background.
		 */
		GREEN_BACKGROUND_BRIGHT("\033[0;102m"),

		/**
		 * Yellow high intensity background.
		 */
		YELLOW_BACKGROUND_BRIGHT("\033[0;103m"),

		/**
		 * Blue high intensity background.
		 */
		BLUE_BACKGROUND_BRIGHT("\033[0;104m"),

		/**
		 * Magenta high intensity background.
		 */
		MAGENTA_BACKGROUND_BRIGHT("\033[0;105m"),

		/**
		 * Cyan high intensity background.
		 */
		CYAN_BACKGROUND_BRIGHT("\033[0;106m"),

		/**
		 * White high intensity background.
		 */
		WHITE_BACKGROUND_BRIGHT("\033[0;107m");

		/**
		 * The style code.
		 */
		private final String code;

		/**
		 * Constructor for ConsoleColors.
		 * 
		 * @param code the style code
		 */
		ConsoleColors(String code) {
			this.code = code;
		}

		@Override
		public String toString() {
			return code;
		}
	}
}
