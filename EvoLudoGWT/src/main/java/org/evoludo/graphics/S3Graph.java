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

import org.evoludo.geom.Path2D;
import org.evoludo.geom.Point2D;
import org.evoludo.geom.Segment2D;
import org.evoludo.graphics.AbstractGraph.HasTrajectory;
import org.evoludo.graphics.AbstractGraph.Shifting;
import org.evoludo.graphics.AbstractGraph.Zooming;
import org.evoludo.simulator.ColorMapCSS;
import org.evoludo.simulator.modules.Module;
import org.evoludo.simulator.views.HasS3;
import org.evoludo.simulator.views.S3;
import org.evoludo.simulator.views.S3Map;
import org.evoludo.ui.ContextMenu;
import org.evoludo.ui.ContextMenuItem;
import org.evoludo.util.Formatter;
import org.evoludo.util.RingBuffer;

import com.google.gwt.core.client.Duration;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.dom.client.Touch;
import com.google.gwt.event.dom.client.DoubleClickEvent;
import com.google.gwt.event.dom.client.DoubleClickHandler;
import com.google.gwt.event.dom.client.TouchStartEvent;
import com.google.gwt.user.client.ui.Widget;

/**
 * Graph for the simplex \(S_3\). The graph is used to visualize the evolution
 * of three traits. The traits are projected onto the three corners of the
 * simplex. The graph provides a context menu to set and/or swap the order of
 * the traits.
 * <p>
 * The graph is backed by a {@link RingBuffer} to store the trajectory. The
 * buffer is updated by calling {@link #addData(double, double[], boolean)}. It
 * is interactive and allows the user to zoom and shift the view. The user can
 * set the initial state by double-clicking on the graph. The graph can be
 * exported in PNG or SVG graphics formats or the trajectory data as CSV.
 *
 * @author Christoph Hauert
 */
