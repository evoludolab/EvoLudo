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

import java.io.PrintStream;
import java.util.Arrays;

import org.evoludo.math.ArrayMath;
import org.evoludo.simulator.EvoLudo;
import org.evoludo.simulator.models.Model;
import org.evoludo.simulator.models.ODERK;
import org.evoludo.util.CLOParser;
import org.evoludo.util.CLOption;
import org.evoludo.util.CLOption.CLODelegate;
import org.evoludo.util.Formatter;

/**
 * The module for investigating the evolutionary dynamics of the conservation
 * game. In essence, the conservation game represents an extended form of the
 * Moran process where the fitness of the two types not only depends on their
 * type but also on the type of the patch they are located on. Or conversely,
 * a simplified form of asymmetric {@code 2×2} games with two strategic
 * types but no interactions between individuals, just feedback with the patch
 * qualities.
 *
 * @author Christoph Hauert
 * 
 * @see Moran
 * @see ATBT
 */
public class CG extends ATBT implements Module.Static {

	/**
	 * The static scores for the two strategic types.
	 * <p>
	 * <strong>Note:</strong>In general
	 */
	protected double[] typeScores;

	/**
	 * The flag to indicate whether there is environmental feedback between
	 * strategies and patches.
	 */
	boolean noFeedback;

	/**
	 * Create a new instance of the module for conservation games.
	 * 
	 * @param engine the manager of modules and pacemeaker for running the model
	 */
	public CG(EvoLudo engine) {
		super(engine);
	}

	@Override
	public void load() {
		super.load(); // ATBT sets names and colors
		// default scores
		typeScores = new double[nTraits];
		typeScores[COOPERATE_RICH] = 1.0;
		typeScores[DEFECT_RICH] = 1.0;
		typeScores[DEFECT_POOR] = 0.0;
		typeScores[COOPERATE_POOR] = 0.0;
	}

	@Override
	public void unload() {
		super.unload();
		typeScores = null;
	}

	@Override
	public String getKey() {
		return "CG";
	}

	@Override
	public String getInfo() {
		return "Title: " + getTitle() + "\nAuthor: David Bromley & Christoph Hauert\nConservation Game.";
	}

	@Override
	public String getTitle() {
		return "Conservation Game";
	}

	@Override
	public String getVersion() {
		return "v1.0 April 2021";
	}

	@Override
	public double getMinGameScore() {
		return ArrayMath.min(typeScores);
	}

	@Override
	public double getMaxGameScore() {
		return ArrayMath.max(typeScores);
	}

	@Override
	public double getMonoGameScore(int type) {
		return typeScores[type];
	}

	@Override
	public double[] getStaticScores() {
		return typeScores;
	}

	@Override
	public void avgScores(double[] density, int n, double[] avgscores) {
		System.arraycopy(typeScores, 0, avgscores, 0, nTraits);
	}

	@Override
	public boolean check() {
		boolean changed = super.check();
		noFeedback = !(ArrayMath.min(feedback) > 0.0);
		return changed;
	}

	/**
	 * Set the fitness values for the four types.
	 * 
	 * @param aValue the array of fitness values
	 */
	public void setFitness(double[] aValue) {
		System.arraycopy(aValue, 0, typeScores, 0, nTraits);
	}

	/**
	 * Get the array of fitness values for each type.
	 *
	 * @return the array of fitness values
	 */
	public double[] getFitness() {
		return typeScores;
	}

	/**
	 * Set the fitness for type {@code aType} to {@code aValue}.
	 *
	 * @param aValue the new fitness value for type {@code aType}
	 * @param aType  the type for which to set the fitness
	 */
	public void setFitness(double aValue, int aType) {
		if (aType < 0 || aType >= nTraits)
			return;
		typeScores[aType] = aValue;
	}

	/**
	 * Get the fitness for type {@code aType}.
	 * 
	 * @param aType the type for which to get the fitness
	 * @return the fitness of type {@code aType}
	 */
	public double getFitness(int aType) {
		if (aType < 0 || aType >= nTraits)
			return Double.NaN;
		return typeScores[aType];
	}

