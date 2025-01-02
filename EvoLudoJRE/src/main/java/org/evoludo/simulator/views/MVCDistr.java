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
import java.text.DecimalFormat;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;

import org.evoludo.graphics.GraphAxis;
import org.evoludo.graphics.HistoGraph;
import org.evoludo.graphics.PopGraph2D;
import org.evoludo.graphics.PopListener;
import org.evoludo.simulator.ColorMap;
import org.evoludo.simulator.ColorMapJRE;
import org.evoludo.simulator.EvoLudoLab;
import org.evoludo.simulator.Geometry;
import org.evoludo.simulator.Network2D;
import org.evoludo.simulator.models.Model;
import org.evoludo.simulator.modules.Continuous;

/**
 *
 * @author Christoph Hauert
 */
public class MVCDistr extends MVAbstract implements PopListener {

	private static final long serialVersionUID = 20110423L;

	double[][]	bins;
	int			nData;
	String		ylabel = "time";
	protected	ColorMap<Color> colorMap;
	@SuppressWarnings("hiding")
	protected Continuous module;

/* NOTE: this requires some work... may not be happening...
 - everything needs to be wrapped in the same scroll - scrolls for each graph are strange; how does this affect zoom operations?
 - if individual scrolls turn out to be the better way, we need to be careful when removing graphs!
 - below is a first sketch to align the initialization with all other classes

	String		yLabelText = "time";
	JLabel		yLabel;

	public MVCDistr(EvoLudoLab lab, Population population) {
		super(lab, population);
		setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
		yLabel = new JLabel(yLabelText, SwingConstants.RIGHT);
		yLabel.setAlignmentY(Component.CENTER_ALIGNMENT);
		add(yLabel);
	}
	
	private void alloc() {
		if( bins==null || bins.length!=nData ) {
			bins = new double[nData][];
			for( int n=0; n<nData; n++ ) bins[n] = new double[HistoGraph.HISTO_BINS];
		}
	}
	
	private void addGraph(int tag) {
		PopGraph graph = new PopGraph(this, null, tag);
		GraphAxis x = graph.getXAxis();
		x.label = ((CXPopulation)population).getTraitName(tag);
		x.showLabel = true;
		x.min = ((CXPopulation)population).getTraitMin(tag);
		x.max = ((CXPopulation)population).getTraitMax(tag);
		x.grid = 0;
		x.majorTicks = 3;
		x.minorTicks = 1;
		GraphAxis y = graph.getYAxis();
		y.label = ylabel;
		y.showLabel = false;
		y.max = 0.0;
		y.min = 10.0;	// this is just to achieve better layout
		y.step = -population.getReportFreq();
		y.majorTicks = 4;
		y.minorTicks = 1;
		y.formatter = new DecimalFormat("0.#");
		y.grid = 0;
		JScrollPane scroll = new JScrollPane(graph, ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		scroll.getViewport().setOpaque(false);
		scroll.setOpaque(false);
		graph.setOpaque(false);
		// assist the layout manager...
		scroll.setAlignmentY(Component.CENTER_ALIGNMENT);
		// insert graph before xLabel
		Component[] comps = getComponents();
		for( int c=0; c<getComponentCount(); c++ ) {
			if( comps[c]!=yLabel ) continue;
			add(Box.createHorizontalStrut(4), c);
			add(scroll, c);
			break;
		}
		graphs.add(graph);
	}
	
	@Override
	public void reset(boolean clear) {
		nData = population.getTraitCount();
		int nGraphs = graphs.size();
		if( nData==nGraphs ) {
			for( java.util.ListIterator<AbstractGraph> i = graphs.listIterator(); i.hasNext(); ) {
				AbstractGraph graph = i.next();
				GraphAxis x = graph.getXAxis();
				x.min = ((CXPopulation)population).getTraitMin(graph.tag);
				x.max = ((CXPopulation)population).getTraitMax(graph.tag);
			}
			super.reset(clear);
			return;
		}
		if( nData<nGraphs )
			for(int n=nGraphs; n>nData; n--) remove(graphs.remove(nData));
		else {
			// invalidate layout of existing graphs - otherwise the newly added graphs will have height zero!
			for(int n=0; n<nGraphs; n++) graphs.get(n).setSize(0, 0);
			for(int n=nGraphs; n<nData; n++) addGraph(n);
		}
// note: java 1.7 requires acquiring a tree lock before calling validateTree - is this the right approach? shouldn't validate be called instead?
//		synchronized( getTreeLock() ) { validateTree(); }
		alloc();
		super.reset(true);
	}
*/

    public MVCDistr(EvoLudoLab lab) {
		super(lab);
		setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
	}

	@Override
	public void setModule(org.evoludo.simulator.modules.Module module) {
		this.module = (Continuous) module;
		super.module = module;
	}

