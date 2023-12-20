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

package org.evoludo.simulator.models;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.evoludo.math.ArrayMath;
import org.evoludo.math.Functions;
import org.evoludo.math.RNGDistribution;
import org.evoludo.simulator.ColorMap;
import org.evoludo.simulator.EvoLudo;
import org.evoludo.simulator.modules.Discrete;
import org.evoludo.simulator.modules.Module;
import org.evoludo.simulator.modules.Module.Map2Fitness;
import org.evoludo.simulator.modules.Module.PlayerUpdateType;
import org.evoludo.util.CLOParser;
import org.evoludo.util.CLOption;
import org.evoludo.util.CLOption.CLODelegate;
import org.evoludo.util.Formatter;
import org.evoludo.util.Plist;

/**
 * Euler method for the numerical integration of systems of ordinary
 * differential equations.
 *
 * @author Christoph Hauert
 */
public class ODEEuler implements Model.ODE {

	/**
	 * Methods that every {@link Module} must implement, which advertises numerical
	 * solutions based on differential equations.
	 *
	 * @author Christoph Hauert
	 */
	public abstract interface HasDE {

		/**
		 * For replicator dynamics the frequencies of all strategies must sum up to one.
		 * Hence, for <code>nTraits</code> strategies there are only
		 * <code>nTraits-1</code> degrees of freedom. <code>dependentTrait</code> marks
		 * the one that is derived from the others.
		 *
		 * @return the index of the dependent trait or -1 if there is none
		 */
		public default int getDependent() {
			return -1;
		}

		/**
		 * Calculate the average payoff/score for the frequency of traits/strategies
		 * specified in the array <code>density</code> for interactions in groups of
		 * size <code>n</code>. The average payoffs/scores for each of the
		 * <code>nTraits</code> traits/strategies must be stored and returned in the
		 * array <code>avgscores</code>.
		 * <p>
		 * <strong>Note:</strong> needs to be thread safe for parallel processing of
		 * PDE's.
		 * <p>
		 * <strong>IMPORTANT:</strong> one of
		 * <ul>
		 * <li><code>{@link #avgScores(double[], int, double[])}</code>,
		 * <li><code>{@link #avgScores(double[], int, double[], int)}</code>, or
		 * <li>
		 * {@link ODEEuler#getDerivatives(double, double[], double[], double[], double[])}
		 * </ul>
		 * must be implemented in modules that advertise the model types
		 * <code>ODE, SDE</code> or <code>PDE</code>.
		 *
		 * @param density   the frequency/density of each trait/strategy
		 * @param n         the size of interaction groups
		 * @param avgscores the array for storing the average payoffs/scores for each
		 *                  strategic type
		 */
		public default void avgScores(double[] density, int n, double[] avgscores) {
			avgScores(density, n, avgscores, 0);
		}

		/**
		 * Calculate the average payoff/score for the frequency of traits/strategies
		 * specified in the array <code>density</code> for interactions in groups of
		 * size <code>n</code> in <em>multi-species interactions</em>. The state of the
		 * current species starts at index <code>skip</code> and the average
		 * payoffs/scores for each of its <code>nTraits</code> traits/strategies must be
		 * stored and returned in the array <code>avgscores</code> starting at index
		 * <code>skip</code>.
		 * <p>
		 * <strong>Note:</strong> needs to be thread safe for parallel processing of
		 * PDE's.
		 * <p>
		 * <strong>IMPORTANT:</strong> one of
		 * <ul>
		 * <li><code>{@link #avgScores(double[], int, double[])}</code>,
		 * <li><code>{@link #avgScores(double[], int, double[], int)}</code>, or
		 * <li>
		 * {@link ODEEuler#getDerivatives(double, double[], double[], double[], double[])}
		 * </ul>
		 * must be implemented in modules that advertise the model types
		 * <code>ODE, SDE</code> or <code>PDE</code>.
		 *
		 * @param density   the frequency/density of each trait/strategy
		 * @param n         the size of interaction groups
		 * @param avgscores the array for storing the average payoffs/scores for each
		 *                  strategic type
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
	 * 
	 * @author Christoph Hauert
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
		public default Model.ODE createODE() {
			return null;
		}
	}

	/**
	 * The pacemaker of all models. Interface with the outside world.
	 */
	protected EvoLudo engine;

	/**
	 * The module associated with this model.
	 */
	protected Module module;

	/**
	 * List with all species in model including this one. List should be shared with
	 * other populations (to simplify bookkeeping) but the species list CANNOT be
	 * static! Otherwise it is impossible to run multiple instances of modules/models
	 * concurrently.
	 */
	protected ArrayList<? extends Module> species;

