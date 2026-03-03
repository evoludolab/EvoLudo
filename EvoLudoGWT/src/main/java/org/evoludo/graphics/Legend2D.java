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

import org.evoludo.geom.Rectangle2D;
import org.evoludo.simulator.ColorMap;
import org.evoludo.simulator.ColorMapCSS;
import org.evoludo.simulator.views.BasicTooltipProvider;
import org.evoludo.util.Formatter;

/**
 * Helper responsible for legend sizing, drawing and tooltips for
 * {@link PopGraph2D}.
 */
public class Legend2D {

	/**
	 * The supported semantic legend modes.
	 */
	public enum Mode {
		/**
		 * No legend is available for the current graph.
		 */
		NONE,

		/**
		 * A continuous gradient legend for fitness values.
		 */
		FITNESS_GRADIENT,

		/**
		 * A continuous gradient legend for density or frequency values, as used by
		 * distribution views.
		 */
		DENSITY_GRADIENT,

		/**
		 * A continuous gradient legend for a single continuous trait.
		 */
		CONTINUOUS_TRAIT,

		/**
		 * A segmented legend for indexed discrete trait values.
		 */
		DISCRETE_TRAIT
	}

	/**
	 * Semantic legend data prepared by views and consumed by legends for rendering,
	 * sizing and hit-testing.
	 * <p>
	 * A {@code LegendSpecs} instance contains the non-geometric meaning of a
	 * legend: its mode, numeric range, marker annotations and optional discrete
	 * entry labels. Views compile this information from model and module state and
	 * push it into legends. The legend then combines it with local layout state
	 * such as bounds, zoom, padding and the active color map.
	 * <p>
	 * For gradient legends, {@link #min}, {@link #max}, {@link #markers} and
	 * {@link #inPercent} are relevant. Endpoint labels and gradient tooltip labels
	 * are derived by the legend from the numeric range, formatting flag and
	 * {@link #mode}. For discrete legends, {@link #labels} are used. The
	 * {@link Mode#NONE} singleton returned by {@link #none()} disables legend
	 * rendering.
	 */
	public static final class LegendSpecs {

		/**
		 * Shared empty marker array for legends without annotations.
		 */
		private static final double[] NO_MARKERS = new double[0];

		/**
		 * Shared empty label array for legends without discrete entries.
		 */
		private static final String[] NO_LABELS = new String[0];

		/**
		 * Singleton legend specs representing the absence of a legend.
		 */
		private static final LegendSpecs NONE = new LegendSpecs(Mode.NONE, Double.NaN, Double.NaN, NO_MARKERS, false,
				NO_LABELS);

		/**
		 * The legend mode.
		 */
		public final Legend2D.Mode mode;

		/**
		 * The minimum value for gradient legends.
		 */
		public final double min;

		/**
		 * The maximum value for gradient legends.
		 */
		public final double max;

		/**
		 * Marker values to annotate on gradient legends.
		 */
		public final double[] markers;

		/**
		 * Flag indicating that gradient values should be formatted as percentages.
		 */
		public final boolean inPercent;

		/**
		 * Labels for discrete legend entries.
		 */
		public final String[] labels;

		/**
		 * Create semantic legend data.
		 *
		 * @param mode      the legend mode
		 * @param min       minimum gradient value
		 * @param max       maximum gradient value
		 * @param markers   marker annotations for gradient legends
		 * @param inPercent {@code true} if gradient values should be formatted as
		 *                  percentages
		 * @param labels    labels for discrete legend entries
		 */
		private LegendSpecs(Legend2D.Mode mode, double min, double max, double[] markers, boolean inPercent,
				String[] labels) {
			this.mode = mode;
			this.min = min;
			this.max = max;
			this.markers = markers == null ? NO_MARKERS : markers;
			this.inPercent = inPercent;
			this.labels = labels == null ? NO_LABELS : labels;
		}

