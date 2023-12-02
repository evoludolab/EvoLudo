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

import java.awt.Color;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.evoludo.math.ArrayMath;
import org.evoludo.math.MersenneTwister;
import org.evoludo.math.RNGDistribution;
import org.evoludo.simulator.models.IBS.HasIBS;
import org.evoludo.simulator.models.Model.Mode;
import org.evoludo.simulator.models.Model.Type;
import org.evoludo.simulator.models.IBSC;
import org.evoludo.simulator.models.IBSD;
import org.evoludo.simulator.models.IBSPopulation;
import org.evoludo.simulator.models.Model;
import org.evoludo.simulator.models.ODEEuler.HasODE;
import org.evoludo.simulator.models.ODERK;
import org.evoludo.simulator.models.PDERD;
import org.evoludo.simulator.models.PDERD.HasPDE;
import org.evoludo.simulator.models.PDESupervisor;
import org.evoludo.simulator.models.SDEEuler;
import org.evoludo.simulator.models.SDEEuler.HasSDE;
import org.evoludo.simulator.modules.ATBT;
import org.evoludo.simulator.modules.CDL;
import org.evoludo.simulator.modules.CDLP;
import org.evoludo.simulator.modules.CDLPQ;
import org.evoludo.simulator.modules.CG;
import org.evoludo.simulator.modules.CLabour;
import org.evoludo.simulator.modules.CSD;
import org.evoludo.simulator.modules.Continuous;
import org.evoludo.simulator.modules.DemesTBT;
import org.evoludo.simulator.modules.Dialect;
import org.evoludo.simulator.modules.Discrete;
import org.evoludo.simulator.modules.EcoMoran;
import org.evoludo.simulator.modules.EcoMutualism;
import org.evoludo.simulator.modules.EcoPGG;
import org.evoludo.simulator.modules.EcoTBT;
import org.evoludo.simulator.modules.Module;
import org.evoludo.simulator.modules.Moran;
import org.evoludo.simulator.modules.Motility;
import org.evoludo.simulator.modules.Mutualism;
import org.evoludo.simulator.modules.NetDyn;
import org.evoludo.simulator.modules.RSP;
import org.evoludo.simulator.modules.TBT;
import org.evoludo.simulator.modules.Test;
import org.evoludo.simulator.views.HasPhase2D;
import org.evoludo.simulator.views.HasPop2D;
import org.evoludo.simulator.views.HasPop3D;
import org.evoludo.simulator.views.HasS3;
import org.evoludo.util.CLOParser;
import org.evoludo.util.CLOProvider;
import org.evoludo.util.CLOption;
import org.evoludo.util.CLOption.CLODelegate;
import org.evoludo.util.CLOption.Key;
import org.evoludo.util.Formatter;
import org.evoludo.util.Plist;

/**
 * Interface with the outside world. Deals with command line options, help,
 * encoding/restoring state, logging, printing of result, etc. GWT/JRE neutral
 * abstract implementation of interface between EvoLudo core and GUI interface.
 * JRE specific code relegated to {@link org.evoludo.simulator.EvoLudoJRE
 * EvoLudoJRE} and GWT specific code to
 * {@link org.evoludo.simulator.EvoLudoGWT EvoLudoGWT}. Two distinct
 * implementations are required e.g. because of significant differences in
 * input/output routines.
 * 
 * @author Christoph Hauert
 */
