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

import java.awt.Color;

import org.evoludo.graphics.GraphStyle;
import org.evoludo.graphics.HistoGraph;
import org.evoludo.math.Distributions;
import org.evoludo.math.Functions;
import org.evoludo.simulator.ColorMapCSS;
import org.evoludo.simulator.EvoLudoGWT;
import org.evoludo.simulator.models.Data;
import org.evoludo.simulator.models.FixationData;
import org.evoludo.simulator.modules.Discrete;
import org.evoludo.simulator.modules.Module;
import org.evoludo.util.Formatter;

/**
 * Histograms of the fixation probabilities and fixation times, sampled over
 * many runs, as a function of the node of the initial mutant.
 * <ul>
 * <li>{@code STATISTICS_FIXATION_PROBABILITY} accumulates the fixations per
 * (possibly binned) node, normalized by the number of samples per node, which
 * are stored in the extra row {@code nTraits} of the data.</li>
 * <li>{@code STATISTICS_FIXATION_TIME} shows either the average fixation time
 * per node, with the number of samples stored in the rows
 * {@code nTraits + 1, ..., 2 nTraits + 1} of the data, or, for large
 * populations, the distribution of fixation times. The extra graph
 * {@code nTraits} shows the absorption times irrespective of the trait that
 * fixated.</li>
 * </ul>
 * <strong>Note:</strong> not ready for multiple species, the fixation data do
 * not report which species fixated.
 *
 * @author Christoph Hauert
 */
public class FixationHistogram extends Histogram {

	/**
	 * The scaling factor to map nodes onto bins.
	 */
	private double scale2bins = 1.0;

	/**
	 * The status of the view.
	 */
	private String status;

	/**
	 * Construct a new view to display the fixation probabilities or times.
	 *
	 * @param engine the pacemaker for running the model
	 * @param type   {@code STATISTICS_FIXATION_PROBABILITY} or
	 *               {@code STATISTICS_FIXATION_TIME}
	 */
	public FixationHistogram(EvoLudoGWT engine, Data type) {
		super(engine, type);
	}

	/**
	 * Check whether the view shows fixation times.
	 *
	 * @return {@code true} for fixation times, {@code false} for probabilities
	 */
	private boolean isTime() {
		return type == Data.STATISTICS_FIXATION_TIME;
	}

	@Override
	public boolean load() {
		if (!super.load())
			return false;
		engine.addSampleListener(this);
		return true;
	}

	@Override
	public void unload() {
		super.unload();
		engine.removeSampleListener(this);
	}

	@Override
	protected int[] getGraphRows(Module<?> module) {
		// no graph for vacant trait if monostop; fixation times include absorption
		int vacant = module.getVacantIdx();
		int skip = (vacant >= 0 && module instanceof Discrete && ((Discrete) module).getMonoStop()) ? vacant : -1;
		int nTraits = module.getNTraits();
		return rows(isTime() ? nTraits + 1 : nTraits, skip);
	}

	@Override
	protected int[] resetGraph(HistoGraph graph, int idx) {
		model.resetStatisticsSample();
		Module<?> module = graph.getModule();
		int nTraits = module.getNTraits();
		int nPop = module.getNPopulation();
		int maxBins = graph.getMaxBins();
		if (isTime()) {
			if (doFixtimeDistr(module)) {
				graph.setNormalized(false);
				graph.setNormalized(-1);
				// widen the bins by one pixel; multiply before dividing to avoid
				// integer division truncating the ratio to zero
				return new int[] { nTraits + 1,
						maxBins * (HistoGraph.MIN_BIN_WIDTH + 1) / (HistoGraph.MIN_BIN_WIDTH + 2) };
			}
			graph.setNormalized(true);
			graph.setNormalized(graph.getRow() + nTraits + 1);
			// doFixtimeDistr ensures nPop <= HistoGraph.DEFAULT_BINS
			return new int[] { 2 * (nTraits + 1), nPop };
		}
		graph.setNormalized(nTraits);
		int n = (model.isSDE() ? 1 : nPop);
		if (maxBins <= 0)
			maxBins = Math.min(n, HistoGraph.DEFAULT_BINS);
		int binSize = 1;
		int bins = n;
		while (bins > maxBins)
			bins = n / ++binSize;
		scale2bins = 1.0 / binSize;
		return new int[] { nTraits + 1, bins };
	}

