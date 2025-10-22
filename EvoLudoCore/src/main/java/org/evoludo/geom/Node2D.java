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
 * Class representing a (network) node in 2D space with position and radius.
 * 
 * @author Christoph Hauert
 */
public class Node2D extends Point2D implements Node {

	/**
	 * The radius {@code r} of the 2D node.
	 */
	double r;

	/**
	 * Create a new 2D node at {@code (0,0)} with radius {@code r=0}.
	 */
	public Node2D() {
		this(0.0, 0.0, 0.0);
	}

	/**
	 * Create a copy of the 2D node {@code n}.
	 * 
	 * @param n the node to copy
	 */
	public Node2D(Node2D n) {
		this(n.x, n.y, n.r);
	}

	/**
	 * Create a new 2D node at {@code (x,y)} with radius {@code r}.
	 * 
	 * @param x the {@code x}-coordinate
	 * @param y the {@code y}-coordinate
	 * @param r the radius
	 */
	public Node2D(double x, double y, double r) {
		super(x, y);
		this.r = r;
	}

	/**
	 * Set the {@code x}- and {@code y}-coordinates and the radius {@code r} of 2D
	 * node.
	 * 
	 * @param x the {@code x}-coordinate
	 * @param y the {@code y}-coordinate
	 * @param r the radius
	 * @return this node
	 */
	public Node2D set(double x, double y, double r) {
		set(x, y);
		this.r = r;
		return this;
	}

	/**
	 * Set the coordinates and radious to those of node {@code n}.
	 * 
	 * @param n the node to copy
	 * @return this node
	 */
	public Node2D set(Node2D n) {
		return set(n.x, n.y, n.r);
	}

	/**
	 * Get the radius {@code r} of this 2D node.
	 * 
	 * @return the radius
	 */
	public double getR() {
		return r;
	}

	/**
	 * Set the radius {@code r} of this 2D node.
	 * 
	 * @param r the new radius
	 * @return this node
	 */
	public Node2D setR(double r) {
		this.r = r;
		return this;
	}

	@Override
	public Node2D scaleR(double scale) {
		r *= scale;
		return this;
	}

	/**
	 * Check it point {@code hit} lies inside of node.
	 * 
	 * @param hit the point to check
	 * @return {@code true} if inside
	 */
	public boolean isHit(Point2D hit) {
		double dx = hit.x - x;
		if (dx > r)
			return false;
		double dy = hit.y - y;
		if (dy > r)
			return false;
		return (dx * dx + dy * dy < r * r);
	}

	/**
	 * Calculate the distance squared between this 2D node {@code n} and node
	 * {@code m}. The distance is <em>between</em> the nodes, i.e. takes their
	 * respective radii into account {@code |n-m|<sup>2</sup>}.
	 * 
	 * @param m the node to calculate distance to
	 * @return the distance to node {@code m}
	 */
	public double distance2(Node2D m) {
		double dx = (m.x - m.r) - (x - r);
		double dy = (m.y - m.r) - (y - r);
		return dx * dx + dy * dy;
	}

	/**
	 * Calculate the distance between this 2D node {@code n} and node {@code m}. The
	 * distance is <em>between</em> the nodes, i.e. takes their respective radii
	 * into account {@code |n-m|<sup>2</sup>}.
	 * <p>
	 * For computational efficiency the fairly expensive square-roots calculations
	 * should be avoided whenever possible.
	 * 
	 * @param m the node to calculate distance to
	 * @return the distance to node {@code m}
	 * 
	 * @see #distance2(Node2D)
	 */
	public double distance(Node2D m) {
		return Math.sqrt(distance2(m));
	}

	@Override
	public String toString() {
		return "(" + x + ", " + y + "; " + r + ")";
	}
}
