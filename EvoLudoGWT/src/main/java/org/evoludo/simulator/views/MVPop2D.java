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
import java.util.Set;

import org.evoludo.graphics.AbstractGraph;
import org.evoludo.graphics.PopGraph2D;
import org.evoludo.math.ArrayMath;
import org.evoludo.simulator.ColorMap;
import org.evoludo.simulator.ColorMapCSS;
import org.evoludo.simulator.EvoLudo.ColorModelType;
import org.evoludo.simulator.EvoLudoGWT;
import org.evoludo.simulator.Geometry;
import org.evoludo.simulator.Network;
import org.evoludo.simulator.Network.Status;
import org.evoludo.simulator.Network2D;
import org.evoludo.simulator.models.IBS;
import org.evoludo.simulator.models.IBSPopulation;
import org.evoludo.simulator.models.Model;
import org.evoludo.simulator.models.Model.Type;
import org.evoludo.simulator.models.ODEEuler.HasDE;
import org.evoludo.simulator.modules.Continuous;
import org.evoludo.simulator.modules.Discrete;
import org.evoludo.simulator.modules.Map2Fitness;
import org.evoludo.simulator.modules.Module;
import org.evoludo.util.Formatter;

/**
 *
 * @author Christoph Hauert
 */
public class MVPop2D extends MVAbstract implements AbstractGraph.NodeGraphController {

	@SuppressWarnings("hiding")
	private Set<PopGraph2D> graphs;

	protected int hitNode = -1;

	@SuppressWarnings("unchecked")
	public MVPop2D(EvoLudoGWT engine, Model.Data type) {
		super(engine, type);
		graphs = (Set<PopGraph2D>) super.graphs;
	}

	@Override
	public String getName() {
		switch( type ) {
			case STRATEGY:
				return "Strategies - Structure";
			case FITNESS:
				return "Fitness - Structure";
			default:
				return null;
		}
	}

	@Override
	public void unload() {
		destroyGraphs();
		super.unload();
		hitNode = -1;
	}

	@Override
	public void layoutComplete() {
		checkLayout();
		update();
	}

	@Override
	protected void checkLayout() {
		if( callback!=null && isActive ) {
			// check if all graphs have layout
			boolean layouting = false;
			for( PopGraph2D graph : graphs ) {
				// graph may not have a network (e.g. for ODE/SDE models)
				Network2D net = graph.getNetwork();
				if( net==null )
					continue;
				Status status = net.getStatus();
				if( status.equals(Status.HAS_LAYOUT) || status.equals(Status.NO_LAYOUT) )
					continue;
				layouting = true;
				break;
			}
			if( !layouting )
				callback.layoutComplete();
		}
	}

	@Override
	public void clear() {
		super.clear();
		for( PopGraph2D graph : graphs )
			graph.clearGraph();
	}

	@Override
	protected void destroyGraphs() {
		// super takes care of removing graphs, just deal with networks here
		for( PopGraph2D graph : graphs ) {
			Network2D net = graph.getNetwork();
			if( net!=null )
				net.cancelLayout();
		}
		super.destroyGraphs();
	}

	@Override
	public void update(boolean force) {
		Model model = engine.getModel();
		switch( model.getModelType() ) {
			case ODE:
				for( PopGraph2D graph : graphs )
					graph.displayMessage("No view available (ODE Solver)");
				break;
			case SDE:
				for( PopGraph2D graph : graphs )
					graph.displayMessage("No view available (SDE Solver)");
				break;
			case PDE:
			default:
				// always read data - some nodes may have changed due to user actions
				double newtime = model.getTime();
				boolean isNext = (Math.abs(timestamp-newtime)>1e-8);
				timestamp = newtime;
				for( PopGraph2D graph : graphs ) {
					boolean doUpdate = isActive || graph.hasHistory();
					// if graph is neither active nor has history, force can be safely ignored
					// otherwise may lead to problems if graph has never been activated
					if (!doUpdate)
						continue;
					switch( type ) {
						case STRATEGY:
							model.getTraitData(graph.getTag(), graph.getData(), graph.getColorMap());
							break;
						case FITNESS:
							// cast should be safe for fitness data
							model.getFitnessData(graph.getTag(), graph.getData(), (ColorMap.Gradient1D<String>) graph.getColorMap());
							break;
						default:
							break;
					}
					graph.addData(isNext);
					graph.paint();
				}
		}
	}

