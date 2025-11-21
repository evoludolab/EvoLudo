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
import org.evoludo.simulator.models.IBS.ScoringType;
import org.evoludo.simulator.models.Model.HasIBS;
import org.evoludo.simulator.modules.Continuous;

/**
 * The core class for individual based simulations with a <em>single</em>
 * continuous trait. As compared to the more generic {@link IBSMCPopulation}
 * this subclass takes advantage of simplifications that are possible for a
 * single trait.
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
	final HasIBS.CPairs cpairmodule;

	/**
	 * For group interaction modules {@code module==groupmodule} holds and
	 * {@code null} otherwise. Convenience field to reduce the number of
	 * (unnecessary) casts.
	 * 
	 * @see HasIBS.CGroups
	 */
	final HasIBS.CGroups cgroupmodule;

	/**
	 * Creates a population of individuals with a single continuous trait for IBS
	 * simulations.
	 * 
	 * @param engine the pacemaker for running the model
	 * @param module the module that defines the rules of the game
	 */
	public IBSCPopulation(EvoLudo engine, Continuous module) {
		super(engine, module);
		if (!(pairmodule instanceof HasIBS.CPairs))
			throw new IllegalArgumentException("module must implement HasIBS.CPairs");
		cpairmodule = (HasIBS.CPairs) pairmodule;
		cgroupmodule = (module instanceof HasIBS.CGroups) ? (HasIBS.CGroups) module : null;
	}

	@Override
	public void setOpponentPop(IBSPopulation<?, ?> opponent) {
		if (!(opponent instanceof IBSCPopulation)) {
			throw new IllegalArgumentException("opponent must be IBSCPopulation");
		}
		super.setOpponentPop(opponent);
	}

	@Override
	public boolean haveSameTrait(int a, int b) {
		return (Math.abs(traits[a] - traits[b]) < 1e-8);
	}

	@Override
	public boolean isSameTrait(int a) {
		return (Math.abs(traits[a] - traitsNext[a]) < 1e-8);
	}

	@Override
	public void swapTraits(int a, int b) {
		traitsNext[a] = traits[b];
		traitsNext[b] = traits[a];
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * Continuous modules with multiple traits never get here.
	 * 
	 * @see IBSMCPopulation#playPairGameAt(int)
	 */
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
		double[] opptraits = opponent.traits;
		for (int i = 0; i < group.nSampled; i++)
			tmpGroup[i] = opptraits[group.group[i]];
		double myScore = cpairmodule.pairScores(traits[me], tmpGroup, group.nSampled, groupScores);
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
	 * Continuous modules with multiple traits never get here.
	 * 
	 * @see IBSMCPopulation#adjustPairGameScoresAt(int)
	 */
	@Override
	public void adjustPairGameScoresAt(int me) {
		// gather players
		double[] opptraits = opponent.traits;
		int nIn = 0;
		int nOut = interaction.kout[me];
		int[] in = null;
		int[] out = interaction.out[me];
		for (int n = 0; n < nOut; n++)
			tmpGroup[n] = opptraits[out[n]];
		int u2 = 2;
		if (!interaction.isUndirected()) {
			// directed graph, count in-neighbors
			u2 = 1;
			nIn = interaction.kin[me];
			in = interaction.in[me];
			for (int n = 0; n < nIn; n++)
				tmpGroup[nOut + n] = opptraits[in[n]];
		}
		int nInter = nOut + nIn;
		double oldScore = cpairmodule.pairScores(traits[me], tmpGroup, nInter, oldScores);
		double newScore = cpairmodule.pairScores(traitsNext[me], tmpGroup, nInter, groupScores);
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
		// same as !interaction.isUndirected() because in != null implies directed graph
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

	/**
	 * {@inheritDoc}
	 * <p>
	 * Continuous modules with multiple traits never get here.
	 * 
	 * @see IBSMCPopulation#playGroupGameAt(int)
	 */
	@Override
	public void playGroupGameAt(IBSGroup group) {
		int size = group.nSampled;
		if (size <= 0) {
			updateScoreAt(group.focal, 0.0);
			return;
		}
		double[] opptraits = opponent.traits;
		for (int i = 0; i < size; i++)
			tmpGroup[i] = opptraits[group.group[i]];
		int me = group.focal;

		switch (group.samplingType) {
			// interact with all neighbors - interact repeatedly if nGroup<groupSize+1
			case ALL:
				// interact with all neighbors
				int nGroup = module.getNGroup();
				if (nGroup < group.nSampled + 1) {
					playGroupSequentiallyAt(me, group, nGroup);
					return;
				}
				// interact with full group (random graphs, all neighbors)

				//$FALL-THROUGH$
			case RANDOM:
				// interact with sampled neighbors
				playGroupOnceAt(me, group);
				return;

			default:
				throw new UnsupportedOperationException("Unknown interaction type (" + interGroup.getSampling() + ")");
		}
	}

	@Override
	void playGroupSequentiallyAt(int me, IBSGroup group, int nGroup) {
		// interact with part of group sequentially
		double myScore = 0.0;
		Arrays.fill(smallScores, 0, group.nSampled, 0.0);
		// for ephemeral scores calculate score of focal only
		boolean ephemeralScores = playerScoring.equals(ScoringType.EPHEMERAL);
		for (int n = 0; n < group.nSampled; n++) {
			for (int i = 0; i < nGroup - 1; i++)
				smallTrait[i] = tmpGroup[(n + i) % group.nSampled];
			myScore += cgroupmodule.groupScores(traits[me], smallTrait, nGroup - 1, groupScores);
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

	@Override
	void playGroupOnceAt(int me, IBSGroup group) {
		// interact with full group (random graphs)
		double myScore = cgroupmodule.groupScores(traits[me], tmpGroup, group.nSampled, groupScores);
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
	 * Continuous modules with multiple traits never get here.
	 * 
	 * @see IBSMCPopulation#yalpGroupGameAt(int)
	 */
	@Override
	public void yalpGroupGameAt(IBSGroup group) {
		if (group.nSampled <= 0)
			return;

		double myScore;
		double myTrait = traits[group.focal];
		double[] opptraits = opponent.traits;
		for (int i = 0; i < group.nSampled; i++)
			tmpGroup[i] = opptraits[group.group[i]];

		int nGroup = module.getNGroup();
		if (nGroup < group.nSampled + 1) { // interact with part of group sequentially
			myScore = 0.0;
			Arrays.fill(smallScores, 0, group.nSampled, 0.0);
			for (int n = 0; n < group.nSampled; n++) {
				for (int i = 0; i < nGroup - 1; i++)
					smallTrait[i] = tmpGroup[(n + i) % group.nSampled];
				myScore += cgroupmodule.groupScores(myTrait, smallTrait, nGroup - 1, groupScores);
				for (int i = 0; i < nGroup - 1; i++)
					smallScores[(n + i) % group.nSampled] += groupScores[i];
			}
			removeScoreAt(group.focal, myScore, group.nSampled);
			for (int i = 0; i < group.nSampled; i++)
				opponent.removeScoreAt(group.group[i], smallScores[i], nGroup - 1);
			return;
		}
		// interact with full group (random graphs)
		myScore = cgroupmodule.groupScores(myTrait, tmpGroup, group.nSampled, groupScores);
		removeScoreAt(group.focal, myScore);

		for (int i = 0; i < group.nSampled; i++)
			opponent.removeScoreAt(group.group[i], groupScores[i]);
	}

	@Override
	public void prepareTraits() {
		System.arraycopy(traits, 0, traitsNext, 0, nPopulation);
	}

	@Override
	public void commitTraitAt(int me) {
		traits[me] = traitsNext[me];
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
