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

package org.evoludo.simulator.models;

import org.evoludo.math.RNGDistribution;
import org.evoludo.simulator.EvoLudo;
import org.evoludo.simulator.Geometry;
import org.evoludo.util.CLOption;

/**
 * Interaction and reference groups in IBS models.
 * 
 * @author Christoph Hauert
 */
public class IBSGroup {

	/**
	 * The empty neighbourhood of a lone individual.
	 */
	private static int[] loner = new int[0];

	/**
	 * The geometry associated with this group.
	 */
	Geometry geometry;

	/**
	 * Storage for selected interaction or reference groups.
	 */
	private int[] mem;

	/**
	 * Reference to the indices of the members of the group.
	 */
	int[] group;

	/**
	 * The index of the focal individual.
	 */
	int focal;

	/**
	 * The shared random number generator to ensure reproducibility of results.
	 * 
	 * @see org.evoludo.simulator.EvoLudo#getRNG()
	 */
	protected RNGDistribution rng;

	/**
	 * Create a new interaction or competition group. The global, shared random
	 * number generator {@code rng} must be passed here to ensure reproducibility of
	 * the simulation results.
	 * <p>
	 * <strong>Note:</strong> The default (empty) constructor does not need to be
	 * declared private in order to prevent instantiation. The compiler does not
	 * generate a default constructor if another one exists.
	 * 
	 * @param rng the random number generator for picking interaction or reference
	 *            groups
	 */
	public IBSGroup(RNGDistribution rng) {
		this.rng = rng;
		self = false;
	}

	/**
	 * Set the geometry associated with this group.
	 * 
	 * @param geometry the geometry associated with group
	 */
	public void setGeometry(Geometry geometry) {
		this.geometry = geometry;
	}

	/**
	 * The current sampling type for the formation of interaction or reference
	 * groups.
	 */
	SamplingType samplingType = SamplingType.ALL;

	/**
	 * Sets the type of sampling of interaction/reference groups to {@code type}.
	 * 
	 * @param type the type of sampling
	 */
	public void setSampling(SamplingType type) {
		samplingType = type;
	}

	/**
	 * Gets the type of sampling of interaction/reference groups.
	 * 
	 * @return the type of sampling
	 */
	public SamplingType getSampling() {
		return samplingType;
	}

	/**
	 * The sample size requested. Default is a single sample for RANDOM sampling.
	 */
	int nSamples = 1;

	/**
	 * The effective sample size. {@code nSampled &lt; nSamples} may hold depending
	 * on the population structure.
	 */
	int nSampled = 1;

	/**
	 * Get the number of interactions with random neighbours.
	 * 
	 * @return the number of interactions
	 */
	public int getNSamples() {
		return nSamples;
	}

	/**
	 * Set the number of interactions with random neighbours.
	 * 
	 * @param nSamples the number of interactions
	 */
	public void setNSamples(int nSamples) {
		if (nSamples <= 0)
			return;
		this.nSamples = nSamples;
		if (mem == null || mem.length < nSamples)
			mem = new int[nSamples];
	}

	/**
	 * Checks whether the type of sampling of interaction/reference groups is
	 * {@code type}.
	 * 
	 * @param type the sampling type to check
	 * @return {@code true} if the type of sampling is {@code type}
	 */
	public boolean isSampling(SamplingType type) {
		return (type == samplingType);
	}

	/**
	 * Get the number of interactions with random neighbours.
	 * 
	 * @return the number of interactions
	 */
	public int getNSampled() {
		return nSampled;
	}

	/**
	 * The flag indicating whether to include the focal individual when sampling.
	 * 
	 * <h3>Notes</h3>
	 * In the original Moran process the offspring can replace the parent. Hence,
	 * for reference groups in well-mixed populations it makes sense to include
	 * the focal individual. In contrast, self-interactions may simplify some
	 * analytical considerations but in IBS never make sense.
	 */
	boolean self;

	/**
	 * Sets whether random sampling should include the focal individual or not.
	 * 
	 * @param self {@code true} if sampling includes focal
	 */
	public void setSelf(boolean self) {
		this.self = self;
	}

