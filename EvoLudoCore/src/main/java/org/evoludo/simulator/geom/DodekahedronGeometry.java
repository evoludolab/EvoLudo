package org.evoludo.simulator.geom;

import org.evoludo.simulator.EvoLudo;

/**
 * Geometry implementation for the dodecahedral graph.
 * 
 * @see <a href="https://en.wikipedia.org/wiki/Dodecahedral_graph">Wikipedia:
 *      Dodecahedral graph</a>
 */
public class DodekahedronGeometry extends AbstractGeometry {

	/**
	 * Create a dodecahedral geometry tied to the given engine.
	 *
	 * @param engine EvoLudo pacemaker
	 */
	public DodekahedronGeometry(EvoLudo engine) {
		super(engine);
		setType(GeometryType.DODEKAHEDRON);
	}

	/**
	 * Generates a dodecahedron graph: a cubic symmetric graph with \(20\) nodes.
	 *
	 * @see <a href="https://en.wikipedia.org/wiki/Dodecahedral_graph">Wikipedia:
	 *      Dodecahedral graph</a>
	 */
	@Override
	public void init() {
		isRewired = false;
		isUndirected = true;
		isRegular = true;
		for (int i = 0; i < size; i += 2) {
			addEdgeAt(i, (size + i - 2) % size);
			addEdgeAt(i, i + 1);
		}
		addEdgeAt(1, 5);
		addEdgeAt(3, 7);
		addEdgeAt(5, 9);
		addEdgeAt(7, 11);
		addEdgeAt(9, 13);
		addEdgeAt(11, 15);
		addEdgeAt(13, 17);
		addEdgeAt(15, 19);
		addEdgeAt(17, 1);
		addEdgeAt(19, 3);
	}

	@Override
	protected boolean checkSettings() {
		boolean doReset = enforceSize(20);
		connectivity = 3.0;
		return doReset;
	}
}
