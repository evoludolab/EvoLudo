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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.evoludo.simulator.EvoLudo;
import org.evoludo.simulator.EvoLudoJRE;
import org.evoludo.simulator.Geometry;
import org.evoludo.simulator.models.ChangeListener;
import org.evoludo.simulator.models.IBSDPopulation;
import org.evoludo.simulator.models.IBSPopulation;
import org.evoludo.simulator.modules.EcoMoran;
import org.evoludo.util.CLOParser;
import org.evoludo.util.CLOption;
import org.evoludo.util.CLOption.CLODelegate;
import org.evoludo.util.Formatter;
import org.evoludo.util.Plist;

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
public class simEMoran extends EcoMoran implements ChangeListener {

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
	 * Create a new simulation to investigate fixation probabilities and times in
	 * the Moran process.
	 * 
	 * @param engine the pacemaker for running the model
	 */
	public simEMoran(EvoLudo engine) {
		super(engine);
	}

	private List<String> statuses;
	private List<String> geometries;
	private List<String> allTimes;

	private String successString = "1";

	@Override
	public void run() {

		// Simulate single pop

		out = ((EvoLudoJRE) engine).getOutput();

		// assumes IBS simulations
		IBSDPopulation pop = (IBSDPopulation) getIBSPopulation();
		nSamples = (int) engine.getModel().getTimeStop();
		allTimes = new ArrayList<>();

		// // print header
		// engine.dumpParameters();

		// evolve population
		statuses = new ArrayList<>();
		geometries = new ArrayList<>();

		for (long r = 1; r <= nSamples; r++) {
			engine.modelNext();
			// statuses.add(engine.getModel().getStatus());
			// geometries.add(Double.toString(engine.getModule().getIBSPopulation().getFitnessAt(1)));

			// geometries.add(values.get(0));
		}
		// engine.dumpEnd();

		for (String status : statuses) {
			out.println(status);
		}
		for (String geometry : geometries) {
			out.println(geometry);
		}
		for (String time : allTimes) {
			out.println(time);
		}
		out.flush();
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

	// /**
	// * Command line option to show the simulation progress.
	// */
	// final CLOption cloProgress = new CLOption("progress", EvoLudo.catSimulation,
	// "--progress print progress reports",
	// new CLODelegate() {
	// @Override
	// public boolean parse(String arg) {
	// progress = cloProgress.isSet();
	// return true;
	// }
	// });

	@Override
	public void collectCLO(CLOParser parser) {
		// prepare command line options
		// parser.addCLO(cloSamples);
		// parser.addCLO(cloProgress);

		super.collectCLO(parser);
		// parser.removeCLO("timeend");
	}

	public static void main(String[] args) {
		EvoLudoJRE engine = new EvoLudoJRE(false);
		engine.custom(new simEMoran(engine), args);
	}

	/**
	 * Time of previous sample.
	 */
	double prevsample;

	@Override
	public synchronized void modelChanged(PendingAction pending) {
		double generation = model.getTime();
		if (model.isRelaxing() || prevsample >= generation) {
			return;
		}
		prevsample = generation;

		String scores = engine.getModule().getIBSPopulation().getScores(2);
		List<String> values = Arrays.stream(scores.split(","))
				.map(String::trim)
				.collect(Collectors.toList());
		int[][] outNodes = engine.getModule().getGeometry().out;
		double nvv = 0.0;
		double nav = 0.0;
		double naa = 0.0;
		double na = 0.0;
		for (int n = 0; n < values.size(); n++) {
			String node = values.get(n);
			if (node.equals(successString)) {
				na++;
			}
			for (int adj : outNodes[n]) {
				String adjNode = values.get(adj);
				if (node.equals(successString)) {
					if (adjNode.equals(successString)) {
						naa++;
					} else {
						nav++;
					}
				} else {
					if (adjNode.equals(successString)) {
						nav++;
					} else {
						nvv++;
					}
				}
			}
		}

		geometries.add("Nav " + String.valueOf(nav));
		statuses.add("Na " + String.valueOf(na));
		allTimes.add("Time: " + String.valueOf(generation));
	}
}
