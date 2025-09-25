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

import java.awt.Color;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.evoludo.math.ArrayMath;
import org.evoludo.math.Combinatorics;
import org.evoludo.math.Functions;
import org.evoludo.math.RNGDistribution;
import org.evoludo.simulator.ColorMap;
import org.evoludo.simulator.EvoLudo;
import org.evoludo.simulator.Geometry;
import org.evoludo.simulator.models.IBS.MigrationType;
import org.evoludo.simulator.models.IBS.ScoringType;
import org.evoludo.simulator.models.IBSGroup.SamplingType;
import org.evoludo.simulator.modules.Map2Fitness;
import org.evoludo.simulator.modules.Module;
import org.evoludo.simulator.modules.PlayerUpdate;
import org.evoludo.simulator.modules.Features;
import org.evoludo.simulator.modules.Features.Payoffs;
import org.evoludo.util.Formatter;
import org.evoludo.util.Plist;

/**
 * The core class for individual based simulations. Manages the population,
 * including keeping track of the payoffs and fitness of individuals.
 * <p>
 * <strong>Note:</strong> traits cannot be handled here because they are
 * represented by {@code int}s in discrete models but {@code double}s in
 * continuous models.
 * 
 * @author Christoph Hauert
 * 
 * @see IBSDPopulation
 * @see IBSMCPopulation
 * @see IBSCPopulation
 */
public abstract class IBSPopulation {

	/**
	 * The pacemaker of all models. Interface with the outside world.
	 */
	protected EvoLudo engine;

	/**
	 * The module associated with this population.
	 */
	protected Module module;

	/**
	 * Convenience field for static modules to avoid casts.
	 */
	protected Features.Static staticmodule;

	/**
	 * Gets the module associated with this population.
	 * 
	 * @return the module associated with this population
	 */
	public Module getModule() {
		return module;
	}

	/**
	 * The interaction partner/opponent of this population
	 * {@code opponent.getModule()==getModule().getOpponent()}. In intra-species
	 * interactions {@code opponent==this}. Convenience field.
	 */
	protected IBSPopulation opponent;

	/**
	 * Logger for keeping track of and reporting events and issues.
	 */
	protected Logger logger;

	/**
	 * The shared random number generator to ensure reproducibility of results.
	 * 
	 * @see EvoLudo#getRNG()
	 */
	protected RNGDistribution rng;

	/**
	 * Flag to indicate whether the model entertains multiple species, i.e.
	 * {@code nSpecies&gt;1}. Convenience field. Reduces calls to {@link Module}.
	 */
	protected boolean isMultispecies = false;

	/**
	 * The population update.
	 */
	PopulationUpdate populationUpdate;

	/**
	 * Creates a population of individuals for IBS simulations.
	 * 
	 * @param engine the pacemaker for running the model
	 * @param module the module that defines the game
	 */
	protected IBSPopulation(EvoLudo engine, Module module) {
		this.engine = engine;
		logger = engine.getLogger();
		opponent = this;
		this.module = module;
		// initialize helper variables
		nTraits = module.getNTraits();
		VACANT = module.getVacant();

		// get shared random number generator
		rng = engine.getRNG();
		isMultispecies = (module.getNSpecies() > 1);

		interGroup = new IBSGroup(rng);
		compGroup = new IBSGroup(rng);
		populationUpdate = new PopulationUpdate((IBS) engine.getModel());
	}

	/**
	 * Set the interaction partner/opponent of this population.
	 * 
	 * @param opponent the interaction partner/opponent
	 */
	public void setOpponentPop(IBSPopulation opponent) {
		this.opponent = opponent;
	}

	/**
	 * Gets the status of the as a formatted string. This is typically used in the
	 * GUI to summarize the progress of the model.
	 * 
	 * @return the status of the population
	 */
	public abstract String getStatus();

	/**
	 * Prior to a synchronous update step the current state must be duplicated in
	 * preparation for processing the next step.
	 * 
	 * @see #commitTraits()
	 */
	public abstract void prepareTraits();

	/**
	 * After a synchronous update step the new state must be copied back to become
	 * the current state.
	 * 
	 * @see #prepareTraits()
	 */
	public abstract void commitTraits();

	/**
	 * The change of a trait of the player at {@code index} is stored in a
	 * temporary variable and must be committed before proceeding.
	 * 
	 * @param index the index of the player that needs to have its new trait
	 *              committed
	 */
	public abstract void commitTraitAt(int index);

	/**
	 * Check if individuals with index <code>a</code> and index <code>b</code> have
	 * the same traits.
	 *
	 * @param a the index of first individual
	 * @param b the index of second individual
	 * @return <code>true</code> if the two individuals have the same traits
	 */
	public abstract boolean haveSameTrait(int a, int b);

	/**
	 * Check if individual with index <code>a</code> has switched traits.
	 * <p>
	 * <strong>Note:</strong> this test is only meaningful before trait are
	 * committed.
	 * 
	 * @param a index of individual
	 * @return <code>true</code> if trait remained unchanged
	 * 
	 * @see #commitTraitAt(int)
	 */
	public abstract boolean isSameTrait(int a);

	/**
	 * Swap traits of individuals with index <code>a</code> and index
	 * <code>b</code>.
	 * <p>
	 * <strong>Note:</strong> the traits still need to be committed.
	 *
	 * @param a the index of first individual
	 * @param b the index of second individual
	 * 
	 * @see #commitTraitAt(int)
	 */
	public abstract void swapTraits(int a, int b);

	/**
	 * Play a pairwise interaction with the individuals in {@code group}.
	 * 
	 * @param group the group of individuals interacting in pairs
	 */
	public abstract void playPairGameAt(IBSGroup group);

	/**
	 * Play a group interaction with the individuals in {@code group}.
	 * 
	 * @param group the group of interacting individuals
	 */
	public abstract void playGroupGameAt(IBSGroup group);

	/**
	 * Adjusts scores of focal individual with index <code>me</code> and its
	 * neighbors after <code>me</code> changed trait. Only works if
	 * <code>adjustScores==true</code>.
	 * <p>
	 * <strong>Important:</strong> new trait must not yet have been committed.
	 *
	 * @param me the index of the focal individual
	 */
	public abstract void adjustPairGameScoresAt(int me);

	/**
	 * Counterpart of {@link #playGroupGameAt(IBSGroup)}, {@link #playGameAt(int)}
	 * and/or {@link #playGameSyncAt(int)}. Removes the payoffs of group
	 * interactions.
	 *
	 * @param group the interaction group
	 */
	public abstract void yalpGroupGameAt(IBSGroup group);

	/**
	 * Adjust score of individual with index {@code index} from {@code before} to
	 * {@code after} and update all applicable helper variables, e.g.
	 * {@code sumFitness}.
	 * <p>
	 * <strong>Important:</strong> Use only to adjust scores of individuals that did
	 * <em>not</em> change trait.
	 * 
	 * @param index  the index of the individual
	 * @param before the score before adjustments
	 * @param after  the score after adjustments
	 */
	public abstract void adjustScoreAt(int index, double before, double after);

	/**
	 * Adjust score of individual with index {@code index} by {@code adjust} and
	 * update all applicable helper variables, e.g. {@code sumFitness}.
	 * 
	 * @param index  the index of the individual
	 * @param adjust the score adjustment
	 */
	public abstract void adjustScoreAt(int index, double adjust);

	/**
	 * Best-response update.
	 * 
	 * <h3>Important:</h3>
	 * <ol>
	 * <li>The array <code>group</code> is untouchable because it may refer to the
	 * population structure. Any change would also permanently change the structure.
	 * <li>The best-response update must be implemented in subclasses that override
	 * this method. By default throws an error.
	 * <li>Instead of overriding the method, subclasses may remove
	 * {@link PlayerUpdate.Type#BEST_RESPONSE} from
	 * {@link org.evoludo.simulator.modules.PlayerUpdate#clo PlayerUpdate#clo}.
	 * </ol>
	 * 
	 * @param me    the index of individual to update
	 * @param group the array with indices of reference group
	 * @param size  the size of the reference group
	 * @return <code>true</code> if trait changed (signaling score needs to be
	 *         reset)
	 */
	public boolean updatePlayerBestResponse(int me, int[] group, int size) {
		throw new Error("Best-response dynamics ill defined!");
	}

	/**
	 * For deterministic updating with multiple traits (more than two), it must
	 * be specified which trait is the preferred one.
	 * <p>
	 * <strong>Summary:</strong> does 'me' prefer 'sample' over 'best'?
	 *
	 * @param me     the index of the focal individual
	 * @param best   the index of the best performing individual
	 * @param sample the index of the sample type
	 * @return <code>true</code> if <code>sample</code> is preferred over
	 *         <code>best</code>
	 */
	public abstract boolean preferredPlayerBest(int me, int best, int sample);

	/**
	 * Check if site with index <code>index</code> is occupied by an individual or
	 * vacant.
	 * <p>
	 * <strong>Note:</strong> Assumes that trait <em>are</em> committed.
	 * 
	 * @param index the index of the individual/site to check
	 * @return <code>true</code> if site <code>index</code> is vacant
	 */
	public boolean isVacantAt(int index) {
		return false;
	}

	/**
	 * Check if site with index <code>index</code> will become vacant in this time
	 * step.
	 * <p>
	 * <strong>Note:</strong> Assumes that trait <em>are not</em> committed.
	 *
	 * @param index the index of the individual/site to check
	 * @return <code>true</code> if site <code>index</code> will become vacant
	 */
	public boolean becomesVacantAt(int index) {
		return false;
	}

	/**
	 * Check if population is monomorphic.
	 * <p>
	 * <strong>Note:</strong> In models that admit vacant sites this does not imply
	 * a homogeneous (or absorbing) state of the population. Without vacant sites
	 * monomorphic states are absorbing, at least in the absence of mutations.
	 * 
	 * @return {@code true} if population is monomorphic
	 * 
	 * @see org.evoludo.simulator.modules.Discrete#cloMonoStop Discrete.cloMonoStop
	 */
	public boolean isMonomorphic() {
		return false;
	}

	/**
	 * Check if population has converged. By default {@code true} if population is
	 * monomorphic and no (zero) mutations. However, different implementations may
	 * have different criteria for convergence.
	 * <p>
	 * <strong>Note:</strong> This tends to be less restrictive than reaching an
	 * absorbing state. Typically convergence is used as a criterion to abort
	 * simulations.
	 *
	 * @return {@code true} if converged.
	 */
	public abstract boolean checkConvergence();

	/**
	 * The update type of players. Convenience field.
	 * 
	 * @see Module#getPlayerUpdate()
	 */
	PlayerUpdate playerUpdate;

	/**
	 * The index of vacant sites or {@code -1} if module does not support vacancies.
	 * Convenience field.
	 * 
	 * @see Module#getVacant()
	 */
	protected int VACANT = -1;

	/**
	 * The number of traits in module. Convenience field.
	 * 
	 * @see Module#getNTraits()
	 */
	protected int nTraits = -1;

	/**
	 * Gets the formatted name of the trait of the individual at site {@code index}.
	 * 
	 * @param index the index of the
	 * @return the string describing the trait
	 */
	public abstract String getTraitNameAt(int index);

	/**
	 * The geometry of the interaction graph.
	 */
	protected Geometry interaction;

	/**
	 * Reference to the interaction group.
	 */
	protected IBSGroup interGroup;

	/**
	 * Gets the interaction group.
	 * 
	 * @return the interaction group
	 */
	public IBSGroup getInterGroup() {
		return interGroup;
	}

	/**
	 * The geometry of the competition graph.
	 */
	protected Geometry competition;

	/**
	 * Reference to the competition/reference/model group.
	 */
	protected IBSGroup compGroup;

	/**
	 * Gets the competition/reference/model group.
	 * 
	 * @return the competition/reference/model group
	 */
	public IBSGroup getCompGroup() {
		return compGroup;
	}

	/**
	 * Gets the population update.
	 * 
	 * @return the population update
	 */
	public PopulationUpdate getPopulationUpdate() {
		return populationUpdate;
	}

	/**
	 * Sets the population update.
	 * 
	 * @param populationUpdate the population update
	 */
	public void setPopulationUpdate(PopulationUpdate populationUpdate) {
		this.populationUpdate = populationUpdate;
	}

	/**
	 * Flag to indicate whether player scores are averaged (default) or accumulated.
	 * 
	 * @see IBS#cloAccumulatedScores
	 */
	protected boolean playerScoreAveraged = true;

	/**
	 * Flag to indicate whether scores are reset whenever a player adopts the
	 * trait of another (default) or only if an actual change of trait occurred.
	 * 
	 * @see IBS#cloScoringType
	 */
	protected ScoringType playerScoring = ScoringType.NONE;

	/**
	 * The type of migration.
	 */
	protected MigrationType migrationType = MigrationType.NONE;

	/**
	 * The probability of migration.
	 */
	protected double pMigration = 0.0;

	/**
	 * The distribution to determine the number of migrants.
	 */
	protected RNGDistribution.Geometric distrMigrants;

	/**
	 * Conveninece variable to store cumulative probability distributions for
	 * replicator updating.
	 * 
	 * @see #updatePlayerAt(int)
	 */
	private double[] cProbs;

	/**
	 * The array of individual fitness values.
	 * 
	 * @see #scores
	 * @see Map2Fitness
	 */
	protected double[] fitness;

	/**
	 * The absolute minimum fitness in the population. Convenience field used for
	 * fitness based picking of focal individuals or for the GUI for scaling graphs.
	 */
	protected double minFitness = Double.MAX_VALUE;

	/**
	 * The absolute maximum fitness in the population. Convenience field used for
	 * fitness based picking of focal individuals or for the GUI for scaling graphs.
	 */
	protected double maxFitness = -Double.MAX_VALUE;

	/**
	 * The total fitness of the population. Convenience field used for fitness
	 * based picking of focal individuals or for the GUI for scaling graphs.
	 */
	protected double sumFitness = -1.0;

	/**
	 * Gets the total fitness of the population.
	 * 
	 * @return the total fitness
	 */
	public double getTotalFitness() {
		return sumFitness;
	}

	/**
	 * The array of individual scores.
	 * 
	 * @see #fitness
	 * @see Map2Fitness
	 */
	protected double[] scores;

	/**
	 * The absolute minimum score in the population. Even though largely replaced by
	 * {@link #minFitness} in simulations it remains a useful and often more
	 * intuitive quantity than fitness when visualizing results in the GUI.
	 * Convenience field.
	 */
	protected double minScore = Double.MAX_VALUE;

	/**
	 * The absolute maximum score in the population. Even though largely replaced by
	 * {@link #maxFitness} in simulations it remains a useful and often more
	 * intuitive quantity than fitness when visualizing results in the GUI.
	 * Convenience field.
	 */
	protected double maxScore = -Double.MAX_VALUE;

	/**
	 * Array to hold scores of individuals during payoff calculations.
	 */
	protected double[] groupScores;

	/**
	 * Array to hold scores of individuals during payoff calculations for small
	 * groups (not all neighbours).
	 */
	protected double[] smallScores;

	/**
	 * The array of individual interaction counts.
	 */
	protected int[] interactions;

	/**
	 * The array of individual tags counts. This can be used to trace ancestry.
	 */
	protected double[] tags;

	/**
	 * Optimization: Number of interactions in well-mixed populations for update
	 * rules that take advantage of {@link IBSDPopulation#updateMixedMeanScores()}.
	 * {@code nMixedInter} is calculated ahead of time in {@link #check()}.
	 */
	protected int nMixedInter = -1;

	/**
	 * Optimization: the lookup table for scores in well-mixed populations.
	 */
	protected double[] typeScores;

	/**
	 * Optimization: the lookup table for fitness in well-mixed populations.
	 */
	protected double[] typeFitness;

	/**
	 * The size of the population. Convenience field to reduce calls to module.
	 */
	protected int nPopulation = 1000;

	/**
	 * The map converting scores to fitness and vice versa. Convenience field to
	 * reduce calls to module.
	 */
	protected Map2Fitness map2fit;

	/**
	 * Optimization: Flag to indicate whether adjusting instead of recalculating
	 * scores is possible.
	 * 
	 * <h3>Notes:</h3>
	 * <ol>
	 * <li>Adjusting scores is only feasible if all individuals have a fixed number
	 * of interactions. For example, if all individuals always interact with all
	 * their neighbours, see {@link IBSGroup.SamplingType#ALL}. The only exception
	 * are well-mixed populations, {@link Geometry.Type#MEANFIELD}. With continuous
	 * traits all possible encounters would need to be considered, which is
	 * computationally not feasible. In contrast, for discrete traits separate
	 * calculations to determine the scores of each trait type are possible.
	 * <li>With random interactions the scores of individuals are based on variable
	 * sets of interaction partners and their numbers of interactions may vary.
	 * </ol>
	 * 
	 * @see IBSGroup.SamplingType
	 */
	protected boolean adjustScores;

	/**
	 * Optimization: Flag to indicate whether lookup tables can be used.
	 */
	protected boolean hasLookupTable;

	/**
	 * Flag to indicate whether dynamic is neutral, i.e. no selection acting on the
	 * different traits. The dynamic is considered neutral if
	 * <code>{@link #maxScore}-{@link #minScore}&lt;1e-8</code>.
	 */
	protected boolean isNeutral;

	/**
	 * Optimization: The index of the individual that currently holds the maximum
	 * score. This allows more efficient fitness based picking.
	 * 
	 * @see #pickFitFocalIndividual()
	 * @see #pickFitFocalIndividual(int)
	 */
	protected int maxEffScoreIdx = -1;

	/**
	 * Perform synchronous migration.
	 * 
	 * @return the number of migrants
	 */
	public int doSyncMigration() {
		// number of migratory events
		int nMigrants = 0;
		switch (migrationType) {
			case NONE:
				break;
			case BIRTH_DEATH:
				nMigrants = nextBinomial(1.0 - pMigration, nPopulation);
				for (int n = 0; n < nMigrants; n++)
					doBirthDeathMigration();
				break;
			case DEATH_BIRTH:
				nMigrants = nextBinomial(1.0 - pMigration, nPopulation);
				for (int n = 0; n < nMigrants; n++)
					doDeathBirthMigration();
				break;
			case DIFFUSION:
				nMigrants = nextBinomial(1.0 - pMigration, nPopulation);
				for (int n = 0; n < nMigrants; n++)
					doDiffusionMigration();
				break;
			default: // should never get here
				throw new Error("Unknown migration type (" + migrationType + ")");
		}
		return nMigrants;
	}

	/**
	 * Perform asynchronous migration.
	 */
	public void doMigration() {
		switch (migrationType) {
			case NONE:
				break;
			case BIRTH_DEATH:
				doBirthDeathMigration();
				break;
			case DEATH_BIRTH:
				doDeathBirthMigration();
				break;
			case DIFFUSION:
				doDiffusionMigration();
				break;
			default: // should never get here
				throw new Error("Unknown migration type (" + migrationType + ")");
		}
	}

