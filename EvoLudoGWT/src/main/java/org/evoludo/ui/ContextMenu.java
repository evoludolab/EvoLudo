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
import com.google.gwt.event.dom.client.MouseDownHandler;
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
import com.google.gwt.user.client.Window.ScrollEvent;
import com.google.gwt.user.client.Window.ScrollHandler;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.RootPanel;

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
 * a two-finger touch (after some configurable delay). The context menu is
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
 * 
 * @author Christoph Hauert
 */
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
	 * Reference to style of context menu. Most important to control positioning of
	 * context menu.
	 */
	protected Style style;

	/**
	 * <code>true</code> to automatically set timer for hiding context menu after
	 * pointer exited context menu.
	 */
	private boolean autoHide = false;

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
			// context menu needs to be added in different places of the DOM
			// depending on whether fullscreen mode is active
			Element fs = NativeJS.getFullscreenElement();
			if (fs != null)
				fs.appendChild(contextMenu.getElement());
			else
				RootPanel.get().add(contextMenu);
			contextMenu.style.setPosition(Position.FIXED);
			// add fullscreen change handler to shared instance of context menu
			if (NativeJS.isFullscreenSupported())
				contextMenu.fullscreenChangeHandler = contextMenu.addFullscreenChangeHandler(contextMenu);
			// add window scroll change handler to shared instance of context menu
			Window.addWindowScrollHandler(new ScrollHandler() {
				@Override
				public void onWindowScroll(ScrollEvent event) {
					if (contextMenu.isVisible())
						contextMenu.close();
				}
			});
			// add mouse down handler to root panel to close context menu if
			// autoClose==false and mouse currently not over context (sub)menu
			RootPanel.get().addDomHandler(new MouseDownHandler() {
				@Override
				public void onMouseDown(MouseDownEvent event) {
					if (!contextMenu.isVisible())
						return; // context menu not visible - nothing to do
					// IMPORTANT: here is potential for race conditions between contextMenu
					// and its submenus! if mouse is over submenu let submenu handle event
					if (contextMenu.childMenu != null && contextMenu.childMenu.isMouseOver)
						return; // submenu visible and mouse over - let submenu handle event
					if (!contextMenu.isMouseOver && !contextMenu.isAutoHide())
						contextMenu.close();
				}
			}, MouseDownEvent.getType());
			contextMenu.participants = new HashMap<Listener, Registration>();
		}
		return contextMenu;
	}

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
	 * Create new context menu. Use shared instance to create top level context
	 * menu, {@link #sharedContextMenu()}.
	 */
	protected ContextMenu() {
		setStyleName("gwt-ContextMenu");
		style = getElement().getStyle();
		style.setDisplay(Display.BLOCK);
		style.setPosition(Position.FIXED);
		style.clearRight();
		style.clearBottom();
		style.clearWidth();
		addDomHandler(this, MouseOverEvent.getType());
		addDomHandler(this, MouseOutEvent.getType());
		close();
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
		assert parent != null;
		parentMenu = parent;
	}

	/**
	 * Register a new <code>listener</code> widget for context menu requests and
	 * associate with the <code>provider</code> of the context menu.
	 * 
	 * @param listener widget sporting a context menu
	 * @param provider provides context menu entries for this listener widget
	 */
	public void add(Listener listener, Provider provider) {
		participants.put(listener, new Registration(listener, provider));
	}

	/**
	 * Add new separator to context menu.
	 */
	public void addSeparator() {
		super.add(new ContextMenuSeparator());
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
		style.setLeft(x + offsetX, Unit.PX);
		style.setTop(y + offsetY, Unit.PX);
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
		Provider provider = participants.get(listener).getProvider();
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
		if (isSubmenu()) {
			// close submenu
			parentMenu.closeChildMenu();
			parentMenu.closeAll();
			return;
		}
		// this is the top level
		setVisible(false);
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
	 * Set delay (in milliseconds) before showing the context menu after a two
	 * finger touch, provided that no other touch events fired during
	 * <code>delay</code>.
	 * 
	 * @param delay the delay for showing context menu after two-finger touch
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

	@Override
	public void onContextMenu(ContextMenuEvent event) {
		Object src = event.getSource();
		if (!(src instanceof Listener))
			return;
		showAt((Listener) src, event.getNativeEvent().getClientX(), event.getNativeEvent().getClientY());
		event.stopPropagation();
		event.preventDefault();
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
	 * Pointer exited context (sub)menu. Start timer to hide (sub)menu.
	 */
	@Override
	public void onMouseOut(MouseOutEvent event) {
		isMouseOver = false;
		setHideTimer();
	}

	/**
	 * Timer for handling touch events triggering the context menu. Used to save the
	 * position where the context menu was requested as well as the listening widget
	 * for use once the timer fires (if it does).
	 */
	public class TouchTimer extends Timer {

		/**
		 * Constructs a new timer for touch events.
		 */
		public TouchTimer() {
		}

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
		 * Listening widget that triggered the scheduling for the context menu.
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
	 * <strong>Note:</strong> gesture events are proprietary implementations by
	 * Apple. Let's try to do without and stick to more primitive touch events.
	 */
	@Override
	public void onTouchStart(TouchStartEvent event) {
		contextMenu.close();
		JsArray<Touch> touches = event.getTouches();
		if (touches.length() > 1) {
			touchTimer.cancel();
			return;
		}
		Object src = event.getSource();
		if (!(src instanceof Listener))
			return; // should not happen
		Touch touch = touches.get(0);
		touchTimer.save((Listener) src, touch.getClientX(), touch.getClientY());
		touchTimer.schedule(longTouch);
		// NOTE: this prevents scrolling of console
		// event.stopPropagation();
		// event.preventDefault();
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * One or more touches moved. Cancel request to show context menu (if
	 * scheduled).
	 * <p>
	 * <strong>Note:</strong> gesture events are proprietary implementations by
	 * Apple. Let's try to do without and stick to more primitive touch events.
	 */
	@Override
	public void onTouchMove(TouchMoveEvent event) {
		// cancel context menu
		touchTimer.cancel();
		contextMenu.close();
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * One or more touches ended. Cancel request to show context menu (if
	 * scheduled).
	 * <p>
	 * <strong>Note:</strong> gesture events are proprietary implementations by
	 * Apple. Let's try to do without and stick to more primitive touch events.
	 */
	@Override
	public void onTouchEnd(TouchEndEvent event) {
		// at least one finger left device - cancel context menu
		touchTimer.cancel();
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
	}

	@Override
	public HandlerRegistration addFullscreenChangeHandler(FullscreenChangeHandler handler) {
		String eventname = NativeJS.fullscreenChangeEventName();
		NativeJS.addFullscreenChangeHandler(eventname, handler);
		return new HandlerRegistration() {
			@Override
			public void removeHandler() {
				NativeJS.removeFullscreenChangeHandler(eventname, handler);
			}
		};
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * Placement of shared context menu element in the DOM needs to be adjusted when
	 * entering or exiting fullscreen mode.
	 */
	@Override
	public void onFullscreenChange(FullscreenChangeEvent event) {
		if (contextMenu == null)
			return;
		// note: if we want to be smart, we can relocate the context menu only if the
		// fullscreen element is among our listeners. worth it? see also Tooltip
		Element fs = NativeJS.getFullscreenElement();
		// note: do not remove contextMenu from parent! this breaks it's functionality!
		// instead, just move contextMenu within the DOM.
		if (fs != null) {
			// entered fullscreen
			fs.appendChild(contextMenu.getElement());
		} else {
			// exited fullscreen
			RootPanel.get().add(contextMenu);
		}
		// position fixed can get lost when relocating element
		contextMenu.style.setPosition(Position.FIXED);
	}
}
