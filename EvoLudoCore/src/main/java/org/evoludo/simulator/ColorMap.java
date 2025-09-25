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

package org.evoludo.simulator;

import java.awt.Color;
import java.util.Arrays;

import org.evoludo.math.ArrayMath;
import org.evoludo.simulator.models.IBSMCPopulation;
import org.evoludo.simulator.models.PDE;

/**
 * Interface for mapping data to colors.
 * <p>
 * Colors are handled very differently in JRE and GWT as well as for HTML
 * <code>canvas</code>'es and WebGL. This class provides a unified interface,
 * which hides the implementation details.
 * <p>
 * <strong>Note:</strong>
 * <ol>
 * <li>the interface provides a number of useful methods to blend colors.
 * <li>the implementation of all methods is optional because not all may be
 * adequate for every data-to-color mapping.
 * <li>the <code>translate(...)</code> methods fill in arrays of type
 * <code>Object[]</code> or return <code>Object</code>'s, respectively. This
 * ambiguity is required to render the interface agnostic to the different
 * implementations.
 * <li>Color is emulated in GWT and can be used to set and manipulate colors but
 * needs to be converted to to CSS (for <code>canvas</code>) or to Material (for
 * WebGL), respectively, before applying to graphics or the GUI.
 * </ol>
 * 
 * @author Christoph Hauert
 *
 * @param <T> type of color object: String or
 *            {@link thothbot.parallax.core.shared.materials.MeshLambertMaterial
 *            MeshLambertMaterial} for GWT and
 *            {@link Color} for JRE
 * 
 * @see org.evoludo.simulator.ColorMapCSS ColorMapCSS
 * @see org.evoludo.simulator.ColorMap3D ColorMap3D
 * @see "Also consult GWT emulation of Color in org.evoludo.emulate.java.awt.Color"
 */
public abstract class ColorMap<T extends Object> {

	/**
	 * Useful constant for converting <code>int</code> representations of color
	 * channels (<code>0-255</code>) to floating point representations
	 * (<code>0-1</code>).
	 */
	protected static final double INV255 = 0.003921568627451;

	/**
	 * Translate the <code>data</code> array of <code>int</code> values to colors
	 * and store the results in the <code>color</code> array. The type of the
	 * <code>color</code> array depends on the implementation.
	 * <p>
	 * <strong>Note:</strong> for performance reasons no validity checks are
	 * performed on the data. If any data entry is negative or
	 * <code>&gt;nColors</code> an {@linkplain ArrayIndexOutOfBoundsException} is
	 * thrown.
	 * 
	 * @param data  the <code>int[]</code> array to convert to colors
	 * @param color the array for the resulting colors
	 * @return <code>true</code> if translation successful
	 */
	public boolean translate(int[] data, T[] color) {
		throw new Error("ColorMap.translate(int[], T[]) not implemented!");
	}

	/**
	 * Translate the multi-trait <code>double[]</code> array <code>data</code> to a
	 * color. The type of object returned depends on the implementation.
	 * 
	 * @param data the <code>double[]</code> array to convert to a color
	 * @return the color object
	 */
	public T translate(double[] data) {
		throw new Error("ColorMap.translate(double[]) not implemented!");
	}

	/**
	 * Translate the <code>data</code> array of <code>double</code> values to colors
	 * and store the results in the <code>color</code> array. The type of the
	 * <code>color</code> array depends on the implementation.
	 * <p>
	 * <strong>Note:</strong> whether <code>data</code> refers to a single or
	 * multiple traits is up to the implementation to decide. For example,
	 * {@link org.evoludo.simulator.models.IBSMCPopulation CXPopulation} stores
	 * multiple
	 * traits in a linear array.
	 * 
	 * @param data  the <code>double[]</code> array to convert to colors
	 * @param color the array for the resulting colors
	 * @return <code>true</code> if translation successful
	 */
	public boolean translate(double[] data, T[] color) {
		throw new Error("ColorMap.translate(double[], T[]) not implemented!");
	}

	/**
	 * Translate the <code>data</code> array of <code>double[]</code> multi-trait
	 * values to colors and store the results in the <code>color</code> array. The
	 * type of the <code>color</code> array depends on the implementation.
	 * 
	 * @param data  the <code>double[][]</code> array to convert to colors
	 * @param color the array for the resulting colors
	 * @return <code>true</code> if translation successful
	 */
	public boolean translate(double[][] data, T[] color) {
		throw new Error("ColorMap.translate(double[][], T[]) not implemented!");
	}

	/**
	 * Translate the <code>data1</code> and <code>data2</code> arrays of
	 * <code>double[]</code> multi-trait values to colors and store the results in
	 * the <code>color</code> array. The type of the <code>color</code> array
	 * depends on the implementation.
	 * <p>
	 * For example, frequencies and fitnesses, say <code>data1</code> and
	 * <code>data2</code>, respectively, yield the average fitness as
	 * <code>data1&middot;data2</code> (dot product), which then gets converted to a
	 * color.
	 * 
	 * @param data1 the first <code>double[]</code> array to convert to colors
	 * @param data2 the second <code>double[]</code> array to convert to colors
	 * @param color the array for the resulting colors
	 * @return <code>true</code> if translation successful
	 */
	public boolean translate(double[][] data1, double[][] data2, T[] color) {
		throw new Error("ColorMap.translate(double[][], double[][], T[]) not implemented!");
	}

