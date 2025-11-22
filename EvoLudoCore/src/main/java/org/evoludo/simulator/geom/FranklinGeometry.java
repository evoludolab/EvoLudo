package org.evoludo.simulator.geom;

import org.evoludo.simulator.EvoLudo;

/**
 * Geometry implementation for the Franklin graph (a 12-node cubic cage).
 * 
 * @see <a href="https://en.wikipedia.org/wiki/Franklin_graph">Wikipedia:
 *      Franklin graph</a>
 */
public class FranklinGeometry extends AbstractGeometry {

	/**
	 * Create a Franklin geometry tied to the given engine.
	 *
	 * @param engine EvoLudo pacemaker
	 */
	public FranklinGeometry(EvoLudo engine) {
		super(engine);
		setType(GeometryType.FRANKLIN);
	}

	/**
	 * Generates the Franklin graph, a cubic cage with \(12\) nodes discovered by
	 * Philip Franklin.
	 *
	 * @see <a href="https://en.wikipedia.org/wiki/Franklin_graph">Wikipedia:
	 *      Franklin graph</a>
	 */
	@Override
	public void init() {
		isRewired = false;
		isUndirected = true;
		isRegular = true;
		alloc();
		for (int i = 1; i < size; i++)
			addEdgeAt(i, i - 1);
		addEdgeAt(0, size - 1);
		addEdgeAt(0, 7);
		addEdgeAt(1, 6);
		addEdgeAt(2, 9);
		addEdgeAt(3, 8);
		addEdgeAt(4, 11);
		addEdgeAt(5, 10);
	}

	@Override
	protected boolean checkSettings() {
		boolean doReset = enforceSize(12);
		connectivity = 3.0;
		return doReset;
	}
}
