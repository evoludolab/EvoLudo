//
// EvoLudo Project
//
// Copyright 2010-2020 Christoph Hauert
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

import java.io.PrintStream;
import java.util.Arrays;
import java.util.logging.Level;

import org.evoludo.math.ArrayMath;
import org.evoludo.simulator.EvoLudo;
import org.evoludo.simulator.EvoLudoJRE;
import org.evoludo.simulator.models.ChangeListener;
import org.evoludo.simulator.modules.CDL;
import org.evoludo.util.CLOParser;
import org.evoludo.util.CLOption;
import org.evoludo.util.CLOption.CLODelegate;
import org.evoludo.util.Formatter;

/**
 * Simulations to investigate the role of volunteering in public goods games.
 * 
 * @author Christoph Hauert
 * 
 * @see "Hauert C., De Monte S., Hofbauer J., Sigmund K. (2002)
 *      <em>Volunteering as Red Queen Mechanism for Cooperation in Public
 *      Goods Games.</em> Science 296:1129-1132. doi: <a href=
 *      'https://doi.org/10.1126/science.1070582'>10.1126/science.1070582</a>"
 */
public class simCDL extends CDL implements ChangeListener {

	/**
	 * The flag to indicate whether to show progress.
	 */
	boolean progress = false;

	/**
	 * The number of samples for fixation probabilities.
	 */
	int nSamples = 100;

	/**
	 * The number of steps for initial frequencies.
	 */
	int nSteps = 100;

	/**
	 * The scan range for non-linearity in public goods games.
	 */
	double[] scanNL;

	/**
	 * The output stream. Defaults to {@code System.out}.
	 */
	PrintStream out;

	/**
	 * Create a new simulation to investigate the role of volunteering in public
	 * goods games.
	 * 
	 * @param engine the pacemaker for running the model
	 */
	public simCDL(EvoLudo engine) {
		super(engine);
	}

	/**
	 * The initial counts for the different strategies.
	 */
	int[] initcount;

