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
import org.evoludo.simulator.models.Data;
import org.evoludo.simulator.models.Model;
import org.evoludo.simulator.modules.Module;
import org.evoludo.ui.ContextMenu;
import org.evoludo.ui.ContextMenuItem;

/**
 * Histograms of the stationary distribution, i.e. the visits of each trait
 * count (IBS) or frequency/density (DE) accumulated over time.
 *
 * @author Christoph Hauert
 */
public class StationaryHistogram extends Histogram {

	/**
	 * The scaling factor to map the mean traits onto bins.
	 */
	private double scale2bins = 1.0;

	/**
	 * Construct a new view to display the stationary distribution.
	 *
	 * @param engine the pacemaker for running the model
	 */
	public StationaryHistogram(EvoLudoGWT engine) {
		super(engine, Data.STATISTICS_STATIONARY);
	}

	@Override
	protected int[] getGraphRows(Module<?> module) {
		return rows(module.getNTraits(), -1);
	}

	@Override
	protected int[] resetGraph(HistoGraph graph, int idx) {
		graph.setNormalized(false);
		int maxBins = graph.getMaxBins();
		int nBins = Math.max(1,
				Math.min(graph.getModule().getNPopulation(), maxBins <= 0 ? HistoGraph.DEFAULT_BINS : maxBins));
		scale2bins = nBins;
		return new int[] { graph.getModule().getNTraits(), nBins };
	}

	@Override
	protected boolean applyStyle(HistoGraph graph) {
		GraphStyle style = graph.getStyle();
		style.yLabel = "visits";
		style.showLabel = true;
		style.xMin = 0.0;
		style.percentX = model.isDE() && !model.isDensity();
		if (!model.isDE()) {
			style.xLabel = "strategy count";
			style.xMax = graph.getModule().getNPopulation();
		} else {
			style.xLabel = (style.percentX ? "frequency" : "density");
			style.xMax = (style.percentX ? 1.0 : 0.0);
		}
		applyTraitLabel(graph);
		return false;
	}

	@Override
	protected void updateData() {
		if (!isActive)
			return;
		double newtime = model.getUpdates();
		if (Math.abs(timestamp - newtime) > Model.EPS) {
			timestamp = newtime;
			updateGraphs();
		}
	}

	@Override
	protected void updateGraphs() {
		double[] state = new double[model.getNMean()];
		model.getMeanTraits(state);
		int idx = 0;
		for (HistoGraph graph : graphs)
			graph.addData((int) (state[idx++] * scale2bins + BIN_EPSILON));
	}

	@Override
	public String getStatus(boolean force) {
		if (isLoaded && model.hasConverged()) {
			engine.logWarning("Non ergodic system - no stationary distribution!");
			return "";
		}
		return super.getStatus(force);
	}

	@Override
	public void populateContextMenu(ContextMenu menu) {
		menu.addSeparator();
		menu.add(new ContextMenuItem("Clear", () -> {
			for (HistoGraph graph : graphs)
				graph.clearData();
			timestamp = -1.0;
			update();
		}));
		super.populateContextMenu(menu);
	}
}
