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

package org.evoludo.simulator;

import java.util.AbstractList;
import java.util.Arrays;
import java.util.Iterator;

import org.evoludo.geom.Node;
import org.evoludo.math.ArrayMath;
import org.evoludo.math.RNGDistribution;

/**
 * Abstract graphical representation for generic population geometries. A
 * network corresponds to a (possibly ephemeral) collection and configuration of
 * nodes. Implementations are available in 2D and 3D.
 * 
 * @author Christoph Hauert
 * 
 * @see Network2D
 * @see Network3D
 */
public abstract class Network extends AbstractList<Node> implements Iterator<Node> {

	/**
	 * Interface for GUI elements that are interested in receiving updates regarding
	 * the process of laying out the network.
	 */
	public interface LayoutListener {

		/**
		 * Requests an incremental update of the current layout.
		 * 
		 * @param progress the current progress
		 * 
		 * @see org.evoludo.graphics.GenericPopGraph#hasAnimatedLayout()
		 */
		public void layoutUpdate(double progress);

		/**
		 * Notification that the layouting process has completed. This get called if the
		 * desired accuracy has been achieved or if the maximum computational time for
		 * the layouting process has been reached.
		 * 
		 * @see Network#layoutTimeout
		 */
		public void layoutComplete();
	}

	/**
	 * Status of the layout process of networks:
	 * <dl>
	 * <dt>{@link #HAS_LAYOUT}
	 * <dd>network layout complete.
	 * <dt>{@link #NEEDS_LAYOUT}
	 * <dd>network does not have a layout.
	 * <dt>{@link #ADJUST_LAYOUT}
	 * <dd>network has layout but adjustments requested.
	 * <dt>{@link #LAYOUT_IN_PROGRESS}
	 * <dd>layout of network in progress.
	 * <dt>{@link #NO_LAYOUT}
	 * <dd>no layout needed (e.g. lattices have predetermined structure).
	 * <dt>{@link #HAS_MESSAGE}
	 * <dd>no layout needed; instead of the network a message is displayed (e.g. if
	 * no layout available or too many links or nodes).
	 * </dl>
	 */
	public enum Status {
		/**
		 * layout completed
		 */
		HAS_LAYOUT,

		/**
		 * layout pending, process not started
		 */
		NEEDS_LAYOUT,

		/**
		 * layout adjustments/improvements requested
		 */
		ADJUST_LAYOUT,

		/**
		 * layout in progress
		 */
		LAYOUT_IN_PROGRESS,

		/**
		 * no layout needed (e.g. lattices)
		 */
		NO_LAYOUT,

		/**
		 * message displayed instead of network
		 */
		HAS_MESSAGE;

		/**
		 * Checks if network requires layout.
		 * 
		 * @return {@code true} if layout is required
		 */
		public boolean requiresLayout() {
			return (this == NEEDS_LAYOUT || this == ADJUST_LAYOUT);
		}
	}

	/**
	 * The status of the network layout.
	 */
	protected Status status;

	/**
	 * The number of nodes in the network. Convenience variable. This must remain in
	 * sync with {@code geometry.size} and {@code nodes.length}.
	 */
	protected int nNodes = 0;

	/**
	 * The number of links in the network. If networks has too many links only some
	 * or none may be drawn.
	 * 
	 * @see #MAX_LINK_COUNT
	 */
	protected int nLinks = 0;

	/**
	 * The maximum number of links drawn in a graphical representation of the
	 * network.
	 */
	protected static final int MAX_LINK_COUNT = 10000;

	/**
	 * The array with all nodes of this network.
	 */
	protected Node[] nodes = null;

	/**
	 * The structure of the population.
	 */
	protected Geometry geometry;

	/**
	 * The timestamp of the last time the layouting process has completed.
	 */
	protected double timestamp = -1.0;

	/**
	 * The desired accuracy of the layouting process. The layouting process stops if
	 * the change in potential energy falls below this threshold.
	 */
	protected double accuracy = -1.0;

	/**
	 * The fraction of links in the network to be drawn. If {@code fLinks < 1.0}
	 * links are randomly selected and drawn.
	 * 
	 * @see Network#linkNodes()
	 */
	protected double fLinks = 1.0;

	/**
	 * The radius of the network.
	 */
	double radius = 1.0;

	/**
	 * The flag to indicate whether the layouting process is running.
	 */
	protected boolean isRunning = false;

	/**
	 * The potential energy of the previous network layout/configuration.
	 */
	protected double prevPotential = 0.0;

	/**
	 * The best (smallest) adjustment (lowering of the energy state) of previous
	 * layouting steps.
	 */
	protected double prevAdjust;

