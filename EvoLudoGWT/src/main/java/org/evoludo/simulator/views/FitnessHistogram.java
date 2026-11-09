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
import org.evoludo.simulator.ColorMap;
import org.evoludo.simulator.ColorMapCSS;
import org.evoludo.simulator.EvoLudoGWT;
import org.evoludo.simulator.models.CModel;
import org.evoludo.simulator.models.DModel;
import org.evoludo.simulator.modules.Module;

/**
 * Histograms of the payoffs. Discrete modules show one histogram per trait
 * (except vacant sites) with a marker for the payoff of the monomorphic
 * population. Continuous modules show a single histogram with markers for the
 * minimum and maximum monomorphic payoffs.
 *
 * @author Christoph Hauert
 */
public class FitnessHistogram extends TraitHistogram {

	/**
	 * Construct a new view to display the histograms of the payoffs.
	 *
	 * @param engine the pacemaker for running the model
	 */
	public FitnessHistogram(EvoLudoGWT engine) {
		super(engine);
	}

	@Override
	public String getName() {
		return "Fitness - Histogram";
	}

	@Override
	protected boolean isAutoscaleY() {
		return true;
	}

	@Override
	protected int[] getGraphRows(Module<?> module) {
		// vacant sites are a trait only in discrete modules; the model skips vacant
		// sites when binning payoffs, which requires them to be the last trait
		return model.isContinuous() ? rows(1, -1) : rows(module.getNTraits(), module.getVacantIdx());
	}

	@Override
	protected int[] resetGraph(HistoGraph graph, int idx) {
		graph.clearMarkers();
		return new int[] { getGraphRows(graph.getModule()).length, histogramBins };
	}

	@Override
	protected boolean applyStyle(HistoGraph graph) {
		GraphStyle style = graph.getStyle();
		if (model.isDensity()) {
			style.yLabel = "density";
			style.percentY = false;
		}
		style.xLabel = "payoffs";
		Module<?> module = graph.getModule();
		int id = module.getId();
		int row = graph.getRow();
		Color color = module.getTraitColors()[row];
		if (model.isContinuous()) {
			style.showLabel = isMultispecies;
			style.graphColor = ColorMapCSS.Color2Css(Color.BLACK);
			CModel cmodel = (CModel) model;
			graph.addMarker(cmodel.getMinMonoScore(id),
					ColorMapCSS.Color2Css(ColorMap.blendColors(color, Color.BLACK, 0.5)),
					"minimum monomorphic payoff");
			graph.addMarker(cmodel.getMaxMonoScore(id),
					ColorMapCSS.Color2Css(ColorMap.blendColors(color, Color.WHITE, 0.5)),
					"maximum monomorphic payoff");
		} else {
			style.showLabel = true;
			applyTraitLabel(graph);
			double mono = ((DModel) model).getMonoScore(id, row);
			if (!Double.isNaN(mono))
				graph.addMarker(mono, ColorMapCSS.Color2Css(ColorMap.blendColors(color, Color.WHITE, 0.5)),
						"monomorphic payoff");
		}
		return setXRange(style, model.getMinScore(id), model.getMaxScore(id));
	}

	@Override
	protected void getHistogramData(int id, double[][] data) {
		model.getFitnessHistogramData(id, data);
	}
}
