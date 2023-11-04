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
import org.evoludo.simulator.modules.DemesTBT;
import org.evoludo.util.CLOParser;
import org.evoludo.util.CLOption;
import org.evoludo.util.CLOption.CLODelegate;
import org.evoludo.util.Formatter;

/**
 *
 * @author Christoph Hauert
 */
public class simDemesTBT extends DemesTBT {

	// additional parameters - set defaults
	private long nSamples = 1000;
	private long nRuns = 1000;
	private boolean analytical = false;
//	private boolean verbose = false;

	// helper parameters/variables for analytical calculations
	double a, b, c, d;	// payoffs
	double w, v;		// selection strength, noise for replicator updates
	double B, Beff;		// (effective) baseline fitness
	double mu, nu;		// migration rate, mutation rate
	int	N, N1, D, M;	// deme size (N-1), number of demes, population size
	double iN, iN1, iN2, i2N2, iD, iM;	// 1/N, 1/(N-1), 1/N^2, 1/(2*N^2), 1/D, 1/M
	double fA, fB;		// fitness of A's and B's in a homogeneous A and B deme, respectively.
	double ifnorm;		// 1/(max fitness difference)
//	double t1, tN1;		// (unconditional) fixation times of single A (B) in one deme
//	double tau1, tauD1;	// (unconditional) fixation time of a single, homogeneous A (B) deme
//	double tau1A, tauD1B;	// conditional fixation time of a single, homogeneous A (B) deme
//	double fixA, fixB;	// fixation probability of single A (B) in deme structured populations
//	double timeA, timeB;	// fixation time of single A (B) in deme structured populations
	private int		switches, lasthomo, lasthomotau, lasthomodeme;
	private double	startgen, starttau;
//	private double	startdeme;
// variable names are inconsistently used for analytics and simulations... 
	private double	p1A, pN1B, t1, t1sq, tN1, tN1sq, t1A, t1Asq, tN1B, tN1Bsq;
	private int		t1n, tN1n, t1An, tN1Bn;
	private double	rhoA, rhoB;		// fixation probabilities of single A (B) in one deme
//	private double	time1, timeN1, time1A, timeN1B;	// fixation times of single A (B) in one deme
	private double	alpha, beta;		// (same as time1A, timeN1B above) conditional fixation times of single A (B) in one deme
	private double	baralpha, barbeta;	// average time of failed invasion attempt by a single A (B) in one deme
//	private int		time1n, timeN1n, time1An, timeN1Bn;
	private double	Phi1, tau1, tauD1, tau1A, tauD1B;
//	private double	PhiD1;
//	private int		tau1n, tauD1n, tau1An, tauD1Bn;

	public simDemesTBT(EvoLudo engine) {
		super(engine);
	}

