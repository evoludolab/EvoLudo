//
// EvoLudo Project
//
// Copyright 2010-2026 Christoph Hauert
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

import java.io.PrintStream;
import java.util.Arrays;

import org.evoludo.math.RNGDistribution;
import org.evoludo.simulator.EvoLudo;
import org.evoludo.simulator.EvoLudoJRE;
import org.evoludo.simulator.models.ChangeListener;
import org.evoludo.simulator.models.DModel;
import org.evoludo.simulator.models.IBS;
import org.evoludo.simulator.models.IBSDPopulation;
import org.evoludo.simulator.modules.CDLP;
import org.evoludo.util.CLODelegate;
import org.evoludo.util.CLOParser;
import org.evoludo.util.CLOption;
import org.evoludo.util.CLOCategory;
import org.evoludo.util.Formatter;

/**
 * Simulations to investigate peer punishment in the voluntary public goods
 * game. This module extends the CDLP module to determine, for example, the
 * probability of fixation in each of the four homogeneous, absorbing states.
 * 
 * @author Christoph Hauert
 * 
 * @see "Hauert C., Traulsen A., Brandt H., Nowak M. A., Sigmund K. (2007)
 *      <em>Via Freedom to Coercion: The Emergence of Costly Punishment.</em>
 *      Science 316:1905-1907. doi: <a href=
 *      'https://doi.org/10.1126/science.1141588'>10.1126/science.1141588</a>"
 * @see "Hauert C., De Monte S., Hofbauer J., Sigmund K. (2002)
 *      <em>Volunteering as Red Queen Mechanism for Cooperation in Public
 *      Goods Games.</em> Science 296:1129-1132. doi: <a href=
 *      'https://doi.org/10.1126/science.1070582'>10.1126/science.1070582</a>"
 */
public class simCDLP extends CDLP implements ChangeListener {

	/**
	 * The threshold for qualifying as a corner state.
	 */
	int threshold = -1;

	/**
	 * The time to reach the punisher corner.
	 */
	long timesamples = -1;

	/**
	 * Generate a histogram of states visited.
	 */
	boolean doLocation = false;

	/**
	 * Generate the basins of attraction.
	 */
	private boolean doBasin = false;

	/**
	 * The flag to indicate whether the initial configuration is given by the
	 * interior fixed point {@code Q} (for CDL only).
	 */
	private boolean initInQ = false;

	/**
	 * The output stream. Defaults to {@code System.out}.
	 */
	PrintStream out;

	/**
	 * Create a new simulation to investigate the role of punishment in voluntary
	 * public goods games.
	 * 
	 * @param engine the pacemaker for running the model
	 */
	public simCDLP(EvoLudo engine) {
		super(engine);
	}

