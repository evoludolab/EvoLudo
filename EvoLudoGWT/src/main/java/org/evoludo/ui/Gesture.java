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

import com.google.gwt.core.client.Duration;
import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.Timer;

/**
 * The state of mouse gestures that move (e.g. shift, rotate or pan the view or
 * shift a node) or zoom the view (with the mouse wheel), including the cursors
 * that reflect the gesture. Moving and zooming may be combined freely, as for
 * touch gestures. However, some pointing devices (e.g. trackpads) emit spurious
 * wheel events when the mouse button is released. Hence, mouse wheel events are
 * ignored within {@link #WHEEL_GRACE_MSEC} after moving ended.
 * <p>
 * The cursor for zooming in or out is shown only for zooming with the mouse
 * wheel, as opposed to e.g. pinching or the context menu.
 * <p>
 * In addition, the position of the pointer (the mouse or a single touch) or
 * the center of a pinch (two touches) is tracked for mouse as well as touch
 * gestures. Handlers start tracking with {@link #start(int, int)} or
 * {@link #startPinch(int, int, int, int)}, report subsequent positions through
 * {@link #drag(int, int)} or {@link #pinch(int, int, int, int)}, and stop
 * tracking with {@link #lose()} if the pointer left the interactive area or
 * the touches ended. The displacement between subsequent positions is given by
 * {@link #getDX()} and {@link #getDY()}, the change of the distance between the
 * touches of a pinch by {@link #getScale()}.
 * <p>
 * Each interactive view keeps its own instance. Mouse event handlers
 * {@link #capture()} the mouse when the button is pressed, such that the
 * gesture continues even if the mouse leaves the view, report moving through
 * {@link #move(Move)} and the end of the gesture through {@link #release()}. Mouse wheel events are checked by {@link #wheel()} and
 * views report the direction of zooming through {@link #zooming(boolean)}.
 *
 * <h2>CSS Style Rules</h2>
 * <dl>
 * <dt>.evoludo-cursorZoomIn, .evoludo-cursorZoomOut</dt>
 * <dd>added to the element of the view while zooming in or out, respectively,
 * and removed {@link #ZOOM_INERTIA_MSEC} after zooming stopped.</dd>
 * <dt>.evoludo-cursorMoveView, .evoludo-cursorRotate,
 * .evoludo-cursorMoveNode</dt>
 * <dd>added to the element of the view while moving, see {@link Move}.</dd>
 * </dl>
 */
public class Gesture {

	/**
	 * The time (in milliseconds) after moving the view with the mouse ended during
	 * which mouse wheel events are ignored.
	 */
	public static final double WHEEL_GRACE_MSEC = 300.0;

	/**
	 * The time (in milliseconds) after zooming in or out stopped until the cursor
	 * for zooming in or out is removed.
	 */
	public static final int ZOOM_INERTIA_MSEC = 150;

	/**
	 * The CSS class name for the cursor while zooming in.
	 */
	private static final String CSS_CURSOR_ZOOM_IN = "evoludo-cursorZoomIn";

	/**
	 * The CSS class name for the cursor while zooming out.
	 */
	private static final String CSS_CURSOR_ZOOM_OUT = "evoludo-cursorZoomOut";

	/**
	 * The kinds of moving gestures, each with its cursor.
	 */
	public enum Move {
		/**
		 * Shift (or pan) the view.
		 */
		VIEW("evoludo-cursorMoveView"),

		/**
		 * Rotate the view.
		 */
		ROTATE("evoludo-cursorRotate"),

		/**
		 * Shift a node.
		 */
		NODE("evoludo-cursorMoveNode");

		/**
		 * The CSS class name for the cursor of this kind of moving.
		 */
		private final String cursor;

		/**
		 * Create a kind of moving with the cursor given by the CSS class name
		 * {@code cursor}.
		 *
		 * @param cursor the CSS class name of the cursor
		 */
		Move(String cursor) {
			this.cursor = cursor;
		}
	}

