/*
 * Copyright (c) 2006, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

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

import java.util.Arrays;

/**
 * Adapted from {@code java.awt.geom.Path2D}, merged with {@code PathIterator}
 * and stripped to the bare bones. In particular, all the {@code Shape} stuff
 * and {@code AffineTransform} removed. Generally made more GWT friendly.
 * 
 * @author Christoph Hauert
 */
public class Path2D {

	/**
	 * An even-odd winding rule for determining the interior of
	 * a path.
	 *
	 * @see PathIterator#WIND_EVEN_ODD
	 * @since 1.6
	 */
	public static final int WIND_EVEN_ODD = PathIterator.WIND_EVEN_ODD;

	/**
	 * A non-zero winding rule for determining the interior of a
	 * path.
	 *
	 * @see PathIterator#WIND_NON_ZERO
	 * @since 1.6
	 */
	public static final int WIND_NON_ZERO = PathIterator.WIND_NON_ZERO;

	// For code simplicity, copy these constants to our namespace
	// and cast them to byte constants for easy storage.
	private static final byte SEG_MOVETO = (byte) PathIterator.SEG_MOVETO;
	private static final byte SEG_LINETO = (byte) PathIterator.SEG_LINETO;
	private static final byte SEG_QUADTO = (byte) PathIterator.SEG_QUADTO;
	private static final byte SEG_CUBICTO = (byte) PathIterator.SEG_CUBICTO;
	private static final byte SEG_CLOSE = (byte) PathIterator.SEG_CLOSE;

	byte[] pointTypes;
	int numTypes;
	int numCoords;
	int windingRule;

	static final int INIT_SIZE = 20;
	static final int EXPAND_MAX = 500;
	static final int EXPAND_MAX_COORDS = EXPAND_MAX * 2;
	static final int EXPAND_MIN = 10; // ensure > 6 (cubics)

	static byte[] expandPointTypes(byte[] oldPointTypes, int needed) {
		final int oldSize = oldPointTypes.length;
		final int newSizeMin = oldSize + needed;
		if (newSizeMin < oldSize) {
			// hard overflow failure - we can't even accommodate
			// new items without overflowing
			throw new ArrayIndexOutOfBoundsException(
					"pointTypes exceeds maximum capacity !");
		}
		// growth algorithm computation
		int grow = oldSize;
		if (grow > EXPAND_MAX) {
			grow = Math.max(EXPAND_MAX, oldSize >> 3); // 1/8th min
		} else if (grow < EXPAND_MIN) {
			grow = EXPAND_MIN;
		}
		assert grow > 0;

		int newSize = oldSize + grow;
		if (newSize < newSizeMin) {
			// overflow in growth algorithm computation
			newSize = Integer.MAX_VALUE;
		}
		// try allocating the larger array
		return Arrays.copyOf(oldPointTypes, newSize);
	}

	double doubleCoords[];

	/**
	 * Constructs a new empty double precision {@code Path2D} object
	 * with a default winding rule of {@link #WIND_NON_ZERO}.
	 *
	 * @since 1.6
	 */
	public Path2D() {
		this(WIND_NON_ZERO, INIT_SIZE);
	}

	/**
	 * Constructs a new empty double precision {@code Path2D} object
	 * with the specified winding rule to control operations that
	 * require the interior of the path to be defined.
	 *
	 * @param rule the winding rule
	 * @see #WIND_EVEN_ODD
	 * @see #WIND_NON_ZERO
	 * @since 1.6
	 */
	public Path2D(int rule) {
		this(rule, INIT_SIZE);
	}

	/**
	 * Constructs a new empty double precision {@code Path2D} object
	 * with the specified winding rule and the specified initial
	 * capacity to store path segments.
	 * This number is an initial guess as to how many path segments
	 * are in the path, but the storage is expanded as needed to store
	 * whatever path segments are added to this path.
	 *
	 * @param rule            the winding rule
	 * @param initialCapacity the estimate for the number of path segments
	 *                        in the path
	 * @see #WIND_EVEN_ODD
	 * @see #WIND_NON_ZERO
	 * @since 1.6
	 */
	public Path2D(int rule, int initialCapacity) {
		setWindingRule(rule);
		this.pointTypes = new byte[initialCapacity];
		doubleCoords = new double[initialCapacity * 2];
	}

