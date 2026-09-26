//
// EvoLudo Project
//
// Copyright 2010-2026 Christoph Hauert
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

import org.evoludo.math.RNGDistribution;
import org.evoludo.simulator.EvoLudo;
import org.evoludo.util.CLOParser;

/**
 * Linear (1D lattice) geometry that supports asymmetric neighbourhoods and
 * optional fixed boundaries.
 */
public class LinearGeometry extends AbstractLattice {

	/**
	 * Difference between the number of neighbours on the left vs right.
	 */
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
			engine.logWarning("too many arguments for linear geometry.");
		double k;
		if (conn.length >= 2) {
			k = Math.max(2, conn[0] + conn[1]);
			setLinearAsymmetry(conn[0] - conn[1]);
		} else if (conn.length == 1) {
			k = Math.max(2, conn[0]);
			setLinearAsymmetry(0);
		} else {
			// keep the previously configured spatial neighbour count
			k = Math.max(2, connectivity > 0 ? degree() : 2);
			setLinearAsymmetry(0);
		}
		// interspecies geometries add a self-loop on top of the spatial neighbours
		// (see #init()); connectivity must reflect the total node degree for the
		// shared buffer-sizing logic in AbstractGeometry#alloc()
		connectivity = (isInterspecies() && k > 1.0 + RNGDistribution.EPS) ? k + 1 : k;
		return true;
	}

	/**
	 * Recover the requested number of spatial (left+right) neighbours from
	 * {@link #connectivity}, undoing the extra self-loop that interspecies
	 * geometries add on top (see {@link #init()}). Connectivity {@code 1} already
	 * means "self only" and is left untouched.
	 *
	 * @return the spatial neighbour count
	 */
	private double degree() {
		return (isInterspecies() && connectivity > 1.0 + RNGDistribution.EPS) ? connectivity - 1 : connectivity;
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

		int neighbors = (int) (degree() + 0.5);
		int left = (neighbors + linearAsymmetry) / 2;
		int right = (neighbors - linearAsymmetry) / 2;
		isUndirected = (left == right);
		clear();

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
	 * Get the current left-right neighbour difference.
	 * 
	 * @return the left-right neighbour difference
	 */
	public int getLinearAsymmetry() {
		return linearAsymmetry;
	}

	@Override
	protected boolean checkSettings() {
		boolean doReset = false;
		boolean interspecies = isInterspecies();
		double k = Math.max(1, Math.rint(degree()));
		boolean invalidConn = (Math.abs(1.0 - k) < RNGDistribution.EPS && (!interspecies && linearAsymmetry == 0))
				|| ((int) (k + 0.5) % 2 == 1 && linearAsymmetry == 0) || k >= size;
		if (invalidConn) {
			double size1 = size - 1.0;
			double newConn = Math.min(Math.max(2, k + 1), size1 - size1 % 2);
			k = newConn;
			warn("requires even integer number of neighbors - using " + (int) k + "!");
			doReset = true;
		}
		// interspecies geometries add a self-loop on top of the spatial neighbours,
		// unless spatial==1 already means "self only" (see #init())
		connectivity = (interspecies && k > 1.0 + RNGDistribution.EPS) ? k + 1 : k;
		if (pRewire > 0.0 && connectivity < 2.0 + 1.0 / size) {
			warn("cannot rewire links for '" + type + "' - ignored!");
			pRewire = 0.0;
		}
		return doReset;
	}

	@Override
	protected LinearGeometry cloneConfig() {
		LinearGeometry clone = (LinearGeometry) super.cloneConfig();
		clone.linearAsymmetry = linearAsymmetry;
		return clone;
	}

	@Override
	protected boolean equalParameters(AbstractGeometry other) {
		LinearGeometry linear = (LinearGeometry) other;
		return Double.compare(linearAsymmetry, linear.linearAsymmetry) == 0;
	}

	@Override
	protected int hashParameters() {
		return Integer.hashCode(linearAsymmetry);
	}
}