		/**
		 * Get the empty legend specs.
		 *
		 * @return empty legend specs
		 */
		public static LegendSpecs none() {
			return NONE;
		}

		/**
		 * Create discrete legend specs.
		 *
		 * @param labels labels for discrete entries
		 * @return legend specs
		 */
		public static LegendSpecs discreteTrait(String[] labels) {
			return new LegendSpecs(Mode.DISCRETE_TRAIT, Double.NaN, Double.NaN, null, false, labels);
		}

		/**
		 * Create continuous-trait legend specs.
		 *
		 * @return legend specs
		 */
		public static LegendSpecs continuousTrait() {
			return new LegendSpecs(Mode.CONTINUOUS_TRAIT, 0.0, 1.0, NO_MARKERS, false, null);
		}

		/**
		 * Create fitness-gradient legend specs.
		 *
		 * @param min     minimum fitness value
		 * @param max     maximum fitness value
		 * @param markers marker values
		 * @return legend specs
		 */
		public static LegendSpecs fitnessGradient(double min, double max, double[] markers) {
			return new LegendSpecs(Mode.FITNESS_GRADIENT, min, max, markers, false, null);
		}

		/**
		 * Create density-gradient legend specs.
		 *
		 * @param max maximum density or frequency value
		 * @return legend specs
		 */
		public static LegendSpecs densityGradient(double max) {
			return new LegendSpecs(Mode.DENSITY_GRADIENT, 0.0, max, NO_MARKERS, true, null);
		}
	}

	/**
	 * Width of the legend color bar in pixels.
	 */
	private static final double BAR_WIDTH = 10.0;

	/**
	 * Gap between graph and legend in pixels.
	 */
	private static final double GAP = 12.0;

	/**
	 * Gap between legend labels and color bar in pixels.
	 */
	private static final double LABEL_PAD = 4.0;

	/**
	 * Minimum height of one legend color band in pixels.
	 */
	private static final double MIN_BAND_HEIGHT = 2.0;

	/**
	 * The owning population graph.
	 */
	private final PopGraph2D owner;

	/**
	 * Semantic legend data supplied by the view.
	 */
	private LegendSpecs legendSpecs = LegendSpecs.none();

	/**
	 * The bounds of the visible legend content, including labels and other legend
	 * decorations, but excluding the outer reserve gap.
	 */
	private final Rectangle2D legendBounds = new Rectangle2D();

	/**
	 * Width removed from the graph content area by a vertical legend.
	 */
	private double reserveWidth = 0.0;

	/**
	 * Height removed from the graph content area by a horizontal legend.
	 */
	private double reserveHeight = 0.0;

	/**
	 * Width budget for legend labels.
	 */
	private double labelWidth = 0.0;

	/**
	 * Current legend bar bounds.
	 */
	private final Rectangle2D barBounds = new Rectangle2D();

	/**
	 * Create a legend helper tied to the given 2D population graph.
	 *
	 * @param owner the graph that provides style, bounds, canvas and color map
	 */
	Legend2D(PopGraph2D owner) {
		this.owner = owner;
	}

	/**
	 * Assign semantic legend data to this legend.
	 *
	 * @param legendSpecs legend data prepared by the view
	 */
	void setLegendSpecs(LegendSpecs legendSpecs) {
		this.legendSpecs = (legendSpecs == null ? LegendSpecs.none() : legendSpecs);
	}

	/**
	 * Get the current legend mode.
	 *
	 * @return legend mode
	 */
	Legend2D.Mode getMode() {
		return legendSpecs.mode;
	}

	/**
	 * Check whether this legend should be shown.
	 *
	 * @return {@code true} if legend content and a legend position are available
	 */
	boolean hasLegend() {
		return owner.style.legendPos != GraphStyle.Position.NONE && legendSpecs.mode != Legend2D.Mode.NONE;
	}

