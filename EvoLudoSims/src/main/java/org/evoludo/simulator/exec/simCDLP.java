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

import org.evoludo.simulator.EvoLudoJRE;
import org.evoludo.math.RNGDistribution;
import org.evoludo.simulator.EvoLudo;
import org.evoludo.simulator.models.Model;
import org.evoludo.simulator.models.IBSDPopulation;
import org.evoludo.simulator.modules.CDLP;
import org.evoludo.util.CLOParser;
import org.evoludo.util.CLOption;
import org.evoludo.util.CLOption.CLODelegate;
import org.evoludo.util.Formatter;

/**
 *
 * @author Christoph Hauert
 */
public class simCDLP extends CDLP implements Model.ChangeListener {

	/* additional parameters */
	int threshold = -1;
	long timesamples = -1;
	boolean doLocation = false;
	private boolean doBasin = false;
	private boolean initInQ = false;
	PrintStream out;

	public simCDLP(EvoLudo engine) {
		super(engine);
	}

	@Override
	public void run() {
		if(!model.isModelType(Model.Type.IBS)) {
			System.err.printf("ERROR: IBS model expected!");
			return;
		}
		org.evoludo.simulator.models.IBS ibs = (org.evoludo.simulator.models.IBS) model;
		IBSDPopulation pop = (IBSDPopulation) getIBSPopulation();
		out = ((EvoLudoJRE) engine).getOutput();

		boolean[] active = getActiveTraits();
		if (isLinearPGG && initInQ && !active[PUNISH])
			model.setInitialTraits(findQ(nGroup, getInterest(), getPayLoner()));

		double[][][] location = null;
		double[] corner = new double[nTraits];
		int[][] fix = null;
		int lastfix = -1;

		if (pop.getMutationProb() > 0.0) {
			fix = new int[nTraits][nTraits];
			if (threshold < 0 || threshold > nPopulation)
				threshold = nPopulation;
		}
		if (doLocation)
			location = new double[nPopulation + 1][nPopulation + 1][nPopulation + 1];
		engine.setReportInterval(1.0);
		engine.modelReset();
		resetStatistics();
		engine.dumpParameters();
		double nGenerations = engine.getNGenerations();

		// do statistics starting from random initial configurations and determine the
		// probablility to end in each of the four corners
		if (pop.getMutationProb() < 1e-10) {
			double[] dinit = new double[nTraits];
			// form random initial configuration of population - restrict to interior
			int[] types = new int[] { COOPERATE, DEFECT, LONER, PUNISH };
			if (!active[LONER] && !active[PUNISH]) {
				types = new int[] { COOPERATE, DEFECT };
				dinit[LONER] = 0;
				dinit[PUNISH] = 0;
				doBasin = false;
			} else if (!active[LONER]) {
				types = new int[] { COOPERATE, DEFECT, PUNISH };
				dinit[LONER] = 0;
				doBasin = false;
			} else if (!active[PUNISH]) {
				types = new int[] { COOPERATE, DEFECT, LONER };
				dinit[PUNISH] = 0;
				doBasin = false;
			}
			double[] basin = new double[nTraits];
			engine.setReportInterval(doBasin ? 1.0 : 10.0);
			int[] typ = types.clone();
			// retrieve the shared RNG to ensure reproducibility of results
			RNGDistribution rng = engine.getRNG();

			nextrial: for (long g = 0; g <= nGenerations; g++) {
				int len = types.length;
				int remaining = nPopulation - len;
				System.arraycopy(types, 0, typ, 0, len);
				for (int n = 0; n < len - 1; n++) {
					int acount = remaining > 0 ? rng.random0n(remaining) : 0;
					int idx = rng.random0n(len - n);
					int atype = typ[idx];
					typ[idx] = typ[len - 1 - n];
					dinit[atype] = acount + 1;
					remaining -= acount;
				}
				dinit[typ[0]] = remaining + 1;
				model.setInitialTraits(dinit);
				engine.modelReinit();
				do {
					engine.modelNext();
					if (doBasin) {
						if (pop.strategiesTypeCount[PUNISH] == 0)
							continue nextrial;
						if (pop.strategiesTypeCount[DEFECT] == 0 && //
								pop.strategiesTypeCount[LONER] == 0) {
							basin[COOPERATE] += (double) pop.strategiesTypeCount[COOPERATE] / (double) nPopulation;
							basin[PUNISH] += (double) pop.strategiesTypeCount[PUNISH] / (double) nPopulation;
							continue nextrial;
						}
					} else {
						for (int n = 0; n < nTraits; n++)
							if (pop.strategiesTypeCount[n] == nPopulation) {
								corner[n]++;
								continue nextrial;
							}
					}
				} while (true);
			}
			if (doBasin) {
				out.println("# absorbtion probabilities for coop-pun-edge (C P C+P)\n"
						+ Formatter.format(basin[COOPERATE] / nGenerations, 6) + "\t"
						+ Formatter.format(basin[PUNISH] / nGenerations, 6) + "\t"
						+ Formatter.format((basin[COOPERATE] + basin[PUNISH]) / nGenerations, 6));
			} else {
				String msg = "# absorbtion probabilities for each corner from random initial configuration\n"
						+ Formatter.format(corner[0] / nGenerations, 6);
				for (int n = 1; n < nTraits; n++)
					msg += "\t" + Formatter.format(corner[n] / nGenerations, 6);
				out.println(msg);
			}
			return;
		}

		if (timesamples > 0) {
			double t = 0.0, t2 = 0.0;
			for (int r = 0; r < timesamples; r++) {
				engine.modelReset();
				while (pop.strategiesTypeCount[PUNISH] < threshold) {
					engine.modelNext();
				}
				double generation = ibs.getTime();
				t += generation;
				t2 += generation * generation;
			}
			double meant = t / timesamples;
			double sdevt = Math.sqrt(t2 / timesamples - meant * meant);
			out.println("# time to reach punisher corner\n" +
//				ChHFormatter.format(meant, 6)+"\t"+
//				ChHFormatter.format(sdevt, 6));
					meant + "\t" + sdevt);
//			System.out.flush();
			return;
		}

		// evolve population
		startStatistics();
		for (long g = 0; g < nGenerations; g++) {
			int c = pop.strategiesTypeCount[COOPERATE];
			int d = pop.strategiesTypeCount[DEFECT];
			int l = pop.strategiesTypeCount[LONER];
			double generation = ibs.getTime();
			double t = generation;
			engine.modelNext();
			double incr = generation - t;
			if (location != null) // save location
				location[c][d][l] += incr;

			for (int n = 0; n < nTraits; n++) {
				if (pop.strategiesTypeCount[n] >= threshold) {
					corner[n] += incr;
					break;
				}
			}

			for (int n = 0; n < nTraits; n++) {
				if (pop.strategiesTypeCount[n] == nPopulation) {
					if (lastfix != n) {
						// fix is never null but make compiler happy
						if (fix != null && lastfix >= 0)
							fix[lastfix][n]++;
						lastfix = n;
						break;
					}
				}
			}
		}
		double generation = ibs.getTime();
		double norm = 1.0 / generation;
		String msg = "# fixation probs\n# \t";
		for (int n = 0; n < nTraits; n++)
			msg += traitName[n] + "\t";
		out.println(msg);
		// fix is never null but make compiler happy
		if (fix != null) {
			for (int n = 0; n < nTraits; n++) {
				msg = "# " + traitName[n] + ":\t";
				double sum = 0.0;
				for (int m = 0; m < nTraits; m++)
					sum += fix[n][m];
				for (int m = 0; m < nTraits; m++)
					msg += Formatter.format(fix[n][m] / sum, 4) + "\t";
				out.println(msg + "(count: " + (int) sum + ")");
			}
		}
		msg = "# corners (" + threshold + " threshold): ";
		for (int n = 0; n < nTraits; n++)
			msg += Formatter.formatFix(corner[n] * norm, 6) + "\t";
		/*
		 * System.out.println("# corners ("+threshold+" threshold):"); if( doLocation )
		 * System.out.print("# "); for( int n=0; n<nTraits; n++ )
		 * System.out.print(ChHFormatter.format(corner[n]*norm, 4)+"\t");
		 */
		out.println(msg);
		msg = "# long-term average:      ";
		for (int n = 0; n < nTraits; n++)
			msg += Formatter.formatFix(mean[n], 6) + "\t" + Formatter.format(Math.sqrt(var[n] / (generation - 1.0)), 6)
					+ "\t";
		/*
		 * System.out.println("\n# long-term average:"); double[] avg =
		 * getStrategyTypeMean(); for( int n=0; n<nTraits; n++ )
		 * System.out.print(ChHFormatter.format(avg[n]/(double)nPopulation, 4)+"\t");
		 */
		out.println(msg);
		if (location != null) {
			out.println("c\td\tl\tp\tfreq");
			for (int i = 0; i <= nPopulation; i++) { // punishers
				for (int j = 0; j <= (nPopulation - i); j++) { // cooperators
					for (int k = 0; k <= (nPopulation - i - j); k++) { // defectors
						double count = location[j][k][nPopulation - i - j - k];
						if (count <= 0.0)
							continue;
						out.println(Formatter.format((double) j / (double) nPopulation, 4) + "\t"
								+ Formatter.format((double) k / (double) nPopulation, 4) + "\t"
								+ Formatter.format((double) (nPopulation - i - j - k) / (double) nPopulation, 4) + "\t"
								+ Formatter.format((double) i / (double) nPopulation, 4) + "\t"
								+ Formatter.format(count * norm, 6));
					}
				}
			}
		}
		out.println("# generations @ end: " + Formatter.formatSci(generation, 6));
		engine.dumpEnd();
		engine.exportState();
	}

