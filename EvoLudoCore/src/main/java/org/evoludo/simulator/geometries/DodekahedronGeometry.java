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
 * Geometry implementation for the dodecahedral graph.
 * 
 * @see <a href="https://en.wikipedia.org/wiki/Dodecahedral_graph">Wikipedia:
 *      Dodecahedral graph</a>
 */
public class DodekahedronGeometry extends AbstractGeometry {

	/**
	 * Create a dodecahedral geometry tied to the given engine.
	 *
	 * @param engine EvoLudo pacemaker
	 */
	public DodekahedronGeometry(EvoLudo engine) {
		super(engine);
		setType(GeometryType.DODEKAHEDRON);
	}

	/**
	 * Generates a dodecahedron graph: a cubic symmetric graph with \(20\) nodes.
	 *
	 * @see <a href="https://en.wikipedia.org/wiki/Dodecahedral_graph">Wikipedia:
	 *      Dodecahedral graph</a>
	 */
	@Override
	public void init() {
		isRewired = false;
		isUndirected = true;
		isRegular = true;
		for (int i = 0; i < size; i += 2) {
			addEdgeAt(i, (size + i - 2) % size);
			addEdgeAt(i, i + 1);
		}
		addEdgeAt(1, 5);
		addEdgeAt(3, 7);
		addEdgeAt(5, 9);
		addEdgeAt(7, 11);
		addEdgeAt(9, 13);
		addEdgeAt(11, 15);
		addEdgeAt(13, 17);
		addEdgeAt(15, 19);
		addEdgeAt(17, 1);
		addEdgeAt(19, 3);
	}

	@Override
	protected boolean checkSettings() {
		boolean doReset = enforceSize(20);
		connectivity = 3.0;
		return doReset;
	}
}
