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

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.logging.Level;

import javax.imageio.ImageIO;

import org.evoludo.graphics.AbstractGraph;
import org.evoludo.simulator.EvoLudoJRE;
import org.evoludo.simulator.MVPop2D;
import org.evoludo.simulator.EvoLudo;
import org.evoludo.simulator.models.Model;
import org.evoludo.simulator.modules.TBT;
import org.evoludo.util.CLOParser;
import org.evoludo.util.CLOption;
import org.evoludo.util.CLOption.CLODelegate;
import org.evoludo.util.Formatter;

/**
 *
 * @author Christoph Hauert
 */
public class simTBT extends TBT implements Model.ChangeListener, Runnable {

	// additional parameters
	boolean progress = false;
	int nRuns = 1;
	double[] scanST;
	double[] scanDG;
	org.evoludo.simulator.models.IBS ibs;

	// snapshots
	long snapinterval = 0;
	String snapprefix = "";
	PrintStream out;

	public simTBT(EvoLudo engine) {
		super(engine);
	}

	@Override
	public void run() {
		if(!model.isModelType(Model.Type.IBS)) {
			System.err.printf("ERROR: IBS model expected!");
			return;
		}
		// initialize
		out = ((EvoLudoJRE) engine).getOutput();
		ibs = (org.evoludo.simulator.models.IBS) model;

		// print header
		engine.dumpParameters();

		// prepare for several runs
		mean = new double[nTraits];
		var = new double[nTraits];
		state = new double[nTraits];
		progress |= (logger.getLevel().intValue() <= Level.FINE.intValue());
		boolean converged = false;

		if (scanDG != null) {
			setPayoff(1.0, COOPERATE, COOPERATE);
			setPayoff(0.0, DEFECT, DEFECT);
			String msg = "# average +/- sdev frequencies, covariance\n# r\t";
			for (int n = 0; n < nTraits; n++)
				msg += traitName[n] + "\t";
			out.println(msg);
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
					converged = !engine.modelRelax();
				} else {
					// if simulations did not converge, keep configuration, only reset time
					// and adjust min/max scores as well as update scores of all individuals
					// to new payoffs.
					ibs.init(true);
					// should be fine to reduce relaxation time
					double relax = engine.getNRelaxation();
					engine.setNRelaxation(relax * 0.5);
					converged = !engine.modelRelax();
					engine.setNRelaxation(relax);
				}
				prevsample = ibs.getTime();
				if (converged) {
					// simulations converged already - mean is current state and sdev is zero
					model.getMeanTrait(getID(), mean);
				} else {
					while ((converged = !engine.modelNext()))
						;
				}
				msg = Formatter.format(r, 3);
				for (int n = 0; n < nTraits; n++)
					msg += "\t" + Formatter.format(mean[n], 6) + "\t" + Formatter.format(Math.sqrt(var[n]), 6);
				out.println(msg);
				out.flush();
				if (progress)
					System.err.printf("progress %d/%d done                    \r",
							(int) ((r - scanDG[0]) / scanDG[2] + 0.5),
							(int) ((scanDG[1] - scanDG[0]) / scanDG[2] + 1.5));
				r += scanDG[2];
			}
			out.println("# generations @ end: " + Formatter.formatSci(ibs.getTime(), 6));
			engine.dumpEnd();
			return;
		}

		if (scanST != null) {
			setPayoff(1.0, COOPERATE, COOPERATE);
			setPayoff(0.0, DEFECT, DEFECT);
			String msg = "# average +/- sdev frequencies\n# S\tT\t";
			for (int n = 0; n < nTraits; n++)
				msg += getName() + "." + traitName[n] + "\t";
			out.println(msg);
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
					converged = !engine.modelRelax();
					prevsample = ibs.getTime();
					if (converged) {
						// simulations converged already - mean is current state and sdev is zero
						model.getMeanTrait(getID(), mean);
					} else {
						while (engine.modelNext())
							;
					}
					msg = Formatter.format(s, 2) + "\t" + Formatter.format(t, 2);
					for (int n = 0; n < nTraits; n++)
						msg += "\t" + Formatter.format(mean[n], 6) + "\t" + Formatter.format(Math.sqrt(var[n]), 6);
					out.println(msg);
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
			out.println("# generations @ end: " + Formatter.formatSci(ibs.getTime(), 6));
			engine.dumpEnd();
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
			converged = !engine.modelRelax();
			// evolve population
			prevsample = ibs.getTime();
			double nGenerations = engine.getNGenerations();
			if (converged) {
				// simulations converged already - mean is current state and sdev is zero
				model.getMeanTrait(getID(), mean);
			} else {
				for (long g = 1; g <= nGenerations; g++) {
					if (snapinterval > 0 && g % snapinterval == 0) {
						// save snapshot
						saveSnapshot(AbstractGraph.SNAPSHOT_PNG);
					}
					engine.modelNext();
					if (progress && g % 1000 == 0) {
						engine.logProgress(g + "/" + nGenerations + " done");
					}
				}
			}
			for (int n = 0; n < nTraits; n++) {
				meanmean[n] += mean[n];
				meanvar[n] += Math.sqrt(var[n] / (nGenerations - 1));
			}
			if (snapinterval < 0)
				saveSnapshot(AbstractGraph.SNAPSHOT_PNG);
		}

