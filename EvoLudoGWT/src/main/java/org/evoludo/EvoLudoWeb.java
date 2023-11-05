package org.evoludo;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.ScriptInjector;
import com.google.gwt.dom.client.DataTransfer;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NodeList;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.DragLeaveEvent;
import com.google.gwt.event.dom.client.DragOverEvent;
import com.google.gwt.event.dom.client.DropEvent;
import com.google.gwt.event.dom.client.TouchEndEvent;
import com.google.gwt.event.dom.client.TouchStartEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DeckLayoutPanel;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HeaderPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.ResizeLayoutPanel;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.Widget;

import java.awt.Color;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.evoludo.simulator.EvoLudoGWT;
import org.evoludo.simulator.EvoLudoTrigger;
import org.evoludo.simulator.EvoLudoViews;
import org.evoludo.simulator.EvoLudoWeb.EvoLogHandler;
import org.evoludo.simulator.models.Model.ChangeListener.PendingAction;
import org.evoludo.ui.ContextMenu;
import org.evoludo.ui.InputEvent;
import org.evoludo.ui.Slider;

/**
 * Entry point classes define <code>onModuleLoad()</code>.
 */
public class EvoLudoWeb implements EntryPoint {

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
	HashMap<String, EvoLudoViews> activeViews = new HashMap<String, EvoLudoViews>();

	/**
	 * Currently visible view
	 */
	EvoLudoViews activeView;

	/**
	 * The transparent backdrop of popup EvoLudo labs is stored here to reuse.
	 */
	protected EvoLudoTrigger.LightboxPanel popup = null;

	public void onModuleLoad() {
		Color black = new Color(0,0,0);
	}

	/**
	 * Outermost panel implementing the ability to resize the GUI of EvoLudo models
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
//XXX		changeViewTo(activeViews.get(evoludoViews.getSelectedItemText()));
		evoludoViews.setFocus(false);
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
//XXX		engine.setDelay((int) evoludoSlider.getValue());
//XXX		evoludoSlider.setValue(engine.getDelay());
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
//XXX		engine.setDelay((int) evoludoSlider.getValue());
//XXX		evoludoSlider.setValue(engine.getDelay());
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
				focusOn(evoludoCLO.getElement());
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
	public void initReset() {
		String action = evoludoInitReset.getText();
//XXX		displayStatus(action + " pending. Waiting for engine to stop...");
//XXX		displayStatusThresholdLevel = Level.ALL.intValue();
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
		toggleRunning();
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
		toggleRunning();
	}

	/**
	 * Start/stop idling and stop/start running EvoLudo model.
	 */
	// XXX in ePubs, clicks on button text seem to be ignored when simulation is
	// running but clicks on the button next to the text are registered as expected.
	// does not apply when simulation is not running. bug in iBooks or issue here???
	public void toggleRunning() {
		// check if another lab is running in ePub
		if (engine.isEPub) {
			if (runningEPub != null && runningEPub != this) {
				runningEPub.toggleRunning();
				runningEPub = this;
				return;
			}
			runningEPub = this;
		}
		engine.toggle();
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
		prevNext();
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
		prevNext();
		showAltKeys(false);
	}

