package org.evoludo.simulator.models;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Logger;

import org.evoludo.math.Combinatorics;
import org.evoludo.math.RNGDistribution;
import org.evoludo.simulator.ColorMap;
import org.evoludo.simulator.EvoLudo;
import org.evoludo.simulator.Geometry;
import org.evoludo.simulator.modules.Module;
import org.evoludo.util.CLOParser;
import org.evoludo.util.CLOption;
import org.evoludo.util.CLOption.CLODelegate;
import org.evoludo.util.Formatter;
import org.evoludo.util.Plist;

/**
 * Base class for individual based simulation models, IBS. This class deals with
 * multiple species, handles the milestone events and delegates to the different
 * populations as appropriate.
 * 
 * @author Christoph Hauert
 */
public abstract class IBS implements Model.IBS {

	/**
	 * Modules that offer individual based simulation models must implement this
	 * interface.
	 * 
	 * @author Christoph Hauert
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
		public default Model.IBS createIBS() {
			return null;
		}
	}

	/**
	 * Indicates current mode of IBS model.
	 */
	protected Mode mode = Mode.DYNAMICS;

	/**
	 * Checks whether the mode {code test} is supported by this model.
	 * 
	 * @param test the mode to test
	 * @return {@code true} if mode is supported
	 * 
	 */
	@Override
	public boolean permitsMode(Mode test) {
		for (IBSPopulation pop : species) {
			if (!pop.permitsMode(test))
				return false;
		}
		return true;
	}

	@Override
	public boolean setMode(Mode mode) {
		if (!permitsMode(mode))
			return false;
		if (this.mode == mode)
			return true;
		this.mode = mode;
		if (mode == Mode.STATISTICS)
			init();
		return true;
	}

	@Override
	public Mode getMode() {
		return mode;
	}

	/**
	 * <code>true</code> if new sample for statistics should be started
	 * ({@link EvoLudo#modelInit()} will be called on next update).
	 */
	protected boolean statisticsSampleNew = false;

	/**
	 * Number of statistics samples collected.
	 */
	protected int nStatisticsSamples = 0;

	/**
	 * Gets the number of statistics samples collected so far.
	 * 
	 * @return the number of samples
	 */
	public int getNStatisticsSamples() {
		return nStatisticsSamples;
	}

	@Override
	public void initStatisticsSample() {
		statisticsSampleNew = false;
	}

	/**
	 * The pacemaker of all models. Interface with the outside world.
	 */
	protected EvoLudo engine;

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
	 * List with the populations of all species in model including this one. List
	 * should be shared with other populations (to simplify bookkeeping) but the
	 * species list CANNOT be static! Otherwise it is impossible to run multiple
	 * instances of modules/models concurrently in a single browser window using GWT.
	 */
	protected ArrayList<IBSPopulation> species;

	/**
	 * Gets the list of species (populations) in the IBS model.
	 * <p>
	 * <strong>Note:</strong> This is different from {@link Module#getSpecies()}!
	 * which returns a list of {@link Module}s that characterize the features of the
	 * different species.
	 * 
	 * @return the list of populations
	 * 
	 * @see Module#getSpecies()
	 */
	public ArrayList<IBSPopulation> getSpecies() {
		return species;
	}

	/**
	 * Gets the species (population) in the IBS model, which is associated with
	 * {@code module}.
	 * 
	 * @param module the module for which to retrieve the associated IBS population
	 * @return the IBS population associated with {@code module}
	 * 
	 * @see #getSpecies()
	 */
	public IBSPopulation getSpecies(Module module) {
		return mod2pop.get(module);
	}

	/**
	 * The number of species in multi-species models.
	 */
	protected int nSpecies;

	/**
	 * Flag to indicate whether the model entertains multiple species, i.e.
	 * {@code nSpecies&gt;1}. Convenience field.
	 */
	protected boolean isMultispecies;

	/**
	 * Map to link {@link Module}s with {@link IBSPopulation}s.
	 */
	protected HashMap<Module, IBSPopulation> mod2pop;

	/**
	 * Add {@link IBSPopulation} to list of species and map to {@link Module}.
	 * Duplicate entries are ignored. Allocate new list if necessary.
	 *
	 * @param mod the module governing the interactions in the population
	 * @param pop the population to add to species list.
	 * @return <code>true</code> if {@code pop} is successfully added;
	 *         <code>false</code> if {@code pop} is already included in list.
	 */
	public boolean addSpecies(Module mod, IBSPopulation pop) {
		// do not add duplicates
		if (species.contains(pop))
			return false;
		if (!species.add(pop))
			return false;
		mod2pop.put(mod, pop);
		pop.setModule(mod);
		nSpecies = species.size();
		if (nSpecies == 1) {
			isMultispecies = false;
			return true;
		}
		isMultispecies = true;
		return true;
	}

	/**
	 * Short-cut to <code>species.get(0)</code> for single species models;
	 * <code>null</code> in multi-species models. Convenience field.
	 */
	protected IBSPopulation population;

	/**
	 * Keeps track of the number of generations (or Monte-Carlo steps) that have
	 * elapsed. In a population of size <code>N</code> one generation corresponds to
	 * <code>N</code> updates, which translates to <code>N</code> events (birth,
	 * death, imitation, etc.).
	 * <p>
	 * <strong>Note:</strong> generally differs from 'real time' (see
	 * {@link #realtime}). <code>generation==0</code> after {@link #reset()} and at
	 * the beginning of a simulation run. <code>generation</code> is incremented
	 * <em>before</em> the next event is processed, to reflect the time at which the
	 * event occurs.
	 */
	protected double generation = -1.0;

	/**
	 * Keeps track of the elapsed time, taking into account the fitness of the
	 * population. For example, less time passes between reproductive events in
	 * populations with high fitness, while more time passes in low fitness
	 * populations because there are fewer reproduction events per unit time. If
	 * individual scares can be negative {@code realtime} is set to
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
	 * <li>currently restricted to discrete strategies where homogeneous population
	 * states can be skipped by deterministically introducing new mutant after an
	 * geometrically (exponentially) distributed waiting time (see
	 * {@link IBSD}).</li>
	 * <li>requires small mutation rates.</li>
	 * <li>does not work for variable population sizes.</li>
	 * </ul>
	 */
	public boolean optimizeHomo = false;

	/**
	 * Creates a population of individuals for IBS simulations.
	 * 
	 * @param engine the pacemeaker for running the model
	 */
	public IBS(EvoLudo engine) {
		this.engine = engine;
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
		logger = engine.getLogger();
		rng = engine.getRNG();
		species = new ArrayList<IBSPopulation>();
		mod2pop = new HashMap<Module, IBSPopulation>();
		for (Module mod : engine.getModule().getSpecies()) {
			IBSPopulation pop = mod.createIBSPop();
			if (pop == null) {
				if (mod instanceof org.evoludo.simulator.modules.Discrete) {
					pop = new IBSDPopulation(engine);
				} else if (mod instanceof org.evoludo.simulator.modules.Continuous.MultiPairs ||
						mod instanceof org.evoludo.simulator.modules.Continuous.MultiGroups) {
					// note: must check MultiContinuous first because it extends Continuous
					pop = new IBSMCPopulation(engine);
				} else if (mod instanceof org.evoludo.simulator.modules.Continuous) {
					pop = new IBSCPopulation(engine);
				} else {
					engine.fatal("unknown module type '" + mod + "'... fix me!");
					// fatal does not return control
				}
			}
			addSpecies(mod, pop);
			pop.load();
		}
		// set shortcut for single species modules
		population = isMultispecies ? null : species.get(0);
		// IBSPopulation(s) created, now set their opponents
		for (IBSPopulation pop : species)
			pop.opponent = mod2pop.get(pop.getModule().getOpponent());
		IBSPopulation pop = species.get(0);
		cloGeometryInteraction.inheritKeysFrom(pop.getModule().cloGeometry);
		cloGeometryReproduction.inheritKeysFrom(pop.getModule().cloGeometry);
		cloPopulationUpdate.addKeys(PopulationUpdateType.values());
		// ToDo: further updates to implement or make standard
		cloPopulationUpdate.removeKey(PopulationUpdateType.WRIGHT_FISHER);
		cloPopulationUpdate.removeKey(PopulationUpdateType.ECOLOGY);
		cloMigration.addKeys(MigrationType.values());
	}

