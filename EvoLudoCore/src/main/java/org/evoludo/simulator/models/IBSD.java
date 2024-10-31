package org.evoludo.simulator.models;

import java.io.PrintStream;

import org.evoludo.math.ArrayMath;
import org.evoludo.simulator.EvoLudo;
import org.evoludo.simulator.modules.Features;
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
public class IBSD extends IBS implements Discrete {

	/**
     * Modules that offer individual based simulation models with discrete traits
     * and pairwise interactions must implement this interface.
     */
    public interface IBSDPairs extends IBS.HasIBS, Features.Pairs {
    
    	/**
    	 * Calculate and return total (accumulated) payoff/score for pairwise
    	 * interactions of the focal individual with trait/strategy {@code me}
    	 * against opponents with different traits/strategies. The respective numbers of
    	 * each of the {@code nTraits} opponent traits/strategies are provided in
    	 * the array {@code tCount}. The payoffs/scores for each of the
    	 * {@code nTraits} opponent traits/strategies must be stored and returned
    	 * in the array {@code tScore}.
    	 * <p>
    	 * <strong>Important:</strong> must be overridden and implemented in subclasses
    	 * that define game interactions between pairs of individuals
    	 * ({@code nGroup=2}, {@code pairwise=true}), otherwise see
    	 * {@link Groups#groupScores(int[], double[])}.
    	 * 
    	 * @param me         the trait index of the focal individual
    	 * @param traitCount number of opponents with each trait/strategy
    	 * @param traitScore array for returning the scores of each opponent
    	 *                   trait/strategy
    	 * @return score of focal individual {@code me} accumulated over all
    	 *         interactions
    	 */
    	public double pairScores(int me, int[] traitCount, double[] traitScore);
    
    	/**
    	 * Calculate the average payoff/score in a finite population with the number of
    	 * each trait/strategy provided in {@code count} for pairwise interactions.
    	 * The payoffs/scores for each of the {@code nTraits} traits/strategies
    	 * must be stored and returned in the array {@code traitScores}.
    	 * <p>
    	 * <strong>Important:</strong> must be overridden and implemented in subclasses
    	 * that define game interactions in well-mixed populations where individuals
    	 * interact with everyone else. Computationally it is not feasible to cover this
    	 * scenario with {@link #pairScores(int, int[], double[])} or
    	 * {@link Groups#groupScores(int[], double[])}, respectively.
    	 * <p>
    	 * <strong>Note:</strong> If explicit calculations of the well-mixed scores are
    	 * not available, interactions with everyone in well-mixed populations should
    	 * checked for and excluded with a warning in {@link #check()} (see
    	 * {@link org.evoludo.simulator.models.IBSMCPopulation#check() CXPopulation} for
    	 * an example).
    	 * 
    	 * @param traitCount number of individuals for each trait/strategy
    	 * @param traitScore array for returning the payoffs/scores of each
    	 *                   trait/strategy
    	 */
    	public void mixedScores(int[] traitCount, double[] traitScore);
    }

    /**
     * Modules that offer individual based simulation models with discrete traits
     * and interactions in groups must implement this interface.
     */
    public interface IBSDGroups extends IBSDPairs, Features.Groups {
    
    	/**
    	 * Calculate the payoff/score for interactions in groups consisting of
    	 * traits/strategies with respective numbers given in the array
    	 * {@code tCount}. The interaction group size is given by the sum over
    	 * {@code tCount[i]} for {@code i=0,1,...,nTraits}. The payoffs/scores
    	 * for each of the {@code nTraits} traits/strategies must be stored and
    	 * returned in the array {@code tScore}.
    	 * <p>
    	 * <strong>Important:</strong> must be overridden and implemented in subclasses
    	 * that define game interactions among groups of individuals (for groups with
    	 * sizes {@code nGroup&gt;2}, otherwise see
    	 * {@link #pairScores(int, int[], double[])}).
    	 * 
    	 * @param traitCount group composition given by the number of individuals with
    	 *                   each trait/strategy
    	 * @param traitScore array for returning the payoffs/scores of each
    	 *                   trait/strategy
    	 */
    	public void groupScores(int[] traitCount, double[] traitScore);
    
