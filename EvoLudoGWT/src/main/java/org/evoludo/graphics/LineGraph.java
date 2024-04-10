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
import org.evoludo.graphics.AbstractGraph.Shifting;
import org.evoludo.graphics.AbstractGraph.Zooming;
import org.evoludo.ui.ContextMenu;
import org.evoludo.ui.ContextMenuItem;
import org.evoludo.math.ArrayMath;
import org.evoludo.math.Functions;
import org.evoludo.util.RingBuffer;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.user.client.Command;

/**
 *
 * @author Christoph Hauert
 */
public class LineGraph extends AbstractGraph implements Shifting, Zooming {

	public interface LineGraphController extends Controller {
		public String getTooltipAt(LineGraph graph, double x, double y);
	}

	/**
	 * The number of lines shown on this graph.
	 */
	int nLines = -1;

	/**
	 * Set the number of lines shown on this graph.
	 * 
	 * @param nLines the number of lines shown
	 */
	public void setNLines(int nLines) {
		if (nLines != this.nLines)
			buffer = null;
		this.nLines = nLines;
	}

	/**
	 * Get the number of lines shown on this graph.
	 * 
	 * @return the number of lines shown
	 
	 */
	public int getNLines() {
		return nLines;
	}

	/**
	 * The default number of (time) steps shown on this graph.
	 */
	protected double steps = DEFAULT_STEPS;

	/**
	 * Create new line graph for {@code controller}. The {@code id} is used to
	 * distinguish different graphs of the same module to visualize different
	 * components of the data and represents the index of the data column.
	 * 
	 * @param controller the controller of this graph
	 * @param id         the id of the graph
	 */
	public LineGraph(Controller controller, int id) {
		super(controller, id);
		setStylePrimaryName("evoludo-LineGraph");
	}

	// note: labels, yMin and yMax etc. must be set at this point to calculate bounds
	@Override
	public void reset() {
		double oldMin = style.xMin;
		style.xMin = Functions.roundDown(style.xMin+0.5);
		if( style.xMax-style.xMin<1e-6 )
			style.xMin = style.xMax-1.0;
		super.reset();
		calcBounds();
		if (buffer == null || buffer.capacity() < MIN_BUFFER_SIZE)
			buffer = new RingBuffer<double[]>(Math.max((int) bounds.getWidth(), DEFAULT_BUFFER_SIZE));
		buffer.clear();
		setSteps(steps*(style.xMax-style.xMin)/(style.xMax-oldMin));
	}

	public void addData(double x, double[] data, boolean force) {
		// always add data
		buffer.append(prependTime2Data(x, data));
		// dynamically extend range if needed - never reduces range (would need to consult RingBuffer for this)
		double min = ArrayMath.min(data);
		// ignore NaN's in data
		if( min==min )
			style.yMin = Math.min(style.yMin, min);
		double max = ArrayMath.max(data);
		// ignore NaN's in data
		if( max==max )
			style.yMax = Math.max(style.yMax, max);
	}

