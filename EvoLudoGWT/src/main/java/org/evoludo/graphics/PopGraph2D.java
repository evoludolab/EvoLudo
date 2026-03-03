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

import org.evoludo.geom.Node2D;
import org.evoludo.geom.Path2D;
import org.evoludo.geom.Point2D;
import org.evoludo.graphics.AbstractGraph.Shifting;
import org.evoludo.simulator.ColorMap;
import org.evoludo.simulator.ColorMapCSS;
import org.evoludo.simulator.Network.Status;
import org.evoludo.simulator.Network2D;
import org.evoludo.simulator.geometries.AbstractGeometry;
import org.evoludo.simulator.geometries.GeometryType;
import org.evoludo.simulator.geometries.HierarchicalGeometry;
import org.evoludo.simulator.modules.Module;
import org.evoludo.simulator.views.AbstractView;
import org.evoludo.simulator.views.BasicTooltipProvider;
import org.evoludo.simulator.views.Distribution;
import org.evoludo.ui.ContextMenu;
import org.evoludo.ui.ContextMenuRadioItem;
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
	 * Message shown when the selected binning cannot be rendered in the available
	 * graph area.
	 */
	private static final String INFO_BINNING_TOO_FINE = "Binning too fine!";

	/**
	 * Message shown when the selected binning cannot be rendered in the available
	 * graph area.
	 */
	private static final String INFO_POPSIZE_TOO_BIG = "Population size too big!";

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

	/**
	 * Add submenu to select the legend position.
	 *
	 * @param menu the context menu to populate
	 */
	private void addLegendPositionMenu(ContextMenu menu) {
		if (legendSpecs.mode == LegendSpecs.Mode.NONE)
			return;
		ContextMenu legendMenu = new ContextMenu(menu);
		addLegendPositionItem(legendMenu, "None", GraphStyle.Position.NONE);
		addLegendPositionItem(legendMenu, "Right", GraphStyle.Position.EAST);
		addLegendPositionItem(legendMenu, "Bottom", GraphStyle.Position.SOUTH);
		addLegendPositionItem(legendMenu, "Left", GraphStyle.Position.WEST);
		addLegendPositionItem(legendMenu, "Top", GraphStyle.Position.NORTH);
		menu.add("Legend", legendMenu);
	}

	/**
	 * Add radio item for selecting legend position.
	 *
	 * @param menu     submenu to populate
	 * @param label    item label
	 * @param position target legend position
	 */
	private void addLegendPositionItem(ContextMenu menu, String label, GraphStyle.Position position) {
		ContextMenuRadioItem item = new ContextMenuRadioItem(label, () -> {
			if (style.legendPos == position)
				return;
			style.legendPos = position;
			onResize();
			if (!hasMessage)
				paint(true);
		});
		item.setSelected(style.legendPos == position);
		menu.add(item);
	}

	@Override
	protected void addZoomMenu(ContextMenu menu) {
		super.addZoomMenu(menu);
		// piggy-back on zoom menu... a bit hackish...
		addLegendPositionMenu(menu);
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
		drawLegend();
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
		drawLegend();
		g.restore();
	}

	/**
	 * Draw the active legend.
	 */
	protected void drawLegend() {
		if (!hasLegend() || (legendReserveWidth <= 0.0 && legendReserveHeight <= 0.0))
			return;
		switch (legendSpecs.mode) {
			case FITNESS_GRADIENT:
			case DENSITY_GRADIENT:
				drawGradientLegend();
				return;
			case CONTINUOUS_TRAIT:
				drawGradientLegend((ColorMap.Gradient1D<String>) getColorMap(), legendSpecs.min, legendSpecs.max,
						legendSpecs.markers, formatGradientLegendValue(legendSpecs.min),
						formatGradientLegendValue(legendSpecs.max));
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
	 * Draw frame and legend overlays without the content clip. Subclasses that keep
	 * content pan/zoom separate can reuse this after mapping viewport ranges.
	 *
	 * @param decoratedFrame {@code true} to draw the decorated frame
	 */
	protected void drawFrameOverlay(boolean decoratedFrame) {
		g.save();
		g.scale(scale, scale);
		g.translate(bounds.getX(), bounds.getY());
		drawFrame(decoratedFrame ? 4 : 0, decoratedFrame ? 4 : 0);
		g.restore();
		g.save();
		g.scale(scale, scale);
		g.translate(bounds.getX() - viewCorner.getX(), bounds.getY() - viewCorner.getY());
		g.scale(zoomFactor, zoomFactor);
		drawLegend();
		g.restore();
	}

	/**
	 * Draw a color legend for gradient-based population graphs.
	 */
	private void drawGradientLegend() {
		if (!(getColorMap() instanceof ColorMap.Gradient1D))
			return;
		double min = legendSpecs.min;
		double max = legendSpecs.max;
		if (!Double.isFinite(min) || !Double.isFinite(max))
			return;
		double[] markers = legendSpecs.markers;
		drawGradientLegend((ColorMap.Gradient1D<String>) getColorMap(), min, max, markers,
				formatGradientLegendValue(min), formatGradientLegendValue(max));
	}

	/**
	 * Draw a continuous sampled legend with min/max endpoint labels.
	 *
	 * @param gradient the color map to sample
	 * @param min      minimum legend value
	 * @param max      maximum legend value
	 * @param markers  any marker annotations to include
	 * @param minLabel label for the minimum value
	 * @param maxLabel label for the maximum value
	 */
	private void drawGradientLegend(ColorMap.Gradient1D<String> gradient, double min, double max,
			double[] markers, String minLabel, String maxLabel) {
		boolean vertical = style.legendPos.isVertical();
		LegendLayout layout = getLegendLayout(vertical ? LegendLayout.BAR_WIDTH : bounds.getWidth() * 0.6,
				vertical ? bounds.getHeight() * 0.6 : LegendLayout.BAR_WIDTH);
		if (layout == null)
			return;
		double[] samples = getLegendSamples(min, max, vertical ? layout.barHeight : layout.barWidth, markers);
		drawGradientLegendBands(layout, samples, gradient);
		drawLegendFrame(layout);
		g.setFillStyle(style.frameColor);
		setFont(style.ticksLabelFont);
		if (vertical) {
			g.fillText(maxLabel, getLegendLabelX(maxLabel, layout), layout.barY + 4.5);
			g.fillText(minLabel, getLegendLabelX(minLabel, layout), layout.barY + layout.barHeight + 4.5);
			drawLegendMarkers(markers, min, max, layout);
			return;
		}
		double labelY = getHorizontalLegendLabelY(layout, LegendLayout.BAR_WIDTH + 12.5, -style.minPadding);
		g.fillText(minLabel, layout.barX, labelY);
		g.fillText(maxLabel, layout.barX + layout.barWidth - g.measureText(maxLabel).getWidth(), labelY);
	}

	/**
	 * Fill a sampled gradient legend.
	 *
	 * @param layout   legend layout
	 * @param samples  values mapped onto the bar
	 * @param gradient color map used to render the samples
	 */
	private void drawGradientLegendBands(LegendLayout layout, double[] samples, ColorMap.Gradient1D<String> gradient) {
		boolean vertical = style.legendPos.isVertical();
		int steps = samples.length;
		double stepSpan = (vertical ? layout.barHeight : layout.barWidth) / steps;
		for (int i = 0; i < steps; i++) {
			double start = Math.floor((vertical ? layout.barY : layout.barX) + i * stepSpan);
			double end = Math.floor((vertical ? layout.barY : layout.barX) + (i + 1) * stepSpan);
			if (i == steps - 1)
				end = vertical ? layout.barY + layout.barHeight : layout.barX + layout.barWidth;
			if (end <= start)
				end = start + 1.0;
			g.setFillStyle(gradient.translate(samples[i]));
			if (vertical) {
				fillRect(layout.barX, start, layout.barWidth, end - start);
				continue;
			}
			fillRect(start, layout.barY, end - start, layout.barHeight);
		}
	}

	/**
	 * Draw the outline around a legend color bar.
	 *
	 * @param layout the legend layout
	 */
	private void drawLegendFrame(LegendLayout layout) {
		g.setStrokeStyle(style.frameColor);
		g.setLineWidth(1.0);
		if (style.legendPos.isVertical()) {
			g.strokeRect(layout.barX + 0.5, layout.barY - 0.5, layout.barWidth, layout.barHeight + 1.0);
			return;
		}
		g.strokeRect(layout.barX - 0.5, layout.barY + 0.5, layout.barWidth + 1.0, layout.barHeight);
	}

	/**
	 * Draw a segmented legend for discrete traits.
	 */
	private void drawDiscreteTraitLegend() {
		String[] colors = getDiscreteTraitLegendColors();
		String[] labels = legendSpecs.discreteLabels;
		if (labels.length == 0)
			labels = null;
		if (colors == null || labels == null)
			return;
		int nSegments = Math.min(colors.length, labels.length);
		if (nSegments <= 0)
			return;
		g.setStrokeStyle(style.frameColor);
		g.setLineWidth(1.0);
		g.setFillStyle(style.frameColor);
		setFont(style.ticksLabelFont);
		if (style.legendPos.isVertical()) {
			LegendLayout layout = getLegendLayout(LegendLayout.BAR_WIDTH, bounds.getHeight() * 0.6);
			if (layout == null)
				return;
			double stepHeight = layout.barHeight / nSegments;
			for (int i = 0; i < nSegments; i++) {
				double y0 = Math.floor(layout.barY + i * stepHeight);
				double y1 = Math.floor(layout.barY + (i + 1) * stepHeight);
				if (i == nSegments - 1)
					y1 = layout.barY + layout.barHeight;
				if (y1 <= y0)
					y1 = y0 + 1.0;
				g.setFillStyle(colors[i]);
				fillRect(layout.barX, y0, layout.barWidth, y1 - y0);
				g.setFillStyle(style.frameColor);
				g.fillText(labels[i], getLegendLabelX(labels[i], layout), 0.5 * (y0 + y1) + 4.5);
			}
			drawLegendFrame(layout);
			return;
		}
		double barWidth = getHorizontalDiscreteTraitLegendBarWidth(labels);
		// increase legend bar height to overlay trait names
		double barHeight = Math.max(LegendLayout.BAR_WIDTH, 16.0);
		LegendLayout layout = getLegendLayout(barWidth, barHeight);
		if (layout == null)
			return;
		double stepWidth = layout.barWidth / nSegments;
		for (int i = 0; i < nSegments; i++) {
			double x0 = Math.floor(layout.barX + i * stepWidth);
			double x1 = Math.floor(layout.barX + (i + 1) * stepWidth);
			if (i == nSegments - 1)
				x1 = layout.barX + layout.barWidth;
			if (x1 <= x0)
				x1 = x0 + 1.0;
			g.setFillStyle(colors[i]);
			fillRect(x0, layout.barY, x1 - x0, layout.barHeight);
			g.setFillStyle(ColorMapCSS.luminance(colors[i]) > 0.55 ? "#000000" : "#ffffff");
			double textWidth = g.measureText(labels[i]).getWidth();
			g.fillText(labels[i], x0 + 0.5 * (x1 - x0 - textWidth), layout.barY + 0.5 * (layout.barHeight + 9.0));
		}
		drawLegendFrame(layout);
	}

	@Override
	public String getTooltipAt(int x, int y) {
		if (bounds.contains(x, y))
			return super.getTooltipAt(x, y);
		return getLegendTooltipAt(x, y);
	}

	/**
	 * Get the tooltip for the active legend at screen coordinates {@code (x,y)}.
	 *
	 * @param x the screen x-coordinate
	 * @param y the screen y-coordinate
	 * @return tooltip HTML or {@code null}
	 */
	private String getLegendTooltipAt(int x, int y) {
		element.removeClassName(EVOLUDO_CURSOR_NODE);
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
	 * Get the tooltip for the active gradient legend at screen coordinates
	 * {@code (x,y)}.
	 *
	 * @param x the screen x-coordinate
	 * @param y the screen y-coordinate
	 * @return tooltip HTML or {@code null}
	 */
	private String getGradientLegendTooltipAt(int x, int y) {
		if (!(getColorMap() instanceof ColorMap.Gradient1D))
			return null;
		double min = legendSpecs.min;
		double max = legendSpecs.max;
		if (!Double.isFinite(min) || !Double.isFinite(max))
			return null;
		double value = getGradientLegendValueAt(x, y, min, max);
		if (!Double.isFinite(value))
			return null;
		String color = ((ColorMap.Gradient1D<String>) getColorMap()).translate(value);
		String label;
		switch (legendSpecs.mode) {
			case FITNESS_GRADIENT:
				label = "fitness";
				break;
			case DENSITY_GRADIENT:
				label = "frequency";
				break;
			default:
				label = "value";
				break;
		}
		return label + ": " + BasicTooltipProvider.SPAN_COLOR + color
				+ BasicTooltipProvider.TABLE_CELL_BULLET
				+ formatGradientLegendValue(value);
	}

	/**
	 * Get the tooltip for the trait legend at screen coordinates {@code (x,y)}.
	 *
	 * @param x the screen x-coordinate
	 * @param y the screen y-coordinate
	 * @return tooltip HTML or {@code null}
	 */
	private String getTraitLegendTooltipAt(int x, int y) {
		String label = legendSpecs.tooltipLabel == null ? "trait" : legendSpecs.tooltipLabel;
		switch (legendSpecs.mode) {
			case CONTINUOUS_TRAIT:
				ColorMap.Gradient1D<String> gradient = (ColorMap.Gradient1D<String>) getColorMap();
				double value = getGradientLegendValueAt(x, y, legendSpecs.min, legendSpecs.max);
				if (!Double.isFinite(value))
					return null;
				String color = gradient.translate(value);
				return label + ": " + BasicTooltipProvider.SPAN_COLOR + color + BasicTooltipProvider.TABLE_CELL_BULLET
						+ Formatter.formatPretty(value, 2);
			case DISCRETE_TRAIT:
				String[] colors = getDiscreteTraitLegendColors();
				String[] labels = legendSpecs.discreteLabels;
				if (labels.length == 0)
					labels = null;
				if (colors == null || labels == null)
					return null;
				int nSegments = Math.min(colors.length, labels.length);
				LegendLayout layout = style.legendPos.isVertical()
						? getLegendLayout(LegendLayout.BAR_WIDTH, bounds.getHeight() * 0.6)
						: getLegendLayout(getHorizontalDiscreteTraitLegendBarWidth(labels),
								Math.max(LegendLayout.BAR_WIDTH, 16.0));
				int idx = getLegendBandIndexAt(x, y, nSegments, layout);
				if (idx < 0)
					return null;
				return label + ": " + BasicTooltipProvider.SPAN_COLOR + colors[idx]
						+ BasicTooltipProvider.TABLE_CELL_BULLET + labels[idx];
			case NONE:
			case FITNESS_GRADIENT:
			case DENSITY_GRADIENT:
			default:
				return null;
		}
	}

	/**
	 * Get the continuous legend value at screen coordinates {@code (x,y)}.
	 *
	 * @param x   the screen x-coordinate
	 * @param y   the screen y-coordinate
	 * @param min minimum legend value
	 * @param max maximum legend value
	 * @return legend value, or {@link Double#NaN} if outside the legend
	 */
	private double getGradientLegendValueAt(int x, int y, double min, double max) {
		LegendLayout layout = getLegendLayout(
				style.legendPos.isVertical() ? LegendLayout.BAR_WIDTH : bounds.getWidth() * 0.6,
				style.legendPos.isVertical() ? bounds.getHeight() * 0.6 : LegendLayout.BAR_WIDTH);
		if (layout == null)
			return Double.NaN;
		double fraction = getLegendAxisFractionAt(x, y, layout);
		if (!Double.isFinite(fraction))
			return Double.NaN;
		return max - fraction * (max - min);
	}

	/**
	 * Convert screen coordinates to a normalized position on a legend.
	 *
	 * @param x      screen x-coordinate
	 * @param y      screen y-coordinate
	 * @param layout legend layout
	 * @return normalized legend position in {@code [0,1]}, or {@link Double#NaN} if
	 *         outside
	 */
	private double getLegendAxisFractionAt(int x, int y, LegendLayout layout) {
		if (layout == null)
			return Double.NaN;
		double sx = (viewCorner.getX() + x - bounds.getX()) / zoomFactor;
		double sy = (viewCorner.getY() + y - bounds.getY()) / zoomFactor;
		if (sx < layout.barX || sx > layout.barX + layout.barWidth || sy < layout.barY
				|| sy > layout.barY + layout.barHeight)
			return Double.NaN;
		if (style.legendPos.isVertical())
			return Math.max(0.0, Math.min(1.0, (sy - layout.barY) / layout.barHeight));
		return Math.max(0.0, Math.min(1.0, (sx - layout.barX) / layout.barWidth));
	}

	/**
	 * Convert screen coordinates to the corresponding band index on a legend.
	 *
	 * @param x      screen x-coordinate
	 * @param y      screen y-coordinate
	 * @param nBands number of legend bands
	 * @param layout legend layout
	 * @return band index, or {@code -1} if outside the legend
	 */
	private int getLegendBandIndexAt(int x, int y, int nBands, LegendLayout layout) {
		double fraction = getLegendAxisFractionAt(x, y, layout);
		if (!Double.isFinite(fraction))
			return -1;
		return Math.min(nBands - 1, Math.max(0, (int) (fraction * nBands)));
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
	private double[] getLegendSamples(double min, double max, double height, double[] markers) {
		int steps = Math.max(2, (int) Math.floor(height / LegendLayout.MIN_BAND_HEIGHT));
		double[] samples = new double[steps];
		for (int i = 0; i < steps; i++)
			samples[i] = min + (1.0 - (double) i / Math.max(1, steps - 1)) * (max - min);
		double range = max - min;
		if (range <= 0.0 || markers.length == 0)
			return samples;
		boolean[] used = new boolean[steps];
		for (double marker : markers) {
			int target = (int) Math.rint((max - marker) / range * (steps - 1));
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
	 * Width removed from the graph area for a vertical legend, excluding any
	 * y-axis decorations already reserved by {@link AbstractGraph}.
	 */
	protected double legendGraphReserveWidth = 0.0;

	/**
	 * Height reserved for the fitness legend strip.
	 */
	protected double legendReserveHeight = 0.0;

	/**
	 * Height removed from the graph area for a horizontal legend, excluding any
	 * x-axis decorations already reserved by {@link AbstractGraph}.
	 */
	protected double legendGraphReserveHeight = 0.0;

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
				return;
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
			if (view instanceof Distribution)
				displayMessage(INFO_BINNING_TOO_FINE);
			else
				displayMessage(INFO_POPSIZE_TOO_BIG);
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
	 * Return whether a legend can be shown for the current graph data.
	 *
	 * @return {@code true} if a legend should be shown
	 */
	protected boolean hasLegend() {
		return style.legendPos != GraphStyle.Position.NONE && legendSpecs.mode != LegendSpecs.Mode.NONE;
	}

	/**
	 * Format a value shown on the active gradient legend.
	 *
	 * @param value the legend value
	 * @return formatted label
	 */
	protected String formatGradientLegendValue(double value) {
		return (legendSpecs.inPercent ? Formatter.formatPercent(value, 1) : Formatter.formatPretty(value, 2));
	}

	/**
	 * Draw marker ticks and labels for homogeneous-state fitness values.
	 *
	 * @param markers the markers to draw
	 * @param min     minimum fitness on legend
	 * @param max     maximum fitness on legend
	 * @param layout  legend layout
	 */
	private void drawLegendMarkers(double[] markers, double min, double max, LegendLayout layout) {
		if (markers.length == 0)
			return;
		double range = max - min;
		for (double marker : markers) {
			double frac = (range <= 0.0 ? 0.5 : (max - marker) / range);
			double y = Math.max(layout.barY,
					Math.min(layout.barY + layout.barHeight, layout.barY + frac * layout.barHeight));
			g.setFillStyle(style.frameColor);
			String label = Formatter.formatPretty(marker, 2);
			g.fillText(label, getLegendLabelX(label, layout), y + 4.5);
		}
	}

	/**
	 * Get the x-coordinate for drawing vertical legend text.
	 *
	 * @param label  the legend label
	 * @param layout the legend layout
	 * @return x-coordinate for drawing the label
	 */
	private double getLegendLabelX(String label, LegendLayout layout) {
		double labelX = (style.legendPos == GraphStyle.Position.EAST
				? layout.barX + layout.barWidth + getLegendLabelPad(style.legendPos)
				: -legendReserveWidth + style.minPadding);
		if (style.legendPos != GraphStyle.Position.WEST)
			return labelX;
		return labelX + legendLabelWidth - g.measureText(label).getWidth();
	}

	/**
	 * Get the y-coordinate for horizontal legend labels.
	 *
	 * @param layout        the legend layout
	 * @param southLabelGap label offset below the bar for south legends
	 * @param northLabelY   label y-position for north legends
	 * @return y-coordinate for drawing the labels
	 */
	private double getHorizontalLegendLabelY(LegendLayout layout, double southLabelGap, double northLabelY) {
		if (style.legendPos == GraphStyle.Position.SOUTH)
			return layout.barY + southLabelGap;
		switch (legendSpecs.mode) {
			case FITNESS_GRADIENT:
			case DENSITY_GRADIENT:
				return layout.barY + layout.barHeight + 12.5;
			default:
				return northLabelY;
		}
	}

	/**
	 * Update legend sizing based on current font metrics and model range.
	 */
	protected void updateLegendLayout() {
		legendReserveWidth = 0.0;
		legendGraphReserveWidth = 0.0;
		legendReserveHeight = 0.0;
		legendGraphReserveHeight = 0.0;
		legendLabelWidth = 0.0;
		if (!hasLegend())
			return;
		LegendSpecs.Mode mode = legendSpecs.mode;
		String font = g.getFont();
		setFont(style.ticksLabelFont);
		switch (mode) {
			case FITNESS_GRADIENT:
			case DENSITY_GRADIENT:
				double min = legendSpecs.min;
				double max = legendSpecs.max;
				if (!Double.isFinite(min) || !Double.isFinite(max)) {
					g.setFont(font);
					return;
				}
				legendLabelWidth = Math.max(
						g.measureText(formatGradientLegendValue(max)).getWidth(),
						g.measureText(formatGradientLegendValue(min)).getWidth());
				for (double marker : legendSpecs.markers)
					legendLabelWidth = Math.max(legendLabelWidth,
							g.measureText(Formatter.formatPretty(marker, 2)).getWidth());
				break;
			case CONTINUOUS_TRAIT:
				legendLabelWidth = Math.max(g.measureText(formatGradientLegendValue(legendSpecs.min)).getWidth(),
						g.measureText(formatGradientLegendValue(legendSpecs.max)).getWidth());
				break;
			case DISCRETE_TRAIT:
				if (legendSpecs.discreteLabels.length == 0)
					break;
				for (String label : legendSpecs.discreteLabels)
					legendLabelWidth = Math.max(legendLabelWidth, g.measureText(label).getWidth());
				break;
			case NONE:
			default:
				break;
		}
		g.setFont(font);
		double offset = 0.0;
		switch (style.legendPos) {
			case EAST:
			case WEST:
				// add space if y-axis decoration and legend on same side
				if ((style.legendPos == GraphStyle.Position.EAST) == style.showYAxisRight)
					offset = getYAxisDecorationWidth();

				legendGraphReserveWidth = style.minPadding + legendLabelWidth
						+ getLegendLabelPad(style.legendPos) + LegendLayout.BAR_WIDTH + LegendLayout.GAP;
				legendReserveWidth = offset + legendGraphReserveWidth;
				double maxReserve = Math.max(0.0, bounds.getWidth() * 0.45);
				if (legendReserveWidth > maxReserve) {
					legendReserveWidth = maxReserve;
					legendGraphReserveWidth = Math.max(0.0, legendReserveWidth - offset);
					legendLabelWidth = Math.max(0.0, legendGraphReserveWidth - style.minPadding
							- getLegendLabelPad(style.legendPos) - LegendLayout.BAR_WIDTH
							- LegendLayout.GAP);
				}
				return;
			case SOUTH:
				offset = getLegendOffset();
				// $FALL-THROUGH$
			case NORTH:
				legendGraphReserveHeight = 0.6 * LegendLayout.GAP
						+ (mode == LegendSpecs.Mode.DISCRETE_TRAIT ? Math.max(LegendLayout.BAR_WIDTH, 16.0)
								: LegendLayout.BAR_WIDTH + 14.0)
						+ style.minPadding;
				legendReserveHeight = offset + legendGraphReserveHeight;
				if (style.legendPos == GraphStyle.Position.NORTH
						&& (mode == LegendSpecs.Mode.FITNESS_GRADIENT || mode == LegendSpecs.Mode.DENSITY_GRADIENT))
					legendReserveHeight -= style.minPadding;
				if (style.legendPos != GraphStyle.Position.SOUTH)
					legendGraphReserveHeight = legendReserveHeight;
				return;
			case NONE:
			default:
				return;
		}
	}

	/**
	 * Get the extra space occupied below the plot by x-axis decorations.
	 *
	 * @return decoration height in pixels
	 */
	private double getLegendOffset() {
		double offset = 0.0;
		if (style.showXTicks)
			offset += style.tickLength + 2.0;
		if (style.showXTickLabels)
			offset += 14.0;
		if (style.showXLabel && style.xLabel != null)
			offset += 20.0;
		return offset;
	}

	/**
	 * Get the padding between legend labels and the color bar.
	 *
	 * @param pos the legend position
	 * @return label padding in pixels
	 */
	protected double getLegendLabelPad(GraphStyle.Position pos) {
		return (pos == GraphStyle.Position.WEST && !style.showYAxisRight ? 2.0 * LegendLayout.LABEL_PAD
				: LegendLayout.LABEL_PAD);
	}

	/**
	 * Estimate the horizontal space used by y-axis ticks, labels and title.
	 *
	 * @return y-axis decoration width in pixels
	 */
	private double getYAxisDecorationWidth() {
		double width = 0.0;
		if (style.showYLabel && style.yLabel != null)
			width += 12.0;
		if (style.showYTickLabels) {
			String font = g.getFont();
			setFont(style.ticksLabelFont);
			if (style.percentY) {
				int digits = style.yMax <= 1.0 ? 1 : 0;
				width += g.measureText(Formatter.formatPercent(100, digits)).getWidth();
			} else {
				width += g.measureText(
						Formatter.formatFix(-Math.max(Math.abs(style.yMin), Math.abs(style.yMax)), 2)).getWidth();
			}
			width += 4.0;
			g.setFont(font);
		}
		if (style.showYTicks)
			width += style.tickLength + 2.0;
		return width;
	}

	/**
	 * Compute layout for the legend bar and its labels.
	 *
	 * @param barWidth  desired bar width
	 * @param barHeight desired bar height
	 * @return legend layout or {@code null} if not applicable
	 */
	private LegendLayout getLegendLayout(double barWidth, double barHeight) {
		double barX;
		double barY;
		switch (style.legendPos) {
			case EAST:
				barX = bounds.getWidth() + (style.showYAxisRight ? getYAxisDecorationWidth() : 0.0) + LegendLayout.GAP;
				barY = (bounds.getHeight() - barHeight) * 0.5;
				break;

			case WEST:
				barX = (style.showYAxisRight ? 0.0 : getYAxisDecorationWidth())
						- LegendLayout.GAP - barWidth;
				barY = (bounds.getHeight() - barHeight) * 0.5;
				break;

			case SOUTH:
				barX = (bounds.getWidth() - barWidth) * 0.5;
				barY = bounds.getHeight() + getLegendOffset() + 0.6 * LegendLayout.GAP;
				break;

			case NORTH:
				barX = (bounds.getWidth() - barWidth) * 0.5;
				// no gap at the top
				barY = -0.6 * LegendLayout.GAP - barHeight;
				switch (legendSpecs.mode) {
					case FITNESS_GRADIENT:
					case DENSITY_GRADIENT:
						barY = -legendReserveHeight + style.minPadding;
						break;
					default:
						break;
				}
				break;

			default:
				return null;
		}
		return new LegendLayout(barX, barY, barWidth, barHeight);
	}

	/**
	 * Get the width of a horizontal discrete-trait legend.
	 *
	 * @param labels the segment labels
	 * @return legend width capped at the full graph width
	 */
	private double getHorizontalDiscreteTraitLegendBarWidth(String[] labels) {
		int nSegments = labels == null ? 0 : labels.length;
		if (nSegments <= 0)
			return bounds.getWidth() * 0.6;
		double minWidth = bounds.getWidth() * 0.6;
		double labelWidth = legendLabelWidth > 0.0 ? legendLabelWidth : 0.0;
		double targetWidth = nSegments * (labelWidth + 2.0 * LegendLayout.LABEL_PAD);
		return Math.min(bounds.getWidth(), Math.max(minWidth, targetWidth));
	}

	/**
	 * Get the discrete trait legend colors from the graph's current color map.
	 *
	 * @return indexed colors used by the graph, or {@code null}
	 */
	private String[] getDiscreteTraitLegendColors() {
		if (legendSpecs.mode != LegendSpecs.Mode.DISCRETE_TRAIT)
			return null;
		return ((ColorMap.Index<String>) getColorMap()).getColors();
	}

	/**
	 * Get the width available for the graph after reserving legend space.
	 *
	 * @return graph area width
	 */
	protected double getGraphAreaWidth() {
		return Math.max(0.0, bounds.getWidth() - (style.legendPos.isVertical() ? legendGraphReserveWidth : 0.0));
	}

	/**
	 * Get the left edge of the graph area after reserving legend space.
	 *
	 * @return graph area x-coordinate
	 */
	protected double getGraphAreaX() {
		if (!style.legendPos.isVertical())
			return bounds.getX();
		return bounds.getX() + (style.legendPos == GraphStyle.Position.WEST ? legendGraphReserveWidth : 0.0);
	}

	/**
	 * Get the height available for the graph after reserving legend space.
	 *
	 * @return graph area height
	 */
	protected double getGraphAreaHeight() {
		return Math.max(0.0,
				bounds.getHeight() - (style.legendPos.isHorizontal() ? legendGraphReserveHeight : 0.0));
	}

	/**
	 * Get the top edge of the graph area after reserving legend space.
	 *
	 * @return graph area y-coordinate
	 */
	protected double getGraphAreaY() {
		if (!style.legendPos.isHorizontal())
			return bounds.getY();
		return bounds.getY() + (style.legendPos == GraphStyle.Position.NORTH ? legendGraphReserveHeight : 0.0);
	}

	/**
	 * Legend geometry for drawing and hit-testing.
	 */
	private static class LegendLayout {

		/**
		 * Width of the legend color bar in pixels.
		 */
		static final double BAR_WIDTH = 10.0;

		/**
		 * Gap between graph and legend in pixels.
		 */
		static final double GAP = 16.0;

		/**
		 * Gap between legend labels and color bar in pixels.
		 */
		static final double LABEL_PAD = 4.0;

		/**
		 * Minimum height of one legend color band in pixels.
		 */
		static final double MIN_BAND_HEIGHT = 2.0;

		/**
		 * Legend bar x-coordinate.
		 */
		final double barX;

		/**
		 * Legend bar y-coordinate.
		 */
		final double barY;

		/**
		 * Legend bar width.
		 */
		final double barWidth;

		/**
		 * Legend bar height.
		 */
		final double barHeight;

		/**
		 * Create legend layout.
		 *
		 * @param barX      bar x-coordinate
		 * @param barY      bar y-coordinate
		 * @param barWidth  bar width
		 * @param barHeight bar height
		 */
		LegendLayout(double barX, double barY, double barWidth, double barHeight) {
			this.barX = barX;
			this.barY = barY;
			this.barWidth = barWidth;
			this.barHeight = barHeight;
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
