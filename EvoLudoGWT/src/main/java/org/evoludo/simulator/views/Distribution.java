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
import java.util.Set;

import org.evoludo.graphics.AbstractGraph;
import org.evoludo.graphics.AbstractGraph.GraphStyle;
import org.evoludo.graphics.PopGraph2D;
import org.evoludo.math.ArrayMath;
import org.evoludo.simulator.ColorMap;
import org.evoludo.simulator.ColorMapCSS;
import org.evoludo.simulator.EvoLudoGWT;
import org.evoludo.simulator.Geometry;
import org.evoludo.simulator.models.Model;
import org.evoludo.simulator.modules.Continuous;
import org.evoludo.simulator.modules.Module;
import org.evoludo.ui.ContextMenu;
import org.evoludo.ui.ContextMenuCheckBoxItem;
import org.evoludo.util.Formatter;

import com.google.gwt.user.client.Command;

/**
 *
 * @author Christoph Hauert
 */
public class Distribution extends AbstractView implements AbstractGraph.NodeGraphController {

	@SuppressWarnings("hiding")
	protected Set<PopGraph2D> graphs;
	protected int MAX_BINS = 100;
	double[]	bins;
	int			traitXIdx = 0, traitYIdx = 1;

	@SuppressWarnings("unchecked")
	public Distribution(EvoLudoGWT engine, Model.Data type) {
		super(engine, type);
		graphs = (Set<PopGraph2D>) super.graphs;
	}

	@Override
	public String getName() {
		return "Strategies - Distribution";
	}

	@Override
	public void layoutComplete() {
		// not needed - no network layouts
	}

	@Override
	public void clear() {
		for( PopGraph2D graph : graphs )
			graph.clearGraph();
	}

	@Override
	public void reset(boolean hard) {
		super.reset(hard);
		ArrayList<? extends Module> species = engine.getModule().getSpecies();
		int nGraphs = species.size();
		if( graphs.size()!=nGraphs ) {
			hard = true;
			destroyGraphs();
			for( Module module : species ) {
				PopGraph2D graph = new PopGraph2D(this, module);
				graph.setDebugEnabled(false);
				wrapper.add(graph);
				graphs2mods.put(graph, module);
			}
			gRows = species.size();
			if( gRows*gCols==2 ) {
				// always arrange horizontally if only two graphs
				gRows = 1;
				gCols = 2;
			}
			int width = 100/gCols;
			int height = 100/gRows;
			for( PopGraph2D graph : graphs )
				graph.setSize(width+"%", height+"%");
		}
		for( PopGraph2D graph : graphs ) {
			Module module = graph.getModule();
			int nTraits = module.getNTraits();
			// even if nGraphs did not change, the geometries associated with the graphs still need to be updated
			graph.setGeometry(createGeometry(nTraits));
			PopGraph2D.GraphStyle style = graph.getStyle();
			switch( type ) {
				default:
				case FITNESS:
					// not implemented
					break;
				case STRATEGY:
					graph.setColorMap(new ColorMapCSS.Gradient1D(new Color[] { Color.WHITE, Color.BLACK, Color.YELLOW, Color.RED }, 500));
					Continuous cmod = (Continuous) module;
					double min = cmod.getTraitMin()[traitXIdx];
					double max = cmod.getTraitMax()[traitXIdx];
					if( Math.abs(min-style.xMin)>1e-6 || Math.abs(max-style.xMax)>1e-6 ) {
						style.xMin = min;
						style.xMax = max;
						hard = true;
					}
					style.xLabel = cmod.getTraitName(traitXIdx);
					style.showLabel = false;
					style.showXLabel = true;
					style.showXTicks = true;
					style.showXTickLabels = true;
					style.showXLevels = false;
					if( nTraits==1 ) {
						double rFreq = engine.getReportInterval();
						// adjust y-axis scaling if report frequency has changed
						if( Math.abs(style.yIncr-rFreq)>1e-6 ) {
							style.yMax = 0.0;
							style.yIncr = -rFreq;
							hard = true;
						}
						style.yLabel = "time";
						style.showYLevels = true;
					}
					else {
						min = cmod.getTraitMin()[traitYIdx];
						max = cmod.getTraitMax()[traitYIdx];
						if( Math.abs(min-style.yMin)>1e-6 || Math.abs(max-style.yMax)>1e-6 ) {
							style.yMin = min;
							style.yMax = max;
							hard = true;
						}
						style.yLabel = cmod.getTraitName(traitYIdx);
						style.showYLevels = false;
					}
					style.showDecoratedFrame = true;
					style.percentY = false;
					style.showYLabel = true;
					style.showYTickLabels = true;
					style.showYTicks = true;
					break;
				case DEGREE:
					// not implemented
			}
			if( hard )
				graph.reset();
		}
		update(hard);
	}

	@Override
	public void init() {
		super.init();
		for( PopGraph2D graph : graphs)
			graph.init();
		update();
	}

	@Override
	public void update(boolean force) {
		// always read data - some nodes may have changed due to user actions
		double newtime = model.getTime();
		boolean isNext = (Math.abs(timestamp-newtime)>1e-8);
		timestamp = newtime;
		for( PopGraph2D graph : graphs ) {
			boolean doUpdate = isActive || graph.hasHistory();
			// if graph is neither active nor has history, force can be safely ignored
			// otherwise may lead to problems if graph has never been activated
			if (!doUpdate)
				continue;
			switch( type ) {
				case STRATEGY:
					// process data first
					// casts ok because trait histograms make sense only for continuous models
					((Model.ContinuousIBS)model).get2DTraitHistogramData(graph.getModule().getID(), bins, traitXIdx, traitYIdx);
					ColorMap.Gradient1D<String> cMap = (ColorMap.Gradient1D<String>) graph.getColorMap();
					cMap.setRange(0.0, ArrayMath.max(bins));
					cMap.translate(bins, graph.getData());
					break;
//				case FITNESS:
//					population.getFitHistogramData(bins);
//					break;
				default:
					throw new Error("MVDistribution: not implemented for type "+type);
			}
			graph.addData(isNext);
			graph.paint(force);
		}
	}

