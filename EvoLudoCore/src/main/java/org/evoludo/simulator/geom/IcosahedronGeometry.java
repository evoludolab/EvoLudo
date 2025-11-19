package org.evoludo.simulator.geom;

import org.evoludo.simulator.EvoLudo;
import org.evoludo.simulator.modules.Module;

public class IcosahedronGeometry extends AbstractGeometry {

	public IcosahedronGeometry(EvoLudo engine) {
		super(engine);
		setType(Type.ICOSAHEDRON);
	}

	public IcosahedronGeometry(EvoLudo engine, Module<?> module) {
		super(engine, module);
		setType(Type.ICOSAHEDRON);
	}

	public IcosahedronGeometry(EvoLudo engine, Module<?> popModule, Module<?> oppModule) {
		super(engine, popModule, oppModule);
		setType(Type.ICOSAHEDRON);
	}

	@Override
	public void init() {
		isRewired = false;
		isUndirected = true;
		isRegular = true;
		alloc();
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
