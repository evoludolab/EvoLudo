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
 * Base class for geometries constructed from specific degree distributions.
 */
public abstract class AbstractNetwork extends AbstractGeometry {

	/**
	 * Number of attempts before giving up on constructing the desired graph.
	 */
	static final int MAX_TRIALS = 10;

	/**
	 * Create a network-backed geometry for the provided engine.
	 *
	 * @param engine the EvoLudo pacemaker
	 */
	protected AbstractNetwork(EvoLudo engine) {
		super(engine);
	}

	/**
	 * Utility method to generate a network that realises the requested degree
	 * sequence.
	 *
	 * <h3>Requirements/notes:</h3>
	 * <ol>
	 * <li>The {@code degree} array is expected to be sorted in descending order.
	 * <li>During construction, already connected pairs are rewired via
	 * {@link #rewireNeighbourEdge(int, int, int[], int)} to avoid multiple edges.
	 * <li>If a single node remains unmatched, the method attempts to break one of
	 * the existing links and reconnect both endpoints to that final node.
	 * </ol>
	 *
	 * @param degree the degree (outgoing link count) requested for every node
	 * @return {@code true} if a matching graph was constructed successfully
	 */
	protected boolean initGeometryDegreeDistr(int[] degree) {
		isRewired = false;
		isUndirected = true;
		alloc();

		RNGDistribution rng = engine.getRNG();
		int todo;
		int[] core = new int[size];
		int[] full = new int[size];
		for (int n = 0; n < size; n++)
			core[n] = n;
		todo = size;

		if (!isType(GeometryType.DYNAMIC)) {
			// build a connected core first (excluding leaves)
			int leafIdx = -1;
			for (int i = size - 1; i >= 0; i--) {
				if (degree[i] <= 1)
					continue;
				leafIdx = i + 1;
				break;
			}
			todo = leafIdx;
			int[] active = new int[size];
			int idxa = rng.random0n(todo);
			active[0] = core[idxa];
			core[idxa] = core[--todo];
			int nActive = 1;
			int done = 0;
			while (todo > 0) {
				idxa = rng.random0n(nActive);
				int nodea = active[idxa];
				int idxb = rng.random0n(todo);
				int nodeb = core[idxb];
				addEdgeAt(nodea, nodeb);
				if (kout[nodea] == degree[nodea]) {
					full[done++] = nodea;
					active[idxa] = active[--nActive];
				}
				if (kout[nodeb] == degree[nodeb])
					full[done++] = nodeb;
				else
					active[nActive++] = nodeb;
				core[idxb] = core[--todo];
			}
			if (leafIdx < size)
				System.arraycopy(core, leafIdx, active, nActive, size - leafIdx);
			core = active;
			todo = nActive;
		}

		int escape = 0;
		while (todo > 1) {
			int idxa = rng.random0n(todo);
			int nodea = core[idxa];
			int idxb = rng.random0n(todo - 1);
			if (idxb >= idxa)
				idxb++;
			int nodeb = core[idxb];
			boolean success = true;
			if (isNeighborOf(nodea, nodeb)) {
				// avoid rewiring when nothing is in the connected set yet
				if (todo == size || !rewireNeighbourEdge(nodea, nodeb, full, size - todo))
					success = false;
			} else {
				addEdgeAt(nodea, nodeb);
			}
			if (!success) {
				if (++escape > MAX_TRIALS)
					return false;
				continue;
			}
			escape = 0;
			if (kout[nodea] == degree[nodea]) {
				full[size - todo] = nodea;
				core[idxa] = core[--todo];
				if (idxb == todo)
					idxb = idxa;
			}
			if (kout[nodeb] == degree[nodeb]) {
				full[size - todo] = nodeb;
				core[idxb] = core[--todo];
			}
		}
		if (todo == 1) {
			int nodea = core[0];
			int idxc = rng.random0n(size - 1);
			int nodec = full[idxc];
			int noded = out[nodec][rng.random0n(kout[nodec])];
			if (noded != nodea && !isNeighborOf(nodea, nodec) && !isNeighborOf(nodea, noded)) {
				removeEdgeAt(nodec, noded);
				addEdgeAt(nodea, nodec);
				addEdgeAt(nodea, noded);
				return true;
			}
			return false;
		}
		return true;
	}

	/**
	 * Attempt to rewire existing edges so that nodes {@code nodeA} and
	 * {@code nodeB} can gain additional neighbours without introducing duplicate
	 * links.
	 *
	 * @param nodeA the first node that still needs neighbours
	 * @param nodeB the second node that still needs neighbours
	 * @param done  the pool of nodes whose desired degrees are already satisfied
	 * @param nDone the number of valid entries in {@code done}
	 * @return {@code true} if rewiring succeeded
	 */
	private boolean rewireNeighbourEdge(int nodeA, int nodeB, int[] done, int nDone) {
		if (nDone <= 0)
			return false;
		int nodeC = done[engine.getRNG().random0n(nDone)];
		int nodeD = out[nodeC][engine.getRNG().random0n(kout[nodeC])];
		if (nodeD != nodeB && !isNeighborOf(nodeA, nodeC) && !isNeighborOf(nodeB, nodeD)) {
			removeEdgeAt(nodeC, nodeD);
			addEdgeAt(nodeA, nodeC);
			addEdgeAt(nodeB, nodeD);
			return true;
		}
		if (nodeD != nodeA && !isNeighborOf(nodeA, nodeD) && !isNeighborOf(nodeB, nodeC)) {
			removeEdgeAt(nodeC, nodeD);
			addEdgeAt(nodeA, nodeD);
			addEdgeAt(nodeB, nodeC);
			return true;
		}
		return false;
	}
}
