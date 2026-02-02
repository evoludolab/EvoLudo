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
// Unless required by applicable law or agreed to in writing, hardware
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
import java.util.List;

import org.evoludo.graphics.Network3DGWT;
import org.evoludo.graphics.PopGraph3D;
import org.evoludo.simulator.ColorMap;
import org.evoludo.simulator.ColorMap3D;
import org.evoludo.simulator.geometries.AbstractGeometry;
import org.evoludo.simulator.EvoLudoGWT;
import org.evoludo.simulator.models.CModel;
import org.evoludo.simulator.models.DModel;
import org.evoludo.simulator.models.Data;
import org.evoludo.simulator.models.Model.HasDE;
import org.evoludo.simulator.models.ModelType;
import org.evoludo.simulator.modules.Continuous;
import org.evoludo.simulator.modules.Continuous.ColorModelType;
import org.evoludo.simulator.modules.Discrete;
import org.evoludo.simulator.modules.Map2Fitness;
import org.evoludo.simulator.modules.Module;
import org.evoludo.ui.ContextMenu;
import org.evoludo.ui.ContextMenuCheckBoxItem;

import thothbot.parallax.core.shared.materials.MeshLambertMaterial;

/**
 * 3D population view for the EvoLudo model.
 * <p>
 * {@code Pop3D} renders a three-dimensional population visualization for the
 * EvoLudo simulation. It orchestrates {@link PopGraph3D} subviews that expose
 * the state of a Module (species) in individual-based spatial (IBS) or PDE
 * modes using WebGL backed scene graphs.
 *
 * <h3>Responsibilities</h3>
 * <ul>
 * <li>Allocate and position {@code PopGraph3D} instances based on the model
 * type (IBS vs PDE), arranging interaction and competition geometries as
 * separate views when required.</li>
 * <li>Configure geometric layout metadata so each graph reflects the underlying
 * network topology (lattice, random graph, etc.). Layouts for non-lattice
 * geometries rely on spring-mass placement provided by
 * {@link Network3DGWT}.</li>
 * <li>Construct {@link ColorMap3D} instances that map model state (traits or
 * fitness) to materials, accounting for continuous, discrete and PDE-specific
 * trait representations.</li>
 * <li>Reset and refresh graphs when model settings change, ensuring each graph
 * maintains an isolated color map to avoid WebGL resource reuse issues.</li>
 * <li>Augment the view's context menu with projection controls—orthographic,
 * anaglyph and VR—so users can toggle alternate render modes at runtime.</li>
 * </ul>
 *
 * <h3>Behavior notes</h3>
 * <ul>
 * <li>IBS models may render two graphs per species (interaction and
 * competition). The class arranges them in a grid and synchronizes activation
 * and projection state across graphs.</li>
 * <li>PDE models currently produce a single full-size graph with debugging
 * disabled, reflecting the aggregated field dynamics of the module.</li>
 * <li>Color mapping:
 * <ul>
 * <li>{@code TRAIT}: continuous models select hue or gradient schemes based on
 * trait count; discrete and PDE displays mix indexed palettes and dependent
 * gradients.</li>
 * <li>{@code FITNESS}: values are rendered with a 1D gradient, highlighting
 * monomorphic fitness scores (pale colors for discrete traits, blue accents for
 * continuous traits).</li>
 * </ul>
 * </li>
 * <li>Projection menu entries are mutually aware: orthographic projection
 * disables anaglyph/VR toggles, while anaglyph and VR remain available only
 * when a single graph is visible.</li>
 * </ul>
 *
 * <h3>Usage and integration</h3>
 * <ul>
 * <li>Pop3D is constructed with an {@link EvoLudoGWT} engine and a {@link Data}
 * type describing the quantity to visualize.</li>
 * <li>The class delegates per-module visualization to {@code PopGraph3D} and
 * relies on {@link Module}, {@link AbstractGeometry} and {@link ColorMap}
 * helpers to retrieve state and styling information.</li>
 * <li>Callers should drive reset/update lifecycles from the UI thread that owns
 * the GWT/WebGL canvas; Pop3D assumes single-threaded interactions.</li>
 * </ul>
 *
 * <h3>Implementation details</h3>
 * <ul>
 * <li>Extends {@code GenericPop<MeshLambertMaterial, Network3DGWT, PopGraph3D>}
 * and honours that contract for activation, deactivation and exporting.</li>
 * <li>Graph geometry is applied through {@code setGraphGeometry(...)} and
 * mirrors the 2D view's logic for deciding whether interaction and competition
 * neighbourhoods are distinct.</li>
 * <li>Color map construction uses the {@code ColorMap3D} helpers and adapts to
 * the engine's {@link ColorModelType}. Each graph keeps its own color map to
 * avoid WebGL material sharing.</li>
 * <li>Export support is intentionally limited to PNG stills because 3D canvas
 * capture does not yet deliver vector output.</li>
 * </ul>
 *
 * <h3>Limitations and assumptions</h3>
 * <ul>
 * <li>PDE rendering is restricted to single-species modules.</li>
 * <li>VR and anaglyph projections are only available when a single graph is
 * displayed.</li>
 * <li>Network layouts and spring dynamics are provided by {@code Network3DGWT}
 * and assume compatible WebGL support in the host browser.</li>
 * </ul>
 *
 * @author Christoph Hauert
 * 
 * @see PopGraph3D
 * @see GenericPop
 * @see AbstractGeometry
 * @see ColorMap
 * @see EvoLudoGWT
 * @see Network3DGWT
 */
