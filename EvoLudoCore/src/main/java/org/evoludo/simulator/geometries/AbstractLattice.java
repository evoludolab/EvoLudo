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

package org.evoludo.simulator.geometries;

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

	/**
	 * Report whether the lattice uses fixed boundaries instead of periodic ones.
	 * 
	 * @return {@code true} if the lattice uses fixed boundaries instead of periodic
	 *         ones.
	 */
	public boolean isFixedBoundary() {
		return fixedBoundary;
	}

	/**
	 * Set whether the lattice uses fixed boundaries.
	 * 
	 * @param fixedBoundary {@code true} for fixed boundary conditions
	 */
	public void setFixedBoundary(boolean fixedBoundary) {
		this.fixedBoundary = fixedBoundary;
	}

	/**
	 * Helper to strip {@code f|F} boundary markers from the argument string and
	 * store the result on the instance.
	 * 
	 * @param spec CLI fragment describing the geometry
	 * @return the cleaned specification without boundary decorators
	 */
	protected String stripBoundary(String spec) {
		if (spec == null || spec.isEmpty())
			return "";
		String working = spec;
		boolean fixed = false;
		if (GeometryType.isFixedBoundaryToken(working.charAt(0))) {
			fixed = true;
			working = working.substring(1);
		}
		if (!working.isEmpty() && GeometryType.isFixedBoundaryToken(working.charAt(working.length() - 1))) {
			fixed = true;
			working = working.substring(0, working.length() - 1);
		}
		this.fixedBoundary = fixed;
		return working;
	}

	/**
	 * Enforce square lattice size and optionally even side lengths.
	 *
	 * @param requireEvenSide {@code true} if the side length must be even
	 * @return {@code true} if the size had to be adjusted
	 */
	protected boolean ensureSquareSize(boolean requireEvenSide) {
		boolean doReset = false;
		int side = (int) Math.floor(Math.sqrt(size) + 0.5);
		if (requireEvenSide)
			side = (side + 1) / 2 * 2;
		int side2 = side * side;
		if (setSize(side2)) {
			if (engine.getModule().cloNPopulation.isSet()) {
				String parity = requireEvenSide ? "even integer " : "integer ";
				warn("requires " + parity + "square size - using " + size + "!");
			}
			doReset = true;
		}
		return doReset;
	}

	@Override
	public AbstractLattice clone() {
		AbstractLattice clone = (AbstractLattice) super.clone();
		clone.fixedBoundary = fixedBoundary;
		return clone;
	}

	@Override
	public int hashCode() {
		int result = super.hashCode();
		result = 31 * result + Boolean.hashCode(fixedBoundary);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		AbstractLattice other = (AbstractLattice) obj;
		return fixedBoundary == other.fixedBoundary;
	}
}
