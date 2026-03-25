//
// EvoLudo Project
//
// Copyright 2010-2026 Christoph Hauert
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

import org.evoludo.geom.Point2D;
import org.evoludo.simulator.EvoLudo;
import org.evoludo.simulator.models.Model.HasDE;
import org.evoludo.simulator.models.Model.HasIBS;
import org.evoludo.simulator.modules.Features.Payoffs;
import org.evoludo.simulator.views.BasicTooltipProvider;
import org.evoludo.simulator.views.HasHistogram;
import org.evoludo.simulator.views.HasMean;
import org.evoludo.simulator.views.HasPhase2D;
import org.evoludo.simulator.views.HasPop2D;
import org.evoludo.simulator.views.HasPop3D;
import org.evoludo.util.CLOCategory;
import org.evoludo.util.CLODelegate;
import org.evoludo.util.CLOParser;
import org.evoludo.util.CLOption;
import org.evoludo.util.Formatter;

/**
 * The module to investigate {@code 2×2} games in interactions
 * <em>between</em> species, while competition remains <em>within</em> species.
 * For example, with two well-mixed populations of size {@code N} and {@code M}
 * all individuals of the first species interact with all {@code M} members of
 * the second species. Converely, the members of the second species interact
 * with {@code N} members of the first species.
 * <p>
 * Structured populations are represented by two separate but aligned geometries
 * such that interactions happen between geometries while competition happens
 * within each geometry. For example, lattice structured populations are
 * represented by two layers, one for each species with interactions between
 * them and competition within. Thus, on a regular lattice with {@code k}
 * neighbours an individual with index {@code i} interacts with {@code k+1}
 * individuals, namely it's {@code k} neighbours plus the one at the same
 * location {@code i} on the other layer.
 * 
 * <h3>Notes:</h3>
 * <ol>
 * <li>The population sizes of each species needs to be the same. Except for
 * well-mixed populations.
 * <li>The competition and interaction structures can be different within
 * and/or between species.
 * </ol>
 * 
 * @author Christoph Hauert
 * 
 * @see org.evoludo.simulator.modules.TBT Module for classical {@code 2×2}
 *      games
 */
