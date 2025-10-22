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

import java.io.PrintStream;
import java.text.DecimalFormat;

import org.evoludo.math.Distributions;
import org.evoludo.simulator.EvoLudoJRE;
import org.evoludo.simulator.models.FixationData;
import org.evoludo.simulator.models.Mode;
import org.evoludo.simulator.models.Type;
import org.evoludo.simulator.modules.Moran;
import org.evoludo.util.CLOParser;
import org.evoludo.util.CLOption;
import org.evoludo.util.CLOption.CLODelegate;
import org.evoludo.util.CLOption.Category;
import org.evoludo.util.Formatter;

/**
 * Simulations to investigate the Moran process on graph structured populations.
 * 
 * @author Christoph Hauert
 * 
 * @see "Lieberman, E., Hauert, C., & Nowak, M. A. (2005).
 *      <em>Evolutionary dynamics on graphs.</em>Nature, 433(7023), 312-316.
 *      <a href='http://dx.doi.org/10.1038/nature03204'>doi:
 *      10.1038/nature03204</a>"
 */
public class simMoran extends Moran {

	/**
	 * The flag to indicate whether to show progress.
	 */
	boolean progress = false;

	/**
	 * The output stream. Defaults to {@code System.out}.
	 */
	PrintStream out;

	/**
	 * The EvoLudoJRE engine for running the simulation. This is a convenience field
	 * that saves us casting engine to EvoLudoJRE every time we need to access its
	 * methods.
	 */
	EvoLudoJRE jrengine;

	/**
	 * Create a new simulation to investigate fixation probabilities and times in
	 * the Moran process.
	 * 
	 * @param engine the pacemaker for running the model
	 */
	public simMoran(EvoLudoJRE engine) {
		super(engine);
		out = engine.getOutput();
		jrengine = engine;
	}

	@Override
	public Type[] getModelTypes() {
		return new Type[] { Type.IBS, Type.SDE };
	}

