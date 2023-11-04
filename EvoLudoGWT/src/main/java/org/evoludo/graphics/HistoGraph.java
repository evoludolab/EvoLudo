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

import java.util.ArrayList;
import java.util.Arrays;

import org.evoludo.ui.ContextMenu;
import org.evoludo.ui.ContextMenuCheckBoxItem;
import org.evoludo.math.ArrayMath;
import org.evoludo.math.Functions;
import org.evoludo.simulator.modules.Module;

import com.google.gwt.user.client.Command;

/**
 *
 * @author Christoph Hauert
 */
public class HistoGraph extends AbstractGraph {

	public interface HistoGraphController extends Controller {
		public String getTooltipAt(HistoGraph graph, int bar);
	}

	public class Bin {
		int	index = 0;
		double value = 0.0;

		public void reset() {
			index = 0;
			value = 0.0;
		}

		public void checkMin(int bin) {
			double val = getData(bin);
			if( val<value ) {
				value = val;
				index = bin;
				return;
			}
			if( bin==index && val>value ) {
				// minimum invalid - find new one
			}
		}

		public void checkMax(int bin) {
			double val = getData(bin);
			if( val>value ) {
				value = val;
				index = bin;
				return;
			}
			if( bin==index && val<value ) {
				// maximum invalid - find new one
			}
		}
	}

	// this is a quick and dirty implementation of bin markers - improvements?
	public class Marker {
		double	x;
		int		bin = -1;
		String	color;
		String	descr;
		int[]	linestyle;

		public Marker(double x, String color, String descr) {
			this(x, color, descr, null);
		}

		public Marker(double x, String color, String descr, int[] linestyle) {
			this.x = x;
			this.color = color;
			this.descr = descr;
			this.linestyle = linestyle;
		}
	}

	public void addMarker(double x, String color, String descr) {
		binmarkers.add(new Marker(x, color, descr));
	}

	public void addMarker(double x, String color, String descr, int[] linestyle) {
		binmarkers.add(new Marker(x, color, descr, linestyle));
	}

	public void clearMarkers() {
		binmarkers.clear();
	}

	public String getNoteAt(int bin) {
		for( Marker mark : binmarkers )
			if( mark.bin==bin ) return mark.descr;
		return null;
	}

	protected Module  module;

	private static final double[][] autoscale = new double[][] { //
		{ 1.0, 0.4, 4.0 }, //
		{ 0.5, 0.2, 5.0 }, //
		{ 0.25, 0.08, 5.0 }, //
		{ 0.1, 0.04, 5.0 }, //
		{ 0.05, 0.008, 5.0 }, //
		{ 0.01, 0.0, 5.0 }};
	private int autoscaleidx = 0;
	private ArrayList<Marker> binmarkers = new ArrayList<Marker>();

	/**
	 *
	 */
	public static final int MAX_BINS = 100;

	protected double[][] data;
	protected String message;
	protected boolean isNormalized = true;
	protected int normIdx = -1;
	protected double nSamples;

//	Bin binMin = new Bin();
//	Bin binMax = new Bin();

	/**
	 * Create new histogram graph for <code>module</code> running in
	 * <code>controller</code>. The tag is used to distinguish different graphs of
	 * the same module to visualize different components of the data and represents
	 * the index of the data column.
	 * 
	 * @param controller the controller of this graph
	 * @param module     the module who's data is shown
	 * @param tag        id of the graph
	 */
	public HistoGraph(Controller controller, Module module, int tag) {
		super(controller, tag);
		this.module = module;
		setStylePrimaryName("evoludo-HistoGraph");
	}

	public Module getModule() {
		return module;
	}

	public void setNormalized(boolean isNormalized) {
		this.isNormalized = isNormalized;
		if( isNormalized )
			normIdx = -1;
	}

	public void setNormalized(int normIdx) {
		this.normIdx = normIdx;
	}

	public double[][] getData() {
		return data;
	}

	public void setData(double[][] data) {
		this.data = data;
	}

