//
// EvoLudo Project
//
// Copyright 2010-2026 Christoph Hauert
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

import static org.evoludo.simulator.views.BasicTooltipProvider.tableRow;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import org.evoludo.graphics.AbstractGraph;
import org.evoludo.graphics.GenericPopGraph;
import org.evoludo.graphics.PopGraph1D;
import org.evoludo.graphics.TooltipProvider;
import org.evoludo.math.ArrayMath;
import org.evoludo.simulator.ColorMap;
import org.evoludo.simulator.ColorMapCSS;
import org.evoludo.simulator.EvoLudoGWT;
import org.evoludo.simulator.Network;
import org.evoludo.simulator.Network.Status;
import org.evoludo.simulator.geometries.AbstractGeometry;
import org.evoludo.simulator.geometries.GeometryType;
import org.evoludo.simulator.models.IBS;
import org.evoludo.simulator.models.IBSPopulation;
import org.evoludo.simulator.models.Model;
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
	 * The flag to indicate whether the view shows fitness, otherwise traits.
	 */
	protected final boolean isFitness;

	/**
	 * Construct a new view to display the configuration of the current state of the
	 * EvoLudo model in 2D or 3D.
	 * 
	 * @param engine    the pacemaker for running the model
	 * @param isFitness {@code true} to show fitness, {@code false} for traits
	 */
	protected GenericPop(EvoLudoGWT engine, boolean isFitness) {
		super(engine);
		this.isFitness = isFitness;
	}

	@Override
	public String getName() {
		return (isFitness ? "Fitness - " : "Traits - ") + (this instanceof Pop2D ? "2D" : "3D") + " Structure";
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
	 * Helper method to check whether a single graph suffices to display the
	 * interaction and competition geometries of {@code module}.
	 * <p>
	 * <strong>Note:</strong> this is a display criterion, which is more permissive
	 * than {@link AbstractGeometry#isSingle()}: distinct geometries can still share
	 * a single graphical representation, e.g. a square lattice for interactions
	 * combined with one for competition among second nearest neighbours.
	 *
	 * @param module the module to check the geometries of
	 * @return {@code true} if a single graph suffices
	 *
	 * @see AbstractGeometry#displaySingle(AbstractGeometry, AbstractGeometry)
	 */
	static boolean displaySingle(Module<?> module) {
		IBSPopulation<?, ?> pop = module.getIBSPopulation();
		return AbstractGeometry.displaySingle(pop.getInteractionGeometry(), pop.getCompetitionGeometry());
	}

	/**
	 * Get the species shown by the graphs of IBS models and arrange the graphs:
	 * one graph per species, or two if the interaction and competition geometries
	 * require separate graphs (see {@link #displaySingle(Module)}), in which case
	 * the first shows the interaction geometry. Each species occupies a row,
	 * unless there are only two graphs, which are arranged horizontally.
	 *
	 * @return the species of each graph
	 */
	List<Module<?>> layoutIBSGraphs() {
		List<? extends Module<?>> species = engine.getModule().getSpecies();
		List<Module<?>> slots = new ArrayList<>();
		gCols = 1;
		for (Module<?> mod : species) {
			slots.add(mod);
			if (displaySingle(mod))
				continue;
			slots.add(mod);
			gCols = 2;
		}
		gRows = species.size();
		if (gRows * gCols == 2) {
			gRows = 1;
			gCols = 2;
		}
		return slots;
	}

	/**
	 * Helper method to assign a geometry to a graph.
	 *
	 * @param graph the graph to assign the geometry
	 * @param inter {@code true} for interaction geometry, {@code false} for
	 *              competition geometry
	 */
	void setGraphGeometry(GenericPopGraph<T, N> graph, boolean inter) {
		if (model.isIBS()) {
			Module<?> module = graph.getModule();
			AbstractGeometry igeom = module.getIBSPopulation().getInteractionGeometry();
			AbstractGeometry cgeom = module.getIBSPopulation().getCompetitionGeometry();
			AbstractGeometry geo = inter ? igeom : cgeom;
			if (!igeom.isSingle() && AbstractGeometry.displaySingle(igeom, cgeom))
				// different geometries but only one graph - pick competition.
				// note: this is not a proper solution but fits the requirements of
				// the competition with second nearest neighbours
				geo = cgeom;
			graph.setGeometry(geo);
			return;
		}
		if (model.isPDE()) {
			graph.setGeometry(((PDE) model).getGeometry());
			return;
		}
		graph.displayMessage("No structure to display in " + model.getType().getKey() + " model.");
	}

	@Override
	protected void onModelDidInit() {
		for (G graph : graphs)
			graph.init();
		updateData(true);
		update();
	}

	@Override
	protected void updateData(boolean force) {
		// force intentionally ignored; update policy depends on active/history state.
		if (!(model.isIBS() || model.isPDE()))
			return;
		// always read data for the selected graph set
		double newtime = model.getUpdates();
		boolean isNext = (Math.abs(timestamp - newtime) > Model.EPS);
		boolean updated = false;
		for (G graph : graphs) {
			boolean doUpdate = (isActive || graph.hasHistory());
			if (!doUpdate)
				continue;
			Object[] graphData = graph.getData();
			if (graphData == null || graph.getColorMap() == null)
				continue;
			if (isFitness)
				// cast should be safe for fitness data
				model.getFitnessData(graph.getModule().getId(), graph.getData(),
						(ColorMap.Gradient1D<T>) graph.getColorMap());
			else
				model.getTraitData(graph.getModule().getId(), graph.getData(), graph.getColorMap());
			graph.update(isNext);
			updated = true;
		}
		if (updated)
			timestamp = newtime;
	}

	@Override
	public void update(boolean force) {
		if (model.isIBS() || model.isPDE()) {
			if (!isActive)
				return;
			for (G graph : graphs)
				graph.paint(force);
			return;
		}
		for (G graph : graphs)
			graph.displayMessage("No view available for " + model.getType().getKey() + " model.");
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
	public boolean onKeyDown(String key) {
		if ("s".equals(key)) {
			for (G graph : graphs)
				graph.getNetwork().shake(graph, 0.05);
			return true;
		}
		return super.onKeyDown(key);
	}

	@Override
	public void mouseHitNode(int id, int node, boolean alt) {
		if (model.isIBS())
			((IBS) model).mouseHitNode(id, node, alt);
	}

	@Override
	public String getTooltipAt(AbstractGraph<?> agraph, int node) {
		if (!model.isIBS() && !model.isPDE())
			return null;
		@SuppressWarnings("unchecked")
		G graph = (G) agraph;
		int nNodes = graph.getGeometry().getSize();
		Module<?> module = graph.getModule();
		StringBuilder tip = new StringBuilder(TABLE_STYLE);
		if (module.getNSpecies() > 1)
			tip.append(tableRow("Species", module.getName()));
		if (node >= nNodes)
			// linear geometries with history
			appendLinearTip(node, nNodes, module, graph, tip);
		else if (model.isIBS())
			appendIBSTip(node, module, graph, tip);
		else
			appendPDETip(node, module, graph, tip);
		return tip.append(TABLE_END).toString();
	}

	/**
	 * Append the tooltip of {@code node} for IBS models.
	 * 
	 * @param node   the node index
	 * @param module the module owning the graph
	 * @param graph  the graph providing geometry and color mapping
	 * @param tip    the builder for the tooltip
	 */
	private void appendIBSTip(int node, Module<?> module, G graph, StringBuilder tip) {
		int id = module.getId();
		tip.append(tableRow("Node", node));
		appendCell(tip, "Trait", isFitness ? null : graph.getCSSColorAt(node))
				.append(model.getTraitNameAt(id, node)).append(TABLE_ROW_END);
		String tag = ((IBS) model).getTagNameAt(id, node);
		if (tag != null)
			tip.append(tableRow("Tag", tag));
		if (module instanceof Payoffs) {
			// with payoff-to-fitness maps report payoffs as well
			if (!module.getMap2Fitness().isMap(Map2Fitness.Map.NONE))
				tip.append(tableRow("Payoff", model.getScoreNameAt(id, node)));
			appendCell(tip, "Fitness", isFitness ? graph.getCSSColorAt(node) : null)
					.append(model.getFitnessNameAt(id, node)).append(TABLE_ROW_END);
			int count = ((IBS) model).getInteractionsAt(id, node);
			if (count >= 0)
				tip.append(tableRow("Interactions", count == Integer.MAX_VALUE ? "all" : count));
		}
		// in multi-species modules this points to the other species
		AbstractGeometry intergeom = module.getIBSPopulation().getInteractionGeometry();
		appendNeighbors(node, intergeom, "Neighbours", "Links to", "Link here", tip);
		if (intergeom.isInterspecies())
			// in inter-species interactions, show traits of other species
			appendOppTraitsAt(node, intergeom, findOppGraph(graph, false), tip);
		if (!intergeom.isSingle()) {
			AbstractGeometry compgeom = module.getIBSPopulation().getCompetitionGeometry();
			appendNeighbors(node, compgeom, "Competitors", "Competes for", "Compete here", tip);
			appendOppTraitsAt(node, compgeom, findOppGraph(graph, true), tip);
		}
	}

	/**
	 * Append the tooltip of {@code node} for PDE models.
	 * 
	 * @param node   the node index
	 * @param module the module
	 * @param graph  the graph
	 * @param tip    the builder for the tooltip
	 */
	private void appendPDETip(int node, Module<?> module, G graph, StringBuilder tip) {
		int id = module.getId();
		tip.append(tableRow("Node", node));
		String[] names = module.getTraitNames();
		Color[] colors = module.getTraitColors();
		tip.append(TABLE_ROW_START).append("Traits").append(TABLE_CELL_NEXT);
		for (int n = 0; n < names.length; n++)
			tip.append(n > 0 ? ", " : "").append(SPAN_COLOR).append(ColorMapCSS.Color2Css(colors[n]))
					.append(TABLE_CELL_BULLET).append(names[n]);
		tip.append(TABLE_ROW_END);
		appendCell(tip, "Densities", isFitness ? null : graph.getCSSColorAt(node))
				.append(model.getTraitNameAt(id, node)).append(TABLE_ROW_END);
		if (module instanceof Payoffs) {
			int vac = module.getVacantIdx();
			double[] fitness = model.getMeanFitnessAt(id, node);
			Map2Fitness map = module.getMap2Fitness();
			// with payoff-to-fitness maps report payoffs as well
			if (!map.isMap(Map2Fitness.Map.NONE))
				appendValues(tip.append(TABLE_ROW_START).append("Payoff").append(TABLE_CELL_NEXT), fitness, vac,
						map).append(TABLE_ROW_END);
			appendValues(appendCell(tip, "Fitness", isFitness ? graph.getCSSColorAt(node) : null), fitness, vac,
					null).append(" → ")
					.append(Formatter.pretty(ArrayMath.dot(model.getMeanTraitAt(id, node), fitness), 3))
					.append(TABLE_ROW_END);
		}
		appendNeighbors(node, graph.getGeometry(), "Connections", "Links for", "Link here", tip);
	}

	/**
	 * Append tooltip information for linear geometries with history, where the
	 * nodes of earlier time slices follow the nodes of the current one.
	 * 
	 * @param node   the node index
	 * @param nNodes the number of nodes per time slice
	 * @param module the module that provides the tooltip data
	 * @param graph  the graph that provides view-specific history data
	 * @param tip    the builder for the tooltip
	 */
	private void appendLinearTip(int node, int nNodes, Module<?> module, G graph, StringBuilder tip) {
		int idx = node / nNodes;
		node %= nNodes;
		tip.append(tableRow("Node", node)).append(tableRow("Time", Formatter.format(-idx * model.getTimeStep(), 2)));
		if (!(graph instanceof PopGraph1D))
			return;
		PopGraph1D graph1d = (PopGraph1D) graph;
		String color = graph1d.getHistoryColorAt(idx, node);
		if (color == null)
			return;
		if (isFitness) {
			if (graph1d.isVacantColor(color)) {
				tip.append(tableRow("Fitness", "-"));
				return;
			}
			double value = graph1d.decodeColorValue(color);
			if (Double.isFinite(value))
				appendCell(tip, "Fitness", color).append(Formatter.pretty(value, 2)).append(TABLE_ROW_END);
			return;
		}
		int trait = graph1d.decodeColorIndex(color);
		double value = graph1d.decodeColorValue(color);
		if (trait >= 0 || Double.isFinite(value))
			appendCell(tip, "Trait", color).append(trait >= 0 ? module.getTraitName(trait) : Formatter.pretty(value, 2))
					.append(TABLE_ROW_END);
	}

	/**
	 * Append the start of a table row with {@code label}, followed by a bullet of
	 * {@code color}, unless {@code null}. The caller appends the value and ends the
	 * row.
	 *
	 * @param tip   the builder for the tooltip
	 * @param label the label of the row
	 * @param color the color of the bullet or {@code null}
	 * @return the builder for the tooltip
	 */
	private static StringBuilder appendCell(StringBuilder tip, String label, String color) {
		tip.append(TABLE_ROW_START).append(label).append(TABLE_CELL_NEXT);
		if (color != null)
			tip.append(SPAN_COLOR).append(color).append(TABLE_CELL_BULLET);
		return tip;
	}

	/**
	 * Append a comma separated list of values, or '-' for vacant sites. If
	 * {@code map} is not {@code null}, values are converted into payoffs.
	 * 
	 * @param tip    the builder for the tooltip
	 * @param values the array of values
	 * @param vac    the index of vacant sites
	 * @param map    the map of payoffs to fitness or {@code null}
	 * @return the builder for the tooltip
	 */
	private static StringBuilder appendValues(StringBuilder tip, double[] values, int vac, Map2Fitness map) {
		for (int i = 0; i < values.length; i++) {
			if (i > 0)
				tip.append(", ");
			if (i == vac)
				tip.append("-");
			else
				tip.append(Formatter.formatFix(map == null ? values[i] : map.invmap(values[i]), 3));
		}
		return tip;
	}

	/**
	 * Find the graph of the opponent of the module of {@code graph} that shows its
	 * interaction or competition geometry.
	 * 
	 * @param graph       the graph for which to find the opponent's graph
	 * @param competition {@code true} for the competition geometry
	 * @return the graph of the opponent or {@code null} if none
	 */
	private G findOppGraph(G graph, boolean competition) {
		Module<?> opponent = graph.getModule().getOpponent();
		IBSPopulation<?, ?> opppop = opponent.getIBSPopulation();
		AbstractGeometry oppGeom = competition ? opppop.getCompetitionGeometry() : opppop.getInteractionGeometry();
		for (G oppGraph : graphs) {
			// XXX this should work but somehow the pointers are different even though the
			// objects appear to be the same...
			// if (oppModule == opponent && oppGraph.getGeometry() == oppGeom)
			if (oppGraph != graph && oppGraph.getModule() == opponent
					&& oppGraph.getGeometry().getName().equals(oppGeom.getName()))
				return oppGraph;
		}
		return null;
	}

	/**
	 * Append the traits of the neighbours of {@code node} in {@code geom}, as
	 * shown in the opponent's {@code graph}.
	 * 
	 * @param node  the node index
	 * @param geom  the geometry
	 * @param graph the opponent's graph or {@code null}
	 * @param tip   the builder for the tooltip
	 */
	private static void appendOppTraitsAt(int node, AbstractGeometry geom, GenericPopGraph<?, ?> graph,
			StringBuilder tip) {
		if (graph == null)
			return;
		tip.append(TABLE_ROW_START).append(graph.getGeometry().getName()).append(TABLE_CELL_NEXT).append("[");
		int nNeighs = geom.kout[node];
		if (nNeighs == 0)
			tip.append(" - ");
		int disp = Math.min(nNeighs, 10);
		for (int n = 0; n < disp; n++)
			tip.append(SPAN_COLOR).append(graph.getCSSColorAt(geom.out[node][n])).append(TABLE_CELL_BULLET);
		if (disp < nNeighs)
			tip.append(" ...");
		tip.append("]").append(TABLE_ROW_END);
	}

	/**
	 * Append the neighbours of {@code node} in {@code geom}. Undirected geometries
	 * list the neighbours, or 'all' in well-mixed populations. Directed geometries
	 * list outgoing and incoming links separately, which is useful for debugging
	 * geometries, see {@code AbstractGeometry#checkConnections()}.
	 * 
	 * @param node       the node index
	 * @param geom       the geometry
	 * @param undirected the label for undirected geometries
	 * @param out        the label for outgoing links
	 * @param in         the label for incoming links
	 * @param tip        the builder for the tooltip
	 */
	private static void appendNeighbors(int node, AbstractGeometry geom, String undirected, String out, String in,
			StringBuilder tip) {
		if (!geom.isUndirected()) {
			appendLinks(out, geom.out[node], geom.kout[node], tip);
			appendLinks(in, geom.in[node], geom.kin[node], tip);
		} else if (geom.getType().equals(GeometryType.WELLMIXED))
			tip.append(tableRow(undirected, "all"));
		else
			appendLinks(undirected, geom.out[node], geom.kout[node], tip);
	}

	/**
	 * Append the number and (the first few) indices of {@code k} links.
	 * 
	 * @param label the label of the links
	 * @param links the indices of the linked nodes
	 * @param k     the number of links
	 * @param tip   the builder for the tooltip
	 */
	private static void appendLinks(String label, int[] links, int k, StringBuilder tip) {
		tip.append(TABLE_ROW_START).append(label).append(TABLE_CELL_NEXT);
		if (k == 0)
			tip.append("none");
		else {
			tip.append(k).append(" [").append(links[0]);
			int disp = Math.min(k, 10);
			for (int n = 1; n < disp; n++)
				tip.append(" ").append(links[n]);
			tip.append(disp < k ? " ...]" : "]");
		}
		tip.append(TABLE_ROW_END);
	}
}
