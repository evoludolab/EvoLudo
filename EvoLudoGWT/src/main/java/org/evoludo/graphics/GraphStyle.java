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

import org.evoludo.math.Functions;

/**
 * The style features for graphs. This is a collection of settings for line
 * styles, font sizes, ticks, padding, etc.
 */
public class GraphStyle {

	/**
	 * Positioning options for graph decorations such as legends.
	 */
	public enum Position {
		/**
		 * Top edge of the graph.
		 */
		NORTH,

		/**
		 * Bottom edge of the graph.
		 */
		SOUTH,

		/**
		 * Right edge of the graph.
		 */
		EAST,

		/**
		 * Left edge of the graph.
		 */
		WEST,

		/**
		 * Decoration disabled.
		 */
		NONE;

		/**
		 * Check whether this position is vertical.
		 *
		 * @return {@code true} for east/west
		 */
		public boolean isVertical() {
			return this == EAST || this == WEST;
		}

		/**
		 * Check whether this position is horizontal.
		 *
		 * @return {@code true} for north/south
		 */
		public boolean isHorizontal() {
			return this == NORTH || this == SOUTH;
		}
	}

	/**
	 * Create a style with the default set of axis, label, and tick options.
	 */
	public GraphStyle() {
		// defaults provided via field initializers
	}
	// public AxesStyle x = new AxesStyle();
	// public AxesStyle y = new AxesStyle();

	/**
	 * The minimum padding (in pixels) between boundaries of the HTML element and
	 * the graph.
	 */
	public int minPadding = 2;

	/**
	 * The tick length in pixels.
	 */
	public int tickLength = 6;

	/**
	 * The radius of marker points.
	 */
	public double markerSize = 3.0;

	/**
	 * The color to mark start of trajectory.
	 */
	public String startColor = "#0f0";

	/**
	 * The color to mark end of trajectory.
	 */
	public String endColor = "#f00";

	/**
	 * The minimum value of the {@code x}-axis.
	 */
	public double xMin = 0.0;

	/**
	 * The maximum value of the {@code x}-axis.
	 */
	public double xMax = 1.0;

	/**
	 * The {@code x}-axis increments.
	 */
	public double xIncr = 0.0;

	/**
	 * The minimum value of the {@code y}-axis.
	 */
	public double yMin = 0.0;

	/**
	 * The maximum value of the {@code y}-axis.
	 */
	public double yMax = 1.0;

	/**
	 * The {@code x}-axis increments.
	 */
	public double yIncr = 0.0;

	/**
	 * Set the range of the horizontal axis to {@code xSteps * xIncr}.
	 * 
	 * @param xSteps the number of horizontal steps
	 */
	public void setXRange(int xSteps) {
		double xRange = xSteps * xIncr;
		if (xRange > 0.0) {
			xMax = xMin + xRange;
			return;
		}
		if (xRange < 0.0) {
			xMin = xMax + xRange;
		}
	}

	/**
	 * Set the range of the vertical axis to {@code ySteps * yIncr}.
	 * 
	 * @param ySteps the number of vertical steps
	 */
	public void setYRange(int ySteps) {
		double yRange = ySteps * yIncr;
		if (yRange > 0.0) {
			yMax = yMin + yRange;
			return;
		}
		if (yRange < 0.0) {
			yMin = yMax + yRange;
		}
	}

	/**
	 * The margin, as a fraction of the data range, added at both ends when
	 * fitting an axis range to data. Not applied to percentage axes.
	 */
	private static final double FIT_MARGIN = 0.05;

	/**
	 * The fraction of the axis range below which the data range must drop
	 * before {@link #trackYRange(double, double)} shrinks the range.
	 */
	private static final double SHRINK_FRACTION = 0.75;

	/**
	 * The fraction of the maximum below which a linear axis minimum is
	 * considered insignificant and snapped to zero rather than given its own
	 * (arbitrarily fine-grained) axis floor.
	 */
	static final double MIN_YMAX_FRACTION = 0.01;

	/**
	 * The flag to indicate that the {@code x}-range must be validated against
	 * the next data added, i.e. at the start of a new run.
	 *
	 * @see #newRun()
	 */
	private boolean checkXRange = true;

	/**
	 * The flag to indicate whether the {@code x}-range has been adjusted to data
	 * before. If not and autoscaling is enabled, the {@code x}-range is fitted to
	 * the first data added.
	 */
	private boolean hasXRange = false;

	/**
	 * The flag to indicate that the {@code y}-range must be validated against
	 * the next data added, i.e. at the start of a new run.
	 *
	 * @see #newRun()
	 */
	private boolean checkYRange = true;

