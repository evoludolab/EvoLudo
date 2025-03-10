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

import javax.imageio.ImageIO;

import org.evoludo.graphics.AbstractGraph;
import org.evoludo.simulator.EvoLudo;
import org.evoludo.simulator.EvoLudoJRE;
import org.evoludo.simulator.models.ChangeListener;
import org.evoludo.simulator.models.IBSDPopulation;
import org.evoludo.simulator.modules.CDLPQ;
import org.evoludo.simulator.views.MVPop2D;
import org.evoludo.util.CLOParser;
import org.evoludo.util.CLOption;
import org.evoludo.util.CLOption.CLODelegate;
import org.evoludo.util.Formatter;

/**
 * Simulations of peer- versus pool-punishment in voluntary public goods games.
 * 
 * @author Christoph Hauert
 * 
 * @see "Sigmund K., De Silva H., Traulsen A., Hauert C. (2010)
 *      <em>Social learning promotes institutions for governing the
 *      commons.</em> Nature 466: 861-863.
 *      <a href='https://doi.org/10.1038/nature09203'>doi:
 *      10.1038/nature09203</a>"
 */
public class simCDLPQ extends CDLPQ implements ChangeListener {

	// snapshots
	/**
	 * The interval for saving snapshots.
	 */
	long snapinterval = 1;

	/**
	 * The output stream. Defaults to {@code System.out}.
	 */
	PrintStream out;

	/**
	 * Create a new simulation to investigate the role of pool- versus
	 * peer-punishment in voluntary public goods games.
	 * 
	 * @param engine the pacemaker for running the model
	 */
	public simCDLPQ(EvoLudo engine) {
		super(engine);
	}

	@Override
	public void run() {
		if (!model.isIBS()) {
			System.err.printf("ERROR: IBS model expected!");
			return;
		}
		org.evoludo.simulator.models.IBS ibs = (org.evoludo.simulator.models.IBS) model;
		IBSDPopulation pop = (IBSDPopulation) getIBSPopulation();
		out = ((EvoLudoJRE) engine).getOutput();
		int[][] fix = new int[nTraits][nTraits];
		int lastfix = -1;

		model.setTimeStep(1.0);
		engine.modelReset();
		resetStatistics();

		// print header
		engine.dumpParameters();

		// evolve population
		startStatistics();
		double timeStop = model.getTimeStop();
		for (long g = 0; g < timeStop; g++) {
			if (g % snapinterval == 0) {
				// save snapshot
				saveSnapshot(AbstractGraph.SNAPSHOT_PNG);
			}
			engine.modelNext();
			for (int n = 0; n < nTraits; n++) {
				if (pop.strategiesTypeCount[n] == nPopulation) {
					if (lastfix != n) {
						if (lastfix >= 0)
							fix[lastfix][n]++;
						lastfix = n;
						break;
					}
				}
			}
		}
		String msg = "# fixation probs\n# \t";
		for (int n = 0; n < nTraits; n++)
			msg += getTraitName(n) + "\t";
		out.println(msg);
		for (int n = 0; n < nTraits; n++) {
			msg = "# " + getTraitName(n) + ":\t";
			double sum = 0.0;
			for (int m = 0; m < nTraits; m++)
				sum += fix[n][m];
			for (int m = 0; m < nTraits; m++)
				msg += Formatter.format(fix[n][m] / sum, 4) + "\t";
			out.println(msg + "(count: " + (int) sum + ")");
		}
		double generation = ibs.getTime();
		msg = "# long-term average:      ";
		for (int n = 0; n < nTraits; n++)
			msg += Formatter.formatFix(mean[n], 6) + "\t" + Formatter.format(Math.sqrt(var[n] / (generation - 1.0)), 6)
					+ "\t";
		out.println(msg);

		out.println("# generations @ end: " + Formatter.formatSci(generation, 6));
		engine.dumpEnd();
		engine.exportState();
	}

	/**
	 * Temporary variables for fixation probabilities and absorption times.
	 */
	double[] mean, var, state;

	/**
	 * Time of previous sample.
	 */
	double prevsample;

	@Override
	public synchronized void modelChanged(PendingAction action) {
		updateStatistics(model.getTime());
	}

	@Override
	public synchronized void modelStopped() {
		updateStatistics(model.getTimeStop());
	}

	/**
	 * Start collecting statistics.
	 */
	protected void startStatistics() {
		prevsample = model.getTime();
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
		double wn = w / (time);
		for (int n = 0; n < nTraits; n++) {
			double delta = state[n] - mean[n];
			mean[n] += wn * delta;
			var[n] += w * delta * (state[n] - mean[n]);
		}
		prevsample = time;
	}

	/**
	 * Command line option to set the interval for taking snapshots.
	 */
	public final CLOption cloSnapInterval = new CLOption("snapinterval", "1000", EvoLudo.catSimulation,
			"--snapinterval <n>  save snapshot every n generations", new CLODelegate() {
				@Override
				public boolean parse(String arg) {
					snapinterval = CLOParser.parseLong(arg);
					return true;
				}
			});

	@Override
	public void collectCLO(CLOParser parser) {
		// prepare command line options
		parser.addCLO(cloSnapInterval);

		model.cloTimeStop.setDefault("1000000");
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
				out.println("image written to " + snapfile.getAbsolutePath());
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
		String pre = getKey() + "-t" + Formatter.format(engine.getModel().getTime(), 2);
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
		engine.custom(new simCDLPQ(engine), args);
	}
}
