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

package org.evoludo.simulator.views;

/**
 * Graphs that provide basic tooltips should implement this interface. The
 * tooltip string may include HTML formatting.
 * 
 * <h3>Important</h3>
 * The implementation must be agnostic of GWT or JRE implementations and cannot
 * take advantage of view specific features.
 * 
 * @see org.evoludo.graphics.TooltipProvider
 * 
 * @author Christoph Hauert
 */
public interface BasicTooltipProvider {

	/**
	 * Get the tooltip at the scaled coordinates {@code (x,y)} with the origin in
	 * the lower left corner of the graph.
	 * 
	 * @param x the {@code x} coordinate
	 * @param y the {@code y} coordinate
	 * @return the tooltip
	 */
	public default String getTooltipAt(double x, double y) {
		return null;
	}

	/**
	 * Get the tooltip for the location with index {@code index}. The index
	 * typically refers to an individual node but may equally refer to a location on
	 * a lattice for PDE models or trait distributions.
	 * 
	 * @param index the {@code index} of the location
	 * @return the tooltip
	 */
	public default String getTooltipAt(int index) {
		return null;
	}
}
