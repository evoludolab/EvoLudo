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

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.LinkedList;

import org.evoludo.math.Combinatorics;
import org.evoludo.simulator.modules.Module;

class Data {
	double time;
	double[] state;
	boolean connect;

	public Data(StateData data) {
		this.time = data.time;
		this.state = data.state.clone();
		this.connect = data.connect;
	}
}

class LineQueue extends LinkedList<Data> {

	private static final long serialVersionUID = 20110423L;

	double duration = 0.0;
	Data root = null;

	public void reset() {
		clear(); // empty list
	}

	public void reset(StateData data, double drtn) {
		duration = drtn;
		reset();
		root = new Data(data);
	}

	public void append(StateData data) {
		add(new Data(data));
	}

	@Override
	public boolean add(Data data) {
		super.add(data); // add to end
		Data ancestor = getFirst();
		while (data.time - ancestor.time > duration) {
			root = removeFirst();
			ancestor = getFirst();
		}
		return true;
	}

	public GeneralPath getPath(int idx, FrameLayer frame) {
		if (idx < 0 || idx >= root.state.length)
			return null;
		GraphAxis x = frame.xaxis, y = frame.yaxis;
		Rectangle canvas = frame.canvas;
		// what is the apparently heuristic -2 in xMap and yMap?
		double xMap = (canvas.width - 2) / (x.upper - x.lower);
		double xStart = getLast().time - (x.upper - x.lower); // the time of the most recently added data point
																// corresponds to x.upper
		double yMap = (canvas.height - 2) / (y.upper - y.lower);
		GeneralPath path = new GeneralPath();
		path.moveTo((root.time - xStart) * xMap, (root.state[idx] - y.lower) * yMap);
		for (Data data : this) {
			double ix = (data.time - xStart) * xMap;
			// what is the apparently heuristic -1 in iy?
			double iy = (canvas.height - 1) - (data.state[idx] - y.lower) * yMap;
			if (data.connect)
				path.lineTo(ix, iy);
			else
				path.moveTo(ix, iy);
		}
		return path;
	}

	// apparently SVGGraphics2D does not support clipping but we can do it
	// ourselves!
	public GeneralPath getClippedPath(int idx, FrameLayer frame) {
		if (idx < 0 || idx >= root.state.length)
			return null;
		GraphAxis x = frame.xaxis, y = frame.yaxis;
		Rectangle canvas = frame.canvas;
		// what is the apparently heuristic -2 in xMap and yMap? - see above
		double xMap = (canvas.width - 2) / (x.upper - x.lower);
		// double xStart = ((LineQueueData)getLast()).time-(x.upper-x.lower); // the
		// time of the most recently added data point corresponds to x.upper
		double xStart = getLast().time - (x.upper - x.lower); // the time of the most recently added data point
																// corresponds to x.upper
		double yMap = (canvas.height - 2) / (y.upper - y.lower);
		GeneralPath path = new GeneralPath();
		// the term 1.000001 looks like another heuristic hack...
		Rectangle2D.Double clip = new Rectangle2D.Double(xStart, y.lower, (x.upper - x.lower) * 1.000001,
				(y.upper - y.lower) * 1.000001);
		Point2D.Double p1 = new Point2D.Double(), p2 = new Point2D.Double();
		boolean p2inside = false;
		double state = root.state[idx];
		boolean skipped = true;
		if (state == state) { // silly check ensures state is valied - returns false for NaN
			path.moveTo((root.time - xStart) * xMap, (canvas.height - 1) - (state - y.lower) * yMap);
			p2.setLocation(root.time, state);
			p2inside = clip.contains(p2);
			skipped = false;
		}
		Line2D.Double p1p2 = new Line2D.Double();
		for (Data data : this) {
			state = data.state[idx];
			if (state != state) {
				// invalid state - skip
				skipped = true;
				continue;
			}
			p1.setLocation(p2);
			boolean p1inside = p2inside;
			p2.setLocation(data.time, data.state[idx]);
			p2inside = clip.contains(p2);
			if (skipped) {
				if (p2inside) {
					double ix = (data.time - xStart) * xMap;
					double iy = (canvas.height - 1) - (state - y.lower) * yMap;
					path.moveTo(ix, iy);
				}
				skipped = false;
				continue;
			}
			if (p1inside && p2inside) {
				double ix = (data.time - xStart) * xMap;
				// what is the apparently heuristic -1 in iy? - see above
				double iy = (canvas.height - 1) - (state - y.lower) * yMap;
				if (data.connect)
					path.lineTo(ix, iy);
				else
					path.moveTo(ix, iy);
				continue;
			}
			p1p2.setLine(p1, p2);

			if (p1inside || p2inside || p1p2.intersects(clip)) {
				Point2D.Double inter = intersection(p1p2, clip);
				double ix = (inter.x - xStart) * xMap;
				// what is the apparently heuristic -1 in iy? - see above
				double iy = (canvas.height - 1) - (inter.y - y.lower) * yMap;
				if (p2inside) {
					// p1 is outside
					path.moveTo(ix, iy);
					ix = (p2.x - xStart) * xMap;
					// what is the apparently heuristic -1 in iy? - see above
					iy = (canvas.height - 1) - (p2.y - y.lower) * yMap;
					path.lineTo(ix, iy);
					continue;
				}
				if (p1inside) {
					// p2 is outside
					path.lineTo(ix, iy);
					continue;
				}
				// both outside but line intersects clip
				path.moveTo(ix, iy);
				// determine second intersection points... check edges counter clockwise
				Point2D.Double inter2 = intersectCCW(p1p2, clip);
				ix = (inter2.x - xStart) * xMap;
				// what is the apparently heuristic -1 in iy? - see above
				iy = (canvas.height - 1) - (inter2.y - y.lower) * yMap;
				path.lineTo(ix, iy);
			}
		}
		return path;
	}

