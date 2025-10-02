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
import java.util.Arrays;
import java.util.logging.Level;

import org.evoludo.simulator.EvoLudo;
import org.evoludo.simulator.models.IBSDPopulation;
import org.evoludo.simulator.models.Model;
import org.evoludo.simulator.models.Model.HasDE;
import org.evoludo.simulator.models.Model.HasIBS;
import org.evoludo.simulator.models.RungeKutta;
import org.evoludo.simulator.models.Type;
import org.evoludo.simulator.modules.Features.Multispecies;
import org.evoludo.simulator.views.HasMean;
import org.evoludo.simulator.views.HasPhase2D;
import org.evoludo.simulator.views.HasPop2D;
import org.evoludo.simulator.views.HasPop3D;
import org.evoludo.util.CLOParser;
import org.evoludo.util.CLOption;
import org.evoludo.util.CLOption.CLODelegate;
import org.evoludo.util.CLOption.Category;

/**
 * Lotka-Volterra module for EvoLudo. This module implements the
 * classic Lotka-Volterra equations for predator-prey dynamics.
 * It supports both deterministic and stochastic simulations,
 * as well as individual-based simulations (IBS).
 * 
 * @author Christoph Hauert
 */
public class LV extends Discrete implements HasDE.ODE, HasDE.SDE, HasDE.DualDynamics, HasIBS,
		HasPop2D.Traits, HasPop3D.Traits, HasMean.Traits, HasPhase2D {

	/**
	 * The reference to the predator species.
	 */
	Predator predator;

	/**
	 * The index of the prey.
	 */
	static final int PREY = 0;

	/**
	 * The index of vacant sites. Both species use the same index.
	 */
	static final int VACANT = 1;

	/**
	 * The reaction rates for prey reproduction, predation, and competition.
	 */
	double[] rates;

	/**
	 * Prey net per capita growth rate, {@code a0 - dx}, where {@code dx} denotes
	 * the death rate. Convenience variable for derivative calculations in
	 * differential equations models.
	 */
	double rx;

	/**
	 * Predator net per capita growth rate, {@code b0 - dy}, where {@code dy}
	 * denotes the death rate. Convenience variable for derivative calculations in
	 * differential equations models.
	 */
	double ry;

	/**
	 * Create a new instance of the Lotka-Volterra module.
	 * 
	 * @param engine the pacemaker for running the module
	 */
	public LV(EvoLudo engine) {
		super(engine);
		setName("Prey");
	}

	@Override
	public String getTitle() {
		return "Predator-Prey dynamics";
	}

	@Override
	public void load() {
		super.load();
		setNTraits(2, VACANT); // prey and empty sites
		// optional
		String[] names = new String[nTraits];
		names[PREY] = "Prey";
		names[VACANT] = "Vacant";
		setTraitNames(names);
		// optional
		Color[] colors = new Color[nTraits];
		colors[PREY] = Color.BLUE;
		colors[VACANT] = Color.LIGHT_GRAY;
		setTraitColors(colors);
		predator = new Predator(this);
		predator.load();
	}

	@Override
	public void unload() {
		super.unload();
		predator.unload();
		predator = null;
	}

	@Override
	public boolean check() {
		boolean doReset = super.check();
		if (model.getType().isDE()) {
			// initialize convenience variables for derivative calculations
			rx = rates[0] - getDeathRate();
			ry = predator.rates[0] - predator.getDeathRate();
		}
		return doReset;
	}

	@Override
	public int getDependent() {
		return VACANT;
	}

	@Override
	public void setPhase2DMap(Data2Phase map) {
		// by default show PREY on horizontal and PREDATOR on vertical axis
		// of phase plane; note the nTraits of the host species
		map.setTraits(new int[] { PREY }, new int[] { nTraits + Predator.PREDATOR });
		map.setFixedAxes(true);
	}

	/**
	 * Error message templates for invalid birth parameter.
	 */
	static final String BIRTH_ERROR = "birth rate of %s must be non-negative (changed to 0).";

	/**
	 * Error message templates for invalid competition parameter.
	 */
	static final String COMPETITION_ERROR = "competition rate of %s must be non-negative (changed to 0).";

	/**
	 * Command line option to set the prey parameters.
	 */
	public final CLOption cloPrey = new CLOption("prey", "0.667,-1.333,0.0", Category.Module,
			"--prey <a0,a1[,a2]>  x' = (a0-dx)*x + a1*x*y - a2*x^2\n" +
					"           a0: birth rate\n" +
					"           dx: death rate (see --deathrate)\n" +
					"           a1: predation rate\n" +
					"           a2: competition rate",
			new CLODelegate() {

				@Override
				public boolean parse(String arg) {
					double[] args = CLOParser.parseVector(arg);
					if (args.length < 1 || args.length > 3)
						return false;
					if (rates == null || rates.length != 3)
						rates = new double[3];
					Arrays.fill(rates, 0.0);
					System.arraycopy(args, 0, rates, 0, args.length);
					// sanity checks
					if (rates[0] < 0.0) {
						rates[0] = 0.0; // birth rate must be non-negative
						if (logger.isLoggable(Level.WARNING))
							logger.warning(BIRTH_ERROR.replace("%s", getName()));
					}
					if (rates[2] < 0.0) {
						rates[2] = 0.0; // competition rate must be non-negative
						if (logger.isLoggable(Level.WARNING))
							logger.warning(COMPETITION_ERROR.replace("%s", getName()));
					}
					return true;
				}
			});

	@Override
	public void collectCLO(CLOParser parser) {
		super.collectCLO(parser);
		parser.addCLO(cloPrey);
	}

	/**
	 * The Lotka-Volterra model is defined by the following equations:
	 * \[
	 * \begin{align*}
	 * \frac{dx}{dt} =&amp; x (a_0 - d_x - a_2 x + a_1 y)\\
	 * \frac{dy}{dt} =&amp; y (b_0 - d_y - b_2 y + b_1 x)
	 * \end{align*}
	 * \]
	 * where \(x\), and \(y\) are the densities of prey and predators, respectively.
	 * \(a_0\geq 0\) indicates the per capita birth rate of prey, \(d_x\) the
	 * corresponding death rate, \(a_1\) the rate at which prey get dimished by
	 * predation, and \(a_2\geq 0\) the competition rate among prey. Similarly,
	 * \(\b_0\geq 0\) denotes the per capita birth rate of predators, \(d_y\) the
	 * corresponding death rate, \(b_1\) the rate at which predators grow due to
	 * predation, and \(b_2\geq 0\) the competition rate among predators. In the
	 * predator-prey scenario \(a_1\leq 0\) and \(b_1>0\) must hold, i.e. predators
	 * benefit from the presence of prey but not vice versa.
	 * <p>
	 * In finite populations, the Lotka-Volterra model can be defined in terms of
	 * frequencies, where \(x\), and \(y\) denote the frequencies of prey and
	 * predators, respectively, and \(1-x\), \(1-y\) the remaining available space.
	 * This yields slightly modified dynamical equations:
	 * \[
	 * \begin{align*}
	 * \frac{dx}{dt} =&amp; x (a_0 (1 - x) - d_x - a_2 x + a_1 y)\\
	 * \frac{dy}{dt} =&amp; y (b_0 (1 - y) - d_y - b_2 y + b_1 x (1 - y))
	 * \end{align*}
	 * \]
	 * <strong>Note:</strong> strictly speaking the second set of dynamical
	 * equations for frequencies violates the classical Lotka-Volterra model because
	 * they include a cubic term for increased reproduction mitigated by the other
	 * species but reduced by competition for space, i.e. \(x y (1-y) p_y\). This
	 * results in a qualitatively different dynamics.
	 * 
	 * @param t         the current time
	 * @param state     the current state of the system
	 * @param unused    an unused array (for compatibility with the {@link Payoffs}
	 *                  interface)
	 * @param change    the array to store the changes
	 * @param isDensity the flag indicating if the state is in terms of densities
	 *                  for frequencies
	 * 
	 * @see #rates
	 * @see Predator#rates
	 * @see #getDeathRate()
	 */
	void getDerivatives(double t, double[] state, double[] unused, double[] change, boolean isDensity) {
		int predatorIdx = nTraits + Predator.PREDATOR;
		double x = state[PREY];
		double y = state[predatorIdx];
		// NOTE: the cross-terms cause problems for aligning DEs and IBS results
		if (isDensity) {
			// density dynamics
			change[PREY] = x * (rx - x * rates[2] + y * rates[1]);
			change[predatorIdx] = y * (ry - y * predator.rates[2] + x * predator.rates[1]);
			return;
		}
		// frequency dynamics
		double delta = x * (rx - x * (rates[0] + rates[2]) + y * rates[1]);
		change[PREY] = delta;
		change[VACANT] = -delta;
		delta = y * (ry - y * (predator.rates[0] + predator.rates[2]) + x * (1.0 - y) * predator.rates[1]);
		change[predatorIdx] = delta;
		change[nTraits + VACANT] = -delta;
	}

	@Override
	public Model createModel(Type type) {
		switch (type) {
			case ODE:
				if (model != null && model.getType().isODE())
					return model;
				return new LV.ODE();
			case SDE:
				if (model != null && model.getType().isSDE())
					return model;
				return new LV.SDE();
			// case PDE: not yet ready for multiple species
			case IBS:
				if (model != null && model.getType().isIBS())
					return model;
				return super.createModel(type);
			default:
				return null;
		}
	}

	@Override
	public IBSPop createIBSPopulation() {
		return new IBSPop(engine, this);
	}

	/**
	 * ODE model for the Lotka-Volterra module.
	 */
	public class ODE extends RungeKutta {

		/**
		 * Constructor for the classic Lotka-Volterra model based on ordinary
		 * differential equations.
		 */
		public ODE() {
			super(LV.this.engine);
		}

		@Override
		protected void getDerivatives(double t, double[] state, double[] unused, double[] change) {
			LV.this.getDerivatives(t, state, unused, change, isDensity);
		}
	}

	/**
	 * SDE model for the LV module.
	 */
	public class SDE extends org.evoludo.simulator.models.SDE {

		/**
		 * Constructor for the classic Lotka-Volterra model based on stochastic
		 * differential equations.
		 */
		public SDE() {
			super(LV.this.engine);
		}

		@Override
		protected void getDerivatives(double t, double[] state, double[] unused, double[] change) {
			LV.this.getDerivatives(t, state, unused, change, isDensity);
		}
	}
}

