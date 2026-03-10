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

import org.evoludo.EvoLudoWeb;
import org.evoludo.graphics.Network2DGWT;
import org.evoludo.graphics.Network3DGWT;
import org.evoludo.math.ArrayMath;
import org.evoludo.simulator.geometries.AbstractGeometry;
import org.evoludo.simulator.models.ChangeListener.PendingAction;
import org.evoludo.simulator.models.IBS;
import org.evoludo.simulator.models.Model;
import org.evoludo.simulator.models.ODE;
import org.evoludo.simulator.models.PDE;
import org.evoludo.simulator.views.AbstractView;
import org.evoludo.util.CLOCategory;
import org.evoludo.util.CLODelegate;
import org.evoludo.util.CLOParser;
import org.evoludo.util.CLOption;
import org.evoludo.util.NativeJS;

import com.google.gwt.core.client.Duration;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.Timer;

/**
 * GWT specific implementation of EvoLudo controller.
 *
 * @author Christoph Hauert
 */
public class EvoLudoGWT extends EvoLudo {

	/**
	 * Target number of microscopic IBS updates processed in one hidden GWT chunk.
	 */
	private static final int IBS_CHUNK_TARGET = 10000;

	/**
	 * Target number of ODE/SDE integration increments processed in one hidden GWT
	 * chunk.
	 */
	private static final int OSDE_CHUNK_TARGET = 100;

	/**
	 * Tolerance for floating point comparisons of update milestones.
	 */
	private static final double IBS_EPS = 1e-8;

	/**
	 * Create timer to measure execution times since instantiation.
	 */
	private final Duration elapsedTime = new Duration();

	/**
	 * The reference to the GUI.
	 */
	EvoLudoWeb gui;

	/**
	 * Construct EvoLudo controller for GWT applications (web or ePub).
	 * 
	 * @param gui the reference to the GUI
	 */
	public EvoLudoGWT(EvoLudoWeb gui) {
		this.gui = gui;
		isGWT = true;
	}

	/**
	 * GWT uses the config channel of the logger to report progress
	 */
	@Override
	public void logProgress(String msg) {
		logger.config(msg);
	}

	@Override
	public void execute(Directive directive) {
		Scheduler.get().scheduleDeferred(directive::execute);
	}

	/**
	 * The field to store the command to execute after parsing the command line
	 * options.
	 */
	Directive notifyGUI;

	/**
	 * Parse command line options and set the {@code command} to execute after
	 * parsing completed.
	 * 
	 * @param command the command to execute after parsing
	 * @return the number of issues that have occurred durin parsing
	 */
	public int parseCLO(Directive command) {
		notifyGUI = command;
		return parseCLO();
	}

	@Override
	public void showHelp() {
		gui.showHelp();
		// if module not ready, exit startup
		if (activeModule == null)
			notifyGUI = null;
	}

	/**
	 * Called when the GUI has finished loading and the dimensions of all elements
	 * are known.
	 */
	public void guiReady() {
		if (notifyGUI == null)
			return;
		notifyGUI.execute();
		notifyGUI = null;
	}

	@Override
	public void layoutComplete() {
		if (cloSnap.isSet() && !isSuspended()) {
			// no stopping time requested: take snapshot now
			gui.snapshotReady();
		}
		if (isSuspended()) {
			AbstractView<?> activeView = gui.getActiveView();
			if (activeView != null)
				activeView.update(true);
			Scheduler.get().scheduleDeferred(this::run);
		}
		super.layoutComplete();
	}

	@Override
	public void run() {
		// ignore if already running or not suspended
		if (isRunning || !isSuspended())
			return;
		fireModelRunning();
		switch (activeModel.getMode()) {
			case STATISTICS_SAMPLE:
				// non-blocking way for running an arbitrary number of update
				// steps to obtain one sample
				scheduleSample();
				break;
			case STATISTICS_UPDATE:
			case DYNAMICS:
				// start with an update not the delay
				double chunkStep = getChunkStep(activeModel);
				if (chunkStep > 0.0) {
					scheduleChunkedStep(chunkStep);
					timer.scheduleRepeating(getDelay());
					break;
				}
				if (modelNext())
					timer.scheduleRepeating(getDelay());
				break;
		}
	}

	/**
	 * Timer for running models.
	 */
	Timer timer = new Timer() {
		@Override
		public void run() {
			next();
		}
	};

	/**
	 * <code>true</code> while hidden chunk updates are scheduled or in progress.
	 */
	private boolean chunkedStepScheduled = false;

	/**
	 * The original report interval while scheduled, chunked updates are in
	 * progress.
	 */
	private double chunkTimeStepOrig = 0.0;

	/**
	 * The update milestone at which the current visible step is complete.
	 */
	private double chunkTargetUpdates = 0.0;

