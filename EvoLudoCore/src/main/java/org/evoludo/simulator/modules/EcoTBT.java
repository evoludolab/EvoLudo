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

package org.evoludo.simulator.modules;

import java.awt.Color;
import java.util.Arrays;

import org.evoludo.geom.Point2D;
import org.evoludo.simulator.EvoLudo;
import org.evoludo.simulator.views.HasPhase2D;
import org.evoludo.simulator.views.HasS3;
import org.evoludo.util.Formatter;

/**
 *
 * @author Christoph Hauert
 */
public class EcoTBT extends TBT implements HasS3, HasPhase2D {

	/**
	 * Create a new instance of the module for ecological {@code 2×2} games.
	 * 
	 * @param engine the manager of modules and pacemeaker for running the model
	 */
	public EcoTBT(EvoLudo engine) {
		super(engine);
	}

	@Override
	public void load() {
		super.load(); // TBT sets names and colors already but no harm in it
		nTraits = 3;
		VACANT = 2;
		// trait names
		String[] names = new String[nTraits];
		names[DEFECT] = "Defector";
		names[COOPERATE] = "Cooperator";
		names[VACANT] = "Vacant";
		setTraitNames(names);
		// trait colors (automatically generates lighter versions for new strategists)
		Color[] colors = new Color[2 * nTraits];
		colors[DEFECT] = Color.RED;
		colors[COOPERATE] = Color.BLUE;
		colors[VACANT] = Color.GRAY;
		colors[DEFECT + nTraits] = Color.YELLOW;
		colors[COOPERATE + nTraits] = Color.GREEN;
		colors[VACANT + nTraits] = Color.LIGHT_GRAY;
		setTraitColors(colors);
	}

	@Override
	public String getKey() {
		return "e2x2";
	}

	@Override
	public String getInfo() {
		return "Title: " + getTitle() + "\nAuthor: Christoph Hauert\n2x2 games with variable population sizes.";
	}

	@Override
	public String getTitle() {
		return "Ecological 2x2 games";
	}

	@Override
	public String getVersion() {
		return "v1.0 March 2021";
	}

	@Override
	public int getDependent() {
		// note: needed even though VACANT is the default dependent because TBT
		//		 dedicates DEFECT as the dependent.
		return VACANT;
	}

	@Override
	public double pairScores(int me, int[] tCount, double[] tScore) {
		if (me != VACANT)
			return super.pairScores(me, tCount, tScore);
		// site is vacant
		Arrays.fill(tScore, 0.0);
		return Double.NaN;
	}

	/**
	 * for ecological updates deals with the case where the population is reduced to
	 * a single individual and hence no interactions.
	 */
	@Override
	public void mixedScores(int[] count, double[] traitScores) {
		// note: cannot simply check getPopulationSize because this also needs to work
		// for well-mixed demes
		if (count[COOPERATE] + count[DEFECT] <= 1) {
			// only single individual left - no interactions
			traitScores[COOPERATE] = traitScores[DEFECT] = 0.0;
			return;
		}
		super.mixedScores(count, traitScores);
	}

	EcoTBTMap map;

	@Override
	public Data2Phase getMap() {
		map = new EcoTBTMap();
		return map;
	}

	public class EcoTBTMap implements Data2Phase {

		@Override
		public boolean data2Phase(double[] data, Point2D point) {
			// NOTE: data[0] is time!
			point.x = 1.0 - data[VACANT + 1];
			point.y = Math.min(1.0, data[COOPERATE + 1] / (1.0 - data[VACANT + 1]));
			return true;
		}

		@Override
		public boolean phase2Data(Point2D point, double[] data) {
			data[VACANT] = 1.0 - point.x;
			data[DEFECT] = point.x * (1.0 - point.y);
			data[COOPERATE] = point.x * point.y;
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
			String tip = "<table><tr><td style='text-align:right'><i>" + getXAxisLabel() + ":</i></td><td>"
					+ Formatter.formatPercent(x, 2) + "</td></tr>";
			tip += "<tr><td style='text-align:right'><i>" + getYAxisLabel() + ":</i></td><td>"
					+ Formatter.formatPercent(y, 2) + "</td></tr>";
			tip += "<tr><td colspan='2' style='font-size:1pt'><hr/></td></tr>";
			tip += "<tr><td style='text-align:right'><i>" + getTraitName(VACANT) + ":</i></td><td>"
					+ Formatter.formatPercent(1.0 - x, 2) + "</td></tr>";
			tip += "<tr><td style='text-align:right'><i>" + getTraitName(DEFECT) + ":</i></td><td>"
					+ Formatter.formatPercent(x * (1.0 - y), 2) + "</td></tr>";
			tip += "<tr><td style='text-align:right'><i>" + getTraitName(COOPERATE) + ":</i></td><td>"
					+ Formatter.formatPercent(x * y, 2) + "</td></tr>";
			tip += "</table>";
			return tip;
		}
	}

//this does not (yet?) make much sense...
//	@Override
//	public void init(Model model) {
//		super.init(model);
//		if (!model.isModelType(Model.Type.IBS))
//			return;
//		DPopulation dpop = (DPopulation)model;
//		int[] strat = dpop.strategies;
//		int nPop = strat.length;
//		int[] stratCount = dpop.getStrategiesTypeCount();
//		double pMut = dpop.getMutationProb();
//		FixationData fix = dpop.getFixationData();
//		// place a single individual with the opposite strategy
//		if( model.isMode(Mode.STATISTICS) ) {
//			if( stratCount[COOPERATE]==0 && stratCount[DEFECT]==0 ) {
//				// VACANT population
//				fix.initialNode = dpop.random0n(nPop);
//				int type = dpop.random0n(2)==1?DEFECT:COOPERATE;
//				stratCount[type] = 1;
//				stratCount[VACANT]--;
//				return;
//			}
//		}
//	}
}
