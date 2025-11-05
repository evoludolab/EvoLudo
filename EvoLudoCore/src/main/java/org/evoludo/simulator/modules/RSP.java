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

import org.evoludo.math.ArrayMath;
import org.evoludo.simulator.EvoLudo;
import org.evoludo.simulator.Geometry;
import org.evoludo.simulator.models.IBSD;
import org.evoludo.simulator.models.IBSD.Init;
import org.evoludo.simulator.models.IBSDPopulation;
import org.evoludo.simulator.models.Model.HasDE;
import org.evoludo.simulator.models.Model.HasIBS;
import org.evoludo.simulator.modules.Features.Payoffs;
import org.evoludo.simulator.views.HasHistogram;
import org.evoludo.simulator.views.HasMean;
import org.evoludo.simulator.views.HasPop2D;
import org.evoludo.simulator.views.HasPop3D;
import org.evoludo.simulator.views.HasS3;
import org.evoludo.util.CLOParser;
import org.evoludo.util.CLOption;
import org.evoludo.util.CLOption.CLODelegate;
import org.evoludo.util.CLOption.Category;

/**
 * Cyclical dynamics of the rock-scissors-paper game. A game that children,
 * lizards and even bacteria enjoy playing!
 * 
 * @author Christoph Hauert
 */
