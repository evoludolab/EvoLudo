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

import org.evoludo.simulator.EvoLudo;
import org.evoludo.simulator.models.IBS.HasIBS;
import org.evoludo.simulator.modules.Continuous.MultiPairs;
import org.evoludo.simulator.views.HasConsole;
import org.evoludo.simulator.views.HasDistribution;
import org.evoludo.simulator.views.HasHistogram;
import org.evoludo.simulator.views.HasMean;
import org.evoludo.simulator.views.HasPop2D;
import org.evoludo.simulator.views.HasPop3D;

/**
 *
 * @author Christoph Hauert
 */
public class CLabour extends Continuous implements MultiPairs, HasIBS, HasPop2D.Strategy,
		HasPop3D.Strategy, HasMean.Strategy, HasHistogram.Strategy, HasDistribution.Strategy, HasPop2D.Fitness,
		HasPop3D.Fitness, HasMean.Fitness, HasHistogram.Fitness, HasHistogram.Degree, HasConsole {
// NOTE: in order to introduce a maximum total investment, this module needs to extend models to
// properly deal with mutations

	// local variables
	protected double maxInvest = -1.0;
	protected double[] you;

	public CLabour(EvoLudo engine) {
		super(engine);
	}

	@Override
	public void load() {
		super.load();
		nTraits = 2;
		// trait names
		setTraitNames(new String[] { "Trait 1", "Trait 2" });
		// trait colors (automatically generates lighter versions for min and max)
		setTraitColors(new Color[] { Color.GREEN, // trait 1
				Color.RED // trait 2
		});
		// alloc
		you = new double[nTraits];
	}

	@Override
	public void unload() {
		super.unload();
		you = null;
	}

	@Override
	public String getKey() {
		return "cLabour";
	}

	@Override
	public String getInfo() {
		return "Title: " + getTitle() + "\nAuthor: Christoph Hauert\n" + "Specialization and division of labour.";
	}

	@Override
	public String getTitle() {
		return "Continuous Division of Labour";
	}

	@Override
	public String getVersion() {
		return "v1.0 April 2021";
	}

	@Override
	public double pairScores(double[] me, double[] groupTraits, int len, double[] groupPayoffs) {
		double myScore = 0.0;

		for (int n = 0; n < len; n++) {
			System.arraycopy(groupTraits, n * nTraits, you, 0, nTraits);
			myScore += traits2payoff.payoff(me, you);
			groupPayoffs[n] = traits2payoff.payoff(you, me);
		}
		return myScore;
	}

	public void setMaxInvestment(double aValue) {
		maxInvest = aValue;
	}

	public double getMaxInvestment() {
		return maxInvest;
	}
}
