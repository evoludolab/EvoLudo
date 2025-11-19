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
 * Linear (1D lattice) geometry that supports asymmetric neighbourhoods and
 * optional fixed boundaries.
 */
public class LinearGeometry extends AbstractGeometry {

	private int linearAsymmetry = 0;

	public LinearGeometry(EvoLudo engine) {
		super(engine);
		setType(Type.LINEAR);
	}

	public LinearGeometry(EvoLudo engine, Module<?> module) {
		super(engine, module);
		setType(Type.LINEAR);
	}

	public LinearGeometry(EvoLudo engine, Module<?> popModule, Module<?> oppModule) {
		super(engine, popModule, oppModule);
		setType(Type.LINEAR);
	}

	@Override
	public void init() {
		if (size <= 0)
			throw new IllegalStateException("size must be set before initializing a linear geometry");
		isRewired = false;
		isRegular = !fixedBoundary;
		alloc();
		boolean interspecies = isInterspecies();

		int neighbors = (int) (connectivity + 0.5);
		int left = (neighbors + linearAsymmetry) / 2;
		int right = (neighbors - linearAsymmetry) / 2;
		isUndirected = (left == right);

		for (int i = 0; i < size; i++) {
			for (int j = -left; j <= right; j++) {
				if ((j == 0 && !interspecies) || (fixedBoundary && (i + j < 0 || i + j >= size)))
					continue;
				addLinkAt(i, (i + j + size) % size);
			}
		}
		isValid = true;
	}

	@Override
	public void reset() {
		super.reset();
		linearAsymmetry = 0;
	}

	public void setLinearAsymmetry(int asymmetry) {
		this.linearAsymmetry = asymmetry;
	}

	public int getLinearAsymmetry() {
		return linearAsymmetry;
	}
}