	/**
	 * Utility method for adding the <span style="color:red;">red</span>,
	 * <span style="color:green;">green</span>,
	 * <span style="color:blue;">blue</span> and alpha components of two colors.
	 * 
	 * @param one the first color
	 * @param two the second color
	 * @return component-wise addition of the two colors
	 */
	public static Color addColors(Color one, Color two) {
		return new Color(Math.min(one.getRed() + two.getRed(), 255), Math.min(one.getGreen() + two.getGreen(), 255),
				Math.min(one.getBlue() + two.getBlue(), 255), Math.min(one.getAlpha() + two.getAlpha(), 255));
	}

	/**
	 * Utility method to add (or override) the transparency of color {@code color}.
	 * 
	 * @param color the color to add transparency
	 * @param alpha the new transparency (0: opaque, 255: transparent)
	 * @return the new translucent color
	 */
	public static Color addAlpha(Color color, int alpha) {
		return new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
	}

	/**
	 * Utility method to add (or override) the transparency in the array
	 * {@code color}.
	 * 
	 * @param colors the color array to add transparency
	 * @param alpha  the new transparency (0: opaque, 255: transparent)
	 * @return the new translucent color
	 */
	public static Color[] addAlpha(Color[] colors, int alpha) {
		for (Color color : colors)
			addAlpha(color, alpha);
		return colors;
	}

	/**
	 * Utility method for the smooth, component-wise blending of two colors, where
	 * color <code>one</code> has weight <code>w1</code> (and color <code>two</code>
	 * has weight <code>(1-w1)</code>) and includes the alpha-channel.
	 * 
	 * @param one the first color
	 * @param two the second color
	 * @param w1  the weight of color <code>one</code>
	 * @return component-wise addition of the two colors
	 */
	public static Color blendColors(Color one, Color two, double w1) {
		double w2 = 1.0 - w1;
		return new Color(Math.min((int) (w1 * one.getRed() + w2 * two.getRed()), 255),
				Math.min((int) (w1 * one.getGreen() + w2 * two.getGreen()), 255),
				Math.min((int) (w1 * one.getBlue() + w2 * two.getBlue()), 255),
				Math.min((int) (w1 * one.getAlpha() + w2 * two.getAlpha()), 255));
	}

	/**
	 * Utility method for the smooth, component-wise blending of multiple
	 * <code>colors</code> with respective <code>weights</code> (includes
	 * alpha-channel).
	 * 
	 * @param colors  the colors for blending
	 * @param weights the weights of each color
	 * @return component-wise blending of <code>colors</code>
	 */
	public static Color blendColors(Color[] colors, double[] weights) {
		int n = weights.length;
		if (colors.length < n)
			return Color.WHITE;
		// assume that weights are normalized to avoid overhead
		double red = 0.0;
		double green = 0.0;
		double blue = 0.0;
		double alpha = 0.0;
		for (int i = 0; i < n; i++) {
			double w = weights[i];
			Color ci = colors[i];
			red += w * ci.getRed();
			green += w * ci.getGreen();
			blue += w * ci.getBlue();
			alpha += w * ci.getAlpha();
		}
		return new Color(Math.min((int) red, 255), Math.min((int) green, 255), Math.min((int) blue, 255),
				Math.min((int) alpha, 255));
	}

	/**
	 * Utility method to convert the {@code color} into a color object of type
	 * {@code T}.
	 * 
	 * @param color the color to convert
	 * @return the color represented as an object of type {@code T}
	 */
	public abstract T color2Color(Color color);

	/**
	 * Associates integer indices with colors.
	 * 
	 * @param <T> type of color object: String or
	 *            {@link thothbot.parallax.core.shared.materials.MeshLambertMaterial
	 *            MeshLambertMaterial} for GWT and
	 *            {@link Color} for JRE
	 * 
	 * @see org.evoludo.simulator.ColorMapCSS ColorMapCSS
	 * @see org.evoludo.simulator.ColorMap3D ColorMap3D
	 * @see "Also consult GWT emulation of Color in org.evoludo.emulate.java.awt.Color"
	 */
	public abstract static class Index<T> extends ColorMap<T> {

		/**
		 * Array of generic colors <code>T</code>.
		 */
		protected T[] colors;

		/**
		 * This is currently only needed to pass something to
		 * {@link #toGradient1D(int)}.
		 */
		protected Color[] srccolors;

		/**
		 * Number of colors
		 */
		protected final int nColors;

		/**
		 * Constructs a new index color map.
		 * 
		 * @param size the number of colors
		 */
		protected Index(int size) {
			nColors = size;
		}

		/**
		 * Constructs a new index color map with transparency <code>alpha</code>.
		 * 
		 * @param srccolors the indexed colors
		 * @param alpha     the transparency of the colors
		 * @param colors    the array to store the colors
		 */
		protected Index(Color[] srccolors, int alpha, T[] colors) {
			this.srccolors = srccolors;
			this.colors = colors;
			nColors = colors.length;
			for (int n = 0; n < nColors; n++)
				setColor(n, srccolors[n], alpha);
		}

		/**
		 * Get the array of colors tha backs this color map.
		 * 
		 * @return the array of colors
		 */
		public T[] getColors() {
			return colors;
		}

