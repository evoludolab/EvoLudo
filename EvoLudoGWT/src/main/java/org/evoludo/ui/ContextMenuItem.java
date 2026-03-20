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

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.ContextMenuEvent;
import com.google.gwt.event.dom.client.ContextMenuHandler;
import com.google.gwt.event.dom.client.MouseOutEvent;
import com.google.gwt.event.dom.client.MouseOutHandler;
import com.google.gwt.event.dom.client.MouseOverEvent;
import com.google.gwt.event.dom.client.MouseOverHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.HasEnabled;
import com.google.gwt.user.client.ui.Label;

/**
 * Component of the context menu extension to GWT's user interface.
 * <p>
 * Represents a menu item in the context menu. The context menu item may either
 * execute a command when selected or control the visibility of a submenu.
 * 
 * <h2>CSS Style Rules</h2>
 * <dl>
 * <dt>.gwt-ContextMenuItem</dt>
 * <dd>the context menu item element.</dd>
 * <dt>.gwt-ContextMenuItem-disabled</dt>
 * <dd>if the context menu item is disabled.</dd>
 * <dt>.submenu</dt>
 * <dd>if the context menu item controls a submenu.</dd>
 * </dl>
 * 
 * @author Christoph Hauert
 * 
 * @see ContextMenu
 */