	/**
	 * Recompute the legend reserves from the parent graph area and shrink the given
	 * bounds to the remaining plot area.
	 *
	 * @param graphBounds the graph area before graph-content layout is specialized;
	 *                    updated in place to the remaining plot area
	 */
	void reserve(Rectangle2D graphBounds) {
		reserveWidth = 0.0;
		reserveHeight = 0.0;
		labelWidth = 0.0;
		if (!hasLegend() || owner.g == null)
			return;
		String font = owner.g.getFont();
		owner.setFont(owner.style.ticksLabelFont);
		switch (legendSpecs.mode) {
			case FITNESS_GRADIENT:
				if (!Double.isFinite(legendSpecs.min) || !Double.isFinite(legendSpecs.max)) {
					owner.g.setFont(font);
					return;
				}
				labelWidth = Math.max(owner.g.measureText(formatGradientLegendValue(legendSpecs.max)).getWidth(),
						owner.g.measureText(formatGradientLegendValue(legendSpecs.min)).getWidth());
				for (double marker : legendSpecs.markers)
					labelWidth = Math.max(labelWidth,
							owner.g.measureText(Formatter.formatPretty(marker, 2)).getWidth());
				break;
			case DENSITY_GRADIENT:
				if (!Double.isFinite(legendSpecs.min) || !Double.isFinite(legendSpecs.max)) {
					owner.g.setFont(font);
					return;
				}
				labelWidth = owner.g.measureText(Formatter.formatPercent(0.999, 1)).getWidth();
				break;
			case CONTINUOUS_TRAIT:
				labelWidth = Math.max(owner.g.measureText(formatGradientLegendValue(legendSpecs.min)).getWidth(),
						owner.g.measureText(formatGradientLegendValue(legendSpecs.max)).getWidth());
				break;
			case DISCRETE_TRAIT:
				for (String label : legendSpecs.labels)
					labelWidth = Math.max(labelWidth, owner.g.measureText(label).getWidth());
				break;
			case NONE:
			default:
				break;
		}
		owner.g.setFont(font);
		switch (owner.style.legendPos) {
			case EAST:
			case WEST:
				reserveWidth = labelWidth + BAR_WIDTH + LABEL_PAD + GAP;
				break;
			case SOUTH:
			case NORTH:
				reserveHeight = GAP + LABEL_PAD
						+ (legendSpecs.mode == Legend2D.Mode.DISCRETE_TRAIT ? Math.max(BAR_WIDTH, 16.0)
								: BAR_WIDTH + 14.0);
				break;
			case NONE:
			default:
				return;
		}
		graphBounds.setSize(graphBounds.getWidth() - reserveWidth, graphBounds.getHeight() - reserveHeight);
	}

	void setBounds(int width, int height, Rectangle2D graphBounds) {
		if (!hasLegend())
			return;

		switch (owner.style.legendPos) {
			case EAST:
				double h = graphBounds.getHeight();
				legendBounds.setOrigin(width - reserveWidth, graphBounds.getY() + h * 0.2);
				barBounds.set(legendBounds.getX(), legendBounds.getY(), BAR_WIDTH, h * 0.6);
				break;
			case WEST:
				h = graphBounds.getHeight();
				legendBounds.setOrigin(owner.style.minPadding, graphBounds.getY() + h * 0.2);
				barBounds.set(legendBounds.getX() + labelWidth + getLegendLabelPad(owner.style.legendPos),
						legendBounds.getY(), BAR_WIDTH, h * 0.6);
				graphBounds.shift(reserveWidth, 0.0);
				break;
			case SOUTH:
				double barWidth = (legendSpecs.mode == Legend2D.Mode.DISCRETE_TRAIT
						? getHorizontalDiscreteTraitLegendBarWidth(legendSpecs.labels, graphBounds.getWidth())
						: graphBounds.getWidth() * 0.6);
				double barHeight = (legendSpecs.mode == Legend2D.Mode.DISCRETE_TRAIT ? Math.max(BAR_WIDTH, 16.0)
						: BAR_WIDTH);
				legendBounds.setOrigin(graphBounds.getX() + 0.5 * (graphBounds.getWidth() - barWidth),
						height - reserveHeight);
				barBounds.set(legendBounds.getX(), legendBounds.getY(), barWidth, barHeight);
				break;
			case NORTH:
				barWidth = (legendSpecs.mode == Legend2D.Mode.DISCRETE_TRAIT
						? getHorizontalDiscreteTraitLegendBarWidth(legendSpecs.labels, graphBounds.getWidth())
						: graphBounds.getWidth() * 0.6);
				barHeight = (legendSpecs.mode == Legend2D.Mode.DISCRETE_TRAIT ? Math.max(BAR_WIDTH, 16.0) : BAR_WIDTH);
				legendBounds.setOrigin(graphBounds.getX() + 0.5 * (graphBounds.getWidth() - barWidth),
						owner.style.minPadding);
				barBounds.set(legendBounds.getX(), legendBounds.getY(), barWidth, barHeight);
				graphBounds.shift(0.0, reserveHeight);
				break;
			case NONE:
			default:
				return;
		}
	}

