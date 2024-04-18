package org.evoludo.simulator.modules;

import java.io.PrintStream;
import java.util.ArrayList;

import org.evoludo.math.RNGDistribution;
import org.evoludo.simulator.EvoLudo;
import org.evoludo.util.CLOParser;
import org.evoludo.util.CLOption;
import org.evoludo.util.CLOption.CLODelegate;

public abstract class Mutation {

	/**
	 * The module using this mutation.
	 */
	Module module;

	/**
	 * Convenience field: the shared random number generator to ensure
	 * reproducibility of results. Currently only used for IBS models.
	 * 
	 * @see EvoLudo#getRNG()
	 */
	protected RNGDistribution rng;

	/**
	 * Instantiate new mutation.
	 * 
	 * @param module the map to use as template
	 */
	public Mutation(Module module) {
		this.module = module;
		rng = module.engine.getRNG();
	}

	/**
	 * Mutation type.
	 * 
	 * @see #clo
	 */
	public CLOption.Key type;

	/**
	 * The probability of mutations.
	 */
	public double probability;

	/**
	 * The range of mutations.
	 */
	public double range = 0.0;

	/**
	 * The flag to indicate whether mutations arise uniformly distributed (cosmic
	 * rays) or are tied to reproduction events (thermal mutations).
	 */
	public boolean uniform = false;

	/**
	 * Returns the flag indicating whether mutations are uniformly distributed
	 * (cosmic rays) or tied to reproduction events (thermal mutations).
	 * 
	 * @return {@code true} if mutations are uniformly distributed, {@code false} if
	 *         tied to reproduction events
	 */
	public boolean getUniform() {
		return uniform;
	}

	/**
	 * Check if a mutation arises.
	 * 
	 * @return {@code true} if a mutation should be performed
	 */
	public abstract boolean doMutate();

	/**
	 * Mutate trait {@code trait} in IBS models according to the type of mutation.
	 * 
	 * @param trait the trait to mutate
	 * @return the mutated trait
	 * 
	 * @see Discrete.Type
	 */
	public int mutate(int trait) {
		throw new UnsupportedOperationException("Not implemented");
	}

	/**
	 * Mutate trait {@code trait} in IBS models according to the type of mutation.
	 * 
	 * @param trait the trait to mutate
	 * @return the mutated trait
	 * 
	 * @see Continuous.Type
	 */
	public double mutate(double trait) {
		throw new UnsupportedOperationException("Not implemented");
	}

	public static class Discrete extends Mutation {

		public Discrete(Module module) {
			super(module);
			// add all keys by default
			clo.addKeys(Type.values());
			// defaults to no mutations
			type = Type.NONE;
		}

		@Override
		public boolean doMutate() {
			if (type == Type.NONE || uniform)
				return false;
			if (probability >= 1.0)
				return true;
			return rng.random01() < probability;
		}

		@Override
		public int mutate(int trait) {
			if (type == Type.NONE)
				// no mutations
				return trait;
			int vacant = module.getVacant();
			if (trait == vacant)
				// vacant trait cannot mutate
				return trait;
			// number of active traits (including vacancies)
			int nActive = module.getNActive();
			if (nActive <= (vacant < 0? 1 : 2))
				// no mutations if only one trait is active
				return trait;
			int nTraits = module.getNTraits();
			if (nActive == nTraits) {
				// all traits are active
				int nt = (vacant < 0? nTraits : nTraits - 1);
				switch ((Type) type) {
					case ALL:
						trait = rng.random0n(nt);
						break;
					case OTHER:
						trait = (trait + rng.random0n(nt - 1) + 1) % nTraits;
						break;
					case RANGE:
						int irange = (int) range;
						trait = (trait + rng.random0n(irange * 2 + 1) - irange + nTraits) % nTraits;
						break;
					default:
						return trait;
				}
				if (vacant >= 0 && trait >= vacant)
					trait = (trait + 1) % nTraits;
				return trait;
			}
			// some traits are inactive
			boolean[] active = module.getActiveTraits();
			int idx = -1;
			int mut = trait;
			switch ((Type) type) {
				case ALL:
					idx = rng.random0n(nActive);
					mut = -1;
					while (idx >= 0) {
						if (idx != vacant && active[idx--])
							mut++;
					}
					break;
				case OTHER:
					idx = rng.random0n(nActive - 1);
					mut = -1;
					while (idx >= 0) {
						if (idx != trait && idx != vacant && active[idx--])
							mut++;
					}
					break;
				case RANGE:
					int irange = (int) range;
					mut = (trait + rng.random0n(irange * 2 + 1) - irange + nTraits) % nTraits;
					break;
				default:
			}
			return mut;
		}

