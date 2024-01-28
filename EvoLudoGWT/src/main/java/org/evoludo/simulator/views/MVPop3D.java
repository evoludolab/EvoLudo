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
import org.evoludo.graphics.PopGraph3D;
import org.evoludo.math.ArrayMath;
import org.evoludo.simulator.ColorMap;
import org.evoludo.simulator.ColorMap3D;
import org.evoludo.simulator.ColorMapCSS;
import org.evoludo.simulator.EvoLudo.ColorModelType;
import org.evoludo.simulator.EvoLudoGWT;
import org.evoludo.simulator.Geometry;
import org.evoludo.simulator.Network;
import org.evoludo.simulator.Network.Status;
import org.evoludo.simulator.Network3D;
import org.evoludo.simulator.models.Model;
import org.evoludo.simulator.models.ODEEuler.HasDE;
import org.evoludo.simulator.modules.Discrete;
import org.evoludo.simulator.modules.Map2Fitness;
import org.evoludo.simulator.modules.Module;
import org.evoludo.util.Formatter;

import thothbot.parallax.core.shared.materials.MeshLambertMaterial;

/**
 *
 * @author Christoph Hauert
 */
public class MVPop3D extends MVAbstract implements AbstractGraph.NodeGraphController {

	@SuppressWarnings("hiding")
	private Set<PopGraph3D> graphs;
	protected int hitNode = -1;
	protected PopGraph3D hitGraph = null;

	@SuppressWarnings("unchecked")
	public MVPop3D(EvoLudoGWT engine, Model.Data type) {
		super(engine, type);
		graphs = (Set<PopGraph3D>) super.graphs;
	}

