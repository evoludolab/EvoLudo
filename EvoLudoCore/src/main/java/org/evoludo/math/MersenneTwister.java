package org.evoludo.math;

import java.util.List;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.evoludo.util.Formatter;
import org.evoludo.util.Plist;

/**
 * <h2>MersenneTwister and MersenneTwisterFast</h2>
 * <p>
 * <b>Version 22</b>, based on version MT199937(99/10/29) of the Mersenne
 * Twister algorithm found at
 * <a href="http://www.math.keio.ac.jp/matumoto/emt.html"> The Mersenne Twister
 * Home Page</a>, with the initialization improved using the new 2002/1/26
 * initialization algorithm By Sean Luke, October 2004.
 * 
 * <p>
 * <b>MersenneTwister</b> is a drop-in subclass replacement for
 * java.util.Random. It is properly synchronized and can be used in a
 * multithreaded environment. On modern VMs such as HotSpot, it is approximately
 * 1/3 slower than java.util.Random.
 *
 * <p>
 * <b>MersenneTwisterFast</b> is not a subclass of java.util.Random. It has the
 * same public methods as Random does, however, and it is algorithmically
 * identical to MersenneTwister. MersenneTwisterFast has hard-code inlined all
 * of its methods directly, and made all of them final (well, the ones of
 * consequence anyway). Further, these methods are <i>not</i> synchronized, so
 * the same MersenneTwisterFast instance cannot be shared by multiple threads.
 * But all this helps MersenneTwisterFast achieve well over twice the speed of
 * MersenneTwister. java.util.Random is about 1/3 slower than
 * MersenneTwisterFast.
 *
 * <h3>About the Mersenne Twister</h3>
 * <p>
 * This is a Java version of the C-program for MT19937: Integer version. The
 * MT19937 algorithm was created by Makoto Matsumoto and Takuji Nishimura, who
 * ask: "When you use this, send an email to: matumoto@math.keio.ac.jp with an
 * appropriate reference to your work". Indicate that this is a translation of
 * their algorithm into Java.
 *
 * <p>
 * <b>Reference. </b> Makato Matsumoto and Takuji Nishimura, "Mersenne Twister:
 * A 623-Dimensionally Equidistributed Uniform Pseudo-Random Number Generator",
 * <i>ACM Transactions on Modeling and. Computer Simulation,</i> Vol. 8, No. 1,
 * January 1998, pp 3--30.
 *
 * <h3>About this Version</h3>
 *
 * <p>
 * <b>Changes since V21:</b> Minor documentation HTML fixes.
 *
 * <p>
 * <b>Changes since V20:</b> Added clearGuassian(). Modified stateEquals() to be
 * synchronizd on both objects for MersenneTwister, and changed its
 * documentation. Added synchronization to both setSeed() methods, to
 * writeState(), and to readState() in MersenneTwister. Removed synchronization
 * from readObject() in MersenneTwister.
 *
 * <p>
 * <b>Changes since V19:</b> nextFloat(boolean, boolean) now returns float, not
 * double.
 *
 * <p>
 * <b>Changes since V18:</b> Removed old final declarations, which used to
 * potentially speed up the code, but no longer.
 *
 * <p>
 * <b>Changes since V17:</b> Removed vestigial references to &amp;= 0xffffffff
 * which stemmed from the original C code. The C code could not guarantee that
 * ints were 32 bit, hence the masks. The vestigial references in the Java code
 * were likely optimized out anyway.
 *
 * <p>
 * <b>Changes since V16:</b> Added nextDouble(includeZero, includeOne) and
 * nextFloat(includeZero, includeOne) to allow for half-open, fully-closed, and
 * fully-open intervals.
 *
 * <p>
 * <b>Changes Since V15:</b> Added serialVersionUID to quiet compiler warnings
 * from Sun's overly verbose compilers as of JDK 1.5.
 *
 * <p>
 * <b>Changes Since V14:</b> made strictfp, with StrictMath.log and
 * StrictMath.sqrt in nextGaussian instead of Math.log and Math.sqrt. This is
 * largely just to be safe, as it presently makes no difference in the speed,
 * correctness, or results of the algorithm.
 *
 * <p>
 * <b>Changes Since V13:</b> clone() method CloneNotSupportedException removed.
 *
 * <p>
 * <b>Changes Since V12:</b> clone() method added.
 *
 * <p>
 * <b>Changes Since V11:</b> stateEquals(...) method added. MersenneTwisterFast
 * is equal to other MersenneTwisterFasts with identical state; likewise
 * MersenneTwister is equal to other MersenneTwister with identical state. This
 * isn't equals(...) because that requires a contract of immutability to compare
 * by value.
 *
 * <p>
 * <b>Changes Since V10:</b> A documentation error suggested that setSeed(int[])
 * required an int[] array 624 long. In fact, the array can be any non-zero
 * length. The new version also checks for this fact.
 *
 * <p>
 * <b>Changes Since V9:</b> readState(stream) and writeState(stream) provided.
 *
 * <p>
 * <b>Changes Since V8:</b> setSeed(int) was only using the first 28 bits of the
 * seed; it should have been 32 bits. For small-number seeds the behavior is
 * identical.
 *
 * <p>
 * <b>Changes Since V7:</b> A documentation error in MersenneTwisterFast (but
 * not MersenneTwister) stated that nextDouble selects uniformly from the
 * full-open interval [0,1]. It does not. nextDouble's contract is identical
 * across MersenneTwisterFast, MersenneTwister, and java.util.Random, namely,
 * selection in the half-open interval [0,1). That is, 1.0 should not be
 * returned. A similar contract exists in nextFloat.
 *
 * <p>
 * <b>Changes Since V6:</b> License has changed from LGPL to BSD. New timing
 * information to compare against java.util.Random. Recent versions of HotSpot
 * have helped Random increase in speed to the point where it is faster than
 * MersenneTwister but slower than MersenneTwisterFast (which should be the
 * case, as it's a less complex algorithm but is synchronized).
 * 
 * <p>
 * <b>Changes Since V5:</b> New empty constructor made to work the same as
 * java.util.Random -- namely, it seeds based on the current time in
 * milliseconds.
 *
 * <p>
 * <b>Changes Since V4:</b> New initialization algorithms. See (see
 * <a href="http://www.math.keio.ac.jp/matumoto/MT2002/emt19937ar.html">
 * http://www.math.keio.ac.jp/matumoto/MT2002/emt19937ar.html</a>)
 *
 * <p>
 * The MersenneTwister code is based on standard MT19937 C/C++ code by Takuji
 * Nishimura, with suggestions from Topher Cooper and Marc Rieffel, July 1997.
 * The code was originally translated into Java by Michael Lecuyer, January
 * 1999, and the original code is Copyright (c) 1999 by Michael Lecuyer.
 *
 * <h3>Java notes</h3>
 * 
 * <p>
 * This implementation implements the bug fixes made in Java 1.2's version of
 * Random, which means it can be used with earlier versions of Java. See
 * <a href=
 * "http://www.javasoft.com/products/jdk/1.2/docs/api/java/util/Random.html">
 * the JDK 1.2 java.util.Random documentation</a> for further documentation on
 * the random-number generation contracts made. Additionally, there's an
 * undocumented bug in the JDK java.util.Random.nextBytes() method, which this
 * code fixes.
 *
 * <p>
 * Just like java.util.Random, this generator accepts a long seed but doesn't
 * use all of it. java.util.Random uses 48 bits. The Mersenne Twister instead
 * uses 32 bits (int size). So it's best if your seed does not exceed the int
 * range.
 *
 * <p>
 * MersenneTwister can be used reliably on JDK version 1.1.5 or above. Earlier
 * Java versions have serious bugs in java.util.Random; only MersenneTwisterFast
 * (and not MersenneTwister nor java.util.Random) should be used with them.
 *
 * <h3>License</h3>
 *
 * Copyright (c) 2003 by Sean Luke. <br>
 * Portions copyright (c) 1993 by Michael Lecuyer. <br>
 * All rights reserved. <br>
 *
 * <p>
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * <ul>
 * <li>Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 * <li>Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * <li>Neither the name of the copyright owners, their employers, nor the names
 * of its contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 * </ul>
 * <p>
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *
 * @version 22
 */

