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
 * Square lattice implementations covering von Neumann, second-neighbour, Moore
 * and general neighbourhoods.
 */
public class SquareGeometry extends AbstractLattice {

	/**
	 * The specific square lattice variant implemented by this instance.
	 */
	private final Type variant;

	/**
	 * Create a square lattice geometry tied to the given engine.
	 *
	 * @param engine  EvoLudo pacemaker
	 * @param variant square lattice variant to instantiate
	 */
	public SquareGeometry(EvoLudo engine, Type variant) {
		super(engine);
		this.variant = variant;
		setType(variant);
	}

	@Override
	public boolean parse(String arg) {
		String numeric = stripBoundary(arg);
		switch (variant) {
			case SQUARE:
				if (numeric == null || numeric.isEmpty())
					connectivity = 4;
				else
					connectivity = Integer.parseInt(numeric);
				break;
			case SQUARE_NEUMANN:
			case SQUARE_NEUMANN_2ND:
				warnIfConnectivityProvided(numeric);
				connectivity = 4;
				break;
			case SQUARE_MOORE:
				warnIfConnectivityProvided(numeric);
				connectivity = 8;
				break;
			default:
				throw new IllegalStateException("Unsupported square variant: " + variant);
		}
		return true;
	}

	/**
	 * Emit a warning if the user attempts to specify connectivity for a fixed
	 * stencil.
	 *
	 * @param numeric connectivity string passed on the CLI
	 */
	private void warnIfConnectivityProvided(String numeric) {
		if (numeric != null && !numeric.isEmpty()) {
			Logger log = engine.getLogger();
			if (log.isLoggable(Level.WARNING))
				log.warning("connectivity ignored for " + variant + " geometry");
		}
	}

	/**
	 * Generates square regular lattices (von Neumann, second-neighbour,
	 * Moore, or larger neighbourhood variants).
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
		if (size <= 0)
			throw new IllegalStateException("size must be set before initializing a square geometry");
		isRewired = false;
		isUndirected = true;
		isRegular = true;
		alloc();

		int side = (int) Math.floor(Math.sqrt(size) + 0.5);
		switch (variant) {
			case SQUARE_NEUMANN:
				initSquareVonNeumann(side, side, 0);
				break;
			case SQUARE_NEUMANN_2ND:
				initSquareVonNeumann2nd(side, side, 0);
				break;
			case SQUARE_MOORE:
				initSquareMoore(side, side, 0);
				break;
			case SQUARE:
				if ((int) Math.rint(connectivity) == 1)
					initSquareSelf(side, side, 0);
				else
					initSquare(side, side, 0);
				break;
			default:
				throw new IllegalStateException("Unsupported square variant: " + variant);
		}
		isValid = true;
	}

	/**
	 * Initialize a square lattice that only connects nodes to themselves (used
	 * when connectivity equals one).
	 *
	 * @param side     side length of the (sub) lattice
	 * @param fullside global side length
	 * @param offset   index offset into the population
	 */
	private void initSquareSelf(int side, int fullside, int offset) {
		for (int i = 0; i < side; i++) {
			int x = offset + i * fullside;
			for (int j = 0; j < side; j++) {
				int aPlayer = x + j;
				addLinkAt(aPlayer, aPlayer);
			}
		}
	}

