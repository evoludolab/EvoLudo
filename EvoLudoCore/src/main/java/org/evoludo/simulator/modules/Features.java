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

package org.evoludo.simulator.modules;

import org.evoludo.simulator.models.Model;

/**
 * Interface to query features of the Module.
 * 
 * @author Christoph Hauert
 */
public interface Features {

	/**
	 * Modules that are based on contact processes should implement the
	 * {@link Contact} interface and thus return {@code true}. All other modules
	 * must return {@code false}.
	 * 
	 * @return {@code true} if contact process
	 * 
	 * @see SIR
	 */
	public default boolean isContact() {
		return false;
	}

	/**
	 * Modules that are based on static fitness should implement the {@link Static}
	 * interface and thus return {@code true}. The {@link Moran} modules is an
	 * example. All other modules must return {@code false}.
	 * 
	 * @return {@code false} by default
	 * 
	 * @see org.evoludo.simulator.modules.Moran
	 * @see Static
	 */
	public default boolean isStatic() {
		return false;
	}

	/**
	 * Returns whether interactions are restricted to pairs ({@code false} by
	 * default). For modules that allow interactions in larger groups this returns
	 * {@code true} only if the group size parameter is set to {@code 2}.
	 * 
	 * @return {@code true} if pairwise interactions
	 * 
	 * @see Module#setNGroup(int)
	 */
	public default boolean isPairwise() {
		return false;
	}

	/**
	 * Interface that all modules based on contact processes should implement. The
	 * {@link SIR} module is an example.
	 */
	public interface Contact extends Features {

		@Override
		public default boolean isContact() {
			return true;
		}
	}

	/**
	 * Interface that all modules with static fitness/payoffs should implement. The
	 * original Moran process is an example, see
	 * {@link org.evoludo.simulator.modules.Moran}.
	 */
	public interface Static extends Payoffs {

		@Override
		public default boolean isStatic() {
			return true;
		}

		/**
		 * Gets the static scores for the different types.
		 * 
		 * @return the array with the static scores
		 */
		public double[] getStaticScores();
	}

	/**
	 * Interface that all modules with frequency dependent fitness/payoffs should
	 * implement. The classical {@code 2x2} games is an example, see
	 * {@link org.evoludo.simulator.modules.TBT}.
	 */
	public interface Payoffs extends Features {

		/**
		 * Calculates and returns the minimum payoff/score of an individual. This value
		 * is important for converting payoffs/scores into probabilities, for scaling
		 * graphical output and some optimizations.
		 * 
		 * @return the minimum payoff/score
		 * 
		 * @see Model#getMinScore(int)
		 */
		public abstract double getMinPayoff();

		/**
		 * Calculates and returns the maximum payoff/score of an individual. This value
		 * is important for converting payoffs/scores into probabilities, for scaling
		 * graphical output and some optimizations.
		 * 
		 * @return the maximum payoff/score
		 * 
		 * @see Model#getMaxScore(int)
		 */
		public abstract double getMaxPayoff();

		/**
		 * Checks whether dynamic is neutral, i.e. no selection acting on the different
		 * traits.
		 * 
		 * @return {@code true} if all payoffs identical
		 */
		public default boolean isNeutral() {
			return (Math.abs(getMaxPayoff() - getMinPayoff()) < 1e-8);
		}

		/**
		 * Calculates and returns the minimum payoff/score of individuals in monomorphic
		 * populations.
		 * 
		 * @return the minimum payoff/score in monomorphic populations
		 */
		public abstract double getMinMonoPayoff();

		/**
		 * Calculates and returns the maximum payoff/score of individuals in monomorphic
		 * populations.
		 * 
		 * @return the maximum payoff/score in monomorphic populations
		 */
		public abstract double getMaxMonoPayoff();
	}

	/**
	 * All modules that admit interactions in pairs (as opposed to larger groups)
	 * should implement this interface. The classical {@code 2×2} games are an
	 * example, see {@link org.evoludo.simulator.modules.TBT}.
	 */
	interface Pairs extends Features {

		@Override
		public default boolean isPairwise() {
			return true;
		}
	}

	/**
	 * All modules that admit interactions in larger groups (as opposed to
	 * interactions in pairs) should implement this interface. The voluntary public
	 * goods game is an example, see {@link org.evoludo.simulator.modules.CDL}.
	 */
	interface Groups extends Pairs {

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
	}
}
