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

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Component;
import java.awt.Composite;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.Transparency;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.GeneralPath;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

import javax.swing.ActionMap;
import javax.swing.ImageIcon;
import javax.swing.InputMap;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JLayeredPane;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JRootPane;
import javax.swing.JToolTip;
import javax.swing.JViewport;
import javax.swing.KeyStroke;
import javax.swing.OverlayLayout;
import javax.swing.ToolTipManager;
import javax.swing.event.MouseInputAdapter;

import org.evoludo.geom.Point2D;
import org.evoludo.math.Combinatorics;
import org.evoludo.simulator.modules.Module;

public abstract class AbstractGraph extends JLayeredPane implements ActionListener, GlassLayerListener {

	private static final long serialVersionUID = 20110423L;

	public GraphStyle style;
	private BufferedImage buffer;
	protected Graphics2D plot;
	protected double timestamp = -Double.MAX_VALUE;
	protected JViewport viewport;
	protected boolean hasHistory = false;
	protected GlassLayer glass;
	protected GraphListener controller;
	protected FrameLayer frame;
	public Rectangle canvas;
	protected JPopupMenu menu = new JPopupMenu();
	protected JMenuItem snapMenu;
	protected JMenuItem dumpMenu;
	protected JMenuItem clearMenu;
	protected JCheckBoxMenuItem svgMenu;
	protected boolean doSVG = false;
	protected GeneralPath svgPlot = new GeneralPath();
	protected boolean isLocalDynamics = false;
	protected boolean timeReversed = false;
	protected ToolTipManager tooltips;
	protected JToolTip gtip;
	protected int tooltipInitial;
	protected int tooltipDismiss;
	Module<?> module;
	protected boolean hasViewport = false;
	protected boolean zoomInOut = true;
	protected boolean zoomReset = true;
	protected boolean menuClear = false;
	protected static final String MENU_ZOOM_IN = "+";
	protected static final String MENU_ZOOM_OUT = "-";
	protected static final String MENU_ZOOM_RESET = "zoom";
	protected static final String MENU_CLEAR = "clear";
	protected static final String MENU_SAVE_IMAGE = "save:image";
	protected static final String MENU_SAVE_STATE = "save:state";
	protected static final String MENU_VECTOR = "svg";
	protected static final String MENU_ANTIALIAS = "aa";
	public static final String CUSTOM_MENU_TOGGLE_LOCAL = "local";
	public static final String CUSTOM_MENU_SET_LOCAL_NODE = "node";
	public static final String CUSTOM_MENU_TOGGLE_TIME = "time";
	protected static final Integer LAYER_CANVAS = 10;
	protected static final Integer LAYER_FRAME = 20;
	protected static final Integer LAYER_GLASS = 30;

	// <jf
	public static final int FINDNODEAT_BLOCKED = -3;
	public static final int FINDNODEAT_UNIMPLEMENTED = -2;
	public static final int FINDNODEAT_OUT_OF_BOUNDS = -1;
	// jf>

	protected AbstractGraph(GraphListener controller, Module<?> module) {
		this.controller = controller;
		this.module = module;
		setLayout(new OverlayLayout(this));
		style = new GraphStyle(this);

		// INITIALIZE TOOLTIPS
		tooltips = ToolTipManager.sharedInstance();
		tooltipDismiss = tooltips.getDismissDelay();
		tooltipInitial = tooltips.getInitialDelay();
		setToolTipText("Enabled");

		// INITIALIZE MOUSE LISTENERS
		MouseZoomListener zoomListener = new MouseZoomListener();
		addMouseListener(zoomListener);
		addMouseMotionListener(zoomListener);
	}

	/**
	 * Get the module that backs the graph.
	 * 
	 * @return the module
	 */
	public Module<?> getModule() {
		return module;
	}

	private boolean inited = false;

