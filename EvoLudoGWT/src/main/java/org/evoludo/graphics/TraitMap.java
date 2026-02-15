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

import org.evoludo.geom.Point2D;
import org.evoludo.math.ArrayMath;
import org.evoludo.simulator.views.BasicTooltipProvider;
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
public class TraitMap implements Data2Phase, BasicTooltipProvider {

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
	 * Flag indicating whether the axes are fixed. The default is fixed axes.
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

	@Override
	public void reset() {
		minX = Double.POSITIVE_INFINITY;
		maxX = Double.NEGATIVE_INFINITY;
		minY = Double.POSITIVE_INFINITY;
		maxY = Double.NEGATIVE_INFINITY;
	}

	@Override
	public void setTraits(int[] x, int[] y) {
		stateX = ArrayMath.clone(x);
		stateY = ArrayMath.clone(y);
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
		// NOTE: data[0] is time
		double x = data[stateX[0] + 1];
		int nx = stateX.length;
		if (nx > 1) {
			for (int n = 1; n < nx; n++)
				x += data[stateX[n] + 1];
		}
		minX = Math.min(minX, x);
		maxX = Math.max(maxX, x);
		double y = data[stateY[0] + 1];
		int ny = stateY.length;
		if (ny > 1) {
			for (int n = 1; n < ny; n++)
				y += data[stateY[n] + 1];
		}
		minY = Math.min(minY, y);
		maxY = Math.max(maxY, y);
		point.set(x, y);
		return true;
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
		if (!Double.isFinite(minX))
			minX = findMin(buffer, stateX);
		return minX;
	}

	@Override
	public double getMaxX(RingBuffer<double[]> buffer) {
		if (!Double.isFinite(maxX))
			maxX = findMax(buffer, stateX);
		return maxX;
	}

	@Override
	public double getMinY(RingBuffer<double[]> buffer) {
		if (!Double.isFinite(minY))
			minY = findMin(buffer, stateY);
		return minY;
	}

	@Override
	public double getMaxY(RingBuffer<double[]> buffer) {
		if (!Double.isFinite(maxY))
			maxY = findMax(buffer, stateY);
		return maxY;
	}

	/**
	 * Find the minimum value of the data in the buffer accross all the indices in
	 * <code>idxs</code>.
	 * 
	 * @param buffer the buffer with data points
	 * @param idxs   the array of indices
	 * @return the minimum value
	 */
	private double findMin(RingBuffer<double[]> buffer, int[] idxs) {
		double min = Double.POSITIVE_INFINITY;
		for (double[] data : buffer) {
			double d = data[idxs[0] + 1];
			int nd = idxs.length;
			if (nd > 1) {
				for (int n = 1; n < nd; n++)
					d += data[idxs[n] + 1];
			}
			min = Math.min(min, d);
		}
		return min;
	}

	/**
	 * Find the maximum value of the data in the buffer accross all the indices in
	 * <code>idxs</code>.
	 * 
	 * @param buffer the buffer with data points
	 * @param idxs   the array of indices
	 * @return the maximum value
	 */
	private double findMax(RingBuffer<double[]> buffer, int[] idxs) {
		double max = Double.NEGATIVE_INFINITY;
		for (double[] data : buffer) {
			double d = data[idxs[0] + 1];
			int nd = idxs.length;
			if (nd > 1) {
				for (int n = 1; n < nd; n++)
					d += data[idxs[n] + 1];
			}
			max = Math.max(max, d);
		}
		return max;
	}

	@Override
	public String getTooltipAt(double x, double y) {
		StringBuilder sb = new StringBuilder(TABLE_STYLE + TABLE_ROW_START_RIGHT);
		sb.append(this.paraGraph.style.xLabel) //
				.append(TABLE_CELL_NEXT)
				.append(this.paraGraph.style.percentX ? Formatter.formatPercent(x, 2)
						: Formatter.format(x, 2))
				.append(TABLE_ROW_END);
		sb.append(TABLE_ROW_START_RIGHT) //
				.append(this.paraGraph.style.yLabel) //
				.append(TABLE_CELL_NEXT)
				.append(this.paraGraph.style.percentY ? Formatter.formatPercent(y, 2)
						: Formatter.format(y, 2))
				.append(TABLE_ROW_END);
		sb.append(TABLE_END);
		return sb.toString();
	}
}
