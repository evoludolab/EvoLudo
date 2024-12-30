package org.evoludo.simulator.modules;

import java.awt.Color;
import java.io.PrintStream;

import org.evoludo.geom.Point2D;
import org.evoludo.math.ArrayMath;
import org.evoludo.simulator.ColorMap;
import org.evoludo.simulator.EvoLudo;
import org.evoludo.simulator.models.IBS.HasIBS;
import org.evoludo.simulator.views.HasMean;
import org.evoludo.simulator.views.HasPop2D;
import org.evoludo.simulator.views.HasPop3D;
import org.evoludo.simulator.views.HasS3;
import org.evoludo.simulator.views.S3Map;
import org.evoludo.util.CLOParser;
import org.evoludo.util.CLOption;
import org.evoludo.util.CLOption.CLODelegate;
import org.evoludo.util.Formatter;

public class Centipede extends Discrete implements HasIBS.DPairs,
		HasPop2D.Strategy, HasPop2D.Fitness, HasPop3D.Strategy, HasPop3D.Fitness,
		HasS3, HasMean.Strategy, HasMean.Fitness {

	// parameters
	int nNodes = 4;
	double cost = 1.0;
	double benefit = 3.0;

	// utility variables
	double[][] payFirst;
	double[][] paySecond;
	int nFirst;
	int nSecond;

	public Centipede(EvoLudo engine) {
		super(engine);
	}

	@Override
	public void load() {
		super.load();
		nTraits = 1;
		// trait names
		setTraitNames(new String[] { "(0,0)" });
		// trait colors
		setTraitColors(new Color[] { Color.BLACK });
	}

	@Override
	public String getKey() {
		return "CP";
	}

	@Override
	public String getInfo() {
		return "Title: " + getTitle() + "\nAuthor: JaGeLuSeCh\nCentipede game in single population.";
	}

	@Override
	public String getTitle() {
		return "Centipede game";
	}

	@Override
	public String getVersion() {
		return "v1.0 December 2023";
	}

	@Override
	public boolean check() {
		boolean doReset = super.check();
		nSecond = nNodes / 2 + 1;
		nFirst = (nNodes + 1) / 2 + 1;
		if (payFirst == null || payFirst.length != nFirst || payFirst[0].length != nSecond)
			payFirst = new double[nFirst][nSecond];
		if (paySecond == null || paySecond.length != nFirst || paySecond[0].length != nSecond)
			paySecond = new double[nFirst][nSecond];
		// fill in lookup tables for payoffs
		int nt = nFirst * nSecond;
		if (nt != nTraits) {
			nTraits = nt;
			// make sure all traits are active
			setActiveTraits(null);
			// note: option to disable traits is not available because module initially
			// advertises only a single trait
			doReset = true;
		}
		Color[] colors = new Color[nTraits];
		for (int i = 0; i < nFirst; i++) {
			int row = i * nFirst;
			for (int j = 0; j < nSecond; j++) {
				payFirst[i][j] = (j < i ? j * (benefit - cost) - cost : i * (benefit - cost));
				paySecond[i][j] = (i < j + 1 ? i * (benefit - cost) : j * (benefit - cost) + benefit);
				traitName[row + j] = "(" + i + "," + j + ")";
				colors[row + j] = ColorMap.addColors(
						ColorMap.blendColors(Color.BLACK, Color.RED, (double) (nFirst - i - 1) / (nFirst - 1)),
						ColorMap.blendColors(Color.BLACK, Color.GREEN, (double) (nSecond - j - 1) / (nSecond - 1)));
			}
		}
		setTraitColors(colors);
		return doReset;
	}

	@Override
	public int getNRoles() {
		return (nNodes == 4 ? 2 : 1);
	}

	@Override
	public S3Map getS3Map(int role) {
		if (nNodes != 4)
			return null;
		return new CentiMap(role);
	}

	@Override
	public Mutation.Discrete getMutation() {
		if (mutation == null)
			mutation = new CentiMutations(this);
		return mutation;
	}

	public double getPayFirst(int meTrait, int youTrait) {
		int meFirst = meTrait / nSecond;
		int youSecond = youTrait % nSecond;
		return payFirst[meFirst][youSecond];
	}

	public double getPaySecond(int meTrait, int youTrait) {
		int meSecond = meTrait % nSecond;
		int youFirst = youTrait / nSecond;
		return paySecond[youFirst][meSecond];
	}

	@Override
	public double pairScores(int me, int[] traitCount, double[] traitScore) {
		double myPay = 0.0;
		for (int n = 0; n < nTraits; n++) {
			myPay += traitCount[n] * getPayFirst(me, n);
			myPay += traitCount[n] * getPaySecond(me, n);
			traitScore[n] = getPayFirst(n, me);
			traitScore[n] += getPaySecond(n, me);
		}
		return myPay;
	}

	@Override
	public void mixedScores(int[] traitCount, double[] traitScores) {
		double inorm = 1.0 / (ArrayMath.norm(traitCount) - 1);
		for (int n = 0; n < nTraits; n++) {
			double sn = 0.0;
			for (int i = 0; i < nTraits; i++) {
				int ci = traitCount[i];
				if (n == i)
					ci--;
				sn += ci * (getPayFirst(n, i) + getPaySecond(n, i));
			}
			traitScores[n] = sn * inorm;
		}
	}

	@Override
	public double getMonoGameScore(int trait) {
		return getPayFirst(trait, trait) + getPaySecond(trait, trait);
	}

	@Override
	public double getMinGameScore() {
		return ArrayMath.min(payFirst) + ArrayMath.min(paySecond);
	}

	@Override
	public double getMaxGameScore() {
		return ArrayMath.max(payFirst) + ArrayMath.max(paySecond);
	}

	public void setCost(double cost) {
		this.cost = cost;
	}

	public double getCost() {
		return cost;
	}

	public void setBenefit(double benefit) {
		this.benefit = benefit;
	}

	public double getBenefit() {
		return benefit;
	}

	public void setNodes(int nNodes) {
		if (nNodes == this.nNodes)
			return;
		this.nNodes = nNodes;
		engine.requiresReset(true);
	}

	public double getNodes() {
		return nNodes;
	}

	/**
	 * Command line option to set the cost of cooperation, i.e. the costs incurring
	 * to the player when continuing the centipede game for another round.
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
					setCost(CLOParser.parseDouble(arg));
					return true;
				}

				@Override
				public void report(PrintStream output) {
					output.println("# cost:                 " + Formatter.format(getCost(), 4));
				}
			});

	/**
	 * Command line option to set the benefit of cooperation, i.e. the benefit
	 * provided to the opponent when continuing the centipede game for another
	 * round.
	 */
	public final CLOption cloBenefit = new CLOption("benefit", "3", EvoLudo.catModule,
			"--benefit <b>   benefit of cooperation", new CLODelegate() {

				/**
				 * {@inheritDoc}
				 * <p>
				 * Parse the benefit of cooperation.
				 * 
				 * @param arg the benefit of cooperation
				 */
				@Override
				public boolean parse(String arg) {
					setBenefit(CLOParser.parseDouble(arg));
					return true;
				}

				@Override
				public void report(PrintStream output) {
					output.println("# benefit:              " + Formatter.format(getBenefit(), 4));
				}
			});

	/**
	 * Command line option to set the maximum number of decision nodes for the
	 * centipede game.
	 */
	public final CLOption cloNodes = new CLOption("nodes", "4", EvoLudo.catModule,
			"--nodes <n>     number of decision nodes", new CLODelegate() {

				/**
				 * {@inheritDoc}
				 * <p>
				 * Parse the number of decision nodes.
				 * 
				 * @param arg the number of decision nodes
				 */
				@Override
				public boolean parse(String arg) {
					setNodes(CLOParser.parseInteger(arg));
					return true;
				}

				@Override
				public void report(PrintStream output) {
					output.println("# nodes:                " + getNodes());
				}
			});

	@Override
	public void collectCLO(CLOParser parser) {
		super.collectCLO(parser);
		parser.addCLO(cloCost);
		parser.addCLO(cloBenefit);
		parser.addCLO(cloNodes);
	}

	public class CentiMap extends S3Map {

		public CentiMap(int role) {
			super(role, role == 0 ? "First mover" : "Second mover");
			super.setNames(new String[] { "0", "1", "2" });
		}

		@Override
		public void setNames(String[] names) {
		}

		@Override
		public Color[] getColors() {
			if (getRole() == 0)
				return new Color[] { traitColor[0], traitColor[3], traitColor[6] };
			return new Color[] { traitColor[0], traitColor[1], traitColor[2] };
		}

		@Override
		public Point2D data2S3(double[] data, Point2D p) {
			int[] order = getOrder();
			double[] s3 = new double[3];
			// note: data[0] is time
			if (getRole() == 0) {
				s3[order[0]] = data[1] + data[2] + data[3];
				s3[order[1]] = data[4] + data[5] + data[6];
				s3[order[2]] = data[7] + data[8] + data[9];
			} else {
				s3[order[0]] = data[1] + data[4] + data[7];
				s3[order[1]] = data[2] + data[5] + data[8];
				s3[order[2]] = data[3] + data[6] + data[9];
			}
			return data2S3(s3[0], s3[1], s3[2], p);
		}
	}

	public class CentiMutations extends Mutation.Discrete {

		public CentiMutations(Module module) {
			super(module);
		}

		@Override
		public int mutate(int trait) {
			if (type == Type.NONE || nActive <= 1)
				// no mutations or only one active trait
				return trait;
			if (nTraits != nActive)
				throw new IllegalStateException("Inactive traits not supported");
			int meFirst = trait / nSecond;
			int meSecond = trait % nSecond;
			if (rng.random0n(2) == 0) {
				// mutate trait of first mover
				switch ((Type) type) {
					case ALL:
						return rng.random0n(nFirst) * nSecond + meSecond;
					case OTHER:
						return ((meFirst + rng.random0n(nFirst - 1) + 1) % nFirst) * nSecond + meSecond;
					case RANGE:
						int irange = (int) range;
						meFirst += rng.random0n(irange * 2 + 1) - irange;
						meFirst = Math.min(Math.max(meFirst, 0), nFirst - 1);
						return meFirst * nSecond + meSecond;
						// loop traits around
						// return ((meFirst + rng.random0n(irange * 2 + 1) - irange + nFirst) % nFirst) * nSecond
						// 		+ meSecond;
					default:
						return trait;
				}
			}
			// mutate trait of second mover
			switch ((Type) type) {
				case ALL:
					return meFirst * nSecond + rng.random0n(nSecond);
				case OTHER:
					return meFirst * nSecond + ((meSecond + rng.random0n(nSecond - 1) + 1) % nSecond);
				case RANGE:
					int irange = (int) range;
					meSecond += rng.random0n(irange * 2 + 1) - irange;
					meSecond = Math.min(Math.max(meSecond, 0), nSecond - 1);
					return meFirst * nSecond + meSecond;
					// loop traits around
					// return meFirst * nSecond
					// 		+ (meSecond + rng.random0n(irange * 2 + 1) - irange + nSecond) % nSecond;
				default:
					return trait;
			}
		}
	}
}