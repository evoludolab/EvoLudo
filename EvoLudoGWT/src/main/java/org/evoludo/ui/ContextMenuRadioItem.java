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

import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.user.client.ui.Widget;

/**
 * Component of the context menu extension to GWT's user interface.
 * <p>
 * Implements mutually exclusive radio items in a context menu. Selecting one
 * item deselects all other radio items in the same menu or submenu.
 *
 * <h2>CSS Style Rules</h2>
 * <p>
 * Same as for {@link ContextMenuItem} plus
 * <dl>
 * <dt>.gwt-ContextMenuRadioItem</dt>
 * <dd>the radio item element.</dd>
 * <dt>.gwt-ContextMenuRadioItem-selected</dt>
 * <dd>if the context menu item is selected.</dd>
 * </dl>
 *
 * @author Christoph Hauert
 *
 * @see ContextMenu
 */
@SuppressWarnings("java:S110")
public class ContextMenuRadioItem extends ContextMenuItem {

	/**
	 * {@code true} if the radio item is selected.
	 */
	private boolean isSelected = false;

	/**
	 * Create a new radio menu item with title {@code text}. Initially the menu item
	 * is not selected. Clicking the menu item executes {@code cmd}.
	 *
	 * @param text title of menu item
	 * @param cmd  command to execute when clicked
	 */
	public ContextMenuRadioItem(String text, ScheduledCommand cmd) {
		this(text, false, cmd);
	}

	/**
	 * Create a new radio menu item with title {@code text}.
	 *
	 * @param text       title of menu item
	 * @param isSelected {@code true} if initially selected
	 * @param cmd        command to execute when clicked
	 */
	public ContextMenuRadioItem(String text, boolean isSelected, ScheduledCommand cmd) {
		super(text, cmd);
		this.isSelected = isSelected;
	}

	@Override
	protected void onLoad() {
		super.onLoad();
		setStylePrimaryName("gwt-ContextMenuRadioItem");
		setSelected(isSelected);
	}

	@Override
	public void action() {
		if (!isEnabled())
			return;
		if (getParent() instanceof ContextMenu) {
			ContextMenu menu = (ContextMenu) getParent();
			for (int n = 0; n < menu.getWidgetCount(); n++) {
				Widget item = menu.getWidget(n);
				if (item instanceof ContextMenuRadioItem)
					((ContextMenuRadioItem) item).setSelected(item == this);
			}
		} else {
			setSelected(true);
		}
		super.action();
	}

	/**
	 * Set selection state of radio item.
	 *
	 * @param selected {@code true} to select the item
	 */
	public void setSelected(boolean selected) {
		isSelected = selected;
		setStyleDependentName("selected", selected);
	}

	/**
	 * Check whether the radio item is selected.
	 *
	 * @return {@code true} if selected
	 */
	public boolean isSelected() {
		return isSelected;
	}
}
