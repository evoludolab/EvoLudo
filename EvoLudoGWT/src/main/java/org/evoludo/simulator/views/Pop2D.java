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

import java.awt.Color;
import java.util.List;

import org.evoludo.graphics.GraphStyle;
import org.evoludo.graphics.PopGraph2D;
import org.evoludo.simulator.ColorMap;
import org.evoludo.simulator.ColorMapCSS;
import org.evoludo.simulator.EvoLudo.ColorModelType;
import org.evoludo.simulator.EvoLudoGWT;
import org.evoludo.simulator.Geometry;
import org.evoludo.simulator.Network2D;
import org.evoludo.simulator.geom.GeometryType;
import org.evoludo.simulator.models.CModel;
import org.evoludo.simulator.models.DModel;
import org.evoludo.simulator.models.Data;
import org.evoludo.simulator.models.Model.HasDE;
import org.evoludo.simulator.models.ModelType;
import org.evoludo.simulator.modules.Continuous;
import org.evoludo.simulator.modules.Discrete;
import org.evoludo.simulator.modules.Map2Fitness;
import org.evoludo.simulator.modules.Module;

/**
 * 2D population view for the EvoLudo model.
 * <p>
 * {@code Pop2D} renders a two-dimensional population visualization for
 * the
 * EvoLudo simulation. Pop2D is responsible for creating, configuring and
 * maintaining one or more PopGraph2D subviews that display the current state of
 * a Module (species) in either individual‑based spatial (IBS) or PDE modes.
 *
 * <h3>Responsibilities</h3>
 * <ul>
 * <li>Allocate and arrange one or more PopGraph2D instances according to the
 * model type (IBS vs PDE) and the number of species.</li>
 * <li>Configure graph presentation (axes, ticks, labels, size, border) based
 * on the geometry of the underlying module and the currently selected
 * data type ({@code TRAIT}, {@code FITNESS}).</li>
 * <li>Construct ColorMap instances used by graphs to map model state values
 * to colors. Supports continuous trait gradients (1D, 2D, ND), discrete
 * indices, PDE-specific mappings and fitness highlighting for IBS.</li>
 * <li>Handle resets and model setting changes by rebuilding/refreshing graphs
 * as needed and deciding when a hard reset (reinitializing reporting or
 * time axes) is required.</li>
 * <li>Provide export capabilities for graph content (SVG, PNG).</li>
 * </ul>
 *
 * <h3>Behavior notes</h3>
 * <ul>
 * <li>For IBS (individual-based spatial) models, {@code Pop2D} may create
 * separate
 * graphs for interaction and competition geometries. Graphs are arranged
 * in a grid whose rows correspond to species and whose columns reflect the
 * number of graphs per species (1 or 2).</li>
 * <li>For PDE models {@code Pop2D} currently assumes a single (possibly
 * multi-trait)
 * module and creates a single full-size graph. Debugging features for
 * DE-based modules are disabled in this view.</li>
 * <li>When graph geometry is linear, the graph is configured as an x-vs-time
 * scrolling display (nodes on the x axis, time on the y-axis) and changes
 * to the model's reporting frequency will force a hard reset to reinitialize
 * time axis scaling.</li>
 * <li>Color mapping:
 * <ul>
 * <li>{@code TRAIT}: continuous models use gradient/hue mappings; discrete and
 * PDE trait displays use indexed or 2D gradient schemes appropriate
 * to the number of traits and any dependent variable for PDEs.</li>
 * <li>{@code FITNESS}: values are rendered using a 1D gradient; for IBS models
 * monomorphic (homogeneous) trait fitness scores may be highlighted
 * with pale or special colors to indicate mono‑score locations.</li>
 * </ul>
 * </li>
 * </ul>
 *
 * <h3>Usage and integration</h3>
 * <ul>
 * <li>Pop2D is constructed with an {@code EvoLudoGWT} engine (the simulation
 * controller)
 * and a Data enumeration indicating which quantity to display.</li>
 * <li>The class delegates per-module visualization logic to PopGraph2D and
 * relies on Module, {@code Geometry} and {@code ColorMap} helper classes to
 * query model
 * state and construct visual representations.</li>
 * <li>Reset and update cycles should be invoked from the UI/event thread that
 * drives the GWT view lifecycle; {@code Pop2D} is not safe for concurrent
 * updates
 * from arbitrary threads.</li>
 * </ul>
 *
 * <h3>Implementation details</h3>
 * <ul>
 * <li>Extends {@code GenericPop<String, Network2D, PopGraph2D>} and follows
 * that
 * contract for activation, deactivation and lifecycle management.</li>
 * <li>Graph style is encapsulated in {@code GraphStyle} objects and adjusted by
 * {@code configureGraph(...)}. Changes to reporting frequency (time step)
 * result
 * in a request for a hard reset so that time axes and sampling align with
 * the new frequency.</li>
 * <li>ColorMap construction uses a small palette of CSS-based gradient and
 * index implementations (e.g. {@code ColorMapCSS} classes) and adapts to the
 * engine's chosen {@code ColorModelType}.</li>
 * </ul>
 *
 * <h3>Limitations and assumptions</h3>
 * <ul>
 * <li>PDE mode is currently restricted to single-species.</li>
 * <li>Assumes modules provide geometry information via {@code Geometry}
 * instances and that {@code Module} implementations expose trait counts,
 * colors,
 * and PDE-dependent indices where applicable.</li>
 * </ul>
 *
 * @author Christoph Hauert
 * 
 * @see PopGraph2D
 * @see GenericPop
 * @see Geometry
 * @see ColorMap
 * @see EvoLudoGWT
 */
