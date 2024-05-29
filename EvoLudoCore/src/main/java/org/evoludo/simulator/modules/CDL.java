//
// EvoLudo Project
//
// Copyright 2010 Christoph Hauert
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
//	Hauert, Christoph (<year>) EvoLudo Project, http://www.evoludo.org
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

package org.evoludo.simulator.modules;

import java.awt.Color;
import java.io.PrintStream;
import java.util.Arrays;

import org.evoludo.math.Combinatorics;
import org.evoludo.simulator.EvoLudo;
import org.evoludo.simulator.models.IBS.HasIBS;
import org.evoludo.simulator.models.IBSD;
import org.evoludo.simulator.models.IBSD.Init;
import org.evoludo.simulator.models.IBSDPopulation;
import org.evoludo.simulator.models.ODEEuler.HasODE;
import org.evoludo.simulator.models.PDERD.HasPDE;
import org.evoludo.simulator.models.SDEEuler.HasSDE;
import org.evoludo.simulator.modules.Discrete.Groups;
import org.evoludo.simulator.views.HasConsole;
import org.evoludo.simulator.views.HasHistogram;
import org.evoludo.simulator.views.HasMean;
import org.evoludo.simulator.views.HasPop2D;
import org.evoludo.simulator.views.HasPop3D;
import org.evoludo.simulator.views.HasS3;
import org.evoludo.util.CLOParser;
import org.evoludo.util.CLOption;
import org.evoludo.util.CLOption.CLODelegate;
import org.evoludo.util.Formatter;

/**
 * Cooperation in voluntary (non-linear) public goods interactions.
 * 
 * @author Christoph Hauert
 */
