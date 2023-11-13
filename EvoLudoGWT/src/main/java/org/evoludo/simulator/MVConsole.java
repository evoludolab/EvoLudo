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

import java.util.logging.Level;

import org.evoludo.ui.ContextMenu;
import org.evoludo.ui.ContextMenuItem;
import org.evoludo.simulator.models.Model;

import com.google.gwt.dom.client.Element;
import com.google.gwt.event.dom.client.ContextMenuEvent;
import com.google.gwt.event.dom.client.ContextMenuHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.HTML;

/**
 *
 * @author Christoph Hauert
 */
public class MVConsole extends MVAbstract implements ContextMenu.Provider {

	public static class Console extends HTML implements ContextMenu.Listener {
		@Override
		public HandlerRegistration addContextMenuHandler(ContextMenuHandler handler) {
			return addDomHandler(handler, ContextMenuEvent.getType());
		}
	}

	protected Console log;
	protected ContextMenu contextMenu;

	public MVConsole(EvoLudoGWT engine) {
		super(engine, Model.Data.UNDEFINED);
	}

	@Override
	public String getName() {
		return "Console log";
	}

	@Override
	public void createWidget() {
		super.createWidget();
		log = new Console();
		log.setStylePrimaryName("evoludo-Log");
		wrapper.add(log);
		contextMenu = ContextMenu.sharedContextMenu();
		contextMenu.add(log, this);
	}

	/**
	 * Always returns <code>false</code> to disable fullscreen for console
	 * 
	 * @return <code>false</code>
	 */
	@Override
	public boolean isFullscreenSupported() {
		return false;
	}

	/**
	 *
	 */
	public void clearLog() {
		log.setHTML("");
	}

	/**
	 * Log message in console. The output is prettified by coloring messages
	 * according to their severity:
	 * <dl>
	 * <dt>{@link Level#SEVERE}</dt>
	 * <dd><span style="color:red;">error level, typeset in red color and labeled
	 * with <strong>ERROR:</strong>.</span> Severe issues require immediate
	 * attention. May not be possible to recover and proceed.</dd>
	 * <dt>{@link Level#WARNING}</dt>
	 * <dd><span style="color:orange;">warning level, typeset in orange color and
	 * labeled with <strong>Warning:</strong>.</span> Milder issues encountered that
	 * were typically resolved automatically. For example, settings may have changed
	 * with possibly unintended consequences.</dd>
	 * <dt>{@link Level#INFO}</dt>
	 * <dd>default level. Use for reporting of general information and
	 * milestones.</dd>
	 * <dt>{@link Level#FINE}, {@link Level#FINER}, {@link Level#FINEST}</dt>
	 * <dd><span style="color:blue;">debug level typeset in blue color and labeled
	 * with <strong>DEBUG:</strong>.</span> Used for verbose messaging to assist in
	 * debugging.</dd>
	 * </dl>
	 * 
	 * @param msg   the message to log
	 * @param level the severity level of the message
	 */
	public void log(Level level, String msg) {
		String pretty = msg;
		if (level == Level.SEVERE)
			pretty = "<span style='color:red;'><b>ERROR:</b> " + msg + "</span>";
		else if (level == Level.WARNING)
			pretty = "<span style='color:orange;'><b>Warning:</b> " + msg + "</span>";
		else if (level == Level.FINE || level == Level.FINER || level == Level.FINEST)
			pretty = "<span style='color:blue;'>DEBUG: " + msg + "</span>";
		// abuse of Level.CONFIG for progress reports (GWT does not support custom
		// levels)
		if (level != Level.CONFIG)
			pretty += "<br/>";
		Element ele = log.getElement();
		int scroll = ele.getScrollHeight();
		int top = ele.getScrollTop();
		log.setHTML(log.getHTML() + pretty);
		if (scroll - top - ele.getClientHeight() < 1)
			ele.setScrollTop(scroll);
	}

	@Override
	public void update(boolean force) {
	}

	@Override
	public void onResize() {
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * The console ignores most shortcuts but redefines the following:
	 * <dl>
	 * <dt>{@code Backspace, Delete}</dt>
	 * <dd>Clear the log.</dd>
	 * </dl>
	 */
	@Override
	public boolean keyUpHandler(String key) {
		switch (key) {
			case "Backspace":
			case "Delete":
				// clear log (if active)
				if (!isActive)
					break;
				clearLog();
				return true;
			default:
		}
		return super.keyUpHandler(key);
	}

	private ContextMenuItem clearMenu;

	@Override
	public void populateContextMenuAt(ContextMenu menu, int x, int y) {
		// add menu to clear canvas
		if (clearMenu == null) {
			clearMenu = new ContextMenuItem("Clear", new Command() {
				@Override
				public void execute() {
					clearLog();
				}
			});
		}
		menu.add(clearMenu);
		populateContextMenu(menu);
	}
}
