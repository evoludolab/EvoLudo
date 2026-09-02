//
// EvoLudo Project
//
// Copyright 2010-2026 Christoph Hauert
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

import java.util.ArrayList;
import java.util.Collections;

import org.evoludo.math.ArrayMath;
import org.evoludo.simulator.EvoLudo;
import org.evoludo.simulator.models.IBSD;
import org.evoludo.simulator.models.Model;
import org.evoludo.simulator.models.ModelType;
import org.evoludo.util.CLODelegate;
import org.evoludo.util.CLOParser;
import org.evoludo.util.CLOption;
import org.evoludo.util.CLOCategory;

/**
 * Parent class of all EvoLudo modules with discrete sets of traits.
 * 
 * @author Christoph Hauert
 */
public abstract class Discrete extends Module<Discrete> {

	/**
	 * The mutation operator for discrete traits.
	 */
	protected Mutation.Discrete mutation = null;

	@Override
	public Mutation.Discrete getMutation() {
		return mutation;
	}

	/**
	 * Create new module with a discrete set of traits.
	 * 
	 * @param engine the pacemaker for running the model
	 */
	protected Discrete(EvoLudo engine) {
		this(engine, null);
	}

	/**
	 * Create another module with a discrete set of traits. The additional
	 * module represents another species in multi-species modules that interact with
	 * species {@code partner}.
	 * 
	 * @param partner the partner species
	 */
	protected Discrete(Discrete partner) {
		this(partner.engine, partner);
		add(this);
	}

	/**
	 * Create a new module with a discrete set of traits with pacemaker
	 * {@code engine} and interactions with module {@code partner}. If
	 * {@code partner == null} this is a single species module and interactions
	 * within species ({@code opponent == this} holds).
	 * 
	 * @param engine  the pacemaker for running the model
	 * @param partner the partner species
	 */
	private Discrete(EvoLudo engine, Discrete partner) {
		super(engine, partner);
		if (partner == null)
			species = new ArrayList<>(Collections.singleton(this));
	}

	@Override
	public Model createModel(ModelType type) {
		Model mod = super.createModel(type);
		if (mod != null)
			return mod;
		return new IBSD(engine);
	}

	@Override
	public void load() {
		super.load();
		mutation = new Mutation.Discrete(this);
	}

	@Override
	public void unload() {
		super.unload();
		mutation = null;
	}

	/**
	 * Calculate and return the payoff/score of individuals in monomorphic
	 * populations with trait {@code type}.
	 * <p>
	 * <strong>Note:</strong> Optional implementation. Returns {@code Double#NaN}
	 * if not defined or not implemented.
	 * 
	 * @param type the index of the trait
	 * @return payoff/score in monomorphic population with trait {@code type}
	 */
	public double getMonoPayoff(int type) {
		return Double.NaN;
	}

	/**
	 * Default implementation of {@link Payoffs#getMinMonoPayoff()}. Only available
	 * to modules that implement the {@link Payoffs} interface.
	 * 
	 * @return the minimum payoff in monomorphic populations
	 */
	public double getMinMonoPayoff() {
		double min = Double.MAX_VALUE;
		for (int n = 0; n < nTraits; n++) {
			double monon = getMonoPayoff(n);
			if (monon < min)
				min = monon;
		}
		return min;
	}

	/**
	 * Default implementation of {@link Payoffs#getMaxMonoPayoff()}. Only available
	 * to modules that implement the {@link Payoffs} interface.
	 * 
	 * @return the maximum payoff in monomorphic populations
	 */
	public double getMaxMonoPayoff() {
		double max = -Double.MAX_VALUE;
		for (int n = 0; n < nTraits; n++) {
			double monon = getMonoPayoff(n);
			if (monon > max)
				max = monon;
		}
		return max;
	}

	/**
	 * Per-trait birth rates, overriding {@link Module#birthRate}. {@code null}
	 * as long as no per-trait rates have been set, in which case birth rates
	 * fall back to the single, trait-independent {@link Module#birthRate}.
	 */
	protected double[] birthRates = null;

	@Override
	public void setBirthRate(double rate) {
		birthRates = null;
		super.setBirthRate(rate);
	}

	@Override
	public void setBirthRate(double[] rates) {
		if (rates.length <= 1) {
			setBirthRate(rates.length == 0 ? 0.0 : rates[0]);
			return;
		}
		birthRates = checkRates(rates, "birth rate");
	}

	@Override
	public double getBirthRate() {
		return birthRates == null ? super.getBirthRate() : birthRates[0];
	}

	/**
	 * Get the per capita birth rate of {@code type}. Entries are cycled with
	 * modulo indexing if fewer rates than traits were specified, e.g. a single
	 * rate applies to all traits.
	 *
	 * @param type the trait for which to get the birth rate
	 * @return the birth rate of {@code type}
	 */
	public double getBirthRate(int type) {
		return birthRates == null ? getBirthRate() : birthRates[type % birthRates.length];
	}

	/**
	 * Gets the largest birth rate across all traits - trivially equal to
	 * {@link #getBirthRate()} whenever a single, trait-independent rate is set.
	 *
	 * @return the largest birth rate across all traits
	 */
	public double getMaxBirthRate() {
		return birthRates == null ? getBirthRate() : ArrayMath.max(birthRates);
	}

