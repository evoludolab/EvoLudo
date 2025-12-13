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

package org.evoludo.simulator.ui;

import org.evoludo.simulator.EvoLudoGWT;
import org.evoludo.ui.FullscreenChangeEvent;
import org.evoludo.ui.FullscreenChangeHandler;
import org.evoludo.util.CLOCategory;
import org.evoludo.util.CLODelegate;
import org.evoludo.util.CLOption;
import org.evoludo.util.CLOParser;
import org.evoludo.util.NativeJS;

import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.Widget;

/**
 * Manages fullscreen handling and GUI sizing for EvoLudoWeb instances.
 */
public class FSController implements FullscreenChangeHandler {

	/**
	 * Command line option to set the size of the GUI or enter fullscreen.
	 */
	private final CLOption cloSize = new CLOption("size", "530,620", CLOCategory.GUI,
			"--size <w,h|fullscreen>  size of GUI, w: width, h: height", new CLODelegate() {
				@Override
				public boolean parse(String arg) {
					if (arg == null)
						return false;
					if (arg.startsWith("full")) {
						if (NativeJS.isFullscreenSupported()) {
							engine.setFullscreen(true);
							return true;
						}
						arg = cloSize.getDefault();
					}
					double[] dim = CLOParser.parseVector(arg);
					if (dim.length != 2)
						return false;
					if (fullscreenWidget == null)
						return false;
					fullscreenWidget.setSize((int) dim[0] + "px", (int) dim[1] + "px");
					return true;
				}
			});

	/**
	 * Reference to the engine so fullscreen requests propagate to the core.
	 */
	private final EvoLudoGWT engine;

	/**
	 * Widget whose size/fullscreen state is controlled (typically the lab
	 * container).
	 */
	private Widget fullscreenWidget;

	/**
	 * Registration for the native fullscreen change handler.
	 */
	private HandlerRegistration fullscreenHandler;

	/**
	 * Create a new controller for the given engine.
	 *
	 * @param engine the owning EvoLudoGWT instance
	 * @param widget widget whose size/fullscreen state should be managed
	 */
	// GWT 2.12.2 is unable to handle HandlerRegistration with lambda here
	@SuppressWarnings("java:S1604")
	public FSController(EvoLudoGWT engine, Widget widget) {
		this.engine = engine;
		this.fullscreenWidget = widget;
		if (fullscreenWidget != null)
			engine.setFullscreenElement(fullscreenWidget.getElement());
		if (fullscreenHandler != null || !NativeJS.isFullscreenSupported())
			return;
		final String eventname = NativeJS.fullscreenChangeEventName();
		if (eventname == null)
			return;
		NativeJS.addFullscreenChangeHandler(eventname, this);
		fullscreenHandler = new HandlerRegistration() {
			@Override
			public void removeHandler() {
				NativeJS.removeFullscreenChangeHandler(eventname, FSController.this);
			}
		};
	}

	/**
	 * Return the CLO option controlling fullscreen/sizing.
	 *
	 * @return the {@link CLOption} for --size
	 */
	public CLOption getCloSize() {
		return cloSize;
	}

	/**
	 * Apply the --size option (or its default) using the currently registered
	 * widget.
	 */
	public void parseSize() {
		cloSize.parse();
	}

	/**
	 * React to browser fullscreen toggles by adding/removing the helper CSS class.
	 */
	@Override
	public void onFullscreenChange(FullscreenChangeEvent event) {
		if (NativeJS.isFullscreen()) {
			NativeJS.getFullscreenElement().addClassName("fullscreen");
		} else if (fullscreenWidget != null) {
			fullscreenWidget.getElement().removeClassName("fullscreen");
		}
	}
}