	/**
	 * Constructs a new double precision {@code Path2D} object
	 * from an arbitrary {@link java.awt.Shape} object.
	 * All of the initial geometry and the winding rule for this path are
	 * taken from the specified {@code Shape} object.
	 *
	 * @param p2d the specified {@code Shape} object
	 * @since 1.6
	 */
	public Path2D(Path2D p2d) {
		setWindingRule(p2d.windingRule);
		this.numTypes = p2d.numTypes;
		// trim arrays:
		this.pointTypes = Arrays.copyOf(p2d.pointTypes, p2d.numTypes);
		this.numCoords = p2d.numCoords;
		this.doubleCoords = Arrays.copyOf(doubleCoords, numCoords);
	}

	/**
	 * Constructs a new double precision {@code Path2D} object
	 * from an arbitrary {@link Path2D} object, transformed by an
	 * {@link AffineTransform} object.
	 * All of the initial geometry and the winding rule for this path are
	 * taken from the specified {@code Path2D} object and transformed
	 * by the specified {@code AffineTransform} object.
	 *
	 * @param p2d the specified {@code Path2D} object
	 * @param at  the specified {@code AffineTransform} object
	 * @since 1.6
	 */
	public Path2D(Path2D p2d, AffineTransform at) {
		setWindingRule(p2d.windingRule);
		this.numTypes = p2d.numTypes;
		// trim arrays:
		this.pointTypes = Arrays.copyOf(p2d.pointTypes, p2d.numTypes);
		this.numCoords = p2d.numCoords;
		this.doubleCoords = p2d.cloneCoordsDouble(at);
	}

	public final void trimToSize() {
		// trim arrays:
		if (numTypes < pointTypes.length) {
			this.pointTypes = Arrays.copyOf(pointTypes, numTypes);
		}
		if (numCoords < doubleCoords.length) {
			this.doubleCoords = Arrays.copyOf(doubleCoords, numCoords);
		}
	}

	double[] cloneCoordsDouble(AffineTransform at) {
		// trim arrays:
		double ret[];
		if (at == null) {
			ret = Arrays.copyOf(doubleCoords, numCoords);
		} else {
			ret = new double[numCoords];
			at.transform(doubleCoords, 0, ret, 0, numCoords / 2);
		}
		return ret;
	}

	void append(double x, double y) {
		doubleCoords[numCoords++] = x;
		doubleCoords[numCoords++] = y;
	}

	Point2D getPoint(int coordindex) {
		return new Point2D(doubleCoords[coordindex],
				doubleCoords[coordindex + 1]);
	}

	void needRoom(boolean needMove, int newCoords) {
		if ((numTypes == 0) && needMove) {
			// GWT throw new IllegalPathStateException("missing initial moveto "+
			// cannot throw exception because of override - suck it up or throw error...
			throw new Error("missing initial moveto " +
					"in path definition");
		}
		if (numTypes >= pointTypes.length) {
			pointTypes = expandPointTypes(pointTypes, 1);
		}
		if (numCoords > (doubleCoords.length - newCoords)) {
			doubleCoords = expandCoords(doubleCoords, newCoords);
		}
	}

	static double[] expandCoords(double[] oldCoords, int needed) {
		final int oldSize = oldCoords.length;
		final int newSizeMin = oldSize + needed;
		if (newSizeMin < oldSize) {
			// hard overflow failure - we can't even accommodate
			// new items without overflowing
			// GWT throw new ArrayIndexOutOfBoundsException(
			throw new Error(
					"coords exceeds maximum capacity !");
		}
		// growth algorithm computation
		int grow = oldSize;
		if (grow > EXPAND_MAX_COORDS) {
			grow = Math.max(EXPAND_MAX_COORDS, oldSize >> 3); // 1/8th min
		} else if (grow < EXPAND_MIN) {
			grow = EXPAND_MIN;
		}
		assert grow > needed;

		int newSize = oldSize + grow;
		if (newSize < newSizeMin) {
			// overflow in growth algorithm computation
			newSize = Integer.MAX_VALUE;
		}
		// try allocating the larger array
		return Arrays.copyOf(oldCoords, newSize);
	}

