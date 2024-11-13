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

import java.awt.Color;
import java.util.Iterator;

import org.evoludo.geom.Point2D;
import org.evoludo.graphics.AbstractGraph.Shifting;
import org.evoludo.graphics.AbstractGraph.Zooming;
import org.evoludo.math.ArrayMath;
import org.evoludo.math.Functions;
import org.evoludo.simulator.ColorMapCSS;
import org.evoludo.simulator.models.Data;
import org.evoludo.simulator.modules.Continuous;
import org.evoludo.simulator.modules.Module;
import org.evoludo.simulator.views.BasicTooltipProvider;
import org.evoludo.ui.ContextMenu;
import org.evoludo.ui.ContextMenuItem;
import org.evoludo.util.Formatter;
import org.evoludo.util.RingBuffer;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.user.client.Command;

/**
 * Graph to visualize time series data. The graph can be shifted and zoomed.
 * 
 * @author Christoph Hauert
 */
public class LineGraph extends AbstractGraph<double[]> implements Shifting, Zooming, BasicTooltipProvider {

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
	 * @param module     the module backing the graph
	 */
	public LineGraph(Controller controller, Module module) {
		super(controller, module);
		setStylePrimaryName("evoludo-LineGraph");
		setTooltipProvider(this);
	}

	// note: labels, yMin and yMax etc. must be set at this point to calculate
	// bounds
	@Override
	public void reset() {
		double oldMin = style.xMin;
		style.xMin = Functions.roundDown(style.xMin + 0.5);
		if (style.xMax - style.xMin < 1e-6)
			style.xMin = style.xMax - 1.0;
		super.reset();
		calcBounds();
		if (buffer == null || buffer.getCapacity() < MIN_BUFFER_SIZE)
			buffer = new RingBuffer<double[]>(Math.max((int) bounds.getWidth(), DEFAULT_BUFFER_SIZE));
		setSteps(steps * (style.xMax - style.xMin) / (style.xMax - oldMin));
	}

	/**
	 * Add data to the graph. The time {@code t} is prepended to the data as the
	 * first element.
	 * 
	 * @evoludo.impl The data array is cloned and the time prepended before adding
	 *               it to the buffer.
	 * 
	 * @param t    the time of the data
	 * @param data the data to add
	 */
	public void addData(double t, double[] data) {
		addData(prependTime2Data(t, data));
	}

	/**
	 * Add data to the graph.
	 * 
	 * @evoludo.impl The data array is directly added to the buffer. It is the
	 *               caller's responsibility to ensure that the first entry
	 *               represents time and the data remains unmodified.
	 * 
	 * @param data the data to add
	 */
	public void addData(double[] data) {
		// always add data
		buffer.append(data);
		// time does not count for min/max
		double t = data[0];
		data[0] = style.yMin;
		// dynamically extend range if needed - never reduces range (would need to
		// consult RingBuffer for this)
		double min = ArrayMath.min(data);
		// ignore NaN's in data
		if (min == min)
			style.yMin = Math.min(style.yMin, min);
		double max = ArrayMath.max(data);
		// ignore NaN's in data
		if (max == max)
			style.yMax = Math.max(style.yMax, max);
		data[0] = t;
	}

