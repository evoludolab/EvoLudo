package org.evoludo.simulator.models;

import java.io.PrintStream;

import org.evoludo.math.ArrayMath;
import org.evoludo.simulator.EvoLudo;
import org.evoludo.simulator.modules.Module;
import org.evoludo.util.CLOParser;
import org.evoludo.util.CLOption;
import org.evoludo.util.CLOption.CLODelegate;
import org.evoludo.util.Formatter;

/**
 * Base class for individual based simulation models, IBS, with discrete
 * traits/strategies. This class deals with optimizations, initialization types
 * and statistics but the heavy lifting is done by the {@link IBS} parent.
 * 
 * @author Christoph Hauert
 */
public class IBSD extends IBS implements Model.DiscreteIBS {

	/**
	 * Creates a population of individuals for IBS simulations with discrete
	 * traits/strategies.
	 * 
	 * @param engine the pacemeaker for running the model
	 */
	public IBSD(EvoLudo engine) {
		super(engine);
	}

	/**
	 * EXPERIMENTAL: container for fixation data; should probably become a more
	 * generic structure.
	 */
	protected FixationData fixData = null;

	/**
	 * EXPERIMENTAL: should mature into data structure useful for statistics
	 */
	public static class FixationData {
		/**
		 * The index of the node (location) where the initial mutant arose.
		 */
		public int mutantNode = -1;

		/**
		 * The strategy type of the initial mutant.
		 */
		public int mutantTrait = -1;

		/**
		 * The strategy type that reached fixation.
		 */
		public int typeFixed = -1;

		/**
		 * The number of updates until fixation was reached.
		 */
		public double updatesFixed = -1.0;

		/**
		 * The time until fixation in realtime units.
		 */
		public double timeFixed = -1.0;

		/**
		 * The flag indicating whether the fixation data (probabilities) has been read.
		 */
		public boolean probRead = true;

		/**
		 * The flag indicating whether the fixation times have been read.
		 */
		public boolean timeRead = true;

		/**
		 * Reset the fixation data to get ready for the next sample.
		 */
		public void reset() {
			mutantNode = -1;
			mutantTrait = -1;
			probRead = true;
			timeRead = true;
		}

		@Override
		public String toString() {
			return "{ mutantNode -> " + mutantNode + //
					", mutantTrait -> " + mutantTrait + // "
					", typeFixed -> " + typeFixed + //
					", updatesFixed -> " + Formatter.format(updatesFixed, 6) + //
					", timeFixed -> " + Formatter.format(timeFixed, 6) + //
					", probRead -> " + probRead + //
					", timeRead -> " + timeRead + " }";
		}
	}

	/**
	 * Gets the statistics sample.
	 * 
	 * @return the statistics sample
	 */
	public FixationData getFixationData() {
		return fixData;
	}

	@Override
	public void readStatisticsSample() {
		nStatisticsSamples++;
		statisticsSampleNew = true;
		if (fixData == null)
			return;

		// collect new statistics sample
		IBSDPopulation dpop = (IBSDPopulation) species.get(0);
		fixData.typeFixed = ArrayMath.maxIndex(dpop.strategiesTypeCount);
		Module module = dpop.getModule();
		int vacant = module.getVacant();
		if (fixData.typeFixed == vacant) {
			// closer look is needed - look for what other strategy survived (if any)
			for (int n = 0; n < module.getNTraits(); n++) {
				if (n == vacant)
					continue;
				if (dpop.strategiesTypeCount[n] > 0) {
					// no other strategies should be present
					fixData.typeFixed = n;
					break;
				}
			}
		}
		fixData.timeFixed = realtime;
		fixData.updatesFixed = generation;
		fixData.probRead = false;
		fixData.timeRead = false;
	}

	@Override
	public void load() {
		super.load();
		cloOptimize.addKeys(OptimizationType.values());
		CLOption cloInitType = engine.cloInitType;
		cloInitType.clearKeys();
		cloInitType.addKeys(InitType.values());
		cloInitType.removeKey(InitType.DEFAULT);
		cloInitType.removeKey(InitType.STATISTICS);
		// kaleidoscopes are not standard and must be requested/enabled by modules and
		// their IBSDPopulation implementations.
		cloInitType.removeKey(InitType.KALEIDOSCOPE);
	}

	@Override
	public void unload() {
		super.unload();
		cloOptimize.clearKeys();
		engine.cloInitType.clearKeys();
	}

