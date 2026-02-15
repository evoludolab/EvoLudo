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
 * Star geometry with node 0 as the hub connected to all leaves.
 */
public class StarGeometry extends AbstractGeometry {

	/**
	 * Create a star geometry tied to the given engine.
	 *
	 * @param engine EvoLudo pacemaker
	 */
	public StarGeometry(EvoLudo engine) {
		super(engine);
		setType(GeometryType.STAR);
	}

	@Override
	protected boolean checkSettings() {
		connectivity = 2.0 * (size - 1) / size;
		if (pRewire > 0.0) {
			warn("cannot rewire links - ignored!");
			pRewire = 0.0;
		}
		return false;
	}

	/**
	 * Generates a star geometry with a hub in the middle that is connected to all
	 * other nodes (leaves). The star structure is the simplest undirected
	 * evolutionary amplifier. Node {@code 0} acts as the hub.
	 */
	@Override
	public void init() {
		if (size <= 0)
			throw new IllegalStateException("size must be set before initializing a star geometry");
		isRewired = false;
		isUndirected = true;
		isRegular = false;

		for (int i = 1; i < size; i++) {
			addLinkAt(0, i);
			addLinkAt(i, 0);
		}
		isValid = true;
	}
}
