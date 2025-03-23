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
import org.evoludo.math.Combinatorics;
import org.evoludo.math.RNGDistribution;
import org.evoludo.simulator.ColorMap;
import org.evoludo.simulator.EvoLudo;
import org.evoludo.simulator.Geometry;
import org.evoludo.simulator.models.IBS.HasIBS;
import org.evoludo.simulator.models.IBS.ScoringType;
import org.evoludo.simulator.models.IBSD.Init;
import org.evoludo.simulator.modules.Discrete;
import org.evoludo.simulator.modules.Mutation;
import org.evoludo.util.Formatter;
import org.evoludo.util.Plist;

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
	 * @see HasIBS.DPairs
	 */
	protected HasIBS.DPairs pairmodule;

	/**
	 * For group interaction modules {@code module==groupmodule} holds and
	 * {@code null} otherwise. Convenience field to reduce the number of
	 * (unnecessary) casts.
	 * 
	 * @see HasIBS.DGroups
	 */
	protected HasIBS.DGroups groupmodule;

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
	 * {@link IBSDPopulation#maybeMutateMoran(int, int)}).
	 * </ol>
	 */
	protected boolean optimizeMoran = false;

	/**
	 * The mutation parameters.
	 */
	protected Mutation.Discrete mutation;

	/**
	 * Creates a population of individuals with discrete traits for IBS simulations.
	 * 
	 * @param engine the pacemaker for running the model
	 * @param module the module that defines the game
	 */
	public IBSDPopulation(EvoLudo engine, Discrete module) {
		super(engine, module);
		// important: cannot deal with casting shadowed opponent here because for
		// multi-species modules all species need to be loaded first.
		opponent = this;
		this.module = module;
		mutation = module.getMutation();
		if (module instanceof HasIBS.DPairs)
			pairmodule = (HasIBS.DPairs) module;
		if (module instanceof HasIBS.DGroups)
			groupmodule = (HasIBS.DGroups) module;
		init = new Init((IBS) engine.getModel());
	}

	@Override
	public void setOpponentPop(IBSPopulation opponent) {
		super.setOpponentPop(opponent);
		this.opponent = (IBSDPopulation) super.opponent;
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

	/**
	 * The array with the initial number of individuals of each trait/strategic
	 * type.
	 */
	public int[] initTypeCount;

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
		if (optimizeMoran && populationUpdate.isMoran()) {
			int nLinks = (int) (competition.avgOut * nPopulation + 0.5);
			if (activeLinks == null || activeLinks.length != nLinks) {
				activeLinks = new link[nLinks];
				for (int n = 0; n < nLinks; n++)
					activeLinks[n] = new link();
			}
		} else {
			// conserve memory
			activeLinks = null;
		}
		// groupScores have the same maximum length
		int maxGroup = groupScores.length;
		if (groupStrat == null || groupStrat.length != maxGroup)
			groupStrat = new int[maxGroup];
		if (groupIdxs == null || groupIdxs.length != maxGroup)
			groupIdxs = new int[maxGroup];
		if (smallStrat == null || smallStrat.length != maxGroup)
			smallStrat = new int[maxGroup];
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
				int[] neighs = competition.out[n];
				int no = competition.kout[n];
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
				neighs = competition.in[n];
				int ni = competition.kin[n];
				for (int i = 0; i < ni; i++) {
					int aneigh = neighs[i];
					if (strategies[aneigh] % nTraits == type)
						continue;
					activeLinks[nact].source = aneigh;
					activeLinks[nact].destination = n;
					double ascore = getFitnessAt(aneigh) / (competition.kout[aneigh]);
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
					debugModel = activeLinks[i].destination;
					debugFocal = activeLinks[i].source;
					maybeMutateMoran(debugFocal, debugModel);
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
			int[] neighs = competition.out[n];
			int nn = competition.kout[n];
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
				debugModel = activeLinks[i].destination;
				debugFocal = activeLinks[i].source;
				maybeMutateMoran(debugFocal, debugModel);
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
			int[] neighs = competition.in[n];
			int nn = competition.kin[n];
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
				debugModel = activeLinks[i].source;
				debugFocal = activeLinks[i].destination;
				maybeMutateMoran(debugModel, debugFocal);
				break;
			}
		}
	}

	/**
	 * A minimalstic helper class (or data structure) to represent a single
	 * <em>directed</em> link in the network structure. <em>Undirected</em> links
	 * are represented by two directed ones. This is used for Moran optimizations.
	 * 
	 * @see IBSDPopulation#optimizeMoran
	 */
	static class link {

		/**
		 * Construct a new link.
		 */
		public link() {
		}

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
	 * Perform a single ecological update of the individual with index {@code me}:
	 * <ol>
	 * <li>Focal individual dies with probability proportional to the death rate.
	 * <li>Otherwise, draw a random neighbour and, if unoccupied, place clonal
	 * offspring on neighboring site with probability proportional to fitness.
	 * </ol>
	 */
	@Override
	protected double updatePlayerEcologyAt(int me) {
		debugFocal = me;
		debugModel = -1;
		double randomTestVal = random01() * (module.getDeathRate() + maxFitness); // time rescaling
		if (randomTestVal < module.getDeathRate()) {
			// vacate focal site
			strategiesScratch[me] = VACANT + nTraits;
			updateScoreAt(me, true);
		} else if (randomTestVal < (module.getDeathRate() + getFitnessAt(me))) {
			// fill neighbor site if vacant
			debugModel = pickNeighborSiteAt(me);
			if (isVacantAt(debugModel)) {
				maybeMutateMoran(me, debugModel);
			}
		}
		int nPop = getPopulationSize();
		if (nPop == 0)
			// population went extinct, no more events possible
			return Double.POSITIVE_INFINITY;
		double rate = nPop * (module.getDeathRate() + maxFitness);
		return RNGDistribution.Exponential.next(rng.getRNG(), rate);
	}

	@Override
	public void updateFromModelAt(int index, int modelPlayer) {
		super.updateFromModelAt(index, modelPlayer); // deal with tags
		int newstrat = strategies[modelPlayer] % nTraits;
		int oldstrat = strategies[index] % nTraits;
		if (oldstrat != newstrat)
			newstrat += nTraits;
		strategiesScratch[index] = newstrat;
	}

	@Override
	public double mutateAt(int focal) {
		strategiesScratch[focal] = mutation.mutate(strategies[focal] % nTraits) + nTraits;
		updateScoreAt(focal, true);
		return 1.0 / (nPopulation * module.getSpeciesUpdateRate());
	}

	@Override
	protected boolean maybeMutateAt(int focal, boolean switched) {
		int strat = (switched ? strategiesScratch[focal] : strategies[focal]) % nTraits;
		boolean mutate = mutation.doMutate();
		if (mutate)
			strategiesScratch[focal] = mutation.mutate(strat) + nTraits;
		return switched || mutate;
	}

	@Override
	protected void maybeMutateMoran(int source, int dest) {
		updateFromModelAt(dest, source);
		if (mutation.doMutate())
			strategiesScratch[dest] = mutation.mutate(strategiesScratch[dest] % nTraits) + nTraits;
		updateScoreAt(dest, true);
	}

	@Override
	protected void debugMarkChange() {
		super.debugMarkChange(); // for logging of update
		if (debugFocal >= 0)
			strategies[debugFocal] = (strategies[debugFocal] % nTraits) + nTraits;
		if (debugModel >= 0)
			strategies[debugModel] = (strategies[debugModel] % nTraits) + nTraits;
		if (debugNModels > 0) {
			for (int n = 0; n < debugNModels; n++) {
				int idx = debugModels[n];
				strategies[idx] = (strategies[idx] % nTraits) + nTraits;
			}
		}
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
			double score = typeScores[strategies[idx] % nTraits];
			if (playerScoreAveraged)
				return score;
			return score * interactions[idx];
		}
		return scores[idx];
	}

	@Override
	public double getFitnessAt(int idx) {
		if (hasLookupTable) {
			double fit = typeFitness[strategies[idx] % nTraits];
			if (playerScoreAveraged)
				return fit;
			return fit * interactions[idx];
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
		if (hasLookupTable || //
				(adjustScores && interaction.getType() == Geometry.Type.HIERARCHY //
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
			int newtype = mytype;
			for (int n = 0; n < nTraits; n++) {
				if (!active[n])
					continue;
				// note: if my payoff and highest payoff are tied, keep type
				if (typeScores[n] > max) {
					max = typeScores[n];
					newtype = n;
				}
			}
			if (newtype != mytype) {
				strategiesScratch[me] = newtype + nTraits;
				return true;
			}
			return false;
		}

		// frequency dependent selection: determine active strategy with highest payoff
		if (competition.getType() == Geometry.Type.MEANFIELD) {
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
		int newtype = mytype;
		for (int n = 0; n < nTraits; n++) {
			if (!active[n])
				continue;
			// note: if my payoff and highest payoff are tied, keep type
			if (traitScore[n] > max) {
				max = traitScore[n];
				newtype = n;
			}
		}
		if (newtype != mytype) {
			strategiesScratch[me] = newtype + nTraits;
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
		// for ephemeral scores calculate score of focal only
		boolean ephemeralScores = playerScoring.equals(ScoringType.EPHEMERAL);
		if (group.nSampled <= 0) {
			// isolated individual (note the bookkeeping can be optimized)
			myScore = pairmodule.pairScores(myType, traitCount, traitScore);
			if (ephemeralScores) {
				// no need to update scores of everyone else
				resetScoreAt(me);
				updateScoreAt(me, myScore, 0);
				return;
			}
			updateScoreAt(me, myScore, 0);
			return;
		}

		myScore = pairmodule.pairScores(myType, traitCount, traitScore);
		if (ephemeralScores) {
			// no need to update scores of everyone else
			resetScoreAt(me);
			setScoreAt(me, myScore / group.nSampled, group.nSampled);
			return;
		}
		updateScoreAt(me, myScore, group.nSampled);
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
		int nInter = nIn + nOut - (VACANT < 0 ? 0 : traitCount[VACANT]);
		// my type has changed otherwise we wouldn't get here
		// old/newScore are the total accumulated scores
		double oldScore = u2 * pairmodule.pairScores(oldType, traitCount, traitTempScore);
		double newScore = u2 * pairmodule.pairScores(newType, traitCount, traitScore);
		if (newType == VACANT) {
			double myScore = scores[me];
			accuTypeScores[oldType] -= myScore;
			scores[me] = 0.0;
			interactions[me] = 0;
			updateEffScoreRange(me, myScore, 0.0);
			sumFitness -= fitness[me];
			fitness[me] = 0.0;
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
						double iInter = 1.0 / interactions[you];
						newScore *= iInter;
						oldScore *= iInter;
					}
					opponent.adjustScoreAt(you, u2 * (newScore - oldScore));
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
							double iInter = 1.0 / interactions[you];
							newScore *= iInter;
							oldScore *= iInter;
						}
						// u2 = 1 for directed structures
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
		// for ephemeral scores calculate score of focal only
		boolean ephemeralScores = playerScoring.equals(ScoringType.EPHEMERAL);
		if (group.nSampled <= 0) {
			// isolated individual (note the bookkeeping above is overkill and can be
			// optimized)
			traitCount[myType]++;
			groupmodule.groupScores(traitCount, traitScore);
			if (ephemeralScores) {
				resetScoreAt(me);
				setScoreAt(me, traitScore[myType], 0);
				return;
			}
			updateScoreAt(me, traitScore[myType], 0);
			return;
		}

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
				// interact with full group (random graphs or all neighbors)

				//$FALL-THROUGH$
			case RANDOM:
				// interact with sampled neighbors
				traitCount[myType]++;
				groupmodule.groupScores(traitCount, traitScore);
				if (ephemeralScores) {
					resetScoreAt(me);
					setScoreAt(me, traitScore[myType], 1);
					return;
				}
				updateScoreAt(me, traitScore[myType]);
				for (int i = 0; i < group.nSampled; i++)
					opponent.updateScoreAt(group.group[i], traitScore[groupStrat[i]]);
				return;

			default:
				throw new Error("Unknown interaction type (" + interGroup.getSampling() + ")");
		}
	}

	@Override
	public void yalpGroupGameAt(IBSGroup group) {
		int me = group.focal;
		int newtype = strategiesScratch[me] % nTraits;
		if (newtype == VACANT || group.nSampled <= 0) {
			// isolated individual or vacant site - reset score
			resetScoreAt(me);
			return;
		}
		stripGroupVacancies(group, groupStrat, groupIdxs);
		if (group.nSampled <= 0) {
			// isolated individual (surrounded by vacant sites) - reset score
			resetScoreAt(me);
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
				(interaction.getType() != Geometry.Type.MEANFIELD && //
						interaction.getType() != Geometry.Type.HIERARCHY && //
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
			if (opponent.getInteractionGeometry().getType() == Geometry.Type.MEANFIELD) {
				// competition is well-mixed as well - adjust lookup table
				updateMixedMeanScores();
			} else {
				// XXX combinations of structured and unstructured populations require more
				// attention
				int newstrat = strategies[me] % nTraits;
				if (newstrat == VACANT) {
					resetScoreAt(me);
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
		// members) the following is strictly based on averaged payoffs as it is
		// difficult to determine the number of interactions in the case of accumulated
		// payoffs
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
		// assume innocence until found guilty
		boolean passed = true;
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
		int accStrat = 0;
		double accScores = 0.0;
		for (int n = 0; n < nTraits; n++) {
			if (checkStrategiesTypeCount[n] != strategiesTypeCount[n]) {
				logger.warning("accounting issue: strategy count of trait " + module.getTraitName(n) + " is "
						+ checkStrategiesTypeCount[n] + " but strategiesTypeCount[" + n + "]="
						+ strategiesTypeCount[n]);
				passed = false;
			}
			if (Math.abs(checkAccuTypeScores[n] - accuTypeScores[n]) > 1e-8) {
				logger.warning("accounting issue: accumulated scores of trait " + module.getTraitName(n) + " is "
						+ checkAccuTypeScores[n] + " but accuTypeScores[" + n + "]=" + accuTypeScores[n]);
				passed = false;
			}
			accStrat += strategiesTypeCount[n];
			if (n == VACANT)
				continue;
			accScores += accuTypeScores[n];
		}
		if (nPopulation != accStrat) {
			logger.warning("accounting issue: sum of trait types is " + accStrat + " but nPopulation=" + nPopulation);
			passed = false;
		}
		if (Math.abs(checkScores - accScores) > 1e-8) {
			logger.warning("accounting issue: sum of scores is " + checkScores + " but accuTypeScores add up to "
					+ accScores + " (" + Formatter.format(accuTypeScores, 8) + ")");
			passed = false;
		}
		// do not yet set isConsistent to false because this prevents the test in super
		// to run
		super.isConsistent();
		// super may have already set isConsistent to false; add our assesement
		isConsistent &= passed;
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
		boolean absorbed = isMonomorphic() && (mutation.probability <= 0.0);
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
		debugSame = (oldtype == newtype);
		if (debugSame)
			return;
		strategiesTypeCount[oldtype]--;
		strategiesTypeCount[newtype]++;
	}

	/**
	 * Gets the score of individuals in a population that is monomorphic in trait
	 * {@code type}.
	 * 
	 * @param type the trait type
	 * @return the monomorphic score
	 */
	public double getMonoScore(int type) {
		// for accumulated payoffs this makes only sense with adjustScores, without
		// VACANT and for regular interaction geometries otherwise individuals may
		// have different scores even in homogeneous populations
		if (!playerScoreAveraged && (VACANT >= 0 || !interaction.isRegular))
			return Double.NaN;
		// averaged scores or regular interaction geometries without vacant sites
		// max/min doesn't matter; graph must be regular for accumulated scores
		return processScore(module.getMonoGameScore(type % nTraits), true);
	}

	@Override
	public void getFitnessHistogramData(double[][] bins) {
		int nBins = bins[0].length;
		int maxBin = nBins - 1;
		double map, min = minScore;
		if (isNeutral) {
			map = nBins * 0.5;
			min--;
		} else
			map = nBins / (maxScore - min);
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

	/**
	 * Set the initial trait frequencies in the population. Switches the
	 * initialization type to {@link Init.Type#FREQUENCY}.
	 * 
	 * @param init the initial traits
	 * @return {@code true} if the initialization was successful
	 */
	public boolean setInitialTraits(double[] init) {
		if (init == null || init.length != nTraits)
			return false;
		// switch initialization type to frequencies
		// IBSD calls routine only if frequency is valid key
		this.init.type = Init.Type.FREQUENCY;
		this.init.args = init;
		return true;
	}

	@Override
	public void getInitialTraits(double[] traits) {
		double iPop = 1.0 / nPopulation;
		for (int n = 0; n < nTraits; n++)
			traits[n] = initTypeCount[n] * iPop;
	}

	@Override
	public void getMeanTraits(double[] mean) {
		double iPop = 1.0 / nPopulation;
		for (int n = 0; n < nTraits; n++)
			mean[n] = strategiesTypeCount[n] * iPop;
	}

	@Override
	public synchronized <T> void getTraitData(T[] colors, ColorMap<T> colorMap) {
		colorMap.translate(strategies, colors);
	}

	@Override
	public void getMeanFitness(double[] mean) {
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

		if (optimizeMoran) {
			// optimized Moran type processes are incompatible with mutations!
			if (!populationUpdate.isMoran()) {
				optimizeMoran = false;
				logger.warning("optimizations require Moran-type updates - disabled.");
				doReset = true;
			} else if (mutation.probability > 0.0) {
				optimizeMoran = false;
				logger.warning("optimized Moran-type updates are incompatible with mutations - disabled.");
				doReset = true;
			} else // no need to report both warnings
					// optimized Moran type processes are incompatible with well mixed populations!
			if (interaction.getType() == Geometry.Type.MEANFIELD ||
					(interaction.getType() == Geometry.Type.HIERARCHY &&
							interaction.subgeometry == Geometry.Type.MEANFIELD)) {
				optimizeMoran = false;
				logger.warning("optimized Moran-type updates are incompatible with mean-field geometry - disabled.");
				doReset = true;
			}
		}

		int nGroup = module.getNGroup();
		if (interaction.getType() == Geometry.Type.MEANFIELD && !playerScoreAveraged && nGroup > 2) {
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

		// start allocating memory
		if (strategies == null || strategies.length != nPopulation)
			strategies = new int[nPopulation];
		if (strategiesScratch == null || strategiesScratch.length != nPopulation)
			strategiesScratch = new int[nPopulation];
		if (traitCount == null || traitCount.length != nTraits)
			traitCount = new int[nTraits];
		if (traitScore == null || traitScore.length != nTraits)
			traitScore = new double[nTraits];
		if (accuTypeScores == null || accuTypeScores.length != nTraits)
			accuTypeScores = new double[nTraits];
		if (initTypeCount == null || initTypeCount.length != nTraits)
			initTypeCount = new int[nTraits];
		if (strategiesTypeCount == null || strategiesTypeCount.length != nTraits)
			strategiesTypeCount = new int[nTraits];
		// best-response may require temporary memory - this is peanuts, just reserve it
		if (traitTempScore == null || traitTempScore.length != nTraits)
			traitTempScore = new double[nTraits];
		return doReset;
	}

	@Override
	protected boolean doAdjustScores() {
		switch (playerScoring) {
			case RESET_ALWAYS:
			default:
				// if resetting scores after every update, scores can be adjusted
				// when interacting all neighbours
				return interGroup.isSampling(IBSGroup.SamplingType.ALL);
			case EPHEMERAL:
				// for ephemeral scoring, scores are never adjusted
			case RESET_ON_CHANGE:
				// if scores are reset only on an actual strategy change, scores
				// can never be adjusted
				return false;
		}
	}

	@Override
	public void init() {
		super.init();
		Arrays.fill(strategiesTypeCount, 0);
		switch (init.type) {
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

			case MUTANT:
				int mutant = initMutant();
				FixationData fix = engine.getModel().getFixationData();
				if (fix != null) {
					fix.mutantNode = mutant;
					fix.mutantTrait = (mutant < 0 ? mutant : strategies[mutant]);
					fix.residentTrait = (int) init.args[1];
				}
				break;

			case TEMPERATURE:
				mutant = initTemperature();
				fix = engine.getModel().getFixationData();
				if (fix != null) {
					fix.mutantNode = mutant;
					fix.mutantTrait = strategies[fix.mutantNode];
					fix.residentTrait = (int) init.args[1];
					fix.mutantTrait = (mutant < 0 ? mutant : strategies[mutant]);
				}
				break;

			case KALEIDOSCOPE:
				initKaleidoscope();
				break;
		}
		if (ArrayMath.norm(strategiesTypeCount) != nPopulation)
			// fatal does not return control
			engine.fatal("accounting problem (sum of traits " + ArrayMath.norm(strategiesTypeCount) + "!=" + nPopulation
					+ ").");
		System.arraycopy(strategiesTypeCount, 0, initTypeCount, 0, nTraits);
	}

	/**
	 * Initial configuration with uniform strategy frequencies of all
	 * <em>active</em> strategies.
	 * 
	 * @see IBSD.Init.Type#UNIFORM
	 */
	protected void initUniform() {
		Arrays.fill(strategiesTypeCount, 0);
		int nActive = module.getNActive();
		int[] active = new int[nActive];
		boolean[] act = module.getActiveTraits();
		int idx = 0;
		for (int n = 0; n < nTraits; n++) {
			if (!act[n])
				continue;
			active[idx++] = n;
		}
		for (int n = 0; n < nPopulation; n++) {
			int aStrat = active[random0n(nActive)];
			strategies[n] = aStrat;
			strategiesTypeCount[aStrat]++;
		}
	}

	/**
	 * Initial configuration with strategy frequencies as specified in arguments.
	 * 
	 * @see IBSD.Init.Type#FREQUENCY
	 */
	protected void initFrequency() {
		Arrays.fill(strategiesTypeCount, 0);
		// different traits active
		double[] cumFreqs = new double[nTraits];
		cumFreqs[0] = init.args[0];
		int nInit = init.args.length;
		for (int i = 1; i < nTraits; i++)
			cumFreqs[i] = cumFreqs[i - 1] + (i < nInit ? init.args[i] : 0.0);
		double inorm = 1.0 / cumFreqs[nTraits - 1];
		ArrayMath.multiply(cumFreqs, inorm);

		for (int n = 0; n < nPopulation; n++) {
			int aStrat = -1;
			double aRand = random01();
			for (int i = 0; i < nTraits; i++) {
				if (aRand < cumFreqs[i]) {
					aStrat = i;
					break;
				}
			}
			strategies[n] = aStrat;
			strategiesTypeCount[aStrat]++;
		}
	}

	/**
	 * Monomorphic initial configuration with specified trait (and frequency in
	 * modules that allow empty sites).
	 * 
	 * @see IBSD.Init.Type#MONO
	 */
	protected void initMono() {
		// initArgs contains the index of the monomorphic trait
		int monoType = (int) init.args[0];
		double monoFreq = 1.0;
		if (VACANT >= 0) {
			// if present the second argument indicates the frequency of vacant sites
			// if not use estimate for carrying capacity
			monoFreq = (init.args.length > 1 ? Math.max(0.0, 1.0 - init.args[1])
					: 1.0 - estimateVacantFrequency(monoType));
		}
		initMono(monoType, monoFreq);
	}

	/**
	 * Helper method to determine the frequency of vacant sites based on an estimate
	 * of the carrying capacity. In well-mixed populations this is \(1-d/r\), where
	 * \(d\) is the death rate and \(r\) the fitness of the resident type.
	 * Similarly, on regular graphs the carrying capacity is
	 * \(1-(k-1)d/(r(k-1)-d)\), where \(k\) is the degree of the graph. Finally, on
	 * generic structures, the estimate of the carrying capacity is based on the
	 * average out-degree of all nodes.
	 * <p>
	 * <strong>Note:</strong> residents in structured populations additionally have
	 * a characteristic distribution, which is not accounted for in this estimate.
	 * 
	 * @param type the resident type
	 * @return the estimated frequency of vacant sites
	 */
	private double estimateVacantFrequency(int type) {
		double d = module.getDeathRate();
		double fit = map2fit.map(module.getMonoGameScore(type % nTraits));
		Geometry geometry = module.getGeometry();
		if (geometry.getType() == Geometry.Type.MEANFIELD)
			// carrying capacity is 1.0 - d / fit
			return d / fit;
		double k1 = geometry.avgOut - 1.0;
		// carrying capacity on a k-regular graph is 1.0 - (k - 1) * d / (fit * (k - 1) - d)
		return Math.min(Math.max(k1 * d / (fit * k1 - d), 0.0), 1.0);
	}

	/**
	 * Helper method for initializing monomorphic populations. If the module admits
	 * vacant sites the frequency of individuals with the monomorphic trait is set
	 * to {@code monoFreq}.
	 * 
	 * @param monoType the monomorphic trait
	 * @param monoFreq the frequency of the monomorphic trait
	 */
	private void initMono(int monoType, double monoFreq) {
		monoType = monoType % nTraits;
		if (monoFreq > 1.0 - 1e-8) {
			Arrays.fill(strategies, monoType);
			strategiesTypeCount[monoType] = nPopulation;
		} else {
			// monomorphic population with VACANT sites
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
			// check if population exists
			if (nMono == 0)
				return;
			// relax the monomorphic configuration (ignore monoStop)
			// the actual monomorphic frequency may differ from the requested frequency
			// this is meaningful even for well-mixed populations
			boolean mono = module.getMonoStop();
			module.setMonoStop(false);
			Model model = engine.getModel();
			// update required to calculate scores/fitness
			model.update();
			model.relax();
			module.setMonoStop(mono);
		}
	}

	/**
	 * Monomorphic initial configuration with a single mutant placed in a location
	 * chosen uniformly at random (uniform initialization, cosmic rays).
	 * 
	 * @return the location of the mutant
	 * 
	 * @see IBSD.Init.Type#MUTANT
	 */
	protected int initMutant() {
		// initArgs contains the index of the resident and mutant traits
		int mutantType = (int) init.args[0] % nTraits;
		int len = init.args.length;
		int residentType;
		if (len > 1)
			residentType = (int) init.args[1] % nTraits;
		else
			residentType = (mutantType + 1) % nTraits;
		int loc;
		if (VACANT >= 0) {
			// if present the third argument indicates the frequency of vacant sites
			// if not use estimate for carrying capacity
			double monoFreq = (init.args.length > 2 ? Math.max(0.0, 1.0 - init.args[2])
					: 1.0 - estimateVacantFrequency(residentType));
			if (residentType == VACANT && monoFreq < 1.0 - 1e-8) {
				// problem encountered
				init.type = Init.Type.UNIFORM;
				logger.warning("review " + init.clo.getName() + //
						" settings! - using '" + init.type.getKey() + "'.");
				initUniform();
				return -1;
			}
			// initialize monomorphic resident population at carrying capacity
			// relax resident population to equilibrium configuration if requested
			initMono(residentType, monoFreq);
			// check if resident population went extinct or a single survivor
			// check if resident population went extinct or only a single survivor
			if (strategiesTypeCount[VACANT] >= nPopulation - 1)
				return -1;
			// change trait of random resident to a mutant
			int idx = random0n(getPopulationSize());
			loc = -1;
			while (idx >= 0) {
				if (isVacantAt(++loc))
					continue;
				idx--;
			}
		} else {
			initMono(residentType, 1.0);
			// change trait of random resident to a mutant
			loc = random0n(nPopulation);
		}
		strategies[loc] = mutantType;
		strategiesTypeCount[residentType]--;
		strategiesTypeCount[mutantType]++;
		return loc;
	}

	/**
	 * Monomorphic initial configuration with a single mutant placed in a random
	 * location chosen with probability proprtional to the number of incoming links
	 * (temperature initialization, errors in reproduction).
	 * 
	 * @return the location of the mutant
	 * 
	 * @see IBSD.Init.Type#TEMPERATURE
	 */
	protected int initTemperature() {
		int mutant = initMutant();
		if (interaction.isRegular || mutant < 0)
			return mutant;
		int mutantType = strategies[mutant];
		int residentType = (int) init.args[1];
		// revert mutant back to resident
		strategies[mutant] = residentType;
		// pick parent uniformly at random (everyone has the same fitness)
		int parent;
		if (VACANT < 0)
			parent = random0n(nPopulation);
		else {
			int idx = random0n(getPopulationSize());
			parent = -1;
			while (idx >= 0) {
				if (isVacantAt(++parent))
					continue;
				idx--;
			}
		}
		// now pick neighbouring node uniformly at random to place mutant
		// note: regular structures (including well-mixed) do not get here
		int nneighs = competition.kout[parent];
		if (nneighs == 0)
			// nowhere to place offspring...
			return -1;
		int idx = competition.out[parent][random0n(nneighs)];
		if (strategies[idx] == VACANT) {
			strategiesTypeCount[VACANT]--;
			// number of residents unchanged, initMono decreased it
			strategiesTypeCount[residentType % nTraits]++;
		}
		strategies[idx] = mutantType;
		return idx;
	}

	/**
	 * Initial configuration that generates evolutionary kaleidoscopes for
	 * deterministic update rules. Whether this is possible and and what kind of
	 * initial configurations are required depends on the module. Hence this method
	 * must be overriden in subclasses that admit kaleidoscopes.
	 * <p>
	 * <strong>Note:</strong> requires the explicit adding of the key
	 * {@link IBSD.Init.Type#KALEIDOSCOPE} for IBS models. For example, add
	 * 
	 * <pre>
	 * if (model instanceof IBSD) {
	 * 	CLOption clo = ((IBSDPopulation) getIBSPopulation()).getInit().clo;
	 * 	clo.addKey(Init.Type.KALEIDOSCOPE);
	 * }
	 * </pre>
	 * 
	 * to
	 * {@code org.evoludo.simulator.modules.Module#adjustCLO(org.evoludo.util.CLOParser)}.
	 * 
	 * @see IBSD.Init.Type#KALEIDOSCOPE
	 */
	protected void initKaleidoscope() {
	}

	/**
	 * Initial configuration with monomorphic stripes of each type to investigate
	 * invasion properties of one strategy into another with at least one instance
	 * of all possible pairings.
	 * 
	 * @see IBSD.Init.Type#STRIPES
	 */
	protected void initStripes() {
		// only makes sense for 2D lattices at this point. if not, defaults to uniform
		// random initialization (the only other inittype that doesn't require --init).
		Geometry.Type type = interaction.getType();
		if (interaction.interCompSame && (type == Geometry.Type.SQUARE ||
				type == Geometry.Type.SQUARE_NEUMANN ||
				type == Geometry.Type.SQUARE_MOORE ||
				type == Geometry.Type.LINEAR)) {
			Arrays.fill(strategiesTypeCount, 0);
			boolean act[] = module.getActiveTraits();
			int nActive = module.getNActive();
			int[] active = new int[nActive];
			int idx = 0;
			for (int n = 0; n < nTraits; n++) {
				if (!act[n])
					continue;
				active[idx++] = n;
			}
			// note: shift first strip by half a width to avoid having a boundary at the
			// edge. also prevents losing one trait interface with fixed boundary
			// conditions. procedure tested for 2, 3, 4, 5 traits
			int nStripes = nActive + 2 * sum(2, nActive - 2);
			int size = (interaction.getType() == Geometry.Type.LINEAR ? nPopulation
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
			for (int n = 1; n < nActive; n++) {
				fillStripe(offset, width, active[n]);
				offset += width;
			}
			// second all trait pairs
			int nPasses = Math.max(nActive - 2, 1);
			int incr = 2;
			while (incr <= nPasses) {
				int trait1 = 0;
				int trait2 = incr;
				while (trait2 < nActive) {
					fillStripe(offset, width, active[trait1++]);
					offset += width;
					fillStripe(offset, width, active[trait2++]);
					offset += width;
				}
				incr++;
			}
			Arrays.fill(strategiesTypeCount, 0);
			for (int n = 0; n < nPopulation; n++)
				strategiesTypeCount[strategies[n] % nTraits]++;
			return;
		}
		logger.warning("init 'stripes': 2D lattice structures required - using 'uniform'.");
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
	 * @see #initStripes()
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

	/**
	 * Type of initial configuration.
	 * 
	 * @see IBSD.Init#clo
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
	public boolean mouseHitNode(int hit, boolean alt) {
		if (hit < 0 || hit >= nPopulation)
			return false; // invalid argument
		int newtype = strategies[hit] % nTraits + nTraits + (alt ? -1 : 1);
		return mouseSetHit(hit, newtype % nTraits);
	}

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
			engine.getModel().update();
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
