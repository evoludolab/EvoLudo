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

import javax.swing.JLabel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;

import org.evoludo.geom.Point2D;
import org.evoludo.graphics.AbstractGraph;
import org.evoludo.graphics.GraphAxis;
import org.evoludo.graphics.ParaGraph;
import org.evoludo.graphics.StateData;
import org.evoludo.graphics.StateGraphListener;
import org.evoludo.simulator.models.Model;
import org.evoludo.util.Formatter;

public class MVDS4Manifold extends MVAbstract implements StateGraphListener {

	private static final long serialVersionUID = 20110423L;

	protected JLabel wklabel;
	protected double wk;

	public MVDS4Manifold(EvoLudoLab lab) {
		super(lab);
	}

	@Override
	public void reset(boolean clear) {
		if( graphs.size()>0 ) {
			super.reset(clear);
			return;
		}
		ParaGraph graph = new ParaGraph(this, 0);
		GraphAxis x = graph.getXAxis();
		GraphAxis y = graph.getYAxis();
		x.showLabel = false;
		x.max = 1.0;
		x.min = 0.0;
		y.showLabel = false;
		y.max = 1.0;
		y.min = 0.0;
		JScrollPane scroll = new JScrollPane(graph, ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		scroll.getViewport().setOpaque(false);
		scroll.setOpaque(false);
		graph.setOpaque(false);
		add(scroll);
		wklabel = new JLabel("-");
		wklabel.setAlignmentX(Component.CENTER_ALIGNMENT);
		add(wklabel);
		graphs.add(graph);
		super.reset(true);
	}

	// implement GraphListener - mostly done in MVAbstract
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
			data.connect = model.getMeanTraits(tag, data.state);
		state2coord(data.state, data.now);
		wk = S4Wk(data.state);
	}

// retire setting of initial state in projection of S4 (outdated JRE and only used for punishing models)
	// @Override
	// public void setState(double[] loc, int tag) {
	// 	((Discrete)module).setInit(coord2state(new Point2D(loc[0], loc[1])));
	// 	engine.modelReinit();
	// }

	// @Override
	// public String getToolTipText(Point2D loc, int tag) {
	// 	String[] names = module.getTraitNames();
	// 	double[] state = coord2state(loc);
	// 	String toolTip = "<html>";
	// 	for( int i=0; i<4; i++ )
	// 		toolTip += "<i>"+names[i]+":</i> "+Formatter.format(state[i]*100.0, 2)+"%<br>";
	// 	double wki = S4Wk(((Discrete)module).getInit());
	// 	toolTip += "<i>W<sub>k</sub> :</i> "+Formatter.format(wki, 4);
	// 	return toolTip;
	// }

	@Override
	protected String getFilePrefix() {
		return "s4mani";
	}

	// implement MultiViewPanel - mostly done in MVAbstract
	@Override
	public String getName() {
		return "Strategy - S4 Manifold";
	}

	@Override
	public void update(boolean updateGUI) {
		super.update(updateGUI);
		if( updateGUI )
			updateWK();
	}

	@Override
	public synchronized void parametersChanged(boolean didReset) {
		super.parametersChanged(didReset);
		updateWK();
	}

	private void updateWK() {
		if( wk!=wk ) wk = 0.0;	// catch NaN from 0/0
		wklabel.setText("K="+Formatter.format(wk, 2));
	}

	// COORDINATE CONVERSION UTILITIES
	// recall: V - COOP_REWARD = 0; X - COOP_NONE = 3; W - DEFECT_REWARD = 1; Y - DEFECT_NONE = 2;

	private static double S4Wk(double[] s) {
		return s[1]*s[3]/(s[0]*s[2]);
	}

	private Point2D state2coord(double[] s, Point2D p) {
		p.x = (s[1]+s[2]-s[0]-s[3]+1.0)/2.0;
		p.y = (1.0-(s[2]+s[3]-s[0]-s[1]))/2.0;
		return p;
	}

// 	private double[] coord2state(Point2D p) {
// 		double[] init = ((Discrete)module).getInit();
// 		double wki = 1.0/S4Wk(init);	// here we use Y*V/(X*W)
// 		double[] s = new double[4];
// 		// threshold was 1e-8, which is apparently too small for floats!
// 		if( Math.abs(wki-1.0)<1e-6 ) {
// 			// manifold as good as Wk=1
// 			s[0] = p.y*(1.0-p.x);
// 			s[1] = p.x*p.y;
// 			s[2] = s[1]*(1.0-p.y)/(s[0]+s[1]);
// 			s[3] = s[0]*(1.0-p.y)/(s[0]+s[1]);
// 			return s;
// 		}
// 		double wks = wki*(1.0-p.x-p.y);
// 		double sqr = Math.sqrt(4.0*(wki-1.0)*p.x*p.y+(p.x+p.y+wks)*(p.x+p.y+wks));
// 		double b1 = p.x-p.y+wki*(1.0-p.x+p.y);
// 		double b2 = -p.x-p.y-wks;
// 		double i2wk1 = 1.0/(2.0*(wki-1.0));
// 		s[0] = (b1-sqr)*i2wk1;
// 		s[1] = (b2+sqr)*i2wk1;
// 		s[2] = wki*s[1]*(1.0-p.y)/(s[0]+wki*s[1]);
// 		s[3] = s[0]*(1.0-p.y)/(s[0]+wki*s[1]);
// 		// check if inside simplex - exclude 0 and 1 to avoid complications
// 		if( s[0]>0.0 && s[0]<1.0 && s[1]>0.0 && s[1]<1.0 
// 			&& s[2]>0.0 && s[2]<1.0 && s[3]>0.0 && s[3]<1.0 )
// 			return s;
			
// 		s[0] = (b1+sqr)*i2wk1;
// 		s[1] = (b2-sqr)*i2wk1;
// 		s[2] = wki*s[1]*(1.0-p.y)/(s[0]+wki*s[1]);
// 		s[3] = s[0]*(1.0-p.y)/(s[0]+wki*s[1]);
// 		// check if inside simplex - exclude 0 and 1 to avoid complications
// 		if( s[0]>0.0 && s[0]<1.0 && s[1]>0.0 && s[1]<1.0 
// 			&& s[2]>0.0 && s[2]<1.0 && s[3]>0.0 && s[3]<1.0 )
// 			return s;

// System.out.println("failed... wk="+S4Wk(s)+" s=("+s[0]+", "+s[1]+", "+s[2]+", "+s[3]+")");
// 		logger.warning("initial point not in simplex - how can this happen?!");
// 		System.arraycopy(init, 0, s, 0, 4);
// 		return s;
// 	}
}
