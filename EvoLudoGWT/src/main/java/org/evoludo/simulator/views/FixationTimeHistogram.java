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

import java.util.Arrays;

import org.evoludo.graphics.GraphStyle;
import org.evoludo.graphics.HistoGraph;
import org.evoludo.math.Distributions;
import org.evoludo.math.Functions;
import org.evoludo.simulator.EvoLudoGWT;
import org.evoludo.simulator.models.FixationData;
import org.evoludo.simulator.modules.Module;
import org.evoludo.util.Formatter;

/**
 * Histograms of the fixation times. Shows either the average fixation time per
 * node of the initial mutant, with the number of samples stored in the rows
 * {@code nTraits + 1, ..., 2 nTraits + 1} of the data, or, for large
 * populations, the distribution of fixation times. The extra graph
 * {@code nTraits} shows the absorption times irrespective of the trait that
 * fixated.
 *
 * @author Christoph Hauert
 */
public class FixationTimeHistogram extends FixationHistogram {

	/**
	 * Construct a new view to display the fixation times.
	 *
	 * @param engine the pacemaker for running the model
	 */
	public FixationTimeHistogram(EvoLudoGWT engine) {
		super(engine, "Avg. fix. time:");
	}

	@Override
	public String getName() {
		return "Statistics - Fixation time";
	}

	@Override
	protected int[] getGraphRows(Module<?> module) {
		// add absorption
		int[] rows = super.getGraphRows(module);
		rows = Arrays.copyOf(rows, rows.length + 1);
		rows[rows.length - 1] = module.getNTraits();
		return rows;
	}

	/**
	 * Check whether to show the fixation time distribution or the fixation times
	 * for each node.
	 *
	 * @param module the module of the graph
	 * @return {@code true} to show the fixation time distribution
	 */
	private boolean doFixtimeDistr(Module<?> module) {
		if (model.isSDE())
			return true;
		int maxBins = graphs.get(0).getMaxBins();
		return (maxBins > 0 && module.getNPopulation() > maxBins);
	}

	@Override
	protected int[] resetGraph(HistoGraph graph, int idx) {
		model.resetStatisticsSample();
		Module<?> module = graph.getModule();
		int nTraits = module.getNTraits();
		if (doFixtimeDistr(module)) {
			graph.setNormalized(false);
			graph.setNormalized(-1);
			// widen the bins by one pixel; multiply before dividing to avoid
			// integer division truncating the ratio to zero
			return new int[] { nTraits + 1,
					graph.getMaxBins() * (HistoGraph.MIN_BIN_WIDTH + 1) / (HistoGraph.MIN_BIN_WIDTH + 2) };
		}
		graph.setNormalized(true);
		graph.setNormalized(graph.getRow() + nTraits + 1);
		// doFixtimeDistr ensures nPop <= HistoGraph.DEFAULT_BINS
		return new int[] { 2 * (nTraits + 1), module.getNPopulation() };
	}

	@Override
	protected boolean applyStyle(HistoGraph graph) {
		super.applyStyle(graph);
		GraphStyle style = graph.getStyle();
		int nPop = graph.getModule().getNPopulation();
		boolean distr = doFixtimeDistr(graph.getModule());
		style.xMax = distr ? Functions.roundUp(nPop * 0.25) : nPop - 1.0;
		style.xLabel = distr ? "fixation time" : "node";
		style.yLabel = distr ? "probability" : "time";
		style.percentY = distr;
		return false;
	}

	@Override
	protected double[] getCustomLevels(HistoGraph graph) {
		Module<?> module = graph.getModule();
		if (doFixtimeDistr(module) || !(module instanceof HasHistogram.StatisticsTime))
			return new double[0];
		return ((HasHistogram.StatisticsTime) module).getFixationTimeLevels(graph.getRow());
	}

	@Override
	protected boolean addSample(HistoGraph graph, FixationData fixData) {
		if (fixData.timeRead)
			return false;
		HistoGraph absorption = graphs.get(graphs.size() - 1);
		boolean distr = doFixtimeDistr(absorption.getModule());
		for (HistoGraph g : new HistoGraph[] { graph, absorption }) {
			if (g == null)
				continue;
			if (distr)
				g.addData(fixData.updatesFixed);
			else
				g.addData(fixData.mutantNode, fixData.updatesFixed);
		}
		fixData.timeRead = true;
		return true;
	}

	@Override
	public String getTooltipAt(HistoGraph graph, int bar) {
		if (doFixtimeDistr(graph.getModule()))
			return getRangeTooltip(graph, bar);
		return super.getTooltipAt(graph, bar);
	}

	@Override
	protected String getStatus(HistoGraph graph, int nSamp) {
		if (graph.getSamples() <= 0.0)
			return "-";
		double[][] data = graph.getData();
		int row = graph.getRow();
		if (doFixtimeDistr(graph.getModule()))
			return formatDistributionMean(data[row], graph.getStyle());
		return formatWeightedMean(data[row], data[row + graph.getModule().getNTraits() + 1]);
	}

	/**
	 * Format mean ± sdev for a distribution stored in data (assumed normalized
	 * counts).
	 *
	 * @param dist  the distribution data
	 * @param style the graph style for scaling
	 * @return the formatted string
	 */
	private static String formatDistributionMean(double[] dist, GraphStyle style) {
		double mean = Distributions.distrMean(dist);
		double sdev = Distributions.distrStdev(dist, mean);
		return Formatter.formatFix(style.xMin + mean * (style.xMax - style.xMin), 2)
				+ " ± " + Formatter.formatFix(sdev * (style.xMax - style.xMin), 2);
	}

	/**
	 * Compute weighted mean ± sdev from node-wise times (dat) and sample counts
	 * (sam).
	 *
	 * @param dat the data array
	 * @param sam the sample counts
	 * @return the formatted string
	 */
	private static String formatWeightedMean(double[] dat, double[] sam) {
		double sx = 0.0;
		double sx2 = 0.0;
		double sw = 0.0;
		for (int n = 0; n < dat.length; n++) {
			double w = sam[n];
			if (w <= 0.0)
				continue;
			double x = dat[n];
			sx += x;
			sx2 += x * x / w;
			sw += w;
		}
		if (sw <= 0.0)
			return "-";
		double mean = sx / sw;
		double sdev = Math.sqrt(sx2 / sw - mean * mean);
		return Formatter.formatFix(mean, 4) + " ± " + Formatter.formatFix(sdev, 4);
	}
}
