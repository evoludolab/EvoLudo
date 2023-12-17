//
// EvoLudo Project
//
// Copyright 2020 Christoph Hauert
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

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.logging.Level;

import javax.imageio.ImageIO;

import org.evoludo.graphics.AbstractGraph;
import org.evoludo.simulator.EvoLudoJRE;
import org.evoludo.simulator.MVPop2D;
import org.evoludo.math.Distributions;
import org.evoludo.math.RNGDistribution;
import org.evoludo.simulator.EvoLudo;
import org.evoludo.simulator.Geometry;
import org.evoludo.simulator.models.Model;
import org.evoludo.simulator.modules.Module;
import org.evoludo.simulator.models.IBSDPopulation;
import org.evoludo.simulator.models.IBSPopulation;
import org.evoludo.simulator.modules.Mutualism;
import org.evoludo.util.CLOParser;
import org.evoludo.util.CLOption;
import org.evoludo.util.CLOption.CLODelegate;
import org.evoludo.util.Formatter;

/**
 *
 * @author Christoph Hauert
 */
public class simMutual extends Mutualism implements Model.ChangeListener {

	PrintStream out;
	org.evoludo.simulator.models.IBS ibs;
	IBSDPopulation mypop;
	IBSDPopulation opppop;

	public simMutual(EvoLudo engine) {
		super(engine);
	}

	// additional parameters
	boolean progress = false;
	int nRuns = 1;
	double[] scanST;
	double[] scanDG;
	double[] scanDilemma;

	// snapshots
	long snapinterval = 0;
	String snapprefix = "";
	
	// helper variables
	int myId, oppId;

