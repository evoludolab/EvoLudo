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

import org.evoludo.simulator.EvoLudo;
import org.evoludo.util.CLOParser;
import org.evoludo.util.Formatter;

/**
 * Hierarchical meta-population structure that embeds either well-mixed or
 * square-lattice demes into recursive layers.
 */
public class HierarchicalGeometry extends AbstractLattice {

	private GeometryType subGeometry = GeometryType.WELLMIXED;
	private int[] rawHierarchy = new int[] { 1 };
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
	public boolean parse(String arg) {
		String spec = arg == null ? "" : arg.trim();
		subGeometry = GeometryType.WELLMIXED;
		fixedBoundary = false;
		spec = parseSubGeometry(spec);
		spec = stripBoundary(spec);
		int weightIdx = spec.lastIndexOf('w');
		if (weightIdx >= 0) {
			hierarchyWeight = CLOParser.parseDouble(spec.substring(weightIdx + 1));
			spec = spec.substring(0, weightIdx);
		} else {
			hierarchyWeight = 0.0;
		}
		rawHierarchy = CLOParser.parseIntVector(spec);
		if (rawHierarchy.length == 0)
			rawHierarchy = new int[] { 1 };
		return true;
	}

	@Override
	public void reset() {
		super.reset();
		subGeometry = GeometryType.WELLMIXED;
		rawHierarchy = new int[] { 1 };
		hierarchy = null;
		hierarchyWeight = 0.0;
	}

	/**
	 * @return the geometry used within each hierarchy level (e.g. square or
	 *         well-mixed)
	 */
	public GeometryType getSubGeometry() {
		return subGeometry;
	}

	/**
	 * @return the weight applied to inter-level interactions
	 */
	public double getHierarchyWeight() {
		return hierarchyWeight;
	}

	/**
	 * @return a copy of the processed hierarchy definition (levels plus
	 *         individuals per deme)
	 */
	public int[] getHierarchyLevels() {
		if (hierarchy != null)
			return Arrays.copyOf(hierarchy, hierarchy.length);
		int[] copy = Arrays.copyOf(rawHierarchy, rawHierarchy.length + 1);
		copy[copy.length - 1] = size;
		return copy;
	}

