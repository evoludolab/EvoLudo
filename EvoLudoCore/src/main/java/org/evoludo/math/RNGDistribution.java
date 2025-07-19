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

import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.evoludo.math.MersenneTwister.Chronometer;
import org.evoludo.util.Formatter;

/**
 * Collection of distributions of random numbers based on the Mersenne Twister.
 * <p>
 * Currently available random number distributions:
 * <dl>
 * <dt>Uniform:</dt>
 * <dd>Uniformly distributed random numbers with support
 * <code>[min, max)</code>.</dd>
 * <dt>Exponential:</dt>
 * <dd>Exponentially distributed random numbers with support
 * <code>[0.0, {@link Double#MAX_VALUE})</code></dd>
 * <dt>Normal:</dt>
 * <dd>Normally (or Gaussian) distributed random numbers with support
 * <code>(-{@link Double#MAX_VALUE}, {@link Double#MAX_VALUE})</code>.</dd>
 * <dt>Geometric:</dt>
 * <dd>Geometrically distributed random numbers with support
 * <code>{1,2,3,...}</code>.</dd>
 * <dt>Binomial:</dt>
 * <dd>Binomially distributed random numbers with support
 * <code>{0,1,2,3,...}</code>.</dd>
 * </dl>
 * <p>
 * <strong>Note:</strong> in order to permit 100% reproducible results it is
 * essential that all models <em>must</em> share a single instance of the random
 * number generator (RNG). By the same token, GUI methods that require random
 * numbers (e.g. for laying out networks) <em>must</em> use a different instance
 * of the RNG. If an initial <code>seed</code> was set for the models' RNG then
 * it is recommended that a reproducible seed (the same seed) is also set for
 * the RNG used by the GUI. This ensures that e.g. identical layouts can be
 * re-generated while not interfering with the model calculations.
 * </p>
 * 
 * @see MersenneTwister
 * @see <a href="http://www.math.sci.hiroshima-u.ac.jp/~m-mat/MT/emt.html"> The
 *      Mersenne Twister Home Page</a>
 * 
 * @author Christoph Hauert
 */
public abstract class RNGDistribution {

	/**
	 * Reference to the {@link MersenneTwister} that supplies the random numbers for
	 * the different distributions.
	 */
	protected MersenneTwister rng;

	/**
	 * Seed for random number generator.
	 */
	protected long seed = -1L;

	/**
	 * <code>true</code> if seed was set.
	 */
	protected boolean seedSet = false;

	/**
	 * number of samples for tests.
	 */
	protected int testSamples = 1000000; // 10^6

	/**
	 * Create new random number distribution using <code>rng</code> as the random
	 * number generator. If <code>rng==null</code> a new instance of the
	 * {@link MersenneTwister} random number generator is assigned to distribution.
	 * <p>
	 * Protected default constructor to ensure non-instantiability.
	 * </p>
	 * 
	 * @param rng random number generator
	 */
	protected RNGDistribution(MersenneTwister rng) {
		this.rng = (rng == null ? new MersenneTwister(new Date().getTime()) : rng);
		if (seedSet)
			setRNGSeed(seed);
	}

	/**
	 * Set custom random number generator <code>rng</code> of this distribution.
	 * 
	 * @param rng random number generator
	 */
	public void setRNG(MersenneTwister rng) {
		if (rng != null)
			this.rng = rng;
	}

	/**
	 * Get random number generator of this distribution.
	 * 
	 * @return random number generator of distribution
	 */
	public MersenneTwister getRNG() {
		return rng;
	}

	/**
	 * Uniformly distributed random number from interval <code>[0, 1)</code> with
	 * regular precision (based on 31bit random integer).
	 *
	 * @return random number in <code>[0, 1)</code>
	 * @see MersenneTwister#nextDouble()
	 */
	public double random01() {
		return rng.nextDouble();
	}

	/**
	 * Uniformly distributed random number from interval [0, 1) with high precision
	 * (based on 53bit random integer).
	 *
	 * @return high precision random number in <code>[0, 1)</code>
	 * @see MersenneTwister#nextDoubleHigh()
	 */
	public double random01d() {
		return rng.nextDoubleHigh();
	}

	/**
	 * Uniformly distributed integer random number from interval
	 * <code>[0, n]</code>.
	 * 
	 * @param n upper bound (inclusive)
	 * @return random integer in <code>[0, n]</code>
	 * @see MersenneTwister#nextInt(int)
	 */
	public int random0N(int n) {
		return rng.nextInt(n + 1);
	}

	/**
	 * Uniformly distributed integer random number from interval
	 * <code>[0, n)</code>.
	 * 
	 * @param n integer upper bound (exclusive)
	 * @return random integer in <code>[0, n)</code>
	 * @see MersenneTwister#nextInt(int)
	 */
	public int random0n(int n) {
		return rng.nextInt(n);
	}

	/**
	 * Uniformly distributed random integer in
	 * <code>[0, {@link Integer#MAX_VALUE})</code>.
	 * <p>
	 * Note: compatibility method with {@link java.util.Random}.
	 * </p>
	 * 
	 * @return random integer in <code>[0, {@link Integer#MAX_VALUE})</code>
	 * @see MersenneTwister#nextInt()
	 */
	public int nextInt() {
		return rng.nextInt();
	}

	/**
	 * Uniformly distributed random integer in <code>[0, n)</code>.
	 * <p>
	 * Note: compatibility method with {@link java.util.Random}.
	 * </p>
	 * 
	 * @param n integer upper bound (exclusive)
	 * @return random integer in <code>[0, n)</code>
	 * @see MersenneTwister#nextInt(int)
	 */
	public int nextInt(int n) {
		return rng.nextInt(n);
	}

	/**
	 * Uniformly distributed random byte in
	 * <code>[0, {@link Byte#MAX_VALUE})</code>.
	 * <p>
	 * Note: compatibility method with {@link java.util.Random}.
	 * </p>
	 * 
	 * @return random byte in <code>[0, 127]</code>
	 * @see MersenneTwister#nextByte()
	 */
	public byte nextByte() {
		return rng.nextByte();
	}

	/**
	 * Fill byte array <code>bytes</code> with uniformly distributed random bytes in
	 * <code>[0, {@link Byte#MAX_VALUE})</code>.
	 * <p>
	 * Note: compatibility method with {@link java.util.Random}.
	 * </p>
	 * 
	 * @param bytes array to fill with uniformly distributed random bytes in
	 *              <code>[0, 127]</code>
	 * @see MersenneTwister#nextBytes(byte[])
	 */
	public void nextBytes(byte[] bytes) {
		rng.nextBytes(bytes);
	}

	/**
	 * Uniformly distributed random boolean.
	 * <p>
	 * Note: compatibility method with {@link java.util.Random}.
	 * </p>
	 * 
	 * @return <code>true</code> with probability <code>0.5</code>
	 * @see MersenneTwister#nextBoolean()
	 */
	public boolean nextBoolean() {
		return rng.nextBoolean();
	}

	/**
	 * Uniformly distributed random number from interval <code>[0, 1)</code> with
	 * regular precision (based on 31bit random integer).
	 *
	 * @return random number in <code>[0, 1)</code>
	 * @see MersenneTwister#nextDouble()
	 */
	public double nextDouble() {
		return rng.nextDouble();
	}

	/**
	 * Gaussian distributed random number with mean <code>0</code> and variance
	 * <code>1</code> (standard Normal distribution) with regular precision (based
	 * on 31bit random integer).
	 *
	 * @return random number in <code>(-Double.MAX_VALUE , Double.MAX_VALUE)</code>
	 * @see MersenneTwister#nextGaussian()
	 */
	public synchronized double nextGaussian() {
		return rng.nextGaussian();
	}

	/**
	 * Set seed of random number generator to <code>seed</code>. Since
	 * MersenneTwister only uses lower 32bits of long, <code>seed</code> is
	 * truncated accordingly to fall in interval <code>[0, 2<sup>33</sup>-1]</code>.
	 * 
	 * @param seed for random number generator
	 * @see MersenneTwister#setSeed(long)
	 */
	public void setRNGSeed(long seed) {
		this.seedSet = true;
		this.seed = Math.abs(seed) & (0xffffffff);
		setRNGSeed();
	}

