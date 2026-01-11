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

import java.util.logging.Level;

import org.evoludo.math.RNGDistribution;
import org.evoludo.simulator.ColorMap;
import org.evoludo.simulator.EvoLudo;
import org.evoludo.simulator.geometries.AbstractGeometry;
import org.evoludo.simulator.geometries.GeometryType;
import org.evoludo.simulator.modules.Features.Payoffs;
import org.evoludo.simulator.modules.Map2Fitness;
import org.evoludo.simulator.modules.Module;
import org.evoludo.simulator.modules.Mutation;
import org.evoludo.simulator.modules.SpeciesUpdate;
import org.evoludo.simulator.views.HasHistogram;
import org.evoludo.util.CLOCategory;
import org.evoludo.util.CLODelegate;
import org.evoludo.util.CLOParser;
import org.evoludo.util.CLOption;
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
	 * Keeps track of the time elapsed, measured in number of updates. One unit of
	 * time corresponds to one generation or one Monte-Carlo step, such that in a
	 * population of size <code>N</code> one generation corresponds to
	 * <code>N</code> updates, which translates to <code>N</code> events (birth,
	 * death, imitation, etc.).
	 * <p>
	 * <strong>Notes:</strong>
	 * <ol>
	 * <li><code>updates==0</code> after {@link #reset()} and at the beginning of
	 * a simulation run.
	 * <li><code>updates</code> is incremented <em>before</em> the next event is
	 * processed, to reflect the time at which the event occurs.
	 * <li>generally differs from 'real time'.
	 * <li>models may implement only one time measure.
	 * <li>setting {@code updates = Double.POSITIVE_INFINITY} disables time measured
	 * in terms of updates.
	 * </ol>
	 * 
	 * @see Model#time
	 */
	protected double updates;

	/**
	 * Total population size across all species. Convenience field for
	 * computing per-capita quantities.
	 */
	protected int nTotal;

	@Override
	public boolean permitsSampleStatistics() {
		if (species == null)
			return false;
		for (Module<?> mod : species) {
			if (mod.getMutation().getProbability() > 0.0 || !(mod instanceof HasHistogram.StatisticsProbability
					|| mod instanceof HasHistogram.StatisticsTime))
				return false;
		}
		return true;
	}

	@Override
	public boolean permitsUpdateStatistics() {
		if (species == null)
			return false;
		for (Module<?> mod : species) {
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
	 * Short-cut to {@code species.get(0).getIBSPopulation()} for single species
	 * models; {@code null} in multi-species models. Convenience field.
	 */
	protected IBSPopulation<?, ?> population;

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
	protected IBS(EvoLudo engine) {
		super(engine);
		type = ModelType.IBS;
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
		for (Module<?> mod : species) {
			IBSPopulation<?, ?> pop = createIBSPopulation(mod);
			mod.setIBSPopulation(pop);
		}
		// now that all populations are instantiated, we can assign opponents
		for (Module<?> mod : species) {
			IBSPopulation<?, ?> pop = mod.getIBSPopulation();
			// set opponents
			pop.setOpponentPop(mod.getOpponent().getIBSPopulation());
		}
		Module<?> main = species.get(0);
		// set shortcut for single species modules
		population = isMultispecies ? null : main.getIBSPopulation();
		cloGeometryInteraction.inheritKeysFrom(main.cloGeometry);
		cloGeometryCompetition.inheritKeysFrom(main.cloGeometry);
		if (isMultispecies)
			speciesUpdate = new SpeciesUpdate(main);
		statisticsSettings = new Statistics(this);
	}

	/**
	 * Factory method to create an {@link IBSPopulation} instance for the provided
	 * module type.
	 * 
	 * @param mod module requesting a population implementation
	 * @return instantiated population
	 */
	private IBSPopulation<?, ?> createIBSPopulation(Module<?> mod) {
		IBSPopulation<?, ?> pop = mod.createIBSPopulation();
		if (pop != null)
			return pop;
		if (mod instanceof org.evoludo.simulator.modules.Discrete)
			return new IBSDPopulation(engine, (org.evoludo.simulator.modules.Discrete) mod);
		if (mod instanceof org.evoludo.simulator.modules.Continuous) {
			// continuous module, check trait number
			if (mod.getNTraits() > 1)
				return new IBSMCPopulation(engine, (org.evoludo.simulator.modules.Continuous) mod);
			else
				return new IBSCPopulation(engine, (org.evoludo.simulator.modules.Continuous) mod);
		}
		engine.fatal("unknown module type '" + mod + "'... fix me!");
		// fatal does not return control
		return null;
	}

	@Override
	public void unload() {
		ephrng = null;
		population = null;
		cloGeometryInteraction.clearKeys();
		cloGeometryCompetition.clearKeys();
		cloMigration.clearKeys();
		for (Module<?> mod : species)
			mod.setIBSPopulation(null);
		speciesUpdate = null;
		statisticsSettings = null;
		super.unload();
	}

	@Override
	public boolean check() {
		boolean doReset = super.check();
		boolean anyUnique = false;
		isSynchronous = false;
		nTotal = 0;
		for (Module<?> mod : species) {
			IBSPopulation<?, ?> pop = mod.getIBSPopulation();
			doReset |= pop.check();
			nTotal += mod.getNPopulation();
			isSynchronous |= pop.getPopulationUpdate().isSynchronous();
			if (anyUnique)
				continue;
			AbstractGeometry geom = pop.interaction;
			anyUnique |= geom.isUnique();
			if (!geom.isSingle()) {
				geom = pop.competition;
				anyUnique |= geom.isUnique();
			}
		}
		// make sure all populations use same update scheme
		for (Module<?> mod : species) {
			PopulationUpdate pu = mod.getIBSPopulation().getPopulationUpdate();
			if (pu.isSynchronous() != isSynchronous) {
				pu.setType(PopulationUpdate.Type.SYNC);
				logger.warning("forcing " + PopulationUpdate.Type.SYNC + " for population " + mod.getName() + ".");
				doReset = true;
			}
		}
		if (isMultispecies && speciesUpdate.getType() == SpeciesUpdate.Type.FITNESS && !positiveMinFitness()) {
			// fitness based picking of focal species requires positive fitness
			logger.warning("multispecies models with '" + SpeciesUpdate.Type.FITNESS
					+ "' require positive minimum fitness - switching to '"
					+ SpeciesUpdate.Type.RATE + "'");
			speciesUpdate.setType(SpeciesUpdate.Type.RATE);
		}
		// update converged flag to account for changes that preclude convergence
		if (!doReset) {
			for (Module<?> mod : species) {
				IBSPopulation<?, ?> pop = mod.getIBSPopulation();
				converged &= pop.checkConvergence();
			}
		}
		// if no geometries are unique no need to reset model for statistics (init is
		// sufficient)
		if (!anyUnique)
			statisticsSettings.resetInterval = 0;
		return doReset;
	}

	@Override
	public void reset() {
		super.reset();
		// if any population uses ephemeral payoffs a dummy random number
		// generator is needed for the display
		for (Module<?> mod : species) {
			IBSPopulation<?, ?> pop = mod.getIBSPopulation();
			if (pop.getPlayerScoring().equals(ScoringType.EPHEMERAL)) {
				ephrng = rng.clone();
				break;
			}
		}
		// start with first species when taking turns
		nextSpeciesIdx = (speciesUpdate != null && speciesUpdate.getType() == SpeciesUpdate.Type.TURNS ? -1 : 0);
		for (Module<?> mod : species) {
			IBSPopulation<?, ?> pop = mod.getIBSPopulation();
			pop.reset();
		}
	}

	@Override
	public boolean setMode(Mode mode) {
		if (mode == Mode.STATISTICS_SAMPLE && fixData == null)
			fixData = new FixationData();
		else
			fixData = null;
		return super.setMode(mode);
	}

	@Override
	public void init() {
		init(false);
	}

	@Override
	protected void resetState() {
		super.resetState();
		updates = 0.0;
		time = (positiveMinFitness() ? 0.0 : Double.POSITIVE_INFINITY);
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
		connect = false;
		if (soft) {
			// signal change to engine without destroying state
			// used for simulations in systems with long relaxation times
			engine.paramsDidChange();
			return;
		}
		// initialize all populations
		for (Module<?> mod : species) {
			IBSPopulation<?, ?> pop = mod.getIBSPopulation();
			pop.init();
		}
		// check for convergence separately because initialization may want to
		// relax the configuration, which could result in convergence
		converged = true;
		for (Module<?> mod : species) {
			IBSPopulation<?, ?> pop = mod.getIBSPopulation();
			converged &= pop.checkConvergence();
		}
	}

	/**
	 * Checks whether all species have positive minimum fitness.
	 * 
	 * @return {@code true} if all species have positive minimum fitness
	 */
	private boolean positiveMinFitness() {
		for (Module<?> mod : species) {
			if (!(mod instanceof Payoffs)
					|| mod.getMap2Fitness().map(((Payoffs) mod).getMinPayoff()) <= 0.0)
				return false;
		}
		return true;
	}

	@Override
	public double getUpdates() {
		return updates;
	}

	@Override
	public double getTime() {
		return time;
	}

	@Override
	public String getCounter() {
		if (mode != Mode.DYNAMICS)
			return super.getCounter();
		String upd = "Updates: " + Formatter.format(getUpdates(), 2);
		if (time < Double.POSITIVE_INFINITY)
			return super.getCounter() + " (" + upd + ")";
		return upd;
	}

	@Override
	public void update() {
		// all populations need to be updated/reset before scores can be calculated for
		// inter-species interactions
		for (Module<?> mod : species) {
			if (mod instanceof Payoffs) {
				IBSPopulation<?, ?> pop = mod.getIBSPopulation();
				pop.resetScores();
			}
		}
		for (Module<?> mod : species) {
			if (mod instanceof Payoffs) {
				IBSPopulation<?, ?> pop = mod.getIBSPopulation();
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
				// check if STOP has been requested
				return engine.isRunning();
			}
			initStatisticsSample();
			update();
			return true;
		}
		// convergence only signaled without mutations
		if (converged) {
			return false;
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
		return (updates > nextHalt || Math.abs(nextHalt - updates) >= minIncr);
	}

	/**
	 * Pointer to focal species for debugging.
	 */
	IBSPopulation<?, ?> debugFocalSpecies;

	/**
	 * Advances the IBS model by a step of size {@code stepDt}. The actual time
	 * increment may be shorter, e.g. upon reaching an absorbing state or
	 * homogeneous state, if requested.
	 * <p>
	 * <strong>Note:</strong> the time increment returned is negative if the IBS
	 * converged/absorbed (individual based simulations cannot reverse time).
	 * 
	 * @param stepDt the time increment requested for advancing the IBS model
	 * @return <code>true</code> if <code>ibsStep(double)</code> can be called
	 *         again.
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
		return isSynchronous ? ibsStepSync(stepDt) : ibsStepAsync(stepDt);
	}

	/**
	 * Advances the IBS model by a step of size {@code stepDt}. This corresponds to
	 * {@code nTotal * stepDt} individual updates, where {@code nTotal} is the total
	 * population size across all species. The actual number of updates may be less
	 * than {@code stepDt} if the simulation has converged.
	 * 
	 * @param stepDt the time increment requested for advancing the IBS model
	 * @return <code>true</code> if <code>ibsStep(double)</code> can be called
	 *         again.
	 */
	private boolean ibsStepSync(double stepDt) {
		// reset traits and collect totals based on current populations
		double totRate = resetTraits();
		double gincr = 1.0 / nTotal;
		// nUpdates measured in generations
		int nUpdates = Math.max(1, (int) Math.floor(stepDt));
		for (int f = 0; f < nUpdates; f++) {
			// update populations
			int dt = 0;
			for (Module<?> mod : species) {
				IBSPopulation<?, ?> pop = mod.getIBSPopulation();
				pop.prepareTraits();
				dt += pop.step();
				pop.isConsistent();
			}
			// advance time and real time (if possible)
			updates += dt * gincr;
			if (time < Double.POSITIVE_INFINITY)
				time += RNGDistribution.Exponential.next(rng.getRNG(), dt / totRate);
			// commit traits and reset scores
			for (Module<?> mod : species) {
				IBSPopulation<?, ?> pop = mod.getIBSPopulation();
				pop.commitTraits(); // also check homogeneity
				// TODO: review migration - should be an independent event, independent of
				// population update
				// NOTE: should time advance? e.g. based on number of mutants
				pop.doSyncMigration(); // do migration
				// all scores must be reset before we can re-calculate them
				pop.resetScores();
			}
			// calculate new scores (requires that all traits are committed and reset)
			totRate = updateScores();
			if (converged)
				return false;
		}
		return true;
	}

	/**
	 * Advances the IBS model by a step of size {@code stepDt}. This corresponds to
	 * {@code nTotal * stepDt} individual updates, where {@code nTotal} is the total
	 * population size across all species. The actual number of updates may be less
	 * than
	 * {@code stepDt} if the simulation has converged.
	 * 
	 * @param stepDt the time increment requested for advancing the IBS model
	 * @return <code>true</code> if <code>ibsStep(double)</code> can be called
	 *         again.
	 */
	private boolean ibsStepAsync(double stepDt) {
		// reset traits and collect totals based on current populations
		double totRate = resetTraits();
		// gincr is a constant because based on total maximum population sizes
		double gincr = 1.0 / nTotal;
		// process at least one update.
		// NOTE: nUpdates can exceed Integer.MAX_VALUE (notably for large populations
		// and long relaxation times). using long is not an option because of GWT!
		double dUpdates = Math.max(1.0, Math.ceil(stepDt * nTotal - 1e-8));
		double stepDone = 0.0;
		double gStart = updates;
		while (dUpdates >= 1.0) {
			int nUpdates = Math.min((int) dUpdates, 1000000000); // 1e9 about half of Integer.MAX_VALUE (2.1e9)
			int processed = processEvents(nUpdates, gincr, totRate);
			double stepSize = processed * gincr;
			stepDone += Math.abs(stepSize);
			updates = gStart + Math.abs(stepDone);
			dUpdates = (stepDt - stepDone) / gincr;
			if (converged)
				break;
		}
		resetScores();
		return !converged;
	}

	/**
	 * Resets the traits for all populations and returns the total update rate
	 * across all species.
	 * 
	 * @return the total update rate across all species
	 */
	private double resetTraits() {
		double totRate = 0.0;
		for (Module<?> mod : species) {
			IBSPopulation<?, ?> pop = mod.getIBSPopulation();
			pop.resetTraits();
			totRate += pop.getSpeciesUpdateRate();
		}
		return totRate;
	}

	/**
	 * Resets the scores for all populations that use ephemeral payoffs.
	 */
	private void resetScores() {
		for (Module<?> mod : species) {
			IBSPopulation<?, ?> pop = mod.getIBSPopulation();
			if (!pop.playerScoring.equals(ScoringType.EPHEMERAL))
				continue;
			RNGDistribution freeze = rng;
			rng = ephrng;
			pop.resetScores();
			pop.updateScores();
			rng = freeze;
		}
	}

	/**
	 * For <em>asynchronous</em> updates, updates the scores for all populations,
	 * checks convergence across all species and returns the total update rate.
	 * 
	 * @return the total update rate across all species
	 */
	private double checkConvergence() {
		converged = true;
		double totRate = 0.0;
		for (Module<?> mod : species) {
			IBSPopulation<?, ?> pop = mod.getIBSPopulation();
			pop.isConsistent();
			converged &= pop.checkConvergence();
			totRate += pop.getSpeciesUpdateRate();
		}
		return totRate;
	}

	/**
	 * For <em>synchronous</em> updates, updates the scores for all populations,
	 * checks convergence across all species and returns the total update rate.
	 * 
	 * @return the total update rate across all species
	 */
	private double updateScores() {
		converged = true;
		double totRate = 0.0;
		for (Module<?> mod : species) {
			IBSPopulation<?, ?> pop = mod.getIBSPopulation();
			pop.updateScores();
			converged &= pop.checkConvergence();
			totRate += pop.getSpeciesUpdateRate();
		}
		return totRate;
	}

	/**
	 * Processes up to {@code nUpdates} events, updating time and updates
	 * appropriately.
	 * <p>
	 * Ecological updates use the thinning method for simulating nonhomogeneous
	 * Poisson processes by advancing time on rejected proposals.
	 * 
	 * @param nUpdates the maximum number of updates to process
	 * @param gincr    the growth increment
	 * @param totRate  the total update rate
	 * @return the number of processed updates
	 * 
	 * @see Lewis, P. A. W. & Shedler, G. S. (1979). “Simulation of nonhomogeneous
	 *      Poisson processes by thinning.” Naval Research Logistics Quarterly,
	 *      26(3), 403–413. DOI: 10.1002/nav.3800260304
	 */
	private int processEvents(int nUpdates, double gincr, double totRate) {
		int n = 0;
		while (n < nUpdates) {
			// update event
			int dt = processEvent();
			PopulationUpdate.Type updateType = debugFocalSpecies.getPopulationUpdate().getType();
			if (dt > 0) {
				if (updateType == PopulationUpdate.Type.ONCE) {
					updates += dt;
					n += debugFocalSpecies.getPopulationSize();
				} else {
					updates += dt * gincr;
					n += dt;
				}
				if (time < Double.POSITIVE_INFINITY)
					time += RNGDistribution.Exponential.next(rng.getRNG(), dt / totRate);
			} else if (dt == 0 && time < Double.POSITIVE_INFINITY && updateType == PopulationUpdate.Type.ECOLOGY)
				// thinning logic: advance time even if ecological update failed
				time += RNGDistribution.Exponential.next(rng.getRNG(), 1.0 / totRate);
			if (dt != 0) {
				// dt < 0 indicates extinction of a species
				totRate = checkConvergence();
			}
			if (converged)
				return n;
		}
		return nUpdates;
	}

	/**
	 * Processes a single event.
	 * 
	 * @return the number of elapsed realtime units
	 */
	private int processEvent() {
		debugFocalSpecies = pickFocalSpecies();
		switch (pickEvent(debugFocalSpecies)) {
			case REPLICATION:
				return debugFocalSpecies.step();
			case MUTATION:
				return debugFocalSpecies.mutate();
			default:
				engine.fatal("unknown event type...");
				return 0; // unreachable
		}
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
	@SuppressWarnings("java:S1452") // impossible to specify generic type here
	IBSPopulation<?, ?> getIBSPopulation(int id) {
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
		Module<?> mod = species.get(id);
		Map2Fitness map2fit = mod.getMap2Fitness();
		return map2fit.map(((Payoffs) mod).getMinPayoff());
	}

	@Override
	public double getMaxFitness(int id) {
		Module<?> mod = species.get(id);
		Map2Fitness map2fit = mod.getMap2Fitness();
		return map2fit.map(((Payoffs) mod).getMaxPayoff());
	}

	@Override
	public String getStatus() {
		if (isMultispecies) {
			StringBuilder sb = new StringBuilder();
			for (Module<?> mod : species) {
				IBSPopulation<?, ?> pop = mod.getIBSPopulation();
				if (sb.length() > 0) {
					sb.append("<br/><i>");
				} else {
					sb.append("<i>");
				}
				sb.append(mod.getName()).append(":</i> ").append(pop.getStatus());
			}
			return sb.toString();
		}
		return population.getStatus();
	}

	@Override
	public int getNMean() {
		if (isMultispecies) {
			int nMean = 0;
			for (Module<?> mod : species)
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
			for (Module<?> mod : species) {
				IBSPopulation<?, ?> pop = mod.getIBSPopulation();
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
			for (Module<?> mod : species) {
				IBSPopulation<?, ?> pop = mod.getIBSPopulation();
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
		IBSPopulation<?, ?> pop = getIBSPopulation(id);
		if (!pop.mouseHitNode(hit, alt)) {
			// nothing changed
			return false;
		}
		// update converged
		converged = true;
		for (Module<?> mod : species) {
			pop = mod.getIBSPopulation();
			converged &= pop.checkConvergence();
		}
		return true;
	}

	/**
	 * Type of species update (multi-species models only).
	 */
	SpeciesUpdate speciesUpdate;

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
	@SuppressWarnings("java:S1452") // impossible to specify generic type here
	public IBSPopulation<?, ?> pickFocalSpecies() {
		if (!isMultispecies)
			return population;
		double[] rates = new double[nSpecies];
		int idx = 0;
		double total = 0.0;
		switch (speciesUpdate.getType()) {
			case FITNESS:
				for (Module<?> mod : species) {
					IBSPopulation<?, ?> pop = mod.getIBSPopulation();
					double rate = pop.getTotalFitness();
					rates[idx++] = rate;
					total += rate;
				}
				return pickFocalSpecies(rates, total);
			case SIZE:
				for (Module<?> mod : species) {
					IBSPopulation<?, ?> pop = mod.getIBSPopulation();
					double rate = pop.getPopulationSize();
					rates[idx++] = rate;
					total += rate;
				}
				return pickFocalSpecies(rates, total);
			case RATE:
				for (Module<?> mod : species) {
					IBSPopulation<?, ?> pop = mod.getIBSPopulation();
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
				throw new UnsupportedOperationException("Unknown species update type!");
		}
	}

	/**
	 * Pick focal species with a probability proportional to the entries in
	 * {@code rates}.
	 * 
	 * @param rates the rates with which to pick the focal species
	 * @param total the sum of the rates
	 * @return the focal population or <code>null</code> if all populations extinct
	 */
	private IBSPopulation<?, ?> pickFocalSpecies(double[] rates, double total) {
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
	private IBSPopulation<?, ?> pickFocalSpecies(int skip) {
		if (!isMultispecies)
			return population;
		int speciesIdx = nextSpeciesIdx + skip;
		for (int i = 0; i < nSpecies; i++) {
			nextSpeciesIdx = (speciesIdx + i) % nSpecies;
			IBSPopulation<?, ?> pop = species.get(nextSpeciesIdx).getIBSPopulation();
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
	protected Event pickEvent(IBSPopulation<?, ?> pop) {
		Mutation mu = pop.module.getMutation();
		if (mu.temperature || mu.getProbability() <= 0.0 || random01() > mu.getProbability())
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
			CLOCategory.Model,
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
				public boolean parse(boolean isSet) {
					// default is to average scores
					for (Module<?> mod : species) {
						IBSPopulation<?, ?> pop = mod.getIBSPopulation();
						pop.setPlayerScoreAveraged(!isSet);
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
			CLOption.Argument.REQUIRED, CLOCategory.Model,
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
					String[] playerresets = arg.contains(CLOParser.SPECIES_DELIMITER)
							? arg.split(CLOParser.SPECIES_DELIMITER)
							: arg.split(CLOParser.VECTOR_DELIMITER);
					int n = 0;
					for (Module<?> mod : species) {
						IBSPopulation<?, ?> pop = mod.getIBSPopulation();
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
			CLOCategory.Model,
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
					String[] interactiontypes = arg.contains(CLOParser.SPECIES_DELIMITER)
							? arg.split(CLOParser.SPECIES_DELIMITER)
							: arg.split(CLOParser.VECTOR_DELIMITER);
					int n = 0;
					for (Module<?> mod : species) {
						IBSPopulation<?, ?> pop = mod.getIBSPopulation();
						String intertype = interactiontypes[n++ % interactiontypes.length];
						IBSGroup.SamplingType intt = (IBSGroup.SamplingType) cloInteractions.match(intertype);
						IBSGroup group = pop.getInterGroup();
						if (intt == null)
							return false;
						group.setSampling(intt);
						// parse n, if present
						String[] args = intertype.split(CLOParser.SPLIT_ARG_REGEX);
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
			CLOCategory.Model,
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
					String[] referencetypes = arg.contains(CLOParser.SPECIES_DELIMITER)
							? arg.split(CLOParser.SPECIES_DELIMITER)
							: arg.split(CLOParser.VECTOR_DELIMITER);
					int n = 0;
					for (Module<?> mod : species) {
						IBSPopulation<?, ?> pop = mod.getIBSPopulation();
						String reftype = referencetypes[n++ % referencetypes.length];
						IBSGroup.SamplingType reft = (IBSGroup.SamplingType) cloReferences.match(reftype);
						IBSGroup group = pop.getCompGroup();
						if (reft == null)
							return false;
						group.setSampling(reft);
						// parse n, if present
						String[] args = reftype.split(CLOParser.SPLIT_ARG_REGEX);
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
	public final CLOption cloMigration = new CLOption("migration", "none", CLOCategory.Model,
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
					String[] migrationtypes = arg.contains(CLOParser.SPECIES_DELIMITER)
							? arg.split(CLOParser.SPECIES_DELIMITER)
							: arg.split(CLOParser.VECTOR_DELIMITER);
					int n = 0;
					for (Module<?> mod : species) {
						IBSPopulation<?, ?> pop = mod.getIBSPopulation();
						String migt = migrationtypes[n++ % migrationtypes.length];
						MigrationType mt = (MigrationType) cloMigration.match(migt);
						if (mt != null) {
							pop.setMigrationType(mt);
							if (mt == MigrationType.NONE) {
								pop.setMigrationProb(0.0);
							} else {
								String keyarg = CLOption.stripKey(mt, arg);
								if (!keyarg.isEmpty()) {
									pop.setMigrationProb(CLOParser.parseDouble(keyarg));
									double mig = pop.getMigrationProb();
									if (mig < 1e-8) {
										if (logger.isLoggable(Level.WARNING))
											logger.warning((isMultispecies ? mod.getName() + ": " : "")
													+ "migration rate too small (" + Formatter.formatSci(mig, 4)
													+ ") - reverting to no migration");
										pop.setMigrationType(MigrationType.NONE);
										pop.setMigrationProb(0.0);
									}
								}
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
	 * @see GeometryType
	 * @see #cloGeometryCompetition
	 * @see Module#cloGeometry
	 */
	public final CLOption cloGeometryInteraction = new CLOption("geominter", null, CLOCategory.Model,
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
					if (arg != null) {
						String[] geomargs = arg.split(CLOParser.SPECIES_DELIMITER);
						boolean doReset = false;
						int n = 0;
						for (Module<?> mod : species) {
							// creates new interaction geometry
							IBSPopulation<?, ?> ibs = mod.getIBSPopulation();
							String geomarg = geomargs[n++ % geomargs.length];
							AbstractGeometry current = ibs.interaction;
							AbstractGeometry next = AbstractGeometry.create(engine, geomarg, current);
							if (next != current) {
								next.parse();
								ibs.interaction = next;
								doReset = true;
							}
						}
						engine.requiresReset(doReset);
					}
					return true;
				}
			});

	/**
	 * Command line option to set the competition geometry. This overrides the
	 * {@link Module#cloGeometry} settings.
	 * 
	 * @see GeometryType
	 * @see #cloGeometryInteraction
	 * @see Module#cloGeometry
	 */
	public final CLOption cloGeometryCompetition = new CLOption("geomcomp", null, CLOCategory.Model,
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
					if (arg != null) {
						String[] geomargs = arg.split(CLOParser.SPECIES_DELIMITER);
						boolean doReset = false;
						int n = 0;
						for (Module<?> mod : species) {
							// creates new competition geometry
							IBSPopulation<?, ?> ibs = mod.getIBSPopulation();
							String geomarg = geomargs[n % geomargs.length];
							AbstractGeometry current = ibs.competition;
							AbstractGeometry next = AbstractGeometry.create(engine, geomarg, current);
							if (next != current) {
								next.parse();
								ibs.competition = next;
								doReset = true;
							}
							n++;
						}
						engine.requiresReset(doReset);
					}
					return true;
				}
			});

	/**
	 * Command line option to set the fraction of links to rewire. If graph is
	 * undirected it is preserved and if graph is directed, rewiring is done for
	 * directed links (where undirected links count as two directed links), which
	 * potentially breaks undirected ones.
	 */
	public final CLOption cloGeometryRewire = new CLOption("rewire", "0", CLOCategory.Model,
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
					for (Module<?> mod : species) {
						IBSPopulation<?, ?> pop = mod.getIBSPopulation();
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
	public final CLOption cloGeometryAddwire = new CLOption("addwire", "0", CLOCategory.Model,
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
					for (Module<?> mod : species) {
						IBSPopulation<?, ?> pop = mod.getIBSPopulation();
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
			CLOCategory.Model,
			"--consistency   check consistency of scores etc.", new CLODelegate() {

				/**
				 * {@inheritDoc}
				 * <p>
				 * Parse method to enable consistency checks.
				 * 
				 * @param arg the ignored argument
				 */
				@Override
				public boolean parse(boolean isSet) {
					for (Module<?> mod : species) {
						IBSPopulation<?, ?> pop = mod.getIBSPopulation();
						pop.setConsistencyCheck(isSet);
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
		for (Module<?> mod : species) {
			int vacant = mod.getVacantIdx();
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
		IBSPopulation<?, ?> ibs = species.get(0).getIBSPopulation();
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
	public enum ScoringType implements CLOption.Key {

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
	public enum MigrationType implements CLOption.Key {

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
		public final CLOption clo = new CLOption("statistics", "reset 1", CLOCategory.Simulation,
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
							String[] typeargs = st.split(CLOParser.SPLIT_ARG_REGEX);
							resetInterval = typeargs.length > 1 ? CLOParser.parseInteger(typeargs[1]) : 1;
						}
						return true;
					}

					@Override
					public String getDescription() {
						return "--statistics <s>  settings:\n" + clo.getDescriptionKey();
					}
				});

		/**
		 * Type of statistics.
		 * <dl>
		 * <dt>RESET_GEOMETRY
		 * <dd>Reset geometry every {@code s} samples (never for {@code s&le;0}).
		 * </dl>
		 */
		public enum Type implements CLOption.Key {

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
		super.encodeState(plist);
		plist.append(Plist.encodeKey("Generation", updates));
		boolean isMultiSpecies = (species.size() > 1);
		for (Module<?> mod : species) {
			IBSPopulation<?, ?> pop = mod.getIBSPopulation();
			if (isMultiSpecies)
				plist.append("<key>").append(mod.getName()).append("</key>\n")
						.append("<dict>\n");
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
		super.restoreState(plist);
		updates = (Double) plist.get("Generation");
		connect = false;
		if (species.size() > 1) {
			boolean success = true;
			for (Module<?> mod : species) {
				success |= restorePopulationState((Plist) plist.get(mod.getName()), mod.getIBSPopulation(),
						mod.getName());
			}
			return success;
		}
		// single species
		return restorePopulationState(plist, population, null);
	}

	/**
	 * Restore the state of population {@code pop} from plist {@code plist}.
	 * 
	 * @param plist the plist with the population state
	 * @param pop   the population to restore
	 * @param name  the name of the population (for logging)
	 * @return {@code true} if the restoration was successful, {@code false}
	 *         otherwise
	 */
	private boolean restorePopulationState(Plist plist, IBSPopulation<?, ?> pop, String name) {
		boolean success = true;
		if (!pop.restoreGeometry(plist)) {
			logRestoreWarning("geometry", name);
			success = false;
		}
		if (!pop.restoreInteractions(plist)) {
			logRestoreWarning("interactions", name);
			success = false;
		}
		if (!pop.restoreTraits(plist)) {
			logRestoreWarning("traits", name);
			success = false;
		}
		if (!pop.restoreFitness(plist)) {
			logRestoreWarning("fitness", name);
			success = false;
		}
		return success;
	}

	/**
	 * Log warning message for failed restoration of {@code what} in population
	 * {@code name}.
	 * 
	 * @param what the name of the population attribute that failed to restore
	 * @param name the name of the population (for logging)
	 */
	private void logRestoreWarning(String what, String name) {
		if (logger.isLoggable(Level.WARNING))
			logger.warning("restore " + what + " in " + type.getKey() + "-model failed"
					+ (name != null ? " (" + name + ")." : "."));
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
