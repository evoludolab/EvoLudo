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

import java.util.Arrays;

import org.evoludo.graphics.GraphStyle;
import org.evoludo.graphics.HistoGraph;
import org.evoludo.math.ArrayMath;
import org.evoludo.math.Combinatorics;
import org.evoludo.simulator.EvoLudoGWT;
import org.evoludo.simulator.geometries.AbstractGeometry;
import org.evoludo.simulator.geometries.GeometryFeatures;
import org.evoludo.simulator.geometries.GeometryType;
import org.evoludo.simulator.models.Data;
import org.evoludo.simulator.models.IBSPopulation;
import org.evoludo.simulator.modules.Module;

/**
 * Histograms of the degree distributions of the interaction and competition
 * structures. Undirected structures show one histogram, directed ones show the
 * in-, out- and total degree distributions. Different interaction and
 * competition structures show histograms for both. DE models display a short
 * explanatory message instead, unless the PDE structure is irregular.
 * <p>
 * The degree distributions are recomputed only for dynamic structures or if
 * not yet processed.
 *
 * @author Christoph Hauert
 */
public class DegreeHistogram extends Histogram {

	/**
	 * The flag to indicate whether the degree distributions have been processed.
	 */
	private boolean degreeProcessed;

	/**
	 * Construct a new view to display the degree distributions.
	 *
	 * @param engine the pacemaker for running the model
	 */
	public DegreeHistogram(EvoLudoGWT engine) {
		super(engine, Data.DEGREE);
	}

	@Override
	protected boolean isAutoscaleY() {
		return false;
	}

	@Override
	protected int[] getGraphRows(Module<?> module) {
		AbstractGeometry[] geometries = getGeometries(module);
		return rows(getDegreeLabels(geometries[0], geometries[1]).length, -1);
	}

	/**
	 * Get the interaction and competition geometries of {@code module}.
	 *
	 * @param module the module
	 * @return the interaction and competition geometries
	 */
	private AbstractGeometry[] getGeometries(Module<?> module) {
		if (!model.isIBS())
			return new AbstractGeometry[] { module.getGeometry(), module.getGeometry() };
		IBSPopulation<?, ?> ibspop = module.getIBSPopulation();
		return new AbstractGeometry[] { ibspop.getInteractionGeometry(), ibspop.getCompetitionGeometry() };
	}

	@Override
	public void reset(boolean hard) {
		degreeProcessed = false;
		super.reset(hard);
	}

	@Override
	protected int[] resetGraph(HistoGraph graph, int idx) {
		AbstractGeometry[] geometries = getGeometries(graph.getModule());
		AbstractGeometry inter = geometries[0];
		if (inter == null)
			return null;
		return new int[] { getDegreeLabels(inter, geometries[1]).length,
				Math.min(maxDegree(inter, geometries[1]), HistoGraph.DEFAULT_BINS) };
	}

	@Override
	protected boolean applyStyle(HistoGraph graph) {
		AbstractGeometry[] geometries = getGeometries(graph.getModule());
		AbstractGeometry inter = geometries[0];
		if (inter == null)
			return false;
		GraphStyle style = graph.getStyle();
		style.xLabel = "degree";
		style.label = getDegreeLabels(inter, geometries[1])[graph.getRow()];
		style.showLabel = (style.label != null);
		style.graphColor = "#444";
		style.xMin = 0.0;
		style.xMax = maxDegree(inter, geometries[1]);
		return false;
	}

	@Override
	protected void updateGraphs() {
		double[][] data = null;
		for (HistoGraph graph : graphs) {
			if (model.isDE() && displayDEMessage(graph))
				continue;
			// graphs of the same species share their data
			double[][] graphdata = graph.getData();
			if (graphdata == null || graphdata == data)
				continue;
			data = graphdata;
			AbstractGeometry[] geometries = getGeometries(graph.getModule());
			AbstractGeometry inter = geometries[0];
			AbstractGeometry comp = geometries[1];
			if (inter == null || comp == null || (degreeProcessed
					&& !inter.isType(GeometryType.DYNAMIC) && !comp.isType(GeometryType.DYNAMIC)))
				continue;
			int kmax = maxDegree(inter, comp);
			int nBins = Math.min(kmax, HistoGraph.DEFAULT_BINS);
			// the range of dynamic structures may change
			double[][] old = data;
			if (nBins != data[0].length)
				data = new double[data.length][nBins];
			for (HistoGraph g : graphs) {
				if (g.getData() != old)
					continue;
				g.setData(data);
				g.getStyle().xMax = kmax;
			}
			// the x-axis covers degrees [0, kmax) such that the first bin holds the
			// isolated nodes with degree zero
			double ibinwidth = (double) nBins / kmax;
			getDegreeHistogramData(data, inter, 0, ibinwidth);
			if (inter != comp)
				getDegreeHistogramData(data, comp, inter.isUndirected() ? 1 : 3, ibinwidth);
		}
		degreeProcessed = true;
	}

