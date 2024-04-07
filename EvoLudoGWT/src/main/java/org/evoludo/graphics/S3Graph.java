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

import org.evoludo.geom.Path2D;
import org.evoludo.geom.Point2D;
import org.evoludo.geom.Segment2D;
import org.evoludo.graphics.AbstractGraph.Shifting;
import org.evoludo.graphics.AbstractGraph.Zooming;
import org.evoludo.simulator.ColorMapCSS;
import org.evoludo.ui.ContextMenu;
import org.evoludo.ui.ContextMenuItem;
import org.evoludo.math.ArrayMath;
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
 *
 * @author Christoph Hauert
 */
public class S3Graph extends AbstractGraph implements Zooming, Shifting, //
	DoubleClickHandler {

	protected int nStates;
	private String[] names;
	private int[] order = new int[] { 0, 1, 2 };
	protected double[] init = new double[4];
	protected Point2D e0 = new Point2D();
	protected Point2D e1 = new Point2D();
	protected Point2D e2 = new Point2D();
	protected Path2D outline = new Path2D();

	public S3Graph(Controller controller, int tag) {
		this(controller, 3, tag);
	}

	public S3Graph(Controller controller, int nStates, int tag) {
		super(controller, tag);
		this.nStates = nStates;
		setStylePrimaryName("evoludo-S3Graph");
	}

	@Override
	public void activate() {
		super.activate();
		doubleClickHandler = addDoubleClickHandler(this);
	}

	@Override
	public void alloc() {
		super.alloc();
		// buffer length is nStates + 1 for time
		if (buffer == null || buffer.capacity() < MIN_BUFFER_SIZE) {
			buffer = new RingBuffer<double[]>(Math.max((int) bounds.getWidth(), DEFAULT_BUFFER_SIZE));
		}
	}

	public boolean setColors(Color[] clrs) {
		boolean changed = false;
		int nColors = clrs.length;
		if( colors==null || colors.length!=nColors ) {
			colors = new String[nColors];
			changed = true;
		}
		for( int n=0; n<nColors; n++ ) {
			String coln = ColorMapCSS.Color2Css(clrs[n]);
			if( coln.equals(colors[n]) )
				continue;
			colors[n] = coln;
			changed = true;
		}
		return changed;
	}

	public boolean setNames(String[] nms) {
		boolean changed = false;
		int nNames = nms.length;
		if( names==null || names.length!=nNames ) {
			names = new String[nNames];
			changed = true;
		}
		for( int n=0; n<nNames; n++ ) {
			// note: names[n] is null after memory allocation
			if( nms[n].equals(names[n]) )
				continue;
			names[n] = nms[n];
			changed = true;
		}
		return changed;
	}

	public void setOrder(int[] order) {
		System.arraycopy(order, 0, this.order, 0, this.order.length);
	}

	@Override
	public void reset() {
		super.reset();
		calcBounds();
		buffer.clear();
		paint();
	}

	public void addData(double t, double[] data, boolean force) {
		if (buffer.isEmpty()) {
			buffer.append(prependTime2Data(t, data));
			System.arraycopy(buffer.last(), 1, init, 1, nStates);
		} else {
			double[] last = buffer.last();
			double lastt = last[0];
			if (Math.abs(t - lastt) < 1e-8) {
				buffer.replace(prependTime2Data(t, data));
				System.arraycopy(last, 1, init, 1, nStates);
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
	public void paint() {
		if (!isActive || !doUpdate())
			return;
		paint(true);
		tooltip.update();
	}

	private void paint(boolean withMarkers) {
		g.save();
		g.scale(scale, scale);
		clearCanvas();
		g.translate(bounds.getX() - viewCorner.x, bounds.getY() - viewCorner.y);
		g.scale(zoomFactor, zoomFactor);

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
			S3ToCartesian(current, currPt);
			while (i.hasNext()) {
				double[] prev = i.next();
				double pt = prev[0];
				S3ToCartesian(prev, prevPt);
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
			S3ToCartesian(init, prevPt);
			fillCircle(prevPt.x, prevPt.y, style.markerSize);
			if (!buffer.isEmpty()) {
				g.setFillStyle(style.endColor);
				S3ToCartesian(buffer.last(), currPt);
				fillCircle(currPt.x, currPt.y, style.markerSize);
			}
		}
		if (markers != null) {
			int n = 0;
			int nMarkers = markers.size();
			for (double[] mark : markers) {
				S3ToCartesian(mark, currPt);
				String mcolor = markerColors[n++ % nMarkers];
				if (mark[0] > 0.0) {
					g.setFillStyle(mcolor);
					fillCircle(currPt.x, currPt.y, style.markerSize);
				}
				else {
					g.setLineWidth(style.lineWidth);
					g.setStrokeStyle(mcolor);
					strokeCircle(currPt.x, currPt.y, style.markerSize);
				}
			}
		}
		g.restore();
	}

	@Override
	public void export(MyContext2d ctx) {
		MyContext2d bak = g;
		g = ctx;
		paint(false);
		g = bak;
	}

	static final double SQRT_2 = 1.41421356237;
	private double bufferThreshold;
	private static double MIN_PIXELS = 3.0;

	@Override
	protected boolean calcBounds() {
		if (!super.calcBounds())
			return false;
		bounds.set(style.minPadding, style.minPadding, width-2*style.minPadding, height-2*style.minPadding);
		String font = g.getFont();
		if( style.showXTicks ) {
			int tlen = style.tickLength;
			int tlen2 = (int)(style.tickLength*0.5);
			bounds.adjust(tlen, tlen2, -tlen-tlen, -tlen-tlen2);
		}
		if( style.showXTickLabels )  {
			setFont(style.ticksLabelFont);
			int tik2 = (int)(g.measureText(Formatter.format((style.xMax-style.xMin)/Math.PI, 2)).getWidth()*0.5);
			bounds.adjust(tik2, 0, -tik2-tik2, -14);
		}
		if( style.showLabel ) {
			setFont(style.labelFont);
			// lower left & right
			int xshift = (int)(Math.max(g.measureText(names[order[0]]).getWidth(), 
					Math.max(g.measureText(names[order[1]]).getWidth(), g.measureText(names[order[2]]).getWidth()))*0.5+0.5);
			int yshift = 20;
			bounds.adjust(xshift, yshift, -xshift-xshift, -yshift-yshift);
		}
		// constrain aspect ratio
		double w = bounds.getWidth();
		double h = bounds.getHeight();
		if( w>h ) {
			double nw = Math.min(w, h*SQRT_2);
			bounds.adjust((w-nw)*0.5, 0, nw-w, 0);
		}
		if( w<h ) {
			double nh = w;
			bounds.adjust(0, (h-nh)*0.5, 0, nh-h);
		}
		g.setFont(font);
		// now that the bounds are known determine outline
		S3ToCartesian(1.0, 0.0, 0.0, e0);
		S3ToCartesian(0.0, 1.0, 0.0, e1);
		S3ToCartesian(0.0, 0.0, 1.0, e2);
		outline.reset();
		outline.moveTo(e0.x, e0.y);
		outline.lineTo(e1.x, e1.y);
		outline.lineTo(e2.x, e2.y);
		outline.closePath();
//		outline.lineTo(e0.x, e0.y);
		// store point only if it is estimated to be at least a few pixels from the previous point
		bufferThreshold = MIN_PIXELS * scale / Math.max(bounds.getWidth(), bounds.getHeight());
		bufferThreshold *= bufferThreshold;
		return true;
	}

	public void drawFrame(int sLevels) {
		if( style.showFrame ) {
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
		if( style.showXLevels ) {
			g.setLineWidth(style.frameWidth);
			g.setStrokeStyle(style.levelColor);
			Point2D start = new Point2D();
			Point2D end = new Point2D();
			double iLevels = 1.0/sLevels;
			double x = iLevels;
			for( int l=1; l<sLevels; l++ ) {
				S3ToCartesian(x, 1.0-x, 0.0, start);
				S3ToCartesian(0.0, 1.0-x, x, end);
				strokeLine(start.x, start.y, end.x, end.y);
				S3ToCartesian(0.0, x, 1.0-x, start);
				S3ToCartesian(x, 0.0, 1.0-x, end);
				strokeLine(start.x, start.y, end.x, end.y);
				S3ToCartesian(1.0-x, 0.0, x, start);
				S3ToCartesian(1.0-x, x, 0.0, end);
				strokeLine(start.x, start.y, end.x, end.y);
				x += iLevels;
			}
		}
		if( style.showXTicks ) {
			g.setStrokeStyle(style.frameColor);
			g.setFillStyle(style.frameColor);
			setFont(style.ticksLabelFont);
			double w = bounds.getWidth();
			double h = bounds.getHeight();
			double len = Math.sqrt(h*h+w*w/4);
			double ty = w/(len+len)*(style.tickLength+1);
			double tx = h/len*(style.tickLength+1);
			double iLevels = 1.0/sLevels;
			double x = 0.0;
			String tick;
			Point2D loc = new Point2D();
			for( int l=0; l<=sLevels; l++ ) {
				if( style.percentX )
					tick = Formatter.formatPercent(1.0-x, 0);
				else
					tick = Formatter.format(1.0-x, 2);
				S3ToCartesian(x, 1.0-x, 0.0, loc);
				strokeLine(loc.x, loc.y, loc.x, loc.y+style.tickLength);
				if( style.showXTickLabels )
					g.fillText(tick, loc.x-g.measureText(tick).getWidth()*0.5, loc.y+style.tickLength+12.5);	// center tick labels with ticks
				S3ToCartesian(0.0, x, 1.0-x, loc);
				strokeLine(loc.x, loc.y, loc.x+tx, loc.y-ty);
				if( style.showXTickLabels )
					g.fillText(tick, loc.x+tx+6, loc.y-ty+3);
				S3ToCartesian(1.0-x, 0.0, x, loc);
				strokeLine(loc.x, loc.y, loc.x-tx, loc.y-ty);
				if( style.showXTickLabels )
					g.fillText(tick, loc.x-tx-(g.measureText(tick).getWidth()+6), loc.y-ty+3);
				x += iLevels;
			}
		}
		if( style.showLabel ) {
			double yshift = 14.5;
			if( style.showXTicks )
				yshift += style.tickLength;
			if( style.showXTickLabels )
				yshift += 12.5;
			setFont(style.labelFont);
			g.setFillStyle(colors[order[0]]);
			Point2D loc = new Point2D();
			S3ToCartesian(1.0, 0.0, 0.0, loc);
			String label = names[order[0]];
			g.fillText(label, loc.x-g.measureText(label).getWidth()*0.5, loc.y+yshift);
			g.setFillStyle(colors[order[1]]);
			S3ToCartesian(0.0, 1.0, 0.0, loc);
			label = names[order[1]];
			g.fillText(label, loc.x-g.measureText(label).getWidth()*0.5, loc.y+yshift);
			g.setFillStyle(colors[order[2]]);
			S3ToCartesian(0.0, 0.0, 1.0, loc);
			label = names[order[2]];
			g.fillText(label, loc.x-g.measureText(label).getWidth()*0.5, loc.y-14.5);
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
		if( Duration.currentTimeMillis()-touchEndTime>250.0 || touches.length()>1 )
			// single tap or multiple touches
			return;

		Touch touch = touches.get(0);
		processInitXY(touch.getRelativeX(getElement()), touch.getRelativeY(getElement()));
		event.preventDefault();
	}

	private void processInitXY(int x, int y) {
		double[] s3 = new double[3];
		cartesianToS3(x, y, s3);
		controller.setInitialState(s3);
	}

	protected double[] tip = new double[3];

	// tool tips
	@Override
	public String getTooltipAt(int x, int y) {
		// convert to user coordinates
		double sx = (viewCorner.x + x - bounds.getX()) / zoomFactor + 0.5;
		double sy = (viewCorner.y + y - bounds.getY()) / zoomFactor + 0.5;
		if( !inside(sx, sy) )
			return null;
		cartesianToS3(sx, sy, tip);
		String msg = "<table>";
		for( int i=0; i<3; i++ )
			msg += "<tr><td style='text-align:right'><i>" + names[i] + ":</i></td><td>" //
					+ Formatter.formatPercent(tip[i], 2)+"</td></tr>";
		return msg+"</table>";
	}

	/**
	 * Check if point (in user coordinates) lies inside of simplex.
	 * 
	 * @param x <code>x</code>-coordinate of point
	 * @param y <code>y</code>-coordinate of point
	 * @return <code>true</code> if inside
	 */
	private boolean inside(double x, double y) {
		Point2D p = new Point2D(x, y);
		boolean inside = (Segment2D.orientation(e0, e1, p) > 0);
		if (!inside)
			return false;
		inside &= (Segment2D.orientation(e1, e2, p) > 0);
		if (!inside)
			return false;
		return inside && (Segment2D.orientation(e2, e0, p) > 0);
	}

	private ContextMenuItem swapOrderMenu, clearMenu;
	private ContextMenu setTraitMenu;
	private int cornerIdx= -1;

	@Override
	public void populateContextMenuAt(ContextMenu menu, int x, int y) {
		// add menu to clear canvas
		if( clearMenu==null ) {
			clearMenu = new ContextMenuItem("Clear", new Command() {
				@Override
				public void execute() {
					buffer.clear();
					paint();
				}
			});
		}
		menu.addSeparator();
		menu.add(clearMenu);

		// process swap order context menu
		if( swapOrderMenu==null ) {
			swapOrderMenu = new ContextMenuItem("Swap Order", new Command() {
				@Override
				public void execute() {
					int swap;
					String label = swapOrderMenu.getText();
					if( label.startsWith("Swap "+names[order[0]]) ) {
						swap = order[0];
						order[0] = order[1];
						order[1] = swap;
					}
					else if( label.startsWith("Swap "+names[order[1]]) ) {
						swap = order[1];
						order[1] = order[2];
						order[2] = swap;
					}
					else {
						swap = order[2];
						order[2] = order[0];
						order[0] = swap;
					}
					paint();
				}
			});
		}
		menu.add(swapOrderMenu);
		switch( closestEdge(x, y) ) {
			case EDGE_LEFT:
				swapOrderMenu.setText("Swap "+names[order[0]]+" \u2194 "+names[order[1]]);
				break;
			case EDGE_RIGHT:
				swapOrderMenu.setText("Swap "+names[order[1]]+" \u2194 "+names[order[2]]);
				break;
			// case EDGE_BOTTOM:
			default:
				swapOrderMenu.setText("Swap "+names[order[2]]+" \u2194 "+names[order[0]]);
				break;
		}

		// process set strategy context menu
		if (nStates > 3) {
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
							paint();
						}
					}));
			}
			menu.add("Set trait...", setTraitMenu);
			cornerIdx = closestCorner(x, y);
			// disable already visible traits
			for (int t : order)
				((ContextMenuItem) setTraitMenu.getWidget(t)).setEnabled(false);
		}

		super.populateContextMenuAt(menu, x, y);
	}

	// COORDINATE CONVERSION UTILITIES

	/**
	 * Convert coordinates in simplex \(S_3\) to cartesian coordinates.
	 * <p>
	 * <strong>Note:</strong> The first entry {@code s3[0]} refers to the time!
	 * 
	 * @param s3 the array with the simplex coordinates
	 * @param p  the point in cartesian coordinates
	 * @return the point in cartesian coordinates {@code p}
	 */
	private Point2D S3ToCartesian(double[] s3, Point2D p) {
		// NOTE: s3[0] is time!
		// c: s3[3], d: s3[2], l: s3[1]
		return S3ToCartesian(s3[order[0] + 1], s3[order[1] + 1], s3[order[2] + 1], p);
	}

	/**
	 * Convert coordinates in simplex \(S_3\) to cartesian coordinates.
	 * <p>
	 * <strong>Note:</strong> In order to deal with projections onto \(S_3\)
	 * subspaces the coordinates {@code s1}, {@code s2}, {@code s3}, do not need to
	 * sum up to {@code 1.0}.
	 * 
	 * @param s1 the value of the first trait (lower left corner by default)
	 * @param s2 the value of the second trait (lower right corner by default)
	 * @param s3 the value of the third trait (top corner by default)
	 * @param p  the point in cartesian coordinates
	 * @return the point in cartesian coordinates {@code p}
	 */
	private Point2D S3ToCartesian(double s1, double s2, double s3, Point2D p) {
		// c: s3, d: s2, l: s1
		p.x = s2 - s1;	// [-1, 1]
		p.y = (s3 - s2 - s1 + 1.0) * 0.5 - 1.0 / 3.0;	// [-1/3, 2/3]
		p.scale(s1 + s2 + s3);
		p.x = (p.x + 1.0) * 0.5 * bounds.getWidth();
		p.y = (2.0 / 3.0 - p.y) * bounds.getHeight();
		return p;
	}

	// make sure coordinates are indeed in S3! - if (x, y) is outside return closest point
	private double[] cartesianToS3(int x, int y, double[] s) {
		// convert to user coordinates
		return cartesianToS3((viewCorner.x + x - bounds.getX()) / zoomFactor + 0.5, //
							 (viewCorner.y + y - bounds.getY()) / zoomFactor + 0.5, s);
	}

	private double[] cartesianToS3(double x, double y, double[] s) {
		double s2 = 1.0 - y / (bounds.getHeight() - 1);
		double s1 = x / (bounds.getWidth() - 1) - s2 * 0.5;
		s[order[2]] = Math.max(0.0, s2);
		s[order[1]] = Math.max(0.0, s1);
		s[order[0]] = Math.max(0.0, 1.0 - s1 - s2);
		ArrayMath.normalize(s);
		return s;
	}

	protected final int CORNER_LEFT = 0;
	protected final int CORNER_RIGHT = 1;
	protected final int CORNER_TOP = 2;

	protected int closestCorner(double x, double y) {
		Point2D p = new Point2D(x - bounds.getX(), y - bounds.getY());
		double d0 = p.distance2(e0);
		double d1 = p.distance2(e1);
		double d2 = p.distance2(e2);
		if (d0 < d1) {
			if (d0 < d2)
				return CORNER_LEFT;
			return CORNER_TOP;
		}
		if (d1 < d2)
			return CORNER_RIGHT;
		return CORNER_TOP;
	}

	protected final int EDGE_LEFT = 0;
	protected final int EDGE_RIGHT = 1;
	protected final int EDGE_BOTTOM = 2;

	protected int closestEdge(double x, double y) {
		Point2D p = new Point2D(x - bounds.getX(), y - bounds.getY());
		double d0 = Segment2D.distance2(e0, e1, p);
		double d1 = Segment2D.distance2(e1, e2, p);
		double d2 = Segment2D.distance2(e2, e0, p);
		if (d0 > d1) {
			if (d1 > d2)
				return 2;
			return 1;
		}
		if (d0 > d2)
			return 2;
		return 0;
	}
}
