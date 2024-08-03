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

package org.evoludo.graphics;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;

import javax.swing.JMenuItem;

import org.evoludo.geom.Point2D;
import org.evoludo.simulator.modules.Module;
import org.evoludo.util.Formatter;

public class S3Graph extends AbstractGraph {

	private static final long serialVersionUID = 20110423L;

	private int		nStates = -1;
	private double[]	pos;
	private final StateData	data = new StateData();
	private String[] names;
	private Color[] colors;
	private boolean[] active;
	private final int[] order = new int[] { 0, 1, 2 };
	int role = -1;

	public S3Graph(StateGraphListener controller, Module module, int role) {
		super(controller, module);
		this.role = role;
		hasHistory = true;
		frame = new S3FrameLayer(style, order);
		GraphAxis x = frame.xaxis;
		x.min = 0.0;
		x.max = 1.0;
		add(frame, LAYER_FRAME);
		glass = new GlassLayer(frame, this);
		add(glass, LAYER_GLASS);
	}

	JMenuItem menuSwapOrder;

	protected static final String MENU_SWAP_ORDER = "swap";

	@Override
	protected void initMenu() {
		menuSwapOrder = new JMenuItem("Swap");
		menuSwapOrder.setActionCommand(MENU_SWAP_ORDER);
		menuSwapOrder.addActionListener(this);
		menuSwapOrder.setFont(style.menuFont);
		menu.add(menuSwapOrder);
		super.initMenu();
	}

	@Override
	protected void showPopupMenu(Component comp, Point mouse) {
		if( hasMessage() ) 
			return;	// don't show popup menu
		switch( ((S3FrameLayer)frame).closestEdge(mouse) ) {
			// case 0:
			default:
				menuSwapOrder.setText("Swap "+names[order[0]]+" \u2194 "+names[order[1]]);
				break;
			case 1:
				menuSwapOrder.setText("Swap "+names[order[1]]+" \u2194 "+names[order[2]]);
				break;
			case 2:
				menuSwapOrder.setText("Swap "+names[order[2]]+" \u2194 "+names[order[0]]);
				break;
		}
		super.showPopupMenu(comp, mouse);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		String cmd = e.getActionCommand();
		if( cmd.equals(MENU_SWAP_ORDER) ) {
			int swap;
			String label = menuSwapOrder.getText();
			if( label.startsWith("Swap "+names[order[0]]) ) {
				swap = order[0];
				order[0] = order[1];
				order[1] = swap;
			}
			else if( label.startsWith("Swap "+names[order[1]]) ) {
				swap = order[1];
				order[1] = order[2];
				order[2] = swap;
			}
			else {
				swap = order[2];
				order[2] = order[0];
				order[0] = swap;
			}
			reset(true);
			return;
		}
		super.actionPerformed(e);
	}

	@Override
	public void reinit() {
		int id = module.getID();
		colors = controller.getColors(id);
		frame.setLabels(names, colors, active, id);
// note: this is a bit overkill... but seems necessary do deal with labels...
		frame.init(getBounds());
		((StateGraphListener)controller).getData(data, id);
		data.reset();
		if( doSVG ) {
			S3ToCartesian(data.state, q);
			svgPlot.moveTo(q.x, q.y);
		}
		super.reinit();
	}

	@Override
	public void reset(boolean clear) {
		int id = module.getID();
		nStates = controller.getNData(id);
		data.init(nStates);
		names = controller.getNames(id);
		active = controller.getActives(id);
//		if( names.length!=3 ) {
		if( controller.getActiveCount(id)!=3 ) {
			super.reset(clear);
			setMessage("Requires exactly 3 active traits/strategies!");
			return;
		}
		data.isLocal = isLocalDynamics;
		pos = new double[nStates];
		super.reset(clear);
	}

	@Override
	public void clear() {
		super.clear();
		if( doSVG ) {
			S3ToCartesian(data.state, q);
			svgPlot.moveTo(q.x, q.y);
		}
	}

	@Override
	protected void prepare() {
		if( hasMessage() ) return;	// no need to fetch data
		data.next();
		((StateGraphListener)controller).getData(data, module.getID());
	}

	private final Point2D p = new Point2D();
	private final Point2D q = new Point2D();

