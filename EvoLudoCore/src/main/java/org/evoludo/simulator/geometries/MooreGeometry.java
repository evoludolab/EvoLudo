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
 * Square lattice with Moore neighbourhood (first and second nearest
 * neighbours).
 */
public class MooreGeometry extends SquareGeometry {

	/**
	 * Create a Moore square lattice geometry tied to the given engine.
	 *
	 * @param engine EvoLudo pacemaker
	 */
	public MooreGeometry(EvoLudo engine) {
		super(engine);
		setType(GeometryType.SQUARE_MOORE);
	}

	@Override
	public boolean parse(String arg) {
		String numeric = stripBoundary(arg);
		warnIfConnectivityProvided(numeric);
		connectivity = 8;
		return true;
	}

	@Override
	public void init() {
		int side = prepareSquareLattice();
		initMoore(this, side, side, 0, fixedBoundary);
		isValid = true;
	}

	/**
	 * Initialize one hierarchical Moore square deme using population and deme
	 * sizes.
	 *
	 * @param geometry      geometry receiving the links
	 * @param popSize       total population size
	 * @param demeSize      number of individuals in the deme
	 * @param startIndex    index offset into the population
	 * @param fixedBoundary {@code true} for fixed boundary conditions
	 */
	public static void initHierarchicalMoore(AbstractGeometry geometry, int popSize, int demeSize, int startIndex,
			boolean fixedBoundary) {
		int side = (int) Math.sqrt(demeSize);
		int fullside = (int) Math.sqrt(popSize);
		initMoore(geometry, side, fullside, startIndex, fixedBoundary);
	}

	/**
	 * Initialize a Moore square lattice.
	 *
	 * @param geometry      geometry receiving the links
	 * @param side          side length of the (sub) lattice
	 * @param fullside      global side length
	 * @param offset        index offset into the population
	 * @param fixedBoundary {@code true} for fixed boundary conditions
	 */
	public static void initMoore(AbstractGeometry geometry, int side, int fullside, int offset,
			boolean fixedBoundary) {
		boolean interspecies = geometry.isInterspecies();
		for (int i = 0; i < side; i++) {
			int x = i * fullside;
			int u = ((i - 1 + side) % side) * fullside;
			int d = ((i + 1) % side) * fullside;
			for (int j = 0; j < side; j++) {
				int r = (j + 1) % side;
				int l = (j - 1 + side) % side;
				int aPlayer = offset + x + j;
				if (interspecies)
					geometry.addLinkAt(aPlayer, aPlayer);
				geometry.addLinkAt(aPlayer, offset + u + j);
				geometry.addLinkAt(aPlayer, offset + u + r);
				geometry.addLinkAt(aPlayer, offset + x + r);
				geometry.addLinkAt(aPlayer, offset + d + r);
				geometry.addLinkAt(aPlayer, offset + d + j);
				geometry.addLinkAt(aPlayer, offset + d + l);
				geometry.addLinkAt(aPlayer, offset + x + l);
				geometry.addLinkAt(aPlayer, offset + u + l);
			}
		}
		if (fixedBoundary) {
			adjustBoundaries(geometry, side, fullside, offset, interspecies);
			geometry.isRegular = false;
		}
	}

	/**
	 * Adjust Moore stencils along fixed boundaries.
	 *
	 * @param geometry     geometry receiving the links
	 * @param side         side length of the (sub) lattice
	 * @param fullside     global side length
	 * @param offset       index offset into the population
	 * @param interspecies {@code true} if self-links are required
	 */
	private static void adjustBoundaries(AbstractGeometry geometry, int side, int fullside, int offset,
			boolean interspecies) {
		int aPlayer = offset;
		geometry.clearLinksFrom(aPlayer);
		if (interspecies)
			geometry.addLinkAt(aPlayer, aPlayer);
		geometry.addLinkAt(aPlayer, aPlayer + 1);
		geometry.addLinkAt(aPlayer, aPlayer + fullside);
		geometry.addLinkAt(aPlayer, aPlayer + fullside + 1);

		aPlayer = offset + side - 1;
		geometry.clearLinksFrom(aPlayer);
		if (interspecies)
			geometry.addLinkAt(aPlayer, aPlayer);
		geometry.addLinkAt(aPlayer, aPlayer - 1);
		geometry.addLinkAt(aPlayer, aPlayer + fullside);
		geometry.addLinkAt(aPlayer, aPlayer + fullside - 1);

		aPlayer = offset + (side - 1) * fullside;
		geometry.clearLinksFrom(aPlayer);
		if (interspecies)
			geometry.addLinkAt(aPlayer, aPlayer);
		geometry.addLinkAt(aPlayer, aPlayer + 1);
		geometry.addLinkAt(aPlayer, aPlayer - fullside);
		geometry.addLinkAt(aPlayer, aPlayer - fullside + 1);

		aPlayer = offset + (side - 1) * (fullside + 1);
		geometry.clearLinksFrom(aPlayer);
		if (interspecies)
			geometry.addLinkAt(aPlayer, aPlayer);
		geometry.addLinkAt(aPlayer, aPlayer - 1);
		geometry.addLinkAt(aPlayer, aPlayer - fullside);
		geometry.addLinkAt(aPlayer, aPlayer - fullside - 1);

		for (int i = 1; i < side - 1; i++) {
			aPlayer = offset + i;
			geometry.clearLinksFrom(aPlayer);
			if (interspecies)
				geometry.addLinkAt(aPlayer, aPlayer);
			geometry.addLinkAt(aPlayer, aPlayer - 1);
			geometry.addLinkAt(aPlayer, aPlayer + 1);
			geometry.addLinkAt(aPlayer, aPlayer + fullside);
			geometry.addLinkAt(aPlayer, aPlayer + fullside - 1);
			geometry.addLinkAt(aPlayer, aPlayer + fullside + 1);

			aPlayer = offset + (side - 1) * fullside + i;
			geometry.clearLinksFrom(aPlayer);
			if (interspecies)
				geometry.addLinkAt(aPlayer, aPlayer);
			geometry.addLinkAt(aPlayer, aPlayer - 1);
			geometry.addLinkAt(aPlayer, aPlayer + 1);
			geometry.addLinkAt(aPlayer, aPlayer - fullside);
			geometry.addLinkAt(aPlayer, aPlayer - fullside - 1);
			geometry.addLinkAt(aPlayer, aPlayer - fullside + 1);

			aPlayer = offset + fullside * i;
			geometry.clearLinksFrom(aPlayer);
			if (interspecies)
				geometry.addLinkAt(aPlayer, aPlayer);
			geometry.addLinkAt(aPlayer, aPlayer + 1);
			geometry.addLinkAt(aPlayer, aPlayer - fullside);
			geometry.addLinkAt(aPlayer, aPlayer + fullside);
			geometry.addLinkAt(aPlayer, aPlayer - fullside + 1);
			geometry.addLinkAt(aPlayer, aPlayer + fullside + 1);

			aPlayer = offset + fullside * i + side - 1;
			geometry.clearLinksFrom(aPlayer);
			if (interspecies)
				geometry.addLinkAt(aPlayer, aPlayer);
			geometry.addLinkAt(aPlayer, aPlayer - 1);
			geometry.addLinkAt(aPlayer, aPlayer - fullside);
			geometry.addLinkAt(aPlayer, aPlayer + fullside);
			geometry.addLinkAt(aPlayer, aPlayer - fullside - 1);
			geometry.addLinkAt(aPlayer, aPlayer + fullside - 1);
		}
	}

	@Override
	protected boolean checkSettings() {
		connectivity = 8;
		return ensureSquareSize(false);
	}
}
