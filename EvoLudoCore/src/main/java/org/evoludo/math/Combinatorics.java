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
package org.evoludo.math;

/**
 * Collection of convenience methods for mathematical operations dealing with
 * combinatorics.
 * 
 * @author Christoph Hauert
 */
public class Combinatorics {

	/**
	 * Ensure non-instantiability with private default constructor
	 */
	public Combinatorics() {
	}

	/**
	 * Calculate {@code x^n} for integer {@code x} and integer
	 * {@code n}. For {@code n&geq;0} the result is an integer but returned as
	 * a double, which is necessary to deal with {@code n<0}. All intermediate
	 * calculations are perfomed as integers and will throw the corresponding
	 * exceptions, e.g. if numbers exceed {@code Integer#MAX_VALUE}.
	 * This is an optimization for the CPU intense {@link Math#pow(double, double)}.
	 *
	 * @param x the basis of the power
	 * @param n the integer exponent of the power
	 * @return {@code x^n}
	 */
	public static double pow(int x, int n) {
		double pow = powabs(x, Math.abs(n));
		if (n > 0)
			return pow;
		return 1.0 / pow;
	}

	/**
	 * Helper method to calculate \(x^n\) with \(n&gt;0\).
	 * 
	 * @param x the basis of the power
	 * @param n the (positive) integer exponent of the power
	 * @return {@code x^n}
	 */
	private static int powabs(int x, int n) {
		int x2, x4;

		switch (n) {
			case 10:
				x2 = x * x;
				x4 = x2 * x2;
				return x2 * x4 * x4;
			case 9:
				x2 = x * x * x;
				return x2 * x2 * x2;
			case 8:
				x2 = x * x;
				x2 *= x2;
				return x2 * x2;
			case 7:
				x2 = x * x * x;
				return x * x2 * x2;
			case 6:
				x2 = x * x * x;
				return x2 * x2;
			case 5:
				x2 = x * x;
				return x * x2 * x2;
			case 4:
				x2 = x * x;
				return x2 * x2;
			case 3:
				return x * x * x;
			case 2:
				return x * x;
			case 1:
				return x;
			case 0:
				if (x == 0)
					throw new ArithmeticException("0^0 undefined.");
				return 1;
			default:
				x2 = x * x;
				x4 = x2 * x2;
				int x10 = x2 * x4 * x4;
				int xn = x10;
				int exp = 10;
				while (n - exp > 10) {
					xn *= x10;
					exp += 10;
				}
				return xn * powabs(x, n - exp);
		}
	}

	/**
	 * Calculate {@code x^n} for double {@code x} and integer
	 * {@code n}. This is an optimization for the CPU intense
	 * {@link Math#pow(double, double)}.
	 *
	 * @param x the basis of the power
	 * @param n the exponent of the power
	 * @return {@code x^n}
	 */
	public static double pow(double x, int n) {
		double pow = powabs(x, Math.abs(n));
		if (n > 0)
			return pow;
		return 1.0 / pow;
	}

	/**
	 * Helper method to calculate \(x^n\) with \(n&gt;0\).
	 * 
	 * @param x the basis of the power
	 * @param n the (positive) integer exponent of the power
	 * @return {@code x^n}
	 */
	private static double powabs(double x, int n) {
		double x2, x3, x4, x6, x8;
		if (n < 0) {
			x = 1.0 / x;
			n = -n;
		}
		switch (n) {
			case 16:
				x2 = x * x;
				x4 = x2 * x2;
				x8 = x4 * x4;
				return x8 * x8;
			case 15:
				x3 = x * x * x;
				x6 = x3 * x3;
				return x3 * x6 * x6;
			case 14:
				x2 = x * x;
				x6 = x2 * x2 * x2;
				return x2 * x6 * x6;
			case 13:
				x3 = x * x * x;
				x6 = x3 * x3;
				return x * x6 * x6;
			case 12:
				x2 = x * x;
				x4 = x2 * x2;
				return x4 * x4 * x4;
			case 11:
				x2 = x * x;
				x4 = x2 * x2;
				return x * x2 * x4 * x4;
			case 10:
				x2 = x * x;
				x4 = x2 * x2;
				return x2 * x4 * x4;
			case 9:
				x3 = x * x * x;
				return x3 * x3 * x3;
			case 8:
				x2 = x * x;
				x4 = x2 * x2;
				return x4 * x4;
			case 7:
				x3 = x * x * x;
				return x * x3 * x3;
			case 6:
				x3 = x * x * x;
				return x3 * x3;
			case 5:
				x2 = x * x;
				return x * x2 * x2;
			case 4:
				x2 = x * x;
				return x2 * x2;
			case 3:
				return x * x * x;
			case 2:
				return x * x;
			case 1:
				return x;
			case 0:
				if (Math.abs(x) < 1e-12)
					throw new ArithmeticException("0^0 undefined.");
				return 1.0;
			default:
				x2 = x * x;
				x4 = x2 * x2;
				x8 = x4 * x4;
				double x16 = x8 * x8;
				double xn = x16;
				int exp = 16;
				while (n - exp > 16) {
					xn *= x16;
					exp += 16;
				}
				return xn * powabs(x, n - exp);
		}
	}

