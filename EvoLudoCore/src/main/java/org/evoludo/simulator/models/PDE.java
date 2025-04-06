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
import java.util.Comparator;
import java.util.List;

import org.evoludo.math.ArrayMath;
import org.evoludo.simulator.ColorMap;
import org.evoludo.simulator.EvoLudo;
import org.evoludo.simulator.Geometry;
import org.evoludo.simulator.modules.Map2Fitness;
import org.evoludo.simulator.modules.Module;
import org.evoludo.util.CLOParser;
import org.evoludo.util.CLOption;
import org.evoludo.util.CLOption.CLODelegate;
import org.evoludo.util.Formatter;
import org.evoludo.util.Plist;

/**
 * Numerical integration of partial differential equations for
 * reaction-diffusion systems based on Euler's method (fixed step size)
 * <p>
 * <strong>Important:</strong> Currently multi-species modules are not supported
 * by PDE models.
 *
 * @author Christoph Hauert
 */
public class PDE extends ODE {

	/**
	 * Methods that every {@link Module} must implement, which advertises numerical
	 * solutions based on partial differential equations.
	 */
	public interface HasPDE extends HasDE {

		/**
		 * Provides opportunity for model to supply custom PDE implementation.
		 * <p>
		 * <strong>Important:</strong> if the custom PDE implementation involves random
		 * numbers, the shared random number generator should be used for
		 * reproducibility.
		 *
		 * @return custom PDE model or <code>null</code> to use use default
		 * 
		 * @see Model#rng
		 */
		public default Model createPDE() {
			return null;
		}
	}

	/**
	 * Convenience variable: module associated with this model (useful as long as
	 * PDE models are restricted to single species).
	 */
	protected Module module;

	/**
	 * The supervisor for the integration of the reaction-diffusion process. This
	 * abstraction is required due to incompatibilities between JRE and GWT
	 * implementations. In particular, JRE allows for parallel execution of certain
	 * steps, while GWT uses a scheduling mechanism.
	 * 
	 * @see org.evoludo.simulator.models.PDESupervisorJRE
	 * @see org.evoludo.simulator.models.PDESupervisorGWT
	 */
	protected PDESupervisor supervisor;

	/**
	 * Geometry representing the spatial dimensions of this PDE.
	 */
	protected Geometry space;

	/**
	 * Density distribution of traits as a 2D array. The first entry refers to the
	 * index of the node (e.g. location on lattice) and the second entry refers to
	 * the different traits at that location. {@link #space} defines the geometric
	 * arrangement of the nodes.
	 * <p>
	 * <strong>Note:</strong> this variable is <code>protected</code> to allow
	 * direct access from {@link PDESupervisor} for efficiency reasons.
	 */
	protected double[][] density;

	/**
	 * The next density/frequency distribution of traits as a 2D array. The first
	 * entry refers to the index of the node (e.g. location on lattice) and the
	 * second entry refers to the different traits at that location. {@link #space}
	 * defines the geometric arrangement of the nodes.
	 * <p>
	 * <strong>Note:</strong> this variable is <code>protected</code> to allow
	 * direct access from {@link PDESupervisor} for efficiency reasons.
	 */
	protected double[][] next;

	/**
	 * Fitness distribution of traits as a 2D array. The first entry refers to the
	 * index of the node (e.g. location on lattice) and the second entry refers to
	 * the different traits at that location. {@link #space} defines the geometric
	 * arrangement of the nodes.
	 * <p>
	 * <strong>Note:</strong> this variable is <code>protected</code> to allow
	 * direct access from {@link PDESupervisor} for efficiency reasons.
	 */
	protected double[][] fitness;

	/**
	 * Type of initial configuration for each species.
	 * <p>
	 * <strong>Note:</strong> this variable is deliberately hiding a field from
	 * {@link ODE} because PDE models have their own initialization types. Make
	 * sure {@code initType} is not accessed through calls to {@code super}. Since
	 * PDE's currently support only single species models {@code initType} is a
	 * scalar field.
	 * 
	 * @see #cloInit
	 */
	@SuppressWarnings("hiding")
	protected InitType initType;

	/**
	 * Discretization of space as the total number of spatial units. For example,
	 * for a square lattice the dimensions are
	 * <code>sqrt(discretization)×sqrt(discretization)</code>.
	 * 
	 * @see #cloPdeN
	 * @see #setDiscretization(int)
	 */
	protected int discretization;

	/**
	 * The linear extension of the lattice.
	 * 
	 * @see #cloPdeL
	 * @see #setLinearExtension(double)
	 */
	protected double linext;

	/**
	 * The array of diffusion coefficients for each trait.
	 * 
	 * @see #cloPdeDiffusion
	 * @see #setDiffusion(double[])
	 */
	protected double[] diffcoeff;

	/**
	 * The flag indicating whether preservation of symmetry in the dynamics is
	 * requested.
	 * The model may not be able to honour the request because it requires that the
	 * spatial structure permits preservation of symmetry, i.e. the underlying
	 * spatial geometry represents a lattice.
	 * 
	 * @see #cloPdeSymmetric
	 * @see #getSymmetric()
	 * @see #isSymmetric()
	 */
	boolean requestSymmetric = false;

	/**
	 * The flag indicating whether the dynamics is symmetric. This requires that
	 * symmetry preservation was requested, i.e. <code>requestSymmetric==true</code>
	 * and that the spatial structure permits preservation of symmetry, i.e. the
	 * underlying spatial geometry represents a lattice.
	 * 
	 * @see #cloPdeSymmetric
	 * @see #getSymmetric()
	 * @see #isSymmetric()
	 */
	boolean isSymmetric = false;

	/**
	 * The array containing the minimum densities of each trait. Used to set the
	 * color range when retrieving the density data.
	 * 
	 * @see #getTraitData(int, Object[], ColorMap)
	 */
	protected double[] minDensity;

	/**
	 * The array containing the maximum densities of each trait. Used to set the
	 * color range when retrieving the density data.
	 * 
	 * @see #getTraitData(int, Object[], ColorMap)
	 */
	protected double[] maxDensity;

	/**
	 * The array containing the mean densities of each trait.
	 * 
	 * @see #getMeanTraits(int, double[])
	 */
	protected double[] meanDensity;

	/**
	 * The array containing the minimum fitness of each trait. Used to set the color
	 * range when retrieving the density data.
	 * 
	 * @see #getFitnessData(int, Object[],
	 *      org.evoludo.simulator.ColorMap.Gradient1D)
	 */
	protected double[] minFitness;

	/**
	 * The array containing the maximum fitness of each trait. Used to set the color
	 * range when retrieving the density data.
	 * 
	 * @see #getFitnessData(int, Object[],
	 *      org.evoludo.simulator.ColorMap.Gradient1D)
	 */
	protected double[] maxFitness;

	/**
	 * The array containing the mean fitness of each trait.
	 * 
	 * @see #getMeanFitness(int, double[])
	 */
	protected double[] meanFitness;

	// protected double[] scaleMin, scaleMax;
	// protected boolean[] scaleAuto;

	/**
	 * The index of the dependent trait. Dependent traits make only sense in
	 * replicator type model with frequencies, i.e. normalized states. The dependent
	 * trait is the one that gets derived from the changes in all others in order to
	 * maintain normalization.
	 * <p>
	 * <strong>Note:</strong> This is a simple helper variable and shortcut for
	 * <code>dependents[0]</code>, which makes sense as long as this model cannot
	 * deal with multi-species modules.
	 * 
	 * @see Module#getDependent()
	 */
	protected int dependent = -1;

