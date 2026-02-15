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
 * Utility class for dealing with rectangles in 2D.
 * 
 * Note: this implementation is intended as a drop-in replacement for
 * java.awt.geom.Rectangle2D.Double. Methods are implemented only on an
 * as-needed basis.
 * 
 * @author Christoph Hauert
 */
public class Rectangle2D {

	/**
	 * The right segment of the rectangle.
	 */
	Segment2D right;

	/**
	 * The top segment of the rectangle.
	 */
	Segment2D top;

	/**
	 * The left segment of the rectangle.
	 */
	Segment2D left;

	/**
	 * The bottom segment of the rectangle.
	 */
	Segment2D bottom;

	/**
	 * The origin of the rectangle (lower left corner).
	 */
	protected Point2D origin;

	/**
	 * The width of the rectangle.
	 */
	protected double width;

	/**
	 * The height of the rectangle.
	 */
	protected double height;

	/**
	 * Create a new empty 2D rectangle with lower left corner at <code>(0,0)</code>
	 * and zero width and zero height.
	 */
	public Rectangle2D() {
		this(0.0, 0.0, 0.0, 0.0);
	}

	/**
	 * Create a copy of 2D the rectangle <code>r</code>.
	 * 
	 * @param r the 2D rectangle to copy
	 */
	public Rectangle2D(Rectangle2D r) {
		this(r.origin.x, r.origin.y, r.width, r.height);
	}

	/**
	 * Create a new 2D rectangle with lower left corner at <code>(x,y)</code>, width
	 * <code>width</code> and height <code>height</code>.
	 * 
	 * @param x      the first coordinate of rectangle (lower left corner)
	 * @param y      the second coordinate of rectangle (lower left corner)
	 * @param width  the width of rectangle
	 * @param height the height of rectangle
	 */
	public Rectangle2D(double x, double y, double width, double height) {
		initialize(this, x, y, width, height);
	}

	/**
	 * Helper method to prevent {@code this-escape} warnings.
	 * 
	 * @param self   the rectangle to initialize
	 * @param x      the x-coordinate of the lower left corner
	 * @param y      the y-coordinate of the lower left corner
	 * @param width  the width of the rectangle
	 * @param height the height of the rectangle
	 */
	protected static void initialize(Rectangle2D self, double x, double y, double width, double height) {
		self.right = new Segment2D(x + width, y, x + width, y + height);
		self.top = new Segment2D(x, y + height, x + width, y + height);
		self.left = new Segment2D(x, y, x, y + height);
		self.bottom = new Segment2D(x, y, x + width, y);
		self.origin = self.bottom.p1;
		self.width = width;
		self.height = height;
	}

	/**
	 * Set the coordinates of the lower left corner {@code (x,y)} and the dimensions
	 * of the rectangle.
	 * 
	 * @param x      the {@code x}-coordinate
	 * @param y      the {@code y}-coordinate
	 * @param width  the width
	 * @param height the height
	 * @return the new rectangle
	 */
	public Rectangle2D set(double x, double y, double width, double height) {
		initialize(this, x, y, width, height);
		return this;
	}

	/**
	 * Set the coordinates of the origin, lower left corner {@code (x,y)}, of the
	 * rectangle.
	 * 
	 * @param x the {@code x}-coordinate
	 * @param y the {@code y}-coordinate
	 * @return the transformed rectangle
	 */
	public Rectangle2D setOrigin(double x, double y) {
		shift(x - origin.x, y - origin.y);
		return this;
	}

	/**
	 * Set the dimensions, the width and height, of the rectangle.
	 * 
	 * @param width  the width
	 * @param height the height
	 * @return the transformed rectangle
	 */
	public Rectangle2D setSize(double width, double height) {
		// IMPORTANT: origin = bottom.p1 and hence bottom must be adjusted last
		right.set(origin.x + width, origin.y, origin.x + width, origin.y + height);
		top.set(origin.x, origin.y + height, origin.x + width, origin.y + height);
		left.set(origin.x, origin.y, origin.x, origin.y + height);
		bottom.set(origin.x, origin.y, origin.x + width, origin.y);
		this.width = width;
		this.height = height;
		return this;
	}

