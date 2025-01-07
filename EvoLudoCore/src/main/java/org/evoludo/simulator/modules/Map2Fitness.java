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
import org.evoludo.util.CLOParser;
import org.evoludo.util.CLOption;
import org.evoludo.util.CLOption.CLODelegate;
import org.evoludo.util.Formatter;

/**
 * Map scores/payoffs to fitness and vice versa. Enum on steroids. Currently
 * available maps are:
 * <dl>
 * <dt>none</dt>
 * <dd>no mapping, scores/payoffs equal fitness</dd>
 * <dt>static</dt>
 * <dd>static baseline fitness, {@code b+w*score}</dd>
 * <dt>convex</dt>
 * <dd>convex combination of baseline fitness and scores,
 * {@code b(1-w)+w*scores}</dd>
 * <dt>exponential</dt>
 * <dd>exponential mapping, {@code b*exp(w*score)}</dd>
 * </dl>
 * Note that exponential payoff-to-fitness may easily be the most convincing
 * because it can be easily and uniquely derived from a set of five natural
 * assumptions on the fitness function \(F(u\):
 * <ol>
 * <li>\(F(u)\geq 0\) for every \(u\in\mathbb{R}\)
 * <li>\(F(u)\) is non-decreasing
 * <li>\(F(u)\) is continuous
 * <li>Selection strength \(w\) scales payoffs, i.e. the fitness associated with
 * payoff \(u\) at selection strength \(w\geq 0\) is \(F(w u)\)
 * <li>The probability that an individual is chosen for reproduction is
 * invariant under adding a constant \(K\) to the payoffs of all competing
 * individuals. That is, if \(u_i\) and \(F_i(u_i)\) are the payoff and
 * fecundity of individual \(i\), then
 * \[\frac{F_i(u_i)}{\dsum_j F_j(u_j)} = \frac{F_i(u_i+K)}{\dsum_j F_j(u_j+K)}\]
 * </ol>
 * Up to a rescaling of the selection strength, these assumptions lead to a
 * unique payoff-to-fecundity map, \(F(u)=e^{w u}\). The {@code static} mapping
 * then immediately follows as an approximation for weak selection.
 * 
 * @see <a href="https://doi.org/10.1371/journal.pcbi.1009611">McAvoy, A., Rao,
 *      A. &amp; Hauert, C. (2021) Intriguing effects of selection intensity on
 *      the evolution of prosocial behaviors PLoS Comp. Biol. 17 (11)
 *      e1009611</a>
 * 
 * @author Christoph Hauert
 */
public class Map2Fitness {

	/**
	 * The module that is using this fitness mapping.
	 */
	Module module;

	/**
	 * Baseline fitness for map.
	 */
	double baseline = 1.0;

	/**
	 * Selection strength for map.
	 */
	double selection = 1.0;

	/**
	 * Map type. Defaults to {@link Map#NONE}.
	 */
	Map map = Map.NONE;

	/**
	 * Instantiate new map of type {@code map} for {@code module}.
	 * 
	 * @param module the module using this mapping
	 * @param map    the map to use as template
	 */
	public Map2Fitness(Module module, Map map) {
		this.module = module;
		this.map = map;
	}

	/**
	 * Map {@code score} to fitness, based on currently selected type
	 * {@code map}.
	 * 
	 * @param score the payoff/score to convert to fitness
	 * @return the corresponding fitness
	 * 
	 * @see Map2Fitness#invmap
	 */
	public double map(double score) {
		switch (map) {
			case STATIC:
				return baseline + selection * score; // fitness = b + w score
			case CONVEX:
				return baseline + selection * (score - baseline); // fitness = b (1 - w) + w score
			case EXPONENTIAL:
				return baseline * Math.exp(selection * score); // fitness = b exp( w score)
			case NONE:
			default:
				return score;
		}
	}

	/**
	 * Map {@code fitness} to payoff/score, based on currently selected type
	 * {@code map}.
	 * 
	 * @param fitness the fitness to convert to payoff/score
	 * @return the corresponding payoff/score
	 */
	public double invmap(double fitness) {
		switch (map) {
			case STATIC:
				return (fitness - baseline) / selection; // fitness = b + w score
			case CONVEX:
				return (fitness - baseline * (1.0 - selection)) / selection; // fitness = b (1 - w) + w score
			case EXPONENTIAL:
				return Math.log(fitness / baseline) / selection; // fitness = b exp( w score)
			case NONE:
			default:
				return fitness;
		}
	}

	/**
	 * Checks if this map is of type {@code aMap}.
	 * 
	 * @param aMap the map to compare to
	 * @return {@code true} if map is of type {@code aMap}.
	 */
	public boolean isMap(Map2Fitness.Map aMap) {
		return map.equals(aMap);
	}

	/**
	 * Sets type of map to {@code map}.
	 * 
	 * @param map the type of the map
	 */
	public void setMap(Map2Fitness.Map map) {
		if (map == null)
			return;
		this.map = map;
	}

