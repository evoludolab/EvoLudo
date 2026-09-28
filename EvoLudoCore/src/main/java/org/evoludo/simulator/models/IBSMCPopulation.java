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

package org.evoludo.simulator.models;

import java.awt.Color;
import java.util.Arrays;
import java.util.List;

import org.evoludo.math.ArrayMath;
import org.evoludo.simulator.ColorMap;
import org.evoludo.simulator.EvoLudo;
import org.evoludo.simulator.geometries.GeometryType;
import org.evoludo.simulator.geometries.HierarchicalGeometry;
import org.evoludo.simulator.models.IBS.ScoringType;
import org.evoludo.simulator.models.IBSC.Init;
import org.evoludo.simulator.models.Model.HasIBS;
import org.evoludo.simulator.modules.Continuous;
import org.evoludo.simulator.modules.Features.Payoffs;
import org.evoludo.simulator.modules.Module;
import org.evoludo.util.Formatter;
import org.evoludo.util.Plist;

/**
 * The core class for individual based simulations with <em>multiple</em>
 * continuous traits. Manages the traits of the population, while delegating the
 * management of the population and individual fitness as well as simulation
 * steps to super. Note that some further optimizations and simplifications are
 * possible in the special case of a single continuous trait, which is handled
 * in the subclass {@link IBSCPopulation}.
 * <p>
 * Modules that admit vacant sites (see {@link Continuous#hasVacant()}) mark
 * empty sites by setting all their traits to {@link Double#NaN}.
 * 
 * @author Christoph Hauert
 * 
 * @see IBSPopulation
 * @see IBSCPopulation
 */
public class IBSMCPopulation extends IBSPopulation<Continuous, IBSMCPopulation> {

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
	 * Creates a population of individuals with multiple continuous traits for IBS
	 * simulations.
	 * 
	 * @param engine the pacemaker for running the model
	 * @param module the module that defines the game
	 */
	public IBSMCPopulation(EvoLudo engine, Continuous module) {
		super(engine, module);
		mutation = module.getMutation();
		if (module instanceof HasIBS.MCPairs)
			pairmodule = (HasIBS.MCPairs) module;
		if (module instanceof HasIBS.MCGroups)
			groupmodule = (HasIBS.MCGroups) module;
		init = new Init((IBS) engine.getModel(), module.getNTraits());
	}

	@Override
	public void setOpponentPop(IBSPopulation<?, ?> opponent) {
		if (!(opponent instanceof IBSMCPopulation)) {
			throw new IllegalArgumentException("opponent must be IBSMCPopulation");
		}
		super.setOpponentPop(opponent);
	}

	@Override
	public boolean checkConvergence() {
		// extinction is absorbing but otherwise convergence takes more than just
		// the absence of mutations
		return vacantIdx >= 0 && nOccupied == 0;
	}

	/**
	 * The number of occupied (non-vacant) sites. Only maintained for modules
	 * admitting vacant sites.
	 * 
	 * @see #getPopulationSize()
	 */
	int nOccupied;

	@Override
	public int getPopulationSize() {
		if (vacantIdx < 0)
			return nPopulation;
		return nOccupied;
	}

	/**
	 * Count the number of occupied sites, {@link #nOccupied}, from scratch. Must be
	 * called after bulk changes to the traits that bypass
	 * {@link #commitTraitAt(int)}.
	 */
	private void countOccupied() {
		nOccupied = 0;
		for (int n = 0; n < nPopulation; n++)
			if (!isVacantAt(n))
				nOccupied++;
		invalidateOccupancyIndex();
	}

	@Override
	public boolean isVacantAt(int index) {
		return vacantIdx >= 0 && Double.isNaN(traits[index * nTraits]);
	}

	@Override
	public boolean becomesVacantAt(int index) {
		return vacantIdx >= 0 && Double.isNaN(traitsNext[index * nTraits]);
	}