/**
 * The graphical representation of the simplex \(S_3\). The graph displays the
 * parametric plot of trajectories projected onto the simplex. For more than two
 * traits the context menu allows to pick the displayed traits and swap their
 * corners.
 *
 * <h3>Responsibilities</h3>
 * <ul>
 * <li>Render a simplex (equilateral-triangle style) frame, subdivided levels,
 * tick marks and colored corner labels according to a given {@link S3Map}.</li>
 * <li>Store and draw a time-ordered trajectory of composition vectors in a
 * ring buffer; the buffer entries include time as the first element
 * (internal format: [time, comp0, comp1, comp2, ...]).</li>
 * <li>Efficiently sample and append incoming data using a pixel-distance based
 * threshold to avoid storing nearly identical consecutive points.</li>
 * <li>Draw start/end markers and arbitrary custom markers; export the stored
 * trajectory in a human-readable CSV-like format.</li>
 * <li>Provide user interaction support: double-click and single-touch to set
 * an initial composition, context menus to clear history, swap corner
 * ordering, or remap corners to traits, tooltip support, zooming and
 * shifting behavior provided by the hosting view.</li>
 * </ul>
 *
 * <h3>Important behavior notes</h3>
 * <ul>
 * <li>A {@link S3Map} must be supplied (via {@link #setMap(S3Map)}) to convert
 * between data vectors and simplex/cartesian coordinates. The map also
 * supplies the corner names and color ordering used for labels and export.</li>
 * <li>Incoming data points are added through
 * {@code addData(time, data, force)}.
 * The data array passed to this method is expected to be the composition
 * vector (without time) and will be prepended with the provided {@code time}
 * before storing. If {@code time} is NaN special handling is applied
 * (replace or append depending on the last stored time).</li>
 * <li>When a new sample has the same time (within numerical tolerance) as the
 * last stored sample the last sample is replaced and the initial-state
 * (start point) tracking is updated accordingly.</li>
 * <li>Buffer sampling threshold is computed in screen pixels (see
 * {@link #calcBounds(int,int)}); points closer than the threshold
 * (squared) are not stored unless {@code force} is true.</li>
 * <li>Coordinate conversions for mouse/touch interactions map screen
 * coordinates to scaled simplex coordinates in [0,1] (see
 * {@link #scaledX(double)} / {@link #scaledY(double)}). A hit test
 * {@link #inside(double,double)} ensures interactions occur only when the
 * pointer is inside the simplex triangle.</li>
 * </ul>
 *
 * <h3>Rendering details</h3>
 * <ul>
 * <li>Painting occurs in {@link #paint(boolean)} and delegates to
 * {@link #paintS3(boolean)} which sets up scaling, translation and zooming
 * transforms before drawing.</li>
 * <li>Trajectory drawing uses the configured stroke styles and optionally
 * draws start (green) and end (red) markers; custom markers may be filled
 * or stroked depending on their metadata.</li>
 * <li>The simplex frame supports showing subdivision levels, tick marks and
 * corner labels. The triangle aspect ratio is constrained so the simplex
 * appears equilateral (see {@code SQRT_2}).</li>
 * </ul>
 *
 * <h3>Context menu and UI interactions</h3>
 * <ul>
 * <li>Right-click (context menu) entries include: Clear history, Swap order
 * of traits along the closest edge, and a submenu to set which trait is
 * assigned to the closest corner when the module exposes more than three
 * traits.</li>
 * <li>Double-click and touch events call the host view's
 * {@code setInitialState}
 * with the composition corresponding to the clicked location.</li>
 * <li>Tooltips are provided through an optional {@link TooltipProvider}
 * (the S3Map is set as the tooltip provider by {@link #setMap(S3Map)} when
 * available).</li>
 * </ul>
 *
 * <h3>Public API highlights</h3>
 * <ul>
 * <li>Constructor: {@link #S3Graph(S3, Module, int)} — create widget bound to
 * the provided view and module and associated with a role index.</li>
 * <li>{@link #setMap(S3Map)} and {@link #getMap()} — configure the conversion
 * and label/color semantics for the simplex corners.</li>
 * <li>{@link #addData(double, double[], boolean)} — append or replace
 * entries in the internal trajectory buffer (time is stored as first
 * element).</li>
 * <li>{@link #reset()} — reset state and buffers; {@link #export(MyContext2d)}
 * — render into a provided canvas context (used for exporting images or
 * printing).</li>
 * <li>{@link #exportTrajectory(StringBuilder)} — append a text representation
 * of the stored trajectory (time and the three corner values in the current map
 * ordering).</li>
 * </ul>
 *
 * <h3>Notes and assumptions</h3>
 * <ul>
 * <li>The class assumes three displayed components for the simplex layout,
 * but uses the provided {@link S3Map#getOrder()} and {@link S3Map#getNames()}
 * to support modules with more than three underlying traits (allowing the
 * user to choose which traits are shown at the corners).</li>
 * <li>The widget depends on host-provided styling flags (contained in
 * {@code style})
 * to decide which visual elements to draw (ticks, labels, levels, frame,
 * marker sizes, colors, etc.).</li>
 * <li>All coordinates conversion and painting apply scaling/zoom transforms;
 * callers should trigger {@link #paint(boolean)} when view parameters or
 * data change.</li>
 * </ul>
 * 
 * @author Christoph Hauert
 */
