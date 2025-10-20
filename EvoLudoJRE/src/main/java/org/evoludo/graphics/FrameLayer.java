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

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.font.TextLayout;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.util.ArrayList;

import javax.swing.JComponent;

import org.evoludo.math.Combinatorics;

class FrameThreshold {
	public double level;
	public Color color = Color.black;

	public FrameThreshold(double level, Color color) {
		this.level = level;
		if (color != null)
			this.color = color;
	}
}

public class FrameLayer extends JComponent {

	private static final long serialVersionUID = 20110423L;

	protected GeneralPath frame = new GeneralPath();
	protected GeneralPath grid = new GeneralPath();
	protected Rectangle mousedrag;
	protected java.util.List<GraphLabel> tickLabels = new ArrayList<>();
	protected java.util.List<GraphLabel> axisLabels = new ArrayList<>();
	protected java.util.List<GraphLabel> annotations = new ArrayList<>();
	private final java.util.List<FrameThreshold> thresholds = new ArrayList<>();
	public GraphAxis xaxis = new GraphAxis();
	public GraphAxis yaxis = new GraphAxis();
	public GraphStyle style;
	public java.util.List<GraphLabel> labels = new ArrayList<>();
	public Shape outline;
	public Rectangle canvas = new Rectangle();
	String msg;

	protected static final int DEFAULT_DX = 2;
	protected static final int FRAME_STROKE = 1;
	protected static final int MARGIN = 4;

	public FrameLayer(GraphStyle style) {
		this.style = style;
		canvas.setBounds(getBounds());
	}

	public void init() {
		outline = canvas;
		frame.reset();
		int adjust = (int) style.frameStrokeWidth;
		Rectangle myFrame = new Rectangle(canvas.x - adjust, canvas.y - adjust, canvas.width + adjust,
				canvas.height + adjust);
		frame.append(myFrame, false);
		if (msg != null)
			return;
		formatAxis();
		formatTicks();
		formatAxisLabels();
		formatGrid();
	}

	public void init(Rectangle bounds) {
		Point loc = bounds.getLocation();
		bounds.setLocation(0, 0);
		initGraphRect(bounds);
		init();
		bounds.setLocation(loc);
	}

	protected void initGraphRect(Rectangle bounds) {
		// determine size of graph/canvas
		int top = MARGIN, bottom = MARGIN, left = MARGIN, right = MARGIN;
		if (msg != null) {
			int width = bounds.width - left - right;
			// center graph horizontally
			left = (bounds.width - width) / 2;
			int height = bounds.height - top - bottom;
			// center graph vertically
			top = (bounds.height - height) / 2;
			canvas.setBounds(left, top, width, height);
			return;
		}
		// no message to display
		boolean hasLabels = (labels.size() >= 4);
		int ytickwidth;
		if (yaxis.enabled) {
			if (yaxis.showLabel && yaxis.label != null)
				right += style.labelMetrics.getHeight() - style.labelMetrics.getDescent() + style.gapAxisLabel;
			if (yaxis.majorTicks >= 0) {
				ytickwidth = Math.max(
						style.tickMetrics.stringWidth(yaxis.formatter.format(yaxis.max / Math.PI) + yaxis.unit),
						style.tickMetrics.stringWidth(yaxis.formatter.format(yaxis.min / Math.PI) + yaxis.unit));
				right += style.tickMajorLength + ytickwidth + style.gapTickLabel;
			} else if (yaxis.minorTicks > 0)
				right += style.tickMinorLength;
		}
		if (hasLabels) {
			// note: how to determine the sequence of labels? - this must be specified in a
			// flexible way!
			int spaceleft = 2 + Math.max(style.labelMetrics.stringWidth(labels.get(0).label) / 2,
					style.labelMetrics.stringWidth(labels.get(3).label) / 2);
			int spaceright = 2 + Math.max(style.labelMetrics.stringWidth(labels.get(2).label) / 2,
					style.labelMetrics.stringWidth(labels.get(1).label) / 2);
			left = Math.max(left, spaceleft);
			right = Math.max(right, spaceright);
		}
		int width = bounds.width - left - right;

		if (xaxis.enabled) {
			if (xaxis.showLabel && xaxis.label != null)
				bottom += style.labelMetrics.getHeight() + style.gapAxisLabel;
			if (xaxis.majorTicks >= 0)
				bottom += style.tickMajorLength + style.tickMetrics.getHeight() + style.gapTickLabel;
			else if (xaxis.minorTicks > 0)
				bottom += style.tickMinorLength;
		}
		if (hasLabels) {
			top += style.labelMetrics.getHeight() + style.gapAxisLabel;
			if (!xaxis.showLabel || xaxis.label == null)
				bottom += style.labelMetrics.getHeight() + style.gapAxisLabel;
		}
		int height = bounds.height - top - bottom;
		// center graph vertically
		canvas.setBounds(left, top, width, height);
	}