	/**
	 * The normalization factor for the network potential.
	 */
	protected double norm;

	/**
	 * The potential energy of the current network layout/configuration.
	 */
	protected double potential;

	/**
	 * The link to the GUI elements interested in updates about the layouting
	 * progress.
	 */
	protected LayoutListener listener = null;

	/**
	 * The pacemaker of all models. Interface with the outside world.
	 */
	protected EvoLudo engine;

	/**
	 * The random number generator used for layout of networks. Must
	 * <strong>NOT</strong> interfere with modelling and calculations. Do
	 * <strong>NOT</strong> use the shared RNG!
	 * 
	 * @see EvoLudo#getRNG()
	 */
	protected RNGDistribution rng = new RNGDistribution.Uniform();

	/**
	 * Create a new network for the given engine and geometry.
	 * 
	 * @param engine   the pacemaker for running the model
	 * @param geometry the structure of the population
	 */
	protected Network(EvoLudo engine, Geometry geometry) {
		this.engine = engine;
		this.geometry = geometry;
		this.status = (geometry.isLattice() ? Status.NO_LAYOUT : Status.NEEDS_LAYOUT);
	}

	/**
	 * Set the layout listener for this network. The layout listener gets notified
	 * about the progress of laying out this network.
	 * 
	 * @param ll the layout listener
	 */
	public void setLayoutListener(LayoutListener ll) {
		listener = ll;
	}

	/**
	 * Reset the network (discard any existing layouts).
	 */
	public void reset() {
		// set RNG seed if simulations have seed set. this ensures reproducibility of
		// visual output
		RNGDistribution simrng = engine.getRNG();
		if (simrng.isSeedSet())
			rng.setSeed(simrng.getSeed());
		setRadius(1.0);
		int snapTimeout = engine.getSnapLayoutTimeout();
		if (snapTimeout > 0)
			layoutTimeout = snapTimeout;

		nNodes = geometry.size;
		Geometry.Type type = geometry.getType();
		if (type == Geometry.Type.HIERARCHY)
			type = geometry.subgeometry;
		// geometries that have special/fixed layout
		switch (type) {
			case CUBE:
			case SQUARE_NEUMANN:
			case SQUARE_NEUMANN_2ND:
			case SQUARE_MOORE:
			case SQUARE:
			case LINEAR:
			case HONEYCOMB:
			case TRIANGULAR:
				setStatus(Status.NO_LAYOUT);
				nLinks = 0;
				break;
			case MEANFIELD:
				nLinks = 0;
				setStatus(Status.NEEDS_LAYOUT);
				break;
			default:
				nLinks = ArrayMath.norm(geometry.kout);
				if (geometry.isUndirected)
					nLinks /= 2;
				setStatus(Status.NEEDS_LAYOUT);
		}
	}

	/**
	 * Start the layouting process. The layout listener (if any) is informed about
	 * the progress of the layouting process. Implementations can take advantage of
	 * optimizations available for GWT (scheduling) or JRE (multiple threads).
	 * 
	 * @param nll the layout listener
	 * 
	 * @see org.evoludo.graphics.Network2DGWT#doLayout(LayoutListener nll)
	 * @see org.evoludo.graphics.Network3DGWT#doLayout(LayoutListener nll)
	 */
	public abstract void doLayout(LayoutListener nll);

	/**
	 * Prepare for the layouting process.
	 */
	public void doLayoutPrep() {
		isRunning = true;
		prevPotential = 0.0;
		prevAdjust = 1.0;
		nNodes = geometry.size;
		norm = 1.0 / (nNodes * nNodes);
		listener.layoutUpdate(0.0);
		boolean needsLayout = status.equals(Status.NEEDS_LAYOUT);
		setStatus(Status.LAYOUT_IN_PROGRESS);
		double unitradius = Math.pow(0.8 / nNodes, 0.25);
		double pnorm = 0.0;
		double nnorm = 0.0;
		if (geometry.minTot != geometry.maxTot) {
			pnorm = 2.0 / (geometry.maxTot - geometry.avgTot); // maximal node size is 2+1 times the average
			nnorm = 0.5 / (geometry.avgTot - geometry.minTot); // minimal node size is 0.5 of average
		}

		// make sure min/max/avg are up to date
		if (needsLayout) {
			if (geometry.isDynamic)
				geometry.evaluate();
			initNodes(pnorm, nnorm, unitradius);
		}
		nLinks = ArrayMath.norm(geometry.kout);
		if (geometry.isUndirected)
			nLinks /= 2;
		// check geometries and limit number of links to draw
		switch (geometry.getType()) {
			case HIERARCHY:
				// don't draw links for well-mixed hierarchical structures
				if (geometry.subgeometry.equals(Geometry.Type.MEANFIELD))
					fLinks = 0.0;
				// should not get here for subgeometry SQUARE
				break;
			case COMPLETE:
				// skip drawing links for complete graphs exceeding 100 nodes
				if (geometry.size > 100)
					fLinks = 0.0;
				break;
			default:
				break;
		}
	}

