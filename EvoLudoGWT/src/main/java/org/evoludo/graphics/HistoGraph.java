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

package org.evoludo.graphics;

import java.util.ArrayList;
import java.util.Arrays;

import org.evoludo.ui.ContextMenu;
import org.evoludo.ui.ContextMenuCheckBoxItem;
import org.evoludo.util.Formatter;
import org.evoludo.math.ArrayMath;
import org.evoludo.math.Functions;
import org.evoludo.simulator.modules.Module;
import org.evoludo.simulator.views.BasicTooltipProvider;

import com.google.gwt.user.client.Command;

/**
 * Histogram graph for displaying data in bins. The data is stored in a 2D array
 * with the first index representing the data row and the second index the bin
 * index. The data can be normalized to the total number of samples or to a
 * specific data row. The graph can be used to display data for different types
 * of modules, such as degree distributions, strategies, fitness values, or
 * fixation probabilities.
 * 
 * @author Christoph Hauert
 */
public class HistoGraph extends AbstractGraph<double[]> implements BasicTooltipProvider {

	/**
	 * Marker for highlighting a specific bin in the histogram. The marker is
	 * defined by its {@code x}-coordinate, color, and description. The linestyle
	 * can be set to {@code null} for a solid line or to an array of integers
	 * defining the dash pattern.
	 */
	public class Marker {

		/**
		 * The {@code x}-coordinate of the marker.
		 */
		double x;

		/**
		 * The index of the marked bin.
		 */
		int bin = -1;

		/**
		 * The color of the marker.
		 */
		String color;

		/**
		 * The description of the marker. This is shown on the tolltip when hovering
		 * over the marked bin.
		 */
		String descr;

		/**
		 * The linestyle of the marker. This can be set to {@code null} for a solid line
		 * or to an array of integers defining the dash pattern.
		 */
		int[] linestyle;

		/**
		 * Create a new marker for the histogram at {@code x} with color {@code color}
		 * and description {@code descr}.
		 * 
		 * @param x     the {@code x}-coordinate of the marker
		 * @param color the color of the marker
		 * @param descr the description of the marker
		 */
		public Marker(double x, String color, String descr) {
			this(x, color, descr, null);
		}

		/**
		 * Create a new marker for the histogram at {@code x} with color {@code color},
		 * description {@code descr}, and linestyle {@code linestyle}.
		 * 
		 * @param x         the {@code x}-coordinate of the marker
		 * @param color     the color of the marker
		 * @param descr     the description of the marker
		 * @param linestyle the linestyle of the marker
		 */
		public Marker(double x, String color, String descr, int[] linestyle) {
			this.x = x;
			this.color = color;
			this.descr = descr;
			this.linestyle = linestyle;
		}
	}

	/**
	 * Add marker to histogram at {@code x} with color {@code color} and description
	 * {@code descr}.
	 * 
	 * @param x     the {@code x}-coordinate of the marker
	 * @param color the color of the marker
	 * @param descr the description of the marker
	 */
	public void addMarker(double x, String color, String descr) {
		binmarkers.add(new Marker(x, color, descr));
	}

	/**
	 * Add marker to histogram at {@code x} with color {@code color}, description
	 * {@code descr}, and linestyle {@code linestyle}.
	 * 
	 * @param x         the {@code x}-coordinate of the marker
	 * @param color     the color of the marker
	 * @param descr     the description of the marker
	 * @param linestyle the linestyle of the marker
	 */
	public void addMarker(double x, String color, String descr, int[] linestyle) {
		binmarkers.add(new Marker(x, color, descr, linestyle));
	}

	/**
	 * Clear all markers from the histogram.
	 */
	public void clearMarkers() {
		binmarkers.clear();
	}

	/**
	 * Get the description of the marker at bin {@code bin} or {@code null} if no
	 * marker is set.
	 * 
	 * @param bin the index of the bin
	 * @return the description of the marker
	 */
	public String getNoteAt(int bin) {
		for (Marker mark : binmarkers)
			if (mark.bin == bin)
				return mark.descr;
		return null;
	}

