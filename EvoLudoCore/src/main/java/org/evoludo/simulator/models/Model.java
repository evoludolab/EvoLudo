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
import java.util.logging.Level;
import java.util.logging.Logger;

import org.evoludo.simulator.ColorMap;
import org.evoludo.simulator.EvoLudo;
import org.evoludo.simulator.Geometry;
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
public abstract interface Model extends CLOProvider {

	/**
	 * Common interface for all models with discrete strategy sets.
	 */
	public interface Discrete extends Model {

		@Override
		public default boolean isContinuous() {
			return false;
		}

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
	public interface Continuous extends Model {

		@Override
		public default boolean isContinuous() {
			return true;
		}

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
	}

	/**
	 * Common interface for all differential equations models.
	 * <p>
	 * <strong>Note:</strong> Currently differential equation models are restricted
	 * to discrete strategy sets.
	 */
	public abstract interface DE extends Discrete {

		/**
		 * Return whether this DE model tracks frequencies or densities. Returns
		 * <code>false</code> (i.e. frequency based model) by default.
		 *
		 * @return <code>true</code> if state refers to densities.
		 */
		public default boolean isDensity() {
			return false;
		}

		/**
		 * Sets the discretization of time increments in continuous time models.
		 * <p>
		 * <strong>Note:</strong> Some models may need to adjust, i.e. reduce,
		 * <code>dt</code> (see {@link PDERD#checkDt()}) or choose a variable step size
		 * in which case <code>dt</code> is ignored (see {@link ODERK#getAutoDt()}).
		 *
		 * @param dt the time increments in continuous time models.
		 */
		public void setDt(double dt);

		/**
		 * Gets the discretization of time increments in continuous time models.
		 * <p>
		 * <strong>Note:</strong> This may be different from <code>dt</code> set through
		 * {@link #setDt(double)} if the model required adjustments.
		 *
		 * @return the time increment in continuous time models.
		 */
		public double getDt();

		/**
		 * Sets the desired accuracy for determining convergence. If
		 * <code>y(t+dt)-y(t)&lt;a dt</code> holds, where <code>y(t+dt)</code> denotes
		 * the new state and <code>y(t)</code> the previous state, the numerical
		 * integration is reported as having converged and stops.
		 *
		 * @param accuracy the numerical accuracy
		 */
		public void setAccuracy(double accuracy);

		/**
		 * Gets the numerical accuracy for determining convergence.
		 *
		 * @return the numerical accuracy
		 */
		public double getAccuracy();
	}

	/**
	 * Interface for ordinary differential equation models.
	 */
	public interface ODE extends DE {

		@Override
		public default Type getModelType() {
			return Type.ODE;
		}

		/**
		 * {@inheritDoc}
		 * <p>
		 * ODE and SDE models return <code>true</code> by default.
		 */
		@Override
		public default boolean permitsTimeReversal() {
			return true;
		}
	}

	/**
	 * Interface for stochastic differential equation models.
	 */
	public interface SDE extends ODE {

		@Override
		public default Type getModelType() {
			return Type.SDE;
		}
	}

	/**
	 * Interface for partial differential equation models.
	 */
	public interface PDE extends DE {

		@Override
		public default Type getModelType() {
			return Type.PDE;
		}

		@Override
		public default boolean isAsynchronous() {
			return EvoLudo.isGWT;
		}

		/**
		 * Sets whether symmetries should be preserved. Not all models may be able to
		 * honour the request. For example {@link PDERD} is only able to preserve
		 * symmetries in the diffusion step if the
		 * {@link Geometry#isLattice()} returns <code>true</code>.
		 *
		 * @param symmetric the request to preserve symmetry
		 */
		default void setSymmetric(boolean symmetric) {
		}

		/**
		 * Gets whether the model preserves symmetry. Requires that symmetry
		 * preservation is requested <em>and</em> the model is able to honour the
		 * request.
		 *
		 * @return <code>true</code> if symmetry is preserved
		 */
		public default boolean isSymmetric() {
			return false;
		}