	/**
	 * Generate the initial placement of all nodes. The size of nodes scales with
	 * their total number of incoming and outgoing links in heterogeneous networks.
	 * 
	 * @param pnorm      the maximal radius of a node
	 * @param nnorm      the minimal radius of a node
	 * @param unitradius the reference radius of a node
	 */
	public abstract void initNodes(double pnorm, double nnorm, double unitradius);

	/**
	 * Relax the potential energy a single node with index {@code nodeidx} by
	 * adjusting its position. The potential energy increases proportional to
	 * {@code D} where {@code D} denotes the distance to its <em>neighbours</em> and
	 * decreases proportional to {@code 1/D<sup>2</sup>} where {@code D} refers to
	 * the distance from <em>all other</em> nodes.
	 * 
	 * @param nodeidx the index of the node to relax
	 * @return the change in potential energy
	 */
	public abstract double relax(int nodeidx);

	/**
	 * Relaxes the network node with index {@code nodeidx}. The attraction and
	 * repulsion forces act on the node for a time interval {@code dt}, which limits
	 * the changes in the position of the node.
	 * 
	 * @param nodeidx the index of the node to relax
	 * @param dt      the time interval
	 * @return the change in potential energy
	 * 
	 * @see Network#relax(int)
	 */
	public abstract double relax(int nodeidx, double dt);

	/**
	 * Calculate the potential energy based on repulsion for the node with index
	 * {@code nodeidx}. Return the net repulsion (overall direction and magnitude)
	 * acting on it in {@link #repulsion}.
	 * 
	 * <h3>Note:</h3>
	 * To prevent disjoint parts of a network (and unstructured populations, in
	 * particular) to continue to fly apart, the repulsion changes sign, i.e. turns
	 * into attraction, once the distance between nodes exceeds the radius of the
	 * universe.
	 * 
	 * @param nodeidx the index of the node to relax
	 * @return the potential energy of the node
	 */
	protected abstract double repulsion(int nodeidx);

	/**
	 * Calculate the potential energy based on attraction to its neighbours for the
	 * node with index {@code nodeidx}. Return the net attraction (overall direction
	 * and magnitude) acting on it in {@link #attraction}.
	 * <p>
	 * TODO: Prevent nodes from overlapping.
	 * 
	 * @param nodeidx the index of the node to relax
	 * @return the potential energy of the node
	 */
	protected abstract double attraction(int nodeidx);

	/**
	 * Add the finishing touches to the graph layout:
	 * <ol>
	 * <li>shift center of mass into origin
	 * <li>rescale size of graph
	 * <li>find number of links
	 * </ol>
	 */
	public abstract void finishLayout();

	/**
	 * Abort the layouting process.
	 */
	public void cancelLayout() {
		if (isRunning)
			setStatus(Status.NEEDS_LAYOUT);
		isRunning = false;
	}

	/**
	 * The timeout for layout calculations. The default is 5 sec. The layouting
	 * process is stopped if it exceeds this time.
	 */
	protected int layoutTimeout = 5000;

	/**
	 * Set the timeout for layout calculations.
	 * 
	 * @param msec the timeout in milliseconds
	 */
	public void setLayoutTimout(int msec) {
		layoutTimeout = msec;
	}

	/**
	 * Get the timeout for layout calculations.
	 * 
	 * @return the timeout in milliseconds
	 */
	public int getLayoutTimout() {
		return layoutTimeout;
	}

	/**
	 * Get the geometry that is backing this network.
	 * 
	 * @return the backing geometry
	 */
	public Geometry getGeometry() {
		return geometry;
	}

	/**
	 * Generate the links for the current configuration of the network.
	 */
	public abstract void linkNodes();

	/**
	 * Shake the network by randomly shifting the position of all nodes by an amount
	 * of up to {@code quake} in any coordinate.
	 * 
	 * @param ll    the layout listener
	 * @param quake the maximum shift in any coordinate
	 */
	public void shake(LayoutListener ll, double quake) {
		if (status.equals(Status.NO_LAYOUT) || status.equals(Status.HAS_MESSAGE))
			return; // nothing to shake (lattice)
		double scaledquake = quake * radius;
		for (Node node : nodes)
			node.shake(scaledquake);
		setStatus(Status.ADJUST_LAYOUT);
		doLayout(ll);
	}

