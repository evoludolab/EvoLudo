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
import org.evoludo.simulator.Geometry;
import org.evoludo.simulator.Network;
import org.evoludo.simulator.Network.Status;
import org.evoludo.simulator.modules.Module;
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
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.Label;

/**
 * The base class for population graphs. This class provides the basic
 * functionality for drawing population structures, e.g. lattices or networks.
 * The class is abstract and must be subclassed to provide the actual drawing
 * functionality, including:
 * <ul>
 * <li>(dynamical) layouting of the population structure
 * <li>coloring of nodes according to their traits or fitness
 * <li>handling of mouse events
 * </ul>
 * The class is designed to be used in conjunction with a controller that
 * provides additional functionality, e.g. the ability to change the strategy of
 * a node or to display additional information.
 * 
 * @author Christoph Hauert
 * 
 * @param <T> the type for storing the color data
 * @param <N> the type of the network representation, 2D or 3D
 * 
 * @see PopGraph2D
 * @see PopGraph3D
 * @see Network2DGWT
 * @see Network3DGWT
 */
public abstract class GenericPopGraph<T, N extends Network> extends AbstractGraph<T[]>
		implements Network.LayoutListener, Zooming, DoubleClickHandler {

	/**
	 * The interface for communicating with graphs that show nodes, e.g. lattices or
	 * networks.
	 */
	public interface PopGraphController extends Controller {

		/**
		 * Notifies the controller that the mouse/tap has hit node with index
		 * {@code node} on the graph with the tag {@code id}.
		 * 
		 * @param id   the id of the graph
		 * @param node the index of the node that was hit
		 */
		public default void mouseHitNode(int id, int node) {
			mouseHitNode(id, node, false);
		}

		/**
		 * Notifies the controller that the mouse/tap has hit node with index
		 * {@code node} on the graph with the tag {@code id}. The flag {@code alt}
		 * indicates whether the {@code alt}-modifier was pressed
		 * 
		 * @param id   the id of the graph
		 * @param node the index of the node that was hit
		 * @param alt  {@code true} if the {@code alt}-key was pressed
		 */
		public default void mouseHitNode(int id, int node, boolean alt) {
		}

		/**
		 * Opportunity for the controller to add functionality to the context menu
		 * (optional implementation). Additional entries should be added to
		 * {@code menu}. If the context menu was opened while the mouse was over a node
		 * its index is {@code node}. At this point the menu already contains entries
		 * that are relevant for all graphs, e.g. fullscreen and export. Override this
		 * method to add further, more specialized entries. Finally, the current pane
		 * will be asked whether it wants to add further entries (e.g. autoscale axis).
		 *
		 * @param menu the context menu
		 * @param node the index of node
		 * 
		 * @see Controller#populateContextMenu(ContextMenu)
		 */
		public default void populateContextMenuAt(ContextMenu menu, int node) {
		}
	}

	/**
	 * The structure of the population.
	 */
	protected Geometry geometry;

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
	 * @param controller the controller of this graph
	 * @param module     the module backing the graph
	 */
	public GenericPopGraph(PopGraphController controller, Module module) {
		super(controller, module);
		label = new Label("Gugus");
		label.getElement().getStyle().setZIndex(1);
		label.setVisible(false);
		wrapper.add(label);
		if (controller instanceof TooltipProvider.Index)
			setTooltipProvider((TooltipProvider.Index) controller);
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
			controller.layoutComplete();
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
	public void setGeometry(Geometry geometry) {
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
	public Geometry getGeometry() {
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
		if (!isActive || noGraph || hasMessage)
			return;
		if (invalidated || (isNext && geometry.isDynamic)) {
			// defer layouting to allow 3D view to be up and running
			Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
				@Override
				public void execute() {
					if (hasStaticLayout()) {
						drawLattice();
						controller.layoutComplete();
					}
					else
						layoutNetwork();
				}
			});
		}
	}

	@Override
	public boolean paint(boolean force) {
		if (super.paint(force))
			return true;
		if (!force && !doUpdate())
			return true;
		return false;
	}

	/**
	 * Check whether the layout of the graph is static, i.e. a lattice or lattice
	 * hierarchy.
	 * 
	 * @return {@code true} if the layout is static
	 */
	boolean hasStaticLayout() {
		return (geometry.isLattice()
				|| geometry.getType() == Geometry.Type.HIERARCHY && geometry.subgeometry.isLattice());
	}

	/**
	 * Check whether the layout of the graph is animated.
	 * 
	 * @return {@code true} if the layout is animated
	 */
	boolean hasAnimatedLayout() {
		if (!animate)
			return false;
		return (geometry.size <= MAX_ANIMATE_LAYOUT_VERTICES_DEFAULT
				&& (int) (geometry.avgTot * geometry.size) < 2 * MAX_ANIMATE_LAYOUT_LINKS_DEFAULT);
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
			controller.layoutComplete();
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
		controller.layoutComplete();
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
		if (!network.isStatus(Status.HAS_LAYOUT) || geometry.isDynamic)
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
			element.removeClassName("evoludo-cursorPointNode");
			return null;
		}
		element.addClassName("evoludo-cursorPointNode");
		if (tooltipProvider instanceof TooltipProvider.Index)
			return ((TooltipProvider.Index) tooltipProvider).getTooltipAt(this, node);
		// last resort, try basic tooltip provider
		if (tooltipProvider != null)
			return tooltipProvider.getTooltipAt(node);
		// false alarm
		element.addClassName("evoludo-cursorPointNode");
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
		if (node >= 0 && !controller.isRunning()) {
			// population signals change back to us
			((PopGraphController) controller).mouseHitNode(module.getID(), node, event.isAltKeyDown());
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
		if (!controller.isRunning())
			// population signals change back to us
			((PopGraphController) controller).mouseHitNode(module.getID(), node);
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
		// process shake context menu
		if (shakeMenu == null) {
			shakeMenu = new ContextMenuItem("Shake", new Command() {
				@Override
				public void execute() {
					network.shake(GenericPopGraph.this, 0.05);
				}
			});
		}
		menu.add(shakeMenu);
		shakeMenu.setEnabled(!hasMessage && !hasStaticLayout());

		// process animate context menu
		if (animateMenu == null) {
			animateMenu = new ContextMenuCheckBoxItem("Animate layout", new Command() {
				@Override
				public void execute() {
					animate = !animateMenu.isChecked();
					animateMenu.setChecked(animate);
				}
			});
		}
		animateMenu.setChecked(animate);
		menu.add(animateMenu);
		animateMenu.setEnabled(!hasMessage && !hasStaticLayout());

		// add menu to clear buffer (applies only to linear geometry)
		if (geometry.getType() == Geometry.Type.LINEAR) {
			if (clearMenu == null) {
				clearMenu = new ContextMenuItem("Clear", new Command() {
					@Override
					public void execute() {
						clearHistory();
						paint(true);
					}
				});
			}
			menu.add(clearMenu);
		}

		// process debug node update
		if (isDebugEnabled) {
			int debugNode = findNodeAt(x, y);
			if (debugNode >= 0 && debugNode < geometry.size) {
				if (debugSubmenu == null) {
					debugSubmenu = new ContextMenu(menu);
					debugNodeMenu = new ContextMenuItem("Update node @ -", new Command() {
						@Override
						public void execute() {
							module.getIBSPopulation().debugUpdatePopulationAt(debugNode);
						}
					});
					debugSubmenu.add(debugNodeMenu);
				}
				debugNodeMenu.setText("Update node @ " + debugNode);
				debugNodeMenu.setEnabled(controller.getModel().isIBS());
				debugSubmenuTrigger = menu.add("Debug...", debugSubmenu);
			}
			if (debugSubmenuTrigger != null)
				debugSubmenuTrigger.setEnabled(!controller.isRunning());
		}

		super.populateContextMenuAt(menu, x, y);
	}
}