	/**
	 * Pass seed to random number generator.
	 * 
	 * @see MersenneTwister#setSeed(long)
	 */
	public void setRNGSeed() {
		rng.setSeed(seed);
	}

	/**
	 * Retrieve seed of random number generator in
	 * <code>[0, 2<sup>33</sup>-1]</code>. If no seed has been set, return
	 * <code>-1L</code>.
	 * 
	 * @return seed of random number generator or <code>-1L</code> if seed has not
	 *         been set
	 */
	public long getRNGSeed() {
		if (!seedSet)
			return -1L;
		return seed;
	}

	/**
	 * Clear seed for random number generator.
	 */
	public void clearRNGSeed() {
		seedSet = false;
	}

	/**
	 * Check if seed of random number generator has been set.
	 * 
	 * @return <code>true</code> if seed set
	 */
	public boolean isRNGSeedSet() {
		return seedSet;
	}

	/**
	 * Clone this RNGDistribution to ensure both objects return identical sequences
	 * of random numbers.
	 * <p>
	 * IMPORTANT:
	 * <ol>
	 * <li>overrides {@link java.lang.Object#clone() clone()} in
	 * {@link java.lang.Object} but conflicts with GWT's aversion to
	 * clone()ing...</li>
	 * <li>remove <code>@SuppressWarnings("all")</code> to ensure that no other
	 * issues crept in when modifying method.</li>
	 * </ol>
	 * 
	 * @return clone of RNGDistribution
	 */
	// @Override
	@SuppressWarnings("all")
	public abstract RNGDistribution clone();

	/**
	 * Copy settings of <code>this</code> to <code>clone</code>.
	 * 
	 * @param clone cloned <code>RNGDistribution</code>
	 */
	protected void clone(RNGDistribution clone) {
		clone.seed = this.seed;
		clone.seedSet = this.seedSet;
		clone.testSamples = this.testSamples;
	}

	/**
	 * Uniformly distributed random numbers with support <code>[min, max)</code>.
	 * 
	 * @see <a href=
	 *      "https://en.wikipedia.org/wiki/Uniform_distribution_(continuous)">
	 *      Wikipedia: Uniform distribution</a>
	 */
	public static class Uniform extends RNGDistribution {

		/**
		 * Lower bound of uniform interval (inclusive).
		 */
		private double min;

		/**
		 * Width of interval of uniform random numbers, <code>max-min</code>.
		 */
		private double range;

		/**
		 * Creates uniform random distribution in interval <code>[0,1)</code> with new
		 * instance of {@link MersenneTwister}.
		 */
		public Uniform() {
			this(null, 0.0, 1.0);
		}

		/**
		 * Creates uniform random distribution in interval <code>[min, max)</code> with
		 * new instance of {@link MersenneTwister}.
		 *
		 * @param min lower bound of uniform interval (inclusive)
		 * @param max upper bound of uniform interval (exclusive)
		 */
		public Uniform(double min, double max) {
			this(null, min, max);
		}

		/**
		 * Creates uniform random distribution in interval <code>[min, max)</code> with
		 * random number generator <code>rng</code>.
		 *
		 * @param rng random number generator
		 * @param min lower bound of uniform interval (inclusive)
		 * @param max upper bound of uniform interval (exclusive)
		 * @throws IllegalArgumentException if <code>max&le;min</code>
		 * @see MersenneTwister
		 */
		public Uniform(MersenneTwister rng, double min, double max) throws IllegalArgumentException {
			super(rng);
			if (max <= min)
				throw new IllegalArgumentException("max<=min.");
			this.min = min;
			range = max - min;
		}

		/**
		 * Get the lower bound of the uniform distribution (inclusive).
		 * 
		 * @return the lower bound
		 */
		public double getMin() {
			return min;
		}

		/**
		 * Get the upper bound of the uniform distribution (exclusive).
		 * 
		 * @return the upper bound
		 */
		public double getMax() {
			return min + range;
		}

		/**
		 * Get the range of the uniform distribution.
		 * 
		 * @return the range of the interval <code>max-min</code>
		 */
		public double getRange() {
			return range;
		}

		/**
		 * Generate a uniformly distributed random number from interval
		 * <code>[min, max)</code>.
		 * 
		 * @return the uniformly distributed random number
		 */
		public double next() {
			return min + random01() * range;
		}

		@Override
		public Uniform clone() {
			Uniform clone = new Uniform(rng.clone(), min, min + range);
			clone(clone);
			return clone;
		}

		/**
		 * Uniformly distributed random number in <code>[min, max)</code> using random
		 * number generator <code>rng</code>.
		 * <p>
		 * <strong>Note:</strong> no performance difference between the instance method
		 * {@link #next()} and this static counterpart.
		 * 
		 * @param rng random number generator
		 * @param min lower bound of uniform interval (inclusive)
		 * @param max upper bound of uniform interval (exclusive)
		 * @return uniformly distributed random number from interval
		 *         <code>[min, max)</code>
		 * @throws IllegalArgumentException if <code>max&le;min</code>
		 */
		public static double next(MersenneTwister rng, double min, double max) {
			if (max <= min)
				throw new IllegalArgumentException("max<=min.");
			return min + rng.nextDouble() * (max - min);
		}

		/**
		 * Test Uniform distribution.
		 * 
		 * The test samples the distribution and bins the random numbers. This sample
		 * distribution is compared to the theoretical expectation. The mean deviation
		 * is the mean difference between the actual number of events in each bin and
		 * their expected number. For a perfect match the mean deviation is
		 * <code>0</code>. The test passes if the mean deviation lies within one
		 * standard error from <code>0</code>. This is more stringent than the
		 * traditional 95% confidence interval.
		 * 
		 * @param rng    the random number generator
		 * @param logger the logger for reporting results
		 * @param clock  the stop watch
		 * 
		 * @see <a href="https://en.wikipedia.org/wiki/Standard_error">Wikipedia:
		 *      Standard error</a>
		 */
		public static void test(MersenneTwister rng, Logger logger, Chronometer clock) {
			double min = -10.0;
			double max = 10.0;
			double irange = 1.0 / (max - min);
			RNGDistribution.Uniform uniform = new Uniform(rng, min, max);
			int nBins = 101;
			int[] bins = new int[nBins];
			int nSamples = uniform.testSamples;
			StringBuilder buffer = new StringBuilder();
			logger.info("Testing Uniform distribution [" + min + ", " + max + "): " + nSamples + " samples...");
			double msStart = clock.elapsedTimeMsec();
			for (int n = 0; n < nSamples; n++) {
				double rnd = uniform.next();
				int idx = (int) Math.floor((rnd - min) * irange * nBins);
				bins[idx]++;
			}
			double msEnd = clock.elapsedTimeMsec();
			boolean verbose = (logger.getLevel().intValue() <= Level.FINE.intValue());
			buffer.append("Time elapsed: " + (msEnd - msStart) + " msec\n");
			if (verbose)
				buffer.append("Distribution:\n");
			double low = min;
			double incr = (max - min) / nBins;
			double m1 = 0.0;
			double m2 = 0.0;
			double refn1 = 0.0;
			for (int n = 0; n < nBins; n++) {
				double high = low + incr;
				double refn = cdf(high, min, max) * nSamples;
				int binn = bins[n];
				if (verbose)
					buffer.append("[" + (int) (low * 100.0) * 0.01 + ", " + (int) (high * 100.0) * 0.01 + "): " + binn
							+ " (" + (int) ((refn - refn1) * 100.0) * 0.01 + ")\n");
				double d = binn - (refn - refn1);
				m1 += d;
				m2 += d * d;
				low = high;
				refn1 = refn;
			}
			m1 /= nSamples;
			m2 /= nSamples;
			double stdev = Math.sqrt(m2 - m1 * m1);
			double sterr = stdev / Math.sqrt(nSamples);
			buffer.append("Statistics: mean +/- SEM = " + m1 + " +/- " + sterr);
			logger.info(buffer.toString());
			// in order to pass the test, the mean+/-sterr must include 0
			boolean success = (Math.abs(m1) < sterr);
			if (success) {
				logger.info("Test passed!");
				return;
			}
			logger.severe("Test of Uniform distribution failed...");
		}

