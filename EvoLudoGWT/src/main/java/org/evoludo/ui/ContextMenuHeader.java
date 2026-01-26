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

import com.google.gwt.user.client.ui.Label;

/**
 * Component of the context menu extension to GWT's user interface.
 * <p>
 * Represents a header label in the context menu. The actual layout of the
 * header is controlled via CSS using the class
 * <code>gwt-ContextMenuHeader</code>.
 * 
 * @author Christoph Hauert
 */
public class ContextMenuHeader extends Label {

	/**
	 * Create a new context menu header with the given title.
	 * 
	 * @param title header text
	 */
	public ContextMenuHeader(String title) {
		super(title, false);
	}

	@Override
	protected void onLoad() {
		super.onLoad();
		setStyleName("gwt-ContextMenuHeader");
	}
}
