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
import java.util.Arrays;

import org.evoludo.math.ArrayMath;
import org.evoludo.simulator.ColorMap;
import org.evoludo.simulator.EvoLudo;
import org.evoludo.simulator.Geometry;
import org.evoludo.simulator.Geometry.Type;
import org.evoludo.simulator.models.IBSDPopulation;
import org.evoludo.simulator.models.IBSGroup;
import org.evoludo.simulator.models.Model.HasIBS;
import org.evoludo.simulator.modules.Features.Payoffs;
import org.evoludo.simulator.views.HasHistogram;
import org.evoludo.simulator.views.HasMean;
import org.evoludo.simulator.views.HasPop2D;
import org.evoludo.simulator.views.HasPop3D;
import org.evoludo.util.CLODelegate;
import org.evoludo.util.CLOParser;
import org.evoludo.util.CLOption;
import org.evoludo.util.CLOCategory;
import org.evoludo.util.Formatter;

/**
 * Cooperation in dynamical networks.
 * 
 * @author Christoph Hauert
 */
public class NetGames extends Discrete implements Payoffs,
		HasIBS,
		HasPop2D.Traits, HasPop3D.Traits, HasMean.Traits, HasPop2D.Fitness,
		HasPop3D.Fitness, HasMean.Fitness, HasHistogram.Fitness, HasHistogram.Degree,
		HasHistogram.StatisticsStationary {

	/**
	 * The cost-to-benefit ratio of cooperation.
	 */
	double ratio = 1.0;

	/**
	 * The selection strength on differences in payoff or cooperativity.
	 */
	double selection = 1.0;

	/**
	 * The probability to pick a random model for comparison.
	 */
	double pRandomSample = 0.01;

	// // individual versus social learning
	// public final static int POPULATION_UPDATE_INDIVIDUAL = '-';
	// public final static int POPULATION_UPDATE_SOCIAL = '+';

	/**
	 * The color index of altruists.
	 */
	public static final int TYPE_ALTRUIST = 2;

	/**
	 * The color index of fair players.
	 */
	public static final int TYPE_FAIR = 1;

	/**
	 * The color index of egoists.
	 */
	public static final int TYPE_EGOIST = 0;

	/**
	 * The colors for altruists, fair players, and egoists.
	 */
	Color[] typeColor;

	/**
	 * Create a new instance of the module for cooperation in dynamical networks,
	 * where the network structure encodes the individuals behaviour.
	 * 
	 * @param engine the manager of modules and pacemaker for running the model
	 */
	public NetGames(EvoLudo engine) {
		super(engine);
		nTraits = 2; // cooperativity and activity
	}

	@Override
	public void load() {
		super.load();
		// trait names (optional)
		setTraitNames(new String[] { "Cooperativity", "Activity" });
		// type colors; different from trait colors because we have three types
		// (egoists, fair, altruists) but only two traits (cooperativity, activity)
		typeColor = new Color[] { Color.RED, Color.YELLOW, Color.GREEN };
	}

	@Override
	public void unload() {
		super.unload();
		typeColor = null;
	}

	@Override
	public String getKey() {
		return "NG";
	}

	@Override
	public String getAuthors() {
		return "Christoph Hauert & Lucas Wardil";
	}

	@Override
	public String getTitle() {
		return "Network Games";
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * Dynamical networks use a custom mapping for colors.
	 */
	@Override
	public <T> ColorMap<T> processColorMap(ColorMap<T> colorMap) {
		// Hack alert: originally this is an ColorMap.Index but we want a gradient
		if (colorMap instanceof ColorMap.Index) {
			ColorMap.Gradient1D<T> cMap1D = ((ColorMap.Index<T>) colorMap).toGradient1D(500);
			cMap1D.setGradient(
					new Color[] { ColorMap.addAlpha(typeColor[0], 220), //
							ColorMap.addAlpha(typeColor[1], 220), //
							ColorMap.addAlpha(typeColor[2], 220) });
			cMap1D.setRange(-1.0, 1.0);
			return cMap1D;
		}
		return colorMap;
	}

	@Override
	public double getMinPayoff() {
		// interaction may still be undefined
		if (interaction == null)
			return -1.0;
		interaction.evaluate();
		double min = interaction.minIn - interaction.maxOut * ratio;
		double max = interaction.maxIn - interaction.minOut * ratio;
		if (Math.abs(max - min) < 1e-10)
			return -1.0;
		return min;

	}

	@Override
	public double getMaxPayoff() {
		// interaction may still be undefined
		if (interaction == null)
			return 1.0;
		interaction.evaluate();
		double min = interaction.minIn - interaction.maxOut * ratio;
		double max = interaction.maxIn - interaction.minOut * ratio;
		if (Math.abs(max - min) < 1e-10)
			return 1.0;
		return max;
	}

	// @Override
	// public boolean getMeanTraits(double[] mean) {
	// interaction.evaluate();
	// mean[0] = interaction.avgTot; // avg. activity
	// mean[1] = interaction.avgOut; // avg. cooperativity
	// return true;
	// }

	// @Override
	// public boolean getMeanFitness(double[] meanscores) {
	// double[] performance = pop.performance;
	// meanscores[0] = performance[1]; // min
	// meanscores[1] = performance[2]; // max
	// meanscores[2] = performance[0]; // mean
	// return true;
	// }

	/**
	 * Set the cost-to-benefit ratio of cooperation.
	 * 
	 * @param ratio the new cost-to-benefit ratio
	 */
	public void setRatio(double ratio) {
		// costs could exceed benefits but then the getMax/MinGameScore methods need to
		// be adjusted
		this.ratio = Math.max(0.0, Math.min(1.0, ratio));
	}

	/**
	 * Get the cost-to-benefit ratio of cooperation.
	 * 
	 * @return the cost-to-benefit ratio
	 */
	public double getRatio() {
		return ratio;
	}

	/**
	 * Set the selection strength on differences in payoffs and cooperativity.
	 * 
	 * @param selection the new selection strength
	 */
	public void setSelection(double selection) {
		this.selection = Math.max(0.0, selection);
	}

	/**
	 * Get the selection strength on differences in payoffs and cooperativity.
	 * 
	 * @return the selection strength
	 */
	public double getSelection() {
		return selection;
	}

	/**
	 * Set the probability of picking a random model for comparison.
	 * 
	 * @param prob the new probability to pick a random model
	 */
	public void setRandomSampleProb(double prob) {
		pRandomSample = prob;
	}

	/**
	 * Get the probability of picking a random model for comparison.
	 *
	 * @return the probability to pick a random model
	 */
	public double getRandomSampleProb() {
		return pRandomSample;
	}

	/**
	 * Set the color of pure altruists (only help others, don't receive any help
	 * from others).
	 * 
	 * @param altruist the new color of altruists
	 */
	public void setAltruistColor(Color altruist) {
		typeColor[TYPE_ALTRUIST] = altruist;
		// note, resetting the GUI would be enough
		engine.requiresReset(true);
	}

	/**
	 * Get the color of pure altruists (only help others, don't receive any help
	 * from others).
	 * 
	 * @return the color of altruists
	 */
	public Color getAltruistColor() {
		return typeColor[TYPE_ALTRUIST];
	}

	/**
	 * Set the color of fair individuals (help the same number of individuals as
	 * help them, regardless of any potential reciprocity).
	 * 
	 * @param fair the new color of fair individuals
	 */
	public void setFairColor(Color fair) {
		typeColor[TYPE_FAIR] = fair;
		// note, resetting the GUI would be enough
		engine.requiresReset(true);
	}

	/**
	 * Get the color of fair individuals (help the same number of individuals as
	 * help them, regardless of any potential reciprocity).
	 *
	 * @return the color of fair individuals
	 */
	public Color getFairColor() {
		return typeColor[TYPE_FAIR];
	}

	/**
	 * Set the color of pure egoists (only receive help from others, don't provide
	 * any help to others).
	 *
	 * @param egoist the new color of egoists
	 */
	public void setEgoistColor(Color egoist) {
		typeColor[TYPE_EGOIST] = egoist;
		// note, resetting the GUI would be enough
		engine.requiresReset(true);
	}

	/**
	 * Get the color of pure egoists (only receive help from others, don't provide
	 * any help to others).
	 *
	 * @return the color of egoists
	 */
	public Color getEgoistColor() {
		return typeColor[TYPE_EGOIST];
	}

	/**
	 * Command line option to set the cost-to-benefit ratio of cooperation.
	 */
	public final CLOption cloRatio = new CLOption("ratio", "1", CLOCategory.Module,
			"--ratio <r>     cost-to-benefit ratio of cooperative act", new CLODelegate() {

				/**
				 * {@inheritDoc}
				 * <p>
				 * Parse the cost-to-benefit ratio of cooperation.
				 * 
				 * @param arg the cost-to-benefit ratio of cooperation
				 */
				@Override
				public boolean parse(String arg) {
					setRatio(CLOParser.parseDouble(arg));
					return true;
				}
			});

	/**
	 * Command line option to set the cost-to-benefit ratio of cooperation.
	 */
	public final CLOption cloSelection = new CLOption("selection", "1", CLOCategory.Module,
			"--selection <s>  selection strength", new CLODelegate() {

				/**
				 * {@inheritDoc}
				 * <p>
				 * Parse the cost-to-benefit ratio of cooperation.
				 * 
				 * @param arg the cost-to-benefit ratio of cooperation
				 */
				@Override
				public boolean parse(String arg) {
					setSelection(CLOParser.parseDouble(arg));
					return true;
				}
			});

	/**
	 * Command line option to set the colors for altruists, fair players, and
	 * egoists.
	 */
	public final CLOption cloColors = new CLOption("colors", "green;yellow;red", CLOCategory.GUI,
			"--colors <a[;f[;e]]>  colors for altruists, fair and egoists\n"
					+ "                <a:f:e>: color name or '(r,g,b[,a])' tuplet (r,g,b,a in 0-255)",
			new CLODelegate() {

				/**
				 * {@inheritDoc}
				 * <p>
				 * Parse the (array of) color(s) for altruists, fair players, and egoists. Up to
				 * three colors can be specified in the aforementioned sequence separated by
				 * {@value CLOParser.MATRIX_DELIMITER}. The colors can be specified by their
				 * names, as greyscale, RGB triplets or RGBA quadruplets.
				 * 
				 * @param arg the (array of) color(s)
				 * 
				 * @see CLOParser#parseColor(String)
				 */
				@Override
				public boolean parse(String arg) {
					String[] colorNames = arg.split(CLOParser.MATRIX_DELIMITER);
					switch (colorNames.length) {
						case 3:
							setEgoistColor(CLOParser.parseColor(colorNames[2]));
							//$FALL-THROUGH$
						case 2:
							setFairColor(CLOParser.parseColor(colorNames[1]));
							//$FALL-THROUGH$
						case 1:
							setAltruistColor(CLOParser.parseColor(colorNames[0]));
							return true;
						case 0:
							return false;
						default: // more than three colors - ignore but first three
							logger.warning("too many colors - ignored");
							return false;
					}
				}
			});

	@Override
	public void collectCLO(CLOParser parser) {
		super.collectCLO(parser);
		// make room for our own color option
		parser.removeCLO(cloTraitColors.getName());
		// prepare command line options
		parser.addCLO(cloRatio);
		parser.addCLO(cloSelection);
		// add our version of the --colors option
		parser.addCLO(cloColors);
	}

	@Override
	public void adjustCLO(CLOParser parser) {
		parser.removeCLO(new String[] { "popupdate", "playerupdate", "geometry", "geominter", "geomcomp",
				"mutation", "fitnessmap" });

		cloGeometry.clearKeys();
		cloGeometry.addKey(Type.DYNAMIC);
		cloGeometry.setDefault(Type.DYNAMIC.getKey());
		// parse default (and only option) for geometry
		cloGeometry.parse();

		// only graph dynamics allowed
		CLOption popt = getIBSPopulation().getPopulationUpdate().clo;
		popt.clearKeys();
		popt.addKey("individual", "individual learning - link to random members");
	}

	@Override
	public NetGames.IBSPop createIBSPopulation() {
		return new NetGames.IBSPop(engine, this);
	}

	/**
	 * The extension for IBS simulations implement cooperative actions on dynamical
	 * networks.
	 */
	public class IBSPop extends IBSDPopulation {

		/**
		 * The previous generation time.
		 */
		double prevgen = Double.MAX_VALUE;

		/**
		 * The mean fitness of individuals.
		 */
		double meanPerformance;

		/**
		 * The array with the mean, minimum, and maximum fitness of individuals.
		 */
		double[] performance;

		/**
		 * Create a new instance of the IBS model for cooperative actions on dynamical
		 * networks.
		 * 
		 * @param engine the pacemaker for running the model
		 * @param module the module that defines the game
		 */
		protected IBSPop(EvoLudo engine, NetGames module) {
			super(engine, module);
		}

		@Override
		public synchronized void reset() {
			super.reset();
			// free memory that is not needed (strategies not needed as well...)
			fitness = null;
			interactions = null;
			traitsNext = null;
			tags = null;
		}

		@Override
		public void init() {
			// do not call super
			if (performance == null || performance.length != 3)
				performance = new double[3];
			meanPerformance = 0.0;
			updateFitnessMean();
		}

		@Override
		public String getStatus() {
			return "Scores - mean: " + Formatter.formatFix(performance[0], 3) + " ("
					+ Formatter.formatFix(meanPerformance, 3) + ")" + ", min: " + Formatter.formatFix(performance[1], 3)
					+ ", max: " + Formatter.formatFix(performance[2], 3);
		}

		@Override
		public int step() {
			int focal = random0n(nPopulation);
			// switch( populationUpdateType ) {
			// // individual learning - preferentially link to prosperous members of
			// population
			// case POPULATION_UPDATE_INDIVIDUAL:
			updatePlayerIndividual(focal);
			// // social learning - preferentially link to prosperous friends of friends
			// case POPULATION_UPDATE_SOCIAL:
			// }
			updateFitnessMean();
			return 1;
		}

		/**
		 * Update the focal individual with index {@code focal} by probabilistically
		 * <ol>
		 * <li>removing links from more successful <em>and</em> less cooperative
		 * individuals,
		 * <li>adding links to more successful <em>and</em> more cooperative
		 * individuals.
		 * </ol>
		 * 
		 * @param focal the index of the individual to be updated
		 * 
		 * @see #removeCandidate(int, int)
		 * @see #addCandidate(int, int)
		 */
		protected void updatePlayerIndividual(int focal) {
			int k = interaction.kout[focal];
			// remove links from more successful _and_ less cooperative individuals
			if (k > 0) {
				int mdl = interaction.out[focal][random0n(k)];
				if (removeCandidate(focal, mdl))
					interaction.removeLinkAt(focal, mdl);
			}
			// add links to more successful _and_ more cooperative individuals
			int mdl = random0n(nPopulation - 1);
			if (mdl >= focal)
				mdl++;
			if (addCandidate(focal, mdl))
				interaction.addLinkAt(focal, mdl);
		}

		/**
		 * The focal individual with index {@code focal} considers acts of cooperation
		 * towards the candidate member with index {@code mdl} through a probabilistic
		 * comparison of their differences in fitness as well as in levels of
		 * cooperativity. Links are more likely established to more successful
		 * <em>and</em> more cooperative individuals.
		 * 
		 * @param focal the index of the focal individual
		 * @param mdl   the index of the candidate member
		 * @return {@code true} to establish the direct link from {@code focal} to
		 *         {@code mdl}
		 * 
		 * @see #getFitnessDiff(int, int)
		 * @see #getCooperativityDiff(int, int)
		 * @see #getProbability(double)
		 */
		protected boolean addCandidate(int focal, int mdl) {
			return (random01() < getProbability(getFitnessDiff(focal, mdl))
					* getProbability(getCooperativityDiff(focal, mdl)));
		}

		/**
		 * The focal individual with index {@code focal} considers to stop acts of
		 * cooperation towards the candidate member with index {@code mdl} through a
		 * probabilistic comparison of their differences in fitness as well as in levels
		 * of cooperativity. Links to more successful <em>and</em> less cooperative
		 * individuals are more likely removed.
		 *
		 * @param focal the index of the focal individual
		 * @param mdl   the index of the candidate member
		 * @return {@code true} to remove the direct link from {@code focal} to
		 *         {@code mdl}
		 */
		protected boolean removeCandidate(int focal, int mdl) {
			return (random01() < getProbability(getFitnessDiff(focal, mdl))
					* getProbability(-getCooperativityDiff(focal, mdl)));
		}

		/**
		 * Calculate the difference in fitness of individual with index {@code focal} as
		 * compared to the model individual with index {@code mdl}. The fitness
		 * difference is normalized to {@code [-1,1]} where negative differences refer
		 * to models that do worse and positive ones to models that perform better.
		 * 
		 * @param focal the index of the focal individual
		 * @param mdl   the index of the candidate member
		 * @return the normalized difference in fitness
		 */
		protected double getFitnessDiff(int focal, int mdl) {
			int kinf = interaction.kin[focal];
			int kinm = interaction.kin[mdl];
			int koutf = interaction.kout[focal];
			int koutm = interaction.kout[mdl];
			int totout = koutf + koutm;
			if (totout + kinf + kinm == 0)
				return 0.0;
			return (kinm - kinf - ratio * (koutm - koutf)) / (totout * ratio + kinm + kinf);
		}

		/**
		 * Calculate the difference in cooperativity of individual index {@code focal}
		 * as compared to the model individual with index {@code mdl}. The cooperativity
		 * difference is normalized to {@code [-1,1]} where negative differences refer
		 * to models that are less cooperative and positive ones to models that are more
		 * cooperative.
		 * 
		 * @param focal the index of the focal individual
		 * @param mdl   the index of the candidate member
		 * @return the normalized difference in cooperativity
		 */
		protected double getCooperativityDiff(int focal, int mdl) {
			int fout = interaction.kout[focal];
			int mout = interaction.kout[mdl];
			int tot = fout + mout;
			if (tot == 0)
				return 0.0;
			return (double) (mout - fout) / tot;
		}

		/**
		 * Converts the difference {@code z} into a probability following the Fermi
		 * function
		 * \[p = 1/(1+\exp(-w z)),\]
		 * where \(w\) denotes the selection strength.
		 * 
		 * @param z the difference to convert into a probability
		 * @return the probability
		 */
		protected double getProbability(double z) {
			return 1.0 / (2.0 + Math.expm1(-NetGames.this.getSelection() * z));
		}

		/**
		 * Update the mean, minimum, and maximum fitness of individuals.
		 */
		public void updateFitnessMean() {
			double gen = engine.getModel().getUpdates();
			double p = prevgen >= gen ? 0.0 : prevgen / gen;
			double q = 1.0 - p;
			prevgen = gen;
			double sum = 0.0;
			for (int n = 0; n < nPopulation; n++) {
				double c = interaction.kout[n] * ratio;
				double b = interaction.kin[n];
				if (b + c < 1e-10) {
					// loner node
					scores[n] = 0.0;
					continue;
				}
				double score = b - c;
				scores[n] = score;
				sum += score;
			}
			performance[0] = sum / nPopulation;
			performance[1] = ArrayMath.min(scores);
			performance[2] = ArrayMath.max(scores);
			meanPerformance = meanPerformance * p + performance[0] * q;
			if (Math.abs(performance[1] - performance[0]) < 1e-8) {
				performance[1] = -1.0;
				performance[2] = 1.0;
			}
		}

		@Override
		public double[] getMeanTraits(double[] mean) {
			interaction.evaluate();
			mean[0] = interaction.avgTot; // avg. activity
			mean[1] = interaction.avgOut; // avg. cooperativity
			return mean;
		}

		@Override
		public double[] getMeanFitness(double[] meanscores) {
			meanscores[0] = performance[1]; // min
			meanscores[1] = performance[2]; // max
			meanscores[2] = performance[0]; // mean
			return meanscores;
		}

		@Override
		public synchronized <T> void getFitnessData(T[] colors, ColorMap.Gradient1D<T> cMap) {
			T leafcolor = getLeafColor(colors[0], cMap);
			cMap.setRange(performance[1], performance[2]);
			for (int n = 0; n < nPopulation; n++) {
				if (interaction.kout[n] > 0 || interaction.kin[n] > 0) {
					colors[n] = cMap.translate(scores[n]);
					continue;
				}
				colors[n] = leafcolor;
			}
		}

		/**
		 * Utility method to determine the color of leaves in the network.
		 * 
		 * @param <T>  the type parameter of the color
		 * @param type the type of color to use
		 * @param cMap the color map to use
		 * @return the color of the leaf with type {@code T}
		 */
		private <T> T getLeafColor(T type, ColorMap<T> cMap) {
			T leafcolor;
			// Hack-alert: determine type of color map to use black for leaves in 2D
			// views but gray in 3D views.
			// Important: do not check for MeshLambertMaterial type because then JRE
			// will no longer compile.
			if (type == null || type instanceof String || type instanceof Color) {
				// must be color map for 2D visualizations (GWT or JRE)
				leafcolor = cMap.color2Color(ColorMap.addAlpha(Color.BLACK, 200));
			} else
				// must be color map for #D visualizations (GWT only)
				leafcolor = cMap.color2Color(ColorMap.addAlpha(Color.GRAY, 200));
			return leafcolor;
		}

		@Override
		public void getFitnessHistogramData(double[][] bins) {
			// clear bins
			Arrays.fill(bins[0], 0.0);
			Arrays.fill(bins[1], 0.0);
			int nBins = bins[0].length;
			maxScore = 1.0;
			minScore = -1.0;
			double mapPay = nBins / (maxScore - minScore);
			double mapCoop = nBins / (double) (nPopulation + nPopulation - 2);
			// fill bins
			int max = nBins - 1;
			for (int n = 0; n < nPopulation; n++) {
				double score;
				int p = interaction.kout[n];
				double b = p * ratio;
				int q = interaction.kin[n];
				if (b + q < 1e-10)
					score = 0.0;
				else
					score = (q - b) / (b + q);

				int bin = (int) ((score - minScore) * mapPay);
				bin = Math.max(0, Math.min(max, bin));
				bins[0][bin]++;

				bin = (int) ((p + q) * mapCoop);
				bin = Math.max(0, Math.min(max, bin));
				bins[1][bin]++;
			}
			ArrayMath.multiply(bins[0], 1.0 / nPopulation);
			ArrayMath.multiply(bins[1], 1.0 / nPopulation);
		}

		// @Override
		// public boolean parseGeometry(Geometry geom, String arg) {
		// if (arg.equals("*")) {
		// geom.setType(Type.DYNAMIC);
		// return true;
		// }
		// return false;
		// }

		// @Override
		// public boolean checkGeometry(Geometry geom) {
		// return geom.isType(Type.DYNAMIC);
		// }

		// @Override
		// public boolean generateGeometry(Geometry geom) {
		// if (geom.getType() != Type.DYNAMIC)
		// throw new UnsupportedOperationException("Unknown geometry (" + geom.getType()
		// + ")");
		// geom.isRewired = false;
		// geom.isUndirected = false;
		// geom.isRegular = false;
		// geom.setType(Geometry.Type.DYNAMIC);
		// geom.setSingle(true);
		// geom.alloc();
		// return true;
		// }

		@Override
		public void resetScores() {
			// updateFitnessMean always recalcuates scores from scratch
		}

		@Override
		public void updateScores() {
			// updateFitnessMean always recalcuates scores from scratch
		}

		@Override
		public void resetTraits() {
			// traits are unused and, instead, encoded in the network structure
		}

		@Override
		public void prepareTraits() {
			// traits are unused and, instead, encoded in the network structure
		}

		@Override
		public void commitTraits() {
			// traits are unused and, instead, encoded in the network structure
		}

		@Override
		public void commitTraitAt(int index) {
			// traits are unused and, instead, encoded in the network structure
		}

		@Override
		public boolean haveSameTrait(int a, int b) {
			return false;
		}

		@Override
		public boolean isSameTrait(int a) {
			return false;
		}

		@Override
		public void swapTraits(int a, int b) {
			// traits are unused and, instead, encoded in the network structure
		}

		@Override
		public void playPairGameAt(IBSGroup group) {
			// each connection represent an act of cooperation, no other games played
		}

		@Override
		public void playGroupGameAt(IBSGroup group) {
			// each connection represent an act of cooperation, no other games played
		}

		@Override
		public void playGameSyncAt(int idx) {
			// each connection represent an act of cooperation, no other games played
		}

		@Override
		public void yalpGroupGameAt(IBSGroup groupd) {
			// each connection represent an act of cooperation, no other games played
		}

		@Override
		public boolean updatePlayerBestResponse(int index, int[] group, int size) {
			return false;
		}

		@Override
		public boolean preferredPlayerBest(int me, int best, int sample) {
			return false;
		}

		@Override
		public String getTraitNameAt(int index) {
			return interaction.kin[index] + "-" + interaction.kout[index];
		}

		@Override
		public int getInteractionsAt(int idx) {
			return -1;
		}

		@Override
		public String getTagNameAt(int idx) {
			return null;
		}

		/**
		 * {@inheritDoc}
		 * <p>
		 * <strong>Note:</strong> with the help of <code>engine</code> (see
		 * {@link org.evoludo.simulator.EvoLudoGWT} and
		 * {@link org.evoludo.simulator.EvoLudoJRE}) the color specifics of GWT and
		 * JRE are shielded from interfering with shared code. Some careful checks and
		 * casts are needed to ensure the correct color map is used while preventing
		 * re-allocation of the custom color gradients.
		 */
		@Override
		public synchronized <T> void getTraitData(T[] colors, ColorMap<T> cMap) {
			if (!(cMap instanceof ColorMap.Gradient1D))
				// this should never happen
				return;
			ColorMap.Gradient1D<T> cMap1D = (ColorMap.Gradient1D<T>) cMap;
			T leafcolor = getLeafColor(colors[0], cMap);
			// cooperativity in [-1, 1]; could adjust range for higher resolution
			// cmt.setRange(-1.0, 1.0);
			for (int n = 0; n < nPopulation; n++) {
				int kout = interaction.kout[n];
				int kin = interaction.kin[n];
				if (kin + kout == 0) {
					// paint nodes black that are not part of a network
					colors[n] = leafcolor;
					continue;
				}
				double cooperativity = (double) (kout - kin) / (kout + kin);
				colors[n] = cMap1D.translate(cooperativity);
			}
		}
	}
}
