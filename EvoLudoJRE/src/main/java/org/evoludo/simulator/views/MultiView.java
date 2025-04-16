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

package org.evoludo.simulator.views;

import java.util.ArrayList;

import org.evoludo.simulator.EvoLudo;
import org.evoludo.simulator.models.Model;
import org.evoludo.simulator.models.ODE;
import org.evoludo.simulator.models.PDE;
import org.evoludo.simulator.modules.Module;
import org.evoludo.util.CLOption;

public interface MultiView {

	public String	getName();

	public void		activate();
	
	public void		deactivate();
	
	public boolean	isActive();

	public default void	setModule(Module module) { };

	public void		reset(boolean clear);
	
	public void		init();
	
	public void		update(boolean updateGUI);
	
	public void		end();
	
	public void		parametersChanged(boolean didReset);
	
	public void		setContextMenuEnabled(boolean enabled);

	public enum DataTypes implements CLOption.Key {

		/**
		 * Report the strategies of all individuals.
		 */
		TRAITS("traits", "traits of all individuals"),

		/**
		 * Report the mean traits in all populations.
		 */
		MEAN("traitmean", "mean traits of population(s)"),

		/**
		 * Report the distribution of traits.
		 */
		HISTOGRAM("traithistogram", "histogram of trait distributions"),

		/**
		 * Report the scores of all individuals (prior to mapping to fitness).
		 */
		SCORES("scores", "scores of all individuals"),

		/**
		 * Report the fitness of all individuals (mapped scores).
		 */
		FITNESS("fitness", "fitness of all individuals"),

		/**
		 * Report the mean fitness in all populations.
		 */
		FITMEAN("fitmean", "mean fitness of population(s)"),

		/**
		 * Report the distribution of traits.
		 */
		FITHISTOGRAM("fithistogram", "histogram of fitness distributions"),

		/**
		 * Report the distribution of traits.
		 */
		STRUCTURE("structdegree", "degree distribution of population structure"),

		/**
		 * Report the statistics of fixation probabilities.
		 */
		STAT_PROB("statprob", "statistics of fixation probabilities"),

		/**
		 * Report the statistics of fixation times.
		 */
		STAT_UPDATES("statupdates", "statistics of updates till fixation"),

		/**
		 * Report the statistics of fixation times.
		 */
		STAT_TIMES("stattimes", "statistics of fixation times");

		/**
		 * Key of data types. Used when parsing command line options.
		 * 
		 * @see org.evoludo.simulator.EvoLudoJRE#cloData
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
		DataTypes(String key, String title) {
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

	/**
	 * Determine and return data types that module can report derived from the data
	 * visualizations that are available for the current module.
	 *
	 * @return the array of data types that can be reported by this module
	 * 
	 * @see org.evoludo.simulator.EvoLudoLab#updateViews
	 */
	public static DataTypes[] getAvailableDataTypes(Module module, Model model) {
		ArrayList<DataTypes> dataOutputs = new ArrayList<>();
		// query available views to deduce the data types to report
		// individual data
		boolean isODESDE = (model instanceof ODE && !(model instanceof PDE));
		if (module instanceof HasPop2D.Traits && !isODESDE)
			dataOutputs.add(DataTypes.TRAITS);
		if (module instanceof HasPop2D.Fitness && !isODESDE) {
			dataOutputs.add(DataTypes.SCORES);
			dataOutputs.add(DataTypes.FITNESS);
		}
		// mean data
		if (module instanceof HasMean.Traits || module instanceof HasPhase2D || module instanceof HasS3)
			dataOutputs.add(DataTypes.MEAN);
		if (module instanceof HasMean.Fitness)
			dataOutputs.add(DataTypes.FITMEAN);

		// histograms
		if (module instanceof HasHistogram.Strategy || module instanceof HasDistribution.Strategy)
			dataOutputs.add(DataTypes.HISTOGRAM);
		if (module instanceof HasHistogram.Fitness)
			dataOutputs.add(DataTypes.FITHISTOGRAM);
		if (module instanceof HasHistogram.Degree && !isODESDE)
			dataOutputs.add(DataTypes.STRUCTURE);

		// statistics
		if (module instanceof HasHistogram.StatisticsProbability)
			dataOutputs.add(DataTypes.STAT_PROB);
		if (module instanceof HasHistogram.StatisticsTime) {
			dataOutputs.add(DataTypes.STAT_UPDATES);
			dataOutputs.add(DataTypes.STAT_TIMES);
		}
		return dataOutputs.toArray(new DataTypes[0]);
	}
}
