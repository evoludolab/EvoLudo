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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.evoludo.simulator.EvoLudo;
import org.evoludo.simulator.EvoLudoJRE;
import org.evoludo.simulator.models.ChangeListener;
import org.evoludo.simulator.models.IBSDPopulation;
import org.evoludo.simulator.modules.EcoMoran;
import org.evoludo.util.CLOParser;

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
	private List<String> nVAdist;
	private List<String> nAVdist;
	private List<String> adjToAAdist;
	private List<String> vvAdjToAVdist;
	private List<String> vvAdjToVVdist;
	private List<String> allNumNStepsAway;
	private List<String> allTotalNStepsAway;

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
		nVAdist = new ArrayList<>();
		nAVdist = new ArrayList<>();
		adjToAAdist = new ArrayList<>();
		vvAdjToAVdist = new ArrayList<>();
		vvAdjToVVdist = new ArrayList<>();
		allNumNStepsAway = new ArrayList<>();
		allTotalNStepsAway = new ArrayList<>();

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
		for (String nva : nVAdist) {
			out.println(nva);
		}
		for (String nav : nAVdist) {
			out.println(nav);
		}

		for (String adjToAA : adjToAAdist) {
			out.println(adjToAA);
		}

		for (String vvAdjToAV : vvAdjToAVdist) {
			out.println(vvAdjToAV);
		}

		for (String vvAdjToVV : vvAdjToVVdist) {
			out.println(vvAdjToVV);
		}

		for (String numNStepsAway : allNumNStepsAway) {
			out.println(numNStepsAway);
		}
		for (String totalNStepsAway : allTotalNStepsAway) {
			out.println(totalNStepsAway);
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
		double[] nvaDistArr = new double[outNodes[0].length + 1];
		double[] navDistArr = new double[outNodes[0].length + 1];
		double[] adjToAA = new double[outNodes[0].length * 2 - 1];
		double[] vvAdjToAV = new double[outNodes[0].length];
		double[] vvAdjToVV = new double[outNodes[0].length];
		double[] numNStepsAway = new double[Math.min(50, values.size()) + 1];
		double[] totalNStepsAway = new double[Math.min(50, values.size()) + 1];
		for (int n = 0; n < values.size(); n++) {
			String node = values.get(n);

			if (node.equals(successString)) {
				int currentStep = 0;
				ArrayList<Integer> currentNodes = new ArrayList<>(Arrays.asList(n));
				ArrayList<Integer> nextNodes = new ArrayList<>();
				boolean[] visited = new boolean[values.size()];
				visited[n] = true;
				numNStepsAway[currentStep] += currentNodes.size();
				totalNStepsAway[currentStep] += currentNodes.size();
				int count = 0;
				int countAll = 0;
				while (!currentNodes.isEmpty()) {
					count = 0;
					countAll = 0;
					for (int cn : currentNodes) {
						for (int adj : outNodes[cn]) {
							if (!visited[adj]) {
								visited[adj] = true;
								countAll++;
								nextNodes.add(adj);
								if (values.get(adj).equals(successString)) {
									count++;
								}
							}
						}
					}
					currentNodes = nextNodes;
					nextNodes = new ArrayList<>();
					currentStep++;
					if (currentStep >= numNStepsAway.length) {
						break;
					}
					numNStepsAway[currentStep] += count;

					totalNStepsAway[currentStep] += countAll;
				}

			}

			int temp_nav = 0;
			for (int adj : outNodes[n]) {
				String adjNode = values.get(adj);
				if (node.equals(successString)) {
					if (adjNode.equals(successString)) {
						naa++;
						int adjToAddCounter = 0;
						for (int tempAdj : outNodes[n]) {
							String tempAdjNode = values.get(tempAdj);
							if (tempAdj != adj && tempAdjNode.equals(successString)) {
								adjToAddCounter++;
							}
						}
						for (int tempAdj : outNodes[adj]) {
							String tempAdjNode = values.get(tempAdj);
							if (!tempAdjNode.equals(node) && tempAdjNode.equals(successString)) {
								adjToAddCounter++;
							}
						}
						adjToAA[adjToAddCounter]++;
					} else {
						nav++;
						temp_nav++;

						int adjToAddCounter = 0;
						for (int tempAdj : outNodes[adj]) {
							String tempAdjNode = values.get(tempAdj);
							if (tempAdj != n && !tempAdjNode.equals(successString)) {
								adjToAddCounter++;
							}
						}
						vvAdjToAV[adjToAddCounter]++;
					}
				} else {
					if (adjNode.equals(successString)) {
						nav++;
						temp_nav++;
					} else {
						nvv++;
						int adjToAddCounter = 0;
						for (int tempAdj : outNodes[adj]) {
							String tempAdjNode = values.get(tempAdj);
							if (tempAdj != n && !tempAdjNode.equals(successString)) {
								adjToAddCounter++;
							}
						}
						vvAdjToVV[adjToAddCounter]++;
					}
				}
			}
			if (node.equals(successString)) {
				na++;
				nvaDistArr[temp_nav]++;
			} else {
				navDistArr[temp_nav]++;
			}
		}

		for (int i = 0; i < numNStepsAway.length; i++) {
			numNStepsAway[i] /= na;
		}
		for (int i = 0; i < totalNStepsAway.length; i++) {
			totalNStepsAway[i] /= na;
		}

		geometries.add("Nav " + String.valueOf(nav));
		statuses.add("Na " + String.valueOf(na));
		allTimes.add("Time: " + String.valueOf(generation));
		nVAdist.add("Nva Distribution " + Arrays.toString(nvaDistArr));
		nAVdist.add("Nav Distribution " + Arrays.toString(navDistArr));
		adjToAAdist.add("AdjToAA Distribution " + Arrays.toString(adjToAA));
		vvAdjToAVdist.add("VVAdjToAV Distribution " + Arrays.toString(vvAdjToAV));
		vvAdjToVVdist.add("VVAdjToVV Distribution " + Arrays.toString(vvAdjToVV));
		allNumNStepsAway.add("NumNStepsAway " + Arrays.toString(numNStepsAway));
		allTotalNStepsAway.add("TotalNStepsAway " + Arrays.toString(totalNStepsAway));
	}
}
