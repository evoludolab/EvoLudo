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

package org.evoludo.simulator.geom;

import org.evoludo.simulator.EvoLudo;

/**
 * Base class for lattice-based geometries providing shared handling of fixed
 * boundary flags and parsing utilities.
 */
public abstract class AbstractLattice extends AbstractGeometry {

	/**
	 * Flag indicating whether boundaries are fixed or periodic (default).
	 */
	protected boolean fixedBoundary = false;

	/**
	 * Create a lattice geometry scaffold for the given engine.
	 *
	 * @param engine the EvoLudo pacemaker
	 */
	protected AbstractLattice(EvoLudo engine) {
		super(engine);
	}

	private boolean isInterspecies = false;

	@Override
	public boolean isInterspecies() {
		return isInterspecies;
	}

	/**
	 * @return {@code true} if the lattice uses fixed boundaries instead of periodic
	 *         ones.
	 */
	public boolean isFixedBoundary() {
		return fixedBoundary;
	}

	/**
	 * Set whether the lattice uses fixed boundaries.
	 */
	public void setFixedBoundary(boolean fixedBoundary) {
		this.fixedBoundary = fixedBoundary;
	}

	@Override
	public void reset() {
		super.reset();
		fixedBoundary = false;
	}

	public void init(boolean isInterspecies) {
		this.isInterspecies = isInterspecies;
		init();
	}

	/**
	 * Helper to strip {@code f|F} boundary markers from the argument string and
	 * store the result on the instance.
	 */
	protected String stripBoundary(String spec) {
		if (spec == null || spec.isEmpty())
			return "";
		String working = spec;
		boolean fixed = false;
		if (Type.isFixedBoundaryToken(working.charAt(0))) {
			fixed = true;
			working = working.substring(1);
		}
		if (!working.isEmpty() && Type.isFixedBoundaryToken(working.charAt(working.length() - 1))) {
			fixed = true;
			working = working.substring(0, working.length() - 1);
		}
		this.fixedBoundary = fixed;
		return working;
	}

	@Override
	public AbstractLattice clone() {
		AbstractLattice clone = (AbstractLattice) super.clone();
		clone.fixedBoundary = fixedBoundary;
		return clone;
	}
}
