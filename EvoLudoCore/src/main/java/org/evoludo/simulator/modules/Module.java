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

import org.evoludo.math.RNGDistribution;
import org.evoludo.simulator.ColorMap;
import org.evoludo.simulator.EvoLudo;
import org.evoludo.simulator.Geometry;
import org.evoludo.simulator.models.ChangeListener;
import org.evoludo.simulator.models.ChangeListener.PendingAction;
import org.evoludo.simulator.models.IBS;
import org.evoludo.simulator.models.IBSPopulation;
import org.evoludo.simulator.models.Markers;
import org.evoludo.simulator.models.MilestoneListener;
import org.evoludo.simulator.models.Model;
import org.evoludo.simulator.models.Type;
import org.evoludo.simulator.models.ODEEuler;
import org.evoludo.simulator.models.ODEEuler.HasODE;
import org.evoludo.simulator.models.PDERD;
import org.evoludo.simulator.models.PDERD.HasPDE;
import org.evoludo.simulator.models.SDEEuler.HasSDE;
import org.evoludo.simulator.views.HasPhase2D;
import org.evoludo.simulator.views.HasPhase2D.Data2Phase;
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
public abstract class Module implements Features, MilestoneListener, CLOProvider, Runnable {

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
	 * Reference to current model.
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
	 * Markers for annotating graphical represenations of the state of the model.
	 */
	protected Markers markers;

	/**
	 * Get the list of markers. This serves to mark special values in different
	 * kinds of graphs.
	 * 
	 * @return the list of markers
	 */
	public ArrayList<double[]> getMarkers() {
		return markers.getMarkers();
	}

