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
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.InputElement;
import com.google.gwt.dom.client.Touch;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.HasChangeHandlers;
import com.google.gwt.event.dom.client.TouchEndEvent;
import com.google.gwt.event.dom.client.TouchEndHandler;
import com.google.gwt.event.dom.client.TouchEvent;
import com.google.gwt.event.dom.client.TouchMoveEvent;
import com.google.gwt.event.dom.client.TouchMoveHandler;
import com.google.gwt.event.dom.client.TouchStartEvent;
import com.google.gwt.event.dom.client.TouchStartHandler;
import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.ui.FocusWidget;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.RootPanel;

/**
 * A standard slider widget. The slider handles both linear and logarithmic
 * scales.
 * 
 * <h2>CSS Style Rules</h2>
 * <dl>
 * <dt>.gwt-Slider</dt>
 * <dd>the <code>input</code> element.</dd>
 * </dl>
 * <h3>Example</h3> <blockquote>
 * 
 * <pre>
 * public class SliderExample implements EntryPoint {
 * 	public void onModuleLoad() {
 * 		// Make a new slider and display its value on label.
 * 		VerticalPanel p = new VerticalPanel();
 * 		Label l = new Label("Move slider...");
 * 		Slider s = new Slider();
 * 		s.addChangeHandler(new ChangeHandler() {
 * 			&#64;Override
 * 			public void onChange(ChangeEvent event) {
 * 				l.setText("Slider at " + s.getValue());
 * 			}
 * 		});
 * 		p.add(l);
 * 		p.add(s);
 * 		// Add it to the root panel.
 * 		RootPanel.get().add(p);
 * 	}
 * }
 * </pre>
 * 
 * </blockquote>
 * 
 * @author Christoph Hauert
 */