	@Override
	public boolean check() {
		boolean doReset = super.check();
		if (permitsMode(Mode.STATISTICS))
			fixData = new FixationData();
		else
			fixData = null;
		// NOTE: optimizeHomo is disabled for multi-species (see cloOptimize)
		if (optimizeHomo) {
			if (population.populationUpdateType == PopulationUpdateType.ECOLOGY) {
				optimizeHomo = false;
				logger.warning(
						"optimizations for homogeneous states disabled (incompatible with variable population sizes).");
				doReset = true;
			}
			Module module = population.getModule();
			double pMutation = module.getMutationProb();
			if (pMutation <= 0.0) {
				optimizeHomo = false;
				logger.warning("optimizations for homogeneous states disabled (small mutations required).");
				doReset = true;
			}
			int nPop = module.getNPopulation();
			if (pMutation < 0.0 || pMutation > 0.1 * nPop) {
				logger.warning("optimizations for homogeneous states not recommended (mutations in [0, " + (0.1 / nPop)
						+ ") recommended, now " +
						Formatter.format(pMutation, 4) + ", proceeding)");
				doReset = true;
			}
		}
		return doReset;
	}

	@Override
	public void reset() {
		super.reset();
		if (fixData != null)
			fixData.reset();
	}

	/**
	 * Type of initial density distribution. Currently this model supports:
	 * <dl>
	 * <dt>frequency
	 * <dd>Random distribution of traits with given frequencies
	 * {@link org.evoludo.simulator.modules.Discrete#cloInit
	 * modules.Discrete.cloInit} (default).
	 * <dt>uniform
	 * <dd>Uniform random distribution of traits.
	 * <dt>monomorphic
	 * <dd>Monomorphic trait initialization.
	 * <dt>kaleidoscope
	 * <dd>Symmetric initial distribution, possibly generating evolutionary
	 * kaleidoscopes for deterministic synchronous updates.
	 * <dt>mutant
	 * <dd>Single mutant trait in random location otherwise homogeneous population.
	 * The monomorphic trait is given by the highest frequency in
	 * {@link org.evoludo.simulator.modules.Discrete#cloInit
	 * modules.Discrete.cloInit} and the mutant by the lowest frequency. For modules
	 * that admit vacant sites, their frequency is given by {@code init[VACANT]}.
	 * Vacant traits are ignored when determining the resident or the mutant.
	 * <dt>stripes
	 * <dd>Stripes of different traits. Requires square lattice geometry.
	 * <dt>STATISTICS
	 * <dd>Initialization for statistics. Same as {@link #MUTANT} plus bookkeeping
	 * for statistics. Convenience type for statistics mode. Not user selectable.
	 * <dt>DEFAULT
	 * <dd>Default initialization type. Not user selectable.
	 * </dl>
	 * 
	 * @author Christoph Hauert
	 * 
	 * @see #setInitType(Key)
	 * @see EvoLudo#cloInitType
	 * @see org.evoludo.simulator.modules.Discrete#cloInit
	 *      modules.Discrete.cloInit
	 */
	public enum InitType implements CLOption.Key {

		/**
		 * Random distribution of traits with given frequencies (default).
		 * 
		 * @see org.evoludo.simulator.modules.Discrete#cloInit modules.Discrete.cloInit
		 */
		FREQUENCY("frequency", "random distribution with given frequency"),

		/**
		 * Uniform random distribution of traits.
		 */
		UNIFORM("uniform", "uniform random distribution"),

		/**
		 * Monomorphic initialization of the population. The monomorphic trait is given
		 * by the highest frequency in
		 * {@link org.evoludo.simulator.modules.Discrete#cloInit
		 * modules.Discrete.cloInit}
		 */
		MONO("monomorphic", "monomorphic initialization"),

		/**
		 * Single mutant trait in random location otherwise homogeneous population. The
		 * monomorphic trait is given by the highest frequency in
		 * {@link org.evoludo.simulator.modules.Discrete#cloInit
		 * modules.Discrete.cloInit} and the mutant by the lowest frequency. For modules
		 * that admit vacant sites, their frequency is given by {@code init[VACANT]}.
		 * Vacant traits are ignored when determining the resident or the mutant.
		 */
		MUTANT("mutant", "mutant in homogeneous population"),

		/**
		 * Symmetric initial distribution, possibly generating evolutionary
		 * kaleidoscopes for deterministic synchronous updates.
		 */
		KALEIDOSCOPE("kaleidoscope", "evolutionary kaleidoscopes"),

		/**
		 * Stripes of different traits. Requires square lattice geometry.
		 */
		STRIPES("stripes", "stripes of traits"),

		/**
		 * Initialization for statistics. Same as {@link #MUTANT} plus bookkeeping for
		 * statistics. Convenience type for statistics mode. Not user selectable.
		 * 
		 * @see #MUTANT
		 */
		STATISTICS("-stat", "convenience type for statistics mode"),

		/**
		 * Default initialization type. Not user selectable.
		 */
		DEFAULT("-default", "default initialization");