@SuppressWarnings("java:S110")
public class Pop3D extends GenericPop<MeshLambertMaterial, Network3DGWT, PopGraph3D> {

	/**
	 * Construct a new view to display the configuration of the current state of the
	 * EvoLudo model in 3D.
	 * 
	 * @param engine the pacemaker for running the model
	 * @param type   the type of data to display
	 */
	public Pop3D(EvoLudoGWT engine, Data type) {
		super(engine, type);
	}

	@Override
	protected boolean allocateGraphs() {
		ModelType mt = getModelType();
		if (mt.isIBS())
			return allocateIBSGraphs();

		if (mt.isPDE())
			return allocatePDEGraph();

		return false;
	}

	/**
	 * Allocate graphs for individual-based spatial models. This method creates
	 * separate graphs for interaction and competition geometries.
	 * 
	 * @return {@code true} if graphs were reallocated
	 */
	private boolean allocateIBSGraphs() {
		// how to deal with distinct interaction/competition geometries?
		// - currently two separate graphs are shown one for the interaction and the
		// other for the competition geometry
		// - alternatively links could be drawn in different colors (would need to
		// revise network layout routines)
		// - another alternative is to add context menu to toggle between the different
		// link sets (could be difficult if one is a lattice...)
		int nGraphs = 0;
		List<? extends Module<?>> species = engine.getModule().getSpecies();
		for (Module<?> mod : species)
			nGraphs += mod.getIBSPopulation().getInteractionGeometry().isSingle() ? 1 : 2;

		if (graphs.size() == nGraphs)
			return false;

		destroyGraphs();
		for (Module<?> mod : species) {
			PopGraph3D graph = new PopGraph3D(this, mod);
			wrapper.add(graph);
			graphs.add(graph);
			if (!mod.getIBSPopulation().getInteractionGeometry().isSingle()) {
				graph = new PopGraph3D(this, mod);
				wrapper.add(graph);
				graphs.add(graph);
				// arrange graphs horizontally
				gCols = 2;
			}
		}
		gRows = species.size();
		if (gRows * gCols == 2) {
			// always arrange horizontally if only two graphs
			gRows = 1;
			gCols = 2;
		}
		int width = 100 / gCols;
		int height = 100 / gRows;
		boolean inter = true;
		for (PopGraph3D graph : graphs) {
			graph.setSize(width + "%", height + "%");
			setGraphGeometry(graph, inter);
			inter = !inter;
		}
		return true;
	}

	/**
	 * Allocate graph for PDE models. Currently restricted to single species.
	 * 
	 * @return {@code true} if graphs were reallocated
	 */
	private boolean allocatePDEGraph() {
		// PDEs currently restricted to single species
		if (graphs.size() == 1)
			return false;

		destroyGraphs();
		Module<?> module = engine.getModule();
		PopGraph3D graph = new PopGraph3D(this, module);
		// debugging not available for DE's
		graph.setDebugEnabled(false);
		wrapper.add(graph);
		graphs.add(graph);
		graph.setSize("100%", "100%");
		setGraphGeometry(graph, true);
		return true;
	}

	@Override
	public void modelSettings() {
		super.modelSettings();
		reset(false);
	}

	@Override
	public void reset(boolean hard) {
		super.reset(hard);
		// IMPORTANT: to avoid problems with WebGL and 3D rendering, each graph needs to
		// have its own color map
		boolean inter = true;
		for (PopGraph3D graph : graphs) {
			// update geometries associated with the graphs
			setGraphGeometry(graph, inter);
			inter = !inter;

			Module<?> module = graph.getModule();
			ColorMap<MeshLambertMaterial> cMap = createColorMap(module);

			graph.setColorMap(module.processColorMap(cMap));
			graph.reset();
		}
		update(hard);
	}

