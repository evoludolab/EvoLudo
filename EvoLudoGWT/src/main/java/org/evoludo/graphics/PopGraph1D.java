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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.evoludo.math.ArrayMath;
import org.evoludo.simulator.ColorMap;
import org.evoludo.simulator.Network.Status;
import org.evoludo.simulator.geometries.AbstractGeometry;
import org.evoludo.simulator.geometries.GeometryType;
import org.evoludo.simulator.modules.Module;
import org.evoludo.simulator.views.AbstractView;
import org.evoludo.ui.ContextMenu;
import org.evoludo.ui.ContextMenuCheckBoxItem;
import org.evoludo.util.RingBuffer;

/**
 * Specialized population graph for linear geometries with history.
 * <p>
 * Content is shifted/zoomed, while the frame and axis decorations remain fixed.
 */
@SuppressWarnings("java:S110") // more than 5 parental classes acceptable
public class PopGraph1D extends PopGraph2D {

	/**
	 * Default buffer capacity for linear-history graphs.
	 */
	private static final int DEFAULT_BUFFER_CAPACITY = 1000;

	/**
	 * Empty decoded row sentinel.
	 */
	private static final double[] EMPTY_MASS_ROW = new double[0];

	/**
	 * Empty encoded row sentinel.
	 */
	private static final String[] EMPTY_COLOR_ROW = new String[0];

