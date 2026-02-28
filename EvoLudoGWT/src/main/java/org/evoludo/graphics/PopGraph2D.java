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

import org.evoludo.geom.Node2D;
import org.evoludo.geom.Path2D;
import org.evoludo.geom.Point2D;
import org.evoludo.graphics.AbstractGraph.Shifting;
import org.evoludo.simulator.Network.Status;
import org.evoludo.simulator.ColorMap;
import org.evoludo.simulator.geometries.AbstractGeometry;
import org.evoludo.simulator.geometries.GeometryType;
import org.evoludo.simulator.geometries.HierarchicalGeometry;
import org.evoludo.simulator.Network2D;
import org.evoludo.simulator.models.CModel;
import org.evoludo.simulator.models.Data;
import org.evoludo.simulator.models.Model;
import org.evoludo.simulator.models.DModel;
import org.evoludo.simulator.modules.Module;
import org.evoludo.simulator.modules.Map2Fitness;
import org.evoludo.simulator.modules.Discrete;
import org.evoludo.simulator.modules.Continuous;
import org.evoludo.simulator.views.AbstractView;
import org.evoludo.util.Formatter;

import com.google.gwt.core.client.JsArray;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.dom.client.Touch;
import com.google.gwt.event.dom.client.MouseDownEvent;
import com.google.gwt.event.dom.client.MouseMoveEvent;
import com.google.gwt.event.dom.client.MouseUpEvent;
import com.google.gwt.event.dom.client.TouchEndEvent;
import com.google.gwt.event.dom.client.TouchMoveEvent;

/**
 * The graphical representation of lattices and network structures in 2D. This
 * class adapts a generic population graph view to a 2D canvas, supporting
 * multiple geometry types (triangular, honeycomb, square variants, and generic
 * network layouts) and providing custom drawing, hit-testing and user
 * interaction behavior.
 *
 * <h3>Responsibilities</h3>
 * <ul>
 * <li>Calculate layout bounds and element sizes for a variety of geometry
 * types based on the current canvas size and configuration.</li>
 * <li>Render lattice cells or network nodes and links to a canvas using an
 * internal graphics context.</li>
 * <li>Map input coordinates (mouse/touch) to node indices for selection and
 * dragging, with geometry-specific hit-test logic.</li>
 * <li>Allow interactive shifting of individual network nodes (dragging) and
 * shifting of lattice views.</li>
 * <li>Expose export capability to draw into an external MyContext2d context
 * (e.g., for image export or printing).</li>
 * </ul>
 *
 * <h3>Supported Geometry Types</h3>
 * <ul>
 * <li>TRIANGULAR — drawn as interleaved up/down triangles</li>
 * <li>HONEYCOMB — drawn as regular hexagons</li>
 * <li>SQUARE, SQUARE_MOORE, SQUARE_NEUMANN, SQUARE_NEUMANN_2ND — drawn as
 * square grids with several decoration/neighbor variations; supports
 * hierarchical gaps when geometry describes a HIERARCHY</li>
 * <li>Generic network — uses Node2D positions and a radius-based scaling to
 * draw nodes and links; supports manual node shifting</li>
 * </ul>
 *
 * <h3>Key Concepts &amp; Fields</h3>
 * <ul>
 * <li>data: a String[] holding per-node CSS color values used to paint each
 * cell/node.</li>
 * <li>Bounds and sizing: internal fields such as side, dw, dw2, dh, dh3 and
 * dR compute per-cell/node sizes and are recalculated in calcBounds.</li>
 * <li>Hierarchy support: when geometry indicates a HIERARCHY, the component
 * accounts for hierarchical gaps and levels (hLevels, hPeriods).</li>
 * <li>Interaction flags: hitNode tracks the currently selected node index,
 * hitDragged indicates whether a node is currently being dragged.</li>
 * </ul>
 *
 * <h3>Interaction &amp; Events</h3>
 * <ul>
 * <li>Mouse Down: left-click resolves a node via findNodeAt and sets up
 * dragging
 * state and CSS cursor classes.</li>
 * <li>Mouse Move: when dragging a selected node, the node position is updated
 * (network nodes) or the view is shifted (lattices). CSS classes are toggled
 * during drag.</li>
 * <li>Mouse Up / Touch End: releases dragging state, restores classes, and can
 * update tooltips as needed.</li>
 * </ul>
 *
 * <h3>Rendering</h3>
 * The paint method orchestrates drawing:
 * <ul>
 * <li>When a static lattice layout is available it delegates to dedicated
 * drawXxx methods (drawTriangular, drawHoneycomb, drawSquareLattice,
 * drawSquareNeumann2nd).</li>
 * <li>For arbitrary networks the drawNetwork method scales the universe,
 * draws links and nodes (with per-node color optimization) and supports
 * node diameter computation (dR).</li>
 * <li>prepCanvas prepares the canvas transform (scale, translation and
 * zoom) and manages a saved graphics state so drawing is isolated.</li>
 * </ul>
 *
 * <h3>Sizing &amp; Limits</h3>
 * <ul>
 * <li>MIN_DW, MIN_DH and MIN_DR define minimum drawable dimensions; if elements
 * would be smaller than these thresholds, the graph is disabled and a
 * message is displayed.</li>
 * </ul>
 *
 * <h3>Performance</h3>
 * Drawing operations use batching and occasional optimizations (e.g., reusing
 * fill style until it changes) but may still be expensive for very large
 * populations or frequent updates.
 *
 * <h3>Extensibility &amp; Limitations</h3>
 * <ul>
 * <li>3D geometries (CUBE) are not represented graphically in this class and
 * will display an informational message.</li>
 * <li>Hit-testing for networks uses node bounding checks (Node2D.isHit)
 * and returns the top-most node when overlaps occur.</li>
 * <li>Layouts flagged as LAYOUT_IN_PROGRESS are treated as invalid for
 * hit-testing until the layout completes.</li>
 * </ul>
 *
 * <h3>Styling</h3>
 * CSS classes used by the component:
 * <ul>
 * <li>.evoludo-PopGraph2D — primary graph element</li>
 * <li>.evoludo-Label2D — label element</li>
 * <li>.evoludo-cursorGrabNode — added while a node is grabbed</li>
 * <li>.evoludo-cursorMoveNode — added while a node is being dragged</li>
 * </ul>
 * Many rendering aspects (frame, linkColor, linkWidth, padding, decorated
 * frame)
 * are controlled by the associated style object.
 *
 * <h3>Usage</h3>
 * Instantiate via the view/module infrastructure this project provides. Ensure
 * to call {@link #activate()} and allow {@link #calcBounds()} to run with the
 * appropriate canvas size; {@link #update()} should be invoked whenever the
 * underlying population state changes to refresh the visualization.
 * 
 * @author Christoph Hauert
 */
@SuppressWarnings("java:S110")
public class PopGraph2D extends GenericPopGraph<String, Network2D> implements Shifting {

