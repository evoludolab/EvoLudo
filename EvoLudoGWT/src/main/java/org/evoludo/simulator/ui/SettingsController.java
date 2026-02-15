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

package org.evoludo.simulator.ui;

import org.evoludo.EvoLudoWeb;
import org.evoludo.util.CLOCategory;
import org.evoludo.util.CLODelegate;
import org.evoludo.util.CLOption;
import org.evoludo.util.NativeJS;

import com.google.gwt.dom.client.Document;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Label;

/**
 * Controller responsible for tracking ePub specific settings and emulation
 * flags. It centralizes detection of device capabilities inside an ePub
 * reader, exposes the {@code --emulate} CLO option used for debugging, and
 * keeps a single running lab when resources are constrained.
 */
public class SettingsController {

	/**
	 * Shared settings describing the current ePub environment.
	 */
	private static final class Settings {

		/**
		 * Create a new settings record with default capability flags.
		 */
		private Settings() {
		}

		/** {@code true} if the lab runs in an ePub reader. */
		boolean isEPub;

		/** {@code true} if the lab runs on a standalone ePub page. */
		boolean standalone;

		/** {@code true} if the reader exposes mouse events. */
		boolean hasMouse;

		/** {@code true} if the reader exposes touch events. */
		boolean hasTouch;

		/** {@code true} if the reader exposes keyboard events. */
		boolean hasKeys;
	}

	/**
	 * Label of button to run in standalone mode.
	 */
	public static final String BUTTON_STANDALONE = "Standalone";

	/**
	 * Label of button to apply parameter settings.
	 */
	public static final String BUTTON_APPLY = "Apply";

	/**
	 * Global settings describing the capabilities of the surrounding ePub
	 * environment.
	 */
	private static final Settings SETTINGS = new Settings();

	/**
	 * Reference to the currently running lab in ePub mode. Only a single lab may
	 * run at a time to avoid exhausting reader resources.
	 */
	private static EvoLudoWeb runningLab;

	/**
	 * Label displaying the current command-line options.
	 */
	private final Label cloField;

	/**
	 * Button used to apply or open standalone lab.
	 */
	private final Button applyButton;

	/**
	 * Button used to reset parameters to default values.
	 */
	private final Button defaultButton;

	/**
	 * Button used to open help for command-line options.
	 */
	private final Button helpButton;

	/**
	 * Button used to open the settings dialog.
	 */
	private final Button settingsButton;

	/**
	 * Command line option to mimic ePub modes and to disable device capabilities.
	 * <p>
	 * <strong>Note:</strong> for development/debugging only; should be disabled in
	 * production
	 */
	private final CLOption cloEmulate = new CLOption("emulate", null, CLOCategory.GUI,
			"--emulate <f1[,f2[...]]> list of GUI features to emulate:\n"
					+ "          epub: enable ePub mode\n"
					+ "    standalone: standalone ePub mode\n"
					+ "        nokeys: disable key events (if available)\n"
					+ "       nomouse: disable mouse events (if available)\n"
					+ "       notouch: disable touch events (if available)",
			new CLODelegate() {
				@Override
				public boolean parse(String arg) {
					detectFeatures();
					if (arg != null) {
						if (arg.contains("epub"))
							SETTINGS.isEPub = true;
						if (arg.contains("standalone")) {
							SETTINGS.standalone = true;
							SETTINGS.isEPub = true;
							SETTINGS.hasKeys = NativeJS.hasKeys();
							SETTINGS.hasMouse = NativeJS.hasMouse();
							SETTINGS.hasTouch = NativeJS.hasTouch();
						}
						if (SETTINGS.hasKeys && arg.contains("nokeys"))
							SETTINGS.hasKeys = false;
						if (SETTINGS.hasMouse && arg.contains("nomouse"))
							SETTINGS.hasMouse = false;
						if (SETTINGS.hasTouch && arg.contains("notouch"))
							SETTINGS.hasTouch = false;
					}
					return true;
				}
			});

	/**
	 * Creates a new settings controller.
	 * 
	 * @param cloField       the command-line option field
	 * @param applyButton    the apply button
	 * @param defaultButton  the default button
	 * @param helpButton     the help button
	 * @param settingsButton the settings button
	 */
	public SettingsController(Label cloField, Button applyButton, Button defaultButton,
			Button helpButton,
			Button settingsButton) {
		this.cloField = cloField;
		this.applyButton = applyButton;
		this.defaultButton = defaultButton;
		this.helpButton = helpButton;
		this.settingsButton = settingsButton;
		if (NativeJS.getEPubReader() != null)
			detectFeatures();
	}

