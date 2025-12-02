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
import org.evoludo.util.CLOParser;

/**
 * Linear (1D lattice) geometry that supports asymmetric neighbourhoods and
 * optional fixed boundaries.
 */
public class LinearGeometry extends AbstractLattice {

	private int linearAsymmetry = 0;

	/**
	 * Create a linear (1D lattice) geometry attached to the given engine.
	 *
	 * @param engine EvoLudo pacemaker
	 */
	public LinearGeometry(EvoLudo engine) {
		super(engine);
		setType(GeometryType.LINEAR);
	}

	@Override
	public boolean parse(String arg) {
		String numeric = stripBoundary(arg);
		int[] conn = CLOParser.parseIntVector(numeric);
		if (conn.length > 2)
			logger.warning("too many arguments for linear geometry.");
		if (conn.length >= 2) {
			connectivity = Math.max(2, conn[0] + conn[1]);
			setLinearAsymmetry(conn[0] - conn[1]);
		} else if (conn.length == 1) {
			connectivity = Math.max(2, conn[0]);
			setLinearAsymmetry(0);
		} else {
			int defaultConn = connectivity > 0 ? (int) connectivity : 2;
			connectivity = Math.max(2, defaultConn);
			setLinearAsymmetry(0);
		}
		return true;
	}

	/**
	 * Generates a linear chain (1D lattice). With asymmetric neighbours and fixed
	 * boundaries this represents the simplest suppressor of selection.
	 *
	 * <h3>Requirements/notes:</h3>
	 * <ol>
	 * <li>Supports different numbers of neighbours to the left and right.
	 * <li>For inter-species interactions connectivities are incremented by one and
	 * a connectivity of {@code 1} is admissible.
	 * <li>Boundaries can be fixed or periodic (default).
	 * </ol>
	 */
	@Override
	public void init() {
		if (size <= 0)
			throw new IllegalStateException("size must be set before initializing a linear geometry");
		isRewired = false;
		isRegular = !fixedBoundary;
		boolean interspecies = isInterspecies();

		int neighbors = (int) (connectivity + 0.5);
		int left = (neighbors + linearAsymmetry) / 2;
		int right = (neighbors - linearAsymmetry) / 2;
		isUndirected = (left == right);

		for (int i = 0; i < size; i++) {
			for (int j = -left; j <= right; j++) {
				if ((j == 0 && !interspecies) || (fixedBoundary && (i + j < 0 || i + j >= size)))
					continue;
				addLinkAt(i, (i + j + size) % size);
			}
		}
		isValid = true;
	}

	/**
	 * Set the left-right neighbour difference.
	 *
	 * @param asymmetry positive values favour neighbours on the left
	 */
	public void setLinearAsymmetry(int asymmetry) {
		this.linearAsymmetry = asymmetry;
	}

	/**
	 * @return the left-right neighbour difference
	 */
	public int getLinearAsymmetry() {
		return linearAsymmetry;
	}

	@Override
	protected boolean checkSettings() {
		boolean doReset = false;
		connectivity = Math.max(1, Math.rint(connectivity));
		boolean invalidConn = (Math.abs(1.0 - connectivity) < 1e-8 && (!isInterspecies() && linearAsymmetry == 0))
				|| ((int) (connectivity + 0.5) % 2 == 1 && linearAsymmetry == 0) || connectivity >= size;
		if (invalidConn) {
			double size1 = size - 1.0;
			double newConn = Math.min(Math.max(2, connectivity + 1), size1 - size1 % 2);
			connectivity = newConn;
			warn("requires even integer number of neighbors - using " + (int) connectivity + "!");
			doReset = true;
		}
		if (pRewire > 0.0 && connectivity < 2.0 + 1.0 / size) {
			warn("cannot rewire links for '" + type + "' - ignored!");
			pRewire = 0.0;
		}
		return doReset;
	}

	@Override
	public LinearGeometry clone() {
		LinearGeometry clone = (LinearGeometry) super.clone();
		clone.linearAsymmetry = linearAsymmetry;
		return clone;
	}

	@Override
	public int hashCode() {
		int result = super.hashCode();
		result = 31 * result + Integer.hashCode(linearAsymmetry);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		LinearGeometry other = (LinearGeometry) obj;
		return Double.compare(linearAsymmetry, other.linearAsymmetry) == 0;
	}
}
