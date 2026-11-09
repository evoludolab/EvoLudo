//
// EvoLudo Project
//
// Copyright 2010-2026 Christoph Hauert
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
import java.util.Arrays;
import java.util.List;

import org.evoludo.graphics.GraphStyle;
import org.evoludo.graphics.HistoGraph;
import org.evoludo.math.ArrayMath;
import org.evoludo.math.Combinatorics;
import org.evoludo.math.Distributions;
import org.evoludo.math.Functions;
import org.evoludo.simulator.ColorMap;
import org.evoludo.simulator.ColorMapCSS;
import org.evoludo.simulator.EvoLudoGWT;
import org.evoludo.simulator.geometries.AbstractGeometry;
import org.evoludo.simulator.geometries.GeometryFeatures;
import org.evoludo.simulator.geometries.GeometryType;
import org.evoludo.simulator.models.CModel;
import org.evoludo.simulator.models.DModel;
import org.evoludo.simulator.models.Data;
import org.evoludo.simulator.models.FixationData;
import org.evoludo.simulator.models.IBSPopulation;
import org.evoludo.simulator.models.Mode;
import org.evoludo.simulator.models.Model;
import org.evoludo.simulator.modules.Continuous;
import org.evoludo.simulator.modules.Discrete;
import org.evoludo.simulator.modules.Module;
import org.evoludo.ui.ContextMenu;
import org.evoludo.ui.ContextMenuCheckBoxItem;
import org.evoludo.ui.ContextMenuItem;
import org.evoludo.util.Formatter;
import org.evoludo.util.NativeJS;

/**
 * Histogram view for displaying binned distributions of model quantities.
 * <p>
 * This class implements a graphical view that manages one or more
 * {@link HistoGraph}
 * instances to display histograms for different types of data produced by the
 * simulation model. The view supports a variety of {@link Data} types:
 * <ul>
 * <li>{@code TRAIT} - histograms of continuous trait distributions (uses CModel
 * APIs)</li>
 * <li>{@code FITNESS} - histograms of fitness/payoff values (continuous or
 * discrete)</li>
 * <li>{@code DEGREE} - degree distributions of interaction and competition
 * graphs</li>
 * <li>{@code STATISTICS_FIXATION_PROBABILITY} - per-node fixation probability
 * samples</li>
 * <li>{@code STATISTICS_FIXATION_TIME} - fixation time samples and absorption
 * times</li>
 * <li>{@code STATISTICS_STATIONARY} - stationary distributions / visit-count
 * histograms</li>
 * </ul>
 *
 * <h3>Responsibilities</h3>
 * <ul>
 * <li>Allocate and layout the required number of {@code HistoGraph} panels for
 * each module and data type, taking into account multi-species modules and
 * module-specific settings (traits, vacant indices, population size,
 * etc.).</li>
 * <li>Configure default and data-specific {@link GraphStyle} settings (axis
 * labels, normalization, color, autoscaling, custom y-levels).</li>
 * <li>Manage histogram buffers and binning parameters (bin size, scale factors,
 * max bins) and ensure data arrays are allocated with proper dimensions.</li>
 * <li>Periodically update histogram contents by querying the underlying model
 * (e.g., fetching trait/fitness/degree histogram data, mean traits for
 * stationary statistics, fixation sample callbacks).</li>
 * <li>Handle special cases such as PDE/DE/ODE/SDE models and well-mixed
 * populations (display messages instead of histograms where appropriate).</li>
 * <li>Support context menu actions and export types appropriate for the
 * chosen {@link Data} type (SVG/PNG and CSV for statistics).</li>
 * </ul>
 *
 * <h3>Lifecycle</h3>
 * <ul>
 * <li>The view is constructed with an {@link EvoLudoGWT} engine and a
 * {@link Data} type
 * indicating which histogram to display.</li>
 * <li>{@link #load()} registers listeners for statistical sample events when
 * displaying fixation statistics.</li>
 * <li>{@link #allocateGraphs()} is responsible for creating HistoGraph
 * instances based on the currently configured modules; it avoids unnecessary
 * rebuilds by comparing required and existing graph counts.</li>
 * <li>{@link #reset(boolean)} recomputes graph-specific configuration (axis
 * ranges,
 * markers, normalization flags) and may force a full repaint when axis
 * limits change. It delegates type-specific reset logic to helper methods
 * (resetTrait, resetFitness, resetDegree, resetFixationProbability,
 * resetFixationTime, resetStationary).</li>
 * <li>{@link #update(boolean)} pulls fresh histogram data from the model and
 * triggers painting of all graphs. For performance, updates are skipped
 * when the view is inactive for dynamic data.</li>
 * <li>{@link #modelSample(boolean)} receives fixation-sample callbacks and
 * increments the appropriate histogram bins; it also handles per-sample
 * bookkeeping to avoid double-counting.</li>
 * <li>{@link #modelSettings()} and {@link #modelDidInit()} are used to reapply
 * module-specific settings and to initialize HistoGraph objects after model
 * initialization.</li>
 * </ul>
 *
 * <h3>Implementation details</h3>
 * <ul>
 * <li>scale2bins and binSize are used to map model values (counts, densities,
 * or node indices) to histogram bins. Their values depend on the Data type
 * and the population/graph size.</li>
 * <li>For stationary and fixation statistics the view chooses a sensible bin
 * count that honors {@code HistoGraph.DEFAULT_BINS} and per-graph max bin
 * limits.</li>
 * <li>For degree distributions, the view computes a displayable maximum degree
 * with maxDegree(int) (rounding up to human-friendly magnitudes) and
 * limits the resulting bin count to {@code HistoGraph.DEFAULT_BINS}.</li>
 * <li>Directed graphs create separate histograms for in-degree, out-degree,
 * and total degree. When interaction and competition geometries differ,
 * histograms are created for both structures.</li>
 * <li>For PDE/DE model types that are regular or lattice-like the view may
 * display a short explanatory message instead of a histogram.</li>
 * <li>The class caches whether degree distributions have been processed and
 * re-computes them only when geometries are dynamic or when not yet
 * processed.</li>
 * <li>Frequent status calculations and graph paints are throttled to avoid
 * excessive CPU usage (see {@link AbstractView#MIN_MSEC_BETWEEN_UPDATES} and
 * timestamp checks).</li>
 * </ul>
 *
 * <h3>Fixation statistics</h3>
 * <ul>
 * <li>For {@code STATISTICS_FIXATION_PROBABILITY} the view accumulates counts
 * per node (possibly binned) and reports normalized probabilities in the
 * y-axis.</li>
 * <li>For {@code STATISTICS_FIXATION_TIME} the view supports two modes:
 * <ul>
 * <li>a per-node fixation time histogram (node index on x-axis and
 * average/weighted time in y), or</li>
 * <li>a distribution of fixation times aggregated across nodes (binned
 * times on x-axis).</li>
 * </ul>
 * </li>
 * <li>A special absorption histogram collects fixation times irrespective of
 * initial node (displayed as an extra graph when applicable).</li>
 * <li>The view produces human-readable status strings that summarize average
 * fixation probability and fixation time statistics for display in the UI.</li>
 * </ul>
 *
 * <h3>Export and context menu</h3>
 * <ul>
 * <li>Depending on the Data type, export formats include SVG and PNG for all
 * histograms, and CSV (tabular statistical data) for statistical views.</li>
 * <li>The context menu includes a Clear action for
 * {@code STATISTICS_STATIONARY} to
 * reset accumulated stationary counts.</li>
 * </ul>
 * 
 * @author Christoph Hauert
 *
 * @see HistoGraph
 * @see EvoLudoGWT
 * @see Module
 * @see AbstractGeometry
 * @see GraphStyle
 */
