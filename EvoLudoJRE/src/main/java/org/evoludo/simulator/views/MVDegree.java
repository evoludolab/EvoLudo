//
// EvoLudo Project
//
// Copyright 2010-2026 Christoph Hauert
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
//			(doi: 10.5281/zenodo.14591549 [, <version>])
//
//	<year>:    year of release (or download), and
//	<version>: optional version number (as reported in output header
//			or GUI console) to simplify replication of reported results.
//
// The formatting may be adjusted to comply with publisher requirements.
//

package org.evoludo.simulator.views;

import java.awt.Color;
import java.awt.Component;
import java.util.Arrays;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JLabel;
import javax.swing.SwingConstants;

import org.evoludo.graphics.AbstractGraph;
import org.evoludo.graphics.GraphAxis;
import org.evoludo.graphics.GraphStyle;
import org.evoludo.graphics.HistoData;
import org.evoludo.graphics.HistoGraph;
import org.evoludo.graphics.HistoGraphListener;
import org.evoludo.math.ArrayMath;
import org.evoludo.math.Combinatorics;
import org.evoludo.simulator.EvoLudoLab;
import org.evoludo.simulator.geometries.AbstractGeometry;
import org.evoludo.simulator.geometries.GeometryFeatures;
import org.evoludo.simulator.geometries.GeometryType;

public class MVDegree extends MVAbstract implements HistoGraphListener {

	private static final long serialVersionUID = 20110423L;

	static final String xLabelText = "degree";
	private final JLabel xLabel;
	private boolean isStatic = false, isDirected = false;

	protected static final int K_OUT = 0;
	protected static final int K_IN = 1;
	protected static final int K_TOT = 2;
	protected static final int MAX_BINS = 100;

	public MVDegree(EvoLudoLab lab) {
		super(lab);
		xLabel = new JLabel(xLabelText, SwingConstants.RIGHT);
		xLabel.setAlignmentX(0.5f);
		add(xLabel);
	}

	private void addGraph(int row) {
		HistoGraph graph = new HistoGraph(this, module, row);
		GraphAxis x = graph.getXAxis();
		x.label = xLabelText;
		x.showLabel = false;
		x.grid = 0;
		x.majorTicks = 3;
		x.minorTicks = 1;
		x.min = 0.0;
		x.max = 10.0;
		// assist the layout manager...
		graph.setAlignmentX(Component.CENTER_ALIGNMENT);
		// insert graph before xLabel
		Component[] comps = getComponents();
		for (int c = 0; c < getComponentCount(); c++) {
			if (comps[c] != xLabel)
				continue;
			add(Box.createVerticalStrut(4), c);
			add(graph, c);
			break;
		}
		graphs.add(graph);
	}

	@Override
	public void reset(boolean clear) {
		AbstractGeometry geometry;
		switch (engine.getModel().getType()) {
			case ODE:
			case SDE:
				super.reset(clear);
				return;
			case PDE:
				geometry = module.getGeometry();
				break;
			case IBS:
			default:
				geometry = module.getIBSPopulation().getInteractionGeometry();
				break;
		}
		isStatic = !geometry.isType(GeometryType.DYNAMIC);
		if (graphs.isEmpty())
			addGraph(K_OUT);
		if (isDirected == geometry.isUndirected()) {
			// graph changed from directed to undirected or vice versa
			if (isDirected) {
				// was directed: 3 views present - disable two
				graphs.get(K_IN).setVisible(false);
				graphs.get(K_TOT).setVisible(false);
			} else {
				// was undirected: possibly only 1 view present
				if (graphs.size() == 3) {
					// 3 views present - enable two
					graphs.get(K_IN).setVisible(true);
					graphs.get(K_TOT).setVisible(true);
				} else {
					addGraph(K_IN);
					addGraph(K_TOT);
				}
				// invalidate layout - otherwise newly added graphs will have height zero!
				for (int n = 0; n < 3; n++)
					graphs.get(n).setSize(0, 0);
			}
			isDirected = !geometry.isUndirected();
			clear = true;
		}
		// NOTE: reset triggers a call to getData - above ensures that everything is
		// ready
		super.reset(clear);
	}

