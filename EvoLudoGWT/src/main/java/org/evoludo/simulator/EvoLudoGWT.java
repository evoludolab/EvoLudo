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

package org.evoludo.simulator;

import org.evoludo.EvoLudoWeb;
import org.evoludo.graphics.Network2DGWT;
import org.evoludo.graphics.Network3DGWT;
import org.evoludo.math.ArrayMath;
import org.evoludo.simulator.models.ChangeListener.PendingAction;
import org.evoludo.simulator.models.Mode;
import org.evoludo.simulator.models.PDE;
import org.evoludo.simulator.models.PDESupervisor;
import org.evoludo.simulator.models.PDESupervisorGWT;
import org.evoludo.simulator.models.Type;
import org.evoludo.simulator.views.AbstractView;
import org.evoludo.ui.ContextMenu;
import org.evoludo.ui.ContextMenuCheckBoxItem;
import org.evoludo.util.CLOParser;
import org.evoludo.util.CLOption;
import org.evoludo.util.CLOption.CLODelegate;
import org.evoludo.util.CLOption.Category;
import org.evoludo.util.NativeJS;

import com.google.gwt.core.client.Duration;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.RepeatingCommand;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.Timer;

/**
 * GWT specific implementation of EvoLudo controller.
 *
 * @author Christoph Hauert
 */
public class EvoLudoGWT extends EvoLudo {

	/**
	 * <code>true</code> if container document is HTML
	 */
	public boolean isHTML = true;

	/**
	 * <code>true</code> if part of an ePub
	 */
	public boolean isEPub = false;

	/**
	 * <code>true</code> if standalone EvoLudo lab in ePub
	 */
	public boolean ePubStandalone = false;

	/**
	 * <code>true</code> if ePub has mouse device
	 */
	public boolean ePubHasMouse = false;

	/**
	 * <code>true</code> if ePub has touch device
	 */
	public boolean ePubHasTouch = false;

