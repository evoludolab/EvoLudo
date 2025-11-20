package org.evoludo.simulator.geom;

import org.evoludo.simulator.EvoLudo;
import org.evoludo.simulator.modules.Module;

public class TietzeGeometry extends AbstractGeometry {

	public TietzeGeometry(EvoLudo engine) {
		super(engine);
		setType(Type.TIETZE);
	}

	public TietzeGeometry(EvoLudo engine, Module<?> module) {
		super(engine, module);
		setType(Type.TIETZE);
	}

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