	/**
	 *
	 */
	@Override
	public void run() {
		if(!model.isModelType(Model.Type.IBS)) {
			System.err.printf("ERROR: IBS model expected!");
			return;
		}
		// initialize
		out = ((EvoLudoJRE) engine).getOutput();
		ibs = (org.evoludo.simulator.models.IBS) model;
//		setReportFreq(1.0);
		engine.modelReset();

		/* print header */
		engine.dumpParameters();

		// prepare for several runs
		int nSpecies = getNSpecies();
		mean = new double[nSpecies][nTraits];
		var = new double[nSpecies][nTraits];
		state = new double[nSpecies][nTraits];
		opp = (Mutualism)getOpponent();
		progress |= (logger.getLevel().intValue()<=Level.FINE.intValue());
		double nRelaxation = engine.getNRelaxation();
		engine.setNGenerations(engine.getNGenerations() + nRelaxation);

		mypop = (IBSDPopulation) ibs.getSpecies(this);
		opppop = (IBSDPopulation) ibs.getSpecies(opp);
		myId = getID();
		oppId = opp.getID();

		// try to align elementary step in EvoLudo and Gyuri's simulations
		if (debugGyuri >= 0) {
			// set payoffs of both populations
			double r = 0.01;
			for (Module pop : getSpecies()) {
				Mutualism mut = (Mutualism)pop;
				mut.setPayoff(1.0-r, COOPERATE, COOPERATE);
				mut.setPayoff(-r, COOPERATE, DEFECT);
				mut.setPayoff(1.0, DEFECT, COOPERATE);
				mut.setPayoff(0.0, DEFECT, DEFECT);
			}
			gyuriPrep();
			engine.modelReset();
			gyuriInit();
			for (int s=0; s<debugGyuri; s++) {
				gyuriStep();
				// advance EvoLudo second to allow payoff comparisons
				evoludoStep();
				// consistency checks
				if (lix != elix) {
					logger.warning("step "+s+": lattice differs: evoludo="+elix+", gyuri="+lix);
					continue;
				}
				if (idx != eidx) {
					logger.warning("step "+s+": focal differs: evoludo="+eidx+", gyuri="+idx);
					continue;
				}
				if (nidx != enidx) {
					logger.warning("step "+s+": model differs: evoludo="+enidx+", gyuri="+nidx);
					continue;
				}
				if (lix == 0 && mypop.strategies[idx]%nTraits != player_s[0][idx % nSize][idx / nSize]) {
					logger.warning("step "+s+": state differs: evoludo="+mypop.strategies[idx]%nTraits+", gyuri="+player_s[0][idx % nSize][idx / nSize]);
					continue;
				}
				if (lix == 1 && opppop.strategies[idx]%nTraits != player_s[1][idx % nSize][idx / nSize]) {
					logger.warning("step "+s+": state differs: evoludo="+opppop.strategies[idx]%nTraits+", gyuri="+player_s[1][idx % nSize][idx / nSize]);
					continue;
				}
			}
			// check lattice configurations at end
			boolean ok = true;
			for (int n=0; n<nPopulation; n++) {
				int epn = mypop.strategies[n]%nTraits;
				int eon = opppop.strategies[n]%nTraits;
				int gpn = player_s[0][n % nSize][n / nSize];
				int gon = player_s[1][n % nSize][n / nSize];
				if (epn != gpn) {
					logger.warning("lattice 0 differs @ "+n+": evoludo="+epn+", gyuri="+gpn);
					ok = false;
				}
				if (eon != gon) {
					logger.warning("lattice 1 differs @ "+n+": evoludo="+eon+", gyuri="+gon);
					ok = false;
				}
			}
			if (ok)
				logger.info("lattices identical after "+debugGyuri+" elementary steps!");
			return;
		}

		// scan asymmetric social dilemmas between populations;
		// consider circle around S=0, T=1 with radius r starting clockwise at S=-r, T=0
		if( scanDilemma!=null ) {
			setPayoff(1.0, COOPERATE, COOPERATE);
			setPayoff(0.0, DEFECT, DEFECT);
			opp.setPayoff(1.0, COOPERATE, COOPERATE);
			opp.setPayoff(0.0, DEFECT, DEFECT);
			String msg = "# average +/- sdev frequencies, covariance\n# Ah\tAm\tSh\tTh\tSm\tTm\t";
			String[] names = getTraitNames();
			String[] oppnames = opp.getTraitNames();
			for( int n=0; n<nTraits; n++ )
				msg += getName()+"."+names[n]+"\t";
			for( int n=0; n<opp.getNTraits(); n++ )
				msg += opp.getName()+"."+oppnames[n]+"\t";
			out.println(msg);
			double r = scanDilemma[0];
			double angle = 0.0;
			double aincr = 2.0*Math.PI/scanDilemma[1];
			int count = 0;
			while( angle<2.0*Math.PI ) {
				setPayoff(-r*Math.cos(angle), COOPERATE, DEFECT);
				setPayoff(1.0+r*Math.sin(angle), DEFECT, COOPERATE);
				double oppangle = 0.0;
				while( oppangle<2.0*Math.PI ) {
					opp.setPayoff(-r * Math.cos(oppangle), COOPERATE, DEFECT);
					opp.setPayoff(1.0 + r * Math.sin(oppangle), DEFECT, COOPERATE);
					for (int n = 0; n < nSpecies; n++) {
						Arrays.fill(mean[n], 0.0);
						Arrays.fill(var[n], 0.0);
					}
					engine.modelReset();
					// relax population
					boolean converged = !engine.modelRelax();
					prevsample = ibs.getTime();
					if( converged) {
						// simulations converged already - mean is current state and sdev is zero
						model.getMeanTraits(myId, mean[myId]);
						model.getMeanTraits(oppId, mean[oppId]);
					}
					else {
						while (engine.modelNext());
					}
					msg = Formatter.format(angle, 4)+"\t"+Formatter.format(oppangle, 4)+"\t"
							+Formatter.format(getPayoff(COOPERATE, DEFECT), 4)+"\t"+Formatter.format(getPayoff(DEFECT, COOPERATE), 4)+"\t"
							+Formatter.format(opp.getPayoff(COOPERATE, DEFECT), 4)+"\t"+Formatter.format(opp.getPayoff(DEFECT, COOPERATE), 4);
					double varnorm = 1.0/(ibs.getTime()-nRelaxation-1.0);
					for( int n=0; n<nTraits; n++ )
						msg += "\t"+Formatter.format(mean[myId][n], 6)+"\t"+Formatter.format(Math.sqrt(var[myId][n]*varnorm), 6);
					for( int n=0; n<opp.getNTraits(); n++ )
						msg += "\t"+Formatter.format(mean[oppId][n], 6)+"\t"+Formatter.format(Math.sqrt(var[oppId][n]*varnorm), 6);
					msg += "\t"+Formatter.format(covariance(), 6);
					out.println(msg);
					out.flush();
					count++;
					if( progress )
						engine.logProgress("progress " + count + "/" + (int)(scanDilemma[1]*scanDilemma[1]) + " done");
					oppangle += aincr;
				}
				angle += aincr;
			}
			out.println("# generations @ end: "+Formatter.formatSci(ibs.getTime(), 6));
			engine.dumpEnd();
			engine.exportState();
			return;
		}

		// scan symmetric donation games between populations
		if( scanDG!=null ) {
			String msg = "# average +/- sdev frequencies, chi, covariance\n# r\t";
			String[] names = getTraitNames();
			String[] oppnames = opp.getTraitNames();
			for( int n=0; n<nTraits; n++ )
				msg += getName()+"."+names[n]+"\t";
			for( int n=0; n<opp.getNTraits(); n++ )
				msg += opp.getName()+"."+oppnames[n]+"\t";
			out.println(msg);
			double r=scanDG[0];
			if (r>scanDG[1]) {
				// decrease r - make sure increment is negative
				scanDG[2] = -Math.abs(scanDG[2]);
			}
			while( (scanDG[2] > 0.0 && r<scanDG[1]+scanDG[2]) || (scanDG[2] < 0.0 && r>scanDG[1]+scanDG[2]) ) {
				for (Module pop : getSpecies()) {
					Mutualism mut = (Mutualism)pop;
					// common parameterization of donation game
//					mut.setPayoff(1.0, COOPERATE, COOPERATE);
//					mut.setPayoff(-r, COOPERATE, DEFECT);
//					mut.setPayoff(1.0+r, DEFECT, COOPERATE);
//					mut.setPayoff(0.0, DEFECT, DEFECT);
					// Gyuri's parameterization
					mut.setPayoff(1.0-r, COOPERATE, COOPERATE);
					mut.setPayoff(-r, COOPERATE, DEFECT);
					mut.setPayoff(1.0, DEFECT, COOPERATE);
					mut.setPayoff(0.0, DEFECT, DEFECT);
				}
				for (int n = 0; n < nSpecies; n++) {
					Arrays.fill(mean[n], 0.0);
					Arrays.fill(var[n], 0.0);
				}
				// relax population
				boolean converged;
				if (mypop.isMonomorphic() || opppop.isMonomorphic()) {
					engine.modelReset();
					converged = engine.modelRelax();
				}
				else {
					// if simulations did not converge, keep configuration, only reset time
					// and adjust min/max scores as well as update scores of all individuals
					// to new payoffs.
					ibs.init(true);
					// should be fine to reduce relaxation time
					engine.setNRelaxation(nRelaxation * 0.5);
					converged = engine.modelRelax();
					engine.setNRelaxation(nRelaxation);
				}
				prevsample = ibs.getTime();
				double relax;
				if (converged) {
					// simulations converged already - mean is current state and sdev is zero
					model.getMeanTraits(myId, mean[myId]);
					model.getMeanTraits(oppId, mean[oppId]);
					relax = model.getTime();
				}
				else {
					while (engine.modelNext());
					relax = nRelaxation;
				}
				msg = Formatter.format(r, 3);
				// if converged, varnorm < 0 but no matter because var[n][] = 0;
				double varnorm = 1.0/(ibs.getTime()-relax-1.0);
				double sqrtN = Math.sqrt(getNPopulation());
				for( int n=0; n<nTraits; n++ ) {
					double sdev = Math.sqrt(var[myId][n]*varnorm);
					msg += "\t"+Formatter.format(mean[myId][n], 6)+"\t"+Formatter.format(sdev, 6)+"\t"+Formatter.format(sdev*sqrtN, 6);
				}
				sqrtN = Math.sqrt(opp.getNPopulation());
				for( int n=0; n<opp.getNTraits(); n++ ) {
					double sdev = Math.sqrt(var[oppId][n]*varnorm);
					msg += "\t"+Formatter.format(mean[oppId][n], 6)+"\t"+Formatter.format(sdev, 6)+"\t"+Formatter.format(sdev*sqrtN, 6);
				}
				msg += "\t"+Formatter.format(covariance(), 6);
				out.println(msg);
				out.flush();
				if( progress )
					engine.logProgress("progress " + (int)((r-scanDG[0])/scanDG[2]+1.5) + "/" + (int)((scanDG[1]-scanDG[0])/scanDG[2]+1.5) + " done");
				r += scanDG[2];
			}
			out.println("# generations @ end: "+Formatter.formatSci(ibs.getTime(), 6));
			engine.dumpEnd();
			engine.exportState();
			return;
		}

		// scan S-T-plane of symmetric games between populations
		if( scanST!=null ) {
			setPayoff(1.0, COOPERATE, COOPERATE);
			setPayoff(0.0, DEFECT, DEFECT);
			opp.setPayoff(1.0, COOPERATE, COOPERATE);
			opp.setPayoff(0.0, DEFECT, DEFECT);
			String msg = "# average +/- sdev frequencies, covariance\n# S\tT\t";
			String[] names = getTraitNames();
			String[] oppnames = opp.getTraitNames();
			for( int n=0; n<nTraits; n++ )
				msg += getName()+"."+names[n]+"\t";
			for( int n=0; n<opp.getNTraits(); n++ )
				msg += opp.getName()+"."+oppnames[n]+"\t";
			out.println(msg);
			double s=scanST[0];
			while( s<scanST[1]+scanST[2]/2 ) {
				setPayoff(s, COOPERATE, DEFECT);
				opp.setPayoff(s, COOPERATE, DEFECT);
				double t=scanST[3 % scanST.length];
				while( t<scanST[4 % scanST.length]+scanST[5 % scanST.length]/2 ) {
					setPayoff(t, DEFECT, COOPERATE);
					opp.setPayoff(t, DEFECT, COOPERATE);
					for (int n = 0; n < nSpecies; n++) {
						Arrays.fill(mean[n], 0.0);
						Arrays.fill(var[n], 0.0);
					}
					engine.modelReset();
					// relax population
					boolean converged = !engine.modelRelax();
					prevsample = ibs.getTime();
					double relax;
					if( converged) {
						// simulations converged already - mean is current state and sdev is zero
						model.getMeanTraits(myId, mean[myId]);
						model.getMeanTraits(oppId, mean[oppId]);
						relax = model.getTime();
					}
					else {
						while (engine.modelNext());
						relax = nRelaxation;
					}
					msg = Formatter.format(s, 2)+"\t"+Formatter.format(t, 2);
					// if converged varnorm < 0 but no matter because var[n][] = 0;
					double varnorm = 1.0/(ibs.getTime()-relax-1.0);
					for( int n=0; n<nTraits; n++ )
						msg += "\t"+Formatter.format(mean[myId][n], 6)+"\t"+Formatter.format(Math.sqrt(var[myId][n]*varnorm), 6);
					for( int n=0; n<opp.getNTraits(); n++ )
						msg += "\t"+Formatter.format(mean[oppId][n], 6)+"\t"+Formatter.format(Math.sqrt(var[oppId][n]*varnorm), 6);
					msg += "\t"+Formatter.format(covariance(), 6);
					out.println(msg);
					out.flush();
					if( progress ) {
						int stepss = (int)((scanST[1]-scanST[0])/scanST[2]+1.5);
						int stepst = (int)((scanST[4 % scanST.length]-scanST[3 % scanST.length])/scanST[5 % scanST.length]+1.5);
						engine.logProgress("progress " + (int)((s-scanST[0])/scanST[2]+0.5)*stepst+(int)((t-scanST[3 % scanST.length])/scanST[5 % scanST.length]+0.5) + "/" + (stepss*stepst) + " done");
					}
					t += scanST[5 % scanST.length];
				}
				s += scanST[2];
			}
			out.println("# generations @ end: "+Formatter.formatSci(ibs.getTime(), 6));
			engine.dumpEnd();
			engine.exportState();
			return;
		}

		meanmean = new double[2][nTraits];
		meanvar = new double[2][nTraits];
		for( int r=0; r<nRuns; r++ ) {
			if( r>0 ) {
				for (int n = 0; n < nSpecies; n++) {
					Arrays.fill(mean[n], 0.0);
					Arrays.fill(var[n], 0.0);
				}
				engine.modelReset();
			}
			// relax population
			boolean converged = engine.modelRelax();
			// evolve population
			prevsample = ibs.getTime();
			double nGenerations = engine.getNGenerations();
			if (converged) {
				// simulations converged already - mean is current state and sdev is zero
				model.getMeanTraits(myId, mean[myId]);
				model.getMeanTraits(oppId, mean[oppId]);
			}
			else {
//XXX modelNext stops after nGenerations... use setReportFreq(snapinterval) etc.
				for( long g=0; g<=nGenerations; g++ ) {
					if( snapinterval>0 && g%snapinterval==0 ) {
						// save snapshot
						saveSnapshot(this, AbstractGraph.SNAPSHOT_PNG);
						saveSnapshot((Mutualism)getOpponent(), AbstractGraph.SNAPSHOT_PNG);
					}
					engine.modelNext();
					if( progress && g%1000==0 ) {
						engine.logProgress(g + "/" + nGenerations + " done");
					}
				}
			}
	        for( int n=0; n<nTraits; n++ ) {
				meanmean[myId][n] += mean[myId][n];
				meanmean[oppId][n] += mean[oppId][n];
				meanvar[myId][n] += Math.sqrt(var[myId][n]/(nGenerations-1));
				meanvar[oppId][n] += Math.sqrt(var[oppId][n]/(nGenerations-1));
	        }
		}
		if( snapinterval<0 ) {
			saveSnapshot(this, AbstractGraph.SNAPSHOT_PNG);
			saveSnapshot((Mutualism)getOpponent(), AbstractGraph.SNAPSHOT_PNG);
		}

		String msg = "# average and sdev frequencies\n# ";
		String[] names = getTraitNames();
		String[] oppnames = opp.getTraitNames();
		for( int n=0; n<nTraits; n++ )
			msg += getName()+"."+names[n]+"\t";
		for( int n=0; n<opp.getNTraits(); n++ )
			msg += opp.getName()+"."+oppnames[n]+"\t";
		out.println(msg);
		msg = "";
		// calculate weighted mean and sdev - see wikipedia
		for( int n=0; n<nTraits; n++ )
			msg += Formatter.format(meanmean[myId][n]/nRuns, 6)+"\t"+Formatter.format(meanvar[myId][n]/nRuns, 6)+"\t";
		for( int n=0; n<opp.getNTraits(); n++ )
			msg += Formatter.format(meanmean[oppId][n]/nRuns, 6)+"\t"+Formatter.format(meanvar[oppId][n]/nRuns, 6)+"\t";
		out.println(msg);
		// correlation between populations
		out.println("# covariance: "+Formatter.format(covariance(), 6));
		out.println("# generations @ end: "+Formatter.formatSci(ibs.getTime(), 6));
		engine.dumpEnd();
		engine.exportState();
	}