	@Override
	public void run() {
		if (!model.getType().isIBS()) {
			System.err.printf("ERROR: IBS model expected!");
			return;
		}
		IBS ibs = (IBS) model;
		IBSDPopulation pop = (IBSDPopulation) getIBSPopulation();
		out = ((EvoLudoJRE) engine).getOutput();

		boolean[] active = getActiveTraits();
		if (isLinearPGG && initInQ && !active[PUNISH])
			((DModel) model)
					.setInitialTraits(findQ(nGroup, getInterest(), getPayLoner()));

		double[][][] location = null;
		double[] corner = new double[nTraits];
		int[][] fix = null;
		int lastfix = -1;

		double pMutation = mutation.getProbability();
		if (pMutation > 0.0) {
			fix = new int[nTraits][nTraits];
			if (threshold < 0 || threshold > nPopulation)
				threshold = nPopulation;
		}
		if (doLocation)
			location = new double[nPopulation + 1][nPopulation + 1][nPopulation + 1];
		model.setTimeStep(1.0);
		engine.modelReset();
		resetStatistics();
		engine.writeHeader();
		double timeStop = model.getTimeStop();

		// do statistics starting from random initial configurations and determine the
		// probablility to end in each of the four corners
		if (pMutation < 1e-10) {
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
			model.setTimeStep(doBasin ? 1.0 : 10.0);
			int[] typ = types.clone();
			// retrieve the shared RNG to ensure reproducibility of results
			RNGDistribution rng = engine.getRNG();

			nextrial: for (long g = 0; g <= timeStop; g++) {
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
				((DModel) model).setInitialTraits(dinit);
				engine.modelInit();
				do {
					engine.modelNext();
					int[] tCounts = pop.getTraitsCount();
					if (doBasin) {
						if (tCounts[PUNISH] == 0)
							continue nextrial;
						if (tCounts[DEFECT] == 0 && //
								tCounts[LONER] == 0) {
							basin[COOPERATE] += (double) tCounts[COOPERATE] / (double) nPopulation;
							basin[PUNISH] += (double) tCounts[PUNISH] / (double) nPopulation;
							continue nextrial;
						}
					} else {
						for (int n = 0; n < nTraits; n++)
							if (tCounts[n] == nPopulation) {
								corner[n]++;
								continue nextrial;
							}
					}
				} while (true);
			}
			if (doBasin) {
				out.println("# absorbtion probabilities for coop-pun-edge (C P C+P)\n"
						+ Formatter.format(basin[COOPERATE] / timeStop, 6) + "\t"
						+ Formatter.format(basin[PUNISH] / timeStop, 6) + "\t"
						+ Formatter.format((basin[COOPERATE] + basin[PUNISH]) / timeStop, 6));
			} else {
				StringBuilder msg = new StringBuilder(
						"# absorbtion probabilities for each corner from random initial configuration\n");
				msg.append(Formatter.format(corner[0] / timeStop, 6));
				for (int n = 1; n < nTraits; n++)
					msg.append("\t").append(Formatter.format(corner[n] / timeStop, 6));
				out.println(msg);
			}
			return;
		}

		if (timesamples > 0) {
			double t = 0.0;
			double t2 = 0.0;
			for (int r = 0; r < timesamples; r++) {
				engine.modelReset();
				while (pop.getTraitsCount()[PUNISH] < threshold) {
					engine.modelNext();
				}
				double generation = ibs.getUpdates();
				t += generation;
				t2 += generation * generation;
			}
			double meant = t / timesamples;
			double sdevt = Math.sqrt(t2 / timesamples - meant * meant);
			out.println("# time to reach punisher corner\n" +
			// ChHFormatter.format(meant, 6)+"\t"+
			// ChHFormatter.format(sdevt, 6));
					meant + "\t" + sdevt);
			// System.out.flush();
			return;
		}

		// evolve population
		startStatistics();
		int[] tCounts = pop.getTraitsCount();
		for (long g = 0; g < timeStop; g++) {
			int c = tCounts[COOPERATE];
			int d = tCounts[DEFECT];
			int l = tCounts[LONER];
			double generation = ibs.getUpdates();
			double t = generation;
			engine.modelNext();
			double incr = generation - t;
			if (location != null) // save location
				location[c][d][l] += incr;

			for (int n = 0; n < nTraits; n++) {
				if (tCounts[n] >= threshold) {
					corner[n] += incr;
					break;
				}
			}

			for (int n = 0; n < nTraits; n++) {
				if (tCounts[n] == nPopulation && lastfix != n) {
					// fix is never null but make compiler happy
					if (fix != null && lastfix >= 0)
						fix[lastfix][n]++;
					lastfix = n;
					break;
				}
			}
		}
		double generation = ibs.getUpdates();
		double norm = 1.0 / generation;
		StringBuilder sb = new StringBuilder("# fixation probs\n# \t");
		for (int n = 0; n < nTraits; n++)
			sb.append(getTraitName(n)).append("\t");
		out.println(sb.toString());
		// fix is never null but make compiler happy
		if (fix != null) {
			sb = new StringBuilder();
			for (int n = 0; n < nTraits; n++) {
				sb.append("# ").append(getTraitName(n)).append(":\t");
				double sum = 0.0;
				for (int m = 0; m < nTraits; m++)
					sum += fix[n][m];
				for (int m = 0; m < nTraits; m++)
					sb.append(Formatter.format(fix[n][m] / sum, 4)).append("\t");
				sb.append("(count: ").append((int) sum).append(")");
				out.println(sb);
			}
		}
		sb = new StringBuilder("# corners (" + threshold + " threshold): ");
		for (int n = 0; n < nTraits; n++)
			sb.append(Formatter.formatFix(corner[n] * norm, 6)).append("\t");
		/*
		 * System.out.println("# corners ("+threshold+" threshold):"); if( doLocation )
		 * System.out.print("# "); for( int n=0; n<nTraits; n++ )
		 * System.out.print(ChHFormatter.format(corner[n]*norm, 4)+"\t");
		 */
		out.println(sb);
		sb = new StringBuilder("# long-term average:      ");
		for (int n = 0; n < nTraits; n++) {
			sb.append(Formatter.formatFix(mean[n], 6));
			sb.append("\t");
			sb.append(Formatter.format(Math.sqrt(variance[n] / (generation - 1.0)), 6));
			sb.append("\t");
		}
		/*
		 * System.out.println("\n# long-term average:"); double[] avg =
		 * getStrategyTypeMean(); for( int n=0; n<nTraits; n++ )
		 * System.out.print(ChHFormatter.format(avg[n]/(double)nPopulation, 4)+"\t");
		 */
		out.println(sb);
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
		engine.writeFooter();
		engine.exportState();
	}

	/** Temporary running mean for fixation probabilities. */
	double[] mean;

	/** Temporary running variance for fixation probabilities. */
	double[] variance;

	/** Temporary state buffer for sampling. */
	double[] state;

	/**
	 * Time of previous sample.
	 */
	double prevsample;

