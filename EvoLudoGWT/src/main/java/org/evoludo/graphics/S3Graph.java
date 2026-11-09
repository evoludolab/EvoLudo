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

import org.evoludo.geom.Path2D;
import org.evoludo.geom.Point2D;
import org.evoludo.geom.Segment2D;
import org.evoludo.graphics.AbstractGraph.HasTrajectory;
import org.evoludo.graphics.AbstractGraph.Shifting;
import org.evoludo.graphics.AbstractGraph.Zooming;
import org.evoludo.simulator.ColorMapCSS;
import org.evoludo.simulator.models.Model;
import org.evoludo.simulator.modules.Module;
import org.evoludo.simulator.views.HasS3;
import org.evoludo.simulator.views.S3;
import org.evoludo.simulator.views.S3Map;
import org.evoludo.ui.ContextMenu;
import org.evoludo.ui.ContextMenuItem;
import org.evoludo.util.Formatter;
import org.evoludo.util.RingBuffer;

import com.google.gwt.dom.client.Touch;
import com.google.gwt.event.dom.client.DoubleClickEvent;
import com.google.gwt.event.dom.client.DoubleClickHandler;

/**
 * Graph for the simplex \(S_3\). The graph is used to visualize the evolution
 * of three traits by projecting them onto the corners of the simplex. The view
 * renders parametric trajectories stored in a {@link RingBuffer}, exposes zoom
 * and shift interactions, supports swapping trait assignments via a context
 * menu, and allows exporting to PNG/SVG or CSV.
 *
 * <h3>Responsibilities</h3>
 * <ul>
 * <li>Render a simplex (equilateral-triangle style) frame, subdivided levels,
 * tick marks and colored corner labels according to a given {@link S3Map}.</li>
 * <li>Store and draw a time-ordered trajectory of composition vectors in a
 * ring buffer; the buffer entries include time as the first element
 * (internal format: [time, comp0, comp1, comp2, ...]).</li>
 * <li>Efficiently sample and append incoming data using a display-agnostic
 * heuristic to avoid storing nearly identical consecutive points.</li>
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
 * <li>Buffer sampling uses normalized state-space distance and local curvature;
 * samples failing both criteria are not stored unless {@code force} is
 * true.</li>
 * <li>Coordinate conversions for mouse/touch interactions map screen
 * coordinates to scaled simplex coordinates in [0,1] (see
 * {@link #toScaled(int, int)}). A hit test
 * {@link #inside(double,double)} ensures that tooltips are only shown when the
 * pointer is inside the simplex triangle. Shifting and zooming work anywhere
 * on the graph and can reveal the labels of the corners.</li>
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
	 * The corners of the simplex: lower left, lower right and top.
	 */
	private final Point2D[] corners = { e0, e1, e2 };

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
	 * Tooltip warning shown when setting initial frequencies is ambiguous.
	 */
	private static final String INIT_PROJECTION_WARNING = "Warning: initial frequencies cannot be set from an S3 projection.";

	/**
	 * Duration in milliseconds for the temporary initial-frequency warning.
	 */
	private static final int INIT_PROJECTION_WARNING_DURATION = 2500;

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
	}

	/**
	 * The thinning of the trajectory to conserve memory.
	 */
	private final Thinning thinning = new Thinning.Trajectory();

	@Override
	public boolean paint(boolean force) {
		if (super.paint(force))
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
		if (!prepCanvas(false))
			return;
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
	 * 
	 * @param w graph width
	 * @param h graph height
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
	 * 
	 * @param w graph width
	 * @param h graph height
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
	 * Draw any additional markers stored in {@code markers}.
	 * 
	 * @param w graph width
	 * @param h graph height
	 */
	private void drawCustomMarkers(double w, double h) {
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
	static final double SQRT_2 = Math.sqrt(2);

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
		// the corners: (1, 0, 0), (0, 1, 0) and (0, 0, 1)
		toS3(new double[] { 1.0, 0.0, 0.0 }, 0, e0);
		toS3(new double[] { 1.0, 0.0, 0.0 }, 1, e1);
		toS3(new double[] { 1.0, 0.0, 0.0 }, 2, e2);
		outline.reset();
		outline.moveTo(e0);
		outline.lineTo(e1);
		outline.lineTo(e2);
		outline.closePath();
	}

	/**
	 * Draws the frame of the simplex. The corners are marked by the trait names in
	 * their respective colours. For visual guidance each side is subdivided into
	 * {@code sLevels} sublevels.
	 * 
	 * @param sLevels the number of sublevels for the frame
	 */
	public void drawFrame(int sLevels) {
		drawOutline();
		if (style.showXLevels)
			drawS3Levels(sLevels);
		if (style.showXTicks)
			drawS3Ticks(sLevels);
		if (style.showLabel)
			drawCornerLabels();
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
	private void drawS3Levels(int levels) {
		g.setLineWidth(style.frameWidth);
		g.setStrokeStyle(style.levelColor);
		Point2D start = new Point2D();
		Point2D end = new Point2D();
		for (int l = 1; l < levels; l++) {
			double x = (double) l / levels;
			// lines parallel to each side
			for (int k = 0; k < 3; k++)
				strokeLine(toS3(new double[] { x, 1.0 - x, 0.0 }, k, start),
						toS3(new double[] { 0.0, 1.0 - x, x }, k, end));
		}
	}

	/**
	 * Map the point {@code v} of the simplex, with its coordinates rotated
	 * cyclically by {@code k} positions, onto {@code p} in coordinates of the
	 * graph. The rotation maps each side of the simplex onto the next one.
	 *
	 * @param v the coordinates of the point
	 * @param k the number of positions to rotate the coordinates
	 * @param p the point in coordinates of the graph
	 * @return the point {@code p}
	 */
	private Point2D toS3(double[] v, int k, Point2D p) {
		map.data2S3(v[(3 - k) % 3], v[(4 - k) % 3], v[(5 - k) % 3], p);
		return p.scale(bounds.getWidth(), bounds.getHeight());
	}

	/**
	 * Draw the ticks along the boundaries of the simplex.
	 * 
	 * @param levels the number of sublevels
	 * @param width  the width of the simplex
	 * @param height the height of the simplex
	 */
	private void drawS3Ticks(int levels) {
		g.setStrokeStyle(style.frameColor);
		g.setFillStyle(style.frameColor);
		setFont(style.ticksLabelFont);
		double width = bounds.getWidth();
		double height = bounds.getHeight();
		double len = Math.sqrt(height * height + width * width / 4);
		double ty = width / (len + len) * (style.tickLength + 1);
		double tx = height / len * (style.tickLength + 1);
		// ticks point down from the bottom side and outward from the others
		double[][] dirs = { { 0.0, style.tickLength }, { tx, -ty }, { -tx, -ty } };
		Point2D loc = new Point2D();
		for (int l = 0; l <= levels; l++) {
			double x = (double) l / levels;
			String tick = style.percentX ? Formatter.formatPercent(1.0 - x, 0) : Formatter.format(1.0 - x, 2);
			double tw = g.measureText(tick).getWidth();
			for (int k = 0; k < 3; k++) {
				toS3(new double[] { x, 1.0 - x, 0.0 }, k, loc);
				double ex = loc.getX() + dirs[k][0];
				double ey = loc.getY() + dirs[k][1];
				strokeLine(loc.getX(), loc.getY(), ex, ey);
				if (style.showXTickLabels)
					g.fillText(tick, ex + (k == 0 ? -0.5 * tw : k == 1 ? 6.0 : -tw - 6.0), ey + (k == 0 ? 12.5 : 3.0));
			}
		}
	}

	/**
	 * Draw the corner labels of the simplex in their respective colours.
	 * 
	 * @param width  the width of the simplex
	 * @param height the height of the simplex
	 */
	private void drawCornerLabels() {
		double yshift = 14.5;
		if (style.showXTicks)
			yshift += style.tickLength;
		if (style.showXTickLabels)
			yshift += 12.5;
		setFont(style.labelFont);
		int[] order = map.getOrder();
		for (int k = HasS3.CORNER_LEFT; k <= HasS3.CORNER_TOP; k++) {
			String label = map.getName(k);
			g.setFillStyle(colors[order[k]]);
			// labels below the bottom corners and above the top corner
			g.fillText(label, corners[k].getX() - g.measureText(label).getWidth() * 0.5,
					corners[k].getY() + (k == HasS3.CORNER_TOP ? -14.5 : yshift));
		}
	}

	@Override
	public void onDoubleClick(DoubleClickEvent event) {
		processInitXY(event.getX(), event.getY(), event.getClientX(), event.getClientY(), false);
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * A double tap sets the initial state, as does a double click.
	 */
	@Override
	protected boolean onDoubleTap(int x, int y, Touch touch) {
		processInitXY(x, y, touch.getClientX(), touch.getClientY(), true);
		return true;
	}

	/**
	 * Convert the coordinates {@code (x, y)} relative to the graph into scaled
	 * (Cartesian) coordinates in {@code [0, 1]}.
	 * 
	 * @param x the {@code x}-coordinate relative to the graph
	 * @param y the {@code y}-coordinate relative to the graph
	 * @return the scaled coordinates
	 */
	private Point2D toScaled(int x, int y) {
		Point2D p = toContent(x, y);
		return p.set((p.getX() + 0.5) / (bounds.getWidth() - 1.0), 1.0 - (p.getY() + 0.5) / (bounds.getHeight() - 1.0));
	}

	/**
	 * Helper method to convert screen coordinates into an initial configuration and
	 * set the controller's initial state.
	 * 
	 * @param x       the {@code x}-coordinate relative to the graph
	 * @param y       the {@code y}-coordinate relative to the graph
	 * @param clientX the {@code x}-coordinate relative to the browser window
	 * @param clientY the {@code y}-coordinate relative to the browser window
	 * @param isTouch {@code true} if the interaction was triggered by touch
	 */
	private void processInitXY(int x, int y, int clientX, int clientY, boolean isTouch) {
		if (module.getNTraits() != 3) {
			showProjectionWarning(clientX, clientY, isTouch);
			return;
		}
		// convert to user coordinates
		Point2D p = toScaled(x, y);
		double[] s3 = map.s32Data(p.getX(), p.getY());
		view.setInitialState(s3);
	}

	/**
	 * Show a temporary warning that setting initial frequencies from the current
	 * simplex projection is ambiguous.
	 * 
	 * @param x       the {@code x}-coordinate relative to the browser window
	 * @param y       the {@code y}-coordinate relative to the browser window
	 * @param isTouch {@code true} if the interaction was triggered by touch
	 */
	private void showProjectionWarning(int x, int y, boolean isTouch) {
		tooltip.showEphemeralTip(this, x, y, isTouch, INIT_PROJECTION_WARNING, INIT_PROJECTION_WARNING_DURATION);
	}

	// tool tips
	@Override
	public String getTooltipAt(int x, int y) {
		// convert to user coordinates
		Point2D p = toScaled(x, y);
		double sx = p.getX();
		double sy = p.getY();
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
		return Segment2D.orientation(c0, c1, p) >= 0 && Segment2D.orientation(c1, c2, p) >= 0
				&& Segment2D.orientation(c2, c0, p) >= 0;
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * The zoomed graph, including the labels of the corners, always covers the
	 * canvas, i.e. the graph can be shifted to reveal any part of the canvas.
	 */
	@Override
	protected void setViewCorner(double x, double y, double zoom) {
		double dz = zoom - 1.0;
		viewCorner.set(
				Math.min(dz * (wrapper.getOffsetWidth() - bounds.getX()), Math.max(-dz * bounds.getX(), x)),
				Math.min(dz * (wrapper.getOffsetHeight() - bounds.getY()), Math.max(-dz * bounds.getY(), y)));
	}

	@Override
	public void populateContextMenuAt(ContextMenu menu, int x, int y) {
		addClearMenu(menu);
		addSwapOrderMenu(menu, x, y);
		addSetTraitMenu(menu, x, y);
		super.populateContextMenuAt(menu, x, y);
	}

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
		// the edges connect corners i and (i + 1) % 3
		int i = closestEdge(x, y);
		int j = (i + 1) % 3;
		int[] order = map.getOrder();
		String[] names = map.getNames();
		menu.add(new ContextMenuItem(SWAP_MENU + names[order[i]] + SWAP_MENU_ARROW + names[order[j]], () -> {
			int[] swapped = map.getOrder();
			int tmp = swapped[i];
			swapped[i] = swapped[j];
			swapped[j] = tmp;
			map.setOrder(swapped);
			paint(true);
		}));
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
		ContextMenu setTraitMenu = new ContextMenu(menu);
		for (int traitIdx = 0; traitIdx < names.length; traitIdx++) {
			final int selectedTraitIdx = traitIdx;
			setTraitMenu.add(new ContextMenuItem(names[traitIdx], () -> {
				map.getOrder()[cornerIdx] = selectedTraitIdx;
				paint(true);
			}));
		}
		menu.add("Set trait '" + names[order[cornerIdx]] + "' to", setTraitMenu);
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
		Point2D p = toContent(x, y);
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
		Point2D p = toContent(x, y);
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
			export.append(Formatter.format(s3[0], 8));
			for (int k : order)
				export.append(", ").append(Formatter.format(s3[k + 1], 8));
			export.append("\n");
		}
	}
}