	private double covariance() {
		double[] x = new double[nPopulation];
		double[] y = new double[nPopulation];
		for( int n=0; n<nPopulation; n++ ) {
			x[n] = mypop.strategies[n]%nTraits;
			y[n] = opppop.strategies[n]%nTraits;
		}
		return Distributions.covariance(x, y);
	}

	double[][] mean, var, state, meanmean, meanvar;
	double prevsample;
	Mutualism opp;

	@Override
	public synchronized void modelChanged(PendingAction action) {
		double generation = ibs.getTime();
		if( model.relaxing() || prevsample>=generation ) {
			return;
		}
		model.getMeanTraits(myId, state[myId]);
		model.getMeanTraits(oppId, state[oppId]);
		// calculate weighted mean and sdev - see wikipedia
        double w = generation-prevsample;
        double wn = w/(generation-engine.getNRelaxation());
        for( int n=0; n<nTraits; n++ ) {
			double delta = state[myId][n]-mean[myId][n];
			mean[myId][n] += wn*delta;
			var[myId][n] += w*delta*(state[myId][n]-mean[myId][n]);
			delta = state[oppId][n]-mean[oppId][n];
			mean[oppId][n] += wn*delta;
			var[oppId][n] += w*delta*(state[oppId][n]-mean[oppId][n]);
		}
        prevsample = generation;
	}