	/**
	 * Perform diffusion migration, which is implemented as two players swap their
	 * locations while leaving their respective neighbourhood structure untouched.
	 */
	public void doDiffusionMigration() {
		int migrant = random0n(nPopulation);
		// migrant swaps places with random neighbor
		int[] myNeighs = interaction.out[migrant];
		int aNeigh = myNeighs[random0n(interaction.kout[migrant])];
		updatePlayerSwap(migrant, aNeigh);
	}

	/**
	 * Swap players in locations {@code a} and {@code b}.
	 *
	 * @param a the index of the first individual
	 * @param b the index of the second individual
	 */
	public void updatePlayerSwap(int a, int b) {
		swapTraits(a, b); // trait changed; still needs to be committed
		// fitness accounting:
		// synchronous: no need to worry about fitness - this is determined afterwards
		// asynchronous:
		// - if interactions are random (payoffs are accumulated), simply take the
		// scores along
		// - if interactions are with all neighbors (payoffs are adjusted) we need to
		// recalculate the payoffs
		if (populationUpdate.isSynchronous()) {
			// NOTE: this is not efficient because it deals unnecessarily with types and
			// scores; enough to only copy from scratch to traits.
			commitTraitAt(a);
			commitTraitAt(b);
			return;
		}
		if (adjustScores) {
			if (haveSameTrait(a, b))
				return; // nothing to do
			adjustGameScoresAt(a);
			adjustGameScoresAt(b);
			return;
		}
		// NOTE: again, commitTrait is overkill because the composition of the
		// population has not changed; what about interaction counts?
		commitTraitAt(a);
		commitTraitAt(b);
		// shouldn't we re-calculate the scores of the two individuals in their new
		// location?
		swapScoresAt(a, b);
	}

	/**
	 * Perform a birth-death migration, where a focal individual (selected
	 * proportional to fitness) produces a migrant offspring, which replaces another
	 * member of the population uniformly at random.
	 * <p>
	 * <strong>Note:</strong> This is almost identical to Moran (birth-death)
	 * updating in well-mixed populations (only victim and migrant always different)
	 */
	public void doBirthDeathMigration() {
		int migrant = pickFitFocalIndividual();
		int vacant = random0n(nPopulation - 1);
		if (vacant >= migrant)
			vacant++;
		migrateMoran(migrant, vacant);
	}

	/**
	 * Perform a death-birth migration, where a member of the population (selected
	 * uniformly at random) dies and the remaining individuals compete to repopulate
	 * the vacant site. One competitor succeeds with a probability proportional
	 * proportional to fitness.
	 * <p>
	 * <strong>Note:</strong> This is almost identical to Moran (birth-death)
	 * updating in well-mixed populations
	 */
	public void doDeathBirthMigration() {
		int vacant = random0n(nPopulation);
		int migrant = pickFitFocalIndividual(vacant);
		migrateMoran(migrant, vacant);
	}

	/**
	 * Draws the index of a site in the population uniformly at random, irrespective
	 * of whether it is occupied or not.
	 *
	 * @return the index of the picked site
	 */
	public int pickFocalSite() {
		return random0n(nPopulation);
	}

	/**
	 * Draws the index of a site in the population uniformly at random, irrespective
	 * of whether it is occupied or not.
	 *
	 * @param excl the index of the excluded site
	 * @return the index of the picked site
	 */
	public int pickFocalSite(int excl) {
		if (excl < 0 || excl > nPopulation)
			return pickFocalSite();
		int rand = random0n(nPopulation - 1);
		if (rand >= excl)
			return rand + 1;
		return rand;
	}

	/**
	 * Draws the index of a individual of the population uniformly at random. Vacant
	 * sites are skipped. In the absence of vacant sites this is equivalent to
	 * {@link #pickFocalSite()}.
	 * <p>
	 * <strong>Important:</strong> This method is highly time sensitive. Any
	 * optimization effort is worth making.
	 *
	 * @return the index of the picked individual
	 */
	public int pickFocalIndividual() {
		if (VACANT < 0)
			return pickFocalSite();
		return pickFocal(-1);
	}

	/**
	 * Pick a focal individual uniformly at random but excluding the individual with
	 * index <code>excl</code>. Helper method.
	 * 
	 * @param excl the index of the excluded individual
	 * @return the index of the picked individual
	 */
	private int pickFocal(int excl) {
		int nTot = getPopulationSize();
		if (excl >= 0)
			nTot--;
		int pick = random0n(nTot);
		if (pick + pick > nTot) {
			pick = nTot - pick - 1;
			// start search at tail; cannot stop halfway;
			// vacancies may be concentrated at tail
			for (int n = nPopulation - 1; n >= 0; n--) {
				if (isVacantAt(n))
					continue;
				pick--;
				if (pick < 0) {
					if (n == excl)
						continue;
					return n;
				}
			}
		} else {
			// start search at head; cannot stop halfway;
			// vacancies may be concentrated at head
			for (int n = 0; n < nPopulation; n++) {
				if (isVacantAt(n))
					continue;
				pick--;
				if (pick < 0) {
					if (n == excl)
						continue;
					return n;
				}
			}
		}
		engine.fatal("pickFocal() failed to pick individual...");
		// fatal does not return control
		return -1;
	}

	/**
	 * Draws the index of a individual of the population uniformly at random but
	 * excluding the individual with index <code>excl</code>. Vacant sites are
	 * skipped.
	 * <p>
	 * <strong>Important:</strong> This method is highly time sensitive. Any
	 * optimization effort is worth making.
	 *
	 * @param excl the index of the excluded individual
	 * @return the index of the picked individual
	 */
	public int pickFocalIndividual(int excl) {
		if (VACANT < 0)
			return pickFocalSite(excl);
		if (excl < 0 || excl > nPopulation)
			return pickFocalIndividual();
		return pickFocal(excl);
	}

	/**
	 * Draws the index of a member of the population with a probability proportional
	 * to fitness.
	 * <p>
	 * <strong>Note:</strong> scores must be <code>&ge;0</code>
	 * <p>
	 * <strong>Important:</strong> This method is highly time sensitive. Any
	 * optimization effort is worth making.
	 * 
	 * <h3>Discussions/extensions:</h3>
	 * <ol>
	 * <li>Should picking include vacant sites, or not, or selectable?
	 * <li>Currently vacant sites are excluded for fitness based picking but not for
	 * random picking.
	 * <li>Perform more systematic analysis regarding the threshold population size
	 * (currently {@code nPopulation &geq; 100}) for the two Gillespie methods
	 * sometimes {@code 4*nPopulation} trials are needed, which seems too much...
	 * </ol>
	 *
	 * @return the index of the picked member
	 */
	public int pickFitFocalIndividual() {
		// differences in scores too small, pick random individual
		if (isNeutral)
			return pickFocalIndividual();

		if (VACANT < 0) {
			if (nPopulation >= 100) {
				// optimization of gillespie algorithm to prevent bookkeeping (at the expense of
				// drawing more random numbers) see e.g. http://arxiv.org/pdf/1109.3627.pdf
				double mScore;
				// note: for constant selection maxEffScoreIdx is never set; using the
				// effective current maximum score makes this optimization more efficient
				if (maxEffScoreIdx < 0)
					mScore = maxFitness;
				else
					mScore = getFitnessAt(maxEffScoreIdx);
				int aRand = -1;
				do {
					aRand = random0n(nPopulation);
				} while (random01() * mScore > getFitnessAt(aRand)); // note: if < holds aRand is ok
				return aRand;
			}
			// standard, non-optimized version
			double hit = random01() * sumFitness;
			for (int n = 0; n < nPopulation; n++) {
				hit -= getFitnessAt(n);
				if (hit < 0.0)
					return n;
			}
			if (hit < 1e-6 && getFitnessAt(nPopulation - 1) > 1e-6)
				return nPopulation - 1;
			debugScores(hit);
			engine.fatal("pickFitFocalIndividual() failed to pick individual...");
			// fatal does not return control
			return -1;
		}

		// vacancies require some extra care
		if (nPopulation >= 100) {
			// optimization of gillespie algorithm to prevent bookkeeping (at the expense of
			// drawing more random numbers) see e.g. http://arxiv.org/pdf/1109.3627.pdf
			double mScore;
			// note: for constant selection maxEffScoreIdx is never set using the effective
			// current maximum score makes this optimization more efficient
			if (maxEffScoreIdx < 0)
				mScore = maxFitness;
			else
				mScore = getFitnessAt(maxEffScoreIdx);
			int aRand = -1;
			do {
				aRand = random0n(nPopulation);
			} while (isVacantAt(aRand) || random01() * mScore > getFitnessAt(aRand)); // note: if < holds aRand is ok
			return aRand;
		}

		// standard, non-optimized version
		double hit = random01() * sumFitness;
		for (int n = 0; n < nPopulation; n++) {
			if (isVacantAt(n))
				continue;
			hit -= getFitnessAt(n);
			if (hit <= 0.0)
				return n;
		}
		debugScores(hit);
		engine.fatal("pickFitFocalIndividual() failed to pick individual...");
		// fatal does not return control
		return -1;
	}

	/**
	 * Draws the index of a member of the population with a probability proportional
	 * to fitness but excluding the individual with the index {@code excl}.
	 * <p>
	 * <strong>Note:</strong> scores must be <code>&ge;0</code>
	 * <p>
	 * <strong>Important:</strong> This method is highly time sensitive. Any
	 * optimization effort is worth making.
	 *
	 * @param excl the index of the member that should be excluded from picking
	 * @return the index of the picked member
	 */
	public int pickFitFocalIndividual(int excl) {
		if (excl < 0 || excl > nPopulation)
			return pickFitFocalIndividual();

		// differences in scores too small, pick random individual
		if (isNeutral)
			return pickFocalIndividual(excl);

		if (VACANT < 0) {
			// note: review threshold for optimizations (see pickFitFocalIndividual above)
			if (nPopulation >= 100) {
				// optimization of gillespie algorithm to prevent bookkeeping (at the expense of
				// drawing more random numbers) see e.g. http://arxiv.org/pdf/1109.3627.pdf
				if (excl == maxEffScoreIdx) {
					// excluding the maximum score can cause issues if it is much larger than the
					// rest;
					// need to find the second largest fitness value (note using
					// mapToFitness(maxScore)
					// may be even worse because most candidates are rejected
					double mScore = map2fit.map(second(maxEffScoreIdx));
					int aRand = -1;
					do {
						aRand = random0n(nPopulation - 1);
						if (aRand >= excl)
							aRand++;
					} while (random01() * mScore > getFitnessAt(aRand)); // note: if < holds aRand is ok
					return aRand;
				}

				double mScore;
				// note: for constant selection maxEffScoreIdx is never set
				// using the effective current maximum score makes this optimization more
				// efficient
				if (maxEffScoreIdx < 0)
					mScore = maxFitness;
				else
					mScore = getFitnessAt(maxEffScoreIdx);
				int aRand = -1;
				do {
					aRand = random0n(nPopulation - 1);
					if (aRand >= excl)
						aRand++;
				} while (random01() * mScore > getFitnessAt(aRand)); // note: if < holds aRand is ok
				return aRand;
			}
			double hit = random01() * (sumFitness - getFitnessAt(excl));
			// two loops prevent repeated checks concerning excl
			for (int n = 0; n < excl; n++) {
				hit -= getFitnessAt(n);
				if (hit < 0.0)
					return n;
			}
			for (int n = excl + 1; n < nPopulation; n++) {
				hit -= getFitnessAt(n);
				if (hit < 0.0)
					return n;
			}
			// last resort...
			if (excl == nPopulation - 1) {
				if (hit < 1e-6 && getFitnessAt(nPopulation - 2) > 1e-6)
					return nPopulation - 2;
				if (hit < 1e-6 && getFitnessAt(nPopulation - 1) > 1e-6)
					return nPopulation - 1;
			}
			debugScores(hit);
			engine.fatal("pickFitFocalIndividual(int) failed to pick individual...");
			// fatal does not return control
			return -1;
		}

		// vacancies require some extra care
		if (nPopulation >= 100) {
			// optimization of gillespie algorithm to prevent bookkeeping (at the expense of
			// drawing more random numbers) see e.g. http://arxiv.org/pdf/1109.3627.pdf
			if (excl == maxEffScoreIdx) {
				// excluding the maximum score can cause issues if it is much larger than the
				// rest;
				// need to find the second largest fitness value (note using
				// mapToFitness(maxScore)
				// may be even worse because most candidates are rejected
				double mScore = map2fit.map(second(maxEffScoreIdx));
				int aRand = -1;
				do {
					aRand = random0n(nPopulation - 1);
					if (aRand >= excl)
						aRand++;
				} while (isVacantAt(aRand) || random01() * mScore > getFitnessAt(aRand)); // note: if < holds aRand is
																							// ok
				return aRand;
			}

			double mScore;
			// note: for constant selection maxEffScoreIdx is never set
			// using the effective current maximum score makes this optimization more
			// efficient
			if (maxEffScoreIdx < 0)
				mScore = maxFitness;
			else
				mScore = getFitnessAt(maxEffScoreIdx);
			int aRand = -1;
			do {
				aRand = random0n(nPopulation - 1);
				if (aRand >= excl)
					aRand++;
			} while (random01() * mScore > getFitnessAt(aRand)); // note: if < holds aRand is ok
			return aRand;
		}
		double hit = random01() * (sumFitness - getFitnessAt(excl));
		// two loops prevent repeated checks concerning excl
		for (int n = 0; n < excl; n++) {
			if (isVacantAt(n))
				continue;
			hit -= getFitnessAt(n);
			if (hit <= 0.0)
				return n;
		}
		for (int n = excl + 1; n < nPopulation; n++) {
			if (isVacantAt(n))
				continue;
			hit -= getFitnessAt(n);
			if (hit <= 0.0)
				return n;
		}
		debugScores(hit);
		engine.fatal("pickFitFocalIndividual(int) failed to pick individual...");
		// fatal does not return control
		return -1;
	}

	/**
	 * Find the second highest score.
	 * 
	 * @param excl the index of the member that should be excluded from picking
	 * @return the second highest score
	 */
	private double second(int excl) {
		double max = getScoreAt(maxEffScoreIdx);
		double second = -Double.MAX_VALUE;
		for (int n = 0; n < excl; n++) {
			second = Math.max(second, getScoreAt(n));
			// if a second individual has maximum score, no need to look further
			if (Math.abs(second - max) < 1e-8)
				return second;
		}
		for (int n = excl + 1; n < nPopulation; n++) {
			second = Math.max(second, getScoreAt(n));
			// if a second individual has maximum score, no need to look further
			if (Math.abs(second - max) < 1e-8)
				return second;
		}
		return second;
	}

	/**
	 * Log report if picking failed to shed better light on what might be the root
	 * cause for the failure.
	 * 
	 * @param hit the 'left-over fitness' from picking
	 */
	protected void debugScores(double hit) {
		if (!logger.isLoggable(Level.FINE))
			return;
		logger.fine("aborted in generation: " + Formatter.format(engine.getModel().getUpdates(), 2) + "\nscore dump:");
		double sum = 0.0;
		for (int n = 0; n < nPopulation; n++) {
			double sn = getScoreAt(n);
			double fn = getFitnessAt(n);
			String in = module.isStatic() ? "-" : "" + interactions[n];
			logger.fine("score[" + n + "]=" + Formatter.format(sn, 6) + " → " + Formatter.format(fn, 6)
					+ ", interactions[" + n + "]=" + in +
					", base=" + map2fit.getBaseline() + ", selection=" + map2fit.getSelection());
			sum += fn;
		}
		logger.fine("Failed to pick parent... hit: " + hit + ", sumScores: " + Formatter.format(sumFitness, 6)
				+ " (should be " + Formatter.format(sum, 6) + ")");
	}

	/**
	 * Pick a neighbour of the focal individual {@code me} with probability
	 * proportional to their fitness.
	 * 
	 * @param me the index of the focal individual
	 * @return the index of a neighbour
	 */
	protected int pickFitNeighborAt(int me) {
		return pickFitNeighborAt(me, false);
	}

	/**
	 * Pick a neighbour of the focal individual {@code me} with probability
	 * proportional to their fitness. If the flag {@code withSelf==true} then the
	 * focal individual is included in the picking.
	 *
	 * @param me       the index of the focal individual
	 * @param withSelf the flag whether to include self
	 * @return the index of a neighbour
	 */
	protected int pickFitNeighborAt(int me, boolean withSelf) {
		if (isNeutral)
			return pickNeutralNeighbourAt(me, withSelf);

		// mean-field
		if (competition.getType() == Geometry.Type.MEANFIELD) {
			debugNModels = 0;
			if (withSelf)
				return pickFitFocalIndividual();
			return pickFitFocalIndividual(me);
		}

		if (VACANT < 0) {
			// structured population
			debugModels = competition.in[me];
			debugNModels = competition.kin[me];
			double totFitness = 0.0;
			double myFit = 0.0;
			if (withSelf) {
				if (debugNModels == 0)
					return me;
				myFit = getFitnessAt(me);
				totFitness = myFit;
			} else {
				switch (debugNModels) {
					case 0:
						// no upstream neighbour
						return -1;
					case 1:
						return debugModels[0];
					default:
				}
			}
			for (int n = 0; n < debugNModels; n++)
				totFitness += getFitnessAt(debugModels[n]);

			// negligible fitness, pick uniformly at random; same as neutral above
			if (totFitness <= 1e-8)
				return pickNeutralNeighbourAt(me, withSelf);

			double hit = random01() * totFitness - myFit;
			if (hit < 0.0)
				return me;
			for (int n = 0; n < debugNModels; n++) {
				hit -= getFitnessAt(debugModels[n]);
				if (hit < 0.0)
					return debugModels[n];
			}
			// should not get here
			debugScores(hit);
			throw new Error(
					"drawFitNeighborAt(int) failed to pick neighbour... (" + hit + ", sum: " + totFitness + ")");
		}

		// vacancies require some extra care
		debugModels = competition.in[me];
		debugNModels = competition.kin[me];
		double totFitness = 0.0;
		double myFit = 0.0;
		if (withSelf && !isVacantAt(me)) {
			if (debugNModels == 0)
				return me;
			myFit = getFitnessAt(me);
			totFitness = myFit;
		} else {
			switch (debugNModels) {
				case 0:
					return -1;
				case 1:
					int neigh = debugModels[0];
					if (isVacantAt(neigh))
						return -1;
					return neigh;
				default:
			}
		}
		for (int n = 0; n < debugNModels; n++) {
			int neigh = debugModels[n];
			if (isVacantAt(neigh))
				continue;
			totFitness += getFitnessAt(neigh);
		}

		// negligible fitness, pick uniformly at random; same as neutral above
		if (totFitness <= 1e-8)
			return pickNeutralNeighbourAt(me, withSelf);

		double hit = random01() * totFitness - myFit;
		if (hit < 0.0)
			return me;
		for (int n = 0; n < debugNModels; n++) {
			int neigh = debugModels[n];
			if (isVacantAt(neigh))
				continue;
			hit -= getFitnessAt(neigh);
			if (hit < 0.0)
				return neigh;
		}
		// should not get here
		debugScores(hit);
		throw new Error("drawFitNeighborAt(int) failed to pick neighbour... (" + hit + ", sum: " + totFitness + ")");
	}

