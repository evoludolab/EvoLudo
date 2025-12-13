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

package org.evoludo.simulator.models;

import java.util.Arrays;

import org.evoludo.simulator.EvoLudo;
import org.evoludo.simulator.models.Model.HasDE;
import org.evoludo.simulator.modules.Module;
import org.evoludo.util.CLOParser;
import org.evoludo.util.CLOption;

/**
 * Helper responsible for configuring initial conditions of ODE/SDE models.
 * <p>
 * Encapsulates the initialization types supported by {@link ODE}, the parsing
 * of command line configuration and the construction of initial state vectors.
 */
class ODEInitialize {

	/**
	 * Hosting ODE model used to access species and state arrays.
	 */
	private final ODE ode;

	/**
	 * Create an initializer bound to the supplied ODE model.
	 * 
	 * @param ode owning model
	 */
	ODEInitialize(ODE ode) {
		this.ode = ode;
	}

	/**
	 * Initialize the model state vectors, optionally randomizing species flagged
	 * with {@link InitType#RANDOM}.
	 * 
	 * @param doRandom {@code true} if random initialization should be applied
	 */
	void init(boolean doRandom) {
		int idx = -1;
		// y0 is initialized except for species with random initial frequencies
		if (doRandom) {
			for (Module<?> pop : ode.species) {
				if (!ode.initType[++idx].equals(InitType.RANDOM))
					continue;
				int dim = pop.getNTraits();
				int from = ode.idxSpecies[idx];
				for (int n = 0; n < dim; n++)
					ode.y0[from + n] = ode.rng.random01();
			}
		}
		System.arraycopy(ode.y0, 0, ode.yt, 0, ode.nDim);
	}

	/**
	 * Parse the CLI string describing initial conditions.
	 * 
	 * @param arg command-line fragment
	 * @return {@code true} if parsing succeeded
	 */
	boolean parse(String arg) {
		String[] inittypes = arg.split(CLOParser.SPECIES_DELIMITER);
		int idx = 0;
		int start = 0;
		for (Module<?> pop : ode.species) {
			String inittype = inittypes[idx % inittypes.length];
			String[] typeargs = inittype.split(CLOParser.SPLIT_ARG_REGEX);
			InitType itype = (InitType) ode.cloInit.match(inittype);
			String iargs = null;
			// if matching of inittype failed assume it was omitted; use previous type
			if (itype == null) {
				// if no previous match, give up
				if (idx == 0)
					return false;
				itype = ode.initType[idx - 1];
				iargs = typeargs[0];
			} else if (typeargs.length > 1)
				iargs = typeargs[1];
			int nTraits = pop.getNTraits();
			switch (itype) {
				case MUTANT:
					if (!processMutant(pop, iargs, start))
						return false;
					break;
				case DENSITY:
				case FREQUENCY:
					if (!processDensity(pop, iargs, start))
						return false;
					break;
				case RANDOM:
				case UNIFORM:
				case UNITY:
					// uniform distribution is the default. for densities set all to zero.
					double[] popinit = new double[nTraits];
					Arrays.fill(popinit, 1.0);
					appendY0(popinit, start);
					break;
				default:
					throw new IllegalArgumentException("unknown initialization type: " + itype);
			}
			ode.initType[idx] = itype;
			idx++;
			start += nTraits;
		}
		return true;
	}

	/**
	 * Parse a mutant initialization specification and append it to {@link ODE#y0}.
	 * 
	 * @param pop   module being initialized
	 * @param iargs string containing mutant parameters
	 * @param start index where the species slice begins
	 * @return {@code true} if parsing succeeded
	 */
	private boolean processMutant(Module<?> pop, String iargs, int start) {
		// SDE models only (no population size in ODE)
		double[] initargs = CLOParser.parseVector(iargs);
		if (initargs == null || initargs.length < 1)
			return false;
		// initargs contains the index of the mutant (and resident) trait
		int mutantType;
		int residentType = -1;
		int vacantType = pop.getVacantIdx();
		double vacantFreq = 0.0;
		int nt = pop.getNTraits();
		switch (initargs.length) {
			case 3:
				// vacant frequency
				if (vacantType < 0)
					return false;
				vacantFreq = Math.min(Math.max(initargs[2], 0.0), 1.0);
				//$FALL-THROUGH$
			case 2:
				residentType = (int) initargs[1];
				//$FALL-THROUGH$
			case 1:
				mutantType = (int) initargs[0];
				if (residentType < 0)
					residentType = (mutantType + 1) % nt;
				break;
			default:
				return false;
		}
		// set all initial frequencies to zero
		double[] popinit = new double[nt];
		popinit[mutantType] = 1.0 / pop.getNPopulation();
		if (vacantType >= 0) {
			popinit[vacantType] = vacantFreq;
			popinit[residentType] = Math.max(1.0 - vacantFreq - popinit[mutantType], 0.0);
		} else {
			popinit[residentType] = 1.0 - popinit[mutantType];
		}
		appendY0(popinit, start);
		return true;
	}

