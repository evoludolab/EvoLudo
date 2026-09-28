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
import java.util.Arrays;
import java.util.Collections;

import org.evoludo.math.ArrayMath;
import org.evoludo.simulator.EvoLudo;
import org.evoludo.simulator.models.CModel;
import org.evoludo.simulator.models.IBSC;
import org.evoludo.simulator.models.Model;
import org.evoludo.simulator.models.ModelType;
import org.evoludo.simulator.views.HasPop2D;
import org.evoludo.simulator.views.HasPop3D;
import org.evoludo.util.CLODelegate;
import org.evoludo.util.CLOParser;
import org.evoludo.util.CLOption;
import org.evoludo.util.CLOCategory;
import org.evoludo.util.Formatter;

/**
 * Parent class of all EvoLudo modules with one or more continuous traits.
 * 
 * @author Christoph Hauert
 */
public abstract class Continuous extends Module<Continuous> {

	/**
	 * Coloring methods for multiple continuous traits. Enum on steroids. Currently
	 * available coloring types are:
	 * <dl>
	 * <dt>traits
	 * <dd>Each trait refers to a color channel. At most three traits for
	 * <span style="color:red;">red</span>, <span style="color:green;">green</span>,
	 * and <span style="color:blue;">blue</span> components. The brightness of the
	 * color indicates the value of the continuous trait. This is the default.
	 * <dt>distance
	 * <dd>Color the traits according to their (Euclidian) distance from the origin
	 * (heat map ranging from black and grey to yellow and red).
	 * <dt>DEFAULT
	 * <dd>Default coloring type. Not user selectable.
	 * </dl>
	 * 
	 * @see #cloTraitColorScheme
	 */
	public enum ColorModelType implements CLOption.Key {

		/**
		 * Each trait refers to a color channel. At most three traits for
		 * <span style="color:red;">red</span>, <span style="color:green;">green</span>,
		 * and <span style="color:blue;">blue</span> components.
		 */
		TRAITS("traits", "each trait (&le;3) refers to color channel"), //

		/**
		 * Color the traits according to their (Euclidian) distance from the origin.
		 */
		DISTANCE("distance", "distance of traits from origin"), //

		/**
		 * Default coloring type. Not user selectable.
		 */
		DEFAULT("-default", "default coloring scheme");

		/**
		 * The name of the color model type.
		 */
		private final String key;

		/**
		 * Brief description of the color model type for help display.
		 * 
		 * @see EvoLudo#getCLOHelp()
		 */
		private final String title;

		/**
		 * Create a new color model type.
		 * 
		 * @param key   the name of the color model
		 * @param title the title of the color model
		 * 
		 * @see #cloTraitColorScheme
		 */
		ColorModelType(String key, String title) {
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
			return key + ": " + title;
		}
	}

	/**
	 * The coloring method type.
	 */
	protected ColorModelType colorModelType = ColorModelType.DEFAULT;

	/**
	 * Command line option to set color scheme for coloring continuous traits.
	 * 
	 * @see ColorModelType
	 */
	public final CLOption cloTraitColorScheme = new CLOption("traitcolorscheme", "traits", CLOCategory.GUI,
			"--traitcolorscheme <m>  color scheme for traits:", //
			new CLODelegate() {
				@Override
				public boolean parse(String arg) {
					setColorModelType((ColorModelType) cloTraitColorScheme.match(arg));
					return true;
				}
			});

	/**
	 * Get the type of color model for translating continuous traits into colors.
	 * 
	 * @return the type of color model
	 * 
	 * @see #cloTraitColorScheme
	 */
	public ColorModelType getColorModelType() {
		return colorModelType;
	}

	/**
	 * Set the type of the color model for translating continuous traits into
	 * colors.
	 * 
	 * @param colorModelType the new type of color model
	 * 
	 * @see #cloTraitColorScheme
	 */
	public void setColorModelType(ColorModelType colorModelType) {
		if (colorModelType == null)
			return;
		this.colorModelType = colorModelType;
	}

	/**
	 * Shortcut for species.get(0) as long as continuous modules are restricted to a
	 * single species.
	 */
	Continuous population;

	/**
	 * The flag that indicates whether maximal and minimal scores have already been
	 * calculated.
	 * 
	 * @see #setExtremalScores()
	 */
	protected boolean extremalScoresSet = false;

	/**
	 * The trait minima.
	 * <p>
	 * <strong>Note:</strong> internally traits are always in \([0,1]\).
	 * {@code traitMin} and {@code traitMax} are used to transform traits
	 * appropriately for results.
	 */
	protected double[] traitMin;

	/**
	 * The trait maxima.
	 * <p>
	 * <strong>Note:</strong> internally traits are always in \([0,1]\).
	 * {@code traitMin} and {@code traitMax} are used to transform traits
	 * appropriately for results.
	 */
	protected double[] traitMax;

	/**
	 * The mutation operator for continuous traits.
	 */
	protected Mutation.Continuous mutation = null;

	@Override
	public Mutation.Continuous getMutation() {
		return mutation;
	}

	/**
	 * The map to translate traits of interacting individuals into payoffs.
	 */
	protected Traits2Payoff traits2payoff;

	/**
	 * Create new module with continuous traits.
	 * 
	 * @param engine the pacemaker for running the model
	 */
	protected Continuous(EvoLudo engine) {
		this(engine, null);
	}

	/**
	 * Create another module with continuous traits. The additional module
	 * represents another species in multi-species modules that interact with
	 * species {@code partner}.
	 * 
	 * @param partner the partner species
	 */
	protected Continuous(Continuous partner) {
		this(partner.engine, partner);
	}

	/**
	 * Create a new module with continuous traits for pacemaker {@code engine} and
	 * interactions with module {@code partner}. If {@code partner == null} this is
	 * a single species module and interactions within species
	 * ({@code opponent == this} holds).
	 * 
	 * @param engine  the pacemaker for running the model
	 * @param partner the partner species
	 */
	protected Continuous(EvoLudo engine, Continuous partner) {
		super(engine, partner);
		if (partner == null)
			species = new ArrayList<>(Collections.singleton(this));
		// shortcut while continuous modules are restricted to single species
		population = this;
	}

	@Override
	public Model createModel(ModelType type) {
		Model mod = super.createModel(type);
		if (mod != null)
			return mod;
		return new IBSC(engine);
	}

	@Override
	public void load() {
		super.load();
		traits2payoff = new Traits2Payoff();
		mutation = new Mutation.Continuous(this);
	}

	@Override
	public void unload() {
		super.unload();
		traits2payoff = null;
		mutation = null;
	}

	@Override
	public boolean check() {
		boolean doReset = super.check();
		if (this instanceof Payoffs)
			setExtremalScores();
		// verify trait minima and maxima
		for (int s = 0; s < nTraits; s++) {
			if (traitMax[s] <= traitMin[s]) {
				// set to default
				engine.logWarning("invalid trait range [" + Formatter.format(traitMin[s], 4) + ", "
						+ Formatter.format(traitMax[s], 4) + "] for trait " + s + " - reset to [0, 1]!");
				setTraitRange(0.0, 1.0, s);
				doReset = true;
			}
		}
		// verify maximum combined trait value (requires valid trait ranges)
		if (maxInvest < Double.POSITIVE_INFINITY && maxInvest <= ArrayMath.norm(traitMin)) {
			engine.logWarning("maximum combined trait value " + Formatter.format(maxInvest, 4)
					+ " does not exceed combined trait minima - no limit!");
			setMaxInvest(Double.POSITIVE_INFINITY);
			doReset = true;
		}
		return doReset;
	}

