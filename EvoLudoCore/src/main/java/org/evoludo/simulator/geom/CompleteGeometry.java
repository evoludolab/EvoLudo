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

package org.evoludo.simulator.geom;

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
		setType(Type.COMPLETE);
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
		int size1 = size - 1;
		isRewired = false;
		isUndirected = true;
		isRegular = true;
		connectivity = size1;
		alloc();

		for (int n = 0; n < size; n++) {
			int[] links = new int[size1];
			in[n] = links;
			kin[n] = size1;
			out[n] = links;
			kout[n] = size1;
			for (int i = 0; i < size1; i++) {
				links[i] = (i >= n) ? i + 1 : i;
			}
		}
		isValid = true;
	}
}
