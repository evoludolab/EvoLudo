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

package org.evoludo.simulator.models;

import org.evoludo.math.ArrayMath;
import org.evoludo.simulator.EvoLudo;
import org.evoludo.simulator.Geometry;
import org.evoludo.util.CLOption;

/**
 * Helper class responsible for initializing spatial density fields for PDE
 * based simulations in the EvoLudo framework.
 * <p>
 * PDEInitialize encapsulates a particular initialization "type" (uniform,
 * random, localized shapes, Gaussian, ring, etc.), a background density vector,
 * an optional dependent-component index and an RNG for randomized initial
 * configurations. It provides a single entry point {@code init(double[][],
 * double[], Geometry)} that fills a preallocated two‑dimensional density
 * array where each row corresponds to the state vector at a lattice site.
 * </p>
 *
 * <h3>Concepts and inputs</h3>
 * <ul>
 * <li><b>density</b> - destination array of shape [space.getSize()][nDim]; each
 * row receives the initial density vector for a spatial location.</li>
 * <li><b>y0</b> - baseline initial state vector (peak or default densities)
 * used for the selected initialization pattern.</li>
 * <li><b>background</b> - background density vector used for locations
 * outside the initialized region (e.g. outside a circle or square).</li>
 * <li><b>dependent</b> - index of a dependent component (or {@code -1} if
 * none). When set, several initialization modes perform a correction
 * and normalization so the dependent component and the total density are
 * consistent (e.g. for frequency-based models).</li>
 * <li><b>rng</b> - random number generator used for RANDOM
 * initializations.</li>
 * </ul>
 *
 * <h3>Supported initialization types</h3>
 * <p>
 * The class supports the following patterns via the inner {@link Type} enum:
 * UNIFORM, RANDOM, PERTURBATION, SQUARE, CIRCLE, GAUSSIAN (sombrero) and RING.
 * The chosen type determines how {@code y0} and {@code background} are applied
 * across the lattice: globally, randomly, locally (centered square/circle),
 * or using radially/gaussian-scaled profiles.
 * </p>
 *
 * <h3>Usage</h3>
 * <p>
 * Construct an instance with the desired {@link Type}, background vector,
 * dependent index and RNG (if needed), then call {@link #init(double[][],
 * double[], Geometry)} to populate the simulation density array prior to
 * starting integration or discrete updates.
 * </p>
 *
 * @see Geometry
 * @see Type
 * @see #init(double[][], double[], Geometry)
 */
class PDEInitialize extends ODEInitialize {

	private final PDE pde;

	PDEInitialize(PDE pde) {
		super(pde);
		this.pde = pde;
	}

	public void init(double[][] density) {
		Geometry space = pde.space;
		double[] y0 = new double[pde.nDim];
		System.arraycopy(pde.y0, 0, y0, 0, pde.nDim);

		switch (pde.initType) {
			default:
			case UNIFORM:
				initUniform(density, y0, space);
				break;
			case PERTURBATION:
				initPerturbation(density, y0, space);
				break;
			case RANDOM:
				initRandom(density, y0, space);
				break;
			case CIRCLE:
			case SQUARE:
			case GAUSSIAN:
			case RING:
				initFunction(density, y0, space);
				break;
		}
	}

	private void initUniform(double[][] density, double[] y0, Geometry space) {
		int nDim = y0.length;
		int nodeCount = space.getSize();
		for (int n = 0; n < nodeCount; n++)
			System.arraycopy(y0, 0, density[n], 0, nDim);
	}

	private void initPerturbation(double[][] density, double[] y0, Geometry space) {
		int nDim = y0.length;
		int nodeCount = space.getSize();
		for (int n = 0; n < nodeCount; n++)
			System.arraycopy(pde.background, 0, density[n], 0, nDim);
		switch (space.getType()) {
			case CUBE: {
				int l = (int) (Math.pow(space.getSize(), 1.0 / 3.0) + 0.5);
				System.arraycopy(y0, 0, density[(l * l + l + 1) * l / 2], 0, nDim);
				break;
			}
			case SQUARE_NEUMANN:
			case SQUARE_MOORE:
			case SQUARE:
			case TRIANGULAR:
			case HEXAGONAL: {
				int l = (int) (Math.sqrt(space.getSize()) + 0.5);
				System.arraycopy(y0, 0, density[(l + 1) * l / 2], 0, nDim);
				break;
			}
			default:
				System.arraycopy(y0, 0, density[space.getSize() / 2], 0, nDim);
		}
	}

