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

import java.io.PrintStream;
import java.util.Arrays;

import org.evoludo.simulator.EvoLudo;
import org.evoludo.simulator.models.IBS.PopulationUpdateType;
import org.evoludo.simulator.models.IBSD;
import org.evoludo.simulator.models.IBSD.FixationData;
import org.evoludo.simulator.models.IBSDPopulation;
import org.evoludo.util.CLOParser;
import org.evoludo.util.CLOption;
import org.evoludo.util.CLOption.CLODelegate;
import org.evoludo.util.Formatter;

/**
 *
 * @author Christoph Hauert
 */
public class Motility extends EcoMoran {

	protected double[] migrationRates;
	protected int[] migrationSteps;
	protected int motilityType = MOTILITY_RATE_STEPS;

	public static final int MOTILITY_RATE_STEPS = 0;
	public static final int MOTILITY_RATE_ONLY = 1;

	public Motility(EvoLudo engine) {
		super(engine);
	}

	@Override
	public void load() {
		super.load(); // EcoMoran sets names and colors
		// set defaults
		migrationRates = new double[3];
		migrationRates[VACANT] = 0.0;
		migrationRates[RESIDENT] = 0.0;
		migrationRates[MUTANT] = 0.1;
		migrationSteps = new int[3];
		migrationSteps[VACANT] = 0;
		migrationSteps[RESIDENT] = 0;
		migrationSteps[MUTANT] = 1;

	}

	@Override
	public void unload() {
		super.unload();
		migrationRates = null;
		migrationSteps = null;
	}

	@Override
	public String getKey() {
		return "Motility";
	}

	@Override
	public String getInfo() {
		return "Title: " + getTitle() + "\nAuthor: Christoph Hauert.";
	}

	@Override
	public String getTitle() {
		return "Selection & Motility";
	}

	@Override
	public String getVersion() {
		return "v1.0 April 2021";
	}

	public void setMigrationRates(double[] aVec) {
		if (aVec == null || aVec.length != migrationRates.length)
			return;
		System.arraycopy(aVec, 0, migrationRates, 0, 2);
	}

	public double[] getMigrationRates() {
		return migrationRates;
	}

	public void setMigrationSteps(int[] aVec) {
		if (aVec == null || aVec.length != migrationSteps.length)
			return;
		System.arraycopy(aVec, 0, migrationSteps, 0, 2);
	}

	public int[] getMigrationSteps() {
		return migrationSteps;
	}

	public int getMotilityType() {
		return motilityType;
	}

	public final CLOption cloRates = new CLOption("rates", "0,0,0.1", EvoLudo.catModule,
			"--rates <empty,resident,mutant>  migration rates", new CLODelegate() {
				@Override
				public boolean parse(String arg) {
					setMigrationRates(CLOParser.parseVector(arg));
					return true;
				}

				@Override
				public void report(PrintStream output) {
					output.println("# migrationrates:       " + Formatter.format(getMigrationRates(), 4));
				}
			});
	public final CLOption cloSteps = new CLOption("steps", "0,0,1", EvoLudo.catModule,
			"--steps <empty,resident,mutant>  migration steps", new CLODelegate() {
				@Override
				public boolean parse(String arg) {
					setMigrationSteps(CLOParser.parseIntVector(arg));
					return true;
				}

				@Override
				public void report(PrintStream output) {
					if (motilityType == MOTILITY_RATE_STEPS)
						output.println("# migrationsteps:       " + Formatter.format(getMigrationSteps()));
				}
			});
	public final CLOption cloNoSteps = new CLOption("nosteps", "steps", CLOption.Argument.NONE, EvoLudo.catModule,
			"--nosteps                        rate determines steps", new CLODelegate() {
				@Override
				public boolean parse(String arg) {
					if (cloNoSteps.isDefault())
						motilityType = MOTILITY_RATE_STEPS;
					else
						motilityType = MOTILITY_RATE_ONLY;
					return true;
				}

				@Override
				public void report(PrintStream output) {
					output.println(
							"# motility:             " + (motilityType == MOTILITY_RATE_ONLY ? "rates" : "steps"));
				}
			});

