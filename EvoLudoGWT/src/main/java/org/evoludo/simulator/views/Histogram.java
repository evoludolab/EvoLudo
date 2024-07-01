//
// EvoLudo Project
//
// Copyright 2010 Christoph Hauert
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
//	Hauert, Christoph (<year>) EvoLudo Project, http://www.evoludo.org
//			(doi: <doi>[, <version>])
//
//	<doi>:	digital object identifier of the downloaded release (or the
//			most recent release if downloaded from github.com),
//	<year>:	year of release (or download), and
//	[, <version>]: optional version number (as reported in output header
//			or GUI console) to simplify replication of reported results.
//
// The formatting may be adjusted to comply with publisher requirements.
//

package org.evoludo.simulator.views;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.evoludo.EvoLudoWeb;
import org.evoludo.graphics.AbstractGraph;
import org.evoludo.graphics.AbstractGraph.GraphStyle;
import org.evoludo.graphics.HistoGraph;
import org.evoludo.math.ArrayMath;
import org.evoludo.math.Combinatorics;
import org.evoludo.math.Distributions;
import org.evoludo.math.Functions;
import org.evoludo.simulator.ColorMap;
import org.evoludo.simulator.ColorMapCSS;
import org.evoludo.simulator.EvoLudoGWT;
import org.evoludo.simulator.Geometry;
import org.evoludo.simulator.models.DE;
import org.evoludo.simulator.models.Model;
import org.evoludo.simulator.models.Model.Mode;
import org.evoludo.simulator.models.Statistics;
import org.evoludo.simulator.models.Statistics.FixationData;
import org.evoludo.simulator.modules.Continuous;
import org.evoludo.simulator.modules.Discrete;
import org.evoludo.simulator.modules.Module;
import org.evoludo.ui.ContextMenu;
import org.evoludo.ui.ContextMenuItem;
import org.evoludo.util.Formatter;

import com.google.gwt.core.client.Duration;
import com.google.gwt.user.client.Command;

/**
 * The view to display a histogram of various quantities of the current EvoLudo
 * model.
 *
 * @author Christoph Hauert
 */
public class Histogram extends AbstractView {

	/**
	 * The list of graphs that display the time series data.
	 * 
	 * @evoludo.impl {@code List<HistoGraph> graphs} is deliberately hiding
	 *               {@code List<AbstractGraph> graphs} from the superclass because
	 *               it saves a lot of ugly casting. Note that the two fields point
	 *               to one and the same object.
	 */
	@SuppressWarnings("hiding")
	protected List<HistoGraph> graphs;

	/**
	 * The scaling factor to map the data onto bins.
	 */
	double scale2bins = 1.0;

	/**
	 * The size of the bins in pixels.
	 */
	int binSize = 1;

	/**
	 * Flag to indicate whether the model entertains multiple species, i.e.
	 * {@code nSpecies&gt;1}. Convenience field. Reduces calls to {@link Module}.
	 */
	protected boolean isMultispecies;

	/**
	 * The flag to indicate whether the properties of the gemetric structure have
	 * been processed.
	 */
	protected boolean degreeProcessed;

	/**
	 * Construct a new view to display the histogram of various quantities.
	 * 
	 * @param engine the pacemaker for running the model
	 * @param type   the type of data to display
	 */
	@SuppressWarnings("unchecked")
	public Histogram(EvoLudoGWT engine, Model.Data type) {
		super(engine, type);
		graphs = (List<HistoGraph>) super.graphs;
	}

	@Override
	public String getName() {
		return type.toString();
	}

	/**
	 * Checks if the data type is referring to statistics.
	 * 
	 * @return <code>true</code> for statistics data types
	 */
	public boolean isStatistics() {
		return (type == Model.Data.STATISTICS_FIXATION_PROBABILITY || //
				type == Model.Data.STATISTICS_FIXATION_TIME || //
				type == Model.Data.STATISTICS_STATIONARY);
	}

	@Override
	public Mode getMode() {
		switch (type) {
			case STRATEGY:
			case FITNESS:
			case DEGREE:
			default:
				return Mode.DYNAMICS;
			case STATISTICS_FIXATION_PROBABILITY:
			case STATISTICS_FIXATION_TIME:
				return Mode.STATISTICS_SAMPLE;
			case STATISTICS_STATIONARY:
				return Mode.STATISTICS_UPDATE;
		}
	}