	/**
	 * Gets whether random sampling includes the focal individual or not.
	 * 
	 * @return {@code true} if sampling includes focal
	 */
	public boolean getSelf() {
		return self;
	}

	/**
	 * Sets the interaction/reference group to {@code group} with a size
	 * {@code size} and the focal indiviual {@code focal}.
	 * 
	 * @param focal the index of the focal individual
	 * @param group the array with the indices of the members of the group
	 * @param size  the size of the group
	 */
	public void setGroupAt(int focal, int[] group, int size) {
		this.focal = focal;
		if (group == null) {
			this.group = loner;
			nSampled = 0;
			return;
		}
		this.group = group;
		nSampled = size;
	}

	/**
	 * Gets the array of indices of the interaction/reference group members.
	 * 
	 * @return the interaction/reference group
	 */
	public int[] getGroup() {
		return group;
	}

	/**
	 * Gets the index of the focal individual.
	 * 
	 * @return the index of the focal individual
	 */
	public int getFocal() {
		return focal;
	}

	private static final int[] empty = new int[0];

	/**
	 * Picks an interaction/reference group for the focal individual {@code focal}
	 * and the population structure {@code geom}. If the flag {@code out == true}
	 * then the group is sampled from the outgoing neighbours (downstream) of the
	 * {@code focal} and from the incoming neighbours (upstream) otherwise.
	 * <p>
	 * <strong>Important:</strong> For efficiency the sampled group may be a direct
	 * reference to the population structure. For example,
	 * {@code group = out[focal]} to reference all neighbours of the focal
	 * individual. As a consequence the array group is untouchable and must
	 * <em>never</em> be manipulated because this would result in permanent changes
	 * of the population structure.
	 * 
	 * @param me         the index of the focal individual
	 * @param downstream the flag indicating whether to sample the group from the
	 *                   outgoing (downstream) or incoming (upstream) neighbours
	 * @return the interaction/reference group (same as {@code group})
	 * 
	 * @see #group
	 */
	public int[] pickAt(int me, boolean downstream) {
		focal = me;
		switch (samplingType) {
			case NONE: // speeds things up e.g. for best-response in well-mixed populations
				// XXX if nSampled == 0 updatePlayerAt aborts if no references found...
				// pretend...
				nSampled = 1;
				return empty;

			case ALL:
				if (downstream) {
					group = geometry.out[focal];
					nSampled = geometry.kout[focal];
					return group;
				}
				group = geometry.in[focal];
				nSampled = geometry.kin[focal];
				return group;

			case RANDOM:
				switch (geometry.getType()) {
					case WELLMIXED:
						return pickRandom(geometry.getSize());

					case HIERARCHY:
						return pickRandomHierarchy(downstream);

					default:
						return pickRandomStructured(downstream);
				}
			default:
				throw new UnsupportedOperationException("Unknown group sampling (type: " + samplingType + ")!");
		}
	}

	/**
	 * Pick group of {@code nSamples} random individual with indices
	 * {@code 0 - (size-1)}. The focal individual is included if {@code self==true}.
	 * 
	 * @param size the maximum index to pick
	 * @return the picked group
	 */
	private int[] pickRandom(int size) {
		group = mem;
		nSampled = Math.min(nSamples, size);

		if (nSampled == 1) {
			pickSingle(size);
			return group;
		}

		if (self)
			pickGroup(size);
		else
			pickGroup(size, focal);
		return group;
	}

	/**
	 * Pick group of {@code nSamples} random individual in structured populations
	 * either from the outgoing (downstream) or incoming (upstream) neighbours.
	 * 
	 * @param downstream the flag to indicating sampling from ownstream
	 * @return the picked group
	 */
	private int[] pickRandomStructured(boolean downstream) {
		int[] src = (downstream ? geometry.out[focal] : geometry.in[focal]);
		int len = (downstream ? geometry.kout[focal] : geometry.kin[focal]);
		if (len <= nSamples) {
			nSampled = len;
			group = src;
			return group;
		}
		group = mem;
		nSampled = nSamples;
		if (nSamples == 1) {
			// optimization: single reference is commonly used and saves copying of all
			// neighbors.
			group[0] = src[rng.random0n(len)];
			return group;
		}
		// make sure memory is sufficient for picking
		if (group.length < len) {
			mem = new int[len];
			group = mem;
		}
		System.arraycopy(src, 0, group, 0, len);
		if (nSamples > len / 2) {
			for (int n = 0; n < len - nSamples; n++) {
				int aRand = rng.random0n(len - n);
				group[aRand] = group[len - n - 1];
			}
			return group;
		}
		for (int n = 0; n < nSamples; n++) {
			int aRand = rng.random0n(len - n) + n;
			int swap = group[n];
			group[n] = group[aRand];
			group[aRand] = swap;
		}
		return group;
	}