		/**
		 * Mutate traits in DE models according to the type of mutation. Adjust the
		 * change of the state in {@code change} accordinlgy. In multi-species modules
		 * the indices referring to the current species run from {@code from} to
		 * {@code to}.
		 * 
		 * @param state  the state to mutate
		 * @param change the change of the state
		 * @param from   the start index of the traits to mutate
		 * @param to     the end index of the traits to mutate
		 * @return the mutated trait
		 * 
		 * @see Discrete.Type
		 */
		public void mutate(double[] state, double[] change, int from, int to) {
			int dim;
			double mu1;
			switch ((Type) type) {
				case ALL:
					// mutations to any strategy
					dim = to - from;
					mu1 = 1.0 - probability;
					double muid = probability / dim;
					// any mutates to i: mu / dim
					// i mutates to any other: mu * state[i]
					for (int i = from; i < to; i++)
						change[i] = change[i] * mu1 + muid * (1.0 - dim * state[i]);
					break;
				case OTHER:
					// mutations to any of the _other_ strategies
					dim = to - from;
					mu1 = 1.0 - probability;
					double muid1 = probability / (dim - 1);
					// any other mutates to i: mu / (dim - 1) * (1 - state[i])
					// i mutates to any other: mu * state[i]
					for (int i = from; i < to; i++)
						change[i] = change[i] * mu1 + muid1 * (1.0 - dim * state[i]);
					break;
				case RANGE:
					// mutations to any of the _other_ strategies at most range traits away
					dim = to - from;
					int irange = (int) range;
					mu1 = 1.0 - probability;
					for (int i = from; i < to; i++) {
						double x = 0.0;
						// the range may depend on the current trait i; cannot fall outside 
						// interval [from, to]
						int low = Math.max(from, i - irange);
						int high = Math.min(to, i + irange);
						// any other mutates to i: mu / (high - low) (\sum_{j=i-r}^{i+r} state[j] - state[i])
						// i mutates to any other: mu * state[i]
						for (int j = low; j <= high; j++) {
							if (j != i)
								x += state[j];
						}
						change[i] = change[i] * mu1 + probability * (x / (high - low) - state[i]);						
					}
					break;
				default:
			}
		}

		/**
		 * Command line option to set the type of player updates.
		 */
		public final CLOption clo = new CLOption("mutations", "0.0",
				EvoLudo.catModule,
				"--mutations <p> [<t> [thermal|uniform [<r>]]]  with\n" +
				"             p: mutation probability\n" + //
				"       process: reproduction or cosmic rays\n" + //
				"             r: mutation range\n" + //
				"             t: mutation type, with types:", //
				new CLODelegate() {

					/**
					 * {@inheritDoc}
					 * <p>
					 * Parse player update type(s) for a single or multiple populations/species.
					 * {@code arg} can be a single value or an array of values with the
					 * separator {@value CLOParser#SPECIES_DELIMITER}. The parser cycles through
					 * {@code arg} until all populations/species have the player update type
					 * set.
					 * 
					 * @param arg the (array of) map name(s)
					 */
					@Override
					public boolean parse(String arg) {
						boolean success = true;
						String[] mutations = arg.split(CLOParser.SPECIES_DELIMITER);
						int n = 0;
						ArrayList<? extends Module> species = module.getSpecies();
						for (Module mod : species) {
							String mutts = mutations[n++ % mutations.length];
							String[] args = mutts.split("\\s+|=|,");
							Mutation mut = mod.getMutation();
							// set defaults
							mut.probability = 0.0;
							mut.uniform = false;
							mut.range = 0.0;
							mut.type = Type.NONE;
							switch (args.length) {
								case 4:
									mut.range = CLOParser.parseDouble(args[3]);
									if (mut.range < 1.0) {
										mut.type = Type.NONE;
										module.logger.warning((species.size() > 1 ? mod.getName() + ": " : "") + //
												"mutation range '" + args[3] + "' invalid - using '"
												+ mut.type + "'");
										success = false;
										continue;
									}
									//$FALL-THROUGH$
								case 3:
									mut.uniform = args[2].toLowerCase().startsWith("u");
									//$FALL-THROUGH$
								case 2:
									Type mutt = (Type) clo.match(args[1]);
									if (mutt == null) {
										module.logger.warning((species.size() > 1 ? mod.getName() + ": " : "") + //
												"mutation type '" + args[1] + "' not recognized - using '"
												+ mut.type + "'");
										success = false;
										continue;
									}
									mut.type = mutt;
									//$FALL-THROUGH$
								case 1:
									mut.probability = Math.max(0.0, CLOParser.parseDouble(args[0]));
									if (mut.probability <= 0.0)
										mut.type = Type.NONE;
									else if (mut.type == Type.NONE)
										mut.type = Type.OTHER;
									break;
								case 0:
								default:
									// no arguments, stick to defaults
							}
						}
						return success;
					}

					@Override
					public void report(PrintStream output) {
						ArrayList<? extends Module> species = module.getSpecies();
						for (Module mod : species) {
							Mutation mut = mod.getMutation();
							if (mut.probability <= 0.0)
								continue;
							Type mutt = (Type) mut.type;
							if (mutt == Type.NONE)
								continue;
							output.println("# mutationtype:         " + mutt
									+ (species.size() > 1 ? " (" + mod.getName() + ")" : ""));
							output.println("# mutationprob:         " + mut.probability
									+ (mut.uniform ? " uniform" : " thermal"));
							switch (mutt) {
								case RANGE:
									output.println("# mutationrange:        " + mut.range);
									break;
								default:
									break;
							}
						}
					}

					@Override
					public int getKeyPos() {
						return 1;
					}
				});

