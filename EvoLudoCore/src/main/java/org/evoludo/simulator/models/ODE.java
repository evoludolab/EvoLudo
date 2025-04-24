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

package org.evoludo.simulator.models;

import java.io.PrintStream;
import java.util.Arrays;
import java.util.List;

import org.evoludo.math.ArrayMath;
import org.evoludo.math.Functions;
import org.evoludo.simulator.ColorMap;
import org.evoludo.simulator.EvoLudo;
import org.evoludo.simulator.modules.Features.Payoffs;
import org.evoludo.simulator.modules.Map2Fitness;
import org.evoludo.simulator.modules.Module;
import org.evoludo.simulator.modules.Mutation;
import org.evoludo.simulator.modules.PlayerUpdate;
import org.evoludo.util.CLOParser;
import org.evoludo.util.CLOption;
import org.evoludo.util.CLOption.CLODelegate;
import org.evoludo.util.Formatter;
import org.evoludo.util.Plist;

/**
 * Common base class for all differential equations models. Provides the basic
 * Euler method for the numerical integration of systems of ordinary
 * differential equations.
 * <p>
 * <strong>Note:</strong> Currently differential equation models are restricted
 * to sets of discrete traits.
 *
 * @author Christoph Hauert
 */
public class ODE extends Model implements Discrete {

	/**
	 * Methods that every {@link Module} must implement, which advertises numerical
	 * solutions based on differential equations.
	 */
	public abstract interface HasDE {

		/**
		 * For replicator dynamics the frequencies of all traits must sum up to one.
		 * Hence, for <code>nTraits</code> traits there are only
		 * <code>nTraits-1</code> degrees of freedom. The index returned by
		 * <code>getDependent()</code> marks the one that is derived from the others.
		 * <p>
		 * <strong>Notes:</strong>
		 * <ul>
		 * <li>Dependent traits are used by replicator type models where the frequencies
		 * of all types must sum up to one. Currently only used by Discrete modules.
		 * <li>Density modules do not have dependent traits.
		 * </ul>
		 * 
		 * @return the index of the dependent trait
		 */
		public default int getDependent() {
			return -1;
		}

		/**
		 * Calculate the average payoff/score for the frequency of traits specified in
		 * the array <code>density</code> for interactions in groups of size
		 * <code>n</code>. The average payoffs/scores for each of the
		 * <code>nTraits</code> traits must be stored and returned in the array
		 * <code>avgscores</code>.
		 * <p>
		 * <strong>Note:</strong> needs to be thread safe for parallel processing of
		 * PDE's.
		 * <p>
		 * <strong>IMPORTANT:</strong> one of
		 * <ul>
		 * <li><code>{@link #avgScores(double[], int, double[])}</code>,
		 * <li><code>{@link #avgScores(double[], int, double[], int)}</code>, or
		 * </ul>
		 * should be implemented in modules that advertise the model types
		 * <code>ODE, SDE</code> or <code>PDE</code>.
		 * <p>
		 * Alternatively, the method
		 * {@link ODE#getDerivatives(double, double[], double[], double[])} may be
		 * overridden in a subclass of {@code ODE}, which may prevent calls to
		 * {@code avgScores(...)} altogether.
		 *
		 * @param density   the frequency/density of each trait
		 * @param n         the size of interaction groups
		 * @param avgscores the array for storing the average payoffs/scores of each
		 *                  trait
		 */
		public default void avgScores(double[] density, int n, double[] avgscores) {
			avgScores(density, n, avgscores, 0);
		}

		/**
		 * Calculate the average payoff/score for the frequency of traits specified in
		 * the array <code>density</code> for interactions in groups of size
		 * <code>n</code> in <em>multi-species interactions</em>. The state of the
		 * current species starts at index <code>skip</code> and the average
		 * payoffs/scores for each of its <code>nTraits</code> traits must be stored and
		 * returned in the array <code>avgscores</code> starting at index
		 * <code>skip</code>.
		 * <p>
		 * <strong>Note:</strong> needs to be thread safe for parallel processing of
		 * PDE's.
		 * <p>
		 * <strong>IMPORTANT:</strong> one of
		 * <ul>
		 * <li><code>{@link #avgScores(double[], int, double[])}</code>,
		 * <li><code>{@link #avgScores(double[], int, double[], int)}</code>, or
		 * </ul>
		 * should be implemented in modules that advertise the model types
		 * <code>ODE, SDE</code> or <code>PDE</code>.
		 * <p>
		 * Alternatively, the method
		 * {@link ODE#getDerivatives(double, double[], double[], double[])} may be
		 * overridden in a subclass of {@code ODE}, which may prevent calls to
		 * {@code avgScores(...)} altogether.
		 *
		 * 
		 * @param density   the frequency/density of each trait
		 * @param n         the size of interaction groups
		 * @param avgscores the array for storing the average payoffs/scores of each
		 *                  trait
		 * @param skip      the entries to skip in arrays <code>density</code> and
		 *                  <code>avgscores</code>
		 */
		public default void avgScores(double[] density, int n, double[] avgscores, int skip) {
			if (skip == 0) {
				avgScores(density, n, avgscores);
				return;
			}
			throw new Error("avgScores for multi-species interactions not implemented!");
		}
	}

	/**
	 * Additional methods that must be implemented by {@link Module}s that advertise
	 * numerical solutions based on <em>ordinary</em> differential equations.
	 */
	public interface HasODE extends HasDE {

		/**
		 * Provides opportunity for model to supply custom ODE implementation.
		 * <p>
		 * <strong>Important:</strong> if the custom ODE implementation involves random
		 * numbers, the shared random number generator should be used for
		 * reproducibility
		 *
		 * @return the custom ODE model or <code>null</code> to use use default
		 *
		 * @see EvoLudo#getRNG()
		 */
		public default Model createODE() {
			return null;
		}
	}

	/**
	 * Discretization of time increment for continuous time models. This is the
	 * attempted size of next step to take.
	 * <p>
	 * <strong>Important:</strong> always positive regardless of direction of
	 * integration.
	 * 
	 * @see #dtTaken
	 * @see #forward
	 * @see #cloDEdt
	 * @see #setDt(double)
	 */
	double dt;

	/**
	 * The attempted size of next step to take. In {@code ODE} this is always
	 * equal to {@code dt} but for more sophisticated integrators, {@link RungeKutta} for
	 * example, the attempted step size may get adjusted.
	 * <p>
	 * <strong>Important:</strong> always positive regardless of direction of
	 * integration.
	 * 
	 * @see #next()
	 * @see #deStep(double)
	 */
	double dtTry;

	/**
	 * The size of step taken by integrator. This can be less than the step size
	 * attempted, <code>dtTaken &le; dtTry</code>.
	 * <p>
	 * <strong>Important:</strong> always positive regardless of direction of
	 * integration.
	 * 
	 * @see #deStep(double)
	 */
	double dtTaken;

	/**
	 * The flag is <code>true</code> if the numerical integration moves forward in
	 * time.
	 * 
	 * @see #setTimeReversed(boolean)
	 */
	boolean forward = true;

	/**
	 * Total number of traits (dynamical variables) in the ODE.
	 */
	int nDim = -1;

	/**
	 * The initial frequencies/densities of the ODE. In multi-species modules the
	 * initial states of each species are concatenated into this single array. The
	 * initial frequencies/densities of the first species are stored in
	 * <code>yt[0]</code> through <code>yt[n1]</code> where <code>n1</code> denotes
	 * the number of traits in the first species. The initial frequencies/densities
	 * of the second species are stored in <code>yt[n1+1]</code> through
	 * <code>yt[n1+n2]</code> where <code>n2</code> denotes the number of traits in
	 * the second species, etc.
	 */
	double[] y0;

	/**
	 * The current frequencies/densities of the ODE. In multi-species modules the
	 * current states of each species are concatenated into this single array. The
	 * current frequencies/densities of the first species are stored in
	 * <code>yt[0]</code> through <code>yt[n1]</code> where <code>n1</code> denotes
	 * the number of traits in the first species. The current frequencies/densities
	 * of the second species are stored in <code>yt[n1+1]</code> through
	 * <code>yt[n1+n2]</code> where <code>n2</code> denotes the number of traits in
	 * the second species, etc.
	 */
	double[] yt;

	/**
	 * The current fitness of all traits in the ODE. In multi-species modules the
	 * current fitness of each species are concatenated into this single array. The
	 * current fitness of the first species are stored in <code>ft[0]</code> through
	 * <code>ft[n1]</code> where <code>n1</code> denotes the number of traits in the
	 * first species. The current fitness of the second species are stored in
	 * <code>ft[n1+1]</code> through <code>ft[n1+n2]</code> where <code>n2</code>
	 * denotes the number of traits in the second species, etc.
	 */
	double[] ft;

