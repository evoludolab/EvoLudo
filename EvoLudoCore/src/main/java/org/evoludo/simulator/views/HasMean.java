//
// EvoLudo Project
//
// Copyright 2020 Christoph Hauert
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

package org.evoludo.simulator.views;

/**
 * {@link org.evoludo.simulator.modules.Module Module}s that implement the
 * {@code HasMean} interface request a graphical view to visualize the
 * mean state of the population as a function of time in their GUI:
 * {@link org.evoludo.simulator.views.Mean} for GWT and
 * {@link org.evoludo.simulator.views.MVDMean},
 * {@link org.evoludo.simulator.views.MVCMean} for JRE.
 * <p>
 * <strong>Important:</strong> Implementations have to be agnostic of the
 * runtime environment (JRE vs GWT).
 * 
 * @author Christoph Hauert
 */
public abstract interface HasMean {

	/**
	 * {@link org.evoludo.simulator.modules.Module Module}s that implement this
	 * interface request a {@link org.evoludo.graphics.LineGraph LineGraph}
	 * that depicts the mean strategies of the population. For discrete modules with
	 * \(d\) traits the frequency/density of all traits is shown in a single panel.
	 * In contrast, for continuous modules with \(d\) traits, \(d\) panels are shown
	 * each depicting the mean \(\bar x_i\) of one trait together with its standard
	 * deviation \(\bar x_i \pm \sigma_i\).
	 * <p>
	 * <strong> Note:</strong> this requires that the
	 * {@link org.evoludo.simulator.models.Model Model} implements the
	 * method {@link org.evoludo.simulator.models.Model#getMeanTraits(int, double[])
	 * Model.getMeanTraits(int, double[])}.
	 * 
	 * @author Christoph Hauert
	 */
	public interface Strategy extends HasMean {
	}

	/**
	 * {@link org.evoludo.simulator.modules.Module Module}s that implement this
	 * interface request a {@link org.evoludo.graphics.LineGraph LineGraph}
	 * that depicts the mean fitness of the population. For discrete modules the
	 * mean fitness of each trait is shown together with the overall mean fitness of
	 * the population. For continuous modules the mean overall fitness of the
	 * population \(\bar f\) is shown together with its standard deviation
	 * \(\bar f \pm \sigma\).
	 * <p>
	 * <strong> Note:</strong> this requires that the
	 * {@link org.evoludo.simulator.models.Model Model} implements the method
	 * {@link org.evoludo.simulator.models.Model#getMeanFitness(int, double[])
	 * Model.getMeanFitness(int, double[])}.
	 * 
	 * @author Christoph Hauert
	 */
	public interface Fitness extends HasMean {
	}
}
