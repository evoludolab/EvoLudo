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
import java.util.List;

import org.evoludo.graphics.AbstractGraph;
import org.evoludo.graphics.GraphStyle;
import org.evoludo.graphics.PopGraph2D;
import org.evoludo.graphics.TooltipProvider;
import org.evoludo.math.ArrayMath;
import org.evoludo.simulator.ColorMap;
import org.evoludo.simulator.ColorMapCSS;
import org.evoludo.simulator.EvoLudoGWT;
import org.evoludo.simulator.geometries.AbstractGeometry;
import org.evoludo.simulator.geometries.GeometryType;
import org.evoludo.simulator.models.CModel;
import org.evoludo.simulator.models.Data;
import org.evoludo.simulator.modules.Continuous;
import org.evoludo.simulator.modules.Module;
import org.evoludo.ui.ContextMenu;
import org.evoludo.ui.ContextMenuCheckBoxItem;
import org.evoludo.util.Formatter;

import com.google.gwt.user.client.Command;

/**
 * View component that renders the distribution of continuous traits for one or
 * more species in the EvoLudo simulation. This view produces either a 1D
 * histogram (for a single trait) with temporal history represented as stacked
 * rows, or a 2D density plot (phase plane) for pairs of traits. The view is
 * intended to be used with continuous models only and expects the simulation
 * model to provide trait ranges and histogram data. For more than two traits
 * the context menu allows to select the traits shown along the horizontal and
 * vertical axes.
 *
 * <h3>Responsibilities</h3>
 * <ul>
 * <li>Create and manage one PopGraph2D per species module returned by the
 * engine.
 * <li>Allocate and maintain a shared bin storage array sized to the current
 * geometry ({@code MAX_BINS} for 1D, {@code MAX_BINS * MAX_BINS} for 2D).</li>
 * <li>Configure graph appearance (axis labels, ranges, ticks, color map) based
 * on the underlying Continuous module trait metadata.</li>
 * <li>Request 1D/2D trait histogram data from the CModel implementation and
 * translate those values into the graph data using the graph's color map.</li>
 * <li>Provide tooltips for histogram bins and a contextual menu to select which
 * traits to display on the X/Y axes (the latter only for multi-trait
 * modules).</li>
 * </ul>
 *
 * <h3>Behavioral notes</h3>
 * <ul>
 * <li>{@code MAX_BINS} (100) controls the resolution along each trait axis. For
 * single-trait modules a LINEAR geometry of size {@code MAX_BINS} is used (with
 * history rows), for multi-trait modules a square lattice with
 * {@code MAX_BINS * MAX_BINS} bins is used.</li>
 * <li>The shared {@code bins} array is reallocated when the geometry size
 * changes.</li>
 * <li>When initializing or resetting, the view updates axis ranges from the
 * {@link Continuous} module's reported trait min/max; changes to those ranges
 * trigger
 * a hard reset of the corresponding PopGraph2D.</li>
 * <li>The view currently only supports {@link Data#TRAIT} type; calls with
 * other {@link Data} types throw {@code UnsupportedOperationException}.</li>
 * <li>The tooltip content formats trait interval(s), frequency, and (for 1D
 * histograms) the associated time slice.</li>
 * <li>Trait selection via the context menu is available when the module exposes
 * three or more traits; submenus allow choosing the trait displayed on each
 * axis. Selecting an axis trait triggers a hard reset to rebuild geometries
 * and redraw.</li>
 * <li>Export formats supported by this view are SVG and PNG.</li>
 * </ul>
 *
 * <h3>Collaborators</h3>
 * <ul>
 * <li>{@link EvoLudoGWT} engine — provides modules, configuration, and model
 * time/updates.</li>
 * <li>{@link PopGraph2D} — per-species graph widget used to render
 * histogram/heatmap data.</li>
 * <li>{@link CModel} — queried for 1D/2D trait histogram data.</li>
 * <li>{@link Continuous} — module type that exposes trait names and min/max
 * ranges.</li>
 * <li>{@link AbstractGeometry} — describes graph layout (LINEAR or SQUARE) and
 * required
 * storage size.</li>
 * </ul>
 *
 * <h3>Notes</h3>
 * Histogram data are copied into the shared bins buffer and immediately applied
 * to the graph's data arrays. This avoids repeated allocations but means that
 * graphs cannot retain history independently.
 * 
 * @author Christoph Hauert
 */
public class Distribution extends AbstractView<PopGraph2D> implements TooltipProvider.Index {

	/**
	 * The maximum number of bins for the trait histograms.
	 */
	protected static final int MAX_BINS = 100;

	/**
	 * The storage to accommodate the trait histograms.
	 */
	double[] bins;

