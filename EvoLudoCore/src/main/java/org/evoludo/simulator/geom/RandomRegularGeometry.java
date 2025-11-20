package org.evoludo.simulator.geom;

import java.util.Arrays;

import org.evoludo.simulator.EvoLudo;
import org.evoludo.simulator.modules.Module;

public class RandomRegularGeometry extends AbstractNetwork {

	private static final int MAX_TRIALS = 10;

	public RandomRegularGeometry(EvoLudo engine) {
		super(engine);
		setType(Type.RANDOM_REGULAR_GRAPH);
	}

	public RandomRegularGeometry(EvoLudo engine, Module<?> module) {
		super(engine, module);
		setType(Type.RANDOM_REGULAR_GRAPH);
	}

	public RandomRegularGeometry(EvoLudo engine, Module<?> popModule, Module<?> oppModule) {
		super(engine, popModule, oppModule);
		setType(Type.RANDOM_REGULAR_GRAPH);
	}

	public boolean parse(String arg) {
		if (arg != null && !arg.isEmpty())
			connectivity = Math.max(2, Integer.parseInt(arg));
		return true;
	}

	/**
	 * Generates a connected undirected random regular graph with degree equal to
	 * the requested connectivity, retrying construction if necessary.
	 */
	@Override
	public void init() {
		isRegular = true;
		int[] degrees = new int[size];
		Arrays.fill(degrees, (int) connectivity);
		int trials = 0;
		while (!initGeometryDegreeDistr(degrees) && ++trials < MAX_TRIALS)
			;
		if (trials >= MAX_TRIALS)
			throw new IllegalStateException("Failed to construct random regular graph");
	}

	@Override
	protected boolean checkSettings() {
		connectivity = Math.min(Math.floor(connectivity), size - 1.0);
		int nConn = (int) connectivity;
		if ((size * nConn) % 2 == 1 && setSize(size + 1)) {
			if (engine.getModule().cloNPopulation.isSet())
				warn("requires even link count - increasing size to " + size + "!");
			return true;
		}
		return false;
	}

}
