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

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.ButtonGroup;
import javax.swing.InputMap;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.KeyStroke;

import org.evoludo.geom.Node2D;
import org.evoludo.geom.Path2D;
import org.evoludo.geom.PathIterator;
import org.evoludo.geom.Point2D;
import org.evoludo.simulator.geom.AbstractGeometry;
import org.evoludo.simulator.Network;
import org.evoludo.simulator.Network.Status;
import org.evoludo.simulator.Network2D;
import org.evoludo.simulator.geom.GeometryFeatures;
import org.evoludo.simulator.geom.GeometryType;
import org.evoludo.simulator.geom.HierarchicalGeometry;
import org.evoludo.simulator.models.Model;
import org.evoludo.simulator.models.ModelType;
import org.evoludo.simulator.modules.Module;
import org.evoludo.util.Formatter;

/**
 *
 * @author Christoph Hauert
 */
public class PopGraph2D extends AbstractGraph implements Network.LayoutListener {

	private static final long serialVersionUID = 20110423L;

	protected Network2D network;

	/**
	 * Maximum number of nodes in network for animated layout.
	 */
	static final int MAX_ANIMATE_LAYOUT_VERTICES_DEFAULT = 1000;

	/**
	 * Maximum number of edges in network for animated layout.
	 */
	static final int MAX_ANIMATE_LAYOUT_LINKS_DEFAULT = 5000;

	protected boolean animate = true;
	protected AbstractGeometry geometry;
	protected Color[] colors;

	double scaleX = 1.0;
	double scaleY = 1.0;
	double fLinks = 1.0;

	protected JCheckBoxMenuItem animateMenu = new JCheckBoxMenuItem("Animate layout");
	protected JMenuItem shakeMenu = new JMenuItem("Shake network");
	protected JMenu linkMenu = new JMenu("Visible links");

	protected static final String MENU_ANIMATE = "animate";
	protected static final String MENU_SHAKE = "shake";
	protected static final String MENU_LINK_NONE = "0";
	protected static final String MENU_LINK_1 = "1";
	protected static final String MENU_LINK_5 = "5";
	protected static final String MENU_LINK_10 = "10";
	protected static final String MENU_LINK_50 = "50";
	protected static final String MENU_LINK_ALL = "100";
	protected static final int MAX_LINK_COUNT = 10000;

	public PopGraph2D(PopListener controller, AbstractGeometry geometry, Module<?> module) {
		super(controller, module);
		this.geometry = geometry;

		// enable clear menu
		menuClear = true;
		setOpaque(false);

		InputMap windowInput = getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
		ActionMap windowAction = getActionMap();
		windowInput.put(KeyStroke.getKeyStroke(KeyEvent.VK_S, 0), MENU_SHAKE);
		windowAction.put(MENU_SHAKE, new AbstractAction() {
			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(ActionEvent e) {
				shakeNetwork();
			}
		});
		frame = new FrameLayer(style);
		add(frame, LAYER_FRAME);
		if (geometry == null)
			return;
		network = geometry.getNetwork2D();
		if (network == null) {
			Network2DJRE net = new Network2DJRE(controller.getEngine(), geometry);
			geometry.setNetwork2D(net);
			network = net;
		}
		network.reset();
	}

	// this is up-to-date after call to controller.initData()
	protected void checkGraph() {
		ModelType model = controller.getEngine().getModel().getType();
		switch (model) {
			case ODE:
				setMessage("No view available (ODE Solver)");
				return;
			case SDE:
				setMessage("No view available (SDE Solver)");
				return;
			default:
		}
		if (geometry.isType(GeometryType.CUBE)) {
			setMessage("No representation for geometry!");
			return;
		}
		if (network.getNLinks() * fLinks > MAX_LINK_COUNT) {
			setMessage(
					"Too many links to draw\n(" + Formatter.formatPercent(fLinks, 0) + " out of " + network.getNLinks()
							+ ")");
			network.getLinks().reset();
		}
	}

	@Override
	public void setMessage(String msg) {
		super.setMessage(msg);
		if (network != null)
			network.setStatus(msg == null ? Status.NEEDS_LAYOUT : Status.HAS_MESSAGE);
	}

	protected double completed = 0.0;

	@Override
	public synchronized void layoutUpdate(double p) {
		if (!isVisible())
			return;
		if (hasAnimatedLayout()) {
			// complete layout
			network.finishLayout();
			forceRepaint = true;
			clearMessage();
			repaint();
			return;
		}
		if (p > completed) {
			setMessage("layout progress: " + Formatter.format(100.0 * p, 0) + "%");
			completed = p;
		}
		paintImmediately(getBounds());
	}

