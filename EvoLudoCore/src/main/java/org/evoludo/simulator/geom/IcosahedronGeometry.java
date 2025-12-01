package org.evoludo.simulator.geom;

import org.evoludo.simulator.EvoLudo;

/**
 * Geometry implementation for the icosahedral graph (12 nodes, degree 5).
 */
public class IcosahedronGeometry extends AbstractGeometry {

	/**
	 * Create an icosahedral geometry tied to the given engine.
	 *
	 * @param engine EvoLudo pacemaker
	 */
	public IcosahedronGeometry(EvoLudo engine) {
		super(engine);
		setType(GeometryType.ICOSAHEDRON);
	}

	/**
	 * Generates an icosahedron graph: a symmetric graph with \(12\) nodes and
	 * degree \(5\).
	 *
	 * @see <a href="https://en.wikipedia.org/wiki/Icosahedron_graph">Wikipedia:
	 *      Icosahedron graph</a>
	 */
	@Override
	public void init() {
		isRewired = false;
		isUndirected = true;
		isRegular = true;

		for (int i = 1; i < size; i++)
			addEdgeAt(i, i - 1);
		addEdgeAt(0, 4);
		addEdgeAt(0, 5);
		addEdgeAt(0, 6);
		addEdgeAt(1, 6);
		addEdgeAt(1, 7);
		addEdgeAt(1, 8);
		addEdgeAt(2, 0);
		addEdgeAt(2, 4);
		addEdgeAt(2, 8);
		addEdgeAt(3, 8);
		addEdgeAt(3, 9);
		addEdgeAt(3, 10);
		addEdgeAt(4, 10);
		addEdgeAt(5, 10);
		addEdgeAt(5, 11);
		addEdgeAt(6, 11);
		addEdgeAt(7, 9);
		addEdgeAt(7, 11);
		addEdgeAt(9, 11);
	}

	@Override
	protected boolean checkSettings() {
		boolean doReset = enforceSize(12);
		connectivity = 5.0;
		return doReset;
	}
}
