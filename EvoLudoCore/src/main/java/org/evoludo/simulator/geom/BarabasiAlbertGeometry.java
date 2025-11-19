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

import org.evoludo.math.RNGDistribution;
import org.evoludo.simulator.EvoLudo;
import org.evoludo.simulator.modules.Module;

/**
 * Scale-free network following the Barabási &amp; Albert preferential
 * attachment process.
 */
public class BarabasiAlbertGeometry extends AbstractNetwork {

	public BarabasiAlbertGeometry(EvoLudo engine) {
		super(engine);
		setType(Type.SCALEFREE_BA);
	}

	public BarabasiAlbertGeometry(EvoLudo engine, Module<?> module) {
		super(engine, module);
		setType(Type.SCALEFREE_BA);
	}

	public BarabasiAlbertGeometry(EvoLudo engine, Module<?> popModule, Module<?> oppModule) {
		super(engine, popModule, oppModule);
		setType(Type.SCALEFREE_BA);
	}

	public void parse(String arg) {
		if (arg == null || arg.isEmpty()) {
			if (connectivity < 2)
				connectivity = 2;
			warn("requires connectivity argument - using " + (int) connectivity + ".");
			return;
		}
		connectivity = Math.max(2, Integer.parseInt(arg));
	}

	@Override
	public void init() {
		if (size <= 0)
			throw new IllegalStateException("size must be set before initializing a Barabasi-Albert geometry");
		if (size == 1) {
			alloc();
			isValid = true;
			return;
		}
		isRewired = false;
		isUndirected = true;
		isRegular = false;
		alloc();

		RNGDistribution rng = engine.getRNG();

		int myLinks = Math.max(1, Math.min((int) (connectivity / 2.0 + 0.5), size - 1));
		int nStart = Math.max(myLinks, 2);
		for (int i = 1; i < nStart; i++)
			for (int j = 0; j < i; j++)
				addEdgeAt(i, j);

		int nLinks = nStart * (nStart - 1);
		for (int n = nStart; n < size; n++) {
			for (int i = 0; i < myLinks; i++) {
				int[] myNeigh = out[n];
				int nl = 0;
				for (int j = 0; j < i; j++)
					nl += kout[myNeigh[j]];
				int choices = Math.max(1, nLinks - nl);
				int ndice = rng.random0n(choices);
				int randnode = -1;
				for (int j = 0; j < n; j++) {
					if (isNeighborOf(n, j))
						continue;
					ndice -= kout[j];
					if (ndice < 0) {
						randnode = j;
						break;
					}
				}
				if (randnode < 0)
					throw new IllegalStateException("Failed to attach node in Barabasi-Albert geometry");
				addEdgeAt(n, randnode);
				nLinks++;
			}
			nLinks += myLinks;
		}
		isValid = true;
	}
}