		/**
		 * Assign a new <code>color</code> to <code>index</code>.
		 * 
		 * @param index the index of the <code>color</code>
		 * @param color the new color for <code>index</code>
		 */
		public void setColor(int index, Color color) {
			setColor(index, color, 255);
		}

		/**
		 * Assign a new <code>color</code> to <code>index</code> with transparency
		 * <code>alpha</code>.
		 * <p>
		 * <strong>Note:</strong> the transparency of the supplied color is always
		 * honoured.
		 * 
		 * @param index the index of the <code>color</code>
		 * @param color the new color for <code>index</code>
		 * @param alpha the transparency of the new color
		 */
		public void setColor(int index, Color color, int alpha) {
			if (index < 0 || index >= nColors)
				return;
			if (color.getAlpha() < 255)
				this.colors[index] = color2Color(color);
			else
				this.colors[index] = color2Color(addAlpha(color, alpha));
		}

		@Override
		public boolean translate(int[] data, T[] color) {
			int len = data.length;
			for (int n = 0; n < len; n++)
				color[n] = colors[data[n]];
			return true;
		}

		/**
		 * Convert the index colors into a 1D gradient with a total of {@code nIncr}
		 * shades.
		 * 
		 * @param nIncr the total number of color increments
		 * @return the 1D color gradient
		 */
		public abstract ColorMap.Gradient1D<T> toGradient1D(int nIncr);
	}

	/**
	 * Abstract super class for color maps following a gradient.
	 * 
	 * @param <T> type of color object: String or
	 *            {@link thothbot.parallax.core.shared.materials.MeshLambertMaterial
	 *            MeshLambertMaterial} for GWT and {@link Color} for JRE
	 * 
	 * @see org.evoludo.simulator.ColorMapCSS ColorMapCSS
	 * @see org.evoludo.simulator.ColorMap3D ColorMap3D
	 * @see "Also consult GWT emulation of Color in org.evoludo.emulate.java.awt.Color"
	 */
	public abstract static class Gradient<T> extends ColorMap<T> {

		/**
		 * Utility method for creating a smooth gradient ranging from color
		 * <code>start</code> to color <code>end</code> in <code>last-first</code>
		 * steps. The color gradient is stored in the array <code>gradient</code> from
		 * elements <code>first</code> to <code>last</code> (inclusive).
		 * 
		 * @param gradient the array for storing the color gradient
		 * @param start    the starting color
		 * @param first    the index in <code>gradient</code> for the <code>start</code>
		 *                 color
		 * @param end      the end color
		 * @param last     the index in <code>gradient</code> for the <code>end</code>
		 *                 color
		 */
		public void interpolateColors(T[] gradient, Color start, int first, Color end, int last) {
			for (int n = first; n < last; n++) {
				double x = (double) (n - first) / (double) (last - first);
				gradient[n] = color2Color(blendColors(end, start, x));
			}
			gradient[last] = color2Color(end);
		}

		/**
		 * Utility method for creating a smooth gradient spanning all colors in the
		 * array <code>colors</code>. An equal number of shades are created between two
		 * adjacent colors. The color gradient is stored in the array
		 * <code>gradient</code>.
		 * 
		 * @param gradient the array for storing the color gradient
		 * @param colors   the array of colors for the gradient
		 */
		public void interpolateColors(T[] gradient, Color[] colors) {
			int parts = colors.length - 1;
			int nGradient = gradient.length - 1;
			for (int n = 0; n < parts; n++)
				interpolateColors(gradient, colors[n], n * nGradient / parts, colors[n + 1],
						(n + 1) * nGradient / parts);
		}

		/**
		 * Translate the <code>double</code> value <code>data</code> to a color
		 * gradient. The type of object returned depends on the implementation.
		 * 
		 * @param data the <code>double</code> value to convert to a color
		 * @return the color object
		 */
		public T translate(double data) {
			throw new Error("ColorMap.translate(double) not implemented!");
		}
	}

	/**
	 * Color gradient following the hue.
	 * <p>
	 * The actual data-to-color translation is performed in the super class.
	 * 
	 * @param <T> type of color object: String or
	 *            {@link thothbot.parallax.core.shared.materials.MeshLambertMaterial
	 *            MeshLambertMaterial} for GWT and
	 *            {@link Color} for JRE
	 * 
	 * @see org.evoludo.simulator.ColorMapCSS
	 * @see org.evoludo.simulator.ColorMap3D
	 * @see "Also consult GWT emulation of Color in org.evoludo.emulate.java.awt.Color"
	 */
	public abstract static class Hue<T> extends Gradient1D<T> {

		/**
		 * Construct a new Hue color map, starting at a hue of <code>0.0</code> (red) up
		 * to hue <code>1.0</code> (red, again) and interpolate with
		 * <code>steps-1</code> intermediate colors. The resulting gradient spans
		 * <code>steps+1</code> colors. The default range for mapping data values onto
		 * the color gradient is <code>[0.0, 1.0]</code>.
		 * <p>
		 * <strong>Important:</strong> Cannot allocate the array for the gradient. Must
		 * be handled by subclasses.
		 * 
		 * @param start     the starting hue
		 * @param end       the ending hue
		 * @param ascending the flag to indicate if hue is increasing
		 * @param colors    the array to store the hue colors
		 * 
		 * @see #setRange(double, double)
		 * @see #setHueRange(double, double)
		 */
		protected Hue(double start, double end, boolean ascending, T[] colors) {
			super(colors);
			setHueRange(start, end, ascending);
		}

