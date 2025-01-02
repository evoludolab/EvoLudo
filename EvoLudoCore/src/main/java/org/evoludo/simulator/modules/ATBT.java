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

import org.evoludo.geom.Point2D;
import org.evoludo.math.ArrayMath;
import org.evoludo.simulator.ColorMap;
import org.evoludo.simulator.EvoLudo;
import org.evoludo.simulator.models.IBS.HasIBS;
import org.evoludo.simulator.models.IBSD;
import org.evoludo.simulator.models.IBSD.Init;
import org.evoludo.simulator.models.IBSDPopulation;
import org.evoludo.simulator.models.Model;
import org.evoludo.simulator.models.ODEEuler.HasODE;
import org.evoludo.simulator.models.ODERK;
import org.evoludo.simulator.models.PDERD.HasPDE;
import org.evoludo.simulator.models.SDEEuler.HasSDE;
import org.evoludo.simulator.views.BasicTooltipProvider;
import org.evoludo.simulator.views.HasHistogram;
import org.evoludo.simulator.views.HasMean;
import org.evoludo.simulator.views.HasPhase2D;
import org.evoludo.simulator.views.HasPop2D;
import org.evoludo.simulator.views.HasPop3D;
import org.evoludo.simulator.views.HasS3;
import org.evoludo.util.CLOParser;
import org.evoludo.util.CLOption;
import org.evoludo.util.CLOption.CLODelegate;
import org.evoludo.util.Formatter;

/**
 * The module for investigating the evolutionary dynamics in asymmetric
 * {@code 2×2} games. The origin of asymmetries can either be due to
 * environmental differences where individuals occupy rich or poor sites or due
 * to genetic differences, e.g. with weak and strong types. The key distinction
 * between the two scenarios is that in the former case, the offspring only
 * inherits the strategy but not the patch quality, whereas in the latter both
 * the strategy and type are are transmitted to the offspring.
 *
 * @author Christoph Hauert
 */