	/**
	 * Default implementation of {@link Payoffs#getMinPayoff()}. Only available
	 * to modules that implement the {@link Payoffs} interface.
	 * 
	 * @return the minimum payoff in monomorphic populations
	 *
	 * @see #setExtremalScores()
	 */
	public double getMinPayoff() {
		if (!extremalScoresSet)
			setExtremalScores();
		return cxMinScore;
	}

	/**
	 * Default implementation of {@link Payoffs#getMaxPayoff()}. Only available
	 * to modules that implement the {@link Payoffs} interface.
	 * 
	 * @return the minimum payoff in monomorphic populations
	 *
	 * @see #setExtremalScores()
	 */
	public double getMaxPayoff() {
		if (!extremalScoresSet)
			setExtremalScores();
		return cxMaxScore;
	}

	/**
	 * Default implementation of {@link Payoffs#getMinMonoPayoff()}. Only available
	 * to modules that implement the {@link Payoffs} interface.
	 * 
	 * @return the minimum payoff in monomorphic populations
	 */
	public double getMinMonoPayoff() {
		if (!extremalScoresSet)
			setExtremalScores();
		return cxMinMonoScore;
	}

	/**
	 * Default implementation of {@link Payoffs#getMaxMonoPayoff()}. Only available
	 * to modules that implement the {@link Payoffs} interface.
	 * 
	 * @return the maximum payoff in monomorphic populations
	 */
	public double getMaxMonoPayoff() {
		if (!extremalScoresSet)
			setExtremalScores();
		return cxMaxMonoScore;
	}

	/**
	 * Get the minima for all traits.
	 *
	 * @return the array with the trait minima
	 */
	public double[] getTraitMin() {
		return traitMin;
	}

	/**
	 * Get the maxima for all traits.
	 *
	 * @return the array with the trait maxima
	 */
	public double[] getTraitMax() {
		return traitMax;
	}

	/**
	 * Set the trait minimum and maximum for trait {@code trait}.
	 * 
	 * @param min   the trait minimum
	 * @param max   the trait maximum
	 * @param trait the index of the trait
	 */
	public void setTraitRange(double min, double max, int trait) {
		if (traitMin == null || traitMin.length != nTraits) {
			traitMin = new double[nTraits];
			Arrays.fill(traitMin, 0.0);
		}
		if (traitMax == null || traitMax.length != nTraits) {
			traitMax = new double[nTraits];
			Arrays.fill(traitMax, 0.0);
		}
		if (trait < 0 || trait >= nTraits || min >= max)
			return;
		traitMax[trait] = max;
		traitMin[trait] = min;
		extremalScoresSet = false; // update extremal scores
	}

	/**
	 * The maximum combined trait value, i.e. the sum of all traits (in the units
	 * of their respective trait ranges) must not exceed {@code maxInvest}.
	 * {@link Double#POSITIVE_INFINITY} imposes no limit. Mutations and initial
	 * configurations respect the limit, see
	 * {@link Mutation.Continuous#getExcessInvest(double[], int)}.
	 *
	 * @see #cloMaxInvest
	 */
	protected double maxInvest = Double.POSITIVE_INFINITY;

	/**
	 * Get the maximum combined trait value.
	 *
	 * @return the maximum combined trait value
	 *
	 * @see #maxInvest
	 */
	public double getMaxInvest() {
		return maxInvest;
	}

	/**
	 * Set the maximum combined trait value.
	 *
	 * @param maxInvest the maximum combined trait value
	 *
	 * @see #maxInvest
	 */
	public void setMaxInvest(double maxInvest) {
		this.maxInvest = maxInvest;
		extremalScoresSet = false; // update extremal scores
	}

	/**
	 * Translate continuous traits into payoffs based on configurable cost and
	 * benefit functions.
	 */
	public class Traits2Payoff {

		/**
		 * Create a payoff translator bound to the enclosing module.
		 */
		public Traits2Payoff() {
		}

		/**
		 * The array of cost functions, one for each trait.
		 */
		Costs[] costs;

		/**
		 * The 2D array of cost function parameters. The rows refer to the different
		 * traits and the columns to their cost parameters. {@code ci} is not
		 * necessarily a square array because the each trait may have different numbers
		 * of parameters.
		 * 
		 * @see Costs#nParams
		 */
		double[][] ci;

		/**
		 * Get the array of cost functions, one for each trait.
		 * 
		 * @return the array of cost functions
		 */
		public Costs[] getCostFunctions() {
			return costs;
		}

		/**
		 * Get the cost function for trait {@code index}.
		 * 
		 * @param index the index of the trait
		 * @return the cost function
		 */
		public Costs getCostFunction(int index) {
			return costs[index];
		}

		/**
		 * Set the cost function of the trait {@code index} to {@code costfcn} with
		 * parameters in the array {@code cparams}.
		 * 
		 * @param costfcn the cost function
		 * @param cparams the array of parameters for {@code costfcn}
		 * @param index   the index of the trait
		 */
		public void setCostFunction(Costs costfcn, double[] cparams, int index) {
			if (costs == null || costs.length != nTraits)
				costs = new Costs[nTraits];
			costs[index] = costfcn;
			setCostParameters(cparams, index);
		}

		/**
		 * Set the array of cost function parameters for trait with index {@code idx} to
		 * array {@code params}.
		 * 
		 * @param cparams the array of cost function parameters
		 * @param index   the index of the trait
		 * 
		 * @see #ci
		 */
		public void setCostParameters(double[] cparams, int index) {
			if (ci == null || ci.length != nTraits)
				ci = new double[nTraits][];
			int nParams = costs[index].nParams;
			if (ci[index] == null || ci[index].length != nParams)
				ci[index] = new double[nParams];
			System.arraycopy(cparams, 0, ci[index], 0, nParams);
		}

		/**
		 * Get the array of cost function parameters for trait with index {@code idx}.
		 * 
		 * @param index the index of the trait
		 * @return the array of cost function parameters
		 * 
		 * @see #ci
		 */
		public double[] getCostParameters(int index) {
			return ci[index];
		}

		/**
		 * Get the 2D array of cost function parameters.
		 * 
		 * @return the 2D array of cost function parameters
		 * 
		 * @see #ci
		 */
		public double[][] getCostParameters() {
			return ci;
		}

		/**
		 * Return formatted string of the cost function of trait index {@code idx}.
		 * 
		 * @param idx the index of the trait
		 * @return the formatted string
		 */
		public String formatCosts(int idx) {
			Costs cost = costs[idx];
			return getTraitName(idx) + ": " + cost.key + ": " + cost.title + " " + Formatter.format(ci[idx], 4);
		}

		/**
		 * The array of benefit functions, one for each trait.
		 */
		Benefits[] benefits;

		/**
		 * The 2D array of cost function parameters. The rows refer to the different
		 * traits and the columns to their cost parameters. {@code bi} is not
		 * necessarily a square array because the each trait may have different numbers
		 * of parameters.
		 * 
		 * @see Benefits#nParams
		 */
		double[][] bi;

		/**
		 * Get the array of benefit functions for each trait.
		 * 
		 * @return the array of benefit functions
		 */
		public Benefits[] getBenefitFunctions() {
			return benefits;
		}

		/**
		 * Get the benefit function for trait {@code index}.
		 * 
		 * @param index the index of the trait
		 * @return the benefit function
		 */
		public Benefits getBenefitFunction(int index) {
			return benefits[index];
		}

		/**
		 * Set the benefit function of the trait {@code index} to {@code benefitfcn}.
		 * 
		 * @param benefitfcn the benefit function
		 * @param bparams    the array of benefit function parameters
		 * @param index      the index of the trait
		 */
		public void setBenefitFunction(Benefits benefitfcn, double[] bparams, int index) {
			if (benefits == null || benefits.length != nTraits)
				benefits = new Benefits[nTraits];
			benefits[index] = benefitfcn;
			setBenefitParameters(bparams, index);
		}

