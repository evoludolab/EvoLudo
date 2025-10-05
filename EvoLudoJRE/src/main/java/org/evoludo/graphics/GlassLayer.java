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

package org.evoludo.graphics;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;

import javax.swing.JComponent;

import org.evoludo.geom.Point2D;

public class GlassLayer extends JComponent {

	private static final long serialVersionUID = 20110423L;

	private final GlassLayerListener view;
	private final Rectangle canvas;
	private boolean isClear = false;

	public static final int RADIUS = 5;

	public static int margin = (RADIUS + 1) / 2;

	public GlassLayer(FrameLayer frame, GlassLayerListener view) {
		canvas = frame.canvas;
		this.view = view;
		setOpaque(false);
	}

	public void clear() {
		isClear = true;
	}

	@Override
	public void paintComponent(Graphics g) {
		if (isClear || view.hasMessage()) {
			isClear = false;
			return;
		}
		// frame.style.prepare((Graphics2D)g);
		Graphics2D g2 = (Graphics2D) g;
		if (ToggleAntiAliasingAction.sharedInstance().getAntiAliasing())
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		else
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_DEFAULT);
		g.setColor(Color.green);
		int xshift = canvas.x - margin + 1;
		int yshift = canvas.y - margin + 1;
		Point2D s = view.getStart();
		g.fillOval(xshift + (int) s.getX(), yshift + (int) s.getY(), RADIUS, RADIUS);
		g.setColor(Color.red);
		Point2D p = view.getState();
		g.fillOval(xshift + (int) p.getX(), yshift + (int) p.getY(), RADIUS, RADIUS);
	}

	@Override
	protected void printComponent(Graphics g) {
		// do not print anything
	}
}