public class Histogram extends AbstractView<HistoGraph> {

	/**
	 * Available bin counts for trait/fitness histograms.
	 */
	private static final int[] HISTOGRAM_BIN_OPTIONS = new int[] { 25, 50, 100, 200, 500 };

	/**
	 * The scaling factor to map the data onto bins.
	 */
	double scale2bins = 1.0;

	/**
	 * Small epsilon to avoid floating point truncation when mapping values to bins.
	 */
	private static final double BIN_EPSILON = 1e-12;

	/**
	 * The size of the bins in pixels.
	 */
	int binSize = 1;

	/**
	 * Flag to indicate whether the model entertains multiple species, i.e.
	 * {@code nSpecies&gt;1}. Convenience field. Reduces calls to {@link Module}.
	 */
	protected boolean isMultispecies;

	/**
	 * The flag to indicate whether the properties of the gemetric structure have
	 * been processed.
	 */
	protected boolean degreeProcessed;

	/**
	 * Active number of bins for trait/fitness histograms.
	 */
	private int histogramBins = HistoGraph.DEFAULT_BINS;

	/**
	 * Construct a new view to display the histogram of various quantities.
	 * 
	 * @param engine the pacemaker for running the model
	 * @param type   the type of data to display
	 */
	public Histogram(EvoLudoGWT engine, Data type) {
		super(engine, type);
	}

	@Override
	public boolean load() {
		if (!super.load())
			return false;
		if (type == Data.STATISTICS_FIXATION_PROBABILITY || type == Data.STATISTICS_FIXATION_TIME)
			// listen to samples for fixation statistics
			engine.addSampleListener(this);
		return true;
	}

	@Override
	public void unload() {
		super.unload();
		engine.removeSampleListener(this);
	}

	@Override
	public Mode getMode() {
		return type.getMode();
	}

	@Override
	public String getName() {
		return type.toString();
	}

	@Override
	protected boolean allocateGraphs() {
		int nGraphs = countGraphs();
		if (graphs.size() == nGraphs)
			return false;
		destroyGraphs();
		degreeProcessed = false;
		createGraphs(nGraphs);
		// arrange histograms vertically
		gRows = nGraphs;
		layoutGraphs();
		return true;
	}

	@Override
	protected void beforeResize() {
		if (getOffsetWidth() == 0 || getOffsetHeight() == 0)
			return;
		layoutGraphs();
	}

	/**
	 * Size histogram panels so their drawable frames have equal height while
	 * distributing x-axis decoration space across the stack.
	 */
	private void layoutGraphs() {
		if (graphs.isEmpty())
			return;
		int width = 100 / gCols;
		int height = getOffsetHeight();
		if (height <= 0) {
			layoutFallbackPercent(width);
			return;
		}
		double totalDeco = 0.0;
		for (HistoGraph graph : graphs)
			totalDeco += getXDecorationHeight(graph.getStyle());
		double frameHeight = (height - totalDeco) / gRows;
		if (frameHeight <= 0.0) {
			layoutFallbackPercent(width);
			return;
		}
		int remainder = height;
		int idx = 0;
		for (HistoGraph graph : graphs) {
			int thisHeight = (++idx == graphs.size())
					? remainder
					: (int) Math.round(frameHeight + getXDecorationHeight(graph.getStyle()));
			if (idx != graphs.size())
				remainder -= thisHeight;
			graph.setSize(width + "%", thisHeight + "px");
		}
	}

	/**
	 * Assign equal percent heights when pixel measurements are unavailable.
	 *
	 * @param width width percentage for each graph column
	 */
	private void layoutFallbackPercent(int width) {
		int base = 100 / gRows;
		int remainder = 100;
		int idx = 0;
		for (HistoGraph graph : graphs) {
			int thisHeight = (++idx == graphs.size()) ? remainder : base;
			if (idx != graphs.size())
				remainder -= thisHeight;
			graph.setSize(width + "%", thisHeight + "%");
		}
	}

	/**
	 * Return the vertical space used by x-axis decorations in pixels.
	 *
	 * @param style graph style describing x-axis decorations
	 * @return total decoration height in pixels
	 */
	private double getXDecorationHeight(GraphStyle style) {
		double deco = 0.0;
		if (style.showXLabel && style.xLabel != null)
			deco += 20.0;
		if (style.showXTickLabels)
			deco += 14.0;
		if (style.showXTicks)
			deco += style.tickLength + 2.0;
		return deco;
	}

	/**
	 * Count how many graphs are required based on current {@link Data} type and
	 * module settings.
	 * 
	 * @return number of histogram panels required
	 */
	private int countGraphs() {
		List<? extends Module<?>> species = engine.getModule().getSpecies();
		isMultispecies = (species.size() > 1);
		int nGraphs = 0;
		for (Module<?> module : species)
			nGraphs += getGraphRows(module).length;
		return nGraphs;
	}