		/**
		 * Set the array of benefit function parameters for trait with index {@code idx}
		 * to array {@code params}.
		 * 
		 * @param bparams the array of benefit function parameters
		 * @param index   the index of the trait
		 * 
		 * @see #bi
		 */
		public void setBenefitParameters(double[] bparams, int index) {
			if (bi == null || bi.length != nTraits)
				bi = new double[nTraits][];
			int nParams = benefits[index].nParams;
			if (bi[index] == null || bi[index].length != nParams)
				bi[index] = new double[nParams];
			System.arraycopy(bparams, 0, bi[index], 0, nParams);
		}

		/**
		 * Get the array of benefit function parameters for trait with index
		 * {@code idx}.
		 * 
		 * @param index the index of the trait
		 * @return the array of benefit function parameters
		 * 
		 * @see #bi
		 */
		public double[] getBenefitParameters(int index) {
			return bi[index];
		}

		/**
		 * Get the 2D array of benefit function parameters.
		 * 
		 * @return the 2D array of benefit function parameters
		 * 
		 * @see #bi
		 */
		public double[][] getBenefitParameters() {
			return bi;
		}

		/**
		 * Return formatted string of the benefit function of trait index {@code idx}.
		 * 
		 * @param idx the index of the trait
		 * @return the formatted string
		 */
		public String formatBenefits(int idx) {
			Benefits benefit = benefits[idx];
			return getTraitName(idx) + ": " + benefit.key + " " + benefit.title + " " + Formatter.format(bi[idx], 4);
		}

		/**
		 * Calculate the payoff to the focal individual with traits {@code me} when
		 * interacting with an opponent with traits {@code you}.
		 * 
		 * @param me  the array of traits of the focal individual
		 * @param you the array of traits of the opponent individual
		 * @return the payoff to {@code me}
		 */
		public double payoff(double[] me, double[] you) {
			// assumes that benefits and costs can be decomposed into the different traits
			return benefits(me, you) - costs(me, you);
		}

		/**
		 * Calculate the payoff to the focal individual with trait {@code me} when
		 * interacting with an opponent with trait {@code you}.
		 *
		 * @param me  the trait of the focal individual
		 * @param you the trait of the opponent individual
		 * @return the payoff to {@code me}
		 */
		public double payoff(double me, double you) {
			// assumes that benefits and costs can be decomposed into the different traits
			return benefits(me, you, 0) - costs(me, you, 0);
		}

		/**
		 * Calculate the costs to the focal individual with traits {@code me} when
		 * interacting with an opponent with traits {@code you}.
		 * 
		 * @param me  the array of traits of the focal individual
		 * @param you the array of traits of the opponent individual
		 * @return the costs to {@code me}
		 */
		protected double costs(double[] me, double[] you) {
			double totcosts = 0.0;
			for (int n = 0; n < nTraits; n++)
				totcosts += costs(me[n], you[n], n);
			return totcosts;
		}

		/**
		 * Calculate the costs to the focal individual with trait value {@code me} in
		 * trait with index {@code trait} when interacting with an opponent with trait
		 * value {@code you}.
		 * 
		 * @param me    the trait value of the focal individual
		 * @param you   the trait value of the opponent individual
		 * @param trait the index of the trait
		 * @return the costs to {@code me}
		 */
		protected double costs(double me, double you, int trait) {
			double shift = traitMin[trait];
			double scale = traitMax[trait] - shift;
			return costs[trait].eval(me * scale + shift, you * scale + shift, ci[trait]);
		}

		/**
		 * Calculate the benefits to the focal individual with traits {@code me} when
		 * interacting with an opponent with traits {@code you}.
		 * 
		 * @param me  the array of traits of the focal individual
		 * @param you the array of traits of the opponent individual
		 * @return the benefits to {@code me}
		 */
		protected double benefits(double[] me, double[] you) {
			double totbenefits = 0.0;
			for (int n = 0; n < nTraits; n++)
				totbenefits += benefits(me[n], you[n], n);
			return totbenefits;
		}

		/**
		 * Calculate the benefits to the focal individual with trait value {@code me} in
		 * trait with index {@code trait} when interacting with an opponent with trait
		 * value {@code you}.
		 * 
		 * @param me    the trait value of the focal individual
		 * @param you   the trait value of the opponent individual
		 * @param trait the index of the trait
		 * @return the benefits to {@code me}
		 */
		protected double benefits(double me, double you, int trait) {
			double shift = traitMin[trait];
			double scale = traitMax[trait] - shift;
			return benefits[trait].eval(me * scale + shift, you * scale + shift, bi[trait]);
		}
	}

	/**
	 * Shapes of functions \(f(z)\) translating (combinations of) traits into
	 * payoffs. All shapes satisfy \(f(0)=0\) and are parametrized by the
	 * coefficients \(a_0, a_1, \ldots\). The shape determines the last digit of the
	 * keys of cost and benefit functions, see {@link Costs} and {@link Benefits}.
	 */
	public enum Shape implements CLOption.Key {

		/**
		 * Linear function: \(f(z)=a_0 z\).
		 */
		LINEAR("linear", "a0*z", 1, 0),

		/**
		 * Quadratic function: \(f(z)=a_0 z+a_1 z^2\).
		 */
		QUADRATIC("quadratic", "a0*z+a1*z^2", 2, 1),

		/**
		 * Square root function: \(f(z)=a_0 \sqrt{z}\).
		 */
		SQRT("sqrt", "a0*sqrt(z)", 1, 2),

		/**
		 * Logarithmic function: \(f(z)=a_0 \ln(a_1 z+1)\).
		 */
		LOG("log", "a0*ln(a1*z+1)", 2, 3),

		/**
		 * Saturating exponential function: \(f(z)=a_0 (1-\exp(-a_1 z))\).
		 */
		EXP("exp", "a0*(1-exp(-a1*z))", 2, 4),

		/**
		 * Cubic function: \(f(z)=a_0 z+a_1 z^2+a_2 z^3\).
		 */
		CUBIC("cubic", "a0*z+a1*z^2+a2*z^3", 3, 5),

		/**
		 * Quartic function: \(f(z)=a_0 z+a_1 z^2+a_2 z^3+a_3 z^4\).
		 */
		QUARTIC("quartic", "a0*z+a1*z^2+a2*z^3+a3*z^4", 4, 6),

		/**
		 * Power function: \(f(z)=a_0 z^{a_1}\).
		 */
		POWER("power", "a0*z^a1", 2, 7);

		/**
		 * The key of the shape. Used when parsing command line options.
		 */
		private final String key;

		/**
		 * The formula of the shape in terms of the argument {@code z} and the
		 * parameters {@code a0, a1, ...}.
		 */
		private final String formula;

		/**
		 * The number of parameters of the shape.
		 */
		final int nParams;

		/**
		 * The code of the shape, i.e. the last digit of the keys of cost and benefit
		 * functions.
		 */
		final int code;

		/**
		 * Create a new shape with key {@code key}, formula {@code formula},
		 * {@code nParams} parameters and code {@code code}.
		 * 
		 * @param key     the identifier for parsing of command line options
		 * @param formula the formula of the shape
		 * @param nParams the number of parameters
		 * @param code    the code of the shape
		 */
		Shape(String key, String formula, int nParams, int code) {
			this.key = key;
			this.formula = formula;
			this.nParams = nParams;
			this.code = code;
		}

		/**
		 * Get the number of parameters of the shape.
		 * 
		 * @return the number of parameters
		 */
		public int getNParams() {
			return nParams;
		}