	@Override
	public void unload() {
		rng = null;
		ephrng = null;
		logger = null;
		cloGeometryInteraction.clearKeys();
		cloGeometryReproduction.clearKeys();
		cloPopulationUpdate.clearKeys();
		cloMigration.clearKeys();
		for (IBSPopulation pop : species)
			pop.unload();
		species = null;
		mod2pop = null;
	}

	/**
	 * Flag to indicate whether the population updates are synchronous. In
	 * multi-species models this requires that all species are updated
	 * synchronously. Helper variable for {@code ibsStep(double)}.
	 * 
	 * @see #cloPopulationUpdate
	 * @see #check()
	 * @see #ibsStep(double)
	 */
	boolean isSynchronous;

	@Override
	public boolean check() {
		boolean doReset = false;
		boolean allSync = true, allAsync = true;
		for (IBSPopulation pop : species) {
			doReset |= pop.check();
			boolean sync = pop.getPopulationUpdateType().isSynchronous();
			allSync &= sync;
			allAsync &= !sync;
		}
		isSynchronous = !allAsync;
		if (isSynchronous && !allSync) {
			logger.warning("cannot (yet) mix synchronous and asynchronous population updates - forcing '"
					+ PopulationUpdateType.SYNC + "'");
			for (IBSPopulation pop : species)
				pop.setPopulationUpdateType(PopulationUpdateType.SYNC);
			doReset = true;
		}
		return doReset;
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
		// reset time
		generation = 0.0;
		realtime = 0.0;
		converged = false;
		connect = false;
		if (soft) {
			// signal change to engine without destroying state
			// used for simulations in systems with long relaxation times
			engine.paramsDidChange();
			return;
		}
		converged = true;
		for (IBSPopulation pop : species) {
			pop.init();
			converged &= pop.checkConvergence();
		}
	}

	@Override
	public void reset() {
		nStatisticsSamples = 0;

		// with optimization, homogeneous populations do not wait for next mutation
		// currently only relevant for discrete strategies, see IBSD
		if (optimizeHomo) {
			// NOTE: optimizeHomo == false if pMutation <= 0.0 or isMultispecies == true
			double pMutation = population.getModule().getMutationProb();
			if (distrMutation == null)
				distrMutation = new RNGDistribution.Geometric(rng.getRNG(), pMutation);
			else
				distrMutation.setProbability(pMutation);
		}
		// if any population uses ephemeral payoffs a dummy random number
		// generator is needed for the display
		for (IBSPopulation pop : species) {
			if (pop.getPlayerScoring().equals(ScoringType.EPHEMERAL)) {
				ephrng = rng.clone();
				break;
			}
		}
		nextSpeciesIdx = -1;
		for (IBSPopulation pop : species)
			pop.reset();
	}

	@Override
	public void update() {
		// all populations need to be updated/reset before scores can be calculated for
		// inter-species interactions
		for (IBSPopulation pop : species)
			pop.resetScores();
		for (IBSPopulation pop : species)
			pop.updateScores();
	}

	/**
	 * Flag to indicate whether the model has converged. Once a model has converged
	 * the model execution automatically stops.
	 */
	protected boolean converged = false;

	@Override
	public boolean next() {
		// start new statistics sample if required
		if (isMode(Mode.STATISTICS) && statisticsSampleNew) {
			engine.modelInit();
			engine.modelUpdate();
			// debugCheck("next (new sample)");
			return true;
		}
		// convergence only signaled if pMutation <= 0
		if (converged && !optimizeHomo) {
			engine.fireModelStopped();
			return false;
		}
		// this implies single species
		// (otherwise optimizeHomo == false and population == null)
		if (optimizeHomo && population.isMonomorphic()) {
			engine.fireModelRunning();
			// optimize waiting time in homogeneous states by advancing time and
			// deterministically introduce new mutant (currently single species only)
			// optimizeHomo also requires that pMutation is small (otherwise this
			// optimization is pointless); more specifically pMutation<0.1/nPopulation
			// such that mutations occur less than every 10 generations and hence
			// scores can be assumed to be homogeneous when the mutant arises.
			Module module = population.getModule();
			double realnorm = 1.0 / (population.getTotalFitness() * module.getSpeciesUpdateRate());
			int nPop = module.getNPopulation();
			double norm = 1.0 / nPop;
			int skip = distrMutation.next();
			generation += skip * norm;
			realtime += skip * realnorm;
			population.resetStrategies();
			update();
			// communicate update
			engine.fireModelChanged();
//XXX this can easily skip past requested stops - ignore? does not make much sense anyways.
			realtime += realnorm;
			generation += norm;
			int rand = random0n(nPop);
			population.mutateStrategyAt(rand, false);
			if (population.adjustScores)
				population.adjustGameScoresAt(rand);
			else {
				population.resetScoreAt(rand);
				population.commitStrategyAt(rand);
				population.playGameAt(rand);
			}
			return true;
		}
		double nextHalt = engine.getNextHalt();
		// continue if milestone reached in previous step, i.e. deltat < 1e-8
		double step = engine.getReportInterval();
		double incr = Math.abs(nextHalt - generation);
		if (incr < 1e-8)
			return false;
		step = Math.min(step, incr);
		double deltat = ibsStep(step);
		connect = true;
		// converged if deltat<0
		if (deltat < 0.0) {
			converged = true;
			return false;
		}
		// note: time resolution is limited. need to allow for some slack when
		// signalling convergence. using 1/nPopulation of the first species is 
		// an approximation but hopefully good enough. deviations expected in
		// multi-species modules with different population sizes or different 
		// update rates.
		double minIncr = 1.0 / species.get(0).nPopulation;
		return (Math.abs(nextHalt - generation) >= minIncr);
	}

	/**
	 * The flag to indicate whether the model is currently relaxing the initial configuration.
	 */
	boolean isRelaxing = false;

	@Override
	public boolean relax(double generations) {
		if (generations < 1.0)
			return false;
		isRelaxing = true;
		double rf = engine.getReportInterval();
		engine.setReportInterval(generations);
		boolean cont = next();
		engine.setReportInterval(rf);
		isRelaxing = false;
		return cont;
	}

	@Override
	public boolean relaxing() {
		return isRelaxing;
	}