	@Override
	public void reset(boolean hard) {
		super.reset(hard);
		ArrayList<? extends Module> species = engine.getModule().getSpecies();
		isMultispecies = (species.size() > 1);
		degreeProcessed = false;
		doStatistics = false;
		int nGraphs = 0;
		for (Module pop : species) {
			int nTraits = pop.getNTraits();
			switch (type) {
				case STRATEGY:
					// this only makes sense for continuous traits
					nGraphs += nTraits;
					break;
				case FITNESS:
					if (model.isContinuous())
						nGraphs++;
					else {
						nGraphs += nTraits;
						if (pop.getVacant() >= 0)
							nGraphs--;
					}
					break;
				case DEGREE:
					nGraphs += getDegreeGraphs(pop.getInteractionGeometry(), pop.getCompetitionGeometry());
					break;
				case STATISTICS_FIXATION_PROBABILITY:
				case STATISTICS_STATIONARY:
					nGraphs += nTraits;
					break;
				case STATISTICS_FIXATION_TIME:
					nGraphs += nTraits + 1;
					break;
				default:
					nGraphs = Integer.MIN_VALUE;
			}
		}

		if (graphs.size() != nGraphs) {
			int nXLabels = 0;
			hard = true;
			destroyGraphs();
			for (Module module : species) {
				int nTraits = module.getNTraits();
				switch (type) {
					case STRATEGY:
						// this only makes sense for continuous traits
						for (int n = 0; n < nTraits; n++) {
							HistoGraph graph = new HistoGraph(this, module, n);
							wrapper.add(graph);
							graphs.add(graph);
							AbstractGraph.GraphStyle style = graph.getStyle();
							// fixed style attributes
							style.yLabel = "frequency";
							style.percentY = true;
							style.showYLabel = true;
							style.showYTickLabels = true;
							style.showXTicks = true;
							style.showYTicks = true;
							style.showXLevels = false;
							style.showYLevels = true;
							style.showLabel = isMultispecies;
							style.showXLabel = true;
							style.showXTickLabels = true;
							nXLabels++;
						}
						break;

					case FITNESS:
						nTraits = (model.isContinuous() ? 1 : nTraits);
						int vacant = module.getVacant();
						int paneIdx = 0;
						int bottomPaneIdx = nTraits - 1;
						if (vacant >= 0 && vacant == nTraits - 1)
							bottomPaneIdx--;
						for (int n = 0; n < nTraits; n++) {
							if (n == vacant)
								continue;
							HistoGraph graph = new HistoGraph(this, module, paneIdx++);
							boolean bottomPane = (n == bottomPaneIdx);
							wrapper.add(graph);
							graphs.add(graph);
							AbstractGraph.GraphStyle style = graph.getStyle();
							// fixed style attributes
							if (model instanceof DE && ((DE) model).isDensity()) {
								style.yLabel = "density";
								style.percentY = false;
								style.yMin = 0.0;
								style.yMax = 0.0;
							} else {
								style.yLabel = "frequency";
								style.percentY = true;
								style.yMin = 0.0;
								style.yMax = 1.0;
							}
							style.showYLabel = true;
							style.showYTickLabels = true;
							style.showXTicks = true;
							style.showYTicks = true;
							style.showXLevels = false;
							style.showYLevels = true;
							style.xLabel = "payoffs";
							style.showXLabel = bottomPane; // show only on bottom panel
							style.showXTickLabels = bottomPane;
							if (model.isContinuous())
								style.showLabel = isMultispecies;
							else
								style.showLabel = true;
							if (bottomPane)
								nXLabels++;
						}
						break;

					case DEGREE:
						Model.Type mType = model.getModelType();
						if (mType == Model.Type.ODE || mType == Model.Type.SDE) {
							// happens for ODE/SDE/PDE - do not show distribution
							HistoGraph graph = new HistoGraph(this, module, 0);
							wrapper.add(graph);
							graphs.add(graph);
							break;
						}
						nTraits = getDegreeGraphs(module.getInteractionGeometry(), module.getCompetitionGeometry());
						Geometry inter = (mType == Model.Type.PDE ? module.getGeometry()
								: module.getInteractionGeometry());
						String[] labels = getDegreeLabels(nTraits, inter.isUndirected);
						for (int n = 0; n < nTraits; n++) {
							HistoGraph graph = new HistoGraph(this, module, n);
							boolean bottomPane = (n == nTraits - 1);
							wrapper.add(graph);
							graphs.add(graph);
							AbstractGraph.GraphStyle style = graph.getStyle();
							// fixed style attributes
							style.yLabel = "frequency";
							style.percentY = true;
							style.showYLabel = true;
							style.showYTickLabels = true;
							style.showXTicks = true;
							style.showYTicks = true;
							style.showXLevels = false;
							style.showYLevels = true;
							style.xLabel = "degree";
							style.showXLabel = false;
							style.showXTickLabels = false;
							style.label = labels[n];
							style.showXLabel = bottomPane;
							style.showXTickLabels = bottomPane;
							style.graphColor = "#444";
							if (bottomPane)
								nXLabels++;
						}
						break;

					case STATISTICS_FIXATION_PROBABILITY:
						for (int n = 0; n < nTraits; n++) {
							HistoGraph graph = new HistoGraph(this, module, n);
							boolean bottomPane = (n == nTraits - 1);
							graph.setNormalized(nTraits);
							wrapper.add(graph);
							graphs.add(graph);
							AbstractGraph.GraphStyle style = graph.getStyle();
							// fixed style attributes
							style.yLabel = "probability";
							style.percentY = true;
							style.autoscaleY = true;
							style.showYLabel = true;
							style.showYTickLabels = true;
							style.showXTicks = true;
							style.showYTicks = true;
							style.showXLevels = false;
							style.showYLevels = true;
							style.xLabel = "node";
							style.showLabel = true;
							style.showXLabel = bottomPane; // show only on bottom panel
							style.showXTickLabels = bottomPane;
							style.customYLevels = ((HasHistogram) module).getCustomLevels(type, n);
							if (bottomPane)
								nXLabels++;
						}
						break;

					case STATISTICS_FIXATION_TIME:
						nTraits++; // the last 'trait' is for unconditional absorption times
						for (int n = 0; n < nTraits; n++) {
							HistoGraph graph = new HistoGraph(this, module, n);
							boolean bottomPane = (n == nTraits - 1);
							wrapper.add(graph);
							graphs.add(graph);
							AbstractGraph.GraphStyle style = graph.getStyle();
							// fixed style attributes
							style.yLabel = "frequency";
							style.percentY = true;
							style.showYLabel = true;
							style.showYTickLabels = true;
							style.showXTicks = true;
							style.showYTicks = true;
							style.showXLevels = false;
							style.showYLevels = true;
							style.showLabel = true;
							style.showXLabel = bottomPane; // show only on bottom panel
							style.autoscaleY = true;
							if (bottomPane)
								nXLabels++;
						}
						break;

					case STATISTICS_STATIONARY:
						for (int n = 0; n < nTraits; n++) {
							HistoGraph graph = new HistoGraph(this, module, n);
							boolean bottomPane = (n == nTraits - 1);
							graph.setNormalized(false);
							wrapper.add(graph);
							graphs.add(graph);
							AbstractGraph.GraphStyle style = graph.getStyle();
							// fixed style attributes
							style.yLabel = "visits";
							style.percentY = true;
							style.autoscaleY = true;
							style.showYLabel = true;
							style.showYTickLabels = true;
							style.showXTicks = true;
							style.showYTicks = true;
							style.showXLevels = false;
							style.showYLevels = true;
							style.showLabel = true;
							style.showXLabel = bottomPane; // show only on bottom panel
							style.showXTickLabels = bottomPane;
							if (bottomPane)
								nXLabels++;
						}
						break;
					default:
				}
			}
			// arrange histograms vertically
			gRows = nGraphs;
			int width = 100 / gCols;
			int xaxisdeco = 7; // estimate of height of x-axis decorations in %
								// unfortunately GWT chokes on CSS calc()
			int height = (100 - nXLabels * xaxisdeco) / gRows;
			for (HistoGraph graph : graphs)
				graph.setSize(width + "%", height + (graph.getStyle().showXLabel ? xaxisdeco : 0) + "%");
		}
		Module module = null;
		double[][] data = null;
		int idx = 0;
		for (HistoGraph graph : graphs) {
			AbstractGraph.GraphStyle style = graph.getStyle();
			Module oldmod = module;
			module = graph.getModule();
			boolean newPop = (oldmod != module);
			if (newPop)
				data = graph.getData();
			int vacant = module.getVacant();
			int nTraits = module.getNTraits();
			Color[] colors = module.getTraitColors();

			switch (type) {
				case STRATEGY:
					// histogram of strategies makes only sense for continuous traits
					if (data == null || data.length != nTraits || data[0].length != HistoGraph.MAX_BINS)
						data = new double[nTraits][HistoGraph.MAX_BINS];
					graph.setData(data);
					graph.clearMarkers();
					ArrayList<double[]> markers = module.getMarkers();
					if (markers != null) {
						for (double[] mark : markers)
							graph.addMarker(mark[idx + 1], ColorMapCSS.Color2Css(colors[idx]), null,
									mark[0] > 0.0 ? style.dashedLine : style.dottedLine);
					}
					Continuous cmod = (Continuous) module;
					double min = cmod.getTraitMin()[idx];
					double max = cmod.getTraitMax()[idx];
					if (Math.abs(min - style.xMin) > 1e-6 || Math.abs(max - style.xMax) > 1e-6) {
						style.xMin = min;
						style.xMax = max;
						hard = true;
					}
					style.yMin = 0.0;
					style.yMax = 1.0;
					style.xLabel = module.getTraitName(idx);
					style.graphColor = ColorMapCSS.Color2Css(colors[idx]);
					if (isMultispecies)
						style.label = module.getName() + ": " + module.getTraitName(idx);
					break;

				case FITNESS:
					graph.clearMarkers();
					nTraits = (model.isContinuous() ? 1 : nTraits);
					if (vacant >= 0)
						nTraits--;
					if (data == null || data.length != nTraits || data[0].length != HistoGraph.MAX_BINS)
						data = new double[nTraits][HistoGraph.MAX_BINS];
					graph.setData(data);
					min = model.getMinScore(module.getID());
					max = model.getMaxScore(module.getID());
					if (Math.abs(min - style.xMin) > 1e-6 || Math.abs(max - style.xMax) > 1e-6) {
						style.xMin = min;
						style.xMax = max;
						hard = true;
					}
					style.yMin = 0.0;
					style.yMax = 1.0;
					if (module instanceof Discrete) {
						// cast is save because pop is Discrete
						org.evoludo.simulator.models.Model.Discrete dmodel = (org.evoludo.simulator.models.Model.Discrete) model;
						style.label = (isMultispecies ? module.getName() + ": " : "") + module.getTraitName(idx);
						Color tColor = colors[idx];
						style.graphColor = ColorMapCSS.Color2Css(tColor);
						double mono = dmodel.getMonoScore(module.getID(), idx);
						if (Double.isNaN(mono))
							break;
						graph.addMarker(mono,
								ColorMapCSS.Color2Css(ColorMap.blendColors(tColor, Color.WHITE, 0.5)),
								"monomorphic payoff");
						break;
					}
					if (module instanceof Continuous) {
						// cast is save because pop is Continuous
						org.evoludo.simulator.models.Model.Continuous cmodel = (org.evoludo.simulator.models.Model.Continuous) model;
						Color tcolor = colors[idx];
						graph.addMarker(cmodel.getMinMonoScore(module.getID()),
								ColorMapCSS.Color2Css(ColorMap.blendColors(tcolor, Color.BLACK, 0.5)),
								"minimum monomorphic payoff");
						graph.addMarker(cmodel.getMaxMonoScore(module.getID()),
								ColorMapCSS.Color2Css(ColorMap.blendColors(tcolor, Color.WHITE, 0.5)),
								"maximum monomorphic payoff");
						style.graphColor = ColorMapCSS.Color2Css(Color.BLACK);
						break;
					}
					// unknown population type - do not mark any fitness values
					break;

				case DEGREE:
					Model.Type mType = model.getModelType();
					if (mType == Model.Type.ODE || mType == Model.Type.SDE) {
						graph.setData(null);
						break;
					}
					Geometry inter, comp;
					if (mType == Model.Type.PDE) {
						inter = comp = module.getGeometry();
					} else {
						inter = module.getInteractionGeometry();
						comp = module.getCompetitionGeometry();
					}
					int rows = getDegreeGraphs(inter, comp);
					int cols = getDegreeBins(inter, comp);
					if (data == null || data.length != rows || data[0].length != cols)
						data = new double[rows][cols];
					graph.setData(data);
					style.yMin = 0.0;
					style.yMax = 1.0;
					style.xMin = 0.0;
					if (inter.isUndirected)
						style.xMax = maxDegree(Math.max(inter.maxOut, comp.maxOut));
					else
						style.xMax = maxDegree(Math.max(inter.maxTot, comp.maxTot));
					break;

				case STATISTICS_FIXATION_PROBABILITY:
					checkStatistics();
					style.yMin = 0.0;
					style.yMax = 1.0;
					style.xMin = 0;
					style.xMax = 1;
					style.label = module.getTraitName(idx);
					style.graphColor = ColorMapCSS.Color2Css(colors[idx]);
					if (doStatistics) {
						int nBins;
						if (model.getModelType() == Model.Type.SDE)
							nBins = 1;
						else {
							nBins = module.getNPopulation();
							style.xMax = nBins - 1;
						}
						if (data == null || data.length != nTraits + 1
								|| data[0].length != Math.min(nBins, HistoGraph.MAX_BINS))
							data = new double[nTraits + 1][Math.min(nBins, HistoGraph.MAX_BINS)];
						graph.setData(data);
					} else {
						graph.clearData();
					}
					break;

				case STATISTICS_FIXATION_TIME:
					checkStatistics();
					int nPop = module.getNPopulation();
					style.yMin = 0.0;
					style.yMax = 1.0;
					if (idx < nTraits) {
						style.label = module.getTraitName(idx);
						style.graphColor = ColorMapCSS.Color2Css(colors[idx]);
					} else {
						style.label = "Absorbtion";
						style.graphColor = ColorMapCSS.Color2Css(Color.BLACK);
					}
					if (doFixtimeDistr(module)) {
						if (doStatistics) {
							if (data == null || data.length != nTraits + 1 || data[0].length != HistoGraph.MAX_BINS)
								data = new double[nTraits + 1][HistoGraph.MAX_BINS];
							graph.setData(data);
						} else {
							graph.clearData();
						}
						graph.setNormalized(false);
						graph.setNormalized(-1);
						style.xMin = 0.0;
						style.xMax = Functions.roundUp(nPop / 4);
						style.xLabel = "fixation time";
						style.showXTickLabels = true;
						style.yLabel = "probability";
						style.percentY = true;
						graph.enableAutoscaleYMenu(true);
						style.customYLevels = null;
					} else {
						if (doStatistics) {
							if (data == null || data.length != 2 * (nTraits + 1) || data[0].length != nPop)
								data = new double[2 * (nTraits + 1)][nPop];
							graph.setData(data);
						} else {
							graph.clearData();
						}
						graph.setNormalized(idx + nTraits + 1);
						style.xMin = 0.0;
						style.xMax = nPop - 1;
						style.xLabel = "node";
						style.showXTickLabels = (idx == nTraits);
						style.yLabel = "time";
						style.percentY = false;
						graph.enableAutoscaleYMenu(false);
						style.customYLevels = ((HasHistogram) module).getCustomLevels(type, idx);
					}
					graph.setData(data);
					break;

				case STATISTICS_STATIONARY:
					style.label = (isMultispecies ? module.getName() + ": " : "") + module.getTraitName(idx);
					nPop = module.getNPopulation();
					if (model instanceof DE) {
						if (((DE) model).isDensity()) {
							style.xLabel = "density";
							style.xMin = 0.0;
							style.xMax = 0.0;
						} else {
							style.xLabel = "frequency";
							style.xMin = 0.0;
							style.xMax = 1.0;
							style.percentX = true;
						}
					} else {
						style.xLabel = "strategy count";
						style.xMin = 0.0;
						style.xMax = nPop;
					}
					style.graphColor = ColorMapCSS.Color2Css(colors[idx]);
					// determine the number of bins with maximum of MAX_BINS
					binSize = (nPop + 1) / HistoGraph.MAX_BINS + 1;
					// doubles as the map for frequencies to bins
					scale2bins = (nPop + 1) / binSize; // number of bins
					if (data == null || data.length != nTraits || data[0].length != (int) scale2bins)
						data = new double[nTraits][(int) scale2bins];
					graph.setData(data);
					break;

				default:
			}
			if (hard)
				graph.reset();
			idx++;
		}
		update(hard);
	}