	@Override
	protected void setNextVacantAt(int idx) {
		int offset = idx * nTraits;
		Arrays.fill(traitsNext, offset, offset + nTraits, Double.NaN);
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
	 * Set trait {@code d} of the individual with index {@code idx}.
	 * 
	 * @param idx   the index of the individual
	 * @param d     the trait index
	 * @param trait the new trait value
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
		System.arraycopy(traits, idx * nTraits, idxtraits, 0, nTraits);
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
		System.arraycopy(idxtraits, 0, traits, idx * nTraits, nTraits);
	}

	/**
	 * The array for temporarily storing traits during updates.
	 */
	double[] traitsNext;

	/**
	 * Temporary storage for traits of individuals in group interactions.
	 */
	double[] tmpGroup;

	/**
	 * Temporary storage for traits of individuals in small sub-group interactions.
	 */
	double[] smallTrait;

	/**
	 * Temporary storage for the traits of the focal individual.
	 */
	double[] myTraits;

	/**
	 * Temporary storage for the traits of the focal individual before
	 * the update. Used for adjusting scores.
	 */
	double[] oldTraits;

	/**
	 * Temporary storage for the scores of each participant prior to group
	 * interactions.
	 */
	double[] oldScores;

	/**
	 * Temporary storage for the indices of the occupied members of an interaction
	 * group.
	 * 
	 * @see #stripGroupVacancies(IBSGroup)
	 * @see #gatherNeighbours(int[], int, int)
	 */
	int[] groupIdxs;

	/**
	 * Eliminate vacant sites from the interaction group {@code group}.
	 * <p>
	 * <strong>Important:</strong> {@code group.group} is untouchable! It may be a
	 * reference to {@code AbstractGeometry.out[group.focal]} and hence any changes
	 * would actually alter the geometry! Instead, the occupied members are copied
	 * to {@link #groupIdxs}, which then replaces {@code group.group}.
	 *
	 * @param group the interaction group, which potentially includes vacant sites
	 */
	void stripGroupVacancies(IBSGroup group) {
		if (vacantIdx < 0)
			return;
		int[] members = group.group;
		int nMembers = 0;
		for (int i = 0; i < group.nSampled; i++) {
			int member = members[i];
			if (!opponent.isVacantAt(member))
				groupIdxs[nMembers++] = member;
		}
		group.group = groupIdxs;
		group.nSampled = nMembers;
	}

	/**
	 * Gather the indices and traits of the occupied sites among the {@code nNeigh}
	 * neighbours {@code neigh} in the opponent population and append them to
	 * {@link #groupIdxs} and {@link #tmpGroup}, respectively, starting at position
	 * {@code start}.
	 *
	 * @param neigh  the indices of the neighbours
	 * @param nNeigh the number of neighbours
	 * @param start  the position of the first neighbour to append
	 * @return the total number of gathered neighbours
	 */
	int gatherNeighbours(int[] neigh, int nNeigh, int start) {
		double[] opptraits = opponent.traits;
		int oppntraits = opponent.nTraits;
		int n = start;
		for (int i = 0; i < nNeigh; i++) {
			int you = neigh[i];
			if (opponent.isVacantAt(you))
				continue;
			groupIdxs[n] = you;
			System.arraycopy(opptraits, you * oppntraits, tmpGroup, n * oppntraits, oppntraits);
			n++;
		}
		return n;
	}

	@Override
	public void updateFromModelAt(int index, int modelPlayer) {
		super.updateFromModelAt(index, modelPlayer); // deal with tags
		System.arraycopy(traits, modelPlayer * nTraits, traitsNext, index * nTraits, nTraits);
	}

	@Override
	public boolean haveSameTrait(int a, int b) {
		if (isVacantAt(a) || isVacantAt(b))
			return isVacantAt(a) && isVacantAt(b);
		int idxa = a * nTraits;
		int idxb = b * nTraits;
		for (int i = 0; i < nTraits; i++)
			if (Math.abs(traits[idxa + i] - traits[idxb + i]) > Model.EPS)
				return false;
		return true;
	}

