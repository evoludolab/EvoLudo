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
import org.evoludo.util.CLOParser;

/**
 * Cubic lattice geometry (3D) with optional fixed boundaries.
 */
public class CubicGeometry extends AbstractLattice {

	/**
	 * Create a cubic-lattice geometry tied to the given engine.
	 *
	 * @param engine EvoLudo pacemaker
	 */
	public CubicGeometry(EvoLudo engine) {
		super(engine);
		setType(GeometryType.CUBE);
	}

	@Override
	public boolean parse(String arg) {
		String numeric = stripBoundary(arg);
		connectivity = CLOParser.parseInteger(numeric);
		if (connectivity != 1 && connectivity != 6) {
			connectivity = Math.max(6, connectivity);
		}
		return true;
	}

	/**
	 * Generates a cubic (3D) regular lattice. Supports von-Neumann style
	 * connectivity \(k=6\) as well as larger interaction ranges and optional
	 * fixed boundaries.
	 */
	@Override
	public void init() {
		if (size <= 0)
			throw new IllegalStateException("size must be set before initializing a cubic geometry");
		isRewired = false;
		isUndirected = true;
		isRegular = true;

		int l = (int) Math.floor(Math.pow(size, 1.0 / 3.0) + 0.5);
		int lz = l;
		if (size == 25000) {
			l = 50;
			lz = 10;
		}

		switch ((int) Math.rint(connectivity)) {
			case 1:
				initSelf(l, lz);
				break;
			case 6:
				initSixNeighbors(l, lz);
				break;
			default:
				initRange(l, lz);
		}
		isValid = true;
	}

	/**
	 * Connect each node exclusively to itself (used when connectivity is 1).
	 *
	 * @param l  side length of the lattice
	 * @param lz number of layers along the z-direction
	 */
	private void initSelf(int l, int lz) {
		int l2 = l * l;
		for (int k = 0; k < lz; k++) {
			int z = k * l2;
			for (int i = 0; i < l; i++) {
				int x = i * l;
				for (int j = 0; j < l; j++) {
					int aPlayer = z + x + j;
					addLinkAt(aPlayer, aPlayer);
				}
			}
		}
	}

	/**
	 * Populate the lattice with von-Neumann (6-neighbour) connectivity, respecting
	 * the configured boundary conditions.
	 *
	 * @param l  side length of the lattice
	 * @param lz number of layers along the z-direction
	 */
	private void initSixNeighbors(int l, int lz) {
		boolean interspecies = isInterspecies();
		int l2 = l * l;
		if (fixedBoundary) {
			for (int k = 0; k < lz; k++) {
				int z = k * l2;
				int up = (k >= lz - 1) ? -1 : z + l2;
				int down = (k > 0) ? z - l2 : -1;
				addSixNeighborsFixed(interspecies, z, up, down, l);
			}
		} else {
			for (int k = 0; k < lz; k++) {
				int z = k * l2;
				int up = ((k + 1) % lz) * l2;
				int down = ((k - 1 + lz) % lz) * l2;
				addSixNeighborsToroidal(interspecies, z, up, down, l);
			}
		}
		isRegular = !fixedBoundary;
	}

	/**
	 * Adds the six nearest-neighbors with fixed boundary conditions.
	 *
	 * @param interspecies whether self-links are permitted
	 * @param z            layer offset
	 * @param up           index offset of the layer above
	 * @param down         index offset of the layer below
	 * @param l            side length of the lattice
	 */
	private void addSixNeighborsFixed(boolean interspecies, int z, int up, int down, int l) {
		for (int i = 0; i < l; i++) {
			int x = i * l;
			int north = x - l;
			int south = (i < l - 1) ? x + l : -1;
			for (int j = 0; j < l; j++) {
				int east = (j < l - 1) ? j + 1 : -1;
				int west = j - 1;
				int aPlayer = z + x + j;
				if (interspecies)
					addLinkAt(aPlayer, aPlayer);
				addNeighbor(aPlayer, z, north, j);
				addNeighbor(aPlayer, z, x, east);
				addNeighbor(aPlayer, z, south, j);
				addNeighbor(aPlayer, z, x, west);
				addNeighbor(aPlayer, up, x, j);
				addNeighbor(aPlayer, down, x, j);
			}
		}
	}

	/**
	 * Adds a neighbor if all indices are valid (non-negative).
	 *
	 * @param aPlayer the focal node
	 * @param z       layer offset
	 * @param y       row offset within the layer
	 * @param x       column offset within the row
	 */
	private void addNeighbor(int aPlayer, int z, int y, int x) {
		if (z >= 0 && y >= 0 && x >= 0) {
			addLinkAt(aPlayer, z + y + x);
		}
	}

	/**
	 * Adds the six nearest-neighbors with toroidal boundary conditions.
	 *
	 * @param interspecies whether self-links are permitted
	 * @param z            layer offset
	 * @param up           index offset of the layer above
	 * @param down         index offset of the layer below
	 * @param l            side length of the lattice
	 */
	private void addSixNeighborsToroidal(boolean interspecies, int z, int up, int down, int l) {
		for (int i = 0; i < l; i++) {
			int x = i * l;
			int north = ((i - 1 + l) % l) * l;
			int south = ((i + 1) % l) * l;
			for (int j = 0; j < l; j++) {
				int east = (j + 1) % l;
				int west = (j - 1 + l) % l;
				int aPlayer = z + x + j;
				if (interspecies)
					addLinkAt(aPlayer, aPlayer);
				addLinkAt(aPlayer, z + north + j);
				addLinkAt(aPlayer, z + x + east);
				addLinkAt(aPlayer, z + south + j);
				addLinkAt(aPlayer, z + x + west);
				addLinkAt(aPlayer, up + x + j);
				addLinkAt(aPlayer, down + x + j);
			}
		}
	}