public class ATBT extends TBT implements HasIBS.DPairs, HasODE, HasSDE, HasPDE,
		HasPop2D.Strategy, HasPop3D.Strategy, HasMean.Strategy, HasS3, HasPhase2D, HasPop2D.Fitness, HasPop3D.Fitness,
		HasMean.Fitness, HasHistogram.Fitness, HasHistogram.Degree, HasHistogram.StatisticsStationary {

	/**
	 * The identifier of rich sites: {@code (int) (trait / 2)}.
	 */
	public static final int RICH = 0;

	/**
	 * The identifier of poor sites: {@code (int) (trait / 2)}.
	 */
	public static final int POOR = 1;

	/**
	 * The trait (and index) value of rich cooperators.
	 */
	public static final int COOPERATE_RICH = 2 * RICH + COOPERATE; // 0

	/**
	 * The trait (and index) value of poor cooperators.
	 */
	public static final int COOPERATE_POOR = 2 * POOR + COOPERATE; // 2

	/**
	 * The trait (and index) value of rich defectors.
	 */
	public static final int DEFECT_RICH = 2 * RICH + DEFECT; // 1

	/**
	 * The trait (and index) value of poor defectors.
	 */
	public static final int DEFECT_POOR = 2 * POOR + DEFECT; // 3

	/**
	 * The {@code 2×2} payoff matrix for interactions between cooperators and
	 * defectors.
	 */
	double[][] game;

	/**
	 * The array of environmental values for rich and poor sites.
	 */
	double[] environment;

	/**
	 * The array with environmental feedback rates.
	 * 
	 * @see #setFeedback(double[])
	 */
	double[] feedback;

	/**
	 * The flag indicating the type of environmental asymmetry. If {@code true}
	 * asymmetries are due to environmental differences (non-heritable) and if
	 * {@code false} asymmetries have a genetic origin (heritable).
	 */
	boolean environmentalAsymmetry = true;

	/**
	 * Create a new instance of the module for asymmetric {@code 2×2} games.
	 * 
	 * @param engine the manager of modules and pacemaker for running the model
	 */
	public ATBT(EvoLudo engine) {
		super(engine);
	}

	@Override
	public void load() {
		super.load();
		nTraits = 4;
		// trait names
		String[] names = new String[nTraits];
		names[COOPERATE_RICH] = "Rich Cooperator";
		names[DEFECT_RICH] = "Rich Defector";
		names[COOPERATE_POOR] = "Poor Cooperator";
		names[DEFECT_POOR] = "Poor Defector";
		setTraitNames(names);
		// trait colors (automatically generates lighter versions for new strategists)
		Color[] colors = new Color[nTraits];
		colors[COOPERATE_RICH] = Color.BLUE;
		colors[DEFECT_RICH] = Color.RED;
		colors[COOPERATE_POOR] = ColorMap.blendColors(Color.BLUE, Color.BLACK, 0.5);
		colors[DEFECT_POOR] = ColorMap.blendColors(Color.RED, Color.BLACK, 0.5);
		setTraitColors(colors);
		// allocate
		game = payoffs;	// reuse payoffs from TBT
		environment = new double[nTraits / 2];
		feedback = new double[nTraits];
		payoffs = new double[nTraits][nTraits];
	}

	@Override
	public void unload() {
		super.unload();
		game = null;
		environment = null;
		feedback = null;
	}

	@Override
	public String getKey() {
		return "a2x2";
	}

	@Override
	public String getInfo() {
		return "Title: " + getTitle()
				+ "\nAuthor: Christoph Hauert\nEvolution under environmentally and genetically induced asymmetries.";
	}

	@Override
	public String getTitle() {
		return "Asymmetric 2x2 Games";
	}

	@Override
	public String getVersion() {
		return "v1.0 April 2021";
	}

	@Override
	public int getDependent() {
		return DEFECT_POOR;
	}

	@Override
	public double pairScores(int me, int[] traitCount, double[] traitScore) {
		// need trait counts as double array for dot-product
		double[] state = new double[] { traitCount[COOPERATE_RICH], traitCount[DEFECT_RICH], traitCount[COOPERATE_POOR],
			traitCount[DEFECT_POOR] };

		switch (me) {
			case COOPERATE_RICH:
				traitScore[COOPERATE_RICH] = payoffs[COOPERATE_RICH][COOPERATE_RICH];
				traitScore[DEFECT_RICH] = payoffs[DEFECT_RICH][COOPERATE_RICH];
				traitScore[COOPERATE_POOR] = payoffs[COOPERATE_POOR][COOPERATE_RICH];
				traitScore[DEFECT_POOR] = payoffs[DEFECT_POOR][COOPERATE_RICH];
				return ArrayMath.dot(payoffs[COOPERATE_RICH], state);

			case DEFECT_RICH:
				traitScore[COOPERATE_RICH] = payoffs[COOPERATE_RICH][DEFECT_RICH];
				traitScore[DEFECT_RICH] = payoffs[DEFECT_RICH][DEFECT_RICH];
				traitScore[COOPERATE_POOR] = payoffs[COOPERATE_POOR][DEFECT_RICH];
				traitScore[DEFECT_POOR] = payoffs[DEFECT_POOR][DEFECT_RICH];
				return ArrayMath.dot(payoffs[DEFECT_RICH], state);

			case COOPERATE_POOR:
				traitScore[COOPERATE_RICH] = payoffs[COOPERATE_RICH][COOPERATE_POOR];
				traitScore[DEFECT_RICH] = payoffs[DEFECT_RICH][COOPERATE_POOR];
				traitScore[COOPERATE_POOR] = payoffs[COOPERATE_POOR][COOPERATE_POOR];
				traitScore[DEFECT_POOR] = payoffs[DEFECT_POOR][COOPERATE_POOR];
				return ArrayMath.dot(payoffs[COOPERATE_POOR], state);

			case DEFECT_POOR:
				traitScore[COOPERATE_RICH] = payoffs[COOPERATE_RICH][DEFECT_POOR];
				traitScore[DEFECT_RICH] = payoffs[DEFECT_RICH][DEFECT_POOR];
				traitScore[COOPERATE_POOR] = payoffs[COOPERATE_POOR][DEFECT_POOR];
				traitScore[DEFECT_POOR] = payoffs[DEFECT_POOR][DEFECT_POOR];
				return ArrayMath.dot(payoffs[DEFECT_POOR], state);

			default: // should not end here
				throw new Error("Unknown strategy (" + me + ")");
		}
	}

	@Override
	public void avgScores(double[] density, int n, double[] avgscores) {
		avgscores[COOPERATE_RICH] = ArrayMath.dot(payoffs[COOPERATE_RICH], density);
		avgscores[DEFECT_RICH] = ArrayMath.dot(payoffs[DEFECT_RICH], density);
		avgscores[COOPERATE_POOR] = ArrayMath.dot(payoffs[COOPERATE_POOR], density);
		avgscores[DEFECT_POOR] = ArrayMath.dot(payoffs[DEFECT_POOR], density);
	}

	@Override
	public void mixedScores(int[] count, double[] traitScores) {
		double[] density = new double[] { count[COOPERATE_RICH], count[DEFECT_RICH], count[COOPERATE_POOR],
				count[DEFECT_POOR] };
		double in = 1.0 / (ArrayMath.norm(density) - 1.0);
		traitScores[COOPERATE_RICH] = (ArrayMath.dot(payoffs[COOPERATE_RICH], density)
				- payoffs[COOPERATE_RICH][COOPERATE_RICH]) * in;
		traitScores[DEFECT_RICH] = (ArrayMath.dot(payoffs[DEFECT_RICH], density)
				- payoffs[DEFECT_RICH][DEFECT_RICH]) * in;
		traitScores[COOPERATE_POOR] = (ArrayMath.dot(payoffs[COOPERATE_POOR], density)
				- payoffs[COOPERATE_POOR][COOPERATE_POOR]) * in;
		traitScores[DEFECT_POOR] = (ArrayMath.dot(payoffs[DEFECT_POOR], density)
				- payoffs[DEFECT_POOR][DEFECT_POOR]) * in;
	}

	@Override
	public boolean check() {
		super.check();
		// build payoff matrix (for all models) - if necessary
		if (!init4x4) {
			payoffs[COOPERATE_RICH][COOPERATE_RICH] = payoffs[COOPERATE_RICH][COOPERATE_POOR] //
					= game[COOPERATE][COOPERATE] + environment[RICH];
			payoffs[COOPERATE_RICH][DEFECT_RICH] = payoffs[COOPERATE_RICH][DEFECT_POOR] //
					= game[COOPERATE][DEFECT] + environment[RICH];
			payoffs[DEFECT_RICH][COOPERATE_RICH] = payoffs[DEFECT_RICH][COOPERATE_POOR] //
					= game[DEFECT][COOPERATE] + environment[RICH];
			payoffs[DEFECT_RICH][DEFECT_RICH] = payoffs[DEFECT_RICH][DEFECT_POOR] //
					= game[DEFECT][DEFECT] + environment[RICH];
			payoffs[COOPERATE_POOR][COOPERATE_RICH] = payoffs[COOPERATE_POOR][COOPERATE_POOR] //
					= game[COOPERATE][COOPERATE] + environment[POOR];
			payoffs[COOPERATE_POOR][DEFECT_RICH] = payoffs[COOPERATE_POOR][DEFECT_POOR] //
					= game[COOPERATE][DEFECT] + environment[POOR];
			payoffs[DEFECT_POOR][COOPERATE_RICH] = payoffs[DEFECT_POOR][COOPERATE_POOR] //
					= game[DEFECT][COOPERATE] + environment[POOR];
			payoffs[DEFECT_POOR][DEFECT_RICH] = payoffs[DEFECT_POOR][DEFECT_POOR] //
					= game[DEFECT][DEFECT] + environment[POOR];
		}
		// IMPORTANT: for DE-models feedbacks are rates, for simulations we need to
		// ensure we are dealing with probabilities; better would be to somehow scale
		// time to make these rates compatible with probabilities - this requires more
		// thought.
		for (int i = 0; i < nTraits; i++)
			feedback[i] = Math.max(0.0, Math.min(1.0, feedback[i]));
		return false;
	}

	/**
	 * The flag to indicate whether a generic {@code 4×4} was provided to for
	 * the interactions among the four strategy types.
	 */
	private boolean init4x4;

	/**
	 * Set the payoff matrix to {@code payoffs}, which can be a {@code 2×2} or
	 * {@code 4×4} matrix. For {@code 2×2} matrices, it specifies the
	 * payoffs for interactions between cooperators and defectors, while for
	 * {@code 4×4} matrices any generic payoff matrix for interactions among
	 * four types of strategies is possible.
	 *
	 * @param payoffs the payoff matrix
	 */
	@Override
	public void setPayoffs(double[][] payoffs) {
		init4x4 = false;
		// check if 2x2 or 4x4
		if (payoffs == null || (payoffs.length != 2 && payoffs.length != 4 && payoffs.length != payoffs[0].length))
			return;
		if (payoffs.length == 2) {
			game = payoffs;
			return;
		}
		init4x4 = true;
		this.payoffs = payoffs;
	}

	/**
	 * Set the flag whether asymmetries are due to environmental differences, i.e.
	 * patch qualities (as opposed to genetic differences).
	 *
	 * @param asym {@code true} if asymmetries are of environmental origin
	 */
	public void setEnvironmentalAsymmetry(boolean asym) {
		environmentalAsymmetry = asym;
	}

	/**
	 * Check whether asymmetries are due to environmental differences, i.e. patch
	 * qualities (as opposed to genetic differences).
	 *
	 * @return {@code true} if asymmetries are of environmental origin
	 */
	public boolean getEnvironmentalAsymmetry() {
		return environmentalAsymmetry;
	}

	/**
	 * Set the feedback between strategic types and patch qualities:
	 * <ol>
	 * <li>cooperators restoring poor sites
	 * <li>defectors degrading rich sites
	 * <li>cooperators degrading rich sites
	 * <li>defectors restoring poor sites
	 * </ol>
	 * The array {@code feedback} can have one, two or four elements:
	 * <ol>
	 * <li>all four rates/probabilities are the same
	 * <li>rates/probabilities for cooperators restoring poor sites and defectors
	 * degrading rich sites. The other two, cooperators degrading rich sites and
	 * defectors restoring poor sites are set to zero.
	 * <li value="4">all four rates/probabilities are set.
	 * </ol>
	 * 
	 * @param feedback the array with feedback rates/probabilities
	 * @return {@code true} if feedback successfully set
	 */
	public boolean setFeedback(double[] feedback) {
		if (feedback == null)
			return false;
		// recall:
		// "--feedback <Cb→g:Dg→b:Cg→b:Db→g> feedback between strategies and patches"
		switch (feedback.length) {
			case 1:
				Arrays.fill(this.feedback, feedback[0]);
				break;
			case 2:
				// assume degradation of defectors and restoration of cooperators
				this.feedback[COOPERATE_RICH] = 0.0; // degradation of cooperators
				this.feedback[DEFECT_RICH] = feedback[1]; // degradation of defectors
				this.feedback[COOPERATE_POOR] = feedback[0]; // restoration of cooperators
				this.feedback[DEFECT_POOR] = 0.0; // restoration of defectors
				break;
			case 4:
				this.feedback[COOPERATE_RICH] = feedback[2]; // degradation of cooperators
				this.feedback[DEFECT_RICH] = feedback[1]; // degradation of defectors
				this.feedback[COOPERATE_POOR] = feedback[0]; // restoration of cooperators
				this.feedback[DEFECT_POOR] = feedback[3]; // restoration of defectors
				break;
			default:
				return false;
		}
		return true;
	}

	/**
	 * Get the array with feedback rates/probabilities.
	 * 
	 * @return the feedback array
	 * 
	 * @see #setFeedback(double[])
	 */
	public double[] getFeedback() {
		return feedback;
	}

	/**
	 * Set the (array of) environmental values or patch qualities for rich and poor
	 * sites.
	 *
	 * @param environment the array of patch values
	 * @return {@code true} if environmental values successfully set
	 */
	public boolean setEnvironment(double[] environment) {
		if (environment == null)
			return false;
		// recall: "--environment <g[:b]> payoff on good (bad) patches",
		switch (environment.length) {
			case 1:
				this.environment[RICH] = environment[0];
				this.environment[POOR] = environment[0];
				break;
			case 2:
				this.environment[RICH] = environment[0];
				this.environment[POOR] = environment[1];
				break;
			default:
				return false;
		}
		return true;
	}

	/**
	 * Get the environmental values or patch qualities for rich and poor sites.
	 * 
	 * @return the array of patch values
	 */
	public double[] getEnvironment() {
		// recall: "--environment <g[:b]> payoff on good (bad) patches",
		return environment;
	}

	/**
	 * The map for projecting the four dimensional dynamical system onto a 2D phase
	 * plane.
	 */
	ATBTMap map;

	@Override
	public Data2Phase getPhase2DMap() {
		map = new ATBTMap();
		return map;
	}

	/**
	 * The class that defines the mapping of asymmetric {@code 2×2} games onto
	 * a 2D phase plane: fraction of cooperators along {@code x}-axis and fraction
	 * of rich patches along {@code y}-axis.
	 */
	public class ATBTMap implements Data2Phase, BasicTooltipProvider {

		/**
		 * Constructs a new map to translate between population states and the phase
		 * plane.
		 */
		public ATBTMap() {
		}

		@Override
		public boolean data2Phase(double[] data, Point2D point) {
			// NOTE: data[0] is time!
			point.x = data[COOPERATE_RICH + 1] + data[COOPERATE_POOR + 1];
			point.y = data[COOPERATE_RICH + 1] + data[DEFECT_RICH + 1];
			return true;
		}

		@Override
		public boolean phase2Data(Point2D point, double[] data) {
			// mapping: xr=x*a+m, xp=x(1-a)-m, yr=(1-x)a-m, yp=(1-x)(1-a)+m
			// assume no linkage, i.e. m=0
			data[COOPERATE_RICH] = point.x * point.y;
			data[COOPERATE_POOR] = point.x * (1.0 - point.y);
			data[DEFECT_RICH] = (1.0 - point.x) * point.y;
			data[DEFECT_POOR] = (1.0 - point.x) * (1.0 - point.y);
			return true;
		}

		@Override
		public String getXAxisLabel() {
			return "cooperator frequency";
		}

		@Override
		public String getYAxisLabel() {
			return "frequency of rich patches";
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

	/**
	 * Command line option to set the {@code 2×2} payoff matrix for
	 * interactions between cooperators and defectors or the (generic)
	 * {@code 4×4} payoff matrix for arbitrary interactions between four
	 * strategic types.
	 */
	public final CLOption cloPayoffs4x4 = new CLOption("paymatrix", "1,0;1.65,0", EvoLudo.catModule,
			"--paymatrix <a,b;c,d>   2x2 (or 4x4) payoff matrix", new CLODelegate() {

				/**
				 * {@inheritDoc}
				 * <p>
				 * Parse the payoff matrix for {@code 2×2} games. The argument must have
				 * the form {@code 'P,T;S,R'} referring to the punishment, temptation, sucker's
				 * payoff and reward, respectively. Similarly the generic payoff matrix for
				 * {@code 4×4} games is
				 * {@code 'a00,a01,a02,a03;a10,11,a12,a13;a20,a21,a22,a23;a30,a31,a32,a33'}.
				 * <p>
				 * Note '{@value CLOParser#VECTOR_DELIMITER}' separates entries in 1D arrays and
				 * '{@value CLOParser#MATRIX_DELIMITER}' separates rows in 2D arrays.
				 * 
				 * @param arg the payoff matrix
				 */
				@Override
				public boolean parse(String arg) {
					double[][] payMatrix = CLOParser.parseMatrix(arg);
					if (payMatrix == null || (payMatrix.length != 2 && payMatrix.length != 4) || payMatrix[0].length != payMatrix.length) {
						logger.warning("invalid payoffs (" + arg + ") - using '" + cloPayoffs4x4.getDefault() + "'");
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

	/**
	 * Command line option to set the environmental values of the two different
	 * patch types.
	 */
	public final CLOption cloEnvironment = new CLOption("environment", "0,0", EvoLudo.catModule,
			"--environment <g[,b]>   payoff on rich (poor) patches", new CLODelegate() {

				/**
				 * {@inheritDoc}
				 * <p>
				 * Parse the environmental values of the two different patch types.
				 * 
				 * @param arg the value(s) of the patches
				 * 
				 * @see ATBT#setEnvironment(double[])
				 */
				@Override
				public boolean parse(String arg) {
					if (!setEnvironment(CLOParser.parseVector(arg))) {
						logger.warning(
								"invalid environment (" + arg + ") - using '" + cloEnvironment.getDefault() + "'");
						return false;
					}
					return true;
				}

				@Override
				public void report(PrintStream output) {
					output.println("# environment:          " + Formatter.format(getEnvironment(), 4));
				}
			});

	/**
	 * Command line option to set the origin of asymmetries to differences in
	 * genotype or in the environment.
	 */
	public final CLOption cloAsymmetry = new CLOption("asymmetry", "e", CLOption.Argument.NONE, EvoLudo.catModule,
			"--asymmetry <a>    type of asymmetry\n" //
					+ "             g:    genetic (inherited) asymmetries\n" //
					+ "             e:    environmental (default)",
			new CLODelegate() {
				@Override
				public boolean parse(String arg) {
					setEnvironmentalAsymmetry(arg.charAt(0) == 'e');
					return true;
				}

				@Override
				public void report(PrintStream output) {
					output.println(
							"# asymmetry:            " + (environmentalAsymmetry ? "environmental" : "genetical"));
				}
			});

	/**
	 * Command line option to set the feedback between strategic types and patch
	 * quality.
	 */
	public final CLOption cloFeedback = new CLOption("feedback", "0,0,0,0", EvoLudo.catModule,
			"--feedback <Cp→r,Dr→p[,Cr→p,Dp→r]>   feedback between strategies and patches\n"
					+ "             p→r:  restoration for strategy C and D\n"
					+ "             r→p:  degradation for strategy C and D",
			new CLODelegate() {

				/**
				 * {@inheritDoc}
				 * <p>
				 * Parse the feedback rate/probability at which strategies degrade or restore
				 * the quality of their patch.
				 * 
				 * @param arg the array with degradation and restoration rates/probabilities
				 * 
				 * @see ATBT#setFeedback(double[])
				 */
				@Override
				public boolean parse(String arg) {
					if (!setFeedback(CLOParser.parseVector(arg))) {
						logger.warning("invalid feedback (" + arg + ") - using '" + cloFeedback.getDefault() + "'");
						return false;
					}
					return true;
				}

				// recall:
				// "--feedback <Cb→g:Dg→b:Cg→b:Db→g> feedback between strategies and patches"
				@Override
				public void report(PrintStream output) {
					output.println(
							"# feedback:             " + Formatter.format(new double[] { feedback[COOPERATE_POOR],
									feedback[DEFECT_RICH], feedback[COOPERATE_RICH], feedback[DEFECT_POOR] }, 4));
				}
			});

	@Override
	public void collectCLO(CLOParser parser) {
		super.collectCLO(parser);
		// ATBT provides its own implementation of the --paymatrix option
		parser.removeCLO("paymatrix");
		init4x4 = false;
		// prepare command line options
		parser.addCLO(cloPayoffs4x4);
		parser.addCLO(cloEnvironment);
		parser.addCLO(cloAsymmetry);
		parser.addCLO(cloFeedback);
	}

	@Override
	public void adjustCLO(CLOParser parser) {
		super.adjustCLO(parser);
		if (model instanceof IBSD) {
			CLOption clo = ((IBSDPopulation) getIBSPopulation()).getInit().clo;
			clo.addKey(Init.Type.KALEIDOSCOPE);
		}
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * <strong>Note:</strong> Cannot return more specific type due to subclass
	 * {@link CG#createIBSPop()}
	 */
	@Override
	public ATBT.ATBTPop createIBSPop() {
		return new ATBT.ATBTPop(engine, this);
	}

	@Override
	public Model createODE() {
		return new ATBT.ODE(engine);
	}

	/**
	 * Extends TBT.TBTPop to take advantage of kaleidoscope initializations.
	 */
	public class ATBTPop extends TBT.TBTPop {

		/**
		 * Create a new instance of the IBS model for asymmteric {@code 2×2}
		 * games.
		 * 
		 * @param engine the pacemaker for running the model
		 * @param module the module that defines the game
		 */
		protected ATBTPop(EvoLudo engine, ATBT module) {
			super(engine, module);
		}

		@Override
		public boolean updatePlayerAt(int me) {
			return processEnvironmentalAsymmetryAt(me, super.updatePlayerAt(me));
		}

		@Override
		public boolean isMonomorphic() {
			if (ArrayMath.max(feedback) > 0.0)
				return super.isMonomorphic();
			return ((strategiesTypeCount[ATBT.COOPERATE_POOR] + strategiesTypeCount[ATBT.COOPERATE_RICH] == nPopulation)
					|| (strategiesTypeCount[ATBT.DEFECT_POOR] + strategiesTypeCount[ATBT.DEFECT_RICH] == nPopulation));
		}

		/**
		 * Helper method to process environmental asymmetries. Ensures that only
		 * strategies can be adopted by offspring and processes environmental changes
		 * through feedback with strategic types.
		 * <p>
		 * <strong>Important:</strong> This requires that strategies are not yet
		 * committed.
		 * 
		 * @param me      the index of the focal individual
		 * @param changed the flag whether the focal individual changed strategy
		 * @return {@code true} if strategy and/or patch type has changed
		 */
		private boolean processEnvironmentalAsymmetryAt(int me, boolean changed) {
			if (!environmentalAsymmetry)
				return changed;
			int oldtype = strategies[me] % nTraits;
			if (changed) {
				int newtype = strategiesScratch[me] % nTraits;
				// only strategies can be adopted
				int newstrat = newtype % 2;
				changed = (oldtype % 2 != newstrat);
				// make sure patch type is preserved
				int oldpatch = oldtype / 2;
				strategiesScratch[me] = oldpatch + oldpatch + newstrat + (changed ? nTraits : 0);
			}
			// note: should we allow simultaneous strategy and patch changes? i don't think
			// so... which approach corresponds to the ODE?
			// check patch conversion
			double pFeedback = feedback[oldtype];
			if (pFeedback > 0.0) {
				if (pFeedback >= 1.0 || random01() < pFeedback) {
					// change type of node
					int oldstrat = oldtype < 2 ? ATBT.COOPERATE : ATBT.DEFECT;
					// determine new patch type (old one was GOOD if oldtype is even and will now
					// turn BAD and vice versa)
					int newpatch = (oldtype + 1) % 2;
					strategiesScratch[me] = newpatch + oldstrat + oldstrat + nTraits;
					return true;
				}
			}
			return changed;
		}
	}

	/**
	 * Provide ODE implementation for asymmetric {@code 2×2} games with
	 * environmental feedback.
	 */
	public class ODE extends ODERK {

		/**
		 * Convenience variable: module associated with this model (useful for single
		 * species).
		 */
		protected Module module;

		/**
		 * Constructs a new ODE solver taylored for the integration of asymmetric
		 * {@code 2×2} games with environmental feedback.
		 * 
		 * @param engine the pacemaker for running the model
		 */
		protected ODE(EvoLudo engine) {
			super(engine);
			module = engine.getModule();
		}

		/**
		 * {@inheritDoc}
		 * <p>
		 * Ecological feedback is unable to deal with time reversal.
		 */
		@Override
		public boolean permitsTimeReversal() {
			return false;
		}

		@Override
		protected void getDerivatives(double t, double[] state, double[] fitness, double[] change) {
			avgScores(state, 2, fitness);
			double xr = state[ATBT.COOPERATE_RICH];
			double xp = state[ATBT.COOPERATE_POOR];
			double yr = state[ATBT.DEFECT_RICH];
			double yp = state[ATBT.DEFECT_POOR];
			Map2Fitness map2fit = module.getMapToFitness();
			for (int n = 0; n < nTraits; n++)
				fitness[n] = map2fit.map(fitness[n]);
			double fC = xr * fitness[ATBT.COOPERATE_RICH] + xp * fitness[ATBT.COOPERATE_POOR];
			double fD = yr * fitness[ATBT.DEFECT_RICH] + yp * fitness[ATBT.DEFECT_POOR];
			double dyn;
			dyn = fC * yr - fD * xr + xp * feedback[ATBT.COOPERATE_POOR] - xr * feedback[ATBT.COOPERATE_RICH];
			double err = dyn;
			change[ATBT.COOPERATE_RICH] = dyn;
			dyn = fC * yp - fD * xp - xp * feedback[ATBT.COOPERATE_POOR] + xr * feedback[ATBT.COOPERATE_RICH];
			err += dyn;
			change[ATBT.COOPERATE_POOR] = dyn;
			dyn = -fC * yr + fD * xr + yp * feedback[ATBT.DEFECT_POOR] - yr * feedback[ATBT.DEFECT_RICH];
			err += dyn;
			change[ATBT.DEFECT_RICH] = dyn;
			dyn = -fC * yp + fD * xp - yp * feedback[ATBT.DEFECT_POOR] + yr * feedback[ATBT.DEFECT_RICH];
			err += dyn;
			change[ATBT.DEFECT_POOR] = dyn;
			// Mathematica: exploiting cooperators
			// xr'[t] == (xr[t] ft[[1]] + xp[t] ft[[2]]) yr[t] - (yr[t] ft[[3]] + yp[t]
			// ft[[4]]) xr[t] - lambda xr[t],
			// xp'[t] == (xr[t] ft[[1]] + xp[t] ft[[2]]) yp[t] - (yr[t] ft[[3]] + yp[t]
			// ft[[4]]) xp[t] + lambda xr[t],
			// yr'[t] == (yr[t] ft[[3]] + yp[t] ft[[4]]) xr[t] - (xr[t] ft[[1]] + xp[t]
			// ft[[2]]) yr[t] + lambda rho yp[t],
			// yp'[t] == (yr[t] ft[[3]] + yp[t] ft[[4]]) xp[t] - (xr[t] ft[[1]] + xp[t]
			// ft[[2]]) yp[t] - lambda rho yp[t]}

			// restrict to active strategies
			// note float resolution is 1.1920929E-7
			if (Math.abs(err) > 1e-7 * nActive) {
				if (active == null) {
					// assume all active
					for (int n = 0; n < nTraits; n++)
						change[n] -= err;
					return;
				}
				for (int n = 0; n < nTraits; n++) {
					if (active[n])
						change[n] -= err;
				}
			}
		}
	}
}