	/**
	 * The flag to indicate whether the {@code y}-range has been adjusted to data
	 * before. If not and autoscaling is enabled, the {@code y}-range is fitted to
	 * the first data added.
	 */
	private boolean hasYRange = false;

	/**
	 * Signal the start of a new run, e.g. after a reset or re-initialization.
	 * The current axis ranges are retained if the next data added fits, and
	 * otherwise fitted to that data from scratch.
	 *
	 * @see #addXData(double, double)
	 * @see #addYData(double, double)
	 */
	public void newRun() {
		checkXRange = true;
		checkYRange = true;
	}

	/**
	 * Adjust the {@code x}-range to newly added data spanning
	 * {@code [min, max]}. Same as {@link #addYData(double, double)} but for the
	 * {@code x}-axis.
	 *
	 * @param min the minimum of the new data
	 * @param max the maximum of the new data
	 * @return {@code true} if the range changed
	 */
	public boolean addXData(double min, double max) {
		boolean check = checkXRange;
		checkXRange = false;
		double[] range = adjustRange(min, max, xMin, xMax, check, !hasXRange && autoscaleX, percentX, false);
		if (check)
			hasXRange = true;
		if (range == null)
			return false;
		xMin = range[0];
		xMax = range[1];
		return true;
	}

	/**
	 * Adjust the {@code y}-range to newly added data spanning {@code [min, max]}.
	 * The range changes only if necessary, i.e. if the data does not fit: at the
	 * start of a new run (see {@link #newRun()}) the range is then fitted to the
	 * new data from scratch, otherwise it is expanded to cover the data, whether
	 * autoscaling is enabled or the limits were set explicitly. The range is
	 * never shrunk. Initially, the range is fitted to the data if autoscaling is
	 * enabled. Fitting and expanding add the same margin as
	 * {@link #fitYRange(double, double)}.
	 *
	 * @param min the minimum of the new data
	 * @param max the maximum of the new data
	 * @return {@code true} if the range changed
	 */
	public boolean addYData(double min, double max) {
		boolean check = checkYRange;
		checkYRange = false;
		double[] range = adjustRange(min, max, yMin, yMax, check, !hasYRange && autoscaleY, percentY, logScaleY);
		if (check)
			hasYRange = true;
		if (range == null)
			return false;
		yMin = range[0];
		yMax = range[1];
		return true;
	}

	/**
	 * Fit the {@code x}-range to data spanning {@code [min, max]}, e.g. to zoom
	 * to fit or when enabling autoscaling.
	 *
	 * @param min the minimum of the data
	 * @param max the maximum of the data
	 *
	 * @see #computeRange(double, double, boolean, boolean)
	 */
	public void fitXRange(double min, double max) {
		double[] range = computeRange(min, max, percentX, false);
		xMin = range[0];
		xMax = range[1];
	}

	/**
	 * Fit the {@code y}-range to data spanning {@code [min, max]}, e.g. to zoom
	 * to fit or when enabling autoscaling.
	 *
	 * @param min the minimum of the data
	 * @param max the maximum of the data
	 * @return {@code true} if the range was updated, {@code false} if no valid
	 *         range exists (non-positive data on log scale)
	 *
	 * @see #computeRange(double, double, boolean, boolean)
	 */
	public boolean fitYRange(double min, double max) {
		double[] range = computeRange(min, max, percentY, logScaleY);
		if (range == null)
			return false;
		yMin = range[0];
		yMax = range[1];
		return true;
	}

	/**
	 * Track the {@code y}-range of data that may also decrease, e.g. the bars of
	 * a histogram. The range is fitted to the data spanning {@code [min, max]}
	 * if the data does not fit or if the data range dropped below
	 * {@link #SHRINK_FRACTION} of the {@code y}-range.
	 *
	 * @param min the minimum of the data
	 * @param max the maximum of the data
	 * @return {@code true} if the range was updated
	 */
	public boolean trackYRange(double min, double max) {
		if (min >= yMin && max <= yMax && max - min >= SHRINK_FRACTION * (yMax - yMin))
			return false;
		return fitYRange(min, max);
	}

