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

package org.evoludo.simulator;

import java.util.ArrayList;

import org.evoludo.geom.Node3D;
import org.evoludo.geom.Vector3D;
import org.evoludo.simulator.geometries.AbstractGeometry;
import org.evoludo.simulator.geometries.GeometryFeatures;
import org.evoludo.simulator.geometries.GeometryType;

/**
 * Graphical representation of generic population geometries in 3D. A network
 * corresponds to a (possibly ephemeral) collection and configuration of nodes.
 *
 * @author Christoph Hauert
 */
public abstract class Network3D extends Network<Node3D> {

	/**
	 * Fraction of the nominal nearest-neighbour spacing used as radial shell
	 * thickness. Values below one create more shells than a strict packing
	 * estimate, which makes the seed look more organic while still filling the
	 * volume.
	 */
	private static final double SHELL_THICKNESS_SCALE = 0.75;

	/**
	 * Create a new network in 3D for the given engine and geometry.
	 * 
	 * @param engine   the pacemaker for running the model
	 * @param geometry the structure of the population
	 */
	protected Network3D(EvoLudo engine, AbstractGeometry geometry) {
		// network is shared between different graphs - cannot set listener here!
		super(engine, geometry);
		accuracy = 1e-5;
	}

	@Override
	public void initNodes(double pnorm, double nnorm, double unitradius) {
		if (nodes == null || nodes.length != nNodes) {
			nodes = new Node3D[nNodes];
			for (int k = 0; k < nNodes; k++)
				nodes[k] = new Node3D();
		}

		GeometryFeatures gFeats = geometry.getFeatures();
		double avgTot = gFeats.avgTot;
		unitradius *= 30.0;
		// Initialize the 3D seed as concentric phyllotactic shells. Each shell uses a
		// spherical Fibonacci pattern, while the shell populations are matched to the
		// shell volumes to fill the ball approximately uniformly.
		ArrayList<double[]> seedPositions = createFibonacciShells(nNodes);
		for (int k = 0; k < nNodes; k++) {
			double[] seed = seedPositions.get(k);
			nodes[k].set(seed[0], seed[1], seed[2], scaledNodeRadius(k, avgTot, pnorm, nnorm, unitradius));
		}
	}

	/**
	 * Create a deterministic ball composed of concentric phyllotactic shells.
	 * Node zero is kept at the origin, while the remaining nodes are distributed
	 * over spherical shells with populations proportional to shell volume.
	 * 
	 * @param nNodes the number of nodes to distribute
	 * @return the shell positions sorted from the center to the boundary
	 */
	private static ArrayList<double[]> createFibonacciShells(int nNodes) {
		ArrayList<double[]> shells = new ArrayList<>(nNodes);
		if (nNodes <= 0)
			return shells;
		shells.add(new double[] { 0.0, 0.0, 0.0 });
		if (nNodes == 1)
			return shells;
		final double maxR = 0.85 * UNIVERSE_RADIUS;
		final double targetSpacing = maxR
				* Math.cbrt(4.0 * Math.PI * Math.sqrt(2.0) / (3.0 * nNodes));
		double shellWidth = Math.max(maxR / (nNodes - 1),
				targetSpacing * SHELL_THICKNESS_SCALE);
		int nShells = Math.max(1, (int) Math.ceil(maxR / shellWidth));
		shellWidth = maxR / nShells;
		int[] nPoints = nodesPerShell(maxR, nShells, nNodes - 1);
		for (int s = 0; s < nShells; s++) {
			int pShell = nPoints[s];
			if (pShell <= 0)
				continue;
			double iR = s * shellWidth;
			double oR = Math.min(maxR, iR + shellWidth);
			double sR = volumeMeanRadius(iR, oR);
			double sPhase = s * GOLDEN_ANGLE;
			for (int p = 0; p < pShell; p++) {
				double z = 1.0 - 2.0 * (p + 0.5) / pShell;
				double planar = Math.sqrt(Math.max(0.0, 1.0 - z * z));
				double angle = sPhase + p * GOLDEN_ANGLE;
				shells.add(new double[] { sR * planar * Math.cos(angle), sR * planar * Math.sin(angle),
						sR * z });
			}
		}
		return shells;
	}

