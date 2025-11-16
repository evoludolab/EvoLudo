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
import org.evoludo.simulator.modules.Module;

/**
 * Implementation of Runge-Kutta method with adaptive step size for the
 * numerical integration of systems of differential equations. Adapted from
 * numerical recipes in C.
 * 
 * @author Christoph Hauert
 */
public class RungeKutta extends ODE {

	/**
	 * <code>true</code> if the adaptive step sizes should be used. If
	 * <code>false</code> the traditional Euler's method with fixed step size is
	 * used.
	 * 
	 * @see ODE
	 */
	boolean autoDt = true;

	/**
	 * <code>true</code> to resort to the traditional Euler's method with fixed step
	 * size. This happens if <code>autoDt==false</code> or in multi-species modules
	 * with mixed frequency and density dynamics.
	 * 
	 * @see ODE
	 * @see #autoDt
	 */
	boolean doEuler = false;

	/**
	 * Constructs a new model for the numerical integration of the system of
	 * ordinary differential equations representing the dynamics specified by the
	 * {@link Module} <code>module</code> using the {@link EvoLudo} pacemaker
	 * <code>engine</code> to control the numerical evaluations. The integrator
	 * implements the fifth order Runge-Kutta method with adaptive step size.
	 * 
	 * @param engine the pacemaker for running the model
	 */
	public RungeKutta(EvoLudo engine) {
		super(engine);
		type = Type.RK5;
	}

	@Override
	public void unload() {
		yerr = ytmp = ftmp = null;
		ak2 = ak3 = ak4 = ak5 = ak6 = null;
		super.unload();
	}

	@Override
	public void reset() {
		super.reset();
		if (doEuler)
			return;
		if (yerr == null || yerr.length != nDim) {
			yerr = new double[nDim];
			ytmp = new double[nDim];
			ftmp = new double[nDim];
			ak2 = new double[nDim];
			ak3 = new double[nDim];
			ak4 = new double[nDim];
			ak5 = new double[nDim];
			ak6 = new double[nDim];
		}
	}

	/**
	 * Sets whether adaptive step sizes should be used. This is required for this
	 * implementation of the fifth order Cash-Karp Runge-Kutta method. If set to
	 * {@code false} the integrator defaults to the Euler method with fixed
	 * increments.
	 * 
	 * @param autoDt the flag to indicate whether adaptive step sizes should be
	 *               used.
	 * 
	 * @see ODE
	 */
	public void setAutoDt(boolean autoDt) {
		this.autoDt = autoDt;
	}

	/**
	 * Gets whether adaptive step sizes are used. If not the integrator defaults to
	 * the Euler method with fixed increments.
	 *
	 * @return {@code true} if using adaptive step sizes
	 */
	public boolean getAutoDt() {
		return autoDt;
	}

	@Override
	public boolean check() {
		boolean doReset = super.check();
		if (!autoDt) {
			logger.warning(getClass().getSimpleName()
					+ " - Runge-Kutta with fixed time increments requested - revert to Euler method.");
			doEuler = true;
		}
		return doReset;
	}

	/**
	 * The safety margin for adjusting time steps. Magic number from Numerical
	 * Recipes in C.
	 */
	private static final double SAFETY = 0.9;

	/**
	 * The exponent for increasing the time step if error outside margin. Magic
	 * number from Numerical Recipes in C.
	 * 
	 * @see #ERRCON
	 */
	private static final double PGROW = -0.2;

	/**
	 * The exponent for decreasing the time step if error outside margin. Magic
	 * number from Numerical Recipes in C.
	 * 
	 * @see #ACCURACY
	 */
	private static final double PSHRNK = -0.25;

	/**
	 * The maximum error in a single step. The value {@code ERRCON} equals
	 * {@code (5/SAFETY)^(1/PGROW)}. Magic number from Numerical Recipes in C.
	 */
	private static final double ERRCON = 1.89e-4;

	/**
	 * The accuracy required for the integration. Magic number from Numerical
	 * Recipes in C.
	 */
	private static final double ACCURACY = 1e-7;

	/**
	 * Helper variables and temporary storage for errors, states and fitness when
	 * calculating derivatives for different steps.
	 */
	private double[] yerr;
	private double[] ytmp;
	private double[] ftmp;

