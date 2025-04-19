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

import java.awt.Color;
import java.io.PrintStream;
import java.util.ArrayList;
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
import org.evoludo.simulator.models.ChangeListener;
import org.evoludo.simulator.models.ChangeListener.PendingAction;
import org.evoludo.simulator.models.IBS;
import org.evoludo.simulator.models.IBSC;
import org.evoludo.simulator.models.IBSD;
import org.evoludo.simulator.models.IBSPopulation;
import org.evoludo.simulator.models.MilestoneListener;
import org.evoludo.simulator.models.Mode;
import org.evoludo.simulator.models.Model;
import org.evoludo.simulator.models.ODE.HasODE;
import org.evoludo.simulator.models.RungeKutta;
import org.evoludo.simulator.models.PDE;
import org.evoludo.simulator.models.PDE.HasPDE;
import org.evoludo.simulator.models.PDESupervisor;
import org.evoludo.simulator.models.SDE;
import org.evoludo.simulator.models.SDE.HasSDE;
import org.evoludo.simulator.models.Type;
import org.evoludo.simulator.modules.ATBT;
import org.evoludo.simulator.modules.CDL;
import org.evoludo.simulator.modules.CDLP;
import org.evoludo.simulator.modules.CDLPQ;
import org.evoludo.simulator.modules.CLabour;
import org.evoludo.simulator.modules.CSD;
import org.evoludo.simulator.modules.Centipede;
import org.evoludo.simulator.modules.Continuous;
import org.evoludo.simulator.modules.DemesTBT;
import org.evoludo.simulator.modules.Discrete;
import org.evoludo.simulator.modules.EcoPGG;
import org.evoludo.simulator.modules.Module;
import org.evoludo.simulator.modules.Moran;
import org.evoludo.simulator.modules.NetGames;
import org.evoludo.simulator.modules.RSP;
import org.evoludo.simulator.modules.SIR;
import org.evoludo.simulator.modules.TBT;
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
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 */
public abstract class EvoLudo
		implements CLOProvider, MersenneTwister.Chronometer {

	/**
	 * The interface to execute commands in a manner that is agnostic to the
	 * implementation details regarding GWT or JRE environments.
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
		this(true);
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
	 * <strong>Note:</strong> by default returns {@link RungeKutta} model.
	 * Override to provide custom implementation of ODE model.
	 * 
	 * @param module the interaction {@link Module}
	 * @return the ODE model for <code>module</code>
	 */
	public Model createODE(Module module) {
		if (module instanceof HasODE) {
			Model model = ((HasODE) module).createODE();
			if (model != null)
				return model;
		}
		return new RungeKutta(this);
	}

	/**
	 * Creates stochastic differential equation model for <code>module</code>.
	 * <p>
	 * <strong>Note:</strong> by default returns {@link SDE} model.
	 * Override to provide custom implementation of SDE model.
	 * 
	 * @param module the interaction {@link Module}
	 * @return the SDE model for <code>module</code>
	 */
	public Model createSDE(Module module) {
		if (module instanceof HasSDE) {
			Model model = ((HasSDE) module).createSDE();
			if (model != null)
				return model;
		}
		return new SDE(this);
	}

	/**
	 * Creates partial differential equation model for <code>module</code>.
	 * <p>
	 * <strong>Note:</strong> by default returns {@link PDE} model.
	 * Override to provide custom implementation of PDE model.
	 * 
	 * @param module the interaction {@link Module}
	 * @return the PDE model for <code>module</code>
	 */
	public Model createPDE(Module module) {
		if (module instanceof HasPDE) {
			Model model = ((HasPDE) module).createPDE();
			if (model != null)
				return model;
		}
		return new PDE(this);
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
	public Model createIBS(Module module) {
		if (module instanceof IBS.HasIBS) {
			Model model = ((IBS.HasIBS) module).createIBS();
			if (model != null)
				return model;
		}
		if (module instanceof Continuous)
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
	 * registered {@link MilestoneListener}'s of any changes.
	 *
	 * @param type the type of {@link Model} to load
	 * @return <code>true</code> if model type changed
	 */
	public boolean loadModel(Type type) {
		if (activeModel != null) {
			if (activeModel.isType(type))
				return false;
			// unload previous model
			unloadModel();
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
		activeModule.setModel(activeModel);
		activeModel.load();
		fireModelLoaded();
		return true;
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
	 * The flag to indicate whether a reset of the active model has been requested.
	 * This is necessary after certain parameter changes. For example, changing the
	 * population size (see {@link IBSPopulation#nPopulation}) requires a reset to
	 * (re)generate population geometries and initialize traits.
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
	 * The flag to indicate whether the module reqiuires a full reparsing of the
	 * command line options.
	 */
	private boolean reparseCLO = false;

	/**
	 * Request full reparsing of the command line options. This is necessary if the
	 * command line options have changed in a fundamental way, for example if the
	 * number of traits in a module has changed.
	 */
	public void requestParseCLO() {
		reparseCLO = true;
	}

	/**
	 * List of engine listeners that get notified when the state of the population
	 * changed, for example after population reset or completed an update step.
	 */
	protected List<MilestoneListener> milestoneListeners = new ArrayList<MilestoneListener>();

	/**
	 * Add a milestone listener to the list of listeners that get notified when the
	 * model reaches milestones.
	 * 
	 * @param newListener the new milestone listener
	 */
	public void addMilestoneListener(MilestoneListener newListener) {
		milestoneListeners.add(0, newListener);
	}

	/**
	 * Remove the milestone listener from the list of listeners that get notified
	 * when the model reaches milestones.
	 * 
	 * @param obsoleteListener the listener to remove from list of milestone
	 *                         listeners
	 */
	public void removeMilestoneListener(MilestoneListener obsoleteListener) {
		milestoneListeners.remove(obsoleteListener);
	}

	/**
	 * List of change listeners that get notified when the model changes.
	 */
	protected List<ChangeListener> changeListeners = new ArrayList<ChangeListener>();

	/**
	 * Add a change listener to the list of listeners that get notified when the
	 * model changes.
	 * 
	 * @param newListener the new change listener
	 */
	public void addChangeListener(ChangeListener newListener) {
		changeListeners.add(0, newListener);
	}

	/**
	 * Remove the change listener from the list of listeners that get notified when
	 * the model changes.
	 * 
	 * @param obsoleteListener the listener to remove from the list of change
	 *                         listeners
	 */
	public void removeChangeListener(ChangeListener obsoleteListener) {
		changeListeners.remove(obsoleteListener);
	}

	/**
	 * Unload model framework. Notifies all registered
	 * {@link MilestoneListener}'s.
	 */
	public void unloadModel() {
		unloadModel(false);
	}

	/**
	 * Unload model framework and, if requested, notifies all registered
	 * {@link MilestoneListener}'s.
	 * 
	 * @param quiet set to {@code true} to skip notifying listeners
	 */
	public void unloadModel(boolean quiet) {
		if (activeModel == null)
			return;
		// remove CLO parsers that are no longer needed
		removeCLOProvider(activeModel);
		activeModel.unload();
		if (!quiet)
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
		if (Math.abs(activeModel.getTimeStop()) < 1e-8)
			setSuspended(false);
		// check module first; model may contact module
		boolean doReset = false;
		for (Module mod : activeModule.getSpecies())
			doReset |= mod.check();
		Type type = activeModel.getType();
		doReset |= activeModel.check();
		Type newtype = activeModel.getType();
		if (newtype != type) {
			// model type changed; update model type in clo and parse again
			String[] splitclo = getSplitCLO();
			int idx = 0;
			for (String o : splitclo) {
				if (o.startsWith("model")) {
					splitclo[idx] = "model " + newtype.getKey();
					break;
				}
				idx++;
			}
			parseCLO(splitclo);
			return modelCheck();
		}
		return doReset;
	}

	/**
	 * Reset all populations and notify all listeners.
	 */
	public final void modelReset() {
		modelReset(false);
	}

	/**
	 * Reset all populations and notify all listeners if requested.
	 * <p>
	 * <strong>Note:</strong> if {@code quiet == true} statistics are preserved.
	 * 
	 * @param quiet set to {@code true} to skip notifying listeners
	 */
	public final void modelReset(boolean quiet) {
		if (activeModel == null)
			return;
		// reset random number generator if seed was specified
		if (rng.isRNGSeedSet())
			rng.setRNGSeed();
		// check consistency of parameters in models
		modelCheck();
		for (Module mod : activeModule.getSpecies())
			mod.reset();
		activeModel.reset();
		resetRequested = false;
		modelInit(true);
		if (!quiet) {
			// notify of reset
			activeModel.resetStatisticsSample();
			fireModelReset();
		}
	}

	/**
	 * Initialize all populations and notify all listeners.
	 */
	public final void modelInit() {
		modelInit(false);
	}

	/**
	 * Initialize all populations and notify all listeners if requested. In contrast
	 * to {@link #modelReset(boolean)} this method does not re-generate the
	 * population structures.
	 * 
	 * @param quiet set to {@code true} to skip notifying listeners
	 */
	public final void modelInit(boolean quiet) {
		for (Module mod : activeModule.getSpecies())
			mod.init();
		activeModel.init();
		activeModel.update();
		resetCPUSample();
		modelRelax(quiet);
		if (!quiet) {
			// notify of init
			fireModelInit();
		}
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
	 * Relax model by {@code timeRelax} steps and notify all listeners when done. If
	 * model converged during relaxation notify listeners that model has stopped.
	 *
	 * @return <code>true</code> if converged
	 * 
	 * @see Model#relax()
	 */
	public final boolean modelRelax() {
		return modelRelax(false);
	}

	/**
	 * Relax model by {@code timeRelax} steps and notify all listeners if requested.
	 *
	 * @param quiet set to {@code true} to skip notifying listeners
	 * 
	 * @return <code>true</code> if converged
	 * 
	 * @see Model#relax()
	 */
	public final boolean modelRelax(boolean quiet) {
		if (activeModel.getTimeRelax() < 1.0)
			return activeModel.hasConverged();
		boolean converged = activeModel.relax();
		if (!quiet) {
			if (converged)
				fireModelStopped();
			else
				fireModelRelaxed();
		}
		return converged;
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
		if (activeModel.useScheduling()) {
			activeModel.next();
			return true;
		}
		return modelNextDone(activeModel.next());
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
		endCPUSample();
		if (!cont) {
			fireModelStopped();
			return false;
		}
		fireModelChanged();
		return true;
	}

	/**
	 * Called after parameters have changed. Checks new settings and resets
	 * population(s) (and/or GUI) if necessary.
	 *
	 * @return <code>true</code> if reset was necessary
	 */
	public boolean paramsDidChange() {
		if (resetRequested || modelCheck()) {
			modelReset();
			return true;
		}
		activeModel.update();
		fireSettingsChanged();
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
	 * Get the command line options split into an array with option names followed
	 * by their arguments (if applicable).
	 * 
	 * @return array command line options and arguments
	 */
	public String[] getSplitCLO() {
		if (clo == null)
			return null;
		// strip all whitespace at start and end
		String[] args = clo.trim().split("\\s+--");
		// strip '--' from first argument
		if (args[0].startsWith("--"))
			args[0] = args[0].substring(2);
		return args;
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
		if (isSuspended)
			isRunning = false;
	}

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
		fireModuleLoaded();
		return activeModule;
	}

	/**
	 * Unload current module to free up resources.
	 * 
	 * <h3>Implementation note:</h3>
	 * Called from {@link loadModule} to first unload the active module or triggered
	 * by GWT's {@link org.evoludo.EvoLudoWeb#onUnload()}, i.e. when unloading the
	 * GWT application.
	 */
	public void unloadModule() {
		unloadModel(true);
		if (activeModule != null) {
			removeCLOProvider(activeModule);
			for (Iterator<? extends Module> it = activeModule.getSpecies().iterator(); it.hasNext();) {
				Module mod = it.next();
				mod.unload();
				if (mod != activeModule)
					it.remove();
			}
			activeModule = null;
		}
		fireModuleUnloaded();
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
		if (isSuspended())
			run();
	}

	/**
	 * Requests halting of a running {@link Model} on the next opportunity.
	 */
	public void stop() {
		if (isRunning)
			requestAction(PendingAction.STOP);
	}

	/**
	 * Start the EvoLudo model and calculate the dynamics one step at a time.
	 */
	public abstract void run();

	/**
	 * Advances the EvoLudo model by a single step. Called when pressing the 'Step'
	 * button, the 'n' or 'right-arrow' key.
	 */
	public abstract void next();

	/**
	 * Start or stop EvoLudo model. If model is running wait until next update
	 * is completed to prevent unexpected side effects.
	 */
	public void startStop() {
		if (isRunning) {
			requestAction(PendingAction.STOP);
			return;
		}
		if (activeModel.getMode() == Mode.STATISTICS_SAMPLE) {
			fireModelRunning();
			next();
		} else
			run();
	}

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
		if (!activeModel.permitsTimeReversal() || activeModel.getMode() != Mode.DYNAMICS)
			return;
		activeModel.setTimeReversed(!activeModel.isTimeReversed());
		doPrev = true;
		// next may return immediately - must reverse time again in modelNextDone()!
		next();
	}

	/**
	 * Advances the EvoLudo model by a single debugging step. Called when pressing
	 * the 'Debug' button or 'D' key.
	 */
	public void debug() {
		if (!activeModel.permitsDebugStep())
			return;
		// temporarily change to finer logging level for debugging
		Level logLevel = logger.getLevel();
		logger.setLevel(Level.ALL);
		activeModel.debugStep();
		logger.setLevel(logLevel);
	}

	/**
	 * Run simulation. Currently only implemented by EvoLudoJRE. Returns control to
	 * caller only if the arguments in {@code clo} do not match requirements for
	 * running a simulation.
	 * 
	 * @see #setCLO(String)
	 * @see org.evoludo.simulator.EvoLudoJRE#simulation()
	 */
	public void simulation() {
	}

	/**
	 * The action that is pending (if any).
	 */
	protected PendingAction pendingAction = PendingAction.NONE;

	/**
	 * Requests a {@link PendingAction} to be processed on the next opportunity. If
	 * model is not running the action is processed immediately.
	 * 
	 * @param action the action requested
	 */
	public synchronized void requestAction(PendingAction action) {
		requestAction(action, !isRunning);
	}

	/**
	 * Requests a {@link PendingAction} to be processed on the next opportunity. If
	 * model is not running or {@code now} is set to {@code true} the action is
	 * processed immediately.
	 * 
	 * @param action the action requested
	 * @param now    <code>true</code> to processes action immediately
	 */
	public synchronized void requestAction(PendingAction action, boolean now) {
		if (pendingAction != PendingAction.STOP)
			pendingAction = action;
		// if now process request immediately
		if (now) {
			processPendingAction();
		}
	}

	/**
	 * Called whenever a new module has finished loading. Notifies all registered
	 * {@link MilestoneListener}s.
	 */
	public synchronized void fireModuleLoaded() {
		pendingAction = PendingAction.NONE;
		for (MilestoneListener i : milestoneListeners)
			i.moduleLoaded();
		String authors = activeModule.getAuthors();
		logger.info("Module loaded: " + activeModule.getTitle() + (authors == null ? "" : " by " + authors));
	}

	/**
	 * Called whenever the current module has finished unloading. Notifies all
	 * registered {@link MilestoneListener}s.
	 */
	public synchronized void fireModuleUnloaded() {
		for (MilestoneListener i : milestoneListeners)
			i.moduleUnloaded();
		if (activeModule != null)
			logger.info("Module '" + activeModule.getTitle() + "' unloaded");
		activeModule = null;
	}

	/**
	 * Called after the state of the model has been restored either through
	 * drag'n'drop with the GWT GUI or through the <code>--restore</code> command
	 * line argument. Notifies all registered {@link MilestoneListener}s.
	 *
	 * @see #restoreFromFile()
	 * @see #restoreState(Plist)
	 */
	public synchronized void fireModuleRestored() {
		for (MilestoneListener i : milestoneListeners)
			i.moduleRestored();
		logger.info("Module restored");
	}

	/**
	 * Called whenever a new model has finished loading. Notifies all registered
	 * {@link MilestoneListener}s.
	 */
	public synchronized void fireModelLoaded() {
		pendingAction = PendingAction.NONE;
		for (MilestoneListener i : milestoneListeners)
			i.modelLoaded();
		logger.info("Model '" + activeModel.getType() + "' loaded");
	}

	/**
	 * Called whenever a new model has finished loading. Notifies all registered
	 * {@link MilestoneListener}s.
	 */
	public synchronized void fireModelUnloaded() {
		pendingAction = PendingAction.NONE;
		for (MilestoneListener i : milestoneListeners)
			i.modelUnloaded();
		if (activeModel != null)
			logger.info("Model '" + activeModel.getType() + "' unloaded");
	}

	/**
	 * Called whenever the model starts its calculations. Fires only when starting
	 * to run. Notifies all registered {@link MilestoneListener}s.
	 */
	public synchronized void fireModelRunning() {
		if (isRunning)
			return;
		isRunning = true;
		isSuspended = false;
		for (MilestoneListener i : milestoneListeners)
			i.modelRunning();
		logger.info("Model running");
	}

	/**
	 * Called whenever the state of the model has changed. For example, to
	 * trigger the update of the state displayed in the GUI. Processes pending
	 * actions and notifies all registered {@code Model.MilestoneListener}s.
	 * 
	 * @see MilestoneListener
	 * @see PendingAction
	 */
	public synchronized void fireModelChanged() {
		switch (activeModel.getMode()) {
			default:
			case STATISTICS_UPDATE:
			case DYNAMICS:
				processPendingAction();
				break;
			case STATISTICS_SAMPLE:
				break;
		}
	}

	/**
	 * Called whenever the settings of the model have changed. For example, to
	 * trigger the range of values or markers in the GUI. Notifies all registered
	 * {@link MilestoneListener}s.
	 * 
	 * @see MilestoneListener
	 */
	public synchronized void fireSettingsChanged() {
		for (MilestoneListener i : milestoneListeners)
			i.modelSettings();
	}

	/**
	 * Helper method for handling model changed events and processes pending
	 * actions.
	 * 
	 * @see MilestoneListener
	 * @see PendingAction
	 */
	void processPendingAction() {
		PendingAction action = pendingAction;
		pendingAction = PendingAction.NONE;
		switch (action) {
			case CHANGE_MODE:
				Mode mode = action.mode;
				if (activeModel.setMode(mode)) {
					// mode changed
					if (mode == Mode.STATISTICS_SAMPLE) {
						// do not notify listeners, preserve statistics
						modelReset(true);
					} else {
						// stop running
						isRunning = false;
					}
				} else {
					// mode unchanged
					if (!isRunning || mode != Mode.STATISTICS_SAMPLE)
						break;
					// continue running if mode unchanged
					action = PendingAction.STATISTIC_READY;
				}
				//$FALL-THROUGH$
			case NONE:
			case STATISTIC_FAILED:
			case STATISTIC_READY:
				for (ChangeListener i : changeListeners)
					i.modelChanged(action);
				break;
			case STOP: // stop requested (as opposed to simulations that stopped)
				isRunning = false;
				fireModelStopped();
				break;
			case INIT:
				modelInit();
				break;
			case RESET:
				modelReset();
				break;
			case SHUTDOWN:
				unloadModule();
				break;
			default:
		}
	}

	/**
	 * Called after the model has been re-initialized. Notifies all registered
	 * {@link MilestoneListener}s.
	 */
	public synchronized void fireModelInit() {
		if (activeModel.getMode() == Mode.DYNAMICS || !isRunning) {
			isRunning = false;
			for (MilestoneListener i : milestoneListeners)
				i.modelDidInit();
			logger.info("Model init");
		}
	}

	/**
	 * Called after the model has been reset. Notifies all registered
	 * {@link MilestoneListener}s.
	 */
	public synchronized void fireModelReset() {
		isRunning = false;
		if (activeModel == null)
			return;
		for (MilestoneListener i : milestoneListeners)
			i.modelDidReset();
		logger.info("Model reset");
	}

	/**
	 * Called after the model completed its relaxation. Notifies all registered
	 * {@link MilestoneListener}s.
	 */
	public synchronized void fireModelRelaxed() {
		for (MilestoneListener i : milestoneListeners)
			i.modelRelaxed();
		logger.info("Model relaxed");
	}

	/**
	 * Called after the population has reached an absorbing state (or has converged
	 * to an equilibrium state). Notifies all registered
	 * {@link MilestoneListener}s.
	 */
	public synchronized void fireModelStopped() {
		// model may already have been unloaded
		if (activeModel == null)
			return;
		switch (activeModel.getMode()) {
			case DYNAMICS:
			case STATISTICS_UPDATE:
			default:
				isRunning = false;
				for (MilestoneListener i : milestoneListeners)
					i.modelStopped();
				logger.info("Model stopped");
				break;
			case STATISTICS_SAMPLE:
				// check if new sample completed
				activeModel.readStatisticsSample();
				// note: calling fireModelChanged doesn't work because STATISTICS_SAMPLE
				// prevents firing
				if (pendingAction == PendingAction.NONE)
					pendingAction = PendingAction.STATISTIC_READY;
				processPendingAction();
				break;
		}
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
	 * @see org.evoludo.simulator.EvoLudoGWT#hirePDESupervisor(org.evoludo.simulator.models.PDE)
	 *      EvoLudoGWT#hirePDESupervisor(PDERD)
	 * @see org.evoludo.simulator.EvoLudoJRE#hirePDESupervisor(org.evoludo.simulator.models.PDE)
	 *      EvoLudoJRE#hirePDESupervisor(PDERD)
	 * @see org.evoludo.simulator.models.PDESupervisor
	 * @see org.evoludo.simulator.models.PDESupervisorGWT
	 * @see org.evoludo.simulator.models.PDESupervisorJRE
	 */
	public abstract PDESupervisor hirePDESupervisor(PDE charge);

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
	 * The copyright string.
	 */
	public static final String COPYRIGHT = "\u00a9 Christoph Hauert"; // \u00a9 UTF-8 character code for ©

	/**
	 * Return version string of current model. Version must include reference to git
	 * commit to ensure reproducibility of results.
	 *
	 * @return the version string
	 */
	public String getVersion() {
		String git = getGit();
		if ("unknown".equals(git))
			return COPYRIGHT;
		return "EvoLudo: " + git + " " + COPYRIGHT;
	}

	/**
	 * Get version of JRE (if not running in browser).
	 * 
	 * @return the java version string
	 */
	public String getJavaVersion() {
		return null;
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
	 * Report all parameter settings to <code>output</code> (currently JRE only).
	 */
	public void dumpParameters() {
	}

	/**
	 * Concluding words for report (currently JRE only).
	 */
	public void dumpEnd() {
	}

	/**
	 * Export the current state of the engine using the appropriate means available
	 * in the current environment (GWT/JRE).
	 * 
	 * @see org.evoludo.simulator.EvoLudoGWT#exportState()
	 * @see org.evoludo.simulator.EvoLudoJRE#exportState()
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
		String java = getJavaVersion();
		if (java != null)
			plist.append(Plist.encodeKey("JavaVersion", java));
		plist.append(Plist.encodeKey("CLO", parser.getCLO()));
		activeModel.encodeState(plist);
		// the mersenne twister state is pretty long (and uninteresting) keep at end
		plist.append("<key>RNG state</key>\n" + "<dict>\n" + (rng.getRNG().encodeState()) + "</dict>\n");
		plist.append("</dict>\n" + "</plist>");
		return plist.toString();
	}

	/**
	 * Restore state of EvoLudo model from saved plist, which encodes engine state.
	 * 
	 * @return {@code true} on successfully restoring state
	 * @see #encodeState()
	 */
	public abstract boolean restoreFromFile();

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
	 * @return {@code true} on successfully restoring state
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
		// version check
		int versionstart = version.indexOf(' ') + 1;
		String restoreGit = version.substring(versionstart, version.indexOf(' ', versionstart));
		String myVersion = getVersion();
		versionstart = myVersion.indexOf(' ') + 1;
		String myGit = myVersion.substring(versionstart, myVersion.indexOf(' ', versionstart));
		if (!myGit.equals(restoreGit))
			// versions differ - may or may not be a problem...
			// note: dirty states always count as being different
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
			fireModuleRestored();
		} else {
			logger.warning("restore failed - resetting model.");
			modelReset();
		}
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
	 * <code>--verbose</code> or <code>--restore</code>.
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
		int nParams = cloarray.length;
		// first, deal with --help option
		boolean helpRequested = false;
		String helpName = cloHelp.getName();
		for (int i = 0; i < nParams; i++) {
			String param = cloarray[i];
			if (param.startsWith(helpName)) {
				helpRequested = true;
				break;
			}
		}
		// now deal with --module option
		String moduleParam = cloModule.getName();
		CLOption.Key moduleKey = null;
		String moduleName = "<missing>";
		for (int i = 0; i < nParams; i++) {
			String param = cloarray[i];
			if (param.startsWith(moduleParam)) {
				String[] moduleArgs = param.split("[\\s+,=]");
				if (moduleArgs == null || moduleArgs.length < 2) {
					if (!helpRequested)
						logger.severe("module key missing");
					return null;
				}
				moduleName = moduleArgs[1];
				moduleKey = cloModule.match(moduleName);
				// exact match required
				if (moduleKey != null && !moduleKey.getKey().equals(moduleName))
					moduleKey = null;
				// module parameter found; no need to continue
				cloarray = ArrayMath.drop(cloarray, i--);
				nParams--;
				break;
			}
		}
		if (moduleKey == null || loadModule(moduleKey.getKey()) == null) {
			if (!helpRequested)
				logger.severe("Module '" + moduleName + "' not found!");
			return null;
		}
		// second, determine feasible --model options for given module
		cloModel.clearKeys();
		cloModel.addKeys(activeModule.getModelTypes());
		// third, deal with --model option
		String modelName = cloModel.getName();
		// if IBS is not an option, pick first available model as default (which one
		// remains unspecified)
		Collection<Key> keys = cloModel.getKeys();
		if (keys.isEmpty()) {
			if (!helpRequested)
				logger.severe("No model found!");
			return null;
		}
		// if IBS not an option, pick first model type as default
		Type defaulttype = (keys.contains(Type.IBS) ? Type.IBS : Type.values()[0]);
		Type type = null;
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
				type = Type.parse(newModel);
				if (type == null || !keys.contains(type)) {
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
				cloVerbose.setArg(verbosity);
				cloVerbose.parse();
				// verbosity key found; no need to continue
				break;
			}
		}
		if (helpRequested)
			return null;
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
		return parseCLO(getSplitCLO());
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
	protected boolean parseCLO(String[] cloarray) {
		parser.setLogger(logger);
		cloarray = preprocessCLO(cloarray);
		parser.initCLO();
		// preprocessing removed (and possibly altered) --module and --model options
		// add current settings back to cloarray
		if (activeModule != null)
			cloarray = ArrayMath.append(cloarray, cloModule.getName() + " " + activeModule.getKey());
		if (activeModel != null)
			cloarray = ArrayMath.append(cloarray, cloModel.getName() + " " + activeModel.getType().getKey());
		boolean success = parser.parseCLO(cloarray);
		if (reparseCLO) {
			// start again from scratch
			reparseCLO = false;
			parser.initCLO();
			return parser.parseCLO(cloarray);
		}
		return success;
	}

	/**
	 * Format, encode and output help on command line options.
	 * 
	 * @return the help string
	 */
	public String getCLOHelp() {
		// list trait indices and names
		String globalMsg = "List of command line options";
		if (activeModule != null) {
			globalMsg += " for module '" + activeModule.getKey() + "'";
			if (activeModel != null) {
				globalMsg += " and model '" + activeModel.getType().getKey() + "'";
				catModel.setHeader("Options for model '" + activeModel.getType().getKey() + "'");
			} else
				globalMsg += " (select model for more options)";
			globalMsg += "\n\nGlobal options:";

			int idx = 0;
			String moduleMsg = "";
			for (Module mod : activeModule.getSpecies()) {
				String name = mod.getName();
				int namelen = name.length();
				if (namelen > 0)
					moduleMsg += "\n       Species: " + name;
				int nt = mod.getNTraits();
				for (int n = 0; n < nt; n++)
					moduleMsg += "\n             " + (idx + n) + ": " + mod.getTraitName(n);
				idx += nt;
			}
			catModule.setHeader("Options for module '" + activeModule.getKey() //
					+ "' with trait indices and names:" + moduleMsg);
		} else
			globalMsg += " (select module and model for more info):";
		catGlobal.setHeader(globalMsg);
		return parser.helpCLO(true);
	}

	/**
	 * Show help on command line options.
	 */
	public abstract void showHelp();

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
	 * Command line option to set the type of model (see {@link Type}).
	 */
	public final CLOption cloModel = new CLOption("model", Type.IBS.getKey(), catModule,
			"--model <m>     model type", new CLODelegate() {
				@Override
				public boolean parse(String arg) {
					// option gets special treatment
					return true;
				}

				@Override
				public void report(PrintStream output) {
					output.println("# model:                " + activeModel.getType());
				}
			});

	/**
	 * Command line option to set seed of random number generator.
	 */
	public final CLOption cloSeed = new CLOption("seed", "no seed", CLOption.Argument.OPTIONAL, catModel,
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
	public final CLOption cloDelay = new CLOption("delay", "" + DELAY_INIT, catGUI, // DELAY_INIT
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
	 * Command line option to set the color for trajectories. For example, this
	 * affects the display in {@link org.evoludo.simulator.views.S3} or
	 * {@link org.evoludo.simulator.views.Phase2D}.
	 */
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
	 * Command line option to print help message for available command line options.
	 */
	public final CLOption cloHelp = new CLOption("help", catGlobal,
			"--help          print this help screen", new CLODelegate() {
				@Override
				public boolean parse(String arg) {
					if (cloHelp.isSet()) {
						showHelp();
					}
					return true;
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
		parser.addCLO(cloHelp);
		parser.addCLO(cloVerbose);
		if (!EvoLudo.isGWT)
			// default verbosity if running as java application is warning
			cloVerbose.setDefault("warning");
		parser.addCLO(cloModule);
		parser.addCLO(cloModel);
		parser.addCLO(cloSeed);
		parser.addCLO(cloRun);
		parser.addCLO(cloDelay);
		parser.addCLO(cloRNG);
		// option for trait color schemes only makes sense for modules with multiple
		// continuous traits that have 2D/3D visualizations
		if (activeModel instanceof org.evoludo.simulator.models.Continuous //
				&& activeModule.getNTraits() > 1 //
				&& (activeModule instanceof HasPop2D || activeModule instanceof HasPop3D)) {
			parser.addCLO(cloTraitColorScheme);
			cloTraitColorScheme.addKeys(ColorModelType.values());
		}

		// trajectory color settings used by phase plane and simplex plots
		if (activeModule instanceof HasS3 || activeModule instanceof HasPhase2D)
			parser.addCLO(cloTrajectoryColor);
	}

	/**
	 * The coloring method type.
	 */
	protected ColorModelType colorModelType = ColorModelType.DEFAULT;

	/**
	 * Coloring methods for multiple continuous traits. Enum on steroids. Currently
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
		 * @see EvoLudo#getCLOHelp()
		 */
		private final String title;

		/**
		 * Create a new color model type.
		 * 
		 * @param key   the name of the color model
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

	/**
	 * Report error and stop model execution, if running.
	 * <p>
	 * <strong>IMPORTANT:</strong> Does not return control to caller.
	 * 
	 * @param msg the error message
	 */
	public void fatal(String msg) {
		// stops any ongoing calculations as soon as possible
		pendingAction = PendingAction.STOP;
		if (isRunning) {
			// calling stop() doesn't work because PendingAction.STOP
			// is never processed; do it manually
			fireModelStopped();
			pendingAction = PendingAction.NONE;
		}
		logger.severe(msg);
		if (isGWT)
			throw new Error(msg);
	}

	/**
	 * Constructor to instantiate a new EvoLudo controller. If
	 * {@code loadModules == true}, load all available modules. Otherwise a
	 * specific module needs to be loaded by the caller.
	 * 
	 * @param loadModules the flag to indicate whether to load modules
	 */
	protected EvoLudo(boolean loadModules) {
		logger = Logger.getLogger(EvoLudo.class.getName() + "-" + ID);
		parser = new CLOParser(this);
		if (!loadModules)
			return;
		// load all available modules
		addModule(new Moran(this));
		addModule(new TBT(this));
		addModule(new ATBT(this));
		addModule(new RSP(this));
		addModule(new CDL(this));
		addModule(new CDLP(this));
		addModule(new CDLPQ(this));
		addModule(new Centipede(this));
		addModule(new CSD(this));
		addModule(new CLabour(this));
		addModule(new DemesTBT(this));
		addModule(new EcoPGG(this));
		addModule(new NetGames(this));
		addModule(new SIR(this));
	}
}