/**
 * Predator class representing the predator species in the Lotka-Volterra model.
 */
class Predator extends Discrete implements Multispecies, HasDE {

	/**
	 * The index of the predator.
	 */
	static final int PREDATOR = 0;

	/**
	 * The reference to the prey species.
	 */
	LV prey;

	/**
	 * The reaction rates for predator reproduction, predation, and competition.
	 */
	double[] rates;

	/**
	 * Create a new instance of the phage population.
	 * 
	 * @param prey the reference to the prey species
	 */
	public Predator(LV prey) {
		super(prey);
		this.prey = prey;
		setName("Predator");
		setNTraits(2, LV.VACANT); // predators and empty sites
		// trait names
		String[] names = new String[nTraits];
		names[PREDATOR] = "Predator";
		names[LV.VACANT] = "Vacant";
		setTraitNames(names);
		// trait colors (automatically generates lighter versions for new strategists)
		Color[] colors = new Color[nTraits];
		colors[PREDATOR] = Color.RED;
		colors[LV.VACANT] = Color.LIGHT_GRAY;
		setTraitColors(colors);
	}

	@Override
	public int getDependent() {
		return LV.VACANT;
	}

	/**
	 * Command line option to set the predator parameters.
	 */
	public final CLOption cloPredator = new CLOption("predator", "-1.0,1.0,0.0", Category.Module,
			"--predator <b0,b1[,b2]>  y' = (b0-dy)*y + b1*x*y - b2*y^2\n" +
					"           b0: birth rate\n" +
					"           dy: death rate (see --deathrate)\n" +
					"           b1: predation rate\n" +
					"           b2: competition rate",
			new CLODelegate() {

				@Override
				public boolean parse(String arg) {
					double[] args = CLOParser.parseVector(arg);
					if (args.length < 1 || args.length > 3)
						return false;
					if (rates == null || rates.length != 3)
						rates = new double[3];
					Arrays.fill(rates, 0.0);
					System.arraycopy(args, 0, rates, 0, args.length);
					// sanity checks
					if (rates[0] < 0.0) {
						rates[0] = 0.0; // birth rate must be non-negative
						if (logger.isLoggable(Level.WARNING))
							logger.warning(LV.BIRTH_ERROR.replace("%s", getName()));
					}
					if (rates[2] < 0.0) {
						rates[2] = 0.0; // competition rate must be non-negative
						if (logger.isLoggable(Level.WARNING))
							logger.warning(LV.COMPETITION_ERROR.replace("%s", getName()));
					}
					return true;
				}
			});

