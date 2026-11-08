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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.evoludo.geom.Node2D;
import org.evoludo.geom.Path2D;
import org.evoludo.geom.PathIterator;
import org.evoludo.geom.Point2D;
import org.evoludo.geom.Rectangle2D;
import org.evoludo.simulator.models.Model;
import org.evoludo.simulator.modules.Module;
import org.evoludo.simulator.views.AbstractView;
import org.evoludo.simulator.views.BasicTooltipProvider;
import org.evoludo.ui.ContextMenu;
import org.evoludo.ui.ContextMenuCheckBoxItem;
import org.evoludo.ui.ContextMenuItem;
import org.evoludo.ui.Gesture;
import org.evoludo.ui.Tooltip;
import org.evoludo.util.Formatter;
import org.evoludo.util.NativeJS;
import org.evoludo.util.RingBuffer;

import com.google.gwt.canvas.client.Canvas;
import com.google.gwt.canvas.dom.client.Context2d;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.dom.client.Style;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.dom.client.Touch;
import com.google.gwt.event.dom.client.ContextMenuEvent;
import com.google.gwt.event.dom.client.ContextMenuHandler;
import com.google.gwt.event.dom.client.DoubleClickEvent;
import com.google.gwt.event.dom.client.MouseDownEvent;
import com.google.gwt.event.dom.client.MouseDownHandler;
import com.google.gwt.event.dom.client.MouseMoveEvent;
import com.google.gwt.event.dom.client.MouseMoveHandler;
import com.google.gwt.event.dom.client.MouseUpEvent;
import com.google.gwt.event.dom.client.MouseUpHandler;
import com.google.gwt.event.dom.client.MouseWheelEvent;
import com.google.gwt.event.dom.client.MouseWheelHandler;
import com.google.gwt.event.dom.client.TouchCancelEvent;
import com.google.gwt.event.dom.client.TouchCancelHandler;
import com.google.gwt.event.dom.client.TouchEndEvent;
import com.google.gwt.event.dom.client.TouchEndHandler;
import com.google.gwt.event.dom.client.TouchMoveEvent;
import com.google.gwt.event.dom.client.TouchMoveHandler;
import com.google.gwt.event.dom.client.TouchStartEvent;
import com.google.gwt.event.dom.client.TouchStartHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.FocusPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.LayoutPanel;
import com.google.gwt.user.client.ui.RequiresResize;

/**
 * Abstract base class for 2D graphs rendered into a Canvas within the UI.
 * <p>
 * This class encapsulates common functionality required by a variety of
 * concrete graph implementations: managing the canvas and its high-DPI scaling,
 * calculating drawing bounds, handling user interactions (mouse and touch) for
 * panning and zooming, integrating with shared UI utilities (tooltip and
 * context menu), buffering historical data, and providing a comprehensive set
 * of drawing helpers for frames, axes, ticks, levels, markers and primitive
 * shapes.
 * </p>
 *
 * <h2>Generic parameter</h2>
 * <p>
 * The type parameter &lt;B&gt; denotes the element type stored in an optional
 * RingBuffer used for graphs that maintain historical data (for example
 * time-series or trajectory graphs).
 * </p>
 *
 * <h2>Primary responsibilities</h2>
 * <ul>
 * <li>Allocate and manage a Canvas element and a wrapped 2D rendering context
 * (MyContext2d) tuned for device pixel ratio (retina/high-DPI) support.
 * </li>
 * <li>Compute and maintain drawing bounds that leave room for frames, labels,
 * ticks and tick labels while exposing helper methods to draw full graph
 * frames (axes, tick marks, tick labels, grid levels and custom levels).
 * </li>
 * <li>Provide a pluggable styling model (GraphStyle) and helpers to render
 * axes labels, graph title, and common primitives (lines, circles, filled
 * paths, rectangles and vertical text).
 * </li>
 * <li>Offer a unified interaction model for shifting (panning) and zooming
 * including mouse drag, mouse wheel, single- and multi-touch gestures,
 * pinch-to-zoom, and inertial visual feedback via CSS classes and a
 * scheduled timer.
 * </li>
 * <li>Integrate with shared UI widgets: Tooltip (for hover/touch info) and
 * ContextMenu (for buffer size, log-scale and zoom actions). Subclasses
 * and the owning AbstractView can extend and populate the context menu.
 * </li>
 * <li>Maintain an optional RingBuffer&lt;B&gt; for historical data and provide
 * a
 * small context menu for configuring buffer capacity. Subclasses may
 * implement exporting of trajectory/history data when relevant.
 * </li>
 * </ul>
 *
 * <h2>Interfaces and Extension Points</h2>
 * <p>
 * AbstractGraph provides inner interfaces that concrete graphs and/or their
 * containing view may implement to customize interaction behavior:
 * </p>
 * <ul>
 * <li>Shifting — marker indicating the graph supports shifting. Implementing
 * classes can provide custom shift behavior by implementing Shifter,
 * and can optionally override mouse/touch handlers if they need finer
 * control.</li>
 * <li>Shifter — single method shift(int dx, int dy) used by the base class
 * to move the view corner when panning. The default implementation in
 * AbstractGraph updates the viewCorner constrained to valid extents.</li>
 * <li>Zooming — marker indicating the graph supports zooming. It extends
 * Zoomer and MouseWheelHandler. It documents a default ZOOM_INCR and
 * ZOOM_MAX. Implementations can override zoom behaviour; otherwise the
 * base-class provides sensible defaults.</li>
 * <li>Zoomer — single method zoom(double zoom, int x, int y) used to apply
 * zoom centered on a particular screen coordinate. Subclasses or the
 * containing view can provide custom coordinate-space or content-level
 * behavior by implementing Zoomer.</li>
 * <li>HasLogScaleY — marker for graphs that may support logarithmic Y
 * scaling; the graph will enable/disable the log-y context menu item
 * appropriately.</li>
 * <li>HasTrajectory — marker for graphs that can export trajectories; such
 * graphs should implement an exportTrajectory(StringBuilder) routine.</li>
 * </ul>
 *
 * <h2>Lifecycle</h2>
 * <p>
 * Typical lifecycle methods:
 * </p>
 * <ul>
 * <li>onLoad() — allocates the LayoutPanel wrapper and Canvas, obtains shared
 * Tooltip and ContextMenu instances and configures them, and sets the
 * rendering scale based on device pixel ratio.</li>
 * <li>onUnload() — removes and nulls references to DOM components and
 * unregisters from shared widgets.</li>
 * <li>activate() — attaches mouse and touch handlers for zooming and
 * shifting depending on which interfaces are implemented and on the
 * environment (e.g. special handling for ePub readers).</li>
 * <li>deactivate() — removes all registered input handlers, closes tooltip
 * and context menu, and cancels transient state.</li>
 * <li>onResize() — closes transient UI (tooltip/context menu), updates the
 * canvas coordinate space and recalculates drawing bounds.</li>
 * </ul>
 *
 * <h2>Interaction semantics</h2>
 * <ul>
 * <li>Mouse wheel: zooms with inertia. The wheel delta is converted to a
 * multiplicative zoom factor (ZOOM_INCR ^ -deltaY) and the zoom center
 * is the current mouse position. A CSS class is briefly applied while
 * inertia is present.</li>
 * <li>Mouse drag with left button: initiates panning (shifting) of the
 * viewport when the graph implements Shifting. A CSS class is applied
 * to indicate view movement while dragging.</li>
 * <li>Touch: short taps and long touches are distinguished. Long single
 * touches allow panning; two-finger long touches initiate pinch-to-zoom
 * with the center of the pinch used as zoom focus. Touch handlers
 * preventDefault() for panning and pinch gestures to avoid scrolling the
 * page when interacting with the graph.</li>
 * <li>All interactions are no-ops when a textual message is shown on the
 * graph (hasMessage flag) to avoid conflicting updates.</li>
 * </ul>
 *
 * <h2>Canvas &amp; scaling</h2>
 * <p>
 * The canvas coordinate space is scaled by the detected device pixel ratio to
 * support crisp rendering on high-DPI displays. updateCanvas() sets both the
 * CSS pixel size and the coordinate space (scaled) size; note that setting
 * these properties clears the canvas, so callers should repaint after resizing.
 * </p>
 *
 * <h2>Painting and Helpers</h2>
 * <p>
 * Subclasses must implement export(MyContext2d) and typically override paint()
 * to perform actual rendering. AbstractGraph provides:
 * </p>
 * <ul>
 * <li>Utility primitives for drawing and filling complex paths efficiently
 * (sketch/stroke/fill) with segmentation to avoid freezing the UI.</li>
 * <li>Convenience methods for drawing frames, axis labels, ticks, tick
 * labels (including percent/log formatting), grid levels, and custom
 * level lines.</li>
 * <li>Primitives for circles, lines, rectangles and vertical text.</li>
 * <li>Coordinate conversion helpers to map browser coordinates to scaled
 * [0..1] coordinates relative to the drawing bounds
 * (convertToScaledCoordinates).</li>
 * </ul>
 *
 * <h2>Bounds and layout</h2>
 * <p>
 * calcBounds computes the inner drawing rectangle available for graph content,
 * factoring in style-determined padding, frame width, label space, tick length
 * and tick label widths. adjustBoundsForLabels() and
 * adjustBoundsForTickLabels()
 * are used to reserve space for axis and tick labels. The bounds are used by
 * frame and drawing methods to position elements consistently.
 * </p>
 *
 * <h2>History &amp; buffering</h2>
 * <p>
 * When a RingBuffer&lt;B&gt; is present, hasHistory() returns true and a small
 * buffer-size submenu is added to the context menu to select capacities (5k,
 * 10k, 50k, 100k). Subclasses that maintain historical data should manage
 * insertion into the buffer and may use prependTime2Data helper to attach a
 * timestamp to stored data points.
 * </p>
 *
 * <h2>Tooltips and context menu</h2>
 * <p>
 * A shared Tooltip instance is used to provide hover/touch information. Context
 * menu population is extensible — AbstractGraph populates buffer-size, log-Y
 * toggle and zoom items when applicable and then delegates to the owning
 * AbstractView for additional items.
 * </p>
 *
 * <h2>Messages</h2>
 * <p>
 * displayMessage(String) can be used to show a centered textual message as an
 * overlay on the graph (e.g. "No Data" or "Paused"). When a message is
 * displayed most interaction handlers become inert and painting/updates are
 * suppressed until the message is cleared via clearMessage().
 * </p>
 *
 * <h2>Utilities &amp; Helper classes</h2>
 * <ul>
 * <li>MyContext2d — a thin subclass of Context2d that exposes setLineDash for
 * dashed lines.</li>
 * <li>ZoomCommand — small Command implementation used by context menu items
 * to apply zoom factors.</li>
 * </ul>
 *
 * <h2>Notes and implementation details</h2>
 * <ul>
 * <li>Drawing very large paths is split into segments (MAX_SEGEMENTS) and
 * periodically closed/stroked to avoid performance bottlenecks and UI
 * freezing.</li>
 * <li>Many protected helper methods are provided so subclasses can reuse the
 * standardized rendering behavior and visuals defined by GraphStyle.</li>
 * <li>AbstractGraph intentionally separates coordinate-space transforms (via
 * viewCorner and zoomFactor) from content rendering so subclasses can
 * assume painting inside normalized bounds and apply their own content
 * transforms as needed.</li>
 * </ul>
 *
 * @author Christoph Hauert
 * 
 * @param <B> the type of buffer backing the graph (may be null if the
 *            graph does not use a RingBuffer): typically this is
 *            {@code double[]} but in some cases {@code Color[]},
 *            {@code String[]} or
 *            {@link thothbot.parallax.core.shared.materials.MeshLambertMaterial
 *            MeshLambertMaterial[]} for
 *            {@link org.evoludo.simulator.views.Pop2D Pop2D},
 *            {@link org.evoludo.graphics.PopGraph2D PopGraph2D} or
 *            {@link org.evoludo.graphics.PopGraph3D PopGraph3D}, respectively.
 * 
 * @see #activate()
 * @see #deactivate()
 * @see #paint()
 * @see #resetView()
 * @see #shift(int,int)
 * @see AbstractGraph.Shifting
 * @see AbstractGraph.Shifter
 * @see AbstractGraph.Zooming
 * @see AbstractGraph.Zoomer
 */
