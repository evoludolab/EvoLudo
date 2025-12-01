package org.evoludo.simulator.geom;

import org.evoludo.math.RNGDistribution;
import org.evoludo.simulator.EvoLudo;

/**
 * Strong undirected amplifier graph based on Giakkoupis (2016).
 * 
 * @see <a href="https://doi.org/10.48550/arXiv.1611.01585">Giakkoupis (2016)
 *      Amplifiers and Suppressors of Selection...</a>
 */
public class StrongAmplifierGeometry extends AbstractGeometry {

	/**
	 * Create a strong amplifier geometry tied to the given engine.
	 *
	 * @param engine EvoLudo pacemaker
	 */
	public StrongAmplifierGeometry(EvoLudo engine) {
		super(engine);
		setType(GeometryType.STRONG_AMPLIFIER);
	}

	/**
	 * Generates a strong undirected amplifier of selection as described by
	 * Giakkoupis (2016). Population size satisfies
	 * \(N=n^3+(1+a)n^2\) with integer \(n\) and suitable \(a\geq 5\) and comprises
	 * three node types \(U=n^3\), \(V=n^2\) and \(W=N-U-V\). Nodes in \(U\) are
	 * leaves attached to nodes in \(V\) while nodes in \(V\) connect to \(n^2\)
	 * nodes in the regular core \(W\).
	 *
	 * @see <a href="https://doi.org/10.48550/arXiv.1611.01585">Giakkoupis (2016)
	 *      Amplifiers and Suppressors of Selection...</a>
	 */
	@Override
	public void init() {
		isRewired = false;
		isUndirected = true;
		isRegular = false;
		RNGDistribution rng = engine.getRNG();
		int unit13 = Math.max(5, (int) Math.pow(size / 4, 1.0 / 3.0));
		int unit23 = unit13 * unit13;
		int unit = unit23 * unit13;
		int nU = unit;
		int nV = unit23;
		int nW = size - nU - nV;
		int w0 = 0;
		int wn = nW;
		int v0 = wn;
		int vn = v0 + nV;
		int u0 = vn;
		initRRGCore(rng, w0, wn, unit23);
		int idxU = u0;
		for (int v = v0; v < vn; v++) {
			for (int n = 0; n < unit13; n++)
				addEdgeAt(v, idxU++);
			int l = unit23;
			while (l > 0) {
				int idx = rng.random0n(nW);
				if (isNeighborOf(v, idx))
					continue;
				addEdgeAt(v, idx);
				l--;
			}
		}
	}

	@Override
	protected boolean checkSettings() {
		int unit13 = Math.max(5, (int) Math.pow(size / 4, 1.0 / 3.0));
		int unit23 = unit13 * unit13;
		int unit = unit23 * unit13;
		double lnunit = 3.0 * Math.log(unit13);
		double epsilon = lnunit / unit13;
		double alpha = 3.0 * lnunit / Math.log(1.0 + epsilon);
		int required = (int) (unit + (1 + alpha) * unit23 + 0.5);
		return enforceSize(required);
	}

	/**
	 * Initialise the random-regular core subgraph.
	 *
	 * @param rng    random number generator
	 * @param start  first node index of the core
	 * @param end    (exclusive) end index of the core
	 * @param degree desired degree per node
	 */
	private void initRRGCore(RNGDistribution rng, int start, int end, int degree) {
		int nTodo = end - start;
		int nLinks = nTodo * degree;
		int[] todo = new int[nTodo];
		for (int n = start; n < end; n++)
			todo[n] = n;

		// ensure connectedness for static graphs
		int[] active = new int[nTodo];
		int[] done = new int[nTodo];
		int nDone = 0;
		int idxa = rng.random0n(nTodo);
		active[0] = todo[idxa];
		nTodo--;
		if (idxa != nTodo)
			System.arraycopy(todo, idxa + 1, todo, idxa, nTodo - idxa);
		int nActive = 1;
		while (nTodo > 0) {
			idxa = rng.random0n(nActive);
			int nodea = active[idxa];
			int idxb = rng.random0n(nTodo);
			int nodeb = todo[idxb];
			addEdgeAt(nodea, nodeb);
			if (kout[nodea] == degree) {
				done[nDone++] = nodea;
				nActive--;
				if (idxa != nActive)
					System.arraycopy(active, idxa + 1, active, idxa, nActive - idxa);
			}
			// degree of nodeb not yet reached - add to active list
			if (kout[nodeb] < degree)
				active[nActive++] = nodeb;
			// remove nodeb from core of unconnected nodes
			nTodo--;
			if (idxb != nTodo)
				System.arraycopy(todo, idxb + 1, todo, idxb, nTodo - idxb);
		}
		// now we have a connected graph
		todo = active;
		nTodo = nActive;
		nLinks -= 2 * (end - start - 1);

		// ideally we should go from nTodo=2 to zero but a single node with a different
		// degree is acceptable
		while (nTodo > 1) {
			int a = rng.random0n(nLinks);
			int b = rng.random0n(nLinks - 1);
			if (b >= a)
				b++;

			// identify nodes
			idxa = 0;
			int nodea = todo[idxa];
			a -= degree - kout[nodea];
			while (a >= 0) {
				nodea = todo[++idxa];
				a -= degree - kout[nodea];
			}
			int idxb = 0;
			int nodeb = todo[idxb];
			b -= degree - kout[nodeb];
			while (b >= 0) {
				nodeb = todo[++idxb];
				b -= degree - kout[nodeb];
			}

			if (nodea == nodeb)
				continue;
			if (isNeighborOf(nodea, nodeb)) {
				if (nDone < 1)
					continue;
				if (!rewireNeighbourEdge(rng, nodea, nodeb, done, nDone))
					// cross fingers and try again
					continue;
			} else {
				// A!=B and A-B are not connected
				addEdgeAt(nodea, nodeb);
			}
			nLinks -= 2;
			if (kout[nodea] == degree) {
				done[nDone++] = nodea;
				nTodo--;
				if (idxa != nTodo)
					System.arraycopy(todo, idxa + 1, todo, idxa, nTodo - idxa);
				if (idxb > idxa)
					idxb--;
			}
			if (kout[nodeb] == degree) {
				done[nDone++] = nodeb;
				nTodo--;
				if (idxb != nTodo)
					System.arraycopy(todo, idxb + 1, todo, idxb, nTodo - idxb);
			}
		}
	}

	/**
	 * Attempt to rewire edges so that {@code nodeA} and {@code nodeB} can connect
	 * without duplicating links.
	 *
	 * @param rng   random number generator
	 * @param nodeA first node needing neighbours
	 * @param nodeB second node needing neighbours
	 * @param done  array of already satisfied nodes
	 * @param nDone number of valid entries in {@code done}
	 * @return {@code true} if rewiring succeeded
	 */
	private boolean rewireNeighbourEdge(RNGDistribution rng, int nodeA, int nodeB, int[] done, int nDone) {
		int nodeC = done[rng.random0n(nDone)];
		int nodeD = out[nodeC][rng.random0n(kout[nodeC])];
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
