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

package org.evoludo.simulator.exec;

import java.io.File;
import java.io.PrintStream;
import java.util.Arrays;

import org.evoludo.graphics.AbstractGraph;
import org.evoludo.simulator.EvoLudoJRE;
import org.evoludo.simulator.MVPop2D;
import org.evoludo.math.ArrayMath;
import org.evoludo.math.Distributions;
import org.evoludo.simulator.EvoLudo;
import org.evoludo.simulator.Geometry;
import org.evoludo.simulator.models.IBS;
import org.evoludo.simulator.models.IBSCPopulation;
import org.evoludo.simulator.modules.CSD;
import org.evoludo.util.CLOParser;
import org.evoludo.util.CLOption;
import org.evoludo.util.CLOption.CLODelegate;
import org.evoludo.util.Formatter;

/**
 *
 * @author Christoph Hauert
 */
public class scanCSD extends CSD {

	/* additional parameters */
	double	b1Start = -Double.MAX_VALUE, b1End = -Double.MAX_VALUE, b1Incr = Double.MAX_VALUE;
	boolean	b1Log = false;
	double	b2Start = -Double.MAX_VALUE, b2End = -Double.MAX_VALUE, b2Incr = Double.MAX_VALUE;
	boolean	b2Log = false;
	double	c1Start = -Double.MAX_VALUE, c1End = -Double.MAX_VALUE, c1Incr = Double.MAX_VALUE;
	boolean	c1Log = false;
	double	c2Start = -Double.MAX_VALUE, c2End = -Double.MAX_VALUE, c2Incr = Double.MAX_VALUE;
	boolean	c2Log = false;
	int		nBins = 100;
//	int		timeWindow = 100;
//	boolean classify = false;
	boolean	printDistr = false;
	int snapinterval = 0;
	boolean	progress = false;
	IBSCPopulation pop;
	PrintStream out;

	public scanCSD(EvoLudo engine) {
		super(engine);
	}
	
