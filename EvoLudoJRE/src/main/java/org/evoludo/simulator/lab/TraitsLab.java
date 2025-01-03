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

package org.evoludo.simulator.lab;

import org.evoludo.simulator.EvoLudoJRE;
import org.evoludo.simulator.EvoLudoLab;

public class TraitsLab extends EvoLudoLab {

	private static final long serialVersionUID = 20110423L;

	public TraitsLab() {
		super(new EvoLudoJRE());
	}

//	JTextField	inputNTraits;
//
//	@Override
//	public void addGameParams(JPanel myGame, 
//			GridBagLayout gridbag, 
//			GridBagConstraints constraints) {
//		super.addGameParams(myGame, gridbag, constraints);
//
//		JLabel label = new JLabel("Number of traits:");
//		gridbag.setConstraints(label, constraints);
//		myGame.add(label);
//		inputNTraits = new JTextField("-", 6);
//		gridbag.setConstraints(inputNTraits, constraints);
//		myGame.add(inputNTraits);
//		constraints.gridy++;
//	}
//
//	@Override
//	public void revertParams() {
//		inputNTraits.setText(ChHFormatter.format(((Traits)population).getTraitCount(), 6));
//		super.revertParams();
//	}
//
//	@Override
//	public void applyParams() {
//		((Traits)population).setTraitCount(CLOParser.parseInteger(inputNTraits.getText()));
//		super.applyParams();
//	}
}
