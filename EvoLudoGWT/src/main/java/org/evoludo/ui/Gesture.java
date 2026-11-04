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
 * Each interactive view keeps its own instance. Mouse event handlers report
 * moving through {@link #move(Move)}, the mouse leaving the interactive area
 * while moving through {@link #pause()} and the end of the gesture through
 * {@link #release()}. Mouse wheel events are checked by {@link #wheel()} and
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
	 * The time when moving with the mouse ended.
	 */
	private double moveEndTime = -Double.MAX_VALUE;

	/**
	 * The time of the last mouse wheel event that zoomed the view.
	 */
	private double wheelTime = -Double.MAX_VALUE;

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
	}

	/**
	 * The mouse button was released (or the mouse left the view), which ends the
	 * gesture and removes its cursors.
	 */
	public void release() {
		if (moved)
			moveEndTime = Duration.currentTimeMillis();
		moved = false;
		pause();
		stopZooming();
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
	 * The pointer left the interactive area while moving. Remove the cursor for
	 * moving until moving resumes.
	 *
	 * @see #move(Move)
	 */
	public void pause() {
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
