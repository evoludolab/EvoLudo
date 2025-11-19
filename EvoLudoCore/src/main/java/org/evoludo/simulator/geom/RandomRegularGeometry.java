package org.evoludo.simulator.geom;

import java.util.Arrays;

import org.evoludo.simulator.EvoLudo;
import org.evoludo.simulator.modules.Module;

public class RandomRegularGeometry extends AbstractGeometry {

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

	public void parse(String arg) {
		if (arg != null && !arg.isEmpty())
			connectivity = Math.max(2, Integer.parseInt(arg));
	}

	@Override
	public void init() {
		isRegular = true;
		int[] degrees = new int[size];
		Arrays.fill(degrees, (int) connectivity);
		int trials = 0;
		while (!initGeometryDegreeDistr(degrees) && ++trials < MAX_TRIALS) {
		}
		if (trials >= MAX_TRIALS)
			throw new IllegalStateException("Failed to construct random regular graph");
	}

	@Override
	protected boolean checkSettings() {
		connectivity = Math.min(Math.floor(connectivity), size - 1);
		int nConn = (int) connectivity;
		if ((size * nConn) % 2 == 1 && setSize(size + 1)) {
			if (engine.getModule().cloNPopulation.isSet())
				warn("requires even link count - increasing size to " + size + "!");
			return true;
		}
		return false;
	}

	private boolean initGeometryDegreeDistr(int[] degree) {
		int todo;
		int[] core = new int[size];
		int[] full = new int[size];
		for (int n = 0; n < size; n++)
			core[n] = n;
		todo = size;
		for (int n = 0; n < size; n++)
			full[n] = -1;
		int escape = 0;
		a: while (todo > 1) {
			int idxa = engine.getRNG().random0n(todo);
			int nodea = core[idxa];
			int idxb = engine.getRNG().random0n(todo - 1);
			if (idxb >= idxa)
				idxb++;
			int nodeb = core[idxb];
			boolean hasEdge = false;
			for (int j = 0; j < kout[nodea]; j++)
				if (out[nodea][j] == nodeb) {
					hasEdge = true;
					break;
				}
			if (hasEdge) {
				if (!rewireNeighbourEdge(nodea, nodeb, full, size - todo))
					continue;
			} else {
				addEdgeAt(nodea, nodeb);
			}
			if (++escape > 10)
				return false;
			if (kout[nodea] == degree[nodea]) {
				full[size - todo] = nodea;
				core[idxa] = core[--todo];
				if (idxb == todo)
					idxb = idxa;
			}
			if (kout[nodeb] == degree[nodeb]) {
				full[size - todo] = nodeb;
				core[idxb] = core[--todo];
			}
		}
		if (todo == 1) {
			int nodea = core[0];
			int idxc = engine.getRNG().random0n(size - 1);
			int nodec = full[idxc];
			int noded = out[nodec][engine.getRNG().random0n(kout[nodec])];
			if (noded != nodea && !isNeighborOf(nodea, nodec) && !isNeighborOf(nodea, noded)) {
				removeEdgeAt(nodec, noded);
				addEdgeAt(nodea, nodec);
				addEdgeAt(nodea, noded);
				return true;
			}
			return false;
		}
		return true;
	}

	private boolean rewireNeighbourEdge(int nodeA, int nodeB, int[] done, int nDone) {
		if (nDone <= 0)
			return false;
		int nodeC = done[engine.getRNG().random0n(nDone)];
		int nodeD = out[nodeC][engine.getRNG().random0n(kout[nodeC])];
		if (nodeD != nodeB && !isNeighborOf(nodeA, nodeC) && !isNeighborOf(nodeB, nodeD)) {
			removeEdgeAt(nodeC, nodeD);
			addEdgeAt(nodeA, nodeC);
			addEdgeAt(nodeB, nodeD);
			return true;
		}
		if (nodeD != nodeA && !isNeighborOf(nodeA, nodeD) && !isNeighborOf(nodeB, nodeC)) {
			removeEdgeAt(nodeC, nodeD);
			addEdgeAt(nodeA, nodeD);
			addEdgeAt(nodeB, nodeC);
			return true;
		}
		return false;
	}
}
