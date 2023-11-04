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

package org.evoludo.graphics;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Iterator;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JCheckBoxMenuItem;

public class ToggleAntiAliasingAction extends AbstractAction {

	private static final long serialVersionUID = 20110423L;
	private static ToggleAntiAliasingAction taaa;
	private static boolean doAntiAliasing = false;
	private static final java.util.List<AbstractGraph> aaListeners =	new ArrayList<AbstractGraph>();

	// ensure non-instantiability of ToggleAntiAliasingAction
	private ToggleAntiAliasingAction() {
		super("Antialiasing");
		putValue(Action.SHORT_DESCRIPTION, "Toggle Antialiasing");
		putValue(Action.MNEMONIC_KEY, KeyEvent.VK_A);
		putValue(Action.SELECTED_KEY, false);
	}

	public static ToggleAntiAliasingAction sharedInstance() {
		if( taaa==null ) taaa = new ToggleAntiAliasingAction();
		return taaa;
	}

	public boolean getAntiAliasing() {
		return doAntiAliasing;
	}

	public void setAntiAliasing(boolean doAntiAliasing) {
		putValue(Action.SELECTED_KEY, doAntiAliasing);
		ToggleAntiAliasingAction.doAntiAliasing = doAntiAliasing;
	}

	public void addAntiAliasingListener(AbstractGraph listener) {
		aaListeners.add(listener);
	}

	public void removeAntiAliasingListener(AbstractGraph listener) {
		aaListeners.remove(listener);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if( !(e.getSource() instanceof JCheckBoxMenuItem) ) {
			// event not triggered by menu (e.g. keypress) - toggle state
			setAntiAliasing(!doAntiAliasing);
		}
		else
			doAntiAliasing = (Boolean)getValue(Action.SELECTED_KEY);
		for( Iterator<AbstractGraph> i = aaListeners.iterator(); i.hasNext(); ) {
// note: this is obscure - it should notify listeneres that antialiasing changed!
			i.next().clear();
		}
	}
}
