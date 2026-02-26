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

package org.evoludo.simulator.geometries;

import org.evoludo.simulator.EvoLudo;

/**
 * Geometry implementation for the Heawood graph (14-node cubic symmetric
 * graph).
 *
 * @see <a href="https://en.wikipedia.org/wiki/Heawood_graph">Wikipedia:
 *      Heawood graph</a>
 */
public class HeawoodGeometry extends AbstractGeometry {

	/**
	 * Create a Heawood geometry tied to the given engine.
	 *
	 * @param engine EvoLudo pacemaker
	 */
	public HeawoodGeometry(EvoLudo engine) {
		super(engine);
		setType(GeometryType.HEAWOOD);
	}

	/**
	 * Generates the Heawood graph, a cubic symmetric graph with \(14\) nodes that
	 * is the point-line incidence graph of the Fano plane.
	 *
	 * @see <a href="https://en.wikipedia.org/wiki/Heawood_graph">Wikipedia:
	 *      Heawood graph</a>
	 */
	@Override
	public void init() {
		isRewired = false;
		isUndirected = true;
		isRegular = true;

		int size = getSize();
		for (int i = 1; i < size; i++)
			addEdgeAt(i, i - 1);
		addEdgeAt(0, size - 1);
		addEdgeAt(0, 5);
		addEdgeAt(2, 7);
		addEdgeAt(4, 9);
		addEdgeAt(6, 11);
		addEdgeAt(8, 13);
		addEdgeAt(10, 1);
		addEdgeAt(12, 3);
	}

	@Override
	protected boolean checkSettings() {
		boolean doReset = enforceSize(14);
		connectivity = 3.0;
		return doReset;
	}
}
