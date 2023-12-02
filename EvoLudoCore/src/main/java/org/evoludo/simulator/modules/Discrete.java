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
import org.evoludo.simulator.models.Model;
import org.evoludo.simulator.models.ODEEuler;
import org.evoludo.util.CLOParser;
import org.evoludo.util.CLOption;
import org.evoludo.util.CLOption.CLODelegate;
import org.evoludo.util.Formatter;

/**
 * Parent class of all EvoLudo modules with discrete strategy sets.
 * 
 * @author Christoph Hauert
 */
public abstract class Discrete extends Module {

	/**
	 * All modules that admit interactions in pairs (as opposed to larger groups)
	 * should implement this interface. The classical {@code 2×2} games are an
	 * example, see {@link org.evoludo.simulator.modules.TBT}.
	 * 
	 * @author Christoph Hauert
	 */
	public interface Pairs extends Features {

		@Override
		public default boolean isPairwise() {
			return true;
		}

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
		 * @param me     the trait index of the focal individual
		 * @param tCount number of opponents with each trait/strategy
		 * @param tScore array for returning the scores of each opponent trait/strategy
		 * @return score of focal individual {@code me} accumulated over all
		 *         interactions
		 */
		public double pairScores(int me, int[] tCount, double[] tScore);

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
		 * @param count       number of individuals for each trait/strategy
		 * @param traitScores array for returning the payoffs/scores of each
		 *                    trait/strategy
		 */
		public void mixedScores(int[] count, double[] traitScores);
	}

	/**
	 * All modules that admit interactions in larger groups (as opposed to
	 * interactions in pairs) should implement this interface. The voluntary public
	 * goods game is an example, see {@link org.evoludo.simulator.modules.CDL}.
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
			return getNGroup() == 2;
		}

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
		 * @param tCount group composition given by the number of individuals with each
		 *               trait/strategy
		 * @param tScore array for returning the payoffs/scores of each trait/strategy
		 */
		public void groupScores(int[] tCount, double[] tScore);

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
		 * @param count       number of individuals for each trait/strategy
		 * @param n           interaction group size
		 * @param traitScores array for returning the payoffs/scores of each
		 *                    trait/strategy
		 */
		public void mixedScores(int[] count, int n, double[] traitScores);