	private void initRandom(double[][] density, double[] y0, Geometry space) {
		int nDim = y0.length;
		int nodeCount = space.getSize();
		for (int n = 0; n < nodeCount; n++) {
			double[] ds = density[n]; // ds is only a short-cut - data written to density[]
			for (int i = 0; i < nDim; i++)
				ds[i] = pde.rng.random01() * y0[i];
			if (pde.dependent >= 0)
				ArrayMath.normalize(ds);
		}
	}

	private void initFunction(double[][] density, double[] y0, Geometry space) {
		switch (space.getType()) {
			case CUBE:
				initFunction3D(density, y0, space);
				break;
			case LINEAR:
				initFunction1D(density, y0, space);
				break;
			default: // for square, triangular and hexagonal lattices
				initFunction2D(density, y0, space);
		}
	}

	/**
	 * Initialization for CUBE geometry extracted to reduce cognitive complexity.
	 */
	private void initFunction3D(double[][] density, double[] y0, Geometry space) {
		int l = 50;
		int lz = 10;
		if (space.getSize() != 25000) { // not NOVA dimensions
			l = (int) (Math.pow(space.getSize(), 1.0 / 3.0) + 0.5);
			lz = l;
		}
		for (int z = 0; z < lz; z++)
			for (int y = 0; y < l; y++)
				for (int x = 0; x < l; x++)
					apply(x, y, z, l, lz, y0, density);
	}

	/**
	 * Initialization for LINEAR geometry extracted to reduce cognitive complexity.
	 */
	private void initFunction1D(double[][] density, double[] y0, Geometry space) {
		int nodeCount = space.getSize();
		for (int x = 0; x < nodeCount; x++)
			apply(x, nodeCount, y0, density);
	}

	/**
	 * Initialization for square/triangular/hexagonal lattice geometries extracted
	 * to reduce cognitive complexity.
	 */
	private void initFunction2D(double[][] density, double[] y0, Geometry space) {
		int l = (int) (Math.sqrt(space.getSize()) + 0.5);
		for (int y = 0; y < l; y++)
			for (int x = 0; x < l; x++)
				apply(x, y, l, y0, density);
	}

	/**
	 * Applies the initialization for {@link Geometry.Type#LINEAR}.
	 * 
	 * @param x    the x coordinate
	 * @param y0   the initial state
	 * @param dest the destination array
	 */
	void apply(int x, int l, double[] y0, double[][] dest) {
		switch (pde.initType) {
			case CIRCLE:
			case SQUARE:
				int m = l / 2;
				int l10 = l / 10;
				if (x < m - l10 || x > m + l10)
					y0 = pde.background;
				System.arraycopy(y0, 0, dest[x], 0, y0.length);
				break;
			case GAUSSIAN:
				m = (l - 1) / 2;
				double norm = 1.0 / l;
				scaleDensity(y0, Math.exp(-((x - m) * (x - m)) * norm), dest[x]);
				break;
			case RING:
				m = (l - 1) / 2;
				double m3 = m * 0.333;
				norm = 1.0 / l;
				double r = Math.abs(x - m);
				scaleDensity(y0, Math.exp(-(r - m3) * (r - m3) * norm), dest[x]);
				break;
			default:
		}
	}

