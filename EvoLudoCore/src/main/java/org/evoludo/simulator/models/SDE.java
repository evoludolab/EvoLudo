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

package org.evoludo.simulator.models;

import org.evoludo.math.ArrayMath;
import org.evoludo.simulator.EvoLudo;
import org.evoludo.simulator.models.ODEInitialize.InitType;
import org.evoludo.simulator.modules.Module;
import org.evoludo.simulator.modules.Mutation;
import org.evoludo.simulator.views.HasHistogram;
import org.evoludo.util.CLOParser;
import org.evoludo.util.CLOption;

/**
 * Integrator for stochastic differential equations (SDE) based on Euler's
 * method.
 * 
 * @author Christoph Hauert
 */
public class SDE extends ODE {

	/**
	 * Convenience variable: module associated with this model (useful as long as
	 * SDE models are restricted to single species).
	 */
	protected Module<?> module;

	/**
	 * {@code true} for a custom subclass with a vacant-trait species, routing
	 * {@link #deStep(double)} through the reaction-based
	 * {@link #processEcology(double, double)} instead of the closed-form
	 * {@link #process2TraitsNoise(int, double, double, double)}/
	 * {@link #process3TraitsNoise(double)}.
	 */
	protected boolean isEcological;

	/**
	 * Constructs a new model for the numerical integration of the system of
	 * stochastic differential equations representing the dynamics specified by the
	 * {@link Module} <code>module</code> using the {@link EvoLudo} pacemaker
	 * <code>engine</code> to control the numerical evaluations. The integrator
	 * implements Euler's method (fixed step size).
	 * <p>
	 * <strong>Important:</strong> for reproducibility the shared random number
	 * generator should be used.
	 * 
	 * @param engine the pacemaker for running the model
	 * 
	 * @see EvoLudo#getRNG()
	 */
	public SDE(EvoLudo engine) {
		super(engine);
		type = ModelType.SDE;
	}

	@Override
	public void load() {
		super.load();
		module = engine.getModule();
	}

	@Override
	public synchronized void unload() {
		module = null;
		super.unload();
	}

	@Override
	public boolean check() {
		if (nSpecies > 1) {
			// currently multi-species modules are only acceptable for ecological models
			for (Module<?> mod : species) {
				int nt = mod.getNActive();
				if (nt == 1 || (nt == 2 && mod.getVacantIdx() >= 0))
					continue;
				// multiple traits implies evolutionary module - revert to ODE
				return revertToODE("multi-species require single trait - revert to ODE.");
			}
		} else if (((HasDE) module).getDependent() < 0) {
			// single species module requires dependent trait
			return revertToODE("requires dependent trait - revert to ODE.");
		}
		boolean doReset = super.check();
		isEcological = false;
		// only custom subclasses (which can override computeEffRates() for
		// e.g. a competition matrix) opt into the ecological noise; plain SDE
		// instances (e.g. EcoPGG) keep the closed forms even with a vacant trait
		if (getClass() != SDE.class) {
			for (Module<?> mod : species) {
				if (mod.getVacantIdx() >= 0) {
					isEcological = true;
					break;
				}
			}
		}
		// at this point it is clear that we have a dependent trait
		int dim = nDim - 1;
		// only one or two traits acceptable or, alternatively, two or three
		// traits for replicator dynamics (for SDE but not SDEN)
		if (!getClass().getSuperclass().equals(SDE.class) && (dim < 1 || dim > 2))
			return revertToODE("max. 3 traits incl. dependent - revert to ODE (use SDEN).");
		if (isAdjustedDynamics) {
			// XXX check min/max fitness instead
			// fitness is not guaranteed to be positive
			engine.logWarning(getClass().getSimpleName()
					+ " - adjusted dynamics for SDE's not (yet) implemented (revert to standard dynamics).");
			isAdjustedDynamics = false;
		}
		return doReset;
	}

	/**
	 * Log a warning and request switching to the ODE model.
	 *
	 * @param message the warning message to log
	 * @return {@code true} to signal a reset
	 */
	private boolean revertToODE(String message) {
		engine.logWarning(getClass().getSimpleName() + ": " + message);
		engine.loadModel(ModelType.ODE);
		return true;
	}

