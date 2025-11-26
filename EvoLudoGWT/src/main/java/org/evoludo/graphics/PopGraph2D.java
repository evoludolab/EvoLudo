//
// EvoLudo Project
//
// Copyright 2010-2025 Christoph Hauert
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

import java.util.Arrays;
import java.util.Iterator;

import org.evoludo.geom.Node2D;
import org.evoludo.geom.Path2D;
import org.evoludo.geom.Point2D;
import org.evoludo.graphics.AbstractGraph.Shifting;
import org.evoludo.simulator.Network.Status;
import org.evoludo.simulator.Network2D;
import org.evoludo.simulator.geom.AbstractGeometry;
import org.evoludo.simulator.geom.GeometryType;
import org.evoludo.simulator.geom.HierarchicalGeometry;
import org.evoludo.simulator.modules.Module;
import org.evoludo.simulator.views.AbstractView;
import org.evoludo.util.RingBuffer;

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
 * multiple geometry types (triangular, honeycomb, square variants, linear, and
 * generic network layouts) and providing custom drawing, hit-testing and user
 * interaction behavior.
 *
 * <h3>Responsibilities</h3>
 * <ul>
 * <li>Calculate layout bounds and element sizes for a variety of geometry
 * types based on the current canvas size and configuration.</li>
 * <li>Render lattice cells or network nodes and links to a canvas using an
 * internal graphics context.</li>
 * <li>Maintain an optional history buffer for linear (time-series) lattices,
 * enabling visualization of past states.</li>
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
 * <li>LINEAR — drawn as rows of cells where the vertical axis can represent
 * time; uses an internal RingBuffer to store historical states</li>
 * <li>Generic network — uses Node2D positions and a radius-based scaling to
 * draw nodes and links; supports manual node shifting</li>
 * </ul>
 *
 * <h3>Key Concepts & Fields</h3>
 * <ul>
 * <li>data: a String[] holding per-node CSS color values used to paint each
 * cell/node.</li>
 * <li>buffer: an optional RingBuffer<String[]> used by linear geometry to
 * retain previous states for history visualization. Capacity is based on
 * visible steps and MAX_LINEAR_SIZE.</li>
 * <li>Bounds and sizing: internal fields such as side, dw, dw2, dh, dh3 and
 * dR compute per-cell/node sizes and are recalculated in calcBounds.</li>
 * <li>Hierarchy support: when geometry indicates a HIERARCHY, the component
 * accounts for hierarchical gaps and levels (hLevels, hPeriods).</li>
 * <li>Interaction flags: hitNode tracks the currently selected node index,
 * hitDragged indicates whether a node is currently being dragged.</li>
 * </ul>
 *
 * <h3>Interaction & Events</h3>
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
 * drawSquareNeumann2nd, drawLinearLattice).</li>
 * <li>For arbitrary networks the drawNetwork method scales the universe,
 * draws links and nodes (with per-node color optimization) and supports
 * node diameter computation (dR).</li>
 * <li>prepCanvas prepares the canvas transform (scale, translation and
 * zoom) and manages a saved graphics state so drawing is isolated.</li>
 * </ul>
 *
 * <h3>Sizing & Limits</h3>
 * <ul>
 * <li>MIN_DW, MIN_DH and MIN_DR define minimum drawable dimensions; if elements
 * would be smaller than these thresholds, the graph is disabled and a
 * message is displayed.</li>
 * <li>MAX_LINEAR_SIZE controls the maximum retained history length for linear
 * geometries; callers must ensure MAX_LINEAR_SIZE is adequate for their
 * configured canvas/steps, otherwise an exception is thrown during setup.</li>
 * </ul>
 *
 * <h3>Performance</h3>
 * Drawing operations use batching and occasional optimizations (e.g., reusing
 * fill style until it changes) but may still be expensive for very large
 * populations or frequent updates. For linear geometries the history buffer
 * size is chosen to balance memory and visible steps; it is lazily allocated.
 *
 * <h3>Extensibility & Limitations</h3>
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
 * underlying population state changes to refresh the visualization. For linear
 * geometries, ensure the {@code MAX_LINEAR_SIZE} accommodates the expected
 * width of the viewport.
 * 
 * @author Christoph Hauert
 */
@SuppressWarnings("java:S110")
public class PopGraph2D extends GenericPopGraph<String, Network2D> implements Shifting {

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

	/**
	 * The maximum size of a linear graph. This affects memory allocation to retain
	 * the history of the graph.
	 */
	public static final int MAX_LINEAR_SIZE = 500;