//		System.out.print("# long-term average:      ");
//		double[] avg = getStrategyTypeMean(); 
//		for( int n=0; n<nTraits; n++ )
//			System.out.print(ChHFormatter.formatFix(avg[n]/nPopulation, 6)+"\t");
//		System.out.println("");
//		System.out.println("# generations @ end: "+ChHFormatter.formatSci(generation,6)); 
//		dumpEnd(); 
//		System.out.flush();

		String msg = "# average and sdev frequencies\n# ";
		for (int n = 0; n < nTraits; n++)
			msg += traitName[n] + "\t";
		out.println(msg);
		msg = "";
		for (int n = 0; n < nTraits; n++)
			msg += Formatter.format(meanmean[n] / nRuns, 6) + "\t" + Formatter.format(meanvar[n] / nRuns, 6) + "\t";
		out.println(msg);
		out.println("# generations @ end: " + Formatter.formatSci(ibs.getTime(), 6));
		engine.dumpEnd();

	}

	double[] mean, var, state, meanmean, meanvar;
	double prevsample;

	@Override
	public synchronized void modelChanged(PendingAction pending) {
		double generation = ibs.getTime();
		if (model.relaxing() || prevsample >= generation) {
			return;
		}
		model.getMeanTrait(getID(), state);
		// calculate weighted mean and sdev - see wikipedia
		double w = generation - prevsample;
		double wn = w / (generation - engine.getNRelaxation());
		for (int n = 0; n < nTraits; n++) {
			double delta = state[n] - mean[n];
			mean[n] += wn * delta;
			var[n] += w * delta * (state[n] - mean[n]);
		}
		prevsample = generation;
	}

	@Override
	public synchronized void modelStopped() {
		double generation = ibs.getTime();
		if (model.relaxing() || prevsample >= generation) {
			return;
		}
		// absorbing state reached
		model.getMeanTrait(getID(), state);
//output.println("stop - before: generations="+generations+", prevsample="+prevsample+", state="+ChHFormatter.format(state,4)+
//	", mean="+ChHFormatter.format(mean,4)+", var="+ChHFormatter.format(var,4));
//output.println("test: mean[0].old="+mean[0]+" -> "+((mean[0]*(prevsample-relaxation)+state[0]*(generations-(prevsample-relaxation)))/generations));
		// calculate weighted mean and sdev - see wikipedia
		double nGenerations = engine.getNGenerations();
		double nRelaxation = engine.getNRelaxation();
		double w = nGenerations - (prevsample - nRelaxation);
		double wn = w / nGenerations;
		for (int n = 0; n < nTraits; n++) {
			double delta = state[n] - mean[n];
			mean[n] += wn * delta;
			var[n] += w * delta * (state[n] - mean[n]);
		}
//output.println("stop - after: mean="+ChHFormatter.format(mean,4)+", var="+ChHFormatter.format(var,4));
		prevsample = nGenerations + nRelaxation;
	}

	/*
	 * command line parsing stuff
	 */
	public final CLOption cloProgress = new CLOption("progress", EvoLudo.catSimulation,
			"--progress      make noise about progress", new CLODelegate() {
				@Override
				public boolean parse(String arg) {
					progress = cloProgress.isSet();
					return true;
				}
			});
	public final CLOption cloSnapInterval = new CLOption("snapinterval", "0", EvoLudo.catSimulation,
			"--snapinterval <n>  save snapshot every n generations (-1 only at end)", new CLODelegate() {
				@Override
				public boolean parse(String arg) {
					snapinterval = CLOParser.parseLong(arg);
					return true;
				}
			});
	public final CLOption cloSnapPrefix = new CLOption("snapprefix", "", EvoLudo.catSimulation,
			"--snapprefix <s>  set prefix for snapshot filename", new CLODelegate() {
				@Override
				public boolean parse(String arg) {
					snapprefix = new String(arg.trim());
					return true;
				}
			});
	public final CLOption cloRuns = new CLOption("runs", "1", EvoLudo.catSimulation,
			"--runs <r>      number of repetitions", new CLODelegate() {
				@Override
				public boolean parse(String arg) {
					nRuns = CLOParser.parseInteger(arg);
					return true;
				}

				@Override
				public void report(PrintStream output) {
					output.println("# runs:                 " + nRuns);
				}
			});
	public final CLOption cloScanST = new CLOption("scan", "-1,2,0.05", EvoLudo.catSimulation,
			"--scan <start,end,incr>  scan S-T-plane", new CLODelegate() {
				@Override
				public boolean parse(String arg) {
					if (!cloScanST.isSet())
						return true;
					scanST = CLOParser.parseVector(arg);
					return true;
				}

				@Override
				public void report(PrintStream output) {
					if (cloScanST.isSet())
						output.println("# scan S-T-plane:       " + Formatter.format(scanST, 4));
				}
			});
	public final CLOption cloScanDG = new CLOption("scanDG", "0,0.2,0.02", EvoLudo.catSimulation,
			"--scanDG <start,end,incr>  scan cost-to-benefit ratios in the donation game", new CLODelegate() {
				@Override
				public boolean parse(String arg) {
					if (!cloScanDG.isSet())
						return true;
					scanDG = CLOParser.parseVector(arg);
					return true;
				}

				@Override
				public void report(PrintStream output) {
					if (cloScanDG.isSet())
						output.println("# scan donation game:   " + Formatter.format(scanDG, 4));
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

		engine.cloGenerations.setDefault("10");
		super.collectCLO(parser);
	}

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

	protected File openSnapshot(String ext) {
		String pre = (snapprefix.length() > 0 ? snapprefix
				: getKey() + "R" + Formatter.format(getPayoff(COOPERATE, COOPERATE), 2) + "S"
						+ Formatter.format(getPayoff(COOPERATE, DEFECT), 2) + "T"
						+ Formatter.format(getPayoff(DEFECT, COOPERATE), 2) + "P"
						+ Formatter.format(getPayoff(DEFECT, DEFECT), 2))
				+ "-t" + Formatter.format(engine.getModel().getTime(), 2);
		File snapfile = new File(pre + "." + ext);
		int counter = 0;
		while (snapfile.exists())
			snapfile = new File(pre + "-" + (counter++) + "." + ext);
		return snapfile;
	}
}