	private Geometry createGeometry(int nTraits) {
		Geometry geometry = new Geometry(engine);
		// adding a geometry name will display a label on the graph - not sure whether
		// we really want this...
		// geometry.name = module.getTraitName(n);
		if (nTraits == 1) {
			geometry.setType(Geometry.Type.LINEAR);
			geometry.size = MAX_BINS;
		} else {
			geometry.setType(Geometry.Type.SQUARE);
			geometry.connectivity = 4;
			geometry.size = MAX_BINS * MAX_BINS;
		}
		if (bins == null || bins.length != geometry.size)
			bins = new double[geometry.size];
		return geometry;
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
	public String getTooltipAt(AbstractGraph graph, int node) {
		if( node<0 )
			return null;
		GraphStyle style = graph.getStyle();
		int nBins = MAX_BINS;
		Module module = engine.getModule();
		int nTraits = module.getNTraits();
		if( nTraits == 1 ) {
			int bar = node % nBins;
			double time = -node/nBins*engine.getReportInterval();
			return (style.label!=null?"<b>"+style.label+"</b><br/>":"")+
					"<i>"+style.xLabel+":</i> ["+Formatter.format(style.xMin+bar*(style.xMax-style.xMin)/nBins, 2)+", "+
					Formatter.format(style.xMin+(bar+1)*(style.xMax-style.xMin)/nBins, 2)+"]<br/>"+
					(node<nBins?"<i>frequency:</i> "+Formatter.formatPercent(bins[bar], 1)+"<br/>":"")+
					"<i>"+style.yLabel+":</i> "+Formatter.format(time, 2);
		}
		int bar1 = node % MAX_BINS;
		int bar2 = node / MAX_BINS;
		return (style.label!=null?"<b>"+style.label+"</b><br/>":"")+
				"<i>"+style.xLabel+":</i> ["+Formatter.format(style.xMin+bar1*(style.xMax-style.xMin)/nBins, 2)+", "+
				Formatter.format(style.xMin+(bar1+1)*(style.xMax-style.xMin)/nBins, 2)+"]<br/>"+
				"<i>"+style.yLabel+":</i> ["+Formatter.format(style.yMin+bar2*(style.yMax-style.yMin)/nBins, 2)+", "+
				Formatter.format(style.yMin+(bar2+1)*(style.yMax-style.yMin)/nBins, 2)+"]<br/>"+
				"<i>frequency:</i> "+Formatter.formatPercent(bins[node], 1)+"<br/>";
	}

	private ContextMenuCheckBoxItem[] traitXItems, traitYItems;
	private ContextMenu traitXMenu, traitYMenu;

	@Override
	public void populateContextMenuAt(ContextMenu menu, int node) {
		Module module = engine.getModule();
		int nTraits = module.getNTraits();
		// ignore if less than 3 traits
		if( nTraits<3 ) {
			traitXMenu = traitYMenu = null;
			traitXItems = traitYItems = null;
			populateContextMenu(menu);
			return;
		}
		if( traitXMenu==null || traitXItems==null || traitXItems.length!=nTraits ) {
			traitXMenu = new ContextMenu(menu);
			traitXItems = new ContextMenuCheckBoxItem[nTraits];
			for( int n=0; n<nTraits; n++ ) {
				traitXItems[n] = new ContextMenuCheckBoxItem(module.getTraitName(n), new TraitCommand(traitXItems, n, TraitCommand.X_AXIS));
				traitXMenu.add(traitXItems[n]);
			}
		}
		if( traitYMenu==null || traitYItems==null || traitYItems.length!=nTraits ) {
			traitYMenu = new ContextMenu(menu);
			traitYItems = new ContextMenuCheckBoxItem[nTraits];
			for( int n=0; n<nTraits; n++ ) {
				traitYItems[n] = new ContextMenuCheckBoxItem(module.getTraitName(n), new TraitCommand(traitYItems, n, TraitCommand.Y_AXIS));
				traitYMenu.add(traitYItems[n]);
			}
		}
		// init trait submenus
		for( int n=0; n<traitXItems.length; n++ )
			traitXItems[n].setChecked(false);
		traitXItems[traitXIdx].setChecked(true);
		for( int n=0; n<traitYItems.length; n++ )
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

	public class TraitCommand implements Command {
		public static final int X_AXIS = 0;
		public static final int Y_AXIS = 1;
		ContextMenuCheckBoxItem[] traitItems;
		int idx = -1, axis = -1;

		public TraitCommand(ContextMenuCheckBoxItem[] traitItems, int idx, int axis) {
			this.traitItems = traitItems;
			this.idx = idx;
			this.axis = axis;
		}

		@Override
		public void execute() {
			// make sure all items are unchecked, then check current one
			for( int n=0; n<traitItems.length; n++ )
				traitItems[n].setChecked(false);
			traitItems[idx].setChecked(true);
			if( axis==X_AXIS )
				traitXIdx = idx;
			else traitYIdx = idx;
			reset();
		}
	}
}
