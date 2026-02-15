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

/**
 * The style features for graphs. This is a collection of settings for line
 * styles, font sizes, ticks, padding, etc.
 */
@SuppressWarnings("java:S1104") // Data class used to store style settings
public class GraphStyle {

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
	public int minPadding = 4;

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
	 * The flag to indicate whether to show tick labels along the vertical axis.
	 */
	public boolean showYTickLabels = true;

	/**
	 * The flag to indicate whether y-axis decorations are drawn on the right.
	 */
	public boolean showYAxisRight = true;

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