	/**
	 * everything should be ready now - <code>init()</code> is called only once,
	 * just before the first display
	 * NOTE: <code>init()</code> cannot itself check the status of
	 * <code>inited</code> unless it's final because otherwise
	 * overridden instances of <code>init()</code> will still be executed
	 */
	protected void init() {
		// GET STYLE PARAMETERS
		controller.initStyle(style, this);

		// graph/frame dimensions may need to be layed out again if font changed
		initGraph();

		// Note: root pane is not yet available in constructor and for snapshots in
		// simulations
		JRootPane root = getRootPane();
		if (root != null) {
			// initialize context menu
			menu.setFont(style.menuFont);
			menuClear = hasHistory;
			initMenu();
			// add resize listener only after everything should be properly setup - we do
			// not want to get notified too early.
			addComponentListener(new ResizeListener());
			// add keyboard shortcuts
			addKeyControls(root);
		}

		ToggleAntiAliasingAction.sharedInstance().addAntiAliasingListener(this);

		inited = true;
	}

	private void addKeyControls(JRootPane pane) {
		final Integer DISPLAY_TOGGLE_ANTIALIASING = 9;

		InputMap windowInput = pane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
		ActionMap windowAction = pane.getActionMap();

		// anti-aliasing
		windowInput.put(KeyStroke.getKeyStroke(KeyEvent.VK_A, 0), DISPLAY_TOGGLE_ANTIALIASING);
		windowAction.put(DISPLAY_TOGGLE_ANTIALIASING, ToggleAntiAliasingAction.sharedInstance());
	}

	/**
	 * override this method to add other/further menu items
	 */
	protected void initMenu() {
		JMenuItem menuItem;
		if (zoomInOut) {
			menuItem = new JMenuItem("Enlarge (2x)");
			menuItem.setActionCommand(MENU_ZOOM_IN);
			menuItem.addActionListener(this);
			menuItem.setFont(style.menuFont);
			menu.add(menuItem);
			menuItem = new JMenuItem("Reduce (0.5x)");
			menuItem.setActionCommand(MENU_ZOOM_OUT);
			menuItem.addActionListener(this);
			menuItem.setFont(style.menuFont);
			menu.add(menuItem);
		}
		if (zoomReset || zoomInOut) {
			menuItem = new JMenuItem("Reset Zoom");
			menuItem.setActionCommand(MENU_ZOOM_RESET);
			menuItem.addActionListener(this);
			menuItem.setFont(style.menuFont);
			menu.add(menuItem);
		}
		if (menuClear) {
			clearMenu = new JMenuItem("Clear");
			clearMenu.setActionCommand(MENU_CLEAR);
			clearMenu.addActionListener(this);
			clearMenu.setFont(style.menuFont);
			menu.add(clearMenu);
		}
		menuItem = new JCheckBoxMenuItem(ToggleAntiAliasingAction.sharedInstance());
		menuItem.setFont(style.menuFont);
		menu.add(menuItem);

		menu.add(new JPopupMenu.Separator());
		if (hasHistory && controller.hasSVG()) {
			svgMenu = new JCheckBoxMenuItem("Enable vector graphics", doSVG);
			svgMenu.setActionCommand(MENU_VECTOR);
			svgMenu.addActionListener(this);
			svgMenu.setFont(style.menuFont);
			menu.add(svgMenu);
		}
		snapMenu = new JMenuItem("Save graphics");
		snapMenu.setActionCommand(MENU_SAVE_IMAGE);
		snapMenu.addActionListener(this);
		snapMenu.setFont(style.menuFont);
		menu.add(snapMenu);
		dumpMenu = new JMenuItem("Save state");
		dumpMenu.setActionCommand(MENU_SAVE_STATE);
		dumpMenu.addActionListener(this);
		dumpMenu.setFont(style.menuFont);
		menu.add(dumpMenu);

		controller.initCustomMenu(menu, this);
	}

	public void reinit() {
		// reset timestamp
		timestamp = -Double.MAX_VALUE;
		prepare();
		// should not need to call alloc & co because if hasHistory changed, engine
		// should issue a reset
		// note: early on, plot may not yet have been initialized...
		if (hasHistory && plot != null)
			plot(plot);
		repaint();
	}

