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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Set;

import org.evoludo.geom.Point2D;
import org.evoludo.graphics.AbstractGraph.GraphStyle;
import org.evoludo.graphics.ParaGraph;
import org.evoludo.math.ArrayMath;
import org.evoludo.simulator.ColorMapCSS;
import org.evoludo.simulator.EvoLudoGWT;
import org.evoludo.simulator.models.Model;
import org.evoludo.simulator.models.ODEEuler;
import org.evoludo.simulator.modules.Discrete;
import org.evoludo.simulator.modules.Module;
import org.evoludo.simulator.views.HasPhase2D.Data2Phase;
import org.evoludo.ui.ContextMenu;
import org.evoludo.ui.ContextMenuCheckBoxItem;
import org.evoludo.ui.ContextMenuItem;
import org.evoludo.util.Formatter;
import org.evoludo.util.RingBuffer;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.user.client.Command;

/**
 *
 * @author Christoph Hauert
 */
public class Phase2D extends AbstractView {

	@SuppressWarnings("hiding")
	protected Set<ParaGraph> graphs;
// short-cut as long as only a single ParaGraph graph is acceptable
	protected ParaGraph graph;
	protected int nStates;
	protected double[] state;
	protected double[] minstate;
	protected double[] maxstate;
	protected Data2Phase map;

	/**
	 * @param engine the pacemeaker for running the model
	 */
	@SuppressWarnings("unchecked")
	public Phase2D(EvoLudoGWT engine) {
		super(engine, Model.Data.UNDEFINED);
		graphs = (Set<ParaGraph>) super.graphs;
	}

	@Override
	public String getName() {
		return "Strategies - 2D Phase Plane";
	}

	@Override
	public void unload() {
		super.unload();
		state = null;
	}

	@Override
	public void reset(boolean soft) {
		super.reset(soft);

		GraphStyle style;
		nStates = getNStates();
		if( state==null || state.length!=nStates ) {
			state = new double[nStates];
			minstate = new double[nStates];
			maxstate = new double[nStates];
		}
		Module module = engine.getModule();
		if( graphs.size()!=1 ) {
			soft = false;
			graph = new ParaGraph(this, nStates, module.getID());
			wrapper.add(graph);
			graphs2mods.put(graph, module);
			style = graph.getStyle();
			style.showLabel = true;
			style.showXTicks = true;
			style.showXTickLabels = true;
			style.showXLevels = false;
			style.showYTicks = true;
			style.showYTickLabels = true;
			style.showYLevels = false;
			// arrange graphs vertically (currently only one)
			gRows = 1;
			gCols = 1;
			int width = 100/gCols;
			int height = 100/gRows;
			graph.setSize(width+"%", height+"%");
		}
		graph.setMarkers(module.getMarkers());
		if (map == null) {
			map = ((HasPhase2D) module).getMap();
			if (map == null) {
				map = new TraitMap();
				((HasPhase2D) module).setPhase2DMap(map);
			}
			graph.setMap(map);
		}
		// set axis labels and range
		style = graph.getStyle();
		if (model instanceof Model.DE && ((Model.DE) model).isDensity()) {
			// density model
			style.percentX = false;
			style.percentY = false;
		}
		else {
			// frequency model
			style.percentX = true;
			style.percentY = true;
		}
		style.xLabel = map.getXAxisLabel();
		style.showXLabel = (style.xLabel!=null);
		style.yLabel = map.getYAxisLabel();
		style.showYLabel = (style.yLabel!=null);
		style.trajColor = ColorMapCSS.Color2Css(module.getTrajectoryColor());
		// reset min/max
		Arrays.fill(minstate, Double.MAX_VALUE);
		Arrays.fill(maxstate, -Double.MAX_VALUE);
		updateMinMaxState();
		if( !soft ) {
			graph.reset();
			update();
			graph.autoscale();
		}
	}

	@Override
	public void init() {
		super.init();
		model.getMeanTraits(state);
		updateMinMaxState();
		graph.addData(Double.NaN, state, true);
	}

	@Override
	public void update(boolean force) {
		double newtime = model.getTime();
		if( Math.abs(timestamp-newtime)>1e-8 ) {
			model.getMeanTraits(state);
			updateMinMaxState();
			graph.addData(newtime, state, force);
		}
		graph.paint();
		timestamp = newtime;
	}

	private void updateMinMaxState() {
		for (int n=0; n<nStates; n++) {
			double sn = state[n];
			if (sn > maxstate[n])
				maxstate[n] = sn;
			if (sn < minstate[n])
				minstate[n] = sn;
		}
	}

	protected int getNStates() {
		Module module = engine.getModule();
		ArrayList<? extends Module> species = module.getSpecies();
		int totTraits = 0;
		for (Module mod : species)
			totTraits += mod.getNTraits();
		return totTraits;
	}

