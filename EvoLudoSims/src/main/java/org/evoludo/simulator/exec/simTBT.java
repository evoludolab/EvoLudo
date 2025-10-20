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

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.logging.Level;

import javax.imageio.ImageIO;

import org.evoludo.graphics.AbstractGraph;
import org.evoludo.simulator.EvoLudo;
import org.evoludo.simulator.EvoLudoJRE;
import org.evoludo.simulator.models.ChangeListener;
import org.evoludo.simulator.modules.TBT;
import org.evoludo.simulator.views.MVPop2D;
import org.evoludo.util.CLOParser;
import org.evoludo.util.CLOption;
import org.evoludo.util.CLOption.CLODelegate;
import org.evoludo.util.CLOption.Category;
import org.evoludo.util.Formatter;

/**
 * Simulation of the two player, two trait model.
 * 
 * @author Christoph Hauert
 * 
 * @see "Ohtsuki, H., Hauert, C., Lieberman, E., & Nowak, M. A. (2006).
 *      <em>A simple rule for the evolution of cooperation on graphs and social
 *      networks.</em> Nature, 441(7092), 502-505.
 *      <a href='http://dx.doi.org/10.1038/nature04605'>doi:
 *      10.1038/nature04605</a>"
 */
public class simTBT extends TBT implements ChangeListener {

	/**
	 * The flag to indicate whether to show progress.
	 */
	boolean progress = false;

	/**
	 * The number of runs.
	 */
	int nRuns = 1;

	/**
	 * The scan parameters for the S-T-plane.
	 */
	double[] scanST;

	/**
	 * The scan parameters for the donation game.
	 */
	double[] scanDG;

	/**
	 * The two player, two trait model.
	 */
	org.evoludo.simulator.models.IBS ibs;

	/**
	 * The interval for saving snapshots.
	 */
	long snapinterval = 0;

	/**
	 * The prefix for snapshot filenames.
	 */
	String snapprefix = "";

	/**
	 * The output stream. Defaults to {@code System.out}.
	 */
	PrintStream out;

	/**
	 * Create a new simulation to investigate the two player, two trait model.
	 * 
	 * @param engine the pacemaker for running the model
	 */
	public simTBT(EvoLudo engine) {
		super(engine);
	}