	/**
	 * Width of the fitness legend color bar in pixels.
	 */
	protected static final double FITNESS_LEGEND_BAR_WIDTH = 10.0;

	/**
	 * Gap between graph and fitness legend in pixels.
	 */
	protected static final double FITNESS_LEGEND_GAP = 16.0;

	/**
	 * Padding between fitness legend labels and graph edges in pixels.
	 */
	protected static final double FITNESS_LEGEND_OUTER_PAD = 6.0;

	/**
	 * Gap between fitness legend labels and color bar in pixels.
	 */
	protected static final double FITNESS_LEGEND_LABEL_PAD = 4.0;

	/**
	 * Minimum height of one legend color band in pixels.
	 */
	protected static final double FITNESS_LEGEND_MIN_BAND_HEIGHT = 2.0;

	/**
	 * Create a graph for graphically visualizing the structure of a network (or
	 * population). Allocates the canvas and the label and retrieves the shared
	 * tooltip and context menu.
	 * 
	 * <h3>CSS Style Rules</h3>
	 * <dl>
	 * <dt>.evoludo-PopGraph2D</dt>
	 * <dd>the graph element.</dd>
	 * <dt>.evoludo-Label2D</dt>
	 * <dd>the label element.</dd>
	 * </dl>
	 * 
	 * @param view   the view of this graph
	 * @param module the module backing the graph
	 */
	public PopGraph2D(AbstractView<?> view, Module<?> module) {
		super(view, module);
	}

	@Override
	protected void onLoad() {
		super.onLoad();
		setStylePrimaryName("evoludo-PopGraph2D");
		label.setStyleName("evoludo-Label2D");
	}

	@Override
	public void setGeometry(AbstractGeometry geometry) {
		super.setGeometry(geometry);
		calcBounds();
		ensureData();
	}

	@Override
	public void activate() {
		super.activate();
		ensureData();
	}

	@Override
	public void onResize() {
		super.onResize();
		ensureData();
	}

	/**
	 * Ensure that the data array is properly initialized based on the current
	 * geometry.
	 */
	protected void ensureData() {
		if (noGraph || geometry == null) {
			data = null;
		} else {
			int size = geometry.getSize();
			if (data == null || data.length != size)
				data = new String[size];
		}
	}

