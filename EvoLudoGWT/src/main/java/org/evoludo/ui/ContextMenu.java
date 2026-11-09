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

package org.evoludo.ui;

import java.util.HashMap;

import org.evoludo.util.NativeJS;

import com.google.gwt.core.client.JsArray;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style;
import com.google.gwt.dom.client.Style.Display;
import com.google.gwt.dom.client.Style.Position;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.dom.client.Touch;
import com.google.gwt.event.dom.client.ContextMenuEvent;
import com.google.gwt.event.dom.client.ContextMenuHandler;
import com.google.gwt.event.dom.client.HasContextMenuHandlers;
import com.google.gwt.event.dom.client.HasTouchCancelHandlers;
import com.google.gwt.event.dom.client.HasTouchEndHandlers;
import com.google.gwt.event.dom.client.HasTouchMoveHandlers;
import com.google.gwt.event.dom.client.HasTouchStartHandlers;
import com.google.gwt.event.dom.client.MouseDownEvent;
import com.google.gwt.event.dom.client.MouseMoveEvent;
import com.google.gwt.event.dom.client.MouseOutEvent;
import com.google.gwt.event.dom.client.MouseOutHandler;
import com.google.gwt.event.dom.client.MouseOverEvent;
import com.google.gwt.event.dom.client.MouseOverHandler;
import com.google.gwt.event.dom.client.TouchCancelEvent;
import com.google.gwt.event.dom.client.TouchCancelHandler;
import com.google.gwt.event.dom.client.TouchEndEvent;
import com.google.gwt.event.dom.client.TouchEndHandler;
import com.google.gwt.event.dom.client.TouchMoveEvent;
import com.google.gwt.event.dom.client.TouchMoveHandler;
import com.google.gwt.event.dom.client.TouchStartEvent;
import com.google.gwt.event.dom.client.TouchStartHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.Widget;

/**
 * Context menu extension to GWT's user interface.
 * <p>
 * Main class for implementing context menus in GWT applications. Context menus
 * work through a shared instance (like {@link Tooltip}'s). Each widget that
 * sports context menus (the <code>listener</code> widget) must be associated
 * with a context menu <code>provider</code>.
 * <p>
 * The context menu opens when the {@link ContextMenuHandler} fires (typically
 * by a right click on mouse as a pointing device and a two-finger tap/click on
 * a touch pad). For devices without a pointer the context menu is triggered by
 * a long single-finger touch (after some configurable delay), provided the
 * finger stays within {@link #TOUCH_SLOP} pixels. The context menu is
 * automatically closed for one of the following events:
 * <ol>
 * <li>an entry has been selected</li>
 * <li>scrolling of the window</li>
 * <li>on devices with pointers: once the pointer exited the menu (after some
 * delay)</li>
 * <li>on devices without pointers: by tapping anywhere else on the screen</li>
 * </ol>
 * 
 * <h2>CSS Style Rules</h2>
 * <dl>
 * <dt>.gwt-ContextMenu</dt>
 * <dd>the context menu element.</dd>
 * </dl>
 * Use shared instance to create top level context menu,
 * {@link #sharedContextMenu()}.
 * 
 * @author Christoph Hauert
 */
