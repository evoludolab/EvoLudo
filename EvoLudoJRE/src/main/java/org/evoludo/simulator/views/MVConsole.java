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

package org.evoludo.simulator.views;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyleContext;

import org.evoludo.simulator.EvoLudoLab;

public class MVConsole extends JComponent implements MultiView {

	private static final long serialVersionUID = 20110423L;

	EvoLudoLab lab;
	Logger logger;
	private final JTextPane text;
	private final JPopupMenu menu = new JPopupMenu();
	private static final Color transparent = new Color(0, 0, 0, 0);
	private static final AttributeSet message, debug, warning, error;
	static {
		StyleContext sc = StyleContext.getDefaultStyleContext();
		message = sc.addAttribute(SimpleAttributeSet.EMPTY, StyleConstants.Foreground, Color.black);
		sc = StyleContext.getDefaultStyleContext();
		debug = sc.addAttribute(SimpleAttributeSet.EMPTY, StyleConstants.Foreground, Color.green);
		sc = StyleContext.getDefaultStyleContext();
		warning = sc.addAttribute(SimpleAttributeSet.EMPTY, StyleConstants.Foreground, Color.blue);
		sc = StyleContext.getDefaultStyleContext();
		error = sc.addAttribute(SimpleAttributeSet.EMPTY, StyleConstants.Foreground, Color.red);
	}

	protected static final String MENU_CLEAR = "clear";

	// note: specified in GraphStyle as the default - how do we access this
	// information here?
	private static final Font menuFont = new Font("Default", Font.PLAIN, 11);

	public MVConsole(EvoLudoLab lab) {
		this.lab = lab;
		logger = lab.getEngine().getLogger();
		setLayout(new BorderLayout());
		setOpaque(false);
		text = new JTextPane();
		text.setMargin(new Insets(4, 6, 4, 6));
		text.setFont(menuFont);
		JScrollPane scroll = new JScrollPane(text, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
				JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		scroll.setBorder(
				BorderFactory.createCompoundBorder(
						BorderFactory.createMatteBorder(6, 8, 6, 8, transparent),
						BorderFactory.createMatteBorder(1, 1, 1, 1, Color.gray)));
		scroll.setBackground(transparent);
		ActionHandler handler = new ActionHandler();
		JMenuItem menuItem = new JMenuItem("Clear");
		menuItem.setActionCommand(MENU_CLEAR);
		menuItem.addActionListener(handler);
		menuItem.setFont(menuFont);
		menu.add(menuItem);
		text.setComponentPopupMenu(menu);
		add(scroll, BorderLayout.CENTER);
		clear();
		setToolTipText(null); // no tooltips - individual components may have their own.
	}

	public class ActionHandler implements ActionListener {

		@Override
		public void actionPerformed(ActionEvent e) {
			String cmd = e.getActionCommand();
			if (cmd.equals(MENU_CLEAR)) {
				clear();
				return;
			}
		}
	}

	protected void clear() {
		text.setEditable(true);
		text.selectAll();
		text.replaceSelection("");
		text.setEditable(false);
	}

	public void log(Level level, String msg) {
		String pretty = msg;
		AttributeSet attr = message;
		if (level == Level.WARNING) {
			pretty = "WARNING: " + msg;
			attr = warning;
		} else if (level == Level.SEVERE) {
			pretty = "ERROR: " + msg;
			attr = error;
		} else if (level == Level.FINE || level == Level.FINER || level == Level.FINEST) {
			pretty = "DEBUG: " + msg;
			attr = debug;
		}
		text.setEditable(true);
		// abuse of Level.CONFIG for progress reports (GWT does not support custom
		// levels)
		if (!level.equals(Level.CONFIG)) {
			pretty += "\n";
		} else {
			// replace last line
			int len = text.getDocument().getLength();
			try {
				String content = text.getDocument().getText(0, len);
				int idx = Math.max(0, content.lastIndexOf('\n'));
				text.getDocument().remove(idx, len - idx);
			} catch (BadLocationException e) {
				// shouldn't happen...
			}
		}
		text.setCaretPosition(text.getDocument().getLength());
		text.setCharacterAttributes(attr, false);
		text.replaceSelection(pretty);
		text.setEditable(false);
	}

	// implement MultiViewPanel
	@Override
	public String getName() {
		return "Console Log";
	}

	private boolean isActive = false;

	@Override
	public void activate() {
		isActive = true;
	}

	@Override
	public void deactivate() {
		isActive = false;
	}

	@Override
	public boolean isActive() {
		return isActive;
	}

	// on Max OS X the java focus system is strange/buggy...
	// at least close menu if applet/application loses focus - this is non-standard
	// behavior
	@Override
	public void setContextMenuEnabled(boolean enabled) {
		if (!enabled && menu.isVisible())
			menu.setVisible(false);
	}

	/*
	 * implement MultiViewPanel - and ignore.
	 */
	@Override
	public void parametersChanged(boolean didReset) {
	}

	@Override
	public void reset(boolean clear) {
	}

	@Override
	public void init() {
	}

	@Override
	public void update(boolean updateGUI) {
	}

	@Override
	public void end() {
	}
}
