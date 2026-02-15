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
 * Utility class for 2D vector manipulations.
 * 
 * @author Christoph Hauert
 */
public class Vector2D extends Point2D {

	/**
	 * Create a new 2D vector <code>(0,0)</code>.
	 */
	public Vector2D() {
		super();
	}

	/**
	 * Create a new 2D vector from point/vector <code>p</code>.
	 * 
	 * @param p the point/vector to copy
	 */
	public Vector2D(Point2D p) {
		super(p);
	}

	/**
	 * Create a new 2D vector with coordinates <code>(x,y)</code>.
	 * 
	 * @param x the {@code x}-coordinate
	 * @param y the {@code y}-coordinate
	 */
	public Vector2D(double x, double y) {
		super(x, y);
	}

	/**
	 * Set the 2D vector to pointing from {@code from} to {@code to}.
	 * 
	 * @param from the starting point
	 * @param to   the end point
	 * @return the new vector
	 */
	public Vector2D set(Point2D from, Point2D to) {
		set(to.x - from.x, to.y - from.y);
		return this;
	}

	/**
	 * Shift the coordinates of this 2D vector <code>dx</code> to the right and
	 * <code>dy</code> upwards.
	 * 
	 * @param dx the shift in the <code>x</code>-coordinate
	 * @param dy the shift in the <code>y</code>-coordinate
	 * @return the shifted vector <code>(x+dx,y+dy)</code>
	 * 
	 * @see Point2D#shift(double, double)
	 */
	public Vector2D add(double dx, double dy) {
		shift(dx, dy);
		return this;
	}

	/**
	 * Add the 2D vector <code>add</code> to this vector.
	 * 
	 * @param add the vector to add
	 * @return the new vector
	 */
	public Vector2D add(Vector2D add) {
		shift(add.x, add.y);
		return this;
	}

	/**
	 * Add the 2D vectors <code>a</code> and <code>b</code> and store the result in
	 * this vector.
	 * 
	 * @param a the first vector
	 * @param b the second vector
	 * @return the new vector <code>a+b</code>
	 * 
	 * @see Point2D#set(double, double)
	 */
	public Vector2D add(Point2D a, Point2D b) {
		set(a.x + b.x, a.y + b.y);
		return this;
	}

	/**
	 * Subtract <code>dx</code> and <code>dy</code> from coordinates, i.e. shift
	 * {@code x}-coordinate to the left by {@code dx} and the {@code y}-coordinate
	 * down by {@code dy}. This yields the same result as
	 * <code>add(-dx, -dy)</code>.
	 * 
	 * @param dx the shift in the <code>x</code>-coordinate
	 * @param dy the shift in the <code>y</code>-coordinate
	 * @return the new vector <code>(x-dx,y-dy)</code>
	 */
	public Vector2D sub(double dx, double dy) {
		shift(-dx, -dy);
		return this;
	}

	/**
	 * Subtract the 2D vector <code>sub</code> from this vector.
	 * 
	 * @param sub the vector to subtract
	 * @return the new vector
	 */
	public Vector2D sub(Vector2D sub) {
		shift(-sub.x, -sub.y);
		return this;
	}

	/**
	 * Subtract 2D vectors <code>b</code> from <code>a</code> and store result in
	 * this 2D vector.
	 * 
	 * @param a the first vector for subtraction
	 * @param b the vector to subtract from <code>a</code>
	 * @return the vector <code>a-b</code>
	 * 
	 * @see #set(double, double)
	 */
	public Vector2D sub(Vector2D a, Vector2D b) {
		set(a.x - b.x, a.y - b.y);
		return this;
	}

	/**
	 * Calculate the dot product of 2D vector <code>d</code> and this vector.
	 * 
	 * @param d the vector to calculate the dot product with
	 * @return the dot product <code>v.d</code>
	 */
	public double dot(Vector2D d) {
		return x * d.x + y * d.y;
	}

	/**
	 * Negates the coordinates of 2D vector <code>v</code> to obtain
	 * <code>-v</code>.
	 * 
	 * @return the vector <code>v=-v</code>
	 */
	public Vector2D negate() {
		x = -x;
		y = -y;
		return this;
	}

	/**
	 * Normalizes the 2D vector <code>v</code> such that its length is
	 * <code>|v|=1</code> while preserving the direction.
	 *
	 * @return the normalized vector
	 * 
	 * @see #length()
	 */
	public Vector2D normalize() {
		scale(1.0 / length());
		return this;
	}

	/**
	 * Normalizes the 2D vector <code>v</code> such that its length is
	 * <code>l</code>, i.e. <code>|v|=l</code>, while preserving the direction.
	 *
	 * @param l the new length of the vector
	 * @return the scaled vector
	 * 
	 * @see #length()
	 */
	public Vector2D normalize(double l) {
		scale(l / length());
		return this;
	}

	/**
	 * Scales the 2D vector by the scalar factor <code>s</code> and adds the 2D
	 * vector <code>a</code> to obtain <code>s*v+a</code>. This is a shortcut for
	 * <code>v.scale(s).add(a)</code>.
	 *
	 * @param s the scalar factor
	 * @param a the vector to add
	 * @return the new vector <code>s*v+a</code>
	 * 
	 * @see #scale(double)
	 */
	public Vector2D scaleAdd(double s, Vector2D a) {
		x = x * s + a.x;
		y = y * s + a.y;
		return this;
	}

	/**
	 * Calculate the length of the 2D vector.
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
	 * Calculate the length squared of the 2D vector, <code>|v|<sup>2</sup></code>.
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
	 * {@code Vector2f} from java3d.
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
		return "[" + x + ", " + y + "]";
	}
}