	@Override
	public boolean isSameTrait(int a) {
		if (isVacantAt(a) || becomesVacantAt(a))
			return isVacantAt(a) && becomesVacantAt(a);
		int idxa = a * nTraits;
		for (int i = 0; i < nTraits; i++)
			if (Math.abs(traits[idxa + i] - traitsNext[idxa + i]) > Model.EPS)
				return false;
		return true;
	}

	@Override
	protected void swapTraits(int a, int b) {
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
		if (distmesample < Model.EPS)
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
		System.arraycopy(traits, group.focal * nTraits, myTraits, 0, nTraits);
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
				if (interaction.isType(GeometryType.WELLMIXED) || //
						(interaction.isType(GeometryType.HIERARCHY) && //
								((HierarchicalGeometry) interaction).isSubtype(GeometryType.WELLMIXED)))
					return false;
				return interGroup.isSampling(IBSGroup.SAMPLING_ALL);
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

	/**
	 * {@inheritDoc}
	 * <p>
	 * Continuous modules with a single trait never get here.
	 * 
	 * @see IBSCPopulation#playPairGameAt(IBSGroup)
	 */
	@Override
	public void playPairGameAt(IBSGroup group) {
		int me = group.focal;
		if (isVacantAt(me))
			return;
		stripGroupVacancies(group);
		// for ephemeral scores calculate score of focal only
		boolean ephemeralScores = playerScoring.equals(ScoringType.EPHEMERAL);
		if (group.nSampled <= 0) {
			// isolated individual (or surrounded by vacant sites)
			if (ephemeralScores) {
				// no need to update scores of everyone else
				resetScoreAt(me);
				return;
			}
			// no interactions
			updateScoreAt(me, 0.0, 0);
			return;
		}
		gatherPlayers(group);
		double myScore = pairmodule.pairScores(myTraits, tmpGroup, group.nSampled,
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

	/**
	 * {@inheritDoc}
	 * <p>
	 * Handles single and multiple traits alike. Vacant neighbours do not
	 * interact. If the focal site becomes vacant, its score is reset and its
	 * contributions to the scores of its neighbours are removed; if a vacant
	 * focal site becomes occupied, its contributions are added.
	 */
	@Override
	public void adjustPairGameScoresAt(int me) {
		// gather occupied interaction partners: out-neighbours as well as
		// in-neighbours on directed graphs
		int nInter = gatherNeighbours(interaction.out[me], interaction.kout[me], 0);
		int u2 = 2;
		if (!interaction.isUndirected()) {
			// directed graph, add in-neighbors
			u2 = 1;
			nInter = gatherNeighbours(interaction.in[me], interaction.kin[me], nInter);
		}
		boolean wasVacant = isVacantAt(me);
		boolean isVacant = becomesVacantAt(me);
		int offset = me * nTraits;
		// old/newScore are the scores of single interactions with each partner
		double oldScore = 0.0;
		if (wasVacant)
			Arrays.fill(oldScores, 0, nInter, 0.0);
		else {
			System.arraycopy(traits, offset, oldTraits, 0, nTraits);
			oldScore = pairmodule.pairScores(oldTraits, tmpGroup, nInter, oldScores);
		}
		double newScore = 0.0;
		if (isVacant)
			Arrays.fill(groupScores, 0, nInter, 0.0);
		else {
			System.arraycopy(traitsNext, offset, myTraits, 0, nTraits);
			newScore = pairmodule.pairScores(myTraits, tmpGroup, nInter, groupScores);
		}
		commitTraitAt(me);
		// adjust score of focal; on undirected graphs every partner is interacted
		// with twice, once as the focal and once as the partner, see
		// playGameSyncAt(int)
		if (isVacant)
			resetScoreAt(me);
		else if (wasVacant)
			updateScoreAt(me, u2 * newScore, u2 * nInter);
		else {
			if (playerScoreAveraged) {
				if (nInter > 0) {
					double iInter = 1.0 / nInter;
					newScore *= iInter;
					oldScore *= iInter;
				}
			} else {
				newScore *= u2;
				oldScore *= u2;
			}
			adjustScoreAt(me, oldScore, newScore);
		}
		// adjust scores of partners
		for (int n = 0; n < nInter; n++) {
			int you = groupIdxs[n];
			if (wasVacant) {
				// gained interaction partner
				opponent.updateScoreAt(you, u2 * groupScores[n], u2);
				continue;
			}
			if (isVacant) {
				// lost interaction partner
				opponent.removeScoreAt(you, u2 * oldScores[n], u2);
				continue;
			}
			double diff = u2 * (groupScores[n] - oldScores[n]);
			if (playerScoreAveraged)
				diff /= opponent.interactions[you];
			opponent.adjustScoreAt(you, diff);
		}
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * Continuous modules with a single trait never get here.
	 * 
	 * @see IBSCPopulation#playGroupGameAt(IBSGroup)
	 */
	@Override
	public void playGroupGameAt(IBSGroup group) {
		int me = group.focal;
		if (isVacantAt(me))
			return;
		int nGroup = module.getNGroup();
		if (!group.isSampling(IBSGroup.SAMPLING_ALL) && group.nSampled > nGroup - 1) {
			// random sampling: independently sampled groups, see IBSGroup#nMembers
			playGroupsAt(me, group, nGroup, group.nSampled / (nGroup - 1));
			return;
		}
		stripGroupVacancies(group);
		if (group.nSampled <= 0) {
			// isolated individual (or surrounded by vacant sites)
			playNoGameAt(me);
			return;
		}
		gatherPlayers(group);

		if (group.isSampling(IBSGroup.SAMPLING_ALL) && nGroup < group.nSampled + 1) {
			// interact with all neighbors - interact repeatedly if nGroup<nSampled+1
			playGroupSequentiallyAt(me, group, nGroup);
			return;
		}
		// interact with full group (random graphs, all neighbors or sampled
		// neighbors)
		playGroupOnceAt(me, group);
	}

	/**
	 * Handle the case where a focal player has no opponents and should not play a
	 * game.
	 * 
	 * @param me focal player index
	 */
	void playNoGameAt(int me) {
		if (playerScoring.equals(ScoringType.EPHEMERAL)) {
			resetScoreAt(me);
			return;
		}
		// no interactions
		updateScoreAt(me, 0.0, 0);
	}

	/**
	 * Evaluate sequential group interactions when not enough opponents are
	 * available simultaneously.
	 * 
	 * @param me     focal player index
	 * @param group  sampled opponent group
	 * @param nGroup number of players participating in each interaction
	 */
	void playGroupSequentiallyAt(int me, IBSGroup group, int nGroup) {
		// interact with part of group sequentially
		double myScore = 0.0;
		Arrays.fill(smallScores, 0, group.nSampled, 0.0);
		// for ephemeral scores calculate score of focal only
		boolean ephemeralScores = playerScoring.equals(ScoringType.EPHEMERAL);
		for (int n = 0; n < group.nSampled; n++) {
			for (int i = 0; i < nGroup - 1; i++)
				System.arraycopy(tmpGroup, ((n + i) % group.nSampled) * nTraits, smallTrait, i * nTraits,
						nTraits);
			myScore += groupmodule.groupScores(myTraits, smallTrait, nGroup - 1, groupScores);
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
	}

	/**
	 * Evaluate interactions in {@code nGroups} independently sampled groups. The
	 * members of the groups are stored consecutively in {@code group}, with
	 * {@code nGroup-1} members each. Vacant sites are skipped within each group.
	 * 
	 * @param me      focal player index
	 * @param group   sampled members of all groups
	 * @param nGroup  size of the groups (including the focal individual)
	 * @param nGroups number of groups
	 */
	void playGroupsAt(int me, IBSGroup group, int nGroup, int nGroups) {
		System.arraycopy(traits, me * nTraits, myTraits, 0, nTraits);
		double[] opptraits = opponent.traits;
		int oppntraits = opponent.nTraits;
		double myScore = 0.0;
		int nPlayed = 0;
		// for ephemeral scores calculate score of focal only
		boolean ephemeralScores = playerScoring.equals(ScoringType.EPHEMERAL);
		int nOthers = nGroup - 1;
		for (int g = 0; g < nGroups; g++) {
			int nMembers = 0;
			for (int i = g * nOthers; i < (g + 1) * nOthers; i++) {
				int member = group.group[i];
				if (opponent.isVacantAt(member))
					continue;
				System.arraycopy(opptraits, member * oppntraits, smallTrait, nMembers * oppntraits, oppntraits);
				groupIdxs[nMembers++] = member;
			}
			if (nMembers == 0)
				continue;
			myScore += groupmodule.groupScores(myTraits, smallTrait, nMembers, groupScores);
			nPlayed++;
			if (ephemeralScores)
				continue;
			for (int i = 0; i < nMembers; i++)
				opponent.updateScoreAt(groupIdxs[i], groupScores[i]);
		}
		if (nPlayed == 0) {
			// surrounded by vacant sites
			playNoGameAt(me);
			return;
		}
		if (ephemeralScores) {
			resetScoreAt(me);
			setScoreAt(me, myScore / nPlayed, nPlayed);
			return;
		}
		updateScoreAt(me, myScore, nPlayed);
	}

	/**
	 * Evaluate a single group interaction with the sampled opponents.
	 * 
	 * @param me    focal player index
	 * @param group sampled opponent group
	 */
	void playGroupOnceAt(int me, IBSGroup group) {
		double myScore = groupmodule.groupScores(myTraits, tmpGroup, group.nSampled, groupScores);
		// for ephemeral scores calculate score of focal only
		if (playerScoring.equals(ScoringType.EPHEMERAL)) {
			resetScoreAt(me);
			setScoreAt(me, myScore, 1);
			return;
		}
		updateScoreAt(me, myScore);
		for (int i = 0; i < group.nSampled; i++)
			opponent.updateScoreAt(group.group[i], groupScores[i]);
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * Continuous modules with a single trait never get here.
	 * 
	 * @see IBSCPopulation#yalpGroupGameAt(IBSGroup)
	 */
	@Override
	public void yalpGroupGameAt(IBSGroup group) {
		if (isVacantAt(group.focal))
			return;
		stripGroupVacancies(group);
		if (group.nSampled <= 0)
			return;
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
				myScore += groupmodule.groupScores(myTraits, smallTrait, nGroup - 1, groupScores);
				for (int i = 0; i < nGroup - 1; i++)
					smallScores[(n + i) % group.nSampled] += groupScores[i];
			}
			removeScoreAt(group.focal, myScore, group.nSampled);
			for (int i = 0; i < group.nSampled; i++)
				opponent.removeScoreAt(group.group[i], smallScores[i], nGroup - 1);
			return;
		}
		// interact with full group (random graphs)
		myScore = groupmodule.groupScores(myTraits, tmpGroup, group.nSampled, groupScores);
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
		if (vacantIdx >= 0)
			countOccupied();
	}

	@Override
	public void commitTraitAt(int me) {
		int idx = me * nTraits;
		if (vacantIdx < 0) {
			System.arraycopy(traitsNext, idx, traits, idx, nTraits);
			return;
		}
		boolean wasVacant = isVacantAt(me);
		System.arraycopy(traitsNext, idx, traits, idx, nTraits);
		boolean isVacant = isVacantAt(me);
		if (wasVacant == isVacant)
			return;
		// keep the population size and the compact occupancy index (see
		// IBSPopulation#occupiedSites) in sync with this single-site change
		if (isVacant) {
			nOccupied--;
			trackSiteVacated(me);
			return;
		}
		nOccupied++;
		trackSiteOccupied(me);
	}

	@Override
	public synchronized <T> void getTraitData(T[] colors, ColorMap<T> colorMap) {
		colorMap.translate(traits, colors);
		if (vacantIdx < 0)
			return;
		T vacant = colorMap.color2Color(Color.WHITE);
		for (int n = 0; n < nPopulation; n++)
			if (isVacantAt(n))
				colors[n] = vacant;
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
		for (int n = trait; n < nPopulation * nTraits; n += nTraits) {
			double aTrait = traits[n];
			// skip vacant sites
			if (Double.isNaN(aTrait))
				continue;
			// continuous traits are stored in normalized form
			bins[(int) (aTrait * scale + 0.5)]++;
		}
		// note: with vacant sites the histogram is normalized by the number of
		// sites rather than the population size (cf. fitness histograms)
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
	 */
	public void get2DTraitHistogramData(double[] bins, int trait1, int trait2) {
		// clear bins
		Arrays.fill(bins, 0.0);
		double incr = 1.0 / nPopulation;
		if (nTraits == 1) {
			// ignore trait1 and trait2
			int size = bins.length;
			double scale = (size - 1);
			for (int n = 0; n < nPopulation; n++) {
				// skip vacant sites
				if (Double.isNaN(traits[n]))
					continue;
				// continuous traits are stored in normalized form
				bins[(int) (traits[n] * scale + 0.5)] += incr;
			}
		} else {
			int size = (int) Math.sqrt(bins.length);
			double scale = (size - 1);
			// fill bins
			for (int n = 0; n < nPopulation * nTraits; n += nTraits) {
				// skip vacant sites
				if (Double.isNaN(traits[n]))
					continue;
				bins[(int) (traits[n + trait2] * scale + 0.5) * size
						+ (int) (traits[n + trait1] * scale + 0.5)] += incr;
			}
		}
	}

	/**
	 * Gets all traits of all individuals. The traits are returned as a formatted
	 * string with an accuracy of {@code digits} decimals. With multiple traits they
	 * are listed sequentially for each individual. Traits are reported in the
	 * units of their respective trait ranges.
	 * 
	 * @param digits the number of decimals of the formatted string
	 * @return the formatted traits
	 */
	public String getTraits(int digits) {
		int len = nPopulation * nTraits;
		double[] scaled = new double[len];
		for (int n = 0; n < len; n++) {
			int s = n % nTraits;
			scaled[n] = traitRangeMin[s] + traits[n] * (traitRangeMax[s] - traitRangeMin[s]);
		}
		return Formatter.format(scaled, digits);
	}

	@Override
	public String getTraitNameAt(int index) {
		if (isVacantAt(index))
			return Module.VACANT_NAME;
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
			int count = 0;
			for (int n = 0; n < nPopulation; n++) {
				double aTrait = traits[idx];
				idx += nTraits;
				// skip vacant sites
				if (Double.isNaN(aTrait))
					continue;
				double delta = aTrait - avg;
				avg += delta / ++count;
				variance += delta * (aTrait - avg);
			}
			double scale = traitRangeMax[i] - traitRangeMin[i];
			double shift = traitRangeMin[i];
			// mean undefined in extinct populations
			mean[i] = (count > 0 ? avg * scale + shift : Double.NaN);
			mean[i + nTraits] = (count > 1 ? Math.sqrt(variance / (count - 1)) * scale : 0.0);
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
		for (int i = 0; i < nTraits; i++) {
			int idx = i;
			double m = Double.MAX_VALUE;
			for (int n = 0; n < nPopulation; n++) {
				double aTrait = traits[idx];
				// note: comparisons with NaN (vacant sites) are always false
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
		for (int i = 0; i < nTraits; i++) {
			int idx = i;
			double m = -Double.MAX_VALUE;
			for (int n = 0; n < nPopulation; n++) {
				double aTrait = traits[idx];
				// note: comparisons with NaN (vacant sites) are always false
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
		int count = 0;
		for (int n = 0; n < nPopulation; n++) {
			// skip vacant sites
			if (isVacantAt(n))
				continue;
			double aScore = scores[n];
			double delta = aScore - avg;
			avg += delta / ++count;
			variance += delta * (aScore - avg);
		}
		// mean undefined in extinct populations
		mean[0] = (count > 0 ? avg : Double.NaN);
		mean[1] = (count > 1 ? Math.sqrt(variance / (count - 1)) : 0.0);
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
		if (vacantIdx >= 0)
			status.append("; Density: ")
					.append(Formatter.formatPercent((double) nOccupied / nPopulation, 1));
		return status.toString();
	}

	@Override
	public boolean check() {
		boolean doReset = false;
		doReset |= super.check();
		traitRangeMin = module.getTraitMin();
		traitRangeMax = module.getTraitMax();

		// check interaction geometry
		if (interaction.isType(GeometryType.WELLMIXED) && interGroup.isSampling(IBSGroup.SAMPLING_ALL)) {
			// interacting with everyone in mean-field simulations is not feasible - except
			// for discrete traits
			engine.logWarning(
					"interaction type 'all' unfeasible in well-mixed populations - using single interaction!");
			interGroup.setNSamples(1);
			// change of sampling may affect whether scores can be adjusted
			adjustScores = doAdjustScores();
		}

		if (interaction.isInterspecies())
			engine.logWarning("multi-species interactions with continuous traits have NOT been tested...");

		if (module.getNGroup() > 2)
			engine.logWarning("group interactions with continuous traits have NOT been tested...");

		if (traits == null || traits.length != nPopulation * nTraits) {
			traits = new double[nPopulation * nTraits];
			// traits are now allocated but do not reflect an actual configuration
			// until init() runs
			initialized = false;
		}
		if (traitsNext == null || traitsNext.length != nPopulation * nTraits)
			traitsNext = new double[nPopulation * nTraits];
		if (myTraits == null || myTraits.length != nTraits)
			myTraits = new double[nTraits];

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
		// the mutant (if any) must not be removed when placing vacant sites
		int keep = -1;
		switch (init.type) {
			default:
			case UNIFORM:
				initUniform();
				break;
			case MONO:
				initMono();
				break;
			case GAUSSIAN:
				initGaussian();
				break;
			case MUTANT:
				keep = initMutant();
				break;
		}
		if (vacantIdx >= 0) {
			initVacancies(keep);
			// initialization bypasses the single-site bookkeeping in commitTraitAt(int)
			countOccupied();
		}
		initialized = true;
	}

	/**
	 * Vacate sites uniformly at random such that the fraction of occupied sites
	 * matches the initial population density. The frequency of vacant sites
	 * {@code v} is specified by an optional, additional argument for the first
	 * trait of the initialization type, see {@link Init#clo}, or, if omitted,
	 * the density is estimated from the equilibrium density of a monomorphic
	 * population with the mean trait(s), see {@link #getDensityEstimate()}. At
	 * least one site always remains occupied.
	 *
	 * @param keep the index of the site that must remain occupied or {@code -1}
	 */
	private void initVacancies(int keep) {
		double[] args = init.args[0];
		int nParams = init.type.nParams;
		double density = (args != null && args.length > nParams) ? 1.0 - args[nParams] : getDensityEstimate();
		int nOcc = Math.max(1, Math.min(nPopulation, (int) (density * nPopulation + 0.5)));
		int[] sites = new int[nPopulation];
		for (int n = 0; n < nPopulation; n++)
			sites[n] = n;
		int nFree = nPopulation;
		if (keep >= 0) {
			// move site keep to the end and exclude it from the draw
			sites[keep] = nPopulation - 1;
			sites[nPopulation - 1] = keep;
			nFree--;
		}
		// partial Fisher-Yates shuffle to draw the sites to vacate
		int nVacant = nPopulation - nOcc;
		for (int n = 0; n < nVacant; n++) {
			int pick = n + random0n(nFree - n);
			int site = sites[pick];
			sites[pick] = sites[n];
			int offset = site * nTraits;
			Arrays.fill(traits, offset, offset + nTraits, Double.NaN);
		}
	}

	/**
	 * Estimates the equilibrium population density for a monomorphic population
	 * with the mean trait(s) of the current, fully occupied population in the
	 * mean-field limit, see {@link ODE#getVacantAtEQ(double, double, double, double)}.
	 * For modules implementing {@link Payoffs} the fitness is based on the
	 * payoff of a single pairwise interaction.
	 *
	 * @return the estimated population density
	 */
	protected double getDensityEstimate() {
		double fitness = 0.0;
		if (module instanceof Payoffs && pairmodule != null) {
			double[] mean = new double[nTraits];
			for (int n = 0; n < nPopulation * nTraits; n++)
				mean[n % nTraits] += traits[n];
			ArrayMath.multiply(mean, 1.0 / nPopulation);
			fitness = map2fit.map(pairmodule.pairScores(mean, mean, 1, new double[1]));
		}
		double z = ODE.getVacantAtEQ(module.getBirthRate(), module.getDeathRate(), fitness,
				module.getCompetitionRate());
		if (Double.isNaN(z)) {
			// no net growth - carrying capacity estimate undefined; default to fully
			// occupied and let the user specify an explicit density
			engine.logWarning("no net growth - assume fully occupied.");
			return 1.0;
		}
		return 1.0 - z;
	}

	/**
	 * Initialize each trait uniformly at random in its allowed range.
	 */
	void initUniform() {
		for (int s = 0; s < nTraits; s++) {
			for (int n = s; n < nPopulation * nTraits; n += nTraits)
				traits[n] = random01();
		}
	}

	/**
	 * Initialize all individuals with the same (monomorphic) trait value.
	 */
	void initMono() {
		for (int s = 0; s < nTraits; s++) {
			// initArgs contains monomorphic trait
			double mono = Math.min(Math.max(init.args[s][0], traitRangeMin[s]), traitRangeMax[s]);
			double scaledmono = (mono - traitRangeMin[s]) / (traitRangeMax[s] - traitRangeMin[s]);
			for (int n = s; n < nPopulation * nTraits; n += nTraits)
				traits[n] = scaledmono;
		}
	}

	/**
	 * Initialize traits by sampling from a (possibly truncated) Gaussian
	 * distribution per trait.
	 */
	void initGaussian() {
		for (int s = 0; s < nTraits; s++) {
			double mean = Math.min(Math.max(init.args[s][0], traitRangeMin[s]), traitRangeMax[s]);
			double sdev = Math.min(Math.max(init.args[s][1], traitRangeMin[s]), traitRangeMax[s]);
			double scaledmean = (mean - traitRangeMin[s]) / (traitRangeMax[s] - traitRangeMin[s]);
			double scaledsdev = sdev / (traitRangeMax[s] - traitRangeMin[s]);
			for (int n = s; n < nPopulation * nTraits; n += nTraits)
				traits[n] = Math.min(1.0, Math.max(0.0, randomGaussian(scaledmean, scaledsdev)));
		}
	}

	/**
	 * Initialize a monomorphic resident population with a single mutant.
	 * 
	 * @return the index of the mutant
	 */
	int initMutant() {
		int mutidx = random0n(nPopulation);
		for (int s = 0; s < nTraits; s++) {
			double resident = Math.min(Math.max(init.args[s][0], traitRangeMin[s]), traitRangeMax[s]);
			double scaledresident = (resident - traitRangeMin[s]) / (traitRangeMax[s] - traitRangeMin[s]);
			for (int n = s; n < nPopulation * nTraits; n += nTraits)
				traits[n] = scaledresident;
			double mut = Math.min(Math.max(init.args[s][1], traitRangeMin[s]), traitRangeMax[s]);
			traits[mutidx * nTraits + s] = (mut - traitRangeMin[s]) / (traitRangeMax[s] - traitRangeMin[s]);
		}
		return mutidx;
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
		if (groupIdxs == null || groupIdxs.length != maxGroup)
			groupIdxs = new int[maxGroup];
		if (adjustScores) {
			if (oldTraits == null || oldTraits.length != nTraits)
				oldTraits = new double[nTraits];
			if (oldScores == null || oldScores.length != maxGroup)
				oldScores = new double[maxGroup];
		} else {
			oldTraits = null;
			oldScores = null;
		}
	}

	@Override
	public boolean mouseHitNode(int hit, boolean alt) {
		if (hit < 0 || hit >= nPopulation || isVacantAt(hit))
			return false; // invalid argument or vacant site
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
		if (vacantIdx >= 0)
			countOccupied();
		return true;
	}
}
