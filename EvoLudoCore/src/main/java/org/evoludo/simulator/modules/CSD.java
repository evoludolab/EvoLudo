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
import org.evoludo.simulator.models.IBS.HasIBS;
import org.evoludo.simulator.views.HasDistribution;
import org.evoludo.simulator.views.HasHistogram;
import org.evoludo.simulator.views.HasMean;
import org.evoludo.simulator.views.HasPop2D;
import org.evoludo.simulator.views.HasPop3D;

/**
 * The module for investigating the evolutionary dynamics of the continuous
 * snowdrift game, or more generally, interactions among pairs of individuals
 * with continuous traits. A range of cost and benefit functions determine the
 * payoffs to the interacting individuals. The characteristics of the
 * interaction sensitively depend on the cost and benefit functions as well as
 * the function parameters. For example, for the continuous games the cost and
 * benefit functions \(C(x), B(x)\), are typically assumed to be smooth,
 * strictly increasing functions that satisfy \(B(0)=C(0)=0\) and
 * \(B(x)&gt;C(x)\) at least for small \(x\), where \(x\) denotes the continuous
 * trait, or the investment level. This means that no investements provide no
 * benefits but also entail no costs but benefits, at least for small
 * investments, exceed the costs.
 * <p>
 * In the continuous prisoner's dilemma the payoff to an \(x\)-strategist facing
 * an individual with strategy \(y\) is thus given by
 * \[P(x,y)=B(y)-C(x),\]
 * which means the benefits only depend on the opponents trait while the costs
 * only depend on the player's own trait. Clearly the only way for the \(x\)
 * strategist to increase its payoff is to lower the investment level. The same
 * applies to the \(y\) strategist and hence without any further mechanisms the
 * investment levels will invariably decrease and cooperation vanish, just as in
 * the traditional discrete prisoner's dilemma with the two fixed traits of
 * cooperation and defection.
 * <p>
 * In contrast, in the continuous snowdrift game the payoff to an
 * \(x\)-strategist facing an individual with strategy \(y\) is thus given by
 * \[Q(x,y)=B(x+y)-C(x),\]
 * which means that the benefit that the \(x\)-strategist obtains is based on
 * the total cooperative investment made by both agents, while the costs depend
 * only on its own trait, just as before. Because of \(B(x)&gt;C(x)\) this
 * implies that it is always advantageous for an individual to make a (small)
 * cooperative investment if its partner is not doing so. As a consequence we
 * might expect that cooperative investment levels evolve towards some non-zero
 * level. As it turns out, the evolutionary dynamics is much richer and is
 * analytically tractable using the framework of adaptive dynamics. In
 * particular, it is possible that a population spontaneously split through
 * evolutionary branching into two stably co-existing phenotypes of high and low
 * investors. This dynamic provides an intriguing evolutionary pathway for the
 * origin and emergence discrete traits as assumed in classical
 * {@code 2×2} interactions.
 *
 * @author Christoph Hauert
 * 
 * @see TBT
 */
public class CSD extends Continuous implements 
		HasIBS.CPairs, 
		HasPop2D.Strategy, HasPop3D.Strategy, HasPop2D.Fitness, HasPop3D.Fitness,
		HasMean.Strategy, HasMean.Fitness, 
		HasHistogram.Strategy, HasDistribution.Strategy, HasHistogram.Fitness, 
		HasHistogram.Degree {

	/**
	 * Create a new instance of the module for continuous games.
	 * 
	 * @param engine the manager of modules and pacemaker for running the model
	 */
	public CSD(EvoLudo engine) {
		super(engine);
	}

	@Override
	public void load() {
		super.load();
		nTraits = 1;
		// trait names
		setTraitNames(new String[] { "Investment" });
		// trait colors (automatically generates lighter versions for min and max)
		setTraitColors(new Color[] { Color.BLACK });
	}

	@Override
	public String getKey() {
		return "cSD";
	}

	@Override
	public String getAuthors() {
		return "Christoph Hauert";
	}

	@Override
	public String getTitle() {
		return "Continuous Snowdrift";
	}

	@Override
	public double pairScores(double me, double[] groupTraits, int len, double[] groupPayoffs) {
		double yourInvest;
		double myScore = 0.0;

		for (int n = 0; n < len; n++) {
			yourInvest = groupTraits[n];
			myScore += traits2payoff.payoff(me, yourInvest);
			groupPayoffs[n] = traits2payoff.payoff(yourInvest, me);
		}
		return myScore;
	}
}