		/**
		 * Mutation types. Currently available types of mutations are:
		 * <dl>
		 * <dt>none
		 * <dd>No mutations
		 * <dt>all
		 * <dd>Mutate to any trait
		 * <dt>other
		 * <dd>Mutate to any <em>other</em> trait
		 * <dt>range
		 * <dd>Mutate trait {@code t} to {@code t &pm; <r>}. Only available for modules
		 * where traits have a metric.
		 * </dl>
		 */
		public static enum Type implements CLOption.Key {

			/**
			 * No mutations. This is the default
			 */
			NONE("none", "no mutations"),

			/**
			 * Mutate to any trait.
			 */
			ALL("all", "mutate to any trait"),

			/**
			 * Mutate to any <em>other</em> trait.
			 */
			OTHER("other", "mutate to any other trait"),

			/**
			 * Mutate trait {@code t} to {@code t &pm; <r>}. Only available for modules
			 * where traits have a metric.
			 */
			RANGE("range", "mutate to traits &pm; <r>");

			/**
			 * Key of player update. Used when parsing command line options.
			 * 
			 * @see Module#clo
			 */
			String key;

			/**
			 * Brief description of player update for help display.
			 * 
			 * @see EvoLudo#helpCLO()
			 */
			String title;

			/**
			 * Instantiates a new type of player update type.
			 * 
			 * @param key   the identifier for parsing of command line option
			 * @param title the summary of the player update
			 */
			Type(String key, String title) {
				this.key = key;
				this.title = title;
			}