	/**
	 * Get the data rows of the graphs for {@code module}, i.e. typically the
	 * indices of the traits, based on the current {@link Data} type and module
	 * settings.
	 * 
	 * @param module the module
	 * @return the data rows of the graphs
	 */
	private int[] getGraphRows(Module<?> module) {
		int nTraits = module.getNTraits();
		int vacant = module.getVacantIdx();
		switch (type) {
			case TRAIT:
			case STATISTICS_STATIONARY:
				return rows(nTraits, -1);
			case FITNESS:
				// vacant sites are a trait only in discrete modules
				return model.isContinuous() ? rows(1, -1) : rows(nTraits, vacant);
			case DEGREE:
				if (!model.isIBS())
					return rows(1, -1);
				AbstractGeometry[] geometries = getGeometries(module);
				return rows(getDegreeGraphs(geometries[0], geometries[1]), -1);
			case STATISTICS_FIXATION_TIME:
			case STATISTICS_FIXATION_PROBABILITY:
				// no graph for vacant trait if monostop; fixation times include absorption
				int skip = (vacant >= 0 && module instanceof Discrete && ((Discrete) module).getMonoStop()) ? vacant
						: -1;
				return rows(type == Data.STATISTICS_FIXATION_TIME ? nTraits + 1 : nTraits, skip);
			default:
				throw new IllegalArgumentException("Unknown data type: " + type);
		}
	}

	/**
	 * Get the rows {@code 0, ..., n - 1} except {@code skip}.
	 * 
	 * @param n    the number of rows
	 * @param skip the row to skip or {@code -1}
	 * @return the rows
	 */
	private static int[] rows(int n, int skip) {
		int[] rows = new int[skip >= 0 && skip < n ? n - 1 : n];
		int i = 0;
		for (int r = 0; r < n; r++) {
			if (r != skip)
				rows[i++] = r;
		}
		return rows;
	}

	/**
	 * Get the interaction and competition geometries of {@code module}.
	 * 
	 * @param module the module
	 * @return the interaction and competition geometries
	 */
	private AbstractGeometry[] getGeometries(Module<?> module) {
		if (!model.isIBS())
			return new AbstractGeometry[] { module.getGeometry(), module.getGeometry() };
		IBSPopulation<?, ?> ibspop = module.getIBSPopulation();
		return new AbstractGeometry[] { ibspop.getInteractionGeometry(), ibspop.getCompetitionGeometry() };
	}

	/**
	 * Create and configure graphs for all species. The x-axis shows tick labels
	 * for the last graph of each species and the label for the last graph overall.
	 * 
	 * @param nGraphs total number of graphs to create across species
	 */
	private void createGraphs(int nGraphs) {
		int idx = 0;
		for (Module<?> module : engine.getModule().getSpecies()) {
			int[] rows = getGraphRows(module);
			for (int row : rows) {
				HistoGraph graph = new HistoGraph(this, module, row);
				wrapper.add(graph);
				graphs.add(graph);
				idx++;
				if (type == Data.DEGREE && (model.isODE() || model.isSDE()))
					continue;
				GraphStyle style = graph.getStyle();
				applyDefaultStyle(style, type != Data.TRAIT && type != Data.DEGREE);
				style.showXLabel = (idx == nGraphs);
				style.showXTickLabels = style.showXLabel || row == rows[rows.length - 1];
				applyStyle(graph);
			}
		}
	}

	/**
	 * Apply default style settings to the given graph style of a newly allocated
	 * graph. Settings that users can change through the context menu, such as the
	 * range and autoscaling of the {@code y}-axis, are set here only and are
	 * retained when the view is reset.
	 *
	 * @param style     the graph style to modify
	 * @param autoscale {@code true} to autoscale the {@code y}-axis
	 */
	private void applyDefaultStyle(GraphStyle style, boolean autoscale) {
		style.yLabel = "frequency";
		style.percentY = true;
		style.autoscaleY = autoscale;
		style.yMin = 0.0;
		style.yMax = 1.0;
		style.showYLabel = true;
		style.showYTickLabels = true;
		style.showYAxisRight = false;
		style.showXTicks = true;
		style.showYTicks = true;
		style.showXLevels = false;
		style.showYLevels = true;
		style.showLabel = false;
		style.showXLabel = false;
		style.showXTickLabels = false;
	}

	/**
	 * Apply style settings for trait histograms.
	 * 
	 * @param graph the graph to style
	 * @param n     the index of the graph
	 * @return {@code true} if hard reset is required
	 */
	private boolean applyTraitStyle(HistoGraph graph, int n) {
		boolean hard = false;
		GraphStyle style = graph.getStyle();
		style.showLabel = isMultispecies;
		style.showXLabel = true;
		style.showXTickLabels = true;
		Continuous cmod = (Continuous) graph.getModule();
		double min = cmod.getTraitMin()[n];
		double max = cmod.getTraitMax()[n];
		if (Math.abs(min - style.xMin) > 1e-6 || Math.abs(max - style.xMax) > 1e-6) {
			style.xMin = min;
			style.xMax = max;
			hard = true;
		}
		style.xLabel = cmod.getTraitName(n);
		Color[] colors = cmod.getTraitColors();
		style.graphColor = ColorMapCSS.Color2Css(colors[n]);
		if (isMultispecies)
			style.label = cmod.getName() + ": " + cmod.getTraitName(n);
		return hard;
	}

	/**
	 * Apply style settings for fitness histograms.
	 * 
	 * @param graph the graph to style
	 * @param n     the index of the graph
	 * @return {@code true} if hard reset is required
	 */
	private boolean applyFitnessStyle(HistoGraph graph, int n) {
		boolean hard = false;
		GraphStyle style = graph.getStyle();
		if (model.isDensity()) {
			style.yLabel = "density";
			style.percentY = false;
		}
		style.xLabel = "payoffs";
		if (model.isContinuous())
			style.showLabel = isMultispecies;
		else
			style.showLabel = true;

		Module<?> module = graph.getModule();
		int id = module.getId();
		double min = model.getMinScore(id);
		double max = model.getMaxScore(id);
		if (Math.abs(min - style.xMin) > 1e-6 || Math.abs(max - style.xMax) > 1e-6) {
			style.xMin = min;
			style.xMax = max;
			hard = true;
		}
		Color[] colors = module.getTraitColors();
		if (module instanceof Discrete) {
			// cast is save because pop is Discrete
			DModel dmodel = (DModel) model;
			style.label = (isMultispecies ? module.getName() + ": " : "") + module.getTraitName(n);
			Color tColor = colors[n];
			style.graphColor = ColorMapCSS.Color2Css(tColor);
			double mono = dmodel.getMonoScore(id, n);
			if (!Double.isNaN(mono)) {
				graph.addMarker(mono,
						ColorMapCSS.Color2Css(ColorMap.blendColors(tColor, Color.WHITE, 0.5)),
						"monomorphic payoff");
			}
		}
		if (module instanceof Continuous) {
			// cast is save because pop is Continuous
			CModel cmodel = (CModel) model;
			Color tcolor = colors[n];
			graph.addMarker(cmodel.getMinMonoScore(id),
					ColorMapCSS.Color2Css(ColorMap.blendColors(tcolor, Color.BLACK, 0.5)),
					"minimum monomorphic payoff");
			graph.addMarker(cmodel.getMaxMonoScore(id),
					ColorMapCSS.Color2Css(ColorMap.blendColors(tcolor, Color.WHITE, 0.5)),
					"maximum monomorphic payoff");
			style.graphColor = ColorMapCSS.Color2Css(Color.BLACK);
		}
		return hard;
	}