//Note: some adjustments are needed to make this work with GWT
// - GWT has a thing with clone()ing - Clonable removed
// - java.io stuff is problematic in GWT - removed
// - no advantage of extending java.util.Random - removed
// - floats are bad in GWT - should not be used
// - longs are really bad in GWT - should really not be used
// - int's in JavaScript do not seem to be 32bit! need to use masks for consistent
//	 results (see JavaScript implementation mersenne.js by Stephan Brumme:
//	 https://create.stephan-brumme.com/mersenne-twister/)
// - mag01 not needed - removed
// - loops optimized by reducing access to arrays through local vars
// - logging in tests adapted to work both with JRE/GWT
//-- Christoph Hauert

public class MersenneTwister {

	// Period parameters
	private static final int N = 624;
	private static final int M = 397;
	private static final int MATRIX_A = 0x9908b0df; // private static final * constant vector a
	private static final int BIT32_MASK = 0xffffffff; // 32 bit mask
	private static final int UPPER_MASK = 0x80000000; // most significant w-r bits
	private static final int LOWER_MASK = 0x7fffffff; // least significant r bits

	// Tempering parameters
	private static final int TEMPERING_MASK_B = 0x9d2c5680;
	private static final int TEMPERING_MASK_C = 0xefc60000;

	private int mt[]; // the array for the state vector
	private int mti; // mti==N+1 means mt[N] is not initialized

	// a good initial seed (of int size, though stored in a long)
	// private static final long GOOD_SEED = 4357;

	private double __nextNextGaussian;
	private boolean __haveNextNextGaussian;

	/* ChH: custom methods added for encoding and restoring RNG state */
	/**
	 * Encode state of random number generator as <code>plist</code> for saving.
	 * 
	 * @return <code>plist</code> string encoding state of random number generator
	 */
	synchronized public String encodeState() {
		StringBuilder plist = new StringBuilder();
		plist.append(Plist.encodeKey("mt", mt));
		plist.append(Plist.encodeKey("mti", mti));
		// encode nextGaussian only if one available
		if (__haveNextNextGaussian)
			plist.append(Plist.encodeKey("__nextNextGaussian", __nextNextGaussian));
		return plist.toString();
	}

	/**
	 * Restore state of random number generator from <code>plist</code> encoded
	 * string.
	 * 
	 * @param plist encoded state of random number generator
	 * @return <code>true</code> if state successfully restored
	 */
	synchronized public boolean restoreState(Plist plist) {
		@SuppressWarnings("unchecked")
		List<Integer> rmt = (List<Integer>) plist.get("mt");
		if (rmt == null || rmt.size() != N)
			return false;
		for (int n = 0; n < N; n++)
			mt[n] = rmt.get(n);
		mti = (Integer) plist.get("mti");
		// check if nextGaussian available
		Double nextGauss = (Double) plist.get("__nextNextGaussian");
		if (nextGauss == null) {
			__haveNextNextGaussian = false;
			__nextNextGaussian = Double.NaN;
		} else {
			__haveNextNextGaussian = false;
			__nextNextGaussian = nextGauss;
		}
		return true;
	}

	/**
	 * Returns true if the MersenneTwister's current internal state is equal to
	 * another MersenneTwister. This is roughly the same as equals(other), except
	 * that it compares based on value but does not guarantee the contract of
	 * immutability (obviously random number generators are immutable). Note that
	 * this does NOT check to see if the internal gaussian storage is the same for
	 * both. You can guarantee that the internal gaussian storage is the same (and
	 * so the nextGaussian() methods will return the same values) by calling
	 * clearGaussian() on both objects.
	 * 
	 * @param other another {@link MersenneTwister}
	 * @return <code>true</code> the two {@link MersenneTwister}'s are identical.
	 */
	synchronized public boolean stateEquals(MersenneTwister other) {
		if (other == this)
			return true;
		if (other == null)
			return false;
		if (mti != other.mti)
			return false;
		for (int x = 0; x < mt.length; x++)
			if (mt[x] != other.mt[x])
				return false;
		return true;
	}

