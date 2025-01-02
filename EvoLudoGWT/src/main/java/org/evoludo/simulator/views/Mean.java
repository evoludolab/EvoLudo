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

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import org.evoludo.graphics.AbstractGraph;
import org.evoludo.graphics.AbstractGraph.Shifter;
import org.evoludo.graphics.AbstractGraph.Zoomer;
import org.evoludo.graphics.LineGraph;
import org.evoludo.simulator.ColorMapCSS;
import org.evoludo.simulator.EvoLudoGWT;
import org.evoludo.simulator.models.Data;
import org.evoludo.simulator.models.IBSC;
import org.evoludo.simulator.models.ODEEuler;
import org.evoludo.simulator.modules.Discrete;
import org.evoludo.simulator.modules.Module;

/**
 * The view to display graphs with time series data. Typically this is used to
 * track the mean fitness or configuration of the current EvoLudo model.
 * <p>
 * The view is interactive and allows to shift the time axis and to zoom in and
 * out. For multiple graphs the zooming and shifting is synchronized.
 *
 * @author Christoph Hauert
 */
public class Mean extends AbstractView implements Shifter, Zoomer {

	/**
	 * The list of graphs that display the time series data.
	 * 
	 * @evoludo.impl {@code List<LineGraph> graphs} is deliberately hiding
	 *               {@code List<AbstractGraph> graphs} from the superclass because
	 *               it saves a lot of ugly casting. Note that the two fields point
	 *               to one and the same object.
	 */
	@SuppressWarnings("hiding")
	protected List<LineGraph> graphs;

	/**
	 * The state of the model at the current time.
	 */
	double[] state;

	/**
	 * Construct a new view to display the time series data of the current EvoLudo
	 * model.
	 * 
	 * @param engine the pacemaker for running the model
	 * @param type   the type of data to display
	 */
	@SuppressWarnings("unchecked")
	public Mean(EvoLudoGWT engine, Data type) {
		super(engine, type);
		graphs = (List<LineGraph>) super.graphs;
	}

	@Override
	public String getName() {
		switch (type) {
			case STRATEGY:
				return "Strategies - Mean";
			case FITNESS:
				return "Fitness - Mean";
			default:
				return null;
		}
	}

	@Override
	protected void allocateGraphs() {
		ArrayList<? extends Module> species = engine.getModule().getSpecies();
		int nGraphs = 0;
		// multiple line graphs for multi-species interactions and in case of multiple
		// traits for continuous strategies
		for (Module module : species) {
			if (model.isContinuous())
				nGraphs += module.getNTraits();
			else
				nGraphs++;
		}
		// if the number of graphs has changed, destroy and recreate them
		if (graphs.size() != nGraphs) {
			destroyGraphs();
			// one graph per discrete species or continuous trait
			for (Module module : species) {
				LineGraph graph = new LineGraph(this, module);
				wrapper.add(graph);
				graphs.add(graph);
				if (!model.isContinuous())
					continue;
				for (int n = 1; n < module.getNTraits(); n++) {
					graph = new LineGraph(this, module);
					wrapper.add(graph);
					graphs.add(graph);
				}
			}
			// arrange graphs vertically
			gRows = nGraphs;
			int width = 100 / gCols;
			int height = 100 / gRows;
			for (LineGraph graph : graphs)
				graph.setSize(width + "%", height + "%");
		}
	}

