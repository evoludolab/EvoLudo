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

import com.google.gwt.user.client.ui.Label;

/**
 * Component of the context menu extension to GWT's user interface.
 * <p>
 * Provides a separator between (collections of) context menu items. Typically
 * a thin horizontal line.
 * <p>
 * For CSS styling the context menu separators have the class
 * <code>gwt-ContextMenuSeparator</code>.
 * 
 * @author Christoph Hauert
 * 
 * @see ContextMenu
 */
public class ContextMenuSeparator extends Label {

	/**
	 * Creates a new separator to structure entries in context menus.
	 */
	public ContextMenuSeparator() {
		setStyleName("gwt-ContextMenuSeparator");
	}
}
