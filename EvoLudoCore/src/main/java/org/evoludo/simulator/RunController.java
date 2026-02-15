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

package org.evoludo.simulator;

import java.util.ArrayList;
import java.util.List;

import org.evoludo.simulator.models.ChangeListener;
import org.evoludo.simulator.models.ChangeListener.PendingAction;
import org.evoludo.simulator.models.Mode;
import org.evoludo.simulator.models.RunListener;
import org.evoludo.simulator.modules.Module;
import org.evoludo.util.Formatter;

/**
 * Controller for managing the execution and lifecycle of EvoLudo model
 * simulations.
 * <p>
 * The {@code RunController} handles the runtime behavior of evolutionary
 * models,
 * including starting, stopping, stepping through simulations, and notifying
 * listeners
 * of model state changes. It manages the execution flow, timing, and
 * coordination
 * between the model and its observers.
 * </p>
 * <p>
 * Key responsibilities include:
 * </p>
 * <ul>
 * <li>Managing simulation execution state (running, stopped, suspended)</li>
 * <li>Controlling update delays and timing between simulation steps</li>
 * <li>Handling model initialization, reset, and relaxation</li>
 * <li>Supporting time reversal and debugging capabilities</li>
 * <li>Notifying registered {@link RunListener}s of state changes</li>
 * <li>Processing pending actions and mode changes</li>
 * <li>Collecting CPU performance statistics</li>
 * </ul>
 * <p>
 * The controller operates in conjunction with an {@link EvoLudo} engine
 * instance
 * and supports various execution modes including dynamics simulation,
 * statistics
 * sampling, and step-by-step debugging.
 * </p>
 * 
 * @author Christoph Hauert
 * @see EvoLudo
 * @see RunListener
 * @see PendingAction
 * @see org.evoludo.simulator.models.Model
 */
class RunController {

	/**
	 * Engine whose execution is coordinated by this controller.
	 */
	private final EvoLudo engine;

	/**
	 * Delay between subsequent updates in milliseconds when model is running.
	 */
	protected int delay = (int) EvoLudo.DELAY_INIT;

	/**
	 * Flag to indicate if a backstep is in progress.
	 */
	private boolean doPrev = false;

	/**
	 * Starting time of CPU sampling (negative if not sampling).
	 */
	private double cpu;

	/**
	 * Mean time of CPU sampling.
	 */
	private double cpuMean;

	/**
	 * Variance of time of CPU sampling.
	 */
	private double cpuVar;

	/**
	 * Number of CPU time samples.
	 */
	private int cpuSamples;

	/**
	 * List of listeners that are notified about model run state changes.
	 */
	protected List<RunListener> runListeners = new ArrayList<>();

	/**
	 * Constructs a new RunController for the specified EvoLudo engine.
	 * 
	 * @param engine the EvoLudo engine instance to be controlled by this
	 *               RunController
	 */
	public RunController(EvoLudo engine) {
		this.engine = engine;
	}

	/**
	 * Add a run listener to the list of listeners that get notified about model run
	 * state changes.
	 * 
	 * @param newListener the new run listener
	 * 
	 * @see EvoLudo#addRunListener(RunListener)
	 */
	public void addListener(RunListener newListener) {
		if (!runListeners.contains(newListener))
			runListeners.add(0, newListener);
	}

	/**
	 * Remove the run listener from the list of listeners that get notified when the
	 * model run state changes.
	 * 
	 * @param obsoleteListener the listener to remove from list of run listeners
	 * 
	 * @see EvoLudo#removeRunListener(RunListener)
	 */
	public void removeListener(RunListener obsoleteListener) {
		runListeners.remove(obsoleteListener);
	}

	/**
	 * Called whenever the model starts its calculations. Fires only when starting
	 * to run. Notifies all registered {@link RunListener}s.
	 * 
	 * @see EvoLudo#fireModelRunning()
	 */
	public void fireModelRunning() {
		if (engine.isRunning)
			return;
		engine.isRunning = true;
		engine.isSuspended = false;
		for (RunListener i : runListeners)
			i.modelRunning();
		engine.logger.info("Model running");
	}

