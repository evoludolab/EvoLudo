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
import org.evoludo.simulator.ColorMapCSS;
import org.evoludo.simulator.EvoLudoGWT;
import org.evoludo.simulator.models.FixationData;
import org.evoludo.simulator.models.Mode;
import org.evoludo.simulator.modules.Discrete;
import org.evoludo.simulator.modules.Module;

/**
 * Base class for histograms of fixation statistics, sampled over many runs, as
 * a function of the node of the initial mutant. Shows one graph per trait,
 * except for vacant sites if the run stops when they fixate.
 * <p>
 * <strong>Note:</strong> not ready for multiple species, the fixation data do
 * not report which species fixated.
 *
 * @author Christoph Hauert
 *
 * @see FixationProbabilityHistogram
 * @see FixationTimeHistogram
 */
public abstract class FixationHistogram extends Histogram {

	/**
	 * The status of the view.
	 */
	private String status;

	/**
	 * The prefix of the status, short because the length of the status is limited.
	 */
	private final String statusPrefix;

	/**
	 * Construct a new view to display fixation statistics.
	 *
	 * @param engine       the pacemaker for running the model
	 * @param statusPrefix the prefix of the status
	 */
	protected FixationHistogram(EvoLudoGWT engine, String statusPrefix) {
		super(engine);
		this.statusPrefix = statusPrefix;
	}

	/**
	 * Get the reference levels for {@code graph}.
	 *
	 * @param graph the graph
	 * @return the reference levels
	 */
	protected abstract double[] getCustomLevels(HistoGraph graph);

	/**
	 * Add the sample {@code fixData} to {@code graph}.
	 *
	 * @param graph   the graph of the trait that fixated or {@code null} if the
	 *                trait has no graph
	 * @param fixData the fixation data of the sample
	 * @return {@code true} if the sample was added
	 */
	protected abstract boolean addSample(HistoGraph graph, FixationData fixData);

	/**
	 * Get the status of {@code graph}.
	 *
	 * @param graph the graph
	 * @param nSamp the number of samples
	 * @return the status
	 */
	protected abstract String getStatus(HistoGraph graph, int nSamp);

	@Override
	Mode getMode() {
		return Mode.STATISTICS_SAMPLE;
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
		// no graph for vacant trait if monostop
		int vacant = module.getVacantIdx();
		int skip = (vacant >= 0 && module instanceof Discrete && ((Discrete) module).getMonoStop()) ? vacant : -1;
		return rows(module.getNTraits(), skip);
	}

	@Override
	protected boolean applyStyle(HistoGraph graph) {
		GraphStyle style = graph.getStyle();
		style.showLabel = true;
		style.xMin = 0.0;
		if (graph.getRow() == graph.getModule().getNTraits()) {
			style.label = "Absorption";
			style.graphColor = ColorMapCSS.Color2Css(Color.BLACK);
		} else
			applyTraitLabel(graph);
		style.customYLevels = getCustomLevels(graph);
		return false;
	}

	@Override
	public void modelSettings() {
		super.modelSettings();
		for (HistoGraph graph : graphs)
			graph.getStyle().customYLevels = getCustomLevels(graph);
	}

	@Override
	public void modelSample(boolean success) {
		if (!success)
			return;
		FixationData fixData = model.getFixationData();
		if (fixData.mutantNode < 0)
			return;
		HistoGraph graph = null;
		for (HistoGraph g : graphs) {
			if (g.getRow() == fixData.typeFixed) {
				graph = g;
				break;
			}
		}
		if (!addSample(graph, fixData) || !isActive || !doUpdateView(false))
			return;
		for (HistoGraph g : graphs)
			g.paint(false);
	}

	@Override
	public String getTooltipAt(HistoGraph graph, int bar) {
		// nodes 0, ..., nPop - 1
		int max = (int) graph.getStyle().xMax;
		return getIntTooltip(graph, bar, max + 1, max, true);
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
		StringBuilder sb = new StringBuilder(statusPrefix);
		String sep = " ";
		for (HistoGraph graph : graphs) {
			if (graph.getData() == null) {
				engine.logWarning("Fixation statistics not available!");
				return "";
			}
			sb.append(sep).append(graph.getStyle().label).append(": ").append(getStatus(graph, nSamp));
			sep = ", ";
		}
		status = sb.toString();
		return status;
	}
}
