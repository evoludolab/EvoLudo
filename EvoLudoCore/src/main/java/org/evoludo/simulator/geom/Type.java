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

import org.evoludo.simulator.EvoLudo;
import org.evoludo.simulator.models.IBS;
import org.evoludo.simulator.modules.Module;
import org.evoludo.util.CLOption;

/**
 * The types of graph geometries. Currently available graph geometries are:
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
 */
public enum Type implements CLOption.Key {
	/**
	 * Mean-field/well-mixed population.
	 * 
	 * @see WellmixedGeometry#init()
	 */
	WELLMIXED("M", "well-mixed population"),

	/**
	 * Complete graph, connectivity \(k=N-1\).
	 * 
	 * @see CompleteGeometry#init()
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
	 * @see org.evoludo.simulator.Geometry#initGeometryHierarchical()
	 */
	HIERARCHY("H", "hierarchical (meta-)populations",
			"H[<g>[f]]<n1>[,<n2>[...,<nm>]]w<w> hierarchical\n"//
					+ "                structure for population geometries g:\n"//
					+ "                M: well-mixed (default)\n"//
					+ "                n: square lattice (von neumann)\n"//
					+ "                m: square lattice (moore)\n"//
					+ "                append f for fixed boundaries\n"//
					+ "                n1,...,nm number of units on each level\n"//
					+ "                total of m+1 levels with nPopulation/(n1*...*nm)\n"//
					+ "                individuals in last level\n"//
					+ "                w: strength of ties between levels"),

	/**
	 * Linear lattice, 1D. {@code l<l>[,<r>]} specifies a linear lattice with
	 * {@code l} neighbours to the left and {@code r} to the right. If {@code r} is
	 * missing or {@code r==l} the neighbourhood is symmetric.
	 * 
	 * @see LinearGeometry#init()
	 */
	LINEAR("l", "linear lattice, 1D", "l<l>[,<r>] linear lattice (l neighbourhood,\n"//
			+ "                if r!=l asymmetric neighbourhood)"),

	/**
	 * Square lattice (von Neumann neighbourhood). Four nearest neighbours (north,
	 * east, south, west).
	 * 
	 * @see SquareGeometry#init()
	 * @see SquareGeometry#initVonNeumann(int, int, int)
	 */
	SQUARE_NEUMANN("n", "square lattice (von Neumann)"),

	/**
	 * Square lattice. Four second-nearest neighbours (north-east, north-west,
	 * south-west, south-east).
	 * 
	 * @see SquareGeometry#init()
	 * @see SquareGeometry#initVonNeumann2nd(int, int, int)
	 */
	SQUARE_NEUMANN_2ND("n2", "square lattice, diagonal neighbours"),

	/**
	 * Square lattice (Moore neighbourhood). Eight nearest neighbours (chess kings
	 * moves).
	 * 
	 * @see SquareGeometry#init()
	 * @see SquareGeometry#initMoore(int, int, int)
	 */
	SQUARE_MOORE("m", "square lattice (Moore)"),

	/**
	 * Square lattice, 2D. {@code N<n>} specifies a square lattice with {@code n}
	 * neighbours, where {@code n} is {@code 3x3, 5x5...}.
	 * 
	 * @see SquareGeometry#init()
	 * @see SquareGeometry#init(int, int, int)
	 */
	SQUARE("N", "square lattice, 2D", "N<k> square lattice (k=3x3, 5x5...)"),

	/**
	 * Cubic lattice, 3D. {@code C<n>} cubic lattice with {@code n} neighbours,
	 * where {@code n} is {@code 2+2+2, 3x3x3, 5x5x5...}.
	 * 
	 * @see CubicGeometry#init()
	 */
	CUBE("C", "cubic lattice, 3D", "C<k> cubic lattice (k=2+2+2, 3x3x3, 5x5x5...)"),

	/**
	 * Hexagonal (honeycomb) lattice, connectivity \(k=6\).
	 * 
	 * @see HexagonalGeometry#init()
	 */
	HEXAGONAL("h", "hexagonal lattice (k=6)"),

	/**
	 * Triangular lattice, connectivity \(k=3\).
	 * 
	 * @see TriangularGeometry#init()
	 */
	TRIANGULAR("t", "triangular lattice (k=3)"),