	@Override
	public String getName() {
		switch( type ) {
			case STRATEGY:
				return "Strategies - 3D Structure";
			case FITNESS:
				return "Fitness - 3D Structure";
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
	public void activate() {
		if( isActive )
			return;
		prepare();
		super.activate();
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
			for( PopGraph3D graph : graphs ) {
				// graph may not have a network (e.g. for ODE/SDE models)
				Network3D net = graph.getNetwork();
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
	protected void destroyGraphs() {
		// super takes care of removing graphs, just deal with networks here
		for( PopGraph3D graph : graphs ) {
			Network3D net = graph.getNetwork();
			if( net!=null )
				net.cancelLayout();
			graph.invalidate();
		}
		super.destroyGraphs();
	}

	@Override
	public void reset(boolean soft) {
		super.reset(soft);
		if( !isActive ) {
			for( PopGraph3D graph : graphs)
				graph.invalidate();
			return;
		}
		// prepare initializes or starts suspended animation
		soft &= prepare();
		if (!soft) {
			for( PopGraph3D graph : graphs)
				graph.reset();
		}
		update();
	}

	@Override
	public void init() {
		super.init();
		update();
	}

	@Override
	public void update(boolean force) {
		// so far, no 3D graphs have histories implemented; safely ignore force
		if (!isActive)
			return;

		Model model = engine.getModel();
		switch( model.getModelType() ) {
			case ODE:
				// there is only a single graph but whatever...
				for (PopGraph3D graph : graphs)
					graph.displayMessage("No view available (ODE Solver)");
				break;
			case SDE:
				// there is only a single graph but whatever...
				for (PopGraph3D graph : graphs)
					graph.displayMessage("No view available (SDE Solver)");
				break;
			default:
				double newtime = model.getTime();
				boolean isNext = (Math.abs(timestamp-newtime)>1e-8);
				timestamp = newtime;
				for( PopGraph3D graph : graphs) {
					switch( type ) {
						case STRATEGY:
							model.getTraitData(graph.getTag(), graph.getData(), graph.getColorModel());
							break;
						case FITNESS:
							model.getFitnessData(graph.getTag(), graph.getData(), //
								(ColorMap.Gradient1D<MeshLambertMaterial>)graph.getColorModel());
							break;
						default:
							break;
					}
					graph.update(isNext);
				}
		}
	}

	private boolean prepare() {
		boolean soft = true;
		int nGraphs = 0;
		Geometry geoDE = null;
		Model model = engine.getModel();
		switch( model.getModelType() ) {
			case PDE:
				geoDE = ((Model.PDE)model).getGeometry();
				//$FALL-THROUGH$
			case ODE:
			case SDE:
				nGraphs = 1;
				if( graphs.size()!=nGraphs ) {
					soft = false;
					destroyGraphs();
					Module module = engine.getModule();
					PopGraph3D graph = new PopGraph3D(this, module.getID());
					// debugging not available for DE's
					graph.setDebugEnabled(false);
					wrapper.add(graph);
					graphs2mods.put(graph, module);
					gCols = nGraphs;
				}
				// there is only a single graph for now but whatever...
				for (PopGraph3D graph : graphs)
					graph.setGeometry(geoDE);
				break;
			case IBS:
				// how to deal with distinct interaction/reproduction geometries?
				// - currently two separate graphs are shown one for the interaction and the other for the reproduction geometry
				// - alternatively links could be drawn in different colors (would need to revise network layout routines)
				// - another alternative is to add context menu to toggle between the different link sets (could be difficult if one is a lattice...)
				ArrayList<? extends Module> species = engine.getModule().getSpecies();
				for( Module module : species )
					nGraphs += Geometry.displayUniqueGeometry(module)?1:2;

				if( graphs.size()!=nGraphs ) {
					soft = false;
					destroyGraphs();
					for( Module module : species ) {
						PopGraph3D graph = new PopGraph3D(this, module.getID());
						wrapper.add(graph);
						graphs2mods.put(graph, module);
						if( !Geometry.displayUniqueGeometry(module) ) {
							graph = new PopGraph3D(this, module.getID());
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
					for( PopGraph3D graph : graphs )
						graph.setSize(width+"%", height+"%");
				}
				else {
					// start animation in preparation of activation
					for (PopGraph3D graph : graphs)
						graph.animate();
				}
				// update geometries associated with graphs
				boolean inter = true;
				for (PopGraph3D graph : graphs) {
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
		boolean noWarnings = true;
		// IMPORTANT: to avoid problems with WebGL and 3D rendering, each graph needs to have its own color map
		Model.Type mt = engine.getModel().getModelType();
		if( mt.equals(Model.Type.ODE) || mt.equals(Model.Type.SDE) ) {
			// ODE or SDE model (no geometry and thus no population)
			for( PopGraph3D graph : graphs )
				graph.displayMessage("No view available ("+mt.toString()+" solver)");
			return soft;
		}
		for( PopGraph3D graph : graphs ) {
			ColorMap<MeshLambertMaterial> cMap = null;
			Module module = graphs2mods.get(graph);
			switch( type ) {
				case STRATEGY:
					if( module.isContinuous() ) {
						ColorModelType cmt = engine.getColorModelType();
						int nTraits = module.getNTraits();
						if( cmt==ColorModelType.DISTANCE ) {
							cMap = new ColorMap3D.Gradient1D(new Color[] { Color.BLACK, Color.GRAY, Color.YELLOW, Color.RED }, 500);
							break;
						}
						switch( nTraits ) {
							case 1:
								// set hue range: min = red, max = blue
								cMap = new ColorMap3D.Hue(0.0, 2.0/3.0, 500);
								break;
							case 2:
								Color[] tColors = module.getTraitColors();
								cMap = new ColorMap3D.Gradient2D(tColors[0], tColors[1], Color.BLACK, 50);
								break;
							default:
								if( cmt==ColorModelType.DISTANCE ) {
									cMap = new ColorMap3D.Gradient1D(new Color[] { Color.BLACK, Color.GRAY, Color.YELLOW, Color.RED }, 500);
									break;
								}
								Color[] primaries = new Color[nTraits];
								System.arraycopy(module.getTraitColors(), 0, primaries, 0, nTraits);
								cMap = new ColorMap3D.GradientND(primaries);
								if( cmt!=ColorModelType.DISTANCE ) {
									// log warning only once in case there are multiple species
									if( noWarnings ) {
										noWarnings = false;
										logger.warning("display of >2 continuous traits not (yet) implemented - coloring trait distance");
									}
									engine.setColorModelType(ColorModelType.DISTANCE);
								}
								break;
						}
					}
					else {
						if( engine.isModelType(Model.Type.PDE) ) {
							int nTraits = module.getNTraits();
							Color[] colors = module.getTraitColors();
							int dep = ((HasDE)module).getDependent();
							if (nTraits == 2 && dep >= 0)
								cMap = new ColorMap3D.Gradient1D(colors[dep], colors[(dep + 1) % nTraits], 100);
							else
								cMap = new ColorMap3D.Gradient2D(colors, dep, 100);
						}
						else
							cMap = new ColorMap3D.Index(module.getTraitColors(), (int)(0.75 * 255));
					}
					break;
				case FITNESS:
					ColorMap.Gradient1D<MeshLambertMaterial> cMap1D = new ColorMap3D.Gradient1D(
						new Color[] { ColorMap.addAlpha(Color.BLACK, 220), ColorMap.addAlpha(Color.GRAY, 220),
								ColorMap.addAlpha(Color.YELLOW, 220), ColorMap.addAlpha(Color.RED, 220) },
						500);
					cMap = cMap1D;
					cMap1D.setRange(module.getMinFitness(), module.getMaxFitness());
					if( engine.isModelType(Model.Type.IBS) ) {
						Map2Fitness map2fit = module.getMapToFitness();
						if( module.isContinuous() ) {
// hardcoded colors for min/max mono scores
							cMap1D.setColor(map2fit.map(module.getMinMonoScore()), ColorMap.addAlpha(Color.BLUE.darker(), 220));
							cMap1D.setColor(map2fit.map(module.getMaxMonoScore()), ColorMap.addAlpha(Color.BLUE.brighter(), 220));
						}
						else {
							// mark homogeneous fitness values by pale color
							Color[] pure = module.getTraitColors();
							int nMono = module.getNTraits();
							for( int n=0; n<nMono; n++ ) 
								cMap1D.setColor(map2fit.map(((Discrete)module).getMonoScore(n)), 
										new Color(Math.max(pure[n].getRed(), 127), 
												Math.max(pure[n].getGreen(), 127), 
												Math.max(pure[n].getBlue(), 127), 220));
						}
					}
					break;
				default:
					break;
			}
			if( cMap==null )
				throw new Error("MVPop3D: ColorMap not initialized - needs attention!");
			graph.setColorMap(module.processColorMap(cMap));
		}
		return soft;
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * List of additional shortcuts provided by {@code MVPop3D} for the following
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
				for( PopGraph3D graph : graphs)
					graph.getNetwork().shake(graph, 0.05);
				return true;
			default:
				return super.keyDownHandler(key);
		}
	}

	@Override
	public boolean isFullscreenSupported() {
		if( !super.isFullscreenSupported() )
			return false;
		// fullscreen must be supported by all graphs
		for( PopGraph3D graph : graphs )
			if( !graph.isFullscreenSupported() ) 
				return false;
		return true;
	}

	@Override
	public void mouseHitNode(int id, int node, boolean alt) {
		Model model = engine.getModel();
		if (model.isModelType(Model.Type.IBS))
			((Model.IBS) model).mouseHitNode(id, node, alt);
	}

	@Override
	public String getTooltipAt(AbstractGraph agraph, int node) {
		PopGraph3D graph = (PopGraph3D)agraph;
		Geometry geometry = graph.getGeometry();
		int nNodes = geometry.size;
		Module pop = graphs2mods.get(graph);
		Model model = engine.getModel();
		int id;
		MeshLambertMaterial[] data = graph.getData();

		StringBuilder tip = new StringBuilder("<table style='border-collapse:collapse;border-spacing:0;'>");
		if (pop.getNSpecies() > 1)
			tip.append("<tr><td><i>Species:</i></td><td>"+pop.getName()+"</td></tr>");

			switch( model.getModelType() ) {
			case ODE:
				return null;	// no further information available
			case PDE:
				if( node>=nNodes ) {
					// this can only happen for Geometry.LINEAR
					// but linear geometries are not allowed in 3D
					return "Ceci n'est pas une erreur!";
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
					density = "<tr><td><i>Densities:</i></td><td><span style='color:#"+data[node].getColor().getHexString()+
					"; font-size:175%; line-height:0.57;'>&#x25A0;</span> "+model.getTraitNameAt(id, node)+"</td></tr>";
				else
					density = "<tr><td><i>Densities:</i></td><td>"+model.getTraitNameAt(id, node)+"</td></tr>";
				String fitness = "";
				if( type==Model.Data.FITNESS )
					fitness = "<tr><td><i>Fitness:</i></td><td><span style='color:#"+data[node].getColor().getHexString()+
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
					// but linear geometries are not allowed in 3D
					return "Ceci n'est pas une erreur!";
				}
				Model.IBS ibs = (Model.IBS)model;
				id = pop.getID();
				tip.append("<tr><td><i>Node:</i></td><td>"+node+"</td></tr>");
				if( type==Model.Data.STRATEGY ) {
					// strategy: use color-data to color strategy
					tip.append("<tr><td><i>Strategy:</i></td><td><span style='color:#"+data[node].getColor().getHexString()+
							"; font-size:175%; line-height:0.57;'>&#x25A0;</span> "+model.getTraitNameAt(id, node)+"</td></tr>");
				}
				else {
					tip.append("<tr><td><i>Strategy:</i></td><td>"+model.getTraitNameAt(id, node)+"</td></tr>");
				}
				tip.append("<tr><td><i>Tag:</i></td><td>"+ibs.getTagNameAt(id, node)+"</td></tr>");
				boolean noFitMap = pop.getMapToFitness().isMap(Map2Fitness.Map.NONE);
				String label = (noFitMap?"Fitness":"Score");
				if( type==Model.Data.FITNESS ) {
					// fitness: use color-data to color fitness
					tip.append("<tr><td><i>"+label+":</i></td><td><span style='color:#"+data[node].getColor().getHexString()+
							"; font-size:175%; line-height:0.57;'>&#x25A0;</span> "+model.getFitnessNameAt(id, node)+"</td></tr>");
				}
				else {
					tip.append("<tr><td><i>"+label+":</i></td><td>"+model.getFitnessNameAt(id, node)+"</td></tr>");
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

	// NOTE: same as in MVPop2D - not enough to warrant a common abstract class MVPop
//XXX this is not a unique mapping... each module may entertain several graphs (e.g. for interaction and reproduction graphs)
	private PopGraph3D getGraphFor(Module module) {
		if( graphs==null || module==null )
			return null;
		for( PopGraph3D graph : graphs)
			if( module==graphs2mods.get(graph) )
				return graph;
		return null;
	}

	private static String formatStrategiesAt(int node, Geometry geom, PopGraph3D graph) {
		return formatStrategiesAt(geom.out[node], geom.kout[node], geom.getType(), graph.getGeometry().getName(), graph.getData());
	}

	private static String formatStrategiesAt(int[] links, int k, Geometry.Type type, String name, MeshLambertMaterial[] data) {
		if( type==Geometry.Type.MEANFIELD || k==0 )
			return "";
		String msg = "<tr><td><i style='padding-left:2em'>"+name+":</i></td>"+
				"<td>[<span style='color:#"+data[links[0]].getColor().getHexString()+"; font-size:175%; line-height:0.57;'>&#x25A0;</span>";
		int disp = Math.min(k, 10);
		for( int n=1; n<disp; n++ )
			msg += "<span style='color:#"+data[links[n]].getColor().getHexString()+"; font-size:175%; line-height:0.57;'>&nbsp;&#x25A0;</span>";
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
		return new int[] { EXPORT_PNG };
//		return new int[] { EXPORT_SVG, EXPORT_PNG };
	}
}
