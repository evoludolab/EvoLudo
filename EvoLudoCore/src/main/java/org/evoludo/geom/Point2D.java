//
// EvoLudo Project
//
// Copyright 2010-2026 Christoph Hauert
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
 * Class representing a point in 2D Cartesian space.
 * 
 * @author Christoph Hauert
 */
public class Point2D implements Point {

	/**
	 * The {@code x}-coordinate of the point.
	 */
	double x;

	/**
	 * The {@code y}-coordinate of the point.
	 */
	double y;

	/**
	 * Create a new 2D point with coordinates {@code (0,0)}.
	 */
	public Point2D() {
		this(0.0, 0.0);
	}

	/**
	 * Create a copy of the 2D point {@code p}.
	 * 
	 * @param p the 2D point to copy
	 */
	public Point2D(Point2D p) {
		this(p.x, p.y);
	}

	/**
	 * Create a new 2D point with coordinates {@code (x,y)}.
	 * 
	 * @param x the {@code x} coordinate
	 * @param y the {@code y} coordinate
	 */
	public Point2D(double x, double y) {
		this.x = x;
		this.y = y;
	}

	/**
	 * Set {@code x}- and {@code y}-coordinates of the point.
	 * 
	 * @param x the {@code x}-coordinate
	 * @param y the {@code y}-coordinate
	 * @return this point
	 */
	public Point2D set(double x, double y) {
		this.x = x;
		this.y = y;
		return this;
	}

	/**
	 * Set {@code x}- and {@code y}-coordinates of the point to those of the 2D
	 * point {@code p}.
	 * 
	 * @param p the point to copy coordinates from
	 * @return this point
	 */
	public Point2D set(Point2D p) {
		return set(p.x, p.y);
	}

	/**
	 * Compatibility method to cover
	 * {@link java.awt.geom.Point2D#setLocation(double, double)}.
	 * 
	 * @param x the {@code x}-coordinate
	 * @param y the {@code y}-coordinate
	 * 
	 * @see #set(double, double)
	 */
	public void setLocation(double x, double y) {
		set(x, y);
	}

	/**
	 * Compatibility method to cover
	 * {@link java.awt.geom.Point2D#setLocation(java.awt.geom.Point2D)}.
	 * 
	 * @param p the 2D point to copy the position from
	 * 
	 * @see #set(Point2D)
	 */
	public void setLocation(Point2D p) {
		set(p.x, p.y);
	}

	/**
	 * Shift the 2D point by {@code p.x}, and {@code p.y} in the {@code x}-,
	 * {@code y}-coordinates, respectively, to {@code (x+p.x,y+p.y)}.
	 * 
	 * @param p the shift in the {@code (x, y)}-coordinates
	 * @return the shifted point
	 */
	public Point2D shift(Point2D p) {
		return shift(p.x, p.y);
	}

	/**
	 * Shift the 2D point by {@code dx}, and {@code dy} in the {@code x}-,
	 * {@code y}-coordinates, respectively, to {@code (x+dx,y+dy)}.
	 * 
	 * @param dx the shift in the {@code x}-coordinate
	 * @param dy the shift in the {@code y}-coordinate
	 * @return the shifted point
	 */
	public Point2D shift(double dx, double dy) {
		x += dx;
		y += dy;
		return this;
	}

	/**
	 * Scale the coordinates of this 2D point by a factor {@code s} to
	 * {@code (s*x,s*y)}.
	 * 
	 * @param s the scaling factor
	 * @return the scaled point
	 */
	public Point2D scale(double s) {
		return scale(s, s);
	}

	/**
	 * Scale the coordinates of this 2D point by scalar factors {@code sx} and
	 * {@code sy}, respectively, to {@code (sx*x, sy*y)}.
	 *
	 * @param sx the scaling of the {@code x}-coordinate
	 * @param sy the scaling of the {@code y}-coordinate
	 * @return the scaled point
	 */
	public Point2D scale(double sx, double sy) {
		x *= sx;
		y *= sy;
		return this;
	}

	@Override
	public Point2D shake(double quake) {
		x += quake * (Math.random() - 0.5);
		y += quake * (Math.random() - 0.5);
		return this;
	}

	/**
	 * Get the {@code x}-coordinate of this 2D point.
	 * 
	 * @return the {@code x}-coordinate
	 */
	public double getX() {
		return x;
	}

	/**
	 * Set the {@code x}-coordinate of this 2D point.
	 * 
	 * @param x the new {@code x}-coordinate
	 * @return this point
	 */
	public Point2D setX(double x) {
		this.x = x;
		return this;
	}

	/**
	 * Get the {@code y}-coordinate of this 2D point.
	 * 
	 * @return the {@code y}-coordinate
	 */
	public double getY() {
		return y;
	}

	/**
	 * Set the {@code y}-coordinate of this 2D point.
	 * 
	 * @param y the new {@code y}-coordinate
	 * @return this point
	 */
	public Point2D setY(double y) {
		this.y = y;
		return this;
	}

	/**
	 * Calculate the distance from the origin {@code (0,0)}: \(\sqrt{x^2+y^2}\).
	 * <p>
	 * For computational efficiency the fairly expensive square-roots calculations
	 * should be avoided whenever possible.
	 * 
	 * @return the distance
	 * 
	 * @see #distance2()
	 */
	public double distance() {
		return Math.sqrt(distance2());
	}

	/**
	 * Calculate the distance squared from the origin {@code (0,0)}: \(x^2+y^2\).
	 * 
	 * @return the squared distance
	 */
	public double distance2() {
		return x * x + y * y;
	}

	/**
	 * Calculate the distance between the two 2D points {@code p} and {@code q}:
	 * \(\sqrt{(p.x-q.x)^2+(p.y-q.y)^2}\).
	 * <p>
	 * For computational efficiency the fairly expensive square-roots calculations
	 * should be avoided whenever possible.
	 * 
	 * @param q the point to calculate the distance to
	 * @return the distance between the two points
	 * 
	 * @see #distance2(Point2D)
	 */
	public double distance(Point2D q) {
		return Math.sqrt(distance2(q));
	}

	/**
	 * Calculate the distance squared between the two 2D points {@code p} and
	 * {@code q}: \((p.x-q.x)^2+(p.y-q.y)^2\).
	 * 
	 * @param q the point to calculate distance to
	 * @return the distance squared between the two points
	 */
	public double distance2(Point2D q) {
		double dx = q.x - x;
		double dy = q.y - y;
		return dx * dx + dy * dy;
	}

	@Override
	public String toString() {
		return "(" + x + ", " + y + ")";
	}
}