	/**
	 * The index of the trait to be shown along the x-axis.
	 */
	int traitXIdx = 0;

	/**
	 * The index of the trait to be shown along the y-axis.
	 */
	int traitYIdx = 1;

	/**
	 * Construct a new view to display the distribution of continuous traits of the
	 * current EvoLudo model.
	 * 
	 * @param engine the pacemaker for running the model
	 * @param type   the type of data to display
	 */
	public Distribution(EvoLudoGWT engine, Data type) {
		super(engine, type);
	}

	@Override
	public String getName() {
		return "Traits - Distribution";
	}

	@Override
	protected boolean allocateGraphs() {
		List<? extends Module<?>> species = engine.getModule().getSpecies();
		int nGraphs = species.size();
		if (graphs.size() == nGraphs)
			return false;
		destroyGraphs();
		for (Module<?> module : species) {
			PopGraph2D graph = new PopGraph2D(this, module);
			graph.setDebugEnabled(false);
			wrapper.add(graph);
			graphs.add(graph);
			// even if nGraphs did not change, the geometries associated with the graphs
			// still need to be updated
			graph.setGeometry(createGeometry(module.getNTraits()));
		}
		gRows = species.size();
		if (gRows * gCols == 2) {
			// always arrange horizontally if only two graphs
			gRows = 1;
			gCols = 2;
		}
		int width = 100 / gCols;
		int height = 100 / gRows;
		for (PopGraph2D graph : graphs)
			graph.setSize(width + "%", height + "%");
		return true;
	}

	@Override
	public void reset(boolean hard) {
		super.reset(hard);
		for (PopGraph2D graph : graphs) {
			if (!type.equals(Data.TRAIT)) {
				throw new UnsupportedOperationException("Distribution: not implemented for type " + type);
			}
			resetTrait(graph, hard);
		}
		update(hard);
	}

	/**
	 * Reset the given graph for displaying trait distributions.
	 * 
	 * @param graph the graph to reset
	 * @param hard  the flag to indicate whether to force a hard reset
	 */
	private void resetTrait(PopGraph2D graph, boolean hard) {
		Continuous module = (Continuous) graph.getModule();
		int nTraits = module.getNTraits();
		GraphStyle style = graph.getStyle();
		graph.setColorMap(new ColorMapCSS.Gradient1D(
				new Color[] { Color.WHITE, Color.BLACK, Color.YELLOW, Color.RED }, 500));
		double min = module.getTraitMin()[traitXIdx];
		double max = module.getTraitMax()[traitXIdx];
		if (Math.abs(min - style.xMin) > 1e-6 ||
				Math.abs(max - style.xMax) > 1e-6) {
			style.xMin = min;
			style.xMax = max;
			hard = true;
		}
		style.xLabel = module.getTraitName(traitXIdx);
		style.showLabel = false;
		style.showXLabel = true;
		style.showXTicks = true;
		style.showXTickLabels = true;
		style.showXLevels = false;
		if (nTraits == 1) {
			double rFreq = model.getTimeStep();
			// adjust y-axis scaling if report frequency has changed
			if (Math.abs(style.yIncr - rFreq) > 1e-6) {
				style.yMax = 0.0;
				style.yIncr = -rFreq;
				hard = true;
			}
			style.yLabel = "time";
			style.showYLevels = true;
		} else {
			min = module.getTraitMin()[traitYIdx];
			max = module.getTraitMax()[traitYIdx];
			if (Math.abs(min - style.yMin) > 1e-6 || Math.abs(max - style.yMax) > 1e-6) {
				style.yMin = min;
				style.yMax = max;
				hard = true;
			}
			style.yLabel = module.getTraitName(traitYIdx);
			style.showYLevels = false;
		}
		style.showDecoratedFrame = true;
		style.percentY = false;
		style.showYLabel = true;
		style.showYTickLabels = true;
		style.showYTicks = true;
		if (hard)
			graph.reset();
	}

	@Override
	public void modelDidInit() {
		super.modelDidInit();
		for (PopGraph2D graph : graphs)
			graph.init();
		update();
	}

	@Override
	public void update(boolean force) {
		// always read data - some nodes may have changed due to user actions
		double newtime = model.getUpdates();
		boolean isNext = (Math.abs(timestamp - newtime) > 1e-8);
		timestamp = newtime;
		for (PopGraph2D graph : graphs) {
			boolean doUpdate = isActive || graph.hasHistory();
			// if graph is neither active nor has history, force can be safely ignored
			// otherwise may lead to problems if graph has never been activated
			if (!doUpdate)
				continue;
			if (!type.equals(Data.TRAIT)) {
				throw new UnsupportedOperationException("Distribution: not implemented for type " + type);
			}
			// casts ok because trait histograms make sense only for continuous models
			((CModel) model).get2DTraitHistogramData(graph.getModule().getID(),
					bins, traitXIdx, traitYIdx);
			ColorMap.Gradient1D<String> cMap = (ColorMap.Gradient1D<String>) graph.getColorMap();
			cMap.setRange(0.0, ArrayMath.max(bins));
			cMap.translate(bins, graph.getData());
			graph.update(isNext);
			graph.paint(force);
		}
	}