	/**
	 * Advances the IBS model by a step of size {@code stepDt}. The actual time
	 * increment may be shorter, e.g. upon reaching an absorbing state or
	 * homogeneous state, if requested.
	 * <p>
	 * <strong>Note:</strong> the time increment returned is negative if the IBS
	 * converged/absorbed (individual based simulations cannot reverse time).
	 * 
	 * @param stepDt the time increment requested for advancing the IBS model
	 * @return actual time increment (in generations)
	 * 
	 * @see org.evoludo.simulator.modules.Discrete#cloMonoStop
	 */
	public double ibsStep(double stepDt) {
		// synchronous population updates
		if (isSynchronous) {
			double incr = 0.0;
			int nPopTot = 0;
			double scoreTot = 0.0;
			// reset strategies (colors)
			for (IBSPopulation pop : species) {
				pop.resetStrategies();
				nPopTot += pop.getPopulationSize();
				scoreTot += pop.getTotalFitness();
			}
			// nUpdates measured in generations
			int nUpdates = Math.max(1, (int) Math.floor(stepDt));
			for (int f = 0; f < nUpdates; f++) {
				// advance time and update strategies
				double realtimeIncr = (scoreTot <= 1e-8 ? Double.POSITIVE_INFINITY : nPopTot / scoreTot);
				realtime += realtimeIncr;
				generation++;
				for (IBSPopulation pop : species) {
					pop.prepareStrategies();
					incr += pop.step();
					pop.isConsistent();
				}
				// commit strategies and reset scores
				for (IBSPopulation pop : species) {
					pop.commitStrategies(); // also check homogeneity
					// TODO: review migration - should be an independent event, independent of
					// population update
					// NOTE: should time advance? e.g. based on number of mutants
					pop.doSyncMigration(); // do migration
					// all scores must be reset before we can re-calculate them
					pop.resetScores();
				}
				// calculate new scores (requires that all strategies are committed and reset)
				boolean hasConverged = true;
				for (IBSPopulation pop : species) {
					pop.updateScores();
					hasConverged &= pop.checkConvergence();
					scoreTot += pop.getTotalFitness();
				}
				if (hasConverged)
					return -incr;
			}
			return incr;
		}

		// asynchronous population update - update one individual at a time
		double wPopTot = 0.0;
		double wScoreTot = 0.0;
		// reset strategies (colors)
		for (IBSPopulation pop : species) {
			pop.resetStrategies();
			Module module = pop.getModule();
			double rate = module.getSpeciesUpdateRate();
			// determine generation time and real time increments
			// NOTE: generation time increments based on maximum population sizes and do not
			// take potentially fluctuating sizes into account (relevant for ecological
			// settings)
			// wPopTot += pop.getPopulationSize() * rate;
			// use maximum population size - otherwise the notion of a generation gets
			// confusing
			wPopTot += module.getNPopulation() * rate;
			double sum = pop.getTotalFitness();
			if (wScoreTot >= 0.0 && sum <= 1e-8) {
				wScoreTot = -1.0;
				continue;
			}
			wScoreTot += sum * rate;
		}
		// if wPopTot is based on maximum population size, gincr is a constant
		//TODO: how to define a generation in populations with varying size? realtime
		// only? in particular, gincr is no longer constant if based on actual
		// population size.
		double gincr = 1.0 / wPopTot;
		// round nUpdates up while trying to avoid rounding errors.
		// process at least one update.
		// note: nUpdates can exceed Integer.MAX_VALUE (notably for large populations,
		// long relaxation times and high interaction rates of species). however,
		// switching to long is not an option because of GWT!
		double dUpdates = Math.max(1.0, Math.ceil(stepDt / gincr - 1e-8));
		double stepDone = 0.0;
		double gStart = generation;
		boolean hasConverged = false;
		while (dUpdates >= 1.0) {
			double stepSize = 0.0;
			int nUpdates = Math.min((int) dUpdates, 1000000000); // 1e9 about half of Integer.MAX_VALUE (2.1e9)
			for (int n = 0; n < nUpdates; n++) {
				// update event
				double rincr = (wScoreTot < 0.0 ? 0.0 : 1.0 / (wScoreTot * wScoreTot));
				// advance time
				generation += gincr;
				IBSPopulation focal = pickFocalSpecies();
				realtime += rincr / focal.step();
				realtime = wScoreTot < 0.0 ? Double.POSITIVE_INFINITY : realtime;
				wPopTot = wScoreTot = 0.0;
				hasConverged = true;
				for (IBSPopulation pop : species) {
					pop.isConsistent();
					hasConverged &= pop.checkConvergence();
					// update generation time and real time increments
					double rate = pop.getModule().getSpeciesUpdateRate();
					wPopTot += pop.getPopulationSize() * rate;
					double sum = pop.getTotalFitness();
					if (wScoreTot >= 0.0 && sum <= 1e-8) {
						wScoreTot = -1.0;
						continue;
					}
					wScoreTot += sum * rate;
				}
				if (hasConverged)
					break;
				// if wPopTot is based on maximum population size, gincr is a constant
				// gincr = 1.0 / wPopTot;
			}
			stepSize = nUpdates * gincr;
			stepDone += Math.abs(stepSize);
			generation = gStart + Math.abs(stepDone);
			if (hasConverged)
				// cannot return just yet; still need to update ephemeral scores
				break;
			dUpdates = (stepDt - stepDone) / gincr;
		}
		for (IBSPopulation pop : species) {
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
		if (hasConverged)
			return -stepDone;
		return stepDt;
	}

	@Override
	public double processMonoScore(int id, double score) {
		if (isMultispecies)
			return species.get(id).processMonoScore(score);
		return population.processMonoScore(score);
	}

	@Override
	public double processMinScore(int id, double min) {
		if (isMultispecies)
			return species.get(id).processMinScore(min);
		return population.processMinScore(min);
	}

	@Override
	public double processMaxScore(int id, double max) {
		if (isMultispecies)
			return species.get(id).processMaxScore(max);
		return population.processMaxScore(max);
	}

	@Override
	public String getStatus() {
		if (isMultispecies) {
			String status = "";
			for (IBSPopulation pop : species)
				status += (status.length() > 0 ? "<br/><i>" : "<i>") + pop.getModule().getName() + ":</i> "
						+ pop.getStatus();
			return status;
		}
		return population.getStatus();
	}

	@Override
	public String getCounter() {
		return "time: " + Formatter.format(getTime(), 2) + " ("
				+ Formatter.format(getRealtime(), 2) + ")";
	}

	@Override
	public double getTime() {
		return generation;
	}

	@Override
	public double getRealtime() {
		return realtime;
	}

	@Override
	public boolean hasConverged() {
		return converged;
	}

	@Override
	public void getInitialTraits(double[] init) {
		if (isMultispecies) {
			int skip = 0;
			double[] tmp = new double[init.length];
			for (IBSPopulation pop : species) {
				pop.getInitialTraits(tmp);
				System.arraycopy(tmp, 0, init, skip, pop.nTraits);
				skip += pop.nTraits;
			}
		} else
			population.getInitialTraits(init);
	}

	@Override
	public void getInitialTraits(int id, double[] init) {
		if (isMultispecies)
			species.get(id).getInitialTraits(init);
		else
			population.getInitialTraits(init);
	}

	@Override
	public boolean getMeanTraits(double[] mean) {
		if (isMultispecies) {
			int skip = 0;
			double[] tmp = new double[mean.length];
			for (IBSPopulation pop : species) {
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
		if (isMultispecies)
			species.get(id).getMeanTraits(mean);
		else
			population.getMeanTraits(mean);
		return connect;
	}

	@Override
	public String getTraitNameAt(int id, int idx) {
		if (isMultispecies)
			return species.get(id).getTraitNameAt(idx);
		return population.getTraitNameAt(idx);
	}

	@Override
	public <T> void getTraitData(int id, T[] colors, ColorMap<T> colorMap) {
		if (isMultispecies)
			species.get(id).getTraitData(colors, colorMap);
		else
			population.getTraitData(colors, colorMap);
	}

	@Override
	public boolean getMeanFitness(double[] mean) {
		if (isMultispecies) {
			int skip = 0;
			double[] tmp = new double[mean.length];
			for (IBSPopulation pop : species) {
				Module mod = pop.getModule();
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
		if (isMultispecies)
			species.get(id).getMeanFitness(mean);
		else
			population.getMeanFitness(mean);
		return connect;
	}

	@Override
	public String getFitnessNameAt(int id, int idx) {
		if (isMultispecies)
			return species.get(id).getFitnessNameAt(idx);
		return population.getFitnessNameAt(idx);
	}

	@Override
	public String getScoreNameAt(int id, int idx) {
		if (isMultispecies)
			return species.get(id).getScoreNameAt(idx);
		return population.getScoreNameAt(idx);
	}

	@Override
	public <T> void getFitnessData(int id, T[] colors, ColorMap.Gradient1D<T> colorMap) {
		if (isMultispecies)
			species.get(id).getFitnessData(colors, colorMap);
		else
			population.getFitnessData(colors, colorMap);
	}

	@Override
	public void getFitnessHistogramData(int id, double[][] bins) {
		if (isMultispecies)
			species.get(id).getFitnessHistogramData(bins);
		else
			population.getFitnessHistogramData(bins);
	}

	@Override
	public String getTagNameAt(int id, int idx) {
		if (isMultispecies)
			return species.get(id).getTagNameAt(idx);
		return population.getTagNameAt(idx);
	}

	@Override
	public int getInteractionsAt(int id, int idx) {
		if (isMultispecies)
			return species.get(id).getInteractionsAt(idx);
		return population.getInteractionsAt(idx);
	}

	@Override
	public boolean mouseHitNode(int id, int hit, boolean alt) {
		if (!(isMultispecies ? species.get(id).mouseHitNode(hit, alt) : population.mouseHitNode(hit, alt))) {
			// nothing changed
			return false;
		}
		// update converged
		converged = true;
		for (IBSPopulation pop : species) {
			converged &= pop.checkConvergence();
		}
		return true;
	}

	/**
	 * Flag to indicate whether the current data point belongs to the same time
	 * series. This is used by the GUI to decide whether to connect the data points
	 * or not. Typically this is false only after {@link #init()}, {@link #reset()},
	 * {@link #restoreState(Plist)} or similar.
	 */
	protected boolean connect;

	@Override
	public boolean isConnected() {
		return connect;
	}

	/**
	 * Type of species update (multi-species models only).
	 */
	public SpeciesUpdateType speciesUpdateType = SpeciesUpdateType.SIZE;

	/**
	 * Set species update to <code>type</code>.
	 * 
	 * @param type update type of species
	 */
	public void setSpeciesUpdateType(SpeciesUpdateType type) {
		speciesUpdateType = type;
	}

	/**
	 * @return species update type.
	 */
	public SpeciesUpdateType getSpeciesUpdateType() {
		return speciesUpdateType;
	}

	/**
	 * Pick focal population according to the selected scheme.
	 * 
	 * @return focal population
	 * 
	 * @see SpeciesUpdateType
	 */
	public IBSPopulation pickFocalSpecies() {
		if (!isMultispecies)
			return population;
		switch (speciesUpdateType) {
			case FITNESS:
				double wScoreTot = 0.0;
				for (IBSPopulation pop : species)
					wScoreTot += pop.getTotalFitness() * pop.getModule().getSpeciesUpdateRate();
				return pickFocalSpeciesFitness(wScoreTot);
			case SIZE:
				double wPopTot = 0.0;
				for (IBSPopulation pop : species)
					wPopTot += pop.getPopulationSize() * pop.getModule().getSpeciesUpdateRate();
				return pickFocalSpeciesSize(wPopTot);
			case TURNS:
				// case SYNC:
				return pickFocalSpeciesTurns();
			default:
				throw new Error("unknown species update type!");
		}
	}

	/**
	 * Pick species to update with a probability proportional to the size of the
	 * species weighted by its update rate.
	 * 
	 * @param wPopTot the sum of the population sizes weighted by the corresponding
	 *                species' update rate
	 * @return the focal population
	 * 
	 * @see Module#cloSpeciesUpdateRate
	 */
	private IBSPopulation pickFocalSpeciesSize(double wPopTot) {
		if (!isMultispecies)
			return population;
		double rand = random01() * wPopTot;
		for (IBSPopulation pop : species) {
			rand -= pop.getPopulationSize() * pop.getModule().getSpeciesUpdateRate();
			if (rand < 0.0)
				return pop;
		}
		// should not get here
		return null;
	}

	/**
	 * Pick species to update with a probability proportional to the total fitness
	 * of the species weighted by its update rate.
	 * 
	 * @param wScoreTot the sum of the population sizes weighted by the
	 *                  corresponding
	 *                  species' update rate
	 * @return the focal population
	 * 
	 * @see Module#cloSpeciesUpdateRate
	 */
	private IBSPopulation pickFocalSpeciesFitness(double wScoreTot) {
		if (!isMultispecies)
			return population;
		double rand = random01() * wScoreTot;
		for (IBSPopulation pop : species) {
			rand -= pop.getTotalFitness() * pop.getModule().getSpeciesUpdateRate();
			if (rand < 0.0)
				return pop;
		}
		// should not get here
		return null;
	}

	/**
	 * Index for turn-based-selection to determine which species to pick next.
	 * Simply cycles through species array.
	 */
	private int nextSpeciesIdx = -1;

	/**
	 * Pick species for sequential updates, i.e. pick one population after another
	 * for updating.
	 * 
	 * @return focal population
	 */
	private IBSPopulation pickFocalSpeciesTurns() {
		if (!isMultispecies)
			return population;
		nextSpeciesIdx = (nextSpeciesIdx + 1) % nSpecies;
		return species.get(nextSpeciesIdx);
	}

	/**
	 * Command line option to set the method for updating the population(s).
	 * 
	 * @see PopulationUpdateType
	 */
	public final CLOption cloPopulationUpdate = new CLOption("popupdate", PopulationUpdateType.ASYNC.getKey(), EvoLudo.catModel, 
			"--popupdate <u> [<p>]  population update type; fraction p\n" + //
			"                for synchronous updates (1=all):", new CLODelegate() {

				/**
				 * {@inheritDoc}
				 * <p>
				 * Parse population update types for a single or multiple populations/species.
				 * <code>arg</code> can be a single value or an array of values with the
				 * separator {@value CLOParser#SPECIES_DELIMITER}. The parser cycles through
				 * <code>arg</code> until all populations/species have the population update
				 * type set.
				 * 
				 * @param arg the (array of) update types
				 */
				@Override
				public boolean parse(String arg) {
					String[] popupdates = arg.split(CLOParser.SPECIES_DELIMITER);
					int n = 0;
					boolean success = true;
					for (IBSPopulation pop : species) {
						String updt = popupdates[n++ % popupdates.length];
						PopulationUpdateType put = (PopulationUpdateType) cloPopulationUpdate.match(updt);
						if (put == null) {
							if (success)
								logger.warning((species.size() > 1 ? pop.getModule().getName() + ": " : "") + //
										"population update '" + updt + "' not recognized - using '"
										+ pop.getPopulationUpdateType()
										+ "'");
							success = false;
							continue;
						}
						pop.setPopulationUpdateType(put);
						updt = CLOption.stripKey(put, updt);
						// parse p, if present
						String[] args = updt.split("[ =]");
						double sync = 1.0;
						if (args.length > 1)
							sync = CLOParser.parseDouble(args[1]);
						pop.setSyncFraction(sync);
					}
					return success;
				}

				@Override
				public void report(PrintStream output) {
					for (IBSPopulation pop : species) {
						String opt = (pop instanceof IBSDPopulation && ((IBSDPopulation) pop).optimizeMoran
								? " (optimized)"
								: "");
						output.println("# populationupdate:     " + pop.getPopulationUpdateType() + opt
								+ (isMultispecies ? " (" + pop.getModule().getName() + ")" : ""));
					}
				}
			});

	/**
	 * Command line option to set whether player scores from interactions are
	 * accumulated or averaged (default).
	 * <p>
	 * <strong>Note:</strong> Accumulated scores can be tricky because they are
	 * essentially unbounded... On regular structures the two variants merely amount
	 * to a rescaling of the selection strength.
	 * 
	 * @see Module#cloSelection
	 */
	public final CLOption cloAccumulatedScores = new CLOption("accuscores", "noaccu", CLOption.Argument.NONE, EvoLudo.catModel, 
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
					for (IBSPopulation pop : species)
						pop.setPlayerScoreAveraged(!cloAccumulatedScores.isSet());
					return true;
				}

				@Override
				public void report(PrintStream output) {
					for (IBSPopulation pop : species)
					output.println("# scoring:              " + //
						(pop.getPlayerScoreAveraged() ? "averaged" : "accumulated") + //
						(isMultispecies	? " (" + pop.getModule().getName() + ")" : ""));
	}
			});

	/**
	 * Command line option to set method for resetting the scores of individuals.
	 * 
	 * @see ScoringType
	 */
	public final CLOption cloScoringType = new CLOption("resetscores", ScoringType.RESET_ALWAYS.getKey(), CLOption.Argument.REQUIRED, EvoLudo.catModel, 
			"--resetscores <t>  type for restting scores t:",
			new CLODelegate() {

				/**
				 * {@inheritDoc}
				 * <p>
				 * Parse method for restting the scores of individuals.
				 * 
				 * @param arg the method for resetting the scores.
				 */
				@Override
				public boolean parse(String arg) {
					String[] playerresets = arg.split(CLOParser.SPECIES_DELIMITER);
					int n = 0;
					boolean success = true;
					for (IBSPopulation pop : species) {
						String rest = playerresets[n++ % playerresets.length];
						ScoringType st = (ScoringType) cloScoringType.match(rest);
						if (st == null) {
							if (success)
								logger.warning((species.size() > 1 ? pop.getModule().getName() + ": " : "") + //
										"method to reset scores '" + rest + "' not recognized - using '"
										+ pop.getPlayerScoring()
										+ "'");
							success = false;
							continue;
						}
						pop.setPlayerScoring(st);
					}
					return success;
				}

				@Override
				public void report(PrintStream output) {
					for (IBSPopulation pop : species)
						output.println("# resetscores:          " + pop.getPlayerScoring() + //
							(isMultispecies	? " (" + pop.getModule().getName() + ")" : ""));
				}
			});

	/**
	 * Command line option to set whether players interact with all their neighbours
	 * or a random subsample.
	 */
	public final CLOption cloInteractions = new CLOption("interactions", IBSGroup.SamplingType.ALL.getKey(), EvoLudo.catModel,
			"--interactions <t [n]> select interaction type t:",
			new CLODelegate() {

				/**
				 * {@inheritDoc}
				 * <p>
				 * Parse interactions of players with their neighbours in a single or multiple
				 * populations/species. <code>arg</code> can be a single value or an array of
				 * values with the separator {@value CLOParser#SPECIES_DELIMITER}. The parser
				 * cycles through <code>arg</code> until all populations/species have the player
				 * interactions set.
				 * 
				 * @param arg the (array of) interaction(s)
				 */
				@Override
				public boolean parse(String arg) {
					boolean success = true;
					String[] interactiontypes = arg.split(CLOParser.SPECIES_DELIMITER);
					int n = 0;
					for (IBSPopulation pop : species) {
						String intertype = interactiontypes[n++ % interactiontypes.length];
						IBSGroup.SamplingType intt = (IBSGroup.SamplingType) cloInteractions.match(intertype);
						IBSGroup group = pop.getInteractionGroup();
						if (intt == null) {
							if (success) {
								IBSGroup.SamplingType st = group.getSampling();
								logger.warning((isMultispecies ? pop.getModule().getName() + ": " : "") + //
										"interaction type '" + intertype + //
										"' unknown - using '" + st + //
										(st == IBSGroup.SamplingType.RANDOM ? " " + group.getNSamples() : "") + "'");
							}
							success = false;
							continue;
						}
						group.setSampling(intt);
						intertype = CLOption.stripKey(intt, intertype).trim();
						// parse n, if present
						String[] args = intertype.split("[ =]");
						int nInter = 1;
						if (args.length > 1)
							nInter = CLOParser.parseInteger(args[1]);
						group.setNSamples(nInter);
					}
					return success;
				}

				@Override
				public void report(PrintStream output) {
					for (IBSPopulation pop : species) {
						IBSGroup group = pop.getInteractionGroup();
						IBSGroup.SamplingType st = group.getSampling();
						output.println("# interactions:         " + st + //
							(st == IBSGroup.SamplingType.RANDOM ? " " + group.getNSamples() : "") + //
							(isMultispecies ? " (" + pop.getModule().getName() + ")" : ""));
					}
				}
			});

	/**
	 * Command line option to set the method for choosing references/models among
	 * the neighbours of a player for updating their strategy.
	 */
	public final CLOption cloReferences = new CLOption("references", IBSGroup.SamplingType.RANDOM.getKey() + " 1", EvoLudo.catModel,
			"--references <t [n]> select reference type t:",
			new CLODelegate() {

				/**
				 * {@inheritDoc}
				 * <p>
				 * Parse method for choosing references/models among the neighbours of a player
				 * for updating their strategy in a single or multiple populations/species.
				 * <code>arg</code> can be a single value or an array of values with the
				 * separator {@value CLOParser#SPECIES_DELIMITER}. The parser cycles through
				 * <code>arg</code> until all populations/species have the player's references
				 * set.
				 * 
				 * @param arg the (array of) reference type(s)
				 */
				@Override
				public boolean parse(String arg) {
					boolean success = true;
					String[] referencetypes = arg.split(CLOParser.SPECIES_DELIMITER);
					int n = 0;
					for (IBSPopulation pop : species) {
						String reftype = referencetypes[n++ % referencetypes.length];
						IBSGroup.SamplingType reft = (IBSGroup.SamplingType) cloReferences.match(reftype);
						IBSGroup group = pop.getReferenceGroup();
						if (reft == null) {
							if (success) {
								IBSGroup.SamplingType st = group.getSampling();
								logger.warning((isMultispecies ? pop.getModule().getName() + ": " : "") + //
										"reference type '" + reftype + //
										"' unknown - using '" + st + //
										(st == IBSGroup.SamplingType.RANDOM ? " " + group.getNSamples() : "") + "'");
							}
							success = false;
							continue;
						}
						group.setSampling(reft);
						reftype = CLOption.stripKey(reft, reftype);
						// parse n, if present
						String[] args = reftype.split("[ =]");
						int nInter = 1;
						if (args.length > 1)
							nInter = CLOParser.parseInteger(args[1]);
						group.setNSamples(nInter);
					}
					return success;
				}

				@Override
				public void report(PrintStream output) {
					for (IBSPopulation pop : species) {
						if (pop.getPopulationUpdateType().isMoran())
							continue;
						IBSGroup group = pop.getReferenceGroup();
						IBSGroup.SamplingType st = group.getSampling();
						output.println("# references:           " + st + //
							(st == IBSGroup.SamplingType.RANDOM ? " " + group.getNSamples() : "") + //
							(isMultispecies ? " (" + pop.getModule().getName() + ")" : ""));
					}
				}
			});

	/**
	 * Command line option to set the migration types and probabilities of players.
	 */
	public final CLOption cloMigration = new CLOption("migration", "none", EvoLudo.catModel,
			"--migration <tp>  migration (t type, p probability)",
			new CLODelegate() {

				/**
				 * {@inheritDoc}
				 * <p>
				 * Parse migration types and probabilities of players in a single or multiple
				 * populations/species. <code>arg</code> can be a single value or an array of
				 * values with the separator {@value CLOParser#SPECIES_DELIMITER}. The parser
				 * cycles through <code>arg</code> until all populations/species have the
				 * migration types and probabilities of players set.
				 * 
				 * @param arg the (array of) migration type(s) and probability(ies)
				 */
				@Override
				public boolean parse(String arg) {
					boolean success = true;
					String[] migrationtypes = arg.split(CLOParser.SPECIES_DELIMITER);
					int n = 0;
					for (IBSPopulation pop : species) {
						String migt = migrationtypes[n++ % migrationtypes.length];
						MigrationType mt = (MigrationType) cloMigration.match(migt);
						if (mt == null) {
							if (success)
								logger.warning((isMultispecies ? pop.getModule().getName() + ": " : "")
										+ "migration type '" + migt
										+ "' unknown - using '" + pop.getMigrationType() + "'");
							success = false;
							continue;
						}
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
								logger.warning((isMultispecies ? pop.getModule().getName() + ": " : "")
										+ "migration rate too small (" + Formatter.formatSci(mig, 4)
										+ ") - reverting to no migration");
								success = false;
								pop.setMigrationType(MigrationType.NONE);
								pop.setMigrationProb(0.0);
							}
							continue;
						}
						logger.warning((isMultispecies ? pop.getModule().getName() + ": " : "")
								+ "no migration rate specified - reverting to no migration");
						success = false;
						pop.setMigrationType(MigrationType.NONE);
						pop.setMigrationProb(0.0);
					}
					return success;
				}

				@Override
				public void report(PrintStream output) {
					for (IBSPopulation pop : species) {
						MigrationType mig = pop.getMigrationType();
						output.println("# migration:            " + mig + (isMultispecies ? " ("
								+ pop.getModule().getName() + ")" : ""));
						if (mig != MigrationType.NONE)
							output.println("# migrationrate:        " + Formatter.formatSci(pop.getMigrationProb(), 8)
									+ (isMultispecies ? " ("
											+ pop.getModule().getName() + ")" : ""));
					}
				}
			});

	/**
	 * Command line option to set the interaction geometry. This overrides the
	 * {@link Module#cloGeometry} settings.
	 * 
	 * @see Geometry.Type
	 * @see #cloGeometryReproduction
	 * @see Module#cloGeometry
	 */
	public final CLOption cloGeometryInteraction = new CLOption("geominter", "M", EvoLudo.catModel,
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
					for (IBSPopulation pop : species) {
						// creates new interaction geometry if null or equal to getGeometry()
						Geometry geom = pop.createInteractionGeometry();
						doReset |= geom.parse(geomargs[n++ % geomargs.length]);
					}
					engine.requiresReset(doReset);
					return true;
				}

				@Override
				public void report(PrintStream output) {
					for (IBSPopulation pop : species) {
						Geometry intergeo = pop.getInteractionGeometry();
						// interaction geometry can be null for pde models
						if (intergeo == null || intergeo.interReproSame)
							continue;
						output.println("# interactiongeometry:  " + intergeo.getType().getTitle() +
								(isMultispecies ? " (" + pop.getModule().getName() + ")" : ""));
						intergeo.printParams(output);
					}
				}
			});

	/**
	 * Command line option to set the reproduction geometry. This overrides the
	 * {@link Module#cloGeometry} settings.
	 * 
	 * @see Geometry.Type
	 * @see #cloGeometryInteraction
	 * @see Module#cloGeometry
	 */
	public final CLOption cloGeometryReproduction = new CLOption("geomrepro", "M", EvoLudo.catModel,
			"--geomrepro <>  reproduction geometry (see --geometry)",
			new CLODelegate() {

				/**
				 * {@inheritDoc}
				 * <p>
				 * Parse reproduction geometry in a single or multiple populations/species.
				 * <code>arg</code> can be a single value or an array of values with the
				 * separator {@value CLOParser#SPECIES_DELIMITER}. The parser cycles through
				 * <code>arg</code> until all populations/species have the reproduction geometry
				 * set.
				 * 
				 * @param arg the (array of) reproduction geometry(ies)
				 */
				@Override
				public boolean parse(String arg) {
					// only act if option has been explicitly specified - otherwise cloGeometry will
					// take care of this
					if (!cloGeometryReproduction.isSet())
						return true;
					String[] geomargs = arg.split(CLOParser.SPECIES_DELIMITER);
					boolean doReset = false;
					int n = 0;
					for (IBSPopulation pop : species) {
						// creates new reproduction geometry if null or equal to getGeometry()
						Geometry geom = pop.createReproductionGeometry();
						doReset |= geom.parse(geomargs[n++ % geomargs.length]);
					}
					engine.requiresReset(doReset);
					return true;
				}

				@Override
				public void report(PrintStream output) {
					for (IBSPopulation pop : species) {
						Geometry reprogeo = pop.getReproductionGeometry();
						// reproduction geometry can be null for pde models
						if (reprogeo == null || reprogeo.interReproSame)
							continue;
						output.println("# reproductiongeometry: " + reprogeo.getType().getTitle() +
								(isMultispecies ? " (" + pop.getModule().getName() + ")" : ""));
						reprogeo.printParams(output);
					}
				}
			});

	/**
	 * Command line option to set the fraction of links to rewire. If graph is
	 * undirected it is preserved and if graph is directed, rewiring is done for
	 * directed links (where undirected links count as two directed links), which
	 * potentially breaks undirected ones.
	 */
	public final CLOption cloGeometryRewire = new CLOption("rewire", "-1", EvoLudo.catModel,
			"--rewire <r>    rewire fraction r of graph links",
			new CLODelegate() {

				/**
				 * {@inheritDoc}
				 * <p>
				 * Parse fraction of links to rewire in geometry. {@code arg} rewiring of
				 * interaction and reproduction graphs can be specified by two values separated
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
					for (IBSPopulation pop : species) {
						double[] rewire = CLOParser.parseVector(rewireargs[n++ % rewireargs.length]);
						pop.setRewire(rewire);
					}
					return true;
				}

				@Override
				public void report(PrintStream output) {
					for (IBSPopulation pop : species) {
						Geometry intergeo = pop.getReproductionGeometry();
						output.println("# inter-rewiring: " + Formatter.format(intergeo.pRewire, 4) +
								(isMultispecies ? " (" + pop.getModule().getName() + ")" : ""));
						Geometry reprogeo = pop.getReproductionGeometry();
						// reproduction geometry can be null for pde models
						if (reprogeo == null)
							continue;
						output.println("# repro-rewiring: " + Formatter.format(reprogeo.pRewire, 4) +
								(isMultispecies ? " (" + pop.getModule().getName() + ")" : ""));
						reprogeo.printParams(output);
					}
				}
			});

	/**
	 * Command line option to set the fraction of links to add. If graph is
	 * undirected only undirected links are added and if graph is directed only
	 * directed links are added.
	 */
	public final CLOption cloGeometryAddwire = new CLOption("addwire", "-1", EvoLudo.catModel,
			"--addwire <a>   add fraction a of graph links",
			new CLODelegate() {

				/**
				 * {@inheritDoc}
				 * <p>
				 * Parse fraction of links to add in geometry. {@code arg} rewiring of
				 * interaction and reproduction graphs can be specified by two values separated
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
					for (IBSPopulation pop : species) {
						double[] add = CLOParser.parseVector(addargs[n++ % addargs.length]);
						pop.setAddwire(add);
					}
					return true;
				}

				@Override
				public void report(PrintStream output) {
					for (IBSPopulation pop : species) {
						Geometry intergeo = pop.getReproductionGeometry();
						output.println("# inter-add: " + Formatter.format(intergeo.pRewire, 4) +
								(isMultispecies ? " (" + pop.getModule().getName() + ")" : ""));
						Geometry reprogeo = pop.getReproductionGeometry();
						// reproduction geometry can be null for pde models
						if (reprogeo == null)
							continue;
						output.println("# repro-add: " + Formatter.format(reprogeo.pRewire, 4) +
								(isMultispecies ? " (" + pop.getModule().getName() + ")" : ""));
						reprogeo.printParams(output);
					}
				}
			});

	/**
	 * Command line option to set the method for selecting which species to update.
	 * 
	 * @see SpeciesUpdateType
	 */
	public final CLOption cloSpeciesUpdateType = new CLOption("speciesupdate", SpeciesUpdateType.SIZE.getKey(),
			EvoLudo.catModel, "--speciesupdate <u>  species update type", new CLODelegate() {

				/**
				 * {@inheritDoc}
				 * <p>
				 * Parse method for selecting which species to update in models with multiple
				 * populations/species. <code>arg</code> can be a single value or an array of
				 * values with the separator {@value CLOParser#SPECIES_DELIMITER}. The parser
				 * cycles through <code>arg</code> until all populations/species have the
				 * selection method set.
				 * 
				 * @param arg the (array of) selection method(s)
				 */
				@Override
				public boolean parse(String arg) {
					SpeciesUpdateType sut = (SpeciesUpdateType)cloSpeciesUpdateType.match(arg);
					if (sut == null) {
						logger.warning("species update '" + arg + "' unknown - using '"
								+ getSpeciesUpdateType() + "'");
						return false;
					}
					setSpeciesUpdateType(sut);
					return true;
				}

				@Override
				public void report(PrintStream output) {
					if (species.size() > 1)
						output.println("# speciesupdate:        " + getSpeciesUpdateType());
				}
			});

	/**
	 * Command line option to enable consistency checks.
	 */
	public final CLOption cloConsistency = new CLOption("consistency", "noconsistency", CLOption.Argument.NONE, EvoLudo.catModel,
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
					for (IBSPopulation pop : species)
						pop.setConsistencyCheck(cloConsistency.isSet());
					return true;
				}
			});

	@Override
	public void collectCLO(CLOParser parser) {
		if (species.size() > 1) {
			cloSpeciesUpdateType.addKeys(SpeciesUpdateType.values());
			parser.addCLO(cloSpeciesUpdateType);
		}
		parser.addCLO(cloPopulationUpdate);
		parser.addCLO(cloMigration);
		parser.addCLO(cloGeometryRewire);
		parser.addCLO(cloGeometryAddwire);
		parser.addCLO(cloConsistency);

		boolean anyVacant = false;
		boolean anyNonVacant = false;
		boolean allStatic = true;
		for (IBSPopulation pop : species) {
			int vacant = pop.getModule().getVacant();
			anyVacant |= vacant >= 0;
			anyNonVacant |= vacant < 0;
			allStatic &= pop.getModule().isStatic();
		}
		if (anyNonVacant) {
			// additional options that only make sense without vacant sites
			parser.addCLO(cloReferences);
			cloReferences.clearKeys();
			cloReferences.addKeys(IBSGroup.SamplingType.values());
		}
		if (anyVacant) {
			// restrict population updates to those compatible with ecological models
			cloPopulationUpdate.clearKeys();
			cloPopulationUpdate.addKey(PopulationUpdateType.ECOLOGY);
			cloPopulationUpdate.setDefault(PopulationUpdateType.ECOLOGY.getKey());
		}
		if (!allStatic) {
			// options that are only meaningful if at least some populations do not 
			// have static fitness
			parser.addCLO(cloAccumulatedScores);
			parser.addCLO(cloScoringType);
			cloScoringType.clearKeys();
			cloScoringType.addKeys(ScoringType.values());
			parser.addCLO(cloInteractions);
			cloInteractions.clearKeys();
			cloInteractions.addKeys(IBSGroup.SamplingType.values());
			parser.addCLO(cloGeometryInteraction);
			parser.addCLO(cloGeometryReproduction);
		}
	}

	/**
	 * Types of species updates (only relevant for multi-species models):
	 * <dl>
	 * <dt>size</dt>
	 * <dd>focal species selected proportional to their size</dd>
	 * <dt>fitness</dt>
	 * <dd>focal species selected proportional to their total fitness</dd>
	 * <dt>turns</dt>
	 * <dd>one species is selected after another.</dd>
	 * <dt>sync</dt>
	 * <dd>simultaneous updates of all species.</dd>
	 * </dl>
	 * For <em>size</em> and <em>fitness</em> selection is also proportional to the
	 * update rate of each species.
	 * 
	 * @see Module#speciesUpdateRate
	 */
	public static enum SpeciesUpdateType implements CLOption.Key {

		/**
		 * Pick focal species based on population size.
		 */
		SIZE("size", "pick species based on size"), //

		/**
		 * Pick focal species based on population fitness.
		 */
		FITNESS("fitness", "pick species based on fitness"), //

		/**
		 * Pick species sequentially.
		 */
		TURNS("turns", "pick species sequentially"); //

		/**
		 * Simultaneous updates of all species. Not implemented
		 */
		// SYNC("sync", "simultaneous updates of all species"); //

		/**
		 * Key of species update type. Used for parsing command line options.
		 * 
		 * @see org.evoludo.simulator.models.IBS#cloSpeciesUpdateType
		 *      IBS#cloSpeciesUpdateType
		 * @see org.evoludo.simulator.models.IBS#setSpeciesUpdateType(SpeciesUpdateType)
		 *      IBS.setSpeciesUpdateType(SpeciesUpdateType)
		 */
		String key;

		/**
		 * Brief description of species update type for GUI and help display.
		 * 
		 * @see EvoLudo#helpCLO()
		 */
		String title;

		/**
		 * Instantiate new species update type.
		 * 
		 * @param key   the identifier for parsing of command line options
		 * @param title the summary of the species update for GUI and help display
		 */
		SpeciesUpdateType(String key, String title) {
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
	 * <dt>synchronous</dt>
	 * <dd>Synchronized population updates. The number of individuals that reassess
	 * their strategy is determined by the player update noise,
	 * {@link Module#cloPlayerUpdateNoise}. Without noise all individuals update
	 * their strategy, while with high noise levels only a few update (but at least
	 * one individual and each at most once).</dd>
	 * <dt>Wright-Fisher</dt>
	 * <dd>Wright-Fisher process (synchronous)</dd>
	 * <dt>asynchronous</dt>
	 * <dd>Asynchronous population updates (default).</dd>
	 * <dt>Bd</dt>
	 * <dd>Moran process (birth-death, asynchronous).</dd>
	 * <dt>dB</dt>
	 * <dd>Moran process (death-birth, asynchronous).</dd>
	 * <dt>imitate</dt>
	 * <dd>Moran process (imitate, asynchronous).</dd>
	 * <dt>ecology</dt>
	 * <dd>Asynchronous updates (non-constant population size).</dd>
	 * </dl>
	 * For <b>size</b> and <b>fitness</b> selection is also proportional to the
	 * update rate of each species.
	 * 
	 * @see org.evoludo.simulator.models.IBS#cloPopulationUpdate
	 *      IBS.cloPopulationUpdate
	 * @see Module#cloPlayerUpdateNoise
	 */
	public static enum PopulationUpdateType implements CLOption.Key {

		/**
		 * Synchronized population updates. The number of individuals that reassess
		 * their strategy is determined by the player update noise,
		 * {@link Module#cloPlayerUpdateNoise}. Without noise all individuals update
		 * their strategy, while with high noise levels only a few update (but at least
		 * one individual and each at most once).
		 */
		SYNC("synchronous", "synchronized population updates"),

		/**
		 * Wright-Fisher process (synchronous). (not yet implemented!)
		 */
		WRIGHT_FISHER("Wright-Fisher", "Wright-Fisher process (synchronous)"),

		/**
		 * Asynchronous population updates.
		 */
		ASYNC("asynchronous", "asynchronous population updates"),

		/**
		 * Every individual updates exactly once per generation. In contrast for
		 * {@code ASYNC} every individual updates once <strong>on average</strong>.
		 */
		ONCE("once", "everyone updates once (asynchronous)"),

		/**
		 * Moran process (birth-death, asynchronous).
		 */
		MORAN_BIRTHDEATH("Bd", "Moran process (birth-death, asynchronous)"),

		/**
		 * Moran process (death-birth, asynchronous).
		 */
		MORAN_DEATHBIRTH("dB", "Moran process (death-birth, asynchronous)"),

		/**
		 * Moran process (imitate, asynchronous).
		 */
		MORAN_IMITATE("imitate", "Moran process (imitate, asynchronous)"),

		/**
		 * Asynchronous updates (non-constant population size).
		 */
		ECOLOGY("ecology", "asynchronous updates (non-constant population size)");

		/**
		 * Key of population update type. Used for parsing command line options.
		 * 
		 * @see org.evoludo.simulator.models.IBS#cloPopulationUpdate
		 *      IBS.cloPopulationUpdate
		 */
		String key;

		/**
		 * Brief description of population update type for GUI and help display.
		 * 
		 * @see EvoLudo#helpCLO()
		 */
		String title;

		/**
		 * Instantiate new type of population update.
		 * 
		 * @param key   identifier for parsing of command line option
		 * @param title the summary of geometry for GUI and help display
		 */
		PopulationUpdateType(String key, String title) {
			this.key = key;
			this.title = title;
		}

		/**
		 * Determine whether population update is synchronous.
		 * 
		 * @return {@code true} if update is synchronous
		 */
		public boolean isSynchronous() {
			return (equals(SYNC) || equals(WRIGHT_FISHER));
		}

		/**
		 * Determine whether population update is a variant of Moran updates. Moran type
		 * updates do not require specifying a player update type separately.
		 * 
		 * @return {@code true} if update is Moran
		 * 
		 * @see Module#cloPlayerUpdate
		 */
		public boolean isMoran() {
			return (equals(MORAN_BIRTHDEATH) || equals(MORAN_DEATHBIRTH) || equals(MORAN_IMITATE));
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
	 * 
	 * @see org.evoludo.simulator.modules.Module#speciesUpdateRate
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
		EPHEMERAL("ephemeral", "payoffs for updating only");

		/**
		 * Key of population update type. Used for parsing command line options.
		 * 
		 * @see org.evoludo.simulator.models.IBS#cloPopulationUpdate
		 *      IBS.cloPopulationUpdate
		 */
		String key;

		/**
		 * Brief description of population update type for GUI and help display.
		 * 
		 * @see EvoLudo#helpCLO()
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
	 * 
	 * @see org.evoludo.simulator.modules.Module#speciesUpdateRate
	 */
	public static enum MigrationType implements CLOption.Key {

		/**
		 * No migration.
		 */
		NONE("none", "no migration"),

		/**
		 * Diffusive migration (exchange of neighbors).
		 */
		DIFFUSION("D", "diffusive migration (exchange of neighbors)"),

		/**
		 * Birth-death migration (fit migrates, random death).
		 */
		BIRTH_DEATH("B", "birth-death migration (fit migrates, random death)"),

		/**
		 * Death-birth migration (random death, fit migrates).
		 */
		DEATH_BIRTH("d", "death-birth migration (random death, fit migrates)");

		/**
		 * Key of migration type. Used for parsing command line options.
		 * 
		 * @see org.evoludo.simulator.models.IBS#cloPopulationUpdate
		 *      IBS.cloPopulationUpdate
		 */
		String key;

		/**
		 * Brief description of migration type for GUI and help display.
		 * 
		 * @see EvoLudo#helpCLO()
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

	@Override
	public void encodeState(StringBuilder plist) {
		plist.append(Plist.encodeKey("Generation", getTime()));
		plist.append(Plist.encodeKey("Realtime", getRealtime()));
		plist.append(Plist.encodeKey("Model", getModelType().toString()));
		boolean isMultiSpecies = (species.size() > 1);
		for (IBSPopulation pop : species) {
			if (isMultiSpecies)
				plist.append("<key>" + pop.getModule().getName() + "</key>\n" + "<dict>\n");
			pop.encodeGeometry(plist);
			pop.encodeStrategies(plist);
			pop.encodeFitness(plist);
			pop.encodeInteractions(plist);
			if (isMultiSpecies)
				plist.append("</dict>");
		}
	}

	@Override
	public boolean restoreState(Plist plist) {
		generation = (Double) plist.get("Generation");
		realtime = (Double) plist.get("Realtime");
		connect = false;
		boolean success = true;
		if (species.size() > 1) {
			for (IBSPopulation pop : species) {
				Module module = pop.getModule();
				String name = module.getName();
				Plist pplist = (Plist) plist.get(name);
				if (!pop.restoreGeometry(pplist)) {
					logger.warning("restore geometry failed (" + module.getName() + ").");
					success = false;
				}
				if (!pop.restoreInteractions(pplist)) {
					logger.warning("restore interactions in " + getModelType() + "-model failed (" + name + ").");
					success = false;
				}
				if (!pop.restoreStrategies(pplist)) {
					logger.warning("restore strategies in " + getModelType() + "-model failed (" + name + ").");
					success = false;
				}
				if (!pop.restoreFitness(pplist)) {
					logger.warning("restore fitness in " + getModelType() + "-model failed (" + name + ").");
					success = false;
				}
			}
			return success;
		}
		IBSPopulation pop = species.get(0);
		if (!pop.restoreGeometry(plist)) {
			logger.warning("restore geometry failed.");
			success = false;
		}
		if (!pop.restoreInteractions(plist)) {
			logger.warning("restore interactions in " + getModelType() + "-model failed.");
			success = false;
		}
		if (!pop.restoreStrategies(plist)) {
			logger.warning("restore strategies in " + getModelType() + "-model failed.");
			success = false;
		}
		if (!pop.restoreFitness(plist)) {
			logger.warning("restore fitness in " + getModelType() + "-model failed.");
			success = false;
		}
		return success;
	}

	/**
	 * Set random seed
	 *
	 * @param s the seed for the random number generator
	 */
	public void srandom(long s) {
		rng.setRNGSeed(s);
	}

	/**
	 * random integer number from interval <code>[0, n]</code>.
	 *
	 * @param n the upper limit of interval (inclusive)
	 * @return the random integer number in <code>[0, n]</code>.
	 */
	public int random0N(int n) {
		return rng.random0N(n);
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

	/**
	 * Random number from interval [0, 1) with maximum 53bit resolution.
	 * <p>
	 * <strong>Note:</strong> takes twice as long as regular precision.
	 *
	 * @return the random number in <code>[0, 1)</code>.
	 */
	public double random01d() {
		return rng.random01d();
	}

	/**
	 * Generate Gaussian (normal) distributed random number.
	 * 
	 * @param mean the mean of the Gaussian distribution
	 * @param sdev the (standard deviation) of the Gaussian distribution
	 * @return the Gaussian distributed random number
	 */
	public double randomGaussian(double mean, double sdev) {
		return mean + sdev * rng.nextGaussian();
	}

	/**
	 * Generate binomially distributed random number.
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
		logger.warning(
				"What the heck are you doing here!!! (rand: " + uRand + ", p: " + p + ", n: " + n + " -> " + sum + ")");
		return -1;
	}
}