	@Override
	public void run() {
		if (!model.isIBS()) {
			System.err.printf("ERROR: IBS model expected!");
			return;
		}
		out = ((EvoLudoJRE) engine).getOutput();
		double[][][] fixprob = null;
		double[][] abstime = null;
		engine.setReportInterval(1.0);
		engine.modelReset();
		engine.dumpParameters();

		// TESTING PROBABILITIES AND TIME SCALES
		// initcount = null;
		// double[] prob = new double[nTraits];
		// double[] time = new double[nTraits];
		// double[] time2 = new double[nTraits];
		// for (int s = 1; s <= nSamples; s++) {
		// modelReset();
		// while (modelNext()) ;
		// int winIdx;
		// switch( modelType ) {
		// case ODE:
		// winIdx = ChHMath.maxIndex(ode.getState());
		// break;
		// case PDE:
		// throw new Error("not implemented...");
		// case SDE:
		// winIdx = ChHMath.maxIndex(sde.getState());
		// break;
		// case SIMULATION:
		// default:
		// winIdx = ChHMath.maxIndex(strategiesTypeCount);
		// }
		// prob[winIdx]++;
		// // running average and variance of absorption time
		// double tn = generation;
		// double d = tn-time[winIdx];
		// time[winIdx] += d/s;
		// time2[winIdx] += d*(tn-time[winIdx]);
		// }
		// for( int n=0; n<nTraits; n++ ) {
		// time2[n] /= (prob[n]-1);
		// time2[n] = Math.sqrt(time2[n]);
		// prob[n] /= nSamples;
		// }
		// output.println("fix. probs: "+ChHFormatter.format(prob, 4));
		// output.println("abs. times: "+ChHFormatter.format(time, 4));
		// output.println("abs. sdevs: "+ChHFormatter.format(time2, 4));
		// engine.dumpEnd();
		// engine.exportState();
		// ((EvoLudoJRE) engine).exit(0);
		// END TESTING
		// calculate fixation probabilities for different initial configurations
		// double incr = Math.max(1.0, nPopulation*0.02);
		// double incr = Math.max(1.0, nPopulation*0.05);

		double nGenerations = engine.getNGenerations();
		double nRelaxation = engine.getNRelaxation();
		if (nSteps > 0) {
			// even if seed was set, we need to clear the flag here otherwise subsequent
			// calls to modelReset() will keep generating the same initial configuration!
			engine.getRNG().clearRNGSeed();
			double incr = Math.max(1.0, nPopulation / (double) nSteps);
			int dim = (int) (nPopulation / incr + 0.5);
			fixprob = new double[dim + 1][dim + 1][nTraits];
			abstime = new double[dim + 1][dim + 1];
			state = new double[nTraits];
			int tot = (dim + 1) * (dim + 2) / 2, done = 0;
			initcount = new int[nTraits];
			resetStatistics();
			for (int c = dim; c >= 0; c--) {
				for (int d = dim - c; d >= 0; d--) {
					double[] dinit = new double[nTraits];
					model.getInitialTraits(dinit);
					if (dim == 0) {
						initcount[COOPERATE] = (int) (dinit[COOPERATE] * nPopulation + 0.5);
						initcount[DEFECT] = (int) (dinit[DEFECT] * nPopulation + 0.5);
					} else {
						initcount[COOPERATE] = (int) (c * incr + 0.5);
						initcount[DEFECT] = (int) (d * incr + 0.5);
					}
					initcount[LONER] = nPopulation - initcount[COOPERATE] - initcount[DEFECT];
					if (initcount[LONER] < 0) {
						// rounding error can cause this (e.g. c=d=10, nPopulation=99, nSteps=20)
						initcount[LONER] = 0;
						initcount[(LONER + 1) % nTraits]--;
					}
					ArrayMath.copy(initcount, dinit);
					model.setInitialTraits(dinit);
					for (int s = 1; s <= nSamples; s++) {
						engine.modelReset();
						while (engine.modelNext())
							;
						// if nGenerations was reached, ODE, SDE or simulations may not yet have been
						// absorbed
						// in particular if interior fixed point is attractor
						model.getMeanTraits(getID(), state);
						int winIdx = ArrayMath.maxIndex(state);
						if (state[winIdx] > 0.999)
							// homogeneous state
							fixprob[c][d][winIdx]++;
						else
							// stable mixed state
							ArrayMath.add(fixprob[c][d], state);
						// running average of absorption/convergence time
						double time = engine.getModel().getTime();
						if (nGenerations > 0 && Math.abs(time - nGenerations) < 1e-8)
							// emergency brake triggered
							abstime[c][d] = Double.POSITIVE_INFINITY;
						else
							abstime[c][d] += (time - abstime[c][d]) / s;
					}
					if (nSamples > 1)
						System.err.printf("progress %d/%d                    \r", ++done, tot);
					// fixation probabilities at C, D, L when starting at (c, d, l)
					ArrayMath.multiply(fixprob[c][d], 1.0 / nSamples);
				}
			}
			out.println("# absorption probabilities\n" + "# c\td\tl\tC\tD\tL\tT");
			int[] count = new int[nTraits];
			for (int c = dim; c >= 0; c--) {
				for (int d = dim - c; d >= 0; d--) {
					double[] loc = fixprob[c][d];
					double[] dinit = new double[nTraits];
					model.getInitialTraits(dinit);
					if (dim == 0) {
						count[COOPERATE] = (int) (dinit[COOPERATE] * nPopulation + 0.5);
						count[DEFECT] = (int) (dinit[DEFECT] * nPopulation + 0.5);
					} else {
						count[COOPERATE] = (int) (c * incr + 0.5);
						count[DEFECT] = (int) (d * incr + 0.5);
					}
					count[LONER] = nPopulation - count[COOPERATE] - count[DEFECT];
					if (count[LONER] < 0) {
						count[LONER] = 0;
						count[(LONER + 1) % nTraits]--;
					}
					out.println(count[COOPERATE] + "\t" + count[DEFECT] + "\t" + count[LONER] + "\t"
							+ Formatter.format(loc[COOPERATE], 4) + "\t" + Formatter.format(loc[DEFECT], 4) + "\t"
							+ Formatter.format(loc[LONER], 4) + "\t" + Formatter.format(abstime[c][d], 4));
				}
			}
			engine.dumpEnd();
			engine.exportState();
			((EvoLudoJRE) engine).exit(0);
		}
		if (scanNL != null) {
			mean = new double[nTraits];
			var = new double[nTraits];
			state = new double[nTraits];
			progress |= (logger.getLevel().intValue() <= Level.FINE.intValue());
			int tot = (int) ((scanNL[1] - scanNL[0]) / scanNL[2]) + 1, done = 0;
			nGenerations += nRelaxation;
			out.println("# average frequencies\n# a\tr\tL\tD\tC\tT");
			double a = scanNL[0];
			while (Math.abs(a) < Math.abs(scanNL[1] + scanNL[2])) {
				double r = (interest(1) + interest(nGroup)) * 0.5;
				setInterest(r - a, r + a);
				engine.modelReset();
				resetStatistics();
				// relax population
				boolean converged = !engine.modelRelax();
				startStatistics();
				if (!converged) {
					while (engine.modelNext())
						;
				}
				if (model.hasConverged()) {
					// model converged (ODE only with mu>0) - mean is current state and sdev is zero
					model.getMeanTraits(getID(), mean);
					Arrays.fill(var, 0.0);
				}
				String msg = Formatter.format(a, 4) + "\t" + Formatter.format(r, 4);
				double time = Math.max(engine.getModel().getTime(), nGenerations);
				for (int n = 0; n < nTraits; n++)
					msg += "\t" + Formatter.format(mean[n], 6) + "\t"
							+ Formatter.format(Math.sqrt(var[n] / (time - nRelaxation - 1.0)), 6);
				out.println(msg);
				out.flush();
				done++;
				if (progress)
					System.err.printf("progress %d/%d done                    \r", done, tot);
				a += scanNL[2];
			}
			engine.dumpEnd();
			engine.exportState();
			((EvoLudoJRE) engine).exit(0);
		}
	}

