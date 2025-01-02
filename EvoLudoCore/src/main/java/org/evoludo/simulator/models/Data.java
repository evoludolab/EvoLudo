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
 * Data types that are handled by the model. Currently the following data types
 * are supported:
 * <dl>
 * <dt>Strategy
 * <dd>the data represents strategies.
 * <dt>Fitness
 * <dd>the data represents payoffs/scores/fitness.
 * <dt>Degree
 * <dd>the data represents degrees of the network structure.
 * <dt>Fixation probability
 * <dd>the data represents fixation probabilities.
 * <dt>Fixation time
 * <dd>the data represents fixation times.
 * <dt>Stationary distribution
 * <dd>the data represents the stationary strategy distribution.
 * <dt>undefined
 * <dd>the data type is not defined/unknown.
 * </dl>
 */
public enum Data {

	/**
	 * Undefined: the data type is not defined/unknown.
	 */
	UNDEFINED("undefined"), //

	/**
	 * Strategy: the data represents strategies.
	 */
	STRATEGY("Strategies - Histogram"), //

	/**
	 * Fitness: the data represents payoffs/scores/fitness.
	 */
	FITNESS("Fitness - Histogram"), //

	/**
	 * Degree: the data represents degrees of the network structure.
	 */
	DEGREE("Structure - Degree"), //

	/**
	 * Fixation probability: the data represents fixation probabilities.
	 */
	STATISTICS_FIXATION_PROBABILITY("Statistics - Fixation probability"), //

	/**
	 * Fixation time: the data represents fixation times.
	 */
	STATISTICS_FIXATION_TIME("Statistics - Fixation time"), //

	/**
	 * Stationary distribution.
	 */
	STATISTICS_STATIONARY("Statistics - Stationary distribution"); //

	/**
	 * Identifying id of the type of data.
	 */
	String id;

	/**
	 * Construct an enum for the type of data.
	 * 
	 * @param id the identifier of the data type
	 */
	Data(String id) {
		this.id = id;
	}

	@Override
	public String toString() {
		return id;
	}

	/**
	 * Checks if the data type is a statistics type.
	 * 
	 * @return <code>true</code> for statistics data types
	 */
	public boolean isStatistics() {
		return (this == STATISTICS_FIXATION_PROBABILITY || this == STATISTICS_FIXATION_TIME
				|| this == STATISTICS_STATIONARY);
	}
}
