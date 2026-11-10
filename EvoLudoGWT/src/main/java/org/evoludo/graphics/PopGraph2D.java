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
import org.evoludo.simulator.Network.Status;
import org.evoludo.simulator.Network2D;
import org.evoludo.simulator.geometries.AbstractGeometry;
import org.evoludo.simulator.geometries.GeometryType;
import org.evoludo.simulator.geometries.HierarchicalGeometry;
import org.evoludo.simulator.modules.Module;
import org.evoludo.simulator.views.AbstractView;
import org.evoludo.simulator.views.Distribution;
import org.evoludo.ui.ContextMenu;
import org.evoludo.ui.ContextMenuRadioItem;
import org.evoludo.ui.Gesture;

import com.google.gwt.core.client.JsArray;
import com.google.gwt.dom.client.Touch;
import com.google.gwt.event.dom.client.MouseDownEvent;
import com.google.gwt.event.dom.client.MouseMoveEvent;
import com.google.gwt.event.dom.client.TouchCancelEvent;
import com.google.gwt.event.dom.client.TouchEndEvent;
import com.google.gwt.event.dom.client.TouchMoveEvent;
import com.google.gwt.event.dom.client.TouchStartEvent;

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
 * drawXxx methods (drawTriangular, drawHoneycomb, drawSquareLattice).</li>
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
	 * Helper responsible for drawing and hit-testing the legend.
	 */
	protected Legend2D legend;

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
		msgLabel.addStyleName("evoludo-Message2D");
		legend = new Legend2D(this);
	}

	@Override
	protected void onUnload() {
		super.onUnload();
		legend = null;
	}

	/**
	 * Assign semantic legend data prepared by the view.
	 *
	 * @param specs legend data
	 */
	public void setLegendSpecs(Legend2D.Specs specs) {
		if (legend.setSpecs(specs))
			calcBounds();
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
		if (legend.getMode() == Legend2D.Mode.NONE)
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
		boolean wasBlocked = hasMessage();
		super.onResize();
		ensureData();
		if (!wasBlocked || hasMessage || geometry == null)
			return;
		if (invalidated && !hasStaticLayout()) {
			update(true);
			return;
		}
		paint(true);
	}

	/**
	 * Ensure that the data array is properly initialized based on the current
	 * geometry. Temporary display constraints do not discard the backing colour
	 * data so the graph can immediately redraw when more space becomes available.
	 */
	protected void ensureData() {
		if (geometry == null) {
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
		// default no-op; used in subclasses with history
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
		if (!hasStaticLayout())
			// networks are drawn without a frame; the layout of rewired lattices may
			// only be settled after calcBounds ran, see #handleNetwork(int, int)
			style.showFrame = false;
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
		if (!prepCanvas(false))
			return;
		invalidated = false;
		GeometryType type = getLatticeType();
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
				drawSquareLattice(false);
				break;
			case SQUARE_NEUMANN_2ND:
				drawSquareLattice(true);
				break;
			default:
				view.logWarning("Unsupported geometry: " + type.getTitle());
		}
		g.restore();
		legend.draw();
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
		drawFrame(0, 0);
	}

	/**
	 * Draw honeycomb lattice.
	 */
	private void drawHoneycomb() {
		Path2D hex = Path2D.createPolygon2D(new double[] { 0, -dw2, -dw2, 0, dw2, dw2 },
				new double[] { 0, dh3, dh, dh + dh3, dh, dh3 });
		hex.translate(dw2, 0);
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
		drawFrame(0, 0);
	}

	/**
	 * Draw square (hierarchical) lattices. For second-order von Neumann
	 * neighbourhoods, nodes on alternating sublattices are drawn as circles.
	 *
	 * @param neumann2nd {@code true} for second-order von Neumann neighbourhoods
	 */
	private void drawSquareLattice(boolean neumann2nd) {
		int yshift = (int) bounds.getHeight() - dh;
		int row = 0;
		for (int h = 0; h < side; h++) {
			if (h > 0)
				yshift -= hierarchyGapAt(h);
			int xshift = 0;
			for (int w = 0; w < side; w++) {
				if (w > 0)
					xshift += hierarchyGapAt(w);
				g.setFillStyle(data[row + w]);
				if (neumann2nd && h % 2 != w % 2)
					fillCircle(xshift + dw2, yshift + dw2, dw2);
				else
					fillRectSnapped(xshift, yshift, dw, dh);
				xshift += dw;
			}
			yshift -= dh;
			row += side;
		}
		drawFrame(0, 0);
	}

	/**
	 * Compute the gap before the row or column {@code idx} of hierarchical
	 * structures, i.e. the number of hierarchical levels with a unit starting at
	 * {@code idx}.
	 *
	 * @param idx the row or column index
	 * @return the gap in pixels
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

	@Override
	protected void drawNetwork() {
		if (!prepCanvas(false))
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
		g.setStrokeStyle(style.linkColor);
		Path2D links = network.getLinks();
		if (!links.isEmpty()) {
			g.setLineWidth(style.linkWidth);
			stroke(links);
		}
		String current = data[0];
		if (current == null)
			current = style.frameColor;
		g.setFillStyle(current);
		Node2D[] nodes = network.toArray();
		for (int k = 0; k < nNodes; k++) {
			String next = data[k];
			if (next == null)
				next = current;
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
		g.restore();
		legend.draw();
	}

	@Override
	public String getTooltipAt(int x, int y) {
		return onGraph(x, y) ? super.getTooltipAt(x, y) : legend.getTooltipAt(x, y);
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * The bounds may be smaller than the canvas, e.g. for lattices centered in a
	 * wider canvas, and the zoomed graph extends beyond the bounds. Each edge of
	 * the zoomed graph can be shifted up to the margin of the canvas but no
	 * further. The margin is the smaller of the horizontal and vertical offsets of
	 * the bounds, i.e. the margin along the tighter side. Graphs with axes, i.e. a
	 * decorated frame, and unzoomed graphs are limited to the bounds as usual.
	 */
	@Override
	protected void setViewCorner(double x, double y, double zoom) {
		if (zoom <= 1.0 || style.showDecoratedFrame) {
			super.setViewCorner(x, y, zoom);
			return;
		}
		double margin = Math.min(bounds.getX(), bounds.getY());
		viewCorner.set(
				limit(x, bounds.getX() - margin,
						bounds.getX() + margin + zoom * bounds.getWidth() - wrapper.getOffsetWidth()),
				limit(y, bounds.getY() - margin,
						bounds.getY() + margin + zoom * bounds.getHeight() - wrapper.getOffsetHeight()));
	}

	/**
	 * Limit {@code value} to the interval between {@code a} and {@code b}, in
	 * either order. If the zoomed graph is larger than the canvas, its edges must
	 * not enter the canvas; otherwise the graph must remain within the canvas.
	 *
	 * @param value the value to limit
	 * @param a     one end of the interval
	 * @param b     the other end of the interval
	 * @return the limited value
	 */
	private static double limit(double value, double a, double b) {
		return Math.max(Math.min(a, b), Math.min(Math.max(a, b), value));
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
	 * <ul>
	 * <li>Square lattices: {@code dw} is checked directly; {@code dh} is set
	 * equal to {@code dw} afterwards, so no separate height check is
	 * needed.</li>
	 * <li>Triangular lattices: checked against {@code dw2} (half the node
	 * width) rather than {@code dw} itself, which guarantees
	 * {@code dw = 2 * dw2 >= 2 * MIN_DW} and, because {@code dh} is always
	 * divided by a smaller denominator than {@code dw2}
	 * ({@code side + 1 < side + 3}), also {@code dh >= dw2 >= MIN_DW} —
	 * without a separate height check.</li>
	 * <li>Honeycomb lattices: checked against {@code dw} (itself
	 * {@code 2 * dw2}); as {@code dw} is always even this only ever excludes
	 * the degenerate {@code dw2 == 0} case. See {@link #MIN_DH} for the
	 * actual height constraint ({@code dh3}).</li>
	 * </ul>
	 * Also combined with {@link #MIN_DH} and {@link #MIN_DR} in the final
	 * catch-all check after layout, which disables the graph only if width,
	 * height and diameter are all below their respective minimums.
	 */
	static final int MIN_DW = 1;

	/**
	 * The minimum height of a node in pixels.
	 * <ul>
	 * <li>Square lattices: not checked directly (see {@link #MIN_DW}); by
	 * construction {@code dh} always equals {@code dw}.</li>
	 * <li>Triangular lattices: not checked directly; the constraint is
	 * satisfied transitively through the {@link #MIN_DW} check on
	 * {@code dw2}.</li>
	 * <li>Honeycomb lattices: checked against {@code dh3} (one third of the
	 * full hexagon height, {@code dh = 3 * dh3}), so the effective minimum
	 * full height is {@code dh >= 3 * MIN_DH}.</li>
	 * </ul>
	 * Also combined with {@link #MIN_DW} and {@link #MIN_DR} in the final
	 * catch-all check after layout, which disables the graph only if width,
	 * height and diameter are all below their respective minimums.
	 */
	static final int MIN_DH = 1;

	/**
	 * The minimum diameter of a node in pixels for networks.
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

	@Override
	public void calcBounds(int width, int height) {
		super.calcBounds(width, height);
		clearMessage();
		noGraph = false;
		// reset sizing defaults
		dw = 0;
		dh = 0;
		dR = 0.0;

		isHierarchy = (geometry.getType() == GeometryType.HIERARCHY);
		GeometryType type = getLatticeType();
		if (!isHierarchy || !type.isSquareLattice())
			hLevels = 0;

		// dispatch to concise handlers for each geometry
		// note: rewiring or adding links destroys the regular structure of lattices,
		// which then require layouting like any other network, see Network#reset()
		if (!isHierarchy && type.isLattice() && !geometry.isLattice()) {
			handleNetwork(width, height);
		} else {
			switch (type) {
				case LINEAR:
					if (this instanceof PopGraph1D)
						return;
					//$FALL-THROUGH$
				case CUBE:
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
		}

		// final sanity checks
		if (dw < MIN_DW && dh < MIN_DH && dR < MIN_DR) {
			noGraph = true;
			if (view instanceof Distribution)
				displayMessage(INFO_BINNING_TOO_FINE);
			else
				displayMessage(INFO_POPSIZE_TOO_BIG);
		}
		legend.setBounds(width, height, bounds);
	}

	/**
	 * Get the type of the lattice, i.e. the type of the units of hierarchical
	 * structures.
	 *
	 * @return the type of the lattice
	 */
	private GeometryType getLatticeType() {
		if (isHierarchy)
			return ((HierarchicalGeometry) geometry).getSubType();
		return geometry.getType();
	}

	/**
	 * Adjust bounds to make room for legend.
	 */
	@Override
	protected void adjustBoundsForLegend() {
		legend.reserve(bounds);
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
		int diameter = (int) Math.min(bounds.getWidth(), bounds.getHeight());
		dw2 = diameter / (side + 3);
		int bWidth = dw2 * (side + 1);
		dw = 2 * dw2;
		dh = diameter / (side + 1);
		if (dw2 < MIN_DW) {
			bounds.setSize(width, height);
			return;
		}
		centerBounds(bWidth, dh * side);
	}

	/**
	 * Handle honeycomb lattice geometry.
	 * 
	 * @param width  the width of the canvas
	 * @param height the height of the canvas
	 */
	private void handleHoneycomb(int width, int height) {
		side = (int) (Math.sqrt(geometry.getSize()) + 0.5);
		int diameter = (int) Math.min(bounds.getWidth(), bounds.getHeight());
		dw2 = diameter / (2 * side + 1);
		int bWidth = dw2 * (2 * side + 1);
		dw = 2 * dw2;
		dh3 = diameter / (3 * side + 1);
		dh = 3 * dh3;
		if (dw < MIN_DW || dh3 < MIN_DH) {
			bounds.setSize(width, height);
			return;
		}
		centerBounds(bWidth, dh3 * (3 * side + 1));
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

		double plotX = bounds.getX();
		double plotY = bounds.getY();
		int bWidth = (int) Math.rint(bounds.getWidth());
		int bHeight = (int) Math.rint(bounds.getHeight());
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
		int diameter = (int) Math.min(bounds.getWidth(), bounds.getHeight());
		int radius = diameter / 2;
		dR = Math.sqrt(radius * radius * 2.0 / geometry.getSize());
		if (dR < MIN_DR) {
			bounds.setSize(width, height);
			return;
		}
		networkXAspect = bounds.getWidth() / diameter;
		networkYAspect = bounds.getHeight() / diameter;
		centerBounds(diameter, diameter);
	}

	/**
	 * Center bounds of size {@code width} by {@code height} within the current
	 * bounds. The frame is not shown.
	 *
	 * @param width  the width of the new bounds
	 * @param height the height of the new bounds
	 */
	private void centerBounds(double width, double height) {
		bounds.set(bounds.getX() + (bounds.getWidth() - width) * 0.5,
				bounds.getY() + (bounds.getHeight() - height) * 0.5, width, height);
		style.showFrame = false;
	}

	/**
	 * The ratio of the width of the plot area and the diameter of the network.
	 * Nodes can be dragged within the plot area, i.e. horizontally within
	 * {@code ±networkXAspect} times the radius of the network.
	 *
	 * @see #handleNetwork(int, int)
	 * @see #dragNodeTo(int, int)
	 */
	private double networkXAspect = 1.0;

	/**
	 * The ratio of the height of the plot area and the diameter of the network.
	 * Nodes can be dragged within the plot area, i.e. vertically within
	 * {@code ±networkYAspect} times the radius of the network.
	 *
	 * @see #handleNetwork(int, int)
	 * @see #dragNodeTo(int, int)
	 */
	private double networkYAspect = 1.0;

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

		Point2D p = toLattice(x, y);
		double sx = p.getX();
		double sy = p.getY();
		if (sx < 0.0 || sx > bounds.getWidth() || sy < 0.0 || sy > bounds.getHeight())
			return FINDNODEAT_OUT_OF_BOUNDS;
		int isx = (int) (sx + 0.5);
		int isy = (int) (sy + 0.5);

		switch (getLatticeType()) {
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
		int wgap = 0;
		int hgap = 0;
		for (int i = 0; i < hLevels; i++) {
			wgap += c / hPeriods[i]; // gaps left of mouse
			hgap += (side - 1 - r) / hPeriods[i];
		}
		c = (x - wgap * HIERARCHY_GAP) / dw;
		r = side - 1 - (y - hgap * HIERARCHY_GAP) / dh;
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
	 * Check whether the current graph state supports dragging individual nodes.
	 *
	 * Only non-lattice network layouts with established node positions are
	 * draggable. Static lattice layouts should fall back to the inherited shifter
	 * instead.
	 *
	 * @return {@code true} if node dragging is available
	 */
	protected boolean supportsNodeDragging() {
		return network != null && !hasStaticLayout() && network.isStatus(Status.HAS_LAYOUT);
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * If a node has been hit by a left-click, remember the node's index and its
	 * position relative to the mouse. This information is used by subsequent
	 * {@link MouseMoveEvent}s to drag the node. The node keeps following the mouse
	 * (as closely as possible) even if the mouse leaves the graph.
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
		// super starts tracking the mouse and sets leftMouseButton when inside
		super.onMouseDown(event);
		if (!leftMouseButton || !supportsNodeDragging()) {
			hitNode = -1;
			element.removeClassName(CURSOR_GRAB_NODE_CLASS);
			return;
		}
		int x = event.getX();
		int y = event.getY();
		hitNode = findNodeAt(x, y);
		if (hitNode >= 0) {
			grabNode(x, y);
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
	 * </dl>
	 *
	 * @see #onMouseMove(MouseMoveEvent)
	 */
	@Override
	protected void endDrag() {
		super.endDrag();
		if (hitDragged) {
			// reshow tooltip after dragging
			hitDragged = false;
			tooltip.update();
		}
		hitNode = -1;
		element.removeClassName(CURSOR_GRAB_NODE_CLASS);
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * If a node has been hit by a {@link MouseDownEvent} and is being dragged,
	 * the node follows the mouse as closely as possible, even outside of the
	 * graph. This allows to customize the display of the graph.
	 *
	 * @see #onMouseDown(MouseDownEvent)
	 * @see Gesture#move(Gesture.Move)
	 */
	@Override
	public void onMouseMove(MouseMoveEvent event) {
		if (isReleased(event))
			return;
		if (supportsNodeDragging() && hitNode >= 0) {
			event.preventDefault();
			hitDragged = true;
			gesture.move(Gesture.Move.NODE);
			dragNodeTo(event.getX(), event.getY());
			return;
		}
		super.onMouseMove(event);
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * The number of touches on the graph changed. Release any node selected for
	 * dragging if no touches remain.
	 */
	@Override
	public void onTouchEnd(TouchEndEvent event) {
		super.onTouchEnd(event);
		if (event.getTouches().length() == 0)
			hitNode = -1;
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * Release any node selected for dragging.
	 */
	@Override
	public void onTouchCancel(TouchCancelEvent event) {
		super.onTouchCancel(event);
		hitNode = -1;
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * If a node was touched, remember its position relative to the touch for
	 * dragging the node.
	 *
	 * @see #onTouchMove(TouchMoveEvent)
	 */
	@Override
	public void onTouchStart(TouchStartEvent event) {
		// super sets hitNode if a node was touched
		super.onTouchStart(event);
		JsArray<Touch> touches = event.getTouches();
		if (touches.length() == 1 && supportsNodeDragging() && hitNode >= 0) {
			Touch touch = touches.get(0);
			grabNode(touch.getRelativeX(element), touch.getRelativeY(element));
		}
	}

	/**
	 * {@inheritDoc}
	 * <dl>
	 * <dt>move one finger on node
	 * <dd>shift node. The node follows the touch as closely as possible, even
	 * outside of the graph.
	 * </dl>
	 */
	@Override
	public void onTouchMove(TouchMoveEvent event) {
		JsArray<Touch> touches = event.getTouches();
		if (touches.length() == 1 && supportsNodeDragging() && hitNode >= 0) {
			Touch touch = touches.get(0);
			dragNodeTo(touch.getRelativeX(element), touch.getRelativeY(element));
			event.preventDefault();
			return;
		}
		super.onTouchMove(event);
	}

	/**
	 * Check if the coordinates {@code (x, y)} are on the graph, i.e. within its
	 * (zoomed and shifted) bounds rather than, e.g., on the legend. Shifting and
	 * zooming work anywhere, see {@link #inside(int, int)}.
	 *
	 * @param x the {@code x}-coordinate relative to the graph element
	 * @param y the {@code y}-coordinate relative to the graph element
	 * @return {@code true} if on the graph
	 */
	private boolean onGraph(int x, int y) {
		Point2D p = toLattice(x, y);
		return p.getX() >= 0.0 && p.getX() <= bounds.getWidth() && p.getY() >= 0.0 && p.getY() <= bounds.getHeight();
	}

	/**
	 * Convert the coordinates {@code (x, y)} relative to the graph into
	 * coordinates of the lattice or network, see {@link #toContent(double, double)}.
	 * The frame border is a centered stroke drawn with the zoomed content, such
	 * that only half its width extends past the origin of the content.
	 *
	 * @param x the {@code x}-coordinate relative to the graph
	 * @param y the {@code y}-coordinate relative to the graph
	 * @return the coordinates of the lattice or network
	 */
	protected Point2D toLattice(int x, int y) {
		double half = style.frameWidth * 0.5;
		return toContent(x, y).shift(-half, -half);
	}

	/**
	 * The horizontal offset of the dragged node relative to the pointer in network
	 * coordinates.
	 *
	 * @see #grabNode(int, int)
	 */
	private double grabDX;

	/**
	 * The vertical offset of the dragged node relative to the pointer in network
	 * coordinates.
	 *
	 * @see #grabNode(int, int)
	 */
	private double grabDY;

	/**
	 * Convert the coordinates {@code (x, y)} of the pointer (relative to the graph
	 * element) to network coordinates. Keep aligned with
	 * {@link #findNodeAt(int, int)}. Coordinates outside of the graph map to
	 * positions outside of the network.
	 *
	 * @param x the {@code x}-coordinate of the pointer
	 * @param y the {@code y}-coordinate of the pointer
	 * @return the network coordinates
	 */
	private Point2D toNetwork(int x, int y) {
		double rr = network.getRadius();
		// bounds.width==bounds.height for networks
		double iscale = (rr + rr) / bounds.getWidth();
		return toLattice(x, y).scale(iscale).shift(-rr, -rr);
	}

	/**
	 * Grab the node {@link #hitNode} with the pointer at {@code (x, y)}, i.e.
	 * remember the position of the node relative to the pointer for dragging.
	 *
	 * @param x the {@code x}-coordinate of the pointer
	 * @param y the {@code y}-coordinate of the pointer
	 *
	 * @see #dragNodeTo(int, int)
	 */
	private void grabNode(int x, int y) {
		Point2D pointer = toNetwork(x, y);
		Node2D node = network.get(hitNode);
		grabDX = node.getX() - pointer.getX();
		grabDY = node.getY() - pointer.getY();
	}

	/**
	 * Drag the node {@link #hitNode} such that it follows the pointer at
	 * {@code (x, y)} as closely as possible. The node keeps its position relative
	 * to the pointer when it was grabbed but remains entirely within the plot
	 * area.
	 *
	 * @param x the {@code x}-coordinate of the pointer
	 * @param y the {@code y}-coordinate of the pointer
	 *
	 * @see #grabNode(int, int)
	 */
	private void dragNodeTo(int x, int y) {
		double rr = network.getRadius();
		double xmax = rr * networkXAspect;
		double ymax = rr * networkYAspect;
		Point2D pointer = toNetwork(x, y);
		Node2D node = network.get(hitNode);
		double r = node.getR();
		node.set(Math.max(-xmax + r, Math.min(xmax - r, pointer.getX() + grabDX)),
				Math.max(-ymax + r, Math.min(ymax - r, pointer.getY() + grabDY)));
		network.linkNodes();
		drawNetwork();
	}
}
