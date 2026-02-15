//
// EvoLudo Project
//
// Copyright 2010-2026 Christoph Hauert
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

package org.evoludo.simulator.geometries;

import java.util.Arrays;

import org.evoludo.math.ArrayMath;
import org.evoludo.simulator.EvoLudo;
import org.evoludo.util.CLOParser;
import org.evoludo.util.CLOption;
import org.evoludo.util.Formatter;

/**
 * Hierarchical meta-population structure implementation. Embeds well-mixed or
 * square-lattice demes into recursive layers.
 */
public class HierarchicalGeometry extends AbstractLattice {

	/**
	 * The geometry of each hierarchical level.
	 */
	private GeometryType subType = GeometryType.WELLMIXED;

	/**
	 * The number of units at each hierarchical level.
	 */
	private int[] hierarchy;

	/**
	 * Coupling strength between hierarchical levels.
	 */
	private double hierarchyWeight = 0.0;

	/**
	 * Create a hierarchical geometry tied to the given engine.
	 *
	 * @param engine EvoLudo pacemaker
	 */
	public HierarchicalGeometry(EvoLudo engine) {
		super(engine);
		setType(GeometryType.HIERARCHY);
	}

	@Override
	boolean parse(String spec) {
		// parsing is postponed to checkSettings()
		return true;
	}

	/**
	 * Check if the sub-geometry matches the given type.
	 * 
	 * @param type the geometry type to check against
	 * @return {@code true} if the sub-geometry matches the given type
	 */
	public boolean isSubtype(GeometryType type) {
		return subType == type;
	}

	/**
	 * Retrieve the geometry used within each hierarchy level (e.g. square or
	 * well-mixed).
	 * 
	 * @return the geometry used within each hierarchy level (e.g. square or
	 *         well-mixed)
	 */
	public GeometryType getSubType() {
		return subType;
	}

	/**
	 * Report the weight applied to inter-level interactions.
	 * 
	 * @return the weight applied to inter-level interactions
	 */
	public double getHierarchyWeight() {
		return hierarchyWeight;
	}

	/**
	 * Retrieve a copy of the processed hierarchy definition (levels plus
	 * individuals per deme).
	 * 
	 * @return a copy of the processed hierarchy definition (levels plus
	 *         individuals per deme)
	 */
	public int[] getHierarchyLevels() {
		return hierarchy;
	}

	@Override
	protected boolean checkSettings() {
		boolean reset = false;
		parseHierarchy();
		int nHierarchy = hierarchy.length;
		for (int i = 0; i < nHierarchy; i++) {
			// collapse hierarchies with single units
			if (hierarchy[i] <= 1)
				hierarchy = ArrayMath.drop(hierarchy, i);
		}
		if (nHierarchy == 0) {
			warn("hierarchies must encompass ≥2 levels - reset to single level.");
			setType(subType);
			return true;
		}
		if (nHierarchy != hierarchy.length) {
			warn("hierarchy levels must include >1 units - hierarchies collapsed to " + (nHierarchy + 1) + " levels.");
			reset = true;
		}
		reset |= calcUnitSize();
		return reset;
	}

	/**
	 * Process the hierarchy specifications to calculate the number of units and the
	 * size of each unit. On each level there must be at least 2 units for
	 * well-mixed hierarchies and each unit must contain at least 2 individuals. For
	 * hierarchies with square lattices each level must ahev at least 2x2 units with
	 * at least 3x3 individuals each.
	 * 
	 * @return {@code true} if the size was adjusted and resetting is required
	 */
	private boolean calcUnitSize() {
		int nIndiv;
		connectivity = 0;
		int nHierarchy = hierarchy.length;
		switch (subType) {
			case SQUARE_MOORE:
				connectivity = 8;
				//$FALL-THROUGH$
			case SQUARE_NEUMANN_2ND:
			case SQUARE_NEUMANN:
				connectivity = Math.max(connectivity, 4);
				//$FALL-THROUGH$
			case SQUARE:
				nIndiv = calcSquareUnit(nHierarchy);
				break;
			case COMPLETE:
				subType = GeometryType.WELLMIXED;
				//$FALL-THROUGH$
			case WELLMIXED:
				int prod = 1;
				for (int i = 0; i < nHierarchy; i++)
					prod *= hierarchy[i];
				int nUnit = Math.max(2, size > 0 ? size / Math.max(1, prod) : 2);
				hierarchy[nHierarchy] = nUnit;
				connectivity = Math.max(0, nUnit - 1);
				nIndiv = nUnit * prod;
				break;
			default:
				throw new IllegalStateException("Unhandled sub-geometry type: " + subType);
		}
		if (setSize(nIndiv)) {
			if (engine.getModule().cloNPopulation.isSet())
				warn("hierarchies " + Formatter.format(hierarchy) + " require population size " + size + "!");
			return true;
		}
		return false;
	}

	/**
	 * Calculate the unit size for square-lattice hierarchies, adjusting each level
	 * to a perfect square with at least 2x2 units with at least 3x3 individuals
	 * each.
	 * 
	 * @param nHierarchy the number of hierarchy levels
	 * @return the total size of the graph
	 */
	private int calcSquareUnit(int nHierarchy) {
		int prod = 1;
		if (nHierarchy != 1 || hierarchy[0] != 1) {
			for (int i = 0; i < nHierarchy; i++) {
				int sqrt = (int) Math.sqrt(hierarchy[i]);
				int units = Math.max(4, sqrt * sqrt);
				hierarchy[i] = units;
				prod *= units;
			}
		}
		int nUnit = Math.max(9, size / prod);
		int subside = (int) Math.sqrt(nUnit);
		nUnit = subside * subside;
		connectivity = Math.min(nUnit - 1.0, connectivity);
		hierarchy[nHierarchy] = nUnit;
		return nUnit * prod;
	}

