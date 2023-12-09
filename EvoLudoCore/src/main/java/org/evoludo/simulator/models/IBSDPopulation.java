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

import java.util.Arrays;
import java.util.List;

import org.evoludo.math.ArrayMath;
import org.evoludo.math.Combinatorics;
import org.evoludo.simulator.ColorMap;
import org.evoludo.simulator.EvoLudo;
import org.evoludo.simulator.Geometry;
import org.evoludo.simulator.models.IBS.ScoringType;
import org.evoludo.simulator.models.IBSD.FixationData;
import org.evoludo.simulator.models.IBSD.InitType;
import org.evoludo.simulator.models.Model.Mode;
import org.evoludo.simulator.modules.Discrete;
import org.evoludo.simulator.views.HasHistogram;
import org.evoludo.util.Formatter;
import org.evoludo.util.Plist;

/**
 * A minimalstic helper class (or data structure) to represent a single
 * <em>directed</em> link in the network structure. <em>Undirected</em> links
 * are represented by two directed ones. This is used for Moran optimizations.
 *
 * @author Christoph Hauert
 * 
 * @see IBSDPopulation#optimizeMoran
 */
class link {
	/**
	 * The index of the individual at the tail end of the link.
	 */
	int source;

	/**
	 * The index of the individual at the head of the link.
	 */
	int destination;

	/**
	 * The fitness of the individual at the tail end (for fitness based picking of
	 * individuals).
	 */
	double fitness;
}

/**
 * The core class for individual based simulations with discrete
 * traits/strategies. Manages the strategies of the population,
 * while delegating the management of the population and individual fitness as
 * well as simulation steps to super.
 *
 * @author Christoph Hauert
 * 
 * @see IBSPopulation
 */
public class IBSDPopulation extends IBSPopulation {

	/**
	 * The discrete module associated with this model.
	 * <p>
	 * <strong>Note:</strong> This deliberately hides {@link IBSPopulation#module}.
	 * The two variables point to the same object but this setup avoids unnecessary
	 * casts because only {@link Discrete} modules generate
	 * {@code IBSDPopulation}(s).
	 */
	@SuppressWarnings("hiding")
	protected Discrete module;

	/**
	 * For pairwise interaction modules {@code module==pairmodule} holds and
	 * {@code null} otherwise. Convenience field to reduce the number of
	 * (unnecessary) casts.
	 * 
	 * @see Discrete.Pairs
	 */
	protected Discrete.Pairs pairmodule;

	/**
	 * For group interaction modules {@code module==groupmodule} holds and
	 * {@code null} otherwise. Convenience field to reduce the number of
	 * (unnecessary) casts.
	 * 
	 * @see Discrete.Groups
	 */
	protected Discrete.Groups groupmodule;

	/**
	 * The interaction partner/opponent of this population
	 * {@code opponent.getModule()==getModule().getOpponent()}. In intra-species
	 * interactions {@code opponent==this}. Convenience field.
	 * <p>
	 * <strong>Note:</strong> This deliberately hides
	 * {@link IBSPopulation#opponent}. The two variables point to the same object
	 * but this setup avoids unnecessary casts because only {@link Discrete} modules
	 * generate {@code IBSDPopulation}(s).
	 */
	@SuppressWarnings("hiding")
	IBSDPopulation opponent;

	/**
	 * The flag to indicate whether optimizations of Moran processes are requested.
	 * <code>true</code> if optimizations for Moran process requested.
	 * 
	 * <h3>Note:</h3>
	 * <ol>
	 * <li>Optimizations are requested with the command line option
	 * <code>--optimize</code> (or <code>-1</code>), see
	 * {@link IBSD#cloOptimize}.
	 * <li>Optimizations destroy the time line of events. Do not use if e.g.
	 * fixation times are of interest (but fine for fixation probabilities).
	 * <li>Currently restricted to discrete strategies and structured populations,
	 * where Moran type processes can be optimized by skipping events involving
	 * individuals of the same type (see
	 * {@link IBSDPopulation#updatePlayerMoran(int, int)}).
	 * </ol>
	 */
	protected boolean optimizeMoran = false;

	/**
	 * Creates a population of individuals with discrete traits for IBS simulations.
	 * 
	 * @param engine the pacemeaker for running the model
	 */
	public IBSDPopulation(EvoLudo engine) {
		super(engine);
	}

	@Override
	public void load() {
		super.load();
		// deal with module cast - pairmodule and groupmodule have to wait because
		// nGroup requires parsing of command line options (see check())
		// important: cannot deal with casting shadowed opponent here because for 
		// mutli-species modules all species need to be loaded first.
		module = (Discrete) super.module;
	}

	@Override
	public void unload() {
		super.unload();
		accuTypeScores = null;
		strategiesTypeCount = null;
		activeLinks = null;
		strategies = null;
		strategiesScratch = null;
		groupStrat = null;
		groupIdxs = null;
		smallStrat = null;
		traitCount = null;
		traitScore = null;
		traitTempScore = null;
		module = null;
		pairmodule = null;
		groupmodule = null;
	}

	@Override
	public boolean permitsMode(Mode testmode) {
		boolean modeOK = super.permitsMode(testmode);
		if (modeOK && (module instanceof HasHistogram.StatisticsProbability
				|| module instanceof HasHistogram.StatisticsTime))
			return true;
		return false;
	}

	/**
	 * The array of individual strategies.
	 */
	public int[] strategies;

	/**
	 * The array for temporarily storing strategies during updates.
	 */
	protected int[] strategiesScratch;

	/**
	 * The array with the total scores for each trait/strategic type.
	 */
	protected double[] accuTypeScores;

	/**
	 * The array with the total number of individuals of each trait/strategic type.
	 */
	public int[] strategiesTypeCount;

	@Override
	public int getPopulationSize() {
		if (VACANT < 0)
			return nPopulation;
		return nPopulation - strategiesTypeCount[VACANT];
	}

	/**
	 * The array to keep track of all links along which a change in the population
	 * composition may occur. This is used for optimized Moran updating.
	 * 
	 * @see #optimizeMoran
	 */
	private link[] activeLinks;