	/**
	 * Adds a point to the path by moving to the specified
	 * coordinates specified in double precision.
	 *
	 * @param x the specified X coordinate
	 * @param y the specified Y coordinate
	 * @since 1.6
	 */
	public final synchronized void moveTo(double x, double y) {
		if (numTypes > 0 && pointTypes[numTypes - 1] == SEG_MOVETO) {
			doubleCoords[numCoords - 2] = x;
			doubleCoords[numCoords - 1] = y;
		} else {
			needRoom(false, 2);
			pointTypes[numTypes++] = SEG_MOVETO;
			doubleCoords[numCoords++] = x;
			doubleCoords[numCoords++] = y;
		}
	}

	/**
	 * Adds a point to the path by drawing a straight line from the
	 * current coordinates to the new specified coordinates
	 * specified in double precision.
	 *
	 * @param x the specified X coordinate
	 * @param y the specified Y coordinate
	 * @since 1.6
	 */
	public final synchronized void lineTo(double x, double y) {
		needRoom(true, 2);
		pointTypes[numTypes++] = SEG_LINETO;
		doubleCoords[numCoords++] = x;
		doubleCoords[numCoords++] = y;
	}

	/**
	 * Adds a curved segment, defined by two new points, to the path by
	 * drawing a Quadratic curve that intersects both the current
	 * coordinates and the specified coordinates {@code (x2,y2)},
	 * using the specified point {@code (x1,y1)} as a quadratic
	 * parametric control point.
	 * All coordinates are specified in double precision.
	 *
	 * @param x1 the X coordinate of the quadratic control point
	 * @param y1 the Y coordinate of the quadratic control point
	 * @param x2 the X coordinate of the final end point
	 * @param y2 the Y coordinate of the final end point
	 * @since 1.6
	 */
	public final synchronized void quadTo(double x1, double y1,
			double x2, double y2) {
		needRoom(true, 4);
		pointTypes[numTypes++] = SEG_QUADTO;
		doubleCoords[numCoords++] = x1;
		doubleCoords[numCoords++] = y1;
		doubleCoords[numCoords++] = x2;
		doubleCoords[numCoords++] = y2;
	}

	/**
	 * Adds a curved segment, defined by three new points, to the path by
	 * drawing a B&eacute;zier curve that intersects both the current
	 * coordinates and the specified coordinates {@code (x3,y3)},
	 * using the specified points {@code (x1,y1)} and {@code (x2,y2)} as
	 * B&eacute;zier control points.
	 * All coordinates are specified in double precision.
	 *
	 * @param x1 the X coordinate of the first B&eacute;zier control point
	 * @param y1 the Y coordinate of the first B&eacute;zier control point
	 * @param x2 the X coordinate of the second B&eacute;zier control point
	 * @param y2 the Y coordinate of the second B&eacute;zier control point
	 * @param x3 the X coordinate of the final end point
	 * @param y3 the Y coordinate of the final end point
	 * @since 1.6
	 */
	public final synchronized void curveTo(double x1, double y1,
			double x2, double y2,
			double x3, double y3) {
		needRoom(true, 6);
		pointTypes[numTypes++] = SEG_CUBICTO;
		doubleCoords[numCoords++] = x1;
		doubleCoords[numCoords++] = y1;
		doubleCoords[numCoords++] = x2;
		doubleCoords[numCoords++] = y2;
		doubleCoords[numCoords++] = x3;
		doubleCoords[numCoords++] = y3;
	}

