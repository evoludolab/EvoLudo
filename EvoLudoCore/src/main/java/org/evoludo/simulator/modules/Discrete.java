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

import java.util.ArrayList;

import org.evoludo.simulator.EvoLudo;
import org.evoludo.util.CLOParser;
import org.evoludo.util.CLOption;
import org.evoludo.util.CLOption.CLODelegate;

/**
 * Parent class of all EvoLudo modules with discrete sets of traits.
 * 
 * @author Christoph Hauert
 */
public abstract class Discrete extends Module {

	/**
	 * The list {@code species} contains references to each species in this
	 * module. It deliberately shadows {@link Module#species} to simplify
	 * bookkeeping. During instantiation {@link Module#species} and
	 * {@code species} are linked to represent one and the same list.
	 * 
	 * @see #Discrete(EvoLudo, Discrete)
	 * @see Module#species
	 */
	@SuppressWarnings("hiding")
	ArrayList<Discrete> species;

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
		if (partner == null) {
			species = new ArrayList<Discrete>();
			// recall this.species shadows super.species for later convenience
			super.species = species;
		} else {
			// link ArrayList<Discrete> shadows
			species = partner.species;
		}
		add(this);
	}

	/**
	 * Add {@code dpop} to list of species. Duplicate entries are ignored.
	 * Allocate new list if necessary. Assign generic name to species if none
	 * provided.
	 *
	 * @param dpop the module to add to species list.
	 * @return {@code true} if {@code dpop} successfully added;
	 *         {@code false} adding failed or already included in list.
	 */
	public boolean add(Discrete dpop) {
		// do not add duplicates
		if (species.contains(dpop))
			return false;
		if (!species.add(dpop))
			return false;
		switch (species.size()) {
			case 1:
				break;
			case 2:
				// start naming species (if needed)
				for (Discrete pop : species) {
					if (pop.getName().length() < 1)
						pop.setName("Species-" + pop.ID);
				}
				break;
			default:
				if (dpop.getName().length() < 1)
					dpop.setName("Species-" + dpop.ID);
		}
		return true;
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
	public final CLOption cloMonoStop = new CLOption("monostop", "nostop", CLOption.Argument.NONE, EvoLudo.catModel,
			"--monostop      stop once population become monomorphic",
			new CLODelegate() {

				/**
				 * {@inheritDoc}
				 * <p>
				 * If option is provided, models are requested to stop execution once a
				 * monomorphic state is reached.
				 * 
				 * @param arg no argument required
				 */
				@Override
				public boolean parse(String arg) {
					// default is to continue with monomorphic populations (unless it's an absorbing
					// state)
					for (Discrete dpop : species)
						dpop.setMonoStop(cloMonoStop.isSet());
					return true;
				}
			});

	@Override
	public void collectCLO(CLOParser parser) {
		super.collectCLO(parser);
		parser.addCLO(cloMonoStop);
		parser.addCLO(mutation.clo);
	}
}