		/**
		 * Set hues ranging from {@code start} to {@code end} in increasing colors.
		 * 
		 * <strong>Notes:</strong>
		 * <ol>
		 * <li>If <code>end&le;start</code> the colors warp around through red.
		 * <li>The saturation and brightness of the gradient are both maximal.
		 * </ol>
		 * 
		 * @param start the starting hue
		 * @param end   the ending hue
		 */
		public void setHueRange(double start, double end) {
			setHueRange(start, end, true);
		}

		/**
		 * Set the hues ranging from {@code start} to {@code end} in increasing or
		 * decreasing colors.
		 * 
		 * <strong>Notes:</strong>
		 * <ol>
		 * <li>If <code>end&le;start</code> the colors warp around through red.
		 * <li>If <code>ascending == false</code> the hue is interpolated in
		 * reverse.
		 * <li>The saturation and brightness of the gradient are both maximal.
		 * </ol>
		 * 
		 * @param start     the starting hue
		 * @param end       the ending hue
		 * @param ascending {@code true} to increment the hue value
		 */
		public void setHueRange(double start, double end, boolean ascending) {
			double huevalue = start;
			double hueincr = (ascending ? 1.0 : -1.0) * ((end > start) ? (end - start) : (end + 1.0 - start))
					/ nGradient;
			for (int c = 0; c <= nGradient; c++) {
				// cast creates unnecessary overhead in GWT but not worth the trouble to resolve
				// because the hue range is unlikely to change constantly
				gradient[c] = color2Color(new Color(Color.HSBtoRGB((float) huevalue, 1f, 1f)));
				huevalue += hueincr;
			}
		}
	}

	/**
	 * One dimensional color gradient spanning two or more colors
	 * 
	 * @param <T> type of color object: String or
	 *            {@link thothbot.parallax.core.shared.materials.MeshLambertMaterial
	 *            MeshLambertMaterial} for GWT and {@link Color} for JRE
	 * 
	 * @see org.evoludo.simulator.ColorMapCSS ColorMapCSS
	 * @see org.evoludo.simulator.ColorMap3D ColorMap3D
	 * @see "Also consult GWT emulation of Color in org.evoludo.emulate.java.awt.Color"
	 */
	public abstract static class Gradient1D<T> extends Gradient<T> {

		/**
		 * Reference to pre-allocated gradient colors.
		 */
		protected T[] gradient;

		/**
		 * Number of colors in the gradient: <code>nGradient+1</code>.
		 */
		protected final int nGradient;

		/**
		 * Minimum data value. This value is mapped onto color <code>gradient[0]</code>.
		 */
		protected double min;

		/**
		 * Helper variable to make the mapping from a range of data values to indices of
		 * the gradient array more efficient: <code>map=nBins/(max-min)</code>, where
		 * <code>max</code> maximum data value to be mapped.
		 */
		protected double map;

		/**
		 * The index of the trait that this gradient refers to (or {@code -1} if not
		 * applicable).
		 */
		protected final int trait;

		/**
		 * For internal use only. Construct color gradient running through all the
		 * colors in the array <code>colors</code> for the trait with index
		 * {@code trait} (or {@code -1} if not applicable). The gradient colors of type
		 * {@code T} are stored in {@code gradient}. The number of interpolated
		 * intermediate colors between two subsequent entries in <code>colors</code> is
		 * <code>(gradient.length-1)/(N-1)</code>, where <code>N</code> is the number of
		 * colors in <code>colors</code>. The default range for mapping data values onto
		 * the color gradient is <code>[0.0, 1.0]</code>.
		 * 
		 * @param colors   the equally spaced reference colors of the gradient
		 * @param trait    the trait index for the color gradient
		 * @param gradient the array to store the gradient colors
		 * 
		 * @see #setRange(double, double)
		 */
		protected Gradient1D(Color[] colors, int trait, T[] gradient) {
			this(gradient, trait);
			setGradient(colors, trait);
		}

		/**
		 * For internal use only. Initializes gradient based on the supplied array
		 * {@code gradient}
		 * of type {@code T}.
		 * 
		 * @param gradient the array to store the gradient colors
		 */
		protected Gradient1D(T[] gradient) {
			this(gradient, -1);
		}

		/**
		 * For internal use only. Initializes gradient based on the supplied array
		 * {@code gradient}
		 * of type {@code T} for trait with index {@code trait}. The default range for
		 * mapping data values onto the color gradient is <code>[0.0, 1.0]</code>.
		 * 
		 * @param gradient the array to store the gradient colors
		 * @param trait    the trait index for the color gradient
		 * 
		 * @see #setRange(double, double)
		 */
		protected Gradient1D(T[] gradient, int trait) {
			this.gradient = gradient;
			this.trait = trait;
			nGradient = gradient.length - 1;
			setRange(0.0, 1.0);
		}

		/**
		 * Assign a new <code>color</code> to the gradient for <code>value</code>.
		 * 
		 * @param value the value of the <code>color</code>
		 * @param color the new color for <code>value</code>
		 */
		public void setColor(double value, Color color) {
			setColor(value, color, 255);
		}

