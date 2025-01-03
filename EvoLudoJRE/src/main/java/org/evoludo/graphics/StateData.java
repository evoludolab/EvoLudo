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

import org.evoludo.geom.Point2D;

public class StateData {
	public int		nStates;
	public double[]	state;
	public double[]	pastState;
	public double[]	start;
	public double	time;
	public double	pastTime;
	public boolean	connect;
	public boolean	isLocal = false;
	public int		localNode = -1;

	public Point2D	now = new Point2D();
	public Point2D	then = new Point2D();
	public Point2D	origin = new Point2D();

	public void init(int len) {
		nStates = len;
		state = new double[nStates];
		pastState = new double[nStates];
		start = new double[nStates];
	}

	public void reset() {
		System.arraycopy(state, 0, start, 0, nStates);
		System.arraycopy(state, 0, pastState, 0, nStates);
		then.setLocation(now);
		origin.setLocation(now);
connect = false;
	}

	public void next() {
		double[] swap = pastState;
		pastState = state;
		state = swap;
		pastTime = time;
		Point2D	past = now;
		now = then;
		then = past;
	}
}
