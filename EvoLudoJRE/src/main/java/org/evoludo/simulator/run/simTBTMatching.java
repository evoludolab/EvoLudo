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
import org.evoludo.simulator.models.IBSGroup;
import org.evoludo.simulator.models.Model;
import org.evoludo.util.CLOParser;
import org.evoludo.util.CLOption;
import org.evoludo.util.CLOption.CLODelegate;
import org.evoludo.util.Formatter;
import org.evoludo.math.ArrayMath;
import org.evoludo.math.Distributions;

/**
 *
 * @author Christoph Hauert
 */
public class simTBTMatching extends TBT implements Model.ChangeListener {

	// additional parameters
	boolean matching = false;
	boolean progress = false;
	int nRuns = 1;
	boolean isHomoInit = false;

	boolean doStatistics = false;
	double[]	bins;
	int		nBins;

	public simTBTMatching(EvoLudo engine) {
		super(engine);
	}

	@Override
	public void exec() {
		// initialize
		// requires even number of runs
		if( isHomoInit ) {
			nRuns += nRuns%2;
			setInitFreqs(new double[] { 1.0, 0.0 });
		}
		engine.modelReset();

		// print header
		engine.dumpParameters();

		// reset data
		if( doStatistics ) bins = new double[nBins+1];

		// prepare for several runs
		mean = new double[nTraits];
		var = new double[nTraits];
		meanmean = new double[nTraits];
		meanvar = new double[nTraits];
		state = new double[nTraits];

		for( int r=0; r<nRuns; r++ ) {
			if( r>0 ) {
				Arrays.fill(mean, 0.0);
				Arrays.fill(var, 0.0);
				if( isHomoInit ) setInitFreqs(r%2==1?new double[] { 0.0, 1.0 }:new double[] { 1.0, 0.0 });
				engine.modelReset();
			}
			// relax population
			double nRelaxation = engine.getNRelaxation();
			lastcommit = nRelaxation;	// ensures that statistics ignores relaxation data
			boolean converged = !engine.modelRelax();

			// evolve population
			prevsample = generation;
			if (converged) {
				// simulations converged already - mean is current state and sdev is zero
				getMeanTraits(mean);
			}
			else {
				for( int g=1; g<=nGenerations; g++ ) {
					// note: do not make measurements here - use the hook fireStateChanged instead (keeps track of optimized state changes e.g. for rare mutations)
					engine.modelNext();
					reportProgress(nRelaxation+g);
				}
			}
	        for( int n=0; n<nTraits; n++ ) {
				meanmean[n] += mean[n];
				meanvar[n] += Math.sqrt(var[n]/(nGenerations-1));
	        }
		}
		// one last call to doStatistics to account for what was (or wasn't) happening since the last change of strategy (call to commitStrategyAt)
		doStatistics();
		String msg;
		msg = "# average and sdev frequencies\n# ";
		String[] names = module.getTraitNames();
		for( int n=0; n<nTraits; n++ )
			msg += names[n]+"\t";
		out.println(msg);
		msg = "";
		if( doStatistics && !isHomoInit )
			msg = "# ";	// comment results to make extraction of statistics data easier
		
		// calculate weighted mean and sdev - see wikipedia
//		for( int n=0; n<nTraits; n++ ) msg += ChHFormatter.format(mean[n], 6)+"\t"+ChHFormatter.format(Math.sqrt(var[n]/(generation-1)), 6)+"\t";
		for( int n=0; n<nTraits; n++ )
			msg += Formatter.format(meanmean[n]/nRuns, 6)+"\t"+Formatter.format(meanvar[n]/nRuns, 6)+"\t";
		out.println(msg);
		if( doStatistics ) {
			ArrayMath.normalize(bins);
			double sum = 0.0;
			msg = "# r, histogram of times in states (nBins="+bins.length+", binWidth="+(nPopulation/nBins)+")\n"+(isHomoInit?"#":"")+
					Formatter.format(getPayoff(DEFECT, COOPERATE)-getPayoff(COOPERATE, COOPERATE), 3);
			for( int n=0; n<=nBins; n++ ) {
				// note: it might be that the terminal gets confused if too many 0\t are sent to it...
				msg += "\t"+Formatter.format(bins[n], 6);
				sum += bins[n];
			}
			out.println(msg+"\n# sum-1.0="+Formatter.formatSci(sum-1.0, 6)+" (should be 0)");
double com = Distributions.distrMean(bins);
out.println("# check: mean="+Formatter.format(com, 6)+" +/- "+Formatter.format(Distributions.distrStdev(bins, com), 6));
if( isHomoInit ) out.println("# homo init - half of runs with homogeneous A, other half with homogeneous B");
		}
		out.println("# generations @ end: "+Formatter.formatSci(generation, 6));
		engine.dumpEnd();
		engine.exportState();
	}

