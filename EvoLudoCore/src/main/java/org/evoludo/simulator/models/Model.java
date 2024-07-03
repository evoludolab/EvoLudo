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

package org.evoludo.simulator.models;

import java.awt.Color;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.evoludo.math.RNGDistribution;
import org.evoludo.simulator.ColorMap;
import org.evoludo.simulator.EvoLudo;
import org.evoludo.simulator.Geometry;
import org.evoludo.simulator.models.ChangeListener.PendingAction;
import org.evoludo.simulator.modules.Module;
import org.evoludo.util.CLOProvider;
import org.evoludo.util.CLOption;
import org.evoludo.util.Formatter;
import org.evoludo.util.Plist;

/**
 * Interface for EvoLudo models to interact with {@link Module}s, which define
 * interactions (or games), and the engine {@link EvoLudo}, which manages the
 * (un)loading and execution of different models based on what model types the
 * module supports.
 *
 * @author Christoph Hauert
 */
public abstract class Model implements CLOProvider, Statistics {

	/**
	 * The pacemaker of all models. Interface with the outside world.
	 */
	protected EvoLudo engine;

	/**
	 * Logger for keeping track of and reporting events and issues.
	 */
	protected Logger logger;

	/**
	 * The shared random number generator to ensure reproducibility of results.
	 * 
	 * @see EvoLudo#getRNG()
	 */
	protected RNGDistribution rng;

	/**
	 * Keeps track of the time elapsed. Time is measured in number of generations.
	 * In IBS models this corresponds to one Monte-Carlo step, such that in a
	 * population of size <code>N</code> one generation corresponds to
	 * <code>N</code> updates, which translates to <code>N</code> events (birth,
	 * death, imitation, etc.).
	 * 
	 * <strong>Notes:</strong>
	 * <ol>
	 * <li><code>generation==0</code> after {@link #reset()} and at the beginning of
	 * a simulation run.
	 * <li><code>generation</code> is incremented <em>before</em> the next event is
	 * processed, to reflect the time at which the event occurs.
	 * <li>generally differs from 'real time'.
	 * <li>may be negative for models that admit time reversal (e.g. integrating ODE
	 * backwards).
	 * </ol>
	 * 
	 * @see #permitsTimeReversal()
	 * @see IBS#realtime
	 */
	protected double time = -1.0;

	/**
	 * Short-cut to the list of species modules. Convenience field.
	 */
	protected ArrayList<? extends Module> species;

	/**
	 * Return the species with ID <code>id</code>.
	 *
	 * @param id the species identifier
	 * @return the species
	 */
	public Module getSpecies(int id) {
		return species.get(id);
	}

	/**
	 * The number of species in multi-species models.
	 */
	protected int nSpecies;

	/**
	 * Return the number of species in this model.
	 *
	 * @return the number of species
	 */
	public int getNSpecies() {
		return nSpecies;
	}

	/**
	 * Flag to indicate whether the model entertains multiple species, i.e.
	 * {@code nSpecies&gt;1}. Convenience field.
	 */
	protected boolean isMultispecies;

	/**
	 * Flag to indicate whether the model has converged. Once a model has converged
	 * the model execution automatically stops.
	 */
	protected boolean converged = false;

	/**
	 * Checks if model has converged.
	 *
	 * @return <code>true</code> if model has converged.
	 *
	 * @see #checkConvergence(double)
	 * @see #setAccuracy(double)
	 */
	public boolean hasConverged() {
		return converged;
	}

	/**
	 * Creates a model.
	 * 
	 * @param engine the pacemaker for running the model
	 */
	public Model(EvoLudo engine) {
		this.engine = engine;
		logger = Logger.getLogger(getClass().getName());
	}

	/**
	 * Indicates current mode of IBS model.
	 */
	protected Mode mode = Mode.DYNAMICS;

	/**
	 * Request a change of the {@link Mode} of the model. Returns {@code false} if
	 * {@code mode} is not supported.
	 * 
	 * @param aMode the requested mode
	 * @return {@code true} if mode supported
	 * 
	 * @see EvoLudo#requestAction(ChangeListener.PendingAction)
	 */
	public boolean requestMode(Mode newmode) {
		if (!permitsMode(newmode))
			return false;
		PendingAction.MODE.mode = newmode;
		engine.requestAction(PendingAction.MODE);
		return true;
	}