	protected void setLabels(String[] names, Color[] colors, int tag) {
		labels.clear();
		for (int i = 0; i < names.length; i++)
			labels.add(new GraphLabel(names[i], -1, -1, colors[i]));
	}

	protected void setLabels(String[] names, Color[] colors, boolean[] active, int tag) {
		labels.clear();
		for (int i = 0; i < names.length; i++)
			if (active[i])
				labels.add(new GraphLabel(names[i], -1, -1, colors[i]));
	}

	protected void formatAxis() {
		if (xaxis.enabled) {
			if (xaxis.step > 0.0) {
				double range = (double) canvas.width / (double) DEFAULT_DX * xaxis.step;
				double order = Combinatorics.pow(10.0, (int) Math.floor(Math.log10(range)));
				double max = xaxis.min + Math.floor(range / order * 10.0) * 0.1 * order;
				if (Math.abs(xaxis.max - max) > 1e-8) {
					xaxis.max = max;
					xaxis.restore();
				}
			}
			if (xaxis.step < 0.0) {
				double range = -(double) canvas.width / DEFAULT_DX * xaxis.step;
				double order = Combinatorics.pow(10.0, (int) Math.floor(Math.log10(range)));
				double min = xaxis.max - Math.floor(range / order * 10.0) * 0.1 * order;
				if (Math.abs(xaxis.min - min) > 1e-8) {
					xaxis.min = min;
					xaxis.restore();
				}
			}
		}
		if (yaxis.enabled) {
			if (yaxis.step > 0.0) {
				double max;
				if (yaxis.steps > 0)
					max = yaxis.min + yaxis.steps * yaxis.step;
				else
					max = yaxis.min + (yaxis.majorTicks + 1) * (yaxis.minorTicks + 1) * yaxis.step;
				if (Math.abs(yaxis.max - max) > 1e-8) {
					yaxis.max = max;
					yaxis.restore();
				}
			}
			if (yaxis.step < 0.0) {
				double min;
				if (yaxis.steps > 0)
					min = yaxis.max + yaxis.steps * yaxis.step;
				else
					min = yaxis.max + (yaxis.majorTicks + 1) * (yaxis.minorTicks + 1) * yaxis.step;
				if (Math.abs(yaxis.min - min) > 1e-8) {
					yaxis.min = min;
					yaxis.restore();
				}
			}
		}
	}