   protected void initGraphs() {
		removeAll();
		graphs.clear();
		nData = module.getNTraits();
		bins = new double[nData][];
		double[] min = module.getTraitMin();
		double[] max = module.getTraitMax();
		Model model = engine.getModel();
		for( int n=0; n<nData; n++ ) {
            Geometry geometry = new Geometry(engine);
            geometry.setType(Geometry.Type.LINEAR);
            geometry.size = HistoGraph.HISTO_BINS;
            PopGraph2D graph = new PopGraph2D(this, geometry, module);
			bins[n] = new double[HistoGraph.HISTO_BINS];
			GraphAxis x = graph.getXAxis();
			x.label = module.getTraitName(n);
			x.showLabel = true;
			x.min = min[n];
			x.max = max[n];
			x.grid = 0;
			x.majorTicks = 3;
			x.minorTicks = 1;
			GraphAxis y = graph.getYAxis();
			y.label = ylabel;
			y.showLabel = true;
			if( nData>1 && n!=nData-1 ) y.showLabel = false;
			y.max = 0.0;
			y.min = 10.0;	// this is just to achieve better layout
			y.step = -model.getTimeStep();
			y.majorTicks = 4;
			y.minorTicks = 1;
			y.formatter = new DecimalFormat("0.#");
			y.grid = 0;
			JScrollPane scroll = new JScrollPane(graph, ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
			scroll.getViewport().setOpaque(false);
			scroll.setOpaque(false);
			graph.setOpaque(false);
			add(scroll);
			graphs.add(graph);
			if( nData>1 && n!=nData-1 ) add(Box.createHorizontalStrut(4));
		}
	}

	@Override
	public void reset(boolean clear) {
		int nPanes = module.getNTraits();
		if( nPanes!=nData )
			initGraphs();
		super.reset(clear);
	}
	
	// IMPLEMENT PopListener - alternative location to set axis specs
	@Override
	public boolean	verifyXAxis(GraphAxis x, int tag) {
		boolean changed = false;
		double min = module.getTraitMin()[tag];
		if( Math.abs(x.min-min)>1e-8 ) {
			x.min = min;
			changed = true;
		}
		double max = module.getTraitMax()[tag];
		if( Math.abs(x.max-max)>1e-8 ) {
			x.max = max;
			changed = true;
		}
		return changed;
	}

	@Override
	public boolean	verifyYAxis(GraphAxis y, int tag) {
		boolean changed = false;
		double step = -engine.getModel().getTimeStep();
		if( Math.abs(y.step-step)>1e-8 ) {
			y.step = step;
			changed = true;
		}
		return changed;
	}

	// implement PopListener - mostly done in MVAbstract

    @Override
	public void initColor(int tag) {
		colorMap = new ColorMapJRE.Gradient1D(new Color[] { Color.WHITE, Color.BLACK, Color.RED }, 100);
	}

    @Override
	public double getData(Color[] data, int tag) {
		Model model = engine.getModel();
		// check if we need to process data first
    	double now = model.getTime();
		if( now-timestamp>1e-8 ) {
			// process data first
			((org.evoludo.simulator.models.Continuous) model).getTraitHistogramData(0, bins);
			timestamp = now;
		}
		colorMap.translate(bins[tag], data);
		return now;
	}

    @Override
   	public String getInfoAt(Network2D network, int node, int tag) {
    	int nNodes = network.nNodes;
   
		if( node<0 ) return null;
		PopGraph2D graph = (PopGraph2D)graphs.get(tag);
		int xBin = node%nNodes;
		GraphAxis x = graph.getXAxis();
		double xDelta = (x.max-x.min)/nNodes;
		double xLow = x.min+xBin*xDelta;
		int yBin = node/nNodes;
		GraphAxis y = graph.getYAxis();
		double yDelta = engine.getModel().getTimeStep();
		double yHigh = y.max-yBin*yDelta;
		String info = "<html><i>"+y.label+":</i> ("+y.formatter.format(yHigh)+", "+y.formatter.format(yHigh-yDelta)+"]"+
					  "<br><i>"+x.label+":</i> ["+x.formatter.format(xLow)+", "+x.formatter.format(xLow+xDelta)+")";
		// only the first row returns more detailed information
		if( yBin>0 ) return info;
// note: 'density' and '%' should not be hardwired...
		return info+"<br><i>density:</i> "+x.formatter.format(100.0*bins[tag][xBin])+"%";
	}

    @Override
	protected String getFilePrefix() {
		return "distr";
	}

	// implement MultiViewPanel - mostly done in MVAbstract
    @Override
	public String getName() {
		return "Trait - Distribution";
	}

    @Override
	public boolean mouseHitNode(int node, int tag) {
		return false;
	}

//    @Override
//	public boolean mouseHitNode(int node, int refnode, int tag) {
//		return false;
//	}
}
