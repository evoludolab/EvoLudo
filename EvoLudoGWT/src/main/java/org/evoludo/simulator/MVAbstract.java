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

import java.util.HashMap;
import java.util.Set;
import java.util.logging.Logger;

import org.evoludo.EvoLudoWeb;
import org.evoludo.graphics.AbstractGraph;
import org.evoludo.graphics.AbstractGraph.MyContext2d;
import org.evoludo.ui.ContextMenu;
import org.evoludo.ui.ContextMenuCheckBoxItem;
import org.evoludo.ui.ContextMenuItem;
import org.evoludo.ui.FullscreenChangeEvent;
import org.evoludo.ui.FullscreenChangeHandler;
import org.evoludo.ui.HasFullscreenChangeHandlers;
import org.evoludo.simulator.models.Model;
import org.evoludo.simulator.modules.Module;

import com.google.gwt.canvas.client.Canvas;
import com.google.gwt.canvas.dom.client.Context2d;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.ScriptInjector;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style.Position;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.ComplexPanel;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.ProvidesResize;

/**
 *
 * @author Christoph Hauert
 */
public abstract class MVAbstract extends Composite implements EvoLudoViews, ProvidesResize,
		AbstractGraph.Controller, HasFullscreenChangeHandlers {

	protected HandlerRegistration fullscreenHandler;

	protected EvoLudoGWT engine;

	/**
	 * Logger for keeping track of and reporting events and issues.
	 */
	protected Logger logger;
	protected Model.Data type;
	protected HashMap<AbstractGraph, Module> graphs2mods = new HashMap<AbstractGraph, Module>();
	protected Set<? extends AbstractGraph> graphs = graphs2mods.keySet();
	protected int gRows = 1, gCols = 1;

	protected boolean isActive = false;
	protected double timestamp;

	// widget elements
	ComplexPanel wrapper;

	public MVAbstract(EvoLudoGWT engine, Model.Data type) {
		this.engine = engine;
		this.type = type;
		logger = engine.getLogger();
		createWidget();
		initWidget(wrapper);
	}

	public void createWidget() {
//Note: LayoutPanel would be a nice way to continue the onResize cascade but incompatible with current implementation of context menu and tooltips
		wrapper = new FlowPanel();
		wrapper.getElement().getStyle().setPosition(Position.RELATIVE);
	}

	@Override
	public void load() {
		// set some defaults
		gRows = 1;
		gCols = 1;
	}

	@Override
	public void unload() {
		destroyGraphs();
		isActive = false;
	}

	protected void destroyGraphs() {
		for (AbstractGraph graph : graphs)
			graph.removeFromParent();
		graphs2mods.clear();
		gRows = gCols = 1;
	}

	@Override
	public Model.Data getType() {
		return type;
	}

	@Override
	public Logger getLogger() {
		return logger;
	}

	protected Callback callback;

	@Override
	public void activate(Callback callme) {
		callback = callme;
		activate();
	}

	public void activate() {
		if (isActive)
			return;
		isActive = true;
		clear();
		for (AbstractGraph graph : graphs)
			graph.activate();
		callback.viewActivated(this);
		checkLayout();
		if (isFullscreenSupported())
			fullscreenHandler = addFullscreenChangeHandler(this);
	}

	protected void checkLayout() {
		if (callback != null)
			callback.layoutComplete();
	}

	@Override
	public void deactivate() {
		isActive = false;
		for (AbstractGraph graph : graphs)
			graph.deactivate();
		if (fullscreenHandler != null)
			fullscreenHandler.removeHandler();
	}

	public void clear() {
	}

	@Override
	public String getStatus() {
		return getStatus(false);
	}

	@Override
	public String getStatus(boolean force) {
		return null;
	}

	@Override
	public String getCounter() {
		return null;
	}

	@Override
	public void restored() {
		timestamp = -Double.MAX_VALUE;
		for( AbstractGraph graph : graphs )
			graph.reset();
	}

	@Override
	public void reset(boolean soft) {
		timestamp = -Double.MAX_VALUE;
	}

	@Override
	public void init() {
		timestamp = -Double.MAX_VALUE;
	}

	@Override
	public boolean isRunning() {
		return callback.isRunning();
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * List of additional shortcuts provided by all views for the following keys:
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
	 */
	@Override
	public boolean keyUpHandler(String key) {
		switch (key) {
			case "S":
				// save svg snapshot (if supported)
				if (!hasExportType(EXPORT_SVG))
					return false;
				exportSVG();
				break;
			case "P":
				// save png snapshot (if supported)
				if (!hasExportType(EXPORT_PNG))
					return false;
				exportPNG();
				break;
			case "C":
				// export csv data (if supported)
				if (!hasExportType(EXPORT_DATA))
					return false;
				exportData();
				break;
			case "F":
				// toggle fullscreen (if supported)
				if (!isFullscreenSupported())
					return false;
				if (isFullscreen())
					exitFullscreen();
				else
					requestFullscreen(getElement());
				break;
			default:
				return false;
		}
		return true;
	}

	private boolean hasExportType(int export) {
		for (int e : exportTypes())
			if (e == export)
				return true;
		return false;
	}

// note: works in Safari and Chrome; some weird scaling issues remain with Firefox
//		 for Chrome it is important to use onfullscreenchange and not
//		 onwebkitfullscreenchange! the two do not seem to be identical
	@Override
	public void onFullscreenChange(FullscreenChangeEvent event) {
		if (isFullscreen())
			getFullscreenElement().addClassName("fullscreen");
		else
			wrapper.getElement().removeClassName("fullscreen");
		// deferring onResize helps Chrome to get the dimensions right (not needed for
		// Safari)
		Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
			@Override
			public void execute() {
				onResize();
			}
		});
	}

	@Override
	public void onResize() {
		for (AbstractGraph graph : graphs)
			graph.onResize();
		scheduleUpdate(true);
	}

	private boolean updateScheduled = false;

	protected void scheduleUpdate(boolean updateGUI) {
		if (updateScheduled)
			return;
		Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
			@Override
			public void execute() {
				// deferred updates can cause issues if in the mean time the model
				// has changed and this view is no longer supported. if this is the 
				// case destroyGraphs has been called and graphs is empty.
				if (!graphs.isEmpty())
					update();
				updateScheduled = false;
			}
		});
		updateScheduled = true;
	}

	public AbstractGraph getGraphAt(int x, int y) {
		if (graphs == null)
			return null;
		for (AbstractGraph graph : graphs)
			if (graph.contains(x, y))
				return graph;
		return null;
	}

	public boolean isModelType(Model.Type mtype) {
		return engine.getModel().isModelType(mtype);
	}

	protected ContextMenu exportSubmenu;
	protected ContextMenuItem restoreMenu, exportSubmenuTrigger;
	protected ContextMenuCheckBoxItem fullscreenMenu;

	@Override
	public void populateContextMenu(ContextMenu contextMenu) {
		// models may also like to add entries to context menu
		// IMPORTANT: cannot query model directly due to interference with java
		// simulations. all GUI related methods must be quarantined through EvoLudoGWT
		engine.populateContextMenu(contextMenu);

		// process fullscreen context menu
		if (fullscreenMenu == null && isFullscreenSupported()) {
			fullscreenMenu = new ContextMenuCheckBoxItem("Fullscreen", new Command() {
				@Override
				public void execute() {
					setFullscreen(!isFullscreen());
				}
			});
		}
		if (isFullscreenSupported()) {
			contextMenu.addSeparator();
			contextMenu.add(fullscreenMenu);
			fullscreenMenu.setChecked(isFullscreen());
		}

		// process exports context menu (suppress in ePub, regardless of whether a
		// standalone lab or not)
		int[] types = exportTypes();
		boolean isEPub = engine.isEPub;
		if (!isEPub && types != null && exportSubmenu == null) {
			int nTypes = types.length;
			exportSubmenu = new ContextMenu(contextMenu);
			// always include option to export current state
			exportSubmenu.add(new ContextMenuItem(exportMenuTitle[EXPORT_STATE], new ExportCommand(EXPORT_STATE)));
			for (int n = 0; n < nTypes; n++) {
				int t = types[n];
				exportSubmenu.add(new ContextMenuItem(exportMenuTitle[t], new ExportCommand(t)));
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
					callback.restoreFromFile();
				}
			});
		}
		boolean idle = !callback.isRunning();
		if (restoreMenu != null) {
			restoreMenu.setEnabled(idle);
			contextMenu.add(restoreMenu);
		}
		if (exportSubmenuTrigger != null)
			exportSubmenuTrigger.setEnabled(idle);
	}

	public void setFullscreen(boolean fullscreen) {
		if (fullscreen == isFullscreen())
			return;
		if (fullscreen)
			requestFullscreen(getElement());
		else
			exitFullscreen();
	}

	@Override
	public HandlerRegistration addFullscreenChangeHandler(FullscreenChangeHandler handler) {
		String eventname = _jsFSCname();
		_addFullscreenChangeHandler(eventname, handler);
		return new HandlerRegistration() {
			@Override
			public void removeHandler() {
				_removeFullscreenChangeHandler(eventname, handler);
			}
		};
	}

	/*
	 * helper JSNI methods to deal with fullscreen NOTE: routines work reasonably
	 * well with safari but not with other browsers because many aspects of the
	 * fullscreen API are interpreted differently.
	 */

	private final native void _addFullscreenChangeHandler(String eventname, FullscreenChangeHandler handler)
	/*-{
		$doc
				.addEventListener(
						eventname,
						function(event) {
							handler.@org.evoludo.ui.FullscreenChangeHandler::onFullscreenChange(Lorg/evoludo/ui/FullscreenChangeEvent;)(event);
						}, true);
	}-*/;

	// it is awkward that the handler function needs to be specified again when
	// removing the listener... handler must be exact copy of handler in
	// <code>_addFullscreenChangeHandler(String eventname, FullscreenChangeHandler
	// handler)</code>
	private final native void _removeFullscreenChangeHandler(String eventname, FullscreenChangeHandler handler)
	/*-{
		$doc
				.removeEventListener(
						eventname,
						function(event) {
							handler.@org.evoludo.ui.FullscreenChangeHandler::onFullscreenChange(Lorg/evoludo/ui/FullscreenChangeEvent;)(event);
						}, true);
	}-*/;

	public static native void requestFullscreen(Element ele)
	/*-{
		if (ele.requestFullscreen) {
			ele.requestFullscreen();
			// using promise:
			//			ele.requestFullscreen().then(console.log("request honoured! width="+ele.scrollWidth));
		} else if (ele.msRequestFullscreen) {
			ele.msRequestFullscreen();
		} else if (ele.mozRequestFullScreen) {
			ele.mozRequestFullScreen();
		} else if (ele.webkitRequestFullScreen) {
			ele.webkitRequestFullScreen();
		}
	}-*/;

	public static native void exitFullscreen()
	/*-{
		if ($doc.exitFullscreen) {
			$doc.exitFullscreen();
		} else if ($doc.msExitFullscreen) {
			$doc.msExitFullscreen();
		} else if ($doc.mozCancelFullScreen) {
			$doc.mozCancelFullScreen();
		} else if ($doc.webkitCancelFullScreen) {
			$doc.webkitCancelFullScreen();
		}
	}-*/;

	public final static native boolean isFullscreen()
	/*-{
		if (($doc.fullscreenElement != null)
				|| ($doc.mozFullScreenElement != null)
				|| ($doc.webkitFullscreenElement != null)
				|| ($doc.msFullscreenElement != null))
			return true;
		// NOTE: Document.fullscreen et al. are obsolete - last resort
		return $doc.fullscreen || $doc.mozFullScreen || $doc.webkitIsFullScreen ? true
				: false;
	}-*/;

	/**
	 * Check if web browser supports fullscreen.
	 * 
	 * @return <code>true</code> if fullscreen is supported
	 */
	public native boolean isFullscreenSupported()
	/*-{
		return $doc.fullscreenEnabled || $doc.mozFullScreenEnabled
				|| $doc.webkitFullscreenEnabled || $doc.msFullscreenEnabled ? true
				: false;
	}-*/;

	/**
	 * Gets fullscreen element if in fullscreen mode or <code>null</code> if not in
	 * fullscreen or fullscreen not supported by web browser.
	 *
	 * @return fullscreen element or <code>null</code>
	 */
	public final static native Element getFullscreenElement()
	/*-{
		if ($doc.fullscreenElement != null)
			return $doc.fullscreenElement;
		if ($doc.mozFullScreenElement != null)
			return $doc.mozFullScreenElement;
		if ($doc.webkitFullscreenElement != null)
			return $doc.webkitFullscreenElement;
		if ($doc.msFullscreenElement != null)
			return $doc.msFullscreenElement;
		return null;
	}-*/;

	/**
	 * Determine name of the fullscreen change event in current web browser.
	 * <p>
	 * <strong>Note:</strong> Chrome implements both <code>fullscreenchange</code>
	 * and
	 * <code>webkitfullscreenchange</code> but with slightly different behaviour
	 * (neither identical to Safari). <code>fullscreenchange</code> at least works
	 * for a single graph and hence give it precedence. For Firefox scaling/resizing
	 * issues remain as well as for Chrome with multiple graphs.
	 *
	 * @return web browser specific fullscreen change event name or
	 *         <code>null</code> if Fullscreen API not implemented.
	 */
	private static native String _jsFSCname()
	/*-{
		if ($doc.onfullscreenchange !== undefined)
			return "fullscreenchange";
		if ($doc.onwebkitfullscreenchange !== undefined)
			return "webkitfullscreenchange";
		if ($doc.onmozfullscreenchange !== undefined)
			return "mozfullscreenchange";
		if ($doc.onmsfullscreenchange !== undefined)
			return "msfullscreenchange";
		return null;
	}-*/;

	// create SVG graphics and open in new window/tab
	protected static final int EXPORT_SVG = 1;
	// create PNG graphics and open in new window/tab
	protected static final int EXPORT_PNG = 2;
	// export statistics data
	protected static final int EXPORT_DATA = 3;
	// export state (structure, fitness, parameters)
	protected static final int EXPORT_STATE = 4;

	protected static final String[] exportMenuTitle = { "-", // EXPORT_NONE
			"Vector graphics (svg)", // EXPORT_SVG
			"Bitmap graphics (png)", // EXPORT_PNG
			"Statistics (csv)", // EXPORT_DATA
			"State (plist)" // EXPORT_STATE
	};
	// further possibilities for the future
	// protected static final int EXPORT_PDF = 2;
	// protected static final int EXPORT_EPS = 3;

	/**
	 * Return the list of export types that are acceptable for _all_ graphs in this view.
	 *
	 * @return the list of viable export types
	 */
	protected int[] exportTypes() {
		return null;
	}

	protected void export(int exportType) {
		switch (exportType) {
			case EXPORT_SVG:
				exportSVG();
				break;
			case EXPORT_PNG:
				exportPNG();
				break;
			case EXPORT_DATA:
				exportData();
				break;
			case EXPORT_STATE:
				engine.exportState();
				break;
			default:
		}
		setFullscreen(false);
	}

	private static boolean hasSVGjs = false;

	protected void exportPNG() {
		Canvas canvas = Canvas.createIfSupported();
		if (canvas == null)
			return;
		int scale = getDevicePixelRatio();
		canvas.setCoordinateSpaceWidth(getOffsetWidth() * scale);
		canvas.setCoordinateSpaceHeight(getOffsetHeight() * scale);
		MyContext2d ctx = canvas.getContext2d().cast();
		export(ctx, scale);
		EvoLudoWeb._export(canvas.getCanvasElement().toDataUrl("image/png").replaceFirst("^data:image/[^;]",
				"data:application/octet-stream"), "evoludo.png");
	}

	/**
	 * Get the pixel ratio of the current device. This is intended to prevent distortions on the <code>canvas</code> objects of the data views.
	 *
	 * @return the pixel ratio of the current device
	 */
	public final static native int getDevicePixelRatio()
	/*-{
		return $wnd.devicePixelRatio || 1;
	}-*/;

	protected void exportSVG() {
		if (!hasSVGjs) {
			// script (hopefully) needs to be injected only once
			ScriptInjector.fromString(Resources.INSTANCE.canvas2SVG().getText()).inject();
			hasSVGjs = true;
		}
		int scale = getDevicePixelRatio();
		MyContext2d ctx = _createSVGContext(getOffsetWidth() * scale, getOffsetHeight() * scale);
		export(ctx, scale);
		_exportSVG(ctx);
	}

	protected void export(MyContext2d ctx, int scale) {
		int hOffset = 0;
		int vOffset = 0;
		int count = 0;
		for (AbstractGraph graph : graphs) {
			ctx.save();
			ctx.translate(hOffset, vOffset);
			graph.export(ctx);
			ctx.restore();
			if (++count%gCols==0) {
				hOffset = 0;
				vOffset += scale * graph.getOffsetHeight();				
			}
			else
				hOffset += scale * graph.getOffsetWidth();
		}
	}

	/**
	 * must be overridden by subclasses that return EXPORT_DATA among their
	 * exportTypes
	 */
	protected void exportData() {
	}

	protected static native MyContext2d _createSVGContext(int width, int height) /*-{
		return C2S(width, height);
	}-*/;

	protected static native void _exportSVG(Context2d ctx) /*-{
		@org.evoludo.EvoLudoWeb::_export(Ljava/lang/String;Ljava/lang/String;)("data:image/svg+xml;charset=utf-8,"+
			ctx.getSerializedSvg(true), "evoludo.svg");
	}-*/;

	public class ExportCommand implements Command {
		int exportType = -1;

		public ExportCommand(int exportType) {
			this.exportType = exportType;
		}

		@Override
		public void execute() {
			export(exportType);
		}
	}

	/**
	 *
	 * @author Christoph Hauert
	 */
	public interface Callback {

		/**
		 * Callback routine to notify recipient that the view has finished activation
		 * and is ready to receive/display data.
		 *
		 * @param activeView view that finished its activation.
		 */
		public void viewActivated(EvoLudoViews activeView);

		/**
		 * Notify callback that layout has completed. For example, this signals that a network
		 * is ready for taking a snapshot.
		 */
		public void layoutComplete();

		/**
		 * Enables interactive data views to check whether EvoLudo model is running. For
		 * example, mouse clicks that would change the strategy of an individual in
		 * {@link MVPop2D} or {@link MVPop3D} are ignored if model is running.
		 *
		 * @return <code>true</code> if model is running.
		 */
		public boolean isRunning();

		/**
		 * Request EvoLudo model to restore a previously saved state of the model. This
		 * method is called by the context menu (see
		 * {@link MVAbstract#populateContextMenu(ContextMenu)}).
		 */
		public void restoreFromFile();
	}
}