	/**
	 * Pick group of {@code nSamples} random individual in hierarchical structures
	 * either from the outgoing (downstream) or incoming (upstream) neighbours.
	 * 
	 * @param downstream the flag to indicating sampling from ownstream
	 * @return the picked group
	 */
	private int[] pickRandomHierarchy(boolean downstream) {
		if (nSamples != 1) {
			throw new UnsupportedOperationException(
					"sampling of groups (≥2) in hierarchical structures not (yet) implemented!");
		}
		nSampled = 1;

		HierarchyUnit hu = new HierarchyUnit();
		calcHierarchyUnit(hu);
		group = mem;
		switch (geometry.subgeometry) {
			case WELLMIXED:
				return pickHierarchyMean(hu, downstream);

			case SQUARE_NEUMANN:
			case SQUARE_NEUMANN_2ND:
			case SQUARE_MOORE:
			case SQUARE:
				return pickHierarchySquare(hu, downstream);

			default:
				throw new UnsupportedOperationException(
						"hierachy geometry '" + geometry.subgeometry.getTitle() + "' not supported");
		}
	}

	/**
	 * Internal class to store hierarchy unit information.
	 */
	private class HierarchyUnit {
		int level = 0;
		int maxLevel = geometry.hierarchy.length - 1;
		int unitSize = geometry.hierarchy[maxLevel];
		int levelSize = 1;
		int exclSize = 1;
		double prob = geometry.hierarchyweight;
	}

	/**
	 * Calculate hierarchy unit information.
	 * 
	 * @param hu the hierarchy unit
	 */
	private void calcHierarchyUnit(HierarchyUnit hu) {
		if (hu.prob <= 0.0) {
			// with zero hierarchyweight levelSize is unitSize
			hu.levelSize = hu.unitSize;
			return;
		}

		if ((int) Math.rint(geometry.getConnectivity()) == hu.unitSize - 1) {
			// if individuals are connected to all other members of the unit one hierarchy
			// level is lost.
			// this applies to well-mixed units as well as e.g. square lattices with moore
			// neighbourhood and 3x3 units.
			hu.maxLevel--;
			hu.levelSize = hu.unitSize;
		}
		double rand = rng.random01();
		while (rand < hu.prob && hu.level <= hu.maxLevel) {
			hu.exclSize = hu.levelSize;
			hu.levelSize *= geometry.hierarchy[hu.maxLevel - hu.level];
			hu.level++;
			hu.prob *= geometry.hierarchyweight;
		}
	}

	/**
	 * Pick a single random individual in hierarchical mean-field structure either
	 * from the outgoing (downstream) or incoming (upstream) neighbours.
	 * 
	 * @param hu         the hierarchy unit
	 * @param downstream the flag to indicating sampling from ownstream
	 * @return the picked group
	 */
	private int[] pickHierarchyMean(HierarchyUnit hu, boolean downstream) {
		if (hu.level == 0) {
			// pick random neighbour
			if (downstream)
				group[0] = geometry.out[focal][rng.random0n(geometry.kout[focal])];
			else
				group[0] = geometry.in[focal][rng.random0n(geometry.kin[focal])];
			return group;
		}
		// determine start of level
		int levelStart = (focal / hu.levelSize) * hu.levelSize;
		// determine start of exclude level
		int exclStart = (focal / hu.exclSize) * hu.exclSize; // relative to level
		// pick random individual in level, excluding focal unit
		int model = levelStart + rng.random0n(hu.levelSize - hu.exclSize);
		if (model >= exclStart)
			model += hu.exclSize;
		group[0] = model;
		return group;
	}

