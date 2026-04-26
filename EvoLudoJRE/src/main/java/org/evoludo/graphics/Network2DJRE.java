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

package org.evoludo.graphics;

import java.util.ArrayList;
import java.util.List;

import org.evoludo.simulator.EvoLudo;
import org.evoludo.simulator.EvoLudoJRE;
import org.evoludo.simulator.Network2D;
import org.evoludo.simulator.geometries.AbstractGeometry;
import org.evoludo.simulator.geometries.GeometryType;

/**
 * JRE implementation of the 2D network layout that parallelizes relaxation
 * work across worker threads.
 * 
 * @author Christoph Hauert
 */
public class Network2DJRE extends Network2D implements Runnable {

	// A 1:1 COPY OF THE FOLLOWING CODE IS IN Network3D - FIND WAY TO PREVENT CODE
	// DUPLICATION

	/**
	 * The worker pool used for parallel layout relaxation.
	 */
	List<NetLayoutWorker> workers = new ArrayList<NetLayoutWorker>(1);

	/**
	 * Create a new JRE-backed 2D network layout.
	 * 
	 * @param engine   the pacemaker for running the model
	 * @param geometry the structure of the population
	 */
	public Network2DJRE(EvoLudo engine, AbstractGeometry geometry) {
		super(engine, geometry);
	}

	@Override
	public void doLayout(LayoutListener ll) {
		Status stat = getStatus();
		if (!stat.requiresLayout())
			return;
		this.listener = ll;
		if (stat == Status.NEEDS_LAYOUT && geometry.isType(GeometryType.WELLMIXED)) {
			doLayoutPrep();
			completeLayout();
			return;
		}
		// doLayout(ll, true); // default should be to compute layout in separate thread
		doLayout(ll, false);
	}

	/**
	 * Start the layouting process either in the current thread or in a dedicated
	 * boss thread.
	 * 
	 * @param ll       the layout listener
	 * @param inThread {@code true} to run the boss in its own thread
	 */
	public void doLayout(LayoutListener ll, boolean inThread) {
		this.listener = ll;
		if (inThread)
			newLayoutThread(this, "NetLayoutBoss").start();
		else
			run();
	}

	/**
	 * The number of worker results still outstanding for the current relaxation
	 * sweep.
	 */
	private int pending;

	@Override
	public synchronized void run() {
		doLayoutPrep(); // must call first as it may still allocate nodes etc.
		hireWorkers();
		double start = System.currentTimeMillis();

		// multi-threaded way of layouting
		double adjust;
		do {
			pending = workers.size();
			potential = 0.0;
			for (NetLayoutWorker worker : workers)
				worker.task(NetLayoutWorker.NL_TASK_RELAX);
			// wait for completion
			while (pending > 0) {
				try {
					wait();
				} catch (InterruptedException e) {
				}
			}
			potential *= norm;
			adjust = Math.abs(potential - prevPotential);
			prevPotential = potential;
			prevAdjust = Math.min(prevAdjust, adjust);
			double threshold = getConvergenceAccuracy();
			if (adjust < threshold || (System.currentTimeMillis() - start) > layoutTimeout) {
				isRunning = false;
				break;
			}
			listener.layoutUpdate(threshold / adjust);
		} while (isRunning);

		// finish layout (center and scale network)
		fireWorkers();
		completeLayout();
	}

	/**
	 * Minimal number of nodes assigned per worker before another worker thread is
	 * created.
	 */
	private static final int MIN_WORKLOAD = 100;

	/**
	 * Spawn layout workers and partition the node range between them.
	 */
	private void hireWorkers() {
		if (workers.size() > 0)
			fireWorkers();
		int nWorkers = Runtime.getRuntime().availableProcessors();
		// if there are more than two processors, reserve one for painting
		if (nWorkers > 2)
			nWorkers--;
		nWorkers = Math.min(nWorkers, Math.max(1, nNodes / MIN_WORKLOAD));
		NetLayoutWorker worker;
		int start, end = 0;
		int incr = nNodes / nWorkers;
		for (int i = 0; i < nWorkers - 1; i++) {
			start = end;
			end = start + incr;
			worker = new NetLayoutWorker(start, end);
			newLayoutThread(worker, "NetLayoutWorker-" + (i + 1)).start();
			workers.add(worker);
		}
		// the last worker may be responsible for a few more nodes in case data.nData %
		// count != 0.
		worker = new NetLayoutWorker(end, nNodes);
		newLayoutThread(worker, "NetLayoutWorker-" + nWorkers).start();
		workers.add(worker);
	}

	/**
	 * Create a layout thread.
	 * <p>
	 * Headless launches should not be kept alive by outstanding layout workers once
	 * the main simulation thread has finished.
	 * 
	 * @param worker the layout runnable
	 * @param name   the thread name
	 * @return the configured thread
	 */
	private Thread newLayoutThread(Runnable worker, String name) {
		Thread thread = new Thread(worker, name);
		if (engine instanceof EvoLudoJRE jre)
			thread.setDaemon(jre.isHeadless());
		return thread;
	}

	/**
	 * Stop all worker threads and clear the worker pool.
	 */
	private void fireWorkers() {
		for (NetLayoutWorker worker : workers)
			worker.task(NetLayoutWorker.NL_TASK_EXIT);
		workers.clear();
	}

	/**
	 * Receive the result of a worker task and notify the boss thread once the
	 * current sweep has completed.
	 * 
	 * @param task   the completed task type
	 * @param result the worker's contribution to the potential energy
	 */
	public synchronized void workerComplete(int task, double result) {
		potential += result;
		if (--pending > 0)
			return;
		// System.out.println("Network2DJRE workerComplete: pending="+pending+",
		// potential="+potential+", result="+result);
		notify();
	}

	/**
	 * Worker that relaxes a contiguous range of nodes assigned by the boss thread.
	 */
	public class NetLayoutWorker implements Runnable {

		/**
		 * The owning layout manager.
		 */
		Network2DJRE boss;

		/**
		 * The inclusive start and exclusive end index of the assigned node range.
		 */
		int start, end;

		/**
		 * The current task assigned to this worker.
		 */
		int task = NL_TASK_IDLE;

		/**
		 * Task code indicating that the worker is idle.
		 */
		public static final int NL_TASK_IDLE = 0;

		/**
		 * Task code requesting a relaxation sweep over the assigned node range.
		 */
		public static final int NL_TASK_RELAX = 1;

		/**
		 * Task code requesting worker shutdown.
		 */
		public static final int NL_TASK_EXIT = 2;

		/**
		 * Create a worker for the specified node range.
		 * 
		 * @param start the inclusive start index
		 * @param end   the exclusive end index
		 */
		public NetLayoutWorker(int start, int end) {
			boss = Network2DJRE.this;
			this.start = start;
			this.end = end;
		}

		@Override
		public synchronized void run() {
			while (true) {
				switch (task) {
					// case NL_TASK_IDLE:
					default:
						try {
							wait();
						} catch (InterruptedException e) {
						}
						break;

					case NL_TASK_RELAX:
						double taskpotential = 0.0;
						for (int n = start; n < end; n++)
							taskpotential += relax(n);
						boss.workerComplete(task, taskpotential);
						task = NL_TASK_IDLE;
						break;

					case NL_TASK_EXIT:
						return;
				}
			}
		}

		/**
		 * Assign a new task to this worker.
		 * 
		 * @param tsk the task code
		 */
		public synchronized void task(int tsk) {
			task = tsk;
			notify();
		}
	}
}