	/**
	 * The element of the view that shows the cursor.
	 */
	private final Element element;

	/**
	 * The kind of moving whose cursor is shown, or {@code null} if none.
	 */
	private Move moving;

	/**
	 * The flag to indicate whether the ongoing gesture moved the view (or a node).
	 */
	private boolean moved = false;

	/**
	 * The value of {@link #pointerId} if the mouse pointer is unknown.
	 */
	private static final int NO_POINTER = -1;

	/**
	 * The id of the mouse pointer that was last pressed on the element of the
	 * view, or {@link #NO_POINTER}.
	 */
	private int pointerId = NO_POINTER;

	/**
	 * The id of the mouse pointer captured by the element of the view, or
	 * {@link #NO_POINTER}.
	 */
	private int captured = NO_POINTER;

	/**
	 * The time when moving with the mouse ended.
	 */
	private double moveEndTime = -Double.MAX_VALUE;

	/**
	 * The time of the last mouse wheel event that zoomed the view.
	 */
	private double wheelTime = -Double.MAX_VALUE;

	/**
	 * The value of the coordinates of a position that is not tracked.
	 */
	private static final int UNTRACKED = Integer.MIN_VALUE;

	/**
	 * The {@code x}-coordinate of the tracked position of the pointer (or the
	 * center of a pinch), or {@link #UNTRACKED}.
	 */
	private int x = UNTRACKED;

	/**
	 * The {@code y}-coordinate of the tracked position of the pointer (or the
	 * center of a pinch), or {@link #UNTRACKED}.
	 */
	private int y = UNTRACKED;

	/**
	 * The distance between the touches of the tracked pinch, or {@code 0} if no
	 * pinch is tracked.
	 */
	private double dist = 0.0;

	/**
	 * The horizontal displacement from the current to the previous position.
	 */
	private int dx;

	/**
	 * The vertical displacement from the current to the previous position.
	 */
	private int dy;

	/**
	 * The ratio of the current and previous distance between the touches of a
	 * pinch.
	 */
	private double scale = 1.0;

	/**
	 * The timer to remove the cursor for zooming in or out
	 * {@link #ZOOM_INERTIA_MSEC} after zooming stopped.
	 */
	private final Timer zoomInertiaTimer = new Timer() {
		@Override
		public void run() {
			stopZooming();
		}
	};

	/**
	 * Create a new gesture state, without an ongoing gesture, for the view with
	 * the element {@code element}.
	 *
	 * @param element the element of the view that shows the cursor
	 */
	public Gesture(Element element) {
		this.element = element;
		listenPointerDown(element);
	}

	/**
	 * Record the id of the mouse pointer whenever it is pressed on
	 * {@code element}. Pointer events precede the corresponding mouse events,
	 * such that the id is known when handling {@code mousedown}.
	 *
	 * @param element the element of the view
	 *
	 * @see #capture()
	 */
	private native void listenPointerDown(Element element) /*-{
		var self = this;
		element.addEventListener("pointerdown", function(event) {
			if (event.pointerType === "mouse")
				self.@org.evoludo.ui.Gesture::pointerId = event.pointerId;
		});
	}-*/;

	/**
	 * The mouse button was pressed. The element of the view captures the mouse
	 * pointer until the gesture is released, such that the gesture continues even
	 * if the mouse leaves the view. In contrast to {@code DOM.setCapture(Element)},
	 * pointer capture by the browser leaves mouse wheel events untouched (zooming
	 * still works while moving), lets events bubble as usual and is released by the
	 * browser when the mouse button is released.
	 *
	 * @see #release()
	 */
	public void capture() {
		if (pointerId != NO_POINTER && setPointerCapture(element, pointerId))
			captured = pointerId;
	}

	/**
	 * Capture the pointer with id {@code id} by {@code element}.
	 *
	 * @param element the element of the view
	 * @param id      the id of the pointer
	 * @return {@code true} if the pointer was captured
	 */
	private static native boolean setPointerCapture(Element element, int id) /*-{
		try {
			element.setPointerCapture(id);
			return true;
		} catch (e) {
			// pointer no longer active or pointer capture not supported
			return false;
		}
	}-*/;