	/**
	 * Called whenever the settings of the model have changed. For example, to
	 * trigger the range of values or markers in the GUI. Notifies all registered
	 * {@link RunListener}s.
	 * 
	 * @see EvoLudo#fireSettingsChanged()
	 */
	public void fireSettingsChanged() {
		for (RunListener i : runListeners)
			i.modelSettings();
	}

	/**
	 * Called after the model has been re-initialized. Notifies all registered
	 * {@link RunListener}s.
	 * 
	 * @see EvoLudo#fireModelInit()
	 */
	public void fireModelInit() {
		if (engine.activeModel.getMode() == Mode.DYNAMICS || !engine.isRunning) {
			engine.isRunning = false;
			for (RunListener i : runListeners)
				i.modelDidInit();
			engine.logger.info("Model init");
		}
	}

	/**
	 * Called after the model has been reset. Notifies all registered
	 * {@link RunListener}s.
	 * 
	 * @see EvoLudo#fireModelReset()
	 */
	public void fireModelReset() {
		engine.isRunning = false;
		if (engine.activeModel == null)
			return;
		for (RunListener i : runListeners)
			i.modelDidReset();
		engine.logger.info("Model reset");
	}

	/**
	 * Called after the model completed its relaxation. Notifies all registered
	 * {@link RunListener}s.
	 * 
	 * @see EvoLudo#fireModelRelaxed()
	 */
	public void fireModelRelaxed() {
		for (RunListener i : runListeners)
			i.modelRelaxed();
		engine.logger.info("Model relaxed");
	}

	/**
	 * Called after the population has reached an absorbing state (or has converged
	 * to an equilibrium state). Notifies all registered {@link RunListener}s.
	 * 
	 * @see EvoLudo#fireModelStopped()
	 */
	public void fireModelStopped() {
		if (engine.activeModel == null)
			return;
		engine.isRunning = false;
		for (RunListener i : runListeners)
			i.modelStopped();
		engine.logger.info("Model stopped");
	}

	/**
	 * Start or stop EvoLudo model. If model is running wait until next update is
	 * completed to prevent unexpected side effects.
	 * 
	 * @see EvoLudo#startStop()
	 */
	public void startStop() {
		if (engine.isRunning) {
			requestAction(PendingAction.STOP);
			return;
		}
		if (engine.activeModel.getMode() == Mode.STATISTICS_SAMPLE) {
			engine.fireModelRunning();
			engine.next();
		} else {
			engine.isSuspended = true;
			engine.run();
		}
	}

	/**
	 * Requests halting of a running {@link org.evoludo.simulator.models.Model} on
	 * the next opportunity.
	 * 
	 * @see EvoLudo#stop()
	 */
	public void stop() {
		if (engine.isRunning)
			requestAction(PendingAction.STOP);
	}

	/**
	 * Attempts to backtrack a single step of the EvoLudo model.
	 * 
	 * @see EvoLudo#prev()
	 */
	public void prev() {
		if (!engine.activeModel.permitsTimeReversal() || engine.activeModel.getMode() != Mode.DYNAMICS)
			return;
		engine.activeModel.setTimeReversed(!engine.activeModel.isTimeReversed());
		doPrev = true;
		engine.next();
	}

	/**
	 * Advances the EvoLudo model by a single debugging step.
	 * 
	 * @see EvoLudo#debug()
	 */
	public void debug() {
		if (!engine.activeModel.permitsDebugStep())
			return;
		java.util.logging.Level logLevel = engine.logger.getLevel();
		engine.logger.setLevel(java.util.logging.Level.ALL);
		engine.activeModel.debugStep();
		engine.logger.setLevel(logLevel);
	}

	/**
	 * Requests a {@link PendingAction} to be processed on the next opportunity.
	 * 
	 * @param action the action requested
	 * 
	 * @see EvoLudo#requestAction(PendingAction)
	 */
	public synchronized void requestAction(PendingAction action) {
		requestAction(action, !engine.isRunning);
	}