	/**
	 * Check if current model implements mode {@code test}; by default only
	 * {@link Mode#DYNAMICS} is permitted.
	 *
	 * @param test the mode to test
	 * @return {@code true} if {@code test} is available in current model
	 */
	public boolean permitsMode(Mode test) {
		switch (test) {
			case STATISTICS_SAMPLE:
				return permitsSampleStatistics();
			case STATISTICS_UPDATE:
				return permitsUpdateStatistics();
			case DYNAMICS:
			default:
				return true;
		}
	}

	/**
	 * Sets the {@link Mode} of model/simulator. Returns {@code false} if
	 * {@code mode} is not supported.
	 * <p>
	 * <strong>Note:</strong> Do not set mode directly. Changes of the execution
	 * mode should be coordinated by the engine through requests.
	 *
	 * @param mode change mode of model to <code>mode</code>
	 * @return {@code true} if mode changed
	 *
	 * @see #requestMode(Mode)
	 */
	public boolean setMode(Mode mode) {
		if (!permitsMode(mode))
			return false;
		boolean changed = (this.mode != mode);
		this.mode = mode;
		return changed;
	}

	/**
	 * Gets the {@link Mode} of the model.
	 *
	 * @return mode of model
	 */
	public Mode getMode() {
		return mode;
	}


	/**
	 * Common interface for all models with discrete strategy sets.
	 */
	public interface Discrete {

		/**
		 * Calculate and return the payoff/score of individuals in monomorphic
		 * populations with trait/strategy {@code type} but also deals with payoff
		 * accounting (averaged versus accumulated).
		 *
		 * @param id   the id of the population for multi-species models
		 * @param type trait/strategy
		 * @return payoff/score in monomorphic population with trait/strategy
		 *         {@code type}. Returns {@code NaN} if scores ill defined
		 * 
		 * @see org.evoludo.simulator.modules.Discrete#getMonoGameScore(int)
		 */
		public default double getMonoScore(int id, int type) {
			return Double.NaN;
		}
	}

	/**
	 * Common interface for all models with continuous strategy sets.
	 */
	public interface Continuous {

		/**
		 * Gets the minimum trait values in this module.
		 * 
		 * @param id the id of the population for multi-species models
		 * @return the array with the minimum trait values
		 */
		public double[] getTraitMin(int id);

		/**
		 * Gets the maximum trait values in this module.
		 * 
		 * @param id the id of the population for multi-species models
		 * @return the array with the maximum trait values
		 */
		public double[] getTraitMax(int id);

		/**
		 * Calculates and returns minimum score in monomorphic population. This depends
		 * on the payoff accounting (averaged versus accumulated) as well as the
		 * {@link Geometry}. Since modules are agnostic of runtime details, the request
		 * is simply forwarded to the current {@link Model} together with the species ID
		 * for multi-species modules.
		 * 
		 * @param id the id of the population for multi-species models
		 * @return the minimum monomorphic score
		 */
		public double getMinMonoScore(int id);

		/**
		 * Calculates and returns maximum score in monomorphic population. This depends
		 * on the payoff accounting (averaged versus accumulated) as well as the
		 * {@link Geometry}. Since modules are agnostic of runtime details, the request
		 * is simply forwarded to the current {@link Model} together with the species ID
		 * for multi-species modules.
		 * 
		 * @param id the id of the population for multi-species models
		 * @return the maximum monomorphic score
		 */
		public double getMaxMonoScore(int id);

		/**
		 * Gets the histogram of the trait distributions and returns the data in an
		 * array <code>bins</code>, where the first index denotes the trait (in case
		 * there are multiple) and the second index refers to the bins in the histogram.
		 * <p>
		 * This is a helper method to forward the request to the appropriate
		 * {@link IBSMCPopulation} (for multiple traits) or {@link IBSCPopulation} (for
		 * single traits).
		 *
		 * @param id   the id of the population for multi-species models
		 * @param bins the 2D data array for storing the histogram
		 */
		public void getTraitHistogramData(int id, double[][] bins);