		/**
		 * Helper method to calculate the cumulative distribution function,
		 * <code>cdf</code>, of the Uniform distribution.
		 * 
		 * @param x   the value for which to calculate the <code>cdf</code>
		 * @param min the minimum value
		 * @param max the maximum value
		 * @return <code>cdf</code> at <code>x</code>
		 */
		private static double cdf(double x, double min, double max) {
			return (x - min) / (max - min);
		}
	}

	/**
	 * Exponentially distributed random numbers with support
	 * <code>[0.0, {@link Double#MAX_VALUE})</code>.
	 * 
	 * @see <a href= "https://en.wikipedia.org/wiki/Exponential_distribution">
	 *      Wikipedia: Exponential distribution</a>
	 */
	public static class Exponential extends RNGDistribution {

		/**
		 * The mean of exponential distribution.
		 */
		private double mean;

		/**
		 * Creates exponential distribution with <code>mean==1</code> and a new instance
		 * of {@link MersenneTwister}.
		 */
		public Exponential() {
			this(null, 1.0);
		}

		/**
		 * Creates exponential distribution with <code>mean</code> and a new instance of
		 * {@link MersenneTwister}.
		 * 
		 * @param mean the mean of the exponential distribution
		 */
		public Exponential(double mean) {
			this(null, mean);
		}

		/**
		 * Creates exponential distribution with <code>mean</code> and random number
		 * generator <code>rng</code>.
		 *
		 * @param rng  the random number generator
		 * @param mean the mean of the exponential distribution
		 * @throws IllegalArgumentException if <code>man&le;0</code>
		 * @see MersenneTwister
		 */
		public Exponential(MersenneTwister rng, double mean) throws IllegalArgumentException {
			super(rng);
			if (mean <= 0.0)
				throw new IllegalArgumentException("mean must be >0.");
			this.mean = mean;
		}

		/**
		 * Get the mean of the exponential distribution.
		 * 
		 * @return the mean
		 */
		public double getMean() {
			return mean;
		}

		/**
		 * Generate an exponentially distributed random number with <code>mean</code>.
		 * 
		 * @return the exponentially distributed random number
		 */
		public double next() {
			return -Math.log1p(-random01()) * mean;
		}

		@Override
		public Exponential clone() {
			Exponential clone = new Exponential(rng.clone(), mean);
			clone(clone);
			return clone;
		}

		/**
		 * Exponentially distributed random number with <code>mean</code> using random
		 * number generator <code>rng</code>.
		 * <p>
		 * <strong>Note:</strong> no performance difference between the instance method
		 * {@link #next()} and this static counterpart.
		 * 
		 * @param rng  random number generator
		 * @param mean of exponential distribution
		 * @return exponentially distributed random number with <code>mean</code>
		 * @throws IllegalArgumentException if <code>man&le;0</code>
		 */
		public static double next(MersenneTwister rng, double mean) {
			if (mean <= 0.0)
				throw new IllegalArgumentException("mean must be >0.");
			return -Math.log1p(-rng.nextDouble()) * mean;
		}

		/**
		 * Test Exponential distribution.
		 * <p>
		 * The test samples the distribution and bins the random numbers. This sample
		 * distribution is compared to the theoretical expectation. The mean deviation
		 * is the mean difference between the actual number of events in each bin and
		 * their expected number. For a perfect match the mean deviation is
		 * <code>0</code>. The test passes if the mean deviation lies within one
		 * standard error from <code>0</code>. This is more stringent than the
		 * traditional 95% confidence interval.
		 * 
		 * @param rng    the random number generator
		 * @param logger the logger for reporting results
		 * @param clock  the stop watch
		 * 
		 * @see <a href="https://en.wikipedia.org/wiki/Standard_error">Wikipedia:
		 *      Standard error</a>
		 */
		public static void test(MersenneTwister rng, Logger logger, Chronometer clock) {
			double mean = 10.0;
			double range = 20.0 * mean;
			RNGDistribution.Exponential exponential = new Exponential(rng, mean);
			int nBins = 101;
			int[] bins = new int[nBins];
			double map = (nBins - 1) / range;
			int nSamples = exponential.testSamples;
			StringBuilder buffer = new StringBuilder();
			logger.info("Testing Exponential distribution: " + nSamples + " samples...");
			double msStart = clock.elapsedTimeMsec();
			for (int n = 0; n < nSamples; n++) {
				double rnd = exponential.next();
				int idx = Math.min((int) Math.floor(rnd * map), nBins - 1);
				bins[idx]++;
			}
			double msEnd = clock.elapsedTimeMsec();
			boolean verbose = (logger.getLevel().intValue() <= Level.FINE.intValue());
			buffer.append("Time elapsed: " + (msEnd - msStart) + " msec\n");
			if (verbose)
				buffer.append("Distribution:\n");
			double low = 0.0;
			double incr = range / nBins;
			double m1 = 0.0;
			double m2 = 0.0;
			double refn1 = 0.0;
			for (int n = 0; n < nBins; n++) {
				double high = low + incr;
				double refn = cdf(high, mean) * nSamples;
				int binn = bins[n];
				if (verbose)
					buffer.append("[" + (int) (low * 100.0) * 0.01 + ", " + (int) (high * 100.0) * 0.01 + "): " + binn
							+ " (" + (int) ((refn - refn1) * 100.0) * 0.01 + ")\n");
				double d = binn - (refn - refn1);
				m1 += d;
				m2 += d * d;
				low = high;
				refn1 = refn;
			}
			m1 /= nSamples;
			m2 /= nSamples;
			double stdev = Math.sqrt(m2 - m1 * m1);
			double sterr = stdev / Math.sqrt(nSamples);
			buffer.append("Statistics: mean +/- SEM = " + m1 + " +/- " + sterr);
			logger.info(buffer.toString());
			// in order to pass the test, the mean+/-sterr must include 0
			boolean success = (Math.abs(m1) < sterr);
			if (success) {
				logger.info("Test passed!");
				return;
			}
			logger.severe("Test of Exponential distribution failed...");
		}

		/**
		 * Helper method to calculate the cumulative distribution function,
		 * <code>cdf</code>, of the Exponential distribution.
		 * 
		 * @param x    the value for which to calculate the <code>cdf</code>
		 * @param mean the mean value
		 * @return <code>cdf</code> at <code>x</code>
		 */
		private static double cdf(double x, double mean) {
			return 1.0 - Math.exp(-x / mean);
		}
	}

	/**
	 * Normally (or Gaussian) distributed random numbers with support
	 * <code>(-{@link Double#MAX_VALUE}, {@link Double#MAX_VALUE})</code>.
	 * 
	 * @see <a href= "https://en.wikipedia.org/wiki/Normal_distribution"> Wikipedia:
	 *      Normal distribution</a>
	 */
	public static class Normal extends RNGDistribution {

		/**
		 * The mean of the Normal distribution.
		 */
		private double mean;

		/**
		 * The standard deviation of the Normal distribution.
		 */
		private double stdev;

		/**
		 * Creates a standard Normal distribution with <code>mean==0</code> and standard
		 * deviation <code>stdev==1</code> using and a new instance of
		 * {@link MersenneTwister}.
		 */
		public Normal() {
			this(null, 0.0, 1.0);
		}

		/**
		 * Creates Normal distribution with <code>mean</code> and standard deviation
		 * <code>stdev</code> using and a new instance of {@link MersenneTwister}.
		 *
		 * @param mean  of Normal distribution
		 * @param stdev of Normal distribution
		 */
		public Normal(double mean, double stdev) {
			this(null, mean, stdev);
		}