	/**
	 * The frequency/density increments of the ODE between the current state
	 * {@link #yt} and the previous state {@link #yout}. In multi-species modules
	 * the increments of each species are concatenated into this single array. The
	 * increments in the first species are stored in <code>dyt[0]</code> through
	 * <code>dyt[n1]</code> where <code>n1</code> denotes the number of traits in
	 * the first species. The increments in the second species are stored in
	 * <code>dyt[n1+1]</code> through <code>dyt[n1+n2]</code> where <code>n2</code>
	 * denotes the number of traits in the second species, etc.
	 */
	double[] dyt;

	/**
	 * The previous frequencies/densities of the ODE. In multi-species modules the
	 * previous states of each species are concatenated into this single array. The
	 * previous frequencies/densities of the first species are stored in
	 * <code>yout[0]</code> through <code>yout[n1]</code> where <code>n1</code>
	 * denotes the number of traits in the first species. The current
	 * frequencies/densities of the second species are stored in
	 * <code>yout[n1+1]</code> through <code>yout[n1+n2]</code> where
	 * <code>n2</code> denotes the number of traits in the second species, etc.
	 */
	double[] yout;

	/**
	 * Pointer to temporarily store a reference to the current state
	 * <code>yt</code>. This affects only multi-species modules because fitness
	 * calculations for the derivatives may need access to the state of the other
	 * populations. To allow this {@link #getMeanTraits(int, double[])} uses
	 * <code>dstate</code> while the derivative calculations are in progress
	 * (<code>dstate != null</code>). Otherwise <code>yt</code> is used.
	 */
	double[] dstate;

	/**
	 * For {@link Module}s with static fitness, e.g. the original Moran process
	 * {@link org.evoludo.simulator.modules.Moran}, this array stores the fitness of
	 * all types for efficiency reasons. In multi-species modules the static fitness
	 * of each species are concatenated into this single array. The fitness of the
	 * first species are stored in <code>staticfit[0]</code> through
	 * <code>staticfit[n1]</code> where <code>n1</code> denotes the number of traits
	 * in the first species. The static fitness of the second species are stored in
	 * <code>staticfit[n1+1]</code> through <code>staticfit[n1+n2]</code> where
	 * <code>n2</code> denotes the number of traits in the second species, etc. If
	 * only some species have static fitness only those are stored here. All other
	 * entries in the array are unused but the size remains unchanged and is always
	 * the same as e.g. {@link #yt} or <code>null</code> if no species has static
	 * fitness.
	 */
	double[] staticfit;

	/**
	 * Type of initial configuration for each species.
	 * 
	 * @see #cloInit
	 */
	protected InitType initType[];

	/**
	 * Array containing indices that delimit individual species in the ODE state
	 * variables. In a module with <code>N</code> species the array has
	 * <code>N+1</code> entries. For example, for a single species module with
	 * <code>n1</code> traits <code>idxSpecies = int[] { 0, n1 }</code> holds,
	 * whereas for a two species module with <code>n1</code> and <code>n2</code>
	 * traits, respectively, <code>idxSpecies = int[] { 0, n1, n1+n2 }</code> holds,
	 * and for three species
	 * <code>idxSpecies = int[] { 0, n1, n1+n2, n1+n2+n3 }</code> etc. In other
	 * words, the first entry is always zero and the last always contains the total
	 * number of dynamical variables in this ODE, <code>idxSpecies[N]==nDim</code>.
	 * This means that, e.g. the frequencies/densities referring to species
	 * <code>i</code> are stored in <code>yt[idxSpecies[i]]</code> through
	 * <code>yt[idxSpecies[i+1]]</code>.
	 *
	 * @see #nDim
	 */
	int[] idxSpecies;

	/**
	 * Array containing the update rates for each species. Determines the relative
	 * update rate between species, Only relevant in multi-species modules.
	 * 
	 * @see Module#getSpeciesUpdateRate()
	 */
	double[] rates;

	/**
	 * Array containing the mutation rates/probabilities for each species.
	 * 
	 * @see Module#getMutation()
	 */
	Mutation.Discrete[] mutation;

	/**
	 * Array containing the indices of the dependent trait in each species.
	 * Dependent traits make only sense in replicator type model with frequencies,
	 * i.e. normalized states. The dependent trait is the one that gets derived from
	 * the changes in all others in order to maintain normalization.
	 * 
	 * @see HasODE#getDependent()
	 */
	int[] dependents;

	/**
	 * Convenience variable to indicate whether the model is based on densities.
	 * This is {@code true} if none of the species has a dependent trait.
	 */
	boolean isDensity = false;

	/**
	 * Array containing the inverse of the fitness range:
	 * <code>1.0/(maxFitness - minFitness)</code> for each species. This is used to
	 * normalize imitation rules of players.
	 * 
	 * @see PlayerUpdate.Type#IMITATE
	 */
	double[] invFitRange;

	/**
	 * The names of the traits (dynamical variables) in the ODE. In multi-species
	 * modules all names are concatenated into this single array. The names of the
	 * first species are stored in <code>names[0]</code> through
	 * <code>names[n1]</code> where <code>n1</code> denotes the number of traits in
	 * the first species. The names of the second species are stored in
	 * <code>names[n1+1]</code> through <code>names[n1+n2]</code> where
	 * <code>n2</code> denotes the number of traits in the second species, etc.
	 */
	String[] names;

	/**
	 * Set to <code>true</code> to requests that the numerical integration stops
	 * once the population reaches a monomorphic state.
	 *
	 * @see org.evoludo.simulator.modules.Discrete#cloMonoStop
	 * @see org.evoludo.simulator.modules.Discrete#getMonoStop()
	 *      Discrete.getMonoStop()
	 * @see #isMonomorphic()
	 * @see #checkConvergence(double)
	 * @see #setAccuracy(double)
	 */
	protected boolean monoStop = false;

	/**
	 * <code>true</code> if the adjusted replicator dynamics should be applied.
	 * Compared to the standard replicator equation nothing changes in terms of
	 * trajectories, location of fixed points or their stability. However, the speed
	 * of evolutionary changes depend inversely proportional on the overall fitness
	 * of the population(s) such that changes happen rapidly in a population with
	 * low fitness but slowly in populations with high fitness. The adjusted
	 * replicator dynamics requires that all payoffs are positive.
	 * 
	 * @see #setAdjustedDynamics(boolean)
	 * @see #cloAdjustedDynamics
	 */
	boolean isAdjustedDynamics = false;

	/**
	 * Desired accuracy to determine whether numerical integration has converged or
	 * a population has become monomorphic.
	 * 
	 * @see #cloDEAccuracy
	 * @see #setAccuracy(double)
	 * @see #hasConverged()
	 * @see #isMonomorphic()
	 */
	double accuracy = 1e-4;

	// REVIEW
	// - implement/check disabled traits
	// - review/check calculations for static and ecological cases as well as
	// mutations
	// - check convergence in PDE (advection example seems to stop at fairly high
	// densities)

	/**
	 * Constructs a new model for the numerical integration of the system of
	 * ordinary differential equations representing the dynamics specified by the
	 * {@link Module} <code>module</code> using the {@link EvoLudo} pacemaker
	 * <code>engine</code> to control the numerical evaluations. The integrator
	 * implements Euler's method (fixed step size).
	 * 
	 * @param engine the pacemaker for running the model
	 */
	public ODE(EvoLudo engine) {
		super(engine);
		type = Type.ODE;
	}

	@Override
	public void load() {
		super.load();
		initType = new InitType[nSpecies];
		mutation = new Mutation.Discrete[nSpecies];
		dependents = new int[nSpecies];
		int idx = 0;
		for (Module mod : species) {
			mutation[idx] = (Mutation.Discrete) mod.getMutation();
			dependents[idx] = (mod instanceof HasDE ? ((HasDE) mod).getDependent() : -1);
			idx++;
		}
		isDensity = ArrayMath.max(dependents) < 0;
	}

	@Override
	public void unload() {
		initType = null;
		mutation = null;
		dependents = null;
		yt = ft = dyt = yout = null;
		staticfit = null;
		names = null;
		rates = null;
		invFitRange = null;
		idxSpecies = null;
		cloInit.clearKeys();
		super.unload();
	}