	/**
	 * The list of thresholds for automatically scaling the y-axis. The first
	 * element of each row is the maximum value for the y-axis, the second element
	 * is the minimum value for the y-axis, and the third element is the number of
	 * levels for the y-axis.
	 * <p>
	 * For example, the scale changes from {@code 0-1} with 4 levels to {@code
	 * 0-0.5} with 5 levels if the maximum value drops below {@code 0.4}.
	 */
	private static final double[][] autoscale = new double[][] { //
			{ 1.0, 0.4, 4.0 }, //
			{ 0.5, 0.2, 5.0 }, //
			{ 0.25, 0.08, 5.0 }, //
			{ 0.1, 0.04, 5.0 }, //
			{ 0.05, 0.008, 5.0 }, //
			{ 0.01, 0.0, 5.0 } };

	/**
	 * The index of the current autoscale setting.
	 */
	private int autoscaleidx = 0;

	/**
	 * The list of markers for the histogram.
	 */
	private ArrayList<Marker> binmarkers = new ArrayList<Marker>();

	/**
	 * The data array backing the histogram. This may be shared by multiple
	 * {@code HistoGraph}s, each accessing a different row in the array.
	 */
	double[][] data;

	/**
	 * The index of the data row.
	 */
	int row;

	/**
	 * The message to display if no histogram is available.
	 */
	String message;

	/**
	 * The flag to indicate whether the data is normalized.
	 */
	private boolean isNormalized = true;

	/**
	 * The index of the data row used for normalization.
	 */
	private int normIdx = -1;

	/**
	 * The number of samples in the histogram.
	 */
	double nSamples;

	/**
	 * The number of bins.
	 * 
	 * @evoludo.impl The number of bins needs to be even for doubling the range of
	 *               the histogram.
	 * 
	 * @see #doubleMinRange()
	 * @see #doubleMaxRange()
	 */
	int nBins;

	/**
	 * The maximum number of bins for the histogram.
	 */
	public static final int MAX_BINS = 100;

	/**
	 * The minimum width of a bin.
	 */
	public static final int MIN_BIN_WIDTH = 1;

	/**
	 * Create new histogram graph for <code>module</code> running in
	 * <code>controller</code>. The row is used to identify data entries that apply
	 * to this histogram and represents the index of the data row.
	 * 
	 * @param controller the controller of this graph
	 * @param module     the module backing the graph
	 * @param row        the index of the data row
	 */
	public HistoGraph(Controller controller, Module module, int row) {
		super(controller, module);
		this.row = row;
		setStylePrimaryName("evoludo-HistoGraph");
		setTooltipProvider(this);
	}

	/**
	 * Calculate the maximum number of bins for the histogram with a width of at
	 * least {@code MIN_BIN_WIDTH}.
	 * 
	 * @return the maximum number of bins
	 * 
	 * @see #calcBounds()
	 */
	public int getMaxBins() {
		return (int) (bounds.getWidth() / (MIN_BIN_WIDTH + 1));
	}

	/**
	 * Set whether the data is normalized. If {@code isNormalized} is {@code true},
	 * the sum of the data in each bin is normalized to {@code 1.0}.
	 * <br>
	 * <strong>Note:</strong> This conflicts with {@link #setNormalized(int)}.
	 * 
	 * @param isNormalized {@code true} if the data is normalized, {@code false}
	 *                     otherwise
	 */
	public void setNormalized(boolean isNormalized) {
		this.isNormalized = isNormalized;
		if (isNormalized)
			normIdx = -1;
	}

	/**
	 * Set the index of the data row that is used for normalization. If multiple
	 * {@code HistoGraph}s share the same data, they all need to use the same row
	 * for normalization.
	 * <br>
	 * <strong>Note:</strong> This conflicts with {@link #setNormalized(boolean)}.
	 * 
	 * @param normIdx the index of the data row for normalization
	 */
	public void setNormalized(int normIdx) {
		this.normIdx = normIdx;
	}

	/**
	 * Get the number of samples for this histogram.
	 * 
	 * @return the number of samples
	 */
	public double getSamples() {
		return nSamples;
	}

	/**
	 * Get the total number of samples in bin with index {@code idx}. For data that
	 * is not normalized the number of samples is returned.
	 * 
	 * @param idx the index of the bin
	 * @return the number of samples
	 * 
	 * @see #getSamples()
	 */
	public double getSamples(int idx) {
		if (data != null && normIdx >= 0)
			return data[normIdx][idx];
		return nSamples;
	}

	/**
	 * Get the data for the histogram.
	 * <br>
	 * <strong>Note:</strong> The data may be shared by multiple
	 * {@code HistoGraph}s, each refering to a row in the {@code double[][]} array
	 * specified by the graphs {@code row}.
	 * 
	 * @return the 2D data array for storing the histogram data
	 */
	public double[][] getData() {
		return data;
	}