	void prevNext() {
		if (evoludoStep.getText().equals("Step"))
			engine.next();
		else
			engine.prev();
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
//XXX			logger.severe("Failed to open standalone lab (element '" + elementID + "' not found in DOM).");
			return;
		}
		String href = root.getElement().getAttribute("data-href").trim();
		if (href.length() == 0) {
//XXX			logger.severe("Failed to open standalone lab (invalid reference).");
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
//XXX				applyCLO(evoludoCLO.getText());
			}
		});
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
//XXX				if (defaultCLO == null) {
//XXX					revertCLO();
//XXX					return;
//XXX				}
//XXX				evoludoCLO.setText(defaultCLO);
//XXX				applyCLO(defaultCLO);
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
//XXX				engine.helpCLO();
//XXX				setView("Console log");
			}
		});
	}

	private void showAltKeys(boolean visible) {
		if (visible) {
			evoludoInitReset.setText("Reset");
//XXX			if (engine.getModel().permitsTimeReversal())
				evoludoStep.setText("Previous");
			return;
		}
		evoludoInitReset.setText("Init");
		evoludoStep.setText("Step");
	}

	/**
	 * Status line of EvoLudo model GUI.
	 */
	@UiField
	HTML evoludoStatus;

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
//XXX
		// if (!isValidDnD(data)) {
		// 	displayStatus("Drag'n'drop failed: Invalid state file.", Level.SEVERE.intValue());
		// 	return;
		// }
		// // load decompressor, just in case
		// if (!hasZipJs) {
		// 	// script (hopefully) needs to be injected only once
		// 	ScriptInjector.fromString(Resources.INSTANCE.zip().getText()).inject();
		// 	hasZipJs = true;
		// }
		// handleDnD(data, EvoLudoWeb.this);
	}

	/**
	 * JSNI method: create EvoLudo lab trigger buttons directly from javascript.
	 */
	public static native void exportCreateEvoLudoTrigger()
	/*-{
		$wnd.createEvoLudoTrigger = $entry(@org.evoludo.simulator.EvoLudoWeb::createEvoLudoTrigger(Ljava/lang/String;));
	}-*/;

	/**
	 * Display javascript alert panel - use sparingly.
	 *
	 * @param msg the message to display
	 */
	public static native void alert(String msg) /*-{
		$wnd.alert(msg);
	}-*/;

	/**
	 * JSNI method: expose convenient javascript method to obtain a list of elements
	 * in the DOM that match the selection criterion <code>selector</code>.
	 *
	 * @param selector the criterion for selecting elements in DOM
	 * @return list of elements that match <code>selector</code>
	 */
	public final native NodeList<Element> querySelectorAll(String selector)
	/*-{
		return $doc.querySelectorAll(selector);
	}-*/;

	/**
	 * JSNI method: check if {@code element} is active.
	 * 
	 * @param element the element to check
	 * @return {@code true} if {@code element} is active
	 */
	public final native boolean isElementActive(Element element)
	/*-{
		return ($doc.querySelector(":focus") === element);
	}-*/;

	public final native void focusOn(Element element)
	/*-{
		element.focus();
	}-*/;

	/**
	 * JSNI method: return the pixel ratio of the current device. This is intended
	 * to prevent distortions on the <code>canvas</code> objects of the data views.
	 *
	 * @return pixel ratio of device
	 */
	public final static native int getDevicePixelRatio()
	/*-{
		return $wnd.devicePixelRatio || 1;
	}-*/;

	/**
	 * Retrieve string identifying the ePub reading system:
	 * <ul>
	 * <li>web browser: null</li>
	 * <li>iBooks: iBooks, Apple Books</li>
	 * <li>Adobe Digital Editions 4.5.8 (macOS): RMSDK</li>
	 * <li>Readium: no JS (and no display equations!)</li>
	 * <li>TEA Ebook 1.5.0 (macOS): no JS (and issues with MathML)</li>
	 * <li>calibre: calibre-desktop (supports JS and MathML but appears
	 * unstable)</li>
	 * </ul>
	 * <strong>Note:</strong> nonlinear content in Apple Books on iOS does
	 * <strong>not</strong> report as an ePub reading system (at least on the iPad).
	 * Apple Books on macOS behaves as expected.
	 *
	 * @return identification string of ePub reading system or <code>null</code> if
	 *         no reading system or reading system unknown
	 */
	public final static native String getEPubReader()
	/*-{
		var epub = $wnd.navigator.epubReadingSystem
				|| window.navigator.epubReadingSystem
				|| navigator.epubReadingSystem;
		if (!epub)
			return null;
		return epub.name;
	}-*/;

	/**
	 * JSNI method: test if ePub reader supports <code>feature</code>. in ePub 3
	 * possible features are:
	 * <dl>
	 * <dt>dom-manipulation</dt>
	 * <dd>Scripts MAY make structural changes to the document’s DOM (applies to
	 * spine-level scripting only).</dd>
	 * <dt>layout-changes</dt>
	 * <dd>Scripts MAY modify attributes and CSS styles that affect content layout
	 * (applies to spine-level scripting only).</dd>
	 * <dt>touch-events</dt>
	 * <dd>The device supports touch events and the Reading System passes touch
	 * events to the content.</dd>
	 * <dt>mouse-events</dt>
	 * <dd>The device supports mouse events and the Reading System passes mouse
	 * events to the content.</dd>
	 * <dt>keyboard-events</dt>
	 * <dd>The device supports keyboard events and the Reading System passes
	 * keyboard events to the content.</dd>
	 * <dt>spine-scripting</dt>
	 * <dd>Indicates whether the Reading System supports spine-level scripting
	 * (e.g., so a container-constrained script can determine whether any actions
	 * that depend on scripting support in a Top-level Content Document have any
	 * chance of success before attempting them).</dd>
	 * </dl>
	 *
	 * @see <a href=
	 *      "https://www.w3.org/publishing/epub3/epub-contentdocs.html#app-epubReadingSystem">
	 *      https://www.w3.org/publishing/epub3/epub-contentdocs.html#app-epubReadingSystem</a>
	 *
	 * @param feature the ePub feature to test
	 * @return <code>true</code> if feature supported
	 */
	public final static native boolean ePubReaderHasFeature(String feature)
	/*-{
		var epub = $wnd.navigator.epubReadingSystem;
		if (!epub)
			return false;
		return $wnd.navigator.epubReadingSystem.hasFeature(feature);
	}-*/;

	/**
	 * Indicator whether display system supports WebGL to display population
	 * structures in 3D.
	 */
