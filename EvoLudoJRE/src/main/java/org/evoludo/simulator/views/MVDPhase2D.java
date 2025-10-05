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
//			(doi: 10.5281/zenodo.14591549 [, <version>])
//
//	<year>:    year of release (or download), and
//	<version>: optional version number (as reported in output header
//			or GUI console) to simplify replication of reported results.
//
// The formatting may be adjusted to comply with publisher requirements.
//

package org.evoludo.simulator.views;

import java.util.List;

import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;

import org.evoludo.geom.Point2D;
import org.evoludo.graphics.AbstractGraph;
import org.evoludo.graphics.GraphAxis;
import org.evoludo.graphics.ParaGraph;
import org.evoludo.graphics.StateData;
import org.evoludo.graphics.StateGraphListener;
import org.evoludo.simulator.EvoLudoLab;
import org.evoludo.simulator.models.Model;
import org.evoludo.simulator.modules.Module;
import org.evoludo.simulator.views.HasPhase2D.Data2Phase;
import org.evoludo.util.Formatter;

public class MVDPhase2D extends MVAbstract implements StateGraphListener {

	private static final long serialVersionUID = 20110423L;
	protected ParaGraph graph;
	protected Data2Phase map;

	// NOTE: maybe we should keep track of the number of active traits?

	public MVDPhase2D(EvoLudoLab lab) {
		super(lab);
	}

