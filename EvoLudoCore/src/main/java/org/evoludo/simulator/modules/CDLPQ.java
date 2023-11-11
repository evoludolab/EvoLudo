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

import org.evoludo.math.Combinatorics;
import org.evoludo.simulator.EvoLudo;
import org.evoludo.util.CLOParser;
import org.evoludo.util.CLOption;
import org.evoludo.util.CLOption.CLODelegate;
import org.evoludo.util.Formatter;

/**
 * Cooperation in voluntary (non-linear) public goods interactions with peer and pool punishment.
 * 
 * @author Christoph Hauert
 */
public class CDLPQ extends CDLP {

	/**
	 * The trait (and index) value of pool punishers.
	 */
	public static final int SANCTIONING = 4;

	/**
	 * The cost of pool punishment.
	 * 
	 * @see #leniencyCoop
	 * @see #leniencyLoner
	 */
	double costPoolPunish = 0.3;

	/**
	 * The pool punishment fine for non-contributors.
	 * 
	 * @see #leniencyCoop
	 * @see #leniencyLoner
	 */
	double finePoolPunish = 1.0;

	/**
	 * Create a new instance of the module for voluntary public goods games with
	 * peer and pool punishment.
	 * 
	 * @param engine the manager of modules and pacemeaker for running the model
	 */
	public CDLPQ(EvoLudo engine) {
		super(engine);
	}

	@Override
	public void load() {
		super.load(); // CDL/CDLP sets names and colors already but no harm in it
		nTraits = 5;
		// trait names
		String[] names = new String[nTraits];
		names[LONER] = "Loner";
		names[DEFECT] = "Defector";
		names[COOPERATE] = "Cooperator";
		names[PUNISH] = "Peer-punisher";
		names[SANCTIONING] = "Sanctioner";
		setTraitNames(names);
		// trait colors (automatically generates lighter versions for new strategists)
		Color[] colors = new Color[nTraits];
		// yellow has too little contrast
		colors[LONER] = new Color(238, 204, 17);	// hex #eecc11
		colors[DEFECT] = Color.RED;
		colors[COOPERATE] = Color.BLUE;
		colors[PUNISH] = Color.GREEN;
		colors[SANCTIONING] = Color.MAGENTA;
		setTraitColors(colors);
	}

	@Override
	public String getKey() {
		return "CDLPQ";
	}

	@Override
	public String getInfo() {
		return "Title: " + getTitle() + "\nAuthor: Christoph Hauert.";
	}

	@Override
	public String getTitle() {
		return "Peer & pool punishment in voluntary public goods games";
	}

	@Override
	public String getVersion() {
		return "v1.0 March 2021";
	}

