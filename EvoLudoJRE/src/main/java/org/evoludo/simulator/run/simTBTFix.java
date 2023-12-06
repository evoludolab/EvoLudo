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

package org.evoludo.simulator.run;

import java.io.PrintStream;
import java.util.Arrays;

import org.evoludo.simulator.EvoLudo;
import org.evoludo.simulator.Geometry;
import org.evoludo.simulator.modules.TBT;
import org.evoludo.util.CLOParser;
import org.evoludo.util.CLOption;
import org.evoludo.util.CLOption.CLODelegate;
import org.evoludo.util.Formatter;

/**
 *
 * @author Christoph Hauert
 */
public class simTBTFix extends TBT {

	/* additional parameters */
	int nReset = 1;
	double startR = 1.0, endR = 1.0;
	double startS = 0.0, endS = 0.0;
	double startT = 1.0, endT = 1.0;
	double startP = 0.0, endP = 0.0;
	double startBC = 0.0, endBC = 0.0;
	boolean doBC = false;
	boolean initHub = false;
	boolean initNoHub = false;
	int numSteps = 0;
	long nRuns = 1;
	long nFix = 0;
	double tFix = 0.0;
	double tAFix = 0.0;
	double tBFix = 0.0;
	int	initNode = -1;
//	boolean	doStat = false;

	public simTBTFix(EvoLudo engine) {
		super(engine);
	}

