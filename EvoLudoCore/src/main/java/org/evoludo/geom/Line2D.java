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
 * Utility class for deal with straight (infinite) lines in 2D. <br>
 * Note: the drop-in replacement for java.awt.geom.Line2D.Double is not Line2D
 * but rather Segment2D for straight lines of finite length.
 * 
 * @author Christoph Hauert
 */
public class Line2D {

	/**
	 * The slope of the straight line.
	 * <p>
	 * Note: for vertical lines slope is {@code Double.POSITIVE_INFINITY} or
	 * {@code Double.POSITIVE_INFINITY}.
	 */
	double m;

	/**
	 * The <code>y</code>-intercept of the straight line.
	 * <p>
	 * Note: for vertical lines this is the <code>x</code>-intercept.
	 */
	double b;

	/**
	 * Create a new 2D line with the slope and <code>y</code>-intercept of zero
	 * (corresponding to the <code>x</code>-axis).
	 */
	public Line2D() {
		initialize(this, 0, 0);
	}

	/**
	 * Create a copy of the 2D line <code>l</code>.
	 * 
	 * @param l the 2D line to copy
	 */
	public Line2D(Line2D l) {
		initialize(this, l.m, l.b);
	}

	/**
	 * Create a new 2D line with slope <code>m</code> and <code>y</code>-intercept
	 * <code>b</code>.
	 * 
	 * @param m the slope
	 * @param b the <code>y</code>-intercept
	 */
	public Line2D(double m, double b) {
		initialize(this, m, b);
	}

	/**
	 * Create a new 2D line through points <code>p1</code> and <code>p2</code>.
	 * 
	 * @param p1 the first point
	 * @param p2 the second point
	 */
	public Line2D(Point2D p1, Point2D p2) {
		initialize(this, getSlope(p1, p2), getYIntercept(p1, p2));
	}

	/**
	 * Initialize the line with the given slope and y-intercept. Helper method to
	 * prevent {@code this-escape} warnings.
	 * 
	 * @param line the line to initialize
	 * @param m    the slope
	 * @param b    the y-intercept
	 */
	protected static void initialize(Line2D line, double m, double b) {
		line.m = m;
		line.b = b;
	}

	/**
	 * Calculate the slope of a line running through points {@code p1} and
	 * {@code p2}.
	 * 
	 * @param p1 the first point
	 * @param p2 the second point
	 * @return the slope of the line
	 */
	public static double getSlope(Point2D p1, Point2D p2) {
		return getSlope(p1.x, p1.y, p2.x, p2.y);
	}

	/**
	 * Calculate the slope of a line running through points {@code (x1,y1)} and
	 * {@code (x2,y2)}.
	 * 
	 * @param x1 the <code>x</code>-coordinate of first point
	 * @param y1 the <code>y</code>-coordinate of first point
	 * @param x2 the <code>x</code>-coordinate of second point
	 * @param y2 the <code>y</code>-coordinate of second point
	 * @return the slope of the line
	 */
	public static double getSlope(double x1, double y1, double x2, double y2) {
		if (x1 == x2)
			// vertical line
			return (y2 > y1 ? Double.POSITIVE_INFINITY : Double.NEGATIVE_INFINITY);
		return (y2 - y1) / (x2 - x1);
	}

	/**
	 * Calculate the slope of a line running through points {@code p1} and
	 * {@code p2}.
	 * 
	 * @param p1 the first point
	 * @param p2 the second point
	 * @return the {@code y}-intercept of the line
	 */
	public static double getYIntercept(Point2D p1, Point2D p2) {
		return getYIntercept(p1.x, p1.y, p2.x, p2.y);
	}

	/**
	 * Calculate the slope of a line running through points {@code p1} and
	 * {@code p2}.
	 * 
	 * @param x1 the <code>x</code>-coordinate of first point
	 * @param y1 the <code>y</code>-coordinate of first point
	 * @param x2 the <code>x</code>-coordinate of second point
	 * @param y2 the <code>y</code>-coordinate of second point
	 * @return the {@code y}-intercept of the line
	 */
	public static double getYIntercept(double x1, double y1, double x2, double y2) {
		double slope = getSlope(x1, y1, x2, y2);
		if (!Double.isFinite(slope))
			return Double.NaN;
		return y1 - slope * x1;
	}

	/**
	 * Set the line to pass through point <code>p</code> in the direction of vector
	 * <code>v</code>.
	 * 
	 * @param point     the point on the line
	 * @param direction the direction of the line
	 */
	public void set(Point2D point, Vector2D direction) {
		set(point.x, point.y, point.x + direction.x, point.y + direction.y);
	}

	/**
	 * Set the slope <code>m</code> and <code>y</code>-intercept <code>b</code> of
	 * the line.
	 * 
	 * @param m the slope
	 * @param b the <code>y</code>-intercept
	 */
	public void set(double m, double b) {
		initialize(this, m, b);
	}

	/**
	 * Set the line to pass through points <code>p1</code> and <code>p2</code>.
	 * 
	 * @param p1 the first point on the line
	 * @param p2 the second point on the line
	 */
	public void set(Point2D p1, Point2D p2) {
		set(p1.x, p1.y, p2.x, p2.y);
	}

	/**
	 * Set the line to pass through the points <code>p1</code> and <code>p2</code>.
	 * <p>
	 * Note: for compatibility with {@code java.awt.geom.Line2D.Double}
	 * 
	 * @param p1 the first point on the line
	 * @param p2 the second point on the line
	 */
	public void setLine(Point2D p1, Point2D p2) {
		set(p1.x, p1.y, p2.x, p2.y);
	}