	/**
	 * Frucht graph, size \(N=12\), connectivity \(k=3\).
	 * 
	 * @see FruchtGeometry#init()
	 */
	FRUCHT("0", "Frucht graph (N=12, k=3)"),

	/**
	 * Tietze's graph, size \(N=12\), connectivity \(k=3\).
	 * 
	 * @see TietzeGeometry#init()
	 */
	TIETZE("1", "Tietze graph (N=12, k=3)"),

	/**
	 * Franklin graph, size \(N=12\), connectivity \(k=3\).
	 * 
	 * @see FranklinGeometry#init()
	 */
	FRANKLIN("2", "Franklin graph (N=12, k=3)"),

	/**
	 * Heawood graph, size \(N=14\), connectivity \(k=3\).
	 * 
	 * @see HeawoodGeometry#init()
	 */
	HEAWOOD("3", "Heawood graph (N=14, k=3)"),

	/**
	 * Icosahedron graph, size \(N=12\), connectivity \(k=5\).
	 * 
	 * @see IcosahedronGeometry#init()
	 */
	ICOSAHEDRON("4", "Icosahedron graph (N=12, k=5)"),

	/**
	 * Dodekahedron graph, size \(N=20\), connectivity \(k=3\).
	 * 
	 * @see DodekahedronGeometry#init()
	 */
	DODEKAHEDRON("5", "Dodekahedron graph (N=20, k=3)"),

	/**
	 * Desargues graph, size \(N=20\), connectivity \(k=3\).
	 * 
	 * @see DesarguesGeometry#init()
	 */
	DESARGUES("6", "Desargues graph (N=20, k=3)"),

	/**
	 * Star graph, connectivity \(k=2(N-1)/N\).
	 * 
	 * @see StarGeometry#init()
	 */
	STAR("s", "star (single hub)"),

	/**
	 * Superstar graph (single hub, petals). {@code S<p[,k]>} superstar graph with
	 * {@code p} petals and amplification {@code k}.
	 * 
	 * @see SuperstarGeometry#init()
	 */
	SUPER_STAR("S", "super-star (single hub, petals)",
			"S<p[,a]> super-star, p petals [1], a amplify [3]"),

	/**
	 * Wheel graph, connectivity \(k=4(N-1)/N\).
	 * 
	 * @see WheelGeometry#init()
	 */
	WHEEL("w", "wheel (cycle, single hub)"),

	/**
	 * Strong (undirected) amplifier.
	 * 
	 * @see StrongAmplifierGeometry#init()
	 */
	STRONG_AMPLIFIER("+", "strong (undirected) amplifier"),

	/**
	 * Strong (undirected) suppressor.
	 * 
	 * @see StrongSuppressorGeometry#init()
	 */
	STRONG_SUPPRESSOR("-", "strong (undirected) suppressor"),

	/**
	 * Random regular graph.
	 * 
	 * @see org.evoludo.simulator.Geometry#initGeometryRandomRegularGraph()
	 */
	RANDOM_REGULAR_GRAPH("r", "random regular graph",
			"r<d> random regular graph, d degree [2]"),

	/**
	 * Random graph. {@code R<d>} random graph with degree {@code d}.
	 * 
	 * @see org.evoludo.simulator.Geometry#initGeometryRandomGraph()
	 */
	RANDOM_GRAPH("R", "random graph", "R<d> random graph, d degree [2]"),

	/**
	 * Directed random graph. {@code D<d>} random graph with degree {@code d}.
	 * 
	 * @see org.evoludo.simulator.Geometry#initGeometryRandomGraphDirected()
	 */
	RANDOM_GRAPH_DIRECTED("D", "random graph (directed)",
			"D<d> directed random graph, d degree [2]"),

	/**
	 * Scale-free network. {@code p<e>} scale-free network with exponent {@code e}.
	 * 
	 * @see org.evoludo.simulator.Geometry#initGeometryScaleFree()
	 */
	SCALEFREE("p", "scale-free graph", "p<e> scale-free graph following\n" +
			"                degree distribution with exponent e [-2]"),

