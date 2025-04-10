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

import org.evoludo.util.Formatter;

/**
 * Coloring is a wrapper class for different schemes to map data onto colors for
 * 2D visualizations using the HTML5 <code>canvas</code> element. Each inner
 * class implements the {@link ColorMap} interface to provide an implementation
 * agnostic manner to translate data into colors.
 * <p>
 * The following color mappings are currently available:
 * <dl>
 * <dt>Index</dt>
 * <dd>Associates integer indices with colors.</dd>
 * <dt>Gradient1D</dt>
 * <dd>One dimensional color gradient spanning two or more colors.</dd>
 * <dt>Hue</dt>
 * <dd>Special case of <code>Gradient1D</code> where the gradient follows the
 * color hue.</dd>
 * <dt>Gradient2D</dt>
 * <dd>Two dimensional gradient with each dimension represented by one
 * color.</dd>
 * <dt>GradientND</dt>
 * <dd>Multi-dimensional gradient spanned by an identical number of colors</dd>
 * </dl>
 * 
 * @author Christoph Hauert
 */
public abstract class ColorMapCSS extends ColorMap<String> {

	/**
	 * Constructs a new color map.
	 */
	public ColorMapCSS() {
	}

	/**
	 * Convert Color to CSS style string. For colors with transparency, the output
	 * is of the form <code>rgba(<span style="color:red;">r</span>, 
	 * <span style="color:green;">g</span>, 
	 * <span style="color:blue;">b</span>, a)</code>, where
	 * <code><span style="color:red;">r</span>, 
	 * <span style="color:green;">g</span>, 
	 * <span style="color:blue;">b</span></code>, and <code>a</code> refer to the
	 * red, green, blue and alpha (transparency) components. Without transparency
	 * the output is of the form
	 * <code>#<span style="color:red;">rr</span><span style=
	 * "color:green;">gg</span><span style="color:blue;">bb</span></code>, where
	 * <code><span style="color:red;">rr</span>, <span style=
	 * "color:green;">gg</span></code>, and
	 * <code><span style="color:blue;">bb</span></code> are the hexadecimal
	 * representation of the red, green and blue components, respectively.
	 * 
	 * @param color the color to convert
	 * @return CSS color string
	 * 
	 * @see #RGBA2Css(int, int, int, int)
	 * @see #RGB2Css(int, int, int)
	 */
	public static String Color2Css(Color color) {
		int alpha = color.getAlpha();
		if (alpha < 255)
			return RGBA2Css(color.getRed(), color.getGreen(), color.getBlue(), alpha);
		return RGB2Css(color.getRed(), color.getGreen(), color.getBlue());
	}

	/**
	 * Convert an array of Color's to an array of CSS style strings.
	 * 
	 * @param colors the array of Color's to convert
	 * @return array of CSS color strings
	 * 
	 * @see #Color2Css(Color)
	 */
	public static String[] Color2Css(Color[] colors) {
		int nColors = colors.length;
		String[] css = new String[nColors];
		for (int n = 0; n < nColors; n++)
			css[n] = Color2Css(colors[n]);
		return css;
	}

	/**
	 * Convert the four components of a 32bit color,
	 * <code><span style="color:red;">r</span>, <span style=
	 * "color:green;">g</span>, <span style="color:blue;">b</span></code>, and
	 * <code>a</code> to a CSS color style.
	 * 
	 * @param r the red component <code>0-255</code>
	 * @param g the green component <code>0-255</code>
	 * @param b the blue component <code>0-255</code>
	 * @param a the alpha (transparency) component <code>[0,1]</code>
	 * @return CSS color string <code>rgba(<span style="color:red;">r</span>, 
	 * 		   <span style="color:green;">g</span>, 
	 * 		   <span style="color:blue;">b</span>, a)</code>
	 */
	public static String RGBA2Css(int r, int g, int b, int a) {
		if (a == 255)
			return RGB2Css(r, g, b);
		return "rgba(" + r + "," + g + "," + b + "," + Formatter.format(a * INV255, 3) + ")";
	}

