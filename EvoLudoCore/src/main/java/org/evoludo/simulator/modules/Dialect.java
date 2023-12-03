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

import org.evoludo.math.ArrayMath;
import org.evoludo.simulator.ColorMap;
import org.evoludo.simulator.EvoLudo;
import org.evoludo.simulator.EvoLudo.ColorModelType;
import org.evoludo.simulator.Geometry;
import org.evoludo.simulator.models.IBS.HasIBS;
import org.evoludo.simulator.models.IBSMCPopulation;
import org.evoludo.simulator.views.HasConsole;
import org.evoludo.simulator.views.HasDistribution;
import org.evoludo.simulator.views.HasHistogram;
import org.evoludo.simulator.views.HasMean;
import org.evoludo.simulator.views.HasPop2D;
import org.evoludo.simulator.views.HasPop3D;
import org.evoludo.util.CLOParser;
import org.evoludo.util.CLOption;
import org.evoludo.util.CLOption.CLODelegate;
import org.evoludo.util.Formatter;

/**
 *
 * @author Christoph Hauert
 */
public class Dialect extends Continuous implements Module.Static, HasIBS, //
		HasPop2D.Strategy, HasPop3D.Strategy, HasMean.Strategy, //
		HasHistogram.Strategy, HasDistribution.Strategy, HasHistogram.Degree, HasConsole {

	public static enum Dynamic {
		EW_MODEL("EW"), //
		XY_MODEL("XY"), //
		ADJUST("adjust");

		String id;

		Dynamic(String id) {
			this.id = id;
		}

		String getID() {
			return id;
		}
	}

	public Dynamic updateType = Dynamic.ADJUST;
	boolean traitsPeriodic = false;
	double invTemp = 1.0;
	double adjustment = 0.5;
	double maxAdjustment = -1.0;
	double pRandInteraction = 0.0;
	double randAdjustment = adjustment;
	double maxRandRepulsion = 0.0;

	private static final int ADJUST_NONE = -1;
	private static final int ADJUST_LINEAR = 0;
	private static final int ADJUST_EXP = 1;
	int adjusType = ADJUST_NONE;

	public Dialect(EvoLudo engine) {
		super(engine);
	}

	@Override
	public void load() {
		super.load();
		nTraits = 1;
	}

	@Override
	public String getKey() {
		return "Dialect";
	}

	@Override
	public String getInfo() {
		return "Title: " + getTitle() + "\nAuthor: Christoph Hauert\nLinguistic Evolution of Heterogeneity (Dialects)";
	}

	@Override
	public String getTitle() {
		return "Emergence of Dialects";
	}

	@Override
	public String getVersion() {
		return "v1.0 April 2021";
	}

	@Override
	public void reset() {
		super.reset();
		if (nTraits <= 3)
			setTraitColors(new Color[] { Color.RED, Color.GREEN, Color.BLUE });
		else
			setTraitColors(null);
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * The graphical depictions of the Edward-Wilkinson model uses a periodic color
	 * map of hues.
	 */
	@Override
	public <T> ColorMap<T> processColorMap(ColorMap<T> colorMap) {
		if (colorMap == null)
			return null;
		if (updateType == Dynamic.EW_MODEL) {
			((ColorMap.Hue<T>) colorMap).setHueRange(0.0, 1.0);
			return colorMap;
		}
		if (engine.getColorModelType() == ColorModelType.DISTANCE || nTraits > 3) {
			// allocate color map for distance
			ColorMap.Gradient1D<T> cMap1D = ((ColorMap.GradientND<T>) colorMap).toGradient1D(500);
			cMap1D.setGradient(
					new Color[] { ColorMap.addAlpha(Color.RED, 220), // close
						ColorMap.addAlpha(Color.YELLOW, 220), 
						ColorMap.addAlpha(Color.GRAY, 220),
						ColorMap.addAlpha(Color.WHITE, 220) });
			return cMap1D;
		}
		return colorMap;
	}

	@Override
	public void setNTraits(int nTraits) {
		if (this.nTraits != nTraits) {
			super.setNTraits(nTraits);
			setTraitNames(null);
			engine.requiresReset(true);
		}
	}

	// payoffs/scores make no sense here
	@Override
	public double getMinGameScore() {
		return 0.0;
	}

	@Override
	public double getMaxGameScore() {
		return 1.0;
	}

	/**
	 * Module.Static requires implementation of this. Dialect does not (yet) deal
	 * with payoffs - simply return an array of zero's to prevent any null pointer
	 * errors in upstream classes.
	 */
	@Override
	public double[] getStaticScores() {
		return new double[nTraits];
	}

	@Override
	protected void setExtremalScores() {
		extremalScoresSet = true;
	}

	public void setAdjustmentType(int type) {
		adjusType = type;
	}

	public void setDynamic(Dynamic type) {
		updateType = type;
	}

	public void setTraitsPeriodic(boolean periodic) {
		traitsPeriodic = periodic;
	}

	public void setAdjustment(double adj) {
		adjustment = adj;
	}

	public void setMaxAdjustment(double madj) {
		maxAdjustment = Math.max(Math.min(madj, 1.0), 0.0);
	}

	public void setRandomInteraction(double prnd) {
		pRandInteraction = Math.max(Math.min(prnd, 1.0), 0.0);
	}

	public void setRandomAdjustment(double rep) {
//XXX repulsion only against randomly picked individuals?
		randAdjustment = rep;
	}

	public void setMaxRandomRepulsion(double mrep) {
//XXX should repulsion be stronger for more similar types?
		maxRandRepulsion = Math.max(Math.min(mrep, 1.0), 0.0);
	}

	public void setXYTemperature(double temperature) {
		if (temperature < 1e-8) {
			invTemp = -1.0;
			return;
		}
		invTemp = 1.0 / temperature;
	}

	public final CLOption cloPhenoDim = new CLOption("phenodim", "2", EvoLudo.catModule,
			"--phenodim <d>     number of phenotype dimensions", new CLODelegate() {
				@Override
				public boolean parse(String arg) {
					setNTraits(CLOParser.parseInteger(arg));
					return true;
				}

				@Override
				public void report(PrintStream output) {
					output.println("# phenotypic dimension: " + getNTraits());
				}
			});
	public final CLOption cloPhenoPeriodic = new CLOption("phenoperiodic", "fixed", CLOption.Argument.NONE, EvoLudo.catModule,
			"--phenoperiodic    periodic phenotype space", new CLODelegate() {
				@Override
				public boolean parse(String arg) {
					setTraitsPeriodic(cloPhenoPeriodic.isSet());
					return true;
				}

				@Override
				public void report(PrintStream output) {
					output.println("# periodic phenotypes:  " + traitsPeriodic);
				}
			});
	public final CLOption cloPhenoAdjustType = new CLOption("phenoadjtype", "-1", EvoLudo.catModule,
			"--phenoadjtype <t> type of adjustment", new CLODelegate() {
				@Override
				public boolean parse(String arg) {
					setAdjustmentType(CLOParser.parseInteger(arg));
					return true;
				}

				@Override
				public void report(PrintStream output) {
					if (updateType == Dynamic.ADJUST)
						output.println("# adjustmenttype:       " + Formatter.format(adjusType, 4));
				}
			});
	public final CLOption cloPhenoUpdate = new CLOption("phenoupdate", "adjust", EvoLudo.catModule,
			"--phenoupdate <t>  update type of phenotype\n" + //
			"               XY: XY-model\n"	+ //
			"               EW: Edward-Wilkinson-model\n" + //
			"           adjust: adjustment towards model",
			new CLODelegate() {
				@Override
				public boolean parse(String arg) {
					for (Dynamic d : Dynamic.values()) {
						if (d.getID().equalsIgnoreCase(arg)) {
							setDynamic(d);
							return true;
						}
					}
					logger.warning(
							"unknown phenupdate type '" + arg + "' - using '" + cloPhenoUpdate.getDefault() + "'.");
					return false;
				}

				@Override
				public void report(PrintStream output) {
					output.println("# dynamic:              " + updateType.getID());
				}
			});
	public final CLOption cloXYTemp = new CLOption("xytemp", "1", EvoLudo.catModule,
			"--xytemp <t>       temperature in xy-model", new CLODelegate() {
				@Override
				public boolean parse(String arg) {
					setXYTemperature(CLOParser.parseDouble(arg));
					return true;
				}

				@Override
				public void report(PrintStream output) {
					if (updateType == Dynamic.XY_MODEL)
						output.println("# XY-temperature:       " + Formatter.format(1.0 / invTemp, 4));
				}
			});
	public final CLOption cloPhenoAdjust = new CLOption("phenoadj", "0.1", EvoLudo.catModule,
			"--phenoadj <a>     strength of adjustment towards model [0,1]", new CLODelegate() {
				@Override
				public boolean parse(String arg) {
					setAdjustment(CLOParser.parseDouble(arg));
					return true;
				}

				@Override
				public void report(PrintStream output) {
					if (updateType == Dynamic.ADJUST)
						output.println("# adjustment:           " + Formatter.format(adjustment, 4));
				}
			});
	public final CLOption cloPhenoMaxAdjust = new CLOption("phenomaxadj", "0.1", EvoLudo.catModule,
			"--phenomaxadj <a>  max adjustment, fraction of trait range", new CLODelegate() {
				@Override
				public boolean parse(String arg) {
					setMaxAdjustment(CLOParser.parseDouble(arg));
					return true;
				}

				@Override
				public void report(PrintStream output) {
					if (updateType == Dynamic.ADJUST)
						output.println("# maxadjustment:        " + Formatter.format(maxAdjustment, 4));
				}
			});
	public final CLOption cloPhenoRandInter = new CLOption("phenorandinter", "0", EvoLudo.catModule,
			"--phenorandinter <r> probability interacting with random member", new CLODelegate() {
				@Override
				public boolean parse(String arg) {
					setRandomInteraction(CLOParser.parseDouble(arg));
					return true;
				}

				@Override
				public void report(PrintStream output) {
					if (updateType == Dynamic.ADJUST && pRandInteraction > 0.0)
						output.println("# randinter:           " + Formatter.format(pRandInteraction, 4));
				}
			});
	public final CLOption cloPhenoRandAdjust = new CLOption("phenorandadj", "0.1", EvoLudo.catModule,
			"--phenorandadj <r> adjustment for random members", new CLODelegate() {
				@Override
				public boolean parse(String arg) {
					setRandomAdjustment(CLOParser.parseDouble(arg));
					return true;
				}

				@Override
				public void report(PrintStream output) {
					if (updateType == Dynamic.ADJUST && pRandInteraction > 0.0)
						output.println("# randadjust:           " + Formatter.format(randAdjustment, 4));
				}
			});
	public final CLOption cloPhenoMaxRandRepel = new CLOption("phenomaxrandrepel", "0.1", EvoLudo.catModule,
			"--phenomaxrandrepel <r> repulsive threshold, fraction of trait range", new CLODelegate() {
				@Override
				public boolean parse(String arg) {
					setMaxRandomRepulsion(CLOParser.parseDouble(arg));
					return true;
				}

				@Override
				public void report(PrintStream output) {
					if (updateType == Dynamic.ADJUST && pRandInteraction > 0.0)
						output.println("# maxrandrepel:        " + Formatter.format(maxRandRepulsion, 4));
				}
			});

	@Override
	public void collectCLO(CLOParser parser) {
		// prepare command line options
		parser.addCLO(cloPhenoDim);
		parser.addCLO(cloPhenoPeriodic);
		parser.addCLO(cloPhenoUpdate);
		parser.addCLO(cloXYTemp);
		parser.addCLO(cloPhenoAdjustType);
		parser.addCLO(cloPhenoAdjust);
		parser.addCLO(cloPhenoMaxAdjust);
		parser.addCLO(cloPhenoRandInter);
		parser.addCLO(cloPhenoRandAdjust);
		parser.addCLO(cloPhenoMaxRandRepel);
		super.collectCLO(parser);
	}

	@Override
	public void adjustCLO(CLOParser parser) {
		// remove options that do not make sense in present context
		parser.removeCLO(new String[] { "popupdate", "playerupdate", "costparams", "costfcn",
				"benefitparams", "benefitfcn", "fitnessmap" });
	}

	@Override
	public Dialect.IBS createIBSPop() {
		return new Dialect.IBS(engine);
	}

	public class IBS extends IBSMCPopulation {

		/**
		 * Create a new custom implementation for IBS simulations.
		 * 
		 * @param engine the manager of modules and pacemeaker for running the model
		 */
		protected IBS(EvoLudo engine) {
			super(engine);
		}

		@Override
		public boolean check() {
			boolean doReset = super.check();
			switch (updateType) {
				case EW_MODEL:
					if (nTraits != 1) {
						logger.warning("Edward-Wilkinson-model requires one phenotype dimension (changed to 1-dim).");
						setNTraits(1);
						doReset = true;
					}
					if (interaction.isType(Geometry.Type.HIERARCHY)) {
						logger.warning(
								"Edward-Wilkinson-model does not support hierarchical geometries (changed to von Neumann).");
						interaction.parse("n");
						doReset = true;
					}
					break;
				case XY_MODEL:
					// ensure one periodic phenotype dimension
					if (!traitsPeriodic) {
						logger.warning("XY-model requires periodic boundaries of phenotypes (changed to periodic).");
						setTraitsPeriodic(true);
						doReset = true;
					}
					if (nTraits != 1) {
						logger.warning("XY-model requires one phenotype dimension (changed to 1-dim).");
						setNTraits(1);
						doReset = true;
					}
					if (interaction.isType(Geometry.Type.HIERARCHY)) {
						logger.warning("XY-model does not support hierarchical geometries (changed to von Neumann).");
						interaction.parse("n");
						doReset = true;
					}
					break;
				case ADJUST:
				default:
					if (pRandInteraction <= 0.0) {
						// disable repulsion without random interactions
						pRandInteraction = 0.0;
						randAdjustment = -1.0;
						maxRandRepulsion = -1.0;
					}
					if (adjusType != ADJUST_NONE && adjusType != ADJUST_LINEAR && adjusType != ADJUST_EXP)
						adjusType = ADJUST_NONE;
					break;
			}
			return doReset;
		}
	
		// override default with more efficient version; cannot deal with multiple
		// species
		@Override
		public double step() {
			double gincr = 1.0 / nPopulation;
			int focal = random0n(nPopulation);
			switch (updateType) {
				case EW_MODEL:
					// stochastic version of Edward-Wilkinson model, see Family, F. (1986) J. Phys.
					// A: Math. Gen. 19, L441-L446
					// 'strategies' represent height of boundary layer
					int[] neighs = reproduction.out[focal];
					int nNeighs = neighs.length;
					int minIdx = focal;
					double minHeight = strategies[focal];
					for (int i = 0; i < nNeighs; i++) {
						int idx = neighs[i];
						double iHeight = strategies[idx];
						if (iHeight < minHeight) {
							minHeight = iHeight;
							minIdx = idx;
						}
					}
					// drop grain on location with minimal height in neighborhood of focal
					// (including focal)
					strategies[minIdx] += 1.0;
					break;
				case XY_MODEL:
					// assumes single, periodic trait
					double eNow = potentialAt(focal);
					// pick random spin
					double trial = random01();
					double eTrial = potentialAt(focal, trial);
					if (eTrial < eNow || (invTemp > 0 && random01() < Math.exp((eNow - eTrial) * invTemp))) {
						// if energy of new state is lower - adopt it; if it is higher, adopt with
						// probability based on energy difference
						strategies[focal] = trial;
					}
					break;
				case ADJUST:
				default:
					// adjust focal strategy towards strategy of randomly chosen model
					int mdl = pickRandomModel(focal);
					boolean randoModel = (mdl >= 0);
					if (!randoModel) {
						// pick random neighbour
						mdl = pickNeighborSiteAt(focal);
					}
					int midx = mdl * nTraits;
					int fidx = focal * nTraits;
					if (traitsPeriodic) {
						for (int d = 0; d < nTraits; d++) {
							double diff = strategies[midx + d] - strategies[fidx + d];
							// if diff > 0.5 then focal trait must be lowered to get closer
							if (diff > 0.5)
								diff -= 1.0;
							else
							// if diff < -0.5 then focal trait must be increased to get closer to model
							if (diff < -0.5)
								diff += 1.0;
							double adjust = calculateAdjustment(diff, randoModel, d);
							// newtrait may no longer be in [0, 1]
							// double newtrait = strategies[fidx+n]+diff*adjustment;
							double newtrait = strategies[fidx + d] + adjust;
							// add Gaussian noise
							double sdev = mutSdev[d];
							double mut;
							if (sdev > 0.0)
								mut = randomGaussian(newtrait, sdev);
							else
								mut = newtrait;
							mut += 1.0;
							mut -= Math.floor(mut);
							strategies[fidx + d] = mut;
						}
						break;
					}
					// reflective boundaries
					for (int d = 0; d < nTraits; d++) {
						double diff = strategies[midx + d] - strategies[fidx + d];
						double adjust = calculateAdjustment(diff, randoModel, d);
						double newtrait = strategies[fidx + d] + adjust;
						// double newtrait = strategies[fidx+n]+diff*adjustment;

						// add Gaussian noise, use reflective boundaries at [0, 1] (John Fairfield)
						double sdev = mutSdev[d];
						double mut;
						if (sdev > 0.0)
							mut = randomGaussian(newtrait, sdev);
						else
							mut = newtrait;
						mut = Math.abs(mut);
						if (mut > 1.0)
							mut = 2 - mut;
						strategies[fidx + d] = mut;
						// without mutations simulations are significantly faster...
						// strategies[fidx+n] = newtrait;
					}
			}
			return gincr;
		}

		private double calculateAdjustment(double diff, boolean randoModel, int trait) {
			double adjust;
			switch (adjusType) {
				case ADJUST_LINEAR:
					if (randoModel) {
						// repulsion if difference less than threshold maxRandRepulsion;
						// more similar models result in stronger repulsion
						adjust = Math.signum(diff) * (Math.abs(diff) - maxRandRepulsion) * randAdjustment;
					} else {
						// attraction
						adjust = diff * adjustment;
					}
					if (maxAdjustment > 0.0 && Math.abs(adjust) > maxAdjustment) {
						adjust = maxAdjustment * Math.signum(adjust);
					}
					break;

				case ADJUST_EXP:
					// adjust = diff*Math.exp(-adjustment*Math.abs(diff));
					// double shift = (traitMax[trait]-traitMin[trait])*0.5;
					// NOTE:
					// traits are normalized to [0, 1] (and not to [traitMin, traitMax])
					// diff = modeltrait-focaltrait with a range of [-1, 1]
					// adjustment>0 large effort for different models
					// adjustment<0 large effort for similar models
					// adjustment=0 always go half way towards model
					adjust = diff / (1.0 + Math.exp(-adjustment * (Math.abs(diff) - 0.5)));
					// com.google.gwt.core.client.GWT.log("calculateAdjustment: diff="+diff+",
					// randoModel="+randoModel+" -> "+adjust);
					break;
				default:
					adjust = 0.0;
			}
			return adjust;
		}

		private int pickRandomModel(int focal) {
			if (pRandInteraction <= 0.0)
				return -1;
			double rand = random01();
			if (rand > pRandInteraction)
				return -1;
			// com.google.gwt.core.client.GWT.log("pickRandomModel: rand="+rand+",
			// pRand="+pRandInteraction+", focal="+focal);
//XXX check if model is indeed picked in desired level?
			int mdl;
			switch (reproduction.getType()) {
				case HIERARCHY:
					int levelStart, unitStart;
					// determine level at which to select model and size of level
					int level = 1;
					int maxLevel = reproduction.hierarchy.length - 1;
					int unitSize = reproduction.hierarchy[maxLevel];
					int levelSize = unitSize * reproduction.hierarchy[maxLevel - level];
					double prob = pRandInteraction * pRandInteraction;
					while (rand < prob && level < maxLevel) {
						level++;
						prob *= pRandInteraction;
						levelSize *= reproduction.hierarchy[maxLevel - level];
					}
					switch (reproduction.subgeometry) {
						case MEANFIELD:
							// determine start of unit and of level
							unitStart = (focal / unitSize) * unitSize;
							levelStart = (focal / levelSize) * levelSize;
							// draw random individual in level, excluding focal unit
							mdl = levelStart + random0n(levelSize - unitSize);
							if (mdl >= unitStart)
								mdl += unitSize;
							return mdl;

						case SQUARE:
							// determine location of focal unit relative to desired level
							int unitSide = (int) Math.sqrt(unitSize);
							int levelSide = (int) Math.sqrt(levelSize);
							int side = (int) Math.sqrt(reproduction.size);
							int unitX = ((focal % levelSide) / unitSide) * unitSide;
							int unitY = (((focal / side) % levelSide) / unitSide) * unitSide;
							unitStart = unitY * levelSide + unitX;
							// draw random individual in level, excluding focal unit
							mdl = random0n(levelSize - unitSize);
							for (int i = 0; i < unitSide; i++) {
								if (mdl < unitStart)
									break;
								mdl += unitSide;
								unitStart += levelSide;
							}
							// transform index of model in level to population
							// int levelX = ((focal%side)/levelSide)*levelSide;
							// int levelY = ((focal/side)/levelSide)*levelSide;
							int levelX = ((mdl % side) / levelSide) * levelSide;
							int levelY = ((mdl / side) / levelSide) * levelSide;
							levelStart = levelY * side + levelX;
							int x = mdl % levelSide;
							int y = mdl / levelSide;
							mdl = levelStart + y * side + x;
							return mdl;

						default:
							throw new Error("teach me first how to deal with subgeometry '"
									+ reproduction.subgeometry.getTitle() + "'!");
					}

				case COMPLETE:
				case MEANFIELD:
					mdl = random0n(nPopulation - 1);
					if (mdl >= focal)
						mdl++;
					return mdl;

				default:
					// exclude neighbours - seems a bit overkill (would be more reasonable with a
					// sorted array of neighbours)
//XXX can block for some geometries (e.g. hub for star or wheel geometries)
					do {
						mdl = random0n(nPopulation - 1);
						if (mdl >= focal)
							mdl++;
					} while (reproduction.isNeighborOf(focal, mdl));
					return mdl;
			}
		}

		private double potentialAt(int idx) {
			return potentialAt(idx, strategies[idx]);
		}

		private double potentialAt(int idx, double spin) {
			int[] neighs = reproduction.out[idx];
			int len = neighs.length;
			double sum = 0.0;
			final double twopi = 2.0 * Math.PI;
			for (int i = 0; i < len; i++)
				sum -= Math.cos((spin - strategies[neighs[i]]) * twopi);
			return sum;
		}

		double[] data = new double[0];

		/**
		 * {@inheritDoc}
		 * <p>
		 * Dialect does not (yet) deal with payoffs.
		 */
		@Override
		public String getScoreNameAt(int idx) {
			return "-";
		}

		@Override
		public synchronized <T> void getTraitData(T[] colors, ColorMap<T> cMap) {
			if (updateType == Dynamic.EW_MODEL) {
				if (data.length != nPopulation)
					data = new double[nPopulation];
				// heights in strategies are not normalized for Edwards-Wilkinson model
				double min = ArrayMath.min(strategies);
				double irange = 1.0 / Math.max(1, ArrayMath.max(strategies) - min);
				for (int n = 0; n < nPopulation; n++) {
					data[n] = (strategies[n] - min) * irange;
				}
				cMap.translate(data, colors);
				return;
			}
			if (engine.getColorModelType() != ColorModelType.DISTANCE && nTraits <= 3) {
				// use default color model
				super.getTraitData(colors, cMap);
				return;
			}
			// do 'manual' translation into colors - pre-process data
			if (data.length != nPopulation)
				data = new double[nPopulation];
			int idx = 0;
			// maximum distance from origin of phenotype hypercube
			double imaxdist = 1.0 / Math.sqrt(nTraits);
			// maximum distance from center of phenotype hypercube
			// double imaxdist = 1.0/Math.sqrt(nTraits*0.25);
			for (int n = 0; n < nPopulation; n++) {
				double dist = 0.0;
				for (int i = 0; i < nTraits; i++) {
					// distance from origin of phenotype hypercube
					double si = strategies[idx + i];
					// distance from center of phenotype hypercube
					// double si = strategies[idx+i]-0.5;
					dist += si * si;
				}
				// data[n] = dist;
				data[n] = Math.sqrt(dist) * imaxdist;
				idx += nTraits;
			}
			// translate into colors
			cMap.translate(data, colors);
		}

		@Override
		public void getTraitHistogramData(double[][] bins) {
			if (updateType != Dynamic.EW_MODEL) {
				super.getTraitHistogramData(bins);
				return;
			}
			// heights in strategies are not normalized for Edwards-Wilkinson model (only
			// one trait)
			// clear bins
			Arrays.fill(bins[0], 0.0);
			int nBins = bins[0].length;
			double incr = 1.0 / nPopulation;
			double min = ArrayMath.min(strategies);
			double scale = (nBins - 1) / Math.max(1, ArrayMath.max(strategies) - min);
			// fill bins
			for (int n = 0; n < nPopulation; n++)
				bins[0][(int) ((strategies[n] - min) * scale + 0.5)] += incr;
		}

		@Override
		public void getTraitHistogramData(double[] bins, int trait1, int trait2) {
			if (updateType != Dynamic.EW_MODEL) {
				super.getTraitHistogramData(bins, trait1, trait2);
				return;
			}
			// Edwards-Wilkinson model has only a single trait, ignore trait1 and trait2
			// heights in strategies are not normalized for Edwards-Wilkinson model
			// clear bins
			Arrays.fill(bins, 0.0);
			double incr = 1.0 / nPopulation;
			double min = ArrayMath.min(strategies);
			double scale = (bins.length - 1) / Math.max(1, ArrayMath.max(strategies) - min);
			// fill bins
			for (int n = 0; n < nPopulation; n++)
				bins[(int) ((strategies[n] - min) * scale + 0.5)] += incr;
		}

		@Override
		public void commitStrategies() {
			throw new Error("commitStrategies() should not be called!!!");
		}

		@Override
		public void commitStrategyAt(int me) {
			throw new Error("commitStrategyAt() should not be called!!!");
		}

		@Override
		public void updateScores() {
		}

		@Override
		public void playGameSyncAt(int me) {
			throw new Error("playGameSyncAt() should not be called!!!");
		}

		@Override
		public void playGameAt(int me) {
			throw new Error("playGameAt() should not be called!!!");
		}

		// the strategy still needs to be committed
		@Override
		public void adjustGameScoresAt(int me) {
			throw new Error("adjustGameScoresAt() should not be called!!!");
		}
	}
}
