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

package org.evoludo.graphics;

import java.util.ArrayList;
import java.util.Arrays;

import org.evoludo.math.ArrayMath;
import org.evoludo.math.Functions;
import org.evoludo.simulator.models.Data;
import org.evoludo.simulator.modules.Module;
import org.evoludo.simulator.views.BasicTooltipProvider;
import org.evoludo.simulator.views.Histogram;
import org.evoludo.ui.ContextMenu;
import org.evoludo.ui.ContextMenuCheckBoxItem;
import org.evoludo.ui.ContextMenuItem;
import org.evoludo.util.Formatter;

/**
 * A histogram graph used to render and interact with binned data.
 *
 * <p>
 * HistoGraph renders a histogram from a backing two-dimensional
 * {@code double[][]} data array. Each HistoGraph instance references a specific
 * row in that array (the {@code row} index) and therefore multiple HistoGraph
 * instances may share the same underlying data buffer but operate on different
 * rows. The number of bins rendered is determined by the length of the
 * referenced data row.
 * </p>
 *
 * <h2>Data model and normalization</h2>
 * <ul>
 * <li>The backing data is a {@code double[][]} provided via
 * {@link #setData(double[][])}. The graph uses the configured {@code row} to
 * access its own bin values.</li>
 * <li>Normalization may be controlled in two mutually exclusive ways:
 * <ul>
 * <li>{@link #setNormalized(boolean)} toggles normalization relative to the
 * graph's sample count ({@link #nSamples}). If enabled, values returned by
 * {@link #getData(int)} are already normalized.</li>
 * <li>{@link #setNormalized(int)} sets {@code normIdx}, a separate data row
 * used to normalize per-bin values (per-bin denominators are read from that
 * row). When {@code normIdx >= 0} normalization uses the values in the
 * specified row and the graph will render values as
 * {@code data[row][i] / data[normIdx][i]}.</li>
 * </ul>
 * Note: calling {@link #setNormalized(boolean)} with {@code true} clears a
 * previously set {@code normIdx} and vice versa — these modes conflict.
 * </li>
 * <li>{@link #nSamples} tracks the total number of added samples;
 * {@link #addData(int)} and {@link #addData(double)} increment this counter
 * (and update normalization row when {@code normIdx >= 0}).</li>
 * </ul>
 *
 * <h2>Bins, range and dynamic resizing</h2>
 * <ul>
 * <li>The graph maps an x-coordinate to a bin via {@link #x2bin(double)} and
 * maps bin indices back to an x-coordinate (bin center) with
 * {@link #bin2x(int)}.</li>
 * <li>Several behaviors assume the number of bins ({@code nBins}) is even:
 * {@link #doubleMinRange()} and {@link #doubleMaxRange()} fold and shift bin
 * contents when the numeric x-range must be doubled. These methods require that
 * {@code nBins} is even; this is a precondition for correct
 * range-doubling.</li>
 * <li>{@link #addData(double)} will automatically expand the numeric x-range
 * when
 * a value falls outside the current {@code style.xMin..style.xMax} range by
 * repeatedly calling the range-doubling helpers until the value fits. The
 * number of bins remains constant during these range changes.</li>
 * </ul>
 *
 * <h2>Rendering and autoscaling</h2>
 * <ul>
 * <li>The {@link #paint(boolean)} method draws the histogram bars using the
 * current {@code style} (colors, frame, axis ranges). If
 * {@code style.autoscaleY} is enabled the graph will adapt {@code style.yMin}
 * and {@code style.yMax} automatically based on the observed data values or the
 * configured normalization row.</li>
 * <li>Autoscaling rounds bounds to "nice" values using
 * {@link Functions#roundDown(double)} / {@link Functions#roundUp(double)} and
 * updates only when extrema change sufficiently to avoid jitter.</li>
 * <li>Bars are clipped and mapped into pixel space using {@code bounds} and the
 * current y-range; rendering ensures minimal visible height for very small
 * values.</li>
 * </ul>
 *
 * <h2>Markers and interactivity</h2>
 * <ul>
 * <li>Markers (inner class {@link Marker}) annotate the histogram at an
 * x-coordinate and are drawn as vertical lines spanning bin boundaries. Markers
 * can have a color, description and optional dash pattern ({@code linestyle}).
 * Markers are added with {@link #addMarker(double, String, String)} (or the
 * overload that accepts a dash pattern) and cleared with
 * {@link #clearMarkers()}.</li>
 * <li>When markers are drawn the bin they hit is saved into {@link Marker#bin}
 * which allows the graph to include marker descriptions in tooltips.</li>
 * <li>The graph provides tooltip text for a pixel coordinate via
 * {@link #getTooltipAt(int, int)} and for a bin index via
 * {@link #getTooltipAt(int)}. Tooltip content varies by the histogram
 * {@code view.type} (degree, fitness, trait and several statistics types have
 * specialized tooltip formats).</li>
 * <li>Context menu support includes an Axes submenu with autoscaling,
 * full-range,
 * and right y-axis controls; autoscaling can be enabled/disabled via
 * {@link #enableAutoscaleYMenu(boolean)}.</li>
 * </ul>
 *
 * <h2>Usage notes and important constraints</h2>
 * <ul>
 * <li>Because the data array may be shared across multiple HistoGraph
 * instances, callers must take care when manipulating the shared data buffer
 * concurrently.</li>
 * <li>The range-doubling implementation assumes an even {@code nBins}. Using an
 * odd number of bins will break {@link #doubleMinRange()} /
 * {@link #doubleMaxRange()}.</li>
 * <li>Most methods guard against a {@code null} data array; however many
 * operations are no-ops or return default values (e.g. {@code -1} for missing
 * bins) when no data is set.</li>
 * </ul>
 *
 * @author Christoph Hauert
 * 
 * @see #setData(double[][])
 * @see #setNormalized(boolean)
 * @see #setNormalized(int)
 * @see #addData(int)
 * @see #addData(double)
 * @see #addMarker(double, String, String)
 */
