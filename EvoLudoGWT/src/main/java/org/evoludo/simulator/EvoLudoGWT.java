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

package org.evoludo.simulator;

import org.evoludo.graphics.Network2DGWT;
import org.evoludo.graphics.Network3DGWT;
import org.evoludo.math.ArrayMath;
import org.evoludo.simulator.models.ChangeListener.PendingAction;
import org.evoludo.simulator.models.Mode;
import org.evoludo.simulator.models.PDERD;
import org.evoludo.simulator.models.PDESupervisor;
import org.evoludo.simulator.models.PDESupervisorGWT;
import org.evoludo.simulator.modules.Module;
import org.evoludo.simulator.views.AbstractView;
import org.evoludo.ui.ContextMenu;
import org.evoludo.ui.ContextMenuCheckBoxItem;
import org.evoludo.util.CLOParser;
import org.evoludo.util.CLOption;
import org.evoludo.util.CLOption.CLODelegate;
import org.evoludo.util.NativeJS;
import org.evoludo.util.Plist;
import org.evoludo.util.PlistParser;

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
	 * <code>true</code> if container document is XHTML
	 */
	public boolean isXML = false;

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
	 * generation at which to request snapshot
	 */
	protected double snapshotAt = -Double.MAX_VALUE;

	/**
	 * Create timer to measure execution times since instantiation.
	 */
	private final Duration elapsedTime = new Duration();

	/**
	 * Construct EvoLudo controller for GWT applications (web or ePub).
	 */
	public EvoLudoGWT() {
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

	@Override
	public void layoutComplete() {
		if (snapshotAt < 0.0) {
			super.layoutComplete();
			return;
		}
		if (snapshotAt > 0.0) {
			delay = 1;
			if (activeModel.getMode() == Mode.STATISTICS_SAMPLE || snapshotAt > activeModel.getTimeStep()) {
				run();
				return;
			}
			// clear isSuspended otherwise the engine starts running
			isSuspended = false;
			activeModel.setTimeStep(snapshotAt);
			next();
			// pretend to be running, otherwise the snapshot request
			// will be honoured before the engine had a chance to process
			// the deferred step.
			isRunning = true;
		}
		requestAction(PendingAction.SNAPSHOT);
	}

	@Override
	public void run() {
		isSuspended = false;
		// start with an update not the delay
		next();
		isRunning = true;
		timer.scheduleRepeating(delay);
	}

	/**
	 * Timer for running models.
	 */
	Timer timer = new Timer() {
		@Override
		public void run() {
			double timeStep = activeModel.getTimeStep();
			if (Math.abs(activeModel.getTime() + timeStep - snapshotAt) < timeStep)
				requestAction(PendingAction.SNAPSHOT);
			if (isRunning && !modelNext() && snapshotAt > activeModel.getTime())
				// population absorbed before time for snapshot - do it now
				requestAction(PendingAction.SNAPSHOT);
			if (!isRunning)
				timer.cancel();
		}
	};

	@Override
	public void next() {
		switch (activeModel.getMode()) {
			case STATISTICS_SAMPLE:
				// MODE_STATISTICS: non-blocking way for running an arbitrary number of update
				// steps to obtain one sample
				scheduleSample();
				int samples = activeModel.getNStatisticsSamples();
				if (isRunning && Math.abs(samples - snapshotAt) < 1.0) {
					// process request at once - if desired, resume execution after
					// snapshot was taken.
					isRunning = false;
					requestAction(PendingAction.SNAPSHOT);
					// stop repeating command
				}
				if ((samples + 1) == activeModel.getNSamples()) {
					isRunning = false;
					requestAction(PendingAction.STOP);
				}
				break;
			case STATISTICS_UPDATE:
			case DYNAMICS:
				// MODE_DYNAMICS: update single step
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
				switch (pendingAction) {
					case NONE:
					case STATISTIC:
					case STOP: // finish sample
						break;
					default:
					case APPLY:
					case CLO:
					case INIT:
					case RESET:
					case SNAPSHOT:
					case UNLOAD:
						fireModelStopped();
						return false;
				}
				return modelNext();
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
	public void moduleUnloaded() {
		isRunning = false;
		timer.cancel();
		super.moduleUnloaded();
	}

	@Override
	public boolean restoreState(Plist plist) {
		setCLO((String) (plist.get("CLO")));
		requestAction(PendingAction.APPLY);
		return super.restoreState(plist);
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
			return null;
		// check if --export requested
		String exportName = "export";
		int nParams = cloarray.length;
		for (int i = 0; i < nParams; i++) {
			String param = cloarray[i];
			if (!doRestore && param.startsWith(exportName)) {
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
	public PDESupervisor hirePDESupervisor(PDERD charge) {
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
		isXML = NativeJS.isXML();
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
		switch (activeModel.getModelType()) {
			case ODE:
			case SDE:
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
				return;
			case PDE:
				// add context menu to allow symmetric diffusion
				if (symDiffMenu == null) {
					symDiffMenu = new ContextMenuCheckBoxItem("Symmetric diffusion", new Command() {
						@Override
						public void execute() {
							PDERD pde = (PDERD) activeModel;
							pde.setSymmetric(!pde.isSymmetric());
							pde.check();
						}
					});
				}
				menu.addSeparator();
				menu.add(symDiffMenu);
				PDERD pde = (PDERD) activeModel;
				symDiffMenu.setChecked(pde.isSymmetric());
				Geometry space = pde.getGeometry();
				symDiffMenu.setEnabled(space.isRegular || space.isLattice());
				break;
			case IBS:
			default:
		}
		// process fullscreen context menu
		if (fullscreenMenu == null && NativeJS.isFullscreenSupported()) {
			fullscreenMenu = new ContextMenuCheckBoxItem("Full screen (β)", new Command() {
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
		restoreFromFile(this);
		// how to return success/failure from JSNI?
		return true;
	}

	/**
	 * JSNI method: opens javascript file chooser and attempts to restore state from
	 * selected file
	 *
	 * @param evoludo model that processes contents of selected file
	 */
	private final native void restoreFromFile(EvoLudoGWT evoludo) /*-{
		var input = $doc.createElement('input');
		input.setAttribute('type', 'file');
		input.onchange = function(e) {
			var files = e.target.files;
			if (files.length != 1)
				return;
			var file = files[0];
			var reader = new FileReader();
			reader.onload = function(e) {
				evoludo.@org.evoludo.simulator.EvoLudoGWT::restoreFromFile(Ljava/lang/String;Ljava/lang/String;)(file.name, e.target.result);
			}
			reader.readAsText(file);
		}
		input.click();
	}-*/;

	/**
	 * Restore state of EvoLudo model from String <code>content</code>.
	 *
	 * @param filename (only for reference and reporting of success or failure)
	 * @param content  encoded state of EvoLudo model
	 * @return <code>true</code> if state was successfully restored
	 */
	public boolean restoreFromFile(String filename, String content) {
		Plist parsed = PlistParser.parse(content);
		if (parsed == null || !restoreState(parsed)) {
			logger.severe("failed to parse contents of file '" + filename + "'.");
			return false;
		}
		logger.info("State stored in '" + filename + "' successfully restored.");
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

	@Override
	public void helpCLO() {
		// list trait indices and names
		String msg = "";
		int idx = 0;
		for (Module mod : activeModule.getSpecies()) {
			String name = mod.getName();
			int namelen = name.length();
			if (namelen > 0)
				msg += "\n       Species: " + name;
			int nt = mod.getNTraits();
			for (int n = 0; n < nt; n++)
				msg += "\n             " + (idx + n) + ": " + mod.getTraitName(n);
			idx += nt;
		}
		catModule.setHeader("Module specific options with trait names and indices:" + msg);
		logger.info("<pre>EvoLudoWeb\nList of command line options for module '" + activeModule.getKey() + "':\n"
				+ parser.helpCLO(true) + "</pre>");
	}

	/**
	 * Command line option to mimic ePub modes and to disable device capabilities.
	 * <p>
	 * <strong>Note:</strong> for development/debugging only; should be disabled in
	 * production
	 */
	public final CLOption cloGUIFeatures = new CLOption("gui", "auto", catGUI,
			"--gui <f1[,f2[...]]> list of GUI features (debug only):\n"
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
					if (!cloGUIFeatures.isSet())
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
	public final CLOption cloSnap = new CLOption("snap", "", CLOption.Argument.OPTIONAL, catGUI,
			"--snap [<s>[,<n>]]  snapshot utility (see capture-website)\n"
					+ "      (add '<div id=\"snapshot-ready\"></div>' to <body> upon\n"
					+ "       completion of layout or after max <s> sec and after\n"
					+ "       <n> samples or generations, respectively.)",
			new CLODelegate() {
				@Override
				public boolean parse(String arg) {
					if (!cloSnap.isSet()) {
						snapshotAt = -Double.MAX_VALUE;
						return true;
					}
					snapshotAt = 0.0;
					if (arg.isEmpty())
						return true;
					int[] args = CLOParser.parseIntVector(arg);
					switch (args.length) {
						case 0:
							logger.warning("snap arguments '" + arg + "' unknown - ignored.");
							return false;
						default:
							logger.warning("too many arguments to snap ('" + arg + "') - ignored.");
							return false;
						case 2:
							snapshotAt = args[1];
							if (snapshotAt > 0.0)
								isSuspended = true;
							//$FALL-THROUGH$
						case 1:
							snapLayoutTimeout = args[0] * 1000;
							return true;
					}
				}
			});

	@Override
	public void collectCLO(CLOParser prsr) {
		parser.addCLO(cloGUIFeatures);
		parser.addCLO(cloSnap);
		super.collectCLO(prsr);
	}
}
