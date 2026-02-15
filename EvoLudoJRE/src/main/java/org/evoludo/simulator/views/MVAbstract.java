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

package org.evoludo.simulator.views;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Properties;
import java.util.logging.Logger;

import javax.imageio.ImageIO;
import javax.swing.BoxLayout;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import org.evoludo.geom.Point2D;
import org.evoludo.graphics.AbstractGraph;
import org.evoludo.graphics.GraphListener;
import org.evoludo.graphics.GraphStyle;
import org.evoludo.graphics.PopGraph2D;
import org.evoludo.simulator.EvoLudo;
import org.evoludo.simulator.EvoLudoJRE;
import org.evoludo.simulator.EvoLudoLab;
import org.evoludo.simulator.models.Model;
import org.evoludo.simulator.models.ModelType;
import org.evoludo.simulator.modules.Module;
import org.evoludo.util.Formatter;
import org.freehep.graphics2d.VectorGraphics;
import org.freehep.graphicsio.pdf.PDFGraphics2D;
import org.freehep.graphicsio.ps.PSGraphics2D;
import org.freehep.graphicsio.svg.SVGGraphics2D;

public abstract class MVAbstract extends JComponent
		implements MultiView, GraphListener, ActionListener {

	private static final long serialVersionUID = 20110423L;

	/**
	 * The module associated with this graph.
	 */
	protected Module<?> module;
	protected EvoLudoLab lab;
	protected EvoLudoJRE engine;

	/**
	 * Logger for keeping track of and reporting events and issues.
	 */
	protected Logger logger;

	@Override
	public EvoLudo getEngine() {
		return engine;
	}

	@Override
	public Logger getLogger() {
		return logger;
	}

	// note: maybe we can do some optimization because the majority of panes will
	// only have one graph, which makes this a bit of an overkill...
	protected ArrayList<AbstractGraph> graphs = new ArrayList<>();

	public ArrayList<AbstractGraph> getGraphs() {
		return graphs;
	}

	public boolean isActive = false;
	protected double timestamp = -1.0;
	protected boolean hasSVG = false;

	// NOTE: subclasses must implement at least one of getData()
	// public abstract boolean getData(double data[], int tag);
	// public abstract double[] getData(int tag);

	public MVAbstract(EvoLudoLab lab) {
		this.lab = lab;
		engine = lab.getEngine();
		logger = engine.getLogger();

		// check if freehep graphics libraries are available
		try {
			@SuppressWarnings("unused")
			Class<?> cls = Class.forName("org.freehep.graphics2d.VectorGraphics");
			hasSVG = true;
		} catch (ClassNotFoundException e) {
			// ignore exception - freehep graphics libraries seem missing
			engine.getLogger().warning("freehep library not found - disabling vector graphics export.");
		}
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
	}

	@Override
	public boolean hasSVG() {
		return hasSVG;
	}

	// a chance to customize and touch up the graphics
	@Override
	public void polish(Graphics2D plot, AbstractGraph graph) {
	}

	/*
	 * implement GraphListener - override as appropriate
	 */
	// take care of style changes
	// note: fonts and antialiasing are not set when instantiating the classes -
	// delete/rewrite and do it here!
	// if called from reset then this is ok for antialiasing but not for fonts -
	// screws zooming up!
	@Override
	public void initStyle(GraphStyle style, AbstractGraph owner) {
		// do not set fonts directly because this could screw up zoomed views!
		style.setLabelFont(lab.getLabelFont());
		style.setTickFont(lab.getTickFont());
		style.setLineStroke(lab.getLineStroke());
		style.setFrameStroke(lab.getFrameStroke());
	}

	// note: custom menu stuff should be part of views not of graphs...
	// graph listeners should do the initialization and updating!
	private JCheckBoxMenuItem localMenu, timeMenu;
	private JMenuItem nodeMenu;
	private JPopupMenu.Separator customSeparator = new JPopupMenu.Separator();

	protected static int localNode = -1;

	private boolean menuShowLocal = false;
	private boolean menuSetLocal = false;
	private boolean menuTime = false;

	// <jf
	protected static final String CUSTOM_MENU_TOGGLE_DEBUG = "TDebug";

	protected static final String CUSTOM_MENU_DEBUG_UPDATE_AT = "DebugAt";
	// jf>

	@Override
	public void actionPerformed(ActionEvent e) {
	}

	@Override
	public void initCustomMenu(JPopupMenu menu, AbstractGraph owner) {
	}

	// protected void initCMShowLocalDynamics(JPopupMenu menu, ActionListener
	// handler) {
	// menu.add(customSeparator);
	// localMenu = new JCheckBoxMenuItem("Local dynamics @ -", false);
	// localMenu.setActionCommand(AbstractGraph.CUSTOM_MENU_TOGGLE_LOCAL);
	// localMenu.addActionListener(handler);
	// localMenu.setFont(menu.getFont());
	// menu.add(localMenu);
	// menuShowLocal = true;
	// }

	// protected void initCMSetLocalDynamics(JPopupMenu menu, ActionListener
	// handler) {
	// menu.add(customSeparator);
	// nodeMenu = new JMenuItem("Set location @ -");
	// nodeMenu.setActionCommand(AbstractGraph.CUSTOM_MENU_SET_LOCAL_NODE);
	// nodeMenu.addActionListener(handler);
	// nodeMenu.setFont(menu.getFont());
	// menu.add(nodeMenu);
	// menuSetLocal = true;
	// }

	protected void initCMTimeReversed(JPopupMenu menu, ActionListener handler) {
		if (!engine.getModel().permitsTimeReversal())
			return;
		menu.add(customSeparator);
		timeMenu = new JCheckBoxMenuItem("Time reversed", false);
		timeMenu.setActionCommand(AbstractGraph.CUSTOM_MENU_TOGGLE_TIME);
		timeMenu.addActionListener(handler);
		timeMenu.setFont(menu.getFont());
		menu.add(timeMenu);
		menuTime = true;
	}

	// note: for now only deal with local dynamics menu
	@Override
	public void resetCustomMenu(JPopupMenu menu, AbstractGraph owner) {
		if (!menuShowLocal && !menuSetLocal && !menuTime)
			return;

		int idx;
		switch (engine.getModel().getType()) {
			case PDE:
				hideAllCM(menu);
				if (menuShowLocal) {
					idx = menu.getComponentIndex(localMenu);
					if (idx >= 0)
						menu.getComponent(idx).setVisible(true);
				}
				if (menuSetLocal) {
					idx = menu.getComponentIndex(nodeMenu);
					if (idx >= 0)
						menu.getComponent(idx).setVisible(true);
				}
				// note: does this always hide the right separator? can we group menu items, to
				// enable/disable them all at once?
				if (menuShowLocal || menuSetLocal) {
					idx = menu.getComponentIndex(customSeparator);
					if (idx >= 0)
						menu.getComponent(idx).setVisible(true);
				}
				break;
			case ODE:
				hideAllCM(menu);
				// ODE's allow to reverse the time. this is impossible in stochastic or
				// diffusive systems
				// because information is lost as time progresses.
				if (menuTime) {
					idx = menu.getComponentIndex(timeMenu);
					if (idx >= 0)
						menu.getComponent(idx).setVisible(true);
				}
				// note: does this always hide the right separator? can we group menu items, to
				// enable/disable them all at once?
				if (menuTime) {
					idx = menu.getComponentIndex(customSeparator);
					if (idx >= 0)
						menu.getComponent(idx).setVisible(true);
				}
				break;
			case IBS:
			default:
				hideAllCM(menu);
		}
	}

	private void hideAllCM(JPopupMenu menu) {
		int idx;
		if (menuShowLocal) {
			idx = menu.getComponentIndex(localMenu);
			if (idx >= 0)
				menu.getComponent(idx).setVisible(false);
		}
		if (menuSetLocal) {
			idx = menu.getComponentIndex(nodeMenu);
			if (idx >= 0)
				menu.getComponent(idx).setVisible(false);
		}
		if (menuTime) {
			idx = menu.getComponentIndex(timeMenu);
			if (idx >= 0)
				menu.getComponent(idx).setVisible(false);
		}
		// note: does this always hide the right separator? can we group menu items, to
		// enable/disable them all at once?
		idx = menu.getComponentIndex(customSeparator);
		if (idx >= 0)
			menu.getComponent(idx).setVisible(false);
	}

	@Override
	public void showCustomMenu(JPopupMenu menu, Point loc, AbstractGraph owner) {
		Model model = engine.getModel();
		ModelType mt = model.getType();
		if (menuTime) {
			timeMenu.setEnabled(mt.isODE());
			timeMenu.setSelected(model.isTimeReversed());
		}
		if (mt.isPDE()) {
			if (menuSetLocal) {
				int node = ((PopGraph2D) owner).findNodeAt(loc);
				if (node < 0) {
					nodeMenu.setEnabled(false);
				} else {
					nodeMenu.setText("Set location @ " + node + " (now @ " + localNode + ")");
					nodeMenu.setEnabled(true);
				}
			}
			if (menuShowLocal) {
				localMenu.setText("Local dynamics @ " + localNode);
			}
		}
	}

	@Override
	public void setLocalNode(int localNode) {
		MVAbstract.localNode = localNode;
	}

	@Override
	public int getLocalNode() {
		return localNode;
	}

	@Override
	public void setTimeReversed(boolean reversed) {
		engine.getModel().setTimeReversed(reversed);
	}

	@Override
	public int getNData(int tag) {
		return module.getNTraits();
	}

	@Override
	public Color[] getColors(int tag) {
		return module.getTraitColors();
	}

	public Color[] getActiveColors(int tag) {
		Color[] all = getColors(tag);
		boolean[] active = module.getActiveTraits();
		int nActive = active.length;
		if (nActive == all.length)
			return all;
		Color[] some = new Color[nActive];
		int n = 0;
		for (int i = 0; i < nActive; i++) {
			if (!active[i])
				continue;
			some[n++] = all[i];
		}
		return some;
	}

	@Override
	public Color getColor(int tag) {
		return module.getTraitColors()[tag];
	}

	@Override
	public int getActiveCount(int tag) {
		return module.getNActive();
	}

	@Override
	public boolean[] getActives(int tag) {
		boolean[] active = module.getActiveTraits();
		if (active == null) {
			active = new boolean[module.getNTraits()];
			Arrays.fill(active, true);
		}
		return active;
	}

	@Override
	public String[] getNames(int tag) {
		return module.getTraitNames();
	}

	@Override
	public String getName(int tag) {
		return module.getTraitName(tag);
	}

	// NOTE: at least one getData variant must be overridden in subclass
	@Override
	public boolean getData(double data[], int tag) {
		return false;
	}

	@Override
	public double[] getData(int tag) {
		return null;
	}

	public void setState(double[] state) {
		// note: need to decide how to set the state... setState(Point2D) does not work
		// for S3Graphs...
	}

	public String getToolTipText(Point2D loc, int tag) {
		return null;
	}

	@Override
	public boolean isRunning() {
		return lab.isRunning();
	}

	protected File openSnapshot(String ext) {
		String pre = module.getKey() + "-" + getFilePrefix() + "-t"
				+ Formatter.format(engine.getModel().getUpdates(), 2);
		File snapfile = new File(pre + "." + ext);
		int counter = 0;
		while (snapfile.exists())
			snapfile = new File(pre + "-" + (counter++) + "." + ext);
		return snapfile;
	}

	/**
	 * forward export request to engine
	 */
	@Override
	public void exportState() {
		engine.exportState(null);
	}

	private ExportVectorGraphics evg = null;

	// note: should ask all graphs about their preferred format - if they don't
	// agree, default to PNG - at least if !inViewport
	@Override
	public void saveSnapshot(int tag, boolean inViewport, int format) {
		switch (format) {
			case AbstractGraph.SNAPSHOT_NONE:
				return; // do not waste our time
			case AbstractGraph.SNAPSHOT_EPS:
			case AbstractGraph.SNAPSHOT_PDF:
			case AbstractGraph.SNAPSHOT_SVG:
				if (hasSVG()) {
					if (evg == null)
						evg = new ExportVectorGraphics();
				} else {
					format = AbstractGraph.SNAPSHOT_PNG;
					evg = null;
				}
				break;
			case AbstractGraph.SNAPSHOT_PNG:
				break; // now we are talking
			default:
				logger.warning("unknown file format (" + format + ")");
				return;
		}

		switch (format) {
			case AbstractGraph.SNAPSHOT_PNG:
				BufferedImage snapimage;
				if (inViewport) {
					// JScrollView found - write only current graph
					AbstractGraph snapgraph = graphs.get(tag);
					Dimension size = snapgraph.getSize();
					snapimage = new BufferedImage(size.width, size.height, BufferedImage.TYPE_INT_ARGB);
					Graphics2D g2 = snapimage.createGraphics();
					// use paint because the target is PNG
					snapgraph.paintAll(g2);
					g2.dispose();
				} else {
					Dimension size = getSize();
					snapimage = new BufferedImage(size.width, size.height, BufferedImage.TYPE_INT_ARGB);
					Graphics2D g2 = snapimage.createGraphics();
					for (AbstractGraph snapgraph : graphs) {
						Point loc = snapgraph.getLocation();
						g2.translate(loc.x, loc.y);
						// use paint because the target is PNG
						snapgraph.paintAll(g2);
						g2.translate(-loc.x, -loc.y);
					}
					g2.dispose();
				}
				File snapfile = openSnapshot("png");
				try {
					ImageIO.write(snapimage, "PNG", snapfile);
				} catch (IOException x) {
					logger.warning("writing image to " + snapfile.getAbsolutePath() + " failed!");
					return;
				}
				logger.info("image written to " + snapfile.getAbsolutePath());
				return;

			case AbstractGraph.SNAPSHOT_SVG:
				evg.exportSVG(tag, inViewport);
				return;

			case AbstractGraph.SNAPSHOT_PDF:
				evg.exportPDF(tag, inViewport);
				return;

			case AbstractGraph.SNAPSHOT_EPS:
				evg.exportEPS(tag, inViewport);
				return;

			default:
				logger.warning("unknown file format (" + format + ")");
		}
	}

	/*
	 * for some reason the vector graphics routines need to be 'shielded' from the
	 * rest of the
	 * code because otherwise runtime exceptions concerning missing classes will be
	 * thrown
	 * if the freehep libraries are missing - somehow instantiation of MV classes
	 * fails...
	 * this seems error prone and rewritten in robust manner!
	 */
	public class ExportVectorGraphics {

		void exportSVG(int tag, boolean inViewport) {
			File snapfile = openSnapshot("svg");
			Properties props = new Properties();
			props.setProperty(SVGGraphics2D.TITLE, "Generated by EvoLudo \u00a9 Christoph Hauert");
			if (inViewport) {
				// JScrollView found - write only current graph
				AbstractGraph snapgraph = graphs.get(tag);
				try {
					VectorGraphics vg2 = new SVGGraphics2D(snapfile, snapgraph.getSize());
					vg2.setProperties(props);
					vg2.startExport();
					// use print for high resolution output of PDF
					snapgraph.printAll(vg2);
					vg2.endExport();
					vg2.dispose();
				} catch (IOException x) {
					logger.warning("writing image to " + snapfile.getAbsolutePath() + " failed!");
					return;
				}
				logger.info("image written to " + snapfile.getAbsolutePath());
				return;
			}
			try {
				VectorGraphics vg2 = new SVGGraphics2D(snapfile, getSize());
				vg2.setProperties(props);
				vg2.startExport();
				for (AbstractGraph snapgraph : graphs) {
					Point loc = snapgraph.getLocation();
					vg2.translate(loc.x, loc.y);
					// use print for high resolution output of PDF
					snapgraph.printAll(vg2);
					vg2.translate(-loc.x, -loc.y);
				}
				vg2.endExport();
				vg2.dispose();
			} catch (IOException x) {
				logger.warning("writing SVG graphics to " + snapfile.getAbsolutePath() + " failed!");
				return;
			}
			logger.info("SVG graphics written to " + snapfile.getAbsolutePath());
		}

		void exportPDF(int tag, boolean inViewport) {
			File snapfile = openSnapshot("pdf");
			Properties props = new Properties();
			// NOTE: pdf is not fully implemented - failed to set custom size, background
			// white
			// props.setProperty(PDFGraphics2D.PAGE_SIZE, "600, 480");
			props.setProperty(PDFGraphics2D.AUTHOR, "EvoLudoLabs \u00a9 Christoph Hauert");
			props.setProperty(PDFGraphics2D.PAGE_MARGINS, "0, 0, 0, 0");
			props.setProperty(PDFGraphics2D.TRANSPARENT, "true");
			if (inViewport) {
				// JScrollView found - write only current graph
				AbstractGraph snapgraph = graphs.get(tag);
				try {
					VectorGraphics vg2 = new PDFGraphics2D(snapfile, snapgraph.getSize());
					vg2.setProperties(props);
					vg2.startExport();
					// use print for high resolution output of PDF
					snapgraph.printAll(vg2);
					vg2.endExport();
					vg2.dispose();
				} catch (IOException x) {
					logger.warning("writing PDF graphics to " + snapfile.getAbsolutePath() + " failed!");
					return;
				}
				logger.info("PDF graphics written to " + snapfile.getAbsolutePath());
				return;
			}
			try {
				VectorGraphics vg2 = new PDFGraphics2D(snapfile, getSize());
				vg2.setProperties(props);
				vg2.startExport();
				for (AbstractGraph snapgraph : graphs) {
					Point loc = snapgraph.getLocation();
					vg2.translate(loc.x, loc.y);
					// use print for high resolution output of PDF
					snapgraph.printAll(vg2);
					vg2.translate(-loc.x, -loc.y);
				}
				vg2.endExport();
				vg2.dispose();
			} catch (IOException x) {
				logger.warning("writing PDF graphics to " + snapfile.getAbsolutePath() + " failed!");
				return;
			}
			logger.info("PDF graphics written to " + snapfile.getAbsolutePath());
		}

		void exportEPS(int tag, boolean inViewport) {
			File snapfile = openSnapshot("eps");
			Properties props = new Properties();
			// NOTE: eps is not fully implemented - transparent background failed
			// props.setProperty(PSGraphics2D.CUSTOM_PAGE_SIZE, "300, 240");
			// props.setProperty(PSGraphics2D.AUTHOR, "EvoLudoLabs \u00a9 Christoph
			// Hauert");
			// props.setProperty(PSGraphics2D.TRANSPARENT, "true");
			if (inViewport) {
				// JScrollView found - write only current graph
				AbstractGraph snapgraph = graphs.get(tag);
				try {
					VectorGraphics vg2 = new PSGraphics2D(snapfile, snapgraph.getSize());
					vg2.setProperties(props);
					vg2.startExport();
					// use print for high resolution output of PDF
					snapgraph.printAll(vg2);
					vg2.endExport();
					vg2.dispose();
				} catch (IOException x) {
					logger.warning("writing EPS graphics to " + snapfile.getAbsolutePath() + " failed!");
					return;
				}
				logger.info("EPS graphics written to " + snapfile.getAbsolutePath());
				return;
			}
			try {
				VectorGraphics vg2 = new PSGraphics2D(snapfile, getSize());
				vg2.setProperties(props);
				vg2.startExport();
				for (AbstractGraph snapgraph : graphs) {
					Point loc = snapgraph.getLocation();
					vg2.translate(loc.x, loc.y);
					// use print for high resolution output of PDF
					snapgraph.printAll(vg2);
					vg2.translate(-loc.x, -loc.y);
				}
				vg2.endExport();
				vg2.dispose();
			} catch (IOException x) {
				logger.warning("writing EPS graphics to " + snapfile.getAbsolutePath() + " failed!");
				return;
			}
			logger.info("EPS graphics written to " + snapfile.getAbsolutePath());
		}
	}

	protected String getFilePrefix() {
		return "snap";
	}

	/*
	 * implement MultiViewPanel (partially)
	 */

	@Override
	public boolean isActive() {
		return isActive;
	}

	@Override
	public void activate() {
		if (isActive)
			return; // already active
		isActive = true;
		viewWillGetActive();
	}

	@Override
	public void deactivate() {
		if (!isActive)
			return; // already deactive
		isActive = false;
		for (AbstractGraph graph : graphs)
			graph.deactivate();
	}

	protected void viewWillGetActive() {
		for (AbstractGraph graph : graphs)
			graph.activate();
		repaint();
	}

	// @Override
	// public void setPopulation(Population population) {
	// this.population = population;
	// }

	@Override
	public void setModule(Module<?> module) {
		this.module = module;
	}

	@Override
	public void reset(boolean clear) {
		timestamp = -1.0;
		for (AbstractGraph graph : graphs)
			graph.reset(clear);
		int nPop = module.getNPopulation();
		if (localNode < 0 || localNode >= nPop)
			localNode = nPop / 2;
		// revalidate layout as things may have changed after changing parameters
		validate();
	}

	@Override
	public void init() {
		timestamp = -1.0;
		for (AbstractGraph graph : graphs)
			graph.reinit();
	}

	@Override
	public void update(boolean updateGUI) {
		for (AbstractGraph graph : graphs)
			graph.next(isActive, updateGUI);
	}

	@Override
	public void end() {
		for (AbstractGraph graph : graphs)
			graph.end();
	}

	@Override
	public void parametersChanged(boolean didReset) {
		if (!didReset)
			init();
	}

	// this is an annoying workaround to close the context menu when the java
	// applet/application becomes deactivated
	@Override
	public void setContextMenuEnabled(boolean enabled) {
		for (AbstractGraph graph : graphs)
			graph.setContextMenuEnabled(enabled);
	}
}
