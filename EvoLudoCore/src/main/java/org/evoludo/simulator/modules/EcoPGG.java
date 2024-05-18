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

import org.evoludo.geom.Point2D;
import org.evoludo.math.Combinatorics;
import org.evoludo.math.RNGDistribution;
import org.evoludo.simulator.EvoLudo;
import org.evoludo.simulator.models.IBS.HasIBS;
import org.evoludo.simulator.models.Model;
import org.evoludo.simulator.models.ODEEuler.HasODE;
import org.evoludo.simulator.models.PDERD.HasPDE;
import org.evoludo.simulator.models.PDERDA;
import org.evoludo.simulator.models.SDEEuler.HasSDE;
import org.evoludo.simulator.modules.Discrete.Groups;
import org.evoludo.simulator.views.BasicTooltipProvider;
import org.evoludo.simulator.views.HasConsole;
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
 * Cooperation in ecological public goods interactions.
 * 
 * @author Christoph Hauert
 */
public class EcoPGG extends Discrete implements Groups,
		HasIBS, HasODE, HasSDE, HasPDE, 
		HasPop2D.Strategy, HasPop3D.Strategy, HasMean.Strategy, HasS3, HasPhase2D,
		HasPop2D.Fitness, HasPop3D.Fitness, HasMean.Fitness, 
		HasHistogram.Fitness, HasHistogram.Degree, HasHistogram.StatisticsStationary, HasConsole {

	/**
	 * The trait (and index) value of defectors.
	 */
	public static final int DEFECT = 0;

	/**
	 * The trait (and index) value of cooperators.
	 */
	public static final int COOPERATE = 1;

	/**
	 * The shared random number generator to ensure reproducibility of results.
	 * 
	 * @see EvoLudo#getRNG()
	 */
	protected RNGDistribution rng;

	/**
	 * The multiplication factor of the public good.
	 */
	double interest = 3.0;

	/**
	 * The cost of cooperation or the individual contribution to the public good.
	 */
	double cost = 1.0;

	/**
	 * The payoff to lone cooperators.
	 */
	double payLoneCoop = 0.0;

	/**
	 * The payoff to lone defectors.
	 */
	double payLoneDefect = 0.0;

	/**
	 * Create a new instance of the module for ecological public goods games.
	 * 
	 * @param engine the manager of modules and pacemeaker for running the model
	 */
	public EcoPGG(EvoLudo engine) {
		super(engine);
		rng = engine.getRNG();
	}

	@Override
	public void load() {
		super.load();
		nTraits = 3;
		VACANT = 2;
		// trait names
		String[] names = new String[nTraits];
		names[COOPERATE] = "Cooperator";
		names[DEFECT] = "Defector";
		names[VACANT] = "Vacant";
		setTraitNames(names);
		// trait colors (automatically generates lighter versions for new strategists)
		Color[] colors = new Color[nTraits];
		colors[COOPERATE] = Color.BLUE;
		colors[DEFECT] = Color.RED;
		colors[VACANT] = Color.LIGHT_GRAY;
		setTraitColors(colors);
	}

	@Override
	public String getKey() {
		return "ePGG";
	}

	@Override
	public String getInfo() {
		return "Title: " + getTitle() + "\nAuthor: Christoph Hauert\n" + "Selection diffusion dynamics.";
	}

	@Override
	public String getTitle() {
		return "Ecological public goods games";
	}

	@Override
	public String getVersion() {
		return "v1.0 March 2021";
	}

	@Override
	public double getMinGameScore() {
		return Math.min(getMonoGameScore(COOPERATE),
				Math.min(getMonoGameScore(DEFECT), (interest / nGroup - 1.0) * cost));
	}

	@Override
	public double getMaxGameScore() {
		return Math.max(getMonoGameScore(COOPERATE),
				Math.max(getMonoGameScore(DEFECT), interest * (nGroup - 1) / nGroup * cost));
	}

	@Override
	public double getMonoGameScore(int type) {
		switch (type) {
			case COOPERATE:
				return (interest - 1.0) * cost;
			case DEFECT:
				return 0.0;
			default:
				// this includes VACANT
				return Double.NaN;
		}
	}

	@Override
	public Model.PDE createPDE() {
		return new PDERDA(engine);
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * Proper sampling in finite populations (c.f. CDL): 
	 * \begin{align*} 
	 * f_L =&amp; \sigma\\ 
	 * f_D =&amp; \frac{\binom z{N-1}}{\binom{M-1}{N-1}}\sigma+r\frac x{M-z-1}\\
	 * 	&amp; \left(1-\frac 1{N(M-z)} \left( M-(z-N+1)\frac{\binom
	 * z{N-1}}{\binom{M-1}{N-1}}\right)\right) c\\ 
	 * f_C =&amp; f_d-F(z)c\\ 
	 * F(z) =&amp; 1-\frac rN\frac{M-N}{M-z-1}+\frac{\binom z{N-1}}{\binom{M-1}{N-1}}\\
	 * 	&amp; \left(\frac rN \frac{z+1}{M-z-1}+r\frac{M-z-2}{M-z-1}-1\right)
	 * \end{align*}
	 */
	@Override
	public void mixedScores(int[] traitCount, int n, double[] traitScore) {
		int x = traitCount[COOPERATE];
		int y = traitCount[DEFECT];
		int z = traitCount[VACANT];
		int m = x + y + z;
		int m1 = m - 1;
		// if all or all but one are empty, payoffs are trivial
		if (z >= m1) {
			traitScore[COOPERATE] = payLoneCoop;
			traitScore[DEFECT] = payLoneDefect;
			traitScore[VACANT] = Double.NaN;
			return;
		}
		// payoffs from public goods interactions
		double zn1 = 0.0;
		if (z >= n - 1) {
			// zn1 = Binomial[z, n-1]/Binomial[m-1, n-1]
			zn1 = (double) z / (double) m1;
			for (int i = 1; i < n - 1; i++)
				zn1 *= (double) (z - i) / (double) (m1 - i);
		}
		double imz1 = 1.0 / (m - z - 1);
		double b = interest * x * imz1 * (1.0 - (m - zn1 * (z - n + 1)) / (n * (m - z)));
		double fz = 1.0 - interest * (m - n) * imz1 / n
				+ zn1 * interest * imz1 * (m1 - (double) (n - 1) * (double) (z + 1) / n) - zn1;

		traitScore[VACANT] = Double.NaN;
		traitScore[DEFECT] = zn1 * payLoneDefect + b * cost;
		traitScore[COOPERATE] = zn1 * payLoneCoop + (b - fz) * cost;
	}

	@Override
	public void avgScores(double[] dens, int n, double[] avgscores) {
		double x = dens[COOPERATE];
		double y = dens[DEFECT];
		double z = dens[VACANT];
		double zn1 = Combinatorics.pow(z, n - 1); // pow is really evil... try to make good pow...
		double zn = zn1 * z;
		double fn = (z > 1.0 - 1e-8) ? 2.0 : (1.0 - (1.0 - zn) / (n * (1.0 - z))) / (1.0 - z);
		avgscores[COOPERATE] = payLoneCoop * zn1 + ((interest - 1.0) * (1.0 - zn1) - interest * y * fn) * cost;
		avgscores[DEFECT] = payLoneDefect * zn1 + interest * x * fn * cost;
		avgscores[VACANT] = Double.NaN;
	}

	@Override
	public double pairScores(int me, int[] traitCount, double[] traitScore) {
		traitScore[VACANT] = Double.NaN;
		if (me == VACANT) {
			traitScore[COOPERATE] = payLoneCoop;
			traitScore[DEFECT] = payLoneDefect;
			return Double.NaN;
		}
		switch (me) {
			case COOPERATE:
				traitScore[COOPERATE] = (interest - 1.0) * cost;
				traitScore[DEFECT] = (interest / 2.0) * cost;
				return traitCount[COOPERATE] * (interest - 1.0) * cost + traitCount[DEFECT] * (interest / 2.0 - 1.0) * cost
						+ traitCount[VACANT] * payLoneCoop;

			case DEFECT:
				traitScore[COOPERATE] = (interest / 2.0 - 1.0) * cost;
				traitScore[DEFECT] = 0.0;
				return traitCount[COOPERATE] * interest / 2.0 * cost +
				// count[DEFECT]*0.0+
						traitCount[VACANT] * payLoneDefect;

			default: // should not end here
				throw new Error("Unknown strategy (" + me + ")");
		}
	}

	@Override
	public void groupScores(int[] traitCount, double[] traitScore) {
		int x = traitCount[COOPERATE];
		int n = x + traitCount[DEFECT];

		traitScore[VACANT] = Double.NaN;
		if (n < 2) {
			traitScore[COOPERATE] = payLoneCoop;
			traitScore[DEFECT] = payLoneDefect;
			return;
		}
		traitScore[DEFECT] = x * interest * cost / n;
		traitScore[COOPERATE] = traitScore[DEFECT] - cost;
	}

	/**
	 * Set the cost of cooperation.
	 * 
	 * @param aValue the cost of cooperation
	 */
	public void setCostCoop(double aValue) {
		cost = aValue;
	}

	/**
	 * Get the cost of cooperation.
	 * 
	 * @return the cost of cooperation
	 */
	public double getCostCoop() {
		return cost;
	}

	/**
	 * Set the multiplication factor of the linear public goods game.
	 * 
	 * @param aValue the new multiplication factor
	 */
	public void setInterest(double aValue) {
		interest = aValue;
	}

	/**
	 * Get the multiplication factor of the linear public goods game.
	 *
	 * @return the multiplication factor
	 */
	public double getInterest() {
		return interest;
	}

	/**
	 * Set the payoff to lone cooperators. Defaults to loner payoff.
	 * 
	 * @param aValue the payoff to lone cooperators.
	 */
	public void setPayLoneCoop(double aValue) {
		payLoneCoop = aValue;
	}

	/**
	 * Get the payoff to lone cooperators.
	 * 
	 * @return the payoff to lone cooperators.
	 */
	public double getPayLoneCoop() {
		return payLoneCoop;
	}

	/**
	 * Set the payoff to lone defectors. Defaults to loner payoff.
	 * 
	 * @param aValue the payoff to lone defectors.
	 */
	public void setPayLoneDefect(double aValue) {
		payLoneDefect = aValue;
	}

	/**
	 * Get the payoff to lone defectors.
	 * 
	 * @return the payoff to lone defectors.
	 */
	public double getPayLoneDefect() {
		return payLoneDefect;
	}

	/**
	 * The map for translating the model data into 2D phase plane representation.
	 */
	EcoPGGMap map;

	@Override
	public Data2Phase getPhase2DMap() {
		map = new EcoPGGMap();
		return map;
	}

	/**
	 * The map for translating the data of the ecological public goods game models into 2D phase plane representation.
	 */
	public class EcoPGGMap implements Data2Phase, BasicTooltipProvider {

		@Override
		public boolean data2Phase(double[] data, Point2D point) {
			// NOTE: data[0] is time!
			point.x = 1.0 - data[VACANT + 1];
			point.y = Math.min(1.0, 1.0 - data[DEFECT + 1] / (1.0 - data[VACANT + 1]));
			return true;
		}

		@Override
		public boolean phase2Data(Point2D point, double[] data) {
			data[VACANT] = 1.0 - point.x;
			data[DEFECT] = point.x * (1.0 - point.y);
			data[COOPERATE] = point.x * point.y;
			return true;
		}

		@Override
		public String getXAxisLabel() {
			return "population density";
		}

		@Override
		public String getYAxisLabel() {
			return "relative fraction of cooperators";
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
	 * Command line option to set the multiplication factor for public good
	 * interactions.
	 */
	public final CLOption cloInterest = new CLOption("interest", "3", EvoLudo.catModule,
			"--interest <r>  multiplication factor of public good", new CLODelegate() {

				/**
				 * {@inheritDoc}
				 * <p>
				 * Parse the multiplication factor for the linear public goods games.
				 * 
				 * @param arg the multiplication factor(s)
				 */
				@Override
				public boolean parse(String arg) {
					setInterest(CLOParser.parseDouble(arg));
					return true;
				}

				@Override
				public void report(PrintStream output) {
					output.println("# interest:             " + Formatter.format(getInterest(), 4));
				}
			});

	/**
	 * Command line option to set the cost of cooperation, i.e. contributions to the
	 * public good.
	 */
	public final CLOption cloCost = new CLOption("cost", "1", EvoLudo.catModule,
			"--cost <c>      cost of cooperation", new CLODelegate() {

				/**
				 * {@inheritDoc}
				 * <p>
				 * Parse the cost of cooperation.
				 * 
				 * @param arg the cost of cooperation
				 */
				@Override
				public boolean parse(String arg) {
					setCostCoop(CLOParser.parseDouble(arg));
					return true;
				}

				@Override
				public void report(PrintStream output) {
					output.println("# cost:                 " + Formatter.format(getCostCoop(), 4));
				}
			});

	/**
	 * Command line option to set the payoff to cooperators that failed to find any
	 * interaction partners.
	 */
	public final CLOption cloLoneCooperator = new CLOption("lonecooperator", "0", EvoLudo.catModule,
			"--lonecooperator <l>  payoff of lone cooperator", new CLODelegate() {

				/**
				 * {@inheritDoc}
				 * <p>
				 * Parse the payoff to lone cooperators.
				 * 
				 * @param arg the payoff to lone cooperators
				 */
				@Override
				public boolean parse(String arg) {
					setPayLoneCoop(CLOParser.parseDouble(arg));
					return true;
				}

				@Override
				public void report(PrintStream output) {
					output.println("# lonecoop:             " + Formatter.format(getPayLoneCoop(), 4));
				}
			});
	/**
	 * Command line option to set the payoff to defectors that failed to find any
	 * interaction partners.
	 */
	public final CLOption cloLoneDefector = new CLOption("lonedefector", "0", EvoLudo.catModule,
			"--lonedefector <l>  payoff of lone defector", new CLODelegate() {

				/**
				 * {@inheritDoc}
				 * <p>
				 * Parse the payoff to lone defectors.
				 * 
				 * @param arg the payoff to lone defectors
				 */
				@Override
				public boolean parse(String arg) {
					setPayLoneDefect(CLOParser.parseDouble(arg));
					return true;
				}

				@Override
				public void report(PrintStream output) {
					output.println("# lonedefect:           " + Formatter.format(getPayLoneDefect(), 4));
				}
			});

	@Override
	public void collectCLO(CLOParser parser) {
		// prepare command line options
		super.collectCLO(parser);
		parser.addCLO(cloInterest);
		parser.addCLO(cloCost);
		parser.addCLO(cloLoneCooperator);
		parser.addCLO(cloLoneDefector);
	}
}