	@Override
	public void initStyle(GraphStyle style, AbstractGraph owner) {
		super.initStyle(style, owner);
		xLabel.setFont(style.labelFont);
		xLabel.setBorder(
				BorderFactory.createEmptyBorder(0, 0, 0, getFontMetrics(style.labelFont).stringWidth(xLabelText)));
	}

	/*
	 * // implement HistoGraphListener - mostly done in MVAbstract
	 * 
	 * @Override
	 * public boolean verifyXAxis(GraphAxis x, int tag) {
	 * boolean changed = false;
	 * double min = population.getMinScore();
	 * if( Math.abs(x.min-min)>1e-8 ) {
	 * x.min = min;
	 * changed = true;
	 * }
	 * double max = population.getMaxScore();
	 * if( Math.abs(x.max-max)>1e-8 ) {
	 * x.max = max;
	 * changed = true;
	 * }
	 * return changed;
	 * }
	 */

	/**
	 * Calculate the maximum degree for displaying the degree distribution. This
	 * rounds {@code max} up to {@code 10, 20, 50, 100, 200, 500, 1000} etc. The
	 * result is always strictly larger than {@code max}, i.e. the degrees
	 * {@code 0, 1, ..., max} all fall inside the range {@code [0, kmax)}.
	 *
	 * @param max the maximum degree of the graph
	 * @return the maximum degree to display
	 */
	private static int kmax(int max) {
		// determine range and number of bins required
		// range starts at 0 up to 10, 20, 50, 100, 200, 500, 1000 etc.
		// the number of bins is at most 100
		int order = Math.max(10, (int) Combinatorics.pow(10, (int) Math.floor(Math.log10(max + 1))));
		switch (max / order) {
			default:
			case 0:
				return order;
			case 1:
				return 2 * order;
			case 2:
			case 3:
			case 4:
				return 5 * order;
			case 5:
			case 6:
			case 7:
			case 8:
			case 9:
				return 10 * order;
		}
	}

	/**
	 * Process the degrees {@code in} and store the resulting distribution in
	 * {@code data}. The x-axis covers the degrees {@code [0, kmax)} such that the
	 * first bin holds the isolated nodes with degree zero.
	 *
	 * @param data the histogram data
	 * @param in   the degrees of all nodes (or {@code null} if unavailable)
	 * @param max  the maximum degree of the graph
	 * @return {@code true} if the layout of the histogram changed
	 */
	private static boolean process(HistoData data, int[] in, int max) {
		// boolean changed = false;
		int kmax = kmax(max);
		int b = Math.min(kmax, MAX_BINS);
		if (b > data.state.length)
			data.state = new double[b];
		/*
		 * if( b>data.state.length ) {
		 * data.state = new double[b];
		 * changed = true;
		 * }
		 */
		// note kmax = 10, 20, 50, 100, 200, 500, 1000, ...
		int binwidth = (kmax <= MAX_BINS ? 1 : kmax / MAX_BINS);
		double[] out = data.state;
		Arrays.fill(out, 0.0);
		if (in == null)
			return true;
		int n = in.length;
		for (int i = 0; i < n; i++)
			// guard against stale degree ranges of dynamic geometries
			out[Math.min(in[i] / binwidth, b - 1)]++;
		// return ( changed || changed(out) );

		double minBin = Double.MAX_VALUE, maxBin = -Double.MAX_VALUE, accuBin = 0.0;
		for (int i = 0; i < b; i++) {
			double aBin = out[i];
			if (aBin > 0) {
				minBin = Math.min(minBin, aBin);
				maxBin = Math.max(maxBin, aBin);
				accuBin += aBin;
			}
		}
		double norm = 100.0 / accuBin;
		boolean changed = false;
		if (data.bins != b) {
			data.bins = b;
			changed = true;
		}
		if (data.binwidth != binwidth) {
			data.binwidth = binwidth;
			changed = true;
		}
		if (Math.abs(data.xmin - 0) > 1e-8) {
			data.xmin = 0;
			changed = true;
		}
		if (Math.abs(data.xmax - kmax) > 1e-8) {
			data.xmax = kmax;
			changed = true;
		}
		if (Math.abs(data.ymin - minBin * norm) > 1e-8) {
			data.ymin = minBin * norm;
			if (data.logy && data.ymin <= 0.0)
				data.ymin = 1.0 / accuBin;
			changed = true;
		}
		if (Math.abs(data.ymax - maxBin * norm) > 1e-8) {
			data.ymax = maxBin * norm;
			changed = true;
		}
		if (data.logy) {
			ArrayMath.log10(out);
			ArrayMath.add(out, Math.log10(norm));
		} else
			ArrayMath.multiply(out, norm);
		return changed;
	}

