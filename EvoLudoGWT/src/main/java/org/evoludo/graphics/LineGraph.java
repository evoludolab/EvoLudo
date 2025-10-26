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

import java.awt.Color;
import java.util.Iterator;

import org.evoludo.geom.Point2D;
import org.evoludo.graphics.AbstractGraph.HasLogScaleY;
import org.evoludo.graphics.AbstractGraph.Shifting;
import org.evoludo.graphics.AbstractGraph.Zooming;
import org.evoludo.math.ArrayMath;
import org.evoludo.math.Functions;
import org.evoludo.simulator.ColorMapCSS;
import org.evoludo.simulator.models.Data;
import org.evoludo.simulator.modules.Continuous;
import org.evoludo.simulator.modules.Module;
import org.evoludo.simulator.views.BasicTooltipProvider;
import org.evoludo.simulator.views.Mean;
import org.evoludo.ui.ContextMenu;
import org.evoludo.ui.ContextMenuItem;
import org.evoludo.util.CLOParser;
import org.evoludo.util.Formatter;
import org.evoludo.util.RingBuffer;

import com.google.gwt.core.client.Scheduler;

/**
 * LineGraph visualizes time series data as one or more line plots inside a
 * resizable, pannable and zoomable canvas. It extends AbstractGraph<double[]>
 * and implements interactions for shifting and zooming (Shifting, Zooming),
 * optional logarithmic y-axis scaling (HasLogScaleY) and provides tooltips
 * (BasicTooltipProvider).
 *
 * <h3>Concepts and data layout:</h3>
 * <ul>
 * <li>Each element stored in the internal RingBuffer is a double[] whose
 * first element is the time stamp and subsequent elements are the values
 * to plot for each line (index 1..n). Some modules use a specific
 * layout (e.g. mean, mean-sdev, mean+sdev).</li>
 * <li>The graph maintains an x-range [xMin, xMax] and a y-range [yMin,
 * yMax] via the Style object. The x-range is expressed in the same time
 * units as the first element of the data arrays. The buffer capacity and
 * style.xIncr determine the maximum historical range available for panning
 * (the absolute bounds are [absMin, absMax] where absMax is hard-coded to
 * 0.0 and absMin = absMax - capacity*xIncr).</li>
 * <li>The graph can operate with autoscaling for the y-axis (style.autoscaleY)
 * or with fixed y-limits. When autoscale is enabled, added data updates
 * yMin/yMax to at least cover observed values; ranges are expanded but
 * never shrunk automatically.</li>
 * <li>Logarithmic y-scaling is supported (style.logScaleY), but it requires
 * strictly positive values; if negative or zero values are present the
 * graph disables log-scaling and issues a warning. When autoscaling and
 * log-scaling interact, small positive minima are adjusted to a fraction
 * of the maximum to avoid log(0).</li>
 * </ul>
 *
 * <h3>Main behaviors and responsibilities:</h3>
 * <ul>
 * <li>Construction: create a LineGraph with a Mean view and a Module that
 * supplies trait names, colors and data semantics.</li>
 * <li>Parsing: parse(String args) accepts a comma-separated argument string
 * (no spaces around commas) and recognizes options: {@code log},
 * {@code xmin <value>}, {@code xmax <value>}, {@code ymin <value>},
 * {@code ymax <value>}. ymin/ymax disable autoscale when specified.</li>
 * <li>Data ingestion: addData(double t, double[] data) prepends time and
 * delegates to addData(double[]). addData(double[]) appends the data
 * array into the RingBuffer (the caller must ensure the first element is
 * time and the array is not modified afterwards). addData updates the
 * autoscaled y-range and may disable log-scaling if non-positive values
 * are encountered.</li>
 * <li>Resetting/initialization: reset() prepares the buffer, initializes
 * autoscaled y-range and recomputes the number of steps along x relative
 * to the current view. Buffer size is at least DEFAULT_BUFFER_SIZE or the
 * view width, and steps are clamped between 1 and buffer capacity.</li>
 * <li>Painting: paint(boolean force) renders the lines, optional markers and
 * axes frame. The method clips drawing to the plotting area, supports
 * linear or log y-scaling, and draws colored lines and markers using the
 * module-provided color scheme. Painting is scheduled on the UI thread
 * using schedulePaint() to coalesce multiple requests.</li>
 * <li>Interaction:
 * <ul>
 * <li>shift(dx, dy) pans the x-range in multiples of style.xIncr. Small
 * mouse/touch deltas that do not exceed a single increment are
 * accumulated until sufficient movement occurs. Shifting respects the
 * absolute data bounds determined by the buffer capacity.</li>
 * <li>zoom() resets the view to a default zoom focused on the latest
 * data (xMax = 0.0 and xMin = xMax - max(1, DEFAULT_STEPS*xIncr)).</li>
 * <li>zoom(double zoom, double x, double y) zooms around the right-hand
 * end (xMax is treated as the reference) and enforces a minimum
 * number of steps (MIN_STEPS). Zooming also snaps bounds to integer
 * multiples of style.xIncr and clamps to the available history.</li>
 * </ul>
 * </li>
 * <li>Tooltips: getTooltipAt(int x,int y) converts pixel coordinates to
 * normalized plotting coordinates and delegates to getTooltipAt(double x,
 * double y). The tooltip builder searches the buffer to interpolate the
 * time and values at the requested x position and returns an HTML table
 * with the x-value, y-value and per-trait values (or mean ± sdev when
 * applicable). Tooltips are suppressed while the left mouse button is
 * held and when the point lies outside bounds.</li>
 * <li>Context menu: populateContextMenuAt adds a "Clear" action that clears
 * the graph history and triggers an immediate repaint.</li>
 * </ul>
 *
 * <h3>Fields of interest (summary):</h3>
 * <ul>
 * <li>steps: number of steps shown along the x-axis (clamped to [1,
 * buffer.capacity]).</li>
 * <li>buffer: RingBuffer<double[]> storing historical data arrays (time +
 * values).</li>
 * <li>style: visual and interaction configuration (xMin/xMax/yMin/yMax,
 * xIncr, autoscaleY, logScaleY, markerSize, lineWidth, etc.).</li>
 * <li>paintScheduled: coalesces deferred paint requests.</li>
 * <li>pinchScale / pinch: state used for touch pinch gestures (scale and
 * center).</li>
 * </ul>
 *
 * <h3>Important implementation notes and caveats:</h3>
 * <ul>
 * <li>The buffer stores references to the provided double[] objects for
 * performance — callers must not modify arrays after passing them to
 * addData(double[]).</li>
 * <li>Log-scaling requires strictly positive y-values. If negative or zero
 * values exist the graph will disable log-scale and emit a warning.</li>
 * <li>Painting occurs on the UI thread and is scheduled via GWT Scheduler;
 * callers should avoid heavy synchronous work on the UI thread during
 * paint() to keep the UI responsive.</li>
 * <li>Precision: x- and y-axis snapping to style.xIncr is used to keep numeric
 * bounds aligned with the data grid; floating point rounding may occur
 * when snapping or computing steps.</li>
 * </ul>
 * 
 * <p>
 * This class is intended for use within the EvoLudo UI framework and relies
 * on the surrounding infrastructure (Style, RingBuffer, Module, Mean view,
 * drawing context MyContext2d and Scheduler). It focuses on correctness of
 * visualization, responsive UI updates via scheduled paints and robust handling
 * of autoscaling and log-scaling edge cases.
 *
 * <h3>Usage example (conceptual):</h3>
 * 
 * <pre>
 *   LineGraph g = new LineGraph(meanView, module);
 *   g.parse("ymin 0.0,ymax 1.0"); // configure
 *   g.addData(t, new double[]{t, v1, v2, ...});
 *   g.zoom(2.0, sx, sy); // zoom in
 *   // tooltips and context menu are integrated into the hosting UI
 * </pre>
 *
 * @author Christoph Hauert
 */