	/**
	 * Applies the initialization for {@link Geometry.Type#SQUARE} and variants.
	 * 
	 * @param x    the x coordinate
	 * @param y    the y coordinate
	 * @param l    the linear length of the lattice
	 * @param y0   the initial state
	 * @param dest the destination array
	 */
	void apply(int x, int y, int l, double[] y0, double[][] dest) {
		switch (pde.initType) {
			case CIRCLE:
				int m = l / 2;
				int r2 = Math.max(1, l * l / 100); // (l/10)^2
				if ((x - m) * (x - m) + (y - m) * (y - m) > r2)
					y0 = pde.background;
				System.arraycopy(y0, 0, dest[y * l + x], 0, y0.length);
				break;
			case SQUARE:
				m = l / 2;
				int l10 = l / 10;
				if (x < m - l10 || x > m + l10 || y < m - l10 || y > m + l10)
					y0 = pde.background;
				System.arraycopy(y0, 0, dest[y * l + x], 0, y0.length);
				break;
			case GAUSSIAN:
				m = (l - 1) / 2;
				double norm = 1.0 / l;
				scaleDensity(y0, Math.exp(-((x - m) * (x - m) + (y - m) * (y - m)) * norm),
						dest[y * l + x]);
				break;
			case RING:
				m = (l - 1) / 2;
				double m3 = m * 0.333;
				norm = 1.0 / l;
				double r = Math.sqrt((double) (x - m) * (x - m) + (y - m) * (y - m));
				scaleDensity(y0, Math.exp(-(r - m3) * (r - m3) * norm), dest[y * l + x]);
				break;
			default:
		}
	}

	/**
	 * Applies the initialization for {@link Geometry.Type#CUBE}.
	 * 
	 * @param x    the x coordinate
	 * @param y    the y coordinate
	 * @param z    the z coordinate
	 * @param l    the linear length of the lattice
	 * @param lz   the linear height of the lattice ({@code lz == l} except for
	 *             NOVA)
	 * @param y0   the initial state
	 * @param dest the destination array
	 */
	void apply(int x, int y, int z, int l, int lz, double[] y0, double[][] dest) {
		switch (pde.initType) {
			case CIRCLE:
				int m = l / 2;
				double r3 = l * l * l * 0.001; // (l/10)^3
				if ((x - m) * (x - m) + (y - m) * (y - m) + (z - m) * (z - m) > r3)
					y0 = pde.background;
				System.arraycopy(y0, 0, dest[(z * l + y) * l + x], 0, y0.length);
				break;
			case SQUARE:
				m = l / 2;
				int l10 = l / 10;
				int lz10 = lz / 10;
				if (x < m - l10 || x > m + l10 || y < m - l10 || y > m + l10 || z < m - lz10 || z > m + lz10)
					y0 = pde.background;
				System.arraycopy(y0, 0, dest[(z * l + y) * l + x], 0, y0.length);
				break;
			case GAUSSIAN:
				m = (l - 1) / 2;
				int mz = (lz - 1) / 2;
				double norm = 1.0 / l;
				scaleDensity(y0, Math.exp(-((x - m) * (x - m) + (y - m) * (y - m) + (z - mz) * (z - mz)) * norm),
						dest[(z * l + y) * l + x]);
				break;
			case RING:
				m = (l - 1) / 2;
				double m3 = m * 0.333;
				mz = (lz - 1) / 2;
				norm = 1.0 / l;
				double r = Math.pow((double) (x - m) * (x - m) + (y - m) * (y - m) + (z - mz) * (z - mz),
						1.0 / 3.0);
				scaleDensity(y0, Math.exp(-(r - m3) * (r - m3) * norm), dest[(z * l + y) * l + x]);
				break;
			default:
		}
	}

	/**
	 * Helper method to scale the density vector {@code d} by the scalar factor
	 * {@code scale}. The scalar must lie in \((0, 1)\) such that the initial
	 * densities/frequencies represent the maximum.
	 * 
	 * @param y0    the initial density vector
	 * @param scale the scaling factor
	 * @param d     the location to store the scaled density
	 */
	private void scaleDensity(double[] y0, double scale, double[] d) {
		for (int n = 0; n < y0.length; n++)
			d[n] = (1.0 - scale) * pde.background[n] + scale * y0[n];
		if (pde.dependent >= 0) {
			d[pde.dependent] = Math.max(0.0, 1.0 + d[pde.dependent] - ArrayMath.norm(d));
			ArrayMath.normalize(d);
		}
	}

