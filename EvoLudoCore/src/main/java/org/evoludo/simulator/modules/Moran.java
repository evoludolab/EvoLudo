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

import org.evoludo.math.Combinatorics;
import org.evoludo.simulator.EvoLudo;
import org.evoludo.simulator.models.Data;
import org.evoludo.simulator.models.Model.HasDE;
import org.evoludo.simulator.models.Model.HasIBS;
import org.evoludo.simulator.modules.Features.Static;
import org.evoludo.simulator.views.HasHistogram;
import org.evoludo.simulator.views.HasMean;
import org.evoludo.simulator.views.HasPop2D;
import org.evoludo.simulator.views.HasPop3D;
import org.evoludo.util.CLODelegate;
import org.evoludo.util.CLOParser;
import org.evoludo.util.CLOption;
import org.evoludo.util.CLOCategory;

/**
 * The module for investigating the evolutionary dynamics in the classical Moran
 * process with two types of individuals that have constant fitness values. In
 * the limit of infinite populations the type with the higher fitness invariably
 * takes over regardless of its fitness advantage. However, in finite
 * populations the less fit type may nevertheless reach fixation or the fitter
 * type vanish. In particular, a single mutant in a population of size \(N\)
 * with (scaled) fitness \(r\) in an otherwise homogeneous resident population
 * with fitness \(1\) reaches fixation with probability
 * \begin{align}
 * \rho_N &amp;= \dfrac{1-\frac1r}{1-\frac1{r^N}}.
 * \end{align}
 * Interestingly, for beneficial mutants, \(r&gt;1\), the fixation probability
 * \(\rho\) does not approach \(1\) even in the limit \(N\to\infty\) but rather
 * \(\rho_\infty = 1-1/r\).
 * <p>
 * In structured populations the situation gets even more interesting in that
 * certain population structures can act as evolutionary amplifiers or
 * evolutionary suppressors by increasing or decreasing the fixation
 * probabilities of advantageous mutants.
 *
 * @author Christoph Hauert
 */