	@Override
	public void reset() {
		super.reset();
		// the only potentially absorbing states for SDE's are homogeneous
		// states of the population in the absence of mutations (this
		// includes possible extinction, which remains absorbing even in
		// the presence of mutations).
		converged = false;
		connect = true;
	}

	@Override
	public boolean next(double step) {
		// start new statistics sample if required
		if (mode == Mode.STATISTICS_SAMPLE && statisticsSampleNew) {
			reset();
			init();
			initStatisticsSample();
			update();
			return true;
		}
		return super.next(step);
	}

	@Override
	public boolean setMode(Mode mode) {
		if (mode == Mode.STATISTICS_SAMPLE && fixData == null) {
			fixData = new FixationData();
			// mutant, resident and location of the mutant are set in
			// resetStatisticsSample() once the initial state is known
		} else
			fixData = null;
		return super.setMode(mode);
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * <strong>Note:</strong> SDE's only converge to absorbing states (including
	 * extinction), if there are any. Moreover, SDE's cannot converge with non-zero
	 * mutation rates (except extinction).
	 */
	@Override
	public boolean checkConvergence(double dist2) {
		if (converged)
			return true;
		converged = true;
		boolean hasMutation = false;
		for (int idx = 0; idx < nSpecies; idx++) {
			Module<?> mod = species.get(idx);
			int start = idxSpecies[idx];
			int end = idxSpecies[idx + 1];
			if (mod.getMutation().getProbability() > 0.0) {
				hasMutation = true;
				// if dist2 is zero (or very small) and (at least) one trait is absent,
				// random noise may be invalid (pushing state outside of permissible values)
				if (dist2 < accuracy && min(yt, start, end) < accuracy)
					return false;
				int vacant = mod.getVacantIdx();
				// extinction is absorbing even with mutations
				if (vacant < 0 || yt[start + vacant] <= 1.0 - accuracy) {
					converged = false;
					break;
				}
			} else if (!monoStop && (isDensity || max(yt, start, end) <= 1.0 - accuracy)) {
				converged = false;
				break;
			}
		}
		if (monoStop && !hasMutation)
			converged = isMonomorphic();
		return converged;
	}

	/**
	 * Find the minimum value in a range of an array.
	 * 
	 * @param values the array to search
	 * @param start  the starting index (inclusive)
	 * @param end    the ending index (exclusive)
	 * @return the minimum value in the specified range
	 */
	private static double min(double[] values, int start, int end) {
		double min = Double.MAX_VALUE;
		for (int n = start; n < end; n++) {
			double value = values[n];
			if (value < min)
				min = value;
		}
		return min;
	}

	/**
	 * Find the maximum value in a range of an array.
	 * 
	 * @param values the array to search
	 * @param start  the starting index (inclusive)
	 * @param end    the ending index (exclusive)
	 * @return the maximum value in the specified range
	 */
	private static double max(double[] values, int start, int end) {
		double max = -Double.MAX_VALUE;
		for (int n = start; n < end; n++) {
			double value = values[n];
			if (value > max)
				max = value;
		}
		return max;
	}

	/**
	 * Safety buffer, in standard deviations, for the proactive step-size guard in
	 * {@link #boundVacantStep}: steps are shrunk so the boundary stays at least
	 * this many sigma away from both the estimated drift and the estimated noise
	 * jump.
	 */
	private static final double STEP_SAFETY_SIGMA = 2.0;

