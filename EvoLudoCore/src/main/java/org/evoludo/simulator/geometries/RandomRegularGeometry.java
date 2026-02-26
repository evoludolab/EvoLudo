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

package org.evoludo.simulator.geometries;

import java.util.Arrays;

import org.evoludo.simulator.EvoLudo;

/**
 * Random regular graph geometry that repeatedly samples degree distributions
 * until a connected realization is found.
 */
public class RandomRegularGeometry extends AbstractNetwork {

	/**
	 * Create a random regular geometry tied to the given engine.
	 *
	 * @param engine EvoLudo pacemaker
	 */
	public RandomRegularGeometry(EvoLudo engine) {
		super(engine);
		setType(GeometryType.RANDOM_REGULAR_GRAPH);
	}

	@Override
	public boolean parse(String arg) {
		if (arg != null && !arg.isEmpty())
			connectivity = Math.max(2, Integer.parseInt(arg));
		return true;
	}

	/**
	 * Generates a connected undirected random regular graph with degree equal to
	 * the requested connectivity, retrying construction if necessary.
	 */
	@Override
	public void init() {
		isRegular = true;
		final int[] degrees = new int[size];
		Arrays.fill(degrees, (int) connectivity);
		int trials = 0;
		boolean success;
		do {
			// start from a clean slate each time the random construction is retried
			alloc();
			success = initGeometryDegreeDistr(degrees);
		} while (!success && ++trials < AbstractNetwork.MAX_TRIALS);
		if (!success)
			throw new IllegalStateException("Failed to construct random regular graph");
	}

	@Override
	protected boolean checkSettings() {
		connectivity = Math.min(Math.floor(connectivity), size - 1.0);
		int nConn = (int) connectivity;
		if ((size * nConn) % 2 == 1 && setSize(size + 1)) {
			if (engine.getModule().cloNPopulation.isSet())
				warn("requires even link count - increasing size to " + size + "!");
			return true;
		}
		return false;
	}
}
