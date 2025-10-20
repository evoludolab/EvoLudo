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

import java.awt.Color;

import org.evoludo.simulator.models.Data;
import org.evoludo.simulator.models.SampleListener;

/**
 * {@link org.evoludo.simulator.modules.Module Module}s that implement the
 * {@code HasHistogram} interface request a graphical view to depicts a
 * histogram of data in their GUI:
 * {@link org.evoludo.simulator.views.Histogram} in GWT and
 * {@link org.evoludo.simulator.views.MVCTraitHistogram},
 * {@link org.evoludo.simulator.views.MVFitHistogram} in JRE for a single
 * continuous trait or the fitness distribution, respectively.
 * <p>
 * <strong>Important:</strong> Implementations have to be agnostic of the
 * runtime environment (JRE vs GWT).
 * 
 * @author Christoph Hauert
 */

public abstract interface HasHistogram {

	/**
	 * Some models may provide reference values for histograms as a horizontal line
	 * marking a particular value. For example this is the case for statistics of
	 * fixation probabilities or times in the
	 * {@code org.evoludo.simulator.modules.Moran Moran} module where the analytical
	 * results are provided as a reference to highlight the effects of population
	 * structures.
	 * 
	 * @param type the type of data shown in the histogram
	 * @param idx  the index of the trait
	 * @return the array of levels for reference
	 */
	public default double[] getCustomLevels(Data type, int idx) {
		return new double[0];
	}

	/**
	 * Get the number of traits in the {@link org.evoludo.simulator.modules.Module
	 * Module}s. For example, histograms are shown for each trait in continuous
	 * modules or for the fitness distribution of each strategy in discrete modules.
	 * 
	 * @return the number of traits
	 */
	public int getNTraits();

	/**
	 * Get the colors for the different trait histograms.
	 * 
	 * @return the array with trait colors
	 */
	public Color[] getTraitColors();

	/**
	 * {@link org.evoludo.simulator.modules.Module Module}s that implement the
	 * {@code HasHistogram.Strategy} interface include histograms of strategy
	 * distributions. Currently this only applies to continuous modules.
	 * 
	 * @see org.evoludo.simulator.views.Histogram
	 * @see org.evoludo.simulator.views.MVCTraitHistogram
	 */
	public interface Strategy extends HasHistogram {
	}

	/**
	 * {@link org.evoludo.simulator.modules.Module Module}s that implement the
	 * {@code HasHistogram.Fitness} interface include histograms of fitness
	 * distributions. For discrete modules the distribution is show for each trait
	 * separately.
	 * 
	 * @see org.evoludo.simulator.views.Histogram
	 * @see org.evoludo.simulator.views.MVFitHistogram
	 */
	public interface Fitness extends HasHistogram {
	}

	/**
	 * {@link org.evoludo.simulator.modules.Module Module}s that implement the
	 * {@code HasHistogram.Degree} interface include histograms of the degree
	 * distribution of the population geometry. Degree histograms are show
	 * separately for interaction and reference graphs, as well as for incoming and
	 * outgoing links (for directed graphs).
	 * 
	 * @see org.evoludo.simulator.views.Histogram
	 */
	public interface Degree extends HasHistogram {
	}

	/**
	 * {@link org.evoludo.simulator.modules.Module Module}s that implement the
	 * {@code HasHistogram.StatisticsProbability} interface must be capable of
	 * running statistics over multiple runs and include histograms of fixation
	 * probabilities.
	 * 
	 * @see org.evoludo.simulator.views.Histogram
	 */
	public interface StatisticsProbability extends HasHistogram, SampleListener {
	}

	/**
	 * {@link org.evoludo.simulator.modules.Module Module}s that implement the
	 * {@code HasHistogram.StatisticsTime} interface must be capable of running
	 * statistics over multiple runs and include histograms of fixation times.
	 * 
	 * @see org.evoludo.simulator.views.Histogram
	 */
	public interface StatisticsTime extends HasHistogram, SampleListener {
	}

	/**
	 * {@link org.evoludo.simulator.modules.Module Module}s that implement the
	 * {@code HasHistogram.StatisticsStationary} interface must be capable of
	 * generate histograms of the number of times particular configurations
	 * (frequencies of strategies) are visited, which eventually converges to
	 * a stationary distributions in ergodic settings.
	 * 
	 * @see org.evoludo.simulator.views.Histogram
	 */
	public interface StatisticsStationary extends HasHistogram {
	}

	// public interface Statistics extends StatisticsProbability, StatisticsTime {
	// }

	// public interface All extends Strategy, Fitness, Degree, Statistics {
	// }
}