		/**
		 * Assign a new <code>color</code> to the gradient for <code>value</code> with
		 * transparency <code>alpha</code>.
		 * <p>
		 * <strong>Note:</strong> the transparency of the supplied color is always
		 * honoured but overruled if {@code alpha != 255}.
		 * 
		 * @param value the value of the <code>color</code>
		 * @param color the new color for <code>value</code>
		 * @param alpha the transparency of the new color
		 */
		public void setColor(double value, Color color, int alpha) {
			int bin = binOf(value);
			if (bin < 0)
				return;
			if (alpha == 255 && color.getAlpha() < 255)
				gradient[bin] = color2Color(color);
			else
				gradient[bin] = color2Color(addAlpha(color, alpha));
		}

		/**
		 * Utility method to calculate the index of the gradient color that corresponds
		 * to {@code value}. Returns {@code -1} if {@code value} is invalid or lies
		 * outside the range of the gradient.
		 * 
		 * @param value the value to convert to a bin index of the gradient
		 * @return the index of the bin or {@code -1}
		 */
		protected int binOf(double value) {
			if (!Double.isFinite(value)) // protect against NaN
				return -1;
			int bin = (int) Math.rint((value - min) * map);
			if (bin > nGradient)
				return -1;
			return bin;
		}

		/**
		 * Sets the gradient to span the colors in the array {@code colors}.
		 * 
		 * @param colors the array of colors for the gradient
		 */
		public void setGradient(Color[] colors) {
			setGradient(colors, 0);
		}

		/**
		 * Sets the gradient for the trait with index {@code trait} to span the colors
		 * in the array {@code colors}.
		 * 
		 * @param colors the array of colors for the gradient
		 * @param trait  the trait index for the color gradient
		 */
		public void setGradient(Color[] colors, int trait) {
			int parts = colors.length - 1;
			for (int n = 0; n < parts; n++)
				interpolateColors(gradient, colors[n], n * nGradient / parts, colors[n + 1],
						(n + 1) * nGradient / parts);
		}

		/**
		 * Sets the range of values spanned by the gradient to {@code [min,max]}.
		 * 
		 * @param min the minimum value of the gradient
		 * @param max the maximum value of the gradient
		 */
		public void setRange(double min, double max) {
			this.min = min;
			map = max <= min ? 0.0 : nGradient / (max - min);
		}

		@Override
		public T translate(double data) {
			return gradient[binOf(data)];
		}

		/**
		 * {@inheritDoc}
		 * <p>
		 * <strong>Implementation:</strong> For performance reasons no validity checks
		 * on
		 * <code>data</code>. In particular, all data entries must lie inside the range
		 * for mapping data values.
		 */
		@Override
		public boolean translate(double[] data, T[] color) {
			int len = data.length;
			for (int n = 0; n < len; n++)
				color[n] = gradient[binOf(data[n])];
			return true;
		}

		/**
		 * {@inheritDoc}
		 * <p>
		 * <strong>Implementation:</strong>
		 * <ol>
		 * <li>Each entry in <code>data</code> is assumed to be one dimensional (only
		 * first entry used) and converted to the corresponding gradient color and
		 * returned in the <code>color</code> array.
		 * <li>For performance reasons no validity checks on <code>data</code>. In
		 * particular, all data entries must lie inside the range for mapping data
		 * values.
		 * </ol>
		 * 
		 * @see PDE
		 */
		@Override
		public boolean translate(double[][] data, T[] color) {
			int len = data.length;
			for (int n = 0; n < len; n++)
				color[n] = gradient[binOf(data[n][trait])];
			return true;
		}

		/**
		 * {@inheritDoc}
		 * 
		 * @see PDE
		 */
		@Override
		public boolean translate(double[][] data1, double[][] data2, T[] color) {
			int len = color.length;
			for (int n = 0; n < len; n++)
				color[n] = translate(ArrayMath.dot(data1[n], data2[n]));
			return true;
		}
	}

	/**
	 * Two dimensional color gradient with one color for each dimension.
	 * 
	 * @param <T> type of color object: String or
	 *            {@link thothbot.parallax.core.shared.materials.MeshLambertMaterial
	 *            MeshLambertMaterial} for GWT and
	 *            {@link Color} for JRE
	 * 
	 * @see org.evoludo.simulator.ColorMapCSS ColorMapCSS
	 * @see org.evoludo.simulator.ColorMap3D ColorMap3D
	 * @see "Also consult GWT emulation of Color in org.evoludo.emulate.java.awt.Color"
	 */
	public abstract static class Gradient2D<T> extends Gradient<T> {

		/**
		 * Reference to pre-allocated gradient colors.
		 */
		protected T[][] gradient;

		/**
		 * Number of colors in the gradient: <code>nGradient+1</code>.
		 */
		protected final int nGradient;

		/**
		 * Number of traits in the gradient: <code>nTraits=2</code>. This is only for
		 * convenience and readability of the code.
		 */
		protected final int nTraits;

		/**
		 * Minimum data value. This value is mapped onto color <code>gradient[0]</code>.
		 */
		protected double[] min;

		/**
		 * Helper variable to make the mapping from a range of data values to indices of
		 * the gradient array more efficient: <code>map=nBins/(max-min)</code>, where
		 * <code>max</code> maximum data value to be mapped.
		 */
		protected double[] map;

