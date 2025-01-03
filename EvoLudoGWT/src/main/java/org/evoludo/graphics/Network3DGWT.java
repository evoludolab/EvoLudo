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

import java.util.ArrayList;

import org.evoludo.geom.Node3D;
import org.evoludo.simulator.ColorMap3D;
import org.evoludo.simulator.EvoLudo;
import org.evoludo.simulator.Geometry;
import org.evoludo.simulator.Network3D;

import com.google.gwt.core.client.Duration;
import com.google.gwt.core.client.Scheduler;

import thothbot.parallax.core.shared.cameras.Camera;
import thothbot.parallax.core.shared.math.Color;
import thothbot.parallax.core.shared.math.Vector3;

/**
 * Graphical representation of generic population geometries in 3D. A network
 * corresponds to a (possibly ephemeral) collection and configuration of nodes.
 * This implementation includes optimizations for GWT when scheduling
 * computationally intensive tasks.
 *
 * @author Christoph Hauert
 */
public class Network3DGWT extends Network3D {

	/**
	 * The {@link Camera} controlling the view of the network. Needed to exchange
	 * the visual perspective of different views. For example the 3D view of
	 * strategies and of payoffs should use the same perspective.
	 */
	Camera worldView;

	/**
	 * The links in the network.
	 */
	thothbot.parallax.core.shared.core.Geometry links;

	/**
	 * Create a new network in 3D for the given engine and geometry with a layouting
	 * process optimzed for GWT.
	 * 
	 * @param engine   the pacemaker for running the model
	 * @param geometry the structure of the population
	 */
	public Network3DGWT(EvoLudo engine, Geometry geometry) {
		super(engine, geometry);
	}

	@Override
	public void reset() {
		if (isStatus(Status.LAYOUT_IN_PROGRESS))
			return;
		worldView = null;
		links = null;
		super.reset();
	}

	/**
	 * Get the visual perspective of this network. This can be manipulated by the
	 * GUI but must be synchronized between different views (e.g. strategies versus
	 * payoffs) to avoid confusing the user.
	 * 
	 * @return the perspective of the network
	 */
	public Camera getWorldView() {
		return worldView;
	}

	/**
	 * Set the visual perspective of this network. This can be manipulated by the
	 * GUI but must be synchronized between different views (e.g. strategies versus
	 * payoffs) to avoid confusing the user.
	 * 
	 * @param worldView the new visual perspective
	 */
	public void setWorldView(Camera worldView) {
		this.worldView = worldView;
	}

	/**
	 * Get the links of this 3D network representation.
	 * 
	 * @return the links of this network
	 */
	public thothbot.parallax.core.shared.core.Geometry getLinks() {
		return links;
	}

	@Override
	public void linkNodes() {
		if (nLinks <= 0 || fLinks <= 0.0) {
			return;
		}
		if (nLinks > MAX_LINK_COUNT) {
			engine.getLogger().warning("Too many links to draw - skipping!");
			return;
		}
		ArrayList<Vector3> lines;
		ArrayList<Color> colors = null;
		if (geometry.isUndirected) {
			// draw undirected links
			if (fLinks >= 1.0) {
				// draw all links
				lines = new ArrayList<>(nLinks + nLinks);
				for (int i = 0; i < nNodes; i++) {
					int[] neighs = geometry.out[i];
					int nn = geometry.kout[i];
					Node3D fp = nodes[i];
					Vector3 focal = new Vector3(fp.x, fp.y, fp.z);
					for (int j = 0; j < nn; j++) {
						int k = neighs[j];
						// check if link was already drawn
						if (k < i)
							continue;
						lines.add(focal);
						Node3D np = nodes[k];
						lines.add(new Vector3(np.x, np.y, np.z));
					}
				}
			} else {
				// draw only fraction of undirected links
				// this is pretty memory intensive - hopefully it works...
				int[] idxs = new int[nLinks];
				for (int n = 0; n < nLinks; n++)
					idxs[n] = n;
				int toDraw = (int) (fLinks * nLinks);
				lines = new ArrayList<>(toDraw + toDraw);
				for (int l = 0; l < toDraw; l++) {
					int idxsidx = rng.random0n(nLinks - l);
					int nodeidx = idxs[idxsidx];
					idxs[idxsidx] = idxs[nLinks - l - 1];
					// find node
					int a = -1;
					for (int n = 0; n < nNodes; n++) {
						int k = geometry.kout[n];
						if (nodeidx < k) {
							a = n;
							break;
						}
						nodeidx -= k;
					}
					Node3D ap = nodes[a];
					lines.add(new Vector3(ap.x, ap.y, ap.z));
					Node3D bp = nodes[geometry.out[a][nodeidx]];
					lines.add(new Vector3(bp.x, bp.y, bp.z));
				}
			}
		} else {
			// draw directed links
			if (fLinks >= 1.0) {
				// draw all links
				lines = new ArrayList<>(nLinks + nLinks);
				colors = new ArrayList<>(nLinks + nLinks);
				for (int i = 0; i < nNodes; i++) {
					int[] neighs = geometry.out[i];
					int nn = geometry.kout[i];
					Node3D fp = nodes[i];
					Vector3 focal = new Vector3(fp.x, fp.y, fp.z);
					for (int j = 0; j < nn; j++) {
						int k = neighs[j];
						if (geometry.isNeighborOf(k, i)) {
							// undirected link - check if already drawn
							if (k < i)
								continue;
							// draw link in black
							lines.add(focal);
							Node3D np = nodes[k];
							lines.add(new Vector3(np.x, np.y, np.z));
							colors.add(ColorMap3D.UNDIRECTED);
							colors.add(ColorMap3D.UNDIRECTED);
							continue;
						}
						// directed link - draw link with bright tip
						lines.add(focal);
						Node3D np = nodes[k];
						lines.add(new Vector3(np.x, np.y, np.z));
						colors.add(ColorMap3D.DIRECTED_SRC);
						colors.add(ColorMap3D.DIRECTED_DST);
					}
				}
			} else {
				// draw only fraction of undirected links
				// this is pretty memory intensive - hopefully it works...
				int[] idxs = new int[nLinks];
				for (int n = 0; n < nLinks; n++)
					idxs[n] = n;
				int toDraw = (int) (fLinks * nLinks);
				lines = new ArrayList<>(toDraw + toDraw);
				colors = new ArrayList<>(toDraw + toDraw);
				for (int l = 0; l < toDraw; l++) {
					int idxsidx = rng.random0n(nLinks - l);
					int nodeidx = idxs[idxsidx];
					idxs[idxsidx] = idxs[nLinks - l - 1];
					// find node
					int a = -1;
					for (int n = 0; n < nNodes; n++) {
						int k = geometry.kout[n];
						if (nodeidx < k) {
							a = n;
							break;
						}
						nodeidx -= k;
					}
					// draw all links as directed ones
					Node3D ap = nodes[a];
					lines.add(new Vector3(ap.x, ap.y, ap.z));
					Node3D bp = nodes[geometry.out[a][nodeidx]];
					lines.add(new Vector3(bp.z, bp.y, bp.z));
					colors.add(ColorMap3D.DIRECTED_SRC);
					colors.add(ColorMap3D.DIRECTED_DST);
				}
			}
		}
		links = new thothbot.parallax.core.shared.core.Geometry();
		links.setVertices(lines);
		if (colors != null)
			links.setColors(colors);
	}

