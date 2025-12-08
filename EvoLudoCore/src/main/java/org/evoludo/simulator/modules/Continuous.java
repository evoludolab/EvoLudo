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
import java.util.Arrays;
import java.util.Collections;
import java.util.logging.Level;

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
				if (logger.isLoggable(Level.WARNING))
					logger.warning("invalid trait range [" + Formatter.format(traitMin[s], 4) + ", "
							+ Formatter.format(traitMax[s], 4) + "] for trait " + s + " - reset to [0, 1]!");
				setTraitRange(0.0, 1.0, s);
				doReset = true;
			}
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
	 * Translate continuous traits into payoffs based on configurable cost and
	 * benefit functions.
	 */
	public class Traits2Payoff {

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
			double myinv = me * scale + shift;
			double yourinv = you * scale + shift;
			double ourinv = myinv + yourinv;
			double[] c = ci[trait];

			switch (costs[trait]) {
				case ME_LINEAR:
					return c[0] * myinv;
				case ME_QUAD: // default
					return myinv * (c[1] * myinv + c[0]);
				case ME_SQRT:
					return c[0] * Math.sqrt(myinv);
				case ME_LOG:
					return c[0] * Math.log(c[1] * myinv + 1.0);
				case ME_EXP:
					return c[0] * (1.0 - Math.exp(-c[1] * myinv));

				case WE_LINEAR:
					return c[0] * ourinv;
				case WE_QUAD:
					return (c[1] * ourinv + c[0]) * ourinv;
				case WE_QUBIC:
					return ((c[2] * ourinv + c[1]) * ourinv + c[0]) * ourinv;
				case WE_QUARTIC:
					return (((c[3] * ourinv + c[2]) * ourinv + c[1]) * ourinv + c[0]) * ourinv;

				case MEYOU_LINEAR:
					return c[0] * myinv + c[1] * yourinv + c[2] * myinv * yourinv;

				default: // this is bad
					throw new UnsupportedOperationException("Unknown cost function type (" + costs[trait] + ")");
			}
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
			double myinv = me * scale + shift;
			double yourinv = you * scale + shift;
			double ourinv = myinv + yourinv;
			double[] b = bi[trait];

			switch (benefits[trait]) {
				// benefit depending solely on the 'me' investment
				case ME_LINEAR:
					return b[0] * myinv;
				case ME_QUAD:
					return (b[1] * myinv + b[0]) * myinv;
				case ME_QUBIC:
					return ((b[2] * myinv + b[1]) * myinv + b[0]) * myinv;

				// benefit depending solely on the 'you' investment
				case YOU_LINEAR:
					return b[0] * yourinv;
				case YOU_QUAD:
					return (b[1] * yourinv + b[0]) * yourinv;
				case YOU_SQRT:
					return b[0] * Math.sqrt(yourinv);
				case YOU_LOG:
					return b[0] * Math.log(b[1] * yourinv + 1.0);
				case YOU_EXP:
					return b[0] * (1.0 - Math.exp(-b[1] * yourinv));

				// benefit depending on the sum of 'me' and 'you' investments
				case WE_LINEAR: // was 2
					return b[0] * ourinv;
				case WE_QUAD: // default
					return (b[1] * ourinv + b[0]) * ourinv;
				case WE_SQRT:
					return b[0] * Math.sqrt(ourinv);
				case WE_LOG:
					return b[0] * Math.log(b[1] * ourinv + 1.0);
				case WE_EXP:
					return b[0] * (1.0 - Math.exp(-b[1] * ourinv));

				// benefit depending on 'me' and 'you' investments individually
				case MEYOU_LINEAR:
					return b[0] * myinv + b[1] * yourinv + b[2] * myinv * yourinv;

				default: // this is bad
					throw new UnsupportedOperationException("Unknown benefit function type (" + benefits[trait] + ")");
			}
		}
	}

	/**
	 * Selected cost functions to translate continuous traits into payoffs. Enum on
	 * steroids. Currently available cost functions are:
	 * <dl>
	 * <dt>0
	 * <dd>Linear cost function (independent of opponent): \(C(x,y)=c_0\,x\).
	 * <dt>1
	 * <dd>Quadratic cost function (independent of opponent):
	 * \(C(x,y)=c_0\,x+c_1\,x^2\).
	 * <dt>2
	 * <dd>Square root cost function (independent of opponent): \(C(x,y)=c_0
	 * \sqrt{x}\).
	 * <dt>3
	 * <dd>Logarithmic cost function (independent of opponent): \(C(x,y)=c_0
	 * \ln(c_1\,x+1)\).
	 * <dt>4
	 * <dd>Exponential cost function (independent of opponent): \(C(x,y)=c_0
	 * (1-\exp(-c_1\,x))\).
	 * <dt>10
	 * <dd>Linear cost function (sum of focal, \(x\), and opponent, \(y\), traits):
	 * \(C(x,y)=c_0 (x+y)\).
	 * <dt>11
	 * <dd>Quadratic cost function (sum of focal, \(x\), and opponent, \(y\),
	 * traits): \(C(x,y)=c_0 (x+y)+c_1 (x+y)^2\).
	 * <dt>12
	 * <dd>Cubic cost function (sum of focal, \(x\), and opponent, \(y\), traits):
	 * \(C(x,y)=c_0 (x+y)+c_1 (x+y)^2+c_2 (x+y)^3\).
	 * <dt>13
	 * <dd>Quartic cost function (sum of focal, \(x\), and opponent, \(y\), traits):
	 * \(C(x,y)=c_0 (x+y)+c_1 (x+y)^2+c_2 (x+y)^3+c_3 (x+y)^4\).
	 * <dt>20
	 * <dd>Linear cost function (cross terms of focal, \(x\), and opponent, \(y\),
	 * traits): \(C(x,y)=c_0\,x+c_1\,y+c_2\,x\,y\).
	 * </dl>
	 */
	public enum Costs implements CLOption.Key {

		/**
		 * Linear cost function (independent of opponent): \(C(x,y)=c_0\,x\).
		 */
		ME_LINEAR("0", "C(x,y)=c0*x", 1), //

		/**
		 * Quadratic cost function (independent of opponent):
		 * \(C(x,y)=c_0\,x+c_1\,x^2\).
		 */
		ME_QUAD("1", "C(x,y)=c0*x+c1*x^2", 2), //

		/**
		 * Square root cost function (independent of opponent): \(C(x,y)=c_0 \sqrt{x}\).
		 */
		ME_SQRT("2", "C(x,y)=c0*sqrt(x)", 1), //

		/**
		 * Logarithmic cost function (independent of opponent): \(C(x,y)=c_0
		 * \ln(c_1\,x+1)\).
		 */
		ME_LOG("3", "C(x,y)=c0*ln(c1*x+1)", 2), //

		/**
		 * Exponential cost function (independent of opponent): \(C(x,y)=c_0
		 * (1-\exp(-c_1\,x))\).
		 */
		ME_EXP("4", "C(x,y)=c0*(1-exp(-c1*x))", 2), //

		/**
		 * Linear cost function (sum of focal, \(x\), and opponent, \(y\), traits):
		 * \(C(x,y)=c_0 (x+y)\).
		 */
		WE_LINEAR("10", "C(x,y)=c0*(x+y)", 1), //

		/**
		 * Quadratic cost function (sum of focal, \(x\), and opponent, \(y\), traits):
		 * \(C(x,y)=c_0 (x+y)+c_1 (x+y)^2\).
		 */
		WE_QUAD("11", "C(x,y)=c0*(x+y)+c1*(x+y)^2", 2), //

		/**
		 * Cubic cost function (sum of focal, \(x\), and opponent, \(y\), traits):
		 * \(C(x,y)=c_0 (x+y)+c_1 (x+y)^2+c_2 (x+y)^3\).
		 */
		WE_QUBIC("12", "C(x,y)=c0*(x+y)+c1*(x+y)^2+c2*(x+y)^3", 3), //

		/**
		 * Quartic cost function (sum of focal, \(x\), and opponent, \(y\), traits):
		 * \(C(x,y)=c_0 (x+y)+c_1 (x+y)^2+c_2 (x+y)^3+c_3 (x+y)^4\).
		 */
		WE_QUARTIC("13", "C(x,y)=c0*(x+y)+c1*(x+y)^2+c2*(x+y)^3+c3*(x+y)^4", 4), //

		/**
		 * Linear cost function (cross terms of focal, \(x\), and opponent, \(y\),
		 * traits): \(C(x,y)=c_0\,x+c_1\,y+c_2\,x\,y\).
		 */
		MEYOU_LINEAR("20", "C(x,y)=c0*x+c1*y+c2*x*y", 3);

		/**
		 * The key of the cost function. Used when parsing command line options.
		 * 
		 * @see Continuous#cloCosts
		 */
		String key;

		/**
		 * The brief description of the cost function for the help display.
		 * 
		 * @see EvoLudo#getCLOHelp()
		 */
		String title;

		/**
		 * The number of parameters of the cost function.
		 */
		int nParams;

		/**
		 * Create a new type of cost function with key {@code key} and description
		 * {@code title} as well as {@code nParams} parameters.
		 * 
		 * @param key     the identifier for parsing of command line option
		 * @param title   the summary of the cost function
		 * @param nParams the number of parameters
		 */
		Costs(String key, String title, int nParams) {
			this.key = key;
			this.title = title;
			this.nParams = nParams;
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
	 * Selected benefit functions to translate continuous traits into payoffs. Enum
	 * on steroids. Currently available benefit functions are:
	 * <dl>
	 * <dt>0
	 * <dd>Linear benefit function (independent of focal): \(B(x,y)=b_0\,y\).
	 * <dt>1
	 * <dd>Quadratic benefit function (independent of focal):
	 * \(B(x,y)=b_0\,y+\b_1\,y^2\).
	 * <dt>2
	 * <dd>Saturating benefit function following a square root (independent of
	 * focal): \(B(x,y)=b_0\sqrt{y}\).
	 * <dt>3
	 * <dd>Saturating benefit function following a logarithm (independent of focal):
	 * \(B(x,y)=b_0\log{b_1\,y+1}\).
	 * <dt>4
	 * <dd>Saturating benefit function following an exponential (independent of
	 * focal): \(B(x,y)=b_0 \left(1-e^{-b_1\,y}\right)\).
	 * <dt>10
	 * <dd>Linear benefit function (sum of focal, \(x\), and opponent, \(y\),
	 * traits): \(B(x,y)=b_0\,(x+y)\).
	 * <dt>11
	 * <dd>Quadratic benefit function (sum of focal, \(x\), and opponent, \(y\),
	 * traits): \(B(x,y)=b_0\,(x+y)+\b_1\,(x+y)^2\).
	 * <dt>12
	 * <dd>Saturating benefit function following a square root (sum of focal, \(x\),
	 * and opponent, \(y\), traits): \(B(x,y)=b_0\sqrt{x+y}\).
	 * <dt>13
	 * <dd>Saturating benefit function following a logarithm (sum of focal, \(x\),
	 * and opponent, \(y\), traits): \(B(x,y)=b_0\log{b_1\,(x+y)+1}\).
	 * <dt>14
	 * <dd>Saturating benefit function following an exponential (sum of focal,
	 * \(x\), and opponent, \(y\), traits): \(B(x,y)=b_0
	 * \left(1-e^{-b_1\,(x+y)}\right)\).
	 * <dt>20
	 * <dd>Linear benefit function (with interaction term):
	 * \(B(x,y)=b_0\,x=b_1\,y+\b_2\,x\,y\).
	 * <dt>30
	 * <dd>Linear benefit function (independent of opponent): \(B(x,y)=b_0\,x\).
	 * <dt>31
	 * <dd>Quadratic benefit function (independent of opponent):
	 * \(B(x,y)=b_0\,x+b_1\,x^2\).
	 * <dt>32
	 * <dd>Cubic benefit function (independent of opponent):
	 * \(B(x,y)=b_0\,x+b_1\,x^2+b_2\,x^3\).
	 * </dl>
	 */
	public enum Benefits implements CLOption.Key {

		/**
		 * Linear benefit function (independent of focal): \(B(x,y)=b_0\,y\).
		 */
		YOU_LINEAR("0", "B(x,y)=b0*y", 1), //

		/**
		 * Quadratic benefit function (independent of focal):
		 * \(B(x,y)=b_0\,y+\b_1\,y^2\).
		 */
		YOU_QUAD("1", "B(x,y)=b0*y+b1*y^2", 2), //

		/**
		 * Saturating benefit function following a square root (independent of focal):
		 * \(B(x,y)=b_0\sqrt{y}\).
		 */
		YOU_SQRT("2", "B(x,y)=b0*sqrt(y)", 1), //

		/**
		 * Saturating benefit function following a logarithm (independent of focal):
		 * \(B(x,y)=b_0\log{b_1\,y+1}\).
		 */
		YOU_LOG("3", "B(x,y)=b0*ln(b1*y+1)", 2), //

		/**
		 * Saturating benefit function following an exponential (independent of focal):
		 * \(B(x,y)=b_0 \left(1-e^{-b_1\,y}\right)\).
		 */
		YOU_EXP("4", "B(x,y)=b0*(1-exp(-b1*y))", 2), //

		/**
		 * Linear benefit function (sum of focal, \(x\), and opponent, \(y\), traits):
		 * \(B(x,y)=b_0\,(x+y)\).
		 */
		WE_LINEAR("10", "B(x,y)=b0*(x+y)", 1), //

		/**
		 * Quadratic benefit function (sum of focal, \(x\), and opponent, \(y\),
		 * traits): \(B(x,y)=b_0\,(x+y)+\b_1\,(x+y)^2\).
		 */
		WE_QUAD("11", "B(x,y)=b0*(x+y)+b1*(x+y)^2", 2), // default

		/**
		 * Saturating benefit function following a square root (sum of focal, \(x\), and
		 * opponent, \(y\), traits): \(B(x,y)=b_0\sqrt{x+y}\).
		 */
		WE_SQRT("12", "B(x,y)=b0*sqrt(x+y)", 1), //

		/**
		 * Saturating benefit function following a logarithm (sum of focal, \(x\), and
		 * opponent, \(y\), traits): \(B(x,y)=b_0\log{b_1\,(x+y)+1}\).
		 */
		WE_LOG("13", "B(x,y)=b0*ln(b1*(x+y)+1)", 2), //

		/**
		 * Saturating benefit function following an exponential (sum of focal, \(x\),
		 * and opponent, \(y\), traits): \(B(x,y)=b_0 \left(1-e^{-b_1\,(x+y)}\right)\).
		 */
		WE_EXP("14", "B(x,y)=b0*(1-exp(-b1*(x+y)))", 2), //

		/**
		 * Linear benefit function (with interaction term):
		 * \(B(x,y)=b_0\,x=b_1\,y+\b_2\,x\,y\).
		 */
		MEYOU_LINEAR("20", "B(x,y)=b0*x+b1*y+b2*x*y", 3), //

		/**
		 * Linear benefit function (independent of opponent): \(B(x,y)=b_0\,x\).
		 */
		ME_LINEAR("30", "B(x,y)=b0*x", 1), //

		/**
		 * Quadratic benefit function (independent of opponent):
		 * \(B(x,y)=b_0\,x+b_1\,x^2\).
		 */
		ME_QUAD("31", "B(x,y)=b0*x+b1*x^2", 2), //

		/**
		 * Cubic benefit function (independent of opponent):
		 * \(B(x,y)=b_0\,x+b_1\,x^2+b_2\,x^3\).
		 */
		ME_QUBIC("32", "B(x,y)=b0*x+b1*x^2+b2*x^3", 3);

		/**
		 * The key of the benefit function. Used when parsing command line options.
		 * 
		 * @see Continuous#cloBenefits
		 */
		String key;

		/**
		 * The brief description of the benefit function for the help display.
		 * 
		 * @see EvoLudo#getCLOHelp()
		 */
		String title;

		/**
		 * The number of parameters of the benefit function.
		 */
		int nParams;

		/**
		 * Create a new type of benefit function with key {@code key} and description
		 * {@code title} as well as {@code nParams} parameters.
		 * 
		 * @param key     the identifier for parsing of command line option
		 * @param title   the summary of the benefit function
		 * @param nParams the number of parameters
		 */
		Benefits(String key, String title, int nParams) {
			this.key = key;
			this.title = title;
			this.nParams = nParams;
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
	 * Command line option to set the cost function(s) for continuous traits.
	 * 
	 * @see Costs
	 */
	public final CLOption cloCosts = new CLOption("costs", Costs.ME_LINEAR.getKey() + " 1", CLOCategory.Module,
			"--costs <s0 b00[" + CLOParser.VECTOR_DELIMITER + "b01...[" + CLOParser.TRAIT_DELIMITER + //
					"s1 b10[" + CLOParser.VECTOR_DELIMITER + "b11...]]]>  cost function <si>:",
			new CLODelegate() {

				/**
				 * {@inheritDoc}
				 * <p>
				 * Parse cost functions for each trait. {@code arg} can be a single value or an
				 * array of values with the separator {@value CLOParser#VECTOR_DELIMITER}. The
				 * parser cycles through {@code arg} until all cost functions are set.
				 * 
				 * @param arg the (array of) cost function codes
				 */
				@Override
				public boolean parse(String arg) {
					String[] cstfs = arg.split(CLOParser.TRAIT_DELIMITER);
					Costs prevtype = null;
					for (int n = 0; n < nTraits; n++) {
						String cstf = cstfs[n % cstfs.length];
						Costs type = (Costs) cloCosts.match(cstf);
						String[] cstfargs = cstf.split(CLOParser.SPLIT_ARG_REGEX);
						double[] args;
						if (type == null) {
							if (prevtype == null)
								return false;
							type = prevtype;
							args = CLOParser.parseVector(cstfargs[0]);
						} else if (type.nParams > 0 && cstfargs.length < 2) {
							if (logger.isLoggable(Level.WARNING))
								logger.warning(
										"costs function type '" + type + " requires " + type.nParams + " arguments!");
							return false;
						} else
							args = CLOParser.parseVector(cstfargs[1]);
						prevtype = type;
						if (args.length != type.nParams) {
							if (logger.isLoggable(Level.WARNING))
								logger.warning("costs function type '" + type + " requires " + type.nParams
										+ " costs but found '" + Formatter.format(args, 2) + "'!");
							if (args.length < type.nParams)
								return false;
						}
						traits2payoff.setCostFunction(type, args, n);
					}
					return true;
				}
			});

	/**
	 * Command line option to set the benefit function(s) for continuous traits.
	 * 
	 * @see Benefits
	 */
	public final CLOption cloBenefits = new CLOption("benefits",
			Benefits.WE_LINEAR.getKey() + " 3",
			CLOCategory.Module,
			"--benefits <s0 b00[" + CLOParser.VECTOR_DELIMITER + "b01...[" + CLOParser.TRAIT_DELIMITER + //
					"s1 b10[" + CLOParser.VECTOR_DELIMITER + "b11...]]]>  benefit function <si>:",
			new CLODelegate() {

				/**
				 * {@inheritDoc}
				 * <p>
				 * Parse benefit functions for each trait. {@code arg} can be a single value or
				 * an array of values with the separator {@value CLOParser#VECTOR_DELIMITER}.
				 * The parser cycles through {@code arg} until all cbenefitst functions are set.
				 * 
				 * @param arg the (array of) benefit function codes
				 */
				@Override
				public boolean parse(String arg) {
					String[] bftfs = arg.split(CLOParser.TRAIT_DELIMITER);
					Benefits prevtype = null;
					for (int n = 0; n < nTraits; n++) {
						String bftf = bftfs[n % bftfs.length];
						Benefits type = (Benefits) cloBenefits.match(bftf);
						String[] bftfargs = bftf.split(CLOParser.SPLIT_ARG_REGEX);
						double[] args;
						if (type == null) {
							if (prevtype == null)
								return false;
							type = prevtype;
							args = CLOParser.parseVector(bftfargs[0]);
						} else if (type.nParams > 0 && bftfargs.length < 2) {
							if (logger.isLoggable(Level.WARNING))
								logger.warning(
										"benefits function type '" + type + " requires " + type.nParams
												+ " arguments!");
							return false;
						} else
							args = CLOParser.parseVector(bftfargs[1]);
						prevtype = type;
						if (args.length != type.nParams) {
							if (logger.isLoggable(Level.WARNING))
								logger.warning("benefits function type '" + type + " requires " + type.nParams
										+ " arguments but found '" + Formatter.format(args, 2) + "'!");
							if (args.length < type.nParams)
								return false;
						}
						traits2payoff.setBenefitFunction(type, args, n);
					}
					return true;
				}
			});

	@Override
	public void collectCLO(CLOParser parser) {
		if (this instanceof Features.Multispecies)
			return;
		super.collectCLO(parser);
		parser.addCLO(cloTraitRange);
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