		/**
		 * The index of the color for the first trait.
		 */
		int trait1 = 0;

		/**
		 * The index of the color for the second trait.
		 */
		int trait2 = 1;

		/**
		 * Construct a 2D color gradient. With a dependent trait ({@code dep >= 0}) the
		 * gradient is created on a simplex. While without a dependent trait
		 * ({@code dep < 0}) the gradient
		 * is created with black in the corner where both traits are zero. The number of
		 * shades in the gradient are determined by the dimensions of {@code gradient}.
		 * <p>
		 * <strong>Important:</strong> The {@code gradient} array is assumed to be
		 * square.
		 * 
		 * @param colors   the array of trait colors
		 * @param dep      the index of the dependent trait
		 * @param gradient the array to store the gradient colors
		 */
		protected Gradient2D(Color[] colors, int dep, T[][] gradient) {
			this(dep < 0 ? 2 : 3, gradient.length - 1);
			switch (dep) {
				case 0:
					trait1 = 1;
					trait2 = 2;
					break;
				case 1:
					trait1 = 0;
					trait2 = 2;
					break;
				default:
					trait1 = 0;
					trait2 = 1;
					break;
			}
			this.gradient = gradient;
			setRange(0.0, 1.0);
			double w1 = 0.0;
			double wincr = 1.0 / nGradient;
			Color color1 = colors[trait1];
			Color color2 = colors[trait2];
			// color scheme depends on the presence of a dependent trait
			if (dep < 0) {
				// no dependent trait
				for (int i = 0; i <= nGradient; i++) {
					Color start = blendColors(color1, Color.BLACK, w1);
					Color end = addColors(start, color2);
					interpolateColors(gradient[i], start, 0, end, nGradient);
					w1 += wincr;
				}
				return;
			}
			// create color gradient on simplex
			Color colordep = colors[dep];
			for (int i = 0; i <= nGradient; i++) {
				Color start = blendColors(color1, colordep, w1);
				Color end = blendColors(start, color2, w1);
				interpolateColors(gradient[i], start, 0, end, nGradient - i);
				w1 += wincr;
			}
		}

		/**
		 * For internal use only. Initialize color gradient for {@code nTraits} and
		 * {@code nGradient} increments. The default range for mapping data values onto
		 * the color gradient is <code>[0.0, 1.0]<sup>nTraits</sup></code>.
		 * 
		 * @param nTraits   the number of traits
		 * @param nGradient the number of color interpolations
		 * 
		 * @see #setRange(double, double)
		 */
		protected Gradient2D(int nTraits, int nGradient) {
			this.nTraits = nTraits;
			this.nGradient = nGradient;
			min = new double[nTraits];
			map = new double[nTraits];
			setRange(0.0, 1.0);
		}

		/**
		 * Assign a new <code>color</code> to the gradient for <code>value</code>.
		 * 
		 * @param value the value of the <code>color</code>
		 * @param color the new color for <code>value</code>
		 */
		public void setColor(double[] value, Color color) {
			setColor(value, color, 255);
		}

		/**
		 * Assign a new <code>color</code> to the gradient for <code>value</code> with
		 * transparency <code>alpha</code>.
		 * <p>
		 * <strong>Note:</strong> the transparency of the supplied color is always
		 * honoured but overruled if {@code alpha != 255}.
		 * 
		 * @param value the value of the <code>color</code>
		 * @param color the new color for <code>value</code>
		 * @param alpha the transparency of the new color
		 */
		public void setColor(double[] value, Color color, int alpha) {
			int bin0 = binOf(value, trait1);
			if (bin0 < 0)
				return;
			int bin1 = binOf(value, trait2);
			if (bin1 < 0)
				return;
			if (alpha == 255 && color.getAlpha() < 255)
				gradient[bin0][bin1] = color2Color(color);
			else
				gradient[bin0][bin1] = color2Color(addAlpha(color, alpha));
		}

		/**
		 * Utility method to calculate the index of the gradient color that corresponds
		 * to {@code value} for trait with index {@code trait}. Returns {@code -1} if
		 * {@code value} is invalid or lies outside the range of the gradient.
		 * 
		 * @param value the array of values to convert to a bin index of the gradient
		 * @param trait the index of the trait
		 * @return the index of the bin or {@code -1}
		 */
		protected int binOf(double[] value, int trait) {
			return binOf(value[trait], trait);
		}

		/**
		 * Utility method to calculate the index of the gradient color that corresponds
		 * to {@code value} for trait with index {@code trait}. Returns {@code -1} if
		 * {@code value} is invalid or lies outside the range of the gradient.
		 * 
		 * @param value the value to convert to a bin index of the gradient
		 * @param trait the index of the corresponding trait
		 * @return the index of the bin or {@code -1}
		 */
		protected int binOf(double value, int trait) {
			if (!Double.isFinite(value)) // protect against NaN
				return -1;
			int bin = (int) Math.rint((value - min[trait]) * map[trait]);
			if (bin > nGradient || bin < 0)
				return -1;
			return bin;
		}

		/**
		 * Sets the range of values spanned by the gradient to {@code [min,max]} in both
		 * dimensions.
		 * 
		 * @param min the minimum value of the gradient
		 * @param max the maximum value of the gradient
		 */
		public void setRange(double min, double max) {
			Arrays.fill(this.min, min);
			Arrays.fill(this.map, max <= min ? 0.0 : nGradient / (max - min));
		}

