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
import org.evoludo.simulator.EvoLudo.ColorModelType;
import org.evoludo.simulator.EvoLudoGWT;
import org.evoludo.simulator.Geometry;
import org.evoludo.simulator.models.Data;
import org.evoludo.simulator.models.Model.HasDE;
import org.evoludo.simulator.models.Type;
import org.evoludo.simulator.modules.Map2Fitness;
import org.evoludo.simulator.modules.Module;
import org.evoludo.ui.ContextMenu;
import org.evoludo.ui.ContextMenuCheckBoxItem;

import thothbot.parallax.core.shared.materials.MeshLambertMaterial;

/**
 * The view to display configuration of the current state of the model in 3D.
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
	protected void allocateGraphs() {
		Type mt = model.getType();
		if (mt.isIBS()) {
			// how to deal with distinct interaction/competition geometries?
			// - currently two separate graphs are shown one for the interaction and the
			// other for the competition geometry
			// - alternatively links could be drawn in different colors (would need to
			// revise network layout routines)
			// - another alternative is to add context menu to toggle between the different
			// link sets (could be difficult if one is a lattice...)
			int nGraphs = 0;
			List<? extends Module> species = engine.getModule().getSpecies();
			for (Module module : species)
				nGraphs += Geometry.displayUniqueGeometry(module) ? 1 : 2;

			if (graphs.size() == nGraphs)
				return;

			destroyGraphs();
			for (Module module : species) {
				PopGraph3D graph = new PopGraph3D(this, module);
				wrapper.add(graph);
				graphs.add(graph);
				if (!Geometry.displayUniqueGeometry(module)) {
					graph = new PopGraph3D(this, module);
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
				if (isActive)
					graph.activate();
			}
			return;
		}
		if (mt.isPDE()) {
			// PDEs currently restricted to single species
			if (graphs.size() == 1)
				return;

			destroyGraphs();
			Module module = engine.getModule();
			PopGraph3D graph = new PopGraph3D(this, module);
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
	public void reset(boolean hard) {
		super.reset(hard);
		// IMPORTANT: to avoid problems with WebGL and 3D rendering, each graph needs to
		// have its own color map
		org.evoludo.simulator.models.Continuous cmodel = null;
		org.evoludo.simulator.models.Discrete dmodel = null;
		if (model.isContinuous())
			cmodel = (org.evoludo.simulator.models.Continuous) model;
		else
			dmodel = (org.evoludo.simulator.models.Discrete) model;

		boolean inter = true;
		boolean noWarnings = true;
		Type mt = model.getType();
		boolean isIBS = mt.isIBS();
		boolean isPDE = mt.isPDE();
		for (PopGraph3D graph : graphs) {
			// update geometries associated with the graphs
			setGraphGeometry(graph, inter);
			inter = !inter;
			ColorMap<MeshLambertMaterial> cMap = null;
			Module module = graph.getModule();
			switch (type) {
				case TRAIT:
					if (cmodel != null) {
						ColorModelType cmt = engine.getColorModelType();
						int nTraits = module.getNTraits();
						if (cmt == ColorModelType.DISTANCE) {
							cMap = new ColorMap3D.Gradient1D(
									new Color[] { Color.BLACK, Color.GRAY, Color.YELLOW, Color.RED }, 500);
							break;
						}
						switch (nTraits) {
							case 1:
								// set hue range: min = red, max = blue
								cMap = new ColorMap3D.Hue(0.0, 2.0 / 3.0, 500);
								break;
							case 2:
								Color[] tColors = module.getTraitColors();
								cMap = new ColorMap3D.Gradient2D(tColors, 50);
								break;
							default:
								if (cmt == ColorModelType.DISTANCE) {
									cMap = new ColorMap3D.Gradient1D(
											new Color[] { Color.BLACK, Color.GRAY, Color.YELLOW, Color.RED }, 500);
									break;
								}
								Color[] primaries = new Color[nTraits];
								System.arraycopy(module.getTraitColors(), 0, primaries, 0, nTraits);
								cMap = new ColorMap3D.GradientND(primaries);
								if (cmt != ColorModelType.DISTANCE) {
									// log warning only once in case there are multiple species
									if (noWarnings) {
										noWarnings = false;
										logger.warning(
												"display of >2 continuous traits not (yet) implemented - coloring trait distance");
									}
									engine.setColorModelType(ColorModelType.DISTANCE);
								}
								break;
						}
					} else {
						if (isPDE) {
							int nTraits = module.getNTraits();
							Color[] colors = module.getTraitColors();
							int dep = ((HasDE) module).getDependent();
							if (nTraits == 2 && dep >= 0)
								cMap = new ColorMap3D.Gradient1D(colors[dep], colors[(dep + 1) % nTraits], 100);
							else {
								// vacant space does not count as dependent trait for coloring
								if (module.getVacantIdx() == dep)
									dep = -1;
								cMap = new ColorMap3D.Gradient2D(colors, dep, 100);
							}
						} else
							cMap = new ColorMap3D.Index(module.getTraitColors(), (int) (0.75 * 255));
					}
					break;
				case FITNESS:
					ColorMap.Gradient1D<MeshLambertMaterial> cMap1D = new ColorMap3D.Gradient1D(
							new Color[] { ColorMap.addAlpha(Color.BLACK, 220), ColorMap.addAlpha(Color.GRAY, 220),
									ColorMap.addAlpha(Color.YELLOW, 220), ColorMap.addAlpha(Color.RED, 220) },
							500);
					cMap = cMap1D;
					// cMap1D.setRange(module.getMinFitness(), module.getMaxFitness());
					int id = graph.getModule().getID();
					cMap1D.setRange(model.getMinScore(id), model.getMaxScore(id));
					if (isIBS) {
						Map2Fitness map2fit = module.getMap2Fitness();
						if (cmodel != null) {
							// hardcoded colors for min/max mono scores
							cMap1D.setColor(map2fit.map(cmodel.getMinMonoScore(id)),
									ColorMap.addAlpha(Color.BLUE.darker(), 220));
							cMap1D.setColor(map2fit.map(cmodel.getMaxMonoScore(id)),
									ColorMap.addAlpha(Color.BLUE.brighter(), 220));
						} else if (dmodel != null) {
							// mark homogeneous fitness values by pale color
							Color[] pure = module.getTraitColors();
							int nMono = module.getNTraits();
							for (int n = 0; n < nMono; n++)
								cMap1D.setColor(map2fit.map(dmodel.getMonoScore(id, n)),
										new Color(Math.max(pure[n].getRed(), 127),
												Math.max(pure[n].getGreen(), 127),
												Math.max(pure[n].getBlue(), 127), 220));
						}
					}
					break;
				default:
					break;
			}
			if (cMap == null)
				throw new Error("MVPop3D: ColorMap not initialized - needs attention!");
			graph.setColorMap(module.processColorMap(cMap));
			graph.reset();
		}
		update(hard);
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
