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
	private List<String> degrees;
	private List<String> allDegrees;

	@Override
	public void run() {
		engine.writeHeader();
		out = ((EvoLudoJRE) engine).getOutput();

		nSamples = (int) engine.getModel().getTimeStop();
		allTimes = new ArrayList<>();

		statuses = new ArrayList<>();
		geometries = new ArrayList<>();
		degrees = new ArrayList<>();
		allDegrees = new ArrayList<>();

		for (long r = 1; r <= nSamples; r++) {
			engine.modelNext();
		}

		for (String status : statuses) {
			out.println(status);
		}
		for (String geometry : geometries) {
			out.println(geometry);
		}
		for (String time : allTimes) {
			out.println(time);
		}
		for (String degree : degrees) {
			out.println(degree);
		}
		for (String allDegree : allDegrees) {
			out.println(allDegree);
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

	@Override
	public void collectCLO(CLOParser parser) {
		super.collectCLO(parser);
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

		IBSDPopulation population = (IBSDPopulation) getIBSPopulation();
		int[] traits = new int[nPopulation];
		double na = 0.0;
		for (int n = 0; n < traits.length; n++) {
			traits[n] = population.getTraitAt(n);
			if (traits[n] == RESIDENT) {
				na++;
			}
		}

		statuses.add("Na " + String.valueOf(na));
		allTimes.add("Time: " + String.valueOf(generation));

		boolean graph = true;
		if (graph) {
			int[][] outNodes = engine.getModule().getGeometry().out;
			int[] kout = engine.getModule().getGeometry().kout;
			double nav = 0.0;

			int maxOutDegree = 0;
			for (int[] out : outNodes) {
				if (out.length > maxOutDegree) {
					maxOutDegree = out.length;
				}
			}
			double[] degreeDistArr = new double[maxOutDegree + 1];

			if (allDegrees.isEmpty()) {
				double[] degreeDistArrTemp = new double[maxOutDegree + 1];
				for (int n = 0; n < kout.length; n++) {
					int degree = kout[n];
					degreeDistArrTemp[degree]++;
				}
				allDegrees.add("All Degrees " + Arrays.toString(degreeDistArrTemp));
			}

			for (int n = 0; n < traits.length; n++) {
				int node = traits[n];

				if (node == RESIDENT) {
					degreeDistArr[kout[n]]++;
				}

				for (int i = 0; i < kout[n]; i++) {
					int adj = outNodes[n][i];
					int adjNode = traits[adj];
					if ((node == RESIDENT) != (adjNode == RESIDENT))
						nav++;
				}
			}

			geometries.add("Nav " + String.valueOf(nav));
			degrees.add("Inhabited Degree Distribution " + Arrays.toString(degreeDistArr));
		}
	}
}