	@Override
	public void exec() {
		setReportFreq(0.0);
//		modelReset();
		// print header
		engine.dumpParameters();
		nSamples *= nPopulation;

		if( analytical ) {
			// analytical results
			// init shortcuts
			M = nPopulation;
			iM = 1.0/M;
			N = sizeDemes;
			iN = 1.0/N;
			N1 = N-1;
			iN1 = 1.0/N1;
			iN2 = iN*iN;
			i2N2 = 0.5*iN2;
			D = nDemes;
			iD = 1.0/D;
			mu = pMigration;
			nu = pMutation;
			a = getPayoff(COOPERATE, COOPERATE);
			b = getPayoff(COOPERATE, DEFECT);
			c = getPayoff(DEFECT, COOPERATE);
			d = getPayoff(DEFECT, DEFECT);
			v = invThermalNoise;
			w = playerSelection;
			B = playerBaselineFitness;
			Beff = B*(1.0-w);
			ifnorm = w*(Math.max(Math.max(a, b), Math.max(c, d))-Math.min(Math.min(a, b), Math.min(c, d)));

			fA = fA(N);
			fB = fB(0);
			rhoA = rhoA();
			rhoB = rhoB();
			t1 = t1();
			tN1 = tN1();
			alpha = alpha();
			beta = beta();
			if( 10.0*alpha*mu>1.0 ) logger.warning("Based on the fixation time in one deme ("+Formatter.format(alpha, 6)+") the migration rate ("+Formatter.format(mu, 6)+") cannot be considered small!");
			baralpha = baralpha();
			barbeta = barbeta();
			Phi1 = Phi(1);
			tau1 = tau1();
			tauD1 = tauD1();
			tau1A = tau1A();
			tauD1B = tauD1B();
			double pfixA = rhoA*Phi1;
			double pfixB = rhoB*(1.0-Phi(D-1));
			double fixA = rhoA*(alpha+tau1)+(1.0-rhoA)*baralpha;
			double fixB = rhoB*(beta+tauD1)+(1.0-rhoB)*barbeta;
			double timeA = alpha+tau1A;
			double timeB = beta+tauD1B;
			if( 10.0*timeA*nu>1.0 ) logger.warning("Based on the time to fixation ("+Formatter.format(timeA, 6)+") the mutation rate ("+Formatter.format(nu, 6)+") cannot be considered small!");
			out.println("# (a, b, c, d) = ("+Formatter.format(a, 6)+", "+Formatter.format(b, 6)+", "+Formatter.format(c, 6)+", "+Formatter.format(d, 6)+")");
			if( verbose) {
				out.println("# indiv\trhoA\trhoB\tt1\ttN1\tt1A\ttN1B\t-t1A\t-tN1B");
				out.println(N+"\t"+
							   Formatter.format(rhoA, 6)+"\t"+
							   Formatter.format(rhoB, 6)+"\t"+
							   Formatter.format(t1, 6)+"\t"+
							   Formatter.format(tN1, 6)+"\t"+
							   Formatter.format(alpha, 6)+"\t"+
							   Formatter.format(beta, 6)+"\t"+
							   Formatter.format(baralpha, 6)+"\t"+
							   Formatter.format(barbeta, 6)
							   );
				out.println("# deme\tPhi1\tPhiD1\ttau1\ttauD1\ttau1A\ttauD1B");
				out.println(D+"\t"+
							   Formatter.format(Phi(1), 6)+"\t"+
							   Formatter.format(1.0-Phi(D-1), 6)+"\t"+
							   Formatter.format(tau1, 6)+"\t"+
							   Formatter.format(tauD1, 6)+"\t"+
							   Formatter.format(tau1A, 6)+"\t"+
							   Formatter.format(tauD1B, 6)
							   );
			}

			out.println("# pop\tdemes\tmeanA\tfixA\tfixB\ttimeA\ttimeB\tctimeA\tctimeB");
			out.println(nPopulation+"\t"+D+"\t"+
						   Formatter.format(pfixA/(pfixA+pfixB), 6)+"\t"+
						   Formatter.format(pfixA, 6)+"\t"+
						   Formatter.format(pfixB, 6)+"\t"+
						   Formatter.format(fixA, 6)+"\t"+
						   Formatter.format(fixB, 6)+"\t"+
						   Formatter.format(timeA, 6)+"\t"+
						   Formatter.format(timeB, 6));
		}
		else {
			// simulations
			// keep time-scales straight! - this the default for pMutation>0 anyways but just to make sure.
			optimizeMoran = false;
			if( nSamples>0 ) {
				for( long g=0; g<nSamples; g++ ) {
					double t = generation;
					engine.modelNext();
					double incr = generation-t;
					if( incr<=0.0 ) {
						throw new Error("Time suddenly stopped in generation "+Formatter.format(generation, 3));
					}
				}
			}
double tHomoDemesC = 0.0, tHomoDemesD = 0.0;
double tTotC = 0.0, tTotD = 0.0;
			if( nRuns>0 ) {
				double totGenerations = 0.0;
				setInitFreqs(new double[]{1.0, 0.0});
				for( long g=0; g<nRuns; g++ ) {
					modelDidReinit();
					engine.modelNext();	// this should introduce a mutant and hence make the population inhomogenous
double ts = generation;
double thd = 0.0;
//while( !isHomogeneousPopulation() ) {
while( !isMonomorphic() ) {
	if( strategiesTypeCount[COOPERATE]%sizeDemes==0 ) {
		// demes are homogenous
		double th = generation;
		engine.modelNext();
		thd += generation-th;
		continue;
	}
	engine.modelNext();
}
if( strategiesTypeCount[COOPERATE]==nPopulation ) {
	// mutant cooperator has fixated
	tHomoDemesC += thd;
	tTotC += generation-ts;
}
					totGenerations += generation;
				}
				setInitFreqs(new double[]{0.0, 1.0});
				for( long g=0; g<nRuns; g++ ) {
					modelDidReinit();
					engine.modelNext();	// this should introduce a mutant and hence make the population inhomogenous
double ts = generation;
double thd = 0.0;
//while( !isHomogeneousPopulation() ) {
while( !isMonomorphic() ) {
	if( strategiesTypeCount[COOPERATE]%sizeDemes==0 ) {
		// demes are homogenous
		double th = generation;
		engine.modelNext();
		thd += generation-th;
		continue;
	}
	engine.modelNext();
}
if( strategiesTypeCount[COOPERATE]==0 ) {
	// mutant defector has fixated
	tHomoDemesD += thd;
	tTotD += generation-ts;
}
					totGenerations += generation;
				}
				generation = totGenerations;
			}

			// rescale times
//			time1 *= nPopulation/nDemes;
//			timeN1 *= nPopulation/nDemes;
//			time1A *= nPopulation/nDemes;
//			timeN1B *= nPopulation/nDemes;
			tau1 *= nPopulation;
			tauD1 *= nPopulation;
			tau1A *= nPopulation;
			tauD1B *= nPopulation;
//			t1 *= nPopulation;
//			tN1 *= nPopulation;
//			t1A *= nPopulation;
//			tN1B *= nPopulation;
//			t1sq *= nPopulation*nPopulation;
//			tN1sq *= nPopulation*nPopulation;
//			t1Asq *= nPopulation*nPopulation;
//			tN1Bsq *= nPopulation*nPopulation;

/*			output.println("# (a, b, c, d) = ("+
				ChHFormatter.format(getPayoff(COOPERATE, COOPERATE), 6)+", "+
				ChHFormatter.format(getPayoff(COOPERATE, DEFECT), 6)+", "+
				ChHFormatter.format(getPayoff(DEFECT, COOPERATE), 6)+", "+
				ChHFormatter.format(getPayoff(DEFECT, DEFECT), 6)+")");
			if( verbose ) {
				output.println("# indiv\trhoA\trhoB\tt1\ttN1\tt1A\ttN1B");
				output.println(sizeDemes+"\t"+
							   ChHFormatter.format(rhoA/time1n, 6)+"\t"+
							   ChHFormatter.format(rhoB/timeN1n, 6)+"\t"+
							   ChHFormatter.format(time1/time1n, 6)+"\t"+
							   ChHFormatter.format(timeN1/timeN1n, 6)+"\t"+
							   ChHFormatter.format(time1A/time1An, 6)+"\t"+
							   ChHFormatter.format(timeN1B/timeN1Bn, 6)
							   );
				if(nDemes>1) {
					output.println("# deme\tPhi1\tPhiD1\ttau1\ttauD1\ttau1A\ttauD1B");
					output.println(nDemes+"\t"+
								   ChHFormatter.format(Phi1/tau1n, 6)+"\t"+
								   ChHFormatter.format(PhiD1/tauD1n, 6)+"\t"+
								   ChHFormatter.format(tau1/tau1n, 6)+"\t"+
								   ChHFormatter.format(tauD1/tauD1n, 6)+"\t"+
								   ChHFormatter.format(tau1A/tau1An, 6)+"\t"+
								   ChHFormatter.format(tauD1B/tauD1Bn, 6)
								   );
				}
			}*/

			double norm = 0;
			for( int n=0; n<=nPopulation; n++ ) norm += histogram[n];	// without optimization: norm = nSamples
			double m = 0.0, m2 = 0.0;
			for( int i=0; i<=nPopulation; i++ ) {
				double s = histogram[i]*i;
				m += s;
				m2 += s*i;
			}
			m /= norm*nPopulation;
			m2 /= norm*(nPopulation*nPopulation);
			String msg;
			if( nSamples>0 ) {
				out.println("# pop\tdemes\tmeanC\tsdevC\tpureD\tpureC\tp1\tpM1\tt1\tt1sdev\ttN1\ttN1sdev\tt1A\tt1Asdev\ttN1B\ttN1Bsdev");
				msg = nPopulation+"\t"+nDemes+"\t"+Formatter.format(m, 6)+"\t"+
					Formatter.format(Math.sqrt(m2-m*m), 6)+"\t"+
					Formatter.format(histogram[0]/norm, 6)+"\t"+
					Formatter.format(histogram[nPopulation]/norm, 6)+"\t";
			}
			else {
				out.println("# pop\tdemes\tp1\tpM1\tt1\tt1sdev\ttN1\ttN1sdev\tt1A\tt1Asdev\ttN1B\ttN1Bsdev");
				msg = nPopulation+"\t"+nDemes+"\t";
			}
			out.println(msg+Formatter.format(p1A/t1n, 6)+"\t"+
			    Formatter.format(pN1B/tN1n, 6)+"\t"+
				Formatter.format(t1/t1n, 6)+"\t"+Formatter.format(Math.sqrt((t1sq-(t1/t1n)*t1)/t1n), 6)+"\t"+			// t1
				Formatter.format(tN1/tN1n, 6)+"\t"+Formatter.format(Math.sqrt((tN1sq-(tN1/tN1n)*tN1)/tN1n), 6)+"\t"+	// tN1
				Formatter.format(t1A/t1An, 6)+"\t"+Formatter.format(Math.sqrt((t1Asq-(t1A/t1An)*t1A)/t1An), 6)+"\t"+	// t1A
				Formatter.format(tN1B/tN1Bn, 6)+"\t"+Formatter.format(Math.sqrt((tN1Bsq-(tN1B/tN1Bn)*tN1B)/tN1Bn), 6)	// tN1B
			);
//output.println("#CHECK: mean C="+(getStrategyTypeMean()[COOPERATE]/nPopulation)+", popsize="+nPopulation);
			if( nSamples>0 ) {
				msg = "#HIST ";
				for( int n=0; n<nPopulation; n++ )
					msg += Formatter.format(histogram[n]/norm, 6)+" \t";
				out.println(msg+Formatter.format(histogram[nPopulation]/norm, 6));
			}
else {
	out.println("#HOMO "+Formatter.formatSci(pMigration, 4)+"\t"+Formatter.format(pMigration*t1A/t1An, 6)+"\t"+Formatter.format(pMigration*tN1B/tN1Bn, 6)+"\t"+Formatter.format(tHomoDemesC/tTotC, 6)+"\t"+Formatter.format(tHomoDemesD/tTotD, 6));
}
			out.println("# switches: "+switches);
			out.println("# generations @ end: "+Formatter.formatSci(generation, 6));
		}
		engine.dumpEnd();
//		output.flush();
	}

