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

/**
 * {@link org.evoludo.simulator.modules.Module Module}s that implement the
 * {@code HasPop3D} interface request a graphical view to visualize a 3D
 * representation of the geomtery of the population in their GUI:
 * {@link org.evoludo.simulator.views.Pop3D} for GWT and none for JRE
 * ({@code java3d} has long been retired).
 * <p>
 * <strong>Important:</strong> Implementations have to be agnostic of the
 * runtime environment (JRE vs GWT).
 * 
 * @author Christoph Hauert
 */
public abstract interface HasPop3D {

	/**
	 * {@link org.evoludo.simulator.modules.Module Module}s that implement the
	 * {@code HasPop3D.Traits} interface include 2D graphical visualizations of
	 * the geometry of the population where nodes display the color coded trait
	 * of each individual. For discrete modules each trait has a distinct colour,
	 * whereas for continuous modules the traits are shown on a color gradient
	 * between the minimum and maximum trait values.
	 * 
	 * @see org.evoludo.simulator.views.Pop3D
	 */
	public interface Traits extends HasPop3D {
	}

	/**
	 * {@link org.evoludo.simulator.modules.Module Module}s that implement the
	 * {@code HasPop3D.Traits} interface include 3D graphical visualizations of
	 * the geometry of the population where nodes display the color coded fitness of
	 * each individual. Fitness values are shown on a color gradient between the
	 * minimum and maximum scores/payoffs/fitness.
	 * 
	 * @see org.evoludo.simulator.views.Pop3D
	 */
	public interface Fitness extends HasPop3D {
	}
}