		/**
		 * Creates Normal distribution with <code>mean</code> and standard deviation
		 * <code>stdev</code> using the random number generator <code>rng</code>.
		 *
		 * @param rng   random number generator
		 * @param mean  of Normal distribution
		 * @param stdev of Normal distribution
		 * @throws IllegalArgumentException if <code>man&le;0</code>
		 * @see MersenneTwister
		 */
		public Normal(MersenneTwister rng, double mean, double stdev) throws IllegalArgumentException {
			super(rng);
			if (stdev <= 0.0)
				throw new IllegalArgumentException("standard deviation must be >0.");
			this.mean = mean;
			this.stdev = stdev;
		}

		/**
		 * Get the mean of the Normal distribution.
		 * 
		 * @return the mean
		 */
		public double getMean() {
			return mean;
		}

		/**
		 * Get the standard deviation of the Normal distribution.
		 * 
		 * @return the standard deviation
		 */
		public double getStdev() {
			return stdev;
		}

		/**
		 * Generate a normally distributed random number with <code>mean</code> and
		 * standard deviation <code>stdev</code>.
		 * 
		 * @return the Normally distributed random number
		 */
		public double next() {
			return mean + stdev * rng.nextGaussian();
		}

		@Override
		public Normal clone() {
			Normal clone = new Normal(rng.clone(), mean, stdev);
			clone(clone);
			return clone;
		}

		/**
		 * Normally distributed random number with <code>mean</code> and standard
		 * deviation <code>stdev</code> using random number generator <code>rng</code>.
		 * <p>
		 * <strong>Note:</strong> no performance difference between the instance method
		 * {@link #next()} and this static counterpart.
		 * 
		 * @param rng   random number generator
		 * @param mean  the mean of the Normal distribution
		 * @param stdev the standard deviation of the Normal distribution
		 * @return Normally distributed random number
		 */
		public static double next(MersenneTwister rng, double mean, double stdev) {
			if (stdev <= 0.0)
				throw new IllegalArgumentException("standard deviation must be >0.");
			return mean + stdev * rng.nextGaussian();
		}

		/**
		 * Test Normal distribution.
		 * <p>
		 * The test samples the distribution and bins the random numbers. This sample
		 * distribution is compared to the theoretical expectation. The mean deviation
		 * is the mean difference between the actual number of events in each bin and
		 * their expected number. For a perfect match the mean deviation is
		 * <code>0</code>. The test passes if the mean deviation lies within one
		 * standard error from <code>0</code>. This is more stringent than the
		 * traditional 95% confidence interval.
		 * 
		 * @param rng    the random number generator
		 * @param logger the logger for reporting results
		 * @param clock  the stop watch
		 * 
		 * @see <a href="https://en.wikipedia.org/wiki/Standard_error">Wikipedia:
		 *      Standard error</a>
		 */
		public static void test(MersenneTwister rng, Logger logger, Chronometer clock) {
			double mean = 10.0;
			double stdev = 3.0;
			double range = 8.0 * stdev;
			RNGDistribution.Normal normal = new Normal(rng, mean, stdev);
			int nBins = 102;
			int[] bins = new int[nBins];
			double min = mean - range * 0.5;
			double map = (nBins - 3) / range;
			int nSamples = normal.testSamples;
			StringBuilder buffer = new StringBuilder();
			logger.info("Testing Normal distribution: " + nSamples + " samples...");
			double msStart = clock.elapsedTimeMsec();
			for (int n = 0; n < nSamples; n++) {
				double rnd = normal.next();
				int idx = Math.max(Math.min((int) Math.floor((rnd - min) * map) + 1, nBins - 1), 0);
				bins[idx]++;
			}
			double msEnd = clock.elapsedTimeMsec();
			boolean verbose = (logger.getLevel().intValue() <= Level.FINE.intValue());
			buffer.append("Time elapsed: " + (msEnd - msStart) + " msec\n");
			if (verbose)
				buffer.append("Distribution:\n");
			double low = min;
			double incr = range / (nBins - 3);
			double refn1 = cdf(min, mean, stdev) * nSamples;
			double m1 = bins[0] - refn1;
			double m2 = m1 * m1;
			if (verbose)
				buffer.append("(-oo, " + (int) (low * 100.0) * 0.01 + "): " + bins[0] + " ("
						+ (int) (refn1 * 100.0) * 0.01 + ")\n");
			for (int n = 1; n < nBins - 2; n++) {
				double high = low + incr;
				double refn = cdf(high, mean, stdev) * nSamples;
				int binn = bins[n];
				if (verbose)
					buffer.append("[" + (int) (low * 100.0) * 0.01 + ", " + (int) (high * 100.0) * 0.01 + "): " + binn
							+ " (" + (int) ((refn - refn1) * 100.0) * 0.01 + ")\n");
				double d = binn - (refn - refn1);
				m1 += d;
				m2 += d * d;
				low = high;
				refn1 = refn;
			}
			double d = bins[nBins - 1] - (nSamples - refn1);
			m1 += d;
			m2 += d * d;
			if (verbose)
				buffer.append("[" + (int) (low * 100.0) * 0.01 + ", oo): " + bins[nBins - 1] + " ("
						+ (int) ((nSamples - refn1) * 100.0) * 0.01 + ")\n");
			m1 /= nSamples;
			m2 /= nSamples;
			double sdev = Math.sqrt(m2 - m1 * m1);
			double sterr = sdev / Math.sqrt(nSamples);
			buffer.append("Statistics: mean +/- SEM = " + m1 + " +/- " + sterr);
			logger.info(buffer.toString());
			// in order to pass the test, the mean+/-sterr must include 0
			boolean success = (Math.abs(m1) < sterr);
			if (success) {
				logger.info("Test passed!");
				return;
			}
			logger.severe("Test of Normal distribution failed...");
		}

		/**
		 * Helper method to calculate the cumulative distribution function,
		 * <code>cdf</code>, of the Normal distribution.
		 * 
		 * @param x     the value for which to calculate the <code>cdf</code>
		 * @param mean  the mean value
		 * @param stdev the standard deviation
		 * @return <code>cdf</code> at <code>x</code>
		 * 
		 * @see <a href="https://en.wikipedia.org/wiki/Error_function">Wikipedia: Error
		 *      function</a>
		 */
		private static double cdf(double x, double mean, double stdev) {
			// use numerical approximation of error function
			final double isqrt2 = 0.7071067811865475;
			double xs = (x - mean) / stdev;
			return 0.5 + erf(xs * isqrt2) * 0.5;
		}

		/**
		 * Numerical approximation of error function <code>erf(x)</code> from Abramowitz
		 * &amp; Stegun.
		 * <p>
		 * <strong>Note:</strong> approximation requires <code>x&ge;0</code>. For
		 * <code>x&lt;0</code> use that <code>erf(x)</code> is odd function.
		 * 
		 * @param x the function argument
		 * @return <code>erf(x)</code>
		 */
		private static double erf(double x) {
			final double p = 0.3275911;
			final double a1 = 0.254829592;
			final double a2 = -0.284496736;
			final double a3 = 1.421413741;
			final double a4 = -1.453152027;
			final double a5 = 1.061405429;
			double t = 1.0 / (1.0 + p * Math.abs(x));
			double t2 = t * t;
			double erf = 1.0 - (a1 * t + a2 * t2 + a3 * t * t2 + a4 * t2 * t2 + a5 * t2 * t2 * t) * Math.exp(-x * x);
			if (x < 0)
				return -erf;
			return erf;
		}
	}

	/**
	 * Geometrically distributed random numbers with support
	 * <code>{1,2,3,...}</code>.
	 * 
	 * @see <a href= "https://en.wikipedia.org/wiki/Geometric_distribution">
	 *      Wikipedia: Geometric distribution</a>
	 */
	public static class Geometric extends RNGDistribution {

		/**
		 * Cumulative distribution function of geometric probability distribution.
		 */
		private double[] cdf;

		/**
		 * Success probability of single trial.
		 */
		private double p;

		/**
		 * Mean of geometric distribution.
		 */
		private double mean;

		/**
		 * Creates geometric distribution with success probability <code>p</code> (mean
		 * <code>1/p</code>) and a new instance of {@link MersenneTwister}.
		 *
		 * @param p success probability of single trial
		 */
		public Geometric(double p) {
			this(null, p);
		}