	private static Point2D.Double intersection(Line2D.Double line, Rectangle2D.Double rect) {
		// left edge
		Line2D.Double edge = new Line2D.Double(rect.x, rect.y, rect.x, rect.y + rect.height);
		if (Line2D.linesIntersect(line.x1, line.y1, line.x2, line.y2, rect.x, rect.y, rect.x, rect.y + rect.height))
			return intersectLines(line, edge);
		// top edge
		edge.setLine(rect.x, rect.y + rect.height, rect.x + rect.width, rect.y + rect.height);
		if (Line2D.linesIntersect(line.x1, line.y1, line.x2, line.y2, rect.x, rect.y + rect.height, rect.x + rect.width,
				rect.y + rect.height))
			return intersectLines(line, edge);
		// right edge
		edge.setLine(rect.x + rect.width, rect.y, rect.x + rect.width, rect.y + rect.height);
		if (Line2D.linesIntersect(line.x1, line.y1, line.x2, line.y2, rect.x + rect.width, rect.y, rect.x + rect.width,
				rect.y + rect.height))
			return intersectLines(line, edge);
		// bottom edge
		edge.setLine(rect.x, rect.y, rect.x + rect.width, rect.y);
		if (Line2D.linesIntersect(line.x1, line.y1, line.x2, line.y2, rect.x, rect.y, rect.x + rect.width, rect.y))
			return intersectLines(line, edge);

		// should not get here...
		System.out.println("HELP!!! line should intersect: " + line.intersects(rect) + " line=(" + line.getP1() + ", "
				+ line.getP2() + "), rect=" + rect);
		return new Point2D.Double(rect.x + rect.width / 2, rect.y + rect.height / 2);
	}