	@Override
	public void setGeometry(AbstractGeometry geometry) {
		super.setGeometry(geometry);
		int size = geometry.getSize();
		if (geometry.isType(GeometryType.LINEAR) && size <= MAX_LINEAR_SIZE) {
			// linear graphs maintain history; allocate generously
			if (buffer == null)
				buffer = new RingBuffer<String[]>(2 * MAX_LINEAR_SIZE);
			// with a buffer we need to make sure colors is initialized as well
			if (data == null || data.length != size)
				data = new String[size];
		}
		calcBounds();
	}

	@Override
	public void activate() {
		super.activate();
		// lazy allocation of memory for colors
		if (!hasMessage && (data == null || data.length != geometry.getSize()))
			data = new String[geometry.getSize()];
	}

	@Override
	public void update(boolean isNext) {
		if (buffer != null) {
			// add copy of data array to buffer
			// note: cannot be reliably done in RingBuffer class without reflection
			String[] copy = Arrays.copyOf(data, data.length);
			if (isNext || buffer.isEmpty())
				buffer.append(copy);
			else
				buffer.replace(copy);
		}
		super.update(isNext);
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
			case LINEAR:
				drawLinearLattice();
				break;
			default:
				logger.warning("Unsupported geometry: " + type.getTitle());
		}
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