	/**
	 * Command line option to set the fitness of the four types: cooperators and
	 * defectors on rich and poor patches, respectively.
	 */
	public final CLOption cloFitness = new CLOption("fitness", "1,1,1,1", EvoLudo.catModule,
			"--fitness <Cp,Dr,Cr,Dp>  fitness of CooperatorPoor, DefectorRich, CooperatorRich, DefectorPoor",
			new CLODelegate() {

				/**
				 * {@inheritDoc}
				 * <p>
				 * Parse the fitness array for the four types: cooperators and defectors on rich
				 * and poor patches, respectively.
				 * 
				 * @param arg the array with fitness values
				 */
				@Override
				public boolean parse(String arg) {
					double[] fit = CLOParser.parseVector(arg);
					if (fit.length != 4) {
						logger.warning("--fitness expects vector of length 4 (instead of '" + arg + "') - using '"
								+ cloFitness.getDefault() + "'");
						return false;
					}
					setFitness(fit);
					return true;
				}

				@Override
				public void report(PrintStream output) {
					output.println("# fitness:      " + Formatter.format(getFitness(), 4));
				}
			});

	@Override
	public void collectCLO(CLOParser parser) {
		// prepare command line options
		parser.addCLO(cloFitness);

		super.collectCLO(parser);
	}

	@Override
	public void adjustCLO(CLOParser parser) {
		super.adjustCLO(parser);
		parser.removeCLO(new String[] { "asymmetry", "environment", "payoffs", "popupdate", "playerupdate", "references" });
	}

	@Override
	public CG.IBS createIBSPop() {
		return new CG.IBS(engine);
	}

	@Override
	public Model.ODE createODE() {
		return new CG.ODE(engine, this);
	}

	// // ANALYTICAL CALCULATIONS OF FIXATION PROBABILITIES AND TIMES
	//
	// // Kaveh RSOS (2018) transition probabilities in heterogeneous environments
	// // ============================================================
	// private double TplusPoor(int nPoor) {
	// int N = nPopulation;
	// double nCp = (init[COOPERATE_POOR] * N) - nPoor; // Does this work for static
	// environments? (probably yes)
	// double weightDp = typeScores[DEFECT_POOR] * nPoor;
	// double weightDr = typeScores[DEFECT_RICH] * strategiesTypeCount[DEFECT_RICH];
	// // How does one count how many defect_rich there are for a given nPoor?
	// double weightCp = typeScores[COOPERATE_POOR] * nCp;
	// double weightCr = typeScores[COOPERATE_RICH] *
	// strategiesTypeCount[COOPERATE_RICH]; // How does one count how many
	// cooperate_rich there are for a given nPoor?
	//
	// return (weightDp+weightDr)/(weightDp + weightCp + weightDr + weightCr) *
	// (nCp/N);
	//
	// }
	//
	// private double TplusRich(int nRich) {
	// int N = nPopulation;
	// double nCr = (init[COOPERATE_RICH] * N) - nRich; // Does this work for static
	// environments? (probably yes)
	// double weightDp = typeScores[DEFECT_POOR] * strategiesTypeCount[DEFECT_POOR];
	// // How does one count how many defect_poor there are for a given nRich
	// double weightDr = typeScores[DEFECT_RICH] * nCr;
	// double weightCp = typeScores[COOPERATE_POOR] *
	// strategiesTypeCount[COOPERATE_POOR]; // How does one count how many
	// cooperate_poor there are for a given nRich?
	// double weightCr = typeScores[COOPERATE_RICH] * nCr;
	//
	// return (weightDp+weightDr)/(weightDp + weightCp + weightDr + weightCr) *
	// (nCr/N);
	//
	// }
	//
	// private double TplusTot1(int i) {
	// return TplusRich(i) + TplusPoor(i);
	// }

	/**
	 * Extends TBTIBS to take advantage of kaleidoscope initializations.
	 */
	class IBS extends TBT.IBS {

		/**
		 * Create a new instance of the IBS model for the conservation game.
		 * 
		 * @param engine the pacemeaker for running the model
		 */
		protected IBS(EvoLudo engine) {
			(new TBT(engine)).super(engine);
		}