	/**
	 * {@inheritDoc}
	 * 
	 * <h3>Implementation Notes:</h3> Fifth-order Runge-Kutta step with monitoring
	 * of local truncation error to ensure accuracy and adjust step size. Input are
	 * the dependent variable vector y[1..n] and its derivative dydx[1..n] at the
	 * starting value of the independent variable x. Also input are the step size to
	 * be attempted htry, the required accuracy eps, and the vector yscal[1..n]
	 * against which the error is scaled. On output, y and x are replaced by their
	 * new values, hdid is the step size that was actually accomplished, and hnext
	 * is the estimated next step size. derivs is the user-supplied routine that
	 * computes the right-hand side derivatives.
	 * <p>
	 * If you desire constant fractional errors, plug a pointer to y into the
	 * pointer to yscal calling slot (no need to copy the values into a different
	 * array). If you desire constant absolute errors relative to some maximum
	 * values, set the elements of yscal equal to those maximum values. A useful
	 * “trick” for getting constant fractional errors except “very” near zero
	 * crossings is to set yscal[i] equal to |y[i]| + |h × dydx[i]|. (The routine
	 * odeint, below, does this.)
	 * <p>
	 * Copied from Numerical Recipes in C, chapter 16.2, p.718f
	 */
	@Override
	protected double deStep(double step) {
		if (doEuler)
			return super.deStep(step);
		double errmax;
		double h = step; // set step size to the initial trial value.
		double[] yscal = yt; // constant fractional errors

		while (true) {
			// take a step.
			while (!rkck(h)) {
				// step failed - decrease step width and try again
				h /= 2.0;
			}
			errmax = 0.0; // evaluate accuracy.
			for (int i = 0; i < nDim; i++) {
				if (yscal[i] > 1e-6) // ignore component if variable is zero.
					errmax = Math.max(errmax, Math.abs(yerr[i] / yscal[i]));
			}
			errmax /= ACCURACY; // scale relative to required tolerance.
			if (errmax <= 1.0)
				break; // step succeeded - compute size of next step.
			double htemp = SAFETY * h * Math.pow(errmax, PSHRNK);
			// truncation error too large, reduce step size.
			h = (h >= 0.0 ? Math.max(htemp, 0.1 * h) : Math.min(htemp, 0.1 * h));
			// no more than a factor of 10.
			double tnew = time + h;
			if (tnew == time) {
				logger.warning("stepsize underflow in ODE method RungeKutta.deStep() at time " + time + ".");
				dtTaken = 0.0;
				return -1.0;
			}
		}
		time += h;
		// ensure that dtTry and dtTaken remain positive
		h = Math.abs(h);
		if (errmax > ERRCON)
			dtTry = SAFETY * h * Math.pow(errmax, PGROW);
		else
			dtTry = 2.0 * h; // no more than doubling.
		dtTaken = h;

		// if it's a replicator equation the frequencies must add up to 1
		// this can be used to improve numerical accuracy.
		normalizeState(yout);

		// the new state is in yout - swap and determine new fitness
		double[] swap = yt;
		yt = yout;
		yout = swap;
		// determine fitness of new state
		getDerivatives(time, yt, ft, dyt);
		return ArrayMath.distSq(yout, yt);
	}

	/**
	 * More magic numbers from Numerical Recipes in C.
	 */
	private static final double A2 = 0.2;
	private static final double A3 = 0.3;
	private static final double A4 = 0.6;
	private static final double A5 = 1;
	private static final double A6 = 0.875;
	private static final double B21 = 0.2;
	private static final double B31 = 3.0 / 40.0;
	private static final double B32 = 9.0 / 40.0;
	private static final double B41 = 0.3;
	private static final double B42 = -0.9;
	private static final double B43 = 1.2;
	private static final double B51 = -11.0 / 54.0;
	private static final double B52 = 2.5;
	private static final double B53 = -70.0 / 27.0;
	private static final double B54 = 35.0 / 27.0;
	private static final double B61 = 1631.0 / 55296.0;
	private static final double B62 = 175.0 / 512.0;
	private static final double B63 = 575.0 / 13824.0;
	private static final double B64 = 44275.0 / 110592.0;
	private static final double B65 = 253.0 / 4096.0;
	private static final double C1 = 37.0 / 378.0;
	private static final double C3 = 250.0 / 621.0;
	private static final double C4 = 125.0 / 594.0;
	private static final double C6 = 512.0 / 1771.0;
	private static final double DC5 = -277.0 / 14336.0;
	private static final double DC1 = C1 - 2825.0 / 27648.0;
	private static final double DC3 = C3 - 18575.0 / 48384.0;
	private static final double DC4 = C4 - 13525.0 / 55296.0;
	private static final double DC6 = C6 - 0.25;

	/**
	 * Temporary variables for intermediate results required to implement the
	 * fifth-order Cash-Karp Runge-Kutta method.
	 */
	private double[] ak2;
	private double[] ak3;
	private double[] ak4;
	private double[] ak5;
	private double[] ak6;

