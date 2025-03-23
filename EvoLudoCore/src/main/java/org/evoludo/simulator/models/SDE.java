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
import org.evoludo.simulator.models.ChangeListener.PendingAction;
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
	 * Interface for modules that implement stochastic differential equations (SDE).
	 */
	public interface HasSDE extends HasDE {

		/**
		 * Provides opportunity for module to supply custom implementation for
		 * numerically integrating stochastic differential equations, SDE.
		 * <p>
		 * <strong>Important:</strong> if the custom SDE implementation involves random
		 * numbers, the shared random number generator should be used for
		 * reproducibility
		 * 
		 * @return the custom implementation of the SDE integrator or <code>null</code>
		 *         to use the default
		 * 
		 * @see EvoLudo#getRNG()
		 */
		public default Model createSDE() {
			return null;
		}
	}

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
		if (species.size() > 1) {
			logger.warning("SDE model for inter-species interactions not (yet?) implemented - revert to ODE.");
			engine.loadModel(Type.ODE);
			return true;
		}
		if (isDensity()) {
			logger.warning("SDE model requires fixed population size - revert to ODE.");
			engine.loadModel(Type.ODE);
			return true;
		}
		boolean doReset = super.check();
		if (dependents[0] < 0) {
			logger.warning(getClass().getSimpleName()
					+ " - noise only for replicator type dynamics implemented - revert to ODE (no noise)!");
			engine.loadModel(Type.ODE);
			return true;
		}
		// at this point it is clear that we have a dependent trait
		int dim = nDim - 1;
		// only one or two traits acceptable or, alternatively, two or three
		// traits for replicator dynamics (for SDE but not SDEN)
		if (!getClass().getSuperclass().equals(SDE.class) && (dim < 1 || dim > 2)) {
			logger.warning(getClass().getSimpleName()
					+ " - too many traits (max 2, or 3 for replicator) - revert to ODE!");
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
		resetStatisticsSample();
	}

	@Override
	public boolean next() {
		// start new statistics sample if required
		if (mode == Mode.STATISTICS_SAMPLE && statisticsSampleNew) {
			reset();
			init();
			if (fixData.mutantNode < 0) {
				initStatisticsFailed();
				engine.requestAction(PendingAction.STATISTIC_FAILED, true);
				// check if STOP has been requested
				return engine.isRunning();
			}
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
		if (mutation[0].probability > 0.0) {
			// if dist2 is zero (or very small) and (at least) one trait is absent, 
			// random noise may be invalid (pushing state outside of permissible values)
			if (dist2 < accuracy && ArrayMath.min(yt) < accuracy)
				return false;
			int vacant = module.getVacant();
			// extinction is absorbing even with mutations
			converged = (vacant < 0 ? false : (yt[vacant] > 1.0 - accuracy));
		} else
			converged = monoStop ? isMonomorphic() : (dependents[0] >= 0 && ArrayMath.max(yt) > 1.0 - accuracy);
		return converged;
	}

	/**
	 * {@inheritDoc} For stochastic differential equations Gaussian distributed
	 * white noise is added.
	 * 
	 * <h3>Implementation Notes:</h3>
	 * Integration of SDEs can be optimized in 1 and 2 dimensions for replicator
	 * systems this means 2 or 3 strategies. Currently noise implemented only for
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
		double effnoise = 1.0 / module.getNPopulation();
		// scale noise according effective population size
		int vacant = module.getVacant();
		if (vacant >= 0) {
			double ytv = yt[vacant];
			if (ytv > 1.0 - 1e-8)
				return 0.0;
			effnoise /= (1.0 - ytv);
		}
		double mu = mutation[0].probability;
		switch (nDim) {
			case 2: // two strategies
				x = yt[0];
				double b = ((1.0 - mu) * x * (1.0 - x) + mu) * effnoise;
				double c = Math.sqrt(b);
				double n = c * rng.nextGaussian() * sqrtdt;
				dyt[0] += n;
				dyt[1] -= n;
				if (mu > 0.0) {
					yout[0] = yt[0] + step * dyt[0];
					yout[1] = yt[1] + step * dyt[1];
				} else {
					// in the absence of mutations, extinct traits (or species) must not make
					// a sudden reappearance due to roundoff errors!
					if (yt[0] > 0.0)
						yout[0] = yt[0] + step * dyt[0];
					else
						yout[0] = 0.0;
					if (yt[1] > 0.0)
						yout[1] = yt[1] + step * dyt[1];
					else
						yout[1] = 0.0;
				}
				break;

			case 3: // two dimensions (or three strategies) - e.g. RSP game
				// NOTE: if not replicator equation some adjustments are required (references to
				// e.g. yt[2] would fail)
				// 1) stochastic term
				x = yt[0];
				double y = yt[1];
				double x2 = x * x, xy = x * y, y2 = y * y;
				// B matrix
				// mutations need careful definition - generate any strategy vs any of the
				// _other_ strategies.

				// mutations to any strategy (may result in no change)
				// double bxx = (x-x2+mu*(x2+(1.0-x-x)/3.0))*noise;
				// double bxy, byx = bxy = (-xy+mu*(xy-(x+y)/3.0))*noise;
				// double byy = (y-y2+mu*(y2+(1.0-y-y)/3.0))*noise;

				// mutations to only other strategies
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
					// mutations to any of the 3 strategies
					/*
					 * double invd = 1.0/3.0; double mudt = mu*dt; double mx = mudt*(invd-yt[0])+nx;
					 * double my = mudt*(invd-yt[1])+ny;
					 */
					// mutations to any of the _other_ 2 strategies
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

			default: // any number of strategies
				throw new Error("SDE dimension d>3 not implemented (use SDEN)!");
		}
		// polish result
		idx = ArrayMath.minIndex(yout);
		if (yout[idx] < 0.0) {
			// step too big, resulted in negative densities/frequencies
			// note, yt[idx]>0 must hold (from previous step) but
			// sign of dyt[idx] depends on direction of integration
			step = -yt[idx] / dyt[idx]; // note: dyt[idx]<0 -> step>0
			// ensure all frequencies are positive - despite roundoff errors
			for (int i = 0; i < nDim; i++)
				yout[i] = Math.max(0.0, yt[i] + step * dyt[i]);
			yout[idx] = 0.0; // avoid roundoff errors
		}
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

	@Override
	public void readStatisticsSample() {
		super.readStatisticsSample();
		if (fixData == null)
			return;

		// collect new statistics sample
		fixData.typeFixed = ArrayMath.maxIndex(yt);
		int vacant = module.getVacant();
		if (fixData.typeFixed == vacant) {
			// closer look is needed - look for what other strategy survived (if any)
			for (int n = 0; n < module.getNTraits(); n++) {
				if (n == vacant)
					continue;
				if (yt[n] > 0) {
					// no other strategies should be present
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
		if (!(module instanceof HasHistogram.StatisticsProbability
				|| module instanceof HasHistogram.StatisticsTime)
				|| module.getMutation().probability > 0.0)
			return false;
		// sampling statistics also require:
		// - mutant initialization (same as temperature in well-mixed populations)
		// - convergence unaffected by vacant sites
		return initType[0].equals(InitType.MUTANT);
	}

	@Override
	public boolean permitsUpdateStatistics() {
		return (module instanceof HasHistogram.StatisticsTime);
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
