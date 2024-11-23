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
import java.io.PrintStream;
import java.util.Arrays;

import org.evoludo.math.ArrayMath;
import org.evoludo.simulator.ColorMap;
import org.evoludo.simulator.EvoLudo;
import org.evoludo.simulator.Geometry;
import org.evoludo.simulator.models.IBS.HasIBS;
import org.evoludo.simulator.models.IBSD;
import org.evoludo.simulator.models.IBSD.Init;
import org.evoludo.simulator.models.IBSDPopulation;
import org.evoludo.simulator.models.ODEEuler.HasODE;
import org.evoludo.simulator.models.PDERD.HasPDE;
import org.evoludo.simulator.models.SDEEuler.HasSDE;
import org.evoludo.simulator.views.HasHistogram;
import org.evoludo.simulator.views.HasMean;
import org.evoludo.simulator.views.HasPop2D;
import org.evoludo.simulator.views.HasPop3D;
import org.evoludo.util.CLOParser;
import org.evoludo.util.CLOption;
import org.evoludo.util.CLOption.CLODelegate;
import org.evoludo.util.Formatter;

/**
 * The module for investigating the evolutionary dynamics in the classical
 * {@code 2×2} games. The prisoner's dilemma and the snowdrift game are
 * popular instances of {@code 2×2} games. The ranking of the payoffs
 * determines the characteristics of the game. Consider the payoff matrix
 * \begin{align}
 * \begin{matrix} &amp;
 * \begin{matrix} &amp;\hspace{-3ex} C &amp;\hspace{-1ex} D \end{matrix} \\
 * \begin{matrix} C\\ D \end{matrix} &amp; \hspace{-1ex}
 * \begin{pmatrix}R &amp; S\\ T &amp; P\end{pmatrix}
 * \end{matrix}
 * \end{align}
 * where \(R\) refers to the payoff for mutual cooperation, \(S\) to the
 * sucker's payoff to a cooperator facing a defector, \(T\) to the temptation to
 * defect for a defector facing a cooperator and \(P\) to the punishment for
 * mutual defection. The notation and terminology go back to Robert Axelrod who
 * popularised the prisoner's dilemma in the early '80s. The prisoner's dilemma
 * is characterized by the ranking \(T&gt;R&gt;P&gt;S\), which means that a
 * defector is always better off regardless of what the opponent plays, i.e.
 * \(T&gt;R\) and \(P&gt;S\), yet the payoff for mutual cooperation exceeds that
 * of mutual defection, i.e. \(R&gt;P\). This results in a classical conflict of
 * interest between the individual and the group, which is the essence of social
 * dilemmas. As a consequence cooperation is doomed in the prisoner's dilemma in
 * the absence of supporting mechanisms, such as structured populations with
 * limited local interactions
 * <p>
 * Similarly, the snowdrift game is characterized by \(T&gt;R&gt;S&gt;P\) and
 * now the best strategy depends on the opponent. Against a cooperator it is
 * still better to defect but against a defector it pays to cooperate. The
 * snowdrift game is still a social dilemma because individuals are tempted to
 * move away from mutual cooperation but the dilemma is relaxed because
 * cooperation is not necessarily doomed in the absence of supporting
 * mechanisms.
 *
 * @author Christoph Hauert
 */
