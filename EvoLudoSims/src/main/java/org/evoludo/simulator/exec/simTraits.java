//
// EvoLudo Project
//
// Copyright 2010-2020 Christoph Hauert
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

package org.evoludo.simulator.exec;

import java.io.IOException;
import java.io.PrintStream;
import java.lang.management.ManagementFactory;

import org.evoludo.simulator.EvoLudoJRE;
import org.evoludo.simulator.EvoLudo;
import org.evoludo.simulator.models.Model;
import org.evoludo.simulator.models.IBS;
import org.evoludo.simulator.models.IBS.PopulationUpdateType;
import org.evoludo.simulator.models.IBSDPopulation;
import org.evoludo.simulator.modules.Traits;
import org.evoludo.util.CLOParser;
import org.evoludo.util.CLOption;
import org.evoludo.util.CLOption.CLODelegate;
import org.evoludo.util.Formatter;

import com.sun.management.OperatingSystemMXBean;

public class simTraits extends Traits {

	// timing
	OperatingSystemMXBean osMBean;

	// additional parameters
	long mintime = 60000000000L;	// one minute in nanoseconds
	boolean progress = false;
	int[] popsizes = { 100 };
	IBS ibs;
	IBSDPopulation pop;
	PrintStream out;

	public simTraits(EvoLudo engine) {
		super(engine);
	}

	@Override
	public void run() {
		out = ((EvoLudoJRE) engine).getOutput();
		// for IBS simulations
		ibs = (IBS) engine.getModel();
		pop = (IBSDPopulation) ibs.getSpecies(this);
		engine.cloRelaxation.setDefault("10000");

		// initialize timing
		try {
			osMBean = ManagementFactory.newPlatformMXBeanProxy(
															   ManagementFactory.getPlatformMBeanServer(), 
															   ManagementFactory.OPERATING_SYSTEM_MXBEAN_NAME, 
															   OperatingSystemMXBean.class);
		}
		catch (IOException e) {
			throw new Error("failed to initialize timing system - "+e.getMessage()+".");
		}

		engine.setReportInterval(1.0);
		setInit(null);
		setPlayerUpdateType(PlayerUpdateType.IMITATE);
		setPlayerUpdateNoise(1.0);
		engine.modelReset();
		engine.dumpParameters();

		// dry-run to give the JIT a chance to compile the relevant parts
		setNPopulation(100);
		setMutationProb(0.01);
		pop.setPopulationUpdateType(PopulationUpdateType.ASYNC);
		engine.loadModel(Model.Type.IBS);
		engine.modelReset();
		engine.modelRelax();
		engine.loadModel(Model.Type.SDE);
		engine.modelReset();
		engine.modelRelax();

//		output.println("# pop. size,\tcpuSims,\tcpuSDE,\tratio");
		out.println("# traits, size,\tmcSims [1k MC],\tcpuSims [msec/1k MC],\tmcSDE [1k MC],\tcpuSDE [msec/1k MC],\tratio");
		for( int n=0; n<popsizes.length; n++ ) {
			setNPopulation(popsizes[n]);
			setMutationProb(1.0/popsizes[n]);

			// individual based simulations
			pop.setPopulationUpdateType(PopulationUpdateType.ASYNC);
			engine.loadModel(Model.Type.IBS);
			engine.modelReset();
			long cpuBefore = osMBean.getProcessCpuTime();	// time in nanoseconds
			long cpuSims, mcSims = 0;
			while( (cpuSims = osMBean.getProcessCpuTime()-cpuBefore)<mintime ) {
				for( long g=1; g<=1000; g++ )
					engine.modelNext();
				mcSims += 1000L;
			}
			// stochastic differential equations
			engine.loadModel(Model.Type.SDE);
			engine.modelReset();
			cpuBefore = osMBean.getProcessCpuTime();		// time in nanoseconds
			long cpuSDE, mcSDE = 0;
			while( (cpuSDE = osMBean.getProcessCpuTime()-cpuBefore)<mintime ) {
				for( long g=1; g<=1000; g++ )
					engine.modelNext();
				mcSDE += 1000L;
			}
			cpuSims /= 1000000L;	// convert to milliseconds (accuracy on Mac OS X java 6 is anyways no bigger than that)
			cpuSDE  /= 1000000L;
			out.println(nTraits+"\t"+nPopulation+"\t"+(mcSims/1000L)+"\t"+Formatter.format((cpuSims*1000.0)/mcSims, 2)+"\t"+(mcSDE/1000L)+"\t"+Formatter.format((cpuSDE*1000.0)/mcSDE, 2)+
					"\t"+Formatter.formatSci((double)(cpuSims*mcSDE)/(double)(cpuSDE*mcSims), 6));
		}
		engine.dumpEnd();
		engine.exportState();
	}

	/*
	 * command line parsing stuff
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
	public final CLOption cloProgress = new CLOption("progress", EvoLudo.catSimulation,
    		"--progress      make noise about progress",
    		new CLODelegate() {
		    	@Override
		    	public boolean parse(String arg) {
					progress = cloProgress.isSet();
					return true;
		    	}
		    });
	public final CLOption cloMinTime = new CLOption("mintime", "60", EvoLudo.catSimulation,	// 60 seconds
		  "--mintime <t>   minimal measurement time in seconds",
		  new CLODelegate() {
	    	@Override
	    	public boolean parse(String arg) {
				mintime = CLOParser.parseInteger(arg)*1000000000L;	// convert to nanoseconds
				return true;
	    	}
	    	@Override
	    	public void report(PrintStream output) {
	    		output.println("# mintime:              "+(mintime/1000000000L)+" sec");
	    	}
	    });

  @Override
	public void collectCLO(CLOParser parser) {
		super.collectCLO(parser);

		parser.removeCLO(new String[] { "popsize", "popupdate", "playerupdate", "geometry", "mutation", "generations" });
		parser.addCLO(cloNPopulations);
		parser.addCLO(cloProgress);
		parser.addCLO(cloMinTime);
	}
}