public class Moran extends Discrete implements Static,
		HasIBS, HasDE.RK5, HasDE.EM, HasDE.SDE, HasDE.PDERD, HasDE.PDEADV,
		HasPop2D.Traits, HasPop3D.Traits, HasMean.Traits,
		HasPop2D.Fitness, HasPop3D.Fitness, HasMean.Fitness,
		HasHistogram.Fitness, HasHistogram.Degree, HasHistogram.StatisticsProbability,
		HasHistogram.StatisticsTime, HasHistogram.StatisticsStationary {

	/**
	 * The trait (and index) value of residents.
	 */
	public static final int RESIDENT = 0;

	/**
	 * The trait (and index) value of mutants.
	 */
	public static final int MUTANT = 1;

	/**
	 * The array with the scores for each trait.
	 */
	protected double[] typeScores;

	/**
	 * Create a new instance of the module for the Moran process.
	 * 
	 * @param engine the manager of modules and pacemaker for running the model
	 */
	public Moran(EvoLudo engine) {
		super(engine);
		nTraits = 2; // residents and mutants
	}

	@Override
	public void load() {
		super.load();
		// trait names
		setTraitNames(new String[] { "Resident", "Mutant" });
		// trait colors (automatically generates lighter versions for new traits)
		setTraitColors(new Color[] { Color.BLUE, Color.RED });
		// default scores
		typeScores = new double[nTraits];
		typeScores[RESIDENT] = 1.0;
		typeScores[MUTANT] = 2.0;
	}

	@Override
	public void unload() {
		super.unload();
		typeScores = null;
	}

	@Override
	public String getAuthors() {
		return "Christoph Hauert";
	}

	@Override
	public String getTitle() {
		return "Moran process";
	}

	@Override
	public int getDependent() {
		return RESIDENT;
	}

	@Override
	public double getMinPayoff() {
		return Math.min(typeScores[RESIDENT], typeScores[MUTANT]);
	}

	@Override
	public double getMaxPayoff() {
		return Math.max(typeScores[RESIDENT], typeScores[MUTANT]);
	}

	@Override
	public double getMonoPayoff(int type) {
		return typeScores[type];
	}

	@Override
	public double[] getStaticScores() {
		return typeScores;
	}

	/**
	 * Set the fitness values for residents and mutants.
	 * 
	 * @param aValue the array with fitness values
	 */
	public void setFitness(double[] aValue) {
		System.arraycopy(aValue, 0, typeScores, 0, nTraits);
	}

	/**
	 * Set the fitness value for trait {@code aType} to {@code aValue}.
	 * 
	 * @param aValue the fitness for trait {@code aType}
	 * @param aType  the trait to set the fitness
	 */
	public void setFitness(double aValue, int aType) {
		if (aType < 0 || aType >= nTraits)
			return;
		typeScores[aType] = aValue;
	}

	/**
	 * Get the array of fitness values for residents and mutants.
	 * 
	 * @return the array of fitness values
	 */
	public double[] getFitness() {
		return typeScores;
	}

	/**
	 * Get the fitness value for trait {@code aType}.
	 * 
	 * @param aType the trait to get the fitness
	 * @return the fitness value
	 */
	public double getFitness(int aType) {
		if (aType < 0 || aType >= nTraits)
			return Double.NaN;
		return typeScores[aType];
	}

	@Override
	public double[] getCustomLevels(Data type, int trait) {
		// currently reference levels only available for Moran (birth-death) updates
		// in IBS models (otherwise ibs is null, see reset(Model)
		if (model == null || !model.isIBS() || !getIBSPopulation().getPopulationUpdate().isMoran())
			return new double[0];
		// Note:
		// - return reference levels for fixation probabilities and times based
		// on analytical calculations for the Moran process
		switch (type) {
			case STATISTICS_FIXATION_PROBABILITY:
				return getReferenceProb(trait);
			case STATISTICS_FIXATION_TIME:
				return getReferenceTime(trait);
			default:
				return new double[0];
		}
	}

	/**
	 * Store fixation probability of {@code i} individuals of type {@code A}.
	 */
	double rhoAi = -1.0;

	/**
	 * Store fixation time of {@code i} individuals of type {@code A}.
	 */
	double tAi = -1.0;

	/**
	 * Store absorption time of {@code i} individuals of type {@code A}.
	 */
	double taui = -1.0;

	@Override
	public void modelSettings() {
		super.modelSettings();
		rhoAi = -1.0;
		tAi = -1.0;
		taui = -1.0;
	}

	/**
	 * Helper method to retrieve the reference fixation probabilities for trait
	 * {@code trait} for the initial number of mutants according to {@code init}.
	 * In order to optimize repeated calls the result is stored in the field
	 * {@code statRefProb} and storage allocated as necessary.
	 * 
	 * @param trait the trait for which to get the reference fixation probability
	 * @return the reference fixation probability
	 * 
	 * @see #rhoA(int, int)
	 */
	private double[] getReferenceProb(int trait) {
		if (rhoAi < 0.0) {
			// calculate reference fixation probabilities
			double[] init = new double[nTraits];
			((org.evoludo.simulator.models.DModel) model).getInitialTraits(init);
			int m = (int) (init[MUTANT] * nPopulation);
			m = Math.min(Math.max(m, 1), nPopulation - 1);
			if (nPopulation > 1000)
				rhoAi = Math.max(0.0, 1.0 - 1.0 / Combinatorics.pow(typeScores[MUTANT], m));
			else
				rhoAi = rhoA(m, nPopulation);
		}
		if (trait == MUTANT)
			return new double[] { rhoAi };
		return new double[] { 1.0 - rhoAi };
	}

	/**
	 * Helper method to retrieve the reference fixation times for trait
	 * {@code trait} for the initial number of mutants according to {@code init}.
	 * In order to optimize repeated calls the result is stored in the field
	 * {@code statRefTime} and storage allocated as necessary.
	 * 
	 * @param trait the trait for which to get the reference fixation time
	 * @return the reference fixation time
	 * 
	 * @see #tA1(int)
	 * @see #t1(double, int)
	 */
	private double[] getReferenceTime(int trait) {
		if (tAi < 0.0) {
			// calculate reference fixation times
			// numerical evaluations take too long for large populations
			if (nPopulation > 500)
				return new double[0];
			double[] init = new double[nTraits];
			((org.evoludo.simulator.models.DModel) model).getInitialTraits(init);
			int m = (int) (init[MUTANT] * nPopulation);
			m = Math.min(Math.max(m, 1), nPopulation - 1);
			double rhoA1 = rhoA(1, nPopulation);
			double tA1 = tA1(nPopulation);
			double t1 = t1(rhoA1, nPopulation);
			if (m > 1) {
				rhoAi = rhoA(m, nPopulation);
				tAi = tAi(m, nPopulation, rhoAi, tA1);
				taui = ti(m, nPopulation, t1);
			} else {
				rhoAi = rhoA1;
				tAi = tA1;
				taui = t1;
			}
		}
		if (trait == MUTANT)
			return new double[] { tAi };
		if (trait == nTraits)
			return new double[] { taui };
		return new double[] { (taui - rhoAi * tAi) / (1.0 - rhoAi) };
	}

	/**
	 * Command line option to set the fitness values for residents and mutants.
	 */
	public final CLOption cloFitness = new CLOption("fitness", "1,2", CLOCategory.Module,
			"--fitness <r,m>  fitness of resident, mutant", new CLODelegate() {
				@Override
				public boolean parse(String arg) {
					double[] fit = CLOParser.parseVector(arg);
					if (fit.length != 2)
						return false;
					setFitness(fit);
					return true;
				}
			});

	@Override
	public void collectCLO(CLOParser parser) {
		super.collectCLO(parser);
		// prepare command line options
		parser.addCLO(cloFitness);
	}

	// ANALYTICAL CALCULATIONS OF FIXATION PROBABILITIES AND TIMES

	/**
	 * Transition probability to go from {@code i} to {@code i+1} mutants in a
	 * population of size {@code N}.
	 * 
	 * @param i the number of mutants
	 * @param n the size of the population
	 * @return the transition probability
	 */
	private double Tplus(int i, int n) {
		int N = n;
		double fA = typeScores[MUTANT];
		double fB = typeScores[RESIDENT];
		return (N - i) * i * fA / (N * (i * fA + (N - i) * fB));
	}

	/**
	 * The ratio of transition probabilities to go from {@code i} to {@code i+1}
	 * mutants over the reverse, i.e. \(T_i^-/T_i^+). In the classical Moran process
	 * this is simply a constant and, in particular, independent of {@code i}.
	 * 
	 * @param i the number of mutants
	 * @return the ratio of transition probabilities
	 */
	private double Tratio(int i) {
		double fA = typeScores[MUTANT];
		double fB = typeScores[RESIDENT];
		return fB / fA;
	}

	/**
	 * The fixation probability of {@code i} mutants in a population of constant
	 * size {@code N} (and {@code N-i} residents).
	 * 
	 * @param i the number of mutants
	 * @param n the size of the population
	 * @return the fixation probability
	 */
	protected double rhoA(int i, int n) {
		int N = n;
		double num = 1.0;
		for (int k = 1; k < i; k++) {
			double prod = 1.0;
			for (int j = 1; j <= k; j++) {
				prod *= Tratio(j);
			}
			num += prod;
		}
		double denom = num;
		for (int k = i; k < N; k++) {
			double prod = 1.0;
			for (int j = 1; j <= k; j++) {
				prod *= Tratio(j);
			}
			denom += prod;
		}
		return num / denom;
	}

	/**
	 * The absorption time (in generations) of a single mutant in a population of
	 * constant size {@code N} (and {@code N-1} residents).
	 * 
	 * @param rhoA the fixation probability of a single mutant
	 * @param n    the size of the population
	 * @return the absorption time
	 * 
	 * @see #rhoA(int, int)
	 */
	protected double t1(double rhoA, int n) {
		int N = n;
		double sum = 0.0;
		for (int l = 1; l < N; l++) {
			double sumprod = 0.0;
			for (int k = l; k < N; k++) {
				double prod = 1.0;
				for (int m = l + 1; m <= k; m++) {
					prod *= Tratio(m);
				}
				sumprod += prod;
			}
			sum += sumprod / Tplus(l, n);
		}
		return (sum * rhoA) / N;
	}

	/**
	 * The absorption time (in generations) of {@code i} mutants in a population of
	 * constant size {@code N} (and {@code N-i} residents).
	 * <p>
	 * Derived from the first-step recursion {@code d_{k+1} = Tratio(k)*d_k -
	 * 1/Tplus(k,n)} for {@code d_k = tau_k - tau_{k-1}} (in elementary update
	 * steps), unrolled and summed to give
	 * {@code tau_i = tau_1*phi_i - sum_{k=1}^{i-1} psi(k,i-1)/Tplus(k,n)}, where
	 * {@code phi_i} is the (unnormalized) fixation numerator used in
	 * {@link #rhoA(int, int)} and {@code psi(k,m) = sum_{l=k}^{m}
	 * prod_{j=k+1}^{l} Tratio(j)}.
	 *
	 * @param i  the number of mutants
	 * @param n  the size of the population
	 * @param t1 the absorption time of a single mutant
	 * @return the absorption time of {@code i} mutants
	 *
	 * @see #t1(double, int)
	 */
	protected double ti(int i, int n, double t1) {
		double phi = 1.0;
		double prod = 1.0;
		for (int k = 1; k < i; k++) {
			prod *= Tratio(k);
			phi += prod;
		}
		double sum = 0.0;
		for (int k = 1; k < i; k++) {
			double psi = 0.0;
			double p = 1.0;
			for (int l = k; l < i; l++) {
				psi += p;
				p *= Tratio(l + 1);
			}
			sum += psi / Tplus(k, n);
		}
		return t1 * phi - sum / n;
	}

	/**
	 * The conditional fixation time of a single mutant in a population of fixed
	 * size {@code N} (and {@code N-1} residents) measured in generations.
	 * 
	 * @param n the size of the population
	 * @return the conditional fixation time
	 */
	protected double tA1(int n) {
		int N = n;
		double sum = 0.0;
		for (int l = 1; l < N; l++) {
			double sumprod = 0.0;
			for (int k = l; k < N; k++) {
				double prod = 1.0;
				for (int m = l + 1; m <= k; m++) {
					prod *= Tratio(m);
				}
				sumprod += prod;
			}
			sum += sumprod * rhoA(l, n) / Tplus(l, n);
		}
		return sum / N;
	}

	/**
	 * The conditional fixation time of {@code i} mutants in a population of fixed
	 * size {@code N} (and {@code N-i} residents) measured in generations.
	 * <p>
	 * Derived analogously to {@link #ti(int, int, double)}: the recursion for
	 * {@code e_k = f_k - f_{k-1}}, with {@code f_k = rhoA(k,n)*(conditional
	 * fixation time from k mutants)}, is {@code e_{k+1} = Tratio(k)*e_k -
	 * rhoA(k,n)/Tplus(k,n)}. Unrolling gives {@code f_i = f_1*phi_i -
	 * sum_{k=1}^{i-1} psi(k,i-1)*rhoA(k,n)/Tplus(k,n)}, and the conditional
	 * fixation time of {@code i} mutants is {@code f_i/rhoAi}.
	 *
	 * @param i     the number of mutants
	 * @param n     the size of the population
	 * @param rhoAi the fixation probability of {@code i} mutants
	 * @param tA1   the fixation time of a single mutant
	 * @return the conditional fixation time
	 *
	 * @see #ti(int, int, double)
	 */
	protected double tAi(int i, int n, double rhoAi, double tA1) {
		double rhoA1 = rhoA(1, n);
		double phi = 1.0;
		double prod = 1.0;
		for (int k = 1; k < i; k++) {
			prod *= Tratio(k);
			phi += prod;
		}
		double sum = 0.0;
		for (int k = 1; k < i; k++) {
			double psi = 0.0;
			double p = 1.0;
			for (int l = k; l < i; l++) {
				psi += p;
				p *= Tratio(l + 1);
			}
			sum += psi * rhoA(k, n) / Tplus(k, n);
		}
		return (rhoA1 * tA1 * phi - sum / n) / rhoAi;
	}
}
