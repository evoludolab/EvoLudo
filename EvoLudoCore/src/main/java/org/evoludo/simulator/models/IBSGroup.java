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
	 * Create a new interaction or reproduction group. The global, shared random
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
	}

	/**
	 * Allocate memory that is sufficient to accomodate groups of a maximum size
	 * {@code maxsize}.
	 * 
	 * @param maxsize the maximum group size
	 * 
	 * @see IBSPopulation#check()
	 */
	public void alloc(int maxsize) {
		if (mem == null || mem.length != maxsize) {
			mem = new int[maxsize];
			group = mem;
		}
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
	public double getNSamples() {
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
	public double getNSampled() {
		return nSampled;
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
	 * @param me   the index of the focal individual
	 * @param geom the geometry of the population
	 * @param out  the flag indicating whether to sample the group from the outgoing
	 *             or incoming neighbours
	 * @return the interaction/reference group (same as {@code group})
	 * 
	 * @see #group
	 */
	public int[] pickAt(int me, Geometry geom, boolean out) {
		focal = me;
		switch (samplingType) {
			case NONE: // speeds things up e.g. for best-response in well-mixed populations
				// size = 0;
				// if size==0 then updatePlayerAt aborts because no references found... pretend
				// we have one.
// size = 1;
				nSampled = 0;
				return null;

			case ALL:
				if (out) {
					group = geom.out[focal];
					nSampled = geom.kout[focal];
					return group;
				}
				group = geom.in[focal];
				nSampled = geom.kin[focal];
				return group;

			case RANDOM:
				switch (geom.getType()) {
					case MEANFIELD:
						pickRandom(geom.size);
						return group;

					case HIERARCHY:
						if (nSamples != 1) {
							throw new Error(
									"sampling of groups (≥2) in hierarchical structures not (yet) implemented!");
						}
						nSampled = 1;

						int level = 0;
						int maxLevel = geom.hierarchy.length - 1;
						int unitSize = geom.hierarchy[maxLevel];
						int levelSize = 1;
						int exclSize = 1;
						double prob = geom.hierarchyweight;
						if (prob > 0.0) {
							if ((int) Math.rint(geom.connectivity) == unitSize - 1) {
								// if individuals are connected to all other members of the unit one hierarchy
								// level is lost.
								// this applies to well-mixed units as well as e.g. square lattices with moore
								// neighbourhood and 3x3 units.
								maxLevel--;
								levelSize = unitSize;
							}
							double rand = rng.random01();
							while (rand < prob && level <= maxLevel) {
								exclSize = levelSize;
								levelSize *= geom.hierarchy[maxLevel - level];
								level++;
								prob *= geom.hierarchyweight;
							}
						}
						group = mem;
						int model;
						int levelStart, exclStart;
						switch (geom.subgeometry) {
							case MEANFIELD:
								if (level == 0) {
									// pick random neighbour
									if (out)
										group[0] = geom.out[focal][rng.random0n(geom.kout[focal])];
									else
										group[0] = geom.in[focal][rng.random0n(geom.kin[focal])];
									return group;
								}
								// with zero hierarchyweight levelSize is still 1 instead of unitSize
								levelSize = Math.max(levelSize, unitSize);
								// determine start of level
								levelStart = (focal / levelSize) * levelSize;
								// determine start of exclude level
								exclStart = (focal / exclSize) * exclSize; // relative to level
								// pick random individual in level, excluding focal unit
								model = levelStart + rng.random0n(levelSize - exclSize);
								if (model >= exclStart)
									model += exclSize;
								group[0] = model;
								return group;

							case SQUARE:
								if (level == 0) {
									// pick random neighbour
									if (out)
										group[0] = geom.out[focal][rng.random0n(geom.kout[focal])];
									else
										group[0] = geom.in[focal][rng.random0n(geom.kin[focal])];
									return group;
								}
								// determine start of focal level
								int side = (int) Math.sqrt(geom.size);
								int levelSide = (int) Math.sqrt(levelSize);
								int levelX = ((focal % side) / levelSide) * levelSide;
								int levelY = ((focal / side) / levelSide) * levelSide;
								levelStart = levelY * side + levelX;
								// determine start of excluded level (relative to focal level)
								int exclSide = (int) Math.sqrt(exclSize);
								int exclX = ((focal % side) / exclSide) * exclSide;
								int exclY = ((focal / side) / exclSide) * exclSide;
								exclStart = (exclY - levelY) * levelSide + exclX - levelX;
								// draw random individual in focal level, excluding lower level
								model = rng.random0n(levelSize - exclSize);
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

							default:
								throw new Error(
										"hierachy geometry '" + geom.subgeometry.getTitle() + "' not supported");
						}

					default:
						int[] src = (out ? geom.out[focal] : geom.in[focal]);
						int len = (out ? geom.kout[focal] : geom.kin[focal]);
						if (len <= nSamples) {
							nSampled = len;
							group = src;
							return group;
						}
						// if (len <= defaultsize) {
						// 	group = src;
						// 	size = len;
						// 	return group;
						// }
						group = mem;
						// size = defaultsize;
						nSampled = nSamples;
						if (nSamples == 1) {
							// optimization: single reference is commonly used and saves copying of all
							// neighbors.
							group[0] = src[rng.random0n(len)];
							return group;
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
			default:
				throw new Error("Unknown group sampling (type: " + samplingType + ")!");
		}
	}

	/**
	 * Pick group of {@code size} random individual with index <code>0</code>
	 * through <code>max-1</code> including focal individual.
	 * 
	 * @param max the maximum index to pick
	 */
	private void pickRandom(int max) {
		group = mem;
		nSampled = nSamples;

		if (nSamples == 1) {
			group[0] = rng.random0n(max);
			return;
		}

		int n = 0;
		nextpick: while (n < nSamples) {
			int aPick = rng.random0n(max);
			// sample without replacement
			for (int i = 0; i < n; i++)
				if (group[i] == aPick)
					continue nextpick;
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
	 * @see org.evoludo.simulator.models.IBS#cloReferenceType
	 */
	public static enum SamplingType implements CLOption.Key {
		/**
		 * No interactions.
		 */
		NONE("none", "no interactions"),

		/**
		 * Interact with all neighbours.
		 */
		ALL("all", "interact with all neighbours"),

		/**
		 * Interact with random neighbours.
		 */
		RANDOM("random", "interact with n random neighbours");

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
		 * @see EvoLudo#helpCLO()
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
