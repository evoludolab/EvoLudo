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

import org.evoludo.simulator.EvoLudoJRE;
import org.evoludo.math.ArrayMath;
import org.evoludo.math.RNGDistribution;
import org.evoludo.simulator.EvoLudo;
import org.evoludo.simulator.modules.RSP;
import org.evoludo.util.CLOParser;
import org.evoludo.util.CLOption;
import org.evoludo.util.CLOption.CLODelegate;
import org.evoludo.util.Formatter;

/**
 *
 * @author Christoph Hauert
 */
public class simRSP extends RSP {

	/* additional parameters */
	int threshold = -1;
	long timesamples = -1;
	boolean doLocation = true;
	int nSamples = 100;
	int nSteps = 100;
	int[] initcount;
	PrintStream out;

	public simRSP(EvoLudo engine) {
		super(engine);
	}

	@Override
	public void run() {
		double[][][] fixprob = null;
		double[][] abstime = null;
		initcount = new int[nTraits];
		engine.setReportInterval(1.0);
		engine.modelReset();
		engine.dumpParameters();
		// retrieve the shared RNG to ensure reproducibility of results
		// even if seed was set, we need to clear the flag here otherwise subsequent
		// calls to modelReset() will keep generating the same initial configuration!
		RNGDistribution rng = engine.getRNG();
		rng.clearRNGSeed();

// TESTING PROBABILITIES AND TIME SCALES
		initcount = null;
		double[] prob = new double[nTraits];
		double[] time = new double[nTraits];
		double[] time2 = new double[nTraits];
		double[] mean = new double[nTraits];
		for (int s = 1; s <= nSamples; s++) {
			engine.modelReset();
			while (engine.modelNext())
				;
			model.getMeanTraits(getID(), mean);
			int winIdx = ArrayMath.maxIndex(mean);
			prob[winIdx]++;
			// running average and variance of absorption time
			double tn = engine.getModel().getTime();
			double d = tn - time[winIdx];
			time[winIdx] += d / s;
			time2[winIdx] += d * (tn - time[winIdx]);
		}
		for (int n = 0; n < nTraits; n++) {
			time2[n] /= (prob[n] - 1);
			time2[n] = Math.sqrt(time2[n]);
			prob[n] /= nSamples;
		}
		out.println("fix. probs: " + Formatter.format(prob, 4));
		out.println("abs. times: " + Formatter.format(time, 4));
		out.println("abs. sdevs: " + Formatter.format(time2, 4));
		engine.dumpEnd();
		engine.exportState();
		((EvoLudoJRE) engine).exit(0);
// END TESTING
		// calculate fixation probabilities for different initial configurations
//		double incr = Math.max(1.0, nPopulation*0.02);
//		double incr = Math.max(1.0, nPopulation*0.05);
		double incr = Math.max(1.0, nPopulation / (double) nSteps);
		int dim = (int) (nPopulation / incr + 0.5);
		fixprob = new double[dim + 1][dim + 1][nTraits];
		abstime = new double[dim + 1][dim + 1];
		int tot = (dim + 1) * (dim + 2) / 2, progress = 0;
		double[] dinit = new double[nTraits];
		for (int c = dim; c >= 0; c--) {
			for (int d = dim - c; d >= 0; d--) {
				initcount[ROCK] = (int) (c * incr + 0.5);
				initcount[PAPER] = (int) (d * incr + 0.5);
				initcount[SCISSORS] = nPopulation - initcount[ROCK] - initcount[PAPER];
				ArrayMath.copy(initcount, dinit);
				model.setInitialTraits(dinit);
//logger.info("init="+ChHFormatter.format(initcount)+", maxidx="+ChHMath.maxIndex(strategiesTypeCount)+", maxcount="+ChHMath.max(strategiesTypeCount));
				for (int s = 1; s <= nSamples; s++) {
					engine.modelReset();
					while (engine.modelNext())
						;
					model.getMeanTraits(getID(), mean);
					int winIdx = ArrayMath.maxIndex(mean);
					fixprob[c][d][winIdx]++;
					// running average of absorption time
					double generation = engine.getModel().getTime();
					abstime[c][d] += (generation - abstime[c][d]) / s;
				}
				System.err.printf("progress %d/%d                    \r", ++progress, tot);
				// fixation probabilities at C, D, L when starting at (c, d, l)
				ArrayMath.multiply(fixprob[c][d], 1.0 / nSamples);
			}
		}
		out.println("# absorbtion probabilities\n" + "# c\td\tl\tC\tD\tL\tT");
		for (int c = dim; c >= 0; c--) {
			for (int d = dim - c; d >= 0; d--) {
				double[] loc = fixprob[c][d];
				int ccount = (int) (c * incr + 0.5);
				int dcount = (int) (d * incr + 0.5);
				int lcount = nPopulation - ccount - dcount;
				out.println(ccount + "\t" + dcount + "\t" + lcount + "\t" + Formatter.format(loc[ROCK], 4) + "\t"
						+ Formatter.format(loc[PAPER], 4) + "\t" + Formatter.format(loc[SCISSORS], 4) + "\t"
						+ Formatter.format(abstime[c][d], 4));
			}
		}
		engine.dumpEnd();
		engine.exportState();
	}