	/**
	 * Requests a {@link PendingAction} to be processed on the next opportunity.
	 * 
	 * @param action the action requested
	 * @param now    <code>true</code> to processes action immediately
	 * 
	 * @see EvoLudo#requestAction(PendingAction, boolean)
	 */
	public synchronized void requestAction(PendingAction action, boolean now) {
		if (engine.pendingAction != PendingAction.STOP)
			engine.pendingAction = action;
		if (now) {
			processPendingAction();
		}
	}

	/**
	 * Helper method for handling model changed events and processing pending
	 * actions.
	 * 
	 * @see EvoLudo#processPendingAction()
	 */
	void processPendingAction() {
		PendingAction action = engine.pendingAction;
		engine.pendingAction = PendingAction.NONE;
		switch (action) {
			case CHANGE_MODE:
				if (processChangeMode(action))
					break;
				//$FALL-THROUGH$
			case NONE:
				for (ChangeListener i : engine.changeListeners)
					i.modelChanged(action);
				break;
			case STOP:
				engine.fireModelStopped();
				break;
			case INIT:
				modelInit();
				break;
			case RESET:
				modelReset();
				break;
			case SHUTDOWN:
				if (engine.isRunning) {
					engine.isRunning = false;
					engine.pendingAction = PendingAction.STOP;
				}
				engine.unloadModule();
				break;
			default:
		}
	}

	/**
	 * Process change of mode request.
	 * 
	 * @param action the pending action
	 * @return <code>true</code> if mode unchanged
	 */
	boolean processChangeMode(PendingAction action) {
		Mode mode = action.getMode();
		if (engine.activeModel.setMode(mode)) {
			if (mode == Mode.STATISTICS_SAMPLE) {
				modelReset(true);
			} else {
				if (engine.isRunning)
					engine.fireModelStopped();
			}
		} else {
			if (!engine.isRunning || mode == Mode.STATISTICS_SAMPLE)
				return true;
		}
		return false;
	}

	/**
	 * Reset all populations and notify listeners.
	 * 
	 * @see EvoLudo#modelReset()
	 */
	public void modelReset() {
		modelReset(false);
	}

	/**
	 * Reset all populations and optionally notify listeners.
	 * 
	 * @param quiet set to {@code true} to skip notifying listeners
	 * 
	 * @see EvoLudo#modelReset(boolean)
	 */
	public void modelReset(boolean quiet) {
		if (engine.activeModel == null)
			return;
		if (engine.rng.isSeedSet())
			engine.rng.reset();
		engine.modelCheck();
		for (Module<?> mod : engine.activeModule.getSpecies())
			mod.reset();
		engine.activeModel.reset();
		engine.resetRequested = false;
		modelInit(true);
		if (!quiet) {
			engine.activeModel.resetStatisticsSample();
			engine.fireModelReset();
		}
	}

	/**
	 * Initialize all populations and notify listeners.
	 * 
	 * @see EvoLudo#modelInit()
	 */
	public void modelInit() {
		modelInit(false);
	}

	/**
	 * Initialize all populations and optionally notify listeners.
	 * 
	 * @param quiet set to {@code true} to skip notifying listeners
	 * 
	 * @see EvoLudo#modelInit(boolean)
	 */
	public void modelInit(boolean quiet) {
		for (Module<?> mod : engine.activeModule.getSpecies())
			mod.init();
		engine.activeModel.init();
		engine.activeModel.update();
		resetCPUSample();
		modelRelax(quiet);
		if (!quiet) {
			engine.fireModelInit();
		}
	}

	/**
	 * Relax the model and notify listeners.
	 * 
	 * @return <code>true</code> if converged
	 * 
	 * @see EvoLudo#modelRelax()
	 */
	public boolean modelRelax() {
		return modelRelax(false);
	}