	@Override
	public synchronized void modelStopped() {
		double generation = ibs.getTime();
		double nGenerations = engine.getNGenerations();
		if( model.relaxing() || prevsample>=generation ) {
			return;
		}
		// absorbing state reached
		model.getMeanTraits(myId, state[myId]);
		model.getMeanTraits(oppId, state[oppId]);
//System.err.println("stop - before: generation="+generation+", prevsample="+prevsample+", state="+ChHFormatter.format(state,4)+
//	", mean="+ChHFormatter.format(mean,4)+", var="+ChHFormatter.format(var,4));
//logMessage("test: mean[0].old="+mean[0]+" -> "+((mean[0]*(prevsample-relaxation)+state[0]*(generations-(prevsample-relaxation)))/generations));
		// calculate weighted mean and sdev - see wikipedia
        double w = nGenerations-(prevsample-engine.getNRelaxation());
        double wn = w/nGenerations;
        for( int n=0; n<nTraits; n++ ) {
			double delta = state[myId][n]-mean[myId][n];
			mean[myId][n] += wn*delta;
			var[myId][n] += w*delta*(state[myId][n]-mean[myId][n]);
			delta = state[oppId][n]-mean[oppId][n];
			mean[oppId][n] += wn*delta;
			var[oppId][n] += w*delta*(state[oppId][n]-mean[oppId][n]);
		}
//System.err.println("stop - after: mean="+ChHFormatter.format(mean,4)+", var="+ChHFormatter.format(var,4));
//		prevsample = nGenerations+relaxation;
		prevsample = nGenerations;
	}

