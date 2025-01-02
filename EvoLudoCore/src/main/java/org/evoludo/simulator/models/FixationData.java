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

import org.evoludo.util.Formatter;

/**
 * EXPERIMENTAL: should mature into data structure useful for statistics
 */
public class FixationData {

	/**
	 * Creates a new fixation data structure.
	 */
	public FixationData() {
	}

	/**
	 * The index of the node (location) where the initial mutant arose.
	 */
	public int mutantNode = -1;

	/**
	 * The strategy type of the initial mutant.
	 */
	public int mutantTrait = -1;

	/**
	 * The strategy type of the resident.
	 */
	public int residentTrait = -1;

	/**
	 * The strategy type that reached fixation.
	 */
	public int typeFixed = -1;

	/**
	 * The number of updates until fixation was reached.
	 */
	public double updatesFixed = -1.0;

	/**
	 * The time until fixation in realtime units.
	 */
	public double timeFixed = -1.0;

	/**
	 * The flag indicating whether the fixation data (probabilities) has been read.
	 */
	public boolean probRead = true;

	/**
	 * The flag indicating whether the fixation times have been read.
	 */
	public boolean timeRead = true;

	/**
	 * Reset the fixation data to get ready for the next sample.
	 */
	public void reset() {
		mutantNode = -1;
		mutantTrait = -1;
		residentTrait = -1;
		probRead = true;
		timeRead = true;
	}

	@Override
	public String toString() {
		return "{ mutantNode -> " + mutantNode + //
				", mutantTrait -> " + mutantTrait + //
				", residentTrait -> " + residentTrait + //
				", typeFixed -> " + typeFixed + //
				", updatesFixed -> " + Formatter.format(updatesFixed, 6) + //
				", timeFixed -> " + Formatter.format(timeFixed, 6) + //
				", probRead -> " + probRead + //
				", timeRead -> " + timeRead + " }";
	}
}
