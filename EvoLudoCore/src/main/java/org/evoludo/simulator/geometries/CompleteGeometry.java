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

package org.evoludo.simulator.geometries;

import org.evoludo.simulator.EvoLudo;

/**
 * Geometry implementation for complete graphs where every node connects to
 * every other node.
 * 
 * @see <a href="https://en.wikipedia.org/wiki/Complete_graph">Wikipedia:
 *      Complete graph</a>
 */
public class CompleteGeometry extends AbstractGeometry {

	/**
	 * Create a complete-graph geometry for the given engine.
	 *
	 * @param engine EvoLudo pacemaker
	 */
	public CompleteGeometry(EvoLudo engine) {
		super(engine);
		setType(GeometryType.COMPLETE);
	}

	@Override
	protected boolean checkSettings() {
		connectivity = Math.max(0, size - 1);
		if (pRewire > 0.0 || pAddwire > 0.0) {
			warn("cannot add or rewire links - ignored!");
			pRewire = 0.0;
			pAddwire = 0.0;
		}
		return false;
	}

	/**
	 * Generates a complete graph where every node is connected to every other
	 * node. This mirrors well-mixed (unstructured) populations with the exception
	 * that the focal node itself can be excluded from replacement (e.g. Moran
	 * birth-death updates) while still retaining explicit neighbourhoods.
	 *
	 * @see <a href="https://en.wikipedia.org/wiki/Complete_graph">Wikipedia:
	 *      Complete graph</a>
	 */
	@Override
	public void init() {
		if (size <= 0)
			throw new IllegalStateException("size must be set before initializing a complete geometry");
		isRewired = false;
		isUndirected = true;
		isRegular = true;
		connectivity = size - 1;
		initHierarchicalComplete(this, size, size, 0, false);
		isValid = true;
	}

	/**
	 * Initialize one hierarchical complete-graph deme inside a host geometry.
	 * For demes, this is identical to well-mixed initialization.
	 *
	 * @param geometry      host geometry receiving the links
	 * @param popSize       total population size of the host geometry
	 * @param demeSize      number of individuals in the deme
	 * @param startIndex    index offset of the deme in the host geometry
	 * @param fixedBoundary ignored for complete demes
	 */
	public static void initHierarchicalComplete(AbstractGeometry geometry, int popSize, int demeSize, int startIndex,
			boolean fixedBoundary) {
		int nIndiv = Math.max(0, demeSize);
		int nIndiv1 = Math.max(0, nIndiv - 1);
		for (int n = startIndex; n < startIndex + nIndiv; n++) {
			int[] links = new int[nIndiv1];
			geometry.in[n] = links;
			geometry.kin[n] = nIndiv1;
			geometry.out[n] = links;
			geometry.kout[n] = nIndiv1;
			for (int i = 0; i < nIndiv1; i++)
				links[i] = (startIndex + i >= n) ? startIndex + i + 1 : startIndex + i;
		}
	}
}
