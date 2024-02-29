//
// EvoLudo Project
//
// Copyright 2010 Christoph Hauert
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
//	Hauert, Christoph (<year>) EvoLudo Project, http://www.evoludo.org
//			(doi: <doi>[, <version>])
//
//	<doi>:	digital object identifier of the downloaded release (or the
//			most recent release if downloaded from github.com),
//	<year>:	year of release (or download), and
//	[, <version>]: optional version number (as reported in output header
//			or GUI console) to simplify replication of reported results.
//
// The formatting may be adjusted to comply with publisher requirements.
//

package org.evoludo.simulator.views;

import java.awt.Color;
import java.util.Set;

import org.evoludo.graphics.AbstractGraph.GraphStyle;
import org.evoludo.graphics.S3Graph;
import org.evoludo.simulator.ColorMapCSS;
import org.evoludo.simulator.EvoLudoGWT;
import org.evoludo.simulator.models.Model;
import org.evoludo.simulator.modules.Discrete;
import org.evoludo.simulator.modules.Module;

/**
 *
 * @author Christoph Hauert
 */
public class S3 extends AbstractView {

	@SuppressWarnings("hiding")
	protected Set<S3Graph> graphs;
	protected double[] state, init;
	protected int nState;

	/**
	 * @param engine the pacemeaker for running the model
	 */
	@SuppressWarnings("unchecked")
	public S3(EvoLudoGWT engine) {
		super(engine, Model.Data.STRATEGY);
		graphs = (Set<S3Graph>) super.graphs;
	}

	@Override
	public String getName() {
		return "Strategies - Simplex S3";
	}

	@Override
	public void unload() {
		super.unload();
		state = null;
		init = null;
	}

	@Override
	public void clear() {
		super.clear();
		for( S3Graph graph : graphs )
			graph.clearGraph();
		update();
	}

	@Override
	public void reset(boolean soft) {
		super.reset(soft);
		Module module = engine.getModule();
		int nRoles = module.getNRoles();
		if (graphs.size() != nRoles) {
			soft = false;
			destroyGraphs();
			int[] order = new int[3];
			for (int role = 0; role < nRoles; role++) {
				S3Graph graph = new S3Graph(this, role);
				wrapper.add(graph);
				graphs2mods.put(graph, module);
				GraphStyle style = graph.getStyle();
				style.showLabel = true;
				style.showXTicks = true;
				style.showXTickLabels = true;
				style.showXLevels = true;
				style.showXLabel = true;
				style.showYTicks = style.showXTicks;
				style.showYTickLabels = false;
				style.showYLabel = false;
				style.showYLevels = false;
				// show first three active traits
				boolean[] active = module.getActiveTraits();
				int idx = 0;
				for (int n = 0; n < active.length; n++) {
					if (!active[n])
						continue;
					order[idx++] = n;
					if (idx == 3)
						break;
				}
				if (idx != 3)
					// less than 3 active traits
					graph.displayMessage("Simplex S3 view requires at least 3 active traits!");
				else
					graph.setOrder(order);
			}
			// arrange graphs horizontally
			gRows = 1;
			gCols = nRoles;
			int width = 100 / gCols;
			int height = 100 / gRows;
			for (S3Graph graph : graphs)
				graph.setSize(width + "%", height + "%");
		}
		Color[] colors = module.getTraitColors();
		String[] names = module.getTraitNames();
		nState = module.getNTraits();
		if (state == null || state.length != nState)
			state = new double[nState];
		if (init == null || init.length != nState)
			init = new double[nState];
		for (S3Graph graph : graphs) {
			graph.setMarkers(module.getMarkers());
			graph.getStyle().trajColor = ColorMapCSS.Color2Css(module.getTrajectoryColor());
			soft &= !graph.setNames(names);
			soft &= !graph.setColors(colors);
			if (!soft)
				graph.reset();
		}
		update();
	}

	@Override
	public void init() {
		super.init();
		for (S3Graph graph : graphs) {
			model.getMeanTraits(graph.getTag(), state);
			graph.addData(Double.NaN, state, true);
		}
	}

	@Override
	public void update(boolean force) {
		double newtime = model.getTime();
		boolean isNext = (Math.abs(timestamp - newtime) > 1e-8);
		for (S3Graph graph : graphs) {
			if(isNext) {
				model.getMeanTraits(graph.getTag(), state);
				graph.addData(newtime, state, force);
			}
			graph.paint();
		}
		timestamp = newtime;
	}

	@Override
	public boolean setInitialState(double[] init) {
		Module module = engine.getModule();
		if (module instanceof Discrete) {
			// note: setInitialTraits requires different arguments for discrete and
			// continuous modules
			if (model.setInitialTraits(init)) {
				engine.modelReinit();
				return true;
			}
		}
		return false;
	}

	@Override
	protected int[] exportTypes() {
		return new int[] { EXPORT_SVG, EXPORT_PNG };
	}
}
