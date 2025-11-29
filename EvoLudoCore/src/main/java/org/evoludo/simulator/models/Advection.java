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

import java.util.Arrays;
import java.util.logging.Level;

import org.evoludo.math.ArrayMath;
import org.evoludo.simulator.EvoLudo;
import org.evoludo.simulator.modules.Module;
import org.evoludo.util.CLODelegate;
import org.evoludo.util.CLOParser;
import org.evoludo.util.CLOption;
import org.evoludo.util.CLOCategory;
import org.evoludo.util.Formatter;
import org.evoludo.simulator.geom.GeometryFeatures;

/**
 * Numerical integration of partial differential equations for
 * reaction-diffusion-advection systems based on Euler's method (fixed step
 * size).
 * <p>
 * <strong>Important:</strong> Currently multi-species modules are not supported
 * by PDE models.
 *
 * @author Christoph Hauert
 */
public class Advection extends PDE {

	/**
	 * The 2D array of advection coefficients for each trait against all others.
	 *
	 * @see #cloPdeAdvection
	 */
	protected double[][] advcoeff;

	/**
	 * Helper variable to store the effective advection coefficients. This depends
	 * not only on the advection coefficients {@link #advcoeff} ut also on the time
	 * increment {@link ODE#dt dt} and the linear extension, {@link #linext}.
	 *
	 * @see #initDiffusion(double)
	 */
	protected double[][] beta;

	/**
	 * The flag to indicate whether advection coefficients are non-vanishing. If
	 * {@code false} simply delegate numerical calculations to {@code super}.
	 */
	protected boolean doAdvection = true;

	/**
	 * Constructs a new model for the numerical integration of the system of partial
	 * differential equations representing the dynamics specified by the
	 * {@link Module} <code>module</code> using the {@link EvoLudo} pacemaker
	 * <code>engine</code> to control the numerical evaluations. The integrator
	 * implements reaction-diffusion-advection steps based on Euler's method (fixed
	 * step size).
	 * <p>
	 * <strong>Important:</strong> for reproducibility the shared random number
	 * generator should be used (here only used to generate random initial
	 * configurations).
	 * <p>
	 * <strong>Note:</strong> requires a supervisor that matches the implementation,
	 * i.e. JRE or GWT, respectively, to properly deal with multiple threads or
	 * scheduling.
	 *
	 * @param engine the pacemaker for running the model
	 *
	 * @see EvoLudo#getRNG()
	 * @see org.evoludo.simulator.models.PDESupervisorGWT PDESupervisorGWT
	 * @see org.evoludo.simulator.models.PDESupervisorJRE PDESupervisorJRE
	 */
	public Advection(EvoLudo engine) {
		super(engine);
		type = ModelType.PDEADV;
	}

	// DEBUG - uncomment to disable reactions.
	// @Override
	// protected void react(int start, int end) {
	// // NOTE: swapping would be faster but results in some strange behavior with
	// 'apply' and 'init';
	// // since this is not critical it is not worth tracking down
	// for( int n=0; n<density.length; n++ )
	// System.arraycopy(density[n], 0, next[n], 0, density[n].length);
	// double[] dummy = new double[d];
	// updateFitness(dummy, dummy, dummy);
	// // NOTE: in the absence of reactions, the total/mean density of each type
	// should not change
	//// GWT.log("react: meanDensity="+meanDensity);
	// }

	@Override
	public void diffuse(int start, int end) {
		if (!doAdvection) {
			super.diffuse(start, end);
			return;
		}

		double[] minDens = new double[nDim];
		Arrays.fill(minDens, Double.MAX_VALUE);
		double[] maxDens = new double[nDim];
		Arrays.fill(maxDens, -Double.MAX_VALUE);
		double[] meanDens = new double[nDim];
		double[] delta = new double[nDim];
		double[] adv = new double[nDim];

		if (isSymmetric) {
			diffuseSymmetric(start, end, minDens, maxDens, meanDens, delta, adv);
		} else {
			diffuseStandard(start, end, minDens, maxDens, meanDens, delta, adv);
		}
		updateDensity(minDens, maxDens, meanDens);
	}

