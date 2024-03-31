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

import org.evoludo.math.ArrayMath;
import org.evoludo.simulator.EvoLudo;
import org.evoludo.simulator.modules.Module;
import org.evoludo.simulator.modules.Mutation;
import org.evoludo.util.CLOParser;
import org.evoludo.util.Formatter;

import no.uib.cipr.matrix.DenseMatrix;
import no.uib.cipr.matrix.DenseVector;
import no.uib.cipr.matrix.Matrices;
import no.uib.cipr.matrix.SymmDenseEVD;
import no.uib.cipr.matrix.UpperSPDDenseMatrix;

/**
 * Integrator for stochastic differential equations (SDE) based on Euler's
 * method for more than three traits (dynamical variables) and replicator type
 * dynamics. This requires the <code>no.uib.cipr.matrix</code>-library for
 * numerical matrix manipulations. Due to this library dependence this model is
 * available only to JRE. For a possible GWT port check out
 * <a href="https://mathjs.org">MathJS</a>.
 * 
 * @author Christoph Hauert
 */
public class SDEEulerN extends SDEEuler {

	/**
	 * Diffusion matrix \(\mathbf{B}\) in the Fokker-Planck equation. \(\mathbf{B}\)
	 * is symmetric with \(b_{ij}=b_{ji}\).
	 */
	UpperSPDDenseMatrix matB;

	/**
	 * Transformation matrix \(\mathbf{U}\), such that \(\mathbf{B = U\ L\
	 * U^\trans}\).
	 */
	DenseMatrix matU;

	/**
	 * Diagonal matrix \(\mathbf{L}\) with the eigenvalues on the diagonal.
	 */
	DenseMatrix matL;

	/**
	 * Noise matrix \(\mathbf{C}\) in the Langevin equation with \(\mathbf{C = U\ l\
	 * U^\trans}\), where \(l\) is a matrix with the square roots of the eigenvalues
	 * on the diagonal. This is the equivalent of \(\mathbf{B}\) in the
	 * Fokker-Planck equation.
	 */
	DenseMatrix matC;

	/**
	 * Matrix to store intermediate results.
	 */
	DenseMatrix matTmp;

	/**
	 * Gaussian noise vector.
	 */
	DenseVector gaussian;

	/**
	 * State vector. The current frequencies of the system, equivalent to
	 * <code>yt</code>.
	 * 
	 * @see ODEEuler#yt
	 */
	DenseVector vecyt;

	/**
	 * Numerical solver to calculate the eigenvalues and eigenvectors of a matrix.
	 */
	SymmDenseEVD solver;

	/**
	 * Constructs a new model for the numerical integration of the system of
	 * stochastic differential equations with arbitrary numbers of dynamical
	 * variables representing the dynamics specified by the {@link Module}
	 * <code>module</code> using the {@link EvoLudo} pacemaker <code>engine</code>
	 * to control the numerical evaluations. The integrator implements Euler's
	 * method (fixed step size).
	 * <p>
	 * <strong>Important:</strong> for reproducibility the shared random number
	 * generator should be used.
	 * 
	 * @param engine the pacemeaker for running the model
	 * @param module the module to numerically integrate
	 * 
	 * @see SDEEuler
	 * @see EvoLudo#getRNG()
	 */
	public SDEEulerN(EvoLudo engine) {
		super(engine);
	}

	@Override
	public synchronized void unload() {
		destroy();
		super.unload();
	}

	private void destroy() {
		matB = null;
		matU = matL = matC = matTmp = null;
		solver = null;
		gaussian = vecyt = null;
	}

	@Override
	public void reset() {
		super.reset();
		int d1 = nDim - 1;
		if (d1 < 3) {
			// use optimized SDEEuler method instead
			destroy();
			return;
		}
		if (matB != null && matB.numRows() == d1)
			return;

		matB = new UpperSPDDenseMatrix(d1);
		matL = Matrices.identity(d1);
		matC = new DenseMatrix(d1, d1);
		matTmp = new DenseMatrix(d1, d1);
		solver = new SymmDenseEVD(d1, true);
		gaussian = new DenseVector(d1);
		vecyt = new DenseVector(d1);
	}