	/**
	 * Initialize a square lattice with von Neumann connectivity.
	 *
	 * @param side     side length of the (sub) lattice
	 * @param fullside global side length
	 * @param offset   index offset into the population
	 */
	private void initSquareVonNeumann(int side, int fullside, int offset) {
		boolean interspecies = isInterspecies();
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
			adjustNeumannBoundaries(side, fullside, offset, interspecies);
			isRegular = false;
		}
	}

	/**
	 * Adjust von Neumann neighbourhoods when fixed boundaries are requested.
	 *
	 * @param side         side length of the (sub) lattice
	 * @param fullside     global side length
	 * @param offset       index offset into the population
	 * @param interspecies {@code true} if self-links are required
	 */
	private void adjustNeumannBoundaries(int side, int fullside, int offset, boolean interspecies) {
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

	/**
	 * Initialize a square lattice with second-neighbour von Neumann connectivity.
	 *
	 * @param side     side length of the (sub) lattice
	 * @param fullside global side length
	 * @param offset   index offset into the population
	 */
	private void initSquareVonNeumann2nd(int side, int fullside, int offset) {
		boolean interspecies = isInterspecies();
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
				addLinkAt(aPlayer, offset + u + r);
				addLinkAt(aPlayer, offset + u + l);
				addLinkAt(aPlayer, offset + d + r);
				addLinkAt(aPlayer, offset + d + l);
			}
		}
		if (fixedBoundary) {
			adjustNeumannSecondBoundaries(side, fullside, offset, interspecies);
			isRegular = false;
		}
	}

	/**
	 * Adjust second-neighbour von Neumann stencils along fixed boundaries.
	 *
	 * @param side         side length of the (sub) lattice
	 * @param fullside     global side length
	 * @param offset       index offset into the population
	 * @param interspecies {@code true} if self-links are required
	 */
	private void adjustNeumannSecondBoundaries(int side, int fullside, int offset, boolean interspecies) {
		int aPlayer = offset;
		clearLinksFrom(aPlayer);
		if (interspecies)
			addLinkAt(aPlayer, aPlayer);
		addLinkAt(aPlayer, aPlayer + fullside);
		addLinkAt(aPlayer, aPlayer + fullside + 1);

		aPlayer = offset + side - 1;
		clearLinksFrom(aPlayer);
		if (interspecies)
			addLinkAt(aPlayer, aPlayer);
		addLinkAt(aPlayer, aPlayer + fullside);
		addLinkAt(aPlayer, aPlayer + fullside - 1);

		aPlayer = offset + (side - 1) * fullside;
		clearLinksFrom(aPlayer);
		if (interspecies)
			addLinkAt(aPlayer, aPlayer);
		addLinkAt(aPlayer, aPlayer - fullside);
		addLinkAt(aPlayer, aPlayer - fullside + 1);

		aPlayer = offset + (side - 1) * (fullside + 1);
		clearLinksFrom(aPlayer);
		if (interspecies)
			addLinkAt(aPlayer, aPlayer);
		addLinkAt(aPlayer, aPlayer - fullside);
		addLinkAt(aPlayer, aPlayer - fullside - 1);

		for (int i = 1; i < side - 1; i++) {
			aPlayer = offset + i;
			clearLinksFrom(aPlayer);
			if (interspecies)
				addLinkAt(aPlayer, aPlayer);
			addLinkAt(aPlayer, aPlayer + fullside - 1);
			addLinkAt(aPlayer, aPlayer + fullside + 1);

			aPlayer = offset + (side - 1) * fullside + i;
			clearLinksFrom(aPlayer);
			if (interspecies)
				addLinkAt(aPlayer, aPlayer);
			addLinkAt(aPlayer, aPlayer - fullside - 1);
			addLinkAt(aPlayer, aPlayer - fullside + 1);

			aPlayer = offset + fullside * i;
			clearLinksFrom(aPlayer);
			if (interspecies)
				addLinkAt(aPlayer, aPlayer);
			addLinkAt(aPlayer, aPlayer - fullside + 1);
			addLinkAt(aPlayer, aPlayer + fullside + 1);

			aPlayer = offset + fullside * i + side - 1;
			clearLinksFrom(aPlayer);
			if (interspecies)
				addLinkAt(aPlayer, aPlayer);
			addLinkAt(aPlayer, aPlayer - fullside - 1);
			addLinkAt(aPlayer, aPlayer + fullside - 1);
		}
	}

	/**
	 * Initialize a square lattice with Moore connectivity.
	 *
	 * @param side     side length of the (sub) lattice
	 * @param fullside global side length
	 * @param offset   index offset into the population
	 */
	private void initSquareMoore(int side, int fullside, int offset) {
		boolean interspecies = isInterspecies();
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
				addLinkAt(aPlayer, offset + u + r);
				addLinkAt(aPlayer, offset + x + r);
				addLinkAt(aPlayer, offset + d + r);
				addLinkAt(aPlayer, offset + d + j);
				addLinkAt(aPlayer, offset + d + l);
				addLinkAt(aPlayer, offset + x + l);
				addLinkAt(aPlayer, offset + u + l);
			}
		}
		if (fixedBoundary) {
			adjustMooreBoundaries(side, fullside, offset, interspecies);
			isRegular = false;
		}
	}

	/**
	 * Adjust Moore stencils along fixed boundaries.
	 *
	 * @param side         side length of the (sub) lattice
	 * @param fullside     global side length
	 * @param offset       index offset into the population
	 * @param interspecies {@code true} if self-links are required
	 */
	private void adjustMooreBoundaries(int side, int fullside, int offset, boolean interspecies) {
		int aPlayer = offset;
		clearLinksFrom(aPlayer);
		if (interspecies)
			addLinkAt(aPlayer, aPlayer);
		addLinkAt(aPlayer, aPlayer + 1);
		addLinkAt(aPlayer, aPlayer + fullside);
		addLinkAt(aPlayer, aPlayer + fullside + 1);

		aPlayer = offset + side - 1;
		clearLinksFrom(aPlayer);
		if (interspecies)
			addLinkAt(aPlayer, aPlayer);
		addLinkAt(aPlayer, aPlayer - 1);
		addLinkAt(aPlayer, aPlayer + fullside);
		addLinkAt(aPlayer, aPlayer + fullside - 1);

		aPlayer = offset + (side - 1) * fullside;
		clearLinksFrom(aPlayer);
		if (interspecies)
			addLinkAt(aPlayer, aPlayer);
		addLinkAt(aPlayer, aPlayer + 1);
		addLinkAt(aPlayer, aPlayer - fullside);
		addLinkAt(aPlayer, aPlayer - fullside + 1);

		aPlayer = offset + (side - 1) * (fullside + 1);
		clearLinksFrom(aPlayer);
		if (interspecies)
			addLinkAt(aPlayer, aPlayer);
		addLinkAt(aPlayer, aPlayer - 1);
		addLinkAt(aPlayer, aPlayer - fullside);
		addLinkAt(aPlayer, aPlayer - fullside - 1);

		for (int i = 1; i < side - 1; i++) {
			aPlayer = offset + i;
			clearLinksFrom(aPlayer);
			if (interspecies)
				addLinkAt(aPlayer, aPlayer);
			addLinkAt(aPlayer, aPlayer - 1);
			addLinkAt(aPlayer, aPlayer + 1);
			addLinkAt(aPlayer, aPlayer + fullside);
			addLinkAt(aPlayer, aPlayer + fullside - 1);
			addLinkAt(aPlayer, aPlayer + fullside + 1);

			aPlayer = offset + (side - 1) * fullside + i;
			clearLinksFrom(aPlayer);
			if (interspecies)
				addLinkAt(aPlayer, aPlayer);
			addLinkAt(aPlayer, aPlayer - 1);
			addLinkAt(aPlayer, aPlayer + 1);
			addLinkAt(aPlayer, aPlayer - fullside);
			addLinkAt(aPlayer, aPlayer - fullside - 1);
			addLinkAt(aPlayer, aPlayer - fullside + 1);

			aPlayer = offset + fullside * i;
			clearLinksFrom(aPlayer);
			if (interspecies)
				addLinkAt(aPlayer, aPlayer);
			addLinkAt(aPlayer, aPlayer + 1);
			addLinkAt(aPlayer, aPlayer - fullside);
			addLinkAt(aPlayer, aPlayer + fullside);
			addLinkAt(aPlayer, aPlayer - fullside + 1);
			addLinkAt(aPlayer, aPlayer + fullside + 1);

			aPlayer = offset + fullside * i + side - 1;
			clearLinksFrom(aPlayer);
			if (interspecies)
				addLinkAt(aPlayer, aPlayer);
			addLinkAt(aPlayer, aPlayer - 1);
			addLinkAt(aPlayer, aPlayer - fullside);
			addLinkAt(aPlayer, aPlayer + fullside);
			addLinkAt(aPlayer, aPlayer - fullside - 1);
			addLinkAt(aPlayer, aPlayer + fullside - 1);
		}
	}

	/**
	 * Initialize a square lattice with arbitrary (odd) neighbourhood sizes.
	 *
	 * @param side     side length of the (sub) lattice
	 * @param fullside global side length
	 * @param offset   index offset into the population
	 */
	private void initSquare(int side, int fullside, int offset) {
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
			adjustSquareBoundaries(range, side, fullside, offset, interspecies);
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
	private void adjustSquareBoundaries(int range, int side, int fullside, int offset, boolean interspecies) {
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

	@Override
	protected boolean checkSettings() {
		boolean doReset = false;
		if (variant == Type.SQUARE_MOORE)
			connectivity = 8;
		if (variant == Type.SQUARE_NEUMANN || variant == Type.SQUARE_NEUMANN_2ND)
			connectivity = Math.max(connectivity, 4);
		int side = (int) Math.floor(Math.sqrt(size) + 0.5);
		if (variant == Type.SQUARE_NEUMANN_2ND)
			side = (side + 1) / 2 * 2;
		int side2 = side * side;
		if (setSize(side2)) {
			if (engine.getModule().cloNPopulation.isSet())
				warn("requires even integer square size - using " + size + "!");
			doReset = true;
		}
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
