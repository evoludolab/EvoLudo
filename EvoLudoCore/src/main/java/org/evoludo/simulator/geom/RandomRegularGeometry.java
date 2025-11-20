package org.evoludo.simulator.geom;

import java.util.Arrays;

import org.evoludo.simulator.EvoLudo;
import org.evoludo.simulator.modules.Module;

/**
 * Random regular graph geometry that repeatedly samples degree distributions
 * until a connected realization is found.
 */
public class RandomRegularGeometry extends AbstractNetwork {

	/**
	 * Number of attempts before giving up on constructing the desired graph.
	 */
	private static final int MAX_TRIALS = 10;

	/**
	 * Create a random regular geometry tied to the given engine.
	 *
	 * @param engine EvoLudo pacemaker
	 */
	public RandomRegularGeometry(EvoLudo engine) {
		super(engine);
		setType(Type.RANDOM_REGULAR_GRAPH);
	}

	/**
	 * Create a random regular geometry for the provided module.
	 *
	 * @param engine EvoLudo pacemaker
	 * @param module owning module
	 */
	public RandomRegularGeometry(EvoLudo engine, Module<?> module) {
		super(engine, module);
		setType(Type.RANDOM_REGULAR_GRAPH);
	}

	/**
	 * Create a random regular geometry for the specified populations.
	 *
	 * @param engine    EvoLudo pacemaker
	 * @param popModule focal population module
	 * @param oppModule opponent population module
	 */
	public RandomRegularGeometry(EvoLudo engine, Module<?> popModule, Module<?> oppModule) {
		super(engine, popModule, oppModule);
		setType(Type.RANDOM_REGULAR_GRAPH);
	}

	@Override
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
