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
import java.util.Set;

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
import org.evoludo.simulator.models.IBSD;
import org.evoludo.simulator.models.IBSD.FixationData;
import org.evoludo.simulator.models.Model;
import org.evoludo.simulator.models.Model.Mode;
import org.evoludo.simulator.modules.Continuous;
import org.evoludo.simulator.modules.Discrete;
import org.evoludo.simulator.modules.Module;
import org.evoludo.util.Formatter;

import com.google.gwt.core.client.Duration;

/**
 *
 * @author Christoph Hauert
 */
public class Histogram extends AbstractView implements HistoGraph.HistoGraphController {

	// NOTE: this is a bit of a hack that allows us to use graphs as Set<HistoGraph> here
	//		 but as Set<AbstractGraph> in super classes. saves a lot of ugly casting
	@SuppressWarnings("hiding")
	protected Set<HistoGraph> graphs;

	public static final int FIXATION_NODES = 144;
	protected int nSamples = -1;

	/**
	 * Flag to indicate whether the model entertains multiple species, i.e.
	 * {@code nSpecies&gt;1}. Convenience field. Reduces calls to {@link Module}.
	 */
	protected boolean isMultispecies;
	protected boolean degreeProcessed;

	@SuppressWarnings("unchecked")
	public Histogram(EvoLudoGWT engine, Model.Data type) {
		super(engine, type);
		graphs = (Set<HistoGraph>) super.graphs;
	}

	@Override
	public String getName() {
		switch( type ) {
			case STRATEGY:
				return "Strategies - Histogram";
			case FITNESS:
				return "Fitness - Histogram";
			case DEGREE:
				return "Structure - Degree";
			case STATISTICS_FIXATION_PROBABILITY:
				return "Statistics - Fixation probability";
			case STATISTICS_FIXATION_TIME:
				return "Statistics - Fixation time";
			default:
				return null;
		}
	}

	@Override
	public void activate() {
		model.setMode(type.getMode());
		super.activate();
	}

	@Override
	public void deactivate() {
		// revert to default of dynamics mode
		model.setMode(Mode.DYNAMICS);
		super.deactivate();
	}

