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
 * Wheel geometry: ring lattice plus central hub node 0 connected to every rim
 * node.
 */
public class WheelGeometry extends AbstractGeometry {

	/**
	 * Create a wheel (cycle with hub) geometry tied to the given engine.
	 *
	 * @param engine EvoLudo pacemaker
	 */
	public WheelGeometry(EvoLudo engine) {
		super(engine);
		setType(GeometryType.WHEEL);
	}

	/**
	 * Generates a wheel geometry, i.e. a ring (periodic 1D lattice) with a hub in
	 * the middle that connects to all rim nodes (resembling spokes). The wheel is
	 * an undirected evolutionary amplifier albeit weaker than the star. Node
	 * {@code 0} is the hub.
	 *
	 * @see StarGeometry
	 */
	@Override
	public void init() {
		if (size <= 1)
			throw new IllegalStateException("wheel geometry requires at least two nodes");
		int size1 = size - 1;
		isRewired = false;
		isUndirected = true;
		isRegular = false;
		for (int i = 0; i < size1; i++) {
			addLinkAt(i + 1, (i - 1 + size1) % size1 + 1);
			addLinkAt(i + 1, (i + 1 + size1) % size1 + 1);
			addLinkAt(0, i + 1);
			addLinkAt(i + 1, 0);
		}
		isValid = true;
	}

	@Override
	protected boolean checkSettings() {
		connectivity = 4.0 * (size - 1) / size;
		return false;
	}
}