	@Override
	public synchronized void layoutComplete() {
		clearMessage();
		if (!isVisible())
			return;
		repaint();
	}

	@Override
	public void reinit() {
		if (hasHistory)
			clear();
		((PopListener) controller).initColor(module.getID());
		super.reinit();
	}

	// note: upon initialization reset is called twice: once population fires reset
	// and once resize! all kinds of opportunities for race conditions!
	@Override
	public void reset(boolean clear) {
		if (geometry == null) { // ODE/SDE models
			super.reset(clear || hasHistory);
			return;
		}
		if (network != null)
			network.reset();
		allocColors();
		int node = controller.getLocalNode();
		if (node < 0 || node >= geometry.getSize())
			controller.setLocalNode(geometry.getSize() / 2);
		infonode = -1;
		completed = 0.0;

		super.reset(clear || hasHistory);
		// enable/disable context menu items
		animateMenu.setState(animate);
		switch (geometry.getType()) {
			case TRIANGULAR:
			case HEXAGONAL:
			case SQUARE_NEUMANN:
			case SQUARE_NEUMANN_2ND:
			case SQUARE_MOORE:
			case SQUARE:
			case CUBE:
			case LINEAR:
				animateMenu.setEnabled(false);
				shakeMenu.setEnabled(false);
				linkMenu.setEnabled(false);
				break;

			// case GENERIC:
			// case DYNAMIC:
			// and many more...
			default:
				animateMenu.setEnabled(true);
				shakeMenu.setEnabled(true);
				linkMenu.setEnabled(true);
		}
		if (hasHistory && plot != null) {
			forceRepaint = true;
			plot(plot);
			repaint();
		}
	}

	@Override
	public boolean hasHistory() {
		hasHistory = false;
		if (geometry == null)
			return hasHistory;
		hasHistory = geometry.isType(GeometryType.LINEAR);
		if (clearMenu != null)
			clearMenu.setEnabled(hasHistory);
		return hasHistory;
	}

	@Override
	protected void initMenu() {
		super.initMenu();
		menu.add(new JPopupMenu.Separator());
		shakeMenu.setActionCommand(MENU_SHAKE);
		shakeMenu.addActionListener(this);
		shakeMenu.setFont(style.menuFont);
		menu.add(shakeMenu);
		animateMenu.setActionCommand(MENU_ANIMATE);
		animateMenu.addActionListener(this);
		animateMenu.setFont(style.menuFont);
		menu.add(animateMenu);
		ButtonGroup group = new ButtonGroup();
		JRadioButtonMenuItem radioItem = new JRadioButtonMenuItem("All");
		radioItem.setActionCommand(MENU_LINK_ALL);
		radioItem.setSelected(true);
		radioItem.addActionListener(this);
		radioItem.setFont(style.menuFont);
		group.add(radioItem);
		linkMenu.setFont(style.menuFont);
		linkMenu.add(radioItem);
		radioItem = new JRadioButtonMenuItem("50%");
		radioItem.setActionCommand(MENU_LINK_50);
		radioItem.addActionListener(this);
		radioItem.setFont(style.menuFont);
		group.add(radioItem);
		linkMenu.add(radioItem);
		radioItem = new JRadioButtonMenuItem("10%");
		radioItem.setActionCommand(MENU_LINK_10);
		radioItem.addActionListener(this);
		radioItem.setFont(style.menuFont);
		group.add(radioItem);
		linkMenu.add(radioItem);
		radioItem = new JRadioButtonMenuItem("5%");
		radioItem.setActionCommand(MENU_LINK_5);
		radioItem.addActionListener(this);
		radioItem.setFont(style.menuFont);
		group.add(radioItem);
		linkMenu.add(radioItem);
		radioItem = new JRadioButtonMenuItem("1%");
		radioItem.setActionCommand(MENU_LINK_1);
		radioItem.addActionListener(this);
		radioItem.setFont(style.menuFont);
		group.add(radioItem);
		linkMenu.add(radioItem);
		radioItem = new JRadioButtonMenuItem("None");
		radioItem.setActionCommand(MENU_LINK_NONE);
		radioItem.addActionListener(this);
		radioItem.setFont(style.menuFont);
		group.add(radioItem);
		linkMenu.add(radioItem);
		menu.add(linkMenu);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		String cmd = e.getActionCommand();
		if (cmd.equals(MENU_ANIMATE)) {
			setAnimateLayout(((JCheckBoxMenuItem) e.getSource()).getState());
			return;
		}
		if (cmd.equals(MENU_SHAKE)) {
			shakeNetwork();
			return;
		}
		if (cmd.equals(MENU_LINK_NONE)) {
			changeVisibleLinkFraction(0.0);
			return;
		}
		if (cmd.equals(MENU_LINK_1)) {
			changeVisibleLinkFraction(0.01);
			return;
		}
		if (cmd.equals(MENU_LINK_5)) {
			changeVisibleLinkFraction(0.05);
			return;
		}
		if (cmd.equals(MENU_LINK_10)) {
			changeVisibleLinkFraction(0.1);
			return;
		}
		if (cmd.equals(MENU_LINK_50)) {
			changeVisibleLinkFraction(0.5);
			return;
		}
		if (cmd.equals(MENU_LINK_ALL)) {
			changeVisibleLinkFraction(1.0);
			return;
		}
		super.actionPerformed(e);
	}