	/**
	 * Constructor using the default seed.
	 */
	public MersenneTwister() {
		this(System.currentTimeMillis());
	}

	/**
	 * Constructor using a given seed. Though you pass this seed in as a long, it's
	 * best to make sure it's actually an integer.
	 *
	 * @param seed for random number generator
	 */
	public MersenneTwister(long seed) {
		setSeed(seed);
	}

	/**
	 * Constructor using an array of integers as seed. Your array must have a
	 * non-zero length. Only the first 624 integers in the array are used; if the
	 * array is shorter than this then integers are repeatedly used in a wrap-around
	 * fashion.
	 * 
	 * @param array of integers for seeding the random number generator
	 */
	public MersenneTwister(int[] array) {
		setSeed(array);
	}

	/**
	 * Initialize the pseudo random number generator. Don't pass in a long that's
	 * bigger than an int (Mersenne Twister only uses the first 32 bits for its
	 * seed).
	 * 
	 * <h3>Implementation Notes:</h3>
	 * <ol>
	 * <li>Writing java code that GWT can translate into <em>equivalent</em>
	 * JavaScript turns out to be challenging. Stephan Brumme's JavaScript
	 * implementation supplied the essential tricks. For details see <a href=
	 * "https://create.stephan-brumme.com/mersenne-twister/">https://create.stephan-brumme.com/mersenne-twister/</a>
	 * <li>In order to avoid multiplication overflow split 32 bits into 2x 16 bits
	 * and process them individually (see {@code mtmti1} in code).
	 * <li>See Knuth TAOCP Vol2. 3rd Ed. p.106 for multiplier
	 * </ol>
	 * 
	 * @param seed the seed for the random number generator
	 */
	synchronized public void setSeed(long seed) {
		// Due to a bug in java.util.Random clear up to 1.2, we're
		// doing our own Gaussian variable.
		__haveNextNextGaussian = false;

		mt = new int[N];

		mt[0] = (int) (seed & BIT32_MASK);
		int mtmti1 = mt[0];
		for (mti = 1; mti < N; mti++) {
//ChH: orig			mt[mti] = (1812433253 * (mt[mti-1] ^ (mt[mti-1] >>> 30)) + mti);
//ChH: optimized	mtmti1 = mt[mti-1];
//					mtmti1 = (1812433253 * (mtmti1 ^ (mtmti1 >>> 30)) + mti);
//					mt[mti] = mtmti1;
			int s = mtmti1 ^ (mtmti1 >>> 30);
//check if mask needed - might be only in rare cases
			mtmti1 = ((((((s & 0xffff0000) >>> 16) * 1812433253) << 16) + (s & 0x0000ffff) * 1812433253) + mti)
					& BIT32_MASK;
			mt[mti] = mtmti1;
			// In previous versions, MSBs of the seed affect only MSBs of the array mt[].
			// 2002/01/09 modified by Makoto Matsumoto
			// mt[mti] &= BIT32_MASK; // for <=32 bit machines
		}
	}

	/**
	 * Sets the seed of the MersenneTwister using an array of integers. Your array
	 * must have a non-zero length. Only the first 624 integers in the array are
	 * used; if the array is shorter than this then integers are repeatedly used in
	 * a wrap-around fashion.
	 * 
	 * @param array of integers for seeding the random number generator
	 */
	synchronized public void setSeed(int[] array) {
		if (array.length == 0)
			throw new IllegalArgumentException("Array length must be greater than zero");
		int i, j, k;
		setSeed(19650218L);
		i = 1;
		j = 0;
		k = (N > array.length ? N : array.length);
		int mtmti1 = mt[0];
		for (; k != 0; k--) {
//ChH: orig			mt[i] = (mt[i] ^ ((mt[i-1] ^ (mt[i-1] >>> 30)) * 1664525)) + array[j] + j; /* non linear */
			// mt[i] &= BIT32_MASK; /* for WORDSIZE > 32 machines */
			// avoid multiplication overflow: split 32 bits into 2x 16 bits and process them
			// individually
			int s = mtmti1 ^ (mtmti1 >>> 30);
//check if mask needed - might be only in rare cases
			mtmti1 = ((mt[i] ^ (((((s & 0xffff0000) >>> 16) * 1664525) << 16) + (s & 0x0000ffff) * 1664525)) + array[j]
					+ j) & BIT32_MASK;
			mt[i] = mtmti1;
			i++;
			j++;
			if (i >= N) {
				mt[0] = mt[N - 1];
				i = 1;
			}
			if (j >= array.length)
				j = 0;
		}
		mtmti1 = mt[i - 1];
		for (k = N - 1; k != 0; k--) {
//ChH: orig			mt[i] = (mt[i] ^ ((mt[i-1] ^ (mt[i-1] >>> 30)) * 1566083941)) - i; /* non linear */
			// mt[i] &= BIT32_MASK; /* for WORDSIZE > 32 machines */
			// avoid multiplication overflow: split 32 bits into 2x 16 bits and process them
			// individually
			int s = mtmti1 ^ (mtmti1 >>> 30);
//check if mask needed - might be only in rare cases
			mtmti1 = ((mt[i] ^ (((((s & 0xffff0000) >>> 16) * 1566083941) << 16) + (s & 0x0000ffff) * 1566083941)) - i)
					& BIT32_MASK;
			mt[i] = mtmti1;
			i++;
			if (i >= N) {
				mt[0] = mt[N - 1];
				i = 1;
			}
		}
		mt[0] = 0x80000000; /* MSB is 1; assuring non-zero initial array */
	}

