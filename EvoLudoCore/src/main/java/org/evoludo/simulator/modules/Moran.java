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

import org.evoludo.simulator.EvoLudo;
import org.evoludo.simulator.models.IBS;
import org.evoludo.simulator.models.IBS.HasIBS;
import org.evoludo.simulator.models.Model.Type;
import org.evoludo.simulator.models.Model;
import org.evoludo.simulator.models.ODEEuler.HasODE;
import org.evoludo.simulator.models.PDERD.HasPDE;
import org.evoludo.simulator.models.SDEEuler.HasSDE;
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
public class Moran extends Discrete implements Module.Static,
		HasIBS, HasODE, HasSDE, HasPDE, HasPop2D.Strategy, HasPop3D.Strategy,
		HasMean.Strategy, HasPop2D.Fitness, HasPop3D.Fitness, HasMean.Fitness, HasHistogram.Fitness,
		HasHistogram.Degree, HasHistogram.StatisticsProbability, HasHistogram.StatisticsTime, HasConsole {

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
	 * @param engine the manager of modules and pacemeaker for running the model
	 */
	public Moran(EvoLudo engine) {
		super(engine);
	}

	@Override
	public void load() {
		super.load();
		nTraits = 2;
		// trait names
		String[] names = new String[nTraits];
		names[RESIDENT] = "Resident";
		names[MUTANT] = "Mutant";
		setTraitNames(names);
		// trait colors (automatically generates lighter versions for new strategists)
		Color[] colors = new Color[nTraits];
		colors[RESIDENT] = Color.BLUE;
		colors[MUTANT] = Color.RED;
		setTraitColors(colors);
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
	public String getKey() {
		return "Moran";
	}

	@Override
	public String getInfo() {
		return "Title: " + getTitle() + "\nAuthor: Christoph Hauert\nMoran process in structured population.";
	}

	@Override
	public String getTitle() {
		return "Moran process";
	}

	@Override
	public String getVersion() {
		return "v1.0 March 2021";
	}

	@Override
	public int getDependent() {
		return RESIDENT;
	}

	@Override
	public double getMinGameScore() {
		return Math.min(typeScores[RESIDENT], typeScores[MUTANT]);
	}

	@Override
	public double getMaxGameScore() {
		return Math.max(typeScores[RESIDENT], typeScores[MUTANT]);
	}

	@Override
	public double getMonoGameScore(int type) {
		return typeScores[type];
	}

	@Override
	public double[] getStaticScores() {
		return typeScores;
	}

	@Override
	public void avgScores(double[] density, int n, double[] avgscores) {
		avgscores[RESIDENT] = typeScores[RESIDENT];
		avgscores[MUTANT] = typeScores[MUTANT];
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

	/**
	 * Storage for the reference fixation probability based on analytical
	 * calculations.
	 */
	private double[][] statRefProb;

	/**
	 * Storage for the reference fixation time based on analytical calculations.
	 * <p>
	 * <strong>Note:</strong> {@code statRefTime} has three rows with conditional
	 * fixation times for a single mutant, a single 'resident' as well as the
	 * unconditional absorption time.
	 */
	private double[][] statRefTime;

	@Override
	public double[] getCustomLevels(Model.Data type, int trait) {
		// currently reference levels only available for Moran (birth-death) updates
		// in IBS models (otherwise ibs is null, see reset(Model)
		if (!model.isModelType(Type.IBS) || !((IBS) model).getSpecies(this).getPopulationUpdateType().isMoran())
			return null;
		// Note:
		// - return reference levels for fixation probabilities and times based
		// on analytical calculations for the Moran process
		switch (type) {
			case STATISTICS_FIXATION_PROBABILITY:
				return getReferenceProb(trait);
			case STATISTICS_FIXATION_TIME:
				return getReferenceTime(trait);
			default:
				return null;
		}
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
		if (statRefProb == null || statRefProb.length != nTraits)
			statRefProb = new double[nTraits][];
		if (statRefProb[trait] == null) {
			int m = (int) (init[MUTANT] * nPopulation);
			m = Math.min(Math.max(m, 1), nPopulation - 1);
			double rho = rhoA(m, nPopulation);
			statRefProb[MUTANT] = new double[] { rho };
			statRefProb[RESIDENT] = new double[] { 1.0 - rho };
		}
		return statRefProb[trait];
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
		if (statRefTime == null || statRefTime.length != nTraits + 1)
			statRefTime = new double[nTraits + 1][];
		if (statRefTime[trait] == null) {
			double rho = getReferenceProb(MUTANT)[0];
			int m = (int) (init[MUTANT] * nPopulation);
			m = Math.min(Math.max(m, 1), nPopulation - 1);
			double tai = tAi(m, nPopulation, rho, tA1(nPopulation)) / nPopulation;
			statRefTime[MUTANT] = new double[] { tai };
			double abs = ti(m, nPopulation, t1(rho, nPopulation)) / nPopulation;
			statRefTime[nTraits] = new double[] { abs };
			statRefTime[RESIDENT] = new double[] { abs - rho * tai / (1.0 - rho) };
		}
		return statRefTime[trait];
	}

	// fixation probability of m mutants in resident population of size n with
	// fitness ratio r (mutant/resident)
	// private double moranFixationProb(double r, int m, int n) {
	// double ir = 1.0/r;
	// return (1.0-ChHMath.pow(ir, m))/(1.0-ChHMath.pow(ir, n));
	// }

	/*
	 * command line parsing stuff
	 */
	public final CLOption cloFitness = new CLOption("fitness", "1,2", EvoLudo.catModule,
			"--fitness <r,m>  fitness of resident, mutant", new CLODelegate() {
				@Override
				public boolean parse(String arg) {
					double[] fit = CLOParser.parseVector(arg);
					if (fit.length != 2) {
						logger.warning("--fitness expects vector of length 2 (instead of '" + arg + "') - using '"
								+ cloFitness.getDefault() + "'");
						return false;
					}
					setFitness(fit);
					return true;
				}

				@Override
				public void report(PrintStream output) {
					output.println("# fitness:      " + Formatter.format(getFitness(), 4));
				}
			});

	@Override
	public void collectCLO(CLOParser parser) {
		super.collectCLO(parser);
		// prepare command line options
		parser.addCLO(cloFitness);
	}

	@Override
	public void adjustCLO(CLOParser parser) {
		parser.removeCLO(
				new String[] { "intertype", "numinter", "resetonchange", "accuscores", "geominter", "geomrepro" });
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

	// private double Tminus(int i) {
	// double fA = typeScores[MUTANT];
	// double fB = typeScores[RESIDENT];
	// int N = nPopulation;
	// return i*(N-i)*fB/(N*(i*fA+(N-i)*fB));
	// }

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

	// private double Toitar(int i) {
	// double fA = typeScores[MUTANT];
	// double fB = typeScores[RESIDENT];
	// return fA / fB;
	// }

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

	// private double rhoA() {
	// return rhoA(1);
	// }

	// private double rhoB(int i) {
	// return 1.0-rhoA(i);
	// }

	// private double rhoB() {
	// int N1 = nPopulation-1;
	// return rhoB(N1);
	// }

	/**
	 * The absorbtion time of a single mutant in a population of constant
	 * size {@code N} (and {@code N-1} residents).
	 * 
	 * @param rhoA the fixation probability of a single mutant
	 * @param n    the size of the population
	 * @return the absorbtion time
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
		return sum * rhoA;
	}

	/**
	 * The absorbtion time of {@code i} mutants in a population of constant
	 * size {@code N} (and {@code N-i} residents).
	 * 
	 * @param i  the number of mutants
	 * @param n  the size of the population
	 * @param t1 the absorbtion time of a single mutant
	 * @return the absorbtion time of {@code i} mutants
	 * 
	 * @see #t1(double, int)
	 */
	protected double ti(int i, int n, double t1) {
		int N = n;
		double sum1 = 0.0;
		for (int k = i; k < N; k++) {
			double prod = 1.0;
			for (int m = 1; m <= k; m++) {
				prod *= Tratio(m);
			}
			sum1 += prod;
		}
		sum1 *= -t1;
		double sum2 = 0.0;
		for (int k = i; k < N; k++) {
			double sumprod = 0.0;
			for (int l = 1; l <= k; l++) {
				double prod = 1.0;
				for (int m = l + 1; m <= k; m++) {
					prod *= Tratio(m);
				}
				sumprod += prod / Tplus(l, n);
			}
			sum2 += sumprod;
		}
		return sum1 + sum2;
	}

	// // requires rhoB
	// private double tN1(double rhoB) {
	// int N = nPopulation;
	// double sum = 0.0;
	// for( int l=1; l<N; l++ ) {
	// double sumprod = 0.0;
	// for( int k=l; k<N; k++ ) {
	// double prod = 1.0;
	// for( int m=l+1; m<=k; m++ ) {
	// prod *= Toitar(m);
	// }
	// sumprod += prod;
	// }
	// sum += sumprod/Tminus(l);
	// }
	// return sum*rhoB;
	// }

	/**
	 * The conditional fixation time of a single mutant in a population of fixed
	 * size {@code N} (and {@code N-1} residents).
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
		return sum;
	}

	/**
	 * The conditional fixation time of {@code i} mutants in a population of fixed
	 * size {@code N} (and {@code N-i} residents).
	 * 
	 * @param i    the number of mutants
	 * @param n    the size of the population
	 * @param rhoA the fixation probability of a single mutant
	 * @param tA1  the fixation time of a single mutant
	 * @return the conditional fixation time
	 */
	protected double tAi(int i, int n, double rhoA, double tA1) {
		int N = n;
		double sum1 = 0.0;
		for (int k = i; k < N; k++) {
			double prod = 1.0;
			for (int m = 1; m <= k; m++) {
				prod *= Tratio(m);
			}
			sum1 += prod;
		}
		double irhoAi = 1.0 / rhoA(i, n);
		sum1 *= -tA1 * rhoA * irhoAi;
		double sum2 = 0.0;
		for (int k = i; k < N; k++) {
			double sumprod = 0.0;
			for (int l = 1; l <= k; l++) {
				double prod = 1.0;
				for (int m = l + 1; m <= k; m++) {
					prod *= Tratio(m);
				}
				sumprod += prod / Tplus(l, n) * rhoA(l, n) * irhoAi;
			}
			sum2 += sumprod;
		}
		return sum1 + sum2;
	}

	// private double tBN1() {
	// int N = nPopulation;
	// double sum = 0.0;
	// for( int l=1; l<N; l++ ) {
	// double sumprod = 0.0;
	// for( int k=l; k<N; k++ ) {
	// double prod = 1.0;
	// for( int m=l+1; m<=k; m++ ) {
	// prod *= Toitar(N-m);
	// }
	// sumprod += prod;
	// }
	// sum += sumprod*rhoB(N-l)/Tminus(N-l);
	// }
	// return sum;
	// }

	// tBi() seems to be incorrect... calculate the simpler absorption time instead
	// and derive tBi that way...
	// // requires tBN1, rhoB
	// private double tBi(int i, double rhoB, double tBN1) {
	// int N = nPopulation;
	// double sum1 = 0.0;
	// for( int k=1; k<=i; k++ ) {
	// double prod = 1.0;
	// for( int m=1; m<=k; m++ ) {
	// prod *= Toitar(N-m);
	// }
	// sum1 += prod;
	// }
	// double irhoBi = 1.0/rhoB(i);
	// sum1 *= -tBN1*rhoB*irhoBi;
	// double sum2 = 0.0;
	// for( int k=1; k<=i; k++ ) {
	// double sumprod = 0.0;
	// for( int l=1; l<=k; l++ ) {
	// double prod = 1.0;
	// for( int m=l+1; m<=k; m++ ) {
	// prod *= Toitar(N-m);
	// }
	// sumprod += prod/Tminus(N-l)*rhoB(N-l)*irhoBi;
	// }
	// sum2 += sumprod;
	// }
	// return sum1+sum2;
	// }
}