		@Override
		public default void mixedScores(int[] count, double[] traitScores) {
			mixedScores(count, 2, traitScores);
		}
	}

	/**
	 * The list {@code species} contains references to each species in this
	 * module. It deliberately shadows {@link Module#species} to simplify
	 * bookkeeping. During instantiation {@link Module#species} and
	 * {@code species} are linked to represent one and the same list.
	 * 
	 * @see #Discrete(EvoLudo, Discrete)
	 * @see Module#species
	 */
	@SuppressWarnings("hiding")
	ArrayList<Discrete> species;

	/**
	 * Create new module with a discrete set of strategies.
	 * 
	 * @param engine the pacemeaker for running the model
	 */
	protected Discrete(EvoLudo engine) {
		this(engine, null);
	}

	/**
	 * Create another module with a discrete set of strategies. The additional
	 * module represents another species in multi-species modules that interact with
	 * species {@code partner}.
	 * 
	 * @param partner the partner species
	 */
	protected Discrete(Discrete partner) {
		this(partner.engine, partner);
	}

	/**
	 * Create a new module with a discrete set of strategies with pacemaker
	 * {@code engine} and interactions with module {@code partner}. If
	 * {@code partner == null} this is a single species module and interactions
	 * within species ({@code opponent == this} holds).
	 * 
	 * @param engine  the pacemeaker for running the model
	 * @param partner the partner species
	 */
	protected Discrete(EvoLudo engine, Discrete partner) {
		super(engine, partner);
		if (partner == null) {
			species = new ArrayList<Discrete>();
			// recall this.modules shadows super.modules for later convenience
			super.species = species;
		} else {
			// link ArrayList<Discrete> shadows
			species = partner.species;
		}
		add(this);
	}

	/**
	 * Add {@code dpop} to list of species. Duplicate entries are ignored.
	 * Allocate new list if necessary. Assign generic name to species if none
	 * provided.
	 *
	 * @param dpop the module to add to species list.
	 * @return {@code true} if {@code dpop} successfully added;
	 *         {@code false} adding failed or already included in list.
	 */
	public boolean add(Discrete dpop) {
		// do not add duplicates
		if (species.contains(dpop))
			return false;
		if (!species.add(dpop))
			return false;
		switch (species.size()) {
			case 1:
				break;
			case 2:
				// start naming species (if needed)
				for (Discrete pop : species) {
					if (pop.getName().length() < 1)
						pop.setName("Species-" + pop.ID);
				}
				break;
			default:
				if (dpop.getName().length() < 1)
					dpop.setName("Species-" + dpop.ID);
		}
		return true;
	}

	/**
	 * Calculate and return the payoff/score of individuals in monomorphic
	 * populations with trait/strategy {@code type}.
	 * 
	 * @param type trait/strategy
	 * @return payoff/score in monomorphic population with trait/strategy
	 *         {@code type}
	 */
	public abstract double getMonoGameScore(int type);

	/**
	 * Calculate and return the payoff/score of individuals in monomorphic
	 * populations with trait/strategy {@code type} (see
	 * {@link #getMonoGameScore(int)}) but also deals with payoff accounting
	 * (averaged versus accumulated).
	 *
	 * @param type trait/strategy
	 * @return payoff/score in monomorphic population with trait/strategy
	 *         {@code type}.
	 *         Returns {@code NaN} if scores ill defined (potentially unbounded).
	 */
	public double getMonoScore(int type) {
		return model.processMonoScore(ID, getMonoGameScore(type));
	}

	@Override
	public double getMinMonoGameScore() {
		double min = Double.MAX_VALUE;
		for (int n = 0; n < nTraits; n++) {
			double monon = getMonoGameScore(n);
			if (monon < min)
				min = monon;
		}
		return min;
	}

	@Override
	public double getMaxMonoGameScore() {
		double max = -Double.MAX_VALUE;
		for (int n = 0; n < nTraits; n++) {
			double monon = getMonoGameScore(n);
			if (monon > max)
				max = monon;
		}
		return max;
	}

	/**
	 * The flag to indicate whether models should stop once a monomorphic
	 * state has been reached.
	 */
	protected boolean monoStop = false;

	/**
	 * Set whether models should stop once a monomorphic state has been reached.
	 * 
	 * @param monoStop the flag to indicate whether to stop
	 */
	public void setMonoStop(boolean monoStop) {
		this.monoStop = monoStop;
	}

	/**
	 * Get the flag which indicates whether models stop once a monomorphic state has
	 * been reached.
	 * 
	 * @return {@code true} if models stop when reaching homogeneous states.
	 */
	public boolean getMonoStop() {
		return monoStop;
	}

	/**
	 * Initial frequencies or densities for each strategy/trait.
	 */
	protected double[] init;

	/**
	 * Set initial frequencies or densities of traits to {@code init}. If argument
	 * is {@code null} in frequency based models uniform frequencies are set.
	 * Allocates memory as necessary.
	 * 
	 * @param init the initial frequencies or densities of strategies/traits
	 */
	//TODO more testing required for multiple species
	@SuppressWarnings("null")
	public void setInit(double[] init) {
		boolean isDensity = (model instanceof Model.DE && ((Model.DE) model).isDensity());
		if (this.init == null || this.init.length != nTraits)
			this.init = new double[nTraits];
		if (init == null || init.length != nTraits) {
			// this.init.length == nTraits != init.length always true if called from 
			// setInitXY e.g. in ParaGraph for multi-species modules
			if (!isDensity) {
				// reset strategy frequencies to uniform distribution
				// always signals change
				Arrays.fill(this.init, 1.0 / nTraits);
				return;
			}
			// check if there are vacant traits (check length of names in ODEEuler, which
			// includes vacants, which is a bit hackish...)
			boolean fullInit = (init != null && ((ODEEuler) model).getTraitNames().length == init.length);
			if (!fullInit) {
				engine.logWarning("no default initial state for density models... setting all to zero.");
				Arrays.fill(this.init, 0.0);
				return;
			}
			// apparently there are vacant traits that we are not interested in...
			// would be much easier to simply set y0 in ODEEuler but then
			// the model and the modules get out of sync...
			int idx = 0;
			for (Discrete pop : species) {
				int vacant = pop.getVacant();
				double[] popinit = new double[pop.getNTraits()];
				for (int n = 0; n < pop.getNTraits(); n++) {
					if (n == vacant) {
						popinit[n] = 0.0;
						if (fullInit)
							idx++;
						continue;
					}
					// note: init must be non-null but code checker is confused by fullInit above
					popinit[n] = init[idx++];
				}
				pop.setInit(popinit);
			}
			return;
		}
		System.arraycopy(init, 0, this.init, 0, nTraits);
		if (!isDensity) {
			// normalize frequencies
			ArrayMath.normalize(this.init);
		}
	}

	/**
	 * Get the initial frequencies or densities of all traits.
	 * 
	 * @return the initial frequencies or densities
	 */
	public double[] getInit() {
		return init;
	}

	/**
	 * Check if initial configuration is monomorphic (regardless of whether
	 * {@code VACANT} sites are permitted).
	 *
	 * @return the index of the monomorphic trait or {@code &lt;0} if initial state
	 *         is mixed
	 */
	public int isMonoInit() {
		int monoType = -1;
		for (int i = 0; i < nTraits; i++) {
			if (i == VACANT || init[i] < 1e-8)
				continue;
			if (monoType < 0) {
				monoType = i;
				continue;
			}
			return -1;
		}
		if (monoType < 0)
			return VACANT;
		return monoType;
	}

	/**
	 * Command line option to set the initial configuration.
	 * 
	 * @see org.evoludo.simulator.models.IBSD.InitType models.IBSD.InitType
	 * @see EvoLudo#cloInitType
	 */
	public final CLOption cloInit = new CLOption("init", "uniform", EvoLudo.catModel, null,
			new CLODelegate() {

				/**
				 * {@inheritDoc}
				 * <p>
				 * Parse initial frequencies or densities of each trait. {@code arg} can be a
				 * single value or an array with the separator
				 * {@value CLOParser#MATRIX_DELIMITER}. The parser cycles through {@code arg}
				 * until the initial configuration is set.
				 * <p>
				 * For frequency based modules the initial values do not need to be normalized.
				 * Specifying a dash, "-", instead of a number, disables the corresponding
				 * strategy/trait.
				 * 
				 * @param arg the (array of) of initial frequencies or densities
				 * 
				 * @see Discrete#setInit(double[])
				 * @see Discrete#setActiveTraits(boolean[])
				 */
				@Override
				public boolean parse(String arg) {
					if (arg == null || !cloInit.isSet()) {
						for (Discrete dpop : species) {
							dpop.setInit(null);
							dpop.setActiveTraits(null);
						}
						// could return false to signal parsing problems
						return true;
					}

					String[] initstates = arg.split(CLOParser.SPECIES_DELIMITER);
					int n = 0;
					for (Discrete dpop : species) {
						String[] initstate = initstates[n++ % initstates.length].split(CLOParser.VECTOR_DELIMITER);
						int len = initstate.length;
						int nt = dpop.getNTraits();
						double[] state = new double[nt];
						boolean[] act = new boolean[nt];
						int vacant = dpop.getVacant();
						for (int i = 0; i < nt; i++) {
							String ei = initstate[i % len].trim();
							if (ei.equals("-")) {
								state[i] = 0.0;
								if (i != vacant) {
									act[i] = false;
									continue;
								}
								logger.warning("Vacant sites cannot be deactivated - ignored.");
								act[i] = true;
								continue;
							}
							state[i] = CLOParser.parseDouble(ei);
							act[i] = true;
						}
						dpop.setInit(state);
						dpop.setActiveTraits(act);
					}
					return true;
				}

				@Override
				public void report(PrintStream output) {
					for (Discrete dpop : species) {
						double[] ifreqs = dpop.getInit();
						boolean[] act = dpop.getActiveTraits();
						for (int n = 0; n < dpop.getNTraits(); n++)
							output.println("# init:                 " + Formatter.format(ifreqs[n], 6) + "\t"
									+ (species.size() > 1 ? " " + dpop.getName() + "." : "") + dpop.getTraitName(n)
									+ (act[n] ? " (active)" : " (disabled)"));
					}
				}

				@Override
				public String getDescription() {
					String descr = "--init ";
					if (species.size() > 1) {
						// multi-species
						descr += "<>       initial frequencies/densities of strategies\n" + //
								"                separated by '" + CLOParser.VECTOR_DELIMITER + "' " + //
								"and species by '" + CLOParser.SPECIES_DELIMITER + "' with";
						int idx = 0;
						for (Discrete dpop : species) {
							int nt = dpop.getNTraits();
							for (int n = 0; n < nt; n++) {
								String aTrait = "              " + (idx++) + ": ";
								int traitlen = aTrait.length();
								descr += "\n" + aTrait.substring(traitlen - 16, traitlen) + dpop.getName() + "." + dpop.getTraitName(n);
							}
						}
						descr += "\n             -: disables the respective strategy";
						return descr;
					}
					// single species
					switch (nTraits) {
						case 2:
							descr += "<0>,<1>  initial frequencies/densities, with";
							break;
						case 3:
							descr += "<0>,<1>,<2>  initial frequencies/densities, with";
							break;
						default:
							descr += "<0>,...,<" + (nTraits - 1) + ">  initial frequencies/densities, with";
					}
					for (int n = 0; n < nTraits; n++) {
						String aTrait = "              " + n + ": ";
						int traitlen = aTrait.length();
						descr += "\n" + aTrait.substring(traitlen - 16, traitlen) + traitName[n];
					}
					descr += "\n             -: disables the respective strategy";
					return descr;
				}
			});

	/**
	 * Command line option to request that models stop execution when reaching
	 * monomorphic population states.
	 */
	public final CLOption cloMonoStop = new CLOption("monostop", "nostop", CLOption.Argument.NONE, EvoLudo.catModel,
			"--monostop      stop once population become monomorphic",
			new CLODelegate() {

				/**
				 * {@inheritDoc}
				 * <p>
				 * If option is provided, models are requested to stop execution once a
				 * monomorphic state is reached.
				 * 
				 * @param arg no argument required
				 */
				@Override
				public boolean parse(String arg) {
					// default is to continue with monomorphic populations (unless it's an absorbing
					// state)
					for (Discrete dpop : species)
						dpop.setMonoStop(cloMonoStop.isSet());
					return true;
				}
			});

	@Override
	public void collectCLO(CLOParser parser) {
		super.collectCLO(parser);
		parser.addCLO(cloMonoStop);
		int nt = 0;
		for (Discrete dpop : species)
			nt = Math.max(nt, dpop.getNTraits());
		if (nt > 1) {
			parser.addCLO(cloInit);
		}
	}
}