	/**
	 * Create the ColorMap for a given module/graph based on the current data type
	 * and model settings.
	 * 
	 * @param module module whose data will be visualized
	 * @return configured color map
	 */
	private ColorMap<MeshLambertMaterial> createColorMap(Module<?> module) {
		switch (type) {
			case TRAIT:
				if (model.isContinuous())
					return createTraitColorMapContinuous(module);
				return createTraitColorMapDiscreteOrPDE(module);
			case FITNESS:
				return createFitnessColorMap(module);
			default:
				throw new UnsupportedOperationException("Data type '" + type + "' not yet implemented");
		}
	}

	/**
	 * Helper for continuous trait color maps.
	 * 
	 * @param module module providing trait metadata
	 * @return configured color map
	 */
	private ColorMap<MeshLambertMaterial> createTraitColorMapContinuous(Module<?> module) {
		ColorModelType cmt = ColorModelType.DEFAULT;
		if (module instanceof Continuous)
			cmt = ((Continuous) module).getColorModelType();
		int nTraits = module.getNTraits();
		if (cmt == ColorModelType.DISTANCE) {
			return new ColorMap3D.Gradient1D(new Color[] { Color.BLACK, Color.GRAY, Color.YELLOW, Color.RED }, 500);
		}
		switch (nTraits) {
			case 1:
				// set hue range: min = red, max = blue
				return new ColorMap3D.Hue(0.0, 2.0 / 3.0, 500);
			case 2:
				return new ColorMap3D.Gradient2D(module.getTraitColors(), 50);
			default:
				Color[] primaries = new Color[nTraits];
				System.arraycopy(module.getTraitColors(), 0, primaries, 0, nTraits);
				// prefer ND gradient but force distance coloring if not supported
				if (cmt == ColorModelType.DISTANCE)
					return new ColorMap3D.Gradient1D(
							new Color[] { Color.BLACK, Color.GRAY, Color.YELLOW, Color.RED }, 500);
				// warn and switch to distance coloring (only once is ideal, handled by
				// engine/logger upstream)
				logger.warning("display of >2 continuous traits not (yet) implemented - coloring trait distance");
				if (module instanceof Continuous)
					((Continuous) module).setColorModelType(ColorModelType.DISTANCE);
				return new ColorMap3D.GradientND(primaries);
		}
	}

	/**
	 * Helper for discrete and PDE trait color maps.
	 * 
	 * @param module module providing trait metadata
	 * @return configured color map
	 */
	private ColorMap<MeshLambertMaterial> createTraitColorMapDiscreteOrPDE(Module<?> module) {
		if (getModelType().isPDE()) {
			int nTraits = module.getNTraits();
			Color[] colors = module.getTraitColors();
			int dep = ((HasDE) module).getDependent();
			if (nTraits == 2 && dep >= 0)
				return new ColorMap3D.Gradient1D(colors[dep], colors[(dep + 1) % nTraits], 100);
			// vacant space does not count as dependent trait for coloring
			if (module.getVacantIdx() == dep)
				dep = -1;
			return new ColorMap3D.Gradient2D(colors, dep, 100);
		} else {
			return new ColorMap3D.Index(module.getTraitColors(), (int) (0.75 * 255));
		}
	}

	/**
	 * Helper to create color map for fitness data. Marks monomorphic scores for
	 * IBS.
	 * 
	 * @param module module providing trait metadata
	 * @return configured color map
	 */
	private ColorMap<MeshLambertMaterial> createFitnessColorMap(Module<?> module) {
		ColorMap.Gradient1D<MeshLambertMaterial> cMap1D = new ColorMap3D.Gradient1D(
				new Color[] { ColorMap.addAlpha(Color.BLACK, 220), ColorMap.addAlpha(Color.GRAY, 220),
						ColorMap.addAlpha(Color.YELLOW, 220), ColorMap.addAlpha(Color.RED, 220) },
				500);
		int id = module.getId();
		cMap1D.setRange(model.getMinFitness(id), model.getMaxFitness(id));

		if (!getModelType().isIBS())
			return cMap1D;

		Map2Fitness map2fit = module.getMap2Fitness();
		if (module instanceof Discrete) {
			// mark homogeneous fitness values by pale color
			Color[] pure = module.getTraitColors();
			int nMono = module.getNTraits();
			// cast is safe because module is Discrete
			DModel dmodel = (DModel) model;
			for (int n = 0; n < nMono; n++)
				cMap1D.setColor(map2fit.map(dmodel.getMonoScore(id, n)),
						new Color(Math.max(pure[n].getRed(), 127), Math.max(pure[n].getGreen(), 127),
								Math.max(pure[n].getBlue(), 127), 220));
			return cMap1D;
		}

		if (module instanceof Continuous) {
			// cast is safe because module is Continuous
			CModel cmodel = (CModel) model;
			// hardcoded colors for min/max mono scores
			cMap1D.setColor(map2fit.map(cmodel.getMinMonoScore(id)), ColorMap.addAlpha(Color.BLUE.darker(), 220));
			cMap1D.setColor(map2fit.map(cmodel.getMaxMonoScore(id)), ColorMap.addAlpha(Color.BLUE.brighter(), 220));
			return cMap1D;
		}

		// unknown type of population - no fitness values marked
		return cMap1D;
	}