		/**
		 * Creates geometric distribution with success probability <code>p</code> (mean
		 * <code>1/p</code>) and the random number generator <code>rng</code>.
		 *
		 * @param rng random number generator
		 * @param p   success probability of single trial
		 */
		public Geometric(MersenneTwister rng, double p) {
			super(rng);
			setProbability(p);
		}

		/**
		 * Set the probability of success <code>p</code> for the geometric distribution.
		 * <p>
		 * <strong>Note:</strong>
		 * <ul>
		 * <li>If <code>p&lt;10<sup>-4</sup></code> the geometric distribution is
		 * approximated by an exponential distribution.</li>
		 * <li>cumulative probabilities are limited to <code>50*mean</code> trials.</li>
		 * </ul>
		 *
		 * @param p probability of success of single trial
		 * @throws IllegalArgumentException if <code>p&le;0</code> or
		 *                                  <code>p&ge;1</code>
		 */
		public void setProbability(double p) throws IllegalArgumentException {
			if (p <= 0.0 || p >= 1.0)
				throw new IllegalArgumentException("success probability must be in (0, 1).");
			this.p = p;
			if (cdf != null && Math.abs(cdf[1] - p) < 1e-8) {
				// success probability did not change - nothing to do
				return;
			}
			if (p < 1e-4) {
				// p too small, use exponential distribution as an approximation
				// adjust mean accordingly
				cdf = null;
				mean = Math.floor(1.0 / p + 0.5);
				return;
			}
			mean = 1.0 / p;
			int bins = (int) (50.0 * mean + 0.5);
			double prod = p;
			double q = 1.0 - p;
			if (cdf == null || cdf.length != bins) {
				cdf = new double[bins];
			}
			cdf[0] = 0.0;
			cdf[1] = p;
			for (int i = 2; i < bins - 1; i++) {
				prod *= q;
				cdf[i] = cdf[i - 1] + prod;
			}
			if (cdf[bins - 2] < 0.9999) {
				System.out.println("WARNING: deviation too big... (" + Formatter.format(cdf[bins - 2], 6)
						+ ">0.999 should hold)!");
			}
			cdf[bins - 1] = 1.0;
		}

		/**
		 * Get the success probability of a single trial.
		 * 
		 * @return the success probability of a single trial
		 */
		public double getProbability() {
			return p;
		}

		/**
		 * Generate a geometrically distributed random number with success probability
		 * <code>p</code>.
		 * 
		 * @return the number of trials until first success
		 */
		public int next() {
			double uRand = random01();

			if (cdf == null)
				// use exponential distribution
				return Math.max((int) Math.ceil(-Math.log1p(-uRand) * mean), 1);

			// binary search - start at expected value
			int xmin = 0, xmax = cdf.length - 1, x = (int) mean;

			while (xmax - xmin > 1) {
				if (uRand > cdf[x]) {
					xmin = x;
					x += (xmax - x) / 2;
				} else {
					xmax = x;
					x -= (x - xmin) / 2;
				}
			}
			return xmax;
		}

		@Override
		public Geometric clone() {
			Geometric clone = new Geometric(rng.clone(), p);
			clone(clone);
			return clone;
		}

		/**
		 * Creates geometric distribution with success probability <code>p</code> (mean
		 * <code>1/p</code>) and the random number generator <code>rng</code>.
		 * <p>
		 * <strong>Note:</strong> cumulative distribution function is generated on the
		 * fly. This is fine if only a few geometrically distributed random numbers are
		 * required or if performance is of a minor concern. Otherwise use
		 * {@link RNGDistribution.Geometric#Geometric(double) Geometric(double)} and
		 * {@link #next()}.
		 *
		 * @param rng random number generator
		 * @param p   probability of success of single trial
		 * @return number of trials until first success
		 */
		public static int next(MersenneTwister rng, double p) {
			if (p <= 0.0 || p >= 1.0)
				throw new IllegalArgumentException("success probability must be in (0, 1).");
			if (p < 1e-4) {
				// p too small, use exponential distribution as an approximation
				return Math.max((int) Math.ceil(-Math.log1p(-rng.nextDouble()) * Math.floor(1.0 / p + 0.5)), 1);
			}
			double rnd = rng.nextDouble();
			int n = 1;
			double p0 = 1.0 - p;
			double pn = p;
			double cum = pn;
			while (rnd >= cum) {
				pn *= p0;
				cum += pn;
				n++;
			}
			return n;
		}

		/**
		 * Test Geometric distribution.
		 * <p>
		 * The test samples the distribution and bins the random numbers. This sample
		 * distribution is compared to the theoretical expectation. The mean deviation
		 * is the mean difference between the actual number of events in each bin and
		 * their expected number. For a perfect match the mean deviation is
		 * <code>0</code>. The test passes if the mean deviation lies within one
		 * standard error from <code>0</code>. This is more stringent than the
		 * traditional 95% confidence interval.
		 * 
		 * @param rng    the random number generator
		 * @param logger the logger for reporting results
		 * @param clock  the stop watch
		 */
		public static void test(MersenneTwister rng, Logger logger, Chronometer clock) {
			double mean = 7.5;
			double p = 1.0 / mean;
			RNGDistribution.Geometric geometric = new Geometric(rng, p);
			int nBins = 101;
			int[] bins = new int[nBins];
			int nSamples = geometric.testSamples;
			StringBuilder buffer = new StringBuilder();
			logger.info("Testing Geometric distribution: " + nSamples + " samples...");
			double msStart = clock.elapsedTimeMsec();
			for (int n = 0; n < nSamples; n++) {
				int rnd = geometric.next();
				// int rnd = Geometric.next(rng, p);
				int idx = Math.min(rnd - 1, nBins - 1);
				bins[idx]++;
			}
			double msEnd = clock.elapsedTimeMsec();
			boolean verbose = (logger.getLevel().intValue() <= Level.FINE.intValue());
			buffer.append("Time elapsed: " + (msEnd - msStart) + " msec\n");
			if (verbose)
				buffer.append("Distribution:\n");
			double pmfn = p * nSamples;
			double q = 1.0 - p;
			double m1 = 0.0;
			double m2 = 0.0;
			for (int n = 0; n < nBins - 1; n++) {
				int binn = bins[n];
				if (verbose)
					buffer.append((n + 1) + ": " + binn + " (" + (int) (pmfn * 100.0) * 0.01 + ")\n");
				double d = binn - pmfn;
				m1 += d;
				m2 += d * d;
				pmfn *= q;
			}
			double d = bins[nBins - 1] - pmfn / p;
			m1 += d;
			m2 += d * d;
			if (verbose)
				buffer.append("[" + (nBins - 1) + ", oo): " + bins[nBins - 1] + " (" + (int) (pmfn / p * 100.0) * 0.01
						+ ")\n");
			m1 /= nSamples;
			m2 /= nSamples;
			double sdev = Math.sqrt(m2 - m1 * m1);
			double sterr = sdev / Math.sqrt(nSamples);
			buffer.append("Statistics: mean +/- SEM = " + m1 + " +/- " + sterr);
			logger.info(buffer.toString());
			// in order to pass the test, the mean+/-sterr must include 0
			boolean success = (Math.abs(m1) < sterr);
			if (success) {
				logger.info("Test passed!");
				return;
			}
			logger.severe("Test of Geometric distribution failed...");
		}
	}

	/**
	 * Binomially distributed random numbers with support
	 * <code>{0,1,2,3,..., n}</code>. This represents the number of successes in
	 * <code>n</code> trials each with a probability of success <code>p</code>.
	 * 
	 * @see <a href= "https://en.wikipedia.org/wiki/Binomial_distribution">
	 *      Wikipedia: Binomial distribution</a>
	 */
	public static class Binomial extends RNGDistribution {

		/**
		 * Cumulative distribution function of binomial probability distribution.
		 */
		private double[] cdf;

		/**
		 * Success probability of single trial.
		 */
		private double p;

		/**
		 * Mean number of successes <code>n p</code>.
		 */
		private double mean;

