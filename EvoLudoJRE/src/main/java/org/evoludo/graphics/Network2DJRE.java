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

package org.evoludo.graphics;

import java.util.ArrayList;
import java.util.List;

import org.evoludo.simulator.EvoLudo;
import org.evoludo.simulator.geom.AbstractGeometry;
import org.evoludo.simulator.Network2D;

public class Network2DJRE extends Network2D implements Runnable {

	// A 1:1 COPY OF THE FOLLOWING CODE IS IN Network3D - FIND WAY TO PREVENT CODE
	// DUPLICATION

	List<NetLayoutWorker> workers = new ArrayList<NetLayoutWorker>(1);

	public Network2DJRE(EvoLudo engine, AbstractGeometry geometry) {
		super(engine, geometry);
	}

	@Override
	public void doLayout(LayoutListener ll) {
		if (!status.requiresLayout())
			return;
		// doLayout(ll, true); // default should be to compute layout in separate thread
		doLayout(ll, false);
	}

	public void doLayout(LayoutListener ll, boolean inThread) {
		this.listener = ll;
		if (inThread)
			new Thread(this, "NetLayoutBoss").start();
		else
			run();
	}

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
			if (adjust < accuracy || (System.currentTimeMillis() - start) > layoutTimeout) {
				isRunning = false;
				break;
			}
			listener.layoutUpdate(accuracy / adjust);
		} while (isRunning);

		// finish layout (center and scale network)
		fireWorkers();
		finishLayout();
		setStatus(Status.HAS_LAYOUT);
		listener.layoutComplete();
	}

	private static final int MIN_WORKLOAD = 100;

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
			new Thread(worker, "NetLayoutWorker-" + (i + 1)).start();
			workers.add(worker);
		}
		// the last worker may be responsible for a few more nodes in case data.nData %
		// count != 0.
		worker = new NetLayoutWorker(end, nNodes);
		new Thread(worker, "NetLayoutWorker-" + nWorkers).start();
		workers.add(worker);
	}

	private void fireWorkers() {
		for (NetLayoutWorker worker : workers)
			worker.task(NetLayoutWorker.NL_TASK_EXIT);
		workers.clear();
	}

	public synchronized void workerComplete(int task, double result) {
		potential += result;
		if (--pending > 0)
			return;
		// System.out.println("Network2DJRE workerComplete: pending="+pending+",
		// potential="+potential+", result="+result);
		notify();
	}

	public class NetLayoutWorker implements Runnable {
		Network2DJRE boss;
		int start, end;
		int task = NL_TASK_IDLE;

		public static final int NL_TASK_IDLE = 0;
		public static final int NL_TASK_RELAX = 1;
		public static final int NL_TASK_EXIT = 2;

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

		public synchronized void task(int tsk) {
			task = tsk;
			notify();
		}
	}
}
