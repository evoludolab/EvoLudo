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

import java.util.Iterator;

import org.evoludo.simulator.geometries.GeometryType;
import org.evoludo.simulator.modules.Module;
import org.evoludo.simulator.views.Distribution;

/**
 * Specialized population graph for 1D trait distributions with history.
 * <p>
 * Content is shifted/zoomed, while the frame and axis decorations remain fixed.
 */
public class PopGraph1D extends PopGraph2D {

	/**
	 * Create a graph for 1D distribution rendering.
	 *
	 * @param view   the owning distribution view
	 * @param module the module backing the graph
	 */
	public PopGraph1D(Distribution view, Module<?> module) {
		super(view, module);
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
		if (buffer == null || geometry == null)
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
}
