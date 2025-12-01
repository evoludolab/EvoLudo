package org.evoludo.simulator.geom;

import org.evoludo.simulator.EvoLudo;

/**
 * Geometry implementation for the Desargues (Truncated Petersen) graph.
 * 
 * @see <a href="https://en.wikipedia.org/wiki/Desargues_graph">Wikipedia:
 *      Desargues graph</a>
 */
public class DesarguesGeometry extends AbstractGeometry {

	/**
	 * Create a Desargues geometry tied to the given engine.
	 *
	 * @param engine EvoLudo pacemaker
	 */
	public DesarguesGeometry(EvoLudo engine) {
		super(engine);
		setType(GeometryType.DESARGUES);
	}

	/**
	 * Generates the Desargues graph (also known as the Truncated Petersen graph),
	 * a symmetric cubic graph with \(20\) nodes.
	 *
	 * @see <a href="https://en.wikipedia.org/wiki/Desargues_graph">Wikipedia:
	 *      Desargues graph</a>
	 */
	@Override
	public void init() {
		isRewired = false;
		isUndirected = true;
		isRegular = true;
		for (int i = 0; i < size; i++)
			addEdgeAt(i, (size + i - 1) % size);
		addEdgeAt(0, 9);
		addEdgeAt(1, 12);
		addEdgeAt(2, 7);
		addEdgeAt(3, 18);
		addEdgeAt(4, 13);
		addEdgeAt(5, 16);
		addEdgeAt(6, 11);
		addEdgeAt(8, 17);
		addEdgeAt(10, 15);
		addEdgeAt(14, 19);
	}

	@Override
	protected boolean checkSettings() {
		boolean doReset = enforceSize(20);
		connectivity = 3.0;
		return doReset;
	}
}
