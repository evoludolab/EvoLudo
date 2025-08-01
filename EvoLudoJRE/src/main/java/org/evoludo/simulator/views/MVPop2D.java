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

package org.evoludo.simulator.views;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.Iterator;
import java.util.Properties;

import javax.imageio.ImageIO;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;

import org.evoludo.graphics.AbstractGraph;
import org.evoludo.graphics.GraphAxis;
import org.evoludo.graphics.GraphStyle;
import org.evoludo.graphics.PopGraph2D;
import org.evoludo.graphics.PopListener;
import org.evoludo.simulator.ColorMap;
import org.evoludo.simulator.ColorMapJRE;
import org.evoludo.simulator.EvoLudo;
import org.evoludo.simulator.EvoLudoLab;
import org.evoludo.simulator.Geometry;
import org.evoludo.simulator.Network2D;
import org.evoludo.simulator.models.IBS;
import org.evoludo.simulator.models.Model;
import org.evoludo.simulator.models.Model.HasDE;
import org.evoludo.simulator.models.PDE;
import org.evoludo.simulator.models.Type;
import org.evoludo.simulator.modules.Map2Fitness;
import org.evoludo.simulator.modules.Module;
import org.evoludo.util.Formatter;
import org.freehep.graphics2d.VectorGraphics;
import org.freehep.graphicsio.svg.SVGGraphics2D;

public class MVPop2D extends MVAbstract implements PopListener {

	private static final long serialVersionUID = 20110423L;

	public static enum Data {
		VOID,
		DSTRAT,
		CSTRAT,
		FITNESS,
		TAG
	}

	public Data type = Data.VOID;
	protected ColorMap<Color> colorMap;

	// <jf
	private final JCheckBoxMenuItem toggleDebugCheckBox = new JCheckBoxMenuItem("Enable debugging", false);
	private final JMenuItem debugUpdateAtItem = new JMenuItem("Update @ here");

	protected int CMNode; // the graph node indicated by the mouse when showCustomMenu
	// jf>

	public MVPop2D(EvoLudoLab lab, Data type) {
		super(lab);
		this.type = type;
		setOpaque(false);
	}

	protected String DIM = "";

	@Override
	public String getName() {
		switch (type) {
			case DSTRAT:
				return "Strategy " + DIM + "- Structure";
			case CSTRAT:
				return "Trait " + DIM + "- Structure";
			case FITNESS:
				return "Fitness " + DIM + "- Structure";
			case TAG:
				return "Tags " + DIM + "- Structure";
			default:
				return "Unknown...";
		}
	}

