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

package org.evoludo.simulator.modules;

import java.awt.Color;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.logging.Logger;

import org.evoludo.math.ArrayMath;
import org.evoludo.math.RNGDistribution;
import org.evoludo.simulator.ColorMap;
import org.evoludo.simulator.EvoLudo;
import org.evoludo.simulator.Geometry;
import org.evoludo.simulator.models.IBS;
import org.evoludo.simulator.models.IBS.HasIBS;
import org.evoludo.simulator.models.IBSC;
import org.evoludo.simulator.models.IBSD;
import org.evoludo.simulator.models.Model.Type;
import org.evoludo.simulator.models.ODEEuler;
import org.evoludo.simulator.models.Model.ChangeListener.PendingAction;
import org.evoludo.simulator.models.IBSPopulation;
import org.evoludo.simulator.models.Model;
import org.evoludo.simulator.models.ODEEuler.HasODE;
import org.evoludo.simulator.models.PDERD;
import org.evoludo.simulator.models.PDERD.HasPDE;
import org.evoludo.simulator.models.SDEEuler.HasSDE;
import org.evoludo.simulator.views.HasDistribution;
import org.evoludo.simulator.views.HasHistogram;
import org.evoludo.simulator.views.HasMean;
import org.evoludo.simulator.views.HasPhase2D;
import org.evoludo.simulator.views.HasPop2D;
import org.evoludo.simulator.views.HasS3;
import org.evoludo.util.CLOParser;
import org.evoludo.util.CLOProvider;
import org.evoludo.util.CLOption;
import org.evoludo.util.CLOption.CLODelegate;
import org.evoludo.util.Formatter;

/**
 * Parent class of all EvoLudo modules.
 * 
 * @author Christoph Hauert
 */
public abstract class Module implements Features, Model.MilestoneListener, CLOProvider, Runnable {

	/**
	 * The name of the species. Mainly used in multi-species modules.
	 */
	protected String name;

	/**
	 * Gets the name of this species
	 * 
	 * @return the name of the species
	 */
	public String getName() {
		return name == null ? "" : name;
	}

