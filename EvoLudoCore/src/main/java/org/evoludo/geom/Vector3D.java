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

package org.evoludo.geom;

/**
 * Utility class for 3D vector manipulations.
 *
 * @author Christoph Hauert
 */
public class Vector3D extends Point3D {

	/**
	 * Create a new 3D vector <code>(0,0,0)</code>.
	 */
	public Vector3D() {
		super();
	}

	/**
	 * Create a new 3D vector from point <code>p</code>.
	 * 
	 * @param p the point/vector to copy
	 */
	public Vector3D(Vector3D p) {
		super(p);
	}

	/**
	 * Create a new 3D vector with coordinates <code>(x,y,z)</code>.
	 * 
	 * @param x the {@code x}-coordinate
	 * @param y the {@code y}-coordinate
	 * @param z the {@code z}-coordinate
	 */
	public Vector3D(double x, double y, double z) {
		super(x, y, z);
	}

	/**
	 * Set the 3D vector pointing from {@code from} to {@code to}.
	 * 
	 * @param from the starting point
	 * @param to   the end point
	 * @return the new vector
	 */
	public Vector3D set(Point3D from, Point3D to) {
		x = to.x - from.x;
		y = to.y - from.y;
		z = to.z - from.z;
		return this;
	}

	/**
	 * Shift the coordinates of this 3D vector by <code>dx</code>, <code>dy</code>
	 * and <code>dz</code> in the {@code x}-, {@code y}-, and {@code z}-coordinates,
	 * respectively.
	 * 
	 * @param dx the shift in the <code>x</code>-coordinate
	 * @param dy the shift in the <code>y</code>-coordinate
	 * @param dz the shift in the <code>z</code>-coordinate
	 * @return the shifted vector <code>(x+dx,y+dy,z+dz)</code>
	 */
	public Vector3D add(double dx, double dy, double dz) {
		x += dx;
		y += dy;
		z += dz;
		return this;
	}

	/**
	 * Add the 3D vector <code>add</code> to this vector.
	 * 
	 * @param add the vector to add
	 * @return this new vector
	 */
	public Vector3D add(Vector3D add) {
		return add(add.x, add.y, add.z);
	}

	/**
	 * Add the 3D vectors <code>a</code> and <code>b</code> and store result in this
	 * vector.
	 * 
	 * @param a the first vector
	 * @param b the second vector
	 * @return the new vector <code>a+b</code>
	 */
	public Vector3D add(Point3D a, Point3D b) {
		x = a.x + b.x;
		y = a.y + b.y;
		z = a.z + b.z;
		return this;
	}

	/**
	 * Subtract <code>dx</code>, <code>dy</code> and <code>dz</code> from the
	 * {@code x}-, {@code y}-, and {@code z}-coordinates, respectively. This yields
	 * the same result as <code>add(-dx, -dy, -dz)</code>.
	 * 
	 * @param dx the shift in the <code>x</code>-coordinate
	 * @param dy the shift in the <code>y</code>-coordinate
	 * @param dz the shift in the <code>z</code>-coordinate
	 * @return this new 3D vector <code>(x-dx,y-dy,z-dz)</code>
	 */
	public Vector3D sub(double dx, double dy, double dz) {
		x -= dx;
		y -= dy;
		z -= dz;
		return this;
	}

	/**
	 * Subtract the 3D vector <code>sub</code> from this vector.
	 * 
	 * @param sub the vector to subtract
	 * @return this new vector
	 */
	public Vector3D sub(Vector3D sub) {
		return sub(sub.x, sub.y, sub.z);
	}

	/**
	 * Subtract 3D vectors <code>b</code> from <code>a</code> and store result in
	 * this 3D vector.
	 * 
	 * @param a the first vector for subtraction
	 * @param b the vector to subtract from <code>a</code>
	 * @return the vector <code>a-b</code>
	 */
	public Vector3D sub(Vector3D a, Vector3D b) {
		x = a.x - b.x;
		y = a.y - b.y;
		z = a.z - b.z;
		return this;
	}

	/**
	 * Calculate the dot product of 3D vector <code>d</code> and this vector.
	 * 
	 * @param d the vector to calculate the dot product with
	 * @return the dot product <code>v.d</code>
	 */
	public double dot(Vector3D d) {
		return x * d.x + y * d.y + z * d.z;
	}

	/**
	 * Calculate the cross product of the 3D vector <code>d</code> and this vector.
	 * 
	 * @param d the vector for cross product
	 * @return the cross product <code>v x d</code>
	 */
	public Vector3D cross(Vector3D d) {
		x = y * d.z - z * d.y;
		y = z * d.x - x * d.z;
		z = x * d.y - y * d.x;
		return this;
	}

	/**
	 * Calculate the cross product of two 3D vectors <code>a</code>, <code>b</code>
	 * and store result in this vector.
	 * 
	 * @param a the first vector
	 * @param b the second vector
	 * @return the cross product <code>v=a x b</code>
	 */
	public Vector3D cross(Vector3D a, Vector3D b) {
		x = a.y * b.z - a.z * b.y;
		y = a.z * b.x - a.x * b.z;
		z = a.x * b.y - a.y * b.x;
		return this;
	}

	/**
	 * Negates the coordinates of 3D vector <code>v</code> to obtain
	 * <code>-v</code>.
	 * 
	 * @return the vector <code>v=-v</code>
	 */
	public Vector3D negate() {
		x = -x;
		y = -y;
		z = -z;
		return this;
	}

	/**
	 * Normalizes the 3D vector <code>v</code> such that its length is
	 * <code>|v|=1</code> while preserving the direction.
	 *
	 * @return the normalized vector
	 * 
	 * @see #length()
	 */
	public Vector3D normalize() {
		scale(1.0 / length());
		return this;
	}

	/**
	 * Normalizes the 3D vector <code>v</code> such that its length is
	 * <code>l</code>, i.e. <code>|v|=l</code>, while preserving the direction.
	 *
	 * @param l the new length of the vector
	 * @return the scaled vector
	 * 
	 * @see #length()
	 */
	public Vector3D normalize(double l) {
		scale(l / length());
		return this;
	}

	/**
	 * Scales the 3D vector <code>v</code> by the scalar factor <code>s</code> and
	 * adds the 3D vector <code>a</code>, <code>v=s*v+a</code>. This is a shortcut
	 * for <code>v.scale(s).add(a)</code>.
	 *
	 * @param s the scalar factor
	 * @param a the vector to add
	 * @return the new vector <code>s*v+a</code>
	 */
	public Vector3D scaleAdd(double s, Vector3D a) {
		x = x * s + a.x;
		y = y * s + a.y;
		z = z * s + a.z;
		return this;
	}

	/**
	 * Calculate the length of the 3D vector. 
	 * <p>
	 * For computational efficiency the fairly expensive square-roots calculations
	 * should be avoided whenever possible.
	 * 
	 * @return the length of vector <code>|v|</code>
	 * 
	 * @see #length2()
	 */
	public double length() {
		return distance();
	}

	/**
	 * Calculate the length squared of the 3D vector, <code>|v|<sup>2</sup></code>.
	 *
	 * @return the length squared
	 * 
	 * @see #length()
	 */
	public double length2() {
		return distance2();
	}

	/**
	 * Same as {@link #length2()}. For (historical) compatibility with
	 * {@code Vector3f} from java3d.
	 * 
	 * @return the length squared
	 * 
	 * @see #length2()
	 */
	public double lengthSquared() {
		return length2();
	}

	@Override
	public String toString() {
		return "[" + x + ", " + y + ", " + z + "]";
	}
}
