package org.evoludo.simulator.geom;

import org.evoludo.simulator.EvoLudo;
import org.evoludo.simulator.modules.Module;

public class DesarguesGeometry extends AbstractGeometry {

	public DesarguesGeometry(EvoLudo engine) {
		super(engine);
		setType(Type.DESARGUES);
	}

	public DesarguesGeometry(EvoLudo engine, Module<?> module) {
		super(engine, module);
		setType(Type.DESARGUES);
	}

	public DesarguesGeometry(EvoLudo engine, Module<?> popModule, Module<?> oppModule) {
		super(engine, popModule, oppModule);
		setType(Type.DESARGUES);
	}

	public void parse(String arg) {
	}

	@Override
	public void init() {
		isRewired = false;
		isUndirected = true;
		isRegular = true;
		alloc();
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
}
