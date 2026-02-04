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
import java.util.List;

import org.evoludo.graphics.AbstractGraph.Shifter;
import org.evoludo.graphics.AbstractGraph.Zoomer;
import org.evoludo.math.ArrayMath;
import org.evoludo.graphics.GraphStyle;
import org.evoludo.graphics.LineGraph;
import org.evoludo.simulator.ColorMap;
import org.evoludo.simulator.ColorMapCSS;
import org.evoludo.simulator.EvoLudoGWT;
import org.evoludo.simulator.models.DModel;
import org.evoludo.simulator.models.Data;
import org.evoludo.simulator.models.IBSC;
import org.evoludo.simulator.modules.Module;

/**
 * A view that displays time-series plots of mean trait values or mean fitness
 * for the current EvoLudo model using one or more LineGraph panels.
 *
 * <h3>Responsibilities</h3>
 * <ul>
 * <li>Create and manage the set of {@link LineGraph} panels appropriate for the
 * current model configuration (one panel per discrete species, or one
 * panel per continuous trait when the model is continuous).</li>
 * <li>Configure visual style for each graph (labels, y-range, colors,
 * markers) depending on whether the data represents TRAIT or FITNESS and
 * whether the underlying model is continuous or discrete.</li>
 * <li>Collect mean values from the model and push them to the graphs on
 * updates; supports both mean +/- standard deviation (continuous) and
 * per-trait frequencies/mean payoffs (discrete).</li>
 * <li>Manage zooming and shifting of displayed window of time series data.</li>
 * <li>Handle layout, sizing and resetting of graphs when model settings
 * change.</li>
 * </ul>
 *
 * <h3>Exports</h3>
 * Supports exporting graphs as SVG and PNG as well as exporting the mean time
 * series data as CSV).
 *
 * @author Christoph Hauert
 * 
 * @see org.evoludo.simulator.views.AbstractView
 * @see org.evoludo.graphics.LineGraph
 * @see org.evoludo.simulator.models.Data
 * @see org.evoludo.simulator.models.DModel
 * @see org.evoludo.simulator.models.IBSC
 */
public class Mean extends AbstractView<LineGraph> implements Shifter, Zoomer {

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
	public Mean(EvoLudoGWT engine, Data type) {
		super(engine, type);
	}

	@Override
	public String getName() {
		switch (type) {
			case TRAIT:
				return "Traits - Mean";
			case FITNESS:
				return "Fitness - Mean";
			default:
				return null;
		}
	}

	@Override
	protected boolean allocateGraphs() {
		List<? extends Module<?>> species = engine.getModule().getSpecies();
		int nGraphs = 0;
		// multiple line graphs for multi-species interactions and in case of multiple
		// traits for continuous traits
		for (Module<?> module : species) {
			// only one graph per species for fitness data
			// but separate graphs for multiple continuous traits
			if (model.isContinuous() && type == Data.TRAIT)
				nGraphs += module.getNTraits();
			else
				nGraphs++;
		}
		// if the number of graphs has changed, destroy and recreate them
		if (graphs.size() == nGraphs)
			return false;
		destroyGraphs();
		// one graph per discrete species or continuous trait
		for (Module<?> module : species) {
			addLineGraph(module);
		}
		// arrange graphs vertically
		gRows = nGraphs;
		int width = 100 / gCols;
		int height = 100 / gRows;
		for (LineGraph graph : graphs)
			graph.setSize(width + "%", height + "%");
		return true;
	}

	/**
	 * Create and register the LineGraph(s) for a single module: one graph for the
	 * module, and additional graphs for extra traits when the model is continuous.
	 * 
	 * @param module module for which graphs should be created
	 */
	private void addLineGraph(Module<?> module) {
		LineGraph graph = new LineGraph(this, module);
		wrapper.add(graph);
		graphs.add(graph);
		if (!model.isContinuous())
			return;
		for (int n = 1; n < module.getNTraits(); n++) {
			graph = new LineGraph(this, module);
			wrapper.add(graph);
			graphs.add(graph);
		}
	}

	@Override
	public void reset(boolean hard) {
		super.reset(hard);
		int idx = 0; // index of trait in continuous models

		for (LineGraph graph : graphs) {
			switch (type) {
				case TRAIT:
					if (model.isContinuous()) {
						setupCTraitGraph(graph, idx);
						idx++;
					} else {
						setupDTraitGraph(graph);
					}
					break;

				case FITNESS:
					if (model.isContinuous())
						setupCFitGraph(graph);
					else
						// note: leave nState alone because of multi-species modules!
						setupDFitGraph(graph);
					hard |= setFitRange(graph);
					break;

				default:
					throw new IllegalArgumentException("Unknown data type: " + type);
			}
			finishGraphSetup(graph, hard);
		}
		update(hard);
	}

