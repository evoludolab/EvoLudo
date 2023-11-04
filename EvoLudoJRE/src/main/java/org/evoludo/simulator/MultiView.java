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

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.BevelBorder;
import javax.swing.border.EmptyBorder;

public class MultiView extends JPanel implements ItemListener {

	private static final long serialVersionUID = 20110423L;

	ArrayList<MultiViewPanel>	views;
	MultiViewPanel	activeView;
	int			count, active = -1;
	JComboBox<String>	viewChoice;
	CardLayout dealer;
	JPanel cards;

	public MultiView() {
		dealer = new CardLayout();
//		cards = new JPanel(dealer);
//		views = new ArrayList<MultiViewPanel>();
//		viewChoice = new JComboBox<String>();
//		viewChoice.setToolTipText("Select different views of simulation data");
//		count = 0;
		setOpaque(false);
//		cards.setOpaque(false);
		showBorder(true);
		setLayout(new BorderLayout());
		clear();
	}

	public void clear() {
//		dealer = new CardLayout();
		if( cards==null ) {
			cards = new JPanel(dealer);
			cards.setOpaque(false);
			add(cards, BorderLayout.CENTER);
			cards.add(new JLabel(new ImageIcon(MultiView.class.getResource("/org/evoludo/simulator/resources/EvoLudo.png"))), "EvoLudo");	// splash card
		}
		else
			cards.removeAll();
		if( views==null ) views = new ArrayList<MultiViewPanel>();
		else views.clear();
		if( viewChoice==null ) {
			viewChoice = new JComboBox<String>();
			viewChoice.setToolTipText("Select different views of simulation data");
		}
		else viewChoice.removeAllItems();
		count = 0;
		active = -1;
if( activeView!=null ) activeView.deactivate();
activeView = null;
		viewChoice.setMaximumRowCount(0);
		viewChoice.setSelectedIndex(-1);
//		setOpaque(false);
//		showBorder(true);
//		setLayout(new BorderLayout());
		dealer.show(cards, "EvoLudo");
	}

	public JComboBox<String> getChoice() {
		return viewChoice;
	}

	public void showSplash() {
		dealer.show(cards, "EvoLudo");
		repaint();
	}

	public void addView(MultiViewPanel view) {
		addView(view, -1);
	}
	
	public void addView(MultiViewPanel view, int pos) {
viewChoice.removeItemListener(this);
		String name = view.getName();
		if( pos<0 ) {
			views.add(view);
			viewChoice.addItem(name);
		}
		else {
			views.add(pos, view);
			viewChoice.insertItemAt(name, pos);
		}
		// no one cares about the sequence of cards on the JPanel 
		cards.add((JComponent)view, name);
		viewChoice.setMaximumRowCount(++count);
		viewChoice.setSelectedIndex(0);
viewChoice.addItemListener(this);
	}
	
	public void removeView(MultiViewPanel view) {
viewChoice.removeItemListener(this);
		cards.remove((JComponent)view);
		viewChoice.removeItem(view.getName());
		views.remove(view);
		viewChoice.setMaximumRowCount(--count);
viewChoice.addItemListener(this);
	}
	
	public void setView(String name) {
		// if length of string is 1 or 2, assume an index is given
		if( name.length()<3 )
			viewChoice.setSelectedIndex(Math.max(0, Math.min(viewChoice.getItemCount()-1, Integer.parseInt(name))));
		else
			viewChoice.setSelectedItem(name.replace('_', ' '));
//active = -1;	// force update - this seems necessary because the listener is added early on viewChoice and gets triggered during initialization.
		setView(viewChoice.getSelectedIndex());
	}

	public void changeView(int increment) {
		setView((count+active+increment)%count);
	}

	public String getView() {
		return (String)viewChoice.getSelectedItem();
	}

	public void setView(int idx) {
		if( activeView!=null ) {
			if( idx==active )
				return;
			activeView.deactivate();
		}
		active = Math.min(Math.max(idx, 0), views.size()-1);
		activeView = views.get(active);
		activeView.activate();
		viewChoice.setSelectedIndex(active);
		dealer.show(cards, activeView.getName());
	}
	
	public ArrayList<MultiViewPanel> getViews() {
		return views;
	}

	private void showBorder(boolean show) {
		if( show ) setBorder(BorderFactory.createCompoundBorder(new EmptyBorder(6, 6, 0, 6), new BevelBorder(BevelBorder.LOWERED)));
		else setBorder(null);
	}

	public void parametersChanged(boolean didReset) {
		for( int n=0; n<count; n++ ) views.get(n).parametersChanged(didReset);
	}

	// this is an annoying workaround to close the context menu when the java applet/application becomes deactivated
	public void setContextMenuEnabled(boolean enabled) {
// note: for some reason activeView may not be set...
		if( activeView!=null )
		activeView.setContextMenuEnabled(enabled);
	}

	@Override
	public void itemStateChanged(ItemEvent e) {
		if( e.getStateChange()==ItemEvent.DESELECTED ) return;

		@SuppressWarnings("unchecked")
		JComboBox<String> jcb = (JComboBox<String>)e.getSource();
		int idx = jcb.getSelectedIndex();
		setView(idx);
	}

	public MultiViewPanel getActiveView() {
		return activeView;
	}

	public int getActiveIndex() {
		return active;
	}
}
