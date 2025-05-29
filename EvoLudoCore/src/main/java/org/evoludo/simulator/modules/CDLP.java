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

import org.evoludo.math.Combinatorics;
import org.evoludo.simulator.EvoLudo;
import org.evoludo.util.CLOParser;
import org.evoludo.util.CLOption;
import org.evoludo.util.CLOption.CLODelegate;

/**
 * Cooperation in voluntary (non-linear) public goods interactions with peer
 * punishment.
 * 
 * @author Christoph Hauert
 */
public class CDLP extends CDL {

	/**
	 * The trait (and index) value of peer punishers.
	 */
	public static final int PUNISH = 3;

	/**
	 * The cost of peer punishment of non-contributors.
	 * 
	 * @see #leniencyCoop
	 * @see #leniencyLoner
	 */
	double costPeerPunish = 0.3;

	/**
	 * The fine for peer punishment of non-contributors.
	 * 
	 * @see #leniencyCoop
	 * @see #leniencyLoner
	 */
	double finePeerPunish = 1.0;

	/**
	 * The leniency of peer punishers towards cooperators ({@code 0}: full leninecy,
	 * {@code 1}: no leninecy). Punishment of cooperators applies only if they
	 * happen to find themselves in an interaction group that reveals them as
	 * second-order free riders. For example, a group including a cooperator, a peer
	 * punisher and a defector such that the peer punisher notices the failure of
	 * the cooperator to punish the defector(s). The default is full leniency.
	 */
	double leniencyCoop = 0.0;

	/**
	 * The leniency of peer punishers towards loners ({@code 0}: full leninecy,
	 * {@code 1}: no leninecy). The default is full leniency.
	 */
	double leniencyLoner = 0.0;

	/**
	 * Create a new instance of the module for voluntary public goods games with
	 * peer punishment.
	 * 
	 * @param engine the manager of modules and pacemaker for running the model
	 */
	public CDLP(EvoLudo engine) {
		super(engine);
	}

	@Override
	public void load() {
		super.load(); // CDL sets names and colors already but no harm in it
		nTraits = 4;
		// trait names
		String[] names = new String[nTraits];
		names[LONER] = "Loner";
		names[DEFECT] = "Defector";
		names[COOPERATE] = "Cooperator";
		names[PUNISH] = "Punisher";
		setTraitNames(names);
		// trait colors (automatically generates lighter versions for new traits)
		Color[] colors = new Color[nTraits];
		// yellow has too little contrast
		colors[LONER] = new Color(238, 204, 17); // hex #eecc11
		colors[DEFECT] = Color.RED;
		colors[COOPERATE] = Color.BLUE;
		colors[PUNISH] = Color.GREEN;
		setTraitColors(colors);
	}

	@Override
	public String getAuthors() {
		return "Christoph Hauert";
	}

	@Override
	public String getTitle() {
		return "Punishment in voluntary public goods games";
	}

