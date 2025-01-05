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

package org.evoludo.simulator;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.Properties;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipInputStream;

import javax.swing.Timer;

import org.evoludo.graphics.Network2DJRE;
import org.evoludo.math.ArrayMath;
import org.evoludo.simulator.models.FixationData;
import org.evoludo.simulator.models.IBS;
import org.evoludo.simulator.models.IBSC;
import org.evoludo.simulator.models.IBSD;
import org.evoludo.simulator.models.IBSDPopulation;
import org.evoludo.simulator.models.IBSMCPopulation;
import org.evoludo.simulator.models.IBSPopulation;
import org.evoludo.simulator.models.Mode;
import org.evoludo.simulator.models.Model;
import org.evoludo.simulator.models.PDERD;
import org.evoludo.simulator.models.PDESupervisor;
import org.evoludo.simulator.models.PDESupervisorJRE;
import org.evoludo.simulator.modules.Module;
import org.evoludo.simulator.modules.Traits;
import org.evoludo.simulator.views.MultiView;
import org.evoludo.util.CLOParser;
import org.evoludo.util.CLOption;
import org.evoludo.util.CLOption.CLODelegate;
import org.evoludo.util.Formatter;
import org.evoludo.util.Plist;
import org.evoludo.util.PlistParser;

/**
 * JRE specific implementation of EvoLudo controller.
 * 
 * @author Christoph Hauert
 */
public class EvoLudoJRE extends EvoLudo implements Runnable {

	/**
	 * <code>true</code> when running as JRE application.
	 */
	public boolean isApplication = true;

	/**
	 * Store time to measure execution times since instantiation.
	 */
	private final long startmsec = System.currentTimeMillis();

	@Override
	public int elapsedTimeMsec() {
		return (int) (System.currentTimeMillis() - startmsec);
	}

	/**
	 * The pacemaker for running EvoLudo.
	 */
	Timer timer = null;

	/**
	 * The engine thread. This thread is responsible for running the model.
	 */
	Thread engineThread = null;

	/**
	 * The command execution thread. This thread is responsible for executing
	 * commands.
	 */
	Thread executeThread = null; // command execution thread

	public EvoLudoJRE() {
		this(true);
	}

