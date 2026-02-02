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

package org.evoludo.simulator.models;

import java.awt.Color;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.evoludo.math.RNGDistribution;
import org.evoludo.simulator.ColorMap;
import org.evoludo.simulator.EvoLudo;
import org.evoludo.simulator.models.ChangeListener.PendingAction;
import org.evoludo.simulator.modules.Features;
import org.evoludo.simulator.modules.Module;
import org.evoludo.simulator.views.HasHistogram;
import org.evoludo.util.CLODelegate;
import org.evoludo.util.CLOParser;
import org.evoludo.util.CLOProvider;
import org.evoludo.util.CLOption;
import org.evoludo.util.CLOCategory;
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
public abstract class Model implements CLOProvider {

	/**
	 * Methods that every {@link Module} must implement, which advertises numerical
	 * solutions based on differential equations.
	 */
	public abstract interface HasDE {

		/**
		 * For replicator dynamics the frequencies of all traits must sum up to one.
		 * Hence, for <code>nTraits</code> traits there are only
		 * <code>nTraits-1</code> degrees of freedom. The index returned by
		 * <code>getDependent()</code> marks the one rate of change that is derived from
		 * all the others.
		 * <p>
		 * <strong>Notes:</strong>
		 * <ul>
		 * <li>Dependent traits are used by models where the frequencies of all types
		 * must sum up to one.
		 * <li>Density modules do not have dependent traits and {@code getDependent()}
		 * should return {@code -1}.
		 * <li>Currently differential equations implementations are only provided for
		 * Discrete modules.
		 * </ul>
		 * 
		 * @return the index of the dependent trait (or {@code -1} if there is none)
		 */
		public int getDependent();

		/**
		 * Interface for modules that support differential equation models with both
		 * frequency and density based dynamics.
		 */
		public interface DualDynamics {
		}

		/**
		 * Interface for differential equation models with pairwise interactions.
		 */
		public interface DPairs extends HasDE, Features.Pairs {

			/**
			 * Calculate the average payoff for the frequency of traits specified in
			 * the array <code>state</code> for pairwise interactions. The average payoffs
			 * for each of the <code>nTraits</code> traits must be stored and returned in
			 * the array <code>scores</code>.
			 *
			 * @param state  the frequency/density of each trait
			 * @param scores the array for storing the average payoffs/scores of each trait
			 */
			public void avgScores(double[] state, double[] scores);
		}

		/**
		 * Interface for differential equation models with interactions in groups of
		 * arbitrary size.
		 */
		public interface DGroups extends Features.Groups {

			/**
			 * Calculate the average payoff for the frequency of traits specified in
			 * the array <code>state</code> for interactions in groups of size
			 * <code>n</code>. The average payoffs for each of the <code>nTraits</code>
			 * traits must be stored and returned in the array <code>scores</code>.
			 *
			 * @param state  the frequency/density of each trait
			 * @param n      the size of interaction groups
			 * @param scores the array for storing the average payoffs/scores of each trait
			 */
			public void avgScores(double[] state, int n, double[] scores);
		}

		/**
		 * Interface for ordinary differential equation models; defaults to RK5.
		 */
		public interface ODE extends HasDE {
		}

		/**
		 * Interface for ordinary differential equation models using the fifth order
		 * Runge-Kutta method.
		 */
		public interface RK5 extends ODE {
		}

		/**
		 * Interface for ordinary differential equation models using the Euler-Maruyama
		 * method.
		 */
		public interface EM extends ODE {
		}

		/**
		 * Interface for stochastic differential equation models.
		 */
		public interface SDE extends HasDE {
		}

		/**
		 * Interface for partial differential equation models; defaults to PDERD.
		 */
		public interface PDE extends HasDE {
		}

		/**
		 * Interface for reaction-diffusion partial differential equation models.
		 */
		public interface PDERD extends PDE {
		}

		/**
		 * Interface for reaction-diffusion-advection partial differential equation
		 * models.
		 */
		public interface PDEADV extends PDE {
		}
	}

	/**
	 * Modules that offer individual based simulation models must implement this
	 * interface.
	 */
	public interface HasIBS {

		/**
		 * Provides opportunity for module to supply custom implementation of individual
		 * based simulations, IBS.
		 * <p>
		 * <strong>Important:</strong> if the custom IBS implementation involves random
		 * numbers, the shared random number generator must be used for reproducibility.
		 * 
		 * @return the custom implementation of the IBS or <code>null</code> to use the
		 *         default
		 * 
		 * @see EvoLudo#getRNG()
		 */
		public default Model createIBS() {
			return null;
		}

		/**
		 * Modules that offer individual based simulation models with discrete traits
		 * and pairwise interactions must implement this interface.
		 */
		public interface DPairs extends HasIBS, Features.Pairs {

