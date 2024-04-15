//
// EvoLudo Project
//
// Copyright 2010 Christoph Hauert
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
//	Hauert, Christoph (<year>) EvoLudo Project, http://www.evoludo.org
//			(doi: <doi>[, <version>])
//
//	<doi>:	digital object identifier of the downloaded release (or the
//			most recent release if downloaded from github.com),
//	<year>:	year of release (or download), and
//	[, <version>]: optional version number (as reported in output header
//			or GUI console) to simplify replication of reported results.
//
// The formatting may be adjusted to comply with publisher requirements.
//

package org.evoludo.simulator;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

import org.evoludo.math.ArrayMath;
import org.evoludo.math.Combinatorics;
import org.evoludo.math.RNGDistribution;
import org.evoludo.simulator.models.IBS;
import org.evoludo.simulator.models.IBSPopulation;
import org.evoludo.simulator.models.Model;
import org.evoludo.simulator.modules.Module;
import org.evoludo.util.CLOParser;
import org.evoludo.util.CLOption;
import org.evoludo.util.Formatter;
import org.evoludo.util.Plist;

/**
 * Instances of <code>Geometry</code> represent the interaction and/or
 * reproduction structure of the population. For a list of currently implemented
 * geometries, {@link Type}.
 * 
 * @author Christoph Hauert
 */
public class Geometry {

	/**
	 * The pacemaker of all models. Interface with the outside world.
	 */
	EvoLudo engine;

	/**
	 * The IBS population that has this geometry.
	 */
	IBSPopulation population;

	/**
	 * The IBS population representing the opponent. For intra-species interactions
	 * {@code population==opponent} holds.
	 */
	public IBSPopulation opponent;

	/**
	 * Logger for keeping track of and reporting events and issues.
	 */
	Logger logger;

	/**
	 * Instantiates a new geometry for data visualization with pacemaker {@code engine}.
	 * 
	 * @param engine the pacemeaker for running the model
	 * 
	 * @see org.evoludo.simulator.views.Distribution#createGeometry(int)
	 */
	public Geometry(EvoLudo engine) {
		this.engine = engine;
		logger = engine.getLogger();
	}

	/**
	 * Instantiates a new geometry for intra-species module {@code module} with
	 * pacemaker {@code engine}.
	 * 
	 * @param engine the pacemeaker for running the model
	 * @param module the module with interaction parameters
	 */
	public Geometry(EvoLudo engine, Module module) {
		this(engine, module, module);
	}

	/**
	 * Instantiates a new geometry for inter-species module {@code popModule} and
	 * opponent {@code oppModule} with pacemaker {@code engine}. For intra-species
	 * interactions {@code population==opponent} holds.
	 * 
	 * @param engine    the pacemeaker for running the model
	 * @param popModule the module with interaction parameters
	 * @param oppModule the module of the opponent
	 */
	public Geometry(EvoLudo engine, Module popModule, Module oppModule) {
		this(engine);
		Model model = engine.getModel();
		switch (model.getModelType()) {
			// case ODE:
			// case SDE:
			// should not get here (geometries meaningless)
			// case PDE:
			default:
				return;
			case IBS:
				population = popModule.getIBSPopulation();
				opponent = oppModule.getIBSPopulation();
		}
	}

	/**
	 * Gets the name of this graph. Typically this is "Interaction" or
	 * "Reproduction" for the correpsonding graphs, or "Structure" if the two graphs
	 * are the same. In multi-species models the name of the species may be
	 * prepended.
	 * 
	 * @return the name of the graph
	 * 
	 * @see #setName(String)
	 */
	public String getName() {
		// name == null is possible for pseudo-geometries that are used to
		// display e.g. trait distributions in MVDistribution
		if (name == null || name.equals("Structure"))
			return "";
		if (name.endsWith(": Structure"))
			return name.substring(0, name.length() - ": Structure".length());
		return name;
	}

	/**
	 * Sets the name of this graph.
	 * 
	 * @param name the name of this graph.
	 * 
	 * @see #getName()
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * Checks if the type of this geometry is {@code type}.
	 * 
	 * @param type the type of the geometry to check
	 * @return {@code true} if {@code type} matches this geometry
	 */
	public boolean isType(Type type) {
		if (isRewired)
			return Type.GENERIC.equals(type);
		return geometry.equals(type);
	}

	/**
	 * Get the type of this geometry.
	 * 
	 * @return the type of this geometry
	 */
	public Type getType() {
		if (isRewired)
			return Type.GENERIC;
		return geometry;
	}

	/**
	 * Set the type of this geometry.
	 * 
	 * @param type the type of this geometry
	 */
	public void setType(Type type) {
		geometry = type;
	}

	/**
	 * Local storage for the 2D representation of this graph.
	 */
	private Network2D network2D = null;

	/**
	 * Local storage for the 3D representation of this graph.
	 */
	private Network3D network3D = null;

	/**
	 * Set the 2D network representation of this graph. This is purely for storage.
	 * The network is generated in {@link org.evoludo.simulator.Network2D
	 * Network2D}.
	 * 
	 * @param net the 2D network representation
	 */
	public void setNetwork2D(Network2D net) {
		network2D = net;
	}

	/**
	 * Get the 2D network representation of this graph. This is purely for storage.
	 * The network is generated in {@link org.evoludo.simulator.Network2D
	 * Network2D}.
	 * 
	 * @return the 2D network representation of this graph
	 */
	public Network2D getNetwork2D() {
		if (network2D == null)
			network2D = engine.createNetwork2D(this);
		return network2D;
	}

	/**
	 * Set the 3D network representation of this graph. This is purely for storage.
	 * The network is generated in {@link org.evoludo.simulator.Network3D
	 * Network3D}.
	 * 
	 * @param net the 3D network representation
	 */
	public void setNetwork3D(Network3D net) {
		network3D = net;
	}

	/**
	 * Get the 3D network representation of this graph. This is purely for storage.
	 * The network is generated in {@link org.evoludo.simulator.Network3D
	 * Network3D}.
	 * 
	 * @return the 3D network representation of this graph.
	 */
	public Network3D getNetwork3D() {
		if (network3D == null)
			network3D = engine.createNetwork3D(this);
		return network3D;
	}

	/**
	 * The geometry of the graph.
	 */
	private Type geometry = Type.MEANFIELD;

	/**
	 * Only used for hierarchical geometries to specify the geometry of each level.
	 * 
	 * @see #initGeometryHierarchical()
	 */
	public Type subgeometry = Type.MEANFIELD;

	/**
	 * The available graph geometries:
	 * <dl>
	 * <dt>MEANFIELD</dt>
	 * <dd>mean-field/well-mixed population</dd>
	 * <dt>COMPLETE</dt>
	 * <dd>complete graph (connectivity \(k=N-1\))</dd>
	 * <dt>HIERARCHY</dt>
	 * <dd>hierarchical (meta-)populations</dd>
	 * <dt>STAR</dt>
	 * <dd>star graph (single hub)</dd>
	 * <dt>WHEEL</dt>
	 * <dd>wheel (cycle with single hub)</dd>
	 * <dt>SUPER_STAR</dt>
	 * <dd>super-star (single hub, petals</dd>
	 * <dt>STRONG_AMPLIFIER</dt>
	 * <dd>strong (undirected) amplifier</dd>
	 * <dt>STRONG_SUPPRESSOR</dt>
	 * <dd>strong (undirected) suppressor</dd>
	 * <dt>LINEAR</dt>
	 * <dd>linear lattice, 1D</dd>
	 * <dt>SQUARE_NEUMANN</dt>
	 * <dd>square lattice, 2D (von neumann neighbourhood, nearest neighbours,
	 * connectivity \(k=4\))</dd>
	 * <dt>SQUARE_NEUMANN_2ND</dt>
	 * <dd>square lattice, 2D (second nearest neighbours, connectivity \(k=4\))</dd>
	 * <dt>SQUARE_MOORE</dt>
	 * <dd>square lattice, 2D (moore neighbourhood, first and second nearest
	 * neighbours, connectivity \(k=8\))</dd>
	 * <dt>SQUARE</dt>
	 * <dd>square lattice, 2D</dd>
	 * <dt>CUBE</dt>
	 * <dd>cubic lattice, 3D</dd>
	 * <dt>HONEYCOMB</dt>
	 * <dd>hexagonal/honeycomb lattice (connectivity \(k=6\))</dd>
	 * <dt>TRIANGULAR</dt>
	 * <dd>triangular lattice (connectivity \(k=3\))</dd>
	 * <dt>FRUCHT</dt>
	 * <dd>Frucht graph, \(N=12, k=3\)</dd>
	 * <dt>TIETZE</dt>
	 * <dd>Tietze graph \(N=12, k=3\)</dd>
	 * <dt>FRANKLIN</dt>
	 * <dd>Franklin graph \(N=12, k=3\)</dd>
	 * <dt>HEAWOOD</dt>
	 * <dd>Heawood graph \(N=14, k=3\)</dd>
	 * <dt>ICOSAHEDRON</dt>
	 * <dd>Icosahedron graph \(N=12, k=5\)</dd>
	 * <dt>DODEKAHEDRON</dt>
	 * <dd>Dodekahedron graph \(N=20, k=3\)</dd>
	 * <dt>DESARGUES</dt>
	 * <dd>Desargues graph \(N=20, k=3\)</dd>
	 * <dt>RANDOM_GRAPH</dt>
	 * <dd>random graph</dd>
	 * <dt>RANDOM_GRAPH_DIRECTED</dt>
	 * <dd>directed random graph</dd>
	 * <dt>RANDOM_REGULAR_GRAPH</dt>
	 * <dd>random regular graph</dd>
	 * <dt>SCALEFREE</dt>
	 * <dd>scale-free graph</dd>
	 * <dt>SCALEFREE_BA</dt>
	 * <dd>scale-free graph, Barabasi &amp; Albert</dd>
	 * <dt>SCALEFREE_KLEMM</dt>
	 * <dd>scale-free graph, Klemm &amp; Eguiluz</dd>
	 * </dl>
	 * 
	 * @author Christoph Hauert
	 */
	public enum Type implements CLOption.Key {
		/**
		 * Mean-field/well-mixed population.
		 * 
		 * @see Geometry#initGeometryMeanField()
		 */
		MEANFIELD("M", "mean-field/well-mixed population"),

		/**
		 * Complete graph, connectivity \(k=N-1\).
		 * 
		 * @see Geometry#initGeometryComplete()
		 */
		COMPLETE("c", "complete graph (k=N-1)"),

		/**
		 * Hierarchical (meta-)populations. Supported sub-population types are:
		 * <ul>
		 * <li>{@code M}: well-mixed (default)
		 * <li>{@code n}: square lattice (von neumann)
		 * <li>{@code m}: square lattice (moore)
		 * </ul>
		 * Append {@code f} for fixed lattice boundaries. The hierarchical populations
		 * are specified as a vector with the format {@code <n1>[,<n2>[...,<nm>]]w}
		 * where {@code ni} refers to the number of units in level {@code i}. There is a
		 * total of {@code m+1} levels with {@code nPopulation/(n1*...*nm)} individuals
		 * in the last level (smallest unit). The strength of interaction between levels
		 * is {@code w}.
		 * 
		 * @see Geometry#initGeometryHierarchical()
		 */
		HIERARCHY("H", "hierarchical (meta-)populations",
				"H[<g>[f]]<n1>[,<n2>[...,<nm>]]w<w> hierarchical\n" //
						+ "                structure for population geometries g:\n" //
						+ "                M: well-mixed (default)\n" //
						+ "                n: square lattice (von neumann)\n" //
						+ "                m: square lattice (moore)\n" //
						+ "                append f for fixed boundaries\n" //
						+ "                n1,...,nm number of units on each hierarchical level\n" //
						+ "                total of m+1 levels with nPopulation/(n1*...*nm)\n" //
						+ "                individuals in last level\n" //
						+ "                w: strength of ties between levels"),

		/**
		 * Linear lattice, 1D. {@code l<l>[,<r>]} specifies a linear lattice with
		 * {@code l} neighbours to the left and {@code r} to the right. If {@code r} is
		 * missing or {@code r==l} the neighbourhood is symmetric.
		 * 
		 * @see Geometry#initGeometryLinear()
		 */
		LINEAR("l", "linear lattice, 1D", "l<l>[,<r>] linear lattice (l neighbourhood,\n" //
				+ "                if r!=l asymmetric neighbourhood)"),

		/**
		 * Square lattice (von Neumann neighbourhood). Four nearest neighbours (north,
		 * east, south, west).
		 * 
		 * @see Geometry#initGeometrySquare()
		 * @see Geometry#initGeometrySquareVonNeumann(int, int, int)
		 */
		SQUARE_NEUMANN("n", "square lattice (von Neumann)"),

		/**
		 * Square lattice. Four second-nearest neighbours (north-east, north-west, south-west, south-east).
		 * 
		 * @see Geometry#initGeometrySquare()
		 * @see Geometry#initGeometrySquareVonNeumann2nd(int, int, int)
		 */
		SQUARE_NEUMANN_2ND("n2", "square lattice (von Neumann)"),

		/**
		 * Square lattice (Moore neighbourhood). Eight nearest neighbours (chess kings
		 * moves).
		 * 
		 * @see Geometry#initGeometrySquare()
		 * @see Geometry#initGeometrySquareMoore(int, int, int)
		 */
		SQUARE_MOORE("m", "square lattice (Moore)"),

		/**
		 * Square lattice, 2D. {@code N<n>} specifies a square lattice with {@code n}
		 * neighbours, where {@code n} is {@code 3x3, 5x5...}.
		 * 
		 * @see Geometry#initGeometrySquare()
		 * @see Geometry#initGeometrySquare(int, int, int)
		 */
		SQUARE("N", "square lattice, 2D", "N<k> square lattice (k=3x3, 5x5...)"),

		/**
		 * Cubic lattice, 3D. {@code C<n>} cubic lattice with {@code n} neighbours,
		 * where {@code n} is {@code 2+2+2, 3x3x3, 5x5x5...}.
		 * 
		 * @see Geometry#initGeometryCube()
		 */
		CUBE("C", "cubic lattice, 3D", "C<k> cubic lattice (k=2+2+2, 3x3x3, 5x5x5...)"),

		/**
		 * Hexagonal (honeycomb) lattice, connectivity \(k=6\).
		 * 
		 * @see Geometry#initGeometryHoneycomb()
		 */
		HONEYCOMB("h", "honeycomb lattice (k=6)"),

		/**
		 * Triangular lattice, connectivity \(k=3\).
		 * 
		 * @see Geometry#initGeometryTriangular()
		 */
		TRIANGULAR("t", "triangular lattice (k=3)"),

		/**
		 * Frucht graph, size \(N=12\), connectivity \(k=3\).
		 * 
		 * @see Geometry#initGeometryFruchtGraph()
		 */
		FRUCHT("0", "Frucht graph (N=12, k=3)"),

		/**
		 * Tietze's graph, size \(N=12\), connectivity \(k=3\).
		 * 
		 * @see Geometry#initGeometryTietzeGraph()
		 */
		TIETZE("1", "Tietze graph (N=12, k=3)"),

		/**
		 * Franklin graph, size \(N=12\), connectivity \(k=3\).
		 * 
		 * @see Geometry#initGeometryFranklinGraph()
		 */
		FRANKLIN("2", "Franklin graph (N=12, k=3)"),

		/**
		 * Heawood graph, size \(N=14\), connectivity \(k=3\).
		 * 
		 * @see Geometry#initGeometryHeawoodGraph()
		 */
		HEAWOOD("3", "Heawood graph (N=14, k=3)"),

		/**
		 * Icosahedron graph, size \(N=12\), connectivity \(k=5\).
		 * 
		 * @see Geometry#initGeometryIcosahedronGraph()
		 */
		ICOSAHEDRON("4", "Icosahedron graph (N=12, k=5)"),

		/**
		 * Dodekahedron graph, size \(N=20\), connectivity \(k=3\).
		 * 
		 * @see Geometry#initGeometryDodekahedronGraph()
		 */
		DODEKAHEDRON("5", "Dodekahedron graph (N=20, k=3)"),

		/**
		 * Desargues graph, size \(N=20\), connectivity \(k=3\).
		 * 
		 * @see Geometry#initGeometryDesarguesGraph()
		 */
		DESARGUES("6", "Desargues graph (N=20, k=3)"),

		/**
		 * Star graph, connectivity \(k=2(N-1)/N\).
		 * 
		 * @see Geometry#initGeometryStar()
		 */
		STAR("s", "star (single hub)"),

		/**
		 * Superstar graph (single hub, petals). {@code S<p[,k]>} superstar graph with
		 * {@code p} petals and amplification {@code k}.
		 * 
		 * @see Geometry#initGeometrySuperstar()
		 */
		SUPER_STAR("S", "super-star (single hub, petals)",
				"S<p[,a]> super-star (p petals [1], a amplify [3])"),

		/**
		 * Wheel graph, connectivity \(k=4(N-1)/N\).
		 * 
		 * @see Geometry#initGeometryWheel()
		 */
		WHEEL("w", "wheel (cycle, single hub)"),

		/**
		 * Strong (undirected) amplifier.
		 * 
		 * @see Geometry#initGeometryAmplifier()
		 */
		STRONG_AMPLIFIER("+", "strong (undirected) amplifier"),

		/**
		 * Strong (undirected) suppressor.
		 * 
		 * @see Geometry#initGeometrySuppressor()
		 */
		STRONG_SUPPRESSOR("-", "strong (undirected) suppressor"),

		/**
		 * Random regular graph.
		 * 
		 * @see Geometry#initGeometryRandomRegularGraph()
		 */
		RANDOM_REGULAR_GRAPH("r", "random regular graph",
				"r<d> random regular graph (d degree [2])"),

		/**
		 * Directed random regular graph. (not yet implemented)
		 * 
		 * @see Geometry#initGeometryRandomGraphDirected()
		 */
		// RANDOM_REGULAR_GRAPH_DIRECTED("d", "directed random regular graph"),

		/**
		 * Random graph. {@code R<d>} random graph with degree {@code d}.
		 * 
		 * @see Geometry#initGeometryRandomGraph()
		 */
		RANDOM_GRAPH("R", "random graph", "R<d> random graph (d degree [2])"),

		/**
		 * Directed random graph. {@code D<d>} random graph with degree {@code d}.
		 * 
		 * @see Geometry#initGeometryRandomGraphDirected()
		 */
		RANDOM_GRAPH_DIRECTED("D", "random graph (directed)",
				"D<d> directed random graph (d degree [2])"),

		/**
		 * Scale-free network. {@code p<e>} scale-free network with exponent {@code e}.
		 * 
		 * @see Geometry#initGeometryScaleFree()
		 */
		SCALEFREE("p", "scale-free graph", "p<e> scale-free graph following\n" +
				"                degree distribution with exponent e [-2]"),

		/**
		 * Scale-free network. {@code F<n[,p]>} scale-free network with degree {@code n}
		 * and a fraction of {@code p} random links.
		 * 
		 * @see Geometry#initGeometryScaleFreeKlemm()
		 */
		SCALEFREE_KLEMM("F", "scale-free, small world graph (Klemm & Eguiluz)",
				"F<n[,p]> scale-free, small world graph\n" + //
						"                (Klemm & Eguiluz) with avg. degree n, p random links"),

		/**
		 * Scale-free network. {@code f<d>} scale-free network with degree {@code d}.
		 * 
		 * @see Geometry#initGeometryScaleFreeBA()
		 */
		SCALEFREE_BA("f", "scale-free graph (Barabasi & Albert)",
				"f<n> scale-free graph with avg. degree n\n" + //
						"                (Barabasi & Albert)"),

		/**
		 * Dynamically changeing network structure.
		 * 
		 * @see Geometry#initGeometryScaleFreeBA()
		 */
		DYNAMIC("*", "dynamic geometry"),

		/**
		 * Placeholder for empty geometry. Not user selectable.
		 */
		VOID("-1", "void geometry"),

		/**
		 * Placeholder for generic geometry. Not user selectable.
		 */
		GENERIC("-2", "generic geometry"),

		/**
		 * Placeholder for invalid geometry. Not user selectable.
		 */
		INVALID("-3", "invalid geometry");

		/**
		 * Key of geometry. Used for parsing command line options.
		 * 
		 * @see Module#cloGeometry
		 * @see IBS#cloGeometryInteraction
		 * @see IBS#cloGeometryReproduction
		 */
		String key;

		/**
		 * Brief description of geometry type for GUI and help display.
		 * 
		 * @see EvoLudo#helpCLO()
		 */
		String title;

		/**
		 * Optional long description of geometry type for help display.
		 * 
		 * @see EvoLudo#helpCLO()
		 */
		String description;

		/**
		 * Instantiate new type of geometry.
		 * 
		 * @param key   identifier for parsing of command line option
		 * @param title the summary of geometry for GUI and help display
		 */
		Type(String key, String title) {
			this(key, title, null);
		}

		/**
		 * Instantiate new geometry type.
		 * 
		 * @param key         the identifier for parsing of command line options
		 * @param title       the summary of the geometry for GUI and help display
		 * @param description the optional description of geometry
		 */
		Type(String key, String title, String description) {
			this.key = key;
			this.title = title;
			this.description = description;
		}

		@Override
		public String getKey() {
			return key;
		}

		@Override
		public String getTitle() {
			return title;
		}

		@Override
		public String getDescription() {
			return (description == null ? title : description);
		}

		/**
		 * Check if the geometry is a square lattice.
		 * 
		 * @return {@code true} if the geometry is a square lattice
		 */
		public boolean isSquareLattice() {
			switch (this) {
				case SQUARE:
				case SQUARE_MOORE:
				case SQUARE_NEUMANN:
				case SQUARE_NEUMANN_2ND:
					return true;
				default:
					return false;
			}
		}

