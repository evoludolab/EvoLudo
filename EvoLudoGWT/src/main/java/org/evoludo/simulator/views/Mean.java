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

import java.awt.Color;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.evoludo.graphics.AbstractGraph;
import org.evoludo.graphics.AbstractGraph.GraphStyle;
import org.evoludo.graphics.AbstractGraph.Shifter;
import org.evoludo.graphics.AbstractGraph.Zoomer;
import org.evoludo.graphics.LineGraph;
import org.evoludo.simulator.ColorMapCSS;
import org.evoludo.simulator.EvoLudoGWT;
import org.evoludo.simulator.models.IBSC;
import org.evoludo.simulator.models.Model;
import org.evoludo.simulator.modules.Discrete;
import org.evoludo.simulator.modules.Module;
import org.evoludo.util.Formatter;
import org.evoludo.util.RingBuffer;

/**
 *
 * @author Christoph Hauert
 */
public class Mean extends AbstractView implements LineGraph.LineGraphController, Shifter, Zoomer {

	// NOTE: this is a bit of a hack that allows us to use graphs as Set<LineGraph>
	// here
	// but as Set<AbstractGraph> in super classes. Saves a lot of ugly casting
	@SuppressWarnings("hiding")
	protected List<LineGraph> graphs;

	double[] state, tmp;