	@Override
	public void reset(boolean clear) {
		if (!graphs.isEmpty()) {
			super.reset(clear);
			return;
		}
		graph = new ParaGraph(this, module);
		if (map == null) {
			map = ((HasPhase2D) module).getPhase2DMap();
			if (map == null) {
				map = new TraitMap();
				((HasPhase2D) module).setPhase2DMap(map);
			}
			graph.setMap(map);
		}

		GraphAxis x = graph.getXAxis();
		GraphAxis y = graph.getYAxis();
		String label = map.getXAxisLabel();
		x.label = (label == null ? getXAxisLabel() : label);
		x.showLabel = true;
		x.max = 1.0;
		x.min = 0.0;
		x.majorTicks = 3;
		x.minorTicks = 1;
		label = map.getYAxisLabel();
		y.label = (label == null ? getYAxisLabel() : label);
		y.showLabel = true;
		y.max = 1.0;
		y.min = 0.0;
		y.majorTicks = 3;
		y.minorTicks = 1;

		JScrollPane scroll = new JScrollPane(graph, ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER,
				ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		scroll.getViewport().setOpaque(false);
		scroll.setOpaque(false);
		graph.setOpaque(false);
		add(scroll);
		graphs.add(graph);
		super.reset(true);
	}

	// implement GraphListener - mostly done in MVAbstract
	@Override
	public void initCustomMenu(JPopupMenu menu, AbstractGraph owner) {
		super.initCustomMenu(menu, owner);
		// initCMShowLocalDynamics(menu, owner);
		initCMTimeReversed(menu, owner);
	}

	@Override
	public void getData(StateData data, int tag) {
		Model model = engine.getModel();
		int totTraits = 0;
		for (Module mod : module.getSpecies())
			totTraits += mod.getNTraits();
		double[] mean = new double[totTraits + 1];
		model.getMeanTraits(mean);
		System.arraycopy(mean, 0, mean, 1, totTraits);
		data.time = model.getUpdates();
		mean[0] = data.time;
		Point2D now = new Point2D();
		data.time = model.getUpdates();
		data.connect = map.data2Phase(mean, now);
		data.now.setLocation(now.getX(), 1.0 - now.getY());
	}

	// retire setting of initial state in phase planes (outdated JRE)
	// @Override
	// public void setState(double[] loc) {
	// Point2D xy = new Point2D(loc[0], 1.0 - loc[1]);
	// int totTraits = 0;
	// for (Module mod : module.getSpecies())
	// totTraits += mod.getNTraits();
	// double[] init = new double[totTraits];
	// map.phase2Data(xy, init);
	// ((Discrete)module).setInit(init);
	// // check required to transfer initial frequencies to ODE/SDE/PDE
	// engine.modelCheck();
	// engine.modelReinit();
	// }

	private String getXAxisLabel() {
		int[] traitX = map.getTraitsX();
		StringBuilder xLabel = new StringBuilder(getTraitName(traitX[0]));
		int nx = traitX.length;
		if (nx > 1) {
			for (int n = 1; n < nx; n++)
				xLabel.append("+").append(getTraitName(traitX[n]));
		}
		return xLabel.toString();
	}

	private String getYAxisLabel() {
		int[] traitY = map.getTraitsY();
		StringBuilder yLabel = new StringBuilder(getTraitName(traitY[0]));
		int ny = traitY.length;
		if (ny > 1) {
			for (int n = 1; n < ny; n++)
				yLabel.append("+").append(getTraitName(traitY[n]));
		}
		return yLabel.toString();
	}

	private String getTraitName(int idx) {
		List<? extends Module> species = module.getSpecies();
		int nSpecies = species.size();
		if (nSpecies > 1) {
			for (Module mod : species) {
				int nTraits = mod.getNTraits();
				if (idx < nTraits)
					return mod.getName() + ": " + mod.getTraitName(idx);
				idx -= nTraits;
			}
			// trait not found... should not get here!
			return null;
		}
		return module.getTraitName(idx);
	}

	@Override
	public String getToolTipText(Point2D loc, int tag) {
		if (map instanceof BasicTooltipProvider)
			return "<html>" + ((BasicTooltipProvider) map).getTooltipAt(loc.getX(), loc.getY());
		return null;
	}

	@Override
	protected String getFilePrefix() {
		return "phase";
	}

	// implement MultiViewPanel - mostly done in MVAbstract
	@Override
	public String getName() {
		return "Strategy - Phase 2D";
	}

	public class TraitMap implements Data2Phase, BasicTooltipProvider {

		@Override
		public boolean data2Phase(double[] data, Point2D point) {
			// NOTE: data[0] is time!
			point.set(1.0 - data[3],
					Math.min(1.0, 1.0 - data[1] / (1.0 - data[3])));
			return true;
		}

		@Override
		public boolean phase2Data(Point2D point, double[] data) {
			double x = point.getX();
			double y = point.getY();
			data[2] = 1.0 - x;
			data[1] = x * (1.0 - y);
			data[0] = x * y;
			return true;
		}

		@Override
		public String getXAxisLabel() {
			return "population density";
		}

		@Override
		public String getYAxisLabel() {
			return "relative fraction of cooperators";
		}

		@Override
		public String getTooltipAt(double x, double y) {
			String tip = "<table><tr><td style='text-align:right'><i>" + getXAxisLabel() //
					+ ":</i></td><td>" + Formatter.formatPercent(x, 2) + "</td></tr>";
			tip += "<tr><td style='text-align:right'><i>" + getYAxisLabel() //
					+ ":</i></td><td>" + Formatter.formatPercent(y, 2) + "</td></tr>";
			tip += "<tr><td colspan='2' style='font-size:1pt'><hr/></td></tr>";
			tip += "<tr><td style='text-align:right'><i>" + getTraitName(2) + ":</i></td><td>"
					+ Formatter.formatPercent(1.0 - x, 2) + "</td></tr>";
			tip += "<tr><td style='text-align:right'><i>" + getTraitName(1) + ":</i></td><td>"
					+ Formatter.formatPercent(x * (1.0 - y), 2) + "</td></tr>";
			tip += "<tr><td style='text-align:right'><i>" + getTraitName(0) + ":</i></td><td>"
					+ Formatter.formatPercent(x * y, 2) + "</td></tr>";
			tip += "</table>";
			return tip;
		}

		protected String getTraitName(int idx) {
			List<? extends Module> species = module.getSpecies();
			int nSpecies = species.size();
			if (nSpecies > 1) {
				for (Module mod : species) {
					int nTraits = mod.getNTraits();
					if (idx < nTraits)
						return mod.getName() + ": " + mod.getTraitName(idx);
					idx -= nTraits;
				}
				// trait not found... should not get here!
				return null;
			}
			return module.getTraitName(idx);
		}
	}
}