public class Mutualism extends Discrete implements Payoffs,
		HasIBS.DPairs, HasDE.DPairs, HasDE.EM, HasDE.RK5, // HasSDE, single species only at present // HasPDE, not (yet)
															// an option
		HasPop2D.Traits, HasPop3D.Traits, HasMean.Traits, HasPhase2D, HasPop2D.Fitness, HasPop3D.Fitness,
		HasMean.Fitness, HasHistogram.Fitness, HasHistogram.Degree, HasHistogram.StatisticsStationary {

	/**
	 * The trait (and index) value of cooperators.
	 */
	public static final int COOPERATE = 0;

	/**
	 * The trait (and index) value of defectors.
	 */
	public static final int DEFECT = 1;

	/**
	 * The payoff for mutual cooperation.
	 */
	double alpha;

	/**
	 * The payoff to a cooperator when facing a defector.
	 */
	double beta;

	/**
	 * The payoff to a defector when facing a cooperator.
	 */
	double gamma;

	/**
	 * The payoff for mutual defection.
	 */
	double delta;

	/**
	 * The reference to the partner species with {@code partner.partner == this}.
	 */
	Mutualism partner;

	/**
	 * Create a new instance of the module for inter-species {@code 2×2}
	 * games.
	 * 
	 * <h3>Important:</h3>
	 * This instantiates only the skeleton and only of one species. The other
	 * species is created only when {@code load}ing this module.
	 * 
	 * @param engine the manager of modules and pacemaker for running the model
	 */
	public Mutualism(EvoLudo engine) {
		super(engine);
	}

	/**
	 * Create a new instance of the second, partner species for inter-species
	 * {@code 2×2} games.
	 * 
	 * @param partner the reference to the partner species
	 */
	public Mutualism(Mutualism partner) {
		super(partner);
	}

	/**
	 * {@inheritDoc}
	 * 
	 * <h3>Notes:</h3>
	 * <ul>
	 * <li>Because the {@code Host} and {@code Mutualist} species are identical
	 * (potentially apart from interaction parameters) almost everything can be
	 * allocated and set in the {@code load()} method.
	 * <li>Otherwise, species specific settings can be moved to the constructor. In
	 * case the species differ more and e.g. use different variables additional
	 * species can be defined as inner classes that extend the outer class (here
	 * {@code Mutualism}), see {@code EcoMutualism} for an example.
	 * </ul>
	 */
	// TODO(public-docs): restore a {@code @link} to
	// org.evoludo.simulator.modules.EcoMutualism once the module becomes public.
	@Override
	public void load() {
		super.load();
		setNTraits(2);
		// trait names
		String[] names = new String[nTraits];
		names[DEFECT] = "Defector";
		names[COOPERATE] = "Cooperator";
		setTraitNames(names);
		// trait colors (automatically generates lighter versions for new traits)
		Color[] colors = new Color[2 * nTraits];
		colors[DEFECT] = Color.RED;
		colors[COOPERATE] = Color.BLUE;
		colors[DEFECT + nTraits] = Color.YELLOW;
		colors[COOPERATE + nTraits] = Color.GREEN;
		setTraitColors(colors);
		if (partner == null) {
			setName("Host");
			// load the mutualist population
			partner = new Mutualism(this);
			partner.partner = this;
			partner.load();
			partner.setName("Mutualist");
		}
	}

	/**
	 * {@inheritDoc}
	 * 
	 * <h3>Important:</h3>
	 * This method is only called for the first created instance (see
	 * {@link #Mutualism(EvoLudo)}) but not it's partner (see
	 * {@link #Mutualism(Mutualism)}). In order to properly unload both species
	 * {@code partner.unload()} needs to be called. However, to avoid infinite
	 * loops, use, e.g.
	 * 
	 * <pre>
	 * if (partner != null) {
	 * 	partner.partner = null;
	 * 	partner.unload();
	 * 	partner = null;
	 * }
	 * 
	 * </pre>
	 * 
	 * @see #Mutualism(EvoLudo)
	 * @see #Mutualism(Mutualism)
	 */
	@Override
	public void unload() {
		super.unload();
		if (partner != null) {
			partner.partner = null;
			partner.unload();
			partner = null;
		}
	}

	@Override
	public String getKey() {
		return "Mutual";
	}

	@Override
	public String getAuthors() {
		return "Christoph Hauert & György Szabo";
	}

	@Override
	public String getTitle() {
		return "Mutualisms";
	}

	@Override
	public int getDependent() {
		return DEFECT;
	}

	@Override
	public double getMinPayoff() {
		return Math.min(alpha, Math.min(beta, Math.min(gamma, delta)));
	}

	@Override
	public double getMaxPayoff() {
		return Math.max(alpha, Math.max(beta, Math.max(gamma, delta)));
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * <strong>Note:</strong> mono score is ill defined as the homogeneous states of
	 * the population and its opponent is required!
	 * </p>
	 */
	@Override
	public double getMonoPayoff(int mono) {
		return Double.NaN;
	}

	@Override
	public void avgScores(double[] density, double[] avgscores) {
		// host species has ID = 0, mutualist species has ID = 1
		int me = (id == 0 ? 0 : opponent.nTraits);
		int opp = (me > 0 ? 0 : nTraits);
		avgscores[me + COOPERATE] = density[opp + COOPERATE] * alpha + density[opp + DEFECT] * beta;
		avgscores[me + DEFECT] = density[opp + COOPERATE] * gamma + density[opp + DEFECT] * delta;
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * <strong>Note:</strong> <code>count</code> refers to <em>opponent</em> (here a
	 * different species)
	 */
	@Override
	public void mixedScores(int[] traitCount, double[] traitScore) {
		// avoid self interactions in intra-species interactions
		int selfi = (opponent != this ? 0 : 1);
		int x = traitCount[COOPERATE];
		int y = traitCount[DEFECT];
		double in = 1.0 / (x + y - selfi);
		traitScore[COOPERATE] = ((x - selfi) * alpha + y * beta) * in;
		traitScore[DEFECT] = (x * gamma + (y - selfi) * delta) * in;
	}

	@Override
	public double pairScores(int me, int[] traitCount, double[] traitScore) {
		switch (me) {
			case COOPERATE:
				traitScore[COOPERATE] = partner.alpha;
				traitScore[DEFECT] = partner.gamma;
				return traitCount[COOPERATE] * alpha + traitCount[DEFECT] * beta;

			case DEFECT:
				traitScore[COOPERATE] = partner.beta;
				traitScore[DEFECT] = partner.delta;
				return traitCount[COOPERATE] * gamma + traitCount[DEFECT] * delta;

			default: // should not end here
				throw new UnsupportedOperationException("Unknown trait (" + me + ")");
		}
	}

	/**
	 * Set the payoffs from the {@code 2×2} matrix {@code payoffs} for the
	 * species represented by this module (identified by {@link Module#id}).
	 * 
	 * @param payoffs the payoff matrix
	 */
	public void setPayoffs(double[][] payoffs) {
		alpha = payoffs[COOPERATE][COOPERATE];
		beta = payoffs[COOPERATE][DEFECT];
		gamma = payoffs[DEFECT][COOPERATE];
		delta = payoffs[DEFECT][DEFECT];
	}

	/**
	 * Get the payoffs in the form of a {@code 2×2} matrix for the species
	 * represented by this module (identified by {@link Module#id}).
	 * 
	 * @return the payoff matrix
	 */
	public double[][] getPayoffs() {
		final double[][] payoffs = new double[2][2];
		payoffs[COOPERATE][COOPERATE] = alpha;
		payoffs[COOPERATE][DEFECT] = beta;
		payoffs[DEFECT][COOPERATE] = gamma;
		payoffs[DEFECT][DEFECT] = delta;
		return payoffs;
	}

	/**
	 * Set the payoff when trait {@code me} interacts with {@code you} (in the other
	 * species).
	 * 
	 * @param payoff the payoff to {@code me}
	 * @param me     the trait index of the row player
	 * @param you    the trait index of the column player (refers to other species)
	 */
	public void setPayoff(double payoff, int me, int you) {
		if (me == COOPERATE) {
			if (you == COOPERATE)
				alpha = payoff;
			else
				beta = payoff;
			return;
		}
		if (you == DEFECT)
			delta = payoff;
		else
			gamma = payoff;
	}

	/**
	 * Get the payoff when trait {@code me} interacts with {@code you} (in the other
	 * species).
	 * 
	 * @param me  the trait index of the row player
	 * @param you the trait index of the column player (refers to other species)
	 * @return the payoff to {@code me}
	 */
	public double getPayoff(int me, int you) {
		if (me == COOPERATE) {
			if (you == COOPERATE)
				return alpha;
			return beta;
		}
		if (you == DEFECT)
			return delta;
		return gamma;
	}

	/**
	 * The map for translating the model data into 2D phase plane representation.
	 */
	MutualismMap map;

	@Override
	public Data2Phase getPhase2DMap() {
		if (map == null)
			map = new MutualismMap();
		return map;
	}

	/**
	 * The map for translating the data of the ecological public goods game models
	 * into 2D phase plane representation.
	 */
	public class MutualismMap implements Data2Phase, BasicTooltipProvider {

		/**
		 * Create the 2D projection helper using the surrounding module settings.
		 */
		public MutualismMap() {
			// Intentionally empty: no additional initialization needed.
		}

		@Override
		public boolean data2Phase(double[] data, Point2D point) {
			// NOTE: data[0] is time!
			point.set(data[COOPERATE + 1],
					data[nTraits + COOPERATE + 1]);
			return true;
		}

		@Override
		public boolean phase2Data(Point2D point, double[] data) {
			double x = point.getX();
			double y = point.getY();
			data[COOPERATE] = x;
			data[DEFECT] = 1.0 - x;
			data[nTraits + COOPERATE] = y;
			data[nTraits + DEFECT] = 1.0 - y;
			return true;
		}

		@Override
		public String getXAxisLabel() {
			return getName() + ": " + getTraitName(COOPERATE);
		}

		@Override
		public String getYAxisLabel() {
			return partner.getName() + ": " + partner.getTraitName(COOPERATE);
		}

		@Override
		public String getTooltipAt(double x, double y) {
			StringBuilder tip = new StringBuilder(TABLE_STYLE)
					.append(TABLE_ROW_START_RIGHT)
					.append(getName()).append(": ").append(getTraitName(COOPERATE))
					.append(TABLE_CELL_NEXT).append(Formatter.formatPercent(x, 2))
					.append(TABLE_ROW_END);
			tip.append(TABLE_STYLE).append(TABLE_ROW_START_RIGHT)
					.append(getName()).append(": ").append(getTraitName(DEFECT))
					.append(TABLE_CELL_NEXT).append(Formatter.formatPercent(1.0 - x, 2))
					.append(TABLE_ROW_END);
			tip.append(TABLE_STYLE).append(TABLE_ROW_START_RIGHT)
					.append(partner.getName()).append(": ").append(partner.getTraitName(COOPERATE))
					.append(TABLE_CELL_NEXT).append(Formatter.formatPercent(y, 2))
					.append(TABLE_ROW_END);
			tip.append(TABLE_STYLE).append(TABLE_ROW_START_RIGHT)
					.append(partner.getName()).append(": ").append(partner.getTraitName(DEFECT))
					.append(TABLE_CELL_NEXT).append(Formatter.formatPercent(1.0 - y, 2))
					.append(TABLE_ROW_END);
			tip.append(TABLE_END);
			return tip.toString();
		}
	}

	/**
	 * Command line option to set the payoffs for the host species (when interacting
	 * with the mutualist species).
	 */
	public final CLOption cloPayHost = new CLOption("payhost", "1,0;1.65,0", CLOCategory.Module,
			"--payhost <a,b;c,d>       payoff matrix for host species", new CLODelegate() {

				/**
				 * {@inheritDoc}
				 * 
				 * Parse the payoff matrix of the host species interacting with the mutualist
				 * species through {@code 2×2} games. The argument must have the form
				 * 'P,T;S,R' referring to the punishment, temptation, sucker's payoff and
				 * reward, respectively. Note '{@value CLOParser#VECTOR_DELIMITER}' separates
				 * entries in 1D arrays and '{@value CLOParser#MATRIX_DELIMITER}' separates rows
				 * in 2D arrays.
				 * 
				 * @param arg the payoff matrix
				 */
				@Override
				public boolean parse(String arg) {
					double[][] payMatrix = CLOParser.parseMatrix(arg);
					if (payMatrix.length != 2 || payMatrix[0].length != 2)
						return false;
					setPayoffs(payMatrix);
					return true;
				}
			});

	/**
	 * Command line option to set the payoffs for the mutualist species (when
	 * interacting with the host species).
	 */
	public final CLOption cloPayMutualist = new CLOption("paymutualist", "1,0;1.65,0", CLOCategory.Module,
			"--paymutualist <a,b;c,d>  payoff matrix for mutualist species", new CLODelegate() {

				/**
				 * {@inheritDoc}
				 * <p>
				 * Parse the payoff matrix of the mutualist species interacting with the host
				 * species through {@code 2×2} games. The argument must have the form
				 * 'P,T;S,R' referring to the punishment, temptation, sucker's payoff and
				 * reward, respectively. Note '{@value CLOParser#VECTOR_DELIMITER}' separates
				 * entries in 1D arrays and '{@value CLOParser#MATRIX_DELIMITER}' separates rows
				 * in 2D arrays.
				 * 
				 * @param arg the payoff matrix
				 */
				@Override
				public boolean parse(String arg) {
					double[][] payMatrix = CLOParser.parseMatrix(arg);
					if (payMatrix.length != 2 || payMatrix[0].length != 2)
						return false;
					setPayoffs(payMatrix);
					return true;
				}
			});

	@Override
	public void collectCLO(CLOParser parser) {
		// collect command line options only once
		if (getName().equals("Mutualist")) {
			parser.addCLO(cloPayMutualist);
			return;
		}
		parser.addCLO(cloPayHost);
		super.collectCLO(parser);
	}
}
