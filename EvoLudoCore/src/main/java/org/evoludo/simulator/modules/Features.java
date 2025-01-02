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

/**
 * Interface to query features of the Module.
 * 
 * @author Christoph Hauert
 */
public interface Features {

	/**
	 * Returns whether payoffs/fitness are static ({@code false} by default).
	 * 
	 * @return {@code true} if static
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
	 * Interface that all modules with static fitness/payoffs should implement. The
	 * original Moran process is an example, see
	 * {@link org.evoludo.simulator.modules.Moran}.
	 */
	public interface Static extends Features {

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
