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

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;

import javax.imageio.ImageIO;

import org.evoludo.graphics.AbstractGraph;
import org.evoludo.simulator.EvoLudo;
import org.evoludo.simulator.EvoLudoJRE;
import org.evoludo.simulator.models.ChangeListener;
import org.evoludo.simulator.modules.EcoPGG;
import org.evoludo.simulator.views.MVPop2D;
import org.evoludo.util.CLOParser;
import org.evoludo.util.CLOption;
import org.evoludo.util.CLOption.CLODelegate;
import org.evoludo.util.Formatter;

/**
 *
 * @author Christoph Hauert
 */
public class simEcoPGG extends EcoPGG implements ChangeListener {

	// additional parameters - defaults are set in cloXYZ routines below
	int snapinterval;
	PrintStream out;

	public simEcoPGG(EvoLudo engine) {
		super(engine);
	}

	@Override
	public void run() {
		out = ((EvoLudoJRE) engine).getOutput();
		engine.setReportInterval(1.0);
		engine.modelReset();
		engine.dumpParameters();

		resetStatistics();
		// evolve population
		startStatistics();

		double nGenerations = engine.getNGenerations();
		for( long g=1; g<=nGenerations; g++ ) {
			if( snapinterval>0 && g%snapinterval==0 ) {
				// save snapshot
				saveSnapshot(AbstractGraph.SNAPSHOT_PNG);
			}
			engine.modelNext();
		}

		double generation = engine.getModel().getTime();
		String msg = "# long-term average:      ";
		for( int n=0; n<nTraits; n++ )
			msg += Formatter.formatFix(mean[n], 6)+"\t"+Formatter.format(Math.sqrt(var[n]/(generation-1.0)), 6)+"\t";
		out.println(msg);
		saveSnapshot(AbstractGraph.SNAPSHOT_PNG);

		out.println("# generations @ end: "+Formatter.formatSci(generation, 6));
		engine.dumpEnd();
		engine.exportState();
	}

	double[] mean, var, state;
	double prevsample;

	@Override
	public synchronized void modelChanged(PendingAction action) {
		updateStatistics(engine.getModel().getTime());
	}

	@Override
	public synchronized void modelStopped() {
		updateStatistics(engine.getNGenerations());
	}

	protected void startStatistics() {
		prevsample = engine.getModel().getTime();
	}

	protected void resetStatistics() {
		if (mean == null)
			mean = new double[nTraits];
		if (var == null)
			var = new double[nTraits];
		if (state == null)
			state = new double[nTraits];
		prevsample = Double.MAX_VALUE;
		Arrays.fill(mean, 0.0);
		Arrays.fill(var, 0.0);
	}

	protected void updateStatistics(double time) {
		if (prevsample>=time)
			return;
		model.getMeanTraits(getID(), state);
		// calculate weighted mean and sdev - see wikipedia
        double w = time-prevsample;
        double wn = w/(time);
        for( int n=0; n<nTraits; n++ ) {
			double delta = state[n]-mean[n];
			mean[n] += wn*delta;
			var[n] += w*delta*(state[n]-mean[n]);
		}
        prevsample = time;
	}

	/*
	 * command line parsing stuff
	 */
    // default is no snapshots
	public final CLOption cloSnapInterval = new CLOption("snapinterval", "0", EvoLudo.catSimulation,
			"--snapinterval <n>  save snapshot every n generations",
    		new CLODelegate() {
		    	@Override
		    	public boolean parse(String arg) {
					snapinterval = CLOParser.parseInteger(arg);
					return true;
		    	}
		    });

    @Override
	public void collectCLO(CLOParser parser) {
		// prepare command line options
		parser.addCLO(cloSnapInterval);

		engine.cloGenerations.setDefault("1000");
		super.collectCLO(parser);
	}

	public void saveSnapshot(int format) {
		if( format==AbstractGraph.SNAPSHOT_NONE ) return;	// do not waste our time
		File snapfile;

		switch( format ) {
			case AbstractGraph.SNAPSHOT_PNG:
				int side = Math.max(16*(int)Math.sqrt(nPopulation), 1000);
				BufferedImage snapimage = MVPop2D.getSnapshot(engine, this, side, side);
				snapfile = openSnapshot("png");
				try { 
					ImageIO.write(snapimage, "PNG", snapfile);
				}
				catch( IOException x ) {
					logger.warning("writing image to "+snapfile.getAbsolutePath()+" failed!");
					return;
				}
				out.println("image written to "+snapfile.getAbsolutePath());
				return;

			default:
				logger.warning("unknown file format ("+format+")");
		}
	}

	protected File openSnapshot(String ext) {
		String pre = getKey()+"-t"+Formatter.format(engine.getModel().getTime(), 2);
		File snapfile = new File(pre+"."+ext);
		int counter = 0;
		while( snapfile.exists() )
			snapfile = new File(pre+"-"+(counter++)+"."+ext);
		return snapfile;
	}
}
