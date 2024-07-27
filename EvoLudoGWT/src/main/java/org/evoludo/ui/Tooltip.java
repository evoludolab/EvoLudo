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
import com.google.gwt.event.dom.client.MouseEvent;
import com.google.gwt.event.dom.client.MouseMoveEvent;
import com.google.gwt.event.dom.client.MouseMoveHandler;
import com.google.gwt.event.dom.client.MouseOutEvent;
import com.google.gwt.event.dom.client.MouseOutHandler;
import com.google.gwt.event.dom.client.MouseOverEvent;
import com.google.gwt.event.dom.client.MouseOverHandler;
import com.google.gwt.event.dom.client.MouseWheelEvent;
import com.google.gwt.event.dom.client.MouseWheelHandler;
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
import com.google.gwt.user.client.ui.FocusPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.RootPanel;

/**
 * Tooltip extension to GWT's user interface.
 * <p>
 * Add customizable tooltips to GWT applications. Tooltips work through a shared
 * instance (like {@link ContextMenu}'s). Each widget that sports tooltips (the
 * <code>listener</code> widget) must be associated with a tooltip
 * <code>provider</code>.
 * <p>
 * The tooltip is displayed when the pointer hovers over the
 * <code>listener</code> widget and the tooltip <code>provider</code> returns a
 * non-<code>null</code> string. For devices without pointers the tooltip
 * display is triggered by a one-finger tap. Another tap re-positions or closes
 * the tooltip. The delay for opening the tooltip as well as the timeout for
 * showing it can be configured. The tooltip closes when the {@link ContextMenu}
 * opens.
 * 
 * <h2>CSS Style Rules</h2>
 * <dl>
 * <dt>.gwt-Tooltip</dt>
 * <dd>the tooltip element.</dd>
 * </dl>
 * 
 * @author Christoph Hauert
 */