	/**
	 * Draw linear lattice with history.
	 */
	private void drawLinearLattice() {
		int nSteps = (int) (bounds.getHeight() / dh);
		int yshift = 0;
		Iterator<String[]> it = buffer.iterator();
		while (it.hasNext() && nSteps-- > 0) {
			int xshift = 0;
			String[] state = it.next();
			for (int n = 0; n < geometry.getSize(); n++) {
				g.setFillStyle(state[n]);
				fillRect(xshift, yshift, dw, dh);
				xshift += dw;
			}
			yshift += dh;
		}
		drawFrame(4, 4);
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

	@Override
	public void calcBounds(int width, int height) {
		super.calcBounds(width, height);
		clearMessage();
		noGraph = false;
		// reset sizing defaults
		dw = 0;
		dh = 0;
		dR = 0.0;

		GeometryType type = geometry.getType();
		isHierarchy = (type == GeometryType.HIERARCHY);
		if (isHierarchy)
			type = ((HierarchicalGeometry) geometry).getSubType();
		if (!isHierarchy || !type.isSquareLattice())
			hPeriods = null;

		// dispatch to concise handlers for each geometry
		switch (type) {
			case CUBE:
			case VOID:
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
			case LINEAR:
				handleLinear(width, height);
				break;
			default:
				handleNetwork(width, height);
				break;
		}

		// final sanity checks and lazy allocation
		if (dw < MIN_DW && dh < MIN_DH && dR < MIN_DR) {
			buffer = null;
			data = null;
			noGraph = true;
			displayMessage("Population size to large!");
		} else {
			if (view.isActive() && (data == null || data.length != geometry.getSize()))
				data = new String[geometry.getSize()];
		}
	}

	/**
	 * Handle geometries without graphical representation.
	 * 
	 * @param type the geometry type
	 */
	private void handleNoRepresentation(GeometryType type) {
		buffer = null;
		data = null;
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
		buffer = null;
		side = (int) (Math.sqrt(geometry.getSize()) + 0.5);
		int diameter = Math.min(width, height);
		dw2 = diameter / (side + 3);
		int bWidth = dw2 * (side + 1);
		dw = 2 * dw2;
		dh = diameter / (side + 1);
		if (dw < MIN_DW || dh < MIN_DH) {
			bounds.setSize(width, height);
			return;
		}
		int bHeight = dh * side;
		int dx = (width - bWidth) / 2;
		int dy = (height - bHeight) / 2;
		bounds.set(dx, dy, bWidth, bHeight);
		style.showFrame = false;
	}

	/**
	 * Handle honeycomb lattice geometry.
	 * 
	 * @param width  the width of the canvas
	 * @param height the height of the canvas
	 */
	private void handleHoneycomb(int width, int height) {
		buffer = null;
		side = (int) (Math.sqrt(geometry.getSize()) + 0.5);
		int diameter = Math.min(width, height);
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
		int dx = (width - bWidth) / 2;
		int dy = (height - bHeight) / 2;
		bounds.set(dx, dy, bWidth, bHeight);
		style.showFrame = false;
	}

	/**
	 * Handle square lattice geometry.
	 * 
	 * @param width  the width of the canvas
	 * @param height the height of the canvas
	 */
	private void handleSquare(int width, int height) {
		buffer = null;
		if (style.showDecoratedFrame) {
			super.calcBounds(width, height);
		} else {
			int bWidth = width - 2 * style.minPadding;
			int bHeight = height - 2 * style.minPadding;
			bounds.set(style.minPadding, style.minPadding, bWidth, bHeight);
		}

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

		int bWidth = (int) bounds.getWidth();
		int bHeight = (int) bounds.getHeight();
		dw = (Math.min(bWidth, bHeight) - gap) / side;
		if (dw < MIN_DW) {
			bounds.setSize(width, height);
			return;
		}
		dh = dw;
		int newdim = dw * side + gap;
		int dx = (bWidth - newdim) / 2;
		int dy = (bHeight - newdim) / 2;
		bounds.set(dx, dy, newdim, newdim);
		style.showFrame = true;
	}

	/**
	 * Handle linear lattice geometry with history.
	 * 
	 * @param width  the width of the canvas
	 * @param height the height of the canvas
	 */
	private void handleLinear(int width, int height) {
		int bWidth = (int) bounds.getWidth();
		dw = bWidth / geometry.getSize();
		int bHeight = (int) bounds.getHeight();
		dh = dw;
		int steps = (dh == 0) ? 0 : bHeight / dh;
		if (dw < MIN_DW || steps == 0) {
			bounds.setSize(width, height);
			return;
		}
		int adjw = dw * geometry.getSize();
		int adjh = bHeight - (bHeight % dh);
		int dx = (bWidth - adjw) / 2;
		int dy = (bHeight - adjh) / 2;
		bounds.set(dx, dy, adjw, adjh);
		if (buffer == null)
			throw new IllegalStateException("Increase MAX_LINEAR_SIZE (" + MAX_LINEAR_SIZE + ")!");
		int capacity = 2 * steps;
		buffer.setCapacity(capacity);
		style.setYRange(steps - 1);
		style.showFrame = true;
	}

	/**
	 * Handle generic network geometry.
	 * 
	 * @param width  the width of the canvas
	 * @param height the height of the canvas
	 */
	private void handleNetwork(int width, int height) {
		int diameter = Math.min(width, height);
		int radius = diameter / 2;
		dR = Math.sqrt(radius * radius * 2.0 / geometry.getSize());
		if (dR < MIN_DR) {
			bounds.setSize(width, height);
			return;
		}
		buffer = null;
		bounds.set((width - diameter) / 2.0, (height - diameter) / 2.0, diameter, diameter);
		style.showFrame = false;
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
		if (!bounds.contains(x, y))
			return FINDNODEAT_OUT_OF_BOUNDS;

		int sx = (int) ((viewCorner.getX() + x - bounds.getX()) / zoomFactor + 0.5);
		int sy = (int) ((viewCorner.getY() + y - bounds.getY()) / zoomFactor + 0.5);

		GeometryType type = geometry.getType();
		if (isHierarchy)
			type = ((HierarchicalGeometry) geometry).getSubType();

		switch (type) {
			// 3D views must deal with this
			case CUBE:
				return FINDNODEAT_UNIMPLEMENTED;
			case TRIANGULAR:
				return findTriangularNode(sx, sy);
			case HEXAGONAL:
				return findHoneycombNode(sx, sy);
			case SQUARE_NEUMANN:
			case SQUARE_NEUMANN_2ND:
			case SQUARE_MOORE:
			case SQUARE:
				return findSquareNode(sx, sy);
			case LINEAR:
				return findLinearNode(sx, sy);
			default:
				return findNetworkNode(sx, sy);
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
	 * Find the index of the node at the location with coordinates {@code (x, y)} in
	 * a linear lattice.
	 * 
	 * @param x the {@code x}-coordinate of the location
	 * @param y the {@code y}-coordinate of the location
	 * @return the index of the node
	 */
	private int findLinearNode(int x, int y) {
		int c = x / dw;
		int r = y / dh;
		return r * geometry.getSize() + c;
	}

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
		// super sets mouse coordinates
		super.onMouseDown(event);
		if (event.getNativeButton() == NativeEvent.BUTTON_LEFT) {
			hitNode = findNodeAt(mouseX, mouseY);
			if (hitNode >= 0) {
				element.addClassName(CURSOR_GRAB_NODE_CLASS);
			} else {
				element.removeClassName(CURSOR_GRAB_NODE_CLASS);
			}
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
			hitDragged = true;
			element.addClassName("evoludo-cursorMoveNode");
			int x = event.getX();
			int y = event.getY();
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
				// touchEndTime = Duration.currentTimeMillis();
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
			// shift position of node
			shiftNodeBy(hitNode, mouseX - x, mouseY - y);
			mouseX = x;
			mouseY = y;
			event.preventDefault();
			return;
		}
		super.onTouchMove(event);
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