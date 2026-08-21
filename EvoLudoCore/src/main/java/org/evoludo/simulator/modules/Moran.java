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
			// rhoA(), t1(), tA1(), ti() and tAi() run in O(N) (see their javadoc),
			// not the O(N^2)-O(N^3) their naive nested-loop form would cost, so this
			// cutoff is no longer about raw runtime (benchmarked at <10ms even for
			// N=1'000'000) but simply bounds the O(N) array allocations
			if (nPopulation > 1000000)
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
	 * @return the ratio of transition probabilities
	 */
	private double Tratio() {
		double fA = typeScores[MUTANT];
		double fB = typeScores[RESIDENT];
		return fB / fA;
	}

	/**
	 * The fixation probability of {@code i} mutants in a population of constant
	 * size {@code N} (and {@code N-i} residents).
	 * <p>
	 * Runs in {@code O(N)} by carrying a single running product of
	 * {@link #Tratio()} terms across the loop instead of recomputing it from
	 * scratch for every term, i.e. Algorithm 2 (not the naive, quadratic
	 * Algorithm 1) in Hindersin, Wu, Traulsen &amp; Garc&iacute;a (2019)
	 * "Computation and Simulation of Evolutionary Game Dynamics in Finite
	 * Populations", Sci. Rep. 9:6946. The original, {@code O(N^2)} nested-loop
	 * version (kept here for reference) read:
	 *
	 * <pre>
	 * double gamma = Tratio();
	 * double num = 1.0;
	 * for (int k = 1; k &lt; i; k++) {
	 * 	double prod = 1.0;
	 * 	for (int j = 1; j &lt;= k; j++)
	 * 		prod *= gamma;
	 * 	num += prod;
	 * }
	 * double denom = num;
	 * for (int k = i; k &lt; n; k++) {
	 * 	double prod = 1.0;
	 * 	for (int j = 1; j &lt;= k; j++)
	 * 		prod *= gamma;
	 * 	denom += prod;
	 * }
	 * return num / denom;
	 * </pre>
	 * <p>
	 *
	 * @param i the number of mutants
	 * @param n the size of the population
	 * @return the fixation probability
	 */
	protected double rhoA(int i, int n) {
		double gamma = Tratio();
		double num = 1.0;
		double prod = 1.0;
		for (int k = 1; k < i; k++) {
			prod *= gamma;
			num += prod;
		}
		double denom = num;
		for (int k = i; k < n; k++) {
			prod *= gamma;
			denom += prod;
		}
		return num / denom;
	}

	/**
	 * The absorption time (in generations) of a single mutant in a population of
	 * constant size {@code N} (and {@code N-1} residents).
	 * <p>
	 * Runs in {@code O(N)}, analogous to Algorithm 3 (the {@code R}-recursion) in
	 * Hindersin, Wu, Traulsen &amp; Garc&iacute;a (2019), Sci. Rep. 9:6946: the
	 * inner sum {@code psi(l) = sum_{k=l}^{N-1} prod_{m=l+1}^{k} gamma}
	 * satisfies the backward recursion {@code psi(l-1) = 1 + gamma*psi(l)}
	 * with {@code psi(N-1) = 1}, avoiding recomputing the product from scratch
	 * for every {@code l}. The original, {@code O(N^3)} nested-loop version (kept
	 * here for reference) read:
	 *
	 * <pre>
	 * double gamma = Tratio();
	 * double sum = 0.0;
	 * for (int l = 1; l &lt; n; l++) {
	 * 	double sumprod = 0.0;
	 * 	for (int k = l; k &lt; n; k++) {
	 * 		double prod = 1.0;
	 * 		for (int m = l + 1; m &lt;= k; m++)
	 * 			prod *= gamma;
	 * 		sumprod += prod;
	 * 	}
	 * 	sum += sumprod / Tplus(l, n);
	 * }
	 * return (sum * rhoA) / n;
	 * </pre>
	 * <p>
	 * As in {@link #rhoA(int, int)}, {@link #Tratio()} is constant here and so
	 * cached once in a local {@code gamma} rather than re-evaluated per iteration.
	 *
	 * @param rhoA the fixation probability of a single mutant
	 * @param n    the size of the population
	 * @return the absorption time
	 *
	 * @see #rhoA(int, int)
	 */
	protected double t1(double rhoA, int n) {
		double gamma = Tratio();
		double sum = 0.0;
		double psi = 1.0;
		for (int l = n - 1; l >= 1; l--) {
			sum += psi / Tplus(l, n);
			psi = 1.0 + gamma * psi;
		}
		return (sum * rhoA) / n;
	}

	/**
	 * The absorption time (in generations) of {@code i} mutants in a population of
	 * constant size {@code N} (and {@code N-i} residents).
	 * <p>
	 * Derived from the first-step recursion {@code d_{k+1} = gamma*d_k -
	 * 1/Tplus(k,n)} for {@code d_k = tau_k - tau_{k-1}} (in elementary update
	 * steps), unrolled and summed to give
	 * {@code tau_i = tau_1*phi_i - sum_{k=1}^{i-1} psi(k,i-1)/Tplus(k,n)}, where
	 * {@code phi_i} is the (unnormalized) fixation numerator used in
	 * {@link #rhoA(int, int)} and {@code psi(k,m) = sum_{l=k}^{m}
	 * prod_{j=k+1}^{l} gamma}. This is the same style of recursion as
	 * Algorithm 3 in Hindersin, Wu, Traulsen &amp; Garc&iacute;a (2019), Sci.
	 * Rep. 9:6946 (see {@link #t1(double, int)}), here truncated at {@code i-1}
	 * instead of {@code N-1} since {@link #t1(double, int)} only ever needs the
	 * single-mutant case.
	 * <p>
	 * {@code psi(k,i-1)} itself satisfies the backward recursion
	 * {@code psi(k-1,i-1) = 1 + gamma*psi(k,i-1)}, so it can be swept once in
	 * {@code O(i)} instead of being recomputed from scratch for every {@code k},
	 * which is what the correct but quadratic version below (kept for reference)
	 * did:
	 *
	 * <pre>
	 * double gamma = Tratio();
	 * double sum = 0.0;
	 * for (int k = 1; k &lt; i; k++) {
	 * 	double psi = 0.0;
	 * 	double p = 1.0;
	 * 	for (int l = k; l &lt; i; l++) {
	 * 		psi += p;
	 * 		p *= gamma;
	 * 	}
	 * 	sum += psi / Tplus(k, n);
	 * }
	 * return t1 * phi - sum / n;
	 * </pre>
	 * <p>
	 *
	 * @param i  the number of mutants
	 * @param n  the size of the population
	 * @param t1 the absorption time of a single mutant
	 * @return the absorption time of {@code i} mutants
	 *
	 * @see #t1(double, int)
	 */
	protected double ti(int i, int n, double t1) {
		double gamma = Tratio();
		double phi = 1.0;
		double prod = 1.0;
		for (int k = 1; k < i; k++) {
			prod *= gamma;
			phi += prod;
		}
		double sum = 0.0;
		if (i > 1) {
			double psi = 1.0;
			sum += psi / Tplus(i - 1, n);
			for (int k = i - 2; k >= 1; k--) {
				psi = 1.0 + gamma * psi;
				sum += psi / Tplus(k, n);
			}
		}
		return t1 * phi - sum / n;
	}

	/**
	 * The conditional fixation time of a single mutant in a population of fixed
	 * size {@code N} (and {@code N-1} residents) measured in generations.
	 * <p>
	 * Runs in {@code O(N)}: the same backward {@code psi(l)} recursion as
	 * {@link #t1(double, int)} is combined with a single forward pass that
	 * accumulates the fixation numerator {@code phi_l} (as in
	 * {@link #rhoA(int, int)}) for every {@code l}, so {@code rhoA(l,n)} needed
	 * inside the sum is a plain lookup rather than an {@code O(N)} recomputation
	 * per term (which would otherwise make the whole sum {@code O(N^2)}). This
	 * mirrors the memoized {@code prod-gamma-list} used by Algorithm 4 in
	 * Hindersin, Wu, Traulsen &amp; Garc&iacute;a (2019), Sci. Rep. 9:6946. The
	 * original, {@code O(N^3)} nested-loop version (kept here for reference)
	 * read:
	 *
	 * <pre>
	 * double gamma = Tratio();
	 * double sum = 0.0;
	 * for (int l = 1; l &lt; n; l++) {
	 * 	double sumprod = 0.0;
	 * 	for (int k = l; k &lt; n; k++) {
	 * 		double prod = 1.0;
	 * 		for (int m = l + 1; m &lt;= k; m++)
	 * 			prod *= gamma;
	 * 		sumprod += prod;
	 * 	}
	 * 	sum += sumprod * rhoA(l, n) / Tplus(l, n);
	 * }
	 * return sum / n;
	 * </pre>
	 * <p>
	 *
	 * @param n the size of the population
	 * @return the conditional fixation time
	 */
	protected double tA1(int n) {
		double gamma = Tratio();
		double[] phi = new double[n + 1];
		phi[1] = 1.0;
		double prod = 1.0;
		for (int l = 2; l <= n; l++) {
			prod *= gamma;
			phi[l] = phi[l - 1] + prod;
		}
		double phiN = phi[n];
		double sum = 0.0;
		double psi = 1.0;
		for (int l = n - 1; l >= 1; l--) {
			sum += psi * (phi[l] / phiN) / Tplus(l, n);
			psi = 1.0 + gamma * psi;
		}
		return sum / n;
	}

	/**
	 * The conditional fixation time of {@code i} mutants in a population of fixed
	 * size {@code N} (and {@code N-i} residents) measured in generations.
	 * <p>
	 * Derived analogously to {@link #ti(int, int, double)}: the recursion for
	 * {@code e_k = f_k - f_{k-1}}, with {@code f_k = rhoA(k,n)*(conditional
	 * fixation time from k mutants)}, is {@code e_{k+1} = gamma*e_k -
	 * rhoA(k,n)/Tplus(k,n)}. Unrolling gives {@code f_i = f_1*phi_i -
	 * sum_{k=1}^{i-1} psi(k,i-1)*rhoA(k,n)/Tplus(k,n)}, and the conditional
	 * fixation time of {@code i} mutants is {@code f_i/rhoAi}.
	 * <p>
	 * Runs in {@code O(N)}: as in {@link #tA1(int)}, {@code phi_k} for every
	 * {@code k=1..i-1} (needed for the {@code rhoA(k,n)} lookups) is accumulated
	 * once in a forward pass instead of being recomputed per term, and the same
	 * pass is continued up to {@code n} to obtain the fixation-probability
	 * denominator (see {@link #rhoA(int, int)}); {@code psi(k,i-1)} uses the same
	 * backward recursion as {@link #ti(int, int, double)}. The original,
	 * quadratic-in-{@code rhoA} (and thus effectively {@code O(N^2)}) version
	 * (kept here for reference) read:
	 *
	 * <pre>
	 * double gamma = Tratio();
	 * double rhoA1 = rhoA(1, n);
	 * double phi = 1.0;
	 * double prod = 1.0;
	 * for (int k = 1; k &lt; i; k++) {
	 * 	prod *= gamma;
	 * 	phi += prod;
	 * }
	 * double sum = 0.0;
	 * for (int k = 1; k &lt; i; k++) {
	 * 	double psi = 0.0;
	 * 	double p = 1.0;
	 * 	for (int l = k; l &lt; i; l++) {
	 * 		psi += p;
	 * 		p *= gamma;
	 * 	}
	 * 	sum += psi * rhoA(k, n) / Tplus(k, n);
	 * }
	 * return (rhoA1 * tA1 * phi - sum / n) / rhoAi;
	 * </pre>
	 * <p>
	 *
	 * @param i     the number of mutants
	 * @param n     the size of the population
	 * @param rhoAi the fixation probability of {@code i} mutants
	 * @param tA1   the fixation time of a single mutant
	 * @return the conditional fixation time
	 *
	 * @see #ti(int, int, double)
	 * @see #tA1(int)
	 */
	protected double tAi(int i, int n, double rhoAi, double tA1) {
		double gamma = Tratio();
		double[] phik = new double[Math.max(i, 1)];
		double prod = 1.0;
		double phi = 1.0;
		for (int k = 1; k < i; k++) {
			phik[k] = phi;
			prod *= gamma;
			phi += prod;
		}
		double phiI = phi;
		for (int k = i; k < n; k++) {
			prod *= gamma;
			phi += prod;
		}
		double phiN = phi;
		double rhoA1 = 1.0 / phiN;
		double sum = 0.0;
		if (i > 1) {
			double psi = 1.0;
			sum += psi * (phik[i - 1] / phiN) / Tplus(i - 1, n);
			for (int k = i - 2; k >= 1; k--) {
				psi = 1.0 + gamma * psi;
				sum += psi * (phik[k] / phiN) / Tplus(k, n);
			}
		}
		return (rhoA1 * tA1 * phiI - sum / n) / rhoAi;
	}
}