	@Override
	public void exec() {
		if( pMutation>0.0 ) {
			logger.warning("cannot have non-zero mutation rate for fixation - reset.");
			pMutation = 0.0;
			optimizeHomo = false;
		}
//		modelReset();

		/* print header */
		out.println(
			"# scan 2x2 games in structured populations (5.11.09)");
		engine.dumpParameters();
//		output.print(doBC?"# n     b/c       rho\n":"# n     R       S       T       P       rho\n");
		out.println(doBC?"# n     c/b       rho\n":"# n     R       S       T       P       rho");

		double incrR = 1.0, incrS = 1.0, incrT = 1.0, incrP = 1.0, incrBC = 1.0, r = -1.0;
		if( numSteps>0 ) {
			incrR = Math.abs(startR-endR)/numSteps*(endR>=startR?1.0:-1.0);
			incrS = Math.abs(startS-endS)/numSteps*(endS>=startS?1.0:-1.0);
			incrT = Math.abs(startT-endT)/numSteps*(endT>=startT?1.0:-1.0);
			incrP = Math.abs(startP-endP)/numSteps*(endP>=startP?1.0:-1.0);
			incrBC = Math.abs(startBC-endBC)/numSteps*(endBC>=startBC?1.0:-1.0);
		}
		for( int steps=0; steps<=numSteps; steps++ ) {
			if( doBC ) {
//				r = (interaction.avgOut/2.0+steps*(3.0/2.0*interaction.avgOut)/(double)numSteps);
				r = startBC+incrBC*steps;
				// old parametrization - based on r=b/c
				/*setPayoff(r/(r-1.0), COOPERATE, COOPERATE);
				setPayoff(0.0, COOPERATE, DEFECT);
				setPayoff((r+1.0)/(r-1.0), DEFECT, COOPERATE);
				setPayoff(1.0/(r-1.0), DEFECT, DEFECT);*/
				// new parametrization - based on r=c/b - normalizes payoffs for cooperation.
				setPayoff(1.0, COOPERATE, COOPERATE);
				setPayoff(0.0, COOPERATE, DEFECT);
				setPayoff(1.0+r, DEFECT, COOPERATE);
				setPayoff(r, DEFECT, DEFECT);
			}
			else {
				setPayoff(startR+incrR*steps, COOPERATE, COOPERATE);
				setPayoff(startS+incrS*steps, COOPERATE, DEFECT);
				setPayoff(startT+incrT*steps, DEFECT, COOPERATE);
				setPayoff(startP+incrP*steps, DEFECT, DEFECT);
			}
			nFix = 0;
			tFix = tAFix = tBFix = 0.0;
			int lapReport = 10;
			long lapmsec = System.currentTimeMillis();
			engine.modelReset();

//test single time scale
/*
 output.println("test time scales - goal: "+ChHFormatter.formatFix(1.0/pUpdate, 2)+" on average per individual (pEff="+ChHFormatter.formatFix(pEffUpdate, 6)+")");
setReportFreq(0.0);
setMutationProb(0.001);
long totinter;
double meaninter = 0.0;
double lastgen = 0.0;
double nextreport = 1000;
double reportfreq = 1000;
while( generation<=nRuns ) {
	next();
	double p = lastgen/generation;
	double q = 1.0-p;
	lastgen = generation;
	totinter = 0L;
	for( int i=0; i<nPopulation; i++ ) totinter += interactions[i];
	meaninter = meaninter*p+((double)totinter/(double)nPopulation)*q;
//output.println("generation: "+ChHFormatter.formatFix(generation,3)+" average: "+ChHFormatter.formatFix((double)totinter/(double)nPopulation, 2)+" -> "+ChHFormatter.formatFix(meaninter, 2));
	if( generation>nextreport ) {
		nextreport += reportfreq;
output.println("generation: "+(int)generation+" average: "+ChHFormatter.formatFix((double)totinter/(double)nPopulation, 2)+" -> "+ChHFormatter.formatFix(meaninter, 2));
	}
}
totinter = 0L;
for( int i=0; i<nPopulation; i++ ) totinter += interactions[i];
output.println("-> generation: "+(int)generation+" average: "+ChHFormatter.formatFix(meaninter, 2));
((EvoLudoJRE) engine).exit(0);
 */
//end test time scale
			for( int n=1; n<=nRuns; n++ ) {

				/* init board and calc initial scores */
				if( (n%nReset) == 0 ) 
					engine.modelReset();
				else
					engine.modelReinit();	// use same structure - relevant only for random structures

				reportFreq = 0;
				engine.modelNext();

				/* evolve population */
				while( !(strategiesTypeCount[COOPERATE]==0 || strategiesTypeCount[DEFECT]==0) ) {
					engine.modelNext();
				}

				if( strategiesTypeCount[DEFECT]==0 ) {
					nFix++;
					tAFix += generation;
				}
				else {
					tBFix += generation;
				}
tFix += generation;

				if( n%lapReport==0 ) {
					long suminter = 0L;
					for( int i=0; i<nPopulation; i++ ) suminter += interactions[i];
					out.println("# run: "+n+"\ttime: "+Formatter.formatFix((System.currentTimeMillis()-lapmsec)/1000.0, 2)+"s\t"+
							   "avg. inter: "+Formatter.formatFix(((double)suminter/(double)nPopulation), 2)+"\t"+
							   "rhoA: "+Formatter.formatFix((double)nFix/(double)n, 6)+"\t"+
							   "tAFix: "+Formatter.formatFix(tAFix/nFix, 6)+"\t"+
							   "tBFix: "+Formatter.formatFix(tBFix/(n-nFix), 6)+"\t"+
							   "tFix: "+Formatter.formatFix(tFix/n, 6));
					lapReport *= 10;
					lapmsec = System.currentTimeMillis();
				}
			}

			out.println("#payoffs: (R, S, T, P) = ("+getPayoff(COOPERATE, COOPERATE)+", "+getPayoff(COOPERATE, DEFECT)+", "+getPayoff(DEFECT, COOPERATE)+", "+getPayoff(DEFECT, DEFECT)+")");
			out.println(Formatter.formatFix((double)steps/(double)Math.max(1, numSteps), 4)+"\t"+
				(doBC?Formatter.formatFix(r, 4)+"\t":
				(Formatter.formatFix(getPayoff(COOPERATE, COOPERATE), 4)+"\t"+
				Formatter.formatFix(getPayoff(COOPERATE, DEFECT), 4)+"\t"+
				Formatter.formatFix(getPayoff(DEFECT, COOPERATE), 4)+"\t"+
				Formatter.formatFix(getPayoff(DEFECT, DEFECT), 4)+"\t"))+
				Formatter.formatFix((double)nFix/(double)nRuns, 6)+"\t"+
				Formatter.formatFix(tAFix/nFix, 6)+"\t"+
				Formatter.formatFix(tBFix/(nRuns-nFix), 6)+"\t"+
				Formatter.formatFix(tFix/nRuns, 6)
				);
		}
		engine.dumpEnd();
		engine.exportState();
	}

	@Override
	public void init() {
// note: this is brute force... simply ignore initFreqs settings
		initFreqs[COOPERATE] = 0.0;
		initFreqs[DEFECT] = 1.0;
		super.init();
		Arrays.fill(strategies, DEFECT);
		if( initNode<0 ) {
			if( interaction.geometry==Geometry.STAR || interaction.geometry==Geometry.WHEEL ) {
				if( initHub ) initNode = 0;
				else {
					if( initNoHub ) initNode = random0n(nPopulation-1)+1;
					else initNode = random0n(nPopulation);
				}
			}
			else initNode = random0n(nPopulation);
		}
		strategies[initNode] = COOPERATE;
		strategiesTypeCount[COOPERATE] = 1;
		strategiesTypeCount[DEFECT] = nPopulation-1;
	}

