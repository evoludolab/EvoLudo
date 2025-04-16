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

import java.io.PrintStream;
import java.util.ArrayList;

import org.evoludo.simulator.EvoLudo;
import org.evoludo.simulator.models.IBSPopulation;
import org.evoludo.util.CLOParser;
import org.evoludo.util.CLOption;
import org.evoludo.util.CLOption.CLODelegate;
import org.evoludo.util.Formatter;

/**
 * The implementation of player updates. Player updates are used to update the
 * traits of individuals in a population through (probabilistic) comparisons
 * with the performance of other members (and their trait) in the population. 
 * <p>
 * The player update type can be set to one of the following:
 * <dl>
 * <dt>best
 * <dd>best wins (equal - stay)
 * <dt>best-random
 * <dd>best wins (equal - random)
 * <dt>best-response
 * <dd>best-response dynamics
 * <dt>imitate
 * <dd>imitate/replicate (linear)
 * <dt>imitate-better
 * <dd>imitate/replicate (better only)
 * <dt>proportional
 * <dd>proportional to payoff
 * <dt>thermal
 * <dd>Fermi/thermal update
 * </dl>
 * The player update type can be set via the command line option {@code
 * --playerupdate <u> [<n>[,<e>]]} where {@code <u>} is the player update type,
 * {@code <n>} the noise when updating the trait, and {@code <e>} the error
 * probability when adopting the trait.
 * 
 * @author Christoph Hauert
 */
public class PlayerUpdate {

	/**
	 * The module that is using this player update.
	 */
	Module module;

	/**
	 * Instantiate new player update for use in {@code module}.
	 * 
	 * @param module the module using this player update
	 */
	public PlayerUpdate(Module module) {
		this.module = module;
	}

	/**
	 * The player update type.
	 * 
	 * @see #clo
	 */
	protected Type type = Type.IMITATE;

	/**
	 * Sets the player update type.
	 * 
	 * @param type the updating type for players
	 * @return {@code true} if player update type changed
	 */
	public boolean setType(Type type) {
		if (type == null || type == this.type)
			return false;
		this.type = type;
		return true;
	}

	/**
	 * Gets the player update type.
	 * 
	 * @return the player update type
	 */
	public Type getType() {
		return type;
	}

	/**
	 * The noise of the updating process of players.
	 */
	double noise;

	/**
	 * Set the noise of the updating process of players. With less noise chances are
	 * higher to adopt the traitof individuals even if they perform only marginally
	 * better. Conversely for large noise payoff differences matter less and the
	 * updating process is more random. For {@code noise==1} the process is neutral.
	 * 
	 * @param noise the noise when updating the trait
	 */
	public void setNoise(double noise) {
		this.noise = Math.max(0.0, noise);
	}

	/**
	 * Get the noise of the updating process.
	 * 
	 * @return the noise when updating the trait
	 * 
	 * @see #setNoise(double)
	 */
	public double getNoise() {
		return noise;
	}

	/**
	 * The probability of an error during the updating of the trait.
	 */
	double error;

	/**
	 * Set the error of the updating process. With probability {@code error} an
	 * individual fails to adopt a better performing trait or adopts an worse
	 * performing one. More specifically the range of updating probabilities is
	 * restricted to {@code [error, 1-error]} such that always a chance remains that
	 * the trait of a better performing individual is not adopted or the one of a
	 * worse performing one is adopted.
	 * 
	 * @param error the error when adopting the trait
	 */
	public void setError(double error) {
		error = Math.max(0.0, error);
	}

	/**
	 * Get the error of the updating process.
	 * 
	 * @return the error when adopting the trait
	 * 
	 * @see #setError(double)
	 */
	public double getError() {
		return error;
	}

	@Override
	public String toString() {
		String str = type.toString();
		switch (type) {
			case THERMAL: // fermi update
			case IMITATE: // imitation update
			case IMITATE_BETTER: // imitation update (better traits only)
				str += " " + Formatter.formatSci(noise, 6);
				// XXX add errors to PROPORTIONAL as well as DE models?
				if (module.model.isIBS())
					str += "," + Formatter.formatSci(error, 6);
				break;
			default:
				// no other PlayerUpdate.Type seems to implement noise
				break;
		}
		return str;
	}