		/**
		 * Evaluate the function with shape {@code this} and parameters {@code a} at
		 * {@code z}.
		 * 
		 * @param z the argument
		 * @param a the parameters
		 * @return the function value
		 */
		public double eval(double z, double[] a) {
			switch (this) {
				case QUADRATIC:
					return (a[1] * z + a[0]) * z;
				case SQRT:
					return a[0] * Math.sqrt(z);
				case LOG:
					return a[0] * Math.log(a[1] * z + 1.0);
				case EXP:
					return a[0] * (1.0 - Math.exp(-a[1] * z));
				case CUBIC:
					return ((a[2] * z + a[1]) * z + a[0]) * z;
				case QUARTIC:
					return (((a[3] * z + a[2]) * z + a[1]) * z + a[0]) * z;
				case POWER:
					return a[0] * Math.pow(z, a[1]);
				case LINEAR:
				default:
					return a[0] * z;
			}
		}

		/**
		 * Format the formula of the shape for the argument {@code arg} with parameters
		 * named {@code param}.
		 * 
		 * @param arg   the name of the argument
		 * @param param the name of the parameters
		 * @return the formatted formula
		 */
		String format(String arg, char param) {
			// avoid double parentheses, e.g. sqrt((x+y))
			String bare = (arg.startsWith("(") && arg.endsWith(")") ? arg.substring(1, arg.length() - 1) : arg);
			return formula.replace('a', param).replace("(z)", "(" + bare + ")").replace("z", arg);
		}

		@Override
		public String getKey() {
			return key;
		}

		@Override
		public String getTitle() {
			return "f(z)=" + formula;
		}

		@Override
		public String toString() {
			return key + ": " + getTitle();
		}
	}

	/**
	 * The arguments of cost and benefit functions in pairwise interactions between
	 * a focal individual with trait \(x\) and its opponent with trait \(y\). The
	 * argument determines the first digit of the keys of cost and benefit
	 * functions, see {@link Costs} and {@link Benefits}.
	 */
	public enum Argument {

		/**
		 * The trait of the focal individual, \(x\).
		 */
		ME(0, "x"),

		/**
		 * The sum of the traits of the focal individual and its opponent, \(x+y\).
		 */
		WE(1, "(x+y)"),

		/**
		 * The traits of the focal individual and its opponent including their
		 * interaction, \(c_0 x+c_1 y+c_2 x y\). Not associated with a shape.
		 */
		MEYOU(2, "x,y"),

		/**
		 * The trait of the opponent, \(y\).
		 */
		YOU(3, "y");

		/**
		 * The code of the argument, i.e. the first digit of the keys of cost and
		 * benefit functions.
		 */
		final int code;

		/**
		 * The label of the argument for formatting formulas.
		 */
		final String label;

		/**
		 * Create a new argument with code {@code code} and label {@code label}.
		 * 
		 * @param code  the code of the argument
		 * @param label the label of the argument
		 */
		Argument(int code, String label) {
			this.code = code;
			this.label = label;
		}

		/**
		 * Select the argument for the traits {@code me} and {@code you} of the focal
		 * individual and its opponent, respectively.
		 * 
		 * @param me  the trait of the focal individual
		 * @param you the trait of the opponent
		 * @return the argument
		 */
		double select(double me, double you) {
			switch (this) {
				case YOU:
					return you;
				case WE:
					return me + you;
				case ME:
				default:
					return me;
			}
		}

		/**
		 * Get the key of the function with this argument and shape {@code shape}.
		 * 
		 * @param shape the shape of the function ({@code null} for {@link #MEYOU})
		 * @return the key
		 */
		String key(Shape shape) {
			return String.valueOf(10 * code + (shape == null ? 0 : shape.code));
		}

		/**
		 * Get the title of the function named {@code fcn} with parameters named
		 * {@code param} for this argument and shape {@code shape}.
		 * 
		 * @param fcn   the name of the function
		 * @param param the name of the parameters
		 * @param shape the shape of the function ({@code null} for {@link #MEYOU})
		 * @return the title
		 */
		String title(char fcn, char param, Shape shape) {
			String head = fcn + "(x,y)=";
			if (shape == null)
				return head + param + "0*x+" + param + "1*y+" + param + "2*x*y";
			return head + shape.format(label, param);
		}
	}

	/**
	 * Functions translating the traits of a focal individual and its opponent in
	 * pairwise interactions into payoffs, see {@link Costs} and {@link Benefits}.
	 */
	interface PayoffFunction extends CLOption.Key {

		/**
		 * Get the argument of the function.
		 * 
		 * @return the argument
		 */
		Argument getArgument();

		/**
		 * Get the shape of the function.
		 * 
		 * @return the shape ({@code null} for {@link Argument#MEYOU})
		 */
		Shape getShape();

		/**
		 * Get the number of parameters of the function.
		 * 
		 * @return the number of parameters
		 */
		int getNParams();

		/**
		 * Evaluate the function with parameters {@code a} for the traits {@code me}
		 * and {@code you} of the focal individual and its opponent, respectively.
		 * 
		 * @param me  the trait of the focal individual
		 * @param you the trait of the opponent
		 * @param a   the parameters
		 * @return the function value
		 */
		default double eval(double me, double you, double[] a) {
			Shape shape = getShape();
			if (shape == null)
				return a[0] * me + a[1] * you + a[2] * me * you;
			return shape.eval(getArgument().select(me, you), a);
		}
	}

	/**
	 * A function of a single argument given by its {@link Shape} and parameters.
	 * Convenience class for modules that define their own payoffs, see
	 * {@link #parseTraitFunction(CLOption, String)}.
	 */
	public static class TraitFunction {

		/**
		 * The shape of the function.
		 */
		final Shape shape;

		/**
		 * The parameters of the function.
		 */
		final double[] params;

		/**
		 * Create a new function with shape {@code shape} and parameters
		 * {@code params}.
		 * 
		 * @param shape  the shape of the function
		 * @param params the parameters of the function
		 */
		public TraitFunction(Shape shape, double[] params) {
			this.shape = shape;
			this.params = params;
		}

		/**
		 * Evaluate the function at {@code z}.
		 * 
		 * @param z the argument
		 * @return the function value
		 */
		public double eval(double z) {
			return shape.eval(z, params);
		}

		@Override
		public String toString() {
			return shape.getKey() + " " + Formatter.format(params, 4);
		}
	}

	/**
	 * Cost functions to translate continuous traits into payoffs. Enum on
	 * steroids. The cost \(C(x,y)\) to a focal individual with trait \(x\) when
	 * interacting with an opponent with trait \(y\) is given by a function
	 * {@link Shape} applied to an {@link Argument}. The keys of the cost functions
	 * are two-digit codes, where the first digit specifies the argument and the
	 * second the shape:
	 * <dl>
	 * <dt>first digit (argument)
	 * <dd>{@code 0}: \(x\), {@code 1}: \(x+y\), {@code 2}: cross terms
	 * \(c_0 x+c_1 y+c_2 x y\), {@code 3}: \(y\)
	 * <dt>second digit (shape)
	 * <dd>{@code 0}: linear, {@code 1}: quadratic, {@code 2}: square root,
	 * {@code 3}: logarithmic, {@code 4}: exponential, {@code 5}: cubic,
	 * {@code 6}: quartic, {@code 7}: power
	 * </dl>
	 * Leading zeros are omitted, e.g. {@code 1} denotes \(C(x,y)=c_0 x+c_1 x^2\)
	 * and {@code 11} denotes \(C(x,y)=c_0 (x+y)+c_1 (x+y)^2\). The keys are the
	 * same as for {@link Benefits}.
	 */
	public enum Costs implements PayoffFunction {

		/**
		 * Linear cost function (independent of opponent): \(C(x,y)=c_0\,x\).
		 */
		ME_LINEAR(Argument.ME, Shape.LINEAR),

