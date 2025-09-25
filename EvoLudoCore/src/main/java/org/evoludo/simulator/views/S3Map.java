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

package org.evoludo.simulator.views;

import java.awt.Color;

import org.evoludo.geom.Point2D;
import org.evoludo.math.ArrayMath;
import org.evoludo.util.Formatter;

/**
 * Default mapping of data to simplex \(S_3\) projections. Custom
 * implementations of the {@code Data2S3} interface can be provided by modules
 * that implement the {@code HasS3} interface.
 * 
 * @see HasS3#getS3Map(int)
 */
public class S3Map implements BasicTooltipProvider {

	/**
	 * The name of the map.
	 */
	String label = "";

	/**
	 * The names of the traits.
	 */
	String[] names;

	/**
	 * The colors of the traits.
	 */
	Color[] colors;

	/**
	 * The indices of the traits on the simplex. The first entry refers to the lower
	 * left corner, the second to the lower right and the last entry to the top
	 * corner.
	 */
	int[] order = new int[] { 0, 1, 2 };

	/**
	 * The role of this map.
	 */
	int role = -1;

	/**
	 * Temporary storage for the simplex coordinates of the tooltip.
	 */
	double[] tip = new double[3];

	/**
	 * Create a new mapping of data to simplex projections.
	 */
	public S3Map() {
		this(-1, null);
	}

	/**
	 * Create a new mapping of data to simplex projections for different roles. The
	 * role is specified by the index {@code role} and names {@code label}.
	 * 
	 * @param role  the role of the data
	 * @param label the name of the map
	 */
	public S3Map(int role, String label) {
		this.label = label;
		this.role = role;
	}

	/**
	 * Get the name of the map.
	 * 
	 * @return the name of the map
	 */
	public String getLabel() {
		return label;
	}

	/**
	 * Get the role of the map.
	 * 
	 * @return the role of the map
	 */
	public int getRole() {
		return role;
	}

	/**
	 * Set the indices of the traits displayed on the simplex. The first entry
	 * {@code order[0]} denotes the index of the trait in the lower left corner of
	 * the simplex, the second entry {@code order[1]} the index of the trait in the
	 * lower right corner, and the last entry {@code order[2]} the index of the
	 * trait in the top corner.
	 * <p>
	 * In multi-species models the traits are numbered sequentially, i.e. if the
	 * first species has <code>nTraits</code> then e.g. an index of
	 * <code>nTraits+1</code> refers to the <em>second</em> trait of the second
	 * species. Be careful to account for vacant types in density based models.
	 * 
	 * @param order the array of indices
	 */
	public void setOrder(int[] order) {
		System.arraycopy(order, 0, this.order, 0, this.order.length);
	}

	/**
	 * Get the indices of the traits that span the simplex. The first entry
	 * {@code order[0]} denotes the index of the trait in the lower left corner of
	 * the simplex, the second entry {@code order[1]} the index of the trait in the
	 * lower right corner, and the last entry {@code order[2]} the index of the
	 * trait in the top corner.
	 * 
	 * @return the array of indices
	 */
	public int[] getOrder() {
		return order;
	}

	/**
	 * Set the names of the traits.
	 * 
	 * @param names the names of the traits
	 */
	public void setNames(String[] names) {
		this.names = names;
	}

	/**
	 * Get the names of the traits.
	 * 
	 * @return the names of the traits
	 */
	public String[] getNames() {
		return names;
	}

	/**
	 * Get the name of the traits at the corner with index {@code idx}.
	 * 
	 * @param idx the index of the corner
	 * @return the names of the traits
	 */
	public String getName(int idx) {
		return names[order[idx]];
	}

	/**
	 * Set the colors of the traits.
	 * 
	 * @param colors the colors of the traits
	 */
	public void setColors(Color[] colors) {
		this.colors = colors;
	}

	/**
	 * Get the colors of the traits.
	 * 
	 * @return the colors of the traits
	 */
	public Color[] getColors() {
		return colors;
	}

	/**
	 * Convert the data array to cartesian coordinates of point on simplex. The
	 * conversion observes the selection and order of traits.
	 * <p>
	 * <strong>Notes:</strong>
	 * <ol>
	 * <li>The array <code>s</code> includes the time at <code>s[0]</code> and
	 * should not be altered.
	 * <li>The point on simplex is returned in scaled user coordinates in
	 * {@code [0,1]}.
	 * <li>In order to deal with projections onto \(S_3\) subspaces the coordinates
	 * do not need to sum up to {@code 1.0}.
	 * </ol>
	 * 
	 * @param s3 the data array indicating a point on the simplex
	 * @param p  the cartesian coordinates of the point on the simplex
	 * @return the point {@code p}
	 * 
	 * @see #setOrder(int[])
	 */
	public Point2D data2S3(double[] s3, Point2D p) {
		return data2S3(s3[order[0] + 1], s3[order[1] + 1], s3[order[2] + 1], p);
	}

	/**
	 * Convert data triplet to cartesian coordinates of point on simplex.
	 * <p>
	 * <strong>Notes:</strong>
	 * <ol>
	 * <li>The point on simplex is returned in scaled user coordinates in
	 * {@code [0,1]}.
	 * <li>In order to deal with projections onto \(S_3\) subspaces the coordinates
	 * {@code s1}, {@code s2}, {@code s3}, do not need to sum up to {@code 1.0}.
	 * </ol>
	 * 
	 * @param s1 the index of the trait in the lower left corner of the simplex
	 * @param s2 the index of the trait in the lower right corner of the simplex
	 * @param s3 the index of the trait in the top corner of the simplex
	 * @param p  the cartesian coordinates of the point on the simplex
	 * @return the point {@code p}
	 */
	public Point2D data2S3(double s1, double s2, double s3, Point2D p) {
		// top (c): s3, right (d): s2, left (l): s1
		p.x = s2 - s1; // [-1, 1]
		p.y = (s3 - s2 - s1 + 1.0) * 0.5 - 1.0 / 3.0; // [-1/3, 2/3]
		p.scale(s1 + s2 + s3);
		p.x = (p.x + 1.0) * 0.5;
		p.y = 2.0 / 3.0 - p.y;
		return p;
	}

	/**
	 * Convert scaled cartesian coordinates of point on simplex to data array. The
	 * coordinates are in {@code [0,1]}.
	 * <p>
	 * <strong>Note:</strong> The array <code>data</code> contains a copy
	 * of the last data point recorded in the buffer (excluding time).
	 * 
	 * @param x the x-coordinate of the point
	 * @param y the y-coordinate of the point
	 * @param s the point on the simplex
	 * @return the array {@code s}
	 */
	public double[] s32Data(double x, double y, double[] s) {
		// point is in scaled user coordinates
		s[order[2]] = Math.max(0.0, y);
		double s1 = Math.max(0.0, x - y * 0.5);
		s[order[1]] = s1;
		s[order[0]] = Math.max(0.0, 1.0 - y - s1);
		ArrayMath.normalize(s);
		return s;
	}

	@Override
	public String getTooltipAt(double sx, double sy) {
		s32Data(sx, sy, tip);
		StringBuilder msg = new StringBuilder("<table>");
		for (int i = 0; i < 3; i++)
			msg.append("<tr><td style='text-align:right'><i>")
					.append(names[i])
					.append(":</i></td><td>")
					.append(Formatter.formatPercent(tip[i], 2))
					.append("</td></tr>");
		return msg.append("</table>").toString();
	}
}