	/**
	 * Get the data for the histogram in bin with index {@code idx}. Whether this
	 * returns raw or normalized data depends on the setting of the graph.
	 * 
	 * @param idx the index of the bin
	 * @return the histogram data for the bin {@code idx}
	 * 
	 * @see #setNormalized(boolean)
	 * @see #setNormalized(int)
	 */
	public double getData(int idx) {
		if (normIdx >= 0)
			return data[row][idx] / Math.max(1.0, data[normIdx][idx]);
		if (!isNormalized)
			return data[row][idx] / Math.max(1.0, nSamples);
		return data[row][idx];
	}

	/**
	 * Set the data for the histogram.
	 * <br>
	 * <strong>Note:</strong> The data may be shared by multiple
	 * {@code HistoGraph}s, each refering to a row in the {@code double[][]} array
	 * specified by the graphs {@code row}.
	 * 
	 * @param data the 2D data array for storing the histogram data
	 */
	public void setData(double[][] data) {
		this.data = data;
		if (data != null && data.length > 0 && data[0] != null)
			nBins = data[0].length;
		else
			nBins = -1;
	}

	/**
	 * Add data point to histogram by increasing the count of the bin with index
	 * {@code bin} by one. For normalized data the normalization is updated
	 * accordingly.
	 * 
	 * @param bin the index of the bin in the histogram
	 * 
	 * @see #setNormalized(boolean)
	 * @see #setNormalized(int)
	 */
	public void addData(int bin) {
		nSamples++;
		if (data == null || bin < 0)
			return;
		if (bin >= nBins)
			bin = nBins - 1;
		data[row][bin]++;
		if (normIdx >= 0)
			data[normIdx][bin]++;
	}

	/**
	 * Add data point to histogram by increasing the count of the bin with index
	 * {@code bin} by {@code incr}. For normalized data the normalization is updated
	 * accordingly.
	 * 
	 * @param bin  the index of the bin in the histogram
	 * @param incr the increment to add to the bin
	 * 
	 * @see #setNormalized(boolean)
	 * @see #setNormalized(int)
	 */
	public void addData(int bin, double incr) {
		nSamples++;
		if (data == null || bin < 0)
			return;
		data[row][bin] += incr;
		if (normIdx >= 0)
			data[normIdx][bin]++;
	}

	/**
	 * Add data point {@code x} to histogram. The data point is added to the
	 * histogram for \(x\in[x_\text{min},x_\text{max}]\). If \(x&gt;x_\text{max}]\)
	 * or \(x&lt;x_\text{min}]\) the range is doubled to \([x_\text{min},
	 * x_\text{max} + (x_\text{max}-x_\text{min})]\)) or \([x_\text{min} -
	 * (x_\text{max}-x_\text{min}), x_\text{max}]\)), respectively. The number of
	 * bins remains unchanged.
	 * <br>
	 * <strong>Important:</strong> The number of bins needs to be even!
	 * 
	 * @param x the new data point
	 * 
	 * @see #x2bin(double)
	 */
	public void addData(double x) {
		nSamples++;
		if (data == null)
			return;
		while (x > style.xMax) {
			doubleMaxRange();
			style.xMax = 2 * style.xMax - style.xMin;
		}
		while (x < style.xMin) {
			doubleMinRange();
			style.xMin = 2 * style.xMin - style.xMax;
		}
		data[row][x2bin(x)]++;
	}

	/**
	 * Clear the histrogram data.
	 */
	public void clearData() {
		if (data == null)
			return;
		for (double[] entry : data)
			Arrays.fill(entry, 0.0);
		nSamples = 0.0;
	}

	/**
	 * Double the range of the histogram by lowering the minimum value.
	 * 
	 * @evoludo.impl The number of bins needs to be even!
	 */
	private void doubleMinRange() {
		double[] bins = data[row];
		int nBins2 = nBins / 2;
		for (int i = 0; i < nBins2; i++) {
			int i2 = nBins - 1 - i - i;
			bins[i] = bins[i2] + bins[i2 - 1];
		}
		Arrays.fill(bins, 0, nBins2, 0.0);
	}

	/**
	 * Double the range of the histogram by increasing the maximum value.
	 * 
	 * @evoludo.impl The number of bins needs to be even!
	 */
	private void doubleMaxRange() {
		double[] bins = data[row];
		int nBins2 = nBins / 2;
		for (int i = 0; i < nBins2; i++) {
			int i2 = i + i;
			bins[i] = bins[i2] + bins[i2 + 1];
		}
		Arrays.fill(bins, nBins2, nBins, 0.0);
	}