	/**
	 * The twister: core mechanism for generating pseudo random numbers.
	 * <p>
	 * <strong>Note:</strong> Adapted from <code>mersenne.js</code>, JavaScript
	 * version of MersenneTwister by Stephan Brumme.
	 * 
	 * @see <a href= "https://create.stephan-brumme.com/mersenne-twister/">
	 *      https://create.stephan-brumme.com/mersenne-twister/</a>
	 */
	synchronized private void twist() {
		if (mti < N)
			return;
		// generate N words at one time
		int y, kk;
		for (kk = 0; kk < N - M; kk++) {
			y = (mt[kk] & UPPER_MASK) | (mt[kk + 1] & LOWER_MASK);
			mt[kk] = mt[kk + M] ^ (y >>> 1) ^ ((y & 0x1) * MATRIX_A);
		}
		for (; kk < N - 1; kk++) {
			y = (mt[kk] & UPPER_MASK) | (mt[kk + 1] & LOWER_MASK);
			mt[kk] = mt[kk + (M - N)] ^ (y >>> 1) ^ ((y & 0x1) * MATRIX_A);
		}
		y = (mt[N - 1] & UPPER_MASK) | (mt[0] & LOWER_MASK);
		mt[N - 1] = mt[M - 1] ^ (y >>> 1) ^ ((y & 0x1) * MATRIX_A);

		mti = 0;
	}

	/**
	 * Generates a 32bit 'unsigned' random <code>int</code> in
	 * <code>[0, 2^<sup>33</sup>-1]</code>. All 32bits represent random number - use
	 * with care! Allows to verify output with original mt19937ar.c output.
	 * <p>
	 * From <code>mt19937ar.c</code>: generates a random number on
	 * <code>[0, 0xffffffff]</code>-interval corresponds to:
	 * <code>unsigned long genrand_int32</code>
	 * 
	 * @return random <code>int</code> in <code>[0, 2^<sup>33</sup>-1]</code>
	 */
	private int nextUInt() {
		twist();
		int y = mt[mti++];
		y ^= (y >>> 11); // TEMPERING_SHIFT_U(y)
		y ^= (y << 7) & TEMPERING_MASK_B; // TEMPERING_SHIFT_S(y)
		y ^= (y << 15) & TEMPERING_MASK_C; // TEMPERING_SHIFT_T(y)
		y ^= (y >>> 18); // TEMPERING_SHIFT_L(y)
		return y;
	}

	/**
	 * Generate a 31bit (signed) random <code>int</code> in
	 * <code>[0, Integer.MAX_VALUE)</code>.
	 * <p>
	 * From <code>mt19937ar.c</code>: generates a random number on
	 * <code>[0, 0x7fffffff]</code>-interval corresponds to:
	 * <code>unsigned long genrand_int31</code>
	 * 
	 * @return random <code>int</code> in <code>[0, 2^<sup>32</sup>-1]</code>
	 */
	synchronized public int nextInt() {
		return (nextUInt() >>> 1);
	}

	/**
	 * Generate a random <code>int</code> in <code>[0, n-1]</code>-interval.
	 * 
	 * @param n integer upper bound (exclusive)
	 * @return random integer in <code>[0, n)</code>
	 * 
	 * @throws IllegalArgumentException if <code>n &le; 0</code>
	 */
	synchronized public int nextInt(int n) {
		if (n <= 1) {
			if (n == 1)
				return 0;
			throw new IllegalArgumentException("n must be positive, got: " + n);
		}

		if ((n & -n) == n) {
			// i.e., n is a power of 2
			// ChH: GWT does not look kindly on long - eliminate!
			// return (int)((n * (long)nextInt()) >> 31);
			// avoid multiplication overflow: split 32 bits into 2x 16 bits and process them
			// individually
//			int s = nextInt();
//seems ok but uses lower bits somehow this does not seem to be a good strategy (worth looking into?)
//			return (((((s & 0xffff0000) >>> 16) * n) << 16) +
//					   (s & 0x0000ffff)         * n) & (n-1);
			// calc log_2(n)
			int log2 = 1;
			n = n >>> 2;
			while (n > 0) {
				log2++;
				n = n >>> 1;
			}
			// note: cannot shift by 32bit (apparently turns into nop...); log2>0 must hold;
			// n=1 caught at start
//check if mask needed - might be only in rare cases
			return (nextUInt() >>> (32 - log2)) & BIT32_MASK;
		}

		int bits, val;
		do {
			bits = nextInt();
			val = bits % n;
		} while (bits - val + (n - 1) < 0);
		return val;
	}

	/**
	 * Generate a 64bit unsigned random long in <code>[0, 2^<sup>65</sup>-1]</code>.
	 * 
	 * @return random <code>long</code> integer in
	 *         <code>[0, 2^<sup>65</sup>-1]</code>
	 */
	private long nextULong() {
		long y = nextUInt();
		long z = nextUInt();
		return (y << 32) + z;
	}

	/**
	 * Generate a 63bit (signed) random <code>long</code> integer in
	 * <code>[0, Long.MAX_VALUE)</code>.
	 * <p>
	 * <strong>Note:</strong>
	 * <ul>
	 * <li>Twice as expensive as nextInt().</li>
	 * <li>Do <em>not</em> use in GWT applications (<code>long</code> integers are
	 * CPU hogs).</li>
	 * </ul>
	 * 
	 * @return random <code>long</code> integer in <code>[0,
	 *         2^<sup>63</sup>-1]</code>
	 */
	synchronized public long nextLong() {
		// ChH: looks like this returns an unacceptable 'unsigned' long...
		// int y = nextUInt();
		// int z = nextUInt();
		// return (((long)y) << 32) + z;
		return (nextULong() >>> 1);
	}

