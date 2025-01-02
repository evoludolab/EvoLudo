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

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.util.logging.Logger;

import javax.swing.JPopupMenu;

import org.evoludo.simulator.EvoLudo;

public interface GraphListener {

	public void polish(Graphics2D plot, AbstractGraph graph);

	public void initStyle(GraphStyle style, AbstractGraph owner);

//	public void		setLocalDynamics(boolean isLocalDynamics);

	public void		setLocalNode(int localNode);

	public int		getLocalNode();

	public void		setTimeReversed(boolean reversed);

	public Color	getColor(int tag);

	public Color[]	getColors(int tag);

	public String	getName(int tag);

	public String[] getNames(int tag);

//	public String[] getActiveNames(int tag);

	public boolean[] getActives(int tag);

	public int getActiveCount(int tag);
	
	public int		getNData(int tag);

	public boolean	getData(double[] data, int tag);

	public double[]	getData(int tag);

	public boolean	isRunning();

	public boolean	hasSVG();

	public void		saveSnapshot(int tag, boolean inViewport, int format);

	public void		exportState();

// menu stuff - experimental
//	public void		initCustomMenu(JPopupMenu menu, int tag);
//	public void		resetCustomMenu(JPopupMenu menu, int tag);

	public void		initCustomMenu(JPopupMenu menu, AbstractGraph owner);

	public void		resetCustomMenu(JPopupMenu menu, AbstractGraph owner);

	public void		showCustomMenu(JPopupMenu menu, Point loc, AbstractGraph owner);

	// e.g. track changes of payoffs in fitness histograms
	public default boolean	verifyXAxis(GraphAxis x, int tag) {
		return false;
	}

	// e.g. track changes of payoffs in mean fitness plot
	public default boolean	verifyYAxis(GraphAxis y, int tag) {
		return false;
	}

	// e.g. track changes of payoffs in mean fitness plot
	public default boolean	verifyYThresholds(FrameLayer frame, int tag)  {
		return false;
	}

	// e.g. track changes of payoffs in fitness histogram plot
	public default boolean	verifyMarkedBins(HistoFrameLayer frame, int tag)  {
		return false;
	}

	public Logger getLogger();

	public EvoLudo getEngine();
}
