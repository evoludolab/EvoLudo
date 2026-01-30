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
import org.evoludo.geom.Rectangle2D;
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
		ViewState state = currentViewState();
		if (!state.valid) {
			g.restore();
			return;
		}
		clampViewCorner(state);
		double w = state.w;
		double h = state.h;
		g.save();
		g.translate(bounds.getX(), bounds.getY());
		g.beginPath();
		g.rect(0, 0, w, h);
		g.clip();
		g.translate(-viewCorner.getX(), -viewCorner.getY());
		double xScale = (w / state.baseRangeX) * state.zoomX;
		double yScale = (h / state.baseRangeY) * state.zoomY;

		Point2D nextPt = new Point2D();
		Point2D currPt = new Point2D();

		// Paint trajectory (may also update autoscale ranges)
		String tC = style.trajColor;
		g.setStrokeStyle(tC);
		g.setLineWidth(style.lineWidth);
		drawTrajectory(currPt, nextPt, xScale, yScale);

		// Paint markers if requested (use screen coordinates to keep circles round)
		if (withMarkers)
			drawStartEndMarkers(currPt, xScale, yScale);

		// Paint custom markers if present
		if (markers != null)
			drawCustomMarkers(currPt, xScale, yScale);
		g.restore();
		g.save();
		g.translate(bounds.getX(), bounds.getY());
		drawFrameForView(state, 4, 4);
		g.restore();
		g.restore();
	}

	/**
	 * Draw the frame using the visible ranges implied by the current zoom and
	 * shift.
	 *
	 * @param state   the current view state
	 * @param xLevels number of x-axis levels
	 * @param yLevels number of y-axis levels
	 */
	private void drawFrameForView(ViewState state, int xLevels, int yLevels) {
		if (!state.valid) {
			drawFrame(xLevels, yLevels);
			return;
		}
		double baseXMin = state.baseXMin;
		double baseXMax = state.baseXMin + state.baseRangeX;
		double baseYMin = state.baseYMin;
		double baseYMax = state.baseYMin + state.baseRangeY;
		double visibleRangeX = state.baseRangeX / state.zoomX;
		double visibleRangeY = state.baseRangeY / state.zoomY;
		boolean clampX = style.percentX && visibleRangeX > domain.getWidth();
		boolean clampY = style.percentY && visibleRangeY > domain.getHeight();
		double plotW = state.w;
		double plotH = state.h;
		if (clampX)
			plotW = state.w * domain.getWidth() / visibleRangeX;
		if (clampY)
			plotH = state.h * domain.getHeight() / visibleRangeY;
		double plotX = 0.5 * (state.w - plotW);
		double plotY = 0.5 * (state.h - plotH);
		Rectangle2D boundsBase = new Rectangle2D(bounds);
		applyVisibleRanges(state);
		if (clampX) {
			style.xMin = domain.getX();
			style.xMax = domain.getX() + domain.getWidth();
		}
		if (clampY) {
			style.yMin = domain.getY();
			style.yMax = domain.getY() + domain.getHeight();
		}
		bounds.set(0.0, 0.0, plotW, plotH);
		g.save();
		g.translate(plotX, plotY);
		drawFrame(xLevels, yLevels);
		g.restore();
		bounds.set(boundsBase.getX(), boundsBase.getY(), boundsBase.getWidth(), boundsBase.getHeight());
		style.xMin = baseXMin;
		style.xMax = baseXMax;
		style.yMin = baseYMin;
		style.yMax = baseYMax;
	}

	/**
	 * Adjust axis ranges to match the current zoom and shift.
	 *
	 * @param state the current view state
	 */
	private void applyVisibleRanges(ViewState state) {
		if (!state.valid)
			return;
		double cornerX = viewCorner == null ? 0.0 : viewCorner.getX();
		double cornerY = viewCorner == null ? 0.0 : viewCorner.getY();
		double xMin = state.baseXMin + (cornerX / (state.zoomX * state.w)) * state.baseRangeX;
		double yMax = (state.baseYMin + state.baseRangeY)
				- (cornerY / (state.zoomY * state.h)) * state.baseRangeY;
		double rangeX = state.baseRangeX / state.zoomX;
		double rangeY = state.baseRangeY / state.zoomY;
		style.xMin = xMin;
		style.xMax = xMin + rangeX;
		style.yMax = yMax;
		style.yMin = yMax - rangeY;
	}

	/**
	 * Update and return the cached view state for the current axis ranges.
	 *
	 * @return the updated view state
	 */
	private ViewState currentViewState() {
		return viewState.update(style.xMin, style.xMax, style.yMin, style.yMax, zoomFactor);
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
				strokeLine((nextPt.getX() - style.xMin) * xScale, (style.yMax - nextPt.getY()) * yScale, //
						(currPt.getX() - style.xMin) * xScale, (style.yMax - currPt.getY()) * yScale);
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
		drawMarkerCircle(aPt, xScale, yScale, true, style.markerSize);

		// mark end point if available
		if (!buffer.isEmpty()) {
			map.data2Phase(buffer.last(), aPt);
			g.setFillStyle(style.endColor);
			drawMarkerCircle(aPt, xScale, yScale, true, style.markerSize);
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
				drawMarkerCircle(pt, xScale, yScale, true, style.markerSize);
			} else {
				g.setLineWidth(style.lineWidth);
				g.setStrokeStyle(mcolor);
				drawMarkerCircle(pt, xScale, yScale, false, style.markerSize);
			}
		}
	}

	/**
	 * Draw a marker circle in screen coordinates to avoid distortion from scaling.
	 *
	 * @param pt     marker location in data coordinates
	 * @param xScale base x-axis scale
	 * @param yScale base y-axis scale
	 * @param fill   {@code true} for filled markers, {@code false} for outline
	 * @param radius marker radius in screen pixels
	 */
	private void drawMarkerCircle(Point2D pt, double xScale, double yScale, boolean fill, double radius) {
		double sx = (pt.getX() - style.xMin) * xScale;
		double sy = (style.yMax - pt.getY()) * yScale;
		if (fill)
			fillCircle(sx, sy, radius);
		else
			strokeCircle(sx, sy, radius);
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

	@Override
	public void zoom(double zoom) {
		if (zoom <= 0.0) {
			zoom();
			return;
		}
		int cx = (int) Math.round(bounds.getX() + bounds.getWidth() * 0.5);
		int cy = (int) Math.round(bounds.getY() + bounds.getHeight() * 0.5);
		zoom(zoom, cx, cy);
	}

	@Override
	public void zoom(double zoom, int x, int y) {
		if (hasMessage)
			return;
		if (zoom <= 0.0) {
			zoom();
			return;
		}
		ViewState state = currentViewState();
		if (!state.valid)
			return;
		double minZoomX = computeMinZoom(state.baseRangeX, domain.getWidth());
		double minZoomY = computeMinZoom(state.baseRangeY, domain.getHeight());
		double localX = x - bounds.getX();
		double localY = y - bounds.getY();
		double ux = state.dataXFromLocal(localX);
		double uy = state.dataYFromLocal(localY);
		double minZoom = Math.min(minZoomX, minZoomY);
		if (style.percentX && style.percentY && state.w > 0.0 && state.h > 0.0) {
			double baseScaleX = state.w / state.baseRangeX;
			double baseScaleY = state.h / state.baseRangeY;
			double baseScale = Math.min(baseScaleX, baseScaleY);
			double pMin = Math.min(state.w / domain.getWidth(), state.h / domain.getHeight());
			double factorMin = pMin / baseScale;
			minZoom = Math.max(minZoom, factorMin);
		}
		double oldZoom = zoomFactor;
		double newZoom = Math.min(Zooming.ZOOM_MAX, Math.max(minZoom, zoomFactor * zoom));
		if (Math.abs(newZoom - zoomFactor) < 1e-8)
			return;
		double relX = (ux - state.baseXMin) / state.baseRangeX;
		double baseYMax = state.baseYMin + state.baseRangeY;
		double relY = (baseYMax - uy) / state.baseRangeY;
		double baseXMax = state.baseXMin + state.baseRangeX;
		state.update(state.baseXMin, baseXMax, state.baseYMin, baseYMax, newZoom);
		double newViewCornerX = relX * state.w * state.zoomX - localX;
		double newViewCornerY = relY * state.h * state.zoomY - localY;
		setViewCorner(state, newViewCornerX, newViewCornerY);
		zoomFactor = newZoom;
		if (zoomInertiaTimer != null && zoomInertiaTimer.isRunning()) {
			double dz = newZoom - oldZoom;
			element.addClassName(dz > 0 ? CURSOR_ZOOM_IN_CLASS : CURSOR_ZOOM_OUT_CLASS);
		}
		paint(true);
	}

	@Override
	public void shift(int dx, int dy) {
		if (hasMessage)
			return;
		ViewState state = currentViewState();
		if (!state.valid)
			return;
		double newViewCornerX = viewCorner.getX() + dx;
		double newViewCornerY = viewCorner.getY() + dy;
		setViewCorner(state, newViewCornerX, newViewCornerY);
		paint(true);
	}

	/**
	 * Minimum zoom level to avoid unbounded ranges.
	 */
	private static final double MIN_ZOOM_FACTOR = 1e-6;

	/**
	 * Compute minimum zoom factor for a base range and domain width.
	 *
	 * @param baseRange   axis range in data units
	 * @param domainWidth width of the constrained domain
	 * @return minimum zoom factor that keeps the view within the domain
	 */
	private double computeMinZoom(double baseRange, double domainWidth) {
		double minZoom = MIN_ZOOM_FACTOR;
		if (Double.isFinite(domainWidth) && domainWidth > 0.0) {
			double required = baseRange / domainWidth;
			if (required < 1.0)
				minZoom = Math.max(minZoom, required);
			else
				minZoom = Math.max(minZoom, 1.0);
		}
		return minZoom;
	}

	/**
	 * Reusable rectangle for the domain bounds.
	 */
	private final Rectangle2D domain = new Rectangle2D();

	/**
	 * Reusable view state for zooming and shifting calculations.
	 */
	private final ViewState viewState = new ViewState();

	/**
	 * Return the current domain limits for zooming and shifting.
	 *
	 * @return the domain rectangle (origin at minimum values)
	 */
	private Rectangle2D updateDomain() {
		double minX = 0.0;
		double minY = style.logScaleY ? NEAR_ZERO : 0.0;
		double maxX = style.percentX ? 1.0 : Double.POSITIVE_INFINITY;
		double maxY = style.percentY ? 1.0 : Double.POSITIVE_INFINITY;
		return domain.set(minX, minY, maxX - minX, maxY - minY);
	}

	/**
	 * Helper that caches derived view quantities for the current bounds, ranges,
	 * and zoom factor.
	 */
	private final class ViewState {

		/**
		 * Private constructor for the ViewState class. Prevents external instantiation
		 * of ViewState objects, ensuring that ViewState instances can only be created
		 * within the enclosing class. This follows the typical use for inner classes
		 * that represent internal state or configuration but remain hidden from
		 * outside.
		 */
		private ViewState() {
			// No initialization needed
		}

		/**
		 * Cached frame width in pixels.
		 */
		private double w;

		/**
		 * Cached frame height in pixels.
		 */
		private double h;

		/**
		 * Cached minimum x-axis value for the base range.
		 */
		private double baseXMin;

		/**
		 * Cached minimum y-axis value for the base range.
		 */
		private double baseYMin;

		/**
		 * Cached x-axis range for the base view.
		 */
		private double baseRangeX;

		/**
		 * Cached y-axis range for the base view.
		 */
		private double baseRangeY;

		/**
		 * Cached x-axis zoom factor.
		 */
		private double zoomX;

		/**
		 * Cached y-axis zoom factor.
		 */
		private double zoomY;

		/**
		 * Cached validity flag.
		 */
		private boolean valid;

		/**
		 * Update the cached view state for the provided base ranges and zoom factor.
		 *
		 * @param baseXMin minimum x-axis value
		 * @param baseXMax maximum x-axis value
		 * @param baseYMin minimum y-axis value
		 * @param baseYMax maximum y-axis value
		 * @param factor   zoom factor to apply
		 * @return this view state
		 */
		private ViewState update(double baseXMin, double baseXMax, double baseYMin, double baseYMax, double factor) {
			this.baseXMin = baseXMin;
			this.baseYMin = baseYMin;
			w = bounds.getWidth();
			h = bounds.getHeight();
			baseRangeX = baseXMax - baseXMin;
			baseRangeY = baseYMax - baseYMin;
			valid = !(w <= 0.0 || h <= 0.0 || baseRangeX == 0.0 || baseRangeY == 0.0);
			if (!valid)
				return this;
			computeZoom(factor);
			if (zoomX == 0.0 || zoomY == 0.0) {
				valid = false;
				return this;
			}
			return this;
		}

		/**
		 * Convert a local x-coordinate into data coordinates.
		 *
		 * @param localX x-coordinate relative to the frame
		 * @return x value in data coordinates
		 */
		private double dataXFromLocal(double localX) {
			double cornerX = viewCorner == null ? 0.0 : viewCorner.getX();
			double sx = (cornerX + localX) / zoomX;
			return baseXMin + (sx / w) * baseRangeX;
		}

		/**
		 * Convert a local y-coordinate into data coordinates.
		 *
		 * @param localY y-coordinate relative to the frame
		 * @return y value in data coordinates
		 */
		private double dataYFromLocal(double localY) {
			double cornerY = viewCorner == null ? 0.0 : viewCorner.getY();
			double sy = (cornerY + localY) / zoomY;
			return baseYMin + (1.0 - sy / h) * baseRangeY;
		}

		/**
		 * Compute a view corner value that aligns the visible minimum with
		 * {@code desiredMin}.
		 *
		 * @param desiredMin desired visible minimum
		 * @return view corner x-position in pixels
		 */
		private double viewCornerXForMin(double desiredMin) {
			return (desiredMin - baseXMin) * (zoomX * w) / baseRangeX;
		}

		/**
		 * Compute a view corner value that aligns the visible maximum with
		 * {@code desiredMax}.
		 *
		 * @param desiredMax desired visible maximum
		 * @return view corner y-position in pixels
		 */
		private double viewCornerYForMax(double desiredMax) {
			double baseYMax = baseYMin + baseRangeY;
			return (baseYMax - desiredMax) * (zoomY * h) / baseRangeY;
		}

		/**
		 * Compute per-axis zoom factors after applying domain constraints.
		 *
		 * @param factor requested zoom factor
		 */
		private void computeZoom(double factor) {
			updateDomain();
			zoomX = Math.max(factor, computeMinZoom(baseRangeX, domain.getWidth()));
			zoomY = Math.max(factor, computeMinZoom(baseRangeY, domain.getHeight()));
			if (style.percentX && style.percentY && w > 0.0 && h > 0.0) {
				// Keep equal pixel-per-unit scale on both axes for frequency plots.
				double baseScaleX = w / baseRangeX;
				double baseScaleY = h / baseRangeY;
				double baseScale = Math.min(baseScaleX, baseScaleY);
				double p = baseScale * factor;
				double minPX = baseScaleX * computeMinZoom(baseRangeX, domain.getWidth());
				double minPY = baseScaleY * computeMinZoom(baseRangeY, domain.getHeight());
				double maxPX = baseScaleX * Zooming.ZOOM_MAX;
				double maxPY = baseScaleY * Zooming.ZOOM_MAX;
				p = Math.max(p, Math.min(minPX, minPY));
				p = Math.min(p, Math.min(maxPX, maxPY));
				zoomX = p * baseRangeX / w;
				zoomY = p * baseRangeY / h;
			}
		}
	}

	/**
	 * Clamp the current view corner to keep the visible range inside the domain.
	 *
	 * @param state the current view state
	 */
	private void clampViewCorner(ViewState state) {
		if (viewCorner == null)
			return;
		setViewCorner(state, viewCorner.getX(), viewCorner.getY());
	}

	/**
	 * Clamp and apply the view corner for the provided state.
	 *
	 * @param state   the current view state
	 * @param cornerX candidate x view corner
	 * @param cornerY candidate y view corner
	 */
	private void setViewCorner(ViewState state, double cornerX, double cornerY) {
		if (viewCorner == null)
			return;
		double clampedX = clampViewX(cornerX, state.zoomX, state.w, state.baseRangeX);
		double clampedY = clampViewY(cornerY, state.zoomY, state.h, state.baseRangeY);
		viewCorner.set(clampedX, clampedY);
	}

	/**
	 * Clamp a horizontal view corner to keep the visible range inside the domain.
	 *
	 * @param corner     candidate view corner (pixels)
	 * @param zoom       zoom factor for x-axis
	 * @param width      frame width in pixels
	 * @param baseRangeX base x-axis range
	 * @return clamped view corner value
	 */
	private double clampViewX(double corner, double zoom, double width, double baseRangeX) {
		double rangeX = baseRangeX / zoom;
		double scale = zoom * width / baseRangeX;
		double min = Double.NEGATIVE_INFINITY;
		double max = Double.POSITIVE_INFINITY;
		double domainMin = domain.getX();
		double domainMax = domain.getX() + domain.getWidth();
		if (Double.isFinite(domainMin))
			min = (domainMin - style.xMin) * scale;
		if (Double.isFinite(domainMax))
			max = (domainMax - style.xMin - rangeX) * scale;
		if (min > max)
			return 0.5 * (min + max);
		return Math.min(max, Math.max(min, corner));
	}

	/**
	 * Clamp a vertical view corner to keep the visible range inside the domain.
	 *
	 * @param corner     candidate view corner (pixels)
	 * @param zoom       zoom factor for y-axis
	 * @param height     frame height in pixels
	 * @param baseRangeY base y-axis range
	 * @return clamped view corner value
	 */
	private double clampViewY(double corner, double zoom, double height, double baseRangeY) {
		double rangeY = baseRangeY / zoom;
		double scale = zoom * height / baseRangeY;
		double min = Double.NEGATIVE_INFINITY;
		double max = Double.POSITIVE_INFINITY;
		double domainMin = domain.getY();
		double domainMax = domain.getY() + domain.getHeight();
		if (Double.isFinite(domainMax))
			min = (style.yMax - domainMax) * scale;
		if (Double.isFinite(domainMin))
			max = (style.yMax - domainMin - rangeY) * scale;
		if (min > max)
			return 0.5 * (min + max);
		return Math.min(max, Math.max(min, corner));
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
		double minX = style.xMin;
		double maxX = style.xMax;
		double minY = style.yMin;
		double maxY = style.yMax;
		boolean expandX = false;
		boolean expandY = false;
		if (style.autoscaleX) {
			if (autoscalePoint.getX() < minX) {
				minX = autoscalePoint.getX();
				expandX = true;
			} else if (autoscalePoint.getX() > maxX) {
				maxX = autoscalePoint.getX();
				expandX = true;
			}
		}
		if (style.autoscaleY) {
			if (autoscalePoint.getY() < minY) {
				minY = autoscalePoint.getY();
				expandY = true;
			} else if (autoscalePoint.getY() > maxY) {
				maxY = autoscalePoint.getY();
				expandY = true;
			}
		}
		if (expandX || expandY)
			applyRanges(minX, maxX, minY, maxY, expandX, expandY);
		ensureAutoscaleVisible();
	}

	/**
	 * Shift the view corner to keep the latest autoscale point visible without
	 * shrinking the current ranges.
	 */
	private void ensureAutoscaleVisible() {
		if (viewCorner == null)
			return;
		ViewState state = currentViewState();
		if (!state.valid)
			return;
		double newViewCornerX = viewCorner.getX();
		double newViewCornerY = viewCorner.getY();
		boolean shift = false;
		double visibleRangeX = state.baseRangeX / state.zoomX;
		double visibleRangeY = state.baseRangeY / state.zoomY;
		double cornerX = viewCorner.getX();
		double cornerY = viewCorner.getY();
		double xMinVisible = state.baseXMin + (cornerX / (state.zoomX * state.w)) * state.baseRangeX;
		double xMaxVisible = xMinVisible + visibleRangeX;
		double baseYMax = state.baseYMin + state.baseRangeY;
		double yMaxVisible = baseYMax - (cornerY / (state.zoomY * state.h)) * state.baseRangeY;
		double yMinVisible = yMaxVisible - visibleRangeY;
		if (style.autoscaleX) {
			if (autoscalePoint.getX() < xMinVisible) {
				newViewCornerX = state.viewCornerXForMin(autoscalePoint.getX());
				shift = true;
			} else if (autoscalePoint.getX() > xMaxVisible) {
				double desiredMin = autoscalePoint.getX() - visibleRangeX;
				newViewCornerX = state.viewCornerXForMin(desiredMin);
				shift = true;
			}
		}
		if (style.autoscaleY) {
			if (autoscalePoint.getY() < yMinVisible) {
				double desiredMax = autoscalePoint.getY() + visibleRangeY;
				newViewCornerY = state.viewCornerYForMax(desiredMax);
				shift = true;
			} else if (autoscalePoint.getY() > yMaxVisible) {
				newViewCornerY = state.viewCornerYForMax(autoscalePoint.getY());
				shift = true;
			}
		}
		if (!shift)
			return;
		setViewCorner(state, newViewCornerX, newViewCornerY);
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
			if (style.percentX) {
				min = clampPercentMin(min);
				max = clampPercentMax(max);
				if (min >= max) {
					min = 0.0;
					max = 1.0;
				}
				style.xMin = min;
				style.xMax = max;
			} else {
				style.xMin = Functions.roundDown(min);
				style.xMax = Functions.roundUp(max);
			}
			applyXConstraints();
		}
		if (applyY) {
			double min = minY;
			double max = maxY;
			if (min == max) {
				min *= 0.99;
				max /= 0.99;
			}
			if (style.percentY) {
				min = clampPercentMin(min);
				max = clampPercentMax(max);
				if (min >= max) {
					min = 0.0;
					max = 1.0;
				}
				style.yMin = min;
				style.yMax = max;
			} else {
				style.yMin = Functions.roundDown(min);
				style.yMax = Functions.roundUp(max);
			}
			applyYConstraints();
		}
		if (applyX && applyY && !style.logScaleY && !(style.percentX && style.percentY))
			enforceEqualAxisScale();
	}

	/**
	 * Enforce equal units-per-pixel scale on both axes for linear plots.
	 */
	private void enforceEqualAxisScale() {
		double w = bounds.getWidth();
		double h = bounds.getHeight();
		if (w <= 0.0 || h <= 0.0)
			return;
		double rangeX = style.xMax - style.xMin;
		double rangeY = style.yMax - style.yMin;
		if (rangeX <= 0.0 || rangeY <= 0.0)
			return;
		updateDomain();
		double domainMinX = domain.getX();
		double domainMaxX = domain.getX() + domain.getWidth();
		double domainMinY = domain.getY();
		double domainMaxY = domain.getY() + domain.getHeight();
		boolean clampX = Double.isFinite(domain.getWidth());
		boolean clampY = Double.isFinite(domain.getHeight());
		double scaleX = rangeX / w;
		double scaleY = rangeY / h;
		if (Math.abs(scaleX - scaleY) < 1e-12)
			return;
		if (scaleX > scaleY) {
			double targetRangeY = scaleX * h;
			if (clampY && targetRangeY > domain.getHeight()) {
				targetRangeY = domain.getHeight();
				setCenteredY(targetRangeY, clampY, domainMinY, domainMaxY);
				double targetScale = targetRangeY / h;
				double targetRangeX = targetScale * w;
				setCenteredX(targetRangeX, clampX, domainMinX, domainMaxX);
				return;
			}
			setCenteredY(targetRangeY, clampY, domainMinY, domainMaxY);
			return;
		}
		double targetRangeX = scaleY * w;
		if (clampX && targetRangeX > domain.getWidth()) {
			targetRangeX = domain.getWidth();
			setCenteredX(targetRangeX, clampX, domainMinX, domainMaxX);
			double targetScale = targetRangeX / w;
			double targetRangeY = targetScale * h;
			setCenteredY(targetRangeY, clampY, domainMinY, domainMaxY);
			return;
		}
		setCenteredX(targetRangeX, clampX, domainMinX, domainMaxX);
	}

	/**
	 * Center and optionally clamp the x-range.
	 *
	 * @param range     target range
	 * @param clamp     whether to clamp to the domain
	 * @param domainMin domain minimum
	 * @param domainMax domain maximum
	 */
	private void setCenteredX(double range, boolean clamp, double domainMin, double domainMax) {
		double cx = 0.5 * (style.xMin + style.xMax);
		double min = cx - 0.5 * range;
		double max = cx + 0.5 * range;
		if (clamp) {
			if (min < domainMin) {
				max += domainMin - min;
				min = domainMin;
			}
			if (max > domainMax) {
				min -= max - domainMax;
				max = domainMax;
			}
			if (min < domainMin) {
				min = domainMin;
			}
		}
		style.xMin = snapNearZero(min);
		style.xMax = max;
	}

	/**
	 * Center and optionally clamp the y-range.
	 *
	 * @param range     target range
	 * @param clamp     whether to clamp to the domain
	 * @param domainMin domain minimum
	 * @param domainMax domain maximum
	 */
	private void setCenteredY(double range, boolean clamp, double domainMin, double domainMax) {
		double cy = 0.5 * (style.yMin + style.yMax);
		double min = cy - 0.5 * range;
		double max = cy + 0.5 * range;
		if (clamp) {
			if (min < domainMin) {
				max += domainMin - min;
				min = domainMin;
			}
			if (max > domainMax) {
				min -= max - domainMax;
				max = domainMax;
			}
			if (min < domainMin) {
				min = domainMin;
			}
		}
		style.yMin = snapNearZero(min);
		style.yMax = max;
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
			style.xMin = Math.max(0.0, snapNearZero(style.xMin));
			style.xMax = Math.max(0.0, style.xMax);
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
			style.yMin = Math.max(0.0, snapNearZero(style.yMin));
			style.yMax = Math.max(0.0, style.yMax);
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
		double localX = x - bounds.getX();
		double localY = y - bounds.getY();
		if (!inside(localX, localY))
			return;
		ViewState state = currentViewState();
		if (!state.valid)
			return;
		double ux = state.dataXFromLocal(localX);
		double uy = state.dataYFromLocal(localY);
		double[] last = buffer.last();
		int len = last.length;
		double[] initState = new double[len - 1];
		System.arraycopy(last, 1, initState, 0, len - 1);
		map.phase2Data(new Point2D(ux, uy), initState);
		view.setInitialState(initState);
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
		double localX = x - bounds.getX();
		double localY = y - bounds.getY();
		if (!inside(localX, localY))
			return null;
		ViewState state = currentViewState();
		if (!state.valid)
			return null;
		double ux = state.dataXFromLocal(localX);
		double uy = state.dataYFromLocal(localY);
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
