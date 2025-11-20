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
import org.evoludo.util.CLOParser;

/**
 * Honeycomb/hexagonal lattice geometry with optional fixed boundaries.
 */
public class HexagonalGeometry extends AbstractLattice {

	/**
	 * Create a hexagonal (honeycomb) geometry tied to the given engine.
	 *
	 * @param engine EvoLudo pacemaker
	 */
	public HexagonalGeometry(EvoLudo engine) {
		super(engine);
		setType(Type.HEXAGONAL);
	}

	@Override
	public boolean parse(String arg) {
		String numeric = stripBoundary(arg);
		connectivity = CLOParser.parseInteger(numeric);
		if (connectivity != 1 && connectivity != 6) {
			Logger logger = engine.getLogger();
			if (logger.isLoggable(Level.WARNING))
				logger.warning("invalid connectivity " + connectivity + " - using default k=6.");
			connectivity = 6;
		}
		return true;
	}

	/**
	 * Generates a hexagonal (honeycomb) regular lattice with degree \(6\) (or
	 * optional self-links when connectivity is set to {@code 1}). Fixed boundaries
	 * truncate neighbours; periodic boundaries wrap the lattice.
	 */
	@Override
	public void init() {
		if (size <= 0)
			throw new IllegalStateException("size must be set before initializing a honeycomb geometry");
		isRewired = false;
		isUndirected = true;
		isRegular = true;
		boolean interspecies = isInterspecies();
		alloc();

		int side = (int) Math.floor(Math.sqrt(size) + 0.5);
		if ((int) Math.rint(connectivity) == 1) {
			initK1();
			return;
		}
		for (int i = 0; i < side; i += 2) {
			int x = i * side;
			int u = ((i - 1 + side) % side) * side;
			boolean uNowrap = (i > 0);
			int d = ((i + 1) % side) * side;
			fillEvenIRows(side, interspecies, x, u, uNowrap, d);
			x = ((i + 1) % side) * side;
			u = i * side;
			d = ((i + 2) % side) * side;
			boolean dNowrap = (i < side - 2);
			fillOddIRows(side, interspecies, x, u, d, dNowrap);
		}
		isValid = true;
	}

	/**
	 * Initialize the lattice with self-links only (connectivity 1).
	 */
	private void initK1() {
		int side = (int) Math.floor(Math.sqrt(size) + 0.5);
		for (int i = 0; i < side; i++) {
			int x = i * side;
			for (int j = 0; j < side; j++) {
				int aPlayer = x + j;
				addLinkAt(aPlayer, aPlayer);
			}
		}
		isValid = true;
	}

	/**
	 * Populate even-index rows for the hexagonal lattice, respecting boundary
	 * conditions.
	 *
	 * @param side         side length of the lattice
	 * @param interspecies {@code true} if interspecific interactions allow
	 *                     self-links
	 * @param x            offset for the current row
	 * @param u            index offset for the row above
	 * @param uNowrap      {@code true} if wrapping upward is allowed
	 * @param d            index offset for the row below
	 */
	private void fillEvenIRows(int side, boolean interspecies, int x, int u, boolean uNowrap, int d) {
		for (int j = 0; j < side; j++) {
			int aPlayer = x + j;
			if (interspecies)
				addLinkAt(aPlayer, aPlayer);
			if (!fixedBoundary || uNowrap)
				addLinkAt(aPlayer, u + j);
			int r = (j + 1) % side;
			if (!fixedBoundary || r > 0)
				addLinkAt(aPlayer, x + r);
			addLinkAt(aPlayer, d + j);
			int l = (j - 1 + side) % side;
			if (!fixedBoundary || l < side - 1) {
				addLinkAt(aPlayer, d + l);
				addLinkAt(aPlayer, x + l);
			}
			if (!fixedBoundary || (uNowrap && l < side - 1))
				addLinkAt(aPlayer, u + l);
		}
	}

	/**
	 * Populate odd-index rows for the hexagonal lattice, respecting boundary
	 * conditions.
	 *
	 * @param side         side length of the lattice
	 * @param interspecies {@code true} if interspecific interactions allow
	 *                     self-links
	 * @param x            offset for the current row
	 * @param u            index offset for the row above
	 * @param d            index offset for the row below
	 * @param dNowrap      {@code true} if wrapping downward is allowed
	 */
	private void fillOddIRows(int side, boolean interspecies, int x, int u, int d, boolean dNowrap) {
		for (int j = 0; j < side; j++) {
			int aPlayer = x + j;
			if (interspecies)
				addLinkAt(aPlayer, aPlayer);
			addLinkAt(aPlayer, u + j);
			int r = (j + 1) % side;
			if (!fixedBoundary || r > 0) {
				addLinkAt(aPlayer, u + r);
				addLinkAt(aPlayer, x + r);
			}
			if (!fixedBoundary || (dNowrap && r > 0))
				addLinkAt(aPlayer, d + r);
			if (!fixedBoundary || dNowrap)
				addLinkAt(aPlayer, d + j);
			int l = (j - 1 + side) % side;
			if (!fixedBoundary || l < side - 1)
				addLinkAt(aPlayer, x + l);
			if (!fixedBoundary || l < side - 1)
				addLinkAt(aPlayer, x + l);
		}
	}

	@Override
	protected boolean checkSettings() {
		boolean doReset = false;
		int side = (int) Math.floor(Math.sqrt(size) + 0.5);
		int side2 = side * side;
		if (size != side2 || (side % 2) == 1) {
			side += side % 2;
			side2 = side * side;
			if (setSize(side2)) {
				if (engine.getModule().cloNPopulation.isSet())
					warn("requires even integer square size - using " + size + "!");
				doReset = true;
			}
		}
		if ((Math.abs(connectivity - 6) > 1e-8 && Math.abs(1.0 - connectivity) > 1e-8)
				|| (Math.abs(1.0 - connectivity) < 1e-8 && !isInterspecies())) {
			connectivity = 6;
			warn("requires connectivity 6!");
		}
		return doReset;
	}
}