	/**
	 * Rebin graph data when the number of bins changes.
	 * <p>
	 * Default implementation does nothing. Subclasses can override to preserve
	 * rendered state/history across bin changes.
	 *
	 * @param oldBins previous number of bins per axis
	 * @param newBins new number of bins per axis
	 */
	public void rebinGraphData(int oldBins, int newBins) {
		// default no-op
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * Depending on the backing geometry this either
	 * <ol>
	 * <li>shows a message, if no graphical representation is available, e.g. for 3D
	 * cubic lattices, or if there are too many nodes so that each node becomes too
	 * small to display on screen.
	 * <li>shows lattice geometries.
	 * <li>initiates the generic layouting process for arbitrary network structures.
	 * </ol>
	 * 
	 * @see Network2D
	 */
	@Override
	public boolean paint(boolean force) {
		if (super.paint(force))
			return true;
		if (hasStaticLayout())
			drawLattice();
		else if (!invalidated)
			drawNetwork();
		return false;
	}

	@Override
	protected void drawLattice() {
		if (!prepCanvas())
			return;
		invalidated = false;
		GeometryType type = geometry.getType();
		if (isHierarchy)
			type = ((HierarchicalGeometry) geometry).getSubType();

		switch (type) {
			case TRIANGULAR:
				drawTriangular();
				break;
			case HEXAGONAL:
				drawHoneycomb();
				break;
			case SQUARE_NEUMANN:
			case SQUARE_MOORE:
			case SQUARE:
				drawSquareLattice();
				break;
			case SQUARE_NEUMANN_2ND:
				drawSquareNeumann2nd();
				break;
			default:
				logger.warning("Unsupported geometry: " + type.getTitle());
		}
		drawFitnessLegend();
		g.restore();
	}

	/**
	 * Draw triangular lattice.
	 */
	private void drawTriangular() {
		Path2D triup = Path2D.createPolygon2D(new double[] { 0, dw, dw2 }, new double[] { 0, 0, -dh });
		Path2D tridown = Path2D.createPolygon2D(new double[] { 0, dw, dw2 }, new double[] { 0, 0, dh });
		triup.translate(0, dh);
		tridown.translate(dw2, 0);
		int s2 = side / 2;
		int dht = -side * dh;
		for (int h = 0; h < side; h += 2) {
			int row = h * side;
			for (int w = 0; w < side; w += 2) {
				g.setFillStyle(data[row + w]);
				fill(triup);
				triup.translate(dw, 0);
				g.setFillStyle(data[row + w + 1]);
				fill(tridown);
				tridown.translate(dw, 0);
			}
			int dwtm = -s2 * dw - dw2;
			int dwtp = dwtm + dw;
			triup.translate(dwtp, dh);
			tridown.translate(dwtm, dh);
			row += side;
			for (int w = 0; w < side; w += 2) {
				g.setFillStyle(data[row + w]);
				fill(tridown);
				tridown.translate(dw, 0);
				g.setFillStyle(data[row + w + 1]);
				fill(triup);
				triup.translate(dw, 0);
			}
			triup.translate(dwtm, dh);
			tridown.translate(dwtp, dh);
		}
		triup.translate(0, dht);
		tridown.translate(0, dht);
		drawFrame(0, 0);
	}

	/**
	 * Draw honeycomb lattice.
	 */
	private void drawHoneycomb() {
		Path2D hex = Path2D.createPolygon2D(new double[] { 0, -dw2, -dw2, 0, dw2, dw2 },
				new double[] { 0, dh3, dh, dh + dh3, dh, dh3 });
		hex.translate(dw2, 0);
		int dht = -side * dh;
		int dwt = -side * dw;
		for (int h = 0; h < side; h++) {
			int row = h * side;
			for (int w = 0; w < side; w++) {
				g.setFillStyle(data[row + w]);
				fill(hex);
				hex.translate(dw, 0);
			}
			int wt = dwt - (2 * (h % 2) - 1) * dw2;
			hex.translate(wt, dh);
		}
		if (side % 2 == 0)
			hex.translate(0, dht);
		else
			hex.translate(-dw2, dht);
		drawFrame(0, 0);
	}

	/**
	 * Draw square (hierarchical) lattice (SQUARE, SQUARE_MOORE, SQUARE_NEUMANN).
	 */
	private void drawSquareLattice() {
		int yshift = (int) bounds.getHeight() - dh;
		int row = 0;
		for (int h = 0; h < side; h++) {
			if (isHierarchy && h > 0) {
				yshift -= hierarchyGapAt(h);
			}
			int xshift = 0;
			for (int w = 0; w < side; w++) {
				if (isHierarchy && w > 0) {
					xshift += hierarchyGapAt(w);
				}
				g.setFillStyle(data[row + w]);
				fillRect(xshift, yshift, dw, dh);
				xshift += dw;
			}
			yshift -= dh;
			row += side;
		}
		if (style.showDecoratedFrame)
			drawFrame(4, 4);
		else
			drawFrame(0, 0);
	}

	/**
	 * Compute number of hierarchy gaps that occur before the given index.
	 * This encapsulates the previous inline loop that counted levels until the
	 * first non-divisor was encountered.
	 *
	 * @param idx the row or column index
	 * @return total gap in pixels to subtract/add for this index
	 */
	private int hierarchyGapAt(int idx) {
		int gaps = 0;
		for (int i = 0; i < hLevels; i++) {
			if (idx % hPeriods[i] != 0)
				break;
			gaps++;
		}
		return gaps * HIERARCHY_GAP;
	}

	/**
	 * Draw the special SQUARE_NEUMANN_2ND lattice.
	 */
	private void drawSquareNeumann2nd() {
		int yshift = (int) bounds.getHeight() - dh;
		int row = 0;
		for (int h = 0; h < side; h++) {
			int xshift = 0;
			for (int w = 0; w < side; w++) {
				g.setFillStyle(data[row + w]);
				if (h % 2 == w % 2) {
					fillRect(xshift, yshift, dw, dh);
					xshift += dw;
				} else {
					xshift += dw2;
					int yc = yshift + dw2;
					fillCircle(xshift, yc, dw2);
					xshift += dw2;
				}
			}
			yshift -= dh;
			row += side;
		}
		if (style.showDecoratedFrame)
			drawFrame(4, 4);
		else
			drawFrame(0, 0);
	}

	@Override
	protected void drawNetwork() {
		if (!prepCanvas())
			return;
		invalidated = false;
		int nNodes = geometry.getSize();
		// scale universe
		double r = network.getRadius();
		double su = bounds.getWidth() / (r + r); // same as height in this case
		g.save();
		g.scale(su, su);
		g.save();
		g.translate(r, r);
		g.setFillStyle(style.linkColor);
		Path2D links = network.getLinks();
		if (!links.isEmpty()) {
			g.setLineWidth(style.linkWidth);
			stroke(links);
		}
		String current = data[0];
		g.setFillStyle(current);
		Node2D[] nodes = network.toArray();
		for (int k = 0; k < nNodes; k++) {
			String next = data[k];
			// potential optimization of drawing
			if (!next.equals(current)) {
				g.setFillStyle(next);
				current = next;
			}
			fillCircle(nodes[k]);
		}
		g.restore();
		drawFrame(0, 0, su);
		g.restore();
		drawFitnessLegend();
		g.restore();
	}

	/**
	 * Draw a vertical color legend for fitness-based population graphs.
	 */
	private void drawFitnessLegend() {
		if (!hasLegend() || (legendReserveWidth <= 0.0 && legendReserveHeight <= 0.0)
				|| !(getColorMap() instanceof ColorMap.Gradient1D))
			return;
		Model model = view.getModel();
		double min = model.getMinFitness(module.getId());
		double max = model.getMaxFitness(module.getId());
		if (!Double.isFinite(min) || !Double.isFinite(max))
			return;
		ColorMap.Gradient1D<String> gradient = (ColorMap.Gradient1D<String>) getColorMap();
		ArrayList<LegendMarker> markers = collectLegendMarkers();
		if (style.legendPos.isVertical()) {
			double barHeight = bounds.getHeight() * 0.6;
			double barTop = (bounds.getHeight() - barHeight) * 0.5;
			double barX;
			double labelX;
			switch (style.legendPos) {
				case EAST:
					barX = bounds.getWidth() + FITNESS_LEGEND_GAP;
					labelX = barX + FITNESS_LEGEND_BAR_WIDTH + FITNESS_LEGEND_LABEL_PAD;
					break;
				case WEST:
					barX = -FITNESS_LEGEND_GAP - FITNESS_LEGEND_BAR_WIDTH;
					labelX = -legendReserveWidth + FITNESS_LEGEND_OUTER_PAD;
					break;
				default:
					return;
			}
			double[] samples = getLegendSamples(min, max, barHeight, markers);
			int steps = samples.length;
			double stepHeight = barHeight / steps;
			for (int i = 0; i < steps; i++) {
				double y0 = Math.floor(barTop + i * stepHeight);
				double y1 = Math.floor(barTop + (i + 1) * stepHeight);
				if (i == steps - 1)
					y1 = barTop + barHeight;
				if (y1 <= y0)
					y1 = y0 + 1.0;
				g.setFillStyle(gradient.translate(samples[i]));
				fillRect(barX, y0, FITNESS_LEGEND_BAR_WIDTH, y1 - y0);
			}
			g.setStrokeStyle(style.frameColor);
			g.setLineWidth(1.0);
			g.strokeRect(barX + 0.5, barTop - 0.5, FITNESS_LEGEND_BAR_WIDTH, barHeight + 1.0);
			g.setFillStyle(style.frameColor);
			setFont(style.ticksLabelFont);
			g.fillText(Formatter.formatPretty(max, 2), labelX, barTop + 4.5);
			g.fillText(Formatter.formatPretty(min, 2), labelX, barTop + barHeight + 4.5);
			drawLegendMarkers(markers, min, max, barTop, barHeight, labelX);
			return;
		}
		double barWidth = bounds.getWidth() * 0.6;
		double barLeft = (bounds.getWidth() - barWidth) * 0.5;
		double barY;
		double labelY;
		switch (style.legendPos) {
			case SOUTH:
				barY = bounds.getHeight() + FITNESS_LEGEND_GAP;
				labelY = barY + FITNESS_LEGEND_BAR_WIDTH + 11.0;
				break;
			case NORTH:
				barY = -FITNESS_LEGEND_GAP - FITNESS_LEGEND_BAR_WIDTH;
				labelY = -FITNESS_LEGEND_OUTER_PAD;
				break;
			default:
				return;
		}
		double[] samples = getLegendSamples(min, max, barWidth, markers);
		int steps = samples.length;
		double stepWidth = barWidth / steps;
		for (int i = 0; i < steps; i++) {
			double x0 = Math.floor(barLeft + i * stepWidth);
			double x1 = Math.floor(barLeft + (i + 1) * stepWidth);
			if (i == steps - 1)
				x1 = barLeft + barWidth;
			if (x1 <= x0)
				x1 = x0 + 1.0;
			g.setFillStyle(gradient.translate(samples[i]));
			fillRect(x0, barY, x1 - x0, FITNESS_LEGEND_BAR_WIDTH);
		}
		g.setStrokeStyle(style.frameColor);
		g.setLineWidth(1.0);
		g.strokeRect(barLeft - 0.5, barY + 0.5, barWidth + 1.0, FITNESS_LEGEND_BAR_WIDTH);
		g.setFillStyle(style.frameColor);
		setFont(style.ticksLabelFont);
		g.fillText(Formatter.formatPretty(min, 2), barLeft, labelY);
		g.fillText(Formatter.formatPretty(max, 2),
				barLeft + barWidth - g.measureText(Formatter.formatPretty(max, 2)).getWidth(), labelY);
	}

	@Override
	public String getTooltipAt(int x, int y) {
		String tip = getLegendTooltipAt(x, y);
		return tip != null ? tip : super.getTooltipAt(x, y);
	}

	/**
	 * Get the tooltip for the fitness legend at screen coordinates {@code (x,y)}.
	 *
	 * @param x the screen x-coordinate
	 * @param y the screen y-coordinate
	 * @return tooltip HTML or {@code null}
	 */
	private String getLegendTooltipAt(int x, int y) {
		if (!hasLegend() || (legendReserveWidth <= 0.0 && legendReserveHeight <= 0.0)
				|| !(getColorMap() instanceof ColorMap.Gradient1D))
			return null;
		Model model = view.getModel();
		double min = model.getMinFitness(module.getId());
		double max = model.getMaxFitness(module.getId());
		if (!Double.isFinite(min) || !Double.isFinite(max))
			return null;
		double sx = (viewCorner.getX() + x - bounds.getX()) / zoomFactor;
		double sy = (viewCorner.getY() + y - bounds.getY()) / zoomFactor;
		int idx;
		double value;
		if (style.legendPos.isVertical()) {
			double barHeight = bounds.getHeight() * 0.6;
			double barTop = (bounds.getHeight() - barHeight) * 0.5;
			double barX;
			switch (style.legendPos) {
				case EAST:
					barX = bounds.getWidth() + FITNESS_LEGEND_GAP;
					break;
				case WEST:
					barX = -FITNESS_LEGEND_GAP - FITNESS_LEGEND_BAR_WIDTH;
					break;
				default:
					element.removeClassName(EVOLUDO_CURSOR_NODE);
					return null;
			}
			if (sx < barX || sx > barX + FITNESS_LEGEND_BAR_WIDTH || sy < barTop || sy > barTop + barHeight) {
				element.removeClassName(EVOLUDO_CURSOR_NODE);
				return null;
			}
			double[] samples = getLegendSamples(min, max, barHeight, collectLegendMarkers());
			int steps = samples.length;
			double stepHeight = barHeight / steps;
			idx = Math.min(steps - 1, Math.max(0, (int) ((sy - barTop) / stepHeight)));
			value = samples[idx];
		} else {
			double barWidth = bounds.getWidth() * 0.6;
			double barLeft = (bounds.getWidth() - barWidth) * 0.5;
			double barY;
			switch (style.legendPos) {
				case SOUTH:
					barY = bounds.getHeight() + FITNESS_LEGEND_GAP;
					break;
				case NORTH:
					barY = -FITNESS_LEGEND_GAP - FITNESS_LEGEND_BAR_WIDTH;
					break;
				default:
					element.removeClassName(EVOLUDO_CURSOR_NODE);
					return null;
			}
			if (sx < barLeft || sx > barLeft + barWidth || sy < barY || sy > barY + FITNESS_LEGEND_BAR_WIDTH) {
				element.removeClassName(EVOLUDO_CURSOR_NODE);
				return null;
			}
			double[] samples = getLegendSamples(min, max, barWidth, collectLegendMarkers());
			int steps = samples.length;
			double stepWidth = barWidth / steps;
			idx = Math.min(steps - 1, Math.max(0, (int) ((sx - barLeft) / stepWidth)));
			value = samples[idx];
		}
		String color = ((ColorMap.Gradient1D<String>) getColorMap()).translate(value);
		element.removeClassName(EVOLUDO_CURSOR_NODE);
		return "fitness: <span style='color:" + color + ";'>&#x25A0;</span> "
				+ Formatter.formatPretty(value, 2);
	}

	/**
	 * Get the sampled fitness values used to render the legend.
	 *
	 * @param min     minimum fitness
	 * @param max     maximum fitness
	 * @param height  legend height
	 * @param markers homogeneous-state markers
	 * @return sampled fitness values from top to bottom
	 */
	private double[] getLegendSamples(double min, double max, double height, ArrayList<LegendMarker> markers) {
		int steps = Math.max(2, (int) Math.floor(height / FITNESS_LEGEND_MIN_BAND_HEIGHT));
		double[] samples = new double[steps];
		for (int i = 0; i < steps; i++)
			samples[i] = min + (1.0 - (double) i / Math.max(1, steps - 1)) * (max - min);
		double range = max - min;
		if (range <= 0.0 || markers.isEmpty())
			return samples;
		boolean[] used = new boolean[steps];
		for (LegendMarker marker : markers) {
			int target = (int) Math.rint((max - marker.value) / range * (steps - 1));
			target = Math.max(0, Math.min(steps - 1, target));
			for (int delta = 0; delta < steps; delta++) {
				int lower = target - delta;
				if (lower >= 0 && !used[lower]) {
					samples[lower] = marker.value;
					used[lower] = true;
					break;
				}
				int upper = target + delta;
				if (delta > 0 && upper < steps && !used[upper]) {
					samples[upper] = marker.value;
					used[upper] = true;
					break;
				}
			}
		}
		return samples;
	}

	/**
	 * Helper method to get the canvas ready for drawing the graph.
	 * 
	 * @return {@code true} if the canvas is ready for drawing
	 */
	private boolean prepCanvas() {
		if (hasMessage)
			return false;
		g.save();
		g.scale(scale, scale);
		clearCanvas();
		g.translate(bounds.getX() - viewCorner.getX(), bounds.getY() - viewCorner.getY());
		g.scale(zoomFactor, zoomFactor);
		return true;
	}

	@Override
	protected double getMaxViewCornerX(double zoom) {
		return getOffsetWidth() * (zoom - 1.0);
	}

	@Override
	protected double getMaxViewCornerY(double zoom) {
		return getOffsetHeight() * (zoom - 1.0);
	}

	/**
	 * The size of the graph for lattices.
	 */
	int side;

	/**
	 * The width of a node for lattices.
	 */
	int dw;

	/**
	 * Convenience variable. One half of the width of a node for lattices,
	 * {@code dw/2}.
	 */
	int dw2;

	/**
	 * The height of a node for lattices.
	 */
	int dh;

	/**
	 * Convenience variable. One third of the height of a node for lattices,
	 * {@code dh/3}.
	 */
	int dh3;

	/**
	 * The diameter of nodes for networks.
	 */
	double dR;

	/**
	 * The minimum width of a node in pixels.
	 */
	static final int MIN_DW = 2;

	/**
	 * The minimum height of a node in pixels.
	 */
	static final int MIN_DH = 2;

	/**
	 * The minimum diameter of a node in pixels.
	 */
	static final double MIN_DR = 3.0;

	/**
	 * Convenience variable. The flag indicating whether the backing geometry is a
	 * hierarchical structure.
	 */
	boolean isHierarchy = false;

	/**
	 * Convenience variable. The number of hierarchical levels.
	 */
	int hLevels = 0;

	/**
	 * Convenience variable. The number of units in each hierarchical level.
	 */
	int[] hPeriods = null;

	/**
	 * Convenience variable. The gap between subsequent units in hierarchical
	 * structures.
	 */
	static final int HIERARCHY_GAP = 1; // unit gap in pixels

	/**
	 * Width reserved for the fitness legend strip.
	 */
	protected double legendReserveWidth = 0.0;

	/**
	 * Height reserved for the fitness legend strip.
	 */
	protected double legendReserveHeight = 0.0;

	/**
	 * Width of the legend labels.
	 */
	protected double legendLabelWidth = 0.0;

	@Override
	public void calcBounds(int width, int height) {
		super.calcBounds(width, height);
		clearMessage();
		noGraph = false;
		// reset sizing defaults
		dw = 0;
		dh = 0;
		dR = 0.0;
		updateLegendLayout();

		GeometryType type = geometry.getType();
		isHierarchy = (type == GeometryType.HIERARCHY);
		if (isHierarchy)
			type = ((HierarchicalGeometry) geometry).getSubType();
		if (!isHierarchy || !type.isSquareLattice())
			hPeriods = null;

		// dispatch to concise handlers for each geometry
		switch (type) {
			case CUBE:
			case LINEAR:
				handleNoRepresentation(type);
				break;
			case TRIANGULAR:
				handleTriangular(width, height);
				break;
			case HEXAGONAL:
				handleHoneycomb(width, height);
				break;
			case SQUARE_NEUMANN:
			case SQUARE_NEUMANN_2ND:
			case SQUARE_MOORE:
			case SQUARE:
				handleSquare(width, height);
				break;
			default:
				handleNetwork(width, height);
				break;
		}

		// final sanity checks
		if (dw < MIN_DW && dh < MIN_DH && dR < MIN_DR) {
			noGraph = true;
			displayMessage("Population size to large!");
		}
	}

	/**
	 * Handle geometries without graphical representation.
	 * 
	 * @param type the geometry type
	 */
	private void handleNoRepresentation(GeometryType type) {
		noGraph = true;
		displayMessage("No representation for " + type.getTitle() + "!");
	}

	/**
	 * Handle triangular lattice geometry.
	 * 
	 * @param width  the width of the canvas
	 * @param height the height of the canvas
	 */
	private void handleTriangular(int width, int height) {
		side = (int) (Math.sqrt(geometry.getSize()) + 0.5);
		double plotX = getGraphAreaX();
		double plotW = getGraphAreaWidth();
		double plotY = getGraphAreaY();
		double plotH = getGraphAreaHeight();
		int diameter = (int) Math.min(plotW, plotH);
		dw2 = diameter / (side + 3);
		int bWidth = dw2 * (side + 1);
		dw = 2 * dw2;
		dh = diameter / (side + 1);
		if (dw < MIN_DW || dh < MIN_DH) {
			bounds.setSize(width, height);
			return;
		}
		int bHeight = dh * side;
		double dx = (plotW - bWidth) * 0.5;
		double dy = (plotH - bHeight) * 0.5;
		bounds.set(plotX + dx, plotY + dy, bWidth, bHeight);
		style.showFrame = false;
	}

	/**
	 * Handle honeycomb lattice geometry.
	 * 
	 * @param width  the width of the canvas
	 * @param height the height of the canvas
	 */
	private void handleHoneycomb(int width, int height) {
		side = (int) (Math.sqrt(geometry.getSize()) + 0.5);
		double plotX = getGraphAreaX();
		double plotW = getGraphAreaWidth();
		double plotY = getGraphAreaY();
		double plotH = getGraphAreaHeight();
		int diameter = (int) Math.min(plotW, plotH);
		dw2 = diameter / (2 * side + 1);
		int bWidth = dw2 * (2 * side + 1);
		dw = 2 * dw2;
		dh3 = diameter / (3 * side + 1);
		dh = 3 * dh3;
		if (dw < MIN_DW || dh3 < Math.max(MIN_DH / 3, 1)) {
			bounds.setSize(width, height);
			return;
		}
		int bHeight = dh3 * (3 * side + 1);
		double dx = (plotW - bWidth) * 0.5;
		double dy = (plotH - bHeight) * 0.5;
		bounds.set(plotX + dx, plotY + dy, bWidth, bHeight);
		style.showFrame = false;
	}

	/**
	 * Handle square lattice geometry.
	 * 
	 * @param width  the width of the canvas
	 * @param height the height of the canvas
	 */
	private void handleSquare(int width, int height) {
		side = (int) (Math.sqrt(geometry.getSize()) + 0.5);

		int gap = 0;
		if (isHierarchy) {
			// compute hierarchy levels and periods
			int[] hierarchy = ((HierarchicalGeometry) geometry).getHierarchyLevels();
			hLevels = hierarchy.length - 1;
			if (hPeriods == null || hPeriods.length != hLevels)
				hPeriods = new int[hLevels];
			hPeriods[0] = (int) Math.sqrt(hierarchy[hLevels]);
			gap = side / hPeriods[0] - 1;
			for (int i = 1; i < hLevels; i++) {
				hPeriods[i] = hPeriods[i - 1] * (int) Math.sqrt(hierarchy[hLevels - i]);
				gap += side / hPeriods[i] - 1;
			}
			gap *= HIERARCHY_GAP;
		}

		double plotX = getGraphAreaX();
		double plotY = getGraphAreaY();
		int bWidth = (int) Math.rint(getGraphAreaWidth());
		int bHeight = (int) Math.rint(getGraphAreaHeight());
		dw = (Math.min(bWidth, bHeight) - gap) / side;
		if (dw < MIN_DW) {
			bounds.setSize(width, height);
			return;
		}
		dh = dw;
		dw2 = dw / 2;
		int newdim = dw * side + gap;
		int dx = (bWidth - newdim) / 2;
		int dy = (bHeight - newdim) / 2;
		bounds.set(plotX + dx, plotY + dy, newdim, newdim);
		style.showFrame = true;
	}

	/**
	 * Handle generic network geometry.
	 * 
	 * @param width  the width of the canvas
	 * @param height the height of the canvas
	 */
	private void handleNetwork(int width, int height) {
		double plotX = getGraphAreaX();
		double plotW = getGraphAreaWidth();
		double plotY = getGraphAreaY();
		double plotH = getGraphAreaHeight();
		int diameter = (int) Math.min(plotW, plotH);
		int radius = diameter / 2;
		dR = Math.sqrt(radius * radius * 2.0 / geometry.getSize());
		if (dR < MIN_DR) {
			bounds.setSize(width, height);
			return;
		}
		double dx = (plotW - diameter) * 0.5;
		double dy = (plotH - diameter) * 0.5;
		bounds.set(plotX + dx, plotY + dy, diameter, diameter);
		style.showFrame = false;
	}

	/**
	 * Return whether the graph should reserve space for a fitness legend.
	 *
	 * @return {@code true} if a fitness legend should be shown
	 */
	protected boolean hasLegend() {
		return view.getType() == Data.FITNESS && style.legendPos != GraphStyle.Position.NONE;
	}

	/**
	 * Collect homogeneous-state markers for the fitness legend.
	 *
	 * @return list of legend markers
	 */
	protected ArrayList<LegendMarker> collectLegendMarkers() {
		ArrayList<LegendMarker> markers = new ArrayList<>();
		Model model = view.getModel();
		if (!model.isIBS())
			return markers;
		Map2Fitness map2fit = module.getMap2Fitness();
		int id = module.getId();
		if (module instanceof Discrete && model instanceof DModel) {
			DModel dmodel = (DModel) model;
			for (int n = 0; n < module.getNTraits(); n++) {
				double mono = dmodel.getMonoScore(id, n);
				if (Double.isNaN(mono))
					continue;
				addLegendMarker(markers, map2fit.map(mono));
			}
		} else if (module instanceof Continuous && model instanceof CModel) {
			CModel cmodel = (CModel) model;
			addLegendMarker(markers, map2fit.map(cmodel.getMinMonoScore(id)));
			addLegendMarker(markers, map2fit.map(cmodel.getMaxMonoScore(id)));
		}
		return markers;
	}

	/**
	 * Add a homogeneous-state marker to the legend.
	 *
	 * @param markers list of markers
	 * @param value   the mapped fitness value
	 */
	private void addLegendMarker(ArrayList<LegendMarker> markers, double value) {
		if (!Double.isFinite(value))
			return;
		for (LegendMarker marker : markers) {
			if (Math.abs(marker.value - value) <= 1e-8 * Math.max(1.0, Math.abs(value)))
				return;
		}
		markers.add(new LegendMarker(value));
	}

	/**
	 * Draw marker ticks and labels for homogeneous-state fitness values.
	 *
	 * @param markers   the markers to draw
	 * @param min       minimum fitness on legend
	 * @param max       maximum fitness on legend
	 * @param barTop    top of legend bar
	 * @param barHeight height of legend bar
	 * @param labelX    x position of legend labels
	 */
	private void drawLegendMarkers(ArrayList<LegendMarker> markers, double min, double max, double barTop,
			double barHeight, double labelX) {
		if (markers.isEmpty())
			return;
		double range = max - min;
		for (LegendMarker marker : markers) {
			double frac = (range <= 0.0 ? 0.5 : (max - marker.value) / range);
			double y = Math.max(barTop, Math.min(barTop + barHeight, barTop + frac * barHeight));
			g.setFillStyle(style.frameColor);
			g.fillText(marker.label, labelX, y + 4.5);
		}
	}

	/**
	 * Update legend sizing based on current font metrics and model range.
	 */
	protected void updateLegendLayout() {
		legendReserveWidth = 0.0;
		legendReserveHeight = 0.0;
		legendLabelWidth = 0.0;
		Model model = view.getModel();
		if (!hasLegend() || model == null || !(getColorMap() instanceof ColorMap.Gradient1D))
			return;
		double min = model.getMinFitness(module.getId());
		double max = model.getMaxFitness(module.getId());
		if (!Double.isFinite(min) || !Double.isFinite(max))
			return;
		ArrayList<LegendMarker> markers = collectLegendMarkers();
		String font = g.getFont();
		setFont(style.ticksLabelFont);
		legendLabelWidth = Math.max(
				g.measureText(Formatter.formatPretty(max, 2)).getWidth(),
				g.measureText(Formatter.formatPretty(min, 2)).getWidth());
		for (LegendMarker marker : markers)
			legendLabelWidth = Math.max(legendLabelWidth, g.measureText(marker.label).getWidth());
		g.setFont(font);
		if (style.legendPos.isVertical()) {
			legendReserveWidth = FITNESS_LEGEND_OUTER_PAD + legendLabelWidth + FITNESS_LEGEND_LABEL_PAD
					+ FITNESS_LEGEND_BAR_WIDTH + FITNESS_LEGEND_GAP;
			double maxReserve = Math.max(0.0, bounds.getWidth() * 0.45);
			if (legendReserveWidth > maxReserve) {
				legendReserveWidth = maxReserve;
				legendLabelWidth = Math.max(0.0,
						legendReserveWidth - FITNESS_LEGEND_OUTER_PAD - FITNESS_LEGEND_LABEL_PAD
								- FITNESS_LEGEND_BAR_WIDTH - FITNESS_LEGEND_GAP);
			}
			return;
		}
		legendReserveHeight = FITNESS_LEGEND_GAP + FITNESS_LEGEND_BAR_WIDTH + 14.0;
	}

	/**
	 * Get the width available for the graph after reserving legend space.
	 *
	 * @return graph area width
	 */
	protected double getGraphAreaWidth() {
		return Math.max(0.0, bounds.getWidth() - (style.legendPos.isVertical() ? legendReserveWidth : 0.0));
	}

	/**
	 * Get the left edge of the graph area after reserving legend space.
	 *
	 * @return graph area x-coordinate
	 */
	protected double getGraphAreaX() {
		if (!style.legendPos.isVertical())
			return bounds.getX();
		return bounds.getX() + (style.legendPos == GraphStyle.Position.WEST ? legendReserveWidth : 0.0);
	}

	/**
	 * Get the height available for the graph after reserving legend space.
	 *
	 * @return graph area height
	 */
	protected double getGraphAreaHeight() {
		return Math.max(0.0, bounds.getHeight() - (style.legendPos.isHorizontal() ? legendReserveHeight : 0.0));
	}

	/**
	 * Get the top edge of the graph area after reserving legend space.
	 *
	 * @return graph area y-coordinate
	 */
	protected double getGraphAreaY() {
		if (!style.legendPos.isHorizontal())
			return bounds.getY();
		return bounds.getY() + (style.legendPos == GraphStyle.Position.NORTH ? legendReserveHeight : 0.0);
	}

	/**
	 * Legend entry for homogeneous-state fitness values.
	 */
	protected static class LegendMarker {

		/**
		 * The fitness value associated with the marker.
		 */
		final double value;

		/**
		 * The formatted value shown in the legend.
		 */
		final String label;

		/**
		 * Create a new marker.
		 *
		 * @param value the fitness value
		 */
		LegendMarker(double value) {
			this.value = value;
			label = Formatter.formatPretty(value, 2);
		}
	}

	/**
	 * Get the color of the node at index {@code node} as a CSS color string.
	 * 
	 * @param node the index of the node
	 * @return the color of the node
	 */
	@Override
	public String getCSSColorAt(int node) {
		return data[node];
	}

	/**
	 * Find the index of the node at the location with coordinates {@code (x, y)}.
	 * 
	 * @param x the {@code x}-coordinate of the location
	 * @param y the {@code y}-coordinate of the location
	 * @return the index of the node
	 */
	@Override
	public int findNodeAt(int x, int y) {
		// no network may have been initialized (e.g. for ODE/SDE models)
		if (hasMessage || network == null || invalidated || network.isStatus(Status.LAYOUT_IN_PROGRESS))
			return FINDNODEAT_OUT_OF_BOUNDS;

		// some heuristic adjustments... cause remains mysterious
		x = x - (int) (style.frameWidth * zoomFactor + 0.5);
		y = y - (int) (style.frameWidth * zoomFactor - 0.5);
		double sx = (viewCorner.getX() + x - bounds.getX()) / zoomFactor;
		double sy = (viewCorner.getY() + y - bounds.getY()) / zoomFactor;
		if (sx < 0.0 || sx > bounds.getWidth() || sy < 0.0 || sy > bounds.getHeight())
			return FINDNODEAT_OUT_OF_BOUNDS;
		int isx = (int) (sx + 0.5);
		int isy = (int) (sy + 0.5);

		GeometryType type = geometry.getType();
		if (isHierarchy)
			type = ((HierarchicalGeometry) geometry).getSubType();

		switch (type) {
			// 3D views must deal with this
			case CUBE:
			case LINEAR:
				return FINDNODEAT_UNIMPLEMENTED;
			case TRIANGULAR:
				return findTriangularNode(isx, isy);
			case HEXAGONAL:
				return findHoneycombNode(isx, isy);
			case SQUARE_NEUMANN:
			case SQUARE_NEUMANN_2ND:
			case SQUARE_MOORE:
			case SQUARE:
				return findSquareNode(isx, isy);
			default:
				return findNetworkNode(isx, isy);
		}
	}

	/**
	 * Find the index of the node at the location with coordinates {@code (x, y)} in
	 * a triangular lattice.
	 * 
	 * @param x the {@code x}-coordinate of the location
	 * @param y the {@code y}-coordinate of the location
	 * @return the index of the node
	 */
	private int findTriangularNode(int x, int y) {
		int r = y / dh;
		int c = x / dw2;
		int rx = y - r * dh;
		int cx = x - c * dw2;
		if (c % 2 + r % 2 == 1)
			rx = dh - rx;
		double loc = (double) rx / (double) dh + (double) cx / (double) dw2;
		if ((c == 0 && loc < 1.0) || (c == side && loc > 1.0))
			return FINDNODEAT_OUT_OF_BOUNDS;
		if (loc < 1.0)
			c--;
		return r * side + c;
	}

	/**
	 * Find the index of the node at the location with coordinates {@code (x, y)} in
	 * a honeycomb lattice.
	 * 
	 * @param x the {@code x}-coordinate of the location
	 * @param y the {@code y}-coordinate of the location
	 * @return the index of the node
	 */
	private int findHoneycombNode(int x, int y) {
		int r = y / dh;
		int odd = r % 2;
		int c = (x - odd * dw2) / dw;
		int rx = y - r * dh;
		int cx = x - odd * dw2 - c * dw;
		if (cx < 0) {
			cx += dw; // make sure cx>0
			c--;
		}
		if (rx < dh3) {
			double loc;
			if (cx > dw2) {
				loc = (double) rx / (double) dh3 + (double) (dw - cx) / (double) dw2;
				if (loc < 1.0) {
					c += odd;
					r--;
				}
			} else {
				loc = (double) rx / (double) dh3 + (double) cx / (double) dw2;
				if (loc < 1.0) {
					c -= 1 - odd;
					r--;
				}
			}
		}
		if (r < 0 || c < 0 || r == side || c == side)
			return FINDNODEAT_OUT_OF_BOUNDS;
		return r * side + c;
	}

	/**
	 * Find the index of the node at the location with coordinates {@code (x, y)} in
	 * a (hierarchical) square lattice.
	 * 
	 * @param x the {@code x}-coordinate of the location
	 * @param y the {@code y}-coordinate of the location
	 * @return the index of the node
	 */
	private int findSquareNode(int x, int y) {
		// node 0 in lower left corner
		int c = x / dw;
		int r = side - 1 - y / dh;
		if (isHierarchy) {
			int wgap = 0;
			int hgap = 0;
			for (int i = 0; i < hLevels; i++) {
				wgap += c / hPeriods[i]; // gaps left of mouse
				hgap += (side - 1 - r) / hPeriods[i];
			}
			c = (x - wgap * HIERARCHY_GAP) / dw;
			r = side - 1 - (y - hgap * HIERARCHY_GAP) / dh;
		}
		return r * side + c;
	}

	/**
	 * Locate the node under the given screen coordinates for general network
	 * layouts.
	 * 
	 * @param sx screen x-coordinate
	 * @param sy screen y-coordinate
	 * @return node index or {@code -1} if none hit
	 */
	private int findNetworkNode(int sx, int sy) {
		// note: cannot check bounds (or anything else) to rule out that mouse hovers
		// over node because nodes may have been manually shifted.
		double rr = network.getRadius();
		double iscale = (rr + rr) / bounds.getWidth();
		Point2D mousecoord = new Point2D(sx * iscale - rr, sy * iscale - rr);
		Node2D[] nodes = network.toArray();
		// in the undesirable case that nodes overlap, nodes with a higher index are
		// drawn later (on top)
		// in order to get the top node we need to start from the back
		for (int k = network.size() - 1; k >= 0; k--) {
			if (nodes[k].isHit(mousecoord))
				return k;
		}
		return FINDNODEAT_OUT_OF_BOUNDS;
	}

	/**
	 * The flag to indicate whether {@link #hitNode} is being dragged.
	 */
	protected boolean hitDragged = false;

	/**
	 * {@inheritDoc}
	 * <p>
	 * If a node has been hit by a left-click, remember the node's index and the
	 * current mouse coordinates. This information might be used by subsequent
	 * {@link MouseMoveEvent}s.
	 * 
	 * <h3>CSS Style Rules</h3>
	 * <dl>
	 * <dt>.evoludo-cursorGrabNode</dt>
	 * <dd>added to graph element.</dd>
	 * </dl>
	 * 
	 * @see #onMouseMove(MouseMoveEvent)
	 */
	@Override
	public void onMouseDown(MouseDownEvent event) {
		// super sets mouse coordinates and leftMouseButton when inside
		super.onMouseDown(event);
		if (!leftMouseButton) {
			hitNode = -1;
			element.removeClassName(CURSOR_GRAB_NODE_CLASS);
			return;
		}
		hitNode = findNodeAt(mouseX, mouseY);
		if (hitNode >= 0) {
			element.addClassName(CURSOR_GRAB_NODE_CLASS);
		} else {
			element.removeClassName(CURSOR_GRAB_NODE_CLASS);
		}
	}

	/**
	 * {@inheritDoc}
	 * <dl>
	 * <dt>.evoludo-cursorGrabNode</dt>
	 * <dd>removed from graph element.</dd>
	 * <dt>.evoludo-cursorMoveNode</dt>
	 * <dd>removed from graph element.</dd>
	 * </dl>
	 * 
	 * @see #onMouseMove(MouseMoveEvent)
	 */
	@Override
	public void onMouseUp(MouseUpEvent event) {
		super.onMouseUp(event);
		if (event.getNativeButton() == NativeEvent.BUTTON_LEFT) {
			if (hitDragged) {
				element.removeClassName(CURSOR_MOVE_NODE_CLASS);
				// reshow tooltip after dragging
				hitDragged = false;
				tooltip.update();
			}
			hitNode = -1;
			element.removeClassName(CURSOR_GRAB_NODE_CLASS);
		}
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * If a node has been hit by a {@link MouseDownEvent} and is being dragged,
	 * the position of the node is shifted. This allows to customize the display of
	 * the graph.
	 * 
	 * <h3>CSS Style Rules</h3>
	 * <dl>
	 * <dt>.evoludo-cursorMoveNode</dt>
	 * <dd>added to graph element when dragging a node.</dd>
	 * </dl>
	 * 
	 * @see #onMouseDown(MouseDownEvent)
	 */
	@Override
	public void onMouseMove(MouseMoveEvent event) {
		if (hitNode >= 0) {
			event.preventDefault();
			int x = event.getX();
			int y = event.getY();
			if (!inside(x, y)) {
				element.removeClassName("evoludo-cursorMoveNode");
				mouseX = -Integer.MAX_VALUE;
				mouseY = -Integer.MAX_VALUE;
				return;
			}
			if (mouseX == -Integer.MAX_VALUE || mouseY == -Integer.MAX_VALUE) {
				mouseX = x;
				mouseY = y;
				return;
			}
			hitDragged = true;
			element.addClassName("evoludo-cursorMoveNode");
			shiftNodeBy(hitNode, mouseX - x, mouseY - y);
			mouseX = x;
			mouseY = y;
			return;
		}
		super.onMouseMove(event);
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * The number of touches on the graph changed.
	 */
	@Override
	public void onTouchEnd(TouchEndEvent event) {
		super.onTouchEnd(event);
		JsArray<Touch> touches = event.getTouches();
		switch (touches.length()) {
			case 1: // down from two touches
				Touch touch = touches.get(0);
				int x = touch.getRelativeX(element);
				int y = touch.getRelativeY(element);
				// remember position of touch to prevent jumps
				mouseX = x;
				mouseY = y;
				break;
			case 0: // no more touches remaining - reset values
				hitNode = -1;
				break;
			default:
		}
	}

	/**
	 * {@inheritDoc}
	 * <dl>
	 * <dt>move one finger on node
	 * <dd>shift node.
	 * </dl>
	 */
	@Override
	public void onTouchMove(TouchMoveEvent event) {
		JsArray<Touch> touches = event.getTouches();
		if (touches.length() == 1 && hitNode >= 0) {
			// shift node
			Touch touch = touches.get(0);
			int x = touch.getRelativeX(element);
			int y = touch.getRelativeY(element);
			if (!inside(x, y)) {
				mouseX = -Integer.MAX_VALUE;
				mouseY = -Integer.MAX_VALUE;
				return;
			}
			if (mouseX == -Integer.MAX_VALUE || mouseY == -Integer.MAX_VALUE) {
				mouseX = x;
				mouseY = y;
				return;
			}
			// shift position of node
			shiftNodeBy(hitNode, mouseX - x, mouseY - y);
			mouseX = x;
			mouseY = y;
			event.preventDefault();
			return;
		}
		super.onTouchMove(event);
	}

	@Override
	protected boolean inside(int x, int y) {
		// heuristic offset aligns with findNodeAt bounds checks
		int adjX = x - (int) (style.frameWidth * zoomFactor + 0.5);
		int adjY = y - (int) (style.frameWidth * zoomFactor - 0.5);
		double sx = (viewCorner.getX() + adjX - bounds.getX()) / zoomFactor;
		double sy = (viewCorner.getY() + adjY - bounds.getY()) / zoomFactor;
		if (sy < 0.0 || sy > bounds.getHeight())
			return hasLegend() && legendReserveHeight > 0.0 && style.legendPos.isHorizontal()
					&& (style.legendPos == GraphStyle.Position.SOUTH
							? sy <= bounds.getHeight() + legendReserveHeight && sy >= 0.0
							: sy >= -legendReserveHeight && sy <= bounds.getHeight());
		if (sx >= 0.0 && sx <= bounds.getWidth())
			return true;
		return hasLegend() && legendReserveWidth > 0.0 && style.legendPos.isVertical()
				&& (style.legendPos == GraphStyle.Position.EAST
						? sx <= bounds.getWidth() + legendReserveWidth && sx >= 0.0
						: sx >= -legendReserveWidth && sx <= bounds.getWidth());
	}

	/**
	 * Shift a single node with index {@code nodeidx} by {@code (dx, dy)}. Positive
	 * {@code dx} shift the node to the right and positive {@code dy} shift upwards.
	 * 
	 * @param nodeidx the index of the node
	 * @param dx      the horizontal shift of the node
	 * @param dy      the vertical shift of the node
	 */
	public void shiftNodeBy(int nodeidx, int dx, int dy) {
		if (nodeidx < 0) {
			// invalid node
			return;
		}
		switch (network.getStatus()) {
			case HAS_LAYOUT:
				double rr = network.getRadius();
				// bounds.width==bounds.height for networks
				double iscale = (rr + rr) / (bounds.getWidth() * zoomFactor);
				double xaspect;
				double yaspect;
				int width = getOffsetWidth();
				int height = getOffsetHeight();
				if (width < height) {
					// portrait
					xaspect = 1.0;
					yaspect = (double) height / width;
				} else {
					// landscape
					xaspect = (double) width / height;
					yaspect = 1.0;
				}
				Node2D node = network.get(nodeidx);
				double r = node.getR();
				node.set(Math.max(-rr * xaspect + r, Math.min(rr * xaspect - r, node.getX() - dx * iscale)),
						Math.max(-rr * yaspect + r, Math.min(rr * yaspect - r, node.getY() - dy * iscale)));
				network.linkNodes();
				drawNetwork();
				return;
			case NO_LAYOUT:
				// lattices - shift view instead
				shift(dx, dy);
				return;
			default:
				// nothing (yet) to shift
				return;
		}
	}

	@Override
	public void export(MyContext2d ctx) {
		MyContext2d bak = g;
		g = ctx;
		paint(true);
		g = bak;
	}
}
