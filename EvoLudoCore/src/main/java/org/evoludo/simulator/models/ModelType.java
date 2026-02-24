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

package org.evoludo.simulator.models;

import org.evoludo.util.CLOption;

/**
 * Model types that modules may support. Currently available model types are:
 * <dl>
 * <dt>IBS</dt>
 * <dd>individual based simulations</dd>
 * <dt>ODE</dt>
 * <dd>ordinary differential equations (default)</dd>
 * <dt>ODEEM</dt>
 * <dd>ODE, Euler method</dd>
 * <dt>ODERK5</dt>
 * <dd>ODE, Fifth order Runge-Kutta method</dd>
 * <dt>SDE</dt>
 * <dd>stochastic differential equations</dd>
 * <dt>PDE</dt>
 * <dd>partial differential equations (default)</dd>
 * <dt>PDERD</dt>
 * <dd>PDE, reaction-diffusion model</dd>
 * <dt>PDEADV</dt>
 * <dd>PDE, reaction-diffusion-advection model</dd>
 * </dl>
 */
public enum ModelType implements CLOption.Key {
	/**
	 * Individual based simulation model.
	 */
	IBS("IBS", "individual based simulations"),

	/**
	 * Ordinary differential equation model, defaults to RK5.
	 */
	ODE("ODE", "ordinary differential equations"),

	/**
	 * Fifth order Runge-Kutta method for ordinary differential equation models.
	 */
	RK5("ODERK5", "Fifth order Runge-Kutta method"),

	/**
	 * Euler method for ordinary differential equation models.
	 */
	EM("ODEEM", "Euler method"),

	/**
	 * Euler-Maruyama method for stochastic differential equation models.
	 */
	SDE("SDE", "Euler-Maruyama method"),

	/**
	 * Partial differential equation model, defaults to PDERD (no advection).
	 */
	PDE("PDE", "partial differential equations"),

	/**
	 * Reaction-diffusion model.
	 */
	PDERD("PDERD", "Reaction-diffusion model"),

	/**
	 * Reaction-diffusion-advection model.
	 */
	PDEADV("PDEADV", "Reaction-diffusion-advection model"),

	/**
	 * Place holder if no model available.
	 */
	NONE("NONE", "No model");

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
	ModelType(String key, String title) {
		this.key = key;
		this.title = title;
	}

	/**
	 * Parse the string <code>arg</code> and return the best matching model type.
	 * 
	 * @param arg the string to match with a model type
	 * @return the best matching model type
	 */
	public static ModelType parse(String arg) {
		int best = 0;
		ModelType match = null;
		for (ModelType t : values()) {
			int diff = CLOption.differAt(arg, t.key);
			if (diff > best) {
				best = diff;
				match = t;
			}
		}
		return match;
	}

	/**
	 * Check whether this model type matches the given query type. Distinguishes
	 * between ODE/SDE/PDE/IBS families while remaining agnostic to implementation
	 * details.
	 *
	 * @param query the type or type-family to compare against
	 * @return {@code true} if this type matches {@code query}
	 */
	protected boolean isType(ModelType query) {
		if (query == null)
			return false;
		if (this == query)
			return true;
		switch (query) {
			case ODE:
			case RK5:
			case EM:
				return this == ODE || this == RK5 || this == EM;
			case SDE:
				return this == SDE;
			case PDE:
			case PDERD:
			case PDEADV:
				return this == PDE || this == PDERD || this == PDEADV;
			case IBS:
				return this == IBS;
			default:
				return false;
		}
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
