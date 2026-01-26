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

import org.evoludo.graphics.GraphStyle;
import org.evoludo.graphics.ParaGraph;
import org.evoludo.math.ArrayMath;
import org.evoludo.simulator.ColorMapCSS;
import org.evoludo.simulator.EvoLudoGWT;
import org.evoludo.simulator.models.DModel;
import org.evoludo.simulator.models.Data;
import org.evoludo.simulator.modules.Discrete;
import org.evoludo.simulator.modules.Module;
import org.evoludo.simulator.views.HasPhase2D.Data2Phase;
import org.evoludo.ui.ContextMenu;
import org.evoludo.ui.ContextMenuCheckBoxItem;
import org.evoludo.ui.ContextMenuItem;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.user.client.Command;

/**
 * Phase2D is a view that renders the model's mean trait trajectory in a
 * two‑dimensional phase plane. It wraps a single ParaGraph instance to display
 * a continuous trajectory over time and provides interactive controls to
 * configure which traits appear on the horizontal (X) and vertical (Y) axes.
 *
 * <h3>Responsibilities</h3>
 * <ul>
 * <li>Obtain the current model state (mean traits) and forward it to the
 * {@code ParaGraph} for plotting as a time‑stamped trajectory point.</li>
 * <li>Manage a Data2Phase mapping object that transforms the model's state
 * vector into X/Y coordinates (the map may be supplied by the module or created
 * by the graph and propagated back to the module).</li>
 * <li>Configure graph appearance (axis labels, percent/density formatting,
 * colors, markers) according to module and model settings (e.g., density vs.
 * frequency).</li>
 * <li>Provide a context menu for selecting which trait(s) are summed into each
 * axis, including support for multi‑trait axes, species headers for
 * multispecies modules, and handling of "vacant" trait indices or reduced
 * entries for density models.</li>
 * <li>Support export of visualizations and trajectory data (SVG, PNG and CSV
 * trajectories).</li>
 * <li>Handle initialization and reset semantics for both continuous and
 * discrete models (forwarding initial trait values to the underlying module
 * where required).</li>
 * </ul>
 *
 * <h3>Key behavioral notes</h3>
 * <ul>
 * <li>Only a single ParaGraph is allocated and used by this view; the protected
 * {@code graph} field is a convenient reference to that single graph.</li>
 * <li>When {@link #reset(boolean)} is called the view ensures the ParaGraph
 * receives: markers from the module, the chosen {@code Data2Phase} map, axis
 * labels and percent/density flags, and the configured trajectory color. A hard
 * reset will clear the graph.</li>
 * <li>The {@code Data2Phase} map determines which trait indices contribute to
 * each axis and whether axes are fixed. If the map does not provide axis
 * labels, the view synthesizes labels from trait names (and species names when
 * appropriate).</li>
 * <li>Trait labeling logic handles multispecies modules by prepending species
 * names where useful, and by collapsing or omitting names in the common cases
 * of single trait species or species with a trait + vacant index pair.</li>
 * <li>Context menu construction skips trait selection when axes are fixed. For
 * configurable axes it computes the compact list of menu entries (accounting
 * for density models that omit vacant indices), optionally inserts disabled
 * species header items, and builds checkbox items representing each selectable
 * trait. Selection state is synchronized back to the {@code Data2Phase}
 * map.</li>
 * <li>Trait toggling is implemented by an inner {@code TraitCommand} which
 * updates the map's trait lists (adding, replacing, or removing indices as
 * appropriate), enforces at least one selected trait per axis, and updates the
 * menu checked states.</li>
 * <li>{@link #update(boolean)} only adds a new point to the ParaGraph when the
 * model's update counter has changed; it forwards the current mean traits and
 * repaints the graph.</li>
 * <li>{@link #setInitialState(double[])} forwards initial trait values to the
 * module for discrete models; when successful it triggers model initialization
 * through the engine.</li>
 * </ul>
 *
 * <h3>Integration notes</h3>
 * <ul>
 * <li>The view interacts with an {@code EvoLudoGWT} engine to obtain the
 * current Module and the associated model (for retrieving mean traits, update
 * counts and settings).</li>
 * <li>The module may be multi‑species; the view queries the module for species
 * modules, trait counts, trait names and vacant indices to construct menus and
 * labels.</li>
 * <li>The ParaGraph and Data2Phase types are central collaborators: the graph
 * is the visual component that draws trajectories and axes, while the map
 * encapsulates projection and axis configuration logic.</li>
 * </ul>
 *
 * <h3>Export support</h3>
 * The view advertises three export types: SVG, PNG and CSV of the trajectory
 * data.
 *
 * <h3>Usage</h3>
 * Place the view into the application's UI container; call {@code reset(hard)}
 * after model or module changes to synchronize graph settings, and rely on the
 * engine's pacemaker to drive periodic {@code update} calls to append
 * trajectory points.
 *
 * @author Christoph Hauert
 *
 * @see ParaGraph
 * @see Data2Phase
 * @see EvoLudoGWT
 */