	/**
	 * Apportion the non-central nodes across concentric shells in proportion to
	 * shell volume using the largest-remainder method.
	 * 
	 * @param maxR    the radius of the seed ball
	 * @param nShells the number of shells
	 * @param sNodes  the number of nodes on shells
	 * @return the number of nodes placed on each shell
	 */
	private static int[] nodesPerShell(double maxR, int nShells, int sNodes) {
		double shellWidth = maxR / Math.max(1, nShells);
		int[] nPoints = new int[nShells];
		if (sNodes <= 0)
			return nPoints;
		double[] remainders = new double[nShells];
		double[] wShells = new double[nShells];
		double totWeight = 0.0;
		for (int s = 0; s < nShells; s++) {
			double iR = s * shellWidth;
			double oR = Math.min(maxR, iR + shellWidth);
			double weight = oR * oR * oR - iR * iR * iR;
			wShells[s] = weight;
			totWeight += weight;
		}
		int assigned = 0;
		for (int s = 0; s < nShells; s++) {
			double ideal = sNodes * wShells[s] / totWeight;
			nPoints[s] = (int) Math.floor(ideal);
			remainders[s] = ideal - nPoints[s];
			assigned += nPoints[s];
		}
		for (int extra = assigned; extra < sNodes; extra++) {
			int pick = 0;
			for (int shell = 1; shell < nShells; shell++) {
				if (remainders[shell] > remainders[pick])
					pick = shell;
			}
			nPoints[pick]++;
			remainders[pick] = -1.0;
		}
		return nPoints;
	}

	/**
	 * Compute the radius that best represents the shell volume between two
	 * boundaries. This uses the radius averaged with respect to volume density.
	 * 
	 * @param iR the inner shell boundary
	 * @param oR the outer shell boundary
	 * @return the representative shell radius
	 */
	private static double volumeMeanRadius(double iR, double oR) {
		double i3 = iR * iR * iR;
		double o3 = oR * oR * oR;
		double volMoment = o3 * oR - i3 * iR;
		return volMoment <= 0.0 ? iR : 0.75 * volMoment / (o3 - i3);
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
	public static final double UNIVERSE_RADIUS = 150.0;

	/**
	 * The inverse size of the baseline 3D universe. Convenience constant.
	 */
	private static final double IR = 1.0 / UNIVERSE_RADIUS;

	/**
	 * The inverse squared size of the baseline 3D universe,
	 * {@code 1/R<sup>2</sup>}. Convenience constant.
	 */
	private static final double IR2 = IR * IR;

	/**
	 * Smallest admissible squared center-to-center distance during layout
	 * calculations.
	 */
	private static final double MIN_DISTANCE2 = MIN_DISTANCE * MIN_DISTANCE;

	@Override
	public double relax(int nodeidx) {
		return relax(nodeidx, 1.6);
	}

	@Override
	public double relax(int nodeidx, double dt) {
		double energy = repulsion(nodeidx);
		energy += attraction(nodeidx);
		repulsion.scale(1.0 / geometry.getSize());
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
			double dist = pairDistance(nodei, node, i, nodeidx);
			double overlapDepth = node.getR() + nodei.getR() - dist;
			// force decreases with 1/distance^2 - correct for size of spheres
			// double dist = Math.max(0.0001, (vec.length()-(radius+node.radius))*IR);
			double scaledDist = Math.max(MIN_SCALED_DISTANCE, dist * IR);
			// force becomes attractive if (scaled) distance exceeds 1 to prevent
			// disconnected graphs from flying apart.
			// zero is set at distance 1. assumes equal charges of 1.
			npot -= 2.0 - 1.0 / scaledDist - scaledDist;
			double force = 1.0 / (scaledDist * scaledDist) - 1.0;
			if (overlapDepth > 0.0) {
				double normalizedOverlap = overlapDepth * IR;
				npot += 0.5 * HARD_CORE_STIFFNESS * normalizedOverlap * normalizedOverlap;
				force += HARD_CORE_STIFFNESS * overlapDepth * IR2 / dist;
			}
			vec.scale(force);
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
			double dist = pairDistance(node, nodei, nodeidx, neighs[i]);
			double gap = dist - node.getR() - nodei.getR();
			// force increases linearly with distance - correct for size of spheres
			npot += gap * gap * IR2;
			vec.scale(gap / dist);
			attraction.add(vec);
		}
		if (geometry.isUndirected()) {
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
			double dist = pairDistance(node, nodei, nodeidx, neighs[i]);
			double gap = dist - node.getR() - nodei.getR();
			// force increases linearly with distance - correct for size of spheres
			npot += gap * gap * IR2;
			vec.scale(gap / dist);
			attraction.add(vec);
		}
		int nTot = nOut + nIn;
		if (nTot == 0)
			return 0.0;
		attraction.scale(1.0 / nTot);
		return npot;
	}

