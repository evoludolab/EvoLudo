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
import java.text.DecimalFormat;

import org.evoludo.simulator.EvoLudo;
import org.evoludo.simulator.EvoLudoJRE;
import org.evoludo.simulator.models.IBSDPopulation;
import org.evoludo.simulator.modules.Moran;
import org.evoludo.util.CLOParser;
import org.evoludo.util.CLOption;
import org.evoludo.util.CLOption.CLODelegate;
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
	 * The number of samples for statistics.
	 */
	long nSamples = 100000;

	/**
	 * The flag to indicate whether to show progress.
	 */
	boolean progress = false;

	/**
	 * The output stream. Defaults to {@code System.out}.
	 */
	PrintStream out;

	/**
	 * Create a new simulation to investigate fixation probabilities and times in
	 * the Moran process.
	 * 
	 * @param engine the pacemaker for running the model
	 */
	public simMoran(EvoLudo engine) {
		super(engine);
	}

	@Override
	public void run() {
		out = ((EvoLudoJRE) engine).getOutput();
		// assumes IBS simulations
		IBSDPopulation pop = (IBSDPopulation) getIBSPopulation();
		long nFix = 0;
		long nextReport = -1;
		long msecStart = System.currentTimeMillis();
		double meanAbsTime = 0;
		double meanFixTime = 0;
		double sumSquaresFix = 0;
		double sumSquaresAbs = 0;

		// modelReset();
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
		engine.dumpParameters();

		// evolve population
		for (long r = 1; r <= nSamples; r++) {
			while (engine.modelNext())
				;
			double fixTime = engine.getModel().getTime();
			if (pop.strategiesTypeCount[RESIDENT] == 0) {
				// mutants fixated
				nFix++;
				double difMean = fixTime - meanFixTime;
				meanFixTime += difMean / nFix;
				double difMeanNew = fixTime - meanFixTime;
				sumSquaresFix += difMeanNew * difMean;
			}
			double difMean = fixTime - meanAbsTime;
			meanAbsTime += difMean / r;
			double difMeanNew = fixTime - meanAbsTime;
			sumSquaresAbs += difMeanNew * difMean;
			if (progress && nextReport == r) {
				out.println("# " + Formatter.formatFix((double) nFix / (double) r, 8) + "\t(runs: " + r + ", "
						+ msecToString(System.currentTimeMillis() - msecStart) + ")");
				nextReport *= 10;
			}
			engine.modelReset();
		}
		double rhoA1 = rhoA(1, nPopulation);
		out.println("# r\tfixation prob\t(analytical)\tfixation time\t(analytical)\tabsorbtion time\t(analytical)");
		out.println(Formatter.formatFix(getFitness()[MUTANT], 2) + "\t" +
				Formatter.formatFix((double) nFix / (double) nSamples, 8) + "\t" + Formatter.formatFix(rhoA1, 8) + "\t"
				+
				Formatter.formatFix(meanFixTime * nPopulation, 8) + " ± " +
				Formatter.formatFix(Math.sqrt(sumSquaresFix / (nFix - 1)) * nPopulation, 8) + "\t"
				+ Formatter.formatFix(tA1(nPopulation), 8) + "\t" +
				Formatter.formatFix(meanAbsTime * nPopulation, 8) + " ± " +
				Formatter.formatFix(Math.sqrt(sumSquaresAbs / (nSamples - 1)) * nPopulation, 8) + "\t"
				+ Formatter.formatFix(t1(rhoA1, nPopulation), 8));
		engine.dumpEnd();
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
	 * Set the number of samples for statistics.
	 * 
	 * @param nSamples the number of samples
	 */
	public void setNSamples(long nSamples) {
		if (nSamples <= 0L)
			return;
		this.nSamples = nSamples;
	}

	/**
	 * Get the number of samples for statistics.
	 * 
	 * @return the number of samples
	 */
	public double getNSamples() {
		return nSamples;
	}

	/**
	 * Command line option for setting the number of samples for statistics.
	 */
	final CLOption cloSamples = new CLOption("samples", "100000", EvoLudo.catSimulation, // 10^5
			"--samples <s>   number of samples",
			new CLODelegate() {
				@Override
				public boolean parse(String arg) {
					setNSamples(CLOParser.parseLong(arg));
					return true;
				}

				@Override
				public void report(PrintStream output) {
					// output.println("# samples: "+activeModel.getNStatisticsSamples());
					output.println("# samples:              " + engine.getModel().getNStatisticsSamples());
				}
			});

	/**
	 * Command line option to show the simulation progress.
	 */
	final CLOption cloProgress = new CLOption("progress", EvoLudo.catSimulation,
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
		parser.addCLO(cloSamples);
		parser.addCLO(cloProgress);

		super.collectCLO(parser);
		parser.removeCLO("timeend");
	}
}
