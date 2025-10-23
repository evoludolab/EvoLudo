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

package org.evoludo.simulator.modules;

import org.evoludo.geom.Point2D;
import org.evoludo.simulator.EvoLudo;
import org.evoludo.simulator.views.BasicTooltipProvider;
import org.evoludo.simulator.views.HasPhase2D;
import org.evoludo.simulator.views.HasS3;
import org.evoludo.util.Formatter;

/**
 * The Moran process with variable population sizes. This is a generalization of
 * the orginal Moran process to ecological settings with uncorrelated birth- and
 * death events.
 * 
 * @author Christoph Hauert
 */
public class EcoMoran extends Moran implements HasPhase2D, HasS3 {

	/**
	 * The index for the vacant type.
	 */
	static final int VACANT = 2;

	/**
	 * The mean number of individuals of each type.
	 */
	double[] mean;

	/**
	 * Create a new Moran process with variable population sizes.
	 * 
	 * @param engine the manager of modules and pacemaker for running the model
	 */
	public EcoMoran(EvoLudo engine) {
		super(engine);
		nTraits = 3; // residents, mutants and empty sites
		vacantIdx = VACANT;
	}

	@Override
	public void load() {
		// super sets names and colors
		super.load(); // Moran sets names, colors and scores already
		// just add default score for vacant sites
		typeScores[VACANT] = Double.NaN;
		// local storage
		mean = new double[nTraits];
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
	public String getAuthors() {
		return "George Berry & Christoph Hauert";
	}

	@Override
	public String getTitle() {
		return "Ecological Moran process";
	}

	@Override
	public int getDependent() {
		return VACANT;
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * In ecological settings the interpretation of fitness is simply in terms of
	 * birth rates
	 */
	@Override
	public void setFitness(double[] aValue) {
		System.arraycopy(aValue, 0, typeScores, 0, 2);
		typeScores[VACANT] = Double.NaN;
	}

	/**
	 * The map for converting population configurations to 2D phase space.
	 */
	EcoMoranMap map;

	@Override
	public Data2Phase getPhase2DMap() {
		if (map == null)
			map = new EcoMoranMap();
		return map;
	}

	/**
	 * The class that defines the mapping of mutants, residents, and vacant sites
	 * onto a 2D phase plane: population size along {@code x}-axis and the fraction
	 * of cooperators along {@code y}-axis.
	 */
	public class EcoMoranMap implements Data2Phase, BasicTooltipProvider {

		@Override
		public boolean data2Phase(double[] data, Point2D point) {
			// NOTE: data[0] is time!
			point.set(1.0 - data[VACANT + 1],
					Math.min(1.0, 1.0 - data[RESIDENT + 1] / (1.0 - data[VACANT + 1])));
			return true;
		}

		@Override
		public boolean phase2Data(Point2D point, double[] data) {
			double x = point.getX();
			double y = point.getY();
			data[VACANT] = 1.0 - x;
			data[RESIDENT] = x * (1.0 - y);
			data[MUTANT] = x * y;
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
			tip += "</table>";
			return tip;
		}
	}
}
