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
 * Collection of convenience methods for mathematical operations dealing with
 * distributions.
 * 
 * @author Christoph Hauert
 */
public class Distributions {

	/**
	 * Ensure non-instantiability with private default constructor
	 */
	private Distributions() {
	}

	/**
	 * Sample variance of data points <code>x[i]</code> stored in double vector
	 * <code>x</code> using one-pass algorithm based on Welford's algorithm.
	 * <p>
	 * Note: if the mean is known/needed, it's more efficient to use
	 * {@link #variance(double[], double)} instead.
	 * </p>
	 * 
	 * @param x data vector
	 * @return sample variance of <code>x</code>
	 * @see <a href=
	 *      "https://en.wikipedia.org/wiki/Algorithms_for_calculating_variance#Welford's_online_algorithm">Wikipedia:
	 *      Algorithms for calculating variance</a>
	 */
	public static double variance(double[] x) {
		// two pass calculation
		// return variance(x, mean(x));
		// one-pass online calculation
		int dim = x.length;
		if (dim < 2)
			return Double.NaN;
		double mean = x[0];
		double sum2 = 0.0;
		for (int n = 1; n < dim; n++) {
			double xn = x[n];
			double dx = xn - mean;
			mean += dx / (n + 1);
			sum2 += dx * (xn - mean);
		}
		return sum2 / (dim - 1);
	}

	/**
	 * Sample variance of data points <code>x[i]</code> stored in double vector
	 * <code>x</code> with known <code>mean</code>. This corresponds to the second
	 * pass of a two-pass algorithm.
	 * 
	 * @param x    data vector
	 * @param mean of <code>x</code>
	 * @return sample variance of <code>x</code>
	 */
	public static double variance(double[] x, double mean) {
		double sum2 = 0.0;
		int dim = x.length;
		for (int n = 0; n < dim; n++) {
			double xn = x[n] - mean;
			sum2 += xn * xn;
		}
		return sum2 / (dim - 1);
	}

	/**
	 * (Sample) Standard deviation of data points <code>x[i]</code> stored in double
	 * vector <code>x</code>.
	 * 
	 * @param x data vector
	 * @return sample standard deviation of <code>x</code>
	 * @see #variance(double[])
	 */
	public static double stdev(double[] x) {
		return Math.sqrt(variance(x));
	}

	/**
	 * (Sample) Standard deviation of data points <code>x[i]</code> stored in double
	 * vector <code>x</code>.
	 * 
	 * @param x    data vector
	 * @param mean of data vector <code>x</code>
	 * @return sample standard deviation of <code>x</code>
	 * @see #variance(double[], double)
	 */
	public static double stdev(double[] x, double mean) {
		return Math.sqrt(variance(x, mean));
	}

	/**
	 * Sample covariance of data points <code>x[i]</code>, <code>y[i]</code> stored
	 * in double vectors <code>x</code> and <code>y</code> using one-pass algorithm
	 * based on Welford's algorithm.
	 * <p>
	 * Note: if the means are known/needed, it's more efficient to use
	 * {@link #covariance(double[], double, double[], double)} instead.
	 * </p>
	 * 
	 * @param x first data vector
	 * @param y second data vector
	 * @return sample covariance of <code>x</code> and <code>y</code>
	 * @see <a href=
	 *      "https://en.wikipedia.org/wiki/Algorithms_for_calculating_variance#Covariance">Wikipedia:
	 *      Algorithms for calculating covariance</a>
	 */
	public static double covariance(double[] x, double[] y) {
		int dim = x.length;
		if (dim < 2 || y.length != dim)
			return Double.NaN;
		double meanx = x[0];
		double meany = y[0];
		double cov = 0.0;
		for (int n = 1; n < dim; n++) {
			double xn = x[n];
			double dx = xn - meanx;
			meanx += dx / (n + 1);
			double yn = y[n];
			meany += (yn - meany) / (n + 1);
			cov += dx * (yn - meany);
		}
		return cov / (dim - 1);
	}

