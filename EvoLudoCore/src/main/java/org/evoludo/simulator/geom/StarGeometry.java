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
 * Star geometry with node 0 as the hub connected to all leaves.
 */
public class StarGeometry extends AbstractGeometry {

	public StarGeometry(EvoLudo engine) {
		super(engine);
		setType(Type.STAR);
	}

	public StarGeometry(EvoLudo engine, Module<?> module) {
		super(engine, module);
		setType(Type.STAR);
	}

	public StarGeometry(EvoLudo engine, Module<?> popModule, Module<?> oppModule) {
		super(engine, popModule, oppModule);
		setType(Type.STAR);
	}

	public void parse(String arg) {
		// no parameters to parse for star
	}

	@Override
	public void init() {
		if (size <= 0)
			throw new IllegalStateException("size must be set before initializing a star geometry");
		isRewired = false;
		isUndirected = true;
		isRegular = false;
		alloc();

		for (int i = 1; i < size; i++) {
			addLinkAt(0, i);
			addLinkAt(i, 0);
		}
		isValid = true;
	}
}