public class ContextMenuItem extends Label
		implements HasEnabled, MouseOverHandler, MouseOutHandler, ClickHandler, ContextMenuHandler {

	/**
	 * Prefix indicating that the keyboard key requires the Shift modifier.
	 */
	private static final String KEY_SHIFT_PREFIX = "⇧";

	/**
	 * Prefix indicating that the keyboard key requires the Alt modifier.
	 */
	private static final String KEY_ALT_PREFIX = "⎇";

	/**
	 * Attribute used to expose keyboard keys to CSS.
	 */
	private static final String ATTR_KEY = "data-key";

	/**
	 * Command that gets executed when selecting this menu item and
	 * <code>null</code> if this menu item controls a submenu.
	 */
	Scheduler.ScheduledCommand cmd;

	/**
	 * Reference to submenu if this menu item controls one and <code>null</code>
	 * otherwise.
	 */
	private ContextMenu childMenu = null;

	/**
	 * The handler of mouse over events to open submenu.
	 */
	HandlerRegistration mouseOverHandler;

	/**
	 * The handler of mouse out events to close submenu.
	 */
	HandlerRegistration mouseOutHandler;

	/**
	 * The handler of click events.
	 */
	HandlerRegistration clickHandler;

	/**
	 * The handler of context menu events.
	 */
	HandlerRegistration contextMenuHandler;

	/**
	 * Flag to indicate whether menu item is enabled.
	 */
	private boolean isEnabled = true;

	/**
	 * Keyboard key text rendered in the trailing column of the menu item.
	 */
	private String key;

	/**
	 * Normalized keyboard key used for comparisons during key handling.
	 */
	private String normalizedKey = "";

	/**
	 * Create a new context menu item with the title <code>name</code> and triggers
	 * the command <code>cmd</code> when selected.
	 * 
	 * @param name title of context menu item
	 * @param cmd  command to execute when selected
	 */
	public ContextMenuItem(String name, Scheduler.ScheduledCommand cmd) {
		this(name, cmd, null);
	}

	/**
	 * Create a new context menu item with the title <code>name</code>, triggers
	 * the command <code>cmd</code> when selected and displays the keyboard
	 * <code>key</code>.
	 * 
	 * @param name title of context menu item
	 * @param cmd  command to execute when selected
	 * @param key  keyboard key label shown in the trailing column
	 */
	public ContextMenuItem(String name, Scheduler.ScheduledCommand cmd, String key) {
		super(name, false);
		this.cmd = cmd;
		this.key = key;
	}

	/**
	 * Create a new context menu item with the title <code>name</code> that controls
	 * the submenu <code>child</code>. For CSS styling, context menu items that
	 * control a submenu have the class <code>submenu</code> added.
	 * 
	 * @param name  title of context menu item
	 * @param child submenu to control
	 */
	public ContextMenuItem(String name, ContextMenu child) {
		this(name, (Scheduler.ScheduledCommand) null);
		if (child == null)
			throw new IllegalArgumentException("child submenu must not be null");
		childMenu = child;
	}

	@Override
	protected void onLoad() {
		super.onLoad();
		setStyleName("gwt-ContextMenuItem");
		setKey(key);
		if (!isEnabled)
			setStyleDependentName("disabled", true);
		clickHandler = addClickHandler(this);
		contextMenuHandler = addDomHandler(this, ContextMenuEvent.getType());
		if (hasSubmenu()) {
			mouseOverHandler = addDomHandler(this, MouseOverEvent.getType());
			mouseOutHandler = addDomHandler(this, MouseOutEvent.getType());
			addStyleName("submenu");
		}
	}

	@Override
	protected void onUnload() {
		if (clickHandler != null)
			clickHandler.removeHandler();
		if (contextMenuHandler != null)
			contextMenuHandler.removeHandler();
		if (hasSubmenu()) {
			if (mouseOverHandler != null)
				mouseOverHandler.removeHandler();
			if (mouseOutHandler != null)
				mouseOutHandler.removeHandler();
		}
		super.onUnload();
	}

	/**
	 * Entry method when context menu item is selected. Nothing happens if the
	 * context menu item is disabled. If it controls a submenu then the submenu is
	 * opened or closed, respectively. Otherwise, the assigned command is executed
	 * and the surrounding menu hierarchy is closed.
	 */
	public void action() {
		if (!isEnabled)
			return;
		if (childMenu != null) {
			// toggle submenu if not auto open/close
			if (childMenu.isAttached()) {
				if (!childMenu.isAutoClose())
					closeNow();
			} else if (!childMenu.isAutoOpen()) {
				openSubmenu();
			}
			return;
		}
		if (cmd != null)
			Scheduler.get().scheduleFinally(cmd);
		if (!hasSubmenu())
			((ContextMenu) getParent()).closeAll();
	}

	/**
	 * Open submenu.
	 */
	private void openSubmenu() {
		if (childMenu == null || !isEnabled)
			return;
		childMenu.onLoad();
		childMenu.setOffset(-2, -2);
		childMenu.showAt(getElement().getAbsoluteLeft() + getOffsetWidth() - Window.getScrollLeft(),
				getElement().getAbsoluteTop() - Window.getScrollTop());
		childMenu.open();
	}

	/**
	 * Open submenu if context menu item is enabled and controls a submenu with
	 * <code>autoOpen</code> set to <code>true</code>.
	 * 
	 * @return <code>true</code> if submenu has been opened and <code>false</code>
	 *         otherwise
	 */
	public boolean open() {
		if (!isEnabled || childMenu == null)
			return false;
		if (hasSubmenu() && childMenu.isAutoOpen())
			openSubmenu();
		return true;
	}

	/**
	 * Sets the keyboard key label shown on the right side of the menu item.
	 * Menu items controlling submenus ignore keys.
	 *
	 * @param key the key label, or {@code null} to clear it
	 */
	public void setKey(String key) {
		this.key = (hasSubmenu() || key == null ? null : key.trim());
		normalizedKey = normalizeKey(this.key, false, false);
		if (this.key == null || this.key.isEmpty()) {
			getElement().removeAttribute(ATTR_KEY);
			removeStyleName("has-key");
			return;
		}
		getElement().setAttribute(ATTR_KEY, this.key);
		addStyleName("has-key");
	}

	/**
	 * Checks whether this menu item handles the supplied normalized key.
	 *
	 * @param normalizedKey the normalized key string to compare against
	 * @return {@code true} if this item handles the key
	 */
	public boolean handlesKey(String normalizedKey) {
		return isEnabled && !this.normalizedKey.isEmpty() && this.normalizedKey.equals(normalizedKey);
	}

	/**
	 * Normalizes keyboard key labels and pressed browser keys to a common
	 * comparison format.
	 *
	 * @param key       the displayed or pressed key
	 * @param shiftDown {@code true} if Shift is currently pressed
	 * @param altDown   {@code true} if Alt is currently pressed
	 * @return normalized key representation
	 */
	static String normalizeKey(String key, boolean shiftDown, boolean altDown) {
		if (key == null)
			return "";
		String normalizedKey = key.trim();
		if (normalizedKey.isEmpty())
			return "";
		boolean requiresShift = shiftDown || normalizedKey.contains(KEY_SHIFT_PREFIX);
		boolean requiresAlt = altDown || normalizedKey.contains(KEY_ALT_PREFIX);
		normalizedKey = normalizedKey.replace(KEY_SHIFT_PREFIX, "");
		normalizedKey = normalizedKey.replace(KEY_ALT_PREFIX, "");
		switch (normalizedKey.trim()) {
			case "Enter":
				normalizedKey = "↵";
				break;
			case "ArrowUp":
				normalizedKey = "↑";
				break;
			case "ArrowDown":
				normalizedKey = "↓";
				break;
			case "ArrowLeft":
				normalizedKey = "←";
				break;
			case "ArrowRight":
				normalizedKey = "→";
				break;
			case "Escape":
			case "Esc":
				normalizedKey = "Esc";
				break;
			default:
				normalizedKey = normalizedKey.trim();
				if (normalizedKey.length() == 1)
					normalizedKey = normalizedKey.toUpperCase();
				break;
		}
		return (requiresShift ? KEY_SHIFT_PREFIX : "") + (requiresAlt ? KEY_ALT_PREFIX : "") + normalizedKey;
	}

	/**
	 * Set timer to close submenu, if there is one, otherwise close this context
	 * menu without delay.
	 */
	public void close() {
		if (childMenu == null) {
			((ContextMenu) getParent()).close();
			return;
		}
		childMenu.setHideTimer();
	}

	/**
	 * Close submenu without delay.
	 */
	protected void closeNow() {
		if (childMenu == null)
			return;
		childMenu.close();
	}

	@Override
	public void setEnabled(boolean enabled) {
		setStyleDependentName("disabled", !enabled);
		this.isEnabled = enabled;
	}

	@Override
	public boolean isEnabled() {
		return isEnabled;
	}

	/**
	 * Check if context menu item controls a submenu.
	 * 
	 * @return <code>true</code> if menu item has a submenu
	 */
	public boolean hasSubmenu() {
		return (childMenu != null);
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * Close submenu, if context menu item controls one, and if the submenu has
	 * <code>autoClose</code> set to <code>true</code>, unless pointer now hovers
	 * over submenu.
	 */
	@Override
	public void onMouseOut(MouseOutEvent event) {
		// close childMenu, unless pointer moved into it!
		if (hasSubmenu() && childMenu.isAutoClose()) {
			int x = getAbsoluteLeft() - childMenu.getAbsoluteLeft() + event.getX();
			if (x < 0 || x > childMenu.getOffsetWidth()) {
				closeNow();
				return;
			}
			int y = getAbsoluteTop() - childMenu.getAbsoluteTop() + event.getY();
			if (y < 0 || y > childMenu.getOffsetHeight()) {
				closeNow();
			}
		}
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * Open submenu, if context menu item controls one, and if the submenu has
	 * <code>autoOpen</code> set to <code>true</code>. Otherwise do nothing.
	 */
	@Override
	public void onMouseOver(MouseOverEvent event) {
		if (!hasSubmenu())
			((ContextMenu) getParent()).closeChildMenu();
		if (!childMenu.isAttached())
			open();
		else
			childMenu.cancelHideTimer();
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * Context menu item selected. Execute associated action or toggle visibility of
	 * submenu <code>autoOpen</code> and <code>autoClose</code> are
	 * <code>false</code>.
	 * 
	 * @see #action()
	 */
	@Override
	public void onClick(ClickEvent event) {
		action();
		// needed to prevent page flips in ePubs
		event.stopPropagation();
		event.preventDefault();
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * Suppress default context menu.
	 */
	@Override
	public void onContextMenu(ContextMenuEvent event) {
		event.stopPropagation();
		event.preventDefault();
	}
}
