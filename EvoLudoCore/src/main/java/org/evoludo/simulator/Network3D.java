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

package org.evoludo.simulator;

import org.evoludo.geom.Node3D;
import org.evoludo.geom.Vector3D;

/**
 * Graphical representation of generic population geometries in 3D. A network
 * corresponds to a (possibly ephemeral) collection and configuration of nodes.
 *
 * @author Christoph Hauert
 */
public abstract class Network3D extends Network {

	/**
	 * The array with all nodes of this network. This deliberately overrides
	 * {@code super.nodes}. The two arrays are identical but saves a ton of
	 * unnecesary casts.
	 */
	@SuppressWarnings("hiding")
	protected Node3D[] nodes = null;

	/**
	 * Create a new network in 3D for the given engine and geometry.
	 * 
	 * @param engine   the pacemaker for running the model
	 * @param geometry the structure of the population
	 */
	public Network3D(EvoLudo engine, Geometry geometry) {
		// network is shared between different graphs - cannot set listener here!
		super(engine, geometry);
		setAccuracy(1e-5);
	}

	@Override
	public void initNodes(double pnorm, double nnorm, double unitradius) {
		if (nodes == null || nodes.length != nNodes) {
			nodes = new Node3D[nNodes];
			super.nodes = this.nodes;
			for (int k = 0; k < nNodes; k++)
				nodes[k] = new Node3D();
		}

		double dangle = Math.PI * (3.0 - Math.sqrt(5.0));
		double angle = 0.0;
		double dz = 2.0 / nNodes;
		double z = 1.0 - dz / 2.0;
		unitradius *= 30.0;
		// initialize structure with all nodes equally distributed on a sphere and
		// node zero at center (reduces relaxation for (super-)stars et al. where
		// central hub is always node zero).
		int kin = geometry.kin[0];
		int kout = geometry.kout[0];
		double diff = kout + kin - geometry.avgTot;
		double myr = unitradius * (1.0 + diff * (diff > 0.0 ? pnorm : nnorm));
		nodes[0].set(0.0, 0.0, 0.0, myr);
		for (int k = 1; k < nNodes; k++) {
			double r = (Math.sqrt(1.0 - z * z) + 0.1 * (rng.random01() - 0.5)) * UNIVERSE_RADIUS;
			kin = geometry.kin[k];
			kout = geometry.kout[k];
			diff = kout + kin - geometry.avgTot;
			myr = unitradius * (1.0 + diff * (diff > 0.0 ? pnorm : nnorm));
			nodes[k].set(r * Math.sin(angle), r * Math.cos(angle), z * UNIVERSE_RADIUS, myr);
			z -= dz;
			// add a little noise to prevent getting stuck in symmetrical configurations
			angle += dangle * (0.5 + rng.random01());
		}
	}

	/**
	 * Helper variable to store intermediate results when considering the potential
	 * energy resulting from the attraction between neighbouring nodes.
	 */
	private final Vector3D attraction = new Vector3D();

	/**
	 * Helper variable to store intermediate results when considering the potential
	 * energy resulting from the repulsion between nodes.
	 */
	private final Vector3D repulsion = new Vector3D();

	/**
	 * Temporary storage for the directional vector connecting two nodes.
	 */
	private final Vector3D vec = new Vector3D();

	/**
	 * The baseline size of the 3D universe.
	 */
	public static final double UNIVERSE_RADIUS = 120.0;

	/**
	 * The inverse size of the baseline 3D universe. Convenience constant.
	 */
	private static final double IR = 1.0 / UNIVERSE_RADIUS;

	/**
	 * The inverse squared size of the baseline 3D universe,
	 * {@code 1/R<sup>2</sup>}. Convenience constant.
	 */
	private static final double IR2 = IR * IR;

	@Override
	public double relax(int nodeidx) {
		return relax(nodeidx, 1.6);
	}

	@Override
	public double relax(int nodeidx, double dt) {
		double energy = repulsion(nodeidx);
		energy += attraction(nodeidx);
		repulsion.scale(1.0 / geometry.size);
		repulsion.add(attraction);
		// final double delta = repulsion.length();
		// // empirical constant - choose as big as possible to speed things up but
		// small
		// // enough to allow convergence (prevent excessive wiggling of nodes)
		// if (delta > dt)
		// dt /= delta;
		// // the displacement is at most dt
		// repulsion.scale(dt);
		double delta = Math.min(dt / repulsion.length(), dt);
		repulsion.scale(delta);
		nodes[nodeidx].shift(repulsion);
		return energy;
	}

