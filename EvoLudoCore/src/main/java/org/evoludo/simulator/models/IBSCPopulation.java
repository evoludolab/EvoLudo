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

import org.evoludo.simulator.EvoLudo;
import org.evoludo.simulator.models.IBS.HasIBS;
import org.evoludo.simulator.models.IBS.ScoringType;
import org.evoludo.simulator.modules.Continuous;

/**
 * The core class for individual based simulations with a <em>single</em>
 * continuous trait/strategy. As compared to the more generic
 * {@link IBSMCPopulation} this subclass takes advantage of simplifications that
 * are possible for a single trait/strategy.
 * 
 * @author Christoph Hauert
 * 
 * @see IBSPopulation
 * @see IBSMCPopulation
 */
public class IBSCPopulation extends IBSMCPopulation {

	/**
	 * For pairwise interaction modules {@code module==pairmodule} holds and
	 * {@code null} otherwise. Convenience field to reduce the number of
	 * (unnecessary) casts.
	 * 
	 * @see HasIBS.CPairs
	 */
	@SuppressWarnings("hiding")
	protected HasIBS.CPairs pairmodule;

	/**
	 * For group interaction modules {@code module==groupmodule} holds and
	 * {@code null} otherwise. Convenience field to reduce the number of
	 * (unnecessary) casts.
	 * 
	 * @see HasIBS.CGroups
	 */
	@SuppressWarnings("hiding")
	protected HasIBS.CGroups groupmodule;

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
	IBSCPopulation opponent;

	/**
	 * Creates a population of individuals with a single continuous trait for IBS
	 * simulations.
	 * 
	 * @param engine the pacemaker for running the model
	 * @param module the module that defines the rules of the game
	 */
	public IBSCPopulation(EvoLudo engine, Continuous module) {
		super(engine, module);
		opponent = this;
		if (module instanceof HasIBS.CPairs)
			pairmodule = (HasIBS.CPairs) module;
		if (module instanceof HasIBS.CGroups)
			groupmodule = (HasIBS.CGroups) module;
	}

	@Override
	public void setOpponentPop(IBSPopulation opponent) {
		super.setOpponentPop(opponent);
		this.opponent = (IBSCPopulation) super.opponent;
	}

	@Override
	public boolean haveSameStrategy(int a, int b) {
		return (Math.abs(strategies[a] - strategies[b]) < 1e-8);
	}

	@Override
	public boolean isSameStrategy(int a) {
		return (Math.abs(strategies[a] - strategiesScratch[a]) < 1e-8);
	}

	@Override
	public void swapStrategies(int a, int b) {
		strategiesScratch[a] = strategies[b];
		strategiesScratch[b] = strategies[a];
	}

	@Override
	public void playPairGameAt(IBSGroup group) {
		int me = group.focal;
		// for ephemeral scores calculate score of focal only
		boolean ephemeralScores = playerScoring.equals(ScoringType.EPHEMERAL);
		if (group.nSampled <= 0) {
			if (ephemeralScores) {
				// no need to update scores of everyone else
				resetScoreAt(me);
				return;
			}
			updateScoreAt(me, 0.0);
			return;
		}
		double[] oppstrategies = opponent.strategies;
		for (int i = 0; i < group.nSampled; i++)
			groupStrat[i] = oppstrategies[group.group[i]];
		double myScore = pairmodule.pairScores(strategies[me], groupStrat, group.nSampled, groupScores);
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
		double[] oppstrategies = opponent.strategies;
		int nIn = 0, nOut = interaction.kout[me];
		int[] in = null, out = interaction.out[me];
		for (int n = 0; n < nOut; n++)
			groupStrat[n] = oppstrategies[out[n]];
		int u2 = 2;
		if (!interaction.isUndirected) {
			// directed graph, count in-neighbors
			u2 = 1;
			nIn = interaction.kin[me];
			in = interaction.in[me];
			for (int n = 0; n < nIn; n++)
				groupStrat[nOut + n] = oppstrategies[in[n]];
		}
		int nInter = nOut + nIn;
		double oldScore = pairmodule.pairScores(strategies[me], groupStrat, nInter, oldScores);
		double newScore = pairmodule.pairScores(strategiesScratch[me], groupStrat, nInter, groupScores);
		commitStrategyAt(me);
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
		int size = group.nSampled;
		if (size <= 0) {
			updateScoreAt(group.focal, 0.0);
			return;
		}
		double[] oppstrategies = opponent.strategies;
		for (int i = 0; i < size; i++)
			groupStrat[i] = oppstrategies[group.group[i]];
		int me = group.focal;
		double myStrat = strategies[me];
		// for ephemeral scores calculate score of focal only
		boolean ephemeralScores = playerScoring.equals(ScoringType.EPHEMERAL);

		switch (group.samplingType) {
			// interact with all neighbors - interact repeatedly if nGroup<groupSize+1
			case ALL:
				int nGroup = module.getNGroup();
				if (nGroup < group.nSampled + 1) {
					// interact with part of group sequentially
					double myScore = 0.0;
					Arrays.fill(smallScores, 0, group.nSampled, 0.0);
					for (int n = 0; n < group.nSampled; n++) {
						for (int i = 0; i < nGroup - 1; i++)
							smallStrat[i] = groupStrat[(n + i) % group.nSampled];
						myScore += groupmodule.groupScores(myStrat, smallStrat, nGroup - 1, groupScores);
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
				double myScore = groupmodule.groupScores(myStrat, groupStrat, group.nSampled, groupScores);
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
		if (group.nSampled <= 0)
			return;

		double myScore;
		double myStrat = strategies[group.focal];
		double[] oppstrategies = opponent.strategies;
		for (int i = 0; i < group.nSampled; i++)
			groupStrat[i] = oppstrategies[group.group[i]];

		int nGroup = module.getNGroup();
		if (nGroup < group.nSampled + 1) { // interact with part of group sequentially
			myScore = 0.0;
			Arrays.fill(smallScores, 0, group.nSampled, 0.0);
			for (int n = 0; n < group.nSampled; n++) {
				for (int i = 0; i < nGroup - 1; i++)
					smallStrat[i] = groupStrat[(n + i) % group.nSampled];
				myScore += groupmodule.groupScores(myStrat, smallStrat, nGroup - 1, groupScores);
				for (int i = 0; i < nGroup - 1; i++)
					smallScores[(n + i) % group.nSampled] += groupScores[i];
			}
			removeScoreAt(group.focal, myScore, group.nSampled);
			for (int i = 0; i < group.nSampled; i++)
				opponent.removeScoreAt(group.group[i], smallScores[i], nGroup - 1);
			return;
		}
		// interact with full group (random graphs)
		myScore = groupmodule.groupScores(myStrat, groupStrat, group.nSampled, groupScores);
		removeScoreAt(group.focal, myScore);

		for (int i = 0; i < group.nSampled; i++)
			opponent.removeScoreAt(group.group[i], groupScores[i]);
	}

	@Override
	public void prepareStrategies() {
		System.arraycopy(strategies, 0, strategiesScratch, 0, nPopulation);
	}

	@Override
	public void commitStrategyAt(int me) {
		strategies[me] = strategiesScratch[me];
	}

	/**
	 * Creates a histogram for {@code trait} and returns the result in the array
	 * <code>bins</code>.
	 *
	 * @param bins the array to store the histogram(s)
	 */
	public void getTraitHistogramData(double[] bins) {
		getTraitHistogramData(bins, 0);
	}
}