	/**
	 * Convert {@code x}-coordinate to bin index.
	 * 
	 * @param x the {@code x}-coordinate
	 * @return the index of the bin
	 */
	public int x2bin(double x) {
		int len = data[row].length;
		// if x == style.xMax return len-1 (not len)
		return Math.min((int) ((x - style.xMin) / (style.xMax - style.xMin) * len), len - 1);
	}

	/**
	 * Convert the bin index to {@code x}-coordinate.
	 * 
	 * @param bin the index of the bin
	 * @return the {@code x}-coordinate
	 */
	public double bin2x(int bin) {
		double norm = 1.0 / data[row].length;
		double x = bin * norm;
		double range = style.xMax - style.xMin;
		// center of bin
		x = style.xMin + range * (x + norm * 0.5);
		// would it be nice to round to nearest rational?
		// Math.round involves longs, which are evil in GWT...
		// double rounded = Math.round(x);
		// if (Math.abs(x - rounded) < 1e-12)
		// 	return rounded;
		return x;
	}

	@Override
	public void reset() {
		super.reset();
		nSamples = 0.0;
		if (data == null)
			return;
		Arrays.fill(data[row], 0.0);
		if (normIdx >= 0)
			Arrays.fill(data[normIdx], 0.0);
	}

	@Override
	public void export(MyContext2d ctx) {
		MyContext2d bak = g;
		g = ctx;
		paint(true);
		g = bak;
	}

	@Override
	public boolean displayMessage(String msg) {
		message = msg;
		if (!super.displayMessage(message))
			return false;
		// add frame
		g.save();
		g.scale(scale, scale);
		g.translate(bounds.getX(), bounds.getY());
		drawFrame(4, 4);
		g.restore();
		return true;
	}

	@Override
	public void clearMessage() {
		super.clearMessage();
		message = null;
	}

	@Override
	public boolean paint(boolean force) {
		if (super.paint(force))
			return true;
		if (!force && !doUpdate())
			return true;

		g.save();
		g.scale(scale, scale);
		clearCanvas();
		g.translate(bounds.getX(), bounds.getY());

		g.setFillStyle(style.graphColor);
		int yLevels = 4;
		double[] myData = data[row];
		double[] norm = null;
		if (style.autoscaleY) {
			// Note: autoscaling is pretty slow - could be avoided by checks when adding new
			// data points; increasing
			// max is fairly easy; if max gets reduced a global check for new maximum is
			// required; graph would
			// need to keep track of bin index that contains the maximum.
			double yMax = -Double.MAX_VALUE, yMin = Double.MAX_VALUE;
			if (normIdx >= 0) {
				norm = data[normIdx];
				boolean nodata = true;
				for (int n = 0; n < nBins; n++) {
					double nnorm = norm[n];
					if (nnorm < 1e-8)
						continue;
					double val = myData[n] / nnorm;
					yMin = Math.min(yMin, val);
					yMax = Math.max(yMax, val);
					nodata = false;
				}
				if (nodata) {
					yMax = 1.0;
					yMin = 0.0;
				}
			} else {
				yMax = ArrayMath.max(myData);
				if (!isNormalized)
					yMax /= nSamples;
			}
			if (yMax <= 1.0) {
				// find maximum - assumes that data is normalized and max<=1!
				while (yMax > autoscale[autoscaleidx][0])
					autoscaleidx--;
				while (yMax < autoscale[autoscaleidx][1])
					autoscaleidx++;
				style.yMax = autoscale[autoscaleidx][0];
				yLevels = (int) autoscale[autoscaleidx][2];
			} else {
				// round yMax up to 'nice' boundary
				if (yMax > style.yMax)
					style.yMax = Functions.roundUp(yMax);
				else if (yMax < 0.8 * style.yMax)
					style.yMax = Functions.roundUp(yMax);

				if (normIdx < 0) {
					yMin = ArrayMath.min(myData);
					if (!isNormalized)
						yMin /= nSamples;
				}
				if (yMin < style.yMin)
					style.yMin = Functions.roundDown(yMin);
				else if (yMin > 1.25 * style.yMin)
					style.yMin = Functions.roundDown(yMin);
			}
		}

		double barwidth = bounds.getWidth() / nBins;
		double xshift = 0;
		double h = bounds.getHeight();
		double map = h / (style.yMax - style.yMin);
		if (normIdx >= 0) {
			norm = data[normIdx];
			for (int n = 0; n < nBins; n++) {
				double nnorm = norm[n];
				double barheight = nnorm < 1e-8 ? 0.0
						: Math.min(Math.max(1.0, map * (myData[n] / nnorm - style.yMin)), h - 1);
				fillRect(xshift, h - barheight, barwidth - 1.0, barheight);
				xshift += barwidth;
			}
		} else {
			if (!isNormalized)
				map /= nSamples;
			for (int n = 0; n < nBins; n++) {
				double barheight = Math.min(Math.max(1.0, map * (myData[n] - style.yMin)), h - 1);
				fillRect(xshift, h - barheight, barwidth - 1.0, barheight);
				xshift += barwidth;
			}
		}
		drawMarkers();
		drawFrame(4, yLevels);
		g.restore();
		return false;
	}