			/**
			 * Calculate and return total (accumulated) payoff/score for pairwise
			 * interactions of the focal individual with trait {@code me} against opponents
			 * with different traits. The respective numbers of each of the {@code nTraits}
			 * opponent traits are provided in the array {@code tCount}. The payoffs/scores
			 * for each of the {@code nTraits} opponent traits must be stored and returned
			 * in the array {@code tScore}.
			 * <p>
			 * <strong>Important:</strong> must be overridden and implemented in subclasses
			 * that define game interactions between pairs of individuals
			 * ({@code nGroup=2}, {@code pairwise=true}), otherwise see
			 * {@link DGroups#groupScores(int[], double[])}.
			 * 
			 * @param me         the trait index of the focal individual
			 * @param traitCount number of opponents with each trait
			 * @param traitScore array for returning the scores of each opponent trait
			 * @return score of focal individual {@code me} accumulated over all
			 *         interactions
			 */
			public double pairScores(int me, int[] traitCount, double[] traitScore);

			/**
			 * Calculate the average payoff/score in a finite population with the number of
			 * each trait provided in {@code count} for pairwise interactions. The
			 * payoffs/scores for each of the {@code nTraits} traits must be stored and
			 * returned in the array {@code traitScores}.
			 * <p>
			 * <strong>Important:</strong> must be overridden and implemented in subclasses
			 * that define game interactions in well-mixed populations where individuals
			 * interact with everyone else. Computationally it is not feasible to cover this
			 * scenario with {@link #pairScores(int, int[], double[])} or
			 * {@link DGroups#groupScores(int[], double[])}, respectively.
			 * <p>
			 * <strong>Note:</strong> If explicit calculations of the well-mixed scores are
			 * not available, interactions with everyone in well-mixed populations should
			 * checked for and excluded with a warning in {@link #check()} (see
			 * {@link org.evoludo.simulator.models.IBSMCPopulation#check() CXPopulation} for
			 * an example).
			 * 
			 * @param traitCount number of individuals for each trait
			 * @param traitScore array for returning the payoffs/scores of each trait
			 */
			public void mixedScores(int[] traitCount, double[] traitScore);
		}

		/**
		 * Modules that offer individual based simulation models with discrete traits
		 * and interactions in groups must implement this interface.
		 */
		interface DGroups extends DPairs, Features.Groups {

			/**
			 * Calculate the payoff/score for interactions in groups consisting of
			 * traits with respective numbers given in the array {@code traitCount}. The
			 * interaction group size is given by the sum over {@code traitCount[i]} for
			 * {@code i=0,1,...,nTraits}. The payoffs/scores for each of the {@code nTraits}
			 * traits must be stored and returned in the array {@code traitScore}.
			 * <p>
			 * <strong>Important:</strong> must be overridden and implemented in subclasses
			 * that define game interactions among groups of individuals (for groups with
			 * sizes {@code nGroup&gt;2}, otherwise see
			 * {@link #pairScores(int, int[], double[])}).
			 * 
			 * @param traitCount group composition given by the number of individuals with
			 *                   each trait
			 * @param traitScore array for returning the payoffs/scores of each trait
			 */
			public void groupScores(int[] traitCount, double[] traitScore);

			/**
			 * Calculate the average payoff/score in a finite population with the number of
			 * each trait provided in {@code count} for interaction groups of size
			 * {@code n}. The payoffs/scores for each of the {@code nTraits} traits must be
			 * stored and returned in the array {@code traitScores}.
			 * 
			 * <h3>Notes:</h3>
			 * For payoff calculations:
			 * <ul>
			 * <li>each trait sees one less of its own type in its environment
			 * <li>the size of the environment is {@code nPopulation-1}
			 * <li>the fact that the payoff of each trait does not depend on its own type
			 * simplifies things
			 * </ul>
			 * If explicit calculations of the well-mixed scores are not available,
			 * interactions with everyone in well-mixed populations should be checked for
			 * and excluded with a warning in {@link #check()} (see
			 * {@link org.evoludo.simulator.models.IBSMCPopulation#check() IBSMCPopulation}
			 * for an example).
			 * 
			 * <h3>Important:</h3>
			 * Must be overridden and implemented in subclasses that define game
			 * interactions in well-mixed populations where individuals interact with
			 * everyone else. Computationally it is not feasible to cover this scenario with
			 * {@link #pairScores(int, int[], double[])} or
			 * {@link #groupScores(int[], double[])}, respectively.
			 * 
			 * @param traitCount number of individuals for each trait
			 * @param n          interaction group size
			 * @param traitScore array for returning the payoffs/scores of each trait
			 */
			public void mixedScores(int[] traitCount, int n, double[] traitScore);

			@Override
			public default void mixedScores(int[] traitCount, double[] traitScore) {
				mixedScores(traitCount, 2, traitScore);
			}
		}