	/**
	 * Relax the model and optionally notify listeners.
	 * 
	 * @param quiet set to {@code true} to skip notifying listeners
	 * @return <code>true</code> if converged
	 * 
	 * @see EvoLudo#modelRelax(boolean)
	 */
	public boolean modelRelax(boolean quiet) {
		if (engine.activeModel.getTimeRelax() < 1.0)
			return engine.activeModel.hasConverged();
		boolean converged = engine.activeModel.relax();
		if (!quiet) {
			if (converged)
				engine.fireModelStopped();
			else
				engine.fireModelRelaxed();
		}
		return converged;
	}

	/**
	 * Advance model by one step and notify listeners when done.
	 * 
	 * @return <code>true</code> if the model should continue
	 * 
	 * @see EvoLudo#modelNext()
	 */
	public boolean modelNext() {
		startCPUSample();
		if (engine.activeModel == null)
			return false;
		if (engine.activeModel.useScheduling()) {
			engine.activeModel.next();
			return true;
		}
		return modelNextDone(engine.activeModel.next());
	}

	/**
	 * Called after model next step finished to handle continuation or stop.
	 * 
	 * @param cont <code>false</code> if converged or halting time reached
	 * @return <code>true</code> if the model should continue
	 * 
	 * @see EvoLudo#modelNextDone(boolean)
	 */
	public boolean modelNextDone(boolean cont) {
		if (doPrev) {
			doPrev = false;
			engine.activeModel.setTimeReversed(!engine.activeModel.isTimeReversed());
		}
		endCPUSample();
		if (!cont) {
			engine.fireModelStopped();
			return false;
		}
		engine.fireModelChanged();
		return true;
	}

	/**
	 * Set delay between updates.
	 * 
	 * @param delay in milliseconds
	 * 
	 * @see EvoLudo#setDelay(int)
	 */
	public void setDelay(int delay) {
		this.delay = Math.min(Math.max(delay, (int) EvoLudo.DELAY_MIN), (int) EvoLudo.DELAY_MAX);
	}

	/**
	 * Get current delay between updates.
	 * 
	 * @return delay in milliseconds
	 * 
	 * @see EvoLudo#getDelay()
	 */
	public int getDelay() {
		return delay;
	}

	/**
	 * Increase delay between updates.
	 * 
	 * @see EvoLudo#increaseDelay()
	 */
	public void increaseDelay() {
		int newdelay = delay;
		newdelay = (int) (newdelay * EvoLudo.DELAY_INCR);
		if (newdelay == delay)
			newdelay++;
		setDelay(Math.min(newdelay, (int) EvoLudo.DELAY_MAX));
	}

	/**
	 * Decrease delay between updates.
	 * 
	 * @see EvoLudo#decreaseDelay()
	 */
	public void decreaseDelay() {
		int newdelay = delay;
		newdelay = (int) (newdelay / EvoLudo.DELAY_INCR);
		setDelay(Math.max(newdelay, (int) EvoLudo.DELAY_MIN));
	}

	/**
	 * Reset collected CPU sampling statistics.
	 */
	protected void resetCPUSample() {
		cpu = -1.0;
		cpuMean = cpuVar = 0.0;
		cpuSamples = 0;
	}

	/**
	 * Start a CPU sampling measurement if no sample is active.
	 */
	protected void startCPUSample() {
		if (cpu < 0.0)
			cpu = engine.elapsedTimeMsec();
	}

	/**
	 * Complete a CPU sampling measurement and log aggregated statistics.
	 */
	protected void endCPUSample() {
		if (engine.logger.isLoggable(java.util.logging.Level.FINE)) {
			double time = engine.elapsedTimeMsec() - cpu;
			cpuSamples++;
			double incr = time - cpuMean;
			cpuMean += incr / cpuSamples;
			double incr2 = time - cpuMean;
			cpuVar += incr * incr2;
			cpu = -1.0;
			if (engine.activeModel == null)
				return;
			engine.logger.fine("CPU time: " + time + " @ " + Formatter.format(engine.activeModel.getUpdates(), 3)
					+ ", mean  " + Formatter.formatFix(cpuMean, 2) + " +/- "
					+ Formatter.formatFix(Math.sqrt(cpuVar / (cpuSamples - 1)), 2));
		}
	}
}
