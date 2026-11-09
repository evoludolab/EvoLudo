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

import org.evoludo.util.NativeJS;

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style.Position;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.event.shared.HasHandlers;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.Widget;

/**
 * A widget that implements this interface provides registration for
 * {@link FullscreenChangeHandler} instances.
 * 
 * @author Christoph Hauert
 */
public interface HasFullscreenChangeHandlers extends HasHandlers {

	/**
	 * Adds a {@link FullscreenChangeEvent} handler.
	 * 
	 * @param handler the fullscreenchange handler
	 * @return {@link HandlerRegistration} used to remove this handler
	 */
	default HandlerRegistration addFullscreenChangeHandler(FullscreenChangeHandler handler) {
		String eventname = NativeJS.fullscreenChangeEventName();
		NativeJS.addFullscreenChangeHandler(eventname, handler);
		return () -> NativeJS.removeFullscreenChangeHandler(eventname, handler);
	}

	/**
	 * Place the shared, fixed-position {@code widget}, e.g. a context menu or
	 * tooltip, in the fullscreen element, if there is one, otherwise in the
	 * {@link RootPanel}, such that it remains visible in fullscreen mode.
	 * <p>
	 * <strong>Note:</strong> the widget is moved within the DOM rather than removed
	 * from its parent first, which would break its functionality.
	 * 
	 * @param widget the widget to place
	 */
	static void relocateForFullscreen(Widget widget) {
		Element fs = NativeJS.getFullscreenElement();
		if (fs != null)
			fs.appendChild(widget.getElement());
		else
			RootPanel.get().add(widget);
		// position fixed can get lost when relocating element
		widget.getElement().getStyle().setPosition(Position.FIXED);
	}
}
