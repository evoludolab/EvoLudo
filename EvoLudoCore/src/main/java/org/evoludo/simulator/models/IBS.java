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
import org.evoludo.simulator.ColorMap;
import org.evoludo.simulator.EvoLudo;
import org.evoludo.simulator.Geometry;
import org.evoludo.simulator.models.ChangeListener.PendingAction;
import org.evoludo.simulator.modules.Features;
import org.evoludo.simulator.modules.Features.Payoffs;
import org.evoludo.simulator.modules.Map2Fitness;
import org.evoludo.simulator.modules.Module;
import org.evoludo.simulator.modules.Mutation;
import org.evoludo.simulator.modules.SpeciesUpdate;
import org.evoludo.simulator.views.HasHistogram;
import org.evoludo.util.CLOParser;
import org.evoludo.util.CLOption;
import org.evoludo.util.CLOption.CLODelegate;
import org.evoludo.util.CLOption.Category;
import org.evoludo.util.Formatter;
import org.evoludo.util.Plist;

/**
 * Base class for individual based simulation models, IBS. This class deals with
 * multiple species, handles the milestone events and delegates to the different
 * populations as appropriate.
 * 
 * @author Christoph Hauert
 */
public abstract class IBS extends Model {

	/**
	 * Modules that offer individual based simulation models must implement this
	 * interface.
	 */
	public interface HasIBS {

		/**
		 * Provides opportunity for module to supply custom implementation of individual
		 * based simulations, IBS.
		 * <p>
		 * <strong>Important:</strong> if the custom IBS implementation involves random
		 * numbers, the shared random number generator must be used for reproducibility.
		 * 
		 * @return the custom implementation of the IBS or <code>null</code> to use the
		 *         default
		 * 
		 * @see EvoLudo#getRNG()
		 */
		public default Model createIBS() {
			return null;
		}

		/**
		 * Modules that offer individual based simulation models with discrete traits
		 * and pairwise interactions must implement this interface.
		 */
		public interface DPairs extends IBS.HasIBS, Features.Pairs {

			/**
			 * Calculate and return total (accumulated) payoff/score for pairwise
			 * interactions of the focal individual with trait {@code me} against opponents
			 * with different traits. The respective numbers of each of the {@code nTraits}
			 * opponent traits are provided in the array {@code tCount}. The payoffs/scores
			 * for each of the {@code nTraits} opponent traits must be stored and returned
			 * in the array {@code tScore}.
			 * <p>
			 * <strong>Important:</strong> must be overridden and implemented in subclasses
			 * that define game interactions between pairs of individuals
			 * ({@code nGroup=2}, {@code pairwise=true}), otherwise see
			 * {@link DGroups#groupScores(int[], double[])}.
			 * 
			 * @param me         the trait index of the focal individual
			 * @param traitCount number of opponents with each trait
			 * @param traitScore array for returning the scores of each opponent trait
			 * @return score of focal individual {@code me} accumulated over all
			 *         interactions
			 */
			public double pairScores(int me, int[] traitCount, double[] traitScore);

			/**
			 * Calculate the average payoff/score in a finite population with the number of
			 * each trait provided in {@code count} for pairwise interactions. The
			 * payoffs/scores for each of the {@code nTraits} traits must be stored and
			 * returned in the array {@code traitScores}.
			 * <p>
			 * <strong>Important:</strong> must be overridden and implemented in subclasses
			 * that define game interactions in well-mixed populations where individuals
			 * interact with everyone else. Computationally it is not feasible to cover this
			 * scenario with {@link #pairScores(int, int[], double[])} or
			 * {@link DGroups#groupScores(int[], double[])}, respectively.
			 * <p>
			 * <strong>Note:</strong> If explicit calculations of the well-mixed scores are
			 * not available, interactions with everyone in well-mixed populations should
			 * checked for and excluded with a warning in {@link #check()} (see
			 * {@link org.evoludo.simulator.models.IBSMCPopulation#check() CXPopulation} for
			 * an example).
			 * 
			 * @param traitCount number of individuals for each trait
			 * @param traitScore array for returning the payoffs/scores of each trait
			 */
			public void mixedScores(int[] traitCount, double[] traitScore);
		}

		/**
		 * Modules that offer individual based simulation models with discrete traits
		 * and interactions in groups must implement this interface.
		 */
		interface DGroups extends DPairs, Features.Groups {

			/**
			 * Calculate the payoff/score for interactions in groups consisting of
			 * traits with respective numbers given in the array {@code traitCount}. The
			 * interaction group size is given by the sum over {@code traitCount[i]} for
			 * {@code i=0,1,...,nTraits}. The payoffs/scores for each of the {@code nTraits}
			 * traits must be stored and returned in the array {@code traitScore}.
			 * <p>
			 * <strong>Important:</strong> must be overridden and implemented in subclasses
			 * that define game interactions among groups of individuals (for groups with
			 * sizes {@code nGroup&gt;2}, otherwise see
			 * {@link #pairScores(int, int[], double[])}).
			 * 
			 * @param traitCount group composition given by the number of individuals with
			 *                   each trait
			 * @param traitScore array for returning the payoffs/scores of each trait
			 */
			public void groupScores(int[] traitCount, double[] traitScore);

			/**
			 * Calculate the average payoff/score in a finite population with the number of
			 * each trait provided in {@code count} for interaction groups of size
			 * {@code n}. The payoffs/scores for each of the {@code nTraits} traits must be
			 * stored and returned in the array {@code traitScores}.
			 * 
			 * <h3>Notes:</h3>
			 * For payoff calculations:
			 * <ul>
			 * <li>each trait sees one less of its own type in its environment
			 * <li>the size of the environment is {@code nPopulation-1}
			 * <li>the fact that the payoff of each trait does not depend on its own type
			 * simplifies things
			 * </ul>
			 * If explicit calculations of the well-mixed scores are not available,
			 * interactions with everyone in well-mixed populations should be checked for
			 * and excluded with a warning in {@link #check()} (see
			 * {@link org.evoludo.simulator.models.IBSMCPopulation#check() IBSMCPopulation}
			 * for an example).
			 * 
			 * <h3>Important:</h3>
			 * Must be overridden and implemented in subclasses that define game
			 * interactions in well-mixed populations where individuals interact with
			 * everyone else. Computationally it is not feasible to cover this scenario with
			 * {@link #pairScores(int, int[], double[])} or
			 * {@link #groupScores(int[], double[])}, respectively.
			 * 
			 * @param traitCount number of individuals for each trait
			 * @param n          interaction group size
			 * @param traitScore array for returning the payoffs/scores of each trait
			 */
			public void mixedScores(int[] traitCount, int n, double[] traitScore);

			@Override
			public default void mixedScores(int[] traitCount, double[] traitScore) {
				mixedScores(traitCount, 2, traitScore);
			}
		}

		/**
		 * Modules that offer individual based simulation models with continuous traits
		 * and pairwise interactions must implement this interface.
		 */
		interface CPairs extends IBS.HasIBS, Features.Pairs {
			/**
			 * Calculate the payoff/score for modules with interactions in pairs and a
			 * single continuous trait. The focal individual has trait {@code me} and the
			 * traits of its {@code len} interaction partners are given in {@code group}.
			 * The payoffs/scores for each of the {@code len} opponent
			 * traits must be stored and returned in the array {@code payoffs}.
			 * 
			 * <h3>Note:</h3> Only the first {@code len} entries in {@code group} are
			 * guaranteed to exist and have meaningful values. The population structure may
			 * restrict the size of the interaction group. {@code len&le;nGroup} always
			 * holds.
			 * 
			 * <h3>Important:</h3> must be overridden and implemented in subclasses that
			 * define game interactions between pairs of individuals
			 * ({@code nGroup=2}, {@code pairwise=true}), otherwise see
			 * {@link CGroups#groupScores(double, double[], int, double[])}.
			 * 
			 * @param me           the trait of the focal individual
			 * @param groupTraits  the traits of the group members
			 * @param len          the number of memebrs in the group
			 * @param groupPayoffs the array for returning the payoffs/scores for each group
			 *                     member
			 * @return the total (accumulated) payoff/score for the focal individual
			 */
			public double pairScores(double me, double[] groupTraits, int len, double[] groupPayoffs);
		}

		/**
		 * Modules that offer individual based simulation models with continuous traits
		 * and interactions in groups must implement this interface.
		 */
		interface CGroups extends CPairs, Features.Groups {