	/**
	 * Convert the three components of a 24bit color,
	 * <code><span style="color:red;">r</span>, <span style=
	 * "color:green;">g</span></code>, and
	 * <code><span style="color:blue;">b</span></code> to a CSS color style.
	 * 
	 * @param r the red component <code>0-255</code>
	 * @param g the green component <code>0-255</code>
	 * @param b the blue component <code>0-255</code>
	 * @return CSS color string <code>#<span style="color:red;">rr</span><span 
	 * 		   style="color:green;">gg</span><span 
	 * 		   style="color:blue;">bb</span></code>
	 */
	public static String RGB2Css(int r, int g, int b) {
		return "#" + pad(Integer.toHexString(r)) + pad(Integer.toHexString(g)) + pad(Integer.toHexString(b));
	}

	/**
	 * Utility function to add padding (if needed) of leading zero'es to ensure that
	 * each color component is encoded by a two digit hexadecimal.
	 * 
	 * @param in the input string
	 * @return leading zero'es added as needed to ensure two digit string
	 */
	private static String pad(String in) {
		int len = in.length();
		if (len == 2)
			return in;
		if (len == 1)
			return "0" + in;
		return "00";
	}

	/**
	 * Associates integer indices with colors.
	 */
	public static class Index extends ColorMap.Index<String> {

		/**
		 * Construct a new Index color map.
		 * 
		 * @param colors the array of colors to assign to indices
		 */
		public Index(Color[] colors) {
			this(colors, 255);
		}

		/**
		 * Construct a new Index color map with transparency where \(\alpha\in[0,255]\)
		 * with \(0\) fully transparent and \(255\) fully opaque.
		 * <p>
		 * <strong>Note:</strong> this is a convenient way to generate maps with a
		 * different transparency. The transparency of the supplied colors is always
		 * honoured but overruled if {@code alpha != 255}.
		 * 
		 * @param colors the array of colors to assign to indices
		 * @param alpha  the alpha value for opaque colors
		 */
		public Index(Color[] colors, int alpha) {
			super(colors, alpha, new String[colors.length]);
		}

		@Override
		public String color2Color(Color color) {
			return Color2Css(color);
		}

		@Override
		public Gradient1D<String> toGradient1D(int nIncr) {
			return new ColorMapCSS.Gradient1D(srccolors, nIncr);
		}
	}

	/**
	 * Color gradient following the hue.
	 * <p>
	 * The actual data-to-color translation is performed in the super class.
	 * 
	 * @see ColorMap.Hue
	 */
	public static class Hue extends ColorMap.Hue<String> {

		/**
		 * Construct a new Hue color map, starting at a hue of <code>0.0</code> (red) up
		 * to hue <code>1.0</code> (red, again) and interpolate with
		 * <code>steps-1</code> intermediate colors. The resulting gradient spans
		 * <code>steps+1</code> colors. The default range for mapping data values onto
		 * the color gradient is <code>[0.0, 1.0]</code>.
		 * 
		 * @param nIncr the number of intermediate colors
		 * 
		 * @see #setRange(double, double)
		 * @see #setHueRange(double, double)
		 */
		public Hue(int nIncr) {
			this(0.0, 1.0, nIncr);
		}

		/**
		 * Construct a new Hue color map, starting at hue <code>start</code> up to hue
		 * <code>end</code> (both in <code>[0.0, 1.0]</code>) and interpolate with
		 * <code>steps-1</code> intermediate colors. The resulting gradient spans
		 * <code>steps+1</code> colors. The default range for mapping data values onto
		 * the color gradient is <code>[0.0, 1.0]</code>.
		 * <p>
		 * <strong>Notes:</strong>
		 * <ol>
		 * <li>If <code>end&le;start</code> the colors warp around through red.
		 * <li>If <code>steps&lt;0</code> the hue is interpolated in reverse.
		 * <li>The saturation and brightness of the gradient are both maximal.
		 * </ol>
		 * 
		 * @param start the starting hue
		 * @param end   the ending hue
		 * @param nIncr the number of intermediate colors
		 * 
		 * @see #setRange(double, double)
		 */
		public Hue(double start, double end, int nIncr) {
			super(start, end, nIncr > 0, new String[Math.abs(nIncr)]);
		}

		@Override
		public String color2Color(Color color) {
			return Color2Css(color);
		}
	}

	/**
	 * One dimensional color gradient spanning two or more colors
	 */
	public static class Gradient1D extends ColorMap.Gradient1D<String> {