	/**
	 * Appends the geometry of the specified {@link Iterator} object
	 * to the path, possibly connecting the new geometry to the existing
	 * path segments with a line segment.
	 * If the {@code connect} parameter is {@code true} and the
	 * path is not empty then any initial {@code moveTo} in the
	 * geometry of the appended {@code Shape} is turned into a
	 * {@code lineTo} segment.
	 * If the destination coordinates of such a connecting {@code lineTo}
	 * segment match the ending coordinates of a currently open
	 * subpath then the segment is omitted as superfluous.
	 * The winding rule of the specified {@code Shape} is ignored
	 * and the appended geometry is governed by the winding
	 * rule specified for this path.
	 *
	 * @param pi      the {@code Iterator} whose geometry is appended to this path
	 * @param connect a boolean to control whether or not to turn an initial
	 *                {@code moveTo} segment into a {@code lineTo} segment
	 *                to connect the new geometry to the existing path
	 * @since 1.6
	 */
	public final void append(Iterator pi, boolean connect) {
		double coords[] = new double[6];
		while (!pi.isDone()) {
			switch (pi.currentSegment(coords)) {
				case SEG_MOVETO:
					if (!connect || numTypes < 1 || numCoords < 1) {
						moveTo(coords[0], coords[1]);
						break;
					}
					if (pointTypes[numTypes - 1] != SEG_CLOSE &&
							doubleCoords[numCoords - 2] == coords[0] &&
							doubleCoords[numCoords - 1] == coords[1]) {
						// Collapse out initial moveto/lineto
						break;
					}
					lineTo(coords[0], coords[1]);
					break;
				case SEG_LINETO:
					lineTo(coords[0], coords[1]);
					break;
				case SEG_QUADTO:
					quadTo(coords[0], coords[1],
							coords[2], coords[3]);
					break;
				case SEG_CUBICTO:
					curveTo(coords[0], coords[1],
							coords[2], coords[3],
							coords[4], coords[5]);
					break;
				case SEG_CLOSE:
					closePath();
					break;
				default:
					/* NOTREACHED */
			}
			pi.next();
			connect = false;
		}
	}

	/**
	 * Transforms the geometry of this path using the specified
	 * {@link AffineTransform}. The geometry is transformed in place, which
	 * permanently changes the boundary defined by this object.
	 *
	 * @param at the {@code AffineTransform} used to transform the area
	 * @since 1.6
	 */
	public final void transform(AffineTransform at) {
		at.transform(doubleCoords, 0, doubleCoords, 0, numCoords / 2);
	}

	/**
	 * Translate path.
	 * 
	 * @param dx shift path horizontally by <code>dx</code>
	 * @param dy shift path vertically by <code>dy</code>
	 */
	public void translate(double dx, double dy) {
		int idx = 0;
		int n = 0;
		while (idx < doubleCoords.length) {
			doubleCoords[idx] += dx;
			doubleCoords[idx + 1] += dy;
			idx += Iterator.curvecoords[pointTypes[n++]];
		}
	}

	/**
	 * Returns a high precision and more accurate bounding box of
	 * the <code>Shape</code> than the <code>getBounds</code> method.
	 * Note that there is no guarantee that the returned
	 * {@link Rectangle2D} is the smallest bounding box that encloses
	 * the <code>Shape</code>, only that the <code>Shape</code> lies
	 * entirely within the indicated <code>Rectangle2D</code>. The
	 * bounding box returned by this method is usually tighter than that
	 * returned by the <code>getBounds</code> method and never fails due
	 * to overflow problems since the return value can be an instance of
	 * the <code>Rectangle2D</code> that uses double precision values to
	 * store the dimensions.
	 * 
	 * @return an instance of <code>Rectangle2D</code> that is a
	 *         high-precision bounding box of the <code>Shape</code>.
	 * @since 1.6
	 * 
	 * @evoludo.impl {@code getBounds()} is not implemented
	 */
	public final synchronized Rectangle2D getBounds2D() {
		double x1, y1, x2, y2;
		int i = numCoords;
		if (i > 0) {
			y1 = y2 = doubleCoords[--i];
			x1 = x2 = doubleCoords[--i];
			while (i > 0) {
				double y = doubleCoords[--i];
				double x = doubleCoords[--i];
				if (x < x1)
					x1 = x;
				if (y < y1)
					y1 = y;
				if (x > x2)
					x2 = x;
				if (y > y2)
					y2 = y;
			}
		} else {
			x1 = y1 = x2 = y2 = 0.0;
		}
		return new Rectangle2D(x1, y1, x2 - x1, y2 - y1);
	}

