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
	public static final class Specs {

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
		private static final Specs NONE = new Specs(Mode.NONE, Double.NaN, Double.NaN, NO_MARKERS, false,
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
		private Specs(Legend2D.Mode mode, double min, double max, double[] markers, boolean inPercent,
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
		public static Specs none() {
			return NONE;
		}

		/**
		 * Create discrete legend specs.
		 *
		 * @param labels labels for discrete entries
		 * @return legend specs
		 */
		public static Specs discreteTrait(String[] labels) {
			return new Specs(Mode.DISCRETE_TRAIT, Double.NaN, Double.NaN, null, false, labels);
		}

		/**
		 * Create continuous-trait legend specs.
		 *
		 * @return legend specs
		 */
		public static Specs continuousTrait() {
			return new Specs(Mode.CONTINUOUS_TRAIT, 0.0, 1.0, NO_MARKERS, false, null);
		}

		/**
		 * Create fitness-gradient legend specs.
		 *
		 * @param min     minimum fitness value
		 * @param max     maximum fitness value
		 * @param markers marker values
		 * @return legend specs
		 */
		public static Specs fitnessGradient(double min, double max, double[] markers) {
			return new Specs(Mode.FITNESS_GRADIENT, min, max, markers, false, null);
		}

		/**
		 * Create density-gradient legend specs.
		 *
		 * @param max maximum density or frequency value
		 * @return legend specs
		 */
		public static Specs densityGradient(double max) {
			return new Specs(Mode.DENSITY_GRADIENT, 0.0, max, NO_MARKERS, true, null);
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
	private Specs specs = Specs.none();

	/**
	 * The bounds of the visible legend content, including labels and other legend
	 * decorations, but excluding the outer reserve gap.
	 */
	private final Rectangle2D bounds = new Rectangle2D();

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
	private final Rectangle2D bar = new Rectangle2D();

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
	void setSpecs(Specs legendSpecs) {
		this.specs = (legendSpecs == null ? Specs.none() : legendSpecs);
	}

	/**
	 * Get the current legend mode.
	 *
	 * @return legend mode
	 */
	Legend2D.Mode getMode() {
		return specs.mode;
	}

	/**
	 * Check whether this legend should be shown.
	 *
	 * @return {@code true} if legend content and a legend position are available
	 */
	boolean hasLegend() {
		return owner.style.legendPos != GraphStyle.Position.NONE && specs.mode != Legend2D.Mode.NONE;
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
		switch (specs.mode) {
			case FITNESS_GRADIENT:
				if (!Double.isFinite(specs.min) || !Double.isFinite(specs.max)) {
					owner.g.setFont(font);
					return;
				}
				labelWidth = Math.max(owner.g.measureText(formatGradientLegend(specs.max)).getWidth(),
						owner.g.measureText(formatGradientLegend(specs.min)).getWidth());
				for (double marker : specs.markers)
					labelWidth = Math.max(labelWidth,
							owner.g.measureText(Formatter.formatPretty(marker, 2)).getWidth());
				break;
			case DENSITY_GRADIENT:
				if (!Double.isFinite(specs.min) || !Double.isFinite(specs.max)) {
					owner.g.setFont(font);
					return;
				}
				labelWidth = owner.g.measureText(Formatter.formatPercent(0.999, 1)).getWidth();
				break;
			case CONTINUOUS_TRAIT:
				labelWidth = Math.max(owner.g.measureText(formatGradientLegend(specs.min)).getWidth(),
						owner.g.measureText(formatGradientLegend(specs.max)).getWidth());
				break;
			case DISCRETE_TRAIT:
				for (String label : specs.labels)
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
						+ (specs.mode == Legend2D.Mode.DISCRETE_TRAIT ? Math.max(BAR_WIDTH, 16.0)
								: BAR_WIDTH + 14.0);
				break;
			case NONE:
			default:
				return;
		}
		graphBounds.setSize(graphBounds.getWidth() - reserveWidth, graphBounds.getHeight() - reserveHeight);
	}

	/**
	 * Position the legend within the available graph area.
	 *
	 * @param width the total graph width
	 * @param height the total graph height
	 * @param graphBounds the drawable graph bounds, adjusted as needed
	 */
	void setBounds(int width, int height, Rectangle2D graphBounds) {
		if (!hasLegend())
			return;

		switch (owner.style.legendPos) {
			case EAST:
				double h = graphBounds.getHeight();
				bounds.setOrigin(width - reserveWidth, graphBounds.getY() + h * 0.2);
				bar.set(bounds.getX(), bounds.getY(), BAR_WIDTH, h * 0.6);
				break;
			case WEST:
				h = graphBounds.getHeight();
				bounds.setOrigin(owner.style.minPadding, graphBounds.getY() + h * 0.2);
				bar.set(bounds.getX() + labelWidth + getLabelPad(owner.style.legendPos),
						bounds.getY(), BAR_WIDTH, h * 0.6);
				graphBounds.shift(reserveWidth, 0.0);
				break;
			case SOUTH:
				double barWidth = (specs.mode == Legend2D.Mode.DISCRETE_TRAIT
						? getHorizontalDiscreteTraitsBarWidth(specs.labels, graphBounds.getWidth())
						: graphBounds.getWidth() * 0.6);
				double barHeight = (specs.mode == Legend2D.Mode.DISCRETE_TRAIT ? Math.max(BAR_WIDTH, 16.0)
						: BAR_WIDTH);
				bounds.setOrigin(graphBounds.getX() + 0.5 * (graphBounds.getWidth() - barWidth),
						height - reserveHeight);
				bar.set(bounds.getX(), bounds.getY(), barWidth, barHeight);
				break;
			case NORTH:
				barWidth = (specs.mode == Legend2D.Mode.DISCRETE_TRAIT
						? getHorizontalDiscreteTraitsBarWidth(specs.labels, graphBounds.getWidth())
						: graphBounds.getWidth() * 0.6);
				barHeight = (specs.mode == Legend2D.Mode.DISCRETE_TRAIT ? Math.max(BAR_WIDTH, 16.0) : BAR_WIDTH);
				bounds.setOrigin(graphBounds.getX() + 0.5 * (graphBounds.getWidth() - barWidth),
						owner.style.minPadding);
				bar.set(bounds.getX(), bounds.getY(), barWidth, barHeight);
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
		switch (specs.mode) {
			case FITNESS_GRADIENT:
			case DENSITY_GRADIENT:
			case CONTINUOUS_TRAIT:
				drawGradient();
				break;
			case DISCRETE_TRAIT:
				drawDiscreteTraits();
				break;
			case NONE:
			default:
		}
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
		switch (specs.mode) {
			case FITNESS_GRADIENT:
			case DENSITY_GRADIENT:
				return getGradientTooltipAt(x, y);
			case CONTINUOUS_TRAIT:
			case DISCRETE_TRAIT:
				return getTraitTooltipAt(x, y);
			case NONE:
			default:
				return null;
		}
	}

	/**
	 * Draw the current gradient-style legend, including endpoint labels and marker
	 * annotations.
	 */
	private void drawGradient() {
		if (!(owner.getColorMap() instanceof ColorMap.Gradient1D))
			return;
		if (!Double.isFinite(specs.min) || !Double.isFinite(specs.max))
			return;
		double barWidth = bar.getWidth();
		double barHeight = bar.getHeight();
		if (barWidth <= 0.0 || barHeight <= 0.0)
			return;
		boolean vertical = owner.style.legendPos.isVertical();
		double[] samples = getBins(specs.min, specs.max, vertical ? barHeight : barWidth,
				specs.markers);
		drawGradientBands(samples, (ColorMap.Gradient1D<String>) owner.getColorMap());
		drawFrame();
		owner.g.setFillStyle(owner.style.frameColor);
		owner.setFont(owner.style.ticksLabelFont);
		String minLabel = formatGradientLegend(specs.min);
		String maxLabel = formatGradientLegend(specs.max);
		double barX = bar.getX();
		double barY = bar.getY();
		if (vertical) {
			owner.g.fillText(maxLabel, getLabelX(maxLabel), barY + 4.5);
			owner.g.fillText(minLabel, getLabelX(minLabel), barY + barHeight + 4.5);
			drawMarkers(specs.markers, specs.min, specs.max);
			return;
		}
		double labelY = getHorizontalLabelY(BAR_WIDTH + 12.5, -owner.style.minPadding);
		owner.g.fillText(minLabel, barX, labelY);
		owner.g.fillText(maxLabel, barX + barWidth - owner.g.measureText(maxLabel).getWidth(), labelY);
	}

	/**
	 * Fill the current legend bar with sampled gradient colors.
	 *
	 * @param samples  sampled values along the legend axis
	 * @param gradient gradient used to translate values into CSS colors
	 */
	private void drawGradientBands(double[] samples, ColorMap.Gradient1D<String> gradient) {
		boolean vertical = owner.style.legendPos.isVertical();
		int steps = samples.length;
		double barX = bar.getX();
		double barY = bar.getY();
		double barWidth = bar.getWidth();
		double barHeight = bar.getHeight();
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
	private void drawFrame() {
		owner.g.setStrokeStyle(owner.style.frameColor);
		owner.g.setLineWidth(1.0);
		double barX = bar.getX();
		double barY = bar.getY();
		double barWidth = bar.getWidth();
		double barHeight = bar.getHeight();
		if (owner.style.legendPos.isVertical()) {
			owner.g.strokeRect(barX + 0.5, barY - 0.5, barWidth, barHeight + 1.0);
			return;
		}
		owner.g.strokeRect(barX - 0.5, barY + 0.5, barWidth + 1.0, barHeight);
	}

	/**
	 * Draw the segmented discrete-trait legend.
	 */
	private void drawDiscreteTraits() {
		String[] colors = getDiscreteTraitsColors();
		String[] labels = specs.labels;
		int nSegments = Math.min(colors.length, labels.length);
		if (nSegments <= 0)
			return;
		double barX = bar.getX();
		double barY = bar.getY();
		double barWidth = bar.getWidth();
		double barHeight = bar.getHeight();
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
				owner.g.fillText(labels[i], getLabelX(labels[i]), 0.5 * (y0 + y1) + 4.5);
			}
			drawFrame();
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
		drawFrame();
	}

	/**
	 * Build the tooltip text for gradient legends at the given graph coordinates.
	 *
	 * @param x the x-coordinate in graph widget coordinates
	 * @param y the y-coordinate in graph widget coordinates
	 * @return tooltip HTML or {@code null} if the point is outside the legend
	 */
	private String getGradientTooltipAt(int x, int y) {
		if (!(owner.getColorMap() instanceof ColorMap.Gradient1D))
			return null;
		double value = getGradientAt(x, y, specs.min, specs.max);
		if (!Double.isFinite(value))
			return null;
		String color = ((ColorMap.Gradient1D<String>) owner.getColorMap()).translate(value);
		String label;
		switch (specs.mode) {
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
				+ BasicTooltipProvider.TABLE_CELL_BULLET + formatGradientTooltip(value);
	}

	/**
	 * Build the tooltip text for trait legends at the given graph coordinates.
	 *
	 * @param x the x-coordinate in graph widget coordinates
	 * @param y the y-coordinate in graph widget coordinates
	 * @return tooltip HTML or {@code null} if the point is outside the legend
	 */
	private String getTraitTooltipAt(int x, int y) {
		switch (specs.mode) {
			case CONTINUOUS_TRAIT:
				if (!(owner.getColorMap() instanceof ColorMap.Gradient1D))
					return null;
				double value = getGradientAt(x, y, specs.min, specs.max);
				if (!Double.isFinite(value))
					return null;
				String color = ((ColorMap.Gradient1D<String>) owner.getColorMap()).translate(value);
				return "Trait: " + BasicTooltipProvider.SPAN_COLOR + color
						+ BasicTooltipProvider.TABLE_CELL_BULLET + formatGradientTooltip(value);
			case DISCRETE_TRAIT:
				String[] colors = getDiscreteTraitsColors();
				String[] labels = specs.labels;
				int nSegments = Math.min(colors.length, labels.length);
				if (nSegments == 0)
					return null;
				int idx = (bar.getWidth() > 0.0 && bar.getHeight() > 0.0)
						? getBandIndexAt(x, y, nSegments)
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
	private double getGradientAt(int x, int y, double min, double max) {
		double barWidth = bar.getWidth();
		double barHeight = bar.getHeight();
		if (barWidth <= 0.0 || barHeight <= 0.0)
			return Double.NaN;
		double fraction = getFractionAt(x, y);
		if (!Double.isFinite(fraction))
			return Double.NaN;
		boolean vertical = owner.style.legendPos.isVertical();
		double span = vertical ? barHeight : barWidth;
		double[] samples = getBins(min, max, span, specs.markers);
		if (samples.length == 0)
			return Double.NaN;
		int idx = Math.max(0, Math.min(samples.length - 1, (int) Math.floor(fraction * samples.length)));
		if (fraction >= 1.0)
			idx = samples.length - 1;
		return samples[idx];
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
	private double getFractionAt(int x, int y) {
		double barX = bar.getX();
		double barY = bar.getY();
		double barWidth = bar.getWidth();
		double barHeight = bar.getHeight();
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
	private int getBandIndexAt(int x, int y, int nBands) {
		double fraction = getFractionAt(x, y);
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
	private double[] getBins(double min, double max, double span, double[] markers) {
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
	private void drawMarkers(double[] markers, double min, double max) {
		if (markers.length == 0)
			return;
		double barY = bar.getY();
		double barHeight = bar.getHeight();
		double range = max - min;
		for (double marker : markers) {
			double frac = (range <= 0.0 ? 0.5 : (max - marker) / range);
			double y = Math.max(barY, Math.min(barY + barHeight, barY + frac * barHeight));
			owner.g.setFillStyle(owner.style.frameColor);
			String markerLabel = Formatter.formatPretty(marker, 2);
			owner.g.fillText(markerLabel, getLabelX(markerLabel), y + 4.5);
		}
	}

	/**
	 * Compute the x-coordinate used to draw a legend label next to the current bar.
	 *
	 * @param label the label text to position
	 * @return x-coordinate for the label baseline
	 */
	private double getLabelX(String label) {
		double barWidth = bar.getWidth();
		double labelX = (owner.style.legendPos == GraphStyle.Position.EAST
				? bounds.getX() + barWidth + getLabelPad(owner.style.legendPos)
				: bounds.getX());
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
	private double getHorizontalLabelY(double southLabelGap, double northLabelY) {
		double barY = bar.getY();
		double barHeight = bar.getHeight();
		if (owner.style.legendPos == GraphStyle.Position.SOUTH)
			return barY + southLabelGap;
		switch (specs.mode) {
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
	private double getLabelPad(GraphStyle.Position pos) {
		return (pos == GraphStyle.Position.WEST && !owner.style.showYAxisRight ? 2.0 * LABEL_PAD : LABEL_PAD);
	}

	/**
	 * Compute the width of a horizontal discrete-trait legend bar.
	 *
	 * @param labels     discrete legend labels
	 * @param graphWidth width of the actual graph-content bounds
	 * @return legend bar width in pixels
	 */
	private double getHorizontalDiscreteTraitsBarWidth(String[] labels, double graphWidth) {
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
	private String[] getDiscreteTraitsColors() {
		if (specs.mode != Legend2D.Mode.DISCRETE_TRAIT || !(owner.getColorMap() instanceof ColorMap.Index))
			return new String[0];
		return ((ColorMap.Index<String>) owner.getColorMap()).getColors();
	}

	/**
	 * Format a numeric gradient value for legend labels and tooltips.
	 *
	 * @param value the value to format
	 * @return formatted legend text
	 */
	private String formatGradientLegend(double value) {
		return specs.inPercent ? Formatter.formatPercent(value, 1) : Formatter.formatPretty(value, 2);
	}

	/**
	 * Format a numeric gradient value for HTML tooltips.
	 *
	 * @param value the value to format
	 * @return formatted tooltip text
	 */
	private String formatGradientTooltip(double value) {
		return specs.inPercent ? Formatter.formatPercent(value, 1) : Formatter.pretty(value, 2);
	}
}
