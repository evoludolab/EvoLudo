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
import org.evoludo.util.CLOParser;

/**
 * Scale-free/small-world network following the Klemm &amp; Eguíluz growth
 * process.
 */
public class KlemmEguiluzGeometry extends AbstractNetwork {

	private double klemmProbability = 0.0;

	public KlemmEguiluzGeometry(EvoLudo engine) {
		super(engine);
		setType(Type.SCALEFREE_KLEMM);
	}

	public KlemmEguiluzGeometry(EvoLudo engine, Module<?> module) {
		super(engine, module);
		setType(Type.SCALEFREE_KLEMM);
	}

	public KlemmEguiluzGeometry(EvoLudo engine, Module<?> popModule, Module<?> oppModule) {
		super(engine, popModule, oppModule);
		setType(Type.SCALEFREE_KLEMM);
	}

	public void parse(String arg) {
		double[] values = CLOParser.parseVector(arg);
		if (values.length == 0) {
			connectivity = Math.max(2, (int) Math.round(connectivity > 0 ? connectivity : 2.0));
			klemmProbability = 0.0;
			warn("requires connectivity argument - using " + (int) connectivity + " and p=0.");
		} else {
			connectivity = Math.max(2, (int) values[0]);
			klemmProbability = values.length >= 2 ? clampProbability(values[1]) : 0.0;
		}
		if (pRewire > 0.0 || pAddwire > 0.0) {
			warn("adding or rewiring links not supported - ignoring requests.");
			pRewire = 0.0;
			pAddwire = 0.0;
		}
	}

	private double clampProbability(double value) {
		if (value < 0.0)
			return 0.0;
		if (value > 1.0)
			return 1.0;
		return value;
	}

	@Override
	public void init() {
		if (size <= 0)
			throw new IllegalStateException("size must be set before initializing a Klemm-Eguiluz geometry");
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
		int nActive = Math.min((int) (connectivity / 2.0 + 0.5), size);
		int[] active = new int[nActive];
		for (int i = 0; i < nActive; i++)
			active[i] = i;

		int nStart = Math.max(nActive, 2);
		for (int i = 1; i < nStart; i++)
			for (int j = 0; j < i; j++)
				addEdgeAt(i, j);

		nextnode: for (int n = nStart; n < size; n++) {
			if (klemmProbability < 1e-8) {
				for (int i = 0; i < nActive; i++) {
					addEdgeAt(n, active[i]);
					addEdgeAt(active[i], n);
				}
			} else {
				for (int i = 0; i < nActive; i++) {
					if (klemmProbability > 1.0 - 1e-8 || rng.random01() < klemmProbability) {
						int links = 0;
						for (int j = 0; j < n; j++)
							links += kout[j];
						int randnode;
						do {
							int ndice = rng.random0n(Math.max(1, links));
							randnode = -1;
							for (int j = 0; j < n; j++) {
								ndice -= kout[j];
								if (ndice < 0) {
									randnode = j;
									break;
								}
							}
						} while (randnode < 0 || isNeighborOf(n, randnode));
						addEdgeAt(n, randnode);
						addEdgeAt(randnode, n);
					} else {
						addEdgeAt(n, active[i]);
						addEdgeAt(active[i], n);
					}
				}
			}
			double norm = 0.0;
			for (int i = 0; i < nActive; i++)
				norm += 1.0 / kout[active[i]];
			double hitNew = 1.0 / kout[n];
			double dice = rng.random01() * (norm + hitNew) - hitNew;
			if (dice < 0.0)
				continue nextnode;
			for (int i = 0; i < nActive; i++) {
				dice -= 1.0 / kout[active[i]];
				if (dice < 0.0) {
					active[i] = n;
					continue nextnode;
				}
			}
			throw new IllegalStateException("Emergency in Klemm-Eguiluz network creation.");
		}
		rewireUndirected(klemmProbability);
		isValid = true;
	}
}