	@Override
	public void collectCLO(CLOParser parser) {
		super.collectCLO(parser);
		parser.addCLO(cloPredator);
	}

	@Override
	public IBSPop createIBSPopulation() {
		return new IBSPop(engine, this);
	}
}

/**
 * Individual based simulation implementation of the Lotka-Volterra model.
 */
class IBSPop extends IBSDPopulation {

	/**
	 * The reference to the predator population. If {@code isPredator==true} then
	 * {@code predator==this}.
	 */
	IBSPop predator;

	/**
	 * The reference to the prey population. If {@code isPredator==false} then
	 * {@code prey==this}.
	 */
	IBSPop prey;

	/**
	 * The flag to indicate whether this is a predator population. Convenience
	 * variable.
	 */
	final boolean isPredator;

	/**
	 * The reaction rates for the predator/prey population. Convenience variable.
	 */
	double[] rates;

	/**
	 * The per capita death rate for the predator/prey population. Convenience
	 * variable.
	 */
	double deathRate;

	/**
	 * The maximum rate at which events can occur. This is used to convert rates
	 * into transition probabilities.
	 */
	double maxRate;

	/**
	 * Create a new custom implementation for IBS simulations.
	 * 
	 * @param engine the pacemaker for running the model
	 * @param module the reference to the module implementing the model
	 */
	protected IBSPop(EvoLudo engine, Discrete module) {
		super(engine, module);
		isPredator = (module instanceof Predator);
		if (isPredator) {
			predator = this;
			prey = (IBSPop) opponent;
		} else {
			predator = (IBSPop) opponent;
			prey = this;
		}
	}

