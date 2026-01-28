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

import org.evoludo.graphics.AbstractGraph.Zooming;
import org.evoludo.simulator.ColorMap;
import org.evoludo.simulator.Network;
import org.evoludo.simulator.Network.Status;
import org.evoludo.simulator.geometries.AbstractGeometry;
import org.evoludo.simulator.geometries.GeometryFeatures;
import org.evoludo.simulator.geometries.GeometryType;
import org.evoludo.simulator.geometries.HierarchicalGeometry;
import org.evoludo.simulator.modules.Module;
import org.evoludo.simulator.views.AbstractView;
import org.evoludo.ui.ContextMenu;
import org.evoludo.ui.ContextMenuCheckBoxItem;
import org.evoludo.ui.ContextMenuItem;
import org.evoludo.util.Formatter;

import com.google.gwt.core.client.Duration;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Touch;
import com.google.gwt.event.dom.client.DoubleClickEvent;
import com.google.gwt.event.dom.client.DoubleClickHandler;
import com.google.gwt.event.dom.client.TouchMoveEvent;
import com.google.gwt.event.dom.client.TouchStartEvent;
import com.google.gwt.user.client.ui.Label;

/**
 * Abstract base class for visualizing a population as a graph. GenericPopGraph
 * ties a population AbstractGeometry to an optional Network representation and
 * provides common support for layouting, drawing, interaction and debugging for
 * concrete graph views (2D/3D, lattice or general network).
 *
 * <h3>Key responsibilities:</h3>
 * <ul>
 * <li>Manage the backing AbstractGeometry and its Network (if present). The
 * network may be null for models without spatial structure (e.g. ODE/SDE).</li>
 * <li>Coordinate layouting: static lattice layouts are drawn directly via
 * {@link #drawLattice()}; {@link #drawNetwork()} is used for dynamic/network
 * layouts which calls back via {@link Network.LayoutListener} and this class
 * drives the animation and final drawing via {@link #layoutNetwork()} and
 * {@link #drawNetwork()}.</li>
 * <li>Provide common interaction handling: tooltips, double-clicks, touch
 * gestures (single/double tap, long press, pinch zoom), mouse cursor
 * styling when hovering nodes, and context menu entries (shake,
 * animate layout, clear history, debug submenu).</li>
 * <li>Maintain display state such as labelling, color mapping for node
 * traits, invalidation/refresh logic and flags controlling animated
 * layouting.</li>
 * </ul>
 *
 * <h3>Design notes and behavior:</h3>
 * <ul>
 * <li>The class is generic in the node color/type T and the concrete Network
 * implementation N. Subclasses are responsible for converting node data
 * to CSS color strings and for hit-testing nodes at screen coordinates.</li>
 * <li>Subclasses must implement the rendering and hit-testing contract:
 * {@link #drawLattice()}, {@link #drawNetwork()}, {@link #getCSSColorAt(int)}
 * and {@link #findNodeAt(int,int)}. These methods encapsulate geometry-specific
 * drawing and input logic.</li>
 * <li>Layout updates are coordinated with the Network. For animated layouts
 * the class may repeatedly request redraws during progress updates; for
 * non-animated or very large networks animation can be disabled using the
 * animate flag and size threshold constants.</li>
 * <li>{@link #layoutUpdate(double)} and {@link #layoutComplete()} are
 * synchronized and used by the Network to report progress and completion.
 * {@link #layoutNetwork()} ensures that a layout exists before calling
 * {@link #drawNetwork()}.</li>
 * <li>Touch handling distinguishes single tap, double tap, long press and
 * multi-touch gestures to support node selection, tooltip display,
 * pinching zoom and invoking node-specific actions.</li>
 * <li>Context menu population is centralized here. Implementations add
 * graph-specific menu items but benefit from the common items provided
 * (shake, animate, clear history and debug/update options). The debug
 * submenu is gated by isDebugEnabled and the model type.</li>
 * <li>Invalidation resets the network (if present) and marks the view for
 * redraw; {@link #update(boolean isNext)} defers layout work using a scheduler
 * to allow dependent views (3D rendering) to become ready.</li>
 * <li>TooltipProvider integration: if the view implements TooltipProvider.Index
 * the class will use it to produce tooltips for nodes; otherwise a fallback
 * basic provider may be used when available.</li>
 * </ul>
 *
 * <h3>Important fields and conventions referenced by subclasses:</h3>
 * <ul>
 * <li>{@code geometry} - the population structure (may be null for non-spatial
 * models).</li>
 * <li>{@code network} - the Network representation derived from the geometry
 * (may be null).</li>
 * <li>{@code data} - array of node trait values used for coloring or other
 * per-node state.</li>
 * <li>{@code colorMap} - maps node trait values (T) to display colors.</li>
 * <li>{@code label} - optional label displayed on the graph wrapper.</li>
 * <li>{@code animate} - flag enabling/disabling animated layout progress.</li>
 * <li>{@code invalidated} / {@code noGraph} / {@code hasMessage} - control
 * redraw and message display behavior.</li>
 * <li>{@code hitNode} - index of last node hit by touch/mouse
 * interactions.</li>
 * <li>Return codes for {@link #findNodeAt(int,int)}:
 * {@code FINDNODEAT_OUT_OF_BOUNDS} and {@code FINDNODEAT_UNIMPLEMENTED} are
 * used to signal special cases.</li>
 * </ul>
 *
 * <h3>Extensibility:</h3>
 * Subclasses should concentrate on rendering and input mapping for a specific
 * geometry (e.g. linear history plot, 2D lattice, 3D view). They should:
 * <ol>
 * <li>Populate and maintain the {@code data} array and {@code colorMap} as
 * needed.</li>
 * <li>Implement {@link #getCSSColorAt(int)} to return a valid CSS color string
 * for a node.</li>
 * <li>Implement {@link #drawLattice()} for static lattice-based layouts and
 * {@link #drawNetwork()} for network-based rendering. {@link #drawNetwork()} is
 * invoked after network layout is finished (or repeatedly during animated
 * layout).</li>
 * <li>Implement {@link #findNodeAt(int,int)} to map screen coordinates to node
 * indices and honor the documented special return values.</li>
 * </ol>
 * 
 * @param <T> the type for storing the color data
 * @param <N> the type of the network representation, 2D or 3D
 * 
 * @see PopGraph2D
 * @see PopGraph3D
 * @see Network2DGWT
 * @see Network3DGWT
 * 
 * @author Christoph Hauert
 */
