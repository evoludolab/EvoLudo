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
 * Geometry implementation for the Tietze graph (a cubic 12-node graph).
 *
 * @see <a href="https://en.wikipedia.org/wiki/Tietze's_graph">Wikipedia:
 *      Tietze's graph</a>
 */
public class TietzeGeometry extends AbstractGeometry {

	/**
	 * Create a Tietze geometry tied to the given engine.
	 *
	 * @param engine EvoLudo pacemaker
	 */
	public TietzeGeometry(EvoLudo engine) {
		super(engine);
		setType(GeometryType.TIETZE);
	}

	/**
	 * Generates Tietze's graph, a cubic graph with \(12\) nodes and automorphisms
	 * corresponding to the symmetries of a hexagon.
	 *
	 * @see <a href="https://en.wikipedia.org/wiki/Tietze's_graph">Wikipedia:
	 *      Tietze's graph</a>
	 */
	@Override
	public void init() {
		isRewired = false;
		isUndirected = true;
		isRegular = true;

		for (int i = 1; i < size; i++)
			addEdgeAt(i, i - 1);
		addEdgeAt(0, 4);
		addEdgeAt(0, 8);
		addEdgeAt(1, 6);
		addEdgeAt(2, 10);
		addEdgeAt(3, 7);
		addEdgeAt(5, 11);
		addEdgeAt(9, 11);
	}

	@Override
	protected boolean checkSettings() {
		boolean doReset = enforceSize(12);
		connectivity = 3.0;
		return doReset;
	}
}
