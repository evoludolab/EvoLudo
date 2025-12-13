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

package org.evoludo.simulator.ui;

import java.util.ArrayList;
import java.util.List;

import org.evoludo.EvoLudoWeb;
import org.evoludo.simulator.EvoLudoGWT;
import org.evoludo.simulator.EvoLudoTrigger;
import org.evoludo.simulator.views.AbstractView;
import org.evoludo.util.CLOParser;
import org.evoludo.util.NativeJS;

/**
 * Handles global key events and delegates actions to the owning EvoLudoWeb.
 */
public class KeyHandler {

	/**
	 * The EvoLudoWeb instance that owns this controller.
	 */
	private final EvoLudoWeb gui;

	/**
	 * Tracks whether the Alt modifier is currently pressed.
	 */
	private boolean isAltDown = false;

	/**
	 * Tracks whether the Shift modifier is currently pressed.
	 */
	private boolean isShiftDown = false;

	/**
	 * String value emitted when the Alt key is pressed.
	 */
	public static final String KEY_ALT = "Alt";

	/**
	 * String value emitted when the Shift key is pressed.
	 */
	public static final String KEY_SHIFT = "Shift";

	/**
	 * String value emitted when the Enter key is pressed.
	 */
	public static final String KEY_ENTER = "Enter";

	/**
	 * String value emitted when the Escape key is pressed.
	 */
	public static final String KEY_ESCAPE = "Escape";

	/**
	 * String value emitted when the Backspace key is pressed.
	 */
	public static final String KEY_BACKSPACE = "Backspace";

	/**
	 * String value emitted when the Delete key is pressed.
	 */
	public static final String KEY_DELETE = "Delete";

	/**
	 * Controller that currently has priority (typically the popup lab).
	 */
	private static KeyHandler active;

	/**
	 * All handlers that have requested global registration.
	 */
	private static final List<KeyHandler> keyHandlers = new ArrayList<>();

	/**
	 * Number of handlers that have requested registration.
	 */
	private static int registrationCount = 0;

	/**
	 * Tracks whether the global JS listeners have been installed.
	 */
	private static boolean listenersInstalled = false;

	/**
	 * Indicates whether this controller has been registered globally.
	 */
	private boolean registered = false;

	/**
	 * Index of previously active view before switching to console view.
	 */
	private int toggleIdx = -1;

	/**
	 * Initializes the key event handler.
	 * 
	 * @param gui the owning EvoLudoWeb instance
	 */
	public KeyHandler(EvoLudoWeb gui) {
		this.gui = gui;
	}

	/**
	 * Registers the handler and installs global listeners if needed.
	 */
	public void register() {
		if (registered)
			return;
		registerGlobal(this);
		registered = true;
		if (gui.hasPopup())
			setActive(this);
	}

	/**
	 * Unregisters the handler and removes listeners when no handlers remain.
	 */
	public void unregister() {
		if (!registered)
			return;
		unregisterGlobal(this);
		registered = false;
	}

	/**
	 * Sets the active handler that receives priority for key events.
	 * 
	 * @param handler the handler to set active
	 */
	private static void setActive(KeyHandler handler) {
		active = handler;
	}

	/**
	 * Registers a handler for global key event handling.
	 * 
	 * @param handler the handler to register
	 */
	private static synchronized void registerGlobal(KeyHandler handler) {
		keyHandlers.add(handler);
		if (registrationCount == 0) {
			addGlobalKeyListeners();
			listenersInstalled = true;
		}
		registrationCount++;
	}

	/**
	 * Unregisters a handler from global key event handling.
	 * 
	 * @param handler the handler to unregister
	 */
	private static synchronized void unregisterGlobal(KeyHandler handler) {
		keyHandlers.remove(handler);
		if (registrationCount > 0)
			registrationCount--;
		if (registrationCount == 0 && listenersInstalled) {
			removeGlobalKeyListeners();
			listenersInstalled = false;
		}
	}