	@Override
	public void init() {
		super.init();
		for (HistoGraph graph : graphs)
			graph.init();
		update();
	}

	@Override
	public void update(boolean force) {
		if (!isActive && !doStatistics)
			return;

		double newtime = model.getTime();
		if (Math.abs(timestamp - newtime) > 1e-8) {
			timestamp = newtime;

			// new data available - update histograms
			switch (type) {
				case STRATEGY:
					double[][] data = null;
					// cast ok because trait histograms only make sense for continuous models
					Model.Continuous cmodel = (Model.Continuous) model;
					for (HistoGraph graph : graphs) {
						double[][] graphdata = graph.getData();
						if (data != graphdata) {
							data = graphdata;
							cmodel.getTraitHistogramData(graph.getModule().getID(), data);
						}
					}
					break;

				case FITNESS: // same as STRATEGY except for call to retrieve data
					data = null;
					for (HistoGraph graph : graphs) {
						double[][] graphdata = graph.getData();
						if (data != graphdata) {
							data = graphdata;
							model.getFitnessHistogramData(graph.getModule().getID(), data);
						}
					}
					break;

				case DEGREE:
					data = null;
					double min = 0.0, max = 0.0;
					for (HistoGraph graph : graphs) {
						Module module = graph.getModule();
						Geometry inter = null, comp = null;
						switch (model.getModelType()) {
							case ODE:
								graph.displayMessage("ODE model: well-mixed population.");
								continue;
							case SDE:
								graph.displayMessage("SDE model: well-mixed population.");
								continue;
							case PDE:
								inter = comp = module.getGeometry();
								if (inter.isRegular) {
									graph.displayMessage("PDE model: regular structure with degree "
											+ (int) (inter.connectivity + 0.5) + ".");
									continue;
								}
								if (inter.isLattice()) {
									graph.displayMessage("PDE model: lattice structure with degree "
											+ (int) (inter.connectivity + 0.5) +
											(inter.fixedBoundary ? " (fixed" : " (periodic") + " boundaries).");
									continue;
								}
								break;
							case IBS:
								inter = module.getInteractionGeometry();
								comp = module.getCompetitionGeometry();
								break;
							default: // unreachable
						}
						if (!degreeProcessed || (inter != null && inter.isDynamic)
								|| (comp != null && comp.isDynamic)) {
							double[][] graphdata = graph.getData();
							if (data != graphdata && inter != null && comp != null) {
								data = graphdata;
								// find min and max for degree distribution on dynamic graphs
								int bincount;
								if (inter.isUndirected)
									bincount = maxDegree(Math.max(inter.maxOut, comp.maxOut));
								else
									bincount = maxDegree(Math.max(inter.maxTot, comp.maxTot));
								min = 0.0;
								max = bincount;
								bincount = Math.min(bincount, HistoGraph.MAX_BINS);
								if (bincount != data[0].length) {
									data = new double[data.length][bincount];
									graph.setData(data);
								}
								getDegreeHistogramData(data, inter, comp);
							}
							GraphStyle style = graph.getStyle();
							style.xMin = min;
							style.xMax = max;
						}
					}
					break;

				case STATISTICS_FIXATION_PROBABILITY:
					// NOTE: not fully ready for multi-species; info which species fixated missing
					FixationData fixData = ((Statistics) model).getFixationData();
					if (fixData == null)
						break;
					if (!fixData.probRead) {
						HistoGraph graph = graphs.get(fixData.typeFixed);
						graph.addData(fixData.mutantNode);
						fixData.probRead = true;
					}
					// reset timestamp (needed to ensure processing of statistics data)
					timestamp = -1.0;
					break;

				case STATISTICS_FIXATION_TIME:
					// NOTE: not fully ready for multi-species; info which species fixated missing
					fixData = ((Statistics) model).getFixationData();
					if (fixData == null)
						break;
					if (!fixData.timeRead) {
						HistoGraph graph = graphs.get(fixData.typeFixed);
						HistoGraph absorption = graphs.get(graphs.size() - 1);
						int initNode = fixData.mutantNode;
						if (initNode < 0 || doFixtimeDistr(graph.getModule())) {
							graph.addData(fixData.updatesFixed);
							absorption.addData(fixData.updatesFixed);
						} else {
							graph.addData(initNode, fixData.updatesFixed);
							absorption.addData(initNode, fixData.updatesFixed);
						}
						fixData.timeRead = true;
					}
					// reset timestamp (needed to ensure processing of statistics data)
					timestamp = -1.0;
					break;

				case STATISTICS_STATIONARY:
					int nt = model.getNMean();
					double[] state = new double[nt];
					model.getMeanTraits(state);
					int idx = 0;
					for (HistoGraph graph : graphs)
						graph.addData((int) (state[idx++] * scale2bins));
					break;

				default:
					break;
			}
		}
		for (HistoGraph graph : graphs)
			graph.paint(force);
	}

