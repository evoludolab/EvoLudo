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

package org.evoludo.simulator.geometries;

import org.evoludo.simulator.EvoLudo;

/**
 * Square lattice with von Neumann neighbourhood (four nearest neighbours).
 */
public class VonNeumannGeometry extends SquareGeometry {

	/**
	 * Create a von Neumann square lattice geometry tied to the given engine.
	 *
	 * @param engine EvoLudo pacemaker
	 */
	public VonNeumannGeometry(EvoLudo engine) {
		super(engine);
		setType(GeometryType.SQUARE_NEUMANN);
	}

	@Override
	public boolean parse(String arg) {
		String numeric = stripBoundary(arg);
		warnIfConnectivityProvided(numeric);
		connectivity = 4;
		return true;
	}

	@Override
	public void init() {
		int side = prepareSquareLattice();
		boolean interspecies = isInterspecies();
		int fullside = side;
		int offset = 0;
		for (int i = 0; i < side; i++) {
			int x = i * fullside;
			int u = ((i - 1 + side) % side) * fullside;
			int d = ((i + 1) % side) * fullside;
			for (int j = 0; j < side; j++) {
				int r = (j + 1) % side;
				int l = (j - 1 + side) % side;
				int aPlayer = offset + x + j;
				if (interspecies)
					addLinkAt(aPlayer, aPlayer);
				addLinkAt(aPlayer, offset + u + j);
				addLinkAt(aPlayer, offset + x + r);
				addLinkAt(aPlayer, offset + d + j);
				addLinkAt(aPlayer, offset + x + l);
			}
		}
		if (fixedBoundary) {
			adjustBoundaries(side, fullside, offset, interspecies);
			isRegular = false;
		}
		isValid = true;
	}

	/**
	 * Adjust von Neumann neighbourhoods when fixed boundaries are requested.
	 *
	 * @param side         side length of the (sub) lattice
	 * @param fullside     global side length
	 * @param offset       index offset into the population
	 * @param interspecies {@code true} if self-links are required
	 */
	private void adjustBoundaries(int side, int fullside, int offset, boolean interspecies) {
		int aPlayer = offset;
		clearLinksFrom(aPlayer);
		if (interspecies)
			addLinkAt(aPlayer, aPlayer);
		addLinkAt(aPlayer, aPlayer + 1);
		addLinkAt(aPlayer, aPlayer + fullside);

		aPlayer = offset + side - 1;
		clearLinksFrom(aPlayer);
		if (interspecies)
			addLinkAt(aPlayer, aPlayer);
		addLinkAt(aPlayer, aPlayer - 1);
		addLinkAt(aPlayer, aPlayer + fullside);

		aPlayer = offset + (side - 1) * fullside;
		clearLinksFrom(aPlayer);
		if (interspecies)
			addLinkAt(aPlayer, aPlayer);
		addLinkAt(aPlayer, aPlayer + 1);
		addLinkAt(aPlayer, aPlayer - fullside);

		aPlayer = offset + (side - 1) * (fullside + 1);
		clearLinksFrom(aPlayer);
		if (interspecies)
			addLinkAt(aPlayer, aPlayer);
		addLinkAt(aPlayer, aPlayer - 1);
		addLinkAt(aPlayer, aPlayer - fullside);

		for (int i = 1; i < side - 1; i++) {
			aPlayer = offset + i;
			clearLinksFrom(aPlayer);
			if (interspecies)
				addLinkAt(aPlayer, aPlayer);
			addLinkAt(aPlayer, aPlayer - 1);
			addLinkAt(aPlayer, aPlayer + 1);
			addLinkAt(aPlayer, aPlayer + fullside);

			aPlayer = offset + (side - 1) * fullside + i;
			clearLinksFrom(aPlayer);
			if (interspecies)
				addLinkAt(aPlayer, aPlayer);
			addLinkAt(aPlayer, aPlayer - 1);
			addLinkAt(aPlayer, aPlayer + 1);
			addLinkAt(aPlayer, aPlayer - fullside);

			aPlayer = offset + fullside * i;
			clearLinksFrom(aPlayer);
			if (interspecies)
				addLinkAt(aPlayer, aPlayer);
			addLinkAt(aPlayer, aPlayer + 1);
			addLinkAt(aPlayer, aPlayer - fullside);
			addLinkAt(aPlayer, aPlayer + fullside);

			aPlayer = offset + fullside * i + side - 1;
			clearLinksFrom(aPlayer);
			if (interspecies)
				addLinkAt(aPlayer, aPlayer);
			addLinkAt(aPlayer, aPlayer - 1);
			addLinkAt(aPlayer, aPlayer - fullside);
			addLinkAt(aPlayer, aPlayer + fullside);
		}
	}

	@Override
	protected boolean checkSettings() {
		connectivity = 4;
		return ensureSquareSize(false);
	}
}