		// boolean[] getAutoScale();
		//
		// public double[] getMinScale();
		//
		// public double[] getMaxScale();

		/**
		 * Gets the {@link Geometry} representing the spatial dimensions underlying the
		 * partial differential equation, PDE.
		 *
		 * @return geometry of PDE
		 */
		public Geometry getGeometry();

		/**
		 * Helper method to initialize the effective rate of diffusion for the time
		 * increment <code>dt</code>.
		 * <p>
		 * <strong>Note:</strong> This method needs to be public to permit access by
		 * {@link org.evoludo.simulator.models.PDESupervisorGWT PDESupervisorGWT} and
		 * {@link org.evoludo.simulator.models.PDESupervisorJRE PDESupervisorJRE}
		 * 
		 * @param dt the time increment for diffusion
		 */
		public void initDiffusion(double dt);

		/**
		 * Increments time by <code>dt</code>. This is used by the
		 * {@link PDESupervisor} to report back on the progress.
		 *
		 * @param dt the time that has elapsed
		 * @return {@code true} to continue and {@code false} to request a stop
		 */
		public boolean incrementTime(double dt);

		/**
		 * Indicates that the numerical integration has converged to a homogeneous
		 * state. This is used by the {@link PDESupervisor} to report back on the
		 * progress.
		 */
		public void setConverged();

		/**
		 * Reaction step. Update cells with indices between <code>start</code>
		 * (including) and <code>end</code> (excluding) and return the accumulated/total
		 * change in state.
		 * <p>
		 * <strong>Note:</strong> At the end, the state in <code>density</code> is
		 * unchanged, the new density distribution is in <code>next</code> and the
		 * fitness matching <code>density</code> is updated.
		 * <p>
		 * <strong>Important:</strong> must be thread safe for JRE. In particular, no
		 * memory can be shared with anyone else!
		 *
		 * @param from the index of the first cell (including)
		 * @param to   the index of the last cell (excluding)
		 * @return the accumulated change in state
		 */
		public double react(int from, int to);

		/**
		 * Diffusion step. Update cells with indices between <code>from</code>
		 * (including) and <code>to</code> (excluding). In order to preserve symmetry,
		 * if requested and possible, the neighbouring cells are sorted according to
		 * their density before the diffusion step is performed. The sorting is fairly
		 * expensive in terms of CPU time but it doesn't matter whether the sorting is
		 * ascending or descending.
		 * <p>
		 * <strong>Note:</strong> At the end, the state in <code>next</code> is
		 * unchanged, the new density distribution is in <code>density</code> and the
		 * fitness is untouched/unused.
		 * <p>
		 * <strong>Important:</strong> must be thread safe for JRE. In particular, no
		 * memory can be shared with anyone else!
		 *
		 * @param from the index of the first cell (including)
		 * @param to   the index of the last cell (excluding)
		 */
		public void diffuse(int from, int to);

		/**
		 * Resets minimum, maximum and mean fitnesses prior to reaction step.
		 */
		public void resetFitness();

		/**
		 * Normalizes the mean fitnesses after the reaction step is complete.
		 */
		public void normalizeMeanFitness();

		/**
		 * Initializes minimum, maximum and mean density based on current state.
		 */
		public void setDensity();

		/**
		 * Resets minimum, maximum and mean density prior to diffusion step.
		 */
		public void resetDensity();

		/**
		 * Normalizes the mean density after the diffusion step is complete.
		 */
		public void normalizeMeanDensity();
	}

	/**
	 * Interface for individual based simulation models.
	 */
	public interface IBS extends Model {

		@Override
		public default Type getModelType() {
			return Type.IBS;
		}

		/**
		 * Gets the number of interactions at location <code>idx</code> for species with
		 * ID <code>id</code>. Used by GUI for example to show interaction counts in
		 * tooltips.
		 * <p>
		 * <strong>Note:</strong> optional implementation. Currently makes sense only
		 * for IBS models.
		 *
		 * @param id  the species identifier
		 * @param idx the index of the location
		 * @return the interaction count
		 */
		public default int getInteractionsAt(int id, int idx) {
			return -1;
		}

