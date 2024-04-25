package org.evoludo.simulator.models;

import java.io.PrintStream;

import org.evoludo.simulator.EvoLudo;
import org.evoludo.simulator.modules.Module;
import org.evoludo.util.CLOParser;
import org.evoludo.util.CLOption;
import org.evoludo.util.CLOption.CLODelegate;
import org.evoludo.util.CLOption.Key;
import org.evoludo.util.Formatter;

/**
 * Base class for individual based simulation models, IBS, with a single or
 * multiple continuous traits/strategies. This class deals with initialization
 * and mutation types but the heavy lifting is done by the {@link IBS} parent.
 * 
 * @author Christoph Hauert
 */
public class IBSC extends IBS implements Model.ContinuousIBS {

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
		cloInitType.clearKeys();
	}

	/**
	 * Helper routine to retrieve the {@link IBSPopulation} associated with module
	 * with {@code id}.
	 * 
	 * @param id the {@code id} of the module
	 * 
	 * @return the {@code IBSPopulation}
	 */
	IBSMCPopulation getIBSMCPopulation(int id) {
		return (IBSMCPopulation) (isMultispecies ? species.get(id).getIBSPopulation() : population);
	}

	@Override
	public double[] getTraitMin(int id) {
		return getIBSMCPopulation(id).getTraitMin();
	}

	@Override
	public double[] getTraitMax(int id) {
		return getIBSMCPopulation(id).getTraitMax();
	}

	@Override
	public double getMinMonoScore(int id) {
		return species.get(id).getMinMonoGameScore();
	}
	
	@Override
	public double getMaxMonoScore(int id) {
		return species.get(id).getMaxMonoGameScore();
	}
	
	@Override
	public void getTraitHistogramData(int id, double[][] bins) {
		getIBSMCPopulation(id).getTraitHistogramData(bins);
	}

	@Override
	public void get2DTraitHistogramData(int id, double[] bins, int trait1, int trait2) {
		getIBSMCPopulation(id).get2DTraitHistogramData(bins, trait1, trait2);
	}

	/**
	 * Type of initial density distribution of traits. Currently this model supports:
	 * <dl>
	 * <dt>uniform
	 * Uniform distribution of traits covering entire trait interval (default).
	 * <dt>mono &lt;x&gt;
	 * <dd>Monomorphic population with trait {@code xi}.
	 * <dt>gaussian &lt;m,s&gt;
	 * <dd>Gaussian distribution of traits with mean {@code mi} and standard
	 * deviation
	 * {@code si}.
	 * <dt>mutant &lt;r,m&gt;
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
		 * The arguments for the initialization. Convenience field, meaningful only
		 * immediately after calls to {@link IBSMCPopulation#getInitType(int)} or
		 * {@link IBSCPopulation#getInitType()}.
		 */
		double[] args;

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

		/**
		 * Get the arguments of this initialization type. Convenience field.
		 * 
		 * @return the arguments associated with this initialization type
		 * 
		 * @see #args
		 */
		public double[] getArgs() {
			return args;
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
					boolean success = true;
					IBSMCPopulation cpop = (IBSMCPopulation) population;
					String[] inittypes = arg.split(CLOParser.TRAIT_DELIMITER);
					Module mod = cpop.getModule();
					int nt = mod.getNTraits();
					InitType prevtype = null;
					for (int n = 0; n < nt; n++) {
						String inittype = inittypes[n % inittypes.length];
						double[] initargs = null;
						String[] typeargs = inittype.split("\\s+|=");
						InitType type = (InitType) cloInitType.match(inittype);
						if (type == null && prevtype != null) {
							type = prevtype;
							initargs = CLOParser.parseVector(typeargs[0]);
						} else if (typeargs.length > 1)
							initargs = CLOParser.parseVector(typeargs[1]);
						boolean argsOk = (initargs != null && initargs.length >= type.nParams);
						// only uniform initialization does not require additional arguments
						if (type == null || (!type.equals(InitType.UNIFORM) && !argsOk)) {
							logger.warning(
									(species.size() > 1 ? mod.getName() + ": " : "") +
											"inittype '" + inittype + "' unknown!");
							type = InitType.UNIFORM;
							success = false;
						}
						cpop.setInitType(type, initargs, n);
						prevtype = type;
					}
					return success;
				}

				@Override
				public void report(PrintStream output) {
					IBSMCPopulation cpop = (IBSMCPopulation) population;
					Module mod = cpop.getModule();
					int nt = mod.getNTraits();
					for (int n = 0; n < nt; n++) {
						InitType type = cpop.getInitType(n);
						output.println("# inittype:             " + type.getKey() + " " + //
								Formatter.format(type.args, 2) + (species.size() > 1 ? " ("
										+ mod.getName() + ")" : ""));
					}
				}
			});

	@Override
	public void collectCLO(CLOParser parser) {
		super.collectCLO(parser);
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