	@Override
	public void run() {
		if (!model.getType().isIBS()) {
			System.err.printf("ERROR: IBS model expected!");
			return;
		}
		// initialize
		out = ((EvoLudoJRE) engine).getOutput();
		ibs = (org.evoludo.simulator.models.IBS) model;

		// print header
		engine.writeHeader();

		// prepare for several runs
		mean = new double[nTraits];
		var = new double[nTraits];
		state = new double[nTraits];
		progress |= (logger.getLevel().intValue() <= Level.FINE.intValue());
		boolean converged = false;

		if (scanDG != null) {
			setPayoff(1.0, COOPERATE, COOPERATE);
			setPayoff(0.0, DEFECT, DEFECT);
			StringBuilder sb = new StringBuilder("# average +/- sdev frequencies, covariance\n# r\t");
			for (int n = 0; n < nTraits; n++)
				sb.append(getTraitName(n)).append("\t");
			out.println(sb.toString());
			double r = scanDG[0];
			while (r < scanDG[1] + scanDG[2]) {
				setPayoff(-r, COOPERATE, DEFECT);
				setPayoff(1.0 + r, DEFECT, COOPERATE);
				Arrays.fill(mean, 0.0);
				Arrays.fill(var, 0.0);
				// relax population
				if (converged) {
					// restore original relaxation time
					engine.modelReset();
					converged = engine.modelRelax();
				} else {
					// if simulations did not converge, keep configuration, only reset time
					// and adjust min/max scores as well as update scores of all individuals
					// to new payoffs.
					ibs.init(true);
					// should be fine to reduce relaxation time
					double relax = model.getTimeRelax();
					model.setTimeRelax(relax * 0.5);
					converged = engine.modelRelax();
					model.setTimeRelax(relax);
				}
				prevsample = ibs.getUpdates();
				if (converged) {
					// simulations converged already - mean is current state and sdev is zero
					model.getMeanTraits(getID(), mean);
				} else {
					while ((converged = !engine.modelNext())) {
						// loop until converged (or timeend reached)
					}
				}
				sb.setLength(0);
				sb.append(Formatter.format(r, 3));
				for (int n = 0; n < nTraits; n++) {
					sb.append("\t")
							.append(Formatter.format(mean[n], 6))
							.append("\t")
							.append(Formatter.format(Math.sqrt(var[n]), 6));
				}
				out.println(sb);
				out.flush();
				if (progress)
					System.err.printf("progress %d/%d done                    \r",
							(int) ((r - scanDG[0]) / scanDG[2] + 0.5),
							(int) ((scanDG[1] - scanDG[0]) / scanDG[2] + 1.5));
				r += scanDG[2];
			}
			out.println("# generations @ end: " + Formatter.formatSci(ibs.getUpdates(), 6));
			engine.writeFooter();
			engine.exportState();
			return;
		}

		if (scanST != null) {
			setPayoff(1.0, COOPERATE, COOPERATE);
			setPayoff(0.0, DEFECT, DEFECT);
			StringBuilder sb = new StringBuilder("# average +/- sdev frequencies\n# S\tT\t");
			for (int n = 0; n < nTraits; n++)
				sb.append(getName()).append(".").append(getTraitName(n)).append("\t");
			out.println(sb);
			double s = scanST[0];
			while (s < scanST[1] + scanST[2]) {
				setPayoff(s, COOPERATE, DEFECT);
				double t = scanST[3 % scanST.length];
				while (t < scanST[4 % scanST.length] + scanST[5 % scanST.length]) {
					setPayoff(t, DEFECT, COOPERATE);
					Arrays.fill(mean, 0.0);
					Arrays.fill(var, 0.0);
					engine.modelReset();
					// relax population
					converged = engine.modelRelax();
					prevsample = ibs.getUpdates();
					if (converged) {
						// simulations converged already - mean is current state and sdev is zero
						model.getMeanTraits(getID(), mean);
					} else {
						while (engine.modelNext()) {
							// loop until converged (or timeend reached)
						}
					}
					sb.setLength(0);
					sb.append(Formatter.format(s, 2))
							.append("\t")
							.append(Formatter.format(t, 2));
					for (int n = 0; n < nTraits; n++) {
						sb.append("\t")
								.append(Formatter.format(mean[n], 6))
								.append("\t")
								.append(Formatter.format(Math.sqrt(var[n]), 6));
					}
					out.println(sb);
					out.flush();
					if (progress) {
						int stepss = (int) ((scanST[1] - scanST[0]) / scanST[2] + 1.5);
						int stepst = (int) ((scanST[4 % scanST.length] - scanST[3 % scanST.length])
								/ scanST[5 % scanST.length] + 1.5);
						System.err.printf("progress %d/%d done                    \r",
								(int) ((s - scanST[0]) / scanST[2] + 0.5) * stepst
										+ (int) ((t - scanST[3 % scanST.length]) / scanST[5 % scanST.length] + 0.5),
								stepss * stepst);
					}
					t += scanST[5 % scanST.length];
				}
				s += scanST[2];
			}
			out.println("# generations @ end: " + Formatter.formatSci(ibs.getUpdates(), 6));
			engine.writeFooter();
			engine.exportState();
			return;
		}

		meanmean = new double[nTraits];
		meanvar = new double[nTraits];
		for (int r = 0; r < nRuns; r++) {
			if (r > 0) {
				Arrays.fill(mean, 0.0);
				Arrays.fill(var, 0.0);
				engine.modelReset();
			}
			// relax population
			converged = engine.modelRelax();
			// evolve population
			prevsample = ibs.getUpdates();
			double timeStop = model.getTimeStop();
			if (converged) {
				// simulations converged already - mean is current state and sdev is zero
				model.getMeanTraits(getID(), mean);
			} else {
				for (long g = 1; g <= timeStop; g++) {
					if (snapinterval > 0 && g % snapinterval == 0) {
						// save snapshot
						saveSnapshot(AbstractGraph.SNAPSHOT_PNG);
					}
					engine.modelNext();
					if (progress && g % 1000 == 0) {
						engine.logProgress(g + "/" + timeStop + " done");
					}
				}
			}
			for (int n = 0; n < nTraits; n++) {
				meanmean[n] += mean[n];
				meanvar[n] += Math.sqrt(var[n] / (timeStop - 1));
			}
			if (snapinterval < 0)
				saveSnapshot(AbstractGraph.SNAPSHOT_PNG);
		}

		// System.out.print("# long-term average: ");
		// double[] avg = getStrategyTypeMean();
		// for( int n=0; n<nTraits; n++ )
		// System.out.print(ChHFormatter.formatFix(avg[n]/nPopulation, 6)+"\t");
		// System.out.println("");
		// System.out.println("# generations @ end:
		// "+ChHFormatter.formatSci(generation,6));
		// dumpEnd();
		// engine.exportState();
		// System.out.flush();

		StringBuilder sb = new StringBuilder("# average and sdev frequencies\n# ");
		for (int n = 0; n < nTraits; n++)
			sb.append(getTraitName(n)).append("\t");
		out.println(sb);
		sb.setLength(0);
		for (int n = 0; n < nTraits; n++)
			sb.append(Formatter.format(meanmean[n] / nRuns, 6)).append("\t")
					.append(Formatter.format(meanvar[n] / nRuns, 6)).append("\t");
		out.println(sb);
		out.println("# generations @ end: " + Formatter.formatSci(ibs.getUpdates(), 6));
		engine.writeFooter();
		engine.exportState();
	}

