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

import org.evoludo.geom.Node2D;
import org.evoludo.geom.Path2D;
import org.evoludo.geom.Point2D;
import org.evoludo.geom.Vector2D;

/**
 * Graphical representation of generic population geometries in 2D. A network
 * corresponds to a (possibly ephemeral) collection and configuration of nodes.
 *
 * @author Christoph Hauert
 */
public abstract class Network2D extends Network {

	/**
	 * The links in this network.
	 */
	protected Path2D links = new Path2D();

	/**
	 * The array with all nodes of this network. This deliberately overrides
	 * {@code super.nodes}. The two arrays are identical but saves a ton of
	 * unnecesary casts.
	 */
	@SuppressWarnings("hiding")
	protected Node2D[] nodes = null;

	/**
	 * Create a new network in 2D for the given engine and geometry.
	 * 
	 * @param engine   the pacemaker for running the model
	 * @param geometry the structure of the population
	 */
	public Network2D(EvoLudo engine, Geometry geometry) {
		// network is shared between different graphs - cannot set listener here!
		super(engine, geometry);
	}

	@Override
	public void reset() {
		super.reset();
		setAccuracy(1e-4);
	}

	/**
	 * Get the links in this network for the GUI to draw.
	 * 
	 * @return the links in this network
	 */
	public Path2D getLinks() {
		return links;
	}

	@Override
	public void initNodes(double pnorm, double nnorm, double unitradius) {
		if (nodes == null || nodes.length != nNodes) {
			nodes = new Node2D[nNodes];
			super.nodes = this.nodes;
			for (int k = 0; k < nNodes; k++)
				nodes[k] = new Node2D();
		}
		int kin = geometry.kin[0];
		int kout = geometry.kout[0];
		double diff = kout + kin - geometry.avgTot;
		double myr = unitradius * (1.0 + diff * (diff > 0.0 ? pnorm : nnorm));
		nodes[0].set(0.0, 0.0, myr);
		double scaledquake = 0.05 * radius;
		double angle = 0.0;
		double dangle = 2.0 * Math.PI / nNodes;
		for (int k = 1; k < nNodes; k++) {
			kin = geometry.kin[k];
			kout = geometry.kout[k];
			Node2D node = nodes[k];
			diff = kout + kin - geometry.avgTot;
			myr = unitradius * (1.0 + diff * (diff > 0.0 ? pnorm : nnorm));
			double perturb = scaledquake * (rng.random01() - 0.5);
			node.set(Math.sin(angle) + perturb, Math.cos(angle) + perturb, myr);
			angle += dangle;
		}
	}

	/**
	 * Helper variable to store intermediate results when considering the potential
	 * energy resulting from the attraction between neighbouring nodes.
	 */
	private final Vector2D attraction = new Vector2D();

	/**
	 * Helper variable to store intermediate results when considering the potential
	 * energy resulting from the repulsion between nodes.
	 */
	private final Vector2D repulsion = new Vector2D();

	/**
	 * Temporary storage for the directional vector connecting two nodes.
	 */
	private final Vector2D vec = new Vector2D();

	/**
	 * The baseline size of the 2D universe.
	 */
	public final static double R = 10.0;

	/**
	 * The inverse size of the baseline 2D universe. Convenience constant.
	 */
	protected final static double IR = 1.0 / R;

	/**
	 * The inverse squared size of the baseline 2D universe,
	 * {@code 1/R<sup>2</sup>}. Convenience constant.
	 */
	protected final static double IR2 = IR * IR;

	@Override
	public double relax(int nodeidx) {
		return relax(nodeidx, 0.25);
	}

	@Override
	public double relax(int nodeidx, double dt) {
		double energy = repulsion(nodeidx);
		energy += attraction(nodeidx);
		// save current position and energies
		// lastPos.set(pos);
		repulsion.scale(1.0 / geometry.size);
		repulsion.add(attraction);
		// double delta = Math.min(repulsion.length(), R*0.01);
		double delta = Math.min(dt / repulsion.length(), dt);
		// double delta = Math.min(R*0.05/repulsion.length(), R*0.05);
		repulsion.scale(delta);
		// check if energy is reduced by shift
		nodes[nodeidx].shift(repulsion);
		// double eTrial = potential();
		// if( eTrial<eNow || rng.random01()<Math.exp((eNow-eTrial)*dt) )
		// return eTrial;
		// // reject
		// pos.set(lastPos);
		return energy;
	}

