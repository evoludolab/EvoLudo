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

import org.evoludo.math.RNGDistribution;
import org.evoludo.simulator.EvoLudo;

/**
 * Base class for geometries constructed from specific degree distributions.
 */
@SuppressWarnings("java:S2160") // rng is a convenience field must not affect equality
public abstract class AbstractNetwork extends AbstractGeometry {

	/**
	 * Number of attempts before giving up on constructing the desired graph.
	 */
	static final int MAX_TRIALS = 10;

	/**
	 * The random number generator distribution used for network construction.
	 */
	RNGDistribution rng;

	/**
	 * Create a network-backed geometry for the provided engine.
	 *
	 * @param engine the EvoLudo pacemaker
	 */
	protected AbstractNetwork(EvoLudo engine) {
		super(engine);
		this.rng = engine.getRNG();
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

		int[] core = initializeCore();
		int[] full = new int[size];

		int todo = size;
		if (!isType(GeometryType.DYNAMIC)) {
			todo = buildConnectedCore(core, full, degree);
			core = Arrays.copyOf(core, todo); // keep only active slice
		}

		todo = connectCorePairs(core, full, degree, todo);
		if (todo == 0)
			return true;
		if (todo < 0)
			return false;
		if (todo == 1)
			return handleSingleUnmatched(core[0], full);
		throw new IllegalStateException("unreachable code reached in initGeometryDegreeDistr");
	}

	/**
	 * Initialize the core array with all node indices.
	 *
	 * @return the initialized core array
	 */
	private int[] initializeCore() {
		int[] core = new int[size];
		for (int n = 0; n < size; n++)
			core[n] = n;
		return core;
	}

	/**
	 * Build a connected core of the network first, excluding leaves.
	 *
	 * @param core   the array of node indices to draw from
	 * @param full   the array to store completed nodes in
	 * @param degree the desired degree sequence
	 * @return the number of remaining nodes in {@code core} after building the
	 *         connected core
	 */
	private int buildConnectedCore(int[] core, int[] full, int[] degree) {
		// build a connected core first (excluding leaves)
		int leafIdx = -1;
		for (int i = size - 1; i >= 0; i--) {
			if (degree[i] > 1) {
				leafIdx = i + 1;
				break;
			}
		}
		int todo = leafIdx;
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
		System.arraycopy(active, 0, core, 0, nActive);
		return nActive;
	}

	/**
	 * Connect pairs of nodes from the core until no unmatched nodes remain or no
	 * further progress can be made.
	 *
	 * @param core   the array of node indices to draw from
	 * @param full   the array to store completed nodes in
	 * @param degree the desired degree sequence
	 * @param remain the initial number of nodes in {@code core}
	 * @return the number of remaining nodes in {@code core} after attempting to
	 *         connect all pairs, or -1 if no progress could be made
	 */
	private int connectCorePairs(int[] core, int[] full, int[] degree, int remain) {
		int todo = remain;
		int escape = 0;
		while (todo > 1) {
			int idxa = rng.random0n(todo);
			int nodea = core[idxa];
			int idxb = rng.random0n(todo - 1);
			if (idxb >= idxa)
				idxb++;
			int nodeb = core[idxb];
			boolean success = tryConnectOrRewire(nodea, nodeb, full, size - todo, todo);
			if (!success) {
				if (++escape > MAX_TRIALS)
					return -1;
				continue;
			}
			escape = 0;
			todo = updateProgressAfterConnect(core, full, degree, todo, idxa, idxb);
		}
		return todo;
	}

	/**
	 * Attempt to connect two nodes, rewiring existing edges if necessary.
	 *
	 * @param nodea  the first node to connect
	 * @param nodeb  the second node to connect
	 * @param full   the pool of nodes whose desired degrees are already satisfied
	 * @param nFull  the number of valid entries in {@code full}
	 * @param remain the number of remaining nodes to connect
	 * @return {@code true} if the connection (or rewiring) succeeded
	 */
	private boolean tryConnectOrRewire(int nodea, int nodeb, int[] full, int nFull, int remain) {
		if (isNeighborOf(nodea, nodeb)) {
			// avoid rewiring when nothing is in the connected set yet
			return remain != size && rewireNeighbourEdge(nodea, nodeb, full, nFull);
		}
		addEdgeAt(nodea, nodeb);
		return true;
	}

	/**
	 * Update the progress after successfully connecting two nodes.
	 *
	 * @param core   the array of node indices to draw from
	 * @param full   the array to store completed nodes in
	 * @param degree the desired degree sequence
	 * @param remain the current number of nodes in {@code core}
	 * @param idxa   the index of the first connected node in {@code core}
	 * @param idxb   the index of the second connected node in {@code core}
	 * @return the updated number of nodes remaining in {@code core}
	 */
	private int updateProgressAfterConnect(int[] core, int[] full, int[] degree, int remain, int idxa, int idxb) {
		int nodea = core[idxa];
		int nodeb = core[idxb];
		if (kout[nodea] == degree[nodea]) {
			full[size - remain] = nodea;
			core[idxa] = core[--remain];
			if (idxb == remain)
				idxb = idxa;
		}
		if (kout[nodeb] == degree[nodeb]) {
			full[size - remain] = nodeb;
			core[idxb] = core[--remain];
		}
		return remain;
	}

	/**
	 * Handle the case where a single node remains unmatched by breaking an existing
	 * edge and reconnecting both endpoints to that final node.
	 * 
	 * @param nodea the single unmatched node
	 * @param full  the pool of nodes whose desired degrees are already satisfied
	 * @return {@code true} if reconnection succeeded
	 */
	private boolean handleSingleUnmatched(int nodea, int[] full) {
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