	/*
	 * needs more thought...
	 * private static boolean changed(int[] out) {
	 * double minBin = Double.MAX_VALUE, maxBin = -Double.MAX_VALUE, accuBin = 0.0;
	 * int len = out.length;
	 * for( int i=0; i<len; i++ ) {
	 * double aBin = out[i];
	 * if( aBin>0 ) {
	 * minBin = Math.min(minBin, aBin);
	 * maxBin = Math.max(maxBin, aBin);
	 * accuBin += aBin;
	 * }
	 * }
	 * double norm = 100.0/accuBin;
	 * boolean changed = false;
	 * if( data.bins!=b ) {
	 * data.bins = b;
	 * changed = true;
	 * }
	 * if( data.binwidth!=binwidth ) {
	 * data.binwidth = binwidth;
	 * changed = true;
	 * }
	 * if( Math.abs(data.xmin-0)>1e-8 ) {
	 * data.xmin = 0;
	 * changed = true;
	 * }
	 * if( Math.abs(data.xmax-kmax)>1e-8 ) {
	 * data.xmax = kmax;
	 * changed = true;
	 * }
	 * if( Math.abs(data.ymin-minBin*norm)>1e-8 ) {
	 * data.ymin = minBin*norm;
	 * if( data.logy && data.ymin<=0.0 ) data.ymin = 1.0/accuBin;
	 * changed = true;
	 * }
	 * if( Math.abs(data.ymax-maxBin*norm)>1e-8 ) {
	 * data.ymax = maxBin*norm;
	 * changed = true;
	 * }
	 * if( data.logy ) {
	 * ChHMath.log10(out);
	 * ChHMath.add(out, Math.log10(norm));
	 * }
	 * else
	 * ChHMath.multiply(out, norm);
	 * return changed;
	 * }
	 */

	/**
	 * Calculate the total degrees, i.e. the sum of the in- and out-degrees, of all
	 * nodes in {@code geometry}.
	 *
	 * @param geometry the graph
	 * @return the total degrees of all nodes (or {@code null} if unavailable)
	 */
	private static int[] totalDegrees(AbstractGeometry geometry) {
		int[] kin = geometry.kin;
		int[] kout = geometry.kout;
		if (kin == null || kout == null)
			return null;
		int nNodes = kout.length; // has to be the same as kin.length
		int[] ktot = new int[nNodes];
		for (int i = 0; i < nNodes; i++)
			ktot[i] = kin[i] + kout[i];
		return ktot;
	}

	@Override
	public synchronized boolean getData(HistoData data, int tag) {
		// check if we need to process data first
		double now = engine.getModel().getUpdates();
		boolean changed = false;
		// System.out.println("MVDegree - getData: tag="+tag);
		if (data.timestamp < 0.0 || (now - data.timestamp > 1e-10 && !isStatic)) {
			AbstractGeometry geometry;
			if (engine.getModel().isIBS())
				geometry = module.getIBSPopulation().getInteractionGeometry();
			else
				geometry = module.getGeometry();
			GeometryFeatures features = geometry.getFeatures();

			switch (tag) {
				// case K_OUT:
				default:
					changed = process(data, geometry.kout, features.maxOut);
					break;

				case K_IN:
					changed = process(data, geometry.kin, features.maxIn);
					break;

				case K_TOT:
					changed = process(data, totalDegrees(geometry), features.maxTot);
					break;
			}
		}
		data.timestamp = now;
		return changed;
	}

	@Override
	public String getName(int tag) {
		if (!isDirected)
			return "Links";
		switch (tag) {
			case K_OUT:
				return "Outgoing";
			case K_IN:
				return "Incoming";
			case K_TOT:
				return "Total";
			default:
				return "Unknown";
		}
	}

	// use default color
	@Override
	public Color getColor(int tag) {
		return null;
	}

	@Override
	protected String getFilePrefix() {
		return "degree";
	}

	// implement MultiViewPanel - mostly done in MVAbstract
	@Override
	public String getName() {
		return "Degree - Histogram";
	}
}