	@Override
	protected double repulsion(int nodeidx) {
		repulsion.set(0.0, 0.0, 0.0);
		double npot = 0.0;
		Node3D node = nodes[nodeidx];
		for (int i = 0; i < nNodes; i++) {
			if (i == nodeidx)
				continue;
			Node3D nodei = nodes[i];
			vec.set(nodei, node);
			// force decreases with 1/distance^2 - correct for size of spheres
			// double dist = Math.max(0.0001, (vec.length()-(radius+node.radius))*IR);
			double dist = Math.max(0.0001, vec.length() * IR);
			// force becomes attractive if (scaled) distance exceeds 1 to prevent
			// disconnected graphs from flying apart.
			// zero is set at distance 1. assumes equal charges of 1.
			npot -= 2.0 - 1.0 / dist - dist;
			vec.scale(1.0 / (dist * dist) - 1.0);
			repulsion.add(vec);
		}
		return npot;
	}

	@Override
	protected double attraction(int nodeidx) {
		attraction.set(0.0, 0.0, 0.0);
		double npot = 0.0;
		Node3D node = nodes[nodeidx];
		int nOut = geometry.kout[nodeidx];
		int[] neighs = geometry.out[nodeidx];
		for (int i = 0; i < nOut; i++) {
			Node3D nodei = nodes[neighs[i]];
			vec.set(node, nodei);
			// force increases linearly with distance - correct for size of spheres
			// double dist = vec.length();
			// double distadj = dist-(radius+node.radius);
			// vec.scale(distadj/dist);
			// potential += distadj>0?distadj*distadj*IR2:-distadj*distadj*IR2;
			npot += vec.length2() * IR2;
			// if( index<2 || index==nNodes-1 ) System.out.println(index+":
			// dist="+(dist*IR)+", ("+(distadj*IR)+"), potential="+potential);
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
			Node3D nodei = nodes[neighs[i]];
			vec.set(node, nodei);
			// force increases linearly with distance - correct for size of spheres
			// double dist = vec.length();
			// double distadj = dist-(radius+node.radius);
			// vec.scale(distadj/dist);
			// potential += distadj>0?distadj*distadj*IR2:-distadj*distadj*IR2;
			npot += vec.length2() * IR2;
			// if( index<2 || index==nNodes-1 ) System.out.println(index+":
			// dist="+(dist*IR)+", ("+(distadj*IR)+"), potential="+potential);
			attraction.add(vec);
		}
		int nTot = nOut + nIn;
		if (nTot == 0)
			return 0.0;
		attraction.scale(1.0 / nTot);
		return npot;
	}

	/**
	 * Prepare graph for display:
	 * <ol>
	 * <li>shift center of mass into origin
	 * <li>rescale size of graph
	 * <li>find number of links
	 * </ol>
	 */
	@Override
	public void finishLayout() {
		Vector3D com = new Vector3D();
		for (Node3D node : nodes)
			com.shift(node);
		com.scale(-1.0 / nNodes);
		if (geometry.isDynamic) {
			// need to set radius as well in dynamic networks
			int krange = geometry.maxTot - geometry.minTot;
			double invRange = krange > 0 ? 1.0 / krange : 1.0;
			double unitradius = Math.max(1.0, Math.sqrt(0.6 / nNodes) * UNIVERSE_RADIUS) * 1.2;
			for (int k = 0; k < nNodes; k++) {
				Node3D node = nodes[k];
				node.shift(com);
				int kin = geometry.kin[k];
				int kout = geometry.kout[k];
				node.setR(unitradius * (0.5 + 2.5 * (kout + kin - geometry.minTot) * invRange));
			}
		} else {
			for (Node3D node : nodes) {
				node.shift(com);
				// maxDist = Math.max(maxDist, node.distance2());
			}
		}
		linkNodes();
	}

	@Override
	public Node3D[] toArray() {
		return nodes;
	}

	@Override
	public Node3D get(int index) {
		return nodes[index];
	}
}
