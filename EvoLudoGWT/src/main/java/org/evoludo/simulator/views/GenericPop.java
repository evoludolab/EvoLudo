//
// EvoLudo Project
//
// Copyright 2010-2025 Christoph Hauert
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
//	Hauert, Christoph (<year>) EvoLudo Project, https://www.evoludo.org
//			(doi: 10.5281/zenodo.14591549 [, <version>])
//
//	<year>:    year of release (or download), and
//	<version>: optional version number (as reported in output header
//			or GUI console) to simplify replication of reported results.
//
// The formatting may be adjusted to comply with publisher requirements.
//

package org.evoludo.simulator.views;

import java.awt.Color;

import org.evoludo.graphics.AbstractGraph;
import org.evoludo.graphics.GenericPopGraph;
import org.evoludo.graphics.TooltipProvider;
import org.evoludo.math.ArrayMath;
import org.evoludo.simulator.ColorMap;
import org.evoludo.simulator.ColorMapCSS;
import org.evoludo.simulator.EvoLudoGWT;
import org.evoludo.simulator.Geometry;
import org.evoludo.simulator.Network;
import org.evoludo.simulator.Network.Status;
import org.evoludo.simulator.geom.GeometryType;
import org.evoludo.simulator.models.Data;
import org.evoludo.simulator.models.IBS;
import org.evoludo.simulator.models.ModelType;
import org.evoludo.simulator.models.PDE;
import org.evoludo.simulator.modules.Features.Payoffs;
import org.evoludo.simulator.modules.Map2Fitness;
import org.evoludo.simulator.modules.Module;
import org.evoludo.util.Formatter;

/**
 * Abstract base view for rendering population-related data (traits or fitness)
 * for EvoLudo models that expose spatial/graph structure (IBS and PDE models).
 * <p>
 * GenericPop manages one or more {@link GenericPopGraph} instances, coordinates
 * layout and painting, reads data from the model (traits or fitness) and
 * assembles HTML tooltips describing nodes, their neighbourhoods and
 * metrics. The view supports both 2D and 3D variants (through subclasses such
 * as {@code Pop2D} or {@code Pop3D} to determine the concrete geometry string
 * used in the name).
 * 
 * <h3>Responsibilities:</h3>
 * <ul>
 * <li>Manage lifecycle of graphs (initialisation, invalidation,
 * destruction).</li>
 * <li>Query networks for layout readiness and notify the engine when layout
 * completes.</li>
 * <li>Pull trait or fitness data from the underlying model and forward it
 * to graphs for updating and painting.</li>
 * <li>Assign geometries to graphs based on model/module configuration
 * (interaction vs competition geometry).</li>
 * <li>Provide rich HTML tooltips for nodes that include trait names, colors,
 * payoffs/fitness, neighbour lists, tags, and interaction counts.</li>
 * <li>Forward mouse node-hit events to IBS models and handle a small set
 * of view-level keyboard shortcuts (e.g. 's' for shaking network layouts).</li>
 * </ul>
 * </p>
 *
 * @author Christoph Hauert
 * 
 *         Type parameters:
 * @param <T> the element type used by the color map / trait representation
 * @param <N> the concrete {@link Network} type used by graphs
 * @param <G> the concrete {@link GenericPopGraph} type managed by this view
 * 
 * @see Pop2D
 * @see Pop3D
 */