		/**
		 * Gets the histogram of the trait distribution for the traits of the
		 * {@link Module}. For modules with multiple traits a 2D histogram is generated
		 * for traits <code>trait1</code> and <code>trait2</code>. The histogram is
		 * returned in the linear array <code>bins</code> and arranged in a way that is
		 * compatible with square lattice geometries for visualization by
		 * {@link org.evoludo.simulator.views.Distribution} (GWT only). For modules with
		 * a single trait only, <code>trait1</code> and <code>trait2</code> are ignored.
		 *
		 * @param id     the id of the population for multi-species models
		 * @param bins   the data array for storing the histogram
		 * @param trait1 the index of the first trait (horizontal axis)
		 * @param trait2 the index of the second trait (vertical axis)
		 * 
		 * @see org.evoludo.simulator.Geometry#initGeometrySquare()
		 */
		public void get2DTraitHistogramData(int id, double[] bins, int trait1, int trait2);
	}

	/**
	 * Model types that modules may support. Currently available model types are:
	 * <dl>
	 * <dt>IBS</dt>
	 * <dd>individual based simulations</dd>
	 * <dt>ODE</dt>
	 * <dd>ordinary differential equations</dd>
	 * <dt>SDE</dt>
	 * <dd>stochastic differential equations</dd>
	 * <dt>PDE</dt>
	 * <dd>partial differential equations</dd>
	 * </dl>
	 */
	public static enum Type implements CLOption.Key {
		/**
		 * Individual based simulation model
		 */
		IBS("IBS", "individual based simulations"),

		/**
		 * Ordinary differential equation model
		 */
		ODE("ODE", "ordinary differential equations"),

		/**
		 * Stochastic differential equation model
		 */
		SDE("SDE", "stochastic differential equations"),

		/**
		 * Partial differential equation model
		 */
		PDE("PDE", "partial differential equations");

		/**
		 * Identifying key of the model type.
		 */
		String key;

		/**
		 * Title/description of the model type.
		 */
		String title;

		/**
		 * Construct an enum for model type.
		 * 
		 * @param key   the identifying key of the model
		 * @param title the title/description of the model
		 */
		Type(String key, String title) {
			this.key = key;
			this.title = title;
		}

		/**
		 * Parse the string <code>arg</code> and return the best matching model type.
		 * 
		 * @param arg the string to match with a model type
		 * @return the best matching model type
		 */
		public static Type parse(String arg) {
			int best = 0;
			Type match = null;
			for (Type t : values()) {
				int diff = CLOption.differAt(arg, t.key);
				if (diff > best) {
					best = diff;
					match = t;
				}
			}
			return match;
		}

		@Override
		public String toString() {
			return key + ": " + title;
		}

		@Override
		public String getKey() {
			return key;
		}

		@Override
		public String getTitle() {
			return title;
		}
	}

	/**
	 * Checks if model deals with continuous traits.
	 * 
	 * @return <code>true</code> if traits are continuous
	 */
	public boolean isContinuous() {
		return this instanceof Continuous;
	}

	/**
	 * Gets the type of this model.
	 * 
	 * @return the type of model
	 */
	public abstract Type getModelType();

	/**
	 * Modes of the model. Currently thefollowing modes are supported:
	 * <dl>
	 * <dt>DYNAMICS
	 * <dd>follow the time series of the model. This is the default.
	 * <dt>STATISTICS_SAMPLE
	 * <dd>generate samples to create statistics of the model. Run model until it
	 * stops and advertise that a new data point is available. Start next sample,
	 * once the data is retrieved and processed.
	 * <dt>STATISTICS_UPDATE
	 * <dd>generate samples from single run to create statistics of the
	 * model reflecting the different states of the population.
	 * </dl>
	 */
	public static enum Mode {
		/**
		 * Dynamics: follow the time series of the model.
		 */
		DYNAMICS("dynamics"), //

		/**
		 * Statistics: generate samples to create statistics of the model. Run model
		 * until it stops and advertise that a new data point is available. Start
		 * next sample, once the data is retrieved and processed.
		 */
		STATISTICS_SAMPLE("statistics_sample"), //

		/**
		 * Statistics: generate samples from single run to create statistics of the
		 * model reflecting the different states of the population.
		 */
		STATISTICS_UPDATE("statistics_update"); //

		/**
		 * Identifying id of the type of mode.
		 */
		String id;

		/**
		 * Construct an enum for the type of mode.
		 * 
		 * @param id the identifier of the mode
		 */
		Mode(String id) {
			this.id = id;
		}