	/**
	 * Draw the legend into the owner's canvas.
	 */
	void draw() {
		if (!hasLegend() || owner.hasMessage())
			return;
		owner.g.save();
		owner.g.scale(owner.scale, owner.scale);
		drawLegend();
		owner.g.restore();
	}

	/**
	 * Get the legend tooltip at graph-relative screen coordinates.
	 *
	 * @param x the x-coordinate in graph widget coordinates
	 * @param y the y-coordinate in graph widget coordinates
	 * @return tooltip HTML or {@code null}
	 */
	String getTooltipAt(int x, int y) {
		if (!hasLegend() || owner.contextMenu.isVisible())
			return null;
		owner.element.removeClassName(GenericPopGraph.EVOLUDO_CURSOR_NODE);
		switch (legendSpecs.mode) {
			case FITNESS_GRADIENT:
			case DENSITY_GRADIENT:
				return getGradientLegendTooltipAt(x, y);
			case CONTINUOUS_TRAIT:
			case DISCRETE_TRAIT:
				return getTraitLegendTooltipAt(x, y);
			case NONE:
			default:
				return null;
		}
	}

	/**
	 * Draw the active legend variant selected by the current specs.
	 */
	private void drawLegend() {
		switch (legendSpecs.mode) {
			case FITNESS_GRADIENT:
			case DENSITY_GRADIENT:
			case CONTINUOUS_TRAIT:
				drawGradientLegend();
				return;
			case DISCRETE_TRAIT:
				drawDiscreteTraitLegend();
				return;
			case NONE:
			default:
				return;
		}
	}

	/**
	 * Draw the current gradient-style legend, including endpoint labels and marker
	 * annotations.
	 */
	private void drawGradientLegend() {
		if (!(owner.getColorMap() instanceof ColorMap.Gradient1D))
			return;
		if (!Double.isFinite(legendSpecs.min) || !Double.isFinite(legendSpecs.max))
			return;
		double barWidth = barBounds.getWidth();
		double barHeight = barBounds.getHeight();
		if (barWidth <= 0.0 || barHeight <= 0.0)
			return;
		boolean vertical = owner.style.legendPos.isVertical();
		double[] samples = getLegendSamples(legendSpecs.min, legendSpecs.max, vertical ? barHeight : barWidth,
				legendSpecs.markers);
		drawGradientLegendBands(samples, (ColorMap.Gradient1D<String>) owner.getColorMap());
		drawLegendFrame();
		owner.g.setFillStyle(owner.style.frameColor);
		owner.setFont(owner.style.ticksLabelFont);
		String minLabel = formatGradientLegendValue(legendSpecs.min);
		String maxLabel = formatGradientLegendValue(legendSpecs.max);
		double barX = barBounds.getX();
		double barY = barBounds.getY();
		if (vertical) {
			owner.g.fillText(maxLabel, getLegendLabelX(maxLabel), barY + 4.5);
			owner.g.fillText(minLabel, getLegendLabelX(minLabel), barY + barHeight + 4.5);
			drawLegendMarkers(legendSpecs.markers, legendSpecs.min, legendSpecs.max);
			return;
		}
		double labelY = getHorizontalLegendLabelY(BAR_WIDTH + 12.5, -owner.style.minPadding);
		owner.g.fillText(minLabel, barX, labelY);
		owner.g.fillText(maxLabel, barX + barWidth - owner.g.measureText(maxLabel).getWidth(), labelY);
	}

