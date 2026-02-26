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
 * Geometry implementation for the icosahedral graph (12 nodes, degree 5).
 */
public class IcosahedronGeometry extends AbstractGeometry {

	/**
	 * Create an icosahedral geometry tied to the given engine.
	 *
	 * @param engine EvoLudo pacemaker
	 */
	public IcosahedronGeometry(EvoLudo engine) {
		super(engine);
		setType(GeometryType.ICOSAHEDRON);
	}

	/**
	 * Generates an icosahedron graph: a symmetric graph with \(12\) nodes and
	 * degree \(5\).
	 *
	 * @see <a href="https://en.wikipedia.org/wiki/Icosahedron_graph">Wikipedia:
	 *      Icosahedron graph</a>
	 */
	@Override
	public void init() {
		isRewired = false;
		isUndirected = true;
		isRegular = true;

		for (int i = 1; i < size; i++)
			addEdgeAt(i, i - 1);
		addEdgeAt(0, 4);
		addEdgeAt(0, 5);
		addEdgeAt(0, 6);
		addEdgeAt(1, 6);
		addEdgeAt(1, 7);
		addEdgeAt(1, 8);
		addEdgeAt(2, 0);
		addEdgeAt(2, 4);
		addEdgeAt(2, 8);
		addEdgeAt(3, 8);
		addEdgeAt(3, 9);
		addEdgeAt(3, 10);
		addEdgeAt(4, 10);
		addEdgeAt(5, 10);
		addEdgeAt(5, 11);
		addEdgeAt(6, 11);
		addEdgeAt(7, 9);
		addEdgeAt(7, 11);
		addEdgeAt(9, 11);
	}

	@Override
	protected boolean checkSettings() {
		boolean doReset = enforceSize(12);
		connectivity = 5.0;
		return doReset;
	}
}