	protected void formatAxisLabels() {
		axisLabels.clear();
		if (xaxis.enabled) {
			if (xaxis.showLabel && xaxis.label != null) {
				int ylshift = canvas.y + canvas.height + style.tickMajorLength + style.gapTickLabel
						+ style.tickMetrics.getHeight();
				axisLabels.add(new GraphLabel(xaxis.label,
						canvas.x + (canvas.width - style.labelMetrics.stringWidth(xaxis.label)) / 2,
						ylshift + style.gapAxisLabel + style.labelMetrics.getHeight()
								- style.labelMetrics.getDescent()));
			}
		}
		if (yaxis.enabled) {
			if (yaxis.showLabel && yaxis.label != null) {
				int xlshift = canvas.x + canvas.width + style.tickMajorLength + style.gapTickLabel;
				int ytickwidth = Math.max(
						style.tickMetrics.stringWidth(yaxis.formatter.format(yaxis.max / Math.PI) + yaxis.unit),
						style.tickMetrics.stringWidth(yaxis.formatter.format(yaxis.min / Math.PI) + yaxis.unit));
				GraphLabel label = new GraphLabel(yaxis.label,
						xlshift + ytickwidth + style.gapAxisLabel + style.labelMetrics.getHeight()
								- style.labelMetrics.getDescent(),
						canvas.y + (canvas.height + style.labelMetrics.getHeight()
								+ style.labelMetrics.stringWidth(yaxis.label)) / 2);
				label.vertical = true;
				axisLabels.add(label);
			}
		}
		int nLabels = labels.size();
		if (nLabels >= 4) {
			// the labels only need proper positioning - for now we only deal with the first
			// four: lower-left, upper-left, upper-right, lower-right
			int ytop = -style.gapAxisLabel;
			int ybottom = canvas.height + style.gapAxisLabel + style.labelMetrics.getHeight()
					- style.labelMetrics.getDescent() - 1;
			if (xaxis.majorTicks >= 0)
				ybottom += style.tickMajorLength + style.gapTickLabel + style.tickMetrics.getHeight();
			else if (xaxis.minorTicks > 0)
				ybottom += style.tickMajorLength;
			// note: how to determine the sequence of labels? - this must be specified in a
			// flexible way!
			GraphLabel label = labels.get(0);
			label.setLocation(-style.labelMetrics.stringWidth(label.label) / 2, ybottom); // lower-left
			label = labels.get(3);
			label.setLocation(-style.labelMetrics.stringWidth(label.label) / 2, ytop); // upper-left
			label = labels.get(2);
			label.setLocation(canvas.width - style.labelMetrics.stringWidth(label.label) / 2, ytop); // upper-right
			label = labels.get(1);
			label.setLocation(canvas.width - style.labelMetrics.stringWidth(label.label) / 2, ybottom); // lower-right
		}
	}

	protected void formatGrid() {
		grid.reset();
		if (xaxis.enabled && xaxis.grid > 0) {
			double dx = (double) canvas.width / (double) (xaxis.grid + 1);
			int n = 1;
			for (int i = 0; i < xaxis.grid; i++) {
				int xshift = canvas.x + (int) (n * dx + 0.5);
				grid.moveTo(xshift, canvas.y);
				grid.lineTo(xshift, canvas.y + canvas.height);
				n++;
			}
		}
		if (yaxis.enabled && yaxis.grid > 0) {
			double dy = (double) canvas.height / (double) (yaxis.grid + 1);
			int n = 1;
			for (int i = 0; i < yaxis.grid; i++) {
				int yshift = canvas.y + (int) (n * dy + 0.5);
				grid.moveTo(canvas.x, yshift);
				grid.lineTo(canvas.x + canvas.width, yshift);
				n++;
			}
		}
	}