	/**
	 * Fill the current legend bar with sampled gradient colors.
	 *
	 * @param samples  sampled values along the legend axis
	 * @param gradient gradient used to translate values into CSS colors
	 */
	private void drawGradientLegendBands(double[] samples, ColorMap.Gradient1D<String> gradient) {
		boolean vertical = owner.style.legendPos.isVertical();
		int steps = samples.length;
		double barX = barBounds.getX();
		double barY = barBounds.getY();
		double barWidth = barBounds.getWidth();
		double barHeight = barBounds.getHeight();
		double stepSpan = (vertical ? barHeight : barWidth) / steps;
		for (int i = 0; i < steps; i++) {
			double start = Math.floor((vertical ? barY : barX) + i * stepSpan);
			double end = Math.floor((vertical ? barY : barX) + (i + 1) * stepSpan);
			if (i == steps - 1)
				end = vertical ? barY + barHeight : barX + barWidth;
			if (end <= start)
				end = start + 1.0;
			owner.g.setFillStyle(gradient.translate(samples[i]));
			if (vertical) {
				owner.fillRect(barX, start, barWidth, end - start);
				continue;
			}
			owner.fillRect(start, barY, end - start, barHeight);
		}
	}

	/**
	 * Stroke the outline around the current legend bar.
	 */
	private void drawLegendFrame() {
		owner.g.setStrokeStyle(owner.style.frameColor);
		owner.g.setLineWidth(1.0);
		double barX = barBounds.getX();
		double barY = barBounds.getY();
		double barWidth = barBounds.getWidth();
		double barHeight = barBounds.getHeight();
		if (owner.style.legendPos.isVertical()) {
			owner.g.strokeRect(barX + 0.5, barY - 0.5, barWidth, barHeight + 1.0);
			return;
		}
		owner.g.strokeRect(barX - 0.5, barY + 0.5, barWidth + 1.0, barHeight);
	}

	/**
	 * Draw the segmented discrete-trait legend.
	 */
	private void drawDiscreteTraitLegend() {
		String[] colors = getDiscreteTraitLegendColors();
		String[] labels = legendSpecs.labels;
		int nSegments = Math.min(colors.length, labels.length);
		if (nSegments <= 0)
			return;
		double barX = barBounds.getX();
		double barY = barBounds.getY();
		double barWidth = barBounds.getWidth();
		double barHeight = barBounds.getHeight();
		owner.g.setStrokeStyle(owner.style.frameColor);
		owner.g.setLineWidth(1.0);
		owner.g.setFillStyle(owner.style.frameColor);
		owner.setFont(owner.style.ticksLabelFont);
		if (owner.style.legendPos.isVertical()) {
			if (barWidth <= 0.0 || barHeight <= 0.0)
				return;
			double stepHeight = barHeight / nSegments;
			for (int i = 0; i < nSegments; i++) {
				double y0 = Math.floor(barY + i * stepHeight);
				double y1 = Math.floor(barY + (i + 1) * stepHeight);
				if (i == nSegments - 1)
					y1 = barY + barHeight;
				if (y1 <= y0)
					y1 = y0 + 1.0;
				owner.g.setFillStyle(colors[i]);
				owner.fillRect(barX, y0, barWidth, y1 - y0);
				owner.g.setFillStyle(owner.style.frameColor);
				owner.g.fillText(labels[i], getLegendLabelX(labels[i]), 0.5 * (y0 + y1) + 4.5);
			}
			drawLegendFrame();
			return;
		}
		if (barWidth <= 0.0 || barHeight <= 0.0)
			return;
		double stepWidth = barWidth / nSegments;
		for (int i = 0; i < nSegments; i++) {
			double x0 = Math.floor(barX + i * stepWidth);
			double x1 = Math.floor(barX + (i + 1) * stepWidth);
			if (i == nSegments - 1)
				x1 = barX + barWidth;
			if (x1 <= x0)
				x1 = x0 + 1.0;
			owner.g.setFillStyle(colors[i]);
			owner.fillRect(x0, barY, x1 - x0, barHeight);
			owner.g.setFillStyle(ColorMapCSS.luminance(colors[i]) > 0.55 ? "#000000" : "#ffffff");
			double textWidth = owner.g.measureText(labels[i]).getWidth();
			owner.g.fillText(labels[i], x0 + 0.5 * (x1 - x0 - textWidth), barY + 0.5 * (barHeight + 9.0));
		}
		drawLegendFrame();
	}

