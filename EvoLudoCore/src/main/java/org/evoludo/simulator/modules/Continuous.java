//
// EvoLudo Project
//
// Copyright 2010 Christoph Hauert
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
//	Hauert, Christoph (<year>) EvoLudo Project, http://www.evoludo.org
//			(doi: <doi>[, <version>])
//
//	<doi>:	digital object identifier of the downloaded release (or the
//			most recent release if downloaded from github.com),
//	<year>:	year of release (or download), and
//	[, <version>]: optional version number (as reported in output header
//			or GUI console) to simplify replication of reported results.
//
// The formatting may be adjusted to comply with publisher requirements.
//

package org.evoludo.simulator.modules;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;

import org.evoludo.math.ArrayMath;
import org.evoludo.simulator.EvoLudo;
import org.evoludo.util.CLOParser;
import org.evoludo.util.CLOption;
import org.evoludo.util.CLOption.CLODelegate;
import org.evoludo.util.Formatter;

/**
 * Parent class of all EvoLudo modules with one or more continuous traits.
 * 
 * @author Christoph Hauert
 */
public abstract class Continuous extends Module {

	/**
	 * All modules that admit interactions in pairs (as opposed to larger groups)
	 * should implement this interface. The continuous snowdrift game is an example,
	 * see {@link org.evoludo.simulator.modules.CSD}.
	 * 
	 * @author Christoph Hauert
	 */
	public interface Pairs extends Features {

		@Override
		public default boolean isContinuous() {
			return true;
		}

		@Override
		public default boolean isPairwise() {
			return true;
		}

		/**
		 * Calculate the payoff/score for modules with interactions in pairs and a
		 * single continuous trait. The focal individual has trait {@code me} and the
		 * traits of its {@code len} interaction partners are given in {@code group}.
		 * The payoffs/scores for each of the {@code len} opponent
		 * traits/strategies must be stored and returned in the array
		 * {@code payoffs}.
		 * 
		 * <h3>Note:</h3> Only the first {@code len} entries in {@code group} are
		 * guaranteed to exist and have meaningful values. The population structure may
		 * restrict the size of the interaction group. {@code len&le;nGroup} always
		 * holds.
		 * 
		 * <h3>Important:</h3> must be overridden and implemented in subclasses that
		 * define game interactions between pairs of individuals
		 * ({@code nGroup=2}, {@code pairwise=true}), otherwise see
		 * {@link Groups#groupScores(double, double[], int, double[])}.
		 * 
		 * @param me      the trait of the focal individual
		 * @param group   the traits of the group members
		 * @param len     the number of memebrs in the group
		 * @param payoffs the array for returning the payoffs/scores for each group
		 *                member
		 * @return the total (accumulated) payoff/score for the focal individual
		 */
		public double pairScores(double me, double[] group, int len, double[] payoffs);
	}

	/**
	 * All modules that admit interactions in larger groups (as opposed to
	 * interactions in pairs) should implement this interface.
	 * 
	 * @author Christoph Hauert
	 */
	public interface Groups extends Pairs {

		/**
		 * Get the interaction group size.
		 * 
		 * @return the interaction group size
		 */
		public abstract int getNGroup();

		@Override
		public default boolean isPairwise() {
			return (getNGroup() == 2);
		}

		/**
		 * Calculate the payoff/score for modules with interactions in groups and a
		 * single continuous trait. The focal individual has trait {@code me} and the
		 * traits of its {@code len} interaction partners are given in {@code group}.
		 * The payoffs/scores for each of the {@code len} participants must be
		 * stored and returned in the array {@code payoffs}.
		 * 
		 * <h3>Note:</h3> Only the first {@code len*nTraits} entries in {@code group}
		 * are guaranteed to exist and have meaningful values. The population structure
		 * may restrict the size of the interaction group. {@code len&le;nGroup} always
		 * holds.
		 * 
		 * <h3>Important:</h3> Must be overridden and implemented in subclasses that
		 * define game interactions among groups of individuals with multiple continuous
		 * traits (for groups with sizes {@code nGroup&gt;2}, otherwise see
		 * {@link #pairScores(double, double[], int, double[])}).
		 * 
		 * @param me      the trait of the focal individual
		 * @param group   the traits of the group members
		 * @param len     the number of members in the group
		 * @param payoffs the array for returning the payoffs/scores for each group
		 *                member
		 * @return the payoff/score for the focal individual
		 */
		public double groupScores(double me, double[] group, int len, double[] payoffs);
	}

	/**
	 * All modules that admit interactions in pairs (as opposed to larger groups)
	 * should implement this interface. The division of labour module is an example,
	 * see {@link org.evoludo.simulator.modules.CLabour}.
	 * 
	 * @author Christoph Hauert
	 */
	public interface MultiPairs extends Features {

		@Override
		public default boolean isContinuous() {
			return true;
		}

		@Override
		public default boolean isPairwise() {
			return true;
		}

		/**
		 * Calculate the payoff/score for modules with interactions in pairs and
		 * multiple continuous traits. The focal individual has traits {@code me} and
		 * the traits of its {@code len} interaction partners are given in
		 * {@code group}. The traits they are arranged in the usual manner, i.e. first
		 * all traits of the first group member then all traits by the second group
		 * member etc. for a total of {@code len*nTraits} entries. The payoffs/scores
		 * for each of the {@code len} opponent traits/strategies must be stored
		 * and returned in the array {@code payoffs}.
		 * 
		 * <h3>Note:</h3> Only the first {@code len} entries in {@code group} are
		 * guaranteed to exist and have meaningful values. The population structure may
		 * restrict the size of the interaction group. {@code len&le;nGroup} always
		 * holds.
		 * 
		 * <h3>Important:</h3> must be overridden and implemented in subclasses that
		 * define game interactions between pairs of individuals
		 * ({@code nGroup=2}, {@code pairwise=true}), otherwise see
		 * {@link Groups#groupScores(double, double[], int, double[])}.
		 * 
		 * @param me      the trait of the focal individual
		 * @param group   the traits of the group members
		 * @param len     the number of memebrs in the group
		 * @param payoffs the array for returning the payoffs/scores for each group
		 *                member
		 * @return the total (accumulated) payoff/score for the focal individual
		 */
		public double pairScores(double me[], double[] group, int len, double[] payoffs);
	}

	/**
	 * All modules that admit interactions in larger groups (as opposed to
	 * interactions in pairs) with multiple traits should implement this interface.
	 * 
	 * @author Christoph Hauert
	 */
	public interface MultiGroups extends MultiPairs {

		/**
		 * Get the interaction group size.
		 * 
		 * @return the interaction group size
		 */
		public abstract int getNGroup();

		@Override
		public default boolean isPairwise() {
			return getNGroup() == 2;
		}

		/**
		 * Calculate the payoff/score for modules with interactions in groups and
		 * multiple single continuous traits. The focal individual has traits {@code me}
		 * and the traits of its {@code len} interaction partners are given in
		 * {@code group}. The traits they are arranged in the usual manner, i.e. first
		 * all traits of the first group member then all traits by the second group
		 * member etc. for a total of {@code len*nTraits} entries. The payoffs/scores
		 * for each of the {@code len} participants must be stored and returned in
		 * the array {@code payoffs}.
		 * 
		 * <h3>Note:</h3> Only the first {@code len*nTraits} entries in {@code group}
		 * are guaranteed to exist and have meaningful values. The population structure
		 * may restrict the size of the interaction group. {@code len&le;nGroup} always
		 * holds.
		 * 
		 * <h3>Important:</h3> must be overridden and implemented in subclasses that
		 * define game interactions among groups of individuals with multiple continuous
		 * traits (for groups with sizes {@code nGroup&gt;2}, otherwise see
		 * {@link #pairScores(double[], double[], int, double[])}).
		 * 
		 * @param me      the traits of the focal individual
		 * @param group   the traits of the group members
		 * @param len     the number of memebrs in the group
		 * @param payoffs the array for returning the payoffs/scores for each group
		 *                member
		 * @return the payoff/score for the focal individual
		 */
		public double groupScores(double me[], double[] group, int len, double[] payoffs);
	}

	/**
	 * The list {@code species} contains references to each species in this
	 * module. It deliberately shadows {@link Module#species} to simplify
	 * bookkeeping. During instantiation {@link Module#species} and
	 * {@code species} are linked to represent one and the same list.
	 * <p>
	 * <strong>IMPORTANT:</strong> currently continuous models support only a single
	 * species.
	 *
	 * @see #Continuous(EvoLudo, Continuous)
	 * @see Module#species
	 */
	@SuppressWarnings("hiding")
	ArrayList<Continuous> species;

	/**
	 * Shortcut for species.get(0) as long as continuous modules are restricted to a
	 * single species.
	 */
	Continuous population;

	/**
	 * The flag that indicates whether maximal and minimal scores have already been
	 * calculated.
	 * 
	 * @see #setExtremalScores()
	 */
	protected boolean extremalScoresSet = false;

	/**
	 * The index for storing the trait mean in an array.
	 */
	public final static int TRAIT_MEAN = 0;

	/**
	 * The index for storing the standard deviation of a trait in an array.
	 */
	public final static int TRAIT_SDEV = 1;

	/**
	 * Mean and sdev of initial trait distribution. The mean for trait {@code i} is
	 * stored in position {@code init[i][TRAIT_MEAN]} and the corresponding standard
	 * deviation in {@code init[i][TRAIT_SDEV]}.
	 * <p>
	 * <strong>Note:</strong> internally traits are always in \([0,1]\).
	 * {@code traitMin} and {@code traitMax} are used to transform traits
	 * appropriately for results.
	 */
	protected double[][] init;