	/*
	 * Temporary variables for fixation probabilities and absorption times.
	 */
	double[] mean, var, state, meanmean, meanvar;
	double prevsample;

	@Override
	public synchronized void modelChanged(PendingAction action) {
		updateStatistics(engine.getModel().getTime());
	}

	@Override
	public synchronized void modelStopped() {
		updateStatistics(engine.getNGenerations() + engine.getNRelaxation());
	}

	/**
	 * Start collecting statistics.
	 */
	protected void startStatistics() {
		prevsample = engine.getModel().getTime();
	}

	/**
	 * Reset statistics.
	 */
	protected void resetStatistics() {
		if (mean == null)
			mean = new double[nTraits];
		if (var == null)
			var = new double[nTraits];
		if (state == null)
			state = new double[nTraits];
		prevsample = Double.MAX_VALUE;
		Arrays.fill(mean, 0.0);
		Arrays.fill(var, 0.0);
	}

	/**
	 * Update statistics.
	 * 
	 * @param time the current time
	 */
	protected void updateStatistics(double time) {
		if (prevsample >= time)
			return;
		model.getMeanTraits(getID(), state);
		// calculate weighted mean and sdev - see wikipedia
		double w = time - prevsample;
		double wn = w / (time - engine.getNRelaxation());
		for (int n = 0; n < nTraits; n++) {
			double delta = state[n] - mean[n];
			mean[n] += wn * delta;
			var[n] += w * delta * (state[n] - mean[n]);
		}
		prevsample = time;
	}

	@Override
	public boolean check() {
		boolean doReset = super.check();
		if (model.isODE() && nSamples > 1) {
			logger.warning("ODE models are deterministic, no point in taking multiple samples.");
			nSamples = 1;
		}
		return doReset;
	}

	/*
	 * private double[] findQ(int n, double r, double sigma) { double[] q = new
	 * double[] {1.0/3.0, 1.0/3.0, 1.0/3.0, 0.0}; double zlow = 0.0; double zhigh =
	 * 1.0; double flow = func(zlow, n, r); double fhigh = func(zhigh-0.001, n, r);
	 * if( sign(flow) == sign(fhigh) ) return q;
	 * 
	 * int nIter = 10; for( int i=0; i<nIter; i++ ) { double zmid =
	 * (zlow+zhigh)/2.0; double fmid = func(zmid, n, r); if( sign(fmid) ==
	 * sign(flow) ) { zlow = zmid; flow = fmid; } else { zhigh = zmid; fhigh = fmid;
	 * } } q[LONER] = (zlow+zhigh)/2.0; q[COOPERATE] =
	 * (sigma/(r-1.0)*(1.0-q[LONER])); q[DEFECT] = 1.0-q[LONER]-q[COOPERATE];
	 * output.println("# init: Q=("+q[COOPERATE]+", "+q[DEFECT]+", "+q[LONER]+")");
	 * return q; }
	 * 
	 * private double func(double z, int n, double r) { return
	 * 1.0+(r-1.0)*Math.pow(z, (n-1))-r*(1-Math.pow(z, n))/(n*(1-z)); }
	 * 
	 * private double sign(double x) { return x>0.0?1.0:(x<0.0?0.0:0.5); }
	 */

