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

import java.util.ArrayList;
import java.util.List;

import org.evoludo.graphics.AbstractGraph.GraphStyle;
import org.evoludo.graphics.ParaGraph;
import org.evoludo.math.ArrayMath;
import org.evoludo.simulator.ColorMapCSS;
import org.evoludo.simulator.EvoLudoGWT;
import org.evoludo.simulator.models.Data;
import org.evoludo.simulator.models.ODE;
import org.evoludo.simulator.models.Type;
import org.evoludo.simulator.modules.Discrete;
import org.evoludo.simulator.modules.Module;
import org.evoludo.simulator.views.HasPhase2D.Data2Phase;
import org.evoludo.ui.ContextMenu;
import org.evoludo.ui.ContextMenuCheckBoxItem;
import org.evoludo.ui.ContextMenuItem;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.user.client.Command;

/**
 * The view to display time series of data as a trajectory in a 2D phase plane.
 * 
 * @author Christoph Hauert
 */
public class Phase2D extends AbstractView {

	/**
	 * The list of graphs that display the trajectories in 2D phase planes.
	 * 
	 * @evoludo.impl {@code List<ParaGraph> graphs} is deliberately hiding
	 *               {@code List<AbstractGraph> graphs} from the superclass because
	 *               it saves a lot of ugly casting. Note that the two fields point
	 *               to one and the same object.
	 */
	@SuppressWarnings("hiding")
	protected List<ParaGraph> graphs;

	/**
	 * The graph that displays the trajectory in a 2D phase plane.
	 * 
	 * @evoludo.impl {@code ParaGraph graph} is a short-cut to {@code graphs.get(0)}
	 *               as long as only a single graph is acceptable.
	 */
	protected ParaGraph graph;

	/**
	 * The current state of the model.
	 */
	protected double[] state;

	/**
	 * The map that transforms the current state of the model to a point on the 2D
	 * phase plane.
	 */
	protected Data2Phase map;

	/**
	 * Construct a new view to display the time series data of the current EvoLudo
	 * model as a trajectory in a 2D phase plane.
	 * 
	 * @param engine the pacemaker for running the model
	 */
	@SuppressWarnings("unchecked")
	public Phase2D(EvoLudoGWT engine) {
		super(engine, Data.UNDEFINED);
		graphs = (List<ParaGraph>) super.graphs;
	}

	@Override
	public String getName() {
		return "Traits - 2D Phase Plane";
	}