	/**
	 * Helper variable to keep track of the time spent layouting the network.
	 */
	Duration layout;

	/**
	 * Time when last layout update was processed.
	 */
	int prevLayout;

	/**
	 * Minimum delay in milliseconds between subsequent updates of the layout in the
	 * GUI. This is used to throttle the animated layout process to a default of at
	 * most 20 updates per second.
	 */
	protected final static int MIN_DELAY_ANIMATE_MSEC = 50;

	@Override
	public void doLayout(LayoutListener ll) {
		if (isRunning)
			// layouting process already running
			return;
		Status stat = getStatus();
		if (stat == Status.HAS_LAYOUT || stat == Status.NO_LAYOUT || stat == Status.HAS_MESSAGE)
			return;
		this.listener = ll;
		doLayoutPrep();
		layout = new Duration();
		prevLayout = Integer.MIN_VALUE;
		Scheduler.get().scheduleFixedDelay(new Scheduler.RepeatingCommand() {
			@Override
			public boolean execute() {
				return doLayoutStep();
			}
		}, 5);
	}

	/**
	 * Helper variable to store the index of the node that needs to be processed
	 * next. This is to prevent the GUI from becoming unresponsive and allow the
	 * layouting process to resume after relaxing a fixed number of nodes.
	 * 
	 * @see #MAX_LINKS_PER_STEP
	 */
	protected int nextLayoutNode = 0;

	/**
	 * The minimum number of links to process in one step. After
	 * {@code MAX_LINKS_PER_STEP} links have been processed the next node to
	 * continue is stored in {@link #nextLayoutNode} and control is returned to the
	 * event loop, which prevents the GUI from becoming unresponsive. As a welcome
	 * side effect, this parcelling resulted in a dramatic speed increase.
	 */
	protected static final int MAX_LINKS_PER_STEP = 200;

	/**
	 * Perform a single step in the layouting process by relaxing nodes
	 * sequentially. Returns {@code false} once the layouting process has completed,
	 * i.e. reached the desired accuracy or the time out.
	 * 
	 * @return {@code true} if the layouting process should continue
	 */
	protected boolean doLayoutStep() {
		int nLinksDone = 0;
		for (int n = nextLayoutNode; n < nNodes; n++) {
			potential += relax(n);
			nLinksDone += geometry.kout[n];
			if (nLinksDone > MAX_LINKS_PER_STEP) {
				nextLayoutNode = n + 1;
				return isRunning;
			}
		}
		nextLayoutNode = 0;

		potential *= norm;
		double adjust = Math.abs(potential - prevPotential);
		prevPotential = potential;
		potential = 0.0;
		prevAdjust = Math.min(prevAdjust, adjust);

		int elapsed = layout.elapsedMillis();
		if (adjust < accuracy || elapsed > layoutTimeout) { // layoutTimeout provides emergency exit
			finishLayout();
			setStatus(Status.HAS_LAYOUT);
			isRunning = false;
			timestamp = engine.getModel().getTime();
			listener.layoutComplete();
			return false;
		}
		if (elapsed - prevLayout > MIN_DELAY_ANIMATE_MSEC) {
			listener.layoutUpdate(accuracy / prevAdjust);
			prevLayout = layout.elapsedMillis();
		}
		return isRunning;
	}
}