	/**
	 * Configure a graph style and colors for a discrete module (multiple traits).
	 * 
	 * @param graph the graph to configure
	 */
	private void setupDTraitGraph(LineGraph graph) {
		GraphStyle style = graph.getStyle();
		if (model.isDensity()) {
			style.yLabel = "density";
			style.percentY = false;
		} else {
			style.yLabel = "frequency";
			style.percentY = true;
		}
		Module<?> module = graph.getModule();
		int nState = model.getNMean(module.getId());
		Color[] colors = resizeColors(module.getMeanColors(), nState);
		graph.setColors(ColorMapCSS.Color2Css(colors));
		String[] mcolors = new String[colors.length];
		int i = 0;
		for (Color color : colors)
			mcolors[i++] = ColorMapCSS.Color2Css(ColorMap.addAlpha(color, 100));
		graph.setMarkers(model.getMarkers(), mcolors);
	}

	/**
	 * Configure a graph style and colors for a single continuous trait.
	 * 
	 * @param graph the graph to configure
	 * @param idx   the index of the trait
	 */
	private void setupCTraitGraph(LineGraph graph, int idx) {
		GraphStyle style = graph.getStyle();
		style.yLabel = model.getMeanName(idx);
		style.percentY = false;
		IBSC cmodel = (IBSC) model;
		style.yMin = cmodel.getTraitRangeMin(0)[idx];
		style.yMax = cmodel.getTraitRangeMax(0)[idx];
		Module<?> module = graph.getModule();
		Color color = module.getMeanColors()[idx];
		String[] traitcolors = new String[3];
		traitcolors[0] = ColorMapCSS.Color2Css(color); // mean
		traitcolors[1] = ColorMapCSS.Color2Css(color == Color.BLACK ? Color.LIGHT_GRAY : color.darker()); // min
		traitcolors[2] = traitcolors[1]; // max
		graph.setColors(traitcolors);
	}

	/**
	 * Set the y-range for fitness graphs; returns true if the range changed.
	 * 
	 * @param graph the graph to configure
	 * @return <code>true</code> if the y-range changed
	 */
	private boolean setFitRange(LineGraph graph) {
		Module<?> module = graph.getModule();
		int id = module.getId();
		double min = model.getMinScore(id);
		double max = model.getMaxScore(id);
		if (max - min < 1e-8) {
			min -= 1.0;
			max += 1.0;
		}
		GraphStyle style = graph.getStyle();
		boolean rangeChanged = Math.abs(min - style.yMin) > 1e-8 || Math.abs(max - style.yMax) > 1e-8;
		if (rangeChanged) {
			style.yMin = min;
			style.yMax = max;
		}
		style.yLabel = "payoffs";
		return rangeChanged;
	}

	/**
	 * Configure a graph style and colors for fitness in discrete models.
	 * 
	 * @param graph the graph to configure
	 */
	private void setupDFitGraph(LineGraph graph) {
		Module<?> module = graph.getModule();
		int nState = model.getNMean(module.getId());

		// one 'state' more for the average fitness
		Color[] colors = resizeColors(module.getMeanColors(), nState);
		colors = ArrayMath.append(colors, Color.BLACK);
		graph.setColors(ColorMapCSS.Color2Css(colors));

		// cast is safe because module is Discrete
		DModel dmodel = (DModel) model;
		// nState = nMean + 1 for fitness
		double[] monoScores = new double[nState + 1];
		// the first entry is for dashed (>0) and dotted (<0) lines
		monoScores[0] = 1.0;
		for (int n = 0; n < nState; n++)
			monoScores[n + 1] = dmodel.getMonoScore(module.getId(), n);
		String[] monoColors = new String[nState + 1];
		int k = 0;
		for (Color color : colors)
			monoColors[k++] = ColorMapCSS.Color2Css(ColorMap.addAlpha(color, 100));
		ArrayList<double[]> marker = new ArrayList<>();
		marker.add(monoScores);
		graph.setMarkers(marker, monoColors);
	}

	/**
	 * Configure a graph style and colors for fitness in continuous models.
	 * 
	 * @param graph the graph to configure
	 */
	private void setupCFitGraph(LineGraph graph) {
		// hardcoded color: black for mean, light gray for mean +/- sdev
		Color[] fitcolors = new Color[] { Color.BLACK, Color.LIGHT_GRAY, Color.LIGHT_GRAY };
		graph.setColors(ColorMapCSS.Color2Css(fitcolors));
	}

