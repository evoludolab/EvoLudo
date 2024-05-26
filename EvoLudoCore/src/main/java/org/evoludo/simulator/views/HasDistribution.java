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
 * {@code HasDistribution} interface request a graphical view to visualize the
 * density distribution of data in multiple dimensions in their GUI:
 * {@link org.evoludo.simulator.views.Distribution} for GWT and
 * {@link org.evoludo.simulator.views.MVCDistr},
 * {@link org.evoludo.simulator.views.MVC2Distr} for JRE modules with one or two
 * continuous traits, respectively.
 * <p>
 * <strong>Important:</strong> Implementations have to be agnostic of the
 * runtime environment (JRE vs GWT).
 * 
 * @author Christoph Hauert
 */
public abstract interface HasDistribution {

	/**
	 * Gets the number of traits in the {@link org.evoludo.simulator.modules.Module
	 * Module}s. The trait distribution is shown over time if there is only a single
	 * trait and otherwise a 2D trait distribution is shown where the two traits can
	 * be selected and changed through a context menu.
	 * 
	 * @return the number of traits
	 */
	public int getNTraits();

	/**
	 * Gets the name of the trait with index <code>idx</code>. This is used to label
	 * the graph and entries in the context menu, if applicable.
	 * 
	 * @param idx the index of the trait
	 * @return name of the trait
	 */
	public String getTraitName(int idx);

	/**
	 * {@link org.evoludo.simulator.modules.Module Module}s that implement the
	 * HasDistribution.Strategy interface include a graphical view that depicts the
	 * density distribution of strategic traits over time in their GUI. For example
	 * Modules with continuous traits such as the continuous Snowdrift game,
	 * {@link org.evoludo.simulator.modules.CSD CSD}, show the trait distribution
	 * over time, while the division of labour,
	 * {@link org.evoludo.simulator.modules.CLabour CLabour}, shows a 2D density
	 * distribution.
	 * <p>
	 * <strong> Note:</strong> this requires a continuous
	 * {@link org.evoludo.simulator.models.Model.ContinuousIBS Model}, which
	 * implements the method
	 * {@link org.evoludo.simulator.models.Model.ContinuousIBS#getTraitHistogramData
	 * getTraitHistogramData}.
	 * 
	 * @see HasDistribution
	 * 
	 * @author Christoph Hauert
	 */
	public interface Strategy extends HasDistribution {
	}

	// public interface Fitness {
	//
	// }
}
