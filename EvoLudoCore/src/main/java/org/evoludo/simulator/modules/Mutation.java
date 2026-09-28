//
// EvoLudo Project
//
// Copyright 2010-2026 Christoph Hauert
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

package org.evoludo.simulator.modules;

import java.util.List;

import org.evoludo.math.ArrayMath;
import org.evoludo.math.RNGDistribution;
import org.evoludo.simulator.EvoLudo;
import org.evoludo.util.CLODelegate;
import org.evoludo.util.CLOParser;
import org.evoludo.util.CLOption;
import org.evoludo.util.CLOCategory;

/**
 * The abstract superclass to implement mutations in IBS models with discrete or
 * continuous traits.
 * 
 * @author Christoph Hauert
 */
public abstract class Mutation {

	/**
	 * The module using this mutation.
	 */
	Module<?> module;

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
	protected Mutation(Module<?> module) {
		this.module = module;
		rng = module.engine.getRNG();
	}

	/**
	 * Mutation type.
	 * 
	 * @see Discrete.Type
	 * @see Continuous.Type
	 */
	public CLOption.Key type;

	/**
	 * The probability of mutations.
	 */
	double probability;

	/**
	 * Get the probability of mutations.
	 * 
	 * @return the mutation probability
	 */
	public double getProbability() {
		return probability;
	}

	/**
	 * Set the probability of mutations.
	 * 
	 * @param probability the mutation probability
	 */
	public void setProbability(double probability) {
		this.probability = probability;
	}

	/**
	 * The range of mutations.
	 */
	public double range = 0.0;

	/**
	 * The flag to indicate whether mutations arise uniformly distributed (cosmic
	 * rays) or are tied to reproduction events (temperature mutations).
	 */
	public boolean temperature = false;

