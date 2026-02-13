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

package org.evoludo.simulator.ui;

import org.evoludo.EvoLudoWeb;
import org.evoludo.simulator.EvoLudoGWT;
import org.evoludo.simulator.models.ChangeListener;
import org.evoludo.simulator.models.LifecycleListener;
import org.evoludo.simulator.models.RunListener;
import org.evoludo.simulator.models.SampleListener;

/**
 * Centralizes all engine listener callbacks for {@link EvoLudoWeb}.
 * Interacts with the UI controller directly to keep EvoLudoWeb slim.
 */
public class WebListener implements LifecycleListener, RunListener, ChangeListener, SampleListener {

	/**
	 * The associated EvoLudo engine.
	 */
	private final EvoLudoGWT engine;

	/**
	 * The associated EvoLudoWeb instance.
	 */
	private final EvoLudoWeb gui;

	/**
	 * The key controller used to manage keyboard events.
	 */
	private final KeyHandler keyController;

	/**
	 * Creates a new web listener.
	 * 
	 * @param gui           the EvoLudoWeb GUI
	 * @param engine        the EvoLudo engine
	 * @param keyController the key controller
	 */
	public WebListener(EvoLudoWeb gui, EvoLudoGWT engine, KeyHandler keyController) {
		this.gui = gui;
		this.engine = engine;
		this.keyController = keyController;
		engine.addLifecycleListener(this);
		engine.addRunListener(this);
		engine.addSampleListener(this);
		engine.addChangeListener(this);
	}

	/**
	 * Unregisters this listener from the engine.
	 */
	public void unload() {
		engine.removeLifecycleListener(this);
		engine.removeRunListener(this);
		engine.removeSampleListener(this);
		engine.removeChangeListener(this);
		SettingsController.onModelStopped(gui);
		keyController.unregister();
	}

	@Override
	public void moduleLoaded() {
		keyController.register();
	}

	@Override
	public void moduleUnloaded() {
		gui.resetViewSelection();
		gui.clearCommandLineOptions();
		SettingsController.onModelStopped(gui);
		keyController.unregister();
	}

	@Override
	public void moduleRestored() {
		gui.resetStatusThreshold();
		gui.displayStatus("State successfully restored.");
	}

	@Override
	public void modelUnloaded() {
		SettingsController.onModelStopped(gui);
		gui.resetViewSelection();
	}

	@Override
	public void modelRunning() {
		SettingsController.onModelRunning(gui);
		gui.resetStatusThreshold();
		gui.updateGUI();
	}

	@Override
	public void modelChanged(PendingAction action) {
		switch (action) {
			case CHANGE_MODE:
				gui.resetStatusThreshold();
				//$FALL-THROUGH$
			case NONE:
				gui.updateStatus();
				gui.updateCounter();
				break;
			default:
				// includes SHUTDOWN, RESET, INIT, STOP, MODE
		}
		if (!engine.isRunning())
			gui.updateGUI();
	}

	@Override
	public void modelSample(boolean success) {
		if (success)
			gui.updateStatus();
		gui.updateCounter();
	}

	@Override
	public void modelStopped() {
		SettingsController.onModelStopped(gui);
		gui.updateGUI();
	}

	@Override
	public void modelDidInit() {
		gui.updateGUI();
	}

	@Override
	public void modelSettings() {
		gui.updateGUI();
	}

	@Override
	public void modelDidReset() {
		gui.handleModelDidReset();
	}
}
