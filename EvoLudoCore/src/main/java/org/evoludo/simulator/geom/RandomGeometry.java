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

/**
 * Random (undirected) graph geometry that ensures the generated network is
 * connected before sprinkling additional random edges.
 */
public class RandomGeometry extends AbstractGeometry {

	/**
	 * Create an undirected random-graph geometry coordinated by the provided
	 * engine.
	 *
	 * @param engine EvoLudo pacemaker
	 */
	public RandomGeometry(EvoLudo engine) {
		super(engine);
		setType(GeometryType.RANDOM_GRAPH);
	}

	@Override
	public boolean parse(String arg) {
		connectivity = 2.0;
		if (arg != null && !arg.isEmpty())
			connectivity = Math.max(2, Integer.parseInt(arg));
		return true;
	}

	/**
	 * Generates a connected undirected random graph. Ensures connectivity by
	 * first building a spanning tree and then sprinkling additional random edges.
	 */
	@Override
	public void init() {
		if (size <= 0)
			throw new IllegalStateException("size must be set before initializing a random graph geometry");
		isRewired = false;
		isUndirected = true;
		isRegular = false;
		alloc();
		if (size == 1) {
			isValid = true;
			return;
		}

		RNGDistribution rng = engine.getRNG();

		int nLinks = (int) Math.floor(connectivity * size + 0.5);
		nLinks = (nLinks - nLinks % 2) / 2;
		int[] isolated = new int[size];
		int[] connected = new int[size];
		for (int i = 0; i < size; i++)
			isolated[i] = i;
		int todo = size;
		int done = 0;

		int parent = rng.random0n(todo);
		int parentIdx = isolated[parent];
		todo--;
		if (parent != todo)
			System.arraycopy(isolated, parent + 1, isolated, parent, todo - parent);
		connected[done++] = parentIdx;

		int child = rng.random0n(todo);
		int childIdx = isolated[child];
		todo--;
		System.arraycopy(isolated, child + 1, isolated, child, todo - child);
		connected[done++] = childIdx;
		addEdgeAt(parentIdx, childIdx);
		nLinks--;

		while (todo > 0) {
			parent = rng.random0n(done);
			parentIdx = connected[parent];
			child = rng.random0n(todo);
			childIdx = isolated[child];
			todo--;
			System.arraycopy(isolated, child + 1, isolated, child, todo - child);
			connected[done++] = childIdx;
			addEdgeAt(parentIdx, childIdx);
			nLinks--;
		}

		while (nLinks > 0) {
			parentIdx = rng.random0n(size);
			childIdx = rng.random0n(size - 1);
			if (childIdx >= parentIdx)
				childIdx++;
			if (isNeighborOf(parentIdx, childIdx))
				continue;
			addEdgeAt(parentIdx, childIdx);
			nLinks--;
		}
		isValid = true;
	}

	@Override
	protected boolean checkSettings() {
		if (size > 0) {
			double maxConn = Math.max(0, size - 1);
			double minConn = size > 2 ? 2.0 : maxConn;
			double adjusted = Math.max(minConn, Math.min(connectivity, maxConn));
			if (Math.abs(adjusted - connectivity) > 1e-8) {
				connectivity = adjusted;
				warn("requires connectivity between " + minConn + " and " + maxConn + " - using " + adjusted + "!");
			}
		}
		return false;
	}
}
