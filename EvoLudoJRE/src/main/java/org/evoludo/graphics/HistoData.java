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

public class HistoData {

	public double[]	state = new double[0];
//	public int		bins = HistoGraph.HISTO_BINS;
	public int		bins = 0;
	public int		binwidth;
	public double	xmin = 0.0;
	public double	xmax = 1.0;
	public double	ymin = 0.0;
	public double	ymax = 1.0;
	public double	timestamp = -1.0;
	public boolean	logx = false;
	public boolean	logy = false;

	public void reset() {
		timestamp = -1.0;
		Arrays.fill(state, 0.0);
	}
}
