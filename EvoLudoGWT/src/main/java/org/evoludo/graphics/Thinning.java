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

package org.evoludo.graphics;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.IntSupplier;

import org.evoludo.graphics.AbstractGraph.Zooming;
import org.evoludo.util.RingBuffer;

/**
 * Thinning of trajectories and time series to conserve memory without losing
 * visible detail. Samples, i.e. states with the time prepended, are added to a
 * ring buffer only if the straight line between the stored samples
 * misrepresents the data, analogous to the error control of numerical
 * integration with adaptive step sizes.
 * <p>
 * The most recent sample is always added to the buffer, but provisionally: the
 * next sample replaces it, unless the samples received since the last stored
 * sample deviate from the straight line between the last stored and the next
 * sample by more than the tolerance. In that case the provisional sample is
 * stored for good. Hence, the buffer always ends with the current state.
 * <p>
 * Subclasses determine how deviations are measured:
 * <dl>
 * <dt>{@link Trajectory}</dt>
 * <dd>Trajectories in state space, e.g. in phase planes or simplices:
 * deviations perpendicular to the chord, relative to the length of the
 * chord.</dd>
 * <dt>{@link TimeSeries}</dt>
 * <dd>Time series of one or more values that share the same axis, e.g. in line
 * graphs: deviations from the linear interpolation at the same time, relative
 * to the extent of the values.</dd>
 * </dl>
 * <p>
 * Each graph keeps its own instance, which must be reset whenever a new
 * trajectory or time series starts.
 */
abstract class Thinning {

	/**
	 * The maximum number of samples received since the last stored sample. The
	 * provisional sample is stored once reached, which limits the effort of
	 * checking deviations, e.g. for long straight stretches.
	 */
	static final int MAX_PENDING = 64;

	/**
	 * The relative extent below which coordinates are considered constant and
	 * ignored, e.g. to prevent numerical noise from adding samples.
	 */
	static final double MIN_EXTENT = 1e-12;

	/**
	 * The samples received since the last stored sample. The most recent one is
	 * the provisional sample in the buffer.
	 */
	final List<double[]> pending = new ArrayList<>();

	/**
	 * The minimum values of the coordinates, or {@code null} if no sample was
	 * added since the last reset.
	 */
	double[] min;

	/**
	 * The maximum values of the coordinates, or {@code null} if no sample was
	 * added since the last reset.
	 */
	double[] max;

	/**
	 * The flag to indicate whether the most recent sample in the buffer is
	 * provisional, i.e. replaced by the next sample unless the data requires it.
	 */
	private boolean provisional = false;

	/**
	 * Check whether all samples received since the last stored sample
	 * {@code anchor}, see {@link #pending}, remain within the tolerance of the
	 * straight line between {@code anchor} and {@code sample}. Both samples have
	 * the same length.
	 *
	 * @param anchor the last stored sample
	 * @param sample the new sample
	 * @return {@code true} if the straight line represents the samples
	 */
	abstract boolean fits(double[] anchor, double[] sample);

	/**
	 * Check whether samples with non-finite coordinates must be stored for good.
	 * The default is {@code false}.
	 *
	 * @return {@code true} if samples with non-finite coordinates are stored for
	 *         good
	 */
	boolean keepNonFinite() {
		return false;
	}

	/**
	 * Start a new trajectory or time series with the sample {@code start}, which
	 * the caller has stored in the buffer for good.
	 *
	 * @param start the first sample (time prepended)
	 */
	void reset(double[] start) {
		pending.clear();
		provisional = false;
		min = null;
		max = null;
		if (start != null)
			updateExtent(start);
	}

	/**
	 * Store the provisional sample, if any, for good, e.g. because it marks a
	 * change in the direction of time.
	 */
	void commit() {
		provisional = false;
		pending.clear();
	}

	/**
	 * Add the sample {@code sample} to the buffer {@code buffer}, which must
	 * contain at least the first sample. The sample replaces the provisional
	 * sample, unless the provisional sample is required to represent the data. If
	 * {@code force} is {@code true} the sample is stored for good.
	 *
	 * @param buffer the buffer of samples
	 * @param sample the sample to add (time prepended)
	 * @param force  {@code true} to store the sample for good
	 */
	void add(RingBuffer<double[]> buffer, double[] sample, boolean force) {
		if (min == null || min.length != sample.length)
			reset(buffer.last());
		boolean finite = updateExtent(sample);
		boolean keep = force || (!finite && keepNonFinite());
		// the provisional sample needs the stored one before it as reference
		if (provisional && buffer.getSize() < 2)
			provisional = false;
		if (provisional && (keep || pending.size() >= MAX_PENDING || !fitsAnchor(buffer.get(1), sample)))
			// keep the provisional sample for good
			commit();
		if (provisional)
			buffer.replace(sample);
		else
			buffer.append(sample);
		pending.add(sample);
		if (keep) {
			commit();
			return;
		}
		provisional = true;
	}