//XXX	private final boolean isWebGLSupported;

	/**
	 * JSNI method: check whether WebGL and hence 3D graphics are supported.
	 * <p>
	 * <strong>Note:</strong> asssumes that <code>canvas</code> is supported
	 *
	 * @return <code>true</code> if WebGL is supported
	 */
	private static native boolean _isWebGLSupported() /*-{
		try {
			var canvas = $doc.createElement('canvas');
			// must explicitly check for null (otherwise this may return an object
			// violating the signature of the _isWebGLSupported() method)
			return (!!$wnd.WebGLRenderingContext
					&& (canvas.getContext('webgl') || canvas
							.getContext('experimental-webgl'))) != null;
		} catch (e) {
			return false;
		}
	}-*/;

	/**
	 * JSNI method: base-64 encoding of string.
	 *
	 * @param b string to encode
	 * @return encoded string
	 */
	public static native String b64encode(String b) /*-{
		return window.btoa(b);
	}-*/;

	/**
	 * JSNI method: decoding of base-64 encoded string.
	 *
	 * @param a string to decode
	 * @return decoded string
	 */
	public static native String b64decode(String a) /*-{
		return window.atob(a);
	}-*/;

	/**
	 * JSNI method: encourage browser to download {@code dataURL} as a file named
	 * {@code filename}.
	 * <p>
	 * <strong>Note:</strong> <code>&#x23;</code> characters cause trouble in data
	 * URL. Escape them with <code>%23</code>.
	 *
	 * @param dataURL  the file content encoded as data URL
	 * @param filename the name of the downloaded file
	 */
	public static native void _export(String dataURL, String filename) /*-{
		var elem = $doc.createElement('a');
		// character '#' makes trouble in data URL's - escape
		elem.href = dataURL.replace(/#/g, "%23");
		elem.download = filename;
		$doc.body.appendChild(elem);
		elem.click();
		$doc.body.removeChild(elem);
	}-*/;

	/*
	 * some potentially useful (but currently unused) javascript snippets
	 */

	// /**
	// * open new window displaying data url
	// *
	// * <p>unfortunately an increasing number of browsers deem data url's unsafe
	// and ignore this call.
	// * at present (october 2019), works as expected in Safari but not Google
	// Chrome or Firefox.
	// *
	// * @param dataURL
	// */
	// protected static native void _export(String dataURL)
	// /*-{
	// $wnd.open(dataURL, '_blank');
	// }-*/;

	// public static native String getComputedStyle(Element element, String style)
	// /*-{
	// return $wnd.getComputedStyle(element, null).getPropertyValue(style);
	// }-*/;

	// public static native int getBoundingHeight(Element element)
	// /*-{
	// return element.getBoundingClientRect().height;
	// }-*/;

	// public final native Element activeElement()
	// /*-{
	// return $doc.activeElement;
	// }-*/;

	// public static native JsArray<Node> getAttributes(Element elem) /*-{
	// return elem.attributes;
	// }-*/;

	// public final static native int getScreenWidth()
	// /*-{
	// return $wnd.screen.width;
	// }-*/;

	// public final static native int getScreenHeight()
	// /*-{
	// return $wnd.screen.height;
	// }-*/;

	// protected static native String dumpStyle(Element element)
	// /*-{
	// return Object.keys(element.style).map(function(k) { return k + ":" +
	// element.style[k] }).join(", ");
	// }-*/;

	// public final static native String getLocalStorage(String key)
	// /*-{
	// return localStorage.getItem(key);
	// }-*/;

	// public final static native String setLocalStorage(String key, String value)
	// /*-{
	// return localStorage.setItem(key, value);
	// }-*/;

	// public static native void logJS(String msg)
	// /*-{
	// console.log(msg);
	// }-*/;

	// /**
	// * determine browser prefix for event handlers and features
	// *
	// * @return browser specific javascript prefix
	// */
	// private static native String _jsPrefix()
	// /*-{
	// var styles = $wnd.getComputedStyle($doc.documentElement, '');
	// return (Array.prototype.slice
	// .call(styles).join('')
	// .match(/-(moz|webkit|ms)-/) || (styles.OLink === '' && ['', 'o']))[1];
	// }-*/;
}