	/**
	 * Marks Alt mode active for touch interactions so button labels update
	 * accordingly.
	 */
	public void showAltModeFromTouch() {
		isAltDown = true;
		gui.updateKeys();
	}

	/** Hides the touch-activated Alt mode and restores the original labels. */
	public void hideAltModeFromTouch() {
		isAltDown = false;
		gui.updateKeys();
	}

	/**
	 * Dispatches keydown events first to the active controller, then to others.
	 *
	 * @param key the key value reported by the browser event
	 * @return {@code true} if any controller consumed the event
	 */
	@SuppressWarnings("java:S1144") // Used by JSNI
	private static boolean dispatchKeyDown(String key) {
		KeyHandler current = active;
		if (current != null && current.onKeyDown(key))
			return true;
		for (KeyHandler controller : new ArrayList<>(keyHandlers)) {
			if (controller == current)
				continue;
			if (controller.onKeyDown(key))
				return true;
		}
		return false;
	}

	/**
	 * Dispatches keyup events first to the active controller, then to others.
	 *
	 * @param key the key value reported by the browser event
	 * @return {@code true} if any controller consumed the event
	 */
	@SuppressWarnings("java:S1144") // Used by JSNI
	private static boolean dispatchKeyUp(String key) {
		KeyHandler current = active;
		if (current != null && current.onKeyUp(key))
			return true;
		for (KeyHandler controller : new ArrayList<>(keyHandlers)) {
			if (controller == current)
				continue;
			if (controller.onKeyUp(key))
				return true;
		}
		return false;
	}

	/**
	 * Process {@code keydown} events to allow for <em>repeating</em> keyboard
	 * shortcuts. Use for events where repeating does make sense, such as advancing
	 * a model by a single step or changing the speed of the model execution by
	 * adjusting the delay between subsequent updates. For non-repeating events,
	 * such starting or stopping the model or changing the view, see
	 * {@link #onKeyUp(String)}. The set of keys handled by {@code onKeyUp} and
	 * {@code onKeyDown} should be disjoint.
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
	 * @see #onKeyUp(String)
	 * @see <a href=
	 *      "https://developer.mozilla.org/en-US/docs/Web/API/KeyboardEvent/key/Key_Values">Mozilla
	 *      Key Values</a>
	 * @see AbstractView#onKeyDown(String) AbstractView.keyDownHandler(String)
	 *      and implementing classes for further keys that may be handled by the
	 *      current view
	 */
	public boolean onKeyDown(String key) {
		if (!gui.isShowing())
			return false;
		if (key.equals(KEY_ALT)) {
			isAltDown = true;
			gui.updateKeys();
		}
		if (key.equals(KEY_SHIFT))
			isShiftDown = true;
		if (NativeJS.isElementActive(gui.getCLOElement()))
			return false;
		AbstractView<?> activeView = gui.getActiveView();
		if (activeView != null && activeView.onKeyDown(key))
			return true;
		EvoLudoGWT engine = gui.getEngine();
		switch (key) {
			case "ArrowRight":
			case "n":
				engine.next();
				break;
			case "ArrowLeft":
			case "p":
				engine.prev();
				break;
			case "D":
				engine.debug();
				break;
			case "+":
			case "=":
				engine.decreaseDelay();
				gui.syncDelaySlider();
				break;
			case "-":
				engine.increaseDelay();
				gui.syncDelaySlider();
				break;
			case KEY_ENTER:
				return true;
			default:
				return false;
		}
		return true;
	}