	/**
	 * Generate a random <code>long</code> integer in <code>[0, n)</code> with
	 * <code>n &gt; 0</code>.
	 * <p>
	 * <strong>Note:</strong>
	 * <ul>
	 * <li>Twice as expensive as {@link #nextInt(int)}.</li>
	 * <li>Do <em>not</em> use in GWT applications (<code>long</code> integers are
	 * CPU hogs).</li>
	 * </ul>
	 * 
	 * @param n integer upper bound (exclusive)
	 * @return random <code>long</code> integer in <code>[0, n]</code>
	 * 
	 * @throws IllegalArgumentException if <code>n &le; 0</code>
	 */
	synchronized public long nextLong(long n) {
		if (n <= 1) {
			if (n == 1)
				return 0L;
			throw new IllegalArgumentException("n must be positive, got: " + n);
		}

		// use same optimization as for nextInt(int)
		if ((n & -n) == n) {
			// i.e., n is a power of 2; calc log_2(n)
			int log2 = 1;
			n = n >>> 2;
			while (n > 0) {
				log2++;
				n = n >>> 1;
			}
			// note: cannot shift by 32bit (apparently turns into nop...); log2>0 must hold;
			// n=1 caught at start
			return (nextULong() >>> (64 - log2));
		}

		long bits, val;
		do {
			bits = nextLong();
			val = bits % n;
		} while (bits - val + (n - 1) < 0);
		return val;
	}

	/**
	 * Generates a random short integer on <code>[0, 0xffff]</code>-interval.
	 * <p>
	 * <strong>Note:</strong>
	 * <ul>
	 * <li>same as nextChar() but different return type.</li>
	 * <li>Do <em>not</em> use in GWT applications (<code>short</code> integers are
	 * not supported).
	 * </ul>
	 * 
	 * @return random <code>short</code> integer in <code>[0, 65536)</code>
	 */
	synchronized public short nextShort() {
		return (short) (nextUInt() >>> 16);
	}

	/**
	 * generates a random <code>char</code> on <code>[0, 0xffff]</code>-interval.
	 * <p>
	 * Note: same as nextShort() but different return type.
	 * </p>
	 * 
	 * @return random <code>short</code> integer in <code>[0, 65536)</code>
	 */
	synchronized public char nextChar() {
		return (char) (nextUInt() >>> 16);
	}

	/**
	 * Generates a random <code>byte</code> on <code>[0, 0xff]</code>-interval.
	 * 
	 * @return random <code>short</code> integer in <code>[0, 16)</code>
	 */
	synchronized public byte nextByte() {
		return (byte) (nextUInt() >>> 24);
	}

	/**
	 * Fill array {@code bytes} with random bytes.
	 * 
	 * @param bytes random bytes stored here
	 * @see #nextByte()
	 */
	synchronized public void nextBytes(byte[] bytes) {
		for (int x = 0; x < bytes.length; x++)
			bytes[x] = nextByte();
	}

	/**
	 * Generates a random boolean.
	 * 
	 * @return <code>true</code> with 50% chance
	 */
	synchronized public boolean nextBoolean() {
		return (nextUInt() >>> 31) != 0;
	}

	/**
	 * This generates a coin flip with a probability <code>probability</code> of
	 * returning <code>true</code>, else returning <code>false</code>.
	 * <code>probability</code> must be in <code>[0, 1]</code>. Not as precise as
	 * {@link #nextBoolean(double)}, but twice as fast. To explicitly use this,
	 * remember you may need to cast to <code>float</code> first.
	 * <p>
	 * <strong>Note:</strong> Do <em>not</em> use in GWT applications
	 * (<code>float</code>'s create overhead).
	 * </p>
	 * 
	 * @param probability for returning <code>true</code>
	 * @return <code>true</code> with <code>probability</code>
	 * 
	 * @throws IllegalArgumentException if <code>probability&lt;0</code> or
	 *                                  <code>probability&gt;1</code>
	 */
	synchronized public boolean nextBoolean(float probability) {
		if (probability < 0f || probability > 1f)
			throw new IllegalArgumentException("probability must be between 0.0 and 1.0 inclusive.");
		if (probability == 0f)
			return false; // fix half-open issues
		else if (probability == 1f)
			return true; // fix half-open issues
		// return (nextUInt() >>> 8) / ((float)(1 << 24)) < probability;
		return (nextUInt() >>> 8) * TWO_TO_NEG24 < probability;
	}

	/**
	 * This generates a coin flip with a probability <code>probability</code> of
	 * returning <code>true</code>, else returning <code>false</code>.
	 * <code>probability</code> must be in <code>[0, 1]</code>.
	 * <p>
	 * <strong>Note:</strong> More accurate than {@link #nextBoolean(float)}, but
	 * twice as expensive
	 * </p>
	 *
	 * @param probability for returning true
	 * @return <code>true</code> with <code>probability</code>
	 * 
	 * @throws IllegalArgumentException if <code>probability&lt;0</code> or
	 *                                  <code>probability&gt;1</code>
	 */
	synchronized public boolean nextBoolean(double probability) {
		if (probability < 0.0 || probability > 1.0)
			throw new IllegalArgumentException("probability must be between 0.0 and 1.0 inclusive.");
		if (probability == 0.0)
			return false; // fix half-open issues
		else if (probability == 1.0)
			return true; // fix half-open issues
		return nextDoubleHigh() < probability;
	}

	/**
	 * Generates a random high-precision double on half-open
	 * <code>[0, 1)</code>-interval.
	 * <p>
	 * From <code>mt19937ar.c</code>: generates a random number on
	 * <code>[0,1)</code> with 53-bit resolution corresponds to:
	 * <code>double genrand_res53(void)</code>.
	 * </p>
	 * <p>
	 * <strong>Note:</strong> Twice as expensive as {@link #nextDouble()} or the
	 * equivalent {@link #nextFloat()}.
	 * </p>
	 * 
	 * @return random high-precision <code>double</code> in <code>[0, 1)</code>
	 */
	synchronized public double nextDoubleHigh() {
		int y = nextUInt();
		int z = nextUInt();
		// ChH: long's are killers for GWT performance
		// return ((((long)(y >>> 5)) << 26) + (z >>> 6)) / (double)(1L << 53);
		// return (y >>> 5)*(67108864.0/9007199254740992.0) + (z >>>
		// 6)*(1.0/9007199254740992.0);
		return (y >>> 5) * TWO_TO_NEG27 + (z >>> 6) * TWO_TO_NEG53;
	}

