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

import java.awt.GraphicsEnvironment;
import java.util.List;
import java.util.function.BiFunction;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.evoludo.simulator.EvoLudo;

/**
 * Shared helper for JRE PDE worker thread lifecycle.
 */
final class PDEWorkerPool {

	/**
	 * Common interface for worker thread lifecycle hooks.
	 */
	interface WorkerControl {

		/**
		 * Request this worker to terminate.
		 */
		void exit();
	}

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
		 * The diffusion task. Process one diffusion step.
		 */
		DIFFUSE,

		/**
		 * The exit task. Stop working.
		 */
		EXIT
	}

	/**
	 * Utility class should not be instantiated.
	 */
	private PDEWorkerPool() {
	}

	/**
	 * Stop all workers and clear the workforce.
	 *
	 * @param workers the active workers
	 * @param <W>     the worker type
	 */
	static <W extends WorkerControl> void fireWorkers(List<W> workers) {
		for (W worker : workers)
			worker.exit();
		workers.clear();
	}

	/**
	 * Hire worker threads and assign contiguous PDE ranges to them.
	 *
	 * @param engine      the engine for logging context
	 * @param workers     the worker list to populate
	 * @param nUnits      the number of PDE units
	 * @param minWorkload the minimum work assigned to one worker
	 * @param factory     factory creating the worker for its start/end indices
	 * @param <W>         the worker type
	 */
	static <W extends Runnable & WorkerControl> void hireWorkers(EvoLudo engine, List<W> workers, int nUnits,
			int minWorkload, BiFunction<Integer, Integer, W> factory) {
		int nWorkers = Runtime.getRuntime().availableProcessors();
		if (nWorkers > 2 && !GraphicsEnvironment.isHeadless())
			nWorkers--;
		nWorkers = Math.clamp(nWorkers, 1, nUnits / minWorkload);
		logWorkers(engine, nWorkers, nUnits);
		int end = 0;
		int incr = nUnits / nWorkers;
		for (int i = 0; i < nWorkers - 1; i++) {
			int start = end;
			end = start + incr;
			W worker = factory.apply(start, end);
			new Thread(worker, "RDWorker-" + (i + 1)).start();
			workers.add(worker);
		}
		W worker = factory.apply(end, nUnits);
		new Thread(worker, "RDWorker-" + nWorkers).start();
		workers.add(worker);
	}

	/**
	 * Record completion of one worker task.
	 *
	 * @param pending the current number of pending workers
	 * @return the remaining number of pending workers
	 */
	static int done(int pending) {
		return pending - 1;
	}

	/**
	 * Accumulate change reported by one worker.
	 *
	 * @param change the current cumulative change
	 * @param incr   the additional worker contribution
	 * @return the updated cumulative change
	 */
	static double done(double change, double incr) {
		return change + incr;
	}

	/**
	 * Helper to log the number of worker threads and their workload.
	 *
	 * @param engine   the engine providing the logger
	 * @param nWorkers the number of workers
	 * @param nUnits   the number of PDE units
	 */
	private static void logWorkers(EvoLudo engine, int nWorkers, int nUnits) {
		Logger logger = engine.getLogger();
		if (!logger.isLoggable(Level.INFO))
			return;
		logger.info("Using " + nWorkers + " threads for integrating " + nUnits + " PDE units.");
	}
}