	double[] mean, var, state, meanmean, meanvar;
	double prevsample = generation;

	@Override
	public synchronized void modelChanged(PendingAction pending) {
		if( model.relaxing() || prevsample>=generation ) {
			return;
		}
		getMeanTraits(state);
		// calculate mean and sdev based on an algorithm by D. Knuth - see wikipedia
        double w = generation-prevsample;
        double wn = w/(generation-engine.getNRelaxation());
        for( int n=0; n<nTraits; n++ ) {
			double delta = state[n]-mean[n];
        	mean[n] += wn*delta;
			var[n] += w*delta*(state[n]-mean[n]);
		}
        prevsample = generation;
	}

	@Override
	public synchronized void modelStopped() {
		if( model.relaxing() || prevsample>=generation ) {
			return;
		}
//NOTE: this now also fires when reaching nGenerations. is this a problem? i don't think so.
		// absorbing state reached
		getMeanTraits(state);
		// calculate weighted mean and sdev - see wikipedia
		double nRelaxation = engine.getNRelaxation();
        double w = nGenerations-(prevsample-nRelaxation);
        double wn = w/nGenerations;
        for( int n=0; n<nTraits; n++ ) {
			double delta = state[n]-mean[n];
			mean[n] += wn*delta;
			var[n] += w*delta*(state[n]-mean[n]);
		}
        prevsample = nGenerations+nRelaxation;
	}

	// called after every single update - more overhead, more accuracy
	double lastcommit;

	/**
	 * call doStatistics first so that strategiesTypeCount still reflects the state before the current event
	 */
	@Override
	public void commitStrategyAt(int me) {
		doStatistics();
		super.commitStrategyAt(me);
	}

	private void doStatistics() {
		if( bins==null || lastcommit>=generation ) return;
		if( nPopulation==nBins )
			bins[strategiesTypeCount[COOPERATE]] += (generation-lastcommit);
		else
			bins[(int)(((double)strategiesTypeCount[COOPERATE]/nPopulation)*nBins)] += (generation-lastcommit);
		lastcommit = generation;
	}

	private int lastprogress = 0;

	void reportProgress(int gen) {
		if( progress ) {
			int elapsed = engine.elapsedTimeMsec();
			double nRelaxation = engine.getNRelaxation();
			if( gen%1000==0 || elapsed-lastprogress>1000 ) {
				logProgress(gen+"/"+(nRelaxation+nGenerations)+" done");
				lastprogress = elapsed;
			}
		}
	}

	@Override
	protected void updatePlayerAsyncReplicateAt(int me) {
// misses mutations that also need to be committed
//		updatePlayerAt(me);
		if( updatePlayerAt(me) ) {
			commitStrategyAt(me);
		}
	}

