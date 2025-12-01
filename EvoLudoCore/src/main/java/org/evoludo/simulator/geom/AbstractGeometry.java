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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.evoludo.math.ArrayMath;
import org.evoludo.math.RNGDistribution;
import org.evoludo.simulator.EvoLudo;
import org.evoludo.simulator.Network2D;
import org.evoludo.simulator.Network3D;
import org.evoludo.util.CLOption;
import org.evoludo.util.Formatter;
import org.evoludo.util.Plist;

/**
 * Abstract implementation of the population interaction and competition
 * structures. Instances of {@code AbstractGeometry} describe neighbourhood
 * graphs for IBS, PDE's and graphical representations, such as trait
 * distributions in 1D or 2D.
 */
public abstract class AbstractGeometry {

	/**
	 * Instantiate the requested geometry based on {@code cli}. Parsing of
	 * geometry
	 * specific arguments is left to the created instance via
	 * {@link AbstractGeometry#parse(String)}.
	 *
	 * @param engine the EvoLudo engine providing module/CLI metadata
	 * @param cli    the command line style geometry descriptor
	 * @return the instantiated geometry (arguments not yet parsed)
	 */
	public static AbstractGeometry create(EvoLudo engine, String cli) {
		if (engine == null)
			throw new IllegalArgumentException("engine must not be null");
		if (cli == null || cli.isEmpty())
			throw new IllegalArgumentException("geometry specification must not be empty");
		CLOption clo = engine.getModule().cloGeometry;
		GeometryType type = (GeometryType) clo.match(cli);
		AbstractGeometry geometry = AbstractGeometry.create(engine, type);
		String spec = cli.substring(Math.min(type.key.length(), cli.length()));
		geometry.specs = spec;
		return geometry;
	}

	/**
	 * Factory method for creating geometry instances by type.
	 * 
	 * @param type   geometry type to instantiate
	 * @param engine pacemaker used by the geometry
	 * @return the instantiated geometry
	 */
	public static AbstractGeometry create(EvoLudo engine, GeometryType type) {
		if (type == null)
			throw new IllegalArgumentException("type must not be null");
		switch (type) {
			case WELLMIXED:
				return new WellmixedGeometry(engine);
			case COMPLETE:
				return new CompleteGeometry(engine);
			case LINEAR:
				return new LinearGeometry(engine);
			case TRIANGULAR:
				return new TriangularGeometry(engine);
			case HEXAGONAL:
				return new HexagonalGeometry(engine);
			case SQUARE_NEUMANN:
				return new VonNeumannGeometry(engine);
			case SQUARE_NEUMANN_2ND:
				return new SecondNeighbourGeometry(engine);
			case SQUARE_MOORE:
				return new MooreGeometry(engine);
			case SQUARE:
				return new SquareGeometry(engine);
			case CUBE:
				return new CubicGeometry(engine);
			case STAR:
				return new StarGeometry(engine);
			case SUPER_STAR:
				return new SuperstarGeometry(engine);
			case WHEEL:
				return new WheelGeometry(engine);
			case FRUCHT:
				return new FruchtGeometry(engine);
			case STRONG_AMPLIFIER:
				return new StrongAmplifierGeometry(engine);
			case STRONG_SUPPRESSOR:
				return new StrongSuppressorGeometry(engine);
			case TIETZE:
				return new TietzeGeometry(engine);
			case FRANKLIN:
				return new FranklinGeometry(engine);
			case HEAWOOD:
				return new HeawoodGeometry(engine);
			case ICOSAHEDRON:
				return new IcosahedronGeometry(engine);
			case DODEKAHEDRON:
				return new DodekahedronGeometry(engine);
			case DESARGUES:
				return new DesarguesGeometry(engine);
			case RANDOM_REGULAR_GRAPH:
				return new RandomRegularGeometry(engine);
			case RANDOM_GRAPH:
				return new RandomGeometry(engine);
			case RANDOM_GRAPH_DIRECTED:
				return new RandomDirectedGeometry(engine);
			case HIERARCHY:
				return new HierarchicalGeometry(engine);
			case SCALEFREE:
				return new ScalefreeGeometry(engine);
			case SCALEFREE_BA:
				return new BarabasiAlbertGeometry(engine);
			case SCALEFREE_KLEMM:
				return new KlemmEguiluzGeometry(engine);
			case DYNAMIC:
				return new DynamicGeometry(engine);
			default:
				throw new UnsupportedOperationException("Geometry type '" + type + "' is not implemented yet.");
		}
	}

	/**
	 * Derive competition geometry from current (interaction) geometry for
	 * inter-species interactions with {@code isSingle == true}. This clones
	 * the interaction geometry and simply removes links to self, which corresponds
	 * to interactions with individuals at the same location in the other species.
	 *
	 * @return the derived competition geometry
	 */
	public AbstractGeometry deriveCompetitionGeometry() {
		if (!isSingle)
			throw new IllegalStateException(
					"Cannot derive competition geometry when isSingle == false.");
		AbstractGeometry competition = clone();
		// remove competition with self
		if (!competition.isType(GeometryType.WELLMIXED))
			for (int n = 0; n < size; n++)
				competition.removeLinkAt(n, n);
		return competition;
	}

	/**
	 * Empty link arrays used for well-mixed geometries and while allocating
	 * networks lazily.
	 */
	static final int[] EMPTY_LINKS = new int[0];

	/**
	 * The pacemaker of all models. Interface with the outside world.
	 */
	final EvoLudo engine;

	/**
	 * Logger for keeping track of and reporting events and issues.
	 */
	final Logger logger;

	/**
	 * Local storage for the 2D representation of this graph.
	 */
	Network2D network2D = null;

	/**
	 * Local storage for the 3D representation of this graph.
	 */
	Network3D network3D = null;

	/**
	 * Optional CLI specification used to configure this geometry.
	 */
	String specs;

	/**
	 * Optional descriptive name.
	 */
	String name;