public class Phase2D extends AbstractView<ParaGraph> {

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
	public Phase2D(EvoLudoGWT engine) {
		super(engine, Data.UNDEFINED);
	}

	@Override
	public String getName() {
		return "Traits - 2D Phase Plane";
	}

	@Override
	protected boolean allocateGraphs() {
		GraphStyle style;
		int nStates = model.getNMean();
		if (state == null || state.length != nStates)
			state = new double[nStates];
		Module<?> module = engine.getModule();
		if (graphs.size() == 1)
			return false;
		destroyGraphs();
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
		style.showYAxisRight = false;
		// arrange graphs vertically (currently only one)
		gRows = 1;
		gCols = 1;
		int width = 100 / gCols;
		int height = 100 / gRows;
		graph.setSize(width + "%", height + "%");
		return true;
	}

	@Override
	public void unload() {
		super.unload();
		state = null;
	}

	@Override
	public void reset(boolean hard) {
		super.reset(hard);

		Module<?> module = engine.getModule();
		graph.setMarkers(module.getMarkers());
		// set map for converting data to phase plane coordinates
		map = ((HasPhase2D) module).getPhase2DMap();
		if (map != null)
			graph.setMap(map);
		else
			map = graph.getMap();
		((HasPhase2D) module).setPhase2DMap(map);
		// set axes labels and range
		GraphStyle style = graph.getStyle();
		if (model.isDensity()) {
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
		double newtime = model.getUpdates();
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
		StringBuilder xName = new StringBuilder(getTraitName(traitX[0]));
		int nx = traitX.length;
		if (nx > 1) {
			for (int n = 1; n < nx; n++)
				xName.append("+").append(getTraitName(traitX[n]));
		}
		xName.append(graph.getStyle().percentX ? " frequency" : " density");
		return xName.toString();
	}

	/**
	 * Get the label of the vertical axis.
	 * 
	 * @return the label of the {@code y}-axis
	 */
	private String getYAxisLabel() {
		int[] traitY = map.getTraitsY();
		StringBuilder yName = new StringBuilder(getTraitName(traitY[0]));
		int ny = traitY.length;
		if (ny > 1) {
			for (int n = 1; n < ny; n++)
				yName.append("+").append(getTraitName(traitY[n]));
		}
		yName.append(graph.getStyle().percentY ? " frequency" : " density");
		return yName.toString();
	}

	/**
	 * Get the name of the trait with index {@code idx}. In multi-species modules
	 * the species name is prepended and the index refers to traits of all species.
	 * 
	 * @param idx the index of the trait
	 * @return the name of the trait
	 */
	private String getTraitName(int idx) {
		Module<?> module = engine.getModule();
		List<? extends Module<?>> species = module.getSpecies();
		int nSpecies = species.size();
		if (nSpecies > 1) {
			for (Module<?> mod : species) {
				int nTraits = mod.getNTraits();
				if (idx < nTraits) {
					if (nTraits == 1 || (nTraits == 2 && mod.getVacantIdx() >= 0))
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
		Module<?> module = engine.getModule();
		// note: setInitialTraits requires different arguments for discrete and
		// continuous modules
		if (module instanceof Discrete && ((DModel) model).setInitialTraits(init)) {
			engine.modelInit();
			return true;
		}
		return false;
	}

	@Override
	public void populateContextMenu(ContextMenu menu) {
		addAxesMenu(menu);
		if (map.hasFixedAxes()) {
			super.populateContextMenu(menu);
			return;
		}
		// add context menu for configuring the phase plane axes
		Module<?> module = engine.getModule();
		List<? extends Module<?>> species = module.getSpecies();
		int nSpecies = species.size();
		boolean isMultispecies = nSpecies > 1;
		// no menu entries if single species and less than 3 traits
		if (!isMultispecies && module.getNTraits() < 3) {
			traitXMenu = traitYMenu = null;
			traitXItems = traitYItems = null;
			super.populateContextMenu(menu);
			return;
		}
		buildTraitMenus(menu, species, isMultispecies);
		menu.addSeparator();
		menu.add("X-axis trait", traitXMenu);
		menu.add("Y-axis trait", traitYMenu);
		super.populateContextMenu(menu);
	}

	/**
	 * Add the axes submenu with autoscale, full-range, and y-axis side controls.
	 * 
	 * @param menu the context menu to populate
	 */
	private void addAxesMenu(ContextMenu menu) {
		if (axesMenu == null) {
			axesMenu = new ContextMenu(menu, "Axes");
			autoscaleMenu = new ContextMenuCheckBoxItem("Autoscale axes", () -> {
				GraphStyle style = graph.getStyle();
				boolean enable = !autoscaleMenu.isChecked();
				style.autoscaleX = enable;
				style.autoscaleY = enable;
				autoscaleMenu.setChecked(enable);
				graph.autoscale();
				graph.paint(true);
			});
			fullRangeMenu = new ContextMenuItem("Full range", () -> {
				GraphStyle style = graph.getStyle();
				if (style.autoscaleX || style.autoscaleY) {
					style.autoscaleX = false;
					style.autoscaleY = false;
					autoscaleMenu.setChecked(false);
				}
				style.xMin = 0.0;
				style.xMax = 1.0;
				style.yMin = 0.0;
				style.yMax = 1.0;
				graph.paint(true);
			});
			rightYAxisMenu = new ContextMenuCheckBoxItem("Right Y-axis", () -> {
				GraphStyle style = graph.getStyle();
				style.showYAxisRight = !style.showYAxisRight;
				rightYAxisMenu.setChecked(style.showYAxisRight);
				graph.onResize();
				graph.paint(true);
			});
		}
		axesMenu.clear();
		axesMenu.addHeader("Axes");
		GraphStyle style = graph.getStyle();
		autoscaleMenu.setChecked(style.autoscaleX && style.autoscaleY);
		rightYAxisMenu.setChecked(style.showYAxisRight);
		axesMenu.add(autoscaleMenu);
		if (style.percentX && style.percentY)
			axesMenu.add(fullRangeMenu);
		axesMenu.add(rightYAxisMenu);
		menu.add("Axes", axesMenu);
	}

	/**
	 * Build or update trait selection sub-menus for the X and Y axes.
	 * 
	 * @param parent         the parent context menu
	 * @param species        the list of species modules
	 * @param isMultispecies whether multiple species are present
	 */
	private void buildTraitMenus(ContextMenu parent, List<? extends Module<?>> species, boolean isMultispecies) {
		int totTraits = computeTotalTraits(species, model.isDensity());
		if (traitXMenu == null || traitXItems == null || traitXItems.length != totTraits ||
				traitYMenu == null || traitYItems == null || traitYItems.length != totTraits) {
			traitXMenu = new ContextMenu(parent, "X-axis trait");
			traitYMenu = new ContextMenu(parent, "Y-axis trait");
			traitXItems = new ContextMenuCheckBoxItem[totTraits];
			traitYItems = new ContextMenuCheckBoxItem[totTraits];
			populateTraitItems(species, isMultispecies);
			// restore checked state from map
			for (int n : map.getTraitsX())
				traitXItems[n].setChecked(true);
			for (int n : map.getTraitsY())
				traitYItems[n].setChecked(true);
		}
	}

	/**
	 * Compute total number of trait entries that will appear in the trait menus.
	 * 
	 * @param species   the list of species modules
	 * @param isDensity whether the model is a density model
	 * @return the total number of trait entries
	 */
	private int computeTotalTraits(List<? extends Module<?>> species, boolean isDensity) {
		int totTraits = 0;
		for (Module<?> mod : species) {
			int vidx = mod.getVacantIdx();
			int nt = mod.getNTraits();
			if (isDensity && nt == 1 || (nt == 2 && vidx >= 0))
				totTraits++;
			else
				totTraits += nt;
		}
		return totTraits;
	}

	/**
	 * Populate the trait menu items and optional species headers.
	 * 
	 * @param species        the list of species modules
	 * @param isMultispecies whether multiple species are present
	 */
	private void populateTraitItems(List<? extends Module<?>> species, boolean isMultispecies) {
		int idx = 0;
		for (Module<?> mod : species) {
			int vidx = mod.getVacantIdx();
			int nt = mod.getNTraits();
			if (isMultispecies && !(nt == 1 || (nt == 2 && vidx >= 0))) {
				// add separator unless it's the first species
				// or species with single trait or trait plus vacant
				if (idx > 0) {
					traitXMenu.addSeparator();
					traitYMenu.addSeparator();
				}
				addDisabledSpeciesName(mod, traitXMenu);
				addDisabledSpeciesName(mod, traitYMenu);
			}
			for (int n = 0; n < nt; n++) {
				if (model.isDensity() && n == vidx)
					continue;
				ContextMenuCheckBoxItem traitXItem = new ContextMenuCheckBoxItem(mod.getTraitName(n),
						new TraitCommand(n, TraitCommand.X_AXIS));
				traitXMenu.add(traitXItem);
				ContextMenuCheckBoxItem traitYItem = new ContextMenuCheckBoxItem(mod.getTraitName(n),
						new TraitCommand(n, TraitCommand.Y_AXIS));
				traitYMenu.add(traitYItem);
				traitXItems[idx] = traitXItem;
				traitYItems[idx] = traitYItem;
				idx++;
			}
		}
	}

	/**
	 * Add a disabled menu entry used as species header.
	 * 
	 * @param mod  the species module
	 * @param menu the context menu to populate
	 */
	private void addDisabledSpeciesName(Module<?> mod, ContextMenu menu) {
		ContextMenuItem speciesName = new ContextMenuItem(mod.getName(), (Scheduler.ScheduledCommand) null);
		speciesName.getElement().getStyle()
				.setFontWeight(com.google.gwt.dom.client.Style.FontWeight.BOLD);
		speciesName.setEnabled(false);
		menu.add(speciesName);
	}

	@Override
	protected ExportType[] exportTypes() {
		return new ExportType[] { ExportType.SVG, ExportType.PNG, ExportType.CSV_TRAJ };
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
	 * The context menu for axis-related controls.
	 */
	private ContextMenu axesMenu;

	/**
	 * The context menu item to toggle autoscaling on both axes.
	 */
	private ContextMenuCheckBoxItem autoscaleMenu;

	/**
	 * The context menu item to set the full frequency range.
	 */
	private ContextMenuItem fullRangeMenu;

	/**
	 * The context menu item to toggle the y-axis side.
	 */
	private ContextMenuCheckBoxItem rightYAxisMenu;

	/**
	 * Command to toggle the inclusion of a trait on the phase plane axes.
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
		 * the phase plane axes.
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