@SuppressWarnings("java:S110")
public abstract class AbstractGraph<B> extends FocusPanel
		implements MouseWheelHandler, MouseDownHandler, MouseUpHandler, MouseMoveHandler, //
		TouchStartHandler, TouchEndHandler, TouchMoveHandler, TouchCancelHandler, //
		RequiresResize, Tooltip.Provider, ContextMenu.Listener, ContextMenu.Provider {

	/**
	 * Graphs that support shifting should implement this interface. Basic shifting
	 * capabilities are handled by {@link AbstractGraph}.
	 */
	public interface Shifting extends Shifter, MouseDownHandler, MouseUpHandler, MouseMoveHandler, //
			TouchStartHandler, TouchEndHandler, TouchMoveHandler {
	}

	/**
	 * Graphs that support shifting of their view should implement this interface.
	 * Basic shifting is provided by {@link AbstractGraph} and is automatically
	 * enabled unless {@link AbstractView} is an instance of
	 * {@code Shifter}.
	 */
	public interface Shifter {
		/**
		 * Shift the (zoomed) graph within the view port by {@code (dx, dy)}. Positive
		 * {@code dx} shift the graph to the right and positive {@code dy} shift it
		 * upwards.
		 * 
		 * @param dx the horizontal shift of the graph
		 * @param dy the vertical shift of the graph
		 */
		public void shift(int dx, int dy);
	}

	/**
	 * Graphs that support zooming should implement this interface. Basic zooming
	 * capabilities are handled by {@link AbstractGraph}.
	 * 
	 * @see AbstractGraph#resetView()
	 * @see AbstractGraph#zoom(double)
	 * @see AbstractGraph#zoom(double, double, double)
	 * @see AbstractGraph#zoom(double, int, int)
	 */
	public interface Zooming extends Zoomer, MouseWheelHandler {

		/**
		 * The factor for increasing/decreasing the zoom level on
		 * {@link MouseWheelEvent} or {@link TouchMoveEvent}s.
		 * 
		 * @see #zoom(double, int, int)
		 */
		public final double ZOOM_INCR = 1.01;

		/**
		 * The maximum zoom level.
		 */
		public final double ZOOM_MAX = 25.0;
	}

	/**
	 * Graphs that support zooming should implement this interface. Basic zooming is
	 * provided by {@link AbstractGraph} and is automatically enabled unless
	 * {@link AbstractView} is an instance of {@code Zoomer}.
	 */
	public interface Zoomer {

		/**
		 * Adjust zoom level by the factor {@code zoom} with the center at coordinates
		 * {@code (x,y)} (in display coordinates as provided by event listeners).
		 *
		 * @param zoom the new zoom level
		 * @param x    the {@code x}-coordinate of the zoom center
		 * @param y    the {@code y}-coordinate of the zoom center
		 * 
		 * @see #onMouseWheel(MouseWheelEvent)
		 * @see #onTouchMove(TouchMoveEvent)
		 */
		public void zoom(double zoom, int x, int y);

		/**
		 * Adjust zoom level by the factor {@code zoom}. Leave the center of the view in
		 * place.
		 *
		 * @param zoom the new zoom level
		 */
		public void zoom(double zoom);

		/**
		 * Reset the view to its defaults, or as specified by the options of the view,
		 * including zooming and shifting.
		 */
		public void resetView();
	}

	/**
	 * Graphs that support logarithmic scaling on the y-axis should implement this
	 * interface.
	 */
	public interface HasLogScaleY {
	}

	/**
	 * Graphs that show trajectories and support exporting their data should
	 * implement this interface.
	 */
	public interface HasTrajectory {

		/**
		 * Export the trajectory of the graph to {@code export}.
		 * 
		 * @param export the string builder to export the trajectory
		 */
		public void exportTrajectory(StringBuilder export);
	}

	/**
	 * The handler for {@link DoubleClickEvent}s.
	 */
	HandlerRegistration doubleClickHandler;

	/**
	 * The handler for {@link MouseDownEvent}s.
	 */
	HandlerRegistration mouseDownHandler;

	/**
	 * The handler for {@link MouseUpEvent}s.
	 */
	HandlerRegistration mouseUpHandler;

	/**
	 * The handler for {@link MouseMoveEvent}s.
	 */
	HandlerRegistration mouseMoveHandler;

	/**
	 * The handler for {@link MouseWheelEvent}s.
	 */
	HandlerRegistration mouseWheelHandler;

	/**
	 * The handler for {@link TouchStartEvent}s.
	 */
	HandlerRegistration touchStartHandler;

	/**
	 * The handler for {@link TouchEndEvent}s.
	 */
	HandlerRegistration touchEndHandler;

	/**
	 * The handler for {@link TouchMoveEvent}s.
	 */
	HandlerRegistration touchMoveHandler;

	/**
	 * The handler for {@link TouchCancelEvent}s.
	 */
	HandlerRegistration touchCancelHandler;

	/**
	 * The current zoom level.
	 */
	protected double zoomFactor = 1.0;

	/**
	 * The coordinates of the lower left corner visible on the canvas.
	 */
	protected Point2D viewCorner;

	/**
	 * The view of this graph.
	 */
	protected final AbstractView<?> view;

	/**
	 * The controller for shifting this graph.
	 */
	Shifter shifter;

	/**
	 * The controller for zooming this graph.
	 */
	Zoomer zoomer;

	/**
	 * The reference to the (shared) context menu.
	 */
	protected ContextMenu contextMenu;

	/**
	 * The reference to the (shared) tooltip.
	 */
	protected Tooltip tooltip;

	/**
	 * The provider for tooltips.
	 */
	BasicTooltipProvider tooltipProvider;

	/**
	 * Set the provider for tooltips.
	 * 
	 * @param tooltipProvider the provider for tooltips
	 */
	public void setTooltipProvider(BasicTooltipProvider tooltipProvider) {
		this.tooltipProvider = tooltipProvider;
	}

	/**
	 * The scale of this graph. Used to translate {@code width} and {@code height}
	 * into canvas coordinates. For example, on retina displays the scale is
	 * typically {@code 2}, i.e. two pixels per unit width or height.
	 * 
	 * @see NativeJS#getDevicePixelRatio()
	 * @see Canvas#setCoordinateSpaceWidth(int)
	 * @see Canvas#setCoordinateSpaceHeight(int)
	 */
	protected double scale = 1.0;

	/**
	 * The wrapper element which displays the canvas. Subclasses may use this to add
	 * further elements to the graph such as labels.
	 */
	LayoutPanel wrapper;

	/**
	 * Overlay label used to display graph messages.
	 */
	protected Label msgLabel;

	/**
	 * Handle to the object's underlying DOM element.
	 */
	protected Element element;

	/**
	 * Handle to the canvas for drawing the graphics.
	 */
	protected Canvas canvas;

	/**
	 * Handle to the graphical context for drawing on the canvas.
	 */
	protected MyContext2d g;

	/**
	 * The array of colors used for drawing the graph.
	 */
	protected String[] colors;

	/**
	 * Markers for decorating the graph.
	 */
	protected List<double[]> markers = Collections.emptyList();

	/**
	 * The array of colors used for markers.
	 */
	protected String[] markerColors;

	/**
	 * The module backing the graph.
	 */
	protected Module<?> module;

	/**
	 * The bounds of the displaying area. This excludes any frames and/or axes
	 * labels.
	 */
	protected Rectangle2D bounds = new Rectangle2D();

	/**
	 * The style of the graph. Includes labels, fonts, etc.
	 */
	protected GraphStyle style = new GraphStyle();

	/**
	 * Horizontal gap between y ticks and their labels.
	 */
	private static final double Y_TICK_LABEL_GAP = 4.0;

	/**
	 * Minimum horizontal gap between y tick labels and the y-axis label.
	 */
	private static final double Y_AXIS_LABEL_MIN_GAP = 6.0;

	/**
	 * Default horizontal gap reserved between y tick labels and the y-axis label.
	 * This exceeds {@link #Y_AXIS_LABEL_MIN_GAP} so modest tick-label width changes
	 * can be absorbed without moving the axis label.
	 */
	private static final double Y_AXIS_LABEL_RESERVED_GAP = 10.0;

	/**
	 * Approximate horizontal thickness reserved for the rotated y-axis label.
	 */
	private static final double Y_AXIS_LABEL_THICKNESS = 12.0;

	/**
	 * Optional override for the horizontal offset of the y-axis label. When this is
	 * not set, the offset is derived from the cached y-axis tick-label width and
	 * the configured label gap.
	 */
	protected double yAxisLabelTickSkip = Double.NaN;

	/**
	 * Cached width of the midpoint y tick label from the most recent bounds
	 * calculation.
	 */
	private double cachedMidYTickLabelWidth = Double.NaN;

	/**
	 * The buffer to store historical data, if applicable.
	 */
	protected RingBuffer<B> buffer;

	/**
	 * The minimum buffer size.
	 */
	protected static final int MIN_BUFFER_SIZE = 5000;

	/**
	 * The default buffer size. Must be at least {@code MIN_BUFFER_SIZE}.
	 */
	protected static final int DEFAULT_BUFFER_SIZE = 10000;

	/**
	 * Return the {@link RingBuffer<double[]>} containing historical data, if
	 * applicable.
	 * 
	 * @return the buffer with historical data or {@code null}
	 */
	public RingBuffer<B> getBuffer() {
		return buffer;
	}

	/**
	 * CSS class applied when the user is grabbing a node.
	 */
	static final String CURSOR_GRAB_NODE_CLASS = "evoludo-cursorGrabNode";

	/**
	 * Create the base class for graphs. Allocates the canvas, retrieves the shared
	 * tooltip and context menu. Use the CSS class {@code evoludo-Canvas2D} for
	 * custom formatting of the canvas element.
	 * 
	 * @param view   the view of this graph
	 * @param module the module backing the graph
	 */
	protected AbstractGraph(AbstractView<?> view, Module<?> module) {
		this.view = view;
		this.module = module;
		if (this instanceof Zooming) {
			viewCorner = new Point2D();
			zoomer = (Zoomer) (view instanceof Zoomer ? view : this);
		}
		if (this instanceof Shifting) {
			// Zooming may already have taken care of this
			if (viewCorner == null)
				viewCorner = new Point2D();
			shifter = (Shifter) (view instanceof Shifter ? view : this);
		}

		markerColors = new String[] { "rgb(0,0,0,0.4)" };
	}

	@Override
	protected void onLoad() {
		super.onLoad();
		wrapper = new LayoutPanel();
		add(wrapper);
		element = getElement();
		msgLabel = new Label();
		msgLabel.setStyleName("evoludo-Message");
		msgLabel.getElement().getStyle().setZIndex(2);
		msgLabel.setVisible(false);
		wrapper.add(msgLabel);
		wrapper.setWidgetLeftRight(msgLabel, 0.0, Unit.PX, 0.0, Unit.PX);
		// most graphs use canvas - allocate it here (this is unnecessary for e.g.
		// PopGraph3D but since this is the only exception so far it will have to remove
		// canvas again)
		canvas = Canvas.createIfSupported();
		canvas.setStylePrimaryName("evoludo-Canvas2D");
		g = canvas.getContext2d().cast();
		wrapper.add(canvas);
		canvas.getParent().getElement().getStyle().setHeight(100, Unit.PCT);
		scale = NativeJS.getDevicePixelRatio();
		contextMenu = ContextMenu.sharedContextMenu();
		contextMenu.addListenerWithProvider(this, this);
		tooltip = Tooltip.sharedTooltip();
		tooltip.add(this, this);
		// no delays - at least for debugging...
		// weird delay remains on ibooks (ios)
		tooltip.setDelays(0, 0);
	}

	@Override
	protected void onUnload() {
		if (msgLabel != null)
			msgLabel.removeFromParent();
		msgLabel = null;
		if (canvas != null)
			canvas.removeFromParent();
		canvas = null;
		contextMenu.removeListener(this);
		tooltip.removeListener(this);
		remove(wrapper);
		wrapper = null;
		super.onUnload();
	}

	/**
	 * The options most recently parsed by this graph, empty if none, or
	 * {@code null} to force parsing.
	 *
	 * @see #parse(String)
	 * @see #resetView()
	 */
	protected String options = "";

	/**
	 * Restore the default settings of the graph and parse the options
	 * {@code args}, unless they were already parsed. This preserves settings
	 * changed through the GUI, e.g. the context menu, when the model is reset or
	 * parameters change. Default implementation does nothing.
	 *
	 * @param args the options for the graph, empty for the defaults
	 * @return {@code true} if parsing was successful
	 */
	public boolean parse(String args) {
		return true;
	}

	/**
	 * Perform necessary preparations to show the graph in the GUI. Attaches mouse
	 * and touch handlers for graphs that implement {@link Zooming} or
	 * {@link Shifting} interfaces.
	 * 
	 * @see #deactivate()
	 */
	public void activate() {
		if (this instanceof Zooming) {
			boolean isEPub = NativeJS.isEPub();
			// important: ibooks (desktop) returns ePubReader for standalone pages as well,
			// i.e. isEPub is true
			// however, ibooks (ios) does not report as an ePubReader for standalone pages,
			// i.e. isEPub is false
			boolean ePubStandalone = (Document.get().getElementById("evoludo-standalone") != null);
			if (!isEPub || ePubStandalone)
				mouseWheelHandler = addMouseWheelHandler(this);
			touchStartHandler = addTouchStartHandler(this);
			touchEndHandler = addTouchEndHandler(this);
			touchCancelHandler = addTouchCancelHandler(this);
		}
		if (this instanceof Shifting) {
			mouseDownHandler = addMouseDownHandler(this);
			mouseUpHandler = addMouseUpHandler(this);
			if (touchStartHandler == null)
				touchStartHandler = addTouchStartHandler(this);
			if (touchEndHandler == null)
				touchEndHandler = addTouchEndHandler(this);
			if (touchCancelHandler == null)
				touchCancelHandler = addTouchCancelHandler(this);
		}
	}

	/**
	 * The graph is removed from the GUI. Opportunity for some clean up. Removes
	 * <em>all</em> mouse and touch handlers from graph.
	 * 
	 * @see #activate()
	 */
	public void deactivate() {
		tooltip.close();
		contextMenu.close();
		leftMouseButton = false;
		gesture.release();
		if (doubleClickHandler != null)
			doubleClickHandler.removeHandler();
		doubleClickHandler = null;
		if (mouseDownHandler != null)
			mouseDownHandler.removeHandler();
		mouseDownHandler = null;
		if (mouseMoveHandler != null)
			mouseMoveHandler.removeHandler();
		mouseMoveHandler = null;
		if (mouseWheelHandler != null)
			mouseWheelHandler.removeHandler();
		mouseWheelHandler = null;
		if (mouseUpHandler != null)
			mouseUpHandler.removeHandler();
		mouseUpHandler = null;
		if (touchEndHandler != null)
			touchEndHandler.removeHandler();
		touchEndHandler = null;
		if (touchMoveHandler != null)
			touchMoveHandler.removeHandler();
		touchMoveHandler = null;
		if (touchStartHandler != null)
			touchStartHandler.removeHandler();
		touchStartHandler = null;
		if (touchCancelHandler != null)
			touchCancelHandler.removeHandler();
		touchCancelHandler = null;
	}

	@Override
	public void onResize() {
		tooltip.close();
		contextMenu.close();
		String layoutMessage = null;
		if (hasMessage && this instanceof GenericPopGraph<?, ?>) {
			GenericPopGraph<?, ?> graph = (GenericPopGraph<?, ?>) this;
			if (graph.network != null
					&& graph.network.isStatus(org.evoludo.simulator.Network.Status.LAYOUT_IN_PROGRESS))
				layoutMessage = message;
		}
		updateCanvas();
		// check bounds and update messages if necessary
		calcBounds();
		if (layoutMessage != null && !hasMessage)
			displayMessage(layoutMessage);
	}

	/**
	 * Update the canvas size and coordinate space dimensions.
	 * 
	 * <strong>IMPORTANT:</strong> Setting the canvas size or the coordinate space
	 * dimensions clears the canvas (even with no actual changes)!
	 */
	private void updateCanvas() {
		// the wrapper excludes the (CSS) padding of the graph, i.e. it is the
		// visible area of the canvas
		int width = (wrapper == null ? 0 : wrapper.getOffsetWidth());
		int height = (wrapper == null ? 0 : wrapper.getOffsetHeight());
		// canvas is null for PopGraph3D
		if (canvas == null || width == 0 || height == 0)
			return;
		int sw = (int) (scale * width);
		int sh = (int) (scale * height);
		if (canvas.getCoordinateSpaceWidth() != sw || canvas.getCoordinateSpaceHeight() != sh) {
			canvas.setPixelSize(width, height);
			canvas.setCoordinateSpaceWidth(sw);
			canvas.setCoordinateSpaceHeight(sh);
		}
	}

	/**
	 * Reset the graph. Clear canvas and messages. The view, i.e. zooming, shifting
	 * and axis ranges, is retained; with autoscaling the ranges follow the data of
	 * the new run, see {@link GraphStyle#trackYRange(double, double)}.
	 */
	public void reset() {
		clearMessage();
		clearHistory();
		updateCanvas();
		// the room for tick labels adjusts to the new run
		drawnYTickLabelWidth = 0.0;
		// recalculate bounds so that any message reflecting a still-valid display
		// constraint (e.g. population too big) is restored rather than left cleared
		// with stale, unusable dimensions
		calcBounds();
	}

	/**
	 * Initialize the graph, e.g. for a new run. Do not clear graph. The default
	 * does nothing.
	 */
	public void init() {
		// nothing to initialize by default
	}

	/**
	 * Draw the graph.
	 */
	public void paint() {
		paint(false);
	}

	/**
	 * Get the module that backs the graph.
	 * 
	 * @return the module
	 */
	@SuppressWarnings("java:S1452") // impossible to specify generic type here
	public Module<?> getModule() {
		return module;
	}

	/**
	 * Draw the graph. For re-drawing the graph, set {@code force} to {@code true}.
	 * 
	 * @param force {@code true} to force re-drawing of graph
	 * @return {@code true} if painting skipped
	 */
	public boolean paint(boolean force) {
		syncViewCorner();
		if (!view.isActive() || hasMessage)
			return true;
		tooltip.update();
		return false;
	}

	/**
	 * The width of the bounds that {@link #viewCorner} refers to.
	 */
	private double viewWidth;

	/**
	 * The height of the bounds that {@link #viewCorner} refers to.
	 */
	private double viewHeight;

	/**
	 * Keep the visible region of zoomed graphs in place when the size of the
	 * bounds changes, e.g. on resizing or toggling fullscreen. The view corner is
	 * measured in pixels and hence scaled to the new size of the bounds.
	 */
	private void syncViewCorner() {
		double w = bounds.getWidth();
		double h = bounds.getHeight();
		if (viewCorner == null || w <= 0.0 || h <= 0.0)
			return;
		if (viewWidth > 0.0 && viewHeight > 0.0)
			viewCorner.set(viewCorner.getX() * w / viewWidth, viewCorner.getY() * h / viewHeight);
		viewWidth = w;
		viewHeight = h;
	}

	/**
	 * Assign a list of markers to the graph.
	 * 
	 * @param markers the list of markers
	 */
	public void setMarkers(List<double[]> markers) {
		setMarkers(markers, null);
	}

	/**
	 * Assign a list of markers with custom colors to the graph.
	 * 
	 * @param markers the list of markers
	 * @param colors  the list of custom colors
	 */
	public void setMarkers(List<double[]> markers, String[] colors) {
		if (colors == null)
			colors = new String[] { "rgb(0,0,0,0.4)" };
		this.markers = (markers == null ? Collections.emptyList() : markers);
		markerColors = colors;
	}

	/**
	 * Assign a list of colors to the graph.
	 * 
	 * @param colors the list of colors
	 */
	public void setColors(String[] colors) {
		this.colors = colors;
	}

	/**
	 * Check whether the graph entertains a buffer with historical data.
	 * 
	 * @return {@code true} if the graph stores the data history
	 */
	public boolean hasHistory() {
		return (buffer != null);
	}

	/**
	 * Clear the history of the graph (if there is one).
	 */
	public void clearHistory() {
		if (buffer != null)
			buffer.clear();
	}

	/**
	 * Utility method to prepend time to data point.
	 * 
	 * @param t    the time
	 * @param data the data
	 * @return the new data array including time
	 */
	protected double[] prependTime2Data(double t, double[] data) {
		double[] tdata = new double[data.length + 1];
		System.arraycopy(data, 0, tdata, 1, data.length);
		tdata[0] = t;
		return tdata;
	}

	/**
	 * The flag to indicate whether the graph supports zoom. The default is no
	 * support.
	 */
	protected boolean hasZoom = false;

	/**
	 * {@inheritDoc}
	 * <p>
	 * Adds buffer size menu and queries the view to add further
	 * functionality.
	 * 
	 * @see AbstractView#populateContextMenu(ContextMenu)
	 */
	@Override
	public void populateContextMenuAt(ContextMenu menu, int x, int y) {
		addLogScaleMenu(menu);
		addZoomMenu(menu);
		if (menu.getWidgetCount() > 0 && tooltip.isVisible())
			tooltip.close();
		view.populateContextMenu(menu);
	}

	/**
	 * Opportunity for graph subclasses to contribute graph-local entries to the
	 * shared axes submenu assembled by the owning view.
	 *
	 * @param menu the axes submenu to populate
	 */
	public void populateLocalAxesMenu(ContextMenu menu) {
	}

	/**
	 * Add a clear-history entry to the context menu when this graph stores history
	 * and a reset-view entry when it supports zooming. The view decides which
	 * graphs are affected.
	 *
	 * @param menu the context menu to populate
	 *
	 * @see AbstractView#clearHistory(AbstractGraph)
	 * @see Zoomer#resetView()
	 */
	protected void addClearMenu(ContextMenu menu) {
		int nItems = menu.getWidgetCount();
		if (hasHistory())
			menu.add(new ContextMenuItem("Clear", () -> view.clearHistory(this)));
		if (zoomer != null)
			menu.add(new ContextMenuItem("Reset view", zoomer::resetView));
		if (menu.getWidgetCount() > nItems)
			menu.addSeparator();
	}

	/**
	 * Add logarithmic scale menu item to context menu if graph supports log scale
	 * on
	 * {@code y}-axis.
	 * 
	 * @param menu the context menu to populate
	 */
	protected void addLogScaleMenu(ContextMenu menu) {
		if (this instanceof HasLogScaleY) {
			ContextMenuCheckBoxItem logYMenu = new ContextMenuCheckBoxItem("Logarithmic y-axis",
					(ScheduledCommand) () -> {
						setLogY(!style.logScaleY);
						paint(true);
					});
			logYMenu.setChecked(style.logScaleY);
			logYMenu.setEnabled(style.yMin >= 0.0);
			menu.add(logYMenu);
		}
	}

	/**
	 * Add zoom menu items to context menu if graph supports zooming.
	 * 
	 * @param menu the context menu to populate
	 */
	protected void addZoomMenu(ContextMenu menu) {
		if (!(this instanceof Zooming))
			return;
		ContextMenu zoomMenu = new ContextMenu(menu);
		populateZoomMenu(zoomMenu);
		if (zoomMenu.getWidgetCount() > 0)
			menu.add("Zoom", zoomMenu);
	}

	/**
	 * Populate the zoom submenu with standard zoom actions.
	 * 
	 * @param menu the zoom submenu to populate
	 */
	protected void populateZoomMenu(ContextMenu menu) {
		menu.add(new ContextMenuItem("Zoom in (2x)", new ZoomCommand(2.0)));
		menu.add(new ContextMenuItem("Zoom out (0.5x)", new ZoomCommand(0.5)));
	}

	/**
	 * Set the {@code y}-axis to logarithmic scale if {@code logY} is {@code true}
	 * and if {@code yMin &ge; 0}. If {@code yMin == 0} it is increased to
	 * {@code 0.01 * yMax}. If {@code yMin < 0} logarithmic scale requests are
	 * ignored.
	 * 
	 * @param logY {@code true} to set logarithmic scale on {@code y}-axis
	 */
	void setLogY(boolean logY) {
		if (style.yMin < 0.0 || !logY) {
			style.logScaleY = false;
			return;
		}
		// log scale requested
		style.logScaleY = true;
		if (style.yMin == 0.0) {
			// increase to 1% of yMax
			style.yMin = 0.01 * style.yMax;
		}
	}

	/**
	 * Checks if the (browser) coordinates {@code (x, y)} are inside this graph.
	 * This is useful for controllers that may manage several graphs.
	 * 
	 * @param x the {@code x}-coordinate to check
	 * @param y the {@code y}-coordinate to check
	 * @return {@code true} if {@code (x, y)} is inside graph
	 */
	public boolean contains(int x, int y) {
		x -= element.getAbsoluteLeft();
		if (x < 0 || x > element.getOffsetWidth())
			return false;
		y -= element.getAbsoluteTop();
		return (y >= 0 && y <= element.getOffsetHeight());
	}

	/**
	 * Convert the (browser) coordinates {@code (x, y)} to scaled coordinates in
	 * \([0,1]^2\).
	 * 
	 * @param x the {@code x}-coordinate to convert
	 * @param y the {@code y}-coordinate to convert
	 * @return the converted coordinates
	 */
	public Point2D convertToScaledCoordinates(int x, int y) {
		return convertToScaledCoordinates(x, y, new Point2D());
	}

	/**
	 * Convert the (browser) coordinates {@code (x, y)} to scaled coordinates in
	 * \([0,1]^2\) and store in {@code dest}.
	 * 
	 * @param x    the {@code x}-coordinate to convert
	 * @param y    the {@code y}-coordinate to convert
	 * @param dest the point to save the converted coordinates
	 * @return the converted coordinates
	 */
	public Point2D convertToScaledCoordinates(int x, int y, Point2D dest) {
		dest.set((x - element.getOffsetLeft() - bounds.getX() - 0.5) / bounds.getWidth(),
				1.0 - (y - element.getOffsetTop() - bounds.getY()) / bounds.getHeight());
		return dest;
	}

	/**
	 * Export the graphical context {@code ctx}.
	 * 
	 * @param ctx the graphical context to export
	 */
	public abstract void export(MyContext2d ctx);

	/**
	 * Calculate bounds of drawing area, i.e. of the canvas, which excludes the
	 * (CSS) padding of the graph. If element has never been visible its size is
	 * not yet known.
	 *
	 * @return {@code true} if bounds are successfully calculated.
	 */
	public boolean calcBounds() {
		int width = (wrapper == null ? 0 : wrapper.getOffsetWidth());
		int height = (wrapper == null ? 0 : wrapper.getOffsetHeight());
		if (width == 0 || height == 0)
			return false;
		calcBounds(width, height);
		return true;
	}

	/**
	 * Calculate bounds of drawing area.
	 * 
	 * @param width  the width of the drawing area
	 * @param height the height of the drawing area
	 */
	public void calcBounds(int width, int height) {
		cachedMidYTickLabelWidth = Double.NaN;
		bounds.set(style.minPadding, style.minPadding, width - 2.0 * style.minPadding, height - 2.0 * style.minPadding);
		if (style.showFrame) {
			double f = style.frameWidth;
			double f2 = f + f;
			bounds.adjust(-f, f, -f2, -f2);
		}
		adjustBoundsForLabels();
		adjustBoundsForTickLabels();
		adjustBoundsForLegend();
		if (style.showXTicks)
			bounds.adjust(0, 0, 0, -(style.tickLength + 2));
		if (style.showYTicks)
			adjustBoundsForYSide(style.tickLength + 2.0);
		// keep the frame on whole pixels for crisp lines, see drawFrameBorder(),
		// without encroaching on the room for labels
		double left = Math.ceil(bounds.getX());
		double top = Math.ceil(bounds.getY());
		bounds.set(left, top, Math.floor(bounds.getX() + bounds.getWidth()) - left,
				Math.floor(bounds.getY() + bounds.getHeight()) - top);
		style.setFitMargins(bounds.getWidth(), bounds.getHeight());
	}

	/**
	 * Adjust bounds to make room for axes labels.
	 */
	private void adjustBoundsForLabels() {
		if (style.showXLabel && style.xLabel != null)
			bounds.adjust(0, 0, 0, -20); // 14px for font size plus some padding
		if (style.showYLabel && style.yLabel != null)
			adjustBoundsForYSide(Y_AXIS_LABEL_THICKNESS
					+ (style.showYTickLabels ? Y_AXIS_LABEL_RESERVED_GAP : Y_AXIS_LABEL_MIN_GAP));
	}

	/**
	 * Adjust bounds to make room for tick labels.
	 */
	private void adjustBoundsForTickLabels() {
		// x tick labels are drawn within the frame: first label is pinned to the
		// left edge and the last label to the right edge.
		if (style.showXTickLabels)
			bounds.adjust(0, 0, 0, -14); // 10px for font size plus some padding
		String font = g.getFont();
		setFont(style.ticksLabelFont);
		if (style.showYTickLabels) {
			setFont(style.ticksLabelFont);
			// NOTE: cannot use style.Max or similar because then widths might end up
			// different in views with multiple graphs
			// bounds.width -= g.measureText("100%").getWidth()+(int)(4); // tick label
			// width plus some padding
			int digits;
			if (style.percentY) {
				if (style.yMax <= 1.0)
					digits = 1;
				else
					digits = 0;
			} else {
				digits = computeYTickDigits(4);
			}
			double width = measureMidYTickLabelWidth(digits);
			if (style.percentY)
				width = Math.max(width, measureYTickLabelWidth(new String[] { "100.0" }, true));
			// room for the widest tick label drawn since the last reset
			width = Math.max(width, drawnYTickLabelWidth);
			// share the width with the other graphs of the view to line up frames
			cachedMidYTickLabelWidth = view.getYTickLabelWidth(this, width);
			adjustBoundsForYSide(cachedMidYTickLabelWidth);
			adjustBoundsForYSide(Y_TICK_LABEL_GAP + 2.0);
		}
		g.setFont(font);
	}

	/**
	 * Adjust bounds to make room for y-axis decorations on the selected side.
	 * 
	 * @param padding the horizontal padding to reserve
	 */
	private void adjustBoundsForYSide(double padding) {
		if (padding <= 0.0)
			return;
		if (style.showYAxisRight)
			bounds.adjust(0, 0, -padding, 0);
		else
			bounds.adjust(padding, 0, -padding, 0);
	}

	/**
	 * Adjust bounds to make room for an optional legend.
	 * <p>
	 * The base implementation is a no-op and serves as a hook for subclasses that
	 * render legends.
	 */
	protected void adjustBoundsForLegend() {
		// hook for subclasses that implement a legend
	}

	/**
	 * Draw the frame of the graph including axes labels, ticks and tick marks, as
	 * applicable.
	 * 
	 * <h3>Implementation Note:</h3>
	 * The <em>top, left</em> corner of the canvas is assumed to be at
	 * {@code (0, 0)}. Flips the direction of the {@code y}-axis.
	 * 
	 * @param xLevels the number of vertical levels
	 * @param yLevels the number of horizontal levels
	 * 
	 * @see GraphStyle
	 */
	protected void drawFrame(int xLevels, int yLevels) {
		drawFrame(xLevels, yLevels, -1.0);
	}

	/**
	 * Draw the frame of the graph including axes labels, ticks and tick marks, as
	 * applicable, after scaling the canvas by {@code gscale}.
	 * 
	 * <h3>Implementation Note:</h3>
	 * After scaling the origin {@code (0, 0)} is assumed to be in the <em>bottom,
	 * left</em> corner of the canvas.
	 *
	 * @param xLevels the number of vertical levels
	 * @param yLevels the number of horizontal levels
	 * @param gscale  the scaling applied to the coordinate transformation
	 */
	protected void drawFrame(int xLevels, int yLevels, double gscale) {
		if (!style.showFrame)
			return;

		g.save();
		double w = bounds.getWidth();
		double h = bounds.getHeight();
		drawFrameBorder(w, h);
		drawXLevels(xLevels, w, h);
		drawCustomXLevels(w, h);
		drawYLevels(yLevels, w, h);
		drawCustomYLevels(w, h);
		drawXAxisLabel(w, h);
		drawYAxisLabel(w, h);
		drawGraphLabel(gscale);

		g.restore();
		if (drawnYTickLabelWidth > cachedMidYTickLabelWidth + 0.5 && !isRelayoutScheduled) {
			// tick labels need more room, e.g. after autoscaling or zooming
			isRelayoutScheduled = true;
			Scheduler.get().scheduleDeferred(() -> {
				isRelayoutScheduled = false;
				if (calcBounds())
					paint(true);
			});
		}
	}

	/**
	 * The width of the widest tick label of the {@code y}-axis drawn since the
	 * last reset. Ensures that the room reserved for the tick labels grows when
	 * the range of the {@code y}-axis changes, e.g. through autoscaling or zooming,
	 * but does not shrink until the graph is reset, which avoids jittery frames.
	 *
	 * @see #adjustBoundsForTickLabels()
	 */
	private double drawnYTickLabelWidth = 0.0;

	/**
	 * The flag to indicate that recalculating the bounds is scheduled because the
	 * tick labels of the {@code y}-axis require more room.
	 */
	private boolean isRelayoutScheduled = false;

	/**
	 * Draw the bounding box of the graph frame.
	 *
	 * @param w frame width
	 * @param h frame height
	 */
	private void drawFrameBorder(double w, double h) {
		g.setLineWidth(style.frameWidth);
		g.setStrokeStyle(style.frameColor);
		// NOTE: without the 0.5 the lines are at least 2px thick...
		g.strokeRect(0.5, 0.5, w, h);
	}

	/**
	 * Draw regular vertical guide lines, ticks, and labels.
	 *
	 * @param xLevels number of levels
	 * @param w       frame width
	 * @param h       frame height
	 */
	private void drawXLevels(int xLevels, double w, double h) {
		double range = style.xMax - style.xMin;
		if (xLevels <= 0 || range <= 0.0)
			return;
		double offset = (style.offsetXTickLabels && !style.percentX) ? style.xTickOffset : 0.0;
		double min = style.xMin + offset;
		double max = style.xMax + offset;
		double step = niceTickStep(min, max, xLevels);
		int digits = tickDigits(step, style.percentX);
		g.setLineWidth(style.levelWidth);
		for (double xval : niceTicks(min, max, step)) {
			double level = 0.5 + (xval - min) / range * w;
			if (style.showXLevels && level > 1.5 && level < w - 0.5) {
				g.setStrokeStyle(style.levelColor);
				strokeLine(level, 0, level, h);
			}
			if (style.showXTicks) {
				g.setStrokeStyle(style.frameColor);
				strokeLine(level, h + 0.5, level, h + 0.5 + style.tickLength);
			}
			if (style.showXTickLabels) {
				setFont(style.ticksLabelFont);
				g.setFillStyle(style.frameColor);
				String tick = style.percentX ? Formatter.formatPercent(xval, digits) : Formatter.format(xval, digits);
				// center tick labels with ticks but keep them within the frame
				double tickWidth = g.measureText(tick).getWidth();
				double xpos = Math.max(2.0, Math.min(level - tickWidth * 0.5, w + 0.5 - tickWidth));
				g.fillText(tick, xpos, h + (style.tickLength + 12.5));
			}
		}
	}

	/**
	 * Draw vertical guide lines at custom x positions.
	 *
	 * @param w frame width
	 * @param h frame height
	 */
	private void drawCustomXLevels(double w, double h) {
		if (style.customXLevels == null)
			return;
		for (double cx : style.customXLevels) {
			if (cx > style.xMax || cx < style.xMin)
				continue;
			g.setStrokeStyle(style.customLevelColor);
			double level = 0.5 + (cx - style.xMin) / (style.xMax - style.xMin) * w;
			strokeLine(level, 0, level, h);
		}
	}

	/**
	 * Draw the y-levels including ticks and tick labels.
	 * 
	 * @param yLevels the number of horizontal levels
	 * @param w       the width of the graph
	 * @param h       the height of the graph
	 */
	private void drawYLevels(int yLevels, double w, double h) {
		if (yLevels <= 0)
			return;
		if (style.logScaleY) {
			drawLogYLevels(yLevels, w, h);
			return;
		}
		double range = style.yMax - style.yMin;
		if (range <= 0.0)
			return;
		double offset = style.offsetYTickLabels ? style.yTickOffset : 0.0;
		double min = style.yMin + offset;
		double max = style.yMax + offset;
		double step = niceTickStep(min, max, yLevels);
		int digits = tickDigits(step, style.percentY);
		for (double tval : niceTicks(min, max, step)) {
			double level = 0.5 + (1.0 - (tval - min) / range) * h;
			drawYLevel(w, h, level, tval - offset, digits);
		}
	}

	/**
	 * Draw the y-levels of a logarithmic axis including ticks and tick labels.
	 * Ticks are placed at decades or, if this yields too few ticks, at
	 * {@code m·10^k} with {@code m ∈ {1, 2, 5}} or {@code m ∈ {1, ..., 9}}.
	 *
	 * @param yLevels the targeted number of horizontal levels
	 * @param w       the width of the graph
	 * @param h       the height of the graph
	 */
	private void drawLogYLevels(int yLevels, double w, double h) {
		double[] range = computeYRange();
		double ymin = range[0];
		double yrange = range[1];
		if (yrange <= 0.0 || !Double.isFinite(yrange))
			return;
		double[] ticks = new double[0];
		for (double[] mantissas : LOG_TICK_MANTISSAS) {
			ticks = logTicks(style.yMin, style.yMax, mantissas);
			if (ticks.length >= Math.min(3, yLevels))
				break;
		}
		for (double yval : ticks) {
			double level = 0.5 + (1.0 - (Math.log10(yval) - ymin) / yrange) * h;
			drawYLevel(w, h, level, yval, tickDigits(yval, false));
		}
	}

	/**
	 * The mantissas of tick values on logarithmic axes, in order of preference.
	 *
	 * @see #drawLogYLevels(int, double, double)
	 */
	private static final double[][] LOG_TICK_MANTISSAS = { { 1.0 }, { 1.0, 2.0, 5.0 },
			{ 1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0, 9.0 } };

	/**
	 * Determine the ticks of the form {@code m·10^k} for the given mantissas
	 * {@code m} within {@code [min, max]} on a logarithmic axis.
	 *
	 * @param min       the axis minimum (must be {@code > 0})
	 * @param max       the axis maximum
	 * @param mantissas the mantissas of the ticks
	 * @return the tick values in increasing order
	 */
	static double[] logTicks(double min, double max, double[] mantissas) {
		if (min <= 0.0 || max <= min)
			return new double[0];
		int kmin = (int) Math.floor(Math.log10(min));
		int kmax = (int) Math.floor(Math.log10(max));
		double[] ticks = new double[Math.min((kmax - kmin + 1) * mantissas.length, 1000)];
		int n = 0;
		for (int k = kmin; k <= kmax && n < ticks.length; k++) {
			double mag = Math.pow(10.0, k);
			for (double m : mantissas) {
				double t = m * mag;
				if (t >= min * (1.0 - 1e-9) && t <= max * (1.0 + 1e-9) && n < ticks.length)
					ticks[n++] = t;
			}
		}
		return Arrays.copyOf(ticks, n);
	}

	/**
	 * Draw a single y-level including its guide line (unless at the frame), tick
	 * and tick label.
	 *
	 * @param w      the width of the graph
	 * @param h      the height of the graph
	 * @param level  screen y-position of the level
	 * @param yval   y-value represented by the level
	 * @param digits decimal digits for tick label formatting
	 */
	private void drawYLevel(double w, double h, double level, double yval, int digits) {
		g.setLineWidth(style.levelWidth);
		if (style.showYLevels && level > 1.5 && level < h - 0.5) {
			g.setStrokeStyle(style.levelColor);
			strokeLine(0.5, level, w, level);
		}
		if (style.showYTicks) {
			double tickStart = style.showYAxisRight ? w + 0.5 : 0.5;
			double tickEnd = style.showYAxisRight ? w + 0.5 + style.tickLength : 0.5 - style.tickLength;
			g.setStrokeStyle(style.frameColor);
			strokeLine(tickStart, level, tickEnd, level);
		}
		if (style.showYTickLabels)
			drawYTickLabel(w, level, yval, digits);
	}

	/**
	 * Determine a "nice" spacing of ticks for an axis spanning
	 * {@code [min, max]}: of the form {@code m·10^k} with
	 * {@code m ∈ {1, 2, 2.5, 5}}, as close as possible to dividing the range into
	 * {@code levels} intervals.
	 *
	 * @param min    the axis minimum
	 * @param max    the axis maximum
	 * @param levels the targeted number of intervals
	 * @return the tick spacing
	 */
	static double niceTickStep(double min, double max, int levels) {
		double raw = (max - min) / levels;
		if (raw <= 0.0 || !Double.isFinite(raw))
			return raw;
		double mag = Math.pow(10.0, Math.floor(Math.log10(raw)));
		double best = mag;
		double bestDist = Double.MAX_VALUE;
		for (double m : NICE_TICK_MANTISSAS) {
			double dist = Math.abs(Math.log(m * mag / raw));
			if (dist < bestDist) {
				bestDist = dist;
				best = m * mag;
			}
		}
		return best;
	}

	/**
	 * The mantissas of "nice" tick spacings.
	 *
	 * @see #niceTickStep(double, double, int)
	 */
	private static final double[] NICE_TICK_MANTISSAS = { 1.0, 2.0, 2.5, 5.0, 10.0 };

	/**
	 * Determine the ticks at multiples of {@code step} within {@code [min, max]}.
	 *
	 * @param min  the axis minimum
	 * @param max  the axis maximum
	 * @param step the tick spacing, see {@link #niceTickStep(double, double, int)}
	 * @return the tick values in increasing order
	 */
	static double[] niceTicks(double min, double max, double step) {
		if (step <= 0.0 || !Double.isFinite(step))
			return new double[0];
		double eps = step * 1e-6;
		long first = (long) Math.ceil((min - eps) / step);
		long last = (long) Math.floor((max + eps) / step);
		int n = (int) Math.max(0, Math.min(last - first + 1, 1000));
		double[] ticks = new double[n];
		for (int i = 0; i < n; i++) {
			double t = (first + i) * step;
			// avoid labels like -0
			ticks[i] = Math.abs(t) < eps ? 0.0 : t;
		}
		return ticks;
	}

	/**
	 * Determine the number of decimal digits required to label ticks spaced by
	 * {@code step}.
	 *
	 * @param step    the tick spacing
	 * @param percent whether labels are formatted as percentages
	 * @return the number of decimal digits
	 */
	static int tickDigits(double step, boolean percent) {
		double units = Math.abs(percent ? 100.0 * step : step);
		if (units <= 0.0 || !Double.isFinite(units))
			return 0;
		int digits = 0;
		double scaled = units;
		while (digits < 12 && Math.abs(scaled - Math.rint(scaled)) > 1e-6 * scaled) {
			digits++;
			scaled *= 10.0;
		}
		return digits;
	}

	/**
	 * Compute the (possibly logarithmic) y-range for tick computations.
	 * 
	 * @return array {ymin, yrange}
	 */
	private double[] computeYRange() {
		double ymin;
		double yrange;
		if (style.logScaleY) {
			ymin = Math.log10(style.yMin);
			yrange = Math.log10(style.yMax) - ymin;
		} else {
			ymin = style.yMin;
			yrange = style.yMax - ymin;
		}
		return new double[] { ymin, yrange };
	}

	/**
	 * Draw the y-axis tick label at the specified screen level.
	 *
	 * @param w      frame width
	 * @param level  screen y-position for the tick
	 * @param yval   y-value represented by the tick
	 * @param digits decimal digits for tick label formatting
	 */
	private void drawYTickLabel(double w, double level, double yval, int digits) {
		setFont(style.ticksLabelFont);
		g.setFillStyle(style.frameColor);
		double tickValue = style.offsetYTickLabels ? yval + style.yTickOffset : yval;
		String tick = formatYTickLabel(tickValue, digits);
		String[] numexp = tick.split("\\^");
		double labelWidth = measureYTickLabelWidth(numexp, style.percentY);
		drawnYTickLabelWidth = Math.max(drawnYTickLabelWidth, labelWidth);
		double xpos;
		if (style.showYAxisRight)
			xpos = w + 0.5 + (style.tickLength + Y_TICK_LABEL_GAP);
		else
			xpos = 0.5 - (style.tickLength + Y_TICK_LABEL_GAP) - labelWidth;
		// center label on tick but keep it within the frame at the top
		double ypos = Math.max(level + 4.5, 9.5);
		g.fillText(numexp[0], xpos, ypos);
		xpos += g.measureText(numexp[0]).getWidth();
		if (numexp.length > 1) {
			setFont(style.ticksLabelFont.replace("11px", "9px"));
			g.fillText(numexp[1], xpos, ypos - 4.5);
			xpos += g.measureText(numexp[1]).getWidth();
			setFont(style.ticksLabelFont);
		}
		if (style.percentY)
			g.fillText("%", xpos, ypos);
	}

	/**
	 * Compute the number of decimal digits needed for y-axis tick labels.
	 *
	 * @param levels number of y-axis intervals
	 * @return decimal digits for y tick labels
	 */
	private int computeYTickDigits(int levels) {
		if (style.percentY)
			return style.yMax <= 1.0 ? 1 : 0;
		double tickMin = style.offsetYTickLabels ? style.yMin + style.yTickOffset : style.yMin;
		double tickMax = style.offsetYTickLabels ? style.yMax + style.yTickOffset : style.yMax;
		return (!style.logScaleY)
				? tickDigits(niceTickStep(tickMin, tickMax, levels), false)
				: 2;
	}

	/**
	 * Format a y-axis tick label using the same pretty formatting as the renderer.
	 *
	 * @param tickValue the y value represented by the tick
	 * @param digits    decimal digits for pretty formatting
	 * @return the formatted tick label
	 */
	private String formatYTickLabel(double tickValue, int digits) {
		return Formatter.formatPretty(style.percentY ? 100.0 * tickValue : tickValue, digits);
	}

	/**
	 * Measure the width of a y-axis tick label including exponent and percent.
	 * 
	 * @param numexp  the tick label split into base and exponent parts
	 * @param percent whether a percent sign is appended
	 * @return the total width in pixels
	 */
	private double measureYTickLabelWidth(String[] numexp, boolean percent) {
		setFont(style.ticksLabelFont);
		double width = g.measureText(numexp[0]).getWidth();
		if (numexp.length > 1) {
			String expFont = style.ticksLabelFont.replace("11px", "9px");
			setFont(expFont);
			width += g.measureText(numexp[1]).getWidth();
			setFont(style.ticksLabelFont);
		}
		if (percent)
			width += g.measureText("%").getWidth();
		return width;
	}

	/**
	 * Measure the width of the midpoint y-axis tick label using the same pretty
	 * formatting as the renderer. This is the label most likely to interfere with
	 * the vertical axis label.
	 *
	 * @param digits decimal digits for pretty formatting
	 * @return measured midpoint tick label width
	 */
	private double measureMidYTickLabelWidth(int digits) {
		double tickMin = style.offsetYTickLabels ? style.yMin + style.yTickOffset : style.yMin;
		double tickMax = style.offsetYTickLabels ? style.yMax + style.yTickOffset : style.yMax;
		double sample = 0.5 * (tickMin + tickMax);
		String tick = formatYTickLabel(sample, digits);
		return measureYTickLabelWidth(tick.split("\\^"), style.percentY);
	}

	/**
	 * Draw horizontal guide lines at custom y positions.
	 *
	 * @param w frame width
	 * @param h frame height
	 */
	private void drawCustomYLevels(double w, double h) {
		for (double level : style.customYLevels) {
			if (level > style.yMax || level < style.yMin)
				continue;
			g.setStrokeStyle(style.customLevelColor);
			double ypos = 0.5 + (1.0 - (level - style.yMin) / (style.yMax - style.yMin)) * h;
			strokeLine(0.5, ypos, w, ypos);
		}
	}

	/**
	 * Draw the x-axis label.
	 *
	 * @param w frame width
	 * @param h frame height
	 */
	private void drawXAxisLabel(double w, double h) {
		// x-axis label
		if (!style.showXLabel || style.xLabel == null)
			return;
		setFont(style.axesLabelFont);
		double xpos = (w - g.measureText(style.xLabel).getWidth()) * 0.5;
		double ypos = h + ((style.showXTickLabels ? 14 : 0) + (style.showXTicks ? style.tickLength : 0) + 14);
		g.fillText(style.xLabel, xpos, ypos);
	}

	/**
	 * Draw the y-axis label, including positioning logic based on tick labels.
	 *
	 * @param w frame width
	 * @param h frame height
	 */
	private void drawYAxisLabel(double w, double h) {
		// y-axis label
		if (!style.showYLabel || style.yLabel == null)
			return;
		double tickskip = getYAxisLabelTickSkip();
		setFont(style.axesLabelFont);
		String ylabel = style.yLabel + (style.logScaleY ? " (log)" : "");
		double xpos = style.showYAxisRight ? w + tickskip + style.tickLength + Y_TICK_LABEL_GAP
				: -(tickskip + style.tickLength + Y_TICK_LABEL_GAP);
		fillTextVertical(ylabel, xpos, (h + g.measureText(ylabel).getWidth()) / 2);
	}

	/**
	 * Get the horizontal offset to use for the y-axis label.
	 *
	 * @return the y-axis label offset, including reserved whitespace beyond the y
	 *         tick labels
	 */
	protected double getYAxisLabelTickSkip() {
		if (!Double.isNaN(yAxisLabelTickSkip))
			return yAxisLabelTickSkip;
		if (style.showYTickLabels) {
			double labelWidth = cachedMidYTickLabelWidth;
			if (Double.isNaN(labelWidth)) {
				int digits;
				if (style.percentY) {
					if (style.yMax <= 1.0)
						digits = 1;
					else
						digits = 0;
				} else {
					digits = computeYTickDigits(4);
				}
				labelWidth = measureMidYTickLabelWidth(digits);
			}
			return labelWidth + Y_AXIS_LABEL_RESERVED_GAP;
		}
		return Y_AXIS_LABEL_MIN_GAP;
	}

	/**
	 * Draw the label shown inside the graph canvas.
	 *
	 * @param gscale current canvas scale factor
	 */
	private void drawGraphLabel(double gscale) {
		final String PX_SANS_SERIF = "px sans-serif";
		if (!style.showLabel || style.label == null)
			return;
		int labelFontSize = (int) Math.min(20, Math.max(10, bounds.getHeight() * 0.05));
		g.setFillStyle(style.labelColor);
		if (gscale > 0.0) {
			g.save();
			g.scale(1.0 / gscale, 1.0 / gscale);
			setFont("bold " + labelFontSize + PX_SANS_SERIF);
			g.fillText(style.label, 4, (int) (labelFontSize * 1.2));
			g.restore();
		} else {
			setFont("bold " + labelFontSize + PX_SANS_SERIF);
			g.fillText(style.label, 4, (int) (labelFontSize * 1.2));
		}
	}

	/**
	 * Constant representing the numerical value of \(2 \pi\)
	 */
	private static final double TWOPI = 2.0 * Math.PI;

	/**
	 * Constant representing the numerical value of \(\pi/2\)
	 */
	protected static final double PIHALF = 0.5 * Math.PI;

	/**
	 * The maximum number of line segments to draw before returning control to the
	 * event loop.
	 */
	private static final int MAX_SEGEMENTS = 200;

	/**
	 * Set the font to {@code cssfont} in CSS format for drawing on the canvas.
	 * 
	 * @param cssfont the font for drawing
	 */
	protected void setFont(String cssfont) {
		// adjust size
		int idx;
		int start = cssfont.length();
		for (idx = 0; idx < start; idx++) {
			if (Character.isDigit(cssfont.charAt(idx))) {
				start = idx;
				break;
			}
		}
		for (idx = start; idx < cssfont.length(); idx++) {
			if (!Character.isDigit(cssfont.charAt(idx)))
				break;
		}
		double size = Integer.parseInt(cssfont.substring(start, idx));
		g.setFont(cssfont.substring(0, start) + (int) size + cssfont.substring(idx));
	}

	/**
	 * Set the stroke colour for node with index {@code node}.
	 * 
	 * @param node the index of the node
	 */
	protected void setStrokeStyleAt(int node) {
		g.setStrokeStyle(colors[node]);
	}

	/**
	 * Draw the path {@code path}.
	 * 
	 * @param path the path to draw
	 */
	protected void stroke(Path2D path) {
		sketch(path, false);
	}

	/**
	 * Fill the path {@code path}.
	 * 
	 * @param path the path to fill
	 */
	protected void fill(Path2D path) {
		sketch(path, true);
	}

	/**
	 * Draw or fill (if {@code fill == true}) the path {@code path}.
	 * 
	 * <h3>Implementation notes:</h3>
	 * <ul>
	 * <li>Drawing too many segments at once becomes very slow (why?).
	 * <li>Periodically closing and stroking the path is <em>orders</em> of
	 * magnitudes faster!
	 * <li>This simple change renders freezing of the GUI essentially a non issue.
	 * <li>SVG seems to be a stickler about opening and closing paths or empty paths
	 * (see Canvas2SVG.js)
	 * </ul>
	 *
	 * @param path the path to draw (or fill)
	 * @param fill {@code true} to fill path
	 */
	private void sketch(Path2D path, boolean fill) {
		int nSegments = 0;
		boolean closed = false;
		PathIterator i = path.getPathIterator();
		double[] point = new double[6];
		g.beginPath();
		while (!i.isDone()) {
			switch (i.currentSegment(point)) {
				case PathIterator.SEG_MOVETO:
					g.moveTo(point[0], point[1]);
					break;
				case PathIterator.SEG_LINETO:
					g.lineTo(point[0], point[1]);
					closed = false;
					break;
				case PathIterator.SEG_CLOSE:
					g.closePath();
					closed = true;
					if (fill)
						g.fill();
					else
						g.stroke();
					break;
				default:
			}
			i.next();
			if (++nSegments > MAX_SEGEMENTS) {
				if (!closed)
					closePath(fill);
				nSegments = 0;
				g.beginPath();
				// start next segment where previous ended
				g.moveTo(point[0], point[1]);
			}
		}
		if (nSegments > 0)
			closePath(fill);
	}

	/**
	 * Helper method to close the path and draw (or fill) it.
	 * 
	 * @param fill {@code true} to fill path
	 */
	private void closePath(boolean fill) {
		g.closePath();
		if (fill)
			g.fill();
		else
			g.stroke();
	}

	/**
	 * Draw the rectangle with origin at {@code (x,y)}, width {@code w} and height
	 * {@code h}.
	 * 
	 * @param x the {@code x}-coordinate of the origin
	 * @param y the {@code y}-coordinate of the origin
	 * @param w the width of the rectangle
	 * @param h the height of the rectangle
	 */
	protected void strokeRect(double x, double y, double w, double h) {
		g.strokeRect(x, y, w, h);
	}

	/**
	 * Fill the rectangle with origin at {@code (x,y)}, width {@code w} and height
	 * {@code h}.
	 * 
	 * @param x the {@code x}-coordinate of the origin
	 * @param y the {@code y}-coordinate of the origin
	 * @param w the width of the rectangle
	 * @param h the height of the rectangle
	 */
	protected void fillRect(double x, double y, double w, double h) {
		g.fillRect(x, y, w, h);
	}

	/**
	 * Draw the outline of the 2D node.
	 * 
	 * @param node the node to draw
	 */
	protected void strokeCircle(Node2D node) {
		strokeCircle(node.getX(), node.getY(), node.getR());
	}

	/**
	 * Draw the outline of a circle at {@code point} with {@code radius}.
	 * 
	 * @param point  the node to draw
	 * @param radius the radius of the circle
	 */
	protected void strokeCircle(Point2D point, double radius) {
		strokeCircle(point.getX(), point.getY(), radius);
	}

	/**
	 * Draw the circle with the center at {@code (x,y)} and radius {@code radius}.
	 * 
	 * @param x      the {@code x}-coordinate of the center
	 * @param y      the {@code y}-coordinate of the center
	 * @param radius the radius of the circle
	 */
	protected void strokeCircle(double x, double y, double radius) {
		sketchCircle(x, y, radius);
		g.stroke();
	}

	/**
	 * Draw the filled 2D node.
	 * 
	 * @param node the node to draw
	 */
	protected void fillCircle(Node2D node) {
		fillCircle(node.getX(), node.getY(), node.getR());
	}

	/**
	 * Draw a filled circle at {@code point} with {@code radius}.
	 * 
	 * @param point  the node to draw
	 * @param radius the radius of the circle
	 */
	protected void fillCircle(Point2D point, double radius) {
		fillCircle(point.getX(), point.getY(), radius);
	}

	/**
	 * Fill the circle with the center at {@code (x,y)} and radius {@code radius}.
	 * 
	 * @param x      the {@code x}-coordinate of the center
	 * @param y      the {@code y}-coordinate of the center
	 * @param radius the radius of the circle
	 */
	protected void fillCircle(double x, double y, double radius) {
		sketchCircle(x, y, radius);
		g.fill();
	}

	/**
	 * Helper method to create a path for a circle with the center at {@code (x,y)}
	 * and radius {@code radius}.
	 * 
	 * @param x      the {@code x}-coordinate of the center
	 * @param y      the {@code y}-coordinate of the center
	 * @param radius the radius of the circle
	 */
	private void sketchCircle(double x, double y, double radius) {
		g.beginPath();
		g.arc(x, y, radius, 0.0, TWOPI, true);
		g.closePath();
	}

	/**
	 * Draw a line from point {@code a} to {@code b)}.
	 * 
	 * @param a the start point
	 * @param b the end point
	 */
	protected void strokeLine(Point2D a, Point2D b) {
		strokeLine(a.getX(), a.getY(), b.getX(), b.getY());
	}

	/**
	 * Draw a line from point {@code (sx,sy)} to {@code (ex,ey)}.
	 * 
	 * @param sx the {@code x}-coordinate of the start point
	 * @param sy the {@code y}-coordinate of the start point
	 * @param ex the {@code x}-coordinate of the end point
	 * @param ey the {@code y}-coordinate of the end point
	 */
	protected void strokeLine(double sx, double sy, double ex, double ey) {
		g.beginPath();
		g.moveTo(sx, sy);
		g.lineTo(ex, ey);
		g.closePath();
		g.stroke();
	}

	/**
	 * Draw filled text vertically starting at point {@code (x,y)}
	 * 
	 * @param msg the text to write
	 * @param x   the {@code x}-coordinate of the start point
	 * @param y   the {@code y}-coordinate of the start point
	 */
	protected void fillTextVertical(String msg, double x, double y) {
		g.save();
		g.translate(x, y);
		g.rotate(-PIHALF);
		g.fillText(msg, 0, 0);
		g.restore();
	}

	/**
	 * Clear the graph.
	 */
	public void clearGraph() {
		if (g == null)
			return;
		g.save();
		g.scale(scale, scale);
		clearCanvas();
		g.restore();
	}

	/**
	 * Clear the canvas.
	 * 
	 * <h3>Implementation note:</h3>
	 * Assumes scaled canvas coordinates, i.e. lower, left corner at {@code (0,0)}.
	 */
	protected void clearCanvas() {
		g.clearRect(0, 0, wrapper.getOffsetWidth(), wrapper.getOffsetHeight());
	}

	/**
	 * Reset the transformation of the graphics context to the identity transform.
	 */
	protected void resetTransforms() {
		g.setTransform(1, 0, 0, 1, 0, 0);
	}

	/**
	 * Flag to indicate whether the graph displays a message.
	 */
	protected boolean hasMessage = false;

	/**
	 * The last message displayed on the graph.
	 */
	protected String message = null;

	/**
	 * Cached font size of the last displayed message.
	 */
	protected double msgFontSize = 12.0;

	/**
	 * Cached line height of the last displayed message.
	 */
	protected double msgLineHeight = 16.8;

	/**
	 * Display message {@code msg} on the graph (no HTML formatting).
	 * 
	 * @param msg the message to display
	 * @return {@code true} if message displayed
	 */
	public boolean displayMessage(String msg) {
		return displayMessage(msg, true);
	}

	/**
	 * Display message {@code msg} on the graph (no HTML formatting), optionally
	 * autoscaling the text to the available width.
	 * 
	 * @param msg       the message to display
	 * @param autoscale {@code true} to scale the text to the available width
	 * @return {@code true} if message displayed
	 */
	public boolean displayMessage(String msg, boolean autoscale) {
		if (msg == null || msg.isEmpty()) {
			clearMessage();
			return false;
		}
		message = msg;
		hasMessage = true;
		clearGraph();
		if (g != null) {
			g.save();
			g.setFont("12px sans-serif");
			if (autoscale) {
				double textWidth = g.measureText(msg).getWidth();
				double width = Math.max(1.0, wrapper.getOffsetWidth());
				int fontSize = Math.min((int) (12.0 * 0.666 * width / Math.max(1.0, textWidth)), 24);
				msgFontSize = Math.max(fontSize, 12);
				msgLineHeight = msgFontSize * 1.4;
			}
			g.restore();
		}
		msgLabel.setText(msg);
		Style msgStyle = msgLabel.getElement().getStyle();
		double height = Math.max(1.0, wrapper.getOffsetHeight());
		msgStyle.setFontSize(msgFontSize, Unit.PX);
		msgStyle.setLineHeight(msgLineHeight, Unit.PX);
		wrapper.setWidgetTopHeight(msgLabel, Math.max(0.0, (height - msgLineHeight) * 0.5), Unit.PX,
				msgLineHeight, Unit.PX);
		msgLabel.setVisible(true);
		onMessageDisplayed();
		return true;
	}

	/**
	 * Clear the message.
	 */
	public void clearMessage() {
		if (!hasMessage)
			return;
		message = null;
		hasMessage = false;
		if (msgLabel != null)
			msgLabel.setVisible(false);
		onMessageCleared();
	}

	/**
	 * Hook invoked after a message has been displayed.
	 */
	protected void onMessageDisplayed() {
	}

	/**
	 * Hook invoked after a message has been cleared.
	 */
	protected void onMessageCleared() {
	}

	/**
	 * Check if the graph displays a message.
	 * 
	 * @return {@code true} if message displayed
	 */
	public boolean hasMessage() {
		return hasMessage;
	}

	/**
	 * Reset the view: restore the default zoom and shift, see {@link #zoom()},
	 * then restore the defaults of the options and apply the options of the view
	 * again, see {@link #parse(String)}. Default implementation for graphs that
	 * implement {@code Zooming}.
	 *
	 * @see Zoomer#resetView()
	 */
	public void resetView() {
		zoom();
		String args = options;
		// force parsing
		options = null;
		parse(args);
		if (!hasMessage)
			paint(true);
	}

	/**
	 * Restore the default zoom level and shift of the view.
	 *
	 * @see #resetView()
	 */
	protected void zoom() {
		zoomFactor = 1.0;
		if (viewCorner != null)
			viewCorner.set(0.0, 0.0);
	}

	/**
	 * Adjust zoom level by the factor {@code zoom}. Default implementation for
	 * graphs that implement {@code Zooming}.
	 *
	 * @param zoom the new zoom level
	 *
	 * @see Zooming#zoom(double)
	 */
	public void zoom(double zoom) {
		if (zoom <= 0.0)
			return;
		zoom(zoom, 0.5, 0.5);
	}

	/**
	 * Adjust zoom level by the factor {@code zoom} with the center at coordinates
	 * {@code (x,y)} (in display coordinates as provided by event listeners).
	 * Default implementation for graphs that implement {@code Zooming}.
	 *
	 * @param zoom the new zoom level
	 * @param x    the {@code x}-coordinate of the zoom center
	 * @param y    the {@code y}-coordinate of the zoom center
	 * 
	 * @see Zooming#zoom(double, int, int)
	 */
	public void zoom(double zoom, int x, int y) {
		if (hasMessage)
			return;
		// event coordinates are relative to the graph, bounds relative to the canvas
		zoom(zoom, (viewCorner.getX() + x - canvasPadX() - bounds.getX()) / (bounds.getWidth() * zoomFactor),
				(viewCorner.getY() + y - canvasPadY() - bounds.getY()) / (bounds.getHeight() * zoomFactor));
	}

	/**
	 * Helper method to adjust zoom level with the zoom center at the scaled
	 * coordinates {@code (fx, fy)}, specified as a fraction of the view port in
	 * horizontal and vertical directions, respectively.
	 *
	 * @param zoom the new zoom level
	 * @param fx   the scaled {@code x}-coordinate of the zoom center
	 * @param fy   the scaled {@code y}-coordinate of the zoom center
	 * 
	 * @see #onMouseWheel(MouseWheelEvent)
	 * @see #onTouchMove(TouchMoveEvent)
	 */
	protected void zoom(double zoom, double fx, double fy) {
		if (hasMessage)
			return;
		double newZoomFactor = Math.min(Zooming.ZOOM_MAX, Math.max(1.0, zoomFactor * zoom));
		double dz = newZoomFactor - zoomFactor;
		if (Math.abs(dz) < Model.EPS)
			return;
		setViewCorner(viewCorner.getX() + bounds.getWidth() * dz * fx,
				viewCorner.getY() + bounds.getHeight() * dz * fy, newZoomFactor);
		zoomFactor = newZoomFactor;
		gesture.zooming(dz > 0);
		paint(true);
	}

	/**
	 * Set the view corner to {@code (x, y)}, limited to the admissible range for
	 * the zoom factor {@code zoom}. By default, the zoomed graph always covers the
	 * bounds, i.e. the view corner ranges from {@code 0} to
	 * {@code bounds.getWidth() * (zoom - 1)} horizontally, and analogously
	 * vertically.
	 *
	 * @param x    the requested horizontal offset
	 * @param y    the requested vertical offset
	 * @param zoom the zoom factor
	 */
	protected void setViewCorner(double x, double y, double zoom) {
		viewCorner.set(Math.min(bounds.getWidth() * (zoom - 1.0), Math.max(0.0, x)),
				Math.min(bounds.getHeight() * (zoom - 1.0), Math.max(0.0, y)));
	}

	/**
	 * Shift the (zoomed) graph within the view port by {@code (dx, dy)}. Default
	 * implementation for graphs that implement {@code Shifting}. The view is
	 * limited as for zooming.
	 *
	 * @param dx the horizontal shift of the graph
	 * @param dy the vertical shift of the graph
	 *
	 * @see Shifting#shift(int, int)
	 * @see #setViewCorner(double, double, double)
	 */
	public void shift(int dx, int dy) {
		if (hasMessage)
			return;
		setViewCorner(viewCorner.getX() + dx, viewCorner.getY() + dy, zoomFactor);
		paint(true);
	}

	/**
	 * The flag to indicate whether the left mouse button is pressed.
	 */
	protected boolean leftMouseButton = false;

	/**
	 * The state of gestures that shift or zoom the view, including the tracked
	 * position of the pointer (mouse or touches).
	 */
	final Gesture gesture = new Gesture(getElement());

	/**
	 * Check if the event coordinates are inside the interactive plot area.
	 * Default implementation accepts all coordinates.
	 *
	 * @param x the {@code x}-coordinate relative to the graph element
	 * @param y the {@code y}-coordinate relative to the graph element
	 * @return {@code true} if inside the interactive plot area
	 */
	protected boolean inside(int x, int y) {
		return true;
	}

	/**
	 * The horizontal offset between this widget's own (border-box) edge and the
	 * canvas's, e.g. from CSS padding reserved for visual spacing between
	 * adjacent graphs. Mouse coordinates delivered to {@code onMouse*} handlers
	 * and {@code getTooltipAt(int, int)} are relative to this widget's edge,
	 * whereas {@link #bounds} is relative to the canvas - subtract this (and
	 * {@link #canvasPadY()}) from incoming {@code x}/{@code y} before using
	 * {@link #bounds}, so that hit-testing lines up with what is actually
	 * drawn rather than with this widget's outer edge.
	 *
	 * @return the horizontal canvas inset, or {@code 0} if there is no canvas
	 *         (e.g. {@link PopGraph3D})
	 */
	protected double canvasPadX() {
		return canvas == null ? 0.0 : (double) canvas.getAbsoluteLeft() - getAbsoluteLeft();
	}

	/**
	 * The vertical counterpart of {@link #canvasPadX()}.
	 *
	 * @return the vertical canvas inset, or {@code 0} if there is no canvas
	 *         (e.g. {@link PopGraph3D})
	 */
	protected double canvasPadY() {
		return canvas == null ? 0.0 : (double) canvas.getAbsoluteTop() - getAbsoluteTop();
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * If the left mouse button is pressed inside the graph, start tracking the
	 * mouse for subsequent {@link MouseMoveEvent}s. The graph captures the mouse
	 * until the button is released, such that the gesture continues even if the
	 * mouse leaves the graph.
	 *
	 * @see #onMouseMove(MouseMoveEvent)
	 * @see Gesture#capture()
	 */
	@Override
	public void onMouseDown(MouseDownEvent event) {
		if (event.getNativeButton() == NativeEvent.BUTTON_LEFT) {
			int x = event.getX();
			int y = event.getY();
			if (!inside(x, y)) {
				leftMouseButton = false;
				gesture.lose();
				return;
			}
			gesture.start(x, y);
			gesture.capture();
			leftMouseButton = true;
			if (mouseMoveHandler == null)
				mouseMoveHandler = addMouseMoveHandler(this);
			event.preventDefault();
		}
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * Cancel all interactions with the graph and reset node and mouse information.
	 *
	 * @see #onMouseMove(MouseMoveEvent)
	 * @see Gesture#release()
	 */
	@Override
	public void onMouseUp(MouseUpEvent event) {
		if (event.getNativeButton() == NativeEvent.BUTTON_LEFT) {
			endDrag();
			event.preventDefault();
		}
	}

	/**
	 * End dragging with the left mouse button: stop tracking the mouse and
	 * release the gesture.
	 *
	 * @see #onMouseUp(MouseUpEvent)
	 * @see #isReleased(MouseMoveEvent)
	 */
	protected void endDrag() {
		leftMouseButton = false;
		gesture.release();
		if (mouseMoveHandler != null) {
			mouseMoveHandler.removeHandler();
			mouseMoveHandler = null;
		}
	}

	/**
	 * Check if the left mouse button was released without a
	 * {@link MouseUpEvent}, e.g. outside of the browser window, and, if so, end
	 * dragging.
	 *
	 * @param event the mouse move event
	 * @return {@code true} if dragging ended
	 *
	 * @see Gesture#isPrimaryButtonDown(NativeEvent)
	 */
	protected boolean isReleased(MouseMoveEvent event) {
		if (!leftMouseButton || Gesture.isPrimaryButtonDown(event.getNativeEvent()))
			return false;
		endDrag();
		return true;
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * If the left mouse-key is pressed the graph is shifted within its
	 * viewport. Note this requires that the zoom level of the graph exceeds
	 * {@code 1}. Shifting may be combined with zooming (with the mouse wheel) and
	 * continues if the mouse leaves the graph.
	 *
	 * @see #onMouseDown(MouseDownEvent)
	 * @see Gesture#move(Gesture.Move)
	 */
	@Override
	public void onMouseMove(MouseMoveEvent event) {
		if (isReleased(event))
			return;
		if (leftMouseButton) {
			// shift view
			if (!gesture.drag(event.getX(), event.getY()))
				return;
			gesture.move(Gesture.Move.VIEW);
			shifter.shift(gesture.getDX(), gesture.getDY());
			event.preventDefault();
		}
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * Adjusts the zoom level of the graph, while preserving the location of the
	 * mouse on the graph. The zooming has inertia. Zooming is disabled shortly
	 * after moving the view (or a node) ended.
	 *
	 * @see #zoom(double, int, int)
	 * @see Gesture#wheel()
	 */
	@Override
	public void onMouseWheel(MouseWheelEvent event) {
		int x = event.getX();
		int y = event.getY();
		if (!inside(x, y))
			return;
		event.preventDefault();
		double dz = event.getNativeDeltaY();
		if (hasMessage || dz == 0.0 || !gesture.wheel())
			return;
		zoomer.zoom(Math.pow(Zooming.ZOOM_INCR, -dz), x, y);
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * The touches trigger different actions depending on the number of fingers:
	 * <dl>
	 * <dt>Single finger
	 * <dd>Initiate shifting the view. The touch may also turn out to be a tap,
	 * see {@link #onTouchEnd(TouchEndEvent)}.
	 * <dt>Two fingers
	 * <dd>Initiate pinching zoom.
	 * </dl>
	 *
	 * @see #onTouchMove(TouchMoveEvent)
	 */
	@Override
	public void onTouchStart(TouchStartEvent event) {
		JsArray<Touch> touches = event.getTouches();
		boolean enableMove = false;
		switch (touches.length()) {
			case 1:
				// initiate shift view
				Touch touch = touches.get(0);
				int x = touch.getRelativeX(element);
				int y = touch.getRelativeY(element);
				gesture.startTap(x, y);
				if (inside(x, y)) {
					gesture.start(x, y);
					enableMove = true;
				} else {
					gesture.lose();
				}
				break;
			case 2:
				// initiate pinch zoom
				gesture.cancelTap();
				Touch touch0 = touches.get(0);
				int x0 = touch0.getRelativeX(element);
				int y0 = touch0.getRelativeY(element);
				Touch touch1 = touches.get(1);
				int x1 = touch1.getRelativeX(element);
				int y1 = touch1.getRelativeY(element);
				if (inside(x0, y0) && inside(x1, y1)) {
					gesture.startPinch(x0, y0, x1, y1);
					enableMove = true;
				} else {
					gesture.lose();
				}
				break;
			default:
				gesture.cancelTap();
				break;
		}
		if (enableMove && touchMoveHandler == null)
			touchMoveHandler = addTouchMoveHandler(this);
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * The number of touches on the graph changed. If the last touch completes a
	 * double tap, i.e. the second of two taps ends, the double tap is processed by
	 * {@link #onDoubleTap(int, int, Touch)}. Processing double taps only when the
	 * second touch ends allows the second touch to start dragging instead, as for
	 * double clicks of the mouse.
	 *
	 * @see Gesture#endTap(int, int)
	 */
	@Override
	public void onTouchEnd(TouchEndEvent event) {
		JsArray<Touch> touches = event.getTouches();
		switch (touches.length()) {
			case 1:
				// track remaining touch to prevent jumps
				Touch touch = touches.get(0);
				gesture.start(touch.getRelativeX(element), touch.getRelativeY(element));
				break;
			case 0:
				resetTouches();
				JsArray<Touch> ended = event.getChangedTouches();
				if (ended.length() == 0)
					break;
				Touch last = ended.get(0);
				int x = last.getRelativeX(element);
				int y = last.getRelativeY(element);
				// prevent default (e.g. zooming the page) if double tap was processed
				if (gesture.endTap(x, y) && onDoubleTap(x, y, last))
					event.preventDefault();
				break;
			default:
		}
	}

	/**
	 * Process a double tap at {@code (x, y)}, relative to the graph. The default
	 * ignores double taps.
	 *
	 * @param x     the {@code x}-coordinate of the double tap
	 * @param y     the {@code y}-coordinate of the double tap
	 * @param touch the touch that completed the double tap
	 * @return {@code true} if the double tap was processed
	 *
	 * @see #onTouchEnd(TouchEndEvent)
	 */
	protected boolean onDoubleTap(int x, int y, Touch touch) {
		return false;
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * The browser aborted the touch sequence (e.g. a system gesture took over), so
	 * no {@link TouchEndEvent} follows. Reset as if all touches ended; otherwise
	 * the touch move handler stays registered and keeps intercepting (and
	 * preventing the default of) subsequent touch moves.
	 */
	@Override
	public void onTouchCancel(TouchCancelEvent event) {
		gesture.cancelTap();
		resetTouches();
	}

	/**
	 * Clear the state of shifting and pinching gestures and remove the touch move
	 * handler, which is only needed while touches are active.
	 */
	private void resetTouches() {
		gesture.lose();
		if (touchMoveHandler != null) {
			touchMoveHandler.removeHandler();
			touchMoveHandler = null;
		}
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * The graph reacts to different kinds of touch moves:
	 * <dl>
	 * <dt>Move one finger
	 * <dd>Shift view
	 * <dt>Pinch two fingers
	 * <dd>Zoom view, centered between the fingers
	 * <dt>Move two fingers
	 * <dd>Shift view (combined with zooming if pinching at the same time)
	 * </dl>
	 * Touch moves are ignored while the context menu is showing, e.g. after it was
	 * opened by a long touch and the finger has not yet been lifted.
	 */
	@Override
	public void onTouchMove(TouchMoveEvent event) {
		if (ContextMenu.isShowing())
			return;
		JsArray<Touch> touches = event.getTouches();
		switch (touches.length()) {
			case 1:
				// shift view
				Touch touch = touches.get(0);
				int x = touch.getRelativeX(element);
				int y = touch.getRelativeY(element);
				if (!inside(x, y)) {
					gesture.lose();
					return;
				}
				if (!gesture.drag(x, y))
					return;
				if (shifter != null)
					shifter.shift(gesture.getDX(), gesture.getDY());
				event.preventDefault();
				break;
			case 2:
				// process pinch zoom
				Touch touch0 = touches.get(0);
				int x0 = touch0.getRelativeX(element);
				int y0 = touch0.getRelativeY(element);
				Touch touch1 = touches.get(1);
				int x1 = touch1.getRelativeX(element);
				int y1 = touch1.getRelativeY(element);
				if (!inside(x0, y0) || !inside(x1, y1)) {
					gesture.lose();
					return;
				}
				if (!gesture.pinch(x0, y0, x1, y1))
					return;
				// moving the pinch shifts the view
				if (shifter != null)
					shifter.shift(gesture.getDX(), gesture.getDY());
				if (zoomer != null)
					zoomer.zoom(gesture.getScale(), gesture.getX(), gesture.getY());
				event.preventDefault();
				break;
			default:
				break;
		}
	}

	/**
	 * The zoom command for the context menu.
	 */
	public class ZoomCommand implements Command {

		/**
		 * The adjustment factor of the zoom level.
		 */
		double zoom = -1.0;

		/**
		 * Create a new zoom command with an adjustment factor of {@code zoom}.
		 * 
		 * @param zoom the zoom adjustment factor
		 */
		public ZoomCommand(double zoom) {
			this.zoom = Math.max(0, zoom);
		}

		/**
		 * {@inheritDoc}
		 * <p>
		 * Adjusts the zoom level by the factor {@code zoom}, for all graphs if the
		 * view handles zooming.
		 */
		@Override
		public void execute() {
			zoomer.zoom(zoom);
		}
	}

	@Override
	public HandlerRegistration addContextMenuHandler(ContextMenuHandler handler) {
		return addDomHandler(handler, ContextMenuEvent.getType());
	}

	/**
	 * Get the graph style.
	 * 
	 * @return the graph style
	 */
	public GraphStyle getStyle() {
		return style;
	}

	/**
	 * Set auto-scaling for horizontal and vertical axes.
	 * 
	 * @param x {@code true} to automatically scale the {@code x}-axis
	 * @param y {@code true} to automatically scale the {@code y}-axis
	 */
	public void autoscale(boolean x, boolean y) {
		style.autoscaleX = x;
		style.autoscaleY = y;
	}

	/**
	 * Create custom Context2d that admits drawing dashed lines.
	 */
	public static class MyContext2d extends Context2d {

		/**
		 * Create a new custom context.
		 */
		protected MyContext2d() {
		}

		/**
		 * Set the line dash pattern for drawing lines.
		 * 
		 * @param segments the line dash pattern
		 */
		public final native void setLineDash(int[] segments)
		/*-{
			this.setLineDash(segments);
		}-*/;
	}

	// public class AxesStyle {
	// public double min = 0.0;
	// public double max = 1.0;

	// public boolean autoscale = false;
	// public boolean percent = false;

	// public String label;

	// public boolean showLabel = true;
	// public boolean showLevels = true;
	// public boolean showTicks = true;
	// public boolean showTickLabels = true;
	// }
}
