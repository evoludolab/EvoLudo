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

import org.evoludo.EvoLudoWeb;
import org.evoludo.graphics.Network2DGWT;
import org.evoludo.graphics.Network3DGWT;
import org.evoludo.ui.ContextMenu;
import org.evoludo.ui.ContextMenuCheckBoxItem;
import org.evoludo.math.ArrayMath;
import org.evoludo.simulator.models.IBS;
import org.evoludo.simulator.models.Model;
import org.evoludo.simulator.models.PDESupervisor;
import org.evoludo.simulator.models.Model.Mode;
import org.evoludo.util.CLOParser;
import org.evoludo.util.CLOption;
import org.evoludo.util.CLOption.CLODelegate;
import org.evoludo.util.Plist;

import com.google.gwt.core.client.Duration;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.RepeatingCommand;
import com.google.gwt.dom.client.Document;
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
	protected double snapshotAt;

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
		if (snapshotAt < 0.0)
			return;
		if (snapshotAt > 0.0) {
			delay = 1;
			if (activeModel.isMode(Mode.STATISTICS) || snapshotAt > reportInterval) {
				run();
				return;
			}
			// clear isSuspended otherwise the engine starts running
			isSuspended = false;
			setReportInterval(snapshotAt);
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
		if (activeModel.isMode(Mode.STATISTICS)) {
			isRunning = true;
			// MODE_STATISTICS: non-blocking way for running an arbitrary number of update
			// steps to obtain one sample
			scheduleSample();
			if (isRunning && Math.abs(((IBS)activeModel).getNStatisticsSamples() - snapshotAt) < 1.0) {
				// process request at once - if desired, resume execution after
				// snapshot was taken.
				isRunning = false;
				requestAction(PendingAction.SNAPSHOT);
				// stop repeating command
			}
			return;
		}
		if (isRunning)
			return;
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
			if (Math.abs(activeModel.getTime() + reportInterval - snapshotAt) < reportInterval)
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
		if (isRunning)
			return;
		switch (activeModel.getMode()) {
			case STATISTICS:
				// MODE_STATISTICS: non-blocking way for running an arbitrary number of update
				// steps to obtain one sample
				scheduleSample();
				break;
			case DYNAMICS:
				// MODE_DYNAMICS: update single step
				scheduleStep();
				break;
			default:
				throw new Error("next(): unknown mode...");
		}
	}

	private void scheduleSample() {
		Scheduler.get().scheduleIncremental(new RepeatingCommand() {
			@Override
			public boolean execute() {
				// in unfortunate cases even a single sample can take exceedingly long
				// times. stop/init/reset need to be able to interrupt.
				if (!pendingAction.equals(PendingAction.NONE) && !pendingAction.equals(PendingAction.STATISTIC)) {
//XXX this is abusing modelStopped; should be modelInterrupted or something
// stopped indicates convergence; add/rename milestones
					fireModelStopped();
					return false;
				}
				return modelNext();
			}
		});
	}

	private void scheduleStep() {
		Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
			@Override
			public void execute() {
				modelNext();
			}
		});
}

	@Override
	public void modelUnloaded() {
		isRunning = false;
		timer.cancel();
		super.modelUnloaded();
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
	public PDESupervisor hirePDESupervisor(Model.PDE charge) {
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
	 * @see EvoLudoWeb#ePubReaderHasFeature(String)
	 */
	public void detectGUIFeatures() {
		isGWT = true;
		hasTouch = hasTouch();
		isXML = isXML();
		isEPub = (EvoLudoWeb.getEPubReader() != null);
		// IMPORTANT: ibooks (desktop) returns ePubReader for standalone pages as well,
		// i.e. isEPub is true
		// however, ibooks (ios) does not report as an ePubReader for standalone pages,
		// i.e. isEPub is false
		ePubStandalone = (Document.get().getElementById("evoludo-standalone") != null);
		ePubHasKeys = EvoLudoWeb.ePubReaderHasFeature("keyboard-events");
		ePubHasMouse = EvoLudoWeb.ePubReaderHasFeature("mouse-events");
		ePubHasTouch = EvoLudoWeb.ePubReaderHasFeature("touch-events");
	}

	/**
	 * @return <code>true</code> if execution environment supports keyboard events.
	 */
	public final native boolean hasKeys() /*-{
		return true == ("onkeydown" in $wnd);
	}-*/;

	/**
	 * @return <code>true</code> if execution environment supports mouse events.
	 */
	public final native boolean hasMouse() /*-{
		return true == ("onmousedown" in $wnd);
	}-*/;

	/**
	 * @return <code>true</code> if execution environment supports touch events.
	 */
	public final native boolean hasTouch() /*-{
		return true == ("ontouchstart" in $wnd || $wnd.DocumentTouch
				&& $doc instanceof DocumentTouch);
	}-*/;

	/**
	 * Test whether loaded from an XHTML document.
	 * <p>
	 * <strong>Note:</strong> GWT interferes here and both
	 * <code>Document.get().createElement("div").getTagName()=="DIV"</code> as well
	 * as <code>$doc.createElement("div").tagName == "DIV"</code> falsely (always?)
	 * return <code>false</code>.
	 *
	 * @return <code>true</code> if XML document
	 */
	private final native boolean isXML() /*-{
		return (window.document.createElement("div").tagName == "DIV");
	}-*/;

	private ContextMenuCheckBoxItem timeReverseMenu, symDiffMenu;
	protected ContextMenu debugSubmenu;

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
							Model.PDE pde = (Model.PDE) activeModel;
							pde.setSymmetric(!pde.isSymmetric());
							pde.check();
						}
					});
				}
				menu.addSeparator();
				menu.add(symDiffMenu);
				Model.PDE pde = (Model.PDE) activeModel;
				symDiffMenu.setChecked(pde.isSymmetric());
				Geometry space = pde.getGeometry();
				symDiffMenu.setEnabled(space.isRegular || space.isLattice());
				break;
			case IBS:
			default:
		}
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
		EvoLudoWeb._export("data:text/x-plist;base64," + EvoLudoWeb.b64encode(state), "evoludo.plist");
		logger.info("state saved in 'evoludo.plist'.");
	}

	@Override
	public void helpCLO() {
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
			"--gui <f1[:f2[:...]]> list of GUI features (debug only):\n"
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
						ePubHasKeys = hasKeys();
						ePubHasMouse = hasMouse();
						ePubHasTouch = hasTouch();
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
	public final CLOption cloSnap = new CLOption("snap", catGUI,
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