	@Override
	public String getTooltipAt(HistoGraph graph, int bar) {
		// the x-axis covers degrees [0, kmax)
		int kmax = (int) graph.getStyle().xMax;
		return getIntTooltip(graph, bar, kmax, kmax - 1, false);
	}

	/**
	 * Display a message on {@code graph} for DE models, except PDE models with
	 * irregular structures.
	 *
	 * @param graph the graph to display the message
	 * @return {@code true} if a message is displayed
	 */
	private boolean displayDEMessage(HistoGraph graph) {
		if (!model.isPDE()) {
			graph.displayMessage(model.getType().getKey() + " model: well-mixed population.");
			return true;
		}
		AbstractGeometry geometry = graph.getModule().getGeometry();
		if (!geometry.isRegular() && !geometry.isLattice())
			return false;
		graph.displayMessage("PDE model: " + (geometry.isRegular() ? "regular" : "lattice")
				+ " structure with degree " + (int) (geometry.getConnectivity() + 0.5)
				+ (geometry.isRegular() ? "." : " (fixed boundaries)."));
		return true;
	}

	/**
	 * Calculate the maximum degree for displaying the degree distributions of the
	 * interaction and competition geometries. This rounds the maximum degree up to
	 * {@code 10, 20, 50, 100, 200, 500, 1000} etc. The result is always strictly
	 * larger, i.e. all degrees fall inside the range {@code [0, maxDegree)}. For
	 * directed graphs the total degree determines the range because the in-, out-
	 * and total degree distributions share the same axis.
	 *
	 * @param inter the interaction graph
	 * @param comp  the competition graph
	 * @return the maximum degree to display
	 */
	private static int maxDegree(AbstractGeometry inter, AbstractGeometry comp) {
		int max = 0;
		for (AbstractGeometry geometry : new AbstractGeometry[] { inter, comp }) {
			GeometryFeatures features = geometry.getFeatures();
			max = Math.max(max, geometry.isUndirected() ? features.maxOut : features.maxTot);
		}
		int order = (int) Math.max(10, Combinatorics.pow(10, (int) Math.floor(Math.log10(max + 1.0))));
		int lead = max / order;
		return (lead < 1 ? 1 : lead < 2 ? 2 : lead < 5 ? 5 : 10) * order;
	}

	/**
	 * Generate the degree histograms for {@code geometry}. Undirected graphs fill
	 * row {@code idx}, directed graphs fill the rows {@code idx, idx + 1, idx + 2}
	 * with the in-, out- and total degree distributions, respectively.
	 *
	 * @param data      the data array for storing the histograms
	 * @param geometry  the graph
	 * @param idx       the row for the first histogram
	 * @param ibinwidth the scaling factor to map degrees to bins
	 */
	private static void getDegreeHistogramData(double[][] data, AbstractGeometry geometry, int idx,
			double ibinwidth) {
		boolean undirected = geometry.isUndirected();
		int nRows = undirected ? 1 : 3;
		for (int r = idx; r < idx + nRows; r++)
			Arrays.fill(data[r], 0.0);
		int maxBin = data[idx].length - 1;
		int nodeCount = geometry.getSize();
		for (int i = 0; i < nodeCount; i++) {
			int kin = geometry.kin[i];
			// guard against stale degree ranges of dynamic geometries
			data[idx][Math.min((int) (kin * ibinwidth), maxBin)]++;
			if (undirected)
				continue;
			int kout = geometry.kout[i];
			data[idx + 1][Math.min((int) (kout * ibinwidth), maxBin)]++;
			data[idx + 2][Math.min((int) ((kin + kout) * ibinwidth), maxBin)]++;
		}
		for (int r = idx; r < idx + nRows; r++)
			ArrayMath.multiply(data[r], 1.0 / nodeCount);
	}

	/**
	 * Get the labels for the degree distributions of the interaction and
	 * competition geometries, one per graph.
	 *
	 * @param inter the interaction geometry
	 * @param comp  the competition geometry
	 * @return the labels for the degree distributions
	 */
	private static String[] getDegreeLabels(AbstractGeometry inter, AbstractGeometry comp) {
		if (inter == null || inter == comp)
			return getGeometryLabels(inter, null);
		String[] interLabels = getGeometryLabels(inter, "interaction");
		String[] compLabels = getGeometryLabels(comp, "competition");
		String[] labels = Arrays.copyOf(interLabels, interLabels.length + compLabels.length);
		System.arraycopy(compLabels, 0, labels, interLabels.length, compLabels.length);
		return labels;
	}

	/**
	 * Get the labels for the degree distributions of {@code geometry}.
	 *
	 * @param geometry the geometry
	 * @param name     the name of the geometry or {@code null}
	 * @return the labels for the degree distributions
	 */
	private static String[] getGeometryLabels(AbstractGeometry geometry, String name) {
		if (geometry == null || geometry.isUndirected())
			return new String[] { name };
		String prefix = (name == null ? "" : name + " - ");
		return new String[] { prefix + "in", prefix + "out", prefix + "total" };
	}
}