	/**
	 * Determine the new range of an axis spanning {@code [lo, hi]} after adding
	 * data spanning {@code [min, max]}. If the data does not fit, the range is
	 * fitted to the data if {@code check} is set, and otherwise only the exceeded
	 * limit(s) are expanded with a margin relative to the extended range.
	 *
	 * @param min          the minimum of the new data
	 * @param max          the maximum of the new data
	 * @param lo           the current minimum of the axis
	 * @param hi           the current maximum of the axis
	 * @param check        {@code true} at the start of a new run
	 * @param fitInitially {@code true} to fit the range even if the data fits
	 *                     (only if {@code check} is set)
	 * @param percent      {@code true} for percentage axes
	 * @param log          {@code true} for logarithmic axes
	 * @return the new range as {@code {min, max}}, or {@code null} if the range
	 *         remains unchanged
	 */
	private static double[] adjustRange(double min, double max, double lo, double hi, boolean check,
			boolean fitInitially, boolean percent, boolean log) {
		boolean fits = (min >= lo && max <= hi);
		if (check && (!fits || fitInitially))
			return computeRange(min, max, percent, log);
		if (fits)
			return null;
		double[] range = computeRange(Math.min(min, lo), Math.max(max, hi), percent, log);
		if (range == null)
			return null;
		return new double[] { min < lo ? range[0] : lo, max > hi ? range[1] : hi };
	}

	/**
	 * Compute the axis range that fits data spanning {@code [min, max]}. Except
	 * for percentage axes, a margin of {@link #FIT_MARGIN} times the data range
	 * is added at both ends, but non-negative data never results in a negative
	 * minimum. Data without range is always padded to obtain a finite range. The
	 * limits are rounded to two significant digits and an insignificant linear
	 * minimum snaps to zero (see {@link #roundLinearMin(double, double)}).
	 * Percentage axes are clamped to {@code [0, 1]}.
	 *
	 * @param min     the minimum of the data
	 * @param max     the maximum of the data
	 * @param percent {@code true} for percentage axes
	 * @param log     {@code true} for logarithmic axes
	 * @return the range as {@code {min, max}}, or {@code null} if no valid range
	 *         exists (non-positive data on log scale)
	 */
	private static double[] computeRange(double min, double max, boolean percent, boolean log) {
		if (!percent || min == max) {
			double pad = (max - min) * FIT_MARGIN;
			if (pad == 0.0)
				pad = Math.max(1e-6, Math.abs(min) * FIT_MARGIN);
			min = (min >= 0.0) ? Math.max(0.0, min - pad) : min - pad;
			max += pad;
		}
		if (log) {
			if (max <= 0.0)
				return null;
			if (min <= 0.0)
				min = max * MIN_YMAX_FRACTION;
		}
		double newMax = Functions.roundUp(max);
		double newMin;
		if (log) {
			newMin = Functions.roundDown(min);
			if (newMin <= 0.0)
				newMin = min;
		} else if (min > 0.0) {
			newMin = roundLinearMin(min, newMax);
		} else {
			newMin = Functions.roundDown(min);
		}
		if (percent) {
			newMin = Math.max(0.0, newMin);
			newMax = Math.min(1.0, newMax);
			if (newMin >= newMax) {
				newMin = 0.0;
				newMax = 1.0;
			}
		}
		return new double[] { newMin, newMax };
	}

	/**
	 * Round a candidate linear axis minimum down, snapping it to zero if it is an
	 * insignificant fraction of the overall range. If {@code max} is not a valid
	 * maximum (below {@code min}), {@code min} is rounded down but never snapped.
	 *
	 * @param min the candidate minimum (must be {@code > 0})
	 * @param max the axis maximum
	 * @return the rounded minimum, or {@code 0.0} if snapped
	 */
	static double roundLinearMin(double min, double max) {
		if (max >= min && min < MIN_YMAX_FRACTION * max)
			return 0.0;
		return Functions.roundDown(min);
	}

	/**
	 * The flag to indicate whether to auto-scale the horizontal axis.
	 */
	public boolean autoscaleX = true;

	/**
	 * The flag to indicate whether to auto-scale the vertical axis.
	 */
	public boolean autoscaleY = true;

	/**
	 * The flag to indicate whether tick marks along the horizontal axis are in
	 * percent.
	 */
	public boolean percentX = false;

	/**
	 * The flag to indicate whether tick marks along the vertical axis are in
	 * percent.
	 */
	public boolean percentY = false;

	/**
	 * The flag to indicate whether to use logarithmic scaling on the y-axis.
	 */
	public boolean logScaleY = false;

	/**
	 * The label of the graph (if any).
	 */
	public String label;

	/**
	 * The {@code x}-axis label of the graph (if any).
	 */
	public String xLabel;

	/**
	 * The {@code y}-axis label of the graph (if any).
	 */
	public String yLabel;

