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

package org.evoludo.simulator;

import java.awt.Component;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JLabel;
import javax.swing.SwingConstants;

import org.evoludo.graphics.AbstractGraph;
import org.evoludo.graphics.GraphAxis;
import org.evoludo.graphics.GraphStyle;
import org.evoludo.graphics.LineGraph;
import org.evoludo.graphics.StateData;
import org.evoludo.graphics.StateGraphListener;
import org.evoludo.simulator.modules.Continuous;
import org.evoludo.simulator.models.Model;

public class MVCMean extends MVAbstract implements StateGraphListener {

	private static final long serialVersionUID = 20110423L;

	static final String xLabelText = "time";
	private final JLabel xLabel;

	int nData;
	double[] mean;
	@SuppressWarnings("hiding")
	protected Continuous module;

	public MVCMean(EvoLudoLab lab) {
		super(lab);
		xLabel = new JLabel(xLabelText, SwingConstants.RIGHT);
		xLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
		add(xLabel);
	}

	@Override
	public void setModule(org.evoludo.simulator.modules.Module module) {
		this.module = (Continuous) module;
		super.module = module;
	}

	private void addGraph(int tag) {
		LineGraph graph = new LineGraph(this, tag);
		GraphAxis x = graph.getXAxis();
		x.label = xLabelText;
		x.showLabel = false;
		x.max = 0.0;
		x.min = 1.0;	// min>max forces recalculation
		x.step = -engine.getReportInterval();
		x.grid = 0;
		x.majorTicks = 3;
		x.minorTicks = 1;
		GraphAxis y = graph.getYAxis();
		y.label = module.getTraitName(tag);
		y.showLabel = true;
		y.min = module.getTraitMin()[tag];
		y.max = module.getTraitMax()[tag];
		y.grid = 3;
		y.majorTicks = 3;
		y.minorTicks = 1;
		// assist the layout manager...
		graph.setAlignmentX(Component.CENTER_ALIGNMENT);
		// insert graph before xLabel
		Component[] comps = getComponents();
		for( int c=0; c<getComponentCount(); c++ ) {
			if( comps[c]!=xLabel )
				continue;
			// add space between graphs
			if( getComponentCount()>1 )
				add(Box.createVerticalStrut(4), c++);
			add(graph, c);
			break;
		}
		graphs.add(graph);
	}
	
	@Override
	public void reset(boolean clear) {
		nData = module.getNTraits();
		int nGraphs = graphs.size();
		if( nData==nGraphs ) {
			super.reset(clear);
			return;
		}
		if( nData<nGraphs )
			for(int n=nGraphs; n>nData; n--)
				remove(graphs.remove(nData));
		else {
			// invalidate layout of existing graphs - otherwise the newly added graphs will have height zero!
			for(int n=0; n<nGraphs; n++)
				graphs.get(n).setSize(0, 0);
			for(int n=nGraphs; n<nData; n++)
				addGraph(n);
		}
		if( mean==null || mean.length!=2*nData )
			mean = new double[2*nData];
		super.reset(true);
	}

	@Override
	public void initStyle(GraphStyle style, AbstractGraph owner, int tag) {
		super.initStyle(style, owner, tag);
		if( nData==1 )
			return;
		xLabel.setFont(style.labelFont);
		xLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, getFontMetrics(style.labelFont).stringWidth(xLabelText)));
	}

	/*
	 * implement GraphListener - mostly done in MVAbstract
	 */
// note: this is hackish... simply return 3 for min, max, mean
	@Override
	public int getNData(int tag) {
		return 3;
	}

	@Override
	public void getData(StateData data, int tag) {
		Model model = engine.getModel();
		data.time = model.getTime();
		data.connect = model.getMeanTrait(tag, mean);
		int idx = 2*tag;
		double m = mean[idx];
		double s = mean[idx+1];
		data.state[0] = m;
		data.state[1] = m-s;
		data.state[2] = m+s;
	}

	@Override
	public boolean	verifyXAxis(GraphAxis x, int tag) {
		boolean changed = false;
		double step = -engine.getReportInterval();
		if( Math.abs(x.step-step)>1e-8 ) {
			double steps = (x.max-x.min)/x.step;
			x.min = x.max-steps*step;
			x.step = step;
			changed = true;
		}
		return changed;
	}

	@Override
	public boolean	verifyYAxis(GraphAxis y, int tag) {
		boolean changed = false;
		double min = module.getTraitMin()[tag];
		if( Math.abs(y.min-min)>1e-8 ) {
			y.min = min;
			changed = true;
		}
		double max = module.getTraitMax()[tag];
		if( Math.abs(y.max-max)>1e-8 ) {
			y.max = max;
			changed = true;
		}
		return changed;
	}

	@Override
	protected String getFilePrefix() {
		return "mean";
	}

	/*
	 * implement MultiViewPanel - mostly done in MVAbstract
	 */
	@Override
	public String getName() {
		return "Trait - Mean";
	}
}
