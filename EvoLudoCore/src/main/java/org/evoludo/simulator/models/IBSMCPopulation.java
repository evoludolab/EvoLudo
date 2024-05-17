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

package org.evoludo.simulator.models;

import java.util.Arrays;
import java.util.List;

import org.evoludo.simulator.ColorMap;
import org.evoludo.simulator.EvoLudo;
import org.evoludo.simulator.Geometry;
import org.evoludo.simulator.models.IBS.ScoringType;
import org.evoludo.simulator.models.IBSC.Init;
import org.evoludo.simulator.modules.Continuous;
import org.evoludo.simulator.modules.Mutation;
import org.evoludo.util.Formatter;
import org.evoludo.util.Plist;

/**
 * The core class for individual based simulations with <em>multiple</em>
 * continuous traits/strategies. Manages the strategies of the population, while
 * delegating the management of the population and individual fitness as well as
 * simulation steps to super. Note that some further optimizations and
 * simplifications are possible in the special case of a single continuous
 * trait/strategy, which is handled in the subclass {@link IBSCPopulation}.
 * 
 * @author Christoph Hauert
 * 
 * @see IBSPopulation
 * @see IBSCPopulation
 */
public class IBSMCPopulation extends IBSPopulation {

	/**
	 * The continuous module associated with this model.
	 * <p>
	 * <strong>Note:</strong> This deliberately hides {@link IBSPopulation#module}.
	 * The two variables point to the same object but this setup avoids unnecessary
	 * casts because only {@link Continuous} modules generate
	 * {@code IBSCPopulation}(s).
	 */
	@SuppressWarnings("hiding")
	protected Continuous module;

	/**
	 * For pairwise interaction modules {@code module==pairmodule} holds and
	 * {@code null} otherwise. Convenience field to reduce the number of
	 * (unnecessary) casts.
	 * 
	 * @see Continuous.Pairs
	 */
	protected Continuous.MultiPairs pairmodule;

	/**
	 * For group interaction modules {@code module==groupmodule} holds and
	 * {@code null} otherwise. Convenience field to reduce the number of
	 * (unnecessary) casts.
	 * 
	 * @see Continuous.Groups
	 */
	protected Continuous.MultiGroups groupmodule;

	/**
	 * The interaction partner/opponent of this population
	 * {@code opponent.getModule()==getModule().getOpponent()}. In intra-species
	 * interactions {@code opponent==this}. Convenience field.
	 * <p>
	 * <strong>Note:</strong> This deliberately hides
	 * {@link IBSPopulation#opponent}. The two variables point to the same object
	 * but this setup avoids unnecessary casts because only
	 * {@link org.evoludo.simulator.modules.Discrete Discrete} modules
	 * generate {@code IBSDPopulation}(s).
	 */
	@SuppressWarnings("hiding")
	IBSMCPopulation opponent;

	/**
	 * The mutation parameters.
	 */
	protected Mutation.Continuous mutation;

	/**
	 * Creates a population of individuals with multiple continuous traits for IBS
	 * simulations.
	 * 
	 * @param engine the pacemeaker for running the model
	 */
	public IBSMCPopulation(EvoLudo engine) {
		super(engine);
	}

	@Override
	public void load() {
		super.load();
		// deal with module cast - pairmodule and groupmodule have to wait because
		// nGroup requires parsing of command line options (see check())
		// important: cannot deal with casting shadowed opponent here because for
		// mutli-species modules all species need to be loaded first.
		module = (Continuous) super.module;
		mutation = module.getMutation();
		init = new Init((IBS) engine.getModel(), module.getNTraits());
	}

	@Override
	public void unload() {
		// free resources
		super.unload();
		traitMin = null;
		traitMax = null;
		strategies = null;
		strategiesScratch = null;
		myTrait = null;
		oldTrait = null;
		groupStrat = null;
		smallStrat = null;
		meantrait = null;
		inittrait = null;
		oldScores = null;
		module = null;
		pairmodule = null;
		groupmodule = null;
		mutation = null;
		init = null;
	}