	/**
	 * Apply style settings for degree histograms.
	 * 
	 * @param graph the graph to style
	 * @param n     the index of the graph
	 */
	private void applyDegreeStyle(HistoGraph graph, int n) {
		GraphStyle style = graph.getStyle();
		style.xLabel = "degree";
		AbstractGeometry[] geometries = getGeometries(graph.getModule());
		AbstractGeometry inter = geometries[0];
		AbstractGeometry comp = geometries[1];
		String[] labels = getDegreeLabels(getDegreeGraphs(inter, comp), inter.isUndirected());
		style.label = labels[n];
		if (model.isContinuous())
			style.showLabel = isMultispecies;
		else
			style.showLabel = true;
		style.graphColor = "#444";
		style.xMin = 0.0;
		style.xMax = maxDegree(inter, comp);
	}

	/**
	 * Apply style settings for fixation probability histograms.
	 * 
	 * @param graph the graph to style
	 * @param n     the index of the graph
	 */
	private void applyFixationProbabilityStyle(HistoGraph graph, int n) {
		GraphStyle style = graph.getStyle();
		style.yLabel = "probability";
		style.xLabel = "node";
		style.showLabel = true;
		Module<?> module = graph.getModule();
		style.xMin = 0;
		style.xMax = (model.isSDE() ? 1 : module.getNPopulation() - 1);
		style.label = module.getTraitName(n);
		Color[] colors = module.getTraitColors();
		style.graphColor = ColorMapCSS.Color2Css(colors[n]);
		style.customYLevels = ((HasHistogram) module).getCustomLevels(type, n);
	}

	/**
	 * Apply style settings for fixation time histograms.
	 * 
	 * @param graph the graph to style
	 * @param n     the index of the graph, {@code nTraits} for the absorption time
	 */
	private void applyFixationTimeStyle(HistoGraph graph, int n) {
		GraphStyle style = graph.getStyle();
		style.showLabel = true;
		Module<?> module = graph.getModule();
		int nPop = module.getNPopulation();
		if (n == module.getNTraits()) {
			style.label = "Absorption";
			style.graphColor = ColorMapCSS.Color2Css(Color.BLACK);
		} else {
			Color[] colors = module.getTraitColors();
			style.label = module.getTraitName(n);
			style.graphColor = ColorMapCSS.Color2Css(colors[n]);
		}
		if (doFixtimeDistr(module)) {
			style.xMin = 0.0;
			style.xMax = Functions.roundUp(nPop * 0.25);
			style.xLabel = "fixation time";
			style.yLabel = "probability";
			style.percentY = true;
			style.customYLevels = new double[0];
		} else {
			style.xMin = 0.0;
			style.xMax = nPop - 1.0;
			style.xLabel = "node";
			style.yLabel = "time";
			style.percentY = false;
			style.customYLevels = ((HasHistogram) module).getCustomLevels(type, n);
		}
	}

	/**
	 * Apply style settings for stationary distribution histograms.
	 * 
	 * @param graph the graph to style
	 * @param n     the index of the graph
	 */
	private void applyStationaryStyle(HistoGraph graph, int n) {
		GraphStyle style = graph.getStyle();
		style.yLabel = "visits";
		style.showLabel = true;
		style.percentX = false;
		Module<?> module = graph.getModule();
		int nPop = module.getNPopulation();
		if (model.isDE()) {
			if (model.isDensity()) {
				style.xLabel = "density";
				style.xMin = 0.0;
				style.xMax = 0.0;
			} else {
				style.xLabel = "frequency";
				style.xMin = 0.0;
				style.xMax = 1.0;
				style.percentX = true;
			}
		} else {
			style.xLabel = "strategy count";
			style.xMin = 0.0;
			style.xMax = nPop;
		}
		Color[] colors = module.getTraitColors();
		style.graphColor = ColorMapCSS.Color2Css(colors[n]);
		style.label = (isMultispecies ? module.getName() + ": " : "") + module.getTraitName(n);
	}

	@Override
	public void reset(boolean hard) {
		super.reset(hard);
		degreeProcessed = false;
		Module<?> module = null;
		double[][] data = null;
		int idx = 0;
		for (HistoGraph graph : graphs) {
			// graphs of the same species share their data
			if (graph.getModule() != module) {
				module = graph.getModule();
				data = graph.getData();
			}
			int[] size = resetGraph(graph, idx++);
			if (size == null) {
				graph.setData(null);
				continue;
			}
			if (data == null || data.length != size[0] || data[0].length != size[1])
				data = new double[size[0]][size[1]];
			graph.setData(data);
			hard |= applyStyle(graph);
		}
		if (hard) {
			for (HistoGraph graph : graphs)
				graph.reset();
		}
		update(hard);
	}

	/**
	 * Reset {@code graph} for the current {@link Data} type and model, e.g. its
	 * markers, normalization and binning.
	 * 
	 * @param graph the graph to reset
	 * @param idx   the index of the graph across species
	 * @return the number of rows and columns of the data, or {@code null} if the
	 *         graph shows no data
	 */
	private int[] resetGraph(HistoGraph graph, int idx) {
		Module<?> module = graph.getModule();
		int nTraits = module.getNTraits();
		int nPop = module.getNPopulation();
		int maxBins = graph.getMaxBins();
		switch (type) {
			case TRAIT:
				graph.clearMarkers();
				String color = ColorMapCSS.Color2Css(module.getTraitColors()[graph.getRow()]);
				GraphStyle style = graph.getStyle();
				// markers refer to the traits of all species
				for (double[] mark : model.getMarkers())
					graph.addMarker(mark[idx + 1], color, null, mark[0] > 0.0 ? style.dashedLine : style.dottedLine);
				return new int[] { nTraits, histogramBins };
			case FITNESS:
				graph.clearMarkers();
				// vacant sites are a trait only in discrete modules
				if (!model.isContinuous() && module.getVacantIdx() >= 0)
					nTraits--;
				return new int[] { model.isContinuous() ? 1 : nTraits, histogramBins };
			case DEGREE:
				if (model.isODE() || model.isSDE())
					return null;
				AbstractGeometry[] geometries = getGeometries(module);
				return new int[] { getDegreeGraphs(geometries[0], geometries[1]),
						getDegreeBins(geometries[0], geometries[1]) };
			case STATISTICS_FIXATION_PROBABILITY:
				model.resetStatisticsSample();
				graph.setNormalized(nTraits);
				int n = (model.isSDE() ? 1 : nPop);
				if (maxBins <= 0)
					maxBins = Math.min(n, HistoGraph.DEFAULT_BINS);
				binSize = 1;
				int bins = n;
				while (bins > maxBins)
					bins = n / ++binSize;
				scale2bins = 1.0 / binSize;
				return new int[] { nTraits + 1, bins };
			case STATISTICS_FIXATION_TIME:
				model.resetStatisticsSample();
				if (doFixtimeDistr(module)) {
					graph.setNormalized(false);
					graph.setNormalized(-1);
					// widen the bins by one pixel; multiply before dividing to avoid
					// integer division truncating the ratio to zero
					return new int[] { nTraits + 1,
							maxBins * (HistoGraph.MIN_BIN_WIDTH + 1) / (HistoGraph.MIN_BIN_WIDTH + 2) };
				}
				graph.setNormalized(true);
				graph.setNormalized(graph.getRow() + nTraits + 1);
				// doFixtimeDistr ensures nPop <= HistoGraph.DEFAULT_BINS
				return new int[] { 2 * (nTraits + 1), nPop };
			case STATISTICS_STATIONARY:
				graph.setNormalized(false);
				int nBins = Math.max(1, Math.min(nPop, maxBins <= 0 ? HistoGraph.DEFAULT_BINS : maxBins));
				scale2bins = nBins;
				return new int[] { nTraits, nBins };
			default:
				throw new IllegalArgumentException("Unknown data type: " + type);
		}
	}