	@Override
	public void run() {
		out = ((EvoLudoJRE)engine).getOutput();		
		// assumes IBS simulations
		pop = (IBSCPopulation) ((IBS) engine.getModel()).getSpecies(this);
		double[] bparams = traits2payoff.getBenefitParameters()[0];
		double[] cparams = traits2payoff.getCostParameters()[0];

		// initialize
		double iMean = getInit()[0][TRAIT_MEAN];
		boolean initHigh = false;

		// print header
		engine.dumpParameters();

		// print result legend
		out.println("# tend		b1		b2		c1		c2		mean	sdev	type");

		long prev = progress?System.currentTimeMillis():0L;
		final int SAMPLES = 11; 
		double reportFreq = engine.getReportInterval();
		double nGenerations = engine.getNGenerations();//-(SAMPLES-1)*reportFreq;
		// determine normalized thresholds for convergence to extremal trait values
		// note: for small mutation rates the threshold of 2*sdev is too conservative
		double lowMonoThreshold = Math.max(0.01, 2.0 * getMutationSdev()[0] / (getTraitMax()[0] - getTraitMin()[0]));
		double highMonoThreshold = 1.0 - lowMonoThreshold;
		double lowmean = -1.0, lowstdev = -1.0;
		double[] lowstatistics = null;
		double b1 = b1Start;
		boolean isBistable = false;
		while( Math.abs(b1)-Math.abs(b1End)<1e-4 ) {
			double b2 = b2Start;
			while( Math.abs(b2)-Math.abs(b2End)<1e-4 ) {
				double c1 = c1Start;
				while( Math.abs(c1)-Math.abs(c1End)<1e-4 ) {
					double c2 = c2Start;
					while( Math.abs(c2)-Math.abs(c2End)<1e-4 ) {
						// set parameters
						bparams[0] = b1;
						bparams[1] = b2;
						cparams[0] = c1;
						cparams[1] = c2;
						traits2payoff.setBenefitParameters(bparams);
						traits2payoff.setCostParameters(cparams);
						// initialize population
						double[] myinit = getInit(0);
						if( initHigh )
							myinit[TRAIT_MEAN] = getTraitMax()[0]-iMean;
						else
							myinit[TRAIT_MEAN] = iMean;
						setInit(myinit, 0);
						engine.modelReset();

						// evolve population
						engine.modelRelax();
						while( model.getTime()<nGenerations ) {
							engine.modelNext();
							int g = (int)model.getTime();
							if( snapinterval>0 && g%snapinterval==0 ) {
								// save snapshot
								saveSnapshot();
							}
							if( progress ) {
								long now = System.currentTimeMillis();
								if( now-prev>1000 || g%1000==0 ) {
									prev = now;
									engine.logProgress(g + "/" + (nGenerations+(SAMPLES-1)*reportFreq) + " done");
								}
							}
							// to speed things up, check every 1000 generations whether trait minimum or maximum has been reached
							// more precisely, whether mean trait <lowMonoThreshold or >highMonoThreshold
							if( g%1000==0 ) {
								double mean = Distributions.mean(pop.strategies);
								double tmin = getTraitMin()[0];
								double tmax = getTraitMax()[0];
								if( mean<lowMonoThreshold && ArrayMath.max(pop.strategies)<tmin+0.1*(tmax-tmin) )
									break;
								if( mean>highMonoThreshold && ArrayMath.min(pop.strategies)<tmax-0.1*(tmax-tmin) )
									break;
							}
						}
						// analyze population
						String msg;
						// - create histogram (potentially after averaging over several generations)
						// - calculate statistical quantities (potentially over several generations)
						double[] statistics = new double[SAMPLES*nPopulation];
						System.arraycopy(pop.strategies, 0, statistics, 0, nPopulation);
						for( int n=1; n<SAMPLES; n++ ) {
							engine.modelNext();
							System.arraycopy(pop.strategies, 0, statistics, n*nPopulation, nPopulation);
						}
						double mean = Distributions.mean(statistics);
						double stdev = Distributions.stdev(statistics, mean);
						if( snapinterval<0 )
							saveSnapshot();
						if( mean<lowMonoThreshold ) {
							// investments converged to zero
							if( !initHigh ) {
								// check for bi-stability by starting with high investment levels
								initHigh = true;
								lowmean = mean;
								lowstdev = stdev;
								lowstatistics = statistics;
								continue;
							}
							// monomorphic state, minimum investments
							msg = Formatter.formatFix(mean, 6)+"\t"+Formatter.formatFix(stdev, 6)+"\tmonomorphic";
						}
						else if ( mean>highMonoThreshold ) {
							if( initHigh ) {
								// must be bistable, otherwise initHigh would be false
								msg = "-2\t-1\tbistable";
								if( snapinterval<0 )
									saveSnapshot();
								isBistable = true;
							}
							else {
								// monomorphic state, maximum investments
								msg = Formatter.formatFix(mean, 6)+"\t"+Formatter.formatFix(stdev, 6)+"\tmonomorphic";
							}
						}
						else {
							if( initHigh ) {
								// low initial investments converged to zero; high initial investments did not stay high
								// hence likely insufficient time to converge to low investments 
								// note: argument only works for linear or quadratic cost/benefit functions where traits
								// always converge to extreme values except for ESS
								// report results for first run because more reliable
								msg = Formatter.formatFix(lowmean, 6)+"\t"+Formatter.formatFix(lowstdev, 6)+"\tmonomorphic";
								statistics = lowstatistics;
							}
							else {
								// intermediate investment levels
								double bimodal = Distributions.bimodality(statistics, mean);
								if( bimodal>5.0/9.0 ) {
									// likely bi-modal distribution
									msg = Formatter.formatFix(-mean, 6)+"\t"+Formatter.formatFix(stdev, 6)+
											"\tbranching ("+Formatter.format(bimodal, 6)+")";
								}
								else {
									// likely monomorphic distribution
									msg = Formatter.formatFix(mean, 6)+"\t"+Formatter.formatFix(stdev, 6)+
											"\tmonomorphic ("+Formatter.format(bimodal, 6)+")";
								}
							}
						}

						// print results
						// "# b1     b2	c1	c2		mean	sdev	type"
						out.println((int)model.getTime()+"\t"+Formatter.format(bparams[0], 4)+"\t"+Formatter.format(bparams[1], 4)+"\t"+
								Formatter.format(cparams[0], 4)+"\t"+Formatter.format(cparams[1], 4)+"\t"+
								msg);

						if( printDistr )
							printState(statistics, isBistable ? lowstatistics : null);
//						if( snapinterval<0 ) saveSnapshot();
//DEBUG
//double n = strategies.length;
//double tot = ChHMath.norm(strategies);
//double m1 = ChHMath.centralMoment(strategies, 1);
//double m2 = ChHMath.centralMoment(strategies, mean, 2);
//double m3 = ChHMath.centralMoment(strategies, mean, 3);
//double m4 = ChHMath.centralMoment(strategies, mean, 4);
//double sdev = Math.sqrt(m2);
//double skew = m3/ChHMath.pow(sdev, 3);
//double ekurt = m4/(m2*m2)-3.0;
//double bimod = (skew*skew+1)/(ekurt+3*(n-1)*(n-1)/((n-2)*(n-3)));
//msg =  "tot:  "+ChHFormatter.format(tot, 6)+"\n";
//msg += "n:   "+ChHFormatter.format(n, 0)+"\n";
//msg += "m1:    "+ChHFormatter.format(m1, 6)+"\n";
//msg += "m2:    "+ChHFormatter.format(m2, 6)+"\n";
//msg += "m3:    "+ChHFormatter.format(m3, 6)+"\n";
//msg += "m4:    "+ChHFormatter.format(m4, 6)+"\n";
//msg += "mean:  "+ChHFormatter.format(mean, 6)+"\n";
//msg += "stdev: "+ChHFormatter.format(sdev, 6)+"\t("+ChHFormatter.format(ChHMath.stdev(strategies), 6)+")\n";
//msg += "skew:  "+ChHFormatter.format(skew, 6)+"\t("+ChHFormatter.format(ChHMath.skewness(strategies), 6)+")\n";
//msg += "ekurt: "+ChHFormatter.format(ekurt, 6)+"\t("+ChHFormatter.format(ChHMath.kurtosis(strategies)-3.0, 6)+")\n";
//msg += "bimod: "+ChHFormatter.format(bimod, 6)+"\t("+ChHFormatter.format(ChHMath.bimodality(strategies), 6)+")\n";
//msg += "{ "+ChHFormatter.format(strategies[0], 6);
//for( int i=1; i<nPopulation; i++ ) msg += ", "+strategies[i];
//msg += " }";
//output.println(msg);
//ENDDEBUG
						// reset stuff
						initHigh = false;
						statistics = null;
						lowmean = -1;
						lowstdev = -1;
						lowstatistics = null;
						isBistable = false;
						// next parameter set
						if( Double.isNaN(c2Incr) )
							break;
						c2 = (c2Log?c2*c2Incr:c2+c2Incr);
					}
					if( Double.isNaN(c1Incr) )
						break;
					c1 = (c1Log?c1*c1Incr:c1+c1Incr);
				}
				if( Double.isNaN(b2Incr) )
					break;
				b2 = (b2Log?b2*b2Incr:b2+b2Incr);
			}
			if( Double.isNaN(b1Incr) )
				break;
			b1 = (b1Log?b1*b1Incr:b1+b1Incr);
		}
		engine.dumpEnd();
	}