	@Override
	public double getMinGameScore() {
		double min = super.getMinGameScore();
		int[] sample = new int[nTraits];
		double[] traitScores = new double[nTraits];
		// one defector and many sanctioners
		sample[PUNISH] = 0;
		sample[SANCTIONING] = nGroup - 1;
		groupScores(sample, traitScores);
		min = Math.min(min, Math.min(traitScores[SANCTIONING], traitScores[DEFECT]));
		// one sanctioner and many defectors
		sample[DEFECT] = nGroup - 1;
		sample[SANCTIONING] = 1;
		groupScores(sample, traitScores);
		min = Math.min(min, Math.min(traitScores[SANCTIONING], traitScores[DEFECT]));
		return min;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * Note, monomorphic populations of pool punishers have the same payoff as
	 * monomorphic populations of cooperators.
	 */
	@Override
	public double getMonoGameScore(int type) {
		double mono = super.getMonoGameScore(type == PUNISH || type == SANCTIONING ? COOPERATE : type);
		if (type == SANCTIONING)
			return mono - costPoolPunish;
		return mono;
	}

	@Override
	public double pairScores(int me, int[] tCount, double[] tScore) {
		double mypayoff = super.pairScores(me == PUNISH || me == SANCTIONING ? COOPERATE : me, tCount, tScore);
		int v = tCount[SANCTIONING];
		tScore[SANCTIONING] = tScore[COOPERATE] - costPoolPunish;
		switch (me) {
			case LONER:
				return mypayoff + v * (payLoner - leniencyLoner * finePoolPunish);

			case COOPERATE:
				return mypayoff + v * tScore[COOPERATE];

			case DEFECT:
				return mypayoff + v * (tScore[DEFECT] - finePoolPunish);

			case PUNISH:
				return mypayoff + v * tScore[PUNISH];

			case SANCTIONING:
				return mypayoff + v * tScore[SANCTIONING];

			default: // should not end here
				throw new Error("Unknown strategy (" + me + ")");
		}
	}

	@Override
	public void groupScores(int[] tCount, double[] tScores) {
		int x = tCount[COOPERATE];
		int y = tCount[DEFECT];
		int z = tCount[LONER];
		int w = tCount[PUNISH];
		int v = tCount[SANCTIONING];
		int n = x + y + w + v;

		tScores[LONER] = payLoner;
		if (n < 2) {
			tScores[COOPERATE] = payLoneCoop;
			tScores[DEFECT] = payLoneDefect;
			tScores[PUNISH] = payLoneCoop - z * leniencyLoner * costPeerPunish;
			tScores[SANCTIONING] = payLoneCoop - costPoolPunish;
			tScores[LONER] -= leniencyLoner * (w * finePeerPunish + v * finePoolPunish);
			return;
		}

		// non-linear interest precludes use of super
		double shareC, shareD;
		if (othersOnly) {
			int k = Math.max(0, x + w + v - 1);
			shareC = k * costCoop * interest(k) / (n - 1);
			shareD = (x + w + v) * costCoop * interest(x + w + v) / (n - 1);
		} else {
			shareC = (x + w + v) * costCoop * interest(x + w + v) / n;
			shareD = shareC;
		}
		tScores[COOPERATE] = shareC - costCoop;
		tScores[DEFECT] = shareD;
		tScores[PUNISH] = tScores[COOPERATE];
		tScores[SANCTIONING] = tScores[COOPERATE];

		// peer-punishment
		tScores[DEFECT] -= w * finePeerPunish;
		tScores[PUNISH] -= y * costPeerPunish;
		if (y > 0) {
			// second-order free-riders are revealed by the presence of defectors and face
			// the wrath of peer-punishers
			tScores[COOPERATE] -= w * leniencyCoop * finePeerPunish;
			tScores[PUNISH] -= x * leniencyCoop * costPeerPunish;
		}

		// sanctioning: cooperators and peer-punishers fully count but punishment less
		// effective - provided that alpha>0
		/*
		 * old version where punishment pool get divided among all culprits
		 * double culprits = (double)y;
		 * if( leniencyCoop>1e-6 )
		 * 		culprits += (double)(x+w);
		 * if( culprits>0.0 ) {
		 * 		double punit = (double)v/culprits*finePoolPunish;
		 * 		groupTypeScores[DEFECT] -= punit;
		 * 		groupTypeScores[COOPERATE] -= punit*leniencyCoop;
		 * 		groupTypeScores[PUNISH] -= punit*leniencyCoop;
		 * }
		 */
		tScores[LONER] -= v * leniencyLoner * finePoolPunish;
		tScores[DEFECT] -= v * finePoolPunish;
		tScores[COOPERATE] -= v * leniencyCoop * finePoolPunish;
		tScores[PUNISH] -= v * leniencyCoop * finePoolPunish;
		tScores[SANCTIONING] -= costPoolPunish;
	}

	@Override
	public void mixedScores(int[] count, int n, double[] traitScores) {
		int x = count[COOPERATE];
		int y = count[DEFECT];
		int z = count[LONER];
		int w = count[PUNISH];
		int v = count[SANCTIONING];
		int m = x + y + z + w + v;
		int m1 = m - 1;

		// merge sanctioners and cooperators to calculate public goods payoffs
		// note: punishers will be merged as well in super
		count[COOPERATE] += v;
		// this takes care of the non-linear interest as well as standard or other's
		// only public goods interactions
		super.mixedScores(count, n, traitScores);
		count[COOPERATE] -= v;

		double n1m1 = (double) (n - 1) / (double) (m - 1);
		double punish = v * n1m1 * finePoolPunish;
		traitScores[LONER] -= punish * leniencyLoner;
		traitScores[SANCTIONING] = traitScores[COOPERATE] - costPoolPunish;
		// if all or all but one are loners, payoffs are trivial and we are done
		if (z >= m1)
			return;

		traitScores[DEFECT] -= punish;
		traitScores[COOPERATE] -= punish * leniencyCoop;
		traitScores[PUNISH] -= punish * leniencyCoop;
	}

	@Override
	public void avgScores(double[] density, int n, double[] avgscores) {
		double y = density[DEFECT];
		double w = density[PUNISH];
		double v = density[SANCTIONING];

		// merge sanctioners and cooperators to calculate public goods payoffs
		// note: punishers will be merged as well in super
		density[COOPERATE] += v;
		// this takes care of the non-linear interest as well as standard or other's
		// only public goods interactions
		super.avgScores(density, n, avgscores);
		density[COOPERATE] -= v;

		// unfortunately punishers spoiled the contributor score
		double peer = (n - 1) * w * finePeerPunish;
		double yn2 = Combinatorics.pow(y, n - 2);
		avgscores[SANCTIONING] = avgscores[COOPERATE] + leniencyCoop * (1.0 - yn2) * peer - costPoolPunish;
		double pool = v * (n - 1) * finePoolPunish;
		avgscores[LONER] -= pool * leniencyLoner;
		avgscores[DEFECT] -= pool;
		avgscores[COOPERATE] -= pool * leniencyCoop;
		avgscores[PUNISH] -= pool * leniencyCoop;
	}

	/**
	 * Set the cost of pool punishment.
	 *
	 * @param aValue the cost of pool punishment
	 */
	public void setPoolPunishCost(double aValue) {
		costPoolPunish = aValue;
	}

	/**
	 * Get the cost of pool punishment.
	 * 
	 * @return the cost of pool punishment
	 */
	public double getPoolPunishCost() {
		return costPoolPunish;
	}

	/**
	 * Set the pool punishment fine.
	 * 
	 * @param aValue the pool punishment fine.
	 */
	public void setPoolPunishFine(double aValue) {
		finePoolPunish = aValue;
	}

	/**
	 * Get the fine for pool punishment.
	 * 
	 * @return the pool punishment fine.
	 */
	public double getPoolPunishFine() {
		return finePoolPunish;
	}

	/**
	 * Command line option to set the fine of pool punishment for non-contributors.
	 */
	public final CLOption cloPoolPunish = new CLOption("poolpunish", "1", EvoLudo.catModule,
			"--poolpunish    pool punishment", new CLODelegate() {

				/**
				 * {@inheritDoc}
				 * <p>
				 * Parse the fine of pool punishment for non-contributors.
				 * 
				 * @param arg the pool punishment fine
				 */
				@Override
				public boolean parse(String arg) {
					setPoolPunishFine(CLOParser.parseDouble(arg));
					return true;
				}

				@Override
				public void report(PrintStream output) {
					output.println("# poolpunishment:       " + Formatter.format(getPoolPunishFine(), 4));
				}
			});

	/**
	 * Command line option to set the cost of pool punishment.
	 */
	public final CLOption cloCostPoolPunish = new CLOption("costpoolpunish", "0.3", EvoLudo.catModule,
			"--costpoolpunish  cost of pool punishment", new CLODelegate() {

				/**
				 * {@inheritDoc}
				 * <p>
				 * Parse the cost of pool punishment.
				 * 
				 * @param arg the cost of pool punishment
				 */
				@Override
				public boolean parse(String arg) {
					setPoolPunishCost(CLOParser.parseDouble(arg));
					return true;
				}

				@Override
				public void report(PrintStream output) {
					output.println("# poolpunishmentcost:   " + Formatter.format(getPoolPunishCost(), 4));
				}
			});

	@Override
	public void collectCLO(CLOParser parser) {
		// prepare command line options
		parser.addCLO(cloPoolPunish);
		parser.addCLO(cloCostPoolPunish);

		super.collectCLO(parser);
	}
}