	@Override
	public boolean check() {
		boolean doReset = super.check();
		rates = (isPredator ? ((Predator) module).rates : ((LV) module).rates);
		deathRate = module.getDeathRate();
		// focal peer opponent
		// X 0 0 death, birth: deathRate + rates[0]
		// X X 0 death, competition: deathRate + rates[2]
		// X 0 Y death, birth, predation: deathRate + rates[0] + |rates[1]|
		// X X Y death, competition, predation: deathRate + rates[2] + |rates[1]|
		maxRate = deathRate + Math.max(Math.max(Math.max(rates[0], // birth
				rates[2]), // competition
				rates[0] + Math.abs(rates[1])), // birth + predation
				rates[2] + Math.abs(rates[1])); // competition + predation
		return doReset;
	}

	@Override
	public double getSpeciesUpdateRate() {
		return maxRate * getPopulationSize();
	}

	/**
	 * Individual based simulation implementation of the classical Lotka-Volterra
	 * model in finite and structured populations. For a focal individual three
	 * events are possible based on the occupancy of a randomly selected
	 * neighbouring site {@code A} of the peer species as well as one of the
	 * opponent species {@code B} :
	 * <ol>
	 * <li>reproduction: occurs at rate {@code rates[0]} if {@code A} is empty;
	 * <li>death: spontaneous death occurs at rate {@code rates[0]} and death due to
	 * competition with peers occurs at rate {@code rates[2]} if {@code A} is
	 * occupied;
	 * <li>predation: prey gets eaten at rate {@code rates[1]} if {@code B} is
	 * occupied, while predators have a chance to reproduce, provided {@code A} is
	 * empty.
	 * </ol>
	 * The relative rates at which the two species are updated depends on the
	 * population size as well as the above rates. In well-mixed populations and in
	 * the limit of large populations the above updating rules recover the ODE
	 * model.
	 * 
	 * @see #getSpeciesUpdateRate()
	 * @see LV#getDerivatives(double, double[], double[], double[], boolean)
	 */
	@Override
	protected int updatePlayerEcologyAt(int me) {
		debugFocal = me;
		debugModel = -1;
		double focalDies = deathRate;
		double focalReproduces;
		int peer = pickNeighborSiteAt(me);
		boolean peerVacant = isVacantAt(peer);
		if (peerVacant) {
			// focal may reproduce because neighbouring site is vacant
			focalReproduces = rates[0];
		} else {
			// focal fails to reproduce due to lack of space
			focalReproduces = 0.0;
			// focal may also die due to competition
			focalDies += rates[2];
		}
		// predation if random neighbouring opponent site is occupied
		double focalPredation = 0.0;
		int predation = opponent.pickNeighborSiteAt(me);
		if (!opponent.isVacantAt(predation)) {
			// assume predator.rates[1] > 0 and prey.rates[1] < 0 (at least for now)
			focalPredation = Math.abs(rates[1]);
			// NOTE: the cross-terms cause problems for aligning DEs and IBS results
			// if (!isPredator)
			// focalPredation += Math.abs(predator.rates[1]);
		}
		double totRate = focalReproduces + focalDies + focalPredation;
		if (totRate <= 0.0) {
			// this implies deathRate == 0. converged if at maximum population size
			// and opponent extinct.
			if (traitsCount[VACANT] == 0 && opponent.getPopulationSize() == 0)
				return -1;
			return 0;
		}
		double randomTestVal = random01() * maxRate;
		if (randomTestVal >= totRate) {
			// nothing happens
			return 0;
		}
		boolean isExtinct = false;
		if (randomTestVal < focalReproduces) {
			// focal reproduces spontaneously
			setNextTraitAt(peer, 0); // note: works because Predator.PREDATOR == LV.PREY == 0
			commitTraitAt(peer);
		} else {
			randomTestVal -= focalReproduces;
			if (randomTestVal < focalDies) {
				// focal dies spontaneously or due to competition: vacate focal site
				// more efficient than setNextTraitAt
				traitsNext[me] = VACANT + nTraits;
				commitTraitAt(me);
				isExtinct = (getPopulationSize() == 0);
			} else {
				// focal predation: prey dies, predator reproduces if neighbour vacant
				if (isPredator) {
					// predator reproduces provided random neighbouring site is vacant
					if (peerVacant) {
						setNextTraitAt(peer, Predator.PREDATOR);
						commitTraitAt(peer);
					}
				} else {
					// prey dies; more efficient than setNextTraitAt
					traitsNext[me] = VACANT + nTraits;
					commitTraitAt(me);
					isExtinct = (getPopulationSize() == 0);
				}
			}
		}
		return (isExtinct ? -1 : 1);
	}

	@Override
	public boolean setInitialTraits(double[] init) {
		if (init == null || init.length != nTraits)
			return false;
		// adjust vacant when setting initial frequencies through double click
		init[VACANT] = 1.0 - init[0];
		return super.setInitialTraits(init);
	}
}
