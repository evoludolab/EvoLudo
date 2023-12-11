package org.evoludo.simulator.models;

import java.io.PrintStream;

import org.evoludo.simulator.EvoLudo;
import org.evoludo.simulator.models.IBSD.OptimizationType;
import org.evoludo.simulator.modules.Module;
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
		 * @see EvoLudo#cloMutationType
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
	public void unload() {
		// free resources
		super.unload();
		cloMutationType.clearKeys();
		cloInitType.clearKeys();
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
	 * <dt>uniform
	 * Uniform distribution of traits covering entire trait interval (default).
	 * <dt>mono <x>
	 * <dd>Monomorphic population with trait {@code xi}.
	 * <dt>gaussian <m,s>
	 * <dd>Gaussian distribution of traits with mean {@code mi} and standard
	 * deviation
	 * {@code si}.
	 * <dt>mutant <r,m>
	 * <dd>Monomorphic resident population with trait {@code r} and single mutant
	 * with trait {@code m}.
	 * </dl>
	 * 
	 * @author Christoph Hauert
	 * 
	 * @see #setInitType(Key)
	 * @see EvoLudo#cloInitType
	 */
	public enum InitType implements CLOption.Key {

		/**
		 * Uniform distribution of traits covering entire trait interval (default).
		 */
		UNIFORM("uniform", "uniform trait distribution", 0),

		/**
		 * Monomorphic population with trait(s) {@code xi}.
		 */
		MONO("mono", "monomorphic trait <x1,...,xd>", 1),

		/**
		 * Gaussian distribution of traits with mean {@code mi} and standard deviation
		 * {@code si}.
		 */
		GAUSSIAN("gaussian", "Gaussian traits <m1,...,md;s1,...,sd>", 2),

		/**
		 * Monomorphic resident population with trait {@code ri} and single mutant with
		 * trait {@code mi}.
		 */
		MUTANT("mutant", "mutant in monomorphic resident <m1,...,md;r1,...,rd>", 2);

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
		 * @param key     identifier for parsing of command line option
		 * @param title   summary of geometry
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
	 * Command line option to set the type of initial configuration.
	 * <p>
	 * <strong>Note:</strong> option not automatically added. Models that implement
	 * different initialization types should load it in
	 * {@link #collectCLO(CLOParser)}.
	 * 
	 * @see InitType
	 */
	public final CLOption cloInitType = new CLOption("inittype", InitType.UNIFORM.getKey(), EvoLudo.catModule,
			"--inittype <t>  type of initial configuration", new CLODelegate() {
				@Override
				public boolean parse(String arg) {
					String[] inittypes = arg.split(CLOParser.TRAIT_DELIMITER);
					int n = 0;
					String type = inittypes[n++ % inittypes.length];
					InitType key = (InitType) cloInitType.match(type);
					String[] typeargs = type.split("[\\s=]");
					IBSMCPopulation cpop = (IBSMCPopulation) population;
					Module mod = cpop.getModule();
					if (key == null
							|| (!key.equals(InitType.UNIFORM) && (typeargs == null || typeargs.length < key.nParams))) {
						cpop.setInitType(InitType.UNIFORM, null);
						logger.warning(
								(species.size() > 1 ? mod.getName() + ": " : "") +
										"inittype '" + type + "' unknown - using '"
										+ cpop.getInitType() + "'");
						return false;
					}
					int nt = mod.getNTraits();
					double[][] pargs = null;
					if (key.nParams > 0) {
						double[][] args = CLOParser.parseMatrix(typeargs[1]);
						if (args == null || args.length < key.nParams || args[0].length < 1) {
							cpop.setInitType(InitType.UNIFORM, null);
							logger.warning(
									(species.size() > 1 ? mod.getName() + ": " : "") +
											"inittype '" + type + "' unknown - using '"
											+ cpop.getInitType() + "'");
							return false;
						}
						pargs = new double[key.nParams][nt];
						// process arguments further
						for (int i = 0; i < key.nParams; i++)
							for (int j = 0; j < nt; j++)
								pargs[i][j] = args[i][j % args[i].length];
					}
					cpop.setInitType(key, pargs);
					return true;
				}

				@Override
				public void report(PrintStream output) {
					IBSMCPopulation cpop = (IBSMCPopulation) population;
					output.println(
							"# inittype:             " + cpop.getInitType() + (species.size() > 1 ? " ("
									+ population.getModule().getName() + ")" : ""));
				}
			});

	/**
	 * Command line option to set the mutation type.
	 */
	public final CLOption cloMutationType = new CLOption("mutationtype", MutationType.UNIFORM.getKey(), EvoLudo.catModel,
			"--mutationtype <t>   mutation type:", new CLODelegate() {

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
					MutationType mt = (MutationType) cloMutationType.match(arg);
					if (mt == null) {
						logger.warning((isMultispecies ? population.getModule().getName() + ": " : "")
								+ "mutation type '" + arg + "' unknown - using '"
								+ ((IBSMCPopulation) population).getMutationType() + "'");
						return false;
					}
					((IBSMCPopulation) population).setMutationType(mt);
					return true;
				}

				@Override
				public void report(PrintStream output) {
					output.println(
							"# mutationtype:         " + ((IBSMCPopulation) population).getMutationType().getTitle());
				}
			});

	@Override
	public void collectCLO(CLOParser parser) {
		super.collectCLO(parser);
		// initialize mutation types
		parser.addCLO(cloMutationType);
		cloMutationType.addKeys(MutationType.values());
		parser.addCLO(cloInitType);
		cloInitType.addKeys(InitType.values());
		// interacting with all members of the population is not feasible for continuous
		// traits; use single interaction with random neighbour as default
		cloInteractions.setDefault("random 1");
		// comparing scores with all members of the population is not feasible for
		// continuous traits; use single, random neighbour as default
		cloReferences.setDefault("random 1");
	}
}