	/**
	 * The flag to indicate whether the view is in statistics mode.
	 */
	private boolean doStatistics = false;

	/**
	 * Check the mode of the view.
	 * 
	 * @return {@code true} if the view is in statistics mode
	 */
	private boolean checkStatistics() {
		doStatistics = false;
		// cast to IBSD is safe because checked first
		if (!(model instanceof Statistics) || !model.permitsMode(Mode.STATISTICS_SAMPLE)
				|| ((Statistics) model).getFixationData() == null) {
			for (HistoGraph graph : graphs)
				graph.displayMessage("Fixation: incompatible settings");
			model.resetStatisticsSample();
		} else {
			for (HistoGraph graph : graphs)
				graph.clearMessage();
			doStatistics = true;
		}
		return doStatistics;
	}

	/**
	 * Helper method to check whether to show the fixation time distribution
	 * or the fixation times for each node.
	 * 
	 * @param module the module of the graph
	 * @return {@code true} to show the fixation time distribution
	 */
	private boolean doFixtimeDistr(Module module) {
		return (module.getNPopulation() > HistoGraph.MAX_BINS || model.getModelType() == Model.Type.SDE);
	}

	@Override
	public String getCounter() {
		if (model.getMode() == Mode.STATISTICS_SAMPLE) {
			return "samples: " + ((Statistics) model).getNStatisticsSamples();
		}
		return super.getCounter();
	}