	/**
	 * Helper method to do picking under neutral conditions (no or negligible
	 * fitness differences). In well-mixed populations the picking includes the
	 * focal individual if {@code withSelf==true}. Otherwise picking never includes
	 * {@code me}.
	 * 
	 * @param me       the index of the focal individual
	 * @param withSelf the flag whether to include self
	 * @return the index of a neighbour
	 */
	private int pickNeutralNeighbourAt(int me, boolean withSelf) {
		if (competition.getType() == Geometry.Type.MEANFIELD) {
			debugNModels = 0;
			if (withSelf)
				return pickFocalIndividual();
			return pickFocalIndividual(me);
		}
		return pickNeighborSiteAt(me);
	}

	/**
	 * Picks a neighbouring site of the focal individual {@code me} uniformly at
	 * random. This is where the focal individual places its offspring in the Moran
	 * process under Birth-death updating. The original Moran process refers to
	 * well-mixed (or mean-field) populations where the offspring can replace its
	 * parent (albeit with a small probability of \(o(1/N)\), where \(N\) is the
	 * population size). In contrast in the spatial Moran process the offspring
	 * replaces a <em>neighbour</em> of the parent.
	 * 
	 * <h3>Notes:</h3>
	 * <ol>
	 * <li>The parent is never picked.
	 * <li>Vacant sites are picked with the same probability as occupied ones.
	 * </ol>
	 *
	 * @param me the index of the focal individual
	 * @return the index of a neighbour
	 * 
	 * @see #pickFitNeighborAt(int)
	 * @see #updatePlayerMoranBirthDeath()
	 */
	public int pickNeighborSiteAt(int me) {
		// mean-field
		if (competition.getType() == Geometry.Type.MEANFIELD)
			return pickFocalSite(me);

		debugModels = competition.out[me];
		debugNModels = competition.kout[me];
		switch (debugNModels) {
			case 0:
				// no downstream neighbour? no place to put offspring
				return -1;
			case 1:
				// place offspring in single downstream node
				return debugModels[0];
			default:
				return debugModels[random0n(debugNModels)];
		}
	}

	/**
	 * Update individual with index {@code me} and adopt the trait of individual
	 * with index {@code you}.
	 * <p>
	 * <strong>Note:</strong> method must be subclassed to deal with different data
	 * types of traits but should also include a call to super.
	 *
	 * @param me  the index of the focal individual
	 * @param you the index of the model individual to adopt trait from
	 * 
	 * @see IBSDPopulation#updateFromModelAt(int, int)
	 * @see IBSMCPopulation#updateFromModelAt(int, int)
	 */
	public void updateFromModelAt(int me, int you) {
		tags[me] = tags[you];
		debugModel = you;
	}

	/**
	 * Update the score of individual {@code me}.
	 * <p>
	 * After initialization and for synchronized population updating this method is
	 * invoked (rather than {@link #playGameAt(int)}). The synchronous flag simply
	 * indicates that all players are going to be updated. For directed graphs this
	 * implies that incoming and outgoing links do not need to be treated separately
	 * because every outgoing link corresponds to an incoming link of another node.
	 *
	 * @param me the index of the focal individual
	 */
	public void playGameSyncAt(int me) {
		if (module.isStatic()) {
			engine.fatal("playGameSyncAt(int) should not be called for constant selection!");
			// fatal does not return control
		}
		// during initialization, pretend we are doing this synchronously - just in case
		// someone's interested.
		PopulationUpdate.Type put = populationUpdate.getType();
		populationUpdate.setType(PopulationUpdate.Type.SYNC);
		if (module.isPairwise()) {
			interGroup.pickAt(me, true);
			playPairGameAt(interGroup);
		} else {
			interGroup.pickAt(me, true);
			playGroupGameAt(interGroup);
		}
		populationUpdate.setType(put);
	}

	/**
	 * Update the score of individual {@code me}. This method is called if adjusting
	 * scores is not an option. If site {@code me} is vacant do nothing.
	 *
	 * @param me the index of the focal individual
	 * 
	 * @see #adjustGameScoresAt(int)
	 * @see #adjustPairGameScoresAt(int)
	 */
	public void playGameAt(int me) {
		if (isVacantAt(me))
			return;
		if (module.isStatic()) {
			engine.fatal("playGameAt(int) should not be called for constant selection!");
			// fatal does not return control
		}
		// any graph - interact with out-neighbors
		// same as earlier approach to undirected graphs
		if (adjustScores) {
			throw new Error("ERROR: playGameAt(int idx) and adjustScores are incompatible!");
		}
		if (module.isPairwise()) {
			interGroup.pickAt(me, true);
			playPairGameAt(interGroup);
			// if undirected, we are done
			if (interaction.isUndirected)
				return;

			// directed graph - additionally/separately interact with in-neighbors
			interGroup.pickAt(me, false);
			playPairGameAt(interGroup);
		} else {
			interGroup.pickAt(me, true);
			playGroupGameAt(interGroup);
			// if undirected, we are done
			if (interaction.isUndirected)
				return;
			// directed graph - additionally/separately interact with in-neighbors
			interGroup.pickAt(me, false);
			playGroupGameAt(interGroup);
		}
	}

	/**
	 * Adjust scores of focal player {@code me} and its neighbours (interaction
	 * partners).
	 * 
	 * <h3>Requirements/notes:</h3>
	 * <ol>
	 * <li>This optimized method is only applicable if
	 * {@link IBSGroup.SamplingType#ALL}
	 * is true and not {@link Geometry.Type#MEANFIELD}, i.e. if the interaction
	 * group includes all neighbors but not all other members of the population.
	 * <li>For pairwise interactions more efficient approaches are possible but
	 * those require direct access to the trait and are hence delegated to
	 * subclasses.
	 * </ol>
	 * 
	 * @param me the index of the focal individual
	 * 
	 * @see #adjustPairGameScoresAt(int)
	 */
	public void adjustGameScoresAt(int me) {
		// check first whether an actual trait change has occurred
		if (isSameTrait(me)) {
			commitTraitAt(me);
			return;
		}
		// constant selection does not require involved score adjustments;
		// called only under special circumstances, e.g. with optimizeHomo set;
		// after committing make sure fitness is updated
		if (module.isStatic()) {
			commitTraitAt(me);
			updateScores();
			return;
		}
		if (module.isPairwise()) {
			// scores of pairwise interactions can be adjusted more efficiently
			// but this requires access to the actual traits.
			adjustPairGameScoresAt(me);
			return;
		}
		// any graph - interact with out-neighbors
		// same as earlier approach to undirected graphs
		if (interaction.isUndirected) {
			// undirected graph - same as earlier approach
			// remove old scores
			int[] neigh = interaction.out[me];
			int nNeigh = competition.kout[me];
			interGroup.setGroupAt(me, neigh, nNeigh);
			yalpGroupGameAt(interGroup);
			for (int i = 0; i < nNeigh; i++) {
				int you = neigh[i];
				interGroup.setGroupAt(you, interaction.out[you], interaction.kout[you]);
				yalpGroupGameAt(interGroup);
			}
			commitTraitAt(me);
			// add new scores
			interGroup.setGroupAt(me, neigh, nNeigh);
			playGroupGameAt(interGroup);
			for (int i = 0; i < nNeigh; i++) {
				int you = neigh[i];
				interGroup.setGroupAt(you, interaction.out[you], interaction.kout[you]);
				playGroupGameAt(interGroup);
			}
			return;
		}

		// directed graph - separately interact with in- and out-neighbors
		// remove old scores
		int[] neigh = interaction.out[me];
		int nNeigh = interaction.kout[me];
		interGroup.setGroupAt(me, neigh, nNeigh);
		yalpGroupGameAt(interGroup);
		for (int i = 0; i < nNeigh; i++) {
			int you = neigh[i];
			interGroup.setGroupAt(you, interaction.out[you], interaction.kout[you]);
			yalpGroupGameAt(interGroup);
		}
		neigh = interaction.in[me];
		nNeigh = interaction.kin[me];
		interGroup.setGroupAt(me, neigh, nNeigh);
		yalpGroupGameAt(interGroup);
		for (int i = 0; i < nNeigh; i++) {
			int you = neigh[i];
			interGroup.setGroupAt(you, interaction.in[you], interaction.kin[you]);
			yalpGroupGameAt(interGroup);
		}
		commitTraitAt(me);
		// add new scores
		neigh = interaction.out[me];
		nNeigh = interaction.kout[me];
		interGroup.setGroupAt(me, neigh, nNeigh);
		playGroupGameAt(interGroup);
		for (int i = 0; i < nNeigh; i++) {
			int you = neigh[i];
			interGroup.setGroupAt(you, interaction.out[you], interaction.kout[you]);
			playGroupGameAt(interGroup);
		}
		neigh = interaction.in[me];
		nNeigh = interaction.kin[me];
		interGroup.setGroupAt(me, neigh, nNeigh);
		playGroupGameAt(interGroup);
		for (int i = 0; i < nNeigh; i++) {
			int you = neigh[i];
			interGroup.setGroupAt(you, interaction.in[you], interaction.kin[you]);
			playGroupGameAt(interGroup);
		}
	}

	/**
	 * Update the score of the individual with index {@code index} by adding
	 * {@code newscore} from single interaction.
	 * 
	 * @param index    the index of the individual
	 * @param newscore the new score of the individual
	 */
	public void updateScoreAt(int index, double newscore) {
		updateScoreAt(index, newscore, 1);
	}

	/**
	 * Update the score of the individual with index {@code index} by adding
	 * ({@code incr &gt; 0} or removing, {@code incr &lt; 0}) {@code newscore} as
	 * the result of {@code incr} interactions.
	 * 
	 * <h3>Important:</h3>
	 * <ol>
	 * <li>Traits are already committed when adding scores
	 * (<code>incr&gt;0</code>).
	 * <li>Traits are <em>not</em> committed when removing scores
	 * (<code>incr&lt;0</code>).
	 * <li>This routine is never called for the focal site (i.e. the one that may
	 * have changed trait and hence where it matters whether traits are
	 * committed).
	 * <li>{@link #resetScoreAt(int)} deals with the focal site.
	 * </ol>
	 * 
	 * @param index    the index of the individual
	 * @param newscore score/payoff to add (<code>incr&gt;0</code>) or subtract
	 *                 (<code>incr&lt;0</code>)
	 * @param incr     number of interactions
	 */
	public void updateScoreAt(int index, double newscore, int incr) {
		double before = scores[index];
		// note: incr == 0 doesn't make sense (except possibly for isolated individuals)
		if (incr < 0)
			newscore = -newscore;
		if (playerScoreAveraged) {
			int count = interactions[index];
			scores[index] = (before * count + newscore) / Math.max(1, count + incr);
		} else {
			scores[index] += newscore;
		}
		interactions[index] += incr;
		updateEffScoreRange(index, before, scores[index]);
		updateFitnessAt(index);
	}

	/**
	 * Update the fitness of the individual with index {@code idx} by mapping its
	 * current score. Keeps track of {@code sumFitness}.
	 * <p>
	 * <strong>Note:</strong> If {@code sumFitness} decreases dramatically rounding
	 * errors become an issue. More specifically, if {@code sumFitness} is reduced
	 * to half or less, recalculate from scratch.
	 * 
	 * @param idx the index of the individual
	 */
	public void updateFitnessAt(int idx) {
		double after = map2fit.map(scores[idx]);
		double diff = after - (isVacantAt(idx) ? 0.0 : fitness[idx]);
		fitness[idx] = after;
		sumFitness += diff;
		// whenever sumFitness decreases dramatically rounding errors become an issue
		// if update reduces sumFitness by half or more, recalculate from scratch
		if (-diff > sumFitness)
			sumFitness = ArrayMath.norm(fitness);
	}

	/**
	 * Sets the score of individual with index {@code index} to {@code newscore} as
	 * the result of {@code inter} interactions. Also derives the corresponding
	 * fitness and adjusts {@code sumFitness}.
	 * <p>
	 * <strong>Note:</strong> Assumes that {@link #resetScores()} was called earlier
	 * (or at least {@link #resetScoreAt(int)} for those sites that
	 * {@code setScoreAt(int)} is used for updating their score).
	 *
	 * @param index    the index of the individual
	 * @param newscore new score to set
	 * @param inter    number of interactions
	 */
	public void setScoreAt(int index, double newscore, int inter) {
		interactions[index] = inter;
		scores[index] = (playerScoreAveraged ? newscore : newscore * inter);
		double fit = map2fit.map(scores[index]);
		fitness[index] = fit;
		sumFitness += fit;
	}

	/**
	 * Removes the score {@code nilscore} based on a single interaction from the
	 * individual with index {@code index}.
	 * 
	 * @param index    the index of the individual
	 * @param nilscore the score to remove
	 * 
	 * @see #updateScoreAt(int, double, int)
	 */
	public void removeScoreAt(int index, double nilscore) {
		updateScoreAt(index, nilscore, -1);
	}

	/**
	 * Removes the score {@code nilscore} based on {@code incr} interactions from
	 * the individual with index {@code index}.
	 *
	 * @param index    the index of the individual
	 * @param nilscore the score to remove
	 * @param incr     the number of interactions to remove
	 * 
	 * @see #updateScoreAt(int, double, int)
	 */
	public void removeScoreAt(int index, double nilscore, int incr) {
		updateScoreAt(index, nilscore, -incr);
	}

	/**
	 * Reset score of individual at index <code>index</code>.
	 * <p>
	 * <strong>Important:</strong> traits must not yet have been committed.
	 * 
	 * <h3>Discussions/extensions:</h3>
	 * Revise the entire trait updating procedure: it's inefficient to first
	 * reset scores then update traits then update score...
	 * 
	 * @param index the index of the individual
	 */
	public void resetScoreAt(int index) {
		double before = scores[index];
		scores[index] = 0.0;
		interactions[index] = 0;
		updateEffScoreRange(index, before, 0.0);
		sumFitness -= fitness[index];
		fitness[index] = 0.0;
	}

	/**
	 * Swap the scores (and fitness) of individuals with indices {@code idxa} and
	 * {@code idxb}.
	 * 
	 * @param idxa the index of the first individual
	 * @param idxb the index of the second individual
	 */
	public void swapScoresAt(int idxa, int idxb) {
		double myScore = scores[idxa];
		scores[idxa] = scores[idxb];
		scores[idxb] = myScore;
		myScore = fitness[idxa];
		fitness[idxa] = fitness[idxb];
		fitness[idxb] = myScore;
		int myInteractions = interactions[idxa];
		interactions[idxa] = interactions[idxb];
		interactions[idxb] = myInteractions;
		if (maxEffScoreIdx == idxa)
			maxEffScoreIdx = idxb;
		else if (maxEffScoreIdx == idxb)
			maxEffScoreIdx = idxa;
	}

	/**
	 * Reset scores and fitness of all individuals to zero.
	 */
	public void resetScores() {
		// well-mixed populations use lookup table for scores and fitness
		if (scores != null)
			Arrays.fill(scores, 0.0);
		if (fitness != null)
			Arrays.fill(fitness, 0.0);
		if (interactions != null)
			Arrays.fill(interactions, 0);
		sumFitness = 0.0;
		if (VACANT < 0 || getPopulationSize() == 0) {
			// no vacancies or no population
			maxEffScoreIdx = 0;
			return;
		}
		int start = -1;
		// find first non-vacant site
		while (isVacantAt(++start))
			;
		maxEffScoreIdx = start;
	}

	/**
	 * Update the scores of all individuals in the population.
	 */
	public void updateScores() {
		for (int n = 0; n < nPopulation; n++) {
			// since everybody's score is updated there is no need to treat in- and
			// out-going links differently - after all, each outgoing link is an incoming
			// link for another node...
			playGameSyncAt(n);
			updateFitnessAt(n);
		}
		setMaxEffScoreIdx();
	}

	/**
	 * Gets the score of the individual with index {@code idx}.
	 *
	 * @param idx the index of the individual
	 * @return the score of the individual
	 */
	public double getScoreAt(int idx) {
		return scores[idx];
	}

	/**
	 * Gets the fitness of the individual with index {@code idx}.
	 *
	 * @param idx the index of the individual
	 * @return the fitness of the individual
	 */
	public double getFitnessAt(int idx) {
		return fitness[idx];
	}

	/**
	 * Keep track of highest score through a reference to the corresponding
	 * individual.
	 * 
	 * <h3>Discussions/extensions:</h3>
	 * <ol>
	 * <li>Keeping track of the lowest performing individual is very costly because
	 * the poor performers are much more likely to get updated. In contrast, the
	 * maximum performer is least likely to get replaced. For example, in
	 * <code>cSD</code> with a well-mixed population of <code>10'000</code> spends
	 * <code>&gt;80%</code> of CPU time hunting down the least performing
	 * individual! Besides, the minimum score is never used. The maximum score is
	 * only used for global, fitness based selection such as the Birth-death
	 * process. (room for optimization?)
	 * <li>For synchronous updates this method is not only wasteful but problematic
	 * because sites that are later updated experience different conditions.
	 * <li>For constant fitness ({@code maxEffScoreIdx &lt; 0}) tracking the maximum
	 * is not needed.
	 * <li><code>if (isVacantAt(index)) {...}</code> is only executed if
	 * {@code after == before}, i.e. the code is almost dead... suspicious or
	 * ingenious?!
	 * </ol>
	 * 
	 * @param index  the index of the individual
	 * @param before the score before the update
	 * @param after  the score after the update
	 * @return <code>true</code> if effective range of payoffs has changed (or, more
	 *         precisely, if the reference individual has changed);
	 *         <code>false</code> otherwise.
	 */
	protected boolean updateEffScoreRange(int index, double before, double after) {
		if (populationUpdate.isSynchronous() || maxEffScoreIdx < 0)
			return false;

		if (after > before) {
			// score increased
			if (index == maxEffScoreIdx)
				return false;
			if (after <= scores[maxEffScoreIdx])
				return false;
			maxEffScoreIdx = index;
			return true;
		}
		// score decreased
		if (after < before) {
			if (index == maxEffScoreIdx) {
				// maximum score decreased - find new maximum
				setMaxEffScoreIdx();
				return true;
			}
			return false;
		}
		// site became VACANT
		if (isVacantAt(index)) {
			if (index == maxEffScoreIdx) {
				setMaxEffScoreIdx();
				return true;
			}
			return false;
		}
		// score unchanged
		return false;
	}

	/**
	 * Find the index of the individual with the highest score.
	 */
	protected void setMaxEffScoreIdx() {
		if (maxEffScoreIdx < 0)
			return;
		maxEffScoreIdx = ArrayMath.maxIndex(scores);
	}