	/**
	 * The trait minima.
	 * <p>
	 * <strong>Note:</strong> internally traits are always in \([0,1]\).
	 * {@code traitMin} and {@code traitMax} are used to transform traits
	 * appropriately for results.
	 */
	protected double[] traitMin;

	/**
	 * The trait maxima.
	 * <p>
	 * <strong>Note:</strong> internally traits are always in \([0,1]\).
	 * {@code traitMin} and {@code traitMax} are used to transform traits
	 * appropriately for results.
	 */
	protected double[] traitMax;

	/**
	 * Standard deviation of mutations.
	 * <p>
	 * <strong>Note:</strong> scaled in accordance to to the trait range given by
	 * {@code traitMin} and {@code traitMax}.
	 */
	protected double[] mutSdev;

	/**
	 * Create new module with continuous traits.
	 * 
	 * @param engine the pacemeaker for running the model
	 */
	protected Continuous(EvoLudo engine) {
		this(engine, null);
	}

	/**
	 * Create another module with continuous traits. The additional module
	 * represents another species in multi-species modules that interact with
	 * species {@code partner}.
	 * 
	 * @param partner the partner species
	 */
	protected Continuous(Continuous partner) {
		this(partner.engine, partner);
	}

	/**
	 * Create a new module with continuous traits for pacemaker {@code engine} and
	 * interactions with module {@code partner}. If {@code partner == null} this is
	 * a single species module and interactions within species
	 * ({@code opponent == this} holds).
	 * 
	 * @param engine  the pacemeaker for running the model
	 * @param partner the partner species
	 */
	protected Continuous(EvoLudo engine, Continuous partner) {
		super(engine, partner);
		if (partner == null) {
			species = new ArrayList<Continuous>();
			// recall this.species shadows super.species for later convenience
			super.species = species;
		} else {
			// link ArrayList<Discrete> shadows
			species = partner.species;
		}
		add(this);
		// useful shortcut as long as continuous modules are restricted to single
		// species
		population = this;
	}

	/**
	 * Add {@code cpop} to list of species. Duplicate entries are ignored.
	 * Allocate new list if necessary. Assign generic name to species if none
	 * provided.
	 *
	 * @param cpop the module to add to species list.
	 * @return {@code true} if {@code dpop} successfully added;
	 *         {@code false} adding failed or already included in list.
	 */
	public boolean add(Continuous cpop) {
		// do not add duplicates
		if (species.contains(cpop))
			return false;
		if (!species.add(cpop))
			return false;
		switch (species.size()) {
			case 1:
				break;
			case 2:
				// start naming species (if needed)
				for (Module pop : species) {
					if (pop.getName().length() < 1)
						pop.setName("Species-" + pop.ID);
				}
				break;
			default:
				if (cpop.getName().length() < 1)
					cpop.setName("Species-" + cpop.ID);
		}
		return true;
	}

	/**
	 * The map to translate traitsof interacting individuals into payoffs.
	 */
	protected Traits2Payoff traits2payoff;

	@Override
	public void load() {
		super.load();
		traits2payoff = new Traits2Payoff(this);
	}

	@Override
	public void unload() {
		super.unload();
		traits2payoff = null;
		cloFitnessMap.clearKeys();
		cloPlayerUpdate.clearKeys();
	}

	@Override
	public boolean check() {
		boolean doReset = super.check();
		doReset |= traits2payoff.check();
		setExtremalScores();
		// verify trait minima and maxima
		for (int s = 0; s < nTraits; s++) {
			if (traitMax[s] <= traitMin[s]) {
				// set to default
				logger.warning("invalid trait range [" + Formatter.format(traitMin[s], 4) + ", "
						+ Formatter.format(traitMax[s], 4) + "] for trait " + s + " - reset to [0, 1]!");
				setTraitRange(0.0, 1.0, s);
				doReset = true;
			}
			if (init[s][TRAIT_MEAN] > traitMax[s] || init[s][TRAIT_MEAN] < traitMin[s]) {
				double newmean = Math.min(traitMax[s], Math.max(init[s][TRAIT_MEAN], traitMin[s]));
				logger.warning("initial mean (" + Formatter.format(init[s][TRAIT_MEAN], 4) + ") not in trait range ["
						+ Formatter.format(traitMin[s], 4) + ", " + Formatter.format(traitMax[s], 4) + "] for trait "
						+ s + " - changed to " + Formatter.format(newmean, 4) + "!");
				init[s][TRAIT_MEAN] = newmean;
			}
		}
		return doReset;
	}

	@Override
	public boolean isContinuous() {
		return true;
	}

	/**
	 * Set the mean and standard deviation of the initial trait distribution.
	 * 
	 * @param init the mean and standard deviation
	 * 
	 * @see #init
	 */
	public void setInit(double[][] init) {
		if (init == null || init.length != nTraits || init[0].length != 2)
			return;
		this.init = ArrayMath.clone(init);
	}

	/**
	 * Get the mean and standard deviation of the initial trait distribution.
	 * 
	 * @return the mean and standard deviation
	 */
	public double[][] getInit() {
		if (init == null) {
			init = new double[nTraits][];
			for (int n = 0; n < nTraits; n++)
				init[n] = new double[] { 0.5, 0.1 };
		}
		return ArrayMath.clone(init);
	}

	/**
	 * Set the mean and standard deviation of the initial distribution of trait
	 * {@code trait}.
	 * 
	 * @param init  the mean and standard deviation
	 * @param trait the index of the trait
	 */
	public void setInit(double[] init, int trait) {
		if (init == null || init.length != 2)
			return;
		if (this.init == null || this.init.length != nTraits || this.init[0].length != 2)
			this.init = new double[nTraits][2];
		System.arraycopy(init, 0, this.init[trait], 0, 2);
	}

	/**
	 * Get the mean and standard deviation of the initial distribution of trait
	 * {@code trait}.
	 * 
	 * @param trait the index of the trait
	 * @return the mean and standard deviation
	 */
	public double[] getInit(int trait) {
		return ArrayMath.clone(init[trait]);
	}

	/**
	 * {@inheritDoc}
	 *
	 * @see #setExtremalScores()
	 */
	@Override
	public double getMinGameScore() {
		if (!extremalScoresSet)
			setExtremalScores();
		return cxMinScore;
	}

	/**
	 * {@inheritDoc}
	 *
	 * @see #setExtremalScores()
	 */
	@Override
	public double getMaxGameScore() {
		if (!extremalScoresSet)
			setExtremalScores();
		return cxMaxScore;
	}

	/**
	 * {@inheritDoc}
	 *
	 * @see #setExtremalScores()
	 */
	@Override
	public double getMinMonoGameScore() {
		if (!extremalScoresSet)
			setExtremalScores();
		return cxMinMonoScore;
	}

	/**
	 * {@inheritDoc}
	 *
	 * @see #setExtremalScores()
	 */
	@Override
	public double getMaxMonoGameScore() {
		if (!extremalScoresSet)
			setExtremalScores();
		return cxMaxMonoScore;
	}

	/**
	 * Get the minima for all traits.
	 *
	 * @return the array with the trait minima
	 */
	public double[] getTraitMin() {
		return traitMin;
	}

	/**
	 * Get the maxima for all traits.
	 *
	 * @return the array with the trait maxima
	 */
	public double[] getTraitMax() {
		return traitMax;
	}

	/**
	 * Set the trait minimum and maximum for trait {@code trait}.
	 * 
	 * @param min   the trait minimum
	 * @param max   the trait maximum
	 * @param trait the index of the trait
	 */
	public void setTraitRange(double min, double max, int trait) {
		if (traitMin == null || traitMin.length != nTraits) {
			traitMin = new double[nTraits];
			Arrays.fill(traitMin, 0.0);
		}
		if (traitMax == null || traitMax.length != nTraits) {
			traitMax = new double[nTraits];
			Arrays.fill(traitMax, 0.0);
		}
		if (trait < 0 || trait >= nTraits || min >= max)
			return;
		traitMax[trait] = max;
		traitMin[trait] = min;
		extremalScoresSet = false; // update extremal scores
	}

	/**
	 * Set the standard deviation of Gaussian mutations in each trait.
	 *
	 * @param sdev the array specifying standard deviation of mutations
	 */
	public void setMutationSdev(double[] sdev) {
		if (sdev == null || sdev.length != nTraits)
			return; // invalid argument, ignore
		mutSdev = sdev;
	}

	/**
	 * Get the standard deviation of Gaussian mutations in each trait.
	 *
	 * @return the standard deviation of Gaussian mutations
	 */
	public double[] getMutationSdev() {
		return ArrayMath.clone(mutSdev);
	}

