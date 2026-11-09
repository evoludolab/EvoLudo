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

package org.evoludo.simulator.views;

import org.evoludo.graphics.GraphStyle;
import org.evoludo.graphics.HistoGraph;
import org.evoludo.simulator.EvoLudoGWT;
import org.evoludo.simulator.models.FixationData;
import org.evoludo.simulator.modules.Module;
import org.evoludo.util.Formatter;

/**
 * Histograms of the fixation probabilities as a function of the node of the
 * initial mutant. Fixations are accumulated per (possibly binned) node and
 * normalized by the number of samples per node, which are stored in the extra
 * row {@code nTraits} of the data.
 *
 * @author Christoph Hauert
 */
public class FixationProbabilityHistogram extends FixationHistogram {

	/**
	 * The scaling factor to map nodes onto bins.
	 */
	private double scale2bins = 1.0;

	/**
	 * Construct a new view to display the fixation probabilities.
	 *
	 * @param engine the pacemaker for running the model
	 */
	public FixationProbabilityHistogram(EvoLudoGWT engine) {
		super(engine, "Avg. fix. prob:");
	}

	@Override
	public String getName() {
		return "Statistics - Fixation probability";
	}

	@Override
	protected int[] resetGraph(HistoGraph graph, int idx) {
		model.resetStatisticsSample();
		int nTraits = graph.getModule().getNTraits();
		graph.setNormalized(nTraits);
		int n = (model.isSDE() ? 1 : graph.getModule().getNPopulation());
		int maxBins = graph.getMaxBins();
		if (maxBins <= 0)
			maxBins = Math.min(n, HistoGraph.DEFAULT_BINS);
		int binSize = 1;
		int bins = n;
		while (bins > maxBins)
			bins = n / ++binSize;
		// spread nodes evenly across bins
		scale2bins = (double) bins / n;
		return new int[] { nTraits + 1, bins };
	}

	@Override
	protected boolean applyStyle(HistoGraph graph) {
		super.applyStyle(graph);
		GraphStyle style = graph.getStyle();
		style.yLabel = "probability";
		style.xLabel = "node";
		style.xMax = (model.isSDE() ? 1 : graph.getModule().getNPopulation() - 1);
		return false;
	}

	@Override
	protected double[] getCustomLevels(HistoGraph graph) {
		Module<?> module = graph.getModule();
		if (!(module instanceof HasHistogram.StatisticsProbability))
			return new double[0];
		return ((HasHistogram.StatisticsProbability) module).getFixationProbabilityLevels(graph.getRow());
	}

	@Override
	protected boolean addSample(HistoGraph graph, FixationData fixData) {
		if (fixData.probRead || graph == null)
			return false;
		graph.addData((int) (fixData.mutantNode * scale2bins + BIN_EPSILON));
		fixData.probRead = true;
		return true;
	}

	@Override
	protected String getStatus(HistoGraph graph, int nSamp) {
		return nSamp <= 0 ? "-" : Formatter.formatFix(graph.getSamples() / nSamp, 4);
	}
}