	/**
	 * Perform a single IBS step, i.e. update one individual for asynchronous
	 * updates or once the entire population for synchronous updates.
	 * 
	 * <h3>Discussions/extensions</h3>
	 * <ol>
	 * <li>Review migration. Exclusively used in Demes2x2. Should probably be
	 * treated as an independent event, in particular independent of population or
	 * player updates
	 * <li>Implement Wright-Fisher update.
	 * <li>How to scale realtime with multiple populations? How to define a
	 * generation if population sizes can vary?
	 * <li>{@link #updatePlayerEcology()} returns time increment in realtime units.
	 * Everyone else does fine without...
	 * </ol>
	 * 
	 * @return the number of elapsed realtime units
	 */
	public int step() {
		int[] remain;
		if (pMigration > 0.0 && random01() < pMigration) {
			// migration event
			doMigration();
			return 1;
		}
		// real time increment based on current fitness
		switch (populationUpdate.getType()) {
			case SYNC: // synchronous updates (do not commit traits)
				prepareTraits();
				if (syncFraction >= 1.0) {
					// no noise, update everyone
					for (int n = 0; n < nPopulation; n++)
						updatePlayerAt(n);
					return nPopulation;
				}
				// update only fraction of individuals (at least one but at most once)
				int nSamples = Math.max(1, (int) (syncFraction * nPopulation + 0.5));
				remain = new int[nPopulation];
				for (int n = 0; n < nPopulation; n++)
					remain[n] = n;
				for (int n = nPopulation - 1; n >= nPopulation - nSamples; n--) {
					int idx = random0n(n);
					int focal = remain[idx];
					remain[idx] = remain[n];
					updatePlayerAt(focal);
				}
				return nSamples;

			// case WRIGHT_FISHER:
			// return rincr;

			case ONCE: // asynchronous updates (every individual once)
				int nRemain = nPopulation;
				remain = new int[nRemain];
				for (int n = 0; n < nRemain; n++)
					remain[n] = n;
				while (nRemain > 0) {
					int idx = random0n(nRemain);
					int focal = remain[idx];
					remain[idx] = remain[--nRemain];
					updatePlayerAsyncAt(focal);
				}
				// last to update
				updatePlayerAsyncAt(remain[0]);
				return nPopulation;

			case ASYNC: // exclusively the current payoff matters
				updatePlayerAsync();
				return 1;

			case MORAN_BIRTHDEATH: // moran process - birth-death
				updatePlayerMoranBirthDeath();
				return 1;

			case MORAN_DEATHBIRTH: // moran process - death-birth
				updatePlayerMoranDeathBirth();
				return 1;

			case MORAN_IMITATE: // moran process - imitate
				updatePlayerMoranImitate();
				return 1;

			case ECOLOGY: // ecological updating - varying population sizes
				return updatePlayerEcology();

			default:
				logger.warning("unknown population update type (" + populationUpdate.getType().getKey() + ").");
				return -1;
		}
	}

	/**
	 * Gets the update rate of this species. Only used in multi-species modules.
	 * Determines the relative rate at which this species is picked as compared to
	 * others.
	 * 
	 * @return the species update rate
	 * 
	 * @see EvoLudo#modelNext()
	 */
	public double getSpeciesUpdateRate() {
		if (module instanceof Payoffs && minFitness > 0.0)
			return getTotalFitness();
		return getPopulationSize();
	}

	/**
	 * Perform a mutation event. The focal individual is picked uniformly at random.
	 * 
	 * @return the number of elapsed realtime units
	 */
	public int mutate() {
		mutateAt(pickFocalIndividual());
		return 1;
	}

	/**
	 * Mutate the trait of the focal individual with index {@code focal}. The
	 * mutated trait is committed and the scores updated.
	 * 
	 * @param focal the index of the focal individual
	 * @return the number of elapsed realtime units
	 */
	public abstract int mutateAt(int focal);

	/**
	 * Consider mutating the trait of the focal individual with index {@code focal}.
	 * The trait of the focal individual is stored in the array
	 * {@code traits} unless the focal individual switched trait. In that
	 * case the current trait is stored in the array {@code traitsNext}.
	 * <p>
	 * <strong>Important:</strong> The trait is not committed regardless of whether
	 * a mutation occurred.
	 * 
	 * @param focal    the index of the focal individual
	 * @param switched {@code true} if the focal individual switched trait
	 * @return {@code true} if the trait of the focal individual changed
	 */
	protected abstract boolean maybeMutateAt(int focal, boolean switched);

	/**
	 * Consider mutating the trait of the parent individual with index
	 * {@code source}. The mutated trait is committed and the scores updated.
	 * 
	 * @param source the index of the parent individual
	 * @param dest   the index of the location for the offspring placement
	 */
	protected abstract void maybeMutateMoran(int source, int dest);

	/**
	 * Update focal individual with index {@code focal} for debugging.
	 * 
	 * <h3>Notes:</h3>
	 * <ul>
	 * <li>Must remain in sync with population updates in IBS and step().</li>
	 * <li>For debugging only; time not advanced.</li>
	 * </ul>
	 *
	 * @param focal the index of the individual to update
	 */
	public void debugUpdatePopulationAt(int focal) {
		resetTraits();
		switch (populationUpdate.getType()) {
			case SYNC: // synchronous updating - gets here only in debugging mode
				debugFocal = focal;
				if (updatePlayerAt(focal))
					adjustGameScoresAt(focal);
				break;

			case ONCE: // asynchronous updates (every individual once)
			case ASYNC: // exclusively the current payoff matters
				updatePlayerAsyncAt(focal);
				break;

			case MORAN_BIRTHDEATH: // moran process - birth-death
				updatePlayerMoranBirthDeathAt(focal);
				break;

			case MORAN_DEATHBIRTH: // moran process - death-birth
				debugNModels = 0;
				updatePlayerMoranDeathBirthAt(focal);
				break;

			case MORAN_IMITATE: // moran process - imitate
				debugNModels = 0;
				updatePlayerMoranImitateAt(focal);
				break;

			case ECOLOGY: // ecological updating - varying population sizes
				debugNModels = 0;
				updatePlayerEcologyAt(focal);
				break;

			default:
				logger.warning("unknown population update type (" + populationUpdate.getType().getKey() + ").");
				return;
		}
		debugMarkChange();
		engine.fireModelChanged();
	}

	/**
	 * Helper variable to store index of focal individual (birth) that got
	 * updated during debug step.
	 */
	protected int debugFocal = -1;

	/**
	 * Helper flag to indicate an actual trait change during debug step.
	 */
	protected boolean debugSame = true;

	/**
	 * Helper variable to store index of target individual (death) that got
	 * updated during debug step.
	 */
	protected int debugModel = -1;

	/**
	 * Helper variable to store array of indices of individual that served as models
	 * during debug step.
	 */
	protected int[] debugModels = null;

	/**
	 * Helper variable to number of individual that served as models during debug
	 * step.
	 */
	protected int debugNModels = -1;

	/**
	 * Override in subclass for example to mark those individuals in the GUI that
	 * were involved in the debug step.
	 */
	protected void debugMarkChange() {
		// // <jf
		// logger.fine("update: " + getTraitNameAt(me) + " " + me + " (" +
		// getScoreAt(me) + ") adopts "
		// + getTraitNameAt(you) + " " + you + " (" + getScoreAt(you) + ")");
		// // jf>
		if (logger.isLoggable(Level.FINE) && debugFocal >= 0) {
			StringBuilder sb = new StringBuilder();
			sb.append("focal:").append(formatInfoAt(debugFocal, -1)).append(", ");
			switch (debugNModels) {
				case -1:
				case 0:
					if (debugModel >= 0)
						sb.append("target:").append(formatInfoAt(debugModel, -1));
					break;
				case 1:
					int idx = debugModels[0];
					sb.append("model:").append(formatInfoAt(idx, debugModel));
					break;
				default:
					sb.append("models:");
					for (int n = 0; n < debugNModels - 1; n++) {
						idx = debugModels[n];
						sb.append(formatInfoAt(idx, debugModel)).append(',');
					}
					sb.append(formatInfoAt(debugModels[debugNModels - 1], debugModel));
			}
			if (!debugSame)
				sb.append(" changed");
			logger.fine(sb.toString());
		}
	}

	/**
	 * Helper method to format information about updating the focal individual at
	 * index {@code focal} using the model individual with index {@code model}.
	 * 
	 * @param focal the index of the focal individual
	 * @param model the index of the model individual
	 * @return the formatted information
	 */
	private String formatInfoAt(int focal, int model) {
		return " " + focal + (focal == model ? "* (" : " (") + getTraitNameAt(focal) + ", " + getScoreAt(focal) + ")";
	}

	/**
	 * Perform a single, asynchronous update of the trait of a randomly selected
	 * individual.
	 */
	protected void updatePlayerAsync() {
		updatePlayerAsyncAt(pickFocalIndividual());
	}

	/**
	 * Update the trait of the focal individual with index {@code me}.
	 * 
	 * @param me the index of the focal individual
	 */
	public void updatePlayerAsyncAt(int me) {
		boolean switched = updatePlayerAt(me);
		if (module instanceof Payoffs)
			updateScoreAt(me, switched);
		else if (switched)
			commitTraitAt(me);
	}

	/**
	 * Update the scores of the focal individual with index {@code me}.
	 * 
	 * @param me       the index of the focal individual
	 * @param switched {@code true} if the focal switched trait
	 */
	protected void updateScoreAt(int me, boolean switched) {
		if (adjustScores) {
			// player switched trait - adjust scores, commit trait
			if (switched)
				adjustGameScoresAt(me);
			return;
		}
		if (switched)
			commitTraitAt(me);
		// no need to update ephemeral scores
		if (playerScoring.equals(ScoringType.EPHEMERAL))
			return;
		if (switched || playerScoring.equals(ScoringType.RESET_ALWAYS))
			resetScoreAt(me);
		// let me play single game
		playGameAt(me);
	}

	/**
	 * Perform a single, Moran (Birth-death) update for a random individual selected
	 * with a probability proportional to fitness. This is the original Moran
	 * process where the offspring can replace the parent in well-mixed populations.
	 * For structured populations this corresponds to the spatial Moran process with
	 * <em>Bd</em> updating in Ohtsuki et al. Nature 2005.
	 * 
	 * @see <a href="http://dx.doi.org/10.1038/nature04605">Ohtsuki, H., Hauert,
	 *      C., Lieberman, E. &amp; Nowak, M. (2006) A simple rule for the evolution
	 *      of cooperation on graphs, Nature 441, 502-505</a>
	 */
	public void updatePlayerMoranBirthDeath() {
		updatePlayerMoranBirthDeathAt(pickFitFocalIndividual());
	}

	/**
	 * Perform a Moran (Birth-death) update for the focal individual with index
	 * {@code parent} to produce a clonal offspring and replace one of the parent's
	 * neighbours selected uniformly at random. This is the original Moran process
	 * where the offspring can replace the parent in well-mixed populations.
	 * 
	 * @param parent the index of the parent
	 */
	protected void updatePlayerMoranBirthDeathAt(int parent) {
		debugFocal = parent;
		if (competition.getType() == Geometry.Type.MEANFIELD) {
			debugModel = pickFocalIndividual();
			debugNModels = 0;
		} else
			debugModel = pickNeighborSiteAt(parent);
		if (debugModel < 0)
			return; // parent has no outgoing-neighbors (sink)
		// note: do not return prematurely if <code>debugModel==debugFocal</code>
		// because
		// this would preclude mutations and fail to reset scores.
		maybeMutateMoran(debugFocal, debugModel);
	}

	/**
	 * Perform a single Moran (death-Birth) update for a site selected uniformly at
	 * random. This corresponds to the spatial Moran process with <em>dB</em>
	 * updating in Ohtsuki et al. Nature 2005.
	 * 
	 * @see <a href="http://dx.doi.org/10.1038/nature04605">Ohtsuki, H., Hauert,
	 *      C., Lieberman, E. &amp; Nowak, M. (2006) A simple rule for the evolution
	 *      of cooperation on graphs, Nature 441, 502-505</a>
	 */
	public void updatePlayerMoranDeathBirth() {
		updatePlayerMoranDeathBirthAt(pickFocalIndividual());
	}

	/**
	 * Perform a single Moran (death-Birth) update for the focal site with index
	 * {@code vacant}. One of its (incoming) neighbours succeeds, with a probability
	 * proportional to its fitness, in producing a clonal offspring and placing it
	 * at site {@code vacant}.
	 * 
	 * @param vacant the index of the vacant site
	 */
	protected void updatePlayerMoranDeathBirthAt(int vacant) {
		debugFocal = vacant;
		debugModel = pickFitNeighborAt(vacant);
		if (debugModel < 0)
			return; // vacant has no incoming-neighbors (source)
		maybeMutateMoran(debugModel, debugFocal);
	}

	/**
	 * Perform a single, Moran (imitate) update for a site selected uniformly at
	 * random. This corresponds to <em>imitate</em> in Ohtsuki et al.
	 * Nature 2005.
	 * 
	 * @see <a href="http://dx.doi.org/10.1038/nature04605">Ohtsuki, H., Hauert,
	 *      C., Lieberman, E. &amp; Nowak, M. (2006) A simple rule for the evolution
	 *      of cooperation on graphs, Nature 441, 502-505</a>
	 */
	public void updatePlayerMoranImitate() {
		updatePlayerMoranImitateAt(pickFocalIndividual());
	}

	/**
	 * Update the focal individual with index {@code imitator} by comparing its own
	 * payoff and those of its neighbors. The focal individual imitates the trait
	 * of a neighbour (or keeps its own) with a probability proportional to the
	 * corresponding individual's fitness (including its own).
	 *
	 * @param imitator the index of the individual that reassesses its trait
	 */
	protected void updatePlayerMoranImitateAt(int imitator) {
		debugFocal = imitator;
		debugModel = pickFitNeighborAt(imitator, true);
		if (debugModel < 0)
			return; // vacant has no incoming-neighbors (source)
		maybeMutateMoran(debugModel, debugFocal);
	}

	/**
	 * Perform a single Moran update for the reproducing node with index
	 * {@code source} and the node that gets replaced with index {@code dest}. The
	 * three Moran variants (death-Birth, Birth-death and imitate) differ only in
	 * their selection of the individuals {@code source} and {@code dest} and then
	 * call this method, {@code migrateMoran(int, int)}.
	 * <p>
	 * <strong>Note:</strong> Moran optimizations for discrete trait require
	 * access to this method.
	 *
	 * @param source the index of the parent node
	 * @param dest   the index of the node where the offspring is placed
	 * 
	 * @see IBSDPopulation
	 */
	protected void migrateMoran(int source, int dest) {
		if (adjustScores) {
			if (haveSameTrait(source, dest))
				return;
			updateFromModelAt(dest, source);
			adjustGameScoresAt(dest);
			return;
		}
		if (!haveSameTrait(source, dest)) {
			// replace 'vacant'
			updateFromModelAt(dest, source);
			resetScoreAt(dest);
			commitTraitAt(dest);
			playGameAt(dest);
			return;
		}
		// no actual trait change occurred - reset score always (default) or only on
		// actual change?
		if (playerScoring.equals(ScoringType.RESET_ALWAYS))
			resetScoreAt(dest);
		playGameAt(dest);
	}

	/**
	 * Perform a single ecological update of an individual selected uniformly at
	 * random.
	 * 
	 * @return the number of elapsed realtime units
	 */
	public int updatePlayerEcology() {
		return updatePlayerEcologyAt(pickFocalIndividual());
	}

	/**
	 * Perform a single ecological update of the site with index {@code index}.
	 * <p>
	 * <strong>Important:</strong> No default implementation is possible. Modules
	 * with ecological updates need to subclass {@code IBSPopulation} and provide
	 * their own implementations.
	 *
	 * @param index the index of the focal site
	 * @return the number of elapsed realtime units
	 */
	protected int updatePlayerEcologyAt(int index) {
		throw new Error("updatePlayerEcologyAt not implemented.");
	}

	/**
	 * Perform a single update of the individual with index {@code me}. Returns
	 * {@code true} if the individual adopted the trait of the reference individual.
	 * Does not imply that the trait changed in discrete modules. Whether the
	 * individuals score is reset depends on {@link #playerScoring}
	 *
	 * @param me the index of the focal individual
	 * @return {@code true} if trait of reference adopted
	 * 
	 * @see #resetScoreAt(int)
	 */
	public boolean updatePlayerAt(int me) {
		debugFocal = me;
		// note: choose random neighbor among in-neighbors (those are upstream to serve
		// as models; this is the opposite for birth-death scenarios)
		compGroup.pickAt(me, false);
		debugNModels = compGroup.nSampled;
		debugModels = compGroup.group;
		debugModel = -1;
		if (playerScoring.equals(ScoringType.EPHEMERAL)) {
			// calculate scores of all individual involved in updating
			playGameAt(me);
			for (int i = 0; i < debugNModels; i++)
				playGameAt(debugModels[i]);
		}
		return updatePlayerAt(me, debugModels, debugNModels);
	}

	/**
	 * Perform a single update of the individual with index {@code me} using the
	 * {@code rGroupSize} models in the array {@code refGroup}. Returns
	 * {@code true} if the individual changed its trait to signal that the
	 * focal individual's score will need to be reset.
	 * 
	 * @param me         the index of the focal individual
	 * @param refGroup   the group of reference individuals
	 * @param rGroupSize the number of reference individuals
	 * @return {@code true} if trait of reference adopted
	 * 
	 * @see #resetScoreAt(int)
	 */
	public boolean updatePlayerAt(int me, int[] refGroup, int rGroupSize) {
		if (rGroupSize <= 0)
			return false;

		boolean switched;
		switch (playerUpdate.getType()) {
			case BEST_RESPONSE: // best-response update
				// this makes little sense for continuous traits - should not happen...
				// takes entire population (mean-field) or entire neighborhood into account.
				// for details check updatePlayerBestReply() in DPopulation.java
				switched = updatePlayerBestResponse(me, refGroup, rGroupSize);
				break;

			case BEST: // best update
				switched = updatePlayerBest(me, refGroup, rGroupSize);
				break;

			case BEST_RANDOM: // best update - equal payoffs 50% chance to switch
				switched = updatePlayerBestHalf(me, refGroup, rGroupSize);
				break;

			case PROPORTIONAL: // proportional update
				switched = updateProportionalAbs(me, refGroup, rGroupSize);
				break;

			case IMITATE_BETTER: // imitation update (better traits only)
				switched = updateReplicatorPlus(me, refGroup, rGroupSize);
				break;

			case IMITATE: // imitation update
				switched = updateReplicatorHalf(me, refGroup, rGroupSize);
				break;

			case THERMAL: // fermi update
				switched = updateThermal(me, refGroup, rGroupSize);
				break;

			default:
				throw new Error("Unknown update method for players (" + playerUpdate + ")");
		}
		if (maybeMutateAt(me, switched))
			return true;
		if (playerScoring.equals(ScoringType.RESET_ON_CHANGE))
			// signal change only if actual change of trait occurred
			return switched && !isSameTrait(me);
		return switched;
	}