		/**
		 * Modules that offer individual based simulation models with continuous traits
		 * and pairwise interactions must implement this interface.
		 */
		interface CPairs extends MCPairs {
			/**
			 * Calculate the payoff/score for modules with interactions in pairs and a
			 * single continuous trait. The focal individual has trait {@code me} and the
			 * traits of its {@code len} interaction partners are given in {@code group}.
			 * The payoffs/scores for each of the {@code len} opponent
			 * traits must be stored and returned in the array {@code payoffs}.
			 * 
			 * <h3>Note:</h3> Only the first {@code len} entries in {@code group} are
			 * guaranteed to exist and have meaningful values. The population structure may
			 * restrict the size of the interaction group. {@code len&le;nGroup} always
			 * holds.
			 * 
			 * <h3>Important:</h3> must be overridden and implemented in subclasses that
			 * define game interactions between pairs of individuals
			 * ({@code nGroup=2}, {@code pairwise=true}), otherwise see
			 * {@link CGroups#groupScores(double, double[], int, double[])}.
			 * 
			 * @param me           the trait of the focal individual
			 * @param groupTraits  the traits of the group members
			 * @param len          the number of members in the group
			 * @param groupPayoffs the array for returning the payoffs/scores for each group
			 *                     member
			 * @return the total (accumulated) payoff/score for the focal individual
			 */
			public double pairScores(double me, double[] groupTraits, int len, double[] groupPayoffs);

			@Override
			default double pairScores(double[] me, double[] groupTraits, int len, double[] groupPayoffs) {
				if (me.length != 1)
					throw new IllegalArgumentException("single-trait pairScores expects exactly one trait");
				return pairScores(me[0], groupTraits, len, groupPayoffs);
			}
		}

		/**
		 * Modules that offer individual based simulation models with continuous traits
		 * and interactions in groups must implement this interface.
		 */
		interface CGroups extends CPairs, Features.Groups {

			/**
			 * Calculate the payoff/score for modules with interactions in groups and a
			 * single continuous trait. The focal individual has trait {@code me} and the
			 * traits of its {@code len} interaction partners are given in {@code group}.
			 * The payoffs/scores for each of the {@code len} participants must be
			 * stored and returned in the array {@code payoffs}.
			 * 
			 * <h3>Note:</h3> Only the first {@code len*nTraits} entries in {@code group}
			 * are guaranteed to exist and have meaningful values. The population structure
			 * may restrict the size of the interaction group. {@code len&le;nGroup} always
			 * holds.
			 * 
			 * <h3>Important:</h3> Must be overridden and implemented in subclasses that
			 * define game interactions among groups of individuals with multiple continuous
			 * traits (for groups with sizes {@code nGroup&gt;2}, otherwise see
			 * {@link #pairScores(double, double[], int, double[])}).
			 * 
			 * @param me           the trait of the focal individual
			 * @param groupTraits  the traits of the group members
			 * @param len          the number of members in the group
			 * @param groupPayoffs the array for returning the payoffs/scores for each group
			 *                     member
			 * @return the payoff/score for the focal individual
			 */
			public double groupScores(double me, double[] groupTraits, int len, double[] groupPayoffs);
		}

		/**
		 * Modules that offer individual based simulation models with multiple
		 * continuous traits and pairwise interactions must implement this interface.
		 */
		interface MCPairs extends HasIBS, Features.Pairs {
			/**
			 * Calculate the payoff/score for modules with interactions in pairs and
			 * multiple continuous traits. The focal individual has traits {@code me} and
			 * the traits of its {@code len} interaction partners are given in
			 * {@code group}. The traits they are arranged in the usual manner, i.e. first
			 * all traits of the first group member then all traits by the second group
			 * member etc. for a total of {@code len*nTraits} entries. The payoffs/scores
			 * for each of the {@code len} opponent traits must be stored and returned in
			 * the array {@code payoffs}.
			 * 
			 * <h3>Note:</h3> Only the first {@code len} entries in {@code group} are
			 * guaranteed to exist and have meaningful values. The population structure may
			 * restrict the size of the interaction group. {@code len&le;nGroup} always
			 * holds.
			 * 
			 * <h3>Important:</h3> must be overridden and implemented in subclasses that
			 * define game interactions between pairs of individuals
			 * ({@code nGroup=2}, {@code pairwise=true}), otherwise see
			 * {@link MCGroups#groupScores(double, double[], int, double[])}.
			 * 
			 * @param me           the trait of the focal individual
			 * @param groupTraits  the traits of the group members
			 * @param len          the number of members in the group
			 * @param groupPayoffs the array for returning the payoffs/scores for each group
			 *                     member
			 * @return the total (accumulated) payoff/score for the focal individual
			 */
			public double pairScores(double[] me, double[] groupTraits, int len, double[] groupPayoffs);
		}

