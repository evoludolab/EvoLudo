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
 * Geometry implementation for the Desargues (Truncated Petersen) graph.
 * 
 * @see <a href="https://en.wikipedia.org/wiki/Desargues_graph">Wikipedia:
 *      Desargues graph</a>
 */
public class DesarguesGeometry extends AbstractGeometry {

	/**
	 * Create a Desargues geometry tied to the given engine.
	 *
	 * @param engine EvoLudo pacemaker
	 */
	public DesarguesGeometry(EvoLudo engine) {
		super(engine);
		setType(GeometryType.DESARGUES);
	}

	/**
	 * Generates the Desargues graph (also known as the Truncated Petersen graph),
	 * a symmetric cubic graph with \(20\) nodes.
	 *
	 * @see <a href="https://en.wikipedia.org/wiki/Desargues_graph">Wikipedia:
	 *      Desargues graph</a>
	 */
	@Override
	public void init() {
		isRewired = false;
		isUndirected = true;
		isRegular = true;
		for (int i = 0; i < size; i++)
			addEdgeAt(i, (size + i - 1) % size);
		addEdgeAt(0, 9);
		addEdgeAt(1, 12);
		addEdgeAt(2, 7);
		addEdgeAt(3, 18);
		addEdgeAt(4, 13);
		addEdgeAt(5, 16);
		addEdgeAt(6, 11);
		addEdgeAt(8, 17);
		addEdgeAt(10, 15);
		addEdgeAt(14, 19);
	}

	@Override
	protected boolean checkSettings() {
		boolean doReset = enforceSize(20);
		connectivity = 3.0;
		return doReset;
	}
}
