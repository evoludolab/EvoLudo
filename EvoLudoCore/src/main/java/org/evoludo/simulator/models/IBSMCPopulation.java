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

import java.util.Arrays;
import java.util.List;

import org.evoludo.math.ArrayMath;
import org.evoludo.simulator.ColorMap;
import org.evoludo.simulator.EvoLudo;
import org.evoludo.simulator.Geometry;
import org.evoludo.simulator.models.IBS.ScoringType;
import org.evoludo.simulator.models.IBSC.Init;
import org.evoludo.simulator.models.Model.HasIBS;
import org.evoludo.simulator.modules.Continuous;
import org.evoludo.simulator.modules.Mutation;
import org.evoludo.util.Formatter;
import org.evoludo.util.Plist;

/**
 * The core class for individual based simulations with <em>multiple</em>
 * continuous traits. Manages the traits of the population, while delegating the
 * management of the population and individual fitness as well as simulation
 * steps to super. Note that some further optimizations and simplifications are
 * possible in the special case of a single continuous trait, which is handled
 * in the subclass {@link IBSCPopulation}.
 * 
 * @author Christoph Hauert
 * 
 * @see IBSPopulation
 * @see IBSCPopulation
 */
public class IBSMCPopulation extends IBSPopulation {

	/**
	 * The continuous module associated with this model.
	 * <p>
	 * <strong>Note:</strong> This deliberately hides {@link IBSPopulation#module}.
	 * The two variables point to the same object but this setup avoids unnecessary
	 * casts because only {@link Continuous} modules generate
	 * {@code IBSCPopulation}(s).
	 */
	@SuppressWarnings("hiding")
	protected Continuous module;

	/**
	 * For pairwise interaction modules {@code module==pairmodule} holds and
	 * {@code null} otherwise. Convenience field to reduce the number of
	 * (unnecessary) casts.
	 * 
	 * @see HasIBS.MCPairs
	 */
	protected HasIBS.MCPairs pairmodule;

	/**
	 * For group interaction modules {@code module==groupmodule} holds and
	 * {@code null} otherwise. Convenience field to reduce the number of
	 * (unnecessary) casts.
	 * 
	 * @see HasIBS.MCGroups
	 */
	protected HasIBS.MCGroups groupmodule;

	/**
	 * The interaction partner/opponent of this population
	 * {@code opponent.getModule()==getModule().getOpponent()}. In intra-species
	 * interactions {@code opponent==this}. Convenience field.
	 * <p>
	 * <strong>Note:</strong> This deliberately hides
	 * {@link IBSPopulation#opponent}. The two variables point to the same object
	 * but this setup avoids unnecessary casts because only
	 * {@link org.evoludo.simulator.modules.Discrete Discrete} modules
	 * generate {@code IBSDPopulation}(s).
	 */
	@SuppressWarnings("hiding")
	IBSMCPopulation opponent;

	/**
	 * The mutation parameters.
	 */
	protected Mutation.Continuous mutation;

	/**
	 * Creates a population of individuals with multiple continuous traits for IBS
	 * simulations.
	 * 
	 * @param engine the pacemaker for running the model
	 * @param module the module that defines the game
	 */
	public IBSMCPopulation(EvoLudo engine, Continuous module) {
		super(engine, module);
		// deal with module cast - pairmodule and groupmodule have to wait because
		// nGroup requires parsing of command line options (see check())
		// important: cannot deal with casting shadowed opponent here because for
		// mutli-species modules all species need to be loaded first.
		this.module = module;
		mutation = module.getMutation();
		opponent = this;
		if (module instanceof HasIBS.MCPairs)
			pairmodule = (HasIBS.MCPairs) module;
		if (module instanceof HasIBS.MCGroups)
			groupmodule = (HasIBS.MCGroups) module;
		init = new Init((IBS) engine.getModel(), module.getNTraits());
	}

	@Override
	public void setOpponentPop(IBSPopulation opponent) {
		super.setOpponentPop(opponent);
		this.opponent = (IBSMCPopulation) super.opponent;
	}

	@Override
	public boolean checkConvergence() {
		// takes more than just the absence of mutations
		return false;
	}

	/**
	 * The array with the minimal values for each trait. Convenience
	 * variable to reduce calls to module.
	 * <p>
	 * <strong>Note:</strong> Internally traits are always scaled to
	 * <code>[0, 1]</code>.
	 * 
	 * @see #getTraitRangeMin()
	 */
	protected double[] traitRangeMin;

