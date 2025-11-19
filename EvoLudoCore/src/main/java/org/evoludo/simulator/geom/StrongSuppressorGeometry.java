package org.evoludo.simulator.geom;

import org.evoludo.simulator.EvoLudo;
import org.evoludo.simulator.modules.Module;

public class StrongSuppressorGeometry extends AbstractGeometry {

	public StrongSuppressorGeometry(EvoLudo engine) {
		super(engine);
		setType(Type.STRONG_SUPPRESSOR);
	}

	public StrongSuppressorGeometry(EvoLudo engine, Module<?> module) {
		super(engine, module);
		setType(Type.STRONG_SUPPRESSOR);
	}

	public StrongSuppressorGeometry(EvoLudo engine, Module<?> popModule, Module<?> oppModule) {
		super(engine, popModule, oppModule);
		setType(Type.STRONG_SUPPRESSOR);
	}

	public void parse(String arg) {
	}

	@Override
	public void init() {
		isRewired = false;
		isUndirected = true;
		isRegular = false;
		alloc();
		int unit = (int) Math.floor(Math.pow(size, 0.25));
		int v0 = 0;
		int vn = (int) Math.pow(unit, 4);
		int w0 = vn;
		int wn = vn + unit * unit;
		int u0 = wn;
		for (int v = v0; v < vn; v++) {
			int u = u0 + (v - v0) / unit;
			addEdgeAt(v, u);
			for (int w = w0; w < wn; w++)
				addEdgeAt(v, w);
		}
	}

	@Override
	protected boolean checkSettings() {
		int unit = (int) Math.floor(Math.pow(size, 0.25));
		int required = unit * unit * (1 + unit * (1 + unit));
		return enforceSize(required);
	}
}
