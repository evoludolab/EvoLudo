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
 * Directed super-star geometry with configurable petals and amplification.
 */
public class SuperstarGeometry extends AbstractGeometry {

	private int petals = 1;
	private int amplification = 3;

	public SuperstarGeometry(EvoLudo engine) {
		super(engine);
		setType(Type.SUPER_STAR);
	}

	public SuperstarGeometry(EvoLudo engine, Module<?> module) {
		super(engine, module);
		setType(Type.SUPER_STAR);
	}

	public SuperstarGeometry(EvoLudo engine, Module<?> popModule, Module<?> oppModule) {
		super(engine, popModule, oppModule);
		setType(Type.SUPER_STAR);
	}

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
}