	/**
	 * Get the minima for all traits.
	 *
	 * @return the array with the trait minima
	 */
	public double[] getTraitRangeMin() {
		return traitRangeMin;
	}

	/**
	 * The array with the maximal values for each trait. Convenience
	 * variable to reduce calls to module.
	 * <p>
	 * <strong>Note:</strong> Internally traits are always scaled to
	 * <code>[0, 1]</code>.
	 * 
	 * @see #getTraitRangeMax()
	 */
	protected double[] traitRangeMax;

	/**
	 * Get the maxima for all traits.
	 *
	 * @return the array with the trait maxima
	 */
	public double[] getTraitRangeMax() {
		return traitRangeMax;
	}

	/**
	 * The array of individual traits. The traits of individual {@code i} are stored
	 * at {@code traits[i * nTraits]} through
	 * {@code traits[(i + 1) * nTraits - 1]}
	 */
	double[] traits;

	/**
	 * Get the trait of individual with index {@code idx}.
	 * 
	 * @param idx the index of the individual
	 * @return the trait of the individual
	 */
	public double getTraitAt(int idx) {
		if (nTraits > 1)
			throw new UnsupportedOperationException("use getTraitsAt(idx) instead.");
		return traits[idx];
	}

	/**
	 * Get trait {@code d} of individual with index {@code idx}.
	 * 
	 * @param idx the index of the individual
	 * @param d   the trait index
	 * @return the trait of the individual
	 */
	public double getTraitAt(int idx, int d) {
		return traits[idx * nTraits + d];
	}

	/**
	 * Set the trait of individual with index {@code idx} to {@code trait}.
	 * 
	 * @param idx   the index of the individual
	 * @param trait the trait of the individual
	 */
	public void setTraitAt(int idx, double trait) {
		if (nTraits > 1)
			throw new UnsupportedOperationException("use setTraitsAt(idx) instead.");
		traits[idx] = trait;
	}

	/**
	 * Get trait {@code d} of individual with index {@code idx}.
	 * 
	 * @param idx   the index of the individual
	 * @param d     the trait index
	 * @param trait the trait of the individual
	 * @return the trait of the individual
	 */
	public void setTraitAt(int idx, int d, double trait) {
		traits[idx * nTraits + d] = trait;
	}

	/**
	 * Get the trait array of individual with index {@code idx}. Allocates array to
	 * store the trait values.
	 * 
	 * @param idx the index of the individual
	 * @return the traits of the individual
	 */
	public double[] getTraitsAt(int idx) {
		if (nTraits == 1)
			throw new UnsupportedOperationException("use getTraitAt(idx) instead.");
		return getTraitsAt(idx, new double[nTraits]);
	}

	/**
	 * Get the trait array of individual with index {@code idx}. Stores the trait
	 * values in the array {@code idxtraits}.
	 * 
	 * @param idx       the index of the individual
	 * @param idxtraits the array for storing the traits
	 * @return the traits of the individual
	 */
	public double[] getTraitsAt(int idx, double[] idxtraits) {
		if (nTraits == 1)
			throw new UnsupportedOperationException("use getTraitAt(idx) instead.");
		System.arraycopy(traits, idx, idxtraits, 0, nTraits);
		return idxtraits;
	}

	/**
	 * Set the trait array of individual with index {@code idx} to array
	 * {@code idxtraits}.
	 * 
	 * @param idx       the index of the individual
	 * @param idxtraits the traits of the individual
	 */
	public void setTraitsAt(int idx, double[] idxtraits) {
		if (nTraits == 1)
			throw new UnsupportedOperationException("use setTraitAt(idx) instead.");
		System.arraycopy(idxtraits, 0, traits, idx, nTraits);
	}

	/**
	 * The array for temporarily storing traits during updates.
	 */
	protected double[] traitsNext;

	/**
	 * Temporary storage for traits of individuals in group interactions.
	 */
	protected double[] tmpGroup;

	/**
	 * Temporary storage for traits of individuals in small sub-group interactions.
	 */
	protected double[] smallTrait;

	/**
	 * Temporary storage for the traits of the focal individual.
	 */
	protected double[] myTrait;

	/**
	 * Temporary storage for the traits of the focal individual before
	 * the update. Used for adjusting scores.
	 */
	protected double[] oldTrait;

	/**
	 * Temporary storage for the scores of each participant prior to group
	 * interactions.
	 */
	protected double[] oldScores;

	@Override
	public void updateFromModelAt(int index, int modelPlayer) {
		super.updateFromModelAt(index, modelPlayer); // deal with tags
		System.arraycopy(traits, modelPlayer * nTraits, traitsNext, index * nTraits, nTraits);
	}

