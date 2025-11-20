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

import java.util.Arrays;

import org.evoludo.math.ArrayMath;
import org.evoludo.math.RNGDistribution;
import org.evoludo.simulator.EvoLudo;

/**
 * Base class for geometries constructed from specific degree distributions.
 */
public abstract class AbstractNetwork extends AbstractGeometry {

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
		int todo;
		int[] core = new int[size];
		int[] full = new int[size];
		for (int n = 0; n < size; n++)
			core[n] = n;
		todo = size;
		Arrays.fill(full, -1);
		int escape = 0;
		while (todo > 1) {
			int idxa = engine.getRNG().random0n(todo);
			int nodea = core[idxa];
			int idxb = engine.getRNG().random0n(todo - 1);
			if (idxb >= idxa)
				idxb++;
			int nodeb = core[idxb];
			if (isNeighborOf(nodea, nodeb)) {
				if (!rewireNeighbourEdge(nodea, nodeb, full, size - todo))
					continue;
			} else
				addEdgeAt(nodea, nodeb);
			if (++escape > 10)
				return false;
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
			int idxc = engine.getRNG().random0n(size - 1);
			int nodec = full[idxc];
			int noded = out[nodec][engine.getRNG().random0n(kout[nodec])];
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

	/**
	 * Rewire undirected links while keeping the graph connected.
	 *
	 * <h3>Requirements/notes:</h3>
	 * <ol>
	 * <li>Only applicable to undirected graphs.
	 * <li>Rewiring preserves connectivity of all nodes and the graph remains
	 * undirected.
	 * <li>The number of rewired links is
	 * \(N_\text{rewired}=\min(N_\text{links},
	 * N_\text{links} \log(1-p_\text{undir}))\), i.e. at most the number of
	 * existing undirected links. Thus at most an expected fraction of \(1-1/e\)
	 * (≈63%) of the original links get rewired.
	 * </ol>
	 *
	 * @param prob probability of rewiring any particular undirected link
	 * @return {@code true} if rewiring was performed
	 */
	protected boolean rewireUndirected(double prob) {
		if (!isUndirected || prob <= 0.0)
			return false;
		RNGDistribution rng = engine.getRNG();
		int nLinks = (int) Math
				.floor(ArrayMath.norm(kout) / 2 * Math.min(1.0, -Math.log(1.0 - prob)) + 0.5);
		long done = 0;
		while (done < nLinks) {
			int first, len;
			do {
				first = rng.random0n(size);
				len = kin[first];
			} while (len <= 1 || len == size - 1);
			int firstneigh = in[first][rng.random0n(len)];
			int second;
			do {
				second = rng.random0n(size - 1);
				if (second >= first)
					second++;
				len = kin[second];
			} while (len <= 1 || len == size - 1);
			int secondneigh = in[second][rng.random0n(len)];

			if (!swapEdges(first, firstneigh, second, secondneigh))
				continue;
			if (!isGraphConnected()) {
				swapEdges(first, firstneigh, second, secondneigh);
				swapEdges(first, secondneigh, second, firstneigh);
				continue;
			}
			done += 2;
		}
		return true;
	}

	/**
	 * Utility method to swap edges (undirected links) between nodes: change link
	 * {@code a-an} to {@code a-bn} and {@code b-bn} to {@code b-an}.
	 *
	 * <h3>Requirements/notes:</h3>
	 * Equivalent to invoking {@code rewireEdgeAt(a, bn, an);} followed by
	 * {@code rewireEdgeAt(b, an, bn);} but avoids the additional allocations of
	 * those helper methods.
	 *
	 * @param a  the first node
	 * @param an the neighbour of {@code a} to replace
	 * @param b  the second node
	 * @param bn the neighbour of {@code b} to replace
	 * @return {@code true} if the swap succeeded
	 */
	private boolean swapEdges(int a, int an, int b, int bn) {
		if (a == bn || b == an || an == bn)
			return false;
		if (isNeighborOf(a, bn) || isNeighborOf(b, an))
			return false;

		int[] aout = out[a];
		int ai = -1;
		while (aout[++ai] != an) {
		}
		aout[ai] = bn;
		int[] bout = out[b];
		int bi = -1;
		while (bout[++bi] != bn) {
		}
		bout[bi] = an;

		int[] ain = in[a];
		ai = -1;
		while (ain[++ai] != an) {
		}
		ain[ai] = bn;
		int[] bin = in[b];
		bi = -1;
		while (bin[++bi] != bn) {
		}
		bin[bi] = an;

		aout = out[an];
		ai = -1;
		while (aout[++ai] != a) {
		}
		aout[ai] = b;
		bout = out[bn];
		bi = -1;
		while (bout[++bi] != b) {
		}
		bout[bi] = a;

		ain = in[an];
		ai = -1;
		while (ain[++ai] != a) {
		}
		ain[ai] = b;
		bin = in[bn];
		bi = -1;
		while (bin[++bi] != b) {
		}
		bin[bi] = a;
		return true;
	}
}