		/**
		 * Sets the range of values spanned by the gradient to {@code [min,max]} in both
		 * dimensions.
		 * 
		 * @param min the minimum value of the gradient
		 * @param max the maximum value of the gradient
		 */
		public void setRange(double[] min, double[] max) {
			setRange(min, max, -1);
		}

		/**
		 * Set the range of data values for <em>three</em> dimensional data with
		 * dependent trait. The index of the dependent trait is <code>idx</code>. The
		 * data range for the first trait is <code>[min[0], max[0]]</code> and for the
		 * second trait <code>[min[1], max[1]</code>, assuming that <code>idx=2</code>
		 * for the above illustration.
		 * 
		 * @param min the array with the minimum values
		 * @param max the array with the maximum values
		 * @param idx the index of the dependent trait
		 */
		public void setRange(double[] min, double[] max, int idx) {
			int trait = 0;
			for (int n = 0; n < nTraits; n++) {
				if (n == idx)
					continue;
				double minn = min[n];
				double maxn = max[n];
				this.min[trait] = minn;
				double mapt = maxn <= minn ? 0.0 : nGradient / (maxn - minn);
				map[trait] = mapt;
				trait++;
			}
		}

		/**
		 * Sets the range of values spanned by the gradient to {@code [min,max]} for the
		 * trait with index {@code index}.
		 * 
		 * @param min the minimum value of the gradient
		 * @param max the maximum value of the gradient
		 * @param idx the index of the trait
		 */
		public void setRange(double min, double max, int idx) {
			this.min[idx] = min;
			map[idx] = max <= min ? 0.0 : nGradient / (max - min);
		}

		@Override
		public T translate(double[] data) {
			return gradient[binOf(data, trait1)][binOf(data, trait2)];
		}

		/**
		 * {@inheritDoc}
		 * <p>
		 * <strong>Implementation:</strong>
		 * <ol>
		 * <li>Two subsequent entries, <code>data[n]</code> and
		 * <code>data[n+1]</code> are converted to the corresponding gradient colors.
		 * This applies, for example, to data from continuous games with multiple
		 * traits. The gradient colors are returned in the <code>color</code> array.
		 * <li>For performance reasons no validity checks on
		 * <code>data</code>. In particular, all data entries must lie inside the range
		 * for mapping data values.
		 * </ol>
		 * 
		 * @see IBSMCPopulation
		 */
		@Override
		public boolean translate(double[] data, T[] color) {
			int len = color.length;
			int idx = 0;
			for (int n = 0; n < len; n++) {
				color[n] = gradient[binOf(data[idx], trait1)][binOf(data[idx + 1], trait2)];
				idx += nTraits;
			}
			return true;
		}

		/**
		 * {@inheritDoc}
		 * <p>
		 * <strong>Implementation:</strong>
		 * <ol>
		 * <li>Each entry in <code>data</code> represents a multi-dimensional trait
		 * value, which is converted to the corresponding gradient color and returned in
		 * the <code>color</code> array. An example are trait densities in PDE models.
		 * <li>For performance reasons no validity checks on <code>data</code>. In
		 * particular, all data entries must lie inside the range for mapping data
		 * values.
		 * </ol>
		 * 
		 * @see PDE
		 */
		@Override
		public boolean translate(double[][] data, T[] color) {
			int len = color.length;
			if (nTraits == 2) {
				// no dependent trait - use auto scaling
				for (int n = 0; n < len; n++) {
					double[] datan = data[n];
					color[n] = gradient[binOf(datan, trait1)][binOf(datan, trait2)];
				}
			} else {
				for (int n = 0; n < len; n++) {
					double[] datan = data[n];
					color[n] = gradient[(int) (datan[trait1] * nGradient)][(int) (datan[trait2] * nGradient)];
				}
			}
			return true;
		}
	}

	/**
	 * Color gradient for <code>N</code> dimensional data with one color for each
	 * dimension.
	 * 
	 * @param <T> type of color object: String or
	 *            {@link thothbot.parallax.core.shared.materials.MeshLambertMaterial
	 *            MeshLambertMaterial} for GWT and
	 *            {@link Color} for JRE
	 * 
	 * @see org.evoludo.simulator.ColorMapCSS ColorMapCSS
	 * @see org.evoludo.simulator.ColorMap3D ColorMap3D
	 * @see "Also consult GWT emulation of Color in org.evoludo.emulate.java.awt.Color"
	 */
	public abstract static class GradientND<T> extends ColorMap.Gradient2D<T> {

		/**
		 * Reference to trait colors that provide the basis to generate gradient colors.
		 */
		protected Color[] traits;

		/**
		 * The background color. The default is black.
		 */
		Color bg = Color.BLACK;

		/**
		 * Temporary variable to store the weights of each color component.
		 */
		protected double[] weights;

		/**
		 * Construct <code>N</code> dimensional color gradient to represent
		 * <code>N</code> dimensional data values. Each dimension <code>i</code> spans
		 * colors ranging from black to <code>colors[i]</code>.
		 * <p>
		 * The default range for mapping data values onto the color gradient is
		 * <code>[0.0, 1.0]</code> in all dimensions.
		 * 
		 * @param colors the colors for each dimension
		 * 
		 * @see #setRange(double, double)
		 * @see #setRange(double[], double[])
		 */
		protected GradientND(Color[] colors) {
			this(colors.length);
			traits = Arrays.copyOf(colors, nTraits);
		}

