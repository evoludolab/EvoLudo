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

package org.evoludo.simulator.views;

import org.evoludo.geom.Point2D;
import org.evoludo.math.ArrayMath;
import org.evoludo.simulator.models.Model;
import org.evoludo.util.Formatter;

/**
 * Default mapping of data to simplex \(S_3\) projections. Custom
 * implementations of the {@code Data2S3} interface can be provided by modules
 * that implement the {@code HasS3} interface.
 * 
 * @see HasS3#getS3Map(int)
 */
public class S3Map implements HasS3.Data2S3, BasicTooltipProvider {

	/**
	 * The model that provides the data.
	 */
	Model model;

	/**
	 * The name of the map.
	 */
	String name;

	/**
	 * Create a new mapping of data to simplex projections. The role of the data is
	 * ignored by default.
	 * 
	 * @param model the model that provides the data
	 * @param role  the role of the data
	 */
	public S3Map(Model model, int role) {
		this(model, role, null);
	}

	/**
	 * Create a new mapping of data to simplex projections. The role of the data is
	 * ignored by default.
	 * 
	 * @param model the model that provides the data
	 * @param role  the role of the data
	 * @param name  the name of the map
	 */
	public S3Map(Model model, int role, String name) {
		this.model = model;
		this.name = name;
		// ignore role by default
	}

	/**
	 * The indices of the traits on the simplex. The first entry refers to the lower
	 * left corner, the second to the lower right and the last entry to the top
	 * corner.
	 */
	int[] order = new int[] { 0, 1, 2 };

	/**
	 * Temporary storage for the simplex coordinates of the tooltip.
	 */
	double[] tip = new double[3];

	@Override
	public void setOrder(int[] order) {
		System.arraycopy(order, 0, this.order, 0, this.order.length);
	}

	@Override
	public int[] getOrder() {
		return order;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public Point2D data2S3(double[] s3, Point2D p) {
		return data2S3(s3[order[0] + 1], s3[order[1] + 1], s3[order[2] + 1], p);
	}

	@Override
	public Point2D data2S3(double s1, double s2, double s3, Point2D p) {
		// top (c): s3, right (d): s2, left (l): s1
		p.x = s2 - s1; // [-1, 1]
		p.y = (s3 - s2 - s1 + 1.0) * 0.5 - 1.0 / 3.0; // [-1/3, 2/3]
		p.scale(s1 + s2 + s3);
		p.x = (p.x + 1.0) * 0.5;
		p.y = 2.0 / 3.0 - p.y;
		return p;
	}

	@Override
	public double[] s32Data(double x, double y, double[] s) {
		// point is in scaled user coordinates
		s[order[2]] = Math.max(0.0, y);
		s[order[1]] = Math.max(0.0, x);
		s[order[0]] = Math.max(0.0, 1.0 - x - y);
		ArrayMath.normalize(s);
		return s;
	}

	@Override
	public String getTooltipAt(double sx, double sy) {
		s32Data(sx, sy, tip);
		String[] names = model.getMeanNames();
		String msg = "<table>";
		for (int i = 0; i < 3; i++)
			msg += "<tr><td style='text-align:right'><i>" + names[i] + ":</i></td><td>" //
					+ Formatter.formatPercent(tip[i], 2) + "</td></tr>";
		return msg + "</table>";
	}
}