	/**
	 * Combinations: number of ways to draw {@code k} elements from pool of
	 * size {@code n}.
	 * <p>
	 * <em>Mathematica:</em> {@code Binomial[n,k] = n!/(k!(n-k)!)}
	 * 
	 * @param n the pool size
	 * @param k the number of samples
	 * @return {@code Binomial[n,k]}
	 * @throws ArithmeticException if result {@code &gt;Integer.MAX_VALUE}
	 */
	public static int combinations(int n, int k) {
		if (k < 0 || n < k)
			return 0;
		if (n == 0 || k == 0 || n == k)
			return 1;
		if (k == 1 || n == k + 1)
			return n;
		double comb = (double) n / (double) k;
		for (int i = 1; i < k; i++)
			comb *= (double) (n - i) / (double) i;
		if (comb > Integer.MAX_VALUE)
			throw new ArithmeticException("result exceeds max int");
		return (int) Math.floor(comb + 0.5);
	}

	/**
	 * Combinations: number of ways to draw {@code k} elements from pool of
	 * size {@code n}.
	 * <p>
	 * <em>Mathematica:</em> {@code Binomial[n,k] = n!/(k!(n-k)!)}
	 * 
	 * @param n the pool size
	 * @param k the number of samples
	 * @return {@code Binomial[n,k]}
	 * @throws ArithmeticException if result {@code &gt;Long.MAX_VALUE}
	 */
	public static long combinations(long n, long k) {
		if (k < 0 || n < k)
			return 0;
		if (n == 0 || k == 0 || n == k)
			return 1;
		if (k == 1 || n == k + 1)
			return n;
		double comb = (double) n / (double) k;
		for (int i = 1; i < k; i++)
			comb *= (double) (n - i) / (double) i;
		if (comb > Long.MAX_VALUE)
			throw new ArithmeticException("result exceeds max long");
		return (long) Math.floor(comb + 0.5);
	}

	/**
	 * Efficient calculation of
	 * \[\dfrac{\binom{n}{k}}{\binom{m}{k}}=\frac{n!(m-k)!}{m!(n-k)!},\]
	 * or {@code Binomial[n,k]/Binomial[m,k]} for <em>Mathematica:</em>, while
	 * reducing the risk of numerical overflow.
	 * <p>
	 * Note, this is the same as {@code H2(n, 0, m-n, k)} with {@code m&gt;n}.
	 * 
	 * @param n the size of first pool
	 * @param m the size of the second pool
	 * @param k the number of samples to draw from each pool (without replacement)
	 * @return {@code Binomial[n,k]/Binomial[m,k]}
	 * 
	 * @see #H2(int, int, int, int)
	 */
	public static double combinationFrac(int n, int m, int k) {
		if (k == 0)
			return 1.0;
		if (n < k || k < 0)
			return 0.0;
		if (m < k)
			return 1.0 / 0.0;
		if (n == m)
			return 1.0;
		double frac = (double) n / (double) m;
		for (int i = 1; i < k; i++)
			frac *= (double) (n - i) / (double) (m - i);
		return frac;
	}

	/**
	 * Efficient calculation of the hypergeometric probability distribution
	 * {@code H<sub>2</sub>(X,x,Y,y)} for sampling {@code x} individuals from pool
	 * of size {@code X} and {@code y} individuals from pool of size {@code Y}.
	 * <p>
	 * <em>Mathematica:</em>
	 * {@code H_2[X, x, Y, y] = Binomial[X,x] Binomial[Y,y] / Binomial[X+Y,x+y]}
	 * 
	 * @param X the size of first pool
	 * @param x the number samples from first pool
	 * @param Y the size of second pool
	 * @param y the number samples from second pool
	 * @return {@code H_2[X, x, Y, y]}
	 */
	public static double H2(int X, int x, int Y, int y) {
		if (x < 0 || x > X || y < 0 || y > Y)
			return 0.0;
		if (x < y) {
			int swap = x;
			x = y;
			y = swap;
			swap = X;
			X = Y;
			Y = swap;
		}
		double num = X;
		double XY = X + Y;
		double frac = 1.0;
		for (int n = 0; n < x; n++)
			frac *= (num--) / (XY--);
		num = Y;
		double xy = x + y;
		int terms = y;
		for (int n = 0; n < terms; n++)
			frac *= ((num--) * (xy--)) / ((XY--) * (y--));
		return frac;
	}
}
