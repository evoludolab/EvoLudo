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

import java.util.ArrayList;
import java.util.List;

import org.evoludo.simulator.EvoLudo;
import org.evoludo.simulator.models.PDEWorkerPool.Task;
import org.evoludo.simulator.modules.Features.Payoffs;

/**
 * JRE advection PDE implementation using worker threads for reaction and
 * diffusion steps.
 *
 * @author Christoph Hauert
 */
public class AdvectionJRE extends Advection {

	/**
	 * The list of worker threads.
	 */
	protected List<RDWorker> workers = new ArrayList<>(0);

	/**
	 * The total change in state accumulated over a reaction step.
	 */
	protected double change;

	/**
	 * Scaled diffusion coefficients currently processed by the worker pool.
	 */
	private double[] scaledD;

	/**
	 * Scaled advection coefficients currently processed by the worker pool.
	 */
	private double[][] scaledA;

	/**
	 * Integration step currently processed by the worker pool.
	 */
	private double reactStep;

	/**
	 * The number of workers that have not yet finished their task.
	 */
	private int pending = -1;

	/**
	 * Creates a new JRE advection PDE model with threaded execution support.
	 *
	 * @param engine the pacemaker for running the model
	 */
	public AdvectionJRE(EvoLudo engine) {
		super(engine);
	}

	@Override
	public void reset() {
		super.reset();
		PDEWorkerPool.fireWorkers(workers);
		PDEWorkerPool.hireWorkers(engine, workers, getGeometry().getSize(), RDWorker.RD_MIN_WORKLOAD,
				(start, end) -> new RDWorker(this, start, end));
	}

	@Override
	public synchronized void unload() {
		pending = -1;
		PDEWorkerPool.fireWorkers(workers);
		synchronized (this) {
			notifyAll();
		}
		super.unload();
	}

	@Override
	public synchronized double react(double stepSize) {
		change = 0.0;
		reactStep = stepSize;
		if (module instanceof Payoffs)
			resetFitness();
		pending = workers.size();
		for (RDWorker worker : workers)
			worker.task(Task.REACT);
		while (pending > 0) {
			try {
				wait();
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}
		if (module instanceof Payoffs)
			normalizeMeanFitness();
		return change;
	}

	@Override
	public synchronized void diffuse(double[] scaledD, double[][] scaledA) {
		this.scaledD = scaledD;
		this.scaledA = scaledA;
		resetDensity();
		pending = workers.size();
		for (RDWorker worker : workers)
			worker.task(Task.DIFFUSE);
		while (pending > 0) {
			try {
				wait();
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}
		normalizeMeanDensity();
	}

	/**
	 * Called after the diffusion step completed.
	 */
	protected synchronized void done() {
		pending = PDEWorkerPool.done(pending);
		if (pending <= 0)
			notifyAll();
	}

	/**
	 * Called after the reaction step completed with cumulative change {@code incr}.
	 *
	 * @param incr the cumulative change of state in the reaction step
	 */
	protected synchronized void done(double incr) {
		change = PDEWorkerPool.done(change, incr);
		done();
	}

	/**
	 * Worker class for processing PDE updates.
	 */
	static class RDWorker implements Runnable, PDEWorkerPool.WorkerControl {

		/**
		 * The minimum number of units to process by one worker.
		 */
		public static final int RD_MIN_WORKLOAD = 1000;

		/**
		 * The reference to the worker manager.
		 */
		private final AdvectionJRE boss;

		/**
		 * The index of the first PDE unit processed by this worker.
		 */
		private final int start;

		/**
		 * The index of the last PDE unit processed by this worker.
		 */
		private final int end;

		/**
		 * The next task to address by the worker.
		 */
		private Task task = Task.IDLE;

		/**
		 * Create a new worker, which deals with PDE units {@code start} through
		 * {@code end}.
		 *
		 * @param boss  the reference to the boss
		 * @param start the first unit processed by this worker
		 * @param end   the last unit processed by this worker
		 */
		public RDWorker(AdvectionJRE boss, int start, int end) {
			this.boss = boss;
			this.start = start;
			this.end = end;
		}

		@Override
		public synchronized void run() {
			while (true) {
				switch (task) {
					case REACT:
						boss.done(boss.react(start, end, boss.reactStep));
						task = Task.IDLE;
						break;
					case DIFFUSE:
						boss.diffuse(start, end, boss.scaledD, boss.scaledA);
						boss.done();
						task = Task.IDLE;
						break;
					case EXIT:
						return;
					case IDLE:
					default:
						try {
							wait();
						} catch (InterruptedException e) {
							Thread.currentThread().interrupt();
						}
						break;
				}
			}
		}

		/**
		 * Schedule a new task and notify worker.
		 *
		 * @param task the new task
		 */
		public synchronized void task(Task task) {
			this.task = task;
			notifyAll();
		}

		@Override
		public void exit() {
			task(Task.EXIT);
		}
	}
}