	@Override
	protected boolean updateReplicatorHalf(int me, int[] refGroup, int rGroupSize) {
		// checkParams ensures that rGroupSize is 1
		int opp = refGroup[0];
		if( haveSameStrategy(me, opp) )
			// no need to continue if focal and reference (opponent) have same strategy
			return false;

		if( getInteractionType()==IBSGroup.SAMPLING_ALL ) {
			// SAMPLING_ALL - implies _not_ matching as well as GEOMETRY_MEANFIELD!
			resetScoreAt(me);
			resetScoreAt(opp);
			updateMixedMeanScores(); // no need to set the scores of everyone!
			double[] traitScores = new double[nTraits];
			mixedScores(strategiesTypeCount, nGroup, traitScores);
			int nInter = getPopulationSize()-1;
			setScoreAt(me, traitScores[strategies[me]%nTraits], nInter);
			setScoreAt(opp, traitScores[strategies[opp]%nTraits], nInter);
		}
		else {
			// matching and/or SAMPLING_COUNT
			playGameAt(me, opp);
		}

		// neutral case: choose random neighbor or individual itself
		if( Math.abs(maxScore-minScore)<1e-8 ) {
			int hit = random0n(2);
			if( hit == 1 ) return false;
			updateFromModelAt(me, opp);
			return true;
		}
		
		double myFit = getFitnessAt(me);
		double oppFit = getFitnessAt(opp);
		double aProb;
		
		if( invThermalNoise<=0.0 ) {
			// no noise
			double aDiff = oppFit-myFit;
			if( aDiff>0.0 ) aProb = 1.0-playerError;
			else aProb = (aDiff<0.0?playerError:0.5);
		}
		else {
			if( playerScoreAveraged ) {
				double scale = 0.5*invThermalNoise/(mapToFitness(maxScore)-mapToFitness(minScore));
				aProb = Math.min(1.0-playerError, Math.max(playerError, 0.5+(oppFit-myFit)*scale));
			}
			else {
				// take number of interactions into account for normalization
				double scale = 0.5*invThermalNoise/(mapToFitness(interactions[opp]*maxScore)-mapToFitness(interactions[me]*minScore));
				aProb = Math.min(1.0-playerError, Math.max(playerError, 0.5+(oppFit-myFit)/(interactions[opp]*maxScore-interactions[me]*minScore)*scale));
			}
		}
		if( aProb<=0.0 ) return false;
		
		double choice = random01();
		if( choice>=aProb ) return false;
		updateFromModelAt(me, opp);
		return true;
	}

	@Override
	public void playGameAt(int me) {
		// this only gets called after optimizations introduced a single mutant
		int opp = random0n(nPopulation-1);
		if( opp>me ) opp++;
		playGameAt(me, opp);
//		logError("ceci n'est pas un erreur!");
//		new Exception().printStackTrace();
	}

	int[] group = new int[1];
//101023 simTBTMatching needs access to interactionGroup in Population.java - this is probably a bad idea...
	public void playGameAt(int me, int opp) {
		// any graph - interact with out-neighbors
		// same as earlier approach to undirected graphs
		if( adjustScores ) {
			throw new Error("ERROR: playGameAt(int idx) and adjustScores are incompatible!");
		}
		// reset focal/opponent scores
		resetScoreAt(me);
		resetScoreAt(opp);
		int start = 0;
		if( matching ) {
			// ensure that focal interacts with opponent
			group[0] = opp;
			interactionGroup.setGroupAt(me, group, 1);
			playGameAt(interactionGroup);
			start = 1;
		}
		for( int i=start; i<nInteractions; i++ ) {
			interactionGroup.pickAt(me, interaction, true);
			playGameAt(interactionGroup);
			interactionGroup.pickAt(opp, interaction, true);
			playGameAt(interactionGroup);
		}
	}

