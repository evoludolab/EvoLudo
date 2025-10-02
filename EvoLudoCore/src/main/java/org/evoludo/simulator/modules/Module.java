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

package org.evoludo.simulator.modules;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.evoludo.math.ArrayMath;
import org.evoludo.math.RNGDistribution;
import org.evoludo.simulator.ColorMap;
import org.evoludo.simulator.EvoLudo;
import org.evoludo.simulator.Geometry;
import org.evoludo.simulator.models.Advection;
import org.evoludo.simulator.models.ChangeListener;
import org.evoludo.simulator.models.IBS;
import org.evoludo.simulator.models.IBSPopulation;
import org.evoludo.simulator.models.Markers;
import org.evoludo.simulator.models.MilestoneListener;
import org.evoludo.simulator.models.Model;
import org.evoludo.simulator.models.Model.HasDE;
import org.evoludo.simulator.models.Model.HasIBS;
import org.evoludo.simulator.models.ODE;
import org.evoludo.simulator.models.PDE;
import org.evoludo.simulator.models.RungeKutta;
import org.evoludo.simulator.models.SDE;
import org.evoludo.simulator.models.Type;
import org.evoludo.simulator.views.HasPhase2D;
import org.evoludo.simulator.views.HasPhase2D.Data2Phase;
import org.evoludo.util.CLOParser;
import org.evoludo.util.CLOProvider;
import org.evoludo.util.CLOption;
import org.evoludo.util.CLOption.CLODelegate;
import org.evoludo.util.CLOption.Category;

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
	public final void setName(String name) {
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
	public List<double[]> getMarkers() {
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
	 * Creates a model of type {@code type} for <code>module</code>.
	 * <p>
	 * <strong>Note:</strong> Override to provide custom model implementations.
	 * <p>
	 * <strong>Important:</strong> any custom model implementations that involve
	 * random numbers, must use the shared random number generator for
	 * reproducibility
	 * 
	 * @param type the type of {@link Model} to create
	 * @return the model for <code>module</code> or {@code null} if the module
	 *         does not support the requested model type
	 * 
	 * @see EvoLudo#getRNG()
	 */
	public Model createModel(Type type) {
		if (model != null && model.getType() == type)
			return model;
		// default for ODE is RK5, if available
		if (type == Type.ODE && this instanceof HasDE.RK5)
			type = Type.RK5;
		// default for PDE is PDERD, if available
		else if (type == Type.PDE && this instanceof HasDE.PDERD)
			type = Type.PDERD;
		// return default model for type
		switch (type) {
			case IBS:
				// let subclasses handle IBS
				return null;
			case SDE:
				if (!(this instanceof HasDE.SDE))
					return null;
				return new SDE(engine);
			case RK5:
				if (!(this instanceof HasDE.ODE))
					return null;
				return new RungeKutta(engine);
			case EM:
				if (!(this instanceof HasDE.ODE))
					return null;
				return new ODE(engine);
			case PDEADV:
				if (!(this instanceof HasDE.PDEADV))
					return null;
				return new Advection(engine);
			case PDERD:
				if (!(this instanceof HasDE.PDERD))
					return null;
				return new PDE(engine);
			default:
				return null;
		}
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
	public List<? extends Module> getSpecies() {
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
	 * Returns identifier of the active module. For example, 2x2 games in
	 * {@link org.evoludo.simulator.modules.TBT} return "2x2". This corresponds to
	 * the argument for the {@code --module} option to load a particular module. The
	 * default is to use the class name.
	 * 
	 * @return the identifying key of this module
	 */
	public String getKey() {
		return getClass().getSimpleName();
	}

	/**
	 * Returns a string with information about the authors of the module.
	 * 
	 * @return the names of the authors
	 */
	public String getAuthors() {
		return null;
	}

	/**
	 * Load new module and perform basic initializations.
	 * 
	 * @see EvoLudo#loadModule(String)
	 * @see MilestoneListener#moduleLoaded()
	 */
	public void load() {
		if (this instanceof Payoffs) {
			map2fitness = new Map2Fitness(this, Map2Fitness.Map.NONE);
			playerUpdate = new PlayerUpdate(this);
		}
		// currently only the Test module uses neither Discrete nor Continuous classes.
		if (species == null)
			species = new ArrayList<>();
		engine.addCLOProvider(this);
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
		markers = null;
		opponent = this;
		engine.removeCLOProvider(this);
		engine.removeMilestoneListener(this);
		if (this instanceof ChangeListener)
			engine.removeChangeListener((ChangeListener) this);
		ibspop = null;
		interaction = null;
		competition = null;
		structure = null;
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
	 * untouched and only initializes the traits.
	 * 
	 * @see EvoLudo#modelInit()
	 * @see MilestoneListener#modelDidInit()
	 */
	public void init() {
	}

	/**
	 * Reset Module based on current parameters. This requires that parameters are
	 * consistent, i.e. {@code check()} has been called before.
	 * <p>
	 * <strong>Note:</strong> The difference between {@link #init()} and
	 * {@link #reset()} is that {@link #init()} leaves population structures
	 * untouched and only initializes the traits.
	 * 
	 * @see EvoLudo#modelReset()
	 * @see MilestoneListener#modelDidReset()
	 */
	public void reset() {
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
		if (this instanceof HasIBS)
			types.add(Type.IBS);
		if (this instanceof HasDE.ODE)
			types.add(Type.ODE);
		if (this instanceof HasDE.RK5)
			types.add(Type.RK5);
		if (this instanceof HasDE.EM)
			types.add(Type.EM);
		if (this instanceof HasDE.SDE)
			types.add(Type.SDE);
		if (this instanceof HasDE.PDE)
			types.add(Type.PDE);
		if (this instanceof HasDE.PDERD)
			types.add(Type.PDERD);
		if (this instanceof HasDE.PDEADV)
			types.add(Type.PDEADV);
		return types.toArray(new Type[0]);
	}

	/**
	 * The field point to the IBSPopulation that represents this module in
	 * individual based simulations. {@code null} for all other model types.
	 */
	IBSPopulation ibspop;

	/**
	 * Sets the reference to the IBSPopulation that represents this module in
	 * individual based simulations.
	 * 
	 * @param ibs the individual based population
	 */
	public void setIBSPopulation(IBSPopulation ibs) {
		this.ibspop = ibs;
	}

	/**
	 * Gets the IBSPopulation that represents this module in individual based
	 * simulations or {@code null} for all other types of models.
	 * 
	 * @return the IBSPopulation that represents this module or {@code null}
	 */
	public IBSPopulation getIBSPopulation() {
		return ibspop;
	}

	/**
	 * Opportunity to supply custom individual based simulations.
	 * 
	 * @return the custom IBSPopulation or {@code null} to use default.
	 */
	public IBSPopulation createIBSPopulation() {
		return null;
	}

	/**
	 * The default name of the vacant type (empty site).
	 */
	public static final String VACANT_NAME = "Vacant";

	/**
	 * The default name of the vacant type (empty site).
	 */
	public static final Color VACANT_COLOR = Color.GRAY;

	/**
	 * The index for the vacant type (empty site) or {@code -1} if Module does not
	 * admit empty sites.
	 */
	private int vacantIdx = -1;

	/**
	 * Get the index for the vacant type or {@code -1} if Module does not admit
	 * empty sites. In {@link Discrete} modules this is the index of the vacant type
	 * e.g. in the name or color vectors. Currently unused in {@link Continuous}
	 * modules.
	 * 
	 * @return the index of the vacant type
	 */
	public int getVacantIdx() {
		return vacantIdx;
	}

	/**
	 * Check if module admits vacant type (empty sites).
	 * 
	 * @return {@code true} if module admits vacant sites
	 */
	public boolean hasVacant() {
		return vacantIdx >= 0;
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
	 * Gets the number of roles that an individual can adopt. For example the role
	 * of a proposer or a responder in the Ultimatum game or the first or second
	 * movers in the Centipede game.
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
	public final void setNTraits(int nTraits) {
		setNTraits(nTraits, -1);
	}

	/**
	 * Sets the number of traits and the index of the vacant type (empty site).
	 * 
	 * @param nTraits   the number of traits
	 * @param vacantIdx the index of the vacant type
	 */
	public final void setNTraits(int nTraits, int vacantIdx) {
		// prevent requesting re-parsing of CLOs on initial load
		if (this.nTraits > 0 && (this.nTraits != nTraits))
			engine.requestParseCLO();
		this.nTraits = nTraits;
		this.vacantIdx = vacantIdx;
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
	String[] traitName;

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
		if (traitName == null || !(traitName.length == nTraits || (traitName.length == nTraits - 1 && hasVacant())))
			traitName = new String[nTraits];
		int idx = 0;
		if (names != null) {
			for (String n : names) {
				if (n == null) {
					traitName[idx++] = nameTrait(idx);
					continue;
				}
				traitName[idx++] = n.replace('_', ' ').trim();
			}
		}
		for (int i = idx; i < nTraits; i++)
			traitName[i] = nameTrait(idx++);
	}

	/**
	 * Get default name for trait with index {@code trait}.
	 * 
	 * @param trait the index of the trait
	 * @return the default name for the trait
	 */
	private String nameTrait(int trait) {
		if (trait == vacantIdx)
			return VACANT_NAME;
		return "Trait " + (char) ('A' + trait);
	}

	/**
	 * The array with default colors for traits.
	 * 
	 * <strong>Important:</strong> if {@code defaultColor} is set already here
	 * (static allocation), headless mode for simulations is prevented. In order to
	 * avoid this, simply allocate and assign the colors in the constructor.
	 */
	protected static Color[] defaultColor = new Color[] {
			Color.BLUE,
			Color.RED,
			Color.GREEN,
			Color.YELLOW,
			Color.MAGENTA,
			Color.ORANGE,
			Color.PINK,
			Color.CYAN
	};

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
	 * Gets the colors for the mean values of traits. By default this is the same as
	 * the trait colors. Opportunity for subclasses to return different sets of
	 * colors for plotting mean values.
	 * 
	 * @return the array of mean value colors
	 */
	public Color[] getMeanColors() {
		return getTraitColors();
	}

	/**
	 * Sets trait colors specified in {@code colors}. If {@code colors} is
	 * {@code null} default colors are used until exhausted and then complemented by
	 * random colors. Otherwise the number of colors must equal {@code nTraits} or
	 * {@code 2 * nTraits}. For modules that have vacant sites, the length may be
	 * {@code nTraits - 1} or {@code 2 * (nTraits - 1)}, respectively. In the latter
	 * case the default colors for vacant sites are used.
	 * <p>
	 * <strong>Note:</strong>The meaning of the second set of {@code nTraits} colors
	 * depends on the module type:
	 * <dl>
	 * <dt>Discrete
	 * <dd>the colors of individuals that switched traits since the last update
	 * <dt>Continuous
	 * <dd>the colors for the mean &#177; standard deviation, see e.g.
	 * {@link org.evoludo.simulator.views.Mean}.
	 * </dl>
	 * Specifying the second set of colors is optional. By default they are
	 * automatically generated as lighter versions of the base colors.
	 * 
	 * @param colors the array of colors for the different traits
	 * @return {@code true} if colors successfuly set
	 * 
	 * @see #defaultColor
	 * @see #VACANT_COLOR
	 */
	public boolean setTraitColors(Color[] colors) {
		if (colors == null) {
			colors = new Color[nTraits];
			System.arraycopy(defaultColor, 0, colors, 0, Math.min(nTraits, defaultColor.length));
			if (nTraits > defaultColor.length) {
				// add random colors if needed - do not use the shared RNG to prevent
				// interfering with reproducibility
				RNGDistribution rng = new RNGDistribution.Uniform();
				for (int n = defaultColor.length; n < nTraits; n++)
					colors[n] = new Color(rng.random0n(256), rng.random0n(256), rng.random0n(256));
			}
		}
		if (!(colors.length == nTraits || colors.length == 2 * nTraits
				|| (hasVacant() && (colors.length == nTraits - 1
						|| colors.length == 2 * (nTraits - 1)))))
			return false;
		if (hasVacant()) {
			// insert vacant color
			if (colors.length == 2 * (nTraits - 1))
				colors = ArrayMath.insert(colors, ColorMap.blendColors(VACANT_COLOR, Color.WHITE, 0.333),
						nTraits + vacantIdx);
			colors = ArrayMath.insert(colors, VACANT_COLOR, vacantIdx);
		}
		// now the color array is guaranteed to be of length nTraits or 2 * nTraits
		if (colors.length == nTraits) {
			Color[] cols = new Color[nTraits * 2];
			System.arraycopy(colors, 0, cols, 0, nTraits);
			// generate lighter versions for switched colors
			for (int n = nTraits; n < 2 * nTraits; n++)
				// NOTE: Color.brighter() does not work on pure colors.
				cols[n] = ColorMap.blendColors(colors[n % nTraits], Color.WHITE, 0.333);
			colors = cols;
		}
		traitColor = colors;
		// now the color array is guaranteed to be of length 2 * nTraits
		if (this instanceof Discrete)
			return true;
		// for Continuous traits the second and third set of nTraits colors are for
		// mean -/+ stddev. simply duplicate the second set.
		traitColor = ArrayMath.merge(colors, ArrayMath.drop(colors, 0, nTraits));
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
		engine.requiresReset(changed && model instanceof IBS);
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
	 * Default implementation of Payoffs interface.
	 * 
	 * @return the score-to-fitness map
	 * 
	 * @see Payoffs#getMap2Fitness()
	 */
	public Map2Fitness getMap2Fitness() {
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
	 * Command line option to set the geometry (interaction and competition graphs
	 * identical).
	 * 
	 * @see IBS#cloGeometryInteraction
	 * @see IBS#cloGeometryCompetition
	 */
	public final CLOption cloGeometry = new CLOption("geometry", "M", Category.Model, null,
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
				 * @param arg the encoded population structure
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
				public String getDescription() {
					return structure.usage();
				}
			});

	/**
	 * Command line option to set the population size.
	 */
	public final CLOption cloNPopulation = new CLOption("popsize", "100", Category.Model, null,
			new CLODelegate() {

				/**
				 * {@inheritDoc}
				 * <p>
				 * Parse population size(s) for a single or multiple populations/species.
				 * {@code arg} can be a single value or an array of values. The parser cycles
				 * through {@code arg} until all populations/species have their population size
				 * set.
				 * <p>
				 * <strong>Note:</strong> For population size specifications of the form
				 * '&lt;n&gt;x&lt;n&gt;' or '&lt;n&gt;X&lt;n&gt;' a square lattice is assumed
				 * and the population size is set to {@code n<sup>2</sup>}. The number
				 * after the 'x' or 'X' is ignored.
				 * 
				 * @param arg the (array of) population size(s)
				 */
				@Override
				public boolean parse(String arg) {
					String[] sizes = arg.contains(CLOParser.SPECIES_DELIMITER) ? arg.split(CLOParser.SPECIES_DELIMITER)
							: arg.split(CLOParser.VECTOR_DELIMITER);
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
					StringBuilder sb = new StringBuilder(descr);
					for (int i = 0; i < nSpecies; i++)
						sb.append("            n").append(i).append(": ").append(species.get(i).getName()).append("\n");
					sb.append("                (or nixni, niXni e.g. for lattices)");
					return sb.toString();
				}
			});

	/**
	 * Command line option to set death rate for ecological population updates.
	 */
	public final CLOption cloDeathRate = new CLOption("deathrate", "0.0", Category.Module, null,
			new CLODelegate() {

				/**
				 * {@inheritDoc}
				 * <p>
				 * Parse death rate for ecological population updates for a single or multiple
				 * populations/species. {@code arg} can be a single value or an array of
				 * values. The parser cycles through {@code arg} until all populations/species
				 * have the death rate set.
				 * 
				 * @param arg the (array of) death rate(s)
				 */
				@Override
				public boolean parse(String arg) {
					double[] rates;
					// allow vector delimiter as well as species delimiter for multiple species
					if (arg.contains(CLOParser.SPECIES_DELIMITER))
						rates = CLOParser.parseVector(arg, CLOParser.SPECIES_DELIMITER);
					else
						rates = CLOParser.parseVector(arg);
					if (rates.length == 0)
						return false;
					int n = 0;
					for (Module pop : species) {
						double rate = rates[n++ % rates.length];
						// sanity checks
						if (rate >= 0.0) {
							pop.setDeathRate(rate);
							continue;
						}
						if (logger.isLoggable(Level.WARNING)) {
							String sn = pop.getName();
							logger.warning("death rate" + (sn.isEmpty() ? "" : " of " + sn)
									+ " must be non-negative (changed to 0).");
						}
						pop.setDeathRate(0.0);
					}
					return true;
				}

				@Override
				public String getDescription() {
					String descr = "";
					int nSpecies = species.size();
					switch (nSpecies) {
						case 1:
							return "--deathrate <d>  rate of dying";
						case 2:
							descr = "--deathrate <d0[,d1]>  rates of dying";
							break;
						case 3:
							descr = "--deathrate <d0[,d1,d2]>  rates of dying";
							break;
						default:
							descr = "--deathrate <d0[,...,d" + nSpecies + "]>  rates of dying";
					}
					return descr;
				}
			});

	/**
	 * Command line option to set the size of interaction groups.
	 */
	public final CLOption cloNGroup = new CLOption("groupsize", "2", Category.Module,
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
				 * @param arg the (array of) interaction group size(s)
				 */
				@Override
				public boolean parse(String arg) {
					int[] sizes;
					if (arg.contains(CLOParser.SPECIES_DELIMITER))
						sizes = CLOParser.parseIntVector(arg, CLOParser.SPECIES_DELIMITER);
					else
						sizes = CLOParser.parseIntVector(arg);
					if (sizes.length == 0)
						return false;
					int n = 0;
					for (Module pop : species)
						pop.setNGroup(sizes[n++ % sizes.length]);
					return true;
				}
			});

	/**
	 * Command line option to disable individual traits. Any module offering this
	 * capability needs to add cloTrai to the set of available options. By default
	 * all traits are activated.
	 */
	public final CLOption cloTraitDisable = new CLOption("disable", "none", Category.Module,
			"--disable <d1[" + CLOParser.VECTOR_DELIMITER + "d2...]>  indices of disabled traits.",
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
				 * @param arg the (array of) array of trait name(s)
				 */
				@Override
				public boolean parse(String arg) {
					// activate all traits
					for (Module pop : species)
						pop.setActiveTraits(null);
					if (!cloTraitDisable.isSet())
						return true;
					String[] disabledtraits = arg.split(CLOParser.SPECIES_DELIMITER);
					int n = 0;
					for (Module pop : species) {
						int[] dtraits = CLOParser.parseIntVector(disabledtraits[n++ % disabledtraits.length]);
						int dist = dtraits.length;
						int mint = model.isContinuous() ? 1 : 2;
						if (pop.nTraits - mint < dist)
							return false;
						for (int t : dtraits) {
							if (t < 0 || t >= pop.nTraits)
								return false;
							pop.active[t] = false;
							pop.nActive--;
						}
					}
					return true;
				}
			});

	/**
	 * Command line option to set the color of traits.
	 */
	public final CLOption cloTraitColors = new CLOption("colors", "default", Category.GUI, null,
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
				 * @param arg the (nested) array of trait color(s)
				 */
				@Override
				public boolean parse(String arg) {
					// default colors are set in load()
					if (!cloTraitColors.isSet())
						return true;
					String[] colorsets = arg.split(CLOParser.SPECIES_DELIMITER);
					if (colorsets == null)
						return false;
					int n = 0;
					for (Module mod : species) {
						String[] colors = colorsets[n++ % colorsets.length].split(CLOParser.MATRIX_DELIMITER);
						Color[] myColors = new Color[colors.length];
						for (int i = 0; i < colors.length; i++) {
							Color newColor = CLOParser.parseColor(colors[i]);
							// if color was not recognized, choose random color
							if (newColor == null)
								return false;
							myColors[i] = newColor;
						}
						// setTraitColor deals with missing colors and adding shades
						if (!mod.setTraitColors(myColors))
							return false;
					}
					return true;
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
					StringBuilder sb = new StringBuilder(descr);
					for (Module mod : species) {
						nt = mod.getNTraits();
						for (int n = 0; n < nt; n++) {
							String aTrait = "              " + (idx++) + ": ";
							int traitlen = aTrait.length();
							sb.append("\n")
									.append(aTrait.substring(traitlen - 16, traitlen))
									.append(species.size() > 1 ? mod.getName() + "." : "")
									.append(mod.getTraitName(n));
						}
					}
					if (species.size() > 1)
						sb.append("\n      settings for multi-species separated by '" + CLOParser.SPECIES_DELIMITER
								+ "'");
					return sb.toString();
				}
			});

	/**
	 * Command line option to assign trait names.
	 */
	public final CLOption cloTraitNames = new CLOption("traitnames", "default", Category.Module, null,
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
				 * @param arg the (array of) array of trait name(s)
				 */
				@Override
				public boolean parse(String arg) {
					// default trait names are set in load()
					if (!cloTraitNames.isSet())
						return true;
					String[] namespecies = arg.split(CLOParser.SPECIES_DELIMITER);
					int n = 0;
					for (Module pop : species) {
						String[] names = namespecies[n++ % namespecies.length].split(CLOParser.VECTOR_DELIMITER);
						if (names.length != nTraits)
							return false;
						pop.setTraitNames(names);
					}
					return true;
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
	 * Command line option to set the traits on phase plane axes.
	 * <p>
	 * <strong>Note:</strong> option not automatically added. Modules that supports
	 * multiple traits should load it in {@link #collectCLO(CLOParser)}.
	 */
	public final CLOption cloPhase2DAxes = new CLOption("phase2daxes", "-default", Category.Module, null,
			new CLODelegate() {
				@Override
				public boolean parse(String arg) {
					if (!cloPhase2DAxes.isSet())
						return true;
					int[][] phase2daxes = CLOParser.parseIntMatrix(arg);
					if (phase2daxes.length != 2)
						return false;
					// cast check should be unnecessary. --phase2daxes should only be available if
					// module implements at least HasPhase2D (plus some other conditions).
					if (Module.this instanceof HasPhase2D) {
						Data2Phase map = ((HasPhase2D) Module.this).getPhase2DMap();
						if (map != null && map.hasSetTraits())
							map.setTraits(phase2daxes[0], phase2daxes[1]);
					}
					return true;
				}

				@Override
				public String getDescription() {
					String descr = "--phase2daxes <t>  indices of traits shown on phase plane axes\n" + //
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
		if (this instanceof Features.Multispecies)
			return;

		// prepare command line options
		if (this instanceof Features.Groups)
			parser.addCLO(cloNGroup);

		if (nTraits > 0) {
			parser.addCLO(cloTraitColors);
			parser.addCLO(cloTraitNames);
		}
		// set traits on axes of phase plane (if implemented by module)
		if (this instanceof HasPhase2D) {
			Data2Phase map = ((HasPhase2D) Module.this).getPhase2DMap();
			if (map != null && map.hasSetTraits())
				parser.addCLO(cloPhase2DAxes);
		}

		boolean anyVacant = false;
		boolean anyNonVacant = false;
		boolean anyPayoffs = false;
		int minTraits = Integer.MAX_VALUE;
		int maxTraits = -Integer.MAX_VALUE;
		for (Module mod : species) {
			boolean hasVacant = (mod.getVacantIdx() >= 0);
			anyVacant |= hasVacant;
			anyNonVacant |= !hasVacant;
			anyPayoffs |= (mod instanceof Payoffs);
			int nt = mod.getNTraits();
			minTraits = Math.min(minTraits, nt);
			maxTraits = Math.min(maxTraits, nt);
		}
		if (anyPayoffs) {
			for (Module mod : species) {
				if (!(mod instanceof Payoffs))
					continue;
				map2fitness = ((Payoffs) mod).getMap2Fitness();
				map2fitness.clo.addKeys(Map2Fitness.Map.values());
				// only add to first species implementing Payoffs
				parser.addCLO(map2fitness.clo);
				break;
			}
			if (anyNonVacant) {
				// additional options that only make sense without vacant sites
				playerUpdate.clo.addKeys(PlayerUpdate.Type.values());
				parser.addCLO(playerUpdate.clo);
			}
		}
		if (anyVacant) {
			parser.addCLO(cloDeathRate);
		}
		// add option to disable traits if >=3 traits, except >=2 traits for
		// continuous modules with no vacancies (cannot disable vacancies)
		if (minTraits > 2 || (anyNonVacant && minTraits > 1))
			parser.addCLO(cloTraitDisable);

		if (model == null)
			return;
		// population size option only acceptable for IBS and SDE models
		if (model instanceof IBS || model instanceof SDE) {
			parser.addCLO(cloNPopulation);
		}
		// geometry option only acceptable for IBS and PDE models
		if (model instanceof IBS || model instanceof PDE) {
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
