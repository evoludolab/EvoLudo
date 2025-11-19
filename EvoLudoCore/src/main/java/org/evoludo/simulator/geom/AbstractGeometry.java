//
// EvoLudo Project
//
// Copyright 2010-2025 Christoph Hauert
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//	http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//
// For publications in any form, you are kindly requested to attribute the
// author and project as follows:
//
//	Hauert, Christoph (<year>) EvoLudo Project, https://www.evoludo.org
//			(doi: 10.5281/zenodo.14591549 [, <version>])
//
//	<year>:    year of release (or download), and
//	<version>: optional version number (as reported in output header
//			or GUI console) to simplify replication of reported results.
//
// The formatting may be adjusted to comply with publisher requirements.
//

package org.evoludo.simulator.geom;

import java.util.Arrays;
import java.util.logging.Logger;

import org.evoludo.math.ArrayMath;
import org.evoludo.simulator.EvoLudo;
import org.evoludo.simulator.Network2D;
import org.evoludo.simulator.Network3D;
import org.evoludo.simulator.models.IBS;
import org.evoludo.simulator.models.IBSPopulation;
import org.evoludo.simulator.modules.Module;

/**
 * Abstract base class for future geometry implementations. The original
 * {@link org.evoludo.simulator.Geometry} remains untouched so existing builds
 * stay functional while the refactor progresses.
 *
 * <p>
 * This class currently contains only the core state and helper methods that are
 * required by {@link WellmixedGeometry}. Additional behaviour will be migrated
 * from {@code Geometry} as further geometries are extracted.
 */
public abstract class AbstractGeometry {

	/**
	 * Factory method for creating geometry instances by type.
	 * 
	 * @param type   geometry type to instantiate
	 * @param engine pacemaker used by the geometry
	 * @return the instantiated geometry
	 */
	public static AbstractGeometry create(Type type, EvoLudo engine) {
		if (type == null)
			throw new IllegalArgumentException("type must not be null");
		switch (type) {
			case MEANFIELD:
				return new WellmixedGeometry(engine);
			case COMPLETE:
				return new CompleteGeometry(engine);
			case LINEAR:
				return new LinearGeometry(engine);
			default:
				throw new UnsupportedOperationException("Geometry type '" + type + "' is not implemented yet.");
		}
	}

	/**
	 * Empty link arrays used for well-mixed geometries and while allocating
	 * networks lazily.
	 */
	protected static final int[] EMPTY_LINKS = new int[0];

	/**
	 * The pacemaker of all models. Interface with the outside world.
	 */
	protected final EvoLudo engine;

	/**
	 * Logger for keeping track of and reporting events and issues.
	 */
	protected final Logger logger;

	/**
	 * The IBS population that has this geometry.
	 */
	protected IBSPopulation<?, ?> population;

	/**
	 * The IBS population representing the opponent. For intra-species interactions
	 * {@code population==opponent} holds.
	 */
	protected IBSPopulation<?, ?> opponent;

	/**
	 * Local storage for the 2D representation of this graph.
	 */
	private Network2D network2D = null;

	/**
	 * Local storage for the 3D representation of this graph.
	 */
	private Network3D network3D = null;

	/**
	 * Optional descriptive name.
	 */
	private String name;

	/**
	 * Current geometry type handled by this instance.
	 */
	protected Type type = Type.MEANFIELD;

	/**
	 * The number of nodes in the graph.
	 */
	protected int size = -1;

	/**
	 * Flag indicating whether boundaries are fixed or periodic (default).
	 */
	public boolean fixedBoundary = false;

	/**
	 * Flag indicating whether the network structure is undirected.
	 */
	protected boolean isUndirected = true;

	/**
	 * {@code true} if the network structure is regular (all nodes have the same
	 * number of neighbours).
	 */
	public boolean isRegular = false;

	/**
	 * {@code true} if rewiring should be applied.
	 */
	public boolean isRewired = false;

	/**
	 * {@code true} if geometry has been evaluated.
	 */
	private boolean evaluated = false;

	/**
	 * {@code true} if the network structure has been successfully initialized.
	 */
	public boolean isValid = false;

	/**
	 * Convenience flag denoting whether intra- and interspecific competitions are
	 * identical.
	 */
	public boolean interCompSame = true;

	/**
	 * Connectivity (average number of neighbors).
	 */
	public double connectivity = -1.0;

	/**
	 * Probability for rewiring.
	 */
	public double pRewire = -1.0;

