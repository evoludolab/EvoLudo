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

import static org.evoludo.simulator.views.BasicTooltipProvider.tableRowRight;

import org.evoludo.geom.Point2D;
import org.evoludo.math.ArrayMath;
import org.evoludo.simulator.views.HasPhase2D;
import org.evoludo.simulator.views.HasPhase2D.Data2Phase;
import org.evoludo.util.Formatter;
import org.evoludo.util.RingBuffer;

/**
 * Default mapping of data to the phase plane or phase plane projections.
 * Projections can be the sum of several dynamical variables. Custom
 * implementations of the {@code Data2Phase} interface can be provided by
 * modules that implement the {@code HasPhase2D} interface.
 * 
 * @see HasPhase2D#getPhase2DMap()
 */
public class TraitMap implements Data2Phase {

	/**
	 * The ParaGraph instance.
	 */
	private final ParaGraph paraGraph;

	/**
	 * Construct a TraitMap for the given ParaGraph.
	 * 
	 * @param paraGraph the ParaGraph instance
	 */
	TraitMap(ParaGraph paraGraph) {
		this.paraGraph = paraGraph;
	}

	/**
	 * Flag indicating whether the axes are fixed. The default is axes that can be
	 * changed.
	 */
	boolean hasFixedAxes = false;

	/**
	 * The array of trait indices that are mapped to the <code>x</code>-axis.
	 */
	protected int[] stateX = new int[] { 0 };

	/**
	 * The array of trait indices that are mapped to the <code>y</code>-axis.
	 */
	protected int[] stateY = new int[] { 1 };

	/**
	 * The minimum value of the <code>x</code>-axis.
	 */
	protected double minX;

	/**
	 * The maximum value of the <code>x</code>-axis.
	 */
	protected double maxX;

	/**
	 * The minimum value of the <code>y</code>-axis.
	 */
	protected double minY;

	/**
	 * The maximum value of the <code>y</code>-axis.
	 */
	protected double maxY;

	/**
	 * The most recent data point included in the extremes, {@link #minX},
	 * {@link #maxX}, {@link #minY} and {@link #maxY}.
	 */
	private double[] lastData;

	@Override
	public void reset() {
		minX = Double.POSITIVE_INFINITY;
		maxX = Double.NEGATIVE_INFINITY;
		minY = Double.POSITIVE_INFINITY;
		maxY = Double.NEGATIVE_INFINITY;
		lastData = null;
	}

	@Override
	public void setTraits(int[] x, int[] y) {
		stateX = ArrayMath.clone(x);
		stateY = ArrayMath.clone(y);
		// extremes refer to previous traits
		reset();
	}

	@Override
	public int[] getTraitsX() {
		return stateX;
	}

	@Override
	public int[] getTraitsY() {
		return stateY;
	}

	@Override
	public boolean hasMultitrait() {
		return true;
	}

	@Override
	public boolean hasFixedAxes() {
		return hasFixedAxes;
	}

	@Override
	public void setFixedAxes(boolean hasFixedAxes) {
		this.hasFixedAxes = hasFixedAxes;
	}

	@Override
	public boolean data2Phase(double[] data, Point2D point) {
		point.set(sum(data, stateX), sum(data, stateY));
		return true;
	}

	/**
	 * Sum the values of {@code data} across all the trait indices in {@code idxs}.
	 *
	 * @param data the data point (with time as the first element)
	 * @param idxs the array of trait indices
	 * @return the sum of the values
	 */
	private static double sum(double[] data, int[] idxs) {
		// NOTE: data[0] is time
		double sum = 0.0;
		for (int idx : idxs)
			sum += data[idx + 1];
		return sum;
	}

	/**
	 * Extend the extremes by the data points added to {@code buffer} since the
	 * last update. Only buffered data counts, not, e.g., mapped markers.
	 *
	 * @param buffer the buffer with data points
	 */
	private void update(RingBuffer<double[]> buffer) {
		if (buffer == null || buffer.isEmpty() || buffer.last() == lastData)
			return;
		// iterate from the most recent data point backwards
		for (double[] data : buffer) {
			if (data == lastData)
				break;
			double x = sum(data, stateX);
			double y = sum(data, stateY);
			minX = Math.min(minX, x);
			maxX = Math.max(maxX, x);
			minY = Math.min(minY, y);
			maxY = Math.max(maxY, y);
		}
		lastData = buffer.last();
	}

	@Override
	public boolean phase2Data(Point2D point, double[] data) {
		// point is in user coordinates
		// data is the last/most recent state in buffer (excluding time!)
		if (stateX.length != 1 || stateY.length != 1)
			return false;
		// conversion only possible if each phase plane axis represents a single
		// dynamical variable, i.e. no aggregates
		data[stateX[0]] = point.getX();
		data[stateY[0]] = point.getY();
		return true;
	}

	@Override
	public double getMinX(RingBuffer<double[]> buffer) {
		update(buffer);
		return minX;
	}

	@Override
	public double getMaxX(RingBuffer<double[]> buffer) {
		update(buffer);
		return maxX;
	}

	@Override
	public double getMinY(RingBuffer<double[]> buffer) {
		update(buffer);
		return minY;
	}

	@Override
	public double getMaxY(RingBuffer<double[]> buffer) {
		update(buffer);
		return maxY;
	}

	@Override
	public String getTooltipAt(double x, double y) {
		GraphStyle style = paraGraph.style;
		return TABLE_STYLE
				+ tableRowRight(style.xLabel, style.percentX ? Formatter.formatPercent(x, 2) : Formatter.format(x, 2))
				+ tableRowRight(style.yLabel, style.percentY ? Formatter.formatPercent(y, 2) : Formatter.format(y, 2))
				+ TABLE_END;
	}
}