		/**
		 * Creates binomial distribution with <code>n</code> trials and success
		 * probability <code>p</code> (mean <code>n p</code>) and a new instance of
		 * {@link MersenneTwister}..
		 *
		 * @param p success probability of single trial
		 * @param n number of trials
		 */
		public Binomial(double p, int n) {
			this(null, p, n);
		}

		/**
		 * Creates binomial distribution with <code>n</code> trials and success
		 * probability <code>p</code> (mean <code>n p</code>) and the random number
		 * generator <code>rng</code>.
		 *
		 * @param rng random number generator
		 * @param p   success probability of single trial
		 * @param n   number of trials
		 */
		public Binomial(MersenneTwister rng, double p, int n) throws IllegalArgumentException {
			super(rng);
			setProbabilityTrials(p, n);
		}

		/**
		 * Set the probability of success <code>p</code> and the number of trials
		 * <code>n</code>. This calculates the cumulative probabilities for the binomial
		 * distribution.
		 *
		 * @param p probability of success of single trial
		 * @param n number of trials
		 * @throws IllegalArgumentException if <code>p&le;0</code>, <code>p&ge;1</code>
		 *                                  or <code>n&lt;0</code>
		 */
		public void setProbabilityTrials(double p, int n) throws IllegalArgumentException {
			if (p <= 0.0 || p >= 1.0)
				throw new IllegalArgumentException("success probability must be in (0, 1).");
			if (n < 0)
				throw new IllegalArgumentException("number of trials must be >=0.");

			this.p = p;
			mean = p * n;
			if (cdf == null || cdf.length != n + 1) {
				cdf = new double[n + 1];
			}
			double piqni = Combinatorics.pow(1.0 - p, n);
			cdf[0] = piqni;
			double piq = p / (1.0 - p);
			double comb = n;
			double ni = n - 1;
			for (int i = 1; i < n; i++) {
				piqni *= piq;
				cdf[i] = cdf[i - 1] + comb * piqni;
				comb *= (ni--) / (i + 1);
			}
			cdf[n] = 1.0;
		}

		/**
		 * Get the probability of success <code>p</code> of a single trial for the
		 * binomial distribution.
		 * 
		 * @return the success probability of a single trial
		 */
		public double getProbability() {
			return p;
		}

		/**
		 * Get the number of trials <code>n</code> for the binomial distribution.
		 * 
		 * @return the number of trials
		 */
		public int getTrials() {
			return cdf.length - 1;
		}

		/**
		 * Generate the number of successful trials drawn from the binomial
		 * distribution.
		 * 
		 * @return the number of successful trials
		 */
		public int next() {
			double uRand = random01();

			// binary search - start at expected value
			int xmin = 0, xmax = cdf.length - 1, x = (int) mean;

			while (xmax - xmin > 1) {
				if (uRand > cdf[x]) {
					xmin = x;
					x += (xmax - x) / 2;
				} else {
					xmax = x;
					x -= (x - xmin) / 2;
				}
			}
			return xmax;
		}

		@Override
		public Binomial clone() {
			Binomial clone = new Binomial(rng.clone(), p, getTrials());
			clone(clone);
			return clone;
		}

		/**
		 * Creates binomial distribution for <code>n</code> trials with success
		 * probability <code>p</code> (mean <code>n p</code>) and the random number
		 * generator <code>rng</code>.
		 * <p>
		 * <strong>Note:</strong> cumulative distribution function is generated on the
		 * fly. This is fine if only a few binomially distributed random numbers are
		 * required or if performance is of a minor concern. Otherwise use
		 * {@link RNGDistribution.Binomial#Binomial(double, int) Binomial(double, int)}}
		 * and {@link #next()}.
		 *
		 * @param rng random number generator
		 * @param p   probability of success of single trial
		 * @param n   number of trials
		 * @return number of successful trials
		 */
		public static int next(MersenneTwister rng, double p, int n) {
			double rnd = rng.nextDouble();
			double piqni = Combinatorics.pow(1.0 - p, n);
			if (rnd < piqni)
				return 0;
			double piq = p / (1.0 - p);
			double cum = piqni;
			double comb = n;
			double ni = n - 1;
			for (int i = 1; i <= n; i++) {
				piqni *= piq;
				cum += comb * piqni;
				if (rnd < cum)
					return i;
				comb *= (ni--) / (i + 1);
			}
			// last resort
			if (cum < 1.0 && rnd < 1.0)
				return n;
			throw new Error("this is not happening... probability: " + p + ", trials: " + n
					+ ", cumulative probability: " + cum + ", random number: " + rnd);
		}

		/**
		 * Test Binomial distribution.
		 * <p>
		 * The test samples the distribution and bins the random numbers. This sample
		 * distribution is compared to the theoretical expectation. The mean deviation
		 * is the mean difference between the actual number of events in each bin and
		 * their expected number. For a perfect match the mean deviation is
		 * <code>0</code>. The test passes if the mean deviation lies within one
		 * standard error from <code>0</code>. This is more stringent than the
		 * traditional 95% confidence interval.
		 * 
		 * @param rng    the random number generator
		 * @param logger the logger for reporting results
		 * @param clock  the stop watch
		 */
		public static void test(MersenneTwister rng, Logger logger, Chronometer clock) {
			double p = 0.2;
			int n = 100;
			RNGDistribution.Binomial binomial = new Binomial(rng, p, n);
			int[] bins = new int[n + 1];
			int nSamples = binomial.testSamples;
			StringBuilder buffer = new StringBuilder();
			logger.info("Testing Binomial distribution (p=" + p + ", N=" + n + "): " + nSamples + " samples...");
			double msStart = clock.elapsedTimeMsec();
			for (int i = 0; i < nSamples; i++) {
				int idx = binomial.next();
				// int idx = Binomial.next(rng, p, n);
				bins[idx]++;
			}
			double msEnd = clock.elapsedTimeMsec();
			boolean verbose = (logger.getLevel().intValue() <= Level.FINE.intValue());
			buffer.append("Time elapsed: " + (msEnd - msStart) + " msec\n");
			if (verbose)
				buffer.append("Distribution:\n");
			double piqni = Combinatorics.pow(1.0 - p, n) * nSamples;
			double piq = p / (1.0 - p);
			double comb = 1.0;
			double ni = n;
			double m1 = 0.0;
			double m2 = 0.0;
			for (int i = 0; i <= n; i++) {
				int bini = bins[i];
				double pmfn = piqni * comb;
				if (verbose)
					buffer.append(i + ": " + bini + " (" + (int) (pmfn * 100.0) * 0.01 + ")\n");
				double d = bini - pmfn;
				m1 += d;
				m2 += d * d;
				piqni *= piq;
				comb *= (ni--) / (i + 1);
			}
			m1 /= nSamples;
			m2 /= nSamples;
			double sdev = Math.sqrt(m2 - m1 * m1);
			double sterr = sdev / Math.sqrt(nSamples);
			buffer.append("Statistics: mean +/- SEM = " + m1 + " +/- " + sterr);
			logger.info(buffer.toString());
			// in order to pass the test, the mean+/-sterr must include 0
			boolean success = (Math.abs(m1) < sterr);
			if (success) {
				logger.info("Test passed!");
				return;
			}
			logger.severe("Test of Binomial distribution failed...");
		}
	}

	/**
	 * The interface to execute commands in a manner that is agnostic to the
	 * implementation details regarding GWT or JRE environments.
	 */
	public interface TestCommand {

		/**
		 * The command to execute.
		 */
		public void execute();
	}

	/**
	 * Gillespie algorithm for selecting integers with support
	 * <code>{0,1,2,3,..., n}</code> but with different weights.
	 * <p>
	 * <strong>Note:</strong> if multiple random number are drawn using the
	 * <em>same</em> distribution of weights, optimizations are posssible that
	 * require expensive initialization but then drawing the random number is
	 * significantly cheaper.
	 * <p>
	 * For an excellent overview, see Keith Schwarz' website
	 * <a href="http://www.keithschwarz.com/darts-dice-coins/">Darts, Dice, and
	 * Coins: Sampling from a Discrete Distribution</a>. Of particular interest is
	 * Vose's Alias Method, which is also what's implemented in
	 * <a href="https://ccl.northwestern.edu/netlogo/">NetLogo</a>
	 * 
	 * @see <a href= "https://en.wikipedia.org/wiki/Gillespie_algorithm">
	 *      Wikipedia: Gillespie algorithm</a>
	 */
	public static class Gillespie extends RNGDistribution {

