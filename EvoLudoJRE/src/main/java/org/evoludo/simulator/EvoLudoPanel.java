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

package org.evoludo.simulator;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.BevelBorder;
import javax.swing.border.EmptyBorder;

import org.evoludo.simulator.views.MultiView;

public class EvoLudoPanel extends JPanel implements ItemListener {

	private static final long serialVersionUID = 20110423L;

	ArrayList<MultiView> views;
	MultiView activeView;
	int count;
	int active = -1;
	JComboBox<String> viewChoice;
	CardLayout dealer;
	JPanel cards;

	public EvoLudoPanel() {
		dealer = new CardLayout();
		// cards = new JPanel(dealer);
		// views = new ArrayList<MultiViewPanel>();
		// viewChoice = new JComboBox<String>();
		// viewChoice.setToolTipText("Select different views of simulation data");
		// count = 0;
		setOpaque(false);
		// cards.setOpaque(false);
		showBorder(true);
		setLayout(new BorderLayout());
		clear();
	}

	public void clear() {
		// dealer = new CardLayout();
		if (cards == null) {
			cards = new JPanel(dealer);
			cards.setOpaque(false);
			add(cards, BorderLayout.CENTER);
			// cards.add(new JLabel(new
			// ImageIcon(MultiView.class.getResource("/org/evoludo/simulator/resources/EvoLudo.png"))),
			// "EvoLudo"); // splash card
			cards.add(new JLabel(new ImageIcon(EvoLudoPanel.class.getResource("/images/EvoLudo.png"))), "EvoLudo"); // splash
																													// card
		} else
			cards.removeAll();
		if (views == null)
			views = new ArrayList<>();
		else
			views.clear();
		if (viewChoice == null) {
			viewChoice = new JComboBox<>();
			viewChoice.setToolTipText("Select different views of simulation data");
		} else
			viewChoice.removeAllItems();
		count = 0;
		active = -1;
		if (activeView != null)
			activeView.deactivate();
		activeView = null;
		viewChoice.setMaximumRowCount(0);
		viewChoice.setSelectedIndex(-1);
		// setOpaque(false);
		// showBorder(true);
		// setLayout(new BorderLayout());
		dealer.show(cards, "EvoLudo");
	}

	public JComboBox<String> getChoice() {
		return viewChoice;
	}

	public void showSplash() {
		dealer.show(cards, "EvoLudo");
		repaint();
	}

	public void addView(MultiView view) {
		addView(view, -1);
	}

	public void addView(MultiView view, int pos) {
		viewChoice.removeItemListener(this);
		String name = view.getName();
		if (pos < 0) {
			views.add(view);
			viewChoice.addItem(name);
		} else {
			views.add(pos, view);
			viewChoice.insertItemAt(name, pos);
		}
		// no one cares about the sequence of cards on the JPanel
		cards.add((JComponent) view, name);
		viewChoice.setMaximumRowCount(++count);
		viewChoice.setSelectedIndex(0);
		viewChoice.addItemListener(this);
	}

	public void removeView(MultiView view) {
		viewChoice.removeItemListener(this);
		cards.remove((JComponent) view);
		viewChoice.removeItem(view.getName());
		views.remove(view);
		viewChoice.setMaximumRowCount(--count);
		viewChoice.addItemListener(this);
	}

	/**
	 * Set the active view from a command line style specification. {@code spec}
	 * may be a (1-based) index or a view title; since titles may themselves
	 * contain spaces (e.g. {@code "Traits - Mean"}), {@code spec} is matched
	 * against the longest view title that is a prefix of it (followed by a space
	 * or nothing) rather than splitting on the first space. Trailing text beyond
	 * a matched title is currently ignored (JRE views do not support inline
	 * options, unlike their GWT counterparts).
	 *
	 * @param spec the view index or title
	 */
	public void setView(String spec) {
		int bestLen = -1;
		int bestIdx = -1;
		for (int i = 0; i < views.size(); i++) {
			String vname = views.get(i).getName();
			if (vname == null || vname.length() <= bestLen)
				continue;
			if (spec.equals(vname) || spec.startsWith(vname + " ")) {
				bestLen = vname.length();
				bestIdx = i;
			}
		}
		if (bestIdx < 0) {
			// fall back to a 1-based index, optionally followed by trailing text
			String head = spec.split(" ", 2)[0];
			int idx;
			try {
				idx = Integer.parseInt(head);
			} catch (NumberFormatException e) {
				idx = 1;
			}
			bestIdx = Math.clamp(idx, 1, Math.max(views.size(), 1)) - 1;
		}
		setView(bestIdx);
	}

	public void changeView(int increment) {
		setView((count + active + increment) % count);
	}

	public String getView() {
		return (String) viewChoice.getSelectedItem();
	}

	public void setView(int idx) {
		if (activeView != null) {
			if (idx == active)
				return;
			activeView.deactivate();
		}
		active = Math.clamp(idx, 0, views.size() - 1);
		activeView = views.get(active);
		activeView.activate();
		viewChoice.setSelectedIndex(active);
		dealer.show(cards, activeView.getName());
	}

	public List<MultiView> getViews() {
		return views;
	}

	private void showBorder(boolean show) {
		if (show)
			setBorder(BorderFactory.createCompoundBorder(new EmptyBorder(6, 6, 0, 6),
					new BevelBorder(BevelBorder.LOWERED)));
		else
			setBorder(null);
	}

	public void parametersChanged(boolean didReset) {
		for (int n = 0; n < count; n++)
			views.get(n).parametersChanged(didReset);
	}

	// this is an annoying workaround to close the context menu when the java
	// applet/application becomes deactivated
	public void setContextMenuEnabled(boolean enabled) {
		// note: for some reason activeView may not be set...
		if (activeView != null)
			activeView.setContextMenuEnabled(enabled);
	}

	@Override
	public void itemStateChanged(ItemEvent e) {
		if (e.getStateChange() == ItemEvent.DESELECTED)
			return;

		@SuppressWarnings("unchecked")
		JComboBox<String> jcb = (JComboBox<String>) e.getSource();
		int idx = jcb.getSelectedIndex();
		setView(idx);
	}

	public MultiView getActiveView() {
		return activeView;
	}

	public int getActiveIndex() {
		return active;
	}
}