	/**
	 * Build the tooltip text for gradient legends at the given graph coordinates.
	 *
	 * @param x the x-coordinate in graph widget coordinates
	 * @param y the y-coordinate in graph widget coordinates
	 * @return tooltip HTML or {@code null} if the point is outside the legend
	 */
	private String getGradientLegendTooltipAt(int x, int y) {
		if (!(owner.getColorMap() instanceof ColorMap.Gradient1D))
			return null;
		double value = getGradientLegendValueAt(x, y, legendSpecs.min, legendSpecs.max);
		if (!Double.isFinite(value))
			return null;
		String color = ((ColorMap.Gradient1D<String>) owner.getColorMap()).translate(value);
		String label;
		switch (legendSpecs.mode) {
			case FITNESS_GRADIENT:
				label = "Fitness";
				break;
			case DENSITY_GRADIENT:
				label = "Frequency";
				break;
			default:
				label = "Value";
				break;
		}
		return label + ": " + BasicTooltipProvider.SPAN_COLOR + color
				+ BasicTooltipProvider.TABLE_CELL_BULLET + formatGradientLegendValue(value);
	}

	/**
	 * Build the tooltip text for trait legends at the given graph coordinates.
	 *
	 * @param x the x-coordinate in graph widget coordinates
	 * @param y the y-coordinate in graph widget coordinates
	 * @return tooltip HTML or {@code null} if the point is outside the legend
	 */
	private String getTraitLegendTooltipAt(int x, int y) {
		switch (legendSpecs.mode) {
			case CONTINUOUS_TRAIT:
				if (!(owner.getColorMap() instanceof ColorMap.Gradient1D))
					return null;
				double value = getGradientLegendValueAt(x, y, legendSpecs.min, legendSpecs.max);
				if (!Double.isFinite(value))
					return null;
				String color = ((ColorMap.Gradient1D<String>) owner.getColorMap()).translate(value);
				return "Trait: " + BasicTooltipProvider.SPAN_COLOR + color
						+ BasicTooltipProvider.TABLE_CELL_BULLET + Formatter.formatPretty(value, 2);
			case DISCRETE_TRAIT:
				String[] colors = getDiscreteTraitLegendColors();
				String[] labels = legendSpecs.labels;
				int nSegments = Math.min(colors.length, labels.length);
				if (nSegments == 0)
					return null;
				int idx = (barBounds.getWidth() > 0.0 && barBounds.getHeight() > 0.0)
						? getLegendBandIndexAt(x, y, nSegments)
						: -1;
				if (idx < 0)
					return null;
				return "Trait: " + BasicTooltipProvider.SPAN_COLOR + colors[idx]
						+ BasicTooltipProvider.TABLE_CELL_BULLET + labels[idx];
			default:
				return null;
		}
	}