	public void printState(double[] statistics, double[] lowstatistics) {
		double[] bins = new double[nBins+1];
		double scale = nBins;
		String msg = "# "+(int)engine.getModel().getTime()+":";
		if (lowstatistics != null) {
			int nSamples = lowstatistics.length;
			double incr = 1.0/nSamples;
			for( int i=0; i<nSamples; i++ )
				bins[(int)(lowstatistics[i]*scale+0.5)] += incr;
			for( int n=0; n<=nBins; n++ )
				msg += "\t"+bins[n];
			msg += "\n# ";
		}
		int nSamples = statistics.length;
		double incr = 1.0/nSamples;
		Arrays.fill(bins, 0);
		for( int i=0; i<nSamples; i++ )
			bins[(int)(statistics[i]*scale+0.5)] += incr;
		for( int n=0; n<=nBins; n++ )
			msg += "\t"+bins[n];
		out.println(msg);
	}

//	public void printState() {
//		double[] bins = new double[nBins+1];
//		getTraitHistogramData(bins);
//		String msg = "# "+(int)generation+":";
//		for( int n=0; n<=nBins; n++ ) msg += "\t"+bins[n];
//		output.println(msg);
//	}

	/*
	 * command line parsing stuff
	 */
	public final CLOption cloC1 = new CLOption("c1", "4.56", EvoLudo.catSimulation,
			"--c1<l>,<u>[,<i>[l]]  range of c1 cost parameter [l,u], increment i, append l for logarithmic increments",
			new CLODelegate() {
		    	@Override
		    	public boolean parse(String arg) {
					String[] argv = arg.trim().split(CLOParser.VECTOR_DELIMITER);
					switch( argv.length ) {
						case 1:
							c1Start = CLOParser.parseDouble(argv[0]);
							c1End = Double.MAX_VALUE;
							c1Incr = Double.NaN;
							return true;
						case 2:
							c1Start = CLOParser.parseDouble(argv[0]);
							c1End = CLOParser.parseDouble(argv[1]);
							c1Incr = (c1End-c1Start)*0.1;
							return true;
						default:
							logger.warning("too many arguments for c1 ("+arg+") - ignored.");
							return false;
						case 3:
							double start = CLOParser.parseDouble(argv[0]);
							double end = CLOParser.parseDouble(argv[1]);
							double incr;
							boolean log = false;
							if( argv[2].endsWith("l") ) {
								log = true;
								// ignore sign (<0 is meaningless)
								incr = Math.abs(CLOParser.parseDouble(argv[2].substring(0, argv[2].length()-1)));
								if( (Math.abs(end)<Math.abs(start) && incr>1.0) || (Math.abs(end)>Math.abs(start) && incr<1.0) ) {
									logger.warning("invalid logarithmic increment for c1-range ["+Formatter.format(start, 3)+", "+
											Formatter.format(end, 3)+"] ("+incr+(incr<1.0?">":"<")+"1 should hold) - ignored.");
									return false;
								}
							}
							else {
								// ignore specified sign; set sign of increment based on start and end
								incr = Math.abs(CLOParser.parseDouble(argv[2]));
								if( end<start ) incr = -incr;
							}
							c1Start = start;
							c1End = end;
							c1Incr = incr;
							c1Log = log;
							return true;
					}
		    	}
		    	@Override
		    	public void report(PrintStream output) {
		    		output.println((Double.isNaN(c1Incr)?
		    			"# c1:                   "+Formatter.format(c1Start, 4):
		    			"# c1range:              "+Formatter.format(c1Start, 4)+" - "+Formatter.format(c1End, 4)+
		    				": "+Formatter.format(c1Incr, 4)+(c1Log?" (logarithmic)":"")));
		    	}
		    });
	public final CLOption cloC2 = new CLOption("c2", "-1.6", EvoLudo.catSimulation,
			"--c2<l>,<u>[,<i>[l]]  range of c2 cost parameter [l,u], increment i, append l for logarithmic increments",
			new CLODelegate() {
		    	@Override
		    	public boolean parse(String arg) {
					String[] argv = arg.trim().split(CLOParser.VECTOR_DELIMITER);
					switch( argv.length ) {
						case 1:
							c2Start = CLOParser.parseDouble(argv[0]);
							c2End = Double.MAX_VALUE;
							c2Incr = Double.NaN;
							return true;
						case 2:
							c2Start = CLOParser.parseDouble(argv[0]);
							c2End = CLOParser.parseDouble(argv[1]);
							c2Incr = (c2End-c2Start)*0.1;
							return true;
						default:
							logger.warning("too many arguments for c2 ("+arg+") - ignored.");
							return false;
						case 3:
							double start = CLOParser.parseDouble(argv[0]);
							double end = CLOParser.parseDouble(argv[1]);
							double incr;
							boolean log = false;
							if( argv[2].endsWith("l") ) {
								log = true;
								// ignore sign (<0 is meaningless)
								incr = Math.abs(CLOParser.parseDouble(argv[2].substring(0, argv[2].length()-1)));
								if( (Math.abs(end)<Math.abs(start) && incr>1.0) || (Math.abs(end)>Math.abs(start) && incr<1.0) ) {
									logger.warning("invalid logarithmic increment for c2-range ["+Formatter.format(start, 3)+", "+
											Formatter.format(end, 3)+"] ("+incr+(incr<1.0?">":"<")+"1 should hold) - ignored.");
									return false;
								}
							}
							else {
								// ignore specified sign; set sign of increment based on start and end
								incr = Math.abs(CLOParser.parseDouble(argv[2]));
								if( end<start ) incr = -incr;
							}
							c2Start = start;
							c2End = end;
							c2Incr = incr;
							c2Log = log;
							return true;
					}
		    	}
		    	@Override
		    	public void report(PrintStream output) {
		    		output.println((Double.isNaN(c2Incr)?
		    			"# c2:                   "+Formatter.format(c2Start, 4):
		    			"# c2range:              "+Formatter.format(c2Start, 4)+" - "+Formatter.format(c2End, 4)+
		    				": "+Formatter.format(c2Incr, 4)+(c2Log?" (logarithmic)":"")));
		    	}
		    });