	public double getData(int idx) {
		if( normIdx>=0 )
			return data[tag][idx]/Math.max(1.0, data[normIdx][idx]);
		if( !isNormalized )
			return data[tag][idx]/Math.max(1.0, nSamples);
		return data[tag][idx];
	}

	public double getSamples() {
		return nSamples;
	}

	public double getSamples(int idx) {
		if( data!=null && normIdx>=0 )
			return data[normIdx][idx];
		return nSamples;
	}

	public void addData(int bin) {
		nSamples++;
		if( data==null || bin<0 )
			return;
		data[tag][bin]++;
		if( normIdx>=0 )
			data[normIdx][bin]++;
//		binMin.checkMin(bin);
//		binMax.checkMax(bin);
	}

	public void addData(int bin, double incr) {
		nSamples++;
		if( data==null || bin<0 )
			return;
		data[tag][bin] += incr;
		if( normIdx>=0 )
			data[normIdx][bin]++;
//		binMin.checkMin(bin);
//		binMax.checkMax(bin);
	}

	public void addData(double x) {
		nSamples++;
		if( data==null )
			return;
		while( x>style.xMax ) {
			// double range (of affected type only - plus absorption if necessary)
			doubleBinRange();
			style.xMax = 2*style.xMax-style.xMin;	// double the range not the maximum
		}
		data[tag][x2bin(x)]++;
	}

	// IMPORTANT: bin count needs to be even!
	private void doubleBinRange() {
		double[] bins = data[tag];
		int nBins = bins.length;
		int nBins2 = nBins/2;
		for( int i=0; i<nBins2; i++ ) {
			int i2 = i+i;
			bins[i] = bins[i2]+bins[i2+1];
		}
		Arrays.fill(bins, nBins2, nBins, 0.0);
	}

	public int x2bin(double x) {
		int len = data[tag].length;
		// if x == style.xMax return len-1 (not len)
		return Math.min((int)((x-style.xMin)/(style.xMax-style.xMin)*len), len-1);
	}

	public double bin2x(int bin) {
//		return style.xMin+(double)bin/data[tag].length*(style.xMax-style.xMin);
		double x = (double)bin/data[tag].length;
//		return style.xMin+x*(style.xMax-style.xMin)+x;
		x = style.xMin+x*(style.xMax-style.xMin)+x;
		// would it be nice to round to nearest rational? 
		double rounded = Math.round(x);
		if( Math.abs(x-rounded)<1e-12 )
			return rounded;
		return x;
	}

	@Override
	public void reset() {
		super.reset();
		nSamples = 0.0;
		if( data==null )
			return;
		Arrays.fill(data[tag], 0.0);
		if( normIdx>=0 )
			Arrays.fill(data[normIdx], 0.0);
//		binMin.reset();
//		binMax.reset();
	}

//	/**
//	 *
//	 */
//    @Override
//	protected void initMenu() {
//		super.initMenu();
//		logScaleSeparator = new JPopupMenu.Separator();
//		menu.add(logScaleSeparator);
//		JCheckBoxMenuItem auto = new JCheckBoxMenuItem("Autoscale", doAutoscale);
//		auto.setActionCommand(MENU_AUTO_SCALE);
//		auto.addActionListener(this);
//		auto.setFont(style.menuFont);
//		menu.add(auto);
//		logXScaleMenu = new JCheckBoxMenuItem("X Log Scale", data.logx);
//		logXScaleMenu.setActionCommand(MENU_LOG_SCALE_X);
//		logXScaleMenu.addActionListener(this);
//		logXScaleMenu.setFont(style.menuFont);
//		menu.add(logXScaleMenu);
//		logYScaleMenu = new JCheckBoxMenuItem("Y Log Scale", data.logy);
//		logYScaleMenu.setActionCommand(MENU_LOG_SCALE_Y);
//		logYScaleMenu.addActionListener(this);
//		logYScaleMenu.setFont(style.menuFont);
//		menu.add(logYScaleMenu);
////disable by default for now...
////setLogScaleCMEnabled(false, false);
//	}

//	/**
//	 *
//	 * @param x
//	 * @param y
//	 */
//    public void setLogScaleCMEnabled(boolean x, boolean y) {
//		logScaleSeparator.setVisible(x || y);
//		logXScaleMenu.setVisible(x);
//		logYScaleMenu.setVisible(y);
//	}

//	@Override
//	public void actionPerformed(ActionEvent e) {
//		String cmd = e.getActionCommand();
//		if( cmd.equals(MENU_AUTO_SCALE) ) {
//			doAutoscale = ((JCheckBoxMenuItem)e.getSource()).getState();
//frame.init(getBounds());
//			updatescale = true;
//			repaint();
//			return;
//		}
//		if( cmd.equals(MENU_LOG_SCALE_X) ) {
//			data.logx = ((JCheckBoxMenuItem)e.getSource()).getState();
//frame.xaxis.logScale = data.logx;
//frame.init(getBounds());
////			updatescale = true;
//			data.timestamp = -1.0;	// make sure the data is re-read and processed
//			prepare();
//			repaint();
//			return;
//		}
//		if( cmd.equals(MENU_LOG_SCALE_Y) ) {
//			data.logy = ((JCheckBoxMenuItem)e.getSource()).getState();
//frame.yaxis.logScale = data.logy;
//frame.init(getBounds());
////			updatescale = true;
//			data.timestamp = -1.0;
//			prepare();
//			repaint();
//			return;
//		}
//		super.actionPerformed(e);
//	}

