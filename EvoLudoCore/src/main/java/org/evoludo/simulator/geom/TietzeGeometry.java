package org.evoludo.simulator.geom;

import org.evoludo.simulator.EvoLudo;
import org.evoludo.simulator.modules.Module;

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
		setType(Type.TIETZE);
	}

	/**
	 * Create a Tietze geometry for the provided module.
	 *
	 * @param engine EvoLudo pacemaker
	 * @param module owning module
	 */
	public TietzeGeometry(EvoLudo engine, Module<?> module) {
		super(engine, module);
		setType(Type.TIETZE);
	}

	/**
	 * Create a Tietze geometry for the specified populations.
	 *
	 * @param engine    EvoLudo pacemaker
	 * @param popModule focal population module
	 * @param oppModule opponent population module
	 */
	public TietzeGeometry(EvoLudo engine, Module<?> popModule, Module<?> oppModule) {
		super(engine, popModule, oppModule);
		setType(Type.TIETZE);
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
		alloc();
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
