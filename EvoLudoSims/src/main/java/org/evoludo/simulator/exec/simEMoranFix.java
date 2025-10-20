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
import org.evoludo.simulator.EvoLudoJRE;
import org.evoludo.simulator.models.FixationData;
import org.evoludo.simulator.models.Mode;
import org.evoludo.simulator.modules.EcoMoran;
import org.evoludo.util.CLOParser;
import org.evoludo.util.CLOption;
import org.evoludo.util.CLOption.CLODelegate;
import org.evoludo.util.CLOption.Category;

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
public class simEMoranFix extends EcoMoran {

	/**
	 * The number of samples for statistics.
	 */
	long nSamples;

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
	public simEMoranFix(EvoLudoJRE engine) {
		super(engine);
		out = engine.getOutput();
		jrengine = engine;
	}

	@Override
	public void run() {

		model.requestMode(Mode.STATISTICS_SAMPLE);
		long nextReport = -1;
		long msecStart = System.currentTimeMillis();

		if (progress)
			nextReport = 10;

		engine.writeHeader();
		out = jrengine.getOutput();

		long nSamples = (long) engine.getModel().getNSamples();
		double[][] fixProb = new double[nPopulation][2];

		// evolve population
		for (long r = 1; r <= nSamples; r++) {
			FixationData fixData = jrengine.generateSample();
			int typeFixed = (fixData.typeFixed == fixData.mutantTrait ? 0 : 1);
			fixProb[fixData.popSize - 1][typeFixed]++;

			if (progress && nextReport == r) {
				out.println("# runs: " + r + ", " + msecToString(System.currentTimeMillis() - msecStart));
				nextReport *= 10;
			}
		}
		double meanFix = 0.0;
		double unweightedSamples = 0.0;
		out.println("Population Size, Number Mutant Fixed, Number Resident Fixed");
		double weightedSamples = 0.0;
		double nodeSamples;
		for (int n = 1; n < nPopulation + 1; n++) {

			double[] node = fixProb[n - 1];
			out.println(n + ", " + node[0] + ", " + node[1]);
			nodeSamples = node[0] + node[1];
			if (nodeSamples == 0) {
				continue;
			}
			weightedSamples += nodeSamples * n;
			unweightedSamples += nodeSamples;
			meanFix += node[0] * n;
		}
		meanFix /= weightedSamples;
		out.println("Overall weighted fixation probability: " + meanFix);
		out.println("Number of samples: " + unweightedSamples);
		out.flush();
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
	// final CLOption cloSamples = new CLOption("samples", "100000",
	// EvoLudo.catSimulation, // 10^5
	// "--samples <s> number of samples",
	// new CLODelegate() {
	// @Override
	// public boolean parse(String arg) {
	// setNSamples(CLOParser.parseLong(arg));
	// return true;
	// }

	// @Override
	// public void report(PrintStream output) {
	// // output.println("# samples: "+activeModel.getNStatisticsSamples());
	// output.println("# samples: " + engine.getModel().getNStatisticsSamples());
	// }
	// });

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
		// parser.addCLO(cloSamples);
		parser.addCLO(cloProgress);

		super.collectCLO(parser);
	}

	public static void main(String[] args) {
		EvoLudoJRE engine = new EvoLudoJRE(false);
		engine.custom(new simEMoranFix(engine), args);
	}
}
