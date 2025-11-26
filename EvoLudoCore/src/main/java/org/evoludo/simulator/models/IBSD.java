//
// EvoLudo Project
//
// Copyright 2010-2025 Christoph Hauert
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

package org.evoludo.simulator.models;

import org.evoludo.simulator.geom.GeometryType;
import org.evoludo.math.ArrayMath;
import org.evoludo.simulator.EvoLudo;
import org.evoludo.simulator.geom.AbstractGeometry;
import org.evoludo.simulator.modules.Module;
import org.evoludo.util.CLOCategory;
import org.evoludo.util.CLODelegate;
import org.evoludo.util.CLOParser;
import org.evoludo.util.CLOption;
import org.evoludo.util.Formatter;

/**
 * Base class for individual based simulation models, IBS, with discrete traits.
 * This class deals with optimizations, initialization types and statistics but
 * the heavy lifting is done by the {@link IBS} parent.
 * 
 * @author Christoph Hauert
 */
public class IBSD extends IBS implements DModel {

	/**
	 * Creates a population of individuals for IBS simulations with discrete
	 * traits.
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
		fixData.typeFixed = ArrayMath.maxIndex(dpop.traitsCount);
		Module<?> module = species.get(0);
		int vacant = module.getVacantIdx();
		if (fixData.typeFixed == vacant) {
			// closer look is needed - look for what other traits survived (if any)
			for (int n = 0; n < module.getNTraits(); n++) {
				if (n == vacant)
					continue;
				if (dpop.traitsCount[n] > 0) {
					// no other traits should be present
					fixData.typeFixed = n;
				}
			}
		}
		fixData.timeFixed = time;
		fixData.updatesFixed = updates;
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
		// - no vacant sites or monostop (otherwise extinction is the only absorbing
		// state)
		// - for well-mixed populations any frequency is acceptable
		for (Module<?> mod : species) {
			IBSDPopulation dpop = (IBSDPopulation) mod.getIBSPopulation();
			AbstractGeometry inter = dpop.interaction;
			boolean isWM = inter.isType(GeometryType.WELLMIXED)
					&& (inter.isSingle()
							|| dpop.competition.isType(GeometryType.WELLMIXED));
			Init.Type itype = dpop.getInit().type;
			if (!isWM && !(itype.equals(Init.Type.MUTANT) ||
					itype.equals(Init.Type.TEMPERATURE)))
				return false;
		}
		return true;
	}

	@Override
	public void unload() {
		super.unload();
		cloOptimize.clearKeys();
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
	public void getInitialTraits(double[] init) {
		if (isMultispecies) {
			int skip = 0;
			int idx = 0;
			double[] tmp = new double[init.length];
			while (idx < nSpecies) {
				IBSDPopulation pop = getIBSDPopulation(idx++);
				pop.getInitialTraits(tmp);
				System.arraycopy(tmp, 0, init, skip, pop.nTraits);
				skip += pop.nTraits;
			}
		} else
			((IBSDPopulation) population).getInitialTraits(init);
	}

	@Override
	public void getInitialTraits(int id, double[] init) {
		getIBSDPopulation(id).getInitialTraits(init);
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
		for (Module<?> mod : species) {
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
	public double getMonoScore(int id, int idx) {
		return getIBSDPopulation(id).getMonoScore(idx);
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
		public final CLOption clo = new CLOption("init", Init.Type.UNIFORM.getKey(), CLOCategory.Model, null,
				new CLODelegate() {
					@Override
					public boolean parse(String arg) {
						String[] inittypes = arg.split(CLOParser.SPECIES_DELIMITER);
						int idx = 0;
						Init.Type prevtype = null;
						for (Module<?> mod : ibs.species) {
							IBSDPopulation dpop = (IBSDPopulation) mod.getIBSPopulation();
							String itype = inittypes[idx++ % inittypes.length];
							double[] initargs = null;
							String[] typeargs = itype.split(CLOParser.SPLIT_ARG_REGEX);
							Init.Type newtype = (Init.Type) clo.match(itype);
							Init init = dpop.getInit();
							if (newtype == null && prevtype != null) {
								newtype = prevtype;
								initargs = CLOParser.parseVector(typeargs[0]);
							} else if (typeargs.length > 1)
								initargs = CLOParser.parseVector(typeargs[1]);
							// only uniform or kaleidoscope initializations do not require additional
							// arguments
							if (newtype == null || (initargs == null && !(newtype.equals(Init.Type.UNIFORM)
									|| newtype.equals(Init.Type.KALEIDOSCOPE))))
								return false;
							init.type = newtype;
							init.args = initargs;
							prevtype = newtype;
						}
						return true;
					}

					@Override
					public String getDescription() {
						String descr = "--init <t>      type of initial configuration:\n" + clo.getDescriptionKey()
								+ "\n                with r, m indices of resident, mutant traits";
						boolean noVacant = false;
						for (Module<?> mod : ibs.species)
							noVacant |= mod.getVacantIdx() < 0;
						if (noVacant)
							return descr.replace("[,v]", "");
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
			STRIPES("stripes", "stripes of traits");

			/**
			 * Key of initialization type. Used when parsing command line options.
			 * 
			 * @see Init#clo
			 */
			String key;

			/**
			 * Brief description of initialization type for help display.
			 * 
			 * @see EvoLudo#getCLOHelp()
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
	public enum OptimizationType implements CLOption.Key {

		/**
		 * No optimization (default).
		 */
		NONE("none", "no optimization"),

		/**
		 * Skip homogeneous states by introducing a mutant and advancing the time
		 * according to an exponential distribution for an event happening with
		 * probability {@code Module#getMutationProb()}.
		 */
		HOMO("homo", "skip homogeneous states (single species only)"),

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
		 * @see EvoLudo#getCLOHelp()
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
	public final CLOption cloOptimize = new CLOption("optimize", "none", CLOCategory.Model,
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
					// reset all optimizations
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
									continue;
								}
								for (Module<?> mod : species) {
									IBSDPopulation dpop = (IBSDPopulation) mod.getIBSPopulation();
									dpop.optimizeHomo = true;
								}
								break;
							case MORAN:
								for (Module<?> mod : species) {
									IBSDPopulation dpop = (IBSDPopulation) mod.getIBSPopulation();
									dpop.optimizeMoran = true;
								}
								break;
							case NONE:
							default: // no optimizations
								for (Module<?> mod : species) {
									IBSDPopulation dpop = (IBSDPopulation) mod.getIBSPopulation();
									dpop.optimizeMoran = false;
									dpop.optimizeHomo = false;
								}
								break;
						}
					}
					return true;
				}
			});

	@Override
	public void collectCLO(CLOParser parser) {
		super.collectCLO(parser);
		IBSDPopulation pop = (IBSDPopulation) species.get(0).getIBSPopulation();
		CLOption clo = pop.getInit().clo;
		clo.clearKeys();
		clo.addKeys(Init.Type.values());
		// kaleidoscopes are not standard and must be requested/enabled by modules and
		// their IBSDPopulation implementations.
		clo.removeKey(Init.Type.KALEIDOSCOPE);
		parser.addCLO(clo);
		parser.addCLO(cloOptimize);
		cloOptimize.addKeys(OptimizationType.values());
	}
}