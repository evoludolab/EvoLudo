package org.evoludo.simulator.geom;

import org.evoludo.math.RNGDistribution;
import org.evoludo.simulator.EvoLudo;
import org.evoludo.simulator.modules.Module;

/**
 * Strong undirected amplifier graph based on Giakkoupis (2016).
 */
public class StrongAmplifierGeometry extends AbstractGeometry {

	public StrongAmplifierGeometry(EvoLudo engine) {
		super(engine);
		setType(Type.STRONG_AMPLIFIER);
	}

	public StrongAmplifierGeometry(EvoLudo engine, Module<?> module) {
		super(engine, module);
		setType(Type.STRONG_AMPLIFIER);
	}

	public StrongAmplifierGeometry(EvoLudo engine, Module<?> popModule, Module<?> oppModule) {
		super(engine, popModule, oppModule);
		setType(Type.STRONG_AMPLIFIER);
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
		alloc();
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

	private void initRRGCore(RNGDistribution rng, int start, int end, int degree) {
		int nTodo = end - start;
		int nLinks = nTodo * degree;
		int[] todo = new int[nTodo];
		for (int n = start; n < end; n++)
			todo[n - start] = n;
		int[] active = new int[nTodo];
		int[] done = new int[nTodo];
		int nDone = 0;
		int idxa = rng.random0n(nTodo);
		active[0] = todo[idxa];
		todo[idxa] = todo[--nTodo];
		int nActive = 1;
		while (nTodo > 0) {
			idxa = rng.random0n(nActive);
			int nodea = active[idxa];
			int idxb = rng.random0n(nTodo);
			int nodeb = todo[idxb];
			addEdgeAt(nodea, nodeb);
			if (kout[nodea] == degree) {
				done[nDone++] = nodea;
				active[idxa] = active[--nActive];
			}
			if (kout[nodeb] < degree)
				active[nActive++] = nodeb;
			todo[idxb] = todo[--nTodo];
		}
		todo = active;
		nTodo = nActive;
		nLinks -= 2 * (end - start - 1);
		while (nTodo > 1) {
			int a = rng.random0n(nLinks);
			int b = rng.random0n(nLinks - 1);
			if (b >= a)
				b++;
			int idxNa = 0;
			int nodeA = todo[idxNa];
			a -= degree - kout[nodeA];
			while (a >= 0) {
				nodeA = todo[++idxNa];
				a -= degree - kout[nodeA];
			}
			int idxNb = 0;
			int nodeB = todo[idxNb];
			b -= degree - kout[nodeB];
			while (b >= 0) {
				nodeB = todo[++idxNb];
				b -= degree - kout[nodeB];
			}
			if (nodeA == nodeB)
				continue;
			if (isNeighborOf(nodeA, nodeB)) {
				if (nDone < 1)
					continue;
				if (!rewireNeighbourEdge(rng, nodeA, nodeB, done, nDone))
					continue;
			} else {
				addEdgeAt(nodeA, nodeB);
			}
			nLinks -= 2;
			if (kout[nodeA] == degree)
				todo[idxNa] = todo[--nTodo];
			if (kout[nodeB] == degree)
				todo[idxNb] = todo[--nTodo];
		}
	}

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