public class TBT extends Discrete implements HasIBS.DPairs, HasODE, HasSDE, HasPDE,
		HasPop2D.Strategy, HasPop3D.Strategy, HasMean.Strategy,
		HasPop2D.Fitness, HasPop3D.Fitness, HasMean.Fitness, HasHistogram.Fitness,
		HasHistogram.Degree, HasHistogram.StatisticsProbability, HasHistogram.StatisticsTime,
		HasHistogram.StatisticsStationary {

	/**
	 * The trait (and index) value of cooperators.
	 */
	public static final int COOPERATE = 0;

	/**
	 * The trait (and index) value of defectors.
	 */
	public static final int DEFECT = 1;

	/**
	 * The {@code 2×2} payoff matrix for interactions between cooperators and
	 * defectors.
	 */
	double[][] payoffs;

	/**
	 * Create a new instance of the module for {@code 2×2} games.
	 * 
	 * @param engine the manager of modules and pacemaker for running the model
	 */
	public TBT(EvoLudo engine) {
		super(engine);
	}

	@Override
	public void load() {
		super.load();
		nTraits = 2;
		// trait names
		String[] names = new String[nTraits];
		names[DEFECT] = "Defector";
		names[COOPERATE] = "Cooperator";
		setTraitNames(names);
		// trait colors
		Color[] colors = new Color[2 * nTraits];
		colors[DEFECT] = Color.RED;
		colors[COOPERATE] = Color.BLUE;
		colors[DEFECT + nTraits] = Color.YELLOW;
		colors[COOPERATE + nTraits] = Color.GREEN;
		setTraitColors(colors);
		// payoffs
		payoffs = new double[nTraits][nTraits];
	}

	@Override
	public void unload() {
		super.unload();
		payoffs = null;
	}

	@Override
	public String getKey() {
		return "2x2";
	}

	@Override
	public String getInfo() {
		return "Title: " + getTitle() + "\nAuthor: Christoph Hauert\n2x2 games in populations of constant size.";
	}

	@Override
	public String getTitle() {
		return "2x2 Games";
	}

	@Override
	public String getVersion() {
		return "v1.0 March 2021";
	}

	@Override
	public String getTraitName(int idx) {
		String idxname = super.getTraitName(idx % nTraits);
		if (competition == null || competition.getType() != Geometry.Type.SQUARE_NEUMANN_2ND)
			return idxname;
		if (idx >= nTraits)
			return idxname + " (2nd)";
		return idxname + " (1st)";
	}

	@Override
	public Color[] getMeanColors() {
		Color[] colors = super.getMeanColors();
		// not all models entertain competition geometries, e.g. ODE/SDE
		if (competition == null || competition.getType() != Geometry.Type.SQUARE_NEUMANN_2ND)
			return colors;
		Color[] color2nd = new Color[2 * nTraits];
		for (int n = 0; n < nTraits; n++) {
			Color cn = colors[n];
			color2nd[n] = cn;
			color2nd[nTraits + n] = ColorMap.addAlpha(cn, 150);
		}
		return color2nd;
	}

	@Override
	public int getDependent() {
		return DEFECT;
	}

	@Override
	public double getMinGameScore() {
		return ArrayMath.min(payoffs);
	}

	@Override
	public double getMaxGameScore() {
		return ArrayMath.max(payoffs);
	}

	@Override
	public double getMonoGameScore(int type) {
		return payoffs[type][type];
	}

	@Override
	public double pairScores(int me, int[] traitCount, double[] traitScore) {
		switch (me) {
			case COOPERATE:
				traitScore[COOPERATE] = payoffs[COOPERATE][COOPERATE];
				traitScore[DEFECT] = payoffs[DEFECT][COOPERATE];
				return traitCount[COOPERATE] * payoffs[COOPERATE][COOPERATE] + traitCount[DEFECT] * payoffs[COOPERATE][DEFECT];

			case DEFECT:
				traitScore[COOPERATE] = payoffs[COOPERATE][DEFECT];
				traitScore[DEFECT] = payoffs[DEFECT][DEFECT];
				return traitCount[COOPERATE] * payoffs[DEFECT][COOPERATE] + traitCount[DEFECT] * payoffs[DEFECT][DEFECT];

			default: // should not end here
				throw new Error("Unknown strategy (" + me + ")");
		}
	}

	@Override
	public void avgScores(double[] density, int n, double[] avgscores) {
		avgscores[COOPERATE] = density[COOPERATE] * payoffs[COOPERATE][COOPERATE] + density[DEFECT] * payoffs[COOPERATE][DEFECT];
		avgscores[DEFECT] = density[COOPERATE] * payoffs[DEFECT][COOPERATE] + density[DEFECT] * payoffs[DEFECT][DEFECT];
	}

	@Override
	public void mixedScores(int[] traitCount, double[] traitScore) {
		int x = traitCount[COOPERATE];
		int y = traitCount[DEFECT];
		double in = 1.0 / (x + y - 1);
		traitScore[COOPERATE] = ((x - 1) * payoffs[COOPERATE][COOPERATE] + y * payoffs[COOPERATE][DEFECT]) * in;
		traitScore[DEFECT] = (x * payoffs[DEFECT][COOPERATE] + (y - 1) * payoffs[DEFECT][DEFECT]) * in;
	}

	/**
	 * Set the payoffs from the {@code 2×2} matrix {@code payoffs}.
	 * 
	 * @param payoffs the payoff matrix
	 */
	public void setPayoffs(double[][] payoffs) {
		this.payoffs = payoffs;
	}

	/**
	 * Get the payoffs as a {@code 2×2} matrix.
	 * 
	 * @return the payoff matrix
	 */
	public double[][] getPayoffs() {
		return payoffs;
	}

	/**
	 * Set the payoff for type {@code me} against type {@code you} to
	 * {@code payoff}.
	 *
	 * @param payoff the payoff to {@code me}
	 * @param me     the strategy index of the row player
	 * @param you    the strategy index of the column player
	 */
	public void setPayoff(double payoff, int me, int you) {
		if (me < 0 || me > 2 || you < 0 || you > 2)
			return;
		payoffs[me][you] = payoff;
	}

	/**
	 * Get the payoff for type {@code me} against type {@code you}.
	 *
	 * @param me  the strategy index of the row player
	 * @param you the strategy index of the column player
	 * @return the payoff to {@code me}
	 */
	public double getPayoff(int me, int you) {
		return payoffs[me][you];
	}

	/**
	 * Command line option to set the {@code 2×2} payoff matrix for
	 * interactions between cooperators and defectors.
	 */
	public final CLOption cloPayoffs = new CLOption("paymatrix", "1,0;1.65,0", EvoLudo.catModule,
			"--paymatrix <a,b;c,d>  2x2 payoff matrix", new CLODelegate() {

				/**
				 * {@inheritDoc}
				 * <p>
				 * Parse the payoff matrix for {@code 2×2} games. The argument must have
				 * the form 'R,S;T,P' referring to the punishment, temptation, sucker's payoff
				 * and reward, respectively.
				 * <p>
				 * Note '{@value CLOParser#VECTOR_DELIMITER}' separates entries in 1D arrays and
				 * '{@value CLOParser#MATRIX_DELIMITER}' separates rows in 2D arrays.
				 * 
				 * @param arg the payoff matrix
				 */
				@Override
				public boolean parse(String arg) {
					double[][] payMatrix = CLOParser.parseMatrix(arg);
					if (payMatrix == null || payMatrix.length != 2 || payMatrix[0].length != 2) {
						logger.warning("invalid paymatrix (" + arg + ") - using '" + cloPayoffs.getDefault() + "'");
						return false;
					}
					setPayoffs(payMatrix);
					return true;
				}

				@Override
				public void report(PrintStream output) {
					output.println("# paymatrix:            " + Formatter.format(getPayoffs(), 4));
				}
			});

	@Override
	public void collectCLO(CLOParser parser) {
		super.collectCLO(parser);
		parser.addCLO(cloPayoffs);
	}

	@Override
	public void adjustCLO(CLOParser parser) {
		super.adjustCLO(parser);
		if (model instanceof IBSD) {
			CLOption clo = ((IBSDPopulation) getIBSPopulation()).getInit().clo;
			clo.addKey(Init.Type.KALEIDOSCOPE);
		}
		// handling of 2nd neighbours in von Neumann lattice implemented
		cloGeometry.addKey(Geometry.Type.SQUARE_NEUMANN_2ND);
	}

	@Override
	public IBSDPopulation createIBSPop() {
		return new TBT.TBTPop(engine);
	}

	/**
	 * The extension for IBS simulations specific to populations engaging in
	 * {@code 2×2} games. This extension implements customizations for
	 * {@code Geometry.Type.SQUARE_NEUMANN_2ND} as well as specific initial
	 * conditions that give rise to fascinating evolutionary kaleidoscopes for
	 * deterministic updating.
	 */
	public class TBTPop extends IBSDPopulation {

		/**
		 * Timestamp for the last mean trait calculation.
		 */
		double tsMean = -1.0;

		/**
		 * Timestamp for the last fitness calculation.
		 */
		double tsFit = -1.0;

		/**
		 * The trait frequencies for the two sublattices for
		 * {@code Geometry.Type.SQUARE_NEUMANN_2ND}.
		 */
		double[] tsTraits;

		/**
		 * The trait fitnesses for the two sublattices for
		 * {@code Geometry.Type.SQUARE_NEUMANN_2ND}.
		 */
		double[] tsFits;

		/**
		 * Create a new instance of the IBS model for {@code 2×2} games.
		 * 
		 * @param engine the pacemaker for running the model
		 */
		protected TBTPop(EvoLudo engine) {
			super(engine);
		}

		@Override
		public boolean check() {
			boolean doReset = super.check();
			if (competition.getType() != Geometry.Type.SQUARE_NEUMANN_2ND) {
				tsTraits = null;
				tsFits = null;
			} else {
				tsTraits = new double[2 * nTraits];
				tsFits = new double[2 * nTraits + 1];
			}
			tsMean = -1.0;
			tsFit = -1.0;
			return doReset;
		}

		@Override
		public String getTraitNameAt(int idx) {
			if (competition.getType() != Geometry.Type.SQUARE_NEUMANN_2ND)
				return super.getTraitNameAt(idx);
			int side = (int) Math.sqrt(nPopulation);
			int trait = strategies[idx] % nTraits;
			if ((idx / side) % 2 == (idx % side) % 2)
				return module.getTraitName(trait);
			return module.getTraitName(nTraits + trait);
		}

		@Override
		public int getNMean() {
			if (competition.getType() != Geometry.Type.SQUARE_NEUMANN_2ND)
				return super.getNMean();
			return 2 * nTraits;
		}

		@Override
		public void getMeanTraits(double[] mean) {
			// SQUARE_NEUMANN_2ND geometry for competition results in two disjoint
			// sublattices; report strategy frequencies in each sublattice separately
			if (competition.getType() != Geometry.Type.SQUARE_NEUMANN_2ND) {
				super.getMeanTraits(mean);
				return;
			}

			double newtime = model.getTime();
			if (Math.abs(tsMean - newtime) < model.getTimeStep()) {
				System.arraycopy(tsTraits, 0, mean, 0, mean.length);
				return;
			}
			int n = 0;
			Arrays.fill(mean, 0);
			int side = (int) Math.sqrt(nPopulation);
			while (n < nPopulation) {
				if ((n / side) % 2 == 0) {
					mean[strategies[n++] % nTraits]++;
					mean[nTraits + (strategies[n++] % nTraits)]++;
					continue;
				} 
				mean[nTraits + (strategies[n++] % nTraits)]++;
				mean[strategies[n++] % nTraits]++;
			}
			ArrayMath.multiply(mean, 2.0 / nPopulation);
			System.arraycopy(mean, 0, tsTraits, 0, mean.length);
			tsMean = newtime;
		}

		@Override
		public void getMeanFitness(double[] mean) {
			// SQUARE_NEUMANN_2ND geometry for competition results in two disjoint
			// sublattices; report strategy frequencies in each sublattice separately
			if (competition.getType() != Geometry.Type.SQUARE_NEUMANN_2ND) {
				super.getMeanFitness(mean);
				return;
			}

			double newtime = model.getTime();
			if (Math.abs(tsFit - newtime) < model.getTimeStep()) {
				System.arraycopy(tsFits, 0, mean, 0, mean.length);
				return;
			}
			int n = 0;
			Arrays.fill(mean, 0);
			int side = (int) Math.sqrt(nPopulation);
			while (n < nPopulation) {
				if ((n / side) % 2 == 0) {
					mean[strategies[n] % nTraits] += getFitnessAt(n++);
					mean[nTraits + (strategies[n] % nTraits)] += getFitnessAt(n++);
					continue;
				}
				mean[nTraits + (strategies[n] % nTraits)] += getFitnessAt(n++);
				mean[strategies[n] % nTraits] += getFitnessAt(n++);
			}
			// total payoff in last entry
			mean[2 * nTraits] = sumFitness * 0.25;
			// averages for each sublattice
			ArrayMath.multiply(mean, 2.0 / nPopulation);
			System.arraycopy(mean, 0, tsFits, 0, mean.length);
			tsFit = newtime;
		}

		@Override
		public String getStatus() {
			if (competition.getType() != Geometry.Type.SQUARE_NEUMANN_2ND)
				return super.getStatus();

			getMeanTraits(tsTraits);
			String status = "";
			for (int i = 0; i < 2 * nTraits; i++)
				status += (status.length() > 0 ? ", " : "") + module.getTraitName(i) + ": "
						+ Formatter.formatPercent(tsTraits[i], 1);
			return status;
		}

		@Override
		protected void initKaleidoscope() {
			// kaleidoscopes only available for lattice geometries
			if (!interaction.isLattice()) {
				logger.warning("kaleidoscopes require lattices - using default initialization.");
				initUniform();
				return;
			}
			int mid = -1;
			Arrays.fill(strategiesTypeCount, 0);
			switch (interaction.getType()) {
				case CUBE:
					int l, mz;
					if (nPopulation == 25000) {
						l = 50;
						mz = 5; // 10/2
					} else {
						l = (int) (Math.pow(nPopulation, 1.0 / 3.0) + 0.5);
						mz = l / 2;
					}
					int l2 = l * l;
					int m = l / 2;
					double[] args = init.getArgs();
					int type = ((args != null && args.length > 0) ? (int) args[0] : 0);
					switch (type) {
						default:
						case 0:
							if (l % 2 == 1) {
								// odd dimensions (this excludes NOVA, hence mz=m)
								// place single TBT.DEFECT in center
								mid = m * (l2 + l + 1);
								strategiesTypeCount[strategies[mid] % nTraits]--;
								strategies[mid] = TBT.DEFECT;
								strategiesTypeCount[TBT.DEFECT]++;
							} else {
								// even dimensions - place 2x2x2 cube of TBT.DEFECTors in center
								for (int z = mz - 1; z <= mz; z++)
									for (int y = m - 1; y <= m; y++)
										for (int x = m - 1; x <= m; x++)
											strategies[z * l2 + y * l + x] = TBT.DEFECT;
								strategiesTypeCount[TBT.COOPERATE] -= 2 * 2 * 2;
								strategiesTypeCount[TBT.DEFECT] += 2 * 2 * 2;
							}
							break;
						case 1:
							if (l % 2 == 1) {
								// odd dimensions - place 3x3x3 cube of cooperators in center
								for (int z = mz - 1; z <= mz + 1; z++)
									for (int y = m - 1; y <= m + 1; y++)
										for (int x = m - 1; x <= m + 1; x++)
											strategies[z * l2 + y * l + x] = TBT.COOPERATE;
								strategiesTypeCount[TBT.DEFECT] -= 3 * 3 * 3;
								strategiesTypeCount[TBT.COOPERATE] += 3 * 3 * 3;
							} else {
								// even dimensions - place 4x4x4 cube of cooperators in center
								for (int z = mz - 2; z <= mz + 1; z++)
									for (int y = m - 2; y <= m + 1; y++)
										for (int x = m - 2; x <= m + 1; x++)
											strategies[z * l2 + y * l + x] = TBT.COOPERATE;
								strategiesTypeCount[TBT.DEFECT] -= 4 * 4 * 4;
								strategiesTypeCount[TBT.COOPERATE] += 4 * 4 * 4;
							}
							break;
					}
					break;

				case SQUARE_NEUMANN:
				case SQUARE_NEUMANN_2ND:
				case SQUARE_MOORE:
				case SQUARE:
				case HONEYCOMB:
				case TRIANGULAR:
					l = (int) Math.sqrt(nPopulation);
					m = l / 2;
					mid = m * (l + 1);
					// $FALL-THROUGH$

				case LINEAR:
					if (mid < 0)
						mid = nPopulation / 2;
					int restrait = TBT.COOPERATE;
					Arrays.fill(strategies, restrait);
					int muttrait = (restrait + 1) % nTraits;
					strategies[mid] = muttrait;
					strategiesTypeCount[restrait] = nPopulation - 1;
					strategiesTypeCount[muttrait] = 1;
					break;

				default:
					// should never get here - check made sure of it.
					throw new Error("geometry incompatible with kaleidoscopes!");
			}
		}
	}
}
