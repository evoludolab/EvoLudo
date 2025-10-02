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

import java.awt.Color;

import org.evoludo.simulator.EvoLudo;
import org.evoludo.simulator.models.Model.HasIBS;
import org.evoludo.simulator.modules.Features.Payoffs;
import org.evoludo.simulator.views.HasDistribution;
import org.evoludo.simulator.views.HasHistogram;
import org.evoludo.simulator.views.HasMean;
import org.evoludo.simulator.views.HasPop2D;
import org.evoludo.simulator.views.HasPop3D;

/**
 * The module for the evolution of two behavioural traits. This module is
 * designed for the study of specialization and division of labour.
 * 
 * @author Christoph Hauert
 * 
 * @see "Henriques G. J. B., Ito K., Hauert C., Doebeli M. (2021)
 *      <em>On the importance of evolving phenotype distributions on
 *      evolutionary diversification.</em> PLoS Comput. Biol. 17(2): e1008733.
 *      <a href='https://doi.org/10.1371/journal.pcbi.1008733'>doi:
 *      10.1371/journal.pcbi.1008733</a>"
 */
public class CLabour extends Continuous implements Payoffs,
		HasIBS.MCPairs,
		HasPop2D.Traits, HasPop3D.Traits, HasMean.Traits, HasHistogram.Strategy, HasDistribution.Strategy,
		HasPop2D.Fitness, HasPop3D.Fitness, HasMean.Fitness, HasHistogram.Fitness,
		HasHistogram.Degree {

	// NOTE: in order to introduce a maximum total investment, this module needs to
	// extend models to properly deal with constraints on mutations
	// protected double maxInvest = -1.0;

	/**
	 * Temporary storage for the traits of the opponent.
	 */
	protected double[] you;

	/**
	 * Constructs a new EvoLudo module for the evolution of two behavioural traits.
	 * 
	 * @param engine the manager of modules and pacemaker for running the model
	 */
	public CLabour(EvoLudo engine) {
		super(engine);
		setNTraits(2); // two traits
	}

	@Override
	public void load() {
		super.load();
		// trait colors (automatically generates lighter versions for min and max)
		setTraitColors(new Color[] { Color.GREEN, Color.RED });
		// opponent traits (local storage)
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
	public String getAuthors() {
		return "Christoph Hauert";
	}

	@Override
	public String getTitle() {
		return "Continuous Division of Labour";
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

	// /**
	// * Set the maximum combined investment. This is the maximum an individual can
	// * invest in in all traits combined.
	// *
	// * @param aValue the maximum investment
	// */
	// public void setMaxInvestment(double aValue) {
	// maxInvest = aValue;
	// }

	// /**
	// * Get the maximum combined investment. This is the maximum an individual can
	// * invest in in all traits combined.
	// *
	// * @return the maximum investment
	// */
	// public double getMaxInvestment() {
	// return maxInvest;
	// }
}