	@Override
	public void next() {
		switch (activeModel.getMode()) {
			case STATISTICS_SAMPLE:
				// non-blocking way for running an arbitrary number of update
				// steps to obtain one sample
				scheduleSample();
				break;
			case STATISTICS_UPDATE:
			case DYNAMICS:
				double chunkStep = getChunkStep(activeModel);
				if (chunkStep > 0.0) {
					scheduleChunkedStep(chunkStep);
					break;
				}
				scheduleStep();
				break;
			default:
				throw new UnsupportedOperationException("Unknown mode: " + activeModel.getMode());
		}
	}

	/**
	 * Schedule the next sample.
	 */
	private void scheduleSample() {
		Scheduler.get().scheduleIncremental(() -> {
			// in unfortunate cases even a single sample can take exceedingly long
			// times. stop/init/reset need to be able to interrupt.
			// make sure active model has not been unloaded in the meantime
			if (activeModel == null || pendingAction != PendingAction.NONE) {
				processPendingAction();
				return false;
			}
			if (activeModel.next(activeModel.getTimeStep()))
				return true; // continue sampling
			// sample completed
			boolean failed = (activeModel.getFixationData().mutantNode < 0);
			// continue if running multiple samples or sampling failed
			return fireModelSample(!failed) || failed;
		});
	}

	/**
	 * Schedule the next step.
	 */
	private void scheduleStep() {
		Scheduler.get().scheduleDeferred(() -> {
			if (pendingAction != PendingAction.NONE) {
				processPendingAction();
				return;
			}
			modelNext();
		});
	}

	/**
	 * Determine the hidden chunk step for <code>model</code>.
	 * 
	 * @param model the model to inspect
	 * @return the hidden chunk step in generations, or <code>0</code> if no
	 *         chunking should be used
	 */
	private static double getChunkStep(Model model) {
		if (model instanceof IBS && model.getTimeStep() > 0.0) {
			int nTotal = 0;
			for (int id = 0; id < model.getNSpecies(); id++)
				nTotal += model.getSpecies(id).getNPopulation();
			if (nTotal <= 0)
				return model.getTimeStep();
			return IBS_CHUNK_TARGET / (double) nTotal;
		}
		if (model instanceof PDE) {
			double dt = ((PDE) model).getDt();
			if (dt > 0.0)
				return dt;
		}
		if (model instanceof ODE && !(model instanceof PDE)) {
			double dt = ((ODE) model).getDt();
			if (dt > 0.0) {
				double chunkStep = OSDE_CHUNK_TARGET * dt;
				if (model.getTimeStep() > chunkStep + IBS_EPS)
					return chunkStep;
			}
		}
		return 0.0;
	}

	/**
	 * Schedule a visible report step as a sequence of hidden substeps.
	 */
	private void scheduleChunkedStep(double chunkStep) {
		if (chunkedStepScheduled || activeModel == null)
			return;
		chunkedStepScheduled = true;
		chunkTimeStepOrig = activeModel.getTimeStep();
		chunkTargetUpdates = getNextChunkedReport();
		runController.startCPUSample();
		Scheduler.get().scheduleIncremental(() -> {
			if (!chunkedStepScheduled)
				return false;
			if (activeModel == null) {
				resetChunking();
				return false;
			}
			if (pendingAction != PendingAction.NONE) {
				resetChunking();
				processPendingAction();
				return false;
			}
			double remaining = chunkTargetUpdates - activeModel.getUpdates();
				if (remaining <= IBS_EPS) {
					finishChunkedStep(activeModel.getUpdates() + IBS_EPS < activeModel.getNextHalt());
					return false;
				}
				double chunk = Math.min(chunkStep, remaining);
				boolean cont = activeModel.next(chunk);
				double updates = activeModel.getUpdates();
			boolean reachedTarget = (updates + IBS_EPS >= chunkTargetUpdates);
			if (!cont) {
				finishChunkedStep(false);
				return false;
			}
			if (reachedTarget) {
				finishChunkedStep(activeModel.getUpdates() + IBS_EPS < activeModel.getNextHalt());
				return false;
			}
			return true;
		});
	}

	/**
	 * Calculate the next regular report boundary for the active chunked model.
	 * 
	 * @return the update milestone for the next visible step
	 */
	private double getNextChunkedReport() {
		double updates = activeModel.getUpdates();
		double nextHalt = activeModel.getNextHalt();
		double nextReport = (Math.floor((updates + IBS_EPS) / chunkTimeStepOrig) + 1.0) * chunkTimeStepOrig;
		return Math.min(nextReport, nextHalt);
	}

	/**
	 * Complete the current hidden chunked step and forward the visible-step
	 * result.
	 * 
	 * @param cont <code>true</code> if the model should continue after this step
	 */
	private void finishChunkedStep(boolean cont) {
		resetChunking();
		modelNextDone(cont);
	}

	/**
	 * Reset temporary state used for hidden chunking.
	 */
	private void resetChunking() {
		chunkedStepScheduled = false;
		chunkTimeStepOrig = 0.0;
		chunkTargetUpdates = 0.0;
	}

	@Override
	public synchronized void fireModuleUnloaded() {
		resetChunking();
		isRunning = false;
		timer.cancel();
		super.fireModuleUnloaded();
	}