public class Tooltip extends HTML implements MouseOverHandler, MouseOutHandler, MouseMoveHandler, MouseWheelHandler,
		TouchStartHandler, TouchMoveHandler, TouchEndHandler, FullscreenChangeHandler, HasFullscreenChangeHandlers {

	/**
	 * Provider interface for classes that are responsible for supplying the
	 * contents of the tooltip.
	 */
	public interface Provider {

		/**
		 * Get the tooltip information for the location with coordinates {@code (x, y)}.
		 * The returned string may include HTML elements for formatting.
		 * 
		 * @param x the {@code x}-coordinate for the tooltip
		 * @param y the {@code y}-coordinate for the tooltip
		 * @return the (formatted) string with the tooltip info
		 */
		public String getTooltipAt(int x, int y);
	}

	/**
	 * Manage registrations of listener widgets, their tooltip providers and the
	 * corresponding event handlers. Used for a lookup table to match listeners and
	 * providers with the listeners serving as the lookup key.
	 */
	protected class Registration implements HandlerRegistration {

		/**
		 * References to all event handlers relevant for handling tooltips registered
		 * with the listener widget.
		 */
		HandlerRegistration mouseOverHandler, mouseOutHandler, mouseMoveHandler, mouseWheelHandler, touchStartHandler,
				touchMoveHandler, touchEndHandler;

		/**
		 * Provider of tooltips for this listener widget.
		 */
		Provider provider;

		/**
		 * Listener widget for tooltips.
		 */
		FocusPanel listener;

		/**
		 * Register all event handlers relevant for handling tooltips with
		 * <code>listener</code> widget and associate with the tooltip
		 * <code>provider</code>.
		 * 
		 * @param listener widget sporting tooltips
		 * @param provider provides tooltips for this listener widget
		 */
		public Registration(FocusPanel listener, Provider provider) {
			this.listener = listener;
			this.provider = provider;
			// add mouse handlers
			mouseOverHandler = listener.addMouseOverHandler(Tooltip.this);
			mouseOutHandler = listener.addMouseOutHandler(Tooltip.this);
			mouseMoveHandler = listener.addMouseMoveHandler(Tooltip.this);
			mouseWheelHandler = listener.addMouseWheelHandler(Tooltip.this);
			// add touch handlers
			touchStartHandler = listener.addTouchStartHandler(Tooltip.this);
			touchEndHandler = listener.addTouchEndHandler(Tooltip.this);
			touchMoveHandler = listener.addTouchMoveHandler(Tooltip.this);
		}

		@Override
		public void removeHandler() {
			if (mouseOverHandler != null)
				mouseOverHandler.removeHandler();
			if (mouseOutHandler != null)
				mouseOutHandler.removeHandler();
			if (mouseMoveHandler != null)
				mouseMoveHandler.removeHandler();
			if (mouseWheelHandler != null)
				mouseWheelHandler.removeHandler();
			if (touchStartHandler != null)
				touchStartHandler.removeHandler();
			if (touchEndHandler != null)
				touchEndHandler.removeHandler();
			if (touchMoveHandler != null)
				touchMoveHandler.removeHandler();
		}
	}

	/**
	 * Default horizontal offset of tooltip relative to the coordinates of the
	 * pointer (or the tap that triggered the tooltip).
	 * 
	 * @see #offsetX
	 */
	private static final int DEFAULT_OFFSET_X = 0;

	/**
	 * Horizontal offset of tooltip relative to coordinates of pointer or the
	 * location of the tap triggering the tooltip, respectively. Negative offsets
	 * shift to the left. In general, this should be a non-negative number such that
	 * the pointer location is not obscured by the tooltip.
	 * 
	 * @see #DEFAULT_OFFSET_X
	 * @see #setOffset(int, int)
	 */
	private int offsetX = DEFAULT_OFFSET_X;

	/**
	 * Default vertical offset of tooltip relative to the coordinates of the pointer
	 * (or the tap that triggered the tooltip).
	 * 
	 * @see #offsetY
	 */
	private static final int DEFAULT_OFFSET_Y = 8;

	/**
	 * Vertical offset of tooltip relative to coordinates of pointer or the location
	 * of the tap triggering the tooltip, respectively. Negative offsets shift to
	 * the top. In general, this should be a non-negative number such that the
	 * pointer location is not obscured by the tooltip.
	 * 
	 * @see #DEFAULT_OFFSET_X
	 * @see #setOffset(int, int)
	 */
	private int offsetY = DEFAULT_OFFSET_Y;

	/**
	 * Default delay before showing the tooltip (in milliseconds) after the pointer
	 * started to hover over (part of) element that supplies tooltip.
	 */
	private static final int TOOLTIP_DEFAULT_DELAY_SHOW = 1000;

	/**
	 * Delay before showing the tooltip (in milliseconds) after the pointer started
	 * to hover over (part of) element that supplies tooltip. Does not apply to
	 * tooltips triggered by taps.
	 * 
	 * @see #TOOLTIP_DEFAULT_DELAY_SHOW
	 * @see #setDelays(int, int)
	 */
	private int delayShow = TOOLTIP_DEFAULT_DELAY_SHOW;

	/**
	 * Default time until tooltip expires and closes (in milliseconds).
	 */
	private static final int TOOLTIP_DEFAULT_DELAY_HIDE = 10000;

	/**
	 * Time until tooltip expires and closes (in milliseconds).
	 * 
	 * @see #TOOLTIP_DEFAULT_DELAY_HIDE
	 * @see #setDelays(int, int)
	 */
	private int delayHide = TOOLTIP_DEFAULT_DELAY_HIDE;

	/**
	 * Horizontal offset for tooltip placement relative to tap that triggered it.
	 * Negative offsets shift to the left.
	 */
	protected static final int TOUCH_SHIFT_X = -3;

	/**
	 * Vertical offset for tooltip placement relative to tap that triggered it.
	 * Negative offsets shift to the top.
	 */
	protected static final int TOUCH_SHIFT_Y = 0;

	/**
	 * <code>true</code> if touch events are processed.
	 */
	protected boolean touchEvent = false;

	/**
	 * Coordinates of current tooltip location (relative to <code>listener</code>
	 * widget. This allows to update the tooltip without user interaction. For
	 * example while hovering over an item that changes over time.
	 */
	private int x, y;

	/**
	 * Reference to style of tooltip. Most important to control positioning of
	 * tooltip.
	 */
	protected Style style;

	/**
	 * Reference to fullscreen change handler. When entering fullscreen the tooltip
	 * needs to be moved from the {@link RootPanel} to the fullscreen element and
	 * vice versa.
	 */
	HandlerRegistration fullscreenHandler;

	/**
	 * Lookup table of listener widgets sporting tooltips and their corresponding
	 * providers of the tooltip contents.
	 */
	protected HashMap<FocusPanel, Registration> participants;

	/**
	 * Reference to current provider of tooltips and <code>null</code> if no
	 * provider available.
	 */
	protected Registration current;

	/**
	 * Shared instance of tooltip.
	 */
	protected static Tooltip tooltip;

	/**
	 * The tooltip is added to the {@link RootPanel} of the GWT application and
	 * shared among all elements that sport tooltips. Always use this shared
	 * instance to implement tooltips.
	 * 
	 * @return shared instance of tooltip.
	 */
	public static Tooltip sharedTooltip() {
		if (tooltip == null) {
			tooltip = new Tooltip();
			RootPanel.get().add(tooltip);
			tooltip.style.setPosition(Position.FIXED);
			Window.addWindowScrollHandler(new ScrollHandler() {
				@Override
				public void onWindowScroll(ScrollEvent event) {
					tooltip.close();
				}
			});
			if (NativeJS.isFullscreenSupported())
				tooltip.fullscreenHandler = tooltip.addFullscreenChangeHandler(tooltip);
			tooltip.participants = new HashMap<FocusPanel, Registration>();
			tooltip.addTouchStartHandler(tooltip);
		}
		return tooltip;
	}

	/**
	 * Create new tooltip. Use shared instance to create tooltips,
	 * {@link #sharedTooltip()}.
	 */
	protected Tooltip() {
		setHTML("tooltip");
		setStyleName("gwt-Tooltip");
		style = getElement().getStyle();
		style.setDisplay(Display.BLOCK);
		style.setPosition(Position.FIXED);
		style.clearRight();
		style.clearBottom();
		style.clearWidth();
		close();
	}

	/**
	 * Register a new <code>listener</code> widget for tooltips and associate with
	 * the <code>provider</code> of the tooltip.
	 * 
	 * @param listener widget sporting tooltips
	 * @param provider provides tooltip
	 */
	public void add(FocusPanel listener, Provider provider) {
		participants.put(listener, new Registration(listener, provider));
	}

	/**
	 * Show tooltip. Set timer to show tooltip after delay {@link #delayShow}. If
	 * already visible the location is updated.
	 */
	public void show() {
		if (isVisible()) {
			// update location
			_show();
			return;
		}
		if (delayShow <= 0) {
			_show();
			return;
		}
		if (delayShowTimer.isRunning())
			return;
		delayShowTimer.schedule(delayShow);
	}

	/**
	 * Timer for handling the delay in showing tooltips.
	 */
	private Timer delayShowTimer = new Timer() {
		@Override
		public void run() {
			_show();
		}
	};

	/**
	 * Timer for handling the timeout after which the tooltip is closed.
	 */
	private Timer delayHideTimer = new Timer() {
		@Override
		public void run() {
			close();
		}
	};

	/**
	 * Helper method: show tooltip now and set timeout timer.
	 */
	private void _show() {
		if (!NativeJS.hasFocus()) {
			close();
			return;
		}
		style.clearRight();
		style.clearBottom();
		setPosition(x, y);
		setVisible(true);
		if (delayHide > 0)
			delayHideTimer.schedule(delayHide);
	}

	/**
	 * Close tooltip now.
	 */
	public void close() {
		delayShowTimer.cancel();
		delayHideTimer.cancel();
		setVisible(false);
		// prevents reappearance of tooltip after update.
		current = null;
	}

	/**
	 * Update tooltip if visible.
	 */
	public void update() {
		if (!isVisible())
			return;
		updateTooltip();
	}

	/**
	 * Helper method: remember current provider of tooltips for future use and
	 * update tooltip.
	 * 
	 * @param origin the FocusPanel with tooltips.
	 */
	private void _update(FocusPanel origin) {
		if (!NativeJS.hasFocus()) {
			close();
			return;
		}
		current = participants.get(origin);
		updateTooltip();
	}

	/**
	 * Helper method: update contents and visibility of tooltip.
	 * 
	 * @return <code>true</code> if tooltip is visible.
	 */
	private boolean updateTooltip() {
		if (current == null) {
			close();
			return false;
		}
		String tip = current.provider.getTooltipAt(x + Window.getScrollLeft() - current.listener.getAbsoluteLeft(),
				y + Window.getScrollTop() - current.listener.getAbsoluteTop());
		if (tip == null) {
			close();
			return false;
		}
		setHTML(tip);
		show();
		return true;
	}

	/**
	 * Set position <code>(x, y)</code> of tooltip relative to browser window.
	 * 
	 * @param x horizontal position of tooltip
	 * @param y vertical position of tooltip
	 */
	public void setPosition(int x, int y) {
		style.setPosition(Position.FIXED);
		if (touchEvent) {
			style.setLeft(x + TOUCH_SHIFT_X, Unit.PX);
			style.setTop(y + TOUCH_SHIFT_Y, Unit.PX);
		} else {
			style.setLeft(x + offsetX, Unit.PX);
			style.setTop(y + offsetY, Unit.PX);
		}
	}

	/**
	 * Sets the horizontal and vertical offset between the location where the
	 * tooltip was triggered and the position of the top left corner of the tooltip.
	 * Typically the offset should be non-negative in both directions to ensure that
	 * the tooltip does not obscure the location under the pointer.
	 * 
	 * @param ox horizontal offset
	 * @param oy vertical offset
	 * 
	 * @see #offsetX
	 * @see #offsetY
	 */
	public void setOffset(int ox, int oy) {
		offsetX = ox;
		offsetY = oy;
	}

	/**
	 * Set delay before showing the tooltip as well as the timeout after which the
	 * tooltip is hidden again (both in milliseconds). If <code>show==0</code>
	 * tooltips are shown immediately and if <code>hide==0</code> they never time
	 * out.
	 * 
	 * @param show the delay for showing the tooltip
	 * @param hide the timeout to hide tooltips again
	 * 
	 * @see #delayShow
	 * @see #delayHide
	 */
	public void setDelays(int show, int hide) {
		delayShow = show;
		delayHide = hide;
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * Pointer entered a listener widget. Process (potential) tooltip.
	 */
	@Override
	public void onMouseOver(MouseOverEvent event) {
		processMouseEvent(event);
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * Pointer exited listener widget. Close tooltip.
	 */
	@Override
	public void onMouseOut(MouseOutEvent event) {
		close();
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * Pointer moved on listener widget. Update tooltip.
	 */
	@Override
	public void onMouseMove(MouseMoveEvent event) {
		processMouseEvent(event);
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * Mouse wheel event on listener widget. Update tooltip.
	 */
	@Override
	public void onMouseWheel(MouseWheelEvent event) {
		processMouseEvent(event);
	}

	/**
	 * Update tooltip and save pointer locations.
	 * 
	 * @param event the mouse even that fired
	 */
	protected void processMouseEvent(MouseEvent<?> event) {
		Object src = event.getSource();
		assert src instanceof FocusPanel;
		x = event.getClientX();
		y = event.getClientY();
		touchEvent = false;
		_update((FocusPanel) src);
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * Show tooltip with single, one-finger on tap listener widget.
	 */
	@Override
	public void onTouchStart(TouchStartEvent event) {
		Object src = event.getSource();
		JsArray<Touch> touches = event.getTouches();
		if (src == tooltip || touches.length() > 1) {
			close();
			event.preventDefault();
			event.stopPropagation();
			return;
		}
		Touch touch = touches.get(0);
		x = touch.getClientX();
		y = touch.getClientY();
		touchEvent = true;
		assert src instanceof FocusPanel;
		_update((FocusPanel) src);
		// default passes the touch event to mouse handlers,
		// which is good, but also does selection and magnifying
		// stuff that gets confusing... does not seem to interfere
		// with context menu handlers.
		event.preventDefault();
		event.stopPropagation();
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * Hide tooltip when moving finger across listener widget.
	 */
	@Override
	public void onTouchMove(TouchMoveEvent event) {
		close();
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * Hide tooltip when one or more touches ended.
	 */
	@Override
	public void onTouchEnd(TouchEndEvent event) {
		// prevents reappearance of tooltip after update.
		current = null;
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
	 * Placement of shared tooltip element in the DOM needs to be adjusted when
	 * entering or exiting fullscreen mode.
	 */
	@Override
	public void onFullscreenChange(FullscreenChangeEvent event) {
		if (tooltip == null)
			return;
		// note: if we want to be smart, we can relocate the tooltip only if the
		// fullscreen element is among our listeners. worth it? see also ContexMenu.
		Element fs = NativeJS.getFullscreenElement();
		if (fs != null) {
			// entered fullscreen
			fs.appendChild(tooltip.getElement());
		} else {
			// exited fullscreen
			RootPanel.get().add(tooltip);
		}
		// position fixed can get lost when relocating element
		tooltip.style.setPosition(Position.FIXED);
	}
}