	public final CLOption cloB1 = new CLOption("b1", "6.0", EvoLudo.catSimulation,
			"--b1<l>,<u>[,<i>[l]]  range of b1 benefit parameter [l,u], increment i, append l for logarithmic increments",
			new CLODelegate() {
		    	@Override
		    	public boolean parse(String arg) {
					String[] argv = arg.trim().split(CLOParser.VECTOR_DELIMITER);
					switch( argv.length ) {
						case 1:
							b1Start = CLOParser.parseDouble(argv[0]);
							b1End = Double.MAX_VALUE;
							b1Incr = Double.NaN;
							return true;
						case 2:
							b1Start = CLOParser.parseDouble(argv[0]);
							b1End = CLOParser.parseDouble(argv[1]);
							b1Incr = (b1End-b1Start)*0.1;
							return true;
						default:
							logger.warning("too many arguments for b1 ("+arg+") - ignored.");
							return false;
						case 3:
							double start = CLOParser.parseDouble(argv[0]);
							double end = CLOParser.parseDouble(argv[1]);
							double incr;
							boolean log = false;
							if( argv[2].endsWith("l") ) {
								log = true;
								// ignore sign (<0 is meaningless)
								incr = Math.abs(CLOParser.parseDouble(argv[2].substring(0, argv[2].length()-1)));
								if( (Math.abs(end)<Math.abs(start) && incr>1.0) || (Math.abs(end)>Math.abs(start) && incr<1.0) ) {
									logger.warning("invalid logarithmic increment for b1-range ["+Formatter.format(start, 3)+", "+
											Formatter.format(end, 3)+"] ("+incr+(incr<1.0?">":"<")+"1 should hold) - ignored.");
									return false;
								}
							}
							else {
								// ignore specified sign; set sign of increment based on start and end
								incr = Math.abs(CLOParser.parseDouble(argv[2]));
								if( end<start ) incr = -incr;
							}
							b1Start = start;
							b1End = end;
							b1Incr = incr;
							b1Log = log;
							return true;
					}
		    	}
		    	@Override
		    	public void report(PrintStream output) {
		    		output.println((Double.isNaN(b1Incr)?
		    			"# b1:                   "+Formatter.format(b1Start, 4):
		    			"# b1range:              "+Formatter.format(b1Start, 4)+" - "+Formatter.format(b1End, 4)+
		    				": "+Formatter.format(b1Incr, 4)+(b1Log?" (logarithmic)":"")));
		    	}
		    });
	public final CLOption cloB2 = new CLOption("b2", "-1.4", EvoLudo.catSimulation,
			"--b2<l>,<u>[,<i>[l]]  range of cost parameter [l,u], increment i, append l for logarithmic increments",
			new CLODelegate() {
		    	@Override
		    	public boolean parse(String arg) {
					String[] argv = arg.trim().split(CLOParser.VECTOR_DELIMITER);
					switch( argv.length ) {
						case 1:
							b2Start = CLOParser.parseDouble(argv[0]);
							b2End = Double.MAX_VALUE;
							b2Incr = Double.NaN;
							return true;
						case 2:
							b2Start = CLOParser.parseDouble(argv[0]);
							b2End = CLOParser.parseDouble(argv[1]);
							b2Incr = (b2End-b2Start)*0.1;
							return true;
						default:
							logger.warning("too many arguments for b2 ("+arg+") - ignored.");
							return false;
						case 3:
							double start = CLOParser.parseDouble(argv[0]);
							double end = CLOParser.parseDouble(argv[1]);
							double incr;
							boolean log = false;
							if( argv[2].endsWith("l") ) {
								log = true;
								// ignore sign (<0 is meaningless)
								incr = Math.abs(CLOParser.parseDouble(argv[2].substring(0, argv[2].length()-1)));
								if( (Math.abs(end)<Math.abs(start) && incr>1.0) || (Math.abs(end)>Math.abs(start) && incr<1.0) ) {
									logger.warning("invalid logarithmic increment for b2-range ["+Formatter.format(start, 3)+", "+
											Formatter.format(end, 3)+"] ("+incr+(incr<1.0?">":"<")+"1 should hold) - ignored.");
									return false;
								}
							}
							else {
								// ignore specified sign; set sign of increment based on start and end
								incr = Math.abs(CLOParser.parseDouble(argv[2]));
								if( end<start ) incr = -incr;
							}
							b2Start = start;
							b2End = end;
							b2Incr = incr;
							b2Log = log;
							return true;
					}
		    	}
		    	@Override
		    	public void report(PrintStream output) {
		    		output.println((Double.isNaN(b2Incr)?
		    			"# b2:                   "+Formatter.format(b2Start, 4):
		    			"# b2range:              "+Formatter.format(b2Start, 4)+" - "+Formatter.format(b2End, 4)+
		    				": "+Formatter.format(b2Incr, 4)+(b2Log?" (logarithmic)":"")));
		    	}
		    });
//	public final CLOption cloTimeWindow = new CLOption("timewindow", EvoLudo.catSimulation, "100",
//			"--timewindow <t>  number of generations for moving window",
//  		new CLOSetter() {
//		    	@Override
//		    	public void parse(String arg) {
//					timeWindow = CLOParser.parseInteger(arg);
//		    	}
//		    	@Override
//		    	public void report() {
//		    		output.println("# timewindow:           "+timeWindow);
//		    	}
//		    });
	public final CLOption cloBins = new CLOption("bins", "100", EvoLudo.catSimulation,
			"--bins <b>      number of bins for histogram",
  		new CLODelegate() {
		    	@Override
		    	public boolean parse(String arg) {
					nBins = CLOParser.parseInteger(arg);
					return true;
		    	}
		    	@Override
		    	public void report(PrintStream output) {
		    		output.println("# bins:                 "+nBins);
		    	}
		    });
//	public final CLOption cloClassify = new CLOption("classify", EvoLudo.catSimulation, "noclassify",
//			"--classify      classify type of dynamics",
//  		new CLOSetter() {
//		    	@Override
//		    	public void parse(String arg) {
//					classify = cloClassify.isSet();
//		    	}
//		    	@Override
//		    	public void report() {
//		    		// nothing to report - setting affects only GUI
//		    	}
//		    });
	public final CLOption cloDistribution = new CLOption("distribution", EvoLudo.catSimulation,
			  "--distribution  print distributions",
		new CLODelegate() {
		    	@Override
		    	public boolean parse(String arg) {
					printDistr = cloDistribution.isSet();
					return true;
		    	}
		    });
	public final CLOption cloSnapInterval = new CLOption("snapinterval", "0", EvoLudo.catSimulation,
			  "--snapinterval <n>  save snapshot every n generations (-1 only at end)",
  		new CLODelegate() {
		    	@Override
		    	public boolean parse(String arg) {
					snapinterval = CLOParser.parseInteger(arg);
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

    // override this method in subclasses to add further command line options
	// subclasses must make sure that they include a call to super
	@Override
	public void collectCLO(CLOParser parser) {
		parser.addCLO(cloC1);
		parser.addCLO(cloC2);
		parser.addCLO(cloB1);
		parser.addCLO(cloB2);
//		parser.addCLO(cloTimeWindow);
		parser.addCLO(cloBins);
//		parser.addCLO(cloClassify);
		parser.addCLO(cloDistribution);
		parser.addCLO(cloSnapInterval);
		parser.addCLO(cloProgress);

		engine.cloGenerations.setDefault("10000");

		super.collectCLO(parser);
	}

	@Override
	public void adjustCLO(CLOParser parser) {
		// remove options that do not make sense in present context
		parser.removeCLO(new String[] { "costparams", "benefitparams" });
	}

	public void saveSnapshot() {
		File snapfile;
		int side = Math.max(16*(int)Math.sqrt(getNPopulation()), 1000);

		int format = getGeometry().isLattice()?AbstractGraph.SNAPSHOT_PNG:AbstractGraph.SNAPSHOT_SVG;
		switch( format ) {
			case AbstractGraph.SNAPSHOT_PNG:
				snapfile = openSnapshot("png");
				MVPop2D.exportSnapshotPNG(engine, this, side, side, snapfile);
				return;

			case AbstractGraph.SNAPSHOT_SVG:
				boolean compressed = true;
				snapfile = openSnapshot(compressed?"svgz":"svg");
				MVPop2D.exportSnapshotSVG(engine, this, side, side, snapfile, compressed);
				return;

			default:
				logger.warning("unknown file format ("+format+")");
		}
	}

	protected File openSnapshot(String ext) {
		Geometry geom = getGeometry();
		// use name of out file as prefix for snapshot - not easily accessible, postponed
		String pre = "snap-g"+geom.getType().getKey()+(int)(geom.connectivity)+"-t"+Formatter.format(engine.getModel().getTime(), 2)+
					 "-n"+Formatter.format(getPlayerUpdateNoise(), 3);
		File snapfile = new File(pre+"."+ext);
		int counter = 0;
		while( snapfile.exists() )
			snapfile = new File(pre+"-"+(counter++)+"."+ext);
		return snapfile;
	}
}