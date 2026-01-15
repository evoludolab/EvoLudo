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

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

import org.evoludo.graphics.AbstractGraph;
import org.evoludo.graphics.AbstractGraph.HasTrajectory;
import org.evoludo.graphics.AbstractGraph.MyContext2d;
import org.evoludo.graphics.AbstractGraph.Shifter;
import org.evoludo.graphics.AbstractGraph.Zoomer;
import org.evoludo.graphics.GenericPopGraph;
import org.evoludo.simulator.EvoLudo;
import org.evoludo.simulator.EvoLudoGWT;
import org.evoludo.simulator.Resources;
import org.evoludo.simulator.models.ChangeListener;
import org.evoludo.simulator.models.Data;
import org.evoludo.simulator.models.LifecycleListener;
import org.evoludo.simulator.models.Mode;
import org.evoludo.simulator.models.Model;
import org.evoludo.simulator.models.RunListener;
import org.evoludo.simulator.models.SampleListener;
import org.evoludo.ui.ContextMenu;
import org.evoludo.ui.ContextMenuItem;
import org.evoludo.util.Formatter;
import org.evoludo.util.NativeJS;
import org.evoludo.util.RingBuffer;

import com.google.gwt.canvas.client.Canvas;
import com.google.gwt.core.client.Duration;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.ScriptInjector;
import com.google.gwt.dom.client.Style.Position;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.ComplexPanel;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.ProvidesResize;
import com.google.gwt.user.client.ui.RequiresResize;

/**
 * The parent class of all panels that provide graphical representations the
 * state of the current EvoLudo model.
 *
 * @param <G> the concrete graph type rendered inside the view
 *
 * @author Christoph Hauert
 */