	/**
	 * Types of initial configurations. Currently this model supports the following
	 * density distributions:
	 * <dl>
	 * <dt>UNIFORM
	 * <dd>Uniform/homogeneous distribution of trait densities given by
	 * {@link ODE#y0}.
	 * <dt>RANDOM
	 * <dd>Random trait densities, uniformly distributed between zero and the
	 * densities given by {@link ODE#y0}.
	 * <dt>SQUARE
	 * <dd>Square in the center with uniform densities given by {@link ODE#y0}.
	 * <dt>CIRCLE
	 * <dd>Circle in the center with uniform densities given by {@link ODE#y0}.
	 * <dt>DISTURBANCE
	 * <dd>Spatially homogeneous distribution given by {@link ODE#y0} with a
	 * perturbation in the center cell with densities {@code 1.2*y0}, or, for
	 * frequency
	 * based models with inverted and normalized frequencies.
	 * <dt>GAUSSIAN
	 * <dd>Gaussian density distribution in the center. In 2D lattices this
	 * generates a sombrero-like distribution. Maximum density is given by
	 * {@link ODE#y0}.
	 * <dt>GAUSSIAN_RING
	 * <dd>Ring distribution in the center with Gaussian distributed densities along
	 * the radius. In 2D lattices this generates a donut-like distribution. Maximum
	 * density is given by {@link ODE#y0}.
	 * <dt>DEFAULT
	 * <dd>Default initialization (UNIFORM)
	 * </dl>
	 * 
	 * @see #parse(String)
	 * @see ODE#cloInit
	 */
	public enum Type implements CLOption.Key {

		/**
		 * Uniform/homogeneous distribution of trait densities {@code <d1,...dn>}.
		 */
		UNIFORM("uniform", "uniform densities <d1,...,dn>"),

		/**
		 * Random trait frequencies.
		 */
		RANDOM("random", "random densities"),

		/**
		 * Square in the center with uniform trait densities {@code <d1,...dn>}. This
		 * requires a lattice geometry. In modules with empty space the background
		 * defaults to empty, otherwise the background densities <em>must</em> be
		 * specified as {@code <b1,...bn>}.
		 */
		SQUARE("square", "square in center <d1,...,dn[;b1,...,bn]>"),

		/**
		 * Circle in the center with uniform densities {@code <d1,...dn>}. This requires
		 * a lattice geometry. In modules with empty space the background defaults to
		 * empty, otherwise the background densities <em>must</em> be specified as
		 * {@code <b1,...bn>}.
		 */
		CIRCLE("circle", "circle in center <d1,...,dn[;b1,...,bn]>"),

		/**
		 * Perturbation of a spatially homogeneous distribution with densities
		 * {@code <d1,...dn>}. In modules with empty space the background defaults to
		 * empty, otherwise the background densities <em>must</em> be specified as
		 * {@code <b1,...bn>}.
		 */
		PERTURBATION("perturbation", "perturbation in center <d1,...,dn[;b1,...,bn]>"),

		/**
		 * Gaussian density distribution in the center. This requires a lattice
		 * geometry. In 2D lattices this generates a sombrero-like distribution. The
		 * peak density is {@code <d1,...dn>}. In modules with empty space the
		 * background defaults to empty, otherwise the background densities
		 * <em>must</em> be specified as {@code <b1,...bn>}.
		 */
		GAUSSIAN("sombrero", "sombrero-like distribution <d1,...,dn[;b1,...,bn]>"),

		/**
		 * Ring distribution in the center with Gaussian distributed densities along the
		 * radius. This requires a lattice geometry. In 2D lattices this generates a
		 * donut-like distribution. The peak density is {@code <d1,...dn>}. In modules
		 * with empty space the background defaults to empty, otherwise the background
		 * densities <em>must</em> be specified as {@code <b1,...bn>}.
		 */
		RING("ring", "donut-like distribution <d1,...,dn[;b1,...,bn]>");

		/**
		 * Key of initialization type. Used when parsing command line options.
		 * 
		 * @see ODE#cloInit
		 */
		String key;

		/**
		 * Brief description of initialization type for help display.
		 * 
		 * @see EvoLudo#getCLOHelp()
		 */
		String title;

		/**
		 * Instantiate new initialization type.
		 * 
		 * @param key   identifier for parsing of command line option
		 * @param title summary of geometry
		 */
		Type(String key, String title) {
			this.key = key;
			this.title = title;
		}

		@Override
		public String getKey() {
			return key;
		}

		@Override
		public String getTitle() {
			return title;
		}

		@Override
		public String toString() {
			return key;
		}
	}
}
