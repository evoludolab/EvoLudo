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
import java.util.Arrays;

import org.evoludo.math.ArrayMath;
import org.evoludo.simulator.ColorMap;
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
import org.evoludo.util.CLODelegate;
import org.evoludo.util.CLOParser;
import org.evoludo.util.CLOption;
import org.evoludo.util.CLOCategory;
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
 * now the best trait depends on the opponent. Against a cooperator it is
 * still better to defect but against a defector it pays to cooperate. The
 * snowdrift game is still a social dilemma because individuals are tempted to
 * move away from mutual cooperation but the dilemma is relaxed because
 * cooperation is not necessarily doomed in the absence of supporting
 * mechanisms.
 *
 * @author Christoph Hauert
 */
public class TBT extends Discrete implements Payoffs,
		HasIBS.DPairs, HasDE.DPairs, HasDE.EM, HasDE.RK5, HasDE.SDE, HasDE.PDERD, HasDE.PDEADV,
		HasPop2D.Traits, HasPop3D.Traits, HasMean.Traits,
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
		nTraits = 2; // cooperators and defectors
	}

	@Override
	public void load() {
		super.load();
		// trait names (optional)
		setTraitNames(new String[] { "Cooperator", "Defector" });
		// trait colors (optional)
		setTraitColors(new Color[] { Color.BLUE, Color.RED, Color.GREEN, Color.YELLOW });
		// payoffs (local storage)
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
	public String getAuthors() {
		return "Christoph Hauert";
	}

	@Override
	public String getTitle() {
		return "2x2 Games";
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
	public double getMinPayoff() {
		return ArrayMath.min(payoffs);
	}

	@Override
	public double getMaxPayoff() {
		return ArrayMath.max(payoffs);
	}

	@Override
	public double getMonoPayoff(int type) {
		return payoffs[type][type];
	}

	@Override
	public double pairScores(int me, int[] traitCount, double[] traitScore) {
		switch (me) {
			case COOPERATE:
				traitScore[COOPERATE] = payoffs[COOPERATE][COOPERATE];
				traitScore[DEFECT] = payoffs[DEFECT][COOPERATE];
				return traitCount[COOPERATE] * payoffs[COOPERATE][COOPERATE]
						+ traitCount[DEFECT] * payoffs[COOPERATE][DEFECT];

			case DEFECT:
				traitScore[COOPERATE] = payoffs[COOPERATE][DEFECT];
				traitScore[DEFECT] = payoffs[DEFECT][DEFECT];
				return traitCount[COOPERATE] * payoffs[DEFECT][COOPERATE]
						+ traitCount[DEFECT] * payoffs[DEFECT][DEFECT];

			default: // should not end here
				throw new UnsupportedOperationException("Unknown trait (" + me + ")");
		}
	}

	@Override
	public void avgScores(double[] density, double[] avgscores) {
		avgscores[COOPERATE] = density[COOPERATE] * payoffs[COOPERATE][COOPERATE]
				+ density[DEFECT] * payoffs[COOPERATE][DEFECT];
		avgscores[DEFECT] = density[COOPERATE] * payoffs[DEFECT][COOPERATE]
				+ density[DEFECT] * payoffs[DEFECT][DEFECT];
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
	 * @param me     the trait index of the row player
	 * @param you    the trait index of the column player
	 */
	public void setPayoff(double payoff, int me, int you) {
		if (me < 0 || me > 2 || you < 0 || you > 2)
			return;
		payoffs[me][you] = payoff;
	}

	/**
	 * Get the payoff for type {@code me} against type {@code you}.
	 *
	 * @param me  the trait index of the row player
	 * @param you the trait index of the column player
	 * @return the payoff to {@code me}
	 */
	public double getPayoff(int me, int you) {
		return payoffs[me][you];
	}

	/**
	 * Command line option to set the {@code 2×2} payoff matrix for
	 * interactions between cooperators and defectors.
	 */
	public final CLOption cloPayoffs = new CLOption("paymatrix", "3,0;5,1", CLOCategory.Module,
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
					if (payMatrix.length != 2 || payMatrix[0].length != 2)
						return false;
					setPayoffs(payMatrix);
					return true;
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
	public IBSDPopulation createIBSPopulation() {
		return new TBT.IBSPop(engine, this);
	}

	/**
	 * The extension for IBS simulations specific to populations engaging in
	 * {@code 2×2} games. This extension implements customizations for
	 * {@code Geometry.Type.SQUARE_NEUMANN_2ND} as well as specific initial
	 * conditions that give rise to fascinating evolutionary kaleidoscopes for
	 * deterministic updating.
	 */
	public class IBSPop extends IBSDPopulation {

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
		 * @param module the module that defines the game
		 */
		protected IBSPop(EvoLudo engine, TBT module) {
			super(engine, module);
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
			int trait = getTraitAt(idx);
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
		public double[] getMeanTraits(double[] mean) {
			// SQUARE_NEUMANN_2ND geometry for competition results in two disjoint
			// sublattices; report trait frequencies in each sublattice separately
			if (competition.getType() != Geometry.Type.SQUARE_NEUMANN_2ND) {
				return super.getMeanTraits(mean);
			}

			double newtime = model.getUpdates();
			if (Math.abs(tsMean - newtime) < model.getTimeStep()) {
				System.arraycopy(tsTraits, 0, mean, 0, mean.length);
				return mean;
			}
			int n = 0;
			Arrays.fill(mean, 0);
			int side = (int) Math.sqrt(nPopulation);
			while (n < nPopulation) {
				if ((n / side) % 2 == 0) {
					mean[getTraitAt(n++)]++;
					mean[nTraits + getTraitAt(n++)]++;
					continue;
				}
				mean[nTraits + getTraitAt(n++)]++;
				mean[getTraitAt(n++)]++;
			}
			ArrayMath.multiply(mean, 2.0 / nPopulation);
			System.arraycopy(mean, 0, tsTraits, 0, mean.length);
			tsMean = newtime;
			return mean;
		}

		@Override
		public double[] getMeanFitness(double[] mean) {
			// SQUARE_NEUMANN_2ND geometry for competition results in two disjoint
			// sublattices; report trait frequencies in each sublattice separately
			if (competition.getType() != Geometry.Type.SQUARE_NEUMANN_2ND) {
				return super.getMeanFitness(mean);
			}

			double newtime = model.getUpdates();
			if (Math.abs(tsFit - newtime) < model.getTimeStep()) {
				System.arraycopy(tsFits, 0, mean, 0, mean.length);
				return mean;
			}
			int n = 0;
			Arrays.fill(mean, 0);
			int side = (int) Math.sqrt(nPopulation);
			while (n < nPopulation) {
				if ((n / side) % 2 == 0) {
					mean[getTraitAt(n)] += getFitnessAt(n++);
					mean[nTraits + getTraitAt(n)] += getFitnessAt(n++);
					continue;
				}
				mean[nTraits + getTraitAt(n)] += getFitnessAt(n++);
				mean[getTraitAt(n)] += getFitnessAt(n++);
			}
			// total payoff in last entry
			mean[2 * nTraits] = sumFitness * 0.25;
			// averages for each sublattice
			ArrayMath.multiply(mean, 2.0 / nPopulation);
			System.arraycopy(mean, 0, tsFits, 0, mean.length);
			tsFit = newtime;
			return mean;
		}

		@Override
		public String getStatus() {
			if (competition.getType() != Geometry.Type.SQUARE_NEUMANN_2ND)
				return super.getStatus();

			getMeanTraits(tsTraits);
			StringBuilder status = new StringBuilder();
			for (int i = 0; i < 2 * nTraits; i++) {
				if (status.length() > 0) {
					status.append(", ");
				}
				status.append(module.getTraitName(i)).append(": ")
						.append(Formatter.formatPercent(tsTraits[i], 1));
			}
			return status.toString();
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
			Arrays.fill(traitsCount, 0);
			initMono(TBT.COOPERATE);
			switch (interaction.getType()) {
				case CUBE:
					initCubeKaleidoscope();
					break;

				case SQUARE_NEUMANN:
				case SQUARE_NEUMANN_2ND:
				case SQUARE_MOORE:
				case SQUARE:
				case HEXAGONAL:
				case TRIANGULAR:
					int l = (int) Math.sqrt(nPopulation);
					int m = l / 2;
					mid = m * (l + 1);
					// $FALL-THROUGH$
				case LINEAR:
					if (mid < 0)
						mid = nPopulation / 2;
					setTraitAt(mid, TBT.DEFECT);
					traitsCount[TBT.COOPERATE]--;
					traitsCount[TBT.DEFECT] = 1;
					break;

				default:
					// should never get here - check made sure of it.
					throw new UnsupportedOperationException("geometry incompatible with kaleidoscopes!");
			}
		}

		/**
		 * Initialize kaleidoscope on cubic lattice.
		 */
		private void initCubeKaleidoscope() {
			int l;
			int lz;
			if (nPopulation == 25000) {
				l = 50;
				lz = 10;
			} else {
				l = (int) (Math.pow(nPopulation, 1.0 / 3.0) + 0.5);
				lz = l;
			}
			double[] args = init.getArgs();
			int type = ((args != null && args.length > 0) ? (int) args[0] : 0);
			switch (type) {
				default:
				case 0:
					initCubeKaleidoscopeDinC(l, lz);
					return;
				case 1:
					initCubeKaleidoscopeCinD(l, lz);
					return;
			}
		}

		/**
		 * Initialize kaleidoscope on cubic lattice with defectors in center. If the
		 * population size is {@code 25'000} then the NOVA settings are used (a cube
		 * with dimensions {@code 50×50×10}).
		 * 
		 * @param l  the linear dimension of the cube
		 * @param lz the linear dimension of the z-dimension
		 */
		private void initCubeKaleidoscopeDinC(int l, int lz) {
			int m = l / 2;
			int mz = lz / 2;
			if (l % 2 == 1) {
				// odd dimensions (this excludes NOVA, hence mz=m)
				// place single TBT.DEFECT in center
				int mid = m * ((l + 1) * l + 1);
				traitsCount[TBT.COOPERATE]--;
				setTraitAt(mid, TBT.DEFECT);
				traitsCount[TBT.DEFECT]++;
				return;
			}
			// even dimensions - place 2x2x2 cube of TBT.DEFECTors in center
			for (int z = mz - 1; z <= mz; z++)
				for (int y = m - 1; y <= m; y++)
					for (int x = m - 1; x <= m; x++)
						setTraitAt((z * l + y) * l + x, TBT.DEFECT);
			traitsCount[TBT.COOPERATE] -= 2 * 2 * 2;
			traitsCount[TBT.DEFECT] += 2 * 2 * 2;
		}

		/**
		 * Initialize kaleidoscope on cubic lattice with cooperators in center. If the
		 * population size is {@code 25'000} then the NOVA settings are used (a cube
		 * with dimensions {@code 50×50×10}).
		 * 
		 * @param l  the linear dimension of the cube
		 * @param lz the linear dimension of the cube in the z-dimension
		 */
		@SuppressWarnings("java:S3776") // nested for-loops clearer than refactoring
		private void initCubeKaleidoscopeCinD(int l, int lz) {
			int m = l / 2;
			int mz = lz / 2;
			if (l % 2 == 1) {
				// odd dimensions - place 3x3x3 cube of cooperators in center
				for (int z = mz - 1; z <= mz + 1; z++)
					for (int y = m - 1; y <= m + 1; y++)
						for (int x = m - 1; x <= m + 1; x++)
							setTraitAt((z * l + y) * l + x, TBT.COOPERATE);
				traitsCount[TBT.DEFECT] -= 3 * 3 * 3;
				traitsCount[TBT.COOPERATE] += 3 * 3 * 3;
				return;
			}
			// even dimensions - place 4x4x4 cube of cooperators in center
			for (int z = mz - 2; z <= mz + 1; z++)
				for (int y = m - 2; y <= m + 1; y++)
					for (int x = m - 2; x <= m + 1; x++)
						setTraitAt((z * l + y) * l + x, TBT.COOPERATE);
			traitsCount[TBT.DEFECT] -= 4 * 4 * 4;
			traitsCount[TBT.COOPERATE] += 4 * 4 * 4;
		}
	}
}
