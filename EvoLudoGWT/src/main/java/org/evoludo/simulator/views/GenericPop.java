package org.evoludo.simulator.views;

import java.awt.Color;
import java.util.List;

import org.evoludo.graphics.AbstractGraph;
import org.evoludo.graphics.GenericPopGraph;
import org.evoludo.graphics.GenericPopGraph.PopGraphController;
import org.evoludo.graphics.TooltipProvider;
import org.evoludo.math.ArrayMath;
import org.evoludo.simulator.ColorMap;
import org.evoludo.simulator.ColorMapCSS;
import org.evoludo.simulator.EvoLudoGWT;
import org.evoludo.simulator.Geometry;
import org.evoludo.simulator.Network;
import org.evoludo.simulator.Network.Status;
import org.evoludo.simulator.models.Model;
import org.evoludo.simulator.modules.Map2Fitness;
import org.evoludo.simulator.modules.Module;
import org.evoludo.util.Formatter;

/**
 * The abstract parent class for views that display the configuration of the
 * current state of the model in 2D or 3D. The visual representation depends on
 * the geometry of the model. Lattice structures have a fixed layout but all
 * other strutures are dynamically generated through a process insipired by the
 * physical arrangement of charged spheres that are connected by springs. The
 * spheres represent members of the population and the springs represent their
 * interaction (or competition) neighbourhood. The size of the sphere scales
 * with the size of the individual's neighbourhood. Moreover, the colour of the
 * spheres reflects the state of the individual, for example their trait or
 * fitness.
 *
 * @author Christoph Hauert
 * 
 * @param <T> type of color object: String for 2D and
 *            {@link thothbot.parallax.core.shared.materials.MeshLambertMaterial
 *            MeshLambertMaterial} for 3D
 * @param <N> type of network
 * @param <G> type of graph
 */