	/**
	 * <code>true</code> if ePub has keyboard device
	 */
	public boolean ePubHasKeys = false;

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
		detectGUIFeatures();
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
		Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
			@Override
			public void execute() {
				directive.execute();
			}
		});
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
	 * Called when the GUI has finished loading and the dimensions of all elements are known.
	 */
	public void guiReady() {
		if (notifyGUI == null)
			return;
		notifyGUI.execute();
		notifyGUI = null;
	}

	@Override
	public void layoutComplete() {
		if (cloSnap.isSet()) {
			// --snap set
			double tStop = activeModel.getTimeStop();
			double nSamples = activeModel.getNSamples();
			switch (activeModel.getMode()) {
				case DYNAMICS:
				case STATISTICS_UPDATE:
					if (tStop > 0.0) {
						// run to specified time
						if (tStop < activeModel.getTimeStep())
							activeModel.setTimeStep(tStop);
						// start running - even without --run
						setSuspended(true);
					} else {
						// no stopping time requested: take snapshot now
						gui.snapshotReady();
						// don't start running - even if --run provided
						setSuspended(false);
					}
					if (nSamples > 0.0)
						logger.warning("--samples found: wrong mode for statistics, use --view option.");
					break;
				case STATISTICS_SAMPLE:
					// run to specified sample count
					if (nSamples > 0.0) {
						// start running - even without --run
						setSuspended(true);
					} else {
						// no sample count requested: take snapshot now
						gui.snapshotReady();
						// don't start running - even if --run provided
						setSuspended(false);
					}
					if (Double.isFinite(tStop))
						logger.warning("--timestop found: wrong mode for dynamics, use --view option.");
				default:
			}
		}
		super.layoutComplete();
	}

	@Override
	public void run() {
		// ignore if already running or not suspended
		if (isRunning || !isSuspended())
			return;
		fireModelRunning();
		// start with an update not the delay
		if (modelNext())
			timer.scheduleRepeating(delay);
	}

	/**
	 * Timer for running models.
	 */
	Timer timer = new Timer() {
		boolean processing = false;

		@Override
		public void cancel() {
			processing = false;
			super.cancel();
		}

		@Override
		public void run() {
			if (!processing) {
				processing = true;
				next();
				processing = false;
			}
		}
	};

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
				// schedule single step
				scheduleStep();
				break;
			default:
				throw new Error("next(): unknown mode...");
		}
	}

	/**
	 * Schedule the next sample.
	 */
	private void scheduleSample() {
		Scheduler.get().scheduleIncremental(new RepeatingCommand() {
			@Override
			public boolean execute() {
				// in unfortunate cases even a single sample can take exceedingly long
				// times. stop/init/reset need to be able to interrupt.
				if (pendingAction != PendingAction.NONE) {
					processPendingAction();
					return false;
				}
				if (activeModel.next())
					return true;
				boolean failed = (activeModel.getFixationData().mutantNode < 0);
				fireModelSample(!failed);
				return failed;
			}
		});
	}

	/**
	 * Schedule the next step.
	 */
	private void scheduleStep() {
		Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
			@Override
			public void execute() {
				modelNext();
			}
		});
	}

	@Override
	public synchronized void fireModuleUnloaded() {
		isRunning = false;
		timer.cancel();
		super.fireModuleUnloaded();
	}

	@Override
	public synchronized void fireModelStopped() {
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

	@Override
	void processPendingAction() {
		boolean updateGUI = (pendingAction == PendingAction.STOP 
				&& activeModel.getMode() == Mode.STATISTICS_SAMPLE);
		super.processPendingAction();
		if (updateGUI)
			gui.modelStopped();
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
		if (cloarray == null)
			return new String[] { cloHelp.getName() };
		// check and remove --export option
		String exportName = "export";
		int nParams = cloarray.length;
		for (int i = 0; i < nParams; i++) {
			String param = cloarray[i];
			if (param.startsWith(exportName)) {
				// see --export option in EvoLudoJRE.java
				// remove --export option and file name
				cloarray = ArrayMath.drop(cloarray, i--);
				nParams--;
				continue;
			}
		}
		return cloarray;
	}

	@Override
	public int elapsedTimeMsec() {
		return elapsedTime.elapsedMillis();
	}

	@Override
	public PDESupervisor hirePDESupervisor(PDE charge) {
		return new PDESupervisorGWT(this, charge);
	}

	@Override
	public Network2D createNetwork2D(Geometry geometry) {
		return new Network2DGWT(this, geometry);
	}

	@Override
	public Network3D createNetwork3D(Geometry geometry) {
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
	 * Use JSNI helper methods to query and detect features of the execution
	 * environment.
	 *
	 * @see NativeJS#ePubReaderHasFeature(String)
	 */
	public void detectGUIFeatures() {
		isGWT = true;
		hasTouch = NativeJS.hasTouch();
		isHTML = NativeJS.isHTML();
		isEPub = (NativeJS.getEPubReader() != null);
		// IMPORTANT: ibooks (desktop) returns ePubReader for standalone pages as well,
		// i.e. isEPub is true
		// however, ibooks (ios) does not report as an ePubReader for standalone pages,
		// i.e. isEPub is false
		ePubStandalone = (Document.get().getElementById("evoludo-standalone") != null);
		ePubHasKeys = NativeJS.ePubReaderHasFeature("keyboard-events");
		ePubHasMouse = NativeJS.ePubReaderHasFeature("mouse-events");
		ePubHasTouch = NativeJS.ePubReaderHasFeature("touch-events");
	}

	/**
	 * The context menu item to reverse time.
	 */
	private ContextMenuCheckBoxItem timeReverseMenu;

	/**
	 * The context menu item for symmetrical diffusion (only applies to PDE models).
	 */
	private ContextMenuCheckBoxItem symDiffMenu;

	/**
	 * The field to store the fullscreen context menu.
	 */
	protected ContextMenuCheckBoxItem fullscreenMenu;

	/**
	 * Opportunity to contribute entries to the context menu for models. this needs
	 * to be quarantined in order to not interfere with java simulations.
	 *
	 * @param menu the context menu where entries can be added
	 */
	public void populateContextMenu(ContextMenu menu) {
		Type mt = activeModel.getType();
		if ( mt.isODE() || mt.isSDE() ) {
			// add time reverse context menu
			if (timeReverseMenu == null) {
				timeReverseMenu = new ContextMenuCheckBoxItem("Time reversed", new Command() {
					@Override
					public void execute() {
						activeModel.setTimeReversed(!activeModel.isTimeReversed());
					}
				});
			}
			menu.addSeparator();
			menu.add(timeReverseMenu);
			timeReverseMenu.setChecked(activeModel.isTimeReversed());
			timeReverseMenu.setEnabled(activeModel.permitsTimeReversal());
		} else if (mt.isPDE()) {
			// add context menu to allow symmetric diffusion
			if (symDiffMenu == null) {
				symDiffMenu = new ContextMenuCheckBoxItem("Symmetric diffusion", new Command() {
					@Override
					public void execute() {
						PDE pde = (PDE) activeModel;
						pde.setSymmetric(!pde.isSymmetric());
						pde.check();
					}
				});
			}
			menu.addSeparator();
			menu.add(symDiffMenu);
			PDE pde = (PDE) activeModel;
			symDiffMenu.setChecked(pde.isSymmetric());
			Geometry space = pde.getGeometry();
			symDiffMenu.setEnabled(space.isRegular || space.isLattice());
		}
		// process fullscreen context menu
		if (fullscreenMenu == null && NativeJS.isFullscreenSupported()) {
			fullscreenMenu = new ContextMenuCheckBoxItem("Full screen", new Command() {
				@Override
				public void execute() {
					setFullscreen(!NativeJS.isFullscreen());
				}
			});
		}
		if (NativeJS.isFullscreenSupported()) {
			menu.addSeparator();
			menu.add(fullscreenMenu);
			fullscreenMenu.setChecked(NativeJS.isFullscreen());
		}
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
	 * Command line option to mimic ePub modes and to disable device capabilities.
	 * <p>
	 * <strong>Note:</strong> for development/debugging only; should be disabled in
	 * production
	 */
	public final CLOption cloEmulate = new CLOption("emulate", "auto", Category.GUI,
			"--emulate <f1[,f2[...]]> list of GUI features to emulate:\n"
					+ "          epub: enable ePub mode\n"
					+ "    standalone: standalone ePub mode\n"
					+ "        nokeys: disable key events (if available)\n"
					+ "       nomouse: disable mouse events (if available)\n"
					+ "       notouch: disable touch events (if available)",
			new CLODelegate() {
				@Override
				public boolean parse(String arg) {
					// set/reset defaults
					detectGUIFeatures();
					if (!cloEmulate.isSet())
						return true;
					// simulate ePub mode
					if (arg.contains("epub"))
						isEPub = true;
					// simulate standalone lab in ePub
					if (arg.contains("standalone")) {
						ePubStandalone = true;
						isEPub = true;
						ePubHasKeys = NativeJS.hasKeys();
						ePubHasMouse = NativeJS.hasMouse();
						ePubHasTouch = NativeJS.hasTouch();
					}
					// disable keys (if available)
					if (ePubHasKeys && arg.contains("nokeys"))
						ePubHasKeys = false;
					// disable mouse (if available)
					if (ePubHasMouse && arg.contains("nomouse"))
						ePubHasMouse = false;
					// disable touch (if available)
					if (ePubHasTouch && arg.contains("notouch"))
						ePubHasTouch = false;
					return true;
				}
			});

	/**
	 * Command line option to request that the EvoLudo model signals the completion
	 * of of the layouting procedure for taking snapshots, e.g. with
	 * <code>capture-website</code>.
	 */
	public final CLOption cloSnap = new CLOption("snap", "20", CLOption.Argument.OPTIONAL, Category.GUI,
			"--snap [<s>]    snapshot utility, timeout <s> secs;\n"
					+ "				(add '<div id=\"snapshot-ready\"></div>' to <body>\n"
					+ "				when ready for snapshot, see capture-website docs)\n",
			new CLODelegate() {
				@Override
				public boolean parse(String arg) {
					snapLayoutTimeout = 20000;
					if (!cloSnap.isSet() || arg.isEmpty())
						return true;
					snapLayoutTimeout = Math.max(1, CLOParser.parseInteger(arg)) * 1000;
					return true;
				}
			});

	@Override
	public void collectCLO(CLOParser prsr) {
		parser.addCLO(cloEmulate);
		parser.addCLO(cloSnap);
		super.collectCLO(prsr);
	}
}
