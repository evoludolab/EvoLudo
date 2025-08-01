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

import org.evoludo.math.Combinatorics;
import org.evoludo.simulator.EvoLudo;
import org.evoludo.simulator.models.IBS;
import org.evoludo.simulator.models.IBS.HasIBS;
import org.evoludo.simulator.models.IBSDPopulation;
import org.evoludo.simulator.models.Model;
import org.evoludo.simulator.models.Model.HasDE;
import org.evoludo.simulator.models.RungeKutta;
import org.evoludo.simulator.models.Type;
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
 * The SIR module implements the classic Susceptible-Infected-Recovered (SIR)
 * model of disease dynamics. It simulates the spread of an infectious disease
 * through a population divided into three cohorts: susceptible (S), infected
 * (I), and recovered (R). The model is defined by transition
 * probabilities/rates between these cohorts:
 * <ul>
 * <li>S -> I: Susceptible individuals become infected with a certain
 * probability/rate
 * <li>I -> R: Infected individuals recover with a certain probability/rate
 * <li>I -> S: Infected individuals can become susceptible again (optional)
 * <li>R -> S: Recovered individuals can become susceptible again with a certain
 * probability/rate
 * </ul>
 * 
 * @author Christoph Hauert
 */
public class SIR extends Discrete implements HasIBS, HasDE.ODE, HasDE.SDE, HasDE.PDE,
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
	 * The transition probability/rate for susceptibles to infected, S -> I,
	 * including seasonal variation {@code pSI = pSI[0] + pSI[1] cos(pSI[2] t)}.
	 */
	double[] pSI = new double[] { 1.0, 0.0, 0.0 };

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

	/**
	 * Create a new SIR module with the given pacemaker.
	 * 
	 * @param engine the pacemaker for running the model
	 */
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

	private static final double TWOPI = 2.0 * Math.PI;

	/**
	 * Command line option to set the transition probability for S -> I.
	 */
	public final CLOption cloInfect = new CLOption("infect", "1.0", Category.Module,
			"--infect <β,[A,ω]>  S -> I, β+A cos(2π ω t)", new CLODelegate() {

				@Override
				public boolean parse(String arg) {
					double[] s2i = CLOParser.parseVector(arg);
					boolean isIBS = engine.getModel().getType().isIBS();
					Arrays.fill(pSI, 0.0);
					switch (s2i.length) {
						case 3:
							pSI[2] = s2i[2];
							pSI[1] = TWOPI * s2i[1];
							//$FALL-THROUGH$
						case 1:
							if (isIBS && (s2i[0] < 0.0 || s2i[0] > 1.0))
								return false;
							pSI[0] = s2i[0];
							break;
						default:
							return false;
					}
					return true;
				}
			});

	/**
	 * Command line option to set the transition probability for I -> R.
	 */
	public final CLOption cloRecover = new CLOption("recover", "0.3", Category.Module,
			"--recover <r[,s]>  I -> R, [I -> S]", new CLODelegate() {

				@Override
				public boolean parse(String arg) {
					// set default for optional I -> S transition
					pIS = 0.0;
					double[] probs = CLOParser.parseVector(arg);
					boolean isIBS = engine.getModel().getType().isIBS();
					switch (probs.length) {
						case 2:
							double p = probs[1];
							if (isIBS && (p < 0.0 || p > 1.0))
								break;
							pIS = p;
							//$FALL-THROUGH$
						case 1:
							p = probs[0];
							if (isIBS && (p < 0.0 || p > 1.0))
								break;
							pIR = p;
							return true;
						default:
					}
					return false;
				}
			});

	/**
	 * Command line option to set the transition probability for R -> S.
	 */
	public final CLOption cloResist = new CLOption("resist", "0.7", Category.Module,
			"--resist <r>    R -> S", new CLODelegate() {

				@Override
				public boolean parse(String arg) {
					double r2s = CLOParser.parseDouble(arg);
					boolean isIBS = engine.getModel().getType().isIBS();
					if (isIBS && (r2s < 0.0 || r2s > 1.0))
						return false;
					pRS = r2s;
					return true;
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
	 * \frac{dS}{dt} =&amp; R \cdot p_{RS} + I \cdot p_{IS} - S \cdot I \cdot p_{SI}
	 * \\
	 * \frac{dI}{dt} =&amp; S \cdot I \cdot p_{SI} - I \cdot (p_{IR} + p_{IS}) \\
	 * \frac{dR}{dt} =&amp; I \cdot p_{IR} - R \cdot p_{RS}
	 * \end{align*}
	 * <p>
	 * where \(S\), \(I\), and \(R\) are the densities of susceptible, infected, and
	 * recovered cohorts of individuals and \(p_{SI}, p_{IR}, p_{RS}\), and
	 * \(p_{IS}\) are the transition rates between the different cohorts.
	 * 
	 * @param t      the current time (not used in this model)
	 * @param state  the current state of the system, an array containing the
	 *               densities of the susceptible, infected, and recovered cohorts
	 * @param unused an unused array (for compatibility with the {@link Payoffs}
	 *               interface)
	 * @param change the array to store the changes in the densities of the cohorts
	 */
	void getDerivatives(double t, double[] state, double[] unused, double[] change) {
		double psi1 = pSI[1];
		double psi = pSI[0];
		if (psi1 > 0.0 )
			psi += psi1 * Math.cos(pSI[2] * t);
		change[S] = state[R] * pRS + state[I] * pIS - state[S] * state[I] * psi;
		change[I] = state[S] * state[I] * psi - state[I] * (pIR + pIS);
		change[R] = state[I] * pIR - state[R] * pRS;
	}

	@Override
	public Model createModel(Type type) {
		switch (type) {
			case ODE:
				return new SIR.ODE();
			case SDE:
				return new SIR.SDE();
			case PDE:
				return new SIR.PDE();
			case IBS:
				return super.createModel(type);
			default:
				return null;
		}
	}

	/**
	 * ODE model for the SIR module.
	 */
	public class ODE extends RungeKutta {

		/**
		 * Constructor for the classic SIR model based on ordinary differential
		 * equations.
		 */
		public ODE() {
			super(SIR.this.engine);
		}

		@Override
		protected void getDerivatives(double t, double[] state, double[] unused, double[] change) {
			SIR.this.getDerivatives(t, state, unused, change);
		}
	}

	/**
	 * SDE model for the SIR module.
	 */
	public class SDE extends org.evoludo.simulator.models.SDE {

		/**
		 * Constructor for the SIR model in finite populations based on stochastic
		 * differential equations.
		 */
		public SDE() {
			super(SIR.this.engine);
		}

		@Override
		protected void getDerivatives(double t, double[] state, double[] unused, double[] change) {
			SIR.this.getDerivatives(t, state, unused, change);
		}
	}

	/**
	 * PDE model for the SIR module.
	 */
	public class PDE extends org.evoludo.simulator.models.PDE {

		/**
		 * Constructor for the spatial SIR model based on partial differential
		 * equations.
		 */
		public PDE() {
			super(SIR.this.engine);
		}

		@Override
		protected void getDerivatives(double t, double[] state, double[] unused, double[] change) {
			SIR.this.getDerivatives(t, state, unused, change);
		}
	}

	@Override
	public IBSDPopulation createIBSPop() {
		return new SIR.IBSPop();
	}

	/**
	 * Population for individual based simulations of the SIR module.
	 */
	public class IBSPop extends IBSDPopulation {

		/**
		 * The individual based simulation model for SIR. Convenience variable to avoid
		 * casts when retrieving the elapsed time with {@code getRealtime()}.
		 */
		IBS ibs;

		/**
		 * Constructor for SIR population.
		 */
		protected IBSPop() {
			super(SIR.this.engine, SIR.this);
			ibs = (IBS) model;
		}

		@Override
		public boolean updatePlayerAt(int me, int[] refGroup, int rGroupSize) {
			int type = getTraitAt(me);
			switch (type) {
				case S: // S -> I transition
					int nI = 0;
					for (int n = 0; n < rGroupSize; n++) {
						if ((getTraitAt(refGroup[n])) == I)
							nI++;
					}
					double psi1 = pSI[1];
					double psi = pSI[0];
					if (psi1 > 0.0 )
						psi += psi1 * Math.cos(pSI[2] * ibs.getTime());
					if (nI > 0 && random01() > Combinatorics.pow(1.0 - psi, nI))
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