			@Override
			public String toString() {
				return key + ": " + title;
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

	public static class Continuous extends Mutation {

		public Continuous(Module module) {
			super(module);
			// add all keys by default
			clo.addKeys(Type.values());
			// defaults to no mutations
			type = Type.NONE;
		}

		@Override
		public boolean doMutate() {
			if (type == Type.NONE || uniform)
				return false;
			if (probability >= 1.0)
				return true;
			return rng.random01() < probability;
		}

		@Override
		public double mutate(double trait) {
			double mut;
			switch ((Type) type) {
				case ALL:
					return rng.random01();
				case GAUSSIAN:
					// draw mutants until we find viable one...
					// not very elegant but avoids emphasis of interval boundaries.
					do {
						mut = trait + rng.nextGaussian() * range;
					} while (mut < 0.0 || mut > 1.0);
					// alternative approach - use reflective boundaries (John Fairfield)
					// note: this is much more elegant than the above - is there a biological
					// motivation for 'reflective mutations'? is such a justification necessary?
					// double mut = randomGaussian(orig, mutRange[loc]);
					// mut = Math.abs(mut);
					// if( mut>1.0 ) mut = 2-mut;
					return mut;

					// just truncate to [0,1]
					// mut = trait + rng.nextGaussian() * range;
					// return Math.max(Math.min(mut, 1.0), 0.0);
				case UNIFORM:
					mut = trait + rng.random01() * range * 2 - range;
					return Math.max(Math.min(mut, 1.0), 0.0);
				default:
					return trait;
			}
		}

		/**
		 * Command line option to set the type of player updates.
		 */
		public final CLOption clo = new CLOption("mutations", "0.0",
				EvoLudo.catModule,
			"--mutations <p> [<t> [<r> [thermal|uniform]]]"+CLOParser.TRAIT_DELIMITER+"<p1>...]>  with\n" +
			"             p: mutation probability\n" + //
			"             r: mutation range/sdev (fraction of interval)\n" + //
			"       process: reproduction or cosmic rays\n" + //
			"             t: mutation type, with types:", //
				new CLODelegate() {

					/**
					 * {@inheritDoc}
					 * <p>
					 * Parse player update type(s) for a single or multiple populations/species.
					 * {@code arg} can be a single value or an array of values with the
					 * separator {@value CLOParser#SPECIES_DELIMITER}. The parser cycles through
					 * {@code arg} until all populations/species have the player update type
					 * set.
					 * 
					 * @param arg the (array of) map name(s)
					 */
					@Override
					public boolean parse(String arg) {
						boolean success = true;
						String[] mutations = arg.split(CLOParser.SPECIES_DELIMITER);
						int n = 0;
						ArrayList<? extends Module> species = module.getSpecies();
						for (Module mod : species) {
							String mutts = mutations[n++ % mutations.length];
							String[] args = mutts.split("\\s+|=|,");
							Mutation mut = mod.getMutation();
							// set defaults
							mut.probability = 0.0;
							mut.uniform = false;
							mut.range = 0.0;
							mut.type = Type.NONE;
							switch (args.length) {
								case 4:
									mut.uniform = args[3].toLowerCase().startsWith("u");
									//$FALL-THROUGH$
								case 3:
									mut.range = CLOParser.parseDouble(args[2]);
									if (mut.range <= 0.0) {
										mut.type = Type.NONE;
										module.logger.warning((species.size() > 1 ? mod.getName() + ": " : "") + //
												"mutation range '" + args[2] + "' invalid - using '"
												+ mut.type + "'");
										success = false;
										continue;
									}
									//$FALL-THROUGH$
								case 2:
									Type mutt = (Type) clo.match(args[1]);
									if (mutt == null) {
										module.logger.warning((species.size() > 1 ? mod.getName() + ": " : "") + //
												"mutation type '" + args[1] + "' not recognized - using '"
												+ mut.type + "'");
										success = false;
										continue;
									}
									if (mutt == Type.UNIFORM && mut.range <= 0.0) {
										// no valid range specified, default to entire trait interval
										mut.type = Type.ALL;
									}
									mut.type = mutt;
									//$FALL-THROUGH$
								case 1:
									mut.probability = Math.max(0.0, CLOParser.parseDouble(args[0]));
									if (mut.probability <= 0.0)
										mut.type = Type.NONE;
									else if (mut.type == Type.NONE) {
										mut.type = Type.GAUSSIAN;
										if (mut.range <= 0.0) {
											mut.range = 0.01;
										}
									}
									break;
								case 0:
								default:
									// no arguments, stick to defaults
							}
						}
						return success;
					}

					@Override
					public void report(PrintStream output) {
						ArrayList<? extends Module> species = module.getSpecies();
						for (Module mod : species) {
							Mutation mut = mod.getMutation();
							if (mut.probability <= 0.0)
								continue;
							Type mutt = (Type) mut.type;
							if (mutt == Type.NONE)
								continue;
							output.println("# mutationtype:         " + mutt
									+ (species.size() > 1 ? " (" + mod.getName() + ")" : ""));
							output.println("# mutationprob:         " + mut.probability
									+ (mut.uniform ? " uniform" : " thermal"));
							switch (mutt) {
								case GAUSSIAN:
									output.println("# mutationsdev:        " + mut.range);
									break;
								case UNIFORM:
									output.println("# mutationrange:        " + mut.range);
									break;
								default:
									break;
							}
						}
					}

					@Override
					public int getKeyPos() {
						return 1;
					}
				});

		/**
		 * Mutation types. Currently available types of mutations are:
		 * <dl>
		 * <dt>none
		 * <dd>No mutations
		 * <dt>all
		 * <dd>Mutate to any trait in trait interval, uniformly distributed.
		 * <dt>gaussian
		 * <dd>Mutate trait according to a Gaussian distribution around parental trait
		 * with standard deviation {@code s}.
		 * <dt>uniform
		 * <dd>Mutate trait uniformly distributed around parental trait {@code t} within
		 * a range {@code t &pm; r}.
		 * </dl>
		 */
		public static enum Type implements CLOption.Key {

			/**
			 * No mutations. This is the default
			 */
			NONE("none", "no mutations"),

			/**
			 * Mutate to any trait value.
			 */
			ALL("all", "uniform mutations to any trait value"),

			/**
			 * Gaussian distributed mutations around parental trait.
			 */
			GAUSSIAN("gaussian", "Gaussian mutations, sdev <s>"),

			/**
			 * Mutate parental trait {@code t} to {@code t &pm; <r>}.
			 */
			UNIFORM("uniform", "uniform mutations, &pm; <r>");

			/**
			 * Key of player update. Used when parsing command line options.
			 * 
			 * @see Module#clo
			 */
			String key;

			/**
			 * Brief description of player update for help display.
			 * 
			 * @see EvoLudo#helpCLO()
			 */
			String title;

			/**
			 * Instantiates a new type of player update type.
			 * 
			 * @param key   the identifier for parsing of command line option
			 * @param title the summary of the player update
			 */
			Type(String key, String title) {
				this.key = key;
				this.title = title;
			}

			@Override
			public String toString() {
				return key + ": " + title;
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
}
