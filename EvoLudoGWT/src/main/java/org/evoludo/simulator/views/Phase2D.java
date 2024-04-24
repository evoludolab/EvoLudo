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
import java.util.Set;

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
	public void reset(boolean hard) {
		super.reset(hard);

		GraphStyle style;
		nStates = getNStates();
		if( state==null || state.length!=nStates ) {
			state = new double[nStates];
		}
		Module module = engine.getModule();
		if( graphs.size()!=1 ) {
			hard = true;
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
		// set map for converting data to phase plane coordinates
		map = ((HasPhase2D) module).getPhase2DMap();
		if (map != null)
			graph.setMap(map);	
		map = graph.getMap();	
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
		String label = map.getXAxisLabel();
		style.xLabel = (label == null ? getXAxisLabel() : label);
		style.showXLabel = (style.xLabel != null);
		label = map.getYAxisLabel();
		style.yLabel = (label == null ? getYAxisLabel() : label);
		style.showYLabel = (style.yLabel!=null);
		style.trajColor = ColorMapCSS.Color2Css(module.getTrajectoryColor());
		if (hard)
			graph.reset();
		else
			graph.autoscale();
		update(hard);
	}

	@Override
	public void init() {
		super.init();
		model.getMeanTraits(state);
		graph.addData(Double.NaN, state, true);
	}

	@Override
	public void update(boolean force) {
		double newtime = model.getTime();
		if( Math.abs(timestamp-newtime)>1e-8 ) {
			model.getMeanTraits(state);
			graph.addData(newtime, state, force);
		}
		graph.paint(force);
		timestamp = newtime;
	}

	protected int getNStates() {
		Module module = engine.getModule();
		ArrayList<? extends Module> species = module.getSpecies();
		int totTraits = 0;
		for (Module mod : species)
			totTraits += mod.getNTraits();
		return totTraits;
	}

	private String getXAxisLabel() {
		int[] traitX = map.getTraitsX();
		String xName = getTraitName(traitX[0]);
		int nx = traitX.length;
		if (nx > 1) {
			for (int n = 1; n < nx; n++)
				xName += "+" + getTraitName(traitX[n]);
		}
		return xName + (graph.getStyle().percentX ? " frequency" : " density");
	}

	private String getYAxisLabel() {
		int[] traitY = map.getTraitsY();
		String yName = getTraitName(traitY[0]);
		int ny = traitY.length;
		if (ny > 1) {
			for (int n = 1; n < ny; n++)
				yName += "+" + getTraitName(traitY[n]);
		}
		return yName + (graph.getStyle().percentY ? " frequency" : " density");
	}

	private String getTraitName(int idx) {
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

	@Override
	public void populateContextMenu(ContextMenu menu) {
		if (!map.hasFixedAxis()) {
			// add context menu for configuring the phase plane axis
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
				traitXMenu = new ContextMenu(menu);
				traitXItems = new ContextMenuCheckBoxItem[totTraits];
				traitYMenu = new ContextMenu(menu);
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
						speciesName.getElement().getStyle()
								.setFontWeight(com.google.gwt.dom.client.Style.FontWeight.BOLD);
						speciesName.setEnabled(false);
						traitXMenu.add(speciesName);
						// cannot add same item to two menus...
						speciesName = new ContextMenuItem(mod.getName(),
								(Scheduler.ScheduledCommand) null);
						speciesName.getElement().getStyle()
								.setFontWeight(com.google.gwt.dom.client.Style.FontWeight.BOLD);
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
				for (int n : map.getTraitsX())
					traitXItems[n].setChecked(true);
				for (int n : map.getTraitsY())
					traitYItems[n].setChecked(true);
			}
			menu.addSeparator();
			menu.add("X-axis trait...", traitXMenu);
			menu.add("Y-axis trait...", traitYMenu);
		}
		super.populateContextMenu(menu);		
	}

	@Override
	protected ExportType[] exportTypes() {
		return new ExportType[] { ExportType.SVG, ExportType.PNG, ExportType.TRAJ_DATA };
	}

	private ContextMenuCheckBoxItem[] traitXItems, traitYItems;
	private ContextMenu traitXMenu, traitYMenu;

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
				map.setTraits(toggleState(map.getTraitsX(), traitXItems), map.getTraitsY());
			else
				map.setTraits(map.getTraitsX(), toggleState(map.getTraitsY(), traitYItems));
			Phase2D.this.reset(false);
		}

		int[] toggleState(int[] states, ContextMenuCheckBoxItem[] items) {
			int idx = ArrayMath.first(states, trait);
			if (idx < 0) {
				if (map.hasMultitrait()) {
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