	@Override
	public void collectCLO(CLOParser parser) {
		// prepare command line options
		parser.addCLO(cloRates);
		parser.addCLO(cloSteps);
		parser.addCLO(cloNoSteps);

		super.collectCLO(parser);
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * <strong>Note:</strong> Cannot return more specific type due to subclass
	 * {@link CG#createIBSPop()}
	 */
	@Override
	public Motility.IBS createIBSPop() {
		return new Motility.IBS(engine);
	}

	public class IBS extends IBSDPopulation {

		protected IBS(EvoLudo engine) {
			super(engine);
		}

		@Override
		public boolean check() {
			boolean doReset = super.check();
			// if( populationUpdateType==POPULATION_UPDATE_ASYNC_IMITATE ) {
			// EvoLudo.logWarning("update type "+POPULATION_UPDATE_ASYNC_IMITATE+" not
			// implemented - switching to "+POPULATION_UPDATE_ASYNC_REPLICATE+".");
			// setPopulationUpdateType(POPULATION_UPDATE_ASYNC_REPLICATE);
			// }
			if (populationUpdateType != PopulationUpdateType.MORAN_BIRTHDEATH) {
				logger.warning("currently only birth-death updating supported!");
				setPopulationUpdateType(PopulationUpdateType.MORAN_BIRTHDEATH);
				doReset = true;
			}

			switch (motilityType) {
				case Motility.MOTILITY_RATE_STEPS:
				default:
					if (migrationRates[VACANT] < 1e-16 || migrationSteps[VACANT] == 0) {
						migrationRates[VACANT] = 0.0;
						migrationSteps[VACANT] = 0;
					}
					if (migrationRates[Motility.RESIDENT] < 1e-16 || migrationSteps[Motility.RESIDENT] == 0) {
						migrationRates[Motility.RESIDENT] = 0.0;
						migrationSteps[Motility.RESIDENT] = 0;
					}
					if (migrationRates[Motility.MUTANT] < 1e-16 || migrationSteps[Motility.MUTANT] == 0) {
						migrationRates[Motility.MUTANT] = 0.0;
						migrationSteps[Motility.MUTANT] = 0;
					}
					break;
				case Motility.MOTILITY_RATE_ONLY:
					Arrays.fill(migrationSteps, 0);
					if (migrationRates[Motility.MUTANT] <= 0.0)
						throw new Error("migration rates must be positive - otherwise nothing is happening...");
					break;
			}
			return doReset;
		}

		@Override
		public void init() {
			super.init();

			FixationData fix = ((IBSD) engine.getModel()).getFixationData();
			// place a single resident in an empty network
			if (Math.abs(init[VACANT] - 1.0) < 1e-8) {
				Arrays.fill(strategies, VACANT);
				Arrays.fill(strategiesTypeCount, 0);
				strategiesTypeCount[VACANT] = nPopulation;
				int side = (int) (Math.sqrt(nPopulation) + 0.5);
				int randnode = (side / 2) * (side - 1);
				strategiesTypeCount[strategies[randnode] % nTraits]--;
				strategies[randnode] = Motility.RESIDENT;
				strategiesTypeCount[Motility.RESIDENT]++;
			}
			// if population is monomorphic, place a single individual with the opposite
			// strategy
			if (strategiesTypeCount[Motility.MUTANT] == 0 && strategiesTypeCount[Motility.RESIDENT] == 0) {
				// VACANT population
				fix.mutantNode = rng.random0n(nPopulation);
				int type = rng.random0n(2) == 1 ? Motility.RESIDENT : Motility.MUTANT;
				fix.mutantTrait = type;
				strategiesTypeCount[type] = 1;
				strategiesTypeCount[VACANT]--;
				return;
			}
		}

		@Override
		protected void updatePlayerMoranBirthDeathAt(int parent) {
			int destination = -1;
			switch (motilityType) {
				case Motility.MOTILITY_RATE_STEPS:
				default:
					// choose random neighbor
					destination = pickNeighborSiteAt(parent);
					if (strategies[parent] % nTraits == Motility.MUTANT) {
						// do another random step with probability of migration
						if (random01() < migrationRates[Motility.MUTANT]) {
							for (int s = 0; s < migrationSteps[Motility.MUTANT]; s++)
								destination = pickNeighborSiteAt(destination);
						}
					}
					break;
				case Motility.MOTILITY_RATE_ONLY:
					while (random01() < migrationRates[Motility.MUTANT])
						destination = pickNeighborSiteAt(parent);
					break;
			}
			if (destination < 0 || strategies[destination] != VACANT)
				return;
			updatePlayerMoran(parent, destination);
		}

		@Override
		public boolean checkConvergence() {
			return strategiesTypeCount[VACANT] == 0;
		}
	}
}