		/**
		 * The threshold for switching between the standard and the optimized version of
		 * the Gillespie algorithm.
		 */
		public static final int THRESHOLD_SIZE = 350;

		/**
		 * Creates a weighted distribution over intergers using the Gillespie algorithm
		 * and a new instance of {@link MersenneTwister}.
		 */
		public Gillespie() {
			this(null);
		}

		/**
		 * Creates a weighted distribution over intergers using the Gillespie algorithm
		 * using the random number generator {@code rng}.
		 *
		 * @param rng random number generator
		 */
		public Gillespie(MersenneTwister rng) throws IllegalArgumentException {
			super(rng);
		}

		/**
		 * Return a random integer with support {@code [0, weights.length)} from the
		 * discrete distribution of weights defined by the {@code int[]} array
		 * {@code weights}. Optimized sampling is used above the threshold of
		 * <code> weights.length &gt; {@value #THRESHOLD_SIZE}</code>.
		 * <p>
		 * <strong>Note:</strong> if the sum of weights or the maximum weight is known,
		 * consider using the methods {@code #nextSum(int[], int)} or
		 * {@code #nextMax(int[], int)}, instead.
		 * 
		 * @param weights the array of integer weights of the discrete distribution
		 * @return the random integer
		 */
		public int next(int[] weights) {
			if (weights.length >= THRESHOLD_SIZE)
				return nextMax(weights, ArrayMath.max(weights));
			return nextSum(weights, ArrayMath.norm(weights));
		}

		/**
		 * Return a random integer with support {@code [0, weights.length)} from the
		 * discrete distribution of weights defined by the {@code int[]} array
		 * {@code weights}. Given the sum of weights {@code sum} the Gillespie algorithm
		 * picks a uniformly distributed random number in {@code [0, sum)} and then
		 * walks through the array of weights until the cumulative sum exceeds the
		 * random number. The index of the weight that exceeds the random number is
		 * returned.
		 * <p>
		 * <strong>Note:</strong>
		 * <ol>
		 * <li>this is the standard, non-optimized version of the Gillespie algorithm
		 * and is most likely best for small supports.
		 * <li>For skewed weights consider sorting the weights in descending order.
		 * </ol>
		 * 
		 * @param weights the array of weights of the discrete distribution
		 * @param sum     the sum of all weights
		 * @return the random integer
		 */
		public int nextSum(int[] weights, int sum) {
			int hit = random0n(sum);
			int aRand = -1;
			while (hit >= 0) {
				hit -= weights[++aRand];
			}
			return aRand;
		}

		/**
		 * Return a random integer with support {@code [0, weights.length)} from the
		 * discrete distribution of weights defined by the {@code int[]} array
		 * {@code weights}. Given the maximum weight {@code max}, the drawing of
		 * weighted random numbers can be optmized by simplifying bookkeeping at the
		 * expense of drawing more random numbers.
		 * <p>
		 * <strong>Notes:</strong>
		 * <ol>
		 * <li>The optimization is most effective for large supports but can be hampered
		 * by heavily skewed weight distributions.
		 * <li>For small supports the standard version of the Gillespie algorithm is
		 * likely faster.
		 * <li>{@code max} can be bigger than the actual maximum but the optimization
		 * becomes less efficient.
		 * </ol>
		 * 
		 * @param weights the array of weights of the discrete distribution
		 * @param max     the maximum weight in the array
		 * @return the random integer
		 * 
		 * @see <a href="http://arxiv.org/pdf/1109.3627.pdf">Lipowski, A. &amp;
		 *      Lipowska, D. (2011) Roulette-wheel selection via stochastic
		 *      acceptance</a>
		 */
		public int nextMax(int[] weights, int max) {
			int len = weights.length;
			int aRand = -1;
			do {
				aRand = random0n(len);
			} while (random0n(max) >= weights[aRand]); // note: if < holds aRand is ok
			return aRand;
		}

		/**
		 * Return a random integer with support {@code [0, weights.length)} from the
		 * discrete distribution of weights defined by the {@code double[]} array
		 * {@code weights}. Optimized sampling is used above the threshold of
		 * <code> weights.length &gt; {@value #THRESHOLD_SIZE}</code>.
		 * <p>
		 * <strong>Note:</strong> if the sum of weights or the maximum weight is known,
		 * consider using the methods {@code #nextSum(double[], double)} or
		 * {@code #nextMax(double[], double)}, instead.
		 * 
		 * @param weights the array of weights of the discrete distribution
		 * @return the random integer
		 */
		public int next(double[] weights) {
			if (weights.length > THRESHOLD_SIZE)
				return nextMax(weights, ArrayMath.max(weights));
			return nextSum(weights, ArrayMath.norm(weights));
		}

		/**
		 * Return a random integer with support {@code [0, weights.length)} from the
		 * discrete distribution of weights defined by the {@code double[]} array
		 * {@code weights}. Given the sum of weights {@code sum} the Gillespie algorithm
		 * picks a uniformly distributed random number in {@code [0, sum)} and then
		 * walks through the array of weights until the cumulative sum exceeds the
		 * random number. The index of the weight that exceeds the random number is
		 * returned.
		 * <p>
		 * <strong>Note:</strong>
		 * <ol>
		 * <li>this is the standard, non-optimized version of the Gillespie algorithm
		 * and is most likely best for small supports.
		 * <li>For skewed weights consider sorting the weights in descending order.
		 * </ol>
		 * 
		 * @param weights the array of weights of the discrete distribution
		 * @param sum     the sum of all weights
		 * @return the random integer
		 */
		public int nextSum(double[] weights, double sum) {
			return nextHit(weights, random01() * sum);
		}

		public static int nextSum(MersenneTwister rng, double[] weights, double sum) {
			return nextHit(weights, rng.nextDouble() * sum);
		}

		static int nextHit(double[] weights, double hit) {
			int aRand = -1;
			for (double weight : weights) {
				hit -= weight;
				aRand++;
				if (hit < 0.0)
					return aRand;
			}
			// last resort; try to catch rounding errors
			int len1 = weights.length - 1;
			if (hit < 1e-6 && weights[len1] > 1e-6)
				return len1;
			throw new IllegalArgumentException("Gillespie algorithm failed to pick individual...");
		}

		/**
		 * Return a random integer with support {@code [0, weights.length)} from the
		 * discrete distribution of weights defined by the {@code double[]} array
		 * {@code weights}. Given the maximum weight {@code max}, the drawing of
		 * weighted random numbers can be optmized by simplifying bookkeeping at the
		 * expense of drawing more random numbers.
		 * <p>
		 * <strong>Notes:</strong>
		 * <ol>
		 * <li>The optimization is most effective for large supports but can be hampered
		 * by heavily skewed weight distributions.
		 * <li>For small supports the standard version of the Gillespie algorithm is
		 * likely faster. The threshold appears to be at around 350 elements.
		 * <li>{@code max} can be bigger than the actual maximum but the optimization
		 * becomes less efficient.
		 * </ol>
		 * 
		 * @param weights the array of weights of the discrete distribution
		 * @param max     the maximum weight in the array
		 * @return the random integer
		 * 
		 * @see <a href="http://arxiv.org/pdf/1109.3627.pdf">Lipowski, A. &amp;
		 *      Lipowska, D. (2011) Roulette-wheel selection via stochastic
		 *      acceptance</a>
		 */
		public int nextMax(double[] weights, double max) {
			return nextMax(rng, weights, max);
		}

		public static int nextMax(MersenneTwister rng, double[] weights, double max) {
			int len = weights.length;
			int aRand = -1;
			do {
				aRand = rng.nextInt(len);
			} while (rng.nextDouble() * max > weights[aRand]); // note: if < holds aRand is ok
			return aRand;
		}