	/**
	 * Sample covariance of data points <code>x[i]</code>, <code>y[i]</code> stored
	 * in double vectors <code>x</code> and <code>y</code> with known means
	 * <code>meanx</code> and <code>meany</code>. This corresponds to the second
	 * pass of a two-pass algorithm.
	 * 
	 * @param x     first data vector
	 * @param meanx the mean of <code>x</code>
	 * @param y     second data vector
	 * @param meany the mean of <code>y</code>
	 * @return sample covariance of <code>x</code> and <code>y</code>
	 */
	public static double covariance(double[] x, double meanx, double[] y, double meany) {
		int dim = x.length;
		if (dim < 2)
			return Double.NaN;
		double cov = 0.0;
		for (int n = 0; n < dim; n++)
			cov += (x[n] - meanx) * (y[n] - meany);

		return cov / (dim - 1);
	}

	/**
	 * Pearson correlation coefficient between data points <code>x[i]</code>,
	 * <code>y[i]</code> stored in double vectors <code>x</code> and <code>y</code>
	 * using one-pass algorithm based on Welford's algorithm.
	 * <p>
	 * Note: if the means and variances are known/needed, it's more efficient to use
	 * {@link #correlation(double[], double, double, double[], double, double)}
	 * instead.
	 * 
	 * @param x first data vector
	 * @param y second data vector
	 * @return Pearson correlation coefficient of <code>x</code> and <code>y</code>
	 * 
	 * @see #covariance(double[], double[])
	 * @see #variance(double[])
	 * @see <a href=
	 *      "https://en.wikipedia.org/wiki/Pearson_correlation_coefficient">Wikipedia:
	 *      Pearson correlation coefficient</a>
	 */
	public static double correlation(double[] x, double[] y) {
		int dim = x.length;
		if (dim < 2)
			return Double.NaN;
		double meanx = x[0];
		double meany = y[0];
		double sumx2 = 0.0;
		double sumy2 = 0.0;
		double cov = meanx * meany;
		for (int n = 1; n < dim; n++) {
			double xn = x[n];
			double dx = xn - meanx;
			meanx += dx / (n + 1);
			sumx2 += dx * (xn - meanx);
			double yn = y[n];
			double dy = yn - meany;
			meany += dy / (n + 1);
			sumy2 += dy * (yn - meany);
			cov += dx * (yn - meany);
		}
		return cov / Math.sqrt(sumx2 * sumy2);
	}

	/**
	 * Pearson correlation coefficient between data points <code>x[i]</code>,
	 * <code>y[i]</code> stored in double vectors <code>x</code> and <code>y</code>
	 * with known means <code>meanx</code>, <code>meany</code> and variances
	 * <code>varx</code>, <code>vary</code>.
	 * 
	 * @param x     first data vector
	 * @param meanx the mean of <code>x</code>
	 * @param varx  the variance of <code>x</code>
	 * @param y     second data vector
	 * @param meany the mean of <code>y</code>
	 * @param vary  the variance of <code>y</code>
	 * @return Pearson correlation coefficient of <code>x</code> and <code>y</code>
	 * 
	 * @see #correlation(double[], double[])
	 * @see #covariance(double[], double, double[], double)
	 * @see #variance(double[], double)
	 */
	public static double correlation(double[] x, double meanx, double varx, double[] y, double meany, double vary) {
		int dim = x.length;
		if (dim < 2)
			return Double.NaN;
		double cov = 0.0;
		for (int n = 0; n < dim; n++)
			cov += (x[n] - meanx) * (y[n] - meany);
		return cov / ((dim - 1) * Math.sqrt(varx * vary));
	}

	/**
	 * <code>m</code>-th centralized moment of data points <code>x[i]</code> stored
	 * in double vector <code>x</code>. Central moments are moments about the mean.
	 * For example, the second central moment is the variance.
	 * 
	 * @param x data vector
	 * @param m moment
	 * @return <code>m</code>-th centralized moment of <code>x</code>
	 */
	public static double centralMoment(double[] x, int m) {
		if (m == 0)
			return 1.0;
		if (m == 1)
			return 0.0;
		return centralMoment(x, Distributions.mean(x), m);
	}

	/**
	 * <code>m</code>-th centralized moment of data points <code>x[i]</code> stored
	 * in double vector <code>x</code> with known mean <code>m1</code>. Central
	 * moments are moments about the mean. For example, the second central moment is
	 * the variance.
	 * 
	 * @param x  data vector
	 * @param m1 mean of <code>x</code>
	 * @param m  moment
	 * @return <code>m</code>-th centralized moment of <code>x</code>
	 */
	public static double centralMoment(double[] x, double m1, int m) {
		if (m == 0)
			return 1.0;
		if (m == 1)
			return 0.0;
		int n = x.length;
		double moment = 0.0;
		for (int i = 0; i < n; i++) {
			moment += Combinatorics.pow(x[i] - m1, m);
		}
		return moment / n;
	}