	@Override
	public void reset(boolean hard) {
		super.reset(hard);
		int nSpecies = model.getNSpecies();
		IBSC cmodel = model.isContinuous() ? (IBSC) model : null;
		int nMean = model.getNMean();
		// index of trait in continuous models
		int idx = 0;
		for (LineGraph graph : graphs) {
			AbstractGraph.GraphStyle style = graph.getStyle();
			Module module = graph.getModule();

			switch (type) {
				case STRATEGY:
					state = new double[nMean];
					if (cmodel != null) {
						// continuous module with single trait on graph (single species, for now)
						style.yLabel = model.getMeanName(idx);
						style.percentY = false;
						style.yMin = cmodel.getTraitMin(0)[idx];
						style.yMax = cmodel.getTraitMax(0)[idx];
						Color color = module.getMeanColors()[idx];
						String[] traitcolors = new String[3];
						traitcolors[0] = ColorMapCSS.Color2Css(color); // mean
						traitcolors[1] = ColorMapCSS
								.Color2Css(color == Color.BLACK ? Color.LIGHT_GRAY : color.darker()); // min
						traitcolors[2] = traitcolors[1]; // max
						graph.setColors(traitcolors);
						idx++;
					} else {
						// discrete module with multiple traits on graph
						if (model.isDE() && ((ODEEuler) model).isDensity()) {
							style.yLabel = "density";
							style.percentY = false;
							style.yMin = 0.0;
							style.yMax = 0.0;
						} else {
							style.yLabel = "frequency";
							style.percentY = true;
							style.yMin = 0.0;
							style.yMax = 1.0;
						}
						Color[] colors = module.getMeanColors();
						graph.setColors(ColorMapCSS.Color2Css(colors));
						String[] mcolors = new String[colors.length];
						int n = 0;
						for (Color color : colors)
							mcolors[n++] = ColorMapCSS.Color2Css(ColorMapCSS.addAlpha(color, 100));
						graph.setMarkers(module.getMarkers(), mcolors);
					}
					break;
				case FITNESS:
					Color[] fitcolors;
					int id = module.getID();
					state = new double[nMean + 1];
					if (cmodel != null) {
						// hardcoded color: black for mean, light gray for mean +/- sdev
						fitcolors = new Color[] { Color.BLACK, Color.LIGHT_GRAY, Color.LIGHT_GRAY };
					} else {
						// one 'state' more for the average fitness
						fitcolors = new Color[nMean + 1];
						System.arraycopy(module.getMeanColors(), 0, fitcolors, 0, nMean);
						fitcolors[nMean] = Color.BLACK;
					}
					graph.setColors(ColorMapCSS.Color2Css(fitcolors));
					double min = model.getMinScore(id);
					double max = model.getMaxScore(id);
					if (max - min < 1e-8) {
						min -= 1.0;
						max += 1.0;
					}
					if (Math.abs(min - style.yMin) > 1e-8 || Math.abs(max - style.yMax) > 1e-8) {
						style.yMin = min;
						style.yMax = max;
						hard = true;
					}
					if (nSpecies > 1)
						style.label = module.getName();
					style.yLabel = "payoffs";
					if (module instanceof Discrete) {
						// cast is save because module is Discrete
						org.evoludo.simulator.models.Discrete dmodel = (org.evoludo.simulator.models.Discrete) model;
						double[] monoScores = new double[nMean + 1];
						// the first entry is for dashed (>0) and dotted (<0) lines
						monoScores[0] = 1.0;
						for (int n = 0; n < nMean; n++)
							monoScores[n + 1] = dmodel.getMonoScore(module.getID(), n);
						String[] monoColors = new String[fitcolors.length];
						int n = 0;
						for (Color color : fitcolors)
							monoColors[n++] = ColorMapCSS.Color2Css(ColorMapCSS.addAlpha(color, 100));
						ArrayList<double[]> marker = new ArrayList<>();
						marker.add(monoScores);
						graph.setMarkers(marker, monoColors);
					}
					break;
					default:
						throw new IllegalArgumentException("Unknown data type: " + type);
				}
			if (nSpecies > 1)
				style.label = module.getName();
			style.xLabel = "time";
			style.showXLevels = false;
			double rFreq = model.getTimeStep();
			if (Math.abs(style.xIncr - rFreq) > 1e-8) {
				style.xIncr = rFreq;
				style.xMin = -graph.getSteps() * style.xIncr;
				hard = true;
			}
			style.xMax = 0.0;
			if (hard)
				graph.reset();
		}
		update(hard);
	}

	@Override
	public void init() {
		super.init();
		update(true);
	}

	@Override
	public void update(boolean force) {
		double newtime = model.getTime();
		Module module = null;
		boolean cmodel = model.isContinuous();
		if (Math.abs(timestamp - newtime) > 1e-8) {
			int idx = 0;
			for (LineGraph graph : graphs) {
				Module nod = graph.getModule();
				boolean newmod = module != nod;
				module = nod;
				int id = module.getID();
				int nState = model.getNMean(id);
				switch (type) {
					case STRATEGY:
						if (newmod) {
							idx = 0;
							model.getMeanTraits(id, state);
						}
						// mean cannot be null here
						double[] data;
						if (cmodel) {
							data = new double[4];
							double m = state[idx];
							double s = state[idx + nState];
							data[1] = m;
							data[2] = m - s;
							data[3] = m + s;
							idx++;
						} else {
							data = new double[nState + 1];
							System.arraycopy(state, 0, data, 1, nState);
						}
						data[0] = newtime;
						graph.addData(data);
						break;
					case FITNESS:
						if (newmod)
							model.getMeanFitness(id, state);
						// module cannot be null here but make compiler happy
						if (cmodel) {
							// fitness graph has only a single panel
							data = new double[4];
							double m = state[0];
							double s = state[1];
							data[1] = m;
							data[2] = m - s;
							data[3] = m + s;
						} else {
							// +1 for time, +1 for average
							data = new double[nState + 1 + 1];
							System.arraycopy(state, 0, data, 1, nState + 1);
						}
						data[0] = newtime;
						graph.addData(data);
						break;
					default:
						break;
				}
			}
		}
		if (isActive) {
			for (LineGraph graph : graphs)
				graph.paint(force);
		}
		timestamp = newtime;
	}

	@Override
	protected ExportType[] exportTypes() {
		return new ExportType[] { ExportType.SVG, ExportType.PNG, ExportType.MEAN_DATA };
	}
}