	@Override
	public boolean check() {
		boolean doReset = super.check();
		if( !interaction.interReproSame || interaction.geometry != Geometry.MEANFIELD ) {
			logger.warning("well-mixed populations required for both interactions and reproduction!");
			interaction.geometry = Geometry.MEANFIELD;
			interaction.size = nPopulation;
			interaction.interReproSame = true;
			reproduction = interaction;
//			if( reproduction!=null ) {
//				reproduction.geometry = Geometry.MEANFIELD;
//				reproduction.size = nPopulation;
//			}
			doReset = true;
		}
		if( playerUpdateType!=PlayerUpdateType.IMITATE ) {
			logger.warning("only "+PlayerUpdateType.IMITATE+" currently implemented!");
			setPlayerUpdateType(PlayerUpdateType.IMITATE);
			doReset = true;
		}
		if( getReferenceType()!=IBSGroup.SAMPLING_COUNT ) {
			logger.warning("references are randomly sampled!");
			setReferenceType(IBSGroup.SAMPLING_COUNT);
			doReset = true;
		}
		if( getRGroupSize()!=1 ) {
			logger.warning("reference group size forced to 1!");
			setRGroupSize(1);
			doReset = true;
		}
		if( matching && getInteractionType()==IBSGroup.SAMPLING_ALL ) {
			setInteractionType(IBSGroup.SAMPLING_COUNT);
			logger.warning("matching requires sampling of interaction partners!");
			// change of sampling may affect whether scores can be adjusted but reset takes care of this
			doReset = true;
		}
		if( doStatistics ) {
			nBins = Math.min(nBins, nPopulation);
//			if( nRuns>1 ) {
//				nRuns = 1;
//				logger.warning("statistics only for single run!");
//			}
			if( interaction.geometry!=Geometry.MEANFIELD ) {
				logger.warning("statistics only for well-mixed populations available!");
				interaction.geometry = Geometry.MEANFIELD;
				interaction.interReproSame = true;
				doReset = true;
			}
		}
		// cannot use adjustScores
		adjustScores = false;
/*		if( nInteractions!=1 ) {
			setIGroupSize(nInteractions);
			setNInteractions(1);
			doReset = true;
		}*/
		return doReset;
	}

	/*
	 * command line parsing stuff
	 */
	public final CLOption cloProgress = new CLOption("progress", EvoLudo.catSimulation, CLOption.Argument.NONE, "noprogress",
			"--progress      make noise about progress",
		new CLODelegate() {
			@Override
			public boolean parse(String arg) {
				progress = cloProgress.isSet();
				return true;
			}
		}
	);
	public final CLOption cloMatching = new CLOption("matching", EvoLudo.catSimulation, CLOption.Argument.NONE, "nomatching",
			"--matching      competing individuals must interact",
		new CLODelegate() {
			@Override
			public boolean parse(String arg) {
				matching = cloMatching.isSet();
				return true;
			}
			@Override
			public void report(PrintStream output) {
				output.println("# matching players:     "+matching);
			}
		}
	);
	public final CLOption cloStatistics = new CLOption("statistics", EvoLudo.catSimulation, "100",
    		"--statistics    generate histogram of times spent in each state",
    	new CLODelegate() {
	    	@Override
	    	public boolean parse(String arg) {
	    		if( cloStatistics.isSet() ) {
					doStatistics = true;
					nBins = CLOParser.parseInteger(arg);
	    			return true;
	    		}
				doStatistics = false;
				nBins = 0;
				return true;
	    	}
	    	@Override
	    	public void report(PrintStream output) {
	    		if( doStatistics )
	    			output.println("# statistics bins:      "+nBins);
	    	}
    	}
    );
	public final CLOption cloRuns = new CLOption("runs", 'r', EvoLudo.catSimulation, "1",
    		"--runs <r>      number of repetitions",
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
    	}
    );

    @Override
	public void collectCLO(CLOParser parser) {
		// collect CLOs of super first to allow subclasses to override settings
		super.collectCLO(parser);
		parser.addCLO(cloProgress);
		parser.addCLO(cloMatching);
		parser.addCLO(cloStatistics);
		parser.addCLO(cloRuns);

		cloGenerations.setDefault("10");
    }

	@Override
	public boolean parseInitFrequencies(String cli) {
		if( cli==null || !cli.equals("homo") ) {
			return super.parseInitFrequencies(cli);
		}
		// activate two runs with complementary homogeneous initial configuration
		isHomoInit = true;
		return true;
	}
}