	/**
	 * Adjust the position of the origin, the lower left corner {@code (x,y)}, as
	 * well as the dimensions, i.e. the width and height, of the rectangle.
	 * 
	 * @param dx the horizontal shift of the origin
	 * @param dy the vertical shift of the origin
	 * @param dw the increase in width
	 * @param dh the increase in height
	 * @return the transformed rectangle
	 */
	public Rectangle2D adjust(double dx, double dy, double dw, double dh) {
		width += dw;
		height += dh;
		origin.shift(dx, dy);
		// IMPORTANT: origin = bottom.p1
		right.set(origin.x + width, origin.y, origin.x + width, origin.y + height);
		top.set(origin.x, origin.y + height, origin.x + width, origin.y + height);
		left.set(origin.x, origin.y, origin.x, origin.y + height);
		bottom.set(origin.x, origin.y, origin.x + width, origin.y);
		return this;
	}

	/**
	 * Shift/translate the rectangle.
	 * 
	 * @param dx the horizontal shift of the origin
	 * @param dy the vertical shift of the origin
	 * @return the translated rectangle
	 */
	public Rectangle2D shift(double dx, double dy) {
		right.shift(dx, dy);
		top.shift(dx, dy);
		left.shift(dx, dy);
		// Note: origin gets adjusted automatically because origin = bottom.p1
		bottom.shift(dx, dy);
		return this;
	}

	/**
	 * Check if the rectangle contains the point <code>p</code>.
	 * 
	 * @param p the point to check
	 * @return <code>true</code> if the point lies inside
	 */
	public boolean contains(Point2D p) {
		return contains(p.x, p.y);
	}

	/**
	 * Check if the rectangle contains the point <code>(px, py)</code>.
	 * 
	 * @param px the <code>x</code>-coordinate
	 * @param py the <code>y</code>-coordinate
	 * @return <code>true</code> if the point lies inside
	 */
	public boolean contains(double px, double py) {
		return !(width <= 0.0 || height <= 0.0 ||
				px < origin.x || py < origin.y ||
				px > origin.x + width || py > origin.y + height);
	}

	/**
	 * Check if the line (or line segment) <code>l</code> intersects the rectangle.
	 * 
	 * @param l the line to check
	 * @return <code>true</code> if the line intersects
	 */
	public boolean intersects(Line2D l) {
		if (l.intersects(right))
			return true;
		if (l.intersects(top))
			return true;
		if (l.intersects(left))
			return true;
		return l.intersects(bottom);
	}

	/**
	 * Get the {@code x}-coordinate of the lower left corner of the rectangle.
	 * <p>
	 * Method to ensure compatibility with {@code java.awt.geom.Rectangle2D.Double}.
	 * 
	 * @return the <code>x</code>-coordinate of lower left corner
	 */
	public double getX() {
		return origin.getX();
	}

	/**
	 * Get the {@code y}-coordinate of the lower left corner of the rectangle.
	 * <p>
	 * Method to ensure compatibility with {@code java.awt.geom.Rectangle2D.Double}.
	 * 
	 * @return the <code>y</code>-coordinate of lower left corner
	 */
	public double getY() {
		return origin.getY();
	}

	/**
	 * Get the {@code width} of the rectangle.
	 * <p>
	 * Method to ensure compatibility with {@code java.awt.geom.Rectangle2D.Double}.
	 * 
	 * @return the width of rectangle
	 */
	public double getWidth() {
		return width;
	}

	/**
	 * Get the {@code height} of the rectangle.
	 * <p>
	 * Method to ensure compatibility with {@code java.awt.geom.Rectangle2D.Double}.
	 * 
	 * @return the height of rectangle
	 */
	public double getHeight() {
		return height;
	}

	@Override
	public String toString() {
		return "[origin=" + origin + ", width=" + width + ", height=" + height + "]";
	}
}