public abstract class GenericPop<T, N extends Network<?>, G extends GenericPopGraph<T, N>> extends AbstractView<G>
		implements TooltipProvider.Index {

	/**
	 * The index of the node that was hit by the mouse.
	 */
	protected int hitNode = -1;

	/**
	 * The type of data to display.
	 */
	private final String dim;

	/**
	 * Construct a new view to display the configuration of the current state of the
	 * EvoLudo model in 2D or 3D.
	 * 
	 * @param engine the pacemaker for running the model
	 * @param type   the type of data to display
	 */
	protected GenericPop(EvoLudoGWT engine, Data type) {
		super(engine, type);
		dim = (this instanceof Pop2D) ? "2D" : "3D";
	}

	@Override
	public String getName() {
		switch (type) {
			case TRAIT:
				return "Traits - " + dim + " Structure";
			case FITNESS:
				return "Fitness - " + dim + " Structure";
			default:
				return null;
		}
	}

	@Override
	public void unload() {
		super.unload();
		hitNode = -1;
	}

	@Override
	public void modelChanged(PendingAction action) {
		super.modelChanged(action);
		if (action == PendingAction.CHANGE_MODE) {
			for (G graph : graphs) {
				if (graph.getGeometry().isUnique())
					graph.invalidate();
			}
		}
	}

	@Override
	public boolean hasLayout() {
		// check if all graphs have layout
		for (G graph : graphs) {
			// graph may not have a network (e.g. for ODE/SDE models)
			Network<?> net = graph.getNetwork();
			if (net != null) {
				Status status = net.getStatus();
				// if network status is neither HAS_LAYOUT nor NO_LAYOUT then layout is not
				// ready
				if (!(status.equals(Status.HAS_LAYOUT) || status.equals(Status.NO_LAYOUT)))
					return false;
			}
		}
		return true;
	}

	@Override
	public void layoutComplete() {
		if (isActive && hasLayout())
			engine.layoutComplete();
		update();
	}

	@Override
	protected void destroyGraphs() {
		// super takes care of removing graphs, just deal with networks here
		for (G graph : graphs) {
			Network<?> net = graph.getNetwork();
			if (net != null)
				net.cancelLayout();
			graph.invalidate();
		}
		super.destroyGraphs();
	}

	/**
	 * Helper method to assign a geometry to a graph.
	 * 
	 * @param graph the graph to assign the geometry
	 * @param inter {@code true} for interaction geometry, {@code false} for
	 *              competition geometry
	 */
	void setGraphGeometry(GenericPopGraph<T, N> graph, boolean inter) {
		ModelType mt = model.getType();
		if (mt.isIBS()) {
			Module<?> module = graph.getModule();
			Geometry igeom = module.getInteractionGeometry();
			Geometry cgeom = module.getCompetitionGeometry();
			Geometry geo = inter ? igeom : cgeom;
			if (!igeom.isSingle() && Geometry.displaySingle(igeom, cgeom))
				// different geometries but only one graph - pick competition.
				// note: this is not a proper solution but fits the requirements of
				// the competition with second nearest neighbours
				geo = cgeom;
			graph.setGeometry(geo);
			return;
		}
		if (mt.isPDE()) {
			graph.setGeometry(((PDE) model).getGeometry());
			return;
		}
		graph.displayMessage("No structure to display in " + type + " model.");
	}

	@Override
	public void modelDidInit() {
		super.modelDidInit();
		for (G graph : graphs)
			graph.init();
		update();
	}

	@Override
	public void update(boolean force) {
		ModelType mt = model.getType();
		if (mt.isIBS() || mt.isPDE()) {
			// always read data - some nodes may have changed due to user actions
			double newtime = model.getUpdates();
			boolean isNext = (Math.abs(timestamp - newtime) > 1e-8);
			timestamp = newtime;
			for (G graph : graphs) {
				boolean doUpdate = (isActive || graph.hasHistory()) && !graph.hasMessage();
				// if graph is neither active nor has history, force can be safely ignored
				// otherwise may lead to problems if graph has never been activated
				if (!doUpdate)
					continue;
				switch (type) {
					case TRAIT:
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
			return;
		}
		for (G graph : graphs)
			graph.displayMessage("No view available for " + type + " model.");
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
		if ("s".equals(key)) {
			for (G graph : graphs)
				graph.getNetwork().shake(graph, 0.05);
			return true;
		}
		return super.keyDownHandler(key);
	}

	@Override
	public void mouseHitNode(int id, int node, boolean alt) {
		ModelType mt = model.getType();
		if (mt.isIBS())
			((IBS) model).mouseHitNode(id, node, alt);
	}

	@Override
	public String getTooltipAt(AbstractGraph<?> agraph, int node) {
		@SuppressWarnings("unchecked")
		G graph = (G) agraph;
		Geometry geometry = graph.getGeometry();
		int nNodes = geometry.getSize();
		Module<?> module = graph.getModule();
		StringBuilder tip = new StringBuilder(TABLE_STYLE);
		if (module.getNSpecies() > 1)
			tip.append(TABLE_ROW_START)
					.append("Species")
					.append(TABLE_CELL_NEXT)
					.append(module.getName())
					.append(TABLE_ROW_END);

		ModelType mt = model.getType();
		// Delegate heavy logic to dedicated helpers to reduce cognitive complexity
		if (mt.isIBS())
			return tooltipForIBS(node, nNodes, module, graph, tip);
		if (mt.isPDE())
			return tooltipForPDE(node, nNodes, geometry, module, graph, tip);
		return null;
	}

	/**
	 * Assemble tooltip for IBS models.
	 */
	private String tooltipForIBS(int node, int nNodes, Module<?> module, G graph,
			StringBuilder tip) {
		tip.append(TABLE_ROW_START).append("Node").append(TABLE_CELL_NEXT);
		if (node >= nNodes) {
			// linear/time slices handled separately
			appendLinearTip(node, nNodes, tip);
			return tip.append(TABLE_END).toString();
		}
		IBS ibs = (IBS) model;
		int id = module.getID();

		// node + trait (+ color if trait view)
		appendNodeTraitTip(node, id, graph, tip);

		// tag if present
		String tag = ibs.getTagNameAt(id, node);
		if (tag != null)
			tip.append(TABLE_ROW_START)
					.append("Tag")
					.append(TABLE_CELL_NEXT)
					.append(tag)
					.append(TABLE_ROW_END);

		// payoffs / fitness / interactions
		appendFitnessTip(node, module, tip);

		// in multi-species modules this points to the other species
		Geometry intergeom = module.getInteractionGeometry();
		appendInterNeighborsAt(node, intergeom, tip);
		if (intergeom.isInterspecies()) {
			// in inter-species interactions, show traits of other species
			G oppInterGraph = getOppInterGraph(graph);
			if (oppInterGraph != null)
				appendOppTraitsAt(node, intergeom, oppInterGraph, tip);
		}

		// competition neighbours and opponent competition traits
		if (!intergeom.isSingle()) {
			Geometry compgeom = module.getCompetitionGeometry();
			appendCompNeighborsAt(node, compgeom, tip);
			G oppCompGraph = getOppCompGraph(graph);
			if (oppCompGraph != null)
				appendOppTraitsAt(node, compgeom, oppCompGraph, tip);
		}
		return tip.append(TABLE_END).toString();
	}

	/**
	 * Append tooltip information for linear geometry / time slices.
	 * 
	 * @param node   the node index
	 * @param nNodes the number of nodes per time slice
	 * @param tip    the StringBuilder to append to
	 */
	private void appendLinearTip(int node, int nNodes, StringBuilder tip) {
		// this can only happen for Geometry.LINEAR
		int idx = node / nNodes;
		node %= nNodes;
		double t = idx * model.getTimeStep();
		tip.append(node)
				.append(TABLE_ROW_END)
				.append(TABLE_ROW_START)
				.append("Time")
				.append(TABLE_CELL_NEXT)
				.append(Formatter.format(-t, 2))
				.append(TABLE_ROW_END);
	}

	/**
	 * Append node and trait information (with color when appropriate).
	 * 
	 * @param node  the node index
	 * @param id    the module ID
	 * @param graph the graph
	 * @param tip   the StringBuilder to append to
	 */
	private void appendNodeTraitTip(int node, int id, G graph, StringBuilder tip) {
		tip.append(node)
				.append(TABLE_ROW_END)
				.append(TABLE_ROW_START)
				.append("Trait");
		if (type == Data.TRAIT) {
			// trait: use color-data to color trait
			tip.append(TABLE_CELL_NEXT_COLOR)
					.append(graph.getCSSColorAt(node))
					.append(TABLE_CELL_BULLET);
		} else {
			tip.append(TABLE_CELL_NEXT);
		}
		tip.append(model.getTraitNameAt(id, node))
				.append(TABLE_ROW_END);
	}

	/**
	 * Append trait and density information for PDE models.
	 * 
	 * @param node   the node index
	 * @param module the module
	 * @param graph  the graph
	 * @param tip    the StringBuilder to append to
	 */
	private void appendPDETraitTip(int node, Module<?> module, G graph, StringBuilder tip) {
		String[] s = module.getTraitNames();
		Color[] c = module.getTraitColors();
		tip.append(node)
				.append(TABLE_ROW_END)
				.append(TABLE_ROW_START)
				.append("Traits")
				.append(TABLE_CELL_NEXT_COLOR)
				.append(ColorMapCSS.Color2Css(c[0]))
				.append(TABLE_CELL_BULLET)
				.append(s[0]);
		for (int n = 1; n < s.length; n++)
			tip.append(", <span style='color:")
					.append(ColorMapCSS.Color2Css(c[n]))
					.append(TABLE_CELL_BULLET)
					.append(s[n]);
		tip.append(TABLE_ROW_END)
				.append(TABLE_ROW_START)
				.append("Densities");
		if (type == Data.TRAIT)
			tip.append(TABLE_CELL_NEXT_COLOR)
					.append(graph.getCSSColorAt(node))
					.append(TABLE_CELL_BULLET);
		else
			tip.append(TABLE_CELL_NEXT);
		tip.append(model.getTraitNameAt(module.getID(), node))
				.append(TABLE_ROW_END);
	}

	/**
	 * Append fitness (and payoffs if the module is using payoff-to-fitness
	 * mappings) as well as interaction count for modules implementing the
	 * {@code Payoffs} interface.
	 * 
	 * @param node   the node index
	 * @param module the module
	 * @param tip    the StringBuilder to append to
	 */
	private void appendFitnessTip(int node, Module<?> module, StringBuilder tip) {
		if (!(module instanceof Payoffs))
			return;
		int id = module.getID();
		// with payoff-to-fitness report score first, then fitness (see below)
		// with fitness map report scores as well
		if (!module.getMap2Fitness().isMap(Map2Fitness.Map.NONE))
			// report payoff
			tip.append(TABLE_ROW_START)
					.append("Payoff")
					.append(TABLE_CELL_NEXT)
					.append(model.getScoreNameAt(id, node))
					.append(TABLE_ROW_END);

		// report fitness
		tip.append(TABLE_ROW_START)
				.append("Fitness")
				.append(TABLE_CELL_NEXT)
				.append(model.getFitnessNameAt(id, node))
				.append(TABLE_ROW_END);

		IBS ibs = (IBS) model;
		int count = ibs.getInteractionsAt(id, node);
		if (count >= 0) {
			tip.append(TABLE_ROW_START)
					.append("Interactions")
					.append(TABLE_CELL_NEXT);
			if (count == Integer.MAX_VALUE)
				tip.append("all");
			else
				tip.append(count);
			tip.append(TABLE_ROW_END);
		}
	}

	/**
	 * Append fitness and payoff information for PDE models.
	 * 
	 * @param node   the node index
	 * @param module the module
	 * @param graph  the graph
	 * @param tip    the StringBuilder to append to
	 */
	private void appendPDEFitnessTip(int node, Module<?> module, G graph, StringBuilder tip) {
		if (!(module instanceof Payoffs))
			return;

		// with payoff-to-fitness report score first, then fitness (see below)
		// with fitness map report scores as well
		int vac = module.getVacantIdx();
		int id = module.getID();
		double[] fitness = model.getMeanFitnessAt(id, node);
		Map2Fitness map = module.getMap2Fitness();

		if (!map.isMap(Map2Fitness.Map.NONE)) {
			// report payoff
			tip.append(TABLE_ROW_START)
					.append("Payoff")
					.append(TABLE_CELL_NEXT);
			appendFormattedValues(tip, fitness, vac, map, true);
			tip.append(TABLE_ROW_END);
		}
		// report fitness
		tip.append(TABLE_ROW_START)
				.append("Fitness")
				.append(TABLE_CELL_NEXT);
		if (type == Data.FITNESS)
			tip.append("<span style='color:")
					.append(graph.getCSSColorAt(node)).append(TABLE_CELL_BULLET);
		appendFormattedValues(tip, fitness, vac, map, false);
		tip.append(" → ")
				.append(Formatter.pretty(
						ArrayMath.dot(model.getMeanTraitAt(id, node), fitness), 3))
				.append(TABLE_ROW_END);
	}

	/**
	 * Append a comma separated list of values (or '-' for vacant index).
	 * If useInvMap is true, values are converted via map.invmap(...).
	 * 
	 * @param tip       the StringBuilder to append to
	 * @param values    the array of values
	 * @param vac       the vacant index
	 * @param map       the Map2Fitness
	 * @param useInvMap whether to convert values via map.invmap(...)
	 */
	private void appendFormattedValues(StringBuilder tip, double[] values, int vac, Map2Fitness map,
			boolean useInvMap) {
		for (int i = 0; i < values.length; i++) {
			if (i > 0)
				tip.append(", ");
			if (i == vac) {
				tip.append("-");
				continue;
			}
			if (useInvMap)
				tip.append(Formatter.formatFix(map.invmap(values[i]), 3));
			else
				tip.append(Formatter.formatFix(values[i], 3));
		}
	}

	/**
	 * Assemble tooltip for PDE models.
	 * 
	 * @param node     the node index
	 * @param nNodes   the total number of nodes
	 * @param geometry the geometry of the graph
	 * @param module   the module
	 * @param graph    the graph
	 * @param tip      the StringBuilder to append to
	 */
	private String tooltipForPDE(int node, int nNodes, Geometry geometry, Module<?> module, G graph,
			StringBuilder tip) {
		tip.append(TABLE_ROW_START).append("Node").append(TABLE_CELL_NEXT);

		if (node >= nNodes) {
			// linear/time slices handled separately
			appendLinearTip(node, nNodes, tip);
			return tip.append(TABLE_END).toString();
		}

		appendPDETraitTip(node, module, graph, tip);
		appendPDEFitnessTip(node, module, graph, tip);

		if (geometry.isUndirected())
			appendNeighbors("Connections", geometry.out[node], geometry.kout[node], tip);
		else {
			// useful for debugging geometry - Geometry.checkConnections should be able to
			// catch such problems
			appendNeighbors("Links for", geometry.out[node], geometry.kout[node], tip);
			appendNeighbors("Link here", geometry.in[node], geometry.kin[node], tip);
		}
		return tip.toString();
	}

	/**
	 * Get the interaction graph of the opponent of the module associated with the
	 * given graph.
	 * 
	 * @param graph the graph for which to get the opponent's interaction graph
	 * @return the interaction graph of the opponent
	 */
	private G getOppInterGraph(G graph) {
		Module<?> module = graph.getModule();
		Module<?> opponent = module.getOpponent();
		Geometry oppInter = opponent.getInteractionGeometry();
		for (G oppGraph : graphs) {
			if (oppGraph == graph)
				continue;
			Module<?> oppModule = oppGraph.getModule();
			// XXX this should work but somehow the pointers are different even though the
			// objects appear to be the same...
			// if (oppModule == opponent && oppGraph.getGeometry() == oppInter)
			if (oppModule == opponent && oppGraph.getGeometry().name.equals(oppInter.name))
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
	private G getOppCompGraph(G graph) {
		Module<?> module = graph.getModule();
		Module<?> opponent = module.getOpponent();
		Geometry oppComp = opponent.getCompetitionGeometry();
		for (G oppGraph : graphs) {
			if (oppGraph == graph)
				continue;
			Module<?> oppModule = oppGraph.getModule();
			// this should work but somehow the pointers are different even though the
			// objects appear to be the same...
			// if (oppModule == opponent && oppGraph.getGeometry() == oppComp)
			if (oppModule == opponent && oppGraph.getGeometry().name.equals(oppComp.name))
				return oppGraph;
		}
		return null;
	}

	/**
	 * Append opponent traits at the given node.
	 * 
	 * @param node  the node index
	 * @param geom  the geometry
	 * @param graph the opponent's graph
	 * @param tip   the StringBuilder to append to
	 * @return the updated StringBuilder
	 */
	private StringBuilder appendOppTraitsAt(int node, Geometry geom, G graph, StringBuilder tip) {
		tip.append(TABLE_ROW_START)
				.append(graph.getGeometry().getName())
				.append(TABLE_CELL_NEXT);
		int nNeighs = geom.kout[node];
		if (nNeighs == 0)
			return tip.append("[ - ]")
					.append(TABLE_ROW_END);
		int[] neigh = geom.out[node];
		tip.append("[<span style='color:")
				.append(graph.getCSSColorAt(neigh[0]))
				.append(TABLE_CELL_BULLET);
		int disp = Math.min(nNeighs, 10);
		for (int n = 1; n < disp; n++)
			tip.append("<span style='color:")
					.append(graph.getCSSColorAt(neigh[n]))
					.append(TABLE_CELL_BULLET);
		if (disp < nNeighs)
			tip.append(" ...");
		tip.append("]")
				.append(TABLE_ROW_END);
		return tip;
	}

	/**
	 * Append interaction neighbours at the given node.
	 * 
	 * @param node the node index
	 * @param geom the geometry
	 * @param tip  the StringBuilder to append to
	 * @return the updated StringBuilder
	 */
	private static StringBuilder appendInterNeighborsAt(int node, Geometry geom, StringBuilder tip) {
		if (geom.isUndirected()) {
			// well-mixed is by definition undirected
			if (geom.getType().equals(GeometryType.WELLMIXED)) {
				tip.append(TABLE_ROW_START)
						.append("Neighbours")
						.append(TABLE_CELL_NEXT)
						.append("all");
				tip.append(TABLE_ROW_END);
			} else
				appendNeighbors("Neighbours", geom.out[node], geom.kout[node], tip);
		} else {
			// useful for debugging geometry - Geometry.checkConnections should be able to
			// catch such problems
			appendNeighbors("Links to", geom.out[node], geom.kout[node], tip);
			appendNeighbors("Link here", geom.in[node], geom.kin[node], tip);
		}
		return tip;
	}

	/**
	 * Append competition neighbours at the given node.
	 * 
	 * @param node the node index
	 * @param geom the geometry
	 * @param tip  the StringBuilder to append to
	 * @return the updated StringBuilder
	 */
	private static StringBuilder appendCompNeighborsAt(int node, Geometry geom, StringBuilder tip) {
		if (geom.isUndirected()) {
			// well-mixed is by definition undirected
			if (geom.getType().equals(GeometryType.WELLMIXED)) {
				tip.append(TABLE_ROW_START)
						.append("Competitors")
						.append(TABLE_CELL_NEXT)
						.append("all");
				tip.append(TABLE_ROW_END);
			} else
				appendNeighbors("Competitors", geom.out[node], geom.kout[node], tip);
		} else {
			// useful for debugging geometry - Geometry.checkConnections should be able to
			// catch such problems
			appendNeighbors("Competes for", geom.out[node], geom.kout[node], tip);
			appendNeighbors("Compete here", geom.in[node], geom.kin[node], tip);
		}
		return tip;
	}

	/**
	 * Return a formatted string of the neighbourhood structure at the given node.
	 * 
	 * @param links the array of indices of the neighbours
	 * @param k     the number of links
	 * @param type  the type of the geometry
	 * @return the formatted string
	 */
	private static StringBuilder appendNeighbors(String label, int[] links, int k, StringBuilder tip) {
		tip.append(TABLE_ROW_START)
				.append(label)
				.append(TABLE_CELL_NEXT);
		switch (k) {
			case 0:
				tip.append("none");
				break;
			case 1:
				tip.append("1 [" + links[0] + "]");
				break;
			default:
				tip.append(k).append(" [").append(links[0]);
				int disp = Math.min(k, 10);
				for (int n = 1; n < disp; n++)
					tip.append(" ").append(links[n]);
				if (disp < k)
					tip.append(" ...");
				tip.append("]");
		}
		tip.append(TABLE_ROW_END);
		return tip;
	}
}