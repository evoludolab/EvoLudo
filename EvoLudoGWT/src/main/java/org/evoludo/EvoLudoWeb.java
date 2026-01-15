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
import org.evoludo.simulator.models.Mode;
import org.evoludo.simulator.models.Model;
import org.evoludo.simulator.models.ChangeListener.PendingAction;
import org.evoludo.simulator.modules.Module;
import org.evoludo.simulator.ui.SettingsController;
import org.evoludo.simulator.ui.FSController;
import org.evoludo.simulator.ui.KeyHandler;
import org.evoludo.simulator.ui.ViewController;
import org.evoludo.simulator.ui.WebListener;
import org.evoludo.simulator.views.AbstractView;
import org.evoludo.simulator.views.Console;
import org.evoludo.ui.ContextMenu;
import org.evoludo.ui.InputEvent;
import org.evoludo.ui.Slider;
import org.evoludo.util.CLOParser;
import org.evoludo.util.CLOProvider;
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
import com.google.gwt.event.dom.client.BlurEvent;
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
import com.google.gwt.event.dom.client.FocusEvent;
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
 * EvoLudoWeb keeps a DeckLayoutPanel (evoludoDeck) that displays exactly one
 * view at a time, while {@link ViewController} manages the available views and
 * selection state. Views are created or reused based on module capabilities and
 * model type. Views are responsible for rendering, while EvoLudoWeb
 * orchestrates loading, sizing, activation, deactivation, and disposal. The
 * Console view is treated specially and may be omitted in restricted ePub
 * modes.
 * </p>
 *
 * <h3>Input Handling and Shortcuts</h3>
 * <p>
 * Global keyboard handling is implemented through {@link KeyHandler} to
 * provide reliable shortcuts that control the engine and UI. The controller
 * distinguishes between repeating actions (handled on key down) and
 * non-repeating actions (handled on key up) while JSNI routines install
 * window-level listeners that delegate to the controller. Touch interaction
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
 * <p>
 * <strong>Implementation notes:</strong> this class is designed to be a
 * single, self-contained controller per DOM container element. It expects to
 * run within the GWT environment and to co-operate with EvoLudoGWT, the
 * Model/Module abstraction, and various AbstractView subclasses. It uses JSNI
 * for some browser integration and therefore relies on the presence of the
 * window object and typical browser DOM APIs.
 * </p>
 *
 * @see EvoLudoGWT
 * @see AbstractView
 * @see Console
 */