	/**
	 * Returns the {@code --emulate} command-line option.
	 *
	 * @return CLO option controlling the emulation behaviour
	 */
	public CLOption getCloEmulate() {
		return cloEmulate;
	}

	/**
	 * Applies the appropriate UI toggles based on the current ePub mode. Enables or
	 * disables parameter editing widgets and registers drag-and-drop handlers when
	 * editing is permitted.
	 *
	 * @param dropHandlerRegistrar callback that installs drag-and-drop handlers
	 * @return {@code true} if editing is enabled
	 */
	public boolean applyUiToggles(Runnable dropHandlerRegistrar) {
		boolean editCLO = !SETTINGS.isEPub || SETTINGS.standalone;
		updateCLOControls(editCLO);
		if (editCLO && dropHandlerRegistrar != null)
			dropHandlerRegistrar.run();
		return editCLO;
	}

	/**
	 * Reports whether the environment is an ePub reader.
	 *
	 * @return {@code true} when running in an ePub context
	 */
	public boolean isEPub() {
		return SETTINGS.isEPub;
	}

	/**
	 * Reports whether the ePub lab runs on a standalone page instead of inline
	 * within text flow.
	 *
	 * @return {@code true} for standalone pages
	 */
	public boolean isStandalone() {
		return SETTINGS.standalone;
	}

	/**
	 * Reports whether the ePub reader exposes keyboard input.
	 *
	 * @return {@code true} if keyboard events are available
	 */
	public boolean hasKeys() {
		return SETTINGS.hasKeys;
	}

	/**
	 * Reports whether the ePub reader exposes mouse input.
	 *
	 * @return {@code true} if mouse events are available
	 */
	public boolean hasMouse() {
		return SETTINGS.hasMouse;
	}

	/**
	 * Reports whether the ePub reader exposes touch input.
	 *
	 * @return {@code true} if touch events are available
	 */
	public boolean hasTouch() {
		return SETTINGS.hasTouch;
	}

	/**
	 * Detects the capabilities of the current ePub host and updates the shared
	 * settings accordingly.
	 */
	private void detectFeatures() {
		SETTINGS.isEPub = (NativeJS.getEPubReader() != null);
		SETTINGS.standalone = (Document.get().getElementById("evoludo-standalone") != null);
		SETTINGS.hasKeys = NativeJS.ePubReaderHasFeature("keyboard-events");
		SETTINGS.hasMouse = NativeJS.ePubReaderHasFeature("mouse-events");
		SETTINGS.hasTouch = NativeJS.ePubReaderHasFeature("touch-events");
	}

	/**
	 * Updates the command-line option controls to reflect whether editing is
	 * allowed.
	 *
	 * @param editCLO {@code true} when editing is permitted
	 */
	private void updateCLOControls(boolean editCLO) {
		cloField.setTitle(editCLO ? "Specify simulation parameters"
				: "Current simulation parameters (open standalone lab to modify)");
		applyButton.setText(editCLO ? BUTTON_APPLY : BUTTON_STANDALONE);
		applyButton.setTitle(editCLO ? "Apply parameters" : "Open standalone lab");
		defaultButton.setEnabled(editCLO);
		helpButton.setEnabled(editCLO);
		settingsButton.setTitle(editCLO ? "Change simulation parameters" : "Review simulation parameters");
	}

	/**
	 * Reports whether no other ePub lab is currently running, allowing this lab to
	 * start execution.
	 * 
	 * @return {@code true} when no other ePub lab is running
	 */
	public static boolean isReady() {
		return !SETTINGS.isEPub || runningLab == null;
	}

	/**
	 * Marks the supplied lab as the currently running ePub instance. Only a single
	 * lab may run at once.
	 *
	 * @param lab running lab instance
	 */
	public static void onModelRunning(EvoLudoWeb lab) {
		if (SETTINGS.isEPub && runningLab != null && runningLab != lab)
			throw new IllegalStateException("Another ePub lab is already running!");
		runningLab = lab;
	}

	/**
	 * Clears the running lab marker once execution stops.
	 *
	 * @param lab lab that just stopped
	 */
	public static void onModelStopped(EvoLudoWeb lab) {
		if (!SETTINGS.isEPub)
			return;
		if (runningLab == lab)
			runningLab = null;
	}
}
