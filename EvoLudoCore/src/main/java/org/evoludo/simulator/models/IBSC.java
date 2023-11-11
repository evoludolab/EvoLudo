package org.evoludo.simulator.models;

import java.io.PrintStream;

import org.evoludo.simulator.EvoLudo;
import org.evoludo.simulator.models.IBSD.OptimizationType;
import org.evoludo.util.CLOParser;
import org.evoludo.util.CLOption;
import org.evoludo.util.CLOption.CLODelegate;
import org.evoludo.util.CLOption.Key;

/**
 * Base class for individual based simulation models, IBS, with a single or
 * multiple continuous traits/strategies. This class deals with initialization
 * and mutation types but the heavy lifting is done by the {@link IBS} parent.
 * 
 * @author Christoph Hauert
 */
public class IBSC extends IBS implements Model.ContinuousIBS {

	/**
	 * Type of mutations. Currently this model supports:
	 * <dl>
	 * <dt>NONE
	 * <dd>No mutations (default).
	 * <dt>UNIFORM
	 * <dd>Mutations distributed uniformly at random.
	 * <dt>GAUSSIAN
	 * <dd>Mutations are Gaussian distributed around parental trait/strategy.
	 * </dl>
	 * 
	 * @author Christoph Hauert
	 * 
	 * @see #cloMutationType
	 * @see org.evoludo.simulator.models.IBSMCPopulation#setMutationType(MutationType)
	 *      IBSMCPopulation.setMutationType(MutationType)
	 * @see org.evoludo.simulator.modules.Module#setMutationProb(double)
	 *      Module.setMutationProb(double)
	 */
	public static enum MutationType implements CLOption.Key {

		/**
		 * No mutations (default).
		 */
		NONE("none", "no mutations"),

		/**
		 * Mutations distributed uniformly at random.
		 */
		UNIFORM("uniform", "uniform mutations"),

		/**
		 * Mutations are Gaussian distributed around parental trait/strategy.
		 */
		GAUSSIAN("gaussian", "Gaussian mutations around parental trait");

		/**
		 * Key of mutation type. Used when parsing command line options.
		 * 
		 * @see EvoLudo#cloInitType
		 */
		String key;

		/**
		 * Brief description of mutation type for help display.
		 * 
		 * @see EvoLudo#helpCLO()
		 */
		String title;

