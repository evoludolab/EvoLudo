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

package org.evoludo.simulator.exec;

import java.io.IOException;
import java.io.PrintStream;
import java.lang.management.ManagementFactory;

import org.evoludo.simulator.EvoLudo;
import org.evoludo.simulator.EvoLudoJRE;
import org.evoludo.simulator.models.IBSD;
import org.evoludo.simulator.models.IBSDPopulation;
import org.evoludo.simulator.models.Type;
import org.evoludo.simulator.models.PopulationUpdate;
import org.evoludo.simulator.modules.PlayerUpdate;
import org.evoludo.simulator.modules.Traits;
import org.evoludo.util.CLOParser;
import org.evoludo.util.CLOption;
import org.evoludo.util.CLOption.CLODelegate;
import org.evoludo.util.Formatter;

import com.sun.management.OperatingSystemMXBean;

/**
 * Simulations to investigate individual based simulations versus stochastic
 * differential equations for modelling the evolutionary dynamics of populations
 * with pairwise interactions and {@code d} strategic traits.
 * 
 * @see "Traulsen, A., Claussen, J. C., & Hauert, C. (2012). <em>Stochastic
 *      differential equations for evolutionary dynamics with demographic noise
 *      and mutations</em>. Physical Review E, 71(2), 025106.
 *      <a href='http://dx.doi.org/10.1103/PhysRevE.71.025106'>doi:
 *      10.1103/PhysRevE.71.025106</a>"
 */
public class simTraits extends Traits {

	/**
	 * The operating system management bean to measure the CPU time.
	 */
	OperatingSystemMXBean osMBean;

	/**
	 * The minimal measurement time in nanoseconds. Defaults to 60 seconds.
	 */
	long mintime = 60000000000L; // one minute in nanoseconds

	/**
	 * The flag to indicate whether to show progress.
	 */
	boolean progress = false;

	/**
	 * The population sizes to be tested.
	 */
	int[] popsizes = { 100 };

	/**
	 * The IBS model.
	 */
	IBSD ibs;

	/**
	 * The population.
	 */
	IBSDPopulation pop;

	/**
	 * The output stream. Defaults to {@code System.out}.
	 */
	PrintStream out;

	/**
	 * Create a new simulation to compare the performance of individual based
	 * simulations versus stochastic differential equations.
	 * 
	 * @param engine the pacemaker for running the model
	 */
	public simTraits(EvoLudo engine) {
		super(engine);
	}

