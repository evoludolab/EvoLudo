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

/**
 * Geometry implementation for well-mixed (mean-field) populations.
 */
public class WellmixedGeometry extends AbstractGeometry {

	/**
	 * Create a well-mixed (mean-field) geometry tied to the supplied engine.
	 *
	 * @param engine EvoLudo pacemaker
	 */
	public WellmixedGeometry(EvoLudo engine) {
		super(engine);
		setType(GeometryType.WELLMIXED);
	}

	/**
	 * Generates a well-mixed graph, also termed mean-field network or unstructured
	 * population. In the limit of large population sizes, the results of IBS
	 * simulations must converge to those of the corresponding stochastic and
	 * deterministic dynamical equations as produced by SDE and ODE models.
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