		/**
		 * Instantiate new mutation type.
		 * 
		 * @param key   identifier for parsing of command line option
		 * @param title summary of mutation type
		 */
		MutationType(String key, String title) {
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
	 * Creates a population of individuals for IBS simulations with continuous
	 * traits/strategies.
	 * 
	 * @param engine the pacemeaker for running the model
	 */
	public IBSC(EvoLudo engine) {
		super(engine);
	}

	@Override
	public void load() {
		super.load();
		// initialize mutation types
		cloMutationType.addKeys(MutationType.values());
		engine.cloInitType.addKeys(InitType.values());
		// interacting with all members of the population is not feasible for continuous
		// traits; use single interaction with random neighbour as default
		cloInteractionType.setDefault("r1");
		// comparing scores with all members of the population is not feasible for
		// continuous traits; use single, random neighbour as default
		cloReferenceType.setDefault("r1");
	}

	@Override
	public void unload() {
		// free resources
		super.unload();
		cloMutationType.clearKeys();
		engine.cloInitType.clearKeys();
	}

	@Override
	public void getTraitHistogramData(int id, double[][] bins) {
		((IBSMCPopulation) population).getTraitHistogramData(bins);
	}

	@Override
	public void getTraitHistogramData(int id, double[] bins, int trait1, int trait2) {
		((IBSMCPopulation) population).getTraitHistogramData(bins, trait1, trait2);
	}

	/**
	 * Type of initial density distribution. Currently this model supports:
	 * <dl>
	 * <dt>UNIFORM
	 * Uniform random distribution of traits covering entire trait interval.
	 * <dt>MONO
	 * <dd>Monomorphic population with trait
	 * {@link org.evoludo.simulator.modules.Continuous#cloInit mono}.
	 * <dt>GAUSSIAN
	 * <dd>Gaussian trait distribution with
	 * {@link org.evoludo.simulator.modules.Continuous#cloInit mean} and
	 * {@link org.evoludo.simulator.modules.Continuous#cloInit standard deviation}.
	 * <dt>DELTA
	 * Delta distribution of traits with
	 * {@link org.evoludo.simulator.modules.Continuous#cloInit resident} trait and
	 * {@link org.evoludo.simulator.modules.Continuous#cloInit mutant} trait.
	 * <dt>DEFAULT
	 * <dd>Default initialization (Uniform). Not user selectable.
	 * </dl>
	 * 
	 * @author Christoph Hauert
	 * 
	 * @see #setInitType(Key)
	 * @see EvoLudo#cloInitType
	 */
	public enum InitType implements CLOption.Key {

		/**
		 * Uniform random distribution of traits covering entire trait interval.
		 */
		UNIFORM("uniform", "uniform trait distribution", 0),

		/**
		 * Monomorphic population with trait
		 * {@link org.evoludo.simulator.modules.Continuous#cloInit mono}.
		 */
		MONO("mono", "monomorphic trait distribution", 1),

		/**
		 * Gaussian trait distribution with
		 * {@link org.evoludo.simulator.modules.Continuous#cloInit mean} and
		 * {@link org.evoludo.simulator.modules.Continuous#cloInit standard deviation}.
		 */
		GAUSSIAN("gaussian", "Gaussian trait distribution", 2),

		/**
		 * Delta distribution of traits with
		 * {@link org.evoludo.simulator.modules.Continuous#cloInit resident} trait and
		 * {@link org.evoludo.simulator.modules.Continuous#cloInit mutant} trait.
		 */
		DELTA("delta", "mutant in monomorphic population", 2),

		/**
		 * Default initialization type. Not user selectable.
		 */
		DEFAULT("-default", "default initialization", 0);

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
		 * The number of parameters of the cost function.
		 */
		int nParams;

		/**
		 * Instantiate new initialization type.
		 * 
		 * @param key   identifier for parsing of command line option
		 * @param title summary of geometry
		 * @param nParams the number of parameters
		 */
		InitType(String key, String title, int nParams) {
			this.key = key;
			this.title = title;
			this.nParams = nParams;
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
	 * Command line option to set the mutation type.
	 */
	public final CLOption cloMutationType = new CLOption("mutationtype", "gaussian", EvoLudo.catModel,
			"--mutationtype <t>   mutation type (none, uniform, gaussian)",
			new CLODelegate() {

				/**
				 * {@inheritDoc}
				 * <p>
				 * Parse method for choosing mutation types when updating individuals with one
				 * or mutliple traits/strategies. Only intra-species interactions (single
				 * species modules) are currently supported and only the same mutation type in
				 * all traits/strategies.
				 * 
				 * @param arg the mutation type
				 * 
				 * @see OptimizationType
				 */
				@Override
				public boolean parse(String arg) {
					MutationType mt = (MutationType)cloMutationType.match(arg);
					if (mt == null) {
						logger.warning((isMultispecies ? population.getModule().getName() + ": " : "")
								+ "mutation type '" + arg
								+ "' unknown - using '" + ((IBSMCPopulation) population).getMutationType() + "'");
						return false;
					}
					((IBSMCPopulation) population).setMutationType(mt);
					return true;
				}

				@Override
				public void report(PrintStream output) {
					output.println("# mutationtype:         "
							+ ((IBSMCPopulation) population).getMutationType().getTitle());
				}
			});

	@Override
	public void collectCLO(CLOParser parser) {
		super.collectCLO(parser);
		parser.addCLO(cloMutationType);
	}
}