	@Override
	public boolean checkConvergence() {
		// takes more than just the absence of mutations
		return false;
	}

	/**
	 * The array with the minimal values for each trait/strategy. Convenience
	 * variable.
	 * <p>
	 * <strong>Note:</strong> Internally traits are always scaled to
	 * <code>[0, 1]</code>.
	 * 
	 * @see Continuous#getTraitMin()
	 */
	protected double[] traitMin;

	/**
	 * Get the minima for all traits.
	 *
	 * @return the array with the trait minima
	 */
	public double[] getTraitMin() {
		return traitMin;
	}

	/**
	 * The array with the maximal values for each trait/strategy. Convenience
	 * variable.
	 * <p>
	 * <strong>Note:</strong> Internally traits are always scaled to
	 * <code>[0, 1]</code>.
	 * 
	 * @see Continuous#getTraitMin()
	 */
	protected double[] traitMax;

	/**
	 * Get the maxima for all traits.
	 *
	 * @return the array with the trait maxima
	 */
	public double[] getTraitMax() {
		return traitMax;
	}

	/**
	 * The array of individual traits/strategies. The traits of individual {@code i}
	 * is stored at {@code strategies[i * nTraits]} through
	 * {@code strategies[(i + 1) * nTraits - 1]}
	 */
	public double[] strategies;

	/**
	 * The array for temporarily storing strategies during updates.
	 */
	protected double[] strategiesScratch;

	/**
	 * Temporary storage for strategies/traits of individuals in group interactions.
	 */
	protected double[] groupStrat;

	/**
	 * Temporary storage for strategies/traits of individuals in small sub-group
	 * interactions.
	 */
	protected double[] smallStrat;

	/**
	 * Temporary storage for the traits/strategies of the focal individual.
	 */
	protected double[] myTrait;

	/**
	 * Temporary storage for the traits/strategies of the focal individual before
	 * the update. Used for adjusting scores.
	 */
	protected double[] oldTrait;

	/**
	 * Temporary storage for the scores of each strategy/type prior to the group
	 * interactions.
	 */
	protected double[] oldScores;

	@Override
	public void updateFromModelAt(int index, int modelPlayer) {
		super.updateFromModelAt(index, modelPlayer); // deal with tags
		System.arraycopy(strategies, modelPlayer * nTraits, strategiesScratch, index * nTraits, nTraits);
	}

	@Override
	public boolean haveSameStrategy(int a, int b) {
		int idxa = a * nTraits;
		int idxb = b * nTraits;
		for (int i = 0; i < nTraits; i++)
			if (Math.abs(strategies[idxa + i] - strategies[idxb + i]) > 1e-8)
				return false;
		return true;
	}

	@Override
	public boolean isSameStrategy(int a) {
		int idxa = a * nTraits;
		for (int i = 0; i < nTraits; i++)
			if (Math.abs(strategies[idxa + i] - strategiesScratch[idxa + i]) > 1e-8)
				return false;
		return true;
	}

	@Override
	public void swapStrategies(int a, int b) {
		int idxa = a * nTraits;
		int idxb = b * nTraits;
		System.arraycopy(strategies, idxb, strategiesScratch, idxa, nTraits);
		System.arraycopy(strategies, idxa, strategiesScratch, idxb, nTraits);
	}

	@Override
	public double mutateAt(int focal) {
		updateScoreAt(focal, mutateAt(focal, false));
		return 1.0 / (nPopulation * module.getSpeciesUpdateRate());
	}

	@Override
	protected boolean maybeMutateAt(int focal, boolean switched) {
		if (!mutation.doMutate())
			return switched;
		return mutateAt(focal, switched);
	}

	@Override
	protected void maybeMutateMoran(int source, int dest) {
		updateFromModelAt(dest, source);
		if (mutation.doMutate())
			mutateAt(dest, true);
		updateScoreAt(dest, true);
	}

