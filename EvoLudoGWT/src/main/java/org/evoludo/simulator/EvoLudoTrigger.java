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

package org.evoludo.simulator;

import org.evoludo.EvoLudoWeb;
import org.evoludo.ui.ContextMenu;
import org.evoludo.util.NativeJS;

import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.dom.client.Style;
import com.google.gwt.dom.client.Style.Position;
import com.google.gwt.dom.client.Style.TextAlign;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.dom.client.Style.VerticalAlign;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.MouseDownEvent;
import com.google.gwt.event.dom.client.MouseDownHandler;
import com.google.gwt.event.dom.client.MouseOutEvent;
import com.google.gwt.event.dom.client.MouseOutHandler;
import com.google.gwt.event.dom.client.MouseOverEvent;
import com.google.gwt.event.dom.client.MouseOverHandler;
import com.google.gwt.event.dom.client.TouchMoveEvent;
import com.google.gwt.event.dom.client.TouchMoveHandler;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.FocusPanel;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.PushButton;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.SimplePanel;

/**
 * Create button to launch an EvoLudo lab in a semi-transparent overlay.
 * 
 * @author Christoph Hauert
 */
public class EvoLudoTrigger extends PushButton {

	/**
	 * Lightbox panel to overlay the page.
	 */
	LightboxPanel popup = null;

	/**
	 * Flag to keep track of mouse position. {@code true} if mouse hovers over lab.
	 */
	boolean mouseOverLab = false;

	/**
	 * Create a new EvoLudo trigger button for the HTML element with ID {@code id}.
	 * 
	 * @param id the id of the element
	 */
	public EvoLudoTrigger(final String id) {
		addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				if (popup != null) {
					// NOTE: onLoad will be called automatically - do not call explicitly as it will
					// cause trouble with 3D stuff
					popup.show();
					return;
				}
				popup = new LightboxPanel();
				EvoLudoWeb lab = new EvoLudoWeb(id, popup);
				FocusPanel panel = new FocusPanel();
				panel.addStyleName("evoludo-simulation");
				Style style = panel.getElement().getStyle();
				style.setProperty("borderRadius", "5px");
				style.setProperty("borderWidth", "thick");
				style.setPosition(Position.ABSOLUTE);
				style.setProperty("transform", "translate(-50%, -35%)");
				style.setTop(33, Unit.PCT);
				style.setLeft(50, Unit.PCT);
				panel.setWidget(lab);
				popup.add(panel);
				panel.addMouseOverHandler(new MouseOverHandler() {
					@Override
					public void onMouseOver(MouseOverEvent moe) {
						mouseOverLab = true;
					}
				});
				panel.addMouseOutHandler(new MouseOutHandler() {
					@Override
					public void onMouseOut(MouseOutEvent moe) {
						mouseOverLab = false;
					}
				});
				popup.show();
			}
		});
		Image logo = new Image(Resources.INSTANCE.logoSmall());
		getElement().appendChild(logo.getElement());
		logo.setSize("4ex", "4ex");
		Style logoStyle = logo.getElement().getStyle();
		logoStyle.setVerticalAlign(VerticalAlign.MIDDLE);
		logoStyle.setMarginRight(1.5, Unit.EX);
		getElement().appendChild(new HTMLPanel("span", "explore simulations").getElement());
		addStyleName("gwt-Button");
		setWidth("24ex");
		Style triggerStyle = getElement().getStyle();
		triggerStyle.setTextAlign(TextAlign.CENTER);
		triggerStyle.setVerticalAlign(VerticalAlign.MIDDLE);
		triggerStyle.setLineHeight(5, Unit.EX);
	}

	/**
	 * The panel to provide a transparent overlay over the entire page and displays
	 * the EvoLudo lab. A left-click with the mouse (or tap) outside the lab closes
	 * the overlay, except if the context menu is visible or the lab is shown in
	 * fullscreen.
	 */
	public class LightboxPanel extends SimplePanel {

		/**
		 * Create a new lightbox panel.
		 */
		public LightboxPanel() {
			DOM.sinkEvents(getElement(), Event.ONKEYDOWN);
			addBitlessDomHandler(new MouseDownHandler() {
				@Override
				public void onMouseDown(MouseDownEvent event) {
					if (mouseOverLab || event.getNativeButton() != NativeEvent.BUTTON_LEFT || ContextMenu.isShowing()
							|| NativeJS.isFullscreen())
						return;
					close();
				}
			}, MouseDownEvent.getType());
			addBitlessDomHandler(new TouchMoveHandler() {
				@Override
				public void onTouchMove(TouchMoveEvent event) {
					event.preventDefault();
				}
			}, TouchMoveEvent.getType());
		}

		@Override
		public void onBrowserEvent(Event event) {
			int type = event.getTypeInt();
			if (type == Event.ONKEYDOWN && event.getKeyCode() == KeyCodes.KEY_ESCAPE) {
				close();
				event.stopPropagation();
				return;
			}
			super.onBrowserEvent(event);
			event.stopPropagation();
		}

		/**
		 * Show the overlay.
		 */
		public void show() {
			// some styles get forgotten on detach... kindly remind element
			Style style = getElement().getStyle();
			style.setPosition(Position.FIXED);
			style.setLeft(0, Unit.PX);
			style.setTop(0, Unit.PX);
			style.setWidth(100, Unit.PCT);
			style.setHeight(100, Unit.PCT);
			style.setBackgroundColor("rgba(0, 0, 0, 0.666)");
			style.setProperty("alignItems", "center");
			// make sure overlay lies above page
			style.setZIndex(10);
			RootPanel root = RootPanel.get();
			root.getElement().getStyle().setProperty("overflow", "hidden");
			root.add(this);
		}

		/**
		 * Close the overlay.
		 */
		public void close() {
			popup.removeFromParent();
			RootPanel.get().getElement().getStyle().setProperty("overflow", "auto");
		}
	}
}