		@Override
		protected int initMutant() {
			if (strategiesTypeCount[CG.DEFECT_RICH] == 0 && strategiesTypeCount[CG.DEFECT_POOR] == 0) {
				// no defectors in population - randomly place one
				int mutant = random0n(nPopulation);
				int oldtype = strategies[mutant] % nTraits;
				int env = oldtype / 2;
				// only set strategy, do not change environment
				int newtype = env + env + (oldtype + 1) % 2;
				strategies[mutant] = newtype;
				strategiesTypeCount[newtype] = 1;
				strategiesTypeCount[oldtype]--;
				return mutant;
			}
			if (strategiesTypeCount[CG.COOPERATE_RICH] == 0 && strategiesTypeCount[CG.COOPERATE_POOR] == 0) {
				// no cooperators in population - randomly place one
				int mutant = random0n(nPopulation);
				int oldtype = strategies[mutant] % nTraits;
				int env = oldtype / 2;
				// only set strategy, do not change environment
				int newtype = env + env + (oldtype + 1) % 2;
				strategies[mutant] = newtype;
				strategiesTypeCount[newtype] = 1;
				strategiesTypeCount[oldtype]--;
				return mutant;
			}
			// not absorbed - initialize environment according to frequencies in init
			// the maximum frequency determines the resident trait
			int restrait = ArrayMath.maxIndex(init) % 2;
			int muttrait = (restrait + 1) % 2;
			Arrays.fill(strategiesTypeCount, 0);
			Arrays.fill(strategies, restrait);
			double rich = init[CG.COOPERATE_RICH] + init[CG.DEFECT_RICH];
			for (int n = 0; n < nPopulation; n++) {
				// rich types: 0, 1; poor types: 2, 3
				int type = restrait + (random01() > rich ? 0 : 2);
				strategies[n] = type;
				strategiesTypeCount[type]++;
			}
			// add mutant
			int loc = random0n(nPopulation);
			strategiesTypeCount[strategies[loc]]--;
			strategies[loc] = muttrait;
			strategiesTypeCount[muttrait]++;
			return loc;
		}

		@Override
		public boolean updatePlayerAt(int me) {
			boolean changed = super.updatePlayerAt(me);
			int oldtype = strategies[me] % nTraits;
			double myfit = getFitnessAt(me);
			double myfeed = feedback[oldtype];
			double rand = random01();
			if (rand > (myfit + myfeed) * realtimeIncr)
				// no event
				return changed;
			// check whether individual reproduces or environment changes
			if (rand < myfit * realtimeIncr) {
				// reproduction - offspring replaces randomly chosen neighbour
				int[] group = referenceGroup.pickAt(me, reproduction, true);
				if (group.length < 1)
					// 'me' has no outgoing-neighbors (sink)
					return changed;
				// for reproduction we can use the back-end of Moran updating and only need to
				// adjust updateStrategyAt to deal with strategy versus environment
				updatePlayerMoran(me, group[0]);
				return changed;
			}
			// environment changes - change type of node
			int oldstrat = oldtype / 2;
			// determine new patch type (old one was GOOD if oldtype is even and will now
			// turn BAD and vice versa)
			int newpatch = (oldtype + 1) % 2;
			strategiesScratch[me] = newpatch + oldstrat + oldstrat + nTraits;
			if (adjustScores) {
				adjustGameScoresAt(me);
				return changed;
			}
			// no strategy change happened so no need to consider reseting the scores
			commitStrategyAt(me);
			// playGameAt(me);
			return changed;
		}

		@Override
		protected void updateStrategyAt(int index, int newtype) {
			int oldtype = strategies[index] % nTraits;
			// make sure patch type is preserved
			int oldpatch = oldtype % 2;
			// only strategies can be adopted
			int newstrat = newtype / 2;
			boolean changed = (oldtype / 2 != newstrat);
			strategiesScratch[index] = oldpatch + newstrat + newstrat + (changed ? nTraits : 0);
		}

		@Override
		public boolean isMonomorphic() {
			if (super.isMonomorphic())
				return true;
			// population may be monomorphic even with heterogeneous environment
			return (strategiesTypeCount[CG.COOPERATE_RICH] + strategiesTypeCount[CG.COOPERATE_POOR] == 0
					|| strategiesTypeCount[CG.DEFECT_RICH] + strategiesTypeCount[CG.DEFECT_POOR] == 0);
		}

		@Override
		public boolean checkConvergence() {
			if (!super.checkConvergence())
				// not converged
				return false;
			// maybe converged. without feedback monomorphic population is enough.
			if (noFeedback)
				return true;
			for (int n = 0; n < nTraits; n++) {
				if (strategiesTypeCount[n] == 0 || feedback[n] <= 0.0)
					continue;
				// there are staretgies of type n with positive feedback rates; not yet
				// converged
				return false;
			}
			return true;
		}
	}

	/**
	 * Provide ODE implementation for conservation games.
	 */
	class ODE extends ODERK {

		/**
		 * Constructs a new ODE solver for conservation games.
		 * 
		 * @param engine the pacemeaker for running the model
		 * @param module the module to numerically integrate
		 */
		public ODE(EvoLudo engine, Module module) {
			super(engine, module);
		}

		/**
		 * Ecological feedback renders the system dissipative and hence time reversal is
		 * not possible.
		 */
		@Override
		public boolean permitsTimeReversal() {
			return !noFeedback;
		}
	}
}