	/**
	 * Map a position on the current gradient legend to the corresponding data
	 * value.
	 *
	 * @param x   the x-coordinate in graph widget coordinates
	 * @param y   the y-coordinate in graph widget coordinates
	 * @param min the minimum legend value
	 * @param max the maximum legend value
	 * @return legend value, or {@link Double#NaN} if the point is outside
	 */
	private double getGradientLegendValueAt(int x, int y, double min, double max) {
		double barWidth = barBounds.getWidth();
		double barHeight = barBounds.getHeight();
		if (barWidth <= 0.0 || barHeight <= 0.0)
			return Double.NaN;
		double fraction = getLegendAxisFractionAt(x, y);
		if (!Double.isFinite(fraction))
			return Double.NaN;
		if (owner.style.legendPos.isVertical())
			return max - fraction * (max - min);
		return min + fraction * (max - min);
	}

	/**
	 * Convert graph coordinates to a normalized position along the current legend
	 * bar.
	 *
	 * @param x the x-coordinate in graph widget coordinates
	 * @param y the y-coordinate in graph widget coordinates
	 * @return normalized position in {@code [0,1]}, or {@link Double#NaN} if
	 *         outside the legend bar
	 */
	private double getLegendAxisFractionAt(int x, int y) {
		double barX = barBounds.getX();
		double barY = barBounds.getY();
		double barWidth = barBounds.getWidth();
		double barHeight = barBounds.getHeight();
		if (x < barX || x > barX + barWidth || y < barY || y > barY + barHeight)
			return Double.NaN;
		if (owner.style.legendPos.isVertical())
			return Math.max(0.0, Math.min(1.0, (y - barY) / barHeight));
		return Math.max(0.0, Math.min(1.0, (x - barX) / barWidth));
	}

	/**
	 * Map graph coordinates to the corresponding discrete legend band.
	 *
	 * @param x      the x-coordinate in graph widget coordinates
	 * @param y      the y-coordinate in graph widget coordinates
	 * @param nBands number of discrete legend bands
	 * @return band index, or {@code -1} if outside the legend bar
	 */
	private int getLegendBandIndexAt(int x, int y, int nBands) {
		double fraction = getLegendAxisFractionAt(x, y);
		if (!Double.isFinite(fraction))
			return -1;
		return Math.min(nBands - 1, Math.max(0, (int) (fraction * nBands)));
	}

	/**
	 * Sample values along a gradient legend, preserving marker values when
	 * possible.
	 *
	 * @param min     the minimum legend value
	 * @param max     the maximum legend value
	 * @param span    the pixel span of the legend axis
	 * @param markers marker values that should be represented exactly
	 * @return sampled values from top-to-bottom or left-to-right
	 */
	private double[] getLegendSamples(double min, double max, double span, double[] markers) {
		int steps = Math.max(2, (int) Math.floor(span / MIN_BAND_HEIGHT));
		double[] samples = new double[steps];
		boolean vertical = owner.style.legendPos.isVertical();
		for (int i = 0; i < steps; i++) {
			double frac = (double) i / Math.max(1, steps - 1);
			samples[i] = vertical ? min + (1.0 - frac) * (max - min) : min + frac * (max - min);
		}
		double range = max - min;
		if (range <= 0.0 || markers.length == 0)
			return samples;
		boolean[] used = new boolean[steps];
		for (double marker : markers) {
			int target = (int) Math.rint((vertical ? (max - marker) : (marker - min)) / range * (steps - 1));
			target = Math.max(0, Math.min(steps - 1, target));
			for (int delta = 0; delta < steps; delta++) {
				int lower = target - delta;
				if (lower >= 0 && !used[lower]) {
					samples[lower] = marker;
					used[lower] = true;
					break;
				}
				int upper = target + delta;
				if (delta > 0 && upper < steps && !used[upper]) {
					samples[upper] = marker;
					used[upper] = true;
					break;
				}
			}
		}
		return samples;
	}