	/**
	 * {@inheritDoc}
	 * <p>
	 * Allocates memory for optimized Moran process.
	 * 
	 * <h3>Discussions/extensions</h3>
	 * {@code groupScores} unused in IBSDPopulation. Merge with {@code traitScore}?
	 * 
	 * @see #optimizeMoran
	 * @see #updatePlayerMoranBirthDeath()
	 */
	@Override
	public synchronized void reset() {
		super.reset();
		if (optimizeMoran && populationUpdateType.isMoran()) {
			int nLinks = (int) (reproduction.avgOut * nPopulation + 0.5);
			if (activeLinks == null || activeLinks.length != nLinks) {
				activeLinks = new link[nLinks];
				for (int n = 0; n < nLinks; n++)
					activeLinks[n] = new link();
			}
		} else {
			// conserve memory
			activeLinks = null;
		}
		if (strategies == null || strategies.length != nPopulation)
			strategies = new int[nPopulation];
		if (strategiesScratch == null || strategiesScratch.length != nPopulation)
			strategiesScratch = new int[nPopulation];
		// groupScores have the same maximum length
		int maxGroup = groupScores.length;
		if (groupStrat == null || groupStrat.length != maxGroup)
			groupStrat = new int[maxGroup];
		if (groupIdxs == null || groupIdxs.length != maxGroup)
			groupIdxs = new int[maxGroup];
		if (smallStrat == null || smallStrat.length != maxGroup)
			smallStrat = new int[maxGroup];
		if (traitCount == null || traitCount.length != nTraits)
			traitCount = new int[nTraits];
		if (traitScore == null || traitScore.length != nTraits)
			traitScore = new double[nTraits];
		if (accuTypeScores == null || accuTypeScores.length != nTraits)
			accuTypeScores = new double[nTraits];
		if (strategiesTypeCount == null || strategiesTypeCount.length != nTraits)
			strategiesTypeCount = new int[nTraits];
		// best-response may require temporary memory - this is peanuts, just reserve it
		if (traitTempScore == null || traitTempScore.length != nTraits)
			traitTempScore = new double[nTraits];
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * Optimized Moran process: a significant speed boost is achieved when
	 * restricting events along links that potentially result in an actual change of
	 * the population composition, i.e. by focussing on those links that connect
	 * individuals of different strategies/traits. This destroys the time scale.
	 * 
	 * @see #optimizeMoran
	 */
	@Override
	public void updatePlayerMoranBirthDeath() {
		if (!optimizeMoran) {
			super.updatePlayerMoranBirthDeath();
			return;
		}
		// for two strategies, we need to consider only the nodes of the rarer type but
		// both in- as well as out-links
		if (nTraits == 2) {
			int rareType = (strategiesTypeCount[1] < strategiesTypeCount[0] ? 1 : 0);

			// create list of active links
			// birth-death: keep track of all nodes that have different downstream neighbors
			int nact = 0;
			double totscore = 0.0;
			for (int n = 0; n < nPopulation; n++) {
				int type = strategies[n] % nTraits;
				if (type != rareType)
					continue;
				// check out-neighbors
				int[] neighs = reproduction.out[n];
				int no = reproduction.kout[n];
				for (int i = 0; i < no; i++) {
					int aneigh = neighs[i];
					if (strategies[aneigh] % nTraits == type)
						continue;
					activeLinks[nact].source = n;
					activeLinks[nact].destination = aneigh;
					double ascore = getFitnessAt(n) / no;
					activeLinks[nact].fitness = ascore;
					totscore += ascore;
					nact++;
				}
				// check in-neighbors
				neighs = reproduction.in[n];
				int ni = reproduction.kin[n];
				for (int i = 0; i < ni; i++) {
					int aneigh = neighs[i];
					if (strategies[aneigh] % nTraits == type)
						continue;
					activeLinks[nact].source = aneigh;
					activeLinks[nact].destination = n;
					double ascore = getFitnessAt(aneigh) / (reproduction.kout[aneigh]);
					activeLinks[nact].fitness = ascore;
					totscore += ascore;
					nact++;
				}
			}
			if (nact == 0)
				return; // nothing to do!

			double hit = random01() * totscore;
			for (int i = 0; i < nact; i++) {
				hit -= activeLinks[i].fitness;
				if (hit <= 0.0) {
					debugFocal = activeLinks[i].destination;
					debugTarget = activeLinks[i].source;
					updatePlayerMoran(debugTarget, debugFocal);
					break;
				}
			}
			return;
		}

		// default, more than two strategies - check all nodes
		// create list of active links
		// birth-death: keep track of all nodes that have different downstream neighbors
		int nact = 0;
		double totscore = 0.0;
		for (int n = 0; n < nPopulation; n++) {
			int type = strategies[n] % nTraits;
			int[] neighs = reproduction.out[n];
			int nn = reproduction.kout[n];
			for (int i = 0; i < nn; i++) {
				int aneigh = neighs[i];
				if (strategies[aneigh] % nTraits == type)
					continue;
				activeLinks[nact].source = n;
				activeLinks[nact].destination = aneigh;
				double ascore = getFitnessAt(n) / nn;
				activeLinks[nact].fitness = ascore;
				totscore += ascore;
				nact++;
			}
		}
		if (nact == 0)
			return; // nothing to do!

		double hit = random01() * totscore;
		for (int i = 0; i < nact; i++) {
			hit -= activeLinks[i].fitness;
			if (hit <= 0.0) {
				debugFocal = activeLinks[i].destination;
				debugTarget = activeLinks[i].source;
				updatePlayerMoran(debugTarget, debugFocal);
				break;
			}
		}
	}

	@Override
	public void updatePlayerMoranImitate() {
		updatePlayerMoranDeathBirth(true);
	}

	@Override
	public void updatePlayerMoranDeathBirth() {
		updatePlayerMoranDeathBirth(false);
	}

	/**
	 * The optimized Moran process for death-Birth and imitation updatating. A
	 * significant speed boost is achieved when restricting events along links that
	 * potentially result in an actual change of the population composition, i.e. by
	 * focussing on those links that connect individuals of different
	 * strategies/traits. This destroys the time scale.
	 * 
	 * @param withSelf the flag to indicate whether to include to focal individual
	 * 
	 * @see #optimizeMoran
	 * @see IBSPopulation#updatePlayerMoranDeathBirth()
	 * @see IBSPopulation#updatePlayerMoranImitate()
	 */
	protected void updatePlayerMoranDeathBirth(boolean withSelf) {
		if (!optimizeMoran) {
			super.updatePlayerMoranDeathBirth();
			return;
		}
		// create list of active links
		// death-birth: keep track of all nodes that have at least one different
		// upstream neighbor
		int nact = 0;
		double totscore = 0.0;
		for (int n = 0; n < nPopulation; n++) {
			int type = strategies[n] % nTraits;
			int[] neighs = reproduction.in[n];
			int nn = reproduction.kin[n];
			double nodescore = withSelf ? getFitnessAt(n) : 0.0;
			int count = 0;
			for (int i = 0; i < nn; i++) {
				int aneigh = neighs[i];
				double ascore = getFitnessAt(aneigh);
				nodescore += ascore;
				if (strategies[aneigh] % nTraits == type)
					continue;
				activeLinks[nact + count].source = aneigh;
				activeLinks[nact + count].destination = n;
				activeLinks[nact + count].fitness = ascore;
				count++;
			}
			for (int i = 0; i < count; i++) {
				activeLinks[nact + i].fitness /= nodescore;
				totscore += activeLinks[nact + i].fitness;
			}
			nact += count;
		}
		if (nact == 0)
			return; // nothing to do!

		double hit = random01() * totscore;
		for (int i = 0; i < nact; i++) {
			hit -= activeLinks[i].fitness;
			if (hit <= 0.0) {
				debugFocal = activeLinks[i].source;
				debugTarget = activeLinks[i].destination;
				updatePlayerMoran(debugFocal, debugTarget);
				break;
			}
		}
	}

	/**
	 * Perform a single ecological update of the site with index {@code me}:
	 * <ol>
	 * <li>If focal site is empty, draw random neighbour and, if occupied, place
	 * clonal offspring on focal site with probability proportional to fitness.
	 * <li>If focal site is occupied, individual dies with constant probability.
	 * </ol>
	 *
	 * @param me the index of the focal individual
	 * @return time increment
	 */
	@Override
	protected double updatePlayerEcologyAt(int me) {
		// NOTE: review real time increments - now that the process is adapted to rates
		// (instead of probabilities)
		if (isVacantAt(me)) {
			// 'me' is vacant
			debugTarget = pickNeighborSiteAt(me);
			int nStrat = strategies[debugTarget] % nTraits;
			if (nStrat == VACANT)
				return realtimeIncr;
			// neighbour is occupied - check if focal remains vacant; compare score against
			// average population score
			if (random01() < getFitnessAt(debugTarget) * realtimeIncr) {
				// produce offspring
				updateStrategyAt(me, nStrat);
				// check for mutations
				double pMutation = module.getMutationProb();
				if (pMutation > 0.0 && random01() < pMutation)
					mutateStrategyAt(me, true);
				if (module.isStatic()) {
					commitStrategyAt(me);
					return realtimeIncr;
				}
				if (adjustScores) {
					// strategies must not be committed for adjust-routines
					adjustGameScoresAt(me);
					return realtimeIncr;
				}
				resetScoreAt(me);
				commitStrategyAt(me);
				playGameAt(me);
			}
			return realtimeIncr;
		}
		// 'me' is occupied - vacate site with fixed probability
		debugTarget = -1;
		if (random01() < module.getDeathRate() * realtimeIncr) {
			// vacate focal site
			updateStrategyAt(me, VACANT);
			if (module.isStatic()) {
				commitStrategyAt(me);
				return realtimeIncr;
			}
			if (adjustScores) {
				// strategies must not be committed for adjust-routines
				adjustGameScoresAt(me);
				return realtimeIncr;
			}
			resetScoreAt(me);
			commitStrategyAt(me);
		}
		return realtimeIncr;
	}

	@Override
	public void updateFromModelAt(int index, int modelPlayer) {
		super.updateFromModelAt(index, modelPlayer); // deal with tags
		int newstrat = strategies[modelPlayer] % nTraits;
		updateStrategyAt(index, newstrat);
	}

	@Override
	public void mutateStrategyAt(int index, boolean changed) {
		int aStrat = (changed ? strategiesScratch[index] : strategies[index]) % nTraits;
		if (aStrat == VACANT)
			return;
		int nActive = module.getNActive();
		if (nActive == nTraits) {
			if (VACANT < 0) {
				aStrat = (aStrat + random0n(nTraits - 1) + 1) % nTraits;
				updateStrategyAt(index, aStrat);
				return;
			}
			aStrat = (aStrat + random0n(nTraits - 2) + 1) % nTraits;
			if (aStrat == VACANT)
				aStrat = (aStrat + 1) % nTraits;
			updateStrategyAt(index, aStrat);
			return;
		}
		if (nActive <= 1)
			return; // nothing to mutate
		boolean[] active = module.getActiveTraits();
		if (VACANT < 0) {
			int rand = random0n(nActive - 1);
			aStrat = (aStrat + rand + 1) % nTraits;
			int idx = 0;
			while (idx <= rand) {
				int type = (aStrat + idx) % nTraits;
				if (active != null && !active[type]) {
					aStrat = (aStrat + 1) % nTraits;
					continue;
				}
				idx++;
			}
			updateStrategyAt(index, aStrat);
			return;
		}
		if (nActive <= 2)
			return; // nothing to mutate
		int rand = random0n(nActive - 2);
		aStrat = (aStrat + rand + 1) % nTraits;
		int idx = 0;
		while (idx <= rand) {
			int type = (aStrat + idx) % nTraits;
			if ((active != null && !active[type]) || type == VACANT) {
				aStrat = (aStrat + 1) % nTraits;
				continue;
			}
			idx++;
		}
		updateStrategyAt(index, aStrat);
		return;
	}

	@Override
	protected void debugMarkChange() {
		super.debugMarkChange(); // for logging of update
		if (debugFocal >= 0)
			strategies[debugFocal] = (strategies[debugFocal] % nTraits) + nTraits;
		if (debugTarget >= 0)
			strategies[debugTarget] = (strategies[debugTarget] % nTraits) + nTraits;
		if (debugNModels > 0) {
			for (int n = 0; n < debugNModels; n++) {
				int idx = debugModels[n];
				strategies[idx] = (strategies[idx] % nTraits) + nTraits;
			}
		}
	}

	/**
	 * Update the trait/strategy of individual with index {@code index} to the new
	 * trait/strategy {@code newstrat}.
	 * 
	 * @param index    the index of the individual
	 * @param newstrat the new trait/strategy of the individual
	 */
	protected void updateStrategyAt(int index, int newstrat) {
		int oldstrat = strategies[index] % nTraits;
		if (newstrat != oldstrat)
			newstrat += nTraits;
		strategiesScratch[index] = newstrat;
	}

	@Override
	public boolean haveSameStrategy(int a, int b) {
		return ((strategies[a] % nTraits) == (strategies[b] % nTraits));
	}

	@Override
	public boolean isSameStrategy(int a) {
		return ((strategies[a] % nTraits) == (strategiesScratch[a] % nTraits));
	}

	@Override
	public void swapStrategies(int a, int b) {
		strategiesScratch[a] = strategies[b];
		strategiesScratch[b] = strategies[a];
	}

	@Override
	public void updateScoreAt(int index, double newscore, int incr) {
		int type = strategies[index] % nTraits;
		// since site at index has not changed, nothing needs to be done if it is vacant
		if (type == VACANT)
			return;
		accuTypeScores[type] -= getScoreAt(index);
		super.updateScoreAt(index, newscore, incr);
		accuTypeScores[type] += getScoreAt(index);
	}

	@Override
	public void setScoreAt(int index, double newscore, int inter) {
		super.setScoreAt(index, newscore, inter);
		int type = strategies[index] % nTraits;
		if (type == VACANT)
			return;
		accuTypeScores[type] += getScoreAt(index);
	}

	@Override
	public double getScoreAt(int idx) {
		if (hasLookupTable) {
			return typeScores[strategies[idx] % nTraits];
		}
		return scores[idx];
	}

	@Override
	public double getFitnessAt(int idx) {
		if (hasLookupTable) {
			return typeFitness[strategies[idx] % nTraits];
		}
		return fitness[idx];
	}

	// private void checkScores(String loc, int idx, boolean committed) {
	// int[] tcount = new int[nTraits];
	// double[] tscores = new double[nTraits];
	// if( VACANT>=0 ) tscores[VACANT] = Double.NaN;
	// // check accounting (sumScores, accuTypeScores and strategiesTypeCount)
	// for(int i=0; i<nPopulation; i++ ) {
	// int si = strategies[i]%nTraits;
	// // strategyTypeCount not yet updated if strategy not committed
	// tcount[si]++;
	// // strategies not yet committed
	// if( !committed && i==idx ) si = strategiesScratch[i]%nTraits;
	// if( !Double.isNaN(scores[i]) ) {
	// // ignore score if i used to be vacant
	// if( playerScoreAveraged ) tscores[si] += scores[i];
	// else tscores[si] +=
	// (si==VACANT?scores[i]:playerEffBaseFit+(scores[i]-playerEffBaseFit)*interactions[i]);
	// }
	// }
	// double tot = 0.0;
	// for( int n=0; n<nTraits; n++ ) if( n!=VACANT ) tot += tscores[n];
	// boolean hasIssue = Math.abs(tot-sumScores)>1e-6;
	// for( int n=0; n<nTraits; n++ ) {
	// if( tcount[n]!=strategiesTypeCount[n] || (n!=VACANT &&
	// Math.abs(tscores[n]-accuTypeScores[n])>1e-6) ) {
	// hasIssue = true;
	// break;
	// }
	// }
	// if( hasIssue )
	// GWT.log("* "+loc+" @ "+ChHFormatter.format(generation, 2)+":
	// tcount="+tcount+" ("+strategiesTypeCount+"), tscores="+tscores+
	// " ("+accuTypeScores+") -> "+tot+" ("+sumScores+")");
	// else
	// GWT.log(" "+loc+": tcount="+tcount+" ("+strategiesTypeCount+"),
	// tscores="+tscores+" ("+accuTypeScores+") -> "+tot+" ("+sumScores+")");
	//
	// //check min/max index stuff
	// if( isSynchronous ) return;
	// int minIdx = minEffScoreIdx;
	// int maxIdx = maxEffScoreIdx;
	// setMinEffScoreIdx();
	// setMaxEffScoreIdx();
	// // indices may be different but the reference scores should be the same
	// if( Math.abs(getFitnessAt(minIdx)-getFitnessAt(minEffScoreIdx))>1e-8 ||
	// Math.abs(getFitnessAt(maxIdx)-getFitnessAt(maxEffScoreIdx))>1e-8 ) {
	// GWT.log("* "+loc+" @ "+ChHFormatter.format(generation, 2)+": index="+idx+",
	// minIdx="+minIdx+", maxIdx="+maxIdx+
	// ", minscore="+getFitnessAt(minEffScoreIdx)+" ("+getFitnessAt(minIdx)+"),
	// maxscore="+getFitnessAt(maxEffScoreIdx)+
	// " ("+getFitnessAt(maxIdx)+")");
	// }

	// double[] strat = new double[nTraits];
	// double[] sstrat = new double[nTraits];
	// double[] score = new double[nTraits];
	// for(int i=0; i<nPopulation; i++ ) {
	// int si = strategies[i]%nTraits;
	// strat[si]++;
	// score[si] += scores[i];
	// sstrat[strategiesScratch[i]%nTraits]++;
	// }
	// if( idx>=0 ) {
	// strat[strategies[idx]%nTraits]--;
	// strat[strategiesScratch[idx]%nTraits]++;
	// }
	// if( VACANT>=0 && (score[VACANT]<0 || accuTypeScores[VACANT]<0) ) {
	// GWT.log(loc+" (vacant): strat="+strat+" ("+strategiesTypeCount+"),
	// score="+score+" ("+accuTypeScores+"), scratch="+sstrat);
	// if( idx>=0 )
	// GWT.log("idx="+idx+", strategies="+strategies[idx]+",
	// scratch="+strategiesScratch[idx]+", scores="+scores[idx]);
	// return;
	// }
	// for( int i=0; i<nTraits; i++ ) {
	// if( Math.abs(score[i]-accuTypeScores[i])>1e-6 ) {
	// GWT.log(loc+" "+i+": strat="+strat+" ("+strategiesTypeCount+"),
	// score="+score+" ("+accuTypeScores+"), scratch="+sstrat);
	// if( idx>=0 )
	// GWT.log("idx="+idx+", strategies="+strategies[idx]+",
	// scratch="+strategiesScratch[idx]+", scores="+scores[idx]);
	// return;
	// }
	// }
	// }

	@Override
	public void resetScoreAt(int index) {
		accuTypeScores[strategies[index] % nTraits] -= getScoreAt(index);
		super.resetScoreAt(index);
		accuTypeScores[strategiesScratch[index] % nTraits] += getScoreAt(index);
	}

	@Override
	public void resetScores() {
		// constant selection admits for shortcuts (updateScores() takes care of this)
		if (module.isStatic())
			return;

		super.resetScores();
		Arrays.fill(accuTypeScores, 0.0);
		if (VACANT >= 0)
			accuTypeScores[VACANT] = Double.NaN;
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * <strong>Note:</strong> Overridden to set scores in well-mixed populations
	 * more efficiently.
	 */
	@Override
	public void updateScores() {
		// constant selection admits for some shortcuts
		if (module.isStatic()) {
			sumFitness = 0.0;
			for (int n = 0; n < nTraits; n++) {
				if (n == VACANT) {
					accuTypeScores[n] = Double.NaN;
					continue;
				}
				double count = strategiesTypeCount[n];
				accuTypeScores[n] = count * typeScores[n];
				sumFitness += count * typeFitness[n];
			}
			return;
		}
		// lookup tables in well-mixed populations help to speed up payoff calculations.
		// similarly, payoffs can be calculated more efficiently in deme structured
		// populations as long as demes are well-mixed (although lookup tables are
		// possible but not (yet) implemented.
		if (hasLookupTable || (adjustScores && interaction.isType(Geometry.Type.HIERARCHY)
				&& interaction.subgeometry == Geometry.Type.MEANFIELD)) {
			updateMixedMeanScores();
			return;
		}
		// original procedure
		super.updateScores();
	}

	// @Override
	// public void debugCheck(String info) {
	// if( modelType!=Model.SIMULATION ) return;
	//
	// boolean failed = false;
	// // check strategiesTypeCount
	// int[] type = new int[nTraits];
	// for( int n=0; n<nPopulation; n++ ) type[strategies[n]%nTraits]++;
	// for( int n=0; n<nTraits; n++ )
	// if( type[n]!=strategiesTypeCount[n] ) {
	// GWT.log(info+" - strategiesTypeCount["+n+"] mismatch:
	// "+strategiesTypeCount[n]+" should be "+type[n]);
	// failed = true;
	// }
	//
	// // check sumScores
	// double sum = ChHMath.norm(scores);
	// if( Math.abs(sumScores-sum)>1e-6 ) {
	// GWT.log(info+" - sumScores mismatch: "+sumScores+" should be "+sum);
	// failed = true;
	// }
	//
	// // check accuTypeScores
	// double[] accu = new double[nTraits];
	// for( int n=0; n<nPopulation; n++ ) accu[strategies[n]%nTraits] += scores[n];
	// for( int n=0; n<nTraits; n++ )
	// if( Math.abs(accu[n]-accuTypeScores[n])>1e-6 ) {
	// GWT.log(info+" - accuTypeScores["+n+"] mismatch: "+accuTypeScores[n]+" should
	// be "+accu[n]);
	// failed = true;
	// }
	//
	// if( !failed )
	// GWT.log(info+" - check passed");
	// }

	/**
	 * {@inheritDoc}
	 * <p>
	 * <strong>Note:</strong> Takes composition of entire population into account
	 * for {@code Geometry.Type#MEANFIELD} but only the reference neighborhood in
	 * structured populations.
	 */
	@Override
	public boolean updatePlayerBestResponse(int me, int[] group, int size) {
		// neutral case: there is no best-response -> nothing happens
		if (isNeutral)
			return false;

		// mytype and active refer to focal species, while interaction partners
		// to opponent species
		int mytype = strategies[me] % nTraits;
		boolean[] active = module.getActiveTraits();
		// constant selection: simply choose active strategy with highest payoff
		if (module.isStatic()) {
			double max = typeScores[mytype]; // assumes mytype is active
			int idx = mytype;
			for (int n = 0; n < nTraits; n++) {
				if (!active[n])
					continue;
				// note: if my payoff and highest payoff are tied, keep type
				if (typeScores[n] > max) {
					max = typeScores[n];
					idx = n;
				}
			}
			if (idx != mytype) {
				updateStrategyAt(me, idx);
				return true;
			}
			return false;
		}

		// frequency dependent selection: determine active strategy with highest payoff
		if (reproduction.isType(Geometry.Type.MEANFIELD)) {
			// well-mixed
			System.arraycopy(opponent.strategiesTypeCount, 0, traitCount, 0, nTraits);
			if (interaction.isInterspecies()) {
				// inter-species: focal individual not part of opponents
				if (module.isPairwise())
					pairmodule.mixedScores(traitCount, traitScore);
				else
					groupmodule.mixedScores(traitCount, module.getNGroup(), traitScore);
			} else {
				// intra-species: remove focal individual and evaluate performance of all active
				// strategies
				traitCount[mytype]--;
				for (int n = 0; n < nTraits; n++) {
					if (!active[n]) {
						traitScore[n] = -Double.MAX_VALUE;
						continue;
					}
					// add candidate strategy to the mix
					traitCount[n]++;
					if (module.isPairwise())
						pairmodule.mixedScores(traitCount, traitTempScore);
					else
						groupmodule.mixedScores(traitCount, module.getNGroup(), traitTempScore);
					traitScore[n] = traitTempScore[n];
					traitCount[n]--;
				}
			}
		} else {
			// structured
			if (interaction.isInterspecies()) {
				// inter-species: focal individual not part of opponents but include it's
				// counterpart in opponent population
				// DANGER: following passes in GWT but potentially and permanently changes the
				// structure by adding an element to group.group!!! JRE is less lenient.
				// refGroup[rGroupSize++] = me;
				size = stripVacancies(group, size, groupStrat, groupIdxs);
				countTraits(traitCount, groupStrat, 0, size);
				// RESOLUTION: instead if mytype is not VACANT add it to trait count
				if (mytype != VACANT)
					traitCount[mytype]++;
				if (module.isPairwise())
					pairmodule.mixedScores(traitCount, traitScore);
				else
					groupmodule.mixedScores(traitCount, module.getNGroup(), traitScore);
			} else {
				// intra-species: evaluate performance of focal individual for all active
				// strategies
				size = stripVacancies(group, size, groupStrat, groupIdxs);
				countTraits(traitCount, groupStrat, 0, size);
				for (int n = 0; n < nTraits; n++) {
					if (!active[n]) {
						traitScore[n] = -Double.MAX_VALUE;
						continue;
					}
					// add candidate strategy to the mix
					traitCount[n]++;
					if (module.isPairwise())
						pairmodule.mixedScores(traitCount, traitTempScore);
					else
						groupmodule.mixedScores(traitCount, module.getNGroup(), traitTempScore);
					traitScore[n] = traitTempScore[n];
					traitCount[n]--;
				}
			}
		}
		double max = traitScore[mytype]; // assumes mytype is active
		int idx = mytype;
		for (int n = 0; n < nTraits; n++) {
			if (!active[n])
				continue;
			// note: if my payoff and highest payoff are tied, keep type
			if (traitScore[n] > max) {
				max = traitScore[n];
				idx = n;
			}
		}
		if (idx != mytype) {
			updateStrategyAt(me, idx);
			return true;
		}
		return false;
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * Here we introduce the convention of a cyclic preference where strategies with
	 * lower indices are always preferred. For example, with {@code N} strategic
	 * types, strategy {@code 0} preferred over {@code 1} preferred over {@code 2}
	 * ... preferred over {@code N-1} preferred over {@code N} preferred over
	 * {@code 0}, etc. This convention is arbitrary but seems to make sense for
	 * systems with cyclic dominance of strategies and such systems are most likely
	 * to produce evolutionary kaleidoscopes and only for those is this
	 * deterministic updating of crucial importance. For anything else, these are
	 * irrelevant quibbles.
	 */
	@Override
	public boolean preferredPlayerBest(int me, int best, int sample) {
		int mytype = strategies[me] % nTraits;
		int sampletype = strategies[sample] % nTraits;
		if (mytype == sampletype)
			return true;
		int besttype = strategies[best] % nTraits;
		int distsample = (mytype - sampletype + nTraits) % nTraits;
		int distbest = (mytype - besttype + nTraits) % nTraits;
		return (distsample < distbest);
	}

	// REVIEW: applicability of notes below is unclear as of 10-01-20
	// this was an attempt to track down relevant changes in recent version of the
	// virtuallabs in order to reproduce the results obtained in the Complexity
	// article.
	// apparently it is a combination of the following changes which all result in
	// considerable quantitative changes:
	// 1) scores are reset whenever a player imitates the strategy of another player
	// previously the score was reset only when an actual strategy change occurred.
	// 2) to get closer to the imitation/replication approach, individuals no longer
	// always switch if at least one better model is at hand. they rather switch
	// with some probability and keep their strategy otherwise. this behavior can be
	// tuned using the parameter 'switchpref'.
	// 110620: the parameter 'switchpref' is declared obsolete
	// 3) in the Complexity article only the focal player was allowed to reassess
	// its
	// strategy. in the current terminology this represents a mixture of
	// asynchronous
	// updates referring to replication (emphasis on spatial arrangement) and
	// imitation
	// (emphasis on games/interactions).
	//
	// conclusions
	// the above change 1) leads to a significant boost of cooperation in public
	// goods
	// interactions whereas 2) decreases it (but not as dramatically). the last
	// point 3)
	// essentially leads to a change in the time scale.
	//
	// indicate a strategy change only when an actual change occurred and not
	// upon imitation of a neighbor.

	/**
	 * Temporary storage for strategies/traits of individuals in group interactions.
	 */
	private int[] groupStrat;

	/**
	 * Temporary storage for indices of individuals in group interactions.
	 */
	private int[] groupIdxs;

	/**
	 * Temporary storage for strategies/traits of individuals in small sub-group
	 * interactions.
	 */
	private int[] smallStrat;

	/**
	 * Temporary storage for the number of each strategy/type in group interactions.
	 */
	private int[] traitCount;

	/**
	 * Temporary storage for the scores of each strategy/type in group interactions.
	 */
	private double[] traitScore;

	/**
	 * Temporary storage for the scores of each strategy/type prior to the group
	 * interactions.
	 */
	private double[] traitTempScore;

	/**
	 * Eliminate vacant sites from the assembled group.
	 * <p>
	 * <strong>Important:</strong> {@code group.group} is untouchable! It may be a
	 * reference to {@code Geometry.out[group.focal]} and hence any changes would
	 * actually alter the geometry!
	 *
	 * @param group  the group which potentially includes references to vacant sites
	 * @param gStrat the array of strategies in the group
	 * @param gIdxs  the array of indices of the individuals in the group
	 */
	protected void stripGroupVacancies(IBSGroup group, int[] gStrat, int[] gIdxs) {
		group.nSampled = stripVacancies(group.group, group.nSampled, gStrat, gIdxs);
		if (VACANT < 0)
			return;
		group.group = gIdxs;
	}

	/**
	 * Process traits/strategic types while excluding vacant sites.
	 * 
	 * @param groupidx  the array of indices of the individuals in the group
	 * @param groupsize the size of the group
	 * @param gStrat    the array to store/return the traits/strategic types
	 * @param gIdxs     the array to store/return the pruned indexes
	 * @return the size of the interaction group after pruning
	 */
	protected int stripVacancies(int[] groupidx, int groupsize, int[] gStrat, int[] gIdxs) {
		int[] oppstrategies = opponent.strategies;
		int oppntraits = opponent.nTraits;
		// minor efficiency gain without VACANT
		if (VACANT < 0) {
			for (int i = 0; i < groupsize; i++)
				gStrat[i] = oppstrategies[groupidx[i]] % oppntraits;
			return groupsize;
		}
		// remove vacant sites
		int gSize = 0;
		for (int i = 0; i < groupsize; i++) {
			int gi = groupidx[i];
			int type = oppstrategies[gi] % oppntraits;
			if (type == VACANT)
				continue;
			gStrat[gSize] = type;
			gIdxs[gSize++] = gi;
		}
		return gSize;
	}

	@Override
	public void playPairGameAt(IBSGroup group) {
		int me = group.focal;
		int myType = strategies[me] % nTraits;
		if (myType == VACANT)
			return;
		stripGroupVacancies(group, groupStrat, groupIdxs);
		double myScore;
		countTraits(traitCount, groupStrat, 0, group.nSampled);
		if (group.nSampled <= 0) {
			// isolated individual (note the bookkeeping above is overkill and can be
			// optimized)
			myScore = pairmodule.pairScores(myType, traitCount, traitScore);
			updateScoreAt(me, myScore, 0);
			return;
		}

		myScore = pairmodule.pairScores(myType, traitCount, traitScore);
		updateScoreAt(me, myScore, group.nSampled);
		if (playerScoreReset.equals(ScoringType.EPHEMERAL))
			// no need to update scores of everyone else
			return;
		for (int i = 0; i < group.nSampled; i++)
			opponent.updateScoreAt(group.group[i], traitScore[groupStrat[i]]);
	}

	@Override
	public void adjustScoreAt(int index, double before, double after) {
		int type = strategies[index] % nTraits;
		accuTypeScores[type] += after - before;
		scores[index] = after;
		updateEffScoreRange(index, before, after);
		updateFitnessAt(index);
	}

	@Override
	public void adjustScoreAt(int index, double adjust) {
		int type = strategies[index] % nTraits;
		accuTypeScores[type] += adjust;
		double before = scores[index];
		double after = before + adjust;
		scores[index] = after;
		updateEffScoreRange(index, before, after);
		updateFitnessAt(index);
	}

	@Override
	public void adjustPairGameScoresAt(int me) {
		int oldType = strategies[me] % nTraits;
		int newType = strategiesScratch[me] % nTraits;
		commitStrategyAt(me);
		// count out-neighbors
		int nIn = 0, nOut = interaction.kout[me];
		int[] in = null, out = interaction.out[me];
		Arrays.fill(traitCount, 0);
		// count traits of (outgoing) opponents
		for (int n = 0; n < nOut; n++)
			traitCount[opponent.strategies[out[n]] % nTraits]++;
		int u2 = 2;
		if (!interaction.isUndirected) {
			// directed graph, count in-neighbors
			u2 = 1;
			nIn = interaction.kin[me];
			in = interaction.in[me];
			// add traits of incoming opponents
			for (int n = 0; n < nIn; n++)
				traitCount[opponent.strategies[in[n]] % nTraits]++;
		}
		int nInter = (VACANT < 0 ? nIn + nOut : nIn + nOut - traitCount[VACANT]);
		// my type has changed otherwise we wouldn't get here
		// old/newScore are the total accumulated scores
		double oldScore = u2 * pairmodule.pairScores(oldType, traitCount, traitTempScore);
		double newScore = u2 * pairmodule.pairScores(newType, traitCount, traitScore);
		if (newType == VACANT) {
			// oldScore == scores[me] should hold
			// XXX resetScoreAt assumes pre-committed strategy
			// resetScoreAt(me);
			accuTypeScores[oldType] -= scores[me];
			super.resetScoreAt(me);
			// neighbors lost one interaction partner - adjust (outgoing) opponent's score
			for (int n = 0; n < nOut; n++) {
				int you = out[n];
				int type = opponent.strategies[you] % nTraits;
				opponent.removeScoreAt(you, u2 * (traitTempScore[type] - traitScore[type]), u2);
			}
			// same as !interaction.isUndirected because in != null implies directed graph
			// (see above)
			if (in != null) {
				for (int n = 0; n < nIn; n++) {
					int you = in[n];
					int type = opponent.strategies[you] % nTraits;
					// adjust (incoming) opponent's score
					opponent.removeScoreAt(you, traitTempScore[type] - traitScore[type], 1);
				}
			}
		} else {
			if (oldType == VACANT) {
				updateScoreAt(me, newScore, u2 * nInter);
				// neighbors gained one interaction partner - adjust (outgoing) opponent's score
				for (int n = 0; n < nOut; n++) {
					int you = out[n];
					int type = opponent.strategies[you] % nTraits;
					opponent.updateScoreAt(you, u2 * (traitScore[type] - traitTempScore[type]), u2);
				}
				// same as !interaction.isUndirected because in != null implies directed graph
				// (see above)
				if (in != null) {
					for (int n = 0; n < nIn; n++) {
						int you = in[n];
						int type = opponent.strategies[you] % nTraits;
						// adjust (incoming) opponent's score
						opponent.updateScoreAt(you, traitScore[type] - traitTempScore[type], 1);
					}
				}
			} else {
				// interaction count remains the same but old/newScore are accumulated
				if (playerScoreAveraged && nInter > 0) {
					double iInter = 1.0 / (u2 * nInter);
					newScore *= iInter;
					oldScore *= iInter;
				}
				// adjust score of focal player
				adjustScoreAt(me, oldScore, newScore);
				accuTypeScores[oldType] -= oldScore;
				accuTypeScores[newType] += oldScore;
				// adjust (outgoing) opponent's score
				for (int n = 0; n < nOut; n++) {
					int you = out[n];
					int type = opponent.strategies[you] % nTraits;
					if (type == VACANT)
						continue;
					newScore = traitScore[type];
					oldScore = traitTempScore[type];
					if (playerScoreAveraged) {
						double iInter = u2 / ((double) interactions[you]);
						newScore *= iInter;
						oldScore *= iInter;
					}
					opponent.adjustScoreAt(you, newScore - oldScore);
				}
				// same as !interaction.isUndirected because in != null implies directed graph
				// (see above)
				if (in != null) {
					// adjust (incoming) opponent's score
					for (int n = 0; n < nIn; n++) {
						int you = in[n];
						int type = opponent.strategies[you] % nTraits;
						if (type == VACANT)
							continue;
						newScore = traitScore[type];
						oldScore = traitTempScore[type];
						if (playerScoreAveraged) {
							double iInter = u2 / ((double) interactions[you]);
							newScore *= iInter;
							oldScore *= iInter;
						}
						opponent.adjustScoreAt(you, newScore - oldScore);
					}
				}
			}
		}
	}

	@Override
	public void playGroupGameAt(IBSGroup group) {
		int me = group.focal;
		int myType = strategies[me] % nTraits;
		if (myType == VACANT)
			return;
		stripGroupVacancies(group, groupStrat, groupIdxs);
		countTraits(traitCount, groupStrat, 0, group.nSampled);
		if (group.nSampled <= 0) {
			// isolated individual (note the bookkeeping above is overkill and can be
			// optimized)
			traitCount[myType]++;
			groupmodule.groupScores(traitCount, traitScore);
			updateScoreAt(me, traitScore[myType], 0);
			return;
		}

		// for ephemeral scores calculate score of focal only
		boolean ephemeralScores = playerScoreReset.equals(ScoringType.EPHEMERAL);
		switch (group.samplingType) {
			case ALL:
				// interact with all neighbors
				int nGroup = module.getNGroup();
				if (nGroup < group.nSampled + 1) {
					// interact with part of group sequentially
					double myScore = 0.0;
					Arrays.fill(smallScores, 0, group.nSampled, 0.0);
					for (int n = 0; n < group.nSampled; n++) {
						Arrays.fill(traitCount, 0);
						for (int i = 0; i < nGroup - 1; i++)
							traitCount[groupStrat[(n + i) % group.nSampled]]++;
						traitCount[myType]++;
						groupmodule.groupScores(traitCount, traitScore);
						myScore += traitScore[myType];
						if (ephemeralScores)
							continue;
						for (int i = 0; i < nGroup - 1; i++) {
							int idx = (n + i) % group.nSampled;
							smallScores[idx] += traitScore[groupStrat[idx]];
						}
					}
					updateScoreAt(me, myScore, group.nSampled);
					if (ephemeralScores)
						return;
					for (int i = 0; i < group.nSampled; i++)
						opponent.updateScoreAt(group.group[i], smallScores[i], nGroup - 1);
					return;
				}
				// interact with full group (random graphs or all neighbors)

				//$FALL-THROUGH$
			case RANDOM:
				// interact with sampled neighbors
				traitCount[myType]++;
				groupmodule.groupScores(traitCount, traitScore);
				updateScoreAt(me, traitScore[myType]);
				if (ephemeralScores)
					return;
				for (int i = 0; i < group.nSampled; i++)
					opponent.updateScoreAt(group.group[i], traitScore[groupStrat[i]]);
				return;

			default:
				throw new Error("Unknown interaction type (" + interactionGroup.getSampling() + ")");
		}
	}

	@Override
	public void yalpGroupGameAt(IBSGroup group) {
		int me = group.focal;
		int newtype = strategiesScratch[me] % nTraits;
		if (newtype == VACANT || group.nSampled <= 0) {
			// isolated individual (no connections to other sites) - still need to reset its
			// score
			resetScoreAt(group.focal);
			return;
		}
		stripGroupVacancies(group, groupStrat, groupIdxs);
		if (group.nSampled <= 0) {
			// isolated individual (surrounded by vacant sites) - still need to reset its
			// score
			resetScoreAt(group.focal);
			return;
		}

		int oldtype = strategies[me] % nTraits;
		int nGroup = module.getNGroup();
		if (nGroup < group.nSampled + 1) { // interact with part of group sequentially
			double myScore = 0.0;
			Arrays.fill(smallScores, 0, group.nSampled, 0.0);
			for (int n = 0; n < group.nSampled; n++) {
				Arrays.fill(traitCount, 0);
				for (int i = 0; i < nGroup - 1; i++)
					traitCount[groupStrat[(n + i) % group.nSampled]]++;
				traitCount[oldtype]++;
				groupmodule.groupScores(traitCount, traitScore);
				myScore += traitScore[oldtype];
				for (int i = 0; i < nGroup - 1; i++) {
					int idx = (n + i) % group.nSampled;
					smallScores[idx] += traitScore[groupStrat[idx]];
				}
			}
			removeScoreAt(me, myScore, group.nSampled);
			for (int i = 0; i < group.nSampled; i++)
				opponent.removeScoreAt(group.group[i], smallScores[i], nGroup - 1);
			return;
		}
		// interact with full group (random graphs)
		countTraits(traitCount, groupStrat, 0, group.nSampled);
		traitCount[oldtype]++;
		groupmodule.groupScores(traitCount, traitScore);
		removeScoreAt(me, traitScore[oldtype]);
		for (int i = 0; i < group.nSampled; i++)
			opponent.removeScoreAt(group.group[i], traitScore[groupStrat[i]]);
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * Overridden to allow for adjusting scores in well-mixed populations.
	 */
	@Override
	public void adjustGameScoresAt(int me) {
		// check whether an actual strategy change has occurred
		// NOTE: isSameStrategy() only works before committing strategy!
		if (isSameStrategy(me)) {
			commitStrategyAt(me);
			return;
		}
		// check if original procedure works
		if (module.isStatic() || //
				(!interaction.isType(Geometry.Type.MEANFIELD) && //
						!interaction.isType(Geometry.Type.HIERARCHY) && //
						interaction.subgeometry != Geometry.Type.MEANFIELD)) {
			super.adjustGameScoresAt(me);
			return;
		}
		// NOTE: only well-mixed populations or hierarchical populations with well-mixed
		// units end up here
		// adjusting game scores also requires that interactions are with all (other)
		// members of the population
		commitStrategyAt(me);
		if (interaction.isInterspecies()) {
			if (opponent.getInteractionGeometry().isType(Geometry.Type.MEANFIELD)) {
				// competition is well-mixed as well - adjust lookup table
				updateMixedMeanScores();
			} else {
				// XXX combinations of structured and unstructured populations require more
				// attention
				int newstrat = strategies[me] % nTraits;
				if (newstrat == VACANT) {
					setScoreAt(me, 0.0, 0);
				} else {
					// update score of 'me' based on opponent population
					// store scores for each type in traitScores (including 0.0 for VACANT)
					int nGroup = module.getNGroup();
					if (module.isPairwise())
						pairmodule.mixedScores(opponent.strategiesTypeCount, traitScore);
					else
						groupmodule.mixedScores(opponent.strategiesTypeCount, nGroup, traitScore);
					setScoreAt(me, traitScore[newstrat], nGroup * opponent.getPopulationSize());
				}
			}
		}
		// update opponent population in response to change of strategy of 'me'
		opponent.updateMixedMeanScores();
	}

	/**
	 * Calculate scores in well-mixed populations as well as hierarchical structures
	 * with well-mixed units.
	 */
	protected void updateMixedMeanScores() {
		// note that in well-mixed populations the distinction between accumulated and
		// averaged payoffs is impossible (unless interactions are not with all other
		// members)
		// the following is strictly based on averaged payoffs as it is difficult to
		// determine
		// the number of interactions in the case of accumulated payoffs
		switch (interaction.getType()) {
			case HIERARCHY:
				// XXX needs more attention for inter-species interactions
				int unitSize = interaction.hierarchy[interaction.hierarchy.length - 1];
				for (int unitStart = 0; unitStart < nPopulation; unitStart += unitSize) {
					// count traits in unit
					countTraits(traitCount, strategies, unitStart, unitSize);
					// calculate scores in unit (return in traitScores)
					if (module.isPairwise())
						pairmodule.mixedScores(traitCount, traitScore);
					else
						groupmodule.mixedScores(traitCount, module.getNGroup(), traitScore);
					int uInter = nMixedInter;
					if (VACANT >= 0)
						uInter -= traitCount[VACANT];
					for (int n = unitStart; n < unitStart + unitSize; n++) {
						int type = strategies[n] % nTraits;
						setScoreAt(n, traitScore[type], type == VACANT ? 0 : uInter);
					}
				}
				setMaxEffScoreIdx();
				break;

			case MEANFIELD:
				// store scores for each type in typeScores
				if (module.isPairwise())
					pairmodule.mixedScores(opponent.strategiesTypeCount, typeScores);
				else
					groupmodule.mixedScores(opponent.strategiesTypeCount, module.getNGroup(), typeScores);
				double mxScore = -Double.MAX_VALUE;
				int mxTrait = -Integer.MAX_VALUE;
				sumFitness = 0.0;
				for (int n = 0; n < nTraits; n++) {
					if (n == VACANT) {
						typeScores[n] = 0.0;
						accuTypeScores[n] = Double.NaN;
					}
					int count = strategiesTypeCount[n];
					if (count == 0) {
						typeScores[n] = 0.0;
						accuTypeScores[n] = 0.0;
						typeFitness[n] = map2fit.map(0.0);
						continue;
					}
					double score = typeScores[n];
					if (score > mxScore) {
						mxScore = score;
						mxTrait = n;
					}
					accuTypeScores[n] = count * score;
					double fit = map2fit.map(score);
					typeFitness[n] = fit;
					sumFitness += count * fit;
				}
				int idx = -1;
				if (maxEffScoreIdx >= 0) {
					while ((strategies[++idx] % nTraits) != mxTrait)
						;
					maxEffScoreIdx = idx;
				}
				break;

			default:
				throw new Error("updateMixedMeanScores: invalid interaction geometry");
		}
	}

	/**
	 * Count the number of each trait in the array <code>traits</code> starting at
	 * <code>offset</code> for <code>len</code> individuals. The result is stored in
	 * <code>counts</code>.
	 * <p>
	 * <strong>Note:</strong> <code>offset</code> is convenient for hierarchical
	 * structures and prevents copying parts of the <code>strategies</code> array.
	 *
	 * @param counts the array to return the number of individuals with each
	 *               trait/strategy
	 * @param traits the array with the strategies/traits of he individuals
	 * @param offset the offset into the array {@code traits} to start counting
	 * @param len    the number of individuals to count
	 */
	public void countTraits(int[] counts, int[] traits, int offset, int len) {
		Arrays.fill(counts, 0);
		for (int n = offset; n < offset + len; n++)
			counts[traits[n] % nTraits]++;
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * Reset the colors of recently changed strategies.
	 */
	@Override
	public void resetStrategies() {
		for (int n = 0; n < nPopulation; n++)
			strategies[n] %= nTraits;
	}

	@Override
	public void prepareStrategies() {
		System.arraycopy(strategies, 0, strategiesScratch, 0, nPopulation);
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * For discrete modules, update the strategy count of each type and check if
	 * population reached a homogeneous state.
	 */
	@Override
	public void commitStrategies() {
		int[] swap = strategies;
		strategies = strategiesScratch;
		strategiesScratch = swap;
		updateStrategiesTypeCount();
	}

	/**
	 * Update the count of each trait/strategic type.
	 * 
	 * @see #strategiesTypeCount
	 */
	public void updateStrategiesTypeCount() {
		Arrays.fill(strategiesTypeCount, 0);
		for (int n = 0; n < nPopulation; n++)
			strategiesTypeCount[strategies[n] % nTraits]++;
	}

	/**
	 * Gets the count of each trait/strategic type.
	 * 
	 * @return count of each trait/strategic type
	 */
	public int[] getStrategiesTypeCount() {
		return strategiesTypeCount;
	}

	@Override
	public void isConsistent() {
		if (!isConsistent)
			return;
		super.isConsistent();
		double checkScores = 0.0;
		double[] checkAccuTypeScores = new double[nTraits];
		int[] checkStrategiesTypeCount = new int[nTraits];
		// scores array may not exist
		for (int n = 0; n < nPopulation; n++) {
			int stratn = strategies[n] % nTraits;
			checkStrategiesTypeCount[stratn]++;
			double scoren = getScoreAt(n);
			checkScores += scoren;
			checkAccuTypeScores[stratn] += scoren;
		}
		double accScores = 0.0;
		for (int n = 0; n < nTraits; n++) {
			if (checkStrategiesTypeCount[n] != strategiesTypeCount[n]) {
				logger.warning("accounting issue: strategy count of trait " + module.getTraitName(n) + " is "
						+ checkStrategiesTypeCount[n] + " but strategiesTypeCount[" + n + "]=" + strategiesTypeCount[n]
						+ ")");
				isConsistent = false;
			}
			if (Math.abs(checkAccuTypeScores[n] - accuTypeScores[n]) > 1e-8) {
				logger.warning("accounting issue: accumulated scores of trait " + module.getTraitName(n) + " is "
						+ checkAccuTypeScores[n] + " but accuTypeScores[" + n + "]=" + accuTypeScores[n] + ")");
				isConsistent = false;
			}
			if (n == VACANT)
				continue;
			accScores += accuTypeScores[n];
		}
		if (Math.abs(checkScores - accScores) > 1e-8) {
			logger.warning("accounting issue: sum of scores is " + checkScores + " but accuTypeScores add up to "
					+ accScores + " (" + accuTypeScores + ")");
			isConsistent = false;
		}
	}

	@Override
	public boolean isMonomorphic() {
		// extinct
		int popSize = getPopulationSize();
		if (popSize == 0)
			return true;
		// monomorphic
		for (int n = 0; n < nTraits; n++) {
			if (n == VACANT)
				continue;
			if (strategiesTypeCount[n] == popSize)
				return true;
		}
		return false;
	}

	@Override
	public boolean checkConvergence() {
		// with vacant sites the only absorbing state is extinction
		// alternatively, mutation rates do not matter hence super needs not be
		// consulted
		if (getPopulationSize() == 0)
			return true;
		boolean absorbed = super.checkConvergence();
		// has absorbed if monomorphic and no vacant sites or if stop requested on
		// monomorphic states
		return (absorbed && (VACANT < 0 || module.getMonoStop()));
	}

	@Override
	public boolean isVacantAt(int index) {
		if (VACANT < 0)
			return false;
		return strategies[index] % nTraits == VACANT;
	}

	@Override
	public boolean becomesVacantAt(int index) {
		if (VACANT < 0)
			return false;
		return strategiesScratch[index] % nTraits == VACANT;
	}

	@Override
	public void commitStrategyAt(int me) {
		int newstrat = strategiesScratch[me];
		int newtype = newstrat % nTraits;
		int oldtype = strategies[me] % nTraits;
		strategies[me] = newstrat; // the type may be the same but nevertheless it could have changed
		if (oldtype == newtype)
			return;
		strategiesTypeCount[oldtype]--;
		strategiesTypeCount[newtype]++;
	}

	@Override
	public void getFitnessHistogramData(double[][] bins) {
		int nBins = bins[0].length;
		int maxBin = nBins - 1;
		// for neutral selection maxScore==minScore! in that case assume range [score-1,
		// score+1]
		// needs to be synchronized with GUI (e.g. MVFitness, MVFitHistogram, ...)
		double map, min = minFitness;
		if (isNeutral) {
			map = nBins * 0.5;
			min--;
		} else
			map = nBins / (maxFitness - min);
		int idx = 0;

		// clear bins
		for (int n = 0; n < bins.length; n++)
			Arrays.fill(bins[n], 0.0);
		int bin;
		for (int n = 0; n < nPopulation; n++) {
			int pane = strategies[n] % nTraits;
			if (pane == VACANT)
				continue;
			// this should never hold as VACANT should be the last 'trait'
			if (VACANT >= 0 && pane > VACANT)
				pane--;
			bin = (int) ((getScoreAt(n) - min) * map);
			// XXX accumulated payoffs are a problem - should rescale x-axis; at least
			// handle gracefully
			bin = Math.max(0, Math.min(maxBin, bin));
			bins[pane][bin]++;
		}
		double norm = 1.0 / nPopulation;
		for (int n = 0; n < nTraits; n++) {
			if (n == VACANT)
				continue;
			ArrayMath.multiply(bins[idx++], norm);
		}
	}

	@Override
	public boolean getMeanTrait(double[] mean) {
		double iPop = 1.0 / nPopulation;
		for (int n = 0; n < nTraits; n++)
			mean[n] = strategiesTypeCount[n] * iPop;
		return true;
	}

	@Override
	public synchronized <T> void getTraitData(T[] colors, ColorMap<T> colorMap) {
		colorMap.translate(strategies, colors);
	}

	@Override
	public boolean getMeanFitness(double[] mean) {
		double sum = 0.0;
		for (int n = 0; n < nTraits; n++) {
			if (n == VACANT)
				continue;
			sum += accuTypeScores[n];
			// this returns NaN if no strategies of type n present
			// -> unfortunately rounding errors, i.e. non-zero accuTypeScores, may result in
			// infinity
			// i.e. meanscores[n] = accuTypeScores[n]/strategiesTypeCount[n]; fails
			int count = strategiesTypeCount[n];
			mean[n] = count == 0 ? Double.NaN : accuTypeScores[n] / count;
		}
		mean[nTraits] = sum / getPopulationSize();
		return true;
	}

	/**
	 * Gets the traits of all individuals as indices. Those with indices in
	 * {@code [0, nTraits)} denote individuals that have not changed strategy since
	 * the previous report, while those in {@code [nTraits, 2*nTraits)} have.
	 * 
	 * @return the traits
	 */
	public String getTraits() {
		return Formatter.format(strategies);
	}

	@Override
	public String getTraitNameAt(int idx) {
		return module.getTraitName(strategies[idx] % nTraits);
	}

	@Override
	public String getStatus() {
		String status = "";
		boolean[] active = module.getActiveTraits();
		double norm = 1.0 / nPopulation;
		String[] names = module.getTraitNames();
		for (int i = 0; i < nTraits; i++) {
			if (active != null && !active[i])
				continue;
			status += (status.length() > 0 ? ", " : "") + names[i] + ": "
					+ Formatter.formatPercent(strategiesTypeCount[i] * norm, 1);
		}
		return status;
	}

	@Override
	public boolean check() {
		boolean doReset = super.check();
		// pairwise and group interactions may have changed
		if (module.isPairwise()) {
			pairmodule = (Discrete.Pairs) module;
			groupmodule = null;
		} else {
			pairmodule = null;
			// module may be just be Discrete...
			groupmodule = (module instanceof Discrete.Groups ? (Discrete.Groups) module : null);
		}
		// IBSDPopulation opponent shadows IBSPopulation opponent to save casts
		// important: now all modules/populations have been loaded (load() is too early)
		opponent = (IBSDPopulation) super.opponent;

		if (optimizeMoran) {
			// optimized Moran type processes are incompatible with mutations!
			if (!populationUpdateType.isMoran()) {
				optimizeMoran = false;
				logger.warning("optimizations require Moran-type updates - disabled.");
				doReset = true;
			} else if (module.getMutationProb() > 0.0) {
				optimizeMoran = false;
				logger.warning("optimized Moran-type updates are incompatible with mutations - disabled.");
				doReset = true;
			} else // no need to report both warnings
					// optimized Moran type processes are incompatible with well mixed populations!
				if (interaction.isType(Geometry.Type.MEANFIELD) ||
					(interaction.isType(Geometry.Type.HIERARCHY) &&
							interaction.subgeometry == Geometry.Type.MEANFIELD)) {
				optimizeMoran = false;
				logger.warning("optimized Moran-type updates are incompatible with mean-field geometry - disabled.");
				doReset = true;
			}
		}

		int nGroup = module.getNGroup();
		if (interaction.isType(Geometry.Type.MEANFIELD) && !playerScoreAveraged && nGroup > 2) {
			// check if interaction count exceeds integer range
			try {
				int nPop = opponent.getModule().getNPopulation();
				Combinatorics.combinations(nPop - 1, nGroup - 1);
			} catch (ArithmeticException ae) {
				logger.warning(
						"accumulated payoffs in mean-field geometry with group interactions exceed max interaction count.\n"
								+
								"    switching to averaged payoffs.");
				setPlayerScoreAveraged(true);
			}
		}

		// // note: if imitate population update is selected, scores cannot be adjusted
		// for well-mixed populations....
		// if( populationUpdateType==POPULATION_UPDATE_ASYNC_IMITATE &&
		// (interaction.geometry==Geometry.MEANFIELD ||
		// (interaction.geometry==Geometry.HIERARCHY &&
		// interaction.subgeometry==Geometry.MEANFIELD)) &&
		// interactionGroup.samplingType==Group.SAMPLING_ALL ) {
		// setInteractionType(Group.SAMPLING_COUNT);
		// logger.warning("using random sampling of interaction partners for imitation
		// update in well-mixed populations!");
		// // change of sampling may affect whether scores can be adjusted but reset
		// takes care of this
		// doReset = true;
		// }
		// // if imitation becomes obsolete the above check can be removed
		return doReset;
	}

	@Override
	protected boolean doAdjustScores() {
		// relaxed conditions for adjusting scores: for discrete strategies unstructured
		// populations are feasible.
		return !(!interactionGroup.isSampling(IBSGroup.SamplingType.ALL) ||
				!playerScoreReset.equals(ScoringType.RESET_ALWAYS));
	}

	@Override
	public void init() {
		super.init();
		Model model = engine.getModel();
		IBSD.InitType myType = (IBSD.InitType) module.getInitType();
		if (model.isMode(Mode.STATISTICS))
			myType = IBSD.InitType.STATISTICS;

		switch (myType) {
			case STRIPES:
				initStripes();
				break;

			default:
			case UNIFORM:
				initUniform();
				break;

			case FREQUENCY:
				initFrequency();
				break;

			case MONO:
				initMono();
				break;

			case STATISTICS:
				initStatistics();
				break;

			case MUTANT:
				initMutant();
				break;

			case KALEIDOSCOPE:
				initKaleidoscope();
				break;
		}
	}

	/**
	 * Initial configuration with uniform strategy frequencies.
	 * 
	 * @see IBSD.InitType#UNIFORM
	 */
	protected void initUniform() {
		Arrays.fill(strategiesTypeCount, 0);
		for (int n = 0; n < nPopulation; n++) {
			int aStrat = random0n(nTraits);
			strategies[n] = aStrat;
			strategiesTypeCount[aStrat]++;
		}
	}

	/**
	 * Initial configuration with strategy frequencies as specified in options.
	 * 
	 * @see IBSD.InitType#FREQUENCY
	 * @see Discrete#cloInit
	 */
	protected void initFrequency() {
		if (module.isMonoInit() < 0) {
			Arrays.fill(strategiesTypeCount, 0);
			double[] init = module.getInit();
			// different traits active
			double[] cumFreqs = new double[nTraits];
			cumFreqs[0] = init[0];
			for (int i = 1; i < nTraits; i++)
				cumFreqs[i] = cumFreqs[i - 1] + init[i];
			double norm = 1.0 / cumFreqs[nTraits - 1];
			for (int i = 0; i < nTraits; i++)
				cumFreqs[i] *= norm;
			double aRand;
			int aStrat = -1;

			for (int n = 0; n < nPopulation; n++) {
				aRand = random01();
				for (int i = 0; i < nTraits; i++) {
					if (aRand < cumFreqs[i]) {
						aStrat = i;
						break;
					}
				}
				strategies[n] = aStrat;
				strategiesTypeCount[aStrat]++;
			}
			return;
		}
		// initial frequencies represent monomorphic initial state
		initMono();
	}

	/**
	 * Monomorphic initial configuration.
	 * 
	 * @see IBSD.InitType#MONO
	 * @see Discrete#cloInit
	 */
	protected void initMono() {
		double[] init = module.getInit();
		int monoType = ArrayMath.maxIndex(init);
		Arrays.fill(strategiesTypeCount, 0);
		// marginal efficiency gain for all site equal (including VACANT)
		if (init[monoType] > 1.0 - 1e-8) {
			Arrays.fill(strategies, monoType);
			strategiesTypeCount[monoType] = nPopulation;
		} else {
			// monomorphic population with VACANT sites
			double monoFreq = init[monoType];
			int nMono = 0;
			for (int n = 0; n < nPopulation; n++) {
				if (random01() < monoFreq) {
					strategies[n] = monoType;
					nMono++;
					continue;
				}
				strategies[n] = VACANT;
			}
			strategiesTypeCount[monoType] = nMono;
			strategiesTypeCount[VACANT] = nPopulation - nMono;
		}
	}

	/**
	 * Initial configuration for generating a statistics sample.
	 * 
	 * @see IBSD.InitType#STATISTICS
	 */
	protected void initStatistics() {
		FixationData fix = ((IBSD) engine.getModel()).getFixationData();
		fix.mutantNode = initMutant();
		fix.mutantTrait = strategies[fix.mutantNode];
	}

	/**
	 * Monomorphic initial configuration with a single randomly placed mutant.
	 * 
	 * @return the location of the mutant
	 * 
	 * @see IBSD.InitType#MUTANT
	 * @see Discrete#cloInit
	 */
	protected int initMutant() {
		double[] init = module.getInit();
		int restrait = ArrayMath.maxIndex(init);
		int muttrait = -1;
		Arrays.fill(strategiesTypeCount, 0);
		if (VACANT < 0) {
			// no vacant sites
			Arrays.fill(strategies, restrait);
			strategiesTypeCount[restrait] = nPopulation;
			// place a single individual with the opposite strategy
			muttrait = random0n(nTraits - 1);
			if (muttrait >= restrait)
				muttrait++;
		} else {
			// with vacant sites
			if (restrait == VACANT) {
				double max = -Double.MAX_VALUE;
				for (int n = 0; n < nTraits; n++) {
					if (n == VACANT || init[n] <= max)
						continue;
					restrait = n;
					max = init[n];
				}
			}
			double vfreq = init[VACANT];
			for (int n = 0; n < nPopulation; n++) {
				int type = random01() > vfreq ? restrait : VACANT;
				strategies[n] = type;
				strategiesTypeCount[type]++;
			}
			muttrait = VACANT;
			while (muttrait == VACANT) {
				// place a single individual with the opposite strategy
				muttrait = random0n(nTraits - 1);
				if (muttrait >= restrait)
					muttrait++;
			}
		}
		// place a single individual with a random but different strategy
		int loc = random0n(nPopulation);
		strategiesTypeCount[strategies[loc]]--;
		strategies[loc] = muttrait;
		strategiesTypeCount[muttrait]++;
		return loc;
	}

	/**
	 * Initial configuration that generates evolutionary kaleidoscopes for
	 * deterministic update rules. Whether this is possible and and what kind of
	 * initial configurations are required depends on the module. Hence this method
	 * must be overriden in subclasses that admit kaleidoscopes.
	 * <p>
	 * <strong>Note:</strong> requires the explicit adding of the key
	 * {@link InitType#KALEIDOSCOPE} for IBS models. For example, add
	 * 
	 * <pre>
	 * if (model.isModelType(Type.IBS))
	 * 	engine.cloInitType.addKey(InitType.KALEIDOSCOPE);
	 * </pre>
	 * 
	 * to
	 * {@code org.evoludo.simulator.modules.Module#modelLoaded() Module#modelLoaded()}.
	 * 
	 * @see IBSD.InitType#KALEIDOSCOPE
	 */
	protected void initKaleidoscope() {
	}

	/**
	 * Initial configuration with monomorphic stripes of each type to investigate
	 * invasion properties of one strategy into another with at least one instance
	 * of all possible pairings.
	 */
	protected void initStripes() {
		// only makes sense for 2D lattices at this point. if not, defaults to uniform
		// random initialization (the only other inittype that doesn't require --init).
		if (interaction.interReproSame && (interaction.isType(Geometry.Type.SQUARE) ||
				interaction.isType(Geometry.Type.SQUARE_NEUMANN) ||
				interaction.isType(Geometry.Type.SQUARE_MOORE) ||
				interaction.isType(Geometry.Type.LINEAR))) {
			Arrays.fill(strategiesTypeCount, 0);
			// note: shift first strip by half a width to avoid having a boundary at the
			// edge. also prevents losing one trait interface with fixed boundary
			// conditions. procedure tested for 2, 3, 4, 5 traits
			int nStripes = nTraits + 2 * sum(2, nTraits - 2);
			int size = (interaction.isType(Geometry.Type.LINEAR) ? nPopulation
					: (int) Math.sqrt(nPopulation));
			int width = size / nStripes;
			// make first strip wider
			int width2 = (size - (nStripes - 1) * width) / 2;
			fillStripe(0, width2, 0);
			int offset = (nStripes - 1) * width + width2;
			fillStripe(offset, size - offset, 0);
			strategiesTypeCount[0] += width * size;
			offset = width2;
			// first all individual traits
			for (int n = 1; n < nTraits; n++) {
				fillStripe(offset, width, n);
				offset += width;
			}
			// second all trait pairs
			int nPasses = Math.max(nTraits - 2, 1);
			int incr = 2;
			while (incr <= nPasses) {
				int trait1 = 0;
				int trait2 = incr;
				while (trait2 < nTraits) {
					fillStripe(offset, width, trait1++);
					offset += width;
					fillStripe(offset, width, trait2++);
					offset += width;
				}
				incr++;
			}
			return;
		}
		logger.warning("inittype 'stripes': 2D lattice structures required - using 'uniform'.");
		initUniform();
	}

	/**
	 * Helper method to initialize lattice structures with homogeneous stripes of
	 * each trait/strategic type.
	 * 
	 * @param offset the offset to the start of the stripe
	 * @param width  the width of the stripe
	 * @param trait  the trait/stratgy of the stripe
	 * 
	 * @see InitType#STRIPES
	 */
	private void fillStripe(int offset, int width, int trait) {
		int size = (int) Math.sqrt(nPopulation);
		for (int i = 0; i < size; i++) {
			Arrays.fill(strategies, offset, offset + width, trait);
			offset += size;
		}
	}

	/**
	 * Helper method to determine the number of stripes required so that each
	 * trait/strategic type shares at least one interface with every other trait:
	 * {@code nStripes = nTraits + 2 * sum(2, nTraits - 2)}. Procedure tested for
	 * {@code 2, 3, 4, 5} traits.
	 * 
	 * @param start the starting trait
	 * @param end   the end trait
	 * @return the number of traits
	 */
	private int sum(int start, int end) {
		if (end < start)
			return 0;
		int sum = 0;
		for (int n = start; n <= end; n++)
			sum += n;
		return sum;
	}

	@Override
	public boolean mouseHitNode(int hit, boolean alt) {
		if (hit < 0 || hit >= nPopulation)
			return false; // invalid argument
		int newtype = strategies[hit] % nTraits + nTraits + (alt ? -1 : 1);
		return mouseSetHit(hit, newtype % nTraits);
	}

	// allows drawing of initial configurations - feature got retired; revive?
	// /**
	// * set type of hit node to that of reference node
	// *
	// * @param hit
	// * @param ref
	// * @return
	// */
	// @Override
	// public boolean mouseHitNode(int hit, int ref) {
	// if( hit<0 || hit>=nPopulation )
	// return false; // invalid argument
	// if( ref<0 || ref>=nPopulation )
	// return false; // invalid argument
	// return mouseSetHit(hit, strategies[ref]);
	// }

	/**
	 * Process event from GUI: individual with index {@code hit} was hit by mouse
	 * (or tap) in order to set its strategy to {@code strategy}.
	 * 
	 * @param hit      the index of the individual that was hit by mouse or tap
	 * @param strategy the new strategy of the individual
	 * @return {@code false} if no actions taken (should not happen)
	 */
	private boolean mouseSetHit(int hit, int strategy) {
		strategiesScratch[hit] = strategy;

		/* this is a strategy change - need to adjust scores */
		if (adjustScores) {
			adjustGameScoresAt(hit); // this also commits strategy
		} else {
			commitStrategyAt(hit);
			// when in doubt, recalculate everything for everyone
			engine.modelUpdate();
		}
		engine.fireModelChanged();
		return true;
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * <strong>Important:</strong> Strategies must already be restored!
	 */
	@Override
	public boolean restoreFitness(Plist plist) {
		if (!super.restoreFitness(plist))
			return false;
		if (hasLookupTable) {
			// super could not determine sumFitness
			sumFitness = 0.0;
			for (int n = 0; n < nTraits; n++) {
				sumFitness += strategiesTypeCount[n] * typeFitness[n];
				accuTypeScores[n] = strategiesTypeCount[n] * typeScores[n];
			}
		} else {
			Arrays.fill(accuTypeScores, 0.0);
			for (int n = 0; n < nPopulation; n++) {
				int type = strategies[n] % nTraits;
				if (type == VACANT)
					continue;
				accuTypeScores[type] += getScoreAt(n);
			}
		}
		if (VACANT >= 0)
			accuTypeScores[VACANT] = Double.NaN;
		return true;
	}

	@Override
	public void encodeStrategies(StringBuilder plist) {
		plist.append("<key>Strategies</key>\n<dict>\n");
		String[] names = module.getTraitNames();
		for (int n = 0; n < nTraits; n++)
			plist.append(Plist.encodeKey(Integer.toString(n), names[n]));
		plist.append("</dict>\n");
		plist.append(Plist.encodeKey("Configuration", strategies));
	}

	@Override
	public boolean restoreStrategies(Plist plist) {
		@SuppressWarnings("unchecked")
		List<Integer> strat = (List<Integer>) plist.get("Configuration");
		if (strat == null || strat.size() != nPopulation)
			return false;
		Arrays.fill(strategiesTypeCount, 0);
		for (int n = 0; n < nPopulation; n++) {
			int stratn = strat.get(n);
			strategies[n] = stratn;
			strategiesTypeCount[stratn % nTraits]++;
		}
		return true;
	}
}