	/**
	 * Probability for adding new links.
	 */
	public double pAddwire = -1.0;

	/**
	 * The exponent for scale-free networks (placeholder for future extractions).
	 */
	public double sfExponent = -2.0;

	/**
	 * {@code true} if the network structure is ephemeral.
	 */
	public boolean isDynamic = false;

	/**
	 * The array storing the neighbourhood of each node by listing the indices of
	 * nodes that connect to this one.
	 */
	public int[][] in = null;

	/**
	 * The array storing the neighbourhood of each node by listing the indices of
	 * nodes that this one connects to.
	 */
	public int[][] out = null;

	/**
	 * The array storing the number of incoming neighbours for each node.
	 */
	public int[] kin = null;

	/**
	 * The array storing the number of outgoing neighbours for each node.
	 */
	public int[] kout = null;

	/**
	 * The minimum number of incoming links.
	 */
	public int minIn = -1;

	/**
	 * The maximum number of incoming links.
	 */
	public int maxIn = -1;

	/**
	 * The average number of incoming links.
	 */
	public double avgIn = -1.0;

	/**
	 * The minimum number of outgoing links.
	 */
	public int minOut = -1;

	/**
	 * The maximum number of outgoing links.
	 */
	public int maxOut = -1;

	/**
	 * The average number of outgoing links.
	 */
	public double avgOut = -1.0;

	/**
	 * The minimum sum of incoming and outgoing links.
	 */
	public int minTot = -1;

	/**
	 * The maximum sum of incoming and outgoing links.
	 */
	public int maxTot = -1;

	/**
	 * The average sum of incoming and outgoing links.
	 */
	public double avgTot = -1.0;

	protected AbstractGeometry(EvoLudo engine) {
		this.engine = engine;
		this.logger = engine.getLogger();
	}

	protected AbstractGeometry(EvoLudo engine, Module<?> module) {
		this(engine, module, module);
	}