	/**
	 * Release the pointer with id {@code id} if captured by {@code element}.
	 *
	 * @param element the element of the view
	 * @param id      the id of the pointer
	 */
	private static native void releasePointerCapture(Element element, int id) /*-{
		try {
			if (element.hasPointerCapture(id))
				element.releasePointerCapture(id);
		} catch (e) {
			// pointer no longer active
		}
	}-*/;

	/**
	 * The mouse button was released (or the view deactivated), which ends the
	 * gesture, releases the mouse pointer, stops tracking and removes its cursors.
	 *
	 * @see #capture()
	 */
	public void release() {
		if (captured != NO_POINTER) {
			releasePointerCapture(element, captured);
			captured = NO_POINTER;
		}
		if (moved)
			moveEndTime = Duration.currentTimeMillis();
		moved = false;
		lose();
		stopZooming();
	}

	/**
	 * Start tracking the pointer at {@code (x, y)}, e.g. when the mouse button is
	 * pressed, a single touch started or one of two touches ended.
	 *
	 * @param x the {@code x}-coordinate of the pointer
	 * @param y the {@code y}-coordinate of the pointer
	 */
	public void start(int x, int y) {
		this.x = x;
		this.y = y;
		dist = 0.0;
	}

	/**
	 * Start tracking the pinch with touches at {@code (x0, y0)} and
	 * {@code (x1, y1)}.
	 *
	 * @param x0 the {@code x}-coordinate of the first touch
	 * @param y0 the {@code y}-coordinate of the first touch
	 * @param x1 the {@code x}-coordinate of the second touch
	 * @param y1 the {@code y}-coordinate of the second touch
	 */
	public void startPinch(int x0, int y0, int x1, int y1) {
		x = (x0 + x1) / 2;
		y = (y0 + y1) / 2;
		dist = Math.hypot((double) x0 - x1, (double) y0 - y1);
	}

	/**
	 * Stop tracking, e.g. because the pointer left the interactive area or the
	 * touches ended. This also removes the cursor for moving until moving resumes.
	 *
	 * @see #move(Move)
	 */
	public void lose() {
		x = UNTRACKED;
		y = UNTRACKED;
		dist = 0.0;
		pause();
	}

	/**
	 * The pointer moved to {@code (x, y)}. If the pointer was tracked, the
	 * displacement is available through {@link #getDX()} and {@link #getDY()}.
	 * Otherwise, start tracking at {@code (x, y)}.
	 *
	 * @param x the {@code x}-coordinate of the pointer
	 * @param y the {@code y}-coordinate of the pointer
	 * @return {@code true} if the pointer was tracked and moved
	 */
	public boolean drag(int x, int y) {
		if (this.x == UNTRACKED || this.y == UNTRACKED) {
			start(x, y);
			return false;
		}
		dx = this.x - x;
		dy = this.y - y;
		this.x = x;
		this.y = y;
		return true;
	}

	/**
	 * The touches of a pinch moved to {@code (x0, y0)} and {@code (x1, y1)}. If
	 * the pinch was tracked, the displacement of its center is available through
	 * {@link #getDX()} and {@link #getDY()} and the change of the distance between
	 * the touches through {@link #getScale()}. Otherwise, start tracking the
	 * pinch.
	 *
	 * @param x0 the {@code x}-coordinate of the first touch
	 * @param y0 the {@code y}-coordinate of the first touch
	 * @param x1 the {@code x}-coordinate of the second touch
	 * @param y1 the {@code y}-coordinate of the second touch
	 * @return {@code true} if the pinch was tracked and moved
	 */
	public boolean pinch(int x0, int y0, int x1, int y1) {
		double prevDist = dist;
		int prevX = x;
		int prevY = y;
		startPinch(x0, y0, x1, y1);
		if (prevDist <= 0.0 || dist <= 0.0 || prevX == UNTRACKED || prevY == UNTRACKED)
			return false;
		dx = prevX - x;
		dy = prevY - y;
		scale = dist / prevDist;
		return true;
	}

