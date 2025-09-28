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
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.MouseEvent;

import org.evoludo.geom.Point2D;
import org.evoludo.simulator.modules.Module;
import org.evoludo.simulator.views.HasPhase2D.Data2Phase;

public class ParaGraph extends AbstractGraph {

	private static final long serialVersionUID = 20110423L;

	public int nStates = -1;
	private final StateData data = new StateData();
	private Color[] colors;
	private String[] names;
	protected Data2Phase map;

	public ParaGraph(StateGraphListener controller, Module module) {
		super(controller, module);
		hasHistory = true;
		frame = new FrameLayer(style);
		add(frame, LAYER_FRAME);
		glass = new GlassLayer(frame, this);
		add(glass, LAYER_GLASS);
	}

	public void setMap(Data2Phase map) {
		if (map == null || map.equals(this.map))
			return;
		this.map = map;
	}

	public Data2Phase getMap() {
		return map;
	}

	// 100531 this is adapted from S3Graph - check if needed!
	@Override
	public void reinit() {
		int id = module.getID();
		colors = controller.getColors(id);
		frame.setLabels(names, colors, id);
		// note: this is a bit overkill... but seems necessary to deal with labels...
		frame.init(getBounds());
		((StateGraphListener) controller).getData(data, id);
		data.reset();
		if (doSVG)
			svgPlot.moveTo(data.now.x * canvas.width, data.now.y * canvas.height);
		super.reinit();
	}

	@Override
	public void reset(boolean clear) {
		int id = module.getID();
		nStates = controller.getNData(id);
		data.init(nStates);
		data.isLocal = isLocalDynamics;
		names = controller.getNames(id);
		super.reset(clear);
	}

	@Override
	public void clear() {
		super.clear();
		if (doSVG)
			svgPlot.moveTo(data.now.x * canvas.width, data.now.y * canvas.height);
	}

	@Override
	protected void prepare() {
		data.next();
		((StateGraphListener) controller).getData(data, module.getID());
	}

	@Override
	public void plot(Graphics2D g2d) {
		if (data.time <= timestamp) {
			if (doSVG)
				svgPlot.moveTo(data.now.x * canvas.width, data.now.y * canvas.height);
			return; // up to date
		}
		timestamp = data.time;
		if (data.connect) {
			g2d.setStroke(style.lineStroke);
			g2d.setColor(Color.black);
			double x = data.now.x * canvas.width, y = data.now.y * canvas.height;
			g2d.drawLine((int) (data.then.x * canvas.width), (int) (data.then.y * canvas.height), (int) x, (int) y);
			if (doSVG)
				svgPlot.lineTo(x, y);
			return;
		}
		if (doSVG)
			svgPlot.moveTo(data.now.x * canvas.width, data.now.y * canvas.height);
	}

	// set initial frequency with double-click
	@Override
	protected void mouseClick(Point loc, int count) {
		if (count < 2 || !canvas.contains(loc))
			return;
		// l.setLocation((double)(loc.x-canvas.x)/(double)canvas.width,
		// (double)(loc.y-canvas.y)/(double)canvas.height);
		// ((StateGraphListener)controller).setState(l, tag);
		((StateGraphListener) controller).setState(new double[] { (double) (loc.x - canvas.x) / (double) canvas.width,
				(double) (loc.y - canvas.y) / (double) canvas.height });
		reset(false);
	}

	// tool tips
	private final Point2D l = new Point2D();

	@Override
	public String getToolTipText(MouseEvent event) {
		Point loc = event.getPoint();
		if (!canvas.contains(loc))
			return null;

		l.setLocation((double) (loc.x - canvas.x) / (double) canvas.width,
				(double) (loc.y - canvas.y) / (double) canvas.height);
		return ((StateGraphListener) controller).getToolTipText(l, module.getID());
	}

	@Override
	protected int getSnapshotFormat() {
		if (doSVG)
			return SNAPSHOT_SVG;
		return SNAPSHOT_PNG;
	}

	// IMPLEMENT GLASS LAYER LISTENER
	private final Point2D s = new Point2D();

	@Override
	public Point2D getState() {
		s.x = (int) (data.now.x * canvas.width);
		s.y = (int) (data.now.y * canvas.height);
		return s;
	}

	@Override
	public Point2D getStart() {
		s.x = (int) (data.origin.x * canvas.width);
		s.y = (int) (data.origin.y * canvas.height);
		return s;
	}
}
