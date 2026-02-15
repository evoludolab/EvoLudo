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
 * Class representing a point in 3D Cartesian space.
 * 
 * @author Christoph Hauert
 */
public class Point3D implements Point {

	/**
	 * The {@code x}-coordinate of the point.
	 */
	double x;

	/**
	 * The {@code y}-coordinate of the point.
	 */
	double y;

	/**
	 * The {@code z}-coordinate of the point.
	 */
	double z;

	/**
	 * Create a new 3D point with coordinates {@code (0,0,0)}.
	 */
	public Point3D() {
		this(0.0, 0.0, 0.0);
	}

	/**
	 * Create a copy of the 3D point {@code p}.
	 * 
	 * @param p the 3D point to copy
	 */
	public Point3D(Point3D p) {
		this(p.x, p.y, p.z);
	}

	/**
	 * Create a new 3D point with coordinates {@code (x,y,z)}.
	 * 
	 * @param x the {@code x}-coordinate
	 * @param y the {@code y}-coordinate
	 * @param z the {@code z}-coordinate
	 */
	public Point3D(double x, double y, double z) {
		this.x = x;
		this.y = y;
		this.z = z;
	}

	/**
	 * Set {@code x}-, {@code y}- and {@code z}-coordinates of the 3D point.
	 * 
	 * @param x the {@code x}-coordinate
	 * @param y the {@code y}-coordinate
	 * @param z the {@code z}-coordinate
	 * @return this point
	 */
	public Point3D set(double x, double y, double z) {
		this.x = x;
		this.y = y;
		this.z = z;
		return this;
	}

	/**
	 * Set {@code x}-, {@code y}- and {@code z}-coordinates of the point to those of
	 * the 3D point {@code p}.
	 * 
	 * @param p the 3D point to copy coordinates from
	 * @return this point
	 */
	public Point3D set(Point3D p) {
		return set(p.x, p.y, p.z);
	}

	/**
	 * Compatibility method (following {@link Point2D#setLocation(double, double)}).
	 * 
	 * @param x the {@code x}-coordinate
	 * @param y the {@code y}-coordinate
	 * @param z the {@code z}-coordinate
	 */
	public void setLocation(double x, double y, double z) {
		set(x, y, z);
	}

	/**
	 * Compatibility method (following {@link Point2D#setLocation(Point2D p)}).
	 * 
	 * @param p the 3D point to copy the position from
	 */
	public void setLocation(Point3D p) {
		set(p.x, p.y, p.z);
	}

	/**
	 * Shift the 3D point by {@code p.x}, {@code p.y}, and {@code p.z} in the
	 * {@code x}-, {@code y}-, and {@code z}-coordinates, respectively, to
	 * {@code (x+p.x,y+p.y,z+p.z)}.
	 * 
	 * @param p the shift in the {@code (x, y, z)}-coordinates
	 * @return the shifted point
	 */
	public Point3D shift(Point3D p) {
		return shift(p.x, p.y, p.z);
	}

	/**
	 * Shift the 3D point by {@code dx}, {@code dy}, and {@code dz} in the
	 * {@code x}-, {@code y}-, and {@code z}-coordinates, respectively, to
	 * {@code (x+p.x,y+p.y,z+p.z)}.
	 * 
	 * @param dx the shift in the {@code x}-coordinate
	 * @param dy the shift in the {@code y}-coordinate
	 * @param dz the shift in the {@code z}-coordinate
	 * @return the shifted point
	 */
	public Point3D shift(double dx, double dy, double dz) {
		x += dx;
		y += dy;
		z += dz;
		return this;
	}

	/**
	 * Scale the coordinates of this 3D point by a factor {@code s} to
	 * {@code (s*x,s*y,s*z)}.
	 * 
	 * @param s the scaling factor
	 * @return the scaled point
	 */
	public Point3D scale(double s) {
		return scale(s, s, s);
	}

	/**
	 * Scale the coordinates of this 3D point by scalar factors {@code sx},
	 * {@code sy} and {@code sz}, respectively, to {@code (sx*x, sy*y, sz*z)}.
	 *
	 * @param sx the scaling of the {@code x}-coordinate
	 * @param sy the scaling of the {@code y}-coordinate
	 * @param sz the scaling of the {@code z}-coordinate
	 * @return the scaled point
	 */
	public Point3D scale(double sx, double sy, double sz) {
		x *= sx;
		y *= sy;
		z *= sz;
		return this;
	}

	@Override
	public Point3D shake(double quake) {
		x += quake * (Math.random() - 0.5);
		y += quake * (Math.random() - 0.5);
		z += quake * (Math.random() - 0.5);
		return this;
	}

	/**
	 * Get the {@code x}-coordinate of this 3D point.
	 * 
	 * @return the {@code x}-coordinate
	 */
	public double getX() {
		return x;
	}

	/**
	 * Set the {@code x}-coordinate of this 3D point.
	 * 
	 * @param x the new {@code x}-coordinate
	 * @return this point
	 */
	public Point3D setX(double x) {
		this.x = x;
		return this;
	}

	/**
	 * Get the {@code y}-coordinate of this 3D point.
	 * 
	 * @return the {@code y}-coordinate
	 */
	public double getY() {
		return y;
	}

	/**
	 * Set the {@code y}-coordinate of this 3D point.
	 * 
	 * @param y the new {@code y}-coordinate
	 * @return this point
	 */
	public Point3D setY(double y) {
		this.y = y;
		return this;
	}

	/**
	 * Get the {@code z}-coordinate of this 3D point.
	 * 
	 * @return the {@code z}-coordinate
	 */
	public double getZ() {
		return z;
	}

	/**
	 * Set the {@code z}-coordinate of this 3D point.
	 * 
	 * @param z the new {@code z}-coordinate
	 * @return this point
	 */
	public Point3D setZ(double z) {
		this.z = z;
		return this;
	}

	/**
	 * Calculate distance from the origin {@code (0,0,0)}: \(\sqrt{x^2+y^2+z^2}\).
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
	 * Calculate the distance squared from the origin {@code (0,0,0)}:
	 * \(x^2+y^2+z^2\).
	 * 
	 * @return the squared distance
	 */
	public double distance2() {
		return x * x + y * y + z * z;
	}

	/**
	 * Calculate the distance between the two 3D points {@code p} and {@code q}:
	 * \(\sqrt{(p.x-q.x)^2+(p.y-q.y)^2+(p.z-q.z)^2}\).
	 * <p>
	 * For computational efficiency the fairly expensive square-roots calculations
	 * should be avoided whenever possible.
	 * 
	 * @param q the point to calculate distance to
	 * @return the distance between the two points
	 * 
	 * @see #distance2(Point3D)
	 */
	public double distance(Point3D q) {
		return Math.sqrt(distance2(q));
	}

	/**
	 * Calculate the distance squared between the two 3D points {@code p} and
	 * {@code q}: \((p.x-q.x)^2+(p.y-q.y)^2+(p.z-q.z)^2\).
	 * 
	 * @param q the point to calculate distance to
	 * @return the distance squared between the two points
	 */
	public double distance2(Point3D q) {
		double dx = q.x - x;
		double dy = q.y - y;
		double dz = q.z - z;
		return dx * dx + dy * dy + dz * dz;
	}

	@Override
	public String toString() {
		return "(" + x + ", " + y + ", " + z + ")";
	}
}
