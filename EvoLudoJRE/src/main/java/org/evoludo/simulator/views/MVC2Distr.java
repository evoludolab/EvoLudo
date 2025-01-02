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
import java.awt.Graphics2D;
import java.awt.Rectangle;

import javax.swing.BoxLayout;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;

import org.evoludo.graphics.AbstractGraph;
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

public class MVC2Distr extends MVAbstract implements PopListener {

	private static final long serialVersionUID = 20110423L;

	double[]		bins;
	int			nData;
	String		ylabel = "time";
	protected	ColorMap<Color> colorMap;
	@SuppressWarnings("hiding")
	protected Continuous module;
	protected PopGraph2D graph;

    public MVC2Distr(EvoLudoLab lab) {
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
		// requires exactly 2 traits
// note: this requires further attention - HISTO_BINS might disappear in favor of a dynamic approach
		bins = new double[HistoGraph.HISTO_BINS*HistoGraph.HISTO_BINS];
		// requires exactly 2 traits
        Geometry geometry = new Geometry(engine);
        geometry.setType(Geometry.Type.SQUARE);
        geometry.size = HistoGraph.HISTO_BINS*HistoGraph.HISTO_BINS;
		graph = new PopGraph2D(this, geometry, module);
		GraphAxis x = graph.getXAxis();
		x.label = module.getTraitName(0);
		x.showLabel = true;
		double[] min = module.getTraitMin();
		double[] max = module.getTraitMax();
		x.min = min[0];
		x.max = max[0];
		x.grid = 0;
		x.majorTicks = 3;
		x.minorTicks = 1;
		GraphAxis y = graph.getYAxis();
		y.label = module.getTraitName(1);
		y.showLabel = true;
		y.min = min[1];
		y.max = max[1];
		y.grid = 0;
		y.majorTicks = 3;
		y.minorTicks = 1;
		JScrollPane scroll = new JScrollPane(graph, ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		scroll.getViewport().setOpaque(false);
		scroll.setOpaque(false);
		graph.setOpaque(false);
		add(scroll);
		graphs.add(graph);
	}

	@Override
	public void reset(boolean clear) {
		int nPanes = module.getNTraits();
		if( nPanes!=nData )
			initGraphs();
		super.reset(clear);
	}
	
	// a chance to customize and touch up the graphics
	@Override
	public void polish(Graphics2D plot, AbstractGraph agraph) {
		// add diagonal line
		plot.setPaint(Color.red);
		Rectangle canvas = graph.canvas;
		plot.drawLine(canvas.x, canvas.y+canvas.height, canvas.x+canvas.width, canvas.y);
	}

	// IMPLEMENT PopListener - alternative location to set axis specs
	@Override
	public boolean	verifyXAxis(GraphAxis x, int tag) {
		boolean changed = false;
// requires exactly 2 traits
		double min = module.getTraitMin()[0];
		if( Math.abs(x.min-min)>1e-8 ) {
			x.min = min;
			changed = true;
		}
		double max = module.getTraitMax()[0];
		if( Math.abs(x.max-max)>1e-8 ) {
			x.max = max;
			changed = true;
		}
		return changed;
	}

	@Override
	public boolean	verifyYAxis(GraphAxis y, int tag) {
		boolean changed = false;
// requires exactly 2 traits
		double min = module.getTraitMin()[1];
		if( Math.abs(y.min-min)>1e-8 ) {
			y.min = min;
			changed = true;
		}
		double max = module.getTraitMax()[1];
		if( Math.abs(y.max-max)>1e-8 ) {
			y.max = max;
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
//			((CXPopulation)population).getTraitDensityData(data, colorMap, bins, true);
			((org.evoludo.simulator.models.Continuous) model).get2DTraitHistogramData(0, bins, 0, 1);
			// translate data into colors
			timestamp = now;
		}
		colorMap.translate(bins, data);
		return now;
	}

    @Override
	public String getInfoAt(Network2D network, int node, int tag) {
		if( node<0 )
			return null;
//		PopGraph2D graph = (PopGraph2D)graphs.get(tag);
		int xBin = node%HistoGraph.HISTO_BINS;
		GraphAxis x = graph.getXAxis();
		double xDelta = (x.max-x.min)/HistoGraph.HISTO_BINS;
		double xLow = x.min+xBin*xDelta;
		int yBin = node/HistoGraph.HISTO_BINS;
		GraphAxis y = graph.getYAxis();
		double yDelta = (y.max-y.min)/HistoGraph.HISTO_BINS;
		double yLow = y.min+yBin*yDelta;
// note: 'density' and '%' should not be hardwired...
		return "<html><i>"+x.label+":</i> ["+x.formatter.format(xLow)+", "+x.formatter.format(xLow+xDelta)+")"+
			   "<br><i>"+y.label+":</i> ["+y.formatter.format(yLow)+", "+y.formatter.format(yLow+yDelta)+")"+
			   "<br><i>density:</i> "+x.formatter.format(100.0*bins[node])+"%";
	}

    @Override
	protected String getFilePrefix() {
		return "distr2D";
	}

	// implement MultiViewPanel - mostly done in MVAbstract
    @Override
	public String getName() {
		return "Trait - Distribution 2D";
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