	@Override
	public boolean check() {
		boolean doReset = super.check();
		dstate = null;
		double minFit = Double.MAX_VALUE;
		double maxFit = -Double.MAX_VALUE;
		rates = new double[nSpecies];
		invFitRange = null;
		idxSpecies = new int[nSpecies + 1];
		nDim = 0;
		int idx = 0;
		for (Module mod : species) {
			doReset |= mod.check();
			int nTraits = mod.getNTraits();
			idxSpecies[idx] = nDim;
			rates[idx] = mod.getSpeciesUpdateRate();
			if (mod instanceof Payoffs) {
				if (invFitRange == null || invFitRange.length != nSpecies) {
					invFitRange = new double[nSpecies];
					Arrays.fill(invFitRange, 1.0);
				}
				Map2Fitness map2fit = mod.getMapToFitness();
				Payoffs pmod = (Payoffs) mod;
				minFit = map2fit.map(pmod.getMinPayoff());
				maxFit = map2fit.map(pmod.getMaxPayoff());
				if (maxFit > minFit)
					invFitRange[idx] = 1.0 / (maxFit - minFit);
			}
			idx++;
			nDim += nTraits;
		}
		idxSpecies[nSpecies] = nDim;
		if (yt == null || yt.length != nDim) {
			yt = new double[nDim];
			dyt = new double[nDim];
			yout = new double[nDim];
			ft = new double[nDim];
		}
		names = getMeanNames();

		if (isAdjustedDynamics && minFit <= 0.0) {
			// fitness is not guaranteed to be positive
			logger.warning(getClass().getSimpleName()
					+ " - fitness >0 must hold for adjusted dynamics (revert to standard dynamics).");
			isAdjustedDynamics = false;
		}
		return doReset;
	}

	@Override
	public void reset() {
		int skip = 0;
		staticfit = null;
		for (Module pop : species) {
			if (!pop.isStatic())
				continue;
			// allocate memory after the first population with static fitness is found
			if (staticfit == null || staticfit.length != nDim)
				staticfit = new double[nDim];
			int nTraits = pop.getNTraits();
			System.arraycopy(((Module.Static) pop).getStaticScores(), 0, staticfit, skip,
					nTraits);
			Map2Fitness map2fit = pop.getMapToFitness();
			for (int n = 0; n < nTraits; n++)
				staticfit[skip + n] = map2fit.map(staticfit[skip + n]);
			skip += nTraits;
		}
		normalizeState(y0);
		// check if stop is requested if population becomes monomorphic
		// for multi-species models only first species checked
		// cast is safe because ODEs only avaliable for Discrete modules
		monoStop = ((org.evoludo.simulator.modules.Discrete) species.get(0)).getMonoStop();
	}

	@Override
	public void init() {
		init(true);
	}

	/**
	 * Sets the discretization of time increments in continuous time models.
	 * <p>
	 * <strong>Note:</strong> Some models may need to adjust, i.e. reduce,
	 * <code>deltat</code> (see {@link PDE#checkDt()}) or choose a variable step
	 * size in which case <code>deltat</code> is ignored (see
	 * {@link RungeKutta#getAutoDt()}).
	 *
	 * @param deltat the time increments in continuous time models.
	 */
	public void setDt(double deltat) {
		deltat = Math.max(0.0, deltat);
		if (deltat == 0.0) {
			logger.warning(getClass().getSimpleName() + " - time step must be >0 (dt=" + Formatter.formatSci(dt, 5)
					+ " kept; requested dt=" + Formatter.formatSci(deltat, 5) + " ignored).");
			return;
		}
		dt = deltat;
	}

	/**
	 * Gets the discretization of time increments in continuous time models.
	 * <p>
	 * <strong>Note:</strong> This may be different from <code>dt</code> set through
	 * {@link #setDt(double)} if the model required adjustments.
	 *
	 * @return the time increment in continuous time models.
	 */
	public double getDt() {
		return dt;
	}

	@Override
	public boolean setInitialTraits(double[] init) {
		if (init.length != nDim)
			return false;
		System.arraycopy(init, 0, y0, 0, nDim);
		return true;
	}

	@Override
	public void getInitialTraits(double[] init) {
		System.arraycopy(y0, 0, init, 0, nDim);
	}

	@Override
	public boolean setInitialTraits(int id, double[] init) {
		int start = idxSpecies[id];
		if (init.length != idxSpecies[id + 1] - start)
			return false;
		System.arraycopy(init, start, y0, 0, idxSpecies[id + 1] - start);
		init(false);
		return true;
	}

	@Override
	public void getInitialTraits(int id, double[] init) {
		int start = idxSpecies[id];
		System.arraycopy(y0, start, init, 0, idxSpecies[id + 1] - start);
	}

	@Override
	public double getMonoScore(int id, int idx) {
		org.evoludo.simulator.modules.Discrete module = (org.evoludo.simulator.modules.Discrete) species.get(id);
		return module.getMonoPayoff(idx % module.getNTraits());
	}

	@Override
	public int getNMean() {
		return nDim;
	}

	@Override
	public int getNMean(int id) {
		return idxSpecies[id + 1] - idxSpecies[id];
	}

	@Override
	public boolean getMeanTraits(int id, double[] mean) {
		double[] state = (dstate == null ? yt : dstate);
		int start = idxSpecies[id];
		System.arraycopy(state, start, mean, 0, idxSpecies[id + 1] - start);
		return connect;
	}

	@Override
	public boolean getMeanTraits(double[] mean) {
		double[] state = (dstate == null ? yt : dstate);
		System.arraycopy(state, 0, mean, 0, nDim);
		return connect;
	}

	/**
	 * Unused interface method.
	 */
	@Override
	public <T> void getTraitData(int id, T[] colors, ColorMap<T> colorMap) {
		// not applicable
	}

	@Override
	public double getMinScore(int id) {
		return ((Payoffs) getSpecies(id)).getMinPayoff();
	}

	@Override
	public double getMaxScore(int id) {
		return ((Payoffs) getSpecies(id)).getMaxPayoff();
	}

	@Override
	public double getMinFitness(int id) {
		Module mod = getSpecies(id);
		Map2Fitness map2fit = mod.getMapToFitness();
		return map2fit.map(((Payoffs) mod).getMinPayoff());
	}

	@Override
	public double getMaxFitness(int id) {
		Module mod = getSpecies(id);
		Map2Fitness map2fit = mod.getMapToFitness();
		return map2fit.map(((Payoffs) mod).getMaxPayoff());
	}

	@Override
	public boolean getMeanFitness(int id, double[] mean) {
		double sum = 0.0;
		int start = idxSpecies[id];
		int end = idxSpecies[id + 1];
		for (int n = start; n < end; n++) {
			double ftn = ft[n];
			mean[n - start] = ftn;
			sum += ftn * yt[n];
		}
		mean[end - start] = sum;
		return connect;
	}

	@Override
	public boolean getMeanFitness(double[] mean) {
		System.arraycopy(ft, 0, mean, 0, nDim);
		return connect;
	}

	@Override
	public <T> void getFitnessData(int id, T[] colors, ColorMap.Gradient1D<T> colorMap) {
		// not applicable
	}

	@Override
	public void getFitnessHistogramData(int id, double[][] bins) {
		// tmp variables
		int nBins = bins[0].length;
		int maxBin = nBins - 1;
		// for neutral selection maxScore==minScore! assume range [score-1, score+1]
		// needs to be synchronized with GUI (e.g. MVFitness, MVFitHistogram, ...)
		Module mod = species.get(id);
		Map2Fitness map2fit = mod.getMapToFitness();
		Payoffs pmod = (Payoffs) mod;
		double minFit = map2fit.map(pmod.getMinPayoff());
		double maxFit = map2fit.map(pmod.getMaxPayoff());
		double map;
		if (maxFit - minFit < 1e-8) {
			// close enough to neutral
			map = nBins * 0.5;
			minFit--;
		} else
			map = nBins / (maxFit - minFit);
		int idx = 0;
		int vacant = mod.getVacant();
		// fill bins
		int offset = idxSpecies[id];
		int dim = idxSpecies[id + 1] - offset;
		for (int n = 0; n < dim; n++) {
			if (n == vacant)
				continue;
			Arrays.fill(bins[idx], 0.0);
			int bin = (int) ((ft[offset + idx] - minFit) * map);
			bin = Math.max(0, Math.min(maxBin, bin));
			bins[idx][bin] = yt[offset + idx];
			idx++;
		}
	}

	/**
	 * {@inheritDoc}
	 * 
	 * <h3>Implementation Notes:</h3> Before:
	 * <ol>
	 * <li><code>yt</code> current state
	 * <li><code>ft</code> current fitness
	 * <li><code>dyt</code> current derivatives fitness
	 * <li><code>dtTry</code> size of time step attempting to make
	 * </ol>
	 * After:
	 * <ol>
	 * <li><code>yt</code> next state
	 * <li><code>ft</code> next fitness
	 * <li><code>dyt</code> next derivatives
	 * <li><code>yout</code> contains previous state (i.e. <code>yt</code> when
	 * entering method)
	 * <li><code>stepTaken</code> the time between <code>yt</code> and
	 * <code>yout</code>
	 * </ol>
	 */
	@Override
	public boolean next() {
		if (converged)
			return false;
		connect = true;
		double nextHalt = getNextHalt();
		// continue if milestone reached in previous step, i.e. deltat < 1e-8
		double step = timeStep;
		double deltat = Math.abs(nextHalt - time);
		if (deltat >= 1e-8)
			step = Math.min(step, deltat);
		// NOTE: dt is always >0
		double elapsed = 0.0;
		double timeBefore = time;
		while (Math.abs(step - elapsed) > 1e-8) {
			double nextstep = Math.min(dtTry, step - elapsed);
			double d2 = deStep(forward ? nextstep : -nextstep);
			elapsed += dtTaken;
			converged = checkConvergence(d2);
			if (converged)
				break;
		}
		// try to minimize rounding errors
		time = timeBefore + (forward ? elapsed : -elapsed);
		if (Math.abs(nextHalt - time) < 1e-8)
			return false;
		return !converged;
	}