	private double[] histogram = null;

	@Override
	public synchronized void reset() {
		super.reset();
		if( histogram==null || histogram.length!=(nPopulation+1) )
			histogram = new double[nPopulation+1];
		Arrays.fill(histogram, 0.0);
		switches = 0;
		lasthomo = 0;
		lasthomotau = 0;
		lasthomodeme = 0;
		startgen = 0.0;
		starttau = 0.0;
		p1A = pN1B = 0.0;
		t1 = t1sq = 0.0;
		tN1 = tN1sq = 0.0;
		t1A = t1Asq = 0.0;
		tN1B = tN1Bsq = 0.0;
		Phi1 = 0.0;
//		Phi1 = PhiD1 = 0.0;
		tau1 = tauD1 = 0.0;
		tau1A = tauD1B = 0.0;
//		tau1n = tauD1n = tau1An = tauD1Bn = 0;
		rhoA = rhoB = 0.0;
//		time1 = timeN1 = 0.0;
//		time1A = timeN1B = 0.0;
//		time1n = timeN1n = time1An = timeN1Bn = 0;
		t1n = tN1n = t1An = tN1Bn = 0;
	}

	@Override
	protected void updateStatistics(double time) {
		if (prevsample>=time)
			return;
		histogram[strategiesTypeCount[COOPERATE]] += time-prevsample;
		super.updateStatistics(time);
	}