	/**
	 * Sets the baseline fitness of the map.
	 * 
	 * @param baseline the baseline fitness of the map
	 */
	public void setBaseline(double baseline) {
		this.baseline = baseline;
	}

	/**
	 * Gets the baseline fitness of the map.
	 * 
	 * @return the baseline fitness of the map
	 */
	public double getBaseline() {
		return baseline;
	}

	/**
	 * Sets the selection strength of the map. Must be positive, ignored otherwise.
	 * 
	 * @param selection the strength of selection of the map
	 */
	public void setSelection(double selection) {
		if (selection <= 0.0)
			return;
		this.selection = selection;
	}

	/**
	 * Gets the selection strength of the map.
	 * 
	 * @return the selection strength of the map
	 */
	public double getSelection() {
		return selection;
	}

	/**
	 * Gets the name/key of the current map.
	 * 
	 * @return the map key
	 */
	public String getName() {
		return map.getKey();
	}

	/**
	 * Gets the brief description of the current map.
	 * 
	 * @return the map summary
	 */
	public String getTitle() {
		return map.getTitle();
	}

	/**
	 * Command line option to set the payoff/score to fitness map.
	 */
	public final CLOption clo = new CLOption("fitnessmap", "none", EvoLudo.catModule,
			"--fitnessmap <m> [<b>[,<w>]]  payoff-to-fitness, baseline b [1]\n" + //
					"                and selection strength w [1]:",
			new CLODelegate() {

				/**
				 * {@inheritDoc}
				 * <p>
				 * Parse payoff/score to fitness map(s) for a single or multiple
				 * populations/species. {@code arg} can be a single value or an array of
				 * values with the separator {@value CLOParser#SPECIES_DELIMITER}. The parser
				 * cycles through {@code arg} until all populations/species have the the
				 * fitness map set.
				 * 
				 * @param arg the (array of) map name(s)
				 */
				@Override
				public boolean parse(String arg) {
					boolean success = true;
					String[] map2fitnessspecies = arg.split(CLOParser.SPECIES_DELIMITER);
					int n = 0;
					ArrayList<? extends Module> species = module.getSpecies();
					for (Module mod : species) {
						String m = map2fitnessspecies[n++ % map2fitnessspecies.length];
						Map2Fitness.Map m2fm = (Map2Fitness.Map) clo.match(m);
						Map2Fitness m2f = mod.getMapToFitness();
						if (m2fm == null) {
							module.logger.warning(
									(species.size() > 1 ? mod.getName() + ": " : "") +
											"fitness map '" + m + "' unknown - using '"
											+ m2f.getName() + "'");
							success = false;
							continue;
						}
						m2f.setMap(m2fm);
						// parse b and w, if present
						String[] args = m.split("\\s+|=|,");
						double b = 1.0;
						double w = 1.0;
						switch (args.length) {
							case 3:
								w = CLOParser.parseDouble(args[2]);
								// $FALL-THROUGH$
							case 2:
								b = CLOParser.parseDouble(args[1]);
								break;
							default:
						}
						m2f.setBaseline(b);
						m2f.setSelection(w);
					}
					return success;
				}

				@Override
				public void report(PrintStream output) {
					ArrayList<? extends Module> species = module.getSpecies();
					for (Module mod : species) {
						Map2Fitness m2f = mod.getMapToFitness();
						output.println("# fitnessmap:           " + m2f.getTitle()
								+ (species.size() > 1 ? " ("
										+ mod.getName() + ")" : ""));
						output.println("# basefit:              " + Formatter.format(m2f.getBaseline(), 4));
						output.println("# selection:            " + Formatter.format(m2f.getSelection(), 4));
					}
				}
			});

	/**
	 * Enum representing the different types of payoff/score to fitness maps
	 */
	public enum Map implements CLOption.Key {

		/**
		 * no mapping, scores/payoffs equal fitness, \(fit = score\)
		 */
		NONE("none", "no mapping"),

		/**
		 * static baseline fitness, \(fit = b+w*score\)
		 */
		STATIC("static", "b+w*score"),

		/**
		 * convex combination of baseline fitness and scores, \(fit = b(1-w)+w*score\)
		 */
		CONVEX("convex", "b*(1-w)+w*score"),

		/**
		 * exponential mapping of scores to fitness, \(fit = b*\exp(w*score)\)
		 */
		EXPONENTIAL("exponential", "b*exp(w*score)");

		/**
		 * Key of map. Used when parsing command line options.
		 * 
		 * @see Map2Fitness#clo
		 */
		String key;

		/**
		 * Brief description of map for help display.
		 * 
		 * @see EvoLudo#getCLOHelp()
		 */
		String title;

		/**
		 * Instantiate new type of map.
		 * 
		 * @param key   identifier for parsing of command line option
		 * @param title summary of map
		 */
		Map(String key, String title) {
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