	/**
	 * Scale-free network. {@code f<d>} scale-free network with degree {@code d}.
	 * 
	 * @see org.evoludo.simulator.Geometry#initGeometryScaleFreeBA()
	 */
	SCALEFREE_BA("f", "scale-free graph (Barabasi & Albert)",
			"f<n> scale-free graph, n avg. degree\n" + //
					"                (Barabasi & Albert)"),

	/**
	 * Scale-free network. {@code F<n[,p]>} scale-free network with degree {@code n}
	 * and a fraction of {@code p} random links.
	 * 
	 * @see org.evoludo.simulator.Geometry#initGeometryScaleFreeKlemm()
	 */
	SCALEFREE_KLEMM("F", "scale-free, small world graph (Klemm & Eguiluz)",
			"F<n[,p]> scale-free, small world graph\n" + //
					"                (Klemm & Eguiluz) n avg. degree, p random links"),

	/**
	 * Dynamically changing network structure.
	 * 
	 * @see org.evoludo.simulator.Geometry#initGeometryScaleFreeBA()
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
	 * @see IBS#cloGeometryCompetition
	 */
	String key;

	/**
	 * Brief description of geometry type for GUI and help display.
	 * 
	 * @see EvoLudo#getCLOHelp()
	 */
	String title;

	/**
	 * Optional long description of geometry type for help display.
	 * 
	 * @see EvoLudo#getCLOHelp()
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
			case HEXAGONAL:
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

	/**
	 * Parse {@code cli} and instantiate the requested geometry. Currently only the
	 * geometries that have been extracted (mean-field, complete, linear) are
	 * supported.
	 * 
	 * @param engine the EvoLudo engine providing module/CLI metadata
	 * @param cli    the command line style geometry descriptor
	 * @return the configured geometry
	 */
	public static AbstractGeometry parse(EvoLudo engine, String cli) {
		if (engine == null)
			throw new IllegalArgumentException("engine must not be null");
		if (cli == null || cli.isEmpty())
			throw new IllegalArgumentException("geometry specification must not be empty");
		CLOption clo = engine.getModule().cloGeometry;
		Type type = (Type) clo.match(cli);
		AbstractGeometry geometry = AbstractGeometry.create(type, engine);
		String spec = cli.substring(1);
		switch (type) {
			case WELLMIXED:
			case COMPLETE:
				break;
			case LINEAR:
				((LinearGeometry) geometry).parse(spec);
				break;
			case TRIANGULAR:
				((TriangularGeometry) geometry).parse(spec);
				break;
			case HEXAGONAL:
				((HexagonalGeometry) geometry).parse(spec);
				break;
			case CUBE:
				((CubicGeometry) geometry).parse(spec);
				break;
			case STAR:
				((StarGeometry) geometry).parse(spec);
				break;
			case SUPER_STAR:
				((SuperstarGeometry) geometry).parse(spec);
				break;
			case WHEEL:
				((WheelGeometry) geometry).parse(spec);
				break;
			case FRUCHT:
				((FruchtGeometry) geometry).parse(spec);
				break;
			case STRONG_AMPLIFIER:
				((StrongAmplifierGeometry) geometry).parse(spec);
				break;
			case STRONG_SUPPRESSOR:
				((StrongSuppressorGeometry) geometry).parse(spec);
				break;
			case TIETZE:
				((TietzeGeometry) geometry).parse(spec);
				break;
			case FRANKLIN:
				((FranklinGeometry) geometry).parse(spec);
				break;
			case HEAWOOD:
				((HeawoodGeometry) geometry).parse(spec);
				break;
			case ICOSAHEDRON:
				((IcosahedronGeometry) geometry).parse(spec);
				break;
			case DODEKAHEDRON:
				((DodekahedronGeometry) geometry).parse(spec);
				break;
			case DESARGUES:
				((DesarguesGeometry) geometry).parse(spec);
				break;
			case SQUARE:
			case SQUARE_NEUMANN:
			case SQUARE_NEUMANN_2ND:
			case SQUARE_MOORE:
				((SquareGeometry) geometry).parse(spec);
				break;
			default:
				throw new UnsupportedOperationException(
						"Parsing for geometry '" + type + "' is not yet implemented in the new architecture.");
		}
		return geometry;
	}

	public static boolean isFixedBoundaryToken(char ch) {
		return ch == 'f' || ch == 'F';
	}
}