		/**
		 * Gets formatted tag of individual at location <code>idx</code> for species
		 * with ID <code>id</code>. The formatting may include HTML tags. Used by GUI
		 * for example to show tags in tooltips. Opportunity to track ancestry through
		 * tags.
		 * <p>
		 * <strong>Note:</strong> optional implementation. Currently makes sense only
		 * for IBS models.
		 *
		 * @param id  the species identifier
		 * @param idx the index of the location
		 * @return the formatted tag
		 */
		public default String getTagNameAt(int id, int idx) {
			return null;
		}

		/**
		 * Used by GUI to interact with Model. Called whenever a mouse click/tap was
		 * registered by a node.
		 * 
		 * @param id  the species identifier
		 * @param hit the index of the node hit by mouse
		 * @return <code>false</code> if no actions taken
		 */
		public default boolean mouseHitNode(int id, int hit) {
			return mouseHitNode(id, hit, false);
		}

		/**
		 * Used by GUI to interact with Model. Called whenever a mouse click/tap was
		 * registered by a node, potentially with the Alt modifier key.
		 * <p>
		 * <strong>Experimental:</strong> allow more diverse interactions by including
		 * modifier keys.
		 *
		 * @param id  the species identifier
		 * @param hit the index of the node hit by mouse
		 * @param alt <code>true</code> if modifier key Alt was pressed
		 * @return <code>false</code> if no actions taken
		 */
		public default boolean mouseHitNode(int id, int hit, boolean alt) {
			return false;
		}
	}

	/**
	 * Interface for models with discrete strategy sets
	 */
	public interface DiscreteIBS extends Discrete, IBS {
	}

	/**
	 * Interface for models with continuous strategy sets
	 */
	public interface ContinuousIBS extends Continuous, IBS {

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
	public boolean isContinuous();

	/**
	 * Gets the type of this model.
	 * 
	 * @return the type of model
	 */
	public Type getModelType();

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
	 * GWT models run asynchronously, while JRE runs multiple threads synchronously.
	 * This parallel execution results in an significant speed-boost (for PDE
	 * calculations).
	 *
	 * @return <code>true</code> if model calculations are asynchronous
	 */
	public default boolean isAsynchronous() {
		return false;
	}

	/**
	 * Milestone: Load this model and allocate resources (if applicable).
	 * 
	 * @see MilestoneListener#modelLoaded()
	 */
	public void load();

	/**
	 * Milestone: Unload this model and free resources (if applicable).
	 * 
	 * @see MilestoneListener#modelUnloaded()
	 */
	public void unload();

	/**
	 * Milestone: Initialize this model
	 * 
	 * @see MilestoneListener#modelDidReinit()
	 */
	public void init();

	/**
	 * Milestone: Reset this model
	 * 
	 * @see MilestoneListener#modelDidReset()
	 */
	public void reset();

	/**
	 * Update this model. For example called after initialization and when
	 * parameters changed.
	 * 
	 * @see ChangeListener#modelChanged(ChangeListener.PendingAction)
	 */
	public void update();

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
	public boolean check();

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
	public boolean next();

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
	public default boolean relax(double generations) {
		return false;
	}

	/**
	 * Check if EvoLudo model is in the process of relaxing.
	 * 
	 * @return {@code true} if model is currently relaxing
	 * 
	 * @see #relax(double)
	 */
	public default boolean relaxing() {
		return false;
	}

	/**
	 * Return the logger for reporting information.
	 *
	 * @return the logger
	 */
	public abstract Logger getLogger();

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
	public double getMinScore(int id);

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
	public double getMaxScore(int id);

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
	public double getMinFitness(int id);

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
	public double getMaxFitness(int id);

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
	public default String getCounter() {
		return "time: " + Formatter.format(getTime(), 2);
	}

