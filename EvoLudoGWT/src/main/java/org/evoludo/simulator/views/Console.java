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

package org.evoludo.simulator.views;

import java.util.ListIterator;
import java.util.logging.Level;

import org.evoludo.graphics.AbstractGraph;
import org.evoludo.simulator.EvoLudoGWT;
import org.evoludo.simulator.models.Data;
import org.evoludo.ui.ContextMenu;
import org.evoludo.ui.ContextMenuCheckBoxItem;
import org.evoludo.ui.ContextMenuItem;
import org.evoludo.util.RingBuffer;

import com.google.gwt.dom.client.Element;
import com.google.gwt.event.dom.client.ContextMenuEvent;
import com.google.gwt.event.dom.client.ContextMenuHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Widget;

/**
 * The view to display the console log.
 *
 * @author Christoph Hauert
 */
public class Console extends AbstractView<AbstractGraph<?>> implements ContextMenu.Provider {

	/**
	 * The console log widget. The log is implemented as a ring buffer to store a
	 * limited number of messages. Once the buffer capacity is exceeded, the oldest
	 * messages are discarded. If the buffer capacity is set to {@code 0} the number
	 * of messsages is unlimited. The buffer is displayed in a HTML widget.
	 */
	@SuppressWarnings("java:S110")
	public static class Log extends HTML implements ContextMenu.Listener {

		/**
		 * The default capacity of the log buffer.
		 */
		public static final int DEFAULT_CAPACITY = 1000;

		/**
		 * The buffer to store the log messages.
		 */
		RingBuffer<String> buffer = new RingBuffer<>(DEFAULT_CAPACITY);

		/**
		 * Clear the log buffer and the display.
		 */
		public void clear() {
			buffer.clear();
			setHTML("");
		}

		/**
		 * Add a message to the log buffer.
		 * 
		 * @param msg the message to add
		 */
		public void add(String msg) {
			buffer.append(msg);
		}

		/**
		 * Replace the most recent entry in the log with {@code msg}.
		 * 
		 * @param msg the replacement entry
		 */
		public void replace(String msg) {
			buffer.replace(msg);
		}

		/**
		 * Show the log buffer in the HTML widget.
		 */
		public void show() {
			StringBuilder sb = new StringBuilder();
			ListIterator<String> bufit = buffer.listIterator(buffer.getSize());
			while (bufit.hasPrevious())
				sb.append(bufit.previous()).append("<br/>");
			setHTML(sb.toString());
		}

		@Override
		public HandlerRegistration addContextMenuHandler(ContextMenuHandler handler) {
			return addDomHandler(handler, ContextMenuEvent.getType());
		}
	}

	/**
	 * The console log.
	 */
	protected Log log;

	/**
	 * The context menu for the console.
	 */
	protected ContextMenu contextMenu;

	/**
	 * Create a new console log. This keeps a record of all messages logged by the
	 * model. By default the number of log entries is set to
	 * {@value Log#DEFAULT_CAPACITY}.
	 * 
	 * @param engine the pacemaker for running the model
	 */
	public Console(EvoLudoGWT engine) {
		super(engine, Data.UNDEFINED);
		log = new Log();
		wrapper.add(log);
	}

	@Override
	protected void onLoad() {
		super.onLoad();
		log.setStylePrimaryName("evoludo-Log");
		contextMenu = ContextMenu.sharedContextMenu();
		contextMenu.addListenerWithProvider(log, this);
	}

	@Override
	protected void onUnload() {
		log.setStylePrimaryName("evoludo-Log");
		contextMenu.removeListener(log);
		super.onUnload();
	}

	@Override
	public String getName() {
		return "Console log";
	}

	/**
	 * Clear the console log.
	 * <p>
	 * <strong>Note:</strong> Cannot override {@code clear()} method because this
	 * would clear the log on activiation.
	 */
	public void clearLog() {
		log.clear();
		log(Level.INFO, engine.getVersion());
	}