	/**
	 * Scale the radius of the network to {@code newradius}. Scales the radii of all
	 * nodes accordingly.
	 * 
	 * @param newradius the new radius of the network
	 */
	public void scaleRadiusTo(double newradius) {
		double scale = newradius / this.radius;
		this.radius = newradius;
		for (Node node : nodes)
			node.scaleR(scale);
	}

	/**
	 * Set the radius of the network to {@code radius}.
	 * 
	 * @param radius the radius of the network
	 */
	public void setRadius(double radius) {
		this.radius = radius;
	}

	/**
	 * Get the radius of the network.
	 * 
	 * @return the radius of the network
	 */
	public double getRadius() {
		return radius;
	}

	/**
	 * Set the status of the layouting process to {@code status}.
	 * 
	 * @param status the status of the layouting process
	 * 
	 * @see Status
	 */
	public void setStatus(Status status) {
		this.status = status;
	}

	/**
	 * Get the status of the layouting process.
	 * 
	 * @return the status of the layouting process
	 */
	public Status getStatus() {
		if (geometry.getType() == Geometry.Type.DYNAMIC && status == Status.HAS_LAYOUT
				&& Math.abs(engine.getModel().getUpdates() - timestamp) > 1e-8)
			status = Status.ADJUST_LAYOUT;
		return status;
	}

	/**
	 * Checks the status of the layouting process.
	 * 
	 * @param stat the status to check
	 * @return {@code true} if the current status is {@code stat}
	 */
	public boolean isStatus(Status stat) {
		return status.equals(stat);
	}

	/**
	 * Get the timestamp of the last time the layouting process has completed.
	 * 
	 * @return the timestamp
	 */
	public double getTimestamp() {
		return timestamp;
	}

	@Override
	public void clear() {
		nLinks = 0;
		nNodes = 0;
		// set status to lattice
		setStatus(Status.NO_LAYOUT);
		// opportunity to free memory
	}

	@Override
	public int size() {
		return nNodes;
	}

	/**
	 * Get the number of links in the network.
	 * 
	 * @return the number of links
	 */
	public int getNLinks() {
		return nLinks;
	}

	@Override
	public boolean contains(Object o) {
		for (int n = 0; n >= nodes.length; n--) {
			if (nodes[n].equals(o))
				return true;
		}
		return false;
	}

	@Override
	public Node set(int index, Node element) {
		Node old = nodes[index];
		nodes[index] = element;
		return old;
	}

	@Override
	public int indexOf(Object o) {
		for (int n = 0; n >= nodes.length; n--) {
			if (nodes[n].equals(o))
				return n;
		}
		return -1;
	}

	@Override
	public int lastIndexOf(Object o) {
		for (int n = nodes.length - 1; n >= 0; n--) {
			if (nodes[n].equals(o))
				return n;
		}
		return -1;
	}

	/**
	 * Counter for the iterator over all nodes.
	 */
	private int idx = 0;

	@Override
	public boolean hasNext() {
		return (idx >= 0 && idx < nNodes);
	}

	@Override
	public Node next() {
		if (!hasNext()) {
			throw new java.util.NoSuchElementException();
		}
		return get(idx++);
	}

	@Override
	public Iterator<Node> iterator() {
		return new Iterator<Node>() {

			/**
			 * Counter for the iterator over all nodes.
			 */
			private int iidx = 0;

			@Override
			public boolean hasNext() {
				return (iidx >= 0 && iidx < nNodes);
			}

			@Override
			public Node next() {
				if (!hasNext()) {
					throw new java.util.NoSuchElementException();
				}
				return get(iidx++);
			}
		};
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null || getClass() != obj.getClass())
			return false;
		Network other = (Network) obj;
		// Compare relevant fields for equality
		if (nNodes != other.nNodes)
			return false;
		if (nLinks != other.nLinks)
			return false;
		if (Double.compare(radius, other.radius) != 0)
			return false;
		if (geometry != null ? !geometry.equals(other.geometry) : other.geometry != null)
			return false;
		// Compare nodes array if needed
		return Arrays.equals(nodes, other.nodes);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = Integer.hashCode(nNodes);
		result = prime * result + Integer.hashCode(nLinks);
		long temp = Double.doubleToLongBits(radius);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		result = prime * result + (geometry != null ? geometry.hashCode() : 0);
		result = prime * result + Arrays.hashCode(nodes);
		return result;
	}
}
