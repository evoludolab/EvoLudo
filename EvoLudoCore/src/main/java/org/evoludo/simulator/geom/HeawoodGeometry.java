package org.evoludo.simulator.geom;

import org.evoludo.simulator.EvoLudo;
import org.evoludo.simulator.modules.Module;

public class HeawoodGeometry extends AbstractGeometry {

	public HeawoodGeometry(EvoLudo engine) {
		super(engine);
		setType(Type.HEAWOOD);
	}

	public HeawoodGeometry(EvoLudo engine, Module<?> module) {
		super(engine, module);
		setType(Type.HEAWOOD);
	}

	public HeawoodGeometry(EvoLudo engine, Module<?> popModule, Module<?> oppModule) {
		super(engine, popModule, oppModule);
		setType(Type.HEAWOOD);
	}

	public void parse(String arg) {
	}

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