	/**
	 * Gets the birth rate of every non-vacant trait, in ascending trait-index
	 * order (skipping {@link #vacantIdx}, if any), materializing the modulo
	 * cycling of {@link #getBirthRate(int)} into a properly sized array (unlike
	 * {@link #birthRates} itself, which may hold fewer entries, e.g. a single,
	 * common rate, or even be {@code null}). Callers that need the birth rates
	 * of all non-vacant traits at once, such as an ODE's per-type dynamics,
	 * should use this instead of the raw field - it does not presume any
	 * particular position of {@link #vacantIdx} among the traits.
	 *
	 * @return the birth rate of each non-vacant trait
	 */
	public double[] getBirthRates() {
		double[] rates = new double[vacantIdx >= 0 ? nTraits - 1 : nTraits];
		int j = 0;
		for (int i = 0; i < nTraits; i++) {
			if (i == vacantIdx)
				continue;
			rates[j++] = getBirthRate(i);
		}
		return rates;
	}

	/**
	 * Per-trait death rates, overriding {@link Module#deathRate}. {@code null}
	 * as long as no per-trait rates have been set, in which case death rates
	 * fall back to the single, trait-independent {@link Module#deathRate}.
	 */
	protected double[] deathRates = null;

	@Override
	public void setDeathRate(double rate) {
		deathRates = null;
		super.setDeathRate(rate);
	}

	@Override
	public void setDeathRate(double[] rates) {
		if (rates.length <= 1) {
			setDeathRate(rates.length == 0 ? 0.0 : rates[0]);
			return;
		}
		deathRates = checkRates(rates, "death rate");
	}

	@Override
	public double getDeathRate() {
		return deathRates == null ? super.getDeathRate() : deathRates[0];
	}

	/**
	 * Get the per capita death rate of {@code type}. Entries are cycled with
	 * modulo indexing if fewer rates than traits were specified, e.g. a single
	 * rate applies to all traits.
	 *
	 * @param type the trait for which to get the death rate
	 * @return the death rate of {@code type}
	 */
	public double getDeathRate(int type) {
		return deathRates == null ? getDeathRate() : deathRates[type % deathRates.length];
	}

	/**
	 * Gets the largest death rate across all traits - trivially equal to
	 * {@link #getDeathRate()} whenever a single, trait-independent rate is set.
	 *
	 * @return the largest death rate across all traits
	 */
	public double getMaxDeathRate() {
		return deathRates == null ? getDeathRate() : ArrayMath.max(deathRates);
	}

	/**
	 * Gets the death rate of every non-vacant trait, in ascending trait-index
	 * order (skipping {@link #vacantIdx}, if any), materializing the modulo
	 * cycling of {@link #getDeathRate(int)} into a properly sized array (unlike
	 * {@link #deathRates} itself, which may hold fewer entries, e.g. a single,
	 * common rate, or even be {@code null}). Callers that need the death rates
	 * of all non-vacant traits at once, such as an ODE's per-type dynamics,
	 * should use this instead of the raw field - it does not presume any
	 * particular position of {@link #vacantIdx} among the traits.
	 *
	 * @return the death rate of each non-vacant trait
	 */
	public double[] getDeathRates() {
		double[] rates = new double[vacantIdx >= 0 ? nTraits - 1 : nTraits];
		int j = 0;
		for (int i = 0; i < nTraits; i++) {
			if (i == vacantIdx)
				continue;
			rates[j++] = getDeathRate(i);
		}
		return rates;
	}

	/**
	 * The flag to indicate whether models should stop once a monomorphic
	 * state has been reached.
	 */
	protected boolean monoStop = false;

	/**
	 * Set whether models should stop once a monomorphic state has been reached.
	 * 
	 * @param monoStop the flag to indicate whether to stop
	 */
	public void setMonoStop(boolean monoStop) {
		this.monoStop = monoStop;
	}

	/**
	 * Get the flag which indicates whether models stop once a monomorphic state has
	 * been reached.
	 * 
	 * @return {@code true} if models stop when reaching homogeneous states.
	 */
	public boolean getMonoStop() {
		return monoStop;
	}

	/**
	 * Command line option to request that models stop execution when reaching
	 * monomorphic population states.
	 */
	public final CLOption cloMonoStop = new CLOption("monostop", CLOCategory.Model,
			"--monostop      stop once population become monomorphic",
			new CLODelegate() {

				/**
				 * {@inheritDoc}
				 * <p>
				 * If option is provided, models are requested to stop execution once a
				 * monomorphic state is reached.
				 * 
				 * @param isSet {@code true} if option is provided
				 */
				@Override
				public boolean parse(boolean isSet) {
					// default is to continue with monomorphic populations (unless it's an absorbing
					// state)
					for (Discrete dpop : species)
						dpop.setMonoStop(isSet);
					return true;
				}
			});

	@Override
	public void collectCLO(CLOParser parser) {
		if (this instanceof Features.Multispecies)
			return;
		super.collectCLO(parser);
		parser.addCLO(cloMonoStop);
		// mutations only make sense for species with multiple traits (excluding vacant)
		if (this instanceof Features.Payoffs && (nTraits > 2 || (nTraits == 2 && !hasVacant())))
			parser.addCLO(mutation.clo);
	}
}