	/**
	 * Generate random <code>double</code> on half-open
	 * <code>[0, 1)</code>-interval.
	 * <p>
	 * From <code>mt19937ar.c</code>: generates a random number on
	 * <code>[0,1)</code>-real-interval corresponds to:
	 * <code>double genrand_real2(void)</code>.
	 * </p>
	 * <p>
	 * <strong>Note:</strong>
	 * <ul>
	 * <li>Twice as fast as {@link #nextDoubleHigh()}</li>
	 * <li>Equivalent to {@link #nextFloat()}</li>
	 * <li>One bit lost compared to original due to signed <code>int</code></li>
	 * </ul>
	 * 
	 * @return random <code>double</code> in <code>[0, 1)</code>
	 */
	synchronized public double nextDouble() {
		return nextInt() * TWO_TO_NEG31; // rand/(2^31)
	}

	/**
	 * Generate random double on closed <code>[0, 1]</code>-interval
	 * <p>
	 * From <code>mt19937ar.c</code>: generates a random number on
	 * <code>[0,1]</code>-real-interval corresponds to:
	 * <code>double genrand_real1(void)</code>.
	 * </p>
	 * <p>
	 * <strong>Note:</strong> one bit lost compared to original due to signed
	 * <code>int</code>.
	 * </p>
	 * 
	 * @return random double in <code>[0, 1]</code>
	 */
	synchronized public double nextDoubleClosed() {
		return nextInt() * INV_TWO_TO_31M1; // rand/(2^31- 1)
	}

	/**
	 * Generate random double on open <code>(0, 1)</code>-interval
	 * <p>
	 * From <code>mt19937ar.c</code>: generates a random number on
	 * <code>(0,1)</code>-real-interval corresponds to:
	 * <code>double genrand_real3(void)</code>.
	 * </p>
	 * <p>
	 * <strong>Note:</strong> one bit lost compared to original due to signed
	 * <code>int</code>.
	 * </p>
	 * 
	 * @return random <code>double</code> in <code>(0, 1)</code>
	 */
	synchronized public double nextDoubleOpen() {
		return (nextInt() + 0.5) * TWO_TO_NEG31; // (rand+0.5)/(2^31)
	}

	/**
	 * Generates a random <code>float</code> on half-open
	 * <code>[0, 1)</code>-interval.
	 * <p>
	 * <strong>Note:</strong>
	 * <ul>
	 * <li>Twice as fast as {@link #nextDouble()}.
	 * <li>Do <em>not</em> use in GWT applications (<code>float</code>'s create
	 * overhead).
	 * </ul>
	 * 
	 * @return random <code>float</code> in <code>[0, 1)</code>
	 */
	synchronized public float nextFloat() {
		return nextInt() * TWO_TO_NEG31f;
	}

	private static final double TWO_TO_27 = 134217728.0;
	private static final float TWO_TO_31f = 2147483648f;
	private static final double TWO_TO_31M1 = 2147483647.0;
	private static final double TWO_TO_32 = 4294967296.0;
	private static final double TWO_TO_53 = 9007199254740992.0;
	private static final double TWO_TO_NEG24 = 8.0 / TWO_TO_27;
	private static final double TWO_TO_NEG25 = 4.0 / TWO_TO_27;
	private static final double TWO_TO_NEG27 = 1.0 / TWO_TO_27;
	private static final float TWO_TO_NEG31f = 1f / TWO_TO_31f;
	private static final double TWO_TO_NEG31 = 2.0 / TWO_TO_32;
	private static final double INV_TWO_TO_31M1 = 1.0 / (TWO_TO_31M1);
	private static final double TWO_TO_NEG52 = 2.0 / TWO_TO_53;
	private static final double TWO_TO_NEG53 = 1.0 / TWO_TO_53;

	/**
	 * Clears the internal Gaussian variable from the RNG. You only need to do this
	 * in the rare case that you need to guarantee that two RNGs have identical
	 * internal state. Otherwise, disregard this method.
	 * 
	 * @see #stateEquals(MersenneTwister)
	 */
	synchronized public void clearGaussian() {
		__haveNextNextGaussian = false;
	}

	/**
	 * Generate random number from standard normal distribution (Gaussian
	 * distribution).
	 * 
	 * @return random number
	 */
	synchronized public double nextGaussian() {
		if (__haveNextNextGaussian) {
			__haveNextNextGaussian = false;
			return __nextNextGaussian;
		}
		double v1, v2, s;
		do {
//			int y = nextUInt();
//			int z = nextUInt();
//			int a = nextUInt();
//			int b = nextUInt();
//			/* derived from nextDouble documentation in jdk 1.2 docs, see top */
//			v1 = 2 * (((((long)(y >>> 6)) << 27) + (z >>> 5)) / (double)(1L << 53))	- 1;
//			v2 = 2 * (((((long)(a >>> 6)) << 27) + (b >>> 5)) / (double)(1L << 53))	- 1;
			// ChH: long's are killers for GWT performance
			// return ((((long)(y >>> 5)) << 26) + (z >>> 6)) / (double)(1L << 53);
			v1 = (nextUInt() >>> 5) * TWO_TO_NEG25 + (nextUInt() >>> 6) * TWO_TO_NEG52 - 1;
			v2 = (nextUInt() >>> 5) * TWO_TO_NEG25 + (nextUInt() >>> 6) * TWO_TO_NEG52 - 1;
			s = v1 * v1 + v2 * v2;
		} while (s >= 1 || s == 0);
		// ChH: benefits of StrictMath not transparent... Math.sqrt/log delegate to
		// StrictMath.sqrt/log, which delegate to public static native double
		// sqrt/log(double a)...
		// double multiplier = StrictMath.sqrt(-2 * StrictMath.log(s)/s);
		double multiplier = Math.sqrt(-2.0 * Math.log(s) / s);
		__nextNextGaussian = v2 * multiplier;
		__haveNextNextGaussian = true;
		return v1 * multiplier;
	}