	protected AbstractGeometry(EvoLudo engine, Module<?> popModule, Module<?> oppModule) {
		this(engine);
		if (engine.getModel().getType().isIBS()) {
			population = popModule.getIBSPopulation();
			opponent = oppModule.getIBSPopulation();
		}
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getName() {
		if (name == null || name.equals("Structure"))
			return "";
		if (name.endsWith(": Structure"))
			return name.substring(0, name.length() - ": Structure".length());
		return name;
	}

	public Type getType() {
		return type;
	}

	protected void setType(Type type) {
		this.type = type;
	}

	public void setNetwork2D(Network2D net) {
		network2D = net;
	}

	public Network2D getNetwork2D() {
		return network2D;
	}

	public void setNetwork3D(Network3D net) {
		network3D = net;
	}

	public Network3D getNetwork3D() {
		return network3D;
	}

	public boolean setSize(int size) {
		if (size <= 0 || this.size == size)
			return false;
		this.size = size;
		return true;
	}

	public int getSize() {
		return size;
	}

	/**
	 * Reset the network structure and free the allocated memory.
	 */
	public void reset() {
		network2D = null;
		network3D = null;
		name = null;
		in = null;
		out = null;
		kin = null;
		kout = null;
		size = -1;
		type = Type.MEANFIELD;
		fixedBoundary = false;
		minIn = -1;
		maxIn = -1;
		avgIn = -1.0;
		minOut = -1;
		maxOut = -1;
		avgOut = -1.0;
		minTot = -1;
		maxTot = -1;
		avgTot = -1.0;
		connectivity = -1.0;
		pRewire = -1.0;
		pAddwire = -1.0;
		isUndirected = true;
		isRewired = false;
		interCompSame = true;
		isDynamic = false;
		isRegular = false;
		isValid = false;
		evaluated = false;
	}

	/**
	 * Allocate the memory necessary to store the network structure.
	 */
	protected void alloc() {
		if (size <= 0)
			throw new IllegalStateException("size must be set before allocating geometry");
		if (in == null || in.length != size) {
			in = new int[size][];
			kin = new int[size];
		}
		if (out == null || out.length != size) {
			out = new int[size][];
			kout = new int[size];
		}
		if (isUndirected && isRegular && connectivity >= 0.0) {
			int k = (int) (connectivity + 0.5);
			for (int i = 0; i < size; i++) {
				in[i] = new int[k];
				out[i] = new int[k];
			}
		} else {
			Arrays.fill(in, EMPTY_LINKS);
			Arrays.fill(out, EMPTY_LINKS);
		}
		Arrays.fill(kin, 0);
		Arrays.fill(kout, 0);
		evaluated = false;
	}

	/**
	 * Evaluate geometry. Convenience method to set frequently used quantities such
	 * as {@code minIn, maxOut, avgTot} etc.
	 */
	public void evaluate() {
		if (evaluated && !isDynamic)
			return;
		if (type == Type.MEANFIELD || kout == null || kin == null) {
			maxOut = 0;
			maxIn = 0;
			maxTot = 0;
			minOut = 0;
			minIn = 0;
			minTot = 0;
			avgOut = 0.0;
			avgIn = 0.0;
			avgTot = 0.0;
			evaluated = true;
			return;
		}
		maxOut = -1;
		maxIn = -1;
		maxTot = -1;
		minOut = Integer.MAX_VALUE;
		minIn = Integer.MAX_VALUE;
		minTot = Integer.MAX_VALUE;
		long sumin = 0, sumout = 0, sumtot = 0;
		for (int n = 0; n < size; n++) {
			int lout = kout[n];
			maxOut = Math.max(maxOut, lout);
			minOut = Math.min(minOut, lout);
			sumout += lout;
			int lin = kin[n];
			maxIn = Math.max(maxIn, lin);
			minIn = Math.min(minIn, lin);
			sumin += lin;
			int ltot = lout + lin;
			maxTot = Math.max(maxTot, ltot);
			minTot = Math.min(minTot, ltot);
			sumtot += ltot;
		}
		avgOut = (double) sumout / (double) size;
		avgIn = (double) sumin / (double) size;
		avgTot = (double) sumtot / (double) size;
		evaluated = true;
	}

	/**
	 * Utility method to determine whether a given geometry type is a lattice.
	 * 
	 * @return {@code true} if {@link #getType()} is lattice, {@code false}
	 *         otherwise.
	 */
	public boolean isLattice() {
		if (isRewired)
			return false;
		return type.isLattice();
	}

	/**
	 * Check if graph is connected.
	 *
	 * @return {@code true} if graph is connected
	 */
	public boolean isGraphConnected() {
		if (out == null || kout == null)
			return true;
		boolean[] check = new boolean[size];
		Arrays.fill(check, false);
		return isGraphConnected(0, check);
	}

	private boolean isGraphConnected(int node, boolean[] check) {
		check[node] = true;
		int[] neighs = out[node];
		int len = kout[node];
		for (int i = 0; i < len; i++) {
			int nn = neighs[i];
			if (!check[nn])
				isGraphConnected(nn, check);
		}
		return ArrayMath.max(check);
	}

	/**
	 * Gets the opponent of the population represented by this geometry.
	 */
	public IBSPopulation<?, ?> getOpponent() {
		return opponent;
	}

	/**
	 * @return {@code true} if this geometry links two different populations.
	 */
	protected boolean isInterspecies() {
		return population != opponent;
	}

	/**
	 * Add undirected edge by inserting links in both directions.
	 */
	public void addEdgeAt(int from, int to) {
		addLinkAt(from, to);
		addLinkAt(to, from);
	}

	/**
	 * Add a directed link from {@code from} to {@code to}, allocating storage as
	 * needed.
	 */
	public void addLinkAt(int from, int to) {
		int[] mem = out[from];
		int max = mem.length;
		int ko = kout[from];
		if (max <= ko) {
			int[] newmem = new int[max + 10];
			if (max > 0)
				System.arraycopy(mem, 0, newmem, 0, max);
			out[from] = newmem;
			mem = newmem;
		}
		mem[ko] = to;
		kout[from]++;
		ko++;
		if (ko > maxOut)
			maxOut = ko;

		mem = in[to];
		max = mem.length;
		int ki = kin[to];
		if (max <= ki) {
			int[] newmem = new int[max + 10];
			if (max > 0)
				System.arraycopy(mem, 0, newmem, 0, max);
			in[to] = newmem;
			mem = newmem;
		}
		mem[ki] = from;
		kin[to]++;
		ki++;
		if (ki > maxIn)
			maxIn = ki;
		maxTot = Math.max(maxTot, ko + kin[from]);
		maxTot = Math.max(maxTot, kout[to] + ki);
		evaluated = false;
	}

	/**
	 * Initialise the geometry.
	 */
	public abstract void init();

}