		@Override
		public String toString() {
			return id;
		}
	}

	/**
	 * Data types that are handled by the model. Currently the following data types
	 * are supported:
	 * <dl>
	 * <dt>Strategy
	 * <dd>the data represents strategies.
	 * <dt>Fitness
	 * <dd>the data represents payoffs/scores/fitness.
	 * <dt>Degree
	 * <dd>the data represents degrees of the network structure.
	 * <dt>Fixation probability
	 * <dd>the data represents fixation probabilities.
	 * <dt>Fixation time
	 * <dd>the data represents fixation times.
	 * <dt>Stationary distribution
	 * <dd>the data represents the stationary strategy distribution.
	 * <dt>undefined
	 * <dd>the data type is not defined/unknown.
	 * </dl>
	 */
	public static enum Data {

		/**
		 * Undefined: the data type is not defined/unknown.
		 */
		UNDEFINED("undefined"), //

		/**
		 * Strategy: the data represents strategies.
		 */
		STRATEGY("Strategies - Histogram"), //

		/**
		 * Fitness: the data represents payoffs/scores/fitness.
		 */
		FITNESS("Fitness - Histogram"), //

		/**
		 * Degree: the data represents degrees of the network structure.
		 */
		DEGREE("Structure - Degree"), //

		/**
		 * Fixation probability: the data represents fixation probabilities.
		 */
		STATISTICS_FIXATION_PROBABILITY("Statistics - Fixation probability"), //

		/**
		 * Fixation time: the data represents fixation times.
		 */
		STATISTICS_FIXATION_TIME("Statistics - Fixation time"), //

		/**
		 * Stationary distribution.
		 */
		STATISTICS_STATIONARY("Statistics - Stationary distribution"); //

		/**
		 * Identifying id of the type of data.
		 */
		String id;

		/**
		 * Construct an enum for the type of data.
		 * 
		 * @param id the identifier of the data type
		 */
		Data(String id) {
			this.id = id;
		}

		@Override
		public String toString() {
			return id;
		}
	}

	/**
	 * GWT models run asynchronously and long running tasks require scheduling to
	 * maintain responsive user interface. In contrast, JRE provides a
	 * multi-threaded environment, which allows to run multiple threads
	 * synchronously. This parallel execution results in an significant speed-boost
	 * (for PDE calculations).
	 *
	 * @return <code>true</code> if model calculations are asynchronous
	 */
	public boolean useScheduling() {
		return false;
	}

	/**
	 * Milestone: Load this model and allocate resources (if applicable).
	 * 
	 * @see MilestoneListener#modelLoaded()
	 */
	public void load() {
		rng = engine.getRNG();
		species = engine.getModule().getSpecies();
		nSpecies = species.size();
		isMultispecies = (nSpecies > 1);
	}

	/**
	 * Milestone: Unload this model and free resources (if applicable).
	 * 
	 * @see MilestoneListener#modelUnloaded()
	 */
	public void unload() {
		rng = null;
		species = null;
	}

	/**
	 * Milestone: Initialize this model
	 * 
	 * @see MilestoneListener#modelDidReinit()
	 */
	public abstract void init();

	/**
	 * Milestone: Reset this model
	 * 
	 * @see MilestoneListener#modelDidReset()
	 */
	public abstract void reset();

	/**
	 * Update this model. For example called after initialization and when
	 * parameters changed.
	 * 
	 * @see ChangeListener#modelChanged(ChangeListener.PendingAction)
	 */
	public abstract void update();

	/**
	 * Check consistency of parameters and adjust if necessary (and possible). All
	 * issues and modifications should be reported through <code>logger</code>. Some
	 * parameters can be adjusted while the model remains active or even while
	 * running, whereas others require a reset. An example of the former category is
	 * in general simple adjustments of payoffs, while an example of the latter
	 * category is a change of the population structure.
	 *
	 * @return <code>true</code> if reset required
	 * 
	 * @see java.util.logging.Logger
	 */
	public abstract boolean check();