	/**
	 * Helper variable to store the effective diffusion terms. This depends not only
	 * on the diffusion coefficients {@link #diffcoeff} but also on the time
	 * increment {@link ODE#dt dt} and the linear extension, {@link #linext}.
	 * 
	 * @see #initDiffusion(double)
	 */
	protected double[] alpha;

	/**
	 * In order to preserve symmetry the densities of neighbouring cells in the
	 * diffusion step need to be sorted. The sorting criteria are essentially
	 * irrelevant as long as they are consistently applied.
	 */
	Comparator<double[]> sorting;

	/**
	 * Constructs a new model for the numerical integration of the system of partial
	 * differential equations representing the dynamics specified by the
	 * {@link Module} <code>module</code> using the {@link EvoLudo} pacemaker
	 * <code>engine</code> to control the numerical evaluations. The integrator
	 * implements reaction-diffusion steps based on Euler's method (fixed step
	 * size).
	 * <p>
	 * <strong>Important:</strong> for reproducibility the shared random number
	 * generator should be used (in PDE's only used to generate random initial
	 * configurations).
	 * <p>
	 * <strong>Note:</strong> requires a supervisor that matches the implementation,
	 * i.e. JRE or GWT, respectively, to properly deal with multiple threads or
	 * scheduling.
	 * 
	 * @param engine the pacemaker for running the model
	 * 
	 * @see EvoLudo#getRNG()
	 * @see org.evoludo.simulator.models.PDESupervisorGWT PDESupervisorGWT
	 * @see org.evoludo.simulator.models.PDESupervisorJRE PDESupervisorJRE
	 */
	public PDE(EvoLudo engine) {
		super(engine);
		type = Type.PDE;
	}

	/**
	 * Gets the geometry representing the spatial structure of this PDE.
	 * 
	 * @return the geometry of the PDE
	 */
	public Geometry getGeometry() {
		return space;
	}

	@Override
	public void load() {
		super.load();
		if (supervisor == null)
			supervisor = engine.hirePDESupervisor(this);
		module = engine.getModule();
		space = module.createGeometry();
		sorting = new Comparator<double[]>() {

			/**
			 * {@inheritDoc}
			 * <p>
			 * Sort arrays of {@code double}s simply based on the their first component.
			 * Good enough for our purposes.
			 */
			@Override
			public int compare(double[] o1, double[] o2) {
				return (int) Math.signum(o1[0] - o2[0]);
			}
		};
	}

	@Override
	public synchronized void unload() {
		supervisor.unload();
		supervisor = null;
		space = null;
		density = null;
		next = null;
		fitness = null;
		sorting = null;
		module = null;
		super.unload();
	}

	@Override
	public boolean check() {
		boolean doReset = false;
		if (species.size() > 1) {
			logger.warning("PDE model for inter-species interactions not (yet?) implemented - reverting to ODE.");
			engine.loadModel(Type.ODE);
			return true;
		}
		if (space.getType() == Geometry.Type.MEANFIELD || space.getType() == Geometry.Type.COMPLETE) {
			logger.warning("unstructured population - reverting to ODE.");
			engine.loadModel(Type.ODE);
			return true;
		}
		doReset |= space.setSize(discretization);
		doReset |= space.check();
		if (doReset)
			space.init();
		// need to init space first
		doReset = super.check();
		// shortcut since this is only single species - at least for now
		dependent = dependents[0];
		// some careful checking for suitable time steps is required!
		checkDt();
		// initialize scaled diffusion constant
		initDiffusion(dt);
		// check if diffusion can and should preserve symmetry
		// only makes sense on lattices but regardless of boundary conditions
		// note: here we can only check geometry parameters, features such as
		// space.isLattice are only available after geometry is initialized.
		isSymmetric = (requestSymmetric && space.isLattice());
		if (requestSymmetric != isSymmetric)
			logger.warning("request to preserve symmetry cannot be honoured.");
		return doReset;
	}

