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

package org.evoludo.simulator.views;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Set;

import org.evoludo.graphics.AbstractGraph;
import org.evoludo.graphics.LineGraph;
import org.evoludo.graphics.AbstractGraph.GraphStyle;
import org.evoludo.simulator.ColorMapCSS;
import org.evoludo.simulator.EvoLudoGWT;
import org.evoludo.simulator.models.Model;
import org.evoludo.simulator.modules.Continuous;
import org.evoludo.simulator.modules.Discrete;
import org.evoludo.simulator.modules.Module;
import org.evoludo.util.Formatter;
import org.evoludo.util.RingBuffer;

/**
 *
 * @author Christoph Hauert
 */
public class Mean extends AbstractView implements LineGraph.LineGraphController{

	// NOTE: this is a bit of a hack that allows us to use graphs as Set<LineGraph> here
	//		 but as Set<AbstractGraph> in super classes. Saves a lot of ugly casting
	@SuppressWarnings("hiding")
	protected Set<LineGraph> graphs;

	double[] state, mean;

	@SuppressWarnings("unchecked")
	public Mean(EvoLudoGWT engine, Model.Data type) {
		super(engine, type);
		graphs = (Set<LineGraph>) super.graphs;
	}

	@Override
	public String getName() {
		switch( type ) {
			case STRATEGY:
				return "Strategies - Mean";
			case FITNESS:
				return "Fitness - Mean";
			default:
				return null;
		}
	}