	@Override
	public void reset(boolean soft) {
		super.reset(soft);
		// how to deal with distinct interaction/reproduction geometries?
		// - currently two separate graphs are shown one for the interaction and the other for the reproduction geometry
		// - alternatively links could be drawn in different colors (would need to revise network layout routines)
		// - another alternative is to add context menu to toggle between the different link sets (could be difficult if one is a lattice...)
		int nGraphs = 0;
		Geometry geoDE = null;
		switch( engine.getModel().getModelType() ) {
			case PDE:
				geoDE = ((Model.PDE)engine.getModel()).getGeometry();
				//$FALL-THROUGH$
			case ODE:
			case SDE:
				nGraphs = 1;
				if( graphs.size()!=nGraphs ) {
					soft = false;
					destroyGraphs();
					Module module = engine.getModule();
					PopGraph2D graph = new PopGraph2D(this, module.getID());
					// debugging not available for DE's
					graph.setDebugEnabled(false);
					wrapper.add(graph);
					graphs2mods.put(graph, module);
				}
				// there is only a single graph for now but whatever...
				for (PopGraph2D graph : graphs)
					graph.setGeometry(geoDE);
				break;
			case IBS:
				ArrayList<? extends Module> species = engine.getModule().getSpecies();
				for( Module module : species )
					nGraphs += Geometry.displayUniqueGeometry(module)?1:2;

				if( graphs.size()!=nGraphs ) {
					soft = false;
					destroyGraphs();
					for( Module module : species ) {
						PopGraph2D graph = new PopGraph2D(this, module.getID());
						wrapper.add(graph);
						graphs2mods.put(graph, module);
						if( !Geometry.displayUniqueGeometry(module) ) {
							graph = new PopGraph2D(this, module.getID());
							wrapper.add(graph);
							graphs2mods.put(graph, module);
							// arrange graphs horizontally
							gCols = 2;
						}
					}
					gRows = species.size();
					if( gRows*gCols==2 ) {
						// always arrange horizontally if only two graphs
						gRows = 1;
						gCols = 2;
					}
					int width = 100/gCols;
					int height = 100/gRows;
					for( PopGraph2D graph : graphs )
						graph.setSize(width+"%", height+"%");
				}
				// even if nGraphs did not change, the geometries associated with the graphs still need to be updated
				boolean inter = true;
				for (PopGraph2D graph : graphs) {
					Module module = graphs2mods.get(graph);
					Geometry geo = inter ? module.getInteractionGeometry() : module.getReproductionGeometry();
					graph.setGeometry(geo);
					// alternate between interaction and reproduction geometries
					// no consequences if they are the same
					inter = !inter;
				}
				break;
			default:
		}

		for( PopGraph2D graph : graphs ) {
			Geometry geometry = graph.getGeometry();
			if( geometry==null ) {
				graph.reset();
				continue;
			}
			Module pop = graphs2mods.get(graph);
			PopGraph2D.GraphStyle style = graph.getStyle();
			if( geometry.isType(Geometry.Type.LINEAR) ) {
				// frame, ticks, labels needed
				style.xLabel = "nodes";
				style.showXLabel = true;
				style.showXTickLabels = true;
				style.xMin = 0;
				style.xMax = geometry.size;
				style.yLabel = "time";
				double rFreq = -engine.getReportInterval();
				// if report frequency did not change, we're done
				if( Math.abs(style.yIncr-rFreq)>1e-8 ) {
					style.yIncr = -rFreq;
					soft = false;
				}
				style.yMax = 0.0;
				style.showYLabel = true;
				style.showYTickLabels = true;
				style.showXTicks = true;
				style.showYTicks = true;
				style.showYLevels = true;
			}
			else {
				// border is all we want
				style.showXLabel = false;
				style.showYLabel = false;
				style.showYTickLabels = false;
				style.showXTickLabels = false;
				style.showXTicks = false;
				style.showYTicks = false;
			}
//			style.label = geometry.name;
//			style.showLabel = !style.label.isEmpty();
			style.percentY = false;
			style.showXLevels = false;

			ColorMap<String> cMap = null;
			switch( type ) {
				case STRATEGY:
					if( pop.isContinuous() ) {
						ColorModelType cmt = engine.getColorModelType();
						int nTraits = pop.getNTraits();
						if( cmt==ColorModelType.DISTANCE ) {
							cMap = new ColorMapCSS.Gradient1D(new Color[] { Color.BLACK, Color.GRAY, Color.YELLOW, Color.RED }, 500);
							break;
						}
						switch( nTraits ) {
							case 1:
								// set hue range: min = red, max = blue
								cMap = new ColorMapCSS.Hue(0.0, 2.0/3.0, 500);
								break;
							case 2:
								Color[] traitcolors = pop.getTraitColors();
								cMap = new ColorMapCSS.Gradient2D(traitcolors[0], traitcolors[1], Color.BLACK, 50);
								break;
							default:
								Color[] primaries = new Color[nTraits];
								System.arraycopy(pop.getTraitColors(), 0, primaries, 0, nTraits);
								cMap = new ColorMapCSS.GradientND(primaries);
								break;
						}
					}
					else {
						if( engine.isModelType(Model.Type.PDE) ) {
							int nTraits = pop.getNTraits();
							Color[] colors = pop.getTraitColors();
							int dep = ((HasDE)pop).getDependent();
							if (nTraits == 2 && dep >= 0) {
								int trait = (dep + 1) % nTraits;
								cMap = new ColorMapCSS.Gradient1D(colors[dep], colors[trait], trait, 100);
							}
							else
								cMap = new ColorMapCSS.Gradient2D(colors, dep, 100);
						}
						else
							cMap = new ColorMapCSS.Index(pop.getTraitColors(), 220);
					}
					break;
				case FITNESS:
					ColorMap.Gradient1D<String> cMap1D = new ColorMapCSS.Gradient1D(
							new Color[] { ColorMap.addAlpha(Color.BLACK, 220), ColorMap.addAlpha(Color.GRAY, 220),
									ColorMap.addAlpha(Color.YELLOW, 220), ColorMap.addAlpha(Color.RED, 220) },
							500);
					cMap = cMap1D;
					cMap1D.setRange(pop.getMinFitness(), pop.getMaxFitness());
					if (engine.isModelType(Model.Type.IBS)) {
						Map2Fitness map2fit = pop.getMapToFitness();
						if (pop instanceof Discrete) {
							// mark homogeneous fitness values by pale color
							Color[] pure = pop.getTraitColors();
							int nMono = pop.getNTraits();
							for( int n=0; n<nMono; n++ ) {
								double mono = ((Discrete)pop).getMonoScore(n);
								if (Double.isNaN(mono))
									continue;
								cMap1D.setColor(map2fit.map(mono),
										new Color(Math.max(pure[n].getRed(), 127),
												Math.max(pure[n].getGreen(), 127),
												Math.max(pure[n].getBlue(), 127), 220));
							}
							break;
						}
						if( pop instanceof Continuous ) {
// hardcoded colors for min/max mono scores
							cMap1D.setColor(map2fit.map(pop.getMinMonoScore()), ColorMap.addAlpha(Color.BLUE.darker(), 220));
							cMap1D.setColor(map2fit.map(pop.getMaxMonoScore()), ColorMap.addAlpha(Color.BLUE.brighter(), 220));
							break;
						}
						// unknown type of population - no fitness values marked
					}
					break;
				default:
					break;
			}
			if( cMap==null )
				throw new Error("MVPop2D: ColorMap not initialized - needs attention!");
			graph.setColorMap(pop.processColorMap(cMap));
			if( !soft )
				graph.reset();
		}
		update();
	}

