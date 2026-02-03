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
		return "Prey-Predator dynamics";
	}

	@Override
	public String getAuthors() {
		return "Christoph Hauert";
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

	/*
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
	 */

	@Override
	public Model createModel(ModelType type) {
		switch (type) {
			case ODE:
				if (getModelType().isODE())
					return model;
				return new LV.ODE();
			case SDE:
				if (getModelType().isSDE())
					return model;
				return new LV.SDE();
			// case PDE: not yet ready for multiple species
			case IBS:
				if (getModelType().isIBS())
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
	 * Create a new custom implementation for IBS simulations.
	 * 
	 * @param engine the pacemaker for running the model
	 * @param module the reference to the module implementing the model
	 */
	protected IBSPop(EvoLudo engine, Discrete module) {
		super(engine, module);
	}

	@Override
	protected void updateMaxRate() {
		// maxRate is constant
		if (maxRate > 0.0)
			return;
		super.updateMaxRate();
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
