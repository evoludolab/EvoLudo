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
import java.awt.Component;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JLabel;
import javax.swing.SwingConstants;

import org.evoludo.graphics.AbstractGraph;
import org.evoludo.graphics.GraphAxis;
import org.evoludo.graphics.GraphStyle;
import org.evoludo.graphics.HistoData;
import org.evoludo.graphics.HistoFrameLayer;
import org.evoludo.graphics.HistoGraph;
import org.evoludo.graphics.HistoGraphListener;
import org.evoludo.math.ArrayMath;
import org.evoludo.simulator.EvoLudoLab;
import org.evoludo.simulator.models.Model;

public class MVFitHistogram extends MVAbstract implements HistoGraphListener {

	private static final long serialVersionUID = 20110423L;

	double[][]	bins;
	double[]		max;
	int		nData = -1;
	static final String xLabelText = "fitness";
	private final JLabel xLabel;

	public MVFitHistogram(EvoLudoLab lab) {
		super(lab);
		xLabel = new JLabel(xLabelText, SwingConstants.RIGHT);
		xLabel.setAlignmentX(0.5f);
		add(xLabel);
	}
	
	private void alloc() {
		if( bins==null || bins.length!=nData ) {
			bins = new double[nData][];
			max = new double[nData];
			for( int n=0; n<nData; n++ ) bins[n] = new double[HistoGraph.HISTO_BINS];
		}
	}

	private void addGraph() {
		HistoGraph graph = new HistoGraph(this, module, graphs.size());
		GraphAxis x = graph.getXAxis();
		x.label = xLabelText;	// this is needed for tooltips
		x.showLabel = false;
		x.grid = 0;
		x.majorTicks = 3;
		x.minorTicks = 1;
		// assist the layout manager...
		graph.setAlignmentX(Component.CENTER_ALIGNMENT);
		// insert graph before xLabel
		Component[] comps = getComponents();
		for( int c=0; c<getComponentCount(); c++ ) {
			if( comps[c]!=xLabel ) continue;
			add(Box.createVerticalStrut(4), c);
			add(graph, c);
			break;
		}
		graphs.add(graph);
	}

	@Override
	public void reset(boolean clear) {
		nData = engine.getModel().isContinuous()?1:module.getNTraits();
		int nGraphs = graphs.size();
		int vacant = module.getVacant();
		if (vacant >= 0)
			nData--;
		if( nData==nGraphs ) {
			super.reset(clear);
			return;
		}

		if( nData<nGraphs ) {
            for(int n=nGraphs; n>nData; n--)
            	remove(graphs.remove(nData));
        }
		else {
			// invalidate layout of existing graphs - otherwise the newly added graphs will have height zero!
			for(int n=0; n<nGraphs; n++)
				graphs.get(n).setSize(0, 0);
			for(int n=nGraphs; n<nData; n++)
				addGraph();
		}
		alloc();
		super.reset(true);
	}

	@Override
	public void initStyle(GraphStyle style, AbstractGraph owner) {
		super.initStyle(style, owner);
		xLabel.setFont(style.labelFont);
		xLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, getFontMetrics(style.labelFont).stringWidth(xLabelText)));
	}

	// implement HistoGraphListener - mostly done in MVAbstract
	@Override
	public boolean	verifyXAxis(GraphAxis x, int tag) {
		boolean changed = false;
		Model model = engine.getModel();
		double min = model.getMinScore(tag);
		if( Math.abs(x.min-min)>1e-8 ) {
			x.min = min;
			changed = true;
		}
		double m = model.getMaxScore(tag);
		if( Math.abs(x.max-m)>1e-8 ) {
			x.max = m;
			changed = true;
		}
		if( m-min<1e-6 ) {
			x.min = min-1.0;
			x.max = m+1.0;
			changed = true;
		}
		return changed;
	}

	@Override
	public boolean	verifyMarkedBins(HistoFrameLayer frame, int tag) {
		Color[] colors = getColors(tag);
		Model model = engine.getModel();
		if( model.isContinuous() ) {
			Color tcolor = module.getTraitColors()[tag];
			// cast is save because pop is Continuous
			org.evoludo.simulator.models.Continuous cmodel = (org.evoludo.simulator.models.Continuous) model;
			// for continuous strategies we have a single histogram and may want to mark several bins
			boolean changed = frame.updateMarkedBin(0, cmodel.getMinMonoScore(module.getID()), tcolor.darker());
			changed |= frame.updateMarkedBin(1, cmodel.getMaxMonoScore(module.getID()), tcolor.brighter());
			return changed;
		}
		// cast is save because pop is not Continuous
		org.evoludo.simulator.models.Discrete dmodel = (org.evoludo.simulator.models.Discrete) model;
		// for discrete strategies we have different histograms and mark only a single bin
		return frame.updateMarkedBin(0, 
				dmodel.getMonoScore(module.getID(), tag), 
				new Color(Math.max(colors[tag].getRed(), 127), 
						Math.max(colors[tag].getGreen(), 127), 
						Math.max(colors[tag].getBlue(), 127)));
	}

    @Override
	public boolean getData(HistoData data, int tag) {
		// check if we need to process data first
		double now = engine.getModel().getTime();
		if( now-data.timestamp>1e-10 ) {
			// process data first
			// for neutral selection we have minScore==maxScore in that case Population/DPopulation
			// assumes an interval of [score-1, score+1]
			// note: first argument of getFitnessHistogramData is the species id but since JRE doesn't 
			// deal with multiple species this can be hardcoded to 0.
			engine.getModel().getFitnessHistogramData(0, bins);
			for( int n=0; n<nData; n++ ) {
				double maxBin = 0.0, accuBin = 0.0;
				double[] myBins = bins[n];
				int nBins = myBins.length;
				for( int i=0; i<nBins; i++ ) {
					double aBin = myBins[i];
					maxBin = Math.max(maxBin, aBin);
					accuBin += aBin;
				}
				double norm = 100.0/accuBin;
				max[n] = maxBin*norm;
				ArrayMath.multiply(myBins, norm);
			}
		}
		data.timestamp = now;
		data.state = bins[tag];
		data.bins = HistoGraph.HISTO_BINS;
		data.ymax = max[tag];
// note: should indicate if scales changed?
		return false;
	}

	// no need to display info
	@Override
	public String getName(int tag) {
		if( engine.getModel().isContinuous() )
			return null;
		return super.getName(tag);
	}

	// use default color
	@Override
	public Color getColor(int tag) {
		if( engine.getModel().isContinuous() )
			return null;
		return super.getColor(tag);
	}

	@Override
	protected String getFilePrefix() {
		return "histo";
	}

	// implement MultiViewPanel - mostly done in MVAbstract
    @Override
	public String getName() {
		return "Fitness - Histogram";
	}
}