	/**
	 * Process sites in the symmetric case (neighbour ordering must be sorted).
	 * 
	 * @param start    the starting site index
	 * @param end      the ending site index
	 * @param minDens  the minimum densities (updated)
	 * @param maxDens  the maximum densities (updated)
	 * @param meanDens the mean densities (updated)
	 * @param delta    helper array to store differences
	 * @param adv      helper array to store advection contributions
	 */
	private void diffuseSymmetric(int start, int end, double[] minDens, double[] maxDens,
			double[] meanDens, double[] delta, double[] adv) {
		int[][] in = space.in;
		GeometryFeatures features = space.getFeatures();
		double[][] sort = new double[features.maxIn][];
		for (int n = start; n < end; n++) {
			int[] neighs = in[n];
			int nIn = space.kin[n];
			double[] sn = next[n]; // current state of focal site sn
			double[] s = density[n]; // next state
			ArrayMath.multiply(sn, -space.kout[n], s);
			Arrays.fill(adv, 0.0);
			// collect neighbours
			for (int i = 0; i < nIn; i++) {
				sort[i] = next[neighs[i]];
			}
			// maintain deterministic order for symmetric case
			Arrays.sort(sort, 0, nIn, sorting);
			// accumulate contributions from sorted neighbours
			for (int i = 0; i < nIn; i++) {
				double[] si = sort[i];
				// diffusion
				ArrayMath.add(s, si); // s += si
				// advection contribution from this neighbour
				ArrayMath.sub(si, sn, delta); // delta = si-sn
				ArrayMath.add(delta, 1.0); // delta = 1+si-sn
				addAdvectionContribution(sn, si, delta, adv);
			}
			// finalize site
			ArrayMath.multiply(s, alpha); // s *= alpha
			ArrayMath.add(s, sn); // s += sn
			ArrayMath.add(s, adv); // s += adv
			if (dependent >= 0)
				s[dependent] = 1.0 + s[dependent] - ArrayMath.norm(s);
			minmaxmean(s, minDens, maxDens, meanDens);
		}
	}

	/**
	 * Process sites in the general (asymmetric) case.
	 * 
	 * @param start    the starting site index
	 * @param end      the ending site index
	 * @param minDens  the minimum densities (updated)
	 * @param maxDens  the maximum densities (updated)
	 * @param meanDens the mean densities (updated)
	 * @param delta    helper array to store differences
	 * @param adv      helper array to store advection contributions
	 */
	private void diffuseStandard(int start, int end, double[] minDens, double[] maxDens,
			double[] meanDens, double[] delta, double[] adv) {
		int[][] in = space.in;
		for (int n = start; n < end; n++) {
			int[] neighs = in[n];
			int nIn = space.kin[n];
			double[] sn = next[n]; // current state of focal site sn
			double[] s = density[n]; // next state
			ArrayMath.multiply(sn, -space.kout[n], s);
			Arrays.fill(adv, 0.0);
			for (int i = 0; i < nIn; i++) {
				double[] si = next[neighs[i]]; // neighbour state
				// diffusion
				ArrayMath.add(s, si); // s += si
				// advection contribution from this neighbour
				ArrayMath.sub(si, sn, delta); // delta = si-sn
				ArrayMath.add(delta, 1.0); // delta = 1+si-sn
				addAdvectionContribution(sn, si, delta, adv);
			}
			// finalize site
			ArrayMath.multiply(s, alpha);
			ArrayMath.add(s, sn);
			ArrayMath.add(s, adv);
			if (dependent >= 0)
				s[dependent] = 1.0 + s[dependent] - ArrayMath.norm(s);
			minmaxmean(s, minDens, maxDens, meanDens);
		}
	}

	/**
	 * Adds the advection contribution of a single neighbor (si) to the accumulator
	 * adv for the focal site with state sn using precomputed delta = 1 + si - sn.
	 * 
	 * @param sn    the state of the focal site
	 * @param si    the state of the neighboring site
	 * @param delta the precomputed delta = 1 + si - sn
	 * @param adv   the advection accumulator for the focal site
	 */
	private void addAdvectionContribution(double[] sn, double[] si, double[] delta, double[] adv) {
		int jidx = 0;
		for (int j = 0; j < nDim; j++) {
			if (j == dependent)
				continue;
			double advj = 0.0;
			int kidx = 0;
			for (int k = 0; k < nDim; k++) {
				if (k == dependent)
					continue;
				double dk = delta[k] * 0.5;
				advj += beta[jidx][kidx] * (-sn[j] * dk + si[j] * (1.0 - dk));
				kidx++;
			}
			adv[j] += advj;
			jidx++;
		}
	}

	/**
	 * Gets the 2D advection array for the advection coefficients of each trait
	 * against every other one.
	 *
	 * @return the 2D array of advection coefficients
	 */
	public double[][] getAdvection() {
		return advcoeff;
	}

