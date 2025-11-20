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
 * Directed super-star geometry with configurable petals and amplification. A
 * super-star consists of a central hub surrounded by {@code p} petals, each
 * with a reservoir of size {@code r} feeding into a linear chain of length
 * {@code k} that connects back to the hub. The structure implements a strong
 * directed evolutionary amplifier.
 * 
 * @see <a href="http://dx.doi.org/10.1038/nature03204">Lieberman, Hauert &
 *      Nowak (2005) Nature 433:312-316</a>
 */
public class SuperstarGeometry extends AbstractGeometry {

	/**
	 * Number of petals in the superstar structure.
	 */
	private int petals = 1;
	/**
	 * Amplification factor (length of the linear chain plus additional nodes).
	 */
	private int amplification = 3;

	/**
	 * Create a super-star geometry tied to the given engine.
	 *
	 * @param engine EvoLudo pacemaker
	 */
	public SuperstarGeometry(EvoLudo engine) {
		super(engine);
		setType(Type.SUPER_STAR);
	}

	/**
	 * Create a super-star geometry for the provided module.
	 *
	 * @param engine EvoLudo pacemaker
	 * @param module owning module
	 */
	public SuperstarGeometry(EvoLudo engine, Module<?> module) {
		super(engine, module);
		setType(Type.SUPER_STAR);
	}

	/**
	 * Create a super-star geometry for the specified populations.
	 *
	 * @param engine    EvoLudo pacemaker
	 * @param popModule focal population module
	 * @param oppModule opponent population module
	 */
	public SuperstarGeometry(EvoLudo engine, Module<?> popModule, Module<?> oppModule) {
		super(engine, popModule, oppModule);
		setType(Type.SUPER_STAR);
	}

	@Override
	public boolean parse(String arg) {
		int[] values = CLOParser.parseIntVector(arg);
		if (values.length > 0)
			petals = Math.max(1, values[0]);
		if (values.length > 1)
			amplification = values[1];
		if (petals <= 0)
			petals = 1;
		if (amplification <= 0)
			amplification = 3;
		return true;
	}

	/**
	 * Generates a superstar geometry, i.e. a strong directed amplifier of
	 * selection. Reservoir nodes within each petal connect to the hub via a linear
	 * chain while the hub connects back to all reservoir nodes.
	 *
	 * <h3>Requirements/notes:</h3>
	 * <ol>
	 * <li>Population size \(N=(r+k-1)p+1\) with \(r,k,p\) integer.
	 * <li>Strong amplification requires \(r\gg k,p\).
	 * <li>Node {@code 0} is the hub.
	 * </ol>
	 *
	 * @see <a href="http://dx.doi.org/10.1038/nature03204">Lieberman, Hauert &
	 *      Nowak (2005) Nature 433:312-316</a>
	 */
	@Override
	public void init() {
		if (size <= 0)
			throw new IllegalStateException("size must be set before initializing a super-star geometry");
		isRewired = false;
		isUndirected = false;
		isRegular = false;
		alloc();

		int pnodes = petals * (amplification - 2);
		for (int i = pnodes + 1; i < size; i++) {
			addLinkAt(0, i);
			addLinkAt(i, (i - pnodes - 1) % petals + 1);
		}
		for (int i = 1; i <= (pnodes - petals); i++)
			addLinkAt(i, i + petals);
		for (int i = 1; i <= petals; i++)
			addLinkAt(pnodes - petals + i, 0);
		isValid = true;
	}

	@Override
	protected boolean checkSettings() {
		boolean doReset = false;
		if (petals <= 0) {
			petals = 1;
			warn("requires >=1 petals - using 1!");
		}
		if (amplification < 3) {
			amplification = 3;
			warn("requires amplification >=3 - using 3!");
		}
		int pnodes = petals * (amplification - 2);
		int reservoir = Math.max(1, (size - 1 - pnodes) / petals);
		int requiredSize = reservoir * petals + pnodes + 1;
		if (enforceSize(requiredSize))
			doReset = true;
		connectivity = (double) (2 * reservoir * petals + pnodes) / (double) size;
		isUndirected = false;
		isRegular = false;
		return doReset;
	}

	@Override
	public SuperstarGeometry clone() {
		SuperstarGeometry clone = (SuperstarGeometry) super.clone();
		clone.petals = petals;
		clone.amplification = amplification;
		return clone;
	}
}