	/**
	 * Advance model by one step. The details of what happens during one step
	 * depends on the models {@link Type} as well as its {@link Mode}.
	 * 
	 * @return <code>true</code> if <code>next()</code> can be called again.
	 *         Typically <code>false</code> is returned if the model requires
	 *         attention, such as the following conditions:
	 *         <ul>
	 *         <li>the model has converged
	 *         <li>the model turned monomorphic (stops only if requested)
	 *         <li>a statistics sample is available
	 *         <li>a preset time has been reached
	 *         </ul>
	 * 
	 * @see ChangeListener#modelChanged(ChangeListener.PendingAction)
	 * @see org.evoludo.simulator.modules.Discrete#setMonoStop(boolean)
	 */
	public abstract boolean next();

	/**
	 * Relax the initial configuration of the model over {@code generations}. During
	 * relaxation the method {@link #relaxing()} must return {@code true}.
	 * 
	 * @param generations the number of generations to relax the model
	 * @return {@code false} if converged during relaxation
	 * 
	 * @see #relaxing()
	 * @see #next()
	 */
	public boolean relax(double generations) {
		return false;
	}

	/**
	 * Check if EvoLudo model is in the process of relaxing.
	 * 
	 * @return {@code true} if model is currently relaxing
	 * 
	 * @see #relax(double)
	 */
	public boolean relaxing() {
		return false;
	}

	/**
	 * Return the logger for reporting information.
	 *
	 * @return the logger
	 */
	public Logger getLogger() {
		return logger;
	}

	/**
	 * Returns the minimum score that individuals of species with ID <code>id</code>
	 * can achieve in this model. Takes into account potential adjustments due to
	 * population structure and payoff accounting.
	 * 
	 * @param id the id of the population for multi-species models
	 * @return the minimum score
	 * 
	 * @see Module#getMinGameScore()
	 */
	public abstract double getMinScore(int id);

	/**
	 * Returns the maximum score that individuals of species with ID <code>id</code>
	 * can achieve in this model. Takes into account potential adjustments due to
	 * population structure and payoff accounting.
	 * 
	 * @param id the id of the population for multi-species models
	 * @return the maximum score
	 * 
	 * @see Module#getMaxGameScore()
	 */
	public abstract double getMaxScore(int id);

	/**
	 * Calculates and returns the absolute fitness minimum. This is important to
	 * <ol>
	 * <li>determine probabilities or rates for adopting the strategy of another
	 * player,
	 * <li>optimize fitness based picking of individuals, and
	 * <li>scaling graphical output.
	 * </ol>
	 *
	 * @param id the id of the population for multi-species models
	 * @return the minimum fitness
	 * 
	 * @see #getMinScore(int id)
	 */
	public abstract double getMinFitness(int id);

	/**
	 * Calculates and returns the absolute fitness maximum. This is important to
	 * <ol>
	 * <li>determine probabilities or rates for adopting the strategy of another
	 * player,
	 * <li>optimize fitness based picking of individuals, and
	 * <li>scaling graphical output.
	 * </ol>
	 *
	 * @param id the id of the population for multi-species models
	 * @return the maximum fitness
	 * 
	 * @see #getMaxScore(int id)
	 */
	public abstract double getMaxFitness(int id);

	/**
	 * Returns status message from model. Typically this is a string summarizing the
	 * current state of the simulation. For example, models with discrete strategy
	 * sets (such as 2x2 games, see {@link org.evoludo.simulator.modules.TBT})
	 * return the average frequencies of each strategy type in the population(s),
	 * see {@link IBSDPopulation}. Similarly, models with continuous strategies
	 * (such as continuous snowdrift games, see
	 * {@link org.evoludo.simulator.modules.CSD}) return the mean, minimum and
	 * maximum trait value(s) in the population(s), see {@link IBSMCPopulation}. The
	 * status message is displayed along the bottom of the GUI.
	 * <p>
	 * <strong>Note:</strong> if the model runs into difficulties, problems should
	 * be reported through the logging mechanism. Messages with severity
	 * {@link Level#WARNING} or higher are displayed in the status of the GUI and
	 * override status messages returned here.
	 *
	 * @return status of active model
	 */
	public abstract String getStatus();

	/**
	 * Gets a formatted string summarizing the elapsed time.
	 * 
	 * @return elapsed time as string
	 */
	public String getCounter() {
		return "time: " + Formatter.format(getTime(), 2);
	}

	/**
	 * Gets the elapsed time in model. Time is measured in generations.
	 * 
	 * @return elapsed time
	 * 
	 * @see #time
	 */
	public double getTime() {
		return time;
	}