	@Override
	protected double repulsion(int nodeidx) {
		repulsion.set(0.0, 0.0);
		double npot = 0.0;
		Node2D node = nodes[nodeidx];
		for (int i = 0; i < nNodes; i++) {
			if (i == nodeidx)
				continue;
			Node2D nodei = nodes[i];
			vec.set(nodei, node);
			// ensure positive distance to avoid divisions by zero
			// note: any merit in special treatment of overlapping spheres?
			// for reasonably sized circles the layout process gets worse if distance is
			// adjusted.
			// add charges to reduce chances of overlap? (see below - challenge to find
			// proper scale)
			// double dist = Math.max(0.0001, (vec.length()-0.5*(radius+node.radius))*IR);
			double dist = Math.max(0.0001, vec.length() * IR);
			// double dist = Math.max(0.0001,
			// vec.length()*radius*radius*node.radius*node.radius*R*R);
			// alt double dist = (vec.length()-node.radius-radius)*IR;
			// dist = Math.signum(dist)*Math.max(0.0001, Math.abs(dist));
			// force becomes attractive if (scaled) distance exceeds 1 to prevent
			// disconnected graphs from flying apart.
			// zero is set at distance 1. assumes equal charges of 1.
			// XXX what is -dist term? potential should depend on charge, i.e. on size of
			// node
			npot -= 2.0 - 1.0 / dist - dist;
			// ok potential -= 2.0-1.0/dist;
			// potential -= 2.0-node.radius*radius*R*R/dist;
			// alt potential -= 2.0-1.0/(dist*node.radius*radius);
			vec.scale(1.0 / (dist * dist) - 1.0);
			// double charge = radius*node.radius;
			// vec.scale(charge/(dist*dist)-charge);
			// large nodes have larger charge - does not work properly
			// vec.scale(radius/dist2-0.25);
			// small ones have larger charge
			// vec.scale(1.0/((radius+node.radius)*dist2)-0.25);
			repulsion.add(vec);
		}
		return npot;
	}

	@Override
	protected double attraction(int nodeidx) {
		attraction.set(0.0, 0.0);
		double npot = 0.0;
		Node2D node = nodes[nodeidx];
		int nOut = geometry.kout[nodeidx];
		int[] neighs = geometry.out[nodeidx];
		for (int i = 0; i < nOut; i++) {
			Node2D nodei = nodes[neighs[i]];
			vec.set(node, nodei);
			// force increases linearly with distance - correct for size of spheres (if
			// spheres overlap attraction is negative).
			// note: for reasonably sized circles the layout process gets worse if distance
			// is adjusted.
			// double dist = vec.length();
			// double distadj = dist-0.5*(radius+node.radius);
			// vec.scale(distadj/dist);
			// potential += distadj>0?distadj*distadj*IR2:-distadj*distadj*IR2;
			npot += vec.length2() * IR2;
			attraction.add(vec);
		}
		if (geometry.isUndirected) {
			if (nOut == 0)
				return 0.0;
			attraction.scale(1.0 / nOut);
			return npot;
		}
		// note: in directed networks, undirected links are counted twice
		int nIn = geometry.kin[nodeidx];
		neighs = geometry.in[nodeidx];
		for (int i = 0; i < nIn; i++) {
			Node2D nodei = nodes[neighs[i]];
			vec.set(node, nodei);
			// force increases linearly with distance - correct for size of spheres (if
			// spheres overlap attraction is negative).
			// note: (same as above) for reasonably sized circles the layout process gets
			// worse if distance is adjusted.
			// double dist = vec.length();
			// double distadj = dist-0.5*(radius+node.radius);
			// vec.scale(distadj/dist);
			// potential += distadj>0?distadj*distadj*IR2:-distadj*distadj*IR2;
			npot += vec.length2() * IR2;
			attraction.add(vec);
		}
		int nTot = nOut + nIn;
		if (nTot == 0)
			return 0.0;
		attraction.scale(1.0 / nTot);
		return npot;
	}

	@Override
	public void finishLayout() {
		if (geometry.isDynamic) {
			// on dynamic networks radius needs to be set as well
			// the radius of the nodes is scaled by their degree
			int kin, kout;
			double unitradius = Math.pow(0.8 / nNodes, 0.25);
			double pnorm = 0.0;
			double nnorm = 0.0;
			if (geometry.minTot != geometry.maxTot) {
				pnorm = 2.0 / (geometry.maxTot - geometry.avgTot); // maximal node size is 2+1 times the average
				nnorm = 0.5 / (geometry.avgTot - geometry.minTot); // minimal node size is 0.5 of average
			}
			for (int k = 0; k < nNodes; k++) {
				Node2D node = nodes[k];
				kin = geometry.kin[k];
				kout = geometry.kout[k];
				double diff = kout + kin - geometry.avgTot;
				double myr = unitradius * (1.0 + diff * (diff > 0.0 ? pnorm : nnorm));
				node.setR(myr);
			}
		}
		// prepare graph for display:
		// (1) shift center of mass into origin;
		// (2) rescale size of graph
		Point2D com = new Point2D();
		double maxRad = -Double.MAX_VALUE;
		for (Node2D node : nodes) {
			maxRad = Math.max(maxRad, node.r);
			com.shift(node);
		}
		com.scale(-1.0 / nNodes);
		double shift = -maxRad * 0.5;
		com.shift(shift, shift);
		double maxDist2 = -Double.MAX_VALUE;
		for (Node2D node : nodes) {
			node.shift(com);
			maxDist2 = Math.max(maxDist2, node.distance2());
		}
		// scaling the radius works well - maybe we should scale the line thickness too.
		setRadius(Math.sqrt(maxDist2) + maxRad);
		linkNodes();
	}

