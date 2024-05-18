//
// EvoLudo Project
//
// Copyright 2020 Christoph Hauert
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

package org.evoludo.simulator.views;

import org.evoludo.geom.Point2D;

/**
 * {@link org.evoludo.simulator.modules.Module Module}s that implement the
 * {@code HasS3} interface include a graphical view that depicts the
 * mean state of the population in the simplex \(S_3\) as a
 * function of time in their GUI: {@link org.evoludo.simulator.S3}
 * for GWT and {@link org.evoludo.simulator.MVDS3} for JRE.
 * <p>
 * <strong>Important:</strong> Implementations have to be agnostic of the
 * runtime environment (JRE vs GWT).
 * 
 * @author Christoph Hauert
 */

public interface HasS3 {
	/**
	 * Get the map that transforms the data of the model to a 2D phase plane
	 * (projection).
	 * 
	 * @param role the role of the players
	 * @return the map
	 */
	public default Data2S3 getS3Map(int role) {
		return null;
	}

	/**
	 * Set the map that transforms the data of the model to a 2D phase plane
	 * (projection) to {@code map}. This provides an opportunity for implementing
	 * classes to change settings of the map.
	 * 
	 * @param map the map
	 */
	public default void setS3Map(Data2S3 map) {
	}

	/**
	 * Interface for providing custom mappings from data to the S3 simplex.
	 * 
	 * @author Christoph Hauert
	 */
	public static interface Data2S3 {

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
		 * <li>
		 * </ol>
		 * 
		 * @param s the data array indicating a point on the simplex
		 * @param p the cartesian coordinates of the point on the simplex
		 * @return <code>true</code> upon successful completion of conversion
		 * 
		 * @see #setOrder(int[])
		 */
		public Point2D data2S3(double[] s, Point2D p);

		/**
		 * Convert data triplet to cartesian coordinates of point on simplex.
		 * <p>
		 * <strong>Notes:</strong>
		 * <ol>
		 * <li>The point on simplex is returned in scaled user coordinates in
		 * {@code [0,1]}.
		 * <li>In order to deal with projections onto \(S_3\) subspaces the coordinates
		 * {@code s1}, {@code s2}, {@code s3}, do not need to sum up to {@code 1.0}.
		 * <li>
		 * </ol>
		 * 
		 * @param s1 the index of the trait in the lower left corner of the simplex
		 * @param s2 the index of the trait in the lower right corner of the simplex
		 * @param s3 the index of the trait in the top corner of the simplex
		 * @param p  the cartesian coordinates of the point on the simplex
		 * @return the point {@code p}
		 */
		public Point2D data2S3(double s1, double s2, double s3, Point2D p);

		/**
		 * Convert scaled cartesian coordinates of point on simplex to data array. The
		 * coordinates are in {@code [0,1]}.
		 * <p>
		 * <strong>Note:</strong> The array <code>data</code> contains a copy
		 * of the last data point recorded in the buffer (excluding time).
		 * 
		 * @param p the point on the simplex to convert (in user coordinates)
		 * @param s the array of data
		 * @return the array {@code s}
		 */
		public double[] s32Data(double x, double y, double[] s);

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
		public default void setOrder(int[] order) {
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
		public default int[] getOrder() {
			return new int[] { 0, 1, 2 };
		}

		/**
		 * Return custom additions to context menu.
		 * <p>
		 * <strong>IMPORTANT:</strong> with GUI elements we must be extra careful to
		 * keep GWT and JRE separate. In particular, the menu cannot be passed as an
		 * argument but rather must be retrieved by implementing classes.
		 */
		public default void populateContextMenu() {
		}
	}
}
