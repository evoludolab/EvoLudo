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
	 * Height of horizontal discrete-trait legends in pixels, which show their
	 * labels inside the color bar.
	 */
	private static final double DISCRETE_BAR_HEIGHT = 16.0;

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
	 * @return {@code true} if the room reserved for the legend may change, which
	 *         requires recalculating the bounds of the graph, see
	 *         {@link #reserve(Rectangle2D)}
	 */
	boolean setSpecs(Specs legendSpecs) {
		Mode old = specs.mode;
		this.specs = (legendSpecs == null ? Specs.none() : legendSpecs);
		// the room for density gradients does not depend on their range
		return old != specs.mode || specs.mode != Mode.DENSITY_GRADIENT;
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
		double min = getGradientMin();
		double max = getGradientMax();
		boolean discrete = (specs.mode == Mode.DISCRETE_TRAIT);
		if (!discrete && (!Double.isFinite(min) || !Double.isFinite(max)))
			return;
		String font = owner.g.getFont();
		owner.setFont(owner.style.ticksLabelFont);
		if (discrete) {
			for (String label : specs.labels)
				labelWidth = widest(labelWidth, label);
		} else if (specs.mode == Mode.DENSITY_GRADIENT) {
			labelWidth = widest(0.0, Formatter.formatPercent(0.999, 1));
		} else {
			labelWidth = widest(widest(0.0, formatGradientLegend(min)), formatGradientLegend(max));
			for (double marker : specs.markers)
				labelWidth = widest(labelWidth, Formatter.formatPretty(marker, 2));
		}
		owner.g.setFont(font);
		if (owner.style.legendPos.isVertical())
			reserveWidth = labelWidth + BAR_WIDTH + LABEL_PAD + GAP;
		else
			reserveHeight = GAP + LABEL_PAD + (discrete ? DISCRETE_BAR_HEIGHT : BAR_WIDTH + 14.0);
		graphBounds.setSize(graphBounds.getWidth() - reserveWidth, graphBounds.getHeight() - reserveHeight);
	}

	/**
	 * Get the larger of {@code width} and the width of {@code label} in the current
	 * font.
	 *
	 * @param width the width so far
	 * @param label the label to measure
	 * @return the larger width
	 */
	private double widest(double width, String label) {
		return Math.max(width, owner.g.measureText(label).getWidth());
	}

	/**
	 * Position the legend within the available graph area.
	 *
	 * @param width       the total graph width
	 * @param height      the total graph height
	 * @param graphBounds the drawable graph bounds, adjusted as needed
	 */
	void setBounds(int width, int height, Rectangle2D graphBounds) {
		if (!hasLegend())
			return;
		GraphStyle.Position pos = owner.style.legendPos;
		if (pos.isVertical()) {
			double h = graphBounds.getHeight();
			boolean east = (pos == GraphStyle.Position.EAST);
			bounds.setOrigin(east ? width - reserveWidth + GAP * 0.5 : owner.style.minPadding,
					graphBounds.getY() + h * 0.2);
			bar.set(bounds.getX() + (east ? 0.0 : labelWidth + getLabelPad(pos)), bounds.getY(), BAR_WIDTH, h * 0.6);
			if (!east)
				graphBounds.shift(reserveWidth, 0.0);
			return;
		}
		boolean discrete = (specs.mode == Mode.DISCRETE_TRAIT);
		double graphWidth = graphBounds.getWidth();
		// discrete legends widen to fit their labels
		double barWidth = graphWidth * 0.6;
		if (discrete)
			barWidth = Math.min(graphWidth, Math.max(barWidth, specs.labels.length * (labelWidth + 2.0 * LABEL_PAD)));
		boolean south = (pos == GraphStyle.Position.SOUTH);
		bounds.setOrigin(graphBounds.getX() + 0.5 * (graphWidth - barWidth),
				south ? height - reserveHeight : owner.style.minPadding);
		bar.set(bounds.getX(), bounds.getY(), barWidth, discrete ? DISCRETE_BAR_HEIGHT : BAR_WIDTH);
		if (!south)
			graphBounds.shift(0.0, reserveHeight);
	}

	/**
	 * Draw the legend into the owner's canvas.
	 */
	void draw() {
		if (!hasLegend() || owner.hasMessage())
			return;
		owner.g.save();
		owner.g.scale(owner.scale, owner.scale);
		if (specs.mode == Mode.DISCRETE_TRAIT)
			drawDiscreteTraits();
		else
			drawGradient();
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
		String color;
		String value;
		if (specs.mode == Mode.DISCRETE_TRAIT) {
			String[] colors = getDiscreteTraitsColors();
			double fraction = getFractionAt(x, y);
			int nBands = Math.min(colors.length, specs.labels.length);
			if (!Double.isFinite(fraction) || nBands == 0)
				return null;
			int idx = Math.min(nBands - 1, (int) (fraction * nBands));
			color = colors[idx];
			value = specs.labels[idx];
		} else {
			double gradient = getGradientAt(x, y);
			if (!Double.isFinite(gradient))
				return null;
			color = ((ColorMap.Gradient1D<String>) owner.getColorMap()).translate(gradient);
			value = formatGradientTooltip(gradient);
		}
		String label = (specs.mode == Mode.FITNESS_GRADIENT ? "Fitness"
				: specs.mode == Mode.DENSITY_GRADIENT ? "Frequency" : "Trait");
		return label + ": " + BasicTooltipProvider.SPAN_COLOR + color + BasicTooltipProvider.TABLE_CELL_BULLET
				+ value;
	}

	/**
	 * Draw the current gradient-style legend, including endpoint labels and marker
	 * annotations.
	 */
	private void drawGradient() {
		if (!(owner.getColorMap() instanceof ColorMap.Gradient1D))
			return;
		double min = getGradientMin();
		double max = getGradientMax();
		if (!Double.isFinite(min) || !Double.isFinite(max) || bar.getWidth() <= 0.0 || bar.getHeight() <= 0.0)
			return;
		boolean vertical = owner.style.legendPos.isVertical();
		double[] samples = getBins(min, max);
		ColorMap.Gradient1D<String> gradient = (ColorMap.Gradient1D<String>) owner.getColorMap();
		for (int i = 0; i < samples.length; i++) {
			owner.g.setFillStyle(gradient.translate(samples[i]));
			fillBand(getBand(i, samples.length));
		}
		drawFrame();
		owner.g.setFillStyle(owner.style.frameColor);
		owner.setFont(owner.style.ticksLabelFont);
		String minLabel = formatGradientLegend(min);
		String maxLabel = formatGradientLegend(max);
		double barX = bar.getX();
		double barY = bar.getY();
		if (vertical) {
			owner.g.fillText(maxLabel, getLabelX(maxLabel), barY + 4.5);
			owner.g.fillText(minLabel, getLabelX(minLabel), barY + bar.getHeight() + 4.5);
			for (double marker : specs.markers) {
				double frac = (max > min ? (max - marker) / (max - min) : 0.5);
				double y = barY + Math.max(0.0, Math.min(1.0, frac)) * bar.getHeight();
				String markerLabel = Formatter.formatPretty(marker, 2);
				owner.g.fillText(markerLabel, getLabelX(markerLabel), y + 4.5);
			}
			return;
		}
		double labelY;
		if (owner.style.legendPos == GraphStyle.Position.SOUTH)
			labelY = barY + BAR_WIDTH + 12.5;
		else if (specs.mode == Mode.CONTINUOUS_TRAIT)
			labelY = -owner.style.minPadding;
		else
			labelY = barY + bar.getHeight() + 12.5;
		owner.g.fillText(minLabel, barX, labelY);
		owner.g.fillText(maxLabel, barX + bar.getWidth() - owner.g.measureText(maxLabel).getWidth(), labelY);
	}

	/**
	 * Get the start and end of band {@code i} out of {@code n} along the legend
	 * bar. Bands start and end on whole pixels, except the last one, which ends
	 * with the bar, and are at least one pixel wide.
	 *
	 * @param i the index of the band
	 * @param n the number of bands
	 * @return the start and end of the band in pixels
	 */
	private double[] getBand(int i, int n) {
		boolean vertical = owner.style.legendPos.isVertical();
		double origin = vertical ? bar.getY() : bar.getX();
		double length = vertical ? bar.getHeight() : bar.getWidth();
		double step = length / n;
		double start = Math.floor(origin + i * step);
		double end = (i == n - 1 ? origin + length : Math.floor(origin + (i + 1) * step));
		return new double[] { start, Math.max(end, start + 1.0) };
	}

	/**
	 * Fill the band of the legend bar between {@code band[0]} and {@code band[1]}
	 * with the current fill style.
	 *
	 * @param band the start and end of the band in pixels
	 */
	private void fillBand(double[] band) {
		if (owner.style.legendPos.isVertical())
			owner.fillRect(bar.getX(), band[0], bar.getWidth(), band[1] - band[0]);
		else
			owner.fillRect(band[0], bar.getY(), band[1] - band[0], bar.getHeight());
	}

	/**
	 * Stroke the outline around the current legend bar.
	 */
	private void drawFrame() {
		owner.g.setStrokeStyle(owner.style.frameColor);
		owner.g.setLineWidth(1.0);
		if (owner.style.legendPos.isVertical())
			owner.g.strokeRect(bar.getX() + 0.5, bar.getY() - 0.5, bar.getWidth(), bar.getHeight() + 1.0);
		else
			owner.g.strokeRect(bar.getX() - 0.5, bar.getY() + 0.5, bar.getWidth() + 1.0, bar.getHeight());
	}

	/**
	 * Draw the segmented discrete-trait legend. Vertical legends show the labels
	 * next to the bar, horizontal legends inside the segments.
	 */
	private void drawDiscreteTraits() {
		String[] colors = getDiscreteTraitsColors();
		String[] labels = specs.labels;
		int nSegments = Math.min(colors.length, labels.length);
		if (nSegments <= 0 || bar.getWidth() <= 0.0 || bar.getHeight() <= 0.0)
			return;
		boolean vertical = owner.style.legendPos.isVertical();
		owner.setFont(owner.style.ticksLabelFont);
		for (int i = 0; i < nSegments; i++) {
			double[] band = getBand(i, nSegments);
			owner.g.setFillStyle(colors[i]);
			fillBand(band);
			if (vertical) {
				owner.g.setFillStyle(owner.style.frameColor);
				owner.g.fillText(labels[i], getLabelX(labels[i]), 0.5 * (band[0] + band[1]) + 4.5);
				continue;
			}
			owner.g.setFillStyle(ColorMapCSS.luminance(colors[i]) > 0.55 ? "#000000" : "#ffffff");
			double textWidth = owner.g.measureText(labels[i]).getWidth();
			owner.g.fillText(labels[i], 0.5 * (band[0] + band[1] - textWidth),
					bar.getY() + 0.5 * (bar.getHeight() + 9.0));
		}
		drawFrame();
	}

	/**
	 * Map a position on the current gradient legend to the corresponding data
	 * value.
	 *
	 * @param x the x-coordinate in graph widget coordinates
	 * @param y the y-coordinate in graph widget coordinates
	 * @return legend value, or {@link Double#NaN} if the point is outside
	 */
	private double getGradientAt(int x, int y) {
		double fraction = getFractionAt(x, y);
		if (!(owner.getColorMap() instanceof ColorMap.Gradient1D) || !Double.isFinite(fraction))
			return Double.NaN;
		double[] samples = getBins(getGradientMin(), getGradientMax());
		return samples[Math.min(samples.length - 1, (int) Math.floor(fraction * samples.length))];
	}

	/**
	 * Resolve the minimum value represented by the current gradient legend.
	 *
	 * @return the minimum gradient value
	 */
	private double getGradientMin() {
		if (owner.getColorMap() instanceof ColorMap.Gradient1D)
			return ((ColorMap.Gradient1D<String>) owner.getColorMap()).getMin();
		return specs.min;
	}

	/**
	 * Resolve the maximum value represented by the current gradient legend.
	 *
	 * @return the maximum gradient value
	 */
	private double getGradientMax() {
		if (owner.getColorMap() instanceof ColorMap.Gradient1D)
			return ((ColorMap.Gradient1D<String>) owner.getColorMap()).getMax();
		return specs.max;
	}

	/**
	 * Convert graph coordinates to a normalized position along the current legend
	 * bar.
	 *
	 * @param x the x-coordinate in graph widget coordinates
	 * @param y the y-coordinate in graph widget coordinates
	 * @return normalized position in {@code [0,1]}, or {@link Double#NaN} if
	 *         outside the legend bar or the bar is empty
	 */
	private double getFractionAt(int x, int y) {
		double relX = x - owner.canvasPadX() - bar.getX();
		double relY = y - owner.canvasPadY() - bar.getY();
		double barWidth = bar.getWidth();
		double barHeight = bar.getHeight();
		if (barWidth <= 0.0 || barHeight <= 0.0 || relX < 0.0 || relX > barWidth || relY < 0.0 || relY > barHeight)
			return Double.NaN;
		return owner.style.legendPos.isVertical() ? relY / barHeight : relX / barWidth;
	}

	/**
	 * Sample values along the gradient legend, one per band of at least
	 * {@link #MIN_BAND_HEIGHT} pixels, preserving marker values when possible.
	 *
	 * @param min the minimum legend value
	 * @param max the maximum legend value
	 * @return sampled values from top-to-bottom or left-to-right
	 */
	private double[] getBins(double min, double max) {
		boolean vertical = owner.style.legendPos.isVertical();
		double span = vertical ? bar.getHeight() : bar.getWidth();
		int steps = Math.max(2, (int) Math.floor(span / MIN_BAND_HEIGHT));
		double[] samples = new double[steps];
		for (int i = 0; i < steps; i++) {
			double frac = (double) i / (steps - 1);
			samples[i] = vertical ? min + (1.0 - frac) * (max - min) : min + frac * (max - min);
		}
		double range = max - min;
		if (range <= 0.0)
			return samples;
		boolean[] used = new boolean[steps];
		for (double marker : specs.markers) {
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
	 * Compute the x-coordinate used to draw a legend label next to the current bar.
	 *
	 * @param label the label text to position
	 * @return x-coordinate for the label baseline
	 */
	private double getLabelX(String label) {
		GraphStyle.Position pos = owner.style.legendPos;
		if (pos == GraphStyle.Position.EAST)
			return bounds.getX() + bar.getWidth() + getLabelPad(pos);
		if (pos == GraphStyle.Position.WEST)
			return bounds.getX() + labelWidth - owner.g.measureText(label).getWidth();
		return bounds.getX();
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
	 * Format a numeric gradient value for legend labels, omitting trailing zeros.
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
