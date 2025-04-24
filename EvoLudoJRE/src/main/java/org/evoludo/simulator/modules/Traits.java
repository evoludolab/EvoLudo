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

import java.io.PrintStream;

import org.evoludo.math.ArrayMath;
import org.evoludo.math.RNGDistribution;
import org.evoludo.simulator.EvoLudo;
import org.evoludo.simulator.models.IBS.HasIBS;
import org.evoludo.simulator.models.Model;
import org.evoludo.simulator.models.ODE.HasODE;
import org.evoludo.simulator.models.PDE.HasPDE;
import org.evoludo.simulator.models.SDE.HasSDE;
import org.evoludo.simulator.models.SDEN;
import org.evoludo.simulator.modules.Features.Payoffs;
import org.evoludo.simulator.views.HasHistogram;
import org.evoludo.simulator.views.HasMean;
import org.evoludo.simulator.views.HasPop2D;
import org.evoludo.simulator.views.HasPop3D;
import org.evoludo.util.CLOParser;
import org.evoludo.util.CLOption;
import org.evoludo.util.CLOption.CLODelegate;
import org.evoludo.util.Formatter;

/**
 * Evolutionary dynamics in populations with {@code d} strategic traits.
 *
 * @evoludo.impl This module requires external libraries for modelling
 *               stochastic differential equations. More specifically, for
 *               solving linear algebra problems and eigenvalues, in particular,
 *               for {@code d>3} strategies, the <a href=
 *               "https://javadoc.io/doc/com.googlecode.matrix-toolkits-java/mtj/latest/index.html">
 *               matrix toolkit for java</a> is used. This dependency pulls in
 *               all the native
 *               <a href="https://github.com/fommil/netlib-java">netlib</a>
 *               libraries, weighing in at a total of ~40MB (or ~15MB in final
 *               .jar).
 * 
 * @author Christoph Hauert
 */
