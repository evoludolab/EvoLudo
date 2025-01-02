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

import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.geom.Line2D;
import java.util.Iterator;

public class S3FrameLayer extends FrameLayer {

	private static final long serialVersionUID = 20110423L;

	private final int[] order;

	public S3FrameLayer(GraphStyle style, int[] order) {
		super(style);
		this.order = order;
	}

	Point e0, e1, e2;

	@Override
	public void init() {
	}

	@Override
	public void init(Rectangle bounds) {
		Point loc = bounds.getLocation();
		bounds.setLocation(0, 0);
		initGraphRect(bounds);

		Polygon simplex = new Polygon();
		e0 = new Point(0, 0);
		S3ToCartesian(1.0, 0.0, 0.0, e0, canvas);
		simplex.addPoint(e0.x, e0.y);
		e1 = new Point(0, 0);
		S3ToCartesian(0.0, 1.0, 0.0, e1, canvas);
		simplex.addPoint(e1.x, e1.y);
		e2 = new Point(0, 0);
		S3ToCartesian(0.0, 0.0, 1.0, e2, canvas);
		simplex.addPoint(e2.x, e2.y);

		outline = simplex;
		frame.reset();
		frame.append(simplex, false);
		formatTicks();
		formatAxisLabels();
		formatGrid();
		bounds.setLocation(loc);
	}

	@Override
	protected void initGraphRect(Rectangle bounds) {
		// determine size of graph/canvas
		int top = MARGIN, bottom = MARGIN, left = MARGIN, right = MARGIN;
		if( labels.size()>=3 ) {
			int margin = style.labelMetrics.getHeight()+style.gapAxisLabel;
			top += margin;
			bottom += margin;
			int maxwidth = -Integer.MAX_VALUE;
			for( Iterator<GraphLabel> i = labels.iterator(); i.hasNext(); ) {
				maxwidth = Math.max(maxwidth, style.labelMetrics.stringWidth(i.next().label));
			}
			maxwidth /= 2;
			left += maxwidth;
			right += maxwidth;
		}
		if( xaxis.majorTicks>=0 ) {
			int margin = style.tickMajorLength+style.tickMetrics.getHeight()+style.gapTickLabel;
			left = Math.max(left, margin);
			right = Math.max(right, margin);
			top += style.tickMetrics.getHeight();
			bottom += margin;
		}
		else if( xaxis.minorTicks>0 ) {
			int margin = style.tickMinorLength;
			left = Math.max(left, margin);
			right = Math.max(right, margin);
			bottom += margin;
		}
		int height = bounds.height-top-bottom;
		int width = Math.min(bounds.width-left-right, (int)(height*1.15470053838));	// 2/sqrt(3) to obtain an equilateral triangle
		int nx = Math.max(1, xaxis.majorTicks+1)*(xaxis.minorTicks+1);
		width -= width%nx;
		left += (bounds.width-width-left-right)/2;
		top += (bounds.height-height-top-bottom)/2;
		canvas.setBounds(bounds.x+left, bounds.y+top, width, height);
	}

	@Override
	protected void formatAxisLabels() {
		axisLabels.clear();
		if( labels.size()>=3 ) {
			// the labels only need proper positioning
			int ylshift = canvas.height+style.gapAxisLabel+style.labelMetrics.getHeight()-style.labelMetrics.getDescent()-1;
			if( xaxis.majorTicks>=0 ) ylshift += style.tickMajorLength+style.gapTickLabel+style.tickMetrics.getHeight();
			else if( xaxis.minorTicks>0 ) ylshift += style.tickMajorLength;
			GraphLabel label = labels.get(order[0]);
			label.setLocation(-style.labelMetrics.stringWidth(label.label)/2, ylshift);
			label = labels.get(order[1]);
			label.setLocation(canvas.width-style.labelMetrics.stringWidth(label.label)/2, ylshift);
			label = labels.get(order[2]);
			label.setLocation((canvas.width-style.labelMetrics.stringWidth(label.label))/2, -style.labelMetrics.getHeight());
		}
	}