	/**
	 * {@inheritDoc}
	 * <h3>Requirements/notes:</h3>
	 * Only well-mixed (complete) or square lattice graphs are currently supported.
	 */
	@Override
	public void init() {
		if (hierarchy == null || hierarchy.length == 0)
			throw new IllegalStateException("hierarchy must be configured before initialization");
		isRewired = false;
		isRegular = false;
		isUndirected = true;

		initHierarchy(0, 0);
		isValid = true;
	}

	/**
	 * Utility method to generate hierarchical graphs.
	 *
	 * @param level the hierarchical level
	 * @param start the index of the first node to process
	 */
	private void initHierarchy(int level, int start) {
		if (level == hierarchy.length - 1) {
			int nIndiv = hierarchy[level];
			Hierarchical initializer;
			switch (subType) {
				case SQUARE_NEUMANN:
					initializer = VonNeumannGeometry::initHierarchicalVonNeumann;
					break;
				case SQUARE_NEUMANN_2ND:
					initializer = SecondNeighbourGeometry::initHierarchicalSecondNeighbour;
					break;
				case SQUARE_MOORE:
					initializer = MooreGeometry::initHierarchicalMoore;
					break;
				case SQUARE:
					initializer = SquareGeometry::initHierarchicalSquare;
					break;
				case WELLMIXED:
				case COMPLETE:
					initializer = CompleteGeometry::initHierarchicalComplete;
					break;
				default:
					throw new IllegalStateException("Unhandled sub-geometry type: " + subType);
			}
			initializer.initHierarchicalDeme(this, size, nIndiv, start, fixedBoundary);
			return;
		}
		switch (subType) {
			case SQUARE_NEUMANN:
			case SQUARE_NEUMANN_2ND:
			case SQUARE_MOORE:
			case SQUARE:
				int side = (int) Math.sqrt(size);
				int hskip = 1;
				for (int dd = level + 1; dd < hierarchy.length; dd++)
					hskip *= (int) Math.sqrt(hierarchy[dd]);
				int hside = (int) Math.sqrt(hierarchy[level]);
				for (int i = 0; i < hside; i++)
					for (int j = 0; j < hside; j++)
						initHierarchy(level + 1, start + (i + j * side) * hskip);
				break;
			case WELLMIXED:
			case COMPLETE:
			default:
				hskip = 1;
				for (int dd = level + 1; dd < hierarchy.length; dd++)
					hskip *= hierarchy[dd];
				int skip = start;
				for (int d = 0; d < hierarchy[level]; d++) {
					initHierarchy(level + 1, skip);
					skip += hskip;
				}
		}
	}

	@Override
	public boolean isUnique() {
		if (isRewired)
			return true;
		return subType.isUnique();
	}

	@Override
	public HierarchicalGeometry clone() {
		HierarchicalGeometry clone = (HierarchicalGeometry) super.clone();
		clone.subType = subType;
		if (hierarchy != null)
			clone.hierarchy = Arrays.copyOf(hierarchy, hierarchy.length);
		clone.hierarchyWeight = hierarchyWeight;
		return clone;
	}

	@Override
	public int hashCode() {
		int result = super.hashCode();
		result = 31 * result + (subType == null ? 0 : subType.hashCode());
		result = 31 * result + Arrays.hashCode(hierarchy);
		result = 31 * result + Double.hashCode(hierarchyWeight);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		HierarchicalGeometry other = (HierarchicalGeometry) obj;
		return subType == other.subType && Arrays.equals(hierarchy, other.hierarchy)
				&& Double.compare(hierarchyWeight, other.hierarchyWeight) == 0;
	}

	/**
	 * Parse the hierarchy-related CLI spec, updating subtype, boundary flag and
	 * weight, and storing the raw hierarchy levels (without the computed leaf
	 * size).
	 */
	private void parseHierarchy() {
		CLOption clo = engine.getModule().cloGeometry;
		subType = (GeometryType) clo.match(specs);
		subType = (subType == null ? GeometryType.WELLMIXED : subType);
		switch (subType) {
			case SQUARE_NEUMANN:
			case SQUARE_NEUMANN_2ND:
			case SQUARE_MOORE:
			case SQUARE:
			case WELLMIXED:
			case COMPLETE:
				break;
			default:
				warn("invalid sub-geometry for hierarchical structure: " + subType
						+ " - reset to well-mixed.");
				subType = GeometryType.WELLMIXED;
		}
		specs = CLOption.stripKey(subType, specs);
		fixedBoundary = false;
		if (!specs.isEmpty() && GeometryType.isFixedBoundaryToken(specs.charAt(0))) {
			fixedBoundary = true;
			specs = specs.substring(1);
		}
		int weightIdx = specs.lastIndexOf('w');
		String levels = specs;
		if (weightIdx >= 0) {
			hierarchyWeight = CLOParser.parseDouble(specs.substring(weightIdx + 1));
			levels = specs.substring(0, weightIdx);
		} else {
			hierarchyWeight = 0.0;
		}
		hierarchy = CLOParser.parseIntVector(levels);
		if (hierarchy == null || hierarchy.length == 0)
			hierarchy = new int[] { 1 };
	}
}