	@Override
	public void reset(boolean soft) {
		super.reset(soft);
		ArrayList<? extends Module> species = engine.getModule().getSpecies();
		int nSpecies = model.getNSpecies();
		// multiple line graphs for multi-species interactions and in case of multiple traits for continuous strategies
		boolean cmodel = model.isContinuous();
		int nMean = model.getNMean();
		int nGraphs = (cmodel && type==Model.Data.STRATEGY ? nMean : nSpecies);
		// if the number of graphs has changed, destroy and recreate them
		if( graphs.size()!=nGraphs ) {
			soft = false;
			destroyGraphs();
			for( int n=0; n<nGraphs; n++ ) {
				// in discrete models each species has its own panel with all traits
				// in continuous models each trait has its own panel with 3 lines: mean +/- sdev
				LineGraph graph = new LineGraph(this, n);
//				graphs.add(graph);
				wrapper.add(graph);
graphs2mods.put(graph, species.get(n));
				graph.getStyle().showXLabel = (n==nGraphs-1);
			}
			// arrange graphs vertically
			gRows = nGraphs;
			int width = 100/gCols;
			int height = 100/gRows;
			for( LineGraph graph : graphs )
				graph.setSize(width+"%", height+"%");
		}

		for( LineGraph graph : graphs ) {
			AbstractGraph.GraphStyle style = graph.getStyle();
			Module module = graphs2mods.get(graph);
			Color[] colors = model.getMeanColors();
			// tag is index of species in discrete models and index of trait in continuous models
			int tag = graph.getTag();

			switch( type ) {
				default:
				case STRATEGY:
					if( cmodel ) {
						style.yLabel = module.getTraitName(tag);
						style.percentY = false;
						Continuous cmod = (Continuous) module;
						style.yMin = cmod.getTraitMin()[tag];
						style.yMax = cmod.getTraitMax()[tag];
						String[] traitcolors = new String[3];
						traitcolors[0] = ColorMapCSS.Color2Css(colors[tag]);					// mean
						traitcolors[1] = ColorMapCSS.Color2Css(colors[tag+nMean]);			// min
						traitcolors[2] = ColorMapCSS.Color2Css(colors[tag+nMean+nMean]);	// max
						graph.setColors(traitcolors);
						if( state==null || state.length<3 )
							state = new double[3];	// mean/min/max
						int nState = 2*nMean;
						if( mean==null || mean.length<nState )
							mean = new double[nState];	// mean/sdev
						graph.setNLines(3);
					}
					else {
						int nState = model.getNMean(tag);
						if( state==null || state.length<nState )
							state = new double[nState];
						mean = state;
						if (model instanceof Model.DE && ((Model.DE) model).isDensity()) {
							style.yLabel = "density";
							style.percentY = false;
							style.yMin = 0.0;
							style.yMax = 0.0;
						}
						else {
							style.yLabel = "frequency";
							style.percentY = true;
							style.yMin = 0.0;
							style.yMax = 1.0;
						}
						colors = model.getMeanColors();
						graph.setColors(ColorMapCSS.Color2Css(colors));
						String[] mcolors = new String[colors.length];
						int n = 0;
						for (Color color : colors)
							mcolors[n++] = ColorMapCSS.Color2Css(ColorMapCSS.addAlpha(color, 100));
						graph.setMarkers(module.getMarkers(), mcolors);
						graph.setNLines(nState);
					}
					break;
				case FITNESS:
					if( cmodel ) {
						if( state==null || state.length<nMean )
							state = new double[nMean];
						// only 2 entries needed for mean and sdev payoff
						if( mean==null || mean.length<2 )
							mean = new double[2];	// mean/sdev
						// hardcoded color: black for mean, light gray for mean +/- sdev
						colors = new Color[] {Color.BLACK, Color.LIGHT_GRAY, Color.LIGHT_GRAY};
						graph.setNLines(3);
					}
					else {
						// one 'state' more for the average fitness
						int nState = nMean + 1;
						if( state==null || state.length<nState )
							state = new double[nState];
						mean = state;
						colors = new Color[nState];
						System.arraycopy(model.getMeanColors(), 0, colors, 0, nState-1);
						colors[nState-1] = Color.BLACK;
						graph.setNLines(nState);
					}
					graph.setColors(ColorMapCSS.Color2Css(colors));
					double min = module.getMinScore();
					double max = module.getMaxScore();
					if( max-min<1e-8 ) {
						min -= 1.0;
						max += 1.0;
					}
					if( Math.abs(min-style.yMin)>1e-8 || Math.abs(max-style.yMax)>1e-8 ) {
						style.yMin = min;
						style.yMax = max;
						soft = false;
					}
					if( nSpecies>1 )
						style.label = module.getName();
					style.yLabel = "payoffs";
					if (module instanceof Discrete) {
						Discrete dmod = (Discrete) module;
						double[] monoScores = new double[nMean+1];
						// the first entry is for dashed (>0) and dotted (<0) lines
						monoScores[0] = 1.0;
						for (int n=0;n<nMean;n++)
							monoScores[n+1] = dmod.getMonoScore(n);
						String[] monoColors = new String[colors.length];
						int n = 0;
						for (Color color : colors)
							monoColors[n++] = ColorMapCSS.Color2Css(ColorMapCSS.addAlpha(color, 100));
						ArrayList<double[]> marker = new ArrayList<>();
						marker.add(monoScores);
						graph.setMarkers(marker, monoColors);
					}
					break;
			}
			if( nSpecies>1 ) 
				style.label = module.getName();
			style.xLabel = "time";
			style.showXLevels = false;
			double rFreq = engine.getReportInterval();
			if( Math.abs(style.xIncr-rFreq)>1e-8 ) {
				style.xIncr = rFreq;
				style.xMin = -graph.getSteps()*style.xIncr;
				soft = false;
			}
			style.xMax = 0.0;
			if( !soft )
				graph.reset();
		}
		update();
	}

	@Override
	public void update(boolean force) {
		double newtime = model.getTime();
		Module module = null;
		int nState = -1;
		boolean cmodel = model.isContinuous();
		if( Math.abs(timestamp-newtime)>1e-8 ) {
			for( LineGraph graph : graphs ) {
				Module newMod = graphs2mods.get(graph);
				switch( type ) {
					case STRATEGY:
						int tag = graph.getTag();
						if( module != newMod ) {
							module = newMod;
							model.getMeanTraits(graph.getTag(), mean);
							nState = model.getNMean(tag);
						}
						// module cannot be null here but make compiler happy
						if (module != null && cmodel) {
							double m = mean[tag];
							double s = mean[tag+nState];
							state[0] = m;
							state[1] = m-s;
							state[2] = m+s;
						}
						else
							state = mean;
						graph.addData(newtime, state, force);
						break;
					case FITNESS:
						if( module != newMod ) {
							module = newMod;
							model.getMeanFitness(graph.getTag(), mean);
						}
						// module cannot be null here but make compiler happy
						if (module != null && cmodel) {
							// fitness graph has only a single panel
							double m = mean[0];
							double s = mean[1];
							state[0] = m;
							state[1] = m-s;
							state[2] = m+s;
						}
						else
							state = mean;
						graph.addData(newtime, state, force);
						break;
					default:
						break;
				}
			}
		}
		if (isActive) {
			for (LineGraph graph : graphs)
				graph.paint();
		}
		timestamp = newtime;
	}

