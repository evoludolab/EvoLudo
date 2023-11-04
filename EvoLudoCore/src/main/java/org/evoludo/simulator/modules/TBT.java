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
import org.evoludo.simulator.EvoLudo;
import org.evoludo.simulator.models.IBS.HasIBS;
import org.evoludo.simulator.models.IBSD.InitType;
import org.evoludo.simulator.models.Model.Type;
import org.evoludo.simulator.models.IBSDPopulation;
import org.evoludo.simulator.models.ODEEuler.HasODE;
import org.evoludo.simulator.models.PDERD.HasPDE;
import org.evoludo.simulator.models.SDEEuler.HasSDE;
import org.evoludo.simulator.modules.Discrete.Pairs;
import org.evoludo.simulator.views.HasConsole;
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
 * 		\begin{matrix} &amp;\hspace{-3ex} C &amp;\hspace{-1ex} D \end{matrix} \\
 * 		\begin{matrix} C\\ D \end{matrix} &amp; \hspace{-1ex}
 * 		\begin{pmatrix}R &amp; S\\ T &amp; P\end{pmatrix}
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
public class TBT extends Discrete implements Pairs,
		HasIBS, HasODE, HasSDE, HasPDE,
		HasPop2D.Strategy, HasPop3D.Strategy, HasMean.Strategy,
		HasPop2D.Fitness, HasPop3D.Fitness, HasMean.Fitness, HasHistogram.Fitness,
		HasHistogram.Degree, HasHistogram.StatisticsProbability, HasHistogram.StatisticsTime, HasConsole {

	/**
	 * The trait (and index) value of defectors.
	 */
	public static final int DEFECT = 0;

	/**
	 * The trait (and index) value of cooperators.
	 */
	public static final int COOPERATE = 1;

	/**
	 * The payoff for mutual cooperation. Traditionally termed the <em>reward</em>
	 * {@code R}.
	 */
	double reward = 1.0;

	/**
	 * The payoff to a cooperator interacting with a defector. Traditionally termed
	 * the <em>sucker's payoff</em> {@code S}.
	 */
	double sucker = 0.0;

	/**
	 * The payoff to a defector interacting with a cooperator. Traditionally termed
	 * the <em>temptation to defect</em> {@code T}.
	 */
	double temptation = 1.65;

	/**
	 * The payoff for mutual defection. Traditionally termed the <em>punishment</em>
	 * {@code P}.
	 */
	double punishment = 0.0;

	/**
	 * Create a new instance of the module for {@code 2×2} games.
	 * 
	 * @param engine the manager of modules and pacemeaker for running the model
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
	}

	@Override
	public void modelLoaded() {
		super.modelLoaded();
		if (model.isModelType(Type.IBS))
			engine.cloInitType.addKey(InitType.KALEIDOSCOPE);
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
	public int getDependent() {
		return DEFECT;
	}

	@Override
	public double getMinGameScore() {
		return Math.min(reward, Math.min(sucker, Math.min(temptation, punishment)));
	}

	@Override
	public double getMaxGameScore() {
		return Math.max(reward, Math.max(sucker, Math.max(temptation, punishment)));
	}

	@Override
	public double getMonoGameScore(int type) {
		switch (type) {
			case COOPERATE:
				return reward;
			case DEFECT:
				return punishment;
			default:
				return Double.NaN;
		}
	}

	@Override
	public double pairScores(int me, int[] tCount, double[] tScore) {
		switch (me) {
			case COOPERATE:
				tScore[COOPERATE] = reward;
				tScore[DEFECT] = temptation;
				return tCount[COOPERATE] * reward + tCount[DEFECT] * sucker;

			case DEFECT:
				tScore[COOPERATE] = sucker;
				tScore[DEFECT] = punishment;
				return tCount[COOPERATE] * temptation + tCount[DEFECT] * punishment;

			default: // should not end here
				throw new Error("Unknown strategy (" + me + ")");
		}
	}

	@Override
	public void avgScores(double[] density, int n, double[] avgscores) {
		avgscores[COOPERATE] = density[COOPERATE] * reward + density[DEFECT] * sucker;
		avgscores[DEFECT] = density[COOPERATE] * temptation + density[DEFECT] * punishment;
	}

	@Override
	public void mixedScores(int[] count, double[] traitScores) {
		int x = count[COOPERATE];
		int y = count[DEFECT];
		double in = 1.0 / (x + y - 1);
		traitScores[COOPERATE] = ((x - 1) * reward + y * sucker) * in;
		traitScores[DEFECT] = (x * temptation + (y - 1) * punishment) * in;
	}

	/**
	 * Set the payoffs from the {@code 2×2} matrix {@code payoffs}.
	 * 
	 * @param payoffs the payoff matrix
	 */
	public void setPayoffs(double[][] payoffs) {
		reward = payoffs[COOPERATE][COOPERATE];
		sucker = payoffs[COOPERATE][DEFECT];
		temptation = payoffs[DEFECT][COOPERATE];
		punishment = payoffs[DEFECT][DEFECT];
	}

	/**
	 * Get the payoffs as a {@code 2×2} matrix.
	 * 
	 * @return the payoff matrix
	 */
	public double[][] getPayoffs() {
		final double[][] payoffs = new double[2][2];
		payoffs[COOPERATE][COOPERATE] = reward;
		payoffs[COOPERATE][DEFECT] = sucker;
		payoffs[DEFECT][COOPERATE] = temptation;
		payoffs[DEFECT][DEFECT] = punishment;
		return payoffs;
	}

	/**
	 * Set the payoff of strategy {@code me} interacting with strategy {@code you}
	 * to {@code payoff}.
	 * 
	 * @param payoff the payoff to {@code me}
	 * @param me     the strategy index of the row player
	 * @param you    the strategy index of the column player
	 */
	public void setPayoff(double payoff, int me, int you) {
		if (me == COOPERATE) {
			if (you == COOPERATE)
				reward = payoff;
			else
				sucker = payoff;
			return;
		}
		if (you == DEFECT)
			punishment = payoff;
		else
			temptation = payoff;
	}

	/**
	 * Get the payoff of strategy {@code me} interacting with strategy {@code you}.
	 *
	 * @param me  the strategy index of the row player
	 * @param you the strategy index of the column player
	 * @return the payoff to {@code me}
	 */
	public double getPayoff(int me, int you) {
		if (me == COOPERATE) {
			if (you == COOPERATE)
				return reward;
			return sucker;
		}
		if (you == DEFECT)
			return punishment;
		return temptation;
	}

	/**
	 * Command line option to set the payoff for mutual cooperation.
	 */
	public final CLOption cloReward = new CLOption("reward", "1", EvoLudo.catModule,
			"--reward <r>    reward for mutual cooperation", new CLODelegate() {

				/**
				 * {@inheritDoc}
				 * <p>
				 * Parse the payoff for mutual cooperation.
				 * 
				 * @param arg the reward
				 */
				@Override
				public boolean parse(String arg) {
					if (cloPayoffs.isSet()) {
						if (cloReward.isSet())
							logger.warning("option --paymatrix " + cloPayoffs.getArg() + " takes precedence; --reward "
									+ cloReward.getArg() + " ignored!");
						// this signals no problems to suppress further parsing attempts
						return true;
					}
					setPayoff(CLOParser.parseDouble(arg), COOPERATE, COOPERATE);
					return true;
				}

				@Override
				public void report(PrintStream output) {
					output.println("# reward:               " + Formatter.format(getPayoff(COOPERATE, COOPERATE), 4));
				}
			});

	/**
	 * Command line option to set the sucker's payoff of a cooperator facing a
	 * defector.
	 */
	public final CLOption cloSucker = new CLOption("sucker", "0", EvoLudo.catModule,
			"--sucker <s>    sucker's payoff for being cheated", new CLODelegate() {
				/**
				 * {@inheritDoc}
				 * <p>
				 * Parse the payoff to a cooperator against a defector.
				 * 
				 * @param arg the sucker's payoff
				 */
				@Override
				public boolean parse(String arg) {
					if (cloPayoffs.isSet()) {
						if (cloSucker.isSet())
							logger.warning("option --paymatrix " + cloPayoffs.getArg() + " takes precedence; --sucker "
									+ cloSucker.getArg() + " ignored!");
						// this signals no problems to suppress further parsing attempts
						return true;
					}
					setPayoff(CLOParser.parseDouble(arg), COOPERATE, DEFECT);
					return true;
				}

				@Override
				public void report(PrintStream output) {
					output.println("# sucker:               " + Formatter.format(getPayoff(COOPERATE, DEFECT), 4));
				}
			});

	/**
	 * Command line option to set the payoff for the temptation to defect for a
	 * defector encountering a cooperator.
	 */
	public final CLOption cloTemptation = new CLOption("temptation", "1.65", EvoLudo.catModule,
			"--temptation <t>  temptation to defect", new CLODelegate() {

				/**
				 * {@inheritDoc}
				 * <p>
				 * Parse the payoff to a defector interacting with a cooperator.
				 * 
				 * @param arg the temptation
				 */
				@Override
				public boolean parse(String arg) {
					if (cloPayoffs.isSet()) {
						if (cloTemptation.isSet())
							logger.warning("option --paymatrix " + cloPayoffs.getArg()
									+ " takes precedence; --temptation " + cloTemptation.getArg() + " ignored!");
						// this signals no problems to suppress further parsing attempts
						return true;
					}
					setPayoff(CLOParser.parseDouble(arg), DEFECT, COOPERATE);
					return true;
				}

				@Override
				public void report(PrintStream output) {
					output.println("# temptation:           " + Formatter.format(getPayoff(DEFECT, COOPERATE), 4));
				}
			});

	/**
	 * Command line option to set the payoff for mutual defection.
	 */
	public final CLOption cloPunishment = new CLOption("punishment", "0", EvoLudo.catModule,
			"--punishment <p>  punishment for mutual defection", new CLODelegate() {

				/**
				 * {@inheritDoc}
				 * <p>
				 * Parse the payoff for mutual defection.
				 * 
				 * @param arg the punishment
				 */
				@Override
				public boolean parse(String arg) {
					if (cloPayoffs.isSet()) {
						if (cloPunishment.isSet())
							logger.warning("option --paymatrix " + cloPayoffs.getArg()
									+ " takes precedence; --punishment " + cloPunishment.getArg() + " ignored!");
						// this signals no problems to suppress further parsing attempts
						return true;
					}
					setPayoff(CLOParser.parseDouble(arg), DEFECT, DEFECT);
					return true;
				}

				@Override
				public void report(PrintStream output) {
					output.println("# punishment:           " + Formatter.format(getPayoff(DEFECT, DEFECT), 4));
				}
			});

	/**
	 * Command line option to set the {@code 2×2} payoff matrix for
	 * interactions between cooperators and defectors.
	 */
	public final CLOption cloPayoffs = new CLOption("paymatrix", "0,1.65;0,1", EvoLudo.catModule,
			"--paymatrix <a,b;c,d>  2x2 payoff matrix", new CLODelegate() {

				/**
				 * {@inheritDoc}
				 * <p>
				 * Parse the payoff matrix for {@code 2×2} games. The argument must have
				 * the form 'P,T;S,R' referring to the punishment, temptation, sucker's payoff
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
		parser.addCLO(cloReward);
		parser.addCLO(cloSucker);
		parser.addCLO(cloTemptation);
		parser.addCLO(cloPunishment);
		parser.addCLO(cloPayoffs);
	}

	@Override
	public IBSDPopulation createIBSPop() {
		return new TBT.IBS(engine);
	}

	/**
	 * The extension for IBS simulations specific to {@code 2×2} games. This
	 * extension implements specific initial conditions that give rise to
	 * fascinating evolutionary kaleidoscopes for deterministic updating.
	 *
	 * @author Christoph Hauert
	 */
	public class IBS extends IBSDPopulation {

		/**
		 * Create a new instance of the IBS model for {@code 2×2} games.
		 * 
		 * @param engine the pacemeaker for running the model
		 */
		protected IBS(EvoLudo engine) {
			super(engine);
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
					switch (ArrayMath.maxIndex(init)) {
						default:
						case TBT.COOPERATE:
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
						case TBT.DEFECT:
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
					int restrait = ArrayMath.maxIndex(init);
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