	@Override
	public boolean paint(boolean force) {
		if (super.paint(force))
			return true;
		if (!force && !doUpdate())
			return true;
		g.save();
		g.scale(scale, scale);
		clearCanvas();
		g.translate(bounds.getX(), bounds.getY());
		g.save();
		double w = bounds.getWidth();
		double h = bounds.getHeight();
		g.translate(w, h);
		g.scale(1.0, -1.0);

		double yScale = h / (style.yMax - style.yMin);
		Iterator<double[]> i = buffer.iterator();
		int nLines = buffer.getDepth() - 1;
		if (i.hasNext()) {
			double[] current = i.next();
			g.setLineWidth(style.lineWidth);
			double xScale = w / (style.xMax - style.xMin);
			double start = -style.xMax * xScale;
			for (int n = 0; n < nLines; n++) {
				g.setFillStyle(colors[n]);
				fillCircle(start, (current[n + 1] - style.yMin) * yScale, style.markerSize);
			}
			double end = start;
			while (i.hasNext()) {
				double[] prev = i.next();
				if (current[0] <= prev[0]) {
					current = prev;
					end = start;
					continue;
				}
				double delta = (prev[0] - current[0]) * xScale;
				start += delta;
				if (start < 0.0) {
					for (int n = 0; n < nLines; n++) {
						setStrokeStyleAt(n);
						double pi = prev[n + 1] - style.yMin;
						double ci = current[n + 1] - style.yMin;
						if (start >= -w && end <= 0.0) {
							strokeLine(start, pi * yScale, end, ci * yScale);
							continue;
						}
						double s = Math.max(-w, start);
						double e = Math.min(0, end);
						double m = (ci - pi) / (end - start);
						strokeLine(s, (pi + m * (s - start)) * yScale, e, (ci - m * (end - e)) * yScale);
					}
					if (start <= -w)
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
		return false;
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
		if (!bounds.contains(x, y))
			return null;
		double sx = (x - bounds.getX() - 0.5) / bounds.getWidth();
		if (sx < 0.0 || sx > 1.0)
			return null;
		double height = bounds.getHeight();
		double sy = (height - (y - bounds.getY() + 0.5)) / height;
		if (sy < 0.0 || sy > 1.0)
			return null;
		return tooltipProvider.getTooltipAt(sx, sy);
	}

	@Override
	public String getTooltipAt(double x, double y) {
		double buffert = 0.0;
		double mouset = style.xMin + x * (style.xMax - style.xMin);
		// boolean hasVacant = !(model instanceof Model.DE && ((Model.DE)
		// model).isDensity());
		int vacant = module.getVacant();
		boolean hasVacant = (vacant >= 0);
		Iterator<double[]> i = buffer.iterator();
		String tip = "<table style='border-collapse:collapse;border-spacing:0;'>" +
				(style.label != null ? "<tr><td><b>" + style.label + "</b></td></tr>" : "") +
				"<tr><td style='text-align:right'><i>" + style.xLabel + ":</i></td><td>" +
				Formatter.format(mouset, 2) + "</td></tr>" +
				"<tr><td style='text-align:right'><i>" + style.yLabel + ":</i></td><td>" +
				(style.percentY ? Formatter.formatPercent(style.yMin + y * (style.yMax - style.yMin), 1)
						: Formatter.format(style.yMin + y * (style.yMax - style.yMin), 2))
				+ "</td></tr>";
		if (i.hasNext()) {
			double[] current = i.next();
			int len = current.length;
			while (i.hasNext()) {
				double[] prev = i.next();
				double dt = current[0] - prev[0];
				buffert -= Math.max(0.0, dt);
				if (buffert > mouset) {
					current = prev;
					continue;
				}
				double fx = 1.0 - (mouset - buffert) / dt;
				tip += "<tr><td colspan='2'><hr/></td></tr><tr><td style='text-align:right'><i>" + style.xLabel +
						":</i></td><td>" + Formatter.format(current[0] - fx * dt, 2) + "</td></tr>";
				Color[] colors = module.getTraitColors();
				if (module instanceof Continuous) {
					double inter = interpolate(current[1], prev[1], fx);
					tip += "<tr><td style='text-align:right'><i style='color:"
							+ ColorMapCSS.Color2Css(colors[0]) + ";'>mean:</i></td><td>" +
							(style.percentY ? Formatter.formatPercent(inter, 2) : Formatter.format(inter, 2));
					double sdev = inter - interpolate(current[2], prev[2], fx); // data: mean, mean-sdev, mean+sdev
					tip += " ± " + (style.percentY ? Formatter.formatPercent(sdev, 2) : Formatter.format(sdev, 2))
							+ "</td></tr>";
				} else {
					// len includes time
					for (int n = 0; n < len - 1; n++) {
						if (!hasVacant && n == vacant)
							continue;
						String name;
						Color color;
						Data type = controller.getType();
						int n1 = n + 1;
						if (n1 == len - 1 && type == Data.FITNESS) {
							name = "average";
							color = Color.BLACK;
						} else {
							name = module.getTraitName(n);
							color = colors[n];
						}
						if (name == null)
							continue;
						tip += "<tr><td style='text-align:right'><i style='color:" + ColorMapCSS.Color2Css(color)
								+ ";'>" + name + ":</i></td><td>";
						// deal with NaN's
						if (prev[n1] == prev[n1] && current[n1] == current[n1]) {
							tip += (style.percentY ? Formatter.formatPercent(interpolate(current[n1], prev[n1], fx), 2)
									: Formatter.format(interpolate(current[n1], prev[n1], fx), 2)) + "</td></tr>";
						} else
							tip += "-</td></tr>";
					}
				}
				break;
			}
		}
		return tip + "</table>";
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

	/**
	 * The context menu item to clear the graph.
	 */
	private ContextMenuItem clearMenu;

	/**
	 * The context menu to reset the zoom.
	 */
	private ContextMenuItem zoomResetMenu;

	/**
	 * The context menu to zoom in.
	 */
	private ContextMenuItem zoomInMenu;

	/**
	 * The context menu to zoom out.
	 */
	private ContextMenuItem zoomOutMenu;

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
		menu.addSeparator();
		// process zoom context menu entries
		if (zoomInMenu == null)
			zoomInMenu = new ContextMenuItem("Zoom in x-axis (2x)", new ZoomCommand(2.0));
		menu.add(zoomInMenu);
		if (zoomOutMenu == null)
			zoomOutMenu = new ContextMenuItem("Zoom out x-axis (0.5x)", new ZoomCommand(0.5));
		menu.add(zoomOutMenu);
		if (zoomResetMenu == null)
			zoomResetMenu = new ContextMenuItem("Reset zoom", new ZoomCommand());
		menu.add(zoomResetMenu);
		super.populateContextMenuAt(menu, x, y);
	}

	/**
	 * The command to change the zoom level.
	 */
	public class ZoomCommand implements Command {

		/**
		 * The zoom level.
		 */
		double zoom = -1.0;

		/**
		 * Create new zoom command.
		 */
		public ZoomCommand() {
		}

		/**
		 * Create new zoom command with the specified zoom level.
		 * 
		 * @param zoom the zoom level
		 */
		public ZoomCommand(double zoom) {
			this.zoom = Math.max(0.0, zoom);
		}

		@Override
		public void execute() {
			if (zoom < 0.0)
				zoom();
			else
				zoom(zoom, 0.5, 0.5);
		}
	}
}
