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

import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTML;

/**
 * Small CSS-driven spinner widget with optional delayed activation.
 */
public class Spinner extends Composite {

	/**
	 * Base style of the spinner widget.
	 */
	private static final String STYLE_NAME = "evoludo-Spinner";

	/**
	 * Style added while the spinner should be visible.
	 */
	private static final String STYLE_ACTIVE = "evoludo-Spinner-active";

	/**
	 * HTML structure for the CSS-only spinner.
	 */
	private static final String SPINNER_HTML = "<span></span><span></span><span></span><span></span>"
			+ "<span></span><span></span><span></span><span></span>";

	/**
	 * Delay in milliseconds before the spinner becomes visible.
	 */
	private int delay = 0;

	/**
	 * <code>true</code> while spinning has been requested.
	 */
	private boolean spinning = false;

	/**
	 * <code>true</code> while the spinner is currently visible.
	 */
	private boolean visible = false;

	/**
	 * Timer to defer showing the spinner for short-lived tasks.
	 */
	private final Timer showTimer = new Timer() {
		@Override
		public void run() {
			if (spinning)
				show();
		}
	};

	/**
	 * Create a new spinner widget.
	 */
	public Spinner() {
		initWidget(new HTML(SPINNER_HTML));
	}

	@Override
	protected void onLoad() {
		super.onLoad();
		addStyleName(STYLE_NAME);
	}

	/**
	 * Set the activation delay in milliseconds.
	 *
	 * @param delay the delay before showing the spinner
	 */
	public void setDelay(int delay) {
		this.delay = Math.max(0, delay);
		if (spinning && !visible) {
			showTimer.cancel();
			if (this.delay <= 0)
				show();
			else
				showTimer.schedule(this.delay);
		}
	}

	/**
	 * Set whether the spinner should be spinning.
	 *
	 * @param spinning <code>true</code> to start spinning
	 */
	public void setSpinning(boolean spinning) {
		if (this.spinning == spinning)
			return;
		this.spinning = spinning;
		if (!spinning) {
			showTimer.cancel();
			hide();
			return;
		}
		if (delay <= 0) {
			show();
			return;
		}
		showTimer.schedule(delay);
	}

	/**
	 * Show the spinner.
	 */
	private void show() {
		if (visible)
			return;
		visible = true;
		addStyleName(STYLE_ACTIVE);
	}

	/**
	 * Hide the spinner.
	 */
	private void hide() {
		if (!visible)
			return;
		visible = false;
		removeStyleName(STYLE_ACTIVE);
	}
}