	/**
	 * Apply the style settings for the current {@link Data} type to
	 * {@code graph}.
	 * 
	 * @param graph the graph to style
	 * @return {@code true} if hard reset is required
	 */
	private boolean applyStyle(HistoGraph graph) {
		int row = graph.getRow();
		switch (type) {
			case TRAIT:
				return applyTraitStyle(graph, row);
			case FITNESS:
				return applyFitnessStyle(graph, row);
			case DEGREE:
				applyDegreeStyle(graph, row);
				break;
			case STATISTICS_FIXATION_PROBABILITY:
				applyFixationProbabilityStyle(graph, row);
				break;
			case STATISTICS_FIXATION_TIME:
				applyFixationTimeStyle(graph, row);
				break;
			case STATISTICS_STATIONARY:
				applyStationaryStyle(graph, row);
				break;
			default:
				throw new IllegalArgumentException("Unknown data type: " + type);
		}
		return false;
	}

	@Override
	public void modelSettings() {
		// check if settings changed the number of graphs
		if (graphs.size() != countGraphs()) {
			// reallocate graphs
			reset(true);
			return;
		}
		for (HistoGraph graph : graphs)
			graph.getStyle().customYLevels = ((HasHistogram) graph.getModule()).getCustomLevels(type, graph.getRow());
	}

	@Override
	protected void onModelDidInit() {
		for (HistoGraph graph : graphs)
			graph.init();
		update();
	}

	@Override
	public void modelSample(boolean success) {
		if (!success)
			return;

		// NOTE: not fully ready for multi-species; info which species fixated missing
		FixationData fixData = model.getFixationData();
		int initNode = fixData.mutantNode;
		if (initNode < 0)
			return;
		HistoGraph graph = graphs.get(fixData.typeFixed);
		// new data available - update histograms
		switch (type) {
			case STATISTICS_FIXATION_PROBABILITY:
				if (fixData.probRead)
					return;
				graph.addData((int) (fixData.mutantNode * scale2bins + BIN_EPSILON));
				fixData.probRead = true;
				break;
			case STATISTICS_FIXATION_TIME:
				if (fixData.timeRead)
					return;
				HistoGraph absorption = graphs.get(graphs.size() - 1);
				if (doFixtimeDistr(graph.getModule())) {
					graph.addData(fixData.updatesFixed);
					absorption.addData(fixData.updatesFixed);
				} else {
					graph.addData(initNode, fixData.updatesFixed);
					absorption.addData(initNode, fixData.updatesFixed);
				}
				fixData.timeRead = true;
				break;
			default:
		}
		if (!isActive)
			return;
		if (!doUpdateView(false))
			return;
		for (HistoGraph g : graphs)
			g.paint(false);
	}

	@Override
	protected void updateData() {
		if (!isActive || type != Data.STATISTICS_STATIONARY)
			return;
		double newtime = model.getUpdates();
		if (Math.abs(timestamp - newtime) > Model.EPS) {
			timestamp = newtime;
			updateStationary();
		}
	}

	@Override
	public void update(boolean force) {
		if (!isActive)
			return;

		double newtime = model.getUpdates();
		if (Math.abs(timestamp - newtime) > Model.EPS) {
			timestamp = newtime;
			switch (type) {
				case TRAIT:
				case FITNESS:
					updateHistograms();
					break;
				case DEGREE:
					updateDegree();
					break;
				case STATISTICS_STATIONARY:
					updateStationary();
					break;
				default:
					break;
			}
		}

		for (HistoGraph graph : graphs)
			graph.paint(force);
	}

	/**
	 * Update trait (continuous models) or fitness histograms. Graphs of the same
	 * species share their data.
	 */
	private void updateHistograms() {
		double[][] data = null;
		for (HistoGraph graph : graphs) {
			double[][] graphdata = graph.getData();
			if (data == graphdata)
				continue;
			data = graphdata;
			int id = graph.getModule().getId();
			if (type == Data.TRAIT)
				((CModel) model).getTraitHistogramData(id, data);
			else
				model.getFitnessHistogramData(id, data);
		}
	}

	/**
	 * Update degree histograms, delegating to getDegreeHistogramData where needed.
	 */
	private void updateDegree() {
		double[][] data = null;
		for (HistoGraph graph : graphs) {
			Module<?> module = graph.getModule();
			if (model.isDE() && handleDEGraph(graph, module))
				continue;
			AbstractGeometry[] geometries = getGeometries(module);
			AbstractGeometry inter = geometries[0];
			AbstractGeometry comp = geometries[1];
			boolean needsUpdate = !degreeProcessed
					|| (inter != null && inter.isType(GeometryType.DYNAMIC))
					|| (comp != null && comp.isType(GeometryType.DYNAMIC));
			double[][] graphdata = graph.getData();
			if (!needsUpdate || graphdata == null || inter == null || comp == null || data == graphdata)
				continue;
			// initialize or reuse data buffer and compute histogram
			data = ensureDegreeData(graph, inter, comp, graphdata);
		}
	}