	/**
	 * Process {@code keyup} events to allow for <em>non-repeating</em> keyboard
	 * shortcuts. Use for events where repeating does not make sense, such as
	 * stopping a model or changing views. For repeating events, such as advancing
	 * the model by a single step, see {@link #onKeyDown(String)}. The set of
	 * keys handled by {@code onKeyUp} and {@code onKeyDown} should be
	 * disjoint.
	 * 
	 * <h3>Implementation Notes:</h3>
	 * <ul>
	 * <li>{@code keyup} events are ignored if:
	 * <ol>
	 * <li>this EvoLudo model is not visible.
	 * <li>the command line options field has the focus. With
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
	 * <li>the command line options field has the focus. With
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
	 * @see #onKeyDown(String)
	 * @see <a href=
	 *      "https://developer.mozilla.org/en-US/docs/Web/API/KeyboardEvent/key/Key_Values">Mozilla
	 *      Key Values</a>
	 * @see AbstractView#onKeyUp(String) AbstractView.keyUpHandler(String) and
	 *      implementing classes for further keys that may be handled by the current
	 *      view
	 */
	public boolean onKeyUp(String key) {
		if (!gui.isShowing())
			return false;
		updateModifierStates(key);
		if (gui.isEPub() && !gui.isEPubStandalone())
			return false;
		if (handleCLOActiveKeys(key))
			return true;
		AbstractView<?> activeView = gui.getActiveView();
		if (activeView != null && activeView.onKeyUp(key))
			return true;
		return handleGlobalKeys(key);
	}

	/**
	 * Updates the modifier key states when keys are released.
	 * 
	 * @param key the released key
	 */
	private void updateModifierStates(String key) {
		if (key.equals(KEY_ALT)) {
			isAltDown = false;
			gui.updateKeys();
		}
		if (key.equals(KEY_SHIFT))
			isShiftDown = false;
	}

	/**
	 * Handles key events when the command line options field is active.
	 * 
	 * @param key the released key
	 * @return {@code true} if key has been handled
	 */
	private boolean handleCLOActiveKeys(String key) {
		boolean cloActive = NativeJS.isElementActive(gui.getCLOElement());
		if (!cloActive)
			return false;
		if (isShiftDown && key.equals(KEY_ENTER)) {
			gui.applyCLOFromField();
			return true;
		}
		return !key.equals(KEY_ESCAPE);
	}

	/**
	 * Handles global key events.
	 * 
	 * @param key the released key
	 * @return {@code true} if key has been handled
	 */
	private boolean handleGlobalKeys(String key) {
		switch (key) {
			case "0":
				gui.toggleSettings();
				return true;
			case "1":
			case "2":
			case "3":
			case "4":
			case "5":
			case "6":
			case "7":
			case "8":
			case "9":
				return handleNumericViewSwitch(key);
			case "c":
				return handleConsoleToggle();
			case KEY_ENTER:
			case " ":
				gui.getEngine().startStop();
				return true;
			case KEY_ESCAPE:
				return handleEscapeKey();
			case KEY_BACKSPACE:
			case KEY_DELETE:
				return handleDeleteKeys();
			case "E":
				return handleExport();
			case "F":
				return handleFullscreen();
			case "H":
				gui.showHelp();
				return true;
			default:
				return false;
		}
	}

	/**
	 * Handles numeric view switching for keys 1-9.
	 * 
	 * @param key the released key
	 * @return {@code true} if key has been handled
	 */
	private boolean handleNumericViewSwitch(String key) {
		List<AbstractView<?>> views = gui.getActiveViews();
		int idx = CLOParser.parseInteger(key);
		if (idx <= views.size())
			gui.changeView(views.get(idx - 1));
		return true;
	}

	/**
	 * Handles toggling the console view.
	 * 
	 * @return {@code true} if key has been handled
	 */
	private boolean handleConsoleToggle() {
		List<AbstractView<?>> views = gui.getActiveViews();
		int consoleIdx = views.size() - 1;
		if (consoleIdx < 0)
			return false;
		if (gui.getActiveViewIndex() == consoleIdx) {
			if (toggleIdx >= 0 && toggleIdx < views.size())
				gui.changeView(views.get(toggleIdx));
		} else {
			toggleIdx = gui.getActiveViewIndex();
			gui.changeView(views.get(consoleIdx));
		}
		return true;
	}