	/**
	 * Returns an iterator object that iterates along this path.
	 * boundary and provides access to a flattened view of the
	 * <code>Shape</code> outline geometry.
	 * <p>
	 * Only SEG_MOVETO, SEG_LINETO, and SEG_CLOSE point types are
	 * returned by the iterator.
	 * <p>
	 * Each call to this method returns a fresh <code>PathIterator</code>
	 * object that traverses the path independently from any other
	 * <code>PathIterator</code> objects in use at the same time.
	 * <p>
	 * The iterator for this class is not multi-threaded safe,
	 * which means that the {@code Path2D} class does not
	 * guarantee that modifications to the geometry of this
	 * {@code Path2D} object do not affect any iterations of
	 * that geometry that are already in process.
	 *
	 * @return a new {@code PathIterator} that iterates along the path
	 * @since 1.6
	 */
	public final Iterator getPathIterator() {
		return new Iterator(this);
	}

	/**
	 * Construct a polygon from array of <code>x</code>- and <code>y</code>
	 * coordinates. Essentially replaces java.awt.Polygon.
	 * 
	 * @param x array of <code>x</code>-coordinates
	 * @param y array of <code>y</code>-coordinates
	 * @return path of polygon
	 */
	public static Path2D createPolygon2D(double[] x, double[] y) {
		int len = x.length;
		Path2D path = new Path2D(WIND_NON_ZERO, len + 1);
		path.moveTo(x[0], y[0]);
		for (int n = 1; n < len; n++)
			path.lineTo(x[n], y[n]);
		path.closePath();
		return path;
	}

	/**
	 * Adapted from java.awt.geom.PathIterator, merged with Path2D Iterator and
	 * CopyIterator. Constants moved to Path2D and made more GWT friendly.
	 */
	public static class Iterator implements PathIterator {
		int typeIdx;
		int pointIdx;
		Path2D path;

		static final int curvecoords[] = { 2, 2, 4, 6, 0 };

		Iterator(Path2D path) {
			this.path = path;
			this.doubleCoords = path.doubleCoords;
		}

		double doubleCoords[];

		/**
		 * Returns the coordinates and type of the current path segment in
		 * the iteration.
		 * The return value is the path-segment type:
		 * SEG_MOVETO, SEG_LINETO, SEG_QUADTO, SEG_CUBICTO, or SEG_CLOSE.
		 * A double array of length 6 must be passed in and can be used to
		 * store the coordinates of the point(s).
		 * Each point is stored as a pair of double x,y coordinates.
		 * SEG_MOVETO and SEG_LINETO types returns one point,
		 * SEG_QUADTO returns two points,
		 * SEG_CUBICTO returns 3 points
		 * and SEG_CLOSE does not return any points.
		 * 
		 * @param coords an array that holds the data returned from
		 *               this method
		 * @return the path-segment type of the current path segment.
		 * @see #SEG_MOVETO
		 * @see #SEG_LINETO
		 * @see #SEG_QUADTO
		 * @see #SEG_CUBICTO
		 * @see #SEG_CLOSE
		 */
		@Override
		public int currentSegment(double[] coords) {
			int type = path.pointTypes[typeIdx];
			int numCoords = curvecoords[type];
			if (numCoords > 0) {
				System.arraycopy(doubleCoords, pointIdx,
						coords, 0, numCoords);
			}
			return type;
		}

		/**
		 * Returns the winding rule for determining the interior of the
		 * path.
		 * 
		 * @return the winding rule.
		 * @see #WIND_EVEN_ODD
		 * @see #WIND_NON_ZERO
		 */
		@Override
		public int getWindingRule() {
			return path.getWindingRule();
		}

		/**
		 * Tests if the iteration is complete.
		 * 
		 * @return <code>true</code> if all the segments have
		 *         been read; <code>false</code> otherwise.
		 */
		@Override
		public boolean isDone() {
			return (typeIdx >= path.numTypes);
		}

		/**
		 * Moves the iterator to the next segment of the path forwards
		 * along the primary direction of traversal as long as there are
		 * more points in that direction.
		 */
		@Override
		public void next() {
			int type = path.pointTypes[typeIdx++];
			pointIdx += curvecoords[type];
		}
	}