	/**
	 * The current time. May be negative when integrating backwards.
	 * 
	 * @see #forward
	 * @see #isTimeReversed()
	 */
	double t;

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
	 * The attempted size of next step to take. In {@code ODEEuler} this is always
	 * equal to {@code dt} but for more sophisticated integrators, {@link ODERK} for
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
	 * Storage for intermediate results. This array has the same size as e.g.
	 * {@link #yt}.
	 */
	double[] tmp;

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
	 * @see #cloInitType
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
	 * @see Module#getMutationProb()
	 */
	double[] mus;

	/**
	 * Array containing the indices of the dependent trait in each species.
	 * Dependent traits make only sense in replicator type model with frequencies,
	 * i.e. normalized states. The dependent trait is the one that gets derived from
	 * the changes in all others in order to maintain normalization.
	 * 
	 * @see Module#getDependent()
	 */
	int[] dependents;

	/**
	 * Array containing the inverse of the fitness range:
	 * <code>1.0/(maxFitness - minFitness)</code> for each species. This is used to
	 * normalize imitation rules of players.
	 * 
	 * @see Module.PlayerUpdateType#IMITATE
	 */
	double[] invFitRange;

	/**
	 * <code>true</code> if the previous and the current data point should be
	 * connected.
	 *
	 * @see #checkConvergence(double)
	 * @see #setAccuracy(double)
	 */
	boolean connect = true;

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
	 * <code>true</code> if the numerical integration converged.
	 *
	 * @see #checkConvergence(double)
	 * @see #setAccuracy(double)
	 */
	protected boolean converged = false;

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
	 * @param engine the pacemeaker for running the model
	 * @param module the module to numerically integrate
	 */
	public ODEEuler(EvoLudo engine, Module module) {
		this.engine = engine;
		this.module = module;
	}

	@Override
	public void load() {
		species = module.getSpecies();
		int nSpecies = species.size();
		module = (nSpecies == 1 ? species.get(0) : null);
		initType = new InitType[nSpecies];
		mus = new double[nSpecies];
	}

	@Override
	public void unload() {
		yt = ft = dyt = yout = tmp = null;
		mus = null;
		staticfit = null;
		names = null;
		initType = null;
		cloInitType.clearKeys();
	}

	@Override
	public void reset() {
		t = 0.0;
		int skip = 0;
		for (Module pop : species) {
			int nTraits = pop.getNTraits();
			System.arraycopy(pop.getTraitNames(), 0, names, skip, nTraits);
			skip += nTraits;
		}
		skip = 0;
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
		// note: module undefined for isMultispecies==true
		monoStop = false;
		if (!species.get(0).isContinuous())
			monoStop = ((Discrete) species.get(0)).getMonoStop();
	}

	@Override
	public boolean check() {
		boolean doReset = false;
		int nSpecies = species.size();
		dstate = null;
		double minFit = Double.MAX_VALUE;
		double maxFit = -Double.MAX_VALUE;
		dependents = new int[nSpecies];
		rates = new double[nSpecies];
		invFitRange = new double[nSpecies];
		Arrays.fill(invFitRange, 1.0);
		idxSpecies = new int[nSpecies + 1];
		nDim = 0;
		int idx = 0;
		for (Module pop : species) {
			doReset |= pop.check();
			dependents[idx] = pop.getDependent();
			int nTraits = pop.getNTraits();
			idxSpecies[idx] = nDim;
			rates[idx] = pop.getSpeciesUpdateRate();
			minFit = pop.getMinFitness();
			maxFit = pop.getMaxFitness();
			if (maxFit > minFit)
				invFitRange[idx] = 1.0 / (maxFit - minFit);
			idx++;
			nDim += nTraits;
		}
		idxSpecies[nSpecies] = nDim;
		if (yt == null || yt.length != nDim) {
			yt = new double[nDim];
			ft = new double[nDim];
			dyt = new double[nDim];
			yout = new double[nDim];
			tmp = new double[nDim];
			names = new String[nDim];
		}

		if (isAdjustedDynamics && minFit <= 0.0) {
			// fitness is not guaranteed to be positive
			engine.getLogger().warning(getClass().getSimpleName()
					+ " - fitness >0 must hold for adjusted dynamics (revert to standard dynamics).");
			isAdjustedDynamics = false;
		}
		return doReset;
	}

	@Override
	public void setDt(double deltat) {
		deltat = Math.max(0.0, deltat);
		if (deltat == 0.0) {
			engine.getLogger()
					.warning(getClass().getSimpleName() + " - time step must be >0 (dt=" + Formatter.formatSci(dt, 5)
							+ " kept; requested dt=" + Formatter.formatSci(deltat, 5) + " ignored).");
			return;
		}
		dt = deltat;
	}

	@Override
	public double getDt() {
		return dt;
	}

	@Override
	public boolean setInitialTraits(double[] init) {
		if (init.length != nDim)
			return false;
		System.arraycopy(init, 0, y0, 0, nDim);
		init(false);
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
	 * Gets array with all trait names.
	 * <p>
	 * <strong>NOTE:</strong> this is a convenience method for multi-species modules
	 * to efficiently retrieve all trait names for further processing or
	 * visualization.
	 *
	 * @return the trait names
	 * 
	 * @see #names
	 */
	public String[] getTraitNames() {
		return names;
	}

	/**
	 * Unused interface method.
	 */
	@Override
	public <T> void getTraitData(int ID, T[] colors, ColorMap<T> colorMap) {
		// not applicable
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
	public <T> void getFitnessData(int ID, T[] colors, ColorMap.Gradient1D<T> colorMap) {
		// not applicable
	}

	@Override
	public void getFitnessHistogramData(int id, double[][] bins) {
		// tmp variables
		int nBins = bins[0].length;
		int maxBin = nBins - 1;
		// for neutral selection maxScore==minScore! assume range [score-1, score+1]
		// needs to be synchronized with GUI (e.g. MVFitness, MVFitHistogram, ...)
		Module pop = species.get(id);
		double min = pop.getMinFitness();
		double max = pop.getMaxFitness();
		double map;
		if (max - min < 1e-8) {
			// close enough to neutral
			map = nBins * 0.5;
			min--;
		} else
			map = nBins / (max - min);
		int idx = 0;
		int vacant = pop.getVacant();
		// fill bins
		int offset = idxSpecies[id];
		int dim = idxSpecies[id + 1] - offset;
		for (int n = 0; n < dim; n++) {
			if (n == vacant)
				continue;
			Arrays.fill(bins[idx], 0.0);
			int bin = (int) ((ft[offset + idx] - min) * map);
			bin = Math.max(0, Math.min(maxBin, bin));
			bins[idx][bin] = yt[offset + idx];
			idx++;
		}
	}

	@Override
	public double getTime() {
		return t;
	}

	@Override
	public boolean isConnected() {
		return connect;
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
		double nextHalt = engine.getNextHalt();
		// continue if milestone reached in previous step, i.e. deltat < 1e-8
		double step = engine.getReportInterval();
		double deltat = Math.abs(nextHalt - t);
		if (deltat >= 1e-8)
			step = Math.min(step, deltat);
		// NOTE: dt is always >0
		double elapsed = 0.0;
		double minast = accuracy * accuracy * dt;
		double timeBefore = t;
		while (Math.abs(step - elapsed) > 1e-8) {
			// take deterministic step
			double nextstep = Math.min(dtTry, step - elapsed);
			double d2 = deStep(forward ? nextstep : -nextstep);
			elapsed += dtTaken;
			converged = checkConvergence(d2);
			if (converged)
				break;
			// emergency brake - not up for negotiations.
			if (dtTaken < minast) {
				converged = true;
				break;
			}
		}
		// try to minimize rounding errors
		t = timeBefore + (forward ? elapsed : -elapsed);
		if (Math.abs(nextHalt - t) < 1e-8)
			return false;
		return !converged;
	}

	@Override
	public boolean hasConverged() {
		return converged;
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
		getDerivatives(t, yt, ft, dyt, tmp);
		t += step;
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
		if (isDensity())
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
	 * Calculate the rates of change for all species in <code>state</code> at
	 * <code>time</code> given the fitness <code>fit</code>. The result is returned
	 * in <code>change</code>. <code>scratch</code> is an array for storing
	 * intermediate results.
	 * <p>
	 * <strong>IMPORTANT:</strong> parallel processing for PDE's in JRE requires
	 * this method to be thread safe.
	 * <p>
	 * <strong>Note:</strong> adding <code>synchronized</code> to
	 * this method would seem to make sense but this results in a deadlock for
	 * multiple threads in PDE's and JRE. However, apparently not needed as
	 * javascript and java yield the exact same results (after 15k generations!)
	 *
	 * @param time    the current time
	 * @param state   the array of frequencies/densities denoting the state
	 *                population
	 * @param fit     the array of fitness values of types in population
	 * @param change  the array to return the rate of change of each type in the
	 *                population
	 * @param scratch the array to store intermediate calculations (for PDEs this
	 *                must be thread safe)
	 * @return the array with the changes
	 */
	protected double[] getDerivatives(double time, double[] state, double[] fit, double[] change, double[] scratch) {
		dstate = state;
		int idx = 0;
		for (Module pop : species)
			getDerivatives(time, state, fit, change, scratch, pop, idx++);
		dstate = null;
		idx = 0;
		int from = 0;
		for (Module pop : species) {
			int dim = pop.getNTraits();
			int to = from + dim;
			double rate = rates[idx];
			if (isAdjustedDynamics) {
				double fbar = 0.0;
				for (int i = from; i < to; i++)
					fbar += state[i] * fit[i];
				rate /= fbar;
			}
			for (int i = from; i < to; i++)
				change[i] *= rate;
			double m = mus[idx];
			if (m > 0.0) {
				double mud = m / (dim - 1);
				// mutations to any of the _other_ strategies
				for (int i = from; i < to; i++)
					change[i] = change[i] * (1.0 - m) + mud * (1.0 - dim * state[i]);
			}
			from = to;
			idx++;
		}
		return change;
	}

	/**
	 * Calculate the rates of change for each type in species <code>mod</code>.
	 * <p>
	 * For replicator models the dynamics depends on the selected type of player
	 * updating, see {@link PlayerUpdateType}:
	 * <dl>
	 * <dt>{@link PlayerUpdateType#THERMAL}:
	 * <dd>A popular choice for the so called 'pairwise comparison' approach is to
	 * assume that players \(i\) and \(j\) are randomly chosen and player \(i\)
	 * adopts the strategy of player \(j\) with a probability
	 * \[p_{i\to j}=\frac1{1+\exp[w(f_i-f_j)]},\]
	 * where \(w\geq 0\) denotes the strength of selection and \(f_i, f_j\) the
	 * payoffs of individuals \(i\) and \(j\), respectively. The resulting dynamics
	 * for the frequencies of the different strategic types is then given by
	 * \[
	 * \begin{align}
	 * \dot x_i =&amp; \sum_{j=1}^n x_i x_j \left(\frac1{1+\exp[w(f_i-f_j)]} -
	 * \frac1{1+\exp[w(f_j-f_i)]}\right) \\
	 * =&amp; x_i \sum_{j=1}^n \tanh[w(f_i-f_j)/2]
	 * \end{align}
	 * \]
	 * <dt>{@link PlayerUpdateType#BEST_RESPONSE}:
	 * <dd>\[\dot x_i = x_i (TODO)\]
	 * <dt>{@link PlayerUpdateType#BEST}:
	 * <dd>\[\dot x_i = x_i (TODO)\]
	 * <dt>{@link PlayerUpdateType#BEST_RANDOM}:
	 * <dd>same as {@link PlayerUpdateType#BEST}.
	 * <dt>{@link PlayerUpdateType#PROPORTIONAL}:
	 * <dd>\[\dot x_i = x_i (TODO)\]
	 * <dt>{@link PlayerUpdateType#IMITATE}:
	 * <dd>Imitate strategies of other players at a rate proportional to the
	 * difference in payoffs:
	 * \[\dot x_i = x_i (f_i-\bar f),\]
	 * where \(\bar f\) denotes the the average population payoff with
	 * \(f_i=\sum_{j=1}^n x_j a_{ij}\) denoting the average payoff of type \(i\)
	 * with \(n\) strategy types and \(a_{ij}\) the payoff of a type \(i\)
	 * individual interacting with a type \(j\). This represents the standard
	 * replicator dynamics.
	 * <dt>{@link PlayerUpdateType#IMITATE_BETTER}:
	 * <dd>Imitate strategies of <em>better</em> performing players at a rate
	 * proportional to the difference in payoffs:
	 * \[
	 * \begin{align}
	 * \dot x_i =&amp; \sum_{j=1}^n x_i x_j (f_i-f_j)_+ =
	 * \sum_{j=1}^n x_i x_j (f_i-f_j) - \sum_{j=1}^n x_i x_j (f_i-f_j)_- \\
	 * =&amp; \sum_{j=1}^n x_i x_j (f_i-f_j)/2 = x_i (f_i-\bar f)/2.
	 * \end{align}
	 * \]
	 * Incidentally and somewhat surprisingly this update also reduces to the
	 * standard replicator dynamics albeit with a constant rescaling of time.
	 * </dl>
	 * For ecological models, i.e. with vacancies, calculatee the rates of change.
	 * The default is given by
	 * \[\dot x = x_i (z\cdot f_i - d)\]
	 * where \(z\) denotes vacant space, \(f_i\) the fitness of type \(i\) and \(d\)
	 * is the death rate. This works nicely for replicator type equations but may
	 * not fit more general dynamical equations.
	 *
	 * @param time    current time
	 * @param state   array of frequencies/densities denoting the state population
	 * @param fit     array of fitness values of types in population
	 * @param change  array to return the rate of change of each type in the
	 *                population
	 * @param scratch array to store intermediate calculations (for PDEs this must
	 *                be thread safe)
	 * @param mod     pointer to the current module
	 * @param idx     the index of the module <code>mod</code> in multi-species
	 *                modules
	 * @return the array with the changes
	 * 
	 * @see <a href="http://dx.doi.org/10.1007/s13235-010-0001-4">Sigmund, K.,
	 *      Hauert, C., Traulsen, A. &amp; De Silva, H. (2011) Social control and
	 *      the social contract: the emergence of sanctioning systems for collective
	 *      action, Dyn. Games &amp; Appl. 1, 149-171</a>
	 */
	protected double[] getDerivatives(double time, double[] state, double[] fit, double[] change, double[] scratch,
			Module mod, int idx) {
		int nGroup = mod.getNGroup();
		int skip = idxSpecies[idx];
		avgScores(mod, state, fit, nGroup, scratch, skip);

		int nTraits = mod.getNTraits();
		int end = skip + nTraits;
		int vacant = mod.getVacant();
		if (vacant >= 0) {
			// XXX what happens if one strategy is deactivated!?
			double z = state[vacant];
			double dz = 0.0;
			double drate = mod.getDeathRate();
			for (int n = skip; n < end; n++) {
				if (n == vacant) {
					fit[n] = 0.0;
					continue;
				}
				double dyn = state[n] * (z * fit[n] - drate);
				change[n] = dyn;
				dz -= dyn;
			}
			change[vacant] = dz;
			return change;
		}

		double err = 0.0;
		double noise, inoise;
		PlayerUpdateType put = mod.getPlayerUpdateType();
		switch (put) {
			case THERMAL: // fermi update
				// factor 2 enters - see e.g. Sigmund et al. Dyn Games & Appl. 2011
				// no scaling seems required for comparisons with simulations
				noise = mod.getPlayerUpdateNoise();
				if (noise <= 0.0) {
					// no noise
					for (int n = skip; n < end; n++) {
						double dyn = 0.0, ftn = fit[n];
						for (int i = skip; i < end; i++) {
							if (i == n)
								continue;
							dyn += state[i] * Math.signum(ftn - fit[i]);
						}
						dyn *= state[n];
						change[n] = dyn;
						err += dyn;
					}
					break;
				}
				// some noise
				inoise = 0.5 / noise;
				for (int n = skip; n < end; n++) {
					double dyn = 0.0, ftn = fit[n];
					for (int i = skip; i < end; i++) {
						if (i == n)
							continue;
						dyn += state[i] * Functions.tanh((ftn - fit[i]) * inoise);
					}
					dyn *= state[n];
					change[n] = dyn;
					err += dyn;
				}
				break;

			case BEST_RESPONSE: // best-response update
				bestResponse(mod, state, fit, nGroup, scratch, skip, change);
				for (int n = skip; n < end; n++) {
					double dyn = change[n] - state[n];
					change[n] = dyn;
					err += dyn;
				}
				break;

			case BEST: // imitate the better
			case BEST_RANDOM:
				for (int n = skip; n < end; n++) {
					double dyn = 0.0, ftn = fit[n];
					for (int i = skip; i < end; i++) {
						if (i == n || Math.abs(ftn - fit[i]) < 1e-8)
							continue;
						dyn += (ftn > fit[i] ? state[i] : -state[i]);
					}
					dyn *= state[n];
					change[n] = dyn;
					err += dyn;
				}
				break;

			// XXX 100531 implement! - defaults to replicator
			case PROPORTIONAL: // proportional update

				// NOTES:
				// - all rates are scaled by the maximum fitness difference among all species
				// to preserve their relative time scales.
			case IMITATE_BETTER: // replicator update
			case IMITATE:
				// if noise becomes very small, this should recover PLAYER_UPDATE_BEST
				inoise = invFitRange[idx];
				noise = mod.getPlayerUpdateNoise();
				if (noise > 0.0)
					inoise /= noise;
				for (int n = skip; n < end; n++) {
					double dyn = 0.0, ftn = fit[n];
					for (int i = skip; i < end; i++) {
						// lowering the threshold to 1e-8 results in one failing test (CDL-18-PDE.plist)
						if (i == n || Math.abs(ftn - fit[i]) < 1e-6)
							continue;
						// note: cannot use mean payoff as the transition probabilities must lie in
						// [0,1] - otherwise the timescale gets messed up.
						dyn += state[i] * Math.min(1.0, Math.max(-1.0, (ftn - fit[i]) * inoise));
					}
					dyn *= state[n];
					change[n] = dyn;
					err += dyn;
				}
				break;

			default:
				throw new Error("Unknown update method for players (" + put + ")");
		}

		// restrict to active strategies
		err /= mod.getNActive();
		// note float resolution is 1.1920929E-7
		if (Math.abs(err) > 1e-7) {
			boolean[] active = mod.getActiveTraits();
			for (int n = 0; n < nTraits; n++)
				if (active[n])
					change[skip + n] -= err;
		}
		return change;
	}

	/**
	 * ugly helper method to retrieve fitnesses...
	 *
	 * @param mod     the pointer to the current module
	 * @param state   the array of frequencies/densities denoting the state
	 *                population
	 * @param fit     the array of fitness values of types in population
	 * @param nGroup  the size of interaction group
	 * @param scratch the array to store intermediate calculations (for PDEs this
	 *                must be thread safe)
	 * @param skip    the index of first entry for this population in arrays above
	 */
	private void avgScores(Module mod, double[] state, double[] fit, int nGroup, double[] scratch, int skip) {
		int nTraits = mod.getNTraits();
		if (mod.isStatic()) {
			System.arraycopy(staticfit, skip, fit, skip, nTraits);
			return;
		}
		((HasODE) mod).avgScores(state, nGroup, scratch, skip);
		Map2Fitness map2fit = mod.getMapToFitness();
		for (int n = skip; n < skip + nTraits; n++)
			fit[n] = map2fit.map(scratch[n]);
	}

	/**
	 * Implementation of best-response dynamics (for frequencies)
	 * <p>
	 * <strong>Note:</strong> inactive strategies are excluded and do not qualify as
	 * a best response.
	 * <p>
	 * <strong>IMPORTANT:</strong> needs to be thread safe (must supply memory for
	 * calculations and results)
	 *
	 * @param mod      the reference to the current module
	 * @param freqs    the frequency/density of each trait/strategy
	 * @param fit      the array for storing the average payoffs/scores for each
	 *                 strategic type
	 * @param nGroup   the size of interaction group
	 * @param scratch  the array for storing intermediate results (for thread
	 *                 safety)
	 * @param skip     the entries to skip in arrays <code>freqs</code> and
	 *                 <code>fit</code>
	 * @param response the best-response(s)
	 */
	protected void bestResponse(Module mod, double[] freqs, double[] fit, int nGroup, double[] scratch, int skip,
			double[] response) {
		int nTraits = mod.getNTraits();
		int end = skip + nTraits;
		double min = Double.MAX_VALUE;
		double max = -Double.MAX_VALUE;
		int nMax = 0;
		boolean[] active = mod.getActiveTraits();
		for (int i = skip; i < end; i++) {
			response[i] = 0.0; // reset
			if (active != null && !active[i - skip])
				continue;
			double fiti = fit[i];
			if (min > fiti)
				min = fiti;
			// if difference is tiny, consider them equal
			if (Math.abs(fiti - max) < 1e-6) {
				response[i] = 1.0;
				max = Math.max(fiti, max); // just to avoid any drift
				nMax++;
				continue;
			}
			if (fiti > max) {
				// new max found - clear reply
				for (int j = skip; j < i; j++)
					response[j] = 0.0;
				response[i] = 1.0;
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
		// points where another strategy becomes the best response.
		if (diff < 1e-3 && diff * dtTry < accuracy) {
			System.arraycopy(freqs, skip, response, skip, nTraits);
			return;
		}
		if (nMax > 1) {
			double norm = 1.0 / nMax;
			for (int i = skip; i < end; i++)
				response[i] *= norm;
		}
	}

	@Override
	public void update() {
		getDerivatives(t, yt, ft, dyt, tmp);
	}

	@Override
	public String getStatus() {
		String status = "";
		int from = 0;
		boolean isMultispecies = (species.size() > 1);
		for (Module pop : species) {
			int to = from + pop.getNTraits();
			// omit status for vacant trait in density models
			int vacant = isDensity() ? from + pop.getVacant() : -1;
			String popStatus = "";
			for (int i = from; i < to; i++) {
				if (i == vacant)
					continue;
				popStatus += (popStatus.length() > 0 ? ", " : "") + names[i] + ": " //
						+ (isDensity() ? Formatter.format(yt[i], 1)
								: Formatter.formatPercent(yt[i], 1));
			}
			from = to;
			status += (isMultispecies ? (status.length() > 0 ? "<br/><i>" : "<i>") + pop.getName() + ":</i> " : "")
					+ popStatus;
		}
		return status;
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

	@Override
	public void setAccuracy(double acc) {
		if (acc <= 0.0)
			return;
		accuracy = acc;
	}

	@Override
	public double getAccuracy() {
		return accuracy;
	}

	@Override
	public void init() {
		init(true);
	}

	private void init(boolean doRandom) {
		t = 0.0;
		dtTry = dt;
		connect = false;
		converged = false;
		// PDE models have their own initialization types
		if (isModelType(Type.PDE))
			return;
		int idx = -1;
		// y0 is initialized except for species with random initial frequencies
		if (doRandom) {
			for (Module pop : species) {
				if (!initType[++idx].equals(InitType.RANDOM))
					continue;
				int dim = pop.getNTraits();
				RNGDistribution rng = engine.getRNG();
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
	 * <dd>Initial densities as specified in
	 * {@link org.evoludo.simulator.modules.Discrete#cloInit
	 * modules.Discrete.cloInit} (density modules).
	 * <dt>FREQUENCY
	 * <dd>Initial frequencies as specified in
	 * {@link org.evoludo.simulator.modules.Discrete#cloInit
	 * modules.Discrete.cloInit} (frequency modules).
	 * <dt>UNIFORM
	 * <dd>Uniform frequencies of traits (default; in density modules all densities
	 * are set to zero).
	 * <dt>RANDOM
	 * <dd>Random initial trait frequencies. <strong>Note:</strong> Not available
	 * for density based models.
	 * </dl>
	 * 
	 * @author Christoph Hauert
	 * 
	 * @see org.evoludo.simulator.modules.Discrete#cloInit Discrete.cloInit
	 * @see EvoLudo#cloInitType
	 * @see setInitType(CLOption.Key)
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
		RANDOM("random", "random initial frequencies");

		/**
		 * Key of initialization type. Used when parsing command line options.
		 * 
		 * @see EvoLudo#cloInitType
		 */
		String key;

		/**
		 * Brief description of initialization type for help display.
		 * 
		 * @see EvoLudo#helpCLO()
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
	}

	/**
	 * Parse initializer string {@code arg}. Determine type of initialization and
	 * process its arguments as appropriate.
	 * <p>
	 * <strong>Note:</strong> Not possible to perform parsing in {@code CLODelegate}
	 * of {@link #cloInitType} because PDE model provide their own
	 * {@link PDERD.InitType}s.
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
		for (Module pop : species) {
			String inittype = inittypes[idx % inittypes.length];
			double[] initargs = null;
			String[] typeargs = inittype.split("\\s+|=");
			InitType type = (InitType) cloInitType.match(inittype);
			// if matching of inittype failed assume it was omitted; use previous type
			if (type == null && idx > 0) {
				type = initType[idx - 1];
				initargs = CLOParser.parseVector(typeargs[0]);
			}
			else if (typeargs.length > 1)
				initargs = CLOParser.parseVector(typeargs[1]);
			int nTraits = pop.getNTraits();
			boolean success = false;
			switch (type) {
				case DENSITY:
				case FREQUENCY:
					if (initargs == null || initargs.length != nTraits)
						break;
					System.arraycopy(initargs, 0, y0, start, nTraits);
					success = true;
					break;
				case RANDOM:
				case UNIFORM:
				default:
					// uniform distribution is the default. for densities set all to zero.
					Arrays.fill(y0, start, start + nTraits, isDensity() ? 0.0 : 1.0);
					success = true;
					break;
			}
			if (!success) {
				type = InitType.UNIFORM;
				parseOk = false;
			}
			initType[idx] = type;
			idx++;
			start += nTraits;
		}
		if (!parseOk) {
			engine.getLogger().warning("parsing of initype(s) '" + arg + "' failed.");
			return false;
		}
		return true;
	}

	/**
	 * Command line option to set the time increment <code>dt</code> in DE models.
	 * If the requested <code>dt</code> turns out to be too big, e.g. due to
	 * diffusion in {@link org.evoludo.simulator.models.PDERD}, it is
	 * reduced appropriately. For {@link org.evoludo.simulator.models.ODERK}
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
	 * @see PDERD.InitType
	 */
	public final CLOption cloInitType = new CLOption("inittype", InitType.UNIFORM.getKey(), EvoLudo.catModule,
			"--inittype <t>  type of initial configuration", new CLODelegate() {
				@Override
				public boolean parse(String arg) {
					// parsing must be 'outsourced' to ODEEuler class to enable
					// PDE models to override it and do their own initialization.
					return ODEEuler.this.parse(arg);
				}

				@Override
				public void report(PrintStream output) {
					int idx = 0;
					for (Module pop : species) {
						InitType type = initType[idx++];
						String msg = "# inittype:             " + type;
						if (type.equals(InitType.DENSITY) || type.equals(InitType.FREQUENCY)) {
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

	/**
	 * Command line option to set the probability of mutations for
	 * population(s)/species.
	 */
	public final CLOption cloMutation = new CLOption("mutations", "-1", 
		EvoLudo.catModel, "--mutations <m[,m1,...]>  mutation probabilities",
			new CLODelegate() {

				/**
				 * {@inheritDoc}
				 * <p>
				 * Parse mutation probability(ies) for a single or multiple populations/species.
				 * {@code arg} can be a single value or an array of values with the
				 * separator {@value CLOParser#SPECIES_DELIMITER}. The parser cycles through
				 * {@code arg} until all populations/species have mutation probabilities
				 * rate set.
				 * <p>
				 * <strong>Note:</strong> Negative rates or invalid numbers (such as '-')
				 * disable mutations.
				 * 
				 * @param arg (array of) mutation probability(ies)
				 */
				@Override
				public boolean parse(String arg) {
					String[] mutations = arg.split(CLOParser.SPECIES_DELIMITER);
					String marg;
					for (int n = 0; n < species.size(); n++) {
						marg = mutations[n % mutations.length];
						try {
							mus[n] = Math.max(0.0, Double.parseDouble(marg));
						} catch (NumberFormatException nfe) {
							mus[n] = 0.0;
							engine.getLogger()
									.warning("mutation probabilities '" + marg + "' for trait "+n+" invalid - disabled.");
							return false;
						}
					}
					return true;
				}

				@Override
				public void report(PrintStream output) {
					boolean isMultispecies = species.size() > 1;
					int idx = 0;
					for (Module pop : species) {
						double mut = mus[idx++];
						if (mut > 0.0) {
							output.println("# mutation:             " + Formatter.formatSci(mut, 8)
									+ (isMultispecies ? " (" + pop.getName() + ")" : ""));
							continue;
						}
						output.println("# mutation:             none"
								+ (isMultispecies ? " (" + pop.getName() + ")" : ""));
					}
				}
			});

	@Override
	public void collectCLO(CLOParser parser) {
		parser.addCLO(cloAdjustedDynamics);
		parser.addCLO(cloDEAccuracy);
		parser.addCLO(cloDEdt);
		parser.addCLO(cloInitType);
		cloInitType.clearKeys();
		cloInitType.addKeys(InitType.values());
		if (isDensity()) {
			cloInitType.removeKey(InitType.RANDOM);
			cloInitType.removeKey(InitType.FREQUENCY);
		} else {
			cloInitType.removeKey(InitType.DENSITY);
		}
		if (permitsTimeReversal())
			parser.addCLO(cloTimeReversed);
		parser.addCLO(cloMutation);
	}

	@Override
	public void encodeState(StringBuilder plist) {
		plist.append(Plist.encodeKey("Model", getModelType().toString()));
		plist.append(Plist.encodeKey("Time", t));
		plist.append(Plist.encodeKey("Dt", dt));
		plist.append(Plist.encodeKey("Forward", forward));
		plist.append(Plist.encodeKey("AdjustedDynamics", isAdjustedDynamics));
		plist.append(Plist.encodeKey("Accuracy", accuracy));
		encodeStrategies(plist);
		encodeFitness(plist);
	}

	@Override
	public boolean restoreState(Plist plist) {
		boolean success = true;
		t = (Double) plist.get("Time");
		dt = (Double) plist.get("Dt");
		forward = (Boolean) plist.get("Forward");
		isAdjustedDynamics = (Boolean) plist.get("AdjustedDynamics");
		accuracy = (Double) plist.get("Accuracy");
		connect = false;
		if (!restoreStrategies(plist)) {
			engine.getLogger().warning("restore strategies in " + getModelType() + "-model failed.");
			success = false;
		}
		if (!restoreFitness(plist)) {
			engine.getLogger().warning("restore fitness in " + getModelType() + "-model failed.");
			success = false;
		}
		return success;
	}

	/**
	 * Encodes state of the model in the form of a <code>plist</code> string.
	 * 
	 * @param plist the string builder for the encoded state
	 */
	void encodeStrategies(StringBuilder plist) {
		plist.append(Plist.encodeKey("State", yt));
		plist.append(Plist.encodeKey("StateChange", dyt));
	}

	/**
	 * Restores the state encoded in <code>plist</code>.
	 * 
	 * @param plist the encoded state
	 * @return <code>true</code> if successful
	 */
	boolean restoreStrategies(Plist plist) {
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
