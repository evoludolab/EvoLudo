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

import java.util.Iterator;

import org.evoludo.geom.Point2D;
import org.evoludo.graphics.AbstractGraph.HasTrajectory;
import org.evoludo.graphics.AbstractGraph.Shifting;
import org.evoludo.graphics.AbstractGraph.Zooming;
import org.evoludo.math.Functions;
import org.evoludo.simulator.modules.Module;
import org.evoludo.simulator.views.BasicTooltipProvider;
import org.evoludo.simulator.views.Phase2D;
import org.evoludo.simulator.views.HasPhase2D.Data2Phase;
import org.evoludo.ui.ContextMenu;
import org.evoludo.ui.ContextMenuItem;
import org.evoludo.util.Formatter;
import org.evoludo.util.RingBuffer;

import com.google.gwt.core.client.Duration;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Touch;
import com.google.gwt.event.dom.client.DoubleClickEvent;
import com.google.gwt.event.dom.client.DoubleClickHandler;
import com.google.gwt.event.dom.client.TouchStartEvent;

/**
 * ParaGraph is a specialized 2D parametric/phase-plane graph component for
 * visualizing trajectories produced by a simulation module. Each data sample is
 * expected to be a {@code double[]} whose first element is the time and the
 * remaining elements represent the state vector. ParaGraph maps those state
 * vectors into phase-plane coordinates via a {@link Data2Phase} mapper and
 * draws the resulting parametric curve onto a canvas.
 *
 * <h3>Key responsibilities:</h3>
 * <ul>
 * <li>Maintain a bounded history of parametric data points in a
 * {@code RingBuffer<double[]>}, where each stored array has the time prepended
 * as the first element.</li>
 * <li>Coalesce incoming samples to conserve memory: samples are appended only
 * when the estimated distance (in phase plane units) to the previous stored
 * point exceeds a configurable threshold (derived from a minimum pixel
 * spacing), unless explicitly forced.</li>
 * <li>Handle special time values (NaN) and duplicate times: duplicate times
 * replace the last sample; NaN times mark new trajectory segments and update
 * the recorded initial state accordingly.</li>
 * <li>Map simulation state &lt;-&gt; phase coordinates using a
 * {@link Data2Phase} instance. If the mapper implements
 * {@link BasicTooltipProvider} it will be used as the tooltip provider for
 * parametric tooltips.</li>
 * <li>Render the trajectory with optional start/end markers and custom markers.
 * The drawing code takes into account current axis ranges, scaling, translation
 * and zooming.</li>
 * <li>Provide interactive features:
 * <ul>
 * <li>Double-click or single-touch on the phase plane to convert the clicked
 * phase coordinates back into a simulation initial state and set it on the
 * view.</li>
 * <li>Context menu actions to clear the canvas and toggle autoscaling of
 * axes.</li>
 * <li>Tooltip support for coordinates via a configured tooltip provider.</li>
 * </ul>
 * </li>
 * <li>Autoscale axes to fit the visible data when enabled. Autoscaling computes
 * min/max ranges from the mapper over the current buffer and rounds to sensible
 * tick-friendly bounds. Percent scales are clamped to [0,1] when
 * configured.</li>
 * <li>Export trajectory data to a text format (time, x, y) using the configured
 * mapper and a {@link Formatter} for numeric formatting.</li>
 * </ul>
 *
 * <h3>Rendering details:</h3>
 * <ul>
 * <li>Painting is scheduled with
 * {@link com.google.gwt.core.client.Scheduler#scheduleDeferred} to avoid
 * redundant immediate repaints; a {@code paintScheduled} flag prevents
 * oversubscription of repaint requests.</li>
 * <li>Canvas transform sequence: global scale, translation for view corner and
 * bounds, additional zoom factor, and Y-axis flip to map mathematical
 * coordinates to canvas pixel coordinates.</li>
 * <li>Line width is increased slightly when using semi-transparent trajectory
 * colors to improve visibility.</li>
 * </ul>
 *
 * <h3>Buffering and sampling:</h3>
 * <ul>
 * <li>{@code bufferThreshold} is computed in {@code calcBounds} from
 * {@code MIN_PIXELS} and the current scale and axis ranges. It represents the
 * squared minimal distance (in data units) required between successive stored
 * samples and prevents storing points closer than a few screen pixels.</li>
 * <li>Initial trajectory starting point is stored in {@code init} and updated
 * whenever the buffer begins a new segment (empty buffer) or when a NaN time
 * indicates a new starting point.</li>
 * </ul>
 *
 * <h3>Usage notes:</h3>
 * <ul>
 * <li>Call {@link #setMap(Data2Phase)} to provide a domain-specific mapping
 * between model state and phase-plane coordinates. A default {@code TraitMap}
 * is created lazily if none is set.</li>
 * <li>Use {@link #addData(double, double[], boolean)} to append samples from
 * the simulation. The method clones and prepends the time to the provided array
 * before storing it in the buffer.</li>
 * <li>Enable or disable autoscaling via the context menu or
 * {@link #autoscale()}.</li>
 * </ul>
 *
 * @see Data2Phase
 * @see AbstractGraph
 * @see Phase2D
 * @see Formatter
 * 
 * @author Christoph Hauert
 */
