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

import org.evoludo.graphics.FrameLayer;
import org.evoludo.graphics.GraphAxis;
import org.evoludo.graphics.LineGraph;
import org.evoludo.graphics.StateData;
import org.evoludo.graphics.StateGraphListener;
import org.evoludo.simulator.modules.Discrete;
import org.evoludo.simulator.EvoLudoLab;
import org.evoludo.simulator.models.Model;

public class MVFitness extends MVAbstract implements StateGraphListener {

	private static final long serialVersionUID = 20110423L;
	private static final Color meanFitColor = Color.black;

	public MVFitness(EvoLudoLab lab) {
		super(lab);
	}

	@Override
	public void reset(boolean clear) {
		if( graphs.size()>0 ) {
			super.reset(clear);
			return;
		}
		LineGraph graph = new LineGraph(this, 0);
		GraphAxis x = graph.getXAxis();
		GraphAxis y = graph.getYAxis();
		x.label = "time";
		x.showLabel = true;
		x.max = 0.0;
		x.min = 1.0;	// min>max forces recalculation
		x.step = -engine.getReportInterval();
		x.grid = 0;
		x.majorTicks = 3;
		x.minorTicks = 1;
		y.label = "fitness";
		y.showLabel = true;
		y.grid = 3;
		y.majorTicks = 3;
		y.minorTicks = 1;
		add(graph);
		graphs.add(graph);
		super.reset(true);
	}

	// implement GraphListener - mostly done in MVAbstract
	@Override
	public int getNData(int tag) {
		// continuous strategies require 3 (for min, mean and max of fitness)
		if( engine.getModel().isContinuous() )
			return 3;
		// discrete strategies require nTraits+1 (for each strategy and population mean)
		return super.getNData(tag)+1;
	}

	@Override
	public Color[] getColors(int tag) {
		Color[] c = module.getTraitColors();
		int n = getNData(tag);
		Color[] colors = new Color[n];
		System.arraycopy(c, 0, colors, 0, n-1);
		colors[n-1] = meanFitColor;
		return colors;
	}

//101118 this is problematic - here we assume a 1:1 mapping between traits and mono scores... needs careful redesign! at least we now check the monoscorescount.
	@Override
	public boolean	verifyYThresholds(FrameLayer frame, int tag) {
		// no y-thresholds for continuous traits
		if( engine.getModel().isContinuous() )
			return false;
		Color[] colors = getColors(tag);
		boolean changed = false;
		int n = module.getNTraits();
		for( int i=0; i<n; i++ ) {
			changed |= frame.updateYThreshold(i, 
				((Discrete)module).getMonoScore(i), 
				new Color(Math.max(colors[i].getRed(), 127), 
						Math.max(colors[i].getGreen(), 127), 
						Math.max(colors[i].getBlue(), 127)));
		}
		return changed;
	}

/* really adjust x-axis upon changing report frequency?
	public boolean	verifyXAxis(GraphAxis x, int tag) {
		boolean changed = false;
		double step = -population.getReportFreq();
		if( Math.abs(x.step-step)>1e-8 ) {
			double steps = (x.max-x.min)/x.step;
			x.min = x.max-steps*step;
			x.step = step;
			changed = true;
		}
		return changed;
	}*/

	@Override
	public boolean	verifyYAxis(GraphAxis y, int tag) {
		boolean changed = false;
		double min = module.getMinScore();
		if( Math.abs(y.min-min)>1e-8 ) {
			y.min = min;
			changed = true;
		}
		double max = module.getMaxScore();
		if( Math.abs(y.max-max)>1e-8 ) {
			y.max = max;
			changed = true;
		}
		return changed;
	}

	@Override
	public void getData(StateData data, int tag) {
		Model model = engine.getModel();
		data.time = model.getTime();
		data.connect = model.getMeanFitness(data.state);
	}

	@Override
	protected String getFilePrefix() {
		return "fit";
	}

	// implement MultiViewPanel - mostly done in MVAbstract
	@Override
	public String getName() {
		return "Fitness - Mean";
	}
}