@SuppressWarnings("java:S110")
public class Pop2D extends GenericPop<String, Network2D, PopGraph2D> {

	/**
	 * Construct a new view to display the configuration of the current state of the
	 * EvoLudo model in 2D.
	 * 
	 * @param engine the pacemaker for running the model
	 * @param type   the type of data to display
	 */
	public Pop2D(EvoLudoGWT engine, Data type) {
		super(engine, type);
	}

	@Override
	protected boolean allocateGraphs() {
		ModelType mt = model.getType();
		if (mt.isIBS())
			return allocateIBSGraphs();

		if (mt.isPDE())
			return allocatePDEGraph();
		return false;
	}

	/**
	 * Allocate graphs for individual‑based spatial models. This method creates
	 * separate graphs for interaction and competition geometries.
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
			nGraphs += mod.getInteractionGeometry().isSingle() ? 1 : 2;

		if (graphs.size() == nGraphs)
			return false;
		destroyGraphs();
		for (Module<?> mod : species) {
			PopGraph2D graph = new PopGraph2D(this, mod);
			wrapper.add(graph);
			graphs.add(graph);
			if (!mod.getInteractionGeometry().isSingle()) {
				graph = new PopGraph2D(this, mod);
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
		for (PopGraph2D graph : graphs) {
			graph.setSize(width + "%", height + "%");
			setGraphGeometry(graph, inter);
			inter = !inter;
		}
		return true;
	}

	/**
	 * Allocate graph for PDE models. Currently restricted to single species.
	 */
	private boolean allocatePDEGraph() {
		// PDEs currently restricted to single species
		if (graphs.size() == 1)
			return false;

		destroyGraphs();
		Module<?> module = engine.getModule();
		PopGraph2D graph = new PopGraph2D(this, module);
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
		boolean inter = true;
		for (PopGraph2D graph : graphs) {
			// update geometries associated with the graphs
			setGraphGeometry(graph, inter);
			inter = !inter;

			// configure graphical style and detect if this requires a hard reset
			boolean changed = configureGraph(graph);
			hard = hard || changed;

			// build and assign color map
			Module<?> module = graph.getModule();
			ColorMap<String> cMap = createColorMap(module);
			graph.setColorMap(module.processColorMap(cMap));

			if (hard)
				graph.reset();
		}
		update(hard);
	}

	/**
	 * Configure the graph's style according to its geometry and return true if a
	 * change requires a hard reset (i.e. change of reporting frequency).
	 */
	private boolean configureGraph(PopGraph2D graph) {
		Geometry geometry = graph.getGeometry();
		GraphStyle style = graph.getStyle();

		if (geometry.isType(GeometryType.LINEAR)) {
			// frame, ticks, labels needed
			style.xLabel = "nodes";
			style.showXLabel = true;
			style.showXTickLabels = true;
			style.xMin = 0;
			style.xMax = geometry.getSize();
			style.yLabel = "time";
			double rFreq = model.getTimeStep();
			// if report frequency did not change, we're done
			if (Math.abs(-style.yIncr - rFreq) > 1e-8) {
				style.yIncr = -rFreq;
				// require hard reset when report frequency changed
				style.yMax = 0.0;
				style.showYLabel = true;
				style.showYTickLabels = true;
				style.showXTicks = true;
				style.showYTicks = true;
				style.showYLevels = true;
				style.percentY = false;
				style.showXLevels = false;
				return true;
			}
			style.yMax = 0.0;
			style.showYLabel = true;
			style.showYTickLabels = true;
			style.showXTicks = true;
			style.showYTicks = true;
			style.showYLevels = true;
		} else {
			// border is all we want
			style.showXLabel = false;
			style.showYLabel = false;
			style.showYTickLabels = false;
			style.showXTickLabels = false;
			style.showXTicks = false;
			style.showYTicks = false;
		}
		// style.label = geometry.name;
		// style.showLabel = !style.label.isEmpty();
		style.percentY = false;
		style.showXLevels = false;

		return false;
	}

