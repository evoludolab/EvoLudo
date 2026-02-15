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

package org.evoludo.simulator.views;

import org.evoludo.geom.Point2D;
import org.evoludo.util.RingBuffer;

/**
 * {@link org.evoludo.simulator.modules.Module Module}s that implement the
 * {@code HasPhase2D} interface request a graphical view to visualize the
 * mean state of the population in a 2D (projection) of the phase plane as a
 * function of time in their GUI: {@link org.evoludo.simulator.views.Phase2D}
 * for GWT and {@link org.evoludo.simulator.views.MVDPhase2D} for JRE. The
 * mapping of the data can be customized through the {@link Data2Phase}
 * interface.
 * <p>
 * <strong>Important:</strong> Implementations have to be agnostic of the
 * runtime environment (JRE vs GWT).
 * 
 * @author Christoph Hauert
 */
public interface HasPhase2D {

	/**
	 * Get the map that transforms the data of the module to a 2D phase plane
	 * (projection).
	 * 
	 * @return the map
	 */
	public default Data2Phase getPhase2DMap() {
		return null;
	}

	/**
	 * Set the map that transforms the data of the module to a 2D phase plane
	 * (projection) to {@code map}. This provides an opportunity for implementing
	 * classes to change settings of the map.
	 * 
	 * @param map the map
	 */
	public default void setPhase2DMap(Data2Phase map) {
	}

	/**
	 * Interface for providing custom mappings from data to 2D phase
	 * plane projections.
	 */
	public static interface Data2Phase {

		/**
		 * Convert data array to point on phase plane. Note: <code>data</code> includes
		 * time at <code>node[0]</code> and should not be altered. Point on phase plane
		 * is returned in user coordinates.
		 * 
		 * @param data  array of data to convert
		 * @param point on phase plane
		 * @return <code>true</code> upon successful completion of conversion
		 */
		public boolean data2Phase(double[] data, Point2D point);

		/**
		 * Convert point on phase plane to data array. <code>data</code> contains a copy
		 * of the last data point recorded in the buffer (excluding time).
		 * 
		 * @param point on phase plane to convert (in user coordinates)
		 * @param data  array of data
		 * @return <code>true</code> upon successful completion of conversion
		 */
		public boolean phase2Data(Point2D point, double[] data);

		/**
		 * Reset the map. For maps that implement automatic scaling of the axes this
		 * should reset the range of the phase plane.
		 */
		public default void reset() {
		}

		/**
		 * Return custom label for <code>x</code>-axis
		 * 
		 * @return <code>x</code>-axis label
		 */
		public default String getXAxisLabel() {
			return null;
		}

		/**
		 * Return custom label for <code>y</code>-axis
		 * 
		 * @return <code>y</code>-axis label
		 */
		public default String getYAxisLabel() {
			return null;
		}

		/**
		 * Return minimum value for <code>x</code>-axis. Returns {@code 0} by default.
		 * 
		 * @param buffer the buffer with data points
		 * @return the lower bound for <code>x</code>-axis
		 */
		public default double getMinX(RingBuffer<double[]> buffer) {
			return 0.0;
		}

		/**
		 * Return maximum value for <code>x</code>-axis. Returns {@code 1} by default.
		 * 
		 * @param buffer the buffer with data points
		 * @return upper bound for <code>x</code>-axis
		 */
		public default double getMaxX(RingBuffer<double[]> buffer) {
			return 1.0;
		}

		/**
		 * Return minimum value for <code>y</code>-axis. Returns {@code 0} by default.
		 * 
		 * @param buffer the buffer with data points
		 * @return the lower bound for <code>y</code>-axis
		 */
		public default double getMinY(RingBuffer<double[]> buffer) {
			return 0.0;
		}

		/**
		 * Return maximum value for <code>y</code>-axis. Returns {@code 1} by default.
		 * 
		 * @param buffer the buffer with data points
		 * @return the upper bound for <code>y</code>-axis
		 */
		public default double getMaxY(RingBuffer<double[]> buffer) {
			return 1.0;
		}

		/**
		 * Modules may probe whether {@code setTraits} is implemented. If the method
		 * returns {@code true} the {@code --phase2daxes} command line option is
		 * available otherwise not (e.g. when using custom maps such as in module
		 * {@link org.evoludo.simulator.modules.ATBT ATBT}).
		 * 
		 * @return {@code true} if displayed traits can be customized
		 */
		public default boolean hasSetTraits() {
			return false;
		}

		/**
		 * Allows custom implementations to set the traits displayed on phase plane
		 * axes.
		 * <p>
		 * In multi-species models the traits are numbered sequentially, i.e. if the
		 * first species has <code>nTraits</code> then e.g. an index of
		 * <code>nTraits+1</code> refers to the <em>second</em> trait of the second
		 * species. Be careful to account for vacant types in density based models.
		 * 
		 * @param x the array of indices of horizontal trait(s)
		 * @param y the array of indices of vertical trait(s)
		 */
		public default void setTraits(int[] x, int[] y) {
		}

		/**
		 * Get the array of indices of traits displayed on the horizontal axis of the
		 * phase plane.
		 * 
		 * @return the array of indices
		 */
		public default int[] getTraitsX() {
			return new int[] { 0 };
		}

		/**
		 * Get the array of indices of traits displayed on the vertical axis of the
		 * phase plane.
		 * 
		 * @return the array of indices
		 */
		public default int[] getTraitsY() {
			return new int[] { 1 };
		}

		/**
		 * Return whether multiple traits can be selected for each axis of the phase
		 * plane.
		 * 
		 * @return {@code true} if multiple traits can be selected for each axis
		 * 
		 * @see #hasFixedAxes()
		 */
		public default boolean hasMultitrait() {
			return false;
		}

		/**
		 * Return whether axes of the phase plane are customizable.
		 * 
		 * @return {@code true} if axes can be customized
		 * 
		 * @see #hasMultitrait()
		 */
		public default boolean hasFixedAxes() {
			return true;
		}

		/**
		 * Set whether axes of the phase plane are customizable. Optional
		 * implementation. Axes are fixed by default.
		 * 
		 * @param hasFixedAxes {@code true} if axes are fixed
		 */
		public default void setFixedAxes(boolean hasFixedAxes) {
		}
	}
}