@SuppressWarnings("java:S110")
public class S3Graph extends AbstractGraph<double[]> implements Zooming, Shifting, HasTrajectory, //
		DoubleClickHandler {

	/**
	 * The starting point of the most recent trajectory.
	 */
	double[] init;

	/**
	 * The lower left corner of the simplex.
	 */
	Point2D e0 = new Point2D();

	/**
	 * The lower right corner of the simplex.
	 */
	Point2D e1 = new Point2D();

	/**
	 * The upper corner of the simplex.
	 */
	Point2D e2 = new Point2D();

	/**
	 * The path that outlines the simplex.
	 */
	Path2D outline = new Path2D();

	/**
	 * The map for converting data to simplex coordinates (cartesian).
	 */
	S3Map map;

	/**
	 * The identifier of the role of the data.
	 */
	int role;

	/**
	 * Create a new simplex \(S_3\) graph for {@code module} running in {@code
	 * view} with the specified {@code role}.
	 * 
	 * @param view   the view of this graph
	 * @param module the module backing the graph
	 * @param role   the role of the data
	 */
	public S3Graph(S3 view, Module<?> module, int role) {
		super(view, module);
		this.role = role;
	}

	@Override
	protected void onLoad() {
		super.onLoad();
		setStylePrimaryName("evoludo-S3Graph");
	}

	@Override
	public void activate() {
		super.activate();
		doubleClickHandler = addDoubleClickHandler(this);
	}

	/**
	 * Set the map for converting data to simplex coordinates.
	 * 
	 * @param map the conversion map
	 */
	public void setMap(S3Map map) {
		if (map == null)
			return;
		this.map = map;
		setTooltipProvider(map);
	}

	/**
	 * Get the map for converting data to simplex coordinates.
	 * 
	 * @return the conversion map
	 */
	public S3Map getMap() {
		return map;
	}

	@Override
	public void reset() {
		super.reset();
		if (buffer == null || buffer.getCapacity() < MIN_BUFFER_SIZE)
			buffer = new RingBuffer<double[]>(DEFAULT_BUFFER_SIZE);
		colors = ColorMapCSS.Color2Css(map.getColors());
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
			return;
		}
		double[] last = buffer.last();
		double lastt = last[0];
		if (Math.abs(t - lastt) < 1e-8) {
			buffer.replace(prependTime2Data(t, data));
			System.arraycopy(last, 0, init, 0, last.length);
			return;
		}
		if (Double.isNaN(t)) {
			handleNaNTime(data, lastt);
			return;
		}
		if (force || distSq(data, last) > bufferThreshold) {
			buffer.append(prependTime2Data(t, data));
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
		int len = data.length;
		if (init == null || init.length != len + 1)
			init = new double[len + 1]; // add time
		System.arraycopy(buffer.last(), 0, init, 0, len + 1);
	}

	/**
	 * Handle the case that the time {@code t} is {@code NaN}. If the last time in
	 * the buffer is also {@code NaN} the last data point is replaced by the new
	 * data, otherwise the new data is appended to the buffer. The initial state is
	 * updated accordingly.
	 * 
	 * @param data  the data to add
	 * @param lastt the time of the last data point in the buffer
	 */
	private void handleNaNTime(double[] data, double lastt) {
		double[] add = prependTime2Data(Double.NaN, data);
		if (Double.isNaN(lastt))
			buffer.replace(add);
		else
			buffer.append(add);
		System.arraycopy(buffer.last(), 0, init, 0, data.length);
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
		paintS3(true);
		return false;
	}

	/**
	 * Paint the trajectory in the simplex. If <code>withMarkers</code> is
	 * <code>true</code> the start and end points of the trajectory are marked with
	 * green and red circles, respectively.
	 * 
	 * @param withMarkers <code>true</code> to mark start and end points
	 */
	private void paintS3(boolean withMarkers) {
		g.save();
		g.scale(scale, scale);
		clearCanvas();
		g.translate(bounds.getX() - viewCorner.getX(), bounds.getY() - viewCorner.getY());
		g.scale(zoomFactor, zoomFactor);

		double w = bounds.getWidth();
		double h = bounds.getHeight();

		drawTrajectory(w, h);
		drawFrame(3);
		if (withMarkers)
			drawStartEndMarkers(w, h);
		drawCustomMarkers(w, h);

		g.restore();
	}

	/**
	 * Draw the trajectory stored in the buffer.
	 */
	private void drawTrajectory(double w, double h) {
		Point2D prevPt = new Point2D();
		Point2D currPt = new Point2D();
		String tC = style.trajColor;
		g.setStrokeStyle(tC);
		// increase line width for trajectories with transparency
		g.setLineWidth((tC.startsWith("rgba") ? 1.25 * style.lineWidth : style.lineWidth));
		Iterator<double[]> i = buffer.iterator();
		if (i.hasNext()) {
			double[] current = i.next();
			double ct = current[0];
			map.data2S3(current, currPt);
			currPt.scale(w, h);
			while (i.hasNext()) {
				double[] prev = i.next();
				double pt = prev[0];
				map.data2S3(prev, prevPt);
				prevPt.scale(w, h);
				if (!Double.isNaN(ct))
					strokeLine(prevPt, currPt);
				ct = pt;
				Point2D swap = currPt;
				currPt = prevPt;
				prevPt = swap;
			}
		}
	}

	/**
	 * Draw start and end markers for the trajectory.
	 */
	private void drawStartEndMarkers(double w, double h) {
		Point2D prevPt = new Point2D();
		Point2D currPt = new Point2D();
		g.setFillStyle(style.startColor);
		map.data2S3(init, prevPt);
		fillCircle(prevPt.scale(w, h), style.markerSize);
		if (!buffer.isEmpty()) {
			g.setFillStyle(style.endColor);
			map.data2S3(buffer.last(), currPt);
			fillCircle(currPt.scale(w, h), style.markerSize);
		}
	}

	/**
	 * Draw any additional markers stored in 'markers'.
	 */
	private void drawCustomMarkers(double w, double h) {
		if (markers == null)
			return;
		Point2D currPt = new Point2D();
		int n = 0;
		int nMarkers = markers.size();
		for (double[] mark : markers) {
			map.data2S3(mark, currPt);
			String mcolor = markerColors[n++ % nMarkers];
			if (mark[0] > 0.0) {
				g.setFillStyle(mcolor);
				fillCircle(currPt.scale(w, h), style.markerSize);
			} else {
				g.setLineWidth(style.lineWidth);
				g.setStrokeStyle(mcolor);
				strokeCircle(currPt.scale(w, h), style.markerSize);
			}
		}
	}

	@Override
	public void export(MyContext2d ctx) {
		MyContext2d bak = g;
		g = ctx;
		paintS3(false);
		g = bak;
	}

	/**
	 * Constant for \(\sqrt{2}\). Aspect ratio of equilateral triangle.
	 */
	static final double SQRT_2 = 1.41421356237;

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
		String font = g.getFont();
		if (style.showXTicks) {
			int dx = style.tickLength;
			int dy = dx / 2;
			int dw = -2 * dx;
			int dh = dx - dy;
			bounds.adjust(dx, dy, dw, dh);
		}
		if (style.showXTickLabels) {
			setFont(style.ticksLabelFont);
			double tik = (int) g.measureText(Formatter.format((style.xMax - style.xMin) / Math.PI, 2)).getWidth();
			double tik2 = tik * 0.5;
			bounds.adjust(tik2, 0, -tik, -14);
		}
		if (style.showLabel) {
			setFont(style.labelFont);
			// lower left & right
			int xshift = (int) (Math.max(g.measureText(map.getName(HasS3.CORNER_LEFT)).getWidth(),
					Math.max(g.measureText(map.getName(HasS3.CORNER_RIGHT)).getWidth(),
							g.measureText(map.getName(HasS3.CORNER_TOP)).getWidth()))
					* 0.5 + 0.5);
			int yshift = 20;
			int dw = -2 * xshift;
			int dh = -2 * yshift;
			bounds.adjust(xshift, yshift, dw, dh);
		}
		// constrain aspect ratio
		double w = bounds.getWidth();
		double h = bounds.getHeight();
		if (w > h) {
			double nw = Math.min(w, h * SQRT_2);
			bounds.adjust((w - nw) * 0.5, 0, nw - w, 0);
		}
		if (w < h) {
			double nh = w;
			bounds.adjust(0, (h - nh) * 0.5, 0, nh - h);
		}
		g.setFont(font);
		// now that the bounds are known determine outline
		w = bounds.getWidth();
		h = bounds.getHeight();
		map.data2S3(1.0, 0.0, 0.0, e0);
		e0.scale(w, h);
		map.data2S3(0.0, 1.0, 0.0, e1);
		e1.scale(w, h);
		map.data2S3(0.0, 0.0, 1.0, e2);
		e2.scale(w, h);
		outline.reset();
		outline.moveTo(e0);
		outline.lineTo(e1);
		outline.lineTo(e2);
		outline.closePath();
		// store point only if it is estimated to be at least a few pixels from the
		// previous point
		bufferThreshold = MIN_PIXELS * scale / Math.max(bounds.getWidth(), bounds.getHeight());
		bufferThreshold *= bufferThreshold;
	}

	/**
	 * Draws the frame of the simplex. The corners are marked by the trait names in
	 * their respective colours. For visual guidance each side is subdivided into
	 * {@code sLevels} sublevels.
	 * 
	 * @param sLevels the number of sublevels for the frame
	 */
	public void drawFrame(int sLevels) {
		double w = bounds.getWidth();
		double h = bounds.getHeight();

		drawOutline();
		if (style.showXLevels)
			drawS3Levels(sLevels, w, h);
		if (style.showXTicks)
			drawS3Ticks(sLevels, w, h);
		if (style.showLabel)
			drawCornerLabels(w, h);
		if (style.label != null) {
			g.setFillStyle(style.labelColor);
			g.fillText(style.label, -10.0, -32.0);
		}
	}

	/**
	 * Draw the outline of the simplex.
	 */
	private void drawOutline() {
		if (!style.showFrame)
			return;
		g.setLineWidth(style.frameWidth);
		g.setStrokeStyle(style.frameColor);
		stroke(outline);
	}

	/**
	 * Draw the subdivision levels of the simplex.
	 * 
	 * @param levels the number of sublevels
	 * @param width  the width of the simplex
	 * @param height the height of the simplex
	 */
	private void drawS3Levels(int levels, double width, double height) {
		g.setLineWidth(style.frameWidth);
		g.setStrokeStyle(style.levelColor);
		Point2D start = new Point2D();
		Point2D end = new Point2D();
		double iLevels = 1.0 / levels;
		double x = iLevels;
		for (int l = 1; l < levels; l++) {
			map.data2S3(x, 1.0 - x, 0.0, start);
			map.data2S3(0.0, 1.0 - x, x, end);
			strokeLine(start.scale(width, height), end.scale(width, height));
			map.data2S3(0.0, x, 1.0 - x, start);
			map.data2S3(x, 0.0, 1.0 - x, end);
			strokeLine(start.scale(width, height), end.scale(width, height));
			map.data2S3(1.0 - x, 0.0, x, start);
			map.data2S3(1.0 - x, x, 0.0, end);
			strokeLine(start.scale(width, height), end.scale(width, height));
			x += iLevels;
		}
	}

	/**
	 * Draw the ticks along the boundaries of the simplex.
	 * 
	 * @param levels the number of sublevels
	 * @param width  the width of the simplex
	 * @param height the height of the simplex
	 */
	private void drawS3Ticks(int levels, double width, double height) {
		g.setStrokeStyle(style.frameColor);
		g.setFillStyle(style.frameColor);
		setFont(style.ticksLabelFont);
		double len = Math.sqrt(height * height + width * width / 4);
		double ty = width / (len + len) * (style.tickLength + 1);
		double tx = height / len * (style.tickLength + 1);
		double iLevels = 1.0 / levels;
		double x = 0.0;
		String tick;
		Point2D loc = new Point2D();
		for (int l = 0; l <= levels; l++) {
			if (style.percentX)
				tick = Formatter.formatPercent(1.0 - x, 0);
			else
				tick = Formatter.format(1.0 - x, 2);

			map.data2S3(x, 1.0 - x, 0.0, loc);
			loc.scale(width, height);
			double lx = loc.getX();
			double ly = loc.getY();
			strokeLine(lx, ly, lx, ly + style.tickLength);
			if (style.showXTickLabels)
				// center tick labels with ticks
				g.fillText(tick, lx - g.measureText(tick).getWidth() * 0.5, ly + style.tickLength + 12.5);

			map.data2S3(0.0, x, 1.0 - x, loc);
			loc.scale(width, height);
			lx = loc.getX();
			ly = loc.getY();
			loc.shift(tx, -ty);
			strokeLine(lx, ly, loc.getX(), loc.getY());
			if (style.showXTickLabels)
				g.fillText(tick, loc.getX() + 6, loc.getY() + 3);

			map.data2S3(1.0 - x, 0.0, x, loc);
			loc.scale(width, height);
			lx = loc.getX();
			ly = loc.getY();
			loc.shift(-tx, -ty);
			strokeLine(lx, ly, loc.getX(), loc.getY());
			if (style.showXTickLabels)
				g.fillText(tick, loc.getX() - (g.measureText(tick).getWidth() + 6), loc.getY() + 3);

			x += iLevels;
		}
	}

	/**
	 * Draw the corner labels of the simplex in their respective colours.
	 * 
	 * @param width  the width of the simplex
	 * @param height the height of the simplex
	 */
	private void drawCornerLabels(double width, double height) {
		double yshift = 14.5;
		if (style.showXTicks)
			yshift += style.tickLength;
		if (style.showXTickLabels)
			yshift += 12.5;
		setFont(style.labelFont);
		int[] order = map.getOrder();
		g.setFillStyle(colors[order[0]]);
		Point2D loc = new Point2D();
		map.data2S3(1.0, 0.0, 0.0, loc);
		loc.scale(width, height);
		String label = map.getName(HasS3.CORNER_LEFT);
		g.fillText(label, loc.getX() - g.measureText(label).getWidth() * 0.5, loc.getY() + yshift);

		g.setFillStyle(colors[order[1]]);
		map.data2S3(0.0, 1.0, 0.0, loc);
		loc.scale(width, height);
		label = map.getName(HasS3.CORNER_RIGHT);
		g.fillText(label, loc.getX() - g.measureText(label).getWidth() * 0.5, loc.getY() + yshift);

		g.setFillStyle(colors[order[2]]);
		map.data2S3(0.0, 0.0, 1.0, loc);
		loc.scale(width, height);
		label = map.getName(HasS3.CORNER_TOP);
		g.fillText(label, loc.getX() - g.measureText(label).getWidth() * 0.5, loc.getY() - 14.5);
	}

	@Override
	public void onDoubleClick(DoubleClickEvent event) {
		processInitXY(event.getX(), event.getY());
	}

	@Override
	public void onTouchStart(TouchStartEvent event) {
		super.onTouchStart(event);
		JsArray<Touch> touches = event.getTouches();
		if (Duration.currentTimeMillis() - touchEndTime > 250.0 || touches.length() > 1)
			// single tap or multiple touches
			return;

		Touch touch = touches.get(0);
		processInitXY(touch.getRelativeX(getElement()), touch.getRelativeY(getElement()));
		event.preventDefault();
	}

	/**
	 * Convert the {@code x}-component of the screen coordinates {@code (x, y)} into
	 * scaled (Cartesian) coordinates in {@code [0, 1]}.
	 * 
	 * @param x the {@code x}-coordinate on screen
	 * @return the scaled coordinate
	 */
	double scaledX(double x) {
		return ((viewCorner.getX() + x - bounds.getX()) / zoomFactor + 0.5) / (bounds.getWidth() - 1.0);
	}

	/**
	 * Convert the {@code y}-component of the screen coordinates {@code (x, y)} into
	 * scaled (Cartesian) coordinates in {@code [0, 1]}.
	 * 
	 * @param y the {@code y}-coordinate on screen
	 * @return the scaled coordinate
	 */
	double scaledY(double y) {
		return 1.0 - ((viewCorner.getY() + y - bounds.getY()) / zoomFactor + 0.5) / (bounds.getHeight() - 1.0);
	}

	/**
	 * Helper method to convert screen coordinates into an initial configuration and
	 * set the controller's initial state.
	 * 
	 * @param x the {@code x}-coordinate on screen
	 * @param y the {@code y}-coordinate on screen
	 */
	private void processInitXY(int x, int y) {
		// convert to user coordinates
		double[] s3 = new double[3];
		map.s32Data(scaledX(x), scaledY(y), s3);
		view.setInitialState(s3);
	}

	// tool tips
	@Override
	public String getTooltipAt(int x, int y) {
		// convert to user coordinates
		double sx = scaledX(x);
		double sy = scaledY(y);
		if (!inside(sx, sy))
			return null;
		if (tooltipProvider instanceof TooltipProvider.Simplex)
			return ((TooltipProvider.Simplex) tooltipProvider).getTooltipAt(this, sx, sy);
		if (tooltipProvider != null)
			return tooltipProvider.getTooltipAt(sx, sy);
		return null;
	}

	/**
	 * Check if point (in scaled user coordinates) lies inside of simplex.
	 * 
	 * @param sx <code>x</code>-coordinate of point
	 * @param sy <code>y</code>-coordinate of point
	 * @return <code>true</code> if inside
	 */
	protected boolean inside(double sx, double sy) {
		final Point2D c0 = new Point2D(0.0, 0.0);
		final Point2D c2 = new Point2D(1.0, 0.0);
		final Point2D c1 = new Point2D(0.5, 1.0);
		Point2D p = new Point2D(sx, sy);
		boolean inside = (Segment2D.orientation(c0, c1, p) >= 0);
		if (!inside)
			return false;
		inside &= (Segment2D.orientation(c1, c2, p) >= 0);
		if (!inside)
			return false;
		return (Segment2D.orientation(c2, c0, p) >= 0);
	}

	/**
	 * The context menu item to swap the order of the traits along the closest edge.
	 */
	private ContextMenuItem swapOrderMenu;

	/**
	 * The context menu item to clear the canvas.
	 */
	private ContextMenuItem clearMenu;

	/**
	 * The context menu to select the trait in the closest corner.
	 */
	private ContextMenu setTraitMenu;

	@Override
	public void populateContextMenuAt(ContextMenu menu, int x, int y) {
		addClearMenu(menu);
		addSwapOrderMenu(menu, x, y);
		addSetTraitMenu(menu, x, y);
		super.populateContextMenuAt(menu, x, y);
	}

	/**
	 * Helper method to process the clear context menu.
	 * 
	 * @param menu the context menu to populate
	 */
	private void addClearMenu(ContextMenu menu) {
		if (clearMenu == null) {
			clearMenu = new ContextMenuItem("Clear", () -> {
				clearHistory();
				paint(true);
			});
		}
		menu.addSeparator();
		menu.add(clearMenu);
	}

	/**
	 * The label of the swap order menu item.
	 */
	private static final String SWAP_MENU_LABEL = "Swap Order";

	/**
	 * The prefix of the swap order menu item.
	 */
	private static final String SWAP_MENU = "Swap ";

	/**
	 * The arrow symbol used in the swap order menu item.
	 */
	private static final String SWAP_MENU_ARROW = " \u2194 ";

	/**
	 * Helper method to process the swap order submenu logic of the context menu.
	 * 
	 * @param menu the context menu to populate
	 * @param x    the <code>x</code>-coordinate of the mouse pointer
	 * @param y    the <code>y</code>-coordinate of the mouse pointer
	 */
	private void addSwapOrderMenu(ContextMenu menu, int x, int y) {
		if (swapOrderMenu == null) {
			swapOrderMenu = new ContextMenuItem(SWAP_MENU_LABEL, () -> {
				int[] order = map.getOrder();
				String[] names = map.getNames();
				String label = swapOrderMenu.getText();
				if (label.startsWith(SWAP_MENU + names[order[0]])) {
					swapOrder(order, 0, 1);
				} else if (label.startsWith(SWAP_MENU + names[order[1]])) {
					swapOrder(order, 1, 2);
				} else {
					swapOrder(order, 2, 0);
				}
				map.setOrder(order);
				paint(true);
			});
		}
		menu.add(swapOrderMenu);
		int[] order = map.getOrder();
		String[] names = map.getNames();
		switch (closestEdge(x, y)) {
			case HasS3.EDGE_LEFT:
				swapOrderMenu.setText(SWAP_MENU + names[order[0]] + SWAP_MENU_ARROW + names[order[1]]);
				break;
			case HasS3.EDGE_RIGHT:
				swapOrderMenu.setText(SWAP_MENU + names[order[1]] + SWAP_MENU_ARROW + names[order[2]]);
				break;
			default:
				swapOrderMenu.setText(SWAP_MENU + names[order[2]] + SWAP_MENU_ARROW + names[order[0]]);
				break;
		}
	}

	/**
	 * Helper method to swap two entries in the order array.
	 * 
	 * @param order the order array
	 * @param i     the index of the first entry
	 * @param j     the index of the second entry
	 */
	private void swapOrder(int[] order, int i, int j) {
		int temp = order[i];
		order[i] = order[j];
		order[j] = temp;
	}

	/**
	 * Helper method to process the select traits submenu. For modules with more
	 * than three traits, a submenu is created to allow the user to select the
	 * trait for each corner.
	 * 
	 * @param menu the context menu to populate
	 * @param x    the <code>x</code>-coordinate of the mouse pointer
	 * @param y    the <code>y</code>-coordinate of the mouse pointer
	 */
	private void addSetTraitMenu(ContextMenu menu, int x, int y) {
		if (buffer.getDepth() <= 4)
			return;
		int[] order = map.getOrder();
		String[] names = map.getNames();
		int cornerIdx = closestCorner(x, y);
		if (setTraitMenu == null) {
			setTraitMenu = new ContextMenu(menu);
			for (String name : names)
				setTraitMenu.add(new ContextMenuItem(name, () -> {
					Iterator<Widget> items = setTraitMenu.iterator();
					int idx = 0;
					while (items.hasNext()) {
						ContextMenuItem item = (ContextMenuItem) items.next();
						if (item.getText().equals(name)) {
							order[cornerIdx] = idx;
							break;
						}
						idx++;
					}
					paint(true);
				}));
		}
		menu.add("Set trait '" + names[order[cornerIdx]] + "' to ...", setTraitMenu);
		for (Widget item : setTraitMenu)
			((ContextMenuItem) item).setEnabled(true);
		for (int t : order)
			((ContextMenuItem) setTraitMenu.getWidget(t)).setEnabled(false);
	}

	/**
	 * Find the corner closest to the point {@code (x, y)}.
	 * 
	 * @param x the <code>x</code>-coordinate of the point
	 * @param y the <code>y</code>-coordinate of the point
	 * @return the index of the closest corner
	 */
	protected int closestCorner(double x, double y) {
		Point2D p = new Point2D(x - bounds.getX(), y - bounds.getY());
		double d0 = p.distance2(e0);
		double d1 = p.distance2(e1);
		double d2 = p.distance2(e2);
		if (d0 < d1) {
			if (d0 < d2)
				return HasS3.CORNER_LEFT;
			return HasS3.CORNER_TOP;
		}
		if (d1 < d2)
			return HasS3.CORNER_RIGHT;
		return HasS3.CORNER_TOP;
	}

	/**
	 * Find the edge closest to the point {@code (x, y)}.
	 * 
	 * @param x the <code>x</code>-coordinate of the point
	 * @param y the <code>y</code>-coordinate of the point
	 * @return the index of the closest edge
	 */
	protected int closestEdge(double x, double y) {
		Point2D p = new Point2D(x - bounds.getX(), y - bounds.getY());
		double d0 = Segment2D.distance2(e0, e1, p);
		double d1 = Segment2D.distance2(e1, e2, p);
		double d2 = Segment2D.distance2(e2, e0, p);
		if (d0 > d1) {
			if (d1 > d2)
				return HasS3.EDGE_BOTTOM;
			return HasS3.EDGE_RIGHT;
		}
		if (d0 > d2)
			return HasS3.EDGE_BOTTOM;
		return HasS3.EDGE_LEFT;
	}

	@Override
	public void exportTrajectory(StringBuilder export) {
		if (buffer.isEmpty())
			return;
		// extract the S3 data from buffer
		int[] order = map.getOrder();
		String[] names = map.getNames();
		export.append("# time, " + names[order[0]] + ", " + names[order[1]] + ", " + names[order[2]] + "\n");
		Iterator<double[]> entry = buffer.ordered();
		while (entry.hasNext()) {
			double[] s3 = entry.next();
			export.append(Formatter.format(s3[0], 8))
					.append(", ")
					.append(Formatter.format(s3[order[0] + 1], 8))
					.append(", ")
					.append(Formatter.format(s3[order[1] + 1], 8))
					.append(", ")
					.append(Formatter.format(s3[order[2] + 1], 8))
					.append("\n");
		}
	}
}
