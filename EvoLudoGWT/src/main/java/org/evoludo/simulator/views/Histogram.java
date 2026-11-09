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

import static org.evoludo.simulator.views.BasicTooltipProvider.tableRow;

import java.util.List;

import org.evoludo.graphics.GraphStyle;
import org.evoludo.graphics.HistoGraph;
import org.evoludo.simulator.ColorMapCSS;
import org.evoludo.simulator.EvoLudoGWT;
import org.evoludo.simulator.models.Model;
import org.evoludo.simulator.modules.Module;
import org.evoludo.util.Formatter;
import org.evoludo.util.NativeJS;

/**
 * Base class of the views of histograms. The view stacks one {@link HistoGraph}
 * per data row, typically one per trait, for all species. The subclasses
 * determine the rows, the data and the style of the graphs:
 * <ul>
 * <li>{@link TraitHistogram} - distribution of continuous traits</li>
 * <li>{@link FitnessHistogram} - distribution of payoffs</li>
 * <li>{@link DegreeHistogram} - degree distributions of the interaction and
 * competition structures</li>
 * <li>{@link FixationHistogram} - fixation probabilities and times</li>
 * <li>{@link StationaryHistogram} - stationary distribution</li>
 * </ul>
 * The graphs of one species share their data, a {@code double[][]} array with
 * one row per graph, see {@link HistoGraph#getRow()}.
 *
 * @author Christoph Hauert
 *
 * @see HistoGraph
 */
public abstract class Histogram extends AbstractView<HistoGraph> implements BasicTooltipProvider {

	/**
	 * Small epsilon to avoid floating point truncation when mapping values to bins.
	 */
	protected static final double BIN_EPSILON = 1e-12;

	/**
	 * Flag to indicate whether the model entertains multiple species, i.e.
	 * {@code nSpecies&gt;1}. Convenience field. Reduces calls to {@link Module}.
	 */
	protected boolean isMultispecies;

	/**
	 * Construct a new view to display histograms.
	 *
	 * @param engine the pacemaker for running the model
	 */
	protected Histogram(EvoLudoGWT engine) {
		super(engine);
	}

	/**
	 * Get the data rows of the graphs for {@code module}, typically the indices
	 * of the traits.
	 *
	 * @param module the module
	 * @return the data rows of the graphs
	 */
	protected abstract int[] getGraphRows(Module<?> module);

	/**
	 * Reset {@code graph}, e.g. its markers, normalization and binning.
	 *
	 * @param graph the graph to reset
	 * @param idx   the index of the graph across species
	 * @return the number of rows and columns of the data, or {@code null} if the
	 *         graph shows no data
	 */
	protected abstract int[] resetGraph(HistoGraph graph, int idx);

	/**
	 * Apply the style settings specific to the data type to {@code graph}.
	 *
	 * @param graph the graph to style
	 * @return {@code true} if hard reset is required
	 */
	protected abstract boolean applyStyle(HistoGraph graph);

	/**
	 * Retrieve the current data from the model. Called by {@link #update(boolean)}
	 * if time has advanced. Statistics that accumulate samples do nothing here.
	 */
	protected void updateGraphs() {
	}

	/**
	 * Check whether the {@code y}-axis of new graphs is autoscaled.
	 *
	 * @return {@code true} to autoscale the {@code y}-axis
	 */
	protected boolean isAutoscaleY() {
		return true;
	}

