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

import com.google.gwt.core.client.Scheduler.ScheduledCommand;

/**
 * Component of the context menu extension to GWT's user interface.
 * <p>
 * Implements context menu items with a check box to indicate whether the menu
 * item is active (checked) or not. The layout is controlled via CSS by adding
 * (or removing) the class name <code>checked</code> if menu item is active (or
 * not).
 * 
 * <h2>CSS Style Rules</h2>
 * <p>
 * Same as for {@link ContextMenuItem} plus
 * <dl>
 * <dt>.gwt-ContextMenuItem-checked</dt>
 * <dd>if the context menu item is checked.</dd>
 * </dl>
 * <p>
 * but cannot control a submenu.
 * 
 * @author Christoph Hauert
 * 
 * @see ContextMenu
 */
@SuppressWarnings("java:S110")
public class ContextMenuCheckBoxItem extends ContextMenuItem {

	/**
	 * <code>true</code> if context menu item is checked.
	 */
	private boolean isChecked = false;

	/**
	 * Create new menu item with check box and the title <code>text</code>.
	 * Initially the menu item is not active (unchecked). Clicking the menu item
	 * executes <code>cmd</code>.
	 * 
	 * @param text title of menu item
	 * @param cmd  command to execute when clicked
	 */
	public ContextMenuCheckBoxItem(String text, ScheduledCommand cmd) {
		this(text, false, cmd);
	}

	/**
	 * Create new menu item with check box and the title <code>text</code>.
	 * Initially the menu item is active if <code>checked==true</code>. Clicking the
	 * menu item executes <code>cmd</code>.
	 * 
	 * @param text    title of menu item
	 * @param checked <code>true</code> if initial state is active (checked)
	 * @param cmd     command to execute when clicked
	 */
	public ContextMenuCheckBoxItem(String text, boolean checked, ScheduledCommand cmd) {
		super(text, cmd);
		setChecked(checked);
	}

	/**
	 * Set state of check box in menu item to <code>checked</code>.
	 * 
	 * @param checked <code>true</code> to mark menu item as active (checked)
	 */
	public void setChecked(boolean checked) {
		this.isChecked = checked;
		setStyleDependentName("checked", checked);
	}

	/**
	 * Check if menu item is active (checked).
	 * 
	 * @return <code>true</code> if active (checked)
	 */
	public boolean isChecked() {
		return isChecked;
	}
}
