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

package org.evoludo.graphics;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.text.DecimalFormat;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JPopupMenu;

import org.evoludo.simulator.modules.Module;

public class HistoGraph extends AbstractGraph {

	private static final long serialVersionUID = 20110423L;

	private static final double[][] autoscale = 
			new double[][] { { 100.0, 40.0, 3.0 }, { 50.0, 20.0, 4.0 }, { 25.0, 8.0, 4.0 }, { 10.0, 4.0, 4.0 }, { 5.0, 0.0, 4.0 } };
	private int autoscaleidx;
	private boolean updatescale = false;
	boolean doAutoscale = true;

	public static final int HISTO_BINS = 100;

	private Color	color;
	private String	name;
	private final HistoData data;
	int row = -1;

	protected JCheckBoxMenuItem logXScaleMenu;
	protected JCheckBoxMenuItem logYScaleMenu;
	protected JPopupMenu.Separator logScaleSeparator;
	
	protected static final String MENU_AUTO_SCALE = "auto";
	protected static final String MENU_LOG_SCALE_X = "xlog";
	protected static final String MENU_LOG_SCALE_Y = "ylog";

	public HistoGraph(GraphListener controller, Module module, int row) {
		super(controller, module);
		this.row = row;
		data = new HistoData();
		frame = new HistoFrameLayer(data, style);
		add(frame, LAYER_FRAME);
		GraphAxis y = frame.yaxis;
		y.label = "frequency";
		y.showLabel = true;
		y.min = 0.0;
		y.max = 100.0;
		y.majorTicks = 3;
		y.grid = 3;
		y.unit = "%";
		y.formatter = new DecimalFormat("0");
		setOpaque(false);
		// always save vector graphics
		doSVG = true;
		zoomInOut = false;
	}

    @Override
	protected void initMenu() {
		super.initMenu();
		logScaleSeparator = new JPopupMenu.Separator();
		menu.add(logScaleSeparator);
		JCheckBoxMenuItem auto = new JCheckBoxMenuItem("Autoscale", doAutoscale);
		auto.setActionCommand(MENU_AUTO_SCALE);
		auto.addActionListener(this);
		auto.setFont(style.menuFont);
		menu.add(auto);
		logXScaleMenu = new JCheckBoxMenuItem("X Log Scale", data.logx);
		logXScaleMenu.setActionCommand(MENU_LOG_SCALE_X);
		logXScaleMenu.addActionListener(this);
		logXScaleMenu.setFont(style.menuFont);
		menu.add(logXScaleMenu);
		logYScaleMenu = new JCheckBoxMenuItem("Y Log Scale", data.logy);
		logYScaleMenu.setActionCommand(MENU_LOG_SCALE_Y);
		logYScaleMenu.addActionListener(this);
		logYScaleMenu.setFont(style.menuFont);
		menu.add(logYScaleMenu);
	}

    public void setLogScaleCMEnabled(boolean x, boolean y) {
		logScaleSeparator.setVisible(x || y);
		logXScaleMenu.setVisible(x);
		logYScaleMenu.setVisible(y);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		String cmd = e.getActionCommand();
		if( cmd.equals(MENU_AUTO_SCALE) ) {
			doAutoscale = ((JCheckBoxMenuItem)e.getSource()).getState();
frame.init(getBounds());
			updatescale = true;
			repaint();
			return;
		}
		if( cmd.equals(MENU_LOG_SCALE_X) ) {
			data.logx = ((JCheckBoxMenuItem)e.getSource()).getState();
frame.xaxis.logScale = data.logx;
frame.init(getBounds());
//			updatescale = true;
			data.timestamp = -1.0;	// make sure the data is re-read and processed
			prepare();
			repaint();
			return;
		}
		if( cmd.equals(MENU_LOG_SCALE_Y) ) {
			data.logy = ((JCheckBoxMenuItem)e.getSource()).getState();
frame.yaxis.logScale = data.logy;
frame.init(getBounds());
//			updatescale = true;
			data.timestamp = -1.0;
			prepare();
			repaint();
			return;
		}
		super.actionPerformed(e);
	}