	/**
	 * Create a graph for linear population rendering.
	 *
	 * @param view   the owning view
	 * @param module the module backing the graph
	 */
	public PopGraph1D(AbstractView<?> view, Module<?> module) {
		super(view, module);
		buffer = new RingBuffer<String[]>(DEFAULT_BUFFER_CAPACITY);
		style.showYAxisRight = false;
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
	public String getTooltipAt(int x, int y) {
		String tip = super.getTooltipAt(x, y);
		element.removeClassName(EVOLUDO_CURSOR_NODE);
		return tip;
	}

	/**
	 * Decode the probability mass for a bin from a color-encoded histogram row.
	 *
	 * @param colors   color-encoded histogram row
	 * @param idx      bin index
	 * @param nEntries number of histogram entries to decode
	 * @return decoded bin mass, or {@code -1.0} if unavailable
	 */
	public double decodeBin(String[] colors, int idx, int nEntries) {
		if (idx < 0)
			return -1.0;
		ColorMap.Gradient1D<String> cMap = null;
		if (getColorMap() instanceof ColorMap.Gradient1D)
			cMap = (ColorMap.Gradient1D<String>) getColorMap();
		double[] masses = decodeRow(colors, cMap, nEntries);
		if (idx >= masses.length)
			return -1.0;
		return masses[idx];
	}

	@Override
	public void rebinGraphData(int oldBins, int newBins) {
		if (newBins <= 0 || oldBins <= 0 || geometry == null)
			return;
		String[] oldData = (data == null ? null : Arrays.copyOf(data, data.length));
		ColorMap.Gradient1D<String> cMap = null;
		if (getColorMap() instanceof ColorMap.Gradient1D)
			cMap = (ColorMap.Gradient1D<String>) getColorMap();
		List<String[]> oldHistory = null;
		if (buffer != null && !buffer.isEmpty()) {
			oldHistory = new ArrayList<>(buffer.getSize());
			Iterator<String[]> it = buffer.ordered();
			while (it.hasNext()) {
				String[] row = it.next();
				oldHistory.add(Arrays.copyOf(row, row.length));
			}
		}
		AbstractGeometry rebinnedGeometry = geometry.clone();
		rebinnedGeometry.setSize(newBins);
		setGeometry(rebinnedGeometry);

		if (oldData != null && data != null) {
			String[] rebinned = null;
			if (cMap != null) {
				double[] masses = decodeRow(oldData, cMap, oldBins);
				if (masses.length == oldBins) {
					double[] rebinnedMasses = rebinRow(masses, oldBins, newBins);
					String[] encoded = encodeRow(rebinnedMasses, cMap);
					if (encoded.length == newBins)
						rebinned = encoded;
				}
			}
			if (rebinned == null)
				rebinned = Arrays.copyOf(oldData, data.length);
			System.arraycopy(rebinned, 0, data, 0, Math.min(rebinned.length, data.length));
		}

		if (oldHistory != null && buffer != null) {
			buffer.clear();
			for (String[] row : oldHistory) {
				String[] rebinned = null;
				if (cMap != null) {
					double[] masses = decodeRow(row, cMap, oldBins);
					if (masses.length == oldBins) {
						double[] rebinnedMasses = rebinRow(masses, oldBins, newBins);
						String[] encoded = encodeRow(rebinnedMasses, cMap);
						if (encoded.length == newBins)
							rebinned = encoded;
					}
				}
				if (rebinned == null)
					rebinned = Arrays.copyOf(row, newBins);
				buffer.append(rebinned);
			}
		}
	}

	/**
	 * Decode a 1D histogram row from colors into normalized probability masses.
	 *
	 * @param source source colors
	 * @param cMap   color map used for encoding
	 * @param bins   number of bins
	 * @return decoded masses or an empty array if decoding is not possible
	 */
	private static double[] decodeRow(String[] source, ColorMap.Gradient1D<String> cMap, int bins) {
		if (source == null || cMap == null || bins <= 0)
			return EMPTY_MASS_ROW;
		double[] decoded = new double[bins];
		int len = Math.min(Math.min(source.length, decoded.length), bins);
		double sum = 0.0;
		for (int i = 0; i < len; i++) {
			double mass = nonNegativeFinite(cMap.normalize(source[i]));
			decoded[i] = mass;
			sum += mass;
		}
		if (sum <= 0.0)
			return EMPTY_MASS_ROW;
		double invSum = 1.0 / sum;
		for (int i = 0; i < len; i++)
			decoded[i] *= invSum;
		return decoded;
	}

	/**
	 * Convert non-finite or negative values to zero.
	 *
	 * @param value the value to sanitize
	 * @return {@code value} if finite and non-negative, {@code 0.0} otherwise
	 */
	private static double nonNegativeFinite(double value) {
		return (Double.isFinite(value) && value >= 0.0 ? value : 0.0);
	}

	/**
	 * Encode histogram masses into colors using the graph color map.
	 *
	 * @param masses histogram masses
	 * @param cMap   color map used for encoding
	 * @return encoded colors or an empty array if encoding is not possible
	 */
	private static String[] encodeRow(double[] masses, ColorMap.Gradient1D<String> cMap) {
		if (masses == null || cMap == null || masses.length == 0)
			return EMPTY_COLOR_ROW;
		String[] encoded = new String[masses.length];
		cMap.setRange(0.0, ArrayMath.max(masses));
		cMap.translate(masses, encoded);
		return encoded;
	}

	/**
	 * Rebin 1D histogram masses by exact overlap.
	 *
	 * @param source  source masses
	 * @param oldBins old bins
	 * @param newBins new bins
	 * @return rebinned masses
	 */
	private static double[] rebinRow(double[] source, int oldBins, int newBins) {
		double[] rebinned = new double[newBins];
		for (int i = 0; i < oldBins; i++) {
			double start = i / (double) oldBins;
			double end = (i + 1) / (double) oldBins;
			int j0 = Math.max(0, (int) Math.floor(start * newBins));
			int j1 = Math.min(newBins - 1, (int) Math.ceil(end * newBins) - 1);
			for (int j = j0; j <= j1; j++) {
				double nStart = j / (double) newBins;
				double nEnd = (j + 1) / (double) newBins;
				double overlap = Math.max(0.0, Math.min(end, nEnd) - Math.max(start, nStart));
				if (overlap <= 0.0)
					continue;
				rebinned[j] += source[i] * overlap * oldBins;
			}
		}
		return rebinned;
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
		legend.draw();
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
		if (geometry == null || dh <= 0)
			return;
		double vy = (viewCorner == null ? 0.0 : viewCorner.getY());
		double z = Math.max(1e-12, zoomFactor);
		int nRows = buffer.getSize();
		if (nRows == 0)
			return;
		int startRow = Math.max(0, (int) Math.floor(vy / (z * dh)));
		int rowsVisible = Math.max(1, (int) Math.ceil(bounds.getHeight() / (z * dh)) + 2);
		int endRow = Math.min(nRows, startRow + rowsVisible);
		for (int row = startRow; row < endRow; row++) {
			int xshift = 0;
			String[] state = buffer.get(row);
			int yshift = row * dh;
			for (int n = 0; n < geometry.getSize() && n < state.length; n++) {
				g.setFillStyle(state[n]);
				fillRect(xshift, yshift, dw, dh);
				xshift += dw;
			}
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
		if (style.offsetYTickLabels)
			updateYTickOffset();
		else
			style.yTickOffset = 0.0;
		applyViewportRanges(baseXMin, baseXMax, baseYMin, baseYMax);
		drawFrameOverlay(true);
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
		double rowsPerViewport = h / dh;
		if (rowsPerViewport <= 1.0)
			return;
		double yPerRow = (baseYMin - baseYMax) / (rowsPerViewport - 1.0);
		double topRow = vy / (z * dh);
		double bottomRow = (vy + h) / (z * dh) - 1.0;
		style.xMin = baseXMin + fx0 * xRange;
		style.xMax = baseXMin + fx1 * xRange;
		style.yMax = baseYMax + topRow * yPerRow;
		style.yMin = baseYMax + bottomRow * yPerRow;
	}

	@Override
	protected void populateGraphContextMenu(ContextMenu menu, int x, int y) {
		view.addAxesMenu(menu, this);
	}

	@Override
	public void populateLocalAxesMenu(ContextMenu axesMenu) {
		ContextMenuCheckBoxItem absoluteTimeMenu = new ContextMenuCheckBoxItem("Absolute time", () -> {
			style.offsetYTickLabels = !style.offsetYTickLabels;
			if (style.offsetYTickLabels)
				updateYTickOffset();
			else
				style.yTickOffset = 0.0;
			paint(true);
		});
		absoluteTimeMenu.setChecked(style.offsetYTickLabels);
		axesMenu.add(absoluteTimeMenu);
	}

	@Override
	public void calcBounds(int width, int height) {
		super.calcBounds(width, height);
		clearMessage();
		noGraph = false;
		dw = 0;
		dh = 0;
		dR = 0.0;

		double plotX = bounds.getX();
		int bWidth = (int) Math.rint(bounds.getWidth());
		dw = bWidth / geometry.getSize();
		int bHeight = (int) Math.rint(bounds.getHeight());
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
		bounds.set(plotX + dx, bounds.getY() + dy, adjw, adjh);
		legend.setBounds(width, height, bounds);
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

	@Override
	public void shift(int dx, int dy) {
		if (hasMessage)
			return;
		double z = zoomFactor;
		double maxX = Math.max(0.0, z * bounds.getWidth() - bounds.getWidth());
		double contentHeight = getContentHeight();
		double maxY = Math.max(0.0, z * contentHeight - bounds.getHeight());
		viewCorner.set(
				Math.min(maxX, Math.max(0.0, viewCorner.getX() + dx)),
				Math.min(maxY, Math.max(0.0, viewCorner.getY() + dy)));
		paint(true);
	}

	@Override
	protected void zoom(double zoom, double fx, double fy) {
		if (hasMessage)
			return;
		double newZoomFactor = Math.min(Zooming.ZOOM_MAX, Math.max(1.0, zoomFactor * zoom));
		double dz = newZoomFactor - zoomFactor;
		if (Math.abs(dz) < 1e-8)
			return;
		double w = bounds.getWidth();
		double h = bounds.getHeight();
		double contentHeight = getContentHeight();
		double maxX = Math.max(0.0, w * (newZoomFactor - 1.0));
		double maxY = Math.max(0.0, contentHeight * newZoomFactor - h);
		viewCorner.set(
				Math.min(maxX, Math.max(0.0, viewCorner.getX() + w * dz * fx)),
				Math.min(maxY, Math.max(0.0, viewCorner.getY() + h * dz * fy)));
		zoomFactor = newZoomFactor;
		if (zoomInertiaTimer.isRunning())
			element.addClassName(dz > 0 ? CURSOR_ZOOM_IN_CLASS : CURSOR_ZOOM_OUT_CLASS);
		paint(true);
	}

	/**
	 * Return content height in graph coordinates, taking buffered history into
	 * account.
	 *
	 * @return content height in pixels
	 */
	private double getContentHeight() {
		double contentHeight = bounds.getHeight();
		if (buffer != null && dh > 0)
			contentHeight = Math.max(contentHeight, buffer.getSize() * (double) dh);
		return contentHeight;
	}

	/**
	 * Update the y-axis tick label offset to the current model time.
	 */
	private void updateYTickOffset() {
		if (view.getModel() == null) {
			style.yTickOffset = 0.0;
			return;
		}
		style.yTickOffset = view.getModel().getUpdates();
	}
}