	/**
	 * The time of the last update.
	 */
	protected double updatetime = -1.0;

	/**
	 * The status of the view.
	 */
	protected String status;

	@Override
	public String getStatus(boolean force) {
		// status calculation are somewhat costly - throttle the max number of updates
		double now = Duration.currentTimeMillis();
		if (!force && now - updatetime < AbstractGraph.MIN_MSEC_BETWEEN_UPDATES)
			return status;
		updatetime = now;

		switch (type) {
			case STATISTICS_FIXATION_PROBABILITY:
				int nSam = Math.max(((Statistics) model).getNStatisticsSamples(), 1);
				status = "Avg. fix. prob: ";
				int idx = 0;
				for (HistoGraph graph : graphs) {
					Module module = graph.getModule();
					if (idx > 0)
						status += ", ";
					status += (isMultispecies ? module.getName() + "." : "") + module.getTraitName(idx++) + ": " +
							Formatter.formatFix(graph.getSamples() / nSam, 3);
				}
				return status;

			case STATISTICS_FIXATION_TIME:
				status = "Avg. fix. time: ";
				idx = 0;
				for (HistoGraph graph : graphs) {
					double[][] data = graph.getData();
					if (data == null) {
						logger.warning("Average fixation times not available!");
						return "";
					}
					Module module = graph.getModule();
					int nTraits = module.getNTraits();
					if (idx > 0)
						status += ", ";
					status += (isMultispecies ? module.getName() + "." : "")
							+ graph.getStyle().label + ": ";
					if (doFixtimeDistr(module)) {
						double mean = Distributions.distrMean(data[idx]);
						double sdev = Distributions.distrStdev(data[idx], mean);
						GraphStyle style = graph.getStyle();
						status += Formatter.formatFix(style.xMin + mean * (style.xMax - style.xMin), 1) + " ± " +
								Formatter.formatFix(sdev * (style.xMax - style.xMin), 1);
						idx++;
						continue;
					}
					double sx = 0.0, sx2 = 0.0, sw = 0.0;
					// note: the +1 accounts for the fact that there is an additional graph for
					// the absorption time.
					double[] dat = data[idx];
					double[] sam = data[idx + nTraits + 1];
					int nDat = dat.length;
					for (int n = 0; n < nDat; n++) {
						double w = sam[n];
						if (w <= 0.0)
							continue;
						double x = dat[n];
						sx += x;
						sx2 += x * x / w;
						sw += w;
					}
					if (sw <= 0.0)
						status += "0.000 ± 0.000";
					else {
						double mean = sx / sw;
						status += Formatter.formatFix(mean, 3) + " ± "
								+ Formatter.formatFix(Math.sqrt(sx2 / sw - mean * mean), 3);
					}
					idx++;
				}
				return status;

			case STATISTICS_STATIONARY:
				if (model.hasConverged()) {
					logger.warning("Non ergodic system - no stationary distribution!");
					return "";
				}
				//$FALL-THROUGH$
			default:
				return super.getStatus(force);
		}
	}