	/**
	 * Draw the marked bins.
	 */
	protected void drawMarkers() {
		double barwidth = bounds.getWidth() / nBins;
		double h = bounds.getHeight();
		int nMarkers = binmarkers.size();
		int n = 0;
		for (Marker mark : binmarkers) {
			int bin = x2bin(mark.x);
			mark.bin = bin; // remember bin to easily retrieve notes for tooltips (if any)
			double x = bin * barwidth - 0.5;
			g.setStrokeStyle(mark.color == null ? markerColors[n++ % nMarkers] : mark.color);
			if (mark.linestyle != null)
				g.setLineDash(mark.linestyle);
			strokeLine(x, 0.0, x, h);
			strokeLine(x + barwidth, 0.0, x + barwidth, h);
			g.setLineDash(style.solidLine);
		}
	}

	/**
	 * Get the index of the bin at {@code (x, y)}. Returns {@code -1} if no bin is
	 * found.
	 * 
	 * @param x the {@code x}-coordinate
	 * @param y the {@code y}-coordinate
	 * @return the index of the bin
	 */
	public int getBinAt(int x, int y) {
		if (data == null)
			return -1;
		x -= (int) (3 * style.frameWidth);
		if (!bounds.contains(x, y))
			return -1;
		double ibarwidth = nBins / bounds.getWidth();
		return (int) ((x - bounds.getX()) * ibarwidth);
	}

	@Override
	public String getTooltipAt(int x, int y) {
		int bar = getBinAt(x, y);
		if (bar < 0)
			return null;
		return tooltipProvider.getTooltipAt(bar);
	}