	/**
	 * Parse a density/frequency specification and append it to {@link ODE#y0}.
	 * 
	 * @param pop   module being initialized
	 * @param iargs density values as a string
	 * @param start index where the species slice begins
	 * @return {@code true} if parsing succeeded
	 */
	private boolean processDensity(Module<?> pop, String iargs, int start) {
		double[] initargs = CLOParser.parseVector(iargs);
		if (initargs == null || initargs.length != pop.getNTraits())
			return false;
		appendY0(initargs, start);
		return true;
	}

	/**
	 * Append the provided population initialization vector at the given offset
	 * within {@link ODE#y0}, resizing as needed.
	 * 
	 * @param popinit initialization data
	 * @param start   target offset within {@link ODE#y0}
	 */
	private void appendY0(double[] popinit, int start) {
		if (ode.y0 == null)
			ode.y0 = new double[popinit.length];
		if (ode.y0.length < start + popinit.length) {
			double[] newY0 = new double[start + popinit.length];
			System.arraycopy(ode.y0, 0, newY0, 0, ode.y0.length);
			ode.y0 = newY0;
		}
		System.arraycopy(popinit, 0, ode.y0, start, popinit.length);
	}

	/**
	 * Types of initial configurations. Currently this model supports the following
	 * density distributions:
	 * <dl>
	 * <dt>DENSITY
	 * <dd>Initial densities as specified in {@link ODE#cloInit} (density modules).
	 * <dt>FREQUENCY
	 * <dd>Initial frequencies as specified in {@link ODE#cloInit} (frequency
	 * modules).
	 * <dt>UNIFORM
	 * <dd>Uniform frequencies of traits (default; in density modules all densities
	 * are set to zero).
	 * <dt>RANDOM
	 * <dd>Random initial trait frequencies. <br>
	 * <strong>Note:</strong> Not available for density based models.
	 * </dl>
	 *
	 * @see ODE#cloInit
	 * @see #parse(String)
	 * @see PDEInitialize.Type
	 * @see PDE#parse(String)
	 */
	public enum InitType implements CLOption.Key {

		/**
		 * Set initial densities as specified. In models that support both density and
		 * frequency based dynamics, this selects the density based dynamics.
		 *
		 * @see HasDE.DualDynamics
		 */
		DENSITY("density", "initial trait densities <d1,...,dn>"),

		/**
		 * Set initial frequencies as specified. In models that support both density and
		 * frequency based dynamics, this selects the frequency based dynamics.
		 *
		 * @see HasDE.DualDynamics
		 */
		FREQUENCY("frequency", "initial trait frequencies <f1,...,fn>"),

		/**
		 * Uniform initial frequencies of traits. Not available in density based models.
		 */
		UNIFORM("uniform", "uniform initial frequencies"),

		/**
		 * Random initial trait frequencies. Not available for density based models.
		 */
		RANDOM("random", "random initial frequencies"),

		/**
		 * Uniform initial trait densities of one. In models that support both density
		 * and frequency based dynamics, this selects the density based dynamics.
		 *
		 * @see HasDE.DualDynamics
		 */
		UNITY("unity", "unit densities"),

		/**
		 * Single mutant in homogeneous resident.
		 * <p>
		 * <strong>Note:</strong> Only available for SDE models. Not available for
		 * density based models.
		 *
		 * @see SDE
		 */
		MUTANT("mutant", "single mutant, <m,r[,v]>");

		/**
		 * Key of initialization type. Used when parsing command line options.
		 *
		 * @see #parse(String)
		 */
		String key;

		/**
		 * Brief description of initialization type for help display.
		 *
		 * @see EvoLudo#getCLOHelp()
		 */
		String title;

		/**
		 * Instantiate new initialization type.
		 *
		 * @param key   identifier for parsing of command line option
		 * @param title summary of geometry
		 */
		InitType(String key, String title) {
			this.key = key;
			this.title = title;
		}

		@Override
		public String getKey() {
			return key;
		}

		@Override
		public String getTitle() {
			return title;
		}

		@Override
		public String toString() {
			return key;
		}
	}
}