	/**
	 * Instantiate a new Module with {@code engine} and {@code partner}.
	 * If {@code partner == null} this is a single species module and
	 * interactions within species ({@code opponent == this} holds).
	 * 
	 * @param engine  the pacemaker for running the model
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

	/**
	 * Set the current model. This is available before the model get loaded. Useful
	 * for custom implementations of models.
	 * 
	 * @param model the current model
	 */
	public void setModel(Model model) {
		markers = new Markers(model);
		for (Module pop : species) {
			pop.model = model;
			pop.markers = markers;
		}
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
	 * @see MilestoneListener#moduleLoaded()
	 */
	public void load() {
		map2fitness = new Map2Fitness(this, Map2Fitness.Map.NONE);
		playerUpdate = new PlayerUpdate(this);
		// currently only the Test module uses neither Discrete nor Continuous classes.
		if (species == null)
			species = new ArrayList<Module>();
		engine.addMilestoneListener(this);
		if (this instanceof ChangeListener)
			engine.addChangeListener((ChangeListener) this);
	}

	/**
	 * Unload module and free all resources.
	 * 
	 * @see EvoLudo#unloadModule()
	 * @see MilestoneListener#moduleUnloaded()
	 */
	public void unload() {
		traitName = null;
		traitColor = null;
		trajectoryColor = null;
		map2fitness = null;
		playerUpdate = null;
		opponent = this;
		engine.removeMilestoneListener(this);
		if (this instanceof ChangeListener)
			engine.removeChangeListener((ChangeListener) this);
		if (ibs != null)
			ibs.unload();
		ibs = null;
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
		setActiveTraits(null);
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
	 * @see MilestoneListener#modelDidReinit()
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
	 * @see MilestoneListener#modelDidReset()
	 */
	public void reset() {
		interaction = null;
		competition = null;
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * Default run-loop for modules. Opportunity to override in subclasses for
	 * running customized simulations. Currently only called from
	 * {@link org.evoludo.simulator.EvoLudoJRE#simulation()} if custom
	 * simulation class is specified in {@code jar} file.
	 */
	@Override
	public void run() {
	}

	/**
	 * Return array of Model types that this Module supports.
	 * 
	 * @return the array of supported Model types
	 */
	public Type[] getModelTypes() {
		ArrayList<Type> types = new ArrayList<>();
		if (this instanceof IBS.HasIBS)
			types.add(Type.IBS);
		if (this instanceof HasODE)
			types.add(Type.ODE);
		if (this instanceof HasSDE)
			types.add(Type.SDE);
		if (this instanceof HasPDE)
			types.add(Type.PDE);
		return types.toArray(new Type[0]);
	}

	/**
	 * The field point to the IBSPopulation that represents this module in
	 * individual based simulations. {@code null} for all other model types.
	 */
	IBSPopulation ibs;

	/**
	 * Sets the reference to the IBSPopulation that represents this module in
	 * individual based simulations.
	 * 
	 * @param ibs the individual based population
	 */
	public void setIBSPopulation(IBSPopulation ibs) {
		this.ibs = ibs;
	}

	/**
	 * Gets the IBSPopulation that represents this module in individual based
	 * simulations or {@code null} for all other types of models.
	 * 
	 * @return the IBSPopulation that represents this module or {@code null}
	 */
	public IBSPopulation getIBSPopulation() {
		return ibs;
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
		if (model.isDE() && ((ODEEuler) model).isDensity())
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
	 * Gets the numberof roles that an individual can adopt. For example the role of
	 * a proposer or a responder in the Ultimatum game or the first or second movers
	 * in the Centipede game.
	 * 
	 * @return the number of roles of an individual
	 */
	public int getNRoles() {
		return 1;
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
			this.active = new boolean[nTraits];
			Arrays.fill(this.active, true);
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
	 * Gets the colors for the mean values of traits. By default this is the same as the trait
	 * colors. Opportunity for subclasses to return different sets of colors for
	 * plotting mean values.
	 * 
	 * @return the array of mean value colors
	 */
	public Color[] getMeanColors() {
		return getTraitColors();
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
	 * {@link org.evoludo.simulator.views.Mean}.
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
	 * as {@link org.evoludo.simulator.views.Pop2D} or
	 * {@link org.evoludo.simulator.views.Pop3D}. By default no changes are made.
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
	 * @see Model#getMinScore(int)
	 */
	public abstract double getMinGameScore();

	/**
	 * Calculates and returns the maximum payoff/score of an individual. This value
	 * is important for converting payoffs/scores into probabilities, for scaling
	 * graphical output and some optimizations.
	 * 
	 * @return the maximum payoff/score
	 * 
	 * @see Model#getMaxScore(int)
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
	 * Checks whether dynamic is neutral, i.e. no selection acting on the different
	 * traits.
	 * 
	 * @return {@code true} if all payoffs identical
	 */
	public boolean isNeutral() {
		return (Math.abs(getMaxGameScore() - getMinGameScore()) < 1e-8);
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
	 * The geometry of population (interaction and competition graphs are the same)
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
	 * Opportunity to supply {@link Geometry}, in case interaction and competition
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
	 * The geometry of competition structure
	 */
	protected Geometry competition;

	/**
	 * Sets different geometries for interactions and competition.
	 * 
	 * @param interaction the geometry for interactions
	 * @param competition the geometry for competition
	 * 
	 * @see Geometry
	 */
	public void setGeometries(Geometry interaction, Geometry competition) {
		this.interaction = interaction;
		this.competition = competition;
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
	 * Gets the competition geometry.
	 * 
	 * @return the competition geometry
	 * 
	 * @see Geometry
	 */
	public Geometry getCompetitionGeometry() {
		return competition;
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
	 * @see EvoLudo#requiresReset(boolean)
	 */
	public boolean setNPopulation(int size) {
		int oldNPopulation = nPopulation;
		nPopulation = Math.max(1, size);
		boolean changed = (nPopulation != oldNPopulation);
		engine.requiresReset(changed && model.isIBS());
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
	 * Gets the mutation type.
	 * 
	 * @return the mutation type
	 */
	public abstract Mutation getMutation();

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
	 * Map to convert score/payoff to fitness
	 */
	protected PlayerUpdate playerUpdate;

	/**
	 * Gets the score/payoff to fitness map.
	 * 
	 * @return the score-to-fitness map
	 */
	public PlayerUpdate getPlayerUpdate() {
		return playerUpdate;
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
	 * Command line option to set the geometry (interaction and competition graphs
	 * identical).
	 * 
	 * @see IBS#cloGeometryInteraction
	 * @see IBS#cloGeometryCompetition
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
				 * competition geometry or both, depends on whether either of them is
				 * explicitly set on the command line. Moreover, this can also refer to the
				 * geometry used for PDE models.
				 * 
				 * @param arg encoded population structure
				 */
				@Override
				public boolean parse(String arg) {
					// for IBS models, if both --geominter and --geomcomp are specified they
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
							Geometry geo = ((PDERD) model).getGeometry();
							output.println("# pde geometry:         " + geo.getType().getTitle());
							geo.printParams(output);
							break;
						case IBS:
							for (Module pop : species) {
								// Geometry intergeo = mod.getInteractionGeometry();
								// if( intergeo.interCompSame ) {
								// output.println("# geometry:
								// "+cloGeometry.getKey(intergeo.geometry).getTitle()
								// + (modules.size() > 1?" ("+mod.getName()+")":""));
								// intergeo.printParams(output);
								// }
								if (pop.structure.interCompSame) {
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
					descr += "                (or nixni, niXni e.g. for lattices)";
					return descr;
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
	 * Command line option to disable individual traits. Any module offering this
	 * capability needs to add cloTrai to the set of available options. By default
	 * all traits are activated.
	 */
	public final CLOption cloTraitDisable = new CLOption("disable", "none", EvoLudo.catModule,
			"--disable <d[" + CLOParser.VECTOR_DELIMITER + "d...[" + CLOParser.SPECIES_DELIMITER + "d["
					+ CLOParser.VECTOR_DELIMITER + "d...]]]  indices of disabled traits.",
			new CLODelegate() {

				/**
				 * {@inheritDoc}
				 * <p>
				 * Parse disabled traits for a single or multiple populations/species.
				 * {@code arg} can be a single value or an array of values with the
				 * separator {@value CLOParser#SPECIES_DELIMITER}. The parser cycles through
				 * {@code arg} until all populations/species have the trait(s) disabled. The
				 * indices of the traits are given by an array of values with the separator
				 * {@value CLOParser#VECTOR_DELIMITER}.
				 * 
				 * @param arg (array of) array of trait name(s)
				 */
				@Override
				public boolean parse(String arg) {
					// activate all traits
					for (Module pop : species)
						pop.setActiveTraits(null);
					if (!cloTraitDisable.isSet())
						return true;
					boolean success = true;
					String[] disabledtraits = arg.split(CLOParser.SPECIES_DELIMITER);
					int n = 0;
					for (Module pop : species) {
						int[] dtraits = CLOParser.parseIntVector(disabledtraits[n++ % disabledtraits.length]);
						int dist = dtraits.length;
						int mint = model.isContinuous() ? 1 : 2;
						if (pop.nTraits - mint < dist) {
							logger.warning("at least '" + mint + "' enabled traits required - enabling all.");
							success = false;
							continue;
						}
						for (int t : dtraits) {
							if (t < 0 || t >= pop.nTraits) {
								logger.warning("invalid trait index '" + t + "' - ignored.");
								success = false;
								continue;
							}
							pop.active[t] = false;
							pop.nActive--;
						}
					}
					return success;
				}

				@Override
				public void report(PrintStream output) {
					String msg = "";
					for (Module pop : species) {
						int count = 0;
						for (int n = 0; n < pop.nTraits; n++) {
							if (!pop.active[n])
								continue;
							msg += pop.traitName[n];
							count++;
							if (count < pop.nActive)
								msg += ", ";
						}
					}
					output.println("# disabled traits:      " + (msg.length() > 0 ? msg : "none"));
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
					descr += "\n        ci, ni: color name or (r,g,b) triplet (in 0-255) with i:";
					int idx = 0;
					for (Module pop : species) {
						nt = pop.getNTraits();
						for (int n = 0; n < nt; n++) {
							String aTrait = "              " + (idx++) + ": ";
							int traitlen = aTrait.length();
							descr += "\n" + aTrait.substring(traitlen - 16, traitlen)
									+ (species.size() > 1 ? pop.getName() + "." : "") + pop.getTraitName(n);
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
					// cast check should be unnecessary. --phase2daxis should only be available if
					// module implements at least HasPhase2D (plus some other conditions).
					if (Module.this instanceof HasPhase2D) {
						Data2Phase map = ((HasPhase2D) Module.this).getPhase2DMap();
						if (map != null && map.hasSetTraits())
							map.setTraits(phase2daxis[0], phase2daxis[1]);
					}
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

	@Override
	public void collectCLO(CLOParser parser) {
		// prepare command line options
		map2fitness.clo.addKeys(Map2Fitness.Map.values());
		parser.addCLO(map2fitness.clo);

		if (this instanceof Features.Groups)
			parser.addCLO(cloNGroup);

		if (nTraits > 0) {
			parser.addCLO(cloTraitColors);
			parser.addCLO(cloTraitNames);
		}
		// set traits on axis of phase plane (if implemented by module)
		if (this instanceof HasPhase2D) {
			Data2Phase map = ((HasPhase2D) Module.this).getPhase2DMap();
			if (map != null && map.hasSetTraits())
				parser.addCLO(cloPhase2DAxis);
		}

		// multi-species interactions
		if (species.size() > 1) {
			// individual population update rates only meaningful for multiple species
			parser.addCLO(cloSpeciesUpdateRate);
		}

		boolean anyVacant = false;
		boolean anyNonVacant = false;
		int minTraits = Integer.MAX_VALUE;
		int maxTraits = -Integer.MAX_VALUE;
		for (Module pop : species) {
			boolean hasVacant = (pop.getVacant() >= 0);
			anyVacant |= hasVacant;
			anyNonVacant |= !hasVacant;
			int nt = pop.getNTraits();
			minTraits = Math.min(minTraits, nt);
			maxTraits = Math.min(maxTraits, nt);
		}
		if (anyNonVacant) {
			// additional options that only make sense without vacant sites
			playerUpdate.clo.addKeys(PlayerUpdate.Type.values());
			parser.addCLO(playerUpdate.clo);
		}
		if (anyVacant) {
			parser.addCLO(cloDeathRate);
		}
		// add option to disable traits if >=3 traits, except >=2 traits for
		// continuous modules with no vacancies (cannot disable vacancies)
		if (minTraits > 2 || (anyNonVacant && minTraits > 1))
			parser.addCLO(cloTraitDisable);

		// population size option only acceptable for IBS and SDE models
		if (model.isIBS() || model.isSDE()) {
			parser.addCLO(cloNPopulation);
		}

		// geometry option only acceptable for IBS and PDE models
		if (model.isIBS() || model.isPDE()) {
			cloGeometry.addKeys(Geometry.Type.values());
			// by default remove DYNAMIC and SQUARE_NEUMANN_2ND geometries
			cloGeometry.removeKey(Geometry.Type.DYNAMIC);
			cloGeometry.removeKey(Geometry.Type.SQUARE_NEUMANN_2ND);
			parser.addCLO(cloGeometry);
		}

		// add markers, fixed points in particular
		parser.addCLO(markers.clo);
	}
}
