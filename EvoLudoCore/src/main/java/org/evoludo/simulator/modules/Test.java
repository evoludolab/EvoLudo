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

import org.evoludo.math.MersenneTwister;
import org.evoludo.math.RNGDistribution;
import org.evoludo.simulator.EvoLudo;
import org.evoludo.simulator.EvoLudo.Directive;
import org.evoludo.simulator.models.IBS.HasIBS;
import org.evoludo.simulator.views.HasConsole;
import org.evoludo.util.CLOParser;
import org.evoludo.util.CLOption;

/**
 *
 * @author Christoph Hauert
 */
public class Test extends Module implements HasIBS, HasConsole {

	/**
	 * The shared random number generator to ensure reproducibility of results.
	 * 
	 * @see EvoLudo#getRNG()
	 */
	RNGDistribution rng;

	public Test(EvoLudo engine) {
		super(engine, null);
		rng = engine.getRNG();
	}

	@Override
	public String getKey() {
		return "Test";
	}

	@Override
	public String getInfo() {
		return "Title: " + getTitle() + "\nAuthor: Christoph Hauert\nTest suite for EvoLudo.";
	}

	@Override
	public String getTitle() {
		return "Test suite";
	}

	@Override
	public String getVersion() {
		return "v1.0 April 2021";
	}

	@Override
	public double getMinGameScore() {
		return 0;
	}

	@Override
	public double getMaxGameScore() {
		return 0;
	}

	@Override
	public double getMinMonoGameScore() {
		return 0;
	}

	@Override
	public double getMaxMonoGameScore() {
		return 0;
	}

	/**
	 * Command line option to perform test of random number generator on launch.
	 * This takes approximately 10-20 seconds. The test reports (1) whether the
	 * generated sequence of random numbers is consistent with the reference
	 * implementation of {@link MersenneTwister} and (2) the performance of
	 * MersenneTwister compared to {@link java.util.Random}.
	 */
//XXX will make sense once different tests are implemented
	// public final CLOption cloRNG = new CLOption("RNG", "skip", CLOption.Argument.NONE, EvoLudo.catModule,
	// 		"--RNG       test random number generator", new CLODelegate() {
	// 			@Override
	// 			public boolean parse(String arg) {
	// 				return true;
	// 			}
	// 		});

	// override this method in subclasses to add further command line options
	// subclasses must make sure that they include a call to super
	@Override
	public void collectCLO(CLOParser parser) {
		CLOption optModule = parser.getCLO(engine.cloModule.getName());
		// remove all options
		parser.clearCLO();
		parser.addCLO(optModule);
		// parser.addCLO(cloRNG);
	}

	@Override
	public Test.IBS createIBS() {
		return new Test.IBS(engine);
	}

	public class IBS extends org.evoludo.simulator.models.IBS {

		Test module;
		int test;
		boolean testRunning;

		protected IBS(EvoLudo engine) {
			super(engine);
			this.module = (Test) engine.getModule();
			logger = engine.getLogger();
			rng = engine.getRNG();
		}

		@Override
		public String getCounter() {
			return "test: " + test + " of " + total;
		}

		@Override
		public String getStatus() {
			// return "Test suite for EvoLudo";
			return engine.getVersion();
		}

		int total;

		/**
		 * Skip default loading procedures. Nothing to load.
		 */
		@Override
		public void load() {
		}

		/**
		 * Skip default unloading procedures. Nothing to unload.
		 */
		@Override
		public void unload() {
		}

		/**
		 * Skip default checks. Nothing to check.
		 */
		@Override
		public boolean check() {
			return false;
		}

		/**
		 * Skip default initialization. Minimalist initialization.
		 */
		@Override
		public void init() {
			total = 0;
			test = 0;
			// if (module.cloRNG.isSet())
			// 	total += 3;
			total = 3;
			if (total > 0)
				logger.info("Ready for tests.");
			else
				logger.warning("No tests requested.");
		}

		/**
		 * Skip default reset. Nothing to reset.
		 */
		@Override
		public void reset() {
		}

		/**
		 * Skip default update. Nothing to update.
		 */
		@Override
		public void update() {
		}

		/**
		 * Execute tests sequentially.
		 */
		@Override
		public synchronized boolean next() {
			if (total == 0) {
				logger.info("Nothing to do.");
				return false;
			}
			if (testRunning) {
				logger.info("Test running...");
				return false;
			}
			// if (module.cloRNG.isSet()) {
			// test of RNG requested
			switch (test) {
				case 0:
					logger.info("Testing MersenneTwister (correctness)...");
					engine.execute(new Directive() {
						@Override
						public void execute() {
							testRunning = true;
							int start = engine.elapsedTimeMsec();
							MersenneTwister.testCorrectness(logger);
							int lap = engine.elapsedTimeMsec();
							logger.info("MersenneTwister tests done: " + ((lap - start) / 1000.0) + " sec.");
							testDone();
						}
					});
					break;
				case 1:
					logger.info("Testing MersenneTwister (speed)...");
					engine.execute(new Directive() {
						@Override
						public void execute() {
							testRunning = true;
							int start = engine.elapsedTimeMsec();
							MersenneTwister.testSpeed(logger, engine, 10000000);
							int lap = engine.elapsedTimeMsec();
							logger.info("MersenneTwister tests done: " + ((lap - start) / 1000.0) + " sec.");
							testDone();
						}
					});
					break;
				case 2:
					logger.info("Testing RNGDistributions...");
					engine.execute(new Directive() {
						@Override
						public void execute() {
							testRunning = true;
							int start = engine.elapsedTimeMsec();
							RNGDistribution.Uniform.test(rng.getRNG(), logger, engine);
							RNGDistribution.Exponential.test(rng.getRNG(), logger, engine);
							RNGDistribution.Normal.test(rng.getRNG(), logger, engine);
							RNGDistribution.Geometric.test(rng.getRNG(), logger, engine);
							RNGDistribution.Binomial.test(rng.getRNG(), logger, engine);
							int lap = engine.elapsedTimeMsec();
							logger.info("RNGDistribution tests done: " + ((lap - start) / 1000.0) + " sec.");
							testDone();
						}
					});
					break;
				default:
			}
			// }
			if (total == 0)
				return false;
			return true;
		}

		void testDone() {
			testRunning = false;
			test++;
			boolean done = (test == total);
			if (done)
				logger.info("All tests done.");
			engine.modelNextDone(!done);
		}
		/**
		 * No command line options provided by module Test.
		 */
		@Override
		public void collectCLO(CLOParser parser) {
		}
	}
}
