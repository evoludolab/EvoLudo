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
import org.evoludo.simulator.models.Data;
import org.evoludo.simulator.models.Mode;
import org.evoludo.simulator.models.Model;
import org.evoludo.ui.ContextMenu;
import org.evoludo.ui.ContextMenuItem;
import org.evoludo.util.Formatter;
import org.evoludo.util.NativeJS;
import org.evoludo.util.RingBuffer;

import com.google.gwt.canvas.client.Canvas;
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
 * @author Christoph Hauert
 */
public abstract class AbstractView extends Composite implements RequiresResize, ProvidesResize,
		AbstractGraph.Controller {

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
	protected List<? extends AbstractGraph<?>> graphs = new ArrayList<>();

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
	 * The timestamp of the last update of this view.
	 */
	protected double timestamp;

	/**
	 * The GWT widget that contains the graphical representations of the data.
	 */
	ComplexPanel wrapper;

	/**
	 * The constructor for the abstract view.
	 * 
	 * @param engine the EvoLudo engine
	 * @param type   the type of data shown in this view
	 */
	public AbstractView(EvoLudoGWT engine, Data type) {
		this.engine = engine;
		this.type = type;
		logger = engine.getLogger();
		createWidget();
		initWidget(wrapper);
	}

	/**
	 * Create the widget that will contain the graphical representations of the
	 * data.
	 * 
	 * @evoludo.note LayoutPanel would be a nice way to continue the onResize
	 *               cascade but incompatible with current implementation of context
	 *               menu and tooltips
	 */
	public void createWidget() {
		wrapper = new FlowPanel();
		wrapper.getElement().getStyle().setPosition(Position.RELATIVE);
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
	 * @see #allocateGraphs()
	 */
	public void load() {
		if (isLoaded)
			return;
		gRows = 1;
		gCols = 1;
		model = engine.getModel();
		allocateGraphs();
		isLoaded = true;
	}

	/**
	 * Unload the view. This is called when changing the module or model that
	 * implement this view. This is independent of the activation of the view.
	 */
	public void unload() {
		if (!isLoaded)
			return;
		destroyGraphs();
		isActive = false;
		model = null;
		isLoaded = false;
	}

	/**
	 * Allocate all graphs managed by this view. This is called when loading the
	 * view. Once all views are attached to the browser DOM a call to the graph's
	 * {@code calcBounds(int, int)} is triggered through {@code setBounds(int, int)}
	 * to properly calculate the layout.
	 * 
	 * @see #load()
	 * @see #setBounds(int, int)
	 */
	protected abstract void allocateGraphs();

	/**
	 * Destroy all graphs in this view and free up resources.
	 */
	protected void destroyGraphs() {
		for (AbstractGraph<?> graph : graphs)
			graph.removeFromParent();
		graphs.clear();
		gRows = gCols = 1;
	}

	@Override
	public Data getType() {
		return type;
	}

	@Override
	public Model getModel() {
		return model;
	}

	@Override
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
		for (AbstractGraph<?> graph : graphs)
			graph.activate();
		update(true);
		if (!setMode(getMode())) {
			// this is should not happen because view should not be available
			// if mode is not supported, see EvoLudoWeb#updateViews()
			for (AbstractGraph<?> graph : graphs)
				graph.displayMessage("Mode '" + getMode() + "'' not supported");
		}
		layoutComplete();
	}

	/**
	 * Deactivate the view. This is called when another view is selected in the view
	 * selector.
	 */
	public void deactivate() {
		isActive = false;
		for (AbstractGraph<?> graph : graphs)
			graph.deactivate();
	}

	/**
	 * Clear the view.
	 */
	public void clear() {
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

	@Override
	public void layoutComplete() {
		// some views (currently only networks) may require more involved layouting and
		// report completion once done.
		if (!hasLayout())
			return;
		engine.layoutComplete();
	}

	/**
	 * Get the mode of this view. The graphical visualizations can request different
	 * modes for running the model. The default mode is {@link Mode#DYNAMICS} to
	 * generate a time series of the states of the model. Some views may digest data
	 * and, for example, show statistics such as fixation probabilities or times, in
	 * which case the mode {@link Mode#STATISTICS_SAMPLE} or
	 * {@link Mode#STATISTICS_UPDATE} should be requested.
	 * 
	 * @return the mode of this view
	 * 
	 * @see Mode
	 */
	public Mode getMode() {
		return Mode.DYNAMICS;
	}

	/**
	 * Set the mode of the model to {@code mode}. Does nothing if the model does not
	 * support the requested mode.
	 * 
	 * @param mode the mode to set
	 * @return {@code true} if the mode was successfully set
	 */
	public boolean setMode(Mode mode) {
		// if no module specified there is no model either
		if (model == null)
			return false;
		if (model.getMode() == mode)
			return true;
		return model.requestMode(mode);
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

	/**
	 * Called when a module has been restored.
	 * 
	 * @see org.evoludo.simulator.models.MilestoneListener#moduleRestored()
	 *      MilestoneListener.moduleRestored()
	 */
	public void restored() {
		timestamp = -Double.MAX_VALUE;
		for (AbstractGraph<?> graph : graphs)
			graph.reset();
	}

	/**
	 * Called when a module has been reset. All graphs are reset and updated if
	 * needed, unless {@code hard} is {@code true}.
	 * 
	 * @param hard the flag to indicate whether to do a hard reset
	 * 
	 * @see org.evoludo.simulator.models.MilestoneListener#modelDidReset()
	 *      MilestoneListener.modelDidReset()
	 */
	public void reset(boolean hard) {
		timestamp = -Double.MAX_VALUE;
	}

	/**
	 * Called when a module has been (re-)initialized.
	 * 
	 * @see org.evoludo.simulator.models.MilestoneListener#modelDidInit()
	 *      MilestoneListener.modelDidInit()
	 */
	public void init() {
		timestamp = -Double.MAX_VALUE;
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
	 * 
	 * @see org.evoludo.simulator.models.ChangeListener#modelChanged(org.evoludo.simulator.models.ChangeListener.PendingAction)
	 *      ChangeListener#modelChanged(PendingAction)
	 */
	public abstract void update(boolean force);

	@Override
	public boolean isRunning() {
		return engine.isRunning();
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
		for (AbstractGraph<?> graph : graphs) {
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
		for (AbstractGraph<?> graph : graphs) {
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
	 * @see org.evoludo.EvoLudoWeb#keyDownHandler(String)
	 */
	public boolean keyDownHandler(String key) {
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
	public boolean keyUpHandler(String key) {
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
				if (!hasExportType(ExportType.STAT_DATA))
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
		for (AbstractGraph<?> graph : graphs)
			graph.onResize();
		engine.guiReady();
		if (isActive)
			scheduleUpdate(true);
	}

	public void setBounds(int width, int height) {
		for (AbstractGraph<?> graph : graphs)
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
		Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
			@Override
			public void execute() {
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
			}
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
	public AbstractGraph<?> getGraphAt(int x, int y) {
		if (graphs == null)
			return null;
		for (AbstractGraph<?> graph : graphs)
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

	@Override
	public void populateContextMenu(ContextMenu contextMenu) {
		// models may also like to add entries to context menu
		// IMPORTANT: cannot query model directly due to interference with java
		// simulations. all GUI related methods must be quarantined through EvoLudoGWT
		engine.populateContextMenu(contextMenu);

		// process exports context menu (suppress in ePub, regardless of whether a
		// standalone lab or not)
		ExportType[] types = exportTypes();
		boolean isEPub = engine.isEPub;
		if (!isEPub && types != null && exportSubmenu == null) {
			int nTypes = types.length;
			exportSubmenu = new ContextMenu(contextMenu);
			// always include option to export current state
			exportSubmenu.add(new ContextMenuItem(ExportType.STATE.toString(), new ExportCommand(ExportType.STATE)));
			for (int n = 0; n < nTypes; n++) {
				ExportType t = types[n];
				if (t == ExportType.STATE)
					continue;
				exportSubmenu.add(new ContextMenuItem(t.toString(), new ExportCommand(t)));
			}
		}
		if (exportSubmenu != null) {
			contextMenu.addSeparator();
			exportSubmenuTrigger = contextMenu.add("Export...", exportSubmenu);
		}
		if (!isEPub && restoreMenu == null) {
			restoreMenu = new ContextMenuItem("Restore...", new Command() {
				@Override
				public void execute() {
					engine.restoreFromFile();
				}
			});
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
		STAT_DATA("Statistics (csv)"),

		/**
		 * Trajectory data as a comma separated list, {@code csv} (not yet implemented).
		 */
		TRAJ_DATA("Trajectory (csv)"),

		/**
		 * Mean state data as a comma separated list, {@code csv} (not yet implemented).
		 */
		MEAN_DATA("Mean state (csv)"),

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
		return null;
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
			case STAT_DATA:
				exportStatData();
				break;
			case MEAN_DATA:
				exportMeanData();
				break;
			case TRAJ_DATA:
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
		for (AbstractGraph<?> graph : graphs) {
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
	 * {@link ExportType#STAT_DATA} among their export data types.
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
		for (AbstractGraph<?> graph : graphs) {
			RingBuffer<?> newbuffer = graph.getBuffer();
			if (newbuffer == null || newbuffer.isEmpty() || newbuffer == buffer)
				continue;
			if (!(newbuffer.first() instanceof double[]))
				continue;
			// cast is safe because of the instanceof check
			buffer = (RingBuffer<double[]>) newbuffer;
			String name = graph.getStyle().label;
			if (name != null)
				export.append("# " + name + "\n");
			String legend = "time";
			for (int i = 0; i < buffer.getDepth() - 1; i++) {
				legend += ", " + model.getMeanName(i);
			}
			export.append("# " + legend + "\n");
			Iterator<double[]> entry = buffer.ordered();
			while (entry.hasNext()) {
				export.append(Formatter.format(entry.next(), 8) + "\n");
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
		for (AbstractGraph<?> graph : graphs) {
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