	/**
	 * Set the slope {@code m} and y-intercept {@code b} of the line passing through
	 * points <code>(x1,y1)</code> and <code>(x2,y2)</code>.
	 * 
	 * @param x1 the <code>x</code>-coordinate of first point
	 * @param y1 the <code>y</code>-coordinate of first point
	 * @param x2 the <code>x</code>-coordinate of second point
	 * @param y2 the <code>y</code>-coordinate of second point
	 */
	public void set(double x1, double y1, double x2, double y2) {
		m = getSlope(x1, y1, x2, y2);
		b = getYIntercept(x1, y1, x2, y2);
	}

	/**
	 * Shift the line right by <code>dx</code> and up by <code>dy</code>.
	 * 
	 * @param dx the horizontal shift
	 * @param dy the vertical shift
	 * @return the new line
	 */
	public Line2D shift(double dx, double dy) {
		b += m * dx + dy;
		return this;
	}

	/**
	 * Return the {@code y}-value, <code>y = m*x+b</code>, for given the
	 * <code>x</code>-value.
	 * 
	 * @param x the <code>x</code>-value
	 * @return the <code>y</code>-value
	 */
	public double y(double x) {
		return m * x + b;
	}

	/**
	 * Check if point <code>p</code> lies above or below the line. Above means the
	 * <code>y</code>-coordinate of the point exceeds that of the line at the
	 * corresponding <code>x</code>-coordinate.
	 * 
	 * @param p the point to check
	 * @return <code>true</code> if points lies above line.
	 */
	public boolean above(Point2D p) {
		return above(p.x, p.y);
	}

	/**
	 * Check if point <code>(x,y)</code> lies above or below the line. Above means
	 * the <code>y</code>-coordinate of the point exceeds that of the line at the
	 * corresponding <code>x</code>-coordinate.
	 * 
	 * @param x the <code>x</code>-coordinate of point
	 * @param y the <code>y</code>-coordinate of point
	 * @return <code>true</code> if points lies above line.
	 */
	public boolean above(double x, double y) {
		return (y > y(x));
	}

	/**
	 * Calculate the distance between point <code>p</code> and the line.
	 * 
	 * @param p the point to find distance from line
	 * @return the distance
	 */
	public double distance(Point2D p) {
		return Math.sqrt(distance2(p));
	}

	/**
	 * Calculate the squared distance between point <code>p</code> and the line.
	 * 
	 * @param p the point to find squared distance from line
	 * @return the squared distance
	 */
	public double distance2(Point2D p) {
		double x = -m * p.x + p.y - b;
		return x * x / (m * m + 1);
	}

	/**
	 * Calculate the squared distance between point <code>p</code> and the line
	 * <code>l</code>.
	 * 
	 * @param l the line to find distance from
	 * @param p the point to find distance to line
	 * @return the squared distance
	 */
	public static double distance2(Line2D l, Point2D p) {
		double x = -l.m * p.x + p.y - l.b;
		return x * x / (l.m * l.m + 1);
	}

	/**
	 * Check if two lines are parallel.
	 * <p>
	 * Note: {@code Double.compare} returns zero for two <code>NaN</code>'s, i.e.
	 * for two vertical lines and hence works as expected.
	 * 
	 * @param l the line to compare to
	 * @return <code>true</code> if the lines are parallel
	 */
	public boolean parallel(Line2D l) {
		return (Double.compare(m, l.m) == 0);
	}

	/**
	 * Check if the line is vertical.
	 * 
	 * @return <code>true</code> if the line is vertical
	 */
	public boolean vertical() {
		return (Double.isInfinite(m));
	}

	/**
	 * Check if the line intersects the line <code>l</code>.
	 * 
	 * @param l the line to check
	 * @return <code>true</code> if the lines intersect
	 */
	public boolean intersects(Line2D l) {
		return !parallel(l);
	}

	/**
	 * Check if the line intersects the line segment <code>s</code>.
	 * 
	 * @param s the line segment to check
	 * @return <code>true</code> if the line segment intersects
	 */
	public boolean intersects(Segment2D s) {
		return (above(s.p1) != above(s.p2));
	}

	/**
	 * Check if the line intersects the rectangle <code>r</code>.
	 * 
	 * @param r the rectangle to check
	 * @return <code>true</code> if the line intersects the rectangle
	 */
	public boolean intersects(Rectangle2D r) {
		return r.intersects(this);
	}

	/**
	 * Return the intersection of two lines or {@code Double.NaN} if no
	 * intersection.
	 * 
	 * @param l the line to check
	 * @return the intersection point
	 */
	public Point2D intersection(Line2D l) {
		if (!intersects(l))
			return new Point2D(Double.NaN, Double.NaN);
		double x = (l.b - b) / (m - l.m);
		return new Point2D(x, y(x));
	}

	/**
	 * Return the intersection of the line and the segment <code>s</code> or
	 * {@code Double.NaN} if there is no intersection.
	 * 
	 * @param s the line segment to check
	 * @return the intersection point.
	 */
	public Point2D intersection(Segment2D s) {
		if (!intersects(s))
			return new Point2D(Double.NaN, Double.NaN);
		double x = (s.b - b) / (m - s.m);
		return new Point2D(x, y(x));
	}

	@Override
	public String toString() {
		return "[slope=" + m + ", y-intercept=" + b + "]";
	}
}
