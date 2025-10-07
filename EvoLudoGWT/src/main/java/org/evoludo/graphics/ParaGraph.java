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
import org.evoludo.math.ArrayMath;
import org.evoludo.math.Functions;
import org.evoludo.simulator.modules.Module;
import org.evoludo.simulator.views.BasicTooltipProvider;
import org.evoludo.simulator.views.HasPhase2D;
import org.evoludo.simulator.views.Phase2D;
import org.evoludo.simulator.views.HasPhase2D.Data2Phase;
import org.evoludo.ui.ContextMenu;
import org.evoludo.ui.ContextMenuCheckBoxItem;
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
 * Parametric graph for displaying trajectories in phase plane. The graph is
 * used to display trajectories in phase space, i.e. the state of a system as a
 * function of time.
 * <p>
 * The graph is backed by a {@link RingBuffer} to store the trajectory. The
 * buffer is updated by calling {@link #addData(double, double[], boolean)}. It
 * is interactive and allows the user to zoom and shift the view. The user can
 * set the initial state by double-clicking on the graph. The graph can be
 * exported in PNG or SVG graphics formats or the trajectory data as CSV.
 * <p>
 * The graph provides fine grained configurations for mapping the data to the
 * phase plane, for tooltips, for markers as well as for the style of the graph.
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
	 * The flag to indicate autoscaling of axes.
	 */
	boolean doAutoscale = true;

	/**
	 * Create new parametric graph for <code>module</code> running in
	 * <code>view</code>.
	 * 
	 * @param view   the view of this graph
	 * @param module the module backing the graph
	 */
	public ParaGraph(Phase2D view, Module module) {
		super(view, module);
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
			setMap(new TraitMap());
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
			return;
		}
		double[] last = buffer.last();
		double lastt = last[0];
		int len = last.length;
		if (Math.abs(t - lastt) < 1e-8) {
			buffer.replace(prependTime2Data(t, data));
			System.arraycopy(data, 0, init, 1, len - 1);
			return;
		}
		if (Double.isNaN(t)) {
			handleNaNTime(lastt, t, data, len);
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
	 * @param t     the time of the data
	 * @param data  the data to add
	 * @param lastt the time of the last data point in the buffer
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
		double xScale = bounds.getWidth() / (style.xMax - style.xMin);
		double yScale = h / (style.yMax - style.yMin);

		Point2D nextPt = new Point2D();
		Point2D currPt = new Point2D();
		String tC = style.trajColor;
		g.setStrokeStyle(tC);
		// increase line width for trajectories with transparency
		g.setLineWidth((tC.startsWith("rgba") ? 1.25 * style.lineWidth : style.lineWidth));
		Iterator<double[]> i = buffer.iterator();
		if (i.hasNext()) {
			double[] current = i.next();
			double ct = current[0];
			// current is last point added to buffer
			map.data2Phase(current, currPt);
			// update axes range if necessary
			if (doAutoscale) {
				style.xMin = Functions.roundDown(Math.min(style.xMin, map.getMinX(buffer)));
				style.xMax = Functions.roundUp(Math.max(style.xMax, map.getMaxX(buffer)));
				if (style.percentX) {
					style.xMin = Math.max(style.xMin, 0.0);
					style.xMax = Math.min(style.xMax, 1.0);
				}
				style.yMin = Functions.roundDown(Math.min(style.yMin, map.getMinY(buffer)));
				style.yMax = Functions.roundUp(Math.max(style.yMax, map.getMaxY(buffer)));
				if (style.percentX) {
					style.yMin = Math.max(style.yMin, 0.0);
					style.yMax = Math.min(style.yMax, 1.0);
				}
			}
			while (i.hasNext()) {
				double[] prev = i.next();
				double pt = prev[0];
				map.data2Phase(prev, nextPt);
				if (!Double.isNaN(ct))
					strokeLine((nextPt.getX() - style.xMin) * xScale, (nextPt.getY() - style.yMin) * yScale, //
							(currPt.getX() - style.xMin) * xScale, (currPt.getY() - style.yMin) * yScale);
				ct = pt;
				Point2D swap = currPt;
				currPt = nextPt;
				nextPt = swap;
			}
		}
		if (withMarkers) {
			// mark start and end points of trajectory
			map.data2Phase(init, currPt);
			g.setFillStyle(style.startColor);
			fillCircle((currPt.getX() - style.xMin) * xScale, (currPt.getY() - style.yMin) * yScale, style.markerSize);
			if (!buffer.isEmpty()) {
				map.data2Phase(buffer.last(), currPt);
				g.setFillStyle(style.endColor);
				fillCircle((currPt.getX() - style.xMin) * xScale, (currPt.getY() - style.yMin) * yScale,
						style.markerSize);
			}
		}
		if (markers != null) {
			int n = 0;
			int nMarkers = markers.size();
			for (double[] mark : markers) {
				map.data2Phase(mark, currPt);
				String mcolor = markerColors[n++ % nMarkers];
				if (mark[0] > 0.0) {
					g.setFillStyle(mcolor);
					fillCircle((currPt.getX() - style.xMin) * xScale, (currPt.getY() - style.yMin) * yScale,
							style.markerSize);
				} else {
					g.setLineWidth(style.lineWidth);
					g.setStrokeStyle(mcolor);
					strokeCircle((currPt.getX() - style.xMin) * xScale, (currPt.getY() - style.yMin) * yScale,
							style.markerSize);
				}
			}
		}
		g.restore();
		drawFrame(4, 4);
		g.restore();
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
	 * Automatically adjust the range of both axes to fit the data in the buffer.
	 */
	public void autoscale() {
		if (!doAutoscale)
			return;
		map.reset();
		double min = map.getMinX(buffer);
		double max = map.getMaxX(buffer);
		if (min == max) {
			min *= 0.99;
			max /= 0.99;
		}
		style.xMin = Functions.roundDown(min);
		style.xMax = Functions.roundUp(max);
		min = map.getMinY(buffer);
		max = map.getMaxY(buffer);
		if (min == max) {
			min *= 0.99;
			max /= 0.99;
		}
		style.yMin = Functions.roundDown(min);
		style.yMax = Functions.roundUp(max);
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
	 * The context menu item to autoscale the axes.
	 */
	private ContextMenuCheckBoxItem autoscaleMenu;

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
		// add autoscale menu if not percent scale
		if (!(style.percentX || style.percentY)) {
			if (autoscaleMenu == null) {
				autoscaleMenu = new ContextMenuCheckBoxItem("Autoscale axes", () -> {
					doAutoscale = !autoscaleMenu.isChecked();
					autoscaleMenu.setChecked(doAutoscale);
					autoscale();
					paint(true);
				});
			}
			autoscaleMenu.setChecked(doAutoscale);
			menu.add(autoscaleMenu);
		}
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

	/**
	 * Default mapping of data to the phase plane or phase plane projections.
	 * Projections can be the sum of several dynamical variables. Custom
	 * implementations of the {@code Data2Phase} interface can be provided by
	 * modules that implement the {@code HasPhase2D} interface.
	 * 
	 * @see HasPhase2D#getPhase2DMap()
	 */
	public class TraitMap implements Data2Phase, BasicTooltipProvider {

		/**
		 * Flag indicating whether the axes are fixed. The default is fixed axes.
		 */
		boolean hasFixedAxes = false;

		/**
		 * The array of trait indices that are mapped to the <code>x</code>-axis.
		 */
		protected int[] stateX = new int[] { 0 };

		/**
		 * The array of trait indices that are mapped to the <code>y</code>-axis.
		 */
		protected int[] stateY = new int[] { 1 };

		/**
		 * The minimum value of the <code>x</code>-axis.
		 */
		protected double minX;

		/**
		 * The maximum value of the <code>x</code>-axis.
		 */
		protected double maxX;

		/**
		 * The minimum value of the <code>y</code>-axis.
		 */
		protected double minY;

		/**
		 * The maximum value of the <code>y</code>-axis.
		 */
		protected double maxY;

		@Override
		public void reset() {
			minX = Double.POSITIVE_INFINITY;
			maxX = Double.NEGATIVE_INFINITY;
			minY = Double.POSITIVE_INFINITY;
			maxY = Double.NEGATIVE_INFINITY;
		}

		@Override
		public void setTraits(int[] x, int[] y) {
			stateX = ArrayMath.clone(x);
			stateY = ArrayMath.clone(y);
		}

		@Override
		public int[] getTraitsX() {
			return stateX;
		}

		@Override
		public int[] getTraitsY() {
			return stateY;
		}

		@Override
		public boolean hasMultitrait() {
			return true;
		}

		@Override
		public boolean hasFixedAxes() {
			return hasFixedAxes;
		}

		@Override
		public void setFixedAxes(boolean hasFixedAxes) {
			this.hasFixedAxes = hasFixedAxes;
		}

		@Override
		public boolean data2Phase(double[] data, Point2D point) {
			// NOTE: data[0] is time
			double x = data[stateX[0] + 1];
			int nx = stateX.length;
			if (nx > 1) {
				for (int n = 1; n < nx; n++)
					x += data[stateX[n] + 1];
			}
			minX = Math.min(minX, x);
			maxX = Math.max(maxX, x);
			double y = data[stateY[0] + 1];
			int ny = stateY.length;
			if (ny > 1) {
				for (int n = 1; n < ny; n++)
					y += data[stateY[n] + 1];
			}
			minY = Math.min(minY, y);
			maxY = Math.max(maxY, y);
			point.set(x, y);
			return true;
		}

		@Override
		public boolean phase2Data(Point2D point, double[] data) {
			// point is in user coordinates
			// data is the last/most recent state in buffer (excluding time!)
			if (stateX.length != 1 || stateY.length != 1)
				return false;
			// conversion only possible if each phase plane axis represents a single
			// dynamical variable, i.e. no aggregates
			data[stateX[0]] = point.getX();
			data[stateY[0]] = point.getY();
			return true;
		}

		@Override
		public double getMinX(RingBuffer<double[]> buffer) {
			if (!Double.isFinite(minX))
				minX = findMin(buffer, stateX);
			return minX;
		}

		@Override
		public double getMaxX(RingBuffer<double[]> buffer) {
			if (!Double.isFinite(maxX))
				maxX = findMax(buffer, stateX);
			return maxX;
		}

		@Override
		public double getMinY(RingBuffer<double[]> buffer) {
			if (!Double.isFinite(minY))
				minY = findMin(buffer, stateY);
			return minY;
		}

		@Override
		public double getMaxY(RingBuffer<double[]> buffer) {
			if (!Double.isFinite(maxY))
				maxY = findMax(buffer, stateY);
			return maxY;
		}

		/**
		 * Find the minimum value of the data in the buffer accross all the indices in
		 * <code>idxs</code>.
		 * 
		 * @param buffer the buffer with data points
		 * @param idxs   the array of indices
		 * @return the minimum value
		 */
		private double findMin(RingBuffer<double[]> buffer, int[] idxs) {
			double min = Double.POSITIVE_INFINITY;
			for (double[] data : buffer) {
				double d = data[idxs[0] + 1];
				int nd = idxs.length;
				if (nd > 1) {
					for (int n = 1; n < nd; n++)
						d += data[idxs[n] + 1];
				}
				min = Math.min(min, d);
			}
			return min;
		}

		/**
		 * Find the maximum value of the data in the buffer accross all the indices in
		 * <code>idxs</code>.
		 * 
		 * @param buffer the buffer with data points
		 * @param idxs   the array of indices
		 * @return the maximum value
		 */
		private double findMax(RingBuffer<double[]> buffer, int[] idxs) {
			double max = Double.NEGATIVE_INFINITY;
			for (double[] data : buffer) {
				double d = data[idxs[0] + 1];
				int nd = idxs.length;
				if (nd > 1) {
					for (int n = 1; n < nd; n++)
						d += data[idxs[n] + 1];
				}
				max = Math.max(max, d);
			}
			return max;
		}

		@Override
		public String getTooltipAt(double x, double y) {
			String tip = "<table><tr><td style='text-align:right'><i>" + style.xLabel //
					+ ":</i></td><td>" + (style.percentX ? Formatter.formatPercent(x, 2) : Formatter.format(x, 2))
					+ "</td></tr>";
			tip += "<tr><td style='text-align:right'><i>" + style.yLabel //
					+ ":</i></td><td>" + (style.percentY ? Formatter.formatPercent(y, 2) : Formatter.format(y, 2))
					+ "</td></tr>";
			tip += "</table>";
			return tip;
		}
	}
}