	/**
	 * Updates the focal individual with index {@code me} by adopting the trait
	 * of the best performing reference individual among the {@code rGroupSize}
	 * models in the array {@code refGroup}. Returns {@code true} if the individual
	 * adopted the trait of the reference individual. Does not imply that the trait
	 * changed in discrete modules. Whether the individuals score is reset depends
	 * on {@link #playerScoring}.
	 * 
	 * <h3>Notes:</h3>
	 * <ol>
	 * <li>If the scores of two reference individuals tie but exceed the focal
	 * individual's score, expert advice is needed.
	 * <li>If the focal individual's score is highest but ties with one or more
	 * reference individuals the focal individual keeps its trait.
	 * <li>For the best update it does not matter whether scores are averaged or
	 * accumulated.
	 * </ol>
	 * 
	 * @param me         the index of the focal individual
	 * @param refGroup   the group of reference individuals
	 * @param rGroupSize the number of reference individuals
	 * @return {@code true} if trait of reference adopted
	 * 
	 * @see #preferredPlayerBest(int, int, int)
	 * @see #resetScoreAt(int)
	 */
	protected boolean updatePlayerBest(int me, int[] refGroup, int rGroupSize) {

		// neutral case: no one is better -> nothing happens
		if (isNeutral)
			return false;

		int bestPlayer = me;
		// if the individual's score is highest but ties with a reference, the
		// individual sticks to it's trait.
		double bestScore = getFitnessAt(me) + 1e-8;
		boolean switched = false;

		for (int i = 0; i < rGroupSize; i++) {
			int aPlayer = refGroup[i];
			double aScore = getFitnessAt(aPlayer);
			double bScore = aScore;
			if (Math.abs(bestScore - bScore) < 1e-8) {
				// we need some expert advice on which trait is the preferred one
				// does 'me' prefer 'aPlayer' over 'bestPlayer'?
				if (preferredPlayerBest(me, bestPlayer, aPlayer))
					bScore += 1e-8;
				else
					bScore -= 1e-8;
			}
			if (bestScore > bScore)
				continue;
			bestScore = aScore;
			bestPlayer = aPlayer;
			switched = true;
		}
		if (!switched)
			return false;
		updateFromModelAt(me, bestPlayer);
		return true;
	}

	/**
	 * Updates the focal individual with index {@code me} by adopting the trait
	 * of the best performing reference individual among the the {@code rGroupSize}
	 * models in the array {@code refGroup}. If the scores of two (or more)
	 * references or the score of the focal individual and one (or more)
	 * reference(s) tie, then a coin toss decides which trait to keep/adopt.
	 * Returns {@code true} if the individual adopted the trait of the reference
	 * individual. Does not imply that the trait changed in discrete modules.
	 * Whether the individuals score is reset depends on
	 * {@link #playerScoring}.
	 * 
	 * <h3>Notes:</h3>
	 * For the best update it does not matter whether scores are averaged or
	 * accumulated.
	 * 
	 * @param me         the index of the focal individual
	 * @param refGroup   the group of reference individuals
	 * @param rGroupSize the number of reference individuals
	 * @return {@code true} if trait of reference adopted
	 * 
	 * @see #resetScoreAt(int)
	 */
	protected boolean updatePlayerBestHalf(int me, int[] refGroup, int rGroupSize) {
		int bestPlayer = me;
		double bestScore = getFitnessAt(me);
		boolean switched = false;

		for (int i = 0; i < rGroupSize; i++) {
			int aPlayer = refGroup[i];
			double aScore = getFitnessAt(aPlayer);
			if (aScore > bestScore) {
				bestScore = aScore;
				bestPlayer = aPlayer;
				switched = true;
				continue;
			}
			if (Math.abs(aScore - bestScore) < 1e-8 && random01() < 0.5) {
				// equal scores - switch with probability 50%
				bestPlayer = aPlayer;
				switched = true;
			}
		}
		if (!switched)
			return false;
		updateFromModelAt(me, bestPlayer);
		return true;
	}

	/**
	 * Updates the focal individual with index {@code me} by adopting the trait
	 * of one reference individual (including itself) among the the
	 * {@code rGroupSize} models in the array {@code refGroup} with a probability
	 * proportional to their scores. Returns {@code true} if the individual adopted
	 * the trait of the reference individual. Does not imply that the trait changed
	 * in discrete modules. Whether the individuals score is reset depends on
	 * {@link #playerScoring}.
	 *
	 * @param me         the index of the focal individual
	 * @param refGroup   the group of reference individuals
	 * @param rGroupSize the number of reference individuals
	 * @return {@code true} if trait of reference adopted
	 * 
	 * @see #resetScoreAt(int)
	 */
	protected boolean updateProportionalAbs(int me, int[] refGroup, int rGroupSize) {
		// neutral case: choose random neighbor or individual itself
		if (isNeutral) {
			int hit = random0n(rGroupSize + 1);
			if (hit == rGroupSize)
				return false;
			updateFromModelAt(me, refGroup[hit]);
			return true;
		}

		double myFitness = getFitnessAt(me) - minFitness;
		double totFitness = myFitness;
		for (int i = 0; i < rGroupSize; i++) {
			double aScore = getFitnessAt(refGroup[i]) - minFitness;
			groupScores[i] = aScore;
			totFitness += aScore;
		}
		if (totFitness <= 0.0) { // everybody has the minimal score - pick at random
			int hit = random0n(rGroupSize + 1);
			if (hit == rGroupSize)
				return false;
			updateFromModelAt(me, refGroup[hit]);
			return true;
		}

		double choice = random01() * totFitness;
		double bin = myFitness;
		if (choice <= bin)
			return false; // individual keeps its place

		choice -= bin;
		for (int i = 0; i < rGroupSize; i++) {
			bin = groupScores[i];
			if (choice <= bin) {
				updateFromModelAt(me, refGroup[i]);
				return true;
			}
			choice -= bin;
		}
		// should not get here!
		throw new Error("Problem in updateProportionalAbs()...");
	}

	/**
	 * Updates the focal individual with index {@code me} by adopting the trait
	 * of one reference individual among the the {@code rGroupSize} models in the
	 * array {@code refGroup}. The focal individual \(i\) imitates the trait of a
	 * <em>better</em> performing reference individual \(j\) with a probability
	 * proportional to the difference in fitness:
	 * \[p_{i\to j} = \frac{(f_i-f_j)_+}\alpha,\]
	 * where \(\alpha\) denotes a normalization factor to ensure \(p_{i\to
	 * j}\in[0,1]\). Returns {@code true} if the individual adopted the trait
	 * of the reference individual. Does not imply that the trait changed in
	 * discrete modules. Whether the individuals score is reset depends on
	 * {@link #playerScoring}.
	 * 
	 * @param me         the index of the focal individual
	 * @param refGroup   the group of reference individuals
	 * @param rGroupSize the number of reference individuals
	 * @return {@code true} if trait of reference adopted
	 * 
	 * @see #resetScoreAt(int)
	 */
	protected boolean updateReplicatorPlus(int me, int[] refGroup, int rGroupSize) {
		return updateReplicator(me, refGroup, rGroupSize, true);
	}

	/**
	 * Updates the focal individual with index {@code me} by adopting the trait
	 * of one reference individual among the the {@code rGroupSize} models in the
	 * array {@code refGroup}. The focal individual \(i\) imitates the trait of a
	 * reference individual \(j\) with a probability proportional to the difference
	 * in fitness:
	 * \[p_{i\to j} = \frac{f_i-f_j}\alpha\]
	 * where \(\alpha\) denotes a normalization factor to ensure \(p_{i\to
	 * j}\in[0,1]\). This corresponds to the microscopic updating which recovers the
	 * standard replicator equation in the continuum limit. Returns {@code true} if
	 * the individual adopted the trait of the reference individual. Does not imply
	 * that the trait changed in discrete modules. Whether the individuals score is
	 * reset depends on {@link #playerScoring}.
	 * 
	 * @param me         the index of the focal individual
	 * @param refGroup   the group of reference individuals
	 * @param rGroupSize the number of reference individuals
	 * @return {@code true} if trait of reference adopted
	 * 
	 * @see #resetScoreAt(int)
	 */
	protected boolean updateReplicatorHalf(int me, int[] refGroup, int rGroupSize) {
		return updateReplicator(me, refGroup, rGroupSize, false);
	}

	/**
	 * Helper method for replicator type updates. Returns {@code true} if the
	 * individual adopted the trait of the reference individual. Does not imply that
	 * the trait changed in discrete modules. Whether the individuals score is reset
	 * depends on {@link #playerScoring}.
	 * 
	 * @param me         the index of the focal individual
	 * @param refGroup   the group of reference individuals
	 * @param rGroupSize the number of reference individuals
	 * @param betterOnly the flag to indicate whether only better performing
	 *                   reference individuals are considered
	 * @return {@code true} if trait of reference adopted
	 * 
	 * @see #updateReplicatorPlus(int, int[], int)
	 * @see #updateReplicatorHalf(int, int[], int)
	 */
	private boolean updateReplicator(int me, int[] refGroup, int rGroupSize, boolean betterOnly) {
		// neutral case
		if (isNeutral) {
			// return if betterOnly because no one is better
			if (betterOnly)
				return false;
			// choose random neighbor or individual itself
			int hit = random0n(rGroupSize + 1);
			if (hit == rGroupSize)
				return false;
			updateFromModelAt(me, refGroup[hit]);
			return true;
		}

		double myFitness = getFitnessAt(me);
		double aProb;
		double nProb;
		double norm;
		double noise = playerUpdate.getNoise();
		double error = playerUpdate.getError();
		double equalProb = betterOnly ? error : 0.5;
		// generalize update to competition among arbitrary numbers of players
		if (noise <= 0.0) { // zero noise
			double aDiff = getFitnessAt(refGroup[0]) - myFitness;
			if (aDiff > 0.0)
				aProb = 1.0 - error;
			else
				aProb = (aDiff < 0.0 ? error : equalProb);
			norm = aProb;
			nProb = 1.0 - aProb;
			if (rGroupSize > 1) {
				cProbs[0] = aProb;
				for (int i = 1; i < rGroupSize; i++) {
					aDiff = getFitnessAt(refGroup[i]) - myFitness;
					if (aDiff > 0.0)
						aProb = 1.0 - error;
					else
						aProb = (aDiff < 0.0 ? error : equalProb);
					cProbs[i] = cProbs[i - 1] + aProb;
					nProb *= 1.0 - aProb;
					norm += aProb;
				}
			}
		} else { // some noise
			double inoise = 1.0 / noise;
			double shift = 0.0;
			if (!betterOnly) {
				inoise *= 0.5;
				shift = 0.5;
			}
			if (playerScoreAveraged || adjustScores || playerScoring.equals(ScoringType.EPHEMERAL)) {
				inoise /= (maxFitness - minFitness);
				// generalize update to competition among arbitrary numbers of players
				aProb = Math.min(1.0 - error,
						Math.max(error, (getFitnessAt(refGroup[0]) - myFitness) * inoise + shift));
				norm = aProb;
				nProb = 1.0 - aProb;
				if (rGroupSize > 1) {
					cProbs[0] = aProb;
					for (int i = 1; i < rGroupSize; i++) {
						aProb = Math.min(1.0 - error,
								Math.max(error, (getFitnessAt(refGroup[i]) - myFitness) * inoise + shift));
						cProbs[i] = cProbs[i - 1] + aProb;
						nProb *= 1.0 - aProb;
						norm += aProb;
					}
				}
			} else {
				// not ready for unbounded accumulated payoffs... check() should catch this
				throw new Error("cannot handle unbounded accumulated scores");
			}
		}
		if (norm <= 0.0)
			return false;

		double choice = random01();
		if (choice >= 1.0 - nProb)
			return false;

		// optimization
		if (rGroupSize == 1) {
			updateFromModelAt(me, refGroup[0]);
			return true;
		}

		norm = (1.0 - nProb) / norm;
		for (int i = 0; i < rGroupSize; i++) {
			// normalize cumulative probabilities only if and when needed
			if (choice < cProbs[i] * norm) {
				updateFromModelAt(me, refGroup[i]);
				return true;
			}
		}
		/* should not get here! */
		throw new Error("Problem in " + (betterOnly ? "updateReplicatorPlus()..." : "updateReplicatorHalf()..."));
	}

	/**
	 * Updates the focal individual with index {@code me} by adopting the trait
	 * of one reference individual among the the {@code rGroupSize} models in the
	 * array {@code refGroup}. The focal individual \(i\) imitates the trait of a
	 * reference individual \(j\) with a probability proportional to the difference
	 * in fitness:
	 * \[p_{i\to j} = \frac1{1+\exp(-(f_i-f_j)/T)}\]
	 * where \(T\) denotes the temperature (or noise) in adopting a trait. In the
	 * limit \(T\to\infty\) imitation reduces to a coin toss, \(p_{i\to j}=1/2\). In
	 * contrast, for \(T\to 0\) it converges to the step-function, with
	 * \(\Theta(x)=1\) for positive \(x\), \(\Theta(x)=0\) for \(x\) negative and
	 * \(\Theta(0)=1/2\). Returns {@code true} if the individual adopted the trait
	 * of the reference individual. Does not imply that the trait changed in
	 * discrete modules. Whether the individuals score is reset depends on
	 * {@link #playerScoring}.
	 *
	 * @param me         the index of the focal individual
	 * @param refGroup   the group of reference individuals
	 * @param rGroupSize the number of reference individuals
	 * @return {@code true} if trait of reference adopted
	 * 
	 * @see #resetScoreAt(int)
	 */
	protected boolean updateThermal(int me, int[] refGroup, int rGroupSize) {
		// neutral case: choose random neighbor or individual itself
		if (isNeutral) {
			int hit = random0n(rGroupSize + 1);
			if (hit == rGroupSize)
				return false;
			updateFromModelAt(me, refGroup[hit]);
			return true;
		}

		double myFitness = getFitnessAt(me);
		double aProb;
		double nProb;
		double norm;
		double noise = playerUpdate.getNoise();
		double error = playerUpdate.getError();
		// generalize update to competition among arbitrary numbers of players
		if (noise <= 0.0) { // zero noise
			double aDiff = getFitnessAt(refGroup[0]) - myFitness;
			if (aDiff > 0.0)
				aProb = 1.0 - error;
			else
				aProb = (aDiff < 0.0 ? error : 0.5);
			norm = aProb;
			nProb = 1.0 - aProb;
			if (rGroupSize > 1) {
				cProbs[0] = aProb;
				for (int i = 1; i < rGroupSize; i++) {
					aDiff = getFitnessAt(refGroup[i]) - myFitness;
					if (aDiff > 0)
						aProb = 1.0 - error;
					else
						aProb = (aDiff < 0.0 ? error : 0.5);
					cProbs[i] = cProbs[i - 1] + aProb;
					nProb *= 1.0 - aProb;
					norm += aProb;
				}
			}
		} else { // some noise
			double inoise = 1.0 / noise;
			// the increased accuracy of {@code Math.expm1(x)} for {@code x} near {@code 0}
			// is not so
			// important but hopefully this also means the accuracy is more symmetrical for
			// {@code x}
			// and {@code 1/x}
			aProb = Math.min(1.0 - error, Math.max(error,
					1.0 / (2.0 + Math.expm1(-(getFitnessAt(refGroup[0]) - myFitness) * inoise))));
			norm = aProb;
			nProb = 1.0 - aProb;
			if (rGroupSize > 1) {
				cProbs[0] = aProb;
				for (int i = 1; i < rGroupSize; i++) {
					aProb = Math.min(1.0 - error, Math.max(error,
							1.0 / (2.0 + Math.expm1(-(getFitnessAt(refGroup[i]) - myFitness) * inoise))));
					cProbs[i] = cProbs[i - 1] + aProb;
					nProb *= 1.0 - aProb;
					norm += aProb;
				}
			}
		}
		if (norm <= 0.0)
			return false;

		double choice = random01();
		if (choice >= 1.0 - nProb)
			return false;

		// optimization
		if (rGroupSize == 1) {
			updateFromModelAt(me, refGroup[0]);
			return true;
		}

		norm = (1.0 - nProb) / norm;
		for (int i = 0; i < rGroupSize; i++) {
			// normalize cumulative probabilities only if and when needed
			if (choice < cProbs[i] * norm) {
				updateFromModelAt(me, refGroup[i]);
				return true;
			}
		}
		// should not get here!
		if (logger.isLoggable(Level.FINE)) {
			StringBuilder sb = new StringBuilder();
			sb.append("Report: myScore=").append(myFitness)
					.append(", nProb=").append(nProb)
					.append(", norm=").append(norm)
					.append(", choice=").append(choice)
					.append("\nCumulative probabilities: ");
			for (int i = 0; i < rGroupSize; i++)
				sb.append(cProbs[i]).append('\t');
			logger.fine(sb.toString());
		}
		throw new Error("Problem in updateThermal()...");
	}

	/**
	 * Returns the minimum score {@code min} in this population, taking the
	 * population structure and payoff accounting into account.
	 * 
	 * @return the processed minimum score
	 */
	public double getMinScore() {
		double min = ((Payoffs) module).getMinPayoff();
		if (playerScoreAveraged)
			return min;
		// accumulated payoffs
		return processScore(min, (min < 0.0 ? interaction.maxOut : interaction.minOut));
	}

	/**
	 * Returns the maximum score {@code min} in this population, taking the
	 * population structure and payoff accounting into account.
	 * 
	 * @return the processed maximum score
	 */
	public double getMaxScore() {
		double max = ((Payoffs) module).getMaxPayoff();
		if (playerScoreAveraged)
			return max;
		// accumulated payoffs
		return processScore(max, (max < 0.0 ? interaction.minOut : interaction.maxOut));
	}

	/**
	 * Process the accumulated {@code score} in this population, taking the
	 * updating into account. In heterogeneous networks {@code count} must refer to
	 * the highest degree for maximum scores and the lowest degree for minimum
	 * scores.
	 * 
	 * @param score the minimum or maximum score
	 * @param count the number of interactions for the score
	 * @return the processed extremal score
	 */
	protected double processScore(double score, int count) {
		if (adjustScores) {
			// getMinGameScore in module must deal with structure and games
			if (module.isPairwise()) {
				// count == 0 for well-mixed populations, with vacancies, at most
				// nPopulation-1 interactions
				if (count == 0)
					return 2 * (nPopulation - 1) * score;
				return 2 * count * score;
			}
			// each individual participates in at most nGroup interactions
			return module.getNGroup() * score;
		}
		if (playerScoring == ScoringType.EPHEMERAL) {
			if (module.isPairwise()) {
				if (interGroup.samplingType == SamplingType.RANDOM)
					return interGroup.nSamples * score;
				if (count == 0)
					return (nPopulation - 1) * score;
				return count * score;
			}
			// each individual participates in a single group interaction
			return score;
		}
		// not ready for unbounded accumulated payoffs... check() should catch this
		throw new Error("cannot handle unbounded accumulated scores");
	}

