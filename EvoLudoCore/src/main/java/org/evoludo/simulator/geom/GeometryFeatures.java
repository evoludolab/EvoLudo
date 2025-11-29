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

import java.util.Objects;

/**
 * Encapsulates frequently used geometry statistics such as minimal or maximal
 * degrees.
 */
public class GeometryFeatures {

	/**
	 * The minimum number of incoming links.
	 */
	public final int minIn;

	/**
	 * The maximum number of incoming links.
	 */
	public final int maxIn;

	/**
	 * The average number of incoming links.
	 */
	public final double avgIn;

	/**
	 * The minimum number of outgoing links.
	 */
	public final int minOut;

	/**
	 * The maximum number of outgoing links.
	 */
	public final int maxOut;

	/**
	 * The average number of outgoing links.
	 */
	public final double avgOut;

	/**
	 * The minimum sum of incoming and outgoing links.
	 */
	public final int minTot;

	/**
	 * The maximum sum of incoming and outgoing links.
	 */
	public final int maxTot;

	/**
	 * The average sum of incoming and outgoing links.
	 */
	public final double avgTot;

	/**
	 * Evaluate geometry features for the given geometry.
	 * 
	 * @param geometry the geometry to evaluate
	 */
	public GeometryFeatures(AbstractGeometry geometry) {
		if (geometry.isType(GeometryType.WELLMIXED)) {
			minIn = 0;
			maxIn = 0;
			avgIn = 0.0;
			minOut = 0;
			maxOut = 0;
			avgOut = 0.0;
			minTot = 0;
			maxTot = 0;
			avgTot = 0.0;
			return;
		}
		int maxout = -1;
		int maxin = -1;
		int maxtot = -1;
		int minout = Integer.MAX_VALUE;
		int minin = Integer.MAX_VALUE;
		int mintot = Integer.MAX_VALUE;
		double sumin = 0.0;
		double sumout = 0.0;
		double sumtot = 0.0;
		int size = geometry.size;
		for (int n = 0; n < size; n++) {
			int lout = geometry.kout[n];
			maxout = Math.max(maxout, lout);
			minout = Math.min(minout, lout);
			sumout += lout;
			int lin = geometry.kin[n];
			maxin = Math.max(maxin, lin);
			minin = Math.min(minin, lin);
			sumin += lin;
			int ltot = lout + lin;
			maxtot = Math.max(maxtot, ltot);
			mintot = Math.min(mintot, ltot);
			sumtot += ltot;
		}
		minIn = minin;
		maxIn = maxin;
		minOut = minout;
		maxOut = maxout;
		minTot = mintot;
		maxTot = maxtot;
		double isize = 1.0 / size;
		avgOut = sumout * isize;
		avgIn = sumin * isize;
		avgTot = sumtot * isize;
	}

	public GeometryFeatures(GeometryFeatures other) {
		this.minIn = other.minIn;
		this.maxIn = other.maxIn;
		this.avgIn = other.avgIn;
		this.minOut = other.minOut;
		this.maxOut = other.maxOut;
		this.avgOut = other.avgOut;
		this.minTot = other.minTot;
		this.maxTot = other.maxTot;
		this.avgTot = other.avgTot;
	}

	@Override
	public int hashCode() {
		return Objects.hash(minIn, maxIn, avgIn, minOut, maxOut, avgOut, minTot, maxTot, avgTot);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null || getClass() != obj.getClass())
			return false;
		GeometryFeatures other = (GeometryFeatures) obj;
		return minIn == other.minIn && maxIn == other.maxIn
				&& Double.doubleToLongBits(avgIn) == Double.doubleToLongBits(other.avgIn) && minOut == other.minOut
				&& maxOut == other.maxOut && Double.doubleToLongBits(avgOut) == Double.doubleToLongBits(other.avgOut)
				&& minTot == other.minTot && maxTot == other.maxTot
				&& Double.doubleToLongBits(avgTot) == Double.doubleToLongBits(other.avgTot);
	}
}