	@Override
	public boolean haveSameTrait(int a, int b) {
		int idxa = a * nTraits;
		int idxb = b * nTraits;
		for (int i = 0; i < nTraits; i++)
			if (Math.abs(traits[idxa + i] - traits[idxb + i]) > 1e-8)
				return false;
		return true;
	}

	@Override
	public boolean isSameTrait(int a) {
		int idxa = a * nTraits;
		for (int i = 0; i < nTraits; i++)
			if (Math.abs(traits[idxa + i] - traitsNext[idxa + i]) > 1e-8)
				return false;
		return true;
	}

	@Override
	public void swapTraits(int a, int b) {
		int idxa = a * nTraits;
		int idxb = b * nTraits;
		System.arraycopy(traits, idxb, traitsNext, idxa, nTraits);
		System.arraycopy(traits, idxa, traitsNext, idxb, nTraits);
	}

	@Override
	public int mutateAt(int focal) {
		updateScoreAt(focal, mutateAt(focal, false));
		return 1;
	}

	@Override
	protected boolean maybeMutateAt(int focal, boolean switched) {
		if (!mutation.doMutate())
			return switched;
		return mutateAt(focal, switched);
	}

	@Override
	protected void maybeMutateMoran(int source, int dest) {
		updateFromModelAt(dest, source);
		if (mutation.doMutate())
			mutateAt(dest, true);
		updateScoreAt(dest, true);
	}

