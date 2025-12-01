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

package org.evoludo.simulator.geom;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.evoludo.simulator.EvoLudo;

/**
 * Square lattice with arbitrary neighbourhood sizes. Provides shared helpers
 * for square-based variants.
 */
public class SquareGeometry extends AbstractLattice {

	/**
	 * Create a square lattice geometry tied to the given engine.
	 *
	 * @param engine EvoLudo pacemaker
	 */
	public SquareGeometry(EvoLudo engine) {
		super(engine);
		setType(GeometryType.SQUARE);
	}

	@Override
	public boolean parse(String arg) {
		String numeric = stripBoundary(arg);
		if (numeric == null || numeric.isEmpty())
			connectivity = 4;
		else
			connectivity = Integer.parseInt(numeric);
		return true;
	}

	/**
	 * Emit a warning if the user attempts to specify connectivity for a fixed
	 * stencil.
	 *
	 * @param numeric connectivity string passed on the CLI
	 */
	protected void warnIfConnectivityProvided(String numeric) {
		if (numeric != null && !numeric.isEmpty()) {
			Logger log = engine.getLogger();
			if (log.isLoggable(Level.WARNING))
				log.warning("connectivity ignored for " + getType() + " geometry");
		}
	}

	/**
	 * Generates square regular lattices with arbitrary neighbourhood sizes.
	 * Variant-specific initializers for standard stencils are available to
	 * subclasses.
	 *
	 * <h3>Requirements/notes:</h3>
	 * <ol>
	 * <li>Population size must be a perfect square \(N=n^2\).
	 * <li>Admissible connectivities are \(4\) (von Neumann) or
	 * \((2m+1)^2-1\) for Moore-type stencils.
	 * <li>Inter-species interactions add the focal node as a neighbour and allow
	 * connectivity \(1\).
	 * <li>Boundaries are periodic by default but can be fixed.</li>
	 * </ol>
	 */
	@Override
	public void init() {
		int side = prepareSquareLattice();
		if ((int) Math.rint(connectivity) == 1)
			initSquareSelf(side, side, 0);
		else
			initSquare(side, side, 0);
		isValid = true;
	}

	/**
	 * Common setup for square geometries.
	 *
	 * @return the computed side length of the lattice
	 */
	protected int prepareSquareLattice() {
		if (size <= 0)
			throw new IllegalStateException("size must be set before initializing a square geometry");
		isRewired = false;
		isUndirected = true;
		isRegular = true;
		return (int) Math.floor(Math.sqrt(size) + 0.5);
	}

	/**
	 * Initialize a square lattice that only connects nodes to themselves (used
	 * when connectivity equals one).
	 *
	 * @param side     side length of the (sub) lattice
	 * @param fullside global side length
	 * @param offset   index offset into the population
	 */
	protected void initSquareSelf(int side, int fullside, int offset) {
		for (int i = 0; i < side; i++) {
			int x = offset + i * fullside;
			for (int j = 0; j < side; j++) {
				int aPlayer = x + j;
				addLinkAt(aPlayer, aPlayer);
			}
		}
	}

	/**
	 * Initialize a square lattice with arbitrary (odd) neighbourhood sizes.
	 *
	 * @param side     side length of the (sub) lattice
	 * @param fullside global side length
	 * @param offset   index offset into the population
	 */
	protected void initSquare(int side, int fullside, int offset) {
		boolean interspecies = isInterspecies();
		int range = Math.min(side / 2, Math.max(1, (int) (Math.sqrt(connectivity + 1.5) / 2.0)));

		for (int i = 0; i < side; i++) {
			int x = i * fullside;
			for (int j = 0; j < side; j++) {
				int aPlayer = offset + x + j;
				for (int u = i - range; u <= i + range; u++) {
					int y = offset + ((u + side) % side) * fullside;
					for (int v = j - range; v <= j + range; v++) {
						int bPlayer = y + (v + side) % side;
						if (aPlayer == bPlayer && !interspecies)
							continue;
						addLinkAt(aPlayer, bPlayer);
					}
				}
			}
		}
		if (fixedBoundary) {
			adjustBoundaries(range, side, fullside, offset, interspecies);
			isRegular = false;
		}
	}

