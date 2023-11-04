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

import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;

import org.evoludo.graphics.AbstractGraph;
import org.evoludo.graphics.GraphAxis;
import org.evoludo.graphics.S3Graph;
import org.evoludo.graphics.StateData;
import org.evoludo.graphics.StateGraphListener;
import org.evoludo.simulator.modules.Discrete;
import org.evoludo.simulator.models.Model;

public class MVDS3 extends MVAbstract implements StateGraphListener {

	private static final long serialVersionUID = 20110423L;

	protected boolean[] active;

	public MVDS3(EvoLudoLab lab) {
		super(lab);
	}

	@Override
	public void reset(boolean clear) {
		if( graphs.size()>0 ) {
			active = null;
			if( module.getNTraits() != module.getNActive() )
				active = module.getActiveTraits();
			super.reset(clear);
			return;
		}
		S3Graph graph = new S3Graph(this, 0);
		GraphAxis x = graph.getXAxis();
		x.grid = 2;
		x.majorTicks = 2;
		x.minorTicks = 1;
		JScrollPane scroll = new JScrollPane(graph, ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		scroll.getViewport().setOpaque(false);
		scroll.setOpaque(false);
		graph.setOpaque(false);
		add(scroll);
		graphs.add(graph);
		super.reset(true);
	}

	@Override
	public void initCustomMenu(JPopupMenu menu, AbstractGraph owner, int tag) {
		super.initCustomMenu(menu, owner, tag);
//		initCMShowLocalDynamics(menu, owner);
		initCMTimeReversed(menu, owner);
	}

	@Override
	public void getData(StateData data, int tag) {
		Model model = engine.getModel();
		data.time = model.getTime();
		if( data.isLocal ) {
			System.arraycopy(model.getMeanTraitAt(tag, localNode), 0, data.state, 0, data.state.length);
			data.connect = model.isConnected();
		}
		else
			data.connect = model.getMeanTrait(tag, data.state);
		if( active!=null ) {
			int n = 0, i = 0;
			while( n<Math.min(3, module.getNActive()) ) {
				if( active[i] )
					data.state[n++] = data.state[i];
				i++;
			}
		}
	}

	@Override
	public void setState(double[] state, int tag) {
		((Discrete)module).setInit(state);
		engine.modelReinit();
	}

	@Override
	protected String getFilePrefix() {
		return "s3";
	}

	// implement MultiViewPanel - mostly done in MVAbstract
	@Override
	public String getName() {
		return "Strategy - S3";
	}

	// COORDINATE CONVERSION UTILITIES

//	private double[] coord2state(Point2D p) {
//		double[] s = new double[3];
//		s[2] = 1.0-p.y;
//		s[1] = p.x-s[2]*0.5;
//		s[0] = 1.0-s[1]-s[2];
//		return s;
//	}
}