		/**
		 * Key of initialization type. Used when parsing command line options.
		 * 
		 * @see EvoLudo#cloInitType
		 */
		String key;

		/**
		 * Brief description of initialization type for help display.
		 * 
		 * @see EvoLudo#helpCLO()
		 */
		String title;

		/**
		 * Instantiate new initialization type.
		 * 
		 * @param key   identifier for parsing of command line option
		 * @param title summary of initialization
		 */
		InitType(String key, String title) {
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
	}

	/**
	 * Type of optimizations. Currently this model supports:
	 * <dl>
	 * <dt>NONE
	 * <dd>No optimization (default).
	 * <dt>HOMO
	 * <dd>Skip homogeneous states by introducing a mutant and advancing the time
	 * according to an exponential distribution for an event happening with
	 * probability {@code Module#getMutationProb()}.
	 * <dt>MORAN
	 * <dd>Optimize Moran processes by restring events exclusively to links along
	 * which a change in the composition of the population may occur. This destroys
	 * the time scale.
	 * </dl>
	 * 
	 */
	public static enum OptimizationType implements CLOption.Key {

		/**
		 * No optimization (default).
		 */
		NONE("none", "no optimization"),

		/**
		 * Skip homogeneous states by introducing a mutant and advancing the time
		 * according to an exponential distribution for an event happening with
		 * probability {@code Module#getMutationProb()}.
		 */
		HOMO("homo", "skip homogneous states (single species only)"),

		/**
		 * Optimize Moran processes by restring events exclusively to links along
		 * which a change in the composition of the population may occur. This destroys
		 * the time scale.
		 */
		MORAN("moran", "restrict update to active links (destroys time-scale)");

		/**
		 * Key of optimization type. Used when parsing command line options.
		 * 
		 * @see EvoLudo#cloInitType
		 */
		String key;

		/**
		 * Brief description of optimization type for help display.
		 * 
		 * @see EvoLudo#helpCLO()
		 */
		String title;

		/**
		 * Instantiate new optimization type.
		 * 
		 * @param key   identifier for parsing of command line option
		 * @param title summary of optimization
		 */
		OptimizationType(String key, String title) {
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
	 * Command line option to request optimizations.
	 */
	public final CLOption cloOptimize = new CLOption("optimize", "none", EvoLudo.catModel,
			"--optimize <t1[,t2,...]>  optimize aspects of evolutionary process", new CLODelegate() {

				/**
				 * {@inheritDoc}
				 * <p>
				 * Parse method for choosing optimizations when updating a single or multiple
				 * populations/species. <code>arg</code> can be a single value or an array of
				 * values with the separator {@value CLOParser#SPECIES_DELIMITER}. The parser
				 * cycles through <code>arg</code> until all populations/species have the
				 * player's references set.
				 * 
				 * @param arg the (array of) reference type(s)
				 * 
				 * @see OptimizationType
				 */
				@Override
				public boolean parse(String arg) {
					boolean success = true;
					// reset all optimizations
					optimizeHomo = false;
					for (IBSPopulation pop : species)
						((IBSDPopulation) pop).optimizeMoran = false;
					// process requested optimizations
					String[] optis = arg.split(CLOParser.VECTOR_DELIMITER);
					for (int n = 0; n < optis.length; n++) {
						String opt = optis[n];
						OptimizationType ot = (OptimizationType) cloOptimize.match(opt);
						switch (ot) {
							case HOMO:
								// skip homogeneous populations (single species only)
								if (isMultispecies) {
									logger.warning("homogeneous optimizations require single species - disabled.");
									optimizeHomo = false;
									success = false;
									continue;
								}
								optimizeHomo = true;
								break;
							case MORAN:
								for (IBSPopulation pop : species)
									((IBSDPopulation) pop).optimizeMoran = true;
								break;
							case NONE:
								optimizeHomo = false;
								for (IBSPopulation pop : species)
									((IBSDPopulation) pop).optimizeMoran = false;
								break;
							default:
								break;
						}
					}
					return success;
				}

				@Override
				public void report(PrintStream output) {
					// moran optimization also reported through IBS.cloPopulationUpdate
					String moran = "";
					String homo = "";
					for (IBSPopulation pop : species) {
						if (((IBSDPopulation) pop).optimizeMoran)
							moran += (moran.length() > 0 ? ", " : "") + pop.getModule().getName();
					}
					if (optimizeHomo) {
						homo = "homo" + (moran.length() > 0 ? ", " : "");
					}
					if (homo.length() > 0 || moran.length() > 0)
						output.println("# optimization:         " + homo
								+ (moran.length() > 0 ? "moran (" + moran + ")" : ""));
				}
			});

	@Override
	public void collectCLO(CLOParser parser) {
		super.collectCLO(parser);
		parser.addCLO(cloOptimize);
	}
}