	@Override
	public void reset(boolean clear) {
		// at one point (likely in the distant past) this view lost its ability to
		// display multiple graphs...
		double rInter = engine.getModel().getTimeStep();
		if (graphs.size() != 1) {
			graphs.clear();
			removeAll();
			PopGraph2D graph = new PopGraph2D(this, module.getGeometry(), module);
			GraphAxis axis = graph.getXAxis();
			axis.enabled = false;
			axis = graph.getYAxis();
			axis.enabled = false;
			axis.label = "time";
			axis.showLabel = true;
			axis.max = 0.0;
			axis.min = 10.0; // this is just to achieve better layout
			axis.step = -rInter;
			axis.majorTicks = 4;
			axis.minorTicks = 1;
			axis.formatter = new DecimalFormat("0.#");
			axis.grid = 0;
			JScrollPane scroll = new JScrollPane(graph, ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER,
					ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
			scroll.getViewport().setOpaque(false);
			scroll.setOpaque(false);
			scroll.setBorder(null);
			add(scroll);
			graphs.add(graph);
		}
		for (Iterator<AbstractGraph> i = graphs.iterator(); i.hasNext();) {
			GraphAxis axis = i.next().getYAxis();
			axis.max = 0.0;
			axis.step = -rInter;
			// somehow we should mark that axis.min is invalid...
		}
		super.reset(clear);
	}

	@Override
	public boolean verifyYAxis(GraphAxis y, int tag) {
		boolean axisShown = y.enabled;
		y.enabled = graphs.get(tag).hasHistory();
		return (axisShown != y.enabled);
	}

	@Override
	public void initColor(int tag) {
		Color[] tColors = module.getTraitColors();
		Model model = engine.getModel();
		Type mt = model.getType();
		switch (type) {
			case DSTRAT:
				if (mt.isPDE()) {
					int dep = ((HasDE) module).getDependent();
					switch (module.getNTraits()) {
						case 1:
							colorMap = new ColorMapJRE.Gradient1D(tColors, 500);
							break;
						case 2:
							if (dep >= 0) {
								int trait = (dep + 1) % 2;
								colorMap = new ColorMapJRE.Gradient1D(tColors[dep], tColors[trait], trait, 500);
								break;
							}
							colorMap = new ColorMapJRE.Gradient2D(tColors, 100);
							break;
						case 3:
							if (dep >= 0) {
								colorMap = new ColorMapJRE.Gradient2D(tColors, dep, 100);
								break;
							}
							//$FALL-THROUGH$
						default:
							colorMap = new ColorMapJRE.GradientND(tColors, dep);
							break;
					}
				} else
					colorMap = new ColorMapJRE.Index(tColors);
				break;

			case CSTRAT:
				switch (module.getNTraits()) {
					case 1:
						// set hue range: min = red, max = blue
						colorMap = new ColorMapJRE.Hue(0.0, 2.0 / 3.0, 500);
						break;
					case 2:
						colorMap = new ColorMapJRE.Gradient2D(tColors, 100);
						break;
					case 3: // three traits red, green, blue - calculate on the fly? fewer colors?
					default:
						colorMap = new ColorMapJRE.GradientND(tColors);
						logger.warning("MVPop: display of >2 continuous traits is experimental...");
						break;
				}
				break;

			case FITNESS:
				ColorMap.Gradient1D<Color> cMap1D = new ColorMapJRE.Gradient1D(
						new Color[] { Color.BLACK, Color.GRAY, Color.YELLOW, Color.RED }, 500);
				colorMap = cMap1D;
				// cMap1D.setRange(module.getMinFitness(), module.getMaxFitness());
				cMap1D.setRange(model.getMinScore(tag), model.getMaxScore(tag));
				// DEBUG
				if (mt.isIBS()) {
					Map2Fitness map2fit = module.getMap2Fitness();
					if (model.isContinuous()) {
						// cast is save because pop is Continuous
						org.evoludo.simulator.models.Continuous cmodel = (org.evoludo.simulator.models.Continuous) model;
						// hardcoded colors for min/max mono scores
						cMap1D.setColor(map2fit.map(cmodel.getMinMonoScore(tag)), Color.BLUE.darker());
						cMap1D.setColor(map2fit.map(cmodel.getMaxMonoScore(tag)), Color.BLUE.brighter());
					} else {
						// cast is save because pop is Discrete
						org.evoludo.simulator.models.Discrete dmodel = (org.evoludo.simulator.models.Discrete) model;
						// mark homogeneous fitness values by pale color
						int nMono = module.getNTraits();
						for (int n = 0; n < nMono; n++)
							cMap1D.setColor(map2fit.map(dmodel.getMonoScore(tag, n)),
									new Color(Math.max(tColors[n].getRed(), 127),
											Math.max(tColors[n].getGreen(), 127),
											Math.max(tColors[n].getBlue(), 127)));
					}
				}
				break;

			case TAG:
				colorMap = new ColorMapJRE.Hue(500);
				break;

			default:
				break;
		}
	}

	@Override
	public void initStyle(GraphStyle style, AbstractGraph owner) {
		// this method is called when creating snapshots for simulations but
		// simulations have no GUI (i.e. lab==null)
		if (lab == null) {
			// opportunity to set default styles for snapshots
			return;
		}
		super.initStyle(style, owner);
		((PopGraph2D) owner).setAnimateLayout(lab.doAnimateLayout());
	}

	// implement GraphListener - mostly done in MVAbstract
	@Override
	public void initCustomMenu(JPopupMenu menu, AbstractGraph owner) {
		super.initCustomMenu(menu, owner);
		// initCMSetLocalDynamics(menu, owner);
		// <jf
		// check if findNodeAt is implemented by passing the most unlikely coordinates
		if (owner.findNodeAt(
				new Point(-Integer.MAX_VALUE, -Integer.MAX_VALUE)) != AbstractGraph.FINDNODEAT_UNIMPLEMENTED) {
			toggleDebugCheckBox.setActionCommand(CUSTOM_MENU_TOGGLE_DEBUG);
			toggleDebugCheckBox.addActionListener(this);
			toggleDebugCheckBox.setFont(owner.style.menuFont);
			menu.add(toggleDebugCheckBox);

			debugUpdateAtItem.setActionCommand(CUSTOM_MENU_DEBUG_UPDATE_AT);
			debugUpdateAtItem.addActionListener(this);
			debugUpdateAtItem.setFont(owner.style.menuFont);
			debugUpdateAtItem.setVisible(toggleDebugCheckBox.getState());
			menu.add(debugUpdateAtItem);
		}
	}

	@Override
	public void resetCustomMenu(JPopupMenu menu, AbstractGraph owner) {
		super.resetCustomMenu(menu, owner);

		toggleDebugCheckBox.setState(false);
		debugUpdateAtItem.setVisible(toggleDebugCheckBox.getState());
	}

	// NOTE: JRE is getting in disrepair... review/reimplement?
	// @Override
	// public void showCustomMenu(JPopupMenu menu, Point loc, AbstractGraph owner) {
	// CMNode = owner.findNodeAt(loc);
	// if (CMNode>=0){
	// debugUpdateAtItem.setText("Update node @ "+CMNode);
	// debugUpdateAtItem.setEnabled(true);
	// }
	// else{
	// debugUpdateAtItem.setText("Update node @ -");
	// debugUpdateAtItem.setEnabled(false);
	// }
	// super.showCustomMenu(menu,loc,owner);
	// }

	// @Override
	// public void actionPerformed(ActionEvent e) {
	// String cmd = e.getActionCommand();
	// if( cmd.equals(CUSTOM_MENU_TOGGLE_DEBUG) ) {
	// debugUpdateAtItem.setVisible(toggleDebugCheckBox.getState());
	// return;
	// }
	// if( cmd.equals(CUSTOM_MENU_DEBUG_UPDATE_AT) ) {
	// if (CMNode >= 0) debugUpdatePopulationAt(CMNode);
	// return;
	// }
	// super.actionPerformed(e);
	// }

	// private void debugUpdatePopulationAt(int node) {
	// population.debugUpdatePopulationAt(node);
	// module.debugUpdatePopulationAt(node);
	// }
	// jf>

	@Override
	public double getData(Color[] data, int tag) {
		Model model = engine.getModel();
		switch (type) {
			case DSTRAT:
			case CSTRAT:
				model.getTraitData(tag, data, colorMap);
				break;
			case FITNESS:
				// cast should be safe for fitness data
				model.getFitnessData(tag, data, (ColorMap.Gradient1D<Color>) colorMap);
				break;
			// case TAG:
			// population.getTagData(data, colorMap);
			// break;
			default:
				break;
		}
		return model.getUpdates();
	}

	@Override
	public String getInfoAt(Network2D network, int node, int tag) {
		if (node < 0)
			return null;
		int nNodes = network.getGeometry().size;

		// String toolTip, struct = "";
		String toolTip;
		Model model = engine.getModel();
		switch (model.getType()) {
			case ODE:
				return null; // no further information available

			case PDE:
				if (node >= nNodes) {
					// this can only happen for Geometry.LINEAR
					double t = (node / nNodes) * engine.getModel().getTimeStep();
					if (network.timestamp < t)
						return null;
					return "<html><i>Node:</i> " + (node % nNodes) + "<br><i>Time:</i> " + Formatter.format(-t, 2);
				}
				String[] s = module.getTraitNames();
				String names = "<br><i>Traits:</i> " + s[0];
				for (int n = 1; n < s.length; n++)
					names += ", " + s[n];
				String density = "<br><i>Densities:&nbsp;</i> " + model.getTraitNameAt(tag, node);
				String fitness = "<br><i>Fitness:&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</i> "
						+ model.getFitnessNameAt(tag, node);
				toolTip = "<html><i>Node:</i> " + node + names + density + fitness;
				Geometry diffusion = ((PDE) model).getGeometry();
				if (diffusion.isUndirected)
					toolTip += "<br><i>Connections:</i> " + formatStructureAt(node, diffusion.out, diffusion.kout);
				else
					toolTip += "<br><i>Links to:</i>  " + formatStructureAt(node, diffusion.out, diffusion.kout) +
							"<br><i>Link here:</i> " + formatStructureAt(node, diffusion.in, diffusion.kin);
				return toolTip;

			case IBS:
				if (node >= nNodes) {
					// this can only happen for Geometry.LINEAR
					double t = (node / nNodes) * engine.getModel().getTimeStep();
					if (network.timestamp < t)
						return null;
					return "<html><i>Node:</i> " + (node % nNodes) + "<br><i>Time:</i> " + Formatter.format(-t, 2);
				}
				IBS ibs = (IBS) model;
				int count = ibs.getInteractionsAt(tag, node);
				String nametag = ibs.getTagNameAt(tag, node);
				toolTip = "<html><i>Node:</i> " + node +
						"<br><i>Strategy:</i> " + model.getTraitNameAt(tag, node) +
						(nametag == null ? "" : "<br><i>Tag:</i> " + nametag) +
						"<br><i>Fitness:</i> " + model.getFitnessNameAt(tag, node) +
						(count < 0 ? ""
								: "<br><i>Interactions:</i> " + (count == Integer.MAX_VALUE ? "all" : "" + count));
				Geometry intergeom = module.getInteractionGeometry();
				if (intergeom.isUndirected)
					toolTip += "<br><i>Neighbors:</i> " + formatStructureAt(node, intergeom.out, intergeom.kout);
				// useful for debugging geometry - Geometry.checkConnections should be able to
				// catch such problems
				// toolTip += "<br>in: "+formatStructureAt(node, data.in, data.kin);
				else
					toolTip += "<br><i>Links to:</i>  " + formatStructureAt(node, intergeom.out, intergeom.kout) +
							"<br><i>Link here:</i> " + formatStructureAt(node, intergeom.in, intergeom.kin);
				if (!intergeom.interCompSame) {
					Geometry compgeom = module.getCompetitionGeometry();
					if (compgeom.isUndirected)
						toolTip += "<br><i>Competitors:</i> " + formatStructureAt(node, compgeom.out, compgeom.kout);
					else
						toolTip += "<br><i>Competes for:</i>  " + formatStructureAt(node, compgeom.out, compgeom.kout) +
								"<br><i>Compete here:</i> " + formatStructureAt(node, compgeom.in, compgeom.kin);
				}
				return toolTip;

			default:
				return null;
		}
	}

	private static String formatStructureAt(int node, int[][] structure, int[] degree) {
		if (structure == null)
			return "well-mixed";
		int k = degree[node];
		int[] links = structure[node];
		String msg;
		switch (k) {
			case 0:
				return "none";

			case 1:
				return "1 [" + links[0] + "]";

			default:
				msg = k + " [" + links[0];
				int disp = Math.min(k, 8);
				for (int n = 1; n < disp; n++)
					msg += " " + links[n];
				if (disp < k)
					msg += " ...";
				msg += "]";
		}
		return msg;
	}

	@Override
	protected String getFilePrefix() {
		return "pop";
	}

	// implement MultiViewPanel - mostly done in MVAbstract

	@Override
	public boolean mouseHitNode(int node, int tag) {
		if (lab.isRunning())
			return false;
		return ((IBS) engine.getModel()).mouseHitNode(tag, node, false); // population signals change back to us
	}

	// @Override
	// public boolean mouseHitNode(int node, int refnode, int tag) {
	// if( lab.isRunning() )
	// return false;
	// return module.mouseHitNode(node, refnode); // population signals change back
	// to us
	// }

	public static BufferedImage getSnapshot(EvoLudo engine, Module module, int width, int height) {
		BufferedImage snapimage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g2 = snapimage.createGraphics();
		MVPop2D mvpop = new MVPop2D(null, engine.getModel().isContinuous() ? Data.CSTRAT : Data.DSTRAT);
		mvpop.setModule(module);
		mvpop.reset(true);
		PopGraph2D snapgraph = (PopGraph2D) mvpop.getGraphs().get(0);
		snapgraph.prepareSnapshot(g2, new Rectangle(0, 0, width, height));
		g2.dispose();
		return snapimage;
	}

	/**
	 * static method to create snapshots for simulations in bitmap format
	 * 
	 * @param engine   the pacemaker for running the model reference to engine (for
	 *                 logging)
	 * @param module   reference to module that supplies the data for the snapshot
	 * @param width    of snapshot
	 * @param height   of snapshot
	 * @param snapfile file for saving snapshot
	 */
	public static void exportSnapshotPNG(EvoLudo engine, Module module, int width, int height, File snapfile) {
		BufferedImage snapimage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g2 = snapimage.createGraphics();
		MVPop2D mvpop = new MVPop2D(null, engine.getModel().isContinuous() ? Data.CSTRAT : Data.DSTRAT);
		mvpop.setModule(module);
		mvpop.reset(true);
		PopGraph2D snapgraph = (PopGraph2D) mvpop.getGraphs().get(0);
		snapgraph.prepareSnapshot(g2, new Rectangle(0, 0, width, height));
		g2.dispose();
		try {
			ImageIO.write(snapimage, "PNG", snapfile);
		} catch (IOException x) {
			engine.getLogger().warning("writing PNG image to " + snapfile.getAbsolutePath() + " failed!");
			return;
		}
		engine.getLogger().info("PNG image written to " + snapfile.getAbsolutePath());
		return;
	}

	/**
	 * static method to create snapshots for simulations in vector format
	 * 
	 * @param engine     the pacemaker for running the model reference to engine
	 *                   (for logging)
	 * @param module     reference to module that supplies the data for the snapshot
	 * @param width      of snapshot
	 * @param height     of snapshot
	 * @param snapfile   file for saving snapshot
	 * @param compressed <code>true</code> to compress snapshot
	 */
	public static void exportSnapshotSVG(EvoLudo engine, Module module, int width, int height, File snapfile,
			boolean compressed) {
		Properties props = new Properties();
		MVPop2D mvpop = new MVPop2D(null, engine.getModel().isContinuous() ? Data.CSTRAT : Data.DSTRAT);
		mvpop.setModule(module);
		mvpop.reset(true);
		PopGraph2D snapgraph = (PopGraph2D) mvpop.getGraphs().get(0);
		try {
			props.setProperty(SVGGraphics2D.TITLE, "Generated by EvoLudoLabs \u00a9 Christoph Hauert");
			if (compressed)
				props.setProperty(SVGGraphics2D.COMPRESS, "true");
			props.setProperty(SVGGraphics2D.TRANSPARENT, "true");
			VectorGraphics vg2 = new SVGGraphics2D(snapfile, new Dimension(width, height));
			vg2.setProperties(props);
			vg2.startExport();
			snapgraph.prepareSnapshot(vg2, new Rectangle(0, 0, width, height));
			// use print for high resolution output of PDF
			snapgraph.printAll(vg2);
			vg2.endExport();
			vg2.dispose();
		} catch (IOException x) {
			engine.getLogger().warning("writing SVG graphics to " + snapfile.getAbsolutePath() + " failed!");
			return;
		}
		engine.getLogger().info("SVG graphics written to " + snapfile.getAbsolutePath());
		return;
	}
}