	/**
	 * Resize the color array to the specified length by repeating colors as needed.
	 * 
	 * @param colors array of colors to resize
	 * @param length desired length of the resized array
	 * @return the (resized) array of colors
	 */
	private Color[] resizeColors(Color[] colors, int length) {
		Color[] resized = new Color[length];
		if (length == 0 || colors == null || colors.length == 0)
			return resized;
		for (int i = 0; i < length; i++)
			resized[i] = colors[i % colors.length];
		return resized;
	}

	/**
	 * Finalize the setup of a graph (x-label, x-range, module label).
	 * 
	 * @param graph the graph to configure
	 * @param hard  if true, forces a hard reset of the graph
	 */
	private void finishGraphSetup(LineGraph graph, boolean hard) {
		int nState = graph.getNLines();
		// shared buffer across graphs: keep it large enough for graph with most lines.
		if (state == null || state.length < nState)
			state = new double[nState];
		GraphStyle style = graph.getStyle();
		int nSpecies = model.getNSpecies();
		if (nSpecies > 1) {
			Module<?> module = graph.getModule();
			style.label = module.getName();
		}
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

	@Override
	public void modelSettings() {
		super.modelSettings();
		reset(false);
	}

	@Override
	public void modelDidInit() {
		super.modelDidInit();
		update(true);
	}

	@Override
	public void update(boolean force) {
		double newtime = model.getUpdates();
		Module<?> module = null;
		if (Math.abs(timestamp - newtime) > 1e-8) {
			int idx = 0;
			for (LineGraph graph : graphs) {
				idx = updateGraph(graph, module, idx, newtime);
			}
		}
		if (isActive) {
			for (LineGraph graph : graphs)
				graph.paint(force);
		}
		timestamp = newtime;
	}

	/**
	 * Pull updated mean data for the supplied graph and append it to the data
	 * buffer.
	 * 
	 * @param graph   graph to update
	 * @param module  module currently being processed
	 * @param idx     index offset used when processing multiple traits
	 * @param newtime latest simulation time
	 * @return updated index offset for subsequent graphs
	 */
	private int updateGraph(LineGraph graph, Module<?> module, int idx, double newtime) {
		Module<?> nod = graph.getModule();
		boolean newmod = module != nod;
		module = nod;
		int id = module.getId();
		int nState = model.getNMean(id);
		double[] data;
		switch (type) {
			case TRAIT:
				if (newmod) {
					model.getMeanTraits(id, state);
					idx = 0;
				}
				if (model.isContinuous())
					data = updateCTraitGraph(nState, idx++);
				else
					data = updateDTraitGraph(nState);
				break;
			case FITNESS:
				if (newmod)
					model.getMeanFitness(id, state);
				if (model.isContinuous())
					data = updateCFitGraph();
				else
					data = updateDFitGraph(nState);
				break;
			default:
				throw new IllegalArgumentException("Unknown data type: " + type);
		}
		data[0] = newtime;
		graph.addData(data);
		return idx;
	}

	/**
	 * Build the data array for a discrete-trait graph.
	 * 
	 * @param nState number of traits
	 * @return data array (time slot left unassigned)
	 */
	private double[] updateDTraitGraph(int nState) {
		double[] data = new double[nState + 1];
		System.arraycopy(state, 0, data, 1, nState);
		return data;
	}

	/**
	 * Build the data array for a continuous-trait graph (mean +/- stddev).
	 * 
	 * @param nState number of traits
	 * @param idx    state offset of the current trait
	 * @return data array (time slot left unassigned)
	 */
	private double[] updateCTraitGraph(int nState, int idx) {
		double[] data = new double[4];
		double m = state[idx];
		double s = state[idx + nState];
		data[1] = m;
		data[2] = m - s;
		data[3] = m + s;
		return data;
	}

	/**
	 * Build the data array for discrete fitness graphs.
	 * 
	 * @param nState number of traits
	 * @return data array (time slot left unassigned)
	 */
	private double[] updateDFitGraph(int nState) {
		// +1 for time, +1 for average
		double[] data = new double[nState + 1 + 1];
		System.arraycopy(state, 0, data, 1, nState + 1);
		return data;
	}

	/**
	 * Build the data array for continuous fitness graphs (mean +/- stddev).
	 * 
	 * @return data array (time slot left unassigned)
	 */
	private double[] updateCFitGraph() {
		// fitness graph has only a single panel
		double[] data = new double[4];
		double m = state[0];
		double s = state[1];
		data[1] = m;
		data[2] = m - s;
		data[3] = m + s;
		return data;
	}

	@Override
	protected ExportType[] exportTypes() {
		return new ExportType[] { ExportType.SVG, ExportType.PNG, ExportType.CSV_MEAN };
	}
}