	@Override
	public void run() {
		// set statistics mode
		model.requestMode(Mode.STATISTICS_SAMPLE);
		long nextReport = -1;
		long msecStart = System.currentTimeMillis();

		if (progress)
			nextReport = 10;

		// TEST fix times
		// RESULT excellent agreement with analytics
		// {
		// int mFix = 0;
		// double mFixMean = 0.0, mFixSumVar = 0.0, mAbsMean = 0.0, mAbsSumVar = 0.0;
		// for( long r=1; r<=nSamples; r++ ) {
		// int nUpdates = 0;
		// int nMut = 1;
		// double fitRes = mapToFitness(traitScores[RESIDENT]);
		// double fitMut = mapToFitness(traitScores[MUTANT]);
		// do {
		// // the first nMut individuals are mutants
		// double totFit = fitRes*(nPopulation-nMut)+fitMut*nMut;
		// if( random01()*totFit-fitMut*nMut>0.0 ) {
		// // resident picked
		// if( random0n(nPopulation)<nMut ) nMut--; // mutant replaced
		// }
		// else {
		// // mutant picked
		// if( random0n(nPopulation)>=nMut ) nMut++; // resident replaced
		// }
		// nUpdates++;
		// }
		// while( nMut>0 && nMut<nPopulation );
		// if( nMut==nPopulation ) {
		// mFix++;
		// double delta = nUpdates-mFixMean;
		// mFixMean += delta/mFix;
		// mFixSumVar += delta*(nUpdates-mFixMean);
		// }
		// double delta = nUpdates-mAbsMean;
		// mAbsMean += delta/r;
		// mAbsSumVar += delta*(nUpdates-mAbsMean);
		// }
		// double rhoA1 = rhoA(1);
		// output.println("# r\tfixation prob\t(analytical)\tfixation
		// time\t(analytical)\tabsorbtion time\t(analytical)");
		// output.println(ChHFormatter.formatFix(getFitness()[MUTANT], 2)+"\t"+
		// ChHFormatter.formatFix((double)mFix/nSamples,
		// 8)+"\t"+ChHFormatter.formatFix(rhoA1, 8)+"\t"+
		// ChHFormatter.formatFix(mFixMean, 8)+" ± "+
		// ChHFormatter.formatFix(Math.sqrt(mFixSumVar/(mFix-1)),
		// 8)+"\t"+ChHFormatter.formatFix(tA1(), 8)+"\t"+
		// ChHFormatter.formatFix(mAbsMean, 8)+" ± "+
		// ChHFormatter.formatFix(Math.sqrt(mAbsSumVar/(nSamples-1)),
		// 8)+"\t"+ChHFormatter.formatFix(t1(rhoA1), 8));
		// }
		// END TEST

		// print header
		engine.writeHeader();
		if (engine.cloSeed.isSet()) {
			// RNG seed is set. now clear seed to obtain reproducible statistics
			// rather just a single data point repeatedly
			engine.getRNG().clearSeed();
		}

		long nSamples = (long) engine.getModel().getNSamples();
		double[][] fixProb = new double[nPopulation][2];
		double[][][] fixUpdate = new double[nPopulation][2][3];
		double[][][] fixTime = new double[nPopulation][2][3];
		double[][] absUpdate = new double[nPopulation][3];
		double[][] absTime = new double[nPopulation][3];
		double[][] fixTotUpdate = new double[2][3];
		double[][] fixTotTime = new double[2][3];
		double[] absTotUpdate = new double[3];
		double[] absTotTime = new double[3];
		// evolve population
		for (long r = 1; r <= nSamples; r++) {
			FixationData fixData = jrengine.generateSample();
			int typeFixed = (fixData.typeFixed == fixData.mutantTrait ? 0 : 1);
			fixProb[fixData.mutantNode][typeFixed]++;
			Distributions.pushMeanVar(fixUpdate[fixData.mutantNode][typeFixed], fixData.updatesFixed);
			Distributions.pushMeanVar(absUpdate[fixData.mutantNode], fixData.updatesFixed);
			Distributions.pushMeanVar(fixTime[fixData.mutantNode][typeFixed], fixData.timeFixed);
			Distributions.pushMeanVar(absTime[fixData.mutantNode], fixData.timeFixed);
			Distributions.pushMeanVar(fixTotUpdate[typeFixed], fixData.updatesFixed);
			Distributions.pushMeanVar(absTotUpdate, fixData.updatesFixed);
			Distributions.pushMeanVar(fixTotTime[typeFixed], fixData.timeFixed);
			Distributions.pushMeanVar(absTotTime, fixData.timeFixed);

			if (progress && nextReport == r) {
				out.println("# runs: " + r + ", " + msecToString(System.currentTimeMillis() - msecStart));
				nextReport *= 10;
			}
		}
		double meanFix = 0.0;
		double varFix = 0.0;
		double count = 0.0;
		for (int n = 0; n < nPopulation; n++) {
			double[] node = fixProb[n];
			double norm = node[0] + node[1];
			if (norm <= 0.0)
				continue; // no samples for node n
			double inorm = 1.0 / norm;
			double n0 = node[0] * inorm;
			double dx = n0 - meanFix;
			meanFix += dx / (++count);
			varFix += dx * (n0 - meanFix);
		}
		double rhoA1 = rhoA(1, nPopulation);
		out.println("# r\tfixation prob\t(well-mixed)\tfixation time\t(well-mixed)\tabsorbtion time\t(well-mixed)");
		out.println(Formatter.formatFix(getFitness()[MUTANT], 2) + "\t"
				+ Formatter.formatFix(meanFix, 8) + " ± " + Formatter.formatFix(Math.sqrt(varFix / (count - 1.0)), 8)
				+ "\t"
				+ Formatter.formatFix(rhoA1, 8) + "\t"
				+ Formatter.formatFix(fixTotUpdate[0][0], 8) + " ± "
				+ Formatter.formatFix(Math.sqrt(fixTotUpdate[0][1] / (fixTotUpdate[0][2] - 1)), 8) + "\t"
				+ Formatter.formatFix(tA1(nPopulation), 8) + "\t"
				+ Formatter.formatFix(absTotUpdate[0], 8) + " ± "
				+ Formatter.formatFix(Math.sqrt(absTotUpdate[1] / (absTotUpdate[2] - 1)), 8) + "\t"
				+ Formatter.formatFix(t1(rhoA1, nPopulation), 8));
		engine.writeFooter();
		engine.exportState();
	}

	/**
	 * Helper method to convert milliseconds to a more readable string
	 * representation in the format 'HH:mm:ss.ss'.
	 * 
	 * @param msec the time in milliseconds
	 * @return the formatted string
	 */
	private String msecToString(long msec) {
		long sec = msec / 1000;
		long min = sec / 60;
		sec %= 60;
		long hour = min / 60;
		min %= 60;
		DecimalFormat twodigits = new DecimalFormat("00");
		return hour + ":" + twodigits.format(min) + ":" + twodigits.format(sec);
	}

	/**
	 * Command line option to show the simulation progress.
	 */
	final CLOption cloProgress = new CLOption("progress", Category.Simulation,
			"--progress      print progress reports",
			new CLODelegate() {
				@Override
				public boolean parse(String arg) {
					progress = cloProgress.isSet();
					return true;
				}
			});

	@Override
	public void collectCLO(CLOParser parser) {
		// prepare command line options
		parser.addCLO(cloProgress);
		super.collectCLO(parser);
	}

	/**
	 * Main method to run the simulation.
	 * 
	 * @param args the array of command line arguments
	 */
	public static void main(String[] args) {
		EvoLudoJRE engine = new EvoLudoJRE(false);
		engine.custom(new simMoran(engine), args);
	}
}