	/**
	 * Create a geometry for the given number of traits. Utility method to generate
	 * a linear geometry (with history) for a single trait and a 2D square lattice
	 * for multiple traits.
	 * 
	 * @param nTraits the number of traits
	 * @return the geometry
	 */
	private AbstractGeometry createGeometry(int nTraits) {
		AbstractGeometry geometry;
		// adding a geometry name will display a label on the graph - not sure whether
		// we really want this...
		// geometry.name = module.getTraitName(n);
		if (nTraits == 1) {
			geometry = AbstractGeometry.create(engine, GeometryType.LINEAR);
			geometry.setSize(MAX_BINS);
		} else {
			geometry = AbstractGeometry.create(engine, GeometryType.SQUARE);
			geometry.setConnectivity(4);
			geometry.setSize(MAX_BINS * MAX_BINS);
		}
		if (bins == null || bins.length != geometry.getSize())
			bins = new double[geometry.getSize()];
		return geometry;
	}

	/*
	 * @Override
	 * public boolean verifyMarkedBins(HistoFrameLayer frame, int tag) {
	 * int nMarked = population.getMonoScoreCount();
	 * if( nMarked==0 ) return false;
	 * Color[] colors = getColors(tag);
	 * if( population.isContinuous() ) {
	 * // for continuous strategies we have a single histogram and may want to mark
	 * several bins
	 * boolean changed = false;
	 * for( int n=0; n<nMarked; n++ ) {
	 * changed |= frame.updateMarkedBin(n,
	 * population.getMonoScore(n),
	 * new Color(Math.max(colors[n].getRed(), 127),
	 * Math.max(colors[n].getGreen(), 127),
	 * Math.max(colors[n].getBlue(), 127)));
	 * }
	 * return changed;
	 * }
	 * // for discrete strategies we have different histograms and mark only a
	 * single bin
	 * return frame.updateMarkedBin(0,
	 * population.getMonoScore(tag),
	 * new Color(Math.max(colors[tag].getRed(), 127),
	 * Math.max(colors[tag].getGreen(), 127),
	 * Math.max(colors[tag].getBlue(), 127)));
	 * }
	 */

	@Override
	public String getTooltipAt(AbstractGraph<?> graph, int node) {
		if (node < 0)
			return null;
		GraphStyle style = graph.getStyle();
		int nBins = MAX_BINS;
		StringBuilder tip = new StringBuilder(TABLE_STYLE);
		if (style.label != null)
			tip.append("<b>")
					.append(style.label)
					.append("</b>");
		tip.append(TABLE_ROW_START)
				.append(style.xLabel)
				.append(TABLE_CELL_NEXT);
		Module<?> module = engine.getModule();
		int nTraits = module.getNTraits();
		if (nTraits == 1) {
			int bar = node % nBins;
			int row = node / nBins;
			double time = -row * model.getTimeStep();
			tip.append(Formatter.format(style.xMin + bar * (style.xMax - style.xMin) / nBins, 2))
					.append(", ")
					.append(Formatter.format(style.xMin + (bar + 1) * (style.xMax - style.xMin) / nBins, 2))
					.append("]")
					.append(TABLE_ROW_END);
			if (node < nBins) {
				// report frequency for most recent row
				// all historical data is stored in terms of colors only...
				tip.append(TABLE_ROW_START)
						.append("frequency")
						.append(TABLE_CELL_NEXT)
						.append(Formatter.formatPercent(bins[bar], 1))
						.append(TABLE_ROW_END);
			}
			// + 0.0: silly trick to avoid -0 display
			tip.append(TABLE_ROW_START)
					.append(style.yLabel)
					.append(TABLE_CELL_NEXT)
					.append(Formatter.format(time + 0.0, 2));
		} else {
			int bar1 = node % MAX_BINS;
			int bar2 = node / MAX_BINS;
			// horizontal trait
			tip.append(Formatter.format(style.xMin + bar1 * (style.xMax - style.xMin) / nBins, 2))
					.append(", ")
					.append(Formatter.format(style.xMin + (bar1 + 1) * (style.xMax - style.xMin) / nBins, 2))
					.append("]")
					.append(TABLE_ROW_END);
			// vertical trait
			tip.append(TABLE_ROW_START)
					.append(style.yLabel)
					.append(TABLE_CELL_NEXT)
					.append(Formatter.format(style.yMin + bar2 * (style.yMax - style.yMin) / nBins, 2))
					.append(", ")
					.append(Formatter.format(style.yMin + (bar2 + 1) * (style.yMax - style.yMin) / nBins, 2))
					.append("]");
			// report frequency
			tip.append(TABLE_ROW_START)
					.append("frequency")
					.append(TABLE_CELL_NEXT)
					.append(Formatter.formatPercent(bins[node], 1))
					.append(TABLE_ROW_END);
		}
		tip.append(TABLE_ROW_END)
				.append(TABLE_END);
		return tip.toString();
	}