			/**
			 * Calculate the payoff/score for modules with interactions in groups and a
			 * single continuous trait. The focal individual has trait {@code me} and the
			 * traits of its {@code len} interaction partners are given in {@code group}.
			 * The payoffs/scores for each of the {@code len} participants must be
			 * stored and returned in the array {@code payoffs}.
			 * 
			 * <h3>Note:</h3> Only the first {@code len*nTraits} entries in {@code group}
			 * are guaranteed to exist and have meaningful values. The population structure
			 * may restrict the size of the interaction group. {@code len&le;nGroup} always
			 * holds.
			 * 
			 * <h3>Important:</h3> Must be overridden and implemented in subclasses that
			 * define game interactions among groups of individuals with multiple continuous
			 * traits (for groups with sizes {@code nGroup&gt;2}, otherwise see
			 * {@link #pairScores(double, double[], int, double[])}).
			 * 
			 * @param me           the trait of the focal individual
			 * @param groupTraits  the traits of the group members
			 * @param len          the number of members in the group
			 * @param groupPayoffs the array for returning the payoffs/scores for each group
			 *                     member
			 * @return the payoff/score for the focal individual
			 */
			public double groupScores(double me, double[] groupTraits, int len, double[] groupPayoffs);
		}

		/**
		 * Modules that offer individual based simulation models with multiple
		 * continuous traits and pairwise interactions must implement this interface.
		 */
		interface MCPairs extends IBS.HasIBS, Features.Pairs {
			/**
			 * Calculate the payoff/score for modules with interactions in pairs and
			 * multiple continuous traits. The focal individual has traits {@code me} and
			 * the traits of its {@code len} interaction partners are given in
			 * {@code group}. The traits they are arranged in the usual manner, i.e. first
			 * all traits of the first group member then all traits by the second group
			 * member etc. for a total of {@code len*nTraits} entries. The payoffs/scores
			 * for each of the {@code len} opponent traits must be stored and returned in
			 * the array {@code payoffs}.
			 * 
			 * <h3>Note:</h3> Only the first {@code len} entries in {@code group} are
			 * guaranteed to exist and have meaningful values. The population structure may
			 * restrict the size of the interaction group. {@code len&le;nGroup} always
			 * holds.
			 * 
			 * <h3>Important:</h3> must be overridden and implemented in subclasses that
			 * define game interactions between pairs of individuals
			 * ({@code nGroup=2}, {@code pairwise=true}), otherwise see
			 * {@link MCGroups#groupScores(double, double[], int, double[])}.
			 * 
			 * @param me           the trait of the focal individual
			 * @param groupTraits  the traits of the group members
			 * @param len          the number of memebrs in the group
			 * @param groupPayoffs the array for returning the payoffs/scores for each group
			 *                     member
			 * @return the total (accumulated) payoff/score for the focal individual
			 */
			public double pairScores(double me[], double[] groupTraits, int len, double[] groupPayoffs);
		}

		/**
		 * Modules that offer individual based simulation models with continuous traits
		 * and interactions in groups must implement this interface.
		 */
		interface MCGroups extends CGroups {

			/**
			 * Calculate the payoff/score for modules with interactions in groups and
			 * multiple single continuous traits. The focal individual has traits {@code me}
			 * and the traits of its {@code len} interaction partners are given in
			 * {@code group}. The traits they are arranged in the usual manner, i.e. first
			 * all traits of the first group member then all traits by the second group
			 * member etc. for a total of {@code len*nTraits} entries. The payoffs/scores
			 * for each of the {@code len} participants must be stored and returned in
			 * the array {@code payoffs}.
			 * 
			 * <h3>Note:</h3> Only the first {@code len*nTraits} entries in {@code group}
			 * are guaranteed to exist and have meaningful values. The population structure
			 * may restrict the size of the interaction group. {@code len&le;nGroup} always
			 * holds.
			 * 
			 * <h3>Important:</h3> must be overridden and implemented in subclasses that
			 * define game interactions among groups of individuals with multiple continuous
			 * traits (for groups with sizes {@code nGroup&gt;2}, otherwise see
			 * {@link CPairs#pairScores(double, double[], int, double[])}).
			 * 
			 * @param me      the traits of the focal individual
			 * @param group   the traits of the group members
			 * @param len     the number of memebrs in the group
			 * @param payoffs the array for returning the payoffs/scores for each group
			 *                member
			 * @return the payoff/score for the focal individual
			 */
			public double groupScores(double me[], double[] group, int len, double[] payoffs);
		}
	}

	@Override
	public boolean permitsSampleStatistics() {
		if (species == null)
			return false;
		for (Module mod : species) {
			if (mod.getMutation().probability > 0.0 || !(mod instanceof HasHistogram.StatisticsProbability
					|| mod instanceof HasHistogram.StatisticsTime))
				return false;
		}
		return true;
	}

	@Override
	public boolean permitsUpdateStatistics() {
		if (species == null)
			return false;
		for (Module mod : species) {
			if (!(mod instanceof HasHistogram.StatisticsStationary))
				return false;
		}
		return true;
	}

	/**
	 * The random number generator to display states with ephemeral payoffs. In
	 * order to ensure reproducibility of results this cannot be the same random
	 * number generator as for running the simulations.
	 * 
	 * @see EvoLudo#getRNG()
	 */
	protected RNGDistribution ephrng;

	/**
	 * Geometric (exponential) waiting time distribution for optimizations of
	 * homogeneous populations.
	 * 
	 * @see IBSD#cloOptimize
	 */
	protected RNGDistribution.Geometric distrMutation;

	/**
	 * Short-cut to {@code species.get(0).getIBSPopulation()} for single species
	 * models; {@code null} in multi-species models. Convenience field.
	 */
	protected IBSPopulation population;

	/**
	 * Keeps track of the elapsed time, taking into account the fitness of the
	 * population. For example, less time passes between reproductive events in
	 * populations with high fitness, while more time passes in low fitness
	 * populations because there are fewer reproduction events per unit time. If
	 * individual scores can be negative {@code realtime} is set to
	 * {@code Double#POSITIVE_INFINITY} to indicate that the measure is meaningless.
	 * <p>
	 * <strong>Note:</strong> Requires non-negative individual scores.
	 */
	protected double realtime = -1.0;

	/**
	 * <code>true</code> if optimizations for homogeneous populations requested.
	 * <p>
	 * <strong>Note:</strong>
	 * <ul>
	 * <li>optimizations can be requested with the command line option
	 * <code>--optimize</code>, see {@link IBSD#cloOptimize}.</li>
	 * <li>currently restricted to discrete traits where homogeneous population
	 * states can be skipped by deterministically introducing new mutant after an
	 * geometrically (exponentially) distributed waiting time (see
	 * {@link IBSD}).</li>
	 * <li>requires small mutation rates.</li>
	 * <li>does not work for variable population sizes.</li>
	 * </ul>
	 */
	public boolean optimizeHomo = false;

	/**
	 * Flag to indicate whether the population updates are synchronous. In
	 * multi-species models this requires that all species are updated
	 * synchronously. Helper variable for {@code ibsStep(double)}.
	 * 
	 * @see PopulationUpdate#clo
	 * @see #check()
	 * @see #ibsStep(double)
	 */
	boolean isSynchronous;

