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
import java.io.PrintStream;

import org.evoludo.math.Combinatorics;
import org.evoludo.simulator.EvoLudo;
import org.evoludo.simulator.models.IBS.HasIBS;
import org.evoludo.simulator.models.IBSDPopulation;
import org.evoludo.simulator.models.Model;
import org.evoludo.simulator.models.ODE.HasODE;
import org.evoludo.simulator.models.PDE.HasPDE;
import org.evoludo.simulator.models.RungeKutta;
import org.evoludo.simulator.models.SDE.HasSDE;
import org.evoludo.simulator.views.HasHistogram;
import org.evoludo.simulator.views.HasMean;
import org.evoludo.simulator.views.HasPop2D;
import org.evoludo.simulator.views.HasPop3D;
import org.evoludo.simulator.views.HasS3;
import org.evoludo.util.CLOParser;
import org.evoludo.util.CLOption;
import org.evoludo.util.CLOption.CLODelegate;
import org.evoludo.util.Formatter;

public class SIR extends Discrete implements HasIBS, HasODE, HasSDE, HasPDE,
		HasPop2D.Traits, HasPop3D.Traits, HasMean.Traits, HasS3, HasHistogram.Degree,
		HasHistogram.StatisticsProbability, HasHistogram.StatisticsTime, HasHistogram.StatisticsStationary {

	/**
	 * The index of the susceptible trait/cohort.
	 */
	final static int S = 0;

	/**
	 * The index of the infected trait/cohort.
	 */
	final static int I = 1;

	/**
	 * The index of the recovered trait/cohort.
	 */
	final static int R = 2;

	/**
	 * The transition probability/rate for susceptibles to infected, S -> I.
	 */
	double pSI = 1.0;

	/**
	 * The transition probability/rate for infected to recovered, I -> R.
	 */
	double pIR = 0.3;

	/**
	 * The transition probability/rate for infected to susceptible, I -> S.
	 */
	double pIS = 0.0;

	/**
	 * The transition probability/rate for recovered to susceptible, R -> S.
	 */
	double pRS = 0.7;

	public SIR(EvoLudo engine) {
		super(engine);
	}

	@Override
	public String getTitle() {
		return "Disease dynamics";
	}

	@Override
	public void load() {
		super.load();
		nTraits = 3;
		// optional
		String[] names = new String[nTraits];
		names[S] = "Susceptible";
		names[I] = "Infected";
		names[R] = "Recovered";
		setTraitNames(names);
		// optional
		Color[] colors = new Color[nTraits];
		colors[S] = Color.GREEN;
		colors[I] = Color.RED;
		colors[R] = Color.BLUE;
		setTraitColors(colors);
	}

	@Override
	public int getDependent() {
		return I;
	}

	/**
	 * Command line option to set the transition probability for S -> I.
	 */
	public final CLOption cloInfect = new CLOption("infect", "1.0", EvoLudo.catModule,
			"--infect <i>    S -> I", new CLODelegate() {

				@Override
				public boolean parse(String arg) {
					double s2i = CLOParser.parseDouble(arg);
					if (engine.getModel().isIBS() && (s2i < 0.0 || s2i > 1.0)) {
						logger.warning("invalid probability for S -> I transitions (" + arg + ")");
						return false;
					}
					pSI = s2i;
					return true;
				}

				@Override
				public void report(PrintStream output) {
					output.println("# infect:             " + Formatter.format(pSI, 4));
				}
			});

	/**
	 * Command line option to set the transition probability for I -> R.
	 */
	public final CLOption cloRecover = new CLOption("recover", "0.3", EvoLudo.catModule,
			"--recover <r[,s]>  I -> R, [I -> S]", new CLODelegate() {

				@Override
				public boolean parse(String arg) {
					double[] probs = CLOParser.parseVector(arg);
					switch (probs.length) {
						case 2:
							double p = probs[1];
							if (engine.getModel().isIBS() && (p < 0.0 || p > 1.0))
								break;
							pIS = p;
							//$FALL-THROUGH$
						case 1:
							p = probs[0];
							if (engine.getModel().isIBS() && (p < 0.0 || p > 1.0))
								break;
							pIR = p;
							return true;
						default:
					}
					logger.warning("invalid probabilities for I -> R[,S] transitions (" + arg + ")");
					return false;
				}

				@Override
				public void report(PrintStream output) {
					output.println("# recover:            " + Formatter.format(pIR, 4));
				}
			});

	/**
	 * Command line option to set the transition probability for R -> S.
	 */
	public final CLOption cloResist = new CLOption("resist", "0.7", EvoLudo.catModule,
			"--resist <r>    R -> S", new CLODelegate() {

				@Override
				public boolean parse(String arg) {
					double r2s = CLOParser.parseDouble(arg);
					if (engine.getModel().isIBS() && (r2s < 0.0 || r2s > 1.0)) {
						logger.warning("invalid probability for R -> S transitions (" + arg + ")");
						return false;
					}
					pRS = r2s;
					return true;
				}

				@Override
				public void report(PrintStream output) {
					output.println("# resist:             " + Formatter.format(pRS, 4));
				}
			});

	@Override
	public void collectCLO(CLOParser parser) {
		super.collectCLO(parser);
		parser.addCLO(cloInfect);
		parser.addCLO(cloRecover);
		parser.addCLO(cloResist);
	}

	/**
	 * The SIR model is defined by the following equations:
	 * <p>
	 * \begin{align*}
	 * \frac{dS}{dt} &= R \cdot p_{RS} + I \cdot p_{IS} - S \cdot I \cdot p_{SI} \\
	 * \frac{dI}{dt} &= S \cdot I \cdot p_{SI} - I \cdot (p_{IR} + p_{IS}) \\
	 * \frac{dR}{dt} &= I \cdot p_{IR} - R \cdot p_{RS}
	 * \end{align*}
	 * <p>
	 * where \(S\), \(I\), and \(R\) are the densities of susceptible, infected, and
	 * recovered cohorts of individuals and \(p_{SI}, p_{IR}, p_{RS}\), and
	 * \(p_{IS}\) are the transition rates between the different cohorts.
	 */
	void getDerivatives(double t, double[] state, double[] unused, double[] change) {
		change[S] = state[R] * pRS + state[I] * pIS - state[S] * state[I] * pSI;
		change[I] = state[S] * state[I] * pSI - state[I] * (pIR + pIS);
		change[R] = state[I] * pIR - state[R] * pRS;
	}

	@Override
	public Model createODE() {
		return new SIR.ODE(engine);
	}

	/**
	 * ODE model for the SIR module.
	 */
	public class ODE extends RungeKutta {

		/**
		 * Constructor for the classic SIR model based on ordinary differential
		 * equations.
		 *
		 * @param engine the EvoLudo engine
		 */
		public ODE(EvoLudo engine) {
			super(engine);
		}

		@Override
		protected void getDerivatives(double t, double[] state, double[] unused, double[] change) {
			SIR.this.getDerivatives(t, state, unused, change);
		}
	}

	@Override
	public Model createSDE() {
		return new SIR.SDE(engine);
	}

	/**
	 * SDE model for the SIR module.
	 */
	public class SDE extends org.evoludo.simulator.models.SDE {

		/**
		 * Constructor for the SIR model in finite populations based on stochastic
		 * differential equations.
		 *
		 * @param engine the EvoLudo engine
		 */
		public SDE(EvoLudo engine) {
			super(engine);
		}

		@Override
		protected void getDerivatives(double t, double[] state, double[] unused, double[] change) {
			SIR.this.getDerivatives(t, state, unused, change);
		}
	}

	@Override
	public Model createPDE() {
		return new SIR.PDE(engine);
	}

	/**
	 * PDE model for the SIR module.
	 */
	public class PDE extends org.evoludo.simulator.models.PDE {

		/**
		 * Constructor for the spatial SIR model based on partial differential
		 * equations.
		 *
		 * @param engine the EvoLudo engine
		 */
		public PDE(EvoLudo engine) {
			super(engine);
		}

		@Override
		protected void getDerivatives(double t, double[] state, double[] unused, double[] change) {
			SIR.this.getDerivatives(t, state, unused, change);
		}
	}

	@Override
	public IBSDPopulation createIBSPop() {
		return new SIR.IBSPop(engine, this);
	}

	/**
	 * Population for individual based simulations of the SIR module.
	 */
	public class IBSPop extends IBSDPopulation {

		/**
		 * Constructor for SIR population.
		 *
		 * @param engine the pacemaker for running the model
		 * @param module the module that defines the interactions
		 */
		protected IBSPop(EvoLudo engine, SIR module) {
			super(engine, module);
		}

		@Override
		public boolean updatePlayerAt(int me, int[] refGroup, int rGroupSize) {
			int type = getTraitAt(me);
			switch (type) {
				case S: // S -> I transition
					int nI = 0;
					for (int n = 0; n < rGroupSize; n++) {
						if ((getTraitAt(refGroup[n])) == I)
							nI++; // short for nI = nI + 1
					}
					if (nI > 0 && random01() > Combinatorics.pow(1.0 - pSI, nI))
						return setNextTraitAt(me, I);
					break;
				case I: // I -> R transition
					if (pIR > 0.0 && random01() < pIR)
						return setNextTraitAt(me, R);
					break;
				case R: // R -> S transition
					if (pRS > 0.0 && random01() < pRS)
						return setNextTraitAt(me, S);
					break;
				default:
			}
			return false;
		}

		@Override
		public boolean checkConvergence() {
			if (traitsCount[I] == 0 && pRS < 1e-8) {
				// if R -> S is not possible and I is extinct
				return true;
			}
			return super.checkConvergence();
		}
	}
}