	@Override
	public void export(MyContext2d ctx) {
		MyContext2d bak = g;
		g = ctx;
		paint();
		g = bak;
	}

	@Override
	public boolean displayMessage(String msg) {
		message = msg;
		if (!super.displayMessage(message))
			return false;
		// add frame
		g.save();
		g.scale(scale,  scale);
		g.translate(bounds.getX(), bounds.getY());
		drawFrame(4, 4);
		g.restore();
		return true;
	}

	@Override
	public void clearMessage() {
		super.clearMessage();
		message = null;
	}

	@Override
	public void paint() {
		if (!isActive || displayMessage(message))
			return;

		g.save();
		g.scale(scale,  scale);
		clearCanvas();
		g.translate(bounds.getX(), bounds.getY());

		g.setFillStyle(style.graphColor);
		int yLevels = 4;
		double[] myData = data[tag];
		double[] norm = null;
		int nBins = myData.length;
		if( style.autoscaleY ) {
// Note: autoscaling is pretty slow - could be avoided by checks when adding new data points; increasing
//		 max is fairly easy; if max gets reduced a global check for new maximum is required; graph would
//		 need to keep track of bin index that contains the maximum.
			double yMax = -Double.MAX_VALUE, yMin = Double.MAX_VALUE;
			if( normIdx>=0 ) {
				norm = data[normIdx];
				boolean nodata = true;
				for( int n=0; n<nBins; n++ ) {
					double nnorm = norm[n];
					if( nnorm<1e-8 )
						continue;
					double val = myData[n]/nnorm;
					yMin = Math.min(yMin, val);
					yMax = Math.max(yMax, val);
					nodata = false;
				}
				if( nodata ) {
					yMax = 1.0;
					yMin = 0.0;
				}
			}
			else {
				yMax = ArrayMath.max(myData);
				if( !isNormalized )
					yMax /= nSamples;
			}
			if( yMax<=1.0 ) {
				// find maximum - assumes that data is normalized and max<=1!
				while( yMax>autoscale[autoscaleidx][0] )
					autoscaleidx--;
				while( yMax<autoscale[autoscaleidx][1] )
					autoscaleidx++;
				style.yMax = autoscale[autoscaleidx][0];
				yLevels = (int)autoscale[autoscaleidx][2];
			}
			else {
				// round yMax up to 'nice' boundary
				if( yMax>style.yMax )
					style.yMax = Functions.roundUp(yMax);
				else if( yMax<0.8*style.yMax )
					style.yMax = Functions.roundUp(yMax);		

				if( normIdx<0 ) {
					yMin = ArrayMath.min(myData);
					if( !isNormalized )
						yMin /= nSamples;
				}
				if( yMin<style.yMin )
					style.yMin = Functions.roundDown(yMin);
				else if( yMin>1.25*style.yMin )
					style.yMin = Functions.roundDown(yMin);		
			}
		}

		double barwidth = bounds.getWidth()/nBins;
		double xshift = 1;
		double h = bounds.getHeight();
		double map = h/(style.yMax-style.yMin);
		if( normIdx>=0 ) {
			norm = data[normIdx];
			for( int n=0; n<nBins; n++ ) {
				double nnorm = norm[n];
				double barheight = nnorm < 1e-8 ? 0.0 : Math.min(Math.max(1.0, map*(myData[n]/nnorm-style.yMin)), h-1);
				fillRect(xshift, h-barheight, barwidth-1.0, barheight);
				xshift += barwidth;
			}
		}
		else {
			if( !isNormalized )
				map /= nSamples; 
			for( int n=0; n<nBins; n++ ) {
				double barheight = Math.min(Math.max(1.0, map*(myData[n]-style.yMin)), h-1);
				fillRect(xshift, h-barheight, barwidth-1.0, barheight);
				xshift += barwidth;
			}
		}
		markBins();
		drawFrame(4, yLevels);
		g.restore();
		tooltip.update();
	}