	@Override
	public synchronized void modelChanged(PendingAction action) {
		updateStatistics(engine.getModel().getUpdates());
	}

	@Override
	public synchronized void modelStopped() {
		updateStatistics(model.getTimeStop());
	}

	/**
	 * Start collecting statistics.
	 */
	protected void startStatistics() {
		prevsample = engine.getModel().getUpdates();
	}

	/**
	 * Reset statistics.
	 */
	protected void resetStatistics() {
		if (mean == null)
			mean = new double[nTraits];
		if (variance == null)
			variance = new double[nTraits];
		if (state == null)
			state = new double[nTraits];
		prevsample = Double.MAX_VALUE;
		Arrays.fill(mean, 0.0);
		Arrays.fill(variance, 0.0);
	}

	/**
	 * Update statistics.
	 * 
	 * @param time the current time
	 */
	protected void updateStatistics(double time) {
		if (prevsample >= time)
			return;
		model.getMeanTraits(getId(), state);
		// calculate weighted mean and sdev - see wikipedia
		double w = time - prevsample;
		double wn = w / (time);
		for (int n = 0; n < nTraits; n++) {
			double delta = state[n] - mean[n];
			mean[n] += wn * delta;
			variance[n] += w * delta * (state[n] - mean[n]);
		}
		prevsample = time;
	}

	/**
	 * Find the interior fixed point {@code Q} for the CDL model.
	 * 
	 * @param n     the maximum size of the interaction group
	 * @param r     the multiplication factor of the public good
	 * @param sigma the payoff for loners
	 * @return the interior fixed point {@code Q}
	 */
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
			}
		}
		q[LONER] = (zlow + zhigh) / 2.0;
		q[COOPERATE] = (sigma / (r - 1.0) * (1.0 - q[LONER]));
		q[DEFECT] = 1.0 - q[LONER] - q[COOPERATE];
		out.println("# init: Q=(" + q[COOPERATE] + ", " + q[DEFECT] + ", " + q[LONER] + ")");
		return q;
	}

	/**
	 * The function \(F(z)\) for the interior fixed point \(Q\).
	 * 
	 * @param z the fraction of loners
	 * @param n the maximum size of the interaction group
	 * @param r the multiplication factor of the public good
	 * @return the value of \(F(z)\)
	 */
	private double func(double z, int n, double r) {
		return 1.0 + (r - 1.0) * Math.pow(z, (n - 1)) - r * (1 - Math.pow(z, n)) / (n * (1 - z));
	}

	/**
	 * The sign function. Returns 1.0 for positive values, 0.0 for negative values,
	 * and 0.5 for zero.
	 * 
	 * @param x the value
	 * @return the sign of the value
	 */
	private double sign(double x) {
		if (x > 0.0)
			return 1.0;
		if (x < 0.0)
			return 0.0;
		return 0.5;
	}

	/**
	 * Command line option to set the threshold for corner states.
	 */
	public final CLOption cloThreshold = new CLOption("threshold", "-1", CLOCategory.Simulation,
			"--threshold <t>  threshold for corner count", new CLODelegate() {
				@Override
				public boolean parse(String arg) {
					threshold = CLOParser.parseInteger(arg);
					return true;
				}
			});

	/**
	 * Command line option to determine the basin of attraction of punishers and
	 * cooperators.
	 */
	public final CLOption cloBasin = new CLOption("basin", CLOCategory.Simulation,
			"--basin         basin of attraction - punisher vs. cooperation", new CLODelegate() {
				@Override
				public boolean parse(boolean isSet) {
					doBasin = isSet;
					return true;
				}
			});

	/**
	 * Command line option to determine the time to reach the threshold of
	 * punishers.
	 */
	public final CLOption cloTime2Punish = new CLOption("time2pun", "-1", CLOCategory.Simulation,
			"--time2pun <s>  time to reach threshold of punishers, s number of samples", new CLODelegate() {
				@Override
				public boolean parse(String arg) {
					timesamples = CLOParser.parseInteger(arg);
					return true;
				}
			});

	/**
	 * Command line option to generate a histogram of states visited.
	 */
	public final CLOption cloHistogram = new CLOption("histogram", CLOCategory.Simulation,
			"--histogram     generate histogram of states visited", new CLODelegate() {
				@Override
				public boolean parse(boolean isSet) {
					doLocation = isSet;
					return true;
				}
			});

	@Override
	public void collectCLO(CLOParser parser) {
		parser.addCLO(cloThreshold);
		parser.addCLO(cloBasin);
		parser.addCLO(cloTime2Punish);
		parser.addCLO(cloHistogram);

		model.cloTimeStop.setDefault("1000000");
		super.collectCLO(parser);
	}

	/**
	 * Main method to run the simulation.
	 * 
	 * @param args the array of command line arguments
	 */
	public static void main(String[] args) {
		EvoLudoJRE engine = new EvoLudoJRE(false);
		engine.custom(new simCDLP(engine), args);
	}
}
