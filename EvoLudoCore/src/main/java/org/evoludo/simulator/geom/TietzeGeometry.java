package org.evoludo.simulator.geom;

import org.evoludo.simulator.EvoLudo;

/**
 * Geometry implementation for the Tietze graph (a cubic 12-node graph).
 *
 * @see <a href="https://en.wikipedia.org/wiki/Tietze's_graph">Wikipedia:
 *      Tietze's graph</a>
 */
public class TietzeGeometry extends AbstractGeometry {

	/**
	 * Create a Tietze geometry tied to the given engine.
	 *
	 * @param engine EvoLudo pacemaker
	 */
	public TietzeGeometry(EvoLudo engine) {
		super(engine);
		setType(GeometryType.TIETZE);
	}

	/**
	 * Generates Tietze's graph, a cubic graph with \(12\) nodes and automorphisms
	 * corresponding to the symmetries of a hexagon.
	 *
	 * @see <a href="https://en.wikipedia.org/wiki/Tietze's_graph">Wikipedia:
	 *      Tietze's graph</a>
	 */
	@Override
	public void init() {
		isRewired = false;
		isUndirected = true;
		isRegular = true;

		for (int i = 1; i < size; i++)
			addEdgeAt(i, i - 1);
		addEdgeAt(0, 4);
		addEdgeAt(0, 8);
		addEdgeAt(1, 6);
		addEdgeAt(2, 10);
		addEdgeAt(3, 7);
		addEdgeAt(5, 11);
		addEdgeAt(9, 11);
	}

	@Override
	protected boolean checkSettings() {
		boolean doReset = enforceSize(12);
		connectivity = 3.0;
		return doReset;
	}
}