	private static Point2D.Double intersectCCW(Line2D.Double line, Rectangle2D.Double rect) {
		// bottom edge
		Line2D.Double edge = new Line2D.Double(rect.x, rect.y, rect.x + rect.width, rect.y);
		if (Line2D.linesIntersect(line.x1, line.y1, line.x2, line.y2, rect.x, rect.y, rect.x + rect.width, rect.y))
			return intersectLines(line, edge);
		// right edge
		edge.setLine(rect.x + rect.width, rect.y, rect.x + rect.width, rect.y + rect.height);
		if (Line2D.linesIntersect(line.x1, line.y1, line.x2, line.y2, rect.x + rect.width, rect.y, rect.x + rect.width,
				rect.y + rect.height))
			return intersectLines(line, edge);
		// top edge
		edge.setLine(rect.x, rect.y + rect.height, rect.x + rect.width, rect.y + rect.height);
		if (Line2D.linesIntersect(line.x1, line.y1, line.x2, line.y2, rect.x, rect.y + rect.height, rect.x + rect.width,
				rect.y + rect.height))
			return intersectLines(line, edge);
		// left edge
		edge.setLine(rect.x, rect.y, rect.x, rect.y + rect.height);
		if (Line2D.linesIntersect(line.x1, line.y1, line.x2, line.y2, rect.x, rect.y, rect.x, rect.y + rect.height))
			return intersectLines(line, edge);

		// should not get here...
		System.out.println("HELP!!! line should intersect: " + line.intersects(rect) + " line=(" + line.getP1() + ", "
				+ line.getP2() + "), rect=" + rect);
		return new Point2D.Double(rect.x + rect.width / 2, rect.y + rect.height / 2);
	}

	private static Point2D.Double intersectLines(Line2D.Double line1, Line2D.Double line2) {
		double a = (line1.y2 - line1.y1) / (line1.x2 - line1.x1);
		double b = line1.y1 - a * line1.x1;
		if (Math.abs(line2.x1 - line2.x2) < 1e-8)
			// line2 is vertical
			return new Point2D.Double(line2.x1, a * line2.x1 + b);

		double c = (line2.y2 - line2.y1) / (line2.x2 - line2.x1);
		double d = line2.y1 - c * line2.x1;

		if (Math.abs(line1.x1 - line1.x2) < 1e-8)
			// line1 is vertical
			return new Point2D.Double(line1.x1, c * line1.x1 + d);

		double x = (d - b) / (a - c);
		return new Point2D.Double(x, a * x + b);
	}
}

public class LineGraph extends AbstractGraph {

	private static final long serialVersionUID = 20110423L;

	private int nStates = -1;
	private final StateData state = new StateData();
	private Color[] colors;
	private final LineQueue svgQueue = new LineQueue();
	int row = -1;

	public LineGraph(StateGraphListener controller, Module module, int row) {
		super(controller, module);
		this.row = row;
		frame = new FrameLayer(style);
		add(frame, LAYER_FRAME);
		hasHistory = true;
	}

	@Override
	public void reset(boolean clear) {
		nStates = controller.getNData(row);
		state.init(nStates);
		state.isLocal = isLocalDynamics;
		((StateGraphListener) controller).getData(state, row);
		GraphAxis x = frame.xaxis;
		if (doSVG)
			svgQueue.reset(state, x.max - x.min);
		else
			svgQueue.reset();
		super.reset(clear);
	}

	@Override
	public void reinit() {
		colors = controller.getColors(row);
		((StateGraphListener) controller).getData(state, row);
		GraphAxis x = frame.xaxis, y = frame.yaxis;
		// JRE: only single species supported
		boolean changed = controller.verifyXAxis(x, 0);
		changed |= controller.verifyYAxis(y, 0);
		changed |= controller.verifyYThresholds(frame, 0);
		// do not restore zoom when clearing the canvas
		if (changed) {
			x.restore();
			y.restore();
			clear();
		}
		frame.init(getBounds());
		super.reinit();
	}

	// note: amalgamate svgQueue and svgPlot and let AbstractGraph take care of
	// clear() and deactivate()
	@Override
	public void clear() {
		super.clear();
		if (doSVG) {
			GraphAxis x = frame.xaxis;
			svgQueue.reset(state, x.upper - x.lower); // zoom may have changed the x-range
		}
	}

	@Override
	public void deactivate() {
		super.deactivate();
		if (doSVG)
			svgQueue.reset();
	}

	@Override
	protected void prepare() {
		state.next();
		((StateGraphListener) controller).getData(state, row);
	}