		/**
		 * Check if the geometry is a lattice.
		 * 
		 * @return {@code true} if the geometry is a lattice
		 */
		public boolean isLattice() {
			switch (this) {
				case LINEAR:
				case SQUARE:
				case SQUARE_MOORE:
				case SQUARE_NEUMANN:
				case SQUARE_NEUMANN_2ND:
				case HONEYCOMB:
				case TRIANGULAR:
				case CUBE:
					return true;
				default:
					return false;
			}
		}

		@Override
		public String toString() {
			return key + ": " + title;
		}
	}

	/**
	 * The name of this graph. Typically this is "interaction" or "reproduction" for
	 * the correpsonding graphs, or "structure" if the two graphs are the same. In
	 * multi-species models the name of the species may be prepended.
	 */
	public String name = null;

	/**
	 * This is the neighbourhood network structure of incoming links. More
	 * specifically, for each node a list
	 * of indices of neighbouring nodes with links to this one.
	 * 
	 * <h3>Requirements/notes:</h3>
	 * {@code in[i].length} does not necessarily reflect the number of
	 * neighbours! Use {@code kin[i]} instead. To minimize memory allocation
	 * requests {@code in[i].length>kin[i]} may hold. This is primarily important
	 * for dynamical networks.
	 */
	public int[][] in = null;

	/**
	 * This is the neighbourhood network structure of outgoing links. More
	 * specifically, for each node a list
	 * of indices of neighbouring nodes that links point to.
	 * 
	 * <h3>Requirements/notes:</h3>
	 * {@code out[i].length} does not necessarily reflect the number of
	 * neighbours! Use {@code kout[i]} instead. To minimize memory allocation
	 * requests {@code out[i].length>kout[i]} may hold. This is primarily important
	 * for dynamical networks.
	 */
	public int[][] out = null;

	/**
	 * The array storing the number of incoming neighbours for each node, i.e. the
	 * number of neighbours that have a link pointing to this one.
	 */
	public int[] kin = null;

	/**
	 * The array storing the number of outgoing neighbours for each node, i.e. the
	 * number of links pointing from this node to another.
	 */
	public int[] kout = null;

	/**
	 * The number of nodes in the graph.
	 */
	public int size = -1;

	/**
	 * Sets the number of nodes in the graph to {@code size}. This affects memory
	 * allocations in the model and hence a reset is required if the size changes.
	 * 
	 * @param size the new size of the graph
	 * @return {@code true} if the graph size changed
	 */
	public boolean setSize(int size) {
		if (size <= 0 || this.size == size)
			return false; // no change
		this.size = size;
		return true;
	}

	/**
	 * Flag indicating whether boundaries are fixed or periodic (default).
	 */
	public boolean fixedBoundary = false;

	/*
	 * public Boundary boundary = Boundary.PERIODIC;
	 * 
	 * public enum Boundary { PERIODIC, FIXED, ABSORBING }
	 */

	/**
	 * The number of units in each hierarchical level requested. Processed for
	 * feasibility and then stored in {@code hierarchy}.
	 * 
	 * @see #check()
	 * @see #initGeometryHierarchical()
	 */
	protected int[] rawhierarchy;

	/**
	 * The number of units in each hierarchical level. The lowest level refers to
	 * the number of nodes.
	 * 
	 * @see #initGeometryHierarchical()
	 */
	public int[] hierarchy;

	/**
	 * The strength of interactions between hierarchical levels. For example, with a
	 * strength \(p\) between the first and second level, the strength is \(p^2\)
	 * between the first and third. More generally, the interaction strength is
	 * \(p^n\), where \(n\) denotes the number of levels between the two interacting
	 * individuals.
	 * 
	 * @see #initGeometryHierarchical()
	 */
	public double hierarchyweight;

	/**
	 * The minimum number of incoming links.
	 * 
	 * @see #evaluate()
	 */
	public int minIn = -1;

	/**
	 * The maximum number of incoming links.
	 * 
	 * @see #evaluate()
	 */
	public int maxIn = -1;

	/**
	 * The average number of incoming links.
	 * 
	 * @see #evaluate()
	 */
	public double avgIn = -1.0;

	/**
	 * The minimum number of outgoing links.
	 * 
	 * @see #evaluate()
	 */
	public int minOut = -1;

	/**
	 * The maximum number of outgoing links.
	 * 
	 * @see #evaluate()
	 */
	public int maxOut = -1;

	/**
	 * The average number of outgoing links.
	 * 
	 * @see #evaluate()
	 */
	public double avgOut = -1.0;

	/**
	 * The minimum sum of incoming and outgoing links.
	 * 
	 * @see #evaluate()
	 */
	public int minTot = -1;

	/**
	 * The maximum sum of incoming and outgoing links.
	 * 
	 * @see #evaluate()
	 */
	public int maxTot = -1;

	/**
	 * The average number of the sum of incoming and outgoing links.
	 * 
	 * @see #evaluate()
	 */
	public double avgTot = -1.0;

	/**
	 * The number of petals or tips in superstars.
	 * 
	 * @see #initGeometrySuperstar()
	 */
	public int petalscount = -1;

	/**
	 * The chain length in superstars.
	 * 
	 * @see #initGeometrySuperstar()
	 */
	public int petalsamplification = -1;

	/**
	 * The exponent in scale-free networks.
	 * 
	 * @see #initGeometryScaleFree()
	 */
	public double sfExponent = -2.0;

	/**
	 * The probability for adding links adjust small-world properties of
	 * Klemm-Eguiluz scale-free networks.
	 * 
	 * @see #initGeometryScaleFreeKlemm()
	 */
	public double pKlemm = -2.0;

	/**
	 * The difference between the number of neighbours on the left and the right in
	 * linear, 1D lattices.
	 */
	public int linearAsymmetry = 0;

	/**
	 * The degree or connectivity of the graph or (average) number of neighbours.
	 */
	public double connectivity = -1.0;

	/**
	 * Fraction of undirected links to add or rewire.
	 * 
	 * @see #rewireUndirected()
	 * @see #addUndirected()
	 */
	public double pRewire = -1.0;

	/**
	 * Fraction of directed links to add or rewire.
	 * 
	 * @see #rewireDirected()
	 * @see #addDirected()
	 */
	public double pAddwire = -1.0;

	/**
	 * {@code true} if the graph is undirected.
	 */
	public boolean isUndirected = true;

	/**
	 * {@code true} if the graph includes rewired edges or links.
	 */
	public boolean isRewired = false;

	/**
	 * Gets the opponent of the population represented by this graph. For
	 * intra-species models {@code this.opponent==this.population} holds.
	 * 
	 * @return the opponent population
	 */
	public IBSPopulation getOpponent() {
		return opponent;
	}

	/**
	 * Check if graph refers to inter-species model. For intra-species models
	 * {@code this.opponent==this.population} holds.
	 * 
	 * @return {@code true} for inter-species graphs.
	 */
	public boolean isInterspecies() {
		return (population != opponent);
	}

	/**
	 * {@code true} if the interaction and reproduction graphs are the same.
	 */
	public boolean interReproSame = true;

	/**
	 * Checks whether a single graphical representation can be used for the
	 * interaction and reproduction graphs. Two distinct graphical represntations
	 * are generally required if the two graphs differ but not if they both refer to
	 * the same lattice structure even if connectivities or boundary conditions are
	 * different.
	 *
	 * <h3>Examples:</h3>
	 * A single graphical representation is adequate:
	 * <ol>
	 * <li>if the interaction and reproduction graphs are identical,
	 * <li>if both the interaction and reproduction graphs are lattices, even if the
	 * boundary conditions or the connectivities are different,
	 * <li>but not if the interaction and reproduction graphs are separate instances
	 * of the same random structure, e.g. random regular graphs.
	 * </ol>
	 * <h3>Requirements/notes:</h3>
	 * <ol>
	 * <li>Tooltips need to be careful to report the different graphs and
	 * neighborhoods properly.
	 * </ol>
	 * 
	 * @param inter the interaction graph
	 * @param repro the reproduction graph
	 * @return {@code true} if a single graphical representation suffices,
	 *         {@code false} if two separate graphical representations are required
	 *         for the interaction and the reproduction graphs
	 * 
	 * @see #displayUniqueGeometry(Geometry, Geometry)
	 */
	public static boolean displayUniqueGeometry(Geometry inter, Geometry repro) {
		Type geometry = inter.geometry;
		if (inter.isLattice()) {
			// lattice interaction geometry - return true if reproduction geometry is the
			// same (regardless of connectivity)
			if (repro == null)
				return inter.interReproSame;
			// if both are square lattices a unique geometry is good enough
			if (geometry.isSquareLattice() && repro.geometry.isSquareLattice())
				return true;
			return (repro.geometry == geometry);
		}
		return inter.interReproSame;
	}

	/**
	 * Checks whether a single graphical representation can be used for the
	 * interaction and reproduction graphs.
	 * 
	 * @param module the population whose interaction and reproduction structures to
	 *               check
	 * @return {@code true} if a single graphical representation suffices
	 * 
	 * @see #displayUniqueGeometry(Geometry, Geometry)
	 */
	public static boolean displayUniqueGeometry(Module module) {
		return displayUniqueGeometry(module.getInteractionGeometry(), module.getReproductionGeometry());
	}

	/**
	 * {@code true} if the network structure is ephemeral.
	 */
	public boolean isDynamic = false;

	/**
	 * {@code true} if the network structure is regular (all nodes have the same
	 * number of neighbours).
	 */
	public boolean isRegular = false;

	/**
	 * {@code true} if the network structure has been successfully initialized.
	 */
	public boolean isValid = false;