	protected void changeVisibleLinkFraction(double newFraction) {
		double oldfLinks = fLinks;
		fLinks = newFraction;
		int nLinks = network.getNLinks();
		if (nLinks * oldfLinks > MAX_LINK_COUNT) {
			// check if things only get worse - or at least not substantially better
			if (nLinks * fLinks > MAX_LINK_COUNT)
				return;
			// no, things are getting better but we may need a new layout
			clearMessage();
			if (network.isStatus(Status.HAS_LAYOUT))
				layoutComplete();
			network.doLayout(this);
			return;
		}
		if (nLinks * fLinks > MAX_LINK_COUNT) {
			setMessage("Too many links to draw");
			network.getLinks().reset();
			clear();
			repaint();
			return;
		}
		if (network.getStatus().requiresLayout()) {
			network.doLayout(this);
			return;
		}
		clear();
		repaint();
	}

	boolean hasAnimatedLayout() {
		if (!animate)
			return false;
		GeometryFeatures gFeats = geometry.getFeatures();
		int nodeCount = geometry.getSize();
		return (nodeCount <= MAX_ANIMATE_LAYOUT_VERTICES_DEFAULT
				&& (int) (gFeats.avgTot * nodeCount) < 2 * MAX_ANIMATE_LAYOUT_LINKS_DEFAULT);
	}

	public void setAnimateLayout(boolean animate) {
		this.animate = animate;
		animateMenu.setState(animate);
	}

	protected void shakeNetwork() {
		if (network.getStatus().requiresLayout())
			return;
		completed = 0.0;
		network.shake(this, 0.05);
	}

	protected void allocColors() {
		if (geometry.getSize() <= 0)
			return;
		if (colors == null || colors.length != geometry.getSize())
			colors = new Color[geometry.getSize()];
	}

	// allow subclasses to handle resetting the zoom differently (e.g. PopGraph3D
	// should preserve the view position)
	protected void zoom(boolean resetView) {
		zoom();
	}

	protected boolean forceRepaint = false;

	// tool tips
	protected int infonode = -1; // save node for dynamical updates of info

	protected Point infoloc = null;

	public void prepareSnapshot(Graphics2D g2d, Rectangle myCanvas) {
		if (canvas == null)
			canvas = new Rectangle();
		canvas.setBounds(myCanvas);
		if (network.getStatus().requiresLayout())
			network.doLayout(this);
		drawLinks(g2d, canvas);
		drawNodes(g2d, canvas, true);
	}

	boolean isHierarchy = false;
	int hLevels = 0;
	int[] hPeriods = null;
	static final int HIERARCHY_GAP = 3; // unit gap in pixels
	int hierarchyGap = 0;

