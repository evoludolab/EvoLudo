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

package org.evoludo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import org.evoludo.geom.Rectangle2D;
import org.evoludo.simulator.EvoLudo;
import org.evoludo.simulator.EvoLudoGWT;
import org.evoludo.simulator.EvoLudoTrigger;
import org.evoludo.simulator.Resources;
import org.evoludo.simulator.models.ChangeListener;
import org.evoludo.simulator.models.Data;
import org.evoludo.simulator.models.MilestoneListener;
import org.evoludo.simulator.models.Mode;
import org.evoludo.simulator.models.Model;
import org.evoludo.simulator.models.SampleListener;
import org.evoludo.simulator.models.Type;
import org.evoludo.simulator.modules.Module;
import org.evoludo.simulator.views.AbstractView;
import org.evoludo.simulator.views.Console;
import org.evoludo.simulator.views.Distribution;
import org.evoludo.simulator.views.HasDistribution;
import org.evoludo.simulator.views.HasHistogram;
import org.evoludo.simulator.views.HasMean;
import org.evoludo.simulator.views.HasPhase2D;
import org.evoludo.simulator.views.HasPop2D;
import org.evoludo.simulator.views.HasPop3D;
import org.evoludo.simulator.views.HasS3;
import org.evoludo.simulator.views.Histogram;
import org.evoludo.simulator.views.Mean;
import org.evoludo.simulator.views.Phase2D;
import org.evoludo.simulator.views.Pop2D;
import org.evoludo.simulator.views.Pop3D;
import org.evoludo.simulator.views.S3;
import org.evoludo.ui.ContextMenu;
import org.evoludo.ui.FullscreenChangeEvent;
import org.evoludo.ui.FullscreenChangeHandler;
import org.evoludo.ui.HasFullscreenChangeHandlers;
import org.evoludo.ui.InputEvent;
import org.evoludo.ui.Slider;
import org.evoludo.util.CLOCategory;
import org.evoludo.util.CLODelegate;
import org.evoludo.util.CLOParser;
import org.evoludo.util.CLOProvider;
import org.evoludo.util.CLOption;
import org.evoludo.util.NativeJS;
import org.evoludo.util.Plist;
import org.evoludo.util.PlistParser;
import org.evoludo.util.XMLCoder;
import org.evoludo.util.XMLLogFormatter;

import com.google.gwt.canvas.client.Canvas;
import com.google.gwt.core.client.Duration;
import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.ScriptInjector;
import com.google.gwt.dom.client.DataTransfer;
import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NodeList;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.DragEnterEvent;
import com.google.gwt.event.dom.client.DragLeaveEvent;
import com.google.gwt.event.dom.client.DragLeaveHandler;
import com.google.gwt.event.dom.client.DragOverEvent;
import com.google.gwt.event.dom.client.DropEvent;
import com.google.gwt.event.dom.client.TouchEndEvent;
import com.google.gwt.event.dom.client.TouchStartEvent;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.logging.client.ConsoleLogHandler;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.DeckLayoutPanel;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Frame;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.HeaderPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.ResizeLayoutPanel;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.Widget;

/**
 * <h2>EvoLudoWeb</h2>
 *
 * <p>
 * Top-level GUI controller for an EvoLudo laboratory instance running in a GWT
 * environment (browser or ePub). This class implements the primary user
 * interface and coordinates between the GWT widgets, the EvoLudo engine
 * (EvoLudoGWT), and a collection of AbstractView implementations that render
 * model data. It provides lifecycle integration with GWT (EntryPoint,
 * onModuleLoad/onLoad/onUnload), runtime control of the engine (start/stop,
 * init/reset, step/debug), management of command-line options (CLO), keyboard
 * and drag-and-drop handlers, export/import of state, as well as ePub-specific
 * accommodations.
 * </p>
 *
 * <h3>Responsibilities</h3>
 * <ul>
 * <li>Create and bind the GUI widgets declared in the accompanying UiBinder
 * template.</li>
 * <li>Instantiate and configure the EvoLudoGWT engine and its logger and
 * Console view.</li>
 * <li>Parse and apply command-line options (CLO) to load modules and models,
 * configure views, and restore saved state.</li>
 * <li>Maintain and update a dynamic list of active data views for the
 * running model, showing only the appropriate views for module/model
 * features (2D/3D populations, histograms, statistics, console).</li>
 * <li>Provide keyboard shortcuts and a JS-friendly key listener mechanism
 * (via JSNI) that reliably receives global key events even if GWT's
 * default handlers lose focus.</li>
 * <li>Support drag-and-drop of saved state files, including integrating
 * optional JavaScript zip handling when needed.</li>
 * <li>Handle fullscreen events and resizing, and provide a snapshot-ready
 * signal for automated site capture.</li>
 * <li>Adjust behavior for ePub contexts (inline flow vs standalone page),
 * disabling editing, console, and drag-and-drop where necessary.</li>
 * <li>Expose helper JS entry points to create/insert labs or triggers from
 * external JavaScript (createEvoLudoLab, insertEvoLudoLab,
 * insertEPubEvoLudoLab, createEvoLudoTrigger).</li>
 * </ul>
 *
 * <h3>Lifecycle</h3>
 * <p>
 * - onModuleLoad(): scans the DOM for marker elements (evoludo-simulation and
 * evoludo-trigger-html) and replaces them with interactive labs or trigger
 * buttons. <br>
 * - onLoad(): called when the widget is attached; sets up the engine, logger,
 * console, fullscreen element, and applies any CLO present. <br>
 * - onUnload(): attempts to gracefully shut down the engine, clear the console
 * and remove global listeners. Care is taken to avoid throwing exceptions
 * during unload to keep GWT module reloads stable.
 * </p>
 *
 * <h3>Command-Line Options (CLO)</h3>
 * <p>
 * The class supplies and consumes CLO options that control initial view and
 * GUI size/fullscreen. It provides an editable CLO field in the UI (unless
 * disabled for ePubs), an Apply button (or Standalone link in ePubs), and a
 * Default button to revert to initial settings. The applyCLO() flow:
 * parse CLO, update engine/module/model, load and size views, restore state if
 * present, and activate/resume execution according to parsed flags (e.g. --run,
 * --snap, --samples).
 * </p>
 *
 * <h3>Views and Display</h3>
 * <p>
 * EvoLudoWeb keeps a map of active views (activeViews) and a DeckLayoutPanel
 * (evoludoDeck) that displays exactly one view at a time. Views are created or
 * reused based on module capabilities and model type. Views are responsible for
 * rendering, while EvoLudoWeb orchestrates loading, sizing, activation,
 * deactivation, and disposal. The Console view is treated specially and may be
 * omitted in restricted ePub modes.
 * </p>
 *
 * <h3>Input Handling and Shortcuts</h3>
 * <p>
 * Global keyboard handling is implemented to allow reliable shortcuts that
 * control the engine and UI. Shortcuts are split into keyDownHandler (for
 * repeatable actions like stepping or changing speed) and keyUpHandler (for
 * non-repeatable actions like start/stop, toggling settings, view selection,
 * and export). The class additionally exposes JSNI routines to add/remove
 * window-level listeners that delegate to these handlers. Touch interaction
 * variants are supported for critical controls (Start/Stop, Init/Reset,
 * Settings).
 * </p>
 *
 * <h3>Drag-and-Drop and State Restore</h3>
 * <p>
 * The widget supports drag-and-drop of saved state files. It verifies data
 * transfer validity and, when necessary, injects a JavaScript ZIP handler to
 * decompress archives. Restored state is parsed into a Plist, applied to the
 * engine, and the GUI reconfigured to reflect the restored model and settings.
 * </p>
 *
 * <h3>Logging</h3>
 * <p>
 * Logging is routed through the engine's Logger. EvoLudoWeb installs a custom
 * EvoLogHandler that mirrors important messages to the on-screen Console and
 * to the status line (for warnings/errors). Help text and preformatted blocks
 * are handled carefully to preserve formatting and safe encoding for XML/XHTML
 * contexts and ePubs.
 * </p>
 *
 * <h3>Concurrency and Threading</h3>
 * <p>
 * GWT executes UI work on the browser main thread. EvoLudoWeb schedules
 * deferred actions via the Scheduler where appropriate and ensures that model
 * state changes (start/stop/reset) are requested via the engine's request APIs.
 * No internal multithreading beyond browser event callbacks is used.
 * </p>
 *
 * <h3>Extension Points</h3>
 * <ul>
 * <li>Views: Implement AbstractView to add new visualizations. EvoLudoWeb will
 * discover and include them based on module interfaces and model type.</li>
 * <li>CLO Providers: EvoLudoWeb implements CLOProvider to contribute GUI
 * related options and to react when the engine parses command-line
 * arguments.</li>
 * <li>FullscreenChangeHandler: the class listens for fullscreen changes to
 * apply styling and sizing changes.</li>
 * </ul>
 *
 * <h3>ePub Behaviour</h3>
 * <p>
 * EvoLudoWeb adapts to ePub readers: inline labs in an ePub may have editing,
 * drag-and-drop, and console functionality disabled to accommodate reader
 * restrictions. If the lab is on a standalone ePub page, full interactivity is
 * allowed. Special workarounds are applied for known platform quirks (e.g.
 * Apple Books/touch event handler injection).
 * </p>
 * <strong>Generic reader notes:</strong>
 * <ol>
 * <li>button elements do not work in ePub's and hence trigger-'buttons' must
 * be provided as anchor elements.</li>
 * </ol>
 * <p>
 * <strong>Apple Books:</strong>
 * <ol>
 * <li>challenges for passing information to non-linear page (clo parameters)
 * <ul>
 * <li>parameters and hash cleared from url before opening non-linear page</li>
 * <li>non-linear page has no opener, referrer, parent to link to calling
 * document</li>
 * <li>no need to read data-clo of triggers because parameters cannot be
 * reliably transmitted to popup lab; besides, it would be much easier to simply
 * append the data to the URL.</li>
 * <li>localStorage does not reliably work (>=2sec delay is required between
 * setting the local variable and opening EvoLudoLab.xhtml...)
 * <li>changes to localStorage do not fire storage events</li>
 * </ul>
 * <strong>Solution:</strong> programmatically create individual XHTML pages for
 * each lab, which includes the correct parameters in the data-clo tag, and link
 * to that page.</li>
 * <li>need to do something about touch events on desktop...
 * <ul>
 * <li>attaching touch handlers to triggers does nothing</li>
 * <li>Apple's TouchEvents.js patch does not play nicely with GWT</li>
 * <li>patch seems to prevent marking/highlighting of text passages. worth
 * verifying and filing bug report with Apple if this is indeed the case?</li>
 * </ul>
 * <strong>Solution:</strong> inject script TouchEventsGWT.js for iBooks without
 * touch-events to patch the patch. this could probably be done in a JSNI
 * routine.</li>
 * <li>events in ePubs:
 * <ul>
 * <li>iBooks does not fire 'wheel' or 'scroll' events (i.e. zooming does not
 * work) for labs in the flow of the text; no point in adding them to the
 * canvas(es). however, ok in non-linear labs!</li>
 * <li>key events are sent (at least) to all labs in current chapter (isShowing
 * does not work because it's impossible (?) to determine current page
 * visible...); do not process key events for labs in the flow of the text
 * (except maybe 'Alt'?). however, ok in non-linear labs!</li>
 * <li>check for full screen capabilities causes flicker in iBooks (ugly...);
 * seems to be related to key event handing (never observed for context menu);
 * should be resolved by disabling key handler;</li>
 * </ul>
 * <strong>Solution:</strong> for labs in text flow 'wheel', 'key' and
 * 'fullscreen' events disabled ('Alt' still processed, if ePub has
 * 'key-events') but not in standalone labs. As a consequence, labs in text also
 * do not include console (impossible to scroll...).</li>
 * <li>use different GUI elements in ePub
 * <ul>
 * <li>parameters in <code>evoludoCLO</code> text area are not read-only but
 * essentially impossible to enter or edit anything; parameters should be
 * read-only (mark with grey text color or similar); disable 'Apply' button,
 * change 'Default' button to 'Standalone/External' lab opening a non-linear
 * page or redirecting to evoludo.org, respectively (use 'Alt' key).</li>
 * </ul>
 * <strong>Solution:</strong> <code>evoludoCLO</code> is read only for labs in
 * text flow. 'Apply' and 'Help' buttons disabled (no console to display help
 * text because of scrolling issues). 'Default' button is repurposed to open
 * standalone lab.</li>
 * <li>Clicks on button text do work for starting simulations but not for
 * stopping them... clicking on the button next to its text works as expected.
 * </ol>
 * <p>
 * <strong>Adobe Digital Editions</strong>
 * <ol>
 * <li>non-linear pages are appended to the end, which makes returning to the
 * corresponding text location impossible (somehow a marker should be set to
 * allow using the back button).</li>
 * </ol>
 * 
 * <h3>Usage</h3>
 * <ol>
 * <li>Place a &lt;div class="evoludo-simulation"&gt; in the HTML with an
 * optional
 * data-clo attribute or supply &quot;clo&quot; in the URL. The module will
 * instantiate EvoLudoWeb for each such element during onModuleLoad().</li>
 * <li>Alternatively, call the exported JavaScript helpers to create or insert
 * labs dynamically.</li>
 * <li>Interact with the lab through the provided GUI or keyboard shortcuts.
 * Use Apply/Default to manage parameters and use Export/drag-and-drop to
 * save/restore state.</li>
 * </ol>
 *
 * @implSpec
 *           This class is designed to be a single, self-contained controller
 *           per DOM container element. It expects to run within the GWT
 *           environment and to co-operate with EvoLudoGWT, the Model/Module
 *           abstraction, and various AbstractView subclasses. It uses JSNI for
 *           some browser integration and therefore relies on the presence of
 *           the window object and typical browser DOM APIs.
 *
 * @see EvoLudoGWT
 * @see AbstractView
 * @see Console
 */