	/**
	 * {@inheritDoc}
	 * <p>
	 * For stochastic differential equations Gaussian distributed white noise is
	 * added.
	 *
	 * <h3>Implementation Notes:</h3>
	 * Non-ecological replicator dynamics uses the closed forms
	 * {@link #process2TraitsNoise(int, double, double, double)}/
	 * {@link #process3TraitsNoise(double)} for 2 or 3 traits. Ecological
	 * dynamics instead uses {@link #processEcology(double, double)} for any
	 * number of traits, see {@link #isEcological}. In both cases noise is
	 * computed into {@link #dyt} first, then
	 * {@link #addReactionNoise(double[], double[], double)} runs, and only then
	 * is the step actually applied - so noise added by that hook is never
	 * dropped.
	 *
	 * @see <a href="http://dx.doi.org/10.1103/PhysRevE.85.041901">Traulsen, A.,
	 *      Claussen J. C. &amp; Hauert, Ch. (2012) Stochastic differential
	 *      equations for evolutionary dynamics with demographic noise and
	 *      mutations, Phys. Rev. E 85, 041901</a>
	 * @see <a href="https://doi.org/10.1103/PhysRevLett.94.218102">McKane, A. J.,
	 *      &amp;
	 *      Newman, T. J. (2005). Predator–prey cycles from resonant amplification
	 *      of demographic stochasticity. Physical Review Letters, 94(21),
	 *      218102</a>
	 */
	@Override
	protected double deStep(double step) {
		// SDE models never evaluate concurrently, so the reusable fields are safe
		// to reuse here, unlike PDE's per-call buffers, see ODE#applyUpdates
		computeEffRates(yt, effBirth, effDeath);
		if (!isDensity)
			step = computeStep(step);
		double stepSize = Math.abs(step);
		double sqrtdt = Math.sqrt(stepSize) / stepSize;

		// addReactionNoise() must run before any step is applied, not after, so
		// any noise it adds to dyt (e.g. a trait-to-trait exchange that bypasses
		// vacant) is actually reflected in yout
		if (isEcological) {
			processEcology(step, sqrtdt);
		} else {
			switch (nDim) {
				case 2:
					process2TraitsNoise(0, sqrtdt, mutation[0].getProbability(), getEffectiveNoise(module, 0));
					addReactionNoise(yt, dyt, sqrtdt);
					applyStep2Traits(0, step, mutation[0].getProbability());
					break;

				case 3:
					process3TraitsNoise(sqrtdt);
					addReactionNoise(yt, dyt, sqrtdt);
					if (mutation[0].getProbability() > 0.0) {
						// noise must not push us beyond boundaries of simplex
						ArrayMath.addscale(yt, dyt, step, yout);
						break;
					}
					// in the absence of mutations, extinct traits (or species) must not make
					// a sudden reappearance due to roundoff errors! (this closed-form,
					// non-ecological case has no vacant trait, so nothing reflects)
					applyStep(0, nDim, -1, step);
					break;

				default:
					// unreachable: check() reverts non-ecological dim>2 to ODE, and
					// subclasses bypassing that cap (e.g. SDEN) override deStep()
					throw new IllegalStateException(getClass().getSimpleName()
							+ ": non-ecological SDE with nDim=" + nDim + " not supported here - see SDEN.");
			}
		}

		// reactive closest-boundary clamp: yout is affine in the step size, so
		// the sub-step cancelling each violation is independently computable;
		// the smallest one is safe for all simultaneously - a single pass,
		// rather than two independent ones that can undo each other's fix.
		double bound = step;
		int boundIdx = -1;
		boolean anyViolation = false;
		for (int i = 0; i < yout.length; i++) {
			double yi = yout[i];
			if (yi < 0.0) {
				anyViolation = true;
				// yt[i] already at 0 and still being pushed down isn't a new
				// crossing - it can't move further, and must not force
				// everyone else's step to zero; the defensive clamp below
				// re-pins it exactly regardless, so just skip it here
				if (yt[i] <= 0.0)
					continue;
				// yt[i]>0 must hold (from previous step); sign of dyt[i] depends
				// on direction of integration
				double s = -yt[i] / dyt[i];
				if (Math.abs(s) < Math.abs(bound)) {
					bound = s;
					boundIdx = i;
				}
			} else if (!isDensity && yi > 1.0) {
				anyViolation = true;
				if (yt[i] >= 1.0)
					continue;
				double s = (1.0 - yt[i]) / dyt[i];
				if (Math.abs(s) < Math.abs(bound)) {
					bound = s;
					boundIdx = i;
				}
			}
		}
		if (anyViolation) {
			step = bound;
			ArrayMath.addscale(yt, dyt, step, yout);
			// division/multiplication roundoff in the corrected addscale can
			// leave a residual of either sign on the entry that defined bound -
			// unlike a genuine overshoot, a tiny surviving value on the wrong
			// side of exact would silently defeat the absorbing/reflecting
			// boundary tests upstream (e.g. yt[i] <= 0.0), so force it to the
			// exact boundary rather than merely clamping overshoots. Entries
			// already pinned at 0/1 before this step (excluded above) are not
			// touched here - the defensive clamp below re-pins them exactly.
			if (boundIdx >= 0)
				yout[boundIdx] = (yout[boundIdx] < 0.5) ? 0.0 : 1.0;
			// roundoff may still leave other entries marginally outside their
			// bound even at the corrected step - clamp defensively
			for (int i = 0; i < yout.length; i++)
				yout[i] = isDensity ? Math.max(0.0, yout[i]) : Math.min(1.0, Math.max(0.0, yout[i]));
		}
		if (!isDensity)
			normalizeState(yout);
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
	 * Proactive step-size guard, narrowly scoped to a species' vacant fraction
	 * approaching 1 (population {@code ->} extinct): that is the one regime
	 * where demographic noise itself explodes (the {@code 1/(1-ytv)} term in
	 * {@link #getEffectiveNoise(Module, int)}), rather than just the state
	 * nearing a boundary - which is the reactive clamp's job in
	 * {@link #deStep}. Only ever shrinks {@code step}; see
	 * {@link #boundVacantStep} for how.
	 *
	 * @param step the requested (signed) step size
	 * @return the (signed) step size, shrunk if necessary
	 */
	private double computeStep(double step) {
		double stepSize = Math.abs(step);
		double dir = (step < 0.0) ? -1.0 : 1.0;
		for (int idx = 0; idx < nSpecies; idx++) {
			Module<?> mod = species.get(idx);
			int vacant = mod.getVacantIdx();
			if (vacant < 0)
				continue;
			stepSize = Math.min(stepSize, boundVacantStep(mod, idxSpecies[idx], idxSpecies[idx + 1], vacant, dir));
		}
		return dir * stepSize;
	}

	/**
	 * Bound the step size for one species so its vacant fraction's expected
	 * drift and noise jump stay {@link #STEP_SAFETY_SIGMA} standard deviations
	 * from full extinction. The drift bound uses the previous step's rate (this
	 * step's noise isn't known yet); the noise bound uses this step's effective
	 * birth/death rates (from {@link #computeEffRates}) and the population size
	 * - since the noise jump scales as {@code sqrt(step * activity /
	 * population)}, smaller populations need smaller steps for the same margin.
	 *
	 * @param mod    the species' module
	 * @param start  the species' starting index in {@link #yt}
	 * @param end    the species' ending index (exclusive) in {@link #yt}
	 * @param vacant the species-relative index of the vacant trait
	 * @param dir    the direction of integration, {@code +1.0} or {@code -1.0}
	 * @return the estimated bound on the step size (unsigned)
	 */
	private double boundVacantStep(Module<?> mod, int start, int end, int vacant, double dir) {
		int vacantAbs = start + vacant;
		double z = yt[vacantAbs];
		double margin = 1.0 - z;
		if (margin <= 0.0)
			return Double.POSITIVE_INFINITY; // already extinct
		double buffered = margin / STEP_SAFETY_SIGMA;
		double bound = Double.POSITIVE_INFINITY;
		double rate = dir * dyt[vacantAbs];
		if (rate > 0.0)
			bound = buffered / rate;
		double activity = 0.0;
		for (int n = start; n < end; n++) {
			if (n == vacantAbs)
				continue;
			double x = yt[n];
			if (x <= 0.0)
				continue;
			double birthRate = effBirth[n] + ((ft != null) ? ft[n] : 0.0);
			if (birthRate > 0.0)
				activity += x * z * birthRate;
			if (effDeath[n] > 0.0)
				activity += x * effDeath[n];
		}
		if (activity > 0.0)
			bound = Math.min(bound, buffered * buffered * mod.getNPopulation() / activity);
		return bound;
	}

	/**
	 * Hook for noise from reaction channels not covered by
	 * {@link #computeEffRates(double[], double[], double[])}, e.g. a direct
	 * exchange between two traits that bypasses the vacant trait. No-op by
	 * default.
	 *
	 * @param state  the current state, as in {@link #yt}
	 * @param change the derivatives to add noise to, as in {@link #dyt}
	 * @param sqrtdt the square root of the step size
	 */
	protected void addReactionNoise(double[] state, double[] change, double sqrtdt) {
		// no-op by default
	}

	/**
	 * Add noise for two dependent traits to {@link #dyt}. Split from
	 * {@link #applyStep2Traits(int, double, double)} so callers can run
	 * {@link #addReactionNoise(double[], double[], double)} in between, see
	 * {@link #deStep(double)}.
	 *
	 * @param skip   the start index of the two traits
	 * @param sqrtdt the square root of the step size
	 * @param mu     the mutation rate
	 * @param noise  the noise to be processed
	 */
	private void process2TraitsNoise(int skip, double sqrtdt, double mu, double noise) {
		double x = yt[skip];
		// T^+ + T^-: b = ((1.0 - mu) * x * (1.0 - x) + mu) * noise;
		// noise: n = Math.sqrt(b) * rng.nextGaussian() * sqrtdt;
		double n = Math.sqrt(((1.0 - mu) * x * (1.0 - x) + mu) * noise)
				* rng.nextGaussian() * sqrtdt;
		dyt[skip] += n;
		dyt[skip + 1] -= n;
	}

	/**
	 * Apply the Euler step for two dependent traits, using {@link #dyt} as
	 * finalized by {@link #process2TraitsNoise(int, double, double, double)} and
	 * {@link #addReactionNoise(double[], double[], double)}.
	 *
	 * @param skip the start index of the two traits
	 * @param step the step size
	 * @param mu   the mutation rate
	 */
	private void applyStep2Traits(int skip, double step, double mu) {
		int skip1 = skip + 1;
		if (mu > 0.0) {
			double dy = step * dyt[skip];
			double dy1 = step * dyt[skip1];
			// yt[skip]<=0.0 and yt[skip1]>=1.0 are the same extinction event - but
			// roundoff can let the two drift out of sync (e.g. yt[skip1] already
			// snapped to exactly 1.0 while yt[skip] still carries a stale non-zero
			// residual); treating only one as the trigger leaves that residual
			// behind, where it can later freeze the integrator, see deStep(double)
			if (yt[skip] <= 0.0 || yt[skip1] >= 1.0) {
				dy = Math.max(0.0, dy);
				yout[skip] = dy;
				yout[skip1] = 1.0 - dy;
				return;
			}
			if (yt[skip1] <= 0.0 || yt[skip] >= 1.0) {
				dy1 = Math.max(0.0, dy1);
				yout[skip] = 1.0 - dy1;
				yout[skip1] = dy1;
				return;
			}
			yout[skip] = yt[skip] + dy;
			yout[skip1] = yt[skip1] + dy1;
			return;
		}
		// in the absence of mutations, extinct traits (or species) must not make
		// a sudden reappearance due to roundoff errors! (no vacant trait involved
		// here, so nothing reflects)
		applyStep(skip, skip + 2, -1, step);
	}

	/**
	 * Add noise for the three-trait stochastic update to {@link #dyt}. Called
	 * from {@link #deStep(double)}, before
	 * {@link #addReactionNoise(double[], double[], double)} and the step itself
	 * are applied.
	 *
	 * @param sqrtdt square root of the time step (for noise terms)
	 */
	private void process3TraitsNoise(double sqrtdt) {
		// NOTE: if not replicator equation some adjustments are required (references to
		// e.g. yt[2] would fail)
		double x = yt[0];
		double y = yt[1];
		double x2 = x * x;
		double xy = x * y;
		double y2 = y * y;

		// mutations to only other traits
		double mu = mutation[0].getProbability();
		double effnoise = getEffectiveNoise(module, 0);
		double bxx = (x - x2 + mu * ((1.0 - x) * 0.5 + x2)) * effnoise;
		double bxy = -(xy + mu * ((x + y) * 0.5 - xy)) * effnoise;
		double byx = bxy;
		double byy = (y - y2 + mu * ((1.0 - y) * 0.5 + y2)) * effnoise;

		// eigenvalues of B
		double trB2 = (bxx + byy) * 0.5;
		double detB = bxx * byy - byx * bxy;
		// B has real, non-negative eigenvalues
		double discr = Math.max(0.0, trB2 * trB2 - detB); // discriminant must be non-negative
		double root = Math.sqrt(discr);
		double e1 = trB2 + root;
		double e2 = trB2 - root;

		double u1;
		double u2;
		double v1;
		double v2;
		// avoid problems due to roundoff errors
		if (yt[2] <= 0.0 || e2 < 0.0)
			e2 = 0.0;
		if (Math.abs(bxy) > 1e-12) {
			u1 = e1 - byy;
			u2 = bxy;
			double norm = Math.sqrt(u1 * u1 + u2 * u2);
			u1 /= norm;
			u2 /= norm;
			v1 = e2 - byy;
			v2 = bxy;
			norm = Math.sqrt(v1 * v1 + v2 * v2);
			v1 /= norm;
			v2 /= norm;
		} else if (Math.abs(bxx) > 1e-12) {
			// eigenvectors u, v of B (sync with eigenvalues: e1 -> u, e2 -> v)
			u1 = 1.0;
			u2 = 0.0;
			v1 = 0.0;
			v2 = 1.0;
		} else {
			u1 = 0.0;
			u2 = 1.0;
			v1 = 1.0;
			v2 = 0.0;
		}

		// lambda matrix
		double sqrte1 = Math.sqrt(e1);
		double sqrte2 = Math.sqrt(e2);
		// C matrix
		double cxx = sqrte1 * u1 * u1 + sqrte2 * v1 * v1;
		double cxy = sqrte1 * u1 * u2 + sqrte2 * v1 * v2;
		double cyx = cxy;
		double cyy = sqrte1 * u2 * u2 + sqrte2 * v2 * v2;

		// noise (note this scales with sqrt(dt) - for efficiency applied here)
		double r1 = rng.nextGaussian() * sqrtdt;
		double r2 = rng.nextGaussian() * sqrtdt;
		double nx = cxx * r1 + cxy * r2;
		double ny = cyx * r1 + cyy * r2;

		// combine deterministic and stochastic terms - noise must not push us beyond
		// boundaries of simplex
		if (mu > 0.0) {
			// the deterministic drift term also depends on mutations
			ArrayMath.multiply(dyt, 1.0 - mu);
			// mutations to any of the _other_ 2 traits
			double mudt = mu * 0.5;
			double mx = mudt * (1.0 - 3.0 * x) + nx;
			double my = mudt * (1.0 - 3.0 * y) + ny;
			dyt[0] += mx;
			dyt[1] += my;
			dyt[2] -= mx + my;
		} else {
			dyt[0] += nx;
			dyt[1] += ny;
			if (yt[2] > 0.0)
				dyt[2] -= nx + ny;
		}
	}

	/**
	 * Add reaction-based ecological noise (birth, death, competition) to
	 * {@link #dyt}, per species - every reaction moves individuals between one
	 * active trait and the vacant trait, so noise contributions are independent
	 * per trait and simply add, no eigendecomposition needed, for any number of
	 * traits - then run {@link #addReactionNoise(double[], double[], double)}
	 * and finally apply the Euler step for every species, using the fully
	 * finalized {@link #dyt}, in that order, see {@link #deStep(double)}.
	 *
	 * @param step   the step size
	 * @param sqrtdt the square root of the step size
	 */
	private void processEcology(double step, double sqrtdt) {
		int skip = 0;
		if (isDensity) {
			for (Module<?> mod : species) {
				if (mod.getVacantIdx() >= 0) {
					processEcologyDensity(mod, skip, sqrtdt);
				} else {
					double noise = Math.sqrt(getEffectiveNoise(mod, skip)) * rng.nextGaussian() * sqrtdt;
					// species that went extinct should not make a sudden reappearance
					if (yt[skip] > 0.0)
						dyt[skip] += noise;
				}
				skip += mod.getNTraits();
			}
		} else {
			// frequency dynamics: demographic noise from ecological events only
			for (Module<?> mod : species) {
				if (mod.getVacantIdx() >= 0)
					processEcologyFrequency(mod, skip, sqrtdt);
				else
					process2TraitsNoise(skip, sqrtdt, 0.0, getEffectiveNoise(mod, skip));
				skip += mod.getNTraits();
			}
		}
		addReactionNoise(yt, dyt, sqrtdt);
		skip = 0;
		for (Module<?> mod : species) {
			int vacant = mod.getVacantIdx();
			int nTraits = mod.getNTraits();
			if (isDensity || vacant >= 0)
				// vacant sites can always spontaneously reopen (reflect at 0)
				applyStep(skip, skip + nTraits, vacant >= 0 ? skip + vacant : -1, step);
			else
				applyStep2Traits(skip, step, 0.0);
			skip += nTraits;
		}
	}

	/**
	 * Process density-based ecological dynamics with demographic noise from birth
	 * and death events.
	 *
	 * @param mod    the module to process
	 * @param skip   the starting index in state arrays
	 * @param sqrtdt square root of time step for noise scaling
	 */
	private void processEcologyDensity(Module<?> mod, int skip, double sqrtdt) {
		int vacant = mod.getVacantIdx();
		int vacantIdx = (vacant >= 0) ? skip + vacant : -1;
		int end = skip + mod.getNTraits();
		double nPop = mod.getNPopulation();
		double invN = 1.0 / nPop;

		// Apply demographic noise to each trait; effective rates (with competition
		// adjustments) were already computed for every trait, see #deStep
		for (int n = skip; n < end; n++) {
			if (n == vacantIdx)
				continue;
			double x = yt[n];
			if (x <= 0.0)
				continue;
			double birthRate = effBirth[n] + ((ft != null) ? ft[n] : 0.0);
			double aBirth = (birthRate > 0.0) ? nPop * x * birthRate : 0.0;
			double aDeath = (effDeath[n] > 0.0) ? nPop * x * effDeath[n] : 0.0;
			double eta = 0.0;
			if (aBirth > 0.0)
				eta += Math.sqrt(aBirth) * rng.nextGaussian() * sqrtdt * invN;
			if (aDeath > 0.0)
				eta -= Math.sqrt(aDeath) * rng.nextGaussian() * sqrtdt * invN;
			if (eta != 0.0)
				dyt[n] += eta;
		}
	}

	/**
	 * Process ecological dynamics with demographic noise from birth and death
	 * events.
	 * 
	 * @param mod    the module to process
	 * @param skip   the starting index in state arrays
	 * @param sqrtdt square root of time step for noise scaling
	 */
	private void processEcologyFrequency(Module<?> mod, int skip, double sqrtdt) {
		int vacant = mod.getVacantIdx();
		if (vacant < 0)
			return;
		int end = skip + mod.getNTraits();
		double z = yt[skip + vacant];
		double nPop = mod.getNPopulation();
		double invN = 1.0 / nPop;

		// Apply demographic noise to each trait; effective rates (with competition
		// adjustments) were already computed for every trait, see #deStep
		for (int n = skip; n < end; n++) {
			if (n == skip + vacant)
				continue;
			double x = yt[n];
			if (x <= 0.0)
				continue;
			double birthRate = effBirth[n] + ((ft != null) ? ft[n] : 0.0);
			double aBirth = (birthRate > 0.0 && z > 0.0) ? nPop * x * z * birthRate : 0.0;
			double aDeath = (effDeath[n] > 0.0) ? nPop * x * effDeath[n] : 0.0;
			double eta = 0.0;
			if (aBirth > 0.0)
				eta += Math.sqrt(aBirth) * rng.nextGaussian() * sqrtdt * invN;
			if (aDeath > 0.0)
				eta -= Math.sqrt(aDeath) * rng.nextGaussian() * sqrtdt * invN;
			if (eta != 0.0) {
				dyt[n] += eta;
				dyt[skip + vacant] -= eta;
			}
		}
	}

	/**
	 * Apply an Euler step. Traits at 0 absorb rather than dip further negative
	 * from roundoff, unless {@code dyt[i]} itself carries genuine positive drift
	 * e.g. a reaction source term that does not vanish at the boundary, or
	 * best-response dynamics resulting in legitimate backflow into an
	 * extinct trait. For pure replicator dynamics {@code dyt[i]} is proportional
	 * to {@code yt[i]} and so is already ~0 at the boundary. Thus, this does not
	 * reopen the roundoff-reappearance case the absorption guards against. The
	 * trait at {@code reflect} - typically vacant - reflects at 0 instead:
	 * running out of vacant sites is routine and reversible, unlike genuine
	 * extinction.
	 *
	 * @param start   the starting index (inclusive)
	 * @param end     the ending index (exclusive)
	 * @param reflect the index that reflects rather than absorbs at 0, or -1
	 * @param step    the step size
	 */
	private void applyStep(int start, int end, int reflect, double step) {
		for (int i = start; i < end; i++) {
			if (i == reflect) {
				yout[i] = Math.max(0.0, yt[i] + step * dyt[i]);
				continue;
			}
			if (yt[i] > 0.0) {
				yout[i] = yt[i] + step * dyt[i];
				continue;
			}
			double grow = step * dyt[i];
			yout[i] = (grow > 0.0) ? grow : 0.0;
		}
	}

	/**
	 * Helper method to determine the effective demographic noise for a given module
	 * based on the current population size.
	 * 
	 * @param mod  the module to determine the effective noise
	 * @param skip the starting index for the entries in {@code yt} for this module
	 * @return the effective noise for the given module
	 */
	private double getEffectiveNoise(Module<?> mod, int skip) {
		double effnoise = 1.0 / mod.getNPopulation();
		// scale noise according effective population size
		int vacant = skip + mod.getVacantIdx();
		if (vacant < 0)
			return effnoise;
		double ytv = yt[vacant];
		if (ytv > 1.0 - 1e-8)
			return 0.0;
		return effnoise / (1.0 - ytv);
	}

	@Override
	public void readStatisticsSample() {
		super.readStatisticsSample();
		if (fixData == null || nSpecies > 1)
			return;

		// collect new statistics sample
		Module<?> mod = engine.getModule();
		fixData.typeFixed = ArrayMath.maxIndex(yt);
		int vacant = mod.getVacantIdx();
		if (fixData.typeFixed == vacant) {
			// closer look is needed - look for what other trait survived (if any)
			for (int n = 0; n < mod.getNTraits(); n++) {
				if (n != vacant && yt[n] > 0) {
					// no other traits should be present
					fixData.typeFixed = n;
					break;
				}
			}
		}
		fixData.timeFixed = time;
		fixData.updatesFixed = time;
		fixData.probRead = false;
		fixData.timeRead = false;
	}

	@Override
	public void resetStatisticsSample() {
		super.resetStatisticsSample();
		if (fixData != null) {
			fixData.reset();
			// this needs to be revised for vacant sites
			fixData.mutantTrait = ArrayMath.minIndex(y0);
			fixData.residentTrait = ArrayMath.maxIndex(y0);
			// SDE's have no structure, hence the location of the mutant is meaningless
			// but must be non-negative to mark the samples as valid
			fixData.mutantNode = 0;
		}
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see InitType#MUTANT
	 */
	@Override
	public boolean permitsSampleStatistics() {
		if (nSpecies > 1)
			return false;
		Module<?> mod = engine.getModule();
		// just as in structured populations, sampling statistics require a single
		// mutant in a resident population, see IBSD#permitsSampleStatistics()
		return initType != null && initType[0] == InitType.MUTANT
				&& (mod instanceof HasHistogram.StatisticsProbability
						|| mod instanceof HasHistogram.StatisticsTime)
				&& mod.getMutation().getProbability() <= 0.0;
	}

	@Override
	public boolean permitsUpdateStatistics() {
		for (Module<?> mod : species) {
			if (!(mod instanceof HasHistogram.StatisticsTime))
				return false;
		}
		return true;
	}

	@Override
	public void collectCLO(CLOParser parser) {
		super.collectCLO(parser);
		// mutant initialization and statistics is ok for SDE's
		cloInit.addKey(InitType.MUTANT);
		// SDE's currently are restricted to single species modules and implement
		// mutation to other types only (including ALL as well should be fairly straight
		// forward, though).
		if (mutation == null || mutation.length == 0)
			return;
		CLOption clo = mutation[0].clo;
		clo.clearKeys();
		clo.addKey(Mutation.Discrete.Type.NONE);
		clo.addKey(Mutation.Discrete.Type.OTHER);
	}
}
