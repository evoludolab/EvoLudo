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
 * Geometry implementation for the Franklin graph (a 12-node cubic cage).
 * 
 * @see <a href="https://en.wikipedia.org/wiki/Franklin_graph">Wikipedia:
 *      Franklin graph</a>
 */
public class FranklinGeometry extends AbstractGeometry {

	/**
	 * Create a Franklin geometry tied to the given engine.
	 *
	 * @param engine EvoLudo pacemaker
	 */
	public FranklinGeometry(EvoLudo engine) {
		super(engine);
		setType(GeometryType.FRANKLIN);
	}

	/**
	 * Generates the Franklin graph, a cubic cage with \(12\) nodes discovered by
	 * Philip Franklin.
	 *
	 * @see <a href="https://en.wikipedia.org/wiki/Franklin_graph">Wikipedia:
	 *      Franklin graph</a>
	 */
	@Override
	public void init() {
		isRewired = false;
		isUndirected = true;
		isRegular = true;

		for (int i = 1; i < size; i++)
			addEdgeAt(i, i - 1);
		addEdgeAt(0, size - 1);
		addEdgeAt(0, 7);
		addEdgeAt(1, 6);
		addEdgeAt(2, 9);
		addEdgeAt(3, 8);
		addEdgeAt(4, 11);
		addEdgeAt(5, 10);
	}

	@Override
	protected boolean checkSettings() {
		boolean doReset = enforceSize(12);
		connectivity = 3.0;
		return doReset;
	}
}