	@Override
	public boolean setInitialState(double[] init) {
		// no further processing should be needed - just forward to module and engine
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

	ContextMenu currentMenu;

	@Override
	public void populateContextMenu(ContextMenu menu) {
		currentMenu = menu;
		map.populateContextMenu();
		currentMenu = null;
		super.populateContextMenu(menu);		
	}

	@Override
	protected int[] exportTypes() {
		return new int[] { EXPORT_SVG, EXPORT_PNG };
	}

	public class TraitMap implements Data2Phase {

		// phase plane projections can be the sum of several dynamical variables
		protected int[] stateX = new int[] { 0 };
		protected int[] stateY = new int[] { 1 };
		boolean multi = false;

		@Override
		public void setTraits(int[] x, int[] y) {
			if (!multi) {
				stateX[0] = x[0];
				stateY[0] = y[0];
				return;
			}
			stateX = ArrayMath.clone(x);
			stateY = ArrayMath.clone(y);
		}

		@Override
		public void setMultitrait(boolean multi) {
			this.multi = multi;
		}

		@Override
		public boolean data2Phase(double[] data, Point2D point) {
			// NOTE: data[0] is time
			if (stateX.length == 1) {
				point.x = data[stateX[0] + 1];
			} else {
				point.x = 0.0;
				for (int n : stateX)
					point.x += data[n + 1];
			}
			if (stateY.length == 1) {
				point.y = data[stateY[0] + 1];
			} else {
				point.y = 0.0;
				for (int n : stateY)
					point.y += data[n + 1];
			}
			return true;
		}

		@Override
		public boolean phase2Data(Point2D point, double[] data) {
			// point is in user coordinates
			// data is the last/most recent state in buffer (excluding time!)
			if (stateX.length != 1 || stateY.length != 1)
				return false;
			// conversion only possible phase plane axis each represents a single
			// dynamical variable, i.e. no aggregates
			data[stateX[0]] = point.x;
			data[stateY[0]] = point.y;
			return true;
		}

		@Override
		public String getXAxisLabel() {
			String xName = getTraitName(stateX[0]);
			int nx = stateX.length;
			if ( nx > 1) {
				for (int n=1; n<nx; n++)
				xName += "+"+getTraitName(stateX[n]);
			}
			return xName + (graph.getStyle().percentX ? " frequency" : " density");
		}

		@Override
		public String getYAxisLabel() {
			String yName = getTraitName(stateY[0]);
			int ny = stateY.length;
			if ( ny > 1) {
				for (int n=1; n<ny; n++)
				yName += "+"+getTraitName(stateX[n]);
			}
			return yName + (graph.getStyle().percentY ? " frequency" : " density");
		}

		@Override
		public double getMinX(RingBuffer<double[]> buffer) {
			double minx = minstate[stateX[0]];
			int nx = stateX.length;
			if (nx == 1)
				return minx;
			for (int n = 1; n < nx; n++)
				minx += minstate[stateX[n]];
			return minx;
		}

		@Override
		public double getMaxX(RingBuffer<double[]> buffer) {
			double maxx = maxstate[stateX[0]];
			int nx = stateX.length;
			if (nx == 1)
				return maxx;
			for (int n = 1; n < nx; n++)
				maxx += maxstate[stateX[n]];
			return maxx;
		}

		@Override
		public double getMinY(RingBuffer<double[]> buffer) {
			double miny = minstate[stateY[0]];
			int ny = stateY.length;
			if (ny == 1)
				return miny;
			for (int n = 1; n < ny; n++)
				miny += minstate[stateY[n]];
			return miny;
		}

		@Override
		public double getMaxY(RingBuffer<double[]> buffer) {
			double maxy = maxstate[stateY[0]];
			int nx = stateY.length;
			if (nx == 1)
				return maxy;
			for (int n = 1; n < nx; n++)
				maxy += maxstate[stateY[n]];
			return maxy;
		}

		@Override
		public String getTooltipAt(double x, double y) {
			GraphStyle style = graph.getStyle();
			boolean isDensity = (model instanceof ODEEuler && ((Model.DE) model).isDensity());
			String tip = "<table><tr><td style='text-align:right'><i>" + style.xLabel //
					+ ":</i></td><td>" + (isDensity ? Formatter.format(x, 2) : Formatter.formatPercent(x, 2)) + "</td></tr>";
			tip += "<tr><td style='text-align:right'><i>" + style.yLabel //
					+ ":</i></td><td>" + (isDensity ? Formatter.format(y, 2) : Formatter.formatPercent(y, 2)) + "</td></tr>";
			tip += "<tr><td colspan='2' style='font-size:1pt'><hr/></td></tr>";
			for (int n = 0; n < nStates; n++) {
				tip += "<tr><td style='text-align:right'><i>" + getTraitName(n) + ":</i></td><td>" //
						+ (isDensity ? Formatter.format(state[n], 2) : Formatter.formatPercent(state[n], 2)) + "</td></tr>";
			}
			tip += "</table>";
			return tip;
		}

		protected String getTraitName(int idx) {
			Module module = engine.getModule();
			ArrayList<? extends Module> species = module.getSpecies();
			int nSpecies = species.size();
			if (nSpecies > 1) {
				for (Module mod : species) {
					int nTraits = mod.getNTraits();
					if (idx < nTraits)					
						return mod.getName() + ": " + mod.getTraitName(idx);
					idx -= nTraits;
				}
				// trait not found... should not get here!
				return null;
			}
			return module.getTraitName(idx);
		}

		private ContextMenuCheckBoxItem[] traitXItems, traitYItems;
		private ContextMenu traitXMenu, traitYMenu;

		@Override
		public void populateContextMenu() {
			boolean isDensity = (model instanceof ODEEuler && ((Model.DE) model).isDensity());
			Module module = engine.getModule();
			ArrayList<? extends Module> species = module.getSpecies();
			int nSpecies = species.size();
			boolean isMultispecies = nSpecies > 1;
			// no menu entries if single species and less than 3 traits
			if (!isMultispecies) {
				if (module.getNTraits() < 3) {
					traitXMenu = traitYMenu = null;
					traitXItems = traitYItems = null;
					return;
				}
				// in multi-species models the menu includes species names
				nSpecies = 0;
			}
			int totTraits = 0;
			for (Module mod : species)
				totTraits += mod.getNTraits();
			if (traitXMenu == null || traitXItems == null || traitXItems.length != totTraits ||
					traitYMenu == null || traitYItems == null || traitYItems.length != totTraits) {
				traitXMenu = new ContextMenu(currentMenu);
				traitXItems = new ContextMenuCheckBoxItem[totTraits];
				traitYMenu = new ContextMenu(currentMenu);
				traitYItems = new ContextMenuCheckBoxItem[totTraits];
				int idx = 0;
				for (Module mod : species) {
					int vacant = mod.getVacant();
					int nTraits = mod.getNTraits();
					if (isMultispecies) {
						// add separator unless it's the first species
						if (idx > 0) {
							traitXMenu.addSeparator();
							traitYMenu.addSeparator();
						}
						// add species name as disabled menu entry
						ContextMenuItem speciesName = new ContextMenuItem(mod.getName(),
								(Scheduler.ScheduledCommand) null);
						speciesName.getElement().getStyle().setFontWeight(com.google.gwt.dom.client.Style.FontWeight.BOLD);
						speciesName.setEnabled(false);
						traitXMenu.add(speciesName);
						// cannot add same item to two menus...
						speciesName = new ContextMenuItem(mod.getName(),
								(Scheduler.ScheduledCommand) null);
						speciesName.getElement().getStyle().setFontWeight(com.google.gwt.dom.client.Style.FontWeight.BOLD);
						speciesName.setEnabled(false);
						traitYMenu.add(speciesName);
					}
					for (int n = 0; n < nTraits; n++) {
						ContextMenuCheckBoxItem traitXItem = new ContextMenuCheckBoxItem(mod.getTraitName(n), //
								new TraitCommand(idx, TraitCommand.X_AXIS));
						traitXMenu.add(traitXItem);
						ContextMenuCheckBoxItem traitYItem = new ContextMenuCheckBoxItem(mod.getTraitName(n), //
								new TraitCommand(idx, TraitCommand.Y_AXIS));
						traitYMenu.add(traitYItem);
						if (isDensity && n == vacant) {
							traitXItem.setEnabled(false);
							traitYItem.setEnabled(false);
						}
						traitXItems[idx] = traitXItem;
						traitYItems[idx] = traitYItem;
						idx++;
					}
				}
				for (int n : stateX)
					traitXItems[n].setChecked(true);
				for (int n : stateY)
					traitYItems[n].setChecked(true);
			}
			currentMenu.addSeparator();
			currentMenu.add("X-axis trait...", traitXMenu);
			currentMenu.add("Y-axis trait...", traitYMenu);
		}

		public class TraitCommand implements Command {
			public static final int X_AXIS = 0;
			public static final int Y_AXIS = 1;
			int trait = -1;
			int axis = -1;

			public TraitCommand(int trait, int axis) {
				this.trait = trait;
				this.axis = axis;
			}

			@Override
			public void execute() {
				if(axis == X_AXIS)
					stateX = toggleState(stateX, traitXItems);
				else
					stateY = toggleState(stateY, traitYItems);
				reset(true);
				graph.autoscale();
				graph.paint();
			}

			int[] toggleState(int[] states, ContextMenuCheckBoxItem[] items) {
				int idx = ArrayMath.first(states, trait);
				if (idx < 0) {
					if (multi) {
						// add trait to axis
						states = ArrayMath.append(states, trait);
					} else {
						// replace trait
						states[0] = trait;
					}
				} else {
					// remove trait from axis
					// do not deselect last item
					if (states.length > 1)
						states = ArrayMath.drop(states, idx);
				}
				// make sure all items are unchecked, then check current one
				for (ContextMenuCheckBoxItem item : items)
					item.setChecked(false);
				for (int n : states)
					items[n].setChecked(true);
				return states;
			}
		}
	}
}