public class Slider extends FocusWidget implements HasChangeHandlers, ChangeHandler, ClickHandler, TouchStartHandler,
		TouchEndHandler, TouchMoveHandler, HasInputHandlers, InputHandler {

	/**
	 * Default minimum value of slider.
	 */
	private static final double SLIDER_MIN = 0.0;

	/**
	 * Default maximum value of slider.
	 */
	private static final double SLIDER_MAX = 100.0;

	/**
	 * Default number of steps between minimum and maximum of slider.
	 */
	private static final int SLIDER_STEPS = 100;

	/**
	 * Reference to slider. The &lt;input&gt; element.
	 */
	protected HTML html;

	/**
	 * Title of slider. This string is displayed as the slider's tooltip and is
	 * updated to show the slider's current value.
	 */
	protected String title;

	/**
	 * For logarithmic sliders this is the base of the logarithm. For
	 * <code>logBase&le;0</code> the slider has a linear scale.
	 */
	protected double logBase = -1.0;

	/**
	 * <code>true</code> if minimum and maximum of slider are reversed.
	 */
	protected boolean reversed = false;

	/**
	 * Minimum value of slider.
	 * 
	 * @see #SLIDER_MIN
	 */
	protected double min;

	/**
	 * Maximum value of slider.
	 * 
	 * @see #SLIDER_MAX
	 */
	protected double max;

	/**
	 * Initial value of slider.
	 */
	protected double init;

	/**
	 * Number of steps between minimum and maximum value of slider.
	 * 
	 * @see #SLIDER_STEPS
	 */
	protected int steps;

	/**
	 * Helper variable to the logarithm of the slider range. Reduces calls to the
	 * fairly expensive {@link Math#log(double)}.
	 */
	protected double logRange = -1.0;

	/**
	 * The change event handler registration.
	 */
	ChangeHandler changeHandler;

	/**
	 * The input event handler registration.
	 */
	HandlerRegistration inputRegistration;

	/**
	 * The click event handler registration.
	 */
	HandlerRegistration clickRegistration;

	/**
	 * The touch start event handler registration.
	 */
	HandlerRegistration touchStartRegistration;

	/**
	 * The touch end event handler registration.
	 */
	HandlerRegistration touchEndRegistration;

	/**
	 * The touch move event handler registration.
	 */
	HandlerRegistration touchMoveRegistration;

	/**
	 * The change event handler registration.
	 */
	HandlerRegistration changeRegistration;

	/**
	 * Creates a Slider widget that wraps an existing &lt;input&gt; element. This
	 * element must already be attached to the document. If the element is removed
	 * from the document, you must call RootPanel.detachNow(Widget).
	 * 
	 * @param element the element to be wrapped
	 * @return Slider widget wrapping <code>element</code>
	 */
	public static Slider wrap(com.google.gwt.dom.client.Element element) {
		// Assert that the element is attached.
		if (!Document.get().getBody().isOrHasChild(element))
			throw new IllegalArgumentException("Element must be attached to the document.");

		Slider slider = new Slider(element);
		// Mark it attached and remember it for cleanup.
		slider.onAttach();
		// the deprecation of detachOnWindowClose is a GWT issue - wait for their fix...
		RootPanel.detachOnWindowClose(slider);

		return slider;
	}

	/**
	 * Creates a slider ranging from {@value #SLIDER_MIN} to {@value #SLIDER_MAX}
	 * with {@value #SLIDER_STEPS} steps and initial value
	 * ({@value #SLIDER_MIN}+{@value #SLIDER_MAX})/2.
	 */
	public Slider() {
		this(SLIDER_MIN, SLIDER_MAX);
	}

	/**
	 * Creates a slider ranging from {@value #SLIDER_MIN} to {@value #SLIDER_MAX}
	 * with {@value #SLIDER_STEPS} steps and initial value
	 * ({@value #SLIDER_MIN}+{@value #SLIDER_MAX})/2 as well as a change handler.
	 * 
	 * @param handler the change handler
	 */
	public Slider(ChangeHandler handler) {
		this(SLIDER_MIN, SLIDER_MAX, handler);
	}

	/**
	 * Creates a slider ranging from <code>min</code> to <code>max</code> with
	 * {@value #SLIDER_STEPS} steps and initial value <code>(min+max)/2</code>.
	 * 
	 * @param min the minimum value
	 * @param max the maximum value
	 */
	public Slider(double min, double max) {
		this(min, max, (min + max) / 2, SLIDER_STEPS);
	}

	/**
	 * Creates a slider ranging from <code>min</code> to <code>max</code> with
	 * {@value #SLIDER_STEPS} steps and initial value <code>(min+max)/2</code> as
	 * well as a change handler.
	 * 
	 * @param min     the minimum value
	 * @param max     the maximum value
	 * @param handler the change handler
	 */
	public Slider(double min, double max, ChangeHandler handler) {
		this(min, max, (min + max) / 2, SLIDER_STEPS, handler);
	}

	/**
	 * Creates a slider ranging from <code>min</code> to <code>max</code> with
	 * <code>steps</code> increments and initial value <code>init</code>.
	 * 
	 * @param min   the minimum value
	 * @param max   the maximum value
	 * @param init  the initial value
	 * @param steps the number of steps
	 */
	public Slider(double min, double max, double init, int steps) {
		this(min, max, init, steps, null);
	}

	/**
	 * Creates a slider ranging from <code>min</code> to <code>max</code> with
	 * <code>steps</code> increments and initial value <code>init</code>.
	 * 
	 * @param min   the minimum value
	 * @param max   the maximum value
	 * @param init  the initial value
	 * @param steps the number of steps
	 */
	public Slider(double min, double max, double init, int steps, ChangeHandler handler) {
		html = new HTML("<input class='gwt-Slider' style='width:100%' type='range' min='" + SLIDER_MIN + "' max='"
				+ SLIDER_MAX + "' step='" + ((SLIDER_MAX - SLIDER_MIN) / SLIDER_STEPS) + "' value='"
				+ ((SLIDER_MAX + SLIDER_MIN) / 2) + "' />");
		// the deprecated setElement is a GWT issue - wait for their fix...
		setElement(html.getElement());
		this.changeHandler = (handler == null ? this : handler);
		this.min = min;
		this.max = max;
		this.init = init;
		this.steps = steps;
	}

	/**
	 * Creates a slider ranging from <code>min</code> to <code>max</code> with
	 * <code>steps</code> increments and initial value <code>init</code> as well as
	 * a change handler.
	 * 
	 * @param min     the minimum value
	 * @param max     the maximum value
	 * @param init    the initial value
	 * @param steps   the number of steps
	 * @param handler the change handler
	 */
	public Slider(int min, int max, int init, int steps, ChangeHandler handler) {
		this((double) min, (double) max, (double) init, steps, handler);
	}

	/**
	 * This constructor may be used by subclasses to explicitly use an existing
	 * element. This element must be a &lt;input&gt; element.
	 * 
	 * @param element the element to be used
	 */
	protected Slider(com.google.gwt.dom.client.Element element) {
		super(element.<Element>cast());
		InputElement.as(element);
	}

	@Override
	protected void onLoad() {
		super.onLoad();
		setStyleName("gwt-Slider");
		// set range may already have been called - careful with reversed sliders
		setRange(getMin(), getMax());
		setSteps(steps);
		setValue(init);
		inputRegistration = addInputHandler(this);
		clickRegistration = addClickHandler(this);
		touchStartRegistration = addTouchStartHandler(this);
		touchEndRegistration = addTouchEndHandler(this);
		touchMoveRegistration = addTouchMoveHandler(this);
		changeRegistration = addChangeHandler(changeHandler);
	}

	@Override
	protected void onUnload() {
		if (inputRegistration != null)
			inputRegistration.removeHandler();
		if (clickRegistration != null)
			clickRegistration.removeHandler();
		if (touchStartRegistration != null)
			touchStartRegistration.removeHandler();
		if (touchEndRegistration != null)
			touchEndRegistration.removeHandler();
		if (touchMoveRegistration != null)
			touchMoveRegistration.removeHandler();
		if (changeRegistration != null)
			changeRegistration.removeHandler();
		super.onUnload();
	}

	/**
	 * Get the input element underlying the slider.
	 * 
	 * @return the {@link InputElement}
	 */
	protected InputElement getInputElement() {
		return InputElement.as(DOM.getFirstChild(html.getElement()));
	}

	/**
	 * Set left and right values of slider. Left values that exceed right values are
	 * acceptable.
	 * <p>
	 * <strong>Note:</strong> HTML sliders must have the minimum on the left and the
	 * maximum on the right. This implementation automatically takes care of the
	 * conversion if the maximum is on the left and the minimum on the right.
	 * 
	 * @param left  the left slider value
	 * @param right the right slider value
	 */
	public void setRange(double left, double right) {
		double value = Double.parseDouble(getInputElement().getValue());
		double pos = (value - left) / (right - left);
		reversed = (left > right);
		if (reversed) {
			min = right;
			max = left;
		} else {
			min = left;
			max = right;
		}
		InputElement input = getInputElement();
		input.setAttribute("min", String.valueOf(min));
		input.setAttribute("max", String.valueOf(max));
		input.setValue(String.valueOf(min + pos * (max - min)));
		update();
	}

	/**
	 * Get the minimum value of the slider.
	 * 
	 * @return the minimum value
	 */
	public double getMin() {
		return (reversed ? max : min);
	}

	/**
	 * Get the maximum value of the slider.
	 * 
	 * @return the maximum value
	 */
	public double getMax() {
		return (reversed ? min : max);
	}

	/**
	 * Set the number of steps between the minimum and maximum values of the slider.
	 * 
	 * @param steps the number of steps
	 */
	public void setSteps(int steps) {
		this.steps = steps;
		double incr = (max - min) / steps;
		getInputElement().setAttribute("step", String.valueOf(incr));
	}

	/**
	 * Set the base for a logarithmic slider.
	 * 
	 * @param base the logarithmic base of slider
	 * @throws IllegalArgumentException if <code>base&le;0</code> or the minimum
	 *                                  and/or maximum values of the slider are
	 *                                  <code>&le;0</code>
	 */
	public void setLogBase(double base) {
		if (base <= 0.0)
			throw new IllegalArgumentException("logarithmic base must be positive.");
		if (max <= 0.0 || min <= 0.0)
			throw new IllegalArgumentException("min and max of logarithmic range must be both positive.");
		// possible to work with logarithmic scales for min<0 and max<0 by
		// reinterpreting range - worth the hassle?
		// if( max*min<=0.0 )
		// throw new IllegalArgumentException("logarithmic range cannot include zero.");

		double newLogBase = Math.log(base);
		// logBase == -1 for linear sliders
		if (Math.abs(logBase - newLogBase) < 1e-8)
			return;
		// process change of base
		double value = getValue();
		logBase = newLogBase;
		setValue(value);
		update();
	}

	/**
	 * Sets a linear scale for the slider.
	 */
	public void setLinear() {
		if (logBase < 0.0)
			return;
		double value = getValue();
		// process change of base
		logBase = -1.0;
		setValue(value);
		update();
	}

	/**
	 * Updates the <code>logRange</code> if needed as well as the tooltip of the
	 * slider.
	 */
	private void update() {
		if (logBase > 0.0)
			logRange = (Math.log(max) - Math.log(min)) / logBase;
		updateTitle();
	}

	/**
	 * Helper function to convert from linear to logarithmic scales.
	 * 
	 * @param x linear value
	 * @return logarithmic value
	 */
	private double lin2log(double x) {
		return Math.exp((x - min) / (max - min) * logRange * logBase);
	}

	/**
	 * Helper function to convert from logarithmic to linear scales.
	 * 
	 * @param x logarithmic value
	 * @return linear value
	 */
	private double log2lin(double x) {
		return Math.log(x) / (logBase * logRange) * (max - min) + min;
	}

	@Override
	public void setEnabled(boolean enabled) {
		getInputElement().setDisabled(!enabled);
	}

	/**
	 * Get the value of slider taking its linear or logarithmic scaling into
	 * account.
	 * 
	 * @return current value of slider
	 */
	public double getValue() {
		double value = Double.parseDouble(getInputElement().getValue());
		if (reversed)
			value = max - (value - min);
		if (logBase > 0.0)
			value = lin2log(value);
		return value;
	}

	/**
	 * Set the value of slider taking its linear or logarithmic scaling into
	 * account.
	 * 
	 * @param value the new value of the slider
	 */
	public void setValue(double value) {
		if (logBase > 0.0)
			value = log2lin(value);
		if (reversed)
			value = min + (max - value);
		getInputElement().setValue(String.valueOf(value));
		updateTitle();
	}

	@Override
	public void setTitle(String title) {
		this.title = title;
		updateTitle();
	}

	/**
	 * Updates the tooltip of the slider. By default it shows the sliders minimum,
	 * maximum and current value.
	 */
	protected void updateTitle() {
		if (title == null)
			return;
		// unfortunately GWT does not implement String.format...
		// getElement().setTitle(String.format(title, getValue()));
		String tooltip = title.replace("{min}", String.valueOf(min));
		tooltip = tooltip.replace("{max}", String.valueOf(max));
		tooltip = tooltip.replace("{value}", String.valueOf((int) (getValue() + 0.5)));
		getElement().setTitle(tooltip);
	}

	@Override
	public HandlerRegistration addInputHandler(InputHandler handler) {
		return addDomHandler(handler, InputEvent.getType());
	}

	@Override
	public void onInput(InputEvent event) {
		updateTitle();
	}

	@Override
	public void onClick(ClickEvent event) {
		// ok but slight jumps with mouse; blinks on first touch...
		double x = min + event.getX() * (max - min) / getOffsetWidth();
		getInputElement().setValue(String.valueOf(x));
		// event.preventDefault();
	}

	@Override
	public void onTouchStart(TouchStartEvent event) {
		touchSlider(event);
	}

	@Override
	public void onTouchMove(TouchMoveEvent event) {
		touchSlider(event);
	}

	@Override
	public void onTouchEnd(TouchEndEvent event) {
		touchSlider(event);
	}

	/**
	 * Helper function to update the slider value based on touch events.
	 * 
	 * @param <H>   the handler type
	 * @param event the touch event
	 */
	private <H extends EventHandler> void touchSlider(TouchEvent<H> event) {
		JsArray<Touch> touches = event.getTouches();
		if (touches.length() > 1)
			// ignore multi-touches/gestures
			return;
		Touch touch = touches.get(0);
		InputElement ie = getInputElement();
		double x = min + touch.getRelativeX(ie) * (max - min) / getOffsetWidth();
		ie.setValue(String.valueOf(x));
	}

	@Override
	public HandlerRegistration addChangeHandler(ChangeHandler handler) {
		return addDomHandler(handler, ChangeEvent.getType());
	}

	@Override
	public void onChange(ChangeEvent event) {
		updateTitle();
	}
}