	/**
	 * Pick a single random individual in hierarchical square lattice structure
	 * either
	 * from the outgoing (downstream) or incoming (upstream) neighbours.
	 * 
	 * @param hu         the hierarchy unit
	 * @param downstream the flag to indicating sampling from ownstream
	 * @return the picked group
	 */
	private int[] pickHierarchySquare(HierarchyUnit hu, boolean downstream) {
		if (hu.level == 0) {
			// pick random neighbour
			if (downstream)
				group[0] = geometry.out[focal][rng.random0n(geometry.kout[focal])];
			else
				group[0] = geometry.in[focal][rng.random0n(geometry.kin[focal])];
			return group;
		}
		// determine start of focal level
		int side = (int) Math.sqrt(geometry.getSize());
		int levelSide = (int) Math.sqrt(hu.levelSize);
		int levelX = ((focal % side) / levelSide) * levelSide;
		int levelY = ((focal / side) / levelSide) * levelSide;
		int levelStart = levelY * side + levelX;
		// determine start of excluded level (relative to focal level)
		int exclSide = (int) Math.sqrt(hu.exclSize);
		int exclX = ((focal % side) / exclSide) * exclSide;
		int exclY = ((focal / side) / exclSide) * exclSide;
		int exclStart = (exclY - levelY) * levelSide + exclX - levelX;
		// draw random individual in focal level, excluding lower level
		int model = rng.random0n(hu.levelSize - hu.exclSize);
		for (int i = 0; i < exclSide; i++) {
			if (model < exclStart)
				break;
			model += exclSide;
			exclStart += levelSide;
		}
		// model now relative to levelStart. transform to population level
		int modelX = model % levelSide;
		int modelY = model / levelSide;
		model = levelStart + modelY * side + modelX;
		group[0] = model;
		return group;
	}

	/**
	 * Pick a single random individual with indices {@code 0 - (size-1)}. The focal
	 * individual is included if {@code self==true}.
	 * 
	 * @param size the upper bound of indices to pick (excluding)
	 */
	private void pickSingle(int size) {
		if (self) {
			group[0] = rng.random0n(size);
			return;
		}
		int aPick = rng.random0n(size - 1);
		if (aPick >= focal)
			aPick++;
		group[0] = aPick;
	}

	/**
	 * Pick group of {@code nSampled} random individuals with indices
	 * {@code 0 - (size-1)}. The focal individual is included.
	 * 
	 * @param size the upper bound of indices to pick (excluding)
	 */
	void pickGroup(int size) {
		int n = 0;
		while (n < nSampled) {
			int aPick = rng.random0n(size);
			// sample without replacement
			boolean duplicate = false;
			for (int i = 0; i < n; i++)
				if (group[i] == aPick) {
					duplicate = true;
					break;
				}
			if (duplicate)
				continue;
			group[n++] = aPick;
		}
	}

	/**
	 * Pick group of {@code nSampled} random individuals with indices
	 * {@code 0 - (size-1)}. The focal individual is excluded.
	 * 
	 * @param size  the upper bound of indices to pick (excluding)
	 * @param focal the index of the individual to exclude
	 */
	void pickGroup(int size, int focal) {
		// exclude focal
		int max1 = size - 1;
		int n = 0;
		while (n < nSampled) {
			int aPick = rng.random0n(max1);
			if (aPick >= focal)
				aPick++;
			// sample without replacement
			boolean duplicate = false;
			for (int i = 0; i < n; i++)
				if (group[i] == aPick) {
					duplicate = true;
					break;
				}
			if (duplicate)
				continue;
			group[n++] = aPick;
		}
	}

	/**
	 * Types of sampling of groups for interactions or references:
	 * <dl>
	 * <dt>none</dt>
	 * <dd>no interactions</dd>
	 * <dt>all</dt>
	 * <dd>interact with all neighbours</dd>
	 * <dt>random</dt>
	 * <dd>interact with n random neighbours</dd>
	 * </dl>
	 * 
	 * @see org.evoludo.simulator.models.IBS#cloInteractions
	 * @see org.evoludo.simulator.models.IBS#cloReferences
	 */
	public enum SamplingType implements CLOption.Key {
		/**
		 * No sampling. Not user selectable.
		 */
		NONE("-none", "no samples"),

