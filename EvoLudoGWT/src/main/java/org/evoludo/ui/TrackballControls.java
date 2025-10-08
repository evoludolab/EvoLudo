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

package org.evoludo.ui;

import com.google.gwt.core.client.JsArray;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.dom.client.Touch;
import com.google.gwt.event.dom.client.MouseDownEvent;
import com.google.gwt.event.dom.client.MouseDownHandler;
import com.google.gwt.event.dom.client.MouseMoveEvent;
import com.google.gwt.event.dom.client.MouseMoveHandler;
import com.google.gwt.event.dom.client.MouseUpEvent;
import com.google.gwt.event.dom.client.MouseUpHandler;
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
import com.google.gwt.user.client.ui.RequiresResize;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.Widget;

import thothbot.parallax.core.client.AnimatedScene;
import thothbot.parallax.core.client.controls.Controls;
import thothbot.parallax.core.shared.cameras.OrthographicCamera;
import thothbot.parallax.core.shared.cameras.PerspectiveCamera;
import thothbot.parallax.core.shared.core.Object3D;
import thothbot.parallax.core.shared.math.Quaternion;
import thothbot.parallax.core.shared.math.Vector2;
import thothbot.parallax.core.shared.math.Vector3;

/**
 * Trackball controls for 3D views adapted from Parallax and threejs
 * 
 * <h2>CSS Style Rules</h2> The different trackball actions of zooming, panning
 * and rotating change the style classes. As an example, this allow to change
 * the pointer style accordingly.
 * <dl>
 * <dt>.evoludo-cursorZoomIn</dt>
 * <dd>the 3D view is zooming in.</dd>
 * <dt>.evoludo-cursorZoomOut</dt>
 * <dd>the 3D view is zooming out.</dd>
 * <dt>.evoludo-cursorRotate</dt>
 * <dd>the 3D view is rotating</dd>
 * <dt>.evoludo-cursorMoveView</dt>
 * <dd>the 3D view is panning</dd>
 * </dl>
 * 
 * @author Christoph Hauert
 * 
 * @see <a href="http://thothbot.github.io/">http://thothbot.github.io/</a>
 * @see <a href="https://threejs.org//">https://threejs.org//</a>
 */