	@Override
	public void reset(boolean clear) {
		name = controller.getName(row);
		// specify relative positioning of annotation with respect to top-right-corner
		frame.labels.clear();
		if( name!=null )
			frame.labels.add(new GraphLabel(name, 8, style.labelMetrics.getHeight(), Color.black));
		GraphAxis x = frame.xaxis, y = frame.yaxis;
		// JRE: only single species supported
		boolean changed = controller.verifyXAxis(x, 0);
		changed |= controller.verifyYAxis(y, 0);
		if( changed || clear ) {
			x.restore();
			y.restore();
			frame.init(getBounds());
			clear = true;
		}
//		// needs to be called only after potential changes to the frame such that new binwidth is known
//		controller.verifyMarkedBins((HistoFrameLayer)frame, tag);
		// reset y-axis
		autoscaleidx = 0;
		y.upper = autoscale[autoscaleidx][0];
		y.majorTicks = (int)autoscale[autoscaleidx][2];
		y.grid = y.majorTicks;
//		((HistoGraphListener)controller).getData(data, tag);
		if( ((HistoGraphListener)controller).getData(data, row) ) {
			x.min = data.xmin;
			x.max = data.xmax;
			x.restore();
		}
		if( doAutoscale ) {
			while( data.ymax<autoscale[autoscaleidx][1] ) {
				autoscaleidx++;
				y.upper = autoscale[autoscaleidx][0];
				y.majorTicks = (int)autoscale[autoscaleidx][2];
				y.grid = y.majorTicks;
			}
		}
		updatescale = true;	// after reset, always update scale
		super.reset(clear);
	}

	@Override
	public void reinit() {
		color = controller.getColor(row);
		// needs to be called only after potential changes to the frame such that new binwidth is known
// note: causes grief in Traits
//		 this may get called before bounds are set and hence before the binwidth could be determined - skip if this is the case.
//		controller.verifyMarkedBins((HistoFrameLayer)frame, tag);
frame.init(getBounds());
if( getBounds().width>0 && getBounds().height>0 )
	controller.verifyMarkedBins((HistoFrameLayer)frame, row);
		data.reset();
		super.reinit();
	}

	@Override
	protected void prepare() {
		// get data
//		((HistoGraphListener)controller).getData(data, tag);
		if( ((HistoGraphListener)controller).getData(data, row) ) {
// note: is this too drastic a measure? - do we care?
			GraphAxis x = frame.xaxis;
			x.min = data.xmin;
			x.max = data.xmax;
			x.restore();
frame.init(getBounds());
			updatescale = true;
		}
		// dynamically update tooltip info
		if( gtip!=null && gtip.isShowing() ) gtip.setTipText(getToolTipText(infobin));

		if( !doAutoscale ) return;

		if( data.ymax>autoscale[autoscaleidx][0] ) {
			GraphAxis y = frame.yaxis;
			autoscaleidx = Math.max(0, autoscaleidx-1);
			y.upper = autoscale[autoscaleidx][0];
			y.majorTicks = (int)autoscale[autoscaleidx][2];
			y.grid = y.majorTicks;
			updatescale = true;
			// no need to call repaint - should happen soon anyways.
			return;
		}
		if( data.ymax<autoscale[autoscaleidx][1] ) {
			GraphAxis y = frame.yaxis;
			autoscaleidx++;
			y.upper = autoscale[autoscaleidx][0];
			y.majorTicks = (int)autoscale[autoscaleidx][2];
			y.grid = y.majorTicks;
			updatescale = true;
			// no need to call repaint - should happen soon anyways.
			return;
		}
	}