	/*
	 * Guyri's implementation
	 */
	RNGDistribution grng;
	int nSize;
	int[][][] player_s;
	double temperature;
	int debugGyuri = -1;
	int lix, idx, nidx;

	public void gyuriPrep() {
		// allocate separate RNG and initialize to same seed as default
		grng = engine.getRNG().clone();
		if (!grng.getRNG().stateEquals(engine.getRNG().getRNG()))
			throw new Error("Cloning of RNG failed!");
		// allocate memory
		nSize = (int)(Math.sqrt(nPopulation) + 0.5);
		player_s = new int[2][nSize][nSize];
		// EvoLudo uses averaged payoffs and smaller temperature
		temperature = 5.0*getPlayerUpdateNoise();
	}

	public void gyuriInit() {
		grng.setRNGSeed();
		// recreate initial configuration and compare to EvoLudo
		// first sanity check for RNG...
		double[] dinit = new double[nTraits];
		model.getInitialTraits(0, dinit);
		double myDInit = dinit[DEFECT];
		model.getInitialTraits(1, dinit);
		double oppDInit = dinit[DEFECT];
		// two separate loops are required to preserve state of RNG
		for (int n=0; n<nPopulation; n++) {
			int x = n % nSize;
			int y = n / nSize;
			player_s[0][x][y] = grng.random01() < myDInit ? DEFECT : COOPERATE;
			if (player_s[0][x][y] != mypop.strategies[n]%nTraits)
				logger.warning("strategies in level 0 differ at idx="+n);
		}
		for (int n=0; n<nPopulation; n++) {
			int x = n % nSize;
			int y = n / nSize;
			player_s[1][x][y] = grng.random01() < oppDInit ? DEFECT : COOPERATE;
			if (player_s[1][x][y] != opppop.strategies[n]%nTraits)
				logger.warning("strategies in level 1 differ at idx="+n);
		}

		if (!grng.getRNG().stateEquals(engine.getRNG().getRNG()))
			throw new Error("RNGs diverged after initialization!");
	}