	/**
	 * Current geometry type handled by this instance.
	 */
	GeometryType type = GeometryType.WELLMIXED;

	/**
	 * The number of nodes in the graph.
	 */
	int size = -1;

	/**
	 * Flag indicating whether the network structure is undirected.
	 */
	boolean isUndirected = true;

	/**
	 * {@code true} if the network structure is regular (all nodes have the same
	 * number of neighbours).
	 */
	boolean isRegular = false;

	/**
	 * {@code true} if rewiring should be applied.
	 */
	boolean isRewired = false;

	/**
	 * {@code true} if this geometry links different species/populations.
	 */
	protected boolean isInterspecies = false;

	/**
	 * Lazily computed summary statistics for the geometry.
	 */
	GeometryFeatures features = null;

	/**
	 * {@code true} if the network structure has been successfully initialized.
	 */
	boolean isValid = false;

	/**
	 * Convenience flag denoting whether intra- and interspecific competitions are
	 * identical.
	 */
	boolean isSingle = true;

	/**
	 * Connectivity (average number of neighbors).
	 */
	double connectivity = -1.0;

	/**
	 * Probability for rewiring.
	 */
	double pRewire = -1.0;

	/**
	 * Probability for adding new links.
	 */
	double pAddwire = -1.0;

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
	 * Create a new geometry scaffold linked to the given pacemaker.
	 *
	 * @param engine the EvoLudo engine coordinating simulations
	 */
	protected AbstractGeometry(EvoLudo engine) {
		this.engine = engine;
		this.logger = engine.getLogger();
		this.isInterspecies = engine.getModule().getNSpecies() > 1;
	}

	/**
	 * Set a descriptive name for this geometry (used in UI/tooltips).
	 *
	 * @param name the human-readable name
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * Retrieve the display name of the geometry (sans trailing structural suffixes
	 * such as {@code ": Structure"}).
	 *
	 * @return the display name or the empty string for anonymous structures
	 */
	public String getName() {
		if (name == null || name.equals("Structure"))
			return "";
		if (name.endsWith(": Structure"))
			return name.substring(0, name.length() - ": Structure".length());
		return name;
	}

	/**
	 * Retrieve the name of the key for storing geometry in plist.
	 *
	 * @return the plist key
	 */
	public String getEncodeKey() {
		if (name == null || name.isEmpty())
			return "Structure";
		return name;
	}

	/**
	 * @return the current geometry {@link GeometryType}
	 */
	public GeometryType getType() {
		return type;
	}

	/**
	 * Update the geometry {@link GeometryType}.
	 *
	 * @param type the new type
	 */
	protected void setType(GeometryType type) {
		this.type = type;
	}

	/**
	 * Store the 2D network representation.
	 *
	 * @param net the 2D network
	 */
	public void setNetwork2D(Network2D net) {
		network2D = net;
	}

	/**
	 * @return the stored 2D network representation (may be {@code null})
	 */
	public Network2D getNetwork2D() {
		if (network2D == null)
			network2D = engine.createNetwork2D(this);
		return network2D;
	}

	/**
	 * Store the 3D network representation.
	 *
	 * @param net the 3D network
	 */
	public void setNetwork3D(Network3D net) {
		network3D = net;
	}

	/**
	 * @return the stored 3D network representation (may be {@code null})
	 */
	public Network3D getNetwork3D() {
		if (network3D == null)
			network3D = engine.createNetwork3D(this);
		return network3D;
	}

	/**
	 * Checks whether a single graphical representation can be used for the
	 * interaction and competition graphs. Two distinct graphical representations
	 * are generally required if the two graphs differ but not if they both refer to
	 * the same lattice structure even if connectivities or boundary conditions are
	 * different.
	 *
	 * <h3>Examples:</h3>
	 * A single graphical representation is adequate:
	 * <ol>
	 * <li>if the interaction and competition graphs are identical,
	 * <li>if both the interaction and competition graphs are lattices, even if the
	 * boundary conditions or the connectivities are different,
	 * <li>but not if the interaction and competition graphs are separate instances
	 * of the same random structure, e.g. random regular graphs.
	 * </ol>
	 * <h3>Requirements/notes:</h3>
	 * <ol>
	 * <li>Tooltips need to be careful to report the different graphs and
	 * neighborhoods properly.
	 * </ol>
	 *
	 * @param interaction the interaction geometry (required)
	 * @param competition the competition geometry (optional)
	 * @return {@code true} if a single representation can be reused for both
	 *         structures
	 */
	public static boolean displaySingle(AbstractGeometry interaction, AbstractGeometry competition) {
		if (interaction == null)
			throw new IllegalArgumentException("interaction geometry must not be null");
		GeometryType interactionType = interaction.getType();
		if (interaction.isLattice()) {
			if (competition == null)
				return interaction.isSingle();
			GeometryType competitionType = competition.getType();
			if (interactionType.isSquareLattice() && competitionType.isSquareLattice())
				return true;
			return competitionType == interactionType;
		}
		return interaction.isSingle();
	}

	/**
	 * @return the CLI specification string that configured this geometry, or
	 *         {@code null}
	 */
	public String getSpecs() {
		return specs;
	}

	/**
	 * Parse geometry-specific CLI options.
	 *
	 * @param spec the argument string without the geometry key
	 * @return {@code true} if parsing succeeded, {@code false} if invalid
	 */
	boolean parse(String spec) {
		if (spec == null || spec.isEmpty())
			return true;
		warn("geometry '" + type + "' does not accept parameters - ignoring '" + spec + "'");
		return false;
	}

	/**
	 * Re-parse the stored specification string.
	 *
	 * @return {@code true} if parsing succeeded
	 */
	public boolean parse() {
		return parse(specs);
	}

	/**
	 * Set the size of the network.
	 *
	 * @param size the desired number of nodes
	 * @return {@code true} if the size changed (requiring a reset)
	 */
	public boolean setSize(int size) {
		if (size <= 0 || this.size == size)
			return false;
		this.size = size;
		return true;
	}