	@Override
	public synchronized void fireModelStopped() {
		resetChunking();
		// model may already have been unloaded
		if (activeModel == null)
			return;
		super.fireModelStopped();
		timer.cancel();
		if (cloSnap.isSet()) {
			// take snapshot
			gui.snapshotReady();
		}
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * The command line arguments stored in a typical {@code .plist} file -- in
	 * particular when generated by the {@code --export} option -- includes this
	 * very option. Since JavaScript (GWT) contracts do not permit access to the
	 * users file system without explicit user interaction the {@code --export} does
	 * not make sense. However, it would still be useful to be able to restore the
	 * state of such a file in the browser through drag'n'drop. Here we simply check
	 * if {@code --export} was provided on the command line and discard it if found.
	 * 
	 * @see org.evoludo.simulator.EvoLudoJRE#cloExport
	 */
	@Override
	protected String[] preprocessCLO(String[] cloarray) {
		// once module is loaded pre-processing of command line arguments can proceed
		cloarray = super.preprocessCLO(cloarray);
		// check and remove --export option
		String exportName = "export";
		for (int i = 0; i < cloarray.length; i++) {
			String param = cloarray[i];
			if (param.startsWith(exportName)) {
				// see --export option in EvoLudoJRE.java
				// remove --export option and file name
				cloarray = ArrayMath.drop(cloarray, i);
				break;
			}
		}
		return cloarray;
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * GWT implementation for measuring execution time.
	 * 
	 * @see org.evoludo.simulator.EvoLudoGWT#elapsedTimeMsec
	 *      EvoLudoGWT.elapsedTimeMsec
	 * @see org.evoludo.simulator.EvoLudoJRE#elapsedTimeMsec
	 *      EvoLudoJRE.elapsedTimeMsec
	 */
	@Override
	public int elapsedTimeMsec() {
		return elapsedTime.elapsedMillis();
	}

	@Override
	public Network2D createNetwork2D(AbstractGeometry geometry) {
		return new Network2DGWT(this, geometry);
	}

	@Override
	public Network3D createNetwork3D(AbstractGeometry geometry) {
		return new Network3DGWT(this, geometry);
	}

	@Override
	public String getGit() {
		return Git.INSTANCE.gitVersion();
	}

	@Override
	public String getGitDate() {
		return Git.INSTANCE.gitDate();
	}

	@Override
	public void setDelay(int delay) {
		super.setDelay(delay);
		if (isRunning)
			timer.scheduleRepeating(delay);
	}

	/**
	 * The reference to the fullscreen element.
	 */
	Element fullscreenElement;

	/**
	 * Set the fullscreen element.
	 * 
	 * @param element the element to set as fullscreen
	 */
	public void setFullscreenElement(Element element) {
		fullscreenElement = element;
	}

	/**
	 * Enter or exit fullscreen mode.
	 * 
	 * @param fullscreen {@code true} to enter fullscreen
	 */
	public void setFullscreen(boolean fullscreen) {
		if (fullscreen == NativeJS.isFullscreen())
			return;
		if (fullscreen)
			// note: seems a little weird to get grandparents involved...
			NativeJS.requestFullscreen(fullscreenElement);
		else
			NativeJS.exitFullscreen();
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * Called by <code>Restore...</code> context menu in {@link AbstractView}.
	 */
	@Override
	public boolean restoreFromFile() {
		NativeJS.restoreFromFile(gui);
		// how to return success/failure from JSNI?
		return true;
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * <strong>Note:</strong> nudges web browser to download the current state and
	 * save it in a file named "evoludo.plist". Export (context menu) suppressed in
	 * ePubs.
	 */
	@Override
	public void exportState() {
		String state = encodeState();
		if (state == null)
			return;
		NativeJS.export("data:text/x-plist;base64," + NativeJS.b64encode(state), "evoludo.plist");
		logger.info("state saved in 'evoludo.plist'.");
	}

	/**
	 * Command line option to request that the EvoLudo model signals the completion
	 * of of the layouting procedure for taking snapshots, e.g. with
	 * <code>capture-website</code>.
	 * 
	 * @see <a href="https://github.com/sindresorhus/capture-website-cli"> Github:
	 *      capture-website-cli</a>
	 */
	public final CLOption cloSnap = new CLOption("snap", "20", CLOption.Argument.OPTIONAL, CLOCategory.GUI,
			"--snap [<s>]    snapshot utility, timeout <s> secs;\n"
					+ "                (add '<div id=\"snapshot-ready\"></div>' to <body>\n"
					+ "                when ready for snapshot, see capture-website docs)",
			new CLODelegate() {
				@Override
				public boolean parse(String arg, boolean isSet) {
					if (isSet)
						snapLayoutTimeout = Math.max(1, CLOParser.parseInteger(arg)) * 1000;
					return true;
				}
			});

	@Override
	public void collectCLO(CLOParser prsr) {
		prsr.addCLO(cloSnap);
		super.collectCLO(prsr);
	}
}
