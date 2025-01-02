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

package org.evoludo.simulator.models;

import org.evoludo.util.CLOption;

/**
 * Model types that modules may support. Currently available model types are:
 * <dl>
 * <dt>IBS</dt>
 * <dd>individual based simulations</dd>
 * <dt>ODE</dt>
 * <dd>ordinary differential equations</dd>
 * <dt>SDE</dt>
 * <dd>stochastic differential equations</dd>
 * <dt>PDE</dt>
 * <dd>partial differential equations</dd>
 * </dl>
 */
public enum Type implements CLOption.Key {
	/**
	 * Individual based simulation model
	 */
	IBS("IBS", "individual based simulations"),

	/**
	 * Ordinary differential equation model
	 */
	ODE("ODE", "ordinary differential equations"),

	/**
	 * Stochastic differential equation model
	 */
	SDE("SDE", "stochastic differential equations"),

	/**
	 * Partial differential equation model
	 */
	PDE("PDE", "partial differential equations");

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
	Type(String key, String title) {
		this.key = key;
		this.title = title;
	}

	/**
	 * Parse the string <code>arg</code> and return the best matching model type.
	 * 
	 * @param arg the string to match with a model type
	 * @return the best matching model type
	 */
	public static Type parse(String arg) {
		int best = 0;
		Type match = null;
		for (Type t : values()) {
			int diff = CLOption.differAt(arg, t.key);
			if (diff > best) {
				best = diff;
				match = t;
			}
		}
		return match;
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