@SuppressWarnings("java:S110")
public abstract class GenericPopGraph<T, N extends Network<?>> extends AbstractGraph<T[]>
		implements Network.LayoutListener, Zooming, DoubleClickHandler {

	/**
	 * The CSS class name for changing the cursor when hovering over a node.
	 */
	public static final String EVOLUDO_CURSOR_NODE = "evoludo-cursorPointNode";

	/**
	 * The structure of the population.
	 */
	protected AbstractGeometry geometry;

	/**
	 * The network representation of the population structure or {@code null} if not
	 * applicable.
	 */
	protected N network;

	/**
	 * The array to store the data for drawing the population structure.
	 */
	protected T[] data;

	/**
	 * Maximum number of nodes in network for animated layout.
	 */
	static final int MAX_ANIMATE_LAYOUT_VERTICES_DEFAULT = 1000;

	/**
	 * Maximum number of edges in network for animated layout.
	 */
	static final int MAX_ANIMATE_LAYOUT_LINKS_DEFAULT = 5000;

	/**
	 * The mode of the animation of the network layouting process.
	 */
	protected boolean animate = true;

	/**
	 * The flag to indicate whether the graph needs to be drawn.
	 * 
	 * @see #hasMessage()
	 */
	boolean noGraph = false;

	/**
	 * The flag to indicate whether the graph has been invalidated and needs to be
	 * redrawn.
	 */
	boolean invalidated = true;

	/**
	 * The map for translating discrete traits into colors.
	 */
	protected ColorMap<T> colorMap;

	/**
	 * The label of the graph.
	 */
	protected Label label;

	/**
	 * Create the base class for population graphs.
	 * 
	 * @param view   the view of this graph
	 * @param module the module backing the graph
	 */
	protected GenericPopGraph(AbstractView<?> view, Module<?> module) {
		super(view, module);
	}

	@Override
	protected void onLoad() {
		super.onLoad();
		label = new Label("Gugus");
		label.getElement().getStyle().setZIndex(1);
		label.setVisible(false);
		wrapper.add(label);
		if (view instanceof TooltipProvider.Index)
			setTooltipProvider((TooltipProvider.Index) view);
	}

	@Override
	protected void onUnload() {
		wrapper.remove(label);
		label = null;
		super.onUnload();
	}

	@Override
	public void activate() {
		super.activate();
		if (network != null)
			network.setLayoutListener(this);
		doubleClickHandler = addDoubleClickHandler(this);
	}

	@Override
	public void onResize() {
		super.onResize();
		if (hasMessage)
			view.layoutComplete();
	}

	/**
	 * Set the graph label to the string {@code msg} (no HTML formatting).
	 * 
	 * @param msg the text for the label of the graph
	 */
	public void setGraphLabel(String msg) {
		label.setText(msg);
		label.setVisible(msg != null && !msg.isEmpty());
	}

	/**
	 * Set the geometry backing the graph.
	 * 
	 * @param geometry the structure of the population
	 */
	@SuppressWarnings("unchecked")
	public void setGeometry(AbstractGeometry geometry) {
		this.geometry = geometry;
		// geometry (and network) may be null for ODE or SDE models
		if (geometry == null)
			return;
		setGraphLabel(geometry.getName());
		network = (N) (this instanceof PopGraph2D ? geometry.getNetwork2D() : geometry.getNetwork3D());
	}

	/**
	 * Get the geometry backing the graph.
	 * 
	 * @return the structure of the population
	 */
	public AbstractGeometry getGeometry() {
		return geometry;
	}

	/**
	 * Set the map for translating trait values into colors.
	 * 
	 * @param colorMap the trait-to-colour map
	 */
	public void setColorMap(ColorMap<T> colorMap) {
		this.colorMap = colorMap;
	}

	/**
	 * Get the map for translating trait values into colors.
	 * 
	 * @return the trait-to-colour map
	 */
	public ColorMap<T> getColorMap() {
		return colorMap;
	}

	/**
	 * Get the color data for all nodes as an array.
	 * 
	 * @return the array of node colors
	 */
	public T[] getData() {
		return data;
	}

	/**
	 * Get the network representation of the graph represented by the geometry.
	 * 
	 * @return the 2D network representation of this graph
	 */
	public N getNetwork() {
		return network;
	}

	@Override
	public boolean hasMessage() {
		return super.hasMessage() || noGraph;
	}

	@Override
	public void reset() {
		super.reset();
		invalidate();
	}

	/**
	 * Update the graph.
	 * 
	 * @param isNext {@code true} if the state has changed
	 */
	public void update(boolean isNext) {
		if (!view.isActive() || noGraph || hasMessage)
			return;
		if (invalidated || (isNext && geometry.isType(GeometryType.DYNAMIC))) {
			// defer layouting to allow 3D view to be up and running
			Scheduler.get().scheduleDeferred(() -> {
				if (hasStaticLayout()) {
					drawLattice();
					view.layoutComplete();
				} else
					layoutNetwork();
			});
		}
	}

	/**
	 * Check whether the layout of the graph is static, i.e. a lattice or lattice
	 * hierarchy.
	 * 
	 * @return {@code true} if the layout is static
	 */
	boolean hasStaticLayout() {
		return (geometry.isLattice()
				|| geometry.isType(GeometryType.HIERARCHY)
						&& ((HierarchicalGeometry) geometry).getSubType().isLattice());
	}

	/**
	 * Check whether the layout of the graph is animated.
	 * 
	 * @return {@code true} if the layout is animated
	 */
	boolean hasAnimatedLayout() {
		if (!animate)
			return false;
		GeometryFeatures gFeats = geometry.getFeatures();
		int nodeCount = geometry.getSize();
		return (nodeCount <= MAX_ANIMATE_LAYOUT_VERTICES_DEFAULT
				&& (int) (gFeats.avgTot * nodeCount) < 2 * MAX_ANIMATE_LAYOUT_LINKS_DEFAULT);
	}

	/**
	 * Invalidate the network. This forces networks to be regenerated.
	 */
	public void invalidate() {
		// geometry (and network) may be null for ODE or SDE models
		if (network != null)
			network.reset();
		clearMessage();
		invalidated = true;
		if (hasMessage)
			view.layoutComplete();
	}

	@Override
	public synchronized void layoutUpdate(double progress) {
		if (hasAnimatedLayout()) {
			if (progress > 0.0)
				layoutNetwork();
			return;
		}
		displayMessage("Laying out network...  " + Formatter.formatPercent(progress, 0) + " completed.");
	}

	@Override
	public synchronized void layoutComplete() {
		clearMessage();
		layoutNetwork();
		view.layoutComplete();
	}

	/**
	 * Draws structures with static layout of lattices.
	 * 
	 * @see #hasStaticLayout()
	 */
	protected abstract void drawLattice();

	/**
	 * Draws structures with resulting from dynamic layouting of network.
	 * 
	 * @see #hasStaticLayout()
	 */
	protected void layoutNetwork() {
		if (!network.isStatus(Status.HAS_LAYOUT) || geometry.isType(GeometryType.DYNAMIC))
			network.doLayout(this);
		network.finishLayout();
		drawNetwork();
	}

	/**
	 * Draws the network.
	 */
	protected abstract void drawNetwork();

	@Override
	public String getTooltipAt(int x, int y) {
		// no network may have been initialized (e.g. for ODE/SDE models)
		// when switching views the graph may not yet be ready to return
		// data for tooltips (colors == null)
		if (leftMouseButton || contextMenu.isVisible() || network == null ||
				network.isStatus(Status.LAYOUT_IN_PROGRESS))
			return null;
		int node = findNodeAt(x, y);
		if (node < 0) {
			element.removeClassName(EVOLUDO_CURSOR_NODE);
			return null;
		}
		element.addClassName(EVOLUDO_CURSOR_NODE);
		if (tooltipProvider instanceof TooltipProvider.Index)
			return ((TooltipProvider.Index) tooltipProvider).getTooltipAt(this, node);
		// last resort, try basic tooltip provider
		if (tooltipProvider != null)
			return tooltipProvider.getTooltipAt(node);
		// false alarm
		element.addClassName(EVOLUDO_CURSOR_NODE);
		return null;
	}

	/**
	 * Get the color of the node at index {@code node} as a CSS color string.
	 * 
	 * @param node the index of the node
	 * @return the color of the node
	 */
	public abstract String getCSSColorAt(int node);

	/**
	 * Return value if {@link #findNodeAt(int, int)} couldn't find a node at the
	 * mouse position.
	 */
	static final int FINDNODEAT_OUT_OF_BOUNDS = -1;

	/**
	 * Return value if {@link #findNodeAt(int, int)} isn't implemented for the
	 * particular backing geometry.
	 */
	static final int FINDNODEAT_UNIMPLEMENTED = -2;

	/**
	 * The index of the node that was hit by the mouse or a tap.
	 */
	protected int hitNode = -1;

	/**
	 * Find the index of the node at the location with coordinates {@code (x, y)}.
	 * 
	 * @param x the {@code x}-coordinate of the location
	 * @param y the {@code y}-coordinate of the location
	 * @return the index of the node
	 */
	public abstract int findNodeAt(int x, int y);

	@Override
	public void onDoubleClick(DoubleClickEvent event) {
		// ignore if busy or invalid node
		int node = findNodeAt(event.getX(), event.getY());
		if (node >= 0 && !view.isRunning()) {
			// population signals change back to us
			view.mouseHitNode(module.getId(), node, event.isAltKeyDown());
		}
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * The graph reacts to different kinds of touches:
	 * <dl>
	 * <dt>short touch with two fingers ({@code &lt;250} msec)
	 * <dd>display context menu.
	 * <dt>single long touch ({@code &gt;250} msec) on a node
	 * <dd>display the tooltip.
	 * <dt>long touch with two fingers ({@code &gt;250} msec)
	 * <dd>initiates pinching zoom.
	 * <dt>double tap on a node
	 * <dd>change the strategy of the node, if applicable.
	 * </dl>
	 * 
	 * @see ContextMenu.Provider
	 * @see #populateContextMenuAt(ContextMenu, int, int)
	 * @see #onTouchMove(TouchMoveEvent)
	 */
	@Override
	public void onTouchStart(TouchStartEvent event) {
		// super processes pinching
		super.onTouchStart(event);
		JsArray<Touch> touches = event.getTouches();
		if (touches.length() > 1) {
			// more than one touch point
			return;
		}
		Touch touch = touches.get(0);
		int x = touch.getRelativeX(element);
		int y = touch.getRelativeY(element);
		int node = findNodeAt(x, y);
		if (node < 0) {
			// no node touched
			tooltip.close();
			return;
		}
		if (Duration.currentTimeMillis() - touchEndTime > 250.0) {
			// single tap
			mouseX = x;
			mouseY = y;
			hitNode = node;
			return;
		}
		// double tap?
		if (!view.isRunning())
			// population signals change back to us
			view.mouseHitNode(module.getId(), node);
		event.preventDefault();
	}

	/**
	 * The context menu item for animating the layouting process.
	 */
	private ContextMenuCheckBoxItem animateMenu;

	/**
	 * The context menu item for rearranging networks through random shifts of node
	 * positions.
	 */
	private ContextMenuItem shakeMenu;

	/**
	 * The context menu item to clear the canvas. Only active for linear graphs to
	 * clear the history.
	 */
	private ContextMenuItem clearMenu;

	/**
	 * The context menu for visually exploring (or debugging) the updating process.
	 */
	private ContextMenu debugSubmenu;

	/**
	 * The context menu item for updating the current node.
	 */
	private ContextMenuItem debugNodeMenu;

	/**
	 * The context menu item for attaching the debug submenu.
	 */
	private ContextMenuItem debugSubmenuTrigger;

	/**
	 * The flag to indicate whether the debug submenu is activated. For example,
	 * debugging does not make sense if the nodes refer to states of PDE
	 * calculations.
	 */
	private boolean isDebugEnabled = true;

	/**
	 * Set whether the debugging menu is enabled.
	 * 
	 * @param enabled {@code true} to enable debugging
	 */
	public void setDebugEnabled(boolean enabled) {
		isDebugEnabled = enabled;
	}

	@Override
	public void populateContextMenuAt(ContextMenu menu, int x, int y) {
		if (hasMessage) {
			// skip or disable context menu entries
			super.populateContextMenuAt(menu, x, y);
			return;
		}
		addShakeMenu(menu);
		addAnimateMenu(menu);
		addClearMenu(menu);
		// process debug node update
		addDebugSubmenu(menu, x, y);

		super.populateContextMenuAt(menu, x, y);
	}

	/**
	 * Helper method to process the debug submenu logic for context menu.
	 * 
	 * @param menu the context menu to which the shake menu is added
	 */
	private void addShakeMenu(ContextMenu menu) {
		if (shakeMenu == null) {
			shakeMenu = new ContextMenuItem("Shake", () -> network.shake(GenericPopGraph.this, 0.05));
		}
		menu.add(shakeMenu);
		shakeMenu.setEnabled(!hasStaticLayout());
	}

	/**
	 * Helper method to process the debug submenu logic for context menu.
	 * 
	 * @param menu the context menu to which the animate menu is added
	 */
	private void addAnimateMenu(ContextMenu menu) {
		if (animateMenu == null) {
			animateMenu = new ContextMenuCheckBoxItem("Animate layout", () -> {
				animate = !animateMenu.isChecked();
				animateMenu.setChecked(animate);
			});
		}
		animateMenu.setChecked(animate);
		menu.add(animateMenu);
		animateMenu.setEnabled(!hasMessage && !hasStaticLayout());
	}

	/**
	 * Helper method to process the clear context menu.
	 * 
	 * @param menu the context menu to which the clear menu is added
	 */
	private void addClearMenu(ContextMenu menu) {
		// add menu to clear buffer (applies only to linear geometry)
		if (!hasHistory()) {
			clearMenu = null;
			return;
		}
		if (clearMenu == null) {
			clearMenu = new ContextMenuItem("Clear", () -> {
				clearHistory();
				paint(true);
			});
		}
		menu.add(clearMenu);
	}

	/**
	 * Helper method to process the debug submenu logic for context menu.
	 * 
	 * @param menu the context menu to which the debug submenu is added
	 * @param x    the x-coordinate of the mouse when the context menu was invoked
	 * @param y    the y-coordinate of the mouse when the context menu was invoked
	 */
	private void addDebugSubmenu(ContextMenu menu, int x, int y) {
		if (!isDebugEnabled) {
			return;
		}
		int debugNode = findNodeAt(x, y);
		if (debugNode >= 0 && debugNode < geometry.getSize()) {
			if (debugSubmenu == null) {
				debugSubmenu = new ContextMenu(menu, "Debug");
				debugNodeMenu = new ContextMenuItem("Update node @ -",
						() -> module.getIBSPopulation().debugUpdatePopulationAt(debugNode));
				debugSubmenu.add(debugNodeMenu);
			}
			debugNodeMenu.setText("Update node @ " + debugNode);
			debugNodeMenu.setEnabled(view.getModel().getType().isIBS());
			debugSubmenuTrigger = menu.add("Debug", debugSubmenu);
		}
		if (debugSubmenuTrigger != null)
			debugSubmenuTrigger.setEnabled(!view.isRunning());
	}
}