	/**
	 * @return the number of nodes in the network
	 */
	public int getSize() {
		return size;
	}

	/**
	 * Convenience helper to check the geometry type.
	 *
	 * @param type the type to compare to
	 * @return {@code true} if {@link #getType()} matches {@code type}
	 */
	public boolean isType(GeometryType type) {
		return getType() == type;
	}

	/**
	 * Sets the probability for rewiring existing links.
	 *
	 * @param probability rewiring probability
	 */
	public void setRewire(double probability) {
		pRewire = probability;
	}

	/**
	 * @return the rewiring probability
	 */
	public double getRewire() {
		return pRewire;
	}

	/**
	 * Sets the probability for adding links to the network.
	 *
	 * @param probability link-addition probability
	 */
	public void setAddwire(double probability) {
		pAddwire = probability;
	}

	/**
	 * @return the link-addition probability
	 */
	public double getAddwire() {
		return pAddwire;
	}

	/**
	 * @return {@code true} if the geometry has been rewired.
	 */
	public boolean isRewired() {
		return isRewired;
	}

	/**
	 * Sets whether a single geometry is used for both interaction and competition
	 * graphs.
	 *
	 * @param single {@code true} if both graphs are identical
	 */
	public void setSingle(boolean single) {
		isSingle = single;
	}

	/**
	 * @return {@code true} if a single geometry suffices for interaction and
	 *         competition structures
	 */
	public boolean isSingle() {
		return isSingle;
	}

	/**
	 * Sets the connectivity (average number of neighbors).
	 *
	 * @param connectivity average degree
	 */
	public void setConnectivity(double connectivity) {
		this.connectivity = connectivity;
	}

	/**
	 * @return the current connectivity (average degree)
	 */
	public double getConnectivity() {
		return connectivity;
	}

	/**
	 * @return {@code true} if this geometry is undirected.
	 */
	public boolean isUndirected() {
		return isUndirected;
	}

	/**
	 * @return {@code true} if this geometry is regular (all nodes have identical
	 *         degree).
	 */
	public boolean isRegular() {
		return isRegular;
	}

	/**
	 * Validate geometry parameters and adjust infeasible settings.
	 *
	 * @return {@code true} if adjustments require a reset
	 */
	public final boolean check() {
		boolean doReset = checkSettings();
		validateRewiring();
		alloc();
		return doReset;
	}

	/**
	 * Hook for subclasses to implement geometry specific checks.
	 *
	 * @return {@code true} if adjustments require a reset
	 */
	protected boolean checkSettings() {
		return false;
	}

	/**
	 * Ensure rewiring parameters are feasible for the current connectivity and
	 * adjust them if needed.
	 */
	private void validateRewiring() {
		if (pRewire <= 0.0)
			return;
		int rounded = (int) (connectivity + 1e-6);
		switch (rounded) {
			case 0:
			case 1:
				warn("rewiring needs higher connectivity (should be >3 instead of "
						+ Formatter.format(connectivity, 2) + ") - ignored");
				pRewire = 0.0;
				break;
			case 2:
				warn("consider higher connectivity for rewiring (should be >3 instead of "
						+ Formatter.format(connectivity, 2) + ")");
				break;
			default:
		}
		if (connectivity > size - 2) {
			warn("complete graph, rewiring impossible - ignored");
			pRewire = 0.0;
		} else if (connectivity > size / 2) {
			warn("consider lower connectivity for rewiring ("
					+ Formatter.format(connectivity, 2) + ")");
		}
	}

	/**
	 * Ensure the geometry uses at least {@code requiredSize} nodes.
	 *
	 * @param requiredSize the minimum allowed size
	 * @return {@code true} if enforcing the size changed the network
	 */
	protected boolean enforceSize(int requiredSize) {
		if (setSize(requiredSize)) {
			if (engine.getModule().cloNPopulation.isSet())
				warn("requires size " + requiredSize + "!");
			return true;
		}
		return false;
	}

	/**
	 * Log a warning message using the geometry-conditioned label.
	 *
	 * @param message the message to log
	 */
	protected void warn(String message) {
		if (logger.isLoggable(Level.WARNING))
			logger.warning(getLabel() + message);
	}

	/**
	 * Compose a human readable prefix for log messages.
	 *
	 * @return the label to prepend to log output
	 */
	protected String getLabel() {
		String label = getName();
		if (label == null)
			return type.name() + ": ";
		return label + " - " + type.name() + ": ";
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
		isValid = false;
		features = null;
	}