	protected void formatTicks() {
		// format x-axis
		int yshift = canvas.y + canvas.height;
		// int ylshift =
		// yshift+style.tickMajorLength+style.gapTickLabel+style.tickMetrics.getHeight();
		int xshift;
		int n = 0;

		if (xaxis.enabled) {
			double dx = (double) canvas.width / (double) ((xaxis.majorTicks + 1) * (xaxis.minorTicks + 1));

			if (xaxis.majorTicks >= 0) {
				xshift = canvas.x - (int) style.frameStrokeWidth;
				frame.moveTo(xshift, yshift);
				frame.lineTo(xshift, yshift + style.tickMajorLength);
				n++;

				for (int i = 0; i <= xaxis.majorTicks; i++) {
					for (int j = 0; j < xaxis.minorTicks; j++) {
						xshift = canvas.x - (int) style.frameStrokeWidth + (int) (n * dx + 0.5);
						frame.moveTo(xshift, yshift);
						frame.lineTo(xshift, yshift + style.tickMinorLength);
						n++;
					}
					xshift = canvas.x - (int) style.frameStrokeWidth + (int) (n * dx + 0.5);
					frame.moveTo(xshift, yshift);
					frame.lineTo(xshift, yshift + style.tickMajorLength);
					n++;
				}
			} else {
				for (int j = 0; j < xaxis.minorTicks; j++) {
					xshift = canvas.x - (int) style.frameStrokeWidth + (int) (n * dx + 0.5);
					frame.moveTo(xshift, yshift);
					frame.lineTo(xshift, yshift + style.tickMajorLength);
					n++;
				}
			}
		}

		// format y-axis
		if (yaxis.enabled) {
			double dy;
			if (yaxis.logScale)
				dy = Math.log10(canvas.height + style.frameStrokeWidth)
						/ ((yaxis.majorTicks + 1) * (yaxis.minorTicks + 1));
			else
				dy = (canvas.height + style.frameStrokeWidth) / ((yaxis.majorTicks + 1) * (yaxis.minorTicks + 1));
			n = 0;
			xshift = canvas.x + canvas.width;
			// int xlshift = xshift+style.tickMajorLength+style.gapTickLabel;
			if (yaxis.majorTicks >= 0) {
				yshift = canvas.y - (int) style.frameStrokeWidth;
				frame.moveTo(xshift, yshift);
				frame.lineTo(xshift + style.tickMajorLength, yshift);
				n++;

				for (int i = 0; i <= yaxis.majorTicks; i++) {
					for (int j = 0; j < yaxis.minorTicks; j++) {
						if (yaxis.logScale)
							yshift = canvas.y - (int) style.frameStrokeWidth + (int) (Math.pow(10.0, n * dy) + 0.5);
						else
							yshift = canvas.y - (int) style.frameStrokeWidth + (int) (n * dy + 0.5);
						frame.moveTo(xshift, yshift);
						frame.lineTo(xshift + style.tickMinorLength, yshift);
						n++;
					}
					if (yaxis.logScale)
						yshift = canvas.y - (int) style.frameStrokeWidth + (int) (Math.pow(10.0, n * dy) + 0.5);
					else
						yshift = canvas.y - (int) style.frameStrokeWidth + (int) (n * dy + 0.5);
					frame.moveTo(xshift, yshift);
					frame.lineTo(xshift + style.tickMajorLength, yshift);
					n++;
				}
			} else {
				for (int j = 0; j < yaxis.minorTicks; j++) {
					if (yaxis.logScale)
						yshift = canvas.y - (int) style.frameStrokeWidth + (int) (Math.pow(10.0, n * dy) + 0.5);
					else
						yshift = canvas.y - (int) style.frameStrokeWidth + (int) (n * dy + 0.5);
					frame.moveTo(xshift, yshift);
					frame.lineTo(xshift + style.tickMajorLength, yshift);
					n++;
				}
			}
		}
		formatTickLabels();
	}