	@Override
	protected void calcBounds() {
		super.calcBounds();
		checkGraph();
		if (frame.msg != null)
			return;
		// adjust canvas, init dw, dh etc.
		// init/reset all (helper) variables
		int dw = 1;
		int dh = 1;
		int dR = 1;
		int dw2 = 0;
		int dh3 = 0;
		int width = canvas.width;
		int height = canvas.height;
		int side;

		GeometryType type = geometry.getType();
		isHierarchy = geometry.isType(GeometryType.HIERARCHY);
		if (isHierarchy)
			type = ((HierarchicalGeometry) geometry).getSubType();
		if (!isHierarchy || geometry.getType() != GeometryType.SQUARE)
			hPeriods = null;
		// geometries that have special/fixed layout
		switch (type) {
			case CUBE:
				// cannot deal with 3D structures
				break;

			case SQUARE_NEUMANN:
			case SQUARE_NEUMANN_2ND:
			case SQUARE_MOORE:
			case SQUARE:
				side = (int) (Math.sqrt(geometry.getSize()) + 0.5);
				// for hierarchical structures add gap between units
				hierarchyGap = 0;
				if (isHierarchy) {
					int[] hierarchy = ((HierarchicalGeometry) geometry).getHierarchyLevels();
					hLevels = hierarchy.length - 1;
					if (hPeriods == null || hPeriods.length != hLevels)
						hPeriods = new int[hLevels];
					hPeriods[0] = (int) Math.sqrt(hierarchy[hLevels]);
					hierarchyGap = side / hPeriods[0] - 1;
					for (int i = 1; i < hLevels; i++) {
						hPeriods[i] = hPeriods[i - 1] * (int) Math.sqrt(hierarchy[hLevels - i]);
						hierarchyGap += side / hPeriods[i] - 1;
					}
					hierarchyGap *= HIERARCHY_GAP;
				}
				// keep sites square
				dw = Math.min((width - hierarchyGap) / side, (height - hierarchyGap) / side);
				dh = dw;
				width = dw * side;
				height = dh * side;
				break;

			case TRIANGULAR:
				side = (int) (Math.sqrt(geometry.getSize()) + 0.5);
				dw2 = width / (side + 1);
				width = dw2 * (side + 1);
				dw = 2 * dw2;
				dh = height / side;
				height = dh * side;
				break;

			case HEXAGONAL:
				side = (int) (Math.sqrt(geometry.getSize()) + 0.5);
				dw2 = width / (2 * side + 1);
				width = dw2 * (2 * side + 1);
				dw = 2 * dw2;
				dh3 = height / (3 * side + 1);
				height = dh3 * (3 * side + 1);
				dh = 3 * dh3;
				break;

			case LINEAR:
				side = geometry.getSize();
				dw = width / side;
				if (dw < 1)
					break;
				dh = dw;
				width = dw * side;
				height -= height % dh;
				frame.yaxis.steps = height / dh;
				frame.xaxis.restore();
				frame.yaxis.restore();
				break;

			default:
				width = 2 * Math.min(width / 2, height / 2);
				height = width;
		}
		if (canvas.width > 0 && canvas.height > 0 && dw > 0 && dh > 0 && dR > 0) {
			canvas.x += (canvas.width - width) / 2;
			canvas.y += (canvas.height - height) / 2;
			canvas.width = width;
			canvas.height = height;
		}
		// adjust frame
		if (frame != null)
			frame.init();
	}

	@Override
	public void next(boolean isActive, boolean updateGUI) {
		if (isActive && geometry != null && geometry.isType(GeometryType.DYNAMIC))
			// invalidate time stamp
			timestamp = -Double.MAX_VALUE;
		super.next(isActive, updateGUI);
	}

	@Override
	protected void prepare() {
		if (geometry == null) // ODE/SDE models
			return;
		int id = module.getID();
		boolean isDynamic = geometry.isType(GeometryType.DYNAMIC);
		if (isDynamic) {
			if (timestamp < network.getTimestamp()) {
				// time stamp expired - get data
				// invalidate network layout
				if (network.isStatus(Status.HAS_LAYOUT))
					network.setStatus(Status.ADJUST_LAYOUT);
			}
			if (frame.msg != null)
				return;
			network.doLayout(this);
			return;
		}
		if (frame.msg != null)
			return; // no need to fetch data
		if (network.getStatus().requiresLayout())
			network.doLayout(this);
		// dynamically update tooltip info
		if (gtip != null && gtip.isShowing()) {
			if (infoloc != null)
				// confirm focus node - may have shifted
				infonode = findNodeAt(infoloc);
			gtip.setTipText(((PopListener) controller).getInfoAt(network, infonode, id));
		}
	}

	@Override
	protected synchronized void plot(Graphics2D g2d) {
		if (frame.msg != null) {
			// timestamp = network.timestamp;
			return;
		}
		// if( !hasHistory && !forceRepaint && network.timestamp<=timestamp )
		// return; // up to date
		drawLinks(g2d);
		drawNodes(g2d);
		// DEBUG: placement of nodes vs links
		// drawLinks(plot);
		// give controller a chance to add some finishing touches
		controller.polish(g2d, this);
		if (network.isStatus(Status.LAYOUT_IN_PROGRESS))
			return;
		timestamp = network.getTimestamp();
	}