	/**
	 * Set initial traits in one or all species.
	 * <p>
	 * <strong>NOTE:</strong> this is a convenience method for multi-species modules
	 * to set inital states efficiently for interactions with GUI.
	 *
	 * @param init the array with the initial trait values
	 * @return {@code true} if initial traits successfully set
	 */
	public boolean setInitialTraits(double[] init) {
		return false;
	}

	/**
	 * Collect and return initial trait values for all species.
	 * <p>
	 * <strong>NOTE:</strong> this is a convenience method for multi-species modules
	 * to retrieve states efficiently for further processing or visualization.
	 *
	 * @param init the array for storing the initial trait values
	 */
	public abstract void getInitialTraits(double[] init);

	/**
	 * Set initial trait values for species with ID <code>id</code>.
	 *
	 * @param id   the species identifier
	 * @param init the array with the initial trait values
	 * @return {@code true} if initial traits successfully set
	 */
	public boolean setInitialTraits(int id, double[] init) {
		return false;
	}

	/**
	 * Return initial trait values for species with ID <code>id</code>.
	 *
	 * @param id   the species identifier
	 * @param init the array for storing the initial trait values
	 */
	public abstract void getInitialTraits(int id, double[] init);

	/**
	 * Return the number of mean values for this model including all species (for
	 * traits or fitness). By default this returns the number of traits in the
	 * module. Models that report a different number of mean traits must override
	 * this method
	 *
	 * @return the number of mean values for all species
	 */
	public abstract int getNMean();

	/**
	 * Return the number of mean trait values for species with ID <code>id</code>.
	 *
	 * @param id the species identifier
	 * @return the number of mean values for species {@code id}
	 */
	public abstract int getNMean(int id);

	/**
	 * Return the names of the mean traits of this model.
	 *
	 * @return the names of the mean traits
	 */
	public String[] getMeanNames() {
		int nMean = getNMean();
		String[] names = new String[nMean];
		for (int n = 0; n < nMean; n++)
			names[n] = getMeanName(n);
		return names;
	}

	/**
	 * Return the name of the mean trait with index {@code index} or {@code null} if
	 * index is invalid.
	 *
	 * @param index the index of the mean trait
	 * @return the name of mean trait with index {@code index}
	 */
	public abstract String getMeanName(int index);

	/**
	 * Return the colors for the mean traits of this model.
	 *
	 * @return the color array for the mean values
	 */
	public abstract Color[] getMeanColors();

	/**
	 * Return the colors for the mean traits for species with ID {@code id}.
	 *
	 * @param id the index of the mean trait
	 * @return the color array for the mean values
	 */
	public abstract Color[] getMeanColors(int id);

	/**
	 * Collect and return mean trait values for all species.
	 * <p>
	 * <strong>NOTE:</strong> this is a convenience method for multi-species modules
	 * to retrieve states efficiently for further processing or visualization.
	 *
	 * @param mean the array for storing the mean trait values
	 * @return <code>true</code> if this and previous data point should be
	 *         connected, i.e. no reset had been requested in the mean time.
	 */
	public abstract boolean getMeanTraits(double[] mean);

	/**
	 * Return mean trait values for species with ID <code>id</code>.
	 *
	 * @param id   the species identifier
	 * @param mean the array for storing the mean trait values
	 * @return <code>true</code> if this and the previous data point should be
	 *         connected, i.e. no reset had been requested in the mean time.
	 */
	public abstract boolean getMeanTraits(int id, double[] mean);

	/**
	 * Return mean trait values at location <code>idx</code> for species with ID
	 * <code>id</code>.
	 * <p>
	 * <strong>Note:</strong> optional implementation; currently makes sense only
	 * for local dynamics in PDE models.
	 * <p>
	 * <strong>IMPORTANT:</strong> the returned array is live and should
	 * <em>not</em> be altered.
	 *
	 * @param id  the species identifier
	 * @param idx the index of the location
	 * @return array of mean trait values
	 */
	public double[] getMeanTraitAt(int id, int idx) {
		return null;
	}