		/**
		 * Modules that offer individual based simulation models with continuous traits
		 * and interactions in groups must implement this interface.
		 */
		interface MCGroups extends CGroups {

			/**
			 * Calculate the payoff/score for modules with interactions in groups and
			 * multiple single continuous traits. The focal individual has traits {@code me}
			 * and the traits of its {@code len} interaction partners are given in
			 * {@code group}. The traits they are arranged in the usual manner, i.e. first
			 * all traits of the first group member then all traits by the second group
			 * member etc. for a total of {@code len*nTraits} entries. The payoffs/scores
			 * for each of the {@code len} participants must be stored and returned in
			 * the array {@code payoffs}.
			 * 
			 * <h3>Note:</h3> Only the first {@code len*nTraits} entries in {@code group}
			 * are guaranteed to exist and have meaningful values. The population structure
			 * may restrict the size of the interaction group. {@code len&le;nGroup} always
			 * holds.
			 * 
			 * <h3>Important:</h3> must be overridden and implemented in subclasses that
			 * define game interactions among groups of individuals with multiple continuous
			 * traits (for groups with sizes {@code nGroup&gt;2}, otherwise see
			 * {@link CPairs#pairScores(double, double[], int, double[])}).
			 * 
			 * @param me      the traits of the focal individual
			 * @param group   the traits of the group members
			 * @param len     the number of members in the group
			 * @param payoffs the array for returning the payoffs/scores for each group
			 *                member
			 * @return the payoff/score for the focal individual
			 */
			public double groupScores(double[] me, double[] group, int len, double[] payoffs);
		}
	}

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
	 * Keeps track of the time elapsed, measured based on the rates with which
	 * events occur. This is the natural unit of time for differential equation
	 * model. However, in individual based simulations a simpler measure of time
	 * based on the number of generations is often used. However, in populations of
	 * variable size the notion of one generation is hard to define or keeps
	 * changing over time. This measure takes the size of the population as well as
	 * the fitness of its members into account. For example, less time passes
	 * between reproductive events in large populations with high fitness, while
	 * more time passes in small populations with low fitness because there are
	 * fewer reproduction events per unit time.
	 * 
	 * <strong>Notes:</strong>
	 * <ol>
	 * <li><code>time==0</code> after {@link #reset()} and at the beginning of
	 * a simulation run.
	 * <li><code>time</code> is incremented <em>before</em> the next event is
	 * processed, to reflect the time at which the event occurs.
	 * <li>generally differs from number of updates.
	 * <li>may be negative for models that admit time reversal (e.g. integrating ODE
	 * backwards).
	 * <li>for modules that implement {@code Payoffs} non-negative individual scores
	 * are required.
	 * <li>models may implement only one time measure.
	 * <li>setting {@code time = Double.POSITIVE_INFINITY} disables time measured
	 * in terms of updates.
	 * </ol>
	 * 
	 * @see IBS#updates
	 * @see #permitsTimeReversal()
	 * @see IBS#pickFocalSpecies()
	 */
	protected double time = Double.POSITIVE_INFINITY;

	/**
	 * Short-cut to the list of species modules. Convenience field.
	 */
	protected List<? extends Module<?>> species;

	/**
	 * Markers for annotating graphical representations of the state of the model.
	 */
	protected Markers markers;

	/**
	 * The number of species in multi-species models.
	 */
	protected int nSpecies;

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
	 * Flag to indicate whether the current data point belongs to the same time
	 * series. This is used by the GUI to decide whether to connect the data points
	 * or not. Typically this is false only after {@link #init()}, {@link #reset()},
	 * {@link #restoreState(Plist)} or similar.
	 */
	protected boolean connect;

	/**
	 * Creates a model.
	 * 
	 * @param engine the pacemaker for running the model
	 */
	protected Model(EvoLudo engine) {
		this.engine = engine;
		logger = engine.getLogger();
	}

	/**
	 * Milestone: Load this model and allocate resources (if applicable).
	 * 
	 * @see LifecycleListener#modelLoaded()
	 */
	public void load() {
		rng = engine.getRNG();
		species = engine.getModule().getSpecies();
		nSpecies = species.size();
		isMultispecies = (nSpecies > 1);
		markers = new Markers(this);
	}

	/**
	 * Milestone: Unload this model and free resources (if applicable).
	 * 
	 * @see LifecycleListener#modelUnloaded()
	 */
	public void unload() {
		resetStatisticsSample();
		rng = null;
		species = null;
		markers = null;
	}

	/**
	 * Check consistency of parameters and adjust if necessary (and possible). All
	 * issues and modifications should be reported through <code>logger</code>. Some
	 * parameters can be adjusted while the model remains active or even while
	 * running, whereas others require a reset. An example of the former category is
	 * in general simple adjustments of payoffs, while an example of the latter
	 * category is a change of the population structure.
	 *
	 * @return <code>true</code> if reset required
	 */
	public boolean check() {
		return false;
	}

	/**
	 * Milestone: Reset this model
	 * 
	 * @see RunListener#modelDidReset()
	 */
	public void reset() {
		resetState();
	}

	/**
	 * Milestone: Initialize this model
	 * 
	 * @see RunListener#modelDidInit()
	 */
	public void init() {
		resetState();
	}

	/**
	 * Reset the internal state of this model; called by {@link #reset()} and
	 * {@link #init()}.
	 */
	protected void resetState() {
		time = 0.0;
		converged = false;
		connect = false;
	}

	/**
	 * Update this model. For example called after initialization and when
	 * parameters changed.
	 * 
	 * @see ChangeListener#modelChanged(ChangeListener.PendingAction)
	 */
	public abstract void update();

	/**
	 * Advance model by one step. The details of what happens during one step
	 * depends on the models {@link ModelType} as well as its {@link Mode}.
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
	 * The flag to indicate whether the model is currently relaxing the initial
	 * configuration.
	 */
	boolean isRelaxing = false;

	/**
	 * Relax the initial configuration of the model over {@code timeRelax}
	 * generations. During relaxation the method {@link #isRelaxing()} must return
	 * {@code true}.
	 * 
	 * @return {@code true} if converged during relaxation
	 * 
	 * @see #isRelaxing()
	 * @see #next()
	 * @see #cloTimeRelax
	 */
	public boolean relax() {
		if (hasConverged())
			return true;
		// note: use getUpdates to ensure relaxation works for all models.
		// DE models may not have an update count but have time
		double updt = getUpdates();
		if (timeRelax > 0.0 && updt < timeRelax) {
			isRelaxing = true;
			double rf = timeStep;
			timeStep = timeRelax - updt;
			next();
			timeStep = rf;
			isRelaxing = false;
			if (type == ModelType.IBS) {
				// reset traits after relaxation in IBS models
				for (Module<?> mod : species) {
					IBSPopulation<?, ?> pop = mod.getIBSPopulation();
					pop.resetTraits();
				}
			}
		}
		if (hasConverged()) {
			// no point in reporting failed initializations for statistics
			if (mode != Mode.STATISTICS_SAMPLE)
				logger.warning("extinction during relaxation.");
			return true;
		}
		return false;
	}

	/**
	 * Check if EvoLudo model is in the process of relaxing.
	 * 
	 * @return {@code true} if model is currently relaxing
	 * 
	 * @see #relax()
	 */
	public boolean isRelaxing() {
		return isRelaxing;
	}

	/**
	 * Checks if model has converged.
	 *
	 * @return <code>true</code> if model has converged.
	 *
	 * @see IBSDPopulation#checkConvergence()
	 * @see ODE#setAccuracy(double)
	 */
	public boolean hasConverged() {
		return converged;
	}

	/**
	 * Return the species with ID <code>id</code>.
	 *
	 * @param id the species identifier
	 * @return the species
	 */
	@SuppressWarnings("java:S1452") // impossible to specify generic type here
	public Module<?> getSpecies(int id) {
		if (id < 0 || id >= nSpecies)
			return null;
		return species.get(id);
	}

	/**
	 * Return the number of species in this model.
	 *
	 * @return the number of species
	 */
	public int getNSpecies() {
		return nSpecies;
	}

	/**
	 * Get the list of markers. This serves to mark special values in different
	 * kinds of graphs.
	 * 
	 * @return the list of markers
	 */
	public List<double[]> getMarkers() {
		if (markers == null)
			return null;
		return markers.getMarkers();
	}

	/**
	 * Indicates current mode of IBS model.
	 */
	protected Mode mode = Mode.DYNAMICS;

	/**
	 * Request a change of the {@link Mode} of the model. Returns {@code false} if
	 * {@code newmode} is not supported.
	 * 
	 * @param newmode the requested mode
	 * @return {@code true} if mode supported
	 * 
	 * @see EvoLudo#requestAction(ChangeListener.PendingAction)
	 */
	public boolean requestMode(Mode newmode) {
		if (!permitsMode(newmode))
			return false;
		if (this.mode == newmode)
			return true;
		PendingAction.CHANGE_MODE.mode = newmode;
		engine.requestAction(PendingAction.CHANGE_MODE);
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
	 * {@code mode} is already active or not supported.
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
	 * Check if the current model performs density based dynamics. The default is
	 * frequency based dynamics.
	 * <p>
	 * <strong>Note:</strong> Currently this only applies to differential equation
	 * models.
	 *
	 * @return {@code true} if the model is density based
	 */
	public boolean isDensity() {
		return false;
	}

	/**
	 * Check if the current model settings permit sample statistics. Fixation
	 * probabilities and times are examples of statistics based on samples.
	 * 
	 * @return <code>true</code> if sample statistics are permitted
	 */
	public boolean permitsSampleStatistics() {
		return false;
	}

	/**
	 * Check if the current model settings permit update statistics. Sojourn times
	 * are an example of statistics based on updates.
	 * 
	 * @return <code>true</code> if update statistics are permitted
	 */
	public boolean permitsUpdateStatistics() {
		return false;
	}

	/**
	 * Reset statistics and get ready to start new collection.
	 */
	public void resetStatisticsSample() {
		nStatisticsSamples = 0;
		nStatisticsFailed = 0;
		statisticsSampleNew = true;
	}

	/**
	 * Clear statistics sample and get ready to collect next sample.
	 */
	public void initStatisticsSample() {
		statisticsSampleNew = false;
	}

	/**
	 * Clear statistics sample and get ready to collect next sample.
	 */
	public void initStatisticsFailed() {
		nStatisticsFailed++;
	}

	/**
	 * Signal that statistics sample is ready to process.
	 */
	public void readStatisticsSample() {
		if (!statisticsSampleNew) {
			nStatisticsSamples++;
			statisticsSampleNew = true;
		}
	}

	/**
	 * Gets the number of statistics samples collected so far.
	 * 
	 * @return the number of samples
	 */
	public int getNStatisticsSamples() {
		return nStatisticsSamples;
	}

	/**
	 * Gets the number of failed statistics samples.
	 * 
	 * @return the number of samples
	 */
	public int getNStatisticsFailed() {
		return nStatisticsFailed;
	}

	/**
	 * The flag to indicate whether to start new statistics sample.
	 */
	protected boolean statisticsSampleNew = true;

	/**
	 * Number of statistics samples collected.
	 */
	protected int nStatisticsSamples = 0;

	/**
	 * Number of failed statistics samples.
	 */
	protected int nStatisticsFailed = 0;

	/**
	 * The container for collecting statistics samples.
	 */
	protected FixationData fixData = null;

	/**
	 * Gets the statistics sample of the fixation data.
	 * 
	 * @return the statistics sample
	 */
	public FixationData getFixationData() {
		return fixData;
	}

	/**
	 * Checks if model deals with continuous traits.
	 * 
	 * @return <code>true</code> if traits are continuous
	 */
	public boolean isContinuous() {
		return this instanceof CModel;
	}

	/**
	 * The type of the model: IBS, ODE, SDE, or PDE.
	 */
	protected ModelType type;

	/**
	 * Gets the type of the model.
	 * 
	 * @return the type of the model
	 */
	public ModelType getType() {
		return type;
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
	 * @see Features.Payoffs#getMinPayoff()
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
	 * @see Features.Payoffs#getMaxPayoff()
	 */
	public abstract double getMaxScore(int id);

	/**
	 * Calculates and returns the absolute fitness minimum. This is important to
	 * <ol>
	 * <li>determine probabilities or rates for adopting the trait of another
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
	 * <li>determine probabilities or rates for adopting the trait of another
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
	 * current state of the simulation. For example, models with discrete traits
	 * (such as 2x2 games, see {@link org.evoludo.simulator.modules.TBT}) return the
	 * average frequencies of each strategy type in the population(s), see
	 * {@link IBSDPopulation}. Similarly, models with continuous strategies (such as
	 * continuous snowdrift games, see {@link org.evoludo.simulator.modules.CSD})
	 * return the mean, minimum and maximum trait value(s) in the population(s), see
	 * {@link IBSMCPopulation}. The status message is displayed along the bottom of
	 * the GUI.
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
		if (mode == Mode.DYNAMICS)
			return "Time: " + Formatter.formatFix(getTime(), 2) + " ";
		if (mode == Mode.STATISTICS_SAMPLE) {
			int failed = getNStatisticsFailed();
			return "Samples: " + getNStatisticsSamples() + (failed > 0 ? " (failed: " + failed + ")" : "");
		}
		if (mode == Mode.STATISTICS_UPDATE)
			return "Samples: " + (int) (getUpdates() / getTimeStep()) + " ";
		return "";
	}

	/**
	 * Gets the elapsed time in model. Time is measured in generations. By default,
	 * non-IBS models do not track updates.
	 * 
	 * @return the elapsed time in generations
	 * 
	 * @see IBS#updates
	 */
	public double getUpdates() {
		return time;
	}

	/**
	 * Returns the elapsed time measured in terms of the rates at which events
	 * happen. The time increments of microscopic updates depend e.g. on the size or
	 * the fitness of the population. In large populations with high fitness many
	 * events happen per unit time and hence the increments are smaller. In contrast
	 * in small populations with low fitness fewer events happen and consequently
	 * more time elapses between subsequent events.
	 * 
	 * @return the elapsed time based on rates of events
	 * 
	 * @see #time
	 */
	public double getTime() {
		// Default models track time directly; IBS overrides if needed.
		return time;
	}

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
		int skip = 0;
		for (Module<?> mod : species) {
			int nt = mod.getNTraits();
			System.arraycopy(mod.getTraitNames(), 0, names, skip, nt);
			skip += nt;
		}
		return names;
	}

	/**
	 * Return the name of the mean trait with index {@code index} or {@code null} if
	 * index is invalid.
	 *
	 * @param index the index of the mean trait
	 * @return the name of mean trait with index {@code index}
	 */
	public String getMeanName(int index) {
		for (Module<?> mod : species) {
			int nt = mod.getNTraits();
			if (index < nt) {
				if (mod.getActiveTraits()[index])
					return mod.getTraitName(index);
				return null;
			}
			index -= nt;
		}
		return null;
	}

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
		throw new UnsupportedOperationException("getMeanTraitAt not implemented");
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
		throw new UnsupportedOperationException("getTraitNameAt not implemented");
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
		throw new UnsupportedOperationException("getMeanFitnessAt not implemented");
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
	public boolean isConnected() {
		return connect;
	}

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
	 * dissipative terms in the differential equations. For example, in certain
	 * ecological modules the patch-quality dynamics prevents time reversal, i.e.
	 * results are numerically unstable due to exponential amplification of
	 * deviations in dissipative systems.
	 *
	 * @param reversed the request whether time should be reversed.
	 *
	 * @see org.evoludo.simulator.models.ODE#setTimeReversed
	 *      ODE.setTimeReversed
	 * @see org.evoludo.simulator.models.SDE#setTimeReversed
	 *      SDE.setTimeReversed
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
	 * Gets the next generation for which stopping the model execution has been
	 * requested.
	 * 
	 * @return the next requested stop
	 */
	public double getNextHalt() {
		// watch out for models that allow time reversal!
		// timeStop and timeRelax can be positive or negative
		// note: use getUpdates to ensure relaxation works for all models.
		// DE models may not have an update count but have time
		double updt = getUpdates();
		if (isTimeReversed()) {
			// time is 'decreasing' find next smaller milestone
			double halt = timeStop < updt ? timeStop : Double.NEGATIVE_INFINITY;
			double relax = (Math.abs(timeRelax) > 1e-8 && timeRelax < updt) ? timeRelax
					: Double.NEGATIVE_INFINITY;
			return Math.max(halt, relax);
		}
		// time is 'increasing'
		double halt = timeStop > updt ? timeStop : Double.POSITIVE_INFINITY;
		double relax = (Math.abs(timeRelax) > 1e-8 && timeRelax > updt) ? timeRelax : Double.POSITIVE_INFINITY;
		return Math.min(halt, relax);
	}

	/**
	 * Indicates the interval (measured in generations) after which models report
	 * updates on their current state. for example, the GUI gets updated whenever
	 * <code>timeStep</code> generations (or fractions thereof) have elapsed.
	 * <p>
	 * <strong>Note:</strong> <code>timeStep&lt;0</code> disables reporting;
	 * <code>timeStep=0</code> reports every single update.
	 */
	protected double timeStep = 1.0;

	/**
	 * Set the report interval, i.e. number of updates in one step (see
	 * {@link EvoLudo#modelNext()} measured in generations (or fractions thereof).
	 *
	 * @param aValue the new report interval
	 */
	public void setTimeStep(double aValue) {
		timeStep = Math.max(0.0, aValue);
	}

	/**
	 * Get the interval between subsequent reports to the GUI or the simulation
	 * controller measured in generations (or fractions thereof).
	 * 
	 * @return the report interval
	 */
	public double getTimeStep() {
		return timeStep;
	}

	/**
	 * Command line option to set the number of generations between reports for
	 * {@link EvoLudo#modelNext()}.
	 */
	public final CLOption cloTimeStep = new CLOption("timestep", "1", CLOCategory.Model,
			"--timestep <s>  report frequency in generations", new CLODelegate() {
				@Override
				public boolean parse(String arg) {
					setTimeStep(Double.parseDouble(arg));
					return true;
				}
			});

	/**
	 * The relaxation time for simulations measured in generations.
	 * <p>
	 * <strong>Note:</strong> {@code timeRelax} is set with the command line
	 * option <code>--timerelax</code>
	 * 
	 * @see #cloTimeRelax
	 */
	protected double timeRelax;

	/**
	 * Sets the number of generations to relax the initial configuration of the
	 * active {@link Model}. In interactive mode (with GUI) the active {@link Model}
	 * starts running upon launch and stop after {@code timeRelax}.
	 * 
	 * @param relax the number of generations
	 * 
	 * @see #timeRelax
	 */
	public void setTimeRelax(double relax) {
		timeRelax = relax;
	}

	/**
	 * Gets the number of generations to relax the initial configuration of the
	 * active {@link Model}.
	 * 
	 * @return the number of generations
	 * 
	 * @see #timeRelax
	 */
	public double getTimeRelax() {
		return timeRelax;
	}

	/**
	 * Command line option to set the number of generations to relax the model from
	 * the initial configuration. After relaxation the model is assumed to be close
	 * to its (thermal) equilibrium. In particular, the system should be ready for
	 * measurements such as the trait abundances, their fluctuations or the local
	 * trait configurations in structured populations.
	 */
	public final CLOption cloTimeRelax = new CLOption("timerelax", "0", CLOCategory.Model,
			"--timerelax <r>  relaxation time in generations", new CLODelegate() {
				@Override
				public boolean parse(String arg) {
					setTimeRelax(CLOParser.parseDouble(arg));
					return true;
				}
			});

	/**
	 * Running simulations are halted when <code>time &ge; timeStop</code> holds for
	 * the first time. This is useful to indicate the end of simulations or to
	 * generate (graphical) snapshots in the GUI after a specified amount of time
	 * has elapsed.
	 * <p>
	 * <strong>Note:</strong> {@code timeStop} is set with the command line
	 * option <code>--timestop</code> (or <code>-g</code>),
	 * 
	 * @see #cloTimeStop
	 */
	protected double timeStop;

	/**
	 * Sets the number of generations after which the active {@link Model} is
	 * halted.
	 * 
	 * @param timeStop the number of generations
	 * 
	 * @see #timeStop
	 */
	public void setTimeStop(double timeStop) {
		this.timeStop = timeStop;
	}

	/**
	 * Gets the number of generations after which the active {@link Model} is
	 * halted.
	 * 
	 * @return the number of generations
	 * 
	 * @see #timeStop
	 */
	public double getTimeStop() {
		return timeStop;
	}

	/**
	 * Command line option to set the number of generations after which to stop the
	 * model calculations. Model execution can be resumed afterwards.
	 */
	public final CLOption cloTimeStop = new CLOption("timestop", "never", CLOCategory.Model,
			"--timestop <h>   halt execution after <h> generations", new CLODelegate() {
				@Override
				public boolean parse(String arg) {
					if (cloTimeStop.getDefault().equals(arg))
						setTimeStop(Double.POSITIVE_INFINITY);
					else
						setTimeStop(CLOParser.parseDouble(arg));
					return true;
				}
			});

	/**
	 * The number of statistical samples to collect before returning the results.
	 * <p>
	 * <strong>Note:</strong> {@code nSamples} is set with the command line
	 * option <code>--samples</code>
	 * 
	 * @see #cloSamples
	 */
	protected double nSamples;

	/**
	 * Sets the number of statistical samples taken after which the active
	 * {@link Model} is halted.
	 * 
	 * @param nSamples the number of samples
	 * 
	 * @see #nSamples
	 */
	public void setNSamples(double nSamples) {
		this.nSamples = nSamples;
	}

	/**
	 * Gets the number of statistical samples after which the active {@link Model}
	 * is halted.
	 * 
	 * @return the number of statistical samples
	 * 
	 * @see #nSamples
	 */
	public double getNSamples() {
		return nSamples;
	}

	/**
	 * Command line option to set the number of samples to take for statistical
	 * measurements.
	 */
	public final CLOption cloSamples = new CLOption("samples", "-1", CLOCategory.Simulation,
			"--samples <s>   number of samples for statistics", new CLODelegate() {
				@Override
				public boolean parse(String arg) {
					setNSamples(CLOParser.parseDouble(arg));
					return true;
				}
			});

	@Override
	public void collectCLO(CLOParser parser) {
		if (markers != null)
			parser.addCLO(markers.clo);
		parser.addCLO(cloTimeStep);
		parser.addCLO(cloTimeStop);
		parser.addCLO(cloTimeRelax);
		Module<?> module = getSpecies(0);
		// cannot use permitsSampleStatistics and permitsUpdateStatistics because
		// they also check parameters that have not yet been set
		if (module instanceof HasHistogram.StatisticsProbability
				|| module instanceof HasHistogram.StatisticsTime)
			parser.addCLO(cloSamples);
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
	public void encodeState(StringBuilder plist) {
		plist.append(Plist.encodeKey("Time", time));
		plist.append(Plist.encodeKey("Model", type.toString()));
	}

	/**
	 * Restore the state encoded in the <code>plist</code> inspired <code>map</code>
	 * of {@code key, value}-pairs.
	 * 
	 * @param plist the map of {@code key, value}-pairs
	 * @return <code>true</code> if successful
	 * 
	 * @see org.evoludo.util.Plist
	 * @see org.evoludo.util.PlistReader
	 * @see org.evoludo.util.PlistParser
	 */
	public boolean restoreState(Plist plist) {
		time = (Double) plist.get("Time");
		// note: model type already read, otherwise we wouldn't be here
		return true;
	}
}