	/**
	 * Mutate all traits of the focal individual with index {@code focal}
	 * if {@code mutate == true}. In all cases commit traits and update scores.
	 * 
	 * @param focal    the index of the focal individual that gets updated
	 * @param switched {@code true} if focal already switched trait
	 * @return {@code true} if the trait has changed
	 */
	private boolean mutateAt(int focal, boolean switched) {
		int dest = focal * nTraits;
		double[] trait = switched ? traitsNext : traits;
		for (int i = 0; i < nTraits; i++)
			traitsNext[dest + i] = mutation.mutate(trait[dest + i]);
		return true;
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * Here we introduce the convention the trait closer to {@code me} is
	 * preferred.
	 */
	@Override
	public boolean preferredPlayerBest(int me, int best, int sample) {
		double distmesample = deltaTraits(me, sample);
		if (distmesample < 1e-8)
			return true;
		double distmebest = deltaTraits(me, best);
		return (distmesample < distmebest);
	}

	/**
	 * Measure the (Cartesian) distance between traits at {@code a} and
	 * {@code b}
	 * 
	 * @param a the index where the traits of the first individual start
	 * @param b the index where the traits of the second individual start
	 * @return the distance between {@code a} and {@code b}
	 */
	private double deltaTraits(int a, int b) {
		if (nTraits == 1)
			return Math.abs(traits[a] - traits[b]);
		int idxa = a * nTraits;
		int idxb = b * nTraits;
		double dist = 0.0;
		for (int i = 0; i < nTraits; i++) {
			double d = traits[idxa + i] - traits[idxb + i];
			dist += d * d;
		}
		return Math.sqrt(dist);
	}

	/**
	 * Gather the traits of all individuals in the interaction group {@code group}.
	 * 
	 * @param group the interaction group
	 */
	private void gatherPlayers(IBSGroup group) {
		double[] opptraits = opponent.traits;
		int oppntraits = opponent.nTraits;
		for (int i = 0; i < group.nSampled; i++)
			System.arraycopy(opptraits, group.group[i] * oppntraits, tmpGroup, i * oppntraits, oppntraits);
		System.arraycopy(traits, group.focal * nTraits, myTrait, 0, nTraits);
	}

	@Override
	protected boolean doAdjustScores() {
		switch (playerScoring) {
			case RESET_ON_CHANGE:
				// if scores are reset only on an actual trait change, scores
				// can never be adjusted
			case EPHEMERAL:
				// for ephemeral scoring, scores are never adjusted
				return false;
			case RESET_ALWAYS:
			default:
				// if resetting scores after every update, scores can be adjusted
				// when interacting all neighbours but not in well-mixed populations
				if (interaction.getType() == Geometry.Type.MEANFIELD || //
						(interaction.getType() == Geometry.Type.HIERARCHY && //
								interaction.subgeometry == Geometry.Type.MEANFIELD))
					return false;
				return interGroup.isSampling(IBSGroup.SamplingType.ALL);
		}
	}

	@Override
	public void adjustScoreAt(int index, double before, double after) {
		scores[index] = after;
		updateEffScoreRange(index, before, after);
		updateFitnessAt(index);
	}

	@Override
	public void adjustScoreAt(int index, double adjust) {
		double before = scores[index];
		double after = before + adjust;
		scores[index] = after;
		updateEffScoreRange(index, before, after);
		updateFitnessAt(index);
	}

	@Override
	public void playPairGameAt(IBSGroup group) {
		int me = group.focal;
		// for ephemeral scores calculate score of focal only
		boolean ephemeralScores = playerScoring.equals(ScoringType.EPHEMERAL);
		if (group.nSampled <= 0) {
			// isolated individual
			if (ephemeralScores) {
				// no need to update scores of everyone else
				resetScoreAt(me);
				return;
			}
			updateScoreAt(me, 0.0);
			return;
		}
		gatherPlayers(group);
		double myScore = pairmodule.pairScores(myTrait, tmpGroup, group.nSampled,
				groupScores);
		if (ephemeralScores) {
			// no need to update scores of everyone else
			resetScoreAt(me);
			setScoreAt(me, myScore / group.nSampled, group.nSampled);
			return;
		}
		updateScoreAt(me, myScore, group.nSampled);
		for (int i = 0; i < group.nSampled; i++)
			opponent.updateScoreAt(group.group[i], groupScores[i]);
	}

	@Override
	public void adjustPairGameScoresAt(int me) {
		// gather players
		double[] opptraits = opponent.traits;
		int oppntraits = opponent.nTraits;
		int nIn = 0;
		int nOut = interaction.kout[me];
		int[] in = null;
		int[] out = interaction.out[me];
		for (int n = 0; n < nOut; n++)
			System.arraycopy(opptraits, out[n] * oppntraits, tmpGroup, n * oppntraits, oppntraits);
		int u2 = 2;
		if (!interaction.isUndirected) {
			// directed graph, count in-neighbors
			u2 = 1;
			nIn = interaction.kin[me];
			in = interaction.in[me];
			for (int n = 0; n < nIn; n++)
				System.arraycopy(opptraits, in[n] * oppntraits, tmpGroup, (nOut + n) * oppntraits, oppntraits);
		}
		int nInter = nOut + nIn;
		int offset = me * nTraits;
		System.arraycopy(traits, offset, oldTrait, 0, nTraits);
		System.arraycopy(traitsNext, offset, myTrait, 0, nTraits);
		double oldScore = pairmodule.pairScores(oldTrait, tmpGroup, nInter, oldScores);
		double newScore = pairmodule.pairScores(myTrait, tmpGroup, nInter, groupScores);
		commitTraitAt(me);
		if (playerScoreAveraged) {
			double iInter = 1.0 / nInter;
			newScore *= iInter;
			oldScore *= iInter;
		}
		adjustScoreAt(me, oldScore, newScore);
		for (int n = 0; n < nOut; n++) {
			int you = out[n];
			double diff = groupScores[n] - oldScores[n];
			if (playerScoreAveraged)
				diff = u2 * diff / interactions[you];
			opponent.adjustScoreAt(you, diff);
		}
		// same as !interaction.isUndirected because in != null implies directed graph
		// (see above)
		if (in != null) {
			for (int n = 0; n < nIn; n++) {
				int you = in[n];
				double diff = groupScores[nOut + n] - oldScores[nOut + n];
				if (playerScoreAveraged)
					diff = u2 * diff / interactions[you];
				opponent.adjustScoreAt(you, diff);
			}
		}
	}

	@Override
	public void playGroupGameAt(IBSGroup group) {
		int me = group.focal;
		// for ephemeral scores calculate score of focal only
		boolean ephemeralScores = playerScoring.equals(ScoringType.EPHEMERAL);
		if (group.nSampled <= 0) {
			if (ephemeralScores) {
				resetScoreAt(me);
				return;
			}
			updateScoreAt(me, 0.0);
			return;
		}
		gatherPlayers(group);

		switch (group.samplingType) {
			case ALL:
				// interact with all neighbors
				int nGroup = module.getNGroup();
				if (nGroup < group.nSampled + 1) {
					// interact with part of group sequentially
					double myScore = 0.0;
					Arrays.fill(smallScores, 0, group.nSampled, 0.0);
					for (int n = 0; n < group.nSampled; n++) {
						for (int i = 0; i < nGroup - 1; i++)
							System.arraycopy(tmpGroup, ((n + i) % group.nSampled) * nTraits, smallTrait, i * nTraits,
									nTraits);
						myScore += groupmodule.groupScores(myTrait, smallTrait, nGroup - 1, groupScores);
						if (ephemeralScores)
							continue;
						for (int i = 0; i < nGroup - 1; i++)
							smallScores[(n + i) % group.nSampled] += groupScores[i];
					}
					if (ephemeralScores) {
						resetScoreAt(me);
						setScoreAt(me, myScore / group.nSampled, group.nSampled);
						return;
					}
					updateScoreAt(me, myScore, group.nSampled);
					for (int i = 0; i < group.nSampled; i++)
						opponent.updateScoreAt(group.group[i], smallScores[i], nGroup - 1);
					return;
				}
				// interact with full group (random graphs, all neighbors)

				//$FALL-THROUGH$
			case RANDOM:
				// interact with sampled neighbors
				double myScore = groupmodule.groupScores(myTrait, tmpGroup, group.nSampled, groupScores);
				if (ephemeralScores) {
					resetScoreAt(me);
					setScoreAt(me, myScore, 1);
					return;
				}
				updateScoreAt(me, myScore);
				for (int i = 0; i < group.nSampled; i++)
					opponent.updateScoreAt(group.group[i], groupScores[i]);
				return;

			default:
				throw new Error("Unknown interaction type (" + interGroup.getSampling() + ")");
		}
	}

	@Override
	public void yalpGroupGameAt(IBSGroup group) {
		double myScore;
		gatherPlayers(group);

		int nGroup = module.getNGroup();
		if (nGroup < group.nSampled + 1) { // interact with part of group sequentially
			myScore = 0.0;
			Arrays.fill(smallScores, 0, group.nSampled, 0.0);
			for (int n = 0; n < group.nSampled; n++) {
				for (int i = 0; i < nGroup - 1; i++)
					System.arraycopy(tmpGroup, ((n + i) % group.nSampled) * nTraits, smallTrait, i * nTraits,
							nTraits);
				myScore += groupmodule.groupScores(myTrait, smallTrait, nGroup - 1, groupScores);
				for (int i = 0; i < nGroup - 1; i++)
					smallScores[(n + i) % group.nSampled] += groupScores[i];
			}
			removeScoreAt(group.focal, myScore, group.nSampled);
			for (int i = 0; i < group.nSampled; i++)
				opponent.removeScoreAt(group.group[i], smallScores[i], nGroup - 1);
			return;
		}
		// interact with full group (random graphs)
		myScore = groupmodule.groupScores(myTrait, tmpGroup, group.nSampled, groupScores);
		removeScoreAt(group.focal, myScore);
		for (int i = 0; i < group.nSampled; i++)
			opponent.removeScoreAt(group.group[i], groupScores[i]);
	}

	@Override
	public void prepareTraits() {
		System.arraycopy(traits, 0, traitsNext, 0, nPopulation * nTraits);
	}

	@Override
	public void commitTraits() {
		double[] swap = traits;
		traits = traitsNext;
		traitsNext = swap;
	}

	@Override
	public void commitTraitAt(int me) {
		int idx = me * nTraits;
		System.arraycopy(traitsNext, idx, traits, idx, nTraits);
	}

	@Override
	public synchronized <T> void getTraitData(T[] colors, ColorMap<T> colorMap) {
		colorMap.translate(traits, colors);
	}

	/**
	 * Creates a histogram for each trait separately (if there are multiple) and
	 * returns the result in the array <code>bins</code> where the first index
	 * denotes the trait and the second refers to the bin.
	 *
	 * @param bins the array to store the histogram(s)
	 */
	public void getTraitHistogramData(double[][] bins) {
		// clear bins
		for (int n = 0; n < nTraits; n++)
			getTraitHistogramData(bins[n], n);
	}

	/**
	 * Creates a histogram for the trait with index {@code trait} and returns the
	 * result in the array <code>bins</code>.
	 *
	 * @param bins  the array to store the histogram(s)
	 * @param trait the index of the trait
	 */
	public void getTraitHistogramData(double[] bins, int trait) {
		// clear bins
		Arrays.fill(bins, 0.0);
		int nBins = bins.length;
		double scale = (nBins - 1);
		// fill bins
		for (int n = trait; n < nPopulation * nTraits; n += nTraits)
			// continuous traits are stored in normalized form
			bins[(int) (traits[n] * scale + 0.5)]++;
		double norm = 1.0 / nPopulation;
		for (int n = 0; n < nBins; n++)
			bins[n] *= norm;
	}

	/**
	 * Creates 2D histogram for traits <code>trait1</code> and <code>trait2</code>.
	 * The result is returned in the linear array <code>bins</code> and arranged in
	 * a way that is compatible with square lattice geometries for visualization by
	 * {@link org.evoludo.simulator.views.Distribution} and
	 * {@link org.evoludo.graphics.PopGraph2D} (GWT only).
	 *
	 * @param bins   the linear array to store the 2D histogram
	 * @param trait1 the index of the first trait
	 * @param trait2 the index of the second trait
	 *
	 * @see org.evoludo.simulator.Geometry#initGeometrySquare()
	 */
	public void get2DTraitHistogramData(double[] bins, int trait1, int trait2) {
		// clear bins
		Arrays.fill(bins, 0.0);
		double incr = 1.0 / nPopulation;
		if (nTraits == 1) {
			// ignore trait1 and trait2
			int size = bins.length;
			double scale = (size - 1);
			for (int n = 0; n < nPopulation; n++)
				// continuous traits are stored in normalized form
				bins[(int) (traits[n] * scale + 0.5)] += incr;
		} else {
			int size = (int) Math.sqrt(bins.length);
			double scale = (size - 1);
			// fill bins
			for (int n = 0; n < nPopulation * nTraits; n += nTraits)
				bins[(int) (traits[n + trait2] * scale + 0.5) * size
						+ (int) (traits[n + trait1] * scale + 0.5)] += incr;
		}
	}

	/**
	 * Gets all traits of all individuals. The traits are returned as a formatted
	 * string with an accuracy of {@code digits} decimals. With multiple traits they
	 * are listed sequentially for each individual.
	 * 
	 * @param digits the number of decimals of the formatted string
	 * @return the formatted traits
	 */
	public String getTraits(int digits) {
		return Formatter.format(traits, digits);
	}

	@Override
	public String getTraitNameAt(int index) {
		StringBuilder aName = new StringBuilder();
		int idx = index * nTraits;
		String[] names = module.getTraitNames();
		for (int i = 0; i < (nTraits - 1); i++) {
			aName.append(names[i])
					.append(" → ")
					.append(Formatter.format(traits[idx + i] * (traitRangeMax[i] - traitRangeMin[i]) + traitRangeMin[i],
							3))
					.append(", ");
		}
		aName.append(names[nTraits - 1])
				.append(" → ")
				.append(Formatter.format(
						traits[idx + nTraits - 1] * (traitRangeMax[nTraits - 1] - traitRangeMin[nTraits - 1])
								+ traitRangeMin[nTraits - 1],
						3));
		return aName.toString();
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * For continuous traits the first {@code nTraits} entries represent
	 * the mean of each trait and the second {@code nTraits} entries denote the
	 * standard deviation.
	 */
	@Override
	public double[] getMeanTraits(double[] mean) {
		for (int i = 0; i < nTraits; i++) {
			int idx = i;
			double avg = 0.0;
			double variance = 0.0;
			for (int n = 1; n <= nPopulation; n++) {
				double aTrait = traits[idx];
				double delta = aTrait - avg;
				avg += delta / n;
				variance += delta * (aTrait - avg);
				idx += nTraits;
			}
			double scale = traitRangeMax[i] - traitRangeMin[i];
			double shift = traitRangeMin[i];
			mean[i] = avg * scale + shift;
			mean[i + nTraits] = Math.sqrt(variance / (nPopulation - 1)) * scale;
		}
		return mean;
	}

	/**
	 * Gets the minimal value of each trait in the population and stores it in the
	 * array {@code min}. The array must be of length {@code >= nTraits}.
	 * 
	 * @return the array with the minimal trait values (same as input)
	 */
	public double[] getMinTraits() {
		return getMinTraits(new double[nTraits]);
	}

	/**
	 * Gets the minimal value of each trait in the population and stores it in the
	 * array {@code min}. The array must be of length {@code >= nTraits}.
	 * 
	 * @param min the array to store the minimal trait values
	 * @return the array with the minimal trait values (same as input)
	 */
	public double[] getMinTraits(double[] min) {
		if (nTraits == 1) {
			min[0] = ArrayMath.min(traits);
			return min;
		}
		for (int i = 0; i < nTraits; i++) {
			int idx = i;
			double m = Double.MAX_VALUE;
			for (int n = 0; n < nPopulation; n++) {
				double aTrait = traits[idx];
				if (aTrait < m)
					m = aTrait;
				idx += nTraits;
			}
			double scale = traitRangeMax[i] - traitRangeMin[i];
			double shift = traitRangeMin[i];
			min[i] = m * scale + shift;
		}
		return min;
	}

	/**
	 * Gets the maximal value of each trait in the population and stores it in the
	 * array {@code max}.
	 * 
	 * @return the array with the maximal trait values (same as input)
	 */
	public double[] getMaxTraits() {
		return getMaxTraits(new double[nTraits]);
	}

	/**
	 * Gets the maximal value of each trait in the population and stores it in the
	 * array {@code max}. The array must be of length {@code >= nTraits}.
	 * 
	 * @param max the array to store the maximal trait values
	 * @return the array with the maximal trait values (same as input)
	 */
	public double[] getMaxTraits(double[] max) {
		if (nTraits == 1) {
			max[0] = ArrayMath.max(traits);
			return max;
		}
		for (int i = 0; i < nTraits; i++) {
			int idx = i;
			double m = -Double.MAX_VALUE;
			for (int n = 0; n < nPopulation; n++) {
				double aTrait = traits[idx];
				if (aTrait > m)
					m = aTrait;
				idx += nTraits;
			}
			double scale = traitRangeMax[i] - traitRangeMin[i];
			double shift = traitRangeMin[i];
			max[i] = m * scale + shift;
		}
		return max;
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * For continuous traits the first {@code nTraits} entries represent the mean
	 * fitness of each trait and the second {@code nTraits} entries denote their
	 * standard deviation.
	 */
	@Override
	public double[] getMeanFitness(double[] mean) {
		double avg = 0.0;
		double variance = 0.0;
		for (int n = 0; n < nPopulation; n++) {
			double aScore = scores[n];
			double delta = aScore - avg;
			avg += delta / (n + 1);
			variance += delta * (aScore - avg);
		}
		mean[0] = avg;
		mean[1] = Math.sqrt(variance / (nPopulation - 1));
		return mean;
	}

	/**
	 * Adds the traits of all individuals to the array {@code state}. The array must
	 * be
	 * of length {@code nPopulation * nTraits}.
	 * 
	 * @param state the array to store the traits
	 */
	public void addState(double[] state) {
		if (state == null || state.length != nPopulation * nTraits)
			throw new IllegalArgumentException(
					"state is null or state length != nPopulation * nTraits ");
		ArrayMath.add(state, traits, state);
	}

	/**
	 * The array for calculating and storing the mean traits and their standard
	 * deviation. Must be of length {@code > 2 * nTraits}.
	 */
	private double[] meantrait;

	@Override
	public String getStatus() {
		getMeanTraits(meantrait);
		String[] names = module.getTraitNames();
		StringBuilder status = new StringBuilder();
		status.append(names[0])
				.append(" mean: ")
				.append(Formatter.formatFix(meantrait[0], 3))
				.append(" ± ")
				.append(Formatter.formatFix(meantrait[nTraits], 3));
		for (int i = 1; i < nTraits; i++) {
			status.append("; ")
					.append(names[i])
					.append(" mean: ")
					.append(Formatter.formatFix(meantrait[i], 3))
					.append(" ± ")
					.append(Formatter.formatFix(meantrait[i + nTraits], 3));
		}
		return status.toString();
	}

	@Override
	public boolean check() {
		boolean doReset = false;
		doReset |= super.check();
		traitRangeMin = module.getTraitMin();
		traitRangeMax = module.getTraitMax();

		// check interaction geometry
		if (interaction.getType() == Geometry.Type.MEANFIELD && interGroup.isSampling(IBSGroup.SamplingType.ALL)) {
			// interacting with everyone in mean-field simulations is not feasible - except
			// for discrete traits
			logger.warning(
					"interaction type (" + interGroup.getSampling() + ") unfeasible in well-mixed populations!");
			interGroup.setSampling(IBSGroup.SamplingType.RANDOM);
			// change of sampling may affect whether scores can be adjusted
			adjustScores = doAdjustScores();
		}

		if (interaction.isInterspecies())
			logger.warning("multi-species interactions with continuous traits have NOT been tested...");

		if (module.getNGroup() > 2)
			logger.warning("group interactions with continuous traits have NOT been tested...");

		if (traits == null || traits.length != nPopulation * nTraits)
			traits = new double[nPopulation * nTraits];
		if (traitsNext == null || traitsNext.length != nPopulation * nTraits)
			traitsNext = new double[nPopulation * nTraits];
		if (myTrait == null || myTrait.length != nTraits)
			myTrait = new double[nTraits];

		return doReset;
	}

	/**
	 * Type of initial configuration.
	 * 
	 * @see Init#clo
	 */
	protected Init init;

	/**
	 * Sets the type of the initial configuration and any accompanying arguments. If
	 * either {@code type} or {@code args} are {@code null} the respective current
	 * setting is preserved.
	 * 
	 * @param init the type and arguments of the initial configuration
	 */
	public void setInit(Init init) {
		this.init = init;
	}

	/**
	 * Gets the type of the initial configuration and its arguments.
	 *
	 * @return the type and arguments of the initial configuration
	 */
	public Init getInit() {
		return init;
	}

	@Override
	public void init() {
		super.init();
		int mutidx = -1;
		for (int s = 0; s < nTraits; s++) {
			switch (init.type) {
				default:
				case UNIFORM:
					for (int n = s; n < nPopulation * nTraits; n += nTraits)
						traits[n] = random01();
					break;

				case MONO:
					// initArgs contains monomorphic trait
					double mono = Math.min(Math.max(init.args[s][0], traitRangeMin[s]), traitRangeMax[s]);
					double scaledmono = (mono - traitRangeMin[s]) / (traitRangeMax[s] - traitRangeMin[s]);
					for (int n = s; n < nPopulation * nTraits; n += nTraits)
						traits[n] = scaledmono;
					break;

				case GAUSSIAN:
					double mean = Math.min(Math.max(init.args[s][0], traitRangeMin[s]), traitRangeMax[s]);
					double sdev = Math.min(Math.max(init.args[s][1], traitRangeMin[s]), traitRangeMax[s]);
					double scaledmean = (mean - traitRangeMin[s]) / (traitRangeMax[s] - traitRangeMin[s]);
					double scaledsdev = sdev / (traitRangeMax[s] - traitRangeMin[s]);
					for (int n = s; n < nPopulation * nTraits; n += nTraits)
						traits[n] = Math.min(1.0, Math.max(0.0, randomGaussian(scaledmean, scaledsdev)));
					break;

				case MUTANT:
					double resident = Math.min(Math.max(init.args[s][0], traitRangeMin[s]), traitRangeMax[s]);
					double scaledresident = (resident - traitRangeMin[s]) / (traitRangeMax[s] - traitRangeMin[s]);
					for (int n = s; n < nPopulation * nTraits; n += nTraits)
						traits[n] = scaledresident;
					if (mutidx < 0)
						mutidx = random0n(nPopulation);
					double mut = Math.min(Math.max(init.args[s][1], traitRangeMin[s]), traitRangeMax[s]);
					traits[mutidx] = (mut - traitRangeMin[s]) / (traitRangeMax[s] - traitRangeMin[s]);
					break;
			}
		}
	}

	@Override
	public synchronized void reset() {
		super.reset();
		// groupScores have the same maximum length
		int maxGroup = groupScores.length;
		if (tmpGroup == null || tmpGroup.length != maxGroup * nTraits)
			tmpGroup = new double[maxGroup * nTraits];
		if (smallTrait == null || smallTrait.length != maxGroup * nTraits)
			smallTrait = new double[maxGroup * nTraits];
		if (meantrait == null || meantrait.length != 2 * nTraits)
			meantrait = new double[2 * nTraits];
		if (adjustScores) {
			if (oldTrait == null || oldTrait.length != nTraits)
				oldTrait = new double[nTraits];
			if (oldScores == null || oldScores.length != maxGroup)
				oldScores = new double[maxGroup];
		} else {
			oldTrait = null;
			oldScores = null;
		}
	}

	@Override
	public boolean mouseHitNode(int hit, boolean alt) {
		if (hit < 0 || hit >= nPopulation)
			return false; // invalid argument
		int offset = hit * nTraits;
		for (int n = 0; n < nTraits; n++)
			traitsNext[offset + n] = Math.min(Math.max(traits[offset + n] + (alt ? -0.1 : 0.1), 0.0), 1.0);
		// update scores and display
		if (adjustScores) {
			adjustGameScoresAt(hit); // this also commits trait
		} else {
			commitTraitAt(hit);
			// when in doubt, recalculate entire board
			resetScores();
			updateScores();
		}
		engine.fireModelChanged();
		return true;
	}

	@Override
	public void encodeTraits(StringBuilder plist) {
		plist.append(Plist.encodeKey("Configuration", traits));
	}

	@Override
	public boolean restoreTraits(Plist plist) {
		@SuppressWarnings("unchecked")
		List<Double> config = (List<Double>) plist.get("Configuration");
		int size = nPopulation * nTraits;
		if (config == null || config.size() != size)
			return false;
		for (int n = 0; n < size; n++)
			traits[n] = config.get(n);
		return true;
	}
}