		/**
		 * Sample all neighbours.
		 */
		ALL("all", "all neighbours"),

		/**
		 * Sample some random neighbours.
		 */
		RANDOM("random", "sample n random neighbours");

		/**
		 * Key of interaction type. Used for parsing command line options.
		 * 
		 * @see org.evoludo.simulator.models.IBS#cloInteractions
		 *      IBS.cloInteractions
		 */
		String key;

		/**
		 * Brief description of interaction type for GUI and help display.
		 * 
		 * @see EvoLudo#getCLOHelp()
		 */
		String title;

		/**
		 * Instantiate new type of interactions.
		 * 
		 * @param key   identifier for parsing of command line option
		 * @param title the summary of interactions for GUI and help display
		 */
		SamplingType(String key, String title) {
			this.key = key;
			this.title = title;
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
		public String toString() {
			return key + ": " + title;
		}
	}

	// /**
	// *
	// * @param idx
	// * @param geom
	// */
	// public void pickFitAt(int idx, Geometry geom) {
	// pickFitAt(idx, geom, useOut);
	// }

	// /**
	// *
	// * @param focal
	// * @param geom
	// * @param out
	// */
	// public void pickFitAt(int me, Geometry geom, boolean out) {
	// focal = me;
	// // IMPORTANT: setting group=src saves copying of 'src' but requires that
	// 'group' is NEVER manipulated
	// switch( samplingType ) {
	// case SAMPLING_NONE: // speeds things up e.g. for best-response in well-mixed
	// populations
	//// size = 0;
	//// if size==0 then updatePlayerAt aborts because no references found...
	// pretend we have one.
	// size = 1;
	// return;
	//
	// case SAMPLING_ALL:
	// if( out ) {
	// group = geom.out[focal];
	// size = geom.kout[focal];
	// return;
	// }
	// group = geom.in[focal];
	// size = geom.kin[focal];
	// return;
	//
	// case SAMPLING_COUNT:
	// switch( geom.geometry ) {
	// case Geometry.MEANFIELD:
	// pickRandom(focal, geom.size);
	// break;
	//
	// case Geometry.HIERARCHY:
	// if( defaultsize!=1 ) {
	// throw new Error("sampling of groups (≥2) in hierarchical structures not (yet)
	// implemented!");
	// }
	//
	// int level = 0;
	// int maxLevel = geom.hierarchy.length-1;
	// int unitSize = geom.hierarchy[maxLevel];
	// int levelSize = 1;
	// int exclSize = 1;
	// double prob = geom.hierarchyweight;
	// if( prob>0.0 ) {
	// if( (int)Math.rint(geom.connectivity)==unitSize-1 ) {
	// // if individuals are connected to all other members of the unit one
	// hierarchy level is lost.
	// // this applies to well-mixed units as well as e.g. square lattices with
	// moore neighbourhood and 3x3 units.
	// maxLevel--;
	// levelSize = unitSize;
	// }
	// double rand = rng.random01();
	// while( rand<prob && level<=maxLevel ) {
	// exclSize = levelSize;
	// levelSize *= geom.hierarchy[maxLevel-level];
	// level++;
	// prob *= geom.hierarchyweight;
	// }
	// }
	// group = mem;
	// int model;
	// int levelStart, exclStart;
	// switch( geom.subgeometry ) {
	// case Geometry.MEANFIELD:
	// // determine start of level
	// levelStart = (focal/levelSize)*levelSize;
	// // determine start of exclude level
	// exclStart = (focal/exclSize)*exclSize; // relative to level
	// // pick random individual in level, excluding focal unit
	// model = levelStart+rng.random0n(levelSize-exclSize);
	// if( model>=exclStart ) model += exclSize;
	// group[0] = model;
	// return;
	//
	// case Geometry.SQUARE:
	// if( level==0 ) {
	// // pick random neighbour
	// if( out )
	// group[0] = geom.out[focal][rng.random0n(geom.kout[focal])];
	// else
	// group[0] = geom.in[focal][rng.random0n(geom.kin[focal])];
	// return;
	// }
	// // determine start of focal level
	// int side = (int)Math.sqrt(geom.size);
	// int levelSide = (int)Math.sqrt(levelSize);
	// int levelX = ((focal%side)/levelSide)*levelSide;
	// int levelY = ((focal/side)/levelSide)*levelSide;
	// levelStart = levelY*side+levelX;
	// // determine start of excluded level (relative to focal level)
	// int exclSide = (int)Math.sqrt(exclSize);
	// int exclX = ((focal%side)/exclSide)*exclSide;
	// int exclY = ((focal/side)/exclSide)*exclSide;
	// exclStart = (exclY-levelY)*levelSide+exclX-levelX;
	// // draw random individual in focal level, excluding lower level
	// model = rng.random0n(levelSize-exclSize);
	// for( int i=0; i<exclSide; i++ ) {
	// if( model<exclStart ) break;
	// model += exclSide;
	// exclStart += levelSide;
	// }
	// // model now relative to levelStart. transform to population level
	// int modelX = model%levelSide;
	// int modelY = model/levelSide;
	// model = levelStart+modelY*side+modelX;
	// group[0] = model;
	// return;
	//
	// default:
	// throw new Error("hierachy geometry '"+(char)geom.subgeometry+"' not
	// supported");
	// }
	//
	// default:
	// int[] src = (out?geom.out[focal]:geom.in[focal]);
	// int len = (out?geom.kout[focal]:geom.kin[focal]);
	// if( len<=defaultsize ) {
	// group = src;
	// size = len;
	// return;
	// }
	// group = mem;
	// size = defaultsize;
	// if( size==1 ) {
	// // optimization: single reference is commonly used and saves copying of all
	// neighbors.
	// group[0] = src[rng.random0n(len)];
	// return;
	// }
	// System.arraycopy(src, 0, group, 0, len);
	// if( size>len/2 ) {
	// for( int n=0; n<len-size; n++ ) {
	// int aRand = rng.random0n(len-n);
	// group[aRand] = group[len-n-1];
	// }
	// return;
	// }
	// for( int n=0; n<size; n++ ) {
	// int aRand = rng.random0n(len-n)+n;
	// int swap = group[n];
	// group[n] = group[aRand];
	// group[aRand] = swap;
	// }
	// }
	// return;
	// default:
	// throw new Error("Unknown group sampling (type: "+samplingType+")!");
	// }
	// }