	/**
	 * The flag to indicate whether to show the graph label.
	 */
	public boolean showLabel = true;

	/**
	 * The flag to indicate whether to show the {@code x}-axis label.
	 */
	public boolean showXLabel = true;

	/**
	 * The flag to indicate whether to show the {@code y}-axis label.
	 */
	public boolean showYLabel = true;

	/**
	 * The flag to indicate whether to show tick labels along the horizontal axis.
	 */
	public boolean showXTickLabels = true;

	/**
	 * The flag to indicate whether x-axis tick labels are offset (e.g. to show
	 * absolute time).
	 */
	public boolean offsetXTickLabels = false;

	/**
	 * The offset added to x-axis tick labels when {@link #offsetXTickLabels} is
	 * enabled.
	 */
	public double xTickOffset = 0.0;

	/**
	 * The flag to indicate whether y-axis tick labels are offset (e.g. to show
	 * absolute time).
	 */
	public boolean offsetYTickLabels = false;

	/**
	 * The offset added to y-axis tick labels when {@link #offsetYTickLabels} is
	 * enabled.
	 */
	public double yTickOffset = 0.0;

	/**
	 * The flag to indicate whether to show tick labels along the vertical axis.
	 */
	public boolean showYTickLabels = true;

	/**
	 * The flag to indicate whether y-axis decorations are drawn on the right.
	 */
	public boolean showYAxisRight = true;

	/**
	 * Position of the fitness legend.
	 */
	public Position legendPos = Position.EAST;

	/**
	 * The flag to indicate whether to show the frame of the graph.
	 */
	public boolean showFrame = true;

	/**
	 * The flag to indicate whether to show decorations of the frame (ticks and
	 * labels).
	 * <p>
	 * <strong>Note:</strong> somewhat hackish... used to force showing axes for 2D
	 * histograms (as opposed to plain frame for e.g. lattices)
	 */
	public boolean showDecoratedFrame = false;

	/**
	 * The flag to indicate whether vertical levels are shown.
	 */
	public boolean showXLevels = true;

	/**
	 * The flag to indicate whether horizontal levels are shown.
	 */
	public boolean showYLevels = true;

	/**
	 * The flag to indicate whether tick labels along the horizontal axis are shown.
	 */
	public boolean showXTicks = true;

	/**
	 * The flag to indicate whether tick labels along the vertical axis are shown.
	 */
	public boolean showYTicks = true;

	/**
	 * The array with {@code x}-values to draw custom vertical levels.
	 */
	public double[] customXLevels = new double[0];

	/**
	 * The array with {@code y}-values to draw custom horizontal levels.
	 */
	public double[] customYLevels = new double[0];

	/**
	 * The stroke width of the frame.
	 */
	public double frameWidth = 1.0;

	/**
	 * The stroke width of the levels.
	 */
	public double levelWidth = 0.8;

	/**
	 * The stroke width of lines on the graph.
	 */
	public double lineWidth = 1.4;

	/**
	 * The dashing pattern for a dashed line.
	 */
	public int[] solidLine = new int[0];

	/**
	 * The dashing pattern for a dashed line.
	 */
	public int[] dashedLine = new int[] { 10, 15 };

	/**
	 * The dashing pattern for a dotted line.
	 */
	public int[] dottedLine = new int[] { 2, 5 };

	/**
	 * The stroke width of links on the graph.
	 */
	public double linkWidth = 0.02;

	/**
	 * The color of the frame.
	 */
	public String frameColor = "#000";

	/**
	 * The color of the levels.
	 */
	public String levelColor = "#bbb";

	/**
	 * The color of the custom levels.
	 */
	public String customLevelColor = "#b00";

	/**
	 * The backgorund color of the graph.
	 */
	public String bgColor = "#fff";

	/**
	 * The color of the label of the graph.
	 */
	public String labelColor = "#000";

	/**
	 * The color for drawing the graph.
	 */
	public String graphColor = "#000";

	/**
	 * The color of links on the graph.
	 */
	public String linkColor = "#000";

	/**
	 * The color of trajectories.
	 * 
	 * @see org.evoludo.simulator.CLOController#cloTrajectoryColor
	 */
	public String trajColor = "#000";

	/**
	 * The font for the graph label as a CSS string.
	 */
	public String labelFont = "bold 14px sans-serif";

	/**
	 * The font for the axes labels as a CSS string.
	 */
	public String axesLabelFont = "14px sans-serif";

	/**
	 * The font for the axes tick labels as a CSS string.
	 */
	public String ticksLabelFont = "11px sans-serif";
}
