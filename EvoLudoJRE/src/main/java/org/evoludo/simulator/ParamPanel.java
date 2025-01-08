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

package org.evoludo.simulator;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;


public class ParamPanel implements ActionListener {

	EvoLudoLab controller;
	ParamPanelWindow panel;
    JTextArea inputCLO;

	private static final String CLO_CARD = "CLO";

	public ParamPanel(EvoLudoLab lab) {
		controller = lab;
	}

	public void setVisible(boolean visible) {
		if (panel == null) {
			if (!visible) return;
			panel = new ParamPanelWindow(this);

			// command line options parameters
			JPanel cloCard = new JPanel();
			cloCard.setLayout(new BoxLayout(cloCard, BoxLayout.Y_AXIS));
			JLabel label = new JLabel("command line options:");
			label.setAlignmentX(JComponent.LEFT_ALIGNMENT);
			cloCard.add(label);
			inputCLO = new JTextArea(12, 40);
			inputCLO.setLineWrap(true);
			inputCLO.setWrapStyleWord(true);
			inputCLO.setAlignmentX(JComponent.LEFT_ALIGNMENT);
			cloCard.add(inputCLO);
			panel.addTab(CLO_CARD, cloCard, "Command line options");
			panel.pack();
			panel.setResizable(false);
		}
		inputCLO.setText(controller.getCLO());
		panel.setVisible(true);
	}

    public boolean isVisible() {
		if( panel==null ) return false;
		return panel.isVisible();
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		/* labels must be at most 8 chars long */
		String cmd = "Cancel  OK      Revert  Apply   ";
		String label = e.getActionCommand();
		switch( cmd.indexOf(label) ) {
			case 0:		// Cancel
				panel.setVisible(false);
				break;
			case 16:	// Revert
				inputCLO.setText(controller.getCLO());
				break;
			case 8:		// OK
				panel.setVisible(false);
				//$FALL-THROUGH$
			default:
			case 24:	// Apply
				controller.getEngine().setCLO(inputCLO.getText());
				controller.applyCLO();
			break;
		}
	}
}

class ParamPanelWindow extends JFrame {

	private static final long serialVersionUID = 20110423L;
	ParamPanel controller;
	JTabbedPane tabs;

	public ParamPanelWindow(ParamPanel controller) {
		JPanel panel;
		JButton button;

		this.controller = controller;
		setTitle("Parameters");
		setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
		setLayout(new BorderLayout());
		tabs = new JTabbedPane();
		add(tabs, BorderLayout.CENTER); // panel showing current card

		// create ok/cancel etc buttons
		panel = new JPanel(new GridLayout(1, 4));
		button = new JButton("Cancel");
		button.addActionListener(controller);
		button.setToolTipText("Discard changes, close panel.");
		panel.add(button);
		button = new JButton("Revert");
		button.addActionListener(controller);
		button.setToolTipText("Discard changes, revert parameter settings.");
		panel.add(button);
		button = new JButton("Apply");
		button.addActionListener(controller);
		button.setToolTipText("Apply parameter changes.");
		panel.add(button);
		button = new JButton("OK");
		button.addActionListener(controller);
		button.setToolTipText("Apply changes, close panel.");
		panel.add(button);
		add(panel, BorderLayout.SOUTH);	// panel containing ok/cancel etc buttons
	}

	public void addTab(String name, JPanel tab, String descr) {
		tabs.addTab(name, null, tab, descr);
	}
}