	@SuppressWarnings("unchecked")
	public Mean(EvoLudoGWT engine, Model.Data type) {
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
	public void reset(boolean hard) {
		super.reset(hard);
		ArrayList<? extends Module> species = engine.getModule().getSpecies();
		int nSpecies = model.getNSpecies();
		// multiple line graphs for multi-species interactions and in case of multiple
		// traits for continuous strategies
		IBSC cmodel = model.isContinuous() ? (IBSC) model : null;
		int nMean = model.getNMean();
		int nGraphs = (cmodel != null && type == Model.Data.STRATEGY ? nMean : nSpecies);
		// if the number of graphs has changed, destroy and recreate them
		if (graphs.size() != nGraphs) {
			hard = true;
			destroyGraphs();
			// one graph per discrete species or continuous trait
			for (Module module : species) {
				LineGraph graph = new LineGraph(this, module);
				wrapper.add(graph);
				graphs.add(graph);
				if (cmodel == null)
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

		// index of trait in continuous models
		int idx = 0;
		// make sure we have enough temporary storage for all scenarios
		tmp = new double[2 * model.getNMean() + 1];
		for (LineGraph graph : graphs) {
			AbstractGraph.GraphStyle style = graph.getStyle();
			Module module = graph.getModule();

			switch (type) {
				default:
				case STRATEGY:
					if (cmodel != null) {
						// continuous module with single trait on graph (single species, for now)
						style.yLabel = model.getMeanName(idx);
						style.percentY = false;
						style.yMin = cmodel.getTraitMin(0)[idx];
						style.yMax = cmodel.getTraitMax(0)[idx];
						Color color = model.getMeanColors()[idx];
						String[] traitcolors = new String[3];
						traitcolors[0] = ColorMapCSS.Color2Css(color); // mean
						traitcolors[1] = ColorMapCSS
								.Color2Css(color == Color.BLACK ? Color.LIGHT_GRAY : color.darker()); // min
						traitcolors[2] = traitcolors[1]; // max
						graph.setColors(traitcolors);
						idx++;
					} else {
						// discrete module with multiple traits on graph
						int id = module.getID();
						if (model instanceof Model.DE && ((Model.DE) model).isDensity()) {
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
						Color[] colors = model.getMeanColors(id);
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
					if (cmodel != null) {
						// hardcoded color: black for mean, light gray for mean +/- sdev
						fitcolors = new Color[] { Color.BLACK, Color.LIGHT_GRAY, Color.LIGHT_GRAY };
					} else {
						// one 'state' more for the average fitness
						int nState = model.getNMean(id) + 1;
						fitcolors = new Color[nState];
						System.arraycopy(model.getMeanColors(id), 0, fitcolors, 0, nState - 1);
						fitcolors[nState - 1] = Color.BLACK;
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
						org.evoludo.simulator.models.Model.Discrete dmodel = (org.evoludo.simulator.models.Model.Discrete) model;
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
			}
			if (nSpecies > 1)
				style.label = module.getName();
			style.xLabel = "time";
			style.showXLevels = false;
			double rFreq = engine.getReportInterval();
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
	public void update(boolean force) {
		double newtime = model.getTime();
		Module module = null;
		boolean cmodel = model.isContinuous();
		double[] state = null;
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
							model.getMeanTraits(id, tmp);
						}
						// mean cannot be null here
						if (cmodel) {
							state = new double[4];
							double m = tmp[idx];
							double s = tmp[idx + nState];
							state[1] = m;
							state[2] = m - s;
							state[3] = m + s;
							idx++;
						} else {
							state = new double[nState + 1];
							System.arraycopy(tmp, 0, state, 1, nState);
						}
						state[0] = newtime;
						graph.addData(state, force);
						break;
					case FITNESS:
						if (newmod) {
							model.getMeanFitness(id, tmp);
						}
						// module cannot be null here but make compiler happy
						if (cmodel) {
							// fitness graph has only a single panel
							state = new double[4];
							double m = tmp[0];
							double s = tmp[1];
							state[1] = m;
							state[2] = m - s;
							state[3] = m + s;
						} else {
							state = new double[nState + 1];
							System.arraycopy(tmp, 0, state, 1, nState);
						}
						state[0] = newtime;
						graph.addData(state, force);
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
	public String getTooltipAt(LineGraph graph, double sx, double sy) {
		Module module = graph.getModule();
		int id = module.getID();
		GraphStyle style = graph.getStyle();
		RingBuffer<double[]> buffer = graph.getBuffer();
		double buffert = 0.0;
		double mouset = style.xMin + sx * (style.xMax - style.xMin);
		boolean hasVacant = !(model instanceof Model.DE && ((Model.DE) model).isDensity());
		int vacant = module.getVacant();
		Iterator<double[]> i = buffer.iterator();
		String tip = "<table style='border-collapse:collapse;border-spacing:0;'>" +
				"<tr><td style='text-align:right'><i>" + style.xLabel + ":</i></td><td>" +
				Formatter.format(mouset, 2) + "</td></tr>" +
				"<tr><td style='text-align:right'><i>" + style.yLabel + ":</i></td><td>" +
				(style.percentY ? Formatter.formatPercent(style.yMin + sy * (style.yMax - style.yMin), 1)
						: Formatter.format(style.yMin + sy * (style.yMax - style.yMin), 2))
				+ "</td></tr>";
		if (i.hasNext()) {
			double[] current = i.next();
			int len = current.length;
			while (i.hasNext()) {
				double[] prev = i.next();
				double dt = current[0] - prev[0];
				buffert -= Math.max(0.0, dt);
				if (buffert > mouset) {
					current = prev;
					continue;
				}
				double fx = 1.0 - (mouset - buffert) / dt;
				tip += "<tr><td colspan='2'><hr/></td></tr><tr><td style='text-align:right'><i>" + style.xLabel +
						":</i></td><td>" + Formatter.format(current[0] - fx * dt, 2) + "</td></tr>";
				Color[] colors = model.getMeanColors(id);
				if (model.isContinuous()) {
					double inter = interpolate(current[1], prev[1], fx);
					tip += "<tr><td style='text-align:right'><i style='color:"
							+ ColorMapCSS.Color2Css(colors[0]) + ";'>mean:</i></td><td>" +
							(style.percentY ? Formatter.formatPercent(inter, 2) : Formatter.format(inter, 2));
					double sdev = inter - interpolate(current[2], prev[2], fx); // data: mean, mean-sdev, mean+sdev
					tip += " ± " + (style.percentY ? Formatter.formatPercent(sdev, 2) : Formatter.format(sdev, 2))
							+ "</td></tr>";
				} else {
					// len includes time
					for (int n = 0; n < len - 1; n++) {
						if (!hasVacant && n == vacant)
							continue;
						String name;
						Color color;
						int n1 = n + 1;
						if (n1 == len - 1 && type == Model.Data.FITNESS) {
							name = "average";
							color = Color.BLACK;
						} else {
							name = model.getMeanName(n);
							color = colors[n];
						}
						if (name == null)
							continue;
						tip += "<tr><td style='text-align:right'><i style='color:" + ColorMapCSS.Color2Css(color)
								+ ";'>" + name + ":</i></td><td>";
						// deal with NaN's
						if (prev[n1] == prev[n1] && current[n1] == current[n1]) {
							tip += (style.percentY ? Formatter.formatPercent(interpolate(current[n1], prev[n1], fx), 2)
									: Formatter.format(interpolate(current[n1], prev[n1], fx), 2)) + "</td></tr>";
						} else
							tip += "-</td></tr>";
					}
				}
				break;
			}
		}
		return tip + "</table>";
	}

	private double interpolate(double current, double prev, double x) {
		return (1.0 - x) * current + x * prev;
	}

	@Override
	protected ExportType[] exportTypes() {
		return new ExportType[] { ExportType.SVG, ExportType.PNG, ExportType.MEAN_DATA };
	}
}
