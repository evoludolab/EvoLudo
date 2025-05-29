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

import java.util.Arrays;

import org.evoludo.simulator.EvoLudo;
import org.evoludo.simulator.models.PopulationUpdate;
import org.evoludo.util.CLOParser;
import org.evoludo.util.CLOption;
import org.evoludo.util.CLOption.CLODelegate;
import org.evoludo.util.CLOption.Category;
import org.evoludo.util.Formatter;

/**
 * The implementation of population updates. Population updates are used to update
 * the population size and composition of individuals.
 * <p>
 * The population update type can be set to one of the following:
 * <dl>
 * <dt>synchronous
 * <dd>Synchronized population updates.
 * <dt>Wright-Fisher
 * <dd>Wright-Fisher process (synchronous)
 * <dt>asynchronous
 * <dd>Asynchronous population updates (default).
 * <dt>Bd
 * <dd>Moran process (birth-death, asynchronous).
 * <dt>dB
 * <dd>Moran process (death-birth, asynchronous).
 * <dt>imitate
 * <dd>Moran process (imitate, asynchronous).
 * <dt>ecology
 * <dd>Asynchronous updates (non-constant population size).
 * </dl>
 * <p>
 * The population update type can be set via the command line option {@code
 * --popupdate <u> [<p>]} where {@code <u>} is the population update type, and
 * {@code <p>} the fraction of individuals that reassess their trait.
 * 
 * @author Christoph Hauert
 */
public class SpeciesUpdate {

	/**
	 * The module that is using this species update.
	 */
	Module module;

	/**
	 * The rates at which the different species are updated.
	 */
	double[] rate;

	/**
	 * Instantiate new population update for use in IBS {@code model}s.
	 * 
	 * @param module the module using this species update
	 */
	public SpeciesUpdate(Module module) {
		this.module = module;
		rate = new double[module.getNSpecies()];
		Arrays.fill(rate, 1.0);
	}

	/**
	 * The species update type.
	 * 
	 * @see #clo
	 */
	SpeciesUpdate.Type type;

	/**
	 * Sets the population update type.
	 * 
	 * @param type the updating type for the population
	 * @return {@code true} if population update type changed
	 */
	public boolean setType(SpeciesUpdate.Type type) {
		if (type == null || type == this.type)
			return false;
		this.type = type;
		return true;
	}

	/**
	 * Gets the population update type.
	 * 
	 * @return the population update type
	 */
	public SpeciesUpdate.Type getType() {
		return type;
	}

	@Override
	public String toString() {
		String str = Formatter.format(rate, 3);
		return (module.model.isIBS() ? str + " " + type.getKey() : str);
	}

	/**
	 * Command line option to set the method for updating the population(s).
	 * 
	 * @see PopulationUpdate.Type
	 */
	public final CLOption clo = new CLOption("speciesupdate", "1 " + SpeciesUpdate.Type.SIZE.getKey(), Category.Module,
			"--speciesupdate <r1,r2,...>[ <t>]  species update rates and type:",
			new CLODelegate() {

				/**
				 * {@inheritDoc}
				 * <p>
				 * Parse population update types for a single or multiple populations/species.
				 * <code>arg</code> can be a single value or an array of values with the
				 * separator {@value CLOParser#SPECIES_DELIMITER}. The parser cycles through
				 * <code>arg</code> until all populations/species have the population update
				 * type set.
				 * 
				 * @param arg the (array of) update types
				 */
				@Override
				public boolean parse(String arg) {
					String[] specrates = arg.split("\\s+");
					if (specrates == null || specrates.length == 0) {
						module.logger.warning("no species update rate provided.");
						return false;
					}
					double[] spr = CLOParser.parseVector(specrates[0]);
					int len = spr.length;
					for (int i=0; i<rate.length; i++)
						rate[i] = spr[i % len];
					if (specrates.length > 1) {
						SpeciesUpdate.Type put = (SpeciesUpdate.Type) clo.match(specrates[1]);
						if (put == null)
							return false;
						setType(put);
					}
					return true;
				}
			});

	/**
	 * Types of species updates (only relevant for multi-species models):
	 * <dl>
	 * <dt>size</dt>
	 * <dd>focal species selected proportional to their size</dd>
	 * <dt>fitness</dt>
	 * <dd>focal species selected proportional to their total fitness</dd>
	 * <dt>turns</dt>
	 * <dd>one species is selected after another.</dd>
	 * <dt>sync</dt>
	 * <dd>simultaneous updates of all species (not yet implemented).</dd>
	 * </dl>
	 * For <em>size</em> and <em>fitness</em> selection is also proportional to the
	 * update rate of each species.
	 */
	public static enum Type implements CLOption.Key {

		/**
		 * Pick focal species based on population size.
		 */
		SIZE("size", "pick species based on size"), //

		/**
		 * Pick focal species based on population size.
		 */
		UNIFORM("uniform", "pick species with equal probabilities"), //

		/**
		 * Pick focal species based on population fitness.
		 */
		FITNESS("fitness", "pick species based on fitness"), //

		/**
		 * Pick species sequentially.
		 */
		TURNS("turns", "pick species sequentially"); //

		/**
		 * Simultaneous updates of all species. Not implemented
		 */
		// SYNC("sync", "simultaneous updates of all species"); //

		/**
		 * Key of species update type. Used for parsing command line options.
		 * 
		 * @see SpeciesUpdate#clo
		 */
		String key;

		/**
		 * Brief description of species update type for GUI and help display.
		 * 
		 * @see EvoLudo#getCLOHelp()
		 */
		String title;

		/**
		 * Instantiate new species update type.
		 * 
		 * @param key   the identifier for parsing of command line options
		 * @param title the summary of the species update for GUI and help display
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

		@Override
		public String toString() {
			return key + ": " + title;
		}
	}
}