	/**
	 * Command line option to set the parameters of the initial trait distribution.
	 * For example, mean and standard deviation for a Gaussian distribution of
	 * traits.
	 * 
	 * @see org.evoludo.simulator.models.IBSC.InitType models.IBSC.InitType
	 * @see EvoLudo#cloInitType
	 */
	public final CLOption cloInit = new CLOption("init", "0.2,0.02", EvoLudo.catModel, null,
			new CLODelegate() {

				/**
				 * {@inheritDoc}
				 * <p>
				 * Parse initial trait distribution of each trait. {@code arg} can be a single
				 * {@code(mean, sdev)} pair or an array of pairs with the separator
				 * {@value CLOParser#MATRIX_DELIMITER}. The parser cycles through {@code arg}
				 * until the initial configuration of each trait is set.
				 * 
				 * @param arg the (array of) of initial configurations
				 */
				@Override
				public boolean parse(String arg) {
					// if parsing of a double throws an exception this returns null
					double[][] msinit = CLOParser.parseMatrix(arg);
					if (msinit == null || msinit.length == 0 || msinit[0].length < 2) {
						logger.warning("failed to parse initialization parameters '" + arg + "' - ignored.");
						return false;
					}
					for (int n = 0; n < nTraits; n++)
						population.setInit(msinit[n % msinit.length], n);
					return true;
				}

				@Override
				public void report(PrintStream output) {
					for (int n = 0; n < nTraits; n++)
						output.println(
								"# init (mean&plusmn;sdev):                 " + Formatter.format(init[n][0], 6)
										+ " &plusmn; " + Formatter.format(init[n][1], 6) + "\t" + traitName[n]);
				}

				@Override
				public String getDescription() {
					String descr = "--init, -I";
					switch (nTraits) {
						case 2:
							descr += "<0>,<1>  initial frequencies of strategies, with";
							break;
						case 3:
							descr += "<0>,<1>,<2>  initial frequencies of strategies, with";
							break;
						default:
							descr += "<0>,...,<" + (nTraits - 1)
									+ ">  initial frequencies of strategies, with";
					}
					for (int n = 0; n < nTraits; n++) {
						String aTrait = "              " + n + ": ";
						int traitlen = aTrait.length();
						descr += "\n" + aTrait.substring(traitlen - 16, traitlen) + traitName[n];
					}
					return descr;
				}
			});

	/**
	 * Command line option to set the minimum value of each trait.
	 */
	public final CLOption cloTraitRange = new CLOption("traitrange", "0,1", EvoLudo.catModel, null,
			new CLODelegate() {

				/**
				 * {@inheritDoc}
				 * <p>
				 * Parse the minimum value of each trait. {@code arg} can be a single value or
				 * an array with the separator {@value CLOParser#MATRIX_DELIMITER}. The parser
				 * cycles through {@code arg} until the minimum value of each trait is set.
				 * 
				 * @param arg the (array of) of minimum trait values
				 */
				@Override
				public boolean parse(String arg) {
					// getting ready for multiple species - way ahead of its time...
					String[] speciestraits = arg.split(CLOParser.SPECIES_DELIMITER);
					if (speciestraits == null) {
						logger.warning("traitrange specification '" + arg + "' not recognized - ignored!");
						return false;
					}
					int n = 0;
					for (Continuous cpop : species) {
						String[] traitranges = speciestraits[n++ % speciestraits.length].split(CLOParser.TRAIT_DELIMITER);
						for (int i = 0; i < nTraits; i++) {
						String trange = traitranges[i % traitranges.length];
						double[] range = CLOParser.parseVector(trange);
						if (range==null || range.length<2 || range[0] > range[1]) {
							logger.warning("invalid traitrange '" + trange + "' - using [0,1]!");
							cpop.setTraitRange(0.0, 1.0, i);
							continue;
						}
						cpop.setTraitRange(range[0], range[1], i);
					}
					}
					return true;
				}

				@Override
				public void report(PrintStream output) {
					String msg = "";
					for (Continuous cpop : species) {
						double[] mins = cpop.getTraitMin();
						double[] maxs = cpop.getTraitMax();
						String[] names = cpop.getTraitNames();
						for (int n = 0; n < nTraits; n++) {
							msg = "# traitrange:           " + Formatter.format(mins[n], 4) + "-"
									+ Formatter.format(maxs[n], 4) + //
									" " + names[n] + (species.size() > 1 && n == 0 ? " (" + cpop.getName() + ")" : "");
						}
					}
					output.println(msg);
				}

				@Override
				public String getDescription() {
					switch (nTraits) {
						case 1:
							return "--traitrange <min" + CLOParser.VECTOR_DELIMITER + "max>  range of trait " + traitName[0];
						case 2:
							return "--traitrange <min0" + CLOParser.VECTOR_DELIMITER + "max0" + //
										CLOParser.TRAIT_DELIMITER + "min1" + CLOParser.VECTOR_DELIMITER + "max1]>" + //
										CLOParser.VECTOR_DELIMITER + "  range of traits, with\n"
									+ "             0: " + traitName[0] + "\n" //
									+ "             1: " + traitName[1];
						default:
							String descr = "--traitrange <min0,max0[" + CLOParser.VECTOR_DELIMITER + "..."
									+ CLOParser.VECTOR_DELIMITER + "min" + (nTraits - 1)
									+ ",max" + (nTraits - 1) + "]>  range of traits, with";
							for (int n = 0; n < nTraits; n++) {
								String aTrait = "              " + n + ": ";
								int traitlen = aTrait.length();
								descr += "\n" + aTrait.substring(traitlen - 16, traitlen) + traitName[n];
							}
							return descr;
					}
				}
			});

	/**
	 * Command line option to set the standard deviation of mutations in each trait.
	 */
	public final CLOption cloMutationSdev = new CLOption("mutationsdev", "0.01", EvoLudo.catModel, null,
			new CLODelegate() {

				/**
				 * {@inheritDoc}
				 * <p>
				 * Parse the standard deviation of mutations in each trait. {@code arg} can be a
				 * single value or an array with the separator
				 * {@value CLOParser#MATRIX_DELIMITER}. The parser cycles through {@code arg}
				 * until the standard deviation of mutations in each trait is set.
				 * 
				 * @param arg the (array of) of standard deviation of mutations
				 */
				@Override
				public boolean parse(String arg) {
					// getting ready for multiple species - way ahead of its time...
					String[] traitsdevs = arg.split(CLOParser.SPECIES_DELIMITER);
					if (traitsdevs == null) {
						logger.warning("mutationsdev specification '" + arg + "' not recognized - ignored!");
						return false;
					}
					int n = 0;
					for (Continuous cpop : species) {
						String[] sdevs = traitsdevs[n++ % traitsdevs.length].split(CLOParser.VECTOR_DELIMITER);
						int nt = cpop.getNTraits();
						double[] sdev = new double[nt];
						for (int i = 0; i < nt; i++)
							sdev[i] = CLOParser.parseDouble(sdevs[i % sdevs.length]);
						cpop.setMutationSdev(sdev);
					}
					return true;
				}

				@Override
				public void report(PrintStream output) {
					double[] fvec = getMutationSdev();
					String msg = "# mutationsdev:         " + Formatter.format(fvec[0], 4);
					for (int n = 1; n < nTraits; n++)
						msg += ":" + Formatter.format(fvec[n], 4);
					output.println(msg);
				}

				@Override
				public String getDescription() {
					switch (nTraits) {
						case 1:
							return "--mutationsdev <s>  sdev of mutations in trait " + traitName[0];
						case 2:
							return "--mutationsdev <s0>" + CLOParser.VECTOR_DELIMITER
									+ "<s1> sdev of mutations in each trait, with\n" //
									+ "             0: " + traitName[0] + "\n" //
									+ "             1: " + traitName[1];
						default:
							String descr = "--mutationsdev <s0>" + CLOParser.VECTOR_DELIMITER + "..."
									+ CLOParser.VECTOR_DELIMITER + "<s" + (nTraits - 1)
									+ ">  sdev of mutations in each trait, with";
							for (int n = 0; n < nTraits; n++) {
								String aTrait = "              " + n + ": ";
								int traitlen = aTrait.length();
								descr += "\n" + aTrait.substring(traitlen - 16, traitlen) + traitName[n];
							}
							return descr;
					}
				}
			});

	/**
	 * Command line option to set the cost function(s) for continuous traits.
	 * 
	 * @see Traits2Payoff.Costs
	 */
	public final CLOption cloCostFunction = new CLOption("costfcn", "1", EvoLudo.catModel, null,
			new CLODelegate() {

				/**
				 * {@inheritDoc}
				 * <p>
				 * Parse cost functions for each trait. {@code arg} can be a single value or an
				 * array of values with the separator {@value CLOParser#VECTOR_DELIMITER}. The
				 * parser cycles through {@code arg} until all cost functions are set.
				 * 
				 * @param arg the (array of) cost function codes
				 */
				@Override
				public boolean parse(String arg) {
					String[] cstf = arg.split(CLOParser.VECTOR_DELIMITER);
					int ncstf = cstf.length;
					if (ncstf < 1) {
						logger.warning("failed to parse cost function type '" + arg + "' - ignored.");
						return false;
					}
					boolean success = true;
					for (int s = 0; s < nTraits; s++) {
						String type = cstf[s % ncstf];
						Traits2Payoff.Costs cft = (Traits2Payoff.Costs) cloCostFunction.match(type);
						if (cft == null) {
							logger.warning("cost function type '" + type + "' unknown - using '"
									+ traits2payoff.getCostFunctions()[s].getTitle() + "'");
							success = false;
							continue;
						}
						traits2payoff.setCostFunctions(cft, s);
					}
					return success;
				}

				@Override
				public void report(PrintStream output) {
					Traits2Payoff.Costs[] cfunc = traits2payoff.getCostFunctions();
					String msg = "# costfunction:         " + cfunc[0];
					for (int n = 1; n < nTraits; n++)
						msg += ":" + cfunc[n];
					output.println(msg);
				}

				@Override
				public String getDescription() {
					switch (nTraits) {
						case 1:
							return "--costfcn <s>   cost function of trait " + traitName[0] + "\n" //
									+ "                cost functions: <s>\n" + cloCostFunction.getDescriptionKey();
						case 2:
							return "--costfcn <s0>" + CLOParser.VECTOR_DELIMITER + "<s1>  cost function of traits\n" //
									+ "             0: " + traitName[0] + "\n" //
									+ "             1: " + traitName[1] + "\n" //
									+ "                cost functions: <s>\n" + cloCostFunction.getDescriptionKey();
						default:
							String descr = "--costfcn <s0>" + CLOParser.VECTOR_DELIMITER + "..."
									+ CLOParser.VECTOR_DELIMITER + "<s" + (nTraits - 1)
									+ ">  cost function of traits";
							for (int n = 0; n < nTraits; n++) {
								String aTrait = "              " + n + ": ";
								int traitlen = aTrait.length();
								descr += "\n" + aTrait.substring(traitlen - 16, traitlen) + traitName[n];
							}
							descr += "\n                cost functions: <s>\n" + cloCostFunction.getDescriptionKey();
							return descr;
					}
				}
			});