	// note: method is synchronized to prevent concurrent modification exceptions
	// on tickLabels when restoring state, see paintComponent below;
	// too much effort to get to the root of this for an increasingly outdated JRE
	// frontend.
	protected synchronized void formatTickLabels() {
		tickLabels.clear();
		int yshift = canvas.y + canvas.height;
		int ylshift = yshift + style.tickMajorLength + style.gapTickLabel + style.tickMetrics.getHeight()
				- style.tickMetrics.getDescent();
		int xshift = canvas.x;
		if (xaxis.enabled && xaxis.majorTicks >= 0) {
			int xincr = canvas.width / (xaxis.majorTicks + 1);
			double xstep = (xaxis.upper - xaxis.lower) / (xaxis.majorTicks + 1);
			double xtick = xaxis.lower;
			tickLabels.add(new GraphLabel((xaxis.formatter.format(xtick) + xaxis.unit), xshift, ylshift));
			xshift += xincr;

			for (int i = 0; i <= xaxis.majorTicks; i++) {
				xtick += xstep;
				String label = xaxis.formatter.format(xtick) + xaxis.unit;
				if (i == xaxis.majorTicks)
					tickLabels.add(new GraphLabel(label, xshift - style.tickMetrics.stringWidth(label), ylshift));
				else
					tickLabels
							.add(new GraphLabel(label, xshift - 2 * style.tickMetrics.stringWidth(label) / 3, ylshift));
				xshift += xincr;
			}
		}
		yshift = canvas.y;
		xshift = canvas.x + canvas.width;
		int xlshift = xshift + style.tickMajorLength + style.gapTickLabel;
		if (yaxis.enabled && yaxis.majorTicks >= 0) {
			int yincr = canvas.height / (yaxis.majorTicks + 1);
			double ystep = (yaxis.upper - yaxis.lower) / (yaxis.majorTicks + 1);
			double ytick = yaxis.upper;
			int voff = style.tickMetrics.getHeight() / 2 - 1;
			tickLabels.add(new GraphLabel((yaxis.formatter.format(ytick) + yaxis.unit), xlshift, yshift + 2 * voff));
			yshift += yincr;

			for (int i = 0; i <= yaxis.majorTicks; i++) {
				ytick -= ystep;
				if (i == yaxis.majorTicks)
					tickLabels.add(new GraphLabel(yaxis.formatter.format(ytick) + yaxis.unit, xlshift, yshift));
				else
					tickLabels.add(new GraphLabel(yaxis.formatter.format(ytick) + yaxis.unit, xlshift, yshift + voff));
				yshift += yincr;
			}
		}
	}

	public void setMessage(String msg) {
		boolean msgChanged = (msg != null && this.msg == null) || (msg == null && this.msg != null);
		this.msg = msg;
		if (msgChanged) {
			initGraphRect(getBounds());
			init();
		}
	}

	public void clearMessage() {
		setMessage(null);
	}

	public String getMessage() {
		return msg;
	}

	public void addAnnotation(String label, int x, int y) {
		addAnnotation(label, x, y, Color.black);
	}

	public void addAnnotation(String label, int x, int y, Color color) {
		if (label == null)
			return;
		annotations.add(new GraphLabel(label, x, y, color));
	}

	public void resetAnnotations() {
		annotations.clear();
	}

	public void addThreshold(double level, Color color) {
		thresholds.add(new FrameThreshold(level, color));
	}

	public void resetThresholds() {
		thresholds.clear();
	}

	public boolean updateYThreshold(int idx, double level, Color color) {
		if (Double.isFinite(level)) {
			if (thresholds.size() <= idx)
				return false;
			thresholds.remove(idx);
			return true;
		}
		if (thresholds.size() <= idx) {
			addThreshold(level, color);
			return true;
		}
		boolean changed = false;
		FrameThreshold t = thresholds.get(idx);
		if (Math.abs(level - t.level) > 1e-8)
			changed = true;
		if (!color.equals(t.color))
			changed = true;
		if (changed)
			thresholds.set(idx, new FrameThreshold(level, color));
		return changed;
	}

	protected void zoom(double xmin, double xmax, double ymin, double ymax) {
		formatTickLabels();
	}

	@Override
	protected void paintComponent(Graphics g) {
		Graphics2D g2 = (Graphics2D) g;
		if (ToggleAntiAliasingAction.sharedInstance().getAntiAliasing())
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		else
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_DEFAULT);
		g2.setStroke(style.frameStroke);
		g2.setPaint(Color.black);