	@Override
	protected void plot(Graphics2D g) {
		if( updatescale ) {
			frame.formatTicks();
			frame.formatGrid();
			frame.repaint();
			updatescale = false;
		}
		if( color==null ) g.setColor(Color.black);	// default color is black
		else g.setColor(color);

		int xshift = canvas.x;
		GraphAxis x = frame.xaxis, y = frame.yaxis;
		int binwidth = ((HistoFrameLayer)frame).binwidth;
		int bindraw = binwidth;
		if( binwidth>3 ) bindraw--;
		int start = (int)((x.lower-x.min)/(x.max-x.min)*data.bins+0.5);
		int end = (int)((x.upper-x.min)/(x.max-x.min)*data.bins+0.5);
		for( int i=start; i<end; i++ ) {
			double binheight = data.state[i];
			if( binheight>0.0 ) {
				int ycoord = canvas.y+(int)((1.0-binheight/(y.upper-y.lower))*canvas.height+0.5);
				g.fillRect(xshift, ycoord, bindraw, canvas.y+canvas.height-ycoord);
			}
			xshift += binwidth;
		}
	}

	@Override
	protected int getSnapshotFormat() {
		return SNAPSHOT_SVG;
	}

	private int convertCoord2Bin(int coord) {
		GraphAxis x = frame.xaxis;
		int start = (int)((x.lower-x.min)/(x.max-x.min)*data.bins+0.5);
		return start+(coord-canvas.x)/((HistoFrameLayer)frame).binwidth;
	}

/* unused - roundoff errors lurking...
	private int convertX2Bin(double xcoord) {
		GraphAxis x = frame.xaxis;
		// it's correct to use min & max here
		return Math.max(Math.min((int)((xcoord-x.min)/(x.max-x.min)*(double)data.bins), data.bins-1), 0);
	}

	private double convertBin2X(int bin) {
		GraphAxis x = frame.xaxis;
		return x.min+bin*(x.max-x.min)/(double)data.bins;
	}

	private int convertBin2Coord(int bin) {
		return canvas.x+bin*((HistoFrameLayer)frame).binwidth;
	}*/

	// tool tips
	protected int infobin = -1;
	@Override
	public String getToolTipText(MouseEvent event) {
		Point loc = event.getPoint();
		if( !canvas.contains(loc) ) return null;
		infobin = convertCoord2Bin(loc.x);
		return getToolTipText(infobin);
	}

	private String getToolTipText(int bin) {
		GraphAxis x = frame.xaxis, y = frame.yaxis;
		double xDelta = (x.max-x.min)/data.bins;
		double xLow = x.min+bin*xDelta;
		// use x-fomatter for y-coordinate to add some decimals
		String tt = "<html>";
		if( name!=null ) tt += "<i>"+name+"</i><br>";
		return tt+"<i>"+x.label+":</i> "+x.formatter.format(xLow+xDelta*0.5)+" - ["+x.formatter.format(xLow)+", "+x.formatter.format(xLow+xDelta)+")<br><i>"+y.label+":</i> "+x.formatter.format(data.state[bin])+y.unit;
	}

	private int mouseStart, mouseStartBin;
	@Override
	protected void setZoomRect(Point mouse) {
		mouse.y = canvas.y;
		int binwidth = ((HistoFrameLayer)frame).binwidth;
		mouseStartBin = (mouse.x-canvas.x)/binwidth;
		mouse.x = canvas.x+mouseStartBin*binwidth;
		mouseStart = mouse.x;
		currRect.setBounds(mouse.x, mouse.y, binwidth, canvas.height-1);
		drawRect.setBounds(currRect);
	}

	@Override
	protected void updateZoomRect(Point mouse, boolean shift) {
		mouse.y = canvas.y+canvas.height-1;
		int binwidth = ((HistoFrameLayer)frame).binwidth;
		int bins = (mouse.x-mouseStart)/binwidth;
		if( bins==0 ) bins = (mouse.x<mouseStart?-1:1);
		mouse.x = mouseStart+bins*binwidth;
		super.updateZoomRect(mouse, shift);
	}

	@Override
	protected void zoomRange() {
		GraphAxis x = frame.xaxis;
		int binwidth = ((HistoFrameLayer)frame).binwidth;
		int start = (drawRect.x-canvas.x)/binwidth;
		int range = drawRect.width/binwidth;
		double delta = (x.max-x.min)/data.bins;
		x.lower += start*delta;
		x.upper = x.lower+range*delta;
		frame.init(getBounds());
	}
}