public abstract class AbstractView<G extends AbstractGraph<?>> extends Composite
		implements RequiresResize, ProvidesResize,
		LifecycleListener, RunListener, SampleListener, ChangeListener {

	/**
	 * The reference to the EvoLudo engine that manages the simulation.
	 */
	protected EvoLudoGWT engine;

	/**
	 * The reference to the model that supplies the data for this graph.
	 */
	Model model;

	/**
	 * The type of data shown in this graph.
	 */
	Data type;

	/**
	 * Logger for keeping track of and reporting events and issues.
	 */
	protected Logger logger;

	/**
	 * The list of graphs that are displayed in this view.
	 */
	protected final List<G> graphs;

	/**
	 * The number of rows of graphs in this view.
	 */
	int gRows = 1;

	/**
	 * The number of columns of graphs in this view.
	 */
	int gCols = 1;

	/**
	 * The flag to indicate whether this view is currently active.
	 */
	boolean isActive = false;

	/**
	 * The flag to indicate whether this view has been loaded.
	 */
	boolean isLoaded = false;

	/**
	 * The timestamp of model at the last update of this view.
	 */
	protected double timestamp;

	/**
	 * Time of last GUI update
	 */
	double updatetime = -1.0;

	/**
	 * The GWT widget that contains the graphical representations of the data.
	 */
	ComplexPanel wrapper;

	/**
	 * The constructor for the abstract view.
	 * 
	 * @evoludo.note A {@code LayoutPanel} instead of a {@code FlowPanel} would be a
	 *               nice way to continue the onResize cascade but incompatible with
	 *               current implementation of context menu and tooltips
	 * 
	 * @param engine the EvoLudo engine
	 * @param type   the type of data shown in this view
	 */
	protected AbstractView(EvoLudoGWT engine, Data type) {
		this.engine = engine;
		this.type = type;
		logger = engine.getLogger();
		this.graphs = new ArrayList<>();
		wrapper = new FlowPanel();
		wrapper.getElement().getStyle().setPosition(Position.RELATIVE);
		initWidget(wrapper);
	}

	/**
	 * Get the name of this view. This is used to dynamically build the view
	 * selector.
	 * 
	 * @return the name of this view
	 */
	public abstract String getName();

	/**
	 * Load the view. This is called for modules and models that implement this
	 * view. This is called early on when initializing the view and is independent
	 * of the activation of the view.
	 * 
	 * @return {@code true} if view had not been loaded
	 * 
	 * @see #allocateGraphs()
	 */
	public boolean load() {
		if (isLoaded)
			return false;
		engine.addLifecycleListener(this);
		engine.addRunListener(this);
		engine.addChangeListener(this);
		gRows = 1;
		gCols = 1;
		model = engine.getModel();
		allocateGraphs();
		isLoaded = true;
		options = null;
		return true;
	}

	@Override
	public void moduleUnloaded() {
		unload();
	}

	@Override
	public void modelLoaded() {
		model = engine.getModel();
	}

	@Override
	public void modelUnloaded() {
		unload();
	}

	/**
	 * Unload the view. This is called when changing the module or model that
	 * implement this view. This is independent of the activation of the view.
	 */
	public void unload() {
		if (!isLoaded)
			return;
		engine.removeLifecycleListener(this);
		engine.removeRunListener(this);
		engine.removeChangeListener(this);
		destroyGraphs();
		isActive = false;
		model = null;
		isLoaded = false;
	}

	@Override
	protected void onUnload() {
		unload();
	}

	/**
	 * The string with view specific options.
	 * 
	 * @see org.evoludo.simulator.ui.ViewController#getCloView()
	 */
	String options;

	/**
	 * Set the options string for this view.
	 * 
	 * @param options the options string
	 */
	public void setOptions(String options) {
		this.options = options;
	}

	/**
	 * Parse the arguments provided to this view. The default implementation simply
	 * passes the currently configured option string to all graphs.
	 * 
	 * @return {@code true} if the arguments were successfully parsed
	 */
	public boolean parse() {
		if (options == null)
			return true;
		boolean ok = true;
		for (G graph : graphs)
			ok &= graph.parse(options);
		return ok;
	}

	/**
	 * Allocate all graphs managed by this view. This is called when loading the
	 * view. Once all views are attached to the browser DOM a call to the graph's
	 * {@code calcBounds(int, int)} is triggered through {@code setBounds(int, int)}
	 * to properly calculate the layout.
	 * 
	 * @return {@code true} if graphs were (re)allocated
	 * 
	 * @see #load()
	 * @see #setBounds(int, int)
	 */
	protected abstract boolean allocateGraphs();

	/**
	 * Destroy all graphs in this view and free up resources.
	 */
	protected void destroyGraphs() {
		for (G graph : graphs) {
			graph.deactivate();
			graph.removeFromParent();
		}
		graphs.clear();
		gRows = gCols = 1;
	}

	/**
	 * Get the type of data visualized on the graph.
	 *
	 * @return the data type
	 */
	public Data getType() {
		return type;
	}

	/**
	 * Get the type of the model supplying the data visualized on the graph.
	 *
	 * @return the model type
	 */
	public Model getModel() {
		return model;
	}

	/**
	 * Get the logger for returning progress, problems and messages to user.
	 *
	 * @return the logger for messages
	 */
	public Logger getLogger() {
		return logger;
	}

	/**
	 * Activate the view. This is called when the view is selected in the view
	 * selector.
	 */
	public void activate() {
		if (isActive)
			return;
		isActive = true;
		for (G graph : graphs)
			graph.activate();
		if (!model.requestMode(getMode())) {
			// this is should not happen because view should not be available
			// if mode is not supported, see EvoLudoWeb#updateViews()
			for (G graph : graphs)
				graph.displayMessage("Mode '" + getMode() + "'' not supported");
		}
		update(true);
		layoutComplete();
	}

	/**
	 * Deactivate the view. This is called when another view is selected in the view
	 * selector.
	 */
	public void deactivate() {
		isActive = false;
		for (G graph : graphs)
			graph.deactivate();
	}

	/**
	 * Get the mode required by this view.
	 * 
	 * @return the mode required by this view
	 */
	Mode getMode() {
		return Mode.DYNAMICS;
	}

	/**
	 * Check if the view has finished layouting its graphs. Currently only
	 * {@code GenericPop} and its graphs require layouting.
	 * 
	 * @return {@code true} if the view has layout
	 * 
	 * @see GenericPop
	 * @see GenericPopGraph
	 */
	public boolean hasLayout() {
		return true;
	}

	/**
	 * Notification of the completion of the layouting process.
	 */
	public void layoutComplete() {
		// some views (currently only networks) may require more involved layouting and
		// report completion once done.
		if (!hasLayout())
			return;
		engine.layoutComplete();
	}

	/**
	 * Get the status of this view. Views that aggregate data may want to provide
	 * custom status information. HTML formatting is acceptable.
	 * 
	 * @return the status of this view
	 */
	public String getStatus() {
		return getStatus(false);
	}

	/**
	 * Get the status of this view. Views that aggregate data may want to provide
	 * custom status information. HTML formatting is acceptable. Some status updates
	 * may be expensive to compute and views may decide to ignore the
	 * {@code getStatus} request, except if {@code force} is {@code true}.
	 * 
	 * @param force whether to force an update of the status
	 * @return the status of this view
	 */
	public String getStatus(boolean force) {
		return null;
	}

	@Override
	public void moduleRestored() {
		timestamp = -Double.MAX_VALUE;
		updatetime = -1.0;
		for (G graph : graphs)
			graph.reset();
	}

	@Override
	public void modelDidReset() {
		reset(true);
		parse();
	}

	/**
	 * Called when a module has been reset. All graphs of the view are reset and
	 * updated if needed, unless {@code hard} is {@code false}.
	 * 
	 * @param hard the flag to indicate whether to do a hard reset
	 */
	public void reset(boolean hard) {
		timestamp = -Double.MAX_VALUE;
		updatetime = -1.0;
		if (allocateGraphs()) {
			int with = getOffsetWidth();
			int height = getOffsetHeight();
			if (with > 0 && height > 0)
				setBounds(with, height);
		}
	}

	@Override
	public void modelDidInit() {
		timestamp = -Double.MAX_VALUE;
		updatetime = -1.0;
	}

	@Override
	public void modelStopped() {
		update(true);
	}

	/**
	 * In order to conserve computational resources the minimum time between
	 * subsequent GUI updates has to be at least
	 * <code>MIN_MSEC_BETWEEN_UPDATES</code> milliseconds. If update request are
	 * made more frequently some are request are not honoured and simply dropped.
	 */
	protected static final int MIN_MSEC_BETWEEN_UPDATES = 50; // max 20 updates per second

	@Override
	public void modelChanged(PendingAction action) {
		if (action == PendingAction.NONE) {
			double now = Duration.currentTimeMillis();
			boolean update = (now - updatetime > MIN_MSEC_BETWEEN_UPDATES);
			if (update)
				updatetime = now;
			update(update);
		}
	}

	/**
	 * Called when the view needs updating. This gets called when the selected view
	 * changed or new data is available from the model.
	 * 
	 * @see org.evoludo.simulator.models.ChangeListener#modelChanged(org.evoludo.simulator.models.ChangeListener.PendingAction)
	 *      ChangeListener#modelChanged(PendingAction)
	 */
	public void update() {
		update(false);
	}

	/**
	 * Called when the view needs updating. This gets called when the selected view
	 * changed or new data is available from the model. Views may ignore updating
	 * requests unless {@code force} is {@code true}.
	 * 
	 * @param force {@code true} to force the update
	 */
	public abstract void update(boolean force);

	/**
	 * Checks if the controller is busy running calculations.
	 *
	 * @return {@code true} if calculations are running
	 */
	public boolean isActive() {
		return isActive;
	}

	/**
	 * Checks if the controller is busy running calculations.
	 *
	 * @return {@code true} if calculations are running
	 */
	public boolean isRunning() {
		return engine.isRunning();
	}

	/**
	 * Notifies the controller that the user requested setting a new initial
	 * configuration {@code init} (optional implementation).
	 *
	 * @param init the new initial configuration
	 * @return {@code true} if the request was honoured
	 */
	public boolean setInitialState(double[] init) {
		return false;
	}

	/**
	 * Notifies the controller that the mouse/tap has hit node with index
	 * {@code node} on the graph with the tag {@code id}.
	 * 
	 * @param id   the id of the graph
	 * @param node the index of the node that was hit
	 */
	public void mouseHitNode(int id, int node) {
		mouseHitNode(id, node, false);
	}

	/**
	 * Notifies the controller that the mouse/tap has hit node with index
	 * {@code node} on the graph with the tag {@code id}. The flag {@code alt}
	 * indicates whether the {@code alt}-modifier was pressed (optional
	 * implementation).
	 * 
	 * @param id   the id of the graph
	 * @param node the index of the node that was hit
	 * @param alt  {@code true} if the {@code alt}-key was pressed
	 */
	public void mouseHitNode(int id, int node, boolean alt) {
	}

	/**
	 * Opportunity for the controller to add functionality to the context menu
	 * (optional implementation). Additional entries should be added to
	 * {@code menu}. If the context menu was opened while the mouse was over a node
	 * its index is {@code node}. At this point the menu already contains entries
	 * that are relevant for all graphs, e.g. fullscreen and export. Override this
	 * method to add further, more specialized entries. Finally, the current pane
	 * will be asked whether it wants to add further entries (e.g. autoscale axes)
	 * (optional implementation).
	 *
	 * @param menu the context menu
	 * @param node the index of node
	 * 
	 * @see AbstractView#populateContextMenu(ContextMenu)
	 */
	public void populateContextMenuAt(ContextMenu menu, int node) {
	}

	/**
	 * Default implementation for synchronized shifting of multiple graphs.
	 * 
	 * @param dx the shift in x-direction
	 * @param dy the shift in y-direction
	 * 
	 * @see AbstractGraph.Shifter#shift(int, int)
	 */
	public void shift(int dx, int dy) {
		for (G graph : graphs) {
			if (graph instanceof Shifter)
				((Shifter) graph).shift(dx, dy);
		}
	}

	/**
	 * Default implementation for synchronized zooming of multiple graphs. The
	 * center for zooming is given by the coordinates {@code (x,y)}.
	 * 
	 * @param zoom the zoom factor
	 * @param x    the x-coordinate
	 * @param y    the y-coordinate
	 * 
	 * @see AbstractGraph.Zoomer#zoom(double, int, int)
	 */
	public void zoom(double zoom, int x, int y) {
		for (G graph : graphs) {
			if (graph instanceof Zoomer)
				((Zoomer) graph).zoom(zoom, x, y);
		}
	}

	/**
	 * Opportunity for view to implement keyboard shortcut for actions (repeating).
	 * If the key remains pressed this event is triggered repeatedly.
	 * 
	 * @param key the code of the pressed key
	 * @return {@code true} if the key was handled
	 * 
	 * @see org.evoludo.simulator.ui.KeyHandler#onKeyDown(String)
	 */
	public boolean onKeyDown(String key) {
		return false;
	}

	/**
	 * Opportunity for view to implement keyboard shortcut for actions (non
	 * repeating). For example to clear the display or export graphics.
	 * <p>
	 * List of shortcuts provided by all views for the following keys:
	 * <dl>
	 * <dt>{@code S}</dt>
	 * <dd>Export snapshot of view in the Scalable Vecorized Graphics format,
	 * {@code svg} (if available).</dd>
	 * <dt>{@code P}</dt>
	 * <dd>Export snapshot of view as a bitmap in the Portable Network Graphic
	 * format, {@code png} (if available).</dd>
	 * <dt>{@code C}</dt>
	 * <dd>Export data of model as in the Comma Separated Values format, {@code csv}
	 * (if available). For example, this can be used to export the raw statistical
	 * data obtained from fixation probability calculations.</dd>
	 * <dt>{@code F}</dt>
	 * <dd>Toggle full screen mode of data view without controls (if
	 * available).</dd>
	 * </dl>
	 * 
	 * @param key the code of the released key
	 * @return {@code true} if the key was handled
	 */
	public boolean onKeyUp(String key) {
		switch (key) {
			case "S":
				// save svg snapshot (if supported)
				if (!hasExportType(ExportType.SVG))
					return false;
				exportSVG();
				break;
			case "P":
				// save png snapshot (if supported)
				if (!hasExportType(ExportType.PNG))
					return false;
				exportPNG();
				break;
			case "C":
				// export csv data (if supported)
				if (!hasExportType(ExportType.CSV_STAT))
					return false;
				exportStatData();
				break;
			default:
				return false;
		}
		return true;
	}

	/**
	 * Checks if the view supports the export type {@code type}.
	 * 
	 * @param type the export type to check
	 * @return {@code true} if the view supports the export type
	 */
	private boolean hasExportType(ExportType type) {
		for (ExportType e : exportTypes())
			if (e == type)
				return true;
		return false;
	}

	@Override
	public void onResize() {
		if (getOffsetWidth() == 0 || getOffsetHeight() == 0)
			return;
		for (G graph : graphs)
			graph.onResize();
		engine.guiReady();
		if (isActive)
			scheduleUpdate(true);
	}

	/**
	 * Set the bounds of the view to the given {@code width} and {@code height}.
	 * This is called when loading the views or changing the size of the GUI.
	 * 
	 * @param width  the width of the view
	 * @param height the height of the view
	 */
	public void setBounds(int width, int height) {
		for (G graph : graphs)
			graph.calcBounds(width, height);
	}

	/**
	 * The flag to indicate whether an update is already scheduled. Subsequent
	 * requests are ignored.
	 */
	private boolean updateScheduled = false;

	/**
	 * Schedule an update of the view. If an update is already scheduled subsequent
	 * requests are ignored.
	 * 
	 * @param force {@code true} to force the update
	 * 
	 * @see #update(boolean)
	 */
	protected void scheduleUpdate(boolean force) {
		if (updateScheduled)
			return;
		Scheduler.get().scheduleDeferred(() -> {
			// deferred updates can cause issues if in the mean time the model
			// has changed and this view is no longer supported. if this is the
			// case destroyGraphs has been called and graphs is empty.
			try {
				// catch errors to prevent the views from crashing GUI
				if (!graphs.isEmpty())
					update(force);
			} catch (Exception e) {
				logger.severe("Error updating view: " + e.getMessage());
			}
			updateScheduled = false;
		});
		updateScheduled = true;
	}

	/**
	 * Get the graph at the coordinates {@code (x,y)}.
	 * 
	 * @param x the x-coordinate
	 * @param y the y-coordinate
	 * @return the graph at the coordinates {@code (x,y)}
	 */
	public G getGraphAt(int x, int y) {
		for (G graph : graphs)
			if (graph.contains(x, y))
				return graph;
		return null;
	}

	/**
	 * The field to store the restore context menu.
	 */
	protected ContextMenuItem restoreMenu;

	/**
	 * The field to store the export submenu trigger.
	 */
	protected ContextMenuItem exportSubmenuTrigger;

	/**
	 * The field to store the export context submenu.
	 */
	protected ContextMenu exportSubmenu;

	/**
	 * Opportunity for the controller to add functionality to the context menu
	 * (optional implementation).
	 *
	 * @param contextMenu the context menu
	 */
	public void populateContextMenu(ContextMenu contextMenu) {
		// models may also like to add entries to context menu
		// IMPORTANT: cannot query model directly due to interference with java
		// simulations. all GUI related methods must be quarantined through EvoLudoGWT
		engine.populateContextMenu(contextMenu);

		// process exports context menu (suppress in ePub, regardless of whether a
		// standalone lab or not)
		if (!NativeJS.isEPub()) {
			exportSubmenu = new ContextMenu(contextMenu);
			exportSubmenu.add(new ContextMenuItem(ExportType.STATE.toString(), new ExportCommand(ExportType.STATE)));
			for (ExportType e : exportTypes()) {
				if (e == ExportType.STATE)
					continue; // always included
				exportSubmenu.add(new ContextMenuItem(e.toString(), new ExportCommand(e)));
			}
			contextMenu.addSeparator();
			exportSubmenuTrigger = contextMenu.add("Export...", exportSubmenu);
			if (restoreMenu == null)
				restoreMenu = new ContextMenuItem("Restore...", () -> engine.restoreFromFile());
		}
		boolean idle = !engine.isRunning();
		if (restoreMenu != null) {
			restoreMenu.setEnabled(idle);
			contextMenu.add(restoreMenu);
		}
		if (exportSubmenuTrigger != null)
			exportSubmenuTrigger.setEnabled(idle);
	}

	/**
	 * The available export data types:
	 * <dl>
	 * <dt>SVG</dt>
	 * <dd>scalable vector graphics format, {@code svg}</dd>
	 * <dt>PDF</dt>
	 * <dd>portable document format, {@code pdf} (not yet implemented).</dd>
	 * <dt>EPS</dt>
	 * <dd>encapsulated postscript format, {@code eps} (not yet implemented).</dd>
	 * <dt>PNG</dt>
	 * <dd>portable network graphics format, {@code png}</dd>
	 * <dt>STAT_DATA</dt>
	 * <dd>statistics data as comma separated list, {@code csv}</dd>
	 * <dt>TRAJ_DATA</dt>
	 * <dd>trajectory data as comma separated list, {@code csv} (not yet
	 * implemented).</dd>
	 * <dt>STATE</dt>
	 * <dd>current state of simulation, {@code plist}</dd>
	 * </dl>
	 */
	public enum ExportType {
		/**
		 * Scalable vector graphics format, {@code svg}
		 */
		SVG("Vector graphics (svg)"),

		/**
		 * Portable document format, {@code pdf} (not yet implemented).
		 */
		PDF("Vector graphics (pdf)"),

		/**
		 * Encapsulated postscript format, {@code eps} (not yet implemented).
		 */
		EPS("Vector graphics (eps)"),

		/**
		 * Portable network graphics format, {@code png}
		 */
		PNG("Bitmap graphics (png)"),

		/**
		 * Statistics data as a comma separated list, {@code csv}
		 */
		CSV_STAT("Statistics (csv)"),

		/**
		 * Trajectory data as a comma separated list, {@code csv} (not yet implemented).
		 */
		CSV_TRAJ("Trajectory (csv)"),

		/**
		 * Mean state data as a comma separated list, {@code csv} (not yet implemented).
		 */
		CSV_MEAN("Mean state (csv)"),

		/**
		 * Current state of simulation, {@code plist}
		 * 
		 * @see EvoLudo#exportState()
		 */
		STATE("State (plist)");

		/**
		 * The title of the export type.
		 */
		String title;

		/**
		 * The constructor for the export type.
		 * 
		 * @param title the title of the export type
		 */
		ExportType(String title) {
			this.title = title;
		}

		@Override
		public String toString() {
			return title;
		}
	}

	/**
	 * Return the list of export types that are acceptable for _all_ graphs in this
	 * view.
	 *
	 * @return the list of viable export types
	 */
	protected ExportType[] exportTypes() {
		return new ExportType[0];
	}

	/**
	 * Export the data of the view.
	 * 
	 * @param type the type of data to export
	 */
	protected void export(ExportType type) {
		switch (type) {
			case SVG:
				exportSVG();
				break;
			case PNG:
				exportPNG();
				break;
			case CSV_STAT:
				exportStatData();
				break;
			case CSV_MEAN:
				exportMeanData();
				break;
			case CSV_TRAJ:
				exportTrajData();
				break;
			case STATE:
				engine.exportState();
				break;
			default:
		}
	}

	/**
	 * The flag to indicate whether the script for exporting SVG has been injected.
	 */
	private static boolean hasSVGjs = false;

	/**
	 * Export the view as a PNG image.
	 */
	protected void exportPNG() {
		Canvas canvas = Canvas.createIfSupported();
		if (canvas == null)
			return;
		int scale = NativeJS.getDevicePixelRatio();
		canvas.setCoordinateSpaceWidth(getOffsetWidth() * scale);
		canvas.setCoordinateSpaceHeight(getOffsetHeight() * scale);
		MyContext2d ctx = canvas.getContext2d().cast();
		export(ctx, scale);
		NativeJS.export(canvas.getCanvasElement().toDataUrl("image/png").replaceFirst("^data:image/[^;]",
				"data:application/octet-stream"), "evoludo.png");
	}

	/**
	 * Export the view as a SVG image.
	 */
	protected void exportSVG() {
		if (!hasSVGjs) {
			// script (hopefully) needs to be injected only once
			ScriptInjector.fromString(Resources.INSTANCE.canvas2SVG().getText()).inject();
			hasSVGjs = true;
		}
		int scale = NativeJS.getDevicePixelRatio();
		MyContext2d ctx = NativeJS.createSVGContext(getOffsetWidth() * scale, getOffsetHeight() * scale);
		export(ctx, scale);
		NativeJS.exportSVG(ctx);
	}

	/**
	 * Export each graph in this view.
	 * 
	 * @param ctx   the graphical context of the canvas to export to
	 * @param scale the scaling for the export canvas
	 */
	protected void export(MyContext2d ctx, int scale) {
		int hOffset = 0;
		int vOffset = 0;
		int count = 0;
		for (G graph : graphs) {
			ctx.save();
			ctx.translate(hOffset, vOffset);
			graph.export(ctx);
			ctx.restore();
			if (++count % gCols == 0) {
				hOffset = 0;
				vOffset += scale * graph.getOffsetHeight();
			} else
				hOffset += scale * graph.getOffsetWidth();
		}
	}

	/**
	 * The header for data exports.
	 * 
	 * @return the header as a string builder
	 */
	protected StringBuilder exportDataHeader() {
		StringBuilder export = new StringBuilder("# data exported at " +
				DateTimeFormat.getFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "\n");
		export.append("# " + getName() + "\n");
		return export;
	}

	/**
	 * Export the statistics data.
	 * <p>
	 * <strong>Important:</strong> Must be overridden by subclasses that return
	 * {@link ExportType#CSV_STAT} among their export data types.
	 * 
	 * @see #exportTypes()
	 */
	protected void exportStatData() {
	}

	/**
	 * Export the mean data. By default this returns the buffer data as a comma
	 * separated list.
	 * 
	 * @see #exportTypes()
	 */
	@SuppressWarnings("unchecked")
	protected void exportMeanData() {
		StringBuilder export = exportDataHeader();
		int header = export.length();
		// focus on Mean for now
		RingBuffer<double[]> buffer = null;
		for (G graph : graphs) {
			RingBuffer<?> newbuffer = graph.getBuffer();
			if (newbuffer == null || newbuffer.isEmpty() ||
					newbuffer == buffer || !(newbuffer.first() instanceof double[]))
				continue;
			// cast is safe because of the instanceof check
			buffer = (RingBuffer<double[]>) newbuffer;
			String name = graph.getStyle().label;
			if (name != null)
				export.append("# ")
						.append(name)
						.append("\n");
			StringBuilder legend = new StringBuilder("time");
			for (int i = 0; i < buffer.getDepth() - 1; i++) {
				legend.append(", ")
						.append(model.getMeanName(i));
			}
			export.append("# ")
					.append(legend)
					.append("\n");
			Iterator<double[]> entry = buffer.ordered();
			while (entry.hasNext()) {
				export.append(Formatter.format(entry.next(), 8))
						.append("\n");
			}
		}
		if (export.length() == header)
			return;
		NativeJS.export("data:text/csv;base64," + NativeJS.b64encode(export.toString()), "evoludo_mean.csv");
	}

	/**
	 * Export the trajectories. By default this returns the buffer data as a comma
	 * separated list.
	 * 
	 * @see #exportTypes()
	 */
	protected void exportTrajData() {
		StringBuilder export = exportDataHeader();
		int header = export.length();
		for (G graph : graphs) {
			if (!(graph instanceof HasTrajectory))
				continue;
			((HasTrajectory) graph).exportTrajectory(export);
		}
		if (export.length() == header)
			return;
		NativeJS.export("data:text/csv;base64," + NativeJS.b64encode(export.toString()), "evoludo_traj.csv");
	}

	/**
	 * The export command triggered by the context menu entries.
	 */
	public class ExportCommand implements Command {

		/**
		 * The type of data to export.
		 */
		ExportType exportType;

		/**
		 * The constructor for the export command.
		 * 
		 * @param type the type of data to export
		 */
		public ExportCommand(ExportType type) {
			exportType = type;
		}

		@Override
		public void execute() {
			export(exportType);
		}
	}
}