	@Override
	public void plot(Graphics2D g2d) {
		if (state.time <= timestamp) {
			if (doSVG)
				svgQueue.append(state);
			return; // up to date
		}
		timestamp = state.time;
		GraphAxis x = frame.xaxis;
		double xMap = canvas.width / (x.upper - x.lower);
		// note: improve dx - keep track of rounding errors
		// this is prone to overflows...
		int dx = (int) (state.time * xMap + 0.5) - (int) (state.pastTime * xMap + 0.5);
		if (dx < 0)
			return;
		// and this is prone to roundoff errors and screwing up the time scale...
		// int dx = Math.max(1,
		// (int)((currData[0]-prevData[0])*ixRange*plotdim.width+0.5));
		int shift = Math.min(canvas.width + 1, dx);
		Composite aComposite = g2d.getComposite();
		g2d.setComposite(AlphaComposite.Src);
		g2d.copyArea(shift, 0, canvas.width - shift + 1, canvas.height, -shift, 0);
		g2d.setComposite(AlphaComposite.Clear);
		g2d.fill(new Rectangle(canvas.width - shift, 0, shift, canvas.height));
		g2d.setComposite(aComposite);

		// draw lines
		if (state.connect) {
			g2d.setStroke(style.lineStroke);
			// GraphAxis y = frame.yaxis;
			for (int i = 0; i < nStates; i++) {
				// check if any state is a NaN
				if (state.pastState[i] != state.pastState[i] || state.state[i] != state.state[i])
					continue;
				g2d.setPaint(colors[i]);
				// what is the apparently heuristic -1 in x and width?
				g2d.drawLine(canvas.width - dx - 1, frame.convertY(state.pastState[i]),
						canvas.width - 1, frame.convertY(state.state[i]));
			}
		}
		if (doSVG)
			svgQueue.append(state);
	}

	@Override
	protected void printComponent(Graphics g) {
		super.printComponent(g);
		if (!doSVG)
			return;
		Graphics2D g2 = (Graphics2D) g;
		g2.setStroke(style.lineStroke);
		g2.translate(canvas.x, canvas.y);
		for (int i = 0; i < nStates; i++) {
			g2.setPaint(colors[i]);
			// SVGGraphics2D ignores clipping !@#$%^& - we have to do it ourselves (works
			// for PDFGraphics2D)
			g2.draw(svgQueue.getClippedPath(i, frame));
		}
		g2.translate(-canvas.x, -canvas.y);
	}

	// tool tips
	@Override
	public String getToolTipText(MouseEvent event) {
		Point loc = event.getPoint();
		if (!canvas.contains(loc))
			return null;

		GraphAxis x = frame.xaxis, y = frame.yaxis;
		double sx = (double) (loc.x - canvas.x) / (double) canvas.width;
		double sy = 1.0 - (double) (loc.y - canvas.y) / (double) canvas.height;
		return "<html><i>" + x.label + ":</i> " + x.formatter.format(x.lower + sx * (x.upper - x.lower)) +
				"<br><i>" + y.label + ":</i> " + y.formatter.format(y.lower + sy * (y.upper - y.lower));
	}

	// NOTE: this assumes x.max is fixed at 0.
	// rewrite - probably split into zoomX and zoomY and override zoomX only
	/**
	 *
	 */
	@Override
	protected void zoomRange() {
		GraphAxis x = frame.xaxis, y = frame.yaxis;
		double old = x.upper - x.lower;
		double range = (double) (drawRect.width + 1) / (double) canvas.width * old;
		double order = Combinatorics.pow(10.0, (int) Math.floor(Math.log10(range)));
		x.lower = x.upper - Math.round(range / order * 10.0) * 0.1 * order;

		old = y.upper - y.lower;
		range = (double) (drawRect.height + 1) / (double) canvas.height * old;
		order = Combinatorics.pow(10.0, (int) Math.floor(Math.log10(range)));
		double coord = (canvas.height - (drawRect.y - 1 - canvas.y)) / (double) canvas.height * old;
		y.upper = y.lower + Math.round(coord / order * 10.0) * 0.1 * order;
		coord = (canvas.height - (drawRect.y + drawRect.height - canvas.y)) / (double) canvas.height * old;
		y.lower += Math.round(coord / order * 10.0) * 0.1 * order;
		frame.formatTickLabels();
		clear();
	}

	@Override
	protected int getSnapshotFormat() {
		if (doSVG)
			return SNAPSHOT_SVG;
		return SNAPSHOT_PNG;
	}
}