	/**
	 * Set {@link #vec} to the directional vector from {@code from} to {@code to}
	 * and return the corresponding center-to-center distance. Coincident nodes are
	 * separated by a deterministic fallback direction to keep layouting stable and
	 * reproducible.
	 * 
	 * @param from    the source node
	 * @param to      the target node
	 * @param fromidx the index of the source node
	 * @param toidx   the index of the target node
	 * @return the center-to-center distance
	 */
	private double pairDistance(Node3D from, Node3D to, int fromidx, int toidx) {
		vec.set(from, to);
		double dist2 = vec.length2();
		if (dist2 >= MIN_DISTANCE2)
			return Math.sqrt(dist2);
		int low = Math.min(fromidx, toidx);
		int high = Math.max(fromidx, toidx);
		double dx = fractional((low + 1.0) * 0.7548776662466927 + (high + 1.0) * 0.5698402909980532) - 0.5;
		double dy = fractional((low + 1.0) * 0.4389396481197804 + (high + 1.0) * 0.9132452716531554) - 0.5;
		double dz = fractional((low + 1.0) * 0.2873179542797946 + (high + 1.0) * 0.6710436067037892) - 0.5;
		double fallbackNorm2 = dx * dx + dy * dy + dz * dz;
		if (fallbackNorm2 < MIN_DISTANCE2) {
			dx = 1.0;
			dy = 0.0;
			dz = 0.0;
			fallbackNorm2 = 1.0;
		}
		double scale = MIN_DISTANCE / Math.sqrt(fallbackNorm2);
		if (fromidx > toidx)
			scale = -scale;
		vec.set(dx * scale, dy * scale, dz * scale);
		return MIN_DISTANCE;
	}

	/**
	 * Return the fractional part of {@code value}.
	 * 
	 * @param value the value to reduce to its fractional part
	 * @return the fractional part in the interval {@code [0,1)}
	 */
	private static double fractional(double value) {
		return value - Math.floor(value);
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
		if (geometry.isType(GeometryType.DYNAMIC)) {
			// need to set radius as well in dynamic networks
			GeometryFeatures gFeats = geometry.getFeatures();
			int minTot = gFeats.minTot;
			int krange = gFeats.maxTot - minTot;
			double invRange = krange > 0 ? 1.0 / krange : 1.0;
			double unitradius = Math.max(1.0, Math.sqrt(0.6 / nNodes) * UNIVERSE_RADIUS) * 1.2;
			for (int k = 0; k < nNodes; k++) {
				Node3D node = nodes[k];
				node.shift(com);
				int kin = geometry.kin[k];
				int kout = geometry.kout[k];
				node.setR(unitradius * (0.5 + 2.5 * (kout + kin - minTot) * invRange));
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

	@Override
	public boolean equals(Object obj) {
		return super.equals(obj);
	}

	@Override
	public int hashCode() {
		return super.hashCode();
	}
}
