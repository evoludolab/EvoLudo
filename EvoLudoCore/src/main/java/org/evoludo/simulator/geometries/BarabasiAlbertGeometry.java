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

import org.evoludo.math.RNGDistribution;
import org.evoludo.simulator.EvoLudo;
import org.evoludo.util.CLOParser;
import org.evoludo.util.Formatter;

/**
 * Scale-free network following the Barabási &amp; Albert preferential
 * attachment process.
 */
public class BarabasiAlbertGeometry extends AbstractNetwork {

	/**
	 * Create a Barabási-Albert geometry tied to the given engine.
	 *
	 * @param engine EvoLudo pacemaker
	 */
	public BarabasiAlbertGeometry(EvoLudo engine) {
		super(engine);
		setType(GeometryType.SCALEFREE_BA);
	}

	@Override
	public boolean parse(String arg) {
		if (arg == null || arg.isEmpty()) {
			if (connectivity < 2.0)
				connectivity = 2.0;
			warn("requires connectivity argument - using " + Formatter.format(connectivity, 3) + ".");
		} else {
			connectivity = Math.max(2.0, CLOParser.parseDouble(arg));
		}
		return true;
	}

	/**
	 * Generates a connected undirected scale-free network following the
	 * Barabási–Albert preferential attachment model.
	 */
	@Override
	public void init() {
		if (size <= 0)
			throw new IllegalStateException("size must be set before initializing a Barabasi-Albert geometry");
		if (size == 1) {
			isValid = true;
			return;
		}
		if (connectivity > size - 1.0) {
			connectivity = size - 1.0;
			warn("invalid connectivity - reduced to " + (size - 1));
		}
		isRewired = false;
		isUndirected = true;
		isRegular = false;

		RNGDistribution rng = engine.getRNG();
		// Each new node contributes m links. To realize odd target connectivities,
		// sample m between floor(k/2) and ceil(k/2).
		double k2 = Math.max(1.0, connectivity * 0.5);
		int m = (int) k2;
		double pExtra = k2 - m;

		// Start from a seed clique with m+1 nodes (common BA convention).
		int nCore = Math.max(m + 1, 2);
		for (int i = 1; i < nCore; i++)
			for (int j = 0; j < i; j++)
				addEdgeAt(i, j);

		int nLinks = nCore * (nCore - 1);
		for (int n = nCore; n < size; n++) {
			int myLinks = m;
			if (pExtra > 0.0 && rng.random01() < pExtra)
				myLinks++;
			for (int i = 0; i < myLinks; i++) {
				int[] myNeigh = out[n];
				int nl = 0;
				for (int j = 0; j < i; j++)
					nl += kout[myNeigh[j]];
				int choices = Math.max(1, nLinks - nl);
				int ndice = rng.random0n(choices);
				int randnode = pickPreferentialNode(n, ndice);
				if (randnode < 0)
					throw new IllegalStateException("Failed to attach node in Barabasi-Albert geometry");
				addEdgeAt(n, randnode);
				nLinks++;
			}
			nLinks += myLinks;
		}
		isValid = true;
	}

	/**
	 * Picks a node according to preferential attachment.
	 *
	 * @param n     current node index
	 * @param ndice random index for selection
	 * @return selected node index
	 */
	private int pickPreferentialNode(int n, int ndice) {
		for (int j = 0; j < n; j++) {
			if (!isNeighborOf(n, j)) {
				ndice -= kout[j];
				if (ndice < 0)
					return j;
			}
		}
		return -1;
	}
}
