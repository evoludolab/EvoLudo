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

import org.evoludo.simulator.EvoLudo;
import org.evoludo.simulator.modules.Features.Payoffs;

/**
 * Supervisor of reaction-diffusion processes. Coordinates calculations of the
 * next step. Subclasses are encouraged to take advantage of optimizations
 * available in different frameworks, GWT and JRE in particular. This default
 * implementation does not implement any optimizations.
 *
 * @author Christoph Hauert
 */
public class PDESupervisor {

	/**
	 * Creates a new supervisor to manage the PDE calculations of model
	 * <strong>charge</strong>. Subclasses may introduce specific optimizations. In
	 * particular, parallel execution of computations for JRE and scheduling for
	 * GWT.
	 * 
	 * @param engine the pacemaker for running the model
	 * @param charge the model to supervise
	 */
	public PDESupervisor(EvoLudo engine, PDE charge) {
		this.engine = engine;
		this.charge = charge;
	}

	/**
	 * The model to manage and supervise its execution.
	 */
	protected PDE charge;

	/**
	 * The number of units in the discretization of the PDE.
	 */
	protected int nUnits;

	/**
	 * The pacemaker of all models. Interface with the outside world.
	 */
	protected EvoLudo engine;

	/**
	 * Unload the supervisor and free resources.
	 */
	public void unload() {
		// no clean up necessary in default implementation
	}

	/**
	 * Reset supervisor and update settings.
	 */
	public void reset() {
		nUnits = charge.getGeometry().getSize();
	}

	/**
	 * Called after a change of parameters.
	 */
	public synchronized void update() {
		react();
		charge.setDensity();
	}

	/**
	 * Indicates whether scheduling is used to advance the PDE model. Subclasses may
	 * override this method to indicate whether scheduling is used to avoid blocking
	 * the GUI.
	 * 
	 * @return <code>true</code> if scheduling is used; <code>false</code> otherwise
	 */
	public boolean useScheduling() {
		return false;
	}

	/**
	 * Advances the PDE model by a time step of <code>stepDt</code>. Subclasses may
	 * override this method to implement optimizations. For example, GWT uses
	 * scheduling to avoid blocking the GUI.
	 * 
	 * @param stepDt the time step to advance the PDE
	 * @return <code>true</code> if system converged
	 * 
	 * @see org.evoludo.simulator.models.PDESupervisorGWT#next(double)
	 *      PDESupervisorGWT.next(double)
	 */
	public boolean next(double stepDt) {
		final double timeStop = charge.getTime() + stepDt;
		final double dt = charge.getDt();
		final double acc = charge.getAccuracy();
		final double acc2 = acc * acc;
		final double acc2dt2 = acc2 * dt * dt;
		charge.initDiffusion(dt);
		double timeRemain = stepDt;
		double change = Double.MAX_VALUE;
		while (timeRemain > dt) {
			diffuse();
			change = react();
			// at this point, fitness and density are synchronized
			// the new density distribution is in 'next'
			charge.incrementTime(dt);
			timeRemain = timeStop - charge.getTime();
			if (change > acc2dt2)
				continue;
			charge.setConverged();
			return true;
		}
		// update remainder (if necessary)
		if (timeRemain > 1e-6) {
			charge.initDiffusion(timeRemain);
			diffuse();
			change = react();
			charge.incrementTime(timeRemain);
		}
		if (change > acc2 * timeRemain * timeRemain)
			return true;
		charge.setConverged();
		return false;
	}

	/**
	 * Perform the reaction step.
	 * <p>
	 * <strong>Important:</strong> This is not thread-safe.
	 * 
	 * @return the accumulated total change in state
	 */
	protected synchronized double react() {
		double change;
		if (charge.module instanceof Payoffs) {
			charge.resetFitness();
			change = charge.react(0, nUnits);
			charge.normalizeMeanFitness();
		} else {
			change = charge.react(0, nUnits);
		}
		return change;
	}

	/**
	 * Perform the diffusion step.
	 * <p>
	 * <strong>Important:</strong> This is not thread-safe.
	 */
	protected synchronized void diffuse() {
		charge.resetDensity();
		charge.diffuse(0, nUnits);
		charge.normalizeMeanDensity();
	}
}