	public void gyuriStep() {
// orig
//		ix = (int) randl(L);
//		jx = (int) randl(L);
//		lix = (int) randl(2);
//		liy = 1 - lix;
//		SX = player_s[lix][ix][jx];
//		ip = nov[ix]; im = csok[ix];
//		jp = nov[jx]; jm = csok[jx];
//		ri = (int) randl(4);
//		switch (ri) {
//			case 0:   iy = ix;   jy = jp;   break;
//			case 1:   iy = im;   jy = jx;   break;
//			case 2:   iy = ix;   jy = jm;   break;
//			case 3:   iy = ip;   jy = jx;   break;
//		}
//		SY = player_s[lix][iy][jy];
//		if (SX != SY) {
//			s = player_s[liy][ix][jx];                  PX=pm[SX][s];
//			iz=ip;  jz=jx;    s=player_s[liy][iz][jz];  PX+=pm[SX][s];
//			iz=ix;  jz=jp;    s=player_s[liy][iz][jz];  PX+=pm[SX][s];
//			iz=im;  jz=jx;    s=player_s[liy][iz][jz];  PX+=pm[SX][s];
//			iz=ix;  jz=jm;    s=player_s[liy][iz][jz];  PX+=pm[SX][s];
//
//			ip = nov[iy];      jp = nov[jy];
//			im = csok[iy];     jm = csok[jy];
//
//			s = player_s[liy][iy][jy];                 PY=pm[SY][s];
//			iz=ip;  jz=jy;   s=player_s[liy][iz][jz];  PY+=pm[SY][s];
//			iz=iy;  jz=jp;   s=player_s[liy][iz][jz];  PY+=pm[SY][s];
//			iz=im;  jz=jy;   s=player_s[liy][iz][jz];  PY+=pm[SY][s];
//			iz=iy;  jz=jm;   s=player_s[liy][iz][jz];  PY+=pm[SY][s];
//
//			prd = 1.0 / (1 + exp(-(PY - PX)/temperature));
//			r = randd();
//			if ( r<prd) { player_s[lix][ix][jx] = SY;  }
//		}
// translation (attempt to maintain sequence of random numbers to allow detailed comparisons)
		// first pick species
		lix = grng.random0n(2);
		IBSDPopulation pop = lix == 0 ? mypop : opppop;
		int liy = 1 - lix;
		// pick index of individual and convert to (x, y) coordinates
		idx = grng.random0n(nPopulation);
		int ix = idx % nSize;
		int jx = idx / nSize;
		int SX = player_s[lix][ix][jx];
		// pick random neighbour of idx (use neighbourhood structure of EvoLudo) doesn't matter 
		// which species as long as the structures and pop sizes are the same
		Geometry neighs = mypop.getReproductionGeometry();
		// sanity check (kin contains array with the number of incoming links; same as kout for undirected networks)
		if (neighs.kin[idx] != 4)
			throw new Error("must have exactly 4 neighbours!");
		nidx = neighs.in[idx][grng.random0n(4)];
		// convert neighbour index to (x, y) coordinates
		int iy = nidx % nSize;
		int jy = nidx / nSize;
		int SY = player_s[lix][iy][jy];
		// EvoLudo does not use this optimization (for consistency with other update types)
		// should have no impact (except to require many more random numbers...)
//		if (SX != SY) {
			int s = player_s[liy][ix][jx];
			// determine accumulated payoff of focal player SX at idx against its 5 neighbours on opponent lattice
			double PX = getPayoff(SX, s);
			for (int i=0; i<4; i++ ) {
				int ni = neighs.in[idx][i];
				s = player_s[liy][ni % nSize][ni / nSize];
				PX += getPayoff(SX, s);
			}
			// note: EvoLudo payoffs must not yet have been updated
			// check payoffs (mind the differences with averaged vs accumulated payoffs)
			if (Math.abs(PX-5.0*pop.getFitnessAt(idx))>1e-8)
				logger.warning("payoffs in lattice "+lix+" differ at "+idx+": evoludo="+Formatter.format(pop.getFitnessAt(idx), 6)+" gyuri="+Formatter.format(PX, 6));

			// determine accumulated payoff of neighbour player SY at nidx against its 5 neighbours on opponent lattice
			s = player_s[liy][iy][jy];
			double PY = getPayoff(SY, s);
			for (int i=0; i<4; i++ ) {
				int ni = neighs.in[nidx][i];
				s = player_s[liy][ni % nSize][ni / nSize];
				PY += getPayoff(SY, s);
			}
			// check payoffs
			if (Math.abs(PY-5.0*pop.getFitnessAt(nidx))>1e-8)
				logger.warning("payoffs in lattice "+lix+" differ at "+nidx+": evoludo="+Formatter.format(pop.getFitnessAt(nidx), 6)+" gyuri="+Formatter.format(PY, 6));

//			double prd = 1.0 / (1 + Math.exp(-(PY - PX)/temperature));
			double prd = 1.0 / (2.0 + Math.expm1(-(PY - PX)/temperature));
			double r = grng.random01();
			if (r < prd) {
				player_s[lix][ix][jx] = SY;
			}
//			// check strategies - can only be checked after EvoLudo step
//			if (pop.strategies[idx] % nTraits != player_s[lix][ix][jx])
//				logger.warning("strategies in lattice "+lix+" differ at "+idx+": evoludo="+(pop.strategies[idx] % nTraits)+" gyuri="+player_s[lix][ix][jx]);
//		}
	}

