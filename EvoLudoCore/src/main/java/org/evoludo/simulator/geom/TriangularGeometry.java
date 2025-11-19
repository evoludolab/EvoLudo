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

import org.evoludo.simulator.EvoLudo;
import org.evoludo.simulator.modules.Module;

/**
 * Triangular (hexagonal) lattice geometry supporting periodic or fixed
 * boundaries.
 */
public class TriangularGeometry extends AbstractLattice {

	public TriangularGeometry(EvoLudo engine) {
		super(engine);
		setType(Type.TRIANGULAR);
	}

	public TriangularGeometry(EvoLudo engine, Module<?> module) {
		super(engine, module);
		setType(Type.TRIANGULAR);
	}

	public TriangularGeometry(EvoLudo engine, Module<?> popModule, Module<?> oppModule) {
		super(engine, popModule, oppModule);
		setType(Type.TRIANGULAR);
	}

	/**
	 * Configure the triangular geometry. Supports boundary flags and optional
	 * explicit connectivity (defaults to 3).
	 */
	public void parse(String arg) {
		String numeric = stripBoundary(arg);
		if (numeric == null || numeric.isEmpty()) {
			connectivity = 3;
		} else {
			connectivity = Integer.parseInt(numeric);
		}
	}

	@Override
	public void init() {
		if (size <= 0)
			throw new IllegalStateException("size must be set before initializing a triangular geometry");
		isRewired = false;
		isUndirected = true;
		isRegular = true;
		boolean interspecies = isInterspecies();
		alloc();

		int side = (int) Math.floor(Math.sqrt(size) + 0.5);
		switch ((int) Math.rint(connectivity)) {
			case 1:
				for (int i = 0; i < side; i++) {
					int x = i * side;
					for (int j = 0; j < side; j++) {
						int aPlayer = x + j;
						addLinkAt(aPlayer, aPlayer);
					}
				}
				break;
			default:
				for (int i = 0; i < side; i += 2) {
					int x = i * side;
					int u = ((i - 1 + side) % side) * side;
					boolean uNowrap = (i > 0);
					int d = ((i + 1) % side) * side;
					for (int j = 0; j < side; j += 2) {
						int aPlayer = x + j;
						if (interspecies)
							addLinkAt(aPlayer, aPlayer);
						int r = j + 1;
						addLinkAt(aPlayer, x + r);
						int l = (j - 1 + side) % side;
						if (!fixedBoundary || l < side - 1)
							addLinkAt(aPlayer, x + l);
						addLinkAt(aPlayer, d + j);
						aPlayer = x + j + 1;
						if (interspecies)
							addLinkAt(aPlayer, aPlayer);
						r = (r + 1) % side;
						if (!fixedBoundary || r > 0)
							addLinkAt(aPlayer, x + r);
						l = j;
						addLinkAt(aPlayer, x + l);
						if (!fixedBoundary || uNowrap)
							addLinkAt(aPlayer, u + j + 1);
					}
					x = d;
					u = i * side;
					d = ((i + 2) % side) * side;
					boolean dNowrap = (i < side - 2);
					for (int j = 0; j < side; j += 2) {
						int aPlayer = x + j;
						if (interspecies)
							addLinkAt(aPlayer, aPlayer);
						int r = j + 1;
						addLinkAt(aPlayer, x + r);
						int l = (j - 1 + side) % side;
						if (!fixedBoundary || l < side - 1)
							addLinkAt(aPlayer, x + l);
						addLinkAt(aPlayer, u + j);
						aPlayer = x + j + 1;
						if (interspecies)
							addLinkAt(aPlayer, aPlayer);
						r = (r + 1) % side;
						if (!fixedBoundary || r > 0)
							addLinkAt(aPlayer, x + r);
						l = j;
						addLinkAt(aPlayer, x + l);
						if (!fixedBoundary || dNowrap)
							addLinkAt(aPlayer, d + j + 1);
					}
				}
		}
		isValid = true;
	}
}