	/**
	 * Creates a population of individuals for IBS simulations.
	 * 
	 * @param engine the pacemaker for running the model
	 */
	public IBS(EvoLudo engine) {
		super(engine);
		type = Type.IBS;
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * Loads the IBS model and instantiates the {@link IBSPopulation}s representing
	 * the different species, if applicable.
	 * 
	 * @see IBSPopulation
	 * @see IBSDPopulation
	 * @see IBSMCPopulation
	 * @see IBSCPopulation
	 */
	@Override
	public void load() {
		super.load();
		for (Module mod : species) {
			IBSPopulation pop = mod.createIBSPop();
			if (pop == null) {
				if (mod instanceof org.evoludo.simulator.modules.Discrete) {
					pop = new IBSDPopulation(engine, (org.evoludo.simulator.modules.Discrete) mod);
				} else if (mod instanceof org.evoludo.simulator.modules.Continuous) {
					// continuous module, check trait number
					if (mod.getNTraits() > 1)
						pop = new IBSMCPopulation(engine, (org.evoludo.simulator.modules.Continuous) mod);
					else
						pop = new IBSCPopulation(engine, (org.evoludo.simulator.modules.Continuous) mod);
				} else {
					engine.fatal("unknown module type '" + mod + "'... fix me!");
					// fatal does not return control
				}
			}
			mod.setIBSPopulation(pop);
		}
		// now that all populations are instantiated, we can assign opponents
		for (Module mod : species) {
			IBSPopulation pop = mod.getIBSPopulation();
			// set opponents
			pop.setOpponentPop(mod.getOpponent().getIBSPopulation());
		}
		Module main = species.get(0);
		// set shortcut for single species modules
		population = isMultispecies ? null : main.getIBSPopulation();
		cloGeometryInteraction.inheritKeysFrom(main.cloGeometry);
		cloGeometryCompetition.inheritKeysFrom(main.cloGeometry);
		if (isMultispecies)
			speciesUpdate = new SpeciesUpdate(main);
		statisticsSettings = new Statistics(this);
	}

	@Override
	public void unload() {
		ephrng = null;
		population = null;
		cloGeometryInteraction.clearKeys();
		cloGeometryCompetition.clearKeys();
		cloMigration.clearKeys();
		for (Module mod : species)
			mod.setIBSPopulation(null);
		speciesUpdate = null;
		statisticsSettings = null;
		super.unload();
	}

	@Override
	public boolean check() {
		boolean doReset = super.check();
		boolean allSync = true;
		boolean allAsync = true;
		boolean noneUnique = true;
		boolean allPosFitness = true;
		for (Module mod : species) {
			IBSPopulation pop = mod.getIBSPopulation();
			doReset |= pop.check();
			boolean sync = pop.getPopulationUpdate().isSynchronous();
			allSync &= sync;
			allAsync &= !sync;
			if (mod instanceof Payoffs) {
				// check if all payoffs are positive
				if (mod.getMap2Fitness().map(((Payoffs) mod).getMinPayoff()) <= 0.0)
					allPosFitness = false;
			}
			if (!noneUnique)
				continue;
			Geometry geom = pop.getInteractionGeometry();
			if (geom != null) {
				noneUnique &= !geom.isUniqueGeometry();
				if (geom.interCompSame)
					continue;
			}
			geom = pop.getCompetitionGeometry();
			if (geom != null)
				noneUnique &= !geom.isUniqueGeometry();
		}
		isSynchronous = !allAsync;
		if (isSynchronous && !allSync) {
			logger.warning("cannot (yet) mix synchronous and asynchronous population updates - forcing '"
					+ PopulationUpdate.Type.SYNC + "'");
			for (Module mod : species)
				mod.getIBSPopulation().getPopulationUpdate().setType(PopulationUpdate.Type.SYNC);
			doReset = true;
		}
		if (isMultispecies && !allPosFitness && speciesUpdate.getType() == SpeciesUpdate.Type.FITNESS) {
			// fitness based picking of focal species requires positive fitness
			logger.warning("multispecies models with '" + SpeciesUpdate.Type.FITNESS + "' require positive minimum fitness - switching to '"
					+ SpeciesUpdate.Type.RATE + "'");
			speciesUpdate.setType(SpeciesUpdate.Type.RATE);
		}
		// update converged flag to account for changes that preclude convergence
		if (!doReset) {
			for (Module mod : species) {
				IBSPopulation pop = mod.getIBSPopulation();
				converged &= pop.checkConvergence();
			}
		}
		// if no geometries are unique no need to reset model for statistics (init is sufficient)
		if (noneUnique)
			statisticsSettings.resetInterval = 0;
		return doReset;
	}

	@Override
	public void reset() {
		super.reset();
		// with optimization, homogeneous populations do not wait for next mutation
		// currently only relevant for discrete traits, see IBSD
		if (optimizeHomo) {
			// NOTE: optimizeHomo == false if no mutations or isMultispecies == true
			double pMutation = species.get(0).getMutation().probability;
			if (distrMutation == null)
				distrMutation = new RNGDistribution.Geometric(rng.getRNG(), pMutation);
			else
				distrMutation.setProbability(pMutation);
		}
		// if any population uses ephemeral payoffs a dummy random number
		// generator is needed for the display
		for (Module mod : species) {
			IBSPopulation pop = mod.getIBSPopulation();
			if (pop.getPlayerScoring().equals(ScoringType.EPHEMERAL)) {
				ephrng = rng.clone();
				break;
			}
		}
		nextSpeciesIdx = -1;
		for (Module mod : species) {
			IBSPopulation pop = mod.getIBSPopulation();
			pop.reset();
		}
	}

	@Override
	public void init() {
		init(false);
	}

	/**
	 * Initializes the IBS model. {@code soft} initializations adjust parameters but
	 * do not touch the current state of the IBS population(s).
	 * <p>
	 * <strong>Note:</strong> Method must be {@code public} because of subclasses in
	 * {@code org.evoludo.simulator}.
	 * 
	 * @param soft the flag to indicate whether this should be a {@code soft}
	 *             initialization.
	 */
	public void init(boolean soft) {
		super.init();
		// realtime meaningless if not all populations have positive minimum fitness
		realtime = 0.0;
		for (Module mod : species) {
			if (mod instanceof Payoffs) {
				Map2Fitness map2fit = mod.getMap2Fitness();
				if (map2fit.map(((Payoffs) mod).getMinPayoff()) <= 0.0) {
					realtime = Double.POSITIVE_INFINITY;
					break;
				}
				continue;
			}
			realtime = Double.POSITIVE_INFINITY;
			break;
		}
		connect = false;
		if (soft) {
			// signal change to engine without destroying state
			// used for simulations in systems with long relaxation times
			engine.paramsDidChange();
			return;
		}
		// initialize all populations
		for (Module mod : species) {
			IBSPopulation pop = mod.getIBSPopulation();
			pop.init();
		}
		// check for convergence separately because initialization may want to 
		// relax the configuration, which could result in convergence
		converged = true;
		for (Module mod : species) {
			IBSPopulation pop = mod.getIBSPopulation();
			converged &= pop.checkConvergence();
		}
	}

	@Override
	public void update() {
		// all populations need to be updated/reset before scores can be calculated for
		// inter-species interactions
		for (Module mod : species) {
			if (mod instanceof Payoffs) {
				IBSPopulation pop = mod.getIBSPopulation();
				pop.resetScores();
			}
		}
		for (Module mod : species) {
			if (mod instanceof Payoffs) {
				IBSPopulation pop = mod.getIBSPopulation();
				pop.updateScores();
			}
		}
	}

	@Override
	public boolean next() {
		// start new statistics sample if required
		if (mode == Mode.STATISTICS_SAMPLE && statisticsSampleNew && !isRelaxing) {
			if (statisticsSettings.resetInterval > 0 && 
					nStatisticsSamples % statisticsSettings.resetInterval == 0)
				reset();
			init();
			if (fixData.mutantNode < 0) {
				initStatisticsFailed();
				engine.requestAction(PendingAction.STATISTIC_FAILED, true);
				// check if STOP has been requested
				return engine.isRunning();
			}
			initStatisticsSample();
			update();
			// debugCheck("next (new sample)");
			return true;
		}
		// convergence only signaled without mutations
		if (converged && !optimizeHomo) {
			return false;
		}
		// this implies single species
		// (otherwise optimizeHomo == false and population == null)
		if (optimizeHomo && population.isMonomorphic()) {
			engine.fireModelRunning();
			// optimize waiting time in homogeneous states by advancing time and
			// deterministically introduce new mutant (currently single species only)
			// optimizeHomo also requires that mutations are rare (otherwise this
			// optimization is pointless); more specifically mutation rates
			// <0.1/nPopulation such that mutations occur less than every 10
			// generations and hence scores can be assumed to be homogeneous when
			// the mutant arises.
			Module module = population.getModule();
			double realunit = 1.0 / population.getSpeciesUpdateRate();
			int nPop = module.getNPopulation();
			double unit = 1.0 / nPop;
			// skip time to next event
			int dt = distrMutation.next();
			// XXX this can easily skip past requested stops - ignore?
			updates += dt * unit;
			realtime += RNGDistribution.Exponential.next(rng.getRNG(), dt * realunit);
			population.resetTraits();
			update();
			// communicate update
			engine.fireModelChanged();
			realtime += realunit;
			updates += unit;
			// introduce mutation uniformly at random
			population.mutateAt(random0n(nPop));
			return true;
		}
		double nextHalt = getNextHalt();
		// continue if milestone reached in previous step, i.e. deltat < 1e-8
		double step = timeStep;
		double incr = Math.abs(nextHalt - updates);
		if (incr < 1e-8)
			return false;
		step = Math.min(step, incr);
		connect = true;
		if (!ibsStep(step)) {
			converged = true;
			return false;
		}
		// note: time resolution is limited. need to allow for some slack when
		// signalling convergence. using 1/nPopulation of the first species is
		// an approximation but hopefully good enough. deviations expected in
		// multi-species modules with different population sizes or different
		// update rates.
		double minIncr = 1.0 / species.get(0).getNPopulation();
		return (Math.abs(nextHalt - updates) >= minIncr);
	}

	/**
	 * Pointer to focal species for debugging.
	 */
	IBSPopulation debugFocalSpecies;

	/**
	 * Advances the IBS model by a step of size {@code stepDt}. The actual time
	 * increment may be shorter, e.g. upon reaching an absorbing state or
	 * homogeneous state, if requested.
	 * <p>
	 * <strong>Note:</strong> the time increment returned is negative if the IBS
	 * converged/absorbed (individual based simulations cannot reverse time).
	 * 
	 * @param stepDt the time increment requested for advancing the IBS model
	 * @return <code>true</code> if <code>ibsStep(double)</code> can be called again.
	 *         Typically <code>false</code> is returned if the simulation requires
	 *         attention, such as the following conditions:
	 *         <ul>
	 *         <li>the population(s) have reached an absorbing state
	 *         <li>the population(s) turned monomorphic (stops only if requested)
	 *         <li>the population(s) went extinct
	 *         </ul>
	 * 
	 * @see org.evoludo.simulator.modules.Discrete#cloMonoStop
	 */
	public boolean ibsStep(double stepDt) {
		// synchronous population updates
		if (isSynchronous) {
			int nPopTot = 0;
			double scoreTot = 0.0;
			double popFrac = 1.0;
			// reset traits (colors)
			for (Module mod : species) {
				IBSPopulation pop = mod.getIBSPopulation();
				pop.resetTraits();
				popFrac *= pop.getSyncFraction();
				nPopTot += pop.getPopulationSize();
				scoreTot += pop.getTotalFitness();
			}
			// nUpdates measured in generations
			int nUpdates = Math.max(1, (int) Math.floor(stepDt));
			for (int f = 0; f < nUpdates; f++) {
				// advance time and real time (if possible)
				realtime = (scoreTot <= 1e-8 ? Double.POSITIVE_INFINITY : realtime + nPopTot * popFrac / scoreTot);
				updates += popFrac;
				// update populations
				for (Module mod : species) {
					IBSPopulation pop = mod.getIBSPopulation();
					pop.prepareTraits();
					pop.step();
					pop.isConsistent();
				}
				// commit traits and reset scores
				for (Module mod : species) {
					IBSPopulation pop = mod.getIBSPopulation();
					pop.commitTraits(); // also check homogeneity
					// TODO: review migration - should be an independent event, independent of
					// population update
					// NOTE: should time advance? e.g. based on number of mutants
					pop.doSyncMigration(); // do migration
					// all scores must be reset before we can re-calculate them
					pop.resetScores();
				}
				// calculate new scores (requires that all traits are committed and reset)
				converged = true;
				for (Module mod : species) {
					IBSPopulation pop = mod.getIBSPopulation();
					pop.updateScores();
					converged &= pop.checkConvergence();
					scoreTot += pop.getTotalFitness();
				}
				if (converged)
					return false;
			}
			return true;
		}

		// asynchronous population update - update one individual at a time
		double nTot = 0.0;
		double totRate = 0.0;
		// reset traits (colors)
		for (Module mod : species) {
			IBSPopulation pop = mod.getIBSPopulation();
			pop.resetTraits();
			// NOTE: generation time increments based on maximum population sizes; otherwise
			// notion of generations gets confusing; for ecological settings realtime might 
			// be more relevant.
			nTot += mod.getNPopulation();
			totRate += pop.getSpeciesUpdateRate();
		}
		// gincr is a constant because based on total maximum population sizes
		double gincr = 1.0 / nTot;
		// process at least one update.
		// NOTE: nUpdates can exceed Integer.MAX_VALUE (notably for large populations and
		// long relaxation times). switching to long is not an option because of GWT!
		double dUpdates = Math.max(1.0, Math.ceil(stepDt / gincr - 1e-8));
		double stepDone = 0.0;
		double gStart = updates;
		updates:
		while (dUpdates >= 1.0) {
			double stepSize = 0.0;
			int nUpdates = Math.min((int) dUpdates, 1000000000); // 1e9 about half of Integer.MAX_VALUE (2.1e9)
			for (int n = 0; n < nUpdates; n++) {
				// update event
				int dt = 0;
				debugFocalSpecies = pickFocalSpecies();
				switch (pickEvent(debugFocalSpecies)) {
					// standard replication event
					case REPLICATION:
						dt = debugFocalSpecies.step();
						break;
					// uniform mutation event (temperature based mutations are part of replication events)
					case MUTATION:
						dt = debugFocalSpecies.mutate();
						break;
					// uniform migration events (temperature based migrations are part of replication events)
					// case MIGRATION:
					// dt = debugFocalSpecies.migrate();
					// break;
					default:
						engine.fatal("unknown event type...");
				}
				// advance time and real time (if possible)
				if (dt == 0) {
					// if no time elapsed, nothing happened
					n--;
					continue;
				}
				if (debugFocalSpecies.getPopulationUpdate().getType() == PopulationUpdate.Type.ONCE) {
					updates++;
					n += debugFocalSpecies.getModule().getNPopulation();
				} else {
					updates += gincr;
				}
				converged = true;
				if (dt > 0 && realtime < Double.POSITIVE_INFINITY)
					realtime += RNGDistribution.Exponential.next(rng.getRNG(), dt / totRate);
				totRate = 0.0;
				for (Module mod : species) {
					IBSPopulation pop = mod.getIBSPopulation();
					pop.isConsistent();
					converged &= pop.checkConvergence();
					totRate += pop.getSpeciesUpdateRate();
				}
				if (converged) {
					stepSize = n * gincr;
					stepDone += Math.abs(stepSize);
					updates = gStart + Math.abs(stepDone);
					break updates;
				}
			}
			stepSize = nUpdates * gincr;
			stepDone += Math.abs(stepSize);
			updates = gStart + Math.abs(stepDone);
			dUpdates = (stepDt - stepDone) / gincr;
		}
		for (Module mod : species) {
			IBSPopulation pop = mod.getIBSPopulation();
			if (!pop.playerScoring.equals(ScoringType.EPHEMERAL))
				continue;
			// recalculate scores of entire population for display
			// these scores are just an example and are not used for
			// any calculations; use independent random number generator!
			RNGDistribution freeze = rng;
			rng = ephrng;
			pop.resetScores();
			pop.updateScores();
			rng = freeze;
		}
		return !converged;
	}

	@Override
	public boolean permitsDebugStep() {
		return true;
	}

	@Override
	public void debugStep() {
		ibsStep(0.0);
		debugFocalSpecies.debugMarkChange();
		engine.fireModelChanged();
	}

	/**
	 * Helper routine to retrieve the {@link IBSPopulation} associated with module
	 * with {@code id}.
	 * 
	 * @param id the {@code id} of the module
	 * 
	 * @return the {@code IBSPopulation}
	 */
	IBSPopulation getIBSPopulation(int id) {
		return getSpecies(id).getIBSPopulation();
	}

	@Override
	public double getMinScore(int id) {
		return getIBSPopulation(id).getMinScore();
	}

	@Override
	public double getMaxScore(int id) {
		return getIBSPopulation(id).getMaxScore();
	}

	@Override
	public double getMinFitness(int id) {
		Module mod = species.get(id);
		Map2Fitness map2fit = mod.getMap2Fitness();
		return map2fit.map(((Payoffs) mod).getMinPayoff());
	}

	@Override
	public double getMaxFitness(int id) {
		Module mod = species.get(id);
		Map2Fitness map2fit = mod.getMap2Fitness();
		return map2fit.map(((Payoffs) mod).getMaxPayoff());
	}

	@Override
	public String getStatus() {
		if (isMultispecies) {
			String status = "";
			for (Module mod : species) {
				IBSPopulation pop = mod.getIBSPopulation();
				status += (status.length() > 0 ? "<br/><i>" : "<i>") + mod.getName() + ":</i> "
						+ pop.getStatus();
			}
			return status;
		}
		return population.getStatus();
	}

	@Override
	public String getCounter() {
		String counter = super.getCounter();
		if (mode == Mode.DYNAMICS && Double.isFinite(realtime))
			return counter + " (" + Formatter.format(realtime, 2) + ")";
		return counter;
	}

	/**
	 * Gets the elapsed time in real time units. The real time increments of
	 * microscopic updates depends on the fitness of the population. In populations
	 * with high fitness many events happen per unit time and hence the increments
	 * are smaller. In contrast in populations with low fitness fewer events happen
	 * and consequently more time elapses between subsequent events. By default no
	 * distinction between real time and generation time is made.
	 * 
	 * @return elapsed real time
	 */
	public double getRealtime() {
		return realtime;
	}

	@Override
	public int getNMean() {
		if (isMultispecies) {
			int nMean = 0;
			for (Module mod : species)
				nMean += mod.getIBSPopulation().getNMean();
			return nMean;
		}
		return population.getNMean();
	}

	@Override
	public int getNMean(int id) {
		return getIBSPopulation(id).getNMean();
	}

	@Override
	public boolean getMeanTraits(double[] mean) {
		if (isMultispecies) {
			int skip = 0;
			double[] tmp = new double[mean.length];
			for (Module mod : species) {
				IBSPopulation pop = mod.getIBSPopulation();
				pop.getMeanTraits(tmp);
				System.arraycopy(tmp, 0, mean, skip, pop.nTraits);
				skip += pop.nTraits;
			}
		} else
			population.getMeanTraits(mean);
		return connect;
	}

	@Override
	public boolean getMeanTraits(int id, double[] mean) {
		getIBSPopulation(id).getMeanTraits(mean);
		return connect;
	}

	@Override
	public String getTraitNameAt(int id, int idx) {
		return getIBSPopulation(id).getTraitNameAt(idx);
	}

	@Override
	public <T> void getTraitData(int id, T[] colors, ColorMap<T> colorMap) {
		getIBSPopulation(id).getTraitData(colors, colorMap);
	}

	@Override
	public boolean getMeanFitness(double[] mean) {
		if (isMultispecies) {
			int skip = 0;
			double[] tmp = new double[mean.length];
			for (Module mod : species) {
				IBSPopulation pop = mod.getIBSPopulation();
				pop.getMeanFitness(tmp);
				int nt = mod.getNTraits();
				System.arraycopy(tmp, 0, mean, skip, nt);
				skip += nt;
			}
		} else
			population.getMeanFitness(mean);
		return connect;
	}

	@Override
	public boolean getMeanFitness(int id, double[] mean) {
		getIBSPopulation(id).getMeanFitness(mean);
		return connect;
	}

	@Override
	public String getFitnessNameAt(int id, int idx) {
		return getIBSPopulation(id).getFitnessNameAt(idx);
	}

	@Override
	public String getScoreNameAt(int id, int idx) {
		return getIBSPopulation(id).getScoreNameAt(idx);
	}

	@Override
	public <T> void getFitnessData(int id, T[] colors, ColorMap.Gradient1D<T> colorMap) {
		getIBSPopulation(id).getFitnessData(colors, colorMap);
	}

	@Override
	public void getFitnessHistogramData(int id, double[][] bins) {
		getIBSPopulation(id).getFitnessHistogramData(bins);
	}

	/**
	 * Gets formatted tag of individual at location <code>idx</code> for species
	 * with ID <code>id</code>. The formatting may include HTML tags. Used by GUI
	 * for example to show tags in tooltips. Opportunity to track ancestry through
	 * tags.
	 * <p>
	 * <strong>Note:</strong> optional implementation. Currently makes sense only
	 * for IBS models.
	 *
	 * @param id  the species identifier
	 * @param idx the index of the location
	 * @return the formatted tag
	 */
	public String getTagNameAt(int id, int idx) {
		return getIBSPopulation(id).getTagNameAt(idx);
	}

	/**
	 * Gets the number of interactions at location <code>idx</code> for species with
	 * ID <code>id</code>. Used by GUI for example to show interaction counts in
	 * tooltips.
	 * <p>
	 * <strong>Note:</strong> optional implementation. Currently makes sense only
	 * for IBS models.
	 *
	 * @param id  the species identifier
	 * @param idx the index of the location
	 * @return the interaction count
	 */
	public int getInteractionsAt(int id, int idx) {
		return getIBSPopulation(id).getInteractionsAt(idx);
	}

	/**
	 * Used by GUI to interact with Model. Called whenever a mouse click/tap was
	 * registered by a node.
	 * 
	 * @param id  the species identifier
	 * @param hit the index of the node hit by mouse
	 * @param alt <code>true</code> if {@code Alt}-key pressed
	 * @return <code>false</code> if no actions taken
	 */
	public boolean mouseHitNode(int id, int hit, boolean alt) {
		IBSPopulation pop = getIBSPopulation(id);
		if (!pop.mouseHitNode(hit, alt)) {
			// nothing changed
			return false;
		}
		// update converged
		converged = true;
		for (Module mod : species) {
			pop = mod.getIBSPopulation();
			converged &= pop.checkConvergence();
		}
		return true;
	}

	/**
	 * Type of species update (multi-species models only).
	 */
	public SpeciesUpdate speciesUpdate;

	/**
	 * Get species update type.
	 * 
	 * @return the species update type
	 */
	public SpeciesUpdate getSpeciesUpdate() {
		return speciesUpdate;
	}

	/**
	 * Pick focal population according to the selected scheme.
	 * 
	 * @return the focal population
	 * 
	 * @see SpeciesUpdate.Type
	 */
	public IBSPopulation pickFocalSpecies() {
		if (!isMultispecies)
			return population;
		double[] rates = new double[nSpecies];
		int idx = 0;
		double total = 0.0;
		switch (speciesUpdate.getType()) {
			case FITNESS:
				for (Module mod : species) {
					IBSPopulation pop = mod.getIBSPopulation();
					double rate = pop.getTotalFitness();
					rates[idx++] = rate;
					total += rate;
				}
				return pickFocalSpecies(rates, total);
			case SIZE:
				for (Module mod : species) {
					IBSPopulation pop = mod.getIBSPopulation();
					double rate = pop.getPopulationSize();
					rates[idx++] = rate;
					total += rate;
				}
				return pickFocalSpecies(rates, total);
			case RATE:
				for (Module mod : species) {
					IBSPopulation pop = mod.getIBSPopulation();
					double rate = pop.getSpeciesUpdateRate();
					rates[idx++] = rate;
					total += rate;
				}
				return pickFocalSpecies(rates, total);
			case TURNS:
				return pickFocalSpecies(1);
			case UNIFORM:
				return pickFocalSpecies(random0n(nSpecies));
			// case SYNC:
			default:
				throw new Error("unknown species update type!");
		}
	}

	/**
	 * Pick focal species with a probability proportional to the entries in {@code rates}.
	 * 
	 * @param rates the rates with which to pick the focal species
	 * @param total	the sum of the rates
	 * @return the focal population or <code>null</code> if all populations extinct
	 */
	private IBSPopulation pickFocalSpecies(double[] rates, double total) {
		if (!isMultispecies)
			return population;
		double pick = random01() * total;
		for (int i = 0; i < nSpecies; i++) {
			if (pick < rates[i])
				// found focal species
				return species.get(i).getIBSPopulation();
			pick -= rates[i];
		}
		return null;
	}

	/**
	 * Index for turn-based-selection to determine which species to pick next.
	 * Simply cycles through species array.
	 */
	private int nextSpeciesIdx = 0;

	/**
	 * Pick next focal species. For a previous focal species with index {@code idx}
	 * the next focal species is {@code (idx + skip) % nSpecies}. Extinct
	 * populations are skipped.
	 * 
	 * @param skip the number of species to skip
	 * @return the focal population or <code>null</code> all populations extinct
	 */
	private IBSPopulation pickFocalSpecies(int skip) {
		if (!isMultispecies)
			return population;
		int speciesIdx = nextSpeciesIdx + skip;
		for (int i = 0; i < nSpecies; i++) {
			nextSpeciesIdx = (speciesIdx + i) % nSpecies;
			IBSPopulation pop = species.get(nextSpeciesIdx).getIBSPopulation();
			if (pop.getPopulationSize() > 0)
				return pop;
		}
		return null;
	}

	/**
	 * Pick type of next event in focal population.
	 * 
	 * @param pop the focal population
	 * @return the next event
	 */
	protected Event pickEvent(IBSPopulation pop) {
		Mutation mu = pop.module.getMutation();
		if (mu.temperature || mu.probability <= 0.0 || random01() > mu.probability)
			return Event.REPLICATION;
		return Event.MUTATION;
	}

	/**
	 * Enumeration of possible events in focal population.
	 * 
	 * @see #pickEvent(IBSPopulation)
	 */
	public enum Event {

		/**
		 * Mutation event.
		 */
		MUTATION,

		/**
		 * Replication event.
		 */
		REPLICATION
	}

	/**
	 * Command line option to set whether player scores from interactions are
	 * accumulated or averaged (default).
	 * <p>
	 * <strong>Note:</strong> Accumulated scores can be tricky because they are
	 * essentially unbounded... On regular structures the two variants merely amount
	 * to a rescaling of the selection strength.
	 * 
	 * @see Map2Fitness#setSelection(double)
	 */
	public final CLOption cloAccumulatedScores = new CLOption("accuscores", "noaccu", CLOption.Argument.NONE,
			Category.Model,
			"--accuscores    accumulate scores (instead of averaging)",
			new CLODelegate() {

				/**
				 * {@inheritDoc}
				 * <p>
				 * Parse scoring method for players in a single or multiple populations/species.
				 * {@code arg} is ignored. If commandline option is present the players in
				 * <em>all</em> populations/species use accumulated scores.
				 * 
				 * @param arg ignored
				 */
				@Override
				public boolean parse(String arg) {
					// default is to average scores
					for (Module mod : species) {
						IBSPopulation pop = mod.getIBSPopulation();
						pop.setPlayerScoreAveraged(!cloAccumulatedScores.isSet());
					}
					return true;
				}
			});

	/**
	 * Command line option to set method for resetting the scores of individuals.
	 * 
	 * @see ScoringType
	 */
	public final CLOption cloScoringType = new CLOption("resetscores", ScoringType.RESET_ALWAYS.getKey(),
			CLOption.Argument.REQUIRED, Category.Model,
			"--resetscores <t>  type for resetting scores t:",
			new CLODelegate() {

				/**
				 * {@inheritDoc}
				 * <p>
				 * Parse method for resetting the scores of individuals. <code>arg</code> can be
				 * a single value or an array of values. The parser cycles through
				 * <code>arg</code> until all populations/species have the scoring type set.
				 * 
				 * @param arg the method for resetting the scores.
				 */
				@Override
				public boolean parse(String arg) {
					String[] playerresets = arg.contains(CLOParser.SPECIES_DELIMITER) ? 
						arg.split(CLOParser.SPECIES_DELIMITER) : arg.split(CLOParser.VECTOR_DELIMITER);
					int n = 0;
					for (Module mod : species) {
						IBSPopulation pop = mod.getIBSPopulation();
						String rest = playerresets[n++ % playerresets.length];
						ScoringType st = (ScoringType) cloScoringType.match(rest);
						if (st == null)
							return false;
						pop.setPlayerScoring(st);
					}
					return true;
				}
			});

	/**
	 * Command line option to set whether players interact with all their neighbours
	 * or a random subsample.
	 */
	public final CLOption cloInteractions = new CLOption("interactions", IBSGroup.SamplingType.ALL.getKey(),
			Category.Model,
			"--interactions <t [n]> select interaction type t:",
			new CLODelegate() {

				/**
				 * {@inheritDoc}
				 * <p>
				 * Parse interactions of players with their neighbours in a single or multiple
				 * populations/species. <code>arg</code> can be a single value or an array of
				 * values. The parser cycles through <code>arg</code> until all
				 * populations/species have the player interactions set.
				 * 
				 * @param arg the (array of) interaction(s)
				 */
				@Override
				public boolean parse(String arg) {
					String[] interactiontypes = arg.contains(CLOParser.SPECIES_DELIMITER) ? 
						arg.split(CLOParser.SPECIES_DELIMITER) : arg.split(CLOParser.VECTOR_DELIMITER);
					int n = 0;
					for (Module mod : species) {
						IBSPopulation pop = mod.getIBSPopulation();
						String intertype = interactiontypes[n++ % interactiontypes.length];
						IBSGroup.SamplingType intt = (IBSGroup.SamplingType) cloInteractions.match(intertype);
						IBSGroup group = pop.getInterGroup();
						if (intt == null)
							return false;
						group.setSampling(intt);
						// parse n, if present
						String[] args = intertype.split("\\s+|=");
						int nInter = 1;
						if (args.length > 1)
							nInter = CLOParser.parseInteger(args[1]);
						group.setNSamples(nInter);
					}
					return true;
				}
			});

	/**
	 * Command line option to set the method for choosing references/models among
	 * the neighbours of a player for updating their trait.
	 */
	public final CLOption cloReferences = new CLOption("references", IBSGroup.SamplingType.RANDOM.getKey() + " 1",
			Category.Model,
			"--references <t [n]> select reference type t:",
			new CLODelegate() {

				/**
				 * {@inheritDoc}
				 * <p>
				 * Parse method for choosing references/models among the neighbours of a player
				 * for updating their trait in a single or multiple populations/species.
				 * <code>arg</code> can be a single value or an array of values. The parser
				 * cycles through <code>arg</code> until all populations/species have the
				 * player's references set.
				 * 
				 * @param arg the (array of) reference type(s)
				 */
				@Override
				public boolean parse(String arg) {
					String[] referencetypes = arg.contains(CLOParser.SPECIES_DELIMITER) ? 
						arg.split(CLOParser.SPECIES_DELIMITER) : arg.split(CLOParser.VECTOR_DELIMITER);
					int n = 0;
					for (Module mod : species) {
						IBSPopulation pop = mod.getIBSPopulation();
						String reftype = referencetypes[n++ % referencetypes.length];
						IBSGroup.SamplingType reft = (IBSGroup.SamplingType) cloReferences.match(reftype);
						IBSGroup group = pop.getCompGroup();
						if (reft == null)
							return false; 
						group.setSampling(reft);
						// parse n, if present
						String[] args = reftype.split("\\s+|=");
						int nInter = 1;
						if (args.length > 1)
							nInter = CLOParser.parseInteger(args[1]);
						group.setNSamples(nInter);
					}
					return true;
				}
			});

	/**
	 * Command line option to set the migration types and probabilities of players.
	 */
	public final CLOption cloMigration = new CLOption("migration", "none", Category.Model,
			"--migration <tp>  migration (t type, p probability)",
			new CLODelegate() {

				/**
				 * {@inheritDoc}
				 * <p>
				 * Parse migration types and probabilities of players in a single or multiple
				 * populations/species. <code>arg</code> can be a single value or an array of
				 * values. The parser cycles through <code>arg</code> until all
				 * populations/species have the migration types and probabilities of players
				 * set.
				 * 
				 * @param arg the (array of) migration type(s) and probability(ies)
				 */
				@Override
				public boolean parse(String arg) {
					String[] migrationtypes = arg.contains(CLOParser.SPECIES_DELIMITER) ? 
						arg.split(CLOParser.SPECIES_DELIMITER) : arg.split(CLOParser.VECTOR_DELIMITER);
					int n = 0;
					for (Module mod : species) {
						IBSPopulation pop = mod.getIBSPopulation();
						String migt = migrationtypes[n++ % migrationtypes.length];
						MigrationType mt = (MigrationType) cloMigration.match(migt);
						if (mt == null)
							return false;
						pop.setMigrationType(mt);
						if (pop.getMigrationType() == MigrationType.NONE) {
							pop.setMigrationProb(0.0);
							continue;
						}
						String keyarg = CLOption.stripKey(mt, arg);
						if (keyarg.length() > 0) {
							pop.setMigrationProb(CLOParser.parseDouble(keyarg));
							double mig = pop.getMigrationProb();
							if (mig < 1e-8) {
								logger.warning((isMultispecies ? mod.getName() + ": " : "")
										+ "migration rate too small (" + Formatter.formatSci(mig, 4)
										+ ") - reverting to no migration");
								pop.setMigrationType(MigrationType.NONE);
								pop.setMigrationProb(0.0);
							}
							continue;
						}
						return false;
					}
					return true;
				}
			});

	/**
	 * Command line option to set the interaction geometry. This overrides the
	 * {@link Module#cloGeometry} settings.
	 * 
	 * @see Geometry.Type
	 * @see #cloGeometryCompetition
	 * @see Module#cloGeometry
	 */
	public final CLOption cloGeometryInteraction = new CLOption("geominter", "M", Category.Model,
			"--geominter <>  interaction geometry (see --geometry)",
			new CLODelegate() {

				/**
				 * {@inheritDoc}
				 * <p>
				 * Parse interaction geometry in a single or multiple populations/species.
				 * <code>arg</code> can be a single value or an array of values with the
				 * separator {@value CLOParser#SPECIES_DELIMITER}. The parser cycles through
				 * <code>arg</code> until all populations/species have the interaction geometry
				 * set.
				 * 
				 * @param arg the (array of) interaction geometry(ies)
				 */
				@Override
				public boolean parse(String arg) {
					// only act if option has been explicitly specified - otherwise cloGeometry will
					// take care of this
					if (!cloGeometryInteraction.isSet())
						return true;
					String[] geomargs = arg.split(CLOParser.SPECIES_DELIMITER);
					boolean doReset = false;
					int n = 0;
					for (Module mod : species) {
						IBSPopulation pop = mod.getIBSPopulation();
						// creates new interaction geometry if null or equal to getGeometry()
						Geometry geom = pop.createInteractionGeometry();
						doReset |= geom.parse(geomargs[n++ % geomargs.length]);
					}
					engine.requiresReset(doReset);
					return true;
				}
			});

	/**
	 * Command line option to set the competition geometry. This overrides the
	 * {@link Module#cloGeometry} settings.
	 * 
	 * @see Geometry.Type
	 * @see #cloGeometryInteraction
	 * @see Module#cloGeometry
	 */
	public final CLOption cloGeometryCompetition = new CLOption("geomcomp", "M", Category.Model,
			"--geomcomp <>   competition geometry (see --geometry)",
			new CLODelegate() {

				/**
				 * {@inheritDoc}
				 * <p>
				 * Parse competition geometry in a single or multiple populations/species.
				 * <code>arg</code> can be a single value or an array of values with the
				 * separator {@value CLOParser#SPECIES_DELIMITER}. The parser cycles through
				 * <code>arg</code> until all populations/species have the competition geometry
				 * set.
				 * 
				 * @param arg the (array of) competition geometry(ies)
				 */
				@Override
				public boolean parse(String arg) {
					// only act if option has been explicitly specified - otherwise cloGeometry will
					// take care of this
					if (!cloGeometryCompetition.isSet())
						return true;
					String[] geomargs = arg.split(CLOParser.SPECIES_DELIMITER);
					boolean doReset = false;
					int n = 0;
					for (Module mod : species) {
						IBSPopulation pop = mod.getIBSPopulation();
						// creates new competition geometry if null or equal to getGeometry()
						Geometry geom = pop.createCompetitionGeometry();
						doReset |= geom.parse(geomargs[n++ % geomargs.length]);
					}
					engine.requiresReset(doReset);
					return true;
				}
			});

	/**
	 * Command line option to set the fraction of links to rewire. If graph is
	 * undirected it is preserved and if graph is directed, rewiring is done for
	 * directed links (where undirected links count as two directed links), which
	 * potentially breaks undirected ones.
	 */
	public final CLOption cloGeometryRewire = new CLOption("rewire", "0", Category.Model,
			"--rewire <i[,c]>  rewire fraction of links (interaction/competition)",
			new CLODelegate() {

				/**
				 * {@inheritDoc}
				 * <p>
				 * Parse fraction of links to rewire in geometry. {@code arg} rewiring of
				 * interaction and competition graphs can be specified by two values separated
				 * by {@value CLOParser#CLOParser#VECTOR_DELIMITER}. {@code arg} can be a single
				 * value or an array of values with the separator
				 * {@value CLOParser#SPECIES_DELIMITER}. The parser cycles through {@code arg}
				 * until all populations/species have the rewiring set.
				 * 
				 * @param arg the (array of) rewiring fraction(s)
				 */
				@Override
				public boolean parse(String arg) {
					String[] rewireargs = arg.split(CLOParser.SPECIES_DELIMITER);
					int n = 0;
					for (Module mod : species) {
						IBSPopulation pop = mod.getIBSPopulation();
						double[] rewire = CLOParser.parseVector(rewireargs[n++ % rewireargs.length]);
						pop.setRewire(rewire);
					}
					return true;
				}
			});

	/**
	 * Command line option to set the fraction of links to add. If graph is
	 * undirected only undirected links are added and if graph is directed only
	 * directed links are added.
	 */
	public final CLOption cloGeometryAddwire = new CLOption("addwire", "0", Category.Model,
			"--addwire <i[,c]>  add fraction of links (interaction/competition)",
			new CLODelegate() {

				/**
				 * {@inheritDoc}
				 * <p>
				 * Parse fraction of links to add in geometry. {@code arg} rewiring of
				 * interaction and competition graphs can be specified by two values separated
				 * by {@value CLOParser#VECTOR_DELIMITER}. {@code arg} can be a single
				 * value or an array of values with the separator
				 * {@value CLOParser#SPECIES_DELIMITER}. The parser cycles through {@code arg}
				 * until all populations/species have the rewiring set.
				 * 
				 * @param arg the (array of) rewiring fraction(s)
				 */
				@Override
				public boolean parse(String arg) {
					String[] addargs = arg.split(CLOParser.SPECIES_DELIMITER);
					int n = 0;
					for (Module mod : species) {
						IBSPopulation pop = mod.getIBSPopulation();
						double[] add = CLOParser.parseVector(addargs[n++ % addargs.length]);
						pop.setAddwire(add);
					}
					return true;
				}
			});

	/**
	 * Command line option to enable consistency checks.
	 */
	public final CLOption cloConsistency = new CLOption("consistency", "noconsistency", CLOption.Argument.NONE,
			Category.Model,
			"--consistency   check consistency of scores etc.", new CLODelegate() {

				/**
				 * {@inheritDoc}
				 * <p>
				 * Parse method to enable consistency checks.
				 * 
				 * @param arg the ignored argument
				 */
				@Override
				public boolean parse(String arg) {
					for (Module mod : species) {
						IBSPopulation pop = mod.getIBSPopulation();
						pop.setConsistencyCheck(cloConsistency.isSet());
					}
					return true;
				}
			});

	@Override
	public void collectCLO(CLOParser parser) {
		super.collectCLO(parser);
		cloMigration.addKeys(MigrationType.values());
		parser.addCLO(cloMigration);
		parser.addCLO(cloGeometryRewire);
		parser.addCLO(cloGeometryAddwire);
		parser.addCLO(cloConsistency);
		statisticsSettings.clo.clearKeys();
		statisticsSettings.clo.addKeys(Statistics.Type.values());
		parser.addCLO(statisticsSettings.clo);

		boolean anyVacant = false;
		boolean anyNonVacant = false;
		boolean allStatic = true;
		boolean anyPayoffs = false;
		boolean allPayoffs = true;
		for (Module mod : species) {
			int vacant = mod.getVacant();
			anyVacant |= vacant >= 0;
			anyNonVacant |= vacant < 0;
			allStatic &= mod.isStatic();
			boolean hasPayoffs = (mod instanceof Payoffs);
			anyPayoffs |= hasPayoffs;
			allPayoffs &= hasPayoffs;
		}
		if (species.size() > 1) {
			speciesUpdate.clo.addKeys(SpeciesUpdate.Type.values());
			if (!allPayoffs)
				speciesUpdate.clo.removeKey(SpeciesUpdate.Type.FITNESS);
			parser.addCLO(speciesUpdate.clo);
		}
		if (anyNonVacant) {
			// additional options that only make sense without vacant sites
			parser.addCLO(cloReferences);
			cloReferences.clearKeys();
			cloReferences.addKeys(IBSGroup.SamplingType.values());
		}
		IBSPopulation ibs = species.get(0).getIBSPopulation();
		PopulationUpdate pup = ibs.getPopulationUpdate();
		// ToDo: further updates to implement or make standard
		pup.clo.clearKeys();
		if (anyVacant) {
			// restrict population updates to those compatible with ecological models
			pup.clo.addKey(PopulationUpdate.Type.ECOLOGY);
			pup.clo.setDefault(PopulationUpdate.Type.ECOLOGY.getKey());
		} else {
			pup.clo.addKeys(PopulationUpdate.Type.values());
			pup.clo.removeKey(PopulationUpdate.Type.ECOLOGY);
		}
		parser.addCLO(pup.clo);
		if (anyPayoffs && !allStatic) {
			// options that are only meaningful if at least some populations have
			// (non-static) fitness
			parser.addCLO(cloAccumulatedScores);
			parser.addCLO(cloScoringType);
			cloScoringType.clearKeys();
			cloScoringType.addKeys(ScoringType.values());
			parser.addCLO(cloInteractions);
			cloInteractions.clearKeys();
			cloInteractions.addKeys(IBSGroup.SamplingType.values());
			parser.addCLO(cloGeometryInteraction);
			parser.addCLO(cloGeometryCompetition);
		}
		if (!anyPayoffs) {
			// options that do not make sense for contact processes
			// remove Moran updating
			CLOption opt = pup.clo;
			opt.removeKey(PopulationUpdate.Type.MORAN_BIRTHDEATH);
			opt.removeKey(PopulationUpdate.Type.MORAN_DEATHBIRTH);
			opt.removeKey(PopulationUpdate.Type.MORAN_IMITATE);
		}
	}

	/**
	 * Schedules for resetting individual payoffs/fitness:
	 * <dl>
	 * <dt>onchange</dt>
	 * <dd>Reset when changing trait (only after updating from reference model with
	 * a different trait)</dd>
	 * <dt>onupdate</dt>
	 * <dd>Reset when updating from reference individual (not necessarily a change
	 * in trait)</dd>
	 * <dt>ephemeral</dt>
	 * <dd>Determine payoffs/fitness calculated only for updating</dd>
	 * </dl>
	 */
	public static enum ScoringType implements CLOption.Key {

		/**
		 * Reset when <em>changing</em> trait (only after updating from reference model
		 * with a different trait).
		 */
		RESET_ON_CHANGE("onchange", "when changing trait"),

		/**
		 * Reset when <em>updating</em> from reference individual (not necessarily a
		 * change in trait).
		 */
		RESET_ALWAYS("onupdate", "when updating trait"),

		/**
		 * Determine payoffs/fitness calculated only for updating.
		 */
		EPHEMERAL("ephemeral", "payoffs for updating only"),

		/**
		 * Individuals do not have payoffs/fitness.
		 */
		NONE("none", "no payoffs");

		/**
		 * Key of population update type. Used for parsing command line options.
		 * 
		 * @see org.evoludo.simulator.models.IBS#cloScoringType
		 */
		String key;

		/**
		 * Brief description of population update type for GUI and help display.
		 * 
		 * @see EvoLudo#getCLOHelp()
		 */
		String title;

		/**
		 * Instantiate new type of population update.
		 * 
		 * @param key   identifier for parsing of command line option
		 * @param title the summary of geometry for GUI and help display
		 */
		ScoringType(String key, String title) {
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

	/**
	 * Types of species updates (only relevant for multi-species models):
	 * <dl>
	 * <dt>none</dt>
	 * <dd>no migration</dd>
	 * <dt>D</dt>
	 * <dd>diffusive migration (exchange of neighbors)</dd>
	 * <dt>B</dt>
	 * <dd>birth-death migration (fit migrates, random death).</dd>
	 * <dt>d</dt>
	 * <dd>death-birth migration (random death, fit migrates).</dd>
	 * </dl>
	 */
	public static enum MigrationType implements CLOption.Key {

		/**
		 * No migration.
		 */
		NONE("none", "no migration"),

		/**
		 * Diffusive migration (exchange of neighbors).
		 */
		DIFFUSION("D", "diffusive migration, exchange of neighbors"),

		/**
		 * Birth-death migration (fit migrates, random death).
		 */
		BIRTH_DEATH("B", "birth-death migration, fit migrates, random dies"),

		/**
		 * Death-birth migration (random death, fit migrates).
		 */
		DEATH_BIRTH("d", "death-birth migration, random dies, fit migrates");

		/**
		 * Key of migration type. Used for parsing command line options.
		 * 
		 * @see org.evoludo.simulator.models.IBS#cloMigration
		 */
		String key;

		/**
		 * Brief description of migration type for GUI and help display.
		 * 
		 * @see EvoLudo#getCLOHelp()
		 */
		String title;

		/**
		 * Instantiate new type of migration.
		 * 
		 * @param key   identifier for parsing of command line option
		 * @param title the summary of geometry for GUI and help display
		 */
		MigrationType(String key, String title) {
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

	/**
	 * The settings for statistics mode.
	 */
	Statistics statisticsSettings;

	/**
	 * The class managing the settings for statistics mode.
	 */
	public static class Statistics {

		/**
		 * The model that is using the statistics settings. This is specific to IBS
		 * models.
		 */
		org.evoludo.simulator.models.IBS ibs;

		/**
		 * The statistics type.
		 * 
		 * @see #clo
		 */
		Statistics.Type stattype;

		/**
		 * The number of samples before resetting the geometry.
		 */
		int resetInterval = 0;

		/**
		 * The settings for the statistics.
		 */
		String[] args;

		/**
		 * Get the settings for the statistics.
		 * 
		 * @return the statistics settings
		 * 
		 * @see #args
		 */
		public String[] getArgs() {
			return args;
		}

		@Override
		public String toString() {
			return stattype.getKey() + " " + Formatter.format(args, ";");
		}

		/**
		 * Instantiate new statistics settings for use in IBS {@code model}s.
		 * 
		 * @param ibs the model using this statistics settings
		 */
		public Statistics(org.evoludo.simulator.models.IBS ibs) {
			this.ibs = ibs;
			stattype = Statistics.Type.RESET_GEOMETRY;
		}

		/**
		 * Command line option to customize statistics settings.
		 */
		public final CLOption clo = new CLOption("statistics", "reset 1", Category.Simulation,
				null, new CLODelegate() {
					@Override
					public boolean parse(String arg) {
						// multiple settings are separated by Formatter.MATRIX_DELIMITER
						args = arg.split(Formatter.MATRIX_DELIMITER);
						// currently at most a single setting
						for (String st : args) {
							stattype = (Statistics.Type) clo.match(st.trim(), 2);
							if (stattype == null)
								return false;
							String[] typeargs = st.split("\\s+|=");
							resetInterval = typeargs.length > 1 ? CLOParser.parseInteger(typeargs[1]) : 1;
						}
						return true;
					}

					@Override
					public String getDescription() {
						String descr = "--statistics <s>  settings:\n" + clo.getDescriptionKey();
						return descr;
					}
				});

		/**
		 * Type of statistics.
		 * <dl>
		 * <dt>RESET_GEOMETRY
		 * <dd>Reset geometry every {@code s} samples (never for {@code s&le;0}).
		 * </dl>
		 */
		public static enum Type implements CLOption.Key {

			/**
			 * Skip homogeneous states by introducing a mutant and advancing the time
			 * according to an exponential distribution for an event happening with
			 * probability {@code Module#getMutationProb()}.
			 */
			RESET_GEOMETRY("reset <s>", "reset geometry every <s> samples (0 never)");

			/**
			 * Key of statistics type. Used when parsing command line options.
			 */
			String key;

			/**
			 * Brief description of statistics type for help display.
			 * 
			 * @see EvoLudo#getCLOHelp()
			 */
			String title;

			/**
			 * Instantiate new statistics type.
			 * 
			 * @param key   identifier for parsing of command line option
			 * @param title summary of statistics
			 */
			Type(String key, String title) {
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
	}

	@Override
	public void encodeState(StringBuilder plist) {
		plist.append(Plist.encodeKey("Generation", getUpdates()));
		plist.append(Plist.encodeKey("Realtime", getRealtime()));
		plist.append(Plist.encodeKey("Model", type.toString()));
		boolean isMultiSpecies = (species.size() > 1);
		for (Module mod : species) {
			IBSPopulation pop = mod.getIBSPopulation();
			if (isMultiSpecies)
				plist.append("<key>" + mod.getName() + "</key>\n" + "<dict>\n");
			pop.encodeGeometry(plist);
			pop.encodeTraits(plist);
			pop.encodeFitness(plist);
			pop.encodeInteractions(plist);
			if (isMultiSpecies)
				plist.append("</dict>");
		}
	}

	@Override
	public boolean restoreState(Plist plist) {
		updates = (Double) plist.get("Generation");
		realtime = (Double) plist.get("Realtime");
		connect = false;
		boolean success = true;
		if (species.size() > 1) {
			for (Module mod : species) {
				IBSPopulation pop = mod.getIBSPopulation();
				String name = mod.getName();
				Plist pplist = (Plist) plist.get(name);
				if (!pop.restoreGeometry(pplist)) {
					logger.warning("restore geometry failed (" + name + ").");
					success = false;
				}
				if (!pop.restoreInteractions(pplist)) {
					logger.warning("restore interactions in " + type + "-model failed (" + name + ").");
					success = false;
				}
				if (!pop.restoreTraits(pplist)) {
					logger.warning("restore traits in " + type + "-model failed (" + name + ").");
					success = false;
				}
				if (!pop.restoreFitness(pplist)) {
					logger.warning("restore fitness in " + type + "-model failed (" + name + ").");
					success = false;
				}
			}
			return success;
		}
		if (!population.restoreGeometry(plist)) {
			logger.warning("restore geometry failed.");
			success = false;
		}
		if (!population.restoreInteractions(plist)) {
			logger.warning("restore interactions in " + type + "-model failed.");
			success = false;
		}
		if (!population.restoreTraits(plist)) {
			logger.warning("restore traits in " + type + "-model failed.");
			success = false;
		}
		if (!population.restoreFitness(plist)) {
			logger.warning("restore fitness in " + type + "-model failed.");
			success = false;
		}
		return success;
	}

	/**
	 * random integer number from interval <code>[0, n)</code>.
	 *
	 * @param n the upper limit of interval (exclusive)
	 * @return the random integer number in <code>[0, n)</code>.
	 */
	public int random0n(int n) {
		return rng.random0n(n);
	}

	/**
	 * Random number from interval [0, 1) with 32bit resolution. This is the
	 * default.
	 *
	 * @return the random number in <code>[0, 1)</code>.
	 */
	public double random01() {
		return rng.random01();
	}
}