	/**
	 * Temporary variables for fixation probabilities and absorption times.
	 */
	double[] mean, var, state, meanmean, meanvar;

	/**
	 * Time of previous sample.
	 */
	double prevsample;

	@Override
	public synchronized void modelChanged(PendingAction pending) {
		double generation = ibs.getUpdates();
		if (model.isRelaxing() || prevsample >= generation) {
			return;
		}
		model.getMeanTraits(getID(), state);
		// calculate weighted mean and sdev - see wikipedia
		double w = generation - prevsample;
		double wn = w / (generation - model.getTimeRelax());
		for (int n = 0; n < nTraits; n++) {
			double delta = state[n] - mean[n];
			mean[n] += wn * delta;
			var[n] += w * delta * (state[n] - mean[n]);
		}
		prevsample = generation;
	}

	@Override
	public synchronized void modelStopped() {
		double generation = ibs.getUpdates();
		if (model.isRelaxing() || prevsample >= generation) {
			return;
		}
		// absorbing state reached
		model.getMeanTraits(getID(), state);
		// calculate weighted mean and sdev - see wikipedia
		double timeStop = model.getTimeStop();
		double timeRelax = model.getTimeRelax();
		double w = timeStop - (prevsample - timeRelax);
		double wn = w / timeStop;
		for (int n = 0; n < nTraits; n++) {
			double delta = state[n] - mean[n];
			mean[n] += wn * delta;
			var[n] += w * delta * (state[n] - mean[n]);
		}
		prevsample = timeStop + timeRelax;
	}

	/**
	 * Command line option to show the simulation progress.
	 */
	public final CLOption cloProgress = new CLOption("progress", Category.Simulation,
			"--progress      make noise about progress", new CLODelegate() {
				@Override
				public boolean parse(String arg) {
					progress = cloProgress.isSet();
					return true;
				}
			});

	/**
	 * Command line option to set the interval for taking snapshots.
	 */
	public final CLOption cloSnapInterval = new CLOption("snapinterval", "0", Category.Simulation,
			"--snapinterval <n>  save snapshot every n generations (-1 only at end)", new CLODelegate() {
				@Override
				public boolean parse(String arg) {
					snapinterval = CLOParser.parseLong(arg);
					return true;
				}
			});