	/**
	 * Gets the formatted trait names at location <code>idx</code> for species with
	 * ID <code>id</code>. The formatting may include HTML tags. Used by GUI for
	 * example to show trait names in tooltips.
	 * <p>
	 * <strong>Note:</strong> optional implementation. Currently makes sense only
	 * for IBS models and local dynamics in PDE models.
	 *
	 * @param id  the species identifier
	 * @param idx the index of the location
	 * @return description of traits at <code>idx</code>
	 */
	public String getTraitNameAt(int id, int idx) {
		return null;
	}

	/**
	 * Gets the trait data for species with ID <code>id</code> and translates them
	 * into colors using the <code>colorMap</code>. The result is stored and
	 * returned in <code>colors</code>. Used by GUI to visualize the current state
	 * of the model.
	 * 
	 * @param <T>      color data type. {@link Color} for
	 *                 {@link org.evoludo.graphics.PopGraph2D} and
	 *                 {@link org.evoludo.graphics.PopGraph2D} as well as
	 *                 {@link thothbot.parallax.core.shared.materials.MeshLambertMaterial
	 *                 MeshLambertMaterial} for
	 *                 {@link org.evoludo.graphics.PopGraph3D}
	 * @param id       the species identifier
	 * @param colors   the array for storing the colors for individuals
	 * @param colorMap the map for translating individual traits into colors
	 */
	public abstract <T> void getTraitData(int id, T[] colors, ColorMap<T> colorMap);

	/**
	 * Gets the mean fitness values for traits in all species. The result is stored
	 * and returned in <code>mean</code>. Used by GUI to visualize the current the
	 * state of the model.
	 * <p>
	 * <strong>Note:</strong> this is a convenience method for multi-species modules
	 * to retrieve states efficiently for further processing or visualization.
	 *
	 * @param mean the array for storing the mean fitness values
	 * @return <code>true</code> if this and the previous data point should be
	 *         connected, i.e. no reset had been requested in the mean time.
	 */
	public abstract boolean getMeanFitness(double[] mean);

	/**
	 * Gets the mean fitness values for species with ID <code>id</code>. The result
	 * is stored and returned in <code>mean</code>. Used by GUI to visualize local
	 * dynamics at <code>idx</code>.
	 *
	 * @param id   the species identifier
	 * @param mean the array for storing the mean fitness values
	 * @return <code>true</code> if this and the previous data point should be
	 *         connected, i.e. no reset had been requested in the mean time.
	 */
	public abstract boolean getMeanFitness(int id, double[] mean);

	/**
	 * Gets the mean fitness(es) of this model at location <code>idx</code>. Used
	 * by GUI to visualize local dynamics at <code>idx</code>.
	 * <p>
	 * <strong>Note:</strong> optional implementation. Currently makes sense only
	 * for local dynamics in PDE models.
	 * <p>
	 * <strong>IMPORTANT:</strong> the returned array is live and should
	 * <em>not</em> be altered.
	 * 
	 * @param id  the species identifier
	 * @param idx the location of the fitness values
	 * @return the array of mean fitness values
	 */
	public double[] getMeanFitnessAt(int id, int idx) {
		return null;
	}

	/**
	 * Gets the fitness at location <code>idx</code> for species with ID
	 * <code>id</code> as a formatted string. The formatting may include HTML tags.
	 * Used by GUI for example to show fitness in tooltips.
	 * <p>
	 * <strong>Note:</strong> optional implementation. Currently makes sense only
	 * for IBS and PDE models.
	 *
	 * @param id  the species identifier
	 * @param idx the index of the location
	 * @return the fitness as a formatted string
	 */
	public String getFitnessNameAt(int id, int idx) {
		return null;
	}

	/**
	 * Gets the score at location <code>idx</code> for species with ID
	 * <code>id</code> as a formatted string. The formatting may include HTML tags.
	 * Used by GUI for example to show scores in tooltips.
	 * <p>
	 * <strong>Note:</strong> optional implementation. Currently makes sense only
	 * for IBS and PDE models.
	 *
	 * @param id  the species identifier
	 * @param idx the index of the location
	 * @return the score as a formatted string
	 */
	public String getScoreNameAt(int id, int idx) {
		return null;
	}