	@Override
	protected boolean checkSettings() {
		boolean reset = false;
		if (rawHierarchy == null || rawHierarchy.length == 0)
			rawHierarchy = new int[] { 1 };
		int[] processed = Arrays.copyOf(rawHierarchy, rawHierarchy.length);
		int nHierarchy = 0;
		for (int value : processed) {
			if (value > 1)
				processed[nHierarchy++] = value;
		}
		if (nHierarchy == 0) {
			warn("hierarchies must encompass ≥2 levels - collapsed to single level.");
			processed = new int[] { 1 };
			nHierarchy = 1;
		} else if (nHierarchy != rawHierarchy.length) {
			warn("hierarchy levels must include >1 units - hierarchies collapsed to " + (nHierarchy + 1) + " levels.");
			processed = Arrays.copyOf(processed, nHierarchy);
			reset = true;
		} else {
			processed = Arrays.copyOf(processed, nHierarchy);
		}

		hierarchy = new int[nHierarchy + 1];
		System.arraycopy(processed, 0, hierarchy, 0, nHierarchy);

		int prod = 1;
		double requestedConn = connectivity;
		int nIndiv;
		switch (subGeometry) {
			case SQUARE_NEUMANN_2ND:
			case SQUARE_NEUMANN:
			case SQUARE_MOORE:
			case SQUARE:
				if (nHierarchy != 1 || hierarchy[0] != 1) {
					for (int i = 0; i < nHierarchy; i++) {
						int sqrt = (int) Math.sqrt(hierarchy[i]);
						hierarchy[i] = Math.max(4, sqrt * sqrt);
						prod *= hierarchy[i];
					}
				}
				int nPerDeme = size > 0 ? Math.max(1, size / Math.max(1, prod)) : 1;
				int subside = (int) Math.sqrt(nPerDeme);
				nIndiv = Math.max(9, subside * subside);
				switch (subGeometry) {
					case SQUARE_MOORE:
						connectivity = 8;
						break;
					case SQUARE_NEUMANN:
					case SQUARE_NEUMANN_2ND:
						connectivity = Math.max(4, requestedConn > 0 ? requestedConn : 4);
						break;
					case SQUARE:
					default:
						connectivity = requestedConn > 0 ? requestedConn : 4;
						break;
				}
				break;
			case COMPLETE:
				subGeometry = GeometryType.WELLMIXED;
				//$FALL-THROUGH$
			case WELLMIXED:
			default:
				for (int i = 0; i < nHierarchy; i++)
					prod *= hierarchy[i];
				nIndiv = Math.max(2, size > 0 ? size / Math.max(1, prod) : 2);
				connectivity = Math.max(0, nIndiv - 1);
				if (subGeometry != GeometryType.WELLMIXED) {
					warn("subgeometry '" + subGeometry + "' not supported - using well-mixed demes.");
					subGeometry = GeometryType.WELLMIXED;
					reset = true;
				}
				break;
		}
		hierarchy[nHierarchy] = nIndiv;

		int requiredSize = prod * nIndiv;
		if (requiredSize <= 0)
			requiredSize = size;
		if (setSize(requiredSize)) {
			if (engine.getModule().cloNPopulation.isSet())
				warn("hierarchies " + Formatter.format(hierarchy) + " require population size " + size + "!");
			reset = true;
		}
		return reset;
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
		alloc();
		initHierarchy(0, 0);
		isValid = true;
	}