	/**
	 * Populate the lattice with a larger interaction range than the von-Neumann
	 * stencil, optionally with toroidal wrapping.
	 *
	 * @param l  side length of the lattice
	 * @param lz number of layers along the z-direction
	 */
	private void initRange(int l, int lz) {
		if (fixedBoundary)
			initFixed(l, lz);
		else
			initToroidal(l, lz);
		isRegular = !fixedBoundary;
	}

	/**
	 * Populate the lattice with a larger interaction range than the von-Neumann
	 * stencil, with fixed boundaries.
	 * 
	 * @param l  side length of the lattice
	 * @param lz number of layers along the z-direction
	 * 
	 * @see #initToroidal(int, int)
	 */
	private void initFixed(int l, int lz) {
		boolean interspecies = isInterspecies();
		int l2 = l * l;
		int range = Math.min(l / 2, Math.max(1, (int) (Math.pow(connectivity + 1.5, 1.0 / 3.0) / 2.0)));
		for (int k = 0; k < lz; k++) {
			int z = k * l2;
			for (int i = 0; i < l; i++) {
				int x = i * l;
				for (int j = 0; j < l; j++) {
					int aPlayer = z + x + j;
					int krmin = Math.max(0, k - range);
					int krmax = Math.min(lz - 1, k + range);
					int irmin = Math.max(0, i - range);
					int irmax = Math.min(l - 1, i + range);
					int jrmin = Math.max(0, j - range);
					int jrmax = Math.min(l - 1, j + range);
					for (int kr = krmin; kr <= krmax; kr++) {
						int zr = kr * l2;
						for (int ir = irmin; ir <= irmax; ir++) {
							int yr = ir * l;
							linkNeighboursAt(aPlayer, zr + yr, jrmin, jrmax, l, interspecies);
						}
					}
				}
			}
		}
	}

	/**
	 * Populate the lattice with a larger interaction range than the von-Neumann
	 * stencil, with toroidal wrapping.
	 * 
	 * @param l  side length of the lattice
	 * @param lz number of layers along the z-direction
	 * 
	 * @see #initFixed(int, int)
	 */
	private void initToroidal(int l, int lz) {
		boolean interspecies = isInterspecies();
		int l2 = l * l;
		int range = Math.min(l / 2, Math.max(1, (int) (Math.pow(connectivity + 1.5, 1.0 / 3.0) / 2.0)));
		for (int k = 0; k < lz; k++) {
			int z = k * l2;
			for (int i = 0; i < l; i++) {
				int x = i * l;
				for (int j = 0; j < l; j++) {
					int aPlayer = z + x + j;
					for (int kr = k - range; kr <= k + range; kr++) {
						int zr = ((kr + lz) % lz) * l2;
						for (int ir = i - range; ir <= i + range; ir++) {
							int yr = ((ir + l) % l) * l;
							linkNeighboursAt(aPlayer, zr + yr, j - range, j + range, l, interspecies);
						}
					}
				}
			}
		}
	}

	/**
	 * Links all neighbors in the specified range [min, max] around bOffset to
	 * aPlayer.
	 * 
	 * @param aPlayer      the player to link from
	 * @param bOffset      the base offset for the target players
	 * @param min          the minimum index offset
	 * @param max          the maximum index offset
	 * @param l            the side length of the lattice
	 * @param interspecies whether to allow self-links
	 */
	private void linkNeighboursAt(int aPlayer, int bOffset, int min, int max, int l, boolean interspecies) {
		for (int jr = min; jr <= max; jr++) {
			int bPlayer = bOffset + ((jr + l) % l);
			if (aPlayer == bPlayer && !interspecies)
				continue;
			addLinkAt(aPlayer, bPlayer);
		}
	}

	@Override
	protected boolean checkSettings() {
		boolean doReset = false;
		int range;
		if (size != 25000) {
			int side = Math.max((int) Math.floor(Math.pow(size, 1.0 / 3.0) + 0.5), 2);
			int side3 = side * side * side;
			if (setSize(side3)) {
				if (engine.getModule().cloNPopulation.isSet())
					warn("requires integer cube size - using " + size + "!");
				doReset = true;
			}
			range = Math.min(side / 2,
					Math.max(1, (int) (Math.pow(connectivity + 1.5, 1.0 / 3.0) / 2.0)));
		} else {
			range = Math.min(4, Math.max(1, (int) (Math.pow(connectivity + 1.5, 1.0 / 3.0) / 2.0)));
		}
		int count = (2 * range + 1) * (2 * range + 1) * (2 * range + 1) - 1;
		boolean invalid = (Math.abs(count - connectivity) > 1e-8 && Math.abs(6.0 - connectivity) > 1e-8
				&& Math.abs(1.0 - connectivity) > 1e-8)
				|| (Math.abs(1.0 - connectivity) < 1e-8 && !isInterspecies());
		if (invalid) {
			connectivity = count;
			if (connectivity >= size)
				connectivity = 6;
			warn("has invalid connectivity - using " + (int) connectivity + "!");
			doReset = true;
		}
		return doReset;
	}
}
