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
 * Frucht graph implementation.
 *
 * @see <a href="https://en.wikipedia.org/wiki/Frucht_graph">Wikipedia: Frucht
 *      graph</a>
 */
public class FruchtGeometry extends AbstractGeometry {

	/**
	 * Create a Frucht-graph geometry coordinated by the supplied engine.
	 *
	 * @param engine EvoLudo pacemaker
	 */
	public FruchtGeometry(EvoLudo engine) {
		super(engine);
		setType(GeometryType.FRUCHT);
	}

	@Override
	protected boolean checkSettings() {
		boolean doReset = enforceSize(12);
		connectivity = 3.0;
		return doReset;
	}

	/**
	 * Generates the Frucht graph, the smallest regular graph without any
	 * symmetries (a cubic graph with \(12\) nodes and no automorphisms apart from
	 * the identity).
	 *
	 * @see <a href="https://en.wikipedia.org/wiki/Frucht_graph">Wikipedia: Frucht
	 *      graph</a>
	 */
	@Override
	public void init() {
		if (size != 12)
			throw new IllegalStateException("Frucht graph requires size 12");
		isRewired = false;
		isUndirected = true;
		isRegular = true;
		alloc();

		addEdgeAt(0, 1);
		addEdgeAt(1, 2);
		addEdgeAt(2, 3);
		addEdgeAt(3, 4);
		addEdgeAt(4, 5);
		addEdgeAt(5, 6);
		addEdgeAt(6, 0);
		addEdgeAt(0, 7);
		addEdgeAt(1, 7);
		addEdgeAt(2, 8);
		addEdgeAt(3, 8);
		addEdgeAt(4, 9);
		addEdgeAt(5, 9);
		addEdgeAt(6, 10);
		addEdgeAt(7, 10);
		addEdgeAt(8, 11);
		addEdgeAt(9, 11);
		addEdgeAt(10, 11);
		isValid = true;
	}
}
