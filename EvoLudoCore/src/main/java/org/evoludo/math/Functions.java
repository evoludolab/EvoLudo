//
// EvoLudo Project
//
// Copyright 2010 Christoph Hauert
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
//	Hauert, Christoph (<year>) EvoLudo Project, http://www.evoludo.org
//			(doi: <doi>[, <version>])
//
//	<doi>:	digital object identifier of the downloaded release (or the
//			most recent release if downloaded from github.com),
//	<year>:	year of release (or download), and
//	[, <version>]: optional version number (as reported in output header
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
	public Functions() {
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
	 * <code>2.51&rarr;3, 25.1&rarr;30, 251&rarr;260, 2510&rarr;2600, ...</code>
	 * 
	 * @param value the {@code double} to be rounded up
	 * @return the rounded value
	 */
	public static double roundUp(double value) {
		int magnitude = magnitude(value);
		if (magnitude == 0)
			return Math.ceil(value);
		double factor = Combinatorics.pow(10.0, magnitude);
		return Math.ceil(value / factor) * factor;
	}

	/**
	 * Round <code>value</code> down to the next lower order of magnitude. For
	 * example,
	 * <code>2.51&rarr;2, 25.1&rarr;20, 251&rarr;200, 2510&rarr;2000, ...</code>
	 * 
	 * @param value the {@code double} to be rounded down
	 * @return the rounded value
	 */
	public static double roundDown(double value) {
		int magnitude = magnitude(value);
		if (magnitude == 0)
			return Math.floor(value);
		double factor = Combinatorics.pow(10.0, magnitude);
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
			while (value <= 0.1) {
				value *= 10.0;
				magnitude--;
			}
		}
		return magnitude;
	}

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
}
