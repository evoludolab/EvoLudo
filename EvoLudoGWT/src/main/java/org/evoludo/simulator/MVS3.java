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

package org.evoludo.simulator;

import java.awt.Color;
import java.util.Set;

import org.evoludo.graphics.AbstractGraph;
import org.evoludo.graphics.AbstractGraph.GraphStyle;
import org.evoludo.graphics.S3Graph;
import org.evoludo.simulator.models.Model;
import org.evoludo.simulator.modules.Discrete;
import org.evoludo.simulator.modules.Module;

/**
 *
 * @author Christoph Hauert
 */
public class MVS3 extends MVAbstract implements AbstractGraph.InitController {

	@SuppressWarnings("hiding")
	protected Set<S3Graph> graphs;
// short-cut as long as only a single S3 graph is acceptable
	protected S3Graph graph;
	protected double[] state, init;
	protected int nState, nActive;
	protected boolean[] active;

	protected boolean isEnabled = true;

	/**
	 * @param engine the pacemeaker for running the model
	 */
	@SuppressWarnings("unchecked")
	public MVS3(EvoLudoGWT engine) {
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
		active = null;
	}

	@Override
	public void clear() {
		super.clear();
		graph.clearGraph();
		update();
	}

	@Override
	public void update(boolean force) {
		if( !isEnabled ) {
			graph.displayMessage("Simplex S3 view requires exactly 3 active traits!");
			return;
		}
		Model model = engine.getModel();
		double newtime = model.getTime();
		if( Math.abs(timestamp-newtime)>1e-8 ) {
			model.getMeanTraits(graph.getTag(), state);
			double[] s3 = state;
			if( nActive!=nState ) {
				s3 = new double[3];
				int n = 0;
				for( int i=0; i<nState; i++ ) {
					if( active[i] )
						s3[n++] = state[i];
				}
			}
			graph.addData(newtime, state, force);
		}
		graph.paint();
		timestamp = newtime;
	}

	@Override
	public void reset(boolean soft) {
		super.reset(soft);
		Module module = engine.getModule();
		if( graphs.size()!=1 ) {
			soft = false;
			graph = new S3Graph(this, module.getID());
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
			// arrange graphs (currently only one)
			gRows = 1;
			gCols = 1;
			int width = 100/gCols;
			int height = 100/gRows;
			graph.setSize(width+"%", height+"%");
		}
		Color[] colors = module.getTraitColors();
		String[] names = module.getTraitNames();
		nActive = module.getNActive();
		if( nActive!=3 ) {
			// now we have a problem... what to do?
			graph.setNames(names);
			graph.setColors(colors);
			isEnabled = false;
			update();
			return;
		}
		graph.setMarkers(module.getMarkers());
		nState = module.getNTraits();
		if (state == null || state.length != nState)
			state = new double[nState];
		if (init == null || init.length != nState)
			init = new double[nState];
		graph.getStyle().trajColor = ColorMapCSS.Color2Css(module.getTrajectoryColor());
		if (nActive != nState) {
			active = module.getActiveTraits();
			String[] aNames = new String[nActive];
			Color[] aColors = new Color[nActive];
			int n = 0;
			for (int i=0; i<nState; i++) {
				if (active == null || active[i]) {
					aNames[n] = names[i];
					aColors[n] = colors[i];
					n++;
				}
			}
			names = aNames;
			colors = aColors;
		}
		isEnabled = true;
		soft &= !graph.setNames(names);
		soft &= !graph.setColors(colors);
		if( !soft )
			graph.reset();
		update();
	}

	@Override
	public boolean setInit(double[] init) {
		if( nActive!=nState ) {
			double[] s = new double[nState];
			int n = 0;
			for( int i=0; i<nState; i++ ) {
				if( active[i] )
					s[i] = init[n++];
			}
			init = s;
		}
		Module module = engine.getModule();
		if (module instanceof Discrete) {
			// note: setInitialTraits requires different arguments for discrete and continuous modules
			return engine.getModel().setInitialTraits(init);
		}
		return false;
	}

	@Override
	protected int[] exportTypes() {
		return new int[] { EXPORT_SVG, EXPORT_PNG };
	}
}