	/**
	 * Parse and remove potential sub-geometry tokens from the specification.
	 *
	 * @param spec specification string
	 * @return remainder string once sub-geometry/boundary tokens are consumed
	 */
	private String parseSubGeometry(String spec) {
		if (spec.isEmpty())
			return spec;
		if (spec.startsWith("n2")) {
			subGeometry = GeometryType.SQUARE_NEUMANN_2ND;
			return spec.substring(2);
		}
		char key = spec.charAt(0);
		switch (key) {
			case 'n':
				subGeometry = GeometryType.SQUARE_NEUMANN;
				return spec.substring(1);
			case 'm':
				subGeometry = GeometryType.SQUARE_MOORE;
				return spec.substring(1);
			case 'N':
				subGeometry = GeometryType.SQUARE;
				return spec.substring(1);
			case 'C':
			case 'c':
				subGeometry = GeometryType.COMPLETE;
				return spec.substring(1);
			case 'M':
				subGeometry = GeometryType.WELLMIXED;
				return spec.substring(1);
			default:
				return spec;
		}
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
			int end = start + nIndiv;
			switch (subGeometry) {
				case SQUARE_NEUMANN:
				case SQUARE_NEUMANN_2ND:
				case SQUARE_MOORE:
				case SQUARE:
					initHierarchySquare(start, end);
					return;
				case WELLMIXED:
				case COMPLETE:
				default:
					initHierarchyMeanfield(start, end);
					return;
			}
		}
		switch (subGeometry) {
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

	/**
	 * Utility method to generate hierarchical well-mixed subpopulations (demes).
	 *
	 * @param start the index of the first node to process
	 * @param end   the index of the last node to process
	 */
	private void initHierarchyMeanfield(int start, int end) {
		int nIndiv = end - start;
		int nIndiv1 = Math.max(0, nIndiv - 1);
		for (int n = start; n < end; n++) {
			int[] links = new int[nIndiv1];
			for (int i = 0; i < nIndiv1; i++)
				links[i] = (start + i >= n) ? start + i + 1 : start + i;
			in[n] = links;
			out[n] = links;
			kin[n] = nIndiv1;
			kout[n] = nIndiv1;
		}
	}

	/**
	 * Utility method to generate hierarchical square-lattice demes.
	 *
	 * @param start the index of the first node to process
	 * @param end   the index of the last node to process
	 */
	private void initHierarchySquare(int start, int end) {
		int nIndiv = end - start;
		int demeSide = (int) Math.sqrt(nIndiv);
		int fullSide = (int) Math.sqrt(size);
		switch (subGeometry) {
			case SQUARE_NEUMANN:
				initSquareVonNeumann(demeSide, fullSide, start);
				break;
			case SQUARE_NEUMANN_2ND:
				initSquareVonNeumann2nd(demeSide, fullSide, start);
				break;
			case SQUARE_MOORE:
				initSquareMoore(demeSide, fullSide, start);
				break;
			case SQUARE:
			default:
				initSquare(demeSide, fullSide, start);
				break;
		}
	}

	/**
	 * Initialize a square deme with von Neumann connectivity, optionally adjusting
	 * for boundary conditions.
	 *
	 * @param sideLen  side length of the deme
	 * @param fullside side length of the full lattice
	 * @param offset   index offset of the deme
	 */
	private void initSquareVonNeumann(int sideLen, int fullside, int offset) {
		boolean interspecies = isInterspecies();
		for (int i = 0; i < sideLen; i++) {
			int x = i * fullside;
			int u = ((i - 1 + sideLen) % sideLen) * fullside;
			int d = ((i + 1) % sideLen) * fullside;
			for (int j = 0; j < sideLen; j++) {
				int r = (j + 1) % sideLen;
				int l = (j - 1 + sideLen) % sideLen;
				int player = offset + x + j;
				if (interspecies)
					addLinkAt(player, player);
				addLinkAt(player, offset + u + j);
				addLinkAt(player, offset + x + r);
				addLinkAt(player, offset + d + j);
				addLinkAt(player, offset + x + l);
			}
		}
		if (fixedBoundary)
			adjustNeumannBoundaries(sideLen, fullside, offset, interspecies);
	}

	/**
	 * Adjust von Neumann neighbourhoods along fixed boundaries.
	 *
	 * @param sideLen      side length of the deme
	 * @param fullside     side length of the full lattice
	 * @param offset       index offset of the deme
	 * @param interspecies {@code true} if interspecific interactions allow
	 *                     self-links
	 */
	private void adjustNeumannBoundaries(int sideLen, int fullside, int offset, boolean interspecies) {
		int player = offset;
		clearLinksFrom(player);
		if (interspecies)
			addLinkAt(player, player);
		addLinkAt(player, player + 1);
		addLinkAt(player, player + fullside);

		player = offset + sideLen - 1;
		clearLinksFrom(player);
		if (interspecies)
			addLinkAt(player, player);
		addLinkAt(player, player - 1);
		addLinkAt(player, player + fullside);

		player = offset + (sideLen - 1) * fullside;
		clearLinksFrom(player);
		if (interspecies)
			addLinkAt(player, player);
		addLinkAt(player, player + 1);
		addLinkAt(player, player - fullside);

		player = offset + (sideLen - 1) * (fullside + 1);
		clearLinksFrom(player);
		if (interspecies)
			addLinkAt(player, player);
		addLinkAt(player, player - 1);
		addLinkAt(player, player - fullside);

		for (int i = 1; i < sideLen - 1; i++) {
			player = offset + i;
			clearLinksFrom(player);
			if (interspecies)
				addLinkAt(player, player);
			addLinkAt(player, player - 1);
			addLinkAt(player, player + 1);
			addLinkAt(player, player + fullside);

			player = offset + (sideLen - 1) * fullside + i;
			clearLinksFrom(player);
			if (interspecies)
				addLinkAt(player, player);
			addLinkAt(player, player - 1);
			addLinkAt(player, player + 1);
			addLinkAt(player, player - fullside);

			player = offset + fullside * i;
			clearLinksFrom(player);
			if (interspecies)
				addLinkAt(player, player);
			addLinkAt(player, player + 1);
			addLinkAt(player, player - fullside);
			addLinkAt(player, player + fullside);

			player = offset + fullside * i + sideLen - 1;
			clearLinksFrom(player);
			if (interspecies)
				addLinkAt(player, player);
			addLinkAt(player, player - 1);
			addLinkAt(player, player - fullside);
			addLinkAt(player, player + fullside);
		}
		isRegular = false;
	}

	/**
	 * Initialize a square deme with second-neighbour von Neumann connectivity.
	 *
	 * @param sideLen  side length of the deme
	 * @param fullside side length of the full lattice
	 * @param offset   index offset of the deme
	 */
	private void initSquareVonNeumann2nd(int sideLen, int fullside, int offset) {
		boolean interspecies = isInterspecies();
		for (int i = 0; i < sideLen; i++) {
			int x = i * fullside;
			int u = ((i - 1 + sideLen) % sideLen) * fullside;
			int d = ((i + 1) % sideLen) * fullside;
			for (int j = 0; j < sideLen; j++) {
				int r = (j + 1) % sideLen;
				int l = (j - 1 + sideLen) % sideLen;
				int player = offset + x + j;
				if (interspecies)
					addLinkAt(player, player);
				addLinkAt(player, offset + u + l);
				addLinkAt(player, offset + u + r);
				addLinkAt(player, offset + d + l);
				addLinkAt(player, offset + d + r);
			}
		}
		if (fixedBoundary)
			adjustNeumann2ndBoundaries(sideLen, fullside, offset, interspecies);
	}

	/**
	 * Adjust second-neighbour von Neumann neighbourhoods along fixed boundaries.
	 *
	 * @param sideLen      side length of the deme
	 * @param fullside     side length of the full lattice
	 * @param offset       index offset of the deme
	 * @param interspecies {@code true} if interspecific interactions allow
	 *                     self-links
	 */
	private void adjustNeumann2ndBoundaries(int sideLen, int fullside, int offset, boolean interspecies) {
		int player = offset;
		clearLinksFrom(player);
		if (interspecies)
			addLinkAt(player, player);
		addLinkAt(player, player + fullside + 1);

		player = offset + sideLen - 1;
		clearLinksFrom(player);
		if (interspecies)
			addLinkAt(player, player);
		addLinkAt(player, player + fullside - 1);

		player = offset + (sideLen - 1) * fullside;
		clearLinksFrom(player);
		if (interspecies)
			addLinkAt(player, player);
		addLinkAt(player, player - fullside + 1);

		player = offset + (sideLen - 1) * (fullside + 1);
		clearLinksFrom(player);
		if (interspecies)
			addLinkAt(player, player);
		addLinkAt(player, player - fullside - 1);

		for (int i = 1; i < sideLen - 1; i++) {
			player = offset + i;
			clearLinksFrom(player);
			if (interspecies)
				addLinkAt(player, player);
			addLinkAt(player, player + fullside - 1);
			addLinkAt(player, player + fullside + 1);

			player = offset + (sideLen - 1) * fullside + i;
			clearLinksFrom(player);
			if (interspecies)
				addLinkAt(player, player);
			addLinkAt(player, player - fullside - 1);
			addLinkAt(player, player - fullside + 1);

			player = offset + fullside * i;
			clearLinksFrom(player);
			if (interspecies)
				addLinkAt(player, player);
			addLinkAt(player, player - fullside + 1);
			addLinkAt(player, player + fullside + 1);

			player = offset + fullside * i + sideLen - 1;
			clearLinksFrom(player);
			if (interspecies)
				addLinkAt(player, player);
			addLinkAt(player, player - fullside - 1);
			addLinkAt(player, player + fullside - 1);
		}
		isRegular = false;
	}

	/**
	 * Initialize a square deme with Moore connectivity.
	 *
	 * @param sideLen  side length of the deme
	 * @param fullside side length of the full lattice
	 * @param offset   index offset of the deme
	 */
	private void initSquareMoore(int sideLen, int fullside, int offset) {
		boolean interspecies = isInterspecies();
		for (int i = 0; i < sideLen; i++) {
			int x = i * fullside;
			int u = ((i - 1 + sideLen) % sideLen) * fullside;
			int d = ((i + 1) % sideLen) * fullside;
			for (int j = 0; j < sideLen; j++) {
				int r = (j + 1) % sideLen;
				int l = (j - 1 + sideLen) % sideLen;
				int player = offset + x + j;
				if (interspecies)
					addLinkAt(player, player);
				addLinkAt(player, offset + u + j);
				addLinkAt(player, offset + u + r);
				addLinkAt(player, offset + x + r);
				addLinkAt(player, offset + d + r);
				addLinkAt(player, offset + d + j);
				addLinkAt(player, offset + d + l);
				addLinkAt(player, offset + x + l);
				addLinkAt(player, offset + u + l);
			}
		}
		if (fixedBoundary)
			adjustMooreBoundaries(sideLen, fullside, offset, interspecies);
	}

	/**
	 * Adjust Moore neighbourhoods along fixed boundaries.
	 *
	 * @param sideLen      side length of the deme
	 * @param fullside     side length of the full lattice
	 * @param offset       index offset of the deme
	 * @param interspecies {@code true} if interspecific interactions allow
	 *                     self-links
	 */
	private void adjustMooreBoundaries(int sideLen, int fullside, int offset, boolean interspecies) {
		int player = offset;
		clearLinksFrom(player);
		if (interspecies)
			addLinkAt(player, player);
		addLinkAt(player, player + 1);
		addLinkAt(player, player + fullside);
		addLinkAt(player, player + fullside + 1);

		player = offset + sideLen - 1;
		clearLinksFrom(player);
		if (interspecies)
			addLinkAt(player, player);
		addLinkAt(player, player - 1);
		addLinkAt(player, player + fullside);
		addLinkAt(player, player + fullside - 1);

		player = offset + (sideLen - 1) * fullside;
		clearLinksFrom(player);
		if (interspecies)
			addLinkAt(player, player);
		addLinkAt(player, player + 1);
		addLinkAt(player, player - fullside);
		addLinkAt(player, player - fullside + 1);

		player = offset + (sideLen - 1) * (fullside + 1);
		clearLinksFrom(player);
		if (interspecies)
			addLinkAt(player, player);
		addLinkAt(player, player - 1);
		addLinkAt(player, player - fullside);
		addLinkAt(player, player - fullside - 1);

		for (int i = 1; i < sideLen - 1; i++) {
			player = offset + i;
			clearLinksFrom(player);
			if (interspecies)
				addLinkAt(player, player);
			addLinkAt(player, player - 1);
			addLinkAt(player, player + 1);
			addLinkAt(player, player + fullside);
			addLinkAt(player, player + fullside - 1);
			addLinkAt(player, player + fullside + 1);

			player = offset + (sideLen - 1) * fullside + i;
			clearLinksFrom(player);
			if (interspecies)
				addLinkAt(player, player);
			addLinkAt(player, player - 1);
			addLinkAt(player, player + 1);
			addLinkAt(player, player - fullside);
			addLinkAt(player, player - fullside - 1);
			addLinkAt(player, player - fullside + 1);

			player = offset + fullside * i;
			clearLinksFrom(player);
			if (interspecies)
				addLinkAt(player, player);
			addLinkAt(player, player + 1);
			addLinkAt(player, player - fullside);
			addLinkAt(player, player + fullside);
			addLinkAt(player, player - fullside + 1);
			addLinkAt(player, player + fullside + 1);

			player = offset + fullside * i + sideLen - 1;
			clearLinksFrom(player);
			if (interspecies)
				addLinkAt(player, player);
			addLinkAt(player, player - 1);
			addLinkAt(player, player - fullside);
			addLinkAt(player, player + fullside);
			addLinkAt(player, player - fullside - 1);
			addLinkAt(player, player + fullside - 1);
		}
		isRegular = false;
	}

	/**
	 * Initialize a square deme for arbitrary (odd) neighbourhood sizes.
	 *
	 * @param sideLen  side length of the deme
	 * @param fullside side length of the full lattice
	 * @param offset   index offset of the deme
	 */
	private void initSquare(int sideLen, int fullside, int offset) {
		boolean interspecies = isInterspecies();
		int range = Math.min(sideLen / 2, Math.max(1, (int) (Math.sqrt(connectivity + 1.5) / 2.0)));
		for (int i = 0; i < sideLen; i++) {
			int x = i * fullside;
			for (int j = 0; j < sideLen; j++) {
				int player = offset + x + j;
				for (int u = i - range; u <= i + range; u++) {
					int row = offset + ((u + sideLen) % sideLen) * fullside;
					for (int v = j - range; v <= j + range; v++) {
						int neighbor = row + (v + sideLen) % sideLen;
						if (player == neighbor && !interspecies)
							continue;
						addLinkAt(player, neighbor);
					}
				}
			}
		}
		if (fixedBoundary)
			adjustSquareBoundaries(sideLen, fullside, offset, interspecies, range);
	}

	/**
	 * Adjust arbitrary-range square neighbourhoods along fixed boundaries.
	 *
	 * @param sideLen      side length of the deme
	 * @param fullside     side length of the full lattice
	 * @param offset       index offset of the deme
	 * @param interspecies {@code true} if interspecific interactions allow
	 *                     self-links
	 * @param range        neighbour range
	 */
	private void adjustSquareBoundaries(int sideLen, int fullside, int offset, boolean interspecies, int range) {
		int player = offset;
		clearLinksFrom(player);
		for (int u = 0; u <= range; u++) {
			int r = player + u * fullside;
			for (int v = 0; v <= range; v++) {
				int neighbor = r + v;
				if (player == neighbor && !interspecies)
					continue;
				addLinkAt(player, neighbor);
			}
		}
		player = offset + sideLen - 1;
		clearLinksFrom(player);
		for (int u = 0; u <= range; u++) {
			int r = player + u * fullside;
			for (int v = 0; v <= range; v++) {
				int neighbor = r - v;
				if (player == neighbor && !interspecies)
					continue;
				addLinkAt(player, neighbor);
			}
		}
		player = offset + (sideLen - 1) * fullside;
		clearLinksFrom(player);
		for (int u = 0; u <= range; u++) {
			int r = player - u * fullside;
			for (int v = 0; v <= range; v++) {
				int neighbor = r + v;
				if (player == neighbor && !interspecies)
					continue;
				addLinkAt(player, neighbor);
			}
		}
		player = offset + (sideLen - 1) * (fullside + 1);
		clearLinksFrom(player);
		for (int u = 0; u <= range; u++) {
			int r = player - u * fullside;
			for (int v = 0; v <= range; v++) {
				int neighbor = r - v;
				if (player == neighbor && !interspecies)
					continue;
				addLinkAt(player, neighbor);
			}
		}
		isRegular = false;
	}

	@Override
	public boolean isUnique() {
		if (isRewired)
			return true;
		return subGeometry.isUnique();
	}

	@Override
	public HierarchicalGeometry clone() {
		HierarchicalGeometry clone = (HierarchicalGeometry) super.clone();
		if (rawHierarchy != null)
			clone.rawHierarchy = Arrays.copyOf(rawHierarchy, rawHierarchy.length);
		if (hierarchy != null)
			clone.hierarchy = Arrays.copyOf(hierarchy, hierarchy.length);
		clone.hierarchyWeight = hierarchyWeight;
		return clone;
	}
}