	private boolean absorbed = true;
	private boolean tauabsorbed = false;
	private boolean homodemes = true;

	// called after every single update - more overhead, more accuracy
	@Override
	public void commitStrategyAt(int me) {
		int newtype = strategiesScratch[me]%nTraits;
		int oldtype = strategies[me]%nTraits;
		super.commitStrategyAt(me);
		if( newtype==oldtype ) return;	// nothing changed

		int coop = strategiesTypeCount[COOPERATE];
		int nA = homoDemes(coop);

		// further statistical measurements for debugging
		if( verbose ) {
			if( nA>=0 ) {
				if( !homodemes) {
					// new number of homogeneous demes
//					double tincr = generation-startdeme;
					// this is hackish - for some reason sometimes the count of A types has changed by more than one...
					if( lasthomodeme%sizeDemes>sizeDemes/2 ) {
						// single B type was present
//						timeN1 += tincr;
//						timeN1n++;
						if( lasthomodeme>coop ) {
							// one more B demes
							rhoB++;
//							timeN1B += tincr;
//							timeN1Bn++;
						}
					}
					else {
						// single A type was present
//						time1 += tincr;
//						time1n++;
						if( lasthomodeme<coop ) {
							// one more A demes
							rhoA++;
//							time1A += tincr;
//							time1An++;
						}
						// one A in a B deme is the same as one B in an A deme for N=2.
						if( sizeDemes==2 ) {
//							timeN1 += tincr;
//							timeN1n++;
							if( lasthomodeme>coop ) {
								// one more B demes
								rhoB++;
//								timeN1B += tincr;
//								timeN1Bn++;
							}
						}
					}
					homodemes = true;
				}
			}
			else {
				if( homodemes ) {
					lasthomodeme = coop;
//					startdeme = generation;
				}
				homodemes = false;
			}
		}
		if( nA==0 || nA==nDemes ) {
			if( !absorbed ) {
				// absorbed
				double tincr = generation-startgen;
				double tincr2 = tincr*tincr;
				if( lasthomo==0 ) {
					// started with single A
					t1 += tincr;
					t1sq += tincr2;
					t1n++;
					if( nA>0 ) {
						// the single A fixated
						p1A++;
						t1A += tincr;
						t1Asq += tincr2;
						t1An++;
						switches++;
					}
				}
				else {
					// started with single B
					tN1 += tincr;
					tN1sq += tincr2;
					tN1n++;
					if( nA==0 ) {
						// the single B fixated
						pN1B++;
						tN1B += tincr;
						tN1Bsq += tincr2;
						tN1Bn++;
						switches++;
					}
				}
				absorbed = true;
				lasthomo = nA;
			}

			// further statistical measurements for debugging
			if( verbose ) {
				if( !tauabsorbed ) {
					double tauincr = generation-starttau;
					if( lasthomotau==1 ) {
						// started with one A deme
						tau1 += tauincr;
//						tau1n++;
						if( nA>0 ) {
							// A's fixated
							tau1A += tauincr;
//							tau1An++;
							Phi1++;
						}
					}
					else {
						// started with one B deme
						tauD1 += tauincr;
//						tauD1n++;
						if( nA==0 ) {
							// B's fixated
							tauD1B += tauincr;
//							tauD1Bn++;
//							PhiD1++;
						}
					}
					tauabsorbed = true;
				}
			}
			return;
		}

		if( coop==1 || coop==nPopulation-1 ) {
			if( !absorbed ) return;
			absorbed = false;
			startgen = generation;
			// if we reinit population this ensures that lasthomo reflects the right starting point.
			lasthomo = (coop==1?0:nPopulation);
			return;
		}

		// further statistical measurements for debugging
		if( verbose ) {
			if( nA==1 || nA==nDemes-1 ) {
				if( !tauabsorbed ) return;
				tauabsorbed = false;
				starttau = generation;
				lasthomotau = nA;
				return;
			}
		}
		return;
	}