	/**
	 * Adjust arbitrary-range neighbourhoods along fixed boundaries.
	 *
	 * @param range        interaction range
	 * @param side         side length of the (sub) lattice
	 * @param fullside     global side length
	 * @param offset       index offset into the population
	 * @param interspecies {@code true} if self-links are required
	 */
	private void adjustBoundaries(int range, int side, int fullside, int offset, boolean interspecies) {
		int aPlayer;
		int bPlayer;
		// top left
		aPlayer = offset;
		clearLinksFrom(aPlayer);
		for (int u = 0; u <= range; u++) {
			int r = aPlayer + u * fullside;
			for (int v = 0; v <= range; v++) {
				bPlayer = r + v;
				if (aPlayer == bPlayer && !interspecies)
					continue;
				addLinkAt(aPlayer, bPlayer);
			}
		}
		// top right
		aPlayer = offset + side - 1;
		clearLinksFrom(aPlayer);
		for (int u = 0; u <= range; u++) {
			int r = aPlayer + u * fullside;
			for (int v = -range; v <= 0; v++) {
				bPlayer = r + v;
				if (aPlayer == bPlayer && !interspecies)
					continue;
				addLinkAt(aPlayer, bPlayer);
			}
		}
		// bottom left
		aPlayer = offset + (side - 1) * fullside;
		clearLinksFrom(aPlayer);
		for (int u = -range; u <= 0; u++) {
			int r = aPlayer + u * fullside;
			for (int v = 0; v <= range; v++) {
				bPlayer = r + v;
				if (aPlayer == bPlayer && !interspecies)
					continue;
				addLinkAt(aPlayer, bPlayer);
			}
		}
		// bottom right
		aPlayer = offset + (side - 1) * (fullside + 1);
		clearLinksFrom(aPlayer);
		for (int u = -range; u <= 0; u++) {
			int r = aPlayer + u * fullside;
			for (int v = -range; v <= 0; v++) {
				bPlayer = r + v;
				if (aPlayer == bPlayer && !interspecies)
					continue;
				addLinkAt(aPlayer, bPlayer);
			}
		}
		for (int i = 1; i < side - 1; i++) {
			// top edge
			int row = 0;
			int col = i;
			aPlayer = offset + row * fullside + col;
			clearLinksFrom(aPlayer);
			for (int u = row; u <= row + range; u++) {
				int r = offset + u * fullside;
				for (int v = col - range; v <= col + range; v++) {
					bPlayer = r + (v + side) % side;
					if (aPlayer == bPlayer && !interspecies)
						continue;
					addLinkAt(aPlayer, bPlayer);
				}
			}
			// bottom edge
			row = side - 1;
			col = i;
			aPlayer = offset + row * fullside + col;
			clearLinksFrom(aPlayer);
			for (int u = row - range; u <= row; u++) {
				int r = offset + u * fullside;
				for (int v = col - range; v <= col + range; v++) {
					bPlayer = r + (v + side) % side;
					if (aPlayer == bPlayer && !interspecies)
						continue;
					addLinkAt(aPlayer, bPlayer);
				}
			}
			// left edge
			row = i;
			col = 0;
			aPlayer = offset + row * fullside + col;
			clearLinksFrom(aPlayer);
			for (int u = row - range; u <= row + range; u++) {
				int r = offset + ((u + side) % side) * fullside;
				for (int v = col; v <= col + range; v++) {
					bPlayer = r + v;
					if (aPlayer == bPlayer && !interspecies)
						continue;
					addLinkAt(aPlayer, bPlayer);
				}
			}
			// right edge
			row = i;
			col = side - 1;
			aPlayer = offset + row * fullside + col;
			clearLinksFrom(aPlayer);
			for (int u = row - range; u <= row + range; u++) {
				int r = offset + ((u + side) % side) * fullside;
				for (int v = col - range; v <= col; v++) {
					bPlayer = r + v;
					if (aPlayer == bPlayer && !interspecies)
						continue;
					addLinkAt(aPlayer, bPlayer);
				}
			}
		}
		isRegular = false;
	}

	/**
	 * Enforce square lattice size and optionally even side lengths.
	 *
	 * @param requireEvenSide {@code true} if the side length must be even
	 * @return {@code true} if the size had to be adjusted
	 */
	protected boolean ensureSquareSize(boolean requireEvenSide) {
		boolean doReset = false;
		int side = (int) Math.floor(Math.sqrt(size) + 0.5);
		if (requireEvenSide)
			side = (side + 1) / 2 * 2;
		int side2 = side * side;
		if (setSize(side2)) {
			if (engine.getModule().cloNPopulation.isSet()) {
				String parity = requireEvenSide ? "even integer " : "integer ";
				warn("requires " + parity + "square size - using " + size + "!");
			}
			doReset = true;
		}
		return doReset;
	}

	@Override
	protected boolean checkSettings() {
		boolean doReset = ensureSquareSize(false);
		int side = (int) Math.floor(Math.sqrt(size) + 0.5);
		int range = Math.min(side / 2, Math.max(1, (int) (Math.sqrt(connectivity + 1.5) / 2.0)));
		int count = (2 * range + 1) * (2 * range + 1) - 1;
		boolean invalid = (Math.abs(count - connectivity) > 1e-8 && Math.abs(4.0 - connectivity) > 1e-8
				&& Math.abs(1.0 - connectivity) > 1e-8)
				|| (Math.abs(1.0 - connectivity) < 1e-8 && !isInterspecies());
		if (invalid) {
			connectivity = count;
			if (connectivity >= size)
				connectivity = 4;
			warn("has invalid connectivity - using " + (int) connectivity + "!");
			doReset = true;
		}
		return doReset;
	}
}
