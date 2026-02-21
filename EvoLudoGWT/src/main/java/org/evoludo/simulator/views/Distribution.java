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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.evoludo.graphics.AbstractGraph;
import org.evoludo.graphics.DistrGraph2D;
import org.evoludo.graphics.GraphStyle;
import org.evoludo.graphics.PopGraph1D;
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
import org.evoludo.ui.ContextMenuItem;
import org.evoludo.util.RingBuffer;
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
 * geometry ({@code nBins} for 1D, {@code nBins * nBins} for
 * 2D).</li>
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
 * <li>The number of bins per axis is configurable from the context menu
 * (100/200/500), with {@code DEFAULT_BINS} (100) as the initial value. For
 * single-trait modules a LINEAR geometry of size {@code nBins} is used
 * (with history rows), for multi-trait modules a square lattice with
 * {@code nBins * nBins} bins is used.</li>
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
	 * Default number of bins per trait axis for histograms.
	 */
	protected static final int DEFAULT_BINS = 100;

	/**
	 * Available bin counts per axis.
	 */
	private static final int[] BIN_OPTIONS = new int[] { 100, 200, 500 };

	/**
	 * Sentinel for unavailable frequency values.
	 */
	private static final double INVALID_FREQUENCY = -1.0;

	/**
	 * Active number of bins per axis.
	 */
	private int nBins = DEFAULT_BINS;

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
	public int[] getBufferMenuCapacities() {
		return new int[] { 1000, 2000, 5000, 10000 };
	}

	@Override
	protected boolean allocateGraphs() {
		List<? extends Module<?>> species = engine.getModule().getSpecies();
		int nGraphs = species.size();
		boolean compatible = graphs.size() == nGraphs;
		if (compatible) {
			for (int i = 0; i < nGraphs; i++) {
				boolean linear = species.get(i).getNTraits() == 1;
				PopGraph2D graph = graphs.get(i);
				if (linear != (graph instanceof PopGraph1D)) {
					compatible = false;
					break;
				}
			}
		}
		if (compatible)
			return false;
		destroyGraphs();
		for (Module<?> module : species) {
			PopGraph2D graph = module.getNTraits() == 1
					? new PopGraph1D(this, module)
					: new DistrGraph2D(this, module);
			graph.setDebugEnabled(false);
			graph.setLayoutMenusEnabled(false);
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
		style.showYAxisRight = false;
		if (hard)
			graph.reset();
	}

	@Override
	public void modelDidInit() {
		super.modelDidInit();
		for (PopGraph2D graph : graphs)
			graph.init();
		updateData(true);
		update();
	}

	@Override
	protected void updateData(boolean force) {
		// force intentionally ignored; update policy depends on active/history state.
		// always read data - some nodes may have changed due to user actions
		double newtime = model.getUpdates();
		boolean isNext = (Math.abs(timestamp - newtime) > 1e-8);
		boolean updated = false;
		for (PopGraph2D graph : graphs) {
			boolean doUpdate = (isActive || graph.hasHistory());
			if (!doUpdate)
				continue;
			if (!type.equals(Data.TRAIT)) {
				throw new UnsupportedOperationException("Distribution: not implemented for type " + type);
			}
			// casts ok because trait histograms make sense only for continuous models
			((CModel) model).get2DTraitHistogramData(graph.getModule().getId(),
					bins, traitXIdx, traitYIdx);
			ColorMap.Gradient1D<String> cMap = (ColorMap.Gradient1D<String>) graph.getColorMap();
			cMap.setRange(0.0, ArrayMath.max(bins));
			cMap.translate(bins, graph.getData());
			graph.update(isNext);
			updated = true;
		}
		if (updated)
			timestamp = newtime;
	}

	@Override
	public void update(boolean force) {
		if (!isActive)
			return;
		for (PopGraph2D graph : graphs)
			graph.paint(force);
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
			geometry.setSize(nBins);
		} else {
			geometry = AbstractGeometry.create(engine, GeometryType.SQUARE_NEUMANN);
			geometry.setConnectivity(4);
			geometry.setSize(nBins * nBins);
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
		int binsAxis = nBins;
		PopGraph2D popGraph = (graph instanceof PopGraph2D ? (PopGraph2D) graph : null);
		ColorMap.Gradient1D<String> cMap = null;
		if (popGraph != null && popGraph.getColorMap() instanceof ColorMap.Gradient1D) {
			ColorMap.Gradient1D<String> map = (ColorMap.Gradient1D<String>) popGraph.getColorMap();
			cMap = map;
		}
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
			int bar = node % binsAxis;
			int row = node / binsAxis;
			double time = -row * model.getTimeStep();
			tip.append(Formatter.format(style.xMin + bar * (style.xMax - style.xMin) / binsAxis, 2))
					.append(", ")
					.append(Formatter.format(style.xMin + (bar + 1) * (style.xMax - style.xMin) / binsAxis, 2))
					.append("]")
					.append(TABLE_ROW_END);
			double frequency = INVALID_FREQUENCY;
			if (popGraph instanceof PopGraph1D) {
				RingBuffer<String[]> history = popGraph.getBuffer();
				if (history != null && row < history.getSize()) {
					String[] rowData = history.get(row);
					frequency = decodeProbabilityMass(rowData, bar, binsAxis, cMap);
				}
			}
			if (frequency < 0.0 && !(popGraph instanceof PopGraph1D) && bar < bins.length)
				frequency = bins[bar];
			if (frequency >= 0.0) {
				tip.append(TABLE_ROW_START)
						.append("frequency")
						.append(TABLE_CELL_NEXT)
						.append(Formatter.formatPercent(frequency, 1))
						.append(TABLE_ROW_END);
			}
			// + 0.0: silly trick to avoid -0 display
			tip.append(TABLE_ROW_START)
					.append(style.yLabel)
					.append(TABLE_CELL_NEXT)
					.append(Formatter.format(time + 0.0, 2));
		} else {
			int bar1 = node % binsAxis;
			int bar2 = node / binsAxis;
			// horizontal trait
			tip.append(Formatter.format(style.xMin + bar1 * (style.xMax - style.xMin) / binsAxis, 2))
					.append(", ")
					.append(Formatter.format(style.xMin + (bar1 + 1) * (style.xMax - style.xMin) / binsAxis, 2))
					.append("]")
					.append(TABLE_ROW_END);
			// vertical trait
			tip.append(TABLE_ROW_START)
					.append(style.yLabel)
					.append(TABLE_CELL_NEXT)
					.append(Formatter.format(style.yMin + bar2 * (style.yMax - style.yMin) / binsAxis, 2))
					.append(", ")
					.append(Formatter.format(style.yMin + (bar2 + 1) * (style.yMax - style.yMin) / binsAxis, 2))
					.append("]");
			// report frequency
			double frequency = INVALID_FREQUENCY;
			if (node < bins.length)
				frequency = bins[node];
			if (frequency >= 0.0) {
				tip.append(TABLE_ROW_START)
						.append("frequency")
						.append(TABLE_CELL_NEXT)
						.append(Formatter.formatPercent(frequency, 1))
						.append(TABLE_ROW_END);
			}
		}
		tip.append(TABLE_ROW_END)
				.append(TABLE_END);
		return tip.toString();
	}

	/**
	 * Decode a bin's probability mass from color-encoded histogram data.
	 *
	 * @param colors   the color-encoded histogram
	 * @param idx      the bin index to decode
	 * @param nEntries number of histogram entries to consider
	 * @param cMap     color map used for encoding
	 * @return decoded probability mass or {@link #INVALID_FREQUENCY} if unavailable
	 */
	private static double decodeProbabilityMass(String[] colors, int idx, int nEntries,
			ColorMap.Gradient1D<String> cMap) {
		if (idx < 0)
			return INVALID_FREQUENCY;
		double[] masses = decodeHistogram1D(colors, cMap, nEntries);
		if (masses == null || idx >= masses.length)
			return INVALID_FREQUENCY;
		return masses[idx];
	}

	@Override
	public void populateContextMenu(ContextMenu menu) {
		Module<?> module = engine.getModule();
		int nTraits = module.getNTraits();
		addAxesMenu(menu);
		addBinsMenu(menu, nTraits > 1);
		// ignore if less than 3 traits
		if (nTraits < 3) {
			traitXMenu = traitYMenu = null;
			traitXItems = traitYItems = null;
			super.populateContextMenu(menu);
			return;
		}
		if (traitXMenu == null || traitXItems == null || traitXItems.length != nTraits) {
			traitXMenu = new ContextMenu(menu, "X-axis trait");
			traitXItems = new ContextMenuCheckBoxItem[nTraits];
			for (int n = 0; n < nTraits; n++) {
				traitXItems[n] = new ContextMenuCheckBoxItem(module.getTraitName(n),
						new TraitCommand(traitXItems, n, TraitCommand.X_AXIS));
				traitXMenu.add(traitXItems[n]);
			}
		}
		if (traitYMenu == null || traitYItems == null || traitYItems.length != nTraits) {
			traitYMenu = new ContextMenu(menu, "Y-axis trait");
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
		menu.add("X-axis trait", traitXMenu);
		menu.add("Y-axis trait", traitYMenu);
		super.populateContextMenu(menu);
	}

	/**
	 * Add axes-related entries to the context menu.
	 *
	 * @param menu the context menu to populate
	 */
	private void addAxesMenu(ContextMenu menu) {
		if (graphs.isEmpty())
			return;
		if (axesMenu == null) {
			axesMenu = new ContextMenu(menu, "Axes");
			rightYAxisMenu = new ContextMenuCheckBoxItem("Right Y-axis", () -> {
				boolean showOnRight = !graphs.get(0).getStyle().showYAxisRight;
				rightYAxisMenu.setChecked(showOnRight);
				setRightYAxis(showOnRight);
			});
		}
		axesMenu.clear();
		axesMenu.addHeader("Axes");
		rightYAxisMenu.setChecked(graphs.get(0).getStyle().showYAxisRight);
		axesMenu.add(rightYAxisMenu);
		menu.add("Axes", axesMenu);
	}

	/**
	 * Add binning controls to the context menu.
	 *
	 * @param menu the context menu to populate
	 * @param is2D {@code true} for 2D distributions
	 */
	private void addBinsMenu(ContextMenu menu, boolean is2D) {
		if (graphs.isEmpty())
			return;
		if (binsMenu == null) {
			binsMenu = new ContextMenu(menu, "Bins");
			binOptionItems = new ContextMenuCheckBoxItem[BIN_OPTIONS.length];
			for (int i = 0; i < BIN_OPTIONS.length; i++) {
				final int option = BIN_OPTIONS[i];
				binOptionItems[i] = new ContextMenuCheckBoxItem(Integer.toString(option), () -> {
					applyBinsPerAxis(option);
					updateBinMenuChecks();
				});
			}
		}
		binsMenu.clear();
		binsMenu.addHeader(is2D ? "Bins per axis" : "Bins");
		for (ContextMenuCheckBoxItem item : binOptionItems)
			binsMenu.add(item);
		updateBinMenuChecks();
		ContextMenuItem binsTrigger = menu.add("Bins", binsMenu);
		binsTrigger.setEnabled(!isRunning());
	}

	/**
	 * Update checked state of binning menu entries.
	 */
	private void updateBinMenuChecks() {
		if (binOptionItems == null)
			return;
		for (int i = 0; i < BIN_OPTIONS.length; i++)
			binOptionItems[i].setChecked(BIN_OPTIONS[i] == nBins);
	}

	/**
	 * Apply a new number of bins per axis and preserve existing rendered data.
	 *
	 * @param newBins the new number of bins per axis
	 */
	private void applyBinsPerAxis(int newBins) {
		if (newBins <= 0 || newBins == nBins)
			return;
		int oldBins = nBins;
		nBins = newBins;
		bins = rebinHistogram(bins, oldBins, newBins);
		for (PopGraph2D graph : graphs)
			rebinGraphData(graph, oldBins, newBins);
		updateData(true);
		update(true);
	}

	/**
	 * Rebin data for a single graph while preserving current state/history.
	 *
	 * @param graph   the graph to rebin
	 * @param oldBins previous number of bins per axis
	 * @param newBins new number of bins per axis
	 */
	private void rebinGraphData(PopGraph2D graph, int oldBins, int newBins) {
		if (!(graph instanceof PopGraph1D)) {
			// no history in 2D distribution graphs; data will be rebuilt in updateData()
			graph.setGeometry(createGeometry(graph.getModule().getNTraits()));
			return;
		}
		String[] oldData = graph.getData();
		if (oldData != null)
			oldData = Arrays.copyOf(oldData, oldData.length);
		ColorMap.Gradient1D<String> cMap = null;
		if (graph.getColorMap() instanceof ColorMap.Gradient1D)
			cMap = (ColorMap.Gradient1D<String>) graph.getColorMap();
		List<String[]> oldHistory = null;
		RingBuffer<String[]> history = graph.getBuffer();
		if (history != null && !history.isEmpty()) {
			oldHistory = new ArrayList<>(history.getSize());
			Iterator<String[]> it = history.ordered();
			while (it.hasNext()) {
				String[] row = it.next();
				oldHistory.add(Arrays.copyOf(row, row.length));
			}
		}

		graph.setGeometry(createGeometry(graph.getModule().getNTraits()));

		String[] newData = graph.getData();
		if (oldData != null && newData != null) {
			String[] rebinned = null;
			if (cMap != null) {
				double[] masses = decodeHistogram1D(oldData, cMap, oldBins);
				if (masses != null) {
					double[] rebinnedMasses = rebinHistogram1D(masses, oldBins, newBins);
					rebinned = encodeHistogram(rebinnedMasses, cMap);
				}
			}
			if (rebinned == null)
				rebinned = Arrays.copyOf(oldData, newData.length);
			System.arraycopy(rebinned, 0, newData, 0, Math.min(rebinned.length, newData.length));
		}

		if (oldHistory != null) {
			history.clear();
			for (String[] row : oldHistory) {
				String[] rebinned = null;
				if (cMap != null) {
					double[] masses = decodeHistogram1D(row, cMap, oldBins);
					if (masses != null) {
						double[] rebinnedMasses = rebinHistogram1D(masses, oldBins, newBins);
						rebinned = encodeHistogram(rebinnedMasses, cMap);
					}
				}
				if (rebinned == null)
					rebinned = Arrays.copyOf(row, newBins);
				history.append(rebinned);
			}
		}
	}

	/**
	 * Decode a 1D histogram row from colors into normalized probability masses.
	 *
	 * @param source source colors
	 * @param cMap   color map used for encoding
	 * @param bins   number of bins
	 * @return decoded masses or {@code null} if decoding is not possible
	 */
	private static double[] decodeHistogram1D(String[] source, ColorMap.Gradient1D<String> cMap, int bins) {
		if (source == null || cMap == null || bins <= 0)
			return null;
		double[] decoded = new double[bins];
		int len = Math.min(Math.min(source.length, decoded.length), bins);
		double sum = 0.0;
		for (int i = 0; i < len; i++) {
			double mass = nonNegativeFinite(cMap.normalize(source[i]));
			decoded[i] = mass;
			sum += mass;
		}
		if (sum <= 0.0)
			return null;
		double invSum = 1.0 / sum;
		for (int i = 0; i < len; i++)
			decoded[i] *= invSum;
		return decoded;
	}

	/**
	 * Convert non-finite or negative values to zero.
	 *
	 * @param value the value to sanitize
	 * @return {@code value} if finite and non-negative, {@code 0.0} otherwise
	 */
	private static double nonNegativeFinite(double value) {
		return (Double.isFinite(value) && value >= 0.0 ? value : 0.0);
	}

	/**
	 * Encode histogram masses into colors using the graph color map.
	 *
	 * @param masses histogram masses
	 * @param cMap   color map used for encoding
	 * @return encoded colors
	 */
	private static String[] encodeHistogram(double[] masses, ColorMap.Gradient1D<String> cMap) {
		if (masses == null || cMap == null)
			return null;
		String[] encoded = new String[masses.length];
		cMap.setRange(0.0, ArrayMath.max(masses));
		cMap.translate(masses, encoded);
		return encoded;
	}

	/**
	 * Rebin histogram masses while preserving totals.
	 *
	 * @param source  source histogram
	 * @param oldBins previous bins per axis
	 * @param newBins new bins per axis
	 * @return rebinned histogram
	 */
	private static double[] rebinHistogram(double[] source, int oldBins, int newBins) {
		if (source == null)
			return null;
		if (newBins <= 0)
			return new double[0];
		if (source.length == oldBins)
			return rebinHistogram1D(source, oldBins, newBins);
		if (source.length == oldBins * oldBins)
			return rebinHistogram2D(source, oldBins, newBins);
		return Arrays.copyOf(source, source.length);
	}

	/**
	 * Rebin 1D histogram masses by exact overlap.
	 *
	 * @param source  source masses
	 * @param oldBins old bins
	 * @param newBins new bins
	 * @return rebinned masses
	 */
	private static double[] rebinHistogram1D(double[] source, int oldBins, int newBins) {
		double[] rebinned = new double[newBins];
		for (int i = 0; i < oldBins; i++) {
			double start = i / (double) oldBins;
			double end = (i + 1) / (double) oldBins;
			int j0 = Math.max(0, (int) Math.floor(start * newBins));
			int j1 = Math.min(newBins - 1, (int) Math.ceil(end * newBins) - 1);
			for (int j = j0; j <= j1; j++) {
				double nStart = j / (double) newBins;
				double nEnd = (j + 1) / (double) newBins;
				double overlap = Math.max(0.0, Math.min(end, nEnd) - Math.max(start, nStart));
				if (overlap <= 0.0)
					continue;
				rebinned[j] += source[i] * overlap * oldBins;
			}
		}
		return rebinned;
	}

	/**
	 * Rebin 2D histogram masses by exact cell overlap.
	 *
	 * @param source  source masses (row-major)
	 * @param oldBins old bins per axis
	 * @param newBins new bins per axis
	 * @return rebinned masses (row-major)
	 */
	private static double[] rebinHistogram2D(double[] source, int oldBins, int newBins) {
		double[] rebinned = new double[newBins * newBins];
		for (int oy = 0; oy < oldBins; oy++) {
			double yStart = oy / (double) oldBins;
			double yEnd = (oy + 1) / (double) oldBins;
			int ny0 = Math.max(0, (int) Math.floor(yStart * newBins));
			int ny1 = Math.min(newBins - 1, (int) Math.ceil(yEnd * newBins) - 1);
			for (int ox = 0; ox < oldBins; ox++) {
				double xStart = ox / (double) oldBins;
				double xEnd = (ox + 1) / (double) oldBins;
				int nx0 = Math.max(0, (int) Math.floor(xStart * newBins));
				int nx1 = Math.min(newBins - 1, (int) Math.ceil(xEnd * newBins) - 1);
				double mass = source[oy * oldBins + ox];
				for (int ny = ny0; ny <= ny1; ny++) {
					double nYStart = ny / (double) newBins;
					double nYEnd = (ny + 1) / (double) newBins;
					double yOverlap = Math.max(0.0, Math.min(yEnd, nYEnd) - Math.max(yStart, nYStart));
					if (yOverlap <= 0.0)
						continue;
					for (int nx = nx0; nx <= nx1; nx++) {
						double nXStart = nx / (double) newBins;
						double nXEnd = (nx + 1) / (double) newBins;
						double xOverlap = Math.max(0.0, Math.min(xEnd, nXEnd) - Math.max(xStart, nXStart));
						if (xOverlap <= 0.0)
							continue;
						rebinned[ny * newBins + nx] += mass * xOverlap * yOverlap * oldBins * oldBins;
					}
				}
			}
		}
		return rebinned;
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
	 * The context menu trigger for axis settings.
	 */
	private ContextMenu axesMenu;

	/**
	 * The context menu trigger for binning settings.
	 */
	private ContextMenu binsMenu;

	/**
	 * The context menu item to toggle the y-axis side.
	 */
	private ContextMenuCheckBoxItem rightYAxisMenu;

	/**
	 * The context menu items to select bin count per axis.
	 */
	private ContextMenuCheckBoxItem[] binOptionItems;

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