	/**
	 * Handle messaging for DE/PDE models; returns true if a message was set on the
	 * graph and further processing should be skipped.
	 * 
	 * @param graph  target histogram
	 * @param module module owning the histogram
	 * @return {@code true} if the graph now displays a message
	 */
	private boolean handleDEGraph(HistoGraph graph, Module<?> module) {
		if (model.isPDE()) {
			AbstractGeometry inter = module.getGeometry();
			if (inter.isRegular()) {
				graph.displayMessage("PDE model: regular structure with degree "
						+ (int) (inter.getConnectivity() + 0.5) + ".");
			} else if (inter.isLattice()) {
				graph.displayMessage("PDE model: lattice structure with degree "
						+ (int) (inter.getConnectivity() + 0.5) +
						(inter.isRegular() ? " (periodic)" : " (fixed)") + " boundaries).");
			}
		} else {
			graph.displayMessage(model.getType().getKey() + " model: well-mixed population.");
		}
		return graph.hasMessage();
	}

	/**
	 * Ensure the provided data buffer is sized appropriately for the given
	 * interaction/competition geometries, update the histogram data and set axis
	 * limits on the graph; returns the (possibly new) data buffer.
	 * 
	 * @param graph target histogram
	 * @param inter interaction geometry
	 * @param comp  competition geometry
	 * @param data  current data buffer
	 * @return data buffer ready to be reused by the histogram
	 */
	private double[][] ensureDegreeData(HistoGraph graph, AbstractGeometry inter, AbstractGeometry comp,
			double[][] data) {
		int kmax = maxDegree(inter, comp);
		double min = 0.0;
		double max = kmax;
		int bincount = Math.min(kmax, HistoGraph.DEFAULT_BINS);
		if (bincount != data[0].length) {
			data = new double[data.length][bincount];
			graph.setData(data);
		}
		getDegreeHistogramData(data, inter, comp);
		GraphStyle style = graph.getStyle();
		style.xMin = min;
		style.xMax = max;
		return data;
	}

	/**
	 * Update stationary statistics histograms.
	 */
	private void updateStationary() {
		int nt = model.getNMean();
		double[] state = new double[nt];
		model.getMeanTraits(state);
		int idx = 0;
		for (HistoGraph graph : graphs)
			graph.addData((int) (state[idx++] * scale2bins + BIN_EPSILON));
	}

	/**
	 * Helper method to check whether to show the fixation time distribution
	 * or the fixation times for each node.
	 * 
	 * @param module the module of the graph
	 * @return {@code true} to show the fixation time distribution
	 */
	private boolean doFixtimeDistr(Module<?> module) {
		if (model.isSDE())
			return true;
		int maxBins = graphs.get(0).getMaxBins();
		return (maxBins > 0 && module.getNPopulation() > maxBins);
	}

	/**
	 * The status of the view.
	 */
	protected String status;

	@Override
	public String getStatus(boolean force) {
		// status calculation are somewhat costly - throttle the max number of updates
		if (!doUpdateStatus(force))
			return status;
		// multi-species not supported for statistics
		// clo changes may trigger reset before view has been loaded
		if (isMultispecies || !isLoaded)
			return null;

		switch (type) {
			case STATISTICS_FIXATION_PROBABILITY:
				status = getFixationProbabilityStatus();
				return status;

			case STATISTICS_FIXATION_TIME:
				status = getFixationTimeStatus();
				return status;

			case STATISTICS_STATIONARY:
				if (model.hasConverged()) {
					engine.logWarning("Non ergodic system - no stationary distribution!");
					return "";
				}
				//$FALL-THROUGH$
			default:
				return super.getStatus(force);
		}
	}

	/**
	 * Build status string for fixation probabilities.
	 * 
	 * @return formatted status text
	 */
	private String getFixationProbabilityStatus() {
		int nSamp = model.getNStatisticsSamples();
		StringBuilder sb = new StringBuilder("Avg. fix. prob: ");
		int idx = 0;
		for (HistoGraph graph : graphs) {
			Module<?> module = graph.getModule();
			if (idx > 0)
				sb.append(", ");
			sb.append(isMultispecies ? module.getName() + "." : "")
					.append(module.getTraitName(idx++))
					.append(": ");
			if (nSamp <= 0)
				sb.append("-");
			else
				sb.append(Formatter.formatFix(graph.getSamples() / nSamp, 4));
		}
		return sb.toString();
	}

	/**
	 * Build status string for fixation times.
	 * 
	 * @return formatted status text
	 */
	private String getFixationTimeStatus() {
		StringBuilder sb = new StringBuilder("Avg. fix. time: ");
		int idx = 0;
		for (HistoGraph graph : graphs) {
			double[][] data = graph.getData();
			if (data == null) {
				engine.logWarning("Average fixation times not available!");
				return "";
			}
			Module<?> module = graph.getModule();
			int nTraits = module.getNTraits();
			if (idx > 0)
				sb.append(", ");
			sb.append(isMultispecies ? module.getName() + "." : "")
					.append(graph.getStyle().label)
					.append(": ");
			int skip = module.getVacantIdx();
			if (skip >= 0 &&
					skip == idx &&
					module instanceof Discrete &&
					((Discrete) module).getMonoStop()) {
				idx++;
			}
			boolean isDistribution = doFixtimeDistr(module);
			if (isDistribution)
				sb.append(graph.getSamples() <= 0.0 ? "0.00 ± 0.00"
						: formatDistributionMean(data[idx], graph.getStyle()));
			else
				sb.append(graph.getSamples() <= 0.0 ? "0.0000 ± 0.0000"
						: formatWeightedMean(data[idx], data[idx + nTraits + 1]));
			idx++;
		}
		return sb.toString();
	}

	/**
	 * Format mean ± sdev for a distribution stored in data (assumed normalized
	 * counts).
	 * 
	 * @param dist  the distribution data
	 * @param style the graph style for scaling
	 * @return the formatted string
	 */
	private String formatDistributionMean(double[] dist, GraphStyle style) {
		double mean = Distributions.distrMean(dist);
		double sdev = Distributions.distrStdev(dist, mean);
		return Formatter.formatFix(style.xMin + mean * (style.xMax - style.xMin), 2)
				+ " ± " + Formatter.formatFix(sdev * (style.xMax - style.xMin), 2);
	}

