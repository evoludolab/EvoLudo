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
import org.evoludo.simulator.views.BasicTooltipProvider;
import org.evoludo.simulator.views.HasS3;
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
import com.google.gwt.user.client.Command;
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
public class S3Graph extends AbstractGraph<double[]> implements Zooming, Shifting, HasTrajectory, //
		DoubleClickHandler {

	/**
	 * The names of the traits.
	 */
	private String[] names;

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
	 * controller} with the specified {@code role}.
	 * 
	 * @param controller the controller of this graph
	 * @param module     the module backing the graph
	 * @param role       the role of the data
	 */
	public S3Graph(Controller controller, Module module, int role) {
		super(controller, module);
		this.role = role;
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
		if (map instanceof BasicTooltipProvider)
			setTooltipProvider((BasicTooltipProvider) map);
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
			buffer = new RingBuffer<double[]>(Math.max((int) bounds.getWidth(), DEFAULT_BUFFER_SIZE));
		colors = ColorMapCSS.Color2Css(map.getColors());		
		paint(true);
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
			buffer.append(prependTime2Data(t, data));
			int len = data.length;
			if (init == null || init.length != len)
				init = new double[len + 1]; // add time
			System.arraycopy(buffer.last(), 1, init, 1, len);
		} else {
			double[] last = buffer.last();
			double lastt = last[0];
			int len = last.length - 1;
			if (Math.abs(t - lastt) < 1e-8) {
				buffer.replace(prependTime2Data(t, data));
				System.arraycopy(last, 1, init, 1, len);
			} else {
				if (Double.isNaN(t)) {
					// new starting point
					if (Double.isNaN(lastt))
						buffer.replace(prependTime2Data(t, data));
					else
						buffer.append(prependTime2Data(t, data));
					System.arraycopy(buffer.last(), 1, init, 1, len);
					return;
				}
				if (force || distSq(data, last) > bufferThreshold)
					buffer.append(prependTime2Data(t, data));
			}
		}
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
		g.translate(bounds.getX() - viewCorner.x, bounds.getY() - viewCorner.y);
		g.scale(zoomFactor, zoomFactor);

		Point2D prevPt = new Point2D();
		Point2D currPt = new Point2D();
		double w = bounds.getWidth();
		double h = bounds.getHeight();
		String tC = style.trajColor;
		g.setStrokeStyle(tC);
		// increase line width for trajectories with transparency
		g.setLineWidth((tC.startsWith("rgba") ? 1.25 * style.lineWidth : style.lineWidth));
		Iterator<double[]> i = buffer.iterator();
		if (i.hasNext()) {
			double[] current = i.next();
			double ct = current[0];
			map.data2S3(current, currPt);
			currPt.x *= w;
			currPt.y *= h;
			while (i.hasNext()) {
				double[] prev = i.next();
				double pt = prev[0];
				map.data2S3(prev, prevPt);
				prevPt.x *= w;
				prevPt.y *= h;
				if (!Double.isNaN(ct))
					strokeLine(prevPt.x, prevPt.y, currPt.x, currPt.y);
				current = prev;
				ct = pt;
				Point2D swap = currPt;
				currPt = prevPt;
				prevPt = swap;
			}
		}
		drawFrame(3);
		if (withMarkers) {
			g.setFillStyle(style.startColor);
			map.data2S3(init, prevPt);
			fillCircle(prevPt.x * w, prevPt.y * h, style.markerSize);
			if (!buffer.isEmpty()) {
				g.setFillStyle(style.endColor);
				map.data2S3(buffer.last(), currPt);
				fillCircle(currPt.x * w, currPt.y * h, style.markerSize);
			}
		}
		if (markers != null) {
			int n = 0;
			int nMarkers = markers.size();
			for (double[] mark : markers) {
				map.data2S3(mark, currPt);
				String mcolor = markerColors[n++ % nMarkers];
				if (mark[0] > 0.0) {
					g.setFillStyle(mcolor);
					fillCircle(currPt.x * w, currPt.y * h, style.markerSize);
				} else {
					g.setLineWidth(style.lineWidth);
					g.setStrokeStyle(mcolor);
					strokeCircle(currPt.x * w, currPt.y * h, style.markerSize);
				}
			}
		}
		g.restore();
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
	private static double MIN_PIXELS = 3.0;

	@Override
	public void calcBounds(int width, int height) {
		super.calcBounds(width, height);
		String font = g.getFont();
		if (style.showXTicks) {
			int tlen = style.tickLength;
			int tlen2 = (int) (style.tickLength * 0.5);
			bounds.adjust(tlen, tlen2, -tlen - tlen, -tlen - tlen2);
		}
		if (style.showXTickLabels) {
			setFont(style.ticksLabelFont);
			int tik2 = (int) (g.measureText(Formatter.format((style.xMax - style.xMin) / Math.PI, 2)).getWidth() * 0.5);
			bounds.adjust(tik2, 0, -tik2 - tik2, -14);
		}
		if (style.showLabel) {
			setFont(style.labelFont);
			// lower left & right
			int xshift = (int) (Math.max(g.measureText(map.getName(HasS3.CORNER_LEFT)).getWidth(),
					Math.max(g.measureText(map.getName(HasS3.CORNER_RIGHT)).getWidth(), 
							g.measureText(map.getName(HasS3.CORNER_TOP)).getWidth()))
					* 0.5 + 0.5);
			int yshift = 20;
			bounds.adjust(xshift, yshift, -xshift - xshift, -yshift - yshift);
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
		e0.x *= w;
		e0.y *= h;
		map.data2S3(0.0, 1.0, 0.0, e1);
		e1.x *= w;
		e1.y *= h;
		map.data2S3(0.0, 0.0, 1.0, e2);
		e2.x *= w;
		e2.y *= h;
		outline.reset();
		outline.moveTo(e0.x, e0.y);
		outline.lineTo(e1.x, e1.y);
		outline.lineTo(e2.x, e2.y);
		outline.closePath();
		// outline.lineTo(e0.x, e0.y);
		// store point only if it is estimated to be at least a few pixels from the
		// previous point
		bufferThreshold = MIN_PIXELS * scale / Math.max(bounds.getWidth(), bounds.getHeight());
		bufferThreshold *= bufferThreshold;
	}

	/**
	 * Draws the frame of the simplex. The corners are marked by the trait names in
	 * their respective colours. For visual
	 * guidance each side is subdivided into {@code sLevels} sublevels.
	 * 
	 * @param sLevels the number of sublevels for the frame
	 */
	public void drawFrame(int sLevels) {
		double w = bounds.getWidth();
		double h = bounds.getHeight();
		if (style.showFrame) {
			g.beginPath();
			g.moveTo(e0.x, e0.y);
			g.lineTo(e1.x, e1.y);
			g.lineTo(e2.x, e2.y);
			g.lineTo(e0.x, e0.y);
			g.closePath();
			g.setLineWidth(style.frameWidth);
			g.setStrokeStyle(style.frameColor);
			g.stroke();
		}
		if (style.showXLevels) {
			g.setLineWidth(style.frameWidth);
			g.setStrokeStyle(style.levelColor);
			Point2D start = new Point2D();
			Point2D end = new Point2D();
			double iLevels = 1.0 / sLevels;
			double x = iLevels;
			for (int l = 1; l < sLevels; l++) {
				map.data2S3(x, 1.0 - x, 0.0, start);
				map.data2S3(0.0, 1.0 - x, x, end);
				strokeLine(start.x * w, start.y * h, end.x * w, end.y * h);
				map.data2S3(0.0, x, 1.0 - x, start);
				map.data2S3(x, 0.0, 1.0 - x, end);
				strokeLine(start.x * w, start.y * h, end.x * w, end.y * h);
				map.data2S3(1.0 - x, 0.0, x, start);
				map.data2S3(1.0 - x, x, 0.0, end);
				strokeLine(start.x * w, start.y * h, end.x * w, end.y * h);
				x += iLevels;
			}
		}
		if (style.showXTicks) {
			g.setStrokeStyle(style.frameColor);
			g.setFillStyle(style.frameColor);
			setFont(style.ticksLabelFont);
			double len = Math.sqrt(h * h + w * w / 4);
			double ty = w / (len + len) * (style.tickLength + 1);
			double tx = h / len * (style.tickLength + 1);
			double iLevels = 1.0 / sLevels;
			double x = 0.0;
			String tick;
			Point2D loc = new Point2D();
			for (int l = 0; l <= sLevels; l++) {
				if (style.percentX)
					tick = Formatter.formatPercent(1.0 - x, 0);
				else
					tick = Formatter.format(1.0 - x, 2);
				map.data2S3(x, 1.0 - x, 0.0, loc);
				loc.x *= w;
				loc.y *= h;
				strokeLine(loc.x, loc.y, loc.x, loc.y + style.tickLength);
				if (style.showXTickLabels)
					// center tick labels with ticks
					g.fillText(tick, loc.x - g.measureText(tick).getWidth() * 0.5, loc.y + style.tickLength + 12.5);
				map.data2S3(0.0, x, 1.0 - x, loc);
				loc.x *= w;
				loc.y *= h;
				strokeLine(loc.x, loc.y, loc.x + tx, loc.y - ty);
				if (style.showXTickLabels)
					g.fillText(tick, loc.x + tx + 6, loc.y - ty + 3);
				map.data2S3(1.0 - x, 0.0, x, loc);
				loc.x *= w;
				loc.y *= h;
				strokeLine(loc.x, loc.y, loc.x - tx, loc.y - ty);
				if (style.showXTickLabels)
					g.fillText(tick, loc.x - tx - (g.measureText(tick).getWidth() + 6), loc.y - ty + 3);
				x += iLevels;
			}
		}
		if (style.showLabel) {
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
			loc.x *= w;
			loc.y *= h;
			String label = map.getName(HasS3.CORNER_LEFT);
			g.fillText(label, loc.x - g.measureText(label).getWidth() * 0.5, loc.y + yshift);
			g.setFillStyle(colors[order[1]]);
			map.data2S3(0.0, 1.0, 0.0, loc);
			loc.x *= w;
			loc.y *= h;
			label = map.getName(HasS3.CORNER_RIGHT);
			g.fillText(label, loc.x - g.measureText(label).getWidth() * 0.5, loc.y + yshift);
			g.setFillStyle(colors[order[2]]);
			map.data2S3(0.0, 0.0, 1.0, loc);
			loc.x *= w;
			loc.y *= h;
			label = map.getName(HasS3.CORNER_TOP);
			g.fillText(label, loc.x - g.measureText(label).getWidth() * 0.5, loc.y - 14.5);
		}
		if (style.label != null) {
			g.setFillStyle(style.labelColor);
			g.fillText(style.label, -10.0, -32.0);
		}
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
		return ((viewCorner.x + x - bounds.getX()) / zoomFactor + 0.5) / (bounds.getWidth() - 1.0);
	}

	/**
	 * Convert the {@code y}-component of the screen coordinates {@code (x, y)} into
	 * scaled (Cartesian) coordinates in {@code [0, 1]}.
	 * 
	 * @param y the {@code y}-coordinate on screen
	 * @return the scaled coordinate
	 */
	double scaledY(double y) {
		return 1.0 - ((viewCorner.y + y - bounds.getY()) / zoomFactor + 0.5) / (bounds.getHeight() - 1.0);
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
		controller.setInitialState(s3);
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
		final Point2D e0 = new Point2D(0.0, 0.0);
		final Point2D e2 = new Point2D(1.0, 0.0);
		final Point2D e1 = new Point2D(0.5, 1.0);
		Point2D p = new Point2D(sx, sy);
		boolean inside = (Segment2D.orientation(e0, e1, p) >= 0);
		if (!inside)
			return false;
		inside &= (Segment2D.orientation(e1, e2, p) >= 0);
		if (!inside)
			return false;
		return inside && (Segment2D.orientation(e2, e0, p) >= 0);
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

	/**
	 * The index of the corner closest to the mouse pointer.
	 */
	private int cornerIdx = -1;

	@Override
	public void populateContextMenuAt(ContextMenu menu, int x, int y) {
		// add menu to clear canvas
		if (clearMenu == null) {
			clearMenu = new ContextMenuItem("Clear", new Command() {
				@Override
				public void execute() {
					clearHistory();
					paint(true);
				}
			});
		}
		menu.addSeparator();
		menu.add(clearMenu);

		// process swap order context menu
		if (swapOrderMenu == null) {
			swapOrderMenu = new ContextMenuItem("Swap Order", new Command() {
				@Override
				public void execute() {
					int swap;
					int[] order = map.getOrder();
					String label = swapOrderMenu.getText();
					if (label.startsWith("Swap " + names[order[0]])) {
						swap = order[0];
						order[0] = order[1];
						order[1] = swap;
					} else if (label.startsWith("Swap " + names[order[1]])) {
						swap = order[1];
						order[1] = order[2];
						order[2] = swap;
					} else {
						swap = order[2];
						order[2] = order[0];
						order[0] = swap;
					}
					map.setOrder(order);
					paint(true);
				}
			});
		}
		menu.add(swapOrderMenu);
		int[] order = map.getOrder();
		switch (closestEdge(x, y)) {
			case HasS3.EDGE_LEFT:
				swapOrderMenu.setText("Swap " + names[order[0]] + " \u2194 " + names[order[1]]);
				break;
			case HasS3.EDGE_RIGHT:
				swapOrderMenu.setText("Swap " + names[order[1]] + " \u2194 " + names[order[2]]);
				break;
			// case HasS3.EDGE_BOTTOM:
			default:
				swapOrderMenu.setText("Swap " + names[order[2]] + " \u2194 " + names[order[0]]);
				break;
		}

		// process set strategy context menu for >3 traits (plus time)
		if (buffer.getDepth() > 4) {
			if (setTraitMenu == null) {
				setTraitMenu = new ContextMenu(menu);
				for (String name : names)
					setTraitMenu.add(new ContextMenuItem(name, new Command() {
						@Override
						public void execute() {
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
						}
					}));
			}
			cornerIdx = closestCorner(x, y);
			menu.add("Set trait '" + names[order[cornerIdx]] + "' to ...", setTraitMenu);
			// enable all traits
			for (Widget item : setTraitMenu)
				((ContextMenuItem) item).setEnabled(true);
			// disable already visible traits
			for (int t : order)
				((ContextMenuItem) setTraitMenu.getWidget(t)).setEnabled(false);
		}

		super.populateContextMenuAt(menu, x, y);
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
		export.append("# time, " + names[order[0]] + ", " + names[order[1]] + ", " + names[order[2]] + "\n");
		Iterator<double[]> entry = buffer.ordered();
		while (entry.hasNext()) {
			double[] s3 = entry.next();
			export.append(Formatter.format(s3[0], 8) + ", " + //
					Formatter.format(s3[order[0] + 1], 8) + ", " + //
					Formatter.format(s3[order[1] + 1], 8) + ", " + //
					Formatter.format(s3[order[2] + 1], 8) + "\n");
		}
	}
}
