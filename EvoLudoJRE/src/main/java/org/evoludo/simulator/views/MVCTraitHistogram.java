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
import java.awt.Component;

import javax.swing.Box;

import org.evoludo.graphics.AbstractGraph;
import org.evoludo.graphics.GraphAxis;
import org.evoludo.graphics.HistoData;
import org.evoludo.graphics.HistoGraph;
import org.evoludo.graphics.HistoGraphListener;
import org.evoludo.simulator.EvoLudoLab;
import org.evoludo.simulator.models.Model;
import org.evoludo.simulator.modules.Continuous;

public class MVCTraitHistogram extends MVAbstract implements HistoGraphListener {

	private static final long serialVersionUID = 20110423L;

	double[][]	bins;
	double[]		max;
	int		nData;
	@SuppressWarnings("hiding")
	protected Continuous module;

	public MVCTraitHistogram(EvoLudoLab lab) {
		super(lab);
	}

	@Override
	public void setModule(org.evoludo.simulator.modules.Module module) {
		this.module = (Continuous) module;
		super.module = module;
	}

	private void alloc() {
		if( bins==null || bins.length!=nData ) {
			bins = new double[nData][];
			max = new double[nData];
			for( int n=0; n<nData; n++ )
				bins[n] = new double[HistoGraph.HISTO_BINS];
		}
	}
	
	private void addGraph(int row) {
		HistoGraph graph = new HistoGraph(this, module, row);
		GraphAxis x = graph.getXAxis();
		x.label = module.getTraitName(row);
		x.showLabel = true;
		x.min = module.getTraitMin()[row];
		x.max = module.getTraitMax()[row];
		x.grid = 0;
		x.majorTicks = 3;
		x.minorTicks = 1;
		// assist the layout manager...
		graph.setAlignmentX(Component.CENTER_ALIGNMENT);
		if( row>0 )
			add(Box.createVerticalStrut(4));
		add(graph);
		graphs.add(graph);
	}
	
// note: this has potential for significant streamlinig - add to reset in MVAbstract; subclasses provide nData count (important for e.g. MVDegree); 
//		 call reset(int tag) for customized resetting of each graph
	@Override
	public void reset(boolean clear) {
		nData = module.getNTraits();
		int nGraphs = graphs.size();
		if( nData==nGraphs ) {
			for( AbstractGraph graph : graphs ) {
				GraphAxis x = graph.getXAxis();
				int id = graph.getModule().getID();
				x.min = module.getTraitMin()[id];
				x.max = module.getTraitMax()[id];
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
		alloc();
		super.reset(true);
	}
	
	// implement HistoGraphListener - mostly done in MVAbstract
	@Override
	public boolean	verifyXAxis(GraphAxis x, int tag) {
		boolean changed = false;
		double m = module.getTraitMin()[tag];
		if( Math.abs(x.min-m)>1e-8 ) {
			x.min = m;
			changed = true;
		}
		m = module.getTraitMax()[tag];
		if( Math.abs(x.max-m)>1e-8 ) {
			x.max = m;
			changed = true;
		}
		return changed;
	}

// note: maybe we can mark singular strategies at some point...
//	public boolean	verifyMarkedBins(HistoFrameLayer frame, int tag) {
//	}

	@Override
	public boolean getData(HistoData data, int tag) {
		Model model = engine.getModel();
		// check if we need to process data first
		double now = model.getTime();
		if( now-data.timestamp>1e-10 ) {
			// process data first
			((org.evoludo.simulator.models.Continuous) model).getTraitHistogramData(0, bins);
			// convert to percentages - also need to keep track of maxima
			for( int n=0; n<nData; n++ ) {
				int nBins = bins[n].length;
				double maxData = -Double.MAX_VALUE;
				double[] trait = bins[n];
				for( int i=0; i<nBins; i++ ) {
					trait[i] *= 100.0;
					maxData = Math.max(maxData, trait[i]);
				}
				max[n] = maxData;
			}
//			timestamp = now;
		}
		data.timestamp = now;
		data.state = bins[tag];
		data.bins = HistoGraph.HISTO_BINS;
		data.ymax = max[tag];
// note: should indicate if scales changed
		return false;
	}

	// no need to display info
	@Override
	public String getName(int tag) {
		return null;
	}

// note: this may require some attention for multiple traits... coloring not properly resolved
	// use default color
	@Override
	public Color getColor(int tag) {
		return null;
	}

	@Override
	protected String getFilePrefix() {
		return "trait";
	}

	// implement MultiViewPanel - mostly done in MVAbstract
	@Override
	public String getName() {
		return "Trait - Histogram";
	}
}