	/**
	 * Calculate the maximum degree for displaying the degree distribution. This
	 * rounds {@code max} up to {@code 10, 20, 50, 100, 200, 500, 1000} etc.
	 * 
	 * @param max the maximum degree of the graph
	 * @return the maximum degree to display
	 */
	private static int maxDegree(int max) {
		// determine range and number of bins required
		// range starts at 1 up to 10, 20, 50, 100, 200, 500, 1000 etc.
		// the number of bins is at most 100
		int order = (int) Math.max(10, Combinatorics.pow(10, (int) Math.floor(Math.log10(max + 1))));
		switch (max / order) {
			default:
			case 0:
				return order;
			case 1:
				return 2 * order;
			case 2:
			case 3:
			case 4:
				return 5 * order;
			case 5:
			case 6:
			case 7:
			case 8:
			case 9:
				return 10 * order;
		}
	}

	/**
	 * Generate the degree histogram data for the given interaction and competition
	 * geometries.
	 * 
	 * @param data  the data array for storing the histograms
	 * @param inter the interaction graph
	 * @param comp  the competition graph
	 */
	private void getDegreeHistogramData(double[][] data, Geometry inter, Geometry comp) {
		int kmax = maxDegree(inter.isUndirected ? inter.maxOut : inter.maxTot);
		if (inter != comp)
			kmax = Math.max(kmax, maxDegree(comp.isUndirected ? comp.maxOut : comp.maxTot));
		double ibinwidth = (double) data[0].length / (kmax + 1);
		getDegreeHistogramData(data, inter, 0, ibinwidth);
		if (inter != comp)
			getDegreeHistogramData(data, comp, inter.isUndirected ? 1 : 3, ibinwidth);
		degreeProcessed = true;
	}