	@Override
	protected boolean allocateGraphs() {
		int nGraphs = countGraphs();
		if (graphs.size() == nGraphs)
			return false;
		destroyGraphs();
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
	 * Count how many graphs are required for all species.
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
	 * Get the rows {@code 0, ..., n - 1} except {@code skip}.
	 *
	 * @param n    the number of rows
	 * @param skip the row to skip or {@code -1}
	 * @return the rows
	 */
	protected static int[] rows(int n, int skip) {
		int[] rows = new int[skip >= 0 && skip < n ? n - 1 : n];
		int i = 0;
		for (int r = 0; r < n; r++) {
			if (r != skip)
				rows[i++] = r;
		}
		return rows;
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
				// settings that users can change through the context menu, such as the
				// range and autoscaling of the y-axis, are set here only
				GraphStyle style = graph.getStyle();
				style.yLabel = "frequency";
				style.percentY = true;
				style.autoscaleY = isAutoscaleY();
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
				style.showXLabel = (++idx == nGraphs);
				style.showXTickLabels = style.showXLabel || row == rows[rows.length - 1];
				applyStyle(graph);
			}
		}
	}

	/**
	 * Set the label and color of {@code graph} to those of its trait.
	 *
	 * @param graph the graph to style
	 */
	protected void applyTraitLabel(HistoGraph graph) {
		GraphStyle style = graph.getStyle();
		Module<?> module = graph.getModule();
		int row = graph.getRow();
		style.label = (isMultispecies ? module.getName() + ": " : "") + module.getTraitName(row);
		style.graphColor = ColorMapCSS.Color2Css(module.getTraitColors()[row]);
	}

	/**
	 * Get the tooltip for bin {@code bar} of {@code graph}. By default, this shows
	 * the range of the bin, its value and the notes of markers in the bin.
	 * 
	 * @param graph the graph
	 * @param bar   the index of the bin
	 * @return the tooltip
	 * 
	 * @see #getRangeTooltip(HistoGraph, int)
	 */
	public String getTooltipAt(HistoGraph graph, int bar) {
		return getRangeTooltip(graph, bar);
	}

	/**
	 * Get the tooltip for bin {@code bar} of {@code graph} with the range of the
	 * bin, its value and the notes of markers in the bin.
	 * 
	 * @param graph the graph
	 * @param bar   the index of the bin
	 * @return the tooltip
	 */
	protected static String getRangeTooltip(HistoGraph graph, int bar) {
		GraphStyle style = graph.getStyle();
		double width = (style.xMax - style.xMin) / graph.getNBins();
		String note = graph.getNoteAt(bar);
		return TABLE_STYLE
				+ tableRow(style.xLabel, "[" + Formatter.format(style.xMin + bar * width, 2) + ", "
						+ Formatter.format(style.xMin + (bar + 1) * width, 2) + ")")
				+ tooltipValue(graph, bar)
				+ (note == null ? "" : tableRow("Note", note)) + TABLE_END;
	}

	/**
	 * Get the tooltip for bin {@code bar} of {@code graph} with integer values
	 * {@code 0, 1, ..., max}, where value {@code v < n} maps to bin
	 * {@code floor(v nBins / n)} and the last bin includes all values up to
	 * {@code max}.
	 * 
	 * @param graph   the graph
	 * @param bar     the index of the bin
	 * @param n       the number of values spread evenly across bins
	 * @param max     the largest value
	 * @param samples {@code true} to include the number of samples of the bin
	 * @return the tooltip
	 */
	protected static String getIntTooltip(HistoGraph graph, int bar, int n, int max, boolean samples) {
		int nBins = graph.getNBins();
		int first = (bar * n + nBins - 1) / nBins;
		int last = (bar == nBins - 1 ? max : ((bar + 1) * n + nBins - 1) / nBins - 1);
		return TABLE_STYLE
				+ tableRow(graph.getStyle().xLabel, last > first ? "[" + first + "-" + last + "]" : first)
				+ (samples ? tableRow("samples", (long) graph.getSamples(bar)) : "")
				+ tooltipValue(graph, bar) + TABLE_END;
	}

	/**
	 * Get the tooltip row for the value of bin {@code bar} of {@code graph}.
	 * 
	 * @param graph the graph
	 * @param bar   the index of the bin
	 * @return the tooltip row
	 */
	private static String tooltipValue(HistoGraph graph, int bar) {
		GraphStyle style = graph.getStyle();
		double value = graph.getData(bar);
		return tableRow(style.yLabel,
				style.percentY ? Formatter.formatPercent(value, 2) : Formatter.format(value, 2));
	}

	@Override
	public void reset(boolean hard) {
		super.reset(hard);
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

	@Override
	public void modelSettings() {
		// check if settings changed the number of graphs
		if (graphs.size() != countGraphs())
			reset(true);
	}

	@Override
	protected void onModelDidInit() {
		for (HistoGraph graph : graphs)
			graph.init();
		update();
	}

	@Override
	public void update(boolean force) {
		if (!isActive)
			return;
		double newtime = model.getUpdates();
		if (Math.abs(timestamp - newtime) > Model.EPS) {
			timestamp = newtime;
			updateGraphs();
		}
		for (HistoGraph graph : graphs)
			graph.paint(force);
	}

	/**
	 * Get the name of the data shown, i.e. the name of the view without its
	 * category, in lower case.
	 * 
	 * @return the name of the data
	 */
	protected String getDataName() {
		String name = getName();
		return name.substring(name.indexOf("- ") + 2).toLowerCase();
	}

	@Override
	protected ExportType[] exportTypes() {
		if (getMode().isStatistics())
			return new ExportType[] { ExportType.SVG, ExportType.PNG, ExportType.CSV_STAT };
		return new ExportType[] { ExportType.SVG, ExportType.PNG };
	}

	@Override
	protected void exportStatData() {
		// NOTE: consider exporting more data (e.g. overall fix probs/times, sdev);
		// initial configuration for statistics, structure
		StringBuilder export = new StringBuilder("# ").append(getDataName()).append("\n");
		Module<?> module = null;
		for (HistoGraph graph : graphs) {
			if (module != graph.getModule()) {
				module = graph.getModule();
				if (isMultispecies)
					export.append("# species: ").append(module.getName()).append("\n");
			}
			GraphStyle gStyle = graph.getStyle();
			export.append("# trait: ").append(gStyle.label).append("\n");
			double[][] data = graph.getData();
			if (data == null) {
				export.append("# no data available\n");
				continue;
			}
			int nData = data[0].length;
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