	protected void markBins() {
		int nBins = data[tag].length;
		double barwidth = bounds.getWidth()/nBins;
		double h = bounds.getHeight();
		int nMarkers = binmarkers.size();
		int n = 0;
		for( Marker mark : binmarkers ) {
			int bin = x2bin(mark.x);
			mark.bin = bin;	// remember bin to easily retrieve potential notes for tooltips
			double x = bin*barwidth;
			g.setStrokeStyle(mark.color == null ? markerColors[n++ % nMarkers] : mark.color);
			if (mark.linestyle != null)
				g.setLineDash(mark.linestyle);
			strokeLine(x+1.0, 0.0, x+1.0, h);
			strokeLine(x+barwidth, 0.0, x+barwidth, h);
			g.setLineDash(style.solidLine);
		}
	}

	@Override
	protected boolean calcBounds() {
		if (!super.calcBounds())
			return false;
		int nBins = MAX_BINS;
		if( data!=null )
			nBins = data[tag].length;
		double w = bounds.getWidth();
		int barwidth = (int) ((w-2)/nBins);
		if( barwidth<=0 )
			// return success even though too small for bars
			return true;
		bounds.adjust(0, 0, barwidth*nBins-w, 0);
		return true;
	}

	public int getBinAt(int x, int y) {
		if( data==null )
			return -1;
		x -= (int)(3*style.frameWidth);
		if( !bounds.contains(x, y) )
			return -1;
		int nBins = data[tag].length;
		double ibarwidth = nBins/bounds.getWidth();
		return (int)((x-bounds.getX())*ibarwidth);
	}

	@Override
	public String getTooltipAt(int x, int y) {
		int bar = getBinAt(x, y);
		if( bar<0 )
			return null;
		//int n = graphs.indexOf(graph);
		return ((HistoGraphController)controller).getTooltipAt(this, bar);
	}

	private ContextMenuCheckBoxItem autoscaleYMenu;
	private boolean enableAutoscaleYMenu = true;

	public void enableAutoscaleYMenu(boolean enable) {
		enableAutoscaleYMenu = enable;
	}

	@Override
	public void populateContextMenuAt(ContextMenu menu, int x, int y) {
		// process autoscale context menu
		if( autoscaleYMenu==null ) {
			autoscaleYMenu = new ContextMenuCheckBoxItem("Autoscale y-axis", new Command() {
				@Override
				public void execute() {
					style.autoscaleY = !style.autoscaleY;
				}
			});
		}
		autoscaleYMenu.setChecked(style.autoscaleY);
		autoscaleYMenu.setEnabled(enableAutoscaleYMenu);
		menu.addSeparator();
		menu.add(autoscaleYMenu);
		super.populateContextMenuAt(menu, x, y);
	}
}