	// tool tips

	@Override
	public String getToolTipText(MouseEvent event) {
		infoloc = event.getPoint();
		if (!canvas.contains(infoloc) || frame.msg != null || isMouseDrawing) {
			infonode = -1;
			return null;
		}
		infonode = findNodeAt(infoloc);
		return ((PopListener) controller).getInfoAt(network, infonode, module.getID());
	}

	private int refnode = -1;
	private boolean isMouseDrawing = false;

	private final Point mouse = new Point();

	@Override
	protected boolean mouseDrag(Point loc, int mode, int stage) {
		Model model = controller.getEngine().getModel();
		ModelType mt = model.getType();

		switch (mode) {
			case MOUSE_GRAB:
				if (frame.msg != null || mt.isPDE())
					return false;
				switch (stage) {
					case MOUSE_DRAG_START:
						if (!canvas.contains(loc))
							return false; // do not even get started!
						if (network == null || network.toArray() == null)
							return false;
						// set reference node but do not start drawing yet
						refnode = findNodeAt(loc);
						// clip mouse position relative to interior of canvas
						mouse.setLocation(loc.x - canvas.x, loc.y - canvas.y);
						return (refnode >= 0);

					case MOUSE_DRAG_DRAW:
						Node2D myNode = network.get(refnode);
						double rr = network.getRadius();
						double r = myNode.getR() * 0.5; // radius is actually the diameter...
						double iscale = (rr + rr) / canvas.width; // same as height in this case
						// clip mouse position relative to interior of canvas
						loc.x = Math.max(0, Math.min(canvas.width, loc.x - canvas.x));
						loc.y = Math.max(0, Math.min(canvas.height, loc.y - canvas.y));
						double dx = (mouse.x - loc.x) * iscale;
						double dy = (mouse.y - loc.y) * iscale;
						rr -= r;
						double x = Math.max(-rr, Math.min(myNode.getX() - dx, rr));
						double y = Math.max(-rr, Math.min(myNode.getY() - dy, rr));
						myNode.set(x, y);
						mouse.setLocation(loc);
						repaint();
						return true;

					case MOUSE_DRAG_END:
						// recalculate & repaint links
						network.linkNodes();
						layoutComplete();
						return true;

					default:
						return false;
				}

			case MOUSE_DRAW:
				int node = -1;
				if (!canvas.contains(loc) || frame.msg != null || mt.isPDE())
					return false;
				switch (stage) {
					case MOUSE_DRAG_START:
						// set reference node but do not start drawing yet
						refnode = findNodeAt(loc);
						return (refnode >= 0);

					case MOUSE_DRAG_DRAW:
						isMouseDrawing = true;
						if (refnode < 0)
							return false;
						// find hit node
						node = findNodeAt(loc);
						// make sure changes get repainted
						if (hasHistory)
							forceRepaint = true;
						// worth keeping? only if GWT is on board as well.
						// ((PopListener)controller).mouseHitNode(node, refnode, tag); // population
						// signals change back to us
						((PopListener) controller).mouseHitNode(node, module.getID()); // population signals change back
																						// to us
						return true;

					case MOUSE_DRAG_END:
						// if refnode has not been set, perform strategy change (this is a click rather
						// than drawing)
						if (!isMouseDrawing) {
							node = findNodeAt(loc);
							// make sure changes get repainted
							if (hasHistory)
								forceRepaint = true;
							((PopListener) controller).mouseHitNode(node, module.getID()); // population signals change
																							// back to us
						}
						setToolTipText(((PopListener) controller).getInfoAt(network, node, module.getID()));
						// dispatch fake mouse moved event to re-display tooltip
						dispatchEvent(new MouseEvent(this, MouseEvent.MOUSE_MOVED, System.currentTimeMillis(), 0, loc.x,
								loc.y, 0, false));
						isMouseDrawing = false;
						refnode = -1;
						return true;
					default:
						return false;
				}

			default:
				return false;
		}
	}