	@Override
	public void paint(boolean force) {
		if (!isActive || (!force && !doUpdate()))
			return;
		g.save();
		g.scale(scale,  scale);
		clearCanvas();
		g.translate(bounds.getX(), bounds.getY());
		g.save();
		double w = bounds.getWidth();
		double h = bounds.getHeight();
		g.translate(w, h);
		g.scale(1.0, -1.0);

		double yScale = h/(style.yMax-style.yMin);
		Iterator<double[]> i = buffer.iterator();
		if( i.hasNext() ) {
			double[] current = i.next();
			g.setLineWidth(style.lineWidth);
			double xScale = w/(style.xMax-style.xMin);
			double start = -style.xMax*xScale;
			double end = start;
			while( i.hasNext() ) {
				double[] prev = i.next();
				if( current[0]<prev[0] ) {
					current = prev;
					end = start;
					continue;
				}
				double delta = (prev[0]-current[0])*xScale;
				start += delta;
				if (start < 0.0) {
					for( int n=0; n<nLines; n++ ) {
						setStrokeStyleAt(n);
						double pi = prev[n+1]-style.yMin, ci = current[n+1]-style.yMin;
						if( start>=-w && end<=0.0 ) {
							strokeLine(start, pi*yScale, end, ci*yScale);
							continue;
						}
						double s = Math.max(-w, start);
						double e = Math.min(0, end);
						double m = (ci-pi)/(end-start);
						strokeLine(s, (pi+m*(s-start))*yScale, e, (ci-m*(end-e))*yScale);
					}
					if( start<=-w )
						break;
				}
				current = prev;
				end = start;
			}
		}
		if (markers != null) {
			for (double[] mark : markers) {
				g.setLineDash(mark[0] > 0.0 ? style.dashedLine : style.dottedLine);
				for (int n = 0; n < nLines; n++) {
					double mn = (mark[n + 1] - style.yMin) * yScale;
					g.setStrokeStyle(markerColors[n % markerColors.length]);
					strokeLine(-w, mn, 0.0, mn);
				}
				g.setLineDash(style.solidLine);
			}
		}
		g.restore();
		drawFrame(4, 4);
		g.restore();
	 	tooltip.update();
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
		paint(true);
		g = bak;
	}

	protected double pinchScale = -Double.MAX_VALUE;
	protected Point2D pinch = new Point2D(0.5, 0.5);

	int totDx;
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
		double maxRange = buffer.capacity() * style.xIncr;
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

	protected static final int MIN_STEPS = 10;
	protected static final int DEFAULT_STEPS = 100;

	@Override
	public void zoom() {
		style.xMax = 0.0;
		style.xMin = style.xMax-Math.max(1.0, DEFAULT_STEPS*style.xIncr);
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
		double maxRange = buffer.capacity() * style.xIncr;
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

	public double getSteps() {
		return steps;
	}

	public void setSteps(double steps) {
		this.steps = Math.max(1.0, Math.min(buffer.capacity(), steps));
	}

	public RingBuffer<double[]> getBuffer() {
		return buffer;
	}

	@Override
	public String getTooltipAt(int x, int y) {
		if( leftMouseButton )
			return null;
		if( !bounds.contains(x, y) )
			return null;
		double sx = (x-bounds.getX()-0.5)/bounds.getWidth();
		if( sx<0.0 || sx>1.0 )
			return null;
		double height = bounds.getHeight();
		double sy = (height - (y - bounds.getY() + 0.5)) / height;
		if( sy<0.0 || sy>1.0 )
			return null;
		return ((LineGraphController)controller).getTooltipAt(this, sx, sy);
	}

	private ContextMenuItem clearMenu, zoomResetMenu, zoomInMenu, zoomOutMenu;

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
		menu.addSeparator();
		menu.add(clearMenu);
		menu.addSeparator();
		// process zoom context menu entries
		if( zoomInMenu==null ) 
			zoomInMenu = new ContextMenuItem("Zoom in x-axis (2x)", new ZoomCommand(2.0));
		menu.add(zoomInMenu);
		if( zoomOutMenu==null )
			zoomOutMenu = new ContextMenuItem("Zoom out x-axis (0.5x)", new ZoomCommand(0.5));
		menu.add(zoomOutMenu);
		if( zoomResetMenu==null )
			zoomResetMenu = new ContextMenuItem("Reset zoom", new ZoomCommand());
		menu.add(zoomResetMenu);
		super.populateContextMenuAt(menu, x, y);
	}

	public class ZoomCommand implements Command {
		double zoom = -1.0;

		public ZoomCommand() {	}

		public ZoomCommand(double zoom) {
			this.zoom = Math.max(0.0, zoom);
		}

		@Override
		public void execute() {
			if( zoom<0.0 )
				zoom();
			else
				zoom(zoom, 0.5, 0.5);
		}
	}
}