@SuppressWarnings("java:S110")
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
	 * The list of markers for the histogram.
	 */
	private ArrayList<Marker> binmarkers = new ArrayList<>();

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
	 * The default number of bins for the histogram.
	 */
	public static final int DEFAULT_BINS = 100;

	/**
	 * The minimum width of a bin.
	 */
	public static final int MIN_BIN_WIDTH = 1;

	/**
	 * Create new histogram graph for <code>module</code> running in
	 * <code>view</code>. The row is used to identify data entries that apply
	 * to this histogram and represents the index of the data row.
	 * 
	 * @param view   the view of this graph
	 * @param module the module backing the graph
	 * @param row    the index of the data row
	 */
	public HistoGraph(Histogram view, Module<?> module, int row) {
		super(view, module);
		this.row = row;
	}

	@Override
	protected void onLoad() {
		super.onLoad();
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
		nSamples += 1.0;
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
		nSamples += 1.0;
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
		nSamples += 1.0;
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
		// return rounded;
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
	public void clearMessage() {
		super.clearMessage();
		message = null;
	}

	@Override
	public boolean paint(boolean force) {
		if (super.paint(force))
			return true;

		g.save();
		g.scale(scale, scale);
		clearCanvas();
		g.translate(bounds.getX(), bounds.getY());
		g.setFillStyle(style.graphColor);

		double[] norm = (normIdx >= 0 && data != null && normIdx < data.length) ? data[normIdx] : null;
		double[] myData = data[row];
		if (style.autoscaleY) {
			double yMin;
			double yMax;
			if (norm != null) {
				double[] bounds = computeNormBounds(myData, norm);
				yMin = bounds[0];
				yMax = bounds[1];
			} else {
				yMin = ArrayMath.min(myData);
				yMax = ArrayMath.max(myData);
				if (!isNormalized) {
					yMin /= Math.max(1.0, nSamples);
					yMax /= Math.max(1.0, nSamples);
				}
			}
			updateAutoscaledYMax(yMax);
			updateAutoscaledYMin(yMin);
		}

		double barwidth = bounds.getWidth() / nBins;
		double h = bounds.getHeight();
		double yRange = style.yMax - style.yMin;
		double map = h / (yRange > 0.0 ? yRange : 1.0);

		drawBars(myData, norm, barwidth, h, map);

		drawMarkers();
		drawFrame(4, 4);
		g.restore();
		return false;
	}

	/**
	 * Update and round {@link GraphStyle#yMax} when the observed maximum changed
	 * sufficiently.
	 * 
	 * @param yMax observed maximum bin height
	 */
	private void updateAutoscaledYMax(double yMax) {
		if (Double.isNaN(yMax))
			return;
		if (!(yMax > style.yMax || yMax < 0.8 * style.yMax))
			return;
		double roundedMax = roundAutoscale(yMax, true);
		if (roundedMax <= style.yMin) {
			double delta = Math.max(Math.abs(style.yMin) * 0.1, 1e-12);
			roundedMax = roundAutoscale(style.yMin + delta, true);
			if (roundedMax <= style.yMin)
				roundedMax = style.yMin + delta;
		}
		style.yMax = roundedMax;
	}

	/**
	 * Update and round {@link GraphStyle#yMin} when the observed minimum changed
	 * sufficiently.
	 * 
	 * @param yMin observed minimum bin height
	 */
	private void updateAutoscaledYMin(double yMin) {
		if (!(yMin < style.yMin || yMin > 1.25 * style.yMin))
			return;
		double roundedMin = roundAutoscale(yMin, false);
		if (roundedMin >= style.yMax) {
			double delta = Math.max(Math.abs(style.yMax) * 0.1, 1e-12);
			roundedMin = roundAutoscale(style.yMax - delta, false);
			if (roundedMin >= style.yMax)
				roundedMin = style.yMax - delta;
		}
		style.yMin = roundedMin;
	}

	/**
	 * Round autoscaled bounds. Percent axes use one additional decimal order
	 * compared to {@link Functions#round(double)} for higher sensitivity.
	 *
	 * @param value value to round
	 * @param up    {@code true} to round up, {@code false} to round down
	 * @return rounded value
	 */
	private double roundAutoscale(double value, boolean up) {
		if (!style.percentY)
			return up ? Functions.roundUp(value) : Functions.roundDown(value);
		// percent aware rounding
		int magnitude = Functions.magnitude(value) - 1;
		double factor = Math.pow(10.0, magnitude);
		if (factor == 0.0 || !Double.isFinite(factor))
			return up ? Functions.roundUp(value) : Functions.roundDown(value);
		double scaled = value / factor;
		return (up ? Math.ceil(scaled) : Math.floor(scaled)) * factor;
	}

	/**
	 * Compute min/max from per-bin normalization row; returns {yMin, yMax}. If no
	 * valid normalization entries are found returns {0.0, 1.0}.
	 * 
	 * @param myData histogram counts
	 * @param norm   per-bin normalization data
	 * @return two-element array {yMin, yMax}
	 */
	private double[] computeNormBounds(double[] myData, double[] norm) {
		double yMin = Double.MAX_VALUE;
		double yMax = -Double.MAX_VALUE;
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
			return new double[] { 0.0, 1.0 };
		}
		return new double[] { yMin, yMax };
	}

	/**
	 * Draw bars for the histogram using either per-bin normalization row or global
	 * normalization flag.
	 * 
	 * @param myData   histogram values
	 * @param norm     per-bin normalization data (may be {@code null})
	 * @param barwidth width of a bar in pixels
	 * @param h        total graph height
	 * @param map      conversion factor from value to pixels
	 */
	private void drawBars(double[] myData, double[] norm, double barwidth, double h, double map) {
		double xshift = 0;
		if (norm != null) {
			for (int n = 0; n < nBins; n++) {
				double nnorm = norm[n];
				double barheight = nnorm < 1e-8 ? 0.0
						: Math.min(Math.max(1.0, map * (myData[n] / nnorm - style.yMin)), h - 1);
				fillRect(xshift, h - barheight, barwidth - 1.0, barheight);
				xshift += barwidth;
			}
		} else {
			double sampleNorm = Math.max(1.0, nSamples);
			for (int n = 0; n < nBins; n++) {
				double val = myData[n];
				if (!isNormalized)
					val /= sampleNorm;
				double barheight = Math.min(Math.max(1.0, map * (val - style.yMin)), h - 1);
				fillRect(xshift, h - barheight, barwidth - 1.0, barheight);
				xshift += barwidth;
			}
		}
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
		if (!inside(x, y))
			return -1;
		int adjX = x - (int) (3 * style.frameWidth);
		double ibarwidth = nBins / bounds.getWidth();
		return (int) ((adjX - bounds.getX()) * ibarwidth);
	}

	@Override
	public String getTooltipAt(int x, int y) {
		int bar = getBinAt(x, y);
		if (bar < 0)
			return null;
		BasicTooltipProvider provider = tooltipProvider != null ? tooltipProvider : this;
		return provider.getTooltipAt(bar);
	}

	@Override
	protected boolean inside(int x, int y) {
		int adjX = x - (int) (3 * style.frameWidth);
		return bounds.contains(adjX, y);
	}

	@Override
	public String getTooltipAt(int bar) {
		// note label is null for undirected graph with the same interaction and
		// competition graphs
		StringBuilder tip = new StringBuilder(
				style.showLabel && style.label != null ? "<b>" + style.label + "</b><br/>" : "");
		switch (view.getType()) {
			case DEGREE:
				if (Math.abs(style.xMax - (nBins - 1)) < 1e-6) {
					appendTipDegree(tip, bar);
					break;
				}
				//$FALL-THROUGH$
			case TRAIT:
			case FITNESS:
				appendBinTooltip(tip, bar);
				break;
			case STATISTICS_FIXATION_PROBABILITY:
				appendTipFixProb(tip, bar);
				break;
			case STATISTICS_FIXATION_TIME:
				appendTipFixTime(tip, bar);
				break;
			case STATISTICS_STATIONARY:
				appendTipStationary(tip, bar);
				break;
			default:
				// no additional info
				break;
		}
		return tip.toString();
	}

	/**
	 * Append degree tooltip.
	 * 
	 * @param tip the tooltip string builder
	 * @param bar the bar index
	 */
	private void appendTipDegree(StringBuilder tip, int bar) {
		tip.append(TABLE_STYLE);
		tip.append(TABLE_ROW_START + style.xLabel + TABLE_CELL_NEXT + bar + TABLE_ROW_END);
		tip.append(TABLE_ROW_START + style.yLabel + TABLE_CELL_NEXT
				+ Formatter.formatPercent(data[row][bar], 2) + TABLE_ROW_END + TABLE_END);
	}

	/**
	 * Append tooltip for bin at mouse coordinates.
	 * 
	 * @param tip the tooltip string builder
	 * @param bar the bar index
	 */
	private void appendBinTooltip(StringBuilder tip, int bar) {
		tip.append(TABLE_STYLE);
		tip.append(TABLE_ROW_START + style.xLabel + TABLE_CELL_NEXT + "["
				+ Formatter.format(style.xMin + bar * (style.xMax - style.xMin) / nBins, 2) +
				", " + Formatter.format(style.xMin + (bar + 1) * (style.xMax - style.xMin) / nBins, 2)
				+ ")" + TABLE_ROW_END);
		tip.append(TABLE_ROW_START + style.yLabel + TABLE_CELL_NEXT + Formatter.formatPercent(data[row][bar], 2)
				+ TABLE_ROW_END);
		String note = getNoteAt(bar);
		if (note != null)
			tip.append(TABLE_ROW_START + "Note" + TABLE_CELL_NEXT + note + TABLE_ROW_END);
		tip.append(TABLE_END);
	}

	/**
	 * Append fixation probability tooltip.
	 * 
	 * @param tip the tooltip string builder
	 * @param bar the bar index
	 */
	private void appendTipFixProb(StringBuilder tip, int bar) {
		tip.append(TABLE_STYLE);
		tip.append(TABLE_ROW_START + style.xLabel + TABLE_CELL_NEXT);
		int binSize = (int) ((style.xMax + 1) / nBins);
		if (binSize == 1) {
			tip.append(bar + TABLE_ROW_END);
		} else {
			int start = bar * binSize;
			int end = start + binSize - 1;
			if (bar == nBins - 1) {
				// careful with last bin
				end = Math.max(end, (int) style.xMax);
			}
			String separator = (end - start > 1) ? "-" : ",";
			tip.append("[" + start + separator + end + "]" + TABLE_ROW_END);
		}
		int nTraits = data.length - 1;
		double norm = data[nTraits][bar];
		tip.append(TABLE_ROW_START + "samples" + TABLE_CELL_NEXT + (int) norm + TABLE_ROW_END);
		if (style.percentY) {
			tip.append(TABLE_ROW_START + style.yLabel + TABLE_CELL_NEXT
					+ (norm > 0.0 ? Formatter.formatPercent(data[row][bar] / norm, 2) : "0") +
					TABLE_ROW_END + TABLE_END);
		} else {
			tip.append(TABLE_ROW_START + style.yLabel + TABLE_CELL_NEXT
					+ (norm > 0.0 ? Formatter.format(data[row][bar] / norm, 2) : "0") +
					TABLE_ROW_END + TABLE_END);
		}
	}

	/**
	 * Append fixation time tooltip.
	 * 
	 * @param tip the tooltip string builder
	 * @param bar the bar index
	 */
	private void appendTipFixTime(StringBuilder tip, int bar) {
		tip.append(TABLE_STYLE + TABLE_ROW_START + style.xLabel + TABLE_CELL_NEXT);
		int nPop = module.getNPopulation();
		if (nPop > data[0].length) {
			tip.append("[" + Formatter.format(style.xMin + (double) bar / nBins * (style.xMax - style.xMin), 2)
					+ "-" +
					Formatter.format(style.xMin + (double) (bar + 1) / nBins * (style.xMax - style.xMin), 2)
					+ ")");
		} else {
			tip.append(bar + TABLE_ROW_END +
					TABLE_ROW_START + "samples" + TABLE_CELL_NEXT + (int) getSamples(bar));
		}
		tip.append(TABLE_ROW_END + TABLE_ROW_START + style.yLabel + TABLE_CELL_NEXT);
		if (style.percentY)
			tip.append(Formatter.formatPercent(getData(bar), 2) + "</td>");
		else
			tip.append(Formatter.format(getData(bar), 2) + "</td>");
		tip.append("</tr></table>");
	}

	/**
	 * Append stationary distribution tooltip.
	 * 
	 * @param tip the tooltip string builder
	 * @param bar the bar index
	 */
	private void appendTipStationary(StringBuilder tip, int bar) {
		tip.append(TABLE_STYLE + //
				TABLE_ROW_START + style.xLabel + TABLE_CELL_NEXT);
		int nPop = module.getNPopulation();
		int binSize = (nPop + 1) / DEFAULT_BINS + 1;
		if (binSize == 1) {
			tip.append(bar + TABLE_ROW_END);
		} else {
			int start = bar * binSize;
			int end = start + binSize - 1;
			if (bar == nBins - 1) {
				// careful with last bin
				nPop = module.getNPopulation();
				end = Math.max(end, nPop);
			}
			String separator = (end - start > 1) ? "-" : ",";
			tip.append("[" + start + separator + end + "]" + TABLE_ROW_END);
		}
		tip.append(TABLE_ROW_START + style.yLabel + TABLE_CELL_NEXT
				+ Formatter.formatPercent(data[row][bar] / getSamples(), 2) + TABLE_ROW_END + TABLE_END);
	}

	/**
	 * The context menu for axis-related options.
	 */
	private ContextMenu axesMenu;

	/**
	 * The context menu item to select autoscaling of the y-axis.
	 */
	private ContextMenuCheckBoxItem autoscaleYMenu;

	/**
	 * The context menu item to reset percent axes to full range.
	 */
	private ContextMenuItem fullRangeMenu;

	/**
	 * The context menu item to toggle the y-axis side.
	 */
	private ContextMenuCheckBoxItem rightYAxisMenu;

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
		addAxesMenu(menu);
		super.populateContextMenuAt(menu, x, y);
	}

	/**
	 * Add the axes submenu with autoscale, full-range, and y-axis side controls.
	 * 
	 * @param menu the context menu to populate
	 */
	private void addAxesMenu(ContextMenu menu) {
		if (axesMenu == null) {
			axesMenu = new ContextMenu(menu, "Axes");
			autoscaleYMenu = new ContextMenuCheckBoxItem("Autoscale y-axis", () -> {
				style.autoscaleY = !style.autoscaleY;
				autoscaleYMenu.setChecked(style.autoscaleY);
				paint(true);
			});
			fullRangeMenu = new ContextMenuItem("Full range", () -> {
				if (style.autoscaleY) {
					style.autoscaleY = false;
					autoscaleYMenu.setChecked(false);
				}
				applyFullRange();
				paint(true);
			});
			rightYAxisMenu = new ContextMenuCheckBoxItem("Right Y-axis", () -> {
				boolean showOnRight = !style.showYAxisRight;
				rightYAxisMenu.setChecked(showOnRight);
				view.setRightYAxis(showOnRight);
			});
		}
		axesMenu.clear();
		axesMenu.addHeader("Axes");
		autoscaleYMenu.setChecked(style.autoscaleY);
		autoscaleYMenu.setEnabled(enableAutoscaleYMenu);
		rightYAxisMenu.setChecked(style.showYAxisRight);
		axesMenu.add(autoscaleYMenu);
		if (hasFullRangeMenu())
			axesMenu.add(fullRangeMenu);
		axesMenu.add(rightYAxisMenu);
		menu.add("Axes", axesMenu);
	}

	/**
	 * Determine whether the full-range action is applicable.
	 *
	 * @return {@code true} if the full-range menu item should be shown
	 */
	private boolean hasFullRangeMenu() {
		return style.percentX
				|| style.percentY
				|| view.getType() == Data.STATISTICS_FIXATION_TIME;
	}

	/**
	 * Apply full-range bounds according to axis style and histogram type.
	 */
	private void applyFullRange() {
		if (style.percentX) {
			style.xMin = 0.0;
			style.xMax = 1.0;
		}
		if (style.percentY || view.getType() == Data.STATISTICS_FIXATION_PROBABILITY) {
			style.yMin = 0.0;
			style.yMax = 1.0;
			return;
		}
		if (view.getType() != Data.STATISTICS_FIXATION_TIME) {
			return;
		}
		double yMax = computeObservedYMax();
		style.yMin = 0.0;
		if (!Double.isFinite(yMax) || yMax <= 0.0) {
			style.yMax = 1.0;
			return;
		}
		style.yMax = Math.max(Functions.roundUp(yMax), 1e-12);
	}

	/**
	 * Compute observed maximum y-value from current data and normalization mode.
	 *
	 * @return observed maximum, or {@link Double#NaN} if unavailable
	 */
	private double computeObservedYMax() {
		if (data == null || row < 0 || row >= data.length || data[row] == null)
			return Double.NaN;
		double[] myData = data[row];
		if (normIdx >= 0 && normIdx < data.length && data[normIdx] != null) {
			return computeNormBounds(myData, data[normIdx])[1];
		}
		double yMax = ArrayMath.max(myData);
		if (!isNormalized) {
			yMax /= Math.max(1.0, nSamples);
		}
		return yMax;
	}
}