	/**
	 * Helper method to check whether the squared distance <code>dist2</code>
	 * qualifies to signal convergence.
	 * 
	 * <h3>Implementation Notes:</h3>
	 * <ol>
	 * <li>ODE's can converge even with non-zero mutation rates
	 * <li>step size may keep decreasing without ever reaching the accuracy
	 * threshold (or reach step). This scenario requires an emergency brake
	 * (otherwise the browser becomes completely unresponsive - nasty!). Emergency
	 * brake triggered if <code>stepTaken</code> is so small that more than
	 * <code>1/(accuracy * stepTaken)</code> updates are required to complete
	 * <code>dt</code>.
	 * <li><code>stepTaken &lt; 0</code> may hold but irrelevant because only the
	 * squared value is used.
	 * </ol>
	 * 
	 * @param dist2 the squared distance between the current and previous states
	 *              <code>(y[t+dt]-y[t])<sup>2</sup></code>.
	 * @return <code>true</code> if convergence criteria passed
	 */
	protected boolean checkConvergence(double dist2) {
		if (converged)
			return true;
		double acc = accuracy * dtTaken;
		converged = (dist2 < acc * acc);
		if (!converged && monoStop)
			converged = isMonomorphic();
		return converged;
	}

	/**
	 * Check if population is monomorphic. Note, in multi-species modules all
	 * species need to be monomorphic.
	 * 
	 * @return <code>true</code> if criteria for monomorphic state passed
	 */
	public boolean isMonomorphic() {
		int idx = 0;
		int from = 0;
		for (Module pop : species) {
			int to = from + pop.getNTraits();
			if (!isMonomorphic(from, to, dependents[idx], pop.VACANT))
				return false;
			from = to;
			idx++;
		}
		return true;
	}

	/**
	 * Helper method to check if the species with trait indices {@code start}
	 * through {@code stop} is monomorphic. The species may have dependent trait
	 * {@code dep} and allow for vacant sites (with index {@code vac}).
	 * 
	 * @param start the start index of the species
	 * @param stop  the stop index of the species
	 * @param dep   the index of the dependent trait
	 * @param vac   the index of the vacant trait
	 * @return <code>true</code> if the species is monomorphic
	 */
	private boolean isMonomorphic(int start, int stop, int dep, int vac) {
		boolean mono = false;
		for (int n = start; n < stop; n++) {
			if (n == dep || n == vac || yt[n] < accuracy)
				continue;
			if (mono)
				return false;
			mono = true;
		}
		return true;
	}

	/**
	 * Attempts a numerical integration step of size <code>step</code>. The baseline
	 * are steps of fixed size following Euler's method.
	 *
	 * @param step the time step to attempt
	 * @return squared distance between this state and previous one,
	 *         <code>(yt-yout)<sup>2</sup></code>.
	 */
	protected double deStep(double step) {
		ArrayMath.addscale(yt, dyt, step, yout); // yout = yt+step*dyt
		// polish result
		int idx = ArrayMath.minIndex(yout);
		if (yout[idx] < 0.0) {
			// step too big, resulted in negative densities/frequencies
			// note, yt[idx]>0 must hold (from previous step) but
			// sign of dyt[idx] depends on direction of integration
			step = -yt[idx] / dyt[idx];
			// ensure all frequencies are positive - despite roundoff errors
			for (int i = 0; i < nDim; i++)
				yout[i] = Math.max(0.0, yt[i] + step * dyt[i]);
			yout[idx] = 0.0; // avoid roundoff errors
		}
		normalizeState(yt);
		// the new state is in yout - swap and determine new fitness
		double[] swap = yt;
		yt = yout;
		yout = swap;
		// determine fitness of new state
		getDerivatives(time, yt, ft, dyt);
		time += step;
		dtTaken = Math.abs(step);
		return ArrayMath.distSq(yout, yt);
	}

	/**
	 * Convenience method to normalize state in frequency based models. For density
	 * models nothing needs to be done.
	 *
	 * @param state the array that needs to be normalized if appropriate
	 */
	protected void normalizeState(double[] state) {
		if (isDensity)
			return;
		// multi-species: normalize sections
		int idx = 0;
		int from = 0;
		for (Module pop : species) {
			int to = from + pop.getNTraits();
			if (dependents[idx++] >= 0)
				ArrayMath.normalize(state, from, to);
			from = to;
		}
	}

	/**
	 * Calculate the rates of change for all species in {@code state} at
	 * {@code time} given the fitness {@code fitness} and returned in
	 * {@code change}. For replicator models the dynamics depends on the selected
	 * type of player updating, see {@link PlayerUpdate.Type}, while for modules
	 * with variable population sizes (density based or with vaccant 'space'
	 * (reproductive opportunities)) the fitness denotes the rate of reproduction
	 * moderated by the available 'space'.
	 * <p>
	 * <strong>IMPORTANT:</strong> parallel processing for PDE's in JRE requires
	 * this method to be thread safe.
	 * <p>
	 * <strong>Note:</strong> adding <code>synchronized</code> to
	 * this method would seem to make sense but this results in a deadlock for
	 * multiple threads in PDE's and JRE. However, apparently not needed as
	 * javascript and java yield the exact same results (after 15k generations!)
	 *
	 * @param t       the time at which to calculate the derivative
	 * @param state   the array of frequencies/densities denoting the state
	 *                population
	 * @param fitness the array of fitness values of types in population
	 * @param change  the array to return the rate of change of each type in the
	 *                population
	 * 
	 * @see #updateBest(Module, double[], double[], int, int, double[])
	 * @see #updateBestResponse(Module, double[], double[], int, int, double[])
	 * @see #updateEcology(Module, double[], double[], int, int, double[])
	 * @see #updateImitate(Module, double[], double[], int, int, double[])
	 * @see #updateThermal(Module, double[], double[], int, int, double[])
	 */
	protected void getDerivatives(double t, double[] state, double[] fitness, double[] change) {
		dstate = state;
		int index = 0;
		for (Module mod : species) {
			int nGroup = mod.getNGroup();
			int nTraits = mod.getNTraits();
			int skip = idxSpecies[index];
			if (mod.isStatic()) {
				System.arraycopy(staticfit, skip, fitness, skip, nTraits);
			} else {
				((HasODE) mod).avgScores(state, nGroup, fitness, skip);
				Map2Fitness map2fit = mod.getMapToFitness();
				for (int n = skip; n < skip + nTraits; n++)
					fitness[n] = map2fit.map(fitness[n]);
			}
			if (mod.getVacant() >= 0) {
				updateEcology(mod, state, fitness, nGroup, index, change);
				continue;
			}

			double err = 0.0;
			PlayerUpdate.Type put = mod.getPlayerUpdate().getType();
			switch (put) {
				case THERMAL: // fermi update
					err = updateThermal(mod, state, fitness, nGroup, index, change);
					break;

				case IMITATE: // same as IMITATE_BETTER in continuum limit
					err = updateImitate(mod, state, fitness, nGroup, index, change);
					break;

				case IMITATE_BETTER: // replicator update
					err = updateImitateBetter(mod, state, fitness, nGroup, index, change);
					break;

				case BEST: // imitate the better
				case BEST_RANDOM: // same as BEST in continuum limit
					err = updateBest(mod, state, fitness, nGroup, index, change);
					break;

				case BEST_RESPONSE: // best-response update
					err = updateBestResponse(mod, state, fitness, nGroup, index, change);
					break;

				case PROPORTIONAL: // proportional update
					err = updateProportional(mod, state, fitness, nGroup, index, change);
					break;

				default:
					throw new Error("Unknown update method for players (" + put + ")");
			}

			// restrict to active trait
			// note float resolution is 1.1920929E-7
			if (Math.abs(err) > 1e-7 * mod.getNActive()) {
				boolean[] active = mod.getActiveTraits();
				for (int n = 0; n < nTraits; n++)
					if (active[n])
						change[skip + n] -= err;
			}
			index++;
		}
		dstate = null;
		index = 0;
		int from = 0;
		for (Module pop : species) {
			int dim = pop.getNTraits();
			int to = from + dim;
			double rate = rates[index];
			if (isAdjustedDynamics) {
				double fbar = 0.0;
				for (int i = from; i < to; i++)
					fbar += state[i] * fitness[i];
				rate /= fbar;
			}
			for (int i = from; i < to; i++)
				change[i] *= rate;
			mutation[index].mutate(state, change, from, to);
			from = to;
			index++;
		}
	}