	@Override
	public void init() {
		super.init();
		for( PopGraph2D graph : graphs)
			graph.init();
		update();
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * List of additional shortcuts provided by {@code MVPop2D} for the following
	 * keys:
	 * <dl>
	 * <dt>{@code s}</dt>
	 * <dd>Shake and relax dynamically generated network layouts to help achieve a
	 * lower 'energy' state.</dd>
	 * </dl>
	 * 
	 * @see Network#shake(org.evoludo.simulator.Network.LayoutListener, double)
	 */
	@Override
	public boolean keyDownHandler(String key) {
		switch( key ) {
			case "s":
				for( PopGraph2D graph : graphs)
					graph.getNetwork().shake(graph, 0.05);
				return true;
			default:
				return super.keyDownHandler(key);
		}
	}

	@Override
	public void updateNodeAt(AbstractGraph graph, int node) {
		Model model = engine.getModel();
		if (!model.isModelType(Type.IBS))
			return;
		IBSPopulation pop = ((IBS) model).getSpecies(graphs2mods.get(graph));
		pop.debugUpdatePopulationAt(node);
	}

	@Override
	public void mouseHitNode(int id, int node, boolean alt) {
		Model model = engine.getModel();
		if (model.isModelType(Model.Type.IBS))
			((Model.IBS) model).mouseHitNode(id, node, alt);
	}

	@Override
	public String getTooltipAt(AbstractGraph agraph, int node) {
		PopGraph2D graph = (PopGraph2D)agraph;
		Geometry geometry = graph.getGeometry();
		int nNodes = geometry.size;
		Module pop = graphs2mods.get(graph);
		Model model = engine.getModel();
		int id;
		String[] data = graph.getData();
		StringBuilder tip = new StringBuilder("<table style='border-collapse:collapse;border-spacing:0;'>");
		if (pop.getNSpecies() > 1)
			tip.append("<tr><td><i>Species:</i></td><td>"+pop.getName()+"</td></tr>");

		switch( model.getModelType() ) {
			case ODE:
				return null;	// no further information available

			case PDE:
				if( node>=nNodes ) {
					// this can only happen for Geometry.LINEAR
					int idx = node/nNodes;
					node %= nNodes;
					double t = idx*engine.getReportInterval();
					tip.append("<tr><td><i>Node:</i></td><td>"+node+"</td></tr>"+
							"<tr><td><i>Time:</i></td><td>"+Formatter.format(-t, 2)+"</td></tr>");
					return tip+"</table>";
//NOTE: reverse engineer color data from RingBuffer? - see below
				}
				String[] s = pop.getTraitNames();
				Color[] c = pop.getTraitColors();
				id = pop.getID();
				String names = "<tr><td><i>Strategies:</i></td><td><span style='color:"+ColorMapCSS.Color2Css(c[0])+
						"; font-size:175%; line-height:0.57;'>&#x25A0;</span> "+s[0];
				for( int n=1; n<s.length; n++ )
					names += ", <span style='color:"+ColorMapCSS.Color2Css(c[n])+
						"; font-size:175%; line-height:0.57;'>&#x25A0;</span> "+s[n];
				names += "</td></tr>";
				String density = "";
				if( type==Model.Data.STRATEGY )
					density = "<tr><td><i>Densities:</i></td><td><span style='color:"+data[node]+
					"; font-size:175%; line-height:0.57;'>&#x25A0;</span> "+model.getTraitNameAt(id, node)+"</td></tr>";
				else
					density = "<tr><td><i>Densities:</i></td><td>"+model.getTraitNameAt(id, node)+"</td></tr>";
				String fitness = "";
				if( type==Model.Data.FITNESS )
					fitness = "<tr><td><i>Fitness:</i></td><td><span style='color:"+data[node]+
					"; font-size:175%; line-height:0.57;'>&#x25A0;</span> "+model.getFitnessNameAt(id, node);
				else
					fitness = "<tr><td><i>Fitness:</i></td><td>"+model.getFitnessNameAt(id, node);
				fitness += " → "+Formatter.pretty(ArrayMath.dot(model.getMeanTraitAt(id, node), model.getMeanFitnessAt(id, node)), 3)+"</td></tr>";
				tip.append("<tr><td><i>Node:</i></td><td>"+node+"</td></tr>"+names+density+fitness);
				if( geometry.isUndirected )
					tip.append("<tr><td><i>Connections:</i></td><td>"+formatStructureAt(node, geometry)+"</td></tr>");
				else
					tip.append("<tr><td><i>Links to:</i></td><td>"+formatOutStructureAt(node, geometry)+"</td></tr>"+
							"<tr><td><i>Link here:</i></td><td>"+formatInStructureAt(node, geometry)+"</td></tr>");
				return tip.append("</table>").toString();

			case IBS:
				if( node>=nNodes ) {
					// this can only happen for Geometry.LINEAR
					int idx = node/nNodes;
					node %= nNodes;
					double t = idx*engine.getReportInterval();
					tip.append("<tr><td><i>Node:</i></td><td>"+node+"</td></tr>"+
							"<tr><td><i>Time:</i></td><td>"+Formatter.format(-t, 2)+"</td></tr>");
					return tip+"</table>";
//NOTE: RingBuffer contains color-strings; requires reverse engineering to determine strategies...
//					RingBuffer.Array<String> buffer = graph.getBuffer();
//					if( idx>=buffer.size() ) return tip+"</table>";
//					String[] colors = graph.getBuffer().get(idx);
//					return tip+"<tr><td colspan='2'><hr/></td></tr><tr><td><i>Strategy:</i></td><td style='color:"+colors[node]+";'>"+
//							population.getTraitNameAt(node)+"</td></tr></table>";
				}
				Model.IBS ibs = (Model.IBS)model;
				id = pop.getID();
				tip.append("<tr><td><i>Node:</i></td><td>"+node+"</td></tr>");
				if( type==Model.Data.STRATEGY ) {
					// strategy: use color-data to color strategy
					tip.append("<tr><td><i>Strategy:</i></td><td><span style='color:"+data[node]+
							"; font-size:175%; line-height:0.57;'>&#x25A0;</span> "+model.getTraitNameAt(id, node)+"</td></tr>");
				}
				else {
					tip.append("<tr><td><i>Strategy:</i></td><td>"+model.getTraitNameAt(id, node)+"</td></tr>");
				}
				tip.append("<tr><td><i>Tag:</i></td><td>"+ibs.getTagNameAt(id, node)+"</td></tr>");
				// with payoff-to-fitness report score first, then fitness (see below)
				boolean noFitMap = pop.getMapToFitness().isMap(Map2Fitness.Map.NONE);
				String label = (noFitMap?"Fitness":"Score");
				if( type==Model.Data.FITNESS ) {
					// fitness: use color-data to color strategy
					tip.append("<tr><td><i>"+label+":</i></td><td><span style='color:"+data[node]+
							"; font-size:175%; line-height:0.57;'>&#x25A0;</span> "+model.getScoreNameAt(id, node)+"</td></tr>");
				}
				else {
					tip.append("<tr><td><i>"+label+":</i></td><td>"+model.getScoreNameAt(id, node)+"</td></tr>");
				}
				if( !noFitMap )
					tip.append("<tr><td><i>Fitness:</i></td><td>"+model.getFitnessNameAt(id, node)+"</td></tr>");
				int count = ibs.getInteractionsAt(id, node);
				if (count >= 0) {
					if (count == Integer.MAX_VALUE)
						tip.append("<tr><td><i>Interactions:</i></td><td>all</td></tr>");
					else
						tip.append("<tr><td><i>Interactions:</i></td><td>"+count+"</td></tr>");
				}
				Geometry interaction = pop.getInteractionGeometry();
				if( interaction.isUndirected )
					tip.append("<tr><td><i>Neighbors:</i></td><td>"+formatStructureAt(node, interaction)+"</td></tr>");
				else
					//useful for debugging geometry - Geometry.checkConnections should be able to catch such problems
					tip.append("<tr><td><i>Links to:</i></td><td>"+formatOutStructureAt(node, interaction)+"</td></tr>"+
							"<tr><td><i>Link here:</i></td><td>"+formatInStructureAt(node, interaction)+"</td></tr>");
				if( interaction.isInterspecies() ) {
					tip.append(formatStrategiesAt(node, interaction, getGraphFor(interaction.getOpponent().getModule())));
				}
				Geometry reproduction = pop.getReproductionGeometry();
				if( !reproduction.interReproSame ) {
					if( reproduction.isUndirected )
						tip.append("<tr><td><i>Competitors:</i></td><td>"+formatStructureAt(node, reproduction)+"</td></tr>");
					else
						tip.append("<tr><td><i>Competes for:</i></td><td>"+formatOutStructureAt(node, reproduction)+"</td></tr>"+
								"<tr><td><i>Compete here:</i></td><td>"+formatInStructureAt(node, reproduction)+"</td></tr>");
				}
				return tip.append("</table>").toString();

			default:
				return null;
		}
	}

	// NOTE: same as in MVPop3D - not enough to warrant a common abstract class MVPop
//XXX this is not a unique mapping... each module may entertain several graphs (e.g. for interaction and reproduction graphs)
	private PopGraph2D getGraphFor(Module module) {
		if( graphs==null || module==null )
			return null;
		for( PopGraph2D graph : graphs)
			if( module==graphs2mods.get(graph) )
				return graph;
		return null;
	}

	private static String formatStrategiesAt(int node, Geometry geom, PopGraph2D graph) {
		return formatStrategiesAt(geom.out[node], geom.kout[node], geom.getType(), graph.getGeometry().getName(), graph.getData());
	}

	private static String formatStrategiesAt(int[] links, int k, Geometry.Type type, String name, String[] data) {
		if( type==Geometry.Type.MEANFIELD || k==0 )
			return "";
		String msg = "<tr><td><i style='padding-left:2em'>"+name+":</i></td>"+
				"<td>[<span style='color:"+data[links[0]]+"; font-size:175%; line-height:0.57;'>&#x25A0;</span>";
		int disp = Math.min(k, 10);
		for( int n=1; n<disp; n++ )
			msg += "<span style='color:"+data[links[n]]+"; font-size:175%; line-height:0.57;'>&nbsp;&#x25A0;</span>";
		if( disp<k )
			msg += " ...";
		return msg+"]</td></tr>";
	}

	private static String formatStructureAt(int node, Geometry geom) {
		return formatStructureAt(geom.out[node], geom.kout[node], geom.getType());
	}

	private static String formatOutStructureAt(int node, Geometry geom) {
		return formatStructureAt(geom.out[node], geom.kout[node], geom.getType());
	}

	private static String formatInStructureAt(int node, Geometry geom) {
		return formatStructureAt(geom.in[node], geom.kin[node], geom.getType());
	}

	private static String formatStructureAt(int[] links, int k, Geometry.Type type) {
		if( type==Geometry.Type.MEANFIELD )
			return "all";
		String msg;
		switch( k ) {
			case 0:
				return "none";
			case 1:
				return "1 ["+links[0]+"]";
			default:
				msg = k+" ["+links[0];
				int disp = Math.min(k, 10);
				for( int n=1; n<disp; n++ ) msg += " "+links[n];
				if( disp<k ) msg += " ...";
				msg += "]";
		}
		return msg;
	}

	@Override
	protected int[] exportTypes() {
		return new int[] { EXPORT_SVG, EXPORT_PNG };
	}
}
