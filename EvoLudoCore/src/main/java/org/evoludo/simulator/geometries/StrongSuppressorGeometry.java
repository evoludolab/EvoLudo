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
 * Geometry implementation for the strong suppressor graphs of Giakkoupis
 * (2016).
 * 
 * @see <a href="https://doi.org/10.48550/arXiv.1611.01585">Giakkoupis (2016)
 *      Amplifiers and Suppressors of Selection...</a>
 */
public class StrongSuppressorGeometry extends AbstractGeometry {

	/**
	 * Create a strong suppressor geometry tied to the given engine.
	 *
	 * @param engine EvoLudo pacemaker
	 */
	public StrongSuppressorGeometry(EvoLudo engine) {
		super(engine);
		setType(GeometryType.STRONG_SUPPRESSOR);
	}

	/**
	 * Generates a strong undirected suppressor of selection. Population size obeys
	 * \(N=n^2(1+n(1+n))=n^2+n^3+n^4\) for integer \(n\) with three node types
	 * \(U=n^3\), \(V=n^4\) and \(W=n^2\). Each node in \(V\) connects to one node
	 * in \(U\) and to all nodes in \(W\).
	 *
	 * @see <a href="https://doi.org/10.48550/arXiv.1611.01585">Giakkoupis (2016)
	 *      Amplifiers and Suppressors of Selection...</a>
	 */
	@Override
	public void init() {
		isRewired = false;
		isUndirected = true;
		isRegular = false;

		int unit = (int) Math.floor(Math.pow(size, 0.25));
		int v0 = 0;
		int vn = (int) Math.pow(unit, 4);
		int w0 = vn;
		int wn = vn + unit * unit;
		int u0 = wn;
		for (int v = v0; v < vn; v++) {
			int u = u0 + (v - v0) / unit;
			addEdgeAt(v, u);
			for (int w = w0; w < wn; w++)
				addEdgeAt(v, w);
		}
	}

	@Override
	protected boolean checkSettings() {
		int unit = (int) Math.floor(Math.pow(size, 0.25));
		int required = unit * unit * (1 + unit * (1 + unit));
		return enforceSize(required);
	}
}
