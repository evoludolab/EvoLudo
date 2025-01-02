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

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.ArrayList;

class HistoMarkedBin {
	public int bin;
	public Color color = Color.black;

	public HistoMarkedBin(int bin, Color color) {
		this.bin = bin;
		if( color!=null ) this.color = color;
	}
}

public class HistoFrameLayer extends FrameLayer {

	private static final long serialVersionUID = 20110423L;

	String name = "";
	Color color = Color.black;
	int	binwidth;
	HistoData data;
	private final java.util.List<HistoMarkedBin>	marks = new ArrayList<HistoMarkedBin>();

	public HistoFrameLayer(HistoData data, GraphStyle style) {
		super(style);
		this.data = data;
	}

	@Override
	protected void initGraphRect(Rectangle bounds) {
		super.initGraphRect(bounds);
		binwidth = (int)(canvas.width*(xaxis.max-xaxis.min)/(data.bins*(xaxis.upper-xaxis.lower)));
		int visible = (int)((xaxis.upper-xaxis.lower)/(xaxis.max-xaxis.min)*data.bins+0.5);
		int width = visible*binwidth;
		// center histogram
		canvas.x += (canvas.width-width)/2;
		canvas.width = width;
	}

	public void markBin(int bin, Color bincolor) {
		marks.add(new HistoMarkedBin(bin, bincolor));
	}

	public void resetBinMarks() {
		marks.clear();
	}

	public boolean updateMarkedBin(int idx, double binvalue, Color bincolor) {
		if( !Double.isFinite(binvalue) ) {
			if( marks.size()<=idx ) return false;
			marks.remove(idx);
			return true;
		}
		int bin = convertX2Bin(binvalue);
		if( marks.size()<=idx ) {
			markBin(bin, bincolor);
			return true;
		}
		boolean changed = false;
		HistoMarkedBin b = marks.get(idx);
		if( bin!=b.bin ) changed = true;
		if( !bincolor.equals(b.color) ) changed = true;
		if( changed ) marks.set(idx, new HistoMarkedBin(bin, bincolor));
		return changed;
	}

	@Override
	protected void paintComponent(Graphics g) {
		Graphics2D g2 = (Graphics2D)g;
		int start = (int)((xaxis.lower-xaxis.min)/(xaxis.max-xaxis.min)*data.bins+0.5);
		int end = (int)((xaxis.upper-xaxis.min)/(xaxis.max-xaxis.min)*data.bins+0.5);
		for( HistoMarkedBin b : marks ) {
			if( b.bin<start || b.bin>end ) continue;
			g2.setPaint(b.color);
			int xcoord = canvas.x+(b.bin-start)*binwidth;
			g2.drawLine(xcoord, canvas.y, xcoord, canvas.y+canvas.height-1);
			g2.drawLine(xcoord+binwidth, canvas.y, xcoord+binwidth, canvas.y+canvas.height-1);
		}
		super.paintComponent(g);
	}

/* potentially useful conversion methods - currently unused
	private int convertCoord2Bin(int coord) {
		int start = (int)((xaxis.lower-xaxis.min)/(xaxis.max-xaxis.min)*data.bins+0.5);
		return start+(coord-canvas.x)/binwidth;
	}

	private int convertX2Coord(double xcoord) {
		return canvas.x+(int)((xcoord-xaxis.min)/(xaxis.max-xaxis.min)*canvas.width+0.5);
	}*/

	private int convertX2Bin(double x) {
		return (int)(((x-xaxis.min+xaxis.lower-xaxis.min)/(xaxis.max-xaxis.min))*(data.bins-1));
	}
}