	/**
	 * Convenience field. Array of zero length for initialization and well-mixed
	 * populations.
	 */
	private static final int[] nulllink = new int[0];

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
		geometry = Type.MEANFIELD;
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
		petalscount = -1;
		petalsamplification = -1;
		rawhierarchy = null;
		hierarchy = null;
		sfExponent = -2.0;
		connectivity = -1.0;
		pRewire = -1.0;
		pAddwire = -1.0;
		isUndirected = true;
		isRewired = false;
		interReproSame = true;
		isDynamic = false;
		isRegular = false;
		isValid = false;
	}

	/**
	 * Allocate the memory neccessary to store the network structure.
	 */
	public void alloc() {
		// allocate memory to hold links - avoid null values
		if (in == null || in.length != size) {
			in = new int[size][];
			kin = new int[size];
		}
		if (out == null || out.length != size) {
			out = new int[size][];
			kout = new int[size];
		}
		if (isUndirected && isRegular) {
			int k = (int) (connectivity + 0.5);
			for (int i = 0; i < size; i++) {
				in[i] = new int[k];
				out[i] = new int[k];
				// DEBUG - triggers exceptions to capture some bookkeeping issues
				// Arrays.fill(in[i], -1);
				// Arrays.fill(out[i], -1);
			}
		} else {
			Arrays.fill(in, nulllink);
			Arrays.fill(out, nulllink);
		}
		Arrays.fill(kin, 0);
		Arrays.fill(kout, 0);
	}

	/**
	 * Check the feasibility of the network parameters. If parameters need to be
	 * adjusted a warning is issued through the {@code logger}. Some changes require
	 * a reset of the model because they affect memory allocations (e.g. adjustments
	 * of the population size).
	 * 
	 * @return {@code true} if reset is required
	 */
	public boolean check() {
		boolean doReset = false;
		int side, side2; // helper variables for lattice structures

		// most graphs are undirected
		isUndirected = true;
		switch (geometry) {
			case COMPLETE:
				connectivity = (size - 1);
				petalsamplification = 1;
				if (pRewire > 0.0 || pAddwire > 0.0) {
					logger.warning("cannot add or rewire links for '" + geometry + "' - ignored!");
					pRewire = 0.0;
					pAddwire = 0.0;
				}
				break;
			case HIERARCHY:
				int nHierarchy = rawhierarchy.length;
				// NOTE: - structured populations with a single hierarchy can be meaningful
				// (neighbours vs everyone on a lattice).
				// not meaningful in well-mixed populations, though.
				// - hierarchies with only one unit can be collapsed
				for (int i = nHierarchy - 1; i >= 0; i--) {
					if (rawhierarchy[i] <= 1) {
						if (i < nHierarchy - 1)
							System.arraycopy(rawhierarchy, i + 1, rawhierarchy, i, nHierarchy - i - 1);
						nHierarchy--;
					}
				}
				if (nHierarchy == 0) {
					// no hierarchies remain
					// if subgeometry complete, well-mixed or structured with hierarchyweight==0
					// fall back on subgeometry
					if (subgeometry == Type.MEANFIELD || subgeometry == Type.COMPLETE || hierarchyweight <= 0.0) {
						geometry = subgeometry;
						logger.warning("hierarchies must encompass ≥2 levels - collapsed to geometry '"
								+ geometry + "'!");
						return check();
					}
					// maintain single hierarchy
					rawhierarchy[0] = 1;
					nHierarchy = 1;
				}
				if (nHierarchy != rawhierarchy.length) {
					// one or more hierarchies collapsed
					logger.warning("hierarchy levels must include >1 units - hierarchies collapsed to "
							+ (nHierarchy + 1) + " levels!");
				}
				if (hierarchy == null || hierarchy.length != nHierarchy + 1)
					hierarchy = new int[nHierarchy + 1];
				System.arraycopy(rawhierarchy, 0, hierarchy, 0, nHierarchy);
				int prod = 1;
				int nIndiv;
				// NOTE: if subgeometry is 'm' or 'n' it will be set to SQUARE and hence
				// connectivity will not be set again upon subsequent
				// calls to check(); square subgeometries with larger neighbourhoods are not
				// (yet) supported; any digit after 'N' is
				// interpreted as the number of units in the first hierarchy.
				// note; need to reset connectivity otherwise we cannot properly deal with the
				// FALL_THROUGH below (this makes extensions
				// to larger neighbourhood sizes more difficult but we'll cross that bridge
				// if/when we get there)
				connectivity = 0;
				switch (subgeometry) {
					case SQUARE_MOORE:
						connectivity = 8;
						//$FALL-THROUGH$
					case SQUARE_NEUMANN_2ND:
					case SQUARE_NEUMANN:
						connectivity = Math.max(connectivity, 4); // keep 8 if we fell through
						//$FALL-THROUGH$
					case SQUARE:
						if (nHierarchy != 1 || hierarchy[0] != 1) {
							// all levels of hierarchy must be square integers (exception is structured
							// population with hierarchyweight==0)
							for (int i = 0; i < nHierarchy; i++) {
								int sqrt = (int) Math.sqrt(hierarchy[i]);
								// every hierarchy level needs to be at least a 2x2 grid
								hierarchy[i] = Math.max(4, sqrt * sqrt);
								prod *= hierarchy[i];
							}
						}
						nIndiv = size / prod;
						int subside = (int) Math.sqrt(nIndiv);
						nIndiv = Math.max(9, subside * subside); // at least 3x3 grid of individuals per deme
						break;
					// NOTE: hierarchical stars might be interesting as well... stronger amplifiers?
					default:
						logger.warning("subgeometry '" + subgeometry
								+ "' not supported - well-mixed structure forced!");
						doReset = true;
						//$FALL-THROUGH$
					case COMPLETE:
						// avoid distinctions between MEANFIELD and COMPLETE graphs for subgeometries
						subgeometry = Type.MEANFIELD;
						//$FALL-THROUGH$
					case MEANFIELD:
						for (int i = 0; i < nHierarchy; i++)
							prod *= hierarchy[i];
						nIndiv = Math.max(2, size / prod); // at least two individuals per deme
						connectivity = (nIndiv - 1);
						break;
				}
				hierarchy[nHierarchy] = nIndiv;
				if (setSize(prod * nIndiv)) {
					// show size-change-warning only if an explicit population size was requested
					if (!engine.getModule().cloNPopulation.isDefault())
						logger.warning("hierarchical " + name //
								+ " geometry with levels " + Formatter.format(hierarchy)
								+ " requires population size of " + size + "!");
					doReset = true;
				}
				break;
			case LINEAR:
				// check connectivity
				connectivity = Math.max(1, Math.rint(connectivity));
				// connectivity of one is only allowed with asymmteric interactions or for
				// inter-species modules
				if ((Math.abs(1.0 - connectivity) < 1e-8 && (isInterspecies() || linearAsymmetry == 0)) //
						|| ((int) (connectivity + 0.5) % 2 == 1 && linearAsymmetry == 0) || connectivity >= size) {
					connectivity = Math.min(Math.max(2, connectivity + 1), size - 1 - (size - 1) % 2);
					logger.warning("linear " + name + " geometry requires even integer number of neighbors - using "
							+ connectivity + "!");
					doReset = true;
				}
				if (pRewire > 0.0 && connectivity < 2.0 + 1.0 / size) {
					logger.warning("cannot rewire links for '" + geometry + "' - ignored!");
					pRewire = 0.0;
				}
				break;
			case STAR:
				connectivity = 2.0 * (size - 1) / size;
				petalsamplification = 2;
				if (pRewire > 0.0) {
					logger.warning("cannot rewire links for '" + geometry + "' - ignored!");
					pRewire = 0.0;
				}
				break;
			case WHEEL:
				connectivity = 4.0 * (size - 1) / size;
				break;
			case SUPER_STAR:
				if (petalsamplification < 3) {
					petalsamplification = 3;
					logger.warning("super-star " + name + //
							" geometry requires amplification of >=3 - using " + petalsamplification + "!");
				}
				// check population size
				int pnodes = petalscount * (petalsamplification - 2);
				int nReservoir = (size - 1 - pnodes) / petalscount;
				if (setSize(nReservoir * petalscount + pnodes + 1)) {
					// show size-change-warning only if an explicit population size was requested
					if (!engine.getModule().cloNPopulation.isDefault())
						logger.warning("super-star " + name + " geometry requires special size - using " + size + "!");
					doReset = true;
				}
				connectivity = (double) (2 * nReservoir * petalscount + pnodes) / (double) size;
				isUndirected = false;
				break;
			case STRONG_SUPPRESSOR:
				int unit = (int) Math.floor(Math.pow(size, 0.25));
				if (setSize(unit * unit * (1 + unit * (1 + unit)))) {
					// show size-change-warning only if an explicit population size was requested
					if (!engine.getModule().cloNPopulation.isDefault())
						logger.warning("strong suppressor " + name //
								+ " geometry requires special size - using " + size + "!");
					doReset = true;
				}
				break;
			case STRONG_AMPLIFIER:
				// note unit^(1/3)>=5 must hold to ensure that epsilon<1
				int unit13 = Math.max(5, (int) Math.pow(size / 4, 1.0 / 3.0));
				int unit23 = unit13 * unit13;
				unit = unit23 * unit13;
				double lnunit = 3.0 * Math.log(unit13);
				double epsilon = lnunit / unit13;
				double alpha = 3.0 * lnunit / Math.log(1.0 + epsilon);
				if (setSize((int) (unit + (1 + alpha) * unit23 + 0.5))) {
					// show size-change-warning only if an explicit population size was requested
					if (!engine.getModule().cloNPopulation.isDefault())
						logger.warning("strong amplifier " + name //
								+ " geometry requires special size - using " + size + "!");
					doReset = true;
				}
				break;
			case SQUARE_MOORE: // moore
				connectivity = 8;
				//$FALL-THROUGH$
			case SQUARE_NEUMANN: // von neumann
			case SQUARE_NEUMANN_2ND:
				connectivity = Math.max(connectivity, 4); // keep 8 if we fell through
				//$FALL-THROUGH$
			case SQUARE:
				// check population size
				side = (int) Math.floor(Math.sqrt(size) + 0.5);
				if (geometry == Type.SQUARE_NEUMANN_2ND)
					side = (side + 1) / 2 * 2; // make sure side is even
				side2 = side * side;
				if (setSize(side2)) {
					// show size-change-warning only if an explicit population size was requested
					if (!engine.getModule().cloNPopulation.isDefault())
						logger.warning("square " + name //
								+ " geometry requires integer square size - using " + size + "!");
					doReset = true;
				}
				// check connectivity - must be 1, 4 or 3x3, 5x5, 7x7 etc.
				int range = Math.min(side / 2, Math.max(1, (int) (Math.sqrt(connectivity + 1.5) / 2.0)));
				int count = (2 * range + 1) * (2 * range + 1) - 1;
				if ((Math.abs(count - connectivity) > 1e-8 && Math.abs(4.0 - connectivity) > 1e-8
						&& Math.abs(1.0 - connectivity) > 1e-8)
						|| (Math.abs(1.0 - connectivity) < 1e-8 && !isInterspecies())) {
					connectivity = count;
					if (connectivity >= size)
						connectivity = 4; // simply reset to von Neumann
					logger.warning("square " + name //
							+ " geometry has invalid connectivity - using " + connectivity + "!");
					doReset = true;
				}
				break;
			case CUBE:
				if (size != 25000) {
					// check population size
					side = Math.max((int) Math.floor(Math.pow(size, 1.0 / 3.0) + 0.5), 2); // minimum side length is 2
					int side3 = side * side * side;
					if (setSize(side3)) {
						// show size-change-warning only if an explicit population size was requested
						if (!engine.getModule().cloNPopulation.isDefault())
							logger.warning("cubic " + name //
									+ " geometry requires integer cube size - using " + size + "!");
						doReset = true;
					}
					// check connectivity - must be 6 or 3x3x3, 5x5x5, 7x7x6 etc.
					range = Math.min(side / 2, Math.max(1, (int) (Math.pow(connectivity + 1.5, 1.0 / 3.0) / 2.0)));
				} else
					range = Math.min(4, Math.max(1, (int) (Math.pow(connectivity + 1.5, 1.0 / 3.0) / 2.0)));
				count = (2 * range + 1) * (2 * range + 1) * (2 * range + 1) - 1;
				if ((Math.abs(count - connectivity) > 1e-8 && Math.abs(6.0 - connectivity) > 1e-8
						&& Math.abs(1.0 - connectivity) > 1e-8)
						|| (Math.abs(1.0 - connectivity) < 1e-8 && !isInterspecies())) {
					connectivity = count;
					if (connectivity >= size)
						connectivity = 6; // simply reset to minimum
					logger.warning("cubic " + name //
							+ " geometry has invalid connectivity - using " + connectivity + "!");
					doReset = true;
				}
				break;
			case HONEYCOMB:
				// check population size and set connectivity
				side = (int) Math.floor(Math.sqrt(size) + 0.5);
				side2 = side * side;
				if (size != side2 || (side % 2) == 1) {
					side += side % 2;
					side2 = side * side;
					if (setSize(side2)) {
						// show size-change-warning only if an explicit population size was requested
						if (!engine.getModule().cloNPopulation.isDefault())
							logger.warning("hexagonal " + name //
									+ " geometry requires even integer square size - using " + size
									+ "!");
						doReset = true;
					}
				}
				if ((Math.abs(connectivity - 6) > 1e-8 && Math.abs(1.0 - connectivity) > 1e-8)
						|| (Math.abs(1.0 - connectivity) < 1e-8 && !isInterspecies())) {
					connectivity = 6;
					logger.warning("hexagonal " + name + " geometry requires connectivity 6!");
				}
				break;
			case TRIANGULAR:
				// check population size and set connectivity
				side = (int) Math.floor(Math.sqrt(size) + 0.5);
				side2 = side * side;
				if (size != side2 || (side % 2) == 1) {
					side += side % 2;
					side2 = side * side;
					if (setSize(side2)) {
						// show size-change-warning only if an explicit population size was requested
						if (!engine.getModule().cloNPopulation.isDefault())
							logger.warning("triangular " + name //
									+ " geometry requires even integer square size - using " + size
									+ "!");
						doReset = true;
					}
				}
				if ((Math.abs(connectivity - 3) > 1e-8 && Math.abs(1.0 - connectivity) > 1e-8)
						|| (Math.abs(1.0 - connectivity) < 1e-8 && !isInterspecies())) {
					connectivity = 3;
					logger.warning("triangular " + name + " geometry requires connectivity 3!");
				}
				break;
			case RANDOM_REGULAR_GRAPH:
				// check that number of links is even (since bidirectional); round connectivity
				// to integer
				connectivity = Math.min(Math.floor(connectivity), size - 1);
				int nConn = (int) connectivity;
				if ((size * nConn) % 2 == 1) {
					if (setSize(size + 1)) {
						// show size-change-warning only if an explicit population size was requested
						if (!engine.getModule().cloNPopulation.isDefault())
							logger.warning("RRG " + name //
									+ " geometry requires even (directed) link count - set size to " + size
									+ "!");
						doReset = true;
					}
				}
				break;
			case FRUCHT:
			case TIETZE:
			case FRANKLIN:
				if (setSize(12)) {
					// show size-change-warning only if an explicit population size was requested
					if (!engine.getModule().cloNPopulation.isDefault())
						logger.warning(
								(geometry == Type.FRUCHT ? "Frucht" : (geometry == Type.TIETZE ? "Tietze" : "Franklin"))
										+ " graph " + name + " geometry requires size 12!");
					doReset = true;
				}
				connectivity = 3.0;
				break;
			case HEAWOOD:
				if (setSize(14)) {
					// show size-change-warning only if an explicit population size was requested
					if (!engine.getModule().cloNPopulation.isDefault())
						logger.warning("Heawood graph " + name + " geometry requires size 14!");
					doReset = true;
				}
				connectivity = 3.0;
				break;
			case ICOSAHEDRON:
				if (setSize(12)) {
					// show size-change-warning only if an explicit population size was requested
					if (!engine.getModule().cloNPopulation.isDefault())
						logger.warning("Icosahedron graph " + name + " geometry requires size 12!");
					doReset = true;
				}
				connectivity = 5.0;
				break;
			case DODEKAHEDRON:
			case DESARGUES:
				if (setSize(20)) {
					// show size-change-warning only if an explicit population size was requested
					if (!engine.getModule().cloNPopulation.isDefault())
						logger.warning((geometry == Type.DODEKAHEDRON ? "Dodekahedron" : "Desargues") + " graph " + name
								+ " geometry requires size 20!");
					doReset = true;
				}
				connectivity = 3.0;
				break;
			case MEANFIELD:
				petalsamplification = 1;
				//$FALL-THROUGH$
			case RANDOM_GRAPH:
				break;
			case RANDOM_GRAPH_DIRECTED:
				isUndirected = false;
				break;
			case SCALEFREE: // bi-directional scale-free network
				if (sfExponent >= 0.0) {
					logger.warning("Exponent for scale-free graph is " + Formatter.format(connectivity, 2)
							+ " but must be <0. Resorting to random graph.");
					geometry = Type.RANDOM_GRAPH;
					return check();
				}
				// check connectivity
				double norm = 0.0;
				double conn = 0.0;
				for (int n = 0; n < size; n++) {
					double distrn = Math.pow((double) n / (double) size, sfExponent);
					norm += distrn;
					conn += n * distrn;
				}
				conn /= norm;
				double max = size * 0.5;
				// check feasibility of increasing the number of links
				if (conn < connectivity && connectivity > max) {
					logger.warning("Requested connectivity " + Formatter.format(connectivity, 2)
							+ " too high. Reduced to " + Formatter.format(max, 2) + ".");
					connectivity = max;
				}
				break;
			case SCALEFREE_BA: // bi-directional scale-free network (barabasi-albert model)
			case SCALEFREE_KLEMM: // bi-directional scale-free network (klemm-eguiluz model)
				break;
			case GENERIC:
			case DYNAMIC:
				break;

			default:
				// last resort: try engine - maybe new implementations provide new geometries
				if (!population.checkGeometry(this))
					throw new Error("Unknown geometry");
		}
		if (pRewire > 0.0) {
			switch ((int) (connectivity + 1e-6)) {
				case 1: // k=1-2 don't even try
					logger.severe("rewiring needs higher connectivity (should be >3 instead of "
							+ Formatter.format(connectivity, 2) + ") - ignored");
					pRewire = 0.0;
					break;
				case 2: // k=2-3 challenging
					logger.warning("consider higher connectivity for rewiring (should be >3 instead of "
							+ Formatter.format(connectivity, 2) + ")");
					break;
				default: // cross fingers
			}
			if (connectivity > size - 2) {
				logger.severe("complete graph, rewiring impossible - ignored");
				pRewire = 0.0;
			} else if (connectivity > size / 2) {
				logger.warning("consider lower connectivity for rewiring (" + Formatter.format(connectivity, 2) + ")");
			}
		}
		return doReset;
	}

	/**
	 * Entry point to initialize the network structure according to the given
	 * parameters.
	 * 
	 * @see Geometry.Type
	 * @see #parse(String)
	 * @see #check()
	 */
	public void init() {
		switch (geometry) {
			case MEANFIELD:
				initGeometryMeanField();
				break;
			case COMPLETE:
				initGeometryComplete();
				break;
			case HIERARCHY:
				initGeometryHierarchical();
				break;
			case LINEAR:
				initGeometryLinear();
				break;
			case STAR:
				initGeometryStar();
				break;
			case WHEEL:
				initGeometryWheel();
				break;
			case SUPER_STAR:
				initGeometrySuperstar();
				break;
			case STRONG_AMPLIFIER:
				initGeometryAmplifier();
				break;
			case STRONG_SUPPRESSOR:
				initGeometrySuppressor();
				break;
			case SQUARE_NEUMANN:
			case SQUARE_NEUMANN_2ND:
			case SQUARE_MOORE:
			case SQUARE:
				initGeometrySquare();
				break;
			case CUBE:
				initGeometryCube();
				break;
			case HONEYCOMB:
				initGeometryHoneycomb();
				break;
			case TRIANGULAR:
				initGeometryTriangular();
				break;
			case FRUCHT:
				initGeometryFruchtGraph();
				break;
			case TIETZE:
				initGeometryTietzeGraph();
				break;
			case FRANKLIN:
				initGeometryFranklinGraph();
				break;
			case HEAWOOD:
				initGeometryHeawoodGraph();
				break;
			case ICOSAHEDRON:
				initGeometryIcosahedronGraph();
				break;
			case DODEKAHEDRON:
				initGeometryDodekahedronGraph();
				break;
			case DESARGUES:
				initGeometryDesarguesGraph();
				break;
			case RANDOM_GRAPH:
				initGeometryRandomGraph();
				break;
			case RANDOM_GRAPH_DIRECTED:
				initGeometryRandomGraphDirected();
				break;
			case RANDOM_REGULAR_GRAPH:
				initGeometryRandomRegularGraph();
				break;
			case SCALEFREE: // bi-directional scale-free network
				initGeometryScaleFree();
				break;
			case SCALEFREE_BA: // bi-directional scale-free network (barabasi-albert model)
				initGeometryScaleFreeBA();
				break;
			case SCALEFREE_KLEMM: // bi-directional scale-free network (klemm-eguiluz model)
				initGeometryScaleFreeKlemm();
				break;
			default:
				// last resort: try engine - maybe new implementations provide new geometries
				if (!population.generateGeometry(this))
					throw new Error("Unknown geometry");
		}
		isValid = true;
		evaluated = false;
	}

	/**
	 * Report relevant parameters of geometry and print to {@code output}.
	 * 
	 * @param output the print stream to direct the output to
	 */
	public void printParams(PrintStream output) {
		switch (geometry) {
			case SUPER_STAR:
				output.println("# connectivity:         " + Formatter.format(connectivity, 4));
				output.println("# petalscount:          " + Formatter.format(petalscount, 0));
				output.println("# amplification:        " + Formatter.format(petalsamplification, 4));
				break;
			case LINEAR:
				if (linearAsymmetry != 0) {
					output.println("# connectivity:         " + Formatter.format(connectivity, 4) + " (left: "
							+ ((connectivity + linearAsymmetry) / 2) + ", right: "
							+ ((connectivity - linearAsymmetry) / 2) + ")");
					break;
				}
				//$FALL-THROUGH$
			case MEANFIELD:
			case COMPLETE:
			case STAR:
			case WHEEL:
			case SQUARE_NEUMANN:
			case SQUARE_NEUMANN_2ND:
			case SQUARE_MOORE:
			case SQUARE:
			case CUBE:
			case HONEYCOMB:
			case TRIANGULAR:
			case RANDOM_GRAPH:
			case RANDOM_GRAPH_DIRECTED:
			case RANDOM_REGULAR_GRAPH:
			case SCALEFREE:
			case SCALEFREE_BA:
			case SCALEFREE_KLEMM:
			default:
				output.println("# connectivity:         " + Formatter.format(connectivity, 4));
		}
	}

	/**
	 * Generates well-mixed graph, also termed mean-field networks or unstructured
	 * populations.
	 *
	 * <h3>Requirements/notes:</h3>
	 * In the limit of large population sizes the results of individual based
	 * simulations (IBS) must converge to those of the corresponding deterministic
	 * dynamical equations (ODE's) or, with mutations, the stochastic dynamical
	 * equations (SDE's).
	 */
	public void initGeometryMeanField() {
		isRewired = false;
		isUndirected = true;
		isRegular = true;
		// allocate minimal memory - just in case
		alloc();
	}

	/**
	 * Generates a complete graph where every node is connected to every other node.
	 * This is very similar to well-mixed (unstructured) populations. The only
	 * difference is the potential treatment of the focal node. For example, in the
	 * Moran process offspring can replace their parent in the original formulation
	 * for well-mixed populations (birth-death updating) but this does not occur in
	 * complete networks where offspring replaces one of the parents neighbours.
	 *
	 * <h3>Requirements/notes:</h3>
	 * None.
	 * 
	 * @see <a href="https://en.wikipedia.org/wiki/Complete_graph">Wikipedia:
	 *      Complete graph</a>
	 * @see #initGeometryMeanField()
	 */
	public void initGeometryComplete() {
		int size1 = size - 1;
		isRewired = false;
		isUndirected = true;
		isRegular = true;
		connectivity = size1;
		alloc();

		for (int n = 0; n < size; n++) {
			// setting in- and outlinks equal saves some memory
			int[] links = new int[size1];
			in[n] = links;
			kin[n] = size1;
			out[n] = links;
			kout[n] = size1;
			for (int i = 0; i < size1; i++) {
				if (i >= n)
					links[i] = i + 1;
				else
					links[i] = i;
			}
		}
	}

	/**
	 * Generates hierarchical graphs.
	 * 
	 * <h3>Requirements/notes:</h3>
	 * Only well-mixed (complete) or square lattice graphs are currently supported.
	 */
	public void initGeometryHierarchical() {
		// initialize interaction network
		isRewired = false;
		isRegular = false;
		isUndirected = true;
		alloc();
		// hierarchical structures call for recursive initialization/generation
		initGeometryHierarchy(0, 0);
	}

	/**
	 * Utility method to generate hierarchical graphs.
	 *
	 * <h3>Requirements/notes:</h3>
	 * None.
	 * 
	 * @param level the hierarchical level
	 * @param start the index of the first node to process
	 */
	private void initGeometryHierarchy(int level, int start) {
		if (level == hierarchy.length - 1) {
			// bottom level reached
			int nIndiv = hierarchy[level];
			int end = start + nIndiv;
			switch (subgeometry) {
				case SQUARE_NEUMANN:
				case SQUARE_NEUMANN_2ND:
				case SQUARE_MOORE:
				case SQUARE:
					initGeometryHierarchySquare(start, end);
					return;
				case MEANFIELD:
				case COMPLETE:
				default:
					initGeometryHierarchyMeanfield(start, end);
					return;
			}
		}
		// recursion
		switch (subgeometry) {
			case SQUARE_NEUMANN:
			case SQUARE_NEUMANN_2ND:
			case SQUARE_MOORE:
			case SQUARE:
				int side = (int) Math.sqrt(size);
				int hskip = 1;
				for (int dd = level + 1; dd < hierarchy.length; dd++)
					hskip *= (int) Math.sqrt(hierarchy[dd]);
				int hside = (int) Math.sqrt(hierarchy[level]);
				for (int i = 0; i < hside; i++) {
					for (int j = 0; j < hside; j++) {
						initGeometryHierarchy(level + 1, start + (i + j * side) * hskip);
					}
				}
				break;
			case MEANFIELD:
			case COMPLETE:
			default:
				hskip = 1;
				for (int dd = level + 1; dd < hierarchy.length; dd++)
					hskip *= hierarchy[dd];
				int skip = start;
				for (int d = 0; d < hierarchy[level]; d++) {
					initGeometryHierarchy(level + 1, skip);
					skip += hskip;
				}
		}
	}

	/**
	 * Utility method to generate hierarchical well-mixed subpopulations (demes).
	 *
	 * <h3>Requirements/notes:</h3>
	 * None.
	 * 
	 * @param start the index of the first node to process
	 * @param end   the index of the last node to process
	 * @return the number of nodes processed
	 */
	private int initGeometryHierarchyMeanfield(int start, int end) {
		int nIndiv = end - start;
		int nIndiv1 = nIndiv - 1;
		for (int n = start; n < end; n++) {
			// setting in- and outlinks equal saves some memory
			int[] links = new int[nIndiv1];
			for (int i = 0; i < nIndiv1; i++) {
				if (start + i >= n)
					links[i] = start + i + 1;
				else
					links[i] = start + i;
			}
			in[n] = links;
			out[n] = links;
			kin[n] = nIndiv1;
			kout[n] = nIndiv1;
		}
		return nIndiv;
	}

	/**
	 * Utility method to generate hierarchical square lattices with degree
	 * {@code degree}.
	 *
	 * <h3>Requirements/notes:</h3>
	 * None.
	 * 
	 * @param degree the degree of the square lattice
	 * @param start  the index of the first node to process
	 * @param end    the index of the last node to process
	 * @return the number of nodes processed
	 */
	private int initGeometryHierarchySquare(int start, int end) {
		int nIndiv = end - start;
		int dside = (int) Math.sqrt(nIndiv);
		int side = (int) Math.sqrt(size);
		switch (geometry) {
			case SQUARE_NEUMANN: // von Neumann
				initGeometrySquareVonNeumann(dside, side, start);
				break;
			case SQUARE_NEUMANN_2ND: // von Neumann
				initGeometrySquareVonNeumann2nd(dside, side, start);
				break;
			case SQUARE_MOORE: // Moore
				initGeometrySquareMoore(dside, side, start);
				break;
			case SQUARE: // square lattice
			default:
				initGeometrySquare(dside, side, start);
				break;
		}
		return nIndiv;
	}

	/**
	 * Generates a linear chain (1D lattice). With asymmetric neighbours and fixed
	 * boundaries this represents the simplest suppressor of selection.
	 *
	 * <h3>Requirements/notes:</h3>
	 * <ol>
	 * <li>May have different numbers of neighbours to the left and the right.
	 * <li>For inter-species interactions connectivities are \(+1\) and a
	 * connectivity of \(1\) is admissible.
	 * <li>Boundaries fixed or periodic (default).
	 * </ol>
	 */
	public void initGeometryLinear() {
		isRewired = false;
		isRegular = !fixedBoundary; // is regular if boundaries periodic
		alloc();
		boolean isInterspecies = isInterspecies();

		int left = ((int) (connectivity + 0.5) + linearAsymmetry) / 2;
		int right = ((int) (connectivity + 0.5) - linearAsymmetry) / 2;
		isUndirected = (left == right);
		for (int i = 0; i < size; i++) {
			for (int j = -left; j <= right; j++) {
				if ((j == 0 && !isInterspecies) || (fixedBoundary && (i + j >= size || i + j < 0)))
					continue;
				addLinkAt(i, (i + j + size) % size);
			}
		}
	}

	/**
	 * Generates a star geometry with a hub in the middle that is connected to all
	 * other nodes (leaves). The star structure is the simplest undirected
	 * evolutionary amplifier.
	 *
	 * <h3>Requirements/notes:</h3>
	 * Node \(0\) is the hub.
	 */
	public void initGeometryStar() {
		isRewired = false;
		isUndirected = true;
		isRegular = false;
		alloc();

		// hub is node 0
		for (int i = 1; i < size; i++) {
			addLinkAt(0, i);
			addLinkAt(i, 0);
		}
	}

	/**
	 * Generates a wheel geometry, which corresponds to a ring (periodic 1D lattice)
	 * with a hub in the middle that is connected to all nodes in the ring
	 * (resembling spokes). The wheel structure is an undirected evolutionary
	 * amplifier but less than the star structure.
	 *
	 * <h3>Requirements/notes:</h3>
	 * Node \(0\) is the hub.
	 * 
	 * @see #initGeometryStar()
	 */
	public void initGeometryWheel() {
		int size1 = size - 1;
		isRewired = false;
		isUndirected = true;
		isRegular = false;
		alloc();

		// hub is node 0
		for (int i = 0; i < size1; i++) {
			addLinkAt(i + 1, (i - 1 + size1) % size1 + 1);
			addLinkAt(i + 1, (i + 1 + size1) % size1 + 1);
			addLinkAt(0, i + 1);
			addLinkAt(i + 1, 0);
		}
	}

	/**
	 * Generates a superstar geometry, which represents a strong directed amplifier
	 * of selection. The superstar consists of a central hub surrounded by \(p\)
	 * petals that are each equipped with a reservoir of size \(r\) and a linear
	 * chain of length \(k\) connecting the reservoir with the hub (last node of
	 * each chain). The hub connects to all reservoir nodes (in all petals) and all
	 * reservoir nodes in each petal connect to the first node in the linear chain.
	 * The superstar represents the best studied evolutionary amplifier of arbitrary
	 * strength (in the limit \(N\to\infty\) as well as \(p, r, k \to\infty\)).
	 *
	 * <h3>Requirements/notes:</h3>
	 * <ol>
	 * <li>Population size: \(N=(r+k-1)p+1\) whith \(r,k,p\) integer and \(r\gg
	 * k,p\).
	 * <li>Node \(0\) is the hub.
	 * </ol>
	 * 
	 * @see <a href="http://dx.doi.org/10.1038/nature03204">Lieberman, E., Hauert,
	 *      Ch. &amp; Nowak, M. A. (2005) Evolutionary dynamics on graphs, Nature
	 *      433 312-316.</a>
	 */
	public void initGeometrySuperstar() {
		isRewired = false;
		isUndirected = false;
		isRegular = false;
		alloc();

		// hub is node 0, outermost petals are nodes 1 - p
		// inner petal nodes are p+1 - 2p, 2p+1 - 3p etc.
		// petal nodes (kernel-2)p+1 - (kernel-2)p+p=(kernel-1)p are connected to hub
		int pnodes = petalscount * (petalsamplification - 2);

		// connect hub
		for (int i = pnodes + 1; i < size; i++) {
			addLinkAt(0, i);
			addLinkAt(i, (i - pnodes - 1) % petalscount + 1);
		}

		// chain petals - outer petal nodes to inner petal nodes
		for (int i = 1; i <= (pnodes - petalscount); i++)
			addLinkAt(i, i + petalscount);

		// connect petals - inner petal nodes to hub
		for (int i = 1; i <= petalscount; i++)
			addLinkAt(pnodes - petalscount + i, 0);
	}

	/**
	 * Generates a strong undirected amplifier of selection.
	 *
	 * <h3>Requirements/notes:</h3>
	 * Population size: \(N=n^3+(1+a)n^2\) where \(n\) integer and a suitable
	 * \(a\geq 5\). Three types of nodes \(U=n^3\), \(V=n^2\) and \(W=N-U-V\) where
	 * \(W\) represents a regular graph core with degree \(n^2\), each node in \(U\)
	 * is a leaf and connected to a single node in \(V\), while each node in \(V\)
	 * is connected to \(n^2\) nodes in \(W\).
	 * 
	 * @see <a href="https://doi.org/10.48550/arXiv.1611.01585">Giakkoupis, George
	 *      (2016) Amplifiers and Suppressors of Selection for the Moran Process on
	 *      Undirected Graphs, arXiv 1611.01585</a>
	 */
	public void initGeometryAmplifier() {
		isRewired = false;
		isUndirected = true;
		isRegular = false;
		alloc();

		// retrieve the shared RNG to ensure reproducibility of results
		RNGDistribution rng = engine.getRNG();
		int unit13 = Math.max(5, (int) Math.pow(size / 4, 1.0 / 3.0));
		int unit23 = unit13 * unit13;
		int unit = unit23 * unit13;
		int nU = unit, nV = unit23, nW = size - nU - nV;
		// recall: size = unit^3+(1+a)x^2 for suitable a
		// three types of nodes: unit^3 in U, unit^2 in V and rest in W
		// arrangement: W (regular graph core), V, U
		int w0 = 0, wn = nW, v0 = wn, vn = v0 + nV, u0 = vn;// , un = size;
		// step 1: create core of (approximate) random regular graph of degree unit^2
		initRRGCore(rng, w0, wn, unit23);
		// each node in U is a leaf, connected to a single node in V, and each node in V
		// is connected to unit^2 nodes in W
		int idxU = u0;
		for (int v = v0; v < vn; v++) {
			for (int n = 0; n < unit13; n++)
				addEdgeAt(v, idxU++);
			int l = unit23;
			while (l > 0) {
				int idx = rng.random0n(nW);
				if (isNeighborOf(v, idx))
					continue;
				addEdgeAt(v, idx);
				l--;
			}
		}
	}

	/**
	 * Utility method to generate an (almost) regular graph as the core of an
	 * evolutionary amplifier.
	 *
	 * <h3>Requirements/notes:</h3>
	 * In contrast to {@link #initGeometryRandomRegularGraph()} no extra effort
	 * is made to ensure regularity.
	 * 
	 * @param rng    the random number generator
	 * @param start  the index of the first node in the (almost) regular graph
	 * @param end    the index of the last node in the (almost) regular graph
	 * @param degree the degree/connectivity of the (almost) regular graph
	 */
	private void initRRGCore(RNGDistribution rng, int start, int end, int degree) {
		int nTodo = end - start;
		int nLinks = nTodo * degree;
		int[] todo = new int[nTodo];
		for (int n = 0; n < nTodo; n++)
			todo[n] = n;

		// ensure connectedness for static graphs
		int[] active = new int[nTodo];
		int idxa = rng.random0n(nTodo);
		active[0] = todo[idxa];
		nTodo--;
		if (idxa != nTodo)
			System.arraycopy(todo, idxa + 1, todo, idxa, nTodo - idxa);
		int nActive = 1;
		while (nTodo > 0) {
			idxa = rng.random0n(nActive);
			int nodea = active[idxa];
			int idxb = rng.random0n(nTodo);
			int nodeb = todo[idxb];
			addEdgeAt(nodea, nodeb);
			if (kout[nodea] == degree) {
				nActive--;
				if (idxa != nActive)
					System.arraycopy(active, idxa + 1, active, idxa, nActive - idxa);
			}
			// degree of nodeb not yet reached - add to active list
			if (kout[nodeb] < degree)
				active[nActive++] = nodeb;
			// remove nodeb from core of unconnected nodes
			nTodo--;
			if (idxb != nTodo)
				System.arraycopy(todo, idxb + 1, todo, idxb, nTodo - idxb);
		}
		// now we have a connected graph
		todo = active;
		nTodo = nActive;
		nLinks -= 2 * (end - start - 1);

		// ideally we should go from nTodo=2 to zero but a single node with a different
		// degree is acceptable
		while (nTodo > 1) {
			int a = rng.random0n(nLinks);
			int b = rng.random0n(nLinks - 1);
			if (b >= a)
				b++;

			// identify nodes
			idxa = 0;
			int nodea = todo[idxa];
			a -= degree - kout[nodea];
			while (a >= 0) {
				nodea = todo[++idxa];
				a -= degree - kout[nodea];
			}
			int idxb = 0, nodeb = todo[idxb];
			b -= degree - kout[nodeb];
			while (b >= 0) {
				nodeb = todo[++idxb];
				b -= degree - kout[nodeb];
			}

			if (nodea == nodeb || isNeighborOf(nodea, nodeb))
				continue;
			addEdgeAt(nodea, nodeb);
			nLinks -= 2;
			if (kout[nodea] == degree) {
				nTodo--;
				if (idxa != nTodo)
					System.arraycopy(todo, idxa + 1, todo, idxa, nTodo - idxa);
				if (idxb > idxa)
					idxb--;
			}
			if (kout[nodeb] == degree) {
				nTodo--;
				if (idxb != nTodo)
					System.arraycopy(todo, idxb + 1, todo, idxb, nTodo - idxb);
			}
		}
	}

	/**
	 * Generates a strong undirected suppressor of selection.
	 * 
	 * <h3>Requirements/notes:</h3>
	 * Population size: \(N=n^2(1+n(1+n))=n^2+n^3+n^4\) for \(n\) integer. With
	 * three types of nodes \(U=n^3\), \(V=n^4\) and \(W=n^2\), each node in \(V\)
	 * is connected to one node in \(U\) and to all in \(W\).
	 * 
	 * @see <a href="https://doi.org/10.48550/arXiv.1611.01585">Giakkoupis, George
	 *      (2016) Amplifiers and Suppressors of Selection for the Moran Process on
	 *      Undirected Graphs, arXiv 1611.01585</a>
	 */
	public void initGeometrySuppressor() {
		isRewired = false;
		isUndirected = true;
		isRegular = false;
		alloc();

		int unit = (int) Math.floor(Math.pow(size, 0.25));
		// recall: size = unit^2(1+unit(1+unit)) = unit^2+unit^3+unit^4
		// three types of nodes: unit^2 in W, unit^4 in V and unit^3 in U
		// nodes: V, W, U
		int v0 = 0, vn = (int) Combinatorics.pow(unit, 4), w0 = vn, wn = vn + unit * unit, u0 = wn; // , un = size;
		// each node in V is connected to one node in U and to all nodes in W
		for (int v = v0; v < vn; v++) {
			int u = u0 + (v - v0) / unit;
			addEdgeAt(v, u);
			for (int w = w0; w < wn; w++)
				addEdgeAt(v, w);
		}
	}

	/**
	 * Generates a square regular lattice (chessboard). The most common lattices are
	 * the von Neumann lattice with four nearest neighbours (north, south, east and
	 * west) as well as the Moore lattice with eight nearest neighbours (chess
	 * king's moves).
	 *
	 * <h3>Requirements/notes:</h3>
	 * <ol>
	 * <li>Integer square population size, \(N=n^2\).
	 * <li>Admissible connectivities: \(4\) (von Neumann) or \(3\times 3-1=8\)
	 * (Moore), \(5\times 5-1=24\), \(7\times 7-1=48\) etc.
	 * <li>For inter-species interactions connectivities are \(+1\) and a
	 * connectivity of \(1\) is admissible.
	 * <li>Boundaries are periodic by default.
	 * <li>For {@code SQUARE_MOORE} interactions with {@code Group.SAMPLING_ALL} and
	 * group sizes between {@code 2} and {@code 8} (excluding boundaries) relies on
	 * a particular arrangement of the neighbors, see
	 * {@link org.evoludo.simulator.models.IBSDPopulation#playGroupGameAt(org.evoludo.simulator.models.IBSGroup)
	 * IBSDPopulation.playGroupGameAt(IBSGroup)}
	 * and
	 * {@link org.evoludo.simulator.models.IBSCPopulation#playGroupGameAt(org.evoludo.simulator.models.IBSGroup)
	 * IBSCPopulation.playGroupGameAt(IBSGroup)}.
	 * </ol>
	 * 
	 * @see <a href=
	 *      "https://en.wikipedia.org/wiki/Von_Neumann_neighborhood">Wikipedia: von
	 *      Neumann neighborhood</a>
	 * @see <a href="https://en.wikipedia.org/wiki/Moore_neighborhood">Wikipedia:
	 *      Moore neighborhood</a>
	 */
	public void initGeometrySquare() {
		isRewired = false;
		isUndirected = true;
		isRegular = true;
		alloc();

		int side = (int) Math.floor(Math.sqrt(size) + 0.5);
		switch (geometry) {
			case SQUARE_NEUMANN:
				initGeometrySquareVonNeumann(side, side, 0);
				break;
			case SQUARE_NEUMANN_2ND:
				initGeometrySquareVonNeumann2nd(side, side, 0);
				break;
			case SQUARE_MOORE:
				initGeometrySquareMoore(side, side, 0);
				break;
			case SQUARE:
			default:
				if ((int) Math.rint(connectivity) == 1)
					initGeometrySquareSelf(side, side, 0);
				else
					initGeometrySquare(side, side, 0);
		}
	}

	/**
	 * Utility method to generate a square lattice with exclusively
	 * 'self'-connections.
	 *
	 * <h3>Requirements/notes:</h3>
	 * <ol>
	 * <li>Makes only sense in inter-species interactions.
	 * <li>Inter-species interactions include 'self'-connections.
	 * <li>Hierarchical structures may call this method recursively.
	 * <li>Neighbourhood corresponds to a \(3\times 3\), \(5\times 5\), \(7\times
	 * 7\), etc. area.
	 * <li>Boundaries fixed or periodic (default).
	 * </ol>
	 * 
	 * @param side     the side length of the (sub) lattice
	 * @param fullside the full side length of the entire lattice
	 * @param offset   the offset to the sub-lattice
	 */
	private void initGeometrySquareSelf(int side, int fullside, int offset) {
		int aPlayer;
		for (int i = 0; i < side; i++) {
			int x = offset + i * fullside;
			for (int j = 0; j < side; j++) {
				aPlayer = x + j;
				addLinkAt(aPlayer, aPlayer);
			}
		}
	}

	/**
	 * Utility method to generate a square lattice with von Neumann neighbourhood.
	 *
	 * <h3>Requirements/notes:</h3>
	 * <ol>
	 * <li>Inter-species interactions include 'self'-connections.
	 * <li>Hierarchical structures may call this method recursively.
	 * <li>Neighbourhood corresponds to a \(3\times 3\), \(5\times 5\), \(7\times
	 * 7\), etc. area.
	 * <li>Boundaries fixed or periodic (default).
	 * </ol>
	 * 
	 * @param side     the side length of the (sub) lattice
	 * @param fullside the full side length of the entire lattice
	 * @param offset   the offset to the sub-lattice
	 */
	private void initGeometrySquareVonNeumann(int side, int fullside, int offset) {
		int aPlayer;
		boolean isInterspecies = isInterspecies();

		for (int i = 0; i < side; i++) {
			int x = i * fullside;
			int u = ((i - 1 + side) % side) * fullside;
			int d = ((i + 1) % side) * fullside;
			for (int j = 0; j < side; j++) {
				int r = (j + 1) % side;
				int l = (j - 1 + side) % side;
				aPlayer = offset + x + j;
				if (isInterspecies)
					addLinkAt(aPlayer, aPlayer);
				addLinkAt(aPlayer, offset + u + j);
				addLinkAt(aPlayer, offset + x + r);
				addLinkAt(aPlayer, offset + d + j);
				addLinkAt(aPlayer, offset + x + l);
			}
		}
		if (fixedBoundary) {
			// corners
			aPlayer = offset;
			clearLinksFrom(aPlayer); // upper-left corner
			if (isInterspecies)
				addLinkAt(aPlayer, aPlayer);
			addLinkAt(aPlayer, aPlayer + 1); // right
			addLinkAt(aPlayer, aPlayer + fullside); // down
			aPlayer = offset + side - 1;
			clearLinksFrom(aPlayer); // upper-right corner
			if (isInterspecies)
				addLinkAt(aPlayer, aPlayer);
			addLinkAt(aPlayer, aPlayer - 1); // left
			addLinkAt(aPlayer, aPlayer + fullside); // down
			aPlayer = offset + (side - 1) * fullside;
			clearLinksFrom(aPlayer); // lower-left corner
			if (isInterspecies)
				addLinkAt(aPlayer, aPlayer);
			addLinkAt(aPlayer, aPlayer + 1); // right
			addLinkAt(aPlayer, aPlayer - fullside); // up
			aPlayer = offset + (side - 1) * (fullside + 1);
			clearLinksFrom(aPlayer); // lower-right corner
			if (isInterspecies)
				addLinkAt(aPlayer, aPlayer);
			addLinkAt(aPlayer, aPlayer - 1); // left
			addLinkAt(aPlayer, aPlayer - fullside); // up
			// edges
			for (int i = 1; i < (side - 1); i++) {
				// top
				aPlayer = offset + i;
				clearLinksFrom(aPlayer);
				if (isInterspecies)
					addLinkAt(aPlayer, aPlayer);
				addLinkAt(aPlayer, aPlayer - 1);
				addLinkAt(aPlayer, aPlayer + 1);
				addLinkAt(aPlayer, aPlayer + fullside);
				// bottom
				aPlayer = offset + (side - 1) * fullside + i;
				clearLinksFrom(aPlayer);
				if (isInterspecies)
					addLinkAt(aPlayer, aPlayer);
				addLinkAt(aPlayer, aPlayer - 1);
				addLinkAt(aPlayer, aPlayer + 1);
				addLinkAt(aPlayer, aPlayer - fullside);
				// left
				aPlayer = offset + fullside * i;
				clearLinksFrom(aPlayer);
				if (isInterspecies)
					addLinkAt(aPlayer, aPlayer);
				addLinkAt(aPlayer, aPlayer + 1);
				addLinkAt(aPlayer, aPlayer - fullside);
				addLinkAt(aPlayer, aPlayer + fullside);
				// right
				aPlayer = offset + fullside * i + side - 1;
				clearLinksFrom(aPlayer);
				if (isInterspecies)
					addLinkAt(aPlayer, aPlayer);
				addLinkAt(aPlayer, aPlayer - 1);
				addLinkAt(aPlayer, aPlayer - fullside);
				addLinkAt(aPlayer, aPlayer + fullside);
			}
			isRegular = false;
		}
	}

	/**
	 * Utility method to generate a square lattice with von Neumann neighbourhood.
	 *
	 * <h3>Requirements/notes:</h3>
	 * <ol>
	 * <li>Inter-species interactions include 'self'-connections.
	 * <li>Hierarchical structures may call this method recursively.
	 * <li>Neighbourhood corresponds to a \(3\times 3\), \(5\times 5\), \(7\times
	 * 7\), etc. area.
	 * <li>Boundaries fixed or periodic (default).
	 * </ol>
	 * 
	 * @param side     the side length of the (sub) lattice
	 * @param fullside the full side length of the entire lattice
	 * @param offset   the offset to the sub-lattice
	 */
	private void initGeometrySquareVonNeumann2nd(int side, int fullside, int offset) {
		int aPlayer;
		boolean isInterspecies = isInterspecies();

		for (int i = 0; i < side; i++) {
			int x = i * fullside;
			int u = ((i - 1 + side) % side) * fullside;
			int d = ((i + 1) % side) * fullside;
			for (int j = 0; j < side; j++) {
				int r = (j + 1) % side;
				int l = (j - 1 + side) % side;
				aPlayer = offset + x + j;
				if (isInterspecies)
					addLinkAt(aPlayer, aPlayer);
				addLinkAt(aPlayer, offset + u + l);
				addLinkAt(aPlayer, offset + u + r);
				addLinkAt(aPlayer, offset + d + l);
				addLinkAt(aPlayer, offset + d + r);
			}
		}
		if (fixedBoundary) {
			// corners
			aPlayer = offset;
			clearLinksFrom(aPlayer); // upper-left corner
			if (isInterspecies)
				addLinkAt(aPlayer, aPlayer);
			addLinkAt(aPlayer, aPlayer + fullside + 1); // right, down
			aPlayer = offset + side - 1;
			clearLinksFrom(aPlayer); // upper-right corner
			if (isInterspecies)
				addLinkAt(aPlayer, aPlayer);
			addLinkAt(aPlayer, aPlayer + fullside - 1); // left, down
			aPlayer = offset + (side - 1) * fullside;
			clearLinksFrom(aPlayer); // lower-left corner
			if (isInterspecies)
				addLinkAt(aPlayer, aPlayer);
			addLinkAt(aPlayer, aPlayer - fullside + 1); // right, up
			aPlayer = offset + (side - 1) * (fullside + 1);
			clearLinksFrom(aPlayer); // lower-right corner
			if (isInterspecies)
				addLinkAt(aPlayer, aPlayer);
			addLinkAt(aPlayer, aPlayer - fullside - 1); // left, up
			// edges
			for (int i = 1; i < (side - 1); i++) {
				// top
				aPlayer = offset + i;
				clearLinksFrom(aPlayer);
				if (isInterspecies)
					addLinkAt(aPlayer, aPlayer);
				addLinkAt(aPlayer, aPlayer + fullside - 1);
				addLinkAt(aPlayer, aPlayer + fullside + 1);
				// bottom
				aPlayer = offset + (side - 1) * fullside + i;
				clearLinksFrom(aPlayer);
				if (isInterspecies)
					addLinkAt(aPlayer, aPlayer);
				addLinkAt(aPlayer, aPlayer - fullside - 1);
				addLinkAt(aPlayer, aPlayer - fullside + 1);
				// left
				aPlayer = offset + fullside * i;
				clearLinksFrom(aPlayer);
				if (isInterspecies)
					addLinkAt(aPlayer, aPlayer);
				addLinkAt(aPlayer, aPlayer - fullside + 1);
				addLinkAt(aPlayer, aPlayer + fullside + 1);
				// right
				aPlayer = offset + fullside * i + side - 1;
				clearLinksFrom(aPlayer);
				if (isInterspecies)
					addLinkAt(aPlayer, aPlayer);
				addLinkAt(aPlayer, aPlayer - fullside - 1);
				addLinkAt(aPlayer, aPlayer + fullside - 1);
			}
			isRegular = false;
		}
	}

	/**
	 * Utility method to generate a square lattice with Moore neighbourhood.
	 *
	 * <h3>Requirements/notes:</h3>
	 * <ol>
	 * <li>Inter-species interactions include 'self'-connections.
	 * <li>Hierarchical structures may call this method recursively.
	 * <li>Boundaries fixed or periodic (default).
	 * </ol>
	 * 
	 * @param side     the side length of the (sub) lattice
	 * @param fullside the full side length of the entire lattice
	 * @param offset   the offset to the sub-lattice
	 */
	private void initGeometrySquareMoore(int side, int fullside, int offset) {
		int aPlayer;
		boolean isInterspecies = isInterspecies();

		for (int i = 0; i < side; i++) {
			int x = i * fullside;
			int u = ((i - 1 + side) % side) * fullside;
			int d = ((i + 1) % side) * fullside;
			for (int j = 0; j < side; j++) {
				int r = (j + 1) % side;
				int l = (j - 1 + side) % side;
				aPlayer = offset + x + j;
				if (isInterspecies)
					addLinkAt(aPlayer, aPlayer);
				addLinkAt(aPlayer, offset + u + j);
				addLinkAt(aPlayer, offset + u + r);
				addLinkAt(aPlayer, offset + x + r);
				addLinkAt(aPlayer, offset + d + r);
				addLinkAt(aPlayer, offset + d + j);
				addLinkAt(aPlayer, offset + d + l);
				addLinkAt(aPlayer, offset + x + l);
				addLinkAt(aPlayer, offset + u + l);
			}
		}
		if (fixedBoundary) {
			// corners\
			aPlayer = offset;
			clearLinksFrom(aPlayer);
			if (isInterspecies)
				addLinkAt(aPlayer, aPlayer);
			addLinkAt(aPlayer, aPlayer + 1);
			addLinkAt(aPlayer, aPlayer + fullside);
			addLinkAt(aPlayer, aPlayer + fullside + 1);
			aPlayer = offset + side - 1;
			clearLinksFrom(aPlayer);
			if (isInterspecies)
				addLinkAt(aPlayer, aPlayer);
			addLinkAt(aPlayer, aPlayer - 1);
			addLinkAt(aPlayer, aPlayer + fullside);
			addLinkAt(aPlayer, aPlayer + fullside - 1);
			aPlayer = offset + (side - 1) * fullside;
			clearLinksFrom(aPlayer);
			if (isInterspecies)
				addLinkAt(aPlayer, aPlayer);
			addLinkAt(aPlayer, aPlayer + 1);
			addLinkAt(aPlayer, aPlayer - fullside);
			addLinkAt(aPlayer, aPlayer - fullside + 1);
			aPlayer = offset + (side - 1) * (fullside + 1);
			clearLinksFrom(aPlayer);
			if (isInterspecies)
				addLinkAt(aPlayer, aPlayer);
			addLinkAt(aPlayer, aPlayer - 1);
			addLinkAt(aPlayer, aPlayer - fullside);
			addLinkAt(aPlayer, aPlayer - fullside - 1);
			// edges
			for (int i = 1; i < (side - 1); i++) {
				// top
				aPlayer = offset + i;
				clearLinksFrom(aPlayer);
				if (isInterspecies)
					addLinkAt(aPlayer, aPlayer);
				addLinkAt(aPlayer, aPlayer - 1);
				addLinkAt(aPlayer, aPlayer + 1);
				addLinkAt(aPlayer, aPlayer + fullside);
				addLinkAt(aPlayer, aPlayer + fullside - 1);
				addLinkAt(aPlayer, aPlayer + fullside + 1);
				// bottom
				aPlayer = offset + (side - 1) * fullside + i;
				clearLinksFrom(aPlayer);
				if (isInterspecies)
					addLinkAt(aPlayer, aPlayer);
				addLinkAt(aPlayer, aPlayer - 1);
				addLinkAt(aPlayer, aPlayer + 1);
				addLinkAt(aPlayer, aPlayer - fullside);
				addLinkAt(aPlayer, aPlayer - fullside - 1);
				addLinkAt(aPlayer, aPlayer - fullside + 1);
				// left
				aPlayer = offset + fullside * i;
				clearLinksFrom(aPlayer);
				if (isInterspecies)
					addLinkAt(aPlayer, aPlayer);
				addLinkAt(aPlayer, aPlayer + 1);
				addLinkAt(aPlayer, aPlayer - fullside);
				addLinkAt(aPlayer, aPlayer + fullside);
				addLinkAt(aPlayer, aPlayer - fullside + 1);
				addLinkAt(aPlayer, aPlayer + fullside + 1);
				// right
				aPlayer = offset + fullside * i + side - 1;
				clearLinksFrom(aPlayer);
				if (isInterspecies)
					addLinkAt(aPlayer, aPlayer);
				addLinkAt(aPlayer, aPlayer - 1);
				addLinkAt(aPlayer, aPlayer - fullside);
				addLinkAt(aPlayer, aPlayer + fullside);
				addLinkAt(aPlayer, aPlayer - fullside - 1);
				addLinkAt(aPlayer, aPlayer + fullside - 1);
			}
			isRegular = false;
		}
	}

	/**
	 * Utility method to generate a square lattice with arbitrarily large
	 * neighbourhoods.
	 *
	 * <h3>Requirements/notes:</h3>
	 * <ol>
	 * <li>Inter-species interactions include 'self'-connections.
	 * <li>Hierarchical structures may call this method recursively.
	 * <li>Neighbourhood corresponds to a \(3\times 3\), \(5\times 5\), \(7\times
	 * 7\), etc. area.
	 * <li>Boundaries fixed or periodic (default).
	 * </ol>
	 * 
	 * @param side     the side length of the (sub) lattice
	 * @param fullside the full side length of the entire lattice
	 * @param offset   the offset to the sub-lattice
	 */
	private void initGeometrySquare(int side, int fullside, int offset) {
		int aPlayer, bPlayer;
		boolean isInterspecies = isInterspecies();
		int range = Math.min(side / 2, Math.max(1, (int) (Math.sqrt(connectivity + 1.5) / 2.0)));

		for (int i = 0; i < side; i++) {
			int x = i * fullside, y;
			for (int j = 0; j < side; j++) {
				aPlayer = offset + x + j;
				for (int u = i - range; u <= i + range; u++) {
					y = offset + ((u + side) % side) * fullside;
					for (int v = j - range; v <= j + range; v++) {
						bPlayer = y + (v + side) % side;
						// avoid self-interactions
						if (aPlayer == bPlayer && !isInterspecies)
							continue;
						addLinkAt(aPlayer, bPlayer);
					}
				}
			}
		}
		if (fixedBoundary) {
			// corners - top left
			aPlayer = offset;
			clearLinksFrom(aPlayer);
			for (int u = 0; u <= range; u++) {
				int r = aPlayer + u * fullside;
				for (int v = 0; v <= range; v++) {
					bPlayer = r + v;
					// avoid self-interactions
					if (aPlayer == bPlayer && !isInterspecies)
						continue;
					addLinkAt(aPlayer, bPlayer);
				}
			}
			// corners - top right
			aPlayer = offset + side - 1;
			clearLinksFrom(aPlayer);
			for (int u = 0; u <= range; u++) {
				int r = aPlayer + u * fullside;
				for (int v = -range; v <= 0; v++) {
					bPlayer = r + v;
					// avoid self-interactions
					if (aPlayer == bPlayer && !isInterspecies)
						continue;
					addLinkAt(aPlayer, bPlayer);
				}
			}
			// corners - bottom left
			aPlayer = offset + (side - 1) * fullside;
			clearLinksFrom(aPlayer);
			for (int u = -range; u <= 0; u++) {
				int r = aPlayer + u * fullside;
				for (int v = 0; v <= range; v++) {
					bPlayer = r + v;
					// avoid self-interactions
					if (aPlayer == bPlayer && !isInterspecies)
						continue;
					addLinkAt(aPlayer, bPlayer);
				}
			}
			// corners - bottom right
			aPlayer = offset + (side - 1) * (fullside + 1);
			clearLinksFrom(aPlayer);
			for (int u = -range; u <= 0; u++) {
				int r = aPlayer + u * fullside;
				for (int v = -range; v <= 0; v++) {
					bPlayer = r + v;
					// avoid self-interactions
					if (aPlayer == bPlayer && !isInterspecies)
						continue;
					addLinkAt(aPlayer, bPlayer);
				}
			}
			// edges
			for (int i = 1; i < (side - 1); i++) {
				// top
				int row = 0;
				int col = i;
				aPlayer = offset + row * fullside + col;
				clearLinksFrom(aPlayer);
				for (int u = row; u <= row + range; u++) {
					int r = offset + u * fullside;
					for (int v = col - range; v <= col + range; v++) {
						bPlayer = r + (v + side) % side;
						// avoid self-interactions
						if (aPlayer == bPlayer && !isInterspecies)
							continue;
						addLinkAt(aPlayer, bPlayer);
					}
				}
				// bottom
				row = side - 1;
				col = i;
				aPlayer = offset + row * fullside + col;
				clearLinksFrom(aPlayer);
				for (int u = row - range; u <= row; u++) {
					int r = offset + u * fullside;
					for (int v = col - range; v <= col + range; v++) {
						bPlayer = r + (v + side) % side;
						// avoid self-interactions
						if (aPlayer == bPlayer && !isInterspecies)
							continue;
						addLinkAt(aPlayer, bPlayer);
					}
				}
				// left
				row = i;
				col = 0;
				aPlayer = offset + row * fullside + col;
				clearLinksFrom(aPlayer);
				for (int u = row - range; u <= row + range; u++) {
					int r = offset + ((u + side) % side) * fullside;
					for (int v = col; v <= col + range; v++) {
						bPlayer = r + v;
						// avoid self-interactions
						if (aPlayer == bPlayer && !isInterspecies)
							continue;
						addLinkAt(aPlayer, bPlayer);
					}
				}
				// right
				row = i;
				col = side - 1;
				aPlayer = offset + row * fullside + col;
				clearLinksFrom(aPlayer);
				for (int u = row - range; u <= row + range; u++) {
					int r = offset + ((u + side) % side) * fullside;
					for (int v = col - range; v <= col; v++) {
						bPlayer = r + v;
						// avoid self-interactions
						if (aPlayer == bPlayer && !isInterspecies)
							continue;
						addLinkAt(aPlayer, bPlayer);
					}
				}
			}
			isRegular = false;
		}
	}

	/**
	 * Generates a cubic (3D) regular lattice.
	 *
	 * <h3>Requirements/notes:</h3>
	 * <ol>
	 * <li>Integer cube population size, \(N=n^3\).
	 * <li>Admissible connectivities: \(6\) or \(3\times 3\times 3-1=26\), \(5\times
	 * 5\times 5-1\), \(7\times 7\times 7-1\) etc.
	 * <li>For inter-species interactions connectivities are \(+1\) and a
	 * connectivity of \(1\) is admissible.
	 * <li>Boundaries fixed or periodic (default).
	 * <li>For \(N=25'000\) a lattice is generated representing the NOVA
	 * installation in the Zürich main train station (2006-20012). EvoLudo was
	 * launched on the NOVA on September 9, 2009 to commemorate the bicentenary of
	 * Darwin and 150 years since the publication of his <em>On the Origin of
	 * Species</em>.
	 * </ol>
	 * 
	 * @see <a href=
	 *      "https://en.wikipedia.org/wiki/Z%C3%BCrich_Hauptbahnhof#Station_bells,_clock_and_lights">Wikipedia:
	 *      Zürich HB, NOVA</a>
	 * @see <a href=
	 *      "https://de.wikipedia.org/wiki/Z%C3%BCrich_Hauptbahnhof#Innenraum">Wikipedia:
	 *      Zürich HB, NOVA (more extensive, in German)</a>
	 */
	public void initGeometryCube() {
		isRewired = false;
		isUndirected = true;
		isRegular = true;
		boolean isInterspecies = isInterspecies();
		alloc();

		int l = (int) Math.floor(Math.pow(size, 1.0 / 3.0) + 0.5);
		int lz = l;
		if (size == 25000) {
			l = 50;
			lz = 10;
		}
		int l2 = l * l;
		switch ((int) Math.rint(connectivity)) {
			case 1: // self - meaningful only for inter-species interactions
				for (int k = 0; k < lz; k++) {
					int z = k * l2;
					for (int i = 0; i < l; i++) {
						int x = i * l;
						for (int j = 0; j < l; j++) {
							int aPlayer = z + x + j;
							addLinkAt(aPlayer, aPlayer);
						}
					}
				}
				break;

			case 6: // north, east, south, west, top, bottom
				if (fixedBoundary) {
					// fixed boundary
					for (int k = 0; k < lz; k++) {
						int z = k * l2;
						int u = (k + 1 >= lz ? -1 : (k + 1) * l2);
						int d = (k - 1) * l2;
						for (int i = 0; i < l; i++) {
							int x = i * l;
							int n = (i - 1) * l;
							int s = (i + 1 >= l ? -1 : (i + 1) * l);
							for (int j = 0; j < l; j++) {
								int e = (j + 1 >= l ? -1 : j + 1);
								int w = j - 1;
								int aPlayer = z + x + j;
								if (isInterspecies)
									addLinkAt(aPlayer, aPlayer);
								if (n >= 0)
									addLinkAt(aPlayer, z + n + j);
								if (e >= 0)
									addLinkAt(aPlayer, z + x + e);
								if (s >= 0)
									addLinkAt(aPlayer, z + s + j);
								if (w >= 0)
									addLinkAt(aPlayer, z + x + w);
								if (u >= 0)
									addLinkAt(aPlayer, u + x + j);
								if (d >= 0)
									addLinkAt(aPlayer, d + x + j);
							}
						}
					}
					break;
				}
				// periodic boundary
				for (int k = 0; k < lz; k++) {
					int z = k * l2;
					int u = ((k + 1) % lz) * l2;
					int d = ((k - 1 + lz) % lz) * l2;
					for (int i = 0; i < l; i++) {
						int x = i * l;
						int n = ((i - 1 + l) % l) * l;
						int s = ((i + 1) % l) * l;
						for (int j = 0; j < l; j++) {
							int e = (j + 1) % l;
							int w = (j - 1 + l) % l;
							int aPlayer = z + x + j;
							if (isInterspecies)
								addLinkAt(aPlayer, aPlayer);
							addLinkAt(aPlayer, z + n + j);
							addLinkAt(aPlayer, z + x + e);
							addLinkAt(aPlayer, z + s + j);
							addLinkAt(aPlayer, z + x + w);
							addLinkAt(aPlayer, u + x + j);
							addLinkAt(aPlayer, d + x + j);
						}
					}
				}
				break;

			default: // XxXxX neighborhood - validity of range was checked in Population.java
				int range = Math.min(l / 2, Math.max(1, (int) (Math.pow(connectivity + 1.5, 1.0 / 3.0) / 2.0)));
				if (fixedBoundary) {
					for (int k = 0; k < lz; k++) {
						int z = k * l2;
						for (int i = 0; i < l; i++) {
							int y = i * l;
							for (int j = 0; j < l; j++) {
								int aPlayer = z + y + j;

								for (int kr = Math.max(0, k - range); kr <= Math.min(lz - 1, k + range); kr++) {
									int zr = ((kr + lz) % lz) * l2;
									for (int ir = Math.max(0, i - range); ir <= Math.min(l - 1, i + range); ir++) {
										int yr = ((ir + l) % l) * l;
										for (int jr = Math.max(0, j - range); jr <= Math.min(l - 1, j + range); jr++) {
											int bPlayer = zr + yr + ((jr + l) % l);
											// avoid self-interactions
											if (aPlayer == bPlayer && !isInterspecies)
												continue;
											addLinkAt(aPlayer, bPlayer);
										}
									}
								}
							}
						}
					}
					break;
				}
				// periodic boundary
				for (int k = 0; k < lz; k++) {
					int z = k * l2;
					for (int i = 0; i < l; i++) {
						int y = i * l;
						for (int j = 0; j < l; j++) {
							int aPlayer = z + y + j;

							for (int kr = k - range; kr <= k + range; kr++) {
								int zr = ((kr + lz) % lz) * l2;
								for (int ir = i - range; ir <= i + range; ir++) {
									int yr = ((ir + l) % l) * l;
									for (int jr = j - range; jr <= j + range; jr++) {
										int bPlayer = zr + yr + ((jr + l) % l);
										// avoid self-interactions
										if (aPlayer == bPlayer && !isInterspecies)
											continue;
										addLinkAt(aPlayer, bPlayer);
									}
								}
							}
						}
					}
				}
		}
	}

	/**
	 * Generates a hexagonal regular lattice (degree \(6\)). Also called a
	 * triangular lattice depending on whether the focus is on the nodes or on the
	 * tiles.
	 *
	 * <h3>Requirements/notes:</h3>
	 * <ol>
	 * <li>Even integer square population size, i.e. \(N=n^2\) where \(n\) is even.
	 * <li>Boundaries fixed or periodic (default).
	 * </ol>
	 * 
	 * @see <a href="https://en.wikipedia.org/wiki/Triangular_tiling">Wikipedia:
	 *      Triangular tiling</a>
	 * @see #initGeometryTriangular()
	 */
	public void initGeometryHoneycomb() {
		int aPlayer;

		isRewired = false;
		isUndirected = true;
		isRegular = true;
		boolean isInterspecies = isInterspecies();
		alloc();

		int side = (int) Math.floor(Math.sqrt(size) + 0.5);
		switch ((int) Math.rint(connectivity)) {
			case 1:
				for (int i = 0; i < side; i++) {
					int x = i * side;
					for (int j = 0; j < side; j++) {
						aPlayer = x + j;
						addLinkAt(aPlayer, aPlayer);
					}
				}
				break;

			default:
				for (int i = 0; i < side; i += 2) {
					int x = i * side;
					int u = ((i - 1 + side) % side) * side;
					boolean uNowrap = (i > 0);
					int d = ((i + 1) % side) * side; // d cannot wrap because the increment is 2
					int r, l;
					for (int j = 0; j < side; j++) {
						aPlayer = x + j;
						if (isInterspecies)
							addLinkAt(aPlayer, aPlayer);
						if (!fixedBoundary || uNowrap)
							addLinkAt(aPlayer, u + j);
						r = (j + 1) % side;
						if (!fixedBoundary || r > 0)
							addLinkAt(aPlayer, x + r);
						addLinkAt(aPlayer, d + j);
						l = (j - 1 + side) % side;
						if (!fixedBoundary || l < side - 1) {
							addLinkAt(aPlayer, d + l);
							addLinkAt(aPlayer, x + l);
						}
						if (!fixedBoundary || (uNowrap && l < side - 1))
							addLinkAt(aPlayer, u + l);
					}
					x = ((i + 1) % side) * side; // x cannot wrap because the increment is 2
					u = i * side;
					d = ((i + 2) % side) * side;
					boolean dNowrap = (i < side - 2);
					for (int j = 0; j < side; j++) {
						aPlayer = x + j;
						if (isInterspecies)
							addLinkAt(aPlayer, aPlayer);
						addLinkAt(aPlayer, u + j);
						r = (j + 1) % side;
						if (!fixedBoundary || r > 0) {
							addLinkAt(aPlayer, u + r);
							addLinkAt(aPlayer, x + r);
						}
						if (!fixedBoundary || (dNowrap && r > 0))
							addLinkAt(aPlayer, d + r);
						if (!fixedBoundary || dNowrap)
							addLinkAt(aPlayer, d + j);
						l = (j - 1 + side) % side;
						if (!fixedBoundary || l < side - 1)
							addLinkAt(aPlayer, x + l);
					}
				}
		}
	}

	/**
	 * Generates a triangular regular lattice (degree \(3\)). Also called a
	 * hexagonal lattice depending on whether the focus is on the nodes or on the
	 * tiles.
	 *
	 * <h3>Requirements/notes:</h3>
	 * <ol>
	 * <li>Even integer square population size, i.e. \(N=n^2\) where \(n\) is even.
	 * <li>Boundaries fixed or periodic (default).
	 * </ol>
	 * 
	 * @see <a href="https://en.wikipedia.org/wiki/Hexagonal_lattice">Wikipedia:
	 *      Hexagonal lattice</a>
	 * @see #initGeometryHoneycomb()
	 */
	public void initGeometryTriangular() {
		int aPlayer;

		isRewired = false;
		isUndirected = true;
		isRegular = true;
		boolean isInterspecies = isInterspecies();
		alloc();

		int side = (int) Math.floor(Math.sqrt(size) + 0.5);
		switch ((int) Math.rint(connectivity)) {
			case 1:
				for (int i = 0; i < side; i++) {
					int x = i * side;
					for (int j = 0; j < side; j++) {
						aPlayer = x + j;
						addLinkAt(aPlayer, aPlayer);
					}
				}
				break;

			default:
				for (int i = 0; i < side; i += 2) {
					int x = i * side;
					int u = ((i - 1 + side) % side) * side;
					boolean uNowrap = (i > 0);
					int d = ((i + 1) % side) * side; // d cannot wrap because the i increment is 2
					int r, l;
					for (int j = 0; j < side; j += 2) {
						aPlayer = x + j;
						if (isInterspecies)
							addLinkAt(aPlayer, aPlayer);
						r = j + 1; // r cannot wrap because the j increment is 2
						addLinkAt(aPlayer, x + r);
						l = (j - 1 + side) % side;
						if (!fixedBoundary || l < side - 1)
							addLinkAt(aPlayer, x + l);
						addLinkAt(aPlayer, d + j);
						aPlayer = x + j + 1;
						if (isInterspecies)
							addLinkAt(aPlayer, aPlayer);
						r = (r + 1) % side; // now r can wrap and will be zero if it did
						if (!fixedBoundary || r > 0)
							addLinkAt(aPlayer, x + r);
						l = j;
						addLinkAt(aPlayer, x + l);
						if (!fixedBoundary || uNowrap)
							addLinkAt(aPlayer, u + j + 1);
					}
					x = d;
					u = i * side;
					d = ((i + 2) % side) * side;
					boolean dNowrap = (i < side - 2);
					for (int j = 0; j < side; j += 2) {
						aPlayer = x + j;
						if (isInterspecies)
							addLinkAt(aPlayer, aPlayer);
						r = j + 1; // r cannot wrap because the j increment is 2
						addLinkAt(aPlayer, x + r);
						l = (j - 1 + side) % side;
						if (!fixedBoundary || l < side - 1)
							addLinkAt(aPlayer, x + l);
						addLinkAt(aPlayer, u + j);
						aPlayer = x + j + 1;
						if (isInterspecies)
							addLinkAt(aPlayer, aPlayer);
						r = (r + 1) % side; // now r can wrap and will be zero if it did
						if (!fixedBoundary || r > 0)
							addLinkAt(aPlayer, x + r);
						l = j;
						addLinkAt(aPlayer, x + l);
						if (!fixedBoundary || dNowrap)
							addLinkAt(aPlayer, d + j + 1);
					}
				}
		}
	}

	/**
	 * Generates a Frucht graph. The Frucht graph is the smallest regular graph
	 * without any further symmetries. A cubic graph with \(12\) nodes and no
	 * automorphisms apart from the identity map.
	 *
	 * <h3>Requirements/notes:</h3>
	 * <ol>
	 * <li>No automorphisms, \(k=3, N=12\).
	 * <li>The numbering of the nodes follows McAvoy &amp; Hauert (2015).
	 * </ol>
	 * 
	 * @see <a href="https://en.wikipedia.org/wiki/Frucht_graph">Wikipedia: Frucht
	 *      graph</a>
	 * @see <a href="https://en.wikipedia.org/wiki/Graph_automorphism">Wikipedia:
	 *      Graph automorphism</a>
	 * @see "Frucht, R. (1939), Herstellung von Graphen mit vorgegebener abstrakter
	 *      Gruppe. Compositio Mathematica, 6: 239–250"
	 * @see <a href="https://dx.doi.org/10.1098/rsif.2015.0420">McAvoy, A. &amp;
	 *      Hauert, C. (2015) Structural Symmetry in Evolutionary Games J. R. Soc.
	 *      Interface 12 20150420.</a>
	 */
	public void initGeometryFruchtGraph() {
		isRewired = false;
		isUndirected = true;
		isRegular = true;
		alloc();

		// // link nodes - create ring first
		// for( int i=0; i<size; i++ ) {
		// addLinkAt(i, (i-1+size)%size);
		// addLinkAt(i, (i+1)%size);
		// }
		// addLinkAt(0, 7);
		// addLinkAt(1, 11);
		// addLinkAt(2, 10);
		// addLinkAt(3, 5);
		// addLinkAt(4, 9);
		// addLinkAt(5, 3);
		// addLinkAt(6, 8);
		// addLinkAt(7, 0);
		// addLinkAt(8, 6);
		// addLinkAt(9, 4);
		// addLinkAt(10, 2);
		// addLinkAt(11, 1);
		// use same labels as in McAvoy & Hauert 2015 J. R. Soc. Interface
		addEdgeAt(0, 1);
		addEdgeAt(1, 2);
		addEdgeAt(2, 3);
		addEdgeAt(3, 4);
		addEdgeAt(4, 5);
		addEdgeAt(5, 6);
		addEdgeAt(6, 0);
		addEdgeAt(0, 7);
		addEdgeAt(1, 7);
		addEdgeAt(2, 8);
		addEdgeAt(3, 8);
		addEdgeAt(4, 9);
		addEdgeAt(5, 9);
		addEdgeAt(6, 10);
		addEdgeAt(7, 10);
		addEdgeAt(8, 11);
		addEdgeAt(9, 11);
		addEdgeAt(10, 11);
	}

	/**
	 * Generates a Tietze graph. Tietze’s graph is a (regular) cubic graph with
	 * twelve vertices but with a higher degree of symmetry than the Frucht graph.
	 * Tietze’s graph has twelve automorphisms but is not vertex-transitive.
	 *
	 * <h3>Requirements/notes:</h3>
	 * \(12\) automorphisms, \(k=3, N=12\).
	 * 
	 * @see <a href="https://en.wikipedia.org/wiki/Tietze%27s_graph">Wikipedia:
	 *      Tietze's graph</a>
	 * @see <a href="https://en.wikipedia.org/wiki/Graph_automorphism">Wikipedia:
	 *      Graph automorphism</a>
	 * @see "Tietze, Heinrich (1910), Einige Bemerkungen zum Problem des
	 *      Kartenfärbens auf einseitigen Flächen, DMV Annual Report, 19: 155–159"
	 */
	public void initGeometryTietzeGraph() {
		isRewired = false;
		isUndirected = true;
		isRegular = true;
		alloc();

		// link nodes - create line first
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

	/**
	 * Generates a Franklin graph. The Franklin graph is another a cubic graph with
	 * twelve nodes but is also vertex-transitive. Intuitively speaking, vertex
	 * transitivity means that the graph looks the same from the perspective of
	 * every node.
	 *
	 * <h3>Requirements/notes:</h3>
	 * Vertex-transitive graph, \(k=3, N=12\).
	 * 
	 * @see <a href="https://en.wikipedia.org/wiki/Franklin_graph">Wikipedia:
	 *      Franklin graph</a>
	 * @see <a
	 *      href="https://en.wikipedia.org/wiki/Vertex-transitive_graph">Wikipedia:
	 *      Vertex-transitive graph</a>
	 * @see <a href="https://doi.org/10.1002/sapm1934131363">Franklin, P. A Six
	 *      Color Problem. J. Math. Phys. 13, 363-379, 1934.</a>
	 */
	public void initGeometryFranklinGraph() {
		isRewired = false;
		isUndirected = true;
		isRegular = true;
		alloc();

		// link nodes - create ring first
		for (int i = 1; i < size; i++)
			addEdgeAt(i, i - 1);
		addEdgeAt(0, size - 1);

		addEdgeAt(0, 7);
		addEdgeAt(1, 6);
		addEdgeAt(2, 9);
		addEdgeAt(3, 8);
		addEdgeAt(4, 11);
		addEdgeAt(5, 10);
	}

	/**
	 * Generates a Heawood graph. No cubic symmetric graph with \(12\) vertices
	 * exists. The closest comparable graph is the Heawood graph, which cubic and
	 * symmetric but with \(14\) nodes.
	 * <p>
	 * Symmetric graphs satisfy the stronger structural symmetry requirements of
	 * arc-transitivity. intuitively speaking, arc-transitivity means that not only
	 * does the graph look the same from the perspective of every node but also
	 * that if a pair of neighbouring individuals is randomly relocated to some
	 * other neighbouring pair of nodes, the two individuals are not able to tell
	 * whether and where they have been moved even when both are aware of the
	 * overall graph structure and share their conclusions.
	 *
	 * <h3>Requirements/notes:</h3>
	 * Symmetric graph, \(k=3, N=14\).
	 * 
	 * @see <a href="https://en.wikipedia.org/wiki/Heawood_graph">Wikipedia: Heawood
	 *      graph</a>
	 * @see <a href="https://en.wikipedia.org/wiki/Symmetric_graph">Wikipedia:
	 *      Symmetric graph (arc-transitive)</a>
	 * @see "Heawood, P. J. (1890) Map colouring theorems. Quarterly J. Math. Oxford
	 *      Ser. 24: 322–339."
	 */
	public void initGeometryHeawoodGraph() {
		isRewired = false;
		isUndirected = true;
		isRegular = true;
		alloc();

		// link nodes - create ring first
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

	/**
	 * Generates an icosahedron graph. A symmetric graph with \(12\) nodes and
	 * degree \(5\).
	 * 
	 * <h3>Requirements/notes:</h3>
	 * Symmetric graph, \(k=5, N=12\).
	 * 
	 * @see <a href="https://en.wikipedia.org/wiki/Icosahedron">Wikipedia:
	 *      Icosahedron</a>
	 */
	public void initGeometryIcosahedronGraph() {
		isRewired = false;
		isUndirected = true;
		isRegular = true;
		alloc();

		// link nodes - create line first
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

	/**
	 * Generates a dodecahedron graph. A cubic, symmetric graph with \(20\) nodes
	 * (same as Desargue's graph). The two graphs have the same diameter, mean
	 * shortest path length, and clustering coefficient. Nevertheless the highly
	 * susceptible fixation times differ for the two graphs.
	 * 
	 * <h3>Requirements/notes:</h3>
	 * Symmetric graph, \(k=3, N=20\).
	 * 
	 * @see <a href="https://en.wikipedia.org/wiki/Regular_dodecahedron">Wikipedia:
	 *      Regular dodecahedron</a>
	 * @see #initGeometryDesarguesGraph()
	 */
	public void initGeometryDodekahedronGraph() {
		isRewired = false;
		isUndirected = true;
		isRegular = true;
		alloc();

		// link nodes:
		// - create ring over even numbered vertices
		// - link even to previous odd numbered vertices
		for (int i = 0; i < size; i += 2) {
			addEdgeAt(i, (size + i - 2) % size);
			addEdgeAt(i, i + 1);
		}

		addEdgeAt(1, 5);
		addEdgeAt(3, 7);
		addEdgeAt(5, 9);
		addEdgeAt(7, 11);
		addEdgeAt(9, 13);
		addEdgeAt(11, 15);
		addEdgeAt(13, 17);
		addEdgeAt(15, 19);
		addEdgeAt(17, 1);
		addEdgeAt(19, 3);
	}

	/**
	 * Generates a Desargues graph, named after Girard Desargues (1591–1661). A
	 * cubic, symmetric graph with \(20\) nodes (same as icosahedron graph). The two
	 * graphs have the same diameter, mean shortest path length, and clustering
	 * coefficient. Nevertheless the highly susceptible fixation times differ for
	 * the two graphs.
	 *
	 * <h3>Requirements/notes:</h3>
	 * Symmetric graph, \(k=3, N=20\).
	 * 
	 * @see <a href="https://en.wikipedia.org/wiki/Desargues_graph">Wikipedia:
	 *      Desargues graph</a>
	 * @see <a href="http://dx.doi.org/10.2307/2371806">Kagno, I. N. (1947),
	 *      "Desargues' and Pappus' graphs and their groups", American Journal of
	 *      Mathematics, 69 (4): 859–863</a>
	 * @see #initGeometryDodekahedronGraph()
	 */
	public void initGeometryDesarguesGraph() {
		isRewired = false;
		isUndirected = true;
		isRegular = true;
		alloc();

		// link nodes - create ring first
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

	/**
	 * Generate a (connected, directed) random graph.
	 *
	 * <h3>Requirements/notes:</h3>
	 * <ol>
	 * <li>Rounds link count to even number.
	 * <li>Ensure minimal assumptions, i.e. fully general graph.
	 * </ol>
	 */
	public void initGeometryRandomGraph() {
		int parent, parentIdx, child, childIdx;
		int todo, done;

		// retrieve the shared RNG to ensure reproducibility of results
		RNGDistribution rng = engine.getRNG();

		isRewired = false;
		isUndirected = true;
		isRegular = false;
		alloc();

		// ensure connectedness
		int nLinks = (int) Math.floor(connectivity * size + 0.5);
		nLinks = (nLinks - nLinks % 2) / 2; // nLinks must be even
		int[] isolated = new int[size];
		int[] connected = new int[size];
		for (int i = 0; i < size; i++)
			isolated[i] = i;
		todo = size;
		done = 0;
		parent = rng.random0n(todo);
		parentIdx = isolated[parent];
		todo--;
		if (parent != todo)
			System.arraycopy(isolated, parent + 1, isolated, parent, todo - parent);
		connected[done] = parentIdx;
		done++;
		child = rng.random0n(todo);
		childIdx = isolated[child];
		todo--;
		System.arraycopy(isolated, child + 1, isolated, child, todo - child);
		connected[done] = childIdx;
		done++;
		addLinkAt(parentIdx, childIdx);
		addLinkAt(childIdx, parentIdx);
		nLinks--;
		while (todo > 0) {
			parent = rng.random0n(done);
			parentIdx = connected[parent];
			child = rng.random0n(todo);
			childIdx = isolated[child];
			todo--;
			System.arraycopy(isolated, child + 1, isolated, child, todo - child);
			connected[done] = childIdx;
			done++;
			addLinkAt(parentIdx, childIdx);
			addLinkAt(childIdx, parentIdx);
			nLinks--;
		}

		// now we have a connected graph
		randomlinking: while (nLinks > 0) {
			parentIdx = rng.random0n(size);
			childIdx = rng.random0n(size - 1);
			if (childIdx >= parentIdx)
				childIdx++;
			int[] pNeigh = out[parentIdx];
			int len = kout[parentIdx];
			for (int i = 0; i < len; i++) {
				if (pNeigh[i] == childIdx)
					continue randomlinking;
			}
			addLinkAt(parentIdx, childIdx);
			addLinkAt(childIdx, parentIdx);
			nLinks--;
		}
	}

	/**
	 * Generate a (connected, directed) random graph.
	 *
	 * <h3>Requirements/notes:</h3>
	 * <ol>
	 * <li>check if truly connected
	 * <li>first node may not necessarily get an incoming link
	 * <li>ensure minimal assumptions, i.e. fully general graph
	 * </ol>
	 */
	public void initGeometryRandomGraphDirected() {
		int parent, parentIdx, child, childIdx;
		int todo, done;

		// retrieve the shared RNG to ensure reproducibility of results
		RNGDistribution rng = engine.getRNG();

		isRewired = false;
		isUndirected = false;
		isRegular = false;
		alloc();

		// ensure connectedness
		int nLinks = (int) Math.floor(connectivity * size + 0.5);
		int[] isolated = new int[size];
		int[] connected = new int[size];
		for (int i = 0; i < size; i++)
			isolated[i] = i;
		todo = size;
		done = 0;
		parent = rng.random0n(todo);
		parentIdx = isolated[parent];
		todo--;
		if (parent != todo)
			System.arraycopy(isolated, parent + 1, isolated, parent, todo - parent);
		connected[done] = parentIdx;
		done++;
		child = rng.random0n(todo);
		childIdx = isolated[child];
		todo--;
		System.arraycopy(isolated, child + 1, isolated, child, todo - child);
		connected[done] = childIdx;
		done++;
		addLinkAt(parentIdx, childIdx);
		nLinks--;
		while (todo > 0) {
			parent = rng.random0n(done);
			parentIdx = connected[parent];
			child = rng.random0n(todo);
			childIdx = isolated[child];
			todo--;
			System.arraycopy(isolated, child + 1, isolated, child, todo - child);
			connected[done] = childIdx;
			done++;
			addLinkAt(parentIdx, childIdx);
			nLinks--;
		}

		// now we have a connected graph
		// however it is generally not possible to connect every vertex pair
		// the network has most likely numerous sources and sinks, i.e. each vertex has
		// at least one outgoing OR one incoming link.
		randomlinking: while (nLinks > 0) {
			parentIdx = rng.random0n(size);
			childIdx = rng.random0n(size - 1);
			if (childIdx >= parentIdx)
				childIdx++;
			int[] pNeigh = out[parentIdx];
			int len = kout[parentIdx];
			for (int i = 0; i < len; i++) {
				if (pNeigh[i] == childIdx)
					continue randomlinking;
			}
			addLinkAt(parentIdx, childIdx);
			nLinks--;
		}
	}

	/**
	 * Maximum number of attempts to generate a graph structure.
	 */
	protected static final int MAX_TRIALS = 10;

	/**
	 * Generate a (connected, undirected) random regular graph.
	 *
	 * <h3>Requirements/notes:</h3>
	 * <ol>
	 * <li>Graph generation may fail
	 * <li>After MAX_TRIALS failures, resort to well-mixed population
	 * <li>Minimize risk of failure?
	 * </ol>
	 */
	public void initGeometryRandomRegularGraph() {
		isRegular = true;
		int[] degrees = new int[size];
		Arrays.fill(degrees, (int) connectivity);
		int trials = 0;
		while (!initGeometryDegreeDistr(degrees) && ++trials < MAX_TRIALS)
			;
		if (trials >= 10) {
			// reset sets size=-1
			int mysize = size;
			reset();
			size = mysize;
			check();
			init();
			logger.severe("initGeometryRandomRegularGraph failed - giving up (revert to " + geometry.title + ").");
			return;
		}
	}

	/**
	 * Generate a (connected, undirected) scale-free network. Generates power-law
	 * degree distribution first and then a random network that satisfies those
	 * connectivities.
	 *
	 * <h3>Requirements/notes:</h3>
	 * Makes efforts to match the desired overall connectivity.
	 */
	public void initGeometryScaleFree() {
		double[] distr = new double[size]; // connectivities up to N-1

		// retrieve the shared RNG to ensure reproducibility of results
		RNGDistribution rng = engine.getRNG();

		// generate power law distribution
		if (Math.abs(sfExponent) > 1e-8)
			for (int n = 0; n < size; n++)
				distr[n] = Math.pow((double) n / (double) size, sfExponent);
		else {
			// uniform distribution
			int max = (int) (2.0 * connectivity + 0.5);
			for (int n = 0; n <= max; n++)
				distr[n] = 1.0;
			for (int n = max + 1; n < size; n++)
				distr[n] = 0.0;
		}
		// calculate norm and average connectivity
		double norm = 0.0, conn = 0.0;
		for (int n = 1; n < size; n++) {
			norm += distr[n];
			conn += n * distr[n];
		}
		conn /= norm;
		// normalize distribution - makes life easier
		for (int n = 0; n < size; n++)
			distr[n] /= norm;

		// adjust distribution to match desired connectivity
		if (conn < connectivity) {
			// increase number of links
			double max = size / 2.0;
			// lift distribution
			double x = 1.0 - (connectivity - conn) / (max - conn);
			double lift = (1.0 - x) / (size - 1);
			for (int n = 1; n < size; n++)
				distr[n] = x * distr[n] + lift;
			/*
			 * { double checknorm=0.0, checkconn=0.0; for( int n=1; n<size; n++ ) {
			 * checknorm += distr[n]; checkconn += (double)n*distr[n]; } if(
			 * Math.abs(1.0-checknorm)>1e-10 )
			 * System.out.println("norm violated!!! - norm="+checknorm+" should be "+1+"!");
			 * if( Math.abs(connectivity-checkconn)>1e-8 )
			 * System.out.println("connectivity violated!!! - conn="+checkconn+" should be "
			 * +connectivity+"!"); }
			 */
			// System.out.println("distribution:");
			// for( int n=0; n<size; n++ ) System.out.println(n+": "+distr[n]);
		} else {
			// decrease number of links - requires cutoff/maximum degree
			double km = 0.0, pm = 0.0;
			int m = 1;
			double sump = distr[1], sumpi = sump;
			while (km < connectivity && m < size - 1) {
				m++;
				pm = distr[m];
				sump += pm;
				sumpi += pm * m;
				km = (sumpi - pm * ((m * (m + 1)) / 2)) / (sump - m * pm);
			}
			for (int n = m; n < size; n++)
				distr[n] = 0.0;
			// System.out.println("cutoff:"+m+" -> km="+km);
			double decr = distr[m - 1];
			double newnorm = sump - pm - (m - 1) * decr;
			conn = 0.0;
			for (int n = 1; n < m; n++) {
				distr[n] = (distr[n] - decr) / newnorm;
				conn += distr[n] * n;
			}
			double x = 1.0 - (connectivity - conn) / (m / 2.0 - conn);
			double lift = (1.0 - x) / (m - 1);
			for (int n = 1; n < m; n++)
				distr[n] = x * distr[n] + lift;
			/*
			 * { double checknorm=0.0, checkconn=0.0; for( int n=1; n<size; n++ ) {
			 * checknorm += distr[n]; checkconn += (double)n*distr[n]; } if(
			 * Math.abs(1.0-checknorm)>1e-10 )
			 * System.out.println("norm violated!!! - norm="+checknorm+" should be "+1+"!");
			 * if( Math.abs(connectivity-checkconn)>1e-8 )
			 * System.out.println("connectivity violated!!! - conn="+checkconn+" should be "
			 * +connectivity+"!"); }
			 */
			// System.out.println("distribution:");
			// for( int n=0; n<size; n++ ) System.out.println(n+": "+distr[n]);
		}

		// allocate degree distribution
		int[] degrees = new int[size];
		int links;

		do {
			// choose degrees
			links = 0;
			int leaflinks = 0;
			int nonleaflinks = 0;
			for (int n = 0; n < size; n++) {
				double hit = rng.random01();
				for (int i = 1; i < size - 1; i++) {
					hit -= distr[i];
					if (hit <= 0.0) {
						degrees[n] = i;
						links += i;
						if (i > 1)
							nonleaflinks += i;
						else
							leaflinks++;
						break;
					}
				}
			}
			// check connectivity
			int adj = 0;
			if (connectivity > 0.0) {
				if (connectivity * size > links)
					adj = (int) Math.floor(connectivity * size + 0.5) - links;
				if (Math.max(2.0, connectivity) * size < links)
					adj = (int) Math.floor(Math.max(2.0, connectivity) * size + 0.5) - links;
				if ((links + adj) % 2 == 1)
					adj++; // ensure even number of docks
			}
			logger.warning("adjusting link count: " + links + " by " + adj + " to achieve "
					+ (int) Math.floor(connectivity * size + 0.5));

			// ensure right number of links
			// while( adj!=0 || (nonleaflinks < 2*(size-1)-leaflinks) || (links % 2 == 1) )
			// {
			while (adj != 0) {
				int node = rng.random0n(size);
				// draw new degree for random node
				int odegree = degrees[node], ndegree = -1;
				double hit = rng.random01();
				for (int i = 1; i < size - 1; i++) {
					hit -= distr[i];
					if (hit <= 0.0) {
						ndegree = i;
						break;
					}
				}
				int dd = ndegree - odegree;
				if (Math.abs(adj) <= Math.abs(adj - dd))
					continue;
				degrees[node] = ndegree;
				if (odegree == 1 && ndegree != 1) {
					leaflinks--;
					nonleaflinks += ndegree;
				}
				if (odegree != 1 && ndegree == 1) {
					leaflinks -= odegree;
					nonleaflinks--;
				}
				links += dd;
				adj -= dd;
				logger.warning(
						"links: " + links + ", goal: " + (int) Math.floor(Math.max(2.0, connectivity) * size + 0.5)
								+ ", change: " + (ndegree - odegree) + ", remaining: " + adj);
			}
			// do some basic checks on feasibility of distribution
			// 1) avoid uneven number of links
			// 2) number of non-leaf links must be above a certain threshold
			while ((nonleaflinks < 2 * (size - 1) - leaflinks) || (links % 2 == 1)) {
				int node = rng.random0n(size);
				// add link to random node
				if (degrees[node]++ == 1) {
					leaflinks--;
					nonleaflinks++;
				}
				nonleaflinks++;
				links++;
			}

			// sort degrees
			Arrays.sort(degrees); // sorts ascending
			for (int n = 0; n < size / 2; n++)
				swap(degrees, n, size - n - 1);

			/*
			 * System.out.println("distribution:"); for( int n=0; n<size-1; n++ )
			 * System.out.println(n+": "+distr[n]); System.out.println("degrees:"); for( int
			 * n=0; n<size; n++ ) System.out.println(n+": "+degrees[n]);
			 */
			for (int n = 0; n < 10; n++)
				if (initGeometryDegreeDistr(degrees))
					return;
		}
		/// while( !initGeometryDegreeDistr(geom, rng, degrees) );
		while (true);
		// printConnections(geom);
	}

	/**
	 * Swap elements with indices {@code a} and {@code b} in array {@code x}
	 * 
	 * @param x[] the data array
	 * @param a   the index of the first element
	 * @param b   the index of the second element
	 */
	private static void swap(int x[], int a, int b) {
		int t = x[a];
		x[a] = x[b];
		x[b] = t;
	}

	/**
	 * Utility method to generate network with a given degree distribution.
	 *
	 * <h3>Requirements/notes:</h3>
	 * The degree distribution is sorted with descending degrees.
	 * 
	 * @param distr the array to store the degree distribution
	 * @return {@code true} if generation succeeded
	 */
	private boolean initGeometryDegreeDistr(int[] distr) {

		// retrieve the shared RNG to ensure reproducibility of results
		RNGDistribution rng = engine.getRNG();

		// initialize
		isRewired = false;
		isUndirected = true;
		alloc();

		int[] core = new int[size];
		int[] full = new int[size];
		// DEBUG - triggers exceptions to capture some bookkeeping issues
		// Arrays.fill(core, -1);
		// Arrays.fill(full, -1);
		int[] degree = new int[size];
		System.arraycopy(distr, 0, degree, 0, size);
		int todo = size;
		for (int n = 0; n < todo; n++)
			core[n] = n;

		if (!isDynamic) {
			// ensure connectedness for static graphs; exclude leaves for this stage
			// recall degree's are sorted in descending order
			int leafIdx = -1;
			for (int i = size - 1; i >= 0; i--) {
				if (degree[i] <= 1)
					continue;
				leafIdx = i + 1;
				break;
			}
			todo = leafIdx;
			int done = 0;
			int[] active = new int[size];
			int idxa = rng.random0n(todo);
			active[0] = core[idxa];
			core[idxa] = core[--todo];
			// DEBUG
			// core[todo] = -1;
			int nActive = 1;
			while (todo > 0) {
				idxa = rng.random0n(nActive);
				int nodea = active[idxa];
				int idxb = rng.random0n(todo);
				int nodeb = core[idxb];
				addEdgeAt(nodea, nodeb);
				// if A reached degree add to full and remove from active
				if (kout[nodea] == degree[nodea]) {
					full[done++] = nodea;
					active[idxa] = active[--nActive];
					// DEBUG - triggers exceptions to capture some bookkeeping issues
					// active[nActive] = -1;
				}
				// if B reached degree add to full otherwise add to active
				if (kout[nodeb] == degree[nodeb])
					full[done++] = nodeb;
				else
					active[nActive++] = nodeb;
				// remove nodeb from core of unconnected nodes
				core[idxb] = core[--todo];
				// DEBUG - triggers exceptions to capture some bookkeeping issues
				// core[todo] = -1;
			}
			// now we have a connected core graph; add leaves to active nodes
			if (leafIdx < size)
				// all leaves are in tail of core
				System.arraycopy(core, leafIdx, active, nActive, size - leafIdx);
			core = active;
			todo = nActive;
		}
		// core: list of todo indices of nodes that require further connections
		// full: list of done indices of nodes with requested connectivity
		int escape = 0;
		while (todo > 1) {
			int idxa = rng.random0n(todo);
			int nodea = core[idxa];
			int idxb = rng.random0n(todo - 1);
			if (idxb >= idxa)
				idxb++;
			int nodeb = core[idxb];
			boolean success = true;
			if (isNeighborOf(nodea, nodeb)) {
				// make sure there is at least one node in connected set
				if (todo == size)
					continue;
				// do not yet give up - pick third node at random from connected set plus one of
				// its neighbours
				int idxc = rng.random0n(size - todo);
				int nodec = full[idxc];
				// note: D may or may not be member of full; must not be A or B
				int noded = out[nodec][rng.random0n(kout[nodec])];
				// A-B as well as C-D are connected
				if (noded != nodeb && !isNeighborOf(nodea, nodec) && !isNeighborOf(nodeb, noded)) {
					// note: D=A cannot hold because then isNeighborOf(nodea, nodec)==true
					// break C-D edge, connect A-C and B-D
					// leaves connectivity of C and D unchanged
					removeEdgeAt(nodec, noded);
					addEdgeAt(nodea, nodec);
					addEdgeAt(nodeb, noded);
				} else if (noded != nodea && !isNeighborOf(nodea, noded) && !isNeighborOf(nodeb, nodec)) {
					// note: D=B cannot hold because then isNeighborOf(nodeb, nodec)==true
					// break C-D edge, connect B-C and A-D
					// leaves connectivity of C and D unchanged
					removeEdgeAt(nodec, noded);
					addEdgeAt(nodea, noded);
					addEdgeAt(nodeb, nodec);
				} else
					success = false;
			} else {
				addEdgeAt(nodea, nodeb);
			}
			if (!success) {
				if (++escape > 10) {
					logger.info("initGeometryDegreeDistr appears stuck - retry");
					return false;
				}
				continue;
			}
			escape = 0;
			if (kout[nodea] == degree[nodea]) {
				full[size - todo] = nodea;
				core[idxa] = core[--todo];
				// DEBUG - triggers exceptions to capture some bookkeeping issues
				// core[todo] = -1;
				if (idxb == todo)
					idxb = idxa;
			}
			if (kout[nodeb] == degree[nodeb]) {
				full[size - todo] = nodeb;
				core[idxb] = core[--todo];
				// DEBUG - triggers exceptions to capture some bookkeeping issues
				// core[todo] = -1;
			}
		}
		while (todo == 1) {
			// let's try to fix this
			int nodea = core[0];
			int idxc = rng.random0n(size - 1);
			int nodec = full[idxc];
			int noded = out[nodec][rng.random0n(kout[nodec])];
			// A is single, C-D are connected
			if (noded != nodea && !isNeighborOf(nodea, nodec) && !isNeighborOf(nodea, noded)) {
				// break C-D edge, connect A-C and A-D
				// leaves connectivity of C and D unchanged
				removeEdgeAt(nodec, noded);
				addEdgeAt(nodea, nodec);
				addEdgeAt(nodea, noded);
				return true;
			}
			if (++escape > 10) {
				logger.info("initGeometryDegreeDistr appears stuck - retry");
				return false;
			}
		}
		// check structure
		// evaluateGeometry();
		// checkConnections();
		// checkConnections(degree);
		// end check
		return true;
	}

	/**
	 * Generate a (connected, undirected) scale-free network following the procedure
	 * by Barabási &amp; Albert (1999).
	 *
	 * <h3>Requirements/notes:</h3>
	 * None.
	 * 
	 * @see <a href="http://dx.doi.org/10.1126/science.286.5439.509">Barabási,
	 *      Albert-László &amp; Albert, Réka (1999) "Emergence of scaling in
	 *      random networks", Science. 286 (5439): 509–512</a>
	 */
	public void initGeometryScaleFreeBA() {
		int nStart, nLinks;

		// retrieve the shared RNG to ensure reproducibility of results
		RNGDistribution rng = engine.getRNG();

		isRewired = false;
		isUndirected = true;
		isRegular = false;
		alloc();

		// create fully connected core graph
		int myLinks = Math.min((int) (connectivity / 2.0 + 0.5), size - 1);
		nStart = Math.max(myLinks, 2);
		for (int i = 1; i < nStart; i++) {
			for (int j = 0; j < i; j++) {
				addLinkAt(i, j);
				addLinkAt(j, i);
			}
		}
		nLinks = nStart * (nStart - 1);

		for (int n = nStart; n < size; n++) {
			for (int i = 0; i < myLinks; i++) {
				// count links of my neighbors
				int[] myNeigh = out[n];
				int nl = 0;
				for (int j = 0; j < i; j++)
					nl += kout[myNeigh[j]];

				int ndice = rng.random0n(nLinks - nl);
				int randnode = -1;
				for (int j = 0; j < n; j++) {
					if (isNeighborOf(n, j))
						continue;
					ndice -= kout[j];
					if (ndice < 0) {
						randnode = j;
						break;
					}
				}
				addLinkAt(n, randnode);
				addLinkAt(randnode, n);
				nLinks++; // add only new link of randnode
			}
			nLinks += myLinks; // only now add links of new node
		}
	}

	/**
	 * Generate a (connected, undirected) scale-free network following the procedure
	 * by Klemm &amp; Eguíluz (2001).
	 *
	 * <h3>Requirements/notes:</h3>
	 * <ol>
	 * <li>Effective connectivity is \(k (N-1)/N\) where \(k\) is the desired
	 * average connectivity and \(N\) is the population size.
	 * <li>Add bookkeeping to optimize generation time.
	 * </ol>
	 * 
	 * @see <a href="http://dx.doi.org/10.1103/PhysRevE.65.057102">Klemm, Konstantin
	 *      &amp; Eguíluz, Víctor M. (2001) Growing scale-free networks
	 *      with small-world behavior, Phys. Rev. E 65, 057102.</a>
	 */
	public void initGeometryScaleFreeKlemm() {
		int nStart;

		// retrieve the shared RNG to ensure reproducibility of results
		RNGDistribution rng = engine.getRNG();

		isRewired = false;
		isUndirected = true;
		isRegular = false;
		alloc();

		// create fully connected graph to start with
		int nActive = Math.min((int) (connectivity / 2.0 + 0.5), size);
		int[] active = new int[nActive];
		for (int i = 0; i < nActive; i++)
			active[i] = i;

		nStart = Math.max(nActive, 2);
		for (int i = 1; i < nStart; i++) {
			for (int j = 0; j < i; j++) {
				addLinkAt(i, j);
				addLinkAt(j, i);
			}
		}

		nextnode: for (int n = nStart; n < size; n++) {

			if (pKlemm < 1e-8) {
				// connect to active node
				for (int i = 0; i < nActive; i++) {
					addLinkAt(n, active[i]);
					addLinkAt(active[i], n);
				}
			} else {
				for (int i = 0; i < nActive; i++) {
					if (pKlemm > (1.0 - 1e-8) || rng.random01() < pKlemm) {
						// linear preferential attachment - count links
						int randnode;
						int links = 0;
						// NOTE: some clever bookkeeping should spare us this loop!!!
						for (int j = 0; j < n; j++)
							links += kout[j];
						// avoid double connections
						do {
							int ndice = rng.random0n(links);
							randnode = -1;
							for (int j = 0; j < n; j++) {
								ndice -= kout[j];
								if (ndice < 0) {
									randnode = j;
									break;
								}
							}
						}
						// test produces an error if randnode is still -1
						// test relies on bi-directionality of graph
						// new node n may not have a neighbor yet - treat carefully!
						while (isNeighborOf(n, randnode));
						addLinkAt(n, randnode);
						addLinkAt(randnode, n);
					} else { // connect to active node
						addLinkAt(n, active[i]);
						addLinkAt(active[i], n);
					}
				}
			}
			// norm must be calculated separately because the random linking stuff
			// could change the number of links of the active nodes
			// NOTE: some clever bookkeeping should spare us this loop!!!
			double norm = 0.0;
			for (int i = 0; i < nActive; i++)
				norm += 1.0 / kout[active[i]];
			// treat new node as active
			double hitNew = 1.0 / kout[n];
			// deactivate one node and replace by new node
			double dice = rng.random01() * (norm + hitNew) - hitNew;
			// new node removed?
			if (dice < 0.0)
				continue nextnode; // this leaves active node list unchanged
			for (int i = 0; i < nActive; i++) {
				dice -= 1.0 / kout[active[i]];
				if (dice < 0.0) {
					active[i] = n;
					continue nextnode;
				}
			}
			// we should not arrive here - scream!
			throw new Error("Emergency in scale-free network creation...");
		}
		rewireUndirected(pKlemm);
	}

	/**
	 * Add/rewire directed and undirected random links.
	 *
	 * <h3>Requirements/notes:</h3>
	 * None.
	 * 
	 * @see #rewireUndirected()
	 * @see #rewireDirected()
	 */
	public void rewire() {

		if (isUndirected) {
			isRewired = rewireUndirected(pRewire);
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
	 * links in the graph. Thus, at most an expected fraction of \(1-1/e\) (or
	 * \(~63%\)) of original links get rewired.
	 * </ol>
	 * 
	 * @param prob the probability of rewiring an undirected link
	 * @return {@code true} if geometry rewired
	 */
	public boolean rewireUndirected(double prob) {
		// is rewiring possible?
		if (!isUndirected || prob <= 0.0)
			return false;

		// retrieve the shared RNG to ensure reproducibility of results
		RNGDistribution rng = engine.getRNG();

		// rewire at most the total number of links present in the system (corresponds
		// to a fraction of 1-1/e (~63%) of links rewired)
		int nLinks = (int) Math.floor(ArrayMath.norm(kout) / 2 * Math.min(1.0, -Math.log(1.0 - prob)) + 0.5);
		long done = 0;
		int first, firstneigh, second, secondneigh, len;
		while (done < nLinks) {
			// draw first node - avoid sources (nodes without inlinks), leaves and fully
			// connected nodes
			do {
				first = rng.random0n(size);
				len = kin[first];
			} while (len <= 1 || len == size - 1);
			// choose random neighbor
			firstneigh = in[first][rng.random0n(len)];

			// draw second node - avoid source, leaves and fully connected nodes
			// in addition, firstneigh and second must not be neighbors
			do {
				second = rng.random0n(size - 1);
				if (second >= first)
					second++;
				len = kin[second];
			} while (len <= 1 || len == size - 1);
			// choose random neighbor
			secondneigh = in[second][rng.random0n(len)];

			if (!swapEdges(first, firstneigh, second, secondneigh))
				continue;
			if (!isGraphConnected()) {
				swapEdges(first, firstneigh, second, secondneigh);
				swapEdges(first, secondneigh, second, firstneigh);
			}
			done += 2;
		}
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

		// long nLinks =
		// (long)Math.floor(-linkCount(geom)/2.0*Math.log(1.0-geom.pUndirLinks)+0.5);
		// long nLinks =
		// (long)Math.floor(-(int)(geom.avgOut*size+0.5)/2.0*Math.log(1.0-geom.pUndirLinks)+0.5);
		// add at most the number of links already present in the system
		int nLinks = (int) Math.floor(avgOut * size * pAddwire / 2.0 + 0.5);
		int from, to;
		while (nLinks > 0) {
			from = rng.random0n(size);
			to = rng.random0n(size - 1);
			if (to >= from)
				to++; // avoid self-connections
			if (isNeighborOf(from, to))
				continue; // avoid double connections
			addEdgeAt(from, to);
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

		// make sure the right fraction of original links is replaced!
		// long nLinks = (long)Math.floor(-linkCount()*Math.log(1.0-pDirLinks)+0.5);
		// it should not matter whether we use avgOut or avgIn - check!
		// long nLinks =
		// (long)Math.floor(-(int)(avgOut*size+0.5)*Math.log(1.0-pDirLinks)+0.5);
		// rewire at most the number of directed links present in the system
		// (corresponds to a fraction of 1-1/e (~63%) of links rewired)
		int nLinks = (int) Math.floor((int) (avgOut * size + 0.5) * Math.min(1.0, -Math.log(1.0 - pRewire)) + 0.5);
		int done = 0;
		int last = -1, prev, from, to = -1, len, neigh;
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
						throw new Error("Rewiring troubles - giving up...");
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

		// long nLinks =
		// (long)Math.floor(-linkCount(geom)*Math.log(1.0-geom.pDirLinks)+0.5);
		// long nLinks =
		// (long)Math.floor(-(int)(geom.avgOut*size+0.5)*Math.log(1.0-geom.pDirLinks)+0.5);
		// add at most the number of directed links already present in the system
		int nLinks = (int) Math.floor(avgOut * size * pAddwire + 0.5);
		int from, to;
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
	 * {@code true} if geometry has been evaluated.
	 * 
	 * @see #evaluate()
	 */
	private boolean evaluated = false;

	/**
	 * Evaluate geometry. Convenience method to set frequently used quantities such
	 * as {@code minIn, maxOut, avgTot} etc.
	 *
	 * <h3>Requirements/notes:</h3>
	 * <ol>
	 * <li>For {@link Type#MEANFIELD} geometries all quantities are set to zero.
	 * <li>Dynamic graphs are always evaluated because all quantities are ephemeral.
	 * </ol>
	 */
	public void evaluate() {
		if (evaluated && !isDynamic)
			return;

		// determine minimum, maximum and average connectivities
		if (geometry == Type.MEANFIELD || kout == null || kin == null) {
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

		// NOTE: could be improved for undirected geometries - worth the effort and
		// additional maintenance? probably not...
		for (int n = 0; n < size; n++) {
			int lout = kout[n];
			maxOut = Math.max(maxOut, lout);
			minOut = Math.min(minOut, lout);
			sumout += lout;
			int lin = kin[n];
			// use ChHMath.max(geom.maxIn)?
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
	 * @return <code>true</code> if <code>geometry</code> is lattice,
	 *         <code>false</code> otherwise
	 */
	public boolean isLattice() {
		if (isRewired)
			return false;
		return geometry.isLattice();
	}

	/**
	 * Check if graph is connected.
	 *
	 * <h3>Requirements/notes:</h3>
	 * Requires undirected graphs.
	 * 
	 * @return {@code true} if graph is connected
	 */
	public boolean isGraphConnected() {
		boolean[] check = new boolean[size];
		Arrays.fill(check, false);
		isGraphConnected(0, check);
		return ArrayMath.min(check);
	}

	/**
	 * Check if any other node can be reached from <code>node</code>.
	 *
	 * <h3>Requirements/notes:</h3>
	 * <ol>
	 * <li>Requires undirected graphs.
	 * <li>{@code check} is a {@code boolean} array indicating which nodes can or
	 * cannot be reached from {@code node}.
	 * </ol>
	 * 
	 * @param node  the node to check the connections from
	 * @param check the array of nodes that can be reached
	 * 
	 * @return {@code true} if graph is connected
	 */
	public boolean isGraphConnected(int node, boolean[] check) {
		check[node] = true;
		int[] neighs = out[node];
		int len = kout[node];
		for (int i = 0; i < len; i++) {
			int nn = neighs[i];
			if (!check[nn])
				isGraphConnected(nn, check);
		}
		return ArrayMath.min(check);
	}

	/**
	 * Utility method to swap edges (undirected links) between nodes: change link
	 * {@code a-an} to {@code a-bn} and {@code b-bn} to {@code b-an}.
	 *
	 * <h3>Requirements/notes:</h3>
	 * Does the same as {@code rewireEdgeAt(a, bn, an); rewireEdgeAt(b, an, bn);}
	 * but without allocating and freeing memory.
	 * 
	 * @param a  the first node
	 * @param an the neighbour of the first node
	 * @param b  the second node
	 * @param bn the neighbour of the second node
	 * @return {@code true} if swap succeeded
	 */
	private boolean swapEdges(int a, int an, int b, int bn) {
		if (a == bn || b == an || an == bn)
			return false;
		if (isNeighborOf(a, bn) || isNeighborOf(b, an))
			return false;

		int[] aout = out[a];
		int ai = -1;
		while (aout[++ai] != an)
			;
		aout[ai] = bn;
		int[] bout = out[b];
		int bi = -1;
		while (bout[++bi] != bn)
			;
		bout[bi] = an;

		int[] ain = in[a];
		ai = -1;
		while (ain[++ai] != an)
			;
		ain[ai] = bn;
		int[] bin = in[b];
		bi = -1;
		while (bin[++bi] != bn)
			;
		bin[bi] = an;

		aout = out[an];
		ai = -1;
		while (aout[++ai] != a)
			;
		aout[ai] = b;
		bout = out[bn];
		bi = -1;
		while (bout[++bi] != b)
			;
		bout[bi] = a;

		ain = in[an];
		ai = -1;
		while (ain[++ai] != a)
			;
		ain[ai] = b;
		bin = in[bn];
		bi = -1;
		while (bin[++bi] != b)
			;
		bin[bi] = a;

		return true;
	}

	// private void printConnections() {
	// for( int n=0; n<size; n++ ) {
	// String msg = n+": ";
	// int k = kout[n];
	// for( int i=0; i<k; i++ )
	// msg += out[n][i]+" ";
	// EvoLudo.logDebug(msg);
	// }
	// }

	// private boolean checkConnections(int[] degdist) {
	// boolean ok = true;
	//
	// logger.fine("Checking degree distribution... ");
	// for( int i=0; i<size; i++ ) {
	// //connectivity - degree distribution
	// if( degdist[i] != kout[i] ) {
	// ok = false;
	// logger.fine("Node "+i+" has "+kout[i]+" out-neighbors... ("+degdist[i]+")");
	// }
	// if( degdist[i] != kin[i] ) {
	// ok = false;
	// logger.fine("Node "+i+" has "+kin[i]+" in-neighbors... ("+degdist[i]+")");
	// }
	// }
	// logger.fine("Degree distribution check: "+(ok?"success!":"failed!"));
	// return (ok & checkConnections());
	// }

	/**
	 * Check consistency of links.
	 *
	 * <h3>Requirements/notes:</h3>
	 * <ol>
	 * <li>Self connections are unacceptable.
	 * <li>Double links between nodes are unacceptable.
	 * <li>For undirected networks every outgoing link must correspond to an
	 * incoming link.
	 * <li>ToDo: "self-connections" are acceptable for inter-species interactions.
	 * </ol>
	 * 
	 * @return {@code true} if check succeeded
	 */
	public boolean checkConnections() {
		boolean ok = true, allOk = true;

		logger.fine("Checking multiple out-connections... ");
		for (int i = 0; i < size; i++) {
			// double connections 'out'
			int nout = kout[i];
			for (int j = 0; j < nout; j++) {
				int idx = out[i][j];
				for (int k = j + 1; k < nout; k++)
					if (out[i][k] == idx) {
						ok = false;
						logger.fine("Node " + i + " has double out-connection with node " + idx);
					}
			}
		}
		logger.fine("Multiple out-connections check: " + (ok ? "success!" : "failed!"));
		allOk &= ok;
		ok = true;
		logger.fine("Checking multiple in-connections... ");
		for (int i = 0; i < size; i++) {
			// double connections 'in'
			int nin = kin[i];
			for (int j = 0; j < nin; j++) {
				int idx = in[i][j];
				for (int k = j + 1; k < nin; k++)
					if (in[i][k] == idx) {
						ok = false;
						logger.fine("Node " + i + " has double in-connection with node " + idx);
					}
			}
		}
		logger.fine("Multiple in-connections check: " + (ok ? "success!" : "failed!"));
		allOk &= ok;
		ok = true;
		logger.fine("Checking consistency of in-, out-connections... ");
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
		allOk &= ok;
		ok = true;
		logger.fine("Checking for loops (self-connections) in in-, out-connections... ");
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
		allOk &= ok;
		ok = true;
		if (isRegular) {
			logger.fine("Checking regularity... ");
			int nout = minOut;
			int nin = minIn;
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
			allOk &= ok;
			ok = true;
		}
		if (isUndirected) {
			logger.fine("Checking undirected structure... ");
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
			allOk &= ok;
		}
		return allOk;
	}

	/**
	 * Add edge (undirected link) from node <code>from</code> to node
	 * <code>to</code>.
	 * 
	 * <h3>Requirements/notes:</h3>
	 * None.
	 * 
	 * @param from the first node
	 * @param to   the second node
	 */
	public void addEdgeAt(int from, int to) {
		addLinkAt(from, to);
		addLinkAt(to, from);
	}

	/**
	 * Add link (directed link) from node <code>from</code> to node
	 * <code>to</code>.
	 * 
	 * <h3>Requirements/notes:</h3>
	 * <ol>
	 * <li>Allocate new memory if required.
	 * <li>Geometry needs to be re-evaluated when finished.
	 * </ol>
	 * 
	 * @param from the index of the first node
	 * @param to   the index of the second node
	 * 
	 * @see #evaluate()
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

		// incoming
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
		// if( ko+ki>geom.maxTot ) geom.maxTot = ko+ki;
		maxTot = Math.max(maxTot, ko + kin[from]);
		maxTot = Math.max(maxTot, kout[to] + ki);
		evaluated = false;
	}

	/**
	 * Remove edge (undirected link) from node <code>from</code> to node
	 * <code>to</code>.
	 * 
	 * <h3>Requirements/notes:</h3>
	 * <ol>
	 * <li>Does not update {@code maxIn}, {@code maxOut} or {@code maxTot}.
	 * <li>Geometry needs to be re-evaluated when finished.
	 * </ol>
	 * 
	 * @param from the index of the first node
	 * @param to   the index of the second node
	 * 
	 * @see #evaluate()
	 */
	public void removeEdgeAt(int from, int to) {
		removeLinkAt(from, to);
		removeLinkAt(to, from);
	}

	/**
	 * Remove link (directed link) from node <code>from</code> to node
	 * <code>to</code>.
	 * 
	 * <h3>Requirements/notes:</h3>
	 * <ol>
	 * <li>Does not update {@code maxIn}, {@code maxOut} or {@code maxTot}.
	 * <li>Geometry needs to be re-evaluated when finished.
	 * </ol>
	 * 
	 * @param from the index of the first node
	 * @param to   the index of the second node
	 * 
	 * @see #evaluate()
	 */
	public void removeLinkAt(int from, int to) {
		removeInLink(from, to);
		removeOutLink(from, to);
	}

	/**
	 * Remove all outgoing links from node <code>idx</code>.
	 * 
	 * <h3>Requirements/notes:</h3>
	 * Doesn't free memory.
	 * 
	 * @param idx the index of the node to remove all outgoing links
	 */
	public void clearLinksFrom(int idx) {
		// remove in-links
		int len = kout[idx];
		int[] neigh = out[idx];
		for (int i = 0; i < len; i++)
			removeInLink(idx, neigh[i]);
		// clear out-links
		kout[idx] = 0;
		minOut = 0;
		// could free some memory too...
	}

	/**
	 * Remove incoming link (directed link) to node <code>to</code> from node
	 * <code>from</code>.
	 * 
	 * <h3>Requirements/notes:</h3>
	 * Geometry needs to be re-evaluated when done with all manipulations.
	 * 
	 * @param from the index of the first node
	 * @param to   the index of the second node
	 * 
	 * @see #evaluate()
	 */
	private void removeInLink(int from, int to) {
		// find index
		int idx = -1;
		int[] mem = in[to];
		int k = kin[to];
		for (int i = 0; i < k; i++)
			if (mem[i] == from) {
				idx = i;
				break;
			}
		if (idx < 0)
			return; // not found - ignore
		// remove links - do not shrink array
		System.arraycopy(mem, idx + 1, mem, idx, k - 1 - idx);
		kin[to]--;
		if (k - 1 < minIn)
			minIn = k - 1;
		evaluated = false;
	}

	/**
	 * Remove all incoming links to node <code>idx</code>.
	 * 
	 * <h3>Requirements/notes:</h3>
	 * Doesn't free memory.
	 * 
	 * @param idx the index of the node to remove all incoming links
	 */
	public void clearLinksTo(int idx) {
		// remove out-links
		int len = kin[idx];
		int[] neigh = in[idx];
		for (int i = 0; i < len; i++)
			removeOutLink(neigh[i], idx);
		// clear in-links
		kin[idx] = 0;
		minIn = 0;
		// could free some memory too...
	}

	/**
	 * Remove outgoing link (directed link) from node <code>from</code> to node
	 * <code>to</code>.
	 * 
	 * <h3>Requirements/notes:</h3>
	 * Geometry needs to be re-evaluated when done with all manipulations.
	 * 
	 * @param from the index of the first node
	 * @param to   the index of the second node
	 * 
	 * @see #evaluate()
	 */
	private void removeOutLink(int from, int to) {
		// find index
		int idx = -1;
		int[] mem = out[from];
		int k = kout[from];
		for (int i = 0; i < k; i++)
			if (mem[i] == to) {
				idx = i;
				break;
			}
		if (idx < 0)
			return; // not found - ignore
		// remove links - do not shrink array
		System.arraycopy(mem, idx + 1, mem, idx, k - 1 - idx);
		kout[from]--;
		if (k - 1 < minOut)
			minOut = k - 1;
		evaluated = false;
	}

	/**
	 * Rewire directed link from node <code>from</code> to node <code>prev</code> to
	 * node <code>to</code>.
	 * 
	 * <h3>Requirements/notes:</h3>
	 * Geometry needs to be re-evaluated when done with all manipulations.
	 * 
	 * @param from the index of the first node
	 * @param to   the index of the second node
	 * @param prev the index of the second node
	 * 
	 * @see #evaluate()
	 */
	public void rewireLinkAt(int from, int to, int prev) {
		removeLinkAt(from, prev);
		addLinkAt(from, to);
	}

	/**
	 * Rewire edge (undirected link) from node <code>from</code> to node
	 * <code>prev</code> to node <code>to</code>.
	 * 
	 * <h3>Requirements/notes:</h3>
	 * Geometry needs to be re-evaluated when done with all manipulations.
	 * 
	 * @param from the index of the first node
	 * @param to   the index of the second node
	 * @param prev the index of the second node
	 * 
	 * @see #evaluate()
	 */
	public void rewireEdgeAt(int from, int to, int prev) {
		rewireLinkAt(from, to, prev);
		removeLinkAt(prev, from);
		addLinkAt(to, from);
	}

	/**
	 * Check if {@code check} is a neighbor of {@code focal} (not necessarily the
	 * other way round). For undirected networks {@code focal} and {@code check} can
	 * be exchanged.
	 *
	 * @param focal index of focal individual
	 * @param check index of individual to be checked
	 * @return {@code true} if {@code check} is neighbor of {@code focal}
	 */
	public boolean isNeighborOf(int focal, int check) {
		int[] neigh = out[focal];
		int k = kout[focal];
		for (int n = 0; n < k; n++)
			if (neigh[n] == check)
				return true;
		return false;
	}

	/**
	 * Derive interaction geometry from current (reproduction) geometry. This is
	 * only possible if {@code interReproSame} is {@code true}. Returns {@code null}
	 * otherwise.
	 * <p>
	 * If {@code opp==population} then it is an intra-species interaction, which
	 * allows to simply return {@code this}, i.e. no cloning etc. required.
	 * Otherwise the geometry is cloned, the {@code opponent} set and 'self-loops'
	 * added for interactions with individuals in the same location.
	 *
	 * @param opp the population of interaction partners
	 * @return the derived interaction geometry or {@code null} if it cannot be
	 *         derived
	 */
	public Geometry deriveInteractionGeometry(IBSPopulation opp) {
		// this is reproduction geometry (hence population==opponent)
		if (!interReproSame)
			return null; // impossible to derive interaction geometry
		// intra-species interactions: nothing to derive - use same geometry
		if (population == opp)
			return this;
		Geometry interaction = clone();
		interaction.opponent = opp;
		// add interactions with individual in same location
		for (int n = 0; n < size; n++)
			interaction.addLinkAt(n, n);
		interaction.evaluate();
		return interaction;
	}

	/**
	 * Derive reproduction geometry from current (interaction) geometry. This is
	 * only possible if {@code interReproSame} is {@code true}. Returns {@code null}
	 * otherwise.
	 * <p>
	 * If {@code opp==population} then it is an intra-species interaction, which
	 * allows to simply return {@code this}, i.e. no cloning etc. required.
	 * Otherwise the geometry is cloned, the {@code opponent}, the opponent set and
	 * 'self-loops' removed.
	 *
	 * @return the derived interaction geometry or {@code null} if it cannot be
	 *         derived
	 */
	public Geometry deriveReproductionGeometry() {
		// this is interaction geometry (hence population!=opponent for inter-species
		// interactions)
		if (!interReproSame)
			return null; // impossible to derive reproduction geometry
		// intra-species interactions: nothing to derive - use same geometry
		if (population == opponent)
			return this;
		Geometry reproduction = clone();
		reproduction.opponent = population;
		// add interactions with individual in same location
		if (reproduction.geometry != Type.MEANFIELD)
			for (int n = 0; n < size; n++)
				reproduction.removeLinkAt(n, n);
		reproduction.evaluate();
		return reproduction;
	}

	/**
	 * Parse string of geometry specifications <code>cli</code>. Check for
	 * consistency of settings (e.g. in terms of population size, connectivity). If
	 * adjustments are required and possible inform the user through the
	 * {@code logger}.
	 * 
	 * @param cli the string with geometry specifications
	 * @return {@code true} if reset is required
	 */
	public boolean parse(String cli) {
		boolean doReset = false;
		Type oldGeometry = geometry;
		CLOption clo = engine.getModule().cloGeometry;
		geometry = (Type) clo.match(cli);
		String sub = cli.substring(1);
		boolean oldFixedBoundary = fixedBoundary;
		fixedBoundary = false;
		int len = sub.length();
		if (len > 0) {
			char first = sub.charAt(0);
			// fixed boundaries for regular lattices
			if (first == 'f' || first == 'F') {
				fixedBoundary = true;
				sub = sub.substring(1);
			} else {
				char last = sub.charAt(len - 1);
				if (last == 'f' || last == 'F') {
					fixedBoundary = true;
					sub = sub.substring(0, len - 1);
				}
			}
		}
		doReset |= (oldFixedBoundary != fixedBoundary);

		int[] ivec;
		double[] dvec;
		double oldConnectivity = connectivity;
		Type oldSubGeometry = subgeometry;
		subgeometry = Type.VOID;
		switch (geometry) {
			case MEANFIELD: // mean field
				break;
			case COMPLETE: // complete graph
				break;
			case HIERARCHY: // deme structured, hierarchical graph
				subgeometry = Type.MEANFIELD;
				if (!Character.isDigit(sub.charAt(0))) {
					// check for geometry of hierarchies
					// note: we could allow for different geometries at different levels but seems
					// bit overkill - needs good reasons to implement! (e.g. well-mixed demes
					// in spatial arrangement would make sense)
					subgeometry = (Type) clo.match(sub);
					sub = sub.substring(1);
					// check once more for fixed boundaries
					if (sub.charAt(0) == 'f' || sub.charAt(0) == 'F') {
						fixedBoundary = true;
						sub = sub.substring(1);
					}
				}
				if (oldSubGeometry != subgeometry || oldFixedBoundary != fixedBoundary)
					doReset = true;

				// NOTE: Geometry is capable of dealing with arbitrary square lattices but how
				// to specify connectivity? first number?
				int[] oldRawHierarchy = rawhierarchy;
				// H[n,m[f]]<n0>[:<n1>[:<n2>[...]]]w<d> where <ni> refers to the number of units
				// in level i and
				// <d> to the weight of the linkage between subsequent levels
				int widx = sub.lastIndexOf('w');
				if (widx < 0) {
					// 'w' not found - no coupling between hierarchies (identical to isolated demes)
					hierarchyweight = 0;
					rawhierarchy = CLOParser.parseIntVector(sub);
				} else {
					hierarchyweight = CLOParser.parseDouble(sub.substring(widx + 1));
					rawhierarchy = CLOParser.parseIntVector(sub.substring(0, widx));
				}
				if (oldRawHierarchy != null)
					doReset |= (ArrayMath.norm(ArrayMath.sub(oldRawHierarchy, rawhierarchy)) > 0);
				break;
			case LINEAR: // linear
				int[] conn = CLOParser.parseIntVector(sub);
				switch (conn.length) {
					default:
						logger.warning("too many arguments for linear geometry.");
						//$FALL-THROUGH$
					case 2:
						connectivity = conn[0] + conn[1];
						linearAsymmetry = conn[0] - conn[1];
						break;
					case 1:
						connectivity = conn[0];
						//$FALL-THROUGH$
					case 0:
						connectivity = Math.max(2, connectivity);
						linearAsymmetry = 0;
				}
				break;
			case SQUARE_NEUMANN_2ND: // four second neighbours
			case SQUARE_NEUMANN: // von neumann
				connectivity = 4;
				break;
			case SQUARE_MOORE: // moore
				connectivity = 8;
				break;
			case SQUARE: // square, larger neighborhood
				if (sub.length() < 1)
					sub = "4"; // default
				//$FALL-THROUGH$
			case CUBE: // cubic, larger neighborhood
			case HONEYCOMB: // hexagonal
				if (sub.length() < 1)
					sub = "6"; // default
				//$FALL-THROUGH$
			case TRIANGULAR: // triangular
				if (sub.length() < 1)
					sub = "3"; // default
				// allow any connectivity - check() ensures validity
				connectivity = Integer.parseInt(sub);
				break;
			case FRUCHT: // Frucht graph
			case TIETZE: // Tietze graph
			case FRANKLIN: // Franklin graph
			case HEAWOOD: // Heawood graph
			case DODEKAHEDRON: // Dodekahedron graph
			case DESARGUES: // Desargues graph
				connectivity = 3;
				break;
			case ICOSAHEDRON: // Icosahedron graph
				connectivity = 5;
				break;
			case RANDOM_REGULAR_GRAPH: // random regular graph
				connectivity = Math.max(2, Integer.parseInt(sub));
				break;
			case RANDOM_GRAPH: // random graph
				connectivity = Math.max(2, Integer.parseInt(sub));
				break;
			case RANDOM_GRAPH_DIRECTED: // random graph directed
				connectivity = Math.max(2, Integer.parseInt(sub));
				break;
			case STAR: // star
				petalsamplification = 2;
				break;
			case WHEEL: // wheel - cycle (k=2) with single hub (k=N-1)
				break;
			case SUPER_STAR: // super-star
				int oldPetalsAmplification = petalsamplification;
				int oldPetalsCount = petalscount;
				petalsamplification = 3;
				ivec = CLOParser.parseIntVector(sub);
				switch (ivec.length) {
					default:
					case 2:
						petalsamplification = ivec[1];
						//$FALL-THROUGH$
					case 1:
						petalscount = ivec[0];
						break;
					case 0:
						geometry = Type.INVALID; // too few parameters, change to default geometry
				}
				doReset |= (oldPetalsAmplification != petalsamplification);
				doReset |= (oldPetalsCount != petalscount);
				break;
			case STRONG_AMPLIFIER: // strong amplifier
			case STRONG_SUPPRESSOR: // strong suppressor
				// known geometries but no further settings required
				break;
			case SCALEFREE_BA: // scale-free network - barabasi & albert
				double oldConn = connectivity;
				connectivity = Math.max(2, Integer.parseInt(sub));
				doReset = (Math.abs(oldConn - connectivity) > 1e-8);
				break;
			case SCALEFREE_KLEMM: // scale-free network - klemm
				if (pRewire > 0.0 || pAddwire > 0.0) {
					logger.warning("adding or rewiring links not supported for '" + geometry + "' - ignored!");
					doReset = true;
				}
				pRewire = 0.0;
				pAddwire = 0.0;
				double oldKlemm = pKlemm;
				pKlemm = 0.0;
				dvec = CLOParser.parseVector(sub);
				switch (dvec.length) {
					default:
					case 2:
						pKlemm = Math.min(Math.max(dvec[1], 0.0), 1.0);
						doReset = (Math.abs(oldKlemm - pKlemm) > 1e-8);
						//$FALL-THROUGH$
					case 1:
						connectivity = Math.max(2, (int) dvec[0]);
						break;
					case 0:
						geometry = Type.INVALID; // too few parameters, change to default geometry
				}
				break;
			case SCALEFREE: // scale-free network - uncorrelated, from degree distribution
				double oldSfExponent = sfExponent;
				sfExponent = 0.0;
				dvec = CLOParser.parseVector(sub);
				switch (dvec.length) {
					default:
					case 2:
						sfExponent = dvec[1];
						//$FALL-THROUGH$
					case 1:
						connectivity = Math.max(2, (int) dvec[0]);
						break;
					case 0:
						geometry = Type.INVALID; // too few parameters, change to default geometry
				}
				doReset |= (oldSfExponent != sfExponent);
				break;
			/*
			 * not yet implemented... case 'g': // scale-free network - directed
			 * geom.geometry = Geometry.SCALEFREE_DIRECTED; geom.connectivity = Math.max(2,
			 * arg); break;
			 */
			default:
				// last resort: try engine - maybe new implementations provide new geometries
				if (!population.parseGeometry(this, cli))
					geometry = Type.INVALID; // too few parameters, change to default geometry
				break;
		}

		doReset |= (oldGeometry != geometry);
		if (Math.abs(oldConnectivity - connectivity) > 1e-6)
			doReset = true;
		isValid &= !doReset;
		return doReset;
	}

	/**
	 * @return usage for <code>--geometry</code> command line option
	 */
	public String usage() {
		CLOption clo = engine.getModule().cloGeometry;
		boolean fixedBoundariesAvailable = (clo.isValidKey(Type.LINEAR) || clo.isValidKey(Type.SQUARE)
				|| clo.isValidKey(Type.CUBE)
				|| clo.isValidKey(Type.HONEYCOMB) || clo.isValidKey(Type.TRIANGULAR));
		String descr = "--geometry <>   geometry - interaction==reproduction\n" //
				+ "      argument: <g><k>" //
				+ (fixedBoundariesAvailable ? "[f|F]" : "") + " (g type, k neighbours)\n" //
				+ clo.getDescriptionKey() + "\n      further specifications:" //
				+ (fixedBoundariesAvailable ? "\n           f|F: fixed lattice boundaries (default periodic)" : "");
		return descr;
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
	public Geometry clone() {
		Geometry clone = new Geometry(engine, (population != null ? population.getModule() : null),
				(opponent != null ? opponent.getModule() : null));
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
		if (rawhierarchy != null)
			clone.rawhierarchy = Arrays.copyOf(rawhierarchy, rawhierarchy.length);
		if (hierarchy != null)
			clone.hierarchy = Arrays.copyOf(hierarchy, hierarchy.length);
		clone.hierarchyweight = hierarchyweight;
		clone.size = size;
		clone.geometry = geometry;
		clone.fixedBoundary = fixedBoundary;
		clone.minIn = minIn;
		clone.maxIn = maxIn;
		clone.avgIn = avgIn;
		clone.minOut = minOut;
		clone.maxOut = maxOut;
		clone.avgOut = avgOut;
		clone.minTot = minTot;
		clone.maxTot = maxTot;
		clone.avgTot = avgTot;
		clone.petalscount = petalscount;
		clone.petalsamplification = petalsamplification;
		clone.sfExponent = sfExponent;
		clone.connectivity = connectivity;
		clone.pRewire = pRewire;
		clone.pAddwire = pAddwire;
		clone.isUndirected = isUndirected;
		clone.isRewired = isRewired;
		clone.interReproSame = interReproSame;
		clone.isDynamic = isDynamic;
		clone.isRegular = isRegular;
		clone.isValid = isValid;
		return clone;
	}

	/**
	 * Check if {@code this} Geometry and {@code geo} refer to the same structures.
	 * Different realizations of random structures, such as random regular graphs,
	 * are considered equal as long as their characteristic parameters are the same.
	 *
	 * @param geo the geometry to compare to.
	 * @return {@code true} if the structures are the same
	 */
	public boolean equals(Geometry geo) {
		if (geo == this)
			return true;
		if (geo.geometry != geometry)
			return false;
		if (geo.size != size)
			return false;
		if (Math.abs(geo.connectivity - connectivity) > 1e-6)
			return false;
		return true;
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
		plist.append(Plist.encodeKey("Name", geometry.getTitle()));
		plist.append(Plist.encodeKey("Code", geometry.getKey()));
		// the following lines mimic earlier output
		// plist.append(Plist.encodeKey("Name", geometry.getKey() + ": " +
		// geometry.getDescription()));
		// int code = geometry.getKey().charAt(0);
		// if (code>=48 && code<=57)
		// code += 256-48;
		// plist.append(Plist.encodeKey("Code", code));
		// no need to explicitly encode geometries that can be easily and unambiguously
		// re-generated
		if (isUniqueGeometry()) {
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
		if (!isUniqueGeometry())
			return;
		// decode geometry
		Plist graph = (Plist) plist.get("Graph");
		ArrayList<List<Integer>> outlinks = new ArrayList<List<Integer>>(size);
		ArrayList<ArrayList<Integer>> inlinks = new ArrayList<ArrayList<Integer>>(size);
		List<Integer> placeholder = new ArrayList<Integer>();
		for (int n = 0; n < size; n++) {
			outlinks.add(placeholder);
			inlinks.add(new ArrayList<Integer>());
		}
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
		// finish
		evaluate();
	}

	/**
	 * Check if current geometry unique. Only unique geomteries need to be encoded.
	 * 
	 * <h3>Requirements/notes:</h3>
	 * <ol>
	 * <li>Lattices etc. are not unique because they can be identically recreated.
	 * <li>All geometries involving random elements are unique.
	 * </ol>
	 *
	 * @return {@code true} if geometry is unique
	 */
	public boolean isUniqueGeometry() {
		return isUniqueGeometry(geometry);
	}

	/**
	 * Helper method to check uniqueness of the geometry {@code geo}.
	 * 
	 * <h3>Requirements/notes:</h3>
	 * Hierarchical geometries require recursive checks of uniqueness.
	 * 
	 * @param geo the geometry to be checked for uniqueness
	 * @return {@code true} if geometry is unique
	 */
	private boolean isUniqueGeometry(Type geo) {
		switch (geo) {
			// non-unique geometries
			case MEANFIELD: // mean field
			case COMPLETE: // complete graph
			case LINEAR: // linear
			case SQUARE_NEUMANN: // von neumann
			case SQUARE_MOORE: // moore
			case SQUARE: // square, larger neighborhood
			case CUBE: // cubic, larger neighborhood
			case HONEYCOMB: // hexagonal
			case TRIANGULAR: // triangular
			case FRUCHT: // Frucht graph
			case TIETZE: // Tietze graph
			case FRANKLIN: // Franklin graph
			case HEAWOOD: // Heawood graph
			case DODEKAHEDRON: // Dodekahedron graph
			case DESARGUES: // Desargues graph
			case ICOSAHEDRON: // Icosahedron graph
				// some suppressors are non-unique
				// some amplifiers are non-unique
			case STAR: // star
			case WHEEL: // wheel - cycle (k=2) with single hub (k=N-1)
			case SUPER_STAR: // super-star
				return false;

			// hierarchies of random regular graphs or similar would be unique
			case HIERARCHY: // deme structured, hierarchical graph
				return isUniqueGeometry(subgeometry);

			// unique graphs
			case RANDOM_REGULAR_GRAPH: // random regular graph
			case RANDOM_GRAPH: // random graph
			case RANDOM_GRAPH_DIRECTED: // random graph directed
			case STRONG_AMPLIFIER: // strong amplifier
			case STRONG_SUPPRESSOR: // strong suppressor
			case SCALEFREE_BA: // scale-free network - barabasi & albert
			case SCALEFREE_KLEMM: // scale-free network - klemm
			case SCALEFREE: // scale-free network - uncorrelated, from degree distribution
				// for unknown graphs simply assume is unique
			default:
				return true;
		}
	}
}
