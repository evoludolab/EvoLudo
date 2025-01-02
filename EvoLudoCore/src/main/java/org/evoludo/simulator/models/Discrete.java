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
//			(doi: <doi>[, <version>])
//
//	<doi>:	digital object identifier of the downloaded release (or the
//			most recent release if downloaded from github.com),
//	<year>:	year of release (or download), and
//	[, <version>]: optional version number (as reported in output header
//			or GUI console) to simplify replication of reported results.
//
// The formatting may be adjusted to comply with publisher requirements.
//

package org.evoludo.simulator.models;

/**
 * Common interface for all models with discrete strategy sets.
 */
public interface Discrete {

	/**
	 * Calculate and return the payoff/score of individuals in monomorphic
	 * populations with trait/strategy {@code type} but also deals with payoff
	 * accounting (averaged versus accumulated).
	 *
	 * @param id   the id of the population for multi-species models
	 * @param type trait/strategy
	 * @return payoff/score in monomorphic population with trait/strategy
	 *         {@code type}. Returns {@code NaN} if scores ill defined
	 * 
	 * @see org.evoludo.simulator.modules.Discrete#getMonoGameScore(int)
	 */
	public default double getMonoScore(int id, int type) {
		return Double.NaN;
	}
}