public class CDL extends Discrete implements Groups, HasIBS, HasODE, HasSDE, HasPDE, HasPop2D.Strategy,
		HasPop3D.Strategy, HasMean.Strategy, HasS3, HasPop2D.Fitness, HasPop3D.Fitness, HasMean.Fitness,
		HasHistogram.Fitness, HasHistogram.Degree, HasHistogram.StatisticsStationary, HasConsole {

	/**
	 * The trait (and index) value of cooperators.
	 */
	public static final int COOPERATE = 2;

	/**
	 * The trait (and index) value of defectors.
	 */
	public static final int DEFECT = 1;

	/**
	 * The trait (and index) value of loners.
	 */
	public static final int LONER = 0;

	/**
	 * The multiplication factor of the (non-linear) public good with a single
	 * cooperator.
	 */
	protected double r1 = 3.0;

	/**
	 * The multiplication factor of the (non-linear) public good with all
	 * cooperators.
	 */
	protected double rN = -Double.MAX_VALUE;

	/**
	 * The flag to indicate whether the public good is linear, i.e.
	 * {@code r1 == rN}.
	 */
	protected boolean isLinearPGG = true;

	/**
	 * The cost of cooperation or the individual contribution to the public good.
	 */
	protected double costCoop = 1.0;

	/**
	 * The payoff to loners.
	 */
	protected double payLoner = 1.0;

	/**
	 * The payoff to lone cooperators.
	 */
	protected double payLoneCoop = 1.0;

	/**
	 * The payoff to lone defectors.
	 */
	protected double payLoneDefect = 1.0;

	/**
	 * The flag to indicate whether cooperators benefit from their own contributions
	 * to the common pool.
	 */
	protected boolean othersOnly = false;

	/**
	 * The flag to indicate whether the public good gets created even with a single
	 * participant.
	 */
	protected boolean doSolo = false;

	/**
	 * Helper variable containing the interpolated interest rates for
	 * {@code 0, 1, ..., N} cooperators among the up to {@code N} interacting
	 * individuals.
	 */
	private double[] ninterest;

	/**
	 * Create a new instance of the module for voluntary public goods games.
	 * 
	 * @param engine the manager of modules and pacemeaker for running the model
	 */
	public CDL(EvoLudo engine) {
		super(engine);
	}

	@Override
	public void load() {
		super.load();
		nTraits = 3;
		// trait names
		String[] names = new String[nTraits];
		names[LONER] = "Loner";
		names[DEFECT] = "Defector";
		names[COOPERATE] = "Cooperator";
		setTraitNames(names);
		// trait colors (automatically generates lighter versions for new strategists)
		Color[] colors = new Color[nTraits];
		// yellow has too little contrast
		colors[LONER] = new Color(238, 204, 17); // hex #eecc11
		colors[DEFECT] = Color.RED;
		colors[COOPERATE] = Color.BLUE;
		setTraitColors(colors);
	}

	@Override
	public void unload() {
		super.unload();
		ninterest = null;
	}

	@Override
	public String getKey() {
		return "CDL";
	}

	@Override
	public String getInfo() {
		return "Title: " + getTitle() + "\nAuthor: Christoph Hauert\n"
				+ "Cooperation in voluntary (non-linear) public goods interactions.";
	}

	@Override
	public String getTitle() {
		return "Volunteering in (non-linear) public goods games";
	}

	@Override
	public String getVersion() {
		return "v1.0 March 2021";
	}

	@Override
	public int getDependent() {
		return LONER;
	}

	@Override
	public double getMinGameScore() {
		double min = getMonoGameScore(0);
		for (int n = 1; n < nTraits; n++)
			min = Math.min(min, getMonoGameScore(n));
		min = Math.min(min, payLoneCoop);
		min = Math.min(min, payLoneDefect);
		// one cooperator and many defectors
		int[] sample = new int[nTraits];
		double[] traitScores = new double[nTraits];
		sample[LONER] = 0;
		sample[COOPERATE] = 1;
		sample[DEFECT] = nGroup - 1;
		groupScores(sample, traitScores);
		min = Math.min(min, traitScores[COOPERATE]);
		return min;
	}

	@Override
	public double getMaxGameScore() {
		double max = getMonoGameScore(0);
		for (int n = 1; n < nTraits; n++)
			max = Math.max(max, getMonoGameScore(n));
		max = Math.max(max, payLoneCoop);
		max = Math.max(max, payLoneDefect);
		// many cooperators and one defectors
		int[] sample = new int[nTraits];
		double[] traitScores = new double[nTraits];
		sample[LONER] = 0;
		sample[COOPERATE] = nGroup - 1;
		sample[DEFECT] = 1;
		groupScores(sample, traitScores);
		max = Math.max(max, traitScores[DEFECT]);
		return max;
	}

	@Override
	public double getMonoGameScore(int type) {
		switch (type) {
			case COOPERATE:
				return ((othersOnly ? interest(nGroup - 1) : interest(nGroup)) - 1.0) * costCoop;
			case DEFECT:
				return 0.0;
			case LONER:
				return payLoner;
			default:
				return Double.NaN;
		}
	}

	/**
	 * Helper method to return the interest rate/multiplication factor of the public
	 * good with {@code nc} contributors.
	 * 
	 * @param nc the number of contributors
	 * @return the interest rate for {@code nc} contributors
	 */
	protected double interest(int nc) {
		if (isLinearPGG)
			return r1;
		return ninterest[nc];
	}

	@Override
	public double pairScores(int me, int[] traitCount, double[] traitScore) {
		int x = traitCount[COOPERATE];
		int y = traitCount[DEFECT];
		int z = traitCount[LONER];
		int n = x + y + z;

		traitScore[LONER] = payLoner;
		switch (me) {
			case LONER:
				traitScore[COOPERATE] = payLoneCoop;
				traitScore[DEFECT] = payLoneDefect;
				return n * payLoner;

			case COOPERATE:
				if (othersOnly) {
					traitScore[COOPERATE] = (r1 - 1.0) * costCoop;
					traitScore[DEFECT] = r1 * costCoop;
					return x * traitScore[COOPERATE] - y * costCoop + z * payLoneCoop;
				}
				traitScore[COOPERATE] = (interest(2) - 1.0) * costCoop;
				traitScore[DEFECT] = r1 * 0.5 * costCoop;
				return x * traitScore[COOPERATE] + y * (traitScore[DEFECT] - costCoop) + z * payLoneCoop;

			case DEFECT:
				traitScore[DEFECT] = 0.0;
				if (othersOnly) {
					traitScore[COOPERATE] = -costCoop;
					return x * r1 * costCoop /* + y * 0.0 */ + z * payLoneDefect;
				}
				traitScore[COOPERATE] = (r1 * 0.5 - 1.0) * costCoop;
				return x * r1 * 0.5 * costCoop + /* + y * 0.0 */ +z * payLoneDefect;

			default: // should not end here
				throw new Error("Unknown strategy (" + me + ")");
		}
	}

	@Override
	public void groupScores(int[] traitCount, double[] traitScore) {
		int x = traitCount[COOPERATE];
		int y = traitCount[DEFECT];
		// int z = count[LONER];
		int n = x + y;

		traitScore[LONER] = payLoner;
		if (n < 2) {
			// note: initInterest took care of adjusting payLoneXXX if public good is
			// produced
			// by a single individual
			traitScore[COOPERATE] = payLoneCoop;
			traitScore[DEFECT] = payLoneDefect;
			return;
		}
		if (othersOnly) {
			int k = Math.max(0, x - 1);
			traitScore[COOPERATE] = (k * interest(k) / (n - 1) - 1.0) * costCoop;
			traitScore[DEFECT] = x * interest(x) / (n - 1) * costCoop;
			return;
		}
		double b = x * costCoop * interest(x) / n;
		traitScore[COOPERATE] = b - costCoop;
		traitScore[DEFECT] = b;
	}

	/**
	 * {@inheritDoc}
	 * <dl>
	 * <dt>
	 * standard non-linear PGG:
	 * <dd>
	 * \[
	 * \begin{align}
	 * f_L =&amp; c \sigma \\
	 * f_D =&amp; \frac{X}{M-1}\frac{N}{M-N} (B + S) + H_2(X+Y-1, 0, M-X-Y, N-1)
	 * \sigma c \\
	 * f_C =&amp; \frac{(r_1-1)N}{M-N} \left(1-H_2(X+Y-1, 0, M-X-Y, N-1)\right) c
	 * +\\
	 * &amp; \frac{N}{M-N}\left(\frac{X-2}{M-1} S - \frac{Y}{M-1} B\right) +
	 * H_2(X+Y-1, 0, M-X-Y, N-1) \sigma c
	 * \end{align}
	 * \]
	 * with
	 * \[
	 * \begin{align}
	 * B =&amp; \frac{M-1}{X+Y} \frac{M-N}{M}\left(r_1 - \frac{2 S}{N-1}\right)
	 * \times
	 * \left(\frac{N}{M-N} - \frac{\big(1-H_2(X+Y-1, 0, M-X-Y,
	 * N)\big)M}{N(X+Y-1)}\right) c \\
	 * S =&amp; \frac{(r_\text{all}-r_1)(X-1)}{(X+Y-2)} c
	 * \end{align}
	 * \]
	 * using
	 * \[
	 * H_2(X, x, Y, y) = \frac{\binom{X}{x}\binom{Y}{y}}{\binom{X+Y}{x+y}}
	 * \]
	 * 
	 * <dt>
	 * other's only non-linear PGG:
	 * <dd>
	 * \[
	 * \begin{align}
	 * f_L =&amp; c \sigma \\
	 * f_D =&amp; \frac{X}{M-1} \frac{N}{M-N} (B + S) + H_2(X+Y-1, 0, M-X-Y, N-1)
	 * \sigma c \\
	 * f_C =&amp; \frac{X-2}{M-1} \frac{N}{M-N} (B + S) +\frac{r_1
	 * (N-1)}{(M-N)(X+Y)}c-
	 * \frac{N}{M-N}\left(\frac{r_1 (M-X-Y-N+1)}{N(X+Y)(X+Y-1)}+1\right)\times \\
	 * &amp; \left(1-H_2(X+Y-1, 0, M-X-Y, N-1)\right)c+
	 * H_2(X+Y-1, 0, M-X-Y, N-1) \sigma c
	 * \end{align}
	 * \]
	 * with
	 * \[
	 * \begin{align}
	 * B =&amp; \frac{M-1}{X+Y} \frac{M-N}{M} \left(r_1 - \frac{2 S}{N-1}\right)
	 * \times
	 * \left(\frac{N}{M-N} - \frac{\big(1-H_2(X+Y-1, 0, M-X-Y,
	 * N)\big)M}{N(X+Y-1)}\right)c \\
	 * S =&amp; \frac{(r_\text{all}-r_1)(X-1)}{X+Y-2}\frac{N-1}{N-2}c.
	 * \end{align}
	 * \]
	 * </dl>
	 */
	@Override
	public void mixedScores(int[] count, int n, double[] traitScores) {
		int x = count[COOPERATE];
		int y = count[DEFECT];
		int z = count[LONER];
		int m = x + y + z;
		// formulas fail if nPopulation==nGroup
		if (m == n) {
			groupScores(count, traitScores);
			return;
		}
		int m1 = m - 1;
		// if all or all but one are loners, payoffs are trivial
		traitScores[LONER] = payLoner;
		if (z >= m1) {
			traitScores[DEFECT] = payLoneDefect;
			traitScores[COOPERATE] = payLoneCoop;
			return;
		}
		// payoffs from public goods interactions
		// zn1 = Binomial[z, n-1]/Binomial[m-1, n-1]
		int mz = x + y;
		double zm1n1 = Combinatorics.combinationFrac(z, m1, n - 1); // same as Combinatorics.H2(mz-1, 0, z, n-1)
		traitScores[DEFECT] = payLoneDefect * zm1n1;
		traitScores[COOPERATE] = payLoneCoop * zm1n1;
		if (x == 0)
			return;
		double zn = zm1n1 * (z - n + 1) / m;
		if (isLinearPGG) {
			if (othersOnly) {
				double b = (r1 * x * (1.0 - zm1n1) / (mz - 1)) * costCoop;
				traitScores[DEFECT] += b;
				traitScores[COOPERATE] += b - (1.0 - zm1n1) * costCoop;
				return;
			}
			double b = r1 * costCoop * (1.0 - m * (1.0 - zn) / (n * mz)) / (mz - 1);
			traitScores[DEFECT] += b * x;
			traitScores[COOPERATE] += (r1 - 1.0) * (1.0 - zm1n1) * costCoop - b * y;
			return;
		}
		// non-linear PGG
		if (othersOnly) {
			double s = (rN - r1) * x * (x - 1) / ((n - 2) * (mz - 2));
			double b = (s * (n - 1) / (m - 1) + (r1 * x - s) * (1.0 - zm1n1) / (mz - 1)) * costCoop;
			traitScores[DEFECT] += b;
			traitScores[COOPERATE] += b - (1.0 - zm1n1) * costCoop;
		}
		double s = mz == 2 ? 0 : (r1 - rN) * (x - 1) / (mz - 2);
		double b = (r1 + 2.0 * s / (n - 1)) * (1.0 - m * (1.0 - zn) / (n * mz)) / (mz - 1);
		double sim1 = s / (m - 1);
		traitScores[DEFECT] += x * (b - sim1) * costCoop;
		traitScores[COOPERATE] += ((r1 - 1.0) * (1.0 - zm1n1) - b * y - (x - 2) * sim1) * costCoop;
	}

	/**
	 * {@inheritDoc}
	 * <dl>
	 * <dt>
	 * standard non-linear PGG:
	 * <dd>
	 * \[
	 * \begin{align}
	 * f_L =&amp; c \sigma \\
	 * f_D =&amp; x (B + S) c + \sigma c z^{N-1} \\
	 * f_C =&amp; (r_1-1)\left(1-z^{N-1}\right)c-y B c + x S c + \sigma c z^{N-1}
	 * \end{align}
	 * \]
	 * with
	 * \[
	 * \begin{align}
	 * B =&amp; \frac1{1-z} \left(r_1 - \frac{2 S}{N-1}\right)
	 * \left(1-\frac{1-z^N}{N (1-z)}\right) \\
	 * S =&amp; x \frac{r_\text{all}-r_1}{1-z}
	 * \end{align}
	 * \]
	 * <dt>
	 * other's only non-linear PGG:
	 * <dd>
	 * \[
	 * \begin{align}
	 * f_L =&amp; c \sigma \\
	 * f_D =&amp; x (B + S) c + \sigma c z^{N-1} \\
	 * f_C =&amp; x (B + S) c - \left(1-z^{N-1}\right)c + \sigma c z^{N-1}
	 * \end{align}
	 * \]
	 * with
	 * \[
	 * \begin{align}
	 * B =&amp; \frac1{1-z} \left(r_1 - \frac{2 S}{N-1}\right)
	 * \left(1 - \frac{1-z^N}{N (1-z)}\right) \\
	 * S =&amp; x \frac{r_\text{all}-r_1}{1-z}\frac{N-1}{N-2}
	 * \end{align}
	 * \]
	 * </dl>
	 */
	@Override
	public void avgScores(double[] density, int n, double[] avgscores) {
		double x = density[COOPERATE];
		double y = density[DEFECT];
		double z = density[LONER];

		avgscores[LONER] = payLoner;
		if (z > 1.0 - 1e-7) {
			avgscores[COOPERATE] = payLoneCoop;
			avgscores[DEFECT] = payLoneDefect;
			return;
		}
		double zn1 = Combinatorics.pow(z, n - 1);
		double i1z = 1.0 / (1.0 - z);
		double in1z = i1z / n;
		if (isLinearPGG) {
			if (othersOnly) {
				double c = (1.0 - zn1) * costCoop;
				double b = x * r1 * i1z * c;
				avgscores[DEFECT] = payLoneDefect * zn1 + b;
				avgscores[COOPERATE] = payLoneCoop * zn1 + b - c;
				return;
			}
			double b = r1 * i1z * (1.0 - (1.0 - zn1 * z) * in1z) * costCoop;
			avgscores[DEFECT] = payLoneDefect * zn1 + x * b;
			avgscores[COOPERATE] = payLoneCoop * zn1 + (r1 - 1.0) * (1.0 - zn1) * costCoop - y * b;
			return;
		}
		// non-linear PGG
		if (othersOnly) {
			double l = 1.0 - zn1;
			double s = x * (rN - r1) * (n - 1) / (n - 2) * (1.0 - l * i1z / (n - 1));
			double b = x * i1z * (r1 * l + s) * costCoop;
			avgscores[DEFECT] = payLoneDefect * zn1 + b;
			avgscores[COOPERATE] = payLoneCoop * zn1 + b - l * costCoop;
		}
		// standard returns (contributors get share)
		double s = x * (r1 - rN) * i1z;
		double b = (r1 + 2.0 * s / (n - 1)) * (1.0 - (1.0 - zn1 * z) * in1z) * i1z;
		avgscores[DEFECT] = payLoneDefect * zn1 + x * (b - s) * costCoop;
		avgscores[COOPERATE] = payLoneCoop * zn1 + ((r1 - 1.0) * (1.0 - zn1) - x * s - y * b) * costCoop;
	}

	@Override
	public boolean check() {
		super.check();
		if (!cloLoneCooperator.isSet())
			setPayLoneCoop(getPayLoner());
		if (!cloLoneDefector.isSet())
			setPayLoneDefect(getPayLoner());
		initInterest();
		return false;
	}

	/**
	 * Helper method to initialize the array with nonlinear interest rates for group
	 * size {@code 1,2,3,...,N}, where {@code N} is the maximum interaction group
	 * size based on the interest rate for groups with a single cooperator
	 * {@code r1} and groups of all cooperators {@code rN}. Also takes into account
	 * whether a contributor gets a share of the benefit generated by their own
	 * contribution.
	 */
	private void initInterest() {
		if (doSolo) {
			payLoneDefect = 0.0;
			payLoneCoop = (r1 - 1.0) * costCoop;
		}
		if (isLinearPGG) {
			ninterest = null;
			return;
		}
		// initialize interest rates before calling super because ninterest
		// will be needed to determine min/max game scores
		int ilen = othersOnly ? nGroup : nGroup + 1;
		if (ninterest == null || ninterest.length != ilen)
			ninterest = new double[ilen];
		ninterest[0] = 0.0;
		ninterest[1] = r1;
		double rIncr = (rN - r1) / (ilen - 2);
		for (int n = 2; n < ilen; n++)
			ninterest[n] = ninterest[n - 1] + rIncr;
	}

	/**
	 * Set the cost of cooperation.
	 * 
	 * @param aValue the cost of cooperation
	 */
	public void setCostCoop(double aValue) {
		costCoop = aValue;
	}

	/**
	 * Get the cost of cooperation.
	 * 
	 * @return the cost of cooperation
	 */
	public double getCostCoop() {
		return costCoop;
	}

	/**
	 * Set interest (multiplication factor) in <em>linear</em> public goods
	 * interaction.
	 * 
	 * @param r the multiplication factor in linear public goods interactions
	 */
	public void setInterest(double r) {
		this.r1 = r;
		this.rN = -Double.MAX_VALUE;
		isLinearPGG = true;
	}

	/**
	 * Set non-linear interest (multiplication factor) in public goods interaction
	 * with interest <code>r1</code> with a single contributor and <code>rN</code>
	 * if all <code>N</code> participants contribute. The interest rate for \(n\)
	 * contributors is given by
	 * \[ r(n) = r1 + n * (rN - r1) / M \]
	 * where \(M\) denotes the maximum number of contributors (\(N\) for standard
	 * public good games and one less for other's only public good games).
	 * 
	 * @param r1 the multiplication factor for a single contributor
	 * @param rN the multiplication factor with only contributors
	 */
	public void setInterest(double r1, double rN) {
		if (Math.abs(rN - r1) < 1e-3) {
			setInterest(r1);
			return;
		}
		this.r1 = r1;
		this.rN = rN;
		isLinearPGG = false;
	}

	/**
	 * Get the interest rate in linear public goods games, or, for a single
	 * contributor in non-linear public goods games, i.e. if
	 * {@code isLinearPGG == false}.
	 * 
	 * @return the interest rate
	 */
	public double getInterest() {
		return r1;
	}

	/**
	 * Set whether public goods is produced already with a single contributor and no
	 * other participants. Default is <code>false</code>.
	 * 
	 * @param aValue <code>true</code> if single contributor is sufficient.
	 */
	public void setSolo(boolean aValue) {
		doSolo = aValue;
	}

	/**
	 * Get whether a single contributor is enough to generate the public good.
	 * 
	 * @return <code>true</code> if single contributor is sufficient to produce
	 *         public good.
	 */
	public boolean getSolo() {
		return doSolo;
	}

	/**
	 * Set the flag whether the return of the public good is split among all members
	 * of the group or only all other's, i.e. excluding the contributor itself. In
	 * the other's only case contributors do not reap any share of the benefits
	 * created by their own contributions.
	 * 
	 * @param aValue <code>true</code> if other's only
	 */
	public void setOthersOnly(boolean aValue) {
		othersOnly = aValue;
	}

	/**
	 * Get the flag whether the return of the public good is split among all members
	 * of the group or only all other's, i.e. excluding the contributor itself.
	 * 
	 * @return <code>true</code> other's only
	 */
	public boolean getOthersOnly() {
		return othersOnly;
	}

	/**
	 * Set the payoff of loners.
	 * 
	 * @param aValue the new payoff of loners
	 */
	public void setPayLoner(double aValue) {
		payLoner = aValue;
	}

	/**
	 * Set the payoff of loners.
	 * 
	 * @return the payoff of loners
	 */
	public double getPayLoner() {
		return payLoner;
	}

	/**
	 * Set the payoff to lone cooperators. Defaults to loner payoff.
	 * 
	 * @param aValue the payoff to lone cooperators.
	 */
	public void setPayLoneCoop(double aValue) {
		payLoneCoop = aValue;
	}

	/**
	 * Get the payoff to lone cooperators.
	 * 
	 * @return the payoff to lone cooperators.
	 */
	public double getPayLoneCoop() {
		return payLoneCoop;
	}

	/**
	 * Set the payoff to lone defectors. Defaults to loner payoff.
	 * 
	 * @param aValue the payoff to lone defectors.
	 */
	public void setPayLoneDefect(double aValue) {
		payLoneDefect = aValue;
	}

	/**
	 * Get the payoff to lone defectors.
	 * 
	 * @return the payoff to lone defectors.
	 */
	public double getPayLoneDefect() {
		return payLoneDefect;
	}

	/**
	 * Command line option to set the multiplication factor for public good
	 * interactions.
	 */
	public final CLOption cloInterest = new CLOption("interest", "3", EvoLudo.catModule,
			"--interest <r[,rall]>  multiplication factor of public good\n"
					+ "                (with rall, factor linearly depends on contributors)",
			new CLODelegate() {

				/**
				 * {@inheritDoc}
				 * <p>
				 * Parse the multiplication factor for contributions to the common pool. If only
				 * one value is provided the multiplication factor is constant corresponding to
				 * the classical <em>linear</em> public goods game. If two values are specified
				 * (separated by {@value CLOParser#VECTOR_DELIMITER}), the first represents the
				 * multiplication factor if there is only a single contributor, while the second
				 * refers to the multiplication factor if all particiapnts contribute.
				 * Multiplication factors for intermediate numbers of contributors are linearly
				 * interpolated.
				 * 
				 * @param arg the multiplication factor(s)
				 */
				@Override
				public boolean parse(String arg) {
					double[] r = CLOParser.parseVector(arg);
					switch (r.length) {
						case 1:
							setInterest(r[0]);
							return true;
						case 2:
							setInterest(r[0], r[1]);
							return true;
						default:
							logger.warning("failed to parse interest (" + arg + ").");
							return false;
					}
				}

				@Override
				public void report(PrintStream output) {
					if (isLinearPGG)
						output.println("# interest:             " + Formatter.format(getInterest(), 4));
					else
						output.println("# interest:             " + Formatter.format(interest(1), 4) + " - "
								+ Formatter.format(interest(nGroup), 4));
				}
			});

	/**
	 * Command line option to set the cost of cooperation, i.e. contributions to the
	 * public good.
	 */
	public final CLOption cloCost = new CLOption("cost", "1", EvoLudo.catModule,
			"--cost <c>      cost of cooperation", new CLODelegate() {

				/**
				 * {@inheritDoc}
				 * <p>
				 * Parse the cost of cooperation.
				 * 
				 * @param arg the cost of cooperation
				 */
				@Override
				public boolean parse(String arg) {
					setCostCoop(CLOParser.parseDouble(arg));
					return true;
				}

				@Override
				public void report(PrintStream output) {
					output.println("# cost:                 " + Formatter.format(getCostCoop(), 4));
				}
			});

	/**
	 * Command line option to set the payoff to loners that refuse to participate in
	 * the public goods interaction.
	 */
	public final CLOption cloLoner = new CLOption("loner", "1", EvoLudo.catModule,
			"--loner <l>     loner payoff", new CLODelegate() {

				/**
				 * {@inheritDoc}
				 * <p>
				 * Parse the payoff to loners.
				 * 
				 * @param arg the payoff to loners
				 */
				@Override
				public boolean parse(String arg) {
					setPayLoner(CLOParser.parseDouble(arg));
					return true;
				}

				@Override
				public void report(PrintStream output) {
					output.println("# loner:                " + Formatter.format(getPayLoner(), 4));
				}
			});

	/**
	 * Command line option to set the payoff to cooperators that failed to find any
	 * interaction partners.
	 */
	public final CLOption cloLoneCooperator = new CLOption("lonecoop", "1", EvoLudo.catModule,
			"--lonecoop <c>  payoff of lone cooperator", new CLODelegate() {

				/**
				 * {@inheritDoc}
				 * <p>
				 * Parse the payoff to lone cooperators.
				 * 
				 * @param arg the payoff to lone cooperators
				 */
				@Override
				public boolean parse(String arg) {
					setPayLoneCoop(CLOParser.parseDouble(arg));
					return true;
				}

				@Override
				public void report(PrintStream output) {
					if (!doSolo)
						output.println("# lonecooperator:       " + Formatter.format(getPayLoneCoop(), 4));
				}
			});

	/**
	 * Command line option to set the payoff to defectors that failed to find any
	 * interaction partners.
	 */
	public final CLOption cloLoneDefector = new CLOption("lonedefect", "1", EvoLudo.catModule,
			"--lonedefect <d>  payoff of lone defector", new CLODelegate() {

				/**
				 * {@inheritDoc}
				 * <p>
				 * Parse the payoff to lone defectors.
				 * 
				 * @param arg the payoff to lone defectors
				 */
				@Override
				public boolean parse(String arg) {
					setPayLoneDefect(CLOParser.parseDouble(arg));
					return true;
				}

				@Override
				public void report(PrintStream output) {
					if (!doSolo)
						output.println("# lonedefector:         " + Formatter.format(getPayLoneDefect(), 4));
				}
			});

	/**
	 * Command line option to set whether contributors get a share of the benefits
	 * generated by their own contributions.
	 */
	public final CLOption cloOthers = new CLOption("others", "all", CLOption.Argument.NONE, EvoLudo.catModule,
			"--others        benefits to others only", new CLODelegate() {

				/**
				 * {@inheritDoc}
				 * <p>
				 * If option is provided, the benefits created by a contributor are share only
				 * among the <em>other</em> participants.
				 * 
				 * @param arg no argument required
				 */
				@Override
				public boolean parse(String arg) {
					setOthersOnly(cloOthers.isSet());
					return true;
				}

				@Override
				public void report(PrintStream output) {
					output.println("# othersonly:           " + getOthersOnly());
				}
			});

	/**
	 * Command line option to set whether a single contributor suffices to generate
	 * the public good.
	 */
	public final CLOption cloSolo = new CLOption("solo", "groups", CLOption.Argument.NONE, EvoLudo.catModule,
			"--solo          lone individuals also generate public good\n"
					+ "                (overrides --lonecoop and --lonedefect)",
			new CLODelegate() {

				/**
				 * {@inheritDoc}
				 * <p>
				 * If option is provided, a single contributor suffices to generate the public
				 * good.
				 * 
				 * @param arg no argument required
				 */
				@Override
				public boolean parse(String arg) {
					setSolo(cloSolo.isSet());
					return true;
				}

				@Override
				public void report(PrintStream output) {
					if (doSolo)
						output.println("# sologroups:           " + getSolo());
				}
			});

	@Override
	public void collectCLO(CLOParser parser) {
		super.collectCLO(parser);
		parser.addCLO(cloInterest);
		parser.addCLO(cloCost);
		parser.addCLO(cloLoner);
		parser.addCLO(cloLoneCooperator);
		parser.addCLO(cloLoneDefector);
		parser.addCLO(cloOthers);
		parser.addCLO(cloSolo);
	}

	@Override
	public void adjustCLO(CLOParser parser) {
		super.adjustCLO(parser);
		if (model instanceof IBSD) {
			CLOption clo = ((IBSDPopulation) getIBSPopulation()).getInit().clo;
			clo.addKey(Init.Type.KALEIDOSCOPE);
		}
	}

	@Override
	public CDL.IBS createIBSPop() {
		return new CDL.IBS(engine);
	}

	/**
	 * The extension for IBS simulations specific to voluntary (non-linear) public
	 * goods games. This extension implements specific initial conditions that give
	 * rise to fascinating evolutionary kaleidoscopes for deterministic updating.
	 */
	public class IBS extends IBSDPopulation {

		/**
		 * Create a new instance of the IBS model for voluntary (non-linear) public
		 * goods games.
		 * 
		 * @param engine the pacemeaker for running the model
		 */
		protected IBS(EvoLudo engine) {
			super(engine);
		}

		@Override
		protected void initKaleidoscope() {
			// kaleidoscopes only available for lattice geometries
			if (!getInteractionGeometry().isLattice()) {
				logger.warning("kaleidoscopes require lattices - using default initialization.");
				initUniform();
				return;
			}
			Arrays.fill(strategiesTypeCount, 0);
			int r, c, mid, size;
			switch (getInteractionGeometry().getType()) {
				case CUBE:
					int l, mz;
					if (nPopulation == 25000) {
						l = 50;
						mz = 5; // 10/2
					} else {
						l = (int) (Math.pow(nPopulation, 1.0 / 3.0) + 0.5);
						mz = l / 2;
					}
					int l2 = l * l;
					int m = l / 2;
					Arrays.fill(strategies, CDL.COOPERATE);
					Arrays.fill(strategiesTypeCount, 0);
					strategiesTypeCount[CDL.COOPERATE] = nPopulation;
					if (l % 2 == 0) { // even number of sites
						// since NOVA has even numbers of pixels along each side, place a 4x4x4 cube of
						// D's in the center
						// and a 2x2x2 cube of loners within
						for (int z = mz - 2; z < mz + 2; z++)
							for (int y = m - 2; y < m + 2; y++)
								for (int x = m - 2; x < m + 2; x++)
									strategies[z * l2 + y * l + x] = CDL.DEFECT;
						for (int z = mz - 1; z < mz + 1; z++)
							for (int y = m - 1; y < m + 1; y++)
								for (int x = m - 1; x < m + 1; x++)
									strategies[z * l2 + y * l + x] = CDL.LONER;
						strategiesTypeCount[CDL.LONER] += 2 * 2 * 2;
						strategiesTypeCount[CDL.DEFECT] += 4 * 4 * 4 - 2 * 2 * 2;
						strategiesTypeCount[CDL.COOPERATE] -= 4 * 4 * 4;
					} else {
						// odd number of sites - place 5x5x5 cube of D's in the center and a 3x3x3 cube
						// of loners within
						for (int z = mz - 2; z < mz + 3; z++)
							for (int y = m - 2; y < m + 3; y++)
								for (int x = m - 2; x < m + 3; x++)
									strategies[z * l2 + y * l + x] = CDL.DEFECT;
						for (int z = mz - 1; z < mz + 2; z++)
							for (int y = m - 1; y < m + 2; y++)
								for (int x = m - 1; x < m + 2; x++)
									strategies[z * l2 + y * l + x] = CDL.LONER;
						strategiesTypeCount[CDL.LONER] += 3 * 3 * 3;
						strategiesTypeCount[CDL.DEFECT] += 5 * 5 * 5 - 3 * 3 * 3;
						strategiesTypeCount[CDL.COOPERATE] -= 5 * 5 * 5;
					}
					break;

				case SQUARE_NEUMANN:
				case SQUARE_NEUMANN_2ND:
				case SQUARE_MOORE:
				case SQUARE:
					Arrays.fill(strategies, CDL.COOPERATE);
					Arrays.fill(strategiesTypeCount, 0);
					strategiesTypeCount[CDL.COOPERATE] = nPopulation;
					size = (int) Math.floor(Math.sqrt(nPopulation) + 0.5);
					mid = size / 2;
					/* border around center */
					int cells = 5;
					for (r = -cells / 2; r <= cells / 2; r++)
						for (c = -cells / 2; c <= cells / 2; c++)
							strategies[(mid + r) * size + mid + c] = CDL.DEFECT;
					/* square in center */
					cells = 3;
					for (r = -cells / 2; r <= cells / 2; r++)
						for (c = -cells / 2; c <= cells / 2; c++)
							strategies[(mid + r) * size + mid + c] = CDL.LONER;
					strategiesTypeCount[CDL.LONER] += 3 * 3;
					strategiesTypeCount[CDL.DEFECT] += 5 * 5 - 3 * 3;
					strategiesTypeCount[CDL.COOPERATE] -= 5 * 5;
					break;

				case HONEYCOMB:
					Arrays.fill(strategies, CDL.COOPERATE);
					Arrays.fill(strategiesTypeCount, 0);
					strategiesTypeCount[CDL.COOPERATE] = nPopulation;
					mid = (int) Math.floor(nPopulation + Math.sqrt(nPopulation) + 0.5) / 2;
					size = (int) Math.floor(Math.sqrt(nPopulation) + 0.5);
					strategies[mid] = CDL.LONER;
					strategies[mid + 1] = CDL.LONER;
					strategies[mid - 1] = CDL.LONER;
					strategies[mid - size] = CDL.LONER;
					strategies[mid + size] = CDL.LONER;
					strategies[mid + 2] = CDL.DEFECT;
					strategies[mid - 2] = CDL.DEFECT;
					strategies[mid - 2 * size] = CDL.DEFECT;
					strategies[mid + 2 * size] = CDL.DEFECT;
					strategies[mid - 2 * size - 1] = CDL.DEFECT;
					strategies[mid + 2 * size - 1] = CDL.DEFECT;
					strategies[mid - 2 * size + 1] = CDL.DEFECT;
					strategies[mid + 2 * size + 1] = CDL.DEFECT;
					if (mid % 2 == 1) {
						strategies[mid - size + 1] = CDL.LONER;
						strategies[mid + size + 1] = CDL.LONER;
						strategies[mid - size - 1] = CDL.DEFECT;
						strategies[mid + size - 1] = CDL.DEFECT;
						strategies[mid - size + 2] = CDL.DEFECT;
						strategies[mid + size + 2] = CDL.DEFECT;
					} else {
						strategies[mid - size - 1] = CDL.LONER;
						strategies[mid + size - 1] = CDL.LONER;
						strategies[mid - size + 1] = CDL.DEFECT;
						strategies[mid + size + 1] = CDL.DEFECT;
						strategies[mid - size - 2] = CDL.DEFECT;
						strategies[mid + size - 2] = CDL.DEFECT;
					}
					strategiesTypeCount[CDL.LONER] += 7;
					strategiesTypeCount[CDL.DEFECT] += 12;
					strategiesTypeCount[CDL.COOPERATE] -= 19;
					break;

				case TRIANGULAR:
					Arrays.fill(strategies, CDL.COOPERATE);
					Arrays.fill(strategiesTypeCount, 0);
					strategiesTypeCount[CDL.COOPERATE] = nPopulation;
					mid = (int) Math.floor(nPopulation + Math.sqrt(nPopulation) + 0.5) / 2;
					size = (int) Math.floor(Math.sqrt(nPopulation) + 0.5);
					strategies[mid] = CDL.LONER;
					strategies[mid - 1] = CDL.LONER;
					strategies[mid + 1] = CDL.LONER;
					strategies[mid + size] = CDL.LONER;
					strategies[mid + 2] = CDL.DEFECT;
					strategies[mid - 2] = CDL.DEFECT;
					strategies[mid + 2 + size] = CDL.DEFECT;
					strategies[mid - 2 + size] = CDL.DEFECT;
					strategies[mid + 1 + size] = CDL.DEFECT;
					strategies[mid - 1 + size] = CDL.DEFECT;
					strategies[mid + 1 - size] = CDL.DEFECT;
					strategies[mid - 1 - size] = CDL.DEFECT;
					strategies[mid - size] = CDL.DEFECT;

					strategies[mid + 2 - size] = CDL.DEFECT;
					strategies[mid - 2 - size] = CDL.DEFECT;
					strategies[mid + 3] = CDL.DEFECT;
					strategies[mid - 3] = CDL.DEFECT;
					strategies[mid + 3 - size] = CDL.LONER;
					strategies[mid - 3 - size] = CDL.LONER;
					strategies[mid + 2 * size] = CDL.LONER;
					strategies[mid + 2 * size - 1] = CDL.DEFECT;
					strategies[mid + 2 * size + 1] = CDL.DEFECT;
					strategies[mid + 4 - size] = CDL.DEFECT;
					strategies[mid - 4 - size] = CDL.DEFECT;
					strategies[mid + 3 * size] = CDL.DEFECT;

					strategiesTypeCount[CDL.LONER] += 13;
					strategiesTypeCount[CDL.DEFECT] += 9;
					strategiesTypeCount[CDL.COOPERATE] -= 22;
					break;

				default:
					// should never get here - check made sure of it.
					throw new Error("geometry incompatible with kaleidoscopes!");
			}
		}
	}
}