	// /**
	// * Pick a group of <code>size</code> random individuals with indices ranging
	// * from <code>0</code> to <code>max-1</code>. Exclude focal individual if
	// * <code>exclFocal == true</code>. The picked group is stored in
	// * <code>group</code>.
	// *
	// * @param max the maximum index to pick
	// * @param exclFocal the flag indicating whether the focal individual is
	// excluded from picking
	// */
	// private void pickRandom(int max, boolean exclFocal) {
	// group = mem;
	// size = defaultsize;

	// if (size == 1) {
	// group[0] = pick(max, exclFocal);
	// return;
	// }

	// int n = 0;
	// nextpick: while (n < size) {
	// int aPick = pick(max, exclFocal);
	// for (int i = 0; i < n; i++)
	// if (group[i] == aPick)
	// continue nextpick;
	// group[n++] = aPick;
	// }
	// }

	// /**
	// * Pick a single random individual with index <code>0</code> through
	// * <code>max-1</code>, excluding focal individual if
	// * <code>exclFocal == true</code>.
	// *
	// * @param max
	// * @param exclFocal
	// * @return index of randomly picked individual
	// */
	// private int pick(int max, boolean exclFocal) {
	// if (exclFocal) {
	// // exclude focal individual
	// int aPick = rng.random0n(max - 1);
	// if (aPick >= focal)
	// aPick++;
	// return aPick;
	// }
	// return rng.random0n(max);
	// }

	// /**
	// * pick random individual with index <code>0</code> through <code>max-1</code>
	// including focal individual.
	// *
	// * @param max
	// */
	// private void pickRandom(int max) {
	// pickRandom(max, false);
	// }
}