	/**
	 * Implementation of the player updates for modules with populations of variable
	 * size (density based or with vacant 'space', i.e.
	 * reproductive opportunities). In models with varying reproductive
	 * opportunities the rate of reproduction is given by the product of the
	 * individual's fitness moderated by the available 'space'.
	 * This results in the dynamical equation given by
	 * \[\dot x = x_i (z\cdot f_i - d)\]
	 * where \(z\) denotes vacant space, \(f_i\) the fitness of type \(i\) and \(d\)
	 * is the death rate. This works nicely for replicator type equations but may
	 * not fit more general dynamical equations.
	 * 
	 * @param mod     the module representing the current species
	 * @param state   array of frequencies/densities denoting the state population
	 * @param fitness array of fitness values of types in population
	 * @param nGroup  the interaction group size
	 * @param index   the index of the module <code>mod</code> in multi-species
	 *                modules
	 * @param change  array to return the rate of change of each type in the
	 *                population
	 * @return the total change (should be zero in theory)
	 */
	protected double updateEcology(Module mod, double[] state, double[] fitness, int nGroup, int index,
			double[] change) {
		// XXX what happens if one trait is deactivated!?
		int vacant = mod.getVacant();
		double err = 0.0;
		int skip = idxSpecies[index];
		int end = skip + mod.getNTraits();
		double z = state[vacant];
		double dz = 0.0;
		double drate = mod.getDeathRate();
		for (int n = skip; n < end; n++) {
			if (n == vacant) {
				fitness[n] = 0.0;
				continue;
			}
			double dyn = state[n] * (z * fitness[n] - drate);
			change[n] = dyn;
			dz -= dyn;
			err += dyn;
		}
		change[vacant] = dz;
		err += dz;
		return err;
	}

	/**
	 * Implementation of the player update {@link PlayerUpdate.Type#THERMAL}. This
	 * calculates the rates of change for each type in species <code>mod</code> for
	 * the popular choice for 'pairwise comparisons' where the focal player \(i\)
	 * and one of its neighbours \(j\) are randomly chosen. The focal player \(i\)
	 * adopts the trait of player \(j\) with a probability
	 * \[p_{i\to j}=\frac1{1+\exp[w(f_i-f_j)]},\]
	 * where \(w\geq 0\) denotes the selection strength and \(f_i, f_j\) the
	 * payoffs of individuals \(i\) and \(j\), respectively. The resulting dynamics
	 * for the frequencies of the different traits is then given by
	 * \[
	 * \begin{align}
	 * \dot x_i =&amp; \sum_{j=1}^n x_i x_j \left(\frac1{1+\exp[w(f_i-f_j)]} -
	 * \frac1{1+\exp[w(f_j-f_i)]}\right) \\
	 * =&amp; x_i \sum_{j=1}^n \tanh[w(f_i-f_j)/2].
	 * \end{align}
	 * \]
	 *
	 * @param mod     the module representing the current species
	 * @param state   array of frequencies/densities denoting the state population
	 * @param fitness array of fitness values of types in population
	 * @param nGroup  the interaction group size
	 * @param index   the index of the module <code>mod</code> in multi-species
	 *                modules
	 * @param change  array to return the rate of change of each type in the
	 *                population
	 * @return the total change (should be zero in theory)
	 * 
	 * @see <a href="http://dx.doi.org/10.1007/s13235-010-0001-4">Sigmund, K.,
	 *      Hauert, C., Traulsen, A. &amp; De Silva, H. (2011) Social control and
	 *      the social contract: the emergence of sanctioning systems for collective
	 *      action, Dyn. Games &amp; Appl. 1, 149-171</a>
	 */
	protected double updateThermal(Module mod, double[] state, double[] fitness, int nGroup, int index,
			double[] change) {
		// no scaling seems required for comparisons with simulations
		double noise = mod.getPlayerUpdate().getNoise();
		if (noise <= 0.0)
			return updateBest(mod, state, fitness, nGroup, index, change);
		// some noise; factor 2 enters - see e.g. Sigmund et al. Dyn Games & Appl. 2011
		double inoise = 0.5 / noise;
		double err = 0.0;
		int skip = idxSpecies[index];
		int end = skip + mod.getNTraits();
		for (int n = skip; n < end; n++) {
			double dyn = 0.0;
			double ftn = fitness[n];
			for (int i = skip; i < end; i++) {
				if (i == n)
					continue;
				dyn += state[i] * Functions.tanh((ftn - fitness[i]) * inoise);
			}
			dyn *= state[n];
			change[n] = dyn;
			err += dyn;
		}
		return err;
	}

	/**
	 * Implementation of player update {@link PlayerUpdate.Type#BEST}. This
	 * calculates the rate of change individuals adopt the trait of better
	 * performing individuals with certainty (and never those of worse performing
	 * individuals). This calculates the rates of change for each type in species
	 * <code>mod</code> for the popular choice for 'pairwise comparisons' where the
	 * focal player \(i\) and one of its neighbours \(j\) are randomly chosen. The
	 * focal player \(i\) adopts the trait of player \(j\) if \(f_j&gt;f_i\) but
	 * never adopts those of a player \(j\) with \(f_j&lt;f_i\), where \(f_i,f_j\)
	 * denote the fitness of players \(i\) and \(j\), respectively. In case of a tie
	 * \(f_j=f_i\) the individual sticks to its trait. More specifically, the
	 * probability to adopt the trait of \(j\) is given by
	 * \[p_{i\to j}=\theta(f_j-f_i),\]
	 * where \(\theta(x)\) denotes the Heaviside step function with
	 * \(\theta(x)=0\) for \(x&lt;0\), \(\theta(x)=1\) for \(x&gt;0\). In principle
	 * \(\theta(0)=1/2\) but assuming that players need at least a marginal
	 * incentive to switch traits we set \(\theta(0)=0\). In most cases this
	 * choice is inconsequential. The resulting dynamics for the frequencies of the
	 * different traits is then given by
	 * \[
	 * \begin{align}
	 * \dot x_i =&amp; \sum_{j=1}^n x_i x_j \theta[f_j-f_i)].
	 * \end{align}
	 * \]
	 * <p>
	 * <strong>Note:</strong>In the limit of vanishing noise the updates
	 * {@link #updateThermal(Module, double[], double[], int, int, double[])} and
	 * {@link #updateImitate(Module, double[], double[], int, int, double[])}
	 * recover the {@link PlayerUpdate.Type#BEST} updating type as well.
	 * 
	 * @param mod     the module representing the current species
	 * @param state   array of frequencies/densities denoting the state population
	 * @param fitness array of fitness values of types in population
	 * @param nGroup  the interaction group size
	 * @param index   the index of the module <code>mod</code> in multi-species
	 *                modules
	 * @param change  array to return the rate of change of each type in the
	 *                population
	 * @return the total change (should be zero in theory)
	 */
	protected double updateBest(Module mod, double[] state, double[] fitness, int nGroup, int index, double[] change) {
		int skip = idxSpecies[index];
		int end = skip + mod.getNTraits();
		double err = 0.0;
		for (int n = skip; n < end; n++) {
			double dyn = 0.0;
			double ftn = fitness[n] - 1e-8;
			for (int i = skip; i < end; i++) {
				if (i == n)
					continue;
				dyn += (ftn > fitness[i] ? state[i] : -state[i]);
			}
			dyn *= state[n];
			change[n] = dyn;
			err += dyn;
		}
		return err;
	}

	/**
	 * Implementation of the player update {@link PlayerUpdate.Type#IMITATE}. This
	 * calculates the rates of change for each type in species <code>mod</code> for
	 * the popular choice for 'pairwise comparisons' where the focal player \(i\)
	 * and one of its neighbours \(j\) are randomly chosen. The focal player \(i\)
	 * adopts the trait of \(j\) with a probability proportional to the payoff
	 * difference \(f_j - f_i\):
	 * \[p_{i\to j}=1/2 \left(1 + (f_j-f_i)/(f_j+f_i)\right),\]
	 * where \(f_i,f_j\) denote the fitness of players \(i\) and \(j\),
	 * respectively. The resulting dynamics for the frequencies of the different
	 * traits is then given by the standard replicator equation \[
	 * \begin{align}
	 * \dot x_i =&amp; x_i (f_i-\bar f)
	 * \end{align}
	 * \]
	 * where \(\bar f\) denotes the the average population payoff.
	 * <p>
	 * <strong>Note:</strong> in multi-species models all rates of change are scaled
	 * by the maximum fitness difference for <em>all</em> species to preserve their
	 * relative time scales.
	 * 
	 * @param mod     the module representing the current species
	 * @param state   array of frequencies/densities denoting the state population
	 * @param fitness array of fitness values of types in population
	 * @param nGroup  the interaction group size
	 * @param index   the index of the module <code>mod</code> in multi-species
	 *                modules
	 * @param change  array to return the rate of change of each type in the
	 *                population
	 * @return the total change (should be zero in theory)
	 */
	protected double updateImitate(Module mod, double[] state, double[] fitness, int nGroup, int index,
			double[] change) {
		return updateReplicate(mod, state, fitness, nGroup, index, change, mod.getPlayerUpdate().getNoise());
	}