	@Override
	public void reset(boolean soft) {
		super.reset(soft);
		ArrayList<? extends Module> species = engine.getModule().getSpecies();
		isMultispecies = (species.size() > 1);
		degreeProcessed = false;
		int nGraphs = 0;
		nSamples = 0;
		for( Module pop : species ) {
			int nTraits = pop.getNTraits();
			switch( type ) {
				case STRATEGY:
					// this only makes sense for continuous traits
					nGraphs += nTraits;
					break;
				case FITNESS:
					if( model.isContinuous() )
						nGraphs++;
					else {
						nGraphs += nTraits;
						if( pop.getVacant()>=0 )
							nGraphs--;
					}
					break;
				case DEGREE:
					nGraphs += getDegreeGraphs(pop.getInteractionGeometry(), pop.getReproductionGeometry());
					break;
				case STATISTICS_FIXATION_PROBABILITY:
					nGraphs += nTraits;
					break;
				case STATISTICS_FIXATION_TIME:
					nGraphs += nTraits+1;
					break;
				default:
					nGraphs = Integer.MIN_VALUE;
			}
		}

		if( graphs.size()!=nGraphs ) {
			int nXLabels = 0;
			soft = false;
			destroyGraphs();
			for( Module module : species ) {
				int nTraits = module.getNTraits();
				switch( type ) {
					case STRATEGY:
						// this only makes sense for continuous traits
						for( int n=0; n<nTraits; n++ ) {
							HistoGraph graph = new HistoGraph(this, module, n);
							wrapper.add(graph);
							graphs2mods.put(graph, module);
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
						nTraits = (model.isContinuous()?1:nTraits);
						int vacant = module.getVacant();
						int paneIdx = 0;
						int bottomPaneIdx = nTraits-1;
						if( vacant>=0 && vacant==nTraits-1 )
							bottomPaneIdx--;
						for( int n=0; n<nTraits; n++ ) {
							if( n==vacant )
								continue;
							HistoGraph graph = new HistoGraph(this, module, paneIdx++);
							boolean bottomPane = (n==bottomPaneIdx);
							wrapper.add(graph);
							graphs2mods.put(graph, module);
							AbstractGraph.GraphStyle style = graph.getStyle();
							// fixed style attributes
							if (model instanceof Model.DE && ((Model.DE) model).isDensity()) {
								style.yLabel = "density";
								style.percentY = false;
								style.yMin = 0.0;
								style.yMax = 0.0;
							}
							else {
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
							style.showXLabel = bottomPane;	// show only on bottom panel
							style.showXTickLabels = bottomPane;
							if( model.isContinuous() )
								style.showLabel = isMultispecies;
							else
								style.showLabel = true;
							if (bottomPane)
								nXLabels++;
						}
						break;

					case DEGREE:
						Model.Type mType = model.getModelType();
						if( mType==Model.Type.ODE || mType==Model.Type.SDE ) {
							// happens for ODE/SDE/PDE - do not show distribution
							HistoGraph graph = new HistoGraph(this, module, 0); 
							wrapper.add(graph);
							graphs2mods.put(graph, module);
							break;
						}
						nTraits = getDegreeGraphs(module.getInteractionGeometry(), module.getReproductionGeometry());
						Geometry inter = (mType == Model.Type.PDE ? module.getGeometry() : module.getInteractionGeometry());
						String[] labels = getDegreeLabels(nTraits, inter.isUndirected);
						for( int n=0; n<nTraits; n++ ) {
							HistoGraph graph = new HistoGraph(this, module, n);
							boolean bottomPane = (n==nTraits-1);
							wrapper.add(graph);
							graphs2mods.put(graph, module);
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
							int tag = graph.getTag();
							style.xLabel = "degree";
							style.showXLabel = false;
							style.showXTickLabels = false;
							style.label = labels[tag];
							style.showXLabel = bottomPane;
							style.showXTickLabels = bottomPane;
							style.graphColor = "#444";
							if (bottomPane)
								nXLabels++;
						}
						break;

					case STATISTICS_FIXATION_PROBABILITY:
						for( int n=0; n<nTraits; n++ ) {
							HistoGraph graph = new HistoGraph(this, module, n); 
							boolean bottomPane = (n==nTraits-1);
							graph.setNormalized(nTraits);
							wrapper.add(graph);
							graphs2mods.put(graph, module);
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
							style.showXLabel = bottomPane;	// show only on bottom panel
							style.showXTickLabels = bottomPane;
							style.customYLevels = ((HasHistogram)module).getCustomLevels(type, n);
							if (bottomPane)
								nXLabels++;
						}
						break;

					case STATISTICS_FIXATION_TIME:
						nTraits++;	// the last 'trait' is for unconditional absorption times
						for( int n=0; n<nTraits; n++ ) {
							HistoGraph graph = new HistoGraph(this, module, n); 
							boolean bottomPane = (n==nTraits-1);
							wrapper.add(graph);
							graphs2mods.put(graph, module);
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
							style.showXLabel = bottomPane;	// show only on bottom panel
							style.autoscaleY = true;
							if (bottomPane)
								nXLabels++;
						}
						break;

					default:
				}
			}
			// arrange histograms vertically
			gRows = nGraphs;
			int width = 100/gCols;
			int xaxisdeco = 7;	// estimate of height of x-axis decorations in %
								// unfortunately GWT chokes on CSS calc() 
			int height = (100-nXLabels*xaxisdeco)/gRows;
			for( HistoGraph graph : graphs )
				graph.setSize(width+"%", height+(graph.getStyle().showXLabel?xaxisdeco:0)+"%");
		}
		Module pop = null;
		boolean newPop = false;
		double[][] data = null;
		for( HistoGraph graph : graphs) {
			AbstractGraph.GraphStyle style = graph.getStyle();
			Module oldpop = pop;
			pop = graph.getModule();
			newPop = (oldpop != pop);
			int tag = graph.getTag();
			int vacant = pop.getVacant();
			int nTraits = pop.getNTraits();
			Color[] colors = pop.getTraitColors();

			switch( type ) {
				case STRATEGY:
					// histogram of strategies makes only sense for continuous traits
					if( newPop || data==null || data.length!=nTraits || data[0].length!=HistoGraph.MAX_BINS ) 
						data = new double[nTraits][HistoGraph.MAX_BINS];
					graph.setData(data);
					graph.clearMarkers();
					ArrayList<double[]> markers = pop.getMarkers();
					if (markers != null) {
						for (double[] mark : markers)
							graph.addMarker(mark[tag + 1], ColorMapCSS.Color2Css(colors[tag]), null,
									mark[0] > 0.0 ? style.dashedLine : style.dottedLine);
					}
					Continuous cmod = (Continuous) pop;
					double min = cmod.getTraitMin()[tag];
					double max = cmod.getTraitMax()[tag];
					if( Math.abs(min-style.xMin)>1e-6 || Math.abs(max-style.xMax)>1e-6 ) {
						style.xMin = min;
						style.xMax = max;
						soft = false;
					}
					style.yMin = 0.0;
					style.yMax = 1.0;
					style.xLabel = pop.getTraitName(tag);
					style.graphColor = ColorMapCSS.Color2Css(colors[tag]);
					if( isMultispecies )
						style.label = pop.getName()+": "+pop.getTraitName(tag);
					break;

				case FITNESS:
					graph.clearMarkers();
					nTraits = (model.isContinuous()?1:nTraits);
					if( vacant>=0 )
						nTraits--;
					if( newPop || data==null || data.length!=nTraits || data[0].length!=HistoGraph.MAX_BINS ) 
						data = new double[nTraits][HistoGraph.MAX_BINS];
					graph.setData(data);
					min = pop.getMinScore();
					max = pop.getMaxScore();
					if( Math.abs(min-style.xMin)>1e-6 || Math.abs(max-style.xMax)>1e-6 ) {
						style.xMin = min;
						style.xMax = max;
						soft = false;
					}
					style.yMin = 0.0;
					style.yMax = 1.0;
					if( pop instanceof Discrete ) {
						Discrete dmod = (Discrete)pop;
						style.label = (isMultispecies?pop.getName() + ": " : "") + pop.getTraitName(tag);
						Color tColor = colors[tag];
						style.graphColor = ColorMapCSS.Color2Css(tColor);
						double mono = dmod.getMonoScore(tag);
						if (Double.isNaN(mono))
							continue;
						graph.addMarker(mono,
								ColorMapCSS.Color2Css(ColorMap.blendColors(tColor, Color.WHITE, 0.5)),
								"monomorphic payoff");
						break;
					}
					if( pop instanceof Continuous ) {
						Color tcolor = colors[tag];
						graph.addMarker(pop.getMinMonoScore(),
								ColorMapCSS.Color2Css(ColorMap.blendColors(tcolor, Color.BLACK, 0.5)),
								"minimum monomorphic payoff");
						graph.addMarker(pop.getMaxMonoScore(),
								ColorMapCSS.Color2Css(ColorMap.blendColors(tcolor, Color.WHITE, 0.5)),
								"maximum monomorphic payoff");
						style.graphColor = ColorMapCSS.Color2Css(Color.BLACK);
						break;
					}
					// unknown population type - do not mark any fitness values
					break;

				case DEGREE:
					Model.Type mType = model.getModelType();
					if( mType==Model.Type.ODE || mType==Model.Type.SDE ) {
						graph.setData(null);
						break;
					}
					Geometry inter, repro;
					if( mType==Model.Type.PDE ) {
						inter = repro = pop.getGeometry();
					}
					else {
						inter = pop.getInteractionGeometry();
						repro = pop.getReproductionGeometry();
					}
					if( newPop )
						data = new double[getDegreeGraphs(inter, repro)][getDegreeBins(inter, repro)];
					graph.setData(data);
					style.yMin = 0.0;
					style.yMax = 1.0;
					style.xMin = 0.0;
					if( inter.isUndirected )
						style.xMax = maxDegree(Math.max(inter.maxOut, repro.maxOut));
					else
						style.xMax = maxDegree(Math.max(inter.maxTot, repro.maxTot));
					break;

				case STATISTICS_FIXATION_PROBABILITY:
					newPop &= model.permitsMode(Mode.STATISTICS);
					int nBins = pop.getNPopulation();
					style.yMin = 0.0;
					style.yMax = 1.0;
					style.xMin = 0;
					style.xMax = nBins-1;
					style.label = pop.getTraitName(tag);
					style.graphColor = ColorMapCSS.Color2Css(colors[tag]);
					if( newPop )
						data = new double[nTraits+1][Math.min(nBins, FIXATION_NODES)];
					graph.setData(data);
					break;

				case STATISTICS_FIXATION_TIME:
					newPop &= model.permitsMode(Mode.STATISTICS);
					int nPop = pop.getNPopulation();
					nBins = nPop;
					nTraits++;	// the last 'trait' is for unconditional absorption times
					style.yMin = 0.0;
					style.yMax = 1.0;
					if( tag<nTraits-1 ) {
						style.label = pop.getTraitName(tag);
						style.graphColor = ColorMapCSS.Color2Css(colors[tag]);
					}
					else {
						style.label = "Absorbtion";
						style.graphColor = ColorMapCSS.Color2Css(Color.BLACK);
					}
					if( nPop>FIXATION_NODES ) {
						if( newPop )
							data = new double[nTraits][HistoGraph.MAX_BINS];
						graph.setNormalized(false);
						graph.setNormalized(-1);
						style.xMin = 0.0;
						style.xMax = Functions.roundUp(nPop/4);
						style.xLabel = "fixation time";
						style.showXTickLabels = true;
						style.yLabel = "probability";
						style.percentY = true;
						graph.enableAutoscaleYMenu(true);
						style.customYLevels = null;
					}
					else {
						if( newPop )
							data = new double[2*nTraits][nPop];
						graph.setNormalized(tag+nTraits);
						style.xMin = 0.0;
						style.xMax = nPop-1;
						style.xLabel = "node";
						style.showXTickLabels = (tag==(nTraits-1));
						style.yLabel = "time";
						style.percentY = false;
						graph.enableAutoscaleYMenu(false);
						style.customYLevels = ((HasHistogram)pop).getCustomLevels(type, tag);
					}
					graph.setData(data);
					break;

				default:
			}
			if( !soft )
				graph.reset();
		}
		update();
	}

	@Override
	public void init() {
		super.init();
		for( HistoGraph graph : graphs)
			graph.init();
		update();
	}

	@Override
	public void update(boolean force) {
		boolean doStatistics = type.isStatistics();

		if( !isActive && !doStatistics )
			return;

		double newtime = model.getTime();
		if( Math.abs(timestamp-newtime)<1e-8 ) {
			for( HistoGraph graph : graphs)
				graph.paint();
			return;
		}
		switch( type ) {
			case STRATEGY:
				double[][] data = null;
				// cast ok because trait histograms only make sense for continuous models
				Model.ContinuousIBS cmodel = (Model.ContinuousIBS)model;
				for( HistoGraph graph : graphs) {
					double[][] graphdata = graph.getData();
					if( data!=graphdata ) {
						data = graphdata;
						cmodel.getTraitHistogramData(graph.getTag(), data);
					}
					graph.paint();
				}
				timestamp = newtime;
				return;

			case FITNESS:	// same as STRATEGY except for call to retrieve data
				data = null;
				for( HistoGraph graph : graphs) {
					double[][] graphdata = graph.getData();
					if( data!=graphdata ) {
						data = graphdata;
						model.getFitnessHistogramData(graph.getTag(), data);
					}
					graph.paint();
				}
				timestamp = newtime;
				return;

			case DEGREE:
				data = null;
				double min = 0.0, max = 0.0;
				for( HistoGraph graph : graphs) {
					Module module = graph.getModule();
					Geometry inter = null, repro = null;
					switch( model.getModelType() ) {
						case ODE:
							graph.displayMessage("ODE model: well-mixed population.");
							continue;
						case SDE:
							graph.displayMessage("SDE model: well-mixed population.");
							continue;
						case PDE:
							inter = repro = module.getGeometry();
							if (inter.isRegular) {
								graph.displayMessage("PDE model: regular structure with degree "+(int)(inter.connectivity+0.5)+".");								
								continue;
							}
							if (inter.isLattice()) {
								graph.displayMessage("PDE model: lattice structure with degree "+(int)(inter.connectivity+0.5)+
										(inter.fixedBoundary?" (fixed":" (periodic")+" boundaries).");								
								continue;
							}
							break;
						case IBS:
							inter = module.getInteractionGeometry();
							repro = module.getReproductionGeometry();
							break;
						default: // unreachable
					}
					if (!degreeProcessed || (inter != null && inter.isDynamic) || (repro != null && repro.isDynamic)) {
						double[][] graphdata = graph.getData();
						if (data != graphdata && inter != null && repro != null) {
							data = graphdata;
							// find min and max for degree distribution on dynamic graphs
							int bincount;
							if (inter.isUndirected)
								bincount = maxDegree(Math.max(inter.maxOut, repro.maxOut));
							else
								bincount = maxDegree(Math.max(inter.maxTot, repro.maxTot));
							min = 0.0;
							max = bincount;
							bincount = Math.min(bincount, HistoGraph.MAX_BINS);
							if (bincount != data[0].length) {
								data = new double[data.length][bincount];
								graph.setData(data);
							}
							getDegreeHistogramData(data, inter, repro);
						}
						GraphStyle style = graph.getStyle();
						style.xMin = min;
						style.xMax = max;
					}
					graph.paint();
				}
				timestamp = newtime;
				return;

			case STATISTICS_FIXATION_PROBABILITY:
//NOTE: not fully ready for multi-species; info which species fixated missing
				Module pop = null;
				FixationData fixData = null;
				boolean doStat = model.permitsMode(Mode.STATISTICS);
				for( HistoGraph graph : graphs) {
					if (!doStat || !(model instanceof IBSD)) {
						graph.displayMessage("Statistics mode not available");
						continue;
					}
					Module newPop = graph.getModule();
					if (newPop != pop ) {
						pop = newPop;
						// cast safe - checked above
						fixData = ((IBSD)model).getFixationData();
						if (fixData == null) {
							graph.displayMessage("Statistics mode not available");
							continue;
						}
					}
					if( fixData != null && !fixData.probRead ) {
						if( graph.getTag()==fixData.typeFixed ) {
							graph.addData(fixData.mutantNode);
							nSamples++;
							fixData.probRead = true;
						}
					}
					graph.paint();
				}
				// reset timestamp (needed to ensure processing of statistics data)
				timestamp = -1.0;
				return;

			case STATISTICS_FIXATION_TIME:
//NOTE: not fully ready for multi-species; info which species fixated missing
				data = null;
				fixData = null;
				int nPop = -1;
				pop = null;
				int nTrait = -1;
				doStat = model.permitsMode(Mode.STATISTICS);
				for( HistoGraph graph : graphs) {
					if (!doStat || !(model instanceof IBSD)) {
						graph.displayMessage("Statistics mode not available");
						continue;
					}
					Module newPop = graph.getModule();
					if (newPop != pop ) {
						pop = newPop;
						// cast safe - checked above
						fixData = ((IBSD)model).getFixationData();
						if (fixData == null) {
							graph.displayMessage("Statistics mode not available");
							continue;
						}
						nPop = pop.getNPopulation();
						nTrait = pop.getNTraits();
					}
					if( fixData != null && !fixData.timeRead ) {
						int iNode = fixData.mutantNode;
						if( graph.getTag()==fixData.typeFixed ) {
							if( iNode<0 || nPop>FIXATION_NODES )
								graph.addData(fixData.updatesFixed);
							else
								graph.addData(iNode, fixData.updatesFixed);
						}
						else if( graph.getTag()==nTrait ) {
							if( iNode<0 || nPop>FIXATION_NODES )
								graph.addData(fixData.updatesFixed);
							else
								graph.addData(iNode, fixData.updatesFixed);
							nSamples++;
							fixData.timeRead = true;
						}
					}
					graph.paint();
				}
				// reset timestamp (needed to ensure processing of statistics data)
				timestamp = -1.0;
				return;
	
			default:
				break;
		}
	}

	@Override
	public String getCounter() {
		if( model.isMode(Mode.STATISTICS) ) {
			return "samples: "+nSamples;
		}
		return super.getCounter();
	}

	protected static final int MIN_MSEC_BETWEEN_UPDATES = 100;	// max 10 updates per second
	protected double updatetime = -1.0;
	protected String status;

	@Override
	public String getStatus(boolean force) {
		// status calculation are somewhat costly - throttle the max number of updates
		double now = Duration.currentTimeMillis();
		if( !force && now-updatetime<MIN_MSEC_BETWEEN_UPDATES ) 
			return status;
		updatetime = now;
		Module pop = null;
		int nTraits, nSam;

		switch( type ) {
			case STATISTICS_FIXATION_PROBABILITY:
				nSam = Math.max(nSamples, 1);
				status = "Avg. fix. prob: ";
				for( HistoGraph graph : graphs ) {
					pop = graph.getModule();
					status += (isMultispecies?pop.getName()+".":"")+pop.getTraitName(graph.getTag())+": "+
							Formatter.formatFix(graph.getSamples()/nSam, 3)+", ";
				}
				return status;

			case STATISTICS_FIXATION_TIME:
				status = "Avg. fix. time: ";
				for( HistoGraph graph : graphs ) {
					double[][] data = graph.getData();
					if( data==null )
						return "statistics unavailable";
					pop = graph.getModule();
					int tag = graph.getTag();
					nTraits = pop.getNTraits();
					status += (isMultispecies?pop.getName()+".":"")+(tag==nTraits?"Absorption":pop.getTraitName(tag))+": ";
					int nPop = pop.getNPopulation();
					if( nPop>FIXATION_NODES ) {
						double mean = Distributions.distrMean(data[tag]);
						double sdev = Distributions.distrStdev(data[tag], mean);
						GraphStyle style = graph.getStyle();
						status += Formatter.formatFix(style.xMin+mean*(style.xMax-style.xMin), 1)+" ± "+
								Formatter.formatFix(sdev*(style.xMax-style.xMin), 1)+", ";
						continue;
					}
					double sx = 0.0, sx2 = 0.0, sw = 0.0;
					// note: the +1 accounts for the fact that there is an additional graph for
					//		 the absorption time.
					double[] dat = data[tag], sam = data[nTraits+1+tag];
					int nDat = dat.length;
					for( int n=0; n<nDat; n++ ) {
						double w = sam[n];
						if( w<=0.0 ) continue;
						double x = dat[n];
						sx += x;
						sx2 += x*x/w;
						sw += w;
					}
					if( sw<=0.0 ) status += "0.000 ± 0.000, ";
					else {
						double mean = sx/sw;
						status += Formatter.formatFix(mean, 3)+" ± "+Formatter.formatFix(Math.sqrt(sx2/sw-mean*mean), 3)+", ";
					}
				}
				return status;

			default:
				return super.getStatus(force);
		}
	}

	private static int maxDegree(int max) {
		// determine range and number of bins required
		// range starts at 1 up to 10, 20, 50, 100, 200, 500, 1000 etc.
		// the number of bins is at most 100
		int order = (int) Math.max(10, Combinatorics.pow(10, (int)Math.floor(Math.log10(max+1))));
		switch( max/order ) {
			default:
			case 0:
				return order;
			case 1:
				return 2*order;
			case 2:
			case 3:
			case 4:
				return 5*order;
			case 5:
			case 6:
			case 7:
			case 8:
			case 9:
				return 10*order;
		}
	}

	private void getDegreeHistogramData(double[][] data, Geometry inter, Geometry repro) {
		int kmax = maxDegree(inter.isUndirected?inter.maxOut:inter.maxTot);
		if( inter!=repro )
			kmax = Math.max(kmax, maxDegree(repro.isUndirected?repro.maxOut:repro.maxTot));
		double ibinwidth = (double)data[0].length/(kmax+1);
		getDegreeHistogramData(data, inter, 0, ibinwidth);
		if( inter!=repro )
			getDegreeHistogramData(data, repro, inter.isUndirected?1:3, ibinwidth);
		degreeProcessed = true;
	}

	private void getDegreeHistogramData(double[][] data, Geometry geometry, int idx, double ibinwidth) {
		if( geometry.isUndirected ) {
			double[] dataio = data[idx];
			Arrays.fill(dataio, 0.0);
			for( int i=0; i<geometry.size; i++ )
				dataio[(int)(geometry.kin[i]*ibinwidth)]++;
			ArrayMath.multiply(dataio, 1.0/geometry.size);
			return;
		}
		double[] datao = data[idx];
		double[] datai = data[idx+1];
		double[] datat = data[idx+2];
		Arrays.fill(datao, 0.0);
		Arrays.fill(datai, 0.0);
		Arrays.fill(datat, 0.0);
		for( int i=0; i<geometry.size; i++ ) {
			int kin = geometry.kin[i];
			int kout = geometry.kout[i];
			datao[(int)(kout*ibinwidth)]++;
			datai[(int)(kin*ibinwidth)]++;
			datat[(int)((kin+kout)*ibinwidth)]++;
		}
		double norm = 1.0/geometry.size;
		ArrayMath.multiply(datao, norm);
		ArrayMath.multiply(datai, norm);
		ArrayMath.multiply(datat, norm);
	}

	private int getDegreeGraphs(Geometry inter, Geometry repro) {
		if( inter==null )
			return 1;
		int nGraphs = 1;
		if( !inter.isUndirected )
			nGraphs += 2;
		if( repro!=inter ) {
			nGraphs += 1;
			if( !repro.isUndirected )
				nGraphs += 2;
		}
		return nGraphs;
	}

	private int getDegreeBins(Geometry inter, Geometry repro) {
		if( inter==null )
			return 0;
		int nBins = maxDegree(inter.maxOut);
		if( !inter.isUndirected )
			nBins = Math.max(maxDegree(inter.maxTot)+1, nBins);
		if( repro!=inter )
			nBins = Math.max(maxDegree(repro.maxOut)+1, nBins);
		if( !repro.isUndirected )
			nBins = Math.max(maxDegree(repro.maxTot)+1, nBins);
		return Math.max(2,  Math.min(nBins, HistoGraph.MAX_BINS));
	}

	private String[] getDegreeLabels(int nTraits, boolean interUndirected) {
		switch( nTraits ) {
			case 1:
				// single graph, undirected
				return new String[] { null };
			case 2:
				// separate graphs, undirected
				return new String[] { "interaction", "reproduction" };
			case 3:
				// single directed graph
				return new String[] { "in", "out", "total" };
			case 4:
				// separate graphs, one directed the other undirected
				if( interUndirected )
					return new String[] { "interaction", "reproduction - in", "reproduction - out", "reproduction - total" };
				return new String[] { "interaction - in", "interaction - out", "interaction - total", "reproduction" };
			case 6:
				return new String[] { "interaction - in", "interaction - out", "interaction - total", 
						"reproduction - in", "reproduction - out", "reproduction - total" };
			default:
				// should not happen...
				throw new IllegalArgumentException("unvalid number of degree distributions");
		}
	}

/*	@Override
	public boolean	verifyMarkedBins(HistoFrameLayer frame, int tag) {
		int nMarked = population.getMonoScoreCount();
		if( nMarked==0 ) return false;
		Color[] colors = getColors(tag);
		if( population.isContinuous() ) {
			// for continuous strategies we have a single histogram and may want to mark several bins
			boolean changed = false;
			for( int n=0; n<nMarked; n++ ) {
				changed |= frame.updateMarkedBin(n, 
					population.getMonoScore(n), 
					new Color(Math.max(colors[n].getRed(), 127), 
						Math.max(colors[n].getGreen(), 127), 
						Math.max(colors[n].getBlue(), 127)));
			}
			return changed;
		}
		// for discrete strategies we have different histograms and mark only a single bin
		return frame.updateMarkedBin(0, 
				population.getMonoScore(tag), 
				new Color(Math.max(colors[tag].getRed(), 127), 
						Math.max(colors[tag].getGreen(), 127), 
						Math.max(colors[tag].getBlue(), 127)));
	}*/

	@Override
	public String getTooltipAt(HistoGraph graph, int bar) {
		int n = graph.getTag();
		double[][] data = graph.getData();
		int nBins = data[0].length;
		GraphStyle style = graph.getStyle();
		// note label is null for undirected graph with the same interaction and reproduction graphs
		StringBuilder tip = new StringBuilder(style.showLabel&&style.label!=null?"<b>"+style.label+"</b><br/>":"");
		switch( type ) {
			case DEGREE:
				if( Math.abs(style.xMax-(nBins-1))<1e-6 ) {
					tip.append("<table style='border-collapse:collapse;border-spacing:0;'>");
					tip.append("<tr><td><i>"+style.xLabel+":</i></td><td>"+bar+"</td></tr>");
					tip.append("<tr><td><i>"+style.yLabel+":</i></td><td>"+Formatter.formatPercent(data[n][bar], 2)+"</td></tr></table>");
					break;
				}
				//$FALL-THROUGH$
			case STRATEGY:
			case FITNESS:
				tip.append("<table style='border-collapse:collapse;border-spacing:0;'>");
				tip.append("<tr><td><i>"+style.xLabel+":</i></td><td>["+Formatter.format(style.xMin+bar*(style.xMax-style.xMin)/nBins, 2)+
						", "+Formatter.format(style.xMin+(bar+1)*(style.xMax-style.xMin)/nBins, 2)+")</td></tr>");
				tip.append("<tr><td><i>"+style.yLabel+":</i></td><td>"+Formatter.formatPercent(data[n][bar], 2)+"</td></tr>");
				String note = graph.getNoteAt(bar);
				if( note!=null ) tip.append("<tr><td><i>Note:</i></td><td>"+note+"</td></tr>");
				tip.append("</table>");
				break;
			case STATISTICS_FIXATION_PROBABILITY:
				tip.append("<table style='border-collapse:collapse;border-spacing:0;'>");
				tip.append("<tr><td><i>"+style.xLabel+":</i></td><td>"+bar+"</td></tr>");
				int nTraits = data.length-1;
				double norm = data[nTraits][bar];
				tip.append("<tr><td><i>samples:</i></td><td>"+(int)norm+"</td></tr>");
				if( style.percentY )
					tip.append("<tr><td><i>"+style.yLabel+":</i></td><td>"+(norm>0.0?Formatter.formatPercent(data[n][bar]/norm, 2):"0")+
							"</td></tr></table>");
				else
					tip.append("<tr><td><i>"+style.yLabel+":</i></td><td>"+(norm>0.0?Formatter.format(data[n][bar]/norm, 2):"0")+
							"</td></tr></table>");
				break;
			case STATISTICS_FIXATION_TIME:
				tip.append("<table style='border-collapse:collapse;border-spacing:0;'>"+
						"<tr><td><i>"+style.xLabel+":</i></td><td>");
				Module module = graph.getModule();
				int nPop = module.getNPopulation();
				if( nPop>FIXATION_NODES ) {
					tip.append("["+Formatter.format(style.xMin+(double)bar/nBins*(style.xMax-style.xMin), 2)+"-"+
							Formatter.format(style.xMin+(double)(bar+1)/nBins*(style.xMax-style.xMin), 2)+")");
				}
				else {
					tip.append(bar+"</td></tr>"+
							"<tr><td><i>samples:</i></td><td>"+(int)graph.getSamples(bar));
				}
				tip.append("</td></tr><tr><td><i>"+style.yLabel+":</i></td>");
				if( style.percentY )
					tip.append("<td>"+Formatter.formatPercent(graph.getData(bar), 2)+"</td>");
				else
					tip.append("<td>"+Formatter.format(graph.getData(bar), 2)+"</td>");
				tip.append("</tr></table>");
				break;
			default:
				break;

		}
		return tip.toString();
	}

	@Override
	protected int[] exportTypes() {
		switch( type ) {
			case STATISTICS_FIXATION_PROBABILITY:
			case STATISTICS_FIXATION_TIME:
				return new int[] { EXPORT_SVG, EXPORT_PNG, EXPORT_DATA };
			case DEGREE:
			default:
				return new int[] { EXPORT_SVG, EXPORT_PNG };
		}
	}

	@SuppressWarnings("null")
	@Override
	protected void exportData() {
// NOTE: consider exporting more data (e.g. overall fix probs/times, sdev, sample count); initial configuration for statistics, structure 
		String export;
		switch( type ) {
			case STATISTICS_FIXATION_PROBABILITY:
				export = "# fixation probability\n"; 
				break;
			case STATISTICS_FIXATION_TIME:
				export = "# fixation time\n";
				break;
			default:
				return;
		}
		Module pop = null;
		for( HistoGraph graph : graphs ) {
			Module newPop = graph.getModule();
			if (pop != newPop) {
				pop = newPop;
				if( isMultispecies )
					export += "# species: "+pop.getName()+"\n";
			}
			int tag = graph.getTag();
			export += "# trait: "+pop.getTraitName(tag)+"\n";
			double[][] data = graph.getData();
			if( data==null ) {
				export += "# no data available\n";
				continue;
			}
			export += "# node,\tdata\n";
			int nData = data[tag].length;
			for( int n=0; n<nData; n++ )
				export += graph.bin2x(n)+",\t"+graph.getData(n)+"\n";
		}
		EvoLudoWeb._export("data:text/csv;base64,"+EvoLudoWeb.b64encode(export), "evoludo.csv");
	}
}
