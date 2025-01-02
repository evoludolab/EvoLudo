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

/**
 * {@link org.evoludo.simulator.modules.Module Module}s that implement the
 * {@code HasS3} interface request a graphical view to visualize the
 * mean state of the population in the simplex \(S_3\) as a
 * function of time in their GUI: {@link org.evoludo.simulator.views.S3}
 * for GWT and {@link org.evoludo.simulator.views.MVDS3} for JRE.
 * <p>
 * <strong>Important:</strong> Implementations have to be agnostic of the
 * runtime environment (JRE vs GWT).
 * 
 * @author Christoph Hauert
 */

public interface HasS3 {

	/**
	 * The index of the bottom left corner.
	 */
	public final int CORNER_LEFT = 0;

	/**
	 * The index of the bottom right corner.
	 */
	public final int CORNER_RIGHT = 1;

	/**
	 * The index of the top corner.
	 */
	public final int CORNER_TOP = 2;

	/**
	 * The index of the left edge.
	 */
	public final int EDGE_LEFT = 0;

	/**
	 * The index of the right edge.
	 */
	public final int EDGE_RIGHT = 1;

	/**
	 * The index of the bottom edge.
	 */
	public final int EDGE_BOTTOM = 2;

	/**
	 * Get the map that transforms the data of the model to a 2D phase plane
	 * (projection).
	 * 
	 * @param role the role of the players
	 * @return the map
	 */
	public default S3Map getS3Map(int role) {
		return null;
	}

	/**
	 * Set the map that transforms the data of the model to a 2D phase plane
	 * (projection) to {@code map}. This provides an opportunity for implementing
	 * classes to change settings of the map.
	 * 
	 * @param map the map
	 */
	public default void setS3Map(S3Map map) {
	}
}
