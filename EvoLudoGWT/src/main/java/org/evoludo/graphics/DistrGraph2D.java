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

import org.evoludo.ui.ContextMenu;
import org.evoludo.simulator.modules.Module;
import org.evoludo.simulator.views.Distribution;

/**
 * Specialized graph for 2D lattice-based trait distributions.
 * <p>
 * Content is shifted/zoomed, while the frame and axis decorations remain fixed.
 */
@SuppressWarnings("java:S110")
public class DistrGraph2D extends PopGraph2D {

	/**
	 * Create a graph for 2D distribution rendering.
	 *
	 * @param view   the owning distribution view
	 * @param module the module backing the graph
	 */
	public DistrGraph2D(Distribution view, Module<?> module) {
		super(view, module);
	}

	@Override
	protected void populateGraphContextMenu(ContextMenu menu, int x, int y) {
		view.addAxesMenu(menu, this);
	}

	@Override
	protected void drawLattice() {
		if (!prepCanvas(true))
			return;
		invalidated = false;
		drawSquareContent();
		g.restore();
		drawFrameForView(4, 4);
		legend.draw();
	}

	@Override
	public String getTooltipAt(int x, int y) {
		String tip = super.getTooltipAt(x, y);
		element.removeClassName(EVOLUDO_CURSOR_NODE);
		return tip;
	}

	/**
	 * Draw 2D distribution content as a plain square grid, without frame.
	 */
	private void drawSquareContent() {
		if (data == null)
			return;
		int yshift = (int) bounds.getHeight() - dh;
		int row = 0;
		for (int h = 0; h < side; h++) {
			int xshift = 0;
			for (int w = 0; w < side; w++) {
				g.setFillStyle(data[row + w]);
				fillRect(xshift, yshift, dw, dh);
				xshift += dw;
			}
			yshift -= dh;
			row += side;
		}
	}
}
