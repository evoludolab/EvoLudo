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
import org.evoludo.util.CLOParser;

/**
 * Cubic lattice geometry (3D) with optional fixed boundaries.
 */
public class CubicGeometry extends AbstractLattice {

	public CubicGeometry(EvoLudo engine) {
		super(engine);
		setType(Type.CUBE);
	}

	public CubicGeometry(EvoLudo engine, Module<?> module) {
		super(engine, module);
		setType(Type.CUBE);
	}

	public CubicGeometry(EvoLudo engine, Module<?> popModule, Module<?> oppModule) {
		super(engine, popModule, oppModule);
		setType(Type.CUBE);
	}

	public boolean parse(String arg) {
		String numeric = stripBoundary(arg);
		connectivity = CLOParser.parseInteger(numeric);
		if (connectivity != 1 && connectivity != 6) {
			connectivity = Math.max(6, connectivity);
		}
		return true;
	}

	@Override
	public void init() {
		if (size <= 0)
			throw new IllegalStateException("size must be set before initializing a cubic geometry");
		isRewired = false;
		isUndirected = true;
		isRegular = true;
		alloc();

		int l = (int) Math.floor(Math.pow(size, 1.0 / 3.0) + 0.5);
		int lz = l;
		if (size == 25000) {
			l = 50;
			lz = 10;
		}
		int l2 = l * l;

		switch ((int) Math.rint(connectivity)) {
			case 1:
				initSelf(l, lz, l2);
				break;
			case 6:
				initSixNeighbors(l, lz, l2);
				break;
			default:
				initCubicRange(l, lz, l2);
		}
		isValid = true;
	}

	private void initSelf(int l, int lz, int l2) {
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

	private void initSixNeighbors(int l, int lz, int l2) {
		boolean interspecies = isInterspecies();
		if (fixedBoundary) {
			for (int k = 0; k < lz; k++) {
				int z = k * l2;
				int u = (k >= lz - 1) ? -1 : z + l2;
				int d = (k > 0) ? z - l2 : -1;
				for (int i = 0; i < l; i++) {
					int x = i * l;
					int n = (i > 0) ? x - l : -1;
					int s = (i < l - 1) ? x + l : -1;
					for (int j = 0; j < l; j++) {
						int e = (j < l - 1) ? j + 1 : -1;
						int w = (j > 0) ? j - 1 : -1;
						int aPlayer = z + x + j;
						if (interspecies)
							addLinkAt(aPlayer, aPlayer);
						if (n >= 0)
							addLinkAt(aPlayer, z + n + j);
						if (e >= 0)
							addLinkAt(aPlayer, z + x + e);
						if (s >= 0)
							addLinkAt(aPlayer, z + s + j);
						if (w >= 0)
							addLinkAt(aPlayer, z + x + w);
						if (u >= 0)
							addLinkAt(aPlayer, u + x + j);
						if (d >= 0)
							addLinkAt(aPlayer, d + x + j);
					}
				}
			}
		} else {
			for (int k = 0; k < lz; k++) {
				int z = k * l2;
				int u = ((k + 1) % lz) * l2;
				int d = ((k - 1 + lz) % lz) * l2;
				for (int i = 0; i < l; i++) {
					int x = i * l;
					int n = ((i - 1 + l) % l) * l;
					int s = ((i + 1) % l) * l;
					for (int j = 0; j < l; j++) {
						int e = (j + 1) % l;
						int w = (j - 1 + l) % l;
						int aPlayer = z + x + j;
						if (interspecies)
							addLinkAt(aPlayer, aPlayer);
						addLinkAt(aPlayer, z + n + j);
						addLinkAt(aPlayer, z + x + e);
						addLinkAt(aPlayer, z + s + j);
						addLinkAt(aPlayer, z + x + w);
						addLinkAt(aPlayer, u + x + j);
						addLinkAt(aPlayer, d + x + j);
					}
				}
			}
		}
		isRegular = !fixedBoundary;
	}

	private void initCubicRange(int l, int lz, int l2) {
		boolean interspecies = isInterspecies();
		int range = Math.min(l / 2, Math.max(1, (int) (Math.pow(connectivity + 1.5, 1.0 / 3.0) / 2.0)));

		if (fixedBoundary) {
			for (int k = 0; k < lz; k++) {
				int z = k * l2;
				for (int i = 0; i < l; i++) {
					int x = i * l;
					for (int j = 0; j < l; j++) {
						int aPlayer = z + x + j;
						for (int kr = Math.max(0, k - range); kr <= Math.min(lz - 1, k + range); kr++) {
							int zr = kr * l2;
							for (int ir = Math.max(0, i - range); ir <= Math.min(l - 1, i + range); ir++) {
								int yr = ir * l;
								for (int jr = Math.max(0, j - range); jr <= Math.min(l - 1, j + range); jr++) {
									int bPlayer = zr + yr + jr;
									if (aPlayer == bPlayer && !interspecies)
										continue;
									addLinkAt(aPlayer, bPlayer);
								}
							}
						}
					}
				}
			}
		} else {
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
								for (int jr = j - range; jr <= j + range; jr++) {
									int bPlayer = zr + yr + ((jr + l) % l);
									if (aPlayer == bPlayer && !interspecies)
										continue;
									addLinkAt(aPlayer, bPlayer);
								}
							}
						}
					}
				}
			}
		}
		isRegular = !fixedBoundary;
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