	/**
	 * Extract the optional mutation process, {@code temperature} (mutations tied
	 * to reproduction) or {@code random} (mutations arise uniformly distributed,
	 * cosmic rays), from the arguments {@code args} of the {@code --mutation}
	 * option and set {@link #temperature} accordingly. The process keyword may
	 * appear at any position; {@code random} must match in full to be
	 * distinguishable from the mutation type {@code range}. If no process is
	 * specified, {@link #temperature} remains unchanged.
	 *
	 * @param args the arguments of the {@code --mutation} option
	 * @return the arguments without the process keyword
	 */
	String[] parseProcess(String[] args) {
		for (int i = 0; i < args.length; i++) {
			String process = args[i].toLowerCase();
			boolean isRandom = process.equals("random");
			if (isRandom || process.startsWith("t")) {
				temperature = !isRandom;
				return ArrayMath.drop(args, i);
			}
		}
		return args;
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

	/**
	 * The class to implement mutations in discrete IBS models.
	 */
	public static class Discrete extends Mutation {

		/**
		 * Construct mutator for discrete traits.
		 * 
		 * @param module the module using this mutation
		 */
		public Discrete(Module<?> module) {
			super(module);
			// add all keys by default
			clo.addKeys(Type.values());
			// defaults to no mutations
			type = Type.NONE;
		}

		@Override
		public boolean doMutate() {
			if (type == Type.NONE || !temperature)
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
			int vacant = module.getVacantIdx();
			if (trait == vacant)
				// vacant trait cannot mutate
				return trait;
			// number of active traits (including vacancies)
			int nActive = module.getNActive();
			if (nActive <= (vacant < 0 ? 1 : 2))
				// no mutations if only one trait is active
				return trait;
			int nTraits = module.getNTraits();
			if (nActive == nTraits) {
				// all traits are active
				int nt = (vacant < 0 ? nTraits : nTraits - 1);
				switch ((Type) type) {
					case ALL:
						trait = rng.random0n(nt);
						break;
					case OTHER:
						trait = (trait + rng.random0n(nt - 1) + 1) % nTraits;
						break;
					case RANGE:
						int irange = (int) range;
						trait = Math.floorMod(trait + rng.random0n(irange * 2 + 1) - irange, nTraits);
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
					mut = Math.floorMod(trait + rng.random0n(irange * 2 + 1) - irange, nTraits);
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
		 * 
		 * @see Discrete.Type
		 */
		public void mutate(double[] state, double[] change, int from, int to) {
			int dim;
			double mu1;
			switch ((Type) type) {
				case ALL:
					// mutations to any trait
					dim = to - from;
					mu1 = 1.0 - probability;
					double muid = probability / dim;
					// any mutates to i: mu / dim
					// i mutates to any other: mu * state[i]
					for (int i = from; i < to; i++)
						change[i] = change[i] * mu1 + muid * (1.0 - dim * state[i]);
					break;
				case OTHER:
					// mutations to any of the _other_ traits
					dim = to - from;
					mu1 = 1.0 - probability;
					double muid1 = probability / (dim - 1);
					// any other mutates to i: mu / (dim - 1) * (1 - state[i])
					// i mutates to any other: mu * state[i]
					for (int i = from; i < to; i++)
						change[i] = change[i] * mu1 + muid1 * (1.0 - dim * state[i]);
					break;
				case RANGE:
					// mutations to any of the _other_ traits at most range traits away
					int irange = (int) range;
					mu1 = 1.0 - probability;
					for (int i = from; i < to; i++) {
						double x = 0.0;
						// the range may depend on the current trait i; cannot fall outside
						// interval [from, to]
						int low = Math.max(from, i - irange);
						int high = Math.min(to, i + irange);
						// any other mutates to i: mu / (high - low) (\sum_{j=i-r}^{i+r} state[j] -
						// state[i])
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
		public final CLOption clo = new CLOption("mutation", "0.0",
				CLOCategory.Model,
				"--mutation <p> [temperature|random (default)] [<t> [<r>]]]\n" +
						"             p: mutation probability\n" + //
						"       process: reproduction vs cosmic rays\n" + //
						"             r: mutation range\n" + //
						"             t: mutation type, with types:", //
				new CLODelegate() {

					/**
					 * {@inheritDoc}
					 * <p>
					 * Parse player update type(s) for a single or multiple populations/species.
					 * {@code arg} can be a single value or an array of values. The parser cycles
					 * through {@code arg} until all populations/species have mutations set.
					 * 
					 * @param arg the (array of) map name(s)
					 */
					@Override
					public boolean parse(String arg) {
						String[] mutations = arg.contains(CLOParser.SPECIES_DELIMITER)
								? arg.split(CLOParser.SPECIES_DELIMITER)
								: arg.split(CLOParser.VECTOR_DELIMITER);
						int n = 0;
						List<? extends Module<?>> species = module.getSpecies();
						for (Module<?> mod : species) {
							String mutts = mutations[n++ % mutations.length];
							String[] args = mutts.split("\\s+|=|,");
							Mutation mut = mod.getMutation();
							// set defaults
							mut.probability = 0.0;
							mut.temperature = false;
							mut.range = 0.0;
							mut.type = Type.NONE;
							// deal with optional temperature|random flag first
							args = mut.parseProcess(args);
							switch (args.length) {
								case 3:
									mut.range = CLOParser.parseDouble(args[2]);
									if (mut.range < 1.0)
										return false;
									//$FALL-THROUGH$
								case 2:
									Type mutt = (Type) clo.match(args[1]);
									if (mutt == null)
										return false;
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
									// no arguments, stick to defaults
									break;
								default:
									// too many arguments
									return false;
							}
						}
						return true;
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
		public enum Type implements CLOption.Key {

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
			 * @see Mutation.Discrete#clo
			 */
			String key;

			/**
			 * Brief description of player update for help display.
			 * 
			 * @see EvoLudo#getCLOHelp()
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

	/**
	 * The class to implement mutations in continuous IBS models.
	 */
	public static class Continuous extends Mutation {

		/**
		 * The maximum number of attempts to draw (uniformly distributed) traits that
		 * do not exceed the maximum combined trait value, or to reflect traits back
		 * into the feasible region.
		 *
		 * @see org.evoludo.simulator.modules.Continuous#getMaxInvest()
		 */
		public static final int MAX_REDRAWS = 100;

		/**
		 * The continuous module using this mutation. Convenience field to avoid
		 * casts.
		 */
		private final org.evoludo.simulator.modules.Continuous cmodule;

		/**
		 * Construct mutator for continuous traits.
		 * 
		 * @param module the module using this mutation
		 */
		public Continuous(org.evoludo.simulator.modules.Continuous module) {
			super(module);
			cmodule = module;
			// add all keys by default
			clo.addKeys(Type.values());
			// defaults to no mutations
			type = Type.NONE;
			// continuous traits mutate at reproduction by default
			temperature = true;
		}

		@Override
		public boolean doMutate() {
			if (type == Type.NONE || !temperature)
				return false;
			if (probability >= 1.0)
				return true;
			return rng.random01() < probability;
		}

		/**
		 * {@inheritDoc}
		 * <p>
		 * Mutants outside the trait interval \([0,1]\) are reflected at the
		 * boundaries. For symmetric mutation kernels, reflecting boundaries neither
		 * attract nor repel traits, i.e. under mutations alone, the stationary trait
		 * distribution is uniform. In contrast, truncating mutants results in an
		 * accumulation of traits at the boundaries and redrawing mutants in a
		 * depletion near the boundaries.
		 */
		@Override
		public double mutate(double trait) {
			switch ((Type) type) {
				case UNIFORM:
					return rng.random01();
				case GAUSSIAN:
					return reflect(trait + rng.nextGaussian() * range);
				case RANGE:
					return reflect(trait + rng.random01() * range * 2 - range);
				default:
					return trait;
			}
		}

		/**
		 * Temporary storage for the (remaining) displacement of a mutant in trait
		 * space.
		 */
		private double[] step;

		/**
		 * Mutate all {@code nTraits} traits of a single individual stored in
		 * {@code traits} starting at index {@code offset}. The traits are mutated in
		 * place. Without a maximum combined trait value, each trait is mutated
		 * independently and reflected at the boundaries of its interval, see
		 * {@link #mutate(double)}. Otherwise, the mutant moves along its displacement
		 * vector and is reflected like a billiard ball whenever it hits a boundary of
		 * the feasible region, i.e. the trait intervals or the hyperplane of the
		 * maximum combined trait value. For isotropic mutations, e.g. Gaussian
		 * mutations with the same standard deviation for all traits, reflecting the
		 * path of the mutant does not bias the trait distribution for any shape of the
		 * feasible region. Note that reflecting only the endpoint of the displacement
		 * sequentially at the individual boundaries does introduce biases near
		 * corners. Uniform mutants are redrawn to preserve the uniform distribution on
		 * the feasible region.
		 *
		 * @param traits the array of traits
		 * @param offset the index of the first trait of the individual
		 *
		 * @see org.evoludo.simulator.modules.Continuous#getMaxInvest()
		 */
		public void mutate(double[] traits, int offset) {
			int nTraits = cmodule.getNTraits();
			if (type == Type.UNIFORM) {
				int attempts = MAX_REDRAWS;
				do {
					for (int n = 0; n < nTraits; n++)
						traits[offset + n] = rng.random01();
				} while (getExcessInvest(traits, offset) > 0.0 && --attempts > 0);
				reflectMaxInvest(traits, offset);
				return;
			}
			double maxInvest = cmodule.getMaxInvest();
			if (maxInvest == Double.POSITIVE_INFINITY || !(type == Type.GAUSSIAN || type == Type.RANGE)) {
				for (int n = 0; n < nTraits; n++)
					traits[offset + n] = mutate(traits[offset + n]);
				return;
			}
			if (step == null || step.length != nTraits)
				step = new double[nTraits];
			for (int n = 0; n < nTraits; n++)
				step[n] = (type == Type.GAUSSIAN ? rng.nextGaussian() * range : rng.random01() * range * 2 - range);
			// feasible region: 0 <= traits[n] <= 1 and sum_n scale[n] * traits[n] <= bound
			double[] min = cmodule.getTraitMin();
			double[] max = cmodule.getTraitMax();
			double bound = maxInvest;
			double norm2 = 0.0;
			for (int n = 0; n < nTraits; n++) {
				double scale = max[n] - min[n];
				bound -= min[n];
				norm2 += scale * scale;
			}
			for (int r = 0; r < MAX_REDRAWS; r++) {
				// find the first boundary along the (remaining) path; index nTraits
				// refers to the hyperplane of the maximum combined trait value
				double hit = 1.0;
				int wall = -1;
				double sum = 0.0;
				double drift = 0.0;
				for (int n = 0; n < nTraits; n++) {
					double trait = traits[offset + n];
					double dn = step[n];
					double scale = max[n] - min[n];
					sum += scale * trait;
					drift += scale * dn;
					double t = Double.POSITIVE_INFINITY;
					if (dn < 0.0)
						t = -trait / dn;
					else if (dn > 0.0)
						t = (1.0 - trait) / dn;
					if (t < hit) {
						hit = t;
						wall = n;
					}
				}
				if (drift > 0.0) {
					double t = (bound - sum) / drift;
					if (t < hit) {
						hit = t;
						wall = nTraits;
					}
				}
				if (wall < 0) {
					// no boundary hit on remaining path
					for (int n = 0; n < nTraits; n++)
						traits[offset + n] += step[n];
					return;
				}
				// advance to boundary and reflect remaining path
				hit = Math.max(hit, 0.0);
				for (int n = 0; n < nTraits; n++) {
					traits[offset + n] += hit * step[n];
					step[n] *= 1.0 - hit;
				}
				if (wall < nTraits) {
					traits[offset + wall] = (step[wall] < 0.0 ? 0.0 : 1.0);
					step[wall] = -step[wall];
					continue;
				}
				double proj = 0.0;
				for (int n = 0; n < nTraits; n++)
					proj += (max[n] - min[n]) * step[n];
				proj *= 2.0 / norm2;
				for (int n = 0; n < nTraits; n++)
					step[n] -= proj * (max[n] - min[n]);
			}
			// numerical safety net (e.g. trapped in corner)
			for (int n = 0; n < nTraits; n++)
				traits[offset + n] = reflect(traits[offset + n]);
			reflectMaxInvest(traits, offset);
		}

		/**
		 * Get the amount by which the combined traits of a single individual exceed
		 * the maximum combined trait value of the module. The {@code nTraits} traits
		 * of the individual are stored in {@code traits} starting at index
		 * {@code offset}, scaled to \([0,1]\) as usual. Traits are feasible if the
		 * excess is not positive.
		 *
		 * @param traits the array of traits
		 * @param offset the index of the first trait of the individual
		 * @return the excess of the combined traits
		 *
		 * @see org.evoludo.simulator.modules.Continuous#getMaxInvest()
		 */
		public double getExcessInvest(double[] traits, int offset) {
			double maxInvest = cmodule.getMaxInvest();
			if (maxInvest == Double.POSITIVE_INFINITY)
				return Double.NEGATIVE_INFINITY;
			double[] min = cmodule.getTraitMin();
			double[] max = cmodule.getTraitMax();
			double sum = 0.0;
			for (int n = 0; n < min.length; n++)
				sum += min[n] + traits[offset + n] * (max[n] - min[n]);
			return sum - maxInvest;
		}

		/**
		 * Reflect traits of a single individual that exceed the maximum combined trait
		 * value of the module at the boundary. The {@code nTraits} traits of the
		 * individual are stored in {@code traits} starting at index {@code offset},
		 * scaled to \([0,1]\) as usual. The traits are mirrored at the hyperplane
		 * \(\sum_i t_i=T\), where \(t_i\) denote the traits in the units of their
		 * respective trait ranges and \(T\) the maximum combined trait value, and
		 * reflected back into the trait intervals if necessary. Used for traits that
		 * are set explicitly, e.g. initial configurations. Mutants are reflected along
		 * their path instead, see {@link #mutate(double[], int)}.
		 * <p>
		 * A single reflection suffices unless a single trait can exceed \(T\) on its
		 * own. If the traits remain infeasible after {@link #MAX_REDRAWS} reflections,
		 * they are shrunk towards the trait minima onto the boundary.
		 *
		 * @param traits the array of traits
		 * @param offset the index of the first trait of the individual
		 *
		 * @see org.evoludo.simulator.modules.Continuous#getMaxInvest()
		 */
		public void reflectMaxInvest(double[] traits, int offset) {
			double excess = getExcessInvest(traits, offset);
			if (!(excess > 0.0))
				return; // feasible (or vacant)
			double[] min = cmodule.getTraitMin();
			double[] max = cmodule.getTraitMax();
			int nTraits = min.length;
			double norm2 = 0.0;
			for (int n = 0; n < nTraits; n++) {
				double scale = max[n] - min[n];
				norm2 += scale * scale;
			}
			for (int r = 0; r < MAX_REDRAWS && excess > 0.0; r++) {
				double shift = 2.0 * excess / norm2;
				for (int n = 0; n < nTraits; n++)
					traits[offset + n] = reflect(traits[offset + n] - shift * (max[n] - min[n]));
				excess = getExcessInvest(traits, offset);
			}
			if (excess > 0.0) {
				// last resort: shrink traits towards minima onto boundary
				double scaled = 0.0;
				for (int n = 0; n < nTraits; n++)
					scaled += traits[offset + n] * (max[n] - min[n]);
				double shrink = (scaled - excess) / scaled;
				for (int n = 0; n < nTraits; n++)
					traits[offset + n] *= shrink;
			}
		}

		/**
		 * Reflect the trait {@code trait} at the boundaries of the interval
		 * \([0,1]\). Multiple reflections are applied if needed.
		 *
		 * @param trait the trait
		 * @return the reflected trait in \([0,1]\)
		 */
		static double reflect(double trait) {
			if (trait >= 0.0 && trait <= 1.0)
				return trait;
			double fold = trait % 2.0;
			if (fold < 0.0)
				fold += 2.0;
			return fold > 1.0 ? 2.0 - fold : fold;
		}

		/**
		 * Command line option to set the type of player updates.
		 */
		public final CLOption clo = new CLOption("mutation", "0.0",
				CLOCategory.Model,
				"--mutation <p> [temperature (default)|random] [<t> [<r>]>  with\n" +
						"             p: mutation probability\n" + //
						"             r: mutation range (fraction of interval)\n" + //
						"       process: reproduction vs cosmic rays\n" + //
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
						List<? extends Module<?>> species = module.getSpecies();
						for (Module<?> mod : species) {
							String mutts = mutations[n++ % mutations.length];
							String[] args = mutts.split("\\s+|=|,");
							Mutation mut = mod.getMutation();
							// set defaults; continuous traits mutate at reproduction by default
							mut.probability = 0.0;
							mut.temperature = true;
							mut.range = 0.0;
							mut.type = Type.NONE;
							// deal with optional temperature|random flag first
							args = mut.parseProcess(args);
							switch (args.length) {
								case 3:
									mut.range = CLOParser.parseDouble(args[2]);
									if (mut.range <= 0.0) {
										mut.type = Type.NONE;
										module.engine.logWarning((species.size() > 1 ? mod.getName() + ": " : "") + //
												"mutation range '" + args[2] + "' invalid - using '"
												+ mut.type + "'");
										success = false;
										continue;
									}
									//$FALL-THROUGH$
								case 2:
									Type mutt = (Type) clo.match(args[1]);
									if (mutt == null) {
										module.engine.logWarning((species.size() > 1 ? mod.getName() + ": " : "") + //
												"mutation type '" + args[1] + "' not recognized - using '"
												+ mut.type + "'");
										success = false;
										continue;
									}
									if ((mutt == Type.RANGE || mutt == Type.GAUSSIAN) && mut.range <= 0.0) {
										// no valid range specified, default to entire trait interval
										mut.type = Type.UNIFORM;
										module.engine.logWarning((species.size() > 1 ? mod.getName() + ": " : "") + //
												"no valid range found - using '"
												+ mut.type + "'");
										success = false;
										continue;
									}
									mut.type = mutt;
									//$FALL-THROUGH$
								case 1:
									mut.probability = Math.max(0.0, CLOParser.parseDouble(args[0]));
									if (mut.probability <= 0.0 || mut.type == Type.NONE) {
										mut.type = Type.NONE;
										mut.probability = 0.0;
									}
									break;
								case 0:
									// no arguments, stick to defaults
									break;
								default:
									// too many arguments
									module.engine.logWarning((species.size() > 1 ? mod.getName() + ": " : "") + //
											"too many mutation arguments '" + mutts + "' - using '"
											+ mut.type + "'");
									success = false;
							}
						}
						return success;
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
		 * <dt>uniform
		 * <dd>Mutate to any trait in trait interval, uniformly distributed.
		 * <dt>gaussian
		 * <dd>Mutate trait according to a Gaussian distribution around parental trait
		 * with standard deviation {@code s}.
		 * <dt>range
		 * <dd>Mutate trait uniformly distributed around parental trait {@code t} within
		 * a range {@code t &pm; r}.
		 * </dl>
		 * Gaussian and range mutations are reflected at the boundaries of the trait
		 * interval.
		 */
		public enum Type implements CLOption.Key {

			/**
			 * No mutations. This is the default
			 */
			NONE("none", "no mutations"),

			/**
			 * Mutate to any trait value.
			 */
			UNIFORM("all", "uniform mutations to any trait value"),

			/**
			 * Gaussian distributed mutations around parental trait.
			 */
			GAUSSIAN("gaussian", "Gaussian mutations, sdev <s>"),

			/**
			 * Mutate parental trait {@code t} to {@code t &pm; <r>}.
			 */
			RANGE("range", "mutations with range &pm; <r>");

			/**
			 * Key of player update. Used when parsing command line options.
			 * 
			 * @see Mutation.Continuous#clo
			 */
			String key;

			/**
			 * Brief description of player update for help display.
			 * 
			 * @see EvoLudo#getCLOHelp()
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