	/**
	 * Translates fitness data into colors using ColorMap <code>colorMap</code>.
	 * Used by GUI to visualize current state of model.
	 * 
	 * @param <T>      color data type. {@link Color} for
	 *                 {@link org.evoludo.graphics.PopGraph2D} and
	 *                 {@link org.evoludo.graphics.PopGraph2D} as well as
	 *                 {@link thothbot.parallax.core.shared.materials.MeshLambertMaterial
	 *                 MeshLambertMaterial} for
	 *                 {@link org.evoludo.graphics.PopGraph3D}
	 * @param id       the species identifier
	 * @param colors   the array for storing color values
	 * @param colorMap the map to use for translating traits to colors
	 */
	public abstract <T> void getFitnessData(int id, T[] colors, ColorMap.Gradient1D<T> colorMap);

	/**
	 * Generates a histogram of fitness data and returns the result in the provided
	 * array <code>bins</code>. For Discrete modules a fitness histogram is returned
	 * for each trait separately. For Continuous modules there is, in general, only
	 * a single fitness dimension.
	 * 
	 * @param id   the species identifier
	 * @param bins the array for storing histogram. For Discrete modules this is
	 *             always one dimensional
	 */
	public abstract void getFitnessHistogramData(int id, double[][] bins);

	/**
	 * Checks if the current data point should be connected to the previous one. For
	 * example, returns <code>false</code> after a model has been reset and
	 * subsequent data points no longer refer to the same time series.
	 *
	 * @return <code>true</code> if data points are connected.
	 */
	public abstract boolean isConnected();

	/**
	 * Checks if time reversal is permitted. By default returns <code>false</code>.
	 * Only few models are capable of time reversal.
	 *
	 * @return <code>true</code> if time reversal permissible.
	 *
	 * @see #setTimeReversed(boolean)
	 */
	public boolean permitsTimeReversal() {
		return false;
	}

	/**
	 * Checks if time is reversed. By default returns <code>false</code>. Only few
	 * models are capable of time reversal.
	 *
	 * @return <code>true</code> if time is reversed
	 *
	 * @see #setTimeReversed(boolean)
	 */
	public boolean isTimeReversed() {
		return false;
	}

	/**
	 * Request time reversal if <code>reversed==true</code>. The model may not be
	 * able to honour the request. However, some models allow to travel back in
	 * time. In general, this is only possible for ODE and SDE models and even for
	 * those it may not be feasible due to details of the dynamics, such as
	 * dissipative terms in the differential equations. For example, in
	 * {@link org.evoludo.simulator.modules.CG} the ecological dynamics of the patch
	 * quality prevents time reversal, i.e. results are numerically unstable due to
	 * exponential amplification of deviations in dissipative systems.
	 *
	 * @param reversed the request whether time should be reversed.
	 *
	 * @see org.evoludo.simulator.models.ODEEuler#setTimeReversed
	 *      ODEEuler.setTimeReversed
	 * @see org.evoludo.simulator.models.SDEEuler#setTimeReversed
	 *      SDEEuler.setTimeReversed
	 */
	public void setTimeReversed(boolean reversed) {
	}

	/**
	 * Checks if debugging single steps is supported. By default returns
	 * <code>false</code>.
	 * Only few models support debugging of single update steps.
	 *
	 * @return <code>true</code> if stepwise debuggin is permissible.
	 */
	public boolean permitsDebugStep() {
		return false;
	}

	/**
	 * Perform single debug step in models that allow it.
	 */
	public void debugStep() {
	}

	/**
	 * Encode the state of the model in a <code>plist</code> inspired
	 * <code>XML</code> string. This allows to save the state and restore later with
	 * the exact same results as when continuing to run the model. This even allows
	 * to switch from JRE to GWT or back and obtain identical results!
	 * 
	 * @param plist the {@link java.lang.StringBuilder StringBuilder} to write the
	 *              encoded state to
	 * 
	 * @see org.evoludo.util.Plist
	 * @see org.evoludo.util.XMLCoder
	 */
	public abstract void encodeState(StringBuilder plist);

	/**
	 * Restore the state encoded in the <code>plist</code> inspired <code>map</code>
	 * of {@code key, value}-pairs.
	 * 
	 * @param map the map of {@code key, value}-pairs
	 * @return <code>true</code> if successful
	 * 
	 * @see org.evoludo.util.Plist
	 * @see org.evoludo.util.PlistReader
	 * @see org.evoludo.util.PlistParser
	 */
	public abstract boolean restoreState(Plist map);
}
