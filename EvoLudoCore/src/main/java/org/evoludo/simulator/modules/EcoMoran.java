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

import org.evoludo.geom.Point2D;
import org.evoludo.simulator.EvoLudo;
import org.evoludo.simulator.views.HasPhase2D;
import org.evoludo.simulator.views.HasS3;
import org.evoludo.util.Formatter;

/**
 *
 * @author Christoph Hauert
 */
public class EcoMoran extends Moran implements HasPhase2D, HasS3 {

	// local variables
	double[] mean;

	public EcoMoran(EvoLudo engine) {
		super(engine);
	}

	@Override
	public void load() {
		super.load(); // Moran sets names, colors and scores already but no harm in it
		nTraits = 3;
		VACANT = 2;
		// trait names
		String[] names = new String[nTraits];
		names[RESIDENT] = "Resident";
		names[MUTANT] = "Mutant";
		names[VACANT] = "Vacant";
		setTraitNames(names);
		// trait colors (automatically generates lighter versions for new strategists)
		Color[] colors = new Color[nTraits];
		colors[RESIDENT] = Color.BLUE;
		colors[MUTANT] = Color.RED;
		colors[VACANT] = Color.LIGHT_GRAY;
		setTraitColors(colors);
		// local storage
		mean = new double[nTraits];
		// default scores
		typeScores = new double[nTraits];
		typeScores[RESIDENT] = 1.0;
		typeScores[MUTANT] = 2.0;
		typeScores[VACANT] = Double.NaN;
	}

	@Override
	public void unload() {
		super.unload();
		mean = null;
	}

	@Override
	public String getKey() {
		return "eMoran";
	}

	@Override
	public String getInfo() {
		return "Title: " + getTitle() + "\nAuthor: Christoph Hauert\nMoran process with variable population sizes.";
	}

	@Override
	public String getTitle() {
		return "Ecological Moran process";
	}

	@Override
	public String getVersion() {
		return "v1.0 March 2021";
	}

	@Override
	public int getDependent() {
		// note: needed even though VACANT is the default dependent because Moran
		//		 dedicates RESIDENT as the dependent.
		return VACANT;
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * in ecological settings the interpretation of fitness is simply in terms of
	 * birth rates
	 */
	@Override
	public void setFitness(double[] aValue) {
		System.arraycopy(aValue, 0, typeScores, 0, 2);
		typeScores[VACANT] = Double.NaN;
	}

	EcoMoranMap map;

	@Override
	public Data2Phase getPhase2DMap() {
		map = new EcoMoranMap();
		return map;
	}

	public class EcoMoranMap implements Data2Phase {

		@Override
		public boolean data2Phase(double[] data, Point2D point) {
			// NOTE: data[0] is time!
			point.x = 1.0 - data[VACANT + 1];
			point.y = Math.min(1.0, 1.0 - data[RESIDENT + 1] / (1.0 - data[VACANT + 1]));
			return true;
		}

		@Override
		public boolean phase2Data(Point2D point, double[] data) {
			data[VACANT] = 1.0 - point.x;
			data[RESIDENT] = point.x * (1.0 - point.y);
			data[MUTANT] = point.x * point.y;
			return true;
		}

		@Override
		public String getXAxisLabel() {
			return "population density";
		}

		@Override
		public String getYAxisLabel() {
			return "relative fraction of mutants";
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
			tip += "<tr><td style='text-align:right'><i>" + getTraitName(RESIDENT) + ":</i></td><td>"
					+ Formatter.formatPercent(x * (1.0 - y), 2) + "</td></tr>";
			tip += "<tr><td style='text-align:right'><i>" + getTraitName(MUTANT) + ":</i></td><td>"
					+ Formatter.formatPercent(x * y, 2) + "</td></tr>";
			tip += "</table>";
			return tip;
		}
	}
}