public abstract class GenericPop<T, N extends Network, G extends GenericPopGraph<T, N>> extends AbstractView
		implements PopGraphController, TooltipProvider.Index {

	/**
	 * The list of graphs that display the time series data.
	 * 
	 * @evoludo.impl {@code List<G> graphs} is deliberately hiding
	 *               {@code List<AbstractGraph> graphs} from the superclass because
	 *               it saves a lot of ugly casting. Note that the two fields point
	 *               to one and the same object.
	 */
	@SuppressWarnings("hiding")
	protected List<G> graphs;

	/**
	 * The index of the node that was hit by the mouse.
	 */
	protected int hitNode = -1;

	/**
	 * The type of data to display.
	 */
	String tag;

	/**
	 * Construct a new view to display the configuration of the current state of the
	 * EvoLudo model in 2D or 3D.
	 * 
	 * @param engine the pacemaker for running the model
	 * @param type   the type of data to display
	 */
	@SuppressWarnings("unchecked")
	public GenericPop(EvoLudoGWT engine, Model.Data type) {
		super(engine, type);
		graphs = (List<G>) super.graphs;
		tag = (this instanceof Pop2D) ? "2D" : "3D";
	}

	@Override
	public String getName() {
		switch (type) {
			case STRATEGY:
				return "Strategies - " + tag + " Structure";
			case FITNESS:
				return "Fitness - " + tag + " Structure";
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
		if (isActive) {
			// check if all graphs have layout
			boolean layouting = false;
			for (G graph : graphs) {
				// graph may not have a network (e.g. for ODE/SDE models)
				Network net = graph.getNetwork();
				if (net == null)
					continue;
				Status status = net.getStatus();
				if (status.equals(Status.HAS_LAYOUT) || status.equals(Status.NO_LAYOUT))
					continue;
				layouting = true;
				break;
			}
			if (!layouting)
				engine.layoutComplete();
		}
		update();
	}

	@Override
	protected void destroyGraphs() {
		// super takes care of removing graphs, just deal with networks here
		for (G graph : graphs) {
			Network net = graph.getNetwork();
			if (net != null)
				net.cancelLayout();
			graph.invalidate();
		}
		super.destroyGraphs();
	}

	@Override
	public void init() {
		super.init();
		for (G graph : graphs)
			graph.init();
		update();
	}

	@Override
	public void update(boolean force) {
		switch (model.getModelType()) {
			case ODE:
				for (G graph : graphs)
					graph.displayMessage("No view available (ODE Solver)");
				break;
			case SDE:
				for (G graph : graphs)
					graph.displayMessage("No view available (SDE Solver)");
				break;
			case PDE:
			default:
				// always read data - some nodes may have changed due to user actions
				double newtime = model.getTime();
				boolean isNext = (Math.abs(timestamp - newtime) > 1e-8);
				timestamp = newtime;
				for (G graph : graphs) {
					boolean doUpdate = (isActive || graph.hasHistory()) && !graph.hasMessage();
					// if graph is neither active nor has history, force can be safely ignored
					// otherwise may lead to problems if graph has never been activated
					if (!doUpdate)
						continue;
					switch (type) {
						case STRATEGY:
							model.getTraitData(graph.getModule().getID(), graph.getData(), graph.getColorMap());
							break;
						case FITNESS:
							// cast should be safe for fitness data
							model.getFitnessData(graph.getModule().getID(), graph.getData(),
									(ColorMap.Gradient1D<T>) graph.getColorMap());
							break;
						default:
							break;
					}
					graph.update(isNext);
					graph.paint(force);
				}
		}
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
		switch (key) {
			case "s":
				for (G graph : graphs)
					graph.getNetwork().shake(graph, 0.05);
				return true;
			default:
				return super.keyDownHandler(key);
		}
	}

	@Override
	public void mouseHitNode(int id, int node, boolean alt) {
		if (model.getModelType() == Model.Type.IBS)
			((Model.IBS) model).mouseHitNode(id, node, alt);
	}

	@Override
	public String getTooltipAt(AbstractGraph<?> agraph, int node) {
		@SuppressWarnings("unchecked")
		G graph = (G) agraph;
		Geometry geometry = graph.getGeometry();
		int nNodes = geometry.size;
		Module module = graph.getModule();
		int id;
		StringBuilder tip = new StringBuilder("<table style='border-collapse:collapse;border-spacing:0;'>");
		if (module.getNSpecies() > 1)
			tip.append("<tr><td><i>Species:</i></td><td>" + module.getName() + "</td></tr>");

		switch (model.getModelType()) {
			case ODE:
				return null; // no further information available

			case PDE:
				if (node >= nNodes) {
					// this can only happen for Geometry.LINEAR
					int idx = node / nNodes;
					node %= nNodes;
					double t = idx * engine.getReportInterval();
					tip.append("<tr><td><i>Node:</i></td><td>" + node + "</td></tr>" +
							"<tr><td><i>Time:</i></td><td>" + Formatter.format(-t, 2) + "</td></tr>");
					return tip + "</table>";
					// NOTE: reverse engineer color data from RingBuffer? - see below
				}
				String[] s = module.getTraitNames();
				Color[] c = module.getTraitColors();
				id = module.getID();
				String names = "<tr><td><i>Strategies:</i></td><td><span style='color:" + ColorMapCSS.Color2Css(c[0]) +
						"; font-size:175%; line-height:0.57;'>&#x25A0;</span> " + s[0];
				for (int n = 1; n < s.length; n++)
					names += ", <span style='color:" + ColorMapCSS.Color2Css(c[n]) +
							"; font-size:175%; line-height:0.57;'>&#x25A0;</span> " + s[n];
				names += "</td></tr>";
				String density = "";
				if (type == Model.Data.STRATEGY)
					density = "<tr><td><i>Densities:</i></td><td><span style='color:" + graph.getCSSColorAt(node) +
							"; font-size:175%; line-height:0.57;'>&#x25A0;</span> " + model.getTraitNameAt(id, node)
							+ "</td></tr>";
				else
					density = "<tr><td><i>Densities:</i></td><td>" + model.getTraitNameAt(id, node) + "</td></tr>";
				String fitness = "";
				if (type == Model.Data.FITNESS)
					fitness = "<tr><td><i>Fitness:</i></td><td><span style='color:" + graph.getCSSColorAt(node) +
							"; font-size:175%; line-height:0.57;'>&#x25A0;</span> " + model.getFitnessNameAt(id, node);
				else
					fitness = "<tr><td><i>Fitness:</i></td><td>" + model.getFitnessNameAt(id, node);
				fitness += " → "
						+ Formatter.pretty(
								ArrayMath.dot(model.getMeanTraitAt(id, node), model.getMeanFitnessAt(id, node)), 3)
						+ "</td></tr>";
				tip.append("<tr><td><i>Node:</i></td><td>" + node + "</td></tr>" + names + density + fitness);
				if (geometry.isUndirected)
					tip.append(
							"<tr><td><i>Connections:</i></td><td>" + formatStructureAt(node, geometry) + "</td></tr>");
				else
					tip.append("<tr><td><i>Links to:</i></td><td>" + formatOutStructureAt(node, geometry) + "</td></tr>"
							+
							"<tr><td><i>Link here:</i></td><td>" + formatInStructureAt(node, geometry) + "</td></tr>");
				return tip.append("</table>").toString();

			case IBS:
				if (node >= nNodes) {
					// this can only happen for Geometry.LINEAR
					int idx = node / nNodes;
					node %= nNodes;
					double t = idx * engine.getReportInterval();
					tip.append("<tr><td><i>Node:</i></td><td>" + node + "</td></tr>" +
							"<tr><td><i>Time:</i></td><td>" + Formatter.format(-t, 2) + "</td></tr>");
					return tip + "</table>";
					// NOTE: RingBuffer contains color-strings; requires reverse engineering to
					// determine strategies...
					// RingBuffer.Array<String> buffer = graph.getBuffer();
					// if( idx>=buffer.size() ) return tip+"</table>";
					// String[] colors = graph.getBuffer().get(idx);
					// return tip+"<tr><td
					// colspan='2'><hr/></td></tr><tr><td><i>Strategy:</i></td><td
					// style='color:"+colors[node]+";'>"+
					// population.getTraitNameAt(node)+"</td></tr></table>";
				}
				Model.IBS ibs = (Model.IBS) model;
				id = module.getID();
				tip.append("<tr><td><i>Node:</i></td><td>" + node + "</td></tr>");
				if (type == Model.Data.STRATEGY) {
					// strategy: use color-data to color strategy
					tip.append("<tr><td><i>Strategy:</i></td><td><span style='color:" + graph.getCSSColorAt(node) +
							"; font-size:175%; line-height:0.57;'>&#x25A0;</span> " + model.getTraitNameAt(id, node)
							+ "</td></tr>");
				} else {
					tip.append("<tr><td><i>Strategy:</i></td><td>" + model.getTraitNameAt(id, node) + "</td></tr>");
				}
				String tag = ibs.getTagNameAt(id, node);
				if (tag != null)
					tip.append("<tr><td><i>Tag:</i></td><td>" + tag + "</td></tr>");
				// with payoff-to-fitness report score first, then fitness (see below)
				boolean noFitMap = module.getMapToFitness().isMap(Map2Fitness.Map.NONE);
				String label = (noFitMap ? "Fitness" : "Score");
				if (type == Model.Data.FITNESS) {
					// fitness: use color-data to color strategy
					tip.append("<tr><td><i>" + label + ":</i></td><td><span style='color:" + graph.getCSSColorAt(node) +
							"; font-size:175%; line-height:0.57;'>&#x25A0;</span> " + model.getScoreNameAt(id, node)
							+ "</td></tr>");
				} else {
					tip.append(
							"<tr><td><i>" + label + ":</i></td><td>" + model.getScoreNameAt(id, node) + "</td></tr>");
				}
				if (!noFitMap)
					tip.append("<tr><td><i>Fitness:</i></td><td>" + model.getFitnessNameAt(id, node) + "</td></tr>");
				int count = ibs.getInteractionsAt(id, node);
				if (count >= 0) {
					if (count == Integer.MAX_VALUE)
						tip.append("<tr><td><i>Interactions:</i></td><td>all</td></tr>");
					else
						tip.append("<tr><td><i>Interactions:</i></td><td>" + count + "</td></tr>");
				}

				Geometry intergeom = module.getInteractionGeometry();
				if (intergeom.isUndirected)
					tip.append(
							"<tr><td><i>Neighbors:</i></td><td>" + formatStructureAt(node, intergeom) + "</td></tr>");
				else
					// useful for debugging geometry - Geometry.checkConnections should be able to
					// catch such problems
					tip.append("<tr><td><i>Links to:</i></td><td>" + formatOutStructureAt(node, intergeom)
							+ "</td></tr>" +
							"<tr><td><i>Link here:</i></td><td>" + formatInStructureAt(node, intergeom) + "</td></tr>");
				if (intergeom.isInterspecies())
					tip.append(formatStrategiesAt(node, intergeom, getOpponentInteractionGraph(graph)));

				Geometry compgeom = module.getCompetitionGeometry();
				if (!compgeom.interCompSame) {
					if (compgeom.isUndirected)
						tip.append("<tr><td><i>Competitors:</i></td><td>" + formatStructureAt(node, compgeom)
								+ "</td></tr>");
					else
						tip.append("<tr><td><i>Competes for:</i></td><td>" + formatOutStructureAt(node, compgeom)
								+ "</td></tr>" +
								"<tr><td><i>Compete here:</i></td><td>" + formatInStructureAt(node, compgeom)
								+ "</td></tr>");
					if (intergeom.isInterspecies())
						tip.append(formatStrategiesAt(node, compgeom, getOpponentCompetitionGraph(graph)));
				}
				return tip.append("</table>").toString();

			default:
				return null;
		}
	}

	/**
	 * Get the interaction graph of the opponent of the module associated with the
	 * given graph.
	 * 
	 * @param graph the graph for which to get the opponent's interaction graph
	 * @return the interaction graph of the opponent
	 */
	private G getOpponentInteractionGraph(G graph) {
		Module module = graph.getModule();
		Module opponent = module.getOpponent();
		Geometry oppInter = opponent.getInteractionGeometry();
		for (G oppGraph : graphs) {
			if (oppGraph == graph)
				continue;
			Module oppModule = oppGraph.getModule();
			if (oppModule == opponent && oppGraph.getGeometry() == oppInter)
				return oppGraph;
		}
		return null;
	}

	/**
	 * Get the competition graph of the opponent of the module associated with the
	 * given graph.
	 * 
	 * @param graph the graph for which to get the opponent's competition graph
	 * @return the competition graph of the opponent
	 */
	private G getOpponentCompetitionGraph(G graph) {
		Module module = graph.getModule();
		Module opponent = module.getOpponent();
		Geometry oppComp = opponent.getCompetitionGeometry();
		for (G oppGraph : graphs) {
			if (oppGraph == graph)
				continue;
			Module oppModule = oppGraph.getModule();
			if (oppModule == opponent && oppGraph.getGeometry() == oppComp)
				return oppGraph;
		}
		return null;
	}

	/**
	 * Return a formatted string of the strategies of the opponents at the given
	 * node. For intra-species interactions and models with identical interaction
	 * and competition graphs, {@code graph.getGeometry() == geom} holds.
	 * 
	 * @param node  the index of the node
	 * @param geom  the geometry of the graph
	 * @param graph the graph of the opponent
	 * @return the formatted string
	 */
	private String formatStrategiesAt(int node, Geometry geom, G graph) {
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

	/**
	 * Return a formatted string of the neighbourhood structure at the given node.
	 * 
	 * @param node the index of the node
	 * @param geom the geometry of the graph
	 * @return the formatted string
	 */
	private static String formatStructureAt(int node, Geometry geom) {
		return formatStructureAt(geom.out[node], geom.kout[node], geom.getType());
	}

	/**
	 * Return a formatted string of the outgoing neighbours on directed graphs at
	 * the given node.
	 * 
	 * @param node the index of the node
	 * @param geom the geometry of the graph
	 * @return the formatted string
	 */
	private static String formatOutStructureAt(int node, Geometry geom) {
		return formatStructureAt(geom.out[node], geom.kout[node], geom.getType());
	}

	/**
	 * Return a formatted string of the incoming neighbours on directed graphs at
	 * the given node.
	 * 
	 * @param node the index of the node
	 * @param geom the geometry of the graph
	 * @return the formatted string
	 */
	private static String formatInStructureAt(int node, Geometry geom) {
		return formatStructureAt(geom.in[node], geom.kin[node], geom.getType());
	}

	/**
	 * Return a formatted string of the neighbourhood structure at the given node.
	 * 
	 * @param links the array of indices of the neighbours
	 * @param k     the number of links
	 * @param type  the type of the geometry
	 * @return the formatted string
	 */
	private static String formatStructureAt(int[] links, int k, Geometry.Type type) {
		if (type == Geometry.Type.MEANFIELD)
			return "all";
		String msg;
		switch (k) {
			case 0:
				return "none";
			case 1:
				return "1 [" + links[0] + "]";
			default:
				msg = k + " [" + links[0];
				int disp = Math.min(k, 10);
				for (int n = 1; n < disp; n++)
					msg += " " + links[n];
				if (disp < k)
					msg += " ...";
				msg += "]";
		}
		return msg;
	}
}