		/**
		 * Construct <code>N</code> dimensional color gradient to represent
		 * <code>N</code> dimensional data with a background color other than black, or,
		 * to represent <code>N+1</code> dimensional data values where one data value is
		 * dependent on the other <code>N</code>.
		 * <p>
		 * The color gradient in each dimension <code>i</code> ranges from
		 * <code>bg</code> to <code>colors[i]</code>.
		 * <p>
		 * The default range for mapping data values onto the color gradient is
		 * <code>[0.0, 1.0]</code> in all dimensions.
		 *
		 * @param colors the colors for the <code>N</code> dimensions
		 * @param bg     the color representing the background (or an <code>N+1</code>,
		 *               dependent trait)
		 * 
		 * @see #setRange(double, double)
		 * @see #setRange(double[], double[])
		 */
		protected GradientND(Color[] colors, Color bg) {
			this(colors);
			if (bg != null)
				this.bg = bg;
		}

		/**
		 * Construct <code>N</code> dimensional color gradient to represent
		 * <code>N+1</code> dimensional data values where one data value is dependent on
		 * the other two. This applies, for example, to data based on the replicator
		 * equation and dynamics that unfold on the simplex <code>S<sub>N</sub></code>.
		 * <p>
		 * The index of the dependent trait is <code>idx</code>. It is at its maximum if
		 * all other traits are at their minimum. The color gradient in each dimension
		 * <code>i</code> ranges from <code>colors[idx]</code> to <code>colors[i]</code>
		 * for <code>idx&ne;i</code>.
		 * <p>
		 * The default range for mapping data values onto the color gradient is
		 * <code>[0.0, 1.0]</code> in all dimensions.
		 *
		 * @param colors the colors for the <code>N</code> dimensions
		 * @param idx    the index of the dependent trait
		 * 
		 * @see #setRange(double, double)
		 * @see #setRange(double[], double[])
		 */
		protected GradientND(Color[] colors, int idx) {
			this(idx < 0 || idx >= colors.length ? colors.length : colors.length - 1);
			traits = new Color[nTraits];
			int trait = 0;
			for (int n = 0; n < colors.length; n++) {
				if (n == idx)
					continue;
				traits[trait++] = colors[n];
			}
			if (idx >= 0 && idx < colors.length)
				bg = colors[idx];
		}

		/**
		 * For internal use only. Allocates memory and sets the default range for
		 * mapping data values onto the color gradient to <code>[0.0, 1.0]</code> in
		 * every dimension.
		 * 
		 * @param dim the dimension of the gradient colors
		 * 
		 * @see #setRange(double, double)
		 */
		protected GradientND(int dim) {
			super(dim, 0);
			weights = new double[nTraits];
			// map is zero because nGradient is zero
			Arrays.fill(map, 1.0 / nTraits);
		}

		/**
		 * {@inheritDoc}
		 * <p>
		 * <strong>Implementation:</strong>
		 * <ol>
		 * <li><code>N</code> subsequent entries, <code>data[n]</code> through
		 * <code>data[n+N]</code>, are converted to gradient colors on the fly. This
		 * applies, for example, to data from continuous games with <code>N</code>
		 * traits. The gradient colors are returned in the <code>color</code> array.
		 * <li>For performance reasons no validity checks on <code>data</code>. In
		 * particular, all data entries must lie inside the range for mapping data
		 * values.
		 * </ol>
		 * 
		 * @see IBSMCPopulation
		 */
		@Override
		public boolean translate(double[] data, T[] color) {
			double[] datan = new double[nTraits];
			int len = color.length;
			int idx = 0;
			for (int n = 0; n < len; n++) {
				System.arraycopy(data, idx, datan, 0, nTraits);
				color[n] = super.translate(datan);
				idx += nTraits;
			}
			return true;
		}

		/**
		 * {@inheritDoc}
		 * <p>
		 * <strong>Implementation:</strong>
		 * <ol>
		 * <li>Each entry in <code>data</code> represents a <code>N</code>-dimensional
		 * trait value, which is converted to gradient colors on the fly and returned in
		 * the <code>color</code> array. An example are trait densities in PDE models.
		 * <li>For performance reasons no validity checks on <code>data</code>. In
		 * particular, all data entries must lie inside the range for mapping data
		 * values.
		 * </ol>
		 * 
		 * @see PDE
		 */
		@Override
		public boolean translate(double[][] data, T[] color) {
			int len = color.length;
			for (int n = 0; n < len; n++)
				color[n] = super.translate(data[n]);
			return true;
		}

		/**
		 * Convert the <code>N</code>-dimensional color map into a 1D gradient with a
		 * total of {@code nIncr} shades. This is useful for coloring schemes that are
		 * based on an aggregate characteristics, such as the distance, instead of the
		 * {@code N}-dimensional data.
		 * 
		 * @param nIncr the total number of color increments
		 * @return the 1D color gradient
		 */
		public abstract ColorMap.Gradient1D<T> toGradient1D(int nIncr);
	}
}