	@Override
	public String getTooltipAt(int bar) {
		// note label is null for undirected graph with the same interaction and
		// competition graphs
		StringBuilder tip = new StringBuilder(
				style.showLabel && style.label != null ? "<b>" + style.label + "</b><br/>" : "");
		switch (controller.getType()) {
			case DEGREE:
				if (Math.abs(style.xMax - (nBins - 1)) < 1e-6) {
					tip.append("<table style='border-collapse:collapse;border-spacing:0;'>");
					tip.append("<tr><td><i>" + style.xLabel + ":</i></td><td>" + bar + "</td></tr>");
					tip.append("<tr><td><i>" + style.yLabel + ":</i></td><td>"
							+ Formatter.formatPercent(data[row][bar], 2) + "</td></tr></table>");
					break;
				}
				//$FALL-THROUGH$
			case TRAIT:
			case FITNESS:
				tip.append("<table style='border-collapse:collapse;border-spacing:0;'>");
				tip.append("<tr><td><i>" + style.xLabel + ":</i></td><td>["
						+ Formatter.format(style.xMin + bar * (style.xMax - style.xMin) / nBins, 2) +
						", " + Formatter.format(style.xMin + (bar + 1) * (style.xMax - style.xMin) / nBins, 2)
						+ ")</td></tr>");
				tip.append("<tr><td><i>" + style.yLabel + ":</i></td><td>" + Formatter.formatPercent(data[row][bar], 2)
						+ "</td></tr>");
				String note = getNoteAt(bar);
				if (note != null)
					tip.append("<tr><td><i>Note:</i></td><td>" + note + "</td></tr>");
				tip.append("</table>");
				break;
			case STATISTICS_FIXATION_PROBABILITY:
				tip.append("<table style='border-collapse:collapse;border-spacing:0;'>");
				tip.append("<tr><td><i>" + style.xLabel + ":</i></td>");
				int binSize = (int) ((style.xMax + 1) / nBins);
				if (binSize == 1)
					tip.append("<td>" + bar + "</td></tr>");
				else {
					int start = bar * binSize;
					int end = start + binSize - 1;
					if (bar == nBins - 1) {
						// careful with last bin
						end = Math.max(end, (int) style.xMax);
					}
					String separator = (end - start > 1) ? "-" : ",";
					tip.append("<td>[" + start + separator + end + "]</td></tr>");
				}
				int nTraits = data.length - 1;
				double norm = data[nTraits][bar];
				tip.append("<tr><td><i>samples:</i></td><td>" + (int) norm + "</td></tr>");
				if (style.percentY)
					tip.append("<tr><td><i>" + style.yLabel + ":</i></td><td>"
							+ (norm > 0.0 ? Formatter.formatPercent(data[row][bar] / norm, 2) : "0") +
							"</td></tr></table>");
				else
					tip.append("<tr><td><i>" + style.yLabel + ":</i></td><td>"
							+ (norm > 0.0 ? Formatter.format(data[row][bar] / norm, 2) : "0") +
							"</td></tr></table>");
				break;
			case STATISTICS_FIXATION_TIME:
				tip.append("<table style='border-collapse:collapse;border-spacing:0;'>" +
						"<tr><td><i>" + style.xLabel + ":</i></td><td>");
				int nPop = module.getNPopulation();
				if (nPop > data[0].length) {
					tip.append("[" + Formatter.format(style.xMin + (double) bar / nBins * (style.xMax - style.xMin), 2)
							+ "-" +
							Formatter.format(style.xMin + (double) (bar + 1) / nBins * (style.xMax - style.xMin), 2)
							+ ")");
				} else {
					tip.append(bar + "</td></tr>" +
							"<tr><td><i>samples:</i></td><td>" + (int) getSamples(bar));
				}
				tip.append("</td></tr><tr><td><i>" + style.yLabel + ":</i></td>");
				if (style.percentY)
					tip.append("<td>" + Formatter.formatPercent(getData(bar), 2) + "</td>");
				else
					tip.append("<td>" + Formatter.format(getData(bar), 2) + "</td>");
				tip.append("</tr></table>");
				break;
			case STATISTICS_STATIONARY:
				tip.append("<table style='border-collapse:collapse;border-spacing:0;'>" + //
						"<tr><td><i>" + style.xLabel + ":</i></td>");
				nPop = module.getNPopulation();
				binSize = (nPop + 1) / MAX_BINS + 1;
				if (binSize == 1)
					tip.append("<td>" + bar + "</td></tr>");
				else {
					int start = bar * binSize;
					int end = start + binSize - 1;
					if (bar == nBins - 1) {
						// careful with last bin
						nPop = module.getNPopulation();
						end = Math.max(end, nPop);
					}
					String separator = (end - start > 1) ? "-" : ",";
					tip.append("<td>[" + start + separator + end + "]</td></tr>");
				}
				tip.append("<tr><td><i>" + style.yLabel + ":</i></td><td>"
						+ Formatter.formatPercent(data[row][bar] / getSamples(), 2) + "</td></tr></table>");
				break;
			default:
				break;

		}
		return tip.toString();
	}

	/**
	 * The context menu to select autoscaling of the y-axis.
	 */
	private ContextMenuCheckBoxItem autoscaleYMenu;

	/**
	 * The flag to indicate whether the autoscale y-axis menu is enabled.
	 */
	private boolean enableAutoscaleYMenu = true;

	/**
	 * Enable or disable the autoscale y-axis menu.
	 * 
	 * @param enable {@code true} to enable the autoscale menu
	 */
	public void enableAutoscaleYMenu(boolean enable) {
		enableAutoscaleYMenu = enable;
	}

	@Override
	public void populateContextMenuAt(ContextMenu menu, int x, int y) {
		// process autoscale context menu
		if (autoscaleYMenu == null) {
			autoscaleYMenu = new ContextMenuCheckBoxItem("Autoscale y-axis", new Command() {
				@Override
				public void execute() {
					style.autoscaleY = !style.autoscaleY;
				}
			});
		}
		autoscaleYMenu.setChecked(style.autoscaleY);
		autoscaleYMenu.setEnabled(enableAutoscaleYMenu);
		menu.addSeparator();
		menu.add(autoscaleYMenu);
		super.populateContextMenuAt(menu, x, y);
	}
}
