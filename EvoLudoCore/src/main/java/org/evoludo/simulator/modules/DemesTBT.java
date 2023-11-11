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

package org.evoludo.simulator.modules;

import java.util.Arrays;

import org.evoludo.math.RNGDistribution;
import org.evoludo.simulator.EvoLudo;
import org.evoludo.simulator.Geometry;
import org.evoludo.simulator.models.IBS.MigrationType;
import org.evoludo.simulator.models.IBSD.InitType;
import org.evoludo.simulator.models.Model.Type;
import org.evoludo.simulator.models.IBSD;
import org.evoludo.simulator.models.Model;
import org.evoludo.util.CLOParser;
import org.evoludo.util.CLOption;
import org.evoludo.util.Formatter;

/**
 *
 * @author Christoph Hauert
 */
public class DemesTBT extends TBT {

	/**
	 * The number of demes.
	 */
	int nDemes;

	/**
	 * The size of each deme.
	 */
	int sizeDemes;

	public DemesTBT(EvoLudo engine) {
		super(engine);
	}

	@Override
	public void modelLoaded() {
		super.modelLoaded();
		// TBT added kaleidoscopes but demes cannot deal with them
		if (model.isModelType(Type.IBS))
			engine.cloInitType.removeKey(InitType.KALEIDOSCOPE);
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * Currently only IBS models are supported.
	 */
	@Override
	public boolean hasSupport(Model.Type type) {
		return type == Model.Type.IBS;
	}

	@Override
	public String getKey() {
		return "Demes2x2";
	}

	@Override
	public String getInfo() {
		return "Title: " + getTitle() + "\nAuthor: Christoph Hauert\n2x2 games in deme structured populations.";
	}

	@Override
	public String getTitle() {
		return "2x2 Games in Demes";
	}

	@Override
	public String getVersion() {
		return "v1.0 April 2021";
	}

	// /**
	// * {@inheritDoc}
	// * <p>
	// * <strong>Note:</strong> Demes cannot easily be treated using replicator
	// dynamics. Replicator dynamics should not be an updating option
	// * or possibly be implemented as a meta-population model.
	// */
	// @Override
	// public void avgScores(double[] density, int n, double[] avgscores) {
	// if( nDemes==1 ) {
	// // for a single deme the implementation of TwoByTwo will do
	// super.avgScores(density, n, avgscores);
	// return;
	// }
	// throw new Error("avgScores() should not get called!");
	// }

	@Override
	public void mixedScores(int[] count, double[] traitScores) {
		double ideme = 1.0 / (sizeDemes - 1);
		traitScores[COOPERATE] = ((count[COOPERATE] - 1) * reward + count[DEFECT] * sucker) * ideme;
		traitScores[DEFECT] = (count[COOPERATE] * temptation + (count[DEFECT] - 1) * punishment) * ideme;
	}

	@Override
	public void adjustCLO(CLOParser parser) {
		super.adjustCLO(parser);
		parser.removeCLO(new String[] { "geominter", "geomrepro" });

		cloGeometry.clearKeys();
		cloGeometry.addKey(Geometry.Type.HIERARCHY);
		cloGeometry.setDefault("H1"); // equivalent to well-mixed

		// PLAYER_UPDATE_BEST_REPLY requires avgScores which is not (yet? how?)
		// implemented for demes
		cloPlayerUpdate.removeKey(PlayerUpdateType.BEST_RESPONSE);

		CLOption option = ((org.evoludo.simulator.models.IBS) engine.getModel()).cloMigration;
		option.clearKeys();
		option.addKey(MigrationType.BIRTH_DEATH);
		option.setDefault("B0");
	}

	@Override
	public DemesTBT.IBS createIBSPop() {
		return new DemesTBT.IBS(engine, this);
	}

	public class IBS extends TBT.IBS implements Model.MilestoneListener, Model.ChangeListener {

		// IBSDPopulation pop;
		protected RNGDistribution.Geometric distrMigration, distrMutationMigration;
		private double[] pure;
		int[][] demeTypeCount;
		boolean optimizeMigration;

		protected IBS(EvoLudo engine, Discrete module) {
			super(engine);
		}

		@Override
		public void unload() {
			super.unload();
			demeTypeCount = null;
			pure = null;
			distrMigration = null;
			distrMutationMigration = null;
		}

		@Override
		public boolean check() {
			// inherit optimizeHomo setting - super may disable optimizeHomo if mutations
			// are disabled but we may still want to optimize migration.
			// TODO this is a bit of a hack, should add another optimization key for
			// migrations
			optimizeMigration = ((IBSD) engine.getModel()).optimizeHomo;
			boolean doReset = super.check();
			nTraits = module.getNTraits();
			if (!(interaction.isType(Geometry.Type.MEANFIELD) || (interaction.isType(Geometry.Type.HIERARCHY)
					&& interaction.subgeometry == Geometry.Type.MEANFIELD && interaction.hierarchy.length == 2))) {
				// the only acceptable geometries are well-mixed and hierarchical structures
				// with two levels of well-mixed demes
				logger.warning("invalid geometry - forcing well-mixed population (single deme)!");
				interaction.setType(Geometry.Type.MEANFIELD);
				doReset = true;
			}
			nDemes = 1;
			sizeDemes = nPopulation;
			if (pure == null || pure.length != nTraits)
				pure = new double[nTraits];
			if (interaction.isType(Geometry.Type.HIERARCHY)) {
				nDemes = interaction.hierarchy[0];
				sizeDemes = interaction.hierarchy[1];
			}
			if (demeTypeCount == null || demeTypeCount.length != nDemes)
				demeTypeCount = new int[nDemes][nTraits];
			if (nDemes == 1 || pMigration <= 0.0) {
				migrationType = MigrationType.NONE;
				pMigration = 0.0;
				optimizeMigration = false;
			}
			if (optimizeMigration) {
				// need to get new instances to make sure potential changes in pMigration,
				// pMutation are reflected. must be based on same RNG as the rest of simulations
				// to ensure reproducibility of results
				distrMigration = new RNGDistribution.Geometric(rng.getRNG(), pMigration);
				distrMutationMigration = new RNGDistribution.Geometric(rng.getRNG(),
						pMigration + module.getMutationProb());
			}
			return doReset;
		}

		@Override
		public void init() {
			super.init();

			for (int d = 0; d < nDemes; d++) {
				int skip = d * sizeDemes;
				Arrays.fill(demeTypeCount[d], 0);
				for (int n = 0; n < sizeDemes; n++)
					demeTypeCount[d][strategies[skip + n] % nTraits]++;
			}
			resetStatistics();
			startStatistics();
		}

		@Override
		public String getStatus() {
			double gen = engine.getModel().getTime();
			return super.getStatus() + ", pure: "
					+ (gen > 0.0 ? Formatter.formatPercent((pure[DemesTBT.COOPERATE] + pure[DemesTBT.DEFECT]) / gen, 1)
							: "0.0%");
		}

		@Override
		public boolean checkConvergence() {
			boolean absorbed = super.checkConvergence();

			int nA = homoDemes();
			if (nA > 0 && nA < nDemes) {
				// homogeneous demes but not homogeneous population; only mutants or migrants
				// change the state.
				if (pMutation <= 0.0 && !optimizeMigration)
					// pMigration=0: absorbed (heterogeneous population but homogeneous demes)
					// pMigration>0: waiting for migration to happen
					return (pMigration <= 0.0);

				// determine WHEN next event happens
				// XXX model.advanceTime(distrMutationMigration.next()/(double)nPopulation);
				double timehomo = distrMutationMigration.next() / (double) nPopulation;
				updateStatistics(timehomo);
				// determine WHAT event happens (sequence of this is of crucial importance!)
				if (random01() * (pMutation + pMigration) < pMigration) {
					// do migration
					doMigration();
				} else {
					// do mutation; pick individual proportional to fitness and determine its deme
					int hit = pickFitFocalIndividual();
					int homoType = strategies[hit] % nTraits;
					mutateStrategyAt(hit, false);
					if (adjustScores) {
						adjustGameScoresAt(hit);
					} else {
						// make sure everybody has the homogeneous payoff prior to introducing the
						// mutant
						// ATTENTION: this assumes small mutation rates - should we check?
						resetScores(module.getMonoGameScore(homoType));
						resetScoreAt(hit);
						commitStrategyAt(hit);
						playGameAt(hit);
					}
				}
				// XXX model.advanceTime(1.0/(double)nPopulation);
				engine.fireModelChanged();
				return false; // not absorbed
			}
			return absorbed;
		}

		/**
		 * 
		 * @return number of homogeneous A demes if all demes are homogeneous; -1 if any
		 *         deme is not homogeneous
		 */
		protected int homoDemes() {
			if (strategiesTypeCount[DemesTBT.COOPERATE] % sizeDemes != 0)
				// if number of cooperators is not a multiple of deme size
				// at least one deme must be heterogeneous
				return -1;

			int nA = 0;
			for (int d = 0; d < nDemes; d++) {
				int ccount = demeTypeCount[d][DemesTBT.COOPERATE];
				if (ccount == 0)
					continue;
				if (ccount == sizeDemes) {
					nA++;
					continue;
				}
				return -1;
			}
			return nA;
		}

		/**
		 * {@inheritDoc}
		 * <p>
		 * Override IBSPopulation.doDiffusionMigration() to take demes into account.
		 */
		@Override
		public void doDiffusionMigration() {
			int migrant = random0n(nPopulation);
			// migrant swaps places with random individual from another deme
			int migrantDeme = migrant / sizeDemes;
			int vacant = random0n(nPopulation - sizeDemes);
			if (vacant >= migrantDeme * sizeDemes)
				vacant += sizeDemes;
			updatePlayerSwap(migrant, vacant);
		}

		/**
		 * {@inheritDoc}
		 * <p>
		 * <strong>Notes:</strong>
		 * <ol>
		 * <li>Override IBSPopulation.doBirthDeathMigration() to take demes into
		 * account.
		 * <li>Almost identical (victim and migrant always different) to Moran
		 * (birth-death) updating in well-mixed population.
		 * </ol>
		 */
		@Override
		public void doBirthDeathMigration() {
			int migrant = pickFitFocalIndividual();
			// migrants may end up in parental deme and even replace the parent
			int vacant = random0n(nPopulation);
			// NOTE: updatePlayerMoran mutates offspring... undesirable!
			// copied from Population and removed mutations
			if (adjustScores) {
				if (haveSameStrategy(migrant, vacant))
					return;
				updateFromModelAt(vacant, migrant);
				adjustGameScoresAt(vacant);
				return;
			}
			if (!haveSameStrategy(migrant, vacant)) {
				// replace 'vaccant'
				updateFromModelAt(vacant, migrant);
				resetScoreAt(vacant);
				commitStrategyAt(vacant);
				playGameAt(vacant);
				return;
			}
			// no actual strategy change occurred - reset score always (default) or only on
			// actual change?
			if (playerScoreResetAlways)
				resetScoreAt(vacant);
			playGameAt(vacant);
		}

		/**
		 * {@inheritDoc}
		 * <ol>
		 * <li>Override IBSPopulation.doDeathBirthMigration() to take demes into
		 * account.
		 * <li>Almost identical (victim and migrant always different) to Moran
		 * (death-birth) updating in well-mixed population.
		 * </ol>
		 */
		@Override
		public void doDeathBirthMigration() {
			int vacant = random0n(nPopulation);
			int vacantDeme = vacant / sizeDemes;
			int start = vacantDeme * sizeDemes;
			// check if population is homogeneous or only the deme with a vacancy is
			// heterogeneous.
			// in that case all other individual are of the same type, i.e. a random
			// individual can
			// be drawn - no need to check fitness.
			int ccount = strategiesTypeCount[DemesTBT.COOPERATE];
			if (ccount == 0 || ccount == nPopulation || ccount == demeTypeCount[vacantDeme][DemesTBT.COOPERATE]
					|| nPopulation - ccount == demeTypeCount[vacantDeme][DemesTBT.DEFECT]) {
				int migrant = random0n(nPopulation - sizeDemes);
				if (migrant >= start)
					migrant += sizeDemes;
				// XXX NOTE: updatePlayerMoran checks for mutations - this may not be what we
				// want...
				updatePlayerMoran(migrant, vacant);
				return;
			}
			// migrate - repopulate vacant site with random individual from another deme
			double vacantFitness = 0.0;
			for (int n = start; n < start + sizeDemes; n++)
				vacantFitness += getFitnessAt(n);

			// decrease interval slightly to prevent roundoff errors
			double hit = random01() * (sumFitness - vacantFitness - 1e-6);
			for (int n = 0; n < nPopulation; n++) {
				if (n == start)
					n += sizeDemes;
				hit -= getFitnessAt(n);
				if (hit < 0.0) {
					// XXX NOTE: updatePlayerMoran checks for mutations - this may not be what we
					// want...
					updatePlayerMoran(n, vacant);
					return;
				}
			}
			// last resort
			throw new Error("Dispersal failed... (" + hit + ")");
		}

		@Override
		protected void updatePlayerMoranBirthDeathAt(int parent) {
			// regular updates within single deme
			int myDeme = parent / sizeDemes;
			// replace random member of that deme - in particular, this includes parent
			int expired = myDeme * sizeDemes + random0n(sizeDemes);
			// proceed as before
			updatePlayerMoran(parent, expired);
		}

		@Override
		public synchronized void modelChanged(PendingAction pending) {
			updateStatistics(engine.getModel().getTime());
		}

		@Override
		public synchronized void modelStopped() {
			updateStatistics(engine.getNGenerations());
		}

		protected double prevsample = Double.MAX_VALUE;

		protected void startStatistics() {
			prevsample = engine.getModel().getTime();
		}

		protected void resetStatistics() {
			if (pure == null)
				pure = new double[nTraits];
			prevsample = Double.MAX_VALUE;
			Arrays.fill(pure, 0.0);
		}

		protected void updateStatistics(double time) {
			if (prevsample >= time)
				return;
			double incr = time - prevsample;
			if (strategiesTypeCount[DemesTBT.COOPERATE] == nPopulation)
				pure[DemesTBT.COOPERATE] += incr;
			if (strategiesTypeCount[DemesTBT.DEFECT] == nPopulation)
				pure[DemesTBT.DEFECT] += incr;
			prevsample = time;
		}

		@Override
		public void commitStrategyAt(int me) {
			int deme = me / sizeDemes;
			int newstrat = strategiesScratch[me];
			int oldtype = strategies[me] % nTraits;
			demeTypeCount[deme][oldtype]--;
			demeTypeCount[deme][newstrat % nTraits]++;
			super.commitStrategyAt(me);
		}

		@Override
		public void commitStrategies() {
			super.commitStrategies();
			for (int d = 0; d < nDemes; d++) {
				int skip = d * sizeDemes;
				Arrays.fill(demeTypeCount[d], 0);
				for (int n = 0; n < sizeDemes; n++)
					demeTypeCount[d][strategies[skip + n] % nTraits]++;
			}
		}

		/**
		 * {@inheritDoc}
		 * <p>
		 * Overridden to set scores in demes more efficiently
		 */
		@Override
		public void updateScores() {
			if (adjustScores) {
				updateMixedMeanScores();
				return;
			}
			// original procedure
			super.updateScores();
		}

		/**
		 * {@inheritDoc}
		 * <p>
		 * Overridden to set scores in demes more efficiently
		 */
		@Override
		public void adjustGameScoresAt(int me) {
			// check whether an actual strategy change has occurred
			// note: isSameStrategy() only works before committing strategy!
			if (isSameStrategy(me)) {
				commitStrategyAt(me);
				return;
			}
			commitStrategyAt(me);
			updateMixedMeanScores();
		}
	}
}