	public void reset(boolean clear) {
		// ensure that hasHistory is take into account
		// check if history settings changed - allocate plot if necessary
		if (hasHistory != hasHistory())
			initGraph();
		if (frame != null)
			canvas = frame.canvas;
		else {
			if (canvas == null)
				canvas = new Rectangle();
		}
		// init initializes the context menu - must be called before resetCustomMenu,
		// canvas must be set
		if (!inited)
			init();
		controller.resetCustomMenu(menu, this);
		clearMessage();
		calcBounds();
		if (clear)
			clear();
		reinit();
	}

	public void clear() {
		if (hasHistory && plot != null) {
			prepareGraphics2D(plot);
			Composite aComposite = plot.getComposite();
			plot.setComposite(AlphaComposite.Clear);
			// clear entire buffer - could be restricted to canvas but this is a bit tricky
			// because canvas
			// may have changed only the old canvas needs to be cleared - this is not worth
			// the effort.
			plot.fill(new Rectangle(0, 0, getWidth(), getHeight()));
			plot.setComposite(aComposite);
		}
		svgPlot.reset();
		timestamp = -Double.MAX_VALUE;
		repaint();
	}

	protected void prepare() {
	}

	@Override
	public boolean hasMessage() {
		if (frame == null)
			return false;
		return (frame.getMessage() != null);
	}

	public void setMessage(String msg) {
		if (frame != null)
			frame.setMessage(msg);
		if (glass != null)
			glass.clear();
	}

	public String getMessage() {
		if (frame != null)
			return frame.getMessage();
		return null;
	}

	public void clearMessage() {
		if (frame != null)
			frame.clearMessage();
	}

	/**
	 * get ready to be displayed
	 */
	public void activate() {
		setContextMenuEnabled(true);
		if (hasHistory)
			return; // those with hasHistory are up to date anyways
		prepare();
		repaint();
	}

	public void deactivate() {
		// setContextMenuEnabled(false); // just in case
		if (doSVG && hasHistory) {
			// disable vector graphics upon deactivation - but only for those with
			// histories, other views (PopGraph, HistoGraph) may not need this.
			doSVG = false;
			svgMenu.setState(false);
			svgPlot.reset();
		}
	}

	public void next(boolean isActive, boolean updateGUI) {
		if (hasHistory) {
			if (plot == null)
				return; // not yet ready
			prepare();
			plot(plot);
			if (!isActive || !updateGUI)
				return;
			repaint();
			return;
		}
		if (!isActive || !updateGUI)
			return;
		prepare();
		repaint();
	}

	public void end() {
		// tell glass not to plot start & current
		if (glass != null)
			glass.clear();
	}

