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

import java.awt.Color;
import java.awt.Font;
import java.awt.Point;
import java.awt.geom.AffineTransform;

public class GraphLabel extends Point {

	private static final long serialVersionUID = 20110423L;

	String	label = "";
	Color	color = Color.black;
	AffineTransform at;
	boolean	vertical = false;
	Font	font;

	public GraphLabel(String label, int x, int y) {
		this(label, x, y, Color.black);
	}

	public GraphLabel(String label, int x, int y, Color color) {
		this.label = label;
		this.x = x;
		this.y = y;
		this.color = color;
	}
}