	/**
	 * Check whether the straight line between {@code anchor} and {@code sample}
	 * represents the samples received since {@code anchor}. Samples of different
	 * lengths never fit.
	 *
	 * @param anchor the last stored sample
	 * @param sample the new sample
	 * @return {@code true} if the straight line represents the samples
	 */
	private boolean fitsAnchor(double[] anchor, double[] sample) {
		return anchor.length == sample.length && fits(anchor, sample);
	}

	/**
	 * Extend the extent of the coordinates to include {@code sample}.
	 *
	 * @param sample the sample (time prepended)
	 * @return {@code false} if the sample contains non-finite coordinates
	 */
	private boolean updateExtent(double[] sample) {
		int len = sample.length;
		if (min == null || min.length != len) {
			min = new double[len];
			max = new double[len];
			Arrays.fill(min, Double.POSITIVE_INFINITY);
			Arrays.fill(max, Double.NEGATIVE_INFINITY);
		}
		boolean finite = true;
		for (int n = 1; n < len; n++) {
			double x = sample[n];
			if (!Double.isFinite(x)) {
				finite = false;
				continue;
			}
			min[n] = Math.min(min[n], x);
			max[n] = Math.max(max[n], x);
		}
		return finite;
	}

	/**
	 * Thinning of trajectories in state space, e.g. in phase planes or simplices.
	 * Deviations are measured perpendicular to the chord, relative to the length
	 * of the chord, which is scale-free: loops of any size are resolved by roughly
	 * the same number of samples, regardless of whether the trajectory spirals
	 * outwards from an unstable focus or inwards to a stable one. In order to
	 * compare coordinates of different magnitudes, each coordinate is normalized
	 * by the extent of the trajectory, i.e. the range of values it has taken.
	 * Deviations smaller than what remains visible at the maximum zoom level,
	 * relative to the extent, are ignored. Otherwise an endless approach of a
	 * stable focus would keep adding samples and eventually push the beginning of
	 * the trajectory out of the ring buffer. Samples with non-finite coordinates
	 * are stored for good because chords to them are undefined.
	 */
	static final class Trajectory extends Thinning {

		/**
		 * The maximum deviation of samples from the chord, relative to the length of
		 * the chord. For circles, this amounts to about 40 samples per loop.
		 */
		static final double TOLERANCE = 0.02;

		/**
		 * The minimum deviation of samples from the chord that is considered,
		 * relative to the extent of the trajectory: one pixel of a plot, which is at
		 * most 2000 pixels wide, at the maximum zoom level.
		 */
		static final double FLOOR = 1.0 / (Zooming.ZOOM_MAX * 2000.0);

		/**
		 * The weights of the coordinates, i.e. the inverse of the extent of the
		 * trajectory, or zero for coordinates considered constant. Reused to avoid
		 * allocations.
		 */
		private double[] weight;

		@Override
		boolean keepNonFinite() {
			return true;
		}

		@Override
		boolean fits(double[] anchor, double[] sample) {
			int len = sample.length;
			if (weight == null || weight.length != len)
				weight = new double[len];
			// skip time at index 0
			double chordSq = 0.0;
			for (int n = 1; n < len; n++) {
				double extent = max[n] - min[n];
				double scale = Math.max(1.0, Math.max(Math.abs(min[n]), Math.abs(max[n])));
				weight[n] = extent > MIN_EXTENT * scale ? 1.0 / extent : 0.0;
				double d = (sample[n] - anchor[n]) * weight[n];
				chordSq += d * d;
			}
			double tol = Math.max(TOLERANCE * Math.sqrt(chordSq), FLOOR);
			double tolSq = tol * tol;
			for (double[] point : pending) {
				if (distSq(point, anchor, sample, chordSq) > tolSq)
					return false;
			}
			return true;
		}