public class EvoLudoWeb extends Composite
		implements CLOProvider, EntryPoint {

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
	 * Controller for ePub specific behaviour and emulation.
	 */
	private SettingsController settingsController;

	/**
	 * Controller. Manages the interface with the outside world.
	 */
	EvoLudoGWT engine;

	/**
	 * Helper that centralizes keyboard handling.
	 */
	private final KeyHandler keyController = new KeyHandler(this);

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
	 * ID of element in DOM that contains the EvoLudo lab.
	 */
	String elementID = "EvoLudoWeb";

	/**
	 * Controller managing all data views and the {@link #evoludoDeck}.
	 */
	private ViewController viewController;

	/**
	 * Controller managing Lifecycle, Run, Sample, and Change events.
	 */
	WebListener webListener;

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
	 * Controller handling fullscreen and --size option.
	 */
	private FSController fsController;

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
			return;
		}
		// set command line options
		evoludoCLO.getElement().setAttribute("contenteditable", "true");
		evoludoCLO.setText(clo != null ? clo : "");
		evoludoCLO.addDomHandler(event -> keyController.onCLOFocusGained(), FocusEvent.getType());
		evoludoCLO.addDomHandler(event -> keyController.onCLOFocusLost(), BlurEvent.getType());

		// layout speed controls - GWT has no slider...
		// but we can construct one from HTML5!
		evoludoSlider.setRange((int) EvoLudo.DELAY_MAX, (int) EvoLudo.DELAY_MIN);
		evoludoSlider.setLogBase(1.1);
		// note: the {}-place holders interfere with UiBinder...
		evoludoSlider.setTitle("Set delay between updates ({max} - {min}msec); now at {value}msec");
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
		settingsController = new SettingsController(evoludoCLO, evoludoApply, evoludoDefault,
				evoludoHelp, evoludoSettings);
		setupEngine();
		setupLogger(engine);
		setupConsole(logger);
		logFeatures();
		viewController = new ViewController(engine, evoludoDeck, evoludoViews, viewConsole, this::updateGUI);
		webListener = new WebListener(this, engine, keyController);

		// now evoludoPanel is attached and we can set the grandparent as the
		// fullscreen element
		fsController = new FSController(engine, (evoludoPanel.getParent().getParent()));

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
		engine.setCLO(evoludoCLO.getText().trim());
	}

	/**
	 * Reset the current view selection to the default entry.
	 */
	public void resetViewSelection() {
		viewController.resetSelection();
	}

	/**
	 * Clear the command line option field in the GUI.
	 */
	public void clearCommandLineOptions() {
		evoludoCLO.setText("");
	}

	/**
	 * Restore the default log threshold (display all severities).
	 */
	public void resetStatusThreshold() {
		displayStatusThresholdLevel = Level.ALL.intValue();
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
		ConsoleLogHandler logHandler = new ConsoleLogHandler();
		logHandler.setFormatter(new XMLLogFormatter(true,
				(NativeJS.getEPubReader() != null) || NativeJS.isHTML()));
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
		webListener.unload();
		super.onUnload();
	}

	/**
	 * Update GUI for running/stopped model.
	 */
	public void updateGUI() {
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
	public void updateStatus() {
		Model model = engine.getModel();
		// do not force retrieving status if engine is running
		AbstractView<?> view = viewController.getActiveView();
		if (view == null)
			return;
		String s = view.getStatus(!engine.isRunning());
		if (s == null)
			s = model.getStatus();
		displayStatus(s);
		updatetime = Duration.currentTimeMillis();
	}

	/**
	 * Helper method to update counter of GUI.
	 */
	public void updateCounter() {
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
		changeViewTo(viewController.getViewByName(evoludoViews.getSelectedItemText()));
		evoludoViews.setFocus(false);
	}

	/**
	 * Change view of EvoLudo model data. This helper method is called when the user
	 * selects a new view with the popup list {@link #evoludoViews} or when a
	 * particular view is requested through command line options (see
	 * {@link ViewController#getCloView()}).
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
	 * {@link ViewController#getCloView()}). The view is re-activated if
	 * {@code force} is {@code true}
	 * and activation is skipped if the view didn't change and {@code force} is
	 * {@code false}.
	 *
	 * @param newView new view of model data to display
	 * @param force   if {@code true} the view is re-activated even if it didn't
	 *                change
	 */
	protected void changeViewTo(AbstractView<?> newView, boolean force) {
		viewController.changeViewTo(newView, force);
	}

	/**
	 * Return the list of all active views.
	 * 
	 * @return the list of all active views (console included)
	 */
	@SuppressWarnings("java:S1452") // views have unknown generic parameters
	public List<AbstractView<?>> getActiveViews() {
		return viewController.getActiveViews();
	}

	/**
	 * Return the currently active view.
	 * 
	 * @return the active view.
	 */
	@SuppressWarnings("java:S1452") // views have unknown generic parameters
	public AbstractView<?> getActiveView() {
		return viewController.getActiveView();
	}

	/**
	 * Return the index of the currently active view in the list of active views.
	 * 
	 * @return the index of the active view inside the deck or {@code -1}.
	 */
	public int getActiveViewIndex() {
		return viewController.getActiveViewIndex();
	}

	/**
	 * Switches to the provided view.
	 *
	 * @param view the new view to display
	 */
	public void changeView(AbstractView<?> view) {
		changeViewTo(view);
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
	public void toggleSettings() {
		// toggle visibility of field for command line parameters
		evoludoCLOPanel.setVisible(!evoludoCLOPanel.isVisible());
		Scheduler.get().scheduleDeferred(() -> {
			evoludoLayout.onResize();
			NativeJS.focusOn(evoludoCLO.getElement());
		});
	}

	/**
	 * Obtain the DOM element that backs the CLO label for direct manipulation.
	 * 
	 * @return the DOM element that backs the CLO label
	 */
	public Element getCLOElement() {
		return evoludoCLO.getElement();
	}

	/**
	 * Applies the text currently in the CLO field to the engine.
	 */
	public void applyCLOFromField() {
		engine.setCLO(evoludoCLO.getText().replace((char) 160, ' '));
		applyCLO();
	}

	/**
	 * Check if this lab is displayed as standalone page in ePub.
	 * 
	 * @return {@code true} if a standalone
	 */
	public boolean isEPubStandalone() {
		return settingsController.isStandalone();
	}

	/**
	 * Check if this lab is running inside an ePub reader.
	 * 
	 * @return {@code true} when running inside an ePub reader.
	 */
	public boolean isEPub() {
		return settingsController.isEPub();
	}

	/**
	 * Check if the CLO settings panel is visible in the GUI.
	 * 
	 * @return {@code true} if settings panel visible
	 */
	public boolean isCLOPanelVisible() {
		return evoludoCLOPanel.isVisible();
	}

	/**
	 * Closes the overlay containing this EvoLudo lab, if any.
	 * 
	 * @return {@code true} if lab was closed
	 */
	public boolean closePopup() {
		if (popup != null && popup.isAttached()) {
			popup.close();
			return true;
		}
		return false;
	}

	/**
	 * Check if this lab owns a popup overlay.
	 * 
	 * @return {@code true} if lab shown on overlay
	 */
	public boolean hasPopup() {
		return popup != null;
	}

	/**
	 * Return the engine that controls the EvoLudo modules.
	 * 
	 * @return the engine
	 */
	public EvoLudoGWT getEngine() {
		return engine;
	}

	/**
	 * Syncs the delay slider with the engine's current delay.
	 */
	public void syncDelaySlider() {
		evoludoSlider.setValue(engine.getDelay());
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
			keyController.showAltModeFromTouch();
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
		keyController.hideAltModeFromTouch();
	}

	/**
	 * Initialize or reset EvoLudo model. If model is running wait until next update
	 * is completed to prevent unexpected side effects.
	 */
	public void initReset() {
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
		if (engine.isRunning() || SettingsController.isReady())
			engine.startStop();
		else
			logger.warning("another EvoLudo instance is already running!");
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
		keyController.hideAltModeFromTouch();
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
		if (evoludoApply.getText().equals(SettingsController.BUTTON_APPLY)) {
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
		 * Create an empty GUI state snapshot.
		 */
		GUIState() {
		}

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
		guiState.view = viewController.getActiveView();
		// parseCLO() does the heavy lifting and configures the GUI
		guiState.issues = engine.parseCLO(this::configGUI);
		// clearing evoludoDeck calls onUnload on all views - keep them for now
		// to allow recycling if module/model did not change; rely on presence of
		// console to trigger configGUI() when ready
		viewController.prepareForCLO();
		if (guiState.issues > 1) {
			// single issue is already displayed in status line
			displayStatus("Multiple parsing problems (" + guiState.issues + ") - check log for details.",
					Level.WARNING.intValue() + 1);
			fsController.parseSize();
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
		boolean moduleChanged = (newModule == null || newModule != guiState.module || newModel != guiState.model);
		if (moduleChanged) {
			engine.modelReset(true);
			loadViews(false);
			// notify of reset (reset above was quiet because views may not have
			// been ready for notification)
			engine.fireModelReset();
		} else {
			boolean didReset = engine.paramsDidChange();
			loadViews(didReset);
			if (didReset) {
				// do not resume execution if reset was required (unless --run was specified)
				guiState.resume = false;
			}
			// resume running if no reset was necessary or --run was provided
			engine.setSuspended(guiState.resume || engine.isSuspended());
		}
		if (viewController.isInitialViewSet())
			guiState.view = viewController.resolveInitialView(logger);
		else if ((moduleChanged && guiState.view == viewController.getConsoleView())
				|| !viewController.containsView(guiState.view))
			guiState.view = null;
		if (guiState.view == null)
			guiState.view = viewController.getFirstView();
		changeViewTo(guiState.view, true);
		if (guiState.plist != null) {
			Plist state = guiState.plist;
			guiState.plist = null;
			displayStatus("Restoring state...");
			engine.restoreState(state);
			updateCounter();
		}
		AbstractView<?> currentView = viewController.getActiveView();
		if (currentView != null) {
			currentView.parse();
			// view needs to be activated to set the mode of the model
			currentView.activate();
		}
		processCLOSnap();
		if (currentView != null && currentView.hasLayout() && engine.isSuspended())
			engine.run();
	}

	/**
	 * Process the command line options for snap execution,
	 * {@link EvoLudoGWT#cloSnap}, and start the model accordingly.
	 */
	private void processCLOSnap() {
		if (!engine.cloSnap.isSet())
			return;
		// --snap set
		Model activeModel = engine.getModel();
		switch (activeModel.getMode()) {
			case DYNAMICS:
			case STATISTICS_UPDATE:
				snapDynamics(activeModel);
				break;
			case STATISTICS_SAMPLE:
				snapSamples(activeModel);
				break;
			default:
		}
	}

	/**
	 * Helper method to process snap for dynamics mode.
	 * 
	 * @param activeModel the active model
	 */
	private void snapDynamics(Model activeModel) {
		double tStop = activeModel.getTimeStop();
		double nSamples = activeModel.getNSamples();
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
	}

	/**
	 * Helper method to process snap for statistics sample mode.
	 * 
	 * @param activeModel the active model
	 */
	private void snapSamples(Model activeModel) {
		double tStop = activeModel.getTimeStop();
		double nSamples = activeModel.getNSamples();
		// run to specified sample count
		if (nSamples > activeModel.getNStatisticsSamples()) {
			// start running - even without --run
			engine.setSuspended(true);
		}
		if (Double.isFinite(tStop))
			logger.warning("--timestop found: wrong mode for dynamics, use --view option.");
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
	private void loadViews(boolean resetViews) {
		viewController.refreshViews();
		AbstractView<?> anchorView = viewController.getActiveView();
		if (anchorView == null)
			anchorView = viewController.getFirstView();
		if (anchorView == null)
			return;
		int width = anchorView.getOffsetWidth();
		int height = anchorView.getOffsetHeight();
		for (AbstractView<?> view : viewController.getActiveViews()) {
			boolean loaded = view.load();
			if (view != anchorView)
				view.setBounds(width, height);
			if (loaded) {
				if (resetViews)
					view.modelDidReset();
				else
					view.reset(false);
			}
		}
		evoludoLayout.onResize();
		updateDropHandlers();
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
		// note: debugging touch events on ipad can be painful; change '<' to '<=' in
		// the following to make the last message of the highest severity stick to allow
		// minimal reporting...
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
		if (parsed == null || !parsed.containsKey("CLO")) {
			if (logger.isLoggable(Level.SEVERE))
				logger.severe("failed to parse contents of file '" + filename + "'.");
			return;
		}
		engine.unloadModule();
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
	 * The Alt-key toggles the button labels for controlling the EvoLudo lab.
	 */
	public void updateKeys() {
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
		if (keyController.isAltDown()) {
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
		AbstractView<?> view = viewController.getActiveView();
		if (view == null)
			return;
		view.update(true);
		// make sure GUI is in stopped state before taking the snapshot
		updateGUI();
		// add div to DOM to signal completion of layout for capture-website
		snapmarker = Document.get().createDivElement();
		snapmarker.setId("snapshot-ready");
		Document.get().getBody().appendChild(snapmarker);
	}

	/**
	 * Remove the temporary DOM marker indicating that a snapshot is ready.
	 */
	public void clearSnapshotMarker() {
		if (snapmarker == null)
			return;
		Document.get().getBody().removeChild(snapmarker);
		snapmarker = null;
	}

	@Override
	public void collectCLO(CLOParser parser) {
		parser.addCLO(settingsController.getCloEmulate());
		parser.addCLO(viewController.getCloView());
		parser.addCLO(fsController.getCloSize());
	}

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
	 * <li>Popup EvoLudo models acquire all key events by activating their
	 * {@link org.evoludo.simulator.ui.KeyHandler}.</li>
	 * </ol>
	 *
	 * @return <code>true</code> if lab is visible on screen
	 */
	public boolean isShowing() {
		// skip test in ePubs - always evaluates to true...
		if (settingsController.isEPub())
			return true;
		if (keyController.isActive())
			return true;
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
		// TODO: check/resolve problem with console in ePubs - should be ok
		// if content properly XML encoded...
		// nonlinear content in Apple Books (i.e. all interactive labs) do not report
		// as ePubs on iOS (at least for iPad) but as expected on macOS. On both
		// platforms TextFields are disabled through shadow DOM.
		boolean editCLO = settingsController.applyUiToggles(this::updateDropHandlers);
		if (logEvoHandler != null)
			logEvoHandler.setLevel(editCLO ? logger.getLevel() : Level.OFF);
	}

	/**
	 * Update drag-and-drop handlers after UI toggles changed the layout.
	 */
	private void updateDropHandlers() {
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
			sb.append(settingsController.hasKeys() ? "keyboard " : "");
			sb.append(settingsController.hasMouse() ? "mouse " : "");
			sb.append(settingsController.hasTouch() ? "touch " : "");
			sb.append(")");
		}
		logger.info(sb.toString());
	}

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

			String lograw = rec.getMessage();
			int preBegin = lograw.indexOf(TAG_PRE_OPEN);
			if (preBegin < 0) {
				// no <pre> tag found - simple encoding
				console.log(rl, XMLCoder.encode(lograw));
				return;
			}
			StringBuilder log = new StringBuilder();
			int preEnd = 0;
			int start = preBegin;
			// encode all text inside <pre>...</pre> tags
			do {
				preBegin += TAG_PRE_OPEN.length();
				// preformatted component found, preserve formatting
				preEnd = lograw.indexOf(TAG_PRE_CLOSE);
				if (preEnd < 0)
					preEnd = lograw.length();
				String pre = lograw.substring(preBegin, preEnd);
				log.append(lograw.substring(start, preBegin))
						.append(XMLCoder.encode(pre));
				String tail = lograw.substring(preEnd);
				if (!tail.startsWith(TAG_PRE_CLOSE))
					// append </pre> if missing
					log.append(TAG_PRE_CLOSE);
				log.append(tail);
				start = preEnd + TAG_PRE_CLOSE.length();
			} while ((preBegin = lograw.indexOf(TAG_PRE_OPEN, start)) >= 0);
			console.log(rl, log.toString());
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