	/**
	 * Mutate all traits/strategies of the focal individual with index {@code focal}
	 * if {@code mutate == true}. In all cases commit strategies and update scores.
	 * 
	 * @param focal    the index of the focal individual that gets updated
	 * @param switched {@code true} if focal already switched trait
	 * @return {@code true} if the strategy has changed
	 */
	private boolean mutateAt(int focal, boolean switched) {
		int dest = focal * nTraits;
		double[] strat = switched ? strategiesScratch : strategies;
		for (int i = 0; i < nTraits; i++)
			strategiesScratch[dest + i] = mutation.mutate(strat[dest + i]);
		return true;
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * Here we introduce the convention the trait/strategy closer to {@code me} is
	 * preferred.
	 */
	@Override
	public boolean preferredPlayerBest(int me, int best, int sample) {
		double distmesample = deltaStrategies(me, sample);
		if (distmesample < 1e-8)
			return true;
		double distmebest = deltaStrategies(me, best);
		return (distmesample < distmebest);
	}

	/**
	 * Measure the (Cartesian) distance between strategies at {@code a} and
	 * {@code b}
	 * 
	 * @param a the index where the traits/strategies of the first individual start
	 * @param b the index where the traits/strategies of the second individual start
	 * @return the distance between {@code a} and {@code b}
	 */
	private double deltaStrategies(int a, int b) {
		if (nTraits == 1)
			return Math.abs(strategies[a] - strategies[b]);
		int idxa = a * nTraits;
		int idxb = b * nTraits;
		double dist = 0.0;
		for (int i = 0; i < nTraits; i++) {
			double d = strategies[idxa + i] - strategies[idxb + i];
			dist += d * d;
		}
		return Math.sqrt(dist);
	}

	/**
	 * Gather the traits/strategies of all individuals in the interaction group
	 * {@code group}.
	 * 
	 * @param group the interaction group
	 */
	private void gatherPlayers(IBSGroup group) {
		double[] oppstrategies = opponent.strategies;
		int oppntraits = opponent.nTraits;
		for (int i = 0; i < group.nSampled; i++)
			System.arraycopy(oppstrategies, group.group[i] * oppntraits, groupStrat, i * oppntraits, oppntraits);
		System.arraycopy(strategies, group.focal * nTraits, myTrait, 0, nTraits);
	}

	@Override
	protected boolean doAdjustScores() {
		switch (playerScoring) {
			case EPHEMERAL:
				// for ephemeral scoring, scores are never adjusted
				return false;
			case RESET_ALWAYS:
			default:
				// if resetting scores after every update, scores can be adjusted
				// when interacting all neighbours but not in well-mixed populations
				if (interaction.getType() == Geometry.Type.MEANFIELD || //
						(interaction.getType() == Geometry.Type.HIERARCHY && //
								interaction.subgeometry == Geometry.Type.MEANFIELD))
					return false;
				return interGroup.isSampling(IBSGroup.SamplingType.ALL);
			case RESET_ON_CHANGE:
				// if scores are reset only on an actual strategy change, scores
				// can never be adjusted
				return false;
		}
	}

	@Override
	public void adjustScoreAt(int index, double before, double after) {
		scores[index] = after;
		updateEffScoreRange(index, before, after);
		updateFitnessAt(index);
	}

	@Override
	public void adjustScoreAt(int index, double adjust) {
		double before = scores[index];
		double after = before + adjust;
		scores[index] = after;
		updateEffScoreRange(index, before, after);
		updateFitnessAt(index);
	}

	@Override
	public void playPairGameAt(IBSGroup group) {
		int me = group.focal;
		// for ephemeral scores calculate score of focal only
		boolean ephemeralScores = playerScoring.equals(ScoringType.EPHEMERAL);
		if (group.nSampled <= 0) {
			// isolated individual
			if (ephemeralScores) {
				// no need to update scores of everyone else
				resetScoreAt(me);
				return;
			}
			updateScoreAt(me, 0.0);
			return;
		}
		gatherPlayers(group);
		double myScore = pairmodule.pairScores(myTrait, groupStrat, group.nSampled,
				groupScores);
		if (ephemeralScores) {
			// no need to update scores of everyone else
			resetScoreAt(me);
			setScoreAt(me, myScore / group.nSampled, group.nSampled);
			return;
		}
		updateScoreAt(me, myScore, group.nSampled);
		for (int i = 0; i < group.nSampled; i++)
			opponent.updateScoreAt(group.group[i], groupScores[i]);
	}

	@Override
	public void adjustPairGameScoresAt(int me) {
		// gather players
		double[] oppstrategies = opponent.strategies;
		int oppntraits = opponent.nTraits;
		int nIn = 0, nOut = interaction.kout[me];
		int[] in = null, out = interaction.out[me];
		for (int n = 0; n < nOut; n++)
			System.arraycopy(oppstrategies, out[n] * oppntraits, groupStrat, n * oppntraits, oppntraits);
		int u2 = 2;
		if (!interaction.isUndirected) {
			// directed graph, count in-neighbors
			u2 = 1;
			nIn = interaction.kin[me];
			in = interaction.in[me];
			for (int n = 0; n < nIn; n++)
				System.arraycopy(oppstrategies, in[n] * oppntraits, groupStrat, (nOut + n) * oppntraits, oppntraits);
		}
		int nInter = nOut + nIn;
		int offset = me * nTraits;
		System.arraycopy(strategies, offset, oldTrait, 0, nTraits);
		System.arraycopy(strategiesScratch, offset, myTrait, 0, nTraits);
		double oldScore = pairmodule.pairScores(oldTrait, groupStrat, nInter, oldScores);
		double newScore = pairmodule.pairScores(myTrait, groupStrat, nInter, groupScores);
		commitStrategyAt(me);
		if (playerScoreAveraged) {
			double iInter = 1.0 / nInter;
			newScore *= iInter;
			oldScore *= iInter;
		}
		adjustScoreAt(me, oldScore, newScore);
		for (int n = 0; n < nOut; n++) {
			int you = out[n];
			double diff = groupScores[n] - oldScores[n];
			if (playerScoreAveraged)
				diff = u2 * diff / interactions[you];
			opponent.adjustScoreAt(you, diff);
		}
		// same as !interaction.isUndirected because in != null implies directed graph
		// (see above)
		if (in != null) {
			for (int n = 0; n < nIn; n++) {
				int you = in[n];
				double diff = groupScores[nOut + n] - oldScores[nOut + n];
				if (playerScoreAveraged)
					diff = u2 * diff / interactions[you];
				opponent.adjustScoreAt(you, diff);
			}
		}
	}

	@Override
	public void playGroupGameAt(IBSGroup group) {
		int me = group.focal;
		// for ephemeral scores calculate score of focal only
		boolean ephemeralScores = playerScoring.equals(ScoringType.EPHEMERAL);
		if (group.nSampled <= 0) {
			if (ephemeralScores) {
				resetScoreAt(me);
				return;
			}
			updateScoreAt(me, 0.0);
			return;
		}
		gatherPlayers(group);

		switch (group.samplingType) {
			case ALL:
				// interact with all neighbors
				int nGroup = module.getNGroup();
				if (nGroup < group.nSampled + 1) {
					// interact with part of group sequentially
					double myScore = 0.0;
					Arrays.fill(smallScores, 0, group.nSampled, 0.0);
					for (int n = 0; n < group.nSampled; n++) {
						for (int i = 0; i < nGroup - 1; i++)
							System.arraycopy(groupStrat, ((n + i) % group.nSampled) * nTraits, smallStrat, i * nTraits,
									nTraits);
						myScore += groupmodule.groupScores(myTrait, smallStrat, nGroup - 1, groupScores);
						if (ephemeralScores)
							continue;
						for (int i = 0; i < nGroup - 1; i++)
							smallScores[(n + i) % group.nSampled] += groupScores[i];
					}
					if (ephemeralScores) {
						resetScoreAt(me);
						setScoreAt(me, myScore / group.nSampled, group.nSampled);
						return;
					}
					updateScoreAt(me, myScore, group.nSampled);
					for (int i = 0; i < group.nSampled; i++)
						opponent.updateScoreAt(group.group[i], smallScores[i], nGroup - 1);
					return;
				}
				// interact with full group (random graphs, all neighbors)

				//$FALL-THROUGH$
			case RANDOM:
				// interact with sampled neighbors
				double myScore = groupmodule.groupScores(myTrait, groupStrat, group.nSampled, groupScores);
				if (ephemeralScores) {
					resetScoreAt(me);
					setScoreAt(me, myScore, 1);
					return;
				}
				updateScoreAt(me, myScore);
				for (int i = 0; i < group.nSampled; i++)
					opponent.updateScoreAt(group.group[i], groupScores[i]);
				return;

			default:
				throw new Error("Unknown interaction type (" + interGroup.getSampling() + ")");
		}
	}

	@Override
	public void yalpGroupGameAt(IBSGroup group) {
		double myScore;
		gatherPlayers(group);

		int nGroup = module.getNGroup();
		if (nGroup < group.nSampled + 1) { // interact with part of group sequentially
			myScore = 0.0;
			Arrays.fill(smallScores, 0, group.nSampled, 0.0);
			for (int n = 0; n < group.nSampled; n++) {
				for (int i = 0; i < nGroup - 1; i++)
					System.arraycopy(groupStrat, ((n + i) % group.nSampled) * nTraits, smallStrat, i * nTraits,
							nTraits);
				myScore += groupmodule.groupScores(myTrait, smallStrat, nGroup - 1, groupScores);
				for (int i = 0; i < nGroup - 1; i++)
					smallScores[(n + i) % group.nSampled] += groupScores[i];
			}
			removeScoreAt(group.focal, myScore, group.nSampled);
			for (int i = 0; i < group.nSampled; i++)
				opponent.removeScoreAt(group.group[i], smallScores[i], nGroup - 1);
			return;
		}
		// interact with full group (random graphs)
		myScore = groupmodule.groupScores(myTrait, groupStrat, group.nSampled, groupScores);
		removeScoreAt(group.focal, myScore);
		for (int i = 0; i < group.nSampled; i++)
			opponent.removeScoreAt(group.group[i], groupScores[i]);
	}

	@Override
	public void prepareStrategies() {
		System.arraycopy(strategies, 0, strategiesScratch, 0, nPopulation * nTraits);
	}

	@Override
	public void commitStrategies() {
		double[] swap = strategies;
		strategies = strategiesScratch;
		strategiesScratch = swap;
	}

	@Override
	public void commitStrategyAt(int me) {
		int idx = me * nTraits;
		System.arraycopy(strategiesScratch, idx, strategies, idx, nTraits);
	}

	@Override
	public synchronized <T> void getTraitData(T[] colors, ColorMap<T> colorMap) {
		colorMap.translate(strategies, colors);
	}

	/**
	 * Creates a histogram for each trait separately (if there are multiple) and
	 * returns the result in the array <code>bins</code> where the first index
	 * denotes the trait and the second refers to the bin.
	 *
	 * @param bins the array to store the histogram(s)
	 */
	public void getTraitHistogramData(double[][] bins) {
		// clear bins
		for (int n = 0; n < nTraits; n++)
			getTraitHistogramData(bins[n], n);
	}

	/**
	 * Creates a histogram for {@code trait} and returns the result in the array
	 * <code>bins</code>.
	 *
	 * @param bins the array to store the histogram(s)
	 */
	public void getTraitHistogramData(double[] bins, int trait) {
		// clear bins
		Arrays.fill(bins, 0.0);
		int nBins = bins.length;
		double scale = (nBins - 1);
		// fill bins
		for (int n = trait; n < nPopulation * nTraits; n += nTraits)
			// continuous strategies are stored in normalized form
			bins[(int) (strategies[n] * scale + 0.5)]++;
		double norm = 1.0 / nPopulation;
		for (int n = 0; n < nBins; n++)
			bins[n] *= norm;
	}

	/**
	 * Creates 2D histogram for traits <code>trait1</code> and <code>trait2</code>.
	 * The result is returned in the linear array <code>bins</code> and arranged in
	 * a way that is compatible with square lattice geometries for visualization by
	 * {@link org.evoludo.simulator.views.Distribution} and
	 * {@link org.evoludo.graphics.PopGraph2D} (GWT only).
	 *
	 * @param bins   the linear array to store the 2D histogram
	 * @param trait1 the index of the first trait
	 * @param trait2 the index of the second trait
	 *
	 * @see org.evoludo.simulator.Geometry#initGeometrySquare()
	 */
	public void get2DTraitHistogramData(double[] bins, int trait1, int trait2) {
		// clear bins
		Arrays.fill(bins, 0.0);
		double incr = 1.0 / nPopulation;
		if (nTraits == 1) {
			// ignore trait1 and trait2
			int size = bins.length;
			double scale = (size - 1);
			for (int n = 0; n < nPopulation; n++)
				// continuous strategies are stored in normalized form
				bins[(int) (strategies[n] * scale + 0.5)] += incr;
		} else {
			int size = (int) Math.sqrt(bins.length);
			double scale = (size - 1);
			// fill bins
			for (int n = 0; n < nPopulation * nTraits; n += nTraits)
				bins[(int) (strategies[n + trait2] * scale + 0.5) * size
						+ (int) (strategies[n + trait1] * scale + 0.5)] += incr;
		}
	}

	/**
	 * Gets all traits of all individuals. The traits are returned as a formatted
	 * string with an accuracy of {@code digits} decimals. With multiple traits they
	 * are listed sequentially for each individual.
	 * 
	 * @param digits the number of decimals of the formatted string
	 * @return the formatted traits
	 */
	public String getTraits(int digits) {
		return Formatter.format(strategies, digits);
	}

	@Override
	public String getTraitNameAt(int index) {
		String aName = "";
		int idx = index * nTraits;
		String[] names = module.getTraitNames();
		for (int i = 0; i < (nTraits - 1); i++) {
			aName += names[i] + " → "
					+ Formatter.format(strategies[idx + i] * (traitMax[i] - traitMin[i]) + traitMin[i], 3) + ", ";
		}
		aName += names[nTraits - 1] + " → " + Formatter.format(
				strategies[idx + nTraits - 1] * (traitMax[nTraits - 1] - traitMin[nTraits - 1]) + traitMin[nTraits - 1],
				3);
		return aName;
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * <strong>Note:</strong> For continuous modules the IBS model returns the
	 * concatenated mean and stdev of each trait in {@code traits}.
	 */
	@Override
	public void getInitialTraits(double[] traits) {
		System.arraycopy(inittrait, 0, traits, 0, 2 * nTraits);
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * For continuous traits/strategies the first {@code nTraits} entries represent
	 * the mean of each trait and the second {@code nTraits} entries denote the
	 * standard deviation.
	 */
	@Override
	public void getMeanTraits(double[] mean) {
		for (int i = 0; i < nTraits; i++) {
			int idx = i;
			double avg = 0.0, var = 0.0;
			for (int n = 1; n <= nPopulation; n++) {
				double aStrat = strategies[idx];
				double delta = aStrat - avg;
				avg += delta / n;
				var += delta * (aStrat - avg);
				idx += nTraits;
			}
			double scale = traitMax[i] - traitMin[i];
			double shift = traitMin[i];
			mean[i] = avg * scale + shift;
			mean[i + nTraits] = Math.sqrt(var / (nPopulation - 1)) * scale;
		}
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * For continuous traits/strategies the first {@code nTraits} entries represent
	 * the mean fitness of each trait and the second {@code nTraits} entries denote
	 * their standard deviation.
	 */
	@Override
	public void getMeanFitness(double[] mean) {
		double avg = 0.0, var = 0.0;
		for (int n = 0; n < nPopulation; n++) {
			double aScore = scores[n];
			double delta = aScore - avg;
			avg += delta / (n + 1);
			var += delta * (aScore - avg);
		}
		mean[0] = avg;
		mean[1] = Math.sqrt(var / (nPopulation - 1));
	}

	/**
	 * The array for calculating and storing the mean traits and their standard
	 * deviation. Must be of length {@code > 2 * nTraits}.
	 */
	private double[] meantrait;

	/**
	 * The array for storing the mean and standard deviation of the initial state.
	 * Must be of length {@code > 2 * nTraits}.
	 */
	private double[] inittrait;

	@Override
	public String getStatus() {
		getMeanTraits(meantrait);
		String[] names = module.getTraitNames();
		String status = names[0] + " mean: " + Formatter.formatFix(meantrait[0], 3) + " ± "
				+ Formatter.formatFix(meantrait[nTraits], 3);
		for (int i = 1; i < nTraits; i++)
			status += "; " + names[i] + " mean: " + Formatter.formatFix(meantrait[i], 3) + " ± "
					+ Formatter.formatFix(meantrait[i + nTraits], 3);
		return status;
	}

	@Override
	public boolean check() {
		boolean doReset = false;
		doReset |= super.check();
		// deal with casts once and for all
		if (this instanceof IBSCPopulation) {
			pairmodule = null;
			groupmodule = null;
		} else {
			if (module.isPairwise()) {
				pairmodule = (Continuous.MultiPairs) module;
				groupmodule = null;
			} else {
				pairmodule = null;
				// module may be just be Continuous...
				groupmodule = (module instanceof Continuous.MultiGroups ? (Continuous.MultiGroups) module : null);
			}
		}
		// IBSMCPopulation opponent shadows IBSPopulation opponent to save casts
		opponent = (IBSMCPopulation) super.opponent;

		traitMin = module.getTraitMin();
		traitMax = module.getTraitMax();

		// check interaction geometry
		if (interaction.getType() == Geometry.Type.MEANFIELD && interGroup.isSampling(IBSGroup.SamplingType.ALL)) {
			// interacting with everyone in mean-field simulations is not feasible - except
			// for discrete strategies
			logger.warning(
					"interaction type (" + interGroup.getSampling() + ") unfeasible in well-mixed populations!");
			interGroup.setSampling(IBSGroup.SamplingType.RANDOM);
			// change of sampling may affect whether scores can be adjusted
			adjustScores = doAdjustScores();
		}

		if (interaction.isInterspecies())
			logger.warning("multi-species interactions with continuous traits have NOT been tested...");

		if (module.getNGroup() > 2)
			logger.warning("group interactions with continuous traits have NOT been tested...");

		return doReset;
	}

	/**
	 * Type of initial configuration.
	 * 
	 * @see Init#clo
	 */
	protected Init init;

	/**
	 * Sets the type of the initial configuration and any accompanying arguments. If
	 * either {@code type} or {@code args} are {@code null} the respective current
	 * setting is preserved.
	 */
	public void setInit(Init init) {
		this.init = init;
	}

	/**
	 * Gets the type of the initial configuration and its arguments.
	 *
	 * @return the type and arguments of the initial configuration
	 */
	public Init getInit() {
		return init;
	}

	@Override
	public void init() {
		super.init();
		if (inittrait == null || inittrait.length != 2 * nTraits)
			inittrait = new double[2 * nTraits];
		int mutidx = -1;
		for (int s = 0; s < nTraits; s++) {
			switch (init.type) {
				default:
				case UNIFORM:
					for (int n = s; n < nPopulation * nTraits; n += nTraits)
						strategies[n] = random01();
					break;

				case MONO:
					// initArgs contains monomorphic trait
					double mono = Math.min(Math.max(init.args[s][0], traitMin[s]), traitMax[s]);
					double scaledmono = (mono - traitMin[s]) / (traitMax[s] - traitMin[s]);
					for (int n = s; n < nPopulation * nTraits; n += nTraits)
						strategies[n] = scaledmono;
					break;

				case GAUSSIAN:
					double mean = Math.min(Math.max(init.args[s][0], traitMin[s]), traitMax[s]);
					double sdev = Math.min(Math.max(init.args[s][1], traitMin[s]), traitMax[s]);
					double scaledmean = (mean - traitMin[s]) / (traitMax[s] - traitMin[s]);
					double scaledsdev = sdev / (traitMax[s] - traitMin[s]);
					for (int n = s; n < nPopulation * nTraits; n += nTraits)
						strategies[n] = Math.min(1.0, Math.max(0.0, randomGaussian(scaledmean, scaledsdev)));
					break;

				case MUTANT:
					double resident = Math.min(Math.max(init.args[s][0], traitMin[s]), traitMax[s]);
					double scaledresident = (resident - traitMin[s]) / (traitMax[s] - traitMin[s]);
					for (int n = s; n < nPopulation * nTraits; n += nTraits)
						strategies[n] = scaledresident;
					if (mutidx < 0)
						mutidx = random0n(nPopulation);
					double mut = Math.min(Math.max(init.args[s][1], traitMin[s]), traitMax[s]);
					strategies[mutidx] = (mut - traitMin[s]) / (traitMax[s] - traitMin[s]);
					break;
			}
		}
		// calculate mean and stdev of initial configuration
		getMeanTraits(inittrait);
	}

	@Override
	public synchronized void reset() {
		super.reset();
		if (strategies == null || strategies.length != nPopulation * nTraits)
			strategies = new double[nPopulation * nTraits];
		if (strategiesScratch == null || strategiesScratch.length != nPopulation * nTraits)
			strategiesScratch = new double[nPopulation * nTraits];
		if (myTrait == null || myTrait.length != nTraits)
			myTrait = new double[nTraits];
		// groupScores have the same maximum length
		int maxGroup = groupScores.length;
		if (groupStrat == null || groupStrat.length != maxGroup * nTraits)
			groupStrat = new double[maxGroup * nTraits];
		if (smallStrat == null || smallStrat.length != maxGroup * nTraits)
			smallStrat = new double[maxGroup * nTraits];
		if (meantrait == null || meantrait.length != 2 * nTraits)
			meantrait = new double[2 * nTraits];
		if (adjustScores) {
			if (oldTrait == null || oldTrait.length != nTraits)
				oldTrait = new double[nTraits];
			if (oldScores == null || oldScores.length != maxGroup)
				oldScores = new double[maxGroup];
		} else {
			oldTrait = null;
			oldScores = null;
		}
	}

	@Override
	public boolean mouseHitNode(int hit, boolean alt) {
		if (hit < 0 || hit >= nPopulation)
			return false; // invalid argument
		int offset = hit * nTraits;
		for (int n = 0; n < nTraits; n++)
			strategiesScratch[offset + n] = Math.min(Math.max(strategies[offset + n] + (alt ? -0.1 : 0.1), 0.0), 1.0);
		// update scores and display
		if (adjustScores) {
			adjustGameScoresAt(hit); // this also commits strategy
		} else {
			commitStrategyAt(hit);
			// when in doubt, recalculate entire board
			resetScores();
			updateScores();
		}
		engine.fireModelChanged();
		return true;
	}

	@Override
	public void encodeStrategies(StringBuilder plist) {
		plist.append(Plist.encodeKey("Configuration", strategies));
	}

	@Override
	public boolean restoreStrategies(Plist plist) {
		@SuppressWarnings("unchecked")
		List<Double> strat = (List<Double>) plist.get("Configuration");
		int size = nPopulation * nTraits;
		if (strat == null || strat.size() != size)
			return false;
		for (int n = 0; n < size; n++)
			strategies[n] = strat.get(n);
		return true;
	}
}