		/**
		 * Construct color gradient ranging from color <code>start</code> to color
		 * <code>end</code> and interpolate with <code>steps-1</code> intermediate
		 * colors. The resulting gradient spans <code>steps+1</code> colors. The default
		 * range for mapping data values onto the color gradient is
		 * <code>[0.0, 1.0]</code>.
		 * 
		 * @param start the starting color
		 * @param end   the ending color
		 * @param nIncr the number of intermediate colors
		 * 
		 * @see #setRange(double, double)
		 */
		public Gradient1D(Color start, Color end, int nIncr) {
			this(new Color[] { start, end }, 0, nIncr);
		}

		/**
		 * Construct color gradient ranging from color <code>start</code> to color
		 * <code>end</code> and interpolate with <code>steps-1</code> intermediate
		 * colors. The resulting gradient spans <code>steps+1</code> colors. The default
		 * range for mapping data values onto the color gradient is
		 * <code>[0.0, 1.0]</code>.
		 * 
		 * @param start the starting color
		 * @param end   the ending color
		 * @param trait the index of the trait
		 * @param nIncr the number of intermediate colors
		 * 
		 * @see #setRange(double, double)
		 */
		public Gradient1D(Color start, Color end, int trait, int nIncr) {
			this(new Color[] { start, end }, trait, nIncr);
		}

		/**
		 * Construct color gradient running through all the colors in the array
		 * <code>colors</code>. The number of interpolated intermediate colors between
		 * two subsequent entries in <code>colors</code> is
		 * <code>(steps-1)/(N-1)</code>, where <code>N</code> is the number of colors in
		 * <code>colors</code>. The resulting gradient spans <code>steps+1</code>
		 * colors. The default range for mapping data values onto the color gradient is
		 * <code>[0.0, 1.0]</code>.
		 * 
		 * @param colors the equally spaced reference colors of the gradient
		 * @param nIncr  the number of intermediate, gradient colors
		 * 
		 * @see #setRange(double, double)
		 */
		public Gradient1D(Color[] colors, int nIncr) {
			this(colors, 0, nIncr);
		}

		/**
		 * Construct color gradient running through all the colors in the array
		 * <code>colors</code>. The number of interpolated intermediate colors between
		 * two subsequent entries in <code>colors</code> is
		 * <code>(steps-1)/(N-1)</code>, where <code>N</code> is the number of colors in
		 * <code>colors</code>. The resulting gradient spans <code>steps+1</code>
		 * colors. The default range for mapping data values onto the color gradient is
		 * <code>[0.0, 1.0]</code>.
		 * 
		 * @param colors the equally spaced reference colors of the gradient
		 * @param trait  the index of the trait
		 * @param nIncr  the number of intermediate, gradient colors
		 * 
		 * @see #setRange(double, double)
		 */
		public Gradient1D(Color[] colors, int trait, int nIncr) {
			super(colors, trait, new String[nIncr]);
		}

		@Override
		public String color2Color(Color color) {
			return Color2Css(color);
		}
	}

	/**
	 * Two dimensional color gradient with one color for each dimension.
	 */
	public static class Gradient2D extends ColorMap.Gradient2D<String> {

		/**
		 * Construct two dimensional color gradient to represent two dimensional data
		 * values. The first dimension spans colors ranging from black to
		 * <code>colors[0]</code> and the second dimension from black to
		 * <code>colors[1]</code>. Each dimension is interpolated with
		 * <code>steps-1</code> intermediate colors. The resulting gradient spans
		 * <code>(steps+1)<sup>2</sup></code> colors.
		 * <p>
		 * The default range for mapping data values onto the color gradient is
		 * <code>[0.0, 1.0]</code> in both dimensions.
		 * 
		 * @param colors the two colors for each dimension
		 * @param nIncr  the number of intermediate colors per dimension
		 * 
		 * @see #setRange(double, double)
		 * @see #setRange(double[], double[])
		 */
		public Gradient2D(Color[] colors, int nIncr) {
			this(colors[0], colors[1], nIncr);
		}

