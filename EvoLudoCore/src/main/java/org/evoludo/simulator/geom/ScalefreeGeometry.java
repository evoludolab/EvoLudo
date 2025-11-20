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

import java.util.Arrays;

import org.evoludo.math.RNGDistribution;
import org.evoludo.simulator.EvoLudo;
import org.evoludo.simulator.modules.Module;
import org.evoludo.util.CLOParser;
import org.evoludo.util.Formatter;

/**
 * Scale-free network that samples a power-law degree distribution and then
 * constructs a matching undirected graph.
 */
public class ScalefreeGeometry extends AbstractNetwork {

	private double sfExponent = -2.0;

	public ScalefreeGeometry(EvoLudo engine) {
		super(engine);
		setType(Type.SCALEFREE);
	}

	public ScalefreeGeometry(EvoLudo engine, Module<?> module) {
		super(engine, module);
		setType(Type.SCALEFREE);
	}

	public ScalefreeGeometry(EvoLudo engine, Module<?> popModule, Module<?> oppModule) {
		super(engine, popModule, oppModule);
		setType(Type.SCALEFREE);
	}

	public boolean parse(String arg) {
		double[] values = CLOParser.parseVector(arg);
		if (values.length == 0) {
			connectivity = Math.max(2, (int) Math.round(connectivity > 0 ? connectivity : 2.0));
			sfExponent = -2.0;
			warn("requires connectivity argument - using " + (int) connectivity + " and exponent -2.");
			return true;
		}
		connectivity = Math.max(2, (int) values[0]);
		sfExponent = values.length >= 2 ? values[1] : -2.0;
		if (sfExponent >= 0.0) {
			warn("exponent for scale-free graph is " + Formatter.format(sfExponent, 2)
					+ " but must be < 0 - using -2.");
			sfExponent = -2.0;
		}
		return true;
	}