	/**
	 * Compute weighted mean ± sdev from node-wise times (dat) and sample counts
	 * (sam).
	 * 
	 * @param dat the data array
	 * @param sam the sample counts
	 * @return the formatted string
	 */
	private String formatWeightedMean(double[] dat, double[] sam) {
		double sx = 0.0;
		double sx2 = 0.0;
		double sw = 0.0;
		int nDat = dat.length;
		for (int n = 0; n < nDat; n++) {
			double w = sam[n];
			if (w <= 0.0)
				continue;
			double x = dat[n];
			sx += x;
			sx2 += x * x / w;
			sw += w;
		}
		if (sw <= 0.0)
			return "0.0000 ± 0.0000";
		double mean = sx / sw;
		double sdev = Math.sqrt(sx2 / sw - mean * mean);
		return Formatter.formatFix(mean, 4) + " ± " + Formatter.formatFix(sdev, 4);
	}

	/**
	 * Calculate the maximum degree for displaying the degree distribution. This
	 * rounds {@code max} up to {@code 10, 20, 50, 100, 200, 500, 1000} etc. The
	 * result is always strictly larger than {@code max}, i.e. the degrees
	 * {@code 0, 1, ..., max} all fall inside the range {@code [0, maxDegree)}.
	 *
	 * @param max the maximum degree of the graph
	 * @return the maximum degree to display
	 */
	private static int maxDegree(int max) {
		// determine range and number of bins required
		// range starts at 0 up to 10, 20, 50, 100, 200, 500, 1000 etc.
		// the number of bins is at most 100
		int order = (int) Math.max(10, Combinatorics.pow(10, (int) Math.floor(Math.log10(max + 1.0))));
		switch (max / order) {
			default:
			case 0:
				return order;
			case 1:
				return 2 * order;
			case 2:
			case 3:
			case 4:
				return 5 * order;
			case 5:
			case 6:
			case 7:
			case 8:
			case 9:
				return 10 * order;
		}
	}

	/**
	 * Calculate the maximum degree for displaying the degree distributions of the
	 * interaction and competition geometries. For directed graphs the total degree
	 * determines the range because the in-, out- and total degree distributions
	 * share the same axis.
	 *
	 * @param inter the interaction graph
	 * @param comp  the competition graph
	 * @return the maximum degree to display
	 *
	 * @see #maxDegree(int)
	 */
	private static int maxDegree(AbstractGeometry inter, AbstractGeometry comp) {
		GeometryFeatures interFeatures = inter.getFeatures();
		int max = inter.isUndirected() ? interFeatures.maxOut : interFeatures.maxTot;
		if (inter != comp) {
			GeometryFeatures compFeatures = comp.getFeatures();
			max = Math.max(max, comp.isUndirected() ? compFeatures.maxOut : compFeatures.maxTot);
		}
		return maxDegree(max);
	}

	/**
	 * Add the node with degree {@code k} to the corresponding bin.
	 *
	 * @param bins      the degree distribution
	 * @param k         the degree of the node
	 * @param ibinwidth the scaling factor to map degrees to bins
	 */
	private static void binDegree(double[] bins, int k, double ibinwidth) {
		// guard against stale degree ranges of dynamic geometries
		bins[Math.min((int) (k * ibinwidth), bins.length - 1)]++;
	}

	/**
	 * Generate the degree histogram data for the given interaction and competition
	 * geometries.
	 * 
	 * @param data  the data array for storing the histograms
	 * @param inter the interaction graph
	 * @param comp  the competition graph
	 */
	private void getDegreeHistogramData(double[][] data, AbstractGeometry inter, AbstractGeometry comp) {
		// the x-axis covers degrees [0, kmax) such that the first bin holds the
		// isolated nodes with degree zero
		double ibinwidth = (double) data[0].length / maxDegree(inter, comp);
		getDegreeHistogramData(data, inter, 0, ibinwidth);
		if (inter != comp)
			getDegreeHistogramData(data, comp, inter.isUndirected() ? 1 : 3, ibinwidth);
		degreeProcessed = true;
	}

	/**
	 * Generate the degree histogram data for the given geometry.
	 * 
	 * @param data      the data array for storing the histograms
	 * @param geometry  the interaction graph
	 * @param idx       the index for placing the histogram data
	 * @param ibinwidth the scaling factor to map degrees to bins
	 */
	private void getDegreeHistogramData(double[][] data, AbstractGeometry geometry, int idx, double ibinwidth) {
		int nodeCount = geometry.getSize();
		if (geometry.isUndirected()) {
			double[] dataio = data[idx];
			Arrays.fill(dataio, 0.0);
			for (int i = 0; i < nodeCount; i++)
				binDegree(dataio, geometry.kin[i], ibinwidth);
			ArrayMath.multiply(dataio, 1.0 / nodeCount);
			return;
		}
		double[] datao = data[idx];
		double[] datai = data[idx + 1];
		double[] datat = data[idx + 2];
		Arrays.fill(datao, 0.0);
		Arrays.fill(datai, 0.0);
		Arrays.fill(datat, 0.0);
		for (int i = 0; i < nodeCount; i++) {
			int kin = geometry.kin[i];
			int kout = geometry.kout[i];
			binDegree(datao, kout, ibinwidth);
			binDegree(datai, kin, ibinwidth);
			binDegree(datat, kin + kout, ibinwidth);
		}
		double norm = 1.0 / nodeCount;
		ArrayMath.multiply(datao, norm);
		ArrayMath.multiply(datai, norm);
		ArrayMath.multiply(datat, norm);
	}

	/**
	 * Determine the number of histograms required for the degree distributions of
	 * the interaction and competition geometries.
	 * 
	 * @param inter the interaction geometry
	 * @param comp  the competition geometry
	 * @return the number of histograms required
	 */
	private int getDegreeGraphs(AbstractGeometry inter, AbstractGeometry comp) {
		if (inter == null)
			return 1;
		int nGraphs = 1;
		if (!inter.isUndirected())
			nGraphs += 2;
		if (comp != inter) {
			nGraphs += 1;
			if (!comp.isUndirected())
				nGraphs += 2;
		}
		return nGraphs;
	}

	/**
	 * Determine the number of bins required for the degree distributions of the
	 * interaction and competition geometries.
	 * 
	 * @param inter the interaction geometry
	 * @param comp  the competition geometry
	 * @return the number of histograms required
	 */
	private int getDegreeBins(AbstractGeometry inter, AbstractGeometry comp) {
		if (inter == null)
			return 0;
		return Math.min(maxDegree(inter, comp), HistoGraph.DEFAULT_BINS);
	}

