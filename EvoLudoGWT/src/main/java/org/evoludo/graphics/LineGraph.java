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

import java.awt.Color;
import java.util.Arrays;
import java.util.Iterator;

import org.evoludo.geom.Point2D;
import org.evoludo.graphics.AbstractGraph.HasLogScaleY;
import org.evoludo.graphics.AbstractGraph.Shifting;
import org.evoludo.graphics.AbstractGraph.Zooming;
import org.evoludo.math.Functions;
import org.evoludo.simulator.ColorMapCSS;
import org.evoludo.simulator.models.Data;
import org.evoludo.simulator.modules.Continuous;
import org.evoludo.simulator.modules.Module;
import org.evoludo.simulator.views.BasicTooltipProvider;
import org.evoludo.simulator.views.Mean;
import org.evoludo.ui.ContextMenu;
import org.evoludo.ui.ContextMenuCheckBoxItem;
import org.evoludo.ui.ContextMenuItem;
import org.evoludo.util.CLOParser;
import org.evoludo.util.CLOption;
import org.evoludo.util.Formatter;
import org.evoludo.util.RingBuffer;

import com.google.gwt.core.client.Scheduler;

/**
 * LineGraph visualizes time series data as one or more line plots inside a
 * resizable, pannable and zoomable canvas. It extends
 * AbstractGraph&lt;double[]&gt;
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
 * <li>buffer: RingBuffer&lt;double[]&gt; storing historical data arrays (time +
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
	double steps = DEFAULT_STEPS;

	/**
	 * The number of lines to plot.
	 */
	int nLines;

	/**
	 * Padding applied when fitting the y-axis to visible data.
	 */
	private static final double ZOOM_FIT_MARGIN = 0.05;

	/**
	 * Fraction of {@code yMax} below which a linear {@code y}-axis minimum is
	 * considered insignificant and snapped to zero rather than given its own
	 * (arbitrarily fine-grained) axis floor.
	 */
	private static final double MIN_YMAX_FRACTION = 0.01;

	/**
	 * Whether the model tracks real time for the current run (as opposed to only
	 * generation/update counts), see {@code EvoLudo.Model#getTime()}. Determines
	 * whether the "use real time" toggle is shown at all.
	 */
	private boolean realTimeAvailable = false;

	/**
	 * Whether the x-axis currently plots real time (the value appended at the end
	 * of each buffered sample) rather than updates (the value prepended at the
	 * start). Defaults to real time as soon as it becomes available, unless the
	 * user has explicitly picked an axis through the context menu.
	 */
	private boolean useRealTime = false;

	/**
	 * Whether {@link #useRealTime} was set explicitly by the user through the
	 * context menu, in which case it is no longer overridden automatically when
	 * {@link #setRealTimeAvailable(boolean)} is called.
	 */
	private boolean userSetAxis = false;

	/**
	 * The x-axis label to show when plotting updates.
	 */
	private String updatesLabel = "generation";

	/**
	 * The x-axis label to show when plotting real time.
	 */
	private String realTimeLabel = "time";

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
	public void calcBounds(int width, int height) {
		super.calcBounds(width, height);
		double pad = style.markerSize + 2.0;
		if (pad > 0.0 && bounds.getWidth() > pad) {
			// keep some room on the right for latest markers
			bounds.adjust(0, 0, -pad, 0);
		}
	}

	/**
	 * The options of line graphs. The options are selected on the command line,
	 * see {@link LineGraph#parse(String)}. All options except {@link #LOG} take a
	 * numerical parameter, separated from the key by a space.
	 */
	public enum GraphOption implements CLOption.Key {

		/**
		 * Logarithmic scaling of the y-axis.
		 */
		LOG("log", "logarithmic scaling of y-axis"),

		/**
		 * Set the minimum of the x-axis.
		 */
		XMIN("xmin", "minimum of x-axis <x>"),

		/**
		 * Set the maximum of the x-axis.
		 */
		XMAX("xmax", "maximum of x-axis <x>"),

		/**
		 * Set the minimum of the y-axis. Disables autoscaling.
		 */
		YMIN("ymin", "minimum of y-axis <y> (disables autoscaling)"),

		/**
		 * Set the maximum of the y-axis. Disables autoscaling.
		 */
		YMAX("ymax", "maximum of y-axis <y> (disables autoscaling)");

		/**
		 * The key identifying the option on the command line.
		 */
		private final String key;

		/**
		 * The brief description of the option for the help screen.
		 */
		private final String title;

		/**
		 * Create a graph option with the given command line key and description.
		 *
		 * @param key   the name of the option
		 * @param title the description of the option
		 */
		GraphOption(String key, String title) {
			this.key = key;
			this.title = title;
		}

		@Override
		public String getKey() {
			return key;
		}

		@Override
		public String getTitle() {
			return title;
		}
	}

	@Override
	public boolean parse(String args) {
		// set defaults
		style.autoscaleY = true;
		setLogY(false);
		if (args == null)
			return true;
		return parseArgs(args);
	}

	/**
	 * Parse command line options for the graph, see {@link GraphOption} for the
	 * recognized options. Matching is as lenient as for command line keys, i.e.
	 * keys can be abbreviated as well as extended, and the best match wins.
	 * Unknown options or missing parameters are reported and ignored.
	 *
	 * @param args the command line arguments (comma separated)
	 * @return {@code true} if all arguments were successfully parsed
	 *
	 * @see CLOption#match(String, CLOption.Key[])
	 */
	private boolean parseArgs(String args) {
		boolean ok = true;
		for (String arg : args.split(CLOParser.VECTOR_DELIMITER)) {
			arg = arg.trim();
			if (arg.isEmpty())
				continue;
			int split = arg.indexOf(' ');
			String keyname = split < 0 ? arg : arg.substring(0, split);
			GraphOption option = CLOption.match(keyname, GraphOption.values());
			if (option == null) {
				view.logWarning("view '" + view.getName() + "': unknown option '" + arg + "' - ignored.");
				ok = false;
				continue;
			}
			if (option == GraphOption.LOG) {
				setLogY(true);
				continue;
			}
			double param;
			try {
				param = Double.parseDouble(split < 0 ? "" : arg.substring(split + 1).trim());
			} catch (NumberFormatException e) {
				view.logWarning("view '" + view.getName() + "': option '" + option.getKey()
						+ "' requires a numerical parameter - ignored.");
				ok = false;
				continue;
			}
			switch (option) {
				case XMIN:
					style.xMin = param;
					break;
				case XMAX:
					style.xMax = param;
					break;
				case YMIN:
					style.yMin = param;
					style.autoscaleY = false;
					break;
				case YMAX:
					style.yMax = param;
					style.autoscaleY = false;
					break;
				default:
					break;
			}
		}
		return ok;
	}

	@Override
	public void setColors(String[] colors) {
		super.setColors(colors);
		nLines = colors.length;
	}

	/**
	 * Get the number of lines plotted in this graph.
	 * 
	 * @return the number of lines
	 */
	public int getNLines() {
		return nLines;
	}

	/**
	 * Flag indicating that the next sample starts a new time series because the
	 * model was re-initialized.
	 */
	private boolean newTimeseries = false;

	/**
	 * The time value used to separate distinct time series in the history buffer.
	 */
	private static final double TIME_SERIES_BREAK = Double.NaN;

	/**
	 * Flag indicating that the next sample must be appended to anchor a new run
	 * after a restart or a change in time direction.
	 */
	private boolean appendNextSample = false;

	@Override
	public void reset() {
		appendNextSample = false;
		newTimeseries = false;
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

	@Override
	public void init() {
		newTimeseries = true;
	}

	/**
	 * Add data to the graph. The time {@code t} is prepended to the data as the
	 * first element.
	 *
	 * @evoludo.impl The data array is cloned and the time prepended before adding
	 *               it to the buffer.
	 *
	 * @param t        the time (updates) of the data
	 * @param data     the data to add
	 * @param realTime the real time of the data, or {@code Double.NaN} if not
	 *                 available for this run
	 */
	public void addData(double t, double[] data, double realTime) {
		addData(prependTime2Data(t, data), realTime);
	}

	/**
	 * Add data to the graph. Real time is appended as the last element of the
	 * stored sample, alongside updates prepended as the first element, so that
	 * either can be selected as the x-axis without maintaining a second, separately
	 * synchronized buffer.
	 *
	 * @evoludo.impl If the change relative to the latest buffered sample is
	 *               negligible, only the latest sample's time entries are updated
	 *               unless the new sample is needed to anchor the beginning of a
	 *               flat segment. Model initialization inserts a separator and
	 *               starts a new buffered time series. Changes in the direction of
	 *               time are always appended, and the following sample is retained
	 *               once to anchor the new direction. Otherwise the data array is
	 *               directly added to the buffer. It is the caller's
	 *               responsibility to ensure that the first entry represents
	 *               updates and the data remains unmodified.
	 *
	 * @param data     the data to add (updates prepended)
	 * @param realTime the real time of the data, or {@code Double.NaN} if not
	 *                 available for this run
	 */
	public void addData(double[] data, double realTime) {
		double[] extended = Arrays.copyOf(data, data.length + 1);
		extended[data.length] = realTime;
		data = extended;
		if (newTimeseries) {
			newTimeseries = false;
			if (!buffer.isEmpty()) {
				appendSeparator(data.length);
				appendData(data);
				appendNextSample = true;
				return;
			}
		}
		if (!buffer.isEmpty()) {
			double[] last = buffer.last();
			double dt = data[0] - last[0];
			if (Math.abs(dt) < 1e-8) {
				System.arraycopy(data, 0, last, 0, last.length);
				return;
			}
			if (appendNextSample) {
				appendData(data);
				appendNextSample = false;
				return;
			}
			int direction = dt > 0.0 ? 1 : -1;
			int recentDirection = 0;
			Iterator<double[]> iter = buffer.iterator();
			if (iter.hasNext()) {
				iter.next();
				while (iter.hasNext()) {
					double[] previous = iter.next();
					if (!Double.isFinite(previous[0]))
						break;
					recentDirection = timeStepDirection(last[0], previous[0]);
					if (recentDirection != 0)
						break;
				}
			}
			if (recentDirection != 0 && direction != recentDirection) {
				appendData(data);
				appendNextSample = true;
				return;
			}
			if (updateTimeOnly(data, last)) {
				// extend the flat segment along both axes
				last[0] = data[0];
				last[last.length - 1] = data[data.length - 1];
				return;
			}
		}
		// add new sample
		appendData(data);
	}

	/**
	 * Append a sample to the history and update autoscaling metadata.
	 *
	 * @param data the sample to append (updates prepended, real time appended)
	 */
	private void appendData(double[] data) {
		buffer.append(data);
		updateAutoscale(data);
	}

	/**
	 * Append an in-buffer separator that marks the beginning of a new time series.
	 *
	 * @param length the length of the separator sample (including the trailing
	 *               real-time column)
	 */
	private void appendSeparator(int length) {
		double[] separator = new double[length];
		Arrays.fill(separator, Double.NaN);
		separator[0] = TIME_SERIES_BREAK;
		buffer.append(separator);
	}

	/**
	 * Update autoscaled {@code y}-range from a newly appended sample.
	 *
	 * @param data the latest buffered sample (updates prepended, real time
	 *             appended)
	 */
	private void updateAutoscale(double[] data) {
		// only the plotted value columns (1..nLines) count for min/max - the
		// leading updates and trailing real-time columns are excluded
		double min = Double.POSITIVE_INFINITY;
		double max = Double.NEGATIVE_INFINITY;
		for (int n = 1; n <= nLines; n++) {
			double v = data[n];
			if (!Double.isFinite(v))
				continue;
			min = Math.min(min, v);
			max = Math.max(max, v);
		}
		if (style.logScaleY && min <= 0.0)
			// keep log scale: ignore non-positive point for autoscale updates
			return;
		if (min <= 0.0)
			style.logScaleY = false;
		if (style.autoscaleY) {
			// dynamically extend range if needed - never reduce (would need to
			// consult RingBuffer for this)
			if (max != Double.NEGATIVE_INFINITY)
				style.yMax = Math.max(style.yMax, Functions.roundUp(max));
			if (min != Double.POSITIVE_INFINITY) {
				double newMin = (!style.logScaleY && min > 0.0) ? roundLinearYMin(min, style.yMax)
						: Functions.roundDown(min);
				style.yMin = Math.min(style.yMin, newMin);
			}
		}
	}

	/**
	 * Round a candidate linear {@code y}-axis minimum down, snapping it to zero
	 * if it is an insignificant fraction of the overall range.
	 *
	 * @param min  the candidate minimum (must be {@code > 0})
	 * @param yMax the current {@code y}-axis maximum
	 * @return the rounded minimum, or {@code 0.0} if snapped
	 */
	private double roundLinearYMin(double min, double yMax) {
		if (yMax <= 0.0 || min < MIN_YMAX_FRACTION * yMax)
			return 0.0;
		return Functions.roundDown(min);
	}

	/**
	 * Return whether the latest buffered sample should only update its time entry.
	 *
	 * @param current candidate sample
	 * @param last    latest buffered sample
	 * @return {@code true} if only time should be updated
	 */
	private boolean updateTimeOnly(double[] current, double[] last) {
		if (current == null || last == null)
			return false;
		if (current.length != last.length)
			return false;
		double t = current[0];
		double lastt = last[0];
		// non-finite times serve as timeseries separators.
		if (!Double.isFinite(t) || !Double.isFinite(lastt))
			return false;
		return !keepSample(buffer, current, last);
	}

	/**
	 * Get the direction of a time step.
	 *
	 * @param newerTime the newer time value
	 * @param olderTime the older time value
	 * @return {@code +1} for forward time, {@code -1} for backward time, or
	 *         {@code 0} if no finite direction can be inferred
	 */
	private int timeStepDirection(double newerTime, double olderTime) {
		double dt = newerTime - olderTime;
		if (!Double.isFinite(dt) || Math.abs(dt) < 1e-8)
			return 0;
		return dt > 0.0 ? 1 : -1;
	}

	/**
	 * Return whether a step between buffered samples should contribute to the
	 * displayed history.
	 *
	 * @param current the current newer sample
	 * @param prev    the adjacent older sample
	 * @return the displayed width of this history step, or {@code 0.0} if the step
	 *         should be hidden
	 */
	private double historyStepWidth(double[] current, double[] prev) {
		double dt = Math.abs(xValue(current) - xValue(prev));
		if (!Double.isFinite(dt) || dt == 0.0)
			return 0.0;
		return dt;
	}

	/**
	 * Return the x-coordinate of a buffered (or interpolated) sample for the
	 * currently selected axis: the leading updates column, or the trailing real
	 * time column when {@link #useRealTime} is set. Both columns are {@code NaN}
	 * on a time-series separator, so axis selection does not affect separator
	 * detection.
	 *
	 * @param sample the sample, updates prepended and real time appended
	 * @return the x-coordinate to use for display
	 */
	private double xValue(double[] sample) {
		return useRealTime ? sample[sample.length - 1] : sample[0];
	}

	@Override
	protected boolean keepSample(RingBuffer<double[]> buffer, double[] current, double[] last) {
		double max = maxValueDelta(current, last);
		if (!Double.isFinite(max))
			return true;
		if (max > MIN_NORM_DELTA)
			return true;
		if (buffer == null)
			return false;
		if (buffer.getSize() == 1)
			return true;
		if (buffer.getSize() < 2)
			return false;
		Iterator<double[]> iter = buffer.iterator();
		if (!iter.hasNext())
			return false;
		iter.next();
		if (!iter.hasNext())
			return false;
		double[] prev = iter.next();
		return maxValueDelta(last, prev) > MIN_NORM_DELTA;
	}

	/**
	 * Like {@code maxDelta(double[], double[])} but scoped to the plotted value
	 * columns ({@code 1..nLines}), excluding the leading updates and trailing
	 * real-time columns so that neither ever-increasing axis can interfere with
	 * decimation of near-identical samples.
	 *
	 * @param current the current sample
	 * @param last    the reference sample
	 * @return the maximum absolute normalized difference among plotted values, or
	 *         {@link Double#POSITIVE_INFINITY} if the samples are invalid or
	 *         contain non-finite entries
	 */
	private double maxValueDelta(double[] current, double[] last) {
		if (current == null || last == null || current.length != last.length || current.length < 2)
			return Double.POSITIVE_INFINITY;
		double max = 0.0;
		for (int n = 1; n <= nLines; n++) {
			double a = current[n];
			double b = last[n];
			if (!Double.isFinite(a) || !Double.isFinite(b))
				return Double.POSITIVE_INFINITY;
			max = Math.max(max, Math.abs(normDelta(a, b)));
		}
		return max;
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
		double bufmin = minValue(min);
		if (bufmin < 0.0) {
			noLogY();
			return;
		}
		if (logY && bufmin == 0.0)
			bufmin = MIN_YMAX_FRACTION * style.yMax;
		style.yMin = logY ? Functions.roundDown(bufmin) : roundLinearYMin(bufmin, style.yMax);
	}

	/**
	 * Helper method to disable log scale on {@code y}-axis and issue warning.
	 */
	private void noLogY() {
		if (style.logScaleY)
			view.logWarning("Log scale requires positive values");
		style.logScaleY = false;
	}

	/**
	 * Compare two data arrays based on their minimum plotted value (ignoring the
	 * leading updates and trailing real-time columns). Returns a negative
	 * integer, zero, or a positive integer if the minimum of the first argument
	 * is less than, equal to, or greater than the second, respectively.
	 *
	 * @param o1 the first data array
	 * @param o2 the second data array
	 * @return a negative integer, zero, or a positive integer
	 */
	private int compareData(double[] o1, double[] o2) {
		return Double.compare(minValue(o1), minValue(o2));
	}

	/**
	 * Get the minimum plotted value ({@code 1..nLines}) of a buffered sample,
	 * excluding the leading updates and trailing real-time columns.
	 *
	 * @param data the sample
	 * @return the minimum plotted value, or {@link Double#MAX_VALUE} if none is
	 *         finite
	 */
	private double minValue(double[] data) {
		double min = Double.MAX_VALUE;
		for (int n = 1; n <= nLines; n++) {
			double v = data[n];
			if (Double.isFinite(v))
				min = Math.min(min, v);
		}
		return min;
	}

	@Override
	public boolean paint(boolean force) {
		// keep this method small — delegate detailed drawing to helpers
		if (super.paint(force))
			return true;

		g.save();
		g.scale(scale, scale);
		clearCanvas();
		g.translate(bounds.getX(), bounds.getY());

		double w = bounds.getWidth();
		double h = bounds.getHeight();

		// draw the frame (border, gridlines, ticks, labels) first so that data
		// drawn afterwards is never hidden underneath the frame stroke
		if (style.offsetXTickLabels)
			updateXTickOffset();
		else
			style.xTickOffset = 0.0;
		drawFrame(4, 4);

		g.save();
		g.translate(w, h);
		g.scale(1.0, -1.0);

		// map [yMin, yMax] (or their logs) onto [0, h] - the same mapping used by
		// the tick labels (AbstractGraph#computeYRange) and tooltips
		// (getTooltipAt(double, double)), so a data point at yMin or yMax is
		// drawn exactly on the frame border, with no gap and no discrepancy
		// between what is plotted, labelled, and reported in tooltips
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

		g.save();
		g.beginPath();
		// with -w a small strip on left is not drawn... proper fix needed!
		g.rect(style.markerSize, 0.0, -(w + 3.0), h);
		g.clip();
		drawLines(w, ymin, yScale);
		drawMarkers(w, ymin, yScale);
		g.restore();

		drawLatestMarkers(w, ymin, yScale);
		g.restore();
		g.restore();
		return false;
	}

	/**
	 * Draw the line plots.
	 *
	 * @param width  the width of the plotting area
	 * @param ymin   the {@code y}-value (or its log) mapped to the bottom of the
	 *               plotting area
	 * @param yScale the scale factor mapping a {@code y}-value relative to
	 *               {@code ymin} to a pixel offset
	 */
	private void drawLines(double width, double ymin, double yScale) {
		Iterator<double[]> it = buffer.iterator();
		if (!it.hasNext() || nLines <= 0)
			return;

		double[] current = it.next();
		if (!it.hasNext())
			return;
		double[] prev = it.next();
		double[] older = it.hasNext() ? it.next() : null;
		g.setLineWidth(style.lineWidth);
		double xScale = width / (style.xMax - style.xMin);
		double start = -style.xMax * xScale;

		double end = start;
		while (true) {
			double span = historyStepWidth(current, prev);
			if (span > 0.0) {
				start -= span * xScale;
				if (start < 0.0) {
					drawSegmentClipped(start, end, prev, current, ymin, yScale, width);
					if (start <= -width)
						break;
				}
				end = start;
			}
			if (older == null)
				break;
			current = prev;
			prev = older;
			older = it.hasNext() ? it.next() : null;
		}
	}

	/**
	 * Draw markers for the newest sample (extracted helper).
	 *
	 * @param width  the width of the plotting area
	 * @param ymin   the {@code y}-value (or its log) mapped to the bottom of the
	 *               plotting area
	 * @param yScale the scale factor mapping a {@code y}-value relative to
	 *               {@code ymin} to a pixel offset
	 */
	private void drawLatestMarkers(double width, double ymin, double yScale) {
		if (buffer == null || buffer.isEmpty() || nLines <= 0)
			return;
		double[] current = buffer.last();
		if (current == null)
			return;
		double xScale = width / (style.xMax - style.xMin);
		double start = -style.xMax * xScale;
		if (start > 0.0)
			return;
		for (int n = 0; n < nLines; n++) {
			if (style.logScaleY && current[n + 1] <= 0.0)
				continue;
			g.setFillStyle(colors[n]);
			double y = (style.logScaleY ? Math.log10(current[n + 1]) : current[n + 1]) - ymin;
			fillCircle(start, y * yScale, style.markerSize);
		}
	}

	/**
	 * Draw a historical segment between 'start' and 'end', handling clipping
	 * against the visible window (-w .. 0) and log/linear y transforms.
	 *
	 * @param start   starting x-coordinate of the segment
	 * @param end     ending x-coordinate of the segment
	 * @param prev    the previous data array
	 * @param current the current data array
	 * @param ymin    the {@code y}-value (or its log) mapped to the bottom of
	 *                the plotting area
	 * @param yScale  the scale factor mapping a {@code y}-value relative to
	 *                {@code ymin} to a pixel offset
	 * @param width   the width of the plotting area
	 */
	private void drawSegmentClipped(double start, double end,
			double[] prev, double[] current,
			double ymin, double yScale, double width) {
		boolean reverse = current[0] < prev[0];
		for (int n = 0; n < nLines; n++) {
			setStrokeStyleAt(n);
			g.setLineDash(reverse ? style.dottedLine : style.solidLine);
			double pi;
			double ci;
			if (style.logScaleY) {
				double pval = prev[n + 1];
				double cval = current[n + 1];
				if (pval <= 0.0)
					pval = style.yMin;
				if (cval <= 0.0)
					cval = style.yMin;
				pi = Math.log10(pval) - ymin;
				ci = Math.log10(cval) - ymin;
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
		g.setLineDash(style.solidLine);
	}

	/**
	 * Draw horizontal marker lines (if any) using the same y transform.
	 *
	 * @param width  the width of the plotting area
	 * @param ymin   the {@code y}-value (or its log) mapped to the bottom of the
	 *               plotting area
	 * @param yScale the scale factor mapping a {@code y}-value relative to
	 *               {@code ymin} to a pixel offset
	 */
	private void drawMarkers(double width, double ymin, double yScale) {
		if (nLines <= 0 || markers.isEmpty())
			return;
		for (double[] mark : markers) {
			g.setLineDash(mark[0] > 0.0 ? style.dashedLine : style.dottedLine);
			for (int n = 0; n < nLines; n++) {
				double mn1 = mark[n + 1];
				if (style.logScaleY && mn1 <= 0.0)
					continue;
				double myn = ((style.logScaleY ? Math.log10(mn1) : mn1) - ymin) * yScale;
				g.setStrokeStyle(markerColors[n % markerColors.length]);
				strokeLine(-width, myn, 0.0, myn);
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
		double oldXMin = style.xMin;
		double oldXMax = style.xMax;
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
		if (Math.abs(style.xMin - oldXMin) < 1e-8 && Math.abs(style.xMax - oldXMax) < 1e-8)
			return;
		if (zoomInertiaTimer != null && zoomInertiaTimer.isRunning())
			element.addClassName(zoom > 1.0 ? CURSOR_ZOOM_IN_CLASS : CURSOR_ZOOM_OUT_CLASS);
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
		// include the frame border itself, not just its interior, so hovering
		// over the (possibly several pixels wide) frame stroke still resolves to
		// a tooltip instead of going blank right at the edges
		double frame = style.showFrame ? style.frameWidth : 0.0;
		double relX = x - canvasPadX() - bounds.getX();
		double relY = y - canvasPadY() - bounds.getY();
		double width = bounds.getWidth();
		double height = bounds.getHeight();
		if (relX < -frame || relX > width + frame || relY < -frame || relY > height + frame)
			return null;
		double sx = Math.max(0.0, Math.min(1.0, (relX - 0.5) / width));
		double sy = Math.max(0.0, Math.min(1.0, (height - (relY + 0.5)) / height));
		return tooltipProvider.getTooltipAt(sx, sy);
	}

	@Override
	protected boolean inside(int x, int y) {
		return bounds.contains(x, y);
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
	 * 
	 * @param tip    builder receiving the markup
	 * @param mouset x-axis value under the cursor
	 * @param yval   y-axis value under the cursor
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
		if (!it.hasNext())
			return new double[0];
		double[] prev = it.next();
		double[] older = it.hasNext() ? it.next() : null;
		while (true) {
			double dt = current[0] - prev[0];
			double span = historyStepWidth(current, prev);
			if (span > 0.0) {
				accum -= span;
				if (accum <= mouset) {
					double fx = 1.0 - (mouset - accum) / span;
					double[] inter = new double[current.length];
					inter[0] = current[0] - fx * dt;
					for (int n = 1; n < inter.length; n++) {
						inter[n] = interpolate(current[n], prev[n], fx);
					}
					return inter;
					// return new InterpResult(current, prev, fx, time);
				}
			}
			if (older == null)
				break;
			current = prev;
			prev = older;
			older = it.hasNext() ? it.next() : null;
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
				.append(Formatter.format(xValue(inter), 2))
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
		// N.B. inter has updates prepended and real time appended around the
		// nLines plotted values - bound the loop by nLines, not inter.length
		Data type = view.getType();
		boolean[] active = module.getActiveTraits();
		for (int n = 0; n < nLines; n++) {
			int n1 = n + 1;
			String name;
			Color color;
			if (n1 == nLines && type == Data.FITNESS) {
				name = "average";
				color = Color.BLACK;
			} else {
				if (active != null && n < active.length && !active[n])
					continue;
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

	@Override
	public void populateContextMenuAt(ContextMenu menu, int x, int y) {
		addClearMenu(menu);
		view.addAxesMenu(menu, this);
		addZoomMenu(menu);
		if (menu.getWidgetCount() > 0 && tooltip.isVisible())
			tooltip.close();
		view.populateContextMenu(menu);
	}

	@Override
	public void populateLocalAxesMenu(ContextMenu axesMenu) {
		ContextMenuCheckBoxItem autoscaleYMenu = new ContextMenuCheckBoxItem("Autoscale", () -> {
			style.autoscaleY = !style.autoscaleY;
			if (style.autoscaleY)
				updateYRangeFromBuffer(false, false);
			paint(true);
		});
		ContextMenuItem fullYRangeMenu = new ContextMenuItem("Full range", () -> {
			style.autoscaleY = false;
			if (style.percentY) {
				style.yMin = 0.0;
				style.yMax = 1.0;
				clampLogRange();
			}
			paint(true);
		});
		ContextMenuItem fullXRangeMenu = new ContextMenuItem("Full range", () -> {
			if (updateXRangeFromBuffer())
				paint(true);
		});
		ContextMenuCheckBoxItem logYAxesMenu = new ContextMenuCheckBoxItem("Logarithmic", () -> {
			setLogY(!style.logScaleY);
			paint(true);
		});
		ContextMenuCheckBoxItem absoluteTimeMenu = new ContextMenuCheckBoxItem("Absolute " + style.xLabel, () -> {
			style.offsetXTickLabels = !style.offsetXTickLabels;
			if (style.offsetXTickLabels)
				updateXTickOffset();
			else
				style.xTickOffset = 0.0;
			paint(true);
		});
		ContextMenuCheckBoxItem realTimeMenu = new ContextMenuCheckBoxItem("Use " + realTimeLabel, () -> {
			userSetAxis = true;
			switchAxis(!useRealTime);
		});
		String axisLabel = style.xLabel == null ? "time" : style.xLabel;
		autoscaleYMenu.setChecked(style.autoscaleY);
		logYAxesMenu.setChecked(style.logScaleY);
		logYAxesMenu.setEnabled(style.yMin >= 0.0);
		absoluteTimeMenu.setText("Absolute " + axisLabel);
		absoluteTimeMenu.setChecked(style.offsetXTickLabels);
		realTimeMenu.setChecked(useRealTime);
		realTimeMenu.setEnabled(realTimeAvailable);
		axesMenu.addHeader("X-axis");
		axesMenu.add(fullXRangeMenu);
		axesMenu.add(absoluteTimeMenu);
		if (realTimeAvailable)
			axesMenu.add(realTimeMenu);
		axesMenu.addHeader("Y-axis");
		axesMenu.add(autoscaleYMenu);
		fullYRangeMenu.setEnabled(style.percentY);
		axesMenu.add(fullYRangeMenu);
		axesMenu.add(logYAxesMenu);
	}

	/**
	 * Switch the x-axis between updates and real time, relabelling the axis and
	 * recomputing the visible window and tick spacing for the newly selected
	 * axis. The two axes are not linearly related (real time can advance
	 * arbitrarily faster or slower than updates depending on the model's
	 * dynamics), so the previous zoom/pan state cannot be carried over
	 * meaningfully - the full buffered history on the new axis is shown instead.
	 *
	 * @param realTime {@code true} to plot real time, {@code false} for updates
	 */
	private void switchAxis(boolean realTime) {
		if (realTime == useRealTime)
			return;
		useRealTime = realTime;
		style.xLabel = useRealTime ? realTimeLabel : updatesLabel;
		if (updateXRangeFromBuffer())
			style.xIncr = (style.xMax - style.xMin) / Math.max(1.0, steps);
		if (style.offsetXTickLabels)
			updateXTickOffset();
		paint(true);
	}

	/**
	 * Set whether real time is available for the current run, see
	 * {@code EvoLudo.Model#getTime()}. Enables the "Use {@link #realTimeLabel}"
	 * context menu entry; if not available, forces the axis back to updates. The
	 * first time real time becomes available it is selected by default, unless
	 * the user has already picked an axis explicitly through the context menu.
	 *
	 * @param available {@code true} if real time is tracked for this run
	 */
	public void setRealTimeAvailable(boolean available) {
		realTimeAvailable = available;
		if (!available) {
			switchAxis(false);
			return;
		}
		if (!userSetAxis)
			switchAxis(true);
	}

	/**
	 * Set the x-axis labels for the updates and real-time axes, respectively.
	 * Applies immediately to the currently selected axis.
	 *
	 * @param updatesLabel  the label to show when plotting updates
	 * @param realTimeLabel the label to show when plotting real time
	 */
	public void setAxisLabels(String updatesLabel, String realTimeLabel) {
		this.updatesLabel = updatesLabel;
		this.realTimeLabel = realTimeLabel;
		style.xLabel = useRealTime ? realTimeLabel : updatesLabel;
	}

	@Override
	protected void populateZoomMenu(ContextMenu menu) {
		menu.add(new ContextMenuItem("Zoom to fit", () -> {
			if (style.autoscaleY)
				style.autoscaleY = false;
			boolean addMargin = !style.percentY;
			boolean updatedX = updateXRangeFromBuffer();
			boolean updatedY = updateYRangeFromBuffer(true, addMargin);
			if (updatedY && style.percentY) {
				style.yMin = Math.max(0.0, style.yMin);
				style.yMax = Math.min(1.0, style.yMax);
				if (style.yMin > style.yMax) {
					style.yMin = 0.0;
					style.yMax = 1.0;
				}
				clampLogRange();
			}
			if (updatedX || updatedY)
				paint(true);
		}));
		menu.addHeader("X-axis");
		super.populateZoomMenu(menu);
	}

	/**
	 * Update the y-axis range based on buffered data.
	 * 
	 * @param visibleOnly whether to limit to the visible x-range
	 * @param addMargin   whether to add a small top/bottom margin
	 * @return {@code true} if the range was updated
	 */
	private boolean updateYRangeFromBuffer(boolean visibleOnly, boolean addMargin) {
		if (buffer == null || buffer.isEmpty())
			return false;
		double min = Double.POSITIVE_INFINITY;
		double max = Double.NEGATIVE_INFINITY;
		double tMin = 0.0;
		double tMax = 0.0;
		if (visibleOnly) {
			double[] latest = buffer.last();
			if (latest == null)
				return false;
			double latestT = xValue(latest);
			tMax = latestT + style.xMax;
			tMin = latestT + style.xMin;
		}
		Iterator<double[]> it = buffer.ordered();
		while (it.hasNext()) {
			double[] data = it.next();
			double t = xValue(data);
			if (visibleOnly && (t < tMin || t > tMax))
				continue;
			for (int i = 1; i <= nLines; i++) {
				double v = data[i];
				if (!Double.isFinite(v))
					continue;
				if (v < min)
					min = v;
				if (v > max)
					max = v;
			}
		}
		if (min == Double.POSITIVE_INFINITY) {
			if (visibleOnly)
				return updateYRangeFromBuffer(false, addMargin);
			return false;
		}
		if (addMargin) {
			double range = max - min;
			double pad = range * ZOOM_FIT_MARGIN;
			if (pad == 0.0)
				pad = Math.max(1e-6, Math.abs(min) * ZOOM_FIT_MARGIN);
			min -= pad;
			max += pad;
		}
		if (style.logScaleY) {
			if (max <= 0.0)
				return false;
			if (min <= 0.0)
				min = max * 0.01;
		}
		double newMax = Functions.roundUp(max);
		double newMin;
		if (style.logScaleY) {
			newMin = Functions.roundDown(min);
			if (newMin <= 0.0)
				newMin = min;
		} else if (min > 0.0) {
			newMin = roundLinearYMin(min, newMax);
		} else {
			newMin = Functions.roundDown(min);
		}
		style.yMin = newMin;
		style.yMax = newMax;
		return true;
	}

	/**
	 * Update the x-axis range to show all buffered data (latest segment only).
	 * 
	 * @return {@code true} if the range was updated
	 */
	private boolean updateXRangeFromBuffer() {
		if (buffer == null || buffer.isEmpty())
			return false;
		double span = 0.0;
		Iterator<double[]> it = buffer.iterator();
		if (!it.hasNext())
			return false;
		double[] current = it.next();
		if (!it.hasNext())
			return false;
		double[] prev = it.next();
		double[] older = it.hasNext() ? it.next() : null;
		while (true) {
			span += historyStepWidth(current, prev);
			if (older == null)
				break;
			current = prev;
			prev = older;
			older = it.hasNext() ? it.next() : null;
		}
		if (!Double.isFinite(span) || span <= 0.0)
			return false;
		style.xMax = 0.0;
		style.xMin = -span;
		return true;
	}

	/**
	 * Update the x-axis tick label offset to the most recent finite time value.
	 */
	private void updateXTickOffset() {
		if (buffer == null || buffer.isEmpty()) {
			style.xTickOffset = 0.0;
			return;
		}
		Iterator<double[]> it = buffer.iterator();
		while (it.hasNext()) {
			double t = xValue(it.next());
			if (Double.isNaN(t))
				break;
			if (Double.isFinite(t)) {
				style.xTickOffset = t;
				return;
			}
		}
		style.xTickOffset = 0.0;
	}

	/**
	 * Clamp the y-axis range for logarithmic scaling.
	 */
	private void clampLogRange() {
		if (!style.logScaleY)
			return;
		if (style.yMax <= 0.0) {
			noLogY();
			return;
		}
		if (style.yMin <= 0.0)
			style.yMin = 0.01 * style.yMax;
	}
}
