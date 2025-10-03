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

package org.evoludo.geom;

/**
 * Utility class for deal with (finite) line segments in 2D.
 * 
 * Note: this implementation is intended as a drop-in replacement for
 * java.awt.geom.Line2D.Double. Methods are implemented only on an as-needed
 * basis.
 * 
 * @author Christoph Hauert
 */
public class Segment2D extends Line2D {

	/**
	 * The first point of the line segment.
	 */
	Point2D p1;

	/**
	 * The second point of the line segment.
	 */
	Point2D p2;

	/**
	 * Create a new empty 2D line with coordinates <code>(0,0)</code> and zero
	 * length.
	 */
	public Segment2D() {
		this(0.0, 0.0, 0.0, 0.0);
	}

	/**
	 * Create a copy of the 2D line <code>l</code>.
	 * 
	 * @param l the 2D line to copy
	 */
	public Segment2D(Segment2D l) {
		this(l.p1, l.p2);
	}

	/**
	 * Create a new 2D line from point <code>p1</code> to point <code>p2</code>.
	 * 
	 * @param p1 the starting point
	 * @param p2 the end point
	 */
	public Segment2D(Point2D p1, Point2D p2) {
		this(p1.x, p1.y, p2.x, p2.y);
	}

	/**
	 * Create a new 2D line from point <code>(x1,y1)</code> to point
	 * <code>(x2,y2)</code>.
	 * 
	 * @param x1 the <code>x</code>-coordinate of starting point
	 * @param y1 the <code>y</code>-coordinate of starting point
	 * @param x2 the <code>x</code>-coordinate of end point
	 * @param y2 the <code>y</code>-coordinate of end point
	 */
	public Segment2D(double x1, double y1, double x2, double y2) {
		this.p1 = new Point2D();
		this.p2 = new Point2D();
		set(x1, y1, x2, y2);
	}

	/**
	 * Set the line segment from point <code>p1</code> to point <code>p2</code>.
	 * 
	 * @param p1 the first point
	 * @param p2 the second point
	 */
	@Override
	public void set(Point2D p1, Point2D p2) {
		super.set(p1, p2);
		this.p1.set(p1);
		this.p2.set(p2);
	}

	/**
	 * Set the line segment from point <code>(x1,y1)</code> to point
	 * <code>(x2,y2)</code>.
	 * 
	 * @param x1 the <code>x</code>-coordinate of starting point
	 * @param y1 the <code>y</code>-coordinate of starting point
	 * @param x2 the <code>x</code>-coordinate of end point
	 * @param y2 the <code>y</code>-coordinate of end point
	 */
	@Override
	public void set(double x1, double y1, double x2, double y2) {
		super.set(x1, y1, x2, y2);
		p1.set(x1, y1);
		p2.set(x2, y2);
	}

	@Override
	public Segment2D shift(double dx, double dy) {
		p1.shift(dx, dy);
		p2.shift(dx, dy);
		return this;
	}

	@Override
	public double distance(Point2D p) {
		return Math.sqrt(distance2(p));
	}

	@Override
	public double distance2(Point2D p) {
		return distance2(this, p);
	}

	/**
	 * Find the square distance of the point <code>p</code> from the segment
	 * <code>s</code>.
	 * 
	 * @param s the segment
	 * @param p the point
	 * @return the distance of the point from the segment
	 */
	public static double distance2(Segment2D s, Point2D p) {
		return distance2(s.p1, s.p2, p);
	}