	@Override
	protected boolean allocateGraphs() {
		// console entertains no graphs
		return false;
	}

	/**
	 * String constant for opening a colored HTML span.
	 */
	static final String SPAN_OPEN = "<span style='color:";

	/**
	 * String constant for closing an HTML span.
	 */
	static final String SPAN_CLOSE = "</span>";

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
		StringBuilder sb = new StringBuilder();
		if (level == Level.SEVERE)
			sb.append(SPAN_OPEN).append("red;'><b>ERROR:</b> ").append(msg).append(SPAN_CLOSE);
		else if (level == Level.WARNING)
			sb.append(SPAN_OPEN).append("orange;'><b>Warning:</b> ").append(msg).append(SPAN_CLOSE);
		else if (level == Level.FINE || level == Level.FINER || level == Level.FINEST)
			sb.append(SPAN_OPEN).append("blue;'><b>DEBUG:</b> ").append(msg).append(SPAN_CLOSE);
		else
			sb.append(msg);
		Element ele = log.getElement();
		int scroll = ele.getScrollHeight();
		int top = ele.getScrollTop();
		if (log.buffer.getCapacity() == 0) {
			// unlimited log messages
			if (level != Level.CONFIG)
				sb.append("<br/>");
			log.setHTML(log.getHTML() + sb.toString());
		} else {
			// abuse of Level.CONFIG for progress (GWT does not support custom levels)
			if (level != Level.CONFIG)
				log.add(sb.toString());
			else
				log.replace(sb.toString());
			if (scroll - top - ele.getClientHeight() < 1)
				ele.setScrollTop(scroll);
			log.show();
		}
	}

	@Override
	public void update(boolean force) {
		// no regular updates required
		// see log.show() to update display
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * The console ignores most keyboard shortcuts but redefines the following:
	 * <dl>
	 * <dt>{@code Backspace, Delete}</dt>
	 * <dd>Clear the log.</dd>
	 * </dl>
	 */
	@Override
	public boolean onKeyUp(String key) {
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
		return super.onKeyUp(key);
	}

	/**
	 * Set the capacity of the log buffer. If the buffer capacity is set to
	 * {@code 0} the number of messsages is unlimited.
	 * 
	 * @param capacity the capacity of the log buffer
	 */
	public void setLogCapacity(int capacity) {
		log.buffer.setCapacity(capacity);
		String label = (capacity / 1000) + "k";
		if (label.equals("0k"))
			label = "unlimited";
		for (Widget item : bufferSizeMenu) {
			ContextMenuCheckBoxItem menuItem = (ContextMenuCheckBoxItem) item;
			menuItem.setChecked(menuItem.getText().equals(label));
		}
	}

	/**
	 * The context menu to set the buffer size for graphs with historical data.
	 */
	private ContextMenu bufferSizeMenu;

	/**
	 * The context menu item to clear the console.
	 */
	private ContextMenuItem clearMenu;

	@Override
	public void populateContextMenuAt(ContextMenu menu, int x, int y) {
		// add menu to clear canvas
		if (clearMenu == null)
			clearMenu = new ContextMenuItem("Clear", this::clearLog);
		menu.add(clearMenu);
		if (bufferSizeMenu == null) {
			bufferSizeMenu = new ContextMenu(menu);
			bufferSizeMenu.add(new ContextMenuCheckBoxItem("1k", //
					() -> {
						setLogCapacity(1000);
						log.show();
					}));
			bufferSizeMenu.add(new ContextMenuCheckBoxItem("10k",
					() -> {
						setLogCapacity(10000);
						log.show();
					}));
			bufferSizeMenu.add(new ContextMenuCheckBoxItem("unlimited", //
					() -> setLogCapacity(0)));
			setLogCapacity(log.buffer.getCapacity());
		}
		menu.add("Buffer size...", bufferSizeMenu);
		populateContextMenu(menu);
	}
}
