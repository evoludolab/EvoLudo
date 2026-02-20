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

package org.evoludo.graphics;

import java.util.Arrays;
import java.util.Iterator;

import org.evoludo.simulator.Network.Status;
import org.evoludo.simulator.geometries.AbstractGeometry;
import org.evoludo.simulator.geometries.GeometryType;
import org.evoludo.simulator.modules.Module;
import org.evoludo.simulator.views.AbstractView;
import org.evoludo.util.RingBuffer;

/**
 * Specialized population graph for linear geometries with history.
 * <p>
 * Content is shifted/zoomed, while the frame and axis decorations remain fixed.
 */
public class PopGraph1D extends PopGraph2D {

	/**
	 * The maximum number of nodes supported in linear geometry.
	 */
	public static final int MAX_LINEAR_SIZE = 500;

	/**
	 * Create a graph for linear population rendering.
	 *
	 * @param view   the owning view
	 * @param module the module backing the graph
	 */
	public PopGraph1D(AbstractView<?> view, Module<?> module) {
		super(view, module);
		buffer = new RingBuffer<String[]>(2 * MAX_LINEAR_SIZE);
	}

	@Override
	protected void ensureData() {
		if (geometry == null) {
			data = null;
			return;
		}
		int size = geometry.getSize();
		if (data == null || data.length != size)
			data = new String[size];
	}

	@Override
	public void update(boolean isNext) {
		String[] copy = Arrays.copyOf(data, data.length);
		if (isNext || buffer.isEmpty())
			buffer.append(copy);
		else
			buffer.replace(copy);
		super.update(isNext);
	}

	@Override
	protected void drawLattice() {
		GeometryType type = geometry.getType();
		if (type != GeometryType.LINEAR) {
			super.drawLattice();
			return;
		}
		if (!prepContentCanvas())
			return;
		invalidated = false;
		drawLinearContent();
		g.restore();
		drawFrameOverlay();
	}

	/**
	 * Prepare canvas so only content participates in pan/zoom transforms.
	 *
	 * @return {@code true} if canvas is ready for drawing
	 */
	private boolean prepContentCanvas() {
		if (hasMessage)
			return false;
		g.save();
		g.scale(scale, scale);
		clearCanvas();
		g.beginPath();
		g.rect(bounds.getX(), bounds.getY(), bounds.getWidth(), bounds.getHeight());
		g.clip();
		double vx = (viewCorner == null ? 0.0 : viewCorner.getX());
		double vy = (viewCorner == null ? 0.0 : viewCorner.getY());
		g.translate(bounds.getX() - vx, bounds.getY() - vy);
		g.scale(zoomFactor, zoomFactor);
		return true;
	}

	/**
	 * Draw linear lattice content without frame.
	 */
	private void drawLinearContent() {
		if (geometry == null)
			return;
		int nSteps = (int) (bounds.getHeight() / dh);
		int yshift = 0;
		Iterator<String[]> it = buffer.iterator();
		while (it.hasNext() && nSteps-- > 0) {
			int xshift = 0;
			String[] state = it.next();
			for (int n = 0; n < geometry.getSize() && n < state.length; n++) {
				g.setFillStyle(state[n]);
				fillRect(xshift, yshift, dw, dh);
				xshift += dw;
			}
			yshift += dh;
		}
	}

	/**
	 * Draw frame and axis decorations without pan/zoom transforms.
	 */
	private void drawFrameOverlay() {
		double baseXMin = style.xMin;
		double baseXMax = style.xMax;
		double baseYMin = style.yMin;
		double baseYMax = style.yMax;
		applyViewportRanges(baseXMin, baseXMax, baseYMin, baseYMax);
		g.save();
		g.scale(scale, scale);
		g.translate(bounds.getX(), bounds.getY());
		drawFrame(4, 4);
		g.restore();
		style.xMin = baseXMin;
		style.xMax = baseXMax;
		style.yMin = baseYMin;
		style.yMax = baseYMax;
	}

	/**
	 * Map current pan/zoom state to visible axis ranges for frame labels/ticks.
	 *
	 * @param baseXMin baseline x minimum
	 * @param baseXMax baseline x maximum
	 * @param baseYMin baseline y minimum
	 * @param baseYMax baseline y maximum
	 */
	private void applyViewportRanges(double baseXMin, double baseXMax, double baseYMin, double baseYMax) {
		double w = bounds.getWidth();
		double h = bounds.getHeight();
		double z = Math.max(1e-12, zoomFactor);
		if (w <= 0.0 || h <= 0.0)
			return;
		double xRange = baseXMax - baseXMin;
		double yRange = baseYMax - baseYMin;
		if (xRange <= 0.0 || yRange <= 0.0)
			return;
		double vx = (viewCorner == null ? 0.0 : viewCorner.getX());
		double vy = (viewCorner == null ? 0.0 : viewCorner.getY());
		double fx0 = Math.max(0.0, Math.min(1.0, vx / (z * w)));
		double fx1 = Math.max(0.0, Math.min(1.0, (vx + w) / (z * w)));
		double fy0 = Math.max(0.0, Math.min(1.0, vy / (z * h)));
		double fy1 = Math.max(0.0, Math.min(1.0, (vy + h) / (z * h)));
		style.xMin = baseXMin + fx0 * xRange;
		style.xMax = baseXMin + fx1 * xRange;
		style.yMax = baseYMax - fy0 * yRange;
		style.yMin = baseYMax - fy1 * yRange;
	}

	@Override
	public void calcBounds(int width, int height) {
		super.calcBounds(width, height);
		clearMessage();
		noGraph = false;
		dw = 0;
		dh = 0;
		dR = 0.0;

		int bWidth = (int) bounds.getWidth();
		dw = bWidth / geometry.getSize();
		int bHeight = (int) bounds.getHeight();
		dh = dw;
		int steps = (dh == 0) ? 0 : bHeight / dh;
		if (dw < MIN_DW || steps == 0) {
			bounds.setSize(width, height);
			noGraph = true;
			displayMessage("Population size to large!");
			return;
		}
		int adjw = dw * geometry.getSize();
		int adjh = bHeight - (bHeight % dh);
		int dx = (bWidth - adjw) / 2;
		int dy = (bHeight - adjh) / 2;
		bounds.set(bounds.getX() + dx, bounds.getY() + dy, adjw, adjh);
		int capacity = 2 * steps;
		buffer.setCapacity(capacity);
		style.setYRange(steps - 1);
		style.showFrame = true;
	}

	@Override
	public int findNodeAt(int x, int y) {
		// no network may have been initialized (e.g. for ODE/SDE models)
		if (hasMessage || network == null || invalidated || network.isStatus(Status.LAYOUT_IN_PROGRESS))
			return FINDNODEAT_OUT_OF_BOUNDS;

		x = x - (int) (style.frameWidth * zoomFactor + 0.5);
		y = y - (int) (style.frameWidth * zoomFactor - 0.5);
		if (!bounds.contains(x, y))
			return FINDNODEAT_OUT_OF_BOUNDS;

		int sx = (int) ((viewCorner.getX() + x - bounds.getX()) / zoomFactor + 0.5);
		int sy = (int) ((viewCorner.getY() + y - bounds.getY()) / zoomFactor + 0.5);
		int c = sx / dw;
		int r = sy / dh;
		if (c < 0 || c >= geometry.getSize() || r < 0)
			return FINDNODEAT_OUT_OF_BOUNDS;
		return r * geometry.getSize() + c;
	}
}
