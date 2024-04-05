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

import java.util.Arrays;
import java.util.Iterator;

import org.evoludo.geom.Node2D;
import org.evoludo.geom.Path2D;
import org.evoludo.geom.Point2D;
import org.evoludo.graphics.AbstractGraph.Shifting;
import org.evoludo.graphics.AbstractGraph.Zooming;
import org.evoludo.ui.ContextMenu;
import org.evoludo.ui.ContextMenuCheckBoxItem;
import org.evoludo.ui.ContextMenuItem;
import org.evoludo.simulator.ColorMap;
import org.evoludo.simulator.Geometry;
import org.evoludo.simulator.Network;
import org.evoludo.simulator.Network.Status;
import org.evoludo.simulator.Network2D;
import org.evoludo.simulator.models.Model;
import org.evoludo.util.Formatter;
import org.evoludo.util.RingBuffer;

import com.google.gwt.core.client.Duration;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.dom.client.Touch;
import com.google.gwt.event.dom.client.DoubleClickEvent;
import com.google.gwt.event.dom.client.DoubleClickHandler;
import com.google.gwt.event.dom.client.MouseDownEvent;
import com.google.gwt.event.dom.client.MouseMoveEvent;
import com.google.gwt.event.dom.client.MouseUpEvent;
import com.google.gwt.event.dom.client.TouchEndEvent;
import com.google.gwt.event.dom.client.TouchMoveEvent;
import com.google.gwt.event.dom.client.TouchStartEvent;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.Label;

/**
 * The graphical representation of network structures in 2D.
 *
 * @author Christoph Hauert
 */