	@Override
	protected int getSnapshotFormat() {
		// most appropriate format depends on what is displayed
		GeometryType type = geometry.getType();
		if (isHierarchy)
			type = ((HierarchicalGeometry) geometry).getSubType();
		switch (type) {
			case TRIANGULAR:
			case HEXAGONAL:
			case SQUARE_NEUMANN:
			case SQUARE_NEUMANN_2ND:
			case SQUARE_MOORE:
			case SQUARE:
			case CUBE:
			case LINEAR:
				return SNAPSHOT_PNG;

			case WELLMIXED:
			case COMPLETE:
			case WHEEL:
			case STAR:
			case SUPER_STAR:
			case DYNAMIC:
				return SNAPSHOT_SVG;

			default:
				// view type not classified... try svg.
		}
		return SNAPSHOT_SVG;
	}

	protected void drawNodes(Graphics2D g) {
		// drawNodes(g, canvas, network, forceRepaint);
		drawNodes(g, canvas, forceRepaint);
		forceRepaint = false;
	}

	public void drawNodes(Graphics2D g, Rectangle canvasRect, boolean force) {
		int row;
		int side;
		int dw;
		int dw2;
		int dh;
		int dh3;
		int xshift;
		int yshift;
		int width = canvasRect.width;
		int height = canvasRect.height;
		int nNodes = geometry.getSize();
		double scale;
		Rectangle bounds = new Rectangle();
		AffineTransform at;
		Ellipse2D circle;

		GeometryType type = geometry.getType();
		if (isHierarchy)
			type = ((HierarchicalGeometry) geometry).getSubType();
		// this is a hack but it does the trick when hot swapping models...
		if (width < 0)
			width = frame.getWidth();
		if (height < 0)
			height = frame.getHeight();

		// geometries that have special/fixed layout
		switch (type) {
			case CUBE: // should not get here...
				break;

			case TRIANGULAR:
				side = (int) (Math.sqrt(nNodes) + 0.5);
				dw2 = width / (side + 1);
				bounds.width = dw2 * (side + 1);
				dw = 2 * dw2;
				dh = height / side;
				if (dw < 1 || dh < 1) {
					// too small
					setMessage("Population size too large!");
					return;
				}
				// note: bounds and frame not properly synchronized
				bounds.height = dh * side;
				bounds.x = canvasRect.x + (width - bounds.width) / 2;
				bounds.y = canvasRect.y + (height - bounds.height) / 2;

				Polygon triup = new Polygon(new int[] { 0, dw, dw2 }, new int[] { 0, 0, -dh }, 3);
				Polygon tridown = new Polygon(new int[] { 0, dw, dw2 }, new int[] { 0, 0, dh }, 3);
				triup.translate(bounds.x, bounds.y + dh);
				tridown.translate(bounds.x + dw2, bounds.y);
				int s2 = side / 2;

				for (int h = 0; h < side; h += 2) {
					row = h * side;
					for (int w = 0; w < side; w += 2) {
						g.setPaint(colors[row + w]);
						g.fill(triup);
						triup.translate(dw, 0);
						g.setPaint(colors[row + w + 1]);
						g.fill(tridown);
						tridown.translate(dw, 0);
					}
					triup.translate(-s2 * dw + dw2, dh);
					tridown.translate(-s2 * dw - dw2, dh);
					row += side;
					for (int w = 0; w < side; w += 2) {
						g.setPaint(colors[row + w]);
						g.fill(tridown);
						tridown.translate(dw, 0);
						g.setPaint(colors[row + w + 1]);
						g.fill(triup);
						triup.translate(dw, 0);
					}
					triup.translate(-s2 * dw - dw2, dh);
					tridown.translate(-s2 * dw + dw2, dh);
				}
				triup.translate(0, -side * dh);
				tridown.translate(0, -side * dh);
				break;

			case HEXAGONAL:
				side = (int) (Math.sqrt(nNodes) + 0.5);
				dw2 = width / (2 * side + 1);
				bounds.width = dw2 * (2 * side + 1);
				dw = 2 * dw2;
				dh3 = height / (3 * side + 1);
				if (dw < 1 || dh3 < 1) {
					// too small
					setMessage("Population size too large!");
					return;
				}
				bounds.height = dh3 * (3 * side + 1);
				bounds.x = canvasRect.x + (width - bounds.width) / 2;
				bounds.y = canvasRect.y + (height - bounds.height) / 2;
				dh = 3 * dh3;

				Polygon hex = new Polygon(new int[] { 0, -dw2, -dw2, 0, dw2, dw2 },
						new int[] { 0, dh3, dh, dh + dh3, dh, dh3 }, 6);
				hex.translate(bounds.x + dw2, bounds.y);

				for (int h = 0; h < side; h++) {
					row = h * side;
					for (int w = 0; w < side; w++) {
						g.setPaint(colors[row + w]);
						g.fill(hex);
						hex.translate(dw, 0);
					}
					int sign = -2 * (h % 2) + 1;
					hex.translate(-side * dw + sign * dw2, dh);
				}
				if (side % 2 == 0)
					hex.translate(0, -dh * side);
				else
					hex.translate(-dw2, -dh * side);
				break;

			case SQUARE_NEUMANN:
			case SQUARE_NEUMANN_2ND:
			case SQUARE_MOORE:
			case SQUARE:
				side = (int) (Math.sqrt(nNodes) + 0.5);
				// keep sites square
				dw = Math.min((width - hierarchyGap) / side, (height - hierarchyGap) / side);
				if (dw < 1) {
					// too small
					setMessage("Population size too large!");
					return;
				}
				dh = dw;
				bounds.width = dw * side + hierarchyGap;
				bounds.height = dh * side + hierarchyGap;
				bounds.x = canvasRect.x + (width - bounds.width) / 2;
				bounds.y = canvasRect.y + (height - bounds.height) / 2;

				yshift = bounds.y + bounds.height - dh;
				row = 0;
				for (int h = 0; h < side; h++) {
					if (isHierarchy && h > 0) {
						for (int i = 0; i < hLevels; i++) {
							if (h % hPeriods[i] != 0)
								break;
							yshift -= HIERARCHY_GAP;
						}
					}
					xshift = bounds.x;
					for (int w = 0; w < side; w++) {
						if (isHierarchy && w > 0) {
							for (int i = 0; i < hLevels; i++) {
								if (w % hPeriods[i] != 0)
									break;
								xshift += HIERARCHY_GAP;
							}
						}
						g.setPaint(colors[row + w]);
						g.fill(new Rectangle(xshift, yshift, dw, dh));
						xshift += dw;
					}
					yshift -= dh;
					row += side;
				}
				break;

			case LINEAR:
				dw = width / nNodes;
				if (dw < 1) {
					// too small
					setMessage("Population size too large!");
					return;
				}
				dh = dw;
				// hasHistory: this plots on canvas
				Composite aComposite = g.getComposite();
				// advance only if no repaint is requested
				if (!force) {
					g.setComposite(AlphaComposite.Src);
					g.copyArea(0, 0, canvasRect.width, canvasRect.height - dh, 0, dh);
				}
				g.setComposite(AlphaComposite.Clear);
				g.fill(new Rectangle(0, 0, canvasRect.width, dh));
				g.setComposite(aComposite);

				// draw distribution
				xshift = 0;
				for (int n = 0; n < nNodes; n++) {
					g.setPaint(colors[n]);
					g.fill(new Rectangle(xshift, 0, dw, dh));
					xshift += dw;
				}
				break;

			// case WHEEL:
			// case STAR:
			// case WELLMIXED:
			// case COMPLETE:
			// case PETALS:
			// case DYNAMIC:
			default:
				// draw nodes
				at = g.getTransform();
				g.translate(canvasRect.x, canvasRect.y);
				// scale universe
				double r = network.getRadius();
				scale = canvasRect.width / (r + r); // same as height in this case
				g.scale(scale, scale);
				g.translate(r, r);
				circle = new Ellipse2D.Double();
				Node2D[] nodes = network.toArray();
				Color current = colors[0];
				g.setPaint(current);
				for (int k = 0; k < nNodes; k++) {
					Color next = colors[k];
					// potential optimization of drawing
					if (next != current) {
						g.setPaint(next);
						current = next;
					}
					Node2D node = nodes[k];
					double rk = node.getR();
					double rr2 = rk + rk;
					circle.setFrame(node.getX() - rk, node.getY() - rk, rr2, rr2);
					g.fill(circle);
				}
				g.setTransform(at);
				break;
		}
	}