	/**
	 * Implementation of the player update {@link PlayerUpdate.Type#IMITATE_BETTER}.
	 * This calculates the rates of change for each type in species <code>mod</code>
	 * for the popular choice for 'pairwise comparisons' where the focal player
	 * \(i\) and one of its neighbours \(j\) are randomly chosen. The focal player
	 * \(i\) adopts the trait of a <em>better performing</em> player \(j\) with a
	 * probability proportional to the payoff difference \(f_j - f_i\):
	 * \[
	 * \begin{align}
	 * \dot x_i =&amp; \sum_{j=1}^n x_i x_j (f_i-f_j)_+ =
	 * \sum_{j=1}^n x_i x_j (f_i-f_j) - \sum_{j=1}^n x_i x_j (f_i-f_j)_- \\
	 * =&amp; \sum_{j=1}^n x_i x_j (f_i-f_j)/2 = x_i (f_i-\bar f)/2.
	 * \end{align}
	 * \]
	 * Incidentally and somewhat surprisingly this update also reduces to the
	 * standard replicator dynamics albeit with a constant rescaling of time such
	 * that the rate of change are half of the standard replicator equation obtained
	 * from {@code PlayerUpdate.Type#IMITATE}.
	 * <p>
	 * <strong>Note:</strong> in multi-species models all rates of change are scaled
	 * by the maximum fitness difference for <em>all</em> species to preserve their
	 * relative time scales.
	 * 
	 * @param mod     the module representing the current species
	 * @param state   array of frequencies/densities denoting the state population
	 * @param fitness array of fitness values of types in population
	 * @param nGroup  the interaction group size
	 * @param index   the index of the module <code>mod</code> in multi-species
	 *                modules
	 * @param change  array to return the rate of change of each type in the
	 *                population
	 * @return the total change (should be zero in theory)
	 */
	protected double updateImitateBetter(Module mod, double[] state, double[] fitness, int nGroup, int index,
			double[] change) {
		return updateReplicate(mod, state, fitness, nGroup, index, change, 0.5 * mod.getPlayerUpdate().getNoise());
	}

	/**
	 * Helper method to calculate the rate of change for the standard replicator
	 * dynamics with different amounts of noise arising from the microscopic update
	 * rule. In the microscopic implementation of the replicator dynamics the noise
	 * arises from focal individuals \(i\) that adopt the trait of a neighbour
	 * \(j\) with a probability proportional to the payoff difference \(f_j-f_i\),
	 * where \(f_i, f_j\) refer to the respective payoffs of \(i\) and \(j\).
	 * However, the noise is cut in half if the focal imitates only better
	 * performing neighbours, i.e. proportional to \((f_j-f_i)_+\).
	 * 
	 * @param mod     the module representing the current species
	 * @param state   array of frequencies/densities denoting the state population
	 * @param fitness array of fitness values of types in population
	 * @param nGroup  the interaction group size
	 * @param index   the index of the module <code>mod</code> in multi-species
	 *                modules
	 * @param change  array to return the rate of change of each type in the
	 *                population
	 * @param noise   the noise arising from probabilistical updates
	 * @return the total change (should be zero in theory)
	 */
	private double updateReplicate(Module mod, double[] state, double[] fitness, int nGroup, int index,
			double[] change, double noise) {
		// if noise becomes very small, this should recover PLAYER_UPDATE_BEST
		if (noise <= 0.0)
			return updateBest(mod, state, fitness, nGroup, index, change);
		double inoise = ArrayMath.min(invFitRange) / noise;
		int skip = idxSpecies[index];
		int end = skip + mod.getNTraits();
		double err = 0.0;
		for (int n = skip; n < end; n++) {
			double dyn = 0.0;
			double ftn = fitness[n];
			for (int i = skip; i < end; i++) {
				if (i == n)
					continue;
				// note: cannot use mean payoff as the transition probabilities must lie in
				// [0,1] - otherwise the timescale gets messed up.
				dyn += state[i] * Math.min(1.0, Math.max(-1.0, (ftn - fitness[i]) * inoise));
			}
			dyn *= state[n];
			change[n] = dyn;
			err += dyn;
		}
		return err;
	}

	/**
	 * Implementation of the player update {@link PlayerUpdate.Type#PROPORTIONAL}.
	 * This calculates the rates of change for each type in species <code>mod</code>
	 * for a 'pairwise comparison' where the focal player
	 * \(i\) and one of its neighbours \(j\) are randomly chosen. The focal player
	 * \(i\) adopts the trait of a player \(j\) with probability:
	 * \[p_{i\to j}=f_j/(f_i+f_j),\]
	 * where \(f_i, f_j\) refer to the respective payoffs of \(i\) and \(j\). In the
	 * continuum limit this yields
	 * \[
	 * \begin{align}
	 * \dot x_i =&amp; x_i \sum_{j=1}^n x_j \frac{f_i-f_j}{f_i+f_j}.
	 * \end{align}
	 * \]
	 * 
	 * @param mod     the module representing the current species
	 * @param state   array of frequencies/densities denoting the state population
	 * @param fitness array of fitness values of types in population
	 * @param nGroup  the interaction group size
	 * @param index   the index of the module <code>mod</code> in multi-species
	 *                modules
	 * @param change  array to return the rate of change of each type in the
	 *                population
	 * @return the total change (should be zero in theory)
	 */
	protected double updateProportional(Module mod, double[] state, double[] fitness, int nGroup, int index,
			double[] change) {
		int skip = idxSpecies[index];
		int end = skip + mod.getNTraits();
		double err = 0.0;
		for (int n = skip; n < end; n++) {
			double dyn = 0.0;
			double ftn = fitness[n];
			for (int i = skip; i < end; i++) {
				if (i == n)
					continue;
				double fiti = fitness[i];
				dyn += state[i] * (ftn - fiti) / (ftn + fiti);
			}
			dyn *= state[n];
			change[n] = dyn;
			err += dyn;
		}
		return err;
	}

	/**
	 * Implementation of the player update {@link PlayerUpdate.Type#BEST_RESPONSE}.
	 * This calculates the rates of change for each type in species <code>mod</code>
	 * for the best-response dynamic where the focal player \(i\) switches to the
	 * trait \(j\) that has the highest payoff given the current state of the
	 * population. Note, in contrast to other player updates, such as
	 * {@link PlayerUpdate.Type#THERMAL} or {@link PlayerUpdate.Type#IMITATE} the
	 * best-response is an innovative update rule, which means that traits
	 * that are currently not present in the population can get introduced.
	 * Consequently, homogeneous states may not be absorbing.
	 * <p>
	 * <strong>Note:</strong> inactive traits are excluded and do not qualify as
	 * a best response.
	 * <p>
	 * <strong>IMPORTANT:</strong> needs to be thread safe (must supply memory for
	 * calculations and results)
	 * 
	 * @param mod     the module representing the current species
	 * @param state   array of frequencies/densities denoting the state population
	 * @param fitness array of fitness values of types in population
	 * @param nGroup  the interaction group size
	 * @param index   the index of the module <code>mod</code> in multi-species
	 *                modules
	 * @param change  array to return the rate of change of each type in the
	 *                population
	 * @return the total change (should be zero in theory)
	 */
	protected double updateBestResponse(Module mod, double[] state, double[] fitness, int nGroup, int index,
			double[] change) {
		int nTraits = mod.getNTraits();
		int skip = idxSpecies[index];
		int end = skip + nTraits;
		double min = Double.MAX_VALUE;
		double max = -Double.MAX_VALUE;
		int nMax = 0;
		boolean[] active = mod.getActiveTraits();
		for (int i = skip; i < end; i++) {
			change[i] = 0.0; // reset
			if (active != null && !active[i - skip])
				continue;
			double fiti = fitness[i];
			if (min > fiti)
				min = fiti;
			// if difference is tiny, consider them equal
			if (Math.abs(fiti - max) < 1e-6) {
				change[i] = 1.0;
				max = Math.max(fiti, max); // just to avoid any drift
				nMax++;
				continue;
			}
			if (fiti > max) {
				// new max found - clear reply
				for (int j = skip; j < i; j++)
					change[j] = 0.0;
				change[i] = 1.0;
				max = fiti;
				nMax = 1;
			}
		}
		double diff = max - min;
		// note:
		// - the threshold to signal convergence in terms of payoff range is
		// completely heuristic... convergence no longer triggered for CDL with
		// threshold of 1e-4. this should be done in a more systematic manner.
		// - the second condition already triggers early on whenever approaching
		// points where another trait becomes the best response.
		if (diff < 1e-3 && diff * dtTry < accuracy) {
			System.arraycopy(state, skip, change, skip, nTraits);
		} else {
			if (nMax > 1) {
				double norm = 1.0 / nMax;
				for (int i = skip; i < end; i++)
					change[i] *= norm;
			}
		}
		double err = 0.0;
		for (int n = skip; n < end; n++) {
			double dyn = change[n] - state[n];
			change[n] = dyn;
			err += dyn;
		}
		return err;
	}

