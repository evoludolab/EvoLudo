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

import org.evoludo.graphics.GraphAxis;
import org.evoludo.graphics.LineGraph;
import org.evoludo.graphics.StateData;
import org.evoludo.graphics.StateGraphListener;
import org.evoludo.simulator.models.Model;

public class MVDMean extends MVAbstract implements StateGraphListener {

	private static final long serialVersionUID = 20110423L;

	public MVDMean(EvoLudoLab lab) {
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
		y.label = "frequency";
		y.showLabel = true;
		y.min = 0.0;
		y.max = 1.0;
		y.grid = 3;
		y.majorTicks = 3;
		y.minorTicks = 1;
		add(graph);
		graphs.add(graph);
		super.reset(clear);
	}

	/*
	 * implement GraphListener - mostly done in MVAbstract
	 */
	@Override
	public void getData(StateData data, int tag) {
		Model model = engine.getModel();
		data.time = model.getTime();
		data.connect = model.getMeanTraits(tag, data.state);
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
		return "Strategy - Mean";
	}
}