		/**
		 * Construct two dimensional color gradient to represent <em>three</em>
		 * dimensional data values where one data value is dependent on the other two.
		 * This applies, for example, to data based on the replicator equation and
		 * dynamics that unfold on the simplex <code>S<sub>N</sub></code>.
		 * <p>
		 * The index of the dependent trait is <code>idx</code>. It is at its maximum if
		 * both other traits are at their minimum. Thus, the color gradient in the first
		 * dimension ranges from <code>colors[idx]</code> to <code>colors[0]</code> and
		 * from <code>colors[idx]</code> to <code>colors[1]</code> in the second
		 * dimension, assuming that <code>idx=2</code> for the above illustration.
		 * <p>
		 * Each dimension is interpolated with <code>steps-1</code> intermediate colors.
		 * The resulting gradient spans <code>(steps+1)<sup>2</sup></code> colors.
		 * <p>
		 * The default range for mapping data values onto the color gradient is
		 * <code>[0.0, 1.0]</code> in all dimensions.
		 *
		 * @param colors the colors for the three dimensions
		 * @param idx    the index of the dependent trait
		 * @param nIncr  the number of intermediate colors per dimension
		 * 
		 * @see #setRange(double, double)
		 * @see #setRange(double[], double[])
		 */
		public Gradient2D(Color[] colors, int idx, int nIncr) {
			this(colors[idx == 0 ? 1 : 0], colors[idx == 0 || idx == 1 ? 2 : 1],
					idx >= 0 && idx < colors.length ? colors[idx] : Color.BLACK, nIncr);
		}

		/**
		 * Construct two dimensional color gradient to represent two dimensional data
		 * values. The first dimension spans colors ranging from black to
		 * <code>colors[0]</code> and the second dimension from black to
		 * <code>colors[1]</code>. Each dimension is interpolated with
		 * <code>steps-1</code> intermediate colors. The resulting gradient spans
		 * <code>(steps+1)<sup>2</sup></code> colors.
		 * <p>
		 * The default range for mapping data values onto the color gradient is
		 * <code>[0.0, 1.0]</code> in both dimensions.
		 * 
		 * @param trait1 the color representing the first trait
		 * @param trait2 the color representing the second trait
		 * @param nIncr  the number of intermediate colors per dimension
		 * 
		 * @see #setRange(double, double)
		 * @see #setRange(double[], double[])
		 */
		public Gradient2D(Color trait1, Color trait2, int nIncr) {
			this(trait1, trait2, Color.BLACK, nIncr);
		}

		/**
		 * Construct two dimensional color gradient to represent <em>two</em>
		 * dimensional data with a background color other than black, or, to represent
		 * <em>three</em> dimensional data values where one data value is dependent on
		 * the other two.
		 * <p>
		 * The color gradient in the first dimension ranges from <code>bg</code> to
		 * <code>trait1</code> and from <code>bg</code> to <code>trait2</code> in the
		 * second dimension.
		 * <p>
		 * Each dimension is interpolated with <code>steps-1</code> intermediate colors.
		 * The resulting gradient spans <code>(steps+1)<sup>2</sup></code> colors.
		 * <p>
		 * The default range for mapping data values onto the color gradient is
		 * <code>[0.0, 1.0]</code> in both (all) dimensions.
		 *
		 * @param trait1 the color representing the first trait
		 * @param trait2 the color representing the second trait
		 * @param bg     the color representing the background (or a third, dependent
		 *               trait)
		 * @param nIncr  the number of intermediate colors per dimension
		 * 
		 * @see #Gradient2D(Color[], int)
		 * @see #Gradient2D(Color, Color, int)
		 * @see #Gradient2D(Color[], int, int)
		 * @see #setRange(double, double)
		 * @see #setRange(double[], double[])
		 */
		public Gradient2D(Color trait1, Color trait2, Color bg, int nIncr) {
			super(trait1, trait2, bg, new String[nIncr][nIncr]);
		}

		@Override
		public String color2Color(Color color) {
			return Color2Css(color);
		}
	}

	/**
	 * <code>N</code> dimensional color gradient with one color for each dimension.
	 */
	public static class GradientND extends ColorMap.GradientND<String> {

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
		public GradientND(Color[] colors) {
			super(colors);
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
		public GradientND(Color[] colors, Color bg) {
			super(colors, bg);
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
		public GradientND(Color[] colors, int idx) {
			super(colors, idx);
		}

		/**
		 * {@inheritDoc}
		 * <p>
		 * <strong>GradientND:</strong> color gradients are generated on the fly.
		 * 
		 * @return CSS style color string of the corresponding gradient color
		 */
		@Override
		public String translate(double[] data) {
			for (int n = 0; n < nTraits; n++)
				weights[n] = (data[n] - min[n]) * map[n];
			return color2Color(blendColors(traits, weights));
		}

		@Override
		public String color2Color(Color color) {
			return Color2Css(color);
		}

		@Override
		public Gradient1D<String> toGradient1D(int nIncr) {
			return new ColorMapCSS.Gradient1D(traits, nIncr);
		}
	}
}