		@Override
		public Gillespie clone() {
			Gillespie clone = new Gillespie(rng.clone());
			clone(clone);
			return clone;
		}

		/**
		 * Test Gillespie algorithm for random weight distribution.
		 * <p>
		 * The test samples the distribution and bins the random numbers. This sample
		 * distribution is compared to the theoretical expectation. The mean deviation
		 * is the mean difference between the actual number of events in each bin and
		 * their expected number. For a perfect match the mean deviation is
		 * <code>0</code>. The test passes if the mean deviation lies within one
		 * standard error from <code>0</code>. This is more stringent than the
		 * traditional 95% confidence interval.
		 * 
		 * @param rng    the random number generator
		 * @param logger the logger for reporting results
		 * @param clock  the stop watch
		 */
		public static void test(MersenneTwister rng, Logger logger, Chronometer clock) {
			RNGDistribution.Gillespie gillespie = new Gillespie();
			int nBins = THRESHOLD_SIZE;
			// initialize array with random weights
			double[] weights = new double[nBins];
			for (int i = 0; i < nBins; i++)
				weights[i] = gillespie.random01() * 100.0;
			double maxWeight = ArrayMath.max(weights);
			double sumWeight = ArrayMath.norm(weights);
			int nSamples = gillespie.testSamples;
			double[] noopt = new double[nBins];
			logger.info("Testing Gillespie algorithm: " + nSamples + " samples (non-optimized)...");
			double msStart = clock.elapsedTimeMsec();
			for (int i = 0; i < nSamples; i++) {
				int idx = gillespie.nextSum(weights, sumWeight);
				noopt[idx]++;
			}
			double msEnd = clock.elapsedTimeMsec();
			logger.info("Time elapsed: " + (msEnd - msStart) + " msec");
			double[] opt = new double[nBins];
			logger.info("Testing Gillespie algorithm: " + nSamples + " samples (optimized)...");
			msStart = clock.elapsedTimeMsec();
			for (int i = 0; i < nSamples; i++) {
				int idx = gillespie.nextMax(weights, maxWeight);
				opt[idx]++;
			}
			msEnd = clock.elapsedTimeMsec();
			logger.info("Time elapsed: " + (msEnd - msStart) + " msec");
			ArrayMath.normalize(weights);
			ArrayMath.normalize(noopt);
			ArrayMath.normalize(opt);
			boolean verbose = (logger.getLevel().intValue() <= Level.FINE.intValue());
			if (verbose) {
				StringBuilder buffer = new StringBuilder();
				buffer.append("Weighted Distribution:\nbin: non-optimized optimized (expected)\n");
				for (int n = 0; n < nBins; n++)
					buffer.append(
							n + ": " + Formatter.format(noopt[n], 6) + " " + //
									Formatter.format(opt[n], 6) + //
									" (" + Formatter.format(weights[n], 6) + ")\n");
				logger.info(buffer.toString());
			}
			double m1no = 0.0;
			double m2no = 0.0;
			double m1o = 0.0;
			double m2o = 0.0;
			for (int n = 0; n < nBins; n++) {
				double d = noopt[n] - weights[n];
				m1no += d;
				m2no += d * d;
				d = opt[n] - weights[n];
				m1o += d;
				m2o += d * d;
			}
			m1no /= nSamples;
			m2no /= nSamples;
			m1o /= nSamples;
			m2o /= nSamples;
			double stdevno = Math.sqrt(m2no - m1no * m1no);
			double sterrno = stdevno / Math.sqrt(nSamples);
			logger.info("Statistics: mean +/- SEM = " + m1no + " +/- " + sterrno + " (non-optimized)");
			double stdevo = Math.sqrt(m2o - m1o * m1o);
			double sterro = stdevo / Math.sqrt(nSamples);
			logger.info("Statistics: mean +/- SEM = " + m1o + " +/- " + sterro + " (optimized)");
			// in order to pass the test, the mean+/-sterr must include 0
			boolean successno = (Math.abs(m1no) < sterrno);
			boolean successo = (Math.abs(m1o) < sterro);
			if (successno && successo) {
				logger.info("Test passed!");
				return;
			}
			logger.severe("Test of Gillespie algorithm failed...");
		}
	}

	// NOTE: the test uses GWT specifics for measuring time
	// (com.google.gwt.core.client.Duration) and for reporting
	// (com.google.gwt.core.client.GWT.log)
	// public void test() {
	// test(10000, 10000000);
	// }
	//
	// public void test(int nBins, int nSamples) {
	// String msg = "Testing performance of random number generator:
	// nBins="+ChHFormatter.formatSci(nBins, 1)+
	// ", nSamples="+ChHFormatter.formatSci(nSamples, 1);
	// com.google.gwt.core.client.GWT.log(msg);
	// System.out.println(msg);
	//
	// double[] bins = new double[nBins];
	// com.google.gwt.core.client.Duration start = new
	// com.google.gwt.core.client.Duration();
	// for( int n=0; n<nSamples; n++ ) {
	// double rand = random01();
	// bins[(int)(rand*nBins)]++;
	// }
	// double end = start.elapsedMillis();
	// double min = ChHMath.min(bins);
	// double max = ChHMath.max(bins);
	// ChHMath.normalize(bins);
	// double mean = ChHMath.mean(bins);
	// double var = ChHMath.variance(bins, mean);
	// String result = "random01: mean="+ChHFormatter.format(mean, 6)+" +/-
	// "+ChHFormatter.format(Math.sqrt(var), 6)+
	// " ("+ChHFormatter.format(1.0/Math.sqrt(nBins), 6)+")"+
	// ", min="+ChHFormatter.format(min, 0)+", max="+ChHFormatter.format(max, 0)+",
	// time="+ChHFormatter.format(end/1000.0, 3)+"s";
	// com.google.gwt.core.client.GWT.log(result);
	// System.out.println(result);
	//
	// java.util.Arrays.fill(bins, 0.0);
	// start = new com.google.gwt.core.client.Duration();
	// for( int n=0; n<nSamples; n++ ) {
	// double rand = random01d();
	// bins[(int)(rand*nBins)]++;
	// }
	// end = start.elapsedMillis();
	// min = ChHMath.min(bins);
	// max = ChHMath.max(bins);
	// ChHMath.normalize(bins);
	// mean = ChHMath.mean(bins);
	// var = ChHMath.variance(bins, mean);
	// result = "random01d: mean="+ChHFormatter.format(mean, 6)+" +/-
	// "+ChHFormatter.format(Math.sqrt(var), 6)+
	// " ("+ChHFormatter.format(1.0/Math.sqrt(nBins), 6)+")"+
	// ", min="+ChHFormatter.format(min, 0)+", max="+ChHFormatter.format(max, 0)+",
	// time="+ChHFormatter.format(end/1000.0, 3)+"s";
	// com.google.gwt.core.client.GWT.log(result);
	// System.out.println(result);
	//
	//
	// java.util.Arrays.fill(bins, 0.0);
	// start = new com.google.gwt.core.client.Duration();
	// for( int n=0; n<nSamples; n++ ) {
	// int rand = random0n(nBins);
	// bins[rand]++;
	// }
	// end = start.elapsedMillis();
	// min = ChHMath.min(bins);
	// max = ChHMath.max(bins);
	// ChHMath.normalize(bins);
	// mean = ChHMath.mean(bins);
	// var = ChHMath.variance(bins, mean);
	// result = "random0n: mean="+ChHFormatter.format(mean, 6)+" +/-
	// "+ChHFormatter.format(Math.sqrt(var), 6)+
	// " ("+ChHFormatter.format(1.0/Math.sqrt(nBins), 6)+")"+
	// ", min="+ChHFormatter.format(min, 0)+", max="+ChHFormatter.format(max, 0)+",
	// time="+ChHFormatter.format(end/1000.0, 3)+"s";
	// com.google.gwt.core.client.GWT.log(result);
	// System.out.println(result);
	// }
}
