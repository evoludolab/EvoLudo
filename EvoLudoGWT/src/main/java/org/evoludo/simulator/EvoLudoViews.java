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

package org.evoludo.simulator;

import org.evoludo.ui.FullscreenChangeHandler;

import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.RequiresResize;

/**
 *
 * @author Christoph Hauert
 */
public interface EvoLudoViews extends RequiresResize, IsWidget, FullscreenChangeHandler {

	public String getName();

	public void load();

	public void unload();

	public void init();

	public default void reset() {
		reset(false);
	}

	public void reset(boolean soft);

	public void restored();

	public default void update() {
		update(false);
	}

	public void update(boolean force);

	public String getCounter();

	public String getStatus();

	public String getStatus(boolean force);

	public void activate(MVCallback callback);

	public void deactivate();

	/**
	 * Opportunity for view to implement keyboard shortcut for actions (non
	 * repeating). For example to clear the display or export graphics.
	 * 
	 * @param key the code of the released key
	 * @return {@code true} if the key was handled
	 * 
	 * @see org.evoludo.gwt.simulator.EvoLudoWeb#keyUpHandler(String)
	 */
	public default boolean keyUpHandler(String key) {
		return false;
	}

	/**
	 * Opportunity for view to implement keyboard shortcut for actions (repeating).
	 * If the key remains pressed this event is triggered repeatedly.
	 * 
	 * @param key the code of the pressed key
	 * @return {@code true} if the key was handled
	 * 
	 * @see org.evoludo.gwt.simulator.EvoLudoWeb#keyDownHandler(String)
	 */
	public default boolean keyDownHandler(String key) {
		return false;
	}
}