		/**
		 * Quadratic cost function (independent of opponent):
		 * \(C(x,y)=c_0\,x+c_1\,x^2\).
		 */
		ME_QUAD(Argument.ME, Shape.QUADRATIC),

		/**
		 * Square root cost function (independent of opponent): \(C(x,y)=c_0 \sqrt{x}\).
		 */
		ME_SQRT(Argument.ME, Shape.SQRT),

		/**
		 * Logarithmic cost function (independent of opponent): \(C(x,y)=c_0
		 * \ln(c_1\,x+1)\).
		 */
		ME_LOG(Argument.ME, Shape.LOG),

		/**
		 * Exponential cost function (independent of opponent): \(C(x,y)=c_0
		 * (1-\exp(-c_1\,x))\).
		 */
		ME_EXP(Argument.ME, Shape.EXP),

		/**
		 * Power cost function (independent of opponent): \(C(x,y)=c_0\,x^{c_1}\).
		 */
		ME_POWER(Argument.ME, Shape.POWER),

		/**
		 * Linear cost function (sum of focal, \(x\), and opponent, \(y\), traits):
		 * \(C(x,y)=c_0 (x+y)\).
		 */
		WE_LINEAR(Argument.WE, Shape.LINEAR),

		/**
		 * Quadratic cost function (sum of focal, \(x\), and opponent, \(y\), traits):
		 * \(C(x,y)=c_0 (x+y)+c_1 (x+y)^2\).
		 */
		WE_QUAD(Argument.WE, Shape.QUADRATIC),

		/**
		 * Cubic cost function (sum of focal, \(x\), and opponent, \(y\), traits):
		 * \(C(x,y)=c_0 (x+y)+c_1 (x+y)^2+c_2 (x+y)^3\).
		 */
		WE_CUBIC(Argument.WE, Shape.CUBIC),

		/**
		 * Quartic cost function (sum of focal, \(x\), and opponent, \(y\), traits):
		 * \(C(x,y)=c_0 (x+y)+c_1 (x+y)^2+c_2 (x+y)^3+c_3 (x+y)^4\).
		 */
		WE_QUARTIC(Argument.WE, Shape.QUARTIC),

		/**
		 * Power cost function (sum of focal, \(x\), and opponent, \(y\), traits):
		 * \(C(x,y)=c_0 (x+y)^{c_1}\).
		 */
		WE_POWER(Argument.WE, Shape.POWER),

		/**
		 * Linear cost function (cross terms of focal, \(x\), and opponent, \(y\),
		 * traits): \(C(x,y)=c_0\,x+c_1\,y+c_2\,x\,y\).
		 */
		MEYOU_LINEAR(Argument.MEYOU, null);

		/**
		 * The argument of the cost function.
		 */
		final Argument argument;

		/**
		 * The shape of the cost function ({@code null} for {@link Argument#MEYOU}).
		 */
		final Shape shape;

		/**
		 * The key of the cost function. Used when parsing command line options.
		 * 
		 * @see Continuous#cloCosts
		 */
		final String key;

		/**
		 * The brief description of the cost function for the help display.
		 * 
		 * @see EvoLudo#getCLOHelp()
		 */
		final String title;

		/**
		 * The number of parameters of the cost function.
		 */
		final int nParams;

		/**
		 * Create a new type of cost function with argument {@code argument} and shape
		 * {@code shape}.
		 * 
		 * @param argument the argument of the cost function
		 * @param shape    the shape of the cost function
		 */
		Costs(Argument argument, Shape shape) {
			this.argument = argument;
			this.shape = shape;
			key = argument.key(shape);
			title = argument.title('C', 'c', shape);
			nParams = (shape == null ? 3 : shape.nParams);
		}

		@Override
		public Argument getArgument() {
			return argument;
		}

		@Override
		public Shape getShape() {
			return shape;
		}