	/**
	 * Closes the current subpath by drawing a straight line back to
	 * the coordinates of the last {@code moveTo}. If the path is already
	 * closed then this method has no effect.
	 *
	 * @since 1.6
	 */
	public final synchronized void closePath() {
		if (numTypes == 0 || pointTypes[numTypes - 1] != SEG_CLOSE) {
			needRoom(true, 0);
			pointTypes[numTypes++] = SEG_CLOSE;
		}
	}

	/**
	 * Returns the fill style winding rule.
	 *
	 * @return an integer representing the current winding rule.
	 * @see #WIND_EVEN_ODD
	 * @see #WIND_NON_ZERO
	 * @see #setWindingRule
	 * @since 1.6
	 */
	public final synchronized int getWindingRule() {
		return windingRule;
	}

	/**
	 * Sets the winding rule for this path to the specified value.
	 *
	 * @param rule an integer representing the specified
	 *             winding rule
	 * @exception IllegalArgumentException if
	 *                                     {@code rule} is not either
	 *                                     {@link #WIND_EVEN_ODD} or
	 *                                     {@link #WIND_NON_ZERO}
	 * @see #getWindingRule
	 * @since 1.6
	 */
	public final void setWindingRule(int rule) {
		if (rule != WIND_EVEN_ODD && rule != WIND_NON_ZERO) {
			throw new IllegalArgumentException("winding rule must be " +
					"WIND_EVEN_ODD or " +
					"WIND_NON_ZERO");
		}
		windingRule = rule;
	}

	/**
	 * Returns the coordinates most recently added to the end of the path
	 * as a {@link Point2D} object.
	 *
	 * @return a {@code Point2D} object containing the ending coordinates of
	 *         the path or {@code null} if there are no points in the path.
	 * @since 1.6
	 */
	public final synchronized Point2D getCurrentPoint() {
		int index = numCoords;
		if (numTypes < 1 || index < 1) {
			return null;
		}
		if (pointTypes[numTypes - 1] == SEG_CLOSE) {
			loop: for (int i = numTypes - 2; i > 0; i--) {
				switch (pointTypes[i]) {
					case SEG_MOVETO:
						break loop;
					case SEG_LINETO:
						index -= 2;
						break;
					case SEG_QUADTO:
						index -= 4;
						break;
					case SEG_CUBICTO:
						index -= 6;
						break;
					case SEG_CLOSE:
						break;
					default:
						/* NOTREACHED */
				}
			}
		}
		return getPoint(index - 2);
	}

	/**
	 * Resets the path to empty. The append position is set back to the
	 * beginning of the path and all coordinates and point types are
	 * forgotten.
	 *
	 * @since 1.6
	 */
	public final synchronized void reset() {
		numTypes = numCoords = 0;
	}

	public boolean isEmpty() {
		return (numTypes == 0);
	}

	@Override
	public String toString() {
		String msg = "[path=" + numTypes + ": ";
		int npoints = Math.min(numTypes, 10);
		int idx = 0;
		for (int n = 0; n < npoints; n++) {
			switch (pointTypes[n]) {
				case SEG_MOVETO:
					msg += " (" + doubleCoords[idx++] + "," + doubleCoords[idx++] + ")";
					break;
				case SEG_LINETO:
					msg += "-(" + doubleCoords[idx++] + "," + doubleCoords[idx++] + ")";
					break;
				case SEG_QUADTO:
					msg += "2(" + doubleCoords[idx++] + "," + doubleCoords[idx++] + "," + doubleCoords[idx++] + ","
							+ doubleCoords[idx++] + ")";
					break;
				case SEG_CUBICTO:
					msg += "3(" + doubleCoords[idx++] + "," + doubleCoords[idx++] + "," + doubleCoords[idx++] + ","
							+ doubleCoords[idx++] + "," + doubleCoords[idx++] + "," + doubleCoords[idx++] + ")";
					break;
				case SEG_CLOSE:
					msg += ": ";
					break;
				default:
					/* NOTREACHED */
			}
		}
		if (npoints < numTypes)
			msg += "...";
		return msg + "]";
	}
}
