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

package org.evoludo.simulator.views;

import java.util.List;

import org.evoludo.graphics.AbstractGraph.GraphStyle;
import org.evoludo.graphics.S3Graph;
import org.evoludo.simulator.ColorMapCSS;
import org.evoludo.simulator.EvoLudoGWT;
import org.evoludo.simulator.models.DModel;
import org.evoludo.simulator.models.Data;
import org.evoludo.simulator.modules.Discrete;
import org.evoludo.simulator.modules.Module;

/**
 * The view to display time series of data as a trajectory on the \(S_3\)
 * simplex.
 * 
 * @evoludo.impl Note (yet) ready for multiple species!
 *
 * @author Christoph Hauert
 */
public class S3 extends AbstractView<S3Graph> {

	/**
	 * The current state of the model. The end point of the current trajectory.
	 */
	protected double[] state;

	/**
	 * Construct a new view to display the time series data of the current EvoLudo
	 * model as a trajectory in a \(S_3\) simplex.
	 * 
	 * @param engine the pacemaker for running the model
	 */
	public S3(EvoLudoGWT engine) {
		super(engine, Data.TRAIT);
	}

	@Override
	public String getName() {
		return "Traits - Simplex S3";
	}

	@Override
	protected void allocateGraphs() {
		Module<?> module = engine.getModule();
		int nRoles = module.getNRoles();
		if (graphs.size() != nRoles) {
			destroyGraphs();
			int[] order = new int[3];
			for (int role = 0; role < nRoles; role++) {
				S3Graph graph = new S3Graph(this, module, role);
				wrapper.add(graph);
				graphs.add(graph);
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
				// set map for converting data to S3 coordinates
				S3Map map = ((HasS3) module).getS3Map(role);
				if (map == null)
					map = new S3Map(); // no roles by default
				map.setNames(module.getTraitNames());
				map.setColors(module.getTraitColors());
				graph.setMap(map);
				style.label = map.getLabel();
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
					map.setOrder(order);
			}
			// arrange graphs horizontally
			gRows = 1;
			gCols = nRoles;
			int width = 100 / gCols;
			int height = 100 / gRows;
			for (S3Graph graph : graphs)
				graph.setSize(width + "%", height + "%");
		}
	}

	@Override
	public void unload() {
		super.unload();
		state = null;
	}

	@Override
	public void reset(boolean hard) {
		super.reset(hard);
		int nMean = model.getNMean();
		if (state == null || state.length != nMean)
			state = new double[nMean];
		for (S3Graph graph : graphs) {
			S3Map map = graph.getMap();
			Module<?> module = graph.getModule();
			graph.setMarkers(module.getMarkers());
			graph.getStyle().trajColor = ColorMapCSS.Color2Css(module.getTrajectoryColor());
			map.setNames(module.getTraitNames());
			map.setColors(module.getTraitColors());
			graph.reset();
		}
		update(hard);
	}

	@Override
	public void modelDidInit() {
		super.modelDidInit();
		for (S3Graph graph : graphs) {
			model.getMeanTraits(graph.getModule().getID(), state);
			graph.addData(Double.NaN, state, true);
			graph.paint(true);
		}
	}

	@Override
	public void update(boolean force) {
		double newtime = model.getUpdates();
		boolean isNext = (Math.abs(timestamp - newtime) > 1e-8);
		for (S3Graph graph : graphs) {
			if (isNext) {
				model.getMeanTraits(graph.getModule().getID(), state);
				graph.addData(newtime, state, force);
			}
			graph.paint(force);
		}
		timestamp = newtime;
	}

	@Override
	public boolean setInitialState(double[] init) {
		Module<?> module = engine.getModule();
		// note: setInitialTraits requires different arguments for discrete and
		// continuous modules
		if (module instanceof Discrete &&
				((DModel) model).setInitialTraits(init)) {
			engine.modelInit();
			return true;
		}
		return false;
	}

	@Override
	protected ExportType[] exportTypes() {
		return new ExportType[] { ExportType.SVG, ExportType.PNG, ExportType.TRAJ_DATA };
	}
}