	protected void drawLinks(Graphics2D g) {
		// g.setStroke(style.lineStroke);
		// drawLinks(g, canvas, network, geometry);
		drawLinks(g, canvas);
	}

	private static void drawLinks(Graphics2D g, Rectangle canvas, Path2D links, double radius) {
		AffineTransform at = g.getTransform();
		g.translate(canvas.x, canvas.y);
		// scale universe
		double scale = canvas.width / (radius + radius); // same as height in this case
		g.scale(scale, scale);
		g.translate(radius, radius);
		g.setPaint(Color.BLACK);
		// note: drawLine accepts only integers, which is highly suspicious for a scaled
		// Graphics2D
		// let's convert Path2D into java.awt.geom.Path2D
		java.awt.geom.Path2D jlinks = new java.awt.geom.Path2D.Double();
		Path2D.Iterator i = links.getPathIterator();
		double[] point = new double[2];
		while (!i.isDone()) {
			switch (i.currentSegment(point)) {
				case PathIterator.SEG_MOVETO:
					jlinks.moveTo(point[0], point[1]);
					break;
				case PathIterator.SEG_LINETO:
					jlinks.lineTo(point[0], point[1]);
					break;
				case PathIterator.SEG_CLOSE:
					jlinks.closePath();
					break;
				default:
			}
			i.next();
		}
		g.draw(jlinks);
		// end path drawing
		g.setTransform(at);
	}