		@Override
		public int getNParams() {
			return nParams;
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

	/**
	 * Benefit functions to translate continuous traits into payoffs. Enum on
	 * steroids. The benefit \(B(x,y)\) to a focal individual with trait \(x\) when
	 * interacting with an opponent with trait \(y\) is given by a function
	 * {@link Shape} applied to an {@link Argument}. The keys of the benefit
	 * functions are the same as for {@link Costs}, e.g. {@code 31} denotes
	 * \(B(x,y)=b_0 y+b_1 y^2\) and {@code 11} denotes
	 * \(B(x,y)=b_0 (x+y)+b_1 (x+y)^2\).
	 */
	public enum Benefits implements PayoffFunction {

		/**
		 * Linear benefit function (independent of opponent): \(B(x,y)=b_0\,x\).
		 */
		ME_LINEAR(Argument.ME, Shape.LINEAR),

		/**
		 * Quadratic benefit function (independent of opponent):
		 * \(B(x,y)=b_0\,x+b_1\,x^2\).
		 */
		ME_QUAD(Argument.ME, Shape.QUADRATIC),

		/**
		 * Cubic benefit function (independent of opponent):
		 * \(B(x,y)=b_0\,x+b_1\,x^2+b_2\,x^3\).
		 */
		ME_CUBIC(Argument.ME, Shape.CUBIC),

		/**
		 * Power benefit function (independent of opponent): \(B(x,y)=b_0\,x^{b_1}\).
		 */
		ME_POWER(Argument.ME, Shape.POWER),

		/**
		 * Linear benefit function (sum of focal, \(x\), and opponent, \(y\), traits):
		 * \(B(x,y)=b_0\,(x+y)\).
		 */
		WE_LINEAR(Argument.WE, Shape.LINEAR),

		/**
		 * Quadratic benefit function (sum of focal, \(x\), and opponent, \(y\),
		 * traits): \(B(x,y)=b_0\,(x+y)+b_1\,(x+y)^2\).
		 */
		WE_QUAD(Argument.WE, Shape.QUADRATIC),

		/**
		 * Saturating benefit function following a square root (sum of focal, \(x\), and
		 * opponent, \(y\), traits): \(B(x,y)=b_0\sqrt{x+y}\).
		 */
		WE_SQRT(Argument.WE, Shape.SQRT),

		/**
		 * Saturating benefit function following a logarithm (sum of focal, \(x\), and
		 * opponent, \(y\), traits): \(B(x,y)=b_0\ln(b_1\,(x+y)+1)\).
		 */
		WE_LOG(Argument.WE, Shape.LOG),

		/**
		 * Saturating benefit function following an exponential (sum of focal, \(x\),
		 * and opponent, \(y\), traits): \(B(x,y)=b_0 \left(1-e^{-b_1\,(x+y)}\right)\).
		 */
		WE_EXP(Argument.WE, Shape.EXP),

		/**
		 * Power benefit function (sum of focal, \(x\), and opponent, \(y\), traits):
		 * \(B(x,y)=b_0\,(x+y)^{b_1}\).
		 */
		WE_POWER(Argument.WE, Shape.POWER),

		/**
		 * Linear benefit function (with interaction term):
		 * \(B(x,y)=b_0\,x+b_1\,y+b_2\,x\,y\).
		 */
		MEYOU_LINEAR(Argument.MEYOU, null),

		/**
		 * Linear benefit function (independent of focal): \(B(x,y)=b_0\,y\).
		 */
		YOU_LINEAR(Argument.YOU, Shape.LINEAR),

		/**
		 * Quadratic benefit function (independent of focal):
		 * \(B(x,y)=b_0\,y+b_1\,y^2\).
		 */
		YOU_QUAD(Argument.YOU, Shape.QUADRATIC),

		/**
		 * Saturating benefit function following a square root (independent of focal):
		 * \(B(x,y)=b_0\sqrt{y}\).
		 */
		YOU_SQRT(Argument.YOU, Shape.SQRT),

		/**
		 * Saturating benefit function following a logarithm (independent of focal):
		 * \(B(x,y)=b_0\ln(b_1\,y+1)\).
		 */
		YOU_LOG(Argument.YOU, Shape.LOG),

		/**
		 * Saturating benefit function following an exponential (independent of focal):
		 * \(B(x,y)=b_0 \left(1-e^{-b_1\,y}\right)\).
		 */
		YOU_EXP(Argument.YOU, Shape.EXP),

		/**
		 * Power benefit function (independent of focal): \(B(x,y)=b_0\,y^{b_1}\).
		 */
		YOU_POWER(Argument.YOU, Shape.POWER);

		/**
		 * The argument of the benefit function.
		 */
		final Argument argument;

		/**
		 * The shape of the benefit function ({@code null} for
		 * {@link Argument#MEYOU}).
		 */
		final Shape shape;

		/**
		 * The key of the benefit function. Used when parsing command line options.
		 * 
		 * @see Continuous#cloBenefits
		 */
		final String key;

		/**
		 * The brief description of the benefit function for the help display.
		 * 
		 * @see EvoLudo#getCLOHelp()
		 */
		final String title;

		/**
		 * The number of parameters of the benefit function.
		 */
		final int nParams;

		/**
		 * Create a new type of benefit function with argument {@code argument} and
		 * shape {@code shape}.
		 * 
		 * @param argument the argument of the benefit function
		 * @param shape    the shape of the benefit function
		 */
		Benefits(Argument argument, Shape shape) {
			this.argument = argument;
			this.shape = shape;
			key = argument.key(shape);
			title = argument.title('B', 'b', shape);
			nParams = (shape == null ? 3 : shape.nParams);
		}

		@Override
		public Argument getArgument() {
			return argument;
		}

		@Override
		public Shape getShape() {
			return shape;
		}

		@Override
		public int getNParams() {
			return nParams;
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

	/**
	 * Command line option to set the minimum value of each trait.
	 */
	public final CLOption cloTraitRange = new CLOption("traitrange", "0,1", CLOCategory.Module, null,
			new CLODelegate() {

				/**
				 * {@inheritDoc}
				 * <p>
				 * Parse the minimum value of each trait. {@code arg} can be a single value or
				 * an array with the separator {@value CLOParser#MATRIX_DELIMITER}. The parser
				 * cycles through {@code arg} until the minimum value of each trait is set.
				 * 
				 * @param arg the (array of) of minimum trait values
				 */
				@Override
				public boolean parse(String arg) {
					// getting ready for multiple species - way ahead of its time...
					String[] speciestraits = arg.split(CLOParser.SPECIES_DELIMITER);
					if (speciestraits == null)
						return false;
					int n = 0;
					for (Continuous cpop : species) {
						String[] traitranges = speciestraits[n++ % speciestraits.length]
								.split(CLOParser.TRAIT_DELIMITER);
						for (int i = 0; i < nTraits; i++) {
							String trange = traitranges[i % traitranges.length];
							double[] range = CLOParser.parseVector(trange);
							if (range.length < 2 || range[0] > range[1])
								return false;
							cpop.setTraitRange(range[0], range[1], i);
						}
					}
					return true;
				}

				@Override
				public String getDescription() {
					switch (nTraits) {
						case 1:
							return "--traitrange <min" + CLOParser.VECTOR_DELIMITER + "max>  range of trait "
									+ getTraitName(0);
						case 2:
							return "--traitrange <min0" + CLOParser.VECTOR_DELIMITER + "max0" + //
									CLOParser.TRAIT_DELIMITER + "min1" + CLOParser.VECTOR_DELIMITER + "max1]>" + //
									CLOParser.VECTOR_DELIMITER + "  range of traits, with\n"
									+ "             0: " + getTraitName(0) + "\n" //
									+ "             1: " + getTraitName(1);
						default:
							StringBuilder descr = new StringBuilder();
							descr.append("--traitrange <min0").append(CLOParser.VECTOR_DELIMITER).append("max0[")
									.append(CLOParser.TRAIT_DELIMITER).append("...")
									.append(CLOParser.TRAIT_DELIMITER).append("min").append(nTraits - 1)
									.append(CLOParser.VECTOR_DELIMITER).append("max").append(nTraits - 1)
									.append("]>  range of traits, with");
							for (int n = 0; n < nTraits; n++) {
								String aTrait = "              " + n + ": ";
								int traitlen = aTrait.length();
								descr.append("\n").append(aTrait.substring(traitlen - 16, traitlen))
										.append(getTraitName(n));
							}
							return descr.toString();
					}
				}
			});

	/**
	 * Command line option to set the maximum combined trait value.
	 *
	 * @see #maxInvest
	 */
	public final CLOption cloMaxInvest = new CLOption("maxinvest", "none", CLOCategory.Module,
			"--maxinvest <T>  maximum combined trait value, sum of traits ≤ T",
			new CLODelegate() {
				@Override
				public boolean parse(String arg) {
					String max = arg.trim();
					if (max.equalsIgnoreCase("none")) {
						setMaxInvest(Double.POSITIVE_INFINITY);
						return true;
					}
					try {
						setMaxInvest(CLOParser.parseDouble(max));
					} catch (NumberFormatException nfe) {
						return false;
					}
					return true;
				}
			});

	/**
	 * Parse the parameters {@code params} of the function {@code type} with
	 * {@code nParams} parameters for the command line option {@code clo}.
	 * 
	 * @param clo     the command line option
	 * @param type    the function type
	 * @param nParams the number of parameters of {@code type}
	 * @param params  the parameters separated by
	 *                {@value CLOParser#VECTOR_DELIMITER}
	 * @return the parameters or {@code null} if there are too few
	 */
	protected double[] parseParams(CLOption clo, CLOption.Key type, int nParams, String params) {
		double[] args = CLOParser.parseVector(params);
		if (args.length != nParams) {
			engine.logWarning(clo.getName() + ": function '" + type.getKey() + "' requires " + nParams
					+ " parameters but found '" + params + "'.");
			if (args.length < nParams)
				return null;
		}
		return args;
	}

	/**
	 * Parse the function specification {@code arg} for the command line option
	 * {@code clo}, whose keys must be {@link Shape}s. The specification consists of
	 * the key of the shape followed by its parameters, e.g.
	 * {@code "quadratic 1,0.5"}.
	 * 
	 * @param clo the command line option
	 * @param arg the function specification
	 * @return the function or {@code null} if {@code arg} is invalid
	 */
	protected TraitFunction parseTraitFunction(CLOption clo, String arg) {
		String[] args = arg.trim().split(CLOParser.SPLIT_ARG_REGEX);
		Shape shape = (Shape) clo.match(args[0]);
		if (shape == null || args.length < 2) {
			engine.logWarning(clo.getName() + ": invalid function '" + arg + "'.");
			return null;
		}
		double[] params = parseParams(clo, shape, shape.nParams, args[1]);
		return (params == null ? null : new TraitFunction(shape, params));
	}

	/**
	 * Parse the cost or benefit functions {@code arg} for each trait for the
	 * command line option {@code clo}. The functions for different traits are
	 * separated by {@value CLOParser#TRAIT_DELIMITER}. Each function consists of
	 * its key followed by its parameters. If the key is omitted, the function of
	 * the previous trait is used. The parser cycles through {@code arg} until the
	 * functions for all traits are set.
	 * 
	 * @param clo   the command line option
	 * @param arg   the (array of) function specifications
	 * @param costs {@code true} to set cost functions and {@code false} for
	 *              benefit functions
	 * @return {@code true} if parsing succeeded
	 */
	private boolean parsePayoffFunctions(CLOption clo, String arg, boolean costs) {
		String[] fcns = arg.split(CLOParser.TRAIT_DELIMITER);
		PayoffFunction prevtype = null;
		for (int n = 0; n < nTraits; n++) {
			String fcn = fcns[n % fcns.length];
			PayoffFunction type = (PayoffFunction) clo.match(fcn);
			String[] fcnargs = fcn.split(CLOParser.SPLIT_ARG_REGEX);
			String params;
			if (type == null) {
				if (prevtype == null)
					return false;
				type = prevtype;
				params = fcnargs[0];
			} else if (fcnargs.length < 2) {
				engine.logWarning(clo.getName() + ": function '" + type.getKey() + "' requires "
						+ type.getNParams() + " parameters!");
				return false;
			} else
				params = fcnargs[1];
			double[] args = parseParams(clo, type, type.getNParams(), params);
			if (args == null)
				return false;
			prevtype = type;
			if (costs)
				traits2payoff.setCostFunction((Costs) type, args, n);
			else
				traits2payoff.setBenefitFunction((Benefits) type, args, n);
		}
		return true;
	}

	/**
	 * Command line option to set the cost function(s) for continuous traits.
	 * 
	 * @see Costs
	 */
	public final CLOption cloCosts = new CLOption("costs", Costs.ME_LINEAR.getKey() + " 1", CLOCategory.Module,
			"--costs <s0 c00[" + CLOParser.VECTOR_DELIMITER + "c01...[" + CLOParser.TRAIT_DELIMITER + //
					"s1 c10[" + CLOParser.VECTOR_DELIMITER + "c11...]]]>  cost function <si>:",
			new CLODelegate() {
				@Override
				public boolean parse(String arg) {
					return parsePayoffFunctions(cloCosts, arg, true);
				}
			});

	/**
	 * Command line option to set the benefit function(s) for continuous traits.
	 * 
	 * @see Benefits
	 */
	public final CLOption cloBenefits = new CLOption("benefits", Benefits.WE_LINEAR.getKey() + " 3",
			CLOCategory.Module,
			"--benefits <s0 b00[" + CLOParser.VECTOR_DELIMITER + "b01...[" + CLOParser.TRAIT_DELIMITER + //
					"s1 b10[" + CLOParser.VECTOR_DELIMITER + "b11...]]]>  benefit function <si>:",
			new CLODelegate() {
				@Override
				public boolean parse(String arg) {
					return parsePayoffFunctions(cloBenefits, arg, false);
				}
			});

	@Override
	public void collectCLO(CLOParser parser) {
		if (this instanceof Features.Multispecies)
			return;
		super.collectCLO(parser);
		parser.addCLO(cloTraitRange);
		if (nTraits > 1)
			parser.addCLO(cloMaxInvest);
		if (this instanceof Payoffs) {
			cloCosts.addKeys(Costs.values());
			parser.addCLO(cloCosts);
			cloBenefits.addKeys(Benefits.values());
			parser.addCLO(cloBenefits);
			// best-response is not an acceptable update rule for continuous traits -
			// exclude Population.PLAYER_UPDATE_BEST_RESPONSE
			playerUpdate.clo.removeKey(PlayerUpdate.Type.BEST_RESPONSE);
		}
		// TODO: implement enabling/disabling traits as in discrete case
		// // add option to disable traits if >=2 traits
		// if (nTraits > 1)
		// parser.addCLO(cloTraitDisable);
		if (model instanceof CModel && nTraits > 1 && (this instanceof HasPop2D || this instanceof HasPop3D)) {
			cloTraitColorScheme.addKeys(ColorModelType.values());
			parser.addCLO(cloTraitColorScheme);
		}
		parser.addCLO(mutation.clo);
	}

	/**
	 * The absolute minimum score.
	 */
	protected double cxMinScore = Double.MAX_VALUE;

	/**
	 * The absolute maximum score.
	 */
	protected double cxMaxScore = -Double.MAX_VALUE;

	/**
	 * The minimum score in a monomorphic population.
	 */
	protected double cxMinMonoScore = Double.MAX_VALUE;

	/**
	 * The maximum score in a monomorphic population.
	 */
	protected double cxMaxMonoScore = -Double.MAX_VALUE;

	/**
	 * Helper method to numerically determine the minimum and maximum scores in the
	 * game through a brute force hill climbing algorithm for two competing traits
	 * as well as monomorphic populations.
	 */
	protected void setExtremalScores() {
		cxMinScore = findExtremalScore(false);
		cxMaxScore = findExtremalScore(true);
		cxMinMonoScore = findExtremalMonoScore(false);
		cxMaxMonoScore = findExtremalMonoScore(true);
		extremalScoresSet = true;
	}

	/**
	 * The linear grid size to sample payoffs in the (possibly multi-dimensional)
	 * trait space.
	 */
	static final int MINMAX_STEPS = 10;

	/**
	 * The number of iterations for the hill climbing process.
	 */
	static final int MINMAX_ITER = 5;

	/**
	 * Helper method to determine the minimum or maximum payoff.
	 * 
	 * <h3>Implementation notes:</h3>
	 * Repeatedly calls
	 * {@link #findExtrema(double[], double[], int[], int[], double[][], double[][], double[], double[], int[], int[], double, int, double)}
	 * with the most promising interval in each trait for residents and mutants,
	 * respectively. The hill climbing process stops after {@code #MINMAX_ITER}
	 * iterations.
	 * 
	 * @param maximum if {@code true} the maximum is returned and the minimum
	 *                otherwise
	 * @return the minimum or maximum payoff
	 */
	private double findExtremalScore(boolean maximum) {
		double[][] resInterval = new double[nTraits][2];
		double[][] mutInterval = new double[nTraits][2];
		double[] resScale = new double[nTraits];
		double[] mutScale = new double[nTraits];
		double[] resTrait = new double[nTraits];
		double[] mutTrait = new double[nTraits];
		int[] resIdx = new int[nTraits];
		int[] mutIdx = new int[nTraits];
		int[] resMax = new int[nTraits];
		int[] mutMax = new int[nTraits];
		double minmax = maximum ? 1.0 : -1.0;
		double scoreMax = -Double.MAX_VALUE;

		// initialize trait intervals
		for (int n = 0; n < nTraits; n++) {
			resInterval[n][0] = 0;
			resInterval[n][1] = 1;
			mutInterval[n][0] = 0;
			mutInterval[n][1] = 1;
		}

		for (int i = 0; i < MINMAX_ITER; i++) {
			for (int n = 0; n < nTraits; n++) {
				resScale[n] = (resInterval[n][1] - resInterval[n][0]) / MINMAX_STEPS;
				mutScale[n] = (mutInterval[n][1] - mutInterval[n][0]) / MINMAX_STEPS;
			}
			Arrays.fill(resMax, -1);
			Arrays.fill(mutMax, -1);
			scoreMax = Math.max(scoreMax, findExtrema(resTrait, mutTrait, resIdx, mutIdx, resInterval, mutInterval,
					resScale, mutScale, resMax, mutMax, -Double.MAX_VALUE, nTraits - 1, minmax));
			// determine new intervals and scales
			for (int n = 0; n < nTraits; n++) {
				switch (resIdx[n]) {
					case 0:
						resInterval[n][1] = resInterval[n][0] + resScale[n];
						break;
					case MINMAX_STEPS:
						resInterval[n][0] += (MINMAX_STEPS - 1) * resScale[n];
						break;
					default:
						resInterval[n][0] += (resIdx[n] - 1) * resScale[n];
						resInterval[n][1] = resInterval[n][0] + 2.0 * resScale[n];
						break;
				}
				switch (mutIdx[n]) {
					case 0:
						mutInterval[n][1] = mutInterval[n][0] + mutScale[n];
						break;
					case MINMAX_STEPS:
						mutInterval[n][0] += (MINMAX_STEPS - 1) * mutScale[n];
						break;
					default:
						mutInterval[n][0] += (mutIdx[n] - 1) * mutScale[n];
						mutInterval[n][1] = mutInterval[n][0] + 2.0 * mutScale[n];
						break;
				}
			}
		}
		return minmax * scoreMax;
	}

	/**
	 * Helper method to find the minimum or maximum payoff.
	 * 
	 * <h3>Implementation notes:</h3>
	 * The function returns the maximum payoff after discretizing each trait of both
	 * the resident and the mutant into {@code MINMAX_STEPS} intervals. The function
	 * is recursively called for each trait. The indices of the cell (discretized
	 * trait interval) that yields the maximum payoff are returned in the arrays
	 * {@code resMax} and {@code mutMax} for residents and mutants respectively.
	 * This provides the starting point for the next iteration of the hill climber
	 * process.
	 * 
	 * @param resTrait    the array of resident traits (helper variable for
	 *                    recursive calculations)
	 * @param mutTrait    the array of mutant traits (helper variable for recursive
	 *                    calculations)
	 * @param resIdx      the index of the resident trait (helper variable for
	 *                    recursive calculations)
	 * @param mutIdx      the index of the mutant trait (helper variable for
	 *                    recursive calculations)
	 * @param resInterval the resident trait intervals for discretization
	 * @param mutInterval the mutant trait intervals for discretization
	 * @param resScale    the scaling of the width of the resident trait interval
	 * @param mutScale    the scaling of the width of the mutant trait interval
	 * @param resMax      the indices of the discretized cell for the resident that
	 *                    yielded the highest payoff
	 * @param mutMax      the indices of the discretized cell for the mutant that
	 *                    yielded the highest payoff
	 * @param scoreMax    the maximum payoff
	 * @param trait       the current trait for the recursion (helper variable for
	 *                    recursive calculations)
	 * @param minmax      {@code 1.0} to calculate maximum and {@code -1.0} to
	 *                    calculate minimum
	 * @return the minimum or maximum score
	 */
	private double findExtrema(double[] resTrait, double[] mutTrait, int[] resIdx, int[] mutIdx,
			double[][] resInterval,
			double[][] mutInterval, double[] resScale, double[] mutScale, int[] resMax, int[] mutMax, double scoreMax,
			int trait, double minmax) {

		for (int r = 0; r <= MINMAX_STEPS; r++) {
			resIdx[trait] = r;
			resTrait[trait] = resInterval[trait][0] + r * resScale[trait];
			for (int m = 0; m <= MINMAX_STEPS; m++) {
				mutIdx[trait] = m;
				mutTrait[trait] = mutInterval[trait][0] + m * mutScale[trait];
				if (trait > 0) {
					scoreMax = Math.max(scoreMax, findExtrema(resTrait, mutTrait, resIdx, mutIdx, resInterval,
							mutInterval, resScale, mutScale, resMax, mutMax, scoreMax, trait - 1, minmax));
					continue;
				}
				double traitScore = minmax * traits2payoff.payoff(resTrait, mutTrait);
				if (traitScore > scoreMax) {
					scoreMax = traitScore;
					int len = resIdx.length; // nTraits
					System.arraycopy(resIdx, 0, resMax, 0, len);
					System.arraycopy(mutIdx, 0, mutMax, 0, len);
				}
			}
		}
		return scoreMax;
	}

	/**
	 * Helper method to find the minimum or maximum payoff for <em>monomorphic</em>
	 * populations.
	 * 
	 * <h3>Implementation notes:</h3>
	 * This is essentially the same but simplified hill climber process in
	 * {@link #findExtremalScore(boolean)} under the constraint that the population
	 * is monomorphic, i.e. that the resident and mutant traits are identical.
	 * 
	 * @param maximum if {@code true} the maximum is returned and the minimum
	 *                otherwise
	 * @return the minimum or maximum monomorphic score
	 */
	private double findExtremalMonoScore(boolean maximum) {
		double[][] resInterval = new double[nTraits][2];
		double[] resScale = new double[nTraits];
		double[] resTrait = new double[nTraits];
		int[] resIdx = new int[nTraits];
		int[] resMax = new int[nTraits];
		double minmax = maximum ? 1.0 : -1.0;
		double scoreMax = -Double.MAX_VALUE;

		// initialize trait intervals
		for (int n = 0; n < nTraits; n++) {
			resInterval[n][0] = 0;
			resInterval[n][1] = 1;
		}

		for (int i = 0; i < MINMAX_ITER; i++) {
			for (int n = 0; n < nTraits; n++) {
				resScale[n] = (resInterval[n][1] - resInterval[n][0]) / MINMAX_STEPS;
			}
			Arrays.fill(resMax, -1);
			scoreMax = Math.max(scoreMax, findExtrema(resTrait, resIdx, resInterval, resScale, resMax,
					-Double.MAX_VALUE, nTraits - 1, minmax));
			// determine new intervals and scales
			for (int n = 0; n < nTraits; n++) {
				switch (resIdx[n]) {
					case 0:
						resInterval[n][1] = resInterval[n][0] + resScale[n];
						break;
					case MINMAX_STEPS:
						resInterval[n][0] += (MINMAX_STEPS - 1) * resScale[n];
						break;
					default:
						resInterval[n][0] += (resIdx[n] - 1) * resScale[n];
						resInterval[n][1] = resInterval[n][0] + 2.0 * resScale[n];
						break;
				}
			}
		}
		return minmax * scoreMax;
	}

	/**
	 * Helper method to find the minimum or maximum payoff for <em>monomorphic</em>
	 * populations.
	 * 
	 * <h3>Implementation notes:</h3>
	 * This is essentially the same but simplified hill climber process in
	 * {@link #findExtrema(double[], double[], int[], int[], double[][], double[][], double[], double[], int[], int[], double, int, double)}
	 * under the constraint that the population is monomorphic, i.e. that the
	 * resident and mutant traits are identical.
	 * 
	 * @param resTrait    the array of resident traits (helper variable for
	 *                    recursive calculations)
	 * @param resIdx      the index of the resident trait (helper variable for
	 *                    recursive calculations)
	 * @param resInterval the resident trait intervals for discretization
	 * @param resScale    the scaling of the width of the resident trait interval
	 * @param resMax      the indices of the discretized cell for the resident that
	 *                    yielded the highest payoff
	 * @param scoreMax    the maximum payoff
	 * @param trait       the current trait for the recursion (helper variable for
	 *                    recursive calculations)
	 * @param minmax      {@code 1.0} to calculate maximum and {@code -1.0} to
	 *                    calculate minimum
	 * @return the minimum or maximum monomorphic score
	 */
	private double findExtrema(double[] resTrait, int[] resIdx, double[][] resInterval, double[] resScale, int[] resMax,
			double scoreMax, int trait, double minmax) {

		for (int r = 0; r <= MINMAX_STEPS; r++) {
			resIdx[trait] = r;
			resTrait[trait] = resInterval[trait][0] + r * resScale[trait];
			if (trait > 0) {
				scoreMax = Math.max(scoreMax,
						findExtrema(resTrait, resIdx, resInterval, resScale, resMax, scoreMax, trait - 1, minmax));
				continue;
			}
			double traitScore = minmax * traits2payoff.payoff(resTrait, resTrait);
			if (traitScore > scoreMax) {
				scoreMax = traitScore;
				int len = resIdx.length; // nTraits
				System.arraycopy(resIdx, 0, resMax, 0, len);
			}
		}
		return scoreMax;
	}
}