	/**
	 * Clone this MersenneTwister to ensure both objects return identical sequences
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
	 * @return clone of MersenneTwister random number generator
	 */
//	@Override
	@SuppressWarnings("all")
	synchronized public MersenneTwister clone() {
		MersenneTwister clone = new MersenneTwister();
		System.arraycopy(this.mt, 0, clone.mt, 0, N);
		clone.mti = this.mti;
		clone.__haveNextNextGaussian = this.__haveNextNextGaussian;
		clone.__nextNextGaussian = this.__nextNextGaussian;
		return clone;
	}

	/**
	 * ChH: for testing on GWT/JRE platforms logging needs to be rewritten
	 */
	private static StringBuilder buffer = new StringBuilder();

	/**
	 * Append message to buffer (no newline added).
	 * 
	 * @param msg message to append
	 */
	private static void appendProgress(String msg) {
		buffer.append(msg);
	}

	/**
	 * Append newline to buffer.
	 */
	private static void logProgress() {
		buffer.append('\n');
	}

	/**
	 * Append message to buffer (adding newline).
	 * 
	 * @param msg message to append
	 */
	private static void logProgress(String msg) {
		buffer.append(msg + '\n');
	}

	/*
	 * private long uInt(int i) { // must explicitly specify long mask - casting
	 * BIT32_MASK to long does not work! return (i & 0xffffffffL); }
	 */

	/**
	 * Tests if generated random numbers comply with reference
	 * <code>mt19937ar.c</code>.
	 * 
	 * @param logger for reporting progress and results
	 */
	public static void testCorrectness(Logger logger) {
		// references from running mt19937ar.c
		// 1000th number of genrand_int32()
		final long REFERENCE_INT_1000 = 3460025646L;
		// followed by 1000th number of genrand_real2()
		final double REFERENCE_DOUBLE_1000 = 0.72157151577994227409;
		// followed by 1000th number of genrand_res53()
		final double REFERENCE_DOUBLE_HIGH_1000 = 0.07408276207612884967;

		boolean verbose = logger.getLevel().intValue() <= Level.FINE.intValue();
		logProgress("Check MersenneTwister (comparison to output of mt19937ar.c original)");

		MersenneTwister r = new MersenneTwister(new int[] { 0x123, 0x234, 0x345, 0x456 });
		appendProgress("Testing nextUInt():       (32bit random 'unsigned' int)... ");
		long l = -Long.MAX_VALUE;
		for (int j = 0; j < 1000; j++) {
			// first, convert the int from signed to "unsigned"
			l = r.nextUInt();
			if (!verbose)
				continue;
			if (l < 0)
				l += 4294967296L; // max int value
			String s = String.valueOf(l);
			while (s.length() < 10)
				s = " " + s; // buffer
			appendProgress(s + " ");
			if (j % 5 == 4)
				logProgress();
		}
		if (l < 0)
			l += 4294967296L; // max int value
		logProgress(l == REFERENCE_INT_1000 ? "Passed!" : "FAILED: expected " + REFERENCE_INT_1000 + ", got " + l);

		appendProgress("Testing nextDouble():     (31bit low precision float)...   ");
		double d = Double.MAX_VALUE;
		for (int j = 0; j < 1000; j++) {
			d = r.nextDouble();
			if (!verbose)
				continue;
			String s = String.valueOf(l);
			while (s.length() < 10)
				s = " " + s; // buffer
			appendProgress(s + " ");
			if (j % 5 == 4)
				logProgress();
		}
		double diff = Math.abs(d - REFERENCE_DOUBLE_1000);
		logProgress(diff < 1e-12 ? "Passed! Δ=" + diff : "FAILED: expected " + REFERENCE_DOUBLE_1000 + ", got " + d);

		appendProgress("Testing nextDoubleHigh(): (53bit high precision float)...  ");
		for (int j = 0; j < 1000; j++) {
			d = r.nextDoubleHigh();
			if (!verbose)
				continue;
			String s = String.valueOf(l);
			while (s.length() < 10)
				s = " " + s; // buffer
			appendProgress(s + " ");
			if (j % 5 == 4)
				logProgress();
		}
		diff = Math.abs(d - REFERENCE_DOUBLE_HIGH_1000);
		appendProgress(
				diff < 1e-12 ? "Passed! Δ=" + diff : "FAILED: expected " + REFERENCE_DOUBLE_HIGH_1000 + ", got " + d);
		logger.info(buffer.toString());
		buffer.setLength(0);
	}

	/**
	 * Tests speed of MersenneTwister as compared to {@link java.util.Random}.
	 * 
	 * @param logger   for reporting progress and results
	 * @param clock    device for measuring elapsed time
	 * @param nSamples number of samples for testing speed
	 */
	public static void testSpeed(Logger logger, Chronometer clock, int nSamples) {
		final long SEED = 4357;

		logProgress("Check speed of MersenneTwister (comparison to java.util.Random)");

		Random rr = new Random(SEED);
		int xx = 0;
		int ms = clock.elapsedTimeMsec();
		logProgress("Testing Random().nextInt(): ");
		for (int j = 0; j < nSamples; j++)
			xx += rr.nextInt();
		int random = clock.elapsedTimeMsec() - ms;
		logProgress(random + "msec for " + Formatter.formatSci(nSamples, 0) + " samples (checksum: " + xx + ").");

		MersenneTwister r = new MersenneTwister(SEED);
		xx = 0;
		ms = clock.elapsedTimeMsec();
		logProgress("Testing MersenneTwister().nextInt(): ");
		for (int j = 0; j < nSamples; j++)
			xx += r.nextInt();
		int twister = clock.elapsedTimeMsec() - ms;
		logProgress(twister + "msec for " + Formatter.formatSci(nSamples, 0) + " samples (checksum: " + xx + ").");
		appendProgress((twister < random ? "MersenneTwister" : "Random") + " wins - speedup is "
				+ (twister < random ? (int) (100.0 * random / twister) * 0.01 : (int) (100.0 * twister / random) * 0.01)
				+ " fold!");
		logger.info(buffer.toString());
		buffer.setLength(0);
	}

