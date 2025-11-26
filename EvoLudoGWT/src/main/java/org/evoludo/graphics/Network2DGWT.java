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

import org.evoludo.simulator.EvoLudo;
import org.evoludo.simulator.geom.AbstractGeometry;
import org.evoludo.simulator.Network2D;
import org.evoludo.simulator.models.Model;

import com.google.gwt.core.client.Duration;
import com.google.gwt.core.client.Scheduler;

/**
 * Graphical representation of generic population geometries in 2D. A network
 * corresponds to a (possibly ephemeral) collection and configuration of nodes.
 * This implementation includes optimizations for GWT when scheduling
 * computationally intensive tasks.
 *
 * @author Christoph Hauert
 */
public class Network2DGWT extends Network2D {

	/**
	 * Create a new network in 2D for the given engine and geometry with a layouting
	 * process optimzed for GWT.
	 * 
	 * @param engine   the pacemaker for running the model
	 * @param geometry the structure of the population
	 */
	public Network2DGWT(EvoLudo engine, AbstractGeometry geometry) {
		super(engine, geometry);
	}

	@Override
	public void reset() {
		if (isStatus(Status.LAYOUT_IN_PROGRESS))
			return;
		super.reset();
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
	protected static final int MIN_DELAY_ANIMATE_MSEC = 50;

	@Override
	public void doLayout(LayoutListener ll) {
		Status stat = getStatus();
		if (stat != Status.NEEDS_LAYOUT && stat != Status.ADJUST_LAYOUT)
			return;
		this.listener = ll;
		doLayoutPrep();
		layout = new Duration();
		prevLayout = Integer.MIN_VALUE;
		Scheduler.get().scheduleIncremental(this::doLayoutStep);
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
			// in the unlikely event that unloaded during layouting the model may be null
			Model model = engine.getModel();
			if (model == null)
				return false;
			timestamp = model.getUpdates();
			listener.layoutComplete();
			return false;
		}
		if (elapsed - prevLayout > MIN_DELAY_ANIMATE_MSEC) {
			listener.layoutUpdate(accuracy / prevAdjust);
			prevLayout = layout.elapsedMillis();
		}
		return isRunning;
	}

	@Override
	public boolean equals(Object obj) {
		return super.equals(obj);
	}

	@Override
	public int hashCode() {
		return super.hashCode();
	}

}
