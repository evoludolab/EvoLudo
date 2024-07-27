package org.evoludo;

import java.util.HashMap;
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
import org.evoludo.simulator.modules.Module;
import org.evoludo.simulator.views.AbstractView;
import org.evoludo.simulator.views.Console;
import org.evoludo.simulator.views.Distribution;
import org.evoludo.simulator.views.HasConsole;
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
import org.evoludo.ui.InputEvent;
import org.evoludo.ui.Slider;
import org.evoludo.ui.TextLogFormatter;
import org.evoludo.util.CLOParser;
import org.evoludo.util.CLOProvider;
import org.evoludo.util.CLOption;
import org.evoludo.util.CLOption.CLODelegate;
import org.evoludo.util.NativeJS;
import org.evoludo.util.XMLCoder;

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
import com.google.gwt.dom.client.Style;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.DragEnterEvent;
import com.google.gwt.event.dom.client.DragEnterHandler;
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
 * Entry point classes define <code>onModuleLoad()</code>.
 */
public class EvoLudoWeb extends Composite
		implements MilestoneListener, ChangeListener, CLOProvider, EntryPoint {

	/**
	 * <strong>Apple Books (iBook) notes:</strong>
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
	 *
	 * <strong>ePub notes:</strong>
	 * <ol>
	 * <li>Adobe Digital Editions: non-linear pages are appended to the end, which
	 * makes returning to the corresponding text location impossible (somehow a
	 * marker should be set to allow using the back button).</li>
	 * </ol>
	 */

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
	 * In order to conserve computational resources the minimum time between
	 * subsequent GUI updates has to be at least
	 * <code>MIN_MSEC_BETWEEN_UPDATES</code> milliseconds. If update request are
	 * made more frequently some are request are not honoured and simply dropped.
	 */
	protected static final int MIN_MSEC_BETWEEN_UPDATES = 50; // max 20 updates per second

	/**
	 * Time of last GUI update
	 */
	protected double updatetime = -1.0;

	/**
	 * Reference to running engine in ePubs (to conserve resources only one lab can
	 * be running at a time)
	 */
	public static EvoLudoWeb runningEPub;

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
	 * Reference to registration of drag'n'drop handlers (if one was installed).
	 */
	HandlerRegistration dragEnterHandler, dragLeaveHandler;

	/**
	 * ID of element in DOM that contains the EvoLudo lab.
	 */
	String elementID = "EvoLudoWeb";

	/**
	 * Default set of parameters as specified for the initial invocation.
	 */
	private String defaultCLO;

	/**
	 * Look-up table for active views. This is the selection shown in
	 * {@link #evoludoViews}.
	 */
	HashMap<String, AbstractView> activeViews = new HashMap<String, AbstractView>();

	/**
	 * By default the first data view is shown. In general this shows the strategies
	 * in the (structured) population in 2D.
	 */
	private String initialView = "1";

	/**
	 * Currently visible view
	 */
	AbstractView activeView;

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
	 * Mai
	 * n constructor for EvoLudo labs.
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
		if (root == null) {
			// element does not exist - should we create it and attach to DOM? or simply
			// fail?
			initWidget(new Label("Element with ID '" + elementID + "' does not exist!"));
			isWebGLSupported = false;
			return;
		}
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

		// instantiate engine early so that logging can be set up; EvoLudo console is
		// added later
		engine = new EvoLudoGWT();
		engine.addCLOProvider(this);
		engine.addMilestoneListener(this);
		engine.addChangeListener(this);
		logger = engine.getLogger();
		logger.setLevel(Level.INFO);
		// note: log handler needs to know whether this is an ePub (regardless of any
		// feature declarations with --gui) to make sure log is properly XML encoded (if
		// needed).
		boolean isEPub = NativeJS.getEPubReader() != null;
		XMLCoder.setStrict(engine.isXML);
		ConsoleLogHandler logHandler = new ConsoleLogHandler();
		logHandler.setFormatter(new TextLogFormatter(true, isEPub || engine.isXML));
		logger.addHandler(logHandler);

		// set command line options
		evoludoCLO.getElement().setAttribute("contenteditable", "true");
		engine.setCLO(clo);

		// layout speed controls - GWT has no slider...
		// but we can construct one from HTML5!
		evoludoSlider.setRange((int) EvoLudo.DELAY_MAX, (int) EvoLudo.DELAY_MIN);
		evoludoSlider.setLogBase(1.1);
		// note: the {}-place holders interfere with UiBinder...
		evoludoSlider.setTitle("Set delay between updates ({max} - {min}msec); now at {value}msec");

		viewConsole = new Console(engine);
		logEvoHandler = new EvoLogHandler(viewConsole);
		logger.addHandler(logEvoHandler);
		logger.setLevel(Level.INFO);
		logFeatures();
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
		NodeList<Element> labs = NativeJS.querySelectorAll("div.evoludo-simulation");
		int nLabs = labs.getLength();
		for (int n = 0; n < nLabs; n++) {
			Element labElement = labs.getItem(n);
			HTMLPanel lab = HTMLPanel.wrap(labElement);
			String id = labElement.getId();
			if (id == null || id.length() == 0) {
				id = HTMLPanel.createUniqueId();
				labElement.setId(id);
			}
			// check if options have been provided in URL (works only if there is
			// just a single lab on the page).
			if (nLabs > 1) {
				lab.add(new EvoLudoWeb(id, (String) null));
				continue;
			}
			String clo = Window.Location.getParameter("clo");
			if (clo != null) {
				clo = clo.trim();
				if (clo.length() > 0) {
					if (clo.charAt(0) == '"' || clo.charAt(0) == '“' || clo.charAt(0) == "'".charAt(0))
						clo = clo.substring(1);
					if (clo.charAt(clo.length() - 1) == '"' || clo.charAt(clo.length() - 1) == '”'
							|| clo.charAt(clo.length() - 1) == "'".charAt(0))
						clo = clo.substring(0, clo.length() - 1);
					// converts + (and %2B) into spaces to prevent troubles (e.g. for geometries)
					// clo = URL.decodeQueryString(clo);
				}
			}
			lab.add(new EvoLudoWeb(id, clo));
		}
		// process DOM and replace all div's with class evoludo-trigger-html by a
		// trigger button
		NodeList<Element> triggers = NativeJS.querySelectorAll("div.evoludo-trigger-html");
		int nTriggers = triggers.getLength();
		for (int n = 0; n < nTriggers; n++) {
			Element triggerElement = triggers.getItem(n);
			HTMLPanel trigger = HTMLPanel.wrap(triggerElement);
			String id = triggerElement.getId();
			if (id == null || id.length() == 0) {
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
		String clo = engine.getCLO();
		// clo may have been set from the URL or as an HTML attribute
		if (clo == null || clo.length() == 0) {
			RootPanel root = RootPanel.get(elementID);
			if (root != null)
				clo = root.getElement().getAttribute("data-clo").trim();
		}
		applyCLO(clo);
		// save initial set of parameters as default
		defaultCLO = clo;
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
		super.onUnload();
		// clear command line options to ensure they are reset to the original
		// values should the same model get loaded again.
		// note: this clears command line options set via URL but it does not seem
		// possible to load those again. ditto for epubs. only seems to apply
		// to trigger buttons and overlay labs.
		engine.setCLO(null);
		engine.requestAction(PendingAction.UNLOAD);
		viewConsole.clearLog();
	}

	/**
	 * Helper method to limit the number of GUI updates per second to conserve
	 * resources or put them to better use.
	 * 
	 * @return {@code true} if an update is recommended, {@code false} otherwise
	 */
	private boolean doUpdate() {
		double now = Duration.currentTimeMillis();
		if (now - updatetime < MIN_MSEC_BETWEEN_UPDATES)
			return false;
		updatetime = now;
		return true;
	}

	/**
	 * Update GUI. If time since last update is less than
	 * {@link #MIN_MSEC_BETWEEN_UPDATES} then views may choose to skip an update.
	 */
	public void update() {
		update(!engine.isRunning() || doUpdate());
	}

	/**
	 * Update GUI. The update is forced if {@code force} is {@code true}.
	 * 
	 * @param force update update is forced if {@code true}
	 */
	public void update(boolean force) {
		for (AbstractView view : activeViews.values())
			view.update(force);
		if (force)
			updateStatus();
	}

	@Override
	public void moduleLoaded() {
		// NOTE: at this point engine and GUI can be out of sync - better wait for reset
		// to update views
		// for (AbstractView view : activeViews.values())
		// view.load();
		// assume that some kind of keys are always present, i.e. always add listener
		// for e.g. 'Alt' but key shortcuts only if not ePub
		addKeyListeners(this);
		if (popup != null)
			keyListener = this;
	}

	@Override
	public void moduleUnloaded() {
		removeKeyListeners(this);
		if (keyListener == this)
			keyListener = null;
		// clear and close settings (otherwise options may get inherited from previous
		// launches).
		evoludoCLOPanel.setVisible(false);
		evoludoCLO.setText("");
	}

	@Override
	public void moduleRestored() {
		for (AbstractView view : activeViews.values())
			view.restored();
		displayStatusThresholdLevel = Level.ALL.intValue();
		displayStatus("State successfully restored.");
	}

	@Override
	public void modelLoaded() {
		updateViews();
	}

	@Override
	public void modelUnloaded() {
		for (AbstractView view : activeViews.values())
			view.unload();
		activeView = null;
		evoludoViews.setSelectedIndex(-1);
	}

	@Override
	public void modelRunning() {
		if (engine.isEPub) {
			if (runningEPub != null)
				throw new IllegalStateException("Another ePub lab is already running!");
			runningEPub = this;
		}
		updateGUI();
	}

	@Override
	public void modelChanged(PendingAction action) {
		switch (action) {
			case NONE:
				update();
				break;
			case STATISTIC:
				update(true);
				// stop if single statistics requested
				if (engine.isRunning())
					engine.next();
				break;
			case APPLY:
				applyCLO();
				break;
			case SNAPSHOT:
				engine.setSuspended(true);
				// make sure GUI is in stopped state before taking the snapshot
				updateGUI();
				snapshotReady();
				break;
			default:
				// includes RESET, INIT, START, STOP, UNLOAD, MODE
		}
		if (!engine.isRunning())
			updateGUI();
	}

	@Override
	public void modelStopped() {
		if (engine.isEPub) {
			if (runningEPub == null)
				throw new IllegalStateException("Running ePub lab not found!");
			if (runningEPub == this)
				runningEPub = null;
		}
		update(true);
		updateGUI();
	}

	@Override
	public void modelDidReinit() {
		// forward init to all current views
		for (AbstractView view : activeViews.values())
			view.init();
		updateGUI();
	}

	@Override
	public void modelDidReset() {
		// invalidate network
		for (AbstractView view : activeViews.values())
			view.reset(true);
		updateGUI();
		displayStatus(engine.getVersion());
	}

	/**
	 * Update GUI for running/stopped model.
	 */
	private void updateGUI() {
		boolean stopped = !engine.isRunning();
		evoludoStartStop.setText(stopped ? "Start" : "Stop");
		evoludoStep.setEnabled(stopped);
		evoludoInitReset.setEnabled(stopped);
		evoludoApply.setEnabled(stopped);
		evoludoDefault.setEnabled(stopped);
		evoludoSlider.setValue(engine.getDelay());
		evoludoSlider.setEnabled(engine.getModel().getMode() != Mode.STATISTICS_SAMPLE);
		if (stopped)
			updateStatus();
	}

	/**
	 * Helper method to update GUI.
	 */
	private void updateStatus() {
		Model model = engine.getModel();
		// do not force retrieving status if engine is running
		String s = activeView.getStatus(!engine.isRunning());
		if (s == null)
			s = model.getStatus();
		displayStatus(s);
		evoludoTime.setText(model.getCounter());
		updatetime = Duration.currentTimeMillis();
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
	 * Parse <code>name</code> to determine which data view of the EvoLudo model to
	 * display in the GUI. <code>name</code> can either be a number (as a String)
	 * that indicates the position of the requested view in the popup list
	 * {@link #evoludoViews} or the title of the view (with spaces replaced by '_').
	 * This routine is called when processing keyboard shortcuts (see
	 * {@link #keyDownHandler(String)} or command line options (see
	 * {@link #cloView}). If <code>name</code> cannot be found the active view
	 * remains unchanged (if possible) or otherwise shows the view selected in the
	 * popup list {@link #evoludoViews}.
	 *
	 * @param name of data view to display
	 */
	protected void setView(String name) {
		if (name != null) {
			// if length of string is 1 or 2, assume an index is given
			if (name.length() < 3) {
				int idx = Integer.parseInt(name);
				if (idx >= 0 && idx < evoludoViews.getItemCount())
					// in order to align argument to --view and keyboard shortcuts
					// the view count starts at 1 (both 0 and 1 return the first view)
					name = evoludoViews.getItemText(Math.max(0, idx - 1));
				else
					name = null;
			} else
				name = name.replace('_', ' ').trim();
		}
		AbstractView newView = activeViews.get(name);
		if (newView == null) {
			// requested view not found
			if (activeViews.containsValue(activeView)) {
				name = activeView.getName();
				newView = activeView;
			} else {
				newView = activeViews.get(evoludoViews.getSelectedItemText());
				// if still no joy the last resort is the console
				// note: may not be available in ePubs. however, if we still end up
				// here, the problem lies deeper and requires a different resolution.
				if (newView == null)
					newView = activeViews.get(viewConsole.getName());
			}
		}
		for (int n = 0; n < evoludoViews.getItemCount(); n++) {
			if (evoludoViews.getItemText(n).equals(name)) {
				evoludoViews.setSelectedIndex(n);
				break;
			}
		}
		changeViewTo(newView, true);
	}

	/**
	 * Change view of EvoLudo model data. This helper method is called when the user
	 * selects a new view with the popup list {@link #evoludoViews} or when a
	 * particular view is requested through command line options (see
	 * {@link #cloView}).
	 * 
	 * @param newView new view of model data to display
	 */
	protected void changeViewTo(AbstractView newView) {
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
	protected void changeViewTo(AbstractView newView, boolean force) {
		if (force || newView != activeView) {
			if (activeView != null)
				activeView.deactivate();
			// initially activeView would otherwise be null, which causes troubles if mouse
			// is on canvas triggering events...
			activeView = newView;
			evoludoDeck.setWidget(activeView);
			// adding a new widget can cause a flurry of activities; wait until they subside
			// before activation
			Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
				@Override
				public void execute() {
					activeView.activate();
					// initial view has layout and is active resume running
					// otherwise wait until layout is complete
					if (activeView.hasLayout() && engine.isSuspended())
						engine.run();
				}
			});
		}
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
		Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
			@Override
			public void execute() {
				evoludoLayout.onResize();
				NativeJS.focusOn(evoludoCLO.getElement());
			}
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
			showAltKeys(true);
		}
	};

	/**
	 * Touch of <code>Init</code> button started. Set timer for switching to
	 * <code>Reset</code>. Suppress default behaviour (prevents magnifying glass and
	 * text selection).
	 *
	 * @param event the TouchStartEvent that was fired
	 */
	@UiHandler("evoludoInitReset")
	public void onInitResetTouchStart(TouchStartEvent event) {
		showAltTouchTimer.schedule(ContextMenu.LONG_TOUCH_TIME);
		event.preventDefault();
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
		showAltKeys(false);
	}

	/**
	 * Initialize or reset EvoLudo model. If model is running wait until next update
	 * is completed to prevent unexpected side effects.
	 */
	protected void initReset() {
		String action = evoludoInitReset.getText();
		displayStatus(action + " pending. Waiting for engine to stop...");
		displayStatusThresholdLevel = Level.ALL.intValue();
		engine.requestAction(action.equals("Reset") ? PendingAction.RESET : PendingAction.INIT);
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
		startStop();
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
		startStop();
	}

	/**
	 * Initialize or reset EvoLudo model. If model is running wait until next update
	 * is completed to prevent unexpected side effects.
	 */
	protected void startStop() {
		String action = evoludoStartStop.getText();
		engine.requestAction(action.equals("Stop") ? PendingAction.STOP : PendingAction.START);
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
		showAltTouchTimer.schedule(ContextMenu.LONG_TOUCH_TIME);
		event.preventDefault();
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
		showAltKeys(false);
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
			case "Step":
				engine.next();
				return;
			case "Prev":
				engine.prev();
				return;
			case "Debug":
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
		if (evoludoApply.getText().equals("Apply")) {
			scheduleApplyCLO();
			return;
		}
		// open standalone lab (ePubs only)
		RootPanel root = RootPanel.get(elementID);
		if (root == null) {
			logger.severe("Failed to open standalone lab (element '" + elementID + "' not found in DOM).");
			return;
		}
		String href = root.getElement().getAttribute("data-href").trim();
		if (href.length() == 0) {
			logger.severe("Failed to open standalone lab (invalid reference).");
			return;
		}
		Window.Location.assign(href);
	}

	/**
	 * Helper method: request setting new command line parameters.
	 */
	void scheduleApplyCLO() {
		Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
			@Override
			public void execute() {
				// convert any nbsp's to regular spaces
				applyCLO(evoludoCLO.getText().replace((char) 160, ' '));
			}
		});
	}

	/**
	 * Process and apply the command line arguments <code>clo</code>. Loads new
	 * model (and unloads old one), if necessary, and loads/adjusts the data views
	 * as appropriate.
	 * 
	 * @param clo the command line arguments
	 */
	public void applyCLO(String clo) {
		engine.setCLO(clo);
		displayStatusThresholdLevel = Level.ALL.intValue();
		displayStatus("Setting parameters pending. Waiting for engine to stop...");
		if (engine.isRunning()) {
			engine.requestAction(PendingAction.APPLY);
			return;
		}
		applyCLO();
	}

	/**
	 * Helper method: applies the command line arguments stored in
	 * {@link #evoludoCLO}.
	 */
	private void applyCLO() {
		displayStatusThresholdLevel = Level.ALL.intValue();
		String currentView = evoludoViews.getSelectedItemText();
		boolean resume = engine.isRunning() || engine.isSuspended();
		engine.setSuspended(false);
		Model oldModel = engine.getModel();
		Module oldModule = engine.getModule();
		boolean parsingSuccess = engine.parseCLO();
		evoludoSlider.setValue(engine.getDelay());
		revertCLO();
		// reset is required if module and/or model changed
		Module newModule = engine.getModule();
		Model newModel = engine.getModel();
		if (newModule == null || engine.getModule() != oldModule || newModel != oldModel) {
			// process (emulated) ePub restrictions - adds console if possible
			processEPubSettings();
			engine.modelReset();
		} else {
			// process (emulated) ePub restrictions - adds console if possible
			processEPubSettings();
			updateViews();
			if (!engine.paramsDidChange()) {
				// resume running if no reset was necessary or --run was provided
				engine.setSuspended(resume || engine.isSuspended());
				// even without reset necessary data views should be adjusted to:
				// - reflect changes in report frequency (time line graphs, distributions and
				// linear geometries)
				// - changes in payoffs require rescaling of color maps
				for (AbstractView view : activeViews.values())
					view.reset(false);
				updateGUI();
			}
		}
		setView(cloView.isSet() ? initialView : currentView);
		if (!parsingSuccess)
			displayStatus("Problems parsing arguments - check log for details.", Level.WARNING.intValue() + 1);
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
		Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
			@Override
			public void execute() {
				if (defaultCLO == null) {
					revertCLO();
					return;
				}
				evoludoCLO.setText(defaultCLO);
				applyCLO(defaultCLO);
			}
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
	 * Helper method: request display of short description of all available
	 * parameters.
	 */
	void showHelp() {
		Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
			@Override
			public void execute() {
				engine.helpCLO();
				setView("Console log");
			}
		});
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
		if (level >= Level.SEVERE.intValue() && level >= displayStatusThresholdLevel) {
			evoludoStatus.setHTML("<span style='color:red'><b>Error:</b> " + msg + "</span>");
			displayStatusThresholdLevel = level;
			return;
		}
		if (level >= Level.WARNING.intValue() && level >= displayStatusThresholdLevel) {
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
		evoludoOverlay.setVisible(false);
		if (!NativeJS.isValidDnD(data)) {
			displayStatus("Drag'n'drop failed: Invalid state file.", Level.SEVERE.intValue());
			return;
		}
		// load decompressor, just in case
		if (!hasZipJs) {
			// script (hopefully) needs to be injected only once
			ScriptInjector.fromString(Resources.INSTANCE.zip().getText()).inject();
			hasZipJs = true;
		}
		NativeJS.handleDnD(data, engine);
	}

	/**
	 * The helper variable to indicate JavaScript for dealing with zip archives has
	 * already been loaded.
	 */
	private static boolean hasZipJs = false;

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
		if (key.equals("Alt"))
			// alt-key does not count as handled
			showAltKeys(false);
		if (key.equals("Shift"))
			// shift-key does not count as handled
			isShiftDown = false;
		if (engine.isEPub && !engine.ePubStandalone)
			// in ePub text flow only "Alt" key is acceptable and does not count as handled
			return false;
		boolean cloActive = NativeJS.isElementActive(evoludoCLO.getElement());
		if (cloActive) {
			// focus is on command line options ignore keypress
			// except Shift-Enter, which applies the new settings
			if (isShiftDown && key.equals("Enter")) {
				scheduleApplyCLO();
				return true;
			}
			// escape closes the settings field
			if (!key.equals("Escape"))
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
				setView(Integer.toString(CLOParser.parseInteger(key) - 1));
				break;
			case "Enter":
			case " ":
				// start/stop simulation
				startStop();
				break;
			case "Escape":
				// ignore "Escape" for ePubs
				if (engine.isEPub)
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
			case "Backspace":
			case "Delete":
				// stop running simulation; init/reset if not running
				if (engine.isRunning())
					engine.stop();
				else
					initReset();
				break;
			case "E":
				// export state (suppress in ePub's)
				if (engine.isEPub || engine.isRunning())
					return false;
				engine.exportState();
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
		if (key.equals("Alt"))
			// alt-key does not count as handled
			showAltKeys(true);
		if (key.equals("Shift"))
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
			case "Enter":
				// case " ": // spacebar
				return true;
			default:
				return false;
		}
		return true;
	}

	/**
	 * The helper variable to indicate whether the shift key is pressed.
	 */
	private boolean isShiftDown = false;

	/**
	 * The Alt-key toggles the button labels for controlling the EvoLudo lab.
	 * 
	 * @param down {@code true} if the Alt-key is pressed
	 */
	private void showAltKeys(boolean down) {
		if (down) {
			evoludoInitReset.setText("Reset");
			if (engine.getModel().permitsTimeReversal())
				evoludoStep.setText("Previous");
			if (engine.getModel().permitsDebugStep())
				evoludoStep.setText("Debug");
			return;
		}
		evoludoInitReset.setText("Init");
		evoludoStep.setText("Step");
	}

	/**
	 * Adds a marker element with ID <code>snapshot-ready</code> to DOM. This is
	 * used to control automated snapshots using <code>capture-website</code>.
	 * 
	 * @see <a href="https://github.com/sindresorhus/capture-website-cli"> Github:
	 *      capture-website-cli</a>
	 */
	private void snapshotReady() {
		// add div to DOM to signal completion of layout for capture-website
		DivElement marker = Document.get().createDivElement();
		marker.setId("snapshot-ready");
		Document.get().getBody().appendChild(marker);
	}

	/**
	 * Each EvoLudo module may entertain its own selection of views to visualize its
	 * data. Unloads all currently active views, resets the list and generates a new
	 * list of suitable views based on the features of the current model.
	 * <p>
	 * <strong>Note:</strong> the console view is dealt with elsewhere (see
	 * {@link #processEPubSettings}).
	 */
	protected void resetViews() {
		for (AbstractView view : activeViews.values())
			view.unload();
		activeViews.clear();
		updateViews();
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
		HashMap<String, AbstractView> oldViews = activeViews;
		activeViews = new HashMap<>();
		// strategies related views
		Module module = engine.getModule();
		if (module == null) {
			addView(viewConsole, oldViews);
			return;
		}
		Model model = engine.getModel();
		boolean isODESDE = (model.isODE() || model.isSDE());
		if (module instanceof HasPop2D.Strategy && !isODESDE)
			addView(new Pop2D(engine, Data.STRATEGY), oldViews);
		if (isWebGLSupported && module instanceof HasPop3D.Strategy && !isODESDE)
			addView(new Pop3D(engine, Data.STRATEGY), oldViews);
		if (module instanceof HasPhase2D)
			addView(new Phase2D(engine), oldViews);
		if (module instanceof HasMean.Strategy)
			addView(new Mean(engine, Data.STRATEGY), oldViews);
		if (module instanceof HasPhase2D)
			addView(new Phase2D(engine), oldViews);
		if (module instanceof HasS3)
			addView(new S3(engine), oldViews);
		if (module instanceof HasHistogram.Strategy)
			addView(new Histogram(engine, Data.STRATEGY), oldViews);
		if (module instanceof HasDistribution.Strategy)
			addView(new Distribution(engine, Data.STRATEGY), oldViews);
		// fitness related views
		if (module instanceof HasPop2D.Fitness && !isODESDE)
			addView(new Pop2D(engine, Data.FITNESS), oldViews);
		if (isWebGLSupported && module instanceof HasPop3D.Fitness && !isODESDE)
			addView(new Pop3D(engine, Data.FITNESS), oldViews);
		if (module instanceof HasMean.Fitness)
			addView(new Mean(engine, Data.FITNESS), oldViews);
		if (module instanceof HasHistogram.Fitness)
			addView(new Histogram(engine, Data.FITNESS), oldViews);
		// structure related views
		if (module instanceof HasHistogram.Degree && !isODESDE)
			addView(new Histogram(engine, Data.DEGREE), oldViews);
		// statistics related views
		if (model.permitsMode(Mode.STATISTICS_SAMPLE)) {
			// sample statistics available
			if (module instanceof HasHistogram.StatisticsProbability)
				addView(new Histogram(engine, Data.STATISTICS_FIXATION_PROBABILITY), oldViews);
			if (module instanceof HasHistogram.StatisticsTime)
				addView(new Histogram(engine, Data.STATISTICS_FIXATION_TIME), oldViews);
		}
		if (model.permitsMode(Mode.STATISTICS_UPDATE)) {
			// update statistics available
			if (module instanceof HasHistogram.StatisticsStationary)
				addView(new Histogram(engine, Data.STATISTICS_STATIONARY), oldViews);
		}
		// miscellaneous views
		// note: console may be removed for (simulated) ePub modes
		if (module instanceof HasConsole)
			addView(viewConsole, oldViews);
		// load all newly added views and update view selector
		evoludoViews.clear();
		for (AbstractView view : activeViews.values()) {
			if (!oldViews.containsKey(view.getName()))
				view.load();
			evoludoViews.addItem(view.getName());
		}
		// unload views that are no longer available
		for (AbstractView view : oldViews.values())
			view.unload();
		evoludoViews.setItemSelected(0, true);
		if (!activeViews.containsValue(activeView))
			activeView = activeViews.get(evoludoViews.getSelectedItemText());
	}

	/**
	 * Convenience method to add <code>view</code> to list of active views
	 * <code>activeViews</code>.
	 *
	 * @param view to add to active list
	 */
	private void addView(AbstractView view, HashMap<String, AbstractView> oldViews) {
		String name = view.getName();
		if (oldViews.containsKey(name))
			view = oldViews.remove(name);
		activeViews.put(view.getName(), view);
	}

	/**
	 * Command line option to set the data view displayed in the GUI.
	 */
	public final CLOption cloView = new CLOption("view", "1", EvoLudo.catGUI,
			"--view <v>      active view upon launch (v: index or title)", new CLODelegate() {
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
					String descr = "--view <v>      select view with index (also on keyboard)";
					int idx = 1;
					for (AbstractView view : activeViews.values()) {
						String keycode = "              " + (idx++) + ": ";
						int len = keycode.length();
						descr += "\n" + keycode.substring(len - 16, len) + view.getName();
					}
					return descr;
				}
			});

	/**
	 * Command line option to set the size of the GUI or enter fullscreen.
	 */
	public final CLOption cloSize = new CLOption("size", "530,620", EvoLudo.catGUI,
			"--size <w,h|fullscreen>  size of GUI (w: width, h: height in pixels)", new CLODelegate() {
				/**
				 * {@inheritDoc}
				 * <p>
				 * Set the initial size of the lab to {@code arg}.
				 */
				@Override
				public boolean parse(String arg) {
					if (arg.startsWith("full")) {
						if (NativeJS.isFullscreenSupported()) {
							// enter full screen, if supported
							NativeJS.requestFullscreen(evoludoPanel.getParent().getParent().getElement());
							return true;
						}
						arg = cloSize.getDefault();
					}
					double[] dim = CLOParser.parseVector(arg);
					if (dim == null || dim.length != 2)
						return false;
					// note: why do we need to set the initial size on the grandparent?
					evoludoPanel.getParent().getParent().setSize((int) dim[0] + "px", (int) dim[1] + "px");
					return true;
				}
			});

	@Override
	public void collectCLO(CLOParser parser) {
		// prepare command line options
		parser.addCLO(cloView);
		parser.addCLO(cloSize);
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
	private final native void addKeyListeners(EvoLudoWeb evoludo) /*-{
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
	private final native void removeKeyListeners(EvoLudoWeb evoludo) /*-{
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
		int width = placeholder.getOffsetWidth();
		int height = placeholder.getOffsetHeight();
		String id = wrap.getAttribute("id");
		if (id == null || id.length() == 0) {
			id = HTMLPanel.createUniqueId();
			wrap.setAttribute("id", id);
		}
		RootPanel root = RootPanel.get(id);
		wrap.removeAllChildren();
		wrap.addClassName("evoludo-simulation");
		EvoLudoWeb lab = new EvoLudoWeb(id, clo);
		Style wstyle = wrap.getStyle();
		// adopt size of placeholder - needs to account for padding of
		// .evoludo-simulation - how?
		// XXX padding of .evoludo-simulation hardcoded
		wstyle.setWidth(width - 2 * 8, Unit.PX);
		wstyle.setHeight(height - 2 * 8, Unit.PX);
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
		if (engine.isEPub)
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
		if (view.contains(xTop + getOffsetWidth(), yTop + getOffsetHeight()))
			return true;
		return false;
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
		if (id == null || id.length() == 0) {
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
		// ePubs (iBook) prevents scrolling; disable console
		if (!engine.isEPub || engine.ePubStandalone) {
			// make sure console is present; if necessary add and load it
			if (activeViews.put(viewConsole.getName(), viewConsole) == null) {
				viewConsole.load();
				evoludoViews.addItem(viewConsole.getName());
			}
		} else {
			// make sure console is absent; if necessary remove and unload it
			if (activeViews.remove(viewConsole.getName()) != null) {
				viewConsole.unload();
				// console is likely the last view in the list, hence start checking at end
				for (int n = evoludoViews.getItemCount() - 1; n >= 0; n--) {
					if (evoludoViews.getItemText(n).equals(viewConsole.getName())) {
						evoludoViews.removeItem(n);
						break;
					}
				}
			}
		}
		// nonlinear content in Apple Books (i.e. all interactive labs) do not report
		// as ePubs on iOS (at least for iPad) but as expected on macOS. On both
		// platforms TextFields are disabled through shadow DOM.
		boolean editCLO = !engine.isEPub || engine.ePubStandalone;
		// evoludoCLO.setEnabled(editCLO);
		// evoludoCLO.setReadOnly(!editCLO);
		evoludoCLO.setTitle(editCLO ? "Specify simulation parameters"
				: "Current simulation parameters (open standalone lab to modify)");
		evoludoApply.setText(editCLO ? "Apply" : "Standalone");
		evoludoApply.setTitle(editCLO ? "Apply parameters" : "Open standalone lab");
		evoludoDefault.setEnabled(editCLO);
		evoludoHelp.setEnabled(editCLO);
		evoludoSettings.setTitle(editCLO ? "Change simulation parameters" : "Review simulation parameters");
		logEvoHandler.setLevel(editCLO ? logger.getLevel() : Level.OFF);

		// add drop handler to read parameters
		if (editCLO) {
			if (dragEnterHandler == null)
				dragEnterHandler = addDomHandler(new DragEnterHandler() {
					@Override
					public void onDragEnter(DragEnterEvent event) {
						if (engine.isRunning()) {
							evoludoOverlay.setVisible(false);
							displayStatus("Stop lab for drag'n'drop restore.", Level.WARNING.intValue());
							return;
						}
						// event.getDataTransfer() does not list files (only that files are dragged)
						// cannot check whether the drag'n'drop looks promising
						evoludoOverlay.setVisible(true);
					}
				}, DragEnterEvent.getType());
			if (dragLeaveHandler == null)
				dragLeaveHandler = addDomHandler(new DragLeaveHandler() {
					@Override
					public void onDragLeave(DragLeaveEvent event) {
						if (!evoludoOverlay.isVisible() && displayStatusThresholdLevel <= Level.WARNING.intValue())
							displayStatusThresholdLevel = Level.ALL.intValue();
					}
				}, DragLeaveEvent.getType());
		}
	}

	/**
	 * Log GWT features and GUI specifics.
	 */
	void logFeatures() {
		logger.info("GWT Version: " + GWT.getVersion());
		logger.info("GUI features: " + //
				(NativeJS.isWebGLSupported() ? "WebGL " : "") + //
				(NativeJS.isXML() ? "XML " : "") + //
				(NativeJS.hasKeys() ? "keyboard " : "") + //
				(NativeJS.hasMouse() ? "mouse " : "") + //
				(NativeJS.hasTouch() ? "touch " : "") + //
				(NativeJS.isEPub() ? "ePub (" + //
						(engine.ePubHasKeys ? "keyboard" : "") + //
						(engine.ePubHasMouse ? "mouse" : "") + //
						(engine.ePubHasTouch ? "touch" : "") + ")" : ""));
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
		public void publish(LogRecord record) {
			int level = record.getLevel().intValue();
			if (level >= Level.WARNING.intValue()) {
				EvoLudoWeb.this.displayStatus(record.getMessage(), level);
			}
			if (console == null)
				return;
			String log = record.getMessage();
			// do some minimal formatting for console
			boolean preformatted = false;
			if (log.startsWith("<pre>")) {
				// remove tags - assumes entire message is wrapped
				log = log.replaceAll("<.*pre>", "");
				preformatted = true;
			}
			log = XMLCoder.encode(log);
			// add formatting for console
			if (preformatted)
				log = "<pre>" + log + "</pre>";
			console.log(record.getLevel(), log);
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