@SuppressWarnings("java:S110")
public class LineGraph extends AbstractGraph<double[]>
		implements Shifting, Zooming, HasLogScaleY, BasicTooltipProvider {

	/**
	 * The default number of (time) steps shown on this graph.
	 */
	protected double steps = DEFAULT_STEPS;

	/**
	 * Create new line graph for {@code view}. The {@code id} is used to
	 * distinguish different graphs of the same module to visualize different
	 * components of the data and represents the index of the data column.
	 * 
	 * @param view   the view of this graph
	 * @param module the module backing the graph
	 */
	public LineGraph(Mean view, Module<?> module) {
		super(view, module);
	}

	@Override
	protected void onLoad() {
		super.onLoad();
		setStylePrimaryName("evoludo-HistoGraph");
		setTooltipProvider(this);
	}

	@Override
	public boolean parse(String args) {
		// set defaults
		style.autoscaleY = true;
		setLogY(false);
		if (args != null)
			parseArgs(args);
		return true;
	}

	/**
	 * Parse command line options for the graph. The following options are
	 * recognized:
	 * <ul>
	 * <li>{@code log}: enable logarithmic scaling on the y-axis
	 * <li>{@code xmin <value>}: set the minimum x-axis value
	 * <li>{@code xmax <value>}: set the maximum x-axis value
	 * <li>{@code ymin <value>}: set the minimum y-axis value
	 * <li>{@code ymax <value>}: set the maximum y-axis value
	 * </ul>
	 * 
	 * @param args the command line arguments (comma separated, no spaces)
	 */
	private void parseArgs(String args) {
		for (String arg : args.split(CLOParser.VECTOR_DELIMITER)) {
			arg = arg.trim();
			if (arg.startsWith("log")) {
				setLogY(true);
			} else if (arg.startsWith("xmin")) {
				style.xMin = Double.parseDouble(arg.substring(arg.indexOf(" ") + 1).trim());
			} else if (arg.startsWith("xmax")) {
				style.xMax = Double.parseDouble(arg.substring(arg.indexOf(" ") + 1).trim());
			} else if (arg.startsWith("ymin")) {
				style.yMin = Double.parseDouble(arg.substring(arg.indexOf(" ") + 1).trim());
				style.autoscaleY = false;
			} else if (arg.startsWith("ymax")) {
				style.yMax = Double.parseDouble(arg.substring(arg.indexOf(" ") + 1).trim());
				style.autoscaleY = false;
			}
		}
	}

	@Override
	public void reset() {
		if (style.autoscaleY) {
			style.yMin = Double.MAX_VALUE;
			style.yMax = -Double.MAX_VALUE;
		}
		setLogY(style.logScaleY);
		double oldXMin = style.xMin;
		super.reset();
		if (buffer == null || buffer.getCapacity() < MIN_BUFFER_SIZE)
			buffer = new RingBuffer<double[]>(Math.max((int) bounds.getWidth(), DEFAULT_BUFFER_SIZE));
		setSteps(steps * (style.xMax - style.xMin) / (style.xMax - oldXMin));
	}

	/**
	 * Add data to the graph. The time {@code t} is prepended to the data as the
	 * first element.
	 * 
	 * @evoludo.impl The data array is cloned and the time prepended before adding
	 *               it to the buffer.
	 * 
	 * @param t    the time of the data
	 * @param data the data to add
	 */
	public void addData(double t, double[] data) {
		addData(prependTime2Data(t, data));
	}

	/**
	 * Add data to the graph.
	 * 
	 * @evoludo.impl The data array is directly added to the buffer. It is the
	 *               caller's responsibility to ensure that the first entry
	 *               represents time and the data remains unmodified.
	 * 
	 * @param data the data to add
	 */
	public void addData(double[] data) {
		// always add data
		buffer.append(data);
		// time does not count for min/max
		double t = data[0];
		data[0] = style.yMin;
		double min = ArrayMath.min(data);
		if (min <= 0.0)
			style.logScaleY = false;
		if (style.autoscaleY) {
			// dynamically extend range if needed - never reduce (would need to
			// consult RingBuffer for this)
			if (!Double.isNaN(min))
				style.yMin = Math.min(style.yMin, Functions.roundDown(min));
			data[0] = style.yMax;
			double max = ArrayMath.max(data);
			if (!Double.isNaN(max))
				style.yMax = Math.max(style.yMax, Functions.roundUp(max));
		}
		data[0] = t;
	}

	@Override
	void setLogY(boolean logY) {
		if (!hasHistory()) {
			super.setLogY(logY);
			return;
		}
		if (style.yMin < 0.0) {
			noLogY();
			return;
		}
		style.logScaleY = logY;
		if (!style.autoscaleY)
			return;
		double[] min = buffer.min(this::compareData);
		double bufmin = ArrayMath.min(min);
		if (bufmin < 0.0) {
			noLogY();
			return;
		}
		if (logY && bufmin == 0.0)
			bufmin = 0.01 * style.yMax;
		if (!logY && bufmin > 0.0 && bufmin < 0.01 * style.yMax)
			bufmin = 0.0;
		style.yMin = Functions.roundDown(bufmin);
	}

	/**
	 * Helper method to disable log scale on {@code y}-axis and issue warning.
	 */
	private void noLogY() {
		if (style.logScaleY)
			logger.warning("Log scale requires positive values");
		style.logScaleY = false;
	}

	/**
	 * Compare two data arrays based on their minimum value (ignoring first entry,
	 * time). Returns a negative integer, zero, or a positive integer if the minimum
	 * of the first argument is less than, equal to, or greater than the second,
	 * respectively.
	 * 
	 * @param o1 the first data array
	 * @param o2 the second data array
	 * @return a negative integer, zero, or a positive integer
	 */
	private int compareData(double[] o1, double[] o2) {
		// ignore time
		double t = o1[0];
		o1[0] = Double.MAX_VALUE;
		double m1 = ArrayMath.min(o1);
		o1[0] = t;
		t = o2[0];
		o2[0] = Double.MAX_VALUE;
		double m2 = ArrayMath.min(o2);
		o2[0] = t;
		return Double.compare(m1, m2);
	}

	@Override
	public boolean paint(boolean force) {
		// keep this method small — delegate detailed drawing to helpers
		if (super.paint(force))
			return true;
		if (!force && !doUpdate())
			return true;

		g.save();
		g.scale(scale, scale);
		clearCanvas();
		g.translate(bounds.getX(), bounds.getY());
		g.save();

		double w = bounds.getWidth();
		double h = bounds.getHeight();

		g.translate(w, h);
		g.scale(1.0, -1.0);
		g.beginPath();
		// with -w a small strip on left is not drawn... proper fix needed!
		g.rect(style.markerSize, 0.0, -(w + 3.0), h);
		g.clip();

		// compute y transform and draw content
		double[] yinfo = computeYScale(h);
		int nLines = Math.max(0, buffer.getDepth() - 1);
		drawLines(w, yinfo, nLines);
		drawMarkers(w, yinfo[0], nLines);

		g.restore();
		drawFrame(4, 4);
		g.restore();
		return false;
	}

	/**
	 * Compute y-axis scale info: [ymin, yrange, yScale]
	 * 
	 * @param h the height of the plotting area
	 * @return an array containing [ymin, yScale]
	 */
	private double[] computeYScale(double h) {
		double ymin;
		double yrange;
		if (style.logScaleY) {
			ymin = Math.log10(style.yMin);
			yrange = Math.log10(style.yMax) - ymin;
		} else {
			ymin = style.yMin;
			yrange = style.yMax - ymin;
		}
		double yScale = (yrange == 0.0) ? 1.0 : h / yrange;
		return new double[] { ymin, yScale };
	}

	/**
	 * Draw the line plots and (suppressed) markers at the newest sample.
	 * 
	 * @param width  the width of the plotting area
	 * @param yinfo  the y-axis transform info
	 * @param nLines the number of lines to draw
	 */
	private void drawLines(double width, double[] yinfo, int nLines) {
		Iterator<double[]> it = buffer.iterator();
		if (!it.hasNext() || nLines <= 0)
			return;

		double[] current = it.next();
		g.setLineWidth(style.lineWidth);
		double xScale = width / (style.xMax - style.xMin);
		double start = -style.xMax * xScale;

		// draw markers for the newest sample if visible
		if (start <= 0.0) {
			drawLatestMarkers(start, current, nLines, yinfo);
		}

		double end = start;
		while (it.hasNext()) {
			double[] prev = it.next();
			if (current[0] > prev[0]) {
				double delta = (prev[0] - current[0]) * xScale;
				start += delta;
				if (start < 0.0) {
					drawSegmentClipped(start, end, prev, current, nLines, yinfo, width);
					if (start <= -width)
						break;
				}
				end = start;
			}
			current = prev;
		}
	}

	/**
	 * Draw markers for the newest sample (extracted helper).
	 * 
	 * @param start   the starting x-coordinate
	 * @param current the current data array
	 * @param nLines  the number of lines to draw
	 * @param yinfo   the y-axis transform info
	 */
	private void drawLatestMarkers(double start, double[] current, int nLines, double[] yinfo) {
		for (int n = 0; n < nLines; n++) {
			g.setFillStyle(colors[n]);
			double y = (style.logScaleY ? Math.log10(current[n + 1]) : current[n + 1]) - yinfo[0];
			fillCircle(start, y * yinfo[1], style.markerSize);
		}
	}

	/**
	 * Draw a historical segment between 'start' and 'end', handling clipping
	 * against the visible window (-w .. 0) and log/linear y transforms.
	 * 
	 * @param start   the starting x-coordinate of the segme
	 * @param end     the ending x-coordinate of
	 * @param prev    the previous data array
	 * @param current the current data array
	 * @param nLines  the number of lines to draw
	 * @param yinfo   the y-axis transform info
	 * @param w       the width of the plotting area
	 */
	private void drawSegmentClipped(double start, double end,
			double[] prev, double[] current, int nLines,
			double[] yinfo, double width) {
		double ymin = yinfo[0];
		double yScale = yinfo[1];
		for (int n = 0; n < nLines; n++) {
			setStrokeStyleAt(n);
			double pi;
			double ci;
			if (style.logScaleY) {
				pi = Math.log10(prev[n + 1]) - ymin;
				ci = Math.log10(current[n + 1]) - ymin;
			} else {
				pi = prev[n + 1] - ymin;
				ci = current[n + 1] - ymin;
			}
			// fully inside visible area
			if (start >= -width && end <= 0.0) {
				strokeLine(start, pi * yScale, end, ci * yScale);
			} else {
				double s = Math.max(-width, start);
				double e = Math.min(0, end);
				double denom = (end - start);
				double m = (denom == 0.0) ? 0.0 : (ci - pi) / denom;
				if (style.logScaleY && m > 0.0)
					m = Math.log10(m);
				strokeLine(s, (pi + m * (s - start)) * yScale, e, (ci - m * (end - e)) * yScale);
			}
		}
	}

	/**
	 * Draw horizontal marker lines (if any) using the same y transform.
	 * 
	 * @param width  the width of the plotting area
	 * @param ymin   the minimum y-value
	 * @param nLines the number of lines to draw
	 */
	private void drawMarkers(double width, double ymin, int nLines) {
		if (markers == null || nLines <= 0)
			return;
		for (double[] mark : markers) {
			g.setLineDash(mark[0] > 0.0 ? style.dashedLine : style.dottedLine);
			for (int n = 0; n < nLines; n++) {
				double mn = (style.logScaleY ? Math.log10(mark[n + 1]) : mark[n + 1]) - ymin;
				g.setStrokeStyle(markerColors[n % markerColors.length]);
				strokeLine(-width, mn, 0.0, mn);
			}
			g.setLineDash(style.solidLine);
		}
	}

	/**
	 * The flag to indicate whether painting is already scheduled. Subsequent
	 * requests are ignored.
	 */
	private boolean paintScheduled = false;

	/**
	 * Schedule painting of the graph. If painting is already scheduled, subsequent
	 * requests are ignored.
	 * 
	 * @see #paint()
	 */
	protected void schedulePaint() {
		if (paintScheduled)
			return;
		Scheduler.get().scheduleDeferred(() -> {
			paint();
			paintScheduled = false;
		});
		paintScheduled = true;
	}

	@Override
	public void export(MyContext2d ctx) {
		MyContext2d bak = g;
		g = ctx;
		paint(true);
		g = bak;
	}

	/**
	 * The field to track the progress of the pinch gesture.
	 */
	double pinchScale = -Double.MAX_VALUE;

	/**
	 * The center of the pinch gesture.
	 */
	Point2D pinch = new Point2D(0.5, 0.5);

	/**
	 * The total shift in the {@code x}-direction.
	 */
	int totDx;

	/**
	 * The total shift in the {@code y}-direction.
	 */
	int totDy;

	@Override
	public void shift(int dx, int dy) {
		if (hasMessage)
			return;
		totDx += dx;
		totDy += dy;
		double rx = totDx / bounds.getWidth();
		double arx = Math.abs(rx);
		double range = style.xMax - style.xMin;
		// shift in multiples in xIncr
		int units = (int) (range * arx / style.xIncr);
		if (units == 0)
			return;
		totDx = 0;
		totDy = 0;
		double shift = units * style.xIncr;
		shift = (rx > 0.0 ? -shift : shift);
		double maxRange = buffer.getCapacity() * style.xIncr;
		double absMax = 0.0;
		double absMin = absMax - maxRange;
		if (shift > 0.0) {
			// shift right; keep range if lower bound reaches minimum
			style.xMin = Math.max(absMin, style.xMin - Math.max(style.xIncr, shift));
			style.xMax = style.xMin + range;
			if (style.xMax >= absMax) {
				style.xMax = absMax;
				style.xMin = absMax - range;
			}
		} else {
			// shift left; keep range if upper bound reaches maximum
			style.xMax = Math.min(absMax, style.xMax - Math.min(-style.xIncr, shift));
			style.xMin = style.xMax - range;
			if (style.xMin <= absMin) {
				style.xMin = absMin;
				style.xMax = absMin + range;
			}
		}
		schedulePaint();
	}

	/**
	 * The minimum number of steps along the {@code x}-axis.
	 */
	protected static final int MIN_STEPS = 10;

	/**
	 * The default number of steps along the {@code x}-axis.
	 */
	protected static final int DEFAULT_STEPS = 100;

	@Override
	public void zoom() {
		style.xMax = 0.0;
		style.xMin = style.xMax - Math.max(1.0, DEFAULT_STEPS * style.xIncr);
		schedulePaint();
	}

	@Override
	public void zoom(double zoom, double x, double y) {
		if (zoom <= 0.0 || Math.abs(zoom - 1.0) < 1e-6)
			return;
		double xMax = style.xMax;
		double xMin = xMax - (style.xMax - style.xMin) / zoom;
		int nSteps = (int) ((xMax - xMin) / style.xIncr);
		if (nSteps < MIN_STEPS)
			xMin = xMax - MIN_STEPS * style.xIncr;
		double maxRange = buffer.getCapacity() * style.xIncr;
		// note: maximum of zero is hardcoded...
		double absMax = 0.0;
		double absMin = absMax - maxRange;
		style.xMin = Math.max(absMin, (int) (xMin / style.xIncr) * style.xIncr);
		style.xMax = Math.min(0.0, (int) (xMax / style.xIncr) * style.xIncr);
		// zooming out is hard with touch when fully zoomed in...
		if (style.xMax - style.xMin <= style.xIncr) {
			if (zoom < 1.0)
				return;
			style.xMin = style.xMax - style.xIncr;
		}
		schedulePaint();
	}

	/**
	 * Get the number of steps along the {@code x}-axis.
	 * 
	 * @return the number of steps
	 */
	public double getSteps() {
		return steps;
	}

	/**
	 * Set the number of steps along the {@code x}-axis.
	 * 
	 * @param steps the number of steps
	 */
	public void setSteps(double steps) {
		this.steps = Math.max(1.0, Math.min(buffer.getCapacity(), steps));
	}

	@Override
	public String getTooltipAt(int x, int y) {
		if (leftMouseButton)
			return null;
		if (!bounds.contains(x, y))
			return null;
		double sx = (x - bounds.getX() - 0.5) / bounds.getWidth();
		if (sx < 0.0 || sx > 1.0)
			return null;
		double height = bounds.getHeight();
		double sy = (height - (y - bounds.getY() + 0.5)) / height;
		if (sy < 0.0 || sy > 1.0)
			return null;
		return tooltipProvider.getTooltipAt(sx, sy);
	}

	@Override
	public String getTooltipAt(double x, double y) {
		double mouset = style.xMin + x * (style.xMax - style.xMin);
		double ymin;
		double yrange;
		if (style.logScaleY) {
			ymin = Math.log10(style.yMin);
			yrange = Math.log10(style.yMax) - ymin;
		} else {
			ymin = style.yMin;
			yrange = style.yMax - ymin;
		}

		double yval = ymin + y * yrange;
		if (style.logScaleY)
			yval = Math.pow(10.0, yval);

		StringBuilder tip = new StringBuilder(TABLE_STYLE);
		appendTipHeader(tip, mouset, yval);
		appendTipInterpolation(tip, mouset);
		tip.append(TABLE_END);
		return tip.toString();
	}

	/**
	 * Build and append the static header rows (label, x and y rows) to the tooltip.
	 */
	private void appendTipHeader(StringBuilder tip, double mouset, double yval) {
		if (style.label != null) {
			tip.append(TABLE_ROW_START)
					.append(style.label)
					.append(TABLE_ROW_END);
		}
		tip.append(TABLE_ROW_START_RIGHT)
				.append(style.xLabel)
				.append(TABLE_CELL_NEXT)
				.append(Formatter.format(mouset, 2))
				.append(TABLE_ROW_END);
		tip.append(TABLE_ROW_START_RIGHT)
				.append(style.yLabel)
				.append(TABLE_CELL_NEXT)
				.append(style.percentY ? Formatter.formatPercent(yval, 1) : Formatter.format(yval, 2))
				.append(TABLE_ROW_END);
	}

	/**
	 * Scan the buffer and determine the two surrounding samples and the
	 * interpolation factor for the supplied mouse time.
	 * 
	 * @param mouset the mouse time
	 * @return the interpolated data array (time + values) or null if not found
	 */
	private double[] findInterpolationForMouse(double mouset) {
		Iterator<double[]> it = buffer.iterator();
		double accum = 0.0;
		if (!it.hasNext())
			return new double[0];
		double[] current = it.next();
		while (it.hasNext()) {
			double[] prev = it.next();
			double dt = current[0] - prev[0];
			accum -= Math.max(0.0, dt);
			if (accum <= mouset) {
				double fx = 1.0 - (mouset - accum) / dt;
				double[] inter = new double[current.length];
				inter[0] = current[0] - fx * dt;
				for (int n = 1; n < inter.length; n++) {
					inter[n] = interpolate(current[n], prev[n], fx);
				}
				return inter;
				// return new InterpResult(current, prev, fx, time);
			}
			current = prev;
		}
		return new double[0];
	}

	/**
	 * Append interpolated values (either Continuous mean±sdev or trait values)
	 * to the tooltip builder using the precomputed interpolation result.
	 * 
	 * @param tip    the tooltip builder
	 * @param mouset the mouse time
	 */
	private void appendTipInterpolation(StringBuilder tip, double mouset) {
		double[] inter = findInterpolationForMouse(mouset);
		if (inter.length == 0)
			return;
		// time row
		tip.append(TABLE_SEPARATOR)
				.append(TABLE_ROW_START_RIGHT)
				.append(style.xLabel)
				.append(TABLE_CELL_NEXT)
				// .append(Formatter.format(r.time, 2))
				.append(Formatter.format(inter[0], 2))
				.append(TABLE_ROW_END);

		Color[] colors = module.getMeanColors();
		if (module instanceof Continuous) {
			appendTipContinuous(tip, inter, colors);
			return;
		}
		appendTipTraits(tip, inter, colors);
	}

	/**
	 * Append mean ± sdev row for Continuous modules.
	 * 
	 * @param tip    the tooltip builder
	 * @param inter  the interpolated data array
	 * @param colors the colors for each trait
	 */
	private void appendTipContinuous(StringBuilder tip, double[] inter, Color[] colors) {
		tip.append(TABLE_ROW_START_COLOR)
				.append(ColorMapCSS.Color2Css(colors[0]))
				.append(";'>mean")
				.append(TABLE_CELL_NEXT)
				.append(style.percentY ? Formatter.formatPercent(inter[1], 2) : Formatter.format(inter[1], 2));
		tip.append(" ± ")
				.append(style.percentY ? Formatter.formatPercent(inter[2], 2) : Formatter.format(inter[2], 2))
				.append(TABLE_ROW_END); // mean, mean-sdev, mean+sdev
	}

	/**
	 * Append individual trait rows for non-Continuous modules.
	 * 
	 * @param tip    the tooltip builder
	 * @param inter  the interpolated data array
	 * @param colors the colors for each trait
	 */
	private void appendTipTraits(StringBuilder tip, double[] inter, Color[] colors) {
		// N.B. length includes time at index 0
		int len = inter.length;
		Data type = view.getType();
		for (int n = 0; n < len - 1; n++) {
			int n1 = n + 1;
			String name;
			Color color;
			if (n1 == len - 1 && type == Data.FITNESS) {
				name = "average";
				color = Color.BLACK;
			} else {
				name = module.getTraitName(n);
				color = colors[n];
			}
			if (name == null)
				continue;
			tip.append(TABLE_ROW_START_COLOR)
					.append(ColorMapCSS.Color2Css(color)).append(";'>")
					.append(name)
					.append(TABLE_CELL_NEXT);
			if (Double.isNaN(inter[n1])) {
				tip.append("-");
			} else {
				tip.append(style.percentY ? Formatter.formatPercent(inter[n1], 2) : Formatter.format(inter[n1], 2));
			}
			tip.append(TABLE_ROW_END);
		}
	}

	/**
	 * Interpolate linearly between {@code current} and {@code prev} at {@code x}.
	 * 
	 * @param left  the left value
	 * @param right the right value
	 * @param x     the location inbetween
	 * @return the interpolated value
	 */
	private double interpolate(double left, double right, double x) {
		return (1.0 - x) * left + x * right;
	}

	/**
	 * The context menu item to clear the graph.
	 */
	private ContextMenuItem clearMenu;

	@Override
	public void populateContextMenuAt(ContextMenu menu, int x, int y) {
		// add menu to clear canvas
		if (clearMenu == null) {
			clearMenu = new ContextMenuItem("Clear", () -> {
				clearHistory();
				paint(true);
			});
		}
		menu.addSeparator();
		menu.add(clearMenu);
		super.populateContextMenuAt(menu, x, y);
		zoomInMenu.setText("Zoom in x-axis (2x)");
		zoomOutMenu.setText("Zoom out x-axis (0.5x)");
	}
}
