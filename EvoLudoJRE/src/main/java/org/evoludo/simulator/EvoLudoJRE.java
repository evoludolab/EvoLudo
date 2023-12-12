//
// EvoLudo Project
//
// Copyright 2010 Christoph Hauert
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
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.Properties;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipInputStream;

import javax.swing.Timer;

import org.evoludo.graphics.Network2DJRE;
import org.evoludo.math.ArrayMath;
import org.evoludo.simulator.modules.Module;
import org.evoludo.simulator.modules.Discrete;
import org.evoludo.simulator.models.IBS;
import org.evoludo.simulator.models.IBSC;
import org.evoludo.simulator.models.IBSD;
import org.evoludo.simulator.models.IBSDPopulation;
import org.evoludo.simulator.models.IBSMCPopulation;
import org.evoludo.simulator.models.IBSPopulation;
import org.evoludo.simulator.models.Model;
import org.evoludo.simulator.models.PDERD;
import org.evoludo.simulator.models.PDESupervisor;
import org.evoludo.simulator.models.IBSD.FixationData;
import org.evoludo.simulator.models.Model.Mode;
import org.evoludo.simulator.modules.Traits;
import org.evoludo.util.CLOParser;
import org.evoludo.util.CLOption;
import org.evoludo.util.Formatter;
import org.evoludo.util.CLOption.CLODelegate;
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

	Timer timer = null;

	Thread engineThread = null; // engine thread
	Thread executeThread = null; // command execution thread

	public EvoLudoJRE() {
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
		addModule(new Traits(this));
	}

	/**
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

	protected void launchEngine() {
		if (engineThread != null)
			return;
		engineThread = new Thread(this, "Engine");
		engineThread.start();
	}

	@Override
	public PDESupervisor hirePDESupervisor(Model.PDE charge) {
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
	 * poke waiting thread to resume execution.
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
			isSuspended = false;
			if (isRunning)
				return;
			isRunning = true;
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
		super.modelStopped();
	}

	@Override
	public void modelDidReinit() {
		timer.stop();
		isWaiting = true;
		super.modelDidReinit();
	}

	@Override
	public void modelDidReset() {
		timer.stop();
		isWaiting = true;
		super.modelDidReset();
	}

	boolean simulationRunning = false;

	/**
	 * {@inheritDoc}
	 * <p>
	 * Run simulations. This handles the following scenarios:
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
	public void simulation(String[] args) {
		String main = EvoLudoJRE.getAttribute("Engine-Class");
		if (main != null) {
			Module module;
			// customized engine specified
			try {
				// automatic instantiation of desired class!
				module = (Module) Class.forName(main).getDeclaredConstructor(EvoLudo.class).newInstance(this);
			} catch (Exception e) {
				e.printStackTrace();
				// log directly to System.out - no other outlets available at this time
				Logger.getLogger(EvoLudo.class.getName()).severe("Failed to instantiate " + main);
				System.exit(1);
				// code never gets here but prevents null-pointer warnings.
				return;
			}
			// prepend --module option (any additional --module options are ignored)
			args = Arrays.copyOf(args, args.length + 2);
			System.arraycopy(args, 0, args, 2, args.length - 2);
			args[0] = "--" + cloModule.getName();
			String key = module.getKey();
			args[1] = key;
			addModule(module);
			addCLOProvider(module);
			// parse options
			parseCLO(args);
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
		// ensure that --module option is specified
		String moduleOption = "--" + cloModule.getName();
		String moduleName = null;
		int nArgs = args.length;
		// no need to check last argument if it's --module then the module key is missing and still no use
		for (int i = 0; i < nArgs - 1; i++) {
			if (moduleOption.equals(args[i])) {
				moduleName = args[i + 1];
				break;
			}
		}
		if (moduleName == null || modules.get(moduleName) == null) {
			// no module requested - show help
			logError(moduleOption + " " + (moduleName == null ? "option missing!" : moduleName + " module not found!"));
			parser.clearCLO();
			parser.addCLO(cloModule);
			output.println("List of available modules:\n" + parser.helpCLO(false));
			exit(0);
		}
		// parse options
		parseCLO(args);
		if (cloExport.isSet()) {
			// export option provided: run model and dump state.
			setDelay(1);
			setSuspended(true);
			modelReset();
			if (isSuspended) {
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
			}
			dumpEnd();
			exit(0);
		}
		if (dataTypes == null || dataTypes.isEmpty()) {
			// no data types provided for simulation output
			// return control to caller.
			return;
		}
		// run simulation
		// reset model to check and apply all parameters
		modelReset();
		// register hook to dump state when receiving SIGINT
		registerHook();
		// helper variables
		Module module = getModule();
		Model model = getModel();
		// allocate storage and initialize helper variables
		int totTraits = 0;
		boolean isContinuous = false;
		for (Module specie : module.getSpecies()) {
			int nt = specie.getNTraits();
			totTraits += nt;
			isContinuous |= specie.isContinuous();
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
		// helper variables
		double[][] fixProb = new double[0][0];
		double[][] fixUpdate = new double[0][0];
		double[][] fixTime = new double[0][0];
		int nTraits = -1;
		int nPopulation = -1;
		long nSamplesLong = (long) nSamples;
		long samples = 0L;
		int muttrait = -1;
		int restrait = -1;
		// check and print settings
		modelCheck();
		dumpParameters();
		// get ready to run simulation
		modelReset();
		// print data legend (for dynamical reports) and initialize variables (for
		// statistics reports)
		for (Module.DataTypes data : dataTypes) {
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
				// this is tedious because of interaction and reproduction geometries as well as
				// directed graphs with for incoming, outgoing and total degrees, which results
				// in up to 6 distributions
				// output.println("# time,\t" + data.getKey() + (module.getNSpecies() > 1 ?
				// "\tSpecies" : "") + ",\tDegree distribution of population structure");
				// break;
				case STAT_PROB:
					// casts are safe because otherwise STAT_PROB not available
					muttrait = ((IBSD) getModel()).getFixationData().mutantTrait;
					restrait = ArrayMath.maxIndex(((Discrete) module).getInit());
					nTraits = module.getNTraits();
					nPopulation = module.getNPopulation();
					fixProb = new double[nPopulation][nTraits + 1];
					break;
				case STAT_UPDATES:
					// casts are safe because otherwise STAT_UPDATES not available
					muttrait = ((IBSD) getModel()).getFixationData().mutantTrait;
					restrait = ArrayMath.maxIndex(((Discrete) module).getInit());
					nTraits = module.getNTraits();
					nPopulation = module.getNPopulation();
					fixUpdate = new double[nPopulation][9];
					break;
				case STAT_TIMES:
					// casts are safe because otherwise STAT_TIMES not available
					muttrait = ((IBSD) getModel()).getFixationData().mutantTrait;
					restrait = ArrayMath.maxIndex(((Discrete) module).getInit());
					nTraits = module.getNTraits();
					nPopulation = module.getNPopulation();
					fixTime = new double[nPopulation][9];
					break;
				default:
					output.println("# data output for " + data.getKey() + " not (yet) supported!");
			}
		}
		// run simulation
		if (model.isMode(Mode.DYNAMICS)) {
			boolean cont = true;
			// relax initial configuration
			modelRelax();
			while (true) {
				String time = Formatter.format(model.getTime(), dataDigits);
				// report dynamical data
				for (Module.DataTypes data : dataTypes) {
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
								for (Module specie : module.getSpecies()) {
									IBSDPopulation pop = (IBSDPopulation) ((IBS) model).getSpecies(specie);
									output.println(time + ",\t" + data.getKey()
											+ (isMultispecies ? "\t" + specie.getName() : "") + ",\t"
											+ pop.getTraits());
								}
								break;
							}
							if (model instanceof IBSC) {
								boolean isMultispecies = (module.getNSpecies() > 1);
								for (Module specie : module.getSpecies()) {
									IBSMCPopulation pop = (IBSMCPopulation) ((IBS) model).getSpecies(specie);
									output.println(time + ",\t" + data.getKey()
											+ (isMultispecies ? "\t" + specie.getName() : "") + ",\t"
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
								for (Module specie : module.getSpecies()) {
									IBSPopulation pop = ((IBS) model).getSpecies(specie);
									output.println(time + ",\t" + data.getKey()
											+ (isMultispecies ? "\t" + specie.getName() : "") + ",\t"
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
								for (Module specie : module.getSpecies()) {
									IBSPopulation pop = ((IBS) model).getSpecies(specie);
									output.println(time + ",\t" + data.getKey()
											+ (isMultispecies ? "\t" + specie.getName() : "") + ",\t"
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
				if (!cont || (nGenerations > 0.0 && model.getTime() > nGenerations))
					break;
				cont = modelNext();
			}
		} else if (model.isMode(Mode.STATISTICS)) {
			// perform statistics
			if (cloSeed.isSet()) {
				// initial state set. now clear seed to obtain reproducible statistics
				// rather just a single data point repeatedly
				rng.clearRNGSeed();

			}
			while (true) {
				while (modelNext())
					;
				// read and process sample
				samples++;
				FixationData fixData = ((IBSD) model).getFixationData();
				for (Module.DataTypes data : dataTypes) {
					switch (data) {
						case STAT_PROB:
							double[] node = fixProb[fixData.mutantNode];
							node[(fixData.typeFixed == muttrait ? 0 : 1)]++;
							node[2]++;
							fixData.probRead = true;
							break;
						case STAT_UPDATES:
							updateMeanVar(fixUpdate[fixData.mutantNode], (fixData.typeFixed == muttrait),
									fixData.updatesFixed);
							fixData.timeRead = true;
							break;
						case STAT_TIMES:
							updateMeanVar(fixTime[fixData.mutantNode], (fixData.typeFixed == muttrait),
									fixData.timeFixed);
							fixData.timeRead = true;
							break;
						default:
							throw new Error("Statistics for " + data.getKey() + " not supported!");
					}
				}
				if (samples > nSamplesLong)
					break;
				modelReset();
			}
			// report statistics
			for (Module.DataTypes data : dataTypes) {
				switch (data) {
					case STAT_PROB:
						output.println("# index,\t" + data.getKey() + (module.getNSpecies() > 1 ? "\tSpecies" : "")
								+ ",\tFixation probability of " + traitNames[muttrait]
								+ " mutant in " + traitNames[restrait] + " resident for all locations with samples");
						double meanFix = 0.0;
						double varFix = 0.0;
						for (int n = 0; n < nPopulation; n++) {
							double[] node = fixProb[n];
							double norm = node[2];
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
								+ Formatter.format(Math.sqrt(varFix / (nPopulation - 1)) + 0.0, dataDigits));
						break;
					case STAT_UPDATES:
						double meanResUpdates = 0.0;
						double meanResUpdateSdev = 0.0;
						double meanMutUpdates = 0.0;
						double meanMutUpdateSdev = 0.0;
						double meanAbsUpdates = 0.0;
						double meanAbsUpdatesSdev = 0.0;
						output.println("# index,\t" + data.getKey() + (module.getNSpecies() > 1 ? "\tSpecies" : "")
								+ ",\tFixation and absorption updates (mean, sdev, samples) of " + traitNames[muttrait]
								+ " mutant in " + traitNames[restrait] + " resident for all locations");
						for (int n = 0; n < nPopulation; n++) {
							printTimeStat(fixUpdate, n);
							double in1 = 1.0 / (n + 1);
							double[] fn = fixUpdate[n];
							double dx = fn[0] - meanMutUpdates;
							meanMutUpdates += dx * in1;
							// in [1] is the variance convert to sdev first
							dx = Math.sqrt(fn[1] / (fn[2] - 1.0)) - meanMutUpdateSdev;
							meanMutUpdateSdev += dx * in1;
							dx = fn[3] - meanResUpdates;
							meanResUpdates += dx * in1;
							dx = Math.sqrt(fn[3] / (fn[5] - 1.0)) - meanResUpdateSdev;
							meanResUpdateSdev += dx * in1;
							dx = fn[6] - meanAbsUpdates;
							meanAbsUpdates += dx * in1;
							dx = Math.sqrt(fn[6] / (fn[8] - 1.0)) - meanAbsUpdatesSdev;
							meanAbsUpdatesSdev += dx * in1;
						}
						// trick: to avoid -0 output simply add 0...!
						output.println("# overall:\t" + traitNames[muttrait] + ": " //
								+ Formatter.format(meanMutUpdates + 0.0, dataDigits) + " ± " //
								+ Formatter.format(meanMutUpdateSdev + 0.0, dataDigits) //
								+ "\t" + traitNames[restrait] + ": " //
								+ Formatter.format(meanResUpdates + 0.0, dataDigits) + " ± "
								+ Formatter.format(meanResUpdateSdev + 0.0, dataDigits) //
								+ "\tabsorption: " //
								+ Formatter.format(meanAbsUpdates + 0.0, dataDigits) + " ± "
								+ Formatter.format(meanAbsUpdatesSdev + 0.0, dataDigits) //
							);
						break;
					case STAT_TIMES:
						double meanResTimes = 0.0;
						double meanResTimeSdev = 0.0;
						double meanMutTimes = 0.0;
						double meanMutTimeSdev = 0.0;
						double meanAbsTimes = 0.0;
						double meanAbsTimeSdev = 0.0;
						output.println("# index,\t" + data.getKey() + (module.getNSpecies() > 1 ? "\tSpecies" : "")
								+ ",\tFixation and absorption times (mean, sdev, samples) of " + traitNames[muttrait]
								+ " mutant in " + traitNames[restrait] + " resident for all locations");
						for (int n = 0; n < nPopulation; n++) {
							printTimeStat(fixTime, n);
							double in1 = 1.0 / (n + 1);
							double[] fn = fixTime[n];
							double dx = fn[0] - meanMutTimes;
							meanMutTimes += dx * in1;
							// in [1] is the variance convert to sdev first
							dx = Math.sqrt(fn[1] / (fn[2] - 1.0)) - meanMutTimeSdev;
							meanMutTimeSdev += dx * in1;
							dx = fn[3] - meanResTimes;
							meanResTimes += dx * in1;
							dx = Math.sqrt(fn[3] / (fn[5] - 1.0)) - meanResTimeSdev;
							meanResTimeSdev += dx * in1;
							dx = fn[6] - meanAbsTimes;
							meanAbsTimes += dx * in1;
							dx = Math.sqrt(fn[6] / (fn[8] - 1.0)) - meanAbsTimeSdev;
							meanAbsTimeSdev += dx * in1;
						}
						// trick: to avoid -0 output simply add 0...!
						output.println("# overall:\t" + traitNames[muttrait] + ": " //
								+ Formatter.format(meanMutTimes + 0.0, dataDigits) + " ± " //
								+ Formatter.format(meanMutTimeSdev + 0.0, dataDigits) //
								+ "\t" + traitNames[restrait] + ": " //
								+ Formatter.format(meanResTimes + 0.0, dataDigits) + " ± "
								+ Formatter.format(meanResTimeSdev + 0.0, dataDigits) //
								+ "\tabsorption: " //
								+ Formatter.format(meanAbsTimes + 0.0, dataDigits) + " ± "
								+ Formatter.format(meanAbsTimeSdev + 0.0, dataDigits) //
							);
					break;
					default:
						throw new Error("Statistics for " + data.getKey() + " not supported!");
				}
			}
		} else {
			throw new Error("Mode not recognized.");
		}
		dumpEnd();
		exit(0);
	}

	/**
	 * Helper method to calculate running mean and variance for fixation
	 * updates/times. The data structure of the {@code meanvar} array is defined as
	 * follows:
	 * <ol>
	 * <li>mean of mutant fixation
	 * <li>variance of mutant fixation
	 * <li>sample count of mutant fixation
	 * <li>mean of resident fixation
	 * <li>variance of resident fixation
	 * <li>sample count of resident fixation
	 * <li>mean of absorption
	 * <li>variance of absorption
	 * <li>sample count of absorption ({@code meanvar[2] + meanvar[5] == meanvar[8]}
	 * must hold)
	 * </ol>
	 * 
	 * @param meanvar     the array that stores the running mean and variance
	 * @param mutantfixed the flag to indicate whether the mutant fixated
	 * @param x           the time/updates to fixation
	 */
	protected void updateMeanVar(double[] meanvar, boolean mutantfixed, double x) {
		int idx = mutantfixed ? 0 : 3;
		double mean = meanvar[idx];
		double dx = x - mean;
		double norm = ++meanvar[idx + 2];
		mean += dx / norm;
		meanvar[idx] = mean;
		meanvar[idx + 1] += dx * (x - mean);
		// absorption
		idx = 6;
		mean = meanvar[idx];
		dx = x - mean;
		norm = ++meanvar[idx + 2];
		mean += dx / norm;
		meanvar[idx] = mean;
		meanvar[idx + 1] += dx * (x - mean);
	}

	/**
	 * Helper method to print the fixation and absorption updates/times.
	 * 
	 * @param meanvar the 2D array that stores the running mean and variance for all
	 *                nodes
	 * @param n       the index of the node to process
	 * @see #updateMeanVar(double[], boolean, double) see updateMeanVar(double[],
	 *      boolean, double) for structure of {@code meanvar} array
	 */
	private void printTimeStat(double[][] meanvar, int n) {
		double[] node = meanvar[n];
		double normut = node[2];
		double normres = node[5];
		double normabs = node[8];
		// trick: to avoid -0 output simply add 0...!
		output.println(n + ",\t"
				+ Formatter.format(node[0] + 0.0, dataDigits) + " ± " // mutant mean fixation time
				+ Formatter.format(Math.sqrt(node[1] / (normut - 1.0)) + 0.0, dataDigits) + ", " // mutant mean fixation sdev
				+ Formatter.format(normut, 0) + "; " // mutant mean fixation samples
				+ Formatter.format(node[3] + 0.0, dataDigits) + " ± " // resident mean fixation time
				+ Formatter.format(Math.sqrt(node[4] / (normres - 1.0)) + 0.0, dataDigits) + ", " // resident mean fixation sdev
				+ Formatter.format(normres, 0) + "; " // mutant mean fixation samples
				+ Formatter.format(node[6] + 0.0, dataDigits) + " ± " // mean absorption time
				+ Formatter.format(Math.sqrt(node[7] / (normabs - 1.0)) + 0.0, dataDigits) + ", " // mean absorption sdev
				+ Formatter.format(normabs, 0)); // mean absorption samples
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
	 * {@inheritDoc} Retrieves git commit version from file in jar archive.
	 */
	@Override
	public String getGit() {
		return getProperty("git.commit.id.describe");
	}

	/**
	 * {@inheritDoc} Retrieves git commit time from file in jar archive.
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
	 * @return where output should be directed (JRE only)
	 */
	public PrintStream getOutput() {
		return output;
	}

	/**
	 * Set output for all reporting (JRE only). Used by {@link #cloOutput} and
	 * {@link #cloAppend} to redirect output to a file.
	 * 
	 * @param output stream to redirect output to.
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
	 * Name of file to restore state from.
	 */
	String plistname = null;
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
		if (cloarray == null)
			return null;
		// check if --help or --restore requested
		String helpName = cloHelp.getName();
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

	public boolean parseCLO(boolean testing) {
		boolean success = parseCLO();
		if (testing) {
			// nothing to export in testing mode
			exportname = null;
		}
		return success;
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * Special treatment of <code>--restore</code> option.
	 * </p>
	 */
	@Override
	public boolean parseCLO(String[] cloarray) {
		boolean success = super.parseCLO(cloarray);
		clo = Formatter.format(cloarray, " --");
		if (!doRestore)
			return success;
		// parseCLO does not reset model - do it now to be ready for restore
		modelReset();
		// finish restoring
		if (!restoreState(plist)) {
			logger.warning("failed to restore state in '" + plistname + "'");
			return false;
		}
		return success;
	}

	@Override
	public void helpCLO() {
		output.println("List of command line options for module '" + activeModule.getKey() + "':\n" + parser.helpCLO(true));
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
			"--output <f>              redirect output to file", new CLODelegate() {
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
			"--append <f>              append output to file", new CLODelegate() {
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
			"--restore <filename>      restore saved state from file", new CLODelegate() {
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
	public final CLOption cloExport = new CLOption("export", "evoludo-%d.plist", CLOption.Argument.OPTIONAL, catSimulation,
			"--export [<filename>]    export final state of simulation (%d for generation)", new CLODelegate() {
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
	 * @see #simulation(String[])
	 */
	ArrayList<Module.DataTypes> dataTypes;

	/**
	 * The field to specify the accurracy of data reported in simulations.
	 * 
	 * @see #simulation(String[])
	 */
	int dataDigits;

	/**
	 * Helper method to determine whether the data {@code type} requires
	 * {@link Mode#STATISTICS} or {@link Mode#DYNAMICS}.
	 * 
	 * @param type the data type to check
	 * @return {@code true} if {@code Mode#DYNAMICS}
	 */
	private boolean isDynamicsDataType(Module.DataTypes type) {
		return !(type == Module.DataTypes.STAT_PROB || type == Module.DataTypes.STAT_UPDATES || type == Module.DataTypes.STAT_TIMES);
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
						Module.DataTypes data = (Module.DataTypes) cloData.match(type);
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
					Iterator<Module.DataTypes> i = dataTypes.iterator();
					while (i.hasNext()) {
						Module.DataTypes type = i.next();
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
					getModel().setMode(isDynamic ? Mode.DYNAMICS : Mode.STATISTICS);
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

	/**
	 * Command line option to print help message for available command line options.
	 */
	public final CLOption cloHelp = new CLOption("help", catGlobal,
			"--help          print this help screen", new CLODelegate() {
				@Override
				public boolean parse(String arg) {
					if (cloHelp.isSet()) {
						helpCLO();
						exit(0); // abort
					}
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
			if (getModel().permitsMode(Mode.STATISTICS))
				prsr.addCLO(cloNSamples);
			cloData.clearKeys();
			cloData.addKeys(getModule().getAvailableDataTypes());
			prsr.addCLO(cloDigits);
		}
		prsr.addCLO(cloRestore);
		prsr.addCLO(cloHelp);
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

	public String getExportDir() {
		return (exportdir == null ? "." : exportdir);
	}

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