	@Override
	protected void formatGrid() {
		grid.reset();
		if( xaxis.grid>0 ) {
			double xfincr = (double)canvas.width/(double)(xaxis.grid+xaxis.grid+2);
			double yfincr = (double)canvas.height/(double)(xaxis.grid+1);
			for( int i=1; i<=xaxis.grid; i++ ) {
				int xi = (int)(i*xfincr);
				int yi = (int)(i*yfincr);
				grid.moveTo(canvas.x+xi+xi, canvas.y+canvas.height);
				grid.lineTo(canvas.x+xi, canvas.y+canvas.height-yi);
				grid.lineTo(canvas.x+canvas.width-xi, canvas.y+canvas.height-yi);
				grid.lineTo(canvas.x+canvas.width-xi-xi, canvas.y+canvas.height);
			}
		}
	}

	@Override
	protected void formatTicks() {
		// format bottom axis
		int nx = Math.max(1, xaxis.majorTicks+1)*(xaxis.minorTicks+1);
		int yshift = canvas.height+canvas.y;
		int xshift = canvas.x;
		int xincr = canvas.width/nx;
		if( xaxis.majorTicks>=0 ) {
			frame.moveTo(xshift, yshift);
			frame.lineTo(xshift, yshift+style.tickMajorLength);
			xshift += xincr;

			for( int i=0; i<=xaxis.majorTicks; i++ ) {
				for( int j=0; j<xaxis.minorTicks; j++ ) {
					frame.moveTo(xshift, yshift);
					frame.lineTo(xshift, yshift+style.tickMinorLength);
					xshift += xincr;
				}
				frame.moveTo(xshift, yshift);
				frame.lineTo(xshift, yshift+style.tickMajorLength);
				xshift += xincr;
			}
		}
		else {
			if( xaxis.minorTicks>0 )
				for( int j=0; j<=(xaxis.minorTicks+1); j++ ) {
					frame.moveTo(xshift, yshift);
					frame.lineTo(xshift, yshift+style.tickMajorLength);
					xshift += xincr;
				}
		}

		// format sides of isoceles triangle
		xshift = canvas.x;
		yshift = canvas.y+canvas.height;
		double len = Math.sqrt(canvas.height*canvas.height+canvas.width*canvas.width/4);
		int ty = (int)(canvas.width/(len+len)*(style.tickMajorLength+1)+0.5);
		int tx = (int)(canvas.height/len*(style.tickMajorLength+1)+0.5);
		int my = (int)(canvas.width/(len+len)*(style.tickMinorLength)+0.5);
		int mx = (int)(canvas.height/len*(style.tickMinorLength)+0.5);
		double xfincr = (double)canvas.width/(double)(nx+nx);
		double yfincr = (double)canvas.height/(double)nx;
		if( xaxis.majorTicks>=0 ) {
			frame.moveTo(xshift, yshift);
			frame.lineTo(xshift-tx, yshift-ty);
			frame.moveTo(canvas.x+canvas.x+canvas.width-xshift, yshift);
			frame.lineTo(canvas.x+canvas.x+canvas.width-xshift+tx, yshift-ty);
			xshift = (int)(canvas.x+xfincr);
			yshift = (int)(canvas.y+canvas.height-yfincr);
			int m = 2;

			for( int i=0; i<=xaxis.majorTicks; i++ ) {
				for( int j=0; j<xaxis.minorTicks; j++ ) {
					frame.moveTo(xshift, yshift);
					frame.lineTo(xshift-mx, yshift-my);
					frame.moveTo(canvas.x+canvas.x+canvas.width-xshift, yshift);
					frame.lineTo(canvas.x+canvas.x+canvas.width-xshift+mx, yshift-my);
					xshift = (int)(canvas.x+m*xfincr);
					yshift = (int)(canvas.y+canvas.height-m*yfincr);
					m++;
				}
				frame.moveTo(xshift, yshift);
				frame.lineTo(xshift-tx, yshift-ty);
				frame.moveTo(canvas.x+canvas.x+canvas.width-xshift, yshift);
				frame.lineTo(canvas.x+canvas.x+canvas.width-xshift+tx, yshift-ty);
				xshift = (int)(canvas.x+m*xfincr);
				yshift = (int)(canvas.y+canvas.height-m*yfincr);
				m++;
			}
		}
		else {
			if( xaxis.minorTicks>0 )
				for( int j=0; j<=(xaxis.minorTicks+1); j++ ) {
					xshift = (int)(canvas.x+j*xfincr);
					yshift = (int)(canvas.y+canvas.height-j*yfincr);
					frame.moveTo(xshift, yshift);
					frame.lineTo(xshift-tx, yshift-ty);
					frame.moveTo(canvas.x+canvas.x+canvas.width-xshift, yshift);
					frame.lineTo(canvas.x+canvas.x+canvas.width-xshift+tx, yshift-ty);
				}
		}
		formatTickLabels();
	}