	/**
	 * Command line option to set the parameters of the cost function for continuous
	 * traits.
	 * 
	 * @see Traits2Payoff.Costs
	 */
	public final CLOption cloCostParams = new CLOption("costparams", "0", EvoLudo.catModel,
			"--costparams <c0>" + CLOParser.VECTOR_DELIMITER //
					+ "<c1>" + CLOParser.VECTOR_DELIMITER + "..." + CLOParser.VECTOR_DELIMITER //
					+ "<cn>  parameters for cost function" //
					+ (nTraits > 1 ? "\n         different traits separated by '" + CLOParser.MATRIX_DELIMITER + "'"
							: ""),
			new CLODelegate() {

				/**
				 * {@inheritDoc}
				 * <p>
				 * Parse cost function parameters for each trait. {@code arg} can be a single
				 * value or an array of values. Parameters are separated by
				 * {@value CLOParser#VECTOR_DELIMITER} and traits by
				 * {@value CLOParser#MATRIX_DELIMITER}. The parser cycles through {@code arg}
				 * until all parameters of every trait are set.
				 * 
				 * @param arg the (array of) cost function parameters
				 */
				@Override
				public boolean parse(String arg) {
					double[][] cparams = CLOParser.parseMatrix(arg);
					int len = cparams.length;
					if (len == 0) {
						logger.warning("failed to parse cost parameters '" + arg + "' - ignored.");
						return false;
					}
					for (int n = 0; n < nTraits; n++)
						traits2payoff.setCostParameters(cparams[n % cparams.length], n);
					return true;
				}

				@Override
				public void report(PrintStream output) {
					double[][] dvec = traits2payoff.getCostParameters();
					String msg = "# costparams:           " + Formatter.format(dvec[0], 6);
					for (int n = 1; n < nTraits; n++)
						msg += ";" + Formatter.format(dvec[n], 6);
					output.println(msg);
				}
			});

	/**
	 * Command line option to set the benefit function(s) for continuous traits.
	 * 
	 * @see Traits2Payoff.Benefits
	 */
	public final CLOption cloBenefitFunction = new CLOption("benefitfcn", "11", EvoLudo.catModel, null,
			new CLODelegate() {

				/**
				 * {@inheritDoc}
				 * <p>
				 * Parse benefit functions for each trait. {@code arg} can be a single value or
				 * an array of values with the separator {@value CLOParser#VECTOR_DELIMITER}.
				 * The parser cycles through {@code arg} until all cbenefitst functions are set.
				 * 
				 * @param arg the (array of) benefit function codes
				 */
				@Override
				public boolean parse(String arg) {
					String[] bftf = arg.split(CLOParser.VECTOR_DELIMITER);
					int nbftf = bftf.length;
					if (nbftf < 1) {
						logger.warning("failed to parse benefit function type '" + arg + "' - ignored.");
						return false;
					}
					boolean success = true;
					for (int s = 0; s < nTraits; s++) {
						String type = bftf[s % nbftf];
						Traits2Payoff.Benefits bft = (Traits2Payoff.Benefits) cloBenefitFunction.match(type);
						if (bft == null) {
							logger.warning("benefit function type '" + type + "' unknown - using '"
									+ traits2payoff.getBenefitFunctions()[s].getTitle() + "'");
							success = false;
							continue;
						}
						traits2payoff.setBenefitFunctions(bft, s);
					}
					return success;
				}

				@Override
				public void report(PrintStream output) {
					Traits2Payoff.Benefits[] bfunc = traits2payoff.getBenefitFunctions();
					String msg = "# benefitfunction:      " + bfunc[0];
					for (int n = 1; n < nTraits; n++)
						msg += ":" + bfunc[n];
					output.println(msg);
				}

				@Override
				public String getDescription() {
					switch (nTraits) {
						case 1:
							return "--benefitfcn <s>  benefit function of trait " + traitName[0] + "\n" //
									+ "                benefit functions: <s>\n" + cloBenefitFunction.getDescriptionKey();
						case 2:
							return "--benefitfcn <s0>" + CLOParser.VECTOR_DELIMITER //
									+ "<s1> benefit function of traits\n" //
									+ "             0: " + traitName[0] + "\n" //
									+ "             1: " + traitName[1] + "\n" //
									+ "                benefit functions: <s>\n" + cloBenefitFunction.getDescriptionKey();
						default:
							String descr = "--benefitfcn <s0>" + CLOParser.VECTOR_DELIMITER + "..." //
									+ CLOParser.VECTOR_DELIMITER + "<s" + (nTraits - 1) //
									+ ">  benefit function of traits";
							for (int n = 0; n < nTraits; n++) {
								String aTrait = "              " + n + ": ";
								int traitlen = aTrait.length();
								descr += "\n" + aTrait.substring(traitlen - 16, traitlen) + traitName[n];
							}
							descr += "\n                benefit functions: <s>\n" + cloBenefitFunction.getDescriptionKey();
							return descr;
					}
				}
			});

	/**
	 * Command line option to set the parameters of the benefit function(s) for
	 * continuous traits.
	 * 
	 * @see Traits2Payoff.Benefits
	 */
	public final CLOption cloBenefitParams = new CLOption("benefitparams", "0", EvoLudo.catModel,
			"--benefitparams <b0>,<b1>" + CLOParser.VECTOR_DELIMITER + "..." + CLOParser.VECTOR_DELIMITER //
					+ "<bn> parameters for benefit function" //
					+ (nTraits > 1 ? "\n                different traits separated by '" + CLOParser.MATRIX_DELIMITER + "'"
							: ""),
			new CLODelegate() {

				/**
				 * {@inheritDoc}
				 * <p>
				 * Parse benefit function parameters for each trait. {@code arg} can be a single
				 * value or an array of values. Parameters are separated by
				 * {@value CLOParser#VECTOR_DELIMITER} and traits by
				 * {@value CLOParser#MATRIX_DELIMITER}. The parser cycles through {@code arg}
				 * until all parameters of every trait are set.
				 * 
				 * @param arg the (array of) benefit function parameters
				 */
				@Override
				public boolean parse(String arg) {
					double[][] bparams = CLOParser.parseMatrix(arg);
					int len = bparams.length;
					if (len == 0) {
						logger.warning("failed to parse benefit parameters '" + arg + "' - ignored.");
						return false;
					}
					for (int n = 0; n < nTraits; n++)
						traits2payoff.setBenefitParameters(bparams[n % bparams.length], n);
					return true;
				}

				@Override
				public void report(PrintStream output) {
					double[][] dvec = traits2payoff.getBenefitParameters();
					String msg = "# benefitparams:        " + Formatter.format(dvec[0], 6);
					for (int n = 1; n < nTraits; n++)
						msg += ";" + Formatter.format(dvec[n], 6);
					output.println(msg);
				}
			});

	@Override
	public void collectCLO(CLOParser parser) {
		super.collectCLO(parser);
		parser.addCLO(cloMutationSdev);
		parser.addCLO(cloTraitRange);
		parser.addCLO(cloInit);
		cloCostFunction.addKeys(Traits2Payoff.Costs.values());
		parser.addCLO(cloCostFunction);
		parser.addCLO(cloCostParams);
		cloBenefitFunction.addKeys(Traits2Payoff.Benefits.values());
		parser.addCLO(cloBenefitFunction);
		parser.addCLO(cloBenefitParams);
		// best-response is not an acceptable update rule for continuous strategies -
		// exclude Population.PLAYER_UPDATE_BEST_RESPONSE
		cloPlayerUpdate.removeKey(PlayerUpdateType.BEST_RESPONSE);
	}

	/**
	 * Translate continuous traits into payoffs based on configurable cost and
	 * benefit functions.
	 * 
	 * @author Christoph Hauert
	 */
	public static class Traits2Payoff {

