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

	public void parse(String arg) {
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
		addEdgeAt(0, 8);
		addEdgeAt(1, 6);
		addEdgeAt(2, 10);
		addEdgeAt(3, 7);
		addEdgeAt(5, 11);
		addEdgeAt(9, 11);
	}
}