	// utility methods for analytical calculations
	public double fA(int i) {
		double piA = ((i-1)*a+(N-i)*b)*iN1;
		return Beff+w*piA;
	}

	public double fB(int i) {
		double piB = (i*c+(N1-i)*d)*iN1;
		return Beff+w*piB;
	}

	public double Tplus(int i) {
		double fAi = fA(i);
		double fBi = fB(i);
		switch( populationUpdateType ) {
			case MORAN_BIRTHDEATH:
			case MORAN_DEATHBIRTH:
				return (N-i)*i*fAi/(N*(i*fAi+(N-i)*fBi));

			case ASYNC:
				switch( playerUpdateType ) {
					case IMITATE:
						return (N-i)*i*i2N2*(1.0+(fAi-fBi)*ifnorm);
						
					case THERMAL:
						return (N-i)*i*iN2/(2.0+Math.expm1((fBi-fAi)*v));
				}
		}
		return -1.0;
	}
	
	public double Tminus(int i) {
		double fAi = fA(i);
		double fBi = fB(i);
		switch( populationUpdateType ) {
			case MORAN_BIRTHDEATH:
			case MORAN_DEATHBIRTH:
				return i*(N-i)*fBi/(N*(i*fAi+(N-i)*fBi));
				
			case ASYNC:
				switch( playerUpdateType ) {
					case IMITATE:
						return i*(N-i)*i2N2*(1.0+(fBi-fAi)*ifnorm);
						
					case THERMAL:
						return i*(N-i)*iN2/(2.0+Math.expm1((fAi-fBi)*v));
				}
		}
		return -1.0;
	}
	
