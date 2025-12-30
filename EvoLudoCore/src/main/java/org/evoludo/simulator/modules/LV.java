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

import org.evoludo.simulator.EvoLudo;
import org.evoludo.simulator.models.IBSDPopulation;
import org.evoludo.simulator.models.Model;
import org.evoludo.simulator.models.Model.HasDE;
import org.evoludo.simulator.models.Model.HasIBS;
import org.evoludo.simulator.models.ModelType;
import org.evoludo.simulator.models.RungeKutta;
import org.evoludo.simulator.modules.Features.Multispecies;
import org.evoludo.simulator.views.HasMean;
import org.evoludo.simulator.views.HasPhase2D;
import org.evoludo.simulator.views.HasPop2D;
import org.evoludo.simulator.views.HasPop3D;

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
		nTraits = 2; // prey and empty sites
		vacantIdx = VACANT;
	}

	@Override
	public String getTitle() {
		return "Predator-Prey dynamics";
	}

	@Override
	public void load() {
		super.load();
		// set species name
		setName("Prey");
		// trait names (optional)
		setTraitNames(new String[] { "Prey" });
		// trait colors (optional)
		setTraitColors(new Color[] { Color.BLUE });
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
			rx = getBirthRate() - getDeathRate();
			ry = predator.getBirthRate() - predator.getDeathRate();
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
		double[] preyRates = this.competitionRates;
		double[] predRates = predator.competitionRates;
		if (isDensity) {
			// density dynamics
			change[PREY] = x * (rx - x * preyRates[getId()] + y * preyRates[predator.getId()]);
			change[predatorIdx] = y
					* (ry - y * predRates[predator.getId()] + x * predRates[getId()]);
			return;
		}
		// frequency dynamics
		double delta = x * (rx - x * (getBirthRate() + preyRates[getId()]) + y * preyRates[predator.getId()]);
		change[PREY] = delta;
		change[VACANT] = -delta;
		delta = y * (ry - y * (predator.getBirthRate() + predRates[predator.getId()])
				+ x * (1.0 - y) * predRates[getId()]);
		change[predatorIdx] = delta;
		change[nTraits + VACANT] = -delta;
	}

	@Override
	public Model createModel(ModelType type) {
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
			// LV does not advertise ODERK5 but this is the default when extending
			// RungeKutta; causes troubles with tests otherwise
			type = ModelType.ODE;
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
		nTraits = 2; // predators and empty sites
		vacantIdx = LV.VACANT;
	}

	@Override
	public void load() {
		super.load();
		// set species name
		setName("Predator");
		// trait names (optional)
		setTraitNames(new String[] { "Predator" });
		// trait colors (optional)
		setTraitColors(new Color[] { Color.RED });
	}

	@Override
	public int getDependent() {
		return LV.VACANT;
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
	 * The interaction rates with other populations rates for the predator/prey
	 * population. Convenience variable.
	 * 
	 * @see Module#getCompetitionRates()
	 */
	double[] competitionRates;

	/**
	 * The per capita birth rate for the predator/prey population. Convenience
	 * variable.
	 * 
	 * @see Module#getBirthRate()
	 */
	double birthRate;

	/**
	 * The per capita death rate for the predator/prey population. Convenience
	 * variable.
	 * 
	 * @see Module#getDeathRate()
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
		deathRate = module.getDeathRate();
		birthRate = module.getBirthRate();
		competitionRates = module.getCompetitionRates();
		// focal peer opponent
		// X 0 0 death, birth: deathRate + birthRate
		// X X 0 death, competition: deathRate + rates[module.getId()]
		// X 0 Y death, birth, predation: deathRate + birthRate + |rates[1]|
		// X X Y death, competition, predation: deathRate + rates[module.getId()] +
		// |rates[1]|
		maxRate = deathRate + Math.max(Math.max(Math.max(birthRate, // birth
				competitionRates[module.getId()]), // competition
				birthRate + Math.abs(competitionRates[opponent.getModule().getId()])), // birth + predation
				competitionRates[module.getId()] + Math.abs(competitionRates[opponent.getModule().getId()])); // competition
																												// +
																												// predation
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
			focalReproduces = birthRate;
		} else {
			// focal fails to reproduce due to lack of space
			focalReproduces = 0.0;
			// focal may also die due to competition
			focalDies += competitionRates[module.getId()];
		}
		// predation if random neighbouring opponent site is occupied
		int predation = opponent.pickNeighborSiteAt(me);
		if (!opponent.isVacantAt(predation)) {
			double predationRate = Math.abs(competitionRates[opponent.getModule().getId()]);
			if (isPredator) {
				// predator benefits from presence of prey
				focalReproduces += predationRate;
			} else {
				// prey suffers from presence of predator
				focalDies += predationRate;
			}
		}
		double totRate = focalReproduces + focalDies;
		if (totRate <= 0.0) {
			// this implies deathRate == 0. converged if at maximum population size
			// and opponent extinct.
			if (traitsCount[LV.VACANT] == 0 && opponent.getPopulationSize() == 0)
				return -1;
			return 0;
		}
		double randomTestVal = random01() * maxRate;
		if (randomTestVal >= totRate) {
			// nothing happens
			return 0;
		}
		if (randomTestVal < focalReproduces) {
			if (!peerVacant)
				return 1;
			// focal reproduces because neighbouring site is vacant
			setNextTraitAt(peer, 0); // note: works because Predator.PREDATOR == LV.PREY == 0
			commitTraitAt(peer);
			return 1;
		}
		// focal dies spontaneously, due to competition, or as prey due to predation:
		// vacate focal site (more efficient than setNextTraitAt)
		traitsNext[me] = LV.VACANT + nTraits;
		commitTraitAt(me);
		return (getPopulationSize() == 0 ? -1 : 1);
	}

	@Override
	public boolean setInitialTraits(double[] init) {
		if (init == null || init.length != nTraits)
			return false;
		// adjust vacant when setting initial frequencies through double click
		init[LV.VACANT] = 1.0 - init[0];
		return super.setInitialTraits(init);
	}
}