	/**
	 * Command line option to set the number of samples for fixation probabilities.
	 */
	public final CLOption cloNSamples = new CLOption("samples", "100", EvoLudo.catSimulation,
			"--samples       number of samples for fixation probs", new CLODelegate() {
				@Override
				public boolean parse(String arg) {
					nSamples = CLOParser.parseInteger(arg);
					return true;
				}

				@Override
				public void report(PrintStream output) {
					output.println("# samples:              " + nSamples);
				}
			});

	/**
	 * Command line option to set the number of steps for initial frequencies.
	 */
	public final CLOption cloNSteps = new CLOption("steps", "0", EvoLudo.catSimulation,
			"--steps         number of steps for initial frequencies", new CLODelegate() {
				@Override
				public boolean parse(String arg) {
					nSteps = CLOParser.parseInteger(arg);
					return true;
				}

				@Override
				public void report(PrintStream output) {
					output.println("# steps:                " + nSteps);
				}
			});

	/**
	 * Command line option to set the range and increments for scanning
	 * non-linearities.
	 */
	public final CLOption cloScanNL = new CLOption("scanNL", "-2.5,2.5,0.5", EvoLudo.catSimulation,
			"--scanNL <start,end,incr>  scan non-linearity of PGG", new CLODelegate() {
				@Override
				public boolean parse(String arg) {
					if (!cloScanNL.isSet())
						return true;
					scanNL = CLOParser.parseVector(arg);
					return true;
				}

				@Override
				public void report(PrintStream output) {
					if (cloScanNL.isSet())
						output.println("# scan non-linear PGG:  " + Formatter.format(scanNL, 4));
				}
			});

	/**
	 * Command line option to show the simulation progress.
	 */
	public final CLOption cloProgress = new CLOption("progress", EvoLudo.catSimulation,
			"--progress      make noise about progress", new CLODelegate() {
				@Override
				public boolean parse(String arg) {
					progress = cloProgress.isSet();
					return true;
				}
			});

	@Override
	public void collectCLO(CLOParser parser) {
		parser.addCLO(cloNSamples);
		parser.addCLO(cloNSteps);
		parser.addCLO(cloScanNL);
		parser.addCLO(cloProgress);

		engine.cloGenerations.setDefault("1000000");
		super.collectCLO(parser);
	}

	@Override
	public CDL.IBS createIBSPop() {
		return new simCDLIBS(engine);
	}

	/**
	 * The simulation for the CDL module. This class adds specific initializations
	 * for measuring fixation probabilities and absorption times for given initial
	 * strategy frequencies.
	 */
	class simCDLIBS extends CDL.IBS {

		/**
		 * Create a new simulation.
		 * 
		 * @param engine the pacemaker for running the model
		 */
		protected simCDLIBS(EvoLudo engine) {
			super(engine);
		}

		@Override
		public void init() {
			super.init();
			int[] todo = new int[nPopulation];
			for (int n = 0; n < nPopulation; n++)
				todo[n] = n;
			int nTodo = nPopulation;
			strategiesTypeCount[CDL.COOPERATE] = initcount[CDL.COOPERATE];
			for (int n = 0; n < initcount[CDL.COOPERATE]; n++) {
				int pick = rng.random0n(nTodo);
				strategies[todo[pick]] = CDL.COOPERATE;
				todo[pick] = todo[--nTodo];
			}
			strategiesTypeCount[CDL.DEFECT] = initcount[CDL.DEFECT];
			for (int n = 0; n < initcount[CDL.DEFECT]; n++) {
				// logger.info("setStrategyCount: nTodo="+nTodo+",
				// todo="+ChHFormatter.format(todo));
				int pick = rng.random0n(nTodo);
				strategies[todo[pick]] = CDL.DEFECT;
				todo[pick] = todo[--nTodo];
			}
			strategiesTypeCount[CDL.LONER] = initcount[CDL.LONER];
			for (int n = 0; n < initcount[CDL.LONER]; n++)
				strategies[todo[n]] = CDL.LONER;
			resetStatistics();
		}
	}
}