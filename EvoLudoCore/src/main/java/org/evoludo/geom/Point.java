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
 * Interface representing a point in 2D or 3D space determined by Cartesian
 * coordinates.
 * 
 * <h3>Work in progress...</h3>
 * Rudimentary implementation, taylored to the current needs. Could be made more
 * useful/powerful.
 * 
 * @author Christoph Hauert
 */
public interface Point {

	/**
	 * Randomly shake the position of this point by an amount scaled by
	 * {@code quake}.
	 * 
	 * @param quake the amount to shake the point
	 * @return this point to enable chains of manipulations
	 */
	public Point shake(double quake);

}