		/**
		 * Compute the squared distance of {@code point} from the segment between
		 * {@code from} and {@code to} in normalized coordinates. Points beyond the
		 * ends of the segment are measured from the nearest end, such that e.g.
		 * reversals of direction are detected.
		 *
		 * @param point   the point
		 * @param from    the start of the segment
		 * @param to      the end of the segment
		 * @param chordSq the squared length of the segment in normalized
		 *                coordinates
		 * @return the squared distance
		 */
		private double distSq(double[] point, double[] from, double[] to, double chordSq) {
			int len = point.length;
			double dot = 0.0;
			for (int n = 1; n < len; n++)
				dot += (point[n] - from[n]) * (to[n] - from[n]) * weight[n] * weight[n];
			double t = chordSq > 0.0 ? Math.max(0.0, Math.min(1.0, dot / chordSq)) : 0.0;
			double dSq = 0.0;
			for (int n = 1; n < len; n++) {
				double d = (point[n] - from[n] - t * (to[n] - from[n])) * weight[n];
				dSq += d * d;
			}
			return dSq;
		}
	}

	/**
	 * Thinning of time series of one or more values that share the same axis,
	 * e.g. in line graphs. Time is not a geometric dimension: deviations are
	 * measured vertically, i.e. as the difference between the values and their
	 * linear interpolation at the same time, relative to the extent of all values
	 * (on a logarithmic scale, if applicable). Hence, long flat stretches cannot
	 * swallow small changes. Only the values from index {@code 1} (after the time)
	 * through {@link #lastIdx} are considered; further entries, if any, are
	 * ignored.
	 */
	static final class TimeSeries extends Thinning {

		/**
		 * The maximum deviation of values from their linear interpolation, relative
		 * to the extent of the values: half a pixel of a plot, which is at most 2000
		 * pixels high, at ten-fold magnification of the values.
		 */
		static final double TOLERANCE = 0.5 / (10.0 * 2000.0);

		/**
		 * The index of the last value of samples that is considered.
		 */
		private final IntSupplier lastIdx;

		/**
		 * The flag to indicate whether values are shown on a logarithmic scale.
		 */
		private final BooleanSupplier logScale;

		/**
		 * Create a new thinning for time series with values from index {@code 1}
		 * through index {@code lastIdx} of the samples, which are shown on a
		 * logarithmic scale if {@code logScale} is {@code true}.
		 *
		 * @param lastIdx  the index of the last value considered
		 * @param logScale the flag whether values are shown on a logarithmic scale
		 */
		TimeSeries(IntSupplier lastIdx, BooleanSupplier logScale) {
			this.lastIdx = lastIdx;
			this.logScale = logScale;
		}

		/**
		 * {@inheritDoc}
		 * <p>
		 * On a logarithmic scale, values and extent are compared in logarithms.
		 * Values that are not finite (or not positive on a logarithmic scale) must be
		 * so in all three samples to fit.
		 */
		@Override
		boolean fits(double[] anchor, double[] sample) {
			boolean log = logScale.getAsBoolean();
			int last = Math.min(lastIdx.getAsInt(), sample.length - 1);
			// common extent of all values
			double lo = Double.POSITIVE_INFINITY;
			double hi = Double.NEGATIVE_INFINITY;
			for (int n = 1; n <= last; n++) {
				double a = transform(min[n], log);
				double b = transform(max[n], log);
				if (Double.isFinite(a))
					lo = Math.min(lo, a);
				if (Double.isFinite(b))
					hi = Math.max(hi, b);
			}
			double extent = hi - lo;
			double scale = Math.max(1.0, Math.max(Math.abs(lo), Math.abs(hi)));
			if (extent <= MIN_EXTENT * scale)
				// values (nearly) constant, or none finite
				return true;
			double tol = TOLERANCE * extent;
			double dt = sample[0] - anchor[0];
			for (double[] point : pending) {
				double f = dt != 0.0 ? Math.max(0.0, Math.min(1.0, (point[0] - anchor[0]) / dt)) : 0.0;
				for (int n = 1; n <= last; n++) {
					double a = transform(anchor[n], log);
					double b = transform(sample[n], log);
					double p = transform(point[n], log);
					boolean fa = Double.isFinite(a);
					if (fa != Double.isFinite(b) || fa != Double.isFinite(p))
						return false;
					if (fa && Math.abs(p - (a + f * (b - a))) > tol)
						return false;
				}
			}
			return true;
		}

		/**
		 * Transform the value {@code x} to the scale of the axis.
		 *
		 * @param x   the value
		 * @param log {@code true} for a logarithmic scale
		 * @return the transformed value, or {@code NaN} if the value cannot be shown
		 */
		private static double transform(double x, boolean log) {
			if (!log)
				return x;
			return x > 0.0 ? Math.log10(x) : Double.NaN;
		}
	}
}