	protected static void prepareGraphics2D(Graphics2D g2) {
		if (ToggleAntiAliasingAction.sharedInstance().getAntiAliasing())
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		else
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_DEFAULT);
	}

	protected abstract void plot(Graphics2D g);

	@Override
	protected void paintComponent(Graphics g) {
		Graphics2D g2 = (Graphics2D) g;
		prepareGraphics2D(g2);
		super.paintComponent(g);

		// draw background
		if (frame.outline != null) {
			g2.setPaint(style.background);
			g2.fill(frame.outline);
		}

		if (hasHistory) {
			Composite aComposite = g2.getComposite();
			g2.setComposite(AlphaComposite.SrcOver);
			g.drawImage(buffer, canvas.x, canvas.y, null);
			g2.setComposite(aComposite);
		} else {
			if (!hasMessage())
				plot(g2);
		}
	}

	@Override
	protected void printComponent(Graphics g) {
		if (!hasHistory || !doSVG) {
			super.printComponent(g);
			return;
		}
		Graphics2D g2 = (Graphics2D) g;
		// draw background
		if (frame.outline != null) {
			g2.setPaint(style.background);
			g2.fill(frame.outline);
		}
		g2.translate(canvas.x, canvas.y);
		g2.setStroke(style.lineStroke);
		g2.setPaint(Color.black);
		g2.draw(svgPlot);
		g2.translate(-canvas.x, -canvas.y);
	}

	protected void calcBounds() {
		if (frame != null)
			frame.init(getBounds());
		if (frame.canvas != canvas && canvas != null)
			canvas.setRect(getBounds());
	}

	protected boolean initGraph() {
		// having a frame is no longer mandatory
		Rectangle bounds = getBounds();
		// check if ready for layout stuff
		if (bounds.width <= 0 || bounds.height <= 0)
			return false;

		if (hasHistory) {
			if (buffer == null || bounds.width != buffer.getWidth() || bounds.height != buffer.getHeight()) {
				try {
					// note: apparently BufferedImage.TYPE_INT_ARGB can lead to compatibility issues
					// on different platforms - is this true?
					// buffer = new BufferedImage(bounds.width, bounds.height,
					// BufferedImage.TYPE_INT_ARGB);
					// more robust approach to creating a buffer image
					buffer = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice()
							.getDefaultConfiguration()
							.createCompatibleImage(bounds.width, bounds.height, Transparency.TRANSLUCENT);
					plot = buffer.createGraphics();
				} catch (OutOfMemoryError e) {
					// reset zoom and try to start all over - this triggers another component
					// resized event etc.
					controller.getLogger().severe("Out of memory! - resetting view.");
					zoom();
					return false;
				}
			}
		} else {
			// release resources
			if (plot != null) {
				plot.dispose();
				plot = null;
			}
			if (buffer != null)
				buffer = null;
		}
		if (frame != null)
			frame.init(bounds);

		// NOTE: zoom entire graph if parent is the viewport of a JScrollView
		hasViewport = false;
		if (getParent() instanceof JViewport) {
			hasViewport = true;
			viewport = (JViewport) getParent();
		}
		return true;
	}

	public boolean hasHistory() {
		return hasHistory;
	}

	public GraphAxis getXAxis() {
		return frame.xaxis;
	}

	public GraphAxis getYAxis() {
		return frame.yaxis;
	}

	public GraphStyle getStyle() {
		return frame.style;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		String cmd = e.getActionCommand();
		if (cmd.equals(MENU_ZOOM_IN)) {
			zoom(2.0);
			return;
		}
		if (cmd.equals(MENU_ZOOM_OUT)) {
			zoom(0.5);
			return;
		}
		if (cmd.equals(MENU_ZOOM_RESET)) {
			zoom();
			return;
		}
		if (cmd.equals(MENU_CLEAR)) {
			clear();
			return;
		}
		if (cmd.equals(MENU_SAVE_IMAGE)) {
			controller.saveSnapshot(module.getId(), hasViewport, getSnapshotFormat());
			return;
		}
		if (cmd.equals(MENU_SAVE_STATE)) {
			controller.exportState();
			return;
		}
		if (cmd.equals(MENU_VECTOR)) {
			doSVG = ((JCheckBoxMenuItem) e.getSource()).getState();
			svgPlot.reset();
			if (doSVG)
				reset(true);
			return;
		}

		if (cmd.equals(CUSTOM_MENU_TOGGLE_LOCAL)) {
			isLocalDynamics = ((JCheckBoxMenuItem) e.getSource()).getState();
			// clear and reinit canvas
			reset(true);
			return;
		}
		if (cmd.equals(CUSTOM_MENU_SET_LOCAL_NODE)) {
			// set local node and update menu entries
			// recall from MVAbstract that the menu label - we can extract the node from
			// there:
			// nodeMenu.setText("Set Location @ "+node+" (now @ "+localNode+")");
			String label = ((JMenuItem) e.getSource()).getText();
			int start = label.indexOf('@') + 2;
			int end = label.indexOf('(', start) - 1;
			int node = Integer.parseInt(label.substring(start, end));
			controller.setLocalNode(node);
			// note: all views with local dynamics and histories should be reset after a
			// change here!
			return;
		}
		if (cmd.equals(CUSTOM_MENU_TOGGLE_TIME)) {
			timeReversed = ((JCheckBoxMenuItem) e.getSource()).getState();
			controller.setTimeReversed(timeReversed);
		}
	}

	public static final int SNAPSHOT_NONE = -1;
	public static final int SNAPSHOT_PNG = 0;
	public static final int SNAPSHOT_SVG = 1;
	public static final int SNAPSHOT_PDF = 2;
	public static final int SNAPSHOT_EPS = 3;

	protected int getSnapshotFormat() {
		// ignore by default - should become SVG at some point
		return SNAPSHOT_NONE;
	}

	protected void zoom() {
		style.zoom();
		if (hasViewport) {
			// reset the bounds to the visible part of the viewport
			// this will issue a component resized event, which will then take care of
			// resetting the view, repaint etc...
			zoomSize.setSize(viewport.getExtentSize());
			revalidate();
			repaint();
			return;
		}
		reset(true);
	}

	protected void zoom(double s) {
		zoom(s, s);
	}

	private final Dimension zoomSize = new Dimension(-1, -1);

	public void zoom(double xs, double ys) {
		if (!hasViewport) {
			if (frame != null) {
				GraphAxis x = frame.xaxis;
				// zoom only range of ordinate
				double range = (x.upper - x.lower) * xs;
				// note: for now, x.max is fixed (at zero); needs to change for histograms -
				// e.g. override zoomView in subclasses
				x.lower = x.upper - range;
			}
			reset(true);
			return;
		}
		Dimension mysize = getSize();
		Dimension viewsize = viewport.getExtentSize();
		zoomSize.setSize((int) (mysize.width * xs + 0.5), (int) (mysize.height * ys + 0.5));
		Point loc = new Point(viewport.getViewPosition());
		double halfWidth = viewsize.width / 2.0;
		double halfHeight = viewsize.height / 2.0;
		loc.x = (int) Math.max(0.0, (loc.x + halfWidth) * xs + 0.5 - halfWidth);
		loc.y = (int) Math.max(0.0, (loc.y + halfHeight) * xs + 0.5 - halfHeight);
		style.zoom(xs, ys);
		// this will issue a component resized event, which will then take care of
		// resetting the view etc...
		revalidate();
		// place translate request on EDT stack so that it is processed after viewport
		// has resized
		translate(loc);
	}

	public void translate(int x, int y) {
		translate(new Point(x, y));
	}

	public void translate(Point shift) {
		class ViewPositionSetter implements Runnable {
			JViewport vp;
			Point p;

			public ViewPositionSetter(JViewport vp, Point p) {
				this.vp = vp;
				this.p = p;
			}

			@Override
			public void run() {
				vp.setViewPosition(p);
			}
		}
		javax.swing.SwingUtilities.invokeLater(new ViewPositionSetter(viewport, shift));
	}

	/**
	 * {$inheritDoc}
	 * <p>
	 * <strong>Note:</strong> this is important such that the contents of the
	 * viewport can exceed the viewport's size
	 */
	@Override
	public Dimension getPreferredSize() {
		return zoomSize;
	}

	// subclasses may do something with these clicks...
	protected void mouseClick(Point loc, int count) {
	}

	// subclasses may do something with these mouse presses...
	public static final int MOUSE_DRAG_START = 0;

	public static final int MOUSE_DRAG_DRAW = 1;

	public static final int MOUSE_DRAG_END = 2;

	protected boolean mouseDrag(Point loc, int mode, int stage) {
		return false;
	}

	/*
	 * tool tips
	 */
	// dynamically changing tooltips require direct access to JToolTip instance
	@Override
	public JToolTip createToolTip() {
		gtip = super.createToolTip();
		// gtip.setOpaque(false);
		// make tooltip background translucent - use default color, add alpha
		Color bg = gtip.getBackground();
		// transparency of tooltips seems poorly supported on windows - it appears to be
		// either opaque or very transparent
		gtip.setBackground(new Color(bg.getRed(), bg.getGreen(), bg.getBlue(), 160));
		return gtip;
	}

	@Override
	public String getToolTipText(MouseEvent event) {
		return null;
	}

	// <jf override if you can
	/**
	 * Find the node at the given location. Override in subclasses to implement.
	 * 
	 * @param location the point to check for nodes
	 * @return node index or negative value indicating no node/error condition
	 */
	public int findNodeAt(Point location) {
		// Parameter 'location' is part of the API contract for subclasses
		return FINDNODEAT_UNIMPLEMENTED;
	}
	// jf>

	// on Max OS X the java focus system is strange/buggy...
	// at least close menu if applet/application loses focus - this is non-standard
	// behavior
	boolean contextMenuEnabled = false;

	public void setContextMenuEnabled(boolean enabled) {
		// System.out.println("setContextMenuEnabled:
		// contextMenuEnabled="+contextMenuEnabled+", enabled="+enabled);
		contextMenuEnabled = enabled;
		if (!contextMenuEnabled && menu.isVisible())
			menu.setVisible(false);
	}

	protected void showPopupMenu(Component comp, Point mouse) {
		// System.out.println("showPopupMenu: contextMenuEnabled="+contextMenuEnabled+",
		// mouse="+mouse+", component="+comp);
		if (!contextMenuEnabled)
			return;
		boolean idle = !controller.isRunning();
		snapMenu.setEnabled(idle);
		dumpMenu.setEnabled(idle);
		controller.showCustomMenu(menu, mouse, this);
		menu.show(comp, mouse.x, mouse.y);

		mouseAction = MOUSE_MENU;
	}

	protected boolean isPopupMenuVisible() {
		return menu.isVisible();
	}

	// note: context sensitive popupmenu does not disappear if active and then
	// switching apps - how to resolve?!
	/*
	 * // implement PopupMenuListener
	 * public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
	 * }
	 * 
	 * public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
	 * }
	 * 
	 * public void popupMenuCanceled(PopupMenuEvent e) {
	 * }
	 */

	// implement GlassLayerListener
	@Override
	public Point2D getState() {
		return new Point2D(0, 0);
	}

	@Override
	public Point2D getStart() {
		return new Point2D(0, 0);
	}

	// implement ComponentListener - needed for zoom
	private class ResizeListener extends ComponentAdapter {

		private final Dimension dim = new Dimension();

		@Override
		public void componentResized(ComponentEvent e) {
			Dimension newSize = getSize();
			if (newSize.width == dim.width && newSize.height == dim.height)
				return;
			dim.setSize(newSize);
			initGraph();
			calcBounds();
			repaint();
		}
	}

	// implement MouseListener
	Rectangle currRect = new Rectangle();
	Rectangle drawRect = new Rectangle();
	Rectangle prevRect = new Rectangle();
	Rectangle repaintRect = new Rectangle();

	private int mouseAction = MOUSE_NONE;

	// mouse action modes
	protected static final int MOUSE_NONE = 0;
	protected static final int MOUSE_ZOOM = 1;
	protected static final int MOUSE_MOVE = 2;
	protected static final int MOUSE_GRAB = 6;
	protected static final int MOUSE_MENU = 3;
	protected static final int MOUSE_DRAW = 4;
	protected static final int MOUSE_CLICK = 5;
	protected static final Cursor moveCursor;
	protected static final Cursor grabCursor;
	protected static final Cursor zoomInCursor;
	protected static final Cursor zoomOutCursor;
	protected static final Cursor drawCursor;

	static {
		if (java.awt.GraphicsEnvironment.isHeadless()) {
			moveCursor = null;
			grabCursor = null;
			zoomInCursor = null;
			zoomOutCursor = null;
			drawCursor = null;
		} else {
			Toolkit tk = Toolkit.getDefaultToolkit();
			// need to use getResource() to prevent security exceptions in applets
			moveCursor = tk.createCustomCursor(
					new ImageIcon(AbstractGraph.class.getResource("/images/cursorMove.png")).getImage(),
					new Point(0, 0), "Move");
			grabCursor = tk.createCustomCursor(
					new ImageIcon(AbstractGraph.class.getResource("/images/cursorGrab.png")).getImage(),
					new Point(7, 0), "Grab");
			zoomInCursor = tk.createCustomCursor(
					new ImageIcon(AbstractGraph.class.getResource("/images/cursorZoomIn.png")).getImage(),
					new Point(2, 2), "Zoom");
			zoomOutCursor = tk.createCustomCursor(
					new ImageIcon(AbstractGraph.class.getResource("/images/cursorZoomOut.png")).getImage(),
					new Point(2, 2), "Zoom");
			drawCursor = tk.createCustomCursor(
					new ImageIcon(AbstractGraph.class.getResource("/images/cursorDraw.png")).getImage(),
					new Point(0, 14), "Draw");
		}
	}

	synchronized void shiftView(MouseEvent e) {
		Point mouse = e.getPoint();
		Rectangle portrect = viewport.getViewRect();
		Dimension portsize = viewport.getViewSize();
		viewport.setViewPosition(
				new Point(Math.min(Math.max(portrect.x - (mouse.x - currRect.x), 0), portsize.width - portrect.width),
						Math.min(Math.max(portrect.y - (mouse.y - currRect.y), 0), portsize.height - portrect.height)));
	}

	protected void zoomView() {
		if (frame == null)
			return;
		Dimension vvs = viewport.getExtentSize();
		double s = Math.min((double) vvs.width / (double) (drawRect.width + 1),
				(double) vvs.height / (double) (drawRect.height + 1));
		// note that zoom also translates the view
		zoom(s);
		// place another translate request on EDT stack
		translate((int) (drawRect.x * s + 0.5), (int) (drawRect.y * s + 0.5));
	}

	protected void setZoomRect(Point mouse) {
		if (frame == null)
			return;
		if (frame.contains(mouse))
			currRect.setBounds(mouse.x, mouse.y, 0, 0);
		else {
			int x = Math.min(Math.max(mouse.x, canvas.x), canvas.x + canvas.width);
			int y = Math.min(Math.max(mouse.y, canvas.y), canvas.y + canvas.height);
			currRect.setBounds(x, y, 0, 0);
		}
		drawRect.setBounds(currRect);
	}

	protected void updateZoomRect(Point mouse, boolean shift) {
		int x = currRect.x;
		int y = currRect.y;
		int width = mouse.x - x;
		int height = mouse.y - y;
		if (shift) {
			width = Math.min(width, height);
			height = width;
		}
		if (width < 0) {
			x += width;
			width = -width;
		}
		if (height < 0) {
			y += height;
			height = -height;
		}
		drawRect.setBounds(x, y, width, height);
		Rectangle2D.intersect(canvas, drawRect, drawRect);
	}

	protected double convertCoord2X(int xcoord) {
		if (frame == null)
			return -1.0;
		GraphAxis x = frame.xaxis;
		return (xcoord - canvas.x) * (x.upper - x.lower) / canvas.width;
	}

	protected double convertCoord2Y(int ycoord) {
		if (frame == null)
			return -1.0;
		GraphAxis y = frame.yaxis;
		return (canvas.height - (ycoord - canvas.y)) * (y.upper - y.lower) / canvas.height;
	}

	protected void zoomRange() {
		if (frame == null)
			return;
		GraphAxis x = frame.xaxis;
		GraphAxis y = frame.yaxis;
		double old = (x.upper - x.lower) / canvas.width;
		double range = (drawRect.width + 1) * old;
		double order = Combinatorics.pow(10.0, (int) Math.floor(Math.log10(range)));
		double coord = (drawRect.x - 1 - canvas.x) * old;
		x.lower += Math.round(coord / order * 10.0) * 0.1 * order;
		x.upper = x.lower + Math.round(range / order * 10.0) * 0.1 * order;

		old = (y.upper - y.lower) / canvas.height;
		range = (drawRect.height + 1) * old;
		order = Combinatorics.pow(10.0, (int) Math.floor(Math.log10(range)));
		coord = (canvas.height - (drawRect.y - 1 - canvas.y)) * old;
		y.upper = y.lower + Math.round(coord / order * 10.0) * 0.1 * order;
		coord = (canvas.height - (drawRect.y + drawRect.height - canvas.y)) * old;
		y.lower += Math.round(coord / order * 10.0) * 0.1 * order;

		frame.formatTickLabels();
	}

	private class MouseZoomListener extends MouseInputAdapter {

		@Override
		public void mousePressed(MouseEvent e) {
			Point mouse = e.getPoint();
			if (e.isPopupTrigger()) {
				// show popup menu
				showPopupMenu(e.getComponent(), mouse);
				e.consume();
				return;
			}

			if (e.isAltDown()) {
				// zoom view
				setZoomRect(mouse);
				mouseAction = MOUSE_ZOOM;
				frame.mousedrag = drawRect;
				setCursor(new Cursor(Cursor.CROSSHAIR_CURSOR));
				repaint();
				return;
			}

			if (e.isShiftDown() && e.getClickCount() < 2) {
				// shift view
				if (!hasViewport) {
					mouseAction = MOUSE_NONE;
					return;
				}
				Dimension viewportsize = viewport.getSize();
				if (viewportsize.width >= canvas.width && viewportsize.height >= canvas.height)
					return;
				mouseAction = MOUSE_MOVE;
				currRect.setBounds(mouse.x, mouse.y, 0, 0);
				return;
			}

			if (e.getClickCount() >= 2 && mouseDrag(mouse, MOUSE_DRAW, MOUSE_DRAG_START)) {
				mouseAction = MOUSE_DRAW;
				setCursor(drawCursor);
				return;
			}

			if (mouseDrag(mouse, MOUSE_GRAB, MOUSE_DRAG_START)) {
				mouseAction = MOUSE_GRAB;
				setCursor(grabCursor);
				return;
			}

			mouseAction = MOUSE_CLICK;
		}

		@Override
		public void mouseDragged(MouseEvent e) {
			switch (mouseAction) {
				case MOUSE_MENU:
					mouseAction = MOUSE_NONE;
					return;
				case MOUSE_NONE:
					return;
				case MOUSE_ZOOM:
					updateZoomRect(e.getPoint(), e.isShiftDown());
					frame.repaint();
					return;
				case MOUSE_MOVE:
					setCursor(moveCursor);
					shiftView(e);
					return;
				case MOUSE_DRAW:
				case MOUSE_GRAB:
					mouseDrag(e.getPoint(), mouseAction, MOUSE_DRAG_DRAW);
					return;
				default:
			}
		}

		@Override
		public void mouseReleased(MouseEvent e) {
			setCursor(null);
			// <jf 100319 isPopupTrigger event on windows occurs on mouseUp
			if (e.isPopupTrigger()) {
				showPopupMenu(e.getComponent(), e.getPoint());
				e.consume();
				return;
			}
			// jf>
			switch (mouseAction) {
				case MOUSE_CLICK:
					mouseClick(e.getPoint(), e.getClickCount());
					return;
				case MOUSE_NONE:
					return;
				case MOUSE_ZOOM:
					mouseAction = MOUSE_NONE;
					frame.mousedrag = null;
					if (drawRect.width < 10 || drawRect.height < 10) {
						repaint();
						return;
					}
					if (hasViewport) {
						zoomView();
						repaint();
						return;
					}
					zoomRange();
					repaint();
					return;
				case MOUSE_MOVE:
					mouseAction = MOUSE_NONE;
					return;
				case MOUSE_GRAB:
				case MOUSE_DRAW:
					mouseDrag(e.getPoint(), mouseAction, MOUSE_DRAG_END);
					mouseAction = MOUSE_NONE;
					return;
				default:
			}
		}
	}
}
