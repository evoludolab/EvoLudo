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

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Stroke;

public class GraphStyle {

	private final Component component;

	public Font	tickFont = defaultTickFont;
	private double	tickFontSize = FONT_TICK_SIZE;
	FontMetrics	tickMetrics;

	public Font	labelFont = defaultLabelFont;
	private double	labelFontSize = FONT_LABEL_SIZE;
	FontMetrics	labelMetrics;

	public Font		menuFont = defaultMenuFont;

	protected double	lineStrokeWidth = LINE_STROKE;
	public Stroke	lineStroke = defaultLineStroke;

	protected double	frameStrokeWidth = FRAME_STROKE;
	public Stroke	frameStroke = defaultFrameStroke;

	public Color	background = Color.white;
	public Color	zoomframe = new Color(255, 127, 127);
	public Color	gridcolor = new Color(160, 160, 160);

	public int	tickMajorLength = TICK_MAJOR_LENGTH;
	public int	tickMinorLength = TICK_MINOR_LENGTH;
	public int	gapTickLabel = GAP_TICK_LABEL;
	public int	gapAxisLabel = GAP_AXIS_LABEL;

	static final int TICK_MAJOR_LENGTH = 4;
	static final int TICK_MINOR_LENGTH = 2;
	static final int GAP_TICK_LABEL = 3;
	static final int GAP_AXIS_LABEL = 3;
	static final int FONT_MENU_SIZE = 11;
	static final int FONT_TICK_SIZE = 10;
	static final int FONT_LABEL_SIZE = 11;

	public static final double LINE_STROKE = 0.008;
	public static final double FRAME_STROKE = 1.0;

	public static Font	defaultMenuFont = new Font("Default", Font.PLAIN, FONT_MENU_SIZE);
	public static Font	defaultTickFont = new Font("Default", Font.PLAIN, FONT_TICK_SIZE);
	public static Font	defaultLabelFont = new Font("Default", Font.BOLD, FONT_LABEL_SIZE);
	public static Stroke	defaultLineStroke = new BasicStroke((float)LINE_STROKE, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
	public static Stroke	defaultFrameStroke = new BasicStroke((float)FRAME_STROKE, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);

	public GraphStyle(Component component) {
		this.component = component;
		labelMetrics = component.getFontMetrics(labelFont);
		tickMetrics = component.getFontMetrics(tickFont);
	}

	public void setLabelFont(Font font) {
		if( font==null ) return;
//		if( font.getFontName().equals(labelFont.getFontName()) ) return;
		labelFontSize = font.getSize2D();
		labelFont = font.deriveFont((float)(labelFontSize*zoom));
		labelMetrics = component.getFontMetrics(labelFont);
	}

	public void setTickFont(Font font) {
		if( font==null ) return;
//		if( font.getFontName().equals(tickFont.getFontName()) ) return;
		tickFontSize = font.getSize2D();
		tickFont = font.deriveFont((float)(tickFontSize*zoom));
		tickMetrics = component.getFontMetrics(tickFont);
	}

	public void setLineStroke(double width) {
		if( width<=0.0 ) return;
		lineStrokeWidth = width;
		lineStroke = new BasicStroke((float)lineStrokeWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
	}

	public void setFrameStroke(double width) {
		if( width<=0.0 ) return;
		frameStrokeWidth = width;
		frameStroke = new BasicStroke((float)frameStrokeWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
	}

	private double zoom = 1.0;
	private double zoomStroke = 1.0;

	// reset zoom
	public void zoom() {
		tickFont = tickFont.deriveFont((float)tickFontSize);
		labelFont = labelFont.deriveFont((float)labelFontSize);
		tickMetrics = component.getFontMetrics(tickFont);
		labelMetrics = component.getFontMetrics(labelFont);
//		lineStroke = new BasicStroke(lineStrokeWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
		frameStroke = new BasicStroke((float)frameStrokeWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
		tickMajorLength = TICK_MAJOR_LENGTH;
		tickMinorLength = TICK_MINOR_LENGTH;
		gapTickLabel = GAP_TICK_LABEL;
		gapAxisLabel = GAP_AXIS_LABEL;
		zoom = 1.0;
	}

	// zoom in x- and y-direction by a common factor
	public void zoom(double s) {
		zoom(s, s);
	}

	public void zoom(double xs, double ys) {
		// zoom font etc less to maintain an optically balanced appearance
		zoom *= (1.0+Math.min(xs, ys))*0.5;
		zoomStroke *= (1.0+Math.min(xs, ys))*0.25;	// strokes must scale even less
		if( zoom<=1.0 ) {
			zoom();
			return;
		}
		tickFont = tickFont.deriveFont((float)(tickFontSize*zoom));
		labelFont = labelFont.deriveFont((float)(labelFontSize*zoom));
		tickMetrics = component.getFontMetrics(tickFont);
		labelMetrics = component.getFontMetrics(labelFont);
//		lineStroke = new BasicStroke(lineStrokeWidth*zoomStroke, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
		frameStroke = new BasicStroke((float)(frameStrokeWidth*zoomStroke), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
		tickMajorLength = Math.max(TICK_MAJOR_LENGTH, (int)(TICK_MAJOR_LENGTH*zoom+0.5));
		tickMinorLength = Math.max(TICK_MINOR_LENGTH, (int)(TICK_MINOR_LENGTH*zoom+0.5));
		gapTickLabel = Math.max(GAP_TICK_LABEL, (int)(GAP_TICK_LABEL*zoom+0.5));
		gapAxisLabel = Math.max(GAP_AXIS_LABEL, (int)(GAP_AXIS_LABEL*zoom+0.5));
	}
}