	/**
	 * Create the ColorMap for a given module/graph based on the current data type
	 * and model settings.
	 */
	private ColorMap<String> createColorMap(Module<?> module) {
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
	 */
	private ColorMap<String> createTraitColorMapContinuous(Module<?> module) {
		ColorModelType cmt = engine.getColorModelType();
		int nTraits = module.getNTraits();
		if (cmt == ColorModelType.DISTANCE) {
			return new ColorMapCSS.Gradient1D(
					new Color[] { Color.BLACK, Color.GRAY, Color.YELLOW, Color.RED }, 500);
		}
		switch (nTraits) {
			case 1:
				// set hue range: min = red, max = blue
				return new ColorMapCSS.Hue(0.0, 2.0 / 3.0, 500);
			case 2:
				Color[] traitcolors = module.getTraitColors();
				return new ColorMapCSS.Gradient2D(traitcolors, 50);
			default:
				Color[] primaries = new Color[nTraits];
				System.arraycopy(module.getTraitColors(), 0, primaries, 0, nTraits);
				return new ColorMapCSS.GradientND(primaries);
		}
	}

	/**
	 * Helper for discrete and PDE trait color maps.
	 */
	private ColorMap<String> createTraitColorMapDiscreteOrPDE(Module<?> module) {
		if (model.getType().isPDE()) {
			int nTraits = module.getNTraits();
			Color[] colors = module.getTraitColors();
			int dep = ((HasDE) module).getDependent();
			if (nTraits == 2 && dep >= 0) {
				int trait = (dep + 1) % nTraits;
				return new ColorMapCSS.Gradient1D(colors[dep], colors[trait], trait, 100);
			} else {
				// vacant space does not count as dependent trait for coloring
				if (module.getVacantIdx() == dep)
					dep = -1;
				return new ColorMapCSS.Gradient2D(colors, dep, 100);
			}
		} else {
			return new ColorMapCSS.Index(module.getTraitColors(), 220);
		}
	}

	/**
	 * Helper to create color map for FITNESS data. Marks monomorphic scores for
	 * IBS.
	 */
	private ColorMap<String> createFitnessColorMap(Module<?> module) {
		ColorMap.Gradient1D<String> cMap1D = new ColorMapCSS.Gradient1D(
				new Color[] { ColorMap.addAlpha(Color.BLACK, 220), ColorMap.addAlpha(Color.GRAY, 220),
						ColorMap.addAlpha(Color.YELLOW, 220), ColorMap.addAlpha(Color.RED, 220) },
				500);
		ColorMap<String> cMap = cMap1D;
		int tag = module.getID();
		cMap1D.setRange(model.getMinFitness(tag), model.getMaxFitness(tag));

		if (!model.getType().isIBS())
			return cMap;

		Map2Fitness map2fit = module.getMap2Fitness();
		int id = module.getID();

		if (module instanceof Discrete) {
			// mark homogeneous fitness values by pale color
			Color[] pure = module.getTraitColors();
			int nMono = module.getNTraits();
			DModel dmodel = (DModel) model; // safe cast for Discrete population
			for (int n = 0; n < nMono; n++) {
				double mono = dmodel.getMonoScore(id, n);
				if (Double.isNaN(mono))
					continue;
				cMap1D.setColor(map2fit.map(mono),
						new Color(Math.max(pure[n].getRed(), 127), Math.max(pure[n].getGreen(), 127),
								Math.max(pure[n].getBlue(), 127), 220));
			}
			return cMap;
		}

		if (module instanceof Continuous) {
			// cast is safe because pop is Continuous
			CModel cmodel = (CModel) model;
			// hardcoded colors for min/max mono scores
			cMap1D.setColor(map2fit.map(cmodel.getMinMonoScore(id)), ColorMap.addAlpha(Color.BLUE.darker(), 220));
			cMap1D.setColor(map2fit.map(cmodel.getMaxMonoScore(id)), ColorMap.addAlpha(Color.BLUE.brighter(), 220));
			return cMap;
		}

		// unknown type of population - no fitness values marked
		return cMap;
	}

	@Override
	protected ExportType[] exportTypes() {
		return new ExportType[] { ExportType.SVG, ExportType.PNG };
	}
}