public class RSP extends Discrete implements Payoffs,
		HasIBS.DPairs, HasDE.DPairs, HasDE.RK5, HasDE.EM, HasDE.SDE, HasDE.PDERD, HasDE.PDEADV,
		HasPop2D.Traits, HasPop3D.Traits, HasMean.Traits, HasS3, HasPop2D.Fitness,
		HasPop3D.Fitness, HasMean.Fitness, HasHistogram.Fitness, HasHistogram.Degree,
		HasHistogram.StatisticsStationary {

	/**
	 * The trait (and index) value of the trait 'rock'.
	 */
	public static final int ROCK = 0;

	/**
	 * The trait (and index) value of the trait 'scissors'.
	 */
	public static final int SCISSORS = 1;

	/**
	 * The trait (and index) value of the trait 'paper'.
	 */
	public static final int PAPER = 2;

	/**
	 * The {@code 3×3} payoff matrix for interactions between the three types.
	 */
	double[][] payoff;

	/**
	 * Create a new instance of the module for rock-scissors-paper games.
	 * 
	 * @param engine the manager of modules and pacemaker for running the model
	 */
	public RSP(EvoLudo engine) {
		super(engine);
		nTraits = 3; // rock, scissors, paper
	}

	@Override
	public void load() {
		super.load();
		// trait names (optional)
		setTraitNames(new String[] { "Rock", "Scissors", "Paper" });
		// trait colors (optional)
		setTraitColors(new Color[] { Color.BLUE, new Color(238, 204, 17), Color.RED });
		// payoffs (local storage)
		payoff = new double[nTraits][nTraits];
	}

	@Override
	public void unload() {
		super.unload();
		payoff = null;
	}

	@Override
	public String getAuthors() {
		return "Christoph Hauert";
	}

	@Override
	public String getTitle() {
		return "Rock-Scissors-Paper Games";
	}

	@Override
	public int getDependent() {
		return PAPER;
	}

	@Override
	public double getMinPayoff() {
		return ArrayMath.min(payoff);
	}

	@Override
	public double getMaxPayoff() {
		return ArrayMath.max(payoff);
	}

	@Override
	public double getMonoPayoff(int type) {
		return payoff[type][type];
	}

	@Override
	public double pairScores(int me, int[] traitCount, double[] traitScore) {
		switch (me) {
			case ROCK:
				traitScore[ROCK] = payoff[ROCK][ROCK];
				traitScore[SCISSORS] = payoff[SCISSORS][ROCK];
				traitScore[PAPER] = payoff[PAPER][ROCK];
				return traitCount[ROCK] * payoff[ROCK][ROCK] + traitCount[SCISSORS] * payoff[ROCK][SCISSORS]
						+ traitCount[PAPER] * payoff[ROCK][PAPER];

			case SCISSORS:
				traitScore[ROCK] = payoff[ROCK][SCISSORS];
				traitScore[SCISSORS] = payoff[SCISSORS][SCISSORS];
				traitScore[PAPER] = payoff[PAPER][SCISSORS];
				return traitCount[ROCK] * payoff[SCISSORS][ROCK] + traitCount[SCISSORS] * payoff[SCISSORS][SCISSORS]
						+ traitCount[PAPER] * payoff[SCISSORS][PAPER];

			case PAPER:
				traitScore[ROCK] = payoff[ROCK][PAPER];
				traitScore[SCISSORS] = payoff[SCISSORS][PAPER];
				traitScore[PAPER] = payoff[PAPER][PAPER];
				return traitCount[ROCK] * payoff[PAPER][ROCK] + traitCount[SCISSORS] * payoff[PAPER][SCISSORS]
						+ traitCount[PAPER] * payoff[PAPER][PAPER];

			default: // should not end here
				throw new UnsupportedOperationException("Unknown trait (" + me + ")");
		}
	}

	@Override
	public void avgScores(double[] density, double[] avgscores) {
		avgscores[ROCK] = density[ROCK] * payoff[ROCK][ROCK] + density[SCISSORS] * payoff[ROCK][SCISSORS]
				+ density[PAPER] * payoff[ROCK][PAPER];
		avgscores[SCISSORS] = density[ROCK] * payoff[SCISSORS][ROCK] + density[SCISSORS] * payoff[SCISSORS][SCISSORS]
				+ density[PAPER] * payoff[SCISSORS][PAPER];
		avgscores[PAPER] = density[ROCK] * payoff[PAPER][ROCK] + density[SCISSORS] * payoff[PAPER][SCISSORS]
				+ density[PAPER] * payoff[PAPER][PAPER];
	}

	@Override
	public void mixedScores(int[] traitCount, double[] traitScore) {
		int r = traitCount[ROCK];
		int s = traitCount[SCISSORS];
		int p = traitCount[PAPER];
		double im1 = 1.0 / (r + s + p - 1);
		traitScore[ROCK] = ((r - 1) * payoff[ROCK][ROCK] + s * payoff[ROCK][SCISSORS] + p * payoff[ROCK][PAPER]) * im1;
		traitScore[SCISSORS] = (r * payoff[SCISSORS][ROCK] + (s - 1) * payoff[SCISSORS][SCISSORS]
				+ p * payoff[SCISSORS][PAPER]) * im1;
		traitScore[PAPER] = (r * payoff[PAPER][ROCK] + s * payoff[PAPER][SCISSORS] + (p - 1) * payoff[PAPER][PAPER])
				* im1;
	}

	/**
	 * Set the payoff matrix to {@code payMatrix}.
	 * 
	 * @param payMatrix the new payoff matrix
	 */
	public void setPayoffs(double[][] payMatrix) {
		for (int i = 0; i < 3; i++)
			System.arraycopy(payMatrix[i], 0, payoff[i], 0, 3);
	}

	/**
	 * Get the payoff matrix.
	 *
	 * @return the payoff matrix
	 */
	public double[][] getPayoffs() {
		return payoff;
	}

	/**
	 * Command line option to set the payoff matrix.
	 */
	public final CLOption cloPayoff = new CLOption("paymatrix", "0,1,-1;-1,0,1;1,-1,0", Category.Module,
			"--paymatrix <a11,a12,a13;a21,a22,a23;a31,a32,a33>\n" + //
					"                3x3 payoff matrix",
			new CLODelegate() {

				/**
				 * {@inheritDoc}
				 * <p>
				 * Parse the payoff matrix for {@code 3×3} games. The argument must have
				 * the form {@code 'a00,a01,a02;a10,11,a12;a20,a21,a22'} where {@code aij}
				 * refers to the payoff to an individual of type {@code i} interacting with a
				 * type {@code j} trait.
				 * <p>
				 * Note '{@value CLOParser#VECTOR_DELIMITER}' separates entries in 1D arrays and
				 * '{@value CLOParser#MATRIX_DELIMITER}' separates rows in 2D arrays.
				 * 
				 * @param arg the payoff matrix
				 */
				@Override
				public boolean parse(String arg) {
					double[][] payMatrix = CLOParser.parseMatrix(arg);
					if (payMatrix.length != 3 || payMatrix[0].length != 3)
						return false;
					setPayoffs(payMatrix);
					return true;
				}
			});

	@Override
	public void collectCLO(CLOParser parser) {
		super.collectCLO(parser);
		parser.addCLO(cloPayoff);
	}

	@Override
	public void adjustCLO(CLOParser parser) {
		super.adjustCLO(parser);
		if (model instanceof IBSD) {
			CLOption clo = ((IBSDPopulation) getIBSPopulation()).getInit().clo;
			clo.addKey(Init.Type.KALEIDOSCOPE);
		}
	}

	@Override
	public RSP.IBSPop createIBSPopulation() {
		return new RSP.IBSPop(engine, this);
	}

	/**
	 * The extension for IBS simulations specific to {@code 3×3} games. This
	 * extension implements specific initial conditions that give rise to
	 * fascinating evolutionary kaleidoscopes for deterministic updating.
	 */
	public class IBSPop extends IBSDPopulation {

		/**
		 * Create a new instance of the IBS model for {@code 3×3} games.
		 * 
		 * @param engine the pacemaker for running the model
		 * @param module the module that defines the game
		 */
		protected IBSPop(EvoLudo engine, RSP module) {
			super(engine, module);
		}

		@Override
		protected void initKaleidoscope() {
			// kaleidoscopes only available for some geometries
			if (!(interaction.getType() == Geometry.Type.SQUARE || interaction.getType() == Geometry.Type.SQUARE_MOORE
					|| interaction.getType() == Geometry.Type.SQUARE_NEUMANN)) {
				logger.warning("kaleidoscopes require square lattices - using uniform initialization.");
				initUniform();
				return;
			}
			initMono(RSP.ROCK);
			int mid;
			int size;
			switch (interaction.getType()) {
				case SQUARE_NEUMANN:
				case SQUARE_NEUMANN_2ND:
				case SQUARE_MOORE:
				case SQUARE:
					size = (int) Math.floor(Math.sqrt(nPopulation) + 0.5);
					mid = size / 2;
					/* border around center */
					int width = 20;
					int height = 5;
					for (int w = mid - width / 2; w <= mid + width / 2; w++)
						for (int h = mid; h <= mid + height; h++)
							setTraitAt(h * size + w, RSP.SCISSORS);
					for (int w = mid - width / 2; w <= mid + width / 2; w++)
						for (int h = mid - height; h <= mid; h++)
							setTraitAt(h * size + w, RSP.PAPER);
					traitsCount[RSP.ROCK] -= 10 * 20;
					traitsCount[RSP.SCISSORS] += 5 * 20;
					traitsCount[RSP.PAPER] += 5 * 20;
					break;
				case HONEYCOMB:
					break;
				case TRIANGULAR:
					break;
				default:
					// should never get here - check made sure of it.
					throw new UnsupportedOperationException("geometry incompatible with kaleidoscopes!");
			}
		}
	}
}
