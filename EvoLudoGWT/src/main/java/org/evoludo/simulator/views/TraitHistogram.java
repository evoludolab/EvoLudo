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
import org.evoludo.simulator.EvoLudoGWT;
import org.evoludo.simulator.models.CModel;
import org.evoludo.simulator.models.Data;
import org.evoludo.simulator.modules.Continuous;
import org.evoludo.simulator.modules.Module;
import org.evoludo.ui.ContextMenu;
import org.evoludo.ui.ContextMenuCheckBoxItem;
import org.evoludo.ui.ContextMenuItem;

/**
 * Histograms of the continuous traits. The number of bins can be selected
 * through the context menu.
 *
 * @author Christoph Hauert
 */
public class TraitHistogram extends Histogram {

	/**
	 * Available bin counts.
	 */
	private static final int[] HISTOGRAM_BIN_OPTIONS = new int[] { 25, 50, 100, 200, 500 };

	/**
	 * Active number of bins.
	 */
	protected int histogramBins = HistoGraph.DEFAULT_BINS;

	/**
	 * Construct a new view to display the histograms of the traits.
	 *
	 * @param engine the pacemaker for running the model
	 */
	public TraitHistogram(EvoLudoGWT engine) {
		this(engine, Data.TRAIT);
	}

	/**
	 * Construct a new view to display histograms of {@code type}, for subclasses.
	 *
	 * @param engine the pacemaker for running the model
	 * @param type   the type of data to display
	 */
	protected TraitHistogram(EvoLudoGWT engine, Data type) {
		super(engine, type);
	}

	@Override
	public String getName() {
		return "Traits - Histogram";
	}

	@Override
	protected boolean isAutoscaleY() {
		return false;
	}

	@Override
	protected int[] getGraphRows(Module<?> module) {
		return rows(module.getNTraits(), -1);
	}

	@Override
	protected int[] resetGraph(HistoGraph graph, int idx) {
		graph.clearMarkers();
		GraphStyle style = graph.getStyle();
		// markers refer to the traits of all species
		for (double[] mark : model.getMarkers())
			graph.addMarker(mark[idx + 1], style.graphColor, null, mark[0] > 0.0 ? style.dashedLine : style.dottedLine);
		return new int[] { graph.getModule().getNTraits(), histogramBins };
	}

	@Override
	protected boolean applyStyle(HistoGraph graph) {
		GraphStyle style = graph.getStyle();
		style.showLabel = isMultispecies;
		style.showXLabel = true;
		style.showXTickLabels = true;
		Continuous cmod = (Continuous) graph.getModule();
		int row = graph.getRow();
		style.xLabel = cmod.getTraitName(row);
		applyTraitLabel(graph);
		return setXRange(style, cmod.getTraitMin()[row], cmod.getTraitMax()[row]);
	}

	/**
	 * Set the range of the {@code x}-axis of {@code style}.
	 *
	 * @param style the style of the graph
	 * @param min   the minimum of the range
	 * @param max   the maximum of the range
	 * @return {@code true} if the range changed, which requires a hard reset
	 */
	protected static boolean setXRange(GraphStyle style, double min, double max) {
		if (Math.abs(min - style.xMin) <= 1e-6 && Math.abs(max - style.xMax) <= 1e-6)
			return false;
		style.xMin = min;
		style.xMax = max;
		return true;
	}

	@Override
	protected void updateGraphs() {
		// graphs of the same species share their data
		double[][] data = null;
		for (HistoGraph graph : graphs) {
			if (data == graph.getData())
				continue;
			data = graph.getData();
			getHistogramData(graph.getModule().getId(), data);
		}
	}

	/**
	 * Retrieve the histogram data of the species with {@code id} from the model.
	 *
	 * @param id   the id of the species
	 * @param data the array for storing the histogram data
	 */
	protected void getHistogramData(int id, double[][] data) {
		((CModel) model).getTraitHistogramData(id, data);
	}

	@Override
	public void populateContextMenu(ContextMenu menu) {
		int[] binCounts = getBinCounts();
		if (!graphs.isEmpty()) {
			int selected = Math.min(Math.max(histogramBins, binCounts[0]), binCounts[binCounts.length - 1]);
			ContextMenu binsMenu = new ContextMenu(menu);
			for (int bins : binCounts) {
				ContextMenuCheckBoxItem item = new ContextMenuCheckBoxItem(Integer.toString(bins),
						() -> applyHistogramBins(bins));
				item.setChecked(bins == selected);
				binsMenu.add(item);
			}
			menu.addSeparator();
			ContextMenuItem binsTrigger = menu.add("Bins", binsMenu);
			binsTrigger.setEnabled(!isRunning());
		}
		super.populateContextMenu(menu);
	}

	/**
	 * Return the feasible bin counts, i.e. the prefix of
	 * {@link #HISTOGRAM_BIN_OPTIONS} that fits the current graph widths. The
	 * smallest option is always feasible.
	 *
	 * @return feasible bin counts
	 */
	private int[] getBinCounts() {
		int maxFeasible = HISTOGRAM_BIN_OPTIONS[HISTOGRAM_BIN_OPTIONS.length - 1];
		for (HistoGraph graph : graphs) {
			int max = graph.getMaxBins();
			if (max > 0)
				maxFeasible = Math.min(maxFeasible, max);
		}
		int count = 1;
		while (count < HISTOGRAM_BIN_OPTIONS.length && HISTOGRAM_BIN_OPTIONS[count] <= maxFeasible)
			count++;
		return Arrays.copyOf(HISTOGRAM_BIN_OPTIONS, count);
	}

	/**
	 * Apply a new number of bins.
	 *
	 * @param newBins requested number of bins
	 */
	private void applyHistogramBins(int newBins) {
		if (newBins == histogramBins)
			return;
		histogramBins = newBins;
		reset(true);
	}
}
