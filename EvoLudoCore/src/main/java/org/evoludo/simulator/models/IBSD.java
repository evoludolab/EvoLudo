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
	 * @param engine the pacemaker for running the model
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
		 * Creates a new fixation data structure.
		 */
		public FixationData() {
		}

		/**
		 * The index of the node (location) where the initial mutant arose.
		 */
		public int mutantNode = -1;

		/**
		 * The strategy type of the initial mutant.
		 */
		public int mutantTrait = -1;

		/**
		 * The strategy type of the resident.
		 */
		public int residentTrait = -1;

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
			residentTrait = -1;
			probRead = true;
			timeRead = true;
		}

		@Override
		public String toString() {
			return "{ mutantNode -> " + mutantNode + //
					", mutantTrait -> " + mutantTrait + //
					", residentTrait -> " + residentTrait + //
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
		fixData.updatesFixed = generation;
		fixData.probRead = false;
		fixData.timeRead = false;
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
		if (permitsMode(Mode.STATISTICS_SAMPLE))
			fixData = new FixationData();
		else
			fixData = null;
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

	@Override
	public void reset() {
		super.reset();
		if (fixData != null)
			fixData.reset();
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
		public final CLOption clo = new CLOption("inittype", Init.Type.UNIFORM.getKey(), EvoLudo.catModule,
				"--inittype <t>  type of initial configuration", new CLODelegate() {
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
												"inittype '" + inittype + "' unknown!");
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
							output.println("# inittype:             " + init.type + " " + //
									Formatter.format(init.args, 2) + (isMultiSpecies ? " ("
											+ mod.getName() + ")" : ""));
						}
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
			MONO("monomorphic", "monomorphic initialization with trait <t[,v]>"),

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
		MORAN("moran", "restrict update to active links (destroys time-scale)");

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