	/**
	 * Handles the Escape key functionality.
	 * 
	 * @return {@code true} if key has been handled
	 */
	private boolean handleEscapeKey() {
		if (gui.isEPub())
			return false;
		EvoLudoGWT engine = gui.getEngine();
		if (engine.isRunning()) {
			engine.stop();
			return true;
		}
		if (gui.isCLOPanelVisible()) {
			gui.toggleSettings();
			return true;
		}
		if (gui.closePopup())
			return true;
		gui.initReset();
		return true;
	}

	/**
	 * Handles the Delete and Backspace key functionality.
	 * 
	 * @return {@code true} if key has been handled
	 */
	private boolean handleDeleteKeys() {
		EvoLudoGWT engine = gui.getEngine();
		if (engine.isRunning())
			engine.stop();
		else
			gui.initReset();
		return true;
	}

	/**
	 * Handles exporting the current model state.
	 * 
	 * @return {@code true} if key has been handled
	 */
	private boolean handleExport() {
		if (gui.isEPub() || gui.getEngine().isRunning())
			return false;
		gui.getEngine().exportState();
		return true;
	}

	/**
	 * Handles toggling fullscreen mode.
	 * 
	 * @return {@code true} if key has been handled
	 */
	private boolean handleFullscreen() {
		if (!NativeJS.isFullscreenSupported())
			return false;
		gui.getEngine().setFullscreen(!NativeJS.isFullscreen());
		return true;
	}

	/**
	 * Installs JS listeners that route keyboard events to all controllers.
	 */
	private static native void addGlobalKeyListeners() /*-{
		if (!$wnd.EvoLudoUtils) {
			$wnd.EvoLudoUtils = {};
			$wnd.EvoLudoUtils.keyListeners = new Map();
		}
		if (!$wnd.EvoLudoUtils.keyListeners.get('keydown')) {
			$wnd.EvoLudoUtils.keyListeners.set(
					'keydown',
					$entry(function(event) {
						if (@org.evoludo.simulator.ui.KeyHandler::dispatchKeyDown(Ljava/lang/String;)(event.key)) {
							event.preventDefault();
						}
						event.stopPropagation();
					}));
			$wnd.EvoLudoUtils.keyListeners.set(
					'keyup',
					$entry(function(event) {
						if (@org.evoludo.simulator.ui.KeyHandler::dispatchKeyUp(Ljava/lang/String;)(event.key)) {
							event.preventDefault();
						}
						event.stopPropagation();
					}));
		}
		$wnd.addEventListener('keydown', $wnd.EvoLudoUtils.keyListeners.get('keydown'), true);
		$wnd.addEventListener('keyup', $wnd.EvoLudoUtils.keyListeners.get('keyup'), true);
	}-*/;

	/**
	 * Removes the JS listeners that were installed by
	 * {@link #addGlobalKeyListeners()}.
	 */
	private static native void removeGlobalKeyListeners() /*-{
		if (!$wnd.EvoLudoUtils || !$wnd.EvoLudoUtils.keyListeners)
			return;
		var key = $wnd.EvoLudoUtils.keyListeners.get('keydown');
		if (key)
			$wnd.removeEventListener('keydown', key, true);
		key = $wnd.EvoLudoUtils.keyListeners.get('keyup');
		if (key)
			$wnd.removeEventListener('keyup', key, true);
	}-*/;

	/**
	 * Check whether this controller currently receives priority for key events.
	 * 
	 * @return {@code true} if this controller currently receives priority for key
	 *         events.
	 */
	public boolean isActive() {
		return active == this;
	}

	/**
	 * Determine whether the Alt modifier key is currently engaged.
	 * 
	 * @return {@code true} if the Alt modifier is currently engaged.
	 */
	public boolean isAltDown() {
		return isAltDown;
	}

	/**
	 * Determine whether the Shift modifier key is currently engaged.
	 * 
	 * @return {@code true} if the Shift modifier is currently engaged.
	 */
	public boolean isShiftDown() {
		return isShiftDown;
	}
}
