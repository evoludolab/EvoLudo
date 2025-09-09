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

import org.evoludo.math.ArrayMath;
import org.evoludo.simulator.EvoLudo;
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
	protected Module module;

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
		type = Type.SDE;
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
			for (Module mod : species) {
				int nt = mod.getNActive();
				if (nt == 1 || (nt == 2 && mod.getVacant() >= 0))
					continue;
				// multiple traits implies evolutionary module - revert to ODE
				logger.warning("SDE model for multi-species modules requires single trait - revert to ODE.");
				engine.loadModel(Type.ODE);
				return true;
			}
		} else {
			// single species module requires dependent trait
			if (((HasDE) module).getDependent() < 0) {
				logger.warning("SDE model requires dependent trait - revert to ODE.");
				engine.loadModel(Type.ODE);
				return true;
			}
		}
		boolean doReset = super.check();
		// at this point it is clear that we have a dependent trait
		int dim = nDim - 1;
		// only one or two traits acceptable or, alternatively, two or three
		// traits for replicator dynamics (for SDE but not SDEN)
		if (!getClass().getSuperclass().equals(SDE.class) && (dim < 1 || dim > 2)) {
			logger.warning(getClass().getSimpleName()
					+ " - max. 3 traits incl. dependent - revert to ODE (use SDEN).");
			engine.loadModel(Type.ODE);
			return true;
		}
		if (isAdjustedDynamics) {
			// XXX check min/max fitness instead
			// fitness is not guaranteed to be positive
			logger.warning(getClass().getSimpleName()
					+ " - adjusted dynamics for SDE's not (yet) implemented (revert to standard dynamics).");
			isAdjustedDynamics = false;
		}
		return doReset;
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
	public boolean next() {
		// start new statistics sample if required
		if (mode == Mode.STATISTICS_SAMPLE && statisticsSampleNew) {
			reset();
			init();
			initStatisticsSample();
			update();
			// debugCheck("next (new sample)");
			return true;
		}
		return super.next();
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
		for (Module mod : species) {
			if (mod.getMutation().probability > 0.0) {
				// if dist2 is zero (or very small) and (at least) one trait is absent, 
				// random noise may be invalid (pushing state outside of permissible values)
				if (dist2 < accuracy && ArrayMath.min(yt) < accuracy)
					return false;
				int vacant = mod.getVacant();
				// extinction is absorbing even with mutations
				converged &= (vacant < 0 ? false : (yt[vacant] > 1.0 - accuracy));
			} else
				converged &= monoStop ? isMonomorphic() : (!isDensity && ArrayMath.max(yt) > 1.0 - accuracy);
		}
		return converged;
	}

	/**
	 * {@inheritDoc} For stochastic differential equations Gaussian distributed
	 * white noise is added.
	 * 
	 * <h3>Implementation Notes:</h3>
	 * Integration of SDEs can be optimized in 1 and 2 dimensions. For replicator
	 * systems this means 2 or 3 traits. Currently noise implemented only for
	 * replicator type dynamics.
	 * 
	 * @see <a href="http://dx.doi.org/10.1103/PhysRevE.85.041901">Traulsen, A.,
	 *      Claussen J. C. &amp; Hauert, Ch. (2012) Stochastic differential
	 *      equations for evolutionary dynamics with demographic noise and
	 *      mutations, Phys. Rev. E 85, 041901</a>
	 */
	@Override
	protected double deStep(double step) {
		double stepSize = Math.abs(step);
		double sqrtdt = Math.sqrt(stepSize) / stepSize;
		double x;
		int idx;
		switch (nDim) {
			case 2: // two traits
				process2DNoise(0, step, sqrtdt, mutation[0].probability, getEffectiveNoise(module, 0));
				break;

			case 3: // two dimensions (or three traits) - e.g. RSP game
				// NOTE: if not replicator equation some adjustments are required (references to
				// e.g. yt[2] would fail)
				// 1) stochastic term
				x = yt[0];
				double y = yt[1];
				double x2 = x * x, xy = x * y, y2 = y * y;
				// B matrix
				// mutations need careful definition - generate any trait vs any of the
				// _other_ traits.

				// mutations to any trait (may result in no change)
				// double bxx = (x-x2+mu*(x2+(1.0-x-x)/3.0))*noise;
				// double bxy, byx = bxy = (-xy+mu*(xy-(x+y)/3.0))*noise;
				// double byy = (y-y2+mu*(y2+(1.0-y-y)/3.0))*noise;

				// mutations to only other traits
				double mu = mutation[0].probability;
				double effnoise = getEffectiveNoise(module, 0);
				double bxx = (x - x2 + mu * ((1.0 - x) * 0.5 + x2)) * effnoise;
				double bxy, byx = bxy = -(xy + mu * ((x + y) * 0.5 - xy)) * effnoise;
				double byy = (y - y2 + mu * ((1.0 - y) * 0.5 + y2)) * effnoise;

				// eigenvalues of B
				double trB2 = (bxx + byy) * 0.5;
				double detB = bxx * byy - byx * bxy;
				// B has real, non-negative eigenvalues
				double discr = Math.max(0.0, trB2 * trB2 - detB); // discriminant must be non-negative
				double root = Math.sqrt(discr);
				double e1 = trB2 + root, e2 = trB2 - root;
				double u1, u2, v1, v2;
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
				} else
				// eigenvectors u, v of B (sync with eigenvalues: e1 -> u, e2 -> v)
				if (Math.abs(bxx) > 1e-12) {
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
				double cxy, cyx = cxy = sqrte1 * u1 * u2 + sqrte2 * v1 * v2;
				double cyy = sqrte1 * u2 * u2 + sqrte2 * v2 * v2;

				// noise (note this scales with sqrt(dt) - for efficiency applied here)
				double r1 = rng.nextGaussian() * sqrtdt;
				double r2 = rng.nextGaussian() * sqrtdt;
				double nx = cxx * r1 + cxy * r2;
				double ny = cyx * r1 + cyy * r2;
				// 2) deterministic term stored in dyt
				// 3) combine terms - noise must not push us beyond boundaries of simplex
				if (mu > 0.0) {
					// the deterministic drift term also depends on mutations
					ArrayMath.multiply(dyt, 1.0 - mu);
					// mutations to any of the 3 traits
					/*
					 * double invd = 1.0/3.0; double mudt = mu*dt; double mx = mudt*(invd-yt[0])+nx;
					 * double my = mudt*(invd-yt[1])+ny;
					 */
					// mutations to any of the _other_ 2 traits
					double mudt = mu * 0.5;
					double mx = mudt * (1.0 - 3.0 * x) + nx;
					double my = mudt * (1.0 - 3.0 * y) + ny;
					dyt[0] += mx;
					dyt[1] += my;
					dyt[2] -= mx + my;
					ArrayMath.addscale(yt, dyt, step, yout);
				} else {
					dyt[0] += nx;
					dyt[1] += ny;
					if (yt[2] > 0.0)
						dyt[2] -= nx + ny;
					// in the absence of mutations, extinct traits (or species) must not make
					// a sudden reappearance due to roundoff errors!
					for (int i = 0; i < nDim; i++)
						if (yt[i] > 0.0)
							yout[i] = yt[i] + step * dyt[i];
						else
							yout[i] = 0.0;
				}
				break;

			default: // any number of traits (single traits in multiple species)
				int skip = 0;
				if (isDensity) {
					for (Module mod : species) {
						double noise = Math.sqrt(getEffectiveNoise(mod, skip)) * rng.nextGaussian() * sqrtdt;
						// species that went extinct should not make a sudden reappearance
						if (yt[skip] > 0.0) {
							dyt[skip] += noise;
							yout[skip] = yt[skip] + step * dyt[skip];
						}
						skip += mod.getNTraits();
					}
					break;
				}
				// frequency dynamics
				for (Module mod : species) {
					// no mutations in ecological processes
					process2DNoise(skip, step, sqrtdt, 0.0, getEffectiveNoise(mod, skip));
					skip += mod.getNTraits();
				}
				break;
		}
		// polish result
		idx = ArrayMath.minIndex(yout);
		if (yout[idx] < 0.0) {
			// step too big, resulted in negative densities/frequencies
			// note, yt[idx]>0 must hold (from previous step) but
			// sign of dyt[idx] depends on direction of integration
			step = -yt[idx] / dyt[idx]; // note: dyt[idx]<0 -> step>0
			ArrayMath.addscale(yt, dyt, step, yout);
			yout[idx] = 0.0; // avoid roundoff errors
		}
		if (!isDensity) {
			idx = ArrayMath.maxIndex(yout);
			if (yout[idx] > 1.0) {
				// step too big, resulted in frequencies >1
				step = (1.0 - yt[idx]) / dyt[idx];
				ArrayMath.addscale(yt, dyt, step, yout);
				yout[idx] = 1.0; // avoid roundoff errors
			}
			normalizeState(yout);
		}
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
	 * Helper method to process noise with two dependent traits.
	 * 
	 * @param skip	the start index of the two traits
	 * @param step	the step size
	 * @param sqrtdt the square root of the step size
	 * @param mu	the mutation rate
	 * @param noise the noise to be processed
	 */
	private void process2DNoise(int skip, double step, double sqrtdt, double mu, double noise) {
		double x = yt[skip];
		double b = ((1.0 - mu) * x * (1.0 - x) + mu) * noise;
		double c = Math.sqrt(b);
		double n = c * rng.nextGaussian() * sqrtdt;
		dyt[skip] += n;
		int skip1 = skip + 1;
		dyt[skip1] -= n;
		if (mu > 0.0) {
			yout[skip] = yt[skip] + step * dyt[skip];
			yout[skip1] = yt[skip1] + step * dyt[skip1];
		} else {
			// in the absence of mutations, extinct traits (or species) must not make
			// a sudden reappearance due to roundoff errors!
			if (yt[skip] > 0.0)
				yout[skip] = yt[skip] + step * dyt[skip];
			else
				yout[skip] = 0.0;
			if (yt[skip1] > 0.0)
				yout[skip1] = yt[skip1] + step * dyt[skip1];
			else
				yout[skip1] = 0.0;
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
	private double getEffectiveNoise(Module mod, int skip) {
		double effnoise = 1.0 / mod.getNPopulation();
		// scale noise according effective population size
		int vacant = skip + mod.getVacant();
		if (vacant >= 0) {
			double ytv = yt[vacant];
			if (ytv > 1.0 - 1e-8)
				return 0.0;
			effnoise /= (1.0 - ytv);
		}
		return effnoise;
	}

	@Override
	public void readStatisticsSample() {
		super.readStatisticsSample();
		if (fixData == null || nSpecies > 1)
			return;

		// collect new statistics sample
		Module mod = engine.getModule();
		fixData.typeFixed = ArrayMath.maxIndex(yt);
		int vacant = mod.getVacant();
		if (fixData.typeFixed == vacant) {
			// closer look is needed - look for what other trait survived (if any)
			for (int n = 0; n < mod.getNTraits(); n++) {
				if (n == vacant)
					continue;
				if (yt[n] > 0) {
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
		Module mod = engine.getModule();
		if (!(mod instanceof HasHistogram.StatisticsProbability
				|| mod instanceof HasHistogram.StatisticsTime)
				|| mod.getMutation().probability > 0.0)
			return false;
		// sampling statistics also require:
		// - mutant initialization (same as temperature in well-mixed populations)
		// - convergence unaffected by vacant sites
		return initType[0].equals(InitType.MUTANT);
	}

	@Override
	public boolean permitsUpdateStatistics() {
		for (Module mod : species) {
			if (!(mod instanceof HasHistogram.StatisticsTime))
				return false;
		}
		return true;
	}

	@Override
	public void collectCLO(CLOParser parser) {
		super.collectCLO(parser);
		// SDE's currently are restricted to single species modules and implement
		// mutation to other types only (including ALL as well should be fairly straight
		// forward, though).
		if (mutation != null && mutation.length > 0) {
			CLOption clo = mutation[0].clo;
			clo.clearKeys();
			clo.addKey(Mutation.Discrete.Type.NONE);
			clo.addKey(Mutation.Discrete.Type.OTHER);
		}
	}
}