	/**
	 * Sample skewness of data points <code>x[i]</code> stored in double vector
	 * <code>x</code>.
	 * 
	 * @param x data vector
	 * @return skewness of <code>x</code>
	 */
	public static double skewness(double[] x) {
		return skewness(x, Distributions.mean(x));
	}

	/**
	 * Sample skewness of data points <code>x[i]</code> stored in double vector
	 * <code>x</code> with known mean <code>m1</code>.
	 * 
	 * @param x  data vector
	 * @param m1 mean of<code>x</code>
	 * @return skewness of <code>x</code>
	 */
	public static double skewness(double[] x, double m1) {
		// double m3 = centralMoment(x, m1, 3);
		// double s = stdev(x, m1);
		// same as above but slightly more efficient
		int n = x.length;
		double m2 = 0.0, m3 = 0.0;
		for (int i = 0; i < n; i++) {
			double dx = x[i] - m1;
			double dx2 = dx * dx;
			m2 += dx2;
			m3 += dx2 * dx;
		}
		m3 /= n;
		m2 /= (n - 1.0);
		double s = Math.sqrt(m2);
		return m3 / (s * s * s);
	}

	/**
	 * Sample kurtosis of data points <code>x[i]</code> stored in double vector
	 * <code>x</code>.
	 * 
	 * @param x data vector
	 * @return kurtosis of <code>x</code>
	 */
	public static double kurtosis(double[] x) {
		return kurtosis(x, Distributions.mean(x));
	}

	/**
	 * Sample kurtosis of data points <code>x[i]</code> stored in double vector
	 * <code>x</code> with known mean <code>m1</code>.
	 * 
	 * @param x  data vector
	 * @param m1 mean of<code>x</code>
	 * @return kurtosis of <code>x</code>
	 */
	public static double kurtosis(double[] x, double m1) {
		// double m4 = centralMoment(x, m1, 4);
		// double s = stdev(x, m1);
		// return m4/pow(s, 4);
		// same as above but slightly more efficient
		int n = x.length;
		double m2 = 0.0, m4 = 0.0;
		for (int i = 0; i < n; i++) {
			double dx = x[i] - m1;
			double dx2 = dx * dx;
			m2 += dx2;
			m4 += dx2 * dx2;
		}
		m4 /= n;
		m2 /= (n - 1.0);
		return m4 / (m2 * m2);
	}

	/**
	 * Bimodality coefficient of data points <code>x[i]</code> stored in double
	 * vector <code>x</code>. Coefficient lies between <code>5/9</code>, for uniform
	 * and exponential distributions, and <code>1</code>, for Bernoulli
	 * distributions with two distinct values or the sum of two Dirac delta
	 * functions. Values <code>&gt;5/9</code> may indicate bimodal or multimodal
	 * distributions.
	 * 
	 * @see <a href=
	 *      "https://en.wikipedia.org/wiki/Multimodal_distribution#cite_note-59">Wikipedia:
	 *      Multimodal Distribution</a>
	 * @param x data vector
	 * @return bimodality coefficient
	 */
	public static double bimodality(double[] x) {
		return bimodality(x, Distributions.mean(x));
	}

	/**
	 * Bimodality coefficient of data points <code>x[i]</code> stored in double
	 * vector <code>x</code> with known mean <code>m1</code>. Coefficient lies
	 * between <code>5/9</code>, for uniform and exponential distributions, and
	 * <code>1</code>, for Bernoulli distributions with two distinct values or the
	 * sum of two Dirac delta functions. Values <code>&gt;5/9</code> may indicate
	 * bimodal or multimodal distributions.
	 * 
	 * @see <a href=
	 *      "https://en.wikipedia.org/wiki/Multimodal_distribution#cite_note-59">Wikipedia:
	 *      Multimodal Distribution</a>
	 * @param x  data vector
	 * @param m1 mean of<code>x</code>
	 * @return bimodality coefficient
	 */
	public static double bimodality(double[] x, double m1) {
		double n = x.length;
		// double g = skewness(a, m1);
		// double k = kurtosis(a, m1)-3.0;
		// return (g*g+1)/(k+3*(n-1)*(n-1)/((n-2)*(n-3)));
		// same as above but slightly more efficient
		double m2 = 0.0, m3 = 0.0, m4 = 0.0;
		for (int i = 0; i < n; i++) {
			double dx = x[i] - m1;
			double dx2 = dx * dx;
			m2 += dx2;
			m3 += dx2 * dx;
			m4 += dx2 * dx2;
		}
		m4 /= n;
		m3 /= n;
		m2 /= (n - 1.0);
		double g = m3 / Combinatorics.pow(Math.sqrt(m2), 3);
		double k = m4 / (m2 * m2) - 3.0;
		return (g * g + 1) / (k + 3 * (n - 1) * (n - 1) / ((n - 2) * (n - 3)));
	}