	/**
	 * Command line option to set the prefix for snapshot filenames.
	 */
	public final CLOption cloSnapPrefix = new CLOption("snapprefix", "", Category.Simulation,
			"--snapprefix <s>  set prefix for snapshot filename", new CLODelegate() {
				@Override
				public boolean parse(String arg) {
					snapprefix = arg.trim();
					return true;
				}
			});

	/**
	 * Command line option to set the number of runs.
	 */
	public final CLOption cloRuns = new CLOption("runs", "1", Category.Simulation,
			"--runs <r>      number of repetitions", new CLODelegate() {
				@Override
				public boolean parse(String arg) {
					nRuns = CLOParser.parseInteger(arg);
					return true;
				}
			});

	/**
	 * Command line option to scan the S-T-plane.
	 */
	public final CLOption cloScanST = new CLOption("scan", "-1,2,0.05", Category.Simulation,
			"--scan <start,end,incr>  scan S-T-plane", new CLODelegate() {
				@Override
				public boolean parse(String arg) {
					if (!cloScanST.isSet())
						return true;
					scanST = CLOParser.parseVector(arg);
					return (scanST.length > 0);
				}
			});

	/**
	 * Command line option to scan the donation game.
	 */
	public final CLOption cloScanDG = new CLOption("scanDG", "0,0.2,0.02", Category.Simulation,
			"--scanDG <start,end,incr>  scan cost-to-benefit ratios in the donation game", new CLODelegate() {
				@Override
				public boolean parse(String arg) {
					if (!cloScanDG.isSet())
						return true;
					scanDG = CLOParser.parseVector(arg);
					return (scanDG.length > 0);
				}
			});

	@Override
	public void collectCLO(CLOParser parser) {
		parser.addCLO(cloProgress);
		parser.addCLO(cloSnapInterval);
		parser.addCLO(cloSnapPrefix);
		parser.addCLO(cloRuns);
		parser.addCLO(cloScanST);
		parser.addCLO(cloScanDG);

		model.cloTimeStop.setDefault("10");
		super.collectCLO(parser);
	}

	/**
	 * Save snapshot of current configuration.
	 * 
	 * @param format the format of the snapshot
	 * 
	 * @see MVPop2D
	 */
	public void saveSnapshot(int format) {
		if (format == AbstractGraph.SNAPSHOT_NONE)
			return; // do not waste our time
		File snapfile;

		switch (format) {
			case AbstractGraph.SNAPSHOT_PNG:
				int side = Math.max(16 * (int) Math.sqrt(nPopulation), 1000);
				BufferedImage snapimage = MVPop2D.getSnapshot(engine, this, side, side);
				snapfile = openSnapshot("png");
				try {
					ImageIO.write(snapimage, "PNG", snapfile);
				} catch (IOException x) {
					logger.warning("writing image to " + snapfile.getAbsolutePath() + " failed!");
					return;
				}
				logger.info("PNG image written to " + snapfile.getAbsolutePath());
				return;

			default:
				logger.warning("unknown file format (" + format + ")");
		}
	}

	/**
	 * Open file for exporting the snapshot.
	 * 
	 * @param ext the file extension
	 * @return the file for the snapshot
	 */
	protected File openSnapshot(String ext) {
		String pre = (!snapprefix.isEmpty() ? snapprefix
				: getKey() + "R" + Formatter.format(getPayoff(COOPERATE, COOPERATE), 2) + "S"
						+ Formatter.format(getPayoff(COOPERATE, DEFECT), 2) + "T"
						+ Formatter.format(getPayoff(DEFECT, COOPERATE), 2) + "P"
						+ Formatter.format(getPayoff(DEFECT, DEFECT), 2))
				+ "-t" + Formatter.format(engine.getModel().getUpdates(), 2);
		File snapfile = new File(pre + "." + ext);
		int counter = 0;
		while (snapfile.exists())
			snapfile = new File(pre + "-" + (counter++) + "." + ext);
		return snapfile;
	}

	/**
	 * Main method to run the simulation.
	 * 
	 * @param args the array of command line arguments
	 */
	public static void main(String[] args) {
		EvoLudoJRE engine = new EvoLudoJRE(false);
		engine.custom(new simTBT(engine), args);
	}
}