	/**
	 * Gets the elapsed time in model. Typically this is measured in generations for
	 * individual based simulations and continuous time for numerical models.
	 * 
	 * @return elapsed time
	 */
	public double getTime();

	/**
	 * Gets the elapsed time in real time units. The real time increments of
	 * microscopic updates depends on the fitness of the population. In populations
	 * with high fitness many events happen per unit time and hence the increments
	 * are smaller. In contrast in populations with low fitness fewer events happen
	 * and consequently more time elapses between subsequent events. By default no
	 * distinction between real time and generation time is made.
	 * 
	 * @return elapsed real time
	 */
	public default double getRealtime() {
		return getTime();
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
	public default boolean setInitialTraits(double[] init) {
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
	public default boolean setInitialTraits(int id, double[] init) {
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
	 * Return the number of species in this model.
	 *
	 * @return the number of species
	 */
	public abstract int getNSpecies();

	/**
	 * Return the species with ID <code>id</code>.
	 *
	 * @param id the species identifier
	 * @return the species
	 */
	public abstract Module getSpecies(int id);

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
	public default String[] getMeanNames() {
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
	public default double[] getMeanTraitAt(int id, int idx) {
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
	public default String getTraitNameAt(int id, int idx) {
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
	public boolean getMeanFitness(double[] mean);

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
	public default double[] getMeanFitnessAt(int id, int idx) {
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
	public default String getFitnessNameAt(int id, int idx) {
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
	public default String getScoreNameAt(int id, int idx) {
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
	public boolean isConnected();

	/**
	 * Checks if model has converged.
	 *
	 * @return <code>true</code> if model has converged.
	 */
	public boolean hasConverged();

	/**
	 * Checks if time reversal is permitted. By default returns <code>false</code>.
	 * Only few models are capable of time reversal.
	 *
	 * @return <code>true</code> if time reversal permissible.
	 *
	 * @see #setTimeReversed(boolean)
	 */
	public default boolean permitsTimeReversal() {
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
	public default boolean isTimeReversed() {
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
	public default void setTimeReversed(boolean reversed) {
	}

	/**
	 * Checks if debugging single steps is supported. By default returns
	 * <code>false</code>.
	 * Only few models support debugging of single update steps.
	 *
	 * @return <code>true</code> if stepwise debuggin is permissible.
	 */
	public default boolean permitsDebugStep() {
		return false;
	}

	/**
	 * Perform single debug step in models that allow it.
	 */
	public default void debugStep() {
	}

	/**
	 * Check if current model implements mode {@code test}; by default only
	 * {@link Mode#DYNAMICS} is permitted.
	 *
	 * @param test the mode to test
	 * @return {@code true} if {@code test} is available in current model
	 */
	public default boolean permitsMode(Mode test) {
		return test == Mode.DYNAMICS;
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
	public default boolean setMode(Mode mode) {
		if (!permitsMode(mode))
			return false;
		throw new UnsupportedOperationException("setting mode '" + mode + "' not supported");
	}

	/**
	 * Request a change of the {@link Mode} of the model. Returns {@code false} if
	 * {@code mode} is not supported.
	 * 
	 * @param mode the requested mode
	 * @return {@code true} if mode supported
	 * 
	 * @see EvoLudo#requestAction(ChangeListener.PendingAction)
	 */
	public default boolean requestMode(Mode mode) {
		return permitsMode(mode);
	}

	/**
	 * Gets the {@link Mode} of the model.
	 *
	 * @return mode of model
	 */
	public default Mode getMode() {
		return Mode.DYNAMICS;
	}

	/**
	 * Reset statistics and get ready to start new collection.
	 */
	public default void resetStatisticsSample() {
	}

	/**
	 * Clear statistics sample and get ready to collect next sample.
	 */
	public default void initStatisticsSample() {
	}

	/**
	 * Signal that statistics sample is ready to process.
	 */
	public default void readStatisticsSample() {
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
	public void encodeState(StringBuilder plist);

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
	public boolean restoreState(Plist map);
}