	double[] mean, var, state;
	double prevsample;

	@Override
	public synchronized void modelChanged(PendingAction action) {
		updateStatistics(engine.getModel().getTime());
	}

	@Override
	public synchronized void modelStopped() {
		updateStatistics(engine.getNGenerations());
	}

	protected void startStatistics() {
		prevsample = engine.getModel().getTime();
	}

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

	protected void updateStatistics(double time) {
		if (prevsample >= time)
			return;
		model.getMeanTraits(getID(), state);
		// calculate weighted mean and sdev - see wikipedia
		double w = time - prevsample;
		double wn = w / (time);
		for (int n = 0; n < nTraits; n++) {
			double delta = state[n] - mean[n];
			mean[n] += wn * delta;
			var[n] += w * delta * (state[n] - mean[n]);
		}
		prevsample = time;
	}

	private double[] findQ(int n, double r, double sigma) {
		double[] q = new double[] { 1.0 / 3.0, 1.0 / 3.0, 1.0 / 3.0, 0.0 };
		double zlow = 0.0;
		double zhigh = 1.0;
		double flow = func(zlow, n, r);
		double fhigh = func(zhigh - 0.001, n, r);
		if (sign(flow) == sign(fhigh))
			return q;

		int nIter = 10;
		for (int i = 0; i < nIter; i++) {
			double zmid = (zlow + zhigh) / 2.0;
			double fmid = func(zmid, n, r);
			if (sign(fmid) == sign(flow)) {
				zlow = zmid;
				flow = fmid;
			} else {
				zhigh = zmid;
				fhigh = fmid;
			}
		}
		q[LONER] = (zlow + zhigh) / 2.0;
		q[COOPERATE] = (sigma / (r - 1.0) * (1.0 - q[LONER]));
		q[DEFECT] = 1.0 - q[LONER] - q[COOPERATE];
		out.println("# init: Q=(" + q[COOPERATE] + ", " + q[DEFECT] + ", " + q[LONER] + ")");
		return q;
	}