	@Override
	public void init() {
		if (size <= 0)
			throw new IllegalStateException("size must be set before initializing a scale-free geometry");
		if (size == 1) {
			alloc();
			isValid = true;
			return;
		}
		isRewired = false;
		isUndirected = true;
		isRegular = false;

		double[] distr = new double[size];
		RNGDistribution rng = engine.getRNG();

		if (Math.abs(sfExponent) > 1e-8) {
			for (int n = 0; n < size; n++)
				distr[n] = Math.pow((double) n / (double) size, sfExponent);
		} else {
			int max = (int) (2.0 * connectivity + 0.5);
			for (int n = 0; n <= max && n < size; n++)
				distr[n] = 1.0;
			for (int n = max + 1; n < size; n++)
				distr[n] = 0.0;
		}

		double norm = 0.0;
		double conn = 0.0;
		for (int n = 1; n < size; n++) {
			norm += distr[n];
			conn += n * distr[n];
		}
		conn /= norm;
		for (int n = 0; n < size; n++)
			distr[n] /= norm;

		if (conn < connectivity) {
			double max = size / 2.0;
			double x = 1.0 - (connectivity - conn) / (max - conn);
			double lift = (1.0 - x) / (size - 1);
			for (int n = 1; n < size; n++)
				distr[n] = x * distr[n] + lift;
		} else {
			double km = 0.0, pm = 0.0;
			int m = 1;
			double sump = distr[1], sumpi = sump;
			while (km < connectivity && m < size - 1) {
				m++;
				pm = distr[m];
				sump += pm;
				sumpi += pm * m;
				km = (sumpi - pm * ((m * (m + 1)) / 2.0)) / (sump - m * pm);
			}
			for (int n = m; n < size; n++)
				distr[n] = 0.0;
			double decr = distr[m - 1];
			double newnorm = sump - pm - (m - 1) * decr;
			conn = 0.0;
			for (int n = 1; n < m; n++) {
				distr[n] = (distr[n] - decr) / newnorm;
				conn += distr[n] * n;
			}
			double x = 1.0 - (connectivity - conn) / (m / 2.0 - conn);
			double lift = (1.0 - x) / (m - 1);
			for (int n = 1; n < m; n++)
				distr[n] = x * distr[n] + lift;
		}

		int[] degrees = new int[size];

		while (true) {
			int links = 0;
			int leaflinks = 0;
			int nonleaflinks = 0;
			for (int n = 0; n < size; n++) {
				double hit = rng.random01();
				for (int i = 1; i < size - 1; i++) {
					hit -= distr[i];
					if (hit <= 0.0) {
						degrees[n] = i;
						links += i;
						if (i > 1)
							nonleaflinks += i;
						else
							leaflinks++;
						break;
					}
				}
			}

			int adj = 0;
			if (connectivity > 0.0) {
				if (connectivity * size > links)
					adj = (int) Math.floor(connectivity * size + 0.5) - links;
				if (Math.max(2.0, connectivity) * size < links)
					adj = (int) Math.floor(Math.max(2.0, connectivity) * size + 0.5) - links;
				if ((links + adj) % 2 == 1)
					adj++;
			}
			if (adj != 0 && logger.isLoggable(java.util.logging.Level.WARNING))
				logger.warning("adjusting link count: " + links + " by " + adj + " to achieve "
						+ (int) Math.floor(connectivity * size + 0.5));

			while (adj != 0) {
				int node = rng.random0n(size);
				int oldDegree = degrees[node];
				int newDegree = sampleDegree(rng, distr);
				int delta = newDegree - oldDegree;
				if (Math.abs(adj) <= Math.abs(adj - delta))
					continue;
				degrees[node] = newDegree;
				if (oldDegree == 1 && newDegree != 1) {
					leaflinks--;
					nonleaflinks += newDegree;
				}
				if (oldDegree != 1 && newDegree == 1) {
					leaflinks -= oldDegree;
					nonleaflinks--;
				}
				links += delta;
				adj -= delta;
				if (logger.isLoggable(java.util.logging.Level.WARNING)) {
					logger.warning("links: " + links + ", goal: "
							+ (int) Math.floor(Math.max(2.0, connectivity) * size + 0.5) + ", change: " + delta
							+ ", remaining: " + adj);
				}
			}

			while ((nonleaflinks < 2 * (size - 1) - leaflinks) || (links % 2 == 1)) {
				int node = rng.random0n(size);
				if (degrees[node]++ == 1) {
					leaflinks--;
					nonleaflinks++;
				}
				nonleaflinks++;
				links++;
			}

			Arrays.sort(degrees);
			for (int n = 0; n < size / 2; n++)
				swap(degrees, n, size - n - 1);

			for (int attempt = 0; attempt < 10; attempt++) {
				if (initGeometryDegreeDistr(degrees)) {
					isValid = true;
					return;
				}
			}
		}
	}

	private int sampleDegree(RNGDistribution rng, double[] distr) {
		double hit = rng.random01();
		for (int i = 1; i < size - 1; i++) {
			hit -= distr[i];
			if (hit <= 0.0)
				return i;
		}
		return 1;
	}

	private static void swap(int[] array, int a, int b) {
		int tmp = array[a];
		array[a] = array[b];
		array[b] = tmp;
	}

	@Override
	protected boolean checkSettings() {
		boolean reset = false;
		if (sfExponent >= 0.0) {
			warn("exponent for scale-free graph must be negative - using default -2.");
			sfExponent = -2.0;
		}
		if (size > 0) {
			double maxConn = size * 0.5;
			if (connectivity > maxConn) {
				warn("Requested connectivity " + Formatter.format(connectivity, 2) + " too high. Reduced to "
						+ Formatter.format(maxConn, 2) + ".");
				connectivity = maxConn;
				reset = true;
			}
		}
		return reset;
	}

	@Override
	public ScalefreeGeometry clone() {
		ScalefreeGeometry clone = (ScalefreeGeometry) super.clone();
		clone.sfExponent = sfExponent;
		return clone;
	}
}
