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

package org.evoludo.graphics;

import org.evoludo.simulator.views.BasicTooltipProvider;

/**
 * Views that provide more sophisticated tooltips should implement this
 * interface. The tooltip string may include HTML formatting.
 * 
 * @author Christoph Hauert
 */
public interface TooltipProvider extends BasicTooltipProvider {

	/**
	 * Views that provide extended tooltips for specific locations that go beyond
	 * the capabilities of the {@link BasicTooltipProvider} should implement this
	 * interface. The index typically refers to an individual node but may equally
	 * refer to a location on a lattice for PDE models or 2D trait distributions.
	 */
	public interface Index extends TooltipProvider {

		/**
		 * Get the tooltip for the location with index {@code index}.
		 * 
		 * @param graph the graph requesting the tooltip
		 * @param index the {@code index} of the location
		 * @return the tooltip for the node
		 * 
		 * @see org.evoludo.simulator.views.BasicTooltipProvider#getTooltipAt(int)
		 */
		public String getTooltipAt(AbstractGraph<?> graph, int index);
	}

	/**
	 * Views that provide extended tooltips for specific (scaled)
	 * coordinates {@code (x,y)} on a parametric plot that go beyond
	 * the capabilities of the {@link BasicTooltipProvider} should implement this
	 * interface.
	 * 
	 * @see org.evoludo.graphics.ParaGraph
	 * @see org.evoludo.simulator.views.Phase2D
	 */
	public interface Parametric extends TooltipProvider {

		/**
		 * Get the tooltip for the parametric plot {@code graph} at the scaled
		 * coordinates {@code (x,y)} with the origin in the lower left corner of the
		 * phase plane.
		 * 
		 * @param graph the graph requesting the tooltip
		 * @param x     the x-coordinate
		 * @param y     the y-coordinate
		 * @return the tooltip for the location {@code (x,y)}
		 * 
		 * @see org.evoludo.simulator.views.BasicTooltipProvider#getTooltipAt(double,
		 *      double)
		 */
		public String getTooltipAt(ParaGraph graph, double x, double y);
	}

	/**
	 * Views that provide extended tooltips for specific (scaled) coordinates
	 * {@code (x,y)} on a simplex plot that go beyond the capabilities of the
	 * {@link BasicTooltipProvider} should implement this interface.
	 * 
	 * @see org.evoludo.graphics.S3Graph
	 * @see org.evoludo.simulator.views.S3
	 */
	public interface Simplex extends TooltipProvider {

		/**
		 * Get the tooltip for the simplex {@code graph} at the scaled coordinates
		 * {@code (x,y)} with the origin in the lower left corner of the simplex.
		 * 
		 * @param graph the graph requesting the tooltip
		 * @param x     the x-coordinate
		 * @param y     the y-coordinate
		 * @return the tooltip for the location {@code (x,y)}
		 * 
		 * @see org.evoludo.simulator.views.BasicTooltipProvider#getTooltipAt(double,
		 *      double)
		 */
		public String getTooltipAt(S3Graph graph, double x, double y);
	}
}
