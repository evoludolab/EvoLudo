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

import org.evoludo.simulator.EvoLudo;

import com.google.gwt.core.client.Scheduler;

/**
 * Supervisor of reaction-diffusion processes. Coordinates calculations of the
 * next step. Optimized implementation for GWT which uses scheduling to prevent
 * computations from blocking the GUI.
 *
 * @author Christoph Hauert
 */
public class PDESupervisorGWT extends PDESupervisor {

	/**
	 * Creates a new supervisor to manage the PDE calculations of model
	 * <strong>charge</strong> with scheduling in GWT.
	 * 
	 * @param engine the pacemaker for running the model
	 * @param charge the model to supervise
	 */
	public PDESupervisorGWT(EvoLudo engine, PDERD charge) {
		super(engine, charge);
	}

	/**
	 * <code>true</code> if calculations are already in progress. This prevents the
	 * concurrent scheduling of redundant tasks.
	 */
	boolean inProgress = false;

	@Override
	public void unload() {
		super.unload();
		inProgress = false;
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * <strong>Note:</strong> Reaction and diffusion steps could be scheduled
	 * independently or even split into smaller chunks but for now GUI remains
	 * responsive even for {@code 101×101} grid.
	 */
	@Override
	public boolean next(double stepDt) {
		if (inProgress)
			return true;
		inProgress = true;
		final double timeStop = charge.getTime() + stepDt;
		final double dt = charge.getDt();
		double acc = charge.getAccuracy();
		final double acc2 = acc * acc;
		final double acc2dt2 = acc2 * dt * dt;
		charge.initDiffusion(dt);
		Scheduler.get().scheduleIncremental(new Scheduler.RepeatingCommand() {
			@Override
			public boolean execute() {
				// check if emergency brake was pulled (e.g. when unloading running model)
				if (!inProgress)
					return false;
				double timeRemain = timeStop - charge.getTime();
				boolean cont = true;
				double change = Double.MAX_VALUE;
				if (timeRemain > charge.getDt()) {
					diffuse();
					change = react();
					// at this point, fitness and density are synchronized
					// the new density distribution is in 'next'
					cont = charge.incrementTime(dt);
					if (!cont) {
						engine.modelNextDone(false);
						inProgress = false;
						return false;
					}
					if (change > acc2dt2)
						return true;
					charge.setConverged();
					engine.modelNextDone(false);
					inProgress = false;
					return false;
				}
				// update remainder (if necessary)
				if (timeRemain > 1e-6) {
					charge.initDiffusion(timeRemain);
					diffuse();
					change = react();
					cont = charge.incrementTime(timeRemain);
				}
				boolean converged = !(change > acc2 * timeRemain * timeRemain);
				if (converged) {
					charge.setConverged();
					cont = false;
				}
				engine.modelNextDone(cont);
				inProgress = false;
				return false;
			}
		});
		return true;
	}
}