	/**
	 * Generate the degree histogram data for the given geometry.
	 * 
	 * @param data      the data array for storing the histograms
	 * @param geometry  the interaction graph
	 * @param idx       the index for placing the histogram data
	 * @param ibinwidth the scaling factor to map degrees to bins
	 */
	private void getDegreeHistogramData(double[][] data, Geometry geometry, int idx, double ibinwidth) {
		if (geometry.isUndirected) {
			double[] dataio = data[idx];
			Arrays.fill(dataio, 0.0);
			for (int i = 0; i < geometry.size; i++)
				dataio[(int) (geometry.kin[i] * ibinwidth)]++;
			ArrayMath.multiply(dataio, 1.0 / geometry.size);
			return;
		}
		double[] datao = data[idx];
		double[] datai = data[idx + 1];
		double[] datat = data[idx + 2];
		Arrays.fill(datao, 0.0);
		Arrays.fill(datai, 0.0);
		Arrays.fill(datat, 0.0);
		for (int i = 0; i < geometry.size; i++) {
			int kin = geometry.kin[i];
			int kout = geometry.kout[i];
			datao[(int) (kout * ibinwidth)]++;
			datai[(int) (kin * ibinwidth)]++;
			datat[(int) ((kin + kout) * ibinwidth)]++;
		}
		double norm = 1.0 / geometry.size;
		ArrayMath.multiply(datao, norm);
		ArrayMath.multiply(datai, norm);
		ArrayMath.multiply(datat, norm);
	}