	/**
	 * Retrieve and store extremal scores and fitnesses to reduce calls to
	 * {@link Module}.
	 */
	public void updateMinMaxScores() {
		if (!(module instanceof Payoffs))
			return;
		minScore = getMinScore();
		maxScore = getMaxScore();
		isNeutral = ((Payoffs) module).isNeutral();
		minFitness = map2fit.map(minScore);
		maxFitness = map2fit.map(maxScore);
	}

	/**
	 * Check all model parameters for consistency and adjust if necessary (and
	 * feasible). Returns {@code true} if adjustments require a reset. Free memory
	 * if possible and request a reset if new memory needs to be allocated.
	 * 
	 * @return {@code true} if reset is required
	 * 
	 * @see Model#check()
	 * @see Model#reset()
	 * @see #reset()
	 */
	public boolean check() {
		staticmodule = module.isStatic() ? (Features.Static) module : null;
		int ot = nTraits;
		nTraits = module.getNTraits();
		boolean doReset = (ot != nTraits);
		nPopulation = module.getNPopulation();
		map2fit = module.getMap2Fitness();
		playerUpdate = module.getPlayerUpdate();

		// check geometries: --geometry set structure, --geominter set interaction and
		// --geomcomp set competition.
		// now it is time to amalgamate. the more specific options --geominter,
		// --geomcomp take precedence. structure
		// is always available from parsing the default of cloGeometry. hence first
		// check --geominter, --geomcomp.
		IBS master = (IBS) engine.getModel();
		Geometry structure = module.getGeometry();
		if (interaction != null) {
			if (competition != null) {
				// NOTE: this is hackish because every population has its own cloGeometry
				// parsers but only one will actually do the parsing...
				if (structure != null) {
					// --geometry was provided on command line
					if (!interaction.equals(structure) && !master.cloGeometryInteraction.isSet()) {
						interaction = structure;
						doReset = true;
					}
					if (!competition.equals(structure) && !master.cloGeometryCompetition.isSet()) {
						competition = structure;
						doReset = true;
					}
					interaction.interCompSame = competition.interCompSame = interaction.equals(competition);
				}
				// both geometries set on command line OR parameters manually changed and
				// applied - check if can be collapsed
				// NOTE: assumes that same arguments imply same geometries. this precludes that
				// interaction and competition are both random
				// structures with otherwise identical parameters (e.g. random regular graphs of
				// same degree but different realizations)
				if (!interaction.isValid || !competition.isValid) {
					interaction.interCompSame = competition.interCompSame = master.cloGeometryInteraction.getArg()
							.equals(master.cloGeometryCompetition.getArg());
				}
			} else {
				// competition not set - use --geometry (or its default for competition)
				interaction.interCompSame = master.cloGeometryInteraction.getArg().equals(module.cloGeometry.getArg());
				if (!interaction.interCompSame) {
					competition = structure;
					competition.interCompSame = false;
				}
			}
		} else {
			// interaction not set
			if (competition != null) {
				// competition set - use --geometry (or its default for interaction)
				// NOTE: this is slightly different from above because competition knows
				// nothing about potentially different opponents in
				// inter-species interactions.
				interaction = structure;
				interaction.interCompSame = master.cloGeometryCompetition.getArg()
						.equals(module.cloGeometry.getArg());
			} else {
				// neither geometry is set - e.g. initial launch with --geometry specified (or
				// no geometry specifications at all)
				interaction = structure;
				interaction.interCompSame = true;
			}
		}
		// make sure competiton geometry is set
		if (competition == null)
			competition = interaction;
		interGroup.setGeometry(interaction);
		compGroup.setGeometry(competition);
		// set adding of links to geometries
		if (pAddwire != null) {
			double prev = interaction.pAddwire;
			interaction.pAddwire = pAddwire[0];
			doReset |= (Math.abs(prev - interaction.pAddwire) > 1e-8);
			if (competition != null) {
				prev = competition.pAddwire;
				competition.pAddwire = pAddwire[1];
				doReset |= (Math.abs(prev - competition.pAddwire) > 1e-8);
			}
		}
		// set rewiring of links in geometries
		if (pRewire != null) {
			double prev = interaction.pRewire;
			interaction.pRewire = pRewire[0];
			doReset |= (Math.abs(prev - interaction.pRewire) > 1e-8);
			if (competition != null) {
				prev = competition.pRewire;
				competition.pRewire = pRewire[1];
				doReset |= (Math.abs(prev - competition.pRewire) > 1e-8);
			}
		}
		// set names of geometries
		String name = module.getName();
		if (interaction.interCompSame) {
			if (name.isEmpty())
				interaction.name = "Structure";
			else
				interaction.name = name + ": Structure";
		} else {
			if (Geometry.displayUniqueGeometry(interaction, competition)) {
				if (name.isEmpty())
					interaction.name = competition.name = "Structure";
				else
					interaction.name = competition.name = name + ": Structure";
			} else {
				if (name.isEmpty()) {
					interaction.name = "Interaction";
					competition.name = "Competition";
				} else {
					interaction.name = name + ": Interaction";
					competition.name = name + ": Competition";
				}
			}
		}
		// Note: now that interaction and competition are set, we still cannot set
		// structure to null because of subsequent CLO parsing

		// check geometries
		// Warning: there is a small chance that the interaction and competition
		// geometries require different population sizes, which does not make sense
		// and would most likely result in a never ending initialization loop...
		interaction.size = nPopulation;
		doReset |= interaction.check();
		if (competition != null) {
			module.setNPopulation(interaction.size);
			nPopulation = interaction.size; // keep local copy in sync
			competition.size = nPopulation;
			doReset |= competition.check();
			if (competition.size != nPopulation) {
				// try checking interaction geometry again
				interaction.size = competition.size;
				if (interaction.check())
					logger.severe("incompatible interaction and competition geometries!");
			}
		}
		// population structure may require special population sizes
		module.setNPopulation(interaction.size);
		nPopulation = interaction.size; // keep local copy in sync

		// check sampling in special geometries
		int nGroup = module.getNGroup();
		if (interaction.getType() == Geometry.Type.SQUARE && interaction.isRegular && interaction.connectivity > 8 &&
				interGroup.isSampling(IBSGroup.SamplingType.ALL) && nGroup > 2 && nGroup < 9) {
			// if count > 8 then the interaction pattern Group.SAMPLING_ALL with a group
			// size between 2 and 8
			// (excluding boundaries is not allowed because this pattern requires a
			// particular (internal)
			// arrangement of the neighbors.
			interGroup.setSampling(IBSGroup.SamplingType.RANDOM);
			if (logger.isLoggable(Level.WARNING))
				logger.warning(
						"square " + name + " geometry has incompatible interaction pattern and neighborhood size" +
								" - using random sampling of interaction partners!");
		}
		if (interaction.getType() == Geometry.Type.CUBE && interGroup.isSampling(IBSGroup.SamplingType.ALL) &&
				nGroup > 2 && nGroup <= interaction.connectivity) {
			// Group.SAMPLING_ALL only works with pairwise interactions or all neighbors;
			// restrictions do not apply for PDE's
			interGroup.setSampling(IBSGroup.SamplingType.RANDOM);
			if (logger.isLoggable(Level.WARNING))
				logger.warning(
						"cubic " + name + " geometry has incompatible interaction pattern and neighborhood size" +
								" - using random sampling of interaction partners!");
		}

		// check competition geometry (may still be undefined at this point)
		Geometry compgeom = (competition != null ? competition : interaction);
		if ((module instanceof Payoffs) // best-response not an option with contact processes
				&& !populationUpdate.isMoran()
				&& !populationUpdate.getType().equals(PopulationUpdate.Type.ECOLOGY)) {
			// Moran type updates ignore playerUpdate
			if (compgeom.getType() == Geometry.Type.MEANFIELD && compGroup.isSampling(IBSGroup.SamplingType.ALL)
					&& playerUpdate.getType() != PlayerUpdate.Type.BEST_RESPONSE) {
				// 010320 using everyone as a reference in mean-field simulations is not
				// feasible - except for best-response
				// ecological updates are based on births and deaths rather than references
				if (logger.isLoggable(Level.WARNING))
					logger.warning("reference type (" + compGroup.getSampling()
							+ ") unfeasible in well-mixed populations!");
				compGroup.setSampling(IBSGroup.SamplingType.RANDOM);
			}
			// best-response in well-mixed populations should skip sampling of references
			if (compgeom.getType() == Geometry.Type.MEANFIELD
					&& playerUpdate.getType() == PlayerUpdate.Type.BEST_RESPONSE) {
				compGroup.setSampling(IBSGroup.SamplingType.NONE);
			}
		}
		// in the original Moran process offspring can replace the parent
		compGroup.setSelf(populationUpdate.isMoran() && compgeom.getType() == Geometry.Type.MEANFIELD);

		// currently: if pop has interaction structure different from MEANFIELD its
		// opponent population needs to be of the same size
		if (module.getNPopulation() != opponent.getModule().getNPopulation()
				&& opponent.getInteractionGeometry() != null // opponent geometry may not yet be initialized
																// check will be repeated for opponent
				&& (getInteractionGeometry().getType() != Geometry.Type.MEANFIELD
						|| opponent.getInteractionGeometry().getType() != Geometry.Type.MEANFIELD)) {
			// at least for now, both populations need to be of the same size - except for
			// well-mixed populations
			logger.warning(
					"inter-species interactions with populations of different size limited to well-mixed structures"
							+ " - well-mixed structure forced!");
			getInteractionGeometry().setType(Geometry.Type.MEANFIELD);
			opponent.getInteractionGeometry().setType(Geometry.Type.MEANFIELD);
			doReset = true;
		}
		// combinations of unstructured and structured populations in inter-species
		// interactions require more attention. exclude for now.
		if (getInteractionGeometry().isInterspecies() && opponent.getInteractionGeometry() != null &&
				(getInteractionGeometry().getType() != opponent.getInteractionGeometry().getType()) &&
				(getInteractionGeometry().getType() == Geometry.Type.MEANFIELD ||
						opponent.getInteractionGeometry().getType() == Geometry.Type.MEANFIELD)) {
			// opponent not yet ready; check will be repeated for opponent
			logger.warning(
					"interspecies interactions combining well-mixed and structured populations not (yet) tested"
							+ " - well-mixed structure forced!");
			getInteractionGeometry().setType(Geometry.Type.MEANFIELD);
			opponent.getInteractionGeometry().setType(Geometry.Type.MEANFIELD);
			doReset = true;
		}

		if (pMigration < 1e-10)
			setMigrationType(MigrationType.NONE);
		if (migrationType != MigrationType.NONE && pMigration > 0.0) {
			if (!interaction.isUndirected) {
				logger.warning("no migration on directed graphs!");
				setMigrationType(MigrationType.NONE);
			} else if (!interaction.interCompSame) {
				logger.warning("no migration on graphs with different interaction and competition neighborhoods!");
				setMigrationType(MigrationType.NONE);
			} else if (interaction.getType() == Geometry.Type.MEANFIELD) {
				logger.warning("no migration in well-mixed populations!");
				setMigrationType(MigrationType.NONE);
			}
		}
		if (migrationType == MigrationType.NONE)
			setMigrationProb(0.0);
		else {
			// need to get new instance to make sure potential changes in pMigration are
			// reflected
			distrMigrants = new RNGDistribution.Geometric(rng.getRNG(), 1.0 - pMigration);
		}

		nMixedInter = -1;
		if (module instanceof Payoffs) {
			boolean ephemeralScores;
			if (module.isStatic()) {
				adjustScores = true;
				playerScoring = ScoringType.RESET_ALWAYS;
				ephemeralScores = false;
			} else {
				// check if adjustScores can be used - subclasses may have different opinions
				adjustScores = doAdjustScores();
				ephemeralScores = playerScoring.equals(ScoringType.EPHEMERAL);
				if (!adjustScores && !playerScoreAveraged && !ephemeralScores) {
					// non-adjustable and accumulated scores result in potentially unbounded payoffs
					// - revert to averaged scores
					setPlayerScoreAveraged(true);
					logger.warning("accumulated scores may result in unbounded fitness - forcing averaged scores.");
					adjustScores = doAdjustScores(); // should now be true
				}
			}

			hasLookupTable = module.isStatic() || //
					(adjustScores && interaction.getType() == Geometry.Type.MEANFIELD) || //
					(ephemeralScores && interaction.getType() == Geometry.Type.MEANFIELD //
							&& interGroup.isSampling(SamplingType.ALL));
			if (hasLookupTable) {
				// allocate memory for fitness lookup table
				if (typeFitness == null || typeFitness.length != nTraits)
					typeFitness = new double[nTraits];
				if (module.isStatic()) {
					// initialize lookup table for static modules
					typeScores = staticmodule.getStaticScores();
					for (int n = 0; n < nTraits; n++)
						typeFitness[n] = map2fit.map(typeScores[n]);
					maxEffScoreIdx = -1;
				} else {
					// allocate memory for score lookup table
					if (typeScores == null || typeScores.length != nTraits)
						typeScores = new double[nTraits];
					// determine number of interactions in well-mixed populations with adjustScores
					int oPop = opponent.getModule().getNPopulation();
					if (interaction.isInterspecies()) {
						// XXX check how to count the number of interactions for inter-species group
						// interactions only max. population size is known at this point (if sizes can
						// vary)
						nMixedInter = oPop * nGroup;
					} else {
						// this can easily exceed the range of int's... would cause issues with
						// accumulated payoffs; excluded in check()
						// should not affect averaged payoffs. catch exception and set to MAX_VALUE
						try {
							nMixedInter = Combinatorics.combinations(oPop - 1, nGroup - 1);
						} catch (ArithmeticException ae) {
							// note: nMixedInter < 0 means no interactions (static modules)
							nMixedInter = Integer.MAX_VALUE;
						}
					}
				}
				// with lookup tables scores, fitness and interaction arrays not needed
				scores = null;
				fitness = null;
				interactions = null;
				// allocate lookup tables
				if (typeScores == null || typeScores.length != nTraits)
					typeScores = new double[nTraits];
				if (typeFitness == null || typeFitness.length != nTraits)
					typeFitness = new double[nTraits];
			} else {
				// request reset if we had lookup tables before to allocate arrays scores,
				// fitness
				// and interaction
				doReset |= (typeFitness != null || typeScores != null);
				typeFitness = null;
				typeScores = null;
			}
			if (!hasLookupTable || ephemeralScores) {
				// emphemeral scores need both
				if (scores == null || scores.length != nPopulation)
					scores = new double[nPopulation];
				if (fitness == null || fitness.length != nPopulation)
					fitness = new double[nPopulation];
				if (interactions == null || interactions.length != nPopulation)
					interactions = new int[nPopulation];
			}

			// number of interactions can also be determined in structured populations with
			// well-mixed demes
			if (adjustScores && interaction.getType() == Geometry.Type.HIERARCHY && //
					interaction.subgeometry.equals(Geometry.Type.MEANFIELD)) {
				nMixedInter = interaction.hierarchy[interaction.hierarchy.length - 1]
						- (interaction.isInterspecies() ? 0 : 1);
			}
		} else {
			// module has no payoffs, e.g. contact process
			adjustScores = false;
			playerScoring = ScoringType.NONE;
			scores = null;
			fitness = null;
			interactions = null;
			typeFitness = null;
			typeScores = null;
		}
		if (tags == null || tags.length != nPopulation)
			tags = new double[nPopulation];

		// check for scenarios that are untested or work in progress
		if (!interaction.isUndirected && !module.isStatic())
			logger.warning("interactions on directed graphs have received very limited testing...");

		if (VACANT >= 0 && nGroup > 2)
			logger.warning("group interactions with vacant sites have NOT been tested...");

		return doReset;
	}

	/**
	 * The array containing the probabilities for rewiring links of the interaction
	 * and competition graphs.
	 * 
	 * @see Geometry#rewire()
	 */
	protected double[] pRewire;

	/**
	 * Set the probabilities for rewiring links of the interaction and competition
	 * graphs.
	 * 
	 * @param rewire the array
	 *               <code>double[] {&lt;interaction&gt;, &lt;competition&gt;}</code>
	 */
	public void setRewire(double[] rewire) {
		if (pRewire == null)
			pRewire = new double[2];
		if (rewire == null || rewire.length < 1) {
			Arrays.fill(pRewire, 0.0);
			return;
		}
		pRewire[0] = Math.max(Math.min(rewire[0], 1.0), 0.0);
		if (rewire.length == 1) {
			pRewire[1] = pRewire[0];
			return;
		}
		pRewire[1] = Math.max(Math.min(rewire[1], 1.0), 0.0);
	}

	/**
	 * The array containing the probabilities for adding links to the interaction
	 * and competition graphs.
	 * 
	 * @see Geometry#rewire()
	 */
	protected double[] pAddwire;

	/**
	 * Set the probabilities for adding links to the interaction
	 * and competition graphs.
	 * 
	 * @param addwire the array
	 *                <code>double[] {&lt;interaction&gt;, &lt;competition&gt;}</code>
	 */
	public void setAddwire(double[] addwire) {
		if (pAddwire == null)
			pAddwire = new double[2];
		if (addwire == null || addwire.length < 1) {
			Arrays.fill(pAddwire, 0.0);
			return;
		}
		pAddwire[0] = Math.max(Math.min(addwire[0], 1.0), 0.0);
		if (addwire.length == 1) {
			pAddwire[1] = pAddwire[0];
			return;
		}
		pAddwire[1] = Math.max(Math.min(addwire[1], 1.0), 0.0);
	}

	/**
	 * Check if scores can be adjusted rather than recalculated after an individual
	 * changed its trait. This requires that individuals interact with all their
	 * neighbours and that the structure of the population is not well-mixed. Some
	 * implementations may be able to extend adjustments to other structures. For
	 * example, adjusting scores is feasible in well-mixed populations for discrete
	 * traits.
	 * 
	 * <h3>Requirements:</h3>
	 * <dl>
	 * <dt>Group.SAMPLING_ALL</dt>
	 * <dd>individuals need to be interacting with all their neighbours (not just a
	 * randomly selected subset).</dd>
	 * <dt>Geometry.MEANFIELD</dt>
	 * <dd>interactions with everyone are not feasible (impossible to model
	 * efficiently), in general, for unstructured populations (subclasses can do
	 * better, e.g. for discrete trait it is possible, see
	 * {@link IBSDPopulation#doAdjustScores()}).</dd>
	 * <dt>playerScoreReset</dt>
	 * <dd>if scores are reset whenever an individual adopts the trait of another
	 * (regardless of whether an actual trait change occurred) then the expected
	 * number of interactions of each individual remains constant over time (even
	 * though the interaction count may differ for individuals on heterogeneous
	 * structures).
	 * </dd>
	 * </dl>
	 *
	 * @return <code>true</code> if adjusting scores is feasible
	 * 
	 * @see ScoringType
	 * @see IBSDPopulation
	 */
	protected abstract boolean doAdjustScores();