public class EvoLudoWeb extends Composite
		implements HasFullscreenChangeHandlers, FullscreenChangeHandler, MilestoneListener, ChangeListener,
		SampleListener, CLOProvider, EntryPoint {

	/**
	 * GWT magic to define GUI elements (see {@literal EvoLudoWeb.ui.xml}).
	 */
	interface EvoLudoWebBinder extends UiBinder<Widget, EvoLudoWeb> {
	}

	/**
	 * GWT magic to create GUI elements (see {@literal EvoLudoWeb.ui.xml}).
	 */
	private static EvoLudoWebBinder uiBinder = GWT.create(EvoLudoWebBinder.class);

	/**
	 * Time of last GUI update
	 */
	protected double updatetime = -1.0;

	/**
	 * Reference to running engine in ePubs (to conserve resources only one lab can
	 * be running at a time)
	 */
	static EvoLudoWeb runningEPub;

	/**
	 * Flag indicating whether this lab is running in an ePub reader.
	 */
	private boolean isEPub = false;

	/**
	 * <code>true</code> if standalone EvoLudo lab in ePub
	 */
	static boolean ePubStandalone = false;

	/**
	 * <code>true</code> if ePub has mouse device
	 */
	static boolean ePubHasMouse = false;

	/**
	 * <code>true</code> if ePub has touch device
	 */
	static boolean ePubHasTouch = false;

	/**
	 * <code>true</code> if ePub has keyboard device
	 */
	static boolean ePubHasKeys = false;

	/**
	 * Controller. Manages the interface with the outside world.
	 */
	EvoLudoGWT engine;

	/**
	 * Logger for keeping track of and reporting events and issues.
	 */
	protected Logger logger;

	/**
	 * Handler for the log framework to report notifications etc. in EvoLudo's
	 * console
	 */
	protected EvoLogHandler logEvoHandler;

	/**
	 * Reference to registration of drag'n'drop handlers.
	 */
	HandlerRegistration dragEnterHandler;

	/**
	 * Reference to registration of drag'n'drop handlers.
	 */
	HandlerRegistration dragLeaveHandler;

	/**
	 * The reference to the fullscreen event handler.
	 */
	HandlerRegistration fullscreenHandler;

	/**
	 * The reference to the fullscreen widget.
	 */
	Widget fullscreenWidget;

	/**
	 * ID of element in DOM that contains the EvoLudo lab.
	 */
	String elementID = "EvoLudoWeb";

	/**
	 * Look-up table for active views. This is the selection shown in
	 * {@link #evoludoViews}.
	 */
	HashMap<String, AbstractView<?>> activeViews = new HashMap<>();

	/**
	 * By default the first data view is shown. In general this shows the strategies
	 * in the (structured) population in 2D.
	 */
	private String initialView = "1";

	/**
	 * Currently visible view
	 */
	AbstractView<?> activeView;

	/**
	 * The transparent backdrop of popup EvoLudo labs is stored here to reuse.
	 */
	protected EvoLudoTrigger.LightboxPanel popup = null;

	/**
	 * Console view requires slightly special treatment to ensure results of early
	 * feature detection get reported.
	 */
	Console viewConsole;

	/**
	 * Early initializations.
	 */
	static {
		EvoLudoWeb.exportInsertEvoLudoLab();
		EvoLudoWeb.exportInsertEPubEvoLudoLab();
		EvoLudoWeb.exportCreateEvoLudoLab();
		String ePub = NativeJS.getEPubReader();
		if ((ePub != null) && ePub.startsWith("iBooks") && !NativeJS.ePubReaderHasFeature("touch-events")) {
			// patch macOS patch for Apple Books (iBooks) on devices without touch
			// (doesn't play nicely with GWT without some gentle encouragement).
			ScriptInjector.fromString(Resources.INSTANCE.touchEventsGWTHandler().getText())
					.setWindow(ScriptInjector.TOP_WINDOW).inject();
		}
	}

	/**
	 * <strong>Note:</strong> empty default constructor seems to be required by GWT.
	 * Why? At least it does not need to be publicly exposed.
	 */
	private EvoLudoWeb() {
		super();
		isWebGLSupported = false;
	}

	/**
	 * Constructor restricted to trigger buttons that create an overlay displaying
	 * an EvoLudo lab.
	 *
	 * @param id    of trigger element in DOM (unique and automatically generated,
	 *              {@link #onModuleLoad()}.
	 * @param popup semi-transparent overlay
	 */
	public EvoLudoWeb(String id, EvoLudoTrigger.LightboxPanel popup) {
		this(id, (String) null);
		this.popup = popup;
	}

	/**
	 * Main constructor for EvoLudo labs.
	 * <p>
	 * <strong>Note:</strong>
	 * <ul>
	 * <li>If <code>id==null</code> the default ID {@link #elementID} is used
	 * <li>Attempts to fail gracefully if no element with <code>id</code> exists in
	 * DOM or if the HTML5 <code>canvas</code> element is not supported.
	 * </ul>
	 *
	 * @param id  the DOM id of the element containing the lab
	 * @param clo the string with the command line options
	 */
	public EvoLudoWeb(String id, String clo) {
		if (id != null)
			elementID = id;
		RootPanel root = RootPanel.get(elementID);
		if (root == null)
			throw new IllegalArgumentException("Element with ID '" + elementID + "' does not exist!");

		initWidget(uiBinder.createAndBindUi(this));
		// hide overlay
		evoludoOverlay.setVisible(false);

		// create canvas
		if (Canvas.createIfSupported() == null) {
			Label canvasError = new Label("ERROR: no HTML5 canvas available!\ntry another browser!");
			evoludoDeck.add(canvasError);
			isWebGLSupported = false;
			return;
		}
		// canvas is supported, now check if WebGL is supported as well
		isWebGLSupported = NativeJS.isWebGLSupported();

		// set command line options
		evoludoCLO.getElement().setAttribute("contenteditable", "true");
		evoludoCLO.setText(clo != null ? clo : "");

		// layout speed controls - GWT has no slider...
		// but we can construct one from HTML5!
		evoludoSlider.setRange((int) EvoLudo.DELAY_MAX, (int) EvoLudo.DELAY_MIN);
		evoludoSlider.setLogBase(1.1);
		// note: the {}-place holders interfere with UiBinder...
		evoludoSlider.setTitle("Set delay between updates ({max} - {min}msec); now at {value}msec");

		// add full screen change handler
		if (NativeJS.isFullscreenSupported())
			fullscreenHandler = addFullscreenChangeHandler(this);
	}

	/**
	 * Entry point method. Process DOM and add/allocate an EvoLudo lab whenever a
	 * {@code <div>} element with <code>class="evoludo-simulation"</code> is found.
	 * Parameters may be passed to the EvoLudo lab either through the
	 * <code>data-clo</code> attribute or as part of the URL as a parameter
	 * <code>clo</code>. Similarly, {@code <div>} elements with
	 * <code>class="evoludo-trigger-html"</code> are converted to buttons that
	 * trigger a popup EvoLudo lab. Buttons do not work in ePub's and hence
	 * trigger-'buttons' must be provided as anchor elements.
	 */
	@Override
	public void onModuleLoad() {
		// make sure css styling stuff is loaded
		Resources.INSTANCE.css().ensureInjected();
		// process DOM and replace all div's with class evoludo-simulation by an
		// interactive lab
		processEvoLudoLabs();
		// process DOM and replace all div's with class evoludo-trigger-html by a
		// trigger button
		processEvoLudoTriggers();
	}

	/**
	 * Process DOM and add/allocate an EvoLudo lab whenever a {@code <div>} element
	 * with <code>class="evoludo-simulation"</code> is found.
	 * <p>
	 * The first lab found is passed any command line options specified in the URL
	 * (parameter <code>clo</code>). Any subsequent labs need to specify command
	 * line options through the <code>data-clo</code> attribute.
	 */
	private void processEvoLudoLabs() {
		NodeList<Element> labs = NativeJS.querySelectorAll("div.evoludo-simulation");
		int nLabs = labs.getLength();
		for (int n = 0; n < nLabs; n++) {
			Element labElement = labs.getItem(n);
			String clo = (n == 0 ? getCLOFromURL() : null);
			addEvoLudoToDOM(labElement, clo);
		}
	}

	/**
	 * Attribute name for command line options in DOM elements.
	 */
	static final String DOM_DATA_CLO = "data-clo";

	/**
	 * Add an EvoLudo lab to the DOM element with the given ID. If the ID is
	 * {@code null} or empty,
	 * a unique ID is generated. If {@code clo} is {@code null} or empty, the
	 * {@code data-clo} attribute
	 * is read and used to set the command line options for the lab.
	 * 
	 * @param labElement the DOM element representing the lab
	 * @param clo        the command line options for the lab
	 */
	private void addEvoLudoToDOM(Element labElement, String clo) {
		HTMLPanel panel = HTMLPanel.wrap(labElement);
		String id = labElement.getId();
		if (id == null || id.isEmpty())
			id = HTMLPanel.createUniqueId();
		labElement.setId(id);
		if (clo != null && !clo.isEmpty())
			labElement.setAttribute(DOM_DATA_CLO, clo);
		EvoLudoWeb lab = new EvoLudoWeb(id, clo);
		panel.add(lab);
	}

	/**
	 * Extract command line options from URL parameter {@code clo}.
	 * 
	 * @return the command line options
	 */
	private String getCLOFromURL() {
		String clo = Window.Location.getParameter("clo");
		if (clo != null) {
			clo = clo.trim();
			if (!clo.isEmpty()) {
				if (clo.charAt(0) == '"' || clo.charAt(0) == '“' || clo.charAt(0) == "'".charAt(0))
					clo = clo.substring(1);
				if (clo.charAt(clo.length() - 1) == '"' || clo.charAt(clo.length() - 1) == '”'
						|| clo.charAt(clo.length() - 1) == "'".charAt(0))
					clo = clo.substring(0, clo.length() - 1);
				// converts + (and %2B) into spaces to prevent troubles (e.g. for geometries)
				// clo = URL.decodeQueryString(clo);
			}
		}
		return clo;
	}

	/**
	 * Process DOM and replace all {@code <div>} elements with
	 * <code>class="evoludo-trigger-html"</code> by a button that triggers a popup
	 * EvoLudo lab.
	 * <p>
	 * <strong>Note:</strong> buttons do not work in ePub's and hence
	 * trigger-'buttons' must be provided as anchor elements.
	 */
	private void processEvoLudoTriggers() {
		NodeList<Element> triggers = NativeJS.querySelectorAll("div.evoludo-trigger-html");
		int nTriggers = triggers.getLength();
		for (int n = 0; n < nTriggers; n++) {
			Element triggerElement = triggers.getItem(n);
			HTMLPanel trigger = HTMLPanel.wrap(triggerElement);
			String id = triggerElement.getId();
			if (id == null || id.isEmpty()) {
				id = HTMLPanel.createUniqueId();
				triggerElement.setId(id);
			}
			trigger.add(new EvoLudoTrigger(id));
		}
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * Called when loading GWT application. The contents of {@link #evoludoCLO}
	 * serve as the command line options. If {@link #evoludoCLO} is empty, the
	 * <code>data-clo</code> attribute of the lab element is checked for command
	 * line options.
	 */
	@Override
	public void onLoad() {
		super.onLoad();
		setupEngine();
		setupLogger(engine);
		setupConsole(logger);

		// now evoludoPanel is attached and we can set the grandparent as the
		// fullscreen element
		fullscreenWidget = evoludoPanel.getParent().getParent();
		engine.setFullscreenElement(fullscreenWidget.getElement());

		String clo = engine.getCLO();
		// clo may have been set from the URL or as an HTML attribute
		if (clo == null || clo.isEmpty()) {
			RootPanel root = RootPanel.get(elementID);
			if (root != null)
				clo = root.getElement().getAttribute(DOM_DATA_CLO).trim();
		}
		engine.setCLO(clo);
		applyCLO();
	}

	/**
	 * Create EvoLudo engine and load modules.
	 */
	private void setupEngine() {
		if (engine != null)
			return;
		engine = new EvoLudoGWT(this);
		engine.loadModules();
		engine.addCLOProvider(this);
		engine.addMilestoneListener(this);
		engine.addSampleListener(this);
		engine.addChangeListener(this);
		engine.setCLO(evoludoCLO.getText().trim());
	}

	/**
	 * Retrieve the logger and setup the log handler.
	 * 
	 * @param engine the EvoLudo engine
	 */
	private void setupLogger(EvoLudoGWT engine) {
		if (logger != null)
			return;
		logger = engine.getLogger();
		logger.setLevel(Level.INFO);
		// note: log handler needs to know whether this is an ePub (regardless of any
		// feature declarations with --gui) to make sure log is properly XML encoded (if
		// needed).
		isEPub = NativeJS.getEPubReader() != null;
		ConsoleLogHandler logHandler = new ConsoleLogHandler();
		logHandler.setFormatter(new XMLLogFormatter(true, isEPub || NativeJS.isHTML()));
		logger.addHandler(logHandler);
	}

	/**
	 * Setup the EvoLudo console to display log messages.
	 * 
	 * @param logger the logger instance
	 */
	private void setupConsole(Logger logger) {
		if (viewConsole != null)
			return;
		viewConsole = new Console(engine);
		logEvoHandler = new EvoLogHandler(viewConsole);
		logger.addHandler(logEvoHandler);
		logger.setLevel(Level.INFO);
		logger.info(engine.getVersion());
		logFeatures();
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * Called when unloading GWT application. Housekeeping routine to clean up and
	 * free as many resources as possible. For example, ensures that EvoLudo model
	 * stops running, removes lab from key listeners, removes drag'n'drop handler,
	 * unloads model.
	 * <p>
	 * <strong>Important:</strong> ensure that no errors/exceptions are thrown here!
	 * All errors/exceptions are caught and ignored, which results in incomplete
	 * unloading and most likely subsequent re-loading of module will fail. A good
	 * indicator is if the info message "Module XYZ unloaded" appears in log.
	 */
	@Override
	public void onUnload() {
		// clear command line options to ensure they are reset to the original
		// values should the same model get loaded again.
		// note: this clears command line options set via URL but it does not seem
		// possible to load those again. ditto for epubs. only seems to apply
		// to trigger buttons and overlay labs.
		engine.setCLO(null);
		engine.requestAction(PendingAction.SHUTDOWN, true);
		viewConsole.clearLog();
		super.onUnload();
	}

	@Override
	public void moduleLoaded() {
		// assume that some kind of keys are always present, i.e. always add listener
		// for e.g. 'Alt' but key shortcuts only if not ePub
		addKeyListener(this);
		if (popup != null)
			setKeyListener(this);
	}

	@Override
	public void moduleUnloaded() {
		activeView = null;
		evoludoViews.setSelectedIndex(-1);
		removeKeyListener(this);
		if (keyListener == this)
			setKeyListener(null);
		// clear settings
		evoludoCLO.setText("");
	}

	/**
	 * Set the key listener for the EvoLudoWeb instance.
	 * 
	 * @param keyListener the key listener
	 */
	static void setKeyListener(EvoLudoWeb keyListener) {
		EvoLudoWeb.keyListener = keyListener;
	}

	@Override
	public void moduleRestored() {
		displayStatusThresholdLevel = Level.ALL.intValue();
		displayStatus("State successfully restored.");
	}

	@Override
	public void modelUnloaded() {
		activeView = null;
		evoludoViews.setSelectedIndex(-1);
	}

	@Override
	public void modelRunning() {
		if (isEPub) {
			if (runningEPub != null)
				throw new IllegalStateException("Another ePub lab is already running!");
			setRunningEPub(this);
		}
		displayStatusThresholdLevel = Level.ALL.intValue();
		updateGUI();
	}

	/**
	 * Set the currently running ePub lab.
	 * 
	 * @param lab the running ePub lab
	 */
	static void setRunningEPub(EvoLudoWeb lab) {
		runningEPub = lab;
	}

	@Override
	public void modelChanged(PendingAction action) {
		switch (action) {
			case CHANGE_MODE:
				// reset threshold for status messages after mode change
				displayStatusThresholdLevel = Level.ALL.intValue();
				//$FALL-THROUGH$
			case NONE:
				updateStatus();
				updateCounter();
				break;
			default:
				// includes SHUTDOWN, RESET, INIT, STOP, MODE
		}
		if (!engine.isRunning())
			updateGUI();
	}

	@Override
	public void modelSample(boolean success) {
		if (success)
			updateStatus();
		updateCounter();
	}

	@Override
	public void modelStopped() {
		if (isEPub) {
			if (runningEPub == null)
				throw new IllegalStateException("Running ePub lab not found!");
			if (runningEPub == this)
				setRunningEPub(null);
		}
		updateGUI();
	}

	@Override
	public void modelDidInit() {
		updateGUI();
	}

	@Override
	public void modelDidReset() {
		updateGUI();
		// show version after reset but do not overwrite warnings and errors`
		displayStatus(engine.getVersion(), Level.INFO.intValue() + 1);
		if (snapmarker != null)
			Document.get().getBody().removeChild(snapmarker);
		// reset threshold for status messages after reset
		displayStatusThresholdLevel = Level.ALL.intValue();
	}

	/**
	 * Update GUI for running/stopped model.
	 */
	private void updateGUI() {
		boolean stopped = !engine.isRunning();
		evoludoStartStop.setText(stopped ? BUTTON_START : BUTTON_STOP);
		evoludoStep.setEnabled(stopped);
		evoludoInitReset.setEnabled(stopped);
		evoludoApply.setEnabled(stopped);
		evoludoDefault.setEnabled(stopped);
		updateKeys();
		evoludoSlider.setValue(engine.getDelay());
		Model model = engine.getModel();
		if (model == null) {
			evoludoSlider.setEnabled(true);
			return;
		}
		evoludoSlider.setEnabled(model.getMode() != Mode.STATISTICS_SAMPLE);
		if (stopped)
			updateStatus();
		updateCounter();
	}

	/**
	 * Helper method to update status of GUI.
	 */
	private void updateStatus() {
		Model model = engine.getModel();
		// do not force retrieving status if engine is running
		String s = activeView.getStatus(!engine.isRunning());
		if (s == null)
			s = model.getStatus();
		displayStatus(s);
		updatetime = Duration.currentTimeMillis();
	}

	/**
	 * Helper method to update counter of GUI.
	 */
	private void updateCounter() {
		evoludoTime.setText(engine.getModel().getCounter());
	}

	/**
	 * Outermost panel containing the EvoLudo GUI.
	 */
	@UiField
	HTMLPanel evoludoPanel;

	/**
	 * Panel implementing the ability to resize the GUI of EvoLudo models
	 * (only possible in browser or standalone mode in ePubs).
	 */
	@UiField
	ResizeLayoutPanel evoludoResize;

	/**
	 * Basic layout of EvoLudo model GUI with header (for time display,
	 * {@link #evoludoTime}, and view selector {@link #evoludoViews}), footer (for
	 * delay slider {@link #evoludoSlider}, control buttons
	 * {@link #evoludoSettings}, {@link #evoludoInitReset},
	 * {@link #evoludoStartStop}, {@link #evoludoStep} and status line
	 * {@link #evoludoStatus} plus possibly parameters {@link #evoludoCLOPanel}) as
	 * well as the main content area (for different canvases, {@link evoludoDeck}).
	 */
	@UiField
	HeaderPanel evoludoLayout;

	/**
	 * Label to display elapsed generations (time).
	 */
	@UiField
	Label evoludoTime;

	/**
	 * Selector for all graphical representations to visualize the state of the
	 * EvoLudo model.
	 */
	@UiField
	ListBox evoludoViews;

	/**
	 * Handler for changes of the view selector ({@link #evoludoViews}).
	 *
	 * @param event the {@link ChangeEvent} that was fired
	 */
	@UiHandler("evoludoViews")
	public void onViewChange(ChangeEvent event) {
		changeViewTo(activeViews.get(evoludoViews.getSelectedItemText()));
		evoludoViews.setFocus(false);
	}

	/**
	 * Change view of EvoLudo model data. This helper method is called when the user
	 * selects a new view with the popup list {@link #evoludoViews} or when a
	 * particular view is requested through command line options (see
	 * {@link #cloView}).
	 * 
	 * @param newView new view of model data to display
	 */
	protected void changeViewTo(AbstractView<?> newView) {
		changeViewTo(newView, false);
	}

	/**
	 * Change view of EvoLudo model data. This helper method is called when the user
	 * selects a new view with the popup list {@link #evoludoViews} or when a
	 * particular view is requested through command line options (see
	 * {@link #cloView}). The view is re-activated if {@code force} is {@code true}
	 * and activation is skipped if the view didn't change and {@code force} is
	 * {@code false}.
	 *
	 * @param newView new view of model data to display
	 * @param force   if {@code true} the view is re-activated even if it didn't
	 *                change
	 */
	protected void changeViewTo(AbstractView<?> newView, boolean force) {
		if (!force && newView == activeView)
			return;

		if (activeView != null)
			activeView.deactivate();
		// initially activeView would otherwise be null, which causes troubles if mouse
		// is on canvas triggering events...
		activeView = newView;
		evoludoDeck.showWidget(activeView);
		// set selected item in view selector
		int activeIdx = evoludoDeck.getWidgetIndex(activeView);
		evoludoViews.setSelectedIndex(activeIdx);
		// save index of view if not console
		if (activeIdx != activeViews.size() - 1)
			viewIdx = activeIdx;
		// adding a new widget can cause a flurry of activities; wait until they subside
		// before activation
		Scheduler.get().scheduleDeferred(() -> activeView.activate());
		updateGUI();
	}

	/**
	 * Panel containing all the canvas elements to display the EvoLudo model's data.
	 * Only one is shown at any time, selected by {@link #evoludoViews}.
	 */
	@UiField
	DeckLayoutPanel evoludoDeck;

	/**
	 * Slider adjusting the delay between updates.
	 */
	@UiField
	Slider evoludoSlider;

	/**
	 * Slider changed, adjust delay between updates. This event is triggered when
	 * clicking or taping on the slider (surprisingly, this does not trigger an
	 * InputEvent).
	 *
	 * @param event the {@link ClickEvent} that was fired
	 */
	@UiHandler("evoludoSlider")
	public void onSliderClick(ClickEvent event) {
		engine.setDelay((int) evoludoSlider.getValue());
		evoludoSlider.setValue(engine.getDelay());
	}

	/**
	 * Slider changed, adjust delay between updates. This event is triggered when
	 * programmatically changing the slider settings, e.g. after processing key
	 * events to increase/decrease delay or when sliding the slider with touches or
	 * the mouse.
	 *
	 * @param event the {@link InputEvent} that was fired
	 */
	@UiHandler("evoludoSlider")
	public void onSliderInput(InputEvent event) {
		engine.setDelay((int) evoludoSlider.getValue());
		evoludoSlider.setValue(engine.getDelay());
	}

	/**
	 * Label of button to show/hide parameter settings.
	 */
	static final String BUTTON_SETTINGS = "Settings";

	/**
	 * Label of button to start model.
	 */
	static final String BUTTON_START = "Start";

	/**
	 * Label of button to stop model.
	 */
	static final String BUTTON_STOP = "Stop";

	/**
	 * Label of button to initialize model.
	 */
	static final String BUTTON_INIT = "Init";

	/**
	 * Label of button to reset model.
	 */
	static final String BUTTON_RESET = "Reset";

	/**
	 * Label of button to advance model by one step.
	 */
	static final String BUTTON_STEP = "Step";

	/**
	 * Label of button to advance model by single microscopic step.
	 */
	static final String BUTTON_DEBUG = "Debug";

	/**
	 * Label of button to collect one statistics sample.
	 */
	static final String BUTTON_SAMPLE = "Sample";

	/**
	 * Label of button to reverse on step.
	 */
	static final String BUTTON_PREV = "Previous";

	/**
	 * Label of button to show help.
	 */
	static final String BUTTON_HELP = "Help";

	/**
	 * Label of button to apply parameter settings.
	 */
	static final String BUTTON_APPLY = "Apply";

	/**
	 * Label of button to run in standalone mode.
	 */
	static final String BUTTON_STANDALONE = "Standalone";

	/**
	 * Label of button to reset to default parameter settings.
	 */
	static final String BUTTON_DEFAULT = "Default";

	/**
	 * 'Settings' button
	 */
	@UiField
	Button evoludoSettings;

	/**
	 * Toggle visibility of the text field {@link #evoludoCLOPanel} to view or
	 * modify the parameter settings.
	 *
	 * @param event the {@link ClickEvent} that was fired
	 * 
	 * @see #onSettingsTouchEnd(TouchEndEvent)
	 */
	@UiHandler("evoludoSettings")
	public void onSettingsClick(ClickEvent event) {
		toggleSettings();
	}

	/**
	 * Touch of parameter button started. Suppress default behaviour (prevents
	 * magnifying glass and text selection).
	 *
	 * @param event the {@link TouchStartEvent} that was fired
	 */
	@UiHandler("evoludoSettings")
	public void onSettingsTouchStart(TouchStartEvent event) {
		event.preventDefault();
	}

	/**
	 * Toggle visibility of {@link #evoludoCLOPanel} to view or modify the parameter
	 * settings. Touch of Settings-button ended.
	 *
	 * @param event the {@link TouchEndEvent} that was fired
	 * 
	 * @see #onSettingsClick(ClickEvent)
	 */
	@UiHandler("evoludoSettings")
	public void onSettingsTouchEnd(TouchEndEvent event) {
		toggleSettings();
	}

	/**
	 * Helper method to toggle the visibility of {@link #evoludoCLOPanel} to view or
	 * modify the parameter settings.
	 */
	void toggleSettings() {
		// toggle visibility of field for command line parameters
		evoludoCLOPanel.setVisible(!evoludoCLOPanel.isVisible());
		Scheduler.get().scheduleDeferred(() -> {
			evoludoLayout.onResize();
			NativeJS.focusOn(evoludoCLO.getElement());
		});
	}

	/**
	 * The 'Init'/'Reset' button
	 */
	@UiField
	Button evoludoInitReset;

	/**
	 * Initialize or reset EvoLudo model (action depends on title of button). Button
	 * changes to <code>Reset</code> if <code>Alt</code> key is pressed and reverts
	 * to <code>Init</code> after it is released.
	 *
	 * @param event the ClickEvent that was fired
	 * 
	 * @see #keyDownHandler(String)
	 * @see #keyUpHandler(String)
	 */
	@UiHandler("evoludoInitReset")
	public void onInitResetClick(ClickEvent event) {
		// init/reset simulation
		initReset();
	}

	/**
	 * On touch devices, the <code>Init</code> button changes to <code>Reset</code>
	 * for extended touches. This timer controls the delay before <code>Reset</code>
	 * is shown. Any other touch event during this period cancels the timer.
	 */
	protected Timer showAltTouchTimer = new Timer() {
		@Override
		public void run() {
			// show alt-button labels
			isAltDown = true;
			updateKeys();
		}
	};

	/**
	 * Schedule showing alternate/context buttons after a long touch and prevent
	 * default handling.
	 *
	 * @param event the touch event (must not be {@code null})
	 */
	private void scheduleAltButtons(TouchStartEvent event) {
		showAltTouchTimer.schedule(ContextMenu.LONG_TOUCH_TIME);
		event.preventDefault();
	}

	/**
	 * Touch of <code>Init</code> button started. Set timer for switching to
	 * <code>Reset</code>. Suppress default behaviour (prevents magnifying glass and
	 * text selection).
	 *
	 * @param event the TouchStartEvent that was fired
	 */
	@UiHandler("evoludoInitReset")
	public void onInitResetTouchStart(TouchStartEvent event) {
		scheduleAltButtons(event);
	}

	/**
	 * Touch of <code>Init</code> (or <code>Reset</code>) button ended. Take the
	 * appropriate action.
	 * 
	 * @param event the TouchEndEvent that was fired
	 */
	@UiHandler("evoludoInitReset")
	public void onInitResetTouchEnd(TouchEndEvent event) {
		showAltTouchTimer.cancel();
		initReset();
		isAltDown = false;
		updateKeys();
	}

	/**
	 * Initialize or reset EvoLudo model. If model is running wait until next update
	 * is completed to prevent unexpected side effects.
	 */
	protected void initReset() {
		String action = evoludoInitReset.getText();
		displayStatus(action + " pending. Waiting for engine to stop...");
		displayStatusThresholdLevel = Level.ALL.intValue();
		engine.requestAction(action.equals(BUTTON_RESET) ? PendingAction.RESET : PendingAction.INIT);
	}

	/**
	 * The 'Start'/'Stop' button
	 */
	@UiField
	Button evoludoStartStop;

	/**
	 * <code>Start</code>, <code>Stop</code> button clicked. Start EvoLudo model if
	 * not running and stop model if running. Button changes title accordingly.
	 *
	 * @param event the ClickEvent that was fired
	 */
	@UiHandler("evoludoStartStop")
	public void onStartStopClick(ClickEvent event) {
		engine.startStop();
	}

	/**
	 * Touch of <code>Start</code>, <code>Stop</code> button started. Suppress
	 * default behaviour (prevents magnifying glass and text selection).
	 * 
	 * @param event the TouchStartEvent that was fired
	 */
	@UiHandler("evoludoStartStop")
	public void onStartStopTouchStart(TouchStartEvent event) {
		event.preventDefault();
	}

	/**
	 * Touch of <code>Start</code>, <code>Stop</code> button ended. Start EvoLudo
	 * model if not running and stop model if running. Button changes title
	 * accordingly.
	 * 
	 * @param event the TouchEndEvent that was fired
	 */
	@UiHandler("evoludoStartStop")
	public void onStartStopTouchEnd(TouchEndEvent event) {
		engine.startStop();
	}

	/**
	 * The 'Step' button
	 */
	@UiField
	Button evoludoStep;

	/**
	 * <code>Next</code> button clicked. Advances the EvoLudo model by a single
	 * step. If the model is running, this is ignored.
	 *
	 * @param event the ClickEvent that was fired
	 */
	@UiHandler("evoludoStep")
	public void onStepClick(ClickEvent event) {
		prevNextDebug();
	}

	/**
	 * Touch of <code>Next</code> button started. Suppress default behaviour
	 * (prevents magnifying glass and text selection).
	 *
	 * @param event the TouchStartEvent that was fired
	 */
	@UiHandler("evoludoStep")
	public void onStepTouchStart(TouchStartEvent event) {
		scheduleAltButtons(event);
	}

	/**
	 * Touch of <code>Next</code> button ended. Advances the EvoLudo model by a
	 * single step. If the model is running, this is ignored.
	 *
	 * @param event the TouchEndEvent that was fired
	 */
	@UiHandler("evoludoStep")
	public void onStepTouchEnd(TouchEndEvent event) {
		showAltTouchTimer.cancel();
		prevNextDebug();
		isAltDown = false;
		updateKeys();
	}

	/**
	 * Helper method to advance the EvoLudo model by a single step. The label on the
	 * button determines the action:
	 * <dl>
	 * <dt>Step
	 * <dd>advances the model by a single step,
	 * <dt>Prev
	 * <dd>goes back by a single step and
	 * <dt>Debug
	 * <dd>advances the model by a single update.
	 * </dl>
	 */
	private void prevNextDebug() {
		String label = evoludoStep.getText();
		switch (label) {
			case BUTTON_SAMPLE:
			case BUTTON_STEP:
				engine.next();
				return;
			case BUTTON_PREV:
				engine.prev();
				return;
			case BUTTON_DEBUG:
				engine.debug();
				return;
			default:
		}
	}

	/**
	 * Panel containing all elements to change/view parameters. Visibility toggled
	 * with the 'Settings' button, {@link #evoludoSettings}. Initially invisible.
	 */
	@UiField
	FlowPanel evoludoCLOPanel;

	/**
	 * The text field containing the command line options for the EvoLudo model.
	 * Note, {@code TextArea} causes grief with ePubs in Apple Books.
	 */
	@UiField
	Label evoludoCLO;

	/**
	 * The 'Apply' button ('Standalone' in ePubs)
	 */
	@UiField
	Button evoludoApply;

	/**
	 * Apply command line parameters to EvoLudo model. For EvoLudo labs in the text
	 * flow of ePub's parameters cannot be changed and the title of the 'Apply'
	 * button is changed to 'Standalone' and opens a separate standalone EvoLudo
	 * lab, which permits parameter manipulations.
	 *
	 * @param event the ClickEvent that was fired
	 */
	@UiHandler("evoludoApply")
	public void onApplyClick(ClickEvent event) {
		if (evoludoApply.getText().equals(BUTTON_APPLY)) {
			engine.setCLO(evoludoCLO.getText().replace((char) 160, ' '));
			applyCLO();
			return;
		}
		// open standalone lab (ePubs only)
		RootPanel root = RootPanel.get(elementID);
		if (root == null) {
			logger.severe("Failed to open standalone lab (element '" + elementID + "' not found in DOM).");
			return;
		}
		String href = root.getElement().getAttribute("data-href").trim();
		if (href.isEmpty()) {
			logger.severe("Failed to open standalone lab (invalid reference).");
			return;
		}
		Window.Location.assign(href);
	}

	/**
	 * Helper structure to store the current state of the GUI.
	 */
	class GUIState {

		/**
		 * The active module.
		 */
		Module<?> module;

		/**
		 * The active model.
		 */
		Model model;

		/**
		 * The the configuration to restore, if any.
		 */
		Plist plist;

		/**
		 * The active view.
		 */
		AbstractView<?> view;

		/**
		 * The arguments for theactive view.
		 */
		String args;

		/**
		 * The flag to indicate whether to resume execution of the model.
		 */
		boolean resume;

		/**
		 * The number of issues that have occurred during parsing
		 */
		int issues;
	}

	/**
	 * Field to store the current state of the GUI while applying a new set of
	 * parameters.
	 */
	GUIState guiState = new GUIState();

	/**
	 * Process and apply the command line arguments stored in {@link #evoludoCLO}
	 * Loads new model (and unloads old one), if necessary, and loads/adjusts the
	 * data views as appropriate.
	 */
	public void applyCLO() {
		if (engine.isRunning()) {
			logger.warning("Cannot apply parameters while engine is running.");
			return;
		}
		displayStatusThresholdLevel = Level.ALL.intValue();
		guiState.resume = engine.isRunning() || engine.isSuspended();
		engine.setSuspended(false);
		guiState.model = engine.getModel();
		guiState.module = engine.getModule();
		guiState.view = activeView;
		// parseCLO() does the heavy lifting and configures the GUI
		guiState.issues = engine.parseCLO(this::configGUI);
		updateViews();
		// process (emulated) ePub restrictions - adds console if possible
		processEPubSettings();
		List<AbstractView<?>> availableViews = new ArrayList<>(activeViews.values());
		if (cloView.isSet()) {
			// the initialView specification (name or index) may be followed by a space and
			// a comma-separated list of view specific options
			String[] iv = initialView.split(" ", 2);
			// try to interpret first argument as name
			String name = iv[0].replace('_', ' ').trim();
			AbstractView<?> newView = null;
			for (AbstractView<?> view : availableViews) {
				if (view.getName().equals(name)) {
					newView = view;
					break;
				}
			}
			if (newView == null) {
				// try to interpret first argument as index
				int idx = 0;
				try {
					idx = CLOParser.parseInteger(iv[0]);
				} catch (NumberFormatException e) {
					// the argument is not a number, ignore and pick first view
					logger.warning("failed to set view '" + iv[0] + "' - using default.");
				}
				// Ensure idx is within bounds [1, av.length]
				if (idx < 1)
					idx = 1;
				int size = availableViews.size();
				if (idx > size)
					idx = size;
				if (!availableViews.isEmpty())
					newView = availableViews.get(idx - 1);
			}
			guiState.view = newView;
			guiState.args = iv.length > 1 ? iv[1].trim() : null;
		}
		if (guiState.view == null || !activeViews.containsValue(guiState.view)) {
			// initial load and view not set (or not found)
			// pick first available view (at least the console has to be in the list)
			guiState.view = availableViews.get(0);
		}
		if (guiState.view != activeView && activeView != null)
			activeView.deactivate();
		activeView = guiState.view;
		evoludoDeck.showWidget(activeView);
		// set selected item in view selector
		evoludoViews.setSelectedIndex(evoludoDeck.getWidgetIndex(activeView));
		if (guiState.issues > 1) {
			// single issue is already displayed in status line
			displayStatus("Multiple parsing problems (" + guiState.issues + ") - check log for details.",
					Level.WARNING.intValue() + 1);
			cloSize.parse();
		}
		evoludoSlider.setValue(engine.getDelay());
		revertCLO();
	}

	/**
	 * Helper method to update the views after the command line options have been
	 * applied. Ensures that all views are loaded and the GUI updated.
	 */
	private void configGUI() {
		// reset is required if module and/or model changed
		Module<?> newModule = engine.getModule();
		Model newModel = engine.getModel();
		if (newModule == null || newModule != guiState.module || newModel != guiState.model) {
			engine.modelReset(true);
			loadViews();
			// notify of reset (reset above was quiet because views may not have
			// been ready for notification)
			engine.fireModelReset();
		} else {
			if (!engine.paramsDidChange())
				// do not resume execution if reset was required (unless --run was specified)
				guiState.resume = false;
			loadViews();
			// resume running if no reset was necessary or --run was provided
			engine.setSuspended(guiState.resume || engine.isSuspended());
		}
		if (guiState.plist != null) {
			Plist state = guiState.plist;
			guiState.plist = null;
			displayStatus("Restoring state...");
			engine.restoreState(state);
		}
		activeView.parse(guiState.args);
		// view needs to be activated to set the mode of the model
		activeView.activate();
		if (engine.cloSnap.isSet()) {
			// --snap set
			Model activeModel = engine.getModel();
			double tStop = activeModel.getTimeStop();
			double nSamples = activeModel.getNSamples();
			switch (activeModel.getMode()) {
				case DYNAMICS:
				case STATISTICS_UPDATE:
					double deltat = (tStop - activeModel.getTime()) * (activeModel.isTimeReversed() ? -1.0 : 1.0);
					if (Double.isFinite(tStop) && deltat > 0.0) {
						// run to specified time
						if (Math.abs(deltat) < activeModel.getTimeStep())
							activeModel.setTimeStep(deltat);
						// start running - even without --run
						engine.setSuspended(true);
					}
					if (nSamples > 0.0)
						logger.warning("--samples found: wrong mode for statistics, use --view option.");
					break;
				case STATISTICS_SAMPLE:
					// run to specified sample count
					if (nSamples > activeModel.getNStatisticsSamples()) {
						// start running - even without --run
						engine.setSuspended(true);
					}
					if (Double.isFinite(tStop))
						logger.warning("--timestop found: wrong mode for dynamics, use --view option.");
					break;
				default:
			}
		}
		if (activeView.hasLayout() && engine.isSuspended())
			engine.run();
	}

	/**
	 * Helper method to update the views after the command line options have been
	 * applied. Ensures that all views are loaded, the correct sizes applied and the
	 * content reset.
	 * <p>
	 * <strong>Note:</strong> many views need to know their size to adjust, for
	 * exmple, buffer sizes or data storage for statistics. However, only views that
	 * have been added to the DOM have valid sizes. For this reason the dimensions
	 * of the active view are passed to all other views.
	 */
	private void loadViews() {
		// set of available views may have changed (e.g. statistics)
		int width = activeView.getOffsetWidth();
		int height = activeView.getOffsetHeight();
		for (AbstractView<?> view : activeViews.values()) {
			boolean loaded = view.load();
			if (view != activeView)
				view.setBounds(width, height);
			if (loaded)
				view.reset(false);
		}
		evoludoLayout.onResize();
	}

	/**
	 * CLOParser issues warnings for unknown options but do not throw them away (can
	 * be annoying when switching between models).
	 */
	protected void revertCLO() {
		evoludoCLO.setText(engine.getCLO());
	}

	/**
	 * The 'Default' button
	 */
	@UiField
	Button evoludoDefault;

	/**
	 * Discard current parameter settings and revert to default settings on initial
	 * launch of current model. For EvoLudo labs in the text flow of an ePub this
	 * button is disabled.
	 *
	 * @param event the ClickEvent that was fired
	 */
	@UiHandler("evoludoDefault")
	public void onDefaultClick(ClickEvent event) {
		Scheduler.get().scheduleDeferred(() -> {
			String clo = null;
			RootPanel root = RootPanel.get(elementID);
			if (root != null)
				clo = root.getElement().getAttribute(DOM_DATA_CLO).trim();
			if (clo == null || clo.isEmpty()) {
				revertCLO();
				return;
			}
			evoludoCLO.setText(clo);
			engine.setCLO(clo);
			applyCLO();
		});
	}

	/**
	 * The 'Help' button
	 */
	@UiField
	Button evoludoHelp;

	/**
	 * Displays a list and brief description of all parameters in the console,
	 * {@link #viewConsole}, including the default and current settings. For EvoLudo
	 * labs in the text flow of an ePub this button is disabled because the console
	 * is suppressed because scrolling is prevented by ePub reader.
	 *
	 * @param event the ClickEvent that was fired
	 */
	@UiHandler("evoludoHelp")
	public void onHelpClick(ClickEvent event) {
		showHelp();
	}

	/**
	 * Opening tag of pre-formatted text.
	 */
	static final String TAG_PRE_OPEN = "<pre>";

	/**
	 * Closing tag of pre-formatted text.
	 */
	static final String TAG_PRE_CLOSE = "</pre>";

	/**
	 * Show help in the console, {@link #viewConsole}.
	 */
	public void showHelp() {
		guiState.view = viewConsole;
		logger.info(TAG_PRE_OPEN + "EvoLudo (GWT):\n" + engine.getCLOHelp() + TAG_PRE_CLOSE);
		// no view may be available if things went wrong from the start...
		if (evoludoDeck.getWidgetCount() == 0) {
			evoludoDeck.add(viewConsole);
			evoludoDeck.showWidget(viewConsole);
			evoludoViews.clear();
			evoludoViews.addItem(viewConsole.getName());
			displayStatus("Set EvoLudo options to get started.");
			return;
		}
		changeViewTo(viewConsole);
	}

	/**
	 * Status line of EvoLudo model GUI.
	 */
	@UiField
	HTML evoludoStatus;

	/**
	 * Displays a message in the status line of the EvoLudo GUI with the default
	 * level {@link Level#INFO}.
	 * 
	 * @param msg the message to display
	 */
	public void displayStatus(String msg) {
		displayStatus(msg, Level.INFO.intValue());
	}

	/**
	 * Threshold level for overriding status message.
	 */
	private int displayStatusThresholdLevel = Level.ALL.intValue();

	/**
	 * Displays a message in the status line of the EvoLudo GUI with the severity
	 * <code>level</code>. Status messages are only overridden by subsequent
	 * messages with the same or higher levels. The threshold level for displaying
	 * messages is reset the next time the model is initialized or reset.
	 * 
	 * @param msg   the message to display
	 * @param level the severity of the message
	 */
	public void displayStatus(String msg, int level) {
		// during init/reset this should retain the (last) message with the highest
		// level. other messages should be ignored until after init/reset has completed
		// note: <= retains first rather than last message of particular level; this is
		// bad in most cases in particular for info messages
		// if (level <= displayStatusThresholdLevel)
		// note: debugging touch events on ipad can be painful; uncomment the following
		// to make the last message of the highest severity stick to allow minimal
		// reporting...
		if (level < displayStatusThresholdLevel)
			return;
		if (level >= Level.SEVERE.intValue()) {
			evoludoStatus.setHTML("<span style='color:red'><b>Error:</b> " + msg + "</span>");
			displayStatusThresholdLevel = level;
			return;
		}
		if (level >= Level.WARNING.intValue()) {
			evoludoStatus.setHTML("<span style='color:orange'><b>Warning:</b> " + msg + "</span>");
			displayStatusThresholdLevel = level;
			return;
		}
		displayStatusThresholdLevel = Math.max(level, displayStatusThresholdLevel);
		evoludoStatus.setHTML(msg);
	}

	/**
	 * Overlay for drag'n'drop operations
	 */
	@UiField
	HTML evoludoOverlay;

	/**
	 * Handler for drag'n'drop operation entering lab.
	 * <p>
	 * <strong>Note:</strong> apparently DragOverHandler is required for the
	 * DropHandler to work (no actions necessary, though).
	 *
	 * @param doe the DragOverEvent that was fired
	 */
	@UiHandler("evoludoOverlay")
	public void onDragOver(DragOverEvent doe) {
		// method needed for the DropHandler to work
	}

	/**
	 * Handler for drag'n'drop operation exiting lab.
	 *
	 * @param dle the DragLeaveEvent that was fired
	 */
	@UiHandler("evoludoOverlay")
	public void onDragLeave(DragLeaveEvent dle) {
		evoludoOverlay.setVisible(false);
	}

	/**
	 * Handler for dropping and restoring saved state.
	 *
	 * @param drop the DropEvent that was fired
	 */
	@UiHandler("evoludoOverlay")
	public void onDrop(DropEvent drop) {
		DataTransfer data = drop.getDataTransfer();
		drop.preventDefault();
		drop.stopPropagation();
		if (!NativeJS.isValidDnD(data)) {
			evoludoOverlay.setVisible(false);
			displayStatus("Drag'n'drop failed: Invalid state file.", Level.SEVERE.intValue());
			return;
		}
		// load decompressor, just in case
		loadZipJs();
		NativeJS.handleDnD(data, this);
		evoludoOverlay.setVisible(false);
		displayStatus("Processing drag'n'drop...");
	}

	/**
	 * Restore state of EvoLudo model from String <code>content</code>.
	 *
	 * @param filename (only for reference and reporting of success or failure)
	 * @param content  encoded state of EvoLudo model
	 */
	public void restoreFromString(String filename, String content) {
		Plist parsed = PlistParser.parse(content);
		if (parsed == null) {
			if (logger.isLoggable(Level.SEVERE))
				logger.severe("failed to parse contents of file '" + filename + "'.");
			return;
		}
		engine.setCLO((String) (parsed.get("CLO")));
		guiState.plist = parsed;
		applyCLO();
	}

	/**
	 * The helper variable to indicate JavaScript for dealing with zip archives has
	 * already been loaded.
	 */
	private static boolean hasZipJs = false;

	/**
	 * Ensures ZIP JavaScript is loaded exactly once across all instances.
	 * Thread-safe and idempotent.
	 */
	private static void loadZipJs() {
		if (!hasZipJs) {
			ScriptInjector.fromString(Resources.INSTANCE.zip().getText()).inject();
			hasZipJs = true;
		}
	}

	/**
	 * The index of the currently active view in the {@code activeViews} list (and
	 * the {@link #evoludoDeck} widget).
	 */
	private int viewIdx = 0;

	/**
	 * Key value for the {@code Alt} key.
	 */
	static final String KEY_ALT = "Alt";

	/**
	 * Key value for the {@code Shift} key.
	 */
	static final String KEY_SHIFT = "Shift";

	/**
	 * Key value for the {@code Enter} key.
	 */
	static final String KEY_ENTER = "Enter";

	/**
	 * Key value for the {@code Escape} key.
	 */
	static final String KEY_ESCAPE = "Escape";

	/**
	 * Key value for the {@code Backspace} key.
	 */
	static final String KEY_BACKSPACE = "Backspace";

	/**
	 * Key value for the {@code Delete} key.
	 */
	static final String KEY_DELETE = "Delete";

	/**
	 * Process {@code keyup} events to allow for <em>non-repeating</em> keyboard
	 * shortcuts. Use for events where repeating does not make sense, such as
	 * stopping a model or changing views. For repeating events, such as advancing
	 * the model by a single step, see {@link #keyDownHandler(String)}. The set of
	 * keys handled by {@code keyUpHandler} and {@code keyDownHandler} should be
	 * disjoint.
	 * 
	 * <h3>Implementation Notes:</h3>
	 * <ul>
	 * <li>{@code keyup} events are ignored if:
	 * <ol>
	 * <li>this EvoLudo model is not visible.
	 * <li>the command line options field {@link #evoludoCLO} has the focus. With
	 * the exception of {@code Shift-Enter} to apply the new settings to the model.
	 * <li>in an ePub, except when on a standalone page.
	 * </ol>
	 * <li>{@code keyup} events do not propagate further ({@code stopPropagation()}
	 * is always called).
	 * <li>returning {@code true} also prevents default behaviour (calls
	 * {@code preventDefault()}).
	 * </ul>
	 * {@code keyup} events are ignored if:
	 * <ul>
	 * <li>this EvoLudo model is not visible.
	 * <li>the command line options field {@link #evoludoCLO} has the focus. With
	 * the
	 * exception of {@code Shift-Enter}, which applies the new settings to the
	 * model.
	 * <li>when shown in an ePub, except when on a standalone page.
	 * <li>{@code keydown} event does not propagate further.
	 * <li>returning {@code true} also prevents default behaviour.
	 * </ul>
	 * <p>
	 * Global shortcuts provided for the following keys:
	 * <dl>
	 * <dt>{@code Alt}</dt>
	 * <dd>Toggles the mode for some buttons. For example to switch between
	 * {@code Init} and {@code Reset}.</dd>
	 * <dt>{@code Shift}</dt>
	 * <dd>Toggles the mode for some keys, see {@code Enter} below for an
	 * example.</dd>
	 * <dt>{@code 0}</dt>
	 * <dd>Toggles the visibility of the field to view and modify parameter
	 * settings.</dd>
	 * <dt>{@code 1-9}</dt>
	 * <dd>Quick view selector. Switches to data view with the selected index if it
	 * exists. {@code 1} is the first view etc.</dd>
	 * <dt>{@code Enter, Space}</dt>
	 * <dd>Starts (or stops) the current model. Note, {@code Shift-Enter} applies
	 * the new parameter settings if the field is visible and has the keyboard
	 * focus. Same as pressing the {@code Apply}-button.</dd>
	 * <dt>{@code Escape}</dt>
	 * <dd>Implements several functions depending on context:
	 * <ol>
	 * <li>Ignored in ePub.
	 * <li>Closes current EvoLudo simulation if running in a {@link EvoLudoTrigger}
	 * popup panel.
	 * <li>Stops any running model.
	 * <li>Initializes a model that is not running. Note, resets the model if
	 * {@code Alt} is pressed.
	 * </ol>
	 * </dd>
	 * <dt>{@code Backspace, Delete}</dt>
	 * <dd>Stops running models and initializes (resets if {@code Alt} is pressed)
	 * stopped models.</dd>
	 * <dt>{@code E}</dt>
	 * <dd>Export the current state of the model (as a modified {@code plist}).
	 * Ignored if model is running and in ePubs.</dd>
	 * <dt>{@code H}</dt>
	 * <dd>Show the help screen with brief descriptions of all parameters in the
	 * console view.</dd>
	 * </dl>
	 *
	 * @param key the string value of the released key
	 * @return {@code true} if key has been handled
	 * 
	 * @see #keyDownHandler(String)
	 * @see <a href=
	 *      "https://developer.mozilla.org/en-US/docs/Web/API/KeyboardEvent/key/Key_Values">Mozilla
	 *      Key Values</a>
	 * @see AbstractView#keyUpHandler(String) AbstractView.keyUpHandler(String) and
	 *      implementing classes for further keys that may be handled by the current
	 *      view
	 */
	public boolean keyUpHandler(String key) {
		// check if lab is visible
		if (!isShowing())
			return false;
		// process modifiers
		if (key.equals(KEY_ALT)) {
			// alt-key does not count as handled
			isAltDown = false;
			updateKeys();
		}
		if (key.equals(KEY_SHIFT))
			// shift-key does not count as handled
			isShiftDown = false;
		if (isEPub && !ePubStandalone)
			// in ePub text flow only "Alt" key is acceptable and does not count as handled
			return false;
		boolean cloActive = NativeJS.isElementActive(evoludoCLO.getElement());
		if (cloActive) {
			// focus is on command line options ignore keypress
			// except Shift-Enter, which applies the new settings
			if (isShiftDown && key.equals(KEY_ENTER)) {
				engine.setCLO(evoludoCLO.getText().replace((char) 160, ' '));
				applyCLO();
				return true;
			}
			// escape closes the settings field
			if (!key.equals(KEY_ESCAPE))
				return false;
		}
		// activeView may wish to handle key
		if (activeView.keyUpHandler(key))
			return true;
		switch (key) {
			case "0":
				// toggle settings
				toggleSettings();
				break;
			case "1":
			case "2":
			case "3":
			case "4":
			case "5":
			case "6":
			case "7":
			case "8":
			case "9":
				// quick view selector
				java.util.List<AbstractView<?>> allviews = new java.util.ArrayList<>(activeViews.values());
				int idx = CLOParser.parseInteger(key);
				if (idx <= allviews.size())
					changeViewTo(allviews.get(idx - 1));
				break;
			case "c":
				// toggle console
				allviews = new java.util.ArrayList<>(activeViews.values());
				if (evoludoDeck.getWidgetIndex(activeView) == allviews.size() - 1)
					// console is active, switch to previous view
					changeViewTo(allviews.get(viewIdx));
				else
					// switch to console
					changeViewTo(allviews.get(allviews.size() - 1));
				break;
			case KEY_ENTER:
			case " ":
				// start/stop simulation
				engine.startStop();
				break;
			case KEY_ESCAPE:
				// ignore "Escape" for ePubs
				if (isEPub)
					return false;
				// stop running simulation
				if (engine.isRunning()) {
					engine.stop();
					break;
				}
				if (evoludoCLOPanel.isVisible()) {
					toggleSettings();
					break;
				}
				// NOTE: non-printing keys (such as modifiers, delete, or escape) do not fire
				// 'keypress' event! only 'keydown' and 'keyup'.
				// - close overlay (if showing)
				// - stop simulations (if running)
				// - init/reset (if not running)
				if (popup != null && popup.isAttached()) {
					popup.close();
					break;
				}
				//$FALL-THROUGH$
			case KEY_BACKSPACE:
			case KEY_DELETE:
				// stop running simulation; init/reset if not running
				if (engine.isRunning())
					engine.stop();
				else
					initReset();
				break;
			case "E":
				// export state (suppress in ePub's)
				if (isEPub || engine.isRunning())
					return false;
				engine.exportState();
				break;
			case "F":
				// toggle fullscreen (if supported)
				if (!NativeJS.isFullscreenSupported())
					return false;
				engine.setFullscreen(!NativeJS.isFullscreen());
				break;
			case "H":
				// show help panel
				showHelp();
				break;
			default:
				return false;
		}
		return true;
	}

	/**
	 * Process {@code keydown} events to allow for <em>repeating</em> keyboard
	 * shortcuts. Use for events where repeating does make sense, such as advancing
	 * a model by a single step or changing the speed of the model execution by
	 * adjusting the delay between subsequent updates. For non-repeating events,
	 * such starting or stopping the model or changing the view, see
	 * {@link #keyUpHandler(String)}. The set of keys handled by
	 * {@code keyUpHandler} and {@code keyDownHandler} should be disjoint.
	 * 
	 * <h3>Implementation Notes:</h3>
	 * <ul>
	 * <li>{@code keydown} events are ignored if:
	 * <ol>
	 * <li>this EvoLudo model is not visible.
	 * <li>in an ePub, except when on a standalone page.
	 * </ol>
	 * <li>{@code keydown} events do not propagate further
	 * ({@code stopPropagation()} is always called).
	 * <li>returning {@code true} also prevents default behaviour (calls
	 * {@code preventDefault()}).
	 * </ul>
	 * <p>
	 * Global shortcuts provided for the following keys:
	 * <dl>
	 * <dt>{@code Alt}</dt>
	 * <dd>Toggles the mode for some buttons. For example to switch between
	 * {@code Init} and {@code Reset}.</dd>
	 * <dt>{@code Shift}</dt>
	 * <dd>Toggles the mode for some keys, see {@code Enter} below for an
	 * example.</dd>
	 * <dt>{@code ArrowRight, n}</dt>
	 * <dd>Advance the model by a single step. Same as pressing the
	 * {@code Step}-button.</dd>
	 * <dt>{@code +, =}</dt>
	 * <dd>Increase the speed of the model execution. Decrease the delay between
	 * updates. Moves the speed-slider to the right.</dd>
	 * <dt>{@code -}</dt>
	 * <dd>Decrease the speed of the model execution. Increase the delay between
	 * updates. Moves the speed-slider to the right.</dd>
	 * </dl>
	 *
	 * @param key the string value of the pressed key
	 * @return {@code true} if key has been handled
	 * 
	 * @see <a href=
	 *      "https://developer.mozilla.org/en-US/docs/Web/API/KeyboardEvent/key/Key_Values">Mozilla
	 *      Key Values</a>
	 * @see AbstractView#keyDownHandler(String) AbstractView.keyDownHandler(String)
	 *      and implementing classes for further keys that may be handled by the
	 *      current view
	 */
	public boolean keyDownHandler(String key) {
		// check if lab is visible
		if (!isShowing())
			return false;
		if (key.equals(KEY_ALT)) {
			// alt-key does not count as handled
			isAltDown = true;
			updateKeys();
		}
		if (key.equals(KEY_SHIFT))
			// shift-key does not count as handled
			isShiftDown = true;
		boolean cloActive = NativeJS.isElementActive(evoludoCLO.getElement());
		if (cloActive)
			// focus is on command line options ignore keypress
			return false;
		// activeView may wish to handle key
		if (activeView.keyDownHandler(key))
			return true;
		// activeView did not handle key
		switch (key) {
			case "ArrowRight":
			case "n":
				// advance single step
				engine.next();
				break;
			case "ArrowLeft":
			case "p":
				// backtrack single step (if model allows it)
				engine.prev();
				break;
			case "D":
				// perform single, verbose debug step
				engine.debug();
				break;
			case "+":
			case "=":
				// increase speed
				engine.decreaseDelay();
				evoludoSlider.setValue(engine.getDelay());
				break;
			case "-":
				// decrease speed
				engine.increaseDelay();
				evoludoSlider.setValue(engine.getDelay());
				break;
			// prevent side effects for special keys
			case KEY_ENTER:
				// case " ": // spacebar
				return true;
			default:
				return false;
		}
		return true;
	}

	/**
	 * The helper variable to indicate whether the Shift key is pressed.
	 */
	private boolean isShiftDown = false;

	/**
	 * The helper variable to indicate whether the Alt key is pressed.
	 */
	private boolean isAltDown = false;

	/**
	 * The Alt-key toggles the button labels for controlling the EvoLudo lab.
	 */
	private void updateKeys() {
		Model model = engine.getModel();
		boolean statistics = (model != null && model.getMode() == Mode.STATISTICS_SAMPLE);
		// only 'reset' in statistics mode
		if (statistics) {
			evoludoInitReset.setText(BUTTON_RESET);
			evoludoInitReset.setTitle("Reset statistics");
			evoludoStep.setText(BUTTON_SAMPLE);
			evoludoStep.setTitle("Calculate single sample");
			return;
		}
		if (isAltDown) {
			evoludoInitReset.setText(BUTTON_RESET);
			evoludoInitReset.setTitle("Initialize population and regenerate structure");
			if (engine.getModel().permitsTimeReversal()) {
				evoludoStep.setText(BUTTON_PREV);
				evoludoStep.setTitle("Backtrack single simulation step");
			}
			if (engine.getModel().permitsDebugStep()) {
				evoludoStep.setText(BUTTON_DEBUG);
				evoludoStep.setTitle("Single update event");
			}
			return;
		}
		evoludoInitReset.setText(BUTTON_INIT);
		evoludoInitReset.setTitle("Initialize population (preserve structure)");
		evoludoStep.setText(BUTTON_STEP);
		evoludoStep.setTitle("Advance single simulation step");
	}

	/**
	 * Marker element to indicate that a snapshot is ready.
	 *
	 * @see EvoLudoGWT#cloSnap
	 */
	private DivElement snapmarker = null;

	/**
	 * Prepare GUI to create a snapshot. Stops running model, updates GUI (buttons
	 * and view) and adds a marker element with ID <code>snapshot-ready</code> to
	 * DOM. This is used to control automated snapshots using
	 * <code>capture-website</code>.
	 * 
	 * @see <a href="https://github.com/sindresorhus/capture-website-cli"> Github:
	 *      capture-website-cli</a>
	 */
	public void snapshotReady() {
		if (snapmarker != null)
			return;
		activeView.update(true);
		// make sure GUI is in stopped state before taking the snapshot
		updateGUI();
		// add div to DOM to signal completion of layout for capture-website
		snapmarker = Document.get().createDivElement();
		snapmarker.setId("snapshot-ready");
		Document.get().getBody().appendChild(snapmarker);
	}

	/**
	 * Each EvoLudo model may entertain its own selection of views to visualize its
	 * data. Re-use currently active views if possible otherwise instantiate
	 * suitable views based on the features of the current model. Update the view
	 * selector accordingly.
	 * <p>
	 * <strong>Note:</strong> the console view is dealt with elsewhere (see
	 * {@link #processEPubSettings}).
	 */
	protected void updateViews() {
		HashMap<String, AbstractView<?>> oldViews = activeViews;
		activeViews = new HashMap<>();
		evoludoDeck.clear();

		Module<?> module = engine.getModule();
		if (module == null) {
			// no module loaded; show console only
			addView(viewConsole, oldViews);
			return;
		}

		Model model = engine.getModel();
		boolean isODESDE = isModelODESDE(model);

		// Populate views in separated concerns to reduce cognitive complexity
		addStrategyViews(module, isODESDE, oldViews);
		addFitnessViews(module, isODESDE, oldViews);
		addStructureViews(module, isODESDE, oldViews);
		addStatisticsViews(module, model, oldViews);

		// miscellaneous views (console etc.)
		addMiscViews(oldViews);

		// unload views that are no longer available
		for (AbstractView<?> view : oldViews.values())
			view.dispose();
		oldViews.clear();

		// update view selector
		evoludoViews.clear();
		for (AbstractView<?> view : activeViews.values())
			evoludoViews.addItem(view.getName());
	}

	/**
	 * Helper method to determine whether the model represents an ODE or SDE.
	 * <p>
	 * Returns {@code true} when {@code model} is non-{@code null} and its
	 * {@code Type}
	 * indicates an ordinary differential equation (ODE) or a stochastic
	 * differential
	 * equation (SDE). If {@code model} is {@code null} the method returns
	 * {@code false}.
	 * </p>
	 *
	 * @param model the model to inspect, may be {@code null}
	 * @return {@code true} if {@code model} is non-{@code null} and its type is ODE
	 *         or SDE;
	 *         {@code false} otherwise
	 */
	private boolean isModelODESDE(Model model) {
		if (model == null)
			return false;
		Type mt = model.getType();
		return mt.isODE() || mt.isSDE();
	}

	/**
	 * Helper method to add strategy related views to the application based on the
	 * features of the current module and model settings.
	 * 
	 * @param module   the module to inspect
	 * @param isODESDE whether the current model is an ODE or SDE
	 * @param oldViews map of existing views keyed by identifier; used to preserve
	 *                 or replace prior view instances
	 */
	private void addStrategyViews(Module<?> module, boolean isODESDE, HashMap<String, AbstractView<?>> oldViews) {
		if (module instanceof HasPop2D.Traits && !isODESDE)
			addView(new Pop2D(engine, Data.TRAIT), oldViews);
		if (isWebGLSupported && module instanceof HasPop3D.Traits && !isODESDE)
			addView(new Pop3D(engine, Data.TRAIT), oldViews);
		if (module instanceof HasPhase2D)
			addView(new Phase2D(engine), oldViews);
		if (module instanceof HasMean.Traits)
			addView(new Mean(engine, Data.TRAIT), oldViews);
		if (module instanceof HasS3)
			addView(new S3(engine), oldViews);
		if (module instanceof HasHistogram.Strategy)
			addView(new Histogram(engine, Data.TRAIT), oldViews);
		if (module instanceof HasDistribution.Strategy)
			addView(new Distribution(engine, Data.TRAIT), oldViews);
	}

	/**
	 * Helper method to add fitness related views to the application based on the
	 * features of the current module and model settings.
	 * 
	 * @param module   the module to inspect
	 * @param isODESDE whether the current model is an ODE or SDE
	 * @param oldViews map of existing views keyed by identifier; used to preserve
	 *                 or replace prior view instances
	 */
	private void addFitnessViews(Module<?> module, boolean isODESDE, HashMap<String, AbstractView<?>> oldViews) {
		if (module instanceof HasPop2D.Fitness && !isODESDE)
			addView(new Pop2D(engine, Data.FITNESS), oldViews);
		if (isWebGLSupported && module instanceof HasPop3D.Fitness && !isODESDE)
			addView(new Pop3D(engine, Data.FITNESS), oldViews);
		if (module instanceof HasMean.Fitness)
			addView(new Mean(engine, Data.FITNESS), oldViews);
		if (module instanceof HasHistogram.Fitness)
			addView(new Histogram(engine, Data.FITNESS), oldViews);
	}

	// Helper: add structure related views
	/**
	 * Helper method to add structure related views to the application based on the
	 * features of the current module and model settings.
	 * 
	 * @param module   the module to inspect
	 * @param isODESDE whether the current model is an ODE or SDE
	 * @param oldViews map of existing views keyed by identifier; used to preserve
	 *                 or replace prior view instances
	 */
	private void addStructureViews(Module<?> module, boolean isODESDE, HashMap<String, AbstractView<?>> oldViews) {
		if (module instanceof HasHistogram.Degree && !isODESDE)
			addView(new Histogram(engine, Data.DEGREE), oldViews);
	}

	/**
	 * Helper method to add statistics related views to the application based on the
	 * features of the current module and model settings.
	 * 
	 * @param module   the module to inspect
	 * @param model    the model to inspect
	 * @param oldViews map of existing views keyed by identifier; used to preserve
	 *                 or replace prior view instances
	 */
	private void addStatisticsViews(Module<?> module, Model model, HashMap<String, AbstractView<?>> oldViews) {
		if (model != null && model.permitsMode(Mode.STATISTICS_SAMPLE)) {
			if (module instanceof HasHistogram.StatisticsProbability)
				addView(new Histogram(engine, Data.STATISTICS_FIXATION_PROBABILITY), oldViews);
			if (module instanceof HasHistogram.StatisticsTime)
				addView(new Histogram(engine, Data.STATISTICS_FIXATION_TIME), oldViews);
		}
		if (model != null && model.permitsMode(Mode.STATISTICS_UPDATE)
				&& module instanceof HasHistogram.StatisticsStationary) {
			addView(new Histogram(engine, Data.STATISTICS_STATIONARY), oldViews);
		}
	}

	/**
	 * Helper method to add miscellaneous, non-core views to the application (for
	 * example the
	 * developer console). These views are optional and may be omitted in certain
	 * runtime modes (e.g. simulated ePub).
	 *
	 * @param oldViews map of existing views keyed by identifier; used to preserve
	 *                 or replace prior view instances
	 */
	private void addMiscViews(HashMap<String, AbstractView<?>> oldViews) {
		// note: console may be removed for (simulated) ePub modes
		addView(viewConsole, oldViews);
	}

	/**
	 * Helper method to add <code>view</code> to list of active views
	 * <code>activeViews</code>. If a view with the same name already exists in
	 * <code>oldViews</code> it is reused.
	 *
	 * @param view     to add to active list
	 * @param oldViews list of current views
	 */
	private void addView(AbstractView<?> view, HashMap<String, AbstractView<?>> oldViews) {
		String name = view.getName();
		if (oldViews.containsKey(name))
			view = oldViews.remove(name);
		activeViews.put(view.getName(), view);
		evoludoDeck.add(view);
	}

	/**
	 * Command line option to set the data view displayed in the GUI.
	 */
	public final CLOption cloView = new CLOption("view", "1", CLOCategory.GUI, null, new CLODelegate() {
		/**
		 * {@inheritDoc}
		 * <p>
		 * Set the initial view of the lab to {@code arg}. The view can be specified as
		 * an index referring to the list of available data views or as the title of the
		 * data view with spaces replaced by underscores, '_'.
		 */
		@Override
		public boolean parse(String arg) {
			initialView = arg;
			return true;
		}

		@Override
		public String getDescription() {
			StringBuilder descr = new StringBuilder("--view <v>      select view (v: index or title)");
			int idx = 1;
			for (AbstractView<?> view : activeViews.values()) {
				String keycode = "              " + (idx++) + ": ";
				int len = keycode.length();
				descr.append("\n").append(keycode.substring(len - 16, len)).append(view.getName());
			}
			return descr.toString();
		}
	});

	/**
	 * Command line option to set the size of the GUI or enter fullscreen.
	 */
	public final CLOption cloSize = new CLOption("size", "530,620", CLOCategory.GUI,
			"--size <w,h|fullscreen>  size of GUI, w: width, h: height", new CLODelegate() {
				/**
				 * {@inheritDoc}
				 * <p>
				 * Set the initial size of the lab to {@code arg}.
				 */
				@Override
				public boolean parse(String arg) {
					if (arg.startsWith("full")) {
						if (NativeJS.isFullscreenSupported()) {
							engine.setFullscreen(true);
							return true;
						}
						arg = cloSize.getDefault();
					}
					double[] dim = CLOParser.parseVector(arg);
					if (dim.length != 2)
						return false;
					// note: why do we need to set the initial size on the grandparent?
					fullscreenWidget.setSize((int) dim[0] + "px", (int) dim[1] + "px");
					return true;
				}
			});

	/**
	 * Command line option to mimic ePub modes and to disable device capabilities.
	 * <p>
	 * <strong>Note:</strong> for development/debugging only; should be disabled in
	 * production
	 */
	public final CLOption cloEmulate = new CLOption("emulate", null, CLOCategory.GUI,
			"--emulate <f1[,f2[...]]> list of GUI features to emulate:\n"
					+ "          epub: enable ePub mode\n"
					+ "    standalone: standalone ePub mode\n"
					+ "        nokeys: disable key events (if available)\n"
					+ "       nomouse: disable mouse events (if available)\n"
					+ "       notouch: disable touch events (if available)",
			new CLODelegate() {
				@Override
				public boolean parse(String arg) {
					// set/reset defaults
					detectGUIFeatures();
					if (arg == null)
						return true;
					// simulate ePub mode
					if (arg.contains("epub"))
						isEPub = true;
					// simulate standalone lab in ePub
					if (arg.contains("standalone")) {
						ePubStandalone = true;
						isEPub = true;
						ePubHasKeys = NativeJS.hasKeys();
						ePubHasMouse = NativeJS.hasMouse();
						ePubHasTouch = NativeJS.hasTouch();
					}
					// disable keys (if available)
					if (ePubHasKeys && arg.contains("nokeys"))
						ePubHasKeys = false;
					// disable mouse (if available)
					if (ePubHasMouse && arg.contains("nomouse"))
						ePubHasMouse = false;
					// disable touch (if available)
					if (ePubHasTouch && arg.contains("notouch"))
						ePubHasTouch = false;
					return true;
				}
			});

	@Override
	public void collectCLO(CLOParser parser) {
		parser.addCLO(cloEmulate);
		parser.addCLO(cloView);
		parser.addCLO(cloSize);
	}

	/**
	 * Use JSNI helper methods to query and detect features of the execution
	 * environment.
	 *
	 * @see NativeJS#ePubReaderHasFeature(String)
	 */
	public static void detectGUIFeatures() {
		// IMPORTANT: ibooks (desktop) returns ePubReader for standalone pages as well,
		// i.e. isEPub is true
		// however, ibooks (ios) does not report as an ePubReader for standalone pages,
		// i.e. isEPub is false
		ePubStandalone = (Document.get().getElementById("evoludo-standalone") != null);
		ePubHasKeys = NativeJS.ePubReaderHasFeature("keyboard-events");
		ePubHasMouse = NativeJS.ePubReaderHasFeature("mouse-events");
		ePubHasTouch = NativeJS.ePubReaderHasFeature("touch-events");
	}

	@Override
	public HandlerRegistration addFullscreenChangeHandler(FullscreenChangeHandler handler) {
		String eventname = NativeJS.fullscreenChangeEventName();
		NativeJS.addFullscreenChangeHandler(eventname, handler);
		return (HandlerRegistration) () -> NativeJS.removeFullscreenChangeHandler(eventname, handler);
	}

	// note: works in Safari and Chrome; some weird scaling issues remain with
	// Firefox for Chrome it is important to use {@code onfullscreenchange} and not
	// {@code onwebkitfullscreenchange}! the two do not seem to be identical
	@Override
	public void onFullscreenChange(FullscreenChangeEvent event) {
		if (NativeJS.isFullscreen())
			NativeJS.getFullscreenElement().addClassName("fullscreen");
		else
			evoludoPanel.getParent().getParent().getElement().removeClassName("fullscreen");
	}

	/**
	 * JSNI method: add global key event listeners for 'keydown', 'keyup', and
	 * 'keypress' events. All listeners are stored in the map
	 * <code>window.EvoLudoUtils.keyListeners</code>, which links listener functions
	 * to their respective controllers. The key handlers exposed through GWT appear
	 * to be more restricted and often lose focus. This somewhat brute force
	 * approach result in a much better GUI experience.
	 *
	 * @param evoludo GUI controller that handles the key events
	 */
	private final native void addKeyListener(EvoLudoWeb evoludo) /*-{
		// store key listener helpers in $wnd.EvoLudoUtils
		if (!$wnd.EvoLudoUtils) {
			$wnd.EvoLudoUtils = new Object();
			$wnd.EvoLudoUtils.keyListeners = new Map();
		}
		var id = evoludo.@org.evoludo.EvoLudoWeb::elementID;
		// check if key listeners already added
		if (!$wnd.EvoLudoUtils.keyListeners.get('keydown-' + id)) {
			$wnd.EvoLudoUtils.keyListeners
					.set(
							'keydown-' + id,
							function(event) {
								// console.log("event "+event.type+", key "+event.key+", code "+event.code);
								if (evoludo.@org.evoludo.EvoLudoWeb::keyDownHandler(Ljava/lang/String;)(event.key)) {
									event.preventDefault();
								}
								event.stopPropagation();
							});
			$wnd.EvoLudoUtils.keyListeners
					.set(
							'keyup-' + id,
							function(event) {
								// console.log("event "+event.type+", key "+event.key+", code "+event.code);
								if (evoludo.@org.evoludo.EvoLudoWeb::keyUpHandler(Ljava/lang/String;)(event.key)) {
									event.preventDefault();
								}
								event.stopPropagation();
							});
		}
		$wnd.addEventListener('keydown', $wnd.EvoLudoUtils.keyListeners
				.get('keydown-' + id), true);
		$wnd.addEventListener('keyup', $wnd.EvoLudoUtils.keyListeners
				.get('keyup-' + id), true);
	}-*/;

	/**
	 * JSNI method: remove all key event listeners that were registered for
	 * <code>evoludo</code>.
	 *
	 * @param evoludo GUI controller that handles the key events
	 */
	private final native void removeKeyListener(EvoLudoWeb evoludo) /*-{
		if (!$wnd.EvoLudoUtils)
			return;
		var id = evoludo.@org.evoludo.EvoLudoWeb::elementID;
		var key = $wnd.EvoLudoUtils.keyListeners.get('keydown-' + id);
		if (key)
			$wnd.removeEventListener('keydown', key, true);
		key = $wnd.EvoLudoUtils.keyListeners.get('keyup-' + id);
		if (key)
			$wnd.removeEventListener('keyup', key, true);
		key = $wnd.EvoLudoUtils.keyListeners.get('keypress-' + id);
		if (key)
			$wnd.removeEventListener('keypress', key, true);
	}-*/;

	/**
	 * Popup EvoLudo models (see {@link EvoLudoTrigger}) should attract all keyboard
	 * events. This is achieved by setting <code>keyListener</code> to the popup
	 * model.
	 */
	private static EvoLudoWeb keyListener = null;

	/**
	 * Expose method for creating EvoLudo labs (EvoLudoWeb objects) to javascript
	 *
	 * @param id  the ID of element for EvoLudo lab
	 * @param clo command line arguments of model
	 */
	public static void createEvoLudoLab(String id, String clo) {
		RootPanel root = RootPanel.get(id);
		if (root == null) {
			NativeJS.alert("Element with id '" + id + "' not found.");
			return;
		}
		root.add(new EvoLudoWeb(id, clo));
	}

	/**
	 * Insert EvoLudo model in DOM by replacing the <code>placeholder</code>
	 * element.
	 * 
	 * @param placeholder the placeholder element for the EvoLudo lab
	 * @param clo         command line arguments of model
	 */
	public static void insertEvoLudoLab(Element placeholder, String clo) {
		Element wrap = placeholder.getParentElement();
		String id = wrap.getAttribute("id");
		if (id == null || id.isEmpty()) {
			id = HTMLPanel.createUniqueId();
			wrap.setAttribute("id", id);
		}
		RootPanel root = RootPanel.get(id);
		wrap.removeAllChildren();
		wrap.addClassName("evoludo-simulation");
		EvoLudoWeb lab = new EvoLudoWeb(id, clo);
		root.add(lab);
	}

	/**
	 * Check if EvoLudo model is visible on screen. This is used to determine which
	 * model should receive and process key events.
	 * <p>
	 * <strong>Notes:</strong>
	 * <ol>
	 * <li>Does not work in ePub (always returns true; apparently impossible to
	 * determine currently visible page through javascript. thus, all labs (at least
	 * within chapter) would get key events. this is not only messy but even crashes
	 * iBooks when requesting full screen.</li>
	 * <li>If two EvoLudo models are simultaneously visible it is undefined which
	 * lab receives the 'keypress'</li>
	 * <li>Popup EvoLudo models acquire all key events (through the static
	 * {@link #keyListener}), regardless of whether other models are displayed
	 * underneath.</li>
	 * </ol>
	 *
	 * @return <code>true</code> if lab is visible on screen
	 */
	public boolean isShowing() {
		// skip test in ePubs - always evaluates to true...
		if (isEPub)
			return true;
		if (keyListener != null)
			// with key listener no further test necessary; return true if we are the
			// listener.
			return keyListener == this;
		// this works in browser but not in iBooks; Window always returns the entire
		// document (0, 0, fullwidth, fullheight)
		Rectangle2D view = new Rectangle2D(Window.getScrollLeft(), Window.getScrollTop(), Window.getClientWidth(),
				Window.getClientHeight());
		int xTop = getAbsoluteLeft();
		int yTop = getAbsoluteTop();
		if (view.contains(xTop, yTop))
			return true;
		return view.contains((double) xTop + getOffsetWidth(), (double) yTop + getOffsetHeight());
	}

	/**
	 * Fighting Apple Books constraints: Insert EvoLudo model in DOM by replacing
	 * the <code>placeholder</code> element with an iframe. This allows to load the
	 * lab as expected but all interactivity is disabled.
	 * 
	 * @param placeholder the placeholder element for the EvoLudo lab
	 * @param url         command line arguments of model
	 */
	public static void insertEPubEvoLudoLab(Element placeholder, String url) {
		Element wrap = placeholder.getParentElement();
		String id = wrap.getAttribute("id");
		if (id == null || id.isEmpty()) {
			id = HTMLPanel.createUniqueId();
			wrap.setAttribute("id", id);
		}
		RootPanel root = RootPanel.get(id);
		wrap.removeAllChildren();
		wrap.addClassName("evoludo-simulation");
		Frame lab = new Frame(url);
		root.add(lab);
	}

	/**
	 * JSNI method: expose method for inserting EvoLudo models into ePub to
	 * javascript.
	 */
	public static native void exportInsertEPubEvoLudoLab()
	/*-{
		$wnd.insertEPubEvoLudoLab = $entry(@org.evoludo.EvoLudoWeb::insertEPubEvoLudoLab(Lcom/google/gwt/dom/client/Element;Ljava/lang/String;));
	}-*/;

	/**
	 * JSNI method: expose method for inserting EvoLudo models into HTML to
	 * javascript.
	 */
	public static native void exportInsertEvoLudoLab()
	/*-{
		$wnd.insertEvoLudoLab = $entry(@org.evoludo.EvoLudoWeb::insertEvoLudoLab(Lcom/google/gwt/dom/client/Element;Ljava/lang/String;));
	}-*/;

	/**
	 * JSNI method: create EvoLudo labs directly from javascript.
	 */
	public static native void exportCreateEvoLudoLab()
	/*-{
		$wnd.createEvoLudoLab = $entry(@org.evoludo.EvoLudoWeb::createEvoLudoLab(Ljava/lang/String;Ljava/lang/String;));
	}-*/;

	/**
	 * Expose method for creating EvoLudoTriggers to javascript.
	 *
	 * @param id the ID of element for EvoLudo trigger button
	 */
	public static void createEvoLudoTrigger(String id) {
		RootPanel root = RootPanel.get(id);
		if (root == null) {
			NativeJS.alert("Element with id '" + id + "' not found.");
			return;
		}
		root.add(new EvoLudoTrigger(id));
	}

	/**
	 * JSNI method: create EvoLudo lab trigger buttons directly from javascript.
	 */
	public static native void exportCreateEvoLudoTrigger()
	/*-{
		$wnd.createEvoLudoTrigger = $entry(@org.evoludo.EvoLudoWeb::createEvoLudoTrigger(Ljava/lang/String;));
	}-*/;

	/**
	 * Helper method to deal with ePub specifics. If EvoLudo lab is in flow of text
	 * then the console is removed (or added if EvoLudo model is on standalone ePub
	 * page or in browser), drag'n'drop to restore states and setting of parameters
	 * are disabled (or enabled otherwise).
	 */
	protected void processEPubSettings() {
		// nonlinear content in Apple Books (i.e. all interactive labs) do not report
		// as ePubs on iOS (at least for iPad) but as expected on macOS. On both
		// platforms TextFields are disabled through shadow DOM.
		boolean editCLO = !isEPub || ePubStandalone;
		updateConsoleView(editCLO);
		updateCLOControls(editCLO);
		updateDropHandlers(editCLO);
	}

	private void updateConsoleView(boolean editCLO) {
		if (editCLO) {
			// ensure console is present
			if (activeViews.put(viewConsole.getName(), viewConsole) == null) {
				viewConsole.load();
				evoludoViews.addItem(viewConsole.getName());
			}
			return;
		}
		// ensure console is absent
		if (activeViews.remove(viewConsole.getName()) != null) {
			viewConsole.unload();
			for (int n = evoludoViews.getItemCount() - 1; n >= 0; n--) {
				if (evoludoViews.getItemText(n).equals(viewConsole.getName())) {
					evoludoViews.removeItem(n);
					break;
				}
			}
		}
	}

	private void updateCLOControls(boolean editCLO) {
		evoludoCLO.setTitle(editCLO ? "Specify simulation parameters"
				: "Current simulation parameters (open standalone lab to modify)");
		evoludoApply.setText(editCLO ? BUTTON_APPLY : BUTTON_STANDALONE);
		evoludoApply.setTitle(editCLO ? "Apply parameters" : "Open standalone lab");
		evoludoDefault.setEnabled(editCLO);
		evoludoHelp.setEnabled(editCLO);
		evoludoSettings.setTitle(editCLO ? "Change simulation parameters" : "Review simulation parameters");
		logEvoHandler.setLevel(editCLO ? logger.getLevel() : Level.OFF);
	}

	private void updateDropHandlers(boolean editCLO) {
		if (!editCLO)
			return;
		if (dragEnterHandler == null)
			dragEnterHandler = addDomHandler((DragEnterEvent event) -> {
				if (engine.isRunning()) {
					evoludoOverlay.setVisible(false);
					displayStatus("Stop lab for drag'n'drop restore.", Level.WARNING.intValue());
					return;
				}
				evoludoOverlay.setVisible(true);
			}, DragEnterEvent.getType());
		if (dragLeaveHandler == null)
			dragLeaveHandler = addDomHandler((DragLeaveHandler) (event -> {
				if (!evoludoOverlay.isVisible() && displayStatusThresholdLevel <= Level.WARNING.intValue())
					displayStatusThresholdLevel = Level.ALL.intValue();
			}), DragLeaveEvent.getType());
	}

	/**
	 * Log GWT features and GUI specifics.
	 */
	void logFeatures() {
		if (!logger.isLoggable(Level.INFO))
			return;
		StringBuilder sb = new StringBuilder("GUI Version: ");
		sb.append(GWT.getVersion());
		sb.append("\nGUI features: ");
		sb.append(NativeJS.isWebGLSupported() ? "WebGL " : "");
		sb.append(NativeJS.isHTML() ? "HTML " : "XML ");
		sb.append(NativeJS.hasKeys() ? "keyboard " : "");
		sb.append(NativeJS.hasMouse() ? "mouse " : "");
		sb.append(NativeJS.hasTouch() ? "touch " : "");
		if (NativeJS.isEPub()) {
			sb.append("ePub (");
			sb.append(ePubHasKeys ? "keyboard " : "");
			sb.append(ePubHasMouse ? "mouse " : "");
			sb.append(ePubHasTouch ? "touch " : "");
			sb.append(")");
		}
		logger.info(sb.toString());
	}

	/**
	 * Indicator whether display system supports WebGL to display population
	 * structures in 3D.
	 */
	private final boolean isWebGLSupported;

	/**
	 * Custom handler for logging system. Redirects notifications to EvoLudo console
	 * with minimal formatting (preserve {@code &lt;pre&gt;}-formatted string for
	 * help message) and encoding (to avoid the wrath of XHTML).
	 */
	public class EvoLogHandler extends Handler {

		/**
		 * The GUI console (or null).
		 */
		Console console;

		/**
		 * Construct new log handler that reports to the EvoLudo console (if one is
		 * present) and the EvoLudo status line.
		 * 
		 * @param console for reporting log records
		 * 
		 * @see Console
		 * @see EvoLudoWeb#displayStatus(String, int)
		 */
		public EvoLogHandler(Console console) {
			this.console = console;
		}

		/**
		 * {@inheritDoc}
		 * <p>
		 * If severity of notification is {@link Level#WARNING} or higher, the message
		 * is also shown in the status line of the GUI. Ignore log request if no console
		 * provided.
		 */
		@Override
		public void publish(LogRecord rec) {
			Level rl = rec.getLevel();
			int level = rl.intValue();
			if (level >= Level.WARNING.intValue()) {
				EvoLudoWeb.this.displayStatus(rec.getMessage(), level);
			}
			if (console == null)
				return;
			String log = rec.getMessage();
			int preBegin;
			int preEnd = 0;
			// encode all text inside <pre>...</pre> tags
			while ((preBegin = log.indexOf(TAG_PRE_OPEN, preEnd)) >= 0) {
				preBegin += TAG_PRE_OPEN.length();
				// preformatted component found, preserve formatting
				preEnd = log.indexOf(TAG_PRE_CLOSE);
				if (preEnd < 0)
					preEnd = log.length();
				String pre = log.substring(preBegin, preEnd);
				log = log.substring(0, preBegin) + XMLCoder.encode(pre)
						+ log.substring(preEnd + TAG_PRE_CLOSE.length());
			}
			// TODO: check if HTML tags are allowed in epubs
			// if (engine.isEPub)
			// log = XMLCoder.encode(log);
			console.log(rl, log);
		}

		/**
		 * Nothing to flush in GWT.
		 */
		@Override
		public void flush() {
			// ignore - nothing to flush
		}

		/**
		 * Nothing to close in GWT.
		 */
		@Override
		public void close() throws SecurityException {
			// ignore - nothing to close
		}
	}
}