@SuppressWarnings("java:S110")
public class ParaGraph extends AbstractGraph<double[]> implements Zooming, Shifting, HasTrajectory, //
		DoubleClickHandler {

	/**
	 * The starting point of the most recent trajectory.
	 */
	double[] init;

	/**
	 * The map for converting data to phase plane coordinates.
	 */
	Data2Phase map;

	/**
	 * Create new parametric graph for <code>module</code> running in
	 * <code>view</code>.
	 * 
	 * @param view   the view of this graph
	 * @param module the module backing the graph
	 */
	public ParaGraph(Phase2D view, Module<?> module) {
		super(view, module);
	}

	@Override
	protected void onLoad() {
		super.onLoad();
		setStylePrimaryName("evoludo-ParaGraph");
		buffer = new RingBuffer<double[]>(Math.max((int) bounds.getWidth(), DEFAULT_BUFFER_SIZE));
	}

	@Override
	public void activate() {
		super.activate();
		doubleClickHandler = addDoubleClickHandler(this);
		autoscale();
	}

	/**
	 * Set the map for converting data to phase plane coordinates.
	 * 
	 * @param map the conversion map
	 */
	public void setMap(Data2Phase map) {
		if (map == null)
			return;
		this.map = map;
		if (map instanceof BasicTooltipProvider)
			setTooltipProvider((BasicTooltipProvider) map);
	}

	/**
	 * Get the map for converting data to phase plane coordinates.
	 * 
	 * @return the conversion map
	 */
	public Data2Phase getMap() {
		if (map == null)
			setMap(new TraitMap(this));
		return map;
	}

	/**
	 * Add data to the graph. The time {@code t} is prepended to the data as the
	 * first element.
	 * 
	 * @evoludo.impl
	 *               <ul>
	 *               <li>The data array is cloned and the time prepended before
	 *               adding
	 *               it to the buffer.
	 *               <li>In order to conserve memory the data is added only if the
	 *               distance between
	 *               the new data point and the last point in the buffer is larger
	 *               than threshold {@link #bufferThreshold}, unless
	 *               {@code force == true}.
	 *               </ul>
	 * 
	 * @param t     the time of the data
	 * @param data  the data to add
	 * @param force <code>true</code> to force adding the data
	 */
	public void addData(double t, double[] data, boolean force) {
		if (buffer.isEmpty()) {
			handleEmptyBuffer(t, data);
			updateAutoscale();
			return;
		}
		double[] last = buffer.last();
		double lastt = last[0];
		int len = last.length;
		if (Math.abs(t - lastt) < 1e-8) {
			buffer.replace(prependTime2Data(t, data));
			System.arraycopy(data, 0, init, 1, len - 1);
			updateAutoscale();
			return;
		}
		if (Double.isNaN(t)) {
			handleNaNTime(lastt, t, data, len);
			updateAutoscale();
			return;
		}
		if (force || distSq(data, last) > bufferThreshold) {
			buffer.append(prependTime2Data(t, data));
			updateAutoscale();
		}
	}

	/**
	 * Handle the case that the buffer is empty. Add data to the buffer and set the
	 * initial state.
	 * 
	 * @param t    the time at which the data is recorded
	 * @param data the data to add
	 */
	private void handleEmptyBuffer(double t, double[] data) {
		buffer.append(prependTime2Data(t, data));
		double[] last = buffer.last();
		int len = last.length;
		if (init == null || init.length != len)
			init = new double[len];
		System.arraycopy(buffer.last(), 0, init, 0, len);
	}

	/**
	 * Handle the case that the time {@code t} is {@code NaN}. If the last time in
	 * the buffer is also {@code NaN} the last data point is replaced by the new
	 * data, otherwise the new data is appended to the buffer. The initial state is
	 * updated accordingly.
	 * 
	 * @param lastt the time of the last data point in the buffer
	 * @param t     the time of the data
	 * @param data  the data to add
	 * @param len   the length of the data array (including time)
	 */
	private void handleNaNTime(double lastt, double t, double[] data, int len) {
		// new starting point
		if (Double.isNaN(lastt))
			buffer.replace(prependTime2Data(t, data));
		else
			buffer.append(prependTime2Data(t, data));
		System.arraycopy(buffer.last(), 0, init, 0, len);
	}

	/**
	 * Helper method to calculate the distance squared between two vectors.
	 * 
	 * @param vec the first vector
	 * @param buf the second vector
	 * @return the squared distance
	 */
	private double distSq(double[] vec, double[] buf) {
		int dim = vec.length;
		double dist2 = 0.0;
		for (int n = 0; n < dim; n++) {
			double d = vec[n] - buf[n + 1];
			dist2 += d * d;
		}
		return dist2;
	}

	@Override
	public boolean paint(boolean force) {
		if (super.paint(force))
			return true;
		if (!force && !doUpdate())
			return true;
		paintPara(true);
		return false;
	}

	/**
	 * Paint the trajectory in the phase plane. If <code>withMarkers</code> is
	 * <code>true</code> the start and end points of the trajectory are marked with
	 * green and red circles, respectively.
	 * 
	 * @param withMarkers <code>true</code> to mark start and end points
	 */
	private void paintPara(boolean withMarkers) {
		g.save();
		g.scale(scale, scale);
		clearCanvas();
		g.translate(bounds.getX() - viewCorner.getX(), bounds.getY() - viewCorner.getY());
		g.scale(zoomFactor, zoomFactor);
		g.save();
		double h = bounds.getHeight();
		g.translate(0, h);
		g.scale(1.0, -1.0);
		g.beginPath();
		g.rect(0, 0, bounds.getWidth(), h);
		g.clip();
		double xScale = bounds.getWidth() / (style.xMax - style.xMin);
		double yScale = h / (style.yMax - style.yMin);

		Point2D nextPt = new Point2D();
		Point2D currPt = new Point2D();

		// Paint trajectory (may also update autoscale ranges)
		String tC = style.trajColor;
		g.setStrokeStyle(tC);
		// increase line width for trajectories with transparency
		g.setLineWidth((tC.startsWith("rgba") ? 1.25 * style.lineWidth : style.lineWidth));
		drawTrajectory(currPt, nextPt, xScale, yScale);

		// Paint markers if requested
		if (withMarkers)
			drawStartEndMarkers(currPt, xScale, yScale);

		// Paint custom markers if present
		if (markers != null)
			drawCustomMarkers(currPt, xScale, yScale);

		g.restore();
		drawFrame(4, 4);
		g.restore();
	}

	/**
	 * Draw the trajectory stored in the buffer.
	 * 
	 * @param currPt the storage for the current point
	 * @param nextPt the storage for the next point
	 * @param xScale the x-axis scale
	 * @param yScale the y-axis scale
	 */
	private void drawTrajectory(Point2D currPt, Point2D nextPt, double xScale, double yScale) {
		Iterator<double[]> i = buffer.iterator();
		if (!i.hasNext())
			return;

		double[] current = i.next();
		double ct = current[0];
		// current is last point added to buffer
		map.data2Phase(current, currPt);

		while (i.hasNext()) {
			double[] prev = i.next();
			double pt = prev[0];
			map.data2Phase(prev, nextPt);
			if (!Double.isNaN(ct)) {
				strokeLine((nextPt.getX() - style.xMin) * xScale, (nextPt.getY() - style.yMin) * yScale, //
						(currPt.getX() - style.xMin) * xScale, (currPt.getY() - style.yMin) * yScale);
			}
			ct = pt;
			Point2D swap = currPt;
			currPt = nextPt;
			nextPt = swap;
		}
	}

	/**
	 * Draw start and end markers for the trajectory.
	 * 
	 * @param aPt    the storage for a point
	 * @param xScale the x-axis scale
	 * @param yScale the y-axis scale
	 */
	private void drawStartEndMarkers(Point2D aPt, double xScale, double yScale) {
		// mark start point
		map.data2Phase(init, aPt);
		g.setFillStyle(style.startColor);
		fillCircle((aPt.getX() - style.xMin) * xScale, (aPt.getY() - style.yMin) * yScale, style.markerSize);

		// mark end point if available
		if (!buffer.isEmpty()) {
			map.data2Phase(buffer.last(), aPt);
			g.setFillStyle(style.endColor);
			fillCircle((aPt.getX() - style.xMin) * xScale,
					(aPt.getY() - style.yMin) * yScale,
					style.markerSize);
		}
	}

	/**
	 * Draw custom markers stored in {@link #markers}.
	 * 
	 * @param pt     temporary storage for points
	 * @param xScale the x-axis scale
	 * @param yScale the y-axis scale
	 */
	private void drawCustomMarkers(Point2D pt, double xScale, double yScale) {
		int n = 0;
		int nMarkers = markers.size();
		for (double[] mark : markers) {
			map.data2Phase(mark, pt);
			String mcolor = markerColors[n++ % nMarkers];
			if (mark[0] > 0.0) {
				g.setFillStyle(mcolor);
				fillCircle((pt.getX() - style.xMin) * xScale, (pt.getY() - style.yMin) * yScale,
						style.markerSize);
			} else {
				g.setLineWidth(style.lineWidth);
				g.setStrokeStyle(mcolor);
				strokeCircle((pt.getX() - style.xMin) * xScale, (pt.getY() - style.yMin) * yScale,
						style.markerSize);
			}
		}
	}

	/**
	 * Threshold for storing new data point in buffer. Roughly corresponds to the
	 * squared distance between two points that are at least a pixel apart.
	 */
	private double bufferThreshold;

	/**
	 * The minimum distance between two subsequent points in pixels.
	 */
	private static final double MIN_PIXELS = 3.0;

	@Override
	public void calcBounds(int width, int height) {
		super.calcBounds(width, height);
		// store point only if it is estimated to be at least a few pixels from the
		// previous point
		bufferThreshold = MIN_PIXELS * scale * Math.min((style.xMax - style.xMin) / bounds.getWidth(), //
				(style.yMax - style.yMin) / bounds.getHeight());
		bufferThreshold *= bufferThreshold;
	}

	/**
	 * Automatically adjust the range of autoscaled axes to fit the data in the
	 * buffer.
	 */
	public void autoscale() {
		if (!(style.autoscaleX || style.autoscaleY) || buffer == null || buffer.isEmpty())
			return;
		map.reset();
		applyRanges(map.getMinX(buffer), map.getMaxX(buffer),
				map.getMinY(buffer), map.getMaxY(buffer), style.autoscaleX, style.autoscaleY);
	}

	@Override
	public void zoom() {
		super.zoom();
		autoscale();
		paint(true);
	}

	/**
	 * Threshold below which linear axis bounds are snapped to zero.
	 */
	private static final double NEAR_ZERO = 1e-14;

	/**
	 * Reusable point for autoscale calculations.
	 */
	private final Point2D autoscalePoint = new Point2D();

	@Override
	public void clearHistory() {
		super.clearHistory();
		if (map != null)
			map.reset();
	}

	/**
	 * Update autoscale ranges based on the most recent buffered point.
	 */
	private void updateAutoscale() {
		if (!(style.autoscaleX || style.autoscaleY) || buffer == null || buffer.isEmpty())
			return;
		double[] last = buffer.last();
		map.data2Phase(last, autoscalePoint);
		if (buffer.getSize() <= 1) {
			map.reset();
			applyRanges(autoscalePoint.getX(), autoscalePoint.getX(),
					autoscalePoint.getY(), autoscalePoint.getY(), style.autoscaleX, style.autoscaleY);
			return;
		}
		applyRanges(Math.min(style.xMin, autoscalePoint.getX()),
				Math.max(style.xMax, autoscalePoint.getX()),
				Math.min(style.yMin, autoscalePoint.getY()),
				Math.max(style.yMax, autoscalePoint.getY()), style.autoscaleX, style.autoscaleY);
	}

	/**
	 * Apply axis ranges with axis-specific constraints.
	 *
	 * @param minX   minimum x value
	 * @param maxX   maximum x value
	 * @param minY   minimum y value
	 * @param maxY   maximum y value
	 * @param applyX whether to update the x-axis range
	 * @param applyY whether to update the y-axis range
	 */
	private void applyRanges(double minX, double maxX, double minY, double maxY, boolean applyX, boolean applyY) {
		if (applyX) {
			double min = minX;
			double max = maxX;
			if (min == max) {
				min *= 0.99;
				max /= 0.99;
			}
			style.xMin = Functions.roundDown(min);
			style.xMax = Functions.roundUp(max);
			applyXConstraints();
		}
		if (applyY) {
			double min = minY;
			double max = maxY;
			if (min == max) {
				min *= 0.99;
				max /= 0.99;
			}
			style.yMin = Functions.roundDown(min);
			style.yMax = Functions.roundUp(max);
			applyYConstraints();
		}
	}

	/**
	 * Set axis ranges to fit the full buffered trajectory and reset zoom factor.
	 */
	private void zoomToFit() {
		if (buffer == null || buffer.isEmpty())
			return;
		super.zoom();
		map.reset();
		applyRanges(map.getMinX(buffer), map.getMaxX(buffer),
				map.getMinY(buffer), map.getMaxY(buffer), true, true);
	}

	/**
	 * Clamp and snap the x-axis range according to axis settings.
	 */
	private void applyXConstraints() {
		if (style.percentX) {
			style.xMin = clampPercentMin(style.xMin);
			style.xMax = clampPercentMax(style.xMax);
		} else {
			style.xMin = snapNearZero(style.xMin);
		}
	}

	/**
	 * Clamp and snap the y-axis range according to axis settings.
	 */
	private void applyYConstraints() {
		if (style.percentY) {
			style.yMin = clampPercentMin(style.yMin);
			style.yMax = clampPercentMax(style.yMax);
		} else if (style.logScaleY) {
			style.yMin = clampLogMin(style.yMin);
		} else {
			style.yMin = snapNearZero(style.yMin);
		}
	}

	/**
	 * Snap small linear values to zero.
	 *
	 * @param value input value
	 * @return snapped value
	 */
	private double snapNearZero(double value) {
		if (Math.abs(value) < NEAR_ZERO)
			return 0.0;
		return value;
	}

	/**
	 * Clamp the minimum of a percent axis.
	 *
	 * @param value input value
	 * @return clamped value
	 */
	private double clampPercentMin(double value) {
		value = Math.max(0.0, value);
		if (value < NEAR_ZERO)
			value = 0.0;
		return value;
	}

	/**
	 * Clamp the maximum of a percent axis.
	 *
	 * @param value input value
	 * @return clamped value
	 */
	private double clampPercentMax(double value) {
		value = Math.min(1.0, value);
		if (Math.abs(1.0 - value) < NEAR_ZERO)
			value = 1.0;
		return value;
	}

	/**
	 * Clamp the minimum of a log-scaled axis.
	 *
	 * @param value input value
	 * @return clamped value
	 */
	private double clampLogMin(double value) {
		if (!Double.isFinite(value) || value <= 0.0 || value < NEAR_ZERO)
			return NEAR_ZERO;
		return value;
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
		paintPara(false);
		g = bak;
	}

	@Override
	public void onDoubleClick(DoubleClickEvent event) {
		double x = event.getX() - style.frameWidth;
		double y = event.getY();
		processInitXY(x, y);
	}

	/**
	 * Helper method to convert screen coordinates into an initial configuration and
	 * set the controller's initial state.
	 * 
	 * @param x the {@code x}-coordinate on screen
	 * @param y the {@code y}-coordinate on screen
	 */
	private void processInitXY(double x, double y) {
		int sx = (int) ((viewCorner.getX() + x - bounds.getX()) / zoomFactor + 0.5);
		int sy = (int) ((viewCorner.getY() + y - bounds.getY()) / zoomFactor + 0.5);
		if (!inside(sx, sy))
			return;
		double ux = style.xMin + sx / bounds.getWidth() * (style.xMax - style.xMin);
		double uy = style.yMin + (1.0 - sy / bounds.getHeight()) * (style.yMax - style.yMin);
		double[] last = buffer.last();
		int len = last.length;
		double[] state = new double[len - 1];
		System.arraycopy(last, 1, state, 0, len - 1);
		map.phase2Data(new Point2D(ux, uy), state);
		view.setInitialState(state);
	}

	// CHECK!
	@Override
	public void onTouchStart(TouchStartEvent event) {
		super.onTouchStart(event);
		JsArray<Touch> touches = event.getTouches();
		if (Duration.currentTimeMillis() - touchEndTime > 250.0 || touches.length() > 1)
			// single tap or multiple touches
			return;

		Touch touch = touches.get(0);
		processInitXY(touch.getRelativeX(element), touch.getRelativeY(element));
		event.preventDefault();
	}

	@Override
	public String getTooltipAt(int x, int y) {
		int sx = (int) ((viewCorner.getX() + x - bounds.getX()) / zoomFactor + 0.5);
		int sy = (int) ((viewCorner.getY() + y - bounds.getY()) / zoomFactor + 0.5);
		if (!inside(sx, sy))
			return null;
		double ux = style.xMin + sx / bounds.getWidth() * (style.xMax - style.xMin);
		double uy = style.yMin + (1.0 - sy / bounds.getHeight()) * (style.yMax - style.yMin);
		if (tooltipProvider instanceof TooltipProvider.Parametric)
			return ((TooltipProvider.Parametric) tooltipProvider).getTooltipAt(this, ux, uy);
		if (tooltipProvider != null)
			return tooltipProvider.getTooltipAt(ux, uy);
		return null;
	}

	/**
	 * Check if point (in user coordinates but not yet scaled to axes) lies inside
	 * of phase plane.
	 * 
	 * @param x <code>x</code>-coordinate of point
	 * @param y <code>y</code>-coordinate of point
	 * @return <code>true</code> if inside
	 */
	private boolean inside(double x, double y) {
		return !(x < 0 || y < 0 || x > bounds.getWidth() || y > bounds.getHeight());
	}

	/**
	 * The context menu item to clear the canvas.
	 */
	private ContextMenuItem clearMenu;

	/**
	 * The context menu item to zoom to fit the buffered data.
	 */
	private ContextMenuItem zoomFitMenu;

	@Override
	protected void populateZoomMenu(ContextMenu menu) {
		super.populateZoomMenu(menu);
		if (zoomFitMenu == null) {
			zoomFitMenu = new ContextMenuItem("Zoom to fit", () -> {
				zoomToFit();
				paint(true);
			});
		}
		menu.add(zoomFitMenu);
	}

	@Override
	public void populateContextMenuAt(ContextMenu menu, int x, int y) {
		// add menu to clear canvas
		if (clearMenu == null) {
			clearMenu = new ContextMenuItem("Clear", () -> {
				clearHistory();
				paint(true);
			});
		}
		menu.add(clearMenu);
		super.populateContextMenuAt(menu, x, y);
	}

	@Override
	public void exportTrajectory(StringBuilder export) {
		if (buffer.isEmpty())
			return;
		// extract the parametric data from buffer
		export.append("# time, " + style.xLabel + ", " + style.yLabel + "\n");
		Point2D point = new Point2D();
		Iterator<double[]> entry = buffer.ordered();
		while (entry.hasNext()) {
			double[] data = entry.next();
			map.data2Phase(data, point);
			export.append(Formatter.format(data[0], 8))
					.append(", ")
					.append(Formatter.format(point.getX(), 8))
					.append(", ")
					.append(Formatter.format(point.getY(), 8))
					.append("\n");
		}
	}
}