    	/**
    	 * Calculate the average payoff/score in a finite population with the number of
    	 * each trait/strategy provided in {@code count} for interaction groups of
    	 * size {@code n}. The payoffs/scores for each of the {@code nTraits}
    	 * traits/strategies must be stored and returned in the array
    	 * {@code traitScores}.
    	 * 
    	 * <h3>Notes:</h3>
    	 * For payoff calculations:
    	 * <ul>
    	 * <li>each strategy sees one less of its own type in its environment
    	 * <li>the size of the environment is {@code nPopulation-1}
    	 * <li>the fact that the payoff of each strategy does not depend on its own type
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
    	 * @param traitCount number of individuals for each trait/strategy
    	 * @param n          interaction group size
    	 * @param traitScore array for returning the payoffs/scores of each
    	 *                   trait/strategy
    	 */
    	public void mixedScores(int[] traitCount, int n, double[] traitScore);
    
    	@Override
    	public default void mixedScores(int[] traitCount, double[] traitScore) {
    		mixedScores(traitCount, 2, traitScore);
    	}
    }

	/**
	 * Creates a population of individuals for IBS simulations with discrete
	 * traits/strategies.
	 * 
	 * @param engine the pacemaker for running the model
	 */
	public IBSD(EvoLudo engine) {
		super(engine);
	}

	@Override
	public void readStatisticsSample() {
		super.readStatisticsSample();
		if (fixData == null)
			return;

		// collect new statistics sample
		IBSDPopulation dpop = (IBSDPopulation) population;
		fixData.typeFixed = ArrayMath.maxIndex(dpop.strategiesTypeCount);
		Module module = species.get(0);
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
		fixData.updatesFixed = time;
		fixData.probRead = false;
		fixData.timeRead = false;
	}

	@Override
	public void resetStatisticsSample() {
		super.resetStatisticsSample();
		if (fixData != null)
			fixData.reset();
	}

	@Override
	public boolean permitsSampleStatistics() {
		if (!super.permitsSampleStatistics())
			return false;
		// sampling statistics also require:
		// - mutant or temperature initialization
		// - no vacant sites or monostop (otherwise extinction is the only absorbing state)
		for (Module mod : species) {
			Init.Type type = ((IBSDPopulation) mod.getIBSPopulation()).getInit().type;
			if ((type.equals(Init.Type.MUTANT) || 
				type.equals(Init.Type.TEMPERATURE)) &&
				(mod.getVacant() < 0 || (mod.getVacant() >= 0 && ((org.evoludo.simulator.modules.Discrete) mod).getMonoStop())))
				continue;
			return false;
		}
		return true;
	}

	@Override
	public void unload() {
		super.unload();
		cloOptimize.clearKeys();
	}

