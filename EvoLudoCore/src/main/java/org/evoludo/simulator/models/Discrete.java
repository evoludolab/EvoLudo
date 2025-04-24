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

package org.evoludo.simulator.models;

/**
 * Common interface for all models with discrete sets of traits.
 */
public interface Discrete {

	/**
	 * Calculate and return the payoff/score of individuals in monomorphic
	 * populations with trait {@code idx} but also deals with payoff
	 * accounting (averaged versus accumulated).
	 *
	 * @param id  the id of the population for multi-species models
	 * @param idx the index of the trait
	 * @return payoff/score in monomorphic population with trait
	 *         {@code idx}. Returns {@code NaN} if scores ill defined
	 * 
	 * @see org.evoludo.simulator.modules.Discrete#getMonoPayoff(int)
	 */
	public default double getMonoScore(int id, int idx) {
		return Double.NaN;
	}

	/**
	 * Set initial traits in one or all species.
	 * <p>
	 * <strong>NOTE:</strong> this is a convenience method for multi-species modules
	 * to set inital states efficiently for interactions with GUI.
	 *
	 * @param init the array with the initial trait values
	 * @return {@code true} if initial traits successfully set
	 */
	public default boolean setInitialTraits(double[] init) {
		return false;
	}

	/**
	 * Collect and return initial trait values for all species.
	 * <p>
	 * <strong>NOTE:</strong> this is a convenience method for multi-species modules
	 * to retrieve states efficiently for further processing or visualization.
	 *
	 * @param init the array for storing the initial trait values
	 */
	public abstract void getInitialTraits(double[] init);

	/**
	 * Set initial trait values for species with ID <code>id</code>.
	 *
	 * @param id   the species identifier
	 * @param init the array with the initial trait values
	 * @return {@code true} if initial traits successfully set
	 */
	public default boolean setInitialTraits(int id, double[] init) {
		return false;
	}

	/**
	 * Return initial trait values for species with ID <code>id</code>.
	 *
	 * @param id   the species identifier
	 * @param init the array for storing the initial trait values
	 */
	public abstract void getInitialTraits(int id, double[] init);
}