public class Traits extends Discrete implements Payoffs,
		HasIBS.DPairs, HasODE, HasSDE, HasPDE,
		HasPop2D.Traits, HasPop3D.Traits, HasMean.Traits, HasPop2D.Fitness, HasPop3D.Fitness, HasMean.Fitness,
		HasHistogram.Fitness, HasHistogram.Degree {
	protected static final int PAYOFF_UNITY = 0;
	protected static final int PAYOFF_CONST = 1;
	protected static final int PAYOFF_MATRIX = 2;
	protected static final int PAYOFF_RANDOM = 3;

	/**
	 * The shared random number generator to ensure reproducibility of results.
	 * 
	 * @see EvoLudo#getRNG()
	 */
	// RNGDistribution rng;

	double[][] payoff;
	int payoffType = PAYOFF_CONST;

	public Traits(EvoLudo engine) {
		super(engine);
	}

	@Override
	public void load() {
		super.load();
		nTraits = 2;
		initPayoffs();
		// set default colors
		setTraitColors(null);
	}

	@Override
	public void unload() {
		super.unload();
		payoff = null;
	}

	@Override
	public String getKey() {
		return "DT";
	}

	@Override
	public String getAuthors() {
		return "Christoph Hauert";
	}

	@Override
	public String getTitle() {
		return "Random Games with D Types";
	}

	@Override
	public double getMinPayoff() {
		return ArrayMath.min(payoff);
	}

	@Override
	public double getMaxPayoff() {
		return ArrayMath.max(payoff);
	}

	@Override
	public double getMonoPayoff(int type) {
		return payoff[type][type];
	}

	@Override
	public double pairScores(int me, int[] traitCount, double[] traitScore) {
		double myScore = 0.0;
		for (int i = 0; i < nTraits; i++) {
			traitScore[i] = payoff[i][me];
			myScore += traitCount[i] * payoff[me][i];
		}
		return myScore;
	}

	@Override
	public void avgScores(double[] density, int n, double[] avgscores) {
		for (int i = 0; i < nTraits; i++)
			for (int j = 0; j < nTraits; j++)
				avgscores[i] += density[j] * payoff[i][j];
	}

	@Override
	public void mixedScores(int[] traitCount, double[] traitScore) {
		int m1 = -1;
		for (int i = 0; i < nTraits; i++) {
			m1 += traitCount[i];
			traitScore[i] = -payoff[i][i];
			for (int j = 0; j < nTraits; j++)
				traitScore[i] += traitCount[j] * payoff[i][j];
		}
		double im1 = 1.0 / m1;
		for (int i = 0; i < nTraits; i++)
			traitScore[i] *= im1;
	}

	@Override
	public boolean check() {
		if (payoff.length != nTraits) {
			// number of traits have changed
			initPayoffs(); // make sure payoff matrix is valid
			return true;
		}
		return false;
	}

	/**
	 * Get the payoff matrix.
	 * 
	 * @return the payoff matrix
	 */
	public double[][] getPayoffs() {
		return payoff;
	}

	/**
	 * Set the payoff matrix to {@code payoffs}.
	 * 
	 * @param payoff the payoff matrix
	 */
	public void setPayoffs(double[][] payoff) {
		this.payoff = payoff;
		payoffType = PAYOFF_MATRIX;
		initPayoffs();
	}

	/**
	 * Set the payoff matrix to predefined type:
	 * <dl>
	 * <dt>constant
	 * <dd>constant payoff matrix of all {@code 1.0}.
	 * <dt>unity
	 * <dd>identity matrix with all {@code 1.0} on diagonal and {@code 0.0}
	 * otherwise.
	 * <dt>random
	 * <dd>random payoffs in {@code (-1.0, 1.0)}.
	 * <dt>matrix
	 * <dd>if matrix dimensions provided through {@link #setPayoffs(double[][])} are
	 * valid nothing happens but if not this defaults to random.
	 * </dl>
	 */
	public void initPayoffs() {
		switch (payoffType) {
			case PAYOFF_CONST:
				payoff = new double[nTraits][nTraits];
				for (int i = 0; i < nTraits; i++)
					for (int j = 0; j < nTraits; j++)
						payoff[i][j] = 1.0;
				break;
			case PAYOFF_UNITY:
				payoff = new double[nTraits][nTraits];
				for (int i = 0; i < nTraits; i++)
					for (int j = 0; j < nTraits; j++)
						payoff[i][j] = (i == j ? 1.0 : 0.0);
				break;
			case PAYOFF_MATRIX:
				if (payoff != null && payoff.length == nTraits && payoff[0].length == nTraits)
					break;
				//$FALL-THROUGH$
			case PAYOFF_RANDOM:
				RNGDistribution rng = engine.getRNG();
				payoff = new double[nTraits][nTraits];
				for (int i = 0; i < nTraits; i++)
					for (int j = 0; j < nTraits; j++)
						payoff[i][j] = 2.0 * rng.random01() - 1.0;
				break;
			default:
		}
	}

	/*
	 * command line parsing stuff
	 */
	public final CLOption cloTraits = new CLOption("traits", "2", EvoLudo.catModule,
			"--traits <t>    number of traits", new CLODelegate() {
				@Override
				public boolean parse(String arg) {
					setNTraits(CLOParser.parseInteger(arg));
					return true;
				}

				@Override
				public void report(PrintStream output) {
					output.println("# traitcount:           " + getNTraits());
				}
			});
	public final CLOption cloPayoff = new CLOption("paymatrix", "const", EvoLudo.catModule,
			"--paymatrix <a11,a12,...,a1n;...;an1,an2,...,ann>  nxn payoff matrix", new CLODelegate() {
				@Override
				public boolean parse(String arg) {
					if (arg.startsWith("const")) {
						payoffType = PAYOFF_CONST;
						return true;
					}
					if (arg.startsWith("unit")) {
						payoffType = PAYOFF_UNITY;
						return true;
					}
					if (arg.startsWith("random")) {
						payoffType = PAYOFF_RANDOM;
						return true;
					}
					payoffType = PAYOFF_MATRIX;
					double[][] payMatrix = CLOParser.parseMatrix(arg);
					if (payMatrix == null) {
						logger.warning("invalid paymatrix (" + arg + ") - using '" + cloPayoff.getDefault() + "'");
						return false;
					}
					setPayoffs(payMatrix);
					return true;
				}

				@Override
				public void report(PrintStream output) {
					output.println("# paymatrix:            " + Formatter.format(getPayoffs(), 4));
				}
			});

	@Override
	public void collectCLO(CLOParser parser) {
		// prepare command line options
		parser.addCLO(cloTraits);
		parser.addCLO(cloPayoff);
		super.collectCLO(parser);
	}

	@Override
	public Model createSDE() {
		return new SDEN(engine);
	}
}
