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
import org.evoludo.simulator.Geometry;
import org.evoludo.simulator.Network.Status;
import org.evoludo.simulator.Network2D;
import org.evoludo.simulator.modules.Module;
import org.evoludo.util.RingBuffer;

import com.google.gwt.core.client.JsArray;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.dom.client.Touch;
import com.google.gwt.event.dom.client.MouseDownEvent;
import com.google.gwt.event.dom.client.MouseMoveEvent;
import com.google.gwt.event.dom.client.MouseUpEvent;
import com.google.gwt.event.dom.client.TouchEndEvent;
import com.google.gwt.event.dom.client.TouchMoveEvent;

/**
 * The graphical representation of network structures in 2D.
 *
 * @author Christoph Hauert
 */
public class PopGraph2D extends GenericPopGraph<String, Network2D> implements Shifting {

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
	public PopGraph2D(NodeGraphController controller, Module module) {
		super(controller, module);
		setStylePrimaryName("evoludo-PopGraph2D");
		label.setStyleName("evoludo-Label2D");
	}

	@Override
	public void activate() {
		super.activate();
		// lazy allocation of memory for colors
		if (geometry != null && (colors == null || colors.length != geometry.size))
			colors = new String[geometry.size];
	}

	@Override
	public String[] getData() {
		return colors;
	}

	/**
	 * Update the graph.
	 * 
	 * @param isNext {@code true} if the state has changed
	 */
	public void update(boolean isNext) {
		if (buffer != null) {
			// add copy of colors array to buffer
			// note: cannot be reliably done in RingBuffer class without reflection
			String[] copy = Arrays.copyOf(colors, colors.length);
			if (isNext || buffer.isEmpty())
				buffer.append(copy);
			else
				buffer.replace(copy);
		}
		super.update(isNext);
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
	public boolean paint(boolean force) {
		if (super.paint(force))
			return true;
		if (hasStaticLayout())
			layoutLattice();
		else
			drawNetwork();
		return false;
	}

	/**
	 * Invalidate the network. This forces networks to be regenerated.
	 */
	public void invalidate() {
		super.invalidate();
		if (geometry.getType() == Geometry.Type.LINEAR) {
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
			buffer.clear();
			// allocate colors now to store history
			if (colors == null || colors.length != geometry.size)
				colors = new String[geometry.size];
		} else {
			buffer = null;
		}
	}

	@Override
	protected void layoutLattice() {
		super.layoutLattice();
		if (dw < 1 && dh < 1 && dR < 2) {
			displayMessage("Population size to large!");
			return;
		}
		// helper variables
		double xshift, yshift;
		int row;
		Geometry.Type type = geometry.getType();
		if (isHierarchy)
			type = geometry.subgeometry;
		prepCanvas();
		// geometries that have special/fixed layout
		switch (type) {
			case TRIANGULAR:
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
				break;

			case HONEYCOMB:
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
				break;

			case SQUARE_NEUMANN:
			case SQUARE_NEUMANN_2ND:
			case SQUARE_MOORE:
			case SQUARE:
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
				break;

			case LINEAR:
				int nSteps = (int) (bounds.getHeight() / dh);
				yshift = 0;
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
				break;

			default:
				displayMessage("No representation for " + type.getTitle() + "!");
		}
		g.restore();
	}

	/**
	 * Helper method to draw the network.
	 */
	protected synchronized void layoutNetwork() {
		if (network.isStatus(Status.NO_LAYOUT))
			return; // nothing to do (lattice)
		if (!network.isStatus(Status.HAS_LAYOUT) || geometry.isDynamic)
			network.doLayout(this);
		geometry.setNetwork2D(network);
		drawNetwork();
	}

	protected void drawNetwork() {
		if (invalidated)
			// not yet ready to show network
			return;
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
	 * The diameter of nodes for networks.
	 */
	protected int dR;

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

		dw = 0;
		dh = 0;
		dR = 0;
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
				dR = (int) Math.sqrt(radius * radius * 2 / geometry.size);
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

	/**
	 * Get the color of the node at index {@code node} as a CSS color string.
	 * 
	 * @param node the index of the node
	 * @return the color of the node
	 */
	public String getCSSColorAt(int node) {
		return colors[node];
	}

	/**
	 * Find the index of the node at the location with coordinates {@code (x, y)}.
	 * 
	 * @param x the {@code x}-coordinate of the location
	 * @param y the {@code y}-coordinate of the location
	 * @return the index of the node
	 */
	public int findNodeAt(int x, int y) {
		// no network may have been initialized (e.g. for ODE/SDE models)
		if (hasMessage || network == null || invalidated || network.isStatus(Status.LAYOUT_IN_PROGRESS))
			return FINDNODEAT_OUT_OF_BOUNDS;
		int c, r;
		double rr;

		// some heuristic adjustments... cause remains mysterious
		x = x - (int) (style.frameWidth * zoomFactor + 0.5);
		y = y - (int) (style.frameWidth * zoomFactor - 0.5);
		if (!bounds.contains(x, y))
			return FINDNODEAT_OUT_OF_BOUNDS;

		int sx = (int) ((viewCorner.x + x - bounds.getX()) / zoomFactor + 0.5);
		int sy = (int) ((viewCorner.y + y - bounds.getY()) / zoomFactor + 0.5);

		Geometry.Type type = geometry.getType();
		if (isHierarchy)
			type = geometry.subgeometry;
		switch (type) {
			// 3D views must deal with this
			case CUBE:
				return FINDNODEAT_UNIMPLEMENTED;

			case TRIANGULAR:
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
				c = sx / dw;
				r = sy / dh;
				return r * geometry.size + c;

			default:
				// note: cannot check bounds (or anything else) to rule out that mouse hovers
				// over node because nodes may have been manually shifted.
				rr = network.getRadius();
				double iscale = (rr + rr) / bounds.getWidth();
				Point2D mousecoord = new Point2D(sx * iscale - rr, sy * iscale - rr);
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
				drawNetwork();
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

	@Override
	public void export(MyContext2d ctx) {
		MyContext2d bak = g;
		g = ctx;
		paint(true);
		g = bak;
	}
}
