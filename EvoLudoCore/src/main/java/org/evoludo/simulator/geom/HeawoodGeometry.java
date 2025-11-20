package org.evoludo.simulator.geom;

import org.evoludo.simulator.EvoLudo;
import org.evoludo.simulator.modules.Module;

/**
 * Geometry implementation for the Heawood graph (14-node cubic symmetric
 * graph).
 *
 * @see <a href="https://en.wikipedia.org/wiki/Heawood_graph">Wikipedia:
 *      Heawood graph</a>
 */
public class HeawoodGeometry extends AbstractGeometry {

	/**
	 * Create a Heawood geometry tied to the given engine.
	 *
	 * @param engine EvoLudo pacemaker
	 */
	public HeawoodGeometry(EvoLudo engine) {
		super(engine);
		setType(Type.HEAWOOD);
	}

	/**
	 * Create a Heawood geometry for the provided module.
	 *
	 * @param engine EvoLudo pacemaker
	 * @param module owning module
	 */
	public HeawoodGeometry(EvoLudo engine, Module<?> module) {
		super(engine, module);
		setType(Type.HEAWOOD);
	}

	/**
	 * Create a Heawood geometry for the specified populations.
	 *
	 * @param engine    EvoLudo pacemaker
	 * @param popModule focal population module
	 * @param oppModule opponent population module
	 */
	public HeawoodGeometry(EvoLudo engine, Module<?> popModule, Module<?> oppModule) {
		super(engine, popModule, oppModule);
		setType(Type.HEAWOOD);
	}

	/**
	 * Generates the Heawood graph, a cubic symmetric graph with \(14\) nodes that
	 * is the point-line incidence graph of the Fano plane.
	 *
	 * @see <a href="https://en.wikipedia.org/wiki/Heawood_graph">Wikipedia:
	 *      Heawood graph</a>
	 */
	@Override
	public void init() {
		isRewired = false;
		isUndirected = true;
		isRegular = true;
		alloc();
		int size = getSize();
		for (int i = 1; i < size; i++)
			addEdgeAt(i, i - 1);
		addEdgeAt(0, size - 1);
		addEdgeAt(0, 5);
		addEdgeAt(2, 7);
		addEdgeAt(4, 9);
		addEdgeAt(6, 11);
		addEdgeAt(8, 13);
		addEdgeAt(10, 1);
		addEdgeAt(12, 3);
	}

	@Override
	protected boolean checkSettings() {
		boolean doReset = enforceSize(14);
		connectivity = 3.0;
		return doReset;
	}
}