public abstract class EvoLudo
		implements Model.MilestoneListener, Model.ChangeListener, CLOProvider, MersenneTwister.Chronometer {

	/**
	 * The interface to execute commands in a manner that is agnostic to the
	 * implementation details regarding GWT or JRE environments.
	 * 
	 * @author Christoph Hauert
	 */
	public interface Directive {

		/**
		 * The command to execute.
		 */
		public void execute();
	}

	/**
	 * Create an instance of the EvoLudo controller. EvoLudo manages different
	 * module/model implementations and the connection between simulations/numerical
	 * calculations and the GUI interface as well as the execution environment. This
	 * includes logging (verbosity and output channels), restoring previously saved
	 * engine states as well saving the current state of the engine, export its data
	 * or graphical snapshots.
	 */
	public EvoLudo() {
		logger = Logger.getLogger(EvoLudo.class.getName() + "-" + ID);
		addMilestoneListener(this);
		addChangeListener(this);
		parser = new CLOParser(this);
		// load all available modules
		addModule(new Moran(this));
		addModule(new EcoMoran(this));
		addModule(new Motility(this));
		addModule(new CG(this));
		addModule(new TBT(this));
		addModule(new EcoTBT(this));
		addModule(new ATBT(this));
		addModule(new DemesTBT(this));
		addModule(new RSP(this));
		addModule(new CDL(this));
		addModule(new CDLP(this));
		addModule(new CDLPQ(this));
		addModule(new Mutualism(this));
		addModule(new EcoMutualism(this));
		addModule(new EcoPGG(this));
		addModule(new CSD(this));
		addModule(new CLabour(this));
		addModule(new Dialect(this));
		addModule(new NetDyn(this));
		addModule(new Test(this));
	}

	/**
	 * The flag to indicate whether EvoLudo is running in a web browser (or ePub)
	 * using javascript generated by GWT or in the java JRE. Special characters in
	 * strings can result in headaches when running in an ePub (or, worse still, in
	 * XHTML pages).
	 * <p>
	 * <code>true</code> when running as GWT application; <code>false</code> for JRE
	 * applications.
	 */
	public static boolean isGWT = false;

	/**
	 * The flag to indicate whether the current device/program supports touch events
	 * <p>
	 * <strong>Note:</strong> cannot be <code>static</code> or <code>final</code> to
	 * allow disabling touch events for debugging (see
	 * {@link org.evoludo.simulator.EvoLudoGWT#cloGUIFeatures
	 * EvoLudoGWT.cloGUIFeatures}).
	 */
	public boolean hasTouch = false;

	/**
	 * The loggers of each EvoLudo lab instance need to have unique names to keep
	 * the logs separate. Use <code>IDcounter</code> to generate unique
	 * <code>ID</code>'s.
	 */
	private static int IDcounter = 0;

	/**
	 * Unique <code>ID</code> of EvoLudo instance.
	 * 
	 * @see #IDcounter
	 */
	public final int ID = IDcounter++;

	/**
	 * The logger for keeping track of and reporting events and issues.
	 */
	protected Logger logger;

	/**
	 * Utility method to log progress of simulations. For implementation details see
	 * {@link org.evoludo.simulator.EvoLudoJRE EvoLudoJRE} and
	 * {@link org.evoludo.simulator.EvoLudoGWT EvoLudoGWT}
	 *
	 * @param msg progress message
	 */
	public abstract void logProgress(String msg);

	/**
	 * Convenience method to log messages of simulations.
	 *
	 * @param msg information message
	 */
	public void logMessage(String msg) {
		logger.info(msg);
	}

	/**
	 * Convenience method to log warnings of simulations.
	 *
	 * @param msg warning message
	 */
	public void logWarning(String msg) {
		logger.warning(msg);
	}

	/**
	 * Convenience method to log errors of simulations.
	 *
	 * @param msg error message
	 */
	public void logError(String msg) {
		logger.severe(msg);
	}

	/**
	 * The shared random number generator to ensure reproducibility of results.
	 * 
	 * @see #getRNG()
	 */
	protected RNGDistribution rng = new RNGDistribution.Uniform();

	/**
	 * Get the shared random number generator to ensure the reproducibility of
	 * results. This is the only RNG that <strong>MUST</strong> be used for all
	 * calculation, i.e. must be shared with all models including distributions
	 * (e.g. for mutations, migration...)
	 * <p>
	 * <strong>Important:</strong>
	 * <ol>
	 * <li><em>Must</em> be used for any random numbers related to the modelling.
	 * <li>Must <em>not</em> be used for anything else, e.g. shared with layout
	 * procedures for networks (although it may be desirable to set a seed for the
	 * layout routines in a consistent manner if one was set for the simulations, to
	 * also ensure visual reproducibility).
	 * <li>If the number of random numbers drawn changes, any corresponding test
	 * fails almost certainly. Tests pass only if the results are identical.
	 * </ol>
	 *
	 * @return the one and only random number generator for modelling
	 * 
	 * @see RNGDistribution
	 */
	public RNGDistribution getRNG() {
		return rng;
	}

	/**
	 * The lookup table for all available modules.
	 */
	protected HashMap<String, Module> modules = new HashMap<String, Module>();

	/**
	 * Creates ordinary differential equation model for <code>module</code>.
	 * <p>
	 * <strong>Note:</strong> by default returns {@link ODERK} model.
	 * Override to provide custom implementation of ODE model.
	 * 
	 * @param module the interaction {@link Module}
	 * @return the ODE model for <code>module</code>
	 */
	public Model.ODE createODE(Module module) {
		if (module instanceof HasODE) {
			Model.ODE model = ((HasODE) module).createODE();
			if (model != null)
				return model;
		}
		return new ODERK(this, module);
	}

	/**
	 * Creates stochastic differential equation model for <code>module</code>.
	 * <p>
	 * <strong>Note:</strong> by default returns {@link SDEEuler} model.
	 * Override to provide custom implementation of SDE model.
	 * 
	 * @param module the interaction {@link Module}
	 * @return the SDE model for <code>module</code>
	 */
	public Model.SDE createSDE(Module module) {
		if (module instanceof HasSDE) {
			Model.SDE model = ((HasSDE) module).createSDE();
			if (model != null)
				return model;
		}
		return new SDEEuler(this, module);
	}

	/**
	 * Creates partial differential equation model for <code>module</code>.
	 * <p>
	 * <strong>Note:</strong> by default returns {@link PDERD} model.
	 * Override to provide custom implementation of PDE model.
	 * 
	 * @param module the interaction {@link Module}
	 * @return the PDE model for <code>module</code>
	 */
	public Model.PDE createPDE(Module module) {
		if (module instanceof HasPDE) {
			Model.PDE model = ((HasPDE) module).createPDE();
			if (model != null)
				return model;
		}
		return new PDERD(this, module);
	}

	/**
	 * Creates individual based simulation model for <code>module</code>.
	 * <p>
	 * <strong>Note:</strong> by default returns {@link IBSD} for {@link Discrete}
	 * models and {@link IBSC} for {@link Continuous} models.
	 * Override to provide custom implementation of IBS model.
	 * 
	 * @param module the interaction {@link Module}
	 * @return the IBS model for <code>module</code>
	 */
	public Model.IBS createIBS(Module module) {
		if (module instanceof HasIBS) {
			Model.IBS model = ((HasIBS) module).createIBS();
			if (model != null)
				return model;
		}
		if (module.isContinuous())
			return new IBSC(this);
		return new IBSD(this);
	}

	/**
	 * Generate 2D network. This is the factory method to provide different
	 * implementations for GWT and JRE. More specifically, the layouting process in
	 * GWT uses scheduling (asynchronous execution) to prevent the GUI from
	 * stalling, while JRE implementations take advantage of multiple threads for
	 * significantly faster execution due to parallelization.
	 * 
	 * @param geometry the geometry backing the 2D network
	 * @return new instance of a 2D network
	 */
	public abstract Network2D createNetwork2D(Geometry geometry);

	/**
	 * Generate 3D network. This is the factory method to provide different
	 * implementations for GWT and JRE. More specifically, the layouting process in
	 * GWT uses scheduling (asynchronous execution) to prevent the GUI from
	 * stalling, while JRE implementations take advantage of multiple threads for
	 * significantly faster execution due to parallelization.
	 * <p>
	 * <strong>Note:</strong> The {@code java3d} package is obsolete. At present no
	 * 3D implementation for java exists.
	 * 
	 * @param geometry the geometry backing the 3D network
	 * @return new instance of a 3D network
	 */
	public abstract Network3D createNetwork3D(Geometry geometry);

	/**
	 * The active model
	 */
	protected Model activeModel;

	/**
	 * Set model type and loads the corresponding frameworks for individual based
	 * simulations or numerical integration of ODE/SDE/PDE models. Notifies all
	 * registered {@link Model.MilestoneListener}'s of any changes.
	 *
	 * @param type the type of {@link Model} to load
	 * @return <code>true</code> if model type changed
	 */
	public boolean loadModel(Type type) {
		boolean changed = true;
		if (activeModel != null) {
			changed = (activeModel.getModelType() != type);
			if (!changed)
				return false;
			removeCLOProvider(activeModel);
			if (!(activeModel instanceof IBSPopulation))
				activeModel.unload();
		}
		switch (type) {
			case ODE:
				activeModel = createODE(activeModule);
				break;
			case SDE:
				activeModel = createSDE(activeModule);
				break;
			case PDE:
				activeModel = createPDE(activeModule);
				break;
			case IBS:
			default:
				activeModel = createIBS(activeModule);
		}
		addCLOProvider(activeModel);
		activeModel.load();
		if (changed)
			fireModelLoaded();
		requiresReset(changed);
		return changed;
	}

	/**
	 * Get the active model.
	 * 
	 * @return the active model
	 */
	public Model getModel() {
		return activeModel;
	}

	/**
	 * Checks if active {@link Model} is of {@link Model.Type} type.
	 * 
	 * @param type the type of Model to load
	 * @return <code>true</code> if active model is of type <code>type</code>
	 */
	public boolean isModelType(Model.Type type) {
		return activeModel.isModelType(type);
	}

	/**
	 * The flag to indicate whether a reset of the active model has been requested.
	 * This is necessary after certain parameter changes. For example, changing the
	 * population size (see {@link IBSPopulation#nPopulation}) requires a reset to
	 * (re)generate population geometries and initialize types/strategies.
	 */
	private boolean resetRequested = true;

	/**
	 * Request reset the active model, e.g. after change of parameters.
	 * 
	 * @param reset <code>true</code> if reset is requested
	 */
	public void requiresReset(boolean reset) {
		resetRequested |= reset;
	}

	/**
	 * The flag to indicate whether the statistics sample has been read - once the
	 * active model may start working on next sample.
	 */
	protected boolean statisticsSampleRead = true;

	/**
	 * Read the statistics sample from active model if it hasn't been read yet.
	 */
	protected void readStatisticsSample() {
		// check if new sample completed
		if (!statisticsSampleRead) {
			activeModel.readStatisticsSample();
			statisticsSampleRead = true;
		}
	}

	/**
	 * Reset the statistics sample. Call after processing to signal readiness for
	 * next sample.
	 */
	protected void initStatisticsSample() {
		statisticsSampleRead = false;
		activeModel.initStatisticsSample();
	}

	/**
	 * List of engine listeners that get notified when the state of the population
	 * changed, for example after population reset or completed an update step.
	 * 
	 * @see Model.MilestoneListener
	 */
	protected List<Model.MilestoneListener> milestoneListeners = new ArrayList<Model.MilestoneListener>();

	/**
	 * Add a milestone listener to the list of listeners that get notified when the
	 * model reaches milestones.
	 * 
	 * @param newListener the new milestone listener
	 */
	public void addMilestoneListener(Model.MilestoneListener newListener) {
		milestoneListeners.add(newListener);
	}

	/**
	 * Remove the milestone listener from the list of listeners that get notified
	 * when the model reaches milestones.
	 * 
	 * @param obsoleteListener the listener to remove from list of milestone
	 *                         listeners
	 */
	public void removeMilestoneListener(Model.MilestoneListener obsoleteListener) {
		milestoneListeners.remove(obsoleteListener);
	}

	/**
	 * List of change listeners that get notified when the model changes.
	 * 
	 * @see Model.ChangeListener
	 */
	protected List<Model.ChangeListener> changeListeners = new ArrayList<Model.ChangeListener>();

	/**
	 * Add a change listener to the list of listeners that get notified when the
	 * model changes.
	 * 
	 * @param newListener the new change listener
	 */
	public void addChangeListener(Model.ChangeListener newListener) {
		changeListeners.add(newListener);
	}

	/**
	 * Remove the change listener from the list of listeners that get notified when
	 * the model changes.
	 * 
	 * @param obsoleteListener the listener to remove from the list of change
	 *                         listeners
	 */
	public void removeChangeListener(Model.ChangeListener obsoleteListener) {
		changeListeners.remove(obsoleteListener);
	}

	/**
	 * Unload model framework. Notifies all registered
	 * {@link Model.MilestoneListener}'s.
	 */
	public void modelUnload() {
		if (activeModel == null)
			return;
		// remove CLO parsers that are no longer needed
		removeCLOProvider(activeModel);
		activeModel.unload();
		fireModelUnloaded();
		activeModel = null;
	}

	/**
	 * Check consistency of parameters in all populations.
	 * <p>
	 * <strong>Note:</strong> in multi-species interactions optimizations seem
	 * sensible only if all populations involved approve of them.
	 *
	 * @return <code>true</code> if reset is required
	 */
	public final boolean modelCheck() {
		// special case: if --generation 0 model should not run regardless of
		// whether --run option was provided.
		if (Math.abs(nGenerations) < 1e-8)
			setSuspended(false);
		// check module first; model may contact module
		boolean doReset = false;
		for (Module pop : activeModule.getSpecies())
			doReset |= pop.check();
		Type type = activeModel.getModelType();
		doReset |= activeModel.check();
		if (activeModel.getModelType() != type)
			return modelCheck();
		return doReset;
	}

	/**
	 * Reset all populations and notify all listeners.
	 */
	public final void modelReset() {
		// reset random number generator if seed was specified
		if (rng.isRNGSeedSet())
			rng.setRNGSeed();
		// check consistency of parameters in models
		modelCheck();
		for (Module pop : activeModule.getSpecies())
			pop.reset();
		activeModel.reset();
		resetRequested = false;
		modelInit();
		modelUpdate();
		// notify of reset
		fireModelReset();
	}

	/**
	 * Initialize all populations (includes strategies but not structures).
	 */
	public final void modelInit() {
		initStatisticsSample();
		for (Module pop : activeModule.getSpecies())
			pop.init();
		activeModel.init();
		resetCPUSample();
	}

	/**
	 * Re-init all populations and notify all listeners.
	 */
	public final void modelReinit() {
		modelInit();
		modelUpdate();
		fireModelReinit();
	}

	/**
	 * Update all populations.
	 */
	public final void modelUpdate() {
		activeModel.update();
	}

	/**
	 * Starting time of CPU sampling (negative if not sampling).
	 */
	double cpu;

	/**
	 * Mean time of CPU sampling.
	 */
	double cpuMean;

	/**
	 * Variance of time of CPU sampling.
	 */
	double cpuVar;

	/**
	 * Number of CPU time samples
	 */
	int cpuSamples;

	/**
	 * Resets sampling of CPU time.
	 */
	protected void resetCPUSample() {
		cpu = -1.0;
		cpuMean = cpuVar = 0.0;
		cpuSamples = 0;
	}

	/**
	 * Starts sampling CPU time.
	 */
	protected void startCPUSample() {
		if (cpu < 0.0)
			cpu = elapsedTimeMsec();
	}

	/**
	 * Finish sampling CPU time.
	 */
	protected void endCPUSample() {
		if (logger.isLoggable(Level.FINE)) {
			double time = elapsedTimeMsec() - cpu;
			cpuSamples++;
			double incr = time - cpuMean;
			cpuMean += incr / cpuSamples;
			double incr2 = time - cpuMean;
			cpuVar += incr * incr2;
			cpu = -1.0;
			// note: unloading a running simulation clears activeModel
			if (activeModel == null)
				return;
			logger.fine("CPU time: " + time + " @ " + Formatter.format(activeModel.getTime(), 3) + ", mean  "
					+ Formatter.formatFix(cpuMean, 2) + " +/- "
					+ Formatter.formatFix(Math.sqrt(cpuVar / (cpuSamples - 1)), 2));
		}
	}

	/**
	 * Advance model by one step (<code>reportFreq</code> updates) and notify
	 * all listeners.
	 *
	 * @return <code>true</code> if not converged, i.e. if <code>modelNext()</code>
	 *         can be called again.
	 */
	public final boolean modelNext() {
		startCPUSample();
		fireModelRunning();
		if (activeModel.isAsynchronous()) {
			activeModel.next();
			return true;
		}
		return modelNextDone(activeModel.next());
	}

	/**
	 * Relax model by {@code nRelaxation} steps and notify all listeners when done.
	 *
	 * @return <code>true</code> if not converged, i.e. if <code>modelNext()</code>
	 *         can be called.
	 * 
	 * @see #cloRelaxation
	 * @see Model#relax(double)
	 */
	public final boolean modelRelax() {
		if (nRelaxation < 1.0)
			return true;
		boolean cont = activeModel.relax(nRelaxation);
		fireModelRelaxed();
		return cont;
	}

	/**
	 * Called after the calculations of the next state of the model have finished.
	 * For GWT this method serves as a callback for the asynchronous model
	 * computations. For JRE this runs in a separate thread from the GUI and
	 * {@link #modelNext()} directly calls this.
	 *
	 * @param cont <code>false</code> if converged or halting time reached
	 * @return <code>true</code> if not converged, i.e. if <code>modelNext()</code>
	 *         can be called again.
	 */
	public final boolean modelNextDone(boolean cont) {
		if (doPrev) {
			doPrev = false;
			activeModel.setTimeReversed(!activeModel.isTimeReversed());
		}
		if (!cont) {
			fireModelStopped();
			return false;
		}
		fireModelChanged();
		endCPUSample();
		return true;
	}

	/**
	 * Called after parameters have changed. Checks new settings and resets
	 * population(s) (and/or GUI) if necessary.
	 *
	 * @return <code>true</code> if reset was necessary
	 */
	public boolean paramsDidChange() {
		if (resetRequested) {
			modelReset();
			return true;
		}
		if (modelCheck()) {
			modelReset();
			return true;
		}
		modelUpdate();
		return false;
	}

	/**
	 * The command line options (raw string provided in URL, HTML tag, TextArea or
	 * command line)
	 */
	protected String clo;

	/**
	 * Get the raw command line options, as provided in URL, HTML tag, settings
	 * TextArea or command line.
	 * 
	 * @return command line options
	 */
	public String getCLO() {
		return clo;
	}

	/**
	 * Set the raw command line options, as shown e.g. in the settings TextArea.
	 * 
	 * @param clo the new command line option string
	 */
	public void setCLO(String clo) {
		this.clo = clo;
	}

	/**
	 * The flag to indicate whether running of the model is suspended. For example
	 * while parameters are being applied. If the changes do not require a reset of
	 * the model the calculations are resumed after new parameters are applied. Also
	 * used when command line options are set to immediately start running after
	 * loading (see {@link #cloRun}).
	 */
	protected boolean isSuspended = false;

	/**
	 * Check whether the current model is suspended.
	 * 
	 * @return <code>true</code> if model is suspended
	 */
	public boolean isSuspended() {
		return isSuspended;
	}

	/**
	 * Set the flag indicating whether the model is suspended. A suspended model
	 * resumes execution as soon as possible. For example after a new set parameters
	 * has been checked.
	 *
	 * @param suspend <code>true</code> to indicate that model is suspended.
	 */
	public void setSuspended(boolean suspend) {
		isSuspended = suspend;
	}

	/**
	 * Command line option to restore state from file require special considerations
	 * (only applicable to JRE).
	 */
	protected boolean doRestore = false;

	/**
	 * Minimum delay between subsequent updates for speed slider
	 * {@link org.evoludo.ui.Slider}
	 */
	public static final double DELAY_MIN = 1.0;

	/**
	 * Maximum delay between subsequent updates for speed slider
	 * {@link org.evoludo.ui.Slider}
	 */
	public static final double DELAY_MAX = 10000.0;

	/**
	 * Initial delay between subsequent updates for speed slider
	 * {@link org.evoludo.ui.Slider}
	 */
	public static final double DELAY_INIT = 100.0;

	/**
	 * Delay decrement for speed slider {@link org.evoludo.ui.Slider}
	 */
	private static final double DELAY_INCR = 1.2;

	/**
	 * Delay between subsequent updates in milliseconds when model is running.
	 */
	protected int delay = (int) DELAY_INIT;

	/**
	 * Set delay between subsequent updates.
	 *
	 * @param delay in milliseconds
	 */
	public void setDelay(int delay) {
		this.delay = Math.min(Math.max(delay, (int) DELAY_MIN), (int) DELAY_MAX);
	}

	/**
	 * Get delay between subsequent updates.
	 *
	 * @return the delay in milliseconds
	 */
	public int getDelay() {
		return delay;
	}

	/**
	 * Increase delay between subsequent updates by fixed factor.
	 */
	public void increaseDelay() {
		int newdelay = delay;
		newdelay = (int) (newdelay * DELAY_INCR);
		if (newdelay == delay)
			newdelay++;
		setDelay(Math.min(newdelay, (int) DELAY_MAX));
	}

	/**
	 * Decrease delay between subsequent updates by fixed factor.
	 */
	public void decreaseDelay() {
		int newdelay = delay;
		newdelay = (int) (newdelay / DELAY_INCR);
		setDelay(Math.max(newdelay, (int) DELAY_MIN));
	}

	/**
	 * Load new module with key <code>newModuleKey</code>. If necessary first
	 * unload current module.
	 *
	 * @param newModuleKey the key of the module to load
	 * @return <code>true</code> if new module was loaded
	 */
	public Module loadModule(String newModuleKey) {
		Module newModule = modules.get(newModuleKey);
		if (newModule == null) {
			if (activeModule != null) {
				logger.warning("module '" + newModuleKey + "' not found - keeping '" + activeModule.getKey() + "'.");
				return activeModule; // leave as is
			}
			return null;
		}
		// check if newModule is different
		if (activeModule != null) {
			if (activeModule == newModule) {
				return activeModule;
			}
			unloadModule();
		}
		activeModule = newModule;
		if (rng.isRNGSeedSet())
			rng.setRNGSeed();
		addCLOProvider(activeModule);
		activeModule.load();
		return activeModule;
	}

	/**
	 * Unload current module to free up resources.
	 * 
	 * <h3>Implementation note:</h3>
	 * Called from {@link loadModule} to first unload the active module
	 * or triggered by GWT's
	 * {@link org.evoludo.EvoLudoWeb#onUnload()}, i.e. when unloading
	 * the GWT application. In both cases the model has stopped running (either
	 * through {@link PendingAction#APPLY} or {@link PendingAction#UNLOAD}) and
	 * hence no need to issue further requests.
	 */
	public void unloadModule() {
		if (activeModule != null) {
			removeCLOProvider(activeModule);
			// clear species except active one
			// note: cannot simply re-add activeModule after clearing species b/c Module
			// cannot be added to List<? extends Module>...
			for (Iterator<? extends Module> iterator = activeModule.getSpecies().iterator(); iterator.hasNext();) {
				Module pop = iterator.next();
				pop.unload();
				if (pop != activeModule)
					iterator.remove();
			}
		}
		modelUnload();
		activeModule = null;
	}

	/**
	 * The active module.
	 */
	protected Module activeModule;

	/**
	 * Gets the active {@link Module}.
	 * 
	 * @return the active module
	 */
	public Module getModule() {
		return activeModule;
	}

	/**
	 * Add <code>module</code> to lookup table of modules using the module's key. If
	 * a GUI is present, add GUI as a listener of <code>module</code> to get
	 * notified about state changes.
	 *
	 * @param module the module to add to lookup table
	 */
	public void addModule(Module module) {
		String key = module.getKey();
		modules.put(key, module);
		cloModule.addKey(key, module.getTitle());
	}

	/**
	 * Execute <code>directive</code> in JRE or GWT environments.
	 *
	 * @param directive the directive to execute in appropriate GWT or JRE manners
	 */
	public abstract void execute(Directive directive);

	/**
	 * The flag to indicate whether the active model is running.
	 */
	protected boolean isRunning = false;

	/**
	 * Check if the active model is running.
	 * 
	 * @return <code>true</code> if model is running
	 */
	public boolean isRunning() {
		return isRunning;
	}

	/**
	 * Notification from GUI that layout process has finished. Opportunity for
	 * taking snapshots.
	 */
	public void layoutComplete() {
	}

	/**
	 * Requests halting of a running {@link Model} on the next opportunity.
	 */
	public void stop() {
		if (isRunning)
			requestAction(PendingAction.STOP);
	}

	/**
	 * Toggles running of a {@link Model} on the next opportunity: requests starting
	 * a halted model or halting a running
	 */
	public void toggle() {
		if (isRunning)
			stop();
		else
			run();
	}

	/**
	 * Start the EvoLudo model and calculate the dynamics one step at a time.
	 */
	public abstract void run();

	/**
	 * Advances the EvoLudo model by a single step. Called when pressing the 'Next'
	 * button, the 'n' or 'right-arrow' key.
	 */
	public abstract void next();

	/**
	 * Flag to indicate if a backstep is in progress.
	 */
	private boolean doPrev = false;

	/**
	 * Attempts to backtrack a single step of the EvoLudo model. Called when
	 * pressing the 'Prev' button, the 'p' or 'left-arrow' key.
	 */
	public void prev() {
		// requires that model permits time reversal and not in statistics mode
		if (!activeModel.permitsTimeReversal() || activeModel.isMode(Mode.STATISTICS))
			return;
		activeModel.setTimeReversed(!activeModel.isTimeReversed());
		doPrev = true;
		// next may return immediately - must reverse time again in modelNextDone()!
		next();
	}

	@Override
	public void modelStopped() {
		isRunning = false;
	}

	@Override
	public void modelDidReinit() {
		isRunning = false;
	}

	@Override
	public void modelDidReset() {
		isRunning = false;
	}

	/**
	 * Run simulation. Currently only implemented by EvoLudoJRE. Returns control to
	 * caller only if the arguments {@code args} do not match requirements for
	 * running a simulation.
	 * 
	 * @param args the {@code String} array of command line arguments
	 * 
	 * @see org.evoludo.simulator.EvoLudoJRE#simulation(String[])
	 */
	public void simulation(String[] args) {
	}

	/**
	 * The action that is pending (if any).
	 */
	protected PendingAction pendingAction = PendingAction.NONE;

	/**
	 * Requests a {@link PendingAction} to be performed on the next opportunity.
	 * 
	 * @param action the action requested
	 */
	public synchronized void requestAction(PendingAction action) {
		pendingAction = action;
		// if model is not running and not re-parsing of CLOs, process request
		// immediately
		if (!isRunning && !pendingAction.equals(PendingAction.CLO)) {
			_fireModelChanged();
		}
	}

	/**
	 * Called whenever a new model has finished loading. Notifies all registered
	 * {@link Model.MilestoneListener}s.
	 */
	public synchronized void fireModelLoaded() {
		runFired = false;
		pendingAction = PendingAction.NONE;
		for (Model.MilestoneListener i : milestoneListeners)
			i.modelLoaded();
		logger.info(
				"Module '" + activeModule.getTitle() + "' loaded\n" + activeModule.getInfo() + "\nVersion: "
						+ getVersion());
	}

	/**
	 * Called whenever the current model has finished unloading. Notifies all
	 * registered {@link Model.MilestoneListener}s.
	 */
	public synchronized void fireModelUnloaded() {
		runFired = false;
		for (Model.MilestoneListener i : milestoneListeners)
			i.modelUnloaded();
		logger.info("Module '" + activeModule.getTitle() + "' unloaded");
	}

	/**
	 * The flag to indicate whether the model running event has been processed. This
	 * is required to deal with repeated samples for {@code Mode#STATISTICS}.
	 * 
	 * @see Mode#STATISTICS
	 */
	private boolean runFired = false;

	/**
	 * Called whenever the model starts its calculations. Fires only when starting
	 * to run. Notifies all registered {@link Model.MilestoneListener}s.
	 */
	public synchronized void fireModelRunning() {
		if (runFired)
			return;
		for (Model.MilestoneListener i : milestoneListeners)
			i.modelRunning();
		runFired = isRunning();
	}

	/**
	 * Called whenever the state of the model has changed. For example, to
	 * trigger the update of the state displayed in the GUI. Processes pending
	 * actions and notifies all registered {@code Model.MilestoneListener}s.
	 * 
	 * @see Model.MilestoneListener
	 * @see PendingAction
	 */
	public synchronized void fireModelChanged() {
		// any specific request causes model to stop (and potentially resume later)
		if (pendingAction != PendingAction.NONE)
			runFired = false;
		if (activeModel.isMode(Mode.DYNAMICS)) {
			_fireModelChanged();
		}
	}

	/**
	 * Helper method for handling model changed events and processes pending
	 * actions.
	 * 
	 * @see Model.MilestoneListener
	 * @see PendingAction
	 */
	private void _fireModelChanged() {
		switch (pendingAction) {
			case UNLOAD:
				unloadModule();
				pendingAction = PendingAction.NONE;
				return;
			case INIT:
				modelReinit();
				break;
			case RESET:
				modelReset();
				break;
			case STOP:
				// stop requested (as opposed to simulations that stopped)
				runFired = false;
				for (Model.MilestoneListener i : milestoneListeners)
					i.modelStopped();
				break;
			case NONE:
			case APPLY:
			case SNAPSHOT:
			case STATISTIC:
				for (Model.ChangeListener i : changeListeners)
					i.modelChanged(pendingAction);
				break;
			default:
			// note: CLO re-parsing requests are handled separately, see parseCLO()
			// case CLO:
		}
		pendingAction = PendingAction.NONE;
	}

	/**
	 * Called after the state of the model has been restored either through
	 * drag'n'drop with the GWT GUI or through the <code>--restore</code> command
	 * line argument. Notifies all registered {@link Model.MilestoneListener}s.
	 *
	 * @see org.evoludo.EvoLudoWeb#restoreFromFile(String, String)
	 *      EvoLudoWeb.restoreFromFile(String, String)
	 * @see EvoLudo#restoreState(Plist)
	 */
	public synchronized void fireModelRestored() {
		runFired = false;
		for (Model.MilestoneListener i : milestoneListeners)
			i.modelRestored();
		logger.info("Engine restored.");
	}

	/**
	 * Called after the model has been re-initialized. Notifies all registered
	 * {@link Model.MilestoneListener}s.
	 */
	public synchronized void fireModelReinit() {
		if (activeModel.isMode(Mode.DYNAMICS) || !isRunning) {
			runFired = false;
			for (Model.MilestoneListener i : milestoneListeners)
				i.modelDidReinit();
			logger.info("Engine init.");
		}
	}

	/**
	 * Called after the model has been reset. Notifies all registered
	 * {@link Model.MilestoneListener}s.
	 */
	public synchronized void fireModelReset() {
		runFired = false;
		for (Model.MilestoneListener i : milestoneListeners)
			i.modelDidReset();
		logger.info("Engine reset.");
	}

	/**
	 * Called after the model completed its relaxation. Notifies all registered
	 * {@link Model.MilestoneListener}s.
	 */
	public synchronized void fireModelRelaxed() {
		runFired = false;
		for (Model.MilestoneListener i : milestoneListeners)
			i.modelRelaxed();
		logger.info("Engine relaxed.");
	}

	/**
	 * Called after the population has reached an absorbing state (or has converged
	 * to an equilibrium state). Notifies all registered
	 * {@link Model.MilestoneListener}s.
	 */
	public synchronized void fireModelStopped() {
		// check if new sample completed
		readStatisticsSample();
		if (activeModel.isMode(Mode.DYNAMICS)) {
			// MODE_DYNAMICS
			runFired = false;
			for (Model.MilestoneListener i : milestoneListeners)
				i.modelStopped();
		} else {
			// MODE_STATISTICS
			// note: calling fireModelChanged doesn't work because MODE_STATISTICS
			// prevents firing
			if (pendingAction == PendingAction.NONE)
				pendingAction = PendingAction.STATISTIC;
			_fireModelChanged();
		}
	}

	@Override
	public void modelChanged(PendingAction action) {
		switch (action) {
			case APPLY:
				if (isRunning) {
					isSuspended = true;
					isRunning = false;
				}
				break;
			case SNAPSHOT:
				isRunning = false;
				break;
			default:
		}
	}

	/**
	 * The relaxation time for simulations measured in generations.
	 * <p>
	 * <strong>Note:</strong> {@code nRelaxation} is set with the command line
	 * option <code>--relaxation</code>
	 * 
	 * @see #cloRelaxation
	 */
	protected double nRelaxation;

	/**
	 * Sets the number of generations to relax the initial configuration of the
	 * active {@link Model}. In interactive mode (with GUI) the active {@link Model}
	 * starts running upon launch and stop after {@code nRelaxation}.
	 * 
	 * @param nRelax the number of generations
	 * 
	 * @see #nRelaxation
	 */
	public void setNRelaxation(double nRelax) {
		nRelaxation = nRelax;
	}

	/**
	 * Gets the number of generations to relax the initial configuration of the
	 * active {@link Model}.
	 * 
	 * @return the number of generations
	 * 
	 * @see #nRelaxation
	 */
	public double getNRelaxation() {
		return nRelaxation;
	}

	/**
	 * The number of statistical samples to collect before returning the results.
	 * <p>
	 * <strong>Note:</strong> {@code nSamples} is set with the command line
	 * option <code>--samples</code>
	 * 
	 * @see #cloGenerations
	 */
	protected double nSamples;

	/**
	 * Sets the number of statistical samples taken after which the active {@link Model} is
	 * halted.
	 * 
	 * @param nSamples the number of generations
	 * 
	 * @see #nSamples
	 */
	public void setNSamples(double nSamples) {
		this.nSamples = nSamples;
	}

	/**
	 * Gets the number of statistical samples after which the active {@link Model} is
	 * halted.
	 * 
	 * @return the number of statistical samples
	 * 
	 * @see #nSamples
	 */
	public double getNSamples() {
		return nSamples;
	}

	/**
	 * Running simulations are halted when <code>generation &ge; nGenerations</code>
	 * holds for the first time. This is useful to indicate the end of simulations
	 * or to generate (graphical) snapshots in the GUI after a specified amount of
	 * time has elapsed.
	 * <p>
	 * <strong>Note:</strong> {@code nGenerations} is set with the command line
	 * option <code>--generations</code> (or <code>-g</code>),
	 * 
	 * @see #cloGenerations
	 */
	protected double nGenerations;

	/**
	 * Sets the number of generations after which the active {@link Model} is
	 * halted.
	 * 
	 * @param nGenerations the number of generations
	 * 
	 * @see #nGenerations
	 */
	public void setNGenerations(double nGenerations) {
		this.nGenerations = nGenerations;
	}

	/**
	 * Gets the number of generations after which the active {@link Model} is
	 * halted.
	 * 
	 * @return the number of generations
	 * 
	 * @see #nGenerations
	 */
	public double getNGenerations() {
		return nGenerations;
	}

	/**
	 * Gets the next generation for which stopping the model execution has been
	 * requested.
	 * 
	 * @return the next requested stop
	 */
	public double getNextHalt() {
		// watch out for models that allow time reversal!
		// nGenerations and nRelaxation can be positive or negative
		double time = activeModel.getTime();
		if (activeModel.isTimeReversed()) {
			// time is 'decreasing' find next smaller milestone
			double halt = nGenerations < time ? nGenerations : Double.NEGATIVE_INFINITY;
			double relax = (Math.abs(nRelaxation) > 1e-8 && nRelaxation < time) ? nRelaxation : Double.NEGATIVE_INFINITY;
			return Math.max(halt, relax);
		}
		// time is 'increasing'
		double halt = nGenerations > time ? nGenerations : Double.POSITIVE_INFINITY;
		double relax = (Math.abs(nRelaxation) > 1e-8 && nRelaxation > time) ? nRelaxation : Double.POSITIVE_INFINITY;
		return Math.min(halt, relax);
	}

	/**
	 * Indicates the interval (measured in generations) after which models report
	 * updates on their current state. for example, the GUI gets updated whenever
	 * <code>reportInterval</code> generations (or fractions thereof) have elapsed.
	 * <p>
	 * <strong>Note:</strong> <code>reportInterval&lt;0</code> disables reporting;
	 * <code>reportInterval=0</code> reports every single update.
	 */
	protected double reportInterval = 1.0;

	/**
	 * Set the report interval, i.e. number of updates in one step (see
	 * {@link #modelNext()} measured in generations (or fractions thereof).
	 *
	 * @param aValue the new report interval
	 */
	public void setReportInterval(double aValue) {
		reportInterval = Math.max(0.0, aValue);
	}

	/**
	 * Get the interval between subsequent reports to the GUI or the simulation
	 * controller measured in generations (or fractions thereof).
	 * 
	 * @return the report interval
	 */
	public double getReportInterval() {
		return reportInterval;
	}

	/**
	 * <strong>Note:</strong> Instead of sharing logging system, EvoLudo could
	 * implement helper routines for logging notifications. However, when logging
	 * notifications with a severity of {@link Level#WARNING} or higher the default
	 * logging message includes the name of the calling routine and hence would
	 * always refer to the (unhelpful) helper routines.
	 *
	 * @return logger of this EvoLudo controller
	 */
	public Logger getLogger() {
		return logger;
	}

	/**
	 * Hire supervisor for managing PDE calculations. This is the factory method
	 * provide different implementations For GWT and JRE. More specifically, GWT
	 * uses asynchronous execution to prevent the GUI from stalling, while JRE
	 * implementations take advantage of multiple threads for significantly faster
	 * execution due to parallelization.
	 *
	 * @param charge the PDE model to supervise
	 * @return supervisor for coordinating PDE calculations
	 * 
	 * @see org.evoludo.simulator.EvoLudoGWT#hirePDESupervisor(org.evoludo.simulator.models.Model.PDE)
	 *      EvoLudoGWT#hirePDESupervisor(org.evoludo.simulator.models.Model.PDE)
	 * @see org.evoludo.simulator.EvoLudoJRE#hirePDESupervisor(org.evoludo.simulator.models.Model.PDE)
	 *      EvoLudoJRE#hirePDESupervisor(org.evoludo.simulator.models.Model.PDE)
	 * @see org.evoludo.simulator.models.PDESupervisor
	 * @see org.evoludo.simulator.PDESupervisorGWT
	 * @see org.evoludo.simulator.PDESupervisorJRE
	 */
	public abstract PDESupervisor hirePDESupervisor(Model.PDE charge);

	/**
	 * {@inheritDoc}
	 * <p>
	 * Hide GWT/JRE differences in measuring execution time.
	 * 
	 * @see org.evoludo.simulator.EvoLudoGWT#elapsedTimeMsec
	 *      EvoLudoGWT.elapsedTimeMsec
	 * @see org.evoludo.simulator.EvoLudoJRE#elapsedTimeMsec
	 *      EvoLudoJRE.elapsedTimeMsec
	 */
	@Override
	public abstract int elapsedTimeMsec();

	/**
	 * The copyright string
	 */
	public static final String COPYRIGHT = "\u00a9 Christoph Hauert"; // \u00a9 UTF-8 character code for ©

	/**
	 * Return version string of current model. Version must include reference to git
	 * commit to ensure reproducibility of results.
	 *
	 * @return version string
	 */
	public String getVersion() {
		String version = COPYRIGHT;
		String git = getGit();
		if (activeModule != null)
			version += ", " + activeModule.getVersion();
		if ("unknown".equals(git))
			return version;
		return version + " (EvoLudo Engine: " + git + ")";
	}

	/**
	 * Gets current git version of code base.
	 * 
	 * @return the git version string
	 */
	public abstract String getGit();

	/**
	 * Gets the compilation date of the current git version.
	 * 
	 * @return the git compilation date as a string
	 */
	public abstract String getGitDate();

	/**
	 * timeout for layout process of snapshots in msec.
	 */
	protected int snapLayoutTimeout = -1;

	/**
	 * Gets the timeout for the layout process of snapshots in msec.
	 * 
	 * @return timeout in msec
	 */
	public int getSnapLayoutTimeout() {
		return snapLayoutTimeout;
	}

	/**
	 * Report all parameter settings to <code>output</code> (JRE only).
	 */
	public void dumpParameters() {
	}

	/**
	 * Concluding words for report (JRE only).
	 */
	public void dumpEnd() {
	}

	/**
	 * Export the current state of the engine using the appropriate means available
	 * in the current environment (GWT/JRE).
	 */
	public abstract void exportState();

	/**
	 * Encode current state of EvoLudo model as XML string (plist format).
	 *
	 * @return encoded state
	 */
	public String encodeState() {
		StringBuilder plist = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
				+ "<!DOCTYPE plist PUBLIC \"-//Apple//DTD PLIST 1.0//EN\" \"http://www.apple.com/DTDs/PropertyList-1.0.dtd\">\n"
				+ "<plist version=\"1.0\">\n" + "<dict>\n");
		plist.append(Plist.encodeKey("Export date", new Date().toString()));
		plist.append(Plist.encodeKey("Title", activeModule.getTitle()));
		plist.append(Plist.encodeKey("Version", getVersion()));
		plist.append(Plist.encodeKey("CLO", parser.getCLO()));
		activeModel.encodeState(plist);
		// the mersenne twister state is pretty long (and uninteresting) keep at end
		plist.append("<key>RNG state</key>\n" + "<dict>\n" + (rng.getRNG().encodeState()) + "</dict>\n");
		plist.append("</dict>\n" + "</plist>");
		return plist.toString();
	}

	/**
	 * Restore state of EvoLudo model from pre-processed plist, which encodes engine
	 * state (see {@link #encodeState()}).
	 * <p>
	 * <strong>Note:</strong> the appropriate model must already have been loaded
	 * and the command line arguments specified with the key <code>CLO</code> in the
	 * <code>plist</code> must also have been processed already.
	 * </p>
	 * <p>
	 * In JRE the options in <code>plist</code> are merged with any other command
	 * line arguments (albeit the ones in <code>plist</code> have priority to
	 * minimize the chance of complications). In GWT
	 * <code>restoreState(Plist)</code> is overridden to first deal with the command
	 * line arguments.
	 * </p>
	 *
	 * @param plist the lookup table with key value pairs
	 * @return <code>true</code> on successful restoration of state
	 */
	public boolean restoreState(Plist plist) {
		if (plist == null) {
			logger.severe("restore state failed (state empty).");
			return false;
		}
		// retrieve version
		String version = (String) plist.get("Version");
		if (version == null) {
			logger.severe("restore state failed (version missing).");
			return false;
		}
		doRestore = true;
		// version check
		String restoreGit = version.substring(version.lastIndexOf(' '), version.lastIndexOf(')'));
		String myVersion = getVersion();
		String myGit = myVersion.substring(myVersion.lastIndexOf(' '), myVersion.lastIndexOf(')'));
		if (!myGit.equals(restoreGit))
			// versions differ - may or may not be a problem...
			logger.warning(
					"state generated by version " + restoreGit + " but this is " + myGit + " - proceed with caution.");

		// restore RNG generator, population structure and state
		Plist rngstate = (Plist) plist.get("RNG state");
		boolean success = true;
		if (!rng.getRNG().restoreState(rngstate)) {
			logger.warning("restore RNG failed.");
			success = false;
		}
		success &= activeModel.restoreState(plist);
		if (success) {
			logger.info("Restore succeeded.");
			fireModelRestored();
		} else {
			logger.warning("restore failed - resetting model.");
			modelReset();
		}
		doRestore = false;
		return success;
	}

	/**
	 * The parser for command line options.
	 */
	protected CLOParser parser;

	/**
	 * Register <code>clo</code> as a provider of command line options. Initialize
	 * command line parser if necessary.
	 *
	 * @param provider the option provider to add
	 */
	public void addCLOProvider(CLOProvider provider) {
		if (provider == null)
			return;
		parser.addCLOProvider(provider);
	}

	/**
	 * Unregister <code>clo</code> as a provider of command line options.
	 *
	 * @param provider the option provider to remove
	 */
	public void removeCLOProvider(CLOProvider provider) {
		if (parser == null || provider == null)
			return;
		parser.removeCLOProvider(provider);
	}

	/**
	 * Pre-process array of command line arguments. Some arguments need priority
	 * treatment. Examples include the options <code>--module</code>,
	 * <code>--verbosity</code> or <code>--restore</code>.
	 * <dl>
	 * <dt>{@code --module}</dt>
	 * <dd>load module and remove option</dd>
	 * <dt>{@code --verbose}</dt>
	 * <dd>set verbosity level effective immediately. This ensures that issues when
	 * parsing the remaining command line options are already properly reported</dd>
	 * </dl>
	 *
	 * @param cloarray array of command line arguments
	 * @return pre-processed array of command line options
	 * 
	 * @see org.evoludo.simulator.EvoLudoJRE#preprocessCLO(String[])
	 *      EvoLudoJRE#preprocessCLO(String[])
	 * @see org.evoludo.simulator.EvoLudoGWT#preprocessCLO(String[])
	 *      EvoLudoGWT#preprocessCLO(String[])
	 */
	protected String[] preprocessCLO(String[] cloarray) {
		if (cloarray == null)
			return null;
		// first deal with --module option
		String moduleName = cloModule.getName();
		String newModuleKey = null;
		int nParams = cloarray.length;
		for (int i = 0; i < nParams; i++) {
			String param = cloarray[i];
			if (param.startsWith(moduleName)) {
				newModuleKey = CLOption.stripKey(moduleName, param).trim();
				if (param.length() == 0) {
					logger.warning("module key missing");
					return null;
				}
				// module key found; no need to continue
				cloarray = ArrayMath.drop(cloarray, i--);
				nParams--;
				break;
			}
		}
		if (loadModule(newModuleKey) == null)
			return null;
		// second determine feasible --model options for given module
		cloModel.clearKeys();
		Model.Type defaulttype = null;
		if (activeModule instanceof HasIBS && activeModule.hasSupport(Type.IBS)) {
			cloModel.addKey(Type.IBS);
			defaulttype = Type.IBS;
		}
		if (activeModule instanceof HasODE && activeModule.hasSupport(Type.ODE))
			cloModel.addKey(Type.ODE);
		if (activeModule instanceof HasSDE && activeModule.hasSupport(Type.SDE))
			cloModel.addKey(Type.SDE);
		if (activeModule instanceof HasPDE && activeModule.hasSupport(Type.PDE))
			cloModel.addKey(Type.PDE);
		// third deal with --model option
		String modelName = cloModel.getName();
		// if IBS is not an option, pick first available model as default (which one
		// remains unspecified)
		Collection<Key> keys = cloModel.getKeys();
		if (defaulttype == null)
			defaulttype = Model.Type.parse(keys.iterator().next().getKey());
		Model.Type type = null;
		nParams = cloarray.length;
		for (int i = 0; i < nParams; i++) {
			String param = cloarray[i];
			if (param.startsWith(modelName)) {
				String newModel = CLOption.stripKey(modelName, param).trim();
				// remove model option
				cloarray = ArrayMath.drop(cloarray, i--);
				nParams--;
				if (newModel.length() == 0) {
					logger.warning("model key missing - use " + defaulttype.getKey() + " as default.");
					type = defaulttype;
					// model key found; no need to continue
					break;
				}
				type = Model.Type.parse(newModel);
				if (type == null || !activeModule.hasSupport(type)) {
					logger.warning("invalid model type " + newModel + " - use " + defaulttype.getKey()
							+ " as default.");
					type = defaulttype;
				}
				// model key found; no need to continue
				break;
			}
		}
		if (type == null) {
			type = defaulttype;
			if (keys.size() > 1 && !defaulttype.getKey().equals(cloModel.getDefault()))
				// display warning if multiple models available (suppress if defaults match)
				logger.warning("model type unspecified - use " + type.getKey() + " as default.");
		}
		// NOTE: currently models cannot be mix'n'matched between species
		loadModel(type);
		// check if cloOptions contain --verbose
		String verboseName = cloVerbose.getName();
		for (int i = 0; i < nParams; i++) {
			String param = cloarray[i];
			if (param.startsWith(verboseName)) {
				String verbosity = CLOption.stripKey(verboseName, param).trim();
				if (verbosity.length() == 0) {
					logger.warning("verbose level missing - ignored.");
					// remove verbose option
					cloarray = ArrayMath.drop(cloarray, i--);
					nParams--;
					// verbosity key found; no need to continue
					break;
				}
				// parse --verbose first to set logging level already for processing of command
				// line arguments; gets processed again with all others but no harm in it
				cloVerbose.processOption(param, Arrays.asList(new String[] { verbosity }).listIterator());
				cloVerbose.parse();
				// verbosity key found; no need to continue
				break;
			}
		}
		return cloarray;
	}

	/**
	 * Parse command line options.
	 *
	 * @return <code>true</code> if parsing successful and <code>false</code> if
	 *         problems occurred
	 * 
	 * @see #parseCLO(String[])
	 */
	public boolean parseCLO() {
		return parseCLO(clo.trim().split("--"));
	}

	/**
	 * Pre-process and parse array of command line arguments.
	 *
	 * @param cloarray string array of command line arguments
	 * @return <code>true</code> if parsing successful and <code>false</code> if
	 *         problems occurred
	 * 
	 * @see #preprocessCLO(String[])
	 * @see CLOParser#parseCLO(String[])
	 */
	public boolean parseCLO(String[] cloarray) {
		parser.setLogger(logger);
		cloarray = preprocessCLO(cloarray);
		if (cloarray == null) {
			parser.clearCLO();
			parser.addCLO(cloModule);
			logger.severe("Mandatory option --" + cloModule.getName() + " not found!");
			logger.info("<pre>List of available modules:\n" + parser.helpCLO(false) + "</pre>");
			return false;
		}
		parser.initCLO();
		// preprocessing removed (and possibly altered) --module and --model options
		// add current settings back to cloarray
		cloarray = ArrayMath.append(cloarray, cloModule.getName() + " " + activeModule.getKey());
		cloarray = ArrayMath.append(cloarray, cloModel.getName() + " " + activeModel.getModelType());
		boolean success = parser.parseCLO(cloarray);
		if (pendingAction.equals(PendingAction.CLO)) {
			// start again from scratch
			pendingAction = PendingAction.NONE;
			parser.initCLO();
			return parser.parseCLO(cloarray);
		}
		return success;
	}

	/**
	 * Format, encode and output help on command line options.
	 */
	public abstract void helpCLO();

	/**
	 * The category for global options.
	 */
	public static final CLOption.Category catGlobal = new CLOption.Category("Global options:", 50);

	/**
	 * The category for module specific options.
	 */
	public static final CLOption.Category catModule = new CLOption.Category("Module specific options:", 40);

	/**
	 * The category for model specific options.
	 */
	public static final CLOption.Category catModel = new CLOption.Category("Model specific options:", 30);

	/**
	 * The category for simulation specific options.
	 */
	public static final CLOption.Category catSimulation = new CLOption.Category("Simulation specific options:", 20);

	/**
	 * The category for user interface specific options.
	 */
	public static final CLOption.Category catGUI = new CLOption.Category("User interface specific options:", 10);

	/**
	 * Command line option to set module.
	 */
	public final CLOption cloModule = new CLOption("module", null, catGlobal,
			"--module <m>    select module from:", new CLODelegate() {
				@Override
				public boolean parse(String arg) {
					// option gets special treatment
					return true;
				}
			});

	/**
	 * Command line option to set the type of model (see {@link Model.Type}).
	 */
	public final CLOption cloModel = new CLOption("model", Model.Type.IBS.getKey(), catModule,
			"--model <m>     model type", new CLODelegate() {
				@Override
				public boolean parse(String arg) {
					// option gets special treatment
					return true;
				}

				@Override
				public void report(PrintStream output) {
					output.println("# model:                " + activeModel.getModelType());
				}
			});

	/**
	 * Command line option to set seed of random number generator.
	 */
	public final CLOption cloSeed = new CLOption("seed", "no seed", CLOption.Argument.OPTIONAL, catGlobal,
			"--seed[=s]      random seed (0)", new CLODelegate() {
				@Override
				public boolean parse(String arg) {
					if (!cloSeed.isSet()) {
						// set default
						rng.clearRNGSeed();
						return true;
					}
					rng.setRNGSeed(Long.parseLong(arg));
					return true;
				}

				@Override
				public void report(PrintStream output) {
					output.println("# fixedseed:            " + rng.isRNGSeedSet()
							+ (rng.isRNGSeedSet() ? " (" + rng.getRNGSeed() + ")" : ""));
				}
			});

	/**
	 * Command line option to request that the EvoLudo model immediately starts
	 * running after loading.
	 */
	public final CLOption cloRun = new CLOption("run", catGUI,
			"--run           simulations run after launch", new CLODelegate() {
				@Override
				public boolean parse(String arg) {
					// by default do not interfere - i.e. leave simulations running if possible
					if (!cloRun.isSet())
						return true;
					setSuspended(true);
					return true;
				}
			});

	/**
	 * Command line option to set the delay between subsequent updates.
	 */
	public final CLOption cloDelay = new CLOption("delay", "" + DELAY_INIT, catGUI,  // DELAY_INIT
			"--delay <d>     delay between updates (d: delay in msec)", new CLODelegate() {
				@Override
				public boolean parse(String arg) {
					// by default do not interfere - i.e. leave delay as is
					if (!cloDelay.isSet())
						return true;
					setDelay(Integer.parseInt(arg));
					return true;
				}
			});

	/**
	 * Command line option to set the number of generations to relax the model from
	 * the initial configuration. After relaxation the model is assumed to be close
	 * to its (thermal) equilibrium. In particular, the system should be ready for
	 * measurements such as the strategy abundances, their fluctuations or the local
	 * strategy configurations in structured populations.
	 */
	public final CLOption cloRelaxation = new CLOption("relaxation", "0", catGlobal,
			"--relaxation <n>  relaxation time in MC steps", new CLODelegate() {
				@Override
				public boolean parse(String arg) {
					setNRelaxation(CLOParser.parseDouble(arg));
					if (getNRelaxation() > 0)
						setSuspended(true);
					return true;
				}

				@Override
				public void report(PrintStream output) {
					if (nRelaxation > 0)
						output.println("# relaxation:           " + Formatter.format(nRelaxation, 4));
				}
			});

	/**
	 * Command line option to set the number of generations after which to stop the
	 * model calculations. Model execution can be resumed afterwards.
	 */
	public final CLOption cloGenerations = new CLOption("generations", "never", catGlobal,
			"--generations <g>  halt execution after <g> MC steps", new CLODelegate() {
				@Override
				public boolean parse(String arg) {
					if (cloGenerations.isSet()) {
						setNGenerations(CLOParser.parseDouble(arg));
						return true;
					}
					String gens = cloGenerations.getDefault();
					setNGenerations(gens.equals("never") ? Double.POSITIVE_INFINITY : CLOParser.parseDouble(gens));
					return true;
				}

				@Override
				public void report(PrintStream output) {
					if (nGenerations > 0)
						output.println("# generations:          " + Formatter.format(nGenerations, 4));
				}
			});

	/**
	 * Command line option to set the number of samples to take for statistical
	 * measurements. 
	 */
	public final CLOption cloNSamples = new CLOption("samples", "1000", EvoLudo.catSimulation,
			"--samples       number of samples for statistics", new CLODelegate() {
				@Override
				public boolean parse(String arg) {
					setNSamples(CLOParser.parseDouble(arg));
					return true;
				}

		    	@Override
		    	public void report(PrintStream ps) {
					// for customized simulations
		    		ps.println("# samples:              "+Formatter.format(nSamples, 4));
		    	}
			});

	/**
	 * Command line option to set the number of generations between reports for
	 * {@link #modelNext()}.
	 */
	public final CLOption cloReportInterval = new CLOption("reportfreq", "1", catGlobal,
			"--reportfreq <f>  report frequency in MC steps", new CLODelegate() {
				@Override
				public boolean parse(String arg) {
					setReportInterval(Double.parseDouble(arg));
					return true;
				}

				@Override
				public void report(PrintStream output) {
					output.println("# reportfreq:           " + Formatter.format(getReportInterval(), 4));
				}
			});

	/**
	 * Command line option to set the color for trajectories. For example, this
	 * affects the display in {@link org.evoludo.simulator.MVS3} (and
	 * {@link org.evoludo.graphics.S3Graph}) or
	 * {@link org.evoludo.simulator.MVPhase2D} (and
	 * {@link org.evoludo.graphics.ParaGraph}).
	 */
	// note: cannot import org.evoludo.gwt classes in header (otherwise compilation
	// of java port fails).
	public final CLOption cloTrajectoryColor = new CLOption("trajcolor", "black", catGUI,
			"--trajcolor <c>  color for trajectories\n"
					+ "           <c>: color name or '(r,g,b[,a])' with r,g,b,a in [0-255]",
			new CLODelegate() {
				@Override
				public boolean parse(String arg) {
					Color color = CLOParser.parseColor(arg);
					if (color == null)
						return false;
					activeModule.setTrajectoryColor(color);
					return true;
				}
			});

	/**
	 * Command line option to set color scheme for coloring continuous traits.
	 * 
	 * @see ColorModelType
	 */
	public final CLOption cloTraitColorScheme = new CLOption("traitcolorscheme", "traits", catGUI,
			"--traitcolorscheme <m>  color scheme for traits:", //
			new CLODelegate() {
				@Override
				public boolean parse(String arg) {
					setColorModelType((ColorModelType) cloTraitColorScheme.match(arg));
					return true;
				}
			});

	/**
	 * Command line option to set the type of initial configuration.
	 * <p>
	 * <strong>Note:</strong> option not automatically added. Models that implement
	 * different initialization types should load it in
	 * {@link #collectCLO(CLOParser)}.
	 * 
	 * @see org.evoludo.simulator.models.IBSD.InitType
	 * @see org.evoludo.simulator.models.IBSC.InitType
	 * @see org.evoludo.simulator.models.ODEEuler.InitType
	 * @see org.evoludo.simulator.models.PDERD.InitType
	 */
	public final CLOption cloInitType = new CLOption("inittype", "-default", catModel,
			"--inittype <t>  type of initial configuration", new CLODelegate() {
				@Override
				public boolean parse(String arg) {
					activeModel.setInitType(cloInitType.match(arg));
					return true;
				}

				@Override
				public void report(PrintStream output) {
					output.println("# inittype:             " + activeModel.getInitType());
				}
			});

	/**
	 * Command line option to perform test of random number generator on launch.
	 * This takes approximately 10-20 seconds. The test reports (1) whether the
	 * generated sequence of random numbers is consistent with the reference
	 * implementation of {@link MersenneTwister} and (2) the performance of
	 * MersenneTwister compared to {@link java.util.Random}.
	 */
	public final CLOption cloRNG = new CLOption("testRNG", catGlobal,
			"--testRNG       test random number generator", new CLODelegate() {
				@Override
				public boolean parse(String arg) {
					if (!cloRNG.isSet())
						return true;
					// test of RNG requested
					logger.info("Testing MersenneTwister...");
					int start = elapsedTimeMsec();
					MersenneTwister.testCorrectness(logger);
					MersenneTwister.testSpeed(logger, EvoLudo.this, 10000000);
					int lap = elapsedTimeMsec();
					logger.info("MersenneTwister tests done: " + ((lap - start) / 1000.0) + " sec.");
					MersenneTwister mt = rng.getRNG();
					RNGDistribution.Uniform.test(mt, logger, EvoLudo.this);
					RNGDistribution.Exponential.test(mt, logger, EvoLudo.this);
					RNGDistribution.Normal.test(mt, logger, EvoLudo.this);
					RNGDistribution.Geometric.test(mt, logger, EvoLudo.this);
					RNGDistribution.Binomial.test(mt, logger, EvoLudo.this);
					return true;
				}
			});

	/**
	 * Command line option to set verbosity level of logging.
	 */
	public final CLOption cloVerbose = new CLOption("verbose", "info", catGlobal,
			"--verbose <l>   level of verbosity with l one of\n" //
			+ "                all, debug/finest, finer, fine, config,\n" //
			+ "                info, warning, error, or none",
			new CLODelegate() {
				@Override
				public boolean parse(String arg) {
					String larg = arg.toLowerCase();
					if ("all".startsWith(larg)) {
						logger.setLevel(Level.ALL);
						return true;
					}
					if ("debug".startsWith(larg)) {
						logger.setLevel(Level.FINEST);
						return true;
					}
					if ("finest".startsWith(larg)) {
						logger.setLevel(Level.FINEST);
						return true;
					}
					if ("finer".startsWith(larg)) {
						logger.setLevel(Level.FINER);
						return true;
					}
					if ("fine".startsWith(larg)) {
						logger.setLevel(Level.FINE);
						return true;
					}
					if ("debug".startsWith(larg)) {
						logger.setLevel(Level.CONFIG);
						return true;
					}
					if ("warning".startsWith(larg)) {
						logger.setLevel(Level.WARNING);
						return true;
					}
					if ("error".startsWith(larg) || "severe".startsWith(larg)) {
						logger.setLevel(Level.SEVERE);
						return true;
					}
					if ("none".startsWith(larg) || "off".startsWith(larg)) {
						logger.setLevel(Level.OFF);
						return true;
					}
					if ("info".startsWith(larg)) {
						logger.setLevel(Level.INFO);
						return true;
					}
					logger.warning("unknown verbosity '" + arg + "' - using '" + cloVerbose.getDefault() + "'.");
					return false;
				}
			});

	/**
	 * {@inheritDoc}
	 * <p>
	 * <strong>Note:</strong> In contrast to other providers of command line
	 * options, the EvoLudo class maintains a reference to the parser
	 * (<code>prsr</code> and <code>parser</code> must be identical).
	 */
	@Override
	public void collectCLO(CLOParser prsr) {
		parser.addCLO(cloVerbose);
		if (!EvoLudo.isGWT)
			// default verbosity if running as java application is warning
			cloVerbose.setDefault("warning");
		parser.addCLO(cloModule);
		parser.addCLO(cloModel);
		parser.addCLO(cloSeed);
		parser.addCLO(cloRun);
		parser.addCLO(cloDelay);
		parser.addCLO(cloGenerations);
		parser.addCLO(cloRelaxation);
		parser.addCLO(cloReportInterval);
		parser.addCLO(cloRNG);
		// option for trait color schemes only makes sense for modules with multiple
		// continuous traits that have 2D/3D visualizations
		if (activeModule.isContinuous() && activeModule.getNTraits() > 1 && //
			(activeModule instanceof HasPop2D || activeModule instanceof HasPop3D)) {
			parser.addCLO(cloTraitColorScheme);
			cloTraitColorScheme.addKeys(ColorModelType.values());
		}
		parser.addCLO(cloInitType);

		// trajectory color settings used by phase plane and simplex plots
		if (activeModule instanceof HasS3 || activeModule instanceof HasPhase2D)
			parser.addCLO(cloTrajectoryColor);
	}

	/**
	 * The coloring method type.
	 */
	protected ColorModelType colorModelType = ColorModelType.DEFAULT;

	/**
	 * Coloring method types for continuous traits. Enum on steroids. Currently
	 * available coloring types are:
	 * <dl>
	 * <dt>traits
	 * <dd>Each trait refers to a color channel. At most three traits for
	 * <span style="color:red;">red</span>, <span style="color:green;">green</span>,
	 * and <span style="color:blue;">blue</span> components. The brightness of the
	 * color indicates the value of the continuous trait. This is the default.
	 * <dt>distance
	 * <dd>Color the traits according to their (Euclidian) distance from the origin
	 * (heat map ranging from black and grey to yellow and red).
	 * <dt>DEFAULT
	 * <dd>Default coloring type. Not user selectable.
	 * </dl>
	 * <p>
	 * <strong>Note:</strong> currently only used by
	 * {@link org.evoludo.simulator.modules.Dialect}.
	 * 
	 * @see #cloTraitColorScheme
	 */
	public static enum ColorModelType implements CLOption.Key {

		/**
		 * Each trait refers to a color channel. At most three traits for
		 * <span style="color:red;">red</span>, <span style="color:green;">green</span>,
		 * and <span style="color:blue;">blue</span> components.
		 */
		TRAITS("traits", "each trait (&le;3) refers to color channel"), //

		/**
		 * Color the traits according to their (Euclidian) distance from the origin.
		 */
		DISTANCE("distance", "distance of traits from origin"), //

		/**
		 * Default coloring type. Not user selectable.
		 */
		DEFAULT("-default", "default coloring scheme");

		/**
		 * The name of the color model type.
		 */
		private final String key;

		/**
		 * Brief description of the color model type for help display.
		 * 
		 * @see EvoLudo#helpCLO()
		 */
		private final String title;

		/**
		 * Create a new color model type.
		 * 
		 * @param key the name of the color model
		 * @param title the title of the color model
		 * 
		 * @see #cloTraitColorScheme
		 */
		ColorModelType(String key, String title) {
			this.key = key;
			this.title = title;
		}

		@Override
		public String getKey() {
			return key;
		}

		@Override
		public String getTitle() {
			return title;
		}

		@Override
		public String toString() {
			return key + ": " + title;
		}
	}

	/**
	 * Get the type of color model for translating continuous traits into colors.
	 * 
	 * @return the type of color model
	 * 
	 * @see #cloTraitColorScheme
	 */
	public ColorModelType getColorModelType() {
		return colorModelType;
	}

	/**
	 * Set the type of the color model for translating continuous traits into
	 * colors.
	 * 
	 * @param colorModelType the new type of color model
	 * 
	 * @see #cloTraitColorScheme
	 */
	public void setColorModelType(ColorModelType colorModelType) {
		if (colorModelType == null)
			return;
		this.colorModelType = colorModelType;
	}
}
