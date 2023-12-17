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

// EXTEND CXPOPULATION
//	// override method in CXPopulation to introduce maximum investment
//	@Override
//	public void mutateStrategyAt(int index, boolean changed) {
//		// no upper bound - use original method
//		if( maxInvest<0.0 ) {
//			super.mutateStrategyAt(index, changed);
//			return;
//		}
//
//		int idx = index;
//		int loc = 0;
//		if( nTraits>1 ) {
//			idx *= nTraits;
//			loc = random0n(nTraits);
//		}
//		if( !changed ) {
//			// copy current strategy to scratch as it will be committed from there
//			if( nTraits>1 )
//				System.arraycopy(strategies, idx, strategiesScratch, idx, nTraits);
//			else
//				strategiesScratch[idx] = strategies[idx];
//		}
//		double invest = 0.0;
//		for( int i=0; i<nTraits; i++ )
//			invest += strategiesScratch[idx+i];
//		double max = maxInvest-(invest-strategiesScratch[idx+loc]);
//		switch( mutationType ) {
//			case MUTATION_UNIFORM:
//				strategiesScratch[idx+loc] = random01()*max;
//				return;
//			case MUTATION_GAUSSIAN:
//				// draw mutants until we find viable one...
//				// not very elegant but avoids emphasis of interval boundaries.
////c.f. suggestion by jf!
//				double mean = strategiesScratch[idx+loc];
//				double sdev = mutSdev[loc];
//				double mut;
//				do {
//					mut = randomGaussian(mean, sdev);
//				}
//				while( mut<0.0 || mut>max );
//				strategiesScratch[idx+loc] = mut;
//				return;
//			default:
//				throw new Error("Unknown mutation type ("+mutationType+")");
//		}
//	}
//
//	// extend method in CXPopulation to introduce maximum investment
//	@Override
//	public void init(Model mod) {
//		if (!model.isModelType(Model.Type.IBS))
//			return;
//		// no upper bound - we're done
//		if( maxInvest<0.0 )
//			return;
//
//		// search for traits that violate maxInvest
//		double norm = maxInvest/(nTraits-1);
//		for( int n=0; n<nPopulation; n++ ) {
//			double	invest = 0.0;
//			int		idx = n*nTraits;
//			for( int s=0; s<nTraits; s++ ) invest += strategies[idx+s];
//			if( invest>maxInvest ) {
//				// requires correction
//				if( maxInvest>1.0 || invest>1.0 )
//					for( int s=0; s<nTraits; s++ ) strategies[idx+s] = (1.0-strategies[idx+s])*norm;
//				else
//					for( int s=0; s<nTraits; s++ ) strategies[idx+s] *= norm;
//			}
//		}
//	}
// END EXTEND 
}