	@Override
	public void linkNodes() {
		links.reset();
		if (nLinks <= 0 || fLinks <= 0.0)
			return; // nothing to do
		if (nLinks > MAX_LINK_COUNT) {
			engine.getLogger().warning("Too many links to draw - skipping!");
			return;
		}
		if (geometry.isUndirected) {
			if (fLinks >= 1.0) {
				// draw all links
				for (int n = 0; n < nNodes; n++) {
					Node2D nodeA = nodes[n];
					int[] neigh = geometry.out[n];
					int len = geometry.kout[n];
					for (int i = 0; i < len; i++) {
						// check if link was already drawn
						int b = neigh[i];
						if (b < n)
							continue;
						Node2D nodeB = nodes[b];
						links.moveTo(nodeA.x, nodeA.y);
						links.lineTo(nodeB.x, nodeB.y);
					}
				}
				return;
			}
// TODO: the procedure below looks dangerous. randomly pick nodes and neighbours
// probably not worth worrying about double picking of links because this
// should apply only if there are many to begin with...
// add context menu to adjust fraction of visible links
			// draw only fraction of undirected links
			// this is pretty memory intensive - hopefully it works...
			int[] idxs = new int[nLinks];
			for (int n = 0; n < nLinks; n++)
				idxs[n] = n;
			int toDraw = (int) (fLinks * nLinks);
			for (int l = 0; l < toDraw; l++) {
				int idxsidx = rng.random0n(nLinks - l);
				int idx = idxs[idxsidx];
				idxs[idxsidx] = idxs[nLinks - l - 1];
				// find node
				int a = -1;
				for (int n = 0; n < nNodes; n++) {
					int k = geometry.kout[n];
					if (idx < k) {
						a = n;
						break;
					}
					idx -= k;
				}
				Node2D nodeA = nodes[a];
				int[] neigh = geometry.out[a];
				Node2D nodeB = nodes[neigh[idx]];
				links.moveTo(nodeA.x, nodeA.y);
				links.lineTo(nodeB.x, nodeB.y);
			}
			return;
		}
		// directed network
		Vector2D link = new Vector2D();
		Vector2D tip = new Vector2D();
		double arrowsize = Math.pow(0.8 / nNodes, 0.25);
		if (fLinks >= 1.0) {
			// draw all links
			for (int n = 0; n < nNodes; n++) {
				Node2D nodeA = nodes[n];
				int[] neigh = geometry.out[n];
				int len = geometry.kout[n];
				for (int i = 0; i < len; i++) {
					int b = neigh[i];
					if (geometry.isNeighborOf(b, n)) {
						// undirected link - check if already drawn
						if (b < n)
							continue;
						Node2D nodeB = nodes[b];
						links.moveTo(nodeA.x, nodeA.y);
						links.lineTo(nodeB.x, nodeB.y);
						continue;
					}
					// directed link - add arrow
					Node2D nodeB = nodes[b];
					links.moveTo(nodeA.x, nodeA.y);
					links.lineTo(nodeB.x, nodeB.y);
					link.set(nodeB, nodeA);
					link.normalize(0.5 * nodeB.r);
					tip.add(nodeB, link);
					// note: arrows that scale with size of tail node are a bit confusing
					link.normalize(arrowsize);
					links.moveTo(tip.x + link.x - 0.3 * link.y, tip.y + link.y + 0.3 * link.x);
					links.lineTo(tip.x, tip.y);
					links.lineTo(tip.x + link.x + 0.3 * link.y, tip.y + link.y - 0.3 * link.x);
				}
			}
			return;
		}
		// draw only fraction of directed links (undirected links are treated as a pair
		// of directed ones)
		// this is pretty memory intensive - hopefully it works...
		int[] idxs = new int[nLinks];
		for (int n = 0; n < nLinks; n++)
			idxs[n] = n;
		int toDraw = (int) (fLinks * nLinks);
		for (int l = 0; l < toDraw; l++) {
			int idxsidx = rng.random0n(nLinks - l);
			int idx = idxs[idxsidx];
			idxs[idxsidx] = idxs[nLinks - l - 1];
			// find node
			int a = -1;
			for (int n = 0; n < nNodes; n++) {
				int k = geometry.kout[n];
				if (idx < k) {
					a = n;
					break;
				}
				idx -= k;
			}
			Node2D nodeA = nodes[a];
			int[] neigh = geometry.out[a];
			int b = neigh[idx];
			Node2D nodeB = nodes[b];
			links.moveTo(nodeA.x, nodeA.y);
			links.lineTo(nodeB.x, nodeB.y);
			link.set(nodeB, nodeA);
			link.normalize(0.5 * nodeB.r);
			tip.add(nodeB, link);
			// note: arrows that scale with size of tail node are a bit confusing
			link.normalize(arrowsize);
			links.moveTo(tip.x + link.x - 0.3 * link.y, tip.y + link.y + 0.3 * link.x);
			links.lineTo(tip.x, tip.y);
			links.lineTo(tip.x + link.x + 0.3 * link.y, tip.y + link.y - 0.3 * link.x);
		}
	}

	@Override
	public Node2D[] toArray() {
		return nodes;
	}

	@Override
	public Node2D get(int index) {
		return nodes[index];
	}
}
