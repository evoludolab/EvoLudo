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

package org.evoludo.simulator;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Set;

import org.evoludo.graphics.AbstractGraph;
import org.evoludo.graphics.LineGraph;
import org.evoludo.graphics.AbstractGraph.GraphStyle;
import org.evoludo.simulator.models.Model;
import org.evoludo.simulator.models.Model.Data;
import org.evoludo.simulator.modules.Continuous;
import org.evoludo.simulator.modules.Module;
import org.evoludo.util.Formatter;
import org.evoludo.util.RingBuffer;

/**
 *
 * @author Christoph Hauert
 */
public class MVMean extends MVAbstract implements LineGraph.LineGraphController{

	// NOTE: this is a bit of a hack that allows us to use graphs as Set<LineGraph> here
	//		 but as Set<AbstractGraph> in super classes. Saves a lot of ugly casting
	@SuppressWarnings("hiding")
	protected Set<LineGraph> graphs;

	double[] state, mean;

	@SuppressWarnings("unchecked")
	public MVMean(EvoLudoGWT engine, Model.Data type) {
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
//			case DEGREE:
//				return "Structure - Degree";
//			case STATISTICS_FIXATION_PROBABILITY:
//				return "Statistics - Fixation probability";
//			case STATISTICS_FIXATION_TIME:
//				return "Statistics - Fixation time";
			default:
				return null;
		}
	}

	@Override
	public void reset(boolean soft) {
		super.reset(soft);
		ArrayList<? extends Module> species = engine.getModule().getSpecies();
		int nSpecies = species.size();
		int nGraphs = 0;
		// multiple line graphs for multi-species interactions and in case of multiple traits for continuous strategies
		for( Module pop : species ) {
			if( pop.isContinuous() && type==Model.Data.STRATEGY )
				nGraphs += pop.getNTraits();
			else
				nGraphs++;
		}

		if( graphs.size()!=nGraphs ) {
			soft = false;
			destroyGraphs();
			for( Module pop : species ) {
				int nTraits = pop.getNTraits();
				if( pop.isContinuous() && type==Model.Data.STRATEGY ) {
					for( int n=0; n<nTraits; n++ ) {
						// in continuous models each trait has its own panel with 3 lines: mean +/- sdev
						LineGraph graph = new LineGraph(this, 3, n, species.lastIndexOf(pop) == nTraits - 1);
						wrapper.add(graph);
						graphs2mods.put(graph, pop);
					}
					continue;
				}
				LineGraph graph = new LineGraph(this, type == Data.STRATEGY ? nTraits : nTraits + 1, pop.getID());
				wrapper.add(graph);
				graphs2mods.put(graph, pop);
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
			Color[] colors = module.getTraitColors();
			int nTraits = module.getNTraits();

			switch( type ) {
				default:
				case STRATEGY:
					if( module.isContinuous() ) {
						// this is the ID of the species associated with this graph
						int tag = graph.getTag();
						style.yLabel = module.getTraitName(tag);
						style.percentY = false;
						Continuous cmod = (Continuous) module;
						style.yMin = cmod.getTraitMin()[tag];
						style.yMax = cmod.getTraitMax()[tag];
						String[] traitcolors = new String[3];
						traitcolors[0] = ColorMapCSS.Color2Css(colors[tag]);					// mean
						traitcolors[1] = ColorMapCSS.Color2Css(colors[tag+nTraits]);			// min
						traitcolors[2] = ColorMapCSS.Color2Css(colors[tag+nTraits+nTraits]);	// max
						graph.setColors(traitcolors);
						if( state==null || state.length<3 )
							state = new double[3];	// mean/min/max
						int nState = 2*nTraits;
						if( mean==null || mean.length<nState )
							mean = new double[nState];	// mean/sdev
					}
					else {
						int nState = nTraits;
						if( state==null || state.length<nState )
							state = new double[nState];
						mean = state;
						Model model = engine.getModel();
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
						graph.setColors(ColorMapCSS.Color2Css(colors));
						String[] mcolors = new String[colors.length];
						int n = 0;
						for (Color color : colors)
							mcolors[n++] = ColorMapCSS.Color2Css(ColorMapCSS.addAlpha(color, 100));
						graph.setMarkers(module.getMarkers(), mcolors);
					}
					break;
				case FITNESS:
					if( module.isContinuous() ) {
						int nState = nTraits;
						if( state==null || state.length<nState )
							state = new double[nState];
						// only 2 entries needed for mean and sdev payoff
						if( mean==null || mean.length<2 )
							mean = new double[nState];	// mean/sdev
						// hardcoded color: black for mean, light gray for mean +/- sdev
						colors = new Color[] {Color.BLACK, Color.LIGHT_GRAY, Color.LIGHT_GRAY};
					}
					else {
						// one 'state' more for the average fitness
						int nState = nTraits+1;
						if( state==null || state.length<nState )
							state = new double[nState];
						mean = state;
						colors = new Color[nState];
						System.arraycopy(module.getTraitColors(), 0, colors, 0, nState-1);
						colors[nState-1] = Color.BLACK;
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
		Model model = engine.getModel();
		double newtime = model.getTime();
		Module module = null;
		int nTraits = -1;
		if( Math.abs(timestamp-newtime)>1e-8 ) {
			for( LineGraph graph : graphs ) {
				Module newMod = graphs2mods.get(graph);
				switch( type ) {
					case STRATEGY:
						if( module != newMod ) {
							module = newMod;
							model.getMeanTrait(graph.getTag(), mean);
							nTraits = module.getNTraits();
						}
						// module cannot be null here but make compiler happy
						if (module != null && module.isContinuous()) {
							int idx = graph.getTag();
							double m = mean[idx];
							double s = mean[idx+nTraits];
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
						if (module != null && module.isContinuous()) {
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
				if( module.isContinuous() ) {
					double inter = interpolate(current[1], prev[1], fx);
					tip += "<tr><td style='text-align:right'><i style='color:"+ColorMapCSS.Color2Css(module.getTraitColor(0))+";'>mean:</i></td><td>"+
							(style.percentY?Formatter.formatPercent(inter, 2):Formatter.format(inter, 2));
					double sdev = inter-interpolate(current[2], prev[2], fx); // data: mean, mean-sdev, mean+sdev
					tip += " ± "+(style.percentY?Formatter.formatPercent(sdev, 2):Formatter.format(sdev, 2))+"</td></tr>";
				}
				else {
					boolean[] active = module.getActiveTraits();
					for( int n=1; n<len; n++ ) {
						if( !active[n-1] )
							continue;
						String name;
						if( n==len-1 && type==Model.Data.FITNESS )
							name = "average";
						else
							name = module.getTraitName(n-1);
						tip += "<tr><td style='text-align:right'><i style='color:"+ColorMapCSS.Color2Css(module.getTraitColor(n-1))+";'>"+name+":</i></td><td>";
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