	/*
	 * command line parsing stuff
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
	public final CLOption cloNSteps = new CLOption("steps", "20", EvoLudo.catSimulation,
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
//    public final CLOption cloThreshold = new CLOption("threshold", EvoLudo.catSimulation, "-1",
//			"--threshold <t>  threshold for corner count",
//    		new CLOSetter() {
//		    	@Override
//		    	public void parse(String arg) {
//					threshold = CLOParser.parseInteger(arg);
//		    	}
//		    	@Override
//		    	public void report() {
//					output.println("# threshold:            "+threshold);
//		    	}
//		    });
//    public final CLOption cloHistogram = new CLOption("histogram", EvoLudo.catSimulation, CLOption.Argument.NONE, "nohistogram",
//			"--histogram     generate histogram of states visited",
//    		new CLOSetter() {
//		    	@Override
//		    	public void parse(String arg) {
//					doLocation = cloHistogram.isSet();
//		    	}
//		    });

	@Override
	public void collectCLO(CLOParser parser) {
		parser.addCLO(cloNSamples);
		parser.addCLO(cloNSteps);
//		parser.addCLO(cloThreshold);
//		parser.addCLO(cloHistogram);

		engine.cloGenerations.setDefault("1000000");
		super.collectCLO(parser);
	}

	@Override
	public RSP.IBS createIBSPop() {
		return new simRSPIBS(engine);
	}

	class simRSPIBS extends RSP.IBS {

		protected simRSPIBS(EvoLudo engine) {
			super(engine);
		}

		@Override
		public void init() {
			if (initcount == null) {
				super.init();
				return;
			}

			int[] todo = new int[nPopulation];

			for (int n = 0; n < nPopulation; n++)
				todo[n] = n;
			int nTodo = nPopulation;
			strategiesTypeCount[ROCK] = initcount[ROCK];
			for (int n = 0; n < initcount[ROCK]; n++) {
				int pick = rng.random0n(nTodo);
				strategies[todo[pick]] = ROCK;
				todo[pick] = todo[--nTodo];
			}
			strategiesTypeCount[PAPER] = initcount[PAPER];
			for (int n = 0; n < initcount[PAPER]; n++) {
				int pick = rng.random0n(nTodo);
				strategies[todo[pick]] = PAPER;
				todo[pick] = todo[--nTodo];
			}
			strategiesTypeCount[SCISSORS] = initcount[SCISSORS];
			for (int n = 0; n < initcount[SCISSORS]; n++)
				strategies[todo[n]] = SCISSORS;
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
	}
}