	/**
	 * Draw marker labels alongside a vertical gradient legend.
	 *
	 * @param markers marker values to annotate
	 * @param min     the minimum legend value
	 * @param max     the maximum legend value
	 */
	private void drawLegendMarkers(double[] markers, double min, double max) {
		if (markers.length == 0)
			return;
		double barY = barBounds.getY();
		double barHeight = barBounds.getHeight();
		double range = max - min;
		for (double marker : markers) {
			double frac = (range <= 0.0 ? 0.5 : (max - marker) / range);
			double y = Math.max(barY, Math.min(barY + barHeight, barY + frac * barHeight));
			owner.g.setFillStyle(owner.style.frameColor);
			String markerLabel = Formatter.formatPretty(marker, 2);
			owner.g.fillText(markerLabel, getLegendLabelX(markerLabel), y + 4.5);
		}
	}

	/**
	 * Compute the x-coordinate used to draw a legend label next to the current bar.
	 *
	 * @param label the label text to position
	 * @return x-coordinate for the label baseline
	 */
	private double getLegendLabelX(String label) {
		double barWidth = barBounds.getWidth();
		double labelX = (owner.style.legendPos == GraphStyle.Position.EAST
				? legendBounds.getX() + barWidth + getLegendLabelPad(owner.style.legendPos)
				: legendBounds.getX());
		if (owner.style.legendPos != GraphStyle.Position.WEST)
			return labelX;
		return labelX + labelWidth - owner.g.measureText(label).getWidth();
	}

	/**
	 * Compute the y-coordinate used for horizontal legend endpoint labels.
	 *
	 * @param southLabelGap offset below the bar for south legends
	 * @param northLabelY   label baseline for north legends without extra padding
	 * @return y-coordinate for the endpoint labels
	 */
	private double getHorizontalLegendLabelY(double southLabelGap, double northLabelY) {
		double barY = barBounds.getY();
		double barHeight = barBounds.getHeight();
		if (owner.style.legendPos == GraphStyle.Position.SOUTH)
			return barY + southLabelGap;
		switch (legendSpecs.mode) {
			case FITNESS_GRADIENT:
			case DENSITY_GRADIENT:
				return barY + barHeight + 12.5;
			default:
				return northLabelY;
		}
	}

	/**
	 * Get the padding between the legend bar and its labels for the given side.
	 *
	 * @param pos the legend position
	 * @return label padding in pixels
	 */
	private double getLegendLabelPad(GraphStyle.Position pos) {
		return (pos == GraphStyle.Position.WEST && !owner.style.showYAxisRight ? 2.0 * LABEL_PAD : LABEL_PAD);
	}

	/**
	 * Compute the width of a horizontal discrete-trait legend bar.
	 *
	 * @param labels     discrete legend labels
	 * @param graphWidth width of the actual graph-content bounds
	 * @return legend bar width in pixels
	 */
	private double getHorizontalDiscreteTraitLegendBarWidth(String[] labels, double graphWidth) {
		int nSegments = labels.length;
		if (nSegments <= 0)
			return graphWidth * 0.6;
		double minWidth = graphWidth * 0.6;
		double targetWidth = nSegments * (labelWidth + 2.0 * LABEL_PAD);
		return Math.min(graphWidth, Math.max(minWidth, targetWidth));
	}

	/**
	 * Get the indexed colors used by the current discrete-trait legend.
	 *
	 * @return discrete legend colors, or an empty array if unavailable
	 */
	private String[] getDiscreteTraitLegendColors() {
		if (legendSpecs.mode != Legend2D.Mode.DISCRETE_TRAIT || !(owner.getColorMap() instanceof ColorMap.Index))
			return new String[0];
		return ((ColorMap.Index<String>) owner.getColorMap()).getColors();
	}

	/**
	 * Format a numeric gradient value for legend labels and tooltips.
	 *
	 * @param value the value to format
	 * @return formatted legend text
	 */
	private String formatGradientLegendValue(double value) {
		return legendSpecs.inPercent ? Formatter.formatPercent(value, 1) : Formatter.formatPretty(value, 2);
	}
}