		/**
		 * Selected cost functions to translate continuous traits into payoffs. Enum on
		 * steroids. Currently available cost functions are:
		 * <dl>
		 * <dt>0
		 * <dd>Linear cost function (independent of opponent): \(C(x,y)=c_0\,x\).
		 * <dt>1
		 * <dd>Quadratic cost function (independent of opponent):
		 * \(C(x,y)=c_0\,x+c_1\,x^2\).
		 * <dt>2
		 * <dd>Square root cost function (independent of opponent): \(C(x,y)=c_0
		 * \sqrt{x}\).
		 * <dt>3
		 * <dd>Logarithmic cost function (independent of opponent): \(C(x,y)=c_0
		 * \ln(c_1\,x+1)\).
		 * <dt>4
		 * <dd>Exponential cost function (independent of opponent): \(C(x,y)=c_0
		 * (1-\exp(-c_1\,x))\).
		 * <dt>10
		 * <dd>Linear cost function (sum of focal, \(x\), and opponent, \(y\), traits):
		 * \(C(x,y)=c_0 (x+y)\).
		 * <dt>11
		 * <dd>Quadratic cost function (sum of focal, \(x\), and opponent, \(y\),
		 * traits): \(C(x,y)=c_0 (x+y)+c_1 (x+y)^2\).
		 * <dt>12
		 * <dd>Cubic cost function (sum of focal, \(x\), and opponent, \(y\), traits):
		 * \(C(x,y)=c_0 (x+y)+c_1 (x+y)^2+c_2 (x+y)^3\).
		 * <dt>13
		 * <dd>Quartic cost function (sum of focal, \(x\), and opponent, \(y\), traits):
		 * \(C(x,y)=c_0 (x+y)+c_1 (x+y)^2+c_2 (x+y)^3+c_3 (x+y)^4\).
		 * <dt>20
		 * <dd>Linear cost function (cross terms of focal, \(x\), and opponent, \(y\),
		 * traits): \(C(x,y)=c_0\,x+c_1\,y+c_2\,x\,y\).
		 * </dl>
		 */
		public enum Costs implements CLOption.Key {

			/**
			 * Linear cost function (independent of opponent): \(C(x,y)=c_0\,x\).
			 */
			PAYOFF_COST_ME_LINEAR("0", "C(x,y)=c0*x", 1), //

			/**
			 * Quadratic cost function (independent of opponent):
			 * \(C(x,y)=c_0\,x+c_1\,x^2\).
			 */
			PAYOFF_COST_ME_QUAD("1", "C(x,y)=c0*x+c1*x^2", 2), //

			/**
			 * Square root cost function (independent of opponent): \(C(x,y)=c_0 \sqrt{x}\).
			 */
			PAYOFF_COST_ME_SQRT("2", "C(x,y)=c0*sqrt(x)", 1), //

			/**
			 * Logarithmic cost function (independent of opponent): \(C(x,y)=c_0
			 * \ln(c_1\,x+1)\).
			 */
			PAYOFF_COST_ME_LOG("3", "C(x,y)=c0*ln(c1*x+1)", 2), //

			/**
			 * Exponential cost function (independent of opponent): \(C(x,y)=c_0
			 * (1-\exp(-c_1\,x))\).
			 */
			PAYOFF_COST_ME_EXP("4", "C(x,y)=c0*(1-exp(-c1*x))", 2), //

			/**
			 * Linear cost function (sum of focal, \(x\), and opponent, \(y\), traits):
			 * \(C(x,y)=c_0 (x+y)\).
			 */
			PAYOFF_COST_WE_LINEAR("10", "C(x,y)=c0*(x+y)", 1), //

			/**
			 * Quadratic cost function (sum of focal, \(x\), and opponent, \(y\), traits):
			 * \(C(x,y)=c_0 (x+y)+c_1 (x+y)^2\).
			 */
			PAYOFF_COST_WE_QUAD("11", "C(x,y)=c0*(x+y)+c1*(x+y)^2", 2), //

			/**
			 * Cubic cost function (sum of focal, \(x\), and opponent, \(y\), traits):
			 * \(C(x,y)=c_0 (x+y)+c_1 (x+y)^2+c_2 (x+y)^3\).
			 */
			PAYOFF_COST_WE_QUBIC("12", "C(x,y)=c0*(x+y)+c1*(x+y)^2+c2*(x+y)^3", 3), //

			/**
			 * Quartic cost function (sum of focal, \(x\), and opponent, \(y\), traits):
			 * \(C(x,y)=c_0 (x+y)+c_1 (x+y)^2+c_2 (x+y)^3+c_3 (x+y)^4\).
			 */
			PAYOFF_COST_WE_QUARTIC("13", "C(x,y)=c0*(x+y)+c1*(x+y)^2+c2*(x+y)^3+c3*(x+y)^4", 4), //

			/**
			 * Linear cost function (cross terms of focal, \(x\), and opponent, \(y\),
			 * traits): \(C(x,y)=c_0\,x+c_1\,y+c_2\,x\,y\).
			 */
			PAYOFF_COST_MEYOU_LINEAR("20", "C(x,y)=c0*x+c1*y+c2*x*y", 3);

			/**
			 * The key of the cost function. Used when parsing command line options.
			 * 
			 * @see Continuous#cloCostFunction
			 * @see Continuous#cloCostParams
			 */
			String key;

			/**
			 * The brief description of the cost function for the help display.
			 * 
			 * @see EvoLudo#helpCLO()
			 */
			String title;

			/**
			 * The number of parameters of the cost function.
			 */
			int nParams;