		if (msg != null) {
			g2.draw(outline);
			Font msgFont = style.labelFont.deriveFont(2f * style.labelFont.getSize2D());
			FontMetrics msgMetrics = getFontMetrics(msgFont);
			int msgWidth = msgMetrics.stringWidth(msg);
			while (msgWidth + 10 > canvas.width) {
				double scale = 0.840896415254; // 2^(-1/4)
				msgFont = msgFont.deriveFont((float) (scale * msgFont.getSize2D()));
				msgMetrics = getFontMetrics(msgFont);
				msgWidth = msgMetrics.stringWidth(msg);
			}
			g2.setFont(msgFont);
			g2.drawString(msg, canvas.x + (canvas.width - msgWidth) / 2, canvas.y + canvas.height / 2);
			return;
		}

		g2.draw(frame);
		g2.setPaint(style.gridcolor);
		g2.draw(grid);
		g2.setPaint(Color.black);
		g2.setFont(style.tickFont);
		// note: the synchronized block is a hack to prevent concurrent modification
		// exceptions
		// on tickLabels when restoring state; too much effort to get to the root of
		// this
		// for an increasingly outdated JRE frontend.
		synchronized (this) {
			for (GraphLabel t : tickLabels) {
				g2.setPaint(t.color);
				g2.drawString(t.label, t.x, t.y);
			}
			g2.setFont(style.labelFont);
			for (GraphLabel t : axisLabels) {
				g2.setPaint(t.color);
				if (t.vertical)
					drawVerticalString(g2, t.label, t.x, t.y);
				else
					g2.drawString(t.label, t.x, t.y);
			}
			for (GraphLabel t : labels) {
				g2.setPaint(t.color);
				if (t.vertical)
					drawVerticalString(g2, t.label, canvas.x + t.x, canvas.y + t.y);
				else
					g2.drawString(t.label, canvas.x + t.x, canvas.y + t.y);
			}
			for (GraphLabel t : annotations) {
				g2.setPaint(t.color);
				if (t.vertical)
					drawVerticalString(g2, t.label, canvas.x + t.x, canvas.y + t.y);
				else
					g2.drawString(t.label, canvas.x + t.x, canvas.y + t.y);
			}
			for (FrameThreshold t : thresholds) {
				g2.setPaint(t.color);
				int ycoord = convertYcanvas(t.level);
				g2.drawLine(canvas.x, ycoord, canvas.x + canvas.width - FRAME_STROKE, ycoord);
			}
		}
		if (mousedrag != null) {
			g2.setColor(style.zoomframe);
			g2.drawRect(mousedrag.x, mousedrag.y, mousedrag.width, mousedrag.height);
		}
	}

	private void drawVerticalString(Graphics2D g2, String label, int x, int y) {
		// TextLayout vText = new TextLayout(label, style.labelFont, new
		// FontRenderContext(null, true, false));
		TextLayout vText = new TextLayout(label, style.labelFont, g2.getFontRenderContext());
		AffineTransform at = g2.getTransform();
		g2.translate(x, y);
		g2.rotate(-Math.PI / 2.0);
		vText.draw(g2, 0, 0);
		g2.setTransform(at);
	}

	public int convertX(double coord) {
		return (int) ((coord - xaxis.lower) / (xaxis.upper - xaxis.lower) * (canvas.width - 2));
	}

	public double convertX2D(double coord) {
		return (coord - xaxis.lower) / (xaxis.upper - xaxis.lower) * (canvas.width - 2);
	}

	public int convertXcanvas(double coord) {
		return canvas.x + convertX(coord);
	}

	public double convertY2D(double coord) {
		return (canvas.height - 1) - (coord - yaxis.lower) / (yaxis.upper - yaxis.lower) * (canvas.height - 2);
	}

	public int convertY(double coord) {
		return canvas.height - 1 - (int) ((coord - yaxis.lower) / (yaxis.upper - yaxis.lower) * (canvas.height - 2));
	}

	public int convertYcanvas(double coord) {
		return canvas.y + convertY(coord);
	}
}