	/**
	 * Determine the number of histograms required for the degree distributions of
	 * the interaction and competition geometries.
	 * 
	 * @param inter the interaction geometry
	 * @param comp  the competition geometry
	 * @return the number of histograms required
	 */
	private int getDegreeGraphs(Geometry inter, Geometry comp) {
		if (inter == null)
			return 1;
		int nGraphs = 1;
		if (!inter.isUndirected)
			nGraphs += 2;
		if (comp != inter) {
			nGraphs += 1;
			if (!comp.isUndirected)
				nGraphs += 2;
		}
		return nGraphs;
	}

	/**
	 * Determine the number of bins required for the degree distributions of the
	 * interaction and competition geometries.
	 * 
	 * @param inter the interaction geometry
	 * @param comp  the competition geometry
	 * @return the number of histograms required
	 */
	private int getDegreeBins(Geometry inter, Geometry comp) {
		if (inter == null)
			return 0;
		int nBins = maxDegree(inter.maxOut);
		if (!inter.isUndirected)
			nBins = Math.max(maxDegree(inter.maxTot) + 1, nBins);
		if (comp != inter)
			nBins = Math.max(maxDegree(comp.maxOut) + 1, nBins);
		if (!comp.isUndirected)
			nBins = Math.max(maxDegree(comp.maxTot) + 1, nBins);
		return Math.max(2, Math.min(nBins, HistoGraph.MAX_BINS));
	}

	/**
	 * Get the labels for the degree distributions of the interaction and
	 * competition geometries.
	 * 
	 * @param nTraits         the number of traits
	 * @param interUndirected {@code true} if the interaction graph is undirected
	 * @return the labels for the degree distributions
	 */
	private String[] getDegreeLabels(int nTraits, boolean interUndirected) {
		switch (nTraits) {
			case 1:
				// single graph, undirected
				return new String[] { null };
			case 2:
				// separate graphs, undirected
				return new String[] { "interaction", "competition" };
			case 3:
				// single directed graph
				return new String[] { "in", "out", "total" };
			case 4:
				// separate graphs, one directed the other undirected
				if (interUndirected)
					return new String[] { "interaction", "competition - in", "competition - out",
							"competition - total" };
				return new String[] { "interaction - in", "interaction - out", "interaction - total", "competition" };
			case 6:
				return new String[] { "interaction - in", "interaction - out", "interaction - total",
						"competition - in", "competition - out", "competition - total" };
			default:
				// should not happen...
				throw new IllegalArgumentException("unvalid number of degree distributions");
		}
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

	/**
	 * The clear context menu.
	 */
	private ContextMenuItem clearMenu;

	@Override
	public void populateContextMenu(ContextMenu menu) {
		if (type == Model.Data.STATISTICS_STATIONARY) {
			// add menu to clear canvas
			if (clearMenu == null) {
				clearMenu = new ContextMenuItem("Clear", new Command() {
					@Override
					public void execute() {
						for (HistoGraph graph : graphs)
							graph.clearData();
						timestamp = -1.0;
						update();
					}
				});
			}
			menu.addSeparator();
			menu.add(clearMenu);
		} else {
			clearMenu = null;
		}
		super.populateContextMenu(menu);
	}

	@Override
	protected ExportType[] exportTypes() {
		switch (type) {
			case STATISTICS_FIXATION_PROBABILITY:
			case STATISTICS_FIXATION_TIME:
				return new ExportType[] { ExportType.SVG, ExportType.PNG, ExportType.STAT_DATA };
			case DEGREE:
			default:
				return new ExportType[] { ExportType.SVG, ExportType.PNG };
		}
	}

	@SuppressWarnings("null")
	@Override
	protected void exportStatData() {
		// NOTE: consider exporting more data (e.g. overall fix probs/times, sdev,
		// sample count); initial configuration for statistics, structure
		String export;
		switch (type) {
			case STATISTICS_FIXATION_PROBABILITY:
				export = "# fixation probability\n";
				break;
			case STATISTICS_FIXATION_TIME:
				export = "# fixation time\n";
				break;
			default:
				return;
		}
		Module module = null;
		int idx = 0;
		for (HistoGraph graph : graphs) {
			Module newMod = graph.getModule();
			if (module != newMod) {
				module = newMod;
				if (isMultispecies)
					export += "# species: " + module.getName() + "\n";
			}
			export += "# trait: " + model.getMeanName(idx++) + "\n";
			double[][] data = graph.getData();
			if (data == null) {
				export += "# no data available\n";
				continue;
			}
			export += "# node,\tdata\n";
			int nData = data[idx].length;
			for (int n = 0; n < nData; n++)
				export += graph.bin2x(n) + ",\t" + graph.getData(n) + "\n";
		}
		EvoLudoWeb._export("data:text/csv;base64," + EvoLudoWeb.b64encode(export), "evoludo_stat.csv");
	}
}
