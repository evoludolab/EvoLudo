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
// Unless required by applicable law or agreed to in writing, hardware
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
import java.util.List;

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
import org.evoludo.simulator.modules.Map2Fitness;
import org.evoludo.simulator.modules.Module;
import org.evoludo.util.Formatter;

import thothbot.parallax.core.shared.materials.MeshLambertMaterial;

/**
 *
 * @author Christoph Hauert
 */
public class Pop3D extends AbstractView implements AbstractGraph.NodeGraphController {

	@SuppressWarnings("hiding")
	private List<PopGraph3D> graphs;
	protected int hitNode = -1;
	protected PopGraph3D hitGraph = null;

	@SuppressWarnings("unchecked")
	public Pop3D(EvoLudoGWT engine, Model.Data type) {
		super(engine, type);
		graphs = (List<PopGraph3D>) super.graphs;
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
	public void reset(boolean hard) {
		super.reset(hard);
		if( !isActive ) {
			for( PopGraph3D graph : graphs)
				graph.invalidate();
			return;
		}
		// prepare initializes or starts suspended animation
		hard &= prepare();
		if (hard) {
			for( PopGraph3D graph : graphs)
				graph.reset();
		}
		update(hard);
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
							model.getTraitData(graph.getModule().getID(), graph.getData(), graph.getColorModel());
							break;
						case FITNESS:
							model.getFitnessData(graph.getModule().getID(), graph.getData(), //
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
		boolean hard = false;
		int nGraphs = 0;
		Geometry geoDE = null;
		switch( model.getModelType() ) {
			case PDE:
				geoDE = ((Model.PDE)model).getGeometry();
				//$FALL-THROUGH$
			case ODE:
			case SDE:
				nGraphs = 1;
				if( graphs.size()!=nGraphs ) {
					hard = true;
					destroyGraphs();
					Module module = engine.getModule();
					PopGraph3D graph = new PopGraph3D(this, module);
					// debugging not available for DE's
					graph.setDebugEnabled(false);
					wrapper.add(graph);
					graphs.add(graph);
					gCols = nGraphs;
				}
				// there is only a single graph for now but whatever...
				for (PopGraph3D graph : graphs)
					graph.setGeometry(geoDE);
				break;
			case IBS:
				// how to deal with distinct interaction/competition geometries?
				// - currently two separate graphs are shown one for the interaction and the other for the competition geometry
				// - alternatively links could be drawn in different colors (would need to revise network layout routines)
				// - another alternative is to add context menu to toggle between the different link sets (could be difficult if one is a lattice...)
				ArrayList<? extends Module> species = engine.getModule().getSpecies();
				for( Module module : species )
					nGraphs += Geometry.displayUniqueGeometry(module)?1:2;

				if( graphs.size()!=nGraphs ) {
					hard = true;
					destroyGraphs();
					for( Module module : species ) {
						PopGraph3D graph = new PopGraph3D(this, module);
						wrapper.add(graph);
						graphs.add(graph);
						if( !Geometry.displayUniqueGeometry(module) ) {
							graph = new PopGraph3D(this, module);
							wrapper.add(graph);
							graphs.add(graph);
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
					Module module = graph.getModule();
					Geometry geo = inter ? module.getInteractionGeometry() : module.getCompetitionGeometry();
					graph.setGeometry(geo);
					// alternate between interaction and competition geometries
					// no consequences if they are the same
					inter = !inter;
				}
				break;
			default:
		}
		boolean noWarnings = true;
		// IMPORTANT: to avoid problems with WebGL and 3D rendering, each graph needs to have its own color map
		Model.Type mt = model.getModelType();
		if( mt.equals(Model.Type.ODE) || mt.equals(Model.Type.SDE) ) {
			// ODE or SDE model (no geometry and thus no population)
			for( PopGraph3D graph : graphs )
				graph.displayMessage("No view available ("+mt.toString()+" solver)");
			return hard;
		}
		org.evoludo.simulator.models.Model.Continuous cmodel = null;
		org.evoludo.simulator.models.Model.Discrete dmodel = null;
		if (model.isContinuous())
			cmodel = (org.evoludo.simulator.models.Model.Continuous) model;
		else
			dmodel = (org.evoludo.simulator.models.Model.Discrete) model;

		for( PopGraph3D graph : graphs ) {
			ColorMap<MeshLambertMaterial> cMap = null;
			Module module = graph.getModule();
			switch( type ) {
				case STRATEGY:
					if (cmodel != null) {
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
						if( model.getModelType() == Model.Type.PDE ) {
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
					// cMap1D.setRange(module.getMinFitness(), module.getMaxFitness());
					int tag = graph.getModule().getID();
					cMap1D.setRange(model.getMinScore(tag), model.getMaxScore(tag));
					if( model.getModelType() == Model.Type.IBS ) {
						Map2Fitness map2fit = module.getMapToFitness();
						if (cmodel != null) {
// hardcoded colors for min/max mono scores
							cMap1D.setColor(map2fit.map(cmodel.getMinMonoScore(tag)), ColorMap.addAlpha(Color.BLUE.darker(), 220));
							cMap1D.setColor(map2fit.map(cmodel.getMaxMonoScore(tag)), ColorMap.addAlpha(Color.BLUE.brighter(), 220));
						}
						else if (dmodel != null) {
							// mark homogeneous fitness values by pale color
							Color[] pure = module.getTraitColors();
							int nMono = module.getNTraits();
							assert dmodel != null;
							for( int n=0; n<nMono; n++ ) 
								cMap1D.setColor(map2fit.map(dmodel.getMonoScore(tag, n)), 
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
		return hard;
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
		if (model.getModelType() == Model.Type.IBS)
			((Model.IBS) model).mouseHitNode(id, node, alt);
	}

	@Override
	public String getTooltipAt(AbstractGraph agraph, int node) {
		PopGraph3D graph = (PopGraph3D)agraph;
		Geometry geometry = graph.getGeometry();
		int nNodes = geometry.size;
		Module module = graph.getModule();
		int id;
		MeshLambertMaterial[] data = graph.getData();

		StringBuilder tip = new StringBuilder("<table style='border-collapse:collapse;border-spacing:0;'>");
		if (module.getNSpecies() > 1)
			tip.append("<tr><td><i>Species:</i></td><td>"+module.getName()+"</td></tr>");

			switch( model.getModelType() ) {
			case ODE:
				return null;	// no further information available
			case PDE:
				if( node>=nNodes ) {
					// this can only happen for Geometry.LINEAR
					// but linear geometries are not allowed in 3D
					return "Ceci n'est pas une erreur!";
				}
				String[] s = module.getTraitNames();
				Color[] c = module.getTraitColors();
				id = module.getID();
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
				id = module.getID();
				tip.append("<tr><td><i>Node:</i></td><td>"+node+"</td></tr>");
				if( type==Model.Data.STRATEGY ) {
					// strategy: use color-data to color strategy
					tip.append("<tr><td><i>Strategy:</i></td><td><span style='color:#"+data[node].getColor().getHexString()+
							"; font-size:175%; line-height:0.57;'>&#x25A0;</span> "+model.getTraitNameAt(id, node)+"</td></tr>");
				}
				else {
					tip.append("<tr><td><i>Strategy:</i></td><td>"+model.getTraitNameAt(id, node)+"</td></tr>");
				}
				String tag = ibs.getTagNameAt(id, node);
				if (tag != null)
					tip.append("<tr><td><i>Tag:</i></td><td>"+tag+"</td></tr>");
				boolean noFitMap = module.getMapToFitness().isMap(Map2Fitness.Map.NONE);
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

				Geometry intergeom = module.getInteractionGeometry();
				if( intergeom.isUndirected )
					tip.append("<tr><td><i>Neighbors:</i></td><td>"+formatStructureAt(node, intergeom)+"</td></tr>");
				else
					//useful for debugging geometry - Geometry.checkConnections should be able to catch such problems
					tip.append("<tr><td><i>Links to:</i></td><td>"+formatOutStructureAt(node, intergeom)+"</td></tr>"+
							"<tr><td><i>Link here:</i></td><td>"+formatInStructureAt(node, intergeom)+"</td></tr>");
				if( intergeom.isInterspecies() )
					tip.append(formatStrategiesAt(node, intergeom, getOpponentInteractionGraph(graph)));

				Geometry compgeom = module.getCompetitionGeometry();
				if( !compgeom.interCompSame ) {
					if( compgeom.isUndirected )
						tip.append("<tr><td><i>Competitors:</i></td><td>"+formatStructureAt(node, compgeom)+"</td></tr>");
					else
						tip.append("<tr><td><i>Competes for:</i></td><td>"+formatOutStructureAt(node, compgeom)+"</td></tr>"+
								"<tr><td><i>Compete here:</i></td><td>"+formatInStructureAt(node, compgeom)+"</td></tr>");
					if( intergeom.isInterspecies() )
						tip.append(formatStrategiesAt(node, compgeom, getOpponentCompetitionGraph(graph)));
				}
				return tip.append("</table>").toString();
			default:
				return null;
		}
	}

	// NOTE: same as in MVPop2D - not enough to warrant a common abstract class MVPop
	private PopGraph3D getOpponentInteractionGraph(PopGraph3D graph) {
		Module module = graph.getModule();
		Module opponent = module.getOpponent();
		Geometry oppInter = opponent.getInteractionGeometry();
		for( PopGraph3D oppGraph : graphs) {
			if (oppGraph == graph)
				continue;
			Module oppModule = oppGraph.getModule();
			if (oppModule == opponent && oppGraph.getGeometry() == oppInter)
				return oppGraph;
		}
		return null;
	}

	private PopGraph3D getOpponentCompetitionGraph(PopGraph3D graph) {
		Module module = graph.getModule();
		Module opponent = module.getOpponent();
		Geometry oppComp = opponent.getCompetitionGeometry();
		for( PopGraph3D oppGraph : graphs) {
			if (oppGraph == graph)
				continue;
			Module oppModule = oppGraph.getModule();
			if (oppModule == opponent && oppGraph.getGeometry() == oppComp)
				return oppGraph;
		}
		return null;
	}

	private static String formatStrategiesAt(int node, Geometry geom, PopGraph3D graph) {
		int nNeighs = geom.kout[node];
		if (geom.getType() == Geometry.Type.MEANFIELD || nNeighs == 0)
			return "";
		int[] neigh = geom.out[node];
		String msg = "<tr><td><i style='padding-left:2em'>" + graph.getGeometry().getName() + ":</i></td>" +
				"<td>[<span style='color:" + graph.getCSSColorAt(neigh[0])
				+ "; font-size:175%; line-height:0.57;'>&#x25A0;</span>";
		int disp = Math.min(nNeighs, 10);
		for (int n = 1; n < disp; n++)
			msg += "<span style='color:" + graph.getCSSColorAt(neigh[n])
					+ "; font-size:175%; line-height:0.57;'>&nbsp;&#x25A0;</span>";
		if (disp < nNeighs)
			msg += " ...";
		return msg + "]</td></tr>";
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
	protected ExportType[] exportTypes() {
		return new ExportType[] { ExportType.PNG };
//		return new ExportType[] { ExportType.SVG, ExportType.PNG };
	}
}