	public double Tratio(int i) {
		double fAi = fA(i);
		double fBi = fB(i);
		switch( populationUpdateType ) {
			case MORAN_BIRTHDEATH:
			case MORAN_DEATHBIRTH:
				return fBi/fAi;
				
			case ASYNC:
				switch( playerUpdateType ) {
					case IMITATE:
						return (ifnorm+(fBi-fAi))/(ifnorm+(fAi-fBi));
						
					case THERMAL:
						return (2.0+Math.expm1((fBi-fAi)*v))/(2.0+Math.expm1((fAi-fBi)*v));
				}
		}
		return -1.0;
	}
	
	public double Toitar(int i) {
		double fAi = fA(i);
		double fBi = fB(i);
		switch( populationUpdateType ) {
			case MORAN_BIRTHDEATH:
			case MORAN_DEATHBIRTH:
				return fAi/fBi;
				
			case ASYNC:
				switch( playerUpdateType ) {
					case IMITATE:
						return (ifnorm+(fAi-fBi))/(ifnorm+(fBi-fAi));
						
					case THERMAL:
						return (2.0+Math.expm1((fAi-fBi)*v))/(2.0+Math.expm1((fBi-fAi)*v));
				}
		}
		return -1.0;
	}
	
	public double rhoA(int i) {
		double num = 1.0;
		for( int k=1; k<i; k++ ) {
			double prod = 1.0;
			for( int j=1; j<=k; j++ ) {
				prod *= Tratio(j);
			}
			num += prod;
		}
		double denom = num;
		for( int k=i; k<N; k++ ) {
			double prod = 1.0;
			for( int j=1; j<=k; j++ ) {
				prod *= Tratio(j);
			}
			denom += prod;
		}
		return num/denom;
	}

	public double rhoA() {
		return rhoA(1);
	}

	// requires rhoA
	public double t1() {
		double sum = 0.0;
		for( int l=1; l<N; l++ ) {
			double sumprod = 0.0;
			for( int k=l; k<N; k++ ) {
				double prod = 1.0;
				for( int m=l+1; m<=k; m++ ) {
					prod *= Tratio(m);
				}
				sumprod += prod;
			}
			sum += sumprod/Tplus(l);
		}
		return sum*rhoA;
	}
	
	public double alpha() {
		double sum = 0.0;
		for( int l=1; l<N; l++ ) {
			double sumprod = 0.0;
			for( int k=l; k<N; k++ ) {
				double prod = 1.0;
				for( int m=l+1; m<=k; m++ ) {
					prod *= Tratio(m);
				}
				sumprod += prod;
			}
			sum += sumprod*rhoA(l)/Tplus(l);
		}
		return sum;
	}
	
	// requires alpha, t1 and rhoA
	public double baralpha() {
		return (t1-rhoA*alpha)/(1.0-rhoA);
	}

	public double rhoB(int i) {
		return 1.0-rhoA(i);
	}
	
