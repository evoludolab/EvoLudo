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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;

import org.evoludo.simulator.models.LifecycleListener;
import org.evoludo.simulator.models.Model;
import org.evoludo.simulator.models.ModelType;
import org.evoludo.simulator.modules.Module;

/**
 * Helper to manage lifecycle listeners and dispatch lifecycle notifications as
 * well as loading and unloading modules and models.
 */
class LifecycleController {

	/**
	 * Owning engine used to query modules and dispatch events.
	 */
	private final EvoLudo engine;

	/**
	 * List of listeners for module/model load/unload lifecycle events.
	 */
	protected List<LifecycleListener> lifecycleListeners = new ArrayList<>();

	/**
	 * Create a controller bound to the supplied engine.
	 * 
	 * @param engine EvoLudo runtime to interact with
	 */
	public LifecycleController(EvoLudo engine) {
		this.engine = engine;
	}

	/**
	 * Add a lifecycle listener to the list of listeners that get notified when the
	 * model reaches lifecycle milestones.
	 * 
	 * @param newListener the new lifecycle listener
	 * 
	 * @see EvoLudo#addLifecycleListener(LifecycleListener)
	 */
	public void addListener(LifecycleListener newListener) {
		if (!lifecycleListeners.contains(newListener))
			lifecycleListeners.add(0, newListener);
	}

	/**
	 * Remove the lifecycle listener from the list of listeners that get notified
	 * when the model reaches lifecycle milestones.
	 * 
	 * @param obsoleteListener the listener to remove from list of lifecycle
	 *                         listeners
	 * 
	 * @see EvoLudo#removeLifecycleListener(LifecycleListener)
	 */
	public void removeListener(LifecycleListener obsoleteListener) {
		lifecycleListeners.remove(obsoleteListener);
	}

	/**
	 * Called whenever a new module has finished loading. Notifies all registered
	 * {@link LifecycleListener}s.
	 * 
	 * @see EvoLudo#fireModuleLoaded()
	 */
	public void fireModuleLoaded() {
		for (LifecycleListener i : lifecycleListeners)
			i.moduleLoaded();
		String authors = engine.activeModule.getAuthors();
		engine.logger.info(
				"Module loaded: " + engine.activeModule.getTitle() + (authors == null ? "" : " by " + authors));
	}

	/**
	 * Called whenever the current module has finished unloading. Notifies all
	 * registered {@link LifecycleListener}s.
	 * 
	 * @see EvoLudo#fireModuleUnloaded()
	 */
	public void fireModuleUnloaded() {
		for (LifecycleListener i : lifecycleListeners)
			i.moduleUnloaded();
		if (engine.activeModule != null)
			engine.logger.info("Module '" + engine.activeModule.getTitle() + "' unloaded");
	}

	/**
	 * Called after the state of the model has been restored either through
	 * drag'n'drop with the GWT GUI or through the <code>--restore</code> command
	 * line argument. Notifies all registered {@link LifecycleListener}s.
	 * 
	 * @see EvoLudo#fireModuleRestored()
	 */
	public void fireModuleRestored() {
		for (LifecycleListener i : lifecycleListeners)
			i.moduleRestored();
		engine.logger.info("Module restored");
	}

	/**
	 * Called whenever a new model has finished loading. Notifies all registered
	 * {@link LifecycleListener}s.
	 * 
	 * @see EvoLudo#fireModelLoaded()
	 */
	public void fireModelLoaded() {
		for (LifecycleListener i : lifecycleListeners)
			i.modelLoaded();
		engine.logger.info("Model '" + engine.activeModel.getType() + "' loaded");
	}

	/**
	 * Called whenever a new model has finished loading. Notifies all registered
	 * {@link LifecycleListener}s.
	 * 
	 * @see EvoLudo#fireModelUnloaded()
	 */
	public void fireModelUnloaded() {
		for (LifecycleListener i : lifecycleListeners)
			i.modelUnloaded();
		if (engine.activeModel != null)
			engine.logger.info("Model '" + engine.activeModel.getType() + "' unloaded");
	}

	/**
	 * Load new module with key <code>newModuleKey</code>. If necessary first unload
	 * current module.
	 *
	 * @param newModuleKey the key of the module to load
	 * @return false if <code>newModuleKey</code> not found and no active module
	 *         present; true otherwise
	 * 
	 * @see EvoLudo#loadModule(String)
	 */
	public boolean loadModule(String newModuleKey) {
		Module<?> newModule = engine.modules.get(newModuleKey);
		if (newModule == null) {
			if (engine.activeModule == null)
				return false;
			if (engine.logger.isLoggable(Level.WARNING))
				engine.logger
						.warning("module '" + newModuleKey + "' not found - keeping '" + engine.activeModule.getKey() + "'.");
			return true;
		}
		if (engine.activeModule != null) {
			if (engine.activeModule == newModule) {
				return true;
			}
			unloadModule();
		}
		engine.activeModule = newModule;
		if (engine.rng.isSeedSet())
			engine.rng.reset();
		engine.activeModule.load();
		engine.fireModuleLoaded();
		return true;
	}

	/**
	 * Unload current module to free up resources.
	 * 
	 * <h3>Implementation note:</h3>
	 * Called from {@link #loadModule(String)} to first unload the active module or
	 * triggered by GWT's {@link org.evoludo.EvoLudoWeb#onUnload()}, i.e. when
	 * unloading the GWT application.
	 * 
	 * @see EvoLudo#unloadModule()
	 */
	public void unloadModule() {
		unloadModel(true);
		if (engine.activeModule != null) {
			for (Iterator<? extends Module<?>> it = engine.activeModule.getSpecies().iterator(); it.hasNext();) {
				Module<?> mod = it.next();
				mod.unload();
				if (mod != engine.activeModule)
					it.remove();
			}
		}
		engine.fireModuleUnloaded();
	}

	/**
	 * Set model type and loads the corresponding frameworks for individual based
	 * simulations or numerical integration of ODE/SDE/PDE models.
	 *
	 * @param type the type of {@link Model} to load
	 * 
	 * @see EvoLudo#loadModel(ModelType)
	 */
	public void loadModel(ModelType type) {
		if (engine.activeModel != null && engine.activeModel.getType() == type) {
			return;
		}
		Model newModel = engine.activeModule.createModel(type);
		if (newModel == null) {
			if (engine.logger.isLoggable(Level.WARNING)) {
				String msg = "model type '" + type + "' not supported.";
				if (engine.activeModel == null)
					engine.logger.warning(msg);
				else
					engine.logger.warning(msg + " keeping '" + engine.activeModel.getType() + "'.");
			}
			return;
		}
		unloadModel();
		engine.activeModel = newModel;
		engine.addCLOProvider(engine.activeModel);
		engine.activeModule.setModel(engine.activeModel);
		engine.activeModel.load();
		engine.fireModelLoaded();
	}

	/**
	 * Unload model framework. Notifies all registered {@link LifecycleListener}s.
	 * 
	 * @see EvoLudo#unloadModel()
	 */
	public void unloadModel() {
		unloadModel(false);
	}

	/**
	 * Unload model framework and, if requested, notifies all registered
	 * {@link LifecycleListener}s.
	 * 
	 * @param quiet set to {@code true} to skip notifying listeners
	 * 
	 * @see EvoLudo#unloadModel(boolean)
	 */
	public void unloadModel(boolean quiet) {
		if (engine.activeModel == null)
			return;
		engine.removeCLOProvider(engine.activeModel);
		engine.activeModel.unload();
		if (!quiet)
			engine.fireModelUnloaded();
		engine.activeModel = null;
	}
}