	@Override
	protected synchronized void formatTickLabels() {
		tickLabels.clear();
		int yshift = canvas.y+canvas.height;
		int ylshift = yshift+style.tickMajorLength+style.gapTickLabel+style.tickMetrics.getHeight();
		int xshift = canvas.x;
		if( xaxis.majorTicks>=0 ) {

			// bottom tick labels
			int xincr = canvas.width/(xaxis.majorTicks+1);
			double xstep = (xaxis.upper-xaxis.lower)/(xaxis.majorTicks+1);
			double xtick = xaxis.lower;
			tickLabels.add(new GraphLabel(xaxis.formatter.format(xtick), xshift, ylshift));
			xshift += xincr;

			for( int i=0; i<=xaxis.majorTicks; i++ ) {
				xtick += xstep;
				String label = xaxis.formatter.format(xtick);
				if( i==xaxis.majorTicks ) tickLabels.add(new GraphLabel(label, xshift-style.tickMetrics.stringWidth(label), ylshift));
				else tickLabels.add(new GraphLabel(label, xshift-2*style.tickMetrics.stringWidth(label)/3, ylshift));
				xshift += xincr;
			}

			// labels along sides of isoceles triangle
			xshift = canvas.x;
			yshift = canvas.y+canvas.height;
			double len = Math.sqrt(canvas.height*canvas.height+canvas.width*canvas.width/4);
			double xfincr = (double)canvas.width/(double)(xaxis.majorTicks+xaxis.majorTicks+2);
			double yfincr = (double)canvas.height/(double)(xaxis.majorTicks+1);
			int ty = (int)(canvas.width/(len+len)*(style.tickMajorLength+2+style.gapTickLabel)+0.5)-style.tickMetrics.getHeight()/2;
			int tx = (int)(canvas.height/len*(style.tickMajorLength+2+style.gapTickLabel)+0.5);
			xtick = xaxis.lower;
			String label = xaxis.formatter.format(xaxis.max);
			tickLabels.add(new GraphLabel(label, xshift-tx-style.tickMetrics.stringWidth(label), yshift-ty));
			tickLabels.add(new GraphLabel(xaxis.formatter.format(xtick), canvas.x+canvas.x+canvas.width-xshift+tx, yshift-ty));
			xshift = (int)(canvas.x+xfincr);
			yshift = (int)(canvas.y+canvas.height-yfincr);
			int m = 2;

			for( int i=0; i<=xaxis.majorTicks; i++ ) {
				xtick += xstep;
				label = xaxis.formatter.format(xaxis.max-(xtick-xaxis.lower));
				tickLabels.add(new GraphLabel(label, xshift-tx-style.tickMetrics.stringWidth(label), yshift-ty));
				tickLabels.add(new GraphLabel(xaxis.formatter.format(xtick), canvas.x+canvas.x+canvas.width-xshift+tx, yshift-ty));
				xshift = (int)(canvas.x+m*xfincr);
				yshift = (int)(canvas.y+canvas.height-m*yfincr);
				m++;
			}
		}
	}

	protected int closestEdge(Point p) {
		double d0 = Line2D.ptSegDistSq(e0.x, e0.y, e1.x, e1.y, p.x, p.y);
		double d1 = Line2D.ptSegDistSq(e1.x, e1.y, e2.x, e2.y, p.x, p.y);
		double d2 = Line2D.ptSegDistSq(e2.x, e2.y, e0.x, e0.y, p.x, p.y);
		if( d0>d1 ) {
			if( d1>d2 ) return 2;
			return 1;
		}
		if( d0>d2 ) return 2;
		return 0;
	}

	private static void S3ToCartesian(double l, double d, double c, Point p, Rectangle canvas) {
		p.x = (int)((d-l+1.0)*0.5*canvas.width+canvas.x);
		p.y = (int)((1.0-c)*canvas.height+canvas.y);
	}
}