	public double rhoB() {
		return rhoB(N1);
	}
	
	// requires rhoB
	public double tN1() {
		double sum = 0.0;
		for( int l=1; l<N; l++ ) {
			double sumprod = 0.0;
			for( int k=l; k<N; k++ ) {
				double prod = 1.0;
				for( int m=l+1; m<=k; m++ ) {
					prod *= Toitar(N-m);
				}
				sumprod += prod;
			}
			sum += sumprod/Tminus(N-l);
		}
		return sum*rhoB;
	}
	
	public double beta() {
		double sum = 0.0;
		for( int l=1; l<N; l++ ) {
			double sumprod = 0.0;
			for( int k=l; k<N; k++ ) {
				double prod = 1.0;
				for( int m=l+1; m<=k; m++ ) {
					prod *= Toitar(N-m);
				}
				sumprod += prod;
			}
			sum += sumprod*rhoB(N-l)/Tminus(N-l);
		}
		return sum;
	}
	
	// requires beta, tN1 and rhoB
	public double barbeta() {
		return (tN1-rhoB*beta)/(1.0-rhoB);
	}

	// requires fA, fB
	public double pA(int i) {
		return i*fA/(i*fA+(D-i)*fB);
	}

	// requires rhoA
	public double Qplus(int i) {
		double pAi = pA(i);
		double pBi = 1.0-pAi;
		return rhoA*(nu*pBi+mu*pAi*(D-i)*iD);
	}

	// requires rhoB
	public double Qminus(int i) {
		double pAi = pA(i);
		double pBi = 1.0-pAi;
		return rhoB*(nu*pAi+mu*pBi*i*iD);
	}
	
	// requires fA, fB, rhoA, rhoB
	public double Qratio(int i) {
		return rhoB*i*(nu*fA*D+mu*(D-i)*fB)/(rhoA*(D-i)*(nu*fB*D+mu*i*fA));
	}
	
	// requires fA, fB, rhoA, rhoB
	public double Qoitar(int i) {
		return rhoA*(D-i)*(nu*fB*D+mu*i*fA)/(rhoB*i*(nu*fA*D+mu*(D-i)*fB));
	}
	
	// requires rhoA and rhoB
	// fix prob of A
	public double Phi(int i) {
		if( i==0 ) return 0.0;	// B has fixated.
		if( i==D ) return 1.0;	// A has fixated.
		double num = 1.0;
		for( int k=1; k<i; k++ ) {
			double prod = 1.0;
			for( int j=1; j<=k; j++ ) {
				prod *= Qratio(j);
			}
			num += prod;
		}
		double denom = num;
		for( int k=i; k<D; k++ ) {
			double prod = 1.0;
			for( int j=1; j<=k; j++ ) {
				prod *= Qratio(j);
			}
			denom += prod;
		}
		return num/denom;
	}

	// requires rhoA, rhoB, baralpha, barbeta
	public double gamma(int i) {
		double pAi = pA(i);
		double pBi = 1.0-pAi;
		return pAi*((mu*(D-i)*iD*(1.0-rhoA)+nu*(1.0-rhoB))*(baralpha+1.0)+mu*i*iD)+
			   pBi*((mu*i*iD*(1.0-rhoB)+nu*(1.0-rhoA))*(barbeta+1.0)+mu*(D-i)*iD)+1.0-nu-mu;
	}

	// requires Phi1, alpha, beta
	public double tau1() {
		double sum = 0.0;
		for( int l=1; l<D; l++ ) {
			double sumprod = 0.0;
			for( int k=l; k<D; k++ ) {
				double prod = 1.0;
				for( int m=l+1; m<=k; m++ ) {
					prod *= Qratio(m);
				}
				sumprod += prod;
			}
			sum += (alpha+beta+gamma(l)/Qplus(l))*sumprod;
		}
		return (1.0-D*Phi1)*beta+Phi1*sum;
	}