	/**
	 * Given values for n variables y[1..n] and their derivatives dydx[1..n] known
	 * at x, use the fifth-order Cash-Karp Runge-Kutta method to advance the
	 * solution over an interval h and return the incremented variables as
	 * yout[1..n]. Also return an estimate of the local truncation error in yout
	 * using the embedded fourth-order method. The user supplies the routine
	 * derivs(x,y,dydx), which returns derivatives dydx at x.
	 * <p>
	 * Copied from Numerical Recipes in C, chapter 16.2, p.719
	 * 
	 * <h3>Implementation Notes:</h3>
	 * For replicator dynamics all intermediate steps must remain
	 * normalized, i.e. sum ytmp = 1, otherwise reject. Currently this
	 * requires that in multi-species modules either all or none are
	 * frequency based.
	 * 
	 * @param h the step to try to take
	 * @return <code>true</code> if step successful
	 */
	private boolean rkck(double h) {
		double ytmax;
		double ytmin;

		ytmax = -Double.MAX_VALUE;
		ytmin = Double.MAX_VALUE;
		for (int i = 0; i < nDim; i++) { // first step.
			double y = yt[i] + B21 * h * dyt[i];
			ytmp[i] = y;
			ytmax = Math.max(ytmax, y);
			ytmin = Math.min(ytmin, y);
		}
		if (!isDensity && (ytmin < 0.0 || ytmax > 1.0))
			return false;

		getDerivatives(time + A2 * h, ytmp, ftmp, ak2); // second step.
		ytmax = -Double.MAX_VALUE;
		ytmin = Double.MAX_VALUE;
		for (int i = 0; i < nDim; i++) {
			double y = yt[i] + h * (B31 * dyt[i] + B32 * ak2[i]);
			ytmp[i] = y;
			ytmax = Math.max(ytmax, y);
			ytmin = Math.min(ytmin, y);
		}
		if (!isDensity && (ytmin < 0.0 || ytmax > 1.0))
			return false;

		getDerivatives(time + A3 * h, ytmp, ftmp, ak3); // third step.
		ytmax = -Double.MAX_VALUE;
		ytmin = Double.MAX_VALUE;
		for (int i = 0; i < nDim; i++) {
			double y = yt[i] + h * (B41 * dyt[i] + B42 * ak2[i] + B43 * ak3[i]);
			ytmp[i] = y;
			ytmax = Math.max(ytmax, y);
			ytmin = Math.min(ytmin, y);
		}
		if (!isDensity && (ytmin < 0.0 || ytmax > 1.0))
			return false;

		getDerivatives(time + A4 * h, ytmp, ftmp, ak4); // fourth step.
		ytmax = -Double.MAX_VALUE;
		ytmin = Double.MAX_VALUE;
		for (int i = 0; i < nDim; i++) {
			double y = yt[i] + h * (B51 * dyt[i] + B52 * ak2[i] + B53 * ak3[i] + B54 * ak4[i]);
			ytmp[i] = y;
			ytmax = Math.max(ytmax, y);
			ytmin = Math.min(ytmin, y);
		}
		if (!isDensity && (ytmin < 0.0 || ytmax > 1.0))
			return false;

		getDerivatives(time + A5 * h, ytmp, ftmp, ak5); // fifth step.
		ytmax = -Double.MAX_VALUE;
		ytmin = Double.MAX_VALUE;
		for (int i = 0; i < nDim; i++) {
			double y = yt[i] + h * (B61 * dyt[i] + B62 * ak2[i] + B63 * ak3[i] + B64 * ak4[i] + B65 * ak5[i]);
			ytmp[i] = y;
			ytmax = Math.max(ytmax, y);
			ytmin = Math.min(ytmin, y);
		}
		if (!isDensity && (ytmin < 0.0 || ytmax > 1.0))
			return false;

		getDerivatives(time + A6 * h, ytmp, ftmp, ak6); // sixth step.
		ytmax = -Double.MAX_VALUE;
		ytmin = Double.MAX_VALUE;
		for (int i = 0; i < nDim; i++) { // accumulate increments with proper weights.
			double y = yt[i] + h * (C1 * dyt[i] + C3 * ak3[i] + C4 * ak4[i] + C6 * ak6[i]);
			yout[i] = y;
			ytmax = Math.max(ytmax, y);
			ytmin = Math.min(ytmin, y);
		}
		if (!isDensity && (ytmin < 0.0 || ytmax > 1.0))
			return false;

		// estimate error as difference between fourth and fifth order methods.
		for (int i = 0; i < nDim; i++)
			yerr[i] = h * (DC1 * dyt[i] + DC3 * ak3[i] + DC4 * ak4[i] + DC5 * ak5[i] + DC6 * ak6[i]);
		return true;
	}
}
