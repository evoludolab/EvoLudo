package org.evoludo.simulator.geom;

import org.evoludo.simulator.EvoLudo;

/**
 * Geometry implementation for the strong suppressor graphs of Giakkoupis
 * (2016).
 * 
 * @see <a href="https://doi.org/10.48550/arXiv.1611.01585">Giakkoupis (2016)
 *      Amplifiers and Suppressors of Selection...</a>
 */
public class StrongSuppressorGeometry extends AbstractGeometry {

	/**
	 * Create a strong suppressor geometry tied to the given engine.
	 *
	 * @param engine EvoLudo pacemaker
	 */
	public StrongSuppressorGeometry(EvoLudo engine) {
		super(engine);
		setType(Type.STRONG_SUPPRESSOR);
	}

	/**
	 * Generates a strong undirected suppressor of selection. Population size obeys
	 * \(N=n^2(1+n(1+n))=n^2+n^3+n^4\) for integer \(n\) with three node types
	 * \(U=n^3\), \(V=n^4\) and \(W=n^2\). Each node in \(V\) connects to one node
	 * in \(U\) and to all nodes in \(W\).
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
		int unit = (int) Math.floor(Math.pow(size, 0.25));
		int v0 = 0;
		int vn = (int) Math.pow(unit, 4);
		int w0 = vn;
		int wn = vn + unit * unit;
		int u0 = wn;
		for (int v = v0; v < vn; v++) {
			int u = u0 + (v - v0) / unit;
			addEdgeAt(v, u);
			for (int w = w0; w < wn; w++)
				addEdgeAt(v, w);
		}
	}

	@Override
	protected boolean checkSettings() {
		int unit = (int) Math.floor(Math.pow(size, 0.25));
		int required = unit * unit * (1 + unit * (1 + unit));
		return enforceSize(required);
	}
}