	// requires alpha, beta
	public double tauD1() {
		double sum1 = 0.0;
		double sum2 = 0.0;
		for( int l=1; l<D; l++ ) {
			double sumprod = 0.0;
			double prod = 0.0;
			for( int k=l; k<D; k++ ) {
				prod = 1.0;
				for( int m=l+1; m<=k; m++ ) {
					prod *= Qratio(m);
				}
				sumprod += prod;
			}
			double fac = alpha+beta+gamma(l)/Qplus(l);
			sum1 += fac*sumprod;
			sum2 += fac*prod;
		}
		double barPhiD1 = 1.0-Phi(D-1);
		return (D*barPhiD1-1.0)*beta-barPhiD1*sum1+sum2;
	}

	// requires alpha, beta, gamma (rhoA, rhoB, baralpha, barbeta)
	public double tau1A() {
		double sum1 = 0.0;
		double sum2 = 0.0;
		for( int l=1; l<D; l++ ) {
			double sumprod = 0.0;
			for( int k=l; k<D; k++ ) {
				double prod = 1.0;
				for( int m=l+1; m<=k; m++ ) {
					prod *= Qratio(m);
				}
				sumprod += prod;
			}
			double phil = Phi(l);
			sum1 += phil*(alpha*Qratio(l)+beta+gamma(l)/Qplus(l))*sumprod;
			sum2 += phil;
		}
		return sum1-(alpha-beta)*sum2;
	}

	// requires alpha, beta, gamma (rhoA, rhoB, baralpha, barbeta)
	public double tauD1B() {
		double sum1 = 0.0;
		double sum2 = 0.0;
		for( int l=1; l<D; l++ ) {
			double sumprod = 0.0;
			for( int k=l; k<D; k++ ) {
				double prod = 1.0;
				for( int m=l+1; m<=k; m++ ) {
					prod *= Qoitar(D-m);
				}
				sumprod += prod;
			}
			int Dl = D-l;
			double barPhiDl = 1.0-Phi(Dl);
			sum1 += barPhiDl*(alpha+beta*Qoitar(Dl)+gamma(Dl)/Qminus(Dl))*sumprod;
			sum2 += barPhiDl;
		}
		return sum1-(alpha-beta)*sum2;
	}

	/*
	 * command line parsing stuff
	 */
	public final CLOption cloSamples = new CLOption("samples", 's', EvoLudo.catSimulation, "1000",
			"--samples, -s <s>     number of samples",
    		new CLODelegate() {
		    	@Override
		    	public boolean parse(String arg) {
					nSamples = CLOParser.parseLong(arg);
					nRuns = 0;
					return true;
		    	}
		    	@Override
		    	public void report(PrintStream output) {
		    		output.println("# samples:              "+nSamples);
		    	}
		    });
	public final CLOption cloRuns = new CLOption("runs", 'r', EvoLudo.catSimulation, "1000",
			"--runs, -r<r>        number of repetitions",
			new CLODelegate() {
		    	@Override
		    	public boolean parse(String arg) {
					nRuns = CLOParser.parseLong(arg);
					nSamples = 0;
					return true;
		    	}
		    	@Override
		    	public void report(PrintStream output) {
		    		output.println("# runs:                 "+nRuns);
		    	}
		    });
	public final CLOption cloAnalytical = new CLOption("analytical", 'a', EvoLudo.catSimulation, CLOption.Argument.NONE, "noanalytical",
			"--analytical, -a      analytical calculations (instead of simulations)",
    		new CLODelegate() {
		    	@Override
		    	public boolean parse(String arg) {
					analytical = cloAnalytical.isSet();
					return true;
		    	}
		    	@Override
		    	public void report(PrintStream output) {
		    		output.println("# analytical:           "+analytical);
		    	}
		    });
	public final CLOption cloProgress = new CLOption("progress", EvoLudo.catSimulation, CLOption.Argument.NONE, "noverbose",
			"--progress           make noise about progress",
    		new CLODelegate() {
		    	@Override
		    	public boolean parse(String arg) {
					verbose = cloProgress.isSet();
					return true;
		    	}
		    });
 
    // override this method in subclasses to add further command line options
	// subclasses must make sure that they include a call to super
	@Override
	public void collectCLO(CLOParser parser) {
		parser.addCLO(cloSamples);
		parser.addCLO(cloRuns);
		parser.addCLO(cloAnalytical);
		parser.addCLO(cloProgress);

		super.collectCLO(parser);
		parser.removeCLO("generations");
	}
}