	@Override
	public void initDiffusion(double deltat) {
		super.initDiffusion(deltat);
		if (!doAdvection)
			return;
		// initialize beta
		int dim = dependent < 0 ? nDim : nDim - 1;
		if (beta == null || beta.length != dim)
			beta = new double[dim][dim];
		double invdx = calcInvDeltaX();
		double invdx2 = invdx * invdx;
		for (int r = 0; r < dim; r++)
			ArrayMath.multiply(advcoeff[r], deltat * invdx2, beta[r]);
	}

	@Override
	protected void checkDt() {
		super.checkDt();
		// some careful checking for suitable time steps is required!
		// if Ai/(dx^2)>1/dt for any Ai then adjust dt
		// if Ai/(dx^2)*dt*k>1 for any Ai with k the maximum number of outgoing or
		// incoming links then adjust dt
		double invdx = calcInvDeltaX();
		double invdx2 = invdx * invdx;
		// note: negative advcoeff are meaningful to avoid bad areas; maximal dt depends
		// on magnitude of advcoeff
		// DEBUG the following seems too conservative (at least for normalized densities
		// - try without but check whether densities remain in [0,1])
		if (doAdvection) {
			double maxA = Math.max(ArrayMath.max(advcoeff), Math.abs(ArrayMath.min(advcoeff))) * invdx2;
			GeometryFeatures features = space.getFeatures();
			int maxK = Math.max(features.maxOut, features.maxIn);
			// threshold of 1 is much to aggressive - this means everyone in one site
			// migrates! this can introduce artifacts!
			if (dt < 1e-5 || nDim * maxA * maxK * dt > 0.5) {
				double deltat = Math.max(0.5 / (nDim * maxA * maxK), Double.MIN_VALUE);
				if (logger.isLoggable(Level.INFO))
					logger.info("PDE time scale adjusted (advection): dt=" + Formatter.formatSci(deltat, 4)
							+ " (was dt=" + Formatter.formatSci(dt, 4) + ").");
				dt = deltat;
			}
		}
	}

	/**
	 * Command line option to set the advection coefficients of every trait against
	 * all others.
	 */
	public final CLOption cloPdeAdvection = new CLOption("pdeA", null, CLOCategory.Model, null,
			new CLODelegate() {
				@Override
				public boolean parse(String arg) {
					doAdvection = false;
					advcoeff = null;
					if (arg == null)
						return true;
					// dim not yet set - retrieve directly from module
					int dim = module.getNTraits();
					if (module instanceof HasDE && ((HasDE) module).getDependent() >= 0)
						dim--;
					advcoeff = CLOParser.parseMatrix(arg);
					if (advcoeff.length != dim || advcoeff[0].length != dim) {
						advcoeff = null;
						return false;
					}
					doAdvection = true;
					return true;
				}

				@Override
				public String getDescription() {
					StringBuilder descr = new StringBuilder();
					int dim = nDim;
					if (dependent >= 0)
						dim--;
					final String advto = "      towards j (aij>0, or away aij<0), with\n";
					switch (dim) {
						case 1:
							// SINGLE DIM NOT TESTED!
							descr.append("--pdeA <a0>          advection to its own type\n");
							break;
						case 2:
							descr.append(
									"--pdeA <a00,a01;a10,a11> 2x2 matrix for advection aij of type i\n")
									.append(advto);
							break;
						case 3:
							descr.append(
									"--pdeA <a00,...;a10,...;a20,...> 3x3 matrix for advection aij of type i\n")
									.append(advto);
							break;
						default:
							int d1 = nDim - 1;
							descr.append("--pdeA <a00,...;...;a")
									.append(d1)
									.append("0,...> ")
									.append(d1)
									.append("x")
									.append(d1)
									.append(" advection aij of type i\n")
									.append(advto);
					}
					int idx = 0;
					for (int n = 0; n < nDim; n++) {
						if (n == dependent) {
							descr.append("         ")
									.append(names[n])
									.append(" (dependent)\n");
							continue;
						}
						descr.append("      ")
								.append(idx++)
								.append(": ")
								.append(names[n])
								.append("\n");
					}
					descr.append("      advection: ai=Ai/(2*dx^2)");
					return descr.toString();
				}
			});

	@Override
	public void collectCLO(CLOParser parser) {
		// (re)set defaults prior to parsing CLOs
		parser.addCLO(cloPdeAdvection);
		super.collectCLO(parser);
	}
}