	public final CLOption cloGyuri = new CLOption("gyuri", "-1", EvoLudo.catSimulation,
			  "--gyuri <s>     align EvoLudo with Gyuri for s elementary steps",
		new CLODelegate() {
		    	@Override
		    	public boolean parse(String arg) {
		    		debugGyuri = CLOParser.parseInteger(arg);
					return true;
		    	}
		    	@Override
		    	public void report(PrintStream output) {
		    		if( cloGyuri.isSet() )
		    			output.println("# debugging EvoLudo vs Gyuri for "+debugGyuri+" steps... ");
		    	}
		    });

	int elix, eidx, enidx;

	public void evoludoStep() {
		IBSPopulation fpop = ibs.pickFocalSpecies();
		elix = fpop.equals(mypop) ? 0 : 1;
		// lix == elix should hold
		eidx = fpop.pickFocalSite();
		fpop.updatePlayerAsyncAt(eidx);
		// retrieve index of model
		enidx = fpop.getReferenceGroup().getGroup()[0];
	}

	
    /*
	 * command line parsing stuff
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
	public final CLOption cloSnapInterval = new CLOption("snapinterval", "0", EvoLudo.catSimulation,
			  "--snapinterval <n>  save snapshot every n generations (-1 only at end)",
    		new CLODelegate() {
		    	@Override
		    	public boolean parse(String arg) {
					snapinterval = CLOParser.parseLong(arg);
					return true;
		    	}
		    });
	public final CLOption cloSnapPrefix = new CLOption("snapprefix", "", EvoLudo.catSimulation,
			  "--snapprefix <s>  set prefix for snapshot filename",
  		new CLODelegate() {
		    	@Override
		    	public boolean parse(String arg) {
					snapprefix = new String(arg.trim());
					return true;
		    	}
		    });
	public final CLOption cloRuns = new CLOption("runs", "1", EvoLudo.catSimulation,
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
		    });
	public final CLOption cloScanST = new CLOption("scanST", "-1,2,0.05", EvoLudo.catSimulation,
			  "--scanST <start,end,incr>  scan S-T-plane",
		new CLODelegate() {
		    	@Override
		    	public boolean parse(String arg) {
		    		if( !cloScanST.isSet() )
		    			return true;
		    		scanST = CLOParser.parseVector(arg);
		    		if (scanST==null) {
		    			logger.warning("--scanST invalid argument '"+arg+"' - ignored.");
		    			// argument is not a vector; mark as not set
		    			cloScanST.reset();
		    		}
					return true;
		    	}
		    	@Override
		    	public void report(PrintStream output) {
		    		if( cloScanST.isSet() )
		    			output.println("# scan S-T-plane:       "+Formatter.format(scanST, 4));
		    	}
		    });
	public final CLOption cloScanDG = new CLOption("scanDG", "0,0.2,0.02", EvoLudo.catSimulation,
			  "--scanDG <start,end,incr>  scan cost-to-benefit ratios in the donation game",
		new CLODelegate() {
		    	@Override
		    	public boolean parse(String arg) {
		    		if( !cloScanDG.isSet() )
		    			return true;
		    		scanDG = CLOParser.parseVector(arg);
		    		if (scanDG==null) {
		    			logger.warning("--scanDG invalid argument '"+arg+"' - ignored.");
		    			// argument is not a vector; mark as not set
		    			cloScanDG.reset();
		    		}
					return true;
		    	}
		    	@Override
		    	public void report(PrintStream output) {
		    		if( cloScanDG.isSet() )
		    			output.println("# scan donation game:   "+Formatter.format(scanDG, 4));
		    	}
		    });
	public final CLOption cloScanDilemma = new CLOption("scanDilemma", "0.1,100", EvoLudo.catSimulation,
			  "--scanDilemma <radius,steps>  scan asymmetric social dilemmas, circle around S=0, T=1",
		new CLODelegate() {
		    	@Override
		    	public boolean parse(String arg) {
		    		if( !cloScanDilemma.isSet() )
		    			return true;
		    		scanDilemma = CLOParser.parseVector(arg);
		    		if (scanDilemma==null) {
		    			logger.warning("--scanDilemma invalid argument '"+arg+"' - ignored.");
		    			// argument is not a vector; mark as not set
		    			cloScanDilemma.reset();
		    		}
					return true;
		    	}
		    	@Override
		    	public void report(PrintStream output) {
		    		if( cloScanDilemma.isSet() )
		    			output.println("# scan asym. social:    "+Formatter.format(scanDilemma, 4));
		    	}
		    });

	@Override
	public void collectCLO(CLOParser parser) {
		parser.addCLO(cloProgress);
		parser.addCLO(cloSnapInterval);
		parser.addCLO(cloSnapPrefix);
		parser.addCLO(cloRuns);
		parser.addCLO(cloScanST);
		parser.addCLO(cloScanDG);
		parser.addCLO(cloScanDilemma);
		parser.addCLO(cloGyuri);

		engine.cloGenerations.setDefault("10");
		super.collectCLO(parser);
	}

	public void saveSnapshot(Mutualism pop, int format) {
		if( format==AbstractGraph.SNAPSHOT_NONE ) return;	// do not waste our time
		File snapfile;

		switch( format ) {
			case AbstractGraph.SNAPSHOT_PNG:
				int side = Math.max(16*(int)Math.sqrt(nPopulation), 1000);
				BufferedImage snapimage = MVPop2D.getSnapshot(engine, pop, side, side);
				snapfile = openSnapshot(pop, "png");
				try { 
					ImageIO.write(snapimage, "PNG", snapfile);
				}
				catch( IOException x ) {
					logger.warning("writing image to "+snapfile.getAbsolutePath()+" failed!");
					return;
				}
				logger.info("image written to "+snapfile.getAbsolutePath());
				return;

			default:
				logger.warning("unknown file format ("+format+")");
		}
	}

	protected File openSnapshot(Mutualism pop, String ext) {
//		String pre = getImagePrefix()+"-t"+ChHFormatter.format(getGeneration(), 2);
//		String pre = getImagePrefix()+
//		String pre = pop.getName()+
		String pre = (snapprefix.length()>0?snapprefix+"-"+pop.getName():getKey()+"-"+pop.getName()+"-"+
			"R"+Formatter.format(pop.getPayoff(COOPERATE, COOPERATE), 2)+
			"S"+Formatter.format(pop.getPayoff(COOPERATE, DEFECT), 2)+
			"T"+Formatter.format(pop.getPayoff(DEFECT, COOPERATE), 2)+
			"P"+Formatter.format(pop.getPayoff(DEFECT, DEFECT), 2))+
			"-t"+Formatter.format(engine.getModel().getTime(), 2);
		File snapfile = new File(pre+"."+ext);
		int counter = 0;
		while( snapfile.exists() )
			snapfile = new File(pre+"-"+(counter++)+"."+ext);
		return snapfile;
	}
}