	/**
	 * {@inheritDoc}
	 *
	 * <h3>Implementation Notes:</h3>
	 * Without mutations the generic approach to integrate the SDE fails (zero
	 * eigenvalues)! The solution is to restrict analysis to types that are still
	 * present in the population. Not yet implemented. Requires further coding.
	 */
	@Override
	public boolean check() {
		boolean doReset = super.check();
		// super may change the model to ODE
		if (engine.getModel() != this)
			return doReset;
		if (nDim > 3 && (mutation[0].type == Mutation.Discrete.Type.NONE || mutation[0].probability <= 0.0)) {
			mutation[0].type = Mutation.Discrete.Type.OTHER;
			mutation[0].probability = 1.0 / module.getNPopulation();
			engine.getLogger().warning("non-zero mutation rate required for n>3 traits, changed to "
					+ Formatter.formatSci(mutation[0].probability, 2) + ".");
		}
		return doReset;
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * <strong>Note:</strong> for two or three traits (dynamical variables) the
	 * calculations are delegated to the parent class {@link SDEEuler}, which uses
	 * optimized routines based on analytical solutions.
	 */
	@Override
	protected double deStep(double step) {
		if (nDim < 4)
			return super.deStep(step);

		double stepSize = Math.abs(step);
		double sqrtdt = Math.sqrt(stepSize) / stepSize;
		int idx;
		// effective noise may vary for populations of variable size
		double effnoise = 1.0 / module.getNPopulation();
		// scale noise according effective population size
		int vacant = module.getVacant();
		if (vacant >= 0) {
			double ytv = yt[vacant];
			if (ytv > 1.0 - 1e-8)
				return 0.0;
			effnoise /= (1.0 - ytv);
		}
		// SDE's only support single species at this time
		double mu = mutation[0].probability;
		// NOTE: if not replicator equation some adjustments are required (references to
		// e.g. yt[2] would fail)
		int d1 = nDim - 1;
		double d2 = nDim - 2.0;
		double muid1 = mu / (nDim - 1.0);
		double mu1 = 1.0 - mu;
		for (int i = 0; i < d1; i++) {
			matB.set(i, i, (yt[i] * (1.0 - yt[i]) * mu1 + muid1 * (1.0 + yt[i] * d2)) * effnoise);
			for (int j = i + 1; j < d1; j++)
				matB.set(i, j, (-yt[i] * yt[j] * mu1 - muid1 * (yt[i] + yt[j])) * effnoise);
		}
		try {
			solver.factor(matB);
		} catch (no.uib.cipr.matrix.NotConvergedException nce) {
			engine.getLogger().warning("matrix factorization failed...");
		}
		double[] ev = solver.getEigenvalues();
		// IMPORTANT: this procedure fails if one (or more) type(s) goes extinct with
		// mu=0 (results in eigenvalues of zero);
		// check() above needs to address/prevents this
		for (int i = 0; i < d1; i++)
			matL.set(i, i, Math.sqrt(ev[i]));
		matU = solver.getEigenvectors();
		matU.mult(matL, matTmp);
		matTmp.transBmult(matU, matC);
		// generate noise vector
		for (int i = 0; i < d1; i++)
			gaussian.set(i, rng.nextGaussian() * sqrtdt);
		matC.mult(gaussian, vecyt);
		double[] vecytraw = vecyt.getData();
		if (mu > 0.0) {
			// the deterministic drift term also depends on mutations
			ArrayMath.multiply(dyt, 1.0 - mu);
			// mutations to any of the d strategies
			/*
			 * double invd = 1.0/n; double mudt = mu*dt; double mx = mudt*(invd-yt[0])+nx;
			 * double my = mudt*(invd-yt[1])+ny;
			 */
			// mutations to any of the _other_ d-1 strategies
			double mudt = mu / (nDim - 1.0);
			double sum = 0.0;
			double dtmu1 = mu1;
			for (int i = 0; i < d1; i++) {
				double dmu = mudt * (1.0 - nDim * yt[i]) + vecytraw[i];
				sum += dmu;
				dyt[i] = dyt[i] * dtmu1 + dmu;
			}
			dyt[d1] = dyt[d1] * dtmu1 - sum;
			ArrayMath.addscale(yt, dyt, step, yout);
		} else {
			double sum = 0.0;
			for (int i = 0; i < d1; i++) {
				double dmu = vecytraw[i];
				sum += dmu;
				dyt[i] += dmu;
			}
			if (yt[d1] > 0.0)
				dyt[d1] -= sum;
			// extinct species must'nt make a sudden reappearance due to roundoff errors!
			for (int i = 0; i < nDim; i++)
				if (yt[i] > 0.0)
					yout[i] = yt[i] + step * dyt[i];
				else
					yout[i] = 0.0;
		}
		idx = ArrayMath.minIndex(yout);
		if (yout[idx] < 0.0) {
			step = -yt[idx] / dyt[idx]; // note: dy[idx]<0 -> ddt>0
			// ensure all frequencies are positive - despite roundoff errors
			for (int i = 0; i < nDim; i++)
				// XXX shouldn't this be stepTaken?
				yout[i] = Math.max(0.0, yt[i] + step * dyt[i]);
			yout[idx] = 0.0; // avoid roundoff errors
		}
		normalizeState(yout);
		// the new state is in yout - swap and determine new fitness
		double[] swap = yt;
		yt = yout;
		yout = swap;
		// determine fitness of new state
		getDerivatives(t, yt, ft, dyt);
		t += step;
		return ArrayMath.distSq(yout, yt);
	}

	@Override
	public void collectCLO(CLOParser parser) {
		super.collectCLO(parser);
		// SDE's with d>3 traits currently require mutations.
		if (module.getNActive() > 3) {
			mutation[0].clo.removeKey(Mutation.Discrete.Type.NONE);
			mutation[0].clo.setDefault("other 0.01");
		}
	}
}
