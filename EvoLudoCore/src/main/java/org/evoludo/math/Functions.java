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
package org.evoludo.math;

/**
 * Collection of mathematical functions.
 * 
 * @author Christoph Hauert
 */
public class Functions {

	/**
	 * Ensure non-instantiability with private default constructor
	 */
	private Functions() {
	}

	/**
	 * Round <code>value</code> to next order of magnitude. For example, round 4 to
	 * 1 and 6 to 10.
	 * 
	 * @param value the {@code double} to be rounded up
	 * @return the rounded value
	 */
	public static double round(double value) {
		int magnitude = magnitude(value);
		if (magnitude == 0)
			return Math.round(value);
		double factor = Combinatorics.pow(10.0, magnitude - 1);
		return Math.round(value / factor) * factor;
	}

	/**
	 * Round <code>value</code> up to the next lower order of magnitude. For
	 * example,
	 * <code>2.51&rarr;2.6, 25.1&rarr;26, 251&rarr;260, 2510&rarr;2600, ...</code>
	 * 
	 * @param value the {@code double} to be rounded up
	 * @return the rounded value
	 */
	public static double roundUp(double value) {
		int magnitude = magnitude(value);
		double factor = Combinatorics.pow(10.0, magnitude - 1);
		// prevent returning NaN for very small magnitudes
		if (factor == 0.0)
			return value;
		return Math.ceil(value / factor) * factor;
	}

	/**
	 * Round <code>value</code> down to the next lower order of magnitude. For
	 * example,
	 * <code>2.51&rarr;2.5, 25.1&rarr;25, 251&rarr;250, 2510&rarr;2500, ...</code>
	 * 
	 * @param value the {@code double} to be rounded down
	 * @return the rounded value
	 */
	public static double roundDown(double value) {
		int magnitude = magnitude(value);
		double factor = Combinatorics.pow(10.0, magnitude - 1);
		// prevent returning NaN for very small magnitudes
		if (factor == 0.0)
			return value;
		return Math.floor(value / factor) * factor;
	}

	/**
	 * Returns the order of magnitude of {@code value}.
	 * 
	 * @param value the number to determine its order of magnitude
	 * @return the order of magnitude
	 */
	public static int magnitude(double value) {
		if (!Double.isFinite(value))
			return 0;
		value = Math.abs(value);
		// values of zero cause a headache
		if (value < Double.MIN_VALUE)
			return 0;
		int magnitude = 0;
		if (value >= 1.0) {
			while (value >= 10.0) {
				value *= 0.1;
				magnitude++;
			}
		} else {
			while (value < 1.0) {
				value *= 10.0;
				magnitude--;
			}
		}
		return magnitude;
	}

	/**
	 * The maximum argument that the hyperbolic tangent can handle.
	 */
	private static final double TANH_MAX_ARG = 19.0;

	/**
	 * Returns the hyperbolic tangent of a double value. The hyperbolic tangent of
	 * \(x\) is defined to be \((e^x - e^{-x})/(e^x + e^{-x})\), in other words,
	 * \(\sinh(x)/\cosh(x)\). Note that the absolute value of the exact \(\tanh\) is
	 * always less than \(1\).
	 * 
	 * <h3>Implementation Notes:</h3>
	 * <ul>
	 * <li>GWT emulation of {@code Math.tanh(double)} is bad for large {@code z}...
	 * should return {@code 1.0} instead of {@code Double.NaN}!
	 * <li>{@code tanh} requires the calculation of {@code exp(2x)} but this returns
	 * {@code infinity} in JavaScript if {@code x} is too large. Ensure
	 * {@code x &lt; Math.log(Double.MAX_VALUE) * 0.5}, which is \(\approx 350\).
	 * <li>Moreover, {@code tanh} calculations are only meaningful if
	 * {@code exp(2x)-1 != exp(2x)}, which results in a far lower threshold of
	 * merely {@code x &lt; 19}.
	 * </ul>
	 * 
	 * @param x the {@code double} whose hyperbolic tangent to calculate
	 * @return the hyperbolic tangent of {@code x}
	 * 
	 * @see Math#tanh(double)
	 */
	public static double tanh(double x) {
		if (x == 0.0)
			return x;
		if (x > TANH_MAX_ARG)
			return 1.0;
		if (x < -TANH_MAX_ARG)
			return -1.0;
		double e2x = Math.exp(2.0 * x);
		return (e2x - 1.0) / (e2x + 1.0);
	}