	/*
	 * command line parsing stuff
	 */
	public final CLOption cloRatio = new CLOption("ratio", EvoLudo.catSimulation, "0",
		"--ratio <r> | <l>:<u>      range for cost-to-benefit ratio",
		new CLODelegate() {
			@Override
			public boolean parse(String arg) {
				double[] vec = CLOParser.parseVector(arg);
				switch( vec.length ) {
					case 1:
						startBC = vec[0];
						endBC = vec[0];
						break;
					default:
						logger.warning("too many ratio arguments ("+arg+") - ignored");
						return false;
					case 2:
						startBC = vec[0];
						endBC = vec[1];
				}
				doBC = true;
				return true;
			}
			@Override
			public void report(PrintStream output) {
				output.println((Math.abs(startBC-endBC)<1e-8?
						  "# b/c:                  "+Formatter.format(startBC, 4):
						  "# b/c-range:            "+Formatter.format(startBC, 4)+" - "+Formatter.format(endBC, 4)));
			}
		});
	public final CLOption cloReward = new CLOption("reward", 'R', EvoLudo.catSimulation, "1",
		"--reward, -R<r>           reward for mutual cooperation",
		new CLODelegate() {
			@Override
			public boolean parse(String arg) {
				double[] vec = CLOParser.parseVector(arg);
				switch( vec.length ) {
					case 1:
						startR = vec[0];
						endR = vec[0];
						break;
					default:
						logger.warning("too many sucker arguments ("+arg+") - ignored");
						return false;
					case 2:
						startR = vec[0];
						endR = vec[1];
				}
				setPayoff(startR, COOPERATE, COOPERATE);
				return true;
			}
			@Override
			public void report(PrintStream output) {
				output.println((Math.abs(startR-endR)<1e-8?
						  "# reward:               "+Formatter.format(startR, 4):
						  "# rewardrange:          "+Formatter.format(startR, 4)+" - "+Formatter.format(endR, 4)));
			}
		});
	public final CLOption cloSucker = new CLOption("sucker", 'S', EvoLudo.catSimulation, "0",
		"--sucker, -S<s>           sucker's payoff for being cheated",
		new CLODelegate() {
			@Override
			public boolean parse(String arg) {
				double[] vec = CLOParser.parseVector(arg);
				switch( vec.length ) {
					case 1:
						startS = vec[0];
						endS = vec[0];
						break;
					default:
						logger.warning("too many sucker arguments ("+arg+") - ignored");
						return false;
					case 2:
						startS = vec[0];
						endS = vec[1];
				}
				setPayoff(startS, COOPERATE, DEFECT);
				return true;
			}
			@Override
			public void report(PrintStream output) {
				output.println((Math.abs(startS-endS)<1e-8?
						"\n# sucker:               "+Formatter.format(startS, 4):
						"\n# suckerrange:          "+Formatter.format(startS, 4)+" - "+Formatter.format(endS, 4)));
			}
		});
	public final CLOption cloTemptation = new CLOption("temptation", 'T', EvoLudo.catSimulation, "1",
		"--temptation, -T<t>       temptation to defect",
		new CLODelegate() {
			@Override
			public boolean parse(String arg) {
				double[] vec = CLOParser.parseVector(arg);
				switch( vec.length ) {
					case 1:
						startT = vec[0];
						endT = vec[0];
						break;
					default:
						logger.warning("too many temptation arguments ("+arg+") - ignored");
						return false;
					case 2:
						startT = vec[0];
						endT = vec[1];
				}
				setPayoff(startT, DEFECT, COOPERATE);
				return true;
			}
			@Override
			public void report(PrintStream output) {
				output.println((Math.abs(startT-endT)<1e-8?
						"\n# temptation:           "+Formatter.format(startT, 4):
						"\n# temptationrange:      "+Formatter.format(startT, 4)+" - "+Formatter.format(endT, 4)));
			}
		});
	public final CLOption cloPunishment = new CLOption("punishment", 'P', EvoLudo.catSimulation, "0",
		"--punishment, -P<p>       punishment for mutual defection",
		new CLODelegate() {
			@Override
			public boolean parse(String arg) {
				double[] vec = CLOParser.parseVector(arg);
				switch( vec.length ) {
					case 1:
						startP = vec[0];
						endP = vec[0];
						break;
					default:
						logger.warning("too many punishment arguments ("+arg+") - ignored");
						return false;
					case 2:
						startP = vec[0];
						endP = vec[1];
				}
				setPayoff(startP, DEFECT, DEFECT);
				return true;
			}
			@Override
			public void report(PrintStream output) {
				output.println((Math.abs(startP-endP)<1e-8?
						"\n# punishment:           "+Formatter.format(startP, 4):
						"\n# punishmentrange:      "+Formatter.format(startP, 4)+" - "+Formatter.format(endP, 4)));
			}
		});
	public final CLOption cloSteps = new CLOption("steps", 's', EvoLudo.catSimulation, "0",
		"--steps, -s <s>       number of steps within interest interval",
		new CLODelegate() {
	    	@Override
	    	public boolean parse(String arg) {
				numSteps = CLOParser.parseInteger(arg);
				return true;
	    	}
	    	@Override
	    	public void report(PrintStream output) {
	    		output.println("# stepcount:            "+numSteps);
	    	}
	    });
	public final CLOption cloRuns = new CLOption("runs", 'r', EvoLudo.catSimulation, "1",
		"--runs, -r<r>        number of repetitions",
		new CLODelegate() {
	    	@Override
	    	public boolean parse(String arg) {
				nRuns = CLOParser.parseInteger(arg);
				return true;
	    	}
	    	@Override
	    	public void report(PrintStream output) {
	    		output.println("# runs:                 "+nRuns);
	    	}
	    });
	public final CLOption cloResetFreq = new CLOption("resetfreq", EvoLudo.catSimulation, "1",
		"--resetfreq <r>       regenerate population structure every r runs",
		new CLODelegate() {
	    	@Override
	    	public boolean parse(String arg) {
				nReset = CLOParser.parseInteger(arg);
				return true;
	    	}
	    	@Override
	    	public void report(PrintStream output) {
	    		output.println("# resetfreq:            "+nReset);
	    	}
	    });
	public final CLOption cloHub = new CLOption("hub", EvoLudo.catSimulation, CLOption.Argument.NONE, "nohub",
		"--hub                 place mutant in hub (star and wheel structures only)",
		new CLODelegate() {
	    	@Override
	    	public boolean parse(String arg) {
	    		if( cloHub.isSet() ) {
	    			initHub = true;
	    			initNoHub = false;	// exclusive - last argument takes precedence
	    		}
	    		else {
	    			initHub = false;
	    		}
				return true;
	    	}
	    	@Override
	    	public void report(PrintStream output) {
	    		if( (interaction.geometry==Geometry.STAR || interaction.geometry==Geometry.WHEEL) && initHub )
	    			output.println("# init:                 hub");
	    	}
	    });
	public final CLOption cloNoHub = new CLOption("nohub", EvoLudo.catSimulation, CLOption.Argument.NONE, "nonohub",
		"--nohub               do not place mutant in hub (star and wheel structures only)",
		new CLODelegate() {
	    	@Override
	    	public boolean parse(String arg) {
	    		if( cloNoHub.isSet() ) {
	    			initNoHub = true;
	    			initHub = false;	// exclusive - last argument takes precedence
	    		}
	    		else {
	    			initNoHub = false;
	    		}
				return true;
	    	}
	    	@Override
	    	public void report(PrintStream output) {
				if( (interaction.geometry==Geometry.STAR || interaction.geometry==Geometry.WHEEL) && initNoHub )
					output.println("# init:                 leaves");
	    	}
	    });
	public final CLOption cloInitFreqs = new CLOption("initfreqs", 'I', EvoLudo.catSimulation, "uniform", null,
		new CLODelegate() {
	    	@Override
	    	public boolean parse(String arg) {
	    		if( cloInitFreqs.isDefault() ) {
	    			setInitFreqs(null);
	    			return true;
	    		}
// note: somewhat hackish/ad hoc solution to place initial mutant in particular node...
				if( arg.charAt(0)=='x' || arg.charAt(0)=='X' ) {
					initNode = Integer.parseInt(arg.substring(1));
					return true;
				}
	    		parseInitFrequencies(arg);
				return true;
	    	}
	    	@Override
	    	public void report(PrintStream output) {
	    		String[] names = module.getTraitNames();
				for( int n=0; n<nTraits; n++ )
					output.println("# initfreqs:            "+Formatter.format(initFreqs[n], 6)+"\t"+
							names[n]+(traitActive[n]?" (active)":" (disabled)"));
	    	}
	    });

	@Override
	public void collectCLO(CLOParser parser) {
		parser.addCLO(cloRatio);
		parser.addCLO(cloReward);
		parser.addCLO(cloSucker);
		parser.addCLO(cloTemptation);
		parser.addCLO(cloPunishment);
		parser.addCLO(cloSteps);
		parser.addCLO(cloRuns);
		parser.addCLO(cloResetFreq);
		parser.addCLO(cloHub);
		parser.addCLO(cloNoHub);
		parser.addCLO(cloInitFreqs);

		super.collectCLO(parser);
		parser.removeCLO("generations");
	}
}