	/**
	 * Mean of probability distribution with weights <code>w[i]</code> stored in
	 * double vector <code>w</code>. This is the same as the <em>center of
	 * mass</em>.
	 * <p>
	 * <strong>Note:</strong> range of events is assumed to be in
	 * <code>[0, 1]</code>, i.e. first bin at <code>0</code> and last bin at
	 * <code>1</code>.
	 * </p>
	 * 
	 * @param w probability distribution
	 * @return mean of <code>w</code>
	 */
	public static double distrMean(double[] w) {
		double mean = 0.0;
		int n = w.length;
		double norm = w[0];
		for (int i = 0; i < n; i++) {
			double wi = w[i];
			if (wi <= 0.0)
				continue;
			mean += (i + 0.5) * wi;
			norm += wi;
		}
		if (norm < 1e-8)
			return 0.0;
		return mean / (n * norm);
	}

	/**
	 * Variance of probability distribution with weights <code>w[i]</code> stored in
	 * double vector <code>w</code>. This is the same as the second central moment
	 * of the distribution.
	 * <p>
	 * <strong>Note:</strong> range of events is assumed to be in
	 * <code>[0, 1]</code>, i.e. first bin at <code>0</code> and last bin at
	 * <code>1</code>.
	 * </p>
	 * 
	 * @param w probability distribution
	 * @return variance of <code>w</code>
	 * @see #distrCentralMoment(double[], int)
	 */
	public static double distrVariance(double[] w) {
		return distrCentralMoment(w, 2);
	}

	/**
	 * Variance of probability distribution with weights <code>w[i]</code> stored in
	 * double vector <code>w</code> with known mean <code>m1</code>. This is the
	 * same as the second central moment of the distribution.
	 * <p>
	 * <strong>Note:</strong> range of events is assumed to be in
	 * <code>[0, 1]</code>, i.e. first bin at <code>0</code> and last bin at
	 * <code>1</code>.
	 * </p>
	 * 
	 * @param w  probability distribution
	 * @param m1 mean of <code>w</code>
	 * @return variance of <code>w</code>
	 * @see #distrCentralMoment(double[], int, double)
	 */
	public static double distrVariance(double[] w, double m1) {
		return distrCentralMoment(w, 2, m1);
	}

	/**
	 * Standard deviation of probability distribution with weights <code>w[i]</code>
	 * stored in double vector <code>w</code>.
	 * <p>
	 * <strong>Note:</strong> range of events is assumed to be in
	 * <code>[0, 1]</code>, i.e. first bin at <code>0</code> and last bin at
	 * <code>1</code>.
	 * </p>
	 * 
	 * @param w probability distribution
	 * @return standard deviation of <code>w</code>
	 * @see #distrVariance(double[])
	 */
	public static double distrStdev(double[] w) {
		return Math.sqrt(distrVariance(w));
	}

	/**
	 * Standard deviation of probability distribution with weights <code>w[i]</code>
	 * stored in double vector <code>w</code> with known mean <code>m1</code>.
	 * <p>
	 * <strong>Note:</strong> range of events is assumed to be in
	 * <code>[0, 1]</code>, i.e. first bin at <code>0</code> and last bin at
	 * <code>1</code>.
	 * </p>
	 * 
	 * @param w  probability distribution
	 * @param m1 mean of <code>w</code>
	 * @return standard deviation of <code>w</code>
	 * @see #distrVariance(double[])
	 */
	public static double distrStdev(double[] w, double m1) {
		return Math.sqrt(distrVariance(w, m1));
	}