	@Override
	public void update() {
		getDerivatives(time, yt, ft, dyt);
	}

	@Override
	public String getStatus() {
		String status = "";
		int from = 0;
		for (Module pop : species) {
			int to = from + pop.getNTraits();
			// omit status for vacant trait in density models
			int vacant = isDensity ? from + pop.getVacant() : -1;
			String popStatus = "";
			for (int i = from; i < to; i++) {
				if (i == vacant)
					continue;
				popStatus += (popStatus.length() > 0 ? ", " : "") + names[i] + ": " //
						+ (isDensity ? Formatter.format(yt[i], 1)
								: Formatter.formatPercent(yt[i], 1));
			}
			from = to;
			status += (isMultispecies ? (status.length() > 0 ? "<br/><i>" : "<i>") + pop.getName() + ":</i> " : "")
					+ popStatus;
		}
		return status;
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * ODE and SDE models return <code>true</code> by default.
	 */
	@Override
	public boolean permitsTimeReversal() {
		return true;
	}

	@Override
	public boolean isTimeReversed() {
		return !forward;
	}

	@Override
	public void setTimeReversed(boolean reversed) {
		if (!permitsTimeReversal() || forward != reversed)
			return; // nothing to do
		forward = !reversed;
		converged = false;
		update();
	}

	/**
	 * Sets whether adjusted dynamics is requested. It may not be possible to honour
	 * the request, e.g. if payoffs can be negative.
	 * 
	 * @param adjusted if <code>true</code> requests adjusted dynamics
	 */
	public void setAdjustedDynamics(boolean adjusted) {
		isAdjustedDynamics = adjusted;
	}

	/**
	 * Sets the desired accuracy for determining convergence. If
	 * <code>y(t+dt)-y(t)&lt;acc dt</code> holds, where <code>y(t+dt)</code> denotes
	 * the new state and <code>y(t)</code> the previous state, the numerical
	 * integration is reported as having converged and stops.
	 *
	 * @param acc the numerical accuracy
	 */
	public void setAccuracy(double acc) {
		if (acc <= 0.0)
			return;
		accuracy = acc;
	}

	/**
	 * Gets the numerical accuracy for determining convergence.
	 *
	 * @return the numerical accuracy
	 */
	public double getAccuracy() {
		return accuracy;
	}

	/**
	 * Helper method to set the intital state of the model. The method initializes
	 * the state vector {@link #yt} and the initial state vector {@link #y0}. If
	 * {@code doRandom}
	 * is <code>true</code> random initial frequencies are set for species with
	 * {@code InitType.RANDOM}.
	 * 
	 * @param doRandom if <code>true</code> use random initial frequencies as
	 *                 requested
	 * 
	 */
	private void init(boolean doRandom) {
		time = 0.0;
		dtTry = dt;
		connect = false;
		converged = false;
		// PDE models have their own initialization types
		if (isPDE())
			return;
		int idx = -1;
		// y0 is initialized except for species with random initial frequencies
		if (doRandom) {
			for (Module pop : species) {
				if (!initType[++idx].equals(InitType.RANDOM))
					continue;
				int dim = pop.getNTraits();
				int from = idxSpecies[idx];
				for (int n = 0; n < dim; n++)
					y0[from + n] = rng.random01();
			}
		}
		System.arraycopy(y0, 0, yt, 0, nDim);
		normalizeState(yt);
	}

	/**
	 * Types of initial configurations. Currently this model supports the following
	 * density distributions:
	 * <dl>
	 * <dt>DENSITY
	 * <dd>Initial densities as specified in {@link #cloInit} (density modules).
	 * <dt>FREQUENCY
	 * <dd>Initial frequencies as specified in {@link #cloInit} (frequency
	 * modules).
	 * <dt>UNIFORM
	 * <dd>Uniform frequencies of traits (default; in density modules all densities
	 * are set to zero).
	 * <dt>RANDOM
	 * <dd>Random initial trait frequencies. <br>
	 * <strong>Note:</strong> Not available for density based models.
	 * </dl>
	 * 
	 * @see #cloInit
	 * @see #parse(String)
	 * @see PDE.InitType
	 * @see PDE#parse(String)
	 */
	public enum InitType implements CLOption.Key {

		/**
		 * Initial densities as specified.
		 * <p>
		 * <strong>Note:</strong> Not available for frequency based models.
		 */
		DENSITY("density", "initial trait densities <d1,...,dn>"),

		/**
		 * Initial frequencies as specified.
		 * <p>
		 * <strong>Note:</strong> Not available for density based models.
		 */
		FREQUENCY("frequency", "initial trait frequencies <f1,...,fn>"),

		/**
		 * Uniform initial frequencies of traits (default; in density modules all
		 * desnities are set to zero).
		 */
		UNIFORM("uniform", "uniform initial frequencies"),

		/**
		 * Random initial trait frequencies.
		 * <p>
		 * <strong>Note:</strong> Not available for density based models.
		 */
		RANDOM("random", "random initial frequencies"),

		/**
		 * Single mutant in homogeneous resident.
		 * <p>
		 * <strong>Note:</strong> Only available for SDE models. Not available for density based models.
		 * 
		 * @see SDE
		 */
		MUTANT("mutant", "single mutant");

		/**
		 * Key of initialization type. Used when parsing command line options.
		 * 
		 * @see InitType#parse(String)
		 */
		String key;

		/**
		 * Brief description of initialization type for help display.
		 * 
		 * @see EvoLudo#getCLOHelp()
		 */
		String title;

		/**
		 * Instantiate new initialization type.
		 * 
		 * @param key   identifier for parsing of command line option
		 * @param title summary of geometry
		 */
		InitType(String key, String title) {
			this.key = key;
			this.title = title;
		}

		@Override
		public String getKey() {
			return key;
		}

		@Override
		public String getTitle() {
			return title;
		}

		@Override
		public String toString() {
			return key;
		}
	}

	/**
	 * Parse initializer string {@code arg}. Determine type of initialization and
	 * process its arguments as appropriate.
	 * <p>
	 * <strong>Note:</strong> Not possible to perform parsing in {@code CLODelegate}
	 * of {@link #cloInit} because PDE model provide their own
	 * {@link PDE.InitType}s.
	 * 
	 * @param arg the arguments to parse
	 * @return {@code true} if parsing successful
	 * 
	 * @see InitType
	 */
	public boolean parse(String arg) {
		// nDim and idxSpecies not yet initialized
		int dim = 0;
		for (Module pop : species)
			dim += pop.getNTraits();
		if (y0 == null || y0.length != dim)
			y0 = new double[dim];
		Arrays.fill(y0, 0.0);
		String[] inittypes = arg.split(CLOParser.SPECIES_DELIMITER);
		int idx = 0;
		int start = 0;
		boolean parseOk = true;
		species: 
		for (Module pop : species) {
			String inittype = inittypes[idx % inittypes.length];
			double[] initargs = null;
			String[] typeargs = inittype.split("\\s+|=");
			InitType itype = (InitType) cloInit.match(inittype);
			// if matching of inittype failed assume it was omitted; use previous type
			if (itype == null) {
				// if no previous match, give up
				if (idx == 0) {
					parseOk = false;
					break;
				}
				itype = initType[idx - 1];
				initargs = CLOParser.parseVector(typeargs[0]);
			} else if (typeargs.length > 1)
				initargs = CLOParser.parseVector(typeargs[1]);
			int nTraits = pop.getNTraits();
			switch (itype) {
				case MUTANT:
					// SDE models only (no vacant sites)
					// initargs contains the index of the resident and mutant traits
					int mutantType = (int) initargs[0];
					int len = initargs.length;
					int residentType;
					if (len > 1)
						residentType = (int) initargs[1];
					else
						residentType = (mutantType + 1) % nTraits;
					// set all initial frequencies to zero
					Arrays.fill(y0, start, start + nTraits, 0.0);
					y0[mutantType] = 1.0 / pop.getNPopulation();
					y0[residentType] = 1.0 - y0[mutantType];
					break;
				case DENSITY:
				case FREQUENCY:
					if (initargs == null || initargs.length != nTraits) {
						parseOk = false;
						break species;
					}
					System.arraycopy(initargs, 0, y0, start, nTraits);
					break;
				case RANDOM:
				case UNIFORM:
				default:
					// uniform distribution is the default. for densities set all to zero.
					Arrays.fill(y0, start, start + nTraits, isDensity ? 0.0 : 1.0);
					break;
			}
			initType[idx] = itype;
			idx++;
			start += nTraits;
		}
		if (!parseOk) {
			logger.warning("parsing of init '" + arg + "' failed - using " + cloInit.getDefault() + ".");
			return false;
		}
		return true;
	}

	/**
	 * Command line option to set the time increment <code>dt</code> in DE models.
	 * If the requested <code>dt</code> turns out to be too big, e.g. due to
	 * diffusion in {@link org.evoludo.simulator.models.PDE}, it is
	 * reduced appropriately. For {@link org.evoludo.simulator.models.RungeKutta}
	 * this represents the initial step.
	 * 
	 * @see #dt
	 * @see #setDt(double)
	 */
	public final CLOption cloDEdt = new CLOption("dt", "0.1", EvoLudo.catModel,
			"--dt <t>        time increment for ODE/PDE/SDE integration", new CLODelegate() {
				@Override
				public boolean parse(String arg) {
					setDt(CLOParser.parseDouble(arg));
					return true;
				}

				@Override
				public void report(PrintStream output) {
					output.println("# timeincr:             " + Formatter.format(getDt(), 4));
				}
			});

	/**
	 * Command line option to set the initial configuration in ODE/SDE models.
	 * <p>
	 * <strong>Note:</strong> PDE models use their own set of initial
	 * configurations.
	 * 
	 * @see InitType
	 * @see PDE.InitType
	 */
	public final CLOption cloInit = new CLOption("init", InitType.UNIFORM.getKey(), EvoLudo.catModel,
			"--init <t>      type of initial configuration", new CLODelegate() {
				@Override
				public boolean parse(String arg) {
					// parsing must be 'outsourced' to ODE class to enable
					// PDE models to override it and do their own initialization.
					return ODE.this.parse(arg);
				}

				@Override
				public void report(PrintStream output) {
					int idx = 0;
					for (Module pop : species) {
						InitType itype = initType[idx++];
						String msg = "# init:                 " + itype;
						if (itype.equals(InitType.DENSITY) || itype.equals(InitType.FREQUENCY)) {
							int from = idxSpecies[idx];
							msg += Formatter.format(Arrays.copyOfRange(y0, from, from + pop.getNTraits()), 4);
							Arrays.copyOfRange(y0, idxSpecies[idx], pop.getNTraits());
						}
						if (species.size() > 1)
							msg += " (" + pop.getName() + ")";
						output.println(msg);
					}
				}
			});

	/**
	 * Command line option to activate adjusted replicator dynamics (if possible) in
	 * ODE models.
	 * 
	 * @see #isAdjustedDynamics
	 * @see #setAdjustedDynamics(boolean)
	 */
	public final CLOption cloAdjustedDynamics = new CLOption("adjusted", "standard", CLOption.Argument.NONE,
			EvoLudo.catModel,
			"--adjusted      adjusted replicator dynamics", new CLODelegate() {
				@Override
				public boolean parse(String arg) {
					setAdjustedDynamics(cloAdjustedDynamics.isSet());
					return true;
				}

				@Override
				public void report(PrintStream output) {
					output.println("# replicator dynamics:  " + (isAdjustedDynamics ? "adjusted" : "standard"));
				}
			});

	/**
	 * Command line option to set the desired accuracy of ODE calculations as
	 * criteria for convergence and whether population is monomorphic.
	 * 
	 * @see #accuracy
	 * @see #setAccuracy(double)
	 * @see #hasConverged()
	 * @see #isMonomorphic()
	 */
	public final CLOption cloDEAccuracy = new CLOption("accuracy", "0.0001", EvoLudo.catModel,
			"--accuracy <a>  accuracy for convergence, y(t+dt)-y(t)<a dt",
			new CLODelegate() {
				@Override
				public boolean parse(String arg) {
					setAccuracy(CLOParser.parseDouble(arg));
					return true;
				}

				@Override
				public void report(PrintStream output) {
					output.println("# accuracy:             " + getAccuracy());
				}
			});

	/**
	 * Command line option to set the number of generations between reports for
	 * {@link EvoLudo#modelNext()}.
	 */
	public final CLOption cloTimeReversed = new CLOption("timereversed", "forward", EvoLudo.catModel,
			"--timereversed  reverse time", new CLODelegate() {
				@Override
				public boolean parse(String arg) {
					setTimeReversed(cloTimeReversed.isSet());
					return true;
				}

				@Override
				public void report(PrintStream output) {
					output.println("# time:                 " + (isTimeReversed() ? "reversed" : "forward"));
				}
			});

	@Override
	public void collectCLO(CLOParser parser) {
		super.collectCLO(parser);
		parser.addCLO(cloAdjustedDynamics);
		parser.addCLO(cloDEAccuracy);
		parser.addCLO(cloDEdt);
		parser.addCLO(cloInit);
		cloInit.clearKeys();
		cloInit.addKeys(InitType.values());
		if (isDensity) {
			cloInit.removeKey(InitType.RANDOM);
			cloInit.removeKey(InitType.FREQUENCY);
		} else {
			cloInit.removeKey(InitType.DENSITY);
		}
		if (!(this instanceof SDE))
			cloInit.removeKey(InitType.MUTANT);
		if (permitsTimeReversal())
			parser.addCLO(cloTimeReversed);
	}

	@Override
	public void encodeState(StringBuilder plist) {
		plist.append(Plist.encodeKey("Model", type.toString()));
		plist.append(Plist.encodeKey("Time", time));
		plist.append(Plist.encodeKey("Dt", dt));
		plist.append(Plist.encodeKey("Forward", forward));
		plist.append(Plist.encodeKey("AdjustedDynamics", isAdjustedDynamics));
		plist.append(Plist.encodeKey("Accuracy", accuracy));
		encodeTraits(plist);
		encodeFitness(plist);
	}

	@Override
	public boolean restoreState(Plist plist) {
		boolean success = true;
		time = (Double) plist.get("Time");
		dt = (Double) plist.get("Dt");
		forward = (Boolean) plist.get("Forward");
		isAdjustedDynamics = (Boolean) plist.get("AdjustedDynamics");
		accuracy = (Double) plist.get("Accuracy");
		connect = false;
		if (!restoreTraits(plist)) {
			logger.warning("restore traits in " + type + "-model failed.");
			success = false;
		}
		if (!restoreFitness(plist)) {
			logger.warning("restore fitness in " + type + "-model failed.");
			success = false;
		}
		return success;
	}

	/**
	 * Encodes state of the model in the form of a <code>plist</code> string.
	 * 
	 * @param plist the string builder for the encoded state
	 */
	void encodeTraits(StringBuilder plist) {
		plist.append(Plist.encodeKey("State", yt));
		plist.append(Plist.encodeKey("StateChange", dyt));
	}

	/**
	 * Restores the state encoded in <code>plist</code>.
	 * 
	 * @param plist the encoded state
	 * @return <code>true</code> if successful
	 */
	boolean restoreTraits(Plist plist) {
		@SuppressWarnings("unchecked")
		List<Double> state = (List<Double>) plist.get("State");
		@SuppressWarnings("unchecked")
		List<Double> change = (List<Double>) plist.get("StateChange");
		if (state == null || state.size() != nDim || change == null || change.size() != nDim)
			return false;
		for (int n = 0; n < nDim; n++) {
			yt[n] = state.get(n);
			dyt[n] = change.get(n);
		}
		return true;
	}

	/**
	 * Encodes the fitness of the model in the form of a <code>plist</code> string.
	 * 
	 * @param plist the string builder for the encoded state
	 */
	void encodeFitness(StringBuilder plist) {
		plist.append(Plist.encodeKey("Fitness", ft));
	}

	/**
	 * Restores the fitness encoded in <code>plist</code>.
	 * 
	 * @param plist the encoded state
	 * @return <code>true</code> if successful
	 */
	boolean restoreFitness(Plist plist) {
		@SuppressWarnings("unchecked")
		List<Double> fit = (List<Double>) plist.get("Fitness");
		if (fit == null || fit.size() != nDim)
			return false;
		for (int n = 0; n < nDim; n++)
			ft[n] = fit.get(n);
		update();
		return true;
	}
}
