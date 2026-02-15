//
// EvoLudo Project
//
// Copyright 2010-2026 Christoph Hauert
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
 * Modes of the model. Currently thefollowing modes are supported:
 * <dl>
 * <dt>DYNAMICS
 * <dd>follow the time series of the model. This is the default.
 * <dt>STATISTICS_SAMPLE
 * <dd>generate samples to create statistics of the model. Run model until it
 * stops and advertise that a new data point is available. Start next sample,
 * once the data is retrieved and processed.
 * <dt>STATISTICS_UPDATE
 * <dd>generate samples from single run to create statistics of the
 * model reflecting the different states of the population.
 * </dl>
 */
public enum Mode {
	/**
	 * Dynamics: follow the time series of the model.
	 */
	DYNAMICS("dynamics"), //

	/**
	 * Statistics: generate samples to create statistics of the model. Run model
	 * until it stops and advertise that a new data point is available. Start
	 * next sample, once the data is retrieved and processed.
	 */
	STATISTICS_SAMPLE("statistics_sample"), //

	/**
	 * Statistics: generate samples from single run to create statistics of the
	 * model reflecting the different states of the population.
	 */
	STATISTICS_UPDATE("statistics_update"); //

	/**
	 * Identifying id of the type of mode.
	 */
	String id;

	/**
	 * Construct an enum for the type of mode.
	 * 
	 * @param id the identifier of the mode
	 */
	Mode(String id) {
		this.id = id;
	}

	@Override
	public String toString() {
		return id;
	}

	/**
	 * Checks if the mode is a statistics mode.
	 * 
	 * @return {@code true} if the mode is a statistics mode, {@code false}
	 *         otherwise
	 */
	public boolean isStatistics() {
		return this == STATISTICS_SAMPLE || this == STATISTICS_UPDATE;
	}
}