	@Override
	protected void allocateGraphs() {
		GraphStyle style;
		int nStates = model.getNMean();
		if (state == null || state.length != nStates)
			state = new double[nStates];
		Module module = engine.getModule();
		if (graphs.size() != 1) {
			graph = new ParaGraph(this, module);
			wrapper.add(graph);
			graphs.add(graph);
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
			int width = 100 / gCols;
			int height = 100 / gRows;
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

		Module module = engine.getModule();
		graph.setMarkers(module.getMarkers());
		// set map for converting data to phase plane coordinates
		map = ((HasPhase2D) module).getPhase2DMap();
		if (map != null)
			graph.setMap(map);
		else
			map = graph.getMap();
		((HasPhase2D) module).setPhase2DMap(map);
		// set axis labels and range
		GraphStyle style = graph.getStyle();
		if (model.getType().isDE() && ((ODE) model).isDensity()) {
			// density model
			style.percentX = false;
			style.percentY = false;
		} else {
			// frequency model
			style.percentX = true;
			style.percentY = true;
		}
		String label = map.getXAxisLabel();
		style.xLabel = (label == null ? getXAxisLabel() : label);
		style.showXLabel = (style.xLabel != null);
		label = map.getYAxisLabel();
		style.yLabel = (label == null ? getYAxisLabel() : label);
		style.showYLabel = (style.yLabel != null);
		style.trajColor = ColorMapCSS.Color2Css(module.getTrajectoryColor());
		if (hard)
			graph.reset();
		update(hard);
	}

	@Override
	public void modelDidInit() {
		super.modelDidInit();
		model.getMeanTraits(state);
		graph.addData(Double.NaN, state, true);
		graph.paint(true);
	}

	@Override
	public void update(boolean force) {
		double newtime = model.getTime();
		if (Math.abs(timestamp - newtime) > 1e-8) {
			model.getMeanTraits(state);
			graph.addData(newtime, state, force);
		}
		graph.paint(force);
		timestamp = newtime;
	}

	/**
	 * Get the label of the horizontal axis.
	 * 
	 * @return the label of the {@code x}-axis
	 */
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

	/**
	 * Get the label of the vertical axis.
	 * 
	 * @return the label of the {@code y}-axis
	 */
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

	/**
	 * Get the name of the trait with index {@code idx}. In multi-species modules
	 * the species name is prepended and the index refers to traits of all species.
	 * 
	 * @param idx the index of the trait
	 * @return the name of the trait
	 */
	private String getTraitName(int idx) {
		Module module = engine.getModule();
		ArrayList<? extends Module> species = module.getSpecies();
		int nSpecies = species.size();
		if (nSpecies > 1) {
			for (Module mod : species) {
				int nTraits = mod.getNTraits();
				if (idx < nTraits) {
					if (nTraits == 1 || (nTraits == 2 && mod.getVacant() >= 0))
						// omit species name for single trait or trait plus vacant
						return mod.getTraitName(idx);
					return mod.getName() + ": " + mod.getTraitName(idx);
				}
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
			if (((org.evoludo.simulator.models.Discrete) model).setInitialTraits(init)) {
				engine.modelInit();
				return true;
			}
		}
		return false;
	}

	@Override
	public void populateContextMenu(ContextMenu menu) {
		if (!map.hasFixedAxis()) {
			// add context menu for configuring the phase plane axis
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
			Type mt = model.getType();
			boolean isDensity = (mt.isDE() ? ((ODE) model).isDensity() : false);
			for (Module mod : species) {
				int vidx = mod.getVacant();
				int nt = mod.getNTraits();
				if (isDensity && nt == 1 | (nt == 2 && vidx >= 0))
					totTraits++;
				else
					totTraits += nt;
			}
			if (traitXMenu == null || traitXItems == null || traitXItems.length != totTraits ||
					traitYMenu == null || traitYItems == null || traitYItems.length != totTraits) {
				traitXMenu = new ContextMenu(menu);
				traitXItems = new ContextMenuCheckBoxItem[totTraits];
				traitYMenu = new ContextMenu(menu);
				traitYItems = new ContextMenuCheckBoxItem[totTraits];
				int idx = 0;
				for (Module mod : species) {
					int vidx = mod.getVacant();
					int nt = mod.getNTraits();
					if (isMultispecies && !(nt == 1 || (nt == 2 && vidx >= 0))) {
						// add separator unless it's the first species
						// or species with single trait or trait plus vacant
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
					for (int n = 0; n < nt; n++) {
						if (isDensity && n == vidx)
							continue;
						ContextMenuCheckBoxItem traitXItem = new ContextMenuCheckBoxItem(mod.getTraitName(n), //
								new TraitCommand(n, TraitCommand.X_AXIS));
						traitXMenu.add(traitXItem);
						ContextMenuCheckBoxItem traitYItem = new ContextMenuCheckBoxItem(mod.getTraitName(n), //
								new TraitCommand(n, TraitCommand.Y_AXIS));
						traitYMenu.add(traitYItem);
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

	/**
	 * The context menu for selecting traits to display on the horizontal axis.
	 */
	private ContextMenuCheckBoxItem[] traitXItems;

	/**
	 * The context menu for selecting traits to display on the vertical axis.
	 */
	private ContextMenuCheckBoxItem[] traitYItems;

	/**
	 * The context menu trigger for selecting traits to display on the horizontal
	 * axis.
	 */
	private ContextMenu traitXMenu;

	/**
	 * The context menu trigger for selecting traits to display on the vertical
	 * axis.
	 */
	private ContextMenu traitYMenu;

	/**
	 * Command to toggle the inclusion of a trait on the phase plane axis.
	 */
	public class TraitCommand implements Command {

		/**
		 * The index of the horizontal axis.
		 */
		public static final int X_AXIS = 0;

		/**
		 * The index of the vertical axis.
		 */
		public static final int Y_AXIS = 1;

		/**
		 * The index of the trait to show on the axis.
		 */
		int trait = -1;

		/**
		 * The axis that this command affects.
		 */
		int axis = -1;

		/**
		 * Construct a new command to toggle the inclusion of a trait on either one of
		 * the phase plane
		 * axis.
		 * 
		 * @param trait the index of the trait to show/hide on the axis
		 * @param axis  the index of the axis
		 */
		public TraitCommand(int trait, int axis) {
			this.trait = trait;
			this.axis = axis;
		}

		/**
		 * Toggle the inclusion of the trait on the axis.
		 */
		@Override
		public void execute() {
			if (axis == X_AXIS)
				map.setTraits(toggleState(map.getTraitsX(), traitXItems), map.getTraitsY());
			else
				map.setTraits(map.getTraitsX(), toggleState(map.getTraitsY(), traitYItems));
			Phase2D.this.reset(false);
		}

		/**
		 * Toggle the inclusion of traits that are selected in {@code items} on the
		 * current axis.
		 * 
		 * @param states the list of trait indices that are displayed on current axis
		 * @param items  the list of context menu items
		 * @return the updated list of trait indices
		 */
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