	/**
	 * Find the square distance of the point <code>p</code> from the segment,
	 * specified by the two points <code>s1</code> and <code>s2</code>.
	 * 
	 * @param p1 the start of the line segment
	 * @param p2 the end of the line segment
	 * @param p  the point
	 * @return the distance of point from segment
	 */
	public static double distance2(Point2D p1, Point2D p2, Point2D p) {
		// Adjust vectors relative to first point of segment s
		// x2,y2 becomes relative vector of segment
		double x2 = p2.x - p1.x;
		double y2 = p2.y - p1.y;
		// px,py becomes vector relative to s.p1
		double px = p.x - p1.x;
		double py = p.y - p1.y;
		double dot = px * x2 + py * y2;
		double proj2 = 0.0;
		if (dot > 0.0) {
			// switch to backwards vectors relative to x2,y2
			// x2,y2 are already the negative of x1,y1=>x2,y2
			// to get px,py to be the negative of px,py=>x2,y2
			// the dot product of two negated vectors is the same
			// as the dot product of the two normal vectors
			px = x2 - px;
			py = y2 - py;
			dot = px * x2 + py * y2;
			if (dot <= 0.0) {
				// px,py is on the side of x2,y2 away from x1,y1
				// distance to segment is length of (backwards) px,py vector
				// "length of its (clipped) projection" is now 0.0
				proj2 = 0.0;
			} else {
				// px,py is between x1,y1 and x2,y2
				// dotprod is the length of the px,py vector
				// projected on the x2,y2=>x1,y1 vector times the
				// length of the x2,y2=>x1,y1 vector
				proj2 = dot * dot / (x2 * x2 + y2 * y2);
			}
		}
		// Distance to line is now the length of the relative point
		// vector minus the length of its projection onto the line
		// (which is zero if the projection falls outside the range
		// of the line segment).
		double len2 = px * px + py * py - proj2;
		if (len2 < 0) {
			len2 = 0;
		}
		return len2;
	}

	@Override
	public boolean intersects(Line2D l) {
		return l.intersects(this);
	}

	/**
	 * Check if this segment and the segment <code>s</code> intersect.
	 * <p>
	 * Note: code inspired by
	 * https://www.geeksforgeeks.org/check-if-two-given-line-segments-intersect/
	 * 
	 * @param s the other line segment
	 * @return <code>true</code> if the line segments intersect
	 */
	@Override
	public boolean intersects(Segment2D s) {
		// four orientations needed for general and special cases
		int o1 = orientation(p1, p2, s.p1);
		int o2 = orientation(p1, p2, s.p2);
		int o3 = orientation(s.p1, s.p2, p1);
		int o4 = orientation(s.p1, s.p2, p2);

		// return true if any of the intersection conditions are met
		return (o1 != o2 && o3 != o4)
				|| (o1 == 0 && contains(p1, s.p1, p2))
				|| (o2 == 0 && contains(p1, s.p2, p2))
				|| (o3 == 0 && contains(s.p1, p1, s.p2))
				|| (o4 == 0 && contains(s.p1, p2, s.p2));
	}

	/**
	 * Find the orientation of the ordered triplet {@code (p, q, r)}.
	 * <p>
	 * Return value denotes orientation:
	 * <ul>
	 * <li>0: <code>p, q</code> and <code>r</code> are collinear.
	 * <li>1: clockwise
	 * <li>-1: anti-clockwise
	 * </ul>
	 * 
	 * @param p the first point
	 * @param q the second point
	 * @param r the third point
	 * @return the orientation
	 */
	public static int orientation(Point2D p, Point2D q, Point2D r) {
		// see https://www.geeksforgeeks.org/orientation-3-ordered-points/ for details.
		double val = (q.y - p.y) * (r.x - q.x) - (q.x - p.x) * (r.y - q.y);

		if (val == 0.0)
			return 0; // collinear

		return (val > 0) ? 1 : -1; // clockwise or anti-clockwise
	}

	/**
	 * For the three collinear points <code>p,q,x</code>, check if the point
	 * <code>x</code> lies on the line segment <code>p-q</code>.
	 * 
	 * @param p the first point
	 * @param x the second point
	 * @param q the third point
	 * @return <code>true</code> if <code>q</code> lies on segment
	 */
	private static boolean contains(Point2D p, Point2D q, Point2D x) {
		return (x.x <= Math.max(p.x, q.x) && x.x >= Math.min(p.x, q.x) && x.y <= Math.max(p.y, q.y)
				&& x.y >= Math.min(p.y, q.y));
	}

	@Override
	public String toString() {
		return "[start=" + p1 + ", end=" + p2 + "]";
	}
}