			/**
			 * Create a new type of cost function with key {@code key} and description
			 * {@code title} as well as {@code nParams} parameters.
			 * 
			 * @param key     the identifier for parsing of command line option
			 * @param title   the summary of the cost function
			 * @param nParams the number of parameters
			 */
			Costs(String key, String title, int nParams) {
				this.key = key;
				this.title = title;
				this.nParams = nParams;
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

		/**
		 * The array of cost functions, one for each trait.
		 */
		Costs[] costs;

		/**
		 * The 2D array of cost function parameters. The rows refer to the different
		 * traits and the columns to their cost parameters. {@code ci} is not
		 * necessarily a square array because the each trait may have different numbers
		 * of parameters.
		 * 
		 * @see Costs#nParams
		 */
		double[][] ci;

		/**
		 * Get the array of cost functions, one for each trait.
		 * 
		 * @return the array of cost functions
		 */
		public Costs[] getCostFunctions() {
			return costs;
		}

		/**
		 * Get the cost function for trait {@code index}.
		 * 
		 * @param index the index of the trait
		 * @return the cost function
		 */
		public Costs getCostFunction(int index) {
			return costs[index];
		}

		/**
		 * Set the cost function of the trait {@code index} to {@code costfcn}.
		 * 
		 * @param costfcn the cost function
		 * @param index   the index of the trait
		 */
		public void setCostFunctions(Costs costfcn, int index) {
			// need to explicitly read nTraits; check() has not yet been called
			nTraits = module.getNTraits();
			if (costs == null || costs.length != nTraits)
				costs = new Costs[nTraits];
			costs[index] = costfcn;
			if (ci == null || ci.length != nTraits)
				ci = new double[nTraits][];
			if (ci[index] == null || ci[index].length != costfcn.nParams)
				ci[index] = new double[costfcn.nParams];
		}

		/**
		 * Set the cost parameters for each trait as specified in the 2D array
		 * {@code cparams}.
		 * 
		 * @param cparams the 2D array of cost parameters
		 * 
		 * @see #ci
		 */
		public void setCostParameters(double[][] cparams) {
			for (int n = 0; n < nTraits; n++)
				setCostParameters(cparams[n], n);
		}

		/**
		 * Set the cost parameters of the trait {@code index} to the array
		 * {@code cparams}.
		 * 
		 * @param cparams the array of cost function parameters
		 * @param index   the index of the trait
		 */
		public void setCostParameters(double[] cparams, int index) {
			double[] cin = ci[index];
			for (int i = 0; i < cin.length; i++)
				cin[i] = cparams[i % cparams.length];
		}

		/**
		 * Set the cost parameters for single trait modules to the array
		 * {@code cparams}.
		 * 
		 * @param cparams the array of cost function parameters
		 */
		public void setCostParameters(double[] cparams) {
			setCostParameters(cparams, 0);
		}

		/**
		 * Get the 2D array of cost function parameters.
		 * 
		 * @return the 2D array of cost function parameters
		 * 
		 * @see #ci
		 */
		public double[][] getCostParameters() {
			return ci;
		}

		/**
		 * Selected benefit functions to translate continuous traits into payoffs. Enum
		 * on steroids. Currently available benefit functions are:
		 * <dl>
		 * <dt>0
		 * <dd>Linear benefit function (independent of focal): \(B(x,y)=b_0\,y\).
		 * <dt>1
		 * <dd>Quadratic benefit function (independent of focal):
		 * \(B(x,y)=b_0\,y+\b_1\,y^2\).
		 * <dt>2
		 * <dd>Saturating benefit function following a square root (independent of
		 * focal): \(B(x,y)=b_0\sqrt{y}\).
		 * <dt>3
		 * <dd>Saturating benefit function following a logarithm (independent of focal):
		 * \(B(x,y)=b_0\log{b_1\,y+1}\).
		 * <dt>4
		 * <dd>Saturating benefit function following an exponential (independent of
		 * focal): \(B(x,y)=b_0 \left(1-e^{-b_1\,y}\right)\).
		 * <dt>10
		 * <dd>Linear benefit function (sum of focal, \(x\), and opponent, \(y\),
		 * traits): \(B(x,y)=b_0\,(x+y)\).
		 * <dt>11
		 * <dd>Quadratic benefit function (sum of focal, \(x\), and opponent, \(y\),
		 * traits): \(B(x,y)=b_0\,(x+y)+\b_1\,(x+y)^2\).
		 * <dt>12
		 * <dd>Saturating benefit function following a square root (sum of focal, \(x\),
		 * and opponent, \(y\), traits): \(B(x,y)=b_0\sqrt{x+y}\).
		 * <dt>13
		 * <dd>Saturating benefit function following a logarithm (sum of focal, \(x\),
		 * and opponent, \(y\), traits): \(B(x,y)=b_0\log{b_1\,(x+y)+1}\).
		 * <dt>14
		 * <dd>Saturating benefit function following an exponential (sum of focal,
		 * \(x\), and opponent, \(y\), traits): \(B(x,y)=b_0
		 * \left(1-e^{-b_1\,(x+y)}\right)\).
		 * <dt>20
		 * <dd>Linear benefit function (with interaction term):
		 * \(B(x,y)=b_0\,x=b_1\,y+\b_2\,x\,y\).
		 * <dt>30
		 * <dd>Linear benefit function (independent of opponent): \(B(x,y)=b_0\,x\).
		 * <dt>31
		 * <dd>Quadratic benefit function (independent of opponent):
		 * \(B(x,y)=b_0\,x+b_1\,x^2\).
		 * <dt>32
		 * <dd>Cubic benefit function (independent of opponent):
		 * \(B(x,y)=b_0\,x+b_1\,x^2+b_2\,x^3\).
		 * </dl>
		 */
		public enum Benefits implements CLOption.Key {

			/**
			 * Linear benefit function (independent of focal): \(B(x,y)=b_0\,y\).
			 */
			PAYOFF_BENEFIT_YOU_LINEAR("0", "B(x,y)=b0*y", 1), //

			/**
			 * Quadratic benefit function (independent of focal):
			 * \(B(x,y)=b_0\,y+\b_1\,y^2\).
			 */
			PAYOFF_BENEFIT_YOU_QUADR("1", "B(x,y)=b0*y+b1*y^2", 2), //

			/**
			 * Saturating benefit function following a square root (independent of focal):
			 * \(B(x,y)=b_0\sqrt{y}\).
			 */
			PAYOFF_BENEFIT_YOU_SQRT("2", "B(x,y)=b0*sqrt(y)", 1), //

			/**
			 * Saturating benefit function following a logarithm (independent of focal):
			 * \(B(x,y)=b_0\log{b_1\,y+1}\).
			 */
			PAYOFF_BENEFIT_YOU_LOG("3", "B(x,y)=b0*ln(b1*y+1)", 2), //

			/**
			 * Saturating benefit function following an exponential (independent of focal):
			 * \(B(x,y)=b_0 \left(1-e^{-b_1\,y}\right)\).
			 */
			PAYOFF_BENEFIT_YOU_EXP("4", "B(x,y)=b0*(1-exp(-b1*y))", 2), //

			/**
			 * Linear benefit function (sum of focal, \(x\), and opponent, \(y\), traits):
			 * \(B(x,y)=b_0\,(x+y)\).
			 */
			PAYOFF_BENEFIT_WE_LINEAR("10", "B(x,y)=b0*(x+y)", 1), //

			/**
			 * Quadratic benefit function (sum of focal, \(x\), and opponent, \(y\),
			 * traits): \(B(x,y)=b_0\,(x+y)+\b_1\,(x+y)^2\).
			 */
			PAYOFF_BENEFIT_WE_QUAD("11", "B(x,y)=b0*(x+y)+b1*(x+y)^2", 2), // default

			/**
			 * Saturating benefit function following a square root (sum of focal, \(x\), and
			 * opponent, \(y\), traits): \(B(x,y)=b_0\sqrt{x+y}\).
			 */
			PAYOFF_BENEFIT_WE_SQRT("12", "B(x,y)=b0*sqrt(x+y)", 1), //

			/**
			 * Saturating benefit function following a logarithm (sum of focal, \(x\), and
			 * opponent, \(y\), traits): \(B(x,y)=b_0\log{b_1\,(x+y)+1}\).
			 */
			PAYOFF_BENEFIT_WE_LOG("13", "B(x,y)=b0*ln(b1*(x+y)+1)", 2), //

			/**
			 * Saturating benefit function following an exponential (sum of focal, \(x\),
			 * and opponent, \(y\), traits): \(B(x,y)=b_0 \left(1-e^{-b_1\,(x+y)}\right)\).
			 */
			PAYOFF_BENEFIT_WE_EXP("14", "B(x,y)=b0*(1-exp(-b1*(x+y)))", 2), //

			/**
			 * Linear benefit function (with interaction term):
			 * \(B(x,y)=b_0\,x=b_1\,y+\b_2\,x\,y\).
			 */
			PAYOFF_BENEFIT_MEYOU_LINEAR("20", "B(x,y)=b0*x+b1*y+b2*x*y", 3), //

			/**
			 * Linear benefit function (independent of opponent): \(B(x,y)=b_0\,x\).
			 */
			PAYOFF_BENEFIT_ME_LINEAR("30", "B(x,y)=b0*x", 1), //

			/**
			 * Quadratic benefit function (independent of opponent):
			 * \(B(x,y)=b_0\,x+b_1\,x^2\).
			 */
			PAYOFF_BENEFIT_ME_QUADR("31", "B(x,y)=b0*x+b1*x^2", 2), //

			/**
			 * Cubic benefit function (independent of opponent):
			 * \(B(x,y)=b_0\,x+b_1\,x^2+b_2\,x^3\).
			 */
			PAYOFF_BENEFIT_ME_QUBIC("32", "B(x,y)=b0*x+b1*x^2+b2*x^3", 3);

			/**
			 * The key of the benefit function. Used when parsing command line options.
			 * 
			 * @see Continuous#cloBenefitFunction
			 * @see Continuous#cloBenefitParams
			 */
			String key;

			/**
			 * The brief description of the benefit function for the help display.
			 * 
			 * @see EvoLudo#helpCLO()
			 */
			String title;

			/**
			 * The number of parameters of the benefit function.
			 */
			int nParams;

			/**
			 * Create a new type of benefit function with key {@code key} and description
			 * {@code title} as well as {@code nParams} parameters.
			 * 
			 * @param key     the identifier for parsing of command line option
			 * @param title   the summary of the benefit function
			 * @param nParams the number of parameters
			 */
			Benefits(String key, String title, int nParams) {
				this.key = key;
				this.title = title;
				this.nParams = nParams;
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

		/**
		 * The array of benefit functions, one for each trait.
		 */
		Benefits[] benefits;

		/**
		 * The 2D array of cost function parameters. The rows refer to the different
		 * traits and the columns to their cost parameters. {@code bi} is not
		 * necessarily a square array because the each trait may have different numbers
		 * of parameters.
		 * 
		 * @see Benefits#nParams
		 */
		double[][] bi;

		/**
		 * Get the array of benefit functions for each trait.
		 * 
		 * @return the array of benefit functions
		 */
		public Benefits[] getBenefitFunctions() {
			return benefits;
		}

		/**
		 * Get the benefit function for trait {@code index}.
		 * 
		 * @param index the index of the trait
		 * @return the benefit function
		 */
		public Benefits getBenefitFunction(int index) {
			return benefits[index];
		}

		/**
		 * Set the benefit function of the trait {@code index} to {@code benefitfcn}.
		 * 
		 * @param benefitfcn the benefit function
		 * @param index      the index of the trait
		 */
		public void setBenefitFunctions(Benefits benefitfcn, int index) {
			// need to explicitly read nTraits; check() has not yet been called
			nTraits = module.getNTraits();
			if (benefits == null || benefits.length != nTraits)
				benefits = new Benefits[nTraits];
			benefits[index] = benefitfcn;
			if (bi == null || bi.length != nTraits)
				bi = new double[nTraits][];
			if (bi[index] == null || bi[index].length != benefitfcn.nParams)
				bi[index] = new double[benefitfcn.nParams];
		}

		/**
		 * Set the benefit parameters for each trait as specified in the 2D array
		 * {@code bparams}. The rows refer to the different traits and the columns to
		 * their benefit parameters. {@code bparams} is not necessarily a square array
		 * because the each trait may have different numbers of parameters.
		 * 
		 * @param bparams the 2D array of benefit parameters
		 */
		public void setBenefitParameters(double[][] bparams) {
			for (int n = 0; n < nTraits; n++)
				setBenefitParameters(bparams[n], n);
		}

		/**
		 * Set the benefit parameters of the trait {@code index} to the array
		 * {@code bparams}.
		 * 
		 * @param bparams the array of benefit function parameters
		 * @param index   the index of the trait
		 */
		public void setBenefitParameters(double[] bparams, int index) {
			for (int n = 0; n < nTraits; n++) {
				double[] bin = bi[n];
				for (int i = 0; i < bin.length; i++)
					bin[i] = bparams[i % bparams.length];
			}
		}

		/**
		 * Set the benefit parameters for single trait modules to the array
		 * {@code bparams}.
		 * 
		 * @param bparams the array of benefit function parameters
		 */
		public void setBenefitParameters(double[] bparams) {
			setBenefitParameters(bparams, 0);
		}

		/**
		 * Get the 2D array of benefit function parameters. The rows refer to the
		 * different
		 * traits and the columns to their benefit parameters. The array is not
		 * necessarily
		 * square because the each trait may have different numbers of parameters.
		 * 
		 * @return the 2D array of benefit function parameters
		 */
		public double[][] getBenefitParameters() {
			return bi;
		}

		/**
		 * Reference to the backing module.
		 */
		Continuous module;

		/**
		 * Helper variable: the number of traits in {@code module}.
		 */
		int nTraits;

		/**
		 * Helper variable: the array of trait minima in {@code module}.
		 */
		double[] traitMin;

		/**
		 * Helper variable: the array of trait maxima in {@code module}.
		 */
		double[] traitMax;

		/**
		 * Create a new trait-to-payoff mapping for the backing module {@code module}.
		 * 
		 * @param module the backing module
		 */
		public Traits2Payoff(Continuous module) {
			this.module = module;
		}

		/**
		 * Checks the trait-to-payoff mapping.
		 * 
		 * @return {@code true} if reset of module required
		 */
		public boolean check() {
			nTraits = module.getNTraits();
			traitMin = module.getTraitMin();
			traitMax = module.getTraitMax();
			return false;
		}

		/**
		 * Calculate the payoff to the focal individual with traits {@code me} when
		 * interacting with an opponent with traits {@code you}.
		 * 
		 * @param me  the array of traits of the focal individual
		 * @param you the array of traits of the opponent individual
		 * @return the payoff to {@code me}
		 */
		public double payoff(double[] me, double[] you) {
			// assumes that benefits and costs can be decomposed into the different traits
			return benefits(me, you) - costs(me, you);
		}

		/**
		 * Calculate the payoff to the focal individual with trait {@code me} when
		 * interacting with an opponent with trait {@code you}.
		 *
		 * @param me  the trait of the focal individual
		 * @param you the trait of the opponent individual
		 * @return the payoff to {@code me}
		 */
		public double payoff(double me, double you) {
			// assumes that benefits and costs can be decomposed into the different traits
			return benefits(me, you, 0) - costs(me, you, 0);
		}

		/**
		 * Calculate the costs to the focal individual with traits {@code me} when
		 * interacting with an opponent with traits {@code you}.
		 * 
		 * @param me  the array of traits of the focal individual
		 * @param you the array of traits of the opponent individual
		 * @return the costs to {@code me}
		 */
		protected double costs(double[] me, double[] you) {
			double totcosts = 0.0;
			for (int n = 0; n < nTraits; n++)
				totcosts += costs(me[n], you[n], n);
			return totcosts;
		}

		/**
		 * Calculate the costs to the focal individual with trait value {@code me} in
		 * trait with index {@code trait} when interacting with an opponent with trait
		 * value {@code you}.
		 * 
		 * @param me    the trait value of the focal individual
		 * @param you   the trait value of the opponent individual
		 * @param trait the index of the trait
		 * @return the costs to {@code me}
		 */
		protected double costs(double me, double you, int trait) {
			double shift = traitMin[trait];
			double scale = traitMax[trait] - shift;
			double myinv = me * scale + shift;
			double yourinv = you * scale + shift;
			double ourinv = myinv + yourinv;
			double[] c = ci[trait];

			switch (costs[trait]) {
				case PAYOFF_COST_ME_LINEAR:
					return c[0] * myinv;
				case PAYOFF_COST_ME_QUAD: // default
					return myinv * (c[1] * myinv + c[0]);
				case PAYOFF_COST_ME_SQRT:
					return c[0] * Math.sqrt(myinv);
				case PAYOFF_COST_ME_LOG:
					return c[0] * Math.log(c[1] * myinv + 1.0);
				case PAYOFF_COST_ME_EXP:
					return c[0] * (1.0 - Math.exp(-c[1] * myinv));

				case PAYOFF_COST_WE_LINEAR:
					return c[0] * ourinv;
				case PAYOFF_COST_WE_QUAD:
					return (c[1] * ourinv + c[0]) * ourinv;
				case PAYOFF_COST_WE_QUBIC:
					return ((c[2] * ourinv + c[1]) * ourinv + c[0]) * ourinv;
				case PAYOFF_COST_WE_QUARTIC:
					return (((c[3] * ourinv + c[2]) * ourinv + c[1]) * ourinv + c[0]) * ourinv;

				case PAYOFF_COST_MEYOU_LINEAR:
					return c[0] * myinv + c[1] * yourinv + c[2] * myinv * yourinv;

				default: // this is bad
					throw new Error("Unknown cost function type (" + costs[trait] + ")");
			}
		}

		/**
		 * Calculate the benefits to the focal individual with traits {@code me} when
		 * interacting with an opponent with traits {@code you}.
		 * 
		 * @param me  the array of traits of the focal individual
		 * @param you the array of traits of the opponent individual
		 * @return the benefits to {@code me}
		 */
		protected double benefits(double[] me, double[] you) {
			double totbenefits = 0.0;
			for (int n = 0; n < nTraits; n++)
				totbenefits += benefits(me[n], you[n], n);
			return totbenefits;
		}

		/**
		 * Calculate the benefits to the focal individual with trait value {@code me} in
		 * trait with index {@code trait} when interacting with an opponent with trait
		 * value {@code you}.
		 * 
		 * @param me    the trait value of the focal individual
		 * @param you   the trait value of the opponent individual
		 * @param trait the index of the trait
		 * @return the benefits to {@code me}
		 */
		protected double benefits(double me, double you, int trait) {
			double shift = traitMin[trait];
			double scale = traitMax[trait] - shift;
			double myinv = me * scale + shift;
			double yourinv = you * scale + shift;
			double ourinv = myinv + yourinv;
			double[] b = bi[trait];

			switch (benefits[trait]) {
				// benefit depending solely on the 'me' investment
				case PAYOFF_BENEFIT_ME_LINEAR:
					return b[0] * myinv;
				case PAYOFF_BENEFIT_ME_QUADR:
					return (b[1] * myinv + b[0]) * myinv;
				case PAYOFF_BENEFIT_ME_QUBIC:
					return ((b[2] * myinv + b[1]) * myinv + b[0]) * myinv;

				// benefit depending solely on the 'you' investment
				case PAYOFF_BENEFIT_YOU_LINEAR:
					return b[0] * yourinv;
				case PAYOFF_BENEFIT_YOU_QUADR:
					return (b[1] * yourinv + b[0]) * yourinv;
				case PAYOFF_BENEFIT_YOU_SQRT:
					return b[0] * Math.sqrt(yourinv);
				case PAYOFF_BENEFIT_YOU_LOG:
					return b[0] * Math.log(b[1] * yourinv + 1.0);
				case PAYOFF_BENEFIT_YOU_EXP:
					return b[0] * (1.0 - Math.exp(-b[1] * yourinv));

				// benefit depending on the sum of 'me' and 'you' investments
				case PAYOFF_BENEFIT_WE_LINEAR: // was 2
					return b[0] * ourinv;
				case PAYOFF_BENEFIT_WE_QUAD: // default
					return (b[1] * ourinv + b[0]) * ourinv;
				case PAYOFF_BENEFIT_WE_SQRT:
					return b[0] * Math.sqrt(ourinv);
				case PAYOFF_BENEFIT_WE_LOG:
					return b[0] * Math.log(b[1] * ourinv + 1.0);
				case PAYOFF_BENEFIT_WE_EXP:
					return b[0] * (1.0 - Math.exp(-b[1] * ourinv));

				// benefit depending on 'me' and 'you' investments individually
				case PAYOFF_BENEFIT_MEYOU_LINEAR:
					return b[0] * myinv + b[1] * yourinv + b[2] * myinv * yourinv;

				default: // this is bad
					throw new Error("Unknown benefit function type (" + benefits[trait] + ")");
			}
		}
	}

	/**
	 * The absolute minimum score.
	 */
	protected double cxMinScore = Double.MAX_VALUE;

	/**
	 * The absolute maximum score.
	 */
	protected double cxMaxScore = -Double.MAX_VALUE;

	/**
	 * The minimum score in a monomorphic population.
	 */
	protected double cxMinMonoScore = Double.MAX_VALUE;

	/**
	 * The maximum score in a monomorphic population.
	 */
	protected double cxMaxMonoScore = -Double.MAX_VALUE;

	/**
	 * Helper method to numerically determine the minimum and maximum scores in the
	 * game through a brute force hill climbing algorithm for two competing traits
	 * as well as monomorphic populations.
	 */
	protected void setExtremalScores() {
		cxMinScore = findExtremalScore(false);
		cxMaxScore = findExtremalScore(true);
		cxMinMonoScore = findExtremalMonoScore(false);
		cxMaxMonoScore = findExtremalMonoScore(true);
		extremalScoresSet = true;
	}

	/**
	 * The linear grid size to sample payoffs in the (possibly multi-dimensional)
	 * trait space.
	 */
	static final int MINMAX_STEPS = 10;

	/**
	 * The number of iterations for the hill climbing process.
	 */
	static final int MINMAX_ITER = 5;

	/**
	 * Helper method to determine the minimum or maximum payoff.
	 * 
	 * <h3>Implementation notes:</h3>
	 * Repeatedly calls
	 * {@link #findExtrema(double[], double[], int[], int[], double[][], double[][], double[], double[], int[], int[], double, int, double)}
	 * with the most promising interval in each trait for residents and mutants,
	 * respectively. The hill climbing process stops after {@code #MINMAX_ITER}
	 * iterations.
	 * 
	 * @param maximum if {@code true} the maximum is returned and the minimum
	 *                otherwise
	 * @return the minimum or maximum payoff
	 */
	private double findExtremalScore(boolean maximum) {
		double[][] resInterval = new double[nTraits][2];
		double[][] mutInterval = new double[nTraits][2];
		double[] resScale = new double[nTraits];
		double[] mutScale = new double[nTraits];
		double[] resTrait = new double[nTraits];
		double[] mutTrait = new double[nTraits];
		int[] resIdx = new int[nTraits];
		int[] mutIdx = new int[nTraits];
		int[] resMax = new int[nTraits];
		int[] mutMax = new int[nTraits];
		double minmax = maximum ? 1.0 : -1.0;
		double scoreMax = -Double.MAX_VALUE;

		// initialize trait intervals
		for (int n = 0; n < nTraits; n++) {
			resInterval[n][0] = 0;
			resInterval[n][1] = 1;
			mutInterval[n][0] = 0;
			mutInterval[n][1] = 1;
		}

		for (int i = 0; i < MINMAX_ITER; i++) {
			for (int n = 0; n < nTraits; n++) {
				resScale[n] = (resInterval[n][1] - resInterval[n][0]) / MINMAX_STEPS;
				mutScale[n] = (mutInterval[n][1] - mutInterval[n][0]) / MINMAX_STEPS;
			}
			Arrays.fill(resMax, -1);
			Arrays.fill(mutMax, -1);
			scoreMax = Math.max(scoreMax, findExtrema(resTrait, mutTrait, resIdx, mutIdx, resInterval, mutInterval,
					resScale, mutScale, resMax, mutMax, -Double.MAX_VALUE, nTraits - 1, minmax));
			// determine new intervals and scales
			for (int n = 0; n < nTraits; n++) {
				switch (resIdx[n]) {
					case 0:
						resInterval[n][1] = resInterval[n][0] + resScale[n];
						break;
					case MINMAX_STEPS:
						resInterval[n][0] += (MINMAX_STEPS - 1) * resScale[n];
						break;
					default:
						resInterval[n][0] += (resIdx[n] - 1) * resScale[n];
						resInterval[n][1] = resInterval[n][0] + 2.0 * resScale[n];
						break;
				}
				switch (mutIdx[n]) {
					case 0:
						mutInterval[n][1] = mutInterval[n][0] + mutScale[n];
						break;
					case MINMAX_STEPS:
						mutInterval[n][0] += (MINMAX_STEPS - 1) * mutScale[n];
						break;
					default:
						mutInterval[n][0] += (mutIdx[n] - 1) * mutScale[n];
						mutInterval[n][1] = mutInterval[n][0] + 2.0 * mutScale[n];
						break;
				}
			}
		}
		return minmax * scoreMax;
	}

	/**
	 * Helper method to find the minimum or maximum payoff.
	 * 
	 * <h3>Implementation notes:</h3>
	 * The function returns the maximum payoff after discretizing each trait of both
	 * the resident and the mutant into {@code MINMAX_STEPS} intervals. The function
	 * is recursively called for each trait. The indices of the cell (discretized
	 * trait interval) that yields the maximum payoff are returned in the arrays
	 * {@code resMax} and {@code mutMax} for residents and mutants respectively.
	 * This provides the starting point for the next iteration of the hill climber
	 * process.
	 * 
	 * @param resTrait    the array of resident traits (helper variable for
	 *                    recursive calculations)
	 * @param mutTrait    the array of mutant traits (helper variable for recursive
	 *                    calculations)
	 * @param resIdx      the index of the resident trait (helper variable for
	 *                    recursive calculations)
	 * @param mutIdx      the index of the mutant trait (helper variable for
	 *                    recursive calculations)
	 * @param resInterval the resident trait intervals for discretization
	 * @param mutInterval the mutant trait intervals for discretization
	 * @param resScale    the scaling of the width of the resident trait interval
	 * @param mutScale    the scaling of the width of the mutant trait interval
	 * @param resMax      the indices of the discretized cell for the resident that
	 *                    yielded the highest payoff
	 * @param mutMax      the indices of the discretized cell for the mutant that
	 *                    yielded the highest payoff
	 * @param scoreMax    the maximum payoff
	 * @param trait       the current trait for the recursion (helper variable for
	 *                    recursive calculations)
	 * @param minmax      {@code 1.0} to calculate maximum and {@code -1.0} to
	 *                    calculate minimum
	 * @return the minimum or maximum score
	 */
	private double findExtrema(double[] resTrait, double[] mutTrait, int[] resIdx, int[] mutIdx, double[][] resInterval,
			double[][] mutInterval, double[] resScale, double[] mutScale, int[] resMax, int[] mutMax, double scoreMax,
			int trait, double minmax) {

		for (int r = 0; r <= MINMAX_STEPS; r++) {
			resIdx[trait] = r;
			resTrait[trait] = resInterval[trait][0] + r * resScale[trait];
			for (int m = 0; m <= MINMAX_STEPS; m++) {
				mutIdx[trait] = m;
				mutTrait[trait] = mutInterval[trait][0] + m * mutScale[trait];
				if (trait > 0) {
					scoreMax = Math.max(scoreMax, findExtrema(resTrait, mutTrait, resIdx, mutIdx, resInterval,
							mutInterval, resScale, mutScale, resMax, mutMax, scoreMax, trait - 1, minmax));
					continue;
				}
				double traitScore = minmax * traits2payoff.payoff(resTrait, mutTrait);
				if (traitScore > scoreMax) {
					scoreMax = traitScore;
					int len = resIdx.length; // nTraits
					System.arraycopy(resIdx, 0, resMax, 0, len);
					System.arraycopy(mutIdx, 0, mutMax, 0, len);
				}
			}
		}
		return scoreMax;
	}

	/**
	 * Helper method to find the minimum or maximum payoff for <em>monomorphic</em>
	 * populations.
	 * 
	 * <h3>Implementation notes:</h3>
	 * This is essentially the same but simplified hill climber process in
	 * {@link #findExtremalScore(boolean)} under the constraint that the population
	 * is monomorphic, i.e. that the resident and mutant traits are identical.
	 * 
	 * @param maximum if {@code true} the maximum is returned and the minimum
	 *                otherwise
	 * @return the minimum or maximum monomorphic score
	 */
	private double findExtremalMonoScore(boolean maximum) {
		double[][] resInterval = new double[nTraits][2];
		double[] resScale = new double[nTraits];
		double[] resTrait = new double[nTraits];
		int[] resIdx = new int[nTraits];
		int[] resMax = new int[nTraits];
		double minmax = maximum ? 1.0 : -1.0;
		double scoreMax = -Double.MAX_VALUE;

		// initialize trait intervals
		for (int n = 0; n < nTraits; n++) {
			resInterval[n][0] = 0;
			resInterval[n][1] = 1;
		}

		for (int i = 0; i < MINMAX_ITER; i++) {
			for (int n = 0; n < nTraits; n++) {
				resScale[n] = (resInterval[n][1] - resInterval[n][0]) / MINMAX_STEPS;
			}
			Arrays.fill(resMax, -1);
			scoreMax = Math.max(scoreMax, findExtrema(resTrait, resIdx, resInterval, resScale, resMax,
					-Double.MAX_VALUE, nTraits - 1, minmax));
			// determine new intervals and scales
			for (int n = 0; n < nTraits; n++) {
				switch (resIdx[n]) {
					case 0:
						resInterval[n][1] = resInterval[n][0] + resScale[n];
						break;
					case MINMAX_STEPS:
						resInterval[n][0] += (MINMAX_STEPS - 1) * resScale[n];
						break;
					default:
						resInterval[n][0] += (resIdx[n] - 1) * resScale[n];
						resInterval[n][1] = resInterval[n][0] + 2.0 * resScale[n];
						break;
				}
			}
		}
		return minmax * scoreMax;
	}

	/**
	 * Helper method to find the minimum or maximum payoff for <em>monomorphic</em>
	 * populations.
	 * 
	 * <h3>Implementation notes:</h3>
	 * This is essentially the same but simplified hill climber process in
	 * {@link #findExtrema(double[], double[], int[], int[], double[][], double[][], double[], double[], int[], int[], double, int, double)}
	 * under the constraint that the population is monomorphic, i.e. that the
	 * resident and mutant traits are identical.
	 * 
	 * @param resTrait    the array of resident traits (helper variable for
	 *                    recursive calculations)
	 * @param resIdx      the index of the resident trait (helper variable for
	 *                    recursive calculations)
	 * @param resInterval the resident trait intervals for discretization
	 * @param resScale    the scaling of the width of the resident trait interval
	 * @param resMax      the indices of the discretized cell for the resident that
	 *                    yielded the highest payoff
	 * @param scoreMax    the maximum payoff
	 * @param trait       the current trait for the recursion (helper variable for
	 *                    recursive calculations)
	 * @param minmax      {@code 1.0} to calculate maximum and {@code -1.0} to
	 *                    calculate minimum
	 * @return the minimum or maximum monomorphic score
	 */
	private double findExtrema(double[] resTrait, int[] resIdx, double[][] resInterval, double[] resScale, int[] resMax,
			double scoreMax, int trait, double minmax) {

		for (int r = 0; r <= MINMAX_STEPS; r++) {
			resIdx[trait] = r;
			resTrait[trait] = resInterval[trait][0] + r * resScale[trait];
			if (trait > 0) {
				scoreMax = Math.max(scoreMax,
						findExtrema(resTrait, resIdx, resInterval, resScale, resMax, scoreMax, trait - 1, minmax));
				continue;
			}
			double traitScore = minmax * traits2payoff.payoff(resTrait, resTrait);
			if (traitScore > scoreMax) {
				scoreMax = traitScore;
				int len = resIdx.length; // nTraits
				System.arraycopy(resIdx, 0, resMax, 0, len);
			}
		}
		return scoreMax;
	}
}