	@Override
	public void modelDidInit() {
		super.modelDidInit();
		update();
	}

	/**
	 * The context menu item for selecting parallel projection of the graph instead
	 * of the default perspective projection.
	 */
	private ContextMenuCheckBoxItem projectionMenu;

	/**
	 * The context menu item for selecting anaglyph projection of the 3D space for a
	 * reperesentation of the graph suitable for colored 3D glasses.
	 */
	private ContextMenuCheckBoxItem anaglyphMenu;

	/**
	 * The context menu item for selecting stereo projection of the 3D space for a
	 * virtual reality representation of the graph.
	 */
	private ContextMenuCheckBoxItem vrMenu;

	@Override
	public void populateContextMenu(ContextMenu menu) {
		menu.addSeparator();
		boolean multiGraph = graphs.size() > 1;
		PopGraph3D graph = graphs.get(0);
		boolean isOrthographic = graph.isOrthographic();
		boolean isAnaglyph = graph.isAnaglyph();
		boolean isVR = graph.isVR();
		// projections synchronized across graphs.
		addProjectionMenu(menu);
		projectionMenu.setChecked(isOrthographic);
		projectionMenu.setEnabled(!(isAnaglyph || isVR));
		// anaglyph and VR modes are mutually exclusive
		// only available for a single graph
		if (!multiGraph) {
			addAnaglyphMenu(menu);
			addVRMenu(menu);
			anaglyphMenu.setChecked(isAnaglyph);
			anaglyphMenu.setEnabled(!isOrthographic);
			vrMenu.setChecked(isVR);
			vrMenu.setEnabled(!isOrthographic);
		}
		super.populateContextMenu(menu);
	}

	/**
	 * Add the menu item to select parallel projection of the graph instead of the
	 * default perspective projection.
	 * 
	 * @param menu the context menu to which the item is added
	 */
	private void addProjectionMenu(ContextMenu menu) {
		if (projectionMenu == null) {
			projectionMenu = new ContextMenuCheckBoxItem("Parallel projection", () -> {
				boolean isOrtho = !projectionMenu.isChecked();
				for (PopGraph3D graph : graphs)
					graph.setOrthographic(isOrtho);
				projectionMenu.setChecked(isOrtho);
			});
		}
		menu.add(projectionMenu);
	}

	/**
	 * Add the menu item to select anaglyph projection of the 3D space for a
	 * representation of the graph suitable for colored 3D glasses.
	 * 
	 * @param menu the context menu to which the item is added
	 */
	private void addAnaglyphMenu(ContextMenu menu) {
		if (anaglyphMenu == null) {
			anaglyphMenu = new ContextMenuCheckBoxItem("Anaglyph 3D", () -> {
				boolean anaglyph = !anaglyphMenu.isChecked();
				for (PopGraph3D graph : graphs)
					graph.setAnaglyph(anaglyph);
				anaglyphMenu.setChecked(anaglyph);
			});
		}
		menu.add(anaglyphMenu);
	}

	/**
	 * Add the menu item to select stereo projection of the 3D space for a virtual
	 * reality representation of the graph.
	 * 
	 * @param menu the context menu to which the item is added
	 */
	private void addVRMenu(ContextMenu menu) {
		if (graphs.size() == 1) {
			if (vrMenu == null) {
				vrMenu = new ContextMenuCheckBoxItem("Virtual reality (β)", () -> {
					boolean vr = !vrMenu.isChecked();
					for (PopGraph3D graph : graphs)
						graph.setVR(vr);
					vrMenu.setChecked(vr);
				});
			}
			menu.add(vrMenu);
		}
	}

	@Override
	protected ExportType[] exportTypes() {
		return new ExportType[] { ExportType.PNG };
	}
}