	/**
	 * Sets the name of this species. If {@code name == null} the name is
	 * cleared.
	 * 
	 * @param name the new name of the species
	 */
	public void setName(String name) {
		this.name = name;
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
	 * Reference to current model. Note: actual instance is a sub-interface of Model
	 * (currently Model.ODE/SDE/PDE or Model.IBS)
	 */
	protected Model model;

	/**
	 * List with all species in module including this one.
	 * 
	 * <h3>Important:</h3>
	 * List should be shared with other populations (to simplify bookkeeping) but
	 * the species list <em>CANNOT</em> be static! Otherwise it is impossible to run
	 * multiple instances of modules/models concurrently!
	 */
	ArrayList<? extends Module> species;

	/**
	 * In multi-species modules each species is represented by a Module, see
	 * {@link #species}. The {@code ID} provides a unique identifier for each
	 * species.
	 */
	final int ID;

	/**
	 * Gets unique identifier {@code ID} of species.
	 * 
	 * @return the unique identifier of the species.
	 */
	public int getID() {
		return ID;
	}

	/**
	 * The list of markers on graphs. For example to mark fixed points.
	 */
	ArrayList<double[]> markers;

	/**
	 * Instantiate a new Module with {@code engine} and {@code partner}.
	 * If {@code partner == null} this is a single species module and
	 * interactions within species ({@code opponent == this} holds).
	 * 
	 * @param engine  the pacemeaker for running the model
	 * @param partner the module of the partner species or {@code null} for single
	 *                species modules
	 */
	protected Module(EvoLudo engine, Module partner) {
		this.engine = engine;
		logger = engine.getLogger();
		if (partner == null) {
			ID = 0;
			opponent = this;
			return;
		}
		ID = partner.species.size();
		species = partner.species;
		opponent = partner;
		partner.opponent = this;
	}

	@Override
	public void modelLoaded() {
		model = engine.getModel();
	}

	/**
	 * Gets module of species at index {@code idx}.
	 * 
	 * @param idx the index of species to retrieve
	 * @return the module of species
	 */
	public Module getSpecies(int idx) {
		if (idx < 0 || idx >= species.size())
			return null;
		return species.get(idx);
	}

	/**
	 * Gets list with all species.
	 * 
	 * @return the list with all species
	 */
	public ArrayList<? extends Module> getSpecies() {
		return species;
	}

	/**
	 * Gets the number of different species in this module.
	 * 
	 * @return the number of species in this module
	 */
	public int getNSpecies() {
		return species.size();
	}

	/**
	 * Returns identifier of the active module, e.g. 2x2 games in
	 * {@link org.evoludo.simulator.modules.TBT} return "2x2". This corresponds to
	 * the argument for the {@code --module} option to load a particular module.
	 * 
	 * @return the identifying key of this module
	 */
	public abstract String getKey();

	/**
	 * Returns brief description of the active module, including title, author and
	 * version. For example, 2x2 games in {@link org.evoludo.simulator.modules.TBT}
	 * return "Title: 2x2 Games\nAuthor: Christoph Hauert\nTime evolution of
	 * cooperators and defectors in different population structures."
	 * <p>
	 * <strong>Note:</strong> newline characters, '\n', are acceptable in returned
	 * String. If necessary they will be replaced by {@literal <br/>
	 * } e.g. for ePub's. Do <em>not</em> use HTML formatting in info string.
	 * 
	 * @return the description of active module
	 */
	public abstract String getInfo();

	/**
	 * Returns title of active module, e.g. 2x2 games in
	 * {@link org.evoludo.simulator.modules.TBT} returns "2x2 Games".
	 * 
	 * @return the title of active module
	 */
	public abstract String getTitle();

	/**
	 * Version identifier of this module. This is typically used together with the
	 * git commit to uniquely identify the code base.
	 * 
	 * @return the version String
	 * 
	 * @see EvoLudo#getVersion()
	 * @see EvoLudo#getGit()
	 */
	public abstract String getVersion();

	/**
	 * Load new module and perform basic initializations.
	 * 
	 * @see EvoLudo#loadModule(String)
	 * @see Model.MilestoneListener#modelLoaded()
	 */
	public void load() {
		map2fitness = new Map2Fitness(Map2Fitness.Maps.NONE);
		// currently only the Test module uses neither Discrete nor Continuous classes.
		if (species == null)
			species = new ArrayList<Module>();
		engine.addMilestoneListener(this);
		if (this instanceof Model.ChangeListener)
			engine.addChangeListener((Model.ChangeListener) this);
	}

	/**
	 * Unload module and free all resources.
	 * 
	 * @see EvoLudo#unloadModule()
	 * @see Model.MilestoneListener#modelUnloaded()
	 */
	public void unload() {
		traitName = null;
		traitColor = null;
		trajectoryColor = null;
		map2fitness = null;
		cloFitnessMap.clearKeys();
		cloPlayerUpdate.clearKeys();
		opponent = this;
		engine.removeMilestoneListener(this);
		if (this instanceof Model.ChangeListener)
			engine.removeChangeListener((Model.ChangeListener) this);
	}

	/**
	 * Check all parameters. After this call all parameters must be consistent. If
	 * parameter adjustments require a reset then this method must return
	 * {@code true}.
	 * <p>
	 * <strong>Note:</strong> All parameter changes that don't require a reset can
	 * be made on the fly, in particular also while a model is running.
	 * 
	 * @return {@code true} to trigger reset
	 * 
	 * @see EvoLudo#modelCheck()
	 * @see EvoLudo#paramsDidChange()
	 */
	public boolean check() {
		return false;
	}

	/**
	 * Initialize Module based on current parameters. This requires that parameters
	 * are consistent, i.e. {@code check()} has been called before.
	 * <p>
	 * <strong>Note:</strong> The difference between {@link #init()} and
	 * {@link #reset()} is that {@link #init()} leaves population structures
	 * untouched and only initializes the strategies.
	 * 
	 * @see EvoLudo#modelInit()
	 * @see Model.MilestoneListener#modelDidReinit()
	 */
	public void init() {
	}

	/**
	 * Reset Module based on current parameters. This requires that parameters are
	 * consistent, i.e. {@code check()} has been called before.
	 * <p>
	 * <strong>Note:</strong> The difference between {@link #init()} and
	 * {@link #reset()} is that {@link #init()} leaves population structures
	 * untouched and only initializes the strategies.
	 * 
	 * @see EvoLudo#modelReset()
	 * @see Model.MilestoneListener#modelDidReset()
	 */
	public void reset() {
		interaction = null;
		reproduction = null;
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * Default run-loop for modules. Opportunity to override in subclasses for
	 * running customized simulations. Currently only called from
	 * {@link org.evoludo.simulator.EvoLudoJRE#simulation(String[])} if custom
	 * simulation class is specified in {@code jar} file.
	 */
	@Override
	public void run() {
	}

	/**
	 * Determine whether this module supports the Model type {@code type}.
	 * 
	 * @param type the model type to check
	 * @return {@code true} if Model type {@code type} is supported.
	 * 
	 * @see Model.Type
	 */
	public boolean hasSupport(Model.Type type) {
		switch (type) {
			case ODE:
				return this instanceof HasODE;
			case SDE:
				return this instanceof HasSDE;
			case PDE:
				return this instanceof HasPDE;
			case IBS:
				return this instanceof HasIBS;
			default: // unreachable
				return false;
		}
	}

	/**
	 * Opportunity to supply custom individual based simulations.
	 * 
	 * @return the custom IBSPopulation or {@code null} to use default.
	 */
	public IBSPopulation createIBSPop() {
		return null;
	}

	/**
	 * The index for the vacant type (empty site) or {@code -1} if Module does not
	 * admit empty sites.
	 */
	public int VACANT = -1;

	/**
	 * Get the index for the vacant type or {@code -1} if Module does not admit
	 * empty sites. In {@link Discrete} modules this is the index of the vacant type
	 * e.g. in the name or color vectors. Currently unused in {@link Continuous}
	 * modules.
	 * 
	 * @return the index of the vacant type
	 */
	public int getVacant() {
		return VACANT;
	}

	/**
	 * Get the index of dependent type or {@code -1} if Module does not have an
	 * dependent type.
	 * <p>
	 * <strong>Notes:</strong>
	 * <ul>
	 * <li>Dependent types are used by replicator type models where the frequencies
	 * of all types must sum up to one. Currently only used by Discrete modules.
	 * <li>Density modules do not have dependent types.
	 * <li>By default use vacant type as the dependent
	 * </ul>
	 * 
	 * @return the index of the vacant type
	 */
	public int getDependent() {
		if (model instanceof Model.DE && ((Model.DE) model).isDensity())
			return -1;
		return VACANT;
	}

	/**
	 * The number of traits in this module
	 */
	protected int nTraits = -1;

	/**
	 * Gets the number of traits in this Module. For example, in 2x2 Games
	 * {@link org.evoludo.simulator.modules.TBT} this returns {@code 2}.
	 * 
	 * @return the number of traits
	 */
	public int getNTraits() {
		return nTraits;
	}

	/**
	 * Sets the number of traits in this Module.
	 * 
	 * <h3>Note:</h3>
	 * Changing the number of traits is a fairly involved change because it requires
	 * re-allocating most memory. Most Modules set {@code nTraits} early on and
	 * leave it unchanged.
	 * 
	 * @param nTraits the number of traits
	 */
	public void setNTraits(int nTraits) {
		// prevent requesting re-parsing of CLOs on initial load
		if (this.nTraits > 0 && this.nTraits != nTraits)
			engine.requestAction(PendingAction.CLO);
		this.nTraits = nTraits;
	}

	/**
	 * The array indicating which traits are active.
	 */
	boolean[] active;

	/**
	 * The number of active traits. {@code nActive &lt;= nTraits} must hold.
	 */
	protected int nActive;

	/**
	 * Sets which traits are currently active.
	 * 
	 * @param active the array indicating active traits (or {@code null} if all
	 *               traits are active)
	 */
	public void setActiveTraits(boolean[] active) {
		if (active == null) {
			nActive = nTraits;
			return;
		}
		this.active = active;
		int n = 0;
		for (int i = 0; i < active.length; i++)
			if (active[i])
				n++;
		nActive = n;
	}

	/**
	 * Gets an array indicating which traits are active.
	 * 
	 * @return the array of active traits
	 */
	public boolean[] getActiveTraits() {
		if (active == null || active.length != nTraits) {
			active = new boolean[nTraits];
			Arrays.fill(active, true);
		}
		return active;
	}

	/**
	 * Gets the number of active traits.
	 * 
	 * @return the number of active traits
	 */
	public int getNActive() {
		return nActive;
	}

	/**
	 * Names of traits.
	 */
	protected String[] traitName;

	/**
	 * Gets the name of the trait with index {@code trait}.
	 * 
	 * @param trait the index of the trait
	 * @return the name of the trait
	 */
	public String getTraitName(int trait) {
		return getTraitNames()[trait % nTraits];
	}

	/**
	 * Gets the names of all traits. By default returns
	 * <code>{ "Trait A", "Trait B",...}</code>.
	 * 
	 * @return the array with the names of all traits
	 */
	public String[] getTraitNames() {
		if (traitName == null)
			setTraitNames(null);
		return traitName;
	}

	/**
	 * Set trait names. Underscores, '_', are converted to spaces.
	 * <p>
	 * <strong>Note:</strong> If {@code names == null} or
	 * {@code names.length &lt; nTraits} then additional traits are named "Trait A",
	 * "Trait B", etc. If {@code names.length &gt; nTraits} then excess names are
	 * ignored.
	 * 
	 * @param names the names of the traits
	 */
	public void setTraitNames(String[] names) {
		if (traitName == null || traitName.length != nTraits)
			traitName = new String[nTraits];
		int idx = 0;
		if (names != null) {
			for (String n : names)
				traitName[idx++] = n.replace('_', ' ').trim();
		}
		for (int i = idx; i < nTraits; i++)
			traitName[i] = "Trait " + (char) ('A' + i - idx);
	}

	/**
	 * The array with default colors for traits.
	 * 
	 * <strong>Important:</strong> if {@code defaultColor} is set already here
	 * (static allocation), headless mode for simulations is prevented. In order to
	 * avoid this, simply allocate and assign the colors in the constructor.
	 */
	protected static Color[] defaultColor;

	/**
	 * The array with trait colors.
	 * 
	 * <strong>Important:</strong> if {@code traitColor} is set already here
	 * (static allocation), headless mode for simulations is prevented. In order to
	 * avoid this, simply allocate and assign the colors in the constructor.
	 */
	protected Color[] traitColor;

	/**
	 * Gets the color for the trait with index {@code trait}.
	 * 
	 * @param trait the index of the trait
	 * @return the color of the trait
	 */
	public Color getTraitColor(int trait) {
		return getTraitColors()[trait];
	}

	/**
	 * Gets the colors for all traits.
	 * 
	 * @return the array of trait colors
	 */
	public Color[] getTraitColors() {
		if (traitColor == null)
			setTraitColors(null);
		return traitColor;
	}

	/**
	 * Sets trait colors specified in {@code colors}. If less than {@code nTraits}
	 * colors are specified, additional traits are colored using the default colors.
	 * If still not enough, random colors are generated. Both {@link Discrete} and
	 * {@link Continuous} modules require {@code 2*nTraits} colors. The meaning of
	 * the second set of {@code nTraits} colors depends on the trait type:
	 * <dl>
	 * <dt>discrete
	 * <dd>the colors of individuals that switched strategy since the last update
	 * <dt>continuous
	 * <dd>the colors for the mean &#177; standard deviation, see e.g.
	 * {@link org.evoludo.simulator.MVMean}.
	 * </dl>
	 * Specifying the second set of colors is optional. By default they are
	 * automatically generated as lighter versions of the base colors.
	 * 
	 * @param colors the array of colors for the different traits
	 * @return {@code true} always signal that colors have changed (too
	 *         difficult and of too little importance to check whether colors
	 *         remained the same)
	 */
	public boolean setTraitColors(Color[] colors) {
		// assign default colors
		if (defaultColor == null)
			defaultColor = new Color[] {
					Color.RED,
					Color.BLUE,
					Color.YELLOW,
					Color.GREEN,
					Color.MAGENTA,
					Color.ORANGE,
					Color.PINK,
					Color.CYAN
			};
		int nTraits2 = nTraits + nTraits;
		int rColors = 0;
		if (colors == null) {
			colors = new Color[nTraits];
			System.arraycopy(defaultColor, 0, colors, 0, Math.min(nTraits, defaultColor.length));
			rColors = nTraits - defaultColor.length;
		} else if (colors.length < nTraits) {
			// add default colors
			Color[] cols = new Color[nTraits];
			System.arraycopy(colors, 0, cols, 0, colors.length);
			System.arraycopy(defaultColor, 0, cols, colors.length,
					Math.min(nTraits - colors.length, defaultColor.length));
			colors = cols;
			rColors = nTraits - colors.length - defaultColor.length;
		}
		if (rColors > 0) {
			// add random colors if needed - do not use the shared RNG to prevent
			// interfering with reproducibility
			RNGDistribution rng = new RNGDistribution.Uniform();
			for (int n = 0; n < rColors; n++)
				colors[nTraits - n] = new Color(rng.random0n(256), rng.random0n(256), rng.random0n(256));
		}

		// now at least nTraits colors
		if (this instanceof Discrete) {
			// discrete traits and colors
			if (colors.length == nTraits2) {
				traitColor = colors;
				return true;
			}
			Color[] cols = new Color[nTraits2];
			if (colors.length > nTraits2) {
				System.arraycopy(colors, 0, cols, 0, nTraits2);
				traitColor = cols;
				return true;
			}
			System.arraycopy(colors, 0, cols, 0, colors.length);
			for (int n = colors.length; n < nTraits2; n++)
				// NOTE: Color.brighter() does not work on pure colors.
				cols[n] = ColorMap.blendColors(colors[n % nTraits], Color.WHITE, 0.333);
			traitColor = cols;
			return true;
		}
		// continuous traits and colors
		Color[] cColor = new Color[3 * nTraits]; // continuous strategies require colors for min, mean, max of each
													// trait
		for (int n = 0; n < nTraits; n++) {
			Color color = colors[n];
			cColor[n] = color; // mean
			// NOTE: Color.brighter() does not work on pure colors.
			Color brighter = ColorMap.blendColors(color, Color.WHITE, 0.333);
			cColor[n + nTraits] = brighter; // min
			cColor[n + nTraits2] = brighter; // max
		}
		traitColor = cColor;
		return true;
	}

	/**
	 * Color for trajectories.
	 * 
	 * <strong>Important:</strong> if {@code trajectoryColor} is set already
	 * here, headless mode for simulations is prevented. In order to avoid this,
	 * simply allocate and assign the colors in the constructor.
	 */
	protected Color trajectoryColor;

	/**
	 * Gets the color of trajectories.
	 * 
	 * @return the trajectory color (defaults to black)
	 */
	public Color getTrajectoryColor() {
		if (trajectoryColor == null)
			return Color.BLACK;
		return trajectoryColor;
	}

	/**
	 * Sets color of trajectories. Default color is black. If {@code color} is
	 * {@code null} reverts to default.
	 * <p>
	 * <strong>Note:</strong> Using transparent colors is useful for models with
	 * noise (simulations or SDE's) to identify regions of attraction (e.g.
	 * stochastic limit cycles) as darker shaded areas because the population spends
	 * much of its time there while less frequently visited areas of the phase space
	 * are lighter in color.
	 * 
	 * @param color the color for the trajectories
	 * @return {@code true} if color changed
	 */
	public boolean setTrajectoryColor(Color color) {
		boolean changed = !getTrajectoryColor().equals(color);
		trajectoryColor = color;
		return changed;
	}

	/**
	 * Opportunity for modules to make adjustments to the color map in graphs such
	 * as {@link org.evoludo.simulator.MVPop2D} or
	 * {@link org.evoludo.simulator.MVPop3D}. By default no changes are made.
	 * 
	 * @param <T>      the type of the color map
	 * @param colorMap the color map
	 * @return the processed color map
	 * 
	 * @see org.evoludo.simulator.ColorMap
	 */
	public <T> ColorMap<T> processColorMap(ColorMap<T> colorMap) {
		return colorMap;
	}

	/**
	 * Calculates and returns the minimum payoff/score of an individual. This value
	 * is important for converting payoffs/scores into probabilities, for scaling
	 * graphical output and some optimizations.
	 * 
	 * @return the minimum payoff/score
	 * 
	 * @see #getMinScore()
	 * @see Model#processMinScore(int, double)
	 */
	public abstract double getMinGameScore();

	/**
	 * Calculates and returns the maximum payoff/score of an individual. This value
	 * is important for converting payoffs/scores into probabilities, for scaling
	 * graphical output and some optimizations.
	 * 
	 * @return the maximum payoff/score
	 * 
	 * @see #getMaxScore()
	 * @see Model#processMaxScore(int, double)
	 */
	public abstract double getMaxGameScore();

	/**
	 * Calculates and returns the minimum payoff/score of individuals in monomorphic
	 * populations.
	 * 
	 * @return the minimum payoff/score in monomorphic populations
	 */
	public abstract double getMinMonoGameScore();

	/**
	 * Calculates and returns the maximum payoff/score of individuals in monomorphic
	 * populations.
	 * 
	 * @return the maximum payoff/score in monomorphic populations
	 */
	public abstract double getMaxMonoGameScore();

	/**
	 * Similar to {@link #getMinGameScore()} but takes into account potential
	 * adjustments due to population structure or payoff accounting. This depends on
	 * the payoff accounting (averaged versus accumulated) as well as the
	 * {@link Geometry}. Since modules are agnostic of runtime details, the request
	 * is simply forwarded to the current {@link Model} together with the species ID
	 * for multi-species modules.
	 * 
	 * @return the minimum score
	 * 
	 * @see Model#processMinScore(int, double)
	 */
	public double getMinScore() {
		return model.processMinScore(ID, getMinGameScore());
	}

	/**
	 * Similar to {@link #getMaxGameScore()} but takes into account potential
	 * adjustments due to population structure or payoff accounting. This depends on
	 * the payoff accounting (averaged versus accumulated) as well as the
	 * {@link Geometry}. Since modules are agnostic of runtime details, the request
	 * is simply forwarded to the current {@link Model} together with the species ID
	 * for multi-species modules.
	 * 
	 * @return the maximum score
	 */
	public double getMaxScore() {
		return model.processMaxScore(ID, getMaxGameScore());
	}

	/**
	 * Checks whether dynamic is neutral, i.e. no selection acting on the different
	 * traits.
	 * 
	 * @return {@code true} if all payoffs identical
	 */
	public boolean isNeutral() {
		return (Math.abs(getMaxGameScore() - getMinGameScore()) < 1e-8);
	}

	/**
	 * Calculates and returns minimum score in monomorphic population. This depends
	 * on the payoff accounting (averaged versus accumulated) as well as the
	 * {@link Geometry}. Since modules are agnostic of runtime details, the request
	 * is simply forwarded to the current {@link Model} together with the species ID
	 * for multi-species modules.
	 * 
	 * @return the minimum monomorphic score
	 */
	public double getMinMonoScore() {
		return model.processMinScore(ID, getMinMonoGameScore());
	}

	/**
	 * Calculates and returns maximum score in monomorphic population. This depends
	 * on the payoff accounting (averaged versus accumulated) as well as the
	 * {@link Geometry}. Since modules are agnostic of runtime details, the request
	 * is simply forwarded to the current {@link Model} together with the species ID
	 * for multi-species modules.
	 * 
	 * @return the maximum monomorphic score
	 */
	public double getMaxMonoScore() {
		return model.processMaxScore(ID, getMaxMonoGameScore());
	}

	/**
	 * Calculates and returns the absolute fitness minimum. This is important to
	 * <ol>
	 * <li>determine probabilities or rates for adopting the strategy of another
	 * player,
	 * <li>optimize fitness based picking of individuals, and
	 * <li>scaling graphical output.
	 * </ol>
	 *
	 * @return the minimum fitness
	 * 
	 * @see #getMinScore()
	 */
	public double getMinFitness() {
		return map2fitness.map(getMinScore());
	}

	/**
	 * Calculates and returns the absolute fitness maximum. This is important to
	 * <ol>
	 * <li>determine probabilities or rates for adopting the strategy of another
	 * player,
	 * <li>optimize fitness based picking of individuals, and
	 * <li>scaling graphical output.
	 * </ol>
	 *
	 * @return the maximum fitness
	 * 
	 * @see #getMaxScore()
	 */
	public double getMaxFitness() {
		return map2fitness.map(getMaxScore());
	}

	/**
	 * Reference to Module of opponent. For Modules referring to intra-species
	 * interactions {@code opponent == this} must hold.
	 */
	Module opponent;

	/**
	 * Gets the opponent of this module/population. By default, for intra-species
	 * interactions, simply returns this module/population, i.e
	 * {@code opponent == this}.
	 * 
	 * @return the opponent of this population
	 */
	public Module getOpponent() {
		return opponent;
	}

	/**
	 * Sets the opponent of this module/population. By default, for intra-species
	 * interactions, {@code opponent == this} holds.
	 * 
	 * @param opponent the opponent of this population
	 */
	public void setOpponent(Module opponent) {
		this.opponent = opponent;
	}

	/**
	 * The geometry of population (interaction and reproduction graphs are the same)
	 */
	protected Geometry structure;

	/**
	 * Gets the geometry of the population.
	 * 
	 * @return the geometry of the population
	 */
	public Geometry getGeometry() {
		return structure;
	}

	/**
	 * Opportunity to supply {@link Geometry}, in case interaction and reproduction
	 * graphs are the same.
	 * 
	 * @return the new geometry
	 */
	public Geometry createGeometry() {
		if (structure == null)
			structure = new Geometry(engine, this, opponent);
		return structure;
	}

	/**
	 * The geometry of interaction structure
	 */
	protected Geometry interaction;

	/**
	 * The geometry of reproduction structure
	 */
	protected Geometry reproduction;

	/**
	 * Sets different geometries for interactions and reproduction.
	 * 
	 * @param interaction  the geometry for interactions
	 * @param reproduction the geometry for reproduction
	 * 
	 * @see Geometry
	 */
	public void setGeometries(Geometry interaction, Geometry reproduction) {
		this.interaction = interaction;
		this.reproduction = reproduction;
	}

	/**
	 * Gets the interaction geometry.
	 * 
	 * @return the interaction geometry
	 * 
	 * @see Geometry
	 */
	public Geometry getInteractionGeometry() {
		return interaction;
	}

	/**
	 * Gets the reproduction geometry.
	 * 
	 * @return the reproduction geometry
	 * 
	 * @see Geometry
	 */
	public Geometry getReproductionGeometry() {
		return reproduction;
	}

	/**
	 * Update rate for this species. Only used in multi-species modules.
	 */
	protected double speciesUpdateRate = 1.0;

	/**
	 * Sets the species update rate to {@code rate}. Only used in multi-species
	 * modules. Determines the relative rate at which this species is picked as
	 * compared to others.
	 * 
	 * @param rate the update rate of this species
	 * @return {@code true} if species update rate changed
	 * 
	 * @see EvoLudo#modelNext()
	 */
	public boolean setSpeciesUpdateRate(double rate) {
		boolean changed = (Math.abs(speciesUpdateRate - rate) > 1e-8);
		if (rate <= 0.0) {
			logger.warning("population update rate must be positive - ignored, using 1.");
			speciesUpdateRate = 1.0;
			return changed;
		}
		speciesUpdateRate = rate;
		return changed;
	}

	/**
	 * Gets the update rate of this species. Only used in multi-species modules.
	 * Determines the relative rate at which this species is picked as compared to
	 * others.
	 * 
	 * @return the species update rate
	 * 
	 * @see EvoLudo#modelNext()
	 */
	public double getSpeciesUpdateRate() {
		return speciesUpdateRate;
	}

	/**
	 * The population size.
	 */
	protected int nPopulation = 1000;

	/**
	 * Sets the population size. For models with vacancies this is the maximum
	 * population size. Currently this only affects IBS and SDE models. For SDE's it
	 * determines the magnitude of noise. For IBS this is a fundamental change,
	 * which mandates requesting to reset the model.
	 * 
	 * @param size the population size
	 * @return {@code true} if population size changed
	 * 
	 * @see EvoLudo#reset()
	 */
	public boolean setNPopulation(int size) {
		int oldNPopulation = nPopulation;
		nPopulation = Math.max(1, size);
		boolean changed = (nPopulation != oldNPopulation);
		engine.requiresReset(changed && model.isModelType(Type.IBS));
		return changed;
	}

	/**
	 * Gets the population size.
	 * <p>
	 * <strong>Note:</strong> Without vacant sites the population size is fixed. For
	 * ecological models with vacant sites this corresponds to the maximum
	 * population size.
	 * 
	 * @return the population size
	 */
	public int getNPopulation() {
		return nPopulation;
	}

	/**
	 * Type of initial configuration.
	 * 
	 * <h3>Note:</h3>
	 * Different models may use different {@code InitType}s. Consequently this
	 * method cannot accept a particular set of initialization types.
	 * 
	 * @see #cloInitType
	 */
	protected CLOption.Key initType;

	/**
	 * Sets the type of the initial configuration.
	 * 
	 * <h3>Note:</h3>
	 * Different models may use different {@code InitType}s. Consequently this
	 * method cannot accept a particular set of initialization types.
	 *
	 * @param type the type of the initial configuration
	 * 
	 * @see IBSD.InitType
	 * @see IBSC.InitType
	 * @see ODEEuler.InitType
	 * @see PDERD.InitType
	 */
	public void setInitType(CLOption.Key type) {
		if (type == null)
			type = getInitType();
		initType = type;
	}

	/**
	 * Gets the type of the initial configuration.
	 * 
	 * <h3>Note:</h3>
	 * Different models may use different {@code InitType}s. Consequently this
	 * method cannot return a particular set of initialization types.
	 *
	 * @return the type of the initial configuration
	 * 
	 * @see IBSD.InitType
	 * @see IBSC.InitType
	 * @see ODEEuler.InitType
	 * @see PDERD.InitType
	 */
	public CLOption.Key getInitType() {
		if (initType != null) 
			return initType;
		// return proper default
		if (model instanceof IBSD)
			return org.evoludo.simulator.models.IBSD.InitType.DEFAULT;
		if (model instanceof IBSC)
			return org.evoludo.simulator.models.IBSC.InitType.DEFAULT;
		if (model instanceof Model.PDE)	// check for PDEs first
			return org.evoludo.simulator.models.PDERD.InitType.DEFAULT;
		if (model instanceof Model.ODE)	// this holds for PDEs too
			return org.evoludo.simulator.models.ODEEuler.InitType.DEFAULT;
		// unreachable
		throw new Error("unknown model type!");
	}

	/**
	 * Death rate for ecological population updates.
	 */
	protected double deathRate = 1.0;

	/**
	 * Sets the death rate for ecological population updates.
	 * 
	 * @param rate the death rate
	 * @return {@code true} if the death rate changed
	 */
	public boolean setDeathRate(double rate) {
		double newrate = Math.max(0.0, rate);
		boolean changed = (Math.abs(newrate - deathRate) > 1e-7);
		deathRate = newrate;
		return changed;
	}

	/**
	 * Gets the death rate for ecological population updates.
	 * 
	 * @return the death rate
	 */
	public double getDeathRate() {
		return deathRate;
	}

	/**
	 * Probability of mutations, i.e. spontaneous changes of type/strategy.
	 */
	protected double pMutation = -1.0;

	/**
	 * Sets the mutation probability in one update to {@code aValue}. Mutations
	 * are disabled for negative values.
	 * <p>
	 * <strong>Note:</strong> During a reproduction event, or when attempting to
	 * imitate the type/strategy of another individual, mutations or random
	 * exploration may affect the outcome. The implementation of mutations depends
	 * on the model type. In particular whether types/strategies are discrete or
	 * continuous.
	 * 
	 * @param aValue the probability of a mutation.
	 * @return {@code true} if the mutation probability changed
	 */
	public boolean setMutationProb(double aValue) {
		if (aValue < 0.0) {
			// disable mutations
			if (pMutation < 0.0)
				return false;
			pMutation = -1.0;
			return true;
		}
		// enable mutations
		double newvalue = Math.min(aValue, 1.0);
		boolean changed = (Math.abs(newvalue - pMutation) > 1e-7);
		pMutation = newvalue;
		return changed;
	}

	/**
	 * Gets the probability of mutations.
	 * <p>
	 * <strong>Note:</strong> During a reproduction event or when attempting to
	 * imitate the type/strategy of another individual mutations or random
	 * exploration may affect the outcome. The implementation of mutations depends
	 * on the model type. In particular whether types/strategies are discrete or
	 * continuous.
	 * 
	 * @return the mutation probability
	 */
	public double getMutationProb() {
		return pMutation;
	}

	/**
	 * Map to convert score/payoff to fitness
	 */
	protected Map2Fitness map2fitness;

	/**
	 * Gets the score/payoff to fitness map.
	 * 
	 * @return the score-to-fitness map
	 */
	public Map2Fitness getMapToFitness() {
		return map2fitness;
	}

	/**
	 * Player update type.
	 * 
	 * @see #cloPlayerUpdate
	 */
	protected PlayerUpdateType playerUpdateType = PlayerUpdateType.IMITATE;

	/**
	 * Sets the player update type.
	 * 
	 * @param type the updating type for players
	 * @return {@code true} if player update type changed
	 */
	public boolean setPlayerUpdateType(PlayerUpdateType type) {
		if (type == null || type == playerUpdateType)
			return false;
		playerUpdateType = type;
		return true;
	}

	/**
	 * Gets the player update type.
	 * 
	 * @return the player update type
	 */
	public PlayerUpdateType getPlayerUpdateType() {
		return playerUpdateType;
	}

	/**
	 * The noise of the updating process of players.
	 */
	double playerUpdateNoise;

	/**
	 * Set the noise of the updating process of players. With less noise chances are
	 * higher to adopt the strategy of individuals even if they perform only
	 * marginally better. Conversely for large noise payoff differences matter less
	 * and the updating process is more random. For {@code noise==1} the process is
	 * neutral.
	 * 
	 * @param noise the noise when updating the trait
	 */
	public void setPlayerUpdateNoise(double noise) {
		playerUpdateNoise = Math.max(0.0, noise);
	}

	/**
	 * Get the noise of the updating process.
	 * 
	 * @return the noise when updating the trait
	 * 
	 * @see #setPlayerUpdateNoise(double)
	 */
	public double getPlayerUpdateNoise() {
		return playerUpdateNoise;
	}

	/**
	 * The probability of an error during the updating of the trait.
	 */
	double playerUpdateError;

	/**
	 * Set the error of the updating process. With probability {@code error} an
	 * individual fails to adopt a better performing trait or adopts an worse
	 * performing one. More specifically the range of updating probabilities is
	 * restricted to {@code [error, 1-error]} such that always a chance remains that
	 * the trait of a better performing individual is not adopted or the one of a
	 * worse performing one is adopted.
	 * 
	 * @param error the error when adopting the trait
	 */
	public void setPlayerUpdateError(double error) {
		playerUpdateError = Math.max(0.0, error);
	}

	/**
	 * Get the error of the updating process.
	 * 
	 * @return the error when adopting the trait
	 * 
	 * @see #setError(double)
	 */
	public double getPlayerUpdateError() {
		return playerUpdateError;
	}
	/**
	 * The interaction group size.
	 */
	protected int nGroup = 2;

	/**
	 * Sets the interaction group size. Changes trigger a request to reset the
	 * module.
	 * 
	 * @param size the interaction group size
	 * @return {@code true} if the group size changed
	 */
	public boolean setNGroup(int size) {
		int oldNGroup = nGroup;
		nGroup = Math.max(2, size);
		boolean changed = (nGroup != oldNGroup);
		engine.requiresReset(changed);
		return changed;
	}

	/**
	 * Gets the interaction group size.
	 * 
	 * @return the interaction group size
	 */
	public int getNGroup() {
		return nGroup;
	}

	public boolean addMarker(double[] aMark) {
		return addMarker(aMark, true);
	}

	/**
	 * Add marker to list of markers. Markers are provided as {@code double[]}
	 * arrays to indicate special frequencies/densities.
	 * <p>
	 * In multi-species modules the markers for each species are concatenated into a
	 * single array. The frequencies/densities of the marker for the first species
	 * are stored in <code>aMark[0]</code> through <code>aMark[n1]</code> where
	 * <code>n1</code> denotes the number of traits in the first species. The
	 * current frequencies/densities of the second species are stored in
	 * <code>aMark[n1+1]</code> through <code>aMark[n1+n2]</code> where
	 * <code>n2</code> denotes the number of traits in the second species, etc.
	 * 
	 * @param aMark the marker to add
	 * @param filled the flag to indicate whether the marker should be filled 
	 * @return {@code true} if successfull
	 * 
	 * @see org.evoludo.simulator.models.ODEEuler#yt
	 */
	public boolean addMarker(double[] aMark, boolean filled) {
		int nt = nTraits;
		if (species.size()>1){
			nt = 0;
			for (Module pop : species)
				nt += pop.getNTraits();
		}
		if (aMark.length != nt)
			return false;
		if (markers == null) {
			markers = new ArrayList<>(5);
			// share marker array with other modules in multi-species
			for (Module pop : species)
				pop.markers = markers;
		}
		// important: data buffers for ParaGraph & co store time in first element
		return markers.add(ArrayMath.insert(aMark, filled ? 1.0 : -1.0, 0));
	}

	/**
	 * Get the list of markers. This serves to mark special values in different
	 * kinds of graphs.
	 * 
	 * @return the list of markers
	 */
	public ArrayList<double[]> getMarkers() {
		return markers;
	}

	/**
	 * Command line option to set the relative rate for updating each
	 * population/species. Only relevant for multi-species interactions.
	 */
	public final CLOption cloSpeciesUpdateRate = new CLOption("speciesupdaterate", "1", EvoLudo.catModule, null,
			new CLODelegate() {

				/**
				 * {@inheritDoc}
				 * <p>
				 * Parse relative species update rates for multiple populations/species.
				 * {@code arg} can be a single value or an array of values with the
				 * separator {@value CLOParser#SPECIES_DELIMITER}. The parser cycles through
				 * {@code arg} until all populations/species have their update rates set.
				 * 
				 * @param arg encoded population structure
				 */
				@Override
				public boolean parse(String arg) {
					String[] speciesupdaterates = arg.split(CLOParser.SPECIES_DELIMITER);
					int n = 0;
					for (Module pop : species) {
						pop.setSpeciesUpdateRate(
								CLOParser.parseDouble(speciesupdaterates[n++ % speciesupdaterates.length]));
					}
					return true;
				}

				@Override
				public void report(PrintStream output) {
					if (species.size() <= 1)
						return;
					for (Module pop : species) {
						output.println("# speciesupdaterate:    "
								+ Formatter.format(pop.getSpeciesUpdateRate(), 4) + " (" + pop.getName() + ")");
					}
				}

				@Override
				public String getDescription() {
					String descr = "";
					int nSpecies = species.size();
					switch (nSpecies) {
						case 1:
							// not applicable
							return null;
						case 2:
							descr = "--speciesupdaterate <r0:r1> update rates of population i, with\n";
							break;
						case 3:
							descr = "--speciesupdaterate <r0:r1:r2> update rates of population i, with\n";
							break;
						default:
							descr = "--speciesupdaterate <r0:...:r" + nSpecies
									+ "> update rates of population i, with\n";
					}
					for (int i = 0; i < nSpecies; i++)
						descr += "            r" + i + ": " + species.get(i).getName() + "\n";
					descr += "      (loops through update rates)";
					return descr;
				}
			});

	/**
	 * Command line option to set the geometry (interaction and reproduction graphs
	 * identical).
	 * 
	 * @see IBS#cloGeometryInteraction
	 * @see IBS#cloGeometryReproduction
	 */
	public final CLOption cloGeometry = new CLOption("geometry", "M", EvoLudo.catModule, null,
			new CLODelegate() {

				/**
				 * {@inheritDoc}
				 * <p>
				 * Parse encoded population structures for a single or multiple
				 * populations/species. {@code arg} can be a single value or an array of
				 * values with the separator {@value CLOParser#SPECIES_DELIMITER}. The parser
				 * cycles through {@code arg} until all populations/species have their
				 * geometries set.
				 * <p>
				 * <strong>Note:</strong> Whether this refers to the interaction geometry, the
				 * reproduction geometry or both, depends on whether either of them is
				 * explicitly set on the command line. Moreover, this can also refer to the
				 * geometry used for PDE models.
				 * 
				 * @param arg encoded population structure
				 */
				@Override
				public boolean parse(String arg) {
					// for IBS models, if both --geominter and --geomrepro are specified they
					// override --geometry
					String[] geomargs = arg.split(CLOParser.SPECIES_DELIMITER);
					boolean doReset = false;
					int n = 0;
					for (Module pop : species) {
						Geometry geom = pop.createGeometry();
						doReset |= geom.parse(geomargs[n++ % geomargs.length]);
					}
					engine.requiresReset(doReset);
					return true;
				}

				@Override
				public void report(PrintStream output) {
					switch (model.getModelType()) {
						case PDE:
							// inter-species PDEs not yet implemented
							Geometry geo = ((Model.PDE) model).getGeometry();
							output.println("# pde geometry:         " + geo.getType().getTitle());
							geo.printParams(output);
							break;
						case IBS:
							for (Module pop : species) {
								// Geometry intergeo = mod.getInteractionGeometry();
								// if( intergeo.interReproSame ) {
								// output.println("# geometry:
								// "+cloGeometry.getKey(intergeo.geometry).getTitle()
								// + (modules.size() > 1?" ("+mod.getName()+")":""));
								// intergeo.printParams(output);
								// }
								if (pop.structure.interReproSame) {
									output.println("# geometry:             " + pop.structure.getType().getTitle()
											+ (species.size() > 1 ? " (" + pop.getName() + ")" : ""));
									pop.structure.printParams(output);
								}
							}
							break;
						default:
					}
				}

				@Override
				public String getDescription() {
					// // retrieve description
					// Module mod = modules.get(0);
					// return
					// (mod.getGeometry()==null?mod.getInteractionGeometry().usage():mod.getGeometry().usage());
					return structure.usage();
				}
			});

	/**
	 * Command line option to set the population size.
	 */
	public final CLOption cloNPopulation = new CLOption("popsize", "100", EvoLudo.catModule, null,
			new CLODelegate() {

				/**
				 * {@inheritDoc}
				 * <p>
				 * Parse population size(s) for a single or multiple populations/species.
				 * {@code arg} can be a single value or an array of values with the
				 * separator {@value CLOParser#SPECIES_DELIMITER}. The parser cycles through
				 * {@code arg} until all populations/species have their population size
				 * set.
				 * <p>
				 * <strong>Note:</strong> For population size specifications of the form
				 * '&lt;n&gt;x&lt;n&gt;' or '&lt;n&gt;X&lt;n&gt;' a square lattice is assumed
				 * and the population size is set to {@code n<sup>2</sup>}. The number
				 * after the 'x' or 'X' is ignored.
				 * 
				 * @param arg (array of) population size(s)
				 */
				@Override
				public boolean parse(String arg) {
					String[] sizes = arg.split(CLOParser.SPECIES_DELIMITER);
					int n = 0;
					for (Module pop : species) {
						int size = CLOParser.parseDim(sizes[n]);
						if (size < 1)
							continue;
						pop.setNPopulation(size);
						n = (n + 1) % sizes.length;
					}
					return true;
				}

				@Override
				public void report(PrintStream output) {
					boolean isMultispecies = species.size() > 1;
					for (Module pop : species)
						output.println("# populationsize:       " + pop.getNPopulation()
								+ (isMultispecies ? " (" + pop.getName() + ")" : ""));
				}

				@Override
				public String getDescription() {
					String descr = "";
					int nSpecies = species.size();
					switch (nSpecies) {
						case 1:
							return "--popsize <n>|<nxn>  population size n (or nxn, nXn)";
						case 2:
							descr = "--popsize <n0,n1>  size ni of population i, with\n";
							break;
						case 3:
							descr = "--popsize <n0,n1,n2>  size ni of population i, with\n";
							break;
						default:
							descr = "--popsize <n0,...,n" + nSpecies + ">  size ni of population i, with\n";
					}
					for (int i = 0; i < nSpecies; i++)
						descr += "            n" + i + ": " + species.get(i).getName() + "\n";
					descr += "(or nixni, niXni e.g. for lattices)";
					return descr;
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
	public final CLOption cloInitType = new CLOption("inittype", "-default", EvoLudo.catModule,
			"--inittype <t>  type of initial configuration", new CLODelegate() {
				@Override
				public boolean parse(String arg) {
					if (!cloInitType.isSet())
						return true;
					boolean success = true;
					String[] inittypes = arg.split(CLOParser.SPECIES_DELIMITER);
					int n = 0;
					for (Module pop : species) {
						String type = inittypes[n++ % inittypes.length];
						CLOption.Key key = cloInitType.match(type);
						if (key == null) {
							logger.warning(
									(species.size() > 1 ? pop.getName() + ": " : "") +
											"inittype '" + type + "' unknown - using '"
											+ pop.getInitType().getKey() + "'");
							success = false;
							continue;
						}
						pop.setInitType(key);
					}
					return success;
				}

				@Override
				public void report(PrintStream output) {
					for (Module pop : species) {
						output.println(
								"# inittype:             " + pop.getInitType() + (species.size() > 1 ? " ("
										+ pop.getName() + ")" : ""));
					}
				}
			});

	/**
	 * Command line option to set death rate for ecological population updates.
	 */
	public final CLOption cloDeathRate = new CLOption("deathrate", "0.5", EvoLudo.catModule,
			"--deathrate     rate of dying",
			new CLODelegate() {

				/**
				 * {@inheritDoc}
				 * <p>
				 * Parse death rate for ecological population updates for a single or multiple
				 * populations/species. {@code arg} can be a single value or an array of
				 * values with the separator {@value CLOParser#SPECIES_DELIMITER}. The parser
				 * cycles through {@code arg} until all populations/species have the death
				 * rate set.
				 * 
				 * @param arg (array of) death rate(s)
				 */
				@Override
				public boolean parse(String arg) {
					boolean success = true;
					String[] rates = arg.split(CLOParser.SPECIES_DELIMITER);
					int n = 0;
					for (Module pop : species) {
						double rate = CLOParser.parseDouble(rates[n++ % rates.length]);
						if (rate < 0.0) {
							logger.warning((species.size() > 1 ? pop.getName() + ": " : "")
									+ "deathrate must be non-negative - using " + pop.getDeathRate() + "!");
							success = false;
							continue;
						}
						pop.setDeathRate(rate);
					}
					return success;
				}

				@Override
				public void report(PrintStream output) {
					for (Module pop : species) {
						output.println(
								"# deathrate:   " + Formatter.format(pop.getDeathRate(), 4) + (species.size() > 1 ? " ("
										+ pop.getName() + ")" : ""));
					}
				}
			});

	/**
	 * Command line option to set the probability of mutations for
	 * population(s)/species.
	 */
	public final CLOption cloMutation = new CLOption("mutation", "-1", EvoLudo.catModule, null,
			new CLODelegate() {

				/**
				 * {@inheritDoc}
				 * <p>
				 * Parse mutation probability(ies) for a single or multiple populations/species.
				 * {@code arg} can be a single value or an array of values with the
				 * separator {@value CLOParser#SPECIES_DELIMITER}. The parser cycles through
				 * {@code arg} until all populations/species have mutation probabilities
				 * rate set.
				 * <p>
				 * <strong>Note:</strong> Negative rates or invalid numbers (such as '-')
				 * disable mutations.
				 * 
				 * @param arg (array of) mutation probability(ies)
				 */
				@Override
				public boolean parse(String arg) {
					String[] mutations = arg.split(CLOParser.SPECIES_DELIMITER);
					int n = 0;
					try {
						for (Module pop : species) {
							double pMut = Double.parseDouble(mutations[n++ % mutations.length]);
							pop.setMutationProb(pMut);
						}
					} catch (NumberFormatException nfe) {
						for (Module pop : species)
							pop.setMutationProb(-1.0);
						logger.warning("mutation probabilities '" + arg + "' invalid - disabling mutations.");
						return false;
					}
					return true;
				}

				@Override
				public void report(PrintStream output) {
					boolean isMultispecies = species.size() > 1;
					for (Module pop : species) {
						double mut = pop.getMutationProb();
						if (mut > 0.0) {
							output.println("# mutation:             " + Formatter.formatSci(mut, 8)
									+ (isMultispecies ? " (" + pop.getName() + ")" : ""));
							continue;
						}
						if (mut < 0.0) {
							output.println("# mutation:             none"
									+ (isMultispecies ? " (" + pop.getName() + ")" : ""));
							continue;
						}
						output.println("# mutation:             0 (restricted to homogeneous populations)"
								+ (isMultispecies ? " (" + pop.getName() + ")" : ""));
					}
				}

				@Override
				public String getDescription() {
					String descr = "";
					int nSpecies = species.size();
					switch (nSpecies) {
						case 1:
							return "--mutation <m>  mutation probability";
						case 2:
							descr = "--mutation <m0:m1>  mutation probability of population i, with\n";
							break;
						case 3:
							descr = "--mutation <m0:m1:m2>  mutation probability of population i, with\n";
							break;
						default:
							descr = "--mutation <m0:...:m" + nSpecies
									+ ">  mutation probability of population i, with\n";
					}
					for (int i = 0; i < nSpecies; i++)
						descr += "            n" + i + ": " + species.get(i).getName() + "\n";
					descr += "      (loops through mutation rates)";
					return descr;
				}
			});

	/**
	 * Command line option to set the payoff/score to fitness map.
	 */
	public final CLOption cloFitnessMap = new CLOption("fitnessmap", "none", EvoLudo.catModule,
			"--fitnessmap <m> [<b>[,<w>]]  select map with baseline fitness b (1)\n" + //
			"                and selection strength w (1):",
			new CLODelegate() {

				/**
				 * {@inheritDoc}
				 * <p>
				 * Parse payoff/score to fitness map(s) for a single or multiple
				 * populations/species. {@code arg} can be a single value or an array of
				 * values with the separator {@value CLOParser#SPECIES_DELIMITER}. The parser
				 * cycles through {@code arg} until all populations/species have the the
				 * fitness map set.
				 * 
				 * @param arg the (array of) map name(s)
				 */
				@Override
				public boolean parse(String arg) {
					boolean success = true;
					String[] map2fitnessspecies = arg.split(CLOParser.SPECIES_DELIMITER);
					int n = 0;
					for (Module pop : species) {
						String map = map2fitnessspecies[n++ % map2fitnessspecies.length];
						Map2Fitness.Maps m2fm = (Map2Fitness.Maps) cloFitnessMap.match(map);
						Map2Fitness m2f = pop.getMapToFitness();
						if (m2fm == null) {
							logger.warning(
									(species.size() > 1 ? pop.getName() + ": " : "") +
											"fitness map '" + map + "' unknown - using '"
											+ m2f.getName() + "'");
							success = false;
							continue;
						}
						m2f.setMap(m2fm);
						map = CLOption.stripKey(m2fm, map);
						// parse b and w, if present
						String[] args = map.split("[ =,]");
						double b = 1.0;
						double w = 1.0;
						switch (args.length) {
							case 3:
								w = CLOParser.parseDouble(args[2]);
							// $FALL-THROUGH$
							case 2:
								b = CLOParser.parseDouble(args[1]);
								break;
							default:
						}						
						m2f.setBaseline(b);
						m2f.setSelection(w);
					}
					return success;
				}

				@Override
				public void report(PrintStream output) {
					for (Module pop : species) {
						Map2Fitness m2f = pop.getMapToFitness();
						output.println("# fitnessmap:           " + m2f.getTitle()
								+ (species.size() > 1 ? " ("
										+ pop.getName() + ")" : ""));
						output.println("# basefit:              " + Formatter.format(m2f.getBaseline(), 4));
						output.println("# selection:            " + Formatter.format(m2f.getSelection(), 4));
					}
				}
			});

	/**
	 * Command line option to set the type of player updates.
	 */
	public final CLOption cloPlayerUpdate = new CLOption("playerupdate", PlayerUpdateType.IMITATE.getKey(),
			EvoLudo.catModule, 
			"--playerupdate <u> [<n>[,<e>]] set player update type with\n" +//
			"                noise n (neutral=1) and error probability e (0):",
			new CLODelegate() {

				/**
				 * {@inheritDoc}
				 * <p>
				 * Parse player update type(s) for a single or multiple populations/species.
				 * {@code arg} can be a single value or an array of values with the
				 * separator {@value CLOParser#SPECIES_DELIMITER}. The parser cycles through
				 * {@code arg} until all populations/species have the player update type
				 * set.
				 * 
				 * @param arg the (array of) map name(s)
				 */
				@Override
				public boolean parse(String arg) {
					boolean success = true;
					String[] playerupdates = arg.split(CLOParser.SPECIES_DELIMITER);
					int n = 0;
					for (Module pop : species) {
						String updt = playerupdates[n++ % playerupdates.length];
						PlayerUpdateType put = (PlayerUpdateType) cloPlayerUpdate.match(updt);
						if (put == null) {
							if (success)
								logger.warning((species.size() > 1 ? pop.getName() + ": " : "") + //
										"player update '" + updt + "' not recognized - using '"
										+ pop.getPlayerUpdateType()
										+ "'");
							success = false;
							continue;
						}
						pop.setPlayerUpdateType(put);
						updt = CLOption.stripKey(put, updt);
						// parse n, e, if present
						String[] args = updt.split("[ =,]");
						double noise = 1.0;
						double error = 0.0;
						switch (args.length) {
							case 3:
								error = CLOParser.parseDouble(args[2]);
							// $FALL-THROUGH$
							case 2:
								noise = CLOParser.parseDouble(args[1]);
								break;
							default:
						}
						pop.setPlayerUpdateNoise(noise);
						pop.setPlayerUpdateError(error);
					}
					return success;
				}

				@Override
				public void report(PrintStream output) {
					boolean isIBS = model.isModelType(Model.Type.IBS);
					for (Module pop : species) {
						if (isIBS) {
							IBSPopulation ibspop = ((IBS) model).getSpecies(pop);
							// skip populations with Moran updates
							if (ibspop.getPopulationUpdateType().isMoran())
								continue;
						}
						PlayerUpdateType put = pop.getPlayerUpdateType();
						output.println("# playerupdate:         " + put
								+ (species.size() > 1 ? " (" + pop.getName() + ")" : ""));
						switch (put) {
							case THERMAL: // fermi update
							case IMITATE: // imitation update
							case IMITATE_BETTER: // imitation update (better strategies only)
								output.println(
										"# playerupdatenoise:    " + Formatter.formatSci(pop.getPlayerUpdateNoise(), 6));
//XXX errors could probably be added to PROPORTIONAL as well as DE models
								if (isIBS) {
									output.println(
										"# playerupdateerror:    " + Formatter.formatSci(pop.getPlayerUpdateError(), 6));
								}
								break;
							default:
								// no other PlayerUpdateType's seem to implement noise
								break;
						}
					}
				}
			});

	/**
	 * Command line option to set the size of interaction groups.
	 */
	public final CLOption cloNGroup = new CLOption("groupsize", "2", EvoLudo.catModule,
			"--groupsize <n>  size of interaction groups",
			new CLODelegate() {

				/**
				 * {@inheritDoc}
				 * <p>
				 * Parse interaction group size(s) for a single or multiple populations/species.
				 * {@code arg} can be a single value or an array of values with the
				 * separator {@value CLOParser#SPECIES_DELIMITER}. The parser cycles through
				 * {@code arg} until all populations/species have the interaction group
				 * size set.
				 * 
				 * @param arg (array of) interaction group size(s)
				 */
				@Override
				public boolean parse(String arg) {
					boolean success = true;
					String[] sizes = arg.split(CLOParser.SPECIES_DELIMITER);
					int n = 0;
					for (Module pop : species) {
						int size = CLOParser.parseInteger(sizes[n++ % sizes.length]);
						if (size < 1) {
							logger.warning((species.size() > 1 ? pop.getName() + ": " : "")
									+ "group size must be positive - using " + pop.getNGroup() + "!");
							success = false;
							continue;
						}
						pop.setNGroup(size);
					}
					return success;
				}

				@Override
				public void report(PrintStream output) {
					for (Module pop : species)
						output.println("# groupsize:            " + pop.getNGroup()
								+ (species.size() > 1 ? " (" + pop.getName() + ")" : ""));
				}
			});

	/**
	 * Command line option to set the color of traits.
	 */
	public final CLOption cloTraitColors = new CLOption("colors", "default", EvoLudo.catGUI, null,
			new CLODelegate() {

				/**
				 * {@inheritDoc}
				 * <p>
				 * Parse trait colors for a single or multiple populations/species.
				 * {@code arg} can be a single value or an array of values with the
				 * separator {@value CLOParser#SPECIES_DELIMITER}. The parser cycles through
				 * {@code arg} until all populations/species have the trait colors set. The
				 * colors for each species should be an array of values with the separator
				 * {@value CLOParser#MATRIX_DELIMITER}, see
				 * {@link Module#setTraitColors(Color[])} for details.
				 * <p>
				 * <strong>Note:</strong> Colors can be specified in various ways:
				 * <dl>
				 * <dt>Named</dt>
				 * <dd>any named color recognized by java.awt.Color</dd>
				 * <dt>(g)</dt>
				 * <dd>gray scale value with {@code g = 0, ..., 255}.</dd>
				 * <dt>(r,g,b)</dt>
				 * <dd>a color determined by a red, blue, green triple with
				 * {@code r, g, b = 0, ..., 255}.</dd>
				 * <dt>(r,g,b,a)</dt>
				 * <dd>a transparent color determined by a red, blue, green, alpha quadruple
				 * with {@code r, g, b, a = 0, ..., 255}.</dd>
				 * <dl>
				 * If the parsing of a color fails it is replaced by black.
				 * 
				 * @param arg (array of) array of trait color(s)
				 */
				@Override
				public boolean parse(String arg) {
					// default colors are set in load()
					if (!cloTraitColors.isSet())
						return true;
					String[] colorsets = arg.split(CLOParser.SPECIES_DELIMITER);
					if (colorsets == null) {
						logger.warning("color specification '" + arg + "' not recognized - ignored!");
						return false;
					}
					boolean success = true;
					int n = 0;
					for (Module pop : species) {
						String[] colors = colorsets[n++ % colorsets.length].split(CLOParser.MATRIX_DELIMITER);
						Color[] myColors = new Color[colors.length];
						for (int i = 0; i < colors.length; i++) {
							Color newColor = CLOParser.parseColor(colors[i]);
							// if color was not recognized, choose random color
							if (newColor == null) {
								logger.warning(
										"color specification '" + colors[i] + "' not recognized - replaced by black!");
								newColor = Color.BLACK;
								success = false;
							}
							myColors[i] = newColor;
						}
						// setTraitColor deals with missing colors and adding shades
						pop.setTraitColors(myColors);
					}
					return success;
				}

				@Override
				public String getDescription() {
					String descr;
					int nt = -Integer.MAX_VALUE;
					for (Module pop : species)
						nt = Math.max(nt, pop.getNTraits());

					switch (nt) {
						case 1:
							descr = "--colors <c;n>  trait colors (c: regular, n: new)\n" +
									"          c, n: color name or (r,g,b) triplet (in 0-255)";
							break;
						case 2:
							descr = "--colors <c1;c2;n1;n2>  trait colors (c: regular, n: new)";
							break;
						default:
							descr = "--colors <c1;...;c" + nTraits + ";n1;...;n" + nTraits
									+ ">  trait colors (c: regular, n: new)";
					}
					descr += "\n        ci, ni: color name or (r,g,b) triplet (in 0-255), with i:";
					int idx = 0;
					for (Module pop : species) {
						nt = pop.getNTraits();
						for (int n = 0; n < nt; n++) {
							String aTrait = "              " + (idx++) + ": ";
							int traitlen = aTrait.length();
							descr += "\n" + aTrait.substring(traitlen - 16, traitlen) + (species.size() > 1 ? pop.getName() + "." : "") + pop.getTraitName(n);
						}
					}
					if (species.size() > 1)
						descr += "\n      settings for multi-species separated by '" + CLOParser.SPECIES_DELIMITER
								+ "'";
					return descr;
				}
			});

	/**
	 * Command line option to assign trait names.
	 */
	public final CLOption cloTraitNames = new CLOption("traitnames", "default", EvoLudo.catModule, null,
			new CLODelegate() {

				/**
				 * {@inheritDoc}
				 * <p>
				 * Parse trait names for a single or multiple populations/species.
				 * {@code arg} can be a single value or an array of values with the
				 * separator {@value CLOParser#SPECIES_DELIMITER}. The parser cycles through
				 * {@code arg} until all populations/species have the trait colors set. The
				 * trait names for each species should be an array of values with the separator
				 * {@value CLOParser#VECTOR_DELIMITER}.
				 * 
				 * @param arg (array of) array of trait name(s)
				 */
				@Override
				public boolean parse(String arg) {
					// default trait names are set in load()
					if (!cloTraitNames.isSet())
						return true;
					boolean success = true;
					String[] namespecies = arg.split(CLOParser.SPECIES_DELIMITER);
					int n = 0;
					for (Module pop : species) {
						String[] names = namespecies[n++ % namespecies.length].split(CLOParser.VECTOR_DELIMITER);
						if (names.length != nTraits) {
							logger.warning("incorrect number of trait names specified (" + names.length + " instead of "
									+ nTraits + ") - ignored.");
							success = false;
							continue;
						}
						pop.setTraitNames(names);
					}
					return success;
				}

				// note: report(...) not overridden because trait names are reported in
				// {@link Discrete#cloInit} or {@link Continuous#cloInit}, respectively

				@Override
				public String getDescription() {
					String descr;
					int nt = -Integer.MAX_VALUE;
					for (Module pop : species)
						nt = Math.max(nt, pop.getNTraits());

					switch (nt) {
						case 1:
							descr = "--traitnames <n>  override trait name";
							break;
						case 2:
							descr = "--traitnames <n1,n2>  override trait names";
							break;
						default:
							descr = "--traitnames <n1,...,n" + nt + ">  override trait names";
					}
					if (species.size() > 1)
						descr += "\n      settings for multi-species separated by '" + CLOParser.SPECIES_DELIMITER
								+ "'";
					return descr;
				}
			});

	/**
	 * Command line option to set the traits on phase plane axis.
	 * <p>
	 * <strong>Note:</strong> option not automatically added. Modules that supports
	 * multiple traits should load it in {@link #collectCLO(CLOParser)}.
	 */
	public final CLOption cloPhase2DAxis = new CLOption("phase2daxis", "-default", EvoLudo.catModule, null,
			new CLODelegate() {
				@Override
				public boolean parse(String arg) {
					if (!cloPhase2DAxis.isSet())
						return true;
					int[][] phase2daxis = CLOParser.parseIntMatrix(arg);
					if (phase2daxis == null || phase2daxis.length != 2) {
						logger.warning("problem parsing '" + arg + "' - ignored!");
						return false;
					}
					// cast check should be unnecessary. --phase2daxis should only be available if module 
					// implements at least HasPhase2D (plus some other conditions).
					if (Module.this instanceof HasPhase2D)
						((HasPhase2D) Module.this).setPhase2DTraits(phase2daxis[0], phase2daxis[1]);
					return true;
				}

				@Override
				public String getDescription() {
					String descr = "--phase2daxis <t>  indices of traits shown on phase plane axis\n" + //
							"        format: <i0>,<i1>,..." + CLOParser.MATRIX_DELIMITER + //
									"<j0>,<j1>... with \n" + //
							"                <in>, <jn> indices of traits on x-/y-axis";
					if (species.size() > 1) {
						// multi-species module
						descr += "\n      (for second, third etc. species add the total" + //
								 "\n       number of previous traits)";
					}
					return descr;
				}
			});

	/**
	 * Command line option to mark points on graphs (ParaGraph, S3Graph, LineGraph and HistoGraph). Very convenient to indicate fixed points
	 */
	public final CLOption cloPoints = new CLOption("points", "-none", EvoLudo.catGUI, null,
			new CLODelegate() {

				/**
				 * {@inheritDoc}
				 * <p>
				 * Parse point markers for a single or multiple populations/species. These
				 * translate to markers added to different graphs as appropriate. {@code arg}
				 * can be a single value or an array of values with the separator
				 * {@value CLOParser#MATRIX_DELIMITER} for multiple markers. For multiple
				 * species, the separator is {@value CLOParser#SPECIES_DELIMITER}. The the
				 * values of each fixed point is given by an array with the separator
				 * {@value CLOParser#VECTOR_DELIMITER}.
				 * <p>
				 * <strong>Note:</strong>If one or more entries of the marker are negative
				 * an open marker is drawn otherwise the point is filled.
				 * 
				 * @param arg (array of) array of fixed point value(s)
				 */
				@Override
				public boolean parse(String arg) {
					if (!cloPoints.isSet())
						return true;
					boolean success = true;
					int totTraits = 0;
					for (Module pop : species)
						totTraits += pop.getNTraits();
					String[] myMarkers = arg.split(CLOParser.MATRIX_DELIMITER);
					if (markers != null)
						markers.clear();
					for (String aMarker : myMarkers) {
						double[] dmk = new double[totTraits];
						String[] mk = aMarker.split(CLOParser.SPECIES_DELIMITER);
						boolean mksuccess = true;
						boolean filled = true;
						int n = 0;
						int idx = 0;
						for (Module pop : species) {
							double[] smk = CLOParser.parseVector(mk[n++]);
							if (ArrayMath.min(smk)<0.0) {
								filled = false;
								ArrayMath.abs(smk);
							}
							int nt = pop.getNTraits();
							if (smk.length != nt) {
								// ok for frequency based modules or with vacant sites
								int vac = pop.getVacant();
								int dep = pop.getDependent();
								if (!(smk.length == nt - 1 && (vac >= 0 || dep >= 0))) {
									mksuccess = false;
									break;
								}
								switch (model.getModelType()) {
									case IBS:
										smk = ArrayMath.insert(smk, 1.0 - ArrayMath.norm(smk),
												dep >= 0 ? dep : vac);
										break;
									case ODE:
									case SDE:
									case PDE:
										if (((Model.DE) model).isDensity())
											// no dependent traits for density based model (vac >= 0 must hold)
											smk = ArrayMath.insert(smk, 0.0, vac);
										else
											// frequency based models (dep >= 0 must hold)
											smk = ArrayMath.insert(smk, 1.0 - ArrayMath.norm(smk), dep);
										break;
									default:	// unreachable
										break;
								}
							}
							// now smk.length == nt holds
							System.arraycopy(smk, 0, dmk, idx, nt);
							idx += nt;
						}
						if (!mksuccess) {
							logger.warning(
									"failed to set marker '" + aMarker + "' - ignored.");
							success = false;
							continue;
						}
						addMarker(dmk, filled);
					}
					return success;
				}

				@Override
				public String getDescription() {
					String multi = "";
					if (species.size() > 1) 
						multi = "["+CLOParser.SPECIES_DELIMITER+"<j0>,...]";
					String descr = "--points <p>    values of fixed points\n" + //
							"        format: <i0>,<i1>,..."+multi+"["+CLOParser.MATRIX_DELIMITER+"<k0>,<k1>...] with \n" + //
							"                <nm> values of fixed point(s)";
					return descr;
				}
			});

	@Override
	public void collectCLO(CLOParser parser) {
		// prepare command line options
		cloFitnessMap.addKeys(Map2Fitness.Maps.values());
		parser.addCLO(cloFitnessMap);
		parser.addCLO(cloMutation);
		parser.addCLO(cloInitType);

		if (this instanceof Discrete.Groups ||
				this instanceof Continuous.Groups ||
				this instanceof Continuous.MultiGroups)
			parser.addCLO(cloNGroup);

		if (nTraits > 0) {
			parser.addCLO(cloTraitColors);
			parser.addCLO(cloTraitNames);
		}
		// set traits on axis of phase plane (if implemented by module)
		if (this instanceof HasPhase2D && ((HasPhase2D) this).setPhase2DTraits(null, null))
			parser.addCLO(cloPhase2DAxis);

		// multi-species interactions
		if (species.size() > 1) {
			// individual population update rates only meaningful for multiple species
			parser.addCLO(cloSpeciesUpdateRate);
		}

		boolean anyVacant = false;
		boolean anyNonVacant = false;
		boolean anyContinuous = false;
		for (Module pop : species) {
			boolean hasVacant = (pop.getVacant() >= 0);
			anyVacant |= hasVacant;
			anyNonVacant |= !hasVacant;
			anyContinuous |= pop.isContinuous();
		}
		if (anyNonVacant) {
			// additional options that only make sense without vacant sites
			cloPlayerUpdate.addKeys(PlayerUpdateType.values());
			parser.addCLO(cloPlayerUpdate);
		}
		if (anyVacant) {
			parser.addCLO(cloDeathRate);
		}
		// best-response is not an acceptable update rule for continuous strategies -
		// exclude Population.PLAYER_UPDATE_BEST_RESPONSE
		if (anyContinuous) {
			cloPlayerUpdate.removeKey(PlayerUpdateType.BEST_RESPONSE);
		}

		// population size option only acceptable for IBS and SDE models
		if (model.isModelType(Model.Type.IBS) || model.isModelType(Model.Type.SDE)) {
			parser.addCLO(cloNPopulation);
		}

		// geometry option only acceptable for IBS and PDE models
		if (model.isModelType(Model.Type.IBS) || model.isModelType(Model.Type.PDE)) {
			cloGeometry.addKeys(Geometry.Type.values());
			parser.addCLO(cloGeometry);
		}

		// add markers, fixed points in particular
		parser.addCLO(cloPoints);
	}

	/**
	 * Map scores/payoffs to fitness and vice versa. Enum on steroids. Currently
	 * available maps are:
	 * <dl>
	 * <dt>none</dt>
	 * <dd>no mapping, scores/payoffs equal fitness</dd>
	 * <dt>static</dt>
	 * <dd>static baseline fitness, {@code b+w*score}</dd>
	 * <dt>convex</dt>
	 * <dd>convex combination of baseline fitness and scores,
	 * {@code b(1-w)+w*scores}</dd>
	 * <dt>exponential</dt>
	 * <dd>exponential mapping, {@code b*exp(w*score)}</dd>
	 * </dl>
	 * Note that exponential payoff-to-fitness may easily be the most convincing
	 * because it can be easily and uniquely derived from a set of five natural
	 * assumptions on the fitness function \(F(u\):
	 * <ol>
	 * <li>\(F(u)\geq 0\) for every \(u\in\mathbb{R}\)
	 * <li>\(F(u)\) is non-decreasing
	 * <li>\(F(u)\) is continuous
	 * <li>Selection strength \(w\) scales payoffs, i.e. the fitness associated with
	 * payoff \(u\) at selection strength \(w\geq 0\) is \(F(w u)\)
	 * <li>The probability that an individual is chosen for reproduction is
	 * invariant under adding a constant \(K\) to the payoffs of all competing
	 * individuals. That is, if \(u_i\) and \(F_i(u_i)\) are the payoff and
	 * fecundity of individual \(i\), then
	 * \[\frac{F_i(u_i)}{\dsum_j F_j(u_j)} = \frac{F_i(u_i+K)}{\dsum_j F_j(u_j+K)}\]
	 * </ol>
	 * Up to a rescaling of the selection strength, these assumptions lead to a
	 * unique payoff-to-fecundity map, \(F(u)=e^{w u}\). The {@code static} mapping
	 * then immediately follows as an approximation for weak selection.
	 * 
	 * @see <a href="https://doi.org/10.1371/journal.pcbi.1009611">McAvoy, A., Rao,
	 *      A. &amp; Hauert, C. (2021) Intriguing effects of selection intensity on
	 *      the evolution of prosocial behaviors PLoS Comp. Biol. 17 (11)
	 *      e1009611</a>
	 */
	public static class Map2Fitness {

		/**
		 * Enum representing the different types of payoff/score to fitness maps
		 * 
		 * @author Christoph Hauert
		 */
		public enum Maps implements CLOption.Key {

			/**
			 * no mapping, scores/payoffs equal fitness, \(fit = score\)
			 */
			NONE("none", "no mapping"),

			/**
			 * static baseline fitness, \(fit = b+w*score\)
			 */
			STATIC("static", "b+w*score"),

			/**
			 * convex combination of baseline fitness and scores, \(fit = b(1-w)+w*score\)
			 */
			CONVEX("convex", "b*(1-w)+w*score"),

			/**
			 * exponential mapping of scores to fitness, \(fit = b*\exp(w*score)\)
			 */
			EXPONENTIAL("exponential", "b*exp(w*score)");

			/**
			 * Key of map. Used when parsing command line options.
			 * 
			 * @see Module#cloFitnessMap
			 */
			String key;

			/**
			 * Brief description of map for help display.
			 * 
			 * @see EvoLudo#helpCLO()
			 */
			String title;

			/**
			 * Instantiate new type of map.
			 * 
			 * @param key   identifier for parsing of command line option
			 * @param title summary of map
			 */
			Maps(String key, String title) {
				this.key = key;
				this.title = title;
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
		 * Baseline fitness for map.
		 */
		double baseline = 1.0;

		/**
		 * Selection strength for map.
		 */
		double selection = 1.0;

		/**
		 * Map type. Defaults to {@link Maps#NONE}.
		 */
		Maps map = Maps.NONE;

		/**
		 * Instantiate new map of type {@code map}.
		 * 
		 * @param map the map to use as template
		 */
		public Map2Fitness(Maps map) {
			this.map = map;
		}

		/**
		 * Map {@code score} to fitness, based on currently selected type
		 * {@code map}.
		 * 
		 * @param score the payoff/score to convert to fitness
		 * @return the corresponding fitness
		 * 
		 * @see Map2Fitness#invmap
		 */
		public double map(double score) {
			switch (map) {
				case STATIC:
					return baseline + selection * score; // fitness = b + w score
				case CONVEX:
					return baseline + selection * (score - baseline); // fitness = b (1 - w) + w score
				case EXPONENTIAL:
					return baseline * Math.exp(selection * score); // fitness = b exp( w score)
				case NONE:
				default:
					return score;
			}
		}

		/**
		 * Map {@code fitness} to payoff/score, based on currently selected type
		 * {@code map}.
		 * 
		 * @param fitness the fitness to convert to payoff/score
		 * @return the corresponding payoff/score
		 */
		public double invmap(double fitness) {
			switch (map) {
				case STATIC:
					return (fitness - baseline) / selection; // fitness = b + w score
				case CONVEX:
					return (fitness - baseline * (1.0 - selection)) / selection; // fitness = b (1 - w) + w score
				case EXPONENTIAL:
					return Math.log(fitness / baseline) / selection; // fitness = b exp( w score)
				case NONE:
				default:
					return fitness;
			}
		}

		/**
		 * Checks if this map is of type {@code aMap}.
		 * 
		 * @param aMap the map to compare to
		 * @return {@code true} if map is of type {@code aMap}.
		 */
		public boolean isMap(Maps aMap) {
			return map.equals(aMap);
		}

		/**
		 * Sets type of map to {@code map}.
		 * 
		 * @param map the type of the map
		 */
		public void setMap(Maps map) {
			if (map == null)
				return;
			this.map = map;
		}

		/**
		 * Sets the baseline fitness of the map.
		 * 
		 * @param baseline the baseline fitness of the map
		 */
		public void setBaseline(double baseline) {
			this.baseline = baseline;
		}

		/**
		 * Gets the baseline fitness of the map.
		 * 
		 * @return the baseline fitness of the map
		 */
		public double getBaseline() {
			return baseline;
		}

		/**
		 * Sets the selection strength of the map. Must be positive, ignored otherwise.
		 * 
		 * @param selection the strength of selection of the map
		 */
		public void setSelection(double selection) {
			if (selection <= 0.0)
				return;
			this.selection = selection;
		}

		/**
		 * Gets the selection strength of the map.
		 * 
		 * @return the selection strength of the map
		 */
		public double getSelection() {
			return selection;
		}

		/**
		 * Gets the name/key of the current map.
		 * 
		 * @return the map key
		 */
		public String getName() {
			return map.getKey();
		}

		/**
		 * Gets the brief description of the current map.
		 * 
		 * @return the map summary
		 */
		public String getTitle() {
			return map.getTitle();
		}
	}

	/**
	 * Player update types. Enum on steroids. Currently available player update
	 * types are:
	 * <dl>
	 * <dt>best
	 * <dd>best wins (equal - stay)
	 * <dt>best-random
	 * <dd>best wins (equal - random)
	 * <dt>best-response
	 * <dd>best-response dynamics
	 * <dt>imitate
	 * <dd>imitate/replicate (linear)
	 * <dt>imitate-better
	 * <dd>imitate/replicate (better only)
	 * <dt>proportional
	 * <dd>proportional to payoff
	 * <dt>thermal
	 * <dd>Fermi/thermal update
	 * </dl>
	 */
	public static enum PlayerUpdateType implements CLOption.Key {

		/**
		 * best wins (equal - stay)
		 */
		BEST("best", "best wins (equal - stay)"),

		/**
		 * best wins (equal - random)
		 */
		BEST_RANDOM("best-random", "best wins (equal - random)"),

		/**
		 * best-response
		 */
		BEST_RESPONSE("best-response", "best-response"),

		/**
		 * imitate/replicate (linear)
		 */
		IMITATE("imitate", "imitate/replicate (linear)"),

		/**
		 * imitate/replicate (better only)
		 */
		IMITATE_BETTER("imitate-better", "imitate/replicate (better only)"),

		/**
		 * proportional to payoff
		 */
		PROPORTIONAL("proportional", "proportional to payoff"),

		/**
		 * Fermi/thermal update
		 */
		THERMAL("thermal", "Fermi/thermal update");

		/**
		 * Key of player update. Used when parsing command line options.
		 * 
		 * @see Module#cloPlayerUpdate
		 */
		String key;

		/**
		 * Brief description of player update for help display.
		 * 
		 * @see EvoLudo#helpCLO()
		 */
		String title;

		/**
		 * Instantiates a new type of player update type.
		 * 
		 * @param key   the identifier for parsing of command line option
		 * @param title the summary of the player update
		 */
		PlayerUpdateType(String key, String title) {
			this.key = key;
			this.title = title;
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

	public enum DataTypes implements CLOption.Key {

		/**
		 * Report the strategies of all individuals.
		 */
		TRAITS("traits", "traits of all individuals"),

		/**
		 * Report the mean traits in all populations.
		 */
		MEAN("traitmean", "mean traits of population(s)"),

		/**
		 * Report the distribution of traits.
		 */
		HISTOGRAM("traithistogram", "histogram of trait distributions"),

		/**
		 * Report the scores of all individuals (prior to mapping to fitness).
		 */
		SCORES("scores", "scores of all individuals"),

		/**
		 * Report the fitness of all individuals (mapped scores).
		 */
		FITNESS("fitness", "fitness of all individuals"),

		/**
		 * Report the mean fitness in all populations.
		 */
		FITMEAN("fitmean", "mean fitness of population(s)"),

		/**
		 * Report the distribution of traits.
		 */
		FITHISTOGRAM("fithistogram", "histogram of fitness distributions"),

		/**
		 * Report the distribution of traits.
		 */
		STRUCTURE("structdegree", "degree distribution of population structure"),

		/**
		 * Report the statistics of fixation probabilities.
		 */
		STAT_PROB("statprob", "statistics of fixation probabilities"),

		/**
		 * Report the statistics of fixation times.
		 */
		STAT_UPDATES("statupdates", "statistics of updates till fixation"),

		/**
		 * Report the statistics of fixation times.
		 */
		STAT_TIMES("stattimes", "statistics of fixation times");

		/**
		 * Key of data types. Used when parsing command line options.
		 * 
		 * @see org.evoludo.simulator.EvoLudoJRE#cloData
		 */
		String key;

		/**
		 * Brief description of map for help display.
		 * 
		 * @see EvoLudo#helpCLO()
		 */
		String title;

		/**
		 * Instantiate new type of map.
		 * 
		 * @param key   identifier for parsing of command line option
		 * @param title summary of map
		 */
		DataTypes(String key, String title) {
			this.key = key;
			this.title = title;
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
	 * Determine and return data types that module can report derived from the data visualizations that are available for the current module.
	 *
	 * @return the array of data types that can be reported by this module
	 * 
	 * @see org.evoludo.EvoLudoWeb#updateViews
	 */
	public DataTypes[] getAvailableDataTypes() {
		ArrayList<DataTypes> dataOutputs = new ArrayList<>();
		// query available views to deduce the data types to report
		// strategy related data
		boolean isODESDE = model.isModelType(Model.Type.ODE) || model.isModelType(Model.Type.SDE);
		if (this instanceof HasPop2D.Strategy && !isODESDE)
			dataOutputs.add(DataTypes.TRAITS);
		if (this instanceof HasMean.Strategy || this instanceof HasPhase2D || this instanceof HasS3)
			dataOutputs.add(DataTypes.MEAN);
		if (this instanceof HasHistogram.Strategy || this instanceof HasDistribution.Strategy)
			dataOutputs.add(DataTypes.HISTOGRAM);
		// fitness related data
		if (this instanceof HasPop2D.Fitness && !isODESDE) {
			dataOutputs.add(DataTypes.SCORES);
			dataOutputs.add(DataTypes.FITNESS);
		}
		if (this instanceof HasMean.Fitness)
			dataOutputs.add(DataTypes.FITMEAN);
		if (this instanceof HasHistogram.Fitness)
			dataOutputs.add(DataTypes.FITHISTOGRAM);
		// structure related data
		if (this instanceof HasHistogram.Degree && !isODESDE)
			dataOutputs.add(DataTypes.STRUCTURE);
		// statistics related data
		if (this instanceof HasHistogram.StatisticsProbability)
			dataOutputs.add(DataTypes.STAT_PROB);
		if (this instanceof HasHistogram.StatisticsTime) {
			dataOutputs.add(DataTypes.STAT_UPDATES);
			dataOutputs.add(DataTypes.STAT_TIMES);
		}
		return dataOutputs.toArray(new DataTypes[0]);
	}
}