	/**
	 * Evaluate geometry. Convenience method to set frequently used quantities such
	 * as {@code minIn, maxOut, avgTot} etc.
	 */
	public GeometryFeatures getFeatures() {
		if (features == null) {
			if (isType(GeometryType.DYNAMIC))
				return new GeometryFeatures(this);
			features = new GeometryFeatures(this);
		}
		return features;
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
	 * Check if current geometry unique. Only unique geomteries need to be encoded.
	 * 
	 * <h3>Requirements/notes:</h3>
	 * <ol>
	 * <li>Lattices etc. are not unique because they can be identically recreated.
	 * <li>Complete graphs, stars, wheels, etc. are not unique.
	 * <li>All geometries involving random elements are unique.
	 * <li>All rewired geometries are unique.
	 * <li>Hierarchical geometries require recursive checks of uniqueness.
	 * </ol>
	 *
	 * @return {@code true} if geometry is unique
	 */
	public boolean isUnique() {
		if (isRewired)
			return true;
		return type.isUnique();
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

	/**
	 * Depth-first traversal helper used to determine connectivity.
	 *
	 * @param node  node to visit
	 * @param check bookkeeping array marking visited nodes
	 * @return {@code true} if all nodes have been visited
	 */
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
	 * @return {@code true} if this geometry links two different populations.
	 */
	public boolean isInterspecies() {
		return isInterspecies;
	}

	/**
	 * Set whether this geometry links different populations.
	 *
	 * @param interspecies {@code true} for inter-species interactions
	 */
	public void setInterspecies(boolean interspecies) {
		this.isInterspecies = interspecies;
	}

	/**
	 * Add/rewire directed and undirected random links.
	 *
	 * <h3>Requirements/notes:</h3>
	 * None.
	 * 
	 * @see #rewireUndirected(double)
	 * @see #rewireDirected()
	 */
	public void rewire() {

		if (isUndirected) {
			if (pRewire > 0.0) {
				rewireUndirected(pRewire);
				isRewired = true;
			}
			if (pAddwire > 0.0) {
				addUndirected();
				isRegular = false;
				isRewired = true;
			}
			return;
		}
		// graph is directed
		if (pRewire > 0.0) {
			rewireDirected();
			isRewired = true;
		}
		if (pAddwire > 0.0) {
			addDirected();
			isRegular = false;
			isRewired = true;
		}
	}

	/**
	 * Rewire undirected links.
	 *
	 * <h3>Requirements/notes:</h3>
	 * <ol>
	 * <li>Requires an undirected graph.
	 * <li>Rewiring preserves connectivity of all nodes.
	 * <li>Resulting graph obviously remains undirected.
	 * <li>The number of rewired links is \(N_\text{rewired}=\min {N_\text{links},
	 * N_\text{links} \log(1-p_\text{undir})}\), i.e. at most the number undirected
	 * links in the graph. Thus, the expected fraction of original links rewired at
	 * most an \(1-1/e\) (or \(~63%\)).
	 * <li>Any rewiring attempts that disconnect the graph are reverted (but still
	 * count towards the number of rewired links to prevent deadlocks in graphs with
	 * low connectivity).
	 * </ol>
	 * 
	 * @param prob the probability of rewiring an undirected link
	 * @return {@code true} if geometry rewired
	 */
	protected void rewireUndirected(double prob) {
		RNGDistribution rng = engine.getRNG();
		int nLinks = (int) Math
				.floor(ArrayMath.norm(kout) * 0.5 * Math.min(1.0, -Math.log(1.0 - prob)) + 0.5);
		long done = 0;
		while (done < nLinks) {
			int first;
			int len;
			do {
				first = rng.random0n(size);
				len = kin[first];
			} while (len <= 1 || len == size - 1);
			int firstneigh = in[first][rng.random0n(len)];
			int second;
			do {
				second = rng.random0n(size - 1);
				if (second >= first)
					second++;
				len = kin[second];
			} while (len <= 1 || len == size - 1);
			int secondneigh = in[second][rng.random0n(len)];

			if (!swapEdges(first, firstneigh, second, secondneigh))
				continue;
			if (!isGraphConnected()) {
				// revert swap if graph got disconnected
				swapEdges(first, firstneigh, second, secondneigh);
				swapEdges(first, secondneigh, second, firstneigh);
			}
			done += 2;
		}
	}

	/**
	 * Utility method to swap edges (undirected links) between nodes: change link
	 * {@code a-an} to {@code a-bn} and {@code b-bn} to {@code b-an}.
	 *
	 * <h3>Requirements/notes:</h3>
	 * Equivalent to invoking {@code rewireEdgeAt(a, bn, an);} followed by
	 * {@code rewireEdgeAt(b, an, bn);} but avoids the additional allocations of
	 * those helper methods.
	 *
	 * @param a  the first node
	 * @param an the neighbour of {@code a} to replace
	 * @param b  the second node
	 * @param bn the neighbour of {@code b} to replace
	 * @return {@code true} if the swap succeeded
	 */
	boolean swapEdges(int a, int an, int b, int bn) {
		if (a == bn || b == an || an == bn)
			return false;
		if (isNeighborOf(a, bn) || isNeighborOf(b, an))
			return false;

		int[] aout = out[a];
		int ai = -1;
		while (aout[++ai] != an) {
			// advance index until we locate 'an' in a's adjacency list
		}
		aout[ai] = bn;
		int[] bout = out[b];
		int bi = -1;
		while (bout[++bi] != bn) {
			// advance index until we locate 'bn' in b's adjacency list
		}
		bout[bi] = an;

		int[] ain = in[a];
		ai = -1;
		while (ain[++ai] != an) {
			// advance index until we locate 'an' in a's incoming list
		}
		ain[ai] = bn;
		int[] bin = in[b];
		bi = -1;
		while (bin[++bi] != bn) {
			// advance index until we locate 'bn' in b's incoming list
		}
		bin[bi] = an;

		aout = out[an];
		ai = -1;
		while (aout[++ai] != a) {
			// advance index until we locate 'a' in an's adjacency list
		}
		aout[ai] = b;
		bout = out[bn];
		bi = -1;
		while (bout[++bi] != b) {
			// advance index until we locate 'b' in bn's adjacency list
		}
		bout[bi] = a;

		ain = in[an];
		ai = -1;
		while (ain[++ai] != a) {
			// advance index until we locate 'a' in an's incoming list
		}
		ain[ai] = b;
		bin = in[bn];
		bi = -1;
		while (bin[++bi] != b) {
			// advance index until we locate 'b' in bn's incoming list
		}
		bin[bi] = a;
		return true;
	}

	/**
	 * Add undirected links.
	 *
	 * <h3>Requirements/notes:</h3>
	 * The number of links added is \(N_\text{add}=N_\text{links}
	 * p_\text{undir}\).
	 * 
	 * @return {@code true} if adding of undirected links successfult
	 */
	public boolean addUndirected() {
		// retrieve the shared RNG to ensure reproducibility of results
		RNGDistribution rng = engine.getRNG();
		double avgOut = getFeatures().avgOut;

		// add at most the number of links already present in the system
		int nLinks = (int) Math.floor(avgOut * size * pAddwire / 2.0 + 0.5);
		int nodea;
		int nodeb;
		while (nLinks > 0) {
			nodea = rng.random0n(size);
			nodeb = rng.random0n(size - 1);
			if (nodeb >= nodea)
				nodeb++; // avoid self-connections
			if (isNeighborOf(nodea, nodeb))
				continue; // avoid double connections
			addEdgeAt(nodea, nodeb);
			nLinks--;
		}
		return true;
	}

	/**
	 * Rewire directed links.
	 *
	 * <h3>Requirements/notes:</h3>
	 * <ol>
	 * <li>Only undirected graphs are guaranteed to remain connected.
	 * <li>Resulting graph is obviously directed (even if original was undirected).
	 * <li>Rewiring preserves connectivity of all nodes (both inlinks
	 * and outlinks).
	 * <li>The number of rewired links is \(N_\text{rewired}=\min {N_\text{links},
	 * N_\text{links} \log(1-p_\text{dir})}\), i.e. at most the number directed
	 * links in the graph. Thus, at most an expected fraction of \(1-1/e\) (or
	 * \(~63%\)) of original links get rewired.
	 * <li>ToDo: Rewrite similar to rewireUndirected().
	 * </ol>
	 * 
	 * @return {@code true} if rewiring succeeded
	 */
	public boolean rewireDirected() {
		// retrieve the shared RNG to ensure reproducibility of structures
		RNGDistribution rng = engine.getRNG();
		double avgOut = getFeatures().avgOut;

		// make sure the right fraction of original links is replaced!
		// rewire at most the number of directed links present in the system
		// (corresponds to a fraction of 1-1/e (~63%) of links rewired)
		int nLinks = (int) Math.floor((int) (avgOut * size + 0.5) * Math.min(1.0, -Math.log(1.0 - pRewire)) + 0.5);
		int done = 0;
		int last = -1;
		int prev;
		int from;
		int to = -1;
		int len;
		int neigh;
		isUndirected = false;
		do {
			// draw first node - avoid sources (nodes without inlinks) and fully connected
			// nodes
			do {
				last = rng.random0n(size);
				len = kin[last];
			} while (len == 0 || len == size - 1);
			neigh = len == 1 ? 0 : rng.random0n(len);
			from = in[last][neigh]; // link used to come from here
			// note that 'from' must have at least one outlink to 'last'.
			if (kout[from] == size - 1)
				continue; // already linked to everybody else
			// draw random node 'to' that is not a neighbor of 'from' (avoid double
			// connections)
			// in addition, 'to' must not be a source
			do {
				to = rng.random0n(size - 1);
				if (to >= from)
					to++;
			} while (isNeighborOf(from, to) || kin[to] == 0);
			// 'from' -> 'last' rewired to 'from' -> 'to'
			rewireLinkAt(from, to, last);
			done++;

			// rewiring is tricky if there are few highly connected hubs and many nodes with
			// few (single) connections
			// the following may still get stuck...
			while (done < nLinks) {
				// 'to' just got a new inlink -> len>1
				len = kin[to];
				// draw random neighbor of 'to' but exclude newly drawn link
				neigh = (len - 1) == 1 ? 0 : rng.random0n(len - 1);
				from = in[to][neigh]; // link used to come from here
				// is 'from' already linked to everyone else?
				if (kout[from] == size - 1) {
					// are there other feasible neighbors?
					for (int n = 0; n < len - 1; n++)
						if (kout[in[to][n]] < size - 1)
							continue; // there is hope...
					// this looks bad - try node we just came from
					if (kout[in[to][len - 1]] == size - 1) {
						throw new IllegalStateException("Rewiring troubles - giving up...");
					}
					// let's go back - can this fail?
					from = in[to][len - 1];
				}
				prev = to;
				// draw random node 'to' that is not a neighbor of 'from' (avoid double
				// connections)
				// in addition, 'to' must not be a source
				do {
					to = rng.random0n(size - 1);
					if (to >= from)
						to++;
				} while (isNeighborOf(from, to) || kin[to] == 0);
				// 'from' -> 'prev' rewired to 'from' -> 'to'
				rewireLinkAt(from, to, prev);
				done++;
				if (to == last)
					break;
			}
		} while (nLinks - done > 1); // this accounts for the last link(s)

		// if 'from' happens to be the origin we are done
		if (to == last)
			return true;

		// draw last link from origin to next, if they are already neighbors,
		// then rewire additional links to preserve connectivity
		while (isNeighborOf(last, to)) {
			// 'to' just got a new inlink -> len>1
			len = kin[to];
			// draw random neighbor of 'to' but exclude newly drawn link
			neigh = (len - 1) == 1 ? 0 : rng.random0n(len - 1);
			from = in[to][neigh]; // link used to come from here
			if (kout[from] == size - 1)
				continue; // already linked to everybody else
			prev = to;
			// draw random node 'to' that is not a neighbor of 'from' (avoid double
			// connections)
			// in addition, 'to' must not be a source
			do {
				to = rng.random0n(size - 1);
				if (to >= from)
					to++;
			} while (isNeighborOf(from, to) || kin[to] == 0);
			rewireLinkAt(from, to, prev);
		}
		// 'to' just got a new inlink -> len>1
		len = kin[to];
		// draw random neighbor of 'to' but exclude last drawn link
		neigh = (len - 1) == 1 ? 0 : rng.random0n(len - 1);
		rewireLinkAt(to, last, in[to][neigh]);
		return true;
	}

	/**
	 * Add directed links to network.
	 *
	 * <h3>Requirements/notes:</h3>
	 * The number of links added is \(N_\text{add}=N_\text{links}
	 * p_\text{dir}\).
	 * 
	 * @return {@code true} if adding links succeeded
	 */
	public boolean addDirected() {
		// retrieve the shared RNG to ensure reproducibility of results
		RNGDistribution rng = engine.getRNG();
		double avgOut = getFeatures().avgOut;
		// add at most the number of directed links already present in the system
		int nLinks = (int) Math.floor(avgOut * size * pAddwire + 0.5);
		int from;
		int to;
		while (nLinks > 0) {
			from = rng.random0n(size);
			to = rng.random0n(size - 1);
			if (to >= from)
				to++; // avoid self-connections
			if (isNeighborOf(from, to))
				continue; // avoid double connections
			addLinkAt(from, to);
			nLinks--;
		}
		return true;
	}

	/**
	 * Add edge (undirected link) by inserting a pair of directed links.
	 *
	 * @param nodea the index of the first node
	 * @param nodeb the index of the second node
	 */
	public void addEdgeAt(int nodea, int nodeb) {
		addLinkAt(nodea, nodeb);
		addLinkAt(nodeb, nodea);
	}

	/**
	 * Add a directed link from {@code from} to {@code to}, allocating storage as
	 * needed.
	 *
	 * <h3>Requirements/notes:</h3>
	 * <ol>
	 * <li>Allocates additional memory for adjacency lists as needed.</li>
	 * <li>Marks the geometry as requiring {@link #evaluate()} before statistics
	 * such as {@code minIn} or {@code avgOut} are used again.</li>
	 * </ol>
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
		features = null;
	}

	/**
	 * Remove edge (undirected link) from {@code nodea} to {@code nodeb}. The
	 * convenience method simply removes the directed link in both directions.
	 *
	 * @param nodea the index of the first node
	 * @param nodeb the index of the second node
	 */
	public void removeEdgeAt(int nodea, int nodeb) {
		removeLinkAt(nodea, nodeb);
		removeLinkAt(nodeb, nodea);
	}

	/**
	 * Remove a directed link from node {@code from} to node {@code to}. Statistics
	 * are not updated immediately; call {@link #evaluate()} when finished with a
	 * batch of edits.
	 *
	 * @param from the index of the first node
	 * @param to   the index of the second node
	 */
	public void removeLinkAt(int from, int to) {
		removeInLink(from, to);
		removeOutLink(from, to);
	}

	/**
	 * Remove all outgoing links from node {@code idx}. Memory is retained for
	 * potential reuse.
	 */
	public void clearLinksFrom(int idx) {
		int len = kout[idx];
		int[] neigh = out[idx];
		for (int i = 0; i < len; i++)
			removeInLink(idx, neigh[i]);
		kout[idx] = 0;
		features = null;
	}

	/**
	 * Remove all incoming links to node {@code idx}. Memory is retained for
	 * potential reuse.
	 */
	public void clearLinksTo(int idx) {
		int len = kin[idx];
		int[] neigh = in[idx];
		for (int i = 0; i < len; i++)
			removeOutLink(neigh[i], idx);
		kin[idx] = 0;
		features = null;
	}

	/**
	 * Remove an incoming link (directed) to node {@code to} from node
	 * {@code from}. Does not shrink the backing arrays.
	 */
	private void removeInLink(int from, int to) {
		int[] mem = in[to];
		int k = kin[to];
		for (int i = 0; i < k; i++) {
			if (mem[i] == from) {
				if (i < k - 1)
					System.arraycopy(mem, i + 1, mem, i, k - 1 - i);
				kin[to]--;
				features = null;
				return;
			}
		}
	}

	/**
	 * Remove an outgoing link (directed) from node {@code from} to node
	 * {@code to}. Does not shrink the backing arrays.
	 */
	private void removeOutLink(int from, int to) {
		int[] mem = out[from];
		int k = kout[from];
		for (int i = 0; i < k; i++) {
			if (mem[i] == to) {
				if (i < k - 1)
					System.arraycopy(mem, i + 1, mem, i, k - 1 - i);
				kout[from]--;
				features = null;
				return;
			}
		}
	}

	/**
	 * Rewire a directed link so that an edge formerly connecting {@code from} to
	 * {@code prev} now connects to {@code to}. Statistics are not updated until
	 * {@link #evaluate()} is called.
	 *
	 * @param from the node whose outgoing link should change
	 * @param to   the new neighbour
	 * @param prev the previous neighbour to disconnect
	 */
	public void rewireLinkAt(int from, int to, int prev) {
		removeLinkAt(from, prev);
		addLinkAt(from, to);
	}

	/**
	 * Rewire an undirected edge so that an edge formerly connecting {@code focal}
	 * and {@code oldneigh} now connects {@code focal} and {@code newneigh}.
	 *
	 * @param focal    the node whose neighbour should change
	 * @param newneigh the new neighbour
	 * @param oldneigh the previous neighbour to disconnect
	 */
	public void rewireEdgeAt(int focal, int newneigh, int oldneigh) {
		rewireLinkAt(focal, newneigh, oldneigh);
		rewireLinkAt(newneigh, focal, oldneigh);
	}

	/**
	 * Check whether {@code check} is currently a neighbour of {@code focal}.
	 *
	 * @param focal the node whose adjacency list to scan
	 * @param check the node to test for adjacency
	 * @return {@code true} if {@code check} occurs among {@code focal}'s outgoing
	 *         links
	 */
	public boolean isNeighborOf(int focal, int check) {
		if (out == null)
			return false;
		int[] neighs = out[focal];
		int len = kout[focal];
		for (int i = 0; i < len; i++)
			if (neighs[i] == check)
				return true;
		return false;
	}

	/**
	 * Initialise the geometry.
	 */
	public abstract void init();

	/**
	 * Get the usage description for the command line option
	 * <code>--geometry</code>.
	 * 
	 * @return the usage description
	 */
	public String usage() {
		CLOption clo = engine.getModule().cloGeometry;
		boolean fixedBoundariesAvailable = (clo.isValidKey(GeometryType.LINEAR) || clo.isValidKey(GeometryType.SQUARE)
				|| clo.isValidKey(GeometryType.CUBE)
				|| clo.isValidKey(GeometryType.HEXAGONAL) || clo.isValidKey(GeometryType.TRIANGULAR));
		return "--geometry <>   geometry " //
				+ (engine.getModel().getType().isIBS() ? "- interaction==competition\n" : "\n") //
				+ "      argument: <g><k>" //
				+ (fixedBoundariesAvailable ? "[f|F]" : "") + " (g type, k neighbours)\n" //
				+ clo.getDescriptionKey() + "\n      further specifications:" //
				+ (fixedBoundariesAvailable ? "\n           f|F: fixed lattice boundaries (default periodic)" : "");
	}

	/**
	 * Check consistency of links.
	 *
	 * <h3>Requirements/notes:</h3>
	 * <ol>
	 * <li>Self connections are unacceptable.
	 * <li>Double links between nodes are unacceptable.
	 * <li>For undirected networks every outgoing link must correspond to an
	 * incoming link.
	 * </ol>
	 * 
	 * @return {@code true} if check succeeded
	 */
	public boolean isConsistent() {
		boolean ok = true;
		ok &= checkMultiple(kin, in, "in");
		ok &= checkMultiple(kout, out, "out");
		ok &= checkInOut();
		if (!isInterspecies)
			ok &= checkSelfing();

		if (isRegular)
			ok &= checkRegular();

		if (isUndirected)
			ok &= checkUndirected();

		return ok;
	}

	/**
	 * Check for multiple connections in the given links.
	 * 
	 * @param degree the degree of each node
	 * @param links  the adjacency list
	 * @param type   the type of connection ("in" or "out")
	 * @return {@code true} if no multiple connections found
	 */
	@SuppressWarnings("java:S2629") // GWT does not support String.format()
	private boolean checkMultiple(int[] degree, int[][] links, String type) {
		logger.fine("Checking multiple " + type + "-connections... ");
		boolean ok = true;
		for (int i = 0; i < size; i++) {
			// double connections 'in'
			int k = degree[i];
			for (int j = 0; j < k; j++) {
				int idx = links[i][j];
				for (int n = j + 1; n < k; n++)
					if (links[i][n] == idx) {
						ok = false;
						logger.fine("Node " + i + " has double " + type + "-connection with node " + idx);
					}
			}
		}
		logger.fine("Multiple " + type + "-connections check: " + (ok ? "success!" : "failed!"));
		return ok;
	}

	/**
	 * Check consistency of in- and out-connections. Every in-connection must be
	 * balanced by a corresponding out-connection and vice versa.
	 * 
	 * @return {@code true} if all connections are consistent
	 */
	@SuppressWarnings("java:S2629") // GWT does not support String.format()
	private boolean checkInOut() {
		logger.fine("Checking consistency of in-, out-connections... ");
		boolean ok = true;
		for (int i = 0; i < size; i++) {
			// each 'out' connection must be balanced by an 'in' connection
			int[] outi = out[i];
			int nout = kout[i];
			nextlink: for (int j = 0; j < nout; j++) {
				int[] ini = in[outi[j]];
				int nin = kin[outi[j]];
				for (int k = 0; k < nin; k++)
					if (ini[k] == i)
						continue nextlink;
				ok = false;
				logger.fine("Node " + i + " has 'out'-link to node " + outi[j]
						+ ", but there is no corresponding 'in'-link");
			}
		}
		logger.fine("Consistency of in-, out-connections check: " + (ok ? "success!" : "failed!"));
		return ok;
	}

	/**
	 * Check for self-connections (loops).
	 * 
	 * @return {@code true} if no self-connections found
	 */
	@SuppressWarnings("java:S2629") // GWT does not support String.format()
	private boolean checkSelfing() {
		// self-connections are only acceptable for inter-species interactions
		logger.fine("Checking self-connections... ");
		boolean ok = true;
		for (int i = 0; i < size; i++) {
			// report loops
			int[] outi = out[i];
			int nout = kout[i];
			for (int j = 0; j < nout; j++) {
				if (outi[j] == i) {
					ok = false;
					logger.fine("Node " + i + " has loop in 'out'-connections");
				}
			}
			int[] ini = in[i];
			int nin = kin[i];
			for (int j = 0; j < nin; j++) {
				if (ini[j] == i) {
					ok = false;
					logger.fine("Node " + i + " has loop in 'in'-connections");
				}
			}
		}
		logger.fine("Self-connections check: " + (ok ? "success!" : "failed!"));
		return ok;
	}

	/**
	 * Check regularity of the graph.
	 * 
	 * @return {@code true} if the graph is regular
	 */
	@SuppressWarnings("java:S2629") // GWT does not support String.format()
	private boolean checkRegular() {
		boolean ok = true;
		logger.fine("Checking regularity... ");
		GeometryFeatures f = getFeatures();
		int nout = f.minOut;
		int nin = f.minIn;
		for (int i = 0; i < size; i++) {
			if (kout[i] != nout) {
				ok = false;
				logger.fine("Node " + i + " has wrong 'out'-link count - " + kout[i] + " instead of " + nout);
			}
			if (kin[i] != nin) {
				ok = false;
				logger.fine("Node " + i + " has wrong 'in'-link count - " + kin[i] + " instead of " + nin);
			}
		}
		logger.fine("Regularity check: " + (ok ? "success!" : "failed!"));
		return ok;
	}

	/**
	 * Check undirected structure of the graph. Every outgoing link must be
	 * balanced by a corresponding incoming link and vice versa.
	 * 
	 * @return {@code true} if the graph is undirected
	 */
	@SuppressWarnings("java:S2629") // GWT does not support String.format()
	private boolean checkUndirected() {
		logger.fine("Checking undirected structure... ");
		boolean ok = true;
		for (int i = 0; i < size; i++) {
			// each connection must go both ways
			int[] outa = out[i];
			int nouta = kout[i];
			nextout: for (int j = 0; j < nouta; j++) {
				int[] outb = out[outa[j]];
				int noutb = kout[outa[j]];
				for (int k = 0; k < noutb; k++)
					if (outb[k] == i)
						continue nextout;
				ok = false;
				logger.fine("Node " + i + " has 'out'-link to node " + outa[j] + ", but not vice versa");
			}
			int[] ina = in[i];
			int nina = kin[i];
			nextin: for (int j = 0; j < nina; j++) {
				int[] inb = in[ina[j]];
				int ninb = kin[ina[j]];
				for (int k = 0; k < ninb; k++)
					if (inb[k] == i)
						continue nextin;
				ok = false;
				logger.fine("Node " + i + " has 'in'-link to node " + ina[j] + ", but not vice versa");
			}
		}
		logger.fine("Undirected structure check: " + (ok ? "success!" : "failed!"));
		return ok;
	}

	/**
	 * Clone geometry.
	 * 
	 * <h3>Requirements/notes:</h3>
	 * <ol>
	 * <li>Overrides {@link java.lang.Object#clone() clone()} in
	 * {@link java.lang.Object} but conflicts with GWT's aversion to
	 * clone()ing...</li>
	 * <li>Remove <code>@SuppressWarnings("all")</code> to ensure that no other
	 * issues crept in when modifying method.</li>
	 * </ol>
	 * 
	 * @return clone of geometry
	 */
	// @Override
	@SuppressWarnings("all")
	public AbstractGeometry clone() {
		AbstractGeometry clone = AbstractGeometry.create(engine, type);
		clone.specs = specs;
		clone.name = name;
		if (kin != null)
			clone.kin = Arrays.copyOf(kin, kin.length);
		if (kout != null)
			clone.kout = Arrays.copyOf(kout, kout.length);
		if (in != null) {
			clone.in = Arrays.copyOf(in, in.length);
			for (int i = 0; i < in.length; i++)
				clone.in[i] = Arrays.copyOf(in[i], in[i].length);
		}
		if (out != null) {
			clone.out = Arrays.copyOf(out, out.length);
			for (int i = 0; i < out.length; i++)
				clone.out[i] = Arrays.copyOf(out[i], out[i].length);
		}
		clone.size = size;
		clone.type = type;
		clone.features = features == null ? null : new GeometryFeatures(features);
		clone.connectivity = connectivity;
		clone.pRewire = pRewire;
		clone.pAddwire = pAddwire;
		clone.isUndirected = isUndirected;
		clone.isRewired = isRewired;
		clone.isInterspecies = isInterspecies;
		clone.isSingle = isSingle;
		clone.isRegular = isRegular;
		clone.isValid = isValid;
		return clone;
	}

	@Override
	public int hashCode() {
		int result = Objects.hash(engine, specs, name, type, size, isUndirected, isRegular, isRewired,
				isInterspecies, isSingle, isValid, features, connectivity, pRewire, pAddwire);
		result = 31 * result + Arrays.hashCode(kin);
		result = 31 * result + Arrays.hashCode(kout);
		result = 31 * result + Arrays.deepHashCode(in);
		result = 31 * result + Arrays.deepHashCode(out);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null || getClass() != obj.getClass())
			return false;
		AbstractGeometry other = (AbstractGeometry) obj;
		return size == other.size && isUndirected == other.isUndirected && isRegular == other.isRegular
				&& isRewired == other.isRewired && isInterspecies == other.isInterspecies && isSingle == other.isSingle
				&& isValid == other.isValid && Objects.equals(features, other.features)
				&& Double.doubleToLongBits(connectivity) == Double.doubleToLongBits(other.connectivity)
				&& Double.doubleToLongBits(pRewire) == Double.doubleToLongBits(other.pRewire)
				&& Double.doubleToLongBits(pAddwire) == Double.doubleToLongBits(other.pAddwire)
				&& Objects.equals(engine, other.engine) && Objects.equals(specs, other.specs)
				&& Objects.equals(name, other.name) && type == other.type && Arrays.equals(kin, other.kin)
				&& Arrays.equals(kout, other.kout) && Arrays.deepEquals(in, other.in)
				&& Arrays.deepEquals(out, other.out);
	}

	/**
	 * Encode geometry as a plist string fragment.
	 *
	 * @return the geometry encoded as a plist
	 * 
	 * @see #decodeGeometry(Plist)
	 * @see Plist
	 */
	public String encodeGeometry() {
		StringBuilder plist = new StringBuilder();
		plist.append(Plist.encodeKey("Name", type.getTitle()));
		plist.append(Plist.encodeKey("Code", type.getKey()));
		if (isUnique()) {
			// encode geometry
			plist.append("<key>Graph</key>\n<dict>\n");
			// note: in[] and kin[] will be reconstructed on restore
			for (int n = 0; n < size; n++)
				plist.append(Plist.encodeKey(Integer.toString(n), out[n], kout[n]));
			plist.append("</dict>\n");
		}
		return plist.toString();
	}

	/**
	 * Decode the geometry from the plist. The structure is encoded in map which
	 * provides array of neighbor indices for each individual index.
	 * 
	 * <h3>Requirements/notes:</h3>
	 * The population (including its geometry/geometries) must already have been
	 * initialized. This only restores a particular (unique) geometry.
	 * 
	 * @param plist the plist encoding the geometry
	 * 
	 * @see #encodeGeometry()
	 */
	public void decodeGeometry(Plist plist) {
		if (!isUnique())
			return;
		// decode geometry
		Plist graph = (Plist) plist.get("Graph");
		// every inlink is an outlink elsewhere
		ArrayList<ArrayList<Integer>> inlinks = new ArrayList<>(size);
		for (int n = 0; n < size; n++)
			inlinks.add(new ArrayList<>());
		for (Iterator<String> i = graph.keySet().iterator(); i.hasNext();) {
			String idxs = i.next();
			int idx = Integer.parseInt(idxs);
			@SuppressWarnings("unchecked")
			List<Integer> neighs = (List<Integer>) graph.get(idxs);
			out[idx] = Plist.list2int(neighs);
			kout[idx] = out[idx].length;
			// each outlink is someone else's inlink; process links from i to j
			for (Iterator<Integer> j = neighs.iterator(); j.hasNext();)
				inlinks.get(j.next()).add(idx);
		}
		// outlinks already in place; finish inlinks
		for (int n = 0; n < size; n++) {
			in[n] = Plist.list2int(inlinks.get(n));
			kin[n] = in[n].length;
		}
	}
}