	/**
	 * Command line option to set the type of player updates.
	 */
	public final CLOption clo = new CLOption("playerupdate",
			PlayerUpdate.Type.IMITATE.getKey() + " 1,0",
			EvoLudo.catModel,
			"--playerupdate <u> [<n>[,<e>]] set player update type with\n" + //
					"                noise n (neutral=1) and error probability e (0):",
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
					String[] playerupdates = arg.split(CLOParser.SPECIES_DELIMITER);
					int n = 0;
					ArrayList<? extends Module> species = module.getSpecies();
					for (Module mod : species) {
						String updt = playerupdates[n++ % playerupdates.length];
						PlayerUpdate.Type put = (PlayerUpdate.Type) clo.match(updt);
						PlayerUpdate pu = mod.getPlayerUpdate();
						if (put == null) {
							if (success)
								module.logger.warning((species.size() > 1 ? mod.getName() + ": " : "") + //
										"player update '" + updt + "' not recognized - using '"
										+ pu.getType()
										+ "'");
							success = false;
							continue;
						}
						pu.setType(put);
						// parse n, e, if present
						String[] args = updt.split("\\s+|=|,");
						double nois = 1.0;
						double err = 0.0;
						switch (args.length) {
							case 3:
								err = CLOParser.parseDouble(args[2]);
								// $FALL-THROUGH$
							case 2:
								nois = CLOParser.parseDouble(args[1]);
								break;
							default:
						}
						pu.setNoise(nois);
						pu.setError(err);
					}
					return success;
				}

				@Override
				public void report(PrintStream output) {
					ArrayList<? extends Module> species = module.getSpecies();
					boolean isIBS = module.model.isIBS();
					for (Module mod : species) {
						if (isIBS) {
							IBSPopulation ibspop = mod.getIBSPopulation();
							// skip populations with Moran updates
							if (ibspop.getPopulationUpdate().isMoran())
								continue;
						}
						output.println("# playerupdate:         " + mod.getPlayerUpdate()
								+ (species.size() > 1 ? " (" + mod.getName() + ")" : ""));
					}
				}
			});

	/**
	 * Player update types. Enum on steroids. Currently available player update
	 * types are:
	 * <dl>
	 * <dt>best
	 * <dd>best wins (equal - stay)
	 * <dt>best-random
	 * <dd>best wins (equal - random)
	 * <dt>best-response
	 * <dd>best-response dynamics
	 * <dt>imitate
	 * <dd>imitate/replicate (linear)
	 * <dt>imitate-better
	 * <dd>imitate/replicate (better only)
	 * <dt>proportional
	 * <dd>proportional to payoff
	 * <dt>thermal
	 * <dd>Fermi/thermal update
	 * </dl>
	 */
	public static enum Type implements CLOption.Key {

		/**
		 * best wins (equal - stay)
		 */
		BEST("best", "best wins (equal - stay)"),

		/**
		 * best wins (equal - random)
		 */
		BEST_RANDOM("best-random", "best wins (equal - random)"),

		/**
		 * best-response
		 */
		BEST_RESPONSE("best-response", "best-response"),

		/**
		 * imitate/replicate (linear)
		 */
		IMITATE("imitate", "imitate/replicate (linear)"),

		/**
		 * imitate/replicate (better only)
		 */
		IMITATE_BETTER("imitate-better", "imitate/replicate (better only)"),

		/**
		 * proportional to payoff
		 */
		PROPORTIONAL("proportional", "proportional to payoff"),

		/**
		 * Fermi/thermal update
		 */
		THERMAL("thermal", "Fermi/thermal update");

		/**
		 * Key of player update. Used when parsing command line options.
		 * 
		 * @see PlayerUpdate#clo
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