@SuppressWarnings("java:S110")
public class ContextMenu extends FlowPanel
		implements ContextMenuHandler, MouseOverHandler, MouseOutHandler, TouchStartHandler, TouchEndHandler,
		TouchMoveHandler, TouchCancelHandler, FullscreenChangeHandler, HasFullscreenChangeHandlers {

	/**
	 * Listener interface for widgets that offer context menus. This ensures proper
	 * placement of the context menu.
	 */
	public interface Listener extends HasContextMenuHandlers, HasTouchStartHandlers, HasTouchEndHandlers,
			HasTouchMoveHandlers, HasTouchCancelHandlers {

		/**
		 * Get the absolute left positioning of the widget requesting the context menu.
		 * 
		 * @return the absolute left of widget
		 */
		public int getAbsoluteLeft();

		/**
		 * Get the absolute top positioning of the widget requesting the context menu.
		 * 
		 * @return absolute top of widget
		 */
		public int getAbsoluteTop();
	}

	/**
	 * Provider interface for classes that are responsible for supplying the
	 * contents of the context menu.
	 */
	public interface Provider {

		/**
		 * Populate context menu <code>menu</code> in listening widget at (relative)
		 * position <code>(x,y)</code>.
		 * 
		 * @param menu context menu entries are added here
		 * @param x    horizontal coordinate (relative to listening widget)
		 * @param y    horizontal coordinate (relative to listening widget)
		 */
		public void populateContextMenuAt(ContextMenu menu, int x, int y);
	}

	/**
	 * Manage registrations of listener widgets, their context menu providers and
	 * the corresponding event handlers. Used for a lookup table to match listeners
	 * and providers with the listeners serving as the lookup key.
	 */
	protected class Registration implements HandlerRegistration {

		/**
		 * The reference to the context menu event handler.
		 */
		private HandlerRegistration contextMenuHandler;

		/**
		 * The reference to the touch start event handler.
		 */
		private HandlerRegistration touchStartHandler;

		/**
		 * The reference to the touch end event handler.
		 */
		private HandlerRegistration touchEndHandler;

		/**
		 * The reference to the touch move event handler.
		 */
		private HandlerRegistration touchMoveHandler;

		/**
		 * The reference to the touch cancel event handler.
		 */
		private HandlerRegistration touchCancelHandler;

		/**
		 * Provider of context menu entries for this listener widget.
		 */
		private Provider provider;

		/**
		 * Listener widget for context menu requests.
		 */
		private Listener listener;

		/**
		 * Register all event handlers relevant for handling context menus with
		 * <code>listener</code> widget and associate with the context menu
		 * <code>provider</code>.
		 * 
		 * @param listener widget sporting a context menu
		 * @param provider provides context menu entries for this listener widget
		 */
		public Registration(Listener listener, Provider provider) {
			this.provider = provider;
			this.listener = listener;
			// add touch handlers
			touchStartHandler = listener.addTouchStartHandler(ContextMenu.this);
			touchEndHandler = listener.addTouchEndHandler(ContextMenu.this);
			touchMoveHandler = listener.addTouchMoveHandler(ContextMenu.this);
			touchCancelHandler = listener.addTouchCancelHandler(ContextMenu.this);
			// add context menu handler
			contextMenuHandler = listener.addContextMenuHandler(ContextMenu.this);
		}

		/**
		 * Get the provider of entries of context menu for this listener widget.
		 * 
		 * @return the provider
		 */
		public Provider getProvider() {
			return provider;
		}

		/**
		 * Get the listener widget for context menu events.
		 * 
		 * @return the listening widget
		 */
		public Listener getListener() {
			return listener;
		}

		@Override
		public void removeHandler() {
			if (contextMenuHandler != null)
				contextMenuHandler.removeHandler();
			if (touchStartHandler != null)
				touchStartHandler.removeHandler();
			if (touchEndHandler != null)
				touchEndHandler.removeHandler();
			if (touchMoveHandler != null)
				touchMoveHandler.removeHandler();
			if (touchCancelHandler != null)
				touchCancelHandler.removeHandler();
		}
	}

	/**
	 * Default horizontal offset of context menu relative to the coordinates of the
	 * context menu triggering event.
	 * 
	 * @see #offsetX
	 */
	private static final int DEFAULT_OFFSET_X = -6;

	/**
	 * Default vertical offset of context menu relative to the coordinates of the
	 * context menu triggering event.
	 */
	private static final int DEFAULT_OFFSET_Y = -6;

	/**
	 * Horizontal offset of context menu relative to coordinates of context menu
	 * triggering event. In general, this should be a negative number such that any
	 * pointing device actually ends up on top of the context menu.
	 * 
	 * @see #DEFAULT_OFFSET_X
	 * @see #setOffset(int, int)
	 * @see #showAt(int, int)
	 */
	private int offsetX = DEFAULT_OFFSET_X;

	/**
	 * Vertical offset of context menu relative to coordinates of context menu
	 * triggering event. In general, this should be a negative number such that any
	 * pointing device actually ends up on top of the context menu.
	 * 
	 * @see #DEFAULT_OFFSET_Y
	 * @see #setOffset(int, int)
	 * @see #showAt(int, int)
	 */
	private int offsetY = DEFAULT_OFFSET_Y;

	/**
	 * Default delay before hiding the context (sub)menu (in milliseconds) after the
	 * pointer has exited the context (sub)menu.
	 */
	private static final int DELAY_HIDE = 500;

	/**
	 * Delay before hiding the context (sub)menu (in milliseconds) after the pointer
	 * has exited the context (sub)menu.
	 * 
	 * @see #DELAY_HIDE
	 * @see #setDelayHide(int)
	 * @see #setHideTimer()
	 * @see #cancelHideTimer()
	 */
	private int delayHide = DELAY_HIDE;

	/**
	 * Delay in milliseconds before the touch event is considered a 'long touch'
	 * instead of a 'tap'.
	 */
	public static final int LONG_TOUCH_TIME = 500;

	/**
	 * Delay before two finger touch triggers context menu, provided that no other
	 * touch event fired during <code>delayTap</code> milliseconds.
	 * 
	 * @see #LONG_TOUCH_TIME
	 * @see #setLongTouch(int)
	 */
	private int longTouch = LONG_TOUCH_TIME;

	/**
	 * Maximum distance (in pixels) a single touch may drift from its starting point
	 * and still count as a stationary long touch. Fingers resting on a touch screen
	 * jitter and roll when lifted, which fires {@link TouchMoveEvent}s that must
	 * neither cancel a pending context menu nor close one that just opened.
	 */
	private static final int TOUCH_SLOP = 10;

	/**
	 * Style name of the menu item underneath the pointer or finger. Replaces
	 * {@code :hover}, which touch screens apply to (and keep on) the item touched
	 * first and which iPadOS does not apply for trackpads.
	 */
	private static final String HIGHLIGHT_STYLE = "highlight";

	/**
	 * Reference to style of context menu. Most important to control positioning of
	 * context menu.
	 */
	protected Style style;

	/**
	 * <code>true</code> to automatically set timer for hiding context menu after
	 * pointer exited context menu.
	 */
	private boolean autoHide = true;

	/**
	 * <code>true</code> to open sub-menus automatically on hovering over the
	 * corresponding parent menu item.
	 */
	private boolean autoOpen = true;

	/**
	 * <code>true</code> to close sub-menus automatically when pointer exited the
	 * sub-menu.
	 */
	private boolean autoClose = true;

	/**
	 * <code>true</code> if pointer is currently over context (sub)menu.
	 */
	private boolean isMouseOver = false;

	/**
	 * Reference to fullscreen change handler. When entering fullscreen the context
	 * menu needs to be moved from the {@link RootPanel} to the fullscreen element
	 * and vice versa.
	 */
	HandlerRegistration fullscreenChangeHandler;

	/**
	 * Lookup table of listener widgets sporting context menus and their
	 * corresponding providers of the context menu contents.
	 */
	protected HashMap<Listener, Registration> participants;

	/**
	 * The handler of mouse over events.
	 */
	HandlerRegistration mouseOverHandler;

	/**
	 * The handler of mouse out events.
	 */
	HandlerRegistration mouseOutHandler;

	/**
	 * Reference to the parent menu. This is <code>null</code> for the shared, top
	 * level context menu.
	 */
	protected ContextMenu parentMenu = null;

	/**
	 * Reference to current child menu or <code>null</code> if no child menu is
	 * visible.
	 */
	protected ContextMenu childMenu = null;

	/**
	 * Shared instance of context menu.
	 */
	protected static ContextMenu contextMenu;

	/**
	 * The context menu is added to the {@link RootPanel} of the GWT application and
	 * shared among all elements that sport a context menu. Always use this shared
	 * instance to build context menus.
	 * 
	 * @return shared instance of context menu.
	 */
	public static ContextMenu sharedContextMenu() {
		if (contextMenu == null) {
			contextMenu = new ContextMenu();
			setupContextMenuLocation(contextMenu);
			setupContextMenuHandlers(contextMenu);
			contextMenu.participants = new HashMap<>();
		}
		return contextMenu;
	}

	/**
	 * Place the context menu in the DOM: in the fullscreen element, if there is
	 * one, otherwise in the {@link RootPanel}.
	 * <p>
	 * <strong>Note:</strong> the menu is moved within the DOM rather than removed
	 * from its parent first, which would break its functionality.
	 * 
	 * @param menu the context menu
	 */
	private static void setupContextMenuLocation(ContextMenu menu) {
		Element fs = NativeJS.getFullscreenElement();
		if (fs != null)
			fs.appendChild(menu.getElement());
		else
			RootPanel.get().add(menu);
		// position fixed can get lost when relocating element
		menu.style.setPosition(Position.FIXED);
	}

	/**
	 * Setup handlers to manage context menu visibility.
	 * 
	 * @param menu the context menu
	 */
	private static void setupContextMenuHandlers(ContextMenu menu) {
		if (NativeJS.isFullscreenSupported()) {
			menu.fullscreenChangeHandler = menu.addFullscreenChangeHandler(menu);
		}
		Window.addWindowScrollHandler(event -> {
			if (menu.isVisible())
				menu.close();
		});
		RootPanel.get().addDomHandler(event -> {
			if (!menu.isVisible())
				return; // context menu not visible - nothing to do
			// IMPORTANT: here is potential for race conditions between contextMenu
			// and its submenus! if mouse is over submenu let submenu handle event
			if (menu.childMenu != null && menu.childMenu.isMouseOver)
				return; // submenu visible and mouse over - let submenu handle event
			if (!menu.isMouseOver && !menu.isAutoHide())
				menu.close();
		}, MouseDownEvent.getType());
		// touches on the menu (including submenus) highlight the item underneath
		// and execute it only once the finger is lifted. preventing the default
		// suppresses emulated clicks (which execute items on touch down) as well as
		// scrolling.
		menu.addDomHandler(event -> {
			event.preventDefault();
			event.stopPropagation();
			menu.touchOver(event.getTouches().get(0));
		}, TouchStartEvent.getType());
		menu.addDomHandler(event -> {
			event.preventDefault();
			event.stopPropagation();
			menu.touchOver(event.getTouches().get(0));
		}, TouchMoveEvent.getType());
		menu.addDomHandler(event -> {
			event.preventDefault();
			event.stopPropagation();
			menu.touchUp(event.getChangedTouches().get(0), true);
		}, TouchEndEvent.getType());
		menu.addDomHandler(event -> menu.touchUp(event.getChangedTouches().get(0), false),
				TouchCancelEvent.getType());
		menu.closeOnOutsideTouch();
		// highlight item underneath pointer (replaces :hover, which iPadOS does not
		// apply for trackpads); mouse moves in submenus bubble up to this menu
		menu.addDomHandler(event -> menu.highlightAt(event.getClientX() + Window.getScrollLeft(),
				event.getClientY() + Window.getScrollTop()), MouseMoveEvent.getType());
	}

	/**
	 * Close the context menu when touching anywhere outside of it, including parts
	 * of the page outside of the GWT application. The listener is registered on
	 * the document in the capture phase such that it fires even if widgets stop
	 * the propagation of touch events.
	 */
	private native void closeOnOutsideTouch() /*-{
		var menu = this;
		$doc.addEventListener("touchstart", $entry(function(event) {
			var elem = menu.@com.google.gwt.user.client.ui.UIObject::getElement()();
			if (elem.style.display != "none" && !elem.contains(event.target))
				menu.@org.evoludo.ui.ContextMenu::close()();
		}), { capture : true, passive : true });
	}-*/;

	/**
	 * Check if context menu is visible.
	 * 
	 * @return <code>true</code> if context menu is showing
	 */
	public static boolean isShowing() {
		// if not yet instantiated, no need to do so now.
		if (contextMenu == null)
			return false;
		return contextMenu.isVisible();
	}

	/**
	 * Gives the visible context-menu hierarchy priority for handling a key event.
	 * If no visible menu item handles the key, the caller may continue with global
	 * keyboard handling.
	 *
	 * @param key       the key value reported by the browser
	 * @param shiftDown {@code true} if Shift is currently pressed
	 * @param altDown   {@code true} if Alt is currently pressed
	 * @return {@code true} if a visible menu item handled the key
	 */
	public static boolean handleKey(String key, boolean shiftDown, boolean altDown) {
		if (!isShowing())
			return false;
		String normalizedKey = ContextMenuItem.normalizeKey(key, shiftDown, altDown);
		if (contextMenu.handleKey(normalizedKey))
			return true;
		if ("Escape".equals(key))
			contextMenu.closeAll();
		return false;
	}

	/**
	 * Create new context menu. Use shared instance to create top level context
	 * menu, {@link #sharedContextMenu()}.
	 */
	protected ContextMenu() {
	}

	@Override
	protected void onLoad() {
		super.onLoad();
		setStyleName("gwt-ContextMenu");
		style = getElement().getStyle();
		style.setDisplay(Display.BLOCK);
		style.setPosition(Position.FIXED);
		style.clearRight();
		style.clearBottom();
		style.clearWidth();
		mouseOverHandler = addDomHandler(this, MouseOverEvent.getType());
		mouseOutHandler = addDomHandler(this, MouseOutEvent.getType());
		close();
	}

	@Override
	protected void onUnload() {
		if (fullscreenChangeHandler != null) {
			fullscreenChangeHandler.removeHandler();
			fullscreenChangeHandler = null;
		}
		if (mouseOverHandler != null) {
			mouseOverHandler.removeHandler();
			mouseOverHandler = null;
		}
		if (mouseOutHandler != null) {
			mouseOutHandler.removeHandler();
			mouseOutHandler = null;
		}
		style = null;
		super.onUnload();
	}

	/**
	 * Create new context submenu for <code>parent</code> menu. Cannot be used to
	 * create top level context menu. Use shared instance instead,
	 * {@link #sharedContextMenu()}.
	 * <p>
	 * <strong>Note:</strong> for submenus only.
	 * 
	 * @param parent menu (cannot be <code>null</code>)
	 */
	public ContextMenu(ContextMenu parent) {
		this();
		if (parent == null)
			throw new IllegalArgumentException("Parent menu cannot be null.");
		parentMenu = parent;
	}

	/**
	 * Create new context submenu for <code>parent</code> menu with a header.
	 * 
	 * @param parent menu (cannot be <code>null</code>)
	 * @param header the header text (may be {@code null} or empty)
	 */
	public ContextMenu(ContextMenu parent, String header) {
		this(parent);
		addHeader(header);
	}

	/**
	 * Register a new <code>listener</code> widget for context menu requests and
	 * associate with the <code>provider</code> of the context menu.
	 * 
	 * @param listener the widget sporting a context menu
	 * @param provider the provider of context menu entries
	 */
	public void addListenerWithProvider(Listener listener, Provider provider) {
		// drop handlers of earlier registration (e.g. when widget is re-attached)
		Registration previous = participants.put(listener, new Registration(listener, provider));
		if (previous != null)
			previous.removeHandler();
	}

	/**
	 * Remove a previously registered <code>listener</code> widget from context menu
	 * requests.
	 * 
	 * @param listener the widget to remove
	 */
	public void removeListener(Listener listener) {
		Registration registration = participants.remove(listener);
		if (registration != null)
			registration.removeHandler();
	}

	/**
	 * Add new separator to context menu.
	 */
	public void addSeparator() {
		if (getWidgetCount() == 0)
			return;
		Widget last = getWidget(getWidgetCount() - 1);
		if (last instanceof ContextMenuSeparator)
			return;
		super.add(new ContextMenuSeparator());
	}

	/**
	 * Add a disabled header item to the context menu.
	 *
	 * @param title header text
	 * @return the header item
	 */
	public ContextMenuHeader addHeader(String title) {
		if (title == null || title.trim().isEmpty())
			return null;
		addSeparator();
		ContextMenuHeader header = new ContextMenuHeader(title);
		add(header);
		addSeparator();
		return header;
	}

	/**
	 * Add new submenu to context menu.
	 * 
	 * @param name    of menu item with submenu
	 * @param submenu to add
	 * @return context menu item that controls submenu
	 */
	public ContextMenuItem add(String name, ContextMenu submenu) {
		ContextMenuItem subMenuItem = new ContextMenuItem(name, submenu);
		add(subMenuItem);
		submenu.parentMenu = this;
		// submenus inherit settings of parent
		submenu.setAutoHide(autoHide);
		submenu.setAutoOpen(autoOpen);
		submenu.setAutoClose(autoClose);
		return subMenuItem;
	}

	/**
	 * Show context menu (or submenu) at position <code>(x, y)</code> relative to
	 * browser window.
	 * 
	 * @param x horizontal position of context menu
	 * @param y vertical position of context menu
	 */
	protected void showAt(int x, int y) {
		style.setLeft((double) x + offsetX, Unit.PX);
		style.setTop((double) y + offsetY, Unit.PX);
		setVisible(true);
	}

	/**
	 * Prepares context menu (or submenu) for showing at position
	 * <code>(x, y)</code> relative to browser window. Retrieves the context menu
	 * provider for the <code>listener</code> widget and asks the provider to supply
	 * context menu for coordinates <code>(x', y')</code> relative to
	 * <code>listener</code> widget. No context menu is shown if no
	 * <code>provider</code> found or if <code>provider</code> did not supply any
	 * context menu entries.
	 * 
	 * @param listener widget that requested the context menu
	 * @param x        horizontal position of context menu
	 * @param y        vertical position of context menu
	 * 
	 * @see Provider#populateContextMenuAt(ContextMenu, int, int)
	 */
	public void showAt(Listener listener, int x, int y) {
		Registration registration = participants.get(listener);
		if (registration == null)
			return; // listener no longer registered (e.g. detached during long touch)
		Provider provider = registration.getProvider();
		if (provider == null)
			return;
		clear();
		provider.populateContextMenuAt(this, x + Window.getScrollLeft() - listener.getAbsoluteLeft(),
				y + Window.getScrollTop() - listener.getAbsoluteTop());
		if (getWidgetCount() < 1)
			return;
		showAt(x, y);
	}

	/**
	 * Open submenu <code>child</code>. If needed, first close any other currently
	 * open submenu.
	 * 
	 * @param child new child menu to open
	 */
	public void openChildMenu(ContextMenu child) {
		closeChildMenu();
		add(child);
		childMenu = child;
	}

	/**
	 * Close submenu, if one is showing.
	 */
	public void closeChildMenu() {
		if (childMenu == null)
			return;
		remove(childMenu);
		if (isMouseOver)
			cancelHideTimer();
		childMenu.onUnload();
		childMenu = null;
	}

	/**
	 * Open submenu.
	 */
	protected void open() {
		// adds submenu to the DOM
		parentMenu.openChildMenu(this);
		cancelHideTimer();
	}

	/**
	 * Close submenu or context menu if this is the top level.
	 */
	public void close() {
		if (isSubmenu()) {
			// close submenu
			parentMenu.closeChildMenu();
			return;
		}
		// this is the top level
		setVisible(false);
	}

	/**
	 * Recursively close hierarchy of context submenus, including the top level
	 * context menu.
	 */
	public void closeAll() {
		close();
		if (isSubmenu())
			parentMenu.closeAll();
	}

	/**
	 * Walks the visible menu hierarchy and executes the first enabled item that
	 * handles the supplied normalized key.
	 *
	 * @param normalizedKey the normalized key representation
	 * @return {@code true} if a visible item handled the key
	 */
	private boolean handleKey(String normalizedKey) {
		if (childMenu != null && childMenu.handleKey(normalizedKey))
			return true;
		for (int n = 0; n < getWidgetCount(); n++) {
			Widget item = getWidget(n);
			if (!(item instanceof ContextMenuItem))
				continue;
			ContextMenuItem menuItem = (ContextMenuItem) item;
			if (!menuItem.handlesKey(normalizedKey))
				continue;
			menuItem.action();
			return true;
		}
		return false;
	}

	/**
	 * Set timer for delayed hiding of context menu if <code>autoClose</code> is
	 * <code>true</code>. This timer is used when pointer exits context (sub)menu.
	 * 
	 * @see #delayHide
	 */
	protected void setHideTimer() {
		if (!autoHide)
			return;
		if (!hideContextMenuTimer.isRunning())
			hideContextMenuTimer.schedule(delayHide);
		if (parentMenu != null)
			parentMenu.setHideTimer();
	}

	/**
	 * Cancel the timer for hiding the context menu. Triggered when pointer
	 * re-enters context (sub)menu before the timer for hiding the menu runs out.
	 * This cascades through the entire (sub)menu hierarchy.
	 */
	protected void cancelHideTimer() {
		hideContextMenuTimer.cancel();
		if (parentMenu != null)
			parentMenu.cancelHideTimer();
	}

	/**
	 * Sets whether context menu hides automatically (with delay) after pointer
	 * exits context menu.
	 * 
	 * @param auto <code>true</code> to automatically hide context menu after delay
	 * 
	 * @see #setAutoClose(boolean)
	 * @see #setDelayHide(int)
	 */
	public void setAutoHide(boolean auto) {
		autoHide = auto;
	}

	/**
	 * Checks if context menu hides automatically (with some delay) after pointer
	 * exited.
	 * 
	 * @return <code>true</code> if context menu hides automatically
	 * 
	 * @see #setAutoHide(boolean)
	 */
	public boolean isAutoHide() {
		return autoHide;
	}

	/**
	 * Sets whether submenus should open automatically when pointer hovers over
	 * corresponding menu item in parent menu.
	 * 
	 * @param auto <code>true</code> to open submenus automatically
	 * 
	 * @see #setAutoClose(boolean)
	 */
	public void setAutoOpen(boolean auto) {
		autoOpen = auto;
	}

	/**
	 * Checks if submenus open automatically when pointer hovers over corresponding
	 * menu item in parent menu.
	 * 
	 * @return <code>true</code> if context menu opens automatically
	 * 
	 * @see #setAutoOpen(boolean)
	 */
	public boolean isAutoOpen() {
		return autoOpen;
	}

	/**
	 * Sets whether submenus should close automatically when pointer stops hovering
	 * over corresponding menu item in parent menu.
	 * 
	 * @param auto <code>true</code> to close submenus automatically
	 * 
	 * @see #setAutoOpen(boolean)
	 */
	public void setAutoClose(boolean auto) {
		autoClose = auto;
	}

	/**
	 * Checks if submenus close automatically when pointer stops hovering over
	 * corresponding menu item in parent menu.
	 * 
	 * @return <code>true</code> if context menu closes automatically
	 * 
	 * @see #setAutoClose(boolean)
	 */
	public boolean isAutoClose() {
		return autoClose;
	}

	/**
	 * Sets the horizontal and vertical offset between the location where the
	 * context menu was triggered and the position of the top left corner of the
	 * context menu. Typically the offset should be negative in both directions to
	 * ensure that once the context menu is visible, the pointer is located inside
	 * the context menu.
	 * 
	 * @param ox the horizontal offset
	 * @param oy the vertical offset
	 * 
	 * @see #offsetX
	 * @see #offsetY
	 */
	public void setOffset(int ox, int oy) {
		offsetX = ox;
		offsetY = oy;
	}

	/**
	 * Set delay before hiding the context (sub)menu (in milliseconds) after the
	 * pointer has exited the context menu, provided that <code>autoHide</code> is
	 * set to <code>true</code>.
	 * 
	 * @param delay the delay for hiding context (sub)menu
	 * 
	 * @see #delayHide
	 */
	public void setDelayHide(int delay) {
		delayHide = delay;
	}

	/**
	 * Set delay (in milliseconds) before showing the context menu after a long
	 * single-finger touch, provided that the finger was neither lifted nor moved
	 * by more than {@link #TOUCH_SLOP} during <code>delay</code>.
	 *
	 * @param delay the delay for showing context menu after long touch
	 * 
	 * @see #delayHide
	 */
	public void setLongTouch(int delay) {
		longTouch = delay;
	}

	/**
	 * Get the parent menu. The parent can be a submenu or {@code null} for the top
	 * level context menu.
	 * 
	 * @return the parent (sub)menu
	 */
	public ContextMenu getParentMenu() {
		return parentMenu;
	}

	/**
	 * Check if this is a submenu. Returns {@code false} only for the top level
	 * context menu
	 * 
	 * @return <code>true</code> if this is a submenu
	 */
	public boolean isSubmenu() {
		return (parentMenu != null);
	}

	/**
	 * Timer for closing the context menu after a fixed delay.
	 * 
	 * @see #DELAY_HIDE
	 */
	private Timer hideContextMenuTimer = new Timer() {
		@Override
		public void run() {
			close();
		}
	};

	/**
	 * {@inheritDoc}
	 * <p>
	 * Show the context menu at the pointer location. Browsers that additionally
	 * fire a native {@code contextmenu} event on long touches (e.g. Chrome on
	 * Android) would otherwise show the menu twice, which clears and repopulates
	 * the one already showing. Hence, the event is ignored while the finger that
	 * opened the menu is still down.
	 */
	@Override
	public void onContextMenu(ContextMenuEvent event) {
		Object src = event.getSource();
		if (!(src instanceof Listener))
			return;
		event.stopPropagation();
		event.preventDefault();
		if (touchTimer.listener != null && isVisible())
			return; // menu already showing for this long touch
		touchTimer.cancel();
		showAt((Listener) src, event.getNativeEvent().getClientX(), event.getNativeEvent().getClientY());
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * Pointer (re-)entered context (sub)menu. Cancel timer to hide (sub)menu, in
	 * case it was running.
	 */
	@Override
	public void onMouseOver(MouseOverEvent event) {
		isMouseOver = true;
		cancelHideTimer();
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * Pointer exited context (sub)menu. Update highlighted item (if any) and start
	 * timer to hide (sub)menu. Note, GWT fires mouse over and out events only when
	 * entering or exiting the widget but not when moving between its children,
	 * i.e. menu items. Highlights inside the menu are updated on mouse moves.
	 */
	@Override
	public void onMouseOut(MouseOutEvent event) {
		isMouseOver = false;
		setHideTimer();
		contextMenu.highlightAt(event.getClientX() + Window.getScrollLeft(), event.getClientY() + Window.getScrollTop());
	}

	/**
	 * Timer for handling touch events triggering the context menu. Used to save the
	 * position where the context menu was requested as well as the listening widget
	 * for use once the timer fires (if it does).
	 */
	public class TouchTimer extends Timer {

		/**
		 * Horizontal position of touch event scheduled to trigger context menu
		 * (relative to browser window).
		 */
		int touchX = -1;

		/**
		 * Vertical position of touch event scheduled to trigger context menu (relative
		 * to browser window).
		 */
		int touchY = -1;

		/**
		 * Listening widget that triggered the scheduling for the context menu. Reset
		 * to <code>null</code> when the touch ends, i.e. non-<code>null</code> while a
		 * single-finger touch is in progress.
		 */
		Listener listener;

		/**
		 * Save the listener widget as well as the position (relative to browser window)
		 * where the context menu will be displayed when (and if) the timer fires.
		 * 
		 * @param ref listener widget that is (potentially) requesting a context menu
		 * @param x   horizontal position of touch event
		 * @param y   vertical position of touch event
		 */
		public void save(Listener ref, int x, int y) {
			listener = ref;
			touchX = x;
			touchY = y;
		}

		/**
		 * {@inheritDoc}
		 * <p>
		 * Show context menu.
		 */
		@Override
		public void run() {
			showAt(listener, touchX, touchY);
		}
	}

	/**
	 * Timer for handling touch events that are scheduled to trigger context menu.
	 */
	protected TouchTimer touchTimer = new TouchTimer();

	/**
	 * {@inheritDoc}
	 * <p>
	 * A single finger touch schedules the context menu to open after
	 * {@link #longTouch} milliseconds, unless the finger is lifted or moves in the
	 * meantime. Additional fingers cancel the request. Any touch closes the context
	 * menu first, hence a visible context menu during a touch was opened by that
	 * touch.
	 * <p>
	 * <strong>Note:</strong> gesture events are proprietary implementations by
	 * Apple. Let's try to do without and stick to more primitive touch events.
	 */
	@Override
	public void onTouchStart(TouchStartEvent event) {
		contextMenu.close();
		JsArray<Touch> touches = event.getTouches();
		if (touches.length() > 1) {
			touchTimer.cancel();
			touchTimer.listener = null;
			return;
		}
		Object src = event.getSource();
		if (!(src instanceof Listener))
			return; // should not happen
		Touch touch = touches.get(0);
		touchTimer.save((Listener) src, touch.getClientX(), touch.getClientY());
		touchTimer.schedule(longTouch);
		// NOTE: this prevents scrolling of console
		// event.stopPropagation()
		// event.preventDefault()
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * One or more touches moved. Cancel request to show context menu (if
	 * scheduled), unless a single touch merely jittered within {@link #TOUCH_SLOP}
	 * of where it started. Once the context menu is open, the same finger selects
	 * menu items: the item underneath is highlighted and submenus open, while
	 * default actions such as scrolling are suppressed. Note, touch events keep
	 * targeting the listener widget where the touch started and never reach the
	 * menu items.
	 * <p>
	 * <strong>Note:</strong> gesture events are proprietary implementations by
	 * Apple. Let's try to do without and stick to more primitive touch events.
	 */
	@Override
	public void onTouchMove(TouchMoveEvent event) {
		JsArray<Touch> touches = event.getTouches();
		Touch touch = touches.get(0);
		if (isVisible()) {
			// menu opened by this touch - keep it open and track finger
			event.preventDefault();
			if (!isStill(touch))
				touchOver(touch);
			return;
		}
		if (touches.length() == 1 && isStill(touch))
			return;
		// cancel context menu
		touchTimer.cancel();
	}

	/**
	 * Check whether <code>touch</code> is still within {@link #TOUCH_SLOP} of the
	 * location where the touch started.
	 *
	 * @param touch the touch to check
	 * @return <code>true</code> if the touch has not moved significantly
	 */
	private boolean isStill(Touch touch) {
		return Math.abs(touch.getClientX() - touchTimer.touchX) <= TOUCH_SLOP
				&& Math.abs(touch.getClientY() - touchTimer.touchY) <= TOUCH_SLOP;
	}

	/**
	 * Highlight the menu item underneath <code>touch</code> and open its submenu
	 * (or close other submenus), which emulates hovering for touches.
	 *
	 * @param touch the touch over the menu
	 */
	private void touchOver(Touch touch) {
		ContextMenuItem item = highlightAt(touch.getPageX(), touch.getPageY());
		if (item != null)
			item.onMouseOver(null);
	}

	/**
	 * The finger of <code>touch</code> was lifted: clear all highlights and, if
	 * <code>select</code> is <code>true</code>, execute the menu item underneath.
	 * Nothing is executed if the finger was lifted outside the menu, which then
	 * stays open.
	 *
	 * @param touch  the touch that ended
	 * @param select <code>true</code> to execute the menu item underneath
	 */
	private void touchUp(Touch touch, boolean select) {
		ContextMenuItem item = highlightAt(touch.getPageX(), touch.getPageY());
		// clear highlights (menu items may be reused)
		highlightAt(Integer.MIN_VALUE, Integer.MIN_VALUE);
		if (select && item != null)
			item.action();
	}

	/**
	 * Highlight the enabled menu item at page coordinates <code>(x, y)</code> and
	 * clear the highlights of all other items, including those of open submenus.
	 * Submenus are checked first because they overlay their parent menu.
	 *
	 * @param x horizontal page coordinate
	 * @param y vertical page coordinate
	 * @return the highlighted menu item or <code>null</code> if there is none
	 */
	private ContextMenuItem highlightAt(int x, int y) {
		ContextMenuItem hit = null;
		for (int n = getWidgetCount() - 1; n >= 0; n--) {
			Widget widget = getWidget(n);
			if (widget instanceof ContextMenu) {
				ContextMenuItem sub = ((ContextMenu) widget).highlightAt(x, y);
				if (hit == null)
					hit = sub;
				continue;
			}
			if (!(widget instanceof ContextMenuItem))
				continue;
			ContextMenuItem item = (ContextMenuItem) widget;
			int dx = x - item.getAbsoluteLeft();
			int dy = y - item.getAbsoluteTop();
			boolean over = hit == null && item.isEnabled() && dx >= 0 && dx < item.getOffsetWidth() && dy >= 0
					&& dy < item.getOffsetHeight();
			item.setStyleName(HIGHLIGHT_STYLE, over);
			if (over)
				hit = item;
		}
		return hit;
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * One or more touches ended. Cancel request to show context menu (if
	 * scheduled). If the context menu was opened by this touch, the menu item
	 * underneath the lifted finger is selected, unless the finger never moved.
	 * Then the menu stays open for selections by tapping. In addition, the default
	 * action is prevented to suppress emulated mouse events and clicks, which would
	 * otherwise hit the context menu now located underneath the finger.
	 * <p>
	 * <strong>Note:</strong> gesture events are proprietary implementations by
	 * Apple. Let's try to do without and stick to more primitive touch events.
	 */
	@Override
	public void onTouchEnd(TouchEndEvent event) {
		// at least one finger left device - cancel context menu
		touchTimer.cancel();
		touchTimer.listener = null;
		if (!isVisible())
			return;
		event.preventDefault();
		Touch touch = event.getChangedTouches().get(0);
		touchUp(touch, !isStill(touch));
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * Touch canceled. Cancel request to show context menu (if scheduled).
	 * <p>
	 * <strong>Note:</strong> gesture events are proprietary implementations by
	 * Apple. Let's try to do without and stick to more primitive touch events.
	 */
	@Override
	public void onTouchCancel(TouchCancelEvent event) {
		// cancel context menu
		touchTimer.cancel();
		touchTimer.listener = null;
	}

	@Override
	public HandlerRegistration addFullscreenChangeHandler(FullscreenChangeHandler handler) {
		String eventname = NativeJS.fullscreenChangeEventName();
		NativeJS.addFullscreenChangeHandler(eventname, handler);
		return () -> NativeJS.removeFullscreenChangeHandler(eventname, handler);
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * Placement of shared context menu element in the DOM needs to be adjusted when
	 * entering or exiting fullscreen mode.
	 */
	@Override
	public void onFullscreenChange(FullscreenChangeEvent event) {
		// note: if we want to be smart, we can relocate the context menu only if the
		// fullscreen element is among our listeners. worth it? see also Tooltip
		if (contextMenu != null)
			setupContextMenuLocation(contextMenu);
	}
}
