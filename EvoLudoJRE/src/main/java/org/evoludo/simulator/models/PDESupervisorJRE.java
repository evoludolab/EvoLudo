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

import java.awt.GraphicsEnvironment;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

import org.evoludo.simulator.EvoLudo;

/**
 * Supervisor of reaction-diffusion processes. Coordinates calculations of the
 * next step. Optimized implementation for JRE which takes advantage of the
 * computational power available through multiple threads.
 *
 * @author Christoph Hauert
 */
public class PDESupervisorJRE extends PDESupervisor {

	/**
	 * Creates a new supervisor to manage the PDE calculations of model
	 * <strong>charge</strong> with multiple threads in JRE.
	 * 
	 * @param engine the pacemeaker for running the model
	 * @param charge the model to supervise
	 */
	public PDESupervisorJRE(EvoLudo engine, Model.PDE charge) {
		super(engine, charge);
	}

	/**
	 * The list of worker threads.
	 * 
	 * @see RDWorker
	 */
	protected List<RDWorker> workers = new ArrayList<RDWorker>(0);

	/**
	 * The total change in state (distance squared between previous and current
	 * states). Used for managing reactions processed by multiple threads.
	 */
	protected double change;

	/**
	 * The number of workers that have not yet finished their task.
	 */
	private int pending = -1;

	@Override
	public void unload() {
		super.unload();
		pending = -1;
		fireWorkers();
		synchronized (this) {
			notify();
		}
	}

	@Override
	public void reset() {
		super.reset();
		fireWorkers();
		hireWorkers();
	}

	/**
	 * Tell worker threads to abort calculations.
	 */
	public void fireWorkers() {
		ListIterator<RDWorker> i = workers.listIterator();
		while (i.hasNext())
			i.next().task(RDWorker.Task.EXIT);
		workers.clear();
	}

	/**
	 * Hire workforce. The number of worker threads depends on the number of
	 * available processors. With more than two processors and when sporting a GUI
	 * (as opposed to running simulations), one processor is reserved for updating
	 * the GUI for a smoother user experience.
	 */
	private void hireWorkers() {
		int nWorkers = Runtime.getRuntime().availableProcessors();
		engine.getLogger().info("Total of " + nWorkers + " processors available.");
		boolean isHeadless = GraphicsEnvironment.isHeadless();
		if (!isHeadless && nWorkers > 2)
			nWorkers--;
		nWorkers = Math.min(nWorkers, Math.max(1, nUnits / RDWorker.RD_MIN_WORKLOAD));
		engine.getLogger().info("Using " + nWorkers + " threads for integrating " + nUnits + " PDE units.");
		RDWorker worker;
		int start, end = 0;
		int incr = nUnits / nWorkers;
		for (int i = 0; i < nWorkers - 1; i++) {
			start = end;
			end = start + incr;
			worker = new RDWorker(this, charge, start, end);
			new Thread(worker, "RDWorker-" + (i + 1)).start();
			workers.add(worker);
		}
		// the last worker may be responsible for a few more nodes in case
		// data.nData % count != 0.
		worker = new RDWorker(this, charge, end, nUnits);
		new Thread(worker, "RDWorker-" + nWorkers).start();
		workers.add(worker);
	}

	/**
	 * Perform the reaction step using multiple threads (if available).
	 */
	@Override
	protected synchronized double react() {
		change = 0.0;
		charge.resetFitness();
		pending = workers.size();
		for (RDWorker worker : workers)
			worker.task(RDWorker.Task.REACT);
		// wait for completion
		while (pending > 0) {
			try {
				wait();
			} catch (InterruptedException e) {
			}
		}
		charge.normalizeMeanFitness();
		return change;
	}

	/**
	 * Perform the diffusion step using multiple threads (if available).
	 */
	@Override
	protected synchronized void diffuse() {
		charge.resetDensity();
		pending = workers.size();
		for (RDWorker worker : workers)
			worker.task(RDWorker.Task.DIFFUSE);
		// wait for completion
		while (pending > 0) {
			try {
				wait();
			} catch (InterruptedException e) {
			}
		}
		charge.normalizeMeanDensity();
		// DEBUG
		// EvoLudo.logMessage("diffuse - density:
		// mean="+ChHFormatter.formatFix(charge.meanDensity, 8)+
		// "\n min="+ChHFormatter.formatFix(charge.minDensity, 8)+
		// "\n max="+ChHFormatter.formatFix(charge.maxDensity, 8));
	}

	/**
	 * Called after the diffusion step completed. Used for managing multiple
	 * threads.
	 */
	protected synchronized void done() {
		if (--pending > 0)
			return;
		notify();
	}

	/**
	 * Called after the reaction step completed, which resulted in a cumulative
	 * change of state {@code incr}. Used for managing multiple threads.
	 * 
	 * @param incr the cumulative change of state in the reaction step
	 */
	protected synchronized void done(double incr) {
		change += incr;
		done();
	}

	/**
	 * Worker class for processing PDE updates.
	 */
	static class RDWorker implements Runnable {

		/**
		 * The reference to the boss of this worker.
		 */
		PDESupervisorJRE boss;

		/**
		 * The reference to the supervised PDE model.
		 */
		Model.PDE charge;

		/**
		 * The index of the first PDE unit processed by this worker.
		 */
		int start;

		/**
		 * The index of the last PDE unit processed by this worker.
		 */
		int end;

		/**
		 * The next task to address by the worker.
		 */
		Task task = Task.IDLE;

		/**
		 * The minimum number of units to process by one worker.
		 */
		public static final int RD_MIN_WORKLOAD = 1000;

		/**
		 * The list of possible tasks that {@link RDWorker}s can carry out:
		 * <ul>
		 * <li>The idle task. Await further instructions.
		 * <li>The reaction task. Process one reaction step.
		 * <li>The diffuse task. Process one diffusion step.
		 * <li>The exit task. Stop working.
		 * </ul>
		 */
		enum Task {

			/**
			 * The idle task. Await further instructions.
			 */
			IDLE,

			/**
			 * The reaction task. Process one reaction step.
			 */
			REACT,

			/**
			 * The diffuse task. Process one diffusion step.
			 */
			DIFFUSE,

			/**
			 * The exit task. Stop working.
			 */
			EXIT
		}

		/**
		 * Create a new worker, which deals with PDE units {@code start} through
		 * {@code end}. The worker reports to the {@code boss} and retrieves data
		 * for processing from {@code charge}.
		 * 
		 * @param boss	the reference to the boss
		 * @param charge the reference to the charge
		 * @param start the first unit processed byt this worker
		 * @param end   the last unit processed byt this worker
		 */
		public RDWorker(PDESupervisorJRE boss, Model.PDE charge, int start, int end) {
			this.boss = boss;
			this.charge = charge;
			this.start = start;
			this.end = end;
		}

		/**
		 * {@inheritDoc}
		 * <p>
		 * Perform task at hand or await further instructions.
		 */
		@Override
		public synchronized void run() {
			while (true) {
				switch (task) {
					// case IDLE:
					default:
						try {
							wait();
						} catch (InterruptedException e) {
						}
						break;
					case REACT:
						boss.done(charge.react(start, end));
						task = Task.IDLE;
						break;
					case DIFFUSE:
						charge.diffuse(start, end);
						boss.done();
						task = Task.IDLE;
						break;
					case EXIT:
						return;
				}
			}
		}

		/**
		 * Schedule new task {@code tsk} and notify worker.
		 * 
		 * @param tsk the new task
		 */
		public synchronized void task(Task tsk) {
			task = tsk;
			notify();
		}
	}
}