	@Override
	public void reset() {
		super.reset();
		if (space.getType() == Geometry.Type.MEANFIELD)
			return;
		space.init();
		if (fitness == null || fitness.length != space.size || fitness[0].length != nDim) {
			fitness = new double[space.size][nDim];
			density = new double[space.size][nDim];
			next = new double[space.size][nDim];
		}
		if (minDensity == null || minDensity.length != nDim) {
			minDensity = new double[nDim];
			maxDensity = new double[nDim];
			meanDensity = new double[nDim];
			minFitness = new double[nDim];
			maxFitness = new double[nDim];
			meanFitness = new double[nDim];
			// scaleAuto = new boolean[d];
			// Arrays.fill(scaleAuto, true);
		}
		supervisor.reset();
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * For PDEs simply delegate to supervisor for dealing with multiple threads
	 * (JRE) or scheduling (GWT).
	 *
	 */
	@Override
	public void update() {
		supervisor.update();
	}

	/**
	 * The generation when the model execution is halted next. This is needed to
	 * keep track of halting for asynchronous execution in GWT.
	 */
	double gwtHalt;

	@Override
	public boolean useScheduling() {
		return EvoLudo.isGWT;
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * For PDEs simply delegate to supervisor for dealing with multiple threads
	 * (JRE) or scheduling (GWT).
	 */
	@Override
	public boolean next() {
		gwtHalt = getNextHalt();
		// continue if milestone reached in previous step, i.e. deltat < 1e-8
		double step = timeStep;
		double deltat = Math.abs(gwtHalt - time);
		if (deltat >= 1e-8)
			step = Math.min(step, deltat);
		connect = true;
		// note: returns immediately for asynchronous execution (GWT)
		supervisor.next(step);
		// important: for GWT condition never holds because t unchanged
		if (Math.abs(gwtHalt - time) < 1e-8)
			return false;
		return !converged;
	}

	/**
	 * Reaction step. Update cells with indices between <code>start</code>
	 * (including) and <code>end</code> (excluding) and return the accumulated/total
	 * change in state.
	 * <p>
	 * <strong>Note:</strong> At the end, the state in <code>density</code> is
	 * unchanged, the new density distribution is in <code>next</code> and the
	 * fitness matching <code>density</code> is updated.
	 * <p>
	 * <strong>Important:</strong> must be thread safe for JRE. In particular, no
	 * memory can be shared with anyone else!
	 *
	 * @param start the index of the first cell (including)
	 * @param end   the index of the last cell (excluding)
	 * @return the accumulated change in state
	 */
	public double react(int start, int end) {
		double[] minFit = new double[nDim];
		Arrays.fill(minFit, Double.MAX_VALUE);
		double[] maxFit = new double[nDim];
		Arrays.fill(maxFit, -Double.MAX_VALUE);
		double[] meanFit = new double[nDim];
		double[] dy = new double[nDim];
		double change = 0.0;

		if (dependent < 0) {
			for (int n = start; n < end; n++) {
				double[] ds = density[n];
				double[] s = next[n]; // s is only a short-cut - data written to s is stored in next[]
				double[] f = fitness[n];
				getDerivatives(time, ds, f, dy);
				for (int i = 0; i < nDim; i++) {
					double dyidt = dy[i] * dt;
					s[i] = ds[i] + dyidt;
					// update extrema and mean fitness
					double fit = f[i];
					minFit[i] = Math.min(fit, minFit[i]);
					maxFit[i] = Math.max(fit, maxFit[i]);
					meanFit[i] += fit;
					// use dyidt to check for convergence; track mean, min and max densities
					change += dyidt * dyidt;
				}
			}
			// supervisor.updateFitness(minFit, maxFit, meanFit, change);
			updateFitness(minFit, maxFit, meanFit);
			return change;
		}
		// dependent scenario - dependent>0
		for (int n = start; n < end; n++) {
			double[] ds = density[n];
			double[] s = next[n]; // s is only a short-cut - data written to s is stored in next[]
			double[] f = fitness[n];
			getDerivatives(time, ds, f, dy);
			for (int i = 0; i < nDim; i++) {
				double dyidt = dy[i] * dt;
				s[i] = ds[i] + dyidt;
				// update extrema and mean fitness
				double fit = f[i];
				minFit[i] = Math.min(fit, minFit[i]);
				maxFit[i] = Math.max(fit, maxFit[i]);
				meanFit[i] += fit;
				// use dyidt to check for convergence; track mean, min and max densities
				change += dyidt * dyidt;
			}
		}
		updateFitness(minFit, maxFit, meanFit);
		return change;
	}

	/**
	 * Update minimum, maximum and mean fitnesses during to reaction step. In multi
	 * threaded settings each worker reports the minima {@code min}, maxima
	 * {@code max}, and the total {@code mean} fitness (for the calculation of the
	 * mean) of the PDE units it is responsible for.
	 * 
	 * @param min  the array with fitness minima
	 * @param max  the array with fitness maxima
	 * @param mean the array with fitness means
	 */
	public synchronized void updateFitness(double[] min, double[] max, double[] mean) {
		ArrayMath.min(minFitness, min);
		ArrayMath.max(maxFitness, max);
		ArrayMath.add(meanFitness, mean);
	}

	/**
	 * Normalizes the mean fitnesses after the reaction step is complete.
	 */
	public synchronized void normalizeMeanFitness() {
		ArrayMath.multiply(meanFitness, 1.0 / space.size);
	}

	/**
	 * Resets minimum, maximum and mean fitnesses prior to reaction step.
	 */
	public synchronized void resetFitness() {
		Arrays.fill(minFitness, Double.MAX_VALUE);
		Arrays.fill(maxFitness, -Double.MAX_VALUE);
		Arrays.fill(meanFitness, 0.0);
	}

	/**
	 * Diffusion step. Update cells with indices between <code>start</code>
	 * (including) and <code>end</code> (excluding). In order to preserve symmetry,
	 * if requested and possible, the neighbouring cells are sorted according to
	 * their density before the diffusion step is performed. The sorting is fairly
	 * expensive in terms of CPU time but it doesn't matter whether the sorting is
	 * ascending or descending.
	 * <p>
	 * <strong>Note:</strong> At the end, the state in <code>next</code> is
	 * unchanged, the new density distribution is in <code>density</code> and the
	 * fitness is untouched/unused.
	 * <p>
	 * <strong>Important:</strong> must be thread safe for JRE. In particular, no
	 * memory can be shared with anyone else!
	 *
	 * @param start the index of the first cell (including)
	 * @param end   the index of the last cell (excluding)
	 */
	public void diffuse(int start, int end) {
		double[] minDens = new double[nDim];
		Arrays.fill(minDens, Double.MAX_VALUE);
		double[] maxDens = new double[nDim];
		Arrays.fill(maxDens, -Double.MAX_VALUE);
		double[] meanDens = new double[nDim];
		int[][] in = space.in;

		if (isSymmetric) {
			double[][] sort = new double[space.maxIn][];
			double[] si = new double[nDim];
			for (int n = start; n < end; n++) {
				int[] neighs = in[n];
				int nIn = space.kin[n];
				double[] sn = next[n]; // current state of focal site sn
				double[] s = density[n]; // next state
				ArrayMath.multiply(sn, -space.kout[n], s); // s = -k*sn
				// sort neighbours
				for (int i = 0; i < nIn; i++) {
					double[] p = next[neighs[i]];
					sort[i] = p;
				}
				// sorting must maintain integrity of densities at neighbouring sites
				// (sorting based on first element is enough - only equality in the first
				// density but not the others could still result in an eventual break of
				// symmetry due to rounding error.)
				Arrays.sort(sort, 0, nIn, sorting);
				// loop over neighbours
				for (int i = 0; i < nIn; i++) {
					si = sort[i];
					// diffusion
					ArrayMath.add(s, si); // s += si
				}
				ArrayMath.multiply(s, alpha); // s *= alpha
				ArrayMath.add(s, sn); // s += sn
				if (dependent >= 0)
					s[dependent] = 1.0 + s[dependent] - ArrayMath.norm(s);
				// update extrema and mean density
				minmaxmean(s, minDens, maxDens, meanDens);
			}
			updateDensity(minDens, maxDens, meanDens);
			return;
		}

		for (int n = start; n < end; n++) {
			int[] neighs = in[n];
			int nIn = space.kin[n];
			double[] ds = next[n];
			double[] s = density[n];
			ArrayMath.multiply(ds, -space.kout[n], s); // s = -kout*ds[n], current density in ds
			for (int i = 0; i < nIn; i++)
				ArrayMath.add(s, next[neighs[i]]); // s += sum ds[i], where i are neighbours of n
			ArrayMath.multiply(s, alpha); // s *= alpha, s is change in density
			ArrayMath.add(s, ds); // s += ds, new density now in s
			if (dependent >= 0)
				s[dependent] = 1.0 + s[dependent] - ArrayMath.norm(s);
			// update extrema and mean density // min_s:
			// min_dens+(min_kin*min_dens-max_kout*max_dens)*alpha>0
			minmaxmean(s, minDens, maxDens, meanDens); // max_s:
														// max_dens+(max_kin*max_dens-min_kout*min_dens)*alpha<1
														// (with dependent)
		}
		updateDensity(minDens, maxDens, meanDens);
	}

	/**
	 * Resets minimum, maximum and mean density prior to diffusion step.
	 */
	public synchronized void resetDensity() {
		Arrays.fill(minDensity, Double.MAX_VALUE);
		Arrays.fill(maxDensity, -Double.MAX_VALUE);
		Arrays.fill(meanDensity, 0.0);
	}

	/**
	 * Initializes minimum, maximum and mean density based on current state.
	 */
	public synchronized void setDensity() {
		resetDensity();
		for (int n = 0; n < space.size; n++)
			minmaxmean(density[n], minDensity, maxDensity, meanDensity);
		normalizeMeanDensity();
	}

	/**
	 * Update minimum, maximum and mean density during to reaction step. In multi
	 * threaded settings each worker reports the minima {@code min}, maxima
	 * {@code max}, and the total {@code mean} density (for the calculation of the
	 * mean) of the PDE units it is responsible for.
	 * 
	 * @param min  the array with density minima
	 * @param max  the array with density maxima
	 * @param mean the array with density means
	 */
	public synchronized void updateDensity(double[] min, double[] max, double[] mean) {
		ArrayMath.min(minDensity, min);
		ArrayMath.max(maxDensity, max);
		ArrayMath.add(meanDensity, mean);
	}

	/**
	 * Normalizes the mean density after the diffusion step is complete.
	 */
	public synchronized void normalizeMeanDensity() {
		ArrayMath.multiply(meanDensity, 1.0 / space.size);
	}

	@Override
	public boolean getMeanTraits(double[] mean) {
		System.arraycopy(meanDensity, 0, mean, 0, nDim);
		return connect;
	}

	@Override
	public boolean getMeanTraits(int id, double[] mean) {
		System.arraycopy(meanDensity, 0, mean, 0, nDim);
		return connect;
	}

	@Override
	public double[] getMeanTraitAt(int id, int idx) {
		return density[idx];
	}

	@Override
	public String getTraitNameAt(int id, int idx) {
		return Formatter.formatFix(density[idx], 3);
	}

	@Override
	public synchronized <T> void getTraitData(int ID, T[] colors, ColorMap<T> colorMap) {
		int idx = 0;
		// XXX autoscale is currently the only option
		// boolean[] autoScale = pmodel.getAutoScale();
		// double[] min = pmodel.getMinScale();
		// double[] max = pmodel.getMaxScale();
		if (dependent >= 0 && nDim == 2) {
			int n = (dependent + 1) % nDim;
			// cast should be ok because Gradient1D color map (nDim == 2 with dependent
			// trait)
			ColorMap.Gradient1D<T> cMap = (ColorMap.Gradient1D<T>) colorMap;
			// if( autoScale[idx] )
			cMap.setRange(minDensity[n], maxDensity[n]);
			// else
			// cMap.setRange(min[n], max[n]);
			colorMap.translate(density, colors);
			return;
		}
		for (int n = 0; n < nDim; n++) {
			if (n == dependent)
				continue;
			// cast should be ok because Gradient2D color map (nDim == 2 without dependent
			// trait
			// or nDim > 2 with/out depenent trait). GradientND is subclass of Gradient2D
			ColorMap.Gradient2D<T> cMap = (ColorMap.Gradient2D<T>) colorMap;
			// if( autoScale[idx] )
			cMap.setRange(minDensity[n], maxDensity[n], idx);
			// else
			// cMap.setRange(min[n], max[n], idx);
			idx++;
		}
		colorMap.translate(density, colors, dependent);
	}

	@Override
	public boolean getMeanFitness(int id, double[] mean) {
		double sum = 0.0;
		for (int n = 0; n < nDim; n++) {
			double ftn = meanFitness[n];
			mean[n] = ftn;
			sum += ftn * meanDensity[n];
		}
		mean[nDim] = sum;
		return connect;
	}

	@Override
	public double[] getMeanFitnessAt(int id, int idx) {
		return fitness[idx];
	}

	@Override
	public String getFitnessNameAt(int id, int idx) {
		return Formatter.formatFix(fitness[idx], 3);
	}

	@Override
	public synchronized <T> void getFitnessData(int id, T[] colors, ColorMap.Gradient1D<T> colorMap) {
		// auto scale fitness if at least one trait has auto scale set
		// recall, fitness data uses 1D gradient
		// autoscale is currently the only option
		// if( ArrayMath.max(pde.getAutoScale()) )
		// colorMap.setRange(ArrayMath.min(pmodel.getMinValue()),
		// ArrayMath.max(pmodel.getMaxValue()));
		// important: min/max doesn't work with dependent trait because fitness includes
		// all traits but scaling should only be based on the independent traits; if
		// vacancies are possible fitness range is extended because vacant sites have
		// fitness zero
		double minf = Double.MAX_VALUE;
		double maxf = -Double.MAX_VALUE;
		double mind = Double.MAX_VALUE;
		double maxd = -Double.MAX_VALUE;
		Module pop = module.getSpecies(id);
		double nTraits = pop.getNTraits();
		for (int n = 0; n < nTraits; n++) {
			if (n == dependent)
				continue;
			minf = Math.min(minf, minFitness[n]);
			maxf = Math.max(maxf, maxFitness[n]);
			mind = Math.min(mind, minDensity[n]);
			maxd = Math.max(maxd, maxDensity[n]);
		}
		if (pop.getVacant() < 0)
			colorMap.setRange(minf * mind, nTraits * maxf * maxd);
		else
			colorMap.setRange(minf * mind, (nTraits - 1) * maxf * maxd);
		// else
		// colorMap.setRange(minScore, maxScore);
		colorMap.translate(density, fitness, colors);
	}

	@Override
	public synchronized void getFitnessHistogramData(int id, double[][] bins) {
		int nBins = bins[0].length;
		int maxBin = nBins - 1;
		// for neutral selection maxScore==minScore! in that case assume range
		// [score-1, score+1]
		// needs to be synchronized with GUI (e.g. MVFitness, MVFitHistogram, ...)
		// this needs to be revised - module should be able to supply min/max
		// scores/fitness and whether neutral
		// the following is not completely water tight (accumulated scores may cause
		// issues because the range depends on geometry)
		Module pop = module.getSpecies(id);
		Map2Fitness map2fit = pop.getMapToFitness();
		double min = map2fit.map(pop.getMinGameScore());
		double max = map2fit.map(pop.getMaxGameScore());
		double map;
		if (max - min < 1e-8) {
			// close enough to neutral
			map = nBins * 0.5;
			min--;
		} else
			map = nBins / (max - min);
		int idx = 0;
		int vacant = pop.getVacant();
		// clear bins
		for (int n = 0; n < bins.length; n++)
			Arrays.fill(bins[n], 0.0);
		for (int n = 0; n < discretization; n++) {
			double f[] = fitness[n];
			double d[] = density[n];
			idx = 0;
			for (int i = 0; i < nDim; i++) {
				if (i == vacant)
					continue;
				int bin = (int) ((f[idx] - min) * map);
				bin = Math.max(0, Math.min(maxBin, bin));
				bins[idx][bin] += d[idx];
				idx++;
			}
		}
		double norm = 1.0 / discretization;
		idx = 0;
		for (int n = 0; n < nDim; n++) {
			if (n == vacant)
				continue;
			ArrayMath.multiply(bins[idx++], norm);
		}
	}

	@Override
	public String getStatus() {
		String status = "";
		int idx = 0;
		int offset = 0;
		for (Module pop : species) {
			int dim = pop.getNTraits();
			int myDep = dependents[idx++];
			double depFreq = 1.0;
			String popStatus = "";
			for (int i = offset; i < offset + dim; i++) {
				if (i == myDep)
					continue;
				double dens = meanDensity[i];
				popStatus += (popStatus.length() > 0 ? ", " : "") + names[i] + ": " //
						+ Formatter.formatPercent(dens, 1);
				depFreq -= dens;
			}
			if (myDep >= 0)
				popStatus += (popStatus.length() > 0 ? ", " : "") + names[offset + myDep] + ": "
						+ Formatter.formatPercent(depFreq, 1);
			offset += dim;
			status += (isMultispecies ? (status.length() > 0 ? "<br/><i>" : "<i>") + pop.getName() + ":</i> " : "")
					+ popStatus;
		}
		return status;
	}

	@Override
	public boolean isMonomorphic() {
		return false;
	}

	/**
	 * Sets the number of units used to discretize the spatial dimensions for
	 * numerical integration of the PDE.
	 * 
	 * @param d the number of units
	 * 
	 * @see #cloPdeN
	 */
	public void setDiscretization(int d) {
		discretization = d;
	}

	/**
	 * Gets the number of units used to discretize the spatial dimensions for
	 * numerical integration of the PDE.
	 * 
	 * @return the number of units to discretize spatial
	 */
	public int getDiscretization() {
		return discretization;
	}

	/**
	 * Sets the linear extension of the lattice.
	 * <p>
	 * <strong>Note:</strong> For geometries other than 1D, 2D or 3D lattices the
	 * meaning of this parameter is unclear/undefined.
	 * 
	 * @param l the linear extension of the lattice
	 * 
	 * @see #cloPdeL
	 */
	public void setLinearExtension(double l) {
		if (l <= 0)
			return;
		linext = l;
	}

	/**
	 * Gets the linear extension of the lattice.
	 * <p>
	 * <strong>Note:</strong> For geometries other than 1D, 2D or 3D lattices the
	 * meaning of this parameter is unclear/undefined.
	 * 
	 * @return the linear extension
	 */
	public double getLinearExtension() {
		return linext;
	}

	/**
	 * Sets the diffusion coefficients for each trait.
	 * 
	 * @param dc the array of diffusion coefficients
	 * 
	 * @see #cloPdeDiffusion
	 */
	public void setDiffusion(double[] dc) {
		// the number of traits not yet set - retrieve directly from module
		int dim = module.getNTraits();
		if (diffcoeff == null || diffcoeff.length != dim)
			diffcoeff = new double[dim];
		if (dc == null) {
			// set default values for diffusion
			Arrays.fill(diffcoeff, 1.0);
			return;
		}
		int idx = 0;
		// the dependent trait not yet set - retrieve directly from module
		int depTrait = module.getDependent();
		for (int n = 0; n < dim; n++) {
			if (n == depTrait) {
				diffcoeff[n] = 0.0;
				continue;
			}
			diffcoeff[n] = dc[idx % dc.length];
			idx++;
		}
	}

	/**
	 * Gets the diffusion coefficients for all traits as an array.
	 * 
	 * @return the array of diffusion coefficients
	 */
	public double[] getDiffusion() {
		return diffcoeff;
	}

	/**
	 * Sets whether symmetries should be preserved. Not all models may be able to
	 * honour the request. For example {@link PDE} is only able to preserve
	 * symmetries in the diffusion step if the
	 * {@link Geometry#isLattice()} returns <code>true</code>.
	 *
	 * @param symmetric the request to preserve symmetry
	 */
	public void setSymmetric(boolean symmetric) {
		requestSymmetric = symmetric;
	}

	/**
	 * Gets whether preservation of symmetry was requested. Not all models may be
	 * able to honour the request.
	 * 
	 * @return {@code true} if symmetry preservation was requested
	 * 
	 * @see #setSymmetric(boolean)
	 */
	public boolean getSymmetric() {
		return requestSymmetric;
	}

	/**
	 * Gets whether the model preserves symmetry. Requires that symmetry
	 * preservation is requested <em>and</em> the model is able to honour the
	 * request.
	 *
	 * @return <code>true</code> if symmetry is preserved
	 */
	public boolean isSymmetric() {
		return isSymmetric;
	}

	/**
	 * Increments time by <code>incr</code>. This is used by the
	 * {@link PDESupervisor} to report back on the progress.
	 *
	 * @param incr the time that has elapsed
	 * @return {@code true} to continue and {@code false} to request a stop
	 */
	public boolean incrementTime(double incr) {
		time += incr;
		return (Math.abs(gwtHalt - time) > 1e-8);
	}

	/**
	 * Indicates that the numerical integration has converged to a homogeneous
	 * state. This is used by the {@link PDESupervisor} to report back on the
	 * progress.
	 */
	public void setConverged() {
		converged = true;
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * Due to diffusion PDE models do not permit time reversal.
	 */
	@Override
	public boolean permitsTimeReversal() {
		return false;
	}

	// @Override
	// public boolean[] getAutoScale() {
	// return scaleAuto;
	// }
	//
	// @Override
	// public double[] getMinScale() {
	// return scaleMin;
	// }
	//
	// @Override
	// public double[] getMaxScale() {
	// return scaleMax;
	// }
	// /**
	// *
	// * @param auto
	// * @param min
	// * @param max
	// * @return
	// */
	// public void setColorScaling(boolean[] auto, double[] min, double[] max) {
	// if (scaleAuto == null || scaleAuto.length != dim) {
	// scaleAuto = new boolean[d];
	// scaleMin = new double[d];
	// scaleMax = new double[d];
	// Arrays.fill(scaleAuto, true);
	// Arrays.fill(scaleMin, 0.0);
	// Arrays.fill(scaleMax, 1.0);
	// }
	// for (int n = 0; n < d; n++) {
	// scaleMin[n] = min[n];
	// scaleMax[n] = max[n];
	// scaleAuto[n] = auto[n];
	// }
	// }

	// /**
	// *
	// * @param sAuto
	// * @param sMin
	// * @param sMax
	// */
	// public void getColorScaling(boolean[] auto, double[] min, double[] max) {
	// System.arraycopy(scaleMin, 0, min, 0, min.length);
	// System.arraycopy(scaleMax, 0, max, 0, max.length);
	// System.arraycopy(scaleAuto, 0, auto, 0, auto.length);
	// }

	@Override
	public void init() {
		super.init();
		// RD_INIT_SQUARE and RD_INIT_CIRCLE only available on lattices
		InitType itype = initType;
		if (!space.isLattice()) {
			// some initialization types make only sense on lattices
			if (itype == InitType.CIRCLE || itype == InitType.SQUARE || itype == InitType.GAUSSIAN
					|| itype == InitType.RING)
				itype = InitType.UNIFORM;
		}

		boolean isCircular = false;
		switch (itype) {
			default:
			case UNIFORM:
				for (int n = 0; n < space.size; n++)
					System.arraycopy(y0, 0, density[n], 0, nDim);
				break;

			case PERTURBATION:
				for (int n = 0; n < space.size; n++)
					System.arraycopy(y0, 0, density[n], 0, nDim);
				double[] disturb = new double[nDim];
				if (dependent >= 0) {
					// flip frequencies and normalize
					ArrayMath.multiply(y0, -1.0, disturb);
					ArrayMath.add(disturb, 1.0);
					ArrayMath.normalize(disturb);
				} else {
					ArrayMath.multiply(y0, 1.2, disturb);
				}
				System.arraycopy(disturb, 0, density[space.size / 2], 0, nDim);
				break;

			case RANDOM:
				if (dependent >= 0) {
					for (int n = 0; n < space.size; n++) {
						double[] ds = density[n]; // ds is only a short-cut - data written to ds is stored in density[]
						for (int i = 0; i < nDim; i++) {
							double dsi = rng.random01() * y0[i];
							ds[i] = dsi;
						}
						if (dependent >= 0)
							ArrayMath.normalize(ds);
					}
					break;
				}
				for (int n = 0; n < space.size; n++) {
					double[] ds = density[n]; // ds is only a short-cut - data written to ds is stored in density[]
					for (int i = 0; i < nDim; i++) {
						ds[i] = rng.random01() * y0[i];
					}
				}
				break;

			case CIRCLE:
				isCircular = true;
				//$FALL-THROUGH$

			case SQUARE:
				if (dependent >= 0) {
					int len = density[0].length;
					double[] empty = new double[len];
					empty[module.VACANT < 0 ? dependent : module.VACANT] = 1.0;
					for (int n = 0; n < space.size; n++)
						System.arraycopy(empty, 0, density[n], 0, len);
				} else {
					for (int n = 0; n < space.size; n++)
						Arrays.fill(density[n], 0.0);
				}
				switch (space.getType()) {
					case CUBE:
						int l = 50;
						int m = 25;
						int mz = 5;
						if (space.size != 25000) { // not NOVA
							l = (int) (Math.pow(space.size, 1.0 / 3.0) + 0.5);
							m = l / 2;
							mz = m;
						}
						int l2 = l * l;
						int dd = Math.max(1, l / 5);
						int r = nDim / 2;
						double r2 = (dd * dd) * 0.25;
						for (int z = -r; z <= r; z++)
							for (int y = -r; y <= r; y++)
								for (int x = -r; x <= r; x++) {
									if (isCircular && x * x + y * y + z * z >= r2)
										continue;
									System.arraycopy(y0, 0, density[(mz + z) * l2 + (m + y) * l + m + x], 0, nDim);
								}
						break;

					case LINEAR:
						dd = Math.max(2, space.size / 10);
						dd -= space.size % 2 - dd % 2;
						m = (space.size - dd) / 2;
						for (int n = m; n < m + dd; n++)
							System.arraycopy(y0, 0, density[n], 0, nDim);
						break;

					default: // for square, triangular and hexagonal lattices
						l = (int) Math.floor(Math.sqrt(space.size) + 0.5);
						m = l / 2;
						dd = Math.max(1, l / 5);
						r = dd / 2;
						r2 = (dd * dd) * 0.25;
						for (int y = -r; y <= r; y++)
							for (int x = -r; x <= r; x++) {
								if (isCircular && x * x + y * y >= r2)
									continue;
								System.arraycopy(y0, 0, density[(m + y) * l + m + x], 0, nDim);
							}
				}
				break;

			case GAUSSIAN:
				switch (space.getType()) {
					case CUBE:
						int l = 50;
						int lz = 10;
						if (space.size != 25000) { // not NOVA
							l = (int) (Math.pow(space.size, 1.0 / 3.0) + 0.5);
							lz = l;
						}
						double m = (l - 1) * 0.5;
						double mz = (lz - 1) * 0.5;
						int l2 = l * l;
						double norm = 1.0 / l;
						for (int z = 0; z < lz; z++) {
							double z2 = (z - mz) * (z - mz);
							for (int y = 0; y < l; y++) {
								double y2 = (y - m) * (y - m);
								for (int x = 0; x < l; x++) 
									scaleDensity(density[z * l2 + y * l + x], 
											Math.exp(-((x - m) * (x - m) + y2 + z2) * norm));
							}
						}
						break;

					case LINEAR:
						l = space.size;
						m = l * 0.5;
						norm = 1.0 / l;
						for (int x = 0; x < l; x++) {
							double dens = Math.exp(-(x - m) * (x - m) * norm);
							ArrayMath.multiply(y0, dens, density[x]);
						}
						break;

					default: // for square, triangular and hexagonal lattices
						l = (int) Math.floor(Math.sqrt(space.size) + 0.5);
						m = (l - 1) * 0.5;
						norm = 1.0 / l;
						for (int y = 0; y < l; y++) {
							double y2 = (y - m) * (y - m);
							for (int x = 0; x < l; x++) 
								scaleDensity(density[y * l + x], 
										Math.exp(-((x - m) * (x - m) + y2) * norm));
						}
				}
				break;

			case RING:
				switch (space.getType()) {
					case CUBE:
						int l = 50;
						int lz = 10;
						if (space.size != 25000) { // not NOVA
							l = (int) (Math.pow(space.size, 1.0 / 3.0) + 0.5);
							lz = l;
						}
						double m = (l - 1) * 0.5;
						double mz = (lz - 1) * 0.5;
						int l2 = l * l;
						double m3 = m * 0.333;
						double norm = 1.0 / l;
						for (int z = 0; z < lz; z++) {
							double z2 = (z - mz) * (z - mz);
							for (int y = 0; y < l; y++) {
								double y2 = (y - m) * (y - m);
								for (int x = 0; x < l; x++) {
									double r = Math.pow((x - m) * (x - m) + y2 + z2, 1.0 / 3.0);
									scaleDensity(density[z * l2 + y * l + x], 
											Math.exp(-(r - m3) * (r - m3) * norm));
								}
							}
						}
						break;

					case LINEAR:
						l = space.size;
						m = (l - 1) * 0.5;
						m3 = m * 0.333;
						norm = 1.0 / l;
						for (int x = 0; x < l; x++) {
							double r = Math.abs(x - m);
							scaleDensity(density[x], 
									Math.exp(-(r - m3) * (r - m3) * norm));
						}
						break;

					default: // for square, triangular and hexagonal lattices
						l = (int) Math.floor(Math.sqrt(space.size) + 0.5);
						m = (l - 1) * 0.5;
						m3 = m * 0.333;
						norm = 1.0 / l;
						for (int y = 0; y < l; y++) {
							double y2 = (y - m) * (y - m);
							for (int x = 0; x < l; x++) {
								double r = Math.sqrt((x - m) * (x - m) + y2);
								scaleDensity(density[y * l + x], 
										Math.exp(-(r - m3) * (r - m3) * norm));
							}
						}
				}
				break;
		}
		// // gradient
		// int l = (int)Math.floor(Math.sqrt(space.size)+0.5);
		// for( int i=0; i<l; i++ ) {
		// int skip = i*l;
		// for( int j=0; j<l; j++ ) {
		// double[] loc = density[skip+j];
		// loc[0] = (double)i/(double)(l-1);
		// loc[1] = (double)j/(double)(l-1);
		// loc[2] = 0.0;
		// }
		// }
	}

	/**
	 * Helper method to scale the density vector {@code d} by the scalar factor
	 * {@code scale}. The scalar must lie in \((0, 1)\) such that the initial
	 * densities/frequencies represent the maximum.
	 * 
	 * @param d     the density vector to scale
	 * @param scale the scaling factor
	 */
	private void scaleDensity(double[] d, double scale) {
		ArrayMath.multiply(y0, scale, d);
		if (dependent >= 0)
			d[dependent] = 1.0 + d[dependent] - ArrayMath.norm(d);
	}

	/**
	 * Types of initial configurations. Currently this model supports the following
	 * density distributions:
	 * <dl>
	 * <dt>UNIFORM
	 * <dd>Uniform/homogeneous distribution of trait densities given by
	 * {@link ODE#y0}.
	 * <dt>RANDOM
	 * <dd>Random trait densities, uniformly distributed between zero and the
	 * densities given by {@link ODE#y0}.
	 * <dt>SQUARE
	 * <dd>Square in the center with uniform densities given by {@link ODE#y0}.
	 * <dt>CIRCLE
	 * <dd>Circle in the center with uniform densities given by {@link ODE#y0}.
	 * <dt>DISTURBANCE
	 * <dd>Spatially homogeneous distribution given by {@link ODE#y0} with a
	 * perturbation in the center cell with densities {@code 1.2*y0}, or, for frequency
	 * based models with inverted and normalized frequencies.
	 * <dt>GAUSSIAN
	 * <dd>Gaussian density distribution in the center. In 2D lattices this
	 * generates a sombrero-like distribution. Maximum density is given by
	 * {@link ODE#y0}.
	 * <dt>GAUSSIAN_RING
	 * <dd>Ring distribution in the center with Gaussian distributed densities along
	 * the radius. In 2D lattices this generates a donut-like distribution. Maximum
	 * density is given by {@link ODE#y0}.
	 * <dt>DEFAULT
	 * <dd>Default initialization (UNIFORM)
	 * </dl>
	 * 
	 * @see #parse(String)
	 * @see ODE#cloInit
	 */
	public enum InitType implements CLOption.Key {

		/**
		 * Uniform/homogeneous distribution of trait densities.
		 */
		UNIFORM("uniform", "uniform densities <d1,...,dn>"),

		/**
		 * Random trait frequencies.
		 */
		RANDOM("random", "random densities"),

		/**
		 * Square in the center with uniform trait densities.
		 */
		SQUARE("square", "square in center <d1,...,dn>"),

		/**
		 * Circle in the center with uniform densities.
		 */
		CIRCLE("circle", "circle in center <d1,...,dn>"),

		/**
		 * Perturbation of a spatially homogeneous distribution with densities
		 * {@code y0}. The perturbation in the center cell has increased densities by a
		 * factor {@code 1.2}, or, for frequency based models with inverted and
		 * normalized frequencies.
		 */
		PERTURBATION("perturbation", "perturbation in center <d1,...,dn>"),

		/**
		 * Gaussian density distribution in the center. In 2D lattices this generates a
		 * sombrero-like distribution. Maximum density is given as specified.
		 */
		GAUSSIAN("sombrero", "sombrero-like distribution <d1,...,dn>"),

		/**
		 * Ring distribution in the center with Gaussian distributed densities along the
		 * radius. In 2D lattices this generates a donut-like distribution. Maximum
		 * density as specified.
		 */
		RING("ring", "donut-like distribution <d1,...,dn>");

		/**
		 * Key of initialization type. Used when parsing command line options.
		 * 
		 * @see ODE#cloInit
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
	 * @see ODE.InitType
	 * @see PDE.InitType
	 */
	@Override
	public boolean parse(String arg) {
		// this is just for a single species - as everything else in PDE models
		initType = (InitType) cloInit.match(arg);
		String[] typeargs = arg.split("\\s+|=");
		double[] init = null;
		if (typeargs.length > 1)
			init = CLOParser.parseVector(typeargs[1]);
		int nt = module.getNTraits();
		if (y0 == null || y0.length != nt)
			y0 = new double[nt];
		if (initType == null || !initType.equals(InitType.RANDOM) && (init == null || init.length != nt)) {
			initType = InitType.RANDOM;
			logger.warning("parsing of init '" + arg + //
					"' failed - using default " + InitType.RANDOM + "'.");
			return false;
		}
		// init can be null for RANDOM initializations
		if (init == null)
			Arrays.fill(y0, 1.0);
		else
			System.arraycopy(init, 0, y0, 0, nt);
		if (dependent >= 0) {
			// normalize frequencies
			ArrayMath.normalize(y0);
		}
		return true;
	}

	/**
	 * Helper method to initialize the effective rate of diffusion for the time
	 * increment <code>dt</code>.
	 * <p>
	 * <strong>Note:</strong> This method needs to be public to permit access by
	 * {@link org.evoludo.simulator.models.PDESupervisorGWT PDESupervisorGWT} and
	 * {@link org.evoludo.simulator.models.PDESupervisorJRE PDESupervisorJRE}
	 * 
	 * @param deltat the time increment for diffusion
	 */
	public void initDiffusion(double deltat) {
		if (alpha == null || alpha.length != nDim)
			alpha = new double[nDim];
		double invdx = calcInvDeltaX();
		ArrayMath.multiply(diffcoeff, deltat * invdx * invdx, alpha);
	}

	/**
	 * Helper method to check whether the time increment, {@link ODE#dt}, is
	 * acceptable. If it is too large the diffusion step runs into numerical issues.
	 * If <code>dt</code> needs to be decreased a warning is emitted.
	 */
	void checkDt() {
		// some careful checking for suitable time steps is required!
		// if Di/(dx^2)>1/dt for any Di then adjust dt
		// if Di/(dx^2)*dt*k>1 for any Di with k the maximum number of outgoing links
		// then adjust dt
		double invdx = calcInvDeltaX();
		double invdx2 = invdx * invdx;
		double maxD = ArrayMath.max(diffcoeff) * invdx2;
		int maxK = Math.max(space.maxOut, space.maxIn);
		// threshold of 1 is much to aggressive - this means everyone in one site
		// migrates! this can introduce artifacts!
		if (dt < 1e-5 || maxK * maxD * dt > 0.5) {
			double deltat = Math.max(0.5 / (maxD * maxK), Double.MIN_VALUE);
			logger.warning("PDE time scale adjusted (diffusion): dt=" + Formatter.formatSci(deltat, 4)
					+ " (was dt=" + Formatter.formatSci(dt, 4) + ").");
			dt = deltat;
		}
		// dynamical dt
		// min_s1: max_dens+(min_kin*min_dens-max_kout*max_dens)*max_D/dx^2*dt>0
		// min_s2: min_dens+(min_kin*min_dens-max_kout*min_dens)*max_D/dx^2*dt>0
		// max_s1: max_dens+(max_kin*max_dens-min_kout*max_dens)*max_D/dx^2*dt<1 (with
		// dependent)
		// max_s2: min_dens+(max_kin*max_dens-min_kout*min_dens)*max_D/dx^2*dt<1 (with
		// dependent)
		// double minDens = ChHMath.min(minDensity); // exclude dependent
		// double maxDens = ChHMath.max(maxDensity); // exclude dependent
		// double minDens = Math.min(minDensity[1], minDensity[2]);
		// double maxDens = Math.max(maxDensity[1], maxDensity[2]);
		// double minDt1 =
		// -maxDens/(space.minIn*minDens-space.maxOut*maxDens)/maxD/invdx2;
		// double minDt2 =
		// -minDens/(space.minIn*minDens-space.maxOut*minDens)/maxD/invdx2;
		// double maxDt1 =
		// (1.0-maxDens)/(space.maxIn*maxDens-space.minOut*maxDens)/maxD/invdx2;
		// double maxDt2 =
		// (1.0-minDens)/(space.maxIn*maxDens-space.minOut*minDens)/maxD/invdx2;
		// GWT.log("auto dt: minDens="+ChHFormatter.format(minDens, 6)+",
		// maxDens="+ChHFormatter.format(maxDens, 6));
		// GWT.log("auto dt: t="+ChHFormatter.format(time, 6)+",
		// minDt1="+ChHFormatter.format(minDt1, 6)+
		// ", minDt2="+ChHFormatter.format(minDt2, 6)+",
		// maxDt1="+ChHFormatter.format(maxDt1, 6)+
		// ", maxDt2="+ChHFormatter.format(maxDt2, 6)+", dt="+ChHFormatter.format(dt,
		// 6));
	}

	/**
	 * Helper method to calculate <code>1 / dx</code> for different lattice
	 * geometries.
	 * 
	 * @return the <code>1 / dx</code> for the current geometry.
	 */
	double calcInvDeltaX() {
		switch (space.getType()) {
			case CUBE:
				return Math.pow(space.size, 1.0 / 3.0) / linext;
			case LINEAR:
				return space.size / linext;
			case SQUARE_NEUMANN:
			case SQUARE_NEUMANN_2ND:
			case SQUARE_MOORE:
			case SQUARE:
			case TRIANGULAR:
			case HONEYCOMB:
			default:
				return Math.sqrt(space.size) / linext;
		}
	}

	/**
	 * Utility method to update the trait minimum, maximum and mean based on the
	 * provided data array.
	 * 
	 * @param data the data to process
	 * @param min  the array with the minima of each trait
	 * @param max  the array with the maxima of each trait
	 * @param mean the array with the trait means
	 */
	static void minmaxmean(double[] data, double[] min, double[] max, double[] mean) {
		for (int i = 0; i < data.length; i++) {
			double d = data[i];
			min[i] = Math.min(d, min[i]);
			max[i] = Math.max(d, max[i]);
			mean[i] += d;
		}
	}

	/**
	 * Command line option to set the discretization as the total number of spatial
	 * units to use for the numerical integration of the PDE.
	 * 
	 * @see #setDiscretization(int)
	 */
	public final CLOption cloPdeN = new CLOption("pdeN", "5041", EvoLudo.catModel, // 71x71
			// note: XHTML is very fussy about characters... (both variants below simply
			// kill the help screen)
			// "--pdeN <N> discretization PDE (√N bins for lattice side)",
			// "--pdeN <N> discretization PDE (&radic;N bins for lattice side)",
			"--pdeN <N>  discretization PDE (sqrt(N) bins for lattice side)", new CLODelegate() {
				@Override
				public boolean parse(String arg) {
					setDiscretization(CLOParser.parseDim(arg));
					return true;
				}

				@Override
				public void report(PrintStream output) {
					output.println("# pde discretization:   " + space.size);
				}
			});

	/**
	 * Command line option to set the linear extension. This is only meaningful
	 * lattice type geometries.
	 * 
	 * @see #setLinearExtension(double)
	 */
	public final CLOption cloPdeL = new CLOption("pdeL", "100", EvoLudo.catModel,
			"--pdeL <L>  linear extension of spatial domain of PDE", new CLODelegate() {
				@Override
				public boolean parse(String arg) {
					setLinearExtension(CLOParser.parseDouble(arg));
					return true;
				}

				@Override
				public void report(PrintStream output) {
					output.println("# pde linear extension: " + getLinearExtension());
				}
			});

	/**
	 * Command line option to set the diffusion coefficients.
	 * 
	 * @see #setDiffusion(double[])
	 */
	public final CLOption cloPdeDiffusion = new CLOption("pdeD", "1", EvoLudo.catModel, null,
			new CLODelegate() {
				@Override
				public boolean parse(String arg) {
					setDiffusion(CLOParser.parseVector(arg));
					return true;
				}

				@Override
				public void report(PrintStream output) {
					double[] diff = getDiffusion();
					for (int n = 0; n < nDim; n++) {
						if (n == dependent)
							output.println("# pde diffusion:        -\t" + names[n] + " (dependent)");
						else
							output.println("# pde diffusion:        " + Formatter.format(diff[n], 3) + "\t" + names[n]);
					}
				}

				@Override
				public String getDescription() {
					String descrDiff;
					int dim = nDim;
					if (dependent >= 0)
						dim--;
					switch (dim) {
						case 1:
							// NOT TESTED!
							descrDiff = "--pdeD <d0>          diffusion for independent variable";
							break;
						case 2:
							descrDiff = "--pdeD <d0,d1>       diffusion for independent variables, with";
							break;
						case 3:
							descrDiff = "--pdeD <d0,d1,d2>    diffusion for independent variables, with";
							break;
						default:
							descrDiff = "--pdeD <d0,...,d" + (nDim - 1)
									+ ">  diffusion for independent variables, with";
					}
					int idx = 0;
					for (int n = 0; n < nDim; n++) {
						if (n == dependent) {
							descrDiff += "\n          " + names[n] + " (dependent)";
							continue;
						}
						descrDiff += "\n      d" + (idx++) + ": " + names[n];
					}
					descrDiff += "\n      diffusion: di=Di/(2*dx^2)";
					return descrDiff;
				}
			});

	/**
	 * Command line option to request preservation of symmetry. If request cannot be
	 * honoured a warning is issued.
	 * 
	 * @see #setSymmetric(boolean)
	 */
	public final CLOption cloPdeSymmetric = new CLOption("pdeSymmetry", "nosymmetry", CLOption.Argument.NONE,
			EvoLudo.catModel,
			"--pdeSymmetry        request symmetric diffusion of PDE", new CLODelegate() {
				@Override
				public boolean parse(String arg) {
					setSymmetric(cloPdeSymmetric.isSet());
					return true;
				}

				@Override
				public void report(PrintStream output) {
					if (getSymmetric())
						output.println("# pde dynamics:         preserve symmetry");
				}
			});

	// public final CLOption cloPdeColorRange = new CLOption("pdecolorrange",
	// EvoLudo.catGUI,
	// "auto", null,
	// new CLODelegate() {
	// @Override
	// public boolean parse(String arg) {
	// return parsePDEColorRange(arg);
	// }
	// @Override
	// public String getDescription() {
	// // see cloPdeDiffusion
	// }
	// });

	@Override
	public void collectCLO(CLOParser parser) {
		super.collectCLO(parser);
		// geometry for PDE model defaults to von Neumann lattice
		module.cloGeometry.setDefault("n");
		parser.addCLO(cloPdeN);
		parser.addCLO(cloPdeL);
		parser.addCLO(cloPdeDiffusion);
		parser.addCLO(cloPdeSymmetric);
		// parser.addCLO(cloPdeColorRange);
		// ODE loaded its own keys already - clear and reload ours.
		cloInit.clearKeys();
		cloInit.addKeys(InitType.values());
		cloInit.setDefault(InitType.RANDOM.getKey());
	}

	// public boolean parsePDEColorRange(String colorranges) {
	// String[] ranges = colorranges.split(";");
	// if( ranges==null ) {
	// delegate.getLogger().warning("invalid argument for pdecolorrange
	// ("+colorranges+") - ignored.");
	// return false;
	// }
	// if( scaleAuto==null || scaleAuto.length!=d ) {
	// scaleMin = new double[d];
	// scaleMax = new double[d];
	// scaleAuto = new boolean[d];
	// Arrays.fill(scaleMin, 0.0);
	// Arrays.fill(scaleMax, 1.0);
	// Arrays.fill(scaleAuto, true);
	// }
	// int cidx = 0, idx =0;
	// for( int c=0; c<d; c++ ) {
	// if( c==dependent ) continue;
	// String range = ranges[cidx].trim();
	// if( range.startsWith("auto") ) {
	// scaleAuto[idx] = true;
	// scaleMin[idx] = 0.0;
	// scaleMax[idx] = 1.0;
	// }
	// else {
	// String[] minmax = range.split(":");
	// if( minmax==null || minmax.length<2 ) continue;
	// scaleMin[idx] = Double.parseDouble(minmax[0]);
	// scaleMax[idx] = Double.parseDouble(minmax[1]);
	// scaleAuto[idx] = false;
	// }
	// cidx = (cidx+1)%ranges.length;
	// idx++;
	// }
	// return true;
	// }

	@Override
	public void encodeState(StringBuilder plist) {
		super.encodeState(plist);
		encodeGeometry(plist);
	}

	@Override
	public boolean restoreState(Plist plist) {
		boolean success = super.restoreState(plist);
		if (species.size() > 1) {
			for (Module pop : species) {
				Plist pplist = (Plist) plist.get(pop.getName());
				if (!restoreGeometry(pplist)) {
					logger.warning("restore geometry in " + type + "-model failed (" + pop.getName() + ").");
					success = false;
				}
			}
		} else {
			if (!restoreGeometry(plist)) {
				logger.warning("restore geometry in " + type + "-model failed.");
				success = false;
			}
		}
		return success;
	}

	/**
	 * Encodes the geometry of the spatial structure for this PDE in the form of a
	 * <code>plist</code> string.
	 * 
	 * @param plist the string builder for the encoded state
	 */
	void encodeGeometry(StringBuilder plist) {
		plist.append("<key>PDEGeometry</key>\n" + "<dict>\n");
		plist.append(space.encodeGeometry());
		plist.append("</dict>\n");
	}

	/**
	 * Restores the geometry of the spatial structure encoded in <code>plist</code>.
	 * 
	 * @param plist the encoded state
	 * @return <code>true</code> if successful
	 */
	boolean restoreGeometry(Plist plist) {
		Plist geo = (Plist) plist.get("PDEGeometry");
		if (geo == null)
			return false;
		space.decodeGeometry(geo);
		return true;
	}

	@Override
	void encodeStrategies(StringBuilder plist) {
		plist.append(Plist.encodeKey("Density", density));
	}

	@Override
	public boolean restoreStrategies(Plist plist) {
		@SuppressWarnings("unchecked")
		List<List<Double>> state = (List<List<Double>>) plist.get("Density");
		if (state == null || state.size() != space.size || state.get(0) == null || state.get(0).size() != nDim)
			return false;
		for (int n = 0; n < space.size; n++) {
			List<Double> cell = state.get(n);
			double[] dcell = density[n];
			for (int i = 0; i < nDim; i++)
				dcell[i] = cell.get(i);
		}
		return true;
	}

	@Override
	public void encodeFitness(StringBuilder plist) {
		plist.append(Plist.encodeKey("Fitness", fitness));
	}

	@Override
	public boolean restoreFitness(Plist plist) {
		@SuppressWarnings("unchecked")
		List<List<Double>> fit = (List<List<Double>>) plist.get("Fitness");
		if (fit == null || fit.size() != space.size || fit.get(0) == null || fit.get(0).size() != nDim)
			return false;
		for (int n = 0; n < space.size; n++) {
			List<Double> cell = fit.get(n);
			double[] fcell = fitness[n];
			for (int i = 0; i < nDim; i++)
				fcell[i] = cell.get(i);
		}
		update();
		return true;
	}
}
