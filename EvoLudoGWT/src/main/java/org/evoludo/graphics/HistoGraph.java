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

	protected double[][] data;
	protected String message;
	protected boolean isNormalized = true;
	protected int normIdx = -1;
	protected double nSamples;
	int maxBinIdx;

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

	/**
	 * Set whether the data is normalized. If {@code isNormalized} is {@code true},
	 * the sum of the data in each bin is normalized to {@code 1.0}.
	 * <br>
	 * <strong>Note:</strong> This conflicts with {@link #setNormalized(int)}.
	 * 
	 * @param isNormalized {@code true} if the data is normalized, {@code false}
	 *                     otherwise
	 */
	public void setNormalized(boolean isNormalized) {
		this.isNormalized = isNormalized;
		if (isNormalized)
			normIdx = -1;
	}

	/**
	 * Set the index of the data row that is used for normalization. If multiple
	 * {@code HistoGraph}s share the same data, they all need to use the same row
	 * for normalization.
	 * <br>
	 * <strong>Note:</strong> This conflicts with {@link #setNormalized(boolean)}.
	 * 
	 * @param normIdx the index of the data row for normalization
	 */
	public void setNormalized(int normIdx) {
		this.normIdx = normIdx;
	}

	/**
	 * Get the number of samples for this histogram.
	 * 
	 * @return the number of samples
	 */
	public double getSamples() {
		return nSamples;
	}

	/**
	 * Get the total number of samples in bin with index {@code idx}. For data that is not normalized the number of samples is returned.
	 * 
	 * @param idx the index of the bin
	 * @return the number of samples
	 * 
	 * @see #getSamples()
	 */
	public double getSamples(int idx) {
		if (data != null && normIdx >= 0)
			return data[normIdx][idx];
		return nSamples;
	}

	/**
	 * Get the data for the histogram.
	 * <br>
	 * <strong>Note:</strong> The data may be shared by multiple
	 * {@code HistoGraph}s, each refering to a row in the {@code double[][]} array
	 * specified by the graphs {@code tag}.
	 * 
	 * @return the 2D data array for storing the histogram data
	 */
	public double[][] getData() {
		return data;
	}

	/**
	 * Get the data for the histogram in bin with index {@code idx}. Whether this
	 * returns raw or normalized data depends on the setting of the graph.
	 * 
	 * @param idx the index of the bin
	 * @return the histogram data for the bin {@code idx}
	 * 
	 * @see #setNormalized(boolean)
	 * @see #setNormalized(int)
	 */
	public double getData(int idx) {
		if (normIdx >= 0)
			return data[tag][idx] / Math.max(1.0, data[normIdx][idx]);
		if (!isNormalized)
			return data[tag][idx] / Math.max(1.0, nSamples);
		return data[tag][idx];
	}

	/**
	 * Set the data for the histogram. 
	 * <br>
	 * <strong>Note:</strong> The data may be shared by multiple
	 * {@code HistoGraph}s, each refering to a row in the {@code double[][]} array
	 * specified by the graphs {@code tag}.
	 * 
	 * @param data the 2D data array for storing the histogram data
	 */
	public void setData(double[][] data) {
		this.data = data;
		if (data != null && data.length > 0 && data[0] != null)
			maxBinIdx = data[0].length - 1;
		else
			maxBinIdx = -1;
	}

	/**
	 * Add data point to histogram by increasing the count of the bin with index
	 * {@code bin} by one. For normalized data the normalization is updated
	 * accordingly.
	 * 
	 * @param bin the index of the bin in the histogram
	 * 
	 * @see #setNormalized(boolean)
	 * @see #setNormalized(int)
	 */
	public void addData(int bin) {
		nSamples++;
		if (data == null || bin < 0)
			return;
		if (bin > maxBinIdx)
			bin = maxBinIdx;
		data[tag][bin]++;
		if (normIdx >= 0)
			data[normIdx][bin]++;
	}

	/**
	 * Add data point to histogram by increasing the count of the bin with index
	 * {@code bin} by {@code incr}. For normalized data the normalization is updated
	 * accordingly.
	 * 
	 * @param bin the index of the bin in the histogram
	 * @param incr the increment to add to the bin
	 * 
	 * @see #setNormalized(boolean)
	 * @see #setNormalized(int)
	 */
	public void addData(int bin, double incr) {
		nSamples++;
		if (data == null || bin < 0)
			return;
		data[tag][bin] += incr;
		if (normIdx >= 0)
			data[normIdx][bin]++;
	}

	/**
	 * Add data point {@code x} to histogram. The data point is added to the
	 * histogram for \(x\in[x_\text{min},x_\text{max}]\). If \(x&gt;x_\text{max}]\)
	 * or \(x&lt;x_\text{min}]\) the range is doubled to \([x_\text{min},
	 * x_\text{max} + (x_\text{max}-x_\text{min})]\)) or \([x_\text{min} -
	 * (x_\text{max}-x_\text{min}), x_\text{max}]\)), respectively. The number of
	 * bins remains unchanged.
	 * <br>
	 * <strong>Important:</strong> The number of bins needs to be even!
	 * 
	 * @param x the new data point
	 * 
	 * @see #x2bin(double)
	 */
	public void addData(double x) {
		nSamples++;
		if (data == null)
			return;
		while (x > style.xMax) {
			doubleMaxRange();
			style.xMax = 2 * style.xMax - style.xMin;
		}
		while (x < style.xMin) {
			doubleMinRange();
			style.xMin = 2 * style.xMin - style.xMax;
		}
		data[tag][x2bin(x)]++;
	}

	/**
	 * Clear the histrogram data.
	 */
	public void clearData() {
		if (data == null)
			return;
		for (double[] entry : data)
			Arrays.fill(entry, 0.0);
		nSamples = 0.0;
	}

	// IMPORTANT: bin count needs to be even!
	private void doubleMinRange() {
		double[] bins = data[tag];
		int nBins = bins.length;
		int nBins2 = nBins/2;
		for( int i=0; i<nBins2; i++ ) {
			int i2 = nBins - 1 - i - i;
			bins[i] = bins[i2]+bins[i2-1];
		}
		Arrays.fill(bins, 0, nBins2, 0.0);
	}

	// IMPORTANT: bin count needs to be even!
	private void doubleMaxRange() {
		double[] bins = data[tag];
		int nBins = bins.length;
		int nBins2 = nBins/2;
		for( int i=0; i<nBins2; i++ ) {
			int i2 = i+i;
			bins[i] = bins[i2]+bins[i2+1];
		}
		Arrays.fill(bins, nBins2, nBins, 0.0);
	}

	/**
	 * Convert {@code x}-coordinate to bin index.
	 * 
	 * @param x the {@code x}-coordinate
	 * @return the index of the bin 
	 */
	public int x2bin(double x) {
		int len = data[tag].length;
		// if x == style.xMax return len-1 (not len)
		return Math.min((int) ((x - style.xMin) / (style.xMax - style.xMin) * len), len - 1);
	}

	/**
	 * Convert the bin index to {@code x}-coordinate.
	 * 
	 * @param bin the index of the bin
	 * @return the {@code x}-coordinate
	 */
	public double bin2x(int bin) {
		double x = (double) bin / data[tag].length;
		x = style.xMin + x * (style.xMax - style.xMin) + x;
		// would it be nice to round to nearest rational?
		double rounded = Math.round(x);
		if (Math.abs(x - rounded) < 1e-12)
			return rounded;
		return x;
	}

	@Override
	public void reset() {
		super.reset();
		nSamples = 0.0;
		if (data == null)
			return;
		Arrays.fill(data[tag], 0.0);
		if (normIdx >= 0)
			Arrays.fill(data[normIdx], 0.0);
	}

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
		if (!isActive || !doUpdate() || displayMessage(message))
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
		double xshift = 0;
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
		drawMarkers();
		drawFrame(4, yLevels);
		g.restore();
		tooltip.update();
	}

	protected void drawMarkers() {
		int nBins = data[tag].length;
		double barwidth = bounds.getWidth()/nBins;
		double h = bounds.getHeight();
		int nMarkers = binmarkers.size();
		int n = 0;
		for( Marker mark : binmarkers ) {
			int bin = x2bin(mark.x);
			mark.bin = bin;	// remember bin to easily retrieve notes for tooltips (if any)
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
		if (!super.calcBounds() || data == null)
			return false;
		int nBins = data[tag].length;
		double w = bounds.getWidth();
		int barwidth = (int) ((w - 2) / nBins);
		if (barwidth <= 0)
			// return success even though too small for bars
			return true;
		bounds.adjust(0, 0, barwidth * nBins - w, 0);
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