	/**
	 * <code>m</code>-th moment of probability distribution with weights
	 * <code>w[i]</code> stored in double vector <code>w</code>.
	 * <p>
	 * <strong>Note:</strong> range of events is assumed to be in
	 * <code>[0, 1]</code>, i.e. first bin at <code>0</code> and last bin at
	 * <code>1</code>.
	 * </p>
	 * 
	 * @param w probability distribution
	 * @param m moment
	 * @return <code>m</code>-th moment of <code>w</code>
	 */
	public static double distrMoment(double[] w, int m) {
		int n = w.length;
		if (m == 0)
			return n;
		if (m == 1)
			return distrMean(w);
		double ibins = 1.0 / n;
		double moment = 0.0;
		double norm = w[0];
		for (int i = 0; i < n; i++) {
			double wi = w[i];
			if (wi <= 0.0)
				continue;
			double xi = (i + 0.5) * ibins;
			moment += wi * Combinatorics.pow(xi, m);
			norm += wi;
		}
		if (norm < 1e-8)
			return 0.0;
		return moment / ((n - 1) * norm);
	}

	/**
	 * <code>m</code>-th central moment of probability distribution with weights
	 * <code>w[i]</code> stored in double vector <code>w</code>.
	 * <p>
	 * <strong>Note:</strong> range of events is assumed to be in
	 * <code>[0, 1]</code>, i.e. first bin at <code>0</code> and last bin at
	 * <code>1</code>.
	 * </p>
	 * 
	 * @param w probability distribution
	 * @param m moment
	 * @return <code>m</code>-th moment of <code>w</code>
	 */
	public static double distrCentralMoment(double[] w, int m) {
		if (m == 0)
			return 1.0;
		if (m == 1)
			return 0.0;
		return distrCentralMoment(w, m, distrMean(w));
	}

	/**
	 * <code>m</code>-th central moment of probability distribution with weights
	 * <code>w[i]</code> stored in double vector <code>w</code> with known mean
	 * <code>m1</code>.
	 * <p>
	 * <strong>Note:</strong> range of events is assumed to be in
	 * <code>[0, 1]</code>, i.e. first bin at <code>0</code> and last bin at
	 * <code>1</code>.
	 * </p>
	 * 
	 * @param w  probability distribution
	 * @param m  moment
	 * @param m1 mean of <code>w</code>
	 * @return <code>m</code>-th moment of <code>w</code>
	 * @see #distrVariance(double[])
	 */
	public static double distrCentralMoment(double[] w, int m, double m1) {
		if (m == 0)
			return 1.0;
		if (m == 1)
			return 0.0;
		int n = w.length;
		double ibins = 1.0 / n;
		double moment = 0.0;
		double norm = w[0];
		for (int i = 0; i < n; i++) {
			double wi = w[i];
			if (wi <= 0.0)
				continue;
			double xi = (i + 0.5) * ibins;
			moment += wi * Combinatorics.pow(xi - m1, m);
			norm += wi;
		}
		if (norm < 1e-8)
			return 0.0;
		return moment / ((n - 1) * norm);
	}

	/**
	 * Mean of data points <code>x[i]</code> stored in integer vector
	 * <code>x</code>.
	 * 
	 * @param x data vector
	 * @return mean of <code>x</code>
	 */
	public static double mean(int[] x) {
		return (double) ArrayMath.norm(x) / x.length;
	}

	/**
	 * Mean of data points <code>x[i]</code> stored in float vector <code>x</code>.
	 * 
	 * @param x data vector
	 * @return mean of <code>x</code>
	 */
	public static float mean(float[] x) {
		return ArrayMath.norm(x) / x.length;
	}

	/**
	 * Mean of data points <code>x[i]</code> stored in double vector <code>x</code>.
	 * 
	 * @param x data vector
	 * @return mean of <code>x</code>
	 */
	public static double mean(double[] x) {
		return ArrayMath.norm(x) / x.length;
	}

	/**
	 * <code>m</code>-th moment of data points <code>x[i]</code> stored in double
	 * vector <code>x</code>.
	 * 
	 * @param x data vector
	 * @param m moment
	 * @return <code>m</code>-th moment of <code>x</code>
	 */
	public static double moment(double[] x, int m) {
		int n = x.length;
		if (m == 0)
			return n;
		if (m == 1)
			return mean(x);
		double moment = 0.0;
		for (int i = 0; i < n; i++) {
			moment += Combinatorics.pow(x[i], m);
		}
		return moment / n;
	}

}