	@Override
	public void plot(Graphics2D g2d) {
//		if( data.time<=timestamp ) return;	// up to date
		S3ToCartesian(data.state, q);
		if( data.time<=timestamp ) {
			if( doSVG ) svgPlot.moveTo(q.x, q.y);
			return;	// up to date
		}
		timestamp = data.time;
		if( hasMessage() )
			return;	// no need to fetch data
		if( data.connect ) {
			S3ToCartesian(data.pastState, p);
//			S3ToCartesian(data.state, q);
			g2d.setStroke(style.lineStroke);
			g2d.setColor(Color.black);
			g2d.drawLine((int)p.x, (int)p.y, (int)q.x, (int)q.y);
			if( doSVG ) svgPlot.lineTo(q.x, q.y);
			return;
		}
		if( doSVG ) svgPlot.moveTo(q.x, q.y);
	}

	// set initial frequency with double-click
//	private Point2D.Double l = new Point2D.Double();
	@Override
	protected void mouseClick(Point loc, int count) {
		if( hasMessage() ) 
			return;	// ignore clicks
		if( count<2 || !frame.outline.contains(loc) ) return;
//older		cartesianToS3(loc, pos);
//older		controller.setData(pos, tag);
//newer		l.setLocation((double)(loc.x-graph.x)/(double)graph.width, (double)(loc.y-graph.y)/(double)graph.height);
//newer		((StateGraphListener)controller).setState(l, tag);
		((StateGraphListener)controller).setState(cartesianToS3(loc));
	}

	// tool tips
	// no need to call controller for help
	@Override
	public String getToolTipText(MouseEvent event) {
		Point loc = event.getPoint();
		if( !frame.outline.contains(loc) || hasMessage() ) 
			return null;	// nothing to display

		String toolTip = "<html>";
		cartesianToS3(loc, pos);
		for( int i=0; i<3; i++ )
			toolTip += "<i>"+names[i]+":</i> "+Formatter.formatFix(pos[i]*100.0, 2)+"% <br>";
		return toolTip;
	}

	@Override
	protected int getSnapshotFormat() {
		if( doSVG ) return SNAPSHOT_SVG;
		return SNAPSHOT_PNG;
	}

	// IMPLEMENT GLASS LAYER LISTENER
	private final Point2D s = new Point2D();
    @Override
	public Point2D getState() {
		return S3ToCartesian(data.state, s);
	}

    @Override
	public Point2D getStart() {
		return S3ToCartesian(data.start, s);
	}

	// COORDINATE CONVERSION UTILITIES

//	// x: (d-l)/2+0.5; y: c
//	private Point2D S3ToCartesian(double[] s3, Point2D pt) {
//		// c: s3[2], d: s3[1], l: s3[0]
//		// x: (s1-s0)/2+0.5; y: s2
//		return S3ToCartesian(s3[order[0]], s3[order[1]], s3[order[2]], pt);
//	}

//	private Point2D S3ToCartesian(double s0, double s1, double s2, Point2D pt) {
//		pt.x = (int)Math.round((s1-s0+1.0)*0.5*(canvas.width-1));
//		pt.y = (int)Math.round((1.0-s2)*(canvas.height-1));
//		return pt;
//	}

	private Point2D S3ToCartesian(double[] s3, Point2D pt) {
		// c: s3[2], d: s3[1], l: s3[0]
		pt.x = (s3[order[1]]-s3[order[0]]+1.0)*0.5*canvas.width;
		pt.y = (1.0-s3[order[2]])*canvas.height;
		return pt;
	}

	private double[] cartesianToS3(Point pt) {
		double[] s3 = new double[3];
		return cartesianToS3(pt, s3);
	}

	private double[] cartesianToS3(Point pt, double[] s3) {
		return cartesianToS3(pt.x, pt.y, s3);
	}

	private double[] cartesianToS3(int x, int y, double[] s3) {
		double sy = 1.0-(double)(y-canvas.y-1)/(double)(canvas.height-1);
		double sx = (double)(x-canvas.x-1)/(double)(canvas.width-1)-sy*0.5;
		s3[order[2]] = sy;
		s3[order[1]] = sx;
		s3[order[0]] = 1.0-sx-sy;
		return s3;
	}
}