	@Override
	public void populateContextMenuAt(ContextMenu menu, int node) {
		Module<?> module = engine.getModule();
		int nTraits = module.getNTraits();
		// ignore if less than 3 traits
		if (nTraits < 3) {
			traitXMenu = traitYMenu = null;
			traitXItems = traitYItems = null;
			populateContextMenu(menu);
			return;
		}
		if (traitXMenu == null || traitXItems == null || traitXItems.length != nTraits) {
			traitXMenu = new ContextMenu(menu);
			traitXItems = new ContextMenuCheckBoxItem[nTraits];
			for (int n = 0; n < nTraits; n++) {
				traitXItems[n] = new ContextMenuCheckBoxItem(module.getTraitName(n),
						new TraitCommand(traitXItems, n, TraitCommand.X_AXIS));
				traitXMenu.add(traitXItems[n]);
			}
		}
		if (traitYMenu == null || traitYItems == null || traitYItems.length != nTraits) {
			traitYMenu = new ContextMenu(menu);
			traitYItems = new ContextMenuCheckBoxItem[nTraits];
			for (int n = 0; n < nTraits; n++) {
				traitYItems[n] = new ContextMenuCheckBoxItem(module.getTraitName(n),
						new TraitCommand(traitYItems, n, TraitCommand.Y_AXIS));
				traitYMenu.add(traitYItems[n]);
			}
		}
		// init trait submenus
		for (int n = 0; n < traitXItems.length; n++)
			traitXItems[n].setChecked(false);
		traitXItems[traitXIdx].setChecked(true);
		for (int n = 0; n < traitYItems.length; n++)
			traitYItems[n].setChecked(false);
		traitYItems[traitYIdx].setChecked(true);
		menu.addSeparator();
		menu.add("X-axis trait...", traitXMenu);
		menu.add("Y-axis trait...", traitYMenu);
		populateContextMenu(menu);
	}

	@Override
	protected ExportType[] exportTypes() {
		return new ExportType[] { ExportType.SVG, ExportType.PNG };
	}

	/**
	 * The context menu for selecting traits to display on the horizontal axis.
	 */
	private ContextMenuCheckBoxItem[] traitXItems;

	/**
	 * The context menu for selecting traits to display on the vertical axis.
	 */
	private ContextMenuCheckBoxItem[] traitYItems;

	/**
	 * The context menu trigger for selecting traits to display on the horizontal
	 * axis.
	 */
	private ContextMenu traitXMenu;

	/**
	 * The context menu trigger for selecting traits to display on the vertical
	 * axis.
	 */
	private ContextMenu traitYMenu;

	/**
	 * Command to toggle the inclusion of a trait on the phase plane axes.
	 */
	public class TraitCommand implements Command {

		/**
		 * The index of the horizontal axis.
		 */
		public static final int X_AXIS = 0;

		/**
		 * The index of the vertical axis.
		 */
		public static final int Y_AXIS = 1;

		/**
		 * The index of the trait to show on the axis.
		 */
		int idx = -1;

		/**
		 * The axis that this command affects.
		 */
		int axis = -1;

		/**
		 * The list of traits to toggle.
		 */
		ContextMenuCheckBoxItem[] traitItems;

		/**
		 * Construct a new command to toggle the inclusion of a trait on either one of
		 * the phase plane axes.
		 * 
		 * @param traitItems the list of traits to toggle
		 * @param idx        the index of the trait to show/hide on the axis
		 * @param axis       the index of the axis
		 */
		public TraitCommand(ContextMenuCheckBoxItem[] traitItems, int idx, int axis) {
			this.traitItems = traitItems;
			this.idx = idx;
			this.axis = axis;
		}

		@Override
		public void execute() {
			// make sure all items are unchecked, then check current one
			for (int n = 0; n < traitItems.length; n++)
				traitItems[n].setChecked(false);
			traitItems[idx].setChecked(true);
			if (axis == X_AXIS)
				traitXIdx = idx;
			else
				traitYIdx = idx;
			reset(true);
		}
	}
}