public class TrackballControls extends Controls implements MouseWheelHandler, MouseDownHandler, MouseUpHandler,
		MouseMoveHandler, TouchStartHandler, TouchMoveHandler, TouchEndHandler, RequiresResize {

	// XXX should probably use currently unused max/minDistance instead...
	// /**
	// * The minimum distance to the object. Determines how close you can zoom in.
	// */
	// private double minDistance = 0.0;
	//
	// /**
	// * The maximum distance to the object. Determines how far you can zoom out.
	// */
	// private double maxDistance = Double.MAX_VALUE;

	/**
	 * Maximum eye distance. How far you can zoom out.
	 */
	private static final double MAX_EYE_DIST = 5000.0;

	/**
	 * Square of maximum eye distance.
	 */
	private static final double MAX_EYE_DIST_SQ = MAX_EYE_DIST * MAX_EYE_DIST;

	/**
	 * Minimum eye distance. How far you can zoom in.
	 */
	private static final double MIN_EYE_DIST = 1e-4;

	/**
	 * Initial eye distance.
	 */
	double initialDistance;

	/**
	 * Square of minimum eye distance.
	 */
	private static final double MIN_EYE_DIST_SQ = MIN_EYE_DIST * MIN_EYE_DIST;

	/**
	 * Default increments of zoom factor (multiplicative).
	 */
	protected static final double ZOOM_INCR = 1.02;

	/**
	 * <code>true</code> if trackball controls enabled.
	 */
	private boolean isEnabled = true;

	/**
	 * Speed of rotation;
	 */
	private double rotateSpeed = 1.0;

	/**
	 * Speed for zoom.
	 */
	private double zoomSpeed = ZOOM_INCR;

	/**
	 * Speed for panning (shifting).
	 */
	private double panSpeed = 0.3;

	/**
	 * Utility variable to keep track of changes of zoom factor while zooming.
	 */
	private double zoomChange = 1.0;

	/**
	 * Flag indicating whether rotation is enabled.
	 */
	private boolean hasRotate = true;

	/**
	 * Flag indicating whether zooming is enabled.
	 */
	private boolean hasZoom = true;

	/**
	 * Flag indicating whether panning is enabled.
	 */
	private boolean hasPan = true;

	/**
	 * <code>true</code> if rotation is pending.
	 */
	private boolean doRotate = false;

	/**
	 * <code>true</code> if zooming is pending.
	 */
	private boolean doZoom = false;

	/**
	 * <code>true</code> if panning is pending.
	 */
	private boolean doPan = false;

	/**
	 * <code>true</code> while the pointer is dragging.
	 */
	private boolean isDragging = false;

	/**
	 * <code>true</code> to disable dynamic damping of changes to the view..
	 */
	private boolean isDynamicUpdate = true;

	/**
	 * Factor for dynamic damping of changes to the view.
	 */
	private double dynamicDampingFactor = 0.2;

	/**
	 * Radius of trackball. Depends on size of associated widget that displays the
	 * 3D scene.
	 */
	private double radius;

	/**
	 * Target point eye is looking at.
	 */
	private Vector3 target;

	// private Vector3 lastPosition;

	/**
	 * Location of eye.
	 */
	private Vector3 eye;

	/**
	 * Starting coordinates on trackball for rotations.
	 */
	private Vector3 rotateStart;

	/**
	 * End coordinates on trackball for rotations.
	 */
	private Vector3 rotateEnd;

	/**
	 * Starting coordinates on trackball for panning.
	 */
	private Vector2 panStart;

	/**
	 * End coordinates on trackball for panning.
	 */
	private Vector2 panEnd;

	/**
	 * Mouse and touch handlers to control the view of the 3D scene.
	 */
	private HandlerRegistration mouseWheelHandler;
	private HandlerRegistration mouseDownHandler;
	private HandlerRegistration mouseMoveHandler;
	private HandlerRegistration mouseUpHandler;
	private HandlerRegistration touchStartHandler;
	private HandlerRegistration touchMoveHandler;
	private HandlerRegistration touchEndHandler;

	protected static final String CURSOR_MOVE_VIEW = "evoludo-cursorMoveView";
	protected static final String CURSOR_ROTATE_VIEW = "evoludo-cursorRotate";

	/**
	 * Creates a new instance of TrackballControls for the 3D scene displayed in
	 * <code>widget</code> as seen from the <code>camera</code>.
	 * 
	 * @param camera the camera of the rendered 3D scene
	 * @param widget the view that displays the 3D scene and listens for pointer and
	 *               touch events
	 */
	public TrackballControls(Object3D camera, Widget widget) {
		super(camera, widget);

		if (widget.getClass() != RootPanel.class)
			widget.getElement().setAttribute("tabindex", "-1");

		target = new Vector3();
		// lastPosition = new Vector3();
		eye = new Vector3();
		rotateStart = new Vector3();
		rotateEnd = new Vector3();
		panStart = new Vector2();
		panEnd = new Vector2();
	}

	/**
	 * Initialize and register mouse and touch handlers.
	 */
	public void load() {
		Widget widget = getWidget();
		// register mouse handlers
		mouseWheelHandler = widget.addDomHandler(this, MouseWheelEvent.getType());
		mouseDownHandler = widget.addDomHandler(this, MouseDownEvent.getType());
		mouseMoveHandler = widget.addDomHandler(this, MouseMoveEvent.getType());
		mouseUpHandler = widget.addDomHandler(this, MouseUpEvent.getType());
		// register touch handlers
		touchStartHandler = widget.addDomHandler(this, TouchStartEvent.getType());
		touchMoveHandler = widget.addDomHandler(this, TouchMoveEvent.getType());
		touchEndHandler = widget.addDomHandler(this, TouchEndEvent.getType());
		// set initial eye distance
		initialDistance = getObject().getPosition().length();
		// set radius
		onResize();
	}

	/**
	 * Remove mouse and touch handlers.
	 */
	public void unload() {
		if (mouseWheelHandler != null)
			mouseWheelHandler.removeHandler();
		if (mouseDownHandler != null)
			mouseDownHandler.removeHandler();
		if (mouseMoveHandler != null)
			mouseMoveHandler.removeHandler();
		if (mouseUpHandler != null)
			mouseUpHandler.removeHandler();
		if (touchStartHandler != null)
			touchStartHandler.removeHandler();
		if (touchMoveHandler != null)
			touchMoveHandler.removeHandler();
		if (touchEndHandler != null)
			touchEndHandler.removeHandler();
	}

	/**
	 * Check if trackball controls are enabled.
	 * 
	 * @return <code>true</code> if enabled
	 */
	public boolean isEnabled() {
		return isEnabled;
	}

	/**
	 * Enable/disable trackball controls (default: enabled).
	 * 
	 * @param isEnabled enable trackball controls if <code>true</code>
	 */
	public void setEnabled(boolean isEnabled) {
		this.isEnabled = isEnabled;
	}

	/**
	 * Set rotation speed (default: <code>1.0</code>).
	 * 
	 * @param rotateSpeed the speed to rotate 3D view
	 */
	public void setRotateSpeed(double rotateSpeed) {
		this.rotateSpeed = rotateSpeed;
	}

	/**
	 * Set zoom speed (default: <code>1.2</code>).
	 * 
	 * @param zoomSpeed the speed to zoom 3D view
	 */
	public void setZoomSpeed(double zoomSpeed) {
		this.zoomSpeed = zoomSpeed;
	}

	/**
	 * Set pan speed (default: <code>0.3</code>).
	 * 
	 * @param panSpeed the speed to pan 3D view
	 */
	public void setPanSpeed(double panSpeed) {
		this.panSpeed = panSpeed;
	}

	/**
	 * Enable/disable rotation (default: enabled).
	 * 
	 * @param rotate enables rotation if <code>true</code>
	 */
	public void setRotate(boolean rotate) {
		this.hasRotate = rotate;
	}

	/**
	 * Enable/disable zoom (default: enabled).
	 * 
	 * @param zoom enables zoom if <code>true</code>
	 */
	public void setZoom(boolean zoom) {
		this.hasZoom = zoom;
	}

	/**
	 * Enable/disable pan (default: enabled).
	 * 
	 * @param pan enables panning if <code>true</code>
	 */
	public void setPan(boolean pan) {
		this.hasPan = pan;
	}

	/**
	 * Enable/disable dynamic moving of 3D view (default: enabled).
	 * 
	 * @param dynamic enables dynamic updates of 3D view if <code>true</code>
	 */
	public void setDynamicUpdate(boolean dynamic) {
		this.isDynamicUpdate = dynamic;
	}

	/**
	 * Set dynamic damping factor (default: <code>0.2</code>).
	 * 
	 * @param dynamicDampingFactor the factor for dynamic damping of
	 */
	public void setDynamicDampingFactor(double dynamicDampingFactor) {
		this.dynamicDampingFactor = dynamicDampingFactor;
	}

	// /**
	// * Sets minimum distance to the object. Default 0
	// *
	// * @param minDistance
	// */
	// public void setMinDistance(double minDistance) {
	// this.minDistance = minDistance;
	// }

	// /**
	// * Sets maximum distance to the object. Default Infinity.
	// *
	// * @param maxDistance
	// */
	// public void setMaxDistance(double maxDistance) {
	// this.maxDistance = maxDistance;
	// }

	/**
	 * Get the target point where the eye is looking.
	 * 
	 * @return the target point
	 */
	public Vector3 getTarget() {
		return target;
	}

	/**
	 * Set the target point where the eye is looking.
	 * 
	 * @param target the new target for the eye
	 */
	public void setTarget(Vector3 target) {
		this.target = target;
	}

	/**
	 * Update the view of the 3D scene. Perform rotations, zooming and panning as
	 * requested.
	 * <p>
	 * <strong>Note:</strong> This method must be called in the
	 * {@link AnimatedScene#onUpdate} method.
	 */
	public void update() {
		eye.copy(getObject().getPosition()).sub(target);

		if (doRotate) {
			rotateCamera();
			doRotate = false;
		}
		if (doZoom) {
			zoomCamera();
			doZoom = false;
		}
		if (doPan) {
			panCamera();
			doPan = false;
		}

		getObject().getPosition().add(target, eye);
		// checkDistances();
		getObject().lookAt(target);

		// if( lastPosition.distanceTo(getObject().getPosition())>0 )
		// lastPosition.copy(getObject().getPosition());
	}

	/**
	 * Reset zoom.
	 */
	public void zoom() {
		Object3D camera = getObject();
		if (camera instanceof PerspectiveCamera) {
			double eyeDistance = eye.copy(getObject().getPosition()).sub(target).length();
			zoom((initialDistance / eyeDistance));
		}
		if (camera instanceof OrthographicCamera) {
			camera.getScale().set(1.0, 1.0, 1.0);
			camera.updateMatrix();
		}
	}

	/**
	 * Set zoom factor.
	 * 
	 * @param zoom the new zoom factor
	 */
	public void zoom(double zoom) {
		zoomChange = zoom;
		doZoom = true;
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * Adjusts the radius of the (virtual) trackball.
	 */
	@Override
	public void onResize() {
		int width = getWidget().getOffsetWidth();
		int height = getWidget().getOffsetHeight();
		// widget not yet ready - leave radius unchanged (onResize is called when view
		// gets activated (why?) but widget returns zero dimensions)
		if (width == 0 && height == 0)
			return;
		radius = Math.sqrt(width * width + height * height);
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * Zooms the 3D view.
	 */
	@Override
	public void onMouseWheel(MouseWheelEvent event) {
		if (!hasZoom)
			return;
		event.preventDefault();
		double dy = event.getNativeDeltaY();
		if (dy == 0.0)
			return;
		zoomChange = Math.pow(zoomSpeed, -dy);
		doZoom = true;
		getWidget().addStyleName(dy < 0 ? "evoludo-cursorZoomIn" : "evoludo-cursorZoomOut");
		if (!t.isRunning())
			t.schedule(200);
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * Marks the potential start of updates to the view of the 3D scene.
	 */
	@Override
	public void onMouseDown(MouseDownEvent event) {
		if (!hasRotate && !hasPan)
			return;
		isDragging = event.getNativeButton() == NativeEvent.BUTTON_LEFT;
		if (!isDragging)
			return;

		if (hasRotate)
			rotateStart = getMouseProjectionOnBall(event.getX(), event.getY());
		if (hasPan)
			panStart = getMouseOnScreen(event.getX(), event.getY());
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * If the left mouse button was released this marks the end updates to the view
	 * of the 3D scene.
	 */
	@Override
	public void onMouseUp(MouseUpEvent event) {
		if (event.getNativeButton() != NativeEvent.BUTTON_LEFT)
			return;
		isDragging = false;
		getWidget().removeStyleName(CURSOR_ROTATE_VIEW);
		getWidget().removeStyleName(CURSOR_MOVE_VIEW);
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * If the left mouse button is down, this dragging rotates or pans (if Shift-key
	 * is pressed) the view of the 3D scene.
	 */
	@Override
	public void onMouseMove(MouseMoveEvent event) {
		if (!isDragging)
			return;

		boolean isShift = event.isShiftKeyDown();
		if (isShift) {
			if (!hasPan)
				return;
			panEnd = getMouseOnScreen(event.getX(), event.getY());
			doPan = true;
			getWidget().addStyleName(CURSOR_MOVE_VIEW);
		} else {
			if (!hasRotate)
				return;
			rotateEnd = getMouseProjectionOnBall(event.getX(), event.getY());
			doRotate = true;
			getWidget().addStyleName(CURSOR_ROTATE_VIEW);
		}
	}

	/**
	 * Timer for reseting the CSS styling after zooming.
	 */
	private Timer t = new Timer() {
		@Override
		public void run() {
			getWidget().removeStyleName("evoludo-cursorZoomIn");
			getWidget().removeStyleName("evoludo-cursorZoomOut");
		}
	};

	/**
	 * Helper variable to keep track of the distance of two touches between
	 * subsequent touch events.
	 */
	private double pinchZoom;

	/**
	 * {@inheritDoc}
	 * <p>
	 * Marks the potential start of updates to the view of the 3D scene.
	 */
	@Override
	public void onTouchStart(TouchStartEvent event) {
		JsArray<Touch> touches = event.getTouches();
		switch (touches.length()) {
			case 1:
				if (!hasRotate)
					return;
				Touch touch = touches.get(0);
				Element ref = getWidget().getElement();
				int x = touch.getRelativeX(ref);
				int y = touch.getRelativeY(ref);
				rotateStart = getMouseProjectionOnBall(x, y);
				event.preventDefault();
				break;

			case 2:
				if (!hasZoom)
					return;
				// start of pinching
				Touch touch0 = touches.get(0);
				ref = getWidget().getElement();
				int x0 = touch0.getRelativeX(ref);
				int y0 = touch0.getRelativeY(ref);
				Touch touch1 = touches.get(1);
				int x1 = touch1.getRelativeX(ref);
				int y1 = touch1.getRelativeY(ref);
				double dX = x0 - x1;
				double dY = y0 - y1;
				pinchZoom = Math.sqrt(dX * dX + dY * dY);
				// don't signal zooming just yet
				doZoom = false;
				event.preventDefault();
				break;

			default:
				// more than two touch points
		}
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * For one-finger touch rotate the view and for two-finger touch zoom the view
	 * of the 3D scene. Note panning for touch events is not (yet?) implemented.
	 */
	@Override
	public void onTouchMove(TouchMoveEvent event) {
		JsArray<Touch> touches = event.getTouches();
		switch (touches.length()) {
			case 1:
				if (!hasRotate)
					return;
				Touch touch = touches.get(0);
				Element ref = getWidget().getElement();
				int x = touch.getRelativeX(ref);
				int y = touch.getRelativeY(ref);
				rotateEnd = getMouseProjectionOnBall(x, y);
				doRotate = true;
				event.preventDefault();
				break;

			case 2:
				if (!hasZoom)
					return;
				// process pinch zoom
				Touch touch0 = touches.get(0);
				int x0 = touch0.getClientX();
				int y0 = touch0.getClientY();
				Touch touch1 = touches.get(1);
				int x1 = touch1.getClientX();
				int y1 = touch1.getClientY();
				double dX = x0 - x1;
				double dY = y0 - y1;
				double zoom = Math.sqrt(dX * dX + dY * dY);
				zoomChange = pinchZoom / zoom;
				// add some amplification
				if (zoomChange > 1.0)
					zoomChange *= 1.05;
				else
					zoomChange *= 0.952;
				pinchZoom = zoom;
				doZoom = true;
				if (!t.isRunning())
					t.schedule(300);
				event.preventDefault();
				break;

			default:
				// more than two touches
		}
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * At least one touch ended. Marks the end of zooming but if one touch remains
	 * get ready to rotate the 3D scene.
	 */
	@Override
	public void onTouchEnd(TouchEndEvent event) {
		if (!hasRotate)
			return;
		JsArray<Touch> touches = event.getTouches();
		if (touches.length() == 1) {
			// went from two touches to one touch - record position to prevent jumps
			Touch touch = touches.get(0);
			Element ref = getWidget().getElement();
			int x = touch.getRelativeX(ref);
			int y = touch.getRelativeY(ref);
			rotateStart = getMouseProjectionOnBall(x, y);
		}
	}

	/**
	 * Re-scale pointer coordinates to <code>[0, 1]<sup>2</sup></code>. This is used
	 * for panning.
	 * 
	 * @param clientX the horizontal pointer coordinate (relative to widget)
	 * @param clientY the vertical pointer coordinate (relative to widget)
	 * @return scaled pointer coordinates
	 */
	private Vector2 getMouseOnScreen(int clientX, int clientY) {
		return new Vector2(clientX / radius * 0.5, clientY / radius * 0.5);
	}

	/**
	 * The mouse coordinates on the virtual trackball.
	 */
	private Vector3 mouseOnBall = new Vector3();
	// private Vector3 projection = new Vector3();
	// private Vector3 projSide = new Vector3();

	/**
	 * Convert pointer coordinates to coordinates on (virtual) trackball (unit
	 * sphere). This is used for rotation.
	 * 
	 * @param clientX the horizontal pointer coordinate (relative to widget)
	 * @param clientY the vertical pointer coordinate (relative to widget)
	 * @return coordinates of pointer on trackball
	 */
	private Vector3 getMouseProjectionOnBall(int clientX, int clientY) {
		mouseOnBall.set((clientX - getWidget().getOffsetWidth() * 0.5) / radius,
				(getWidget().getOffsetHeight() * 0.5 - clientY) / radius, 0.0);

		double length = mouseOnBall.length();
		if (length > 1.0)
			mouseOnBall.normalize();
		else
			mouseOnBall.setZ(Math.sqrt(1.0 - length * length));

		eye.copy(getObject().getPosition()).sub(target);

		Vector3 projection = getObject().getUp().clone().setLength(mouseOnBall.getY());
		projection.add(getObject().getUp().clone().cross(eye).setLength(mouseOnBall.getX()));
		projection.add(eye.setLength(mouseOnBall.getZ()));

		// attempts to avoid the clone-ing fail for unknown reasons...
		// projection.copy(getObject().getUp());
		// projSide.copy(projection).cross(eye).setLength(mouseOnBall.getX());
		// projection.setLength(mouseOnBall.getY());
		// projection.add(projSide);
		// projection.add(eye.setLength(mouseOnBall.getZ()));

		// projection.copy(getObject().getUp()).setLength(mouseOnBall.getY());
		// projection.add(projSide.copy(getObject().getUp()).cross(eye).setLength(mouseOnBall.getX()));
		// projection.add(eye.setLength(mouseOnBall.getZ()));

		return projection;
	}

	// private void checkDistances() {
	// if( isZoom || isPan ) {
	// if( getObject().getPosition().lengthSq()>maxDistance*maxDistance )
	// getObject().getPosition().setLength(maxDistance);
	//
	// if( eye.lengthSq()<minDistance*minDistance )
	// getObject().getPosition().add(target, eye.setLength(minDistance));
	// }
	// }

	/**
	 * Update eye and target for (dynamical) panning.
	 */
	private void panCamera() {
		Vector2 mouseChange = panEnd.clone().sub(panStart);

		if (mouseChange.lengthSq() > 0.0) {
			mouseChange.multiply(eye.length() * panSpeed);

			Vector3 pan = eye.clone().cross(getObject().getUp()).setLength(mouseChange.getX());
			pan.add(getObject().getUp().clone().setLength(mouseChange.getY()));

			getObject().getPosition().add(pan);
			target.add(pan);

			if (isDynamicUpdate)
				panStart.add(mouseChange.sub(panEnd, panStart).multiply(dynamicDampingFactor));
			else
				panStart = panEnd;
		}
	}

	/**
	 * Update eye and target for zooming.
	 */
	private void zoomCamera() {
		Object3D camera = getObject();
		if (camera instanceof PerspectiveCamera) {
			eye.multiply(zoomChange);
			double eyedist2 = eye.lengthSq();
			if (eyedist2 > MAX_EYE_DIST_SQ)
				eye.setLength(MAX_EYE_DIST);
			else if (eyedist2 < MIN_EYE_DIST_SQ)
				eye.setLength(MIN_EYE_DIST);
			return;
		}
		if (camera instanceof OrthographicCamera) {
			double zoom = camera.getScale().multiply(zoomChange).getZ();
			if (zoom > 5.0)
				camera.getScale().set(5.0, 5.0, 5.0);
			else if (zoom < 0.05)
				camera.getScale().set(0.05, 0.05, 0.05);
			camera.updateMatrix();
		}
	}

	/**
	 * Update eye and target for (dynamical) rotating.
	 */
	private void rotateCamera() {
		double angle = Math.acos(rotateStart.dot(rotateEnd) / (rotateStart.length() * rotateEnd.length()));
		if (angle > 0.0) {
			Vector3 axis = (new Vector3()).cross(rotateStart, rotateEnd).normalize();
			Quaternion quaternion = new Quaternion();

			angle *= rotateSpeed;
			quaternion.setFromAxisAngle(axis, -angle);
			quaternion.multiplyVector3(eye);
			quaternion.multiplyVector3(getObject().getUp());
			quaternion.multiplyVector3(rotateEnd);

			if (isDynamicUpdate) {
				quaternion.setFromAxisAngle(axis, angle * (dynamicDampingFactor - 1.0));
				quaternion.multiplyVector3(rotateStart);
			} else {
				rotateStart = rotateEnd;
			}
		}
	}
}