	/**
	 * The minimal <code>Chronometer</code> interface is only used to hide
	 * differences for measuring execution time in JRE and GWT. This is inspired by
	 * GWT's Duration.
	 */
	public interface Chronometer {

		/**
		 * Calculate elapsed time since instantiation in milliseconds.
		 * 
		 * @return time in milliseconds
		 */
		int elapsedTimeMsec();
	}

	/**
	 * Minimal implementation of Chronometer. Measures elapsed time using the system
	 * clock.
	 * 
	 * @author Christoph Hauert
	 */
	static class StopWatch implements Chronometer {

		/**
		 * Starting time
		 */
		long start = Long.MAX_VALUE;

		/**
		 * Construct new stop watch and start measuring time.
		 */
		public StopWatch() {
			start();
		}

		/**
		 * Start/reset chronometer.
		 */
		public void start() {
			start = System.currentTimeMillis();
		}

		@Override
		public int elapsedTimeMsec() {
			return (int) (System.currentTimeMillis() - start);
		}
	}

	/**
	 * Tests the code.
	 * 
	 * @param args command line arguments - ignored
	 */
	public static void main(String args[]) {
		Logger logger = Logger.getLogger(MersenneTwister.class.getName());
		StopWatch clock = new StopWatch();

		MersenneTwister.testCorrectness(logger);
		MersenneTwister.testSpeed(logger, clock, 100000000);

		// TEST TO COMPARE TYPE CONVERSION BETWEEN
		// MersenneTwisterFast.java AND MersenneTwister.java

		final long SEED = 4357;
		logProgress("\nGrab the first 1000 booleans");
		MersenneTwister r = new MersenneTwister(SEED);
		int j;
		for (j = 0; j < 1000; j++) {
			appendProgress(r.nextBoolean() + " ");
			if (j % 8 == 7)
				logProgress();
		}
		if (!(j % 8 == 7))
			logProgress();

		logProgress("\nGrab 1000 booleans of increasing probability using nextBoolean(double)");
		r = new MersenneTwister(SEED);
		for (j = 0; j < 1000; j++) {
			appendProgress(r.nextBoolean(j / 999.0) + " ");
			if (j % 8 == 7)
				logProgress();
		}
		if (!(j % 8 == 7))
			logProgress();

		logProgress("\nGrab 1000 booleans of increasing probability using nextBoolean(float)");
		r = new MersenneTwister(SEED);
		for (j = 0; j < 1000; j++) {
			appendProgress(r.nextBoolean(j / 999.0f) + " ");
			if (j % 8 == 7)
				logProgress();
		}
		if (!(j % 8 == 7))
			logProgress();

		byte[] bytes = new byte[1000];
		logProgress("\nGrab the first 1000 bytes using nextBytes");
		r = new MersenneTwister(SEED);
		r.nextBytes(bytes);
		for (j = 0; j < 1000; j++) {
			appendProgress(bytes[j] + " ");
			if (j % 16 == 15)
				logProgress();
		}
		if (!(j % 16 == 15))
			logProgress();

		byte b;
		logProgress("\nGrab the first 1000 bytes -- must be same as nextBytes");
		r = new MersenneTwister(SEED);
		for (j = 0; j < 1000; j++) {
			appendProgress((b = r.nextByte()) + " ");
			if (b != bytes[j])
				appendProgress("BAD ");
			if (j % 16 == 15)
				logProgress();
		}
		if (!(j % 16 == 15))
			logProgress();

		logProgress("\nGrab the first 1000 shorts");
		r = new MersenneTwister(SEED);
		for (j = 0; j < 1000; j++) {
			appendProgress(r.nextShort() + " ");
			if (j % 8 == 7)
				logProgress();
		}
		if (!(j % 8 == 7))
			logProgress();

		logProgress("\nGrab the first 1000 ints");
		r = new MersenneTwister(SEED);
		for (j = 0; j < 1000; j++) {
			appendProgress(r.nextInt() + " ");
			if (j % 4 == 3)
				logProgress();
		}
		if (!(j % 4 == 3))
			logProgress();

		logProgress("\nGrab the first 1000 ints of different sizes");
		r = new MersenneTwister(SEED);
		int max = 1;
		for (j = 0; j < 1000; j++) {
			appendProgress(r.nextInt(max) + " ");
			max *= 2;
			if (max <= 0)
				max = 1;
			if (j % 4 == 3)
				logProgress();
		}
		if (!(j % 4 == 3))
			logProgress();

		logProgress("\nGrab the first 1000 longs");
		r = new MersenneTwister(SEED);
		for (j = 0; j < 1000; j++) {
			appendProgress(r.nextLong() + " ");
			if (j % 3 == 2)
				logProgress();
		}
		if (!(j % 3 == 2))
			logProgress();

		logProgress("\nGrab the first 1000 longs of different sizes");
		r = new MersenneTwister(SEED);
		long max2 = 1;
		for (j = 0; j < 1000; j++) {
			appendProgress(r.nextLong(max2) + " ");
			max2 *= 2;
			if (max2 <= 0)
				max2 = 1;
			if (j % 4 == 3)
				logProgress();
		}
		if (!(j % 4 == 3))
			logProgress();

		logProgress("\nGrab the first 1000 floats");
		r = new MersenneTwister(SEED);
		for (j = 0; j < 1000; j++) {
			appendProgress(r.nextFloat() + " ");
			if (j % 4 == 3)
				logProgress();
		}
		if (!(j % 4 == 3))
			logProgress();

		logProgress("\nGrab the first 1000 doubles");
		r = new MersenneTwister(SEED);
		for (j = 0; j < 1000; j++) {
			appendProgress(r.nextDouble() + " ");
			if (j % 3 == 2)
				logProgress();
		}
		if (!(j % 3 == 2))
			logProgress();

		logProgress("\nGrab the first 1000 gaussian doubles");
		r = new MersenneTwister(SEED);
		for (j = 0; j < 1000; j++) {
			appendProgress(r.nextGaussian() + " ");
			if (j % 3 == 2)
				logProgress();
		}
		if (!(j % 3 == 2))
			logProgress();
	}
}