public class PopGraph2D extends AbstractGraph implements Network.LayoutListener, //
		Zooming, Shifting, DoubleClickHandler {

	/**
	 * The structure of the population.
	 */
	protected Geometry geometry;

	/**
	 * The network representation of the population structure or {@code null} if not
	 * applicable.
	 */
	protected Network2D network;

	/**
	 * The buffer to store historical data, if applicable.
	 * <p>
	 * <strong>Note:</strong> This is deliberately hiding
	 * {@link AbstractGraph#buffer} because it serves the exact same purpose but is
	 * a {@code RingBuffer} of type {@code String[]} rather than {@code double[]}.
	 */
	@SuppressWarnings("hiding")
	protected RingBuffer<String[]> buffer;

	/**
	 * {@inheritDoc}
	 * <p>
	 * <strong>Hack alert:</strong> This needs to be overridden because
	 * {@link #buffer} is hiding {@link AbstractGraph#buffer}.
	 */
	@Override
	public boolean hasHistory() {
		return (buffer != null);
	}

	/**
	 * The map for translating discrete traits into colors.
	 */
	protected ColorMap<String> colorMap;

	/**
	 * The label of the graph.
	 */
	protected Label label;

	/**
	 * Create a graph for graphically visualizing the structure of a network (or
	 * population). Allocates the canvas and the label and retrieves the shared
	 * tooltip and context menu.
	 * 
	 * <h2>CSS Style Rules</h2>
	 * <dl>
	 * <dt>.evoludo-PopGraph2D</dt>
	 * <dd>the graph element.</dd>
	 * <dt>.evoludo-Label2D</dt>
	 * <dd>the label element.</dd>
	 * </dl>
	 * 
	 * @param controller the controller of this graph
	 * @param tag        the identifying tag
	 */
	public PopGraph2D(NodeGraphController controller, int tag) {
		super(controller, tag);
		setStylePrimaryName("evoludo-PopGraph2D");
		label = new Label("Gugus");
		label.setStyleName("evoludo-Label2D");
		label.getElement().getStyle().setZIndex(1);
		label.setVisible(false);
		wrapper.add(label);
	}

	@Override
	public void activate() {
		super.activate();
		// lazy allocation of memory for colors
		if (geometry != null && (colors == null || colors.length != geometry.size))
			colors = new String[geometry.size];
		// the listener of network can change - make sure it is us now
		// note geometry (and network) may be null for Model.ODE or Model.SDE
		if (network != null)
			network.setLayoutListener(this);
		doubleClickHandler = addDoubleClickHandler(this);
	}

	@Override
	public void alloc() {
		super.alloc();
		if (geometry.isType(Geometry.Type.LINEAR)) {
			// only linear geometries have a buffer
			double h = bounds.getHeight();
			double w = bounds.getWidth();
			if (h == 0 || dh == 0 || w == 0 || dw == 0) {
				// graph has never been shown - dimensions not yet available
				if (w > 0 && (int) (w / geometry.size) < 1) {
					// too many nodes
					buffer = null;
				} else if (buffer == null || buffer.capacity() != geometry.size) {
					buffer = new RingBuffer<String[]>(geometry.size);
				}
			} else {
				// determine length of history visible
				int steps = Math.max((int) ((h / dh) * 1.25), MIN_BUFFER_SIZE); // visible history plus 1/4 to spare
				if (buffer == null || buffer.capacity() < MIN_BUFFER_SIZE) {
					buffer = new RingBuffer<String[]>(steps);
				} else {
					buffer.setCapacity(steps);
				}
			}
			// since we have a buffer, allocate memory for colors.
			// needed e.g. for histograms (see MVDistribution).
			if (geometry != null && (colors == null || colors.length != geometry.size))
				colors = new String[geometry.size];
		} else {
			buffer = null;
		}
	}

	/**
	 * Set the graph label to the string {@code msg} (no HTML formatting).
	 * 
	 * @param msg the text for the label of the graph
	 */
	public void setGraphLabel(String msg) {
		label.setText(msg);
		label.setVisible(msg != null && !msg.isEmpty());
	}

	/**
	 * Set the geometry backing the graph.
	 * 
	 * @param geometry the structure of the population
	 */
	public void setGeometry(Geometry geometry) {
		this.geometry = geometry;
		// geometry may be null for Model.ODE or Model.SDE
		if (geometry == null)
			return;
		setGraphLabel(geometry.getName());
		// update population
		network = geometry.getNetwork2D();
	}

	/**
	 * Get the geometry backing the graph.
	 * 
	 * @return the structure of the population
	 */
	public Geometry getGeometry() {
		return geometry;
	}

	/**
	 * Set the map for translating trait values into colors.
	 * 
	 * @param colorMap the trait-to-colour map
	 */
	public void setColorMap(ColorMap<String> colorMap) {
		this.colorMap = colorMap;
	}

	/**
	 * Get the map for translating trait values into colors.
	 * 
	 * @return the trait-to-colour map
	 */
	public ColorMap<String> getColorMap() {
		return colorMap;
	}

	/**
	 * Get the color data for all nodes as an array.
	 * 
	 * @return the array of node colors
	 */
	public String[] getData() {
		return colors;
	}

	/**
	 * Get the buffer with historical data about previous states of the network.
	 * This is applicable only if {@code hasHistory()} returns {@code true}, i.e.
	 * currently only for {@code Geometry.Type#LINEAR}. If no historical data
	 * collected this returns {@code null}.
	 * 
	 * @return the buffer with historical data or {@code null}
	 */
	public RingBuffer<String[]> getBuffer() {
		return buffer;
	}

	/**
	 * Get the graphical 2D network representation of the graph represented by
	 * geometry.
	 * 
	 * @return the 2D network representation of this graph
	 */
	public Network2D getNetwork() {
		return network;
	}

	@Override
	public void reset() {
		super.reset();
		if (network != null)
			network.reset();
		calcBounds();
		if (buffer != null)
			buffer.clear();
	}

	@Override
	public synchronized void layoutProgress(double p) {
		displayMessage("Laying out network...  " + Formatter.formatPercent(p, 0) + " completed.");
	}

	@Override
	public synchronized void layoutUpdate() {
		drawNetwork();
	}

	@Override
	public synchronized void layoutComplete() {
		((NodeGraphController) controller).layoutComplete();
	}

	/**
	 * Shift a single node with index {@code nodeidx} by {@code (dx, dy)}. Positive
	 * {@code dx} shift the node to the right and positive {@code dy} shift upwards.
	 * 
	 * @param nodeidx the index of the node
	 * @param dx      the horizontal shift of the node
	 * @param dy      the vertical shift of the node
	 */
	public void shiftNodeBy(int nodeidx, int dx, int dy) {
		if (nodeidx < 0) {
			// invalid node
			return;
		}
		switch (network.getStatus()) {
			case HAS_LAYOUT:
				double rr = network.getRadius();
				// bounds.width==bounds.height for networks
				double iscale = (rr + rr) / (bounds.getWidth() * zoomFactor);
				double xaspect, yaspect;
				if (width < height) {
					// portrait
					xaspect = 1.0;
					yaspect = (double) height / width;
				} else {
					// landscape
					xaspect = (double) width / height;
					yaspect = 1.0;
				}
				Node2D node = network.get(nodeidx);
				node.set(Math.max(-rr * xaspect + node.r, Math.min(rr * xaspect - node.r, node.x - dx * iscale)),
						Math.max(-rr * yaspect + node.r, Math.min(rr * yaspect - node.r, node.y - dy * iscale)));
				network.linkNodes();
				paint();
				return;
			case NO_LAYOUT:
				// lattices - shift view instead
				shift(dx, dy);
				return;
			default:
				// nothing (yet) to shift
				return;
		}
	}

	/**
	 * Helper method to draw the network.
	 */
	private synchronized void drawNetwork() {
		// in case layout was not animated, a message was displayed but now hasMessage
		// must be cleared; also clears canvas
		prepCanvas();
		int nNodes = geometry.size;
		// scale universe
		double r = network.getRadius();
		double su = bounds.getWidth() / (r + r); // same as height in this case
		g.scale(su, su);
		g.save();
		g.translate(r, r);
		g.setFillStyle(style.linkColor);
		Path2D links = network.getLinks();
		if (!links.isEmpty()) {
			g.setLineWidth(style.linkWidth);
			stroke(links);
		}
		String current = colors[0];
		g.setFillStyle(current);
		Node2D[] nodes = network.toArray();
		for (int k = 0; k < nNodes; k++) {
			String next = colors[k];
			// potential optimization of drawing
			if (next != current) {
				g.setFillStyle(next);
				current = next;
			}
			Node2D node = nodes[k];
			fillCircle(node.x, node.y, node.r);
		}
		g.restore();
		drawFrame(0, 0, su);
		g.restore();
	}

	// @Override
	// public void drawFrame(int xLevels, int yLevels, double uniscale ) {
	// GWT.log("PopGraph2D.drawFrame: ");
	// super.drawFrame(xLevels, yLevels, uniscale);
	// if( frame.x>=frame.width ) {
	// g.save();
	// resetTransforms();
	// g.scale(scale, scale);
	// setLineWidth(style.frameWidth);
	// strokeLine(frame.x, frame.y-0.5, frame.x, frame.y+frame.height);
	// g.restore();
	// }
	// }

	/**
	 * Helper method to get the canvas ready for drawing the graph.
	 */
	private void prepCanvas() {
		clearMessage();
		g.save();
		g.scale(scale, scale);
		clearCanvas();
		g.translate(bounds.getX() - viewCorner.x, bounds.getY() - viewCorner.y);
		g.scale(zoomFactor, zoomFactor);
	}

	/**
	 * Add data to buffer of graph. If the graph has a buffer and the state changed,
	 * {@code isNext==true}, then the data is added.
	 *
	 * @param isNext   {@code true} if the state has changed
	 */
	public void addData(boolean isNext) {
		if (buffer != null) {
			// add copy of colors array to buffer
			// note: cannot be reliably done in RingBuffer class without reflection
			String[] colorCopy = Arrays.copyOf(colors, colors.length);
			if (isNext || buffer.isEmpty())
				buffer.append(colorCopy);
			else
				buffer.replace(colorCopy);
		}
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * Depending on the backing geometry this either
	 * <ol>
	 * <li>shows a message, if no graphical representation is available, e.g. for 3D
	 * cubic lattices, or if there are too many nodes so that each node becomes to
	 * small to display on screen.
	 * <li>shows lattice geometries.
	 * <li>initiates the generic layouting process for arbitrary network structures.
	 * </ol>
	 * 
	 * @see Network2D
	 */
	@Override
	public void paint() {
		if (!isActive)
			return;
		// helper variables
		double xshift, yshift;
		int row;

		Geometry.Type type = geometry.getType();
		if (isHierarchy)
			type = geometry.subgeometry;
		// geometries that have special/fixed layout
		switch (type) {
			case CUBE: // should not get here...
			case VOID:
				displayMessage("No representation for geometry!");
				return;

			case TRIANGULAR:
				if (dw < 1) {
					// too small
					displayMessage("Population size to large!");
					return;
				}
				prepCanvas();
				Path2D triup = Path2D.createPolygon2D(new double[] { 0, dw, dw2 }, new double[] { 0, 0, -dh });
				Path2D tridown = Path2D.createPolygon2D(new double[] { 0, dw, dw2 }, new double[] { 0, 0, dh });
				triup.translate(0, dh);
				tridown.translate(dw2, 0);
				int s2 = side / 2;
				for (int h = 0; h < side; h += 2) {
					row = h * side;
					for (int w = 0; w < side; w += 2) {
						setFillStyleAt(row + w);
						fill(triup);
						triup.translate(dw, 0);
						setFillStyleAt(row + w + 1);
						fill(tridown);
						tridown.translate(dw, 0);
					}
					triup.translate(-s2 * dw + dw2, dh);
					tridown.translate(-s2 * dw - dw2, dh);
					row += side;
					for (int w = 0; w < side; w += 2) {
						setFillStyleAt(row + w);
						fill(tridown);
						tridown.translate(dw, 0);
						setFillStyleAt(row + w + 1);
						fill(triup);
						triup.translate(dw, 0);
					}
					triup.translate(-s2 * dw - dw2, dh);
					tridown.translate(-s2 * dw + dw2, dh);
				}
				triup.translate(0, -side * dh);
				tridown.translate(0, -side * dh);
				drawFrame(0, 0);
				g.restore();
				break;

			case HONEYCOMB:
				if (dw < 1) {
					// too small
					displayMessage("Population size to large!");
					return;
				}
				prepCanvas();
				Path2D hex = Path2D.createPolygon2D(new double[] { 0, -dw2, -dw2, 0, dw2, dw2 },
						new double[] { 0, dh3, dh, dh + dh3, dh, dh3 });
				hex.translate(dw2, 0);

				for (int h = 0; h < side; h++) {
					row = h * side;
					for (int w = 0; w < side; w++) {
						setFillStyleAt(row + w);
						fill(hex);
						hex.translate(dw, 0);
					}
					int sign = -2 * (h % 2) + 1;
					hex.translate(-side * dw + sign * dw2, dh);
				}
				if (side % 2 == 0)
					hex.translate(0, -dh * side);
				else
					hex.translate(-dw2, -dh * side);
				drawFrame(0, 0);
				g.restore();
				break;

			case SQUARE_NEUMANN:
			case SQUARE_NEUMANN_2ND:
			case SQUARE_MOORE:
			case SQUARE:
				if (dw < 1) {
					// too small
					displayMessage("Population size to large!");
					return;
				}
				prepCanvas();
				// node 0 in lower left corner
				yshift = bounds.getHeight() - dh;
				row = 0;
				for (int h = 0; h < side; h++) {
					if (isHierarchy && h > 0) {
						for (int i = 0; i < hLevels; i++) {
							if (h % hPeriods[i] != 0)
								break;
							yshift -= HIERARCHY_GAP;
						}
					}
					xshift = 0.0;
					for (int w = 0; w < side; w++) {
						if (isHierarchy && w > 0) {
							for (int i = 0; i < hLevels; i++) {
								if (w % hPeriods[i] != 0)
									break;
								xshift += HIERARCHY_GAP;
							}
						}
						setFillStyleAt(row + w);
						fillRect(xshift, yshift, dw, dh);
						xshift += dw;
					}
					yshift -= dh;
					row += side;
				}
				if (style.showDecoratedFrame)
					drawFrame(4, 4);
				else
					drawFrame(0, 0);
				g.restore();
				break;

			case LINEAR:
				if (dw < 1) {
					// too small
					displayMessage("Population size to large!");
					return;
				}

				int nSteps = (int) (bounds.getHeight() / dh);
				yshift = 0;

				prepCanvas();
				Iterator<String[]> i = buffer.iterator();
				while (i.hasNext() && nSteps-- > 0) {
					xshift = 0;
					String[] state = i.next();
					for (int n = 0; n < geometry.size; n++) {
						g.setFillStyle(state[n]);
						fillRect(xshift, yshift, dw, dh);
						xshift += dw;
					}
					yshift += dh;
				}
				drawFrame(4, 4);
				g.restore();
				break;

			// case GENERIC:
			// case DYNAMIC:
			// and many more...
			default:
				int radius = Math.min(width / 2, height / 2);
				int dR = (int) Math.sqrt(radius * radius * 2 / geometry.size);
				if (dR < 2) {
					// too small
					displayMessage("Population size to large!");
					return;
				}
				switch (network.getStatus()) {
					case ADJUST_LAYOUT:
					case NEEDS_LAYOUT:
						network.doLayout(this);
						break;
					case HAS_LAYOUT:
						drawNetwork();
						break;
					default:
						// lattices, messages or layout in progress...
						break;
				}
				break;
		}
		tooltip.update();
	}

	/**
	 * The size of the graph for lattices.
	 */
	protected int side;

	/**
	 * The width of a node for lattices.
	 */
	protected int dw;

	/**
	 * Convenience variable. One half of the width of a node for lattices,
	 * {@code dw/2}.
	 */
	protected int dw2;

	/**
	 * The height of a node for lattices.
	 */
	protected int dh;

	/**
	 * Convenience variable. One third of the height of a node for lattices,
	 * {@code dh/3}.
	 */
	protected int dh3;

	/**
	 * Convenience variable. The flag indicating whether the backing geometry is a
	 * hierarchical structure.
	 */
	boolean isHierarchy = false;

	/**
	 * Convenience variable. The number of hierarchical levels.
	 */
	int hLevels = 0;

	/**
	 * Convenience variable. The number of units in each hierarchical level.
	 */
	int[] hPeriods = null;

	/**
	 * Convenience variable. The gap between subsequent units in hierarchical
	 * structures.
	 */
	static final int HIERARCHY_GAP = 1; // unit gap in pixels

	@Override
	protected boolean calcBounds() {
		if (!super.calcBounds() || geometry == null)
			return false;

		double h, w;
		int diameter;
		Geometry.Type type = geometry.getType();
		isHierarchy = (type == Geometry.Type.HIERARCHY);
		if (isHierarchy)
			type = geometry.subgeometry;
		if (!isHierarchy || !type.isSquareLattice())
			hPeriods = null;
		// geometries that have special/fixed layout
		switch (type) {
			case CUBE: // should not get here...
			case VOID:
				return true;

			case TRIANGULAR:
				side = (int) (Math.sqrt(geometry.size) + 0.5);
				diameter = Math.min(width, height);
				dw2 = diameter / (side + 3);
				// calculate width and height
				w = dw2 * (side + 1);
				dw = 2 * dw2;
				dh = diameter / (side + 1);
				if (dw < 1 || dh < 1) {
					// too small
					bounds.setSize(width, height);
					return true;
				}
				h = dh * side;
				bounds.set((width - w) / 2, (height - h) / 2, w, h);
				style.showFrame = false;
				break;

			case HONEYCOMB:
				side = (int) (Math.sqrt(geometry.size) + 0.5);
				diameter = Math.min(width, height);
				dw2 = diameter / (2 * side + 1);
				// calculate width and height
				w = dw2 * (2 * side + 1);
				dw = 2 * dw2;
				dh3 = diameter / (3 * side + 1);
				dh = 3 * dh3;
				if (dw < 1 || dh3 < 1) {
					// too small
					bounds.setSize(width, height);
					return true;
				}
				h = dh3 * (3 * side + 1);
				bounds.set((width - w) / 2, (height - h) / 2, w, h);
				style.showFrame = false;
				break;

			case SQUARE_NEUMANN:
			case SQUARE_NEUMANN_2ND:
			case SQUARE_MOORE:
			case SQUARE:
				// note: a bit hackish to allow drawing frame for lattices but axes for 2D
				// distributions
				if (style.showDecoratedFrame)
					super.calcBounds();
				else
					bounds.set(style.minPadding, style.minPadding, width - 2 * style.minPadding,
							height - 2 * style.minPadding);

				side = (int) (Math.sqrt(geometry.size) + 0.5);
				// for hierarchical structures add gap between units
				int gap = 0;
				if (isHierarchy) {
					hLevels = geometry.hierarchy.length - 1;
					if (hPeriods == null || hPeriods.length != hLevels)
						hPeriods = new int[hLevels];
					hPeriods[0] = (int) Math.sqrt(geometry.hierarchy[hLevels]);
					gap = side / hPeriods[0] - 1;
					for (int i = 1; i < hLevels; i++) {
						hPeriods[i] = hPeriods[i - 1] * (int) Math.sqrt(geometry.hierarchy[hLevels - i]);
						gap += side / hPeriods[i] - 1;
					}
					gap *= HIERARCHY_GAP;
				}
				// keep sites square
				w = bounds.getWidth();
				h = bounds.getHeight();
				dw = (int) (Math.min(w, h) - gap) / side;
				if (dw < 1) {
					// too small
					bounds.setSize(width, height);
					return true;
				}
				dh = dw;
				int newdim = dw * side + gap;
				bounds.set((w - newdim) / 2, (h - newdim) / 2, newdim, newdim);
				style.showFrame = true;
				break;

			case LINEAR:
				// estimate y-range
				super.calcBounds();
				w = bounds.getWidth();
				dw = (int) (w / geometry.size);
				if (dw < 1) {
					// too small
					bounds.setSize(width, height);
					return true;
				}
				dh = dw;
				double adjw = dw * geometry.size;
				h = bounds.getHeight();
				double adjh = h - (h % dh);
				bounds.set((w - adjw) / 2, (h - adjh) / 2, adjw, adjh);
				// determine length of history visible
				int steps = (int) (h / dh);
				// polish...
				if (steps > 0) {
					if (buffer == null)
						buffer = new RingBuffer<String[]>(5 * steps / 4);
					else
						buffer.setCapacity(5 * steps / 4);
					// set y-range
					style.setYRange(steps - 1);
				} else {
					if (buffer == null)
						buffer = new RingBuffer<String[]>(geometry.size);
				}
				style.showFrame = true;
				break;

			// case Geometry.WHEEL:
			// case Geometry.STAR:
			// case Geometry.MEANFIELD:
			// case Geometry.COMPLETE:
			// case Geometry.PETALS:
			// case Geometry.DYNAMIC:
			default:
				int radius = Math.min(width / 2, height / 2);
				int dR = (int) Math.sqrt(radius * radius * 2 / geometry.size);
				buffer = null;
				if (dR < 2) {
					// too small
					bounds.setSize(width, height);
					return true;
				}
				diameter = Math.min(width, height);
				bounds.set((width - diameter) / 2, (height - diameter) / 2, diameter, diameter);
				style.showFrame = false;
				break;
		}
		return true;
	}

	@Override
	public String getTooltipAt(int x, int y) {
		// no network may have been initialized (e.g. for ODE/SDE models)
		if (hitDragged || contextMenu.isVisible() || network == null || network.isStatus(Status.LAYOUT_IN_PROGRESS))
			return null;
		int node = findNodeAt(x, y);
		// when switching views the graph may not yet be ready to return
		// data for tooltips (colors == null)
		if (node < 0 || colors == null) {
			element.removeClassName("evoludo-cursorPointNode");
			return null;
		}
		element.addClassName("evoludo-cursorPointNode");
		return ((NodeGraphController) controller).getTooltipAt(this, node);
	}

	/**
	 * Return value if {@link #findNodeAt(int, int)} couldn't find a node at the
	 * mouse position.
	 */
	static final int FINDNODEAT_OUT_OF_BOUNDS = -1;

	/**
	 * Return value if {@link #findNodeAt(int, int)} isn't implemented for the
	 * particular backing geometry.
	 */
	static final int FINDNODEAT_UNIMPLEMENTED = -2;

	/**
	 * Find the index of the node at the location with coordinates {@code (x, y)}.
	 * 
	 * @param x the {@code x}-coordinate of the location
	 * @param y the {@code y}-coordinate of the location
	 * @return the index of the node
	 */
	public int findNodeAt(int x, int y) {
		// no network may have been initialized (e.g. for ODE/SDE models)
		if (hasMessage || network == null || network.isStatus(Status.LAYOUT_IN_PROGRESS))
			return FINDNODEAT_OUT_OF_BOUNDS;
		int sx, sy, c, r;
		double rr;

		Geometry.Type type = geometry.getType();
		if (isHierarchy)
			type = geometry.subgeometry;
		switch (type) {
			// 3D views must deal with this
			case CUBE:
				return FINDNODEAT_UNIMPLEMENTED;

			case TRIANGULAR:
				// some heuristic adjustments... cause remains mysterious
				x = x - (int) (style.frameWidth * zoomFactor + 0.5);
				y = y - (int) (style.frameWidth * zoomFactor - 0.5);
				if (!bounds.contains(x, y))
					return FINDNODEAT_OUT_OF_BOUNDS;

				sx = (int) ((viewCorner.x + x - bounds.getX()) / zoomFactor + 0.5);
				sy = (int) ((viewCorner.y + y - bounds.getY()) / zoomFactor + 0.5);
				r = sy / dh;
				c = sx / dw2;
				int rx = sy - r * dh;
				int cx = sx - c * dw2;
				if (c % 2 + r % 2 == 1)
					rx = dh - rx;
				double loc = (double) rx / (double) dh + (double) cx / (double) dw2;
				if ((c == 0 && loc < 1.0) || (c == side && loc > 1.0))
					return FINDNODEAT_OUT_OF_BOUNDS;
				if (loc < 1.0)
					c--;
				return r * side + c;

			case HONEYCOMB:
				// some heuristic adjustments... cause remains mysterious
				x = x - (int) (style.frameWidth * zoomFactor + 0.5);
				y = y - (int) (style.frameWidth * zoomFactor - 0.5);
				if (!bounds.contains(x, y))
					return FINDNODEAT_OUT_OF_BOUNDS;

				sx = (int) ((viewCorner.x + x - bounds.getX()) / zoomFactor + 0.5);
				sy = (int) ((viewCorner.y + y - bounds.getY()) / zoomFactor + 0.5);
				r = sy / dh;
				int odd = r % 2;
				c = (sx - odd * dw2) / dw;
				rx = sy - r * dh;
				cx = sx - odd * dw2 - c * dw;
				if (cx < 0) {
					cx += dw; // make sure cx>0
					c--;
				}
				if (rx < dh3) {
					if (cx > dw2) {
						loc = (double) rx / (double) dh3 + (double) (dw - cx) / (double) dw2;
						if (loc < 1.0) {
							c += odd;
							r--;
						}
					} else {
						loc = (double) rx / (double) dh3 + (double) cx / (double) dw2;
						if (loc < 1.0) {
							c -= 1 - odd;
							r--;
						}
					}
				}
				if (r < 0 || c < 0 || r == side || c == side)
					return FINDNODEAT_OUT_OF_BOUNDS;
				return r * side + c;

			case SQUARE_NEUMANN:
			case SQUARE_NEUMANN_2ND:
			case SQUARE_MOORE:
			case SQUARE:
				// some heuristic adjustments... cause remains mysterious
				x = x - (int) (style.frameWidth * zoomFactor + 0.5);
				y = y - (int) (style.frameWidth * zoomFactor - 0.5);
				if (!bounds.contains(x, y))
					return FINDNODEAT_OUT_OF_BOUNDS;

				sx = (int) ((viewCorner.x + x - bounds.getX()) / zoomFactor + 0.5);
				sy = (int) ((viewCorner.y + y - bounds.getY()) / zoomFactor + 0.5);
				// node 0 in lower left corner
				c = sx / dw;
				r = side - 1 - sy / dh;
				if (isHierarchy) {
					// find accumulated width of gaps to the left of mouse pointer as well as height
					// of gaps above mouse pointer
					// without gaps we would be in col c
					int wgap = 0, hgap = 0;
					for (int i = 0; i < hLevels; i++) {
						wgap += c / hPeriods[i]; // gaps left of mouse
						hgap += (side - 1 - r) / hPeriods[i];
					}
					c = (sx - wgap * HIERARCHY_GAP) / dw;
					r = side - 1 - (sy - hgap * HIERARCHY_GAP) / dh;
				}
				return r * side + c;

			case LINEAR:
				if (!bounds.contains(x, y))
					return FINDNODEAT_OUT_OF_BOUNDS;
				sx = (int) ((viewCorner.x + x - bounds.getX()) / zoomFactor + 0.5);
				sy = (int) ((viewCorner.y + y - bounds.getY()) / zoomFactor + 0.5);
				c = sx / dw;
				r = sy / dh;
				return r * geometry.size + c;

			default:
				// note: cannot check bounds (or anything else) to rule out that mouse hovers
				// over node because nodes may have been manually shifted.
				rr = network.getRadius();
				double iscale = (rr + rr) / (bounds.getWidth() * zoomFactor);
				Point2D mousecoord = new Point2D((viewCorner.x + x - bounds.getX()) * iscale - rr,
						(viewCorner.y + y - bounds.getY()) * iscale - rr);
				Node2D[] nodes = network.toArray();
				// in the undesirable case that nodes overlap, nodes with a higher index are
				// drawn later (on top)
				// in order to get the top node we need to start from the back
				for (int k = network.nNodes - 1; k >= 0; k--) {
					Node2D hit = nodes[k];
					rr = hit.r;
					if (mousecoord.distance2(hit) <= rr * rr)
						return k;
				}
				return FINDNODEAT_OUT_OF_BOUNDS;
		}
	}

	/**
	 * The index of the node that was hit by the mouse or a tap.
	 */
	protected int hitNode = -1;

	/**
	 * The flag to indicate whether {@link #hitNode} is being dragged.
	 */
	protected boolean hitDragged = false;

	/**
	 * {@inheritDoc}
	 * <p>
	 * If a node has been hit by a left-click, remember the node's index and the
	 * current mouse coordinates. This information might be used by subsequent
	 * {@link MouseMoveEvent}s.
	 * 
	 * <h2>CSS Style Rules</h2>
	 * <dl>
	 * <dt>.evoludo-cursorGrabNode</dt>
	 * <dd>added to graph element.</dd>
	 * </dl>
	 * 
	 * @see #onMouseMove(MouseMoveEvent)
	 */
	@Override
	public void onMouseDown(MouseDownEvent event) {
		// super sets mouse coordinates
		super.onMouseDown(event);
		if (event.getNativeButton() == NativeEvent.BUTTON_LEFT) {
			hitNode = findNodeAt(mouseX, mouseY);
			if (hitNode >= 0) {
				element.addClassName("evoludo-cursorGrabNode");
			} else {
				element.removeClassName("evoludo-cursorGrabNode");
			}
		}
	}

	/**
	 * {@inheritDoc}
	 * <dl>
	 * <dt>.evoludo-cursorGrabNode</dt>
	 * <dd>removed from graph element.</dd>
	 * <dt>.evoludo-cursorMoveNode</dt>
	 * <dd>removed from graph element.</dd>
	 * </dl>
	 * 
	 * @see #onMouseMove(MouseMoveEvent)
	 */
	@Override
	public void onMouseUp(MouseUpEvent event) {
		super.onMouseUp(event);
		if (event.getNativeButton() == NativeEvent.BUTTON_LEFT) {
			if (hitDragged) {
				element.removeClassName("evoludo-cursorMoveNode");
				// reshow tooltip after dragging
				hitDragged = false;
				tooltip.update();
			}
			hitNode = -1;
			element.removeClassName("evoludo-cursorGrabNode");
		}
	}

	@Override
	public void onDoubleClick(DoubleClickEvent event) {
		// ignore if busy or invalid node
		int node = findNodeAt(event.getX(), event.getY());
		if (node >= 0 && !controller.isRunning()) {
			// population signals change back to us
			((NodeGraphController) controller).mouseHitNode(tag, node, event.isAltKeyDown());
		}
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * If a node has been hit by a {@link MouseDownEvent} and is being dragged,
	 * the position of the node is shifted. This allows to customize the display of
	 * the graph.
	 * 
	 * <h2>CSS Style Rules</h2>
	 * <dl>
	 * <dt>.evoludo-cursorMoveNode</dt>
	 * <dd>added to graph element when dragging a node.</dd>
	 * </dl>
	 * 
	 * @see #onMouseDown(MouseDownEvent)
	 */
	@Override
	public void onMouseMove(MouseMoveEvent event) {
		if (hitNode >= 0) {
			hitDragged = true;
			element.addClassName("evoludo-cursorMoveNode");
			int x = event.getX();
			int y = event.getY();
			shiftNodeBy(hitNode, mouseX - x, mouseY - y);
			mouseX = x;
			mouseY = y;
			return;
		}
		super.onMouseMove(event);
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * The graph reacts to different kinds of touches:
	 * <dl>
	 * <dt>short touch with two fingers ({@code &lt;250} msec)
	 * <dd>display context menu.
	 * <dt>single long touch ({@code &gt;250} msec) on a node
	 * <dd>display the tooltip.
	 * <dt>long touch with two fingers ({@code &gt;250} msec)
	 * <dd>initiates pinching zoom.
	 * <dt>double tap on a node
	 * <dd>change the strategy of the node, if applicable.
	 * </dl>
	 * 
	 * @see ContextMenu.Provider
	 * @see #populateContextMenuAt(ContextMenu, int, int)
	 * @see #onTouchMove(TouchMoveEvent)
	 */
	@Override
	public void onTouchStart(TouchStartEvent event) {
		// super processes pinching
		super.onTouchStart(event);
		JsArray<Touch> touches = event.getTouches();
		if (touches.length() > 1) {
			// more than one touch point
			return;
		}
		Touch touch = touches.get(0);
		int x = touch.getRelativeX(element);
		int y = touch.getRelativeY(element);
		int node = findNodeAt(x, y);
		if (node < 0) {
			// no node touched
			tooltip.close();
			return;
		}
		if (Duration.currentTimeMillis() - touchEndTime > 250.0) {
			// single tap
			mouseX = x;
			mouseY = y;
			hitNode = node;
			return;
		}
		// double tap?
		if (!controller.isRunning())
			// population signals change back to us
			((NodeGraphController) controller).mouseHitNode(tag, node);
		event.preventDefault();
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * The number of touches on the graph changed.
	 */
	@Override
	public void onTouchEnd(TouchEndEvent event) {
		super.onTouchEnd(event);
		JsArray<Touch> touches = event.getTouches();
		switch (touches.length()) {
			case 1: // down from two touches
				Touch touch = touches.get(0);
				int x = touch.getRelativeX(element);
				int y = touch.getRelativeY(element);
				// remember position of touch to prevent jumps
				mouseX = x;
				mouseY = y;
				break;
			case 0: // no more touches remaining - reset values
				// touchEndTime = Duration.currentTimeMillis();
				hitNode = -1;
				break;
			default:
		}
	}

	/**
	 * {@inheritDoc}
	 * <dl>
	 * <dt>move one finger on node
	 * <dd>shift node.
	 * </dl>
	 */
	@Override
	public void onTouchMove(TouchMoveEvent event) {
		JsArray<Touch> touches = event.getTouches();
		if (touches.length() == 1 && hitNode >= 0) {
			// shift node
			Touch touch = touches.get(0);
			int x = touch.getRelativeX(element);
			int y = touch.getRelativeY(element);
			// shift position of node
			shiftNodeBy(hitNode, mouseX - x, mouseY - y);
			mouseX = x;
			mouseY = y;
			event.preventDefault();
			return;
		}
		super.onTouchMove(event);
	}

	/**
	 * The context menu item for animating the layouting process.
	 */
	private ContextMenuCheckBoxItem animateMenu;

	/**
	 * The context menu item for rearranging networks through random shifts of node
	 * positions.
	 */
	private ContextMenuItem shakeMenu;

	/**
	 * The context menu item to clear the canvas. Only active for linear graphs to
	 * clear the history.
	 */
	private ContextMenuItem clearMenu;

	/**
	 * The context menu for visually exploring (or debugging) the updating process.
	 */
	private ContextMenu debugSubmenu;

	/**
	 * The context menu item for updating the current node.
	 */
	private ContextMenuItem debugNodeMenu;

	/**
	 * The context menu item for attaching the debug submenu.
	 */
	private ContextMenuItem debugSubmenuTrigger;

	/**
	 * The flag to indicate whether the debug submenu is activated. For example,
	 * debugging does not make sense if the nodes refer to states of PDE
	 * calculations.
	 */
	private boolean isDebugEnabled = true;

	/**
	 * Set whether the debugging menu is enabled.
	 * 
	 * @param enabled {@code true} to enable debugging
	 */
	public void setDebugEnabled(boolean enabled) {
		isDebugEnabled = enabled;
	}

	@Override
	public void populateContextMenuAt(ContextMenu menu, int x, int y) {
		if (hasMessage) {
			// skip or disable context menu entries
			super.populateContextMenuAt(menu, x, y);
			return;
		}
		// process shake context menu
		if (shakeMenu == null) {
			shakeMenu = new ContextMenuItem("Shake", new Command() {
				@Override
				public void execute() {
					network.shake(PopGraph2D.this, 0.05);
				}
			});
		}
		menu.add(shakeMenu);
		Geometry.Type type = geometry.getType();
		switch (type) {
			case HIERARCHY:
				shakeMenu.setEnabled(!geometry.subgeometry.isLattice());
				break;
			// list of graphs that have static layout and hence do not permit shaking
			case LINEAR:
			case SQUARE_NEUMANN:
			case SQUARE_NEUMANN_2ND:
			case SQUARE_MOORE:
			case SQUARE:
			case CUBE:
			case HONEYCOMB:
			case TRIANGULAR:
			case VOID:
			case INVALID:
				shakeMenu.setEnabled(false);
				break;
			default:
				shakeMenu.setEnabled(true);
				break;
		}

		// process animate context menu
		if (animateMenu == null) {
			animateMenu = new ContextMenuCheckBoxItem("Animate layout", new Command() {
				@Override
				public void execute() {
					network.toggleAnimateLayout();
				}
			});
		}
		animateMenu.setChecked(network == null ? false : network.doAnimateLayout());
		menu.add(animateMenu);
		animateMenu.setEnabled(!hasMessage);

		// add menu to clear buffer (applies only to linear geometry) 
		if (type == Geometry.Type.LINEAR) {
			if( clearMenu==null ) {
				clearMenu = new ContextMenuItem("Clear", new Command() {
					@Override
					public void execute() {
						buffer.clear();
						paint();
					}
				});
			}
			menu.add(clearMenu);
		}

		// process debug node update
		if (isDebugEnabled) {
			int debugNode = findNodeAt(x, y);
			if (debugNode >= 0) {
				if (debugSubmenu == null) {
					debugSubmenu = new ContextMenu(menu);
					debugNodeMenu = new ContextMenuItem("Update node @ -", new Command() {
						@Override
						public void execute() {
							((NodeGraphController) controller).updateNodeAt(PopGraph2D.this, debugNode);
						}
					});
					debugSubmenu.add(debugNodeMenu);
				}
				debugNodeMenu.setText("Update node @ " + debugNode);
				debugNodeMenu.setEnabled(((NodeGraphController) controller).isModelType(Model.Type.IBS));
				debugSubmenuTrigger = menu.add("Debug...", debugSubmenu);
			}
			if (debugSubmenuTrigger != null)
				debugSubmenuTrigger.setEnabled(!controller.isRunning());
		}

		super.populateContextMenuAt(menu, x, y);
	}

	@Override
	public void export(MyContext2d ctx) {
		MyContext2d bak = g;
		g = ctx;
		paint();
		g = bak;
	}
}