	private double func(double z, int n, double r) {
		return 1.0 + (r - 1.0) * Math.pow(z, (n - 1)) - r * (1 - Math.pow(z, n)) / (n * (1 - z));
	}

	private double sign(double x) {
		return x > 0.0 ? 1.0 : (x < 0.0 ? 0.0 : 0.5);
	}

	/*
	 * command line parsing stuff
	 */
	public final CLOption cloThreshold = new CLOption("threshold", "-1", EvoLudo.catSimulation,
			"--threshold <t>  threshold for corner count", new CLODelegate() {
				@Override
				public boolean parse(String arg) {
					threshold = CLOParser.parseInteger(arg);
					return true;
				}

				@Override
				public void report(PrintStream output) {
					output.println("# threshold:            " + threshold);
				}
			});
	public final CLOption cloBasin = new CLOption("basin", EvoLudo.catSimulation,
			"--basin         basin of attraction - punisher vs. cooperation", new CLODelegate() {
				@Override
				public boolean parse(String arg) {
					doBasin = cloBasin.isSet();
					return true;
				}
			});
	public final CLOption cloTime2Punish = new CLOption("time2pun", "-1", EvoLudo.catSimulation,
			"--time2pun <s>  time to reach threshold of punishers, s number of samples", new CLODelegate() {
				@Override
				public boolean parse(String arg) {
					timesamples = CLOParser.parseInteger(arg);
					return true;
				}

				@Override
				public void report(PrintStream output) {
					output.println(timesamples > 0 ? "# samples:              " + timesamples : "");
				}
			});
	public final CLOption cloHistogram = new CLOption("histogram", EvoLudo.catSimulation,
			"--histogram     generate histogram of states visited", new CLODelegate() {
				@Override
				public boolean parse(String arg) {
					doLocation = cloHistogram.isSet();
					return true;
				}
			});

	@Override
	public void collectCLO(CLOParser parser) {
		parser.addCLO(cloThreshold);
		parser.addCLO(cloBasin);
		parser.addCLO(cloTime2Punish);
		parser.addCLO(cloHistogram);

		engine.cloGenerations.setDefault("1000000");
		super.collectCLO(parser);
	}
}