	/**
	 * Reset the model. All parameters must be consistent at this point. Allocate
	 * memory and initialize the interaction and competition structures. If
	 * structures include random elements, e.g. random regular graphs, a new
	 * structure is generated. Generate initial configuration. Subclasses must
	 * override this method to allocate memory for the trait and call super.
	 * 
	 * @see #check()
	 * @see Model#reset()
	 */
	public synchronized void reset() {
		interaction.init();
		interaction.rewire();
		interaction.evaluate();

		// for accumulated payoffs the min and max scores can only be determined
		// after the structure of the population is known. note scores are potentially
		// unbounded for accumulated scores with random interactions (not adjustable)
		updateMinMaxScores();

		// check for specific population update types
		if (populationUpdate.isMoran()) {
			// avoid negative fitness for Moran type updates
			if (minFitness < 0.0) {
				if (logger.isLoggable(Level.WARNING))
					logger.warning("Moran updates require fitness>=0 (score range [" + Formatter.format(minScore, 6)
							+ ", " + Formatter.format(maxScore, 6) + "]; " + "fitness range ["
							+ Formatter.format(minFitness, 6) + ", " + Formatter.format(maxFitness, 6) + "]).\n"
							+ "Changed baseline fitness to " + map2fit.getBaseline()
							+ (!map2fit.isMap(Map2Fitness.Map.STATIC) ? " with static payoff-to-fitness map" : ""));
				// just change to something meaningful
				map2fit.setMap(Map2Fitness.Map.STATIC);
				map2fit.setBaseline(-minFitness);
				updateMinMaxScores();
				if (minFitness < 0.0) {
					throw new Error("Adjustment of selection failed... (minimal fitness: "
							+ Formatter.format(minScore, 6) + " should be positive)");
				}
			}
			// use referenceGroup for picking random neighbour in Moran process
			// (birth-death)
			// reason: referenceGroup properly deals with hierarchies
			// future: pick parent to populate vacated site (death-birth, fitness dependent)
			compGroup.setSampling(IBSGroup.SamplingType.RANDOM);
			compGroup.setNSamples(1);
		}
		// avoid numerical overflow for strong selection (mainly applies to exponential
		// payoff-to-fitness mapping)
		if (maxFitness * nPopulation > Double.MAX_VALUE) {
			double mScore = map2fit.invmap(Double.MAX_VALUE / nPopulation) * 0.99; // only go to 99% of maximum, just in
																					// case
			map2fit.setSelection(Functions.round(mScore / maxScore * map2fit.getSelection()));
			// note: the maximum selection strength may be significantly higher if
			// populations,
			// on average, are unable to achieve the highest individual payoffs
			if (logger.isLoggable(Level.WARNING))
				logger.warning(
						"selection strength too strong (numerical overflow) - reduced to (conservative) maximum of "
								+ Formatter.format(map2fit.getSelection(), 4));
			updateMinMaxScores();
		}

		if (interaction.interCompSame) {
			competition = interaction.deriveCompetitionGeometry();
		} else {
			competition.init();
			competition.rewire();
			competition.evaluate();
		}
		module.setGeometries(interaction, competition);
		// determine maximum reasonable group size
		int maxGroup = Math.max(Math.max(interaction.maxIn, interaction.maxOut),
				Math.max(competition.maxIn, competition.maxOut));
		int nGroup = module.getNGroup();
		maxGroup = Math.max(maxGroup, nGroup) + 1; // add 1 if focal should be part of group
		if (groupScores == null || groupScores.length != maxGroup)
			groupScores = new double[maxGroup]; // can hold scores for any group size!
		if (smallScores == null || smallScores.length != maxGroup)
			smallScores = new double[maxGroup]; // can hold scores for any group size!
		if (cProbs == null || cProbs.length != maxGroup)
			cProbs = new double[maxGroup]; // can hold groups of any size!

		// number of interactions can be determined for ephemeral payoffs
		// store in interactions array
		if (playerScoring.equals(ScoringType.EPHEMERAL)) {
			if (interGroup.isSampling(SamplingType.ALL)) {
				if (nGroup > 2) {
					// single interaction with all members in neighbourhood
					Arrays.fill(interactions, 1);
				} else {
					// pairwise interactions
					if (interaction.isRegular)
						Arrays.fill(interactions, (int) (interaction.connectivity + 0.5));
					else
						System.arraycopy(interaction.kin, 0, interactions, 0, interactions.length);
				}
			} else {
				Arrays.fill(interactions, Combinatorics.combinations(interGroup.nSamples, nGroup - 1));
			}
		}
	}

	/**
	 * Initialize the model. All parameters must be consistent. Subclasses must
	 * override this method to generate the initial trait configuration and call
	 * super.
	 * <p>
	 * <strong>Note:</strong> Initialization leaves the interaction and competition
	 * structures untouched
	 * 
	 * @see #check()
	 * @see Model#init()
	 */
	public void init() {
		// initialize tags
		if (tags != null)
			for (int n = 0; n < nPopulation; n++)
				tags[n] = n;
		// if flagged as inconsistent, no (further) checks are performed
		isConsistent = consistencyCheckRequested;
	}

	/**
	 * The flag to indicate whether the state of the IBS model is consistent.
	 * 
	 * @see #isConsistent()
	 */
	boolean isConsistent;

	/**
	 * The flag to indicate whether consistency checks on the state of the IBS model
	 * are requested.
	 * 
	 * @see #isConsistent()
	 */
	boolean consistencyCheckRequested;

	/**
	 * Enable consistency checks of the state of the IBS model. Never use in
	 * production.
	 * 
	 * @param check {@code true} to request consistency checks
	 */
	public void setConsistencyCheck(boolean check) {
		consistencyCheckRequested = check;
	}

	/**
	 * Convenience method during development to perform a number of consistency
	 * checks of the current state. Once an inconsistency is found there is no need
	 * to keep looking and no further checks are performed.
	 * <p>
	 * Execution time is of little concern here. Never use in the final simulation
	 * code.
	 */
	public void isConsistent() {
		if (!isConsistent || !logger.isLoggable(Level.WARNING))
			return;
		// universal consistency checks
		for (int n = 0; n < nPopulation; n++) {
			double scoren = getScoreAt(n);
			if (Double.isNaN(scoren)) {
				logger.warning("scoring issue @ " + n + ": score=" + scoren + " is NaN...");
				isConsistent = false;
				continue;
			}
			int interactionsn = getInteractionsAt(n);
			if (isVacantAt(n)) {
				if (scoren > 1e-12) {
					logger.warning("scoring issue @ " + n + ": score=" + scoren + " of vacant site should be zero");
					isConsistent = false;
				}
				if (interactionsn != -1) {
					// vacant sites and static modules have an interaction count of -1
					logger.warning("interactions issue @ " + n + ": interactions=" + interactionsn
							+ " of vacant site should be -1");
					isConsistent = false;
				}
				continue;
			}
			if (interactionsn == 0) {
				if (scoren > 1e-12) {
					logger.warning(
							"scoring issue @ " + n + ": score=" + scoren + " of isolated site should be zero");
					isConsistent = false;
				}
				continue;
			}
			if (scoren + 1e-12 < minScore || scoren - 1e-12 > maxScore) {
				logger.warning(
						"scoring issue @ " + n + ": score=" + scoren + " not in [" + minScore + ", " + maxScore
								+ "]");
				isConsistent = false;
			}
			double fitn = getFitnessAt(n);
			if (fitn + 1e-12 < minFitness || fitn - 1e-12 > maxFitness) {
				logger.warning(
						"scoring issue @ " + n + ": fitness=" + fitn + " not in [" + minFitness + ", " + maxFitness
								+ "]");
				isConsistent = false;
			}
			if (Math.abs(map2fit.map(scoren) - fitn) > 1e-12) {
				logger.warning(
						"scoring issue @ " + n + ": score=" + scoren + " maps to " + map2fit.map(scoren)
								+ " instead of fitness=" + fitn);
				isConsistent = false;
			}
		}
		if (adjustScores) {
			// recalculate scores/fitness
			if (hasLookupTable) {
				double[] typeScoresStore = typeScores;
				double[] typeFitnessStore = ArrayMath.clone(typeFitness);
				Arrays.fill(typeFitness, Double.MAX_VALUE);
				double sumFitnessStore = sumFitness;
				if (!module.isStatic()) {
					// don't destroy static scores
					typeScoresStore = ArrayMath.clone(typeScores);
					Arrays.fill(typeScores, Double.MAX_VALUE);
					sumFitness = 0.0;
					engine.getModel().update(); // initialize typeScores/typeFitness
				}
				for (int n = 0; n < nTraits; n++) {
					if (n == VACANT && typeScores[n] != typeScoresStore[n]) {
						logger.warning("scoring issue for vacant trait " + n + ": score=" + typeScoresStore[n]
								+ " instead of " + typeScores[n] + " (NaN)");
						isConsistent = false;
					}
					if (Math.abs(typeScores[n] - typeScoresStore[n]) > 1e-12) {
						logger.warning(
								"scoring issue for trait " + n + ": score=" + typeScoresStore[n] + " instead of "
										+ typeScores[n]);
						isConsistent = false;
					}
					typeFitness[n] = map2fit.map(typeScores[n]);
					if (Math.abs(typeFitness[n] - typeFitnessStore[n]) > 1e-12) {
						logger.warning(
								"fitness issue for trait " + n + ": fitness=" + typeFitnessStore[n] + " instead of "
										+ typeFitness[n]);
						isConsistent = false;
					}
					if (Math.abs(map2fit.map(typeScores[n]) - typeFitness[n]) > 1e-12) {
						logger.warning(
								"scoring issue for trait " + n + ": score=" + typeScores[n] + " maps to "
										+ map2fit.map(typeScores[n])
										+ " instead of fitness=" + typeFitness[n]);
						isConsistent = false;
					}
				}
				if (Math.abs(sumFitness - sumFitnessStore) > 1e-12) {
					logger.warning("accounting issue: sum of fitness is " + sumFitnessStore
							+ " instead of recalculated fitness " + sumFitness);
					isConsistent = false;
				}
				double checkFitness = 0.0;
				for (int n = 0; n < nPopulation; n++)
					checkFitness += getFitnessAt(n);
				if (Math.abs(sumFitness - checkFitness) > Combinatorics.pow(10,
						-11 + Functions.magnitude(sumFitness))) {
					logger.warning(
							"accounting issue: fitness sums to " + checkFitness + " instead of sumFitness=" + sumFitness
									+
									" (delta=" + Math.abs(sumFitness - checkFitness) + ", max="
									+ Combinatorics.pow(10, -11 + Functions.magnitude(sumFitness)) + ")");
					isConsistent = false;
				}
				// restore data
				typeScores = typeScoresStore;
				typeFitness = typeFitnessStore;
				sumFitness = sumFitnessStore;
			} else {
				// no lookup tables
				double[] scoresStore = scores;
				scores = new double[nPopulation];
				double[] fitnessStore = fitness;
				fitness = new double[nPopulation];
				double sumFitnessStore = sumFitness;
				engine.getModel().update();
				for (int n = 0; n < nPopulation; n++) {
					if (Math.abs(scores[n] - scoresStore[n]) > 1e-12) {
						logger.warning(
								"scoring issue @ " + n + ": score=" + scoresStore[n] + " instead of " + scores[n]);
						isConsistent = false;
					}
					if (Math.abs(fitness[n] - fitnessStore[n]) > 1e-12) {
						logger.warning(
								"fitness issue @ " + n + ": fitness=" + fitnessStore[n] + " instead of " + fitness[n]);
						isConsistent = false;
					}
					if (Math.abs(map2fit.map(scores[n]) - fitness[n]) > 1e-12) {
						logger.warning(
								"scoring issue @ " + n + ": score=" + scores[n] + " maps to " + map2fit.map(scores[n])
										+ " instead of fitness=" + fitness[n]);
						isConsistent = false;
					}
				}
				if (Math.abs(sumFitness - sumFitnessStore) > 1e-12) {
					logger.warning("accounting issue: sum of fitness is " + sumFitnessStore
							+ " instead of recalculated fitness " + sumFitness);
					isConsistent = false;
				}
				double checkFitness = ArrayMath.norm(fitness);
				if (Math.abs(sumFitness - checkFitness) > 1e-12) {
					logger.warning(
							"accounting issue: sum of fitness is " + checkFitness + " instead of sumFitness="
									+ sumFitness);
					isConsistent = false;
				}
				// restore data
				scores = scoresStore;
				fitness = fitnessStore;
				sumFitness = sumFitnessStore;
			}
		} else {
			// no adjust scores
			double checkFitness = 0.0;
			// fitness array may not exist
			for (int n = 0; n < nPopulation; n++)
				checkFitness += getFitnessAt(n);
			if (Math.abs(checkFitness - sumFitness) > 1e-8) {
				logger.warning("accounting issue: sum of fitness is " + checkFitness + " but sumFitness=" + sumFitness);
				isConsistent = false;
			}
		}
		if (!isConsistent)
			logger.warning("inconsistency found @ " + engine.getModel().getUpdates());
	}

	/**
	 * Reset all traits in preparation of the next update step. Simply an
	 * opportunity for customizations in subclasses.
	 */
	public void resetTraits() {
	}

	/**
	 * Return the number of mean values for this population (for traits or fitness).
	 * <p>
	 * <strong>Note:</strong> The number of mean traits in a model may differ from
	 * the number of traits in the corresponding module. This is the case for
	 * example for {@link Geometry.Type#SQUARE_NEUMANN_2ND} with two disjoint
	 * interaction or competition graphs.
	 *
	 * @return the number of mean values
	 * 
	 * @see IBS#getNMean()
	 */
	public int getNMean() {
		return nTraits;
	}

	/**
	 * Returns the mean trait(s) of this population in the array {@code mean}. Used
	 * by GUI to visualize the current state of this IBS model.
	 * 
	 * @param mean the array for returning the trait values
	 * 
	 * @see Model#getMeanTraits(int, double[])
	 */
	public abstract void getMeanTraits(double[] mean);

	/**
	 * Returns the traits of all individuals in this population coded as colors in
	 * the array {@code colors} using the map {@code colorMap}. Used by GUI to
	 * visualize the current state of this IBS model. Colors are coded in different
	 * data types {@code <T>} depending on the runtime environment (GWT or JRE) as
	 * well as the graph (e.g. {@link org.evoludo.graphics.PopGraph2D
	 * PopGraph2D} or {@link org.evoludo.graphics.PopGraph3D PopGraph3D}).
	 * 
	 * @param <T>      the type of color data ({@link String} or
	 *                 {@link thothbot.parallax.core.shared.materials.MeshLambertMaterial
	 *                 MeshLambertMaterial} for GWT and {@link Color} for JRE).
	 * @param colors   the array where the colors of all nodes are stored
	 * @param colorMap the map that converts traits into colors
	 */
	public abstract <T> void getTraitData(T[] colors, ColorMap<T> colorMap);

	/**
	 * Returns the mean fitness of this population in the array {@code mean}. Used
	 * by GUI to visualize the current state of this IBS model. Returns {@code true}
	 * if data point belongs to the same time series and {@code false} if a new
	 * series was started through {@link #init()} or {@link #reset()}.
	 * 
	 * @param mean the array for storing the mean fitness values
	 * 
	 * @see Model#getMeanFitness(int, double[])
	 */
	public abstract void getMeanFitness(double[] mean);

	/**
	 * Returns the fitness of all individuals in this population coded as colors in
	 * the array {@code colors} using the map {@code colorMap}. Used by GUI to
	 * visualize the current state of this IBS model. Colors are coded in different
	 * data types {@code <T>} depending on the runtime environment (GWT or JRE) as
	 * well as the graph (e.g. {@link org.evoludo.graphics.PopGraph2D
	 * PopGraph2D} or {@link org.evoludo.graphics.PopGraph3D PopGraph3D}).
	 * 
	 * @param <T>      the type of color data ({@link String} or
	 *                 {@link thothbot.parallax.core.shared.materials.MeshLambertMaterial
	 *                 MeshLambertMaterial} for GWT and {@link Color} for JRE).
	 * @param colors   the array where the colors of all nodes are stored
	 * @param colorMap the map that converts traits into colors
	 */
	public <T> void getFitnessData(T[] colors, ColorMap.Gradient1D<T> colorMap) {
		if (hasLookupTable) {
			if (VACANT < 0) {
				for (int n = 0; n < nPopulation; n++)
					colors[n] = colorMap.translate(getFitnessAt(n));
				return;
			}
			T vacant = colorMap.color2Color(Color.WHITE);
			for (int n = 0; n < nPopulation; n++) {
				if (isVacantAt(n)) {
					// how to color vacant sites?
					colors[n] = vacant;
					continue;
				}
				colors[n] = colorMap.translate(getFitnessAt(n));
			}
			return;
		}
		if (VACANT < 0) {
			colorMap.translate(fitness, colors);
			return;
		}
		T vacant = colorMap.color2Color(Color.WHITE);
		for (int n = 0; n < nPopulation; n++) {
			if (isVacantAt(n)) {
				// how to color vacant sites?
				colors[n] = vacant;
				continue;
			}
			colors[n] = colorMap.translate(getFitnessAt(n));
		}
	}

	/**
	 * Generates a histogram of the fitness distribution in this population. The
	 * result is returned in the array {@code bins}.
	 * 
	 * <h3>Notes:</h3>
	 * <ol>
	 * <li>{@code bins} is a 2D array because discrete models generate histograms
	 * for each trait separately.
	 * <li>By default generate a histogram of the scores in {@code bins[0]}.
	 * <li>Consider moving to {@link IBSDPopulation} and {@link IBSCPopulation} with
	 * arguments {@code bins[][]} and {@code bins[]}, respectively.
	 * </ol>
	 *
	 * @param bins the 2D array to store the histogram(s)
	 */
	public void getFitnessHistogramData(double[][] bins) {
		// clear bins
		Arrays.fill(bins[0], 0.0);
		int nBins = bins[0].length;
		// for neutral selection maxScore==minScore!
		// in that case assume range [score-1, score+1]
		// needs to be synchronized with GUI (e.g. MVFitness, MVFitHistogram, ...)
		double min = minScore;
		double map;
		if (isNeutral) {
			map = nBins * 0.5;
			min--;
		} else
			map = nBins / (maxScore - minScore);

		// fill bins
		int max = nBins - 1;
		for (int n = 0; n < nPopulation; n++) {
			if (isVacantAt(n))
				continue;
			int bin = (int) ((scores[n] - min) * map);
			bin = Math.max(0, Math.min(max, bin));
			bins[0][bin]++;
		}
		ArrayMath.multiply(bins[0], 1.0 / nPopulation);
	}