	@Override
	public double getMinPayoff() {
		double min = super.getMinPayoff();
		int[] sample = new int[nTraits];
		double[] traitScores = new double[nTraits];

		if (nGroup > 2) {
			// one cooperator, one defector and many punishers - requires nGroup>2
			sample[COOPERATE] = 1;
			sample[DEFECT] = 1;
			sample[PUNISH] = nGroup - 2;
			groupScores(sample, traitScores);
			min = Math.min(min, traitScores[COOPERATE]);
			// one punisher, one defector and many cooperators - requires nGroup>2
			sample[COOPERATE] = nGroup - 2;
			sample[DEFECT] = 1;
			sample[PUNISH] = 1;
			groupScores(sample, traitScores);
			min = Math.min(min, traitScores[PUNISH]);
		}
		// one defector and many punishers
		sample[COOPERATE] = 0;
		sample[DEFECT] = 1;
		sample[PUNISH] = nGroup - 1;
		groupScores(sample, traitScores);
		min = Math.min(min, traitScores[DEFECT]);
		// one punisher and many defectors
		sample[DEFECT] = nGroup - 1;
		sample[PUNISH] = 1;
		groupScores(sample, traitScores);
		min = Math.min(min, traitScores[PUNISH]);
		return min;
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * Note, monomorphic populations of peer punishers have the same payoff as
	 * monomorphic populations of cooperators.
	 */
	@Override
	public double getMonoPayoff(int type) {
		return super.getMonoPayoff(type == PUNISH ? COOPERATE : type);
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * <strong>Note:</strong> Leniency with cooperators (and punishing cooperators
	 * that failed to
	 * punish defectors) does not matter in pairwise interactions because this
	 * requires at least groups of three or more players. For example a cooperator,
	 * a defector and a punisher interact. In such a group composition the
	 * cooperator reveals the fact that it does not punish the defector
	 * (second-order free riding) and in turn may get punished by the punisher.
	 */
	@Override
	public double pairScores(int me, int[] traitCount, double[] traitScore) {
		double mypayoff = super.pairScores(me == PUNISH ? COOPERATE : me, traitCount, traitScore);
		int w = traitCount[PUNISH];
		traitScore[PUNISH] = traitScore[COOPERATE];

		switch (me) {
			case LONER:
				traitScore[PUNISH] -= leniencyLoner * costPeerPunish;
				return mypayoff + w * (payLoner - leniencyLoner * finePeerPunish);

			case COOPERATE:
				return mypayoff + w * traitScore[COOPERATE];

			case DEFECT:
				traitScore[PUNISH] -= costPeerPunish;
				return mypayoff + w * (traitScore[DEFECT] - finePeerPunish);

			case PUNISH:
				traitScore[LONER] -= leniencyLoner * finePeerPunish;
				traitScore[DEFECT] -= finePeerPunish;
				// in pairwise interactions cooperators never get fined because their
				// failure to punish never gets revealed.
				traitScore[PUNISH] -= leniencyLoner * costPeerPunish;
				return mypayoff + w * traitScore[PUNISH]
						- (traitCount[LONER] * leniencyLoner + traitCount[DEFECT]) * costPeerPunish;

			default: // should not end here
				throw new Error("Unknown trait (" + me + ")");
		}
	}

	@Override
	public void groupScores(int[] traitCount, double[] traitScore) {
		int x = traitCount[COOPERATE];
		int y = traitCount[DEFECT];
		int z = traitCount[LONER];
		int w = traitCount[PUNISH];
		int n = x + y + w;

		traitScore[LONER] = payLoner;
		if (n < 2) {
			traitScore[COOPERATE] = payLoneCoop;
			traitScore[DEFECT] = payLoneDefect;
			traitScore[PUNISH] = payLoneCoop - z * leniencyLoner * costPeerPunish;
			traitScore[LONER] -= w * leniencyLoner * finePeerPunish;
			return;
		}

		// non-linear interest precludes use of super
		double shareC, shareD;
		if (othersOnly) {
			int k = Math.max(0, x + w - 1);
			shareC = k * costCoop * interest(k) / (n - 1);
			shareD = (x + w) * costCoop * interest(x + w) / (n - 1);
		} else {
			shareC = (x + w) * costCoop * interest(x + w) / n;
			shareD = shareC;
		}
		traitScore[COOPERATE] = shareC - costCoop;
		traitScore[DEFECT] = shareD - w * finePeerPunish;
		traitScore[PUNISH] = traitScore[COOPERATE] - z * leniencyLoner * costPeerPunish;
		if (y > 0) {
			// note: non-punishing cooperators reveal themselves only in the
			// presence of defectors
			traitScore[COOPERATE] -= w * leniencyCoop * finePeerPunish;
			traitScore[PUNISH] -= (y + x * leniencyCoop) * costPeerPunish;
		}
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * Proper sampling in finite populations - formulas for standard public goods
	 * interactions with private punishment:
	 * \[
	 * \begin{align}
	 * f_C =&amp; \sigma-\frac w{M-1}(N-1)\delta\beta\\
	 * f_D =&amp; B-\frac w{M-1}(N-1)\beta\\
	 * f_C =&amp; B-F(z)c-w(N-1)G(y)\alpha\beta\\
	 * f_P =&amp; B-F(z)c-\frac y{M-1}(N-1)\gamma-x(N-1)G(y)\alpha\gamma-\frac
	 * z{M-1}(N-1)\delta\gamma
	 * \end{align}
	 * \]
	 * with
	 * \[
	 * \begin{align}
	 * B =&amp; \frac{\binom z{N-1}}{\binom{M-1}{N-1}}\sigma+r\frac
	 * {x+w}{M-z-1}\times\\
	 * \left(1-\frac 1{N(M-z)}\left( M-(z-N+1)\frac{\binom
	 * z{N-1}}{\binom{M-1}{N-1}}\right)\right) c\\
	 * F(z) =&amp; 1-\frac rN\frac{M-N}{M-z-1}+\frac{\binom
	 * z{N-1}}{\binom{M-1}{N-1}}
	 * \left(\frac rN \frac{z+1}{M-z-1}+r\frac{M-z-2}{M-z-1}-1\right)\\
	 * G(y) =&amp; \frac 1{M-1}-\frac
	 * 1{M-y-1}\frac{\binom{M-y-1}{N-1}}{\binom{M-1}{N-1}}
	 * \end{align}
	 * \]
	 */
	@Override
	public void mixedScores(int[] count, int n, double[] traitScores) {
		int x = count[COOPERATE];
		int y = count[DEFECT];
		int z = count[LONER];
		int w = count[PUNISH];
		int m = x + y + z + w;
		int m1 = m - 1;

		// merge punishers and cooperators to calculate public goods payoffs
		count[COOPERATE] += w;
		// this takes care of the non-linear interest as well as standard or other's
		// only public goods interactions
		super.mixedScores(count, n, traitScores);
		count[COOPERATE] -= w;

		// if all or all but one are loners, payoffs are trivial
		traitScores[PUNISH] = traitScores[COOPERATE];
		if (z >= m1) {
			if (leniencyLoner > 0.0 && w > 0) {
				// single punisher with no/some leniency towards loners
				double n1m1l = (double) (n - 1) / (double) (m - 1) * leniencyLoner;
				traitScores[LONER] -= w * n1m1l * finePeerPunish;
				traitScores[PUNISH] -= z * n1m1l * costPeerPunish;
			}
			return;
		}
		double n1m1 = (double) (n - 1) / (double) (m - 1);
		double fineD = w * n1m1 * finePeerPunish;
		traitScores[DEFECT] -= fineD;
		traitScores[PUNISH] -= y * n1m1 * costPeerPunish;
		if (leniencyLoner > 0.0) {
			traitScores[LONER] -= fineD * leniencyLoner;
			traitScores[PUNISH] -= z * n1m1 * leniencyLoner * costPeerPunish;
		}
		if (leniencyCoop > 0.0 && y > 0) {
			double px = n1m1;
			if (m - y >= n - 1) {
				int my1 = m - y - 1;
				// yn2 = Binomial[m-y-1, n-1]/Binomial[m-1, n-1]
				double yn2 = Combinatorics.combinationFrac(my1, m1, n - 1);
				px -= (n - 1) * yn2 / my1;
			}
			traitScores[COOPERATE] -= w * px * leniencyCoop * finePeerPunish;
			traitScores[PUNISH] -= x * px * leniencyCoop * costPeerPunish;
		}
	}

	@Override
	public void avgScores(double[] density, int n, double[] avgscores) {
		double x = density[COOPERATE];
		double y = density[DEFECT];
		double z = density[LONER];
		double w = density[PUNISH];

		// merge punishers and cooperators to calculate public goods payoffs
		density[COOPERATE] += w;
		// this takes care of the non-linear interest as well as standard or other's
		// only public goods interactions
		super.avgScores(density, n, avgscores);
		density[COOPERATE] -= w;

		avgscores[PUNISH] = avgscores[COOPERATE];
		double peer = (n - 1) * w * finePeerPunish;
		double yn2 = Combinatorics.pow(y, n - 2);
		avgscores[COOPERATE] -= leniencyCoop * (1.0 - yn2) * peer;
		avgscores[DEFECT] -= peer;
		avgscores[LONER] -= leniencyLoner * peer;
		avgscores[PUNISH] -= (leniencyCoop * (n - 1) * x * (1.0 - yn2) + leniencyLoner * (n - 1) * z + y * (n - 1))
				* costPeerPunish;
	}

	/**
	 * Set the cost of peer punishment of non-contributors.
	 * 
	 * @param aValue the cost of cooperation.
	 */
	public void setPunishCost(double aValue) {
		costPeerPunish = aValue;
	}

	/**
	 * Get the cost of peer punishment of non-contributors.
	 * 
	 * @return the cost of peer punishment.
	 */
	public double getPunishCost() {
		return costPeerPunish;
	}

	/**
	 * Set the fine peer punishment for non-contributors.
	 * 
	 * @param aValue the peer punishment fine.
	 */
	public void setPunishFine(double aValue) {
		finePeerPunish = aValue;
	}

	/**
	 * Get the peer punishment fine to non-contributors.
	 * 
	 * @return the peer punishment fine.
	 */
	public double getPunishFine() {
		return finePeerPunish;
	}

	/**
	 * Set the leniency towards cooperators, provided the composition of the
	 * interaction group reveals them as second-order free riders.
	 * 
	 * @param aValue the leniency towards cooperators.
	 * 
	 * @see #leniencyCoop
	 */
	public void setLeniencyCoop(double aValue) {
		leniencyCoop = aValue;
	}

	/**
	 * Get the leniency towards cooperators, provided the composition of the
	 * interaction group reveals them as second-order free riders.
	 * 
	 * @return the leniency towards cooperators.
	 * 
	 * @see #leniencyCoop
	 */
	public double getLeniencyCoop() {
		return leniencyCoop;
	}

	/**
	 * Set the leniency of punishment towards loners.
	 * 
	 * @param aValue the leniency towards loners.
	 * 
	 * @see #leniencyLoner
	 */
	public void setLeniencyLoner(double aValue) {
		leniencyLoner = aValue;
	}

	/**
	 * Get the leniency towards loners.
	 * 
	 * @return the leniency towards loners.
	 * 
	 * @see #leniencyLoner
	 */
	public double getLeniencyLoner() {
		return leniencyLoner;
	}

	/**
	 * Command line option to set the leniency of peer punishers towards
	 * cooperators, provided the composition of the interaction group reveals them
	 * as second-order free riders.
	 */
	public final CLOption cloLeniencyCooperators = new CLOption("leniencycoop", "0", EvoLudo.catModule,
			"--leniencycoop <l>  leniency for punishing cooperators", new CLODelegate() {

				/**
				 * {@inheritDoc}
				 * <p>
				 * Parse the leniency of peer punishers towards cooperators.
				 * 
				 * @param arg the leniency towards cooperators
				 */
				@Override
				public boolean parse(String arg) {
					setLeniencyCoop(CLOParser.parseDouble(arg));
					return true;
				}
			});

	/**
	 * Command line option to set the leniency of peer punishers towards loners.
	 */
	public final CLOption cloLeniencyLoners = new CLOption("leniencyloner", "0", EvoLudo.catModule,
			"--leniencyloner <l>  leniency for punishing loners", new CLODelegate() {

				/**
				 * {@inheritDoc}
				 * <p>
				 * Parse the leniency of peer punishers towards loners.
				 * 
				 * @param arg the leniency towards cooperators
				 */
				@Override
				public boolean parse(String arg) {
					setLeniencyLoner(CLOParser.parseDouble(arg));
					return true;
				}
			});

	/**
	 * Command line option to set the fine of peer punishment for non-contributors.
	 */
	public final CLOption cloPunishment = new CLOption("punishment", "1", EvoLudo.catModule,
			"--punishment <p>  punishment/fine", new CLODelegate() {

				/**
				 * {@inheritDoc}
				 * <p>
				 * Parse the fine of peer punishment for non-contributors.
				 * 
				 * @param arg the peer punishment fine
				 */
				@Override
				public boolean parse(String arg) {
					setPunishFine(CLOParser.parseDouble(arg));
					return true;
				}
			});

	/**
	 * Command line option to set the cost of peer punishment.
	 */
	public final CLOption cloCostPunish = new CLOption("costpunish", "0.3", EvoLudo.catModule,
			"--costpunish <c>  cost of punishment", new CLODelegate() {

				/**
				 * {@inheritDoc}
				 * <p>
				 * Parse the cost of peer punishment.
				 * 
				 * @param arg the cost of peer punishment
				 */
				@Override
				public boolean parse(String arg) {
					setPunishCost(CLOParser.parseDouble(arg));
					return true;
				}
			});

	@Override
	public void collectCLO(CLOParser parser) {
		// prepare command line options
		parser.addCLO(cloLeniencyCooperators);
		parser.addCLO(cloLeniencyLoners);
		parser.addCLO(cloPunishment);
		parser.addCLO(cloCostPunish);
		super.collectCLO(parser);
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * The parent class {@code CDL} admits kaleidoscopes. None have been identified
	 * for {@code CDLP} - use default IBS model.
	 */
	@Override
	public CDL.IBSPop createIBSPop() {
		return null;
	}
}
