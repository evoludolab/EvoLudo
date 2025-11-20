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
 * Geometry implementation for well-mixed (mean-field) populations. Serves as an
 * example for extracting the legacy {@link org.evoludo.simulator.Geometry}
 * monolith into dedicated subclasses.
 */
public class WellmixedGeometry extends AbstractGeometry {

	public WellmixedGeometry(EvoLudo engine) {
		super(engine);
		setType(Type.WELLMIXED);
	}

	/**
	 * Create a well-mixed geometry for the provided module.
	 *
	 * @param engine EvoLudo pacemaker
	 * @param module owning module
	 */
	public WellmixedGeometry(EvoLudo engine, Module<?> module) {
		super(engine, module);
		setType(Type.WELLMIXED);
	}

	/**
	 * Create a well-mixed geometry for the specified populations.
	 *
	 * @param engine    EvoLudo pacemaker
	 * @param popModule focal population module
	 * @param oppModule opponent population module
	 */
	public WellmixedGeometry(EvoLudo engine, Module<?> popModule, Module<?> oppModule) {
		super(engine, popModule, oppModule);
		setType(Type.WELLMIXED);
	}

	/**
	 * Generates a well-mixed graph, also termed mean-field network or unstructured
	 * population. In the limit of large population sizes the results of IBS
	 * simulations must converge to those of the corresponding deterministic
	 * dynamical equations (ODEs) or, with mutations, the stochastic dynamical
	 * equations (SDEs).
	 */
	@Override
	public void init() {
		isRewired = false;
		isUndirected = true;
		isRegular = true;
		alloc();
		isValid = true;
	}
}