	// drawLinks is static to simplify taking snapshots programmatically
	public void drawLinks(Graphics2D g, Rectangle canvasRect) {
		if (network.getNLinks() > 0) {
			g.setStroke(style.lineStroke);
			drawLinks(g, canvasRect, network.getLinks(), network.getRadius());
		}
	}

	@Override
	public int findNodeAt(Point loc) {
		// <jf
		if (!canvas.contains(loc) || frame.msg != null)
			return FINDNODEAT_OUT_OF_BOUNDS;
		// jf>
		return findNodeAt(loc.x, loc.y, canvas);
	}

	protected int findNodeAt(int x, int y, Rectangle canvasRect) {
		int c, r;
		double iscale;
		Point2D mousecoord;
		double rr;
		int side, dw, dw2, dh, dh3;
		int width = canvasRect.width;
		int height = canvasRect.height;
		int nNodes = geometry.getSize();
		x -= canvasRect.x;
		y -= canvasRect.y;

		switch (geometry.getType()) {
			case TRIANGULAR:
				side = (int) (Math.sqrt(nNodes) + 0.5);
				dw2 = width / (side + 1);
				dh = height / side;

				r = (y - 1) / dh;
				c = (x - 1) / dw2;
				int rx = y - 1 - r * dh;
				int cx = x - 1 - c * dw2;
				if (c % 2 + r % 2 == 1)
					rx = dh - rx;
				double loc = (double) rx / (double) dh + (double) cx / (double) dw2;
				if ((c == 0 && loc < 1.0) || (c == side && loc > 1.0))
					return -1;
				if (loc < 1.0)
					c--;
				return r * side + c;

			case HEXAGONAL:
				side = (int) (Math.sqrt(nNodes) + 0.5);
				dw2 = width / (2 * side + 1);
				dw = 2 * dw2;
				dh3 = height / (3 * side + 1);
				dh = 3 * dh3;

				r = (y - 1) / dh;
				int odd = r % 2;
				c = (x - 1 - odd * dw2) / dw;
				rx = y - 1 - r * dh;
				cx = x - 1 - odd * dw2 - c * dw;
				if (cx < 0) {
					cx += dw; // make sure cx>0
					c--;
				}
				if (rx < dh3) {
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

			case LINEAR:
				dw = width / nNodes;
				dh = dw;

				c = (x - 2) / dw;
				r = (y - 1) / dh;
				return r * nNodes + c;

			case SQUARE_NEUMANN:
			case SQUARE_NEUMANN_2ND:
			case SQUARE_MOORE:
			case SQUARE:
				side = (int) (Math.sqrt(nNodes) + 0.5);
				// keep sites square
				dw = Math.min(width / side, height / side);
				dh = dw;

				c = x / dw;
				r = (canvasRect.height - y) / dh;
				return r * side + c;

			// 3D views can deal with this
			case CUBE:
				return FINDNODEAT_UNIMPLEMENTED;

			default:
				rr = network.getRadius();
				iscale = (rr + rr) / canvasRect.width; // same as height in this case
				mousecoord = new Point2D(x * iscale - rr, y * iscale - rr);
				Node2D[] nodes = network.toArray();
				// in the undesirable case that nodes overlap, nodes with a higher index are
				// drawn later (on top)
				// in order to get the top node we need to start from the back
				for (int k = nNodes - 1; k >= 0; k--) {
					Node2D hit = nodes[k];
					double rk = hit.getR();
					if (mousecoord.distance2(hit) <= rk * rk)
						return k;
				}
				return FINDNODEAT_OUT_OF_BOUNDS;
		}
	}
}