	/**
	 * Constructor for JRE application. This constructor is used when running
	 * EvoLudo as a JRE application.
	 */
	public EvoLudoJRE(boolean loadModules) {
		super(loadModules);
		// allocate a coalescing timer for poking the engine in regular intervals
		// note: timer needs to be ready before parsing command line options
		timer = new Timer(0, new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent evt) {
				poke();
			}
		});
		launchEngine();
		// add modules that require JRE (e.g. due to libraries)
		if (loadModules)
			addModule(new Traits(this));
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * JRE uses stderr to report progress
	 */
	@Override
	public void logProgress(String msg) {
		// string of 80 spaces to
		final String filler = "                                                                                \r";
		String padding = filler.substring(filler.length() - msg.length());
		System.err.printf(msg + padding);
	}

	@Override
	public void execute(Directive directive) {
		executeThread = new Thread(new Runnable() {
			@Override
			public void run() {
				directive.execute();
			}
		}, "Execute");
		executeThread.start();
	}

	/**
	 * Launches the engine in a separate thread. Does nothing if the engine is
	 * already running.
	 */
	protected void launchEngine() {
		if (engineThread != null)
			return;
		engineThread = new Thread(this, "Engine");
		engineThread.start();
	}

	@Override
	public PDESupervisor hirePDESupervisor(PDERD charge) {
		return new PDESupervisorJRE(this, charge);
	}

	@Override
	public Network2D createNetwork2D(Geometry geometry) {
		return new Network2DJRE(this, geometry);
	}

	@Override
	public Network3D createNetwork3D(Geometry geometry) {
		return null;
	}

	@Override
	public void setDelay(int delay) {
		super.setDelay(delay);
		timer.setDelay(delay);
		// if delay is more that APP_MIN_DELAY keep timer running or restart it
		if (!timer.isRunning() && delay > 1 && isRunning) {
			timer.start();
			return;
		}
		// if not don't bother with timer - run, run, run as fast as you can
		if (timer.isRunning() && delay <= 1) {
			timer.stop();
			poke();
		}
	}

	/**
	 * Poke waiting thread to resume execution.
	 */
	public void poke() {
		if (isWaiting) {
			synchronized (this) {
				isWaiting = false;
				notify();
			}
		}
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * The <code>run()</code> method is double booked: first to start running the
	 * EvoLudo model and second to implements the {@link Runnable} interface for
	 * starting the engine in a separate thread. The two tasks can be easily triaged
	 * because requests to start running are always issued from the Event Dispatch
	 * Thread (EDT) while the engine is running in a thread named
	 * <code>Engine</code>.
	 */
	@Override
	public void run() {
		Thread me = Thread.currentThread();
		if (!me.getName().equals("Engine")) {
			if (isRunning)
				return;
			fireModelRunning();
			// this is the EDT, check if engine thread alive and kicking
			if (engineThread == null) {
				logger.severe("engine crashed. resetting and relaunching.");
				modelReset();
				launchEngine();
			}
			// if delay is more that APP_MIN_DELAY set timer
			// if not don't bother with timer - run, run, run as fast as you can
			if (delay > 1)
				timer.start();
			else
				poke();
			return;
		}
		// this is the engine thread, start waiting for tasks
		isWaiting = true;
		me.setPriority(Thread.MIN_PRIORITY);
		while (true) {
			// Possible optimization:
			// to reduce waiting times the next state should be calculated while waiting but
			// this makes synchronization with frontend much more difficult - engine would
			// probably have to keep a copy of the relevant data - currently the views keep
			// this copy... if the engine took care of that it would be enough to pass a
			// reference to the views!
			while (isWaiting) {
				synchronized (this) {
					try {
						wait();
					} catch (InterruptedException e) {
					}
				}
			}
			// woke up; calculate next step
			try {
				modelNext();
			} catch (Error e) {
				// we do not really catch this error but rather spread the word...
				// the error will be reported in the console anyways, therefore only check
				// we have a front-end and if so raise alert.
				logger.severe("Engine crashed: " + e.getMessage());
				throw e;
			}
			if (!isRunning) {
				timer.stop();
				isWaiting = true;
				continue;
			}
			// wait only if delay is more that APP_MIN_DELAY
			isWaiting = (delay > 1);
		}
	}

	@Override
	public void next() {
		poke();
	}

	@Override
	public void modelStopped() {
		timer.stop();
		isWaiting = true;
		if (simulationRunning) {
			simulationRunning = false;
			notify();
		}
	}

	@Override
	public void modelDidInit() {
		timer.stop();
		isWaiting = true;
	}

	@Override
	public void modelDidReset() {
		timer.stop();
		isWaiting = true;
	}

	/**
	 * The flag to indicate whether a simulation is running. Simulations are running
	 * in headless mode.
	 */
	boolean simulationRunning = false;

	/**
	 * Run custom module {@code module} with command line options {@code args}.
	 * 
	 * @param module the custom module to run
	 * @param args   the command line options
	 */
	public void custom(Module module, String[] args) {
		isApplication = false;
		System.setProperty("java.awt.headless", "true");
		// prepend --module option (in case not specified)
		args = ArrayMath.merge(new String[] {"--module", module.getKey()}, args);
		// EvoLudo has its own parser for command line options and expects a single string
		setCLO(Formatter.format(args, " "));
		addModule(module);
		addCLOProvider(module);
		// parse options
		parseCLO();
		// reset model to check and apply all parameters
		modelReset();
		// register hook to dump state when receiving SIGINT
		registerHook();
		// run simulation
		module.run();
		// close output stream if needed
		PrintStream out = getOutput();
		if (!out.equals(System.out))
			out.close();
		exit(0);
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * This handles the following scenarios:
	 * <ol>
	 * <li>If the manifest file in the jar includes the {@code Engine-Class}
	 * attribute, the methods attempts to dynamically allocate the specified class
	 * and run customized simulations.
	 * <li>If the command line options {@code args} include the {@code --export}
	 * option, simulations are run and the final state is dumped to the specified
	 * file.
	 * <li>If the command line options {@code args} include the {@code --data}
	 * option with valid types, simulations are run and results reported as
	 * requested. Data type requests that are incompatible with the active
	 * {@link Module} or {@link Model} are rejected when parsing command line
	 * option.
	 * <li>Only if none of the above applies the control is returned to the caller.
	 * </ol>
	 * 
	 * @see #cloExport
	 * @see #cloData
	 */
	@Override
	public void simulation() {
		String[] args = getSplitCLO();
		// parse options
		if (!parseCLO(args)) {
			// problems parsing command line options
			// return control to caller.
			return;
		}
		if (cloExport.isSet()) {
			// export option provided: run model and dump state.
			setDelay(1);
			setSuspended(true);
			modelReset();
			simulationRunning = true;
			while (simulationRunning) {
				synchronized (this) {
					run();
					try {
						wait();
					} catch (InterruptedException e) {
						break;
					}
				}
			}
			exportState();
			dumpEnd();
			exit(0);
		}
		if (dataTypes == null || dataTypes.isEmpty()) {
			// no data types provided for simulation output
			// return control to caller.
			return;
		}
		// prepare to run simulations
		Module module = getModule();
		Model model = getModel();
		// request mode based on data types (only one mode allowed and ensured by option parser)
		Mode mode = (isDynamicsDataType(dataTypes.get(0)) ? 
				Mode.DYNAMICS : Mode.STATISTICS_SAMPLE);
		if (!model.requestMode(mode)) {
			// mode not supported
			logger.info("Mode " + mode + " not supported!");
			return;
		}
		// reset model to check and apply all parameters
		modelReset();
		// register hook to dump state when receiving SIGINT
		registerHook();
		// allocate storage and initialize helper variables
		int totTraits = 0;
		boolean isContinuous = model.isContinuous();
		for (Module specie : module.getSpecies()) {
			int nt = specie.getNTraits();
			totTraits += nt;
		}
		double[] meantrait = isContinuous ? new double[2 * totTraits] : new double[totTraits];
		double[] meanfit = isContinuous ? new double[2 * totTraits] : new double[totTraits + 1];
		String[] traitNames = new String[totTraits];
		int offset = 0;
		for (Module specie : module.getSpecies()) {
			String[] tnames = specie.getTraitNames();
			int nt = specie.getNTraits();
			System.arraycopy(tnames, 0, traitNames, offset, nt);
			offset += nt;
		}
		// helper variables for statistics
		double[][] fixProb = new double[0][];
		double[][] fixUpdate = new double[0][];
		double[][] fixTime = new double[0][];
		double[] fixTotUpdate = new double[0];
		double[] fixTotTime = new double[0];
		int nTraits = -1;
		int nPopulation = -1;
		long nSamples = (long) model.getNSamples();
		long samples = 0L;
		// print settings
		dumpParameters();
		// print data legend (for dynamical reports) and initialize variables (for
		// statistics reports)
		for (MultiView.DataTypes data : dataTypes) {
			switch (data) {
				case MEAN:
					output.println("# time,\t" + data.getKey() + ",\t" + Formatter.format(traitNames)
							+ (isContinuous ? ", sdev" : ""));
					break;
				case FITMEAN:
					output.println("# time,\t" + data.getKey() + ",\t" + Formatter.format(traitNames)
							+ (isContinuous ? ", sdev" : ", Population"));
					break;
				case TRAITS:
					output.println("# time,\t" + data.getKey() + (module.getNSpecies() > 1 ? "\tSpecies" : "")
							+ ",\tTraits of all Individuals");
					break;
				case SCORES:
					output.println("# time,\t" + data.getKey() + (module.getNSpecies() > 1 ? "\tSpecies" : "")
							+ ",\tScores of all Individuals");
					break;
				case FITNESS:
					output.println("# time,\t" + data.getKey() + (module.getNSpecies() > 1 ? "\tSpecies" : "")
							+ ",\tFitness of all Individuals");
					break;
				// case FITHISTOGRAM:
				// break;
				// case HISTOGRAM:
				// break;
				// case STRUCTURE:
				// this is tedious because of interaction and competition geometries as well as
				// directed graphs with for incoming, outgoing and total degrees, which results
				// in up to 6 distributions
				// output.println("# time,\t" + data.getKey() + (module.getNSpecies() > 1 ?
				// "\tSpecies" : "") + ",\tDegree distribution of population structure");
				// break;
				case STAT_PROB:
					nTraits = module.getNTraits();
					nPopulation = module.getNPopulation();
					fixProb = new double[nPopulation][nTraits + 1];
					break;
				case STAT_UPDATES:
					nTraits = module.getNTraits();
					nPopulation = module.getNPopulation();
					fixUpdate = new double[nPopulation][9];
					fixTotUpdate = new double[9];
					break;
				case STAT_TIMES:
					nTraits = module.getNTraits();
					nPopulation = module.getNPopulation();
					fixTime = new double[nPopulation][9];
					fixTotTime = new double[9];
					break;
				default:
					output.println("# data output for " + data.getKey() + " not (yet) supported!");
					return;
			}
		}
		// run simulation
		switch (model.getMode()) {

			case DYNAMICS:
				boolean cont = true;
				while (true) {
					String time = Formatter.format(model.getTime(), dataDigits);
					// report dynamical data
					for (MultiView.DataTypes data : dataTypes) {
						switch (data) {
							case MEAN:
								model.getMeanTraits(meantrait);
								output.println(time + ",\t" + data.getKey() + ",\t"
										+ Formatter.format(meantrait, dataDigits));
								break;
							case FITMEAN:
								model.getMeanFitness(meanfit);
								output.println(time + ",\t" + data.getKey() + ",\t"
										+ Formatter.format(meanfit, dataDigits));
								break;
							case TRAITS:
								if (model instanceof IBSD) {
									boolean isMultispecies = (module.getNSpecies() > 1);
									for (Module mod : module.getSpecies()) {
										IBSDPopulation pop = (IBSDPopulation) mod.getIBSPopulation();
										output.println(time + ",\t" + data.getKey()
												+ (isMultispecies ? "\t" + mod.getName() : "") + ",\t"
												+ pop.getTraits());
									}
									break;
								}
								if (model instanceof IBSC) {
									boolean isMultispecies = (module.getNSpecies() > 1);
									for (Module mod : module.getSpecies()) {
										IBSMCPopulation pop = (IBSMCPopulation) mod.getIBSPopulation();
										output.println(time + ",\t" + data.getKey()
												+ (isMultispecies ? "\t" + mod.getName() : "") + ",\t"
												+ pop.getTraits(dataDigits));
									}
									break;
								}
								if (model instanceof PDERD) {
									output.println("How to best report trait distribution in PDE?");
									break;
								}
								throw new Error("This never happens.");
							case SCORES:
								if (model instanceof IBS) {
									boolean isMultispecies = (module.getNSpecies() > 1);
									for (Module mod : module.getSpecies()) {
										IBSPopulation pop = mod.getIBSPopulation();
										output.println(time + ",\t" + data.getKey()
												+ (isMultispecies ? "\t" + mod.getName() : "") + ",\t"
												+ pop.getScores(dataDigits));
									}
									break;
								}
								if (model instanceof PDERD) {
									output.println("How to best report score distribution in PDE?");
									break;
								}
								throw new Error("This never happens.");
							case FITNESS:
								if (model instanceof IBS) {
									boolean isMultispecies = (module.getNSpecies() > 1);
									for (Module mod : module.getSpecies()) {
										IBSPopulation pop = mod.getIBSPopulation();
										output.println(time + ",\t" + data.getKey()
												+ (isMultispecies ? "\t" + mod.getName() : "") + ",\t"
												+ pop.getFitness(dataDigits));
									}
									break;
								}
								if (model instanceof PDERD) {
									output.println("How to best report fitness distribution in PDE?");
									break;
								}
								throw new Error("This never happens.");
							// case FITHISTOGRAM:
							// break;
							// case HISTOGRAM:
							// break;
							// case STRUCTURE:
							// break;
							default:
								output.println("# dynamics output for " + data.getKey() + " not supported!");
						}
					}
					double timeStop = model.getTimeStop();
					if (!cont || (timeStop > 0.0 && model.getTime() > timeStop))
						break;
					cont = modelNext();
				}
				break;

			case STATISTICS_SAMPLE:
				// perform statistics
				if (cloSeed.isSet()) {
					// initial state set. now clear seed to obtain reproducible statistics
					// rather just a single data point repeatedly
					rng.clearRNGSeed();

				}
				isRunning = true;
				while (isRunning) {
					FixationData fixData = generateSample();
					samples++;
					boolean mutantFixed = (fixData.typeFixed == fixData.mutantTrait);
					for (MultiView.DataTypes data : dataTypes) {
						switch (data) {
							case STAT_PROB:
								double[] node = fixProb[fixData.mutantNode];
								node[(mutantFixed ? 0 : 1)]++;
								node[nTraits]++;
								fixData.probRead = true;
								break;
							case STAT_UPDATES:
								updateMeanVar(fixUpdate[fixData.mutantNode], mutantFixed,
										fixData.updatesFixed);
								updateMeanVar(fixTotUpdate, mutantFixed, fixData.updatesFixed);
								fixData.timeRead = true;
								break;
							case STAT_TIMES:
								updateMeanVar(fixTime[fixData.mutantNode], mutantFixed,
										fixData.timeFixed);
								updateMeanVar(fixTotTime, mutantFixed, fixData.timeFixed);
								fixData.timeRead = true;
								break;
							default:
								throw new Error("Statistics for " + data.getKey() + " not supported!");
						}
					}
					isRunning = (samples < nSamples);
				}
				// report statistics
				int nFailed = model.getNStatisticsFailed();
				FixationData fixData = model.getFixationData();
				for (MultiView.DataTypes data : dataTypes) {
					switch (data) {
						case STAT_PROB:
							output.println("# index,\t" + data.getKey() + (module.getNSpecies() > 1 ? "\tSpecies" : "")
									+ ",\tFixation probability of single " + traitNames[fixData.mutantTrait]
									+ " in " + traitNames[fixData.residentTrait]
									+ " populations for all locations with samples");
							double meanFix = 0.0;
							double varFix = 0.0;
							for (int n = 0; n < nPopulation; n++) {
								double[] node = fixProb[n];
								double norm = node[nTraits];
								if (norm <= 0.0)
									continue;	// no samples for node n
								double inorm = 1.0 / norm;
								double n0 = node[0] * inorm;
								double n1 = node[1] * inorm;
								output.println(n + ",\t"
										+ Formatter.format(n0, dataDigits) + ", " // mutant fixation
										+ Formatter.format(n1, dataDigits) + ", " // resident fixation
										+ Formatter.format(norm, 0)); // sample size
								double dx = n0 - meanFix;
								meanFix += dx / (n + 1);
								varFix += dx * (n0 - meanFix);
							}
							// trick: to avoid -0 output simply add 0...!
							output.println("# overall:\t" + Formatter.format(meanFix + 0.0, dataDigits) + " ± "
									+ Formatter.format(Math.sqrt(varFix / (nPopulation - 1)) + 0.0, dataDigits) + ", "
									+ Formatter.format(samples, 0) +
									(nFailed > 0L ? " (" + nFailed + " failed)" : ""));
							break;
						case STAT_UPDATES:
							output.println("# index,\t" + data.getKey() + (module.getNSpecies() > 1 ? "\tSpecies" : "")
									+ ",\tFixation and absorption updates (mean, sdev, samples) of single "
									+ traitNames[fixData.mutantTrait]
									+ " in " + traitNames[fixData.residentTrait] + " population for all locations");
							for (int n = 0; n < nPopulation; n++)
								printTimeStat(fixUpdate, n);
							String tail = "";
							if (nFailed > 0L)
								tail = " (" + nFailed + " failed)";
							printTimeStat(fixTotUpdate, "# overall:\t", tail);
							break;
						case STAT_TIMES:
							output.println("# index,\t" + data.getKey() + (module.getNSpecies() > 1 ? "\tSpecies" : "")
									+ ",\tFixation and absorption times (mean, sdev, samples) of single "
									+ traitNames[fixData.mutantTrait]
									+ " in " + traitNames[fixData.residentTrait] + " population for all locations");
							for (int n = 0; n < nPopulation; n++)
								printTimeStat(fixTime, n);
							tail = "";
							if (nFailed > 0L)
								tail = " (" + nFailed + " failed)";
							printTimeStat(fixTotTime, "# overall:\t", tail);
							break;
						default:
							throw new Error("Statistics for " + data.getKey() + " not supported!");
					}
				}
				break;
			case STATISTICS_UPDATE:
				throw new Error("Mode " + Mode.STATISTICS_UPDATE + " not implemented.");
			default:
				throw new Error("Mode not recognized.");
		}
		dumpEnd();
		exit(0);
	}

	/**
	 * Generate a single, valid statistics sample. 
	 * 
	 * @return the statistics sample
	 * 
	 * @see EvoLudoGWT#scheduleSample()
	 */
	public FixationData generateSample() {
		FixationData fix;
		do {
			// in unfortunate cases even a single sample can take exceedingly long
			// times. stop/init/reset need to be able to interrupt.
			switch (pendingAction) {
				case NONE:
				case STATISTIC:
				case STOP: // finish sample
					break;
				default:
					fireModelStopped();
					break;
			}
			while (activeModel.next()) {
				fireModelChanged();
			}
			fireModelStopped();
			fix = activeModel.getFixationData();
		} while (fix.mutantNode < 0);
		return fix;
	}

	/**
	 * Index of the mutants mean fixation probability/updates/time.
	 */
	static int MUTANT_MEAN = 0;

	/**
	 * Index of the mutants variance of fixation probability/updates/times.
	 */
	static int MUTANT_VAR = 1;

	/**
	 * Index of the number of samples for the mutants mean and variance.
	 */
	static int MUTANT_NORM = 2;

	/**
	 * Index of the reseidents mean fixation probability/updates/time.
	 */
	static int RESIDENT_MEAN = 3;

	/**
	 * Index of the residents variance of fixation probability/updates/times.
	 */
	static int RESIDENT_VAR = 4;

	/**
	 * Index of the number of samples for the residents mean and variance.
	 */
	static int RESIDENT_NORM = 5;

	/**
	 * Index of the mean  absorption probability/update/time.
	 */
	static int ABSORPTION_MEAN = 6;

	/**
	 * Index of the variance of the mean absorption probability/updates/time.
	 */
	static int ABSORPTION_VAR = 7;

	/**
	 * Index of the the number of samples for absorption probability/updates/time.
	 */
	static int ABSORPTION_NORM = 8;

	/**
	 * Helper method to calculate running mean and variance for fixation
	 * updates/times. The data structure of the {@code meanvar} array is defined as
	 * follows:
	 * <dl>
	 * <dt>{@code MUTANT_MEAN}
	 * <dd>mean of mutant fixation
	 * <dt>{@code MUTANT_VAR}
	 * <dd>variance of mutant fixation
	 * <dt>{@code MUTANT_NORM}
	 * <dd>sample count of mutant fixation
	 * <dt>{@code RESIDENT_MEAN}
	 * <dd>mean of resident fixation
	 * <dt>{@code RESIDENT_VAR}
	 * <dd>variance of resident fixation
	 * <dt>{@code RESIDENT_NORM}
	 * <dd>sample count of resident fixation
	 * <dt>{@code ABSORPTION_MEAN}
	 * <dd>mean of absorption
	 * <dt>{@code ABSORPTION_VAR}
	 * <dd>variance of absorption
	 * <dt>{@code ABSORPTION_NORM}
	 * <dd>sample count of absorption ({@code meanvar[MUTANT_NORM] + meanvar[RESIDENT_NORM] == meanvar[ABSORPTION_NORM]}
	 * must hold)
	 * </dl>
	 * 
	 * @param meanvar     the array that stores the running mean and variance
	 * @param mutantfixed the flag to indicate whether the mutant fixated
	 * @param x           the time/updates to fixation
	 */
	void updateMeanVar(double[] meanvar, boolean mutantfixed, double x) {
		updateMeanVar(meanvar, x, mutantfixed ? MUTANT_MEAN : RESIDENT_MEAN);
		updateMeanVar(meanvar, x, ABSORPTION_MEAN);
	}

	/**
	 * Helper method to calculate running mean and variance samples {@code x}. The
	 * entries in the {@code meanvar} array are
	 * {@code [offset: mean, offset + 1: variance, offset + 2: sample count]}.
	 * 
	 * @param meanvar the array with the running mean and variance
	 * @param x       the fixation probability/update/time
	 * @param offset  the offset in the {@code meanvar} array
	 */
	private void updateMeanVar(double[] meanvar, double x, int offset) {
		double mean = meanvar[offset];
		double dx = x - mean;
		double norm = ++meanvar[offset + 2];
		mean += dx / norm;
		meanvar[offset] = mean;
		meanvar[offset + 1] += dx * (x - mean);
	}

	/**
	 * Helper method to print the fixation and absorption updates/times.
	 * 
	 * @param meanvar the 2D array with mean and variance for all nodes
	 * @param n       the index of the node to process
	 */
	private void printTimeStat(double[][] meanvar, int n) {
		printTimeStat(meanvar[n], n + ",\t");
	}

	/**
	 * Helper method to print the fixation and absorption updates/times.
	 * 
	 * @param meanvar the array with the running mean and variance
	 * @param head    the string to prepend to the output
	 * 
	 * @see #updateMeanVar(double[], boolean, double) see updateMeanVar(double[],
	 *      boolean, double) for structure of {@code meanvar} array
	 */
	private void printTimeStat(double[] meanvar, String head) {
		printTimeStat(meanvar, head, "");
	}

	/**
	 * Helper method to print the fixation and absorption updates/times.
	 * 
	 * @param meanvar the array with the running mean and variance
	 * @param head    the string to prepend to the output
	 * @param tail    the string to append to the output
	 * 
	 * @see #updateMeanVar(double[], boolean, double) see updateMeanVar(double[],
	 *      boolean, double) for structure of {@code meanvar} array
	 */
	private void printTimeStat(double[] meanvar, String head, String tail) {
		double normut = meanvar[MUTANT_NORM];
		double normres = meanvar[RESIDENT_NORM];
		double normabs = meanvar[ABSORPTION_NORM];
		if (normabs <= 0.0)
			return; // no samples for node n
		// trick: to avoid -0 output simply add 0...!
		output.println(head + Formatter.format(meanvar[MUTANT_MEAN] + 0.0, dataDigits) + " ± " // mutant mean fixation time
				+ (normut > 1.0 ? Formatter.format(Math.sqrt(meanvar[MUTANT_VAR] / (normut - 1.0)) + 0.0, dataDigits) : "-") + ", " // mutant mean fixation
																									// sdev
				+ Formatter.format(normut, 0) + "; " // mutant mean fixation samples
				+ Formatter.format(meanvar[RESIDENT_MEAN] + 0.0, dataDigits) + " ± " // resident mean fixation time
				+ (normres > 1.0 ? Formatter.format(Math.sqrt(meanvar[RESIDENT_VAR] / (normres - 1.0)) + 0.0, dataDigits) : "-") + ", "  // resident mean
																									// fixation sdev
				+ Formatter.format(normres, 0) + "; " // mutant mean fixation samples
				+ Formatter.format(meanvar[ABSORPTION_MEAN] + 0.0, dataDigits) + " ± " // mean absorption time
				+ (normabs > 1.0 ? Formatter.format(Math.sqrt(meanvar[ABSORPTION_VAR] / (normabs - 1.0)) + 0.0, dataDigits) : "-") + ", " // mean absorption
																									// sdev
				+ Formatter.format(normabs, 0)
				+ tail); // mean absorption samples
	}

	/**
	 * Flag indicating whether engine is idling.
	 */
	private boolean isWaiting = true;

	/**
	 * Name of file that stores current git version in jar archive.
	 */
	protected static final String GIT_PROPERTIES_FILE = "/git.properties";

	/**
	 * Properties of GIT_PROPERTIES_FILE are stored here.
	 */
	protected static Properties properties = new Properties();

	/**
	 * Helper method to read {@link Properties} from properties in jar archive.
	 * <p>
	 * <b>Note:</b> {@link java.io.UnsupportedEncodingException} should never be
	 * thrown.
	 */
	private static void readProperties() {
		if (!properties.isEmpty())
			return;
		try {
			properties.load(EvoLudo.class.getResourceAsStream(GIT_PROPERTIES_FILE));
		} catch (Exception e) {
			// not much we can do...
		}
	}

	/**
	 * Helper method to retrieve property for given key from properties in jar
	 * archive.
	 * 
	 * @param key the name of the property
	 * @return the value of the property
	 */
	private String getProperty(String key) {
		readProperties();
		if (properties.isEmpty())
			return "unresolved"; // reading attributes failed
		String value = properties.getProperty(key);
		if (value == null)
			return "unknown";
		return value;
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * Retrieves git commit version from file in jar archive.
	 */
	@Override
	public String getGit() {
		return getProperty("git.commit.id.describe");
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * Retrieves git commit time from file in jar archive.
	 */
	@Override
	public String getGitDate() {
		return getProperty("git.build.time");
	}

	/**
	 * Attributes of MANIFEST are stored here.
	 */
	protected static Attributes attributes = null;

	/**
	 * Helper method to read {@link Attributes} from MANIFEST in jar archive.
	 * <p>
	 * <b>Note:</b> {@link java.io.UnsupportedEncodingException} should never be
	 * thrown.
	 */
	private static void readAttributes() {
		if (attributes != null)
			return;
		try {
			Class<IBSPopulation> myClass = IBSPopulation.class;
			String className = "/" + myClass.getName().replace('.', '/') + ".class";
			String myUrl = myClass.getResource(className).toString();
			int to = myUrl.indexOf("!/");
			String jarName;
			jarName = URLDecoder.decode(myUrl.substring(0, to + 1), "UTF-8");
			attributes = new Manifest(new URL(jarName + "/META-INF/MANIFEST.MF").openStream()).getMainAttributes();
		} catch (Exception e) {
			// not much we can do...
		}
	}

	/**
	 * Retrieve attribute for given key.
	 * 
	 * @param key of <code>attribute</code>
	 * @return attribute for <code>key</code>
	 */
	public static String getAttribute(String key) {
		readAttributes();
		if (attributes == null)
			return null; // reading attributes failed
		return attributes.getValue(key);
	}

	/**
	 * Retrieve attribute for given key.
	 * 
	 * @param key of <code>attribute</code>
	 * @return attribute for <code>key</code>
	 */
	public static String getAttribute(Attributes.Name key) {
		readAttributes();
		if (attributes == null)
			return null; // reading attributes failed
		return attributes.getValue(key);
	}

	/**
	 * All output should be printed to <code>output</code> (defaults to
	 * <code>stdout</code>). This is only relevant for JRE applications (mainly
	 * simulations) and ignored by GWT.
	 * <p>
	 * <strong>Note:</strong> <code>output</code> can be customized using the
	 * <code>--output</code> and <code>--append</code> command line options (see
	 * {@link EvoLudoJRE#cloOutput} and {@link EvoLudoJRE#cloAppend}, respectively.
	 * 
	 */
	protected PrintStream output = System.out;

	/**
	 * Get output for all reporting (JRE only). Used by {@link #cloOutput} and
	 * {@link #cloAppend} to redirect output to a file.
	 * 
	 * @return the output for reporting
	 */
	public PrintStream getOutput() {
		return output;
	}

	/**
	 * Set output for all reporting (JRE only). Used by {@link #cloOutput} and
	 * {@link #cloAppend} to redirect output to a file.
	 * 
	 * @param output the output stream for reporting
	 */
	public void setOutput(PrintStream output) {
		this.output = (output == null ? System.out : output);
		parser.setOutput(this.output);
	}

	@Override
	public void dumpParameters() {
		output.println(
				// print easily identifiable delimiter to simplify processing of data files
				// containing
				// multiple simulation runs - e.g. for graphical representations in MATLAB
				"! New Record" + "\n# " + activeModule.getTitle() + "\n# " + getVersion() + "\n# today:                "
						+ (new Date().toString()));
		output.println("# arguments:            " + parser.getCLO());
		parser.dumpCLO();
		output.println("# data:");
		output.flush();
	}

	/**
	 * Name of file to export state of model at end of run.
	 */
	String exportname = null;

	/**
	 * Name of directory for exports.
	 */
	String exportdir = null;

	@Override
	public void dumpEnd() {
		int deltamilli = elapsedTimeMsec();
		int deltasec = deltamilli / 1000;
		int deltamin = deltasec / 60;
		deltasec %= 60;
		int deltahour = deltamin / 60;
		deltamin %= 60;
		DecimalFormat twodigits = new DecimalFormat("00");
		output.println("# runningtime:          " + deltahour + ":" + twodigits.format(deltamin) + ":"
				+ twodigits.format(deltasec) + "." + twodigits.format(deltamilli % 1000));
	}

	/**
	 * The name of the file to restore state from.
	 */
	String plistname = null;

	/**
	 * The processed state for restoring.
	 */
	Plist plist = null;

	/**
	 * {@inheritDoc}
	 * <dl>
	 * <dt>{@code --help}</dt>
	 * <dd>ignore all other options.</dd>
	 * <dt>{@code --restore <filename>}</dt>
	 * <dd>load options from {@code filename} and ignore all simulator options on
	 * command line, but not GUI options.</dd>
	 * </dl>
	 */
	@Override
	protected String[] preprocessCLO(String[] cloarray) {
		// once module is loaded pre-processing of command line arguments can proceed
		cloarray = super.preprocessCLO(cloarray);
		String helpName = cloHelp.getName();
		if (cloarray == null)
			return new String[] { helpName };
		// check if --help or --restore requested
		String restoreName = cloRestore.getName();
		int nParams = cloarray.length;
		for (int i = 0; i < nParams; i++) {
			String param = cloarray[i];
			if (param.startsWith(helpName)) {
				// discard/ignore all other options
				return new String[] { helpName };
			}
			if (param.startsWith(restoreName)) {
				plistname = CLOption.stripKey(restoreName, param).trim();
				cloarray = ArrayMath.drop(cloarray, i--);
				nParams--;
				if (plistname.length() == 0) {
					plistname = null;
					logger.warning("file name to restore state missing - ignored.");
					break;
				}
				// ignore if already restoring; strip restore option and argument
				if (!doRestore) {
					plist = readPlist(plistname);
					if (plist == null)
						continue;
					String restoreOptions = (String) plist.get("CLO");
					if (restoreOptions == null) {
						logger.warning("state in '" + plistname + "' corrupt (CLO key missing) - ignored.");
						plist = null;
						continue;
					}
					String[] clos = restoreOptions.split("--");
					String moduleName = cloModule.getName();
					String moduleKey = activeModule.getKey();
					int rParams = clos.length;
					for (int j = 0; j < rParams; j++) {
						String rParam = clos[j];
						if (rParam.startsWith(moduleName)) {
							String rModule = CLOption.stripKey(moduleName, param).trim();
							if (!rModule.equals(moduleKey)) {
								logger.warning("state in '" + plistname + "' refers to module '" + rModule
										+ "' but expected '" + moduleKey + "' - skipping restore.");
								plist = null;
								break;
							}
							// merge options and remove --module from clos
							clos = ArrayMath.drop(clos, j--);
							rParams--;
							String[] sargs = new String[nParams + rParams];
							System.arraycopy(cloarray, 0, sargs, 0, nParams);
							System.arraycopy(clos, 0, sargs, nParams, rParams);
							doRestore = true;
							// restart preprocessing with extended command line arguments
							// note: if the same option is listed multiple times the last one overwrites
							// the previous ones. thus, any options specified in the restore file take
							// precedence over those specified on the command line.
							return preprocessCLO(sargs);
						}
					}
				}
			}
		}
		return cloarray;
	}

	@Override
	public boolean restoreFromFile() {
		if (!restoreState(plist)) {
			logger.warning("failed to restore state in '" + plistname + "'");
			return false;
		}
		return true;
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * Special treatment of <code>--restore</code> option.
	 * </p>
	 */
	@Override
	public boolean parseCLO() {
		boolean success = super.parseCLO();
		if (activeModule == null) {
			// this is fatal - exit
			fatal("--module missing - exiting!");
		}
		if (!doRestore)
			return success;
		if (!success)
			logger.warning("abort restoring of state in '" + plistname + "' due to parsing problems.");
		// parseCLO does not reset model - do it now to be ready for restore
		modelReset();
		// finish restoring
		return restoreFromFile();
	}

	@Override
	public void helpCLO() {
		super.helpCLO();
		output.print("List of command line options");
		String missing = "";
		if (activeModule != null) {
			output.print(" for module '" + activeModule.getKey() + "'");
			if (activeModel != null)
				output.print(" and model '" + activeModel.getModelType().getKey() + "'");
			else
				missing = " (select model for more options)";
		}
		else
			missing = " (select module and model for more options)";	
		output.println(missing + ":\n" + parser.helpCLO(true));
		exit(0);
	}

	/**
	 * Check if command line option <code>name</code> is available.
	 *
	 * @param name of command line option
	 * @return <code>true</code> if <code>name</code> is an option.
	 */
	public boolean providesCLO(String name) {
		return parser.providesCLO(name);
	}

	/**
	 * Command line option to redirect output to file (output overwrites potentially
	 * existing file).
	 */
	public final CLOption cloOutput = new CLOption("output", "stdout", catSimulation,
			"--output <f>    redirect output to file", new CLODelegate() {
				@Override
				public boolean parse(String arg) {
					// --append option takes precedence; ignore --output setting
					if (cloAppend.isSet())
						return true;
					if (!cloOutput.isSet()) {
						setOutput(null);
						return true;
					}
					File out = new File(arg);
					if (fileCheck(out, true)) {
						try {
							// open print stream for appending
							setOutput(new PrintStream(new FileOutputStream(out, true), true));
							return true;
						} catch (Exception e) {
							// ignore exception
						}
					}
					setOutput(null);
					logger.warning("failed to open '" + arg + "' - using stdout.");
					return false;
				}
			});

	/**
	 * Command line option to redirect output to file (appends output to potentially
	 * existing file).
	 */
	public final CLOption cloAppend = new CLOption("append", "stdout", catSimulation,
			"--append <f>    append output to file", new CLODelegate() {
				@Override
				public boolean parse(String arg) {
					// --append option takes precedence; ignore --output setting
					if (!cloAppend.isSet()) {
						if (cloOutput.isSet())
							return true;
						// if neither --append nor --output are set use default stdout
						setOutput(null);
						return true;
					}
					File out = new File(arg);
					if (fileCheck(out, false)) {
						try {
							// open print stream for appending
							setOutput(new PrintStream(new FileOutputStream(out, true), true));
							return true;
						} catch (Exception e) {
							// ignore exception
						}
					}
					setOutput(null);
					logger.warning("failed to append to '" + arg + "' - using stdout.");
					return false;
				}
			});

	/**
	 * Command line option to restore state from file. Typically states have been
	 * saved previously using the export options in the context menu of the GUI or
	 * when requesting to save the end state of a simulation run with
	 * {@code --export}, see {@link #cloExport}.
	 */
	public final CLOption cloRestore = new CLOption("restore", "norestore", catSimulation,
			"--restore <filename>  restore saved state from file", new CLODelegate() {
				@Override
				public boolean parse(String arg) {
					// option gets special treatment
					return true;
				}

				@Override
				public void report(PrintStream out) {
					if (cloRestore.isSet())
						out.println("# restored:             " + cloRestore.getArg());
				}
			});

	/**
	 * Command line option to export end state of simulation to file. Saved states
	 * can be read using {@code --restore} to restore the state and resume
	 * execution, see {@link #cloRestore}.
	 */
	public final CLOption cloExport = new CLOption("export", "evoludo-%d.plist", CLOption.Argument.OPTIONAL,
			catSimulation,
			"--export [<filename>]  export final state of simulation (%d for generation)", new CLODelegate() {
				@Override
				public boolean parse(String arg) {
					if (!cloExport.isSet()) {
						exportname = null;
						return true;
					}
					exportname = arg;
					if (!cloExport.isDefault())
						return true;
					// arg is default; prefer --append or --output file name (with extension plist
					// added or substituted)
					if (cloAppend.isSet()) {
						exportname = cloAppend.getArg();
						return true;
					}
					if (cloOutput.isSet()) {
						exportname = cloOutput.getArg();
						return true;
					}
					// use default
					return true;
				}
			});

	/**
	 * The data array that contains identifiers for the kind of data reported by
	 * simulations.
	 * 
	 * @see #simulation()
	 */
	ArrayList<MultiView.DataTypes> dataTypes;

	/**
	 * The field to specify the accurracy of data reported in simulations.
	 * 
	 * @see #simulation()
	 */
	int dataDigits;

	/**
	 * Helper method to determine whether the data {@code type} requires
	 * {@link Mode#STATISTICS_SAMPLE} or {@link Mode#DYNAMICS}.
	 * 
	 * @param type the data type to check
	 * @return {@code true} if {@code Mode#DYNAMICS}
	 */
	private boolean isDynamicsDataType(MultiView.DataTypes type) {
		return !(type == MultiView.DataTypes.STAT_PROB || type == MultiView.DataTypes.STAT_UPDATES
				|| type == MultiView.DataTypes.STAT_TIMES);
	}

	/**
	 * Command line option to set the data reported by simulations.
	 */
	public final CLOption cloData = new CLOption("data", "none", catSimulation,
			"--data <d[,d1,...]>  type of data to report", new CLODelegate() {
				@Override
				public boolean parse(String arg) {
					if (cloData.isDefault() || cloData.getDefault().equals(arg)) {
						return true; // no default
					}
					boolean success = true;
					String[] dataOutput = arg.split(CLOParser.VECTOR_DELIMITER);
					dataTypes = new ArrayList<>();
					for (String type : dataOutput) {
						MultiView.DataTypes data = (MultiView.DataTypes) cloData.match(type);
						if (data == null) {
							logger.warning(
									"Data type '" + type + "' not supported by module '" + getModule().getName() + "'");
							success = false;
							continue;
						}
						if (dataTypes.contains(data))
							logger.warning("Duplicate data type '" + type + "' or bad match - ignored.");
						else
							dataTypes.add(data);
					}
					// check if any data output requested
					if (dataTypes == null || dataTypes.isEmpty()) {
						logError("No data type recognized. Use --help for information about --data option.");
						exit(1);
					}
					// cannot mix dynamics and statistics modes
					boolean isDynamic = isDynamicsDataType(dataTypes.get(0));
					Iterator<MultiView.DataTypes> i = dataTypes.iterator();
					while (i.hasNext()) {
						MultiView.DataTypes type = i.next();
						if (isDynamicsDataType(type) != isDynamic) {
							logger.warning(
									"Cannot mix data from dynamics and statistics - ignoring '" + type.getKey() + "'");
							i.remove();
							// note: cannot use 'for (Module.DataTypes type : dataTypes)'
							// because then 'dataTypes.remove(type)' throws a concurrent
							// modification exception
							success = false;
							continue;
						}
						if (!isDynamicsDataType(type) && getModule().getNSpecies() > 1) {
							logger.warning("Statistics not (yet) implemented for multi-species modules - ignoring '"
									+ type.getKey() + "'");
							i.remove();
							success = false;
							continue;
						}
					}
					dataTypes.trimToSize();
					// too early to set the mode; relevant options may not have been parsed yet
					return success;
				}
			});

	/**
	 * Command line option to set the data reported by simulations.
	 */
	public final CLOption cloDigits = new CLOption("digits", "4", catSimulation,
			"--digits <d>    precision of data output", new CLODelegate() {
				@Override
				public boolean parse(String arg) {
					dataDigits = CLOParser.parseInteger(arg);
					return true;
				}
			});

	@Override
	public void collectCLO(CLOParser prsr) {
		// some options are only meaningful when running simulations
		if (!isApplication) {
			// simulation
			prsr.addCLO(cloOutput);
			prsr.addCLO(cloAppend);
			prsr.addCLO(cloExport);
			// XXX should not be added for customized simulations or should it?
			prsr.addCLO(cloData);
			cloData.clearKeys();
			cloData.addKeys(MultiView.getAvailableDataTypes(activeModule, activeModel));
			prsr.addCLO(cloDigits);
		}
		prsr.addCLO(cloRestore);
		super.collectCLO(prsr);
		// some options are not meaningful when running simulations
		if (!isApplication) {
			// --run does not make sense for simulations
			prsr.removeCLO(cloRun);
		}
	}

	@Override
	public String getJavaVersion() {
		return System.getProperty("java.version");
	}

	/**
	 * Process PLIST file <code>name</code> and return the parsed content. If
	 * <code>name</code> ends with <code>.zip</code> it is assumed to be a
	 * compressed file. If parsing fails <code>null</code> is returned.
	 * 
	 * @param name the name of the PLIST file
	 * @return the parsed content of the PLIST file
	 */
	public Plist readPlist(String name) {
		if (name.endsWith(".zip")) {
			// assume compressed file
			StringBuilder content = new StringBuilder();
			try {
				ZipInputStream zis = new ZipInputStream(new FileInputStream(name));
				// process first entry in zip file
				zis.getNextEntry();
				BufferedReader in = new BufferedReader(new InputStreamReader(zis));
				String line;
				while ((line = in.readLine()) != null)
					content.append(line);
				in.close();
				zis.close();
			} catch (Exception e) {
				logger.warning("failed to read state in '" + name + "'");
			}
			return PlistParser.parse(content.toString());
		}
		// keep for compatibility
		if (name.endsWith(".gz")) {
			// assume compressed file
			StringBuilder content = new StringBuilder();
			try {
				GZIPInputStream gis = new GZIPInputStream(new FileInputStream(name));
				BufferedReader in = new BufferedReader(new InputStreamReader(gis));
				String line;
				while ((line = in.readLine()) != null)
					content.append(line);
				in.close();
				gis.close();
			} catch (Exception e) {
				logger.warning("failed to read state in '" + name + "'");
			}
			return PlistParser.parse(content.toString());
		}
		try {
			String content = new String(Files.readAllBytes(Paths.get(name)));
			return PlistParser.parse(content);
		} catch (Exception e) {
			logger.warning("failed to read state in '" + name + "'");
			// e.printStackTrace(); // for debugging
			return null;
		}
	}

	/**
	 * Helper method to generate a unique file name based on the
	 * <code>template</code>. If <code>template</code> sports and extension it is
	 * replaced by <code>extension</code> otherwise the <code>extension</code> is
	 * appended. If the <code>template</code> contains <code>%d</code> it is
	 * replaced by the current generation. Finally, if necessary, <code>"-n"</code>
	 * is appended to <code>template</code>, where <code>n</code> is a number, to
	 * make file name unique.
	 * 
	 * @param template  where the placeholder <code>%d</code> is replaced by current
	 *                  generation (if present).
	 * @param extension of file name
	 * @return unique file
	 */
	private File uniqueFile(String template, String extension) {
		// replace/add extension
		if (!template.endsWith("." + extension)) {
			int ext = template.lastIndexOf('.');
			if (ext < 0)
				ext = template.length();
			template = template.substring(0, ext) + "." + extension;
		}
		// if template starts with '/' it's and absolute path
		// otherwise set path relative to export directory
		if (!template.startsWith(File.separator)) {
			// create full path; defaults to current directory
			String dir = getExportDir();
			if (!dir.endsWith(File.separator))
				dir += File.separator;
			template = dir + template;
		}
		if (template.contains("%d"))
			template = String.format(template, (int) activeModel.getTime());
		File unique = new File(template);
		int counter = 0;
		while (!fileCheck(unique, true) && counter < 100) {
			unique = new File(template.substring(0, template.lastIndexOf('.')) + "-" + (++counter) + "." + extension);
		}
		// check if emergency brake was pulled
		if (counter >= 1000)
			return null;
		return unique;
	}

	/**
	 * Helper method to check whether <code>file</code> is writable.
	 * 
	 * @param file   name of file to check
	 * @param unique if <code>true</code> file must not exist
	 * @return <code>true</code> if all checks passed
	 */
	private boolean fileCheck(File file, boolean unique) {
		if (file.isDirectory()) {
			logger.warning("'" + file.getPath() + "' is a directory");
			return false;
		}
		if (unique || !file.exists()) {
			try {
				if (!file.createNewFile()) {
					logger.warning("file '" + file.getPath() + "' already exists");
					return false;
				}
			} catch (IOException io) {
				logger.warning("failed to create file '" + file.getPath() + "'");
				return false;
			}
		}
		if (!file.canWrite()) {
			logger.warning("file '" + file.getPath() + "' not writable");
			return false;
		}
		return true;
	}

	/**
	 * Get the directory for exports.
	 * 
	 * @return the export directory
	 */
	public String getExportDir() {
		return (exportdir == null ? "." : exportdir);
	}

	/**
	 * Set the directory for exports.
	 * 
	 * @param dir the export directory
	 */
	public void setExportDir(File dir) {
		exportdir = dir.getAbsolutePath();
	}

	@Override
	public void exportState() {
		if (exportname == null)
			return;
		exportState(exportname);
	}

	/**
	 * Export state of module to {@code filename}. If {@code filename == null} try
	 * {@code exportname}. If {@code exportname == null} too then create new unique
	 * filename.
	 * 
	 * @param filename the filename for exporting the state
	 */
	public void exportState(String filename) {
		File export;
		if (filename == null)
			filename = exportname;
		if (filename == null)
			export = openSnapshot("plist");
		else
			export = uniqueFile(filename, "plist");
		String state = encodeState();
		if (state == null) {
			logger.severe("failed to encode state.");
			return;
		}
		try {
			// if export==null this throws an exception
			PrintStream stream = new PrintStream(export);
			stream.println(state);
			stream.close();
			logger.info("state saved in '" + export.getName() + "'.");
		} catch (Exception e) {
			String msg = "";
			if (export != null)
				msg = "to '" + export.getPath() + "' ";
			else if (filename != null)
				msg = "to '" + filename + ".plist' ";
			logger.warning("failed to export state " + msg + "- using '"
					+ (cloAppend.isSet() ? cloAppend.getArg() : cloOutput.getArg()) + "'");
			output.println(state);
		}
	}

	/**
	 * Open file with name <code>evoludo-%d</code> (the placeholder <code>%d</code>
	 * is replaced by current generation) and extension <code>ext</code>. Used to
	 * export snapshots of the current state or other data. Ensures that file does
	 * not exist and, if necessary, appends <code>-n</code> to the file name where
	 * <code>n</code> is a number that makes the name unique.
	 * <p>
	 * <b>Note:</b> almost copy from MVAbstract, better organization could prevent
	 * the duplication...
	 * 
	 * @param ext file name extension
	 * @return new unique file
	 */
	protected File openSnapshot(String ext) {
		String dir = getExportDir();
		if (!dir.endsWith(File.separator))
			dir += File.separator;
		File snapfile = new File(dir + String.format("evoludo-%d." + ext, (int) activeModel.getTime()));
		int counter = 0;
		while (snapfile.exists() && counter < 1000)
			snapfile = new File(dir + String.format("evoludo-%d-%d." + ext, (int) activeModel.getTime(), ++counter));
		if (counter >= 1000)
			return null;
		return snapfile;
	}

	@Override
	public void fatal(String msg) {
		super.fatal(msg);
		exit(1);
	}

	/*
	 * <strong>Notes:</strong> (as of 20191223 likely only of historical
	 * interest; as of 20231006 entry point retired, see EvoLudoLab#main.)
	 * <p>
	 * At least on Mac OS X.6 (Snow Leopard) initializing static Color makes the JRE
	 * ignore the headless request. The behavior is inconsistent and hence most
	 * likely a cause of Apple's java implementation. In particular, if
	 * -Djava.awt.headless=true is specified on the command line, the request is
	 * honored but not by setting the system property. Fortunately an easy
	 * work-around exists in that the colors (and potentially other AWT classes)
	 * should only be instantiated in the constructor and not along with the
	 * variable declaration. probably the JRE switches to headless=false once it
	 * encounters a static allocation of AWT classes. Since this happens even before
	 * entering main(), subsequent requests to set headless may fail/be ignored.
	 */

	/**
	 * The reference to the shutdown hook (if any). Used to dump state of simulation
	 * before an emergency shutdown.
	 */
	ShutdownHook hook;

	/**
	 * Register shut down hook.
	 */
	public void registerHook() {
		hook = new ShutdownHook(this);
		Runtime.getRuntime().addShutdownHook(hook);
	}

	/**
	 * Exit simulation. Remove shutdown hook (if any) and terminate execution.
	 * 
	 * @param status the exit status of the simulation
	 */
	public void exit(int status) {
		if (hook != null)
			Runtime.getRuntime().removeShutdownHook(hook);

		System.exit(status);
	}

	/**
	 * The shutdown hook. Gets called when the running simulation received an
	 * {@code SIGNT} signal to write an emergency dumpe of current state of
	 * simulations before exiting.
	 */
	static class ShutdownHook extends Thread {

		/**
		 * The reference to the pacemaker of the current simulation.
		 */
		EvoLudoJRE engine;

		/**
		 * Initialize the shutdown hook.
		 * 
		 * @param engine the pacemaker of the current simulation
		 */
		public ShutdownHook(EvoLudoJRE engine) {
			this.engine = engine;
		}

		/**
		 * {@inheritDoc}
		 * <p>
		 * Export current state of the simulation.
		 */
		@Override
		public void run() {
			engine.exportState("panic-%d");
		}
	}
}
