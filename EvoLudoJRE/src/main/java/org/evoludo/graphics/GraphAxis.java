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

import java.text.DecimalFormat;

public class GraphAxis {

	public double	min = 0.0;
	public double	lower = 0.0;
	public double	max = 1.0;
	public double	upper = 1.0;
	public double	step = 0.0;
	public int		steps = 0;
//	public double	iRange = 1.0;
	public int		grid = 0;
	public int		majorTicks = -1;
	public int		minorTicks = 0;
	public boolean	logScale = false;
	public String	unit = "";
	public DecimalFormat	formatter = new DecimalFormat("0.##");
	public String	label;
	public boolean	showLabel = false;
	public boolean	enabled = true;

	public void restore() {
		lower = min;
		upper = max;
	}
}