	@Override
	protected boolean applyStyle(HistoGraph graph) {
		GraphStyle style = graph.getStyle();
		style.showLabel = true;
		style.xMin = 0.0;
		Module<?> module = graph.getModule();
		int nPop = module.getNPopulation();
		int row = graph.getRow();
		if (row == module.getNTraits()) {
			style.label = "Absorption";
			style.graphColor = ColorMapCSS.Color2Css(Color.BLACK);
		} else
			applyTraitLabel(graph);
		style.customYLevels = ((HasHistogram) module).getCustomLevels(type, row);
		if (!isTime()) {
			style.yLabel = "probability";
			style.xLabel = "node";
			style.xMax = (model.isSDE() ? 1 : nPop - 1);
		} else if (doFixtimeDistr(module)) {
			style.xMax = Functions.roundUp(nPop * 0.25);
			style.xLabel = "fixation time";
			style.yLabel = "probability";
			style.percentY = true;
			style.customYLevels = new double[0];
		} else {
			style.xMax = nPop - 1.0;
			style.xLabel = "node";
			style.yLabel = "time";
			style.percentY = false;
		}
		return false;
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
	public void modelSample(boolean success) {
		if (!success)
			return;
		FixationData fixData = model.getFixationData();
		int initNode = fixData.mutantNode;
		if (initNode < 0)
			return;
		HistoGraph graph = null;
		for (HistoGraph g : graphs) {
			if (g.getRow() == fixData.typeFixed) {
				graph = g;
				break;
			}
		}
		if (isTime()) {
			if (fixData.timeRead)
				return;
			HistoGraph absorption = graphs.get(graphs.size() - 1);
			boolean distr = doFixtimeDistr(absorption.getModule());
			for (HistoGraph g : new HistoGraph[] { graph, absorption }) {
				if (g == null)
					continue;
				if (distr)
					g.addData(fixData.updatesFixed);
				else
					g.addData(initNode, fixData.updatesFixed);
			}
			fixData.timeRead = true;
		} else {
			if (fixData.probRead || graph == null)
				return;
			graph.addData((int) (initNode * scale2bins + BIN_EPSILON));
			fixData.probRead = true;
		}
		if (isActive && doUpdateView(false)) {
			for (HistoGraph g : graphs)
				g.paint(false);
		}
	}

	@Override
	public String getStatus(boolean force) {
		// status calculation are somewhat costly - throttle the max number of updates
		if (!doUpdateStatus(force))
			return status;
		// multi-species not supported for statistics
		// clo changes may trigger reset before view has been loaded
		if (isMultispecies || !isLoaded)
			return null;
		int nSamp = model.getNStatisticsSamples();
		StringBuilder sb = new StringBuilder(isTime() ? "Avg. fix. time: " : "Avg. fix. prob: ");
		for (HistoGraph graph : graphs) {
			double[][] data = graph.getData();
			if (data == null) {
				engine.logWarning("Fixation statistics not available!");
				return "";
			}
			if (graph != graphs.get(0))
				sb.append(", ");
			sb.append(graph.getStyle().label).append(": ");
			int row = graph.getRow();
			if (!isTime())
				sb.append(nSamp <= 0 ? "-" : Formatter.formatFix(graph.getSamples() / nSamp, 4));
			else if (graph.getSamples() <= 0.0)
				sb.append("-");
			else if (doFixtimeDistr(graph.getModule()))
				sb.append(formatDistributionMean(data[row], graph.getStyle()));
			else
				sb.append(formatWeightedMean(data[row], data[row + graph.getModule().getNTraits() + 1]));
		}
		status = sb.toString();
		return status;
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