	@Override
	public boolean check() {
		boolean doReset = super.check();
		// NOTE: optimizeHomo is disabled for multi-species (see cloOptimize)
		if (optimizeHomo) {
			if (population.populationUpdate.getType() == PopulationUpdate.Type.ECOLOGY) {
				optimizeHomo = false;
				logger.warning(
						"optimizations for homogeneous states disabled (incompatible with variable population sizes).");
				doReset = true;
			}
			Module module = population.getModule();
			double pMutation = module.getMutation().probability;
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

	/**
	 * Helper routine to retrieve the {@link IBSDPopulation} associated with module
	 * with {@code id}.
	 * 
	 * @param id the {@code id} of the module
	 * @return the {@code IBSDPopulation}
	 */
	IBSDPopulation getIBSDPopulation(int id) {
		return (IBSDPopulation) getIBSPopulation(id);
	}

	@Override
	public boolean setInitialTraits(double[] init) {
		IBSDPopulation pop = (IBSDPopulation) species.get(0).getIBSPopulation();
		if (!pop.getInit().clo.isValidKey(Init.Type.FREQUENCY))
			return false;

		if (!isMultispecies)
			return pop.setInitialTraits(init);

		int skip = 0;
		boolean success = true;
		for (Module mod : species) {
			IBSDPopulation dpop = (IBSDPopulation) mod.getIBSPopulation();
			double[] tmp = new double[dpop.nTraits];
			System.arraycopy(init, skip, tmp, 0, dpop.nTraits);
			success &= dpop.setInitialTraits(tmp);
			skip += dpop.nTraits;
		}
		return success;
	}

	@Override
	public boolean setInitialTraits(int id, double[] init) {
		return getIBSDPopulation(id).setInitialTraits(init);
	}

	@Override
	public double getMonoScore(int id, int type) {
		return getIBSDPopulation(id).getMonoScore(type);
	}

	/**
	 * The initialization of populations with discrete traits. This includes the
	 * initialization type as well as any accompanying arguments.
	 */
	public static class Init {

		/**
		 * The model that is using this initialization. This is specific to IBS models.
		 */
		org.evoludo.simulator.models.IBS ibs;

		/**
		 * Instantiate new initialization for use in IBS {@code model}s.
		 * 
		 * @param ibs the model using this initialization
		 */
		public Init(org.evoludo.simulator.models.IBS ibs) {
			this.ibs = ibs;
			type = Type.UNIFORM;
		}

		/**
		 * The population update type.
		 * 
		 * @see #clo
		 */
		Init.Type type;

		/**
		 * The arguments for the initialization.
		 */
		double[] args;

		/**
		 * Get the arguments of this initialization type.
		 * 
		 * @return the arguments associated with this initialization type
		 * 
		 * @see #args
		 */
		public double[] getArgs() {
			return args;
		}

		@Override
		public String toString() {
			return type.getKey() + " " + Formatter.format(args, 2);
		}

		/**
		 * Command line option to set the type of initial configuration.
		 * <p>
		 * <strong>Note:</strong> option not automatically added. Models that implement
		 * different initialization types should load it in
		 * {@link #collectCLO(CLOParser)}.
		 * 
		 * @see Type
		 */
		public final CLOption clo = new CLOption("init", Init.Type.UNIFORM.getKey(), EvoLudo.catModule, null,
				new CLODelegate() {
					@Override
					public boolean parse(String arg) {
						boolean success = true;
						String[] inittypes = arg.split(CLOParser.SPECIES_DELIMITER);
						int idx = 0;
						Init.Type prevtype = null;
						boolean isMultiSpecies = (ibs.species.size() > 1);
						for (Module mod : ibs.species) {
							IBSDPopulation dpop = (IBSDPopulation) mod.getIBSPopulation();
							String inittype = inittypes[idx++ % inittypes.length];
							double[] initargs = null;
							String[] typeargs = inittype.split("\\s+|=");
							Init.Type newtype = (Init.Type) clo.match(inittype);
							Init init = dpop.getInit();
							if (newtype == null && prevtype != null) {
								newtype = prevtype;
								initargs = CLOParser.parseVector(typeargs[0]);
							} else if (typeargs.length > 1)
								initargs = CLOParser.parseVector(typeargs[1]);
							// only uniform or kaleidoscope initializations do not require additional
							// arguments
							if (newtype == null || (initargs == null && !(newtype.equals(Init.Type.UNIFORM)
									|| newtype.equals(Init.Type.KALEIDOSCOPE)))) {
								ibs.logger.warning(
										(isMultiSpecies ? mod.getName() + ": " : "") +
												"init '" + inittype + "' unknown!");
								// default to uniform
								newtype = Init.Type.UNIFORM;
								success = false;
							}
							init.type = newtype;
							init.args = initargs;
							prevtype = newtype;
						}
						return success;
					}

					@Override
					public void report(PrintStream output) {
						boolean isMultiSpecies = (ibs.species.size() > 1);
						for (Module mod : ibs.species) {
							IBSDPopulation dpop = (IBSDPopulation) mod.getIBSPopulation();
							Init init = dpop.getInit();
							output.println("# init:                 " + init.type + " " + //
									Formatter.format(init.args, 2) + (isMultiSpecies ? " ("
											+ mod.getName() + ")" : ""));
						}
					}

					@Override
					public String getDescription() {
						String descr = "--init <t>      type of initial configuration:\n" + clo.getDescriptionKey()
							+ "\n                with r, m indices of resident, mutant traits";
						boolean noVacant = false;
						for (Module mod : ibs.species)
							noVacant |= mod.getVacant() < 0;
						if (noVacant)
							return descr.replaceAll("\\[,v\\]", "");
						return descr + "\n                and v frequency of vacant sites";
					}
				});

		/**
		 * Type of initial density distribution. Currently this model supports:
		 * <dl>
		 * <dt>frequency &lt;f1,..,fn&gt;
		 * <dd>Random distribution of traits with given frequencies
		 * {@code &lt;f1,..,fd&gt;}
		 * for traits {@code 1,...,d} (default).
		 * <dt>uniform
		 * <dd>Uniform random distribution of traits.
		 * <dt>monomorphic &lt;t[,v]&gt;
		 * <dd>Monomorphic initialization of trait {@code t}. For modules that admit
		 * vacant sites, their frequency is {@code v}.
		 * <dt>kaleidoscope
		 * <dd>Symmetric initial distribution, possibly generating evolutionary
		 * kaleidoscopes for deterministic synchronous updates.
		 * <dt>mutant &lt;m,r[,v]&gt;
		 * <dd>Single mutant with trait {@code m} in random location of otherwise
		 * homogeneous population with trait {@code r}. For modules that admit vacant
		 * sites, their frequency is {@code v}.
		 * <dt>stripes &lt;t1,...,td&gt;
		 * <dd>Stripes of different traits. Ensures that at least one interface between
		 * any two traits exists. Requires square lattice geometry.
		 * <dt>STATISTICS
		 * <dd>Initialization for statistics. Same as {@link #MUTANT} plus bookkeeping
		 * for statistics. Convenience type for statistics mode. Not user selectable.
		 * </dl>
		 * 
		 * @see Init#clo
		 * @see IBSDPopulation#getInit()
		 */
		public enum Type implements CLOption.Key {

			/**
			 * Random distribution of traits with frequencies {@code &lt;f1,..,fd&gt;}
			 * (default).
			 */
			FREQUENCY("frequency", "random distribution with frequency <f1,..,fd>"),

			/**
			 * Uniform random distribution of traits.
			 */
			UNIFORM("uniform", "uniform random distribution"),

			/**
			 * Monomorphic initialization of the population with the specified trait. For
			 * modules that admit vacant sites, their frequency is {@code v}. The parameters
			 * are specified through the argument of the format {@code t[,v]}.
			 */
			MONO("monomorphic", "monomorphic initialization with trait <r[,v]>"),

			/**
			 * Single mutant with trait {@code m} in otherwise homogeneous population with
			 * trait {@code r}. The mutant is placed in a location chosen uniformly at
			 * random (uniform initialization). For modules that admit vacant sites, their
			 * frequency is {@code v}. The parameters are specified through the argument of
			 * the format {@code m,r[,v]}.
			 */
			MUTANT("mutant", "uniformly distributed, <m,r[,v]>"),

			/**
			 * Single mutant with trait {@code m} in otherwise homogeneous population with
			 * trait {@code r}. The mutant is placed in a random location with probability
			 * proportional to number of incoming links (temperature initialization). For
			 * modules that admit vacant sites, their frequency is {@code v}. The parameters
			 * are specified through the argument of the format {@code m,r[,v]}.
			 */
			TEMPERATURE("temperature", "temperature distribution, <m,r[,v]>"),

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
			STATISTICS("-stat", "convenience type for statistics mode");

			/**
			 * Key of initialization type. Used when parsing command line options.
			 * 
			 * @see Init#clo
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
		MORAN("moran", "update active links only (destroys time-scale)");

		/**
		 * Key of optimization type. Used when parsing command line options.
		 * 
		 * @see IBSD#cloOptimize
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
			"--optimize <t1[,t2,...]>  enable optimizations:", new CLODelegate() {

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
					for (Module mod : species) {
						IBSDPopulation dpop = (IBSDPopulation) mod.getIBSPopulation();
						dpop.optimizeMoran = false;
					}
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
								for (Module mod : species) {
									IBSDPopulation dpop = (IBSDPopulation) mod.getIBSPopulation();
									dpop.optimizeMoran = true;
								}
								break;
							case NONE:
								optimizeHomo = false;
								for (Module mod : species) {
									IBSDPopulation dpop = (IBSDPopulation) mod.getIBSPopulation();
									dpop.optimizeMoran = false;
								}
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
					for (Module mod : species) {
						IBSDPopulation dpop = (IBSDPopulation) mod.getIBSPopulation();
						if (dpop.optimizeMoran)
							moran += (moran.length() > 0 ? ", " : "") + mod.getName();
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
		IBSDPopulation pop = (IBSDPopulation) species.get(0).getIBSPopulation();
		CLOption clo = pop.getInit().clo;
		clo.clearKeys();
		clo.addKeys(Init.Type.values());
		parser.addCLO(clo);
		// kaleidoscopes are not standard and must be requested/enabled by modules and
		// their IBSDPopulation implementations.
		clo.removeKey(Init.Type.KALEIDOSCOPE);
		parser.addCLO(clo);
		parser.addCLO(cloOptimize);
		cloOptimize.addKeys(OptimizationType.values());
	}
}
