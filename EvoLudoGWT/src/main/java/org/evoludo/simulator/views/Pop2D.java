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
import java.util.ArrayList;

import org.evoludo.graphics.PopGraph2D;
import org.evoludo.simulator.ColorMap;
import org.evoludo.simulator.ColorMapCSS;
import org.evoludo.simulator.EvoLudo.ColorModelType;
import org.evoludo.simulator.EvoLudoGWT;
import org.evoludo.simulator.Geometry;
import org.evoludo.simulator.Network2D;
import org.evoludo.simulator.models.Data;
import org.evoludo.simulator.models.ODE.HasDE;
import org.evoludo.simulator.modules.Continuous;
import org.evoludo.simulator.modules.Discrete;
import org.evoludo.simulator.modules.Map2Fitness;
import org.evoludo.simulator.modules.Module;

/**
 * The view to display configuration of the current state of the model in 2D.
 * The visual representation depends on the geometry of the model. Lattice
 * structures have a fixed layout but all other strutures are dynamically
 * generated through a process insipired by the physical arrangement of charged
 * spheres that are connected by springs. The spheres represent members of the
 * population and the springs represent their interaction (or competition)
 * neighbourhood. The size of the sphere scales with the size of the
 * individual's neighbourhood. Moreover, the colour of the spheres reflects the
 * state of the individual, for example their trait or fitness.
 *
 * @author Christoph Hauert
 */
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
	protected void allocateGraphs() {
		// how to deal with distinct interaction/competition geometries?
		// - currently two separate graphs are shown one for the interaction and the
		// other for the competition geometry
		// - alternatively links could be drawn in different colors (would need to
		// revise network layout routines)
		// - another alternative is to add context menu to toggle between the different
		// link sets (could be difficult if one is a lattice...)
		if (model.isIBS()) {
			int nGraphs = 0;
			ArrayList<? extends Module> species = engine.getModule().getSpecies();
			for (Module module : species)
				nGraphs += Geometry.displayUniqueGeometry(module) ? 1 : 2;

			if (graphs.size() == nGraphs)
				return;
			destroyGraphs();
			for (Module module : species) {
				PopGraph2D graph = new PopGraph2D(this, module);
				wrapper.add(graph);
				graphs.add(graph);
				if (!Geometry.displayUniqueGeometry(module)) {
					graph = new PopGraph2D(this, module);
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
				if (isActive)
					graph.activate();
			}
			return;
		}
		if (model.isPDE()) {
			// PDEs currently restricted to single species
			if (graphs.size() == 1)
				return;

			destroyGraphs();
			Module module = engine.getModule();
			PopGraph2D graph = new PopGraph2D(this, module);
			// debugging not available for DE's
			graph.setDebugEnabled(false);
			wrapper.add(graph);
			graphs.add(graph);
			graph.setSize("100%", "100%");
			setGraphGeometry(graph, true);
			if (isActive)
				graph.activate();
		}
	}

	@Override
	public void clear() {
		super.clear();
		for (PopGraph2D graph : graphs)
			graph.clearGraph();
	}

	@Override
	public void reset(boolean hard) {
		super.reset(hard);
		boolean inter = true;
		for (PopGraph2D graph : graphs) {
			// update geometries associated with the graphs
			setGraphGeometry(graph, inter);
			inter = !inter;
			Geometry geometry = graph.getGeometry();
			Module module = graph.getModule();
			PopGraph2D.GraphStyle style = graph.getStyle();
			if (geometry.getType() == Geometry.Type.LINEAR) {
				// frame, ticks, labels needed
				style.xLabel = "nodes";
				style.showXLabel = true;
				style.showXTickLabels = true;
				style.xMin = 0;
				style.xMax = geometry.size;
				style.yLabel = "time";
				double rFreq = model.getTimeStep();
				// if report frequency did not change, we're done
				if (Math.abs(-style.yIncr - rFreq) > 1e-8) {
					style.yIncr = -rFreq;
					hard = true;
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

			ColorMap<String> cMap = null;
			switch (type) {
				case TRAIT:
					if (model.isContinuous()) {
						ColorModelType cmt = engine.getColorModelType();
						int nTraits = module.getNTraits();
						if (cmt == ColorModelType.DISTANCE) {
							cMap = new ColorMapCSS.Gradient1D(
									new Color[] { Color.BLACK, Color.GRAY, Color.YELLOW, Color.RED }, 500);
							break;
						}
						switch (nTraits) {
							case 1:
								// set hue range: min = red, max = blue
								cMap = new ColorMapCSS.Hue(0.0, 2.0 / 3.0, 500);
								break;
							case 2:
								Color[] traitcolors = module.getTraitColors();
								cMap = new ColorMapCSS.Gradient2D(traitcolors[0], traitcolors[1], Color.BLACK, 50);
								break;
							default:
								Color[] primaries = new Color[nTraits];
								System.arraycopy(module.getTraitColors(), 0, primaries, 0, nTraits);
								cMap = new ColorMapCSS.GradientND(primaries);
								break;
						}
					} else {
						if (model.isPDE()) {
							int nTraits = module.getNTraits();
							Color[] colors = module.getTraitColors();
							int dep = ((HasDE) module).getDependent();
							if (nTraits == 2 && dep >= 0) {
								int trait = (dep + 1) % nTraits;
								cMap = new ColorMapCSS.Gradient1D(colors[dep], colors[trait], trait, 100);
							} else
								cMap = new ColorMapCSS.Gradient2D(colors, dep, 100);
						} else
							cMap = new ColorMapCSS.Index(module.getTraitColors(), 220);
					}
					break;
				case FITNESS:
					ColorMap.Gradient1D<String> cMap1D = new ColorMapCSS.Gradient1D(
							new Color[] { ColorMap.addAlpha(Color.BLACK, 220), ColorMap.addAlpha(Color.GRAY, 220),
									ColorMap.addAlpha(Color.YELLOW, 220), ColorMap.addAlpha(Color.RED, 220) },
							500);
					cMap = cMap1D;
					// cMap1D.setRange(pop.getMinFitness(), pop.getMaxFitness());
					int tag = graph.getModule().getID();
					cMap1D.setRange(model.getMinScore(tag), model.getMaxScore(tag));
					if (model.isIBS()) {
						Map2Fitness map2fit = module.getMapToFitness();
						int id = module.getID();
						if (module instanceof Discrete) {
							// mark homogeneous fitness values by pale color
							Color[] pure = module.getTraitColors();
							int nMono = module.getNTraits();
							for (int n = 0; n < nMono; n++) {
								// cast is save because pop is Discrete
								org.evoludo.simulator.models.Discrete dmodel = (org.evoludo.simulator.models.Discrete) model;
								double mono = dmodel.getMonoScore(id, n);
								if (Double.isNaN(mono))
									continue;
								cMap1D.setColor(map2fit.map(mono),
										new Color(Math.max(pure[n].getRed(), 127),
												Math.max(pure[n].getGreen(), 127),
												Math.max(pure[n].getBlue(), 127), 220));
							}
							break;
						}
						if (module instanceof Continuous) {
							// cast is save because pop is Continuous
							org.evoludo.simulator.models.Continuous cmodel = (org.evoludo.simulator.models.Continuous) model;
							// hardcoded colors for min/max mono scores
							cMap1D.setColor(map2fit.map(cmodel.getMinMonoScore(id)),
									ColorMap.addAlpha(Color.BLUE.darker(), 220));
							cMap1D.setColor(map2fit.map(cmodel.getMaxMonoScore(id)),
									ColorMap.addAlpha(Color.BLUE.brighter(), 220));
							break;
						}
						// unknown type of population - no fitness values marked
					}
					break;
				default:
					break;
			}
			if (cMap == null)
				throw new Error("MVPop2D: ColorMap not initialized - needs attention!");
			graph.setColorMap(module.processColorMap(cMap));
			if (hard)
				graph.reset();
		}
		update(hard);
	}

	@Override
	protected ExportType[] exportTypes() {
		return new ExportType[] { ExportType.SVG, ExportType.PNG };
	}
}