	/**
	 * Gets the scores of all individuals with precision {@code digits}. Scores of
	 * vacant sites are reported as {@value Double#NaN}.
	 * 
	 * @param digits the number of digits for the scores
	 * @return the formatted scores
	 */
	public String getScores(int digits) {
		if (hasLookupTable) {
			StringBuilder buf = new StringBuilder();
			buf.append(Formatter.format(getScoreAt(0), digits));
			for (int n = 1; n < nPopulation; n++)
				buf.append(Formatter.VECTOR_DELIMITER).append(" ").append(Formatter.format(getScoreAt(n), digits));
			return buf.toString();
		}
		return Formatter.format(scores, digits);
	}

	/**
	 * Gets the formatted score of the individual with index {@code idx}.
	 * 
	 * @param idx the index of the individual
	 * @return the formatted score
	 */
	public String getScoreNameAt(int idx) {
		if (isVacantAt(idx))
			return "-";
		return Formatter.format(getScoreAt(idx), 4);
	}

	/**
	 * Gets the fitness of all individuals with precision {@code digits}. Fitness of
	 * vacant sites are reported as {@value Double#NaN}.
	 * 
	 * @param digits the number of digits for the scores
	 * @return the formatted fitness
	 */
	public String getFitness(int digits) {
		if (hasLookupTable) {
			StringBuilder buf = new StringBuilder();
			buf.append(Formatter.format(getFitnessAt(0), digits));
			for (int n = 1; n < nPopulation; n++)
				buf.append(Formatter.VECTOR_DELIMITER).append(" ").append(Formatter.format(getFitnessAt(n), digits));
			return buf.toString();
		}
		return Formatter.format(fitness, digits);
	}

	/**
	 * Gets the formatted fitness of the individual with index {@code idx} as a
	 * string. If the flag {@code pretty} is set the formatting is prettyfied by
	 * replacing the exponent {@code E} in scientific notation to a power of
	 * {@code 10}.
	 *
	 * @param idx    the index of the individual
	 * @param pretty flag to prettify formatting
	 * @return the formatted fitness
	 */
	public String getFitnessNameAt(int idx, boolean pretty) {
		if (isVacantAt(idx))
			return "-";
		// for strong selection fitness can be huge - use scientific notation if >10^7
		double fiti = getFitnessAt(idx);
		return fiti > 1e7
				? (pretty ? (Formatter.formatSci(fiti, 4).replace("E", "⋅10<sup>") + "</sup>")
						: Formatter.formatSci(fiti, 4))
				: Formatter.format(fiti, 4);
	}

	/**
	 * Gets the formatted and prettyfied fitness of the individual with index
	 * {@code idx} as a string.
	 *
	 * @param idx the index of the individual
	 * @return the formatted fitness
	 */
	public String getFitnessNameAt(int idx) {
		return getFitnessNameAt(idx, true);
	}

	/**
	 * Gets the number of interactions of the individual with index {@code idx}.
	 * Returns {@code -1} if site {@code idx} is vacant or fitness is static, i.e.
	 * not based on interactions.
	 * 
	 * @param idx the index of the individual
	 * @return the number of interactions
	 */
	public int getInteractionsAt(int idx) {
		if (isVacantAt(idx))
			return -1;
		if (hasLookupTable) {
			if (module.isStatic())
				return -1;
			return nMixedInter;
		}
		return interactions[idx];
	}

	/**
	 * Copies the tags of all individuals in the population and returns them in the
	 * array {@code mem}.
	 * 
	 * @param mem the array to copy the tags into
	 * @return the array of tags
	 */
	public double[] getTags(double[] mem) {
		System.arraycopy(tags, 0, mem, 0, nPopulation);
		return mem;
	}

	/**
	 * Returns the tags of all individuals in this population coded as colors in
	 * the array {@code colors} using the map {@code colorMap}. Used by GUI to
	 * visualize the current state of this IBS model. Colors are coded in different
	 * data types {@code <T>} depending on the runtime environment (GWT or JRE) as
	 * well as the graph (e.g. {@link org.evoludo.graphics.PopGraph2D
	 * PopGraph2D} or {@link org.evoludo.graphics.PopGraph3D PopGraph3D}).
	 * 
	 * @param <T>      the type of color data ({@link String} or
	 *                 {@link thothbot.parallax.core.shared.materials.MeshLambertMaterial
	 *                 MeshLambertMaterial} for GWT and {@link Color} for JRE).
	 * @param colors   the array where the colors of all nodes are stored
	 * @param colorMap the map that converts tags into colors
	 */
	public <T> void getTagData(T[] colors, ColorMap<T> colorMap) {
		colorMap.translate(tags, colors);
	}

	/**
	 * Gets the tag of the individual with index {@code idx}.
	 *
	 * @param idx the index of the individual
	 * @return the tag number
	 */
	public double getTagAt(int idx) {
		return tags[idx];
	}

	/**
	 * Gets the formatted tag of the individual with index {@code idx} as string.
	 *
	 * @param idx the index of the individual
	 * @return the tag as a string
	 */
	public String getTagNameAt(int idx) {
		return Formatter.format(tags[idx], 4);
	}

	/**
	 * Sets the tag of the individual with index {@code idx}.
	 *
	 * @param idx the index of the individual
	 * @param tag the new tag of the individual
	 * @return {@code true} if the tag changed
	 */
	public boolean setTagAt(int idx, double tag) {
		double old = tags[idx];
		tags[idx] = tag;
		return (Math.abs(tag - old) < 1e-8);
	}

	/**
	 * Gets current population size. For most models with fixed population sizes
	 * this simply returns nPopulation. Ecological models with variable population
	 * sizes must override this method to return the actual population size.
	 *
	 * @return current population size
	 */
	public int getPopulationSize() {
		return nPopulation;
	}

	/**
	 * The fraction of the population that gets updated in synchronous updates.
	 */
	double syncFraction = 1.0;

	/**
	 * Set the fraction of the population that gets updated in synchronous updates.
	 * 
	 * @param sync the fraction that gets updated
	 */
	public void setSyncFraction(double sync) {
		if (sync <= 0.0 || sync > 1.0 || Math.abs(syncFraction - sync) < 1e-8)
			return;
		syncFraction = sync;
	}

	/**
	 * Get the fraction of the population that gets updated in synchronous updates.
	 * 
	 * @return the fraction that gets updated
	 */
	public double getSyncFraction() {
		return syncFraction;
	}

	/**
	 * Sets the type of migrations to {@code type}.
	 * 
	 * @param type the new type of migrations
	 */
	public void setMigrationType(MigrationType type) {
		migrationType = type == null ? MigrationType.NONE : type;
	}

	/**
	 * Gets the type of migrations.
	 * 
	 * @return the type of migrations
	 */
	public MigrationType getMigrationType() {
		return migrationType;
	}

	/**
	 * Sets the migration probability to {@code aValue}.
	 * 
	 * @param aValue the new migration probability
	 */
	public void setMigrationProb(double aValue) {
		pMigration = aValue;
	}

	/**
	 * Gets the migration probability.
	 * 
	 * @return the migration probability
	 */
	public double getMigrationProb() {
		return pMigration;
	}

	/**
	 * Gets the structure of interactions.
	 * 
	 * @return the interaction structure
	 */
	public Geometry getInteractionGeometry() {
		return interaction;
	}

	/**
	 * Creates a new instance of the interaction structure, if needed.
	 * 
	 * @return the interaction structure
	 */
	public Geometry createInteractionGeometry() {
		if (interaction == null || interaction == module.getGeometry())
			interaction = new Geometry(engine, module, opponent.getModule());
		return interaction;
	}

	/**
	 * Gets the structure of competition or imitations.
	 * 
	 * @return the competition structure
	 */
	public Geometry getCompetitionGeometry() {
		return competition;
	}

	/**
	 * Creates a new instance of the competition or imitation structure, if needed.
	 * 
	 * @return the competition structure
	 */
	public Geometry createCompetitionGeometry() {
		if (competition == null || competition == module.getGeometry())
			competition = new Geometry(engine, module);
		return competition;
	}

	/**
	 * Sets the type for managing scores of individuals.
	 * 
	 * @param type the type for managing scores
	 * 
	 * @see ScoringType
	 */
	public void setPlayerScoring(ScoringType type) {
		playerScoring = type;
	}

	/**
	 * Gets the type for managing scores of individuals.
	 * 
	 * @return {@code true} if scores are always reset
	 * 
	 * @see ScoringType
	 */
	public ScoringType getPlayerScoring() {
		return playerScoring;
	}

	/**
	 * Sets whether scores of individuals are averaged over multiple interactions or
	 * accumulated.
	 * 
	 * @param aver the flag to indicate whether scores are averaged
	 */
	public void setPlayerScoreAveraged(boolean aver) {
		playerScoreAveraged = aver;
	}

	/**
	 * Gets whether player scores are averaged (as opposed to accumulated).
	 *
	 * @return {@code true} if player scores are averaged.
	 */
	public boolean getPlayerScoreAveraged() {
		return playerScoreAveraged;
	}

	/**
	 * Provide opportunity/hook for subclasses to introduce new geometries.
	 * 
	 * @param geom the current empty/uninitialized geometry
	 * @param arg  the commandline argument
	 * @return {@code true} if parsing was successful
	 * 
	 * @see Geometry#parse(String)
	 */
	public boolean parseGeometry(Geometry geom, String arg) {
		return false;
	}

	/**
	 * Provide opportunity/hook for subclasses to introduce new geometries.
	 * 
	 * @param geom the geometry to check
	 * @return {@code true} if checks were successful
	 * 
	 * @see Geometry#check()
	 */
	public boolean checkGeometry(Geometry geom) {
		return false;
	}

	/**
	 * Provide opportunity/hook for subclasses to introduce new geometries.
	 * 
	 * @param geom the geometry to initialize
	 * @return {@code true} if generation of structure was successful
	 * 
	 * @see Geometry#init()
	 */
	public boolean generateGeometry(Geometry geom) {
		return false;
	}

	// private long linkCount(Geometry geom) {
	// long outcount = 0, incount = 0;
	// for( int n=0; n<nPopulation; n++ ) {
	// outcount += geom.kout[n];
	// incount += geom.kin[n];
	// }
	// if( outcount != incount ) {
	// logger.severe("ALARM: some links point to nirvana!? ("+incount+",
	// "+outcount+")");
	// }
	// return outcount;
	// }

	/**
	 * Called from GUI if node/individual with index {@code idx} received a mouse
	 * click or tap.
	 *
	 * @param hit the index of the node
	 * @return {@code false} if no actions taken
	 */
	public boolean mouseHitNode(int hit) {
		return mouseHitNode(hit, false);
	}

	// allows drawing of initial configurations - feature got retired; revive?
	// /**
	// *
	// * @param hit
	// * @param ref
	// * @return
	// */
	// public boolean mouseHitNode(int hit, int ref) {
	// return false;
	// }

	/**
	 * Called from GUI if node/individual with index {@code idx} received a mouse
	 * click or tap and indicates whether the {@code alt}-key had been pressed.
	 *
	 * @param hit the index of the node
	 * @param alt {@code true} if the {@code alt}-key was pressed
	 * @return {@code false} if no actions taken
	 */
	public boolean mouseHitNode(int hit, boolean alt) {
		return false;
	}

	/**
	 * Encode the fitness of all individuals in the IBS model in a
	 * <code>plist</code> inspired <code>XML</code> string.
	 * 
	 * @param plist the {@link java.lang.StringBuilder StringBuilder} to write the
	 *              encoded state to
	 * 
	 * @see Model#encodeState(StringBuilder)
	 */
	public void encodeFitness(StringBuilder plist) {
		if (!hasLookupTable)
			plist.append(Plist.encodeKey("Fitness", scores));
	}

	/**
	 * Restore the fitness of all individuals encoded in the <code>plist</code>
	 * inspired <code>map</code> of {@code key, value}-pairs.
	 * 
	 * @param plist the map of {@code key, value}-pairs
	 * @return <code>true</code> if successful
	 * 
	 * @see Model#restoreState(Plist)
	 */
	public boolean restoreFitness(Plist plist) {
		if (!hasLookupTable) {
			@SuppressWarnings("unchecked")
			List<Double> fit = (List<Double>) plist.get("Fitness");
			if (fit == null || fit.size() != nPopulation)
				return false;
			sumFitness = 0.0;
			for (int n = 0; n < nPopulation; n++) {
				double nscore = fit.get(n);
				scores[n] = nscore;
				double nfit = map2fit.map(nscore);
				fitness[n] = nfit;
				sumFitness += nfit;
			}
			setMaxEffScoreIdx();
		}
		return true;
	}

	/**
	 * Encode the interactions of all individuals in the IBS model in a
	 * <code>plist</code> inspired <code>XML</code> string.
	 * 
	 * @param plist the {@link java.lang.StringBuilder StringBuilder} to write the
	 *              encoded state to
	 * 
	 * @see Model#encodeState(StringBuilder)
	 */
	public void encodeInteractions(StringBuilder plist) {
		if (!hasLookupTable)
			plist.append(Plist.encodeKey("Interactions", interactions));
	}

	/**
	 * Restore the interactions of all individuals encoded in the <code>plist</code>
	 * inspired <code>map</code> of {@code key, value}-pairs.
	 * 
	 * @param plist the map of {@code key, value}-pairs
	 * @return <code>true</code> if successful
	 * 
	 * @see Model#restoreState(Plist)
	 */
	public boolean restoreInteractions(Plist plist) {
		if (!hasLookupTable) {
			@SuppressWarnings("unchecked")
			List<Integer> inter = (List<Integer>) plist.get("Interactions");
			if (inter == null || inter.size() != nPopulation)
				return false;
			for (int n = 0; n < nPopulation; n++)
				interactions[n] = inter.get(n);
		}
		return true;
	}

	/**
	 * Encode the traits of all individuals in the IBS model in a
	 * <code>plist</code> inspired <code>XML</code> string.
	 * 
	 * @param plist the {@link java.lang.StringBuilder StringBuilder} to write the
	 *              encoded state to
	 * 
	 * @see Model#encodeState(StringBuilder)
	 */
	public abstract void encodeTraits(StringBuilder plist);

	/**
	 * Restore the traits of all individuals encoded in the <code>plist</code>
	 * inspired <code>map</code> of {@code key, value}-pairs.
	 * 
	 * @param plist the map of {@code key, value}-pairs
	 * @return <code>true</code> if successful
	 * 
	 * @see Model#restoreState(Plist)
	 */
	public abstract boolean restoreTraits(Plist plist);

	/**
	 * Encode the interaction and competition structures of the IBS model in a
	 * <code>plist</code> inspired <code>XML</code> string.
	 * 
	 * @param plist the {@link java.lang.StringBuilder StringBuilder} to write the
	 *              encoded state to
	 * 
	 * @see Model#encodeState(StringBuilder)
	 */
	public void encodeGeometry(StringBuilder plist) {
		plist.append(
				"<key>" + interaction.name + "</key>\n" +
						"<dict>\n");
		plist.append(interaction.encodeGeometry());
		plist.append("</dict>\n");
		if (interaction.interCompSame)
			return;
		plist.append(
				"<key>" + competition.name + "</key>\n" +
						"<dict>\n");
		plist.append(competition.encodeGeometry());
		plist.append("</dict>\n");
	}

	/**
	 * Restore the interaction and competition structures encoded in the
	 * <code>plist</code> inspired <code>map</code> of {@code key, value}-pairs.
	 * 
	 * @param plist the map of {@code key, value}-pairs
	 * @return <code>true</code> if successful
	 * 
	 * @see Model#restoreState(Plist)
	 */
	public boolean restoreGeometry(Plist plist) {
		Plist igeo = (Plist) plist.get(interaction.name);
		if (igeo == null)
			return false;
		interaction.decodeGeometry(igeo);
		if (interaction.interCompSame) {
			competition = interaction.deriveCompetitionGeometry();
			return true;
		}
		Plist rgeo = (Plist) plist.get(competition.name);
		if (rgeo == null)
			return false;
		competition.decodeGeometry(rgeo);
		return true;
	}

	/**
	 * Set the seed of the random number generator for competition simulation runs.
	 *
	 * @param s the seed for random number generator
	 */
	public void srandom(long s) {
		rng.setRNGSeed(s);
	}

	/**
	 * Draw a uniformly distributed random integer number from the closed interval
	 * {@code [0, n]}.
	 *
	 * @param n the upper limit of interval (inclusive)
	 * @return the random integer number in <code>[0, n]</code>.
	 */
	public int random0N(int n) {
		return rng.random0N(n);
	}

	/**
	 * Draw a uniformly distributed random integer number from the semi-closed
	 * interval {@code [0, n)}.
	 *
	 * @param n the upper limit of interval (exclusive)
	 * @return the random integer number in <code>[0, n)</code>.
	 */
	public int random0n(int n) {
		return rng.random0n(n);
	}

	/**
	 * Draw a uniformly distributed random {@code double} from the semi-closed
	 * interval {@code [0, 1)} with 32bit resolution. This is the default.
	 *
	 * @return the random number in <code>[0, 1)</code>.
	 */
	public double random01() {
		return rng.random01();
	}

	/**
	 * Draw a uniformly distributed random {@code double} from the semi-closed
	 * interval {@code [0, 1)} with maximal 53bit resolution.
	 * <p>
	 * <strong>Note:</strong> takes twice as long as regular precision.
	 *
	 * @return the random number in <code>[0, 1)</code>.
	 */
	public double random01d() {
		return rng.random01d();
	}

	/**
	 * Draw a Gaussian (normal) distributed random {@code double}.
	 *
	 * @param mean the mean of the Gaussian distribution
	 * @param sdev the standard deviation of the Gaussian distribution
	 * @return the Gaussian random number
	 */
	public double randomGaussian(double mean, double sdev) {
		return mean + sdev * rng.nextGaussian();
	}

	/**
	 * Draw a binomially distributed random integer.
	 *
	 * @param p the probability of success
	 * @param n the number of trials
	 * @return the number of successes
	 */
	public int nextBinomial(double p, int n) {
		if (n == 0)
			return 0;
		if (p > 1.0 - 1e-8)
			return 0;

		// check if gaussian approximation is suitable
		double np = n * p;
		if (np / (1.0 - p) > 9.0 && n * (1.0 - p) / p > 9.0)
			return (int) Math.floor(randomGaussian(np + 0.5, Math.sqrt(np * (1.0 - p))));

		double uRand = random01();
		double pi = Combinatorics.pow(1.0 - p, n);
		double f = p / (1.0 - p);
		double sum = 0.0;

		for (int i = 0; i <= n; i++) {
			sum += pi * Combinatorics.combinations(n, i);
			if (uRand <= sum)
				return i;
			pi *= f;
		}
		if (logger.isLoggable(Level.WARNING)) {
			StringBuilder sb = new StringBuilder("What the heck are you doing here!!! (rand: ");
			sb.append(uRand).append(", p: ").append(p).append(", n: ").append(n).append(" → ").append(sum).append(")");
			logger.warning(sb.toString());
		}
		return -1;
	}
}
