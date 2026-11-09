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

import java.util.Arrays;
import java.util.Iterator;

import org.evoludo.geom.Point2D;
import org.evoludo.graphics.AbstractGraph.HasTrajectory;
import org.evoludo.graphics.AbstractGraph.Shifting;
import org.evoludo.graphics.AbstractGraph.Zooming;
import org.evoludo.simulator.models.Model;
import org.evoludo.simulator.modules.Module;
import org.evoludo.simulator.views.BasicTooltipProvider;
import org.evoludo.simulator.views.Phase2D;
import org.evoludo.simulator.views.HasPhase2D.Data2Phase;
import org.evoludo.ui.ContextMenu;
import org.evoludo.ui.ContextMenuItem;
import org.evoludo.util.Formatter;
import org.evoludo.util.RingBuffer;

import com.google.gwt.dom.client.Touch;
import com.google.gwt.event.dom.client.DoubleClickEvent;
import com.google.gwt.event.dom.client.DoubleClickHandler;

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
 * <li>Coalesce incoming samples to conserve memory using a display-agnostic
 * heuristic. Samples are appended when normalized state-space distance exceeds
 * a threshold or trajectory curvature indicates a significant turn, unless
 * explicitly forced.</li>
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
 * <li>Autoscale axes independently to fit all buffered data when enabled. The
 * ranges expand as needed and shrink if the data occupies only a small part of
 * them, see {@link GraphStyle#fitXRange(double, double, boolean)}. As for
 * other graphs, zooming and shifting move the view, while the axes show the
 * visible ranges, but the view may extend beyond the fitted ranges, see
 * {@link #setViewCorner(double, double, double)}. Zooming or shifting disables
 * autoscaling, and without autoscaling trajectories may leave the graph. Zoom
 * to fit fits the ranges to all data once.</li>
 * <li>Export trajectory data to a text format (time, x, y) using the configured
 * mapper and a {@link Formatter} for numeric formatting.</li>
 * </ul>
 *
 * <h3>Rendering details:</h3>
 * <ul>
 * <li>The trajectory is drawn in screen coordinates, scaled from the current
 * axes ranges, and clipped to the frame.</li>
 * </ul>
 *
 * <h3>Buffering and sampling:</h3>
 * <ul>
 * <li>Sample retention uses a display-agnostic heuristic based on normalized
 * state-space distance and local curvature.</li>
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
	 *               adding it to the buffer.
	 *               <li>In order to conserve memory the trajectory is thinned,
	 *               i.e. the data replaces the most recent data unless required
	 *               to represent the trajectory, or {@code force == true}.
	 *               </ul>
	 *
	 * @param t     the time of the data
	 * @param data  the data to add
	 * @param force <code>true</code> to force adding the data
	 *
	 * @see Thinning
	 */
	public void addData(double t, double[] data, boolean force) {
		double[] entry = prependTime2Data(t, data);
		if (buffer.isEmpty() || Double.isNaN(t)) {
			// new starting point, replaces a preceding starting point
			if (!buffer.isEmpty() && Double.isNaN(buffer.last()[0]))
				buffer.replace(entry);
			else
				buffer.append(entry);
			init = Arrays.copyOf(entry, entry.length);
			thinning.reset(entry);
		} else if (Math.abs(t - buffer.last()[0]) < Model.EPS) {
			buffer.replace(entry);
			System.arraycopy(data, 0, init, 1, data.length);
		} else
			thinning.add(buffer, entry, force);
		updateAutoscale();
	}

	/**
	 * The thinning of the trajectory to conserve memory.
	 */
	private final Thinning thinning = new Thinning.Trajectory();

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
		if (!(style.xMax > style.xMin && style.yMax > style.yMin) || !prepCanvas(true))
			return;
		// trajectory in coordinates of the content, see prepCanvas(boolean)
		double xScale = bounds.getWidth() / (style.xMax - style.xMin);
		double yScale = bounds.getHeight() / (style.yMax - style.yMin);
		Point2D nextPt = new Point2D();
		Point2D currPt = new Point2D();
		g.setStrokeStyle(style.trajColor);
		g.setLineWidth(style.lineWidth / zoomFactor);
		drawTrajectory(currPt, nextPt, xScale, yScale);
		g.restore();
		drawFrameForView(4, 4);
		// markers on top of the frame, not clipped and not scaled
		g.save();
		g.scale(scale, scale);
		g.translate(bounds.getX() - viewCorner.getX(), bounds.getY() - viewCorner.getY());
		if (withMarkers)
			drawStartEndMarkers(currPt, xScale * zoomFactor, yScale * zoomFactor);
		if (markers != null)
			drawCustomMarkers(currPt, xScale * zoomFactor, yScale * zoomFactor);
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
		// skip markers outside of the frame, e.g. when zoomed in
		double vx = sx - viewCorner.getX();
		double vy = sy - viewCorner.getY();
		if (vx < 0.0 || vx > bounds.getWidth() || vy < 0.0 || vy > bounds.getHeight())
			return;
		if (fill)
			fillCircle(sx, sy, radius);
		else
			strokeCircle(sx, sy, radius);
	}

	/**
	 * Apply autoscaling, e.g. when enabled: fit the ranges of autoscaled axes to
	 * all data in the buffer. Subsequently, the ranges track the data as it is
	 * added. Manual zooming or shifting disables autoscaling.
	 *
	 * @see #updateAutoscale()
	 */
	public void autoscale() {
		fit(style.autoscaleX, style.autoscaleY);
	}

	/**
	 * Reset the zoom and fit the ranges of the axes {@code x} and/or {@code y} to
	 * all data in the buffer, e.g. for autoscaling or zooming to fit.
	 *
	 * @param x {@code true} to fit the {@code x}-axis
	 * @param y {@code true} to fit the {@code y}-axis
	 */
	private void fit(boolean x, boolean y) {
		if (!(x || y) || buffer == null || buffer.isEmpty())
			return;
		super.zoom();
		map.reset();
		fitRanges(x, y, false);
	}

	/**
	 * Show the full range of frequencies, {@code [0, 1]} on both axes. As for
	 * zooming, this disables autoscaling.
	 */
	public void showFullRange() {
		super.zoom();
		style.autoscaleX = false;
		style.autoscaleY = false;
		style.xMin = 0.0;
		style.xMax = 1.0;
		style.yMin = 0.0;
		style.yMax = 1.0;
		paint(true);
	}

	@Override
	protected void zoom() {
		// by default both axes are autoscaled
		style.autoscaleX = true;
		style.autoscaleY = true;
		autoscale();
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * Unlike for other graphs, the view is not limited to the axes ranges but to
	 * non-negative values and, on percent axes, to at most {@code 1}. Zooming or
	 * shifting disables autoscaling, such that the view remains in place.
	 */
	@Override
	protected void setViewCorner(double x, double y, double zoom) {
		// pixels per unit on the axes
		double px = zoom * bounds.getWidth() / (style.xMax - style.xMin);
		double py = zoom * bounds.getHeight() / (style.yMax - style.yMin);
		double xHi = style.percentX ? 1.0 : Double.POSITIVE_INFINITY;
		double yHi = style.percentY ? 1.0 : Double.POSITIVE_INFINITY;
		viewCorner.set(Math.max(-style.xMin * px, Math.min((xHi - style.xMin) * px - bounds.getWidth(), x)),
				Math.max((style.yMax - yHi) * py, Math.min(style.yMax * py - bounds.getHeight(), y)));
		if (zoom != 1.0 || viewCorner.getX() != 0.0 || viewCorner.getY() != 0.0) {
			style.autoscaleX = false;
			style.autoscaleY = false;
		}
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * Zooming out is limited to the full range of percent axes and to
	 * {@link Zooming#ZOOM_MAX} otherwise.
	 */
	@Override
	protected double getMinZoom() {
		double min = 1.0 / Zooming.ZOOM_MAX;
		if (style.percentX)
			min = Math.max(min, style.xMax - style.xMin);
		if (style.percentY)
			min = Math.max(min, style.yMax - style.yMin);
		return Math.min(1.0, min);
	}

	@Override
	public void clearHistory() {
		super.clearHistory();
		if (map != null)
			map.reset();
	}

	/**
	 * If autoscaling is enabled, track the axis ranges to fit the extent of all
	 * buffered data. Without autoscaling the ranges remain unchanged, such that
	 * trajectories may leave the graph.
	 */
	private void updateAutoscale() {
		if (buffer == null || buffer.isEmpty())
			return;
		// the map keeps track of the extent of all buffered data
		if (buffer.getSize() <= 1)
			map.reset();
		fitRanges(style.autoscaleX, style.autoscaleY, true);
	}

	/**
	 * Fit the ranges of the axes {@code x} and/or {@code y} to the extent of all
	 * buffered data, as reported by the map. The axes are fitted independently,
	 * e.g. for densities of species with very different abundances.
	 *
	 * @param x     {@code true} to fit the {@code x}-axis
	 * @param y     {@code true} to fit the {@code y}-axis
	 * @param track {@code true} to only refit if needed, for autoscaling, see
	 *              {@link GraphStyle#fitYRange(double, double, boolean)}
	 */
	private void fitRanges(boolean x, boolean y, boolean track) {
		if (x)
			style.fitXRange(map.getMinX(buffer), map.getMaxX(buffer), track);
		if (y)
			style.fitYRange(map.getMinY(buffer), map.getMaxY(buffer), track);
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
		processInitXY(event.getX(), event.getY());
	}

	/**
	 * Helper method to convert the coordinates {@code (x, y)} relative to the
	 * graph into an initial configuration and set the controller's initial state.
	 * The conversion matches that of the tooltips.
	 *
	 * @param x the {@code x}-coordinate relative to the graph
	 * @param y the {@code y}-coordinate relative to the graph
	 *
	 * @see #getTooltipAt(int, int)
	 */
	private void processInitXY(int x, int y) {
		Point2D point = toPhase(x, y);
		if (point == null || buffer.isEmpty())
			return;
		double[] last = buffer.last();
		double[] initState = Arrays.copyOfRange(last, 1, last.length);
		map.phase2Data(point, initState);
		view.setInitialState(initState);
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * A double tap sets the initial state, as does a double click.
	 */
	@Override
	protected boolean onDoubleTap(int x, int y, Touch touch) {
		processInitXY(x, y);
		return true;
	}

	@Override
	public String getTooltipAt(int x, int y) {
		Point2D point = toPhase(x, y);
		if (point == null || tooltipProvider == null)
			return null;
		if (tooltipProvider instanceof TooltipProvider.Parametric)
			return ((TooltipProvider.Parametric) tooltipProvider).getTooltipAt(this, point.getX(), point.getY());
		return tooltipProvider.getTooltipAt(point.getX(), point.getY());
	}

	/**
	 * Convert the coordinates {@code (x, y)} relative to the graph into phase
	 * plane coordinates.
	 *
	 * @param x the {@code x}-coordinate relative to the graph
	 * @param y the {@code y}-coordinate relative to the graph
	 * @return the phase plane coordinates or {@code null} if outside of the frame
	 */
	private Point2D toPhase(int x, int y) {
		double w = bounds.getWidth();
		double h = bounds.getHeight();
		double fx = x - canvasPadX() - bounds.getX();
		double fy = y - canvasPadY() - bounds.getY();
		if (!(fx >= 0.0 && fx <= w && fy >= 0.0 && fy <= h))
			return null;
		Point2D p = toContent(x, y);
		return p.set(style.xMin + p.getX() / w * (style.xMax - style.xMin),
				style.yMax - p.getY() / h * (style.yMax - style.yMin));
	}

	@Override
	protected boolean inside(int x, int y) {
		return toPhase(x, y) != null;
	}

	@Override
	protected void populateZoomMenu(ContextMenu menu) {
		super.populateZoomMenu(menu);
		menu.add(new ContextMenuItem("Zoom to fit", () -> {
			fit(true, true);
			paint(true);
		}));
	}

	@Override
	public void populateContextMenuAt(ContextMenu menu, int x, int y) {
		addClearMenu(menu);
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