	/**
	 * The number of Newton&ndash;Raphson iterations used by
	 * {@link #fifthRoot(double)}. Chosen with a comfortable margin beyond the
	 * empirically established minimum (9 iterations for arguments in
	 * \((1.89\times 10^{-4}, 1]\), 11 for the much wider \([10^{-4}, 10^9]\), each
	 * verified against 2&times;10<sup>6</sup> log-uniform samples with zero
	 * failures to reach a stable fixed point); a few extra iterations cost
	 * essentially nothing since each is just a handful of arithmetic operations.
	 */
	private static final int FIFTHROOT_ITER = 12;

	/**
	 * Returns the (real) fifth root of {@code x}, i.e. \(y\) such that
	 * \(y^5 = x\), computed through a fixed number of Newton&ndash;Raphson
	 * iterations using only {@code +}, {@code -}, {@code *}, {@code /} and
	 * {@link Math#sqrt(double)}.
	 * <p>
	 * Unlike {@code Math.pow(x, 0.2)}, this is guaranteed to return bit-identical
	 * results on every platform, including between the JRE and GWT/JavaScript
	 * builds: {@code Math.sqrt} and the four basic arithmetic operators are all
	 * required by IEEE&nbsp;754 to be correctly rounded, but {@code Math.pow}
	 * with a non-trivial exponent is not - Java's and JavaScript's
	 * implementations can (and do) differ in the last bit. Introduced because
	 * the adaptive step-size control of the Runge-Kutta integrator needs
	 * \(\mathtt{errmax}^{-0.2}\), and even single-ULP disagreements there
	 * compound, over many integration steps, into JRE/GWT trajectories that
	 * diverge macroscopically.
	 *
	 * <h3>Implementation Notes:</h3>
	 * <ul>
	 * <li>The starting guess {@code sqrt(sqrt(x))} (\(=x^{0.25}\), close to the
	 * target exponent \(0.2\)) converges to a stable fixed point well within
	 * {@code FIFTHROOT_ITER} iterations for arguments roughly in
	 * \([10^{-4}, 10^9]\) (see {@link #FIFTHROOT_ITER}). This is not intended as
	 * a bullet-proof replacement for {@code Math.pow(x, 0.2)} across the entire
	 * {@code double} range - arguments near {@link Double#MIN_VALUE} or
	 * {@link Double#MAX_VALUE} may need more iterations to converge.
	 * <li>The starting guess itself need not be identical across platforms for
	 * the final result to be: since every iteration step uses only
	 * IEEE-754-exact operations, any two starting points in the basin of the
	 * same root converge to the same fixed point once fully converged.
	 * <li>Handles negative {@code x} through the odd-root identity
	 * \(\sqrt[5]{-x} = -\sqrt[5]{x}\).
	 * </ul>
	 *
	 * @param x the {@code double} whose fifth root to calculate
	 * @return the fifth root of {@code x}
	 */
	public static double fifthRoot(double x) {
		if (x == 0.0)
			return x;
		boolean negative = x < 0.0;
		double a = negative ? -x : x;
		double y = Math.sqrt(Math.sqrt(a));
		for (int i = 0; i < FIFTHROOT_ITER; i++) {
			double y4 = y * y * y * y;
			y = (4.0 * y + a / y4) / 5.0;
		}
		return negative ? -y : y;
	}
}