	/**
	 * Get the labels for the degree distributions of the interaction and
	 * competition geometries.
	 * 
	 * @param nTraits         the number of traits
	 * @param interUndirected {@code true} if the interaction graph is undirected
	 * @return the labels for the degree distributions
	 */
	private String[] getDegreeLabels(int nTraits, boolean interUndirected) {
		switch (nTraits) {
			case 1:
				// single graph, undirected
				return new String[] { null };
			case 2:
				// separate graphs, undirected
				return new String[] { "interaction", "competition" };
			case 3:
				// single directed graph
				return new String[] { "in", "out", "total" };
			case 4:
				// separate graphs, one directed the other undirected
				if (interUndirected)
					return new String[] { "interaction", "competition - in", "competition - out",
							"competition - total" };
				return new String[] { "interaction - in", "interaction - out", "interaction - total", "competition" };
			case 6:
				return new String[] { "interaction - in", "interaction - out", "interaction - total",
						"competition - in", "competition - out", "competition - total" };
			default:
				// should not happen...
				throw new IllegalArgumentException("unvalid number of degree distributions");
		}
	}

	/**
	 * Populate context menu with histogram-specific actions.
	 *
	 * @param menu the context menu to populate
	 */
	@Override
	public void populateContextMenu(ContextMenu menu) {
		if (hasHistogramBinsMenu()) {
			addHistogramBinsMenu(menu);
		} else if (type == Data.STATISTICS_STATIONARY) {
			// add menu to clear canvas
			ContextMenuItem clearMenu = new ContextMenuItem("Clear", () -> {
				for (HistoGraph graph : graphs)
					graph.clearData();
				timestamp = -1.0;
				update();
			});
			menu.addSeparator();
			menu.add(clearMenu);
		}
		super.populateContextMenu(menu);
	}

	/**
	 * Check whether the histogram-bins submenu is applicable.
	 *
	 * @return {@code true} if current histograms support manual bin selection
	 */
	private boolean hasHistogramBinsMenu() {
		if (graphs.isEmpty())
			return false;
		if (type == Data.TRAIT)
			return model != null && model.isContinuous();
		return type == Data.FITNESS;
	}

	/**
	 * Add submenu for selecting the number of bins in trait/fitness histograms.
	 *
	 * @param menu context menu to populate
	 */
	private void addHistogramBinsMenu(ContextMenu menu) {
		int[] binCounts = getBinCounts();
		if (binCounts.length == 0)
			return;
		int selected = Math.min(Math.max(histogramBins, binCounts[0]), binCounts[binCounts.length - 1]);
		ContextMenu binsMenu = new ContextMenu(menu);
		for (int bins : binCounts) {
			ContextMenuCheckBoxItem item = new ContextMenuCheckBoxItem(Integer.toString(bins),
					() -> applyHistogramBins(bins));
			item.setChecked(bins == selected);
			binsMenu.add(item);
		}
		menu.addSeparator();
		ContextMenuItem binsTrigger = menu.add("Bins", binsMenu);
		binsTrigger.setEnabled(!isRunning());
	}

	/**
	 * Return the feasible bin counts for trait/fitness histogram menus.
	 * <p>
	 * Feasible entries are the prefix of {@link #HISTOGRAM_BIN_OPTIONS} that fit
	 * the current graph widths.
	 *
	 * @return feasible bin counts
	 */
	public int[] getBinCounts() {
		if (graphs.isEmpty())
			return HISTOGRAM_BIN_OPTIONS;
		int maxFeasible = HISTOGRAM_BIN_OPTIONS[HISTOGRAM_BIN_OPTIONS.length - 1];
		for (HistoGraph graph : graphs) {
			int max = graph.getMaxBins();
			if (max > 0)
				maxFeasible = Math.min(maxFeasible, max);
		}
		int count = 0;
		for (int option : HISTOGRAM_BIN_OPTIONS) {
			if (option > maxFeasible)
				break;
			count++;
		}
		return Arrays.copyOf(HISTOGRAM_BIN_OPTIONS, count);
	}

	/**
	 * Apply a new number of bins for trait/fitness histograms.
	 *
	 * @param newBins requested number of bins
	 */
	private void applyHistogramBins(int newBins) {
		int[] binCounts = getBinCounts();
		if (binCounts.length == 0)
			return;
		int clamped = binCounts[0];
		for (int bins : binCounts) {
			if (bins > newBins)
				break;
			clamped = bins;
		}
		if (clamped == histogramBins)
			return;
		histogramBins = clamped;
		reset(true);
	}

	@Override
	protected ExportType[] exportTypes() {
		switch (type) {
			case STATISTICS_FIXATION_PROBABILITY:
			case STATISTICS_FIXATION_TIME:
			case STATISTICS_STATIONARY:
				return new ExportType[] { ExportType.SVG, ExportType.PNG, ExportType.CSV_STAT };
			case DEGREE:
			default:
				return new ExportType[] { ExportType.SVG, ExportType.PNG };
		}
	}

	@Override
	protected void exportStatData() {
		// NOTE: consider exporting more data (e.g. overall fix probs/times, sdev);
		// initial configuration for statistics, structure
		StringBuilder export = new StringBuilder();
		switch (type) {
			case STATISTICS_FIXATION_PROBABILITY:
				export.append("# fixation probability\n");
				break;
			case STATISTICS_FIXATION_TIME:
				export.append("# fixation time\n");
				break;
			case STATISTICS_STATIONARY:
				export.append("# stationary distribution\n");
				break;
			default:
				return;
		}
		Module<?> module = null;
		int idx = -1;
		for (HistoGraph graph : graphs) {
			Module<?> newMod = graph.getModule();
			idx++;
			if (module != newMod) {
				module = newMod;
				if (isMultispecies)
					export.append("# species: ").append(module.getName()).append("\n");
			}
			export.append("# trait: ").append(model.getMeanName(idx)).append("\n");
			double[][] data = graph.getData();
			if (data == null) {
				export.append("# no data available\n");
				continue;
			}
			int nData = data[idx].length;
			GraphStyle gStyle = graph.getStyle();
			double binWidth = (gStyle.xMax - gStyle.xMin) / nData;
			boolean hasRange = binWidth > 1.0 + 1e-9;
			export.append(
					hasRange ? "# bin,\tvalue,\tsamples,\tdata,\tfrom,\tto\n" : "# bin,\tvalue,\tsamples,\tdata\n");
			for (int n = 0; n < nData; n++) {
				export.append(n).append(",\t")
						.append(graph.getData(n)).append(",\t")
						.append((long) graph.getSamples(n));
				if (hasRange)
					export.append(",\t").append(Math.round(gStyle.xMin + n * binWidth)).append(",\t")
							.append(n == nData - 1 ? Math.round(gStyle.xMax)
									: Math.round(gStyle.xMin + (n + 1) * binWidth) - 1);
				export.append("\n");
			}
		}
		NativeJS.export("data:text/csv;base64," + NativeJS.b64encode(export.toString()), "evoludo_stat.csv");
	}
}
