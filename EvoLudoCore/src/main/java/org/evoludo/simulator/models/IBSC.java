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

import java.io.PrintStream;

import org.evoludo.simulator.EvoLudo;
import org.evoludo.simulator.modules.Module;
import org.evoludo.util.CLOParser;
import org.evoludo.util.CLOption;
import org.evoludo.util.CLOption.CLODelegate;
import org.evoludo.util.Formatter;

/**
 * Base class for individual based simulation models, IBS, with a single or
 * multiple continuous traits/strategies. This class deals with initialization
 * and mutation types but the heavy lifting is done by the {@link IBS} parent.
 * 
 * @author Christoph Hauert
 */
public class IBSC extends IBS implements Continuous {

	/**
	 * Creates a population of individuals for IBS simulations with continuous
	 * traits/strategies.
	 * 
	 * @param engine the pacemaker for running the model
	 */
	public IBSC(EvoLudo engine) {
		super(engine);
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
	 * The initialization of populations with continuous traits. This includes the
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
		 * @param ibs     the model using this initialization
		 * @param nTraits the number of traits
		 */
		public Init(org.evoludo.simulator.models.IBS ibs, int nTraits) {
			this.ibs = ibs;
			type = Type.UNIFORM;
			args = new double[nTraits][];
		}

		/**
		 * The population update type.
		 * 
		 * @see #clo
		 */
		Init.Type type;

		/**
		 * The array of arguments for the initialization of each trait.
		 */
		double[][] args;

		/**
		 * Get the arguments of this initialization type.
		 * 
		 * @return the arguments associated with this initialization type
		 * 
		 * @see #args
		 */
		public double[][] getArgs() {
			return args;
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
		public final CLOption clo = new CLOption("init", Init.Type.UNIFORM.getKey(), EvoLudo.catModel,
				"--init <t>      type of initial configuration", new CLODelegate() {
					@Override
					public boolean parse(String arg) {
						boolean success = true;
						for (Module mod : ibs.species) {
							IBSMCPopulation cpop = (IBSMCPopulation) mod.getIBSPopulation();
							String[] inittypes = arg.split(CLOParser.TRAIT_DELIMITER);
							int nt = mod.getNTraits();
							Init.Type prevtype = null;
							boolean isMultiSpecies = (ibs.species.size() > 1);
							for (int n = 0; n < nt; n++) {
								String inittype = inittypes[n % inittypes.length];
								double[] initargs = null;
								String[] typeargs = inittype.split("\\s+|=");
								Init.Type newtype = (Init.Type) clo.match(inittype);
								Init init = cpop.getInit();
								if (newtype == null && prevtype != null) {
									newtype = prevtype;
									initargs = CLOParser.parseVector(typeargs[0]);
								} else if (typeargs.length > 1)
									initargs = CLOParser.parseVector(typeargs[1]);
								boolean argsOk = (initargs != null && initargs.length >= newtype.nParams);
								// only uniform initialization does not require additional arguments
								if (newtype == null || (!newtype.equals(Init.Type.UNIFORM) && !argsOk)) {
									ibs.logger.warning(
											(isMultiSpecies ? mod.getName() + ": " : "") +
													"init '" + inittype + "' unknown!");
									newtype = Init.Type.UNIFORM;
									success = false;
								}
								init.type = newtype;
								init.args[n] = initargs;
								prevtype = newtype;
							}
						}
						return success;
					}

					@Override
					public void report(PrintStream output) {
						boolean isMultiSpecies = (ibs.species.size() > 1);
						for (Module mod : ibs.species) {
							IBSMCPopulation cpop = (IBSMCPopulation) mod.getIBSPopulation();
							Init init = cpop.getInit();
							output.println("# init:                 " + init.type + " " + //
									Formatter.format(init.args, 2) + (isMultiSpecies ? " ("
											+ mod.getName() + ")" : ""));
						}
					}
				});

		/**
		 * Type of initial density distribution of traits. Currently this model
		 * supports:
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
		 * @see #clo
		 * @see IBSMCPopulation#getInit()
		 */
		public enum Type implements CLOption.Key {

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
			Type(String key, String title, int nParams) {
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
	}

	@Override
	public void collectCLO(CLOParser parser) {
		super.collectCLO(parser);
		IBSMCPopulation pop = (IBSMCPopulation) species.get(0).getIBSPopulation();
		CLOption clo = pop.getInit().clo;
		clo.clearKeys();
		clo.addKeys(Init.Type.values());
		parser.addCLO(clo);
		// interacting with all members of the population is not feasible for continuous
		// traits; use single interaction with random neighbour as default
		cloInteractions.setDefault("random 1");
		// comparing scores with all members of the population is not feasible for
		// continuous traits; use single, random neighbour as default
		cloReferences.setDefault("random 1");
	}
}