	@Override
	public String getTooltipAt(LineGraph graph, double sx, double sy) {
		GraphStyle style = graph.getStyle();
		RingBuffer<double[]> buffer = graph.getBuffer();
		double buffert = 0.0;
		double mouset = style.xMin+sx*(style.xMax-style.xMin);
		Module module = graphs2mods.get(graph);
		Iterator<double[]> i = buffer.iterator();
		String tip = "<table style='border-collapse:collapse;border-spacing:0;'>"+
				"<tr><td style='text-align:right'><i>"+style.xLabel+":</i></td><td>"+
					Formatter.format(mouset, 2)+"</td></tr>"+
				"<tr><td style='text-align:right'><i>"+style.yLabel+":</i></td><td>"+
					(style.percentY?Formatter.formatPercent(style.yMin+sy*(style.yMax-style.yMin), 1):
					Formatter.format(style.yMin+sy*(style.yMax-style.yMin), 2))+"</td></tr>";
		if( i.hasNext() ) {
			double[] current = i.next();
			int len = current.length;
			while( i.hasNext() ) {
				double[] prev = i.next();
				double dt = current[0]-prev[0];
				buffert -= Math.max(0.0, dt);
				if( buffert>mouset ) {
					current = prev;
					continue;
				}
				double fx = 1.0-(mouset-buffert)/dt;
				tip += "<tr><td colspan='2'><hr/></td></tr><tr><td style='text-align:right'><i>"+style.xLabel+
					   ":</i></td><td>"+Formatter.format(current[0]-fx*dt, 2)+"</td></tr>";
				if( model.isContinuous() ) {
					double inter = interpolate(current[1], prev[1], fx);
					tip += "<tr><td style='text-align:right'><i style='color:"+ColorMapCSS.Color2Css(module.getTraitColor(0))+";'>mean:</i></td><td>"+
							(style.percentY?Formatter.formatPercent(inter, 2):Formatter.format(inter, 2));
					double sdev = inter-interpolate(current[2], prev[2], fx); // data: mean, mean-sdev, mean+sdev
					tip += " ± "+(style.percentY?Formatter.formatPercent(sdev, 2):Formatter.format(sdev, 2))+"</td></tr>";
				}
				else {
					boolean[] active = module.getActiveTraits();
					Color[] colors = model.getMeanColors();
					for( int n=1; n<len; n++ ) {
						if( !active[n-1] )
							continue;
						String name;
						if( n==len-1 && type==Model.Data.FITNESS )
							name = "average";
						else
							name = module.getTraitName(n-1);
						tip += "<tr><td style='text-align:right'><i style='color:"+ColorMapCSS.Color2Css(colors[n-1])+";'>"+name+":</i></td><td>";
						// deal with NaN's
						if( prev[n]==prev[n] && current[n]==current[n] ) {
							tip += (style.percentY?Formatter.formatPercent(interpolate(current[n], prev[n], fx), 2):
								Formatter.format(interpolate(current[n], prev[n], fx), 2))+"</td></tr>";
						}
						else
							tip += "-</td></tr>";
					}
				}
				break;
			}
		}
		return tip+"</table>";
	}

	private double interpolate(double current, double prev, double x) {
		return (1.0-x)*current+x*prev;
	}

	@Override
	protected int[] exportTypes() {
		return new int[] { EXPORT_SVG, EXPORT_PNG };
	}
}
