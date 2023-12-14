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

import java.util.ArrayList;
import org.evoludo.simulator.EvoLudo;
import org.evoludo.util.CLOParser;
import org.evoludo.util.CLOption;
import org.evoludo.util.CLOption.CLODelegate;

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

	@Deprecated
	public void setInit(double[] init) {
	}

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
	}
}
