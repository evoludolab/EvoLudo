//
// EvoLudo Project
//
// Copyright 2010 Christoph Hauert
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
//	Hauert, Christoph (<year>) EvoLudo Project, http://www.evoludo.org
//			(doi: <doi>[, <version>])
//
//	<doi>:	digital object identifier of the downloaded release (or the
//			most recent release if downloaded from github.com),
//	<year>:	year of release (or download), and
//	[, <version>]: optional version number (as reported in output header
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
import org.evoludo.ui.ContextMenu;
import org.evoludo.ui.ContextMenuItem;
import org.evoludo.math.Functions;
import org.evoludo.simulator.views.HasPhase2D.Data2Phase;
import org.evoludo.util.Formatter;
import org.evoludo.util.RingBuffer;

import com.google.gwt.core.client.Duration;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Touch;
import com.google.gwt.event.dom.client.DoubleClickEvent;
import com.google.gwt.event.dom.client.DoubleClickHandler;
import com.google.gwt.event.dom.client.TouchStartEvent;
import com.google.gwt.user.client.Command;

/**
 *
 * @author Christoph Hauert
 */
public class ParaGraph extends AbstractGraph implements Zooming, Shifting, HasTrajectory, //
	DoubleClickHandler {

	protected int nStates;
	protected double[] init;
	protected Data2Phase map;

	public ParaGraph(Controller controller, int nStates, int tag) {
		super(controller, tag);
		this.nStates = nStates;
		setStylePrimaryName("evoludo-ParaGraph");
	}

	@Override
	public void activate() {
		super.activate();
		doubleClickHandler = addDoubleClickHandler(this);
		autoscale();
	}

	@Override
	public void alloc() {
		super.alloc();
		// buffer length is nStates + 1 for time
		if (buffer == null || buffer.capacity() < MIN_BUFFER_SIZE) {
			buffer = new RingBuffer<double[]>(Math.max((int) bounds.getWidth(), DEFAULT_BUFFER_SIZE));
		}
		if (init == null || init.length != nStates + 1)
			init = new double[nStates + 1];
	}

	public void setMap(Data2Phase map) {
		if (map == null || map.equals(this.map))
			return;
		this.map = map;
	}

	public Data2Phase getMap() {
		return map;
	}

	public RingBuffer<double[]> getBuffer() {
		return buffer;
	}

	@Override
	public void reset() {
		super.reset();
		calcBounds();
		buffer.clear();
	}

	public void addData(double t, double[] data, boolean force) {
		if (buffer.isEmpty()) {
			buffer.append(prependTime2Data(t, data));
			System.arraycopy(buffer.last(), 1, init, 1, nStates);
		}
		else {
			double[] last = buffer.last();
			double lastt = last[0];
			if (Math.abs(t - lastt) < 1e-8) {
				buffer.replace(prependTime2Data(t, data));
				System.arraycopy(data, 0, init, 1, nStates);
			} else {
				if (Double.isNaN(t)) {
					// new starting point
					if (Double.isNaN(lastt))
						buffer.replace(prependTime2Data(t, data));
					else
						buffer.append(prependTime2Data(t, data));
					System.arraycopy(buffer.last(), 1, init, 1, nStates);
					return;
				}
				if (force || distSq(data, last) > bufferThreshold)
					buffer.append(prependTime2Data(t, data));
			}
		}
	}

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
	public void paint(boolean force) {
		if (!isActive || (!force && !doUpdate()))
			return;
		paintPara(true);
	 	tooltip.update();
	}

	private void paintPara(boolean withMarkers) {
		g.save();
		g.scale(scale,  scale);
		clearCanvas();
		g.translate(bounds.getX() - viewCorner.x, bounds.getY() - viewCorner.y);
		g.scale(zoomFactor, zoomFactor);
		g.save();
		double h = bounds.getHeight();
		g.translate(0, h);
		g.scale(1.0, -1.0);

		double xScale = bounds.getWidth()/(style.xMax-style.xMin);
		double yScale = h/(style.yMax-style.yMin);
		Point2D nextPt = new Point2D();
		Point2D currPt = new Point2D();
		String tC = style.trajColor;
		g.setStrokeStyle(tC);
		// increase line width for trajectories with transparency
		g.setLineWidth((tC.startsWith("rgba")?1.25*style.lineWidth:style.lineWidth));
		Iterator<double[]> i = buffer.iterator();
		if( i.hasNext() ) {
			double[] current = i.next();
			double ct = current[0];
			map.data2Phase(current, currPt);
			while( i.hasNext() ) {
				double[] prev = i.next();
				double pt = prev[0];
				map.data2Phase(prev, nextPt);
				if (!Double.isNaN(ct))
					strokeLine((nextPt.x - style.xMin) * xScale, (nextPt.y - style.yMin) * yScale, //
						(currPt.x - style.xMin) * xScale, (currPt.y - style.yMin) * yScale);
				current = prev;
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
			fillCircle((currPt.x - style.xMin) * xScale, (currPt.y - style.yMin) * yScale, style.markerSize);
			if (!buffer.isEmpty()) {
				map.data2Phase(buffer.last(), currPt);
				g.setFillStyle(style.endColor);
				fillCircle((currPt.x - style.xMin) * xScale, (currPt.y - style.yMin) * yScale, style.markerSize);
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
					fillCircle((currPt.x - style.xMin) * xScale, (currPt.y - style.yMin) * yScale, style.markerSize);
				}
				else {
					g.setLineWidth(style.lineWidth);
					g.setStrokeStyle(mcolor);
					strokeCircle((currPt.x - style.xMin) * xScale, (currPt.y - style.yMin) * yScale, style.markerSize);
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

	private static double MIN_PIXELS = 3.0;

	@Override
	protected boolean calcBounds() {
		if (!super.calcBounds())
			return false;
		// store point only if it is estimated to be at least a few pixels from the previous point
		bufferThreshold = MIN_PIXELS * scale * Math.min((style.xMax-style.xMin) / bounds.getWidth(), //
				(style.yMax-style.yMin) / bounds.getHeight());
		bufferThreshold *= bufferThreshold;
		return true;
	}

	public void autoscale() {
		double minmax;
		style.xMin = (Double.isNaN(minmax = map.getMinX(buffer)) ? 0.0 : Functions.roundDown(minmax));
		style.xMax = (Double.isNaN(minmax = map.getMaxX(buffer)) ? 1.0 : Functions.roundUp(minmax));
		style.yMin = (Double.isNaN(minmax = map.getMinY(buffer)) ? 0.0 : Functions.roundDown(minmax));
		style.yMax = (Double.isNaN(minmax = map.getMaxY(buffer)) ? 1.0 : Functions.roundUp(minmax));
	}

	private boolean paintScheduled = false;

	protected void schedulePaint() {
		if( paintScheduled ) return;
		Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
			@Override
			public void execute() {
				paint();
				paintScheduled = false;
			}
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

	private void processInitXY(double x, double y) {
		int sx = (int) ((viewCorner.x + x - bounds.getX()) / zoomFactor + 0.5);
		int sy = (int) ((viewCorner.y + y - bounds.getY()) / zoomFactor + 0.5);
		if( !inside(sx, sy) )
			return;
		double ux = style.xMin + sx / bounds.getWidth() * (style.xMax - style.xMin);
		double uy = style.yMin + (1.0 - sy / bounds.getHeight()) * (style.yMax - style.yMin);
		double[] state = new double[nStates];
		System.arraycopy(buffer.last(), 1, state, 0, nStates);
		map.phase2Data(new Point2D(ux, uy), state);
		controller.setInitialState(state);
	}

//CHECK!
	@Override
	public void onTouchStart(TouchStartEvent event) {
		super.onTouchStart(event);
		JsArray<Touch> touches = event.getTouches();
		if( Duration.currentTimeMillis()-touchEndTime>250.0 || touches.length()>1 )
			// single tap or multiple touches
			return;

		Touch touch = touches.get(0);
		processInitXY(touch.getRelativeX(element), touch.getRelativeY(element));
		event.preventDefault();
	}

	@Override
	public String getTooltipAt(int x, int y) {
		int sx = (int) ((viewCorner.x + x - bounds.getX()) / zoomFactor + 0.5);
		int sy = (int) ((viewCorner.y + y - bounds.getY()) / zoomFactor + 0.5);
		if( !inside(sx, sy) )
			return null;
		double ux = style.xMin + sx / bounds.getWidth() * (style.xMax - style.xMin);
		double uy = style.yMin + (1.0 - sy / bounds.getHeight()) * (style.yMax - style.yMin);
		return map.getTooltipAt(ux, uy);
	}

	/**
	 * Check if point (in user coordinates but not yet scaled to axis) lies inside
	 * of phase plane.
	 * 
	 * @param x <code>x</code>-coordinate of point
	 * @param y <code>y</code>-coordinate of point
	 * @return <code>true</code> if inside
	 */
	private boolean inside(double x, double y) {
		return !( x<0 || y<0 || x>bounds.getWidth() || y>bounds.getHeight());
	}

	private ContextMenuItem clearMenu, autoscaleMenu;

	@Override
	public void populateContextMenuAt(ContextMenu menu, int x, int y) {
		// add menu to clear canvas
		if( clearMenu==null ) {
			clearMenu = new ContextMenuItem("Clear", new Command() {
				@Override
				public void execute() {
					buffer.clear();
					paint(true);
				}
			});
		}
		if( autoscaleMenu==null ) {
			autoscaleMenu = new ContextMenuItem("Autoscale Axis", new Command() {
				@Override
				public void execute() {
					// map.reset(); // see Phase2D.update - requires updateMinMaxState before painting
					autoscale();
					paint(true);
				}
			});
		}
		menu.add(clearMenu);
		menu.add(autoscaleMenu);
		super.populateContextMenuAt(menu, x, y);
	}

	@Override
	public void exportTrajectory(StringBuilder export) {
		if (buffer.isEmpty())
			return;
		// extract the parametric data from buffer
		export.append("# time, " + map.getXAxisLabel() + ", " + map.getYAxisLabel() + "\n");
		Point2D point = new Point2D();
		Iterator<double[]> entry = buffer.ordered();
		while (entry.hasNext()) {
			double[] data = entry.next();
			map.data2Phase(data, point);
			export.append(Formatter.format(data[0], 8) + ", " + //
					Formatter.format(point.x, 8) + ", " + //
					Formatter.format(point.y, 8) + "\n");
		}
	}
}