	/**
	 * Get the {@code x}-coordinate of the tracked pointer (or center of the
	 * pinch).
	 *
	 * @return the {@code x}-coordinate
	 */
	public int getX() {
		return x;
	}

	/**
	 * Get the {@code y}-coordinate of the tracked pointer (or center of the
	 * pinch).
	 *
	 * @return the {@code y}-coordinate
	 */
	public int getY() {
		return y;
	}

	/**
	 * Get the horizontal displacement from the current to the previous position
	 * of the pointer (or center of the pinch), i.e. the shift of the view that
	 * follows the pointer.
	 *
	 * @return the horizontal displacement
	 *
	 * @see #drag(int, int)
	 * @see #pinch(int, int, int, int)
	 */
	public int getDX() {
		return dx;
	}

	/**
	 * Get the vertical displacement from the current to the previous position of
	 * the pointer (or center of the pinch), i.e. the shift of the view that
	 * follows the pointer.
	 *
	 * @return the vertical displacement
	 *
	 * @see #drag(int, int)
	 * @see #pinch(int, int, int, int)
	 */
	public int getDY() {
		return dy;
	}

	/**
	 * Get the ratio of the current and previous distance between the touches of
	 * the pinch, i.e. the change of the zoom level.
	 *
	 * @return the change of the zoom level
	 *
	 * @see #pinch(int, int, int, int)
	 */
	public double getScale() {
		return scale;
	}

	/**
	 * Remove the cursor for zooming in or out.
	 */
	private void stopZooming() {
		zoomInertiaTimer.cancel();
		element.removeClassName(CSS_CURSOR_ZOOM_IN);
		element.removeClassName(CSS_CURSOR_ZOOM_OUT);
	}

	/**
	 * The ongoing gesture moves the view (or a node) by moving of kind
	 * {@code kind}. Show the cursor of {@code kind}.
	 *
	 * @param kind the kind of moving
	 */
	public void move(Move kind) {
		moved = true;
		if (moving == kind)
			return;
		pause();
		moving = kind;
		element.addClassName(kind.cursor);
	}

	/**
	 * Remove the cursor for moving until moving resumes.
	 *
	 * @see #move(Move)
	 */
	private void pause() {
		if (moving == null)
			return;
		element.removeClassName(moving.cursor);
		moving = null;
	}

	/**
	 * The view zoomed in (or out). Show the cursor for zooming in (or out) until
	 * {@link #ZOOM_INERTIA_MSEC} after zooming stopped. Ignored unless zooming
	 * was triggered by the mouse wheel, i.e. within {@link #ZOOM_INERTIA_MSEC}
	 * after the last wheel event, e.g. for zooming by pinching or through the
	 * context menu.
	 *
	 * @param in {@code true} if the view zoomed in
	 */
	public void zooming(boolean in) {
		if (Duration.currentTimeMillis() - wheelTime > ZOOM_INERTIA_MSEC)
			return;
		element.removeClassName(in ? CSS_CURSOR_ZOOM_OUT : CSS_CURSOR_ZOOM_IN);
		element.addClassName(in ? CSS_CURSOR_ZOOM_IN : CSS_CURSOR_ZOOM_OUT);
		zoomInertiaTimer.schedule(ZOOM_INERTIA_MSEC);
	}

	/**
	 * Check whether a mouse wheel event may zoom the view and, if so, record its
	 * time. Wheel events are ignored within {@link #WHEEL_GRACE_MSEC} after moving
	 * ended, because some pointing devices emit spurious wheel events when the
	 * button is released.
	 *
	 * @return {@code true} if the view may be zoomed
	 */
	public boolean wheel() {
		double now = Duration.currentTimeMillis();
		if (now - moveEndTime < WHEEL_GRACE_MSEC)
			return false;
		wheelTime = now;
		return true;
	}
}
