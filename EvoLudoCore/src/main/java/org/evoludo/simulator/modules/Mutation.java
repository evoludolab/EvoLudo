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
	protected CLOption.Key type;

	/**
	 * The probability of mutations.
	 */
	double probability;

	/**
	 * The range of mutations.
	 */
	double range = 0.0;

	/**
	 * The flag to indicate whether mutations arise uniformly distributed (cosmic
	 * rays) or are tied to reproduction events (thermal mutations).
	 */
	boolean uniform = true;

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

	public static class Discrete extends Mutation {

		public Discrete(Module module) {
			super(module);
			// add all keys by default
			clo.addKeys(Type.values());
			// defaults to no mutations
			type = Type.NONE;
		}

		/**
		 * Check if a mutation arises.
		 * 
		 * @return {@code true} if a mutation should be performed
		 */
		public boolean doMutate() {
			return type != Type.NONE && rng.random01() < probability;
		}

		/**
		 * Mutate trait {@code trait} according to the type of mutation.
		 * 
		 * @param trait the trait to mutate
		 * @return the mutated trait
		 * 
		 * @see Discrete.Type
		 */
		public int mutate(int trait) {
			switch ((Type) type) {
				case ALL:
					return rng.random0n(module.getNTraits());
				case OTHER:
					int mut = rng.random0n(module.getNTraits() - 1);
					return mut >= trait ? mut + 1 : mut;
				case RANGE:
					int irange = (int) range;
					int nt = module.getNTraits();
					return (trait + rng.random0n(irange * 2 + 1) - irange + nt) % nt;
				default:
					return trait;
			}
		}

		/**
		 * Command line option to set the type of player updates.
		 */
		public final CLOption clo = new CLOption("mutations", "0.0",
				EvoLudo.catModule,
				"--mutations <p> [<t>[ thermal|uniform [<r>]]] set probability p,\n" + //
						"                type t, process and range r:",
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

		/**
		 * Check if a mutation arises.
		 * 
		 * @return {@code true} if a mutation should be performed
		 */
		public boolean doMutate() {
			return type != Type.NONE && rng.random01() < probability;
		}

		/**
		 * Mutate trait {@code trait} according to the type of mutation.
		 * 
		 * @param trait the trait to mutate
		 * @return the mutated trait
		 * 
		 * @see Continuous.Type
		 */
		public double mutate(double trait, int idx) {
			double mut;
			switch ((Type) type) {
				case ALL:
					return rng.random01();
				case GAUSSIAN:
					mut = trait + rng.nextGaussian() * range;
					return Math.max(Math.min(mut, 1.0), 0.0);
				case RANGE:
					mut = trait + rng.random01() * range * 2 - range;
					return Math.max(Math.min(mut, 1.0), 0.0);
				default:
					return trait;
			}
		}

		/**
		 * Command line option to set the type of player updates.
		 */
		public final CLOption clo = new CLOption("mutations", "none",
				EvoLudo.catModule,
				"--mutations <t> [<p>[ thermal|uniform [<r>]]] set mutation type t with\n" + //
						"                probability p, range/sdev r as well as process:",
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
							Type mutt = (Type) clo.match(mutts);
							Mutation mut = mod.getMutation();
							if (mutt == null) {
								module.logger.warning((species.size() > 1 ? mod.getName() + ": " : "") + //
										"mutation '" + mutts + "' not recognized - using '"
										+ mut.type + "'");
								success = false;
								continue;
							}
							mut.type = mutt;
							// parse <p>[ thermal|uniform] if present
							String[] args = mutts.split("\\s+|=|,");
							// set defaults
							mut.probability = 0.0;
							mut.uniform = true;
							switch (args.length) {
								case 4:
									mut.range = CLOParser.parseDouble(args[3]);
									// $FALL-THROUGH$
								case 3:
									mut.uniform = args[2].toLowerCase().startsWith("u");
									// $FALL-THROUGH$
								case 2:
									mut.probability = CLOParser.parseDouble(args[1]);
									if (mut.probability <= 0.0) {
										mut.type = Type.NONE;
										module.logger.warning((species.size() > 1 ? mod.getName() + ": " : "") + //
												"mutation probability '" + mutts + "' invalid - using '"
												+ mut.type + "'");
										success = false;
										continue;
									}
									break;
								default:
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
								case RANGE:
									output.println("# mutationrange:        " + mut.range);
									break;
								default:
									break;
							}
						}
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
			RANGE("range", "uniform mutations, &pm; <r>");

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