	@Override
	public void run() {
		out = ((EvoLudoJRE) engine).getOutput();
		// for IBS simulations
		ibs = (IBSD) engine.getModel();
		pop = (IBSDPopulation) getIBSPopulation();
		model.cloTimeRelax.setDefault("10000");

		// initialize timing
		try {
			osMBean = ManagementFactory.newPlatformMXBeanProxy(
					ManagementFactory.getPlatformMBeanServer(),
					ManagementFactory.OPERATING_SYSTEM_MXBEAN_NAME,
					OperatingSystemMXBean.class);
		} catch (IOException e) {
			throw new Error("failed to initialize timing system - " + e.getMessage() + ".");
		}

		model.setTimeStep(1.0);
		// default init type is UNIFORM
		playerUpdate.setType(PlayerUpdate.Type.IMITATE);
		playerUpdate.setNoise(1.0);
		engine.modelReset();
		engine.dumpParameters();

		// dry-run to give the JIT a chance to compile the relevant parts
		setNPopulation(100);
		mutation.probability = 0.01;
		pop.getPopulationUpdate().setType(PopulationUpdate.Type.ASYNC);
		engine.loadModel(Type.IBS);
		engine.modelReset();
		engine.modelRelax();
		engine.loadModel(Type.SDE);
		engine.modelReset();
		engine.modelRelax();

		// output.println("# pop. size,\tcpuSims,\tcpuSDE,\tratio");
		out.println(
				"# traits, size,\tmcSims [1k MC],\tcpuSims [msec/1k MC],\tmcSDE [1k MC],\tcpuSDE [msec/1k MC],\tratio");
		for (int n = 0; n < popsizes.length; n++) {
			setNPopulation(popsizes[n]);
			mutation.probability = 1.0 / popsizes[n];

			// individual based simulations
			engine.loadModel(Type.IBS);
			pop.getPopulationUpdate().setType(PopulationUpdate.Type.ASYNC);
			engine.modelReset();
			long cpuBefore = osMBean.getProcessCpuTime(); // time in nanoseconds
			long cpuSims, mcSims = 0;
			while ((cpuSims = osMBean.getProcessCpuTime() - cpuBefore) < mintime) {
				for (long g = 1; g <= 1000; g++)
					engine.modelNext();
				mcSims += 1000L;
			}
			// stochastic differential equations
			engine.loadModel(Type.SDE);
			engine.modelReset();
			cpuBefore = osMBean.getProcessCpuTime(); // time in nanoseconds
			long cpuSDE, mcSDE = 0;
			while ((cpuSDE = osMBean.getProcessCpuTime() - cpuBefore) < mintime) {
				for (long g = 1; g <= 1000; g++)
					engine.modelNext();
				mcSDE += 1000L;
			}
			cpuSims /= 1000000L; // convert to milliseconds (accuracy on Mac OS X java 6 is anyways no bigger
									// than that)
			cpuSDE /= 1000000L;
			out.println(nTraits + "\t" + nPopulation + "\t" + (mcSims / 1000L) + "\t"
					+ Formatter.format((cpuSims * 1000.0) / mcSims, 2) + "\t" + (mcSDE / 1000L) + "\t"
					+ Formatter.format((cpuSDE * 1000.0) / mcSDE, 2) +
					"\t" + Formatter.formatSci((double) (cpuSims * mcSDE) / (double) (cpuSDE * mcSims), 6));
		}
		engine.dumpEnd();
		engine.exportState();
	}

	/**
	 * Command line option to specify the population sizes.
	 */
	public final CLOption cloNPopulations = new CLOption("popsize", "100", EvoLudo.catSimulation,
			"--popsize <n1:n2:...>  array of population sizes",
			new CLODelegate() {
				@Override
				public boolean parse(String arg) {
					popsizes = CLOParser.parseIntVector(arg);
					return true;
				}
			});

	/**
	 * Command line option to show the simulation progress.
	 */
	public final CLOption cloProgress = new CLOption("progress", EvoLudo.catSimulation,
			"--progress      make noise about progress",
			new CLODelegate() {
				@Override
				public boolean parse(String arg) {
					progress = cloProgress.isSet();
					return true;
				}
			});

	/**
	 * Command line option to set the minimal measurement time.
	 */
	public final CLOption cloMinTime = new CLOption("mintime", "60", EvoLudo.catSimulation, // 60 seconds
			"--mintime <t>   minimal measurement time in seconds",
			new CLODelegate() {
				@Override
				public boolean parse(String arg) {
					mintime = CLOParser.parseInteger(arg) * 1000000000L; // convert to nanoseconds
					return true;
				}

				@Override
				public void report(PrintStream output) {
					output.println("# mintime:              " + (mintime / 1000000000L) + " sec");
				}
			});

	@Override
	public void collectCLO(CLOParser parser) {
		super.collectCLO(parser);

		parser.removeCLO(
				new String[] { "popsize", "popupdate", "playerupdate", "geometry", "mutation", "timestop" });
		parser.addCLO(cloNPopulations);
		parser.addCLO(cloProgress);
		parser.addCLO(cloMinTime);
	}

	/**
	 * Main method to run the simulation.
	 * 
	 * @param args the array of command line arguments
	 */
	public static void main(String[] args) {
		EvoLudoJRE engine = new EvoLudoJRE(false);
		engine.custom(new simTraits(engine), args);
	}
}
