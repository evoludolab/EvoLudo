//
// EvoLudo Project
//
// Copyright 2010-2026 Christoph Hauert
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

import java.util.Arrays;

import thothbot.parallax.core.shared.materials.MeshLambertMaterial;
import thothbot.parallax.core.shared.math.Color;

/**
 * Coloring3D is a wrapper class for different schemes to map data onto colors
 * for 3D visualization using WebGL. Each inner class implements the
 * {@link ColorMap} interface to provide an implementation agnostic manner to
 * translate data into colors.
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
 * <p>
 * <strong>Important:</strong> the Parallax library provides access to WebGL
 * technology directly from GWT. The following code is compliant with Parallax
 * version 1.6. Unfortunately, colors are handled differently in the two
 * frameworks. In particular, java.awt.Color and
 * thothbot.parallax.core.shared.math.Color are incompatible. In the following
 * any reference to Color refers to the WebGL version, while references to
 * regular java Color's use the fully qualified name, java.awt.Color.
 * 
 * @author Christoph Hauert
 * 
 * @see <a href="https://github.com/thothbot/parallax">Github: Parallax, Java 3D
 *      SDK</a>
 */
public abstract class ColorMap3D extends ColorMap<MeshLambertMaterial> {

	/**
	 * Protected constructor to restrict instantiation to subclasses.
	 */
	protected ColorMap3D() {
	}

	/**
	 * The color black.
	 */
	public static final Color BLACK = Color2Color(java.awt.Color.BLACK);

	/**
	 * The color of undirected links.
	 */
	public static final Color UNDIRECTED = Color2Color(java.awt.Color.BLACK);

	/**
	 * The baseline color of directed links.
	 */
	public static final Color DIRECTED = Color2Color(java.awt.Color.GRAY);

	/**
	 * The color of directed links at the origin.
	 */
	public static final Color DIRECTED_SRC = Color2Color(java.awt.Color.GREEN);

	/**
	 * The color of directed links at the destination.
	 */
	public static final Color DIRECTED_DST = Color2Color(java.awt.Color.RED);

	/**
	 * Convert a java.awt.Color object to a Color object.
	 * 
	 * @param color the java.awt.Color
	 * @return the corresponding thothbot.parallax.core.shared.math.Color
	 */
	public static Color Color2Color(java.awt.Color color) {
		return new Color(color.getRGB());
	}

	/**
	 * Convert <span style="color:red;">red</span>,
	 * <span style="color:green;">green</span>,
	 * <span style="color:blue;">blue</span> to a WebGL Color object.
	 *
	 * @param r the <span style="color:red;">red</span> component of the color in
	 *          <code>[0.0, 1.0]</code>
	 * @param g the <span style="color:green;">green</span> component of the color
	 *          in <code>[0.0, 1.0]</code>
	 * @param b the <span style="color:blue;">blue</span> component of the color in
	 *          <code>[0.0, 1.0]</code>
	 * @return the corresponding thothbot.parallax.core.shared.math.Color object
	 */
	public static Color Color2Color(double r, double g, double b) {
		Color color = new Color();
		color.setRGB(r, g, b);
		return color;
	}

	/**
	 * Helper constant: the factor to derive darker shades of a color.
	 * 
	 * @see java.awt.Color
	 */
	private static final double FACTOR = 0.7;

	/**
	 * Convert <span style="color:red;">red</span>,
	 * <span style="color:green;">green</span>,
	 * <span style="color:blue;">blue</span> and transparency values (RGBA) to a
	 * WebGL Material object.
	 *
	 * @param r the <span style="color:red;">red</span> component of the color in
	 *          <code>[0.0, 1.0]</code>
	 * @param g the <span style="color:green;">green</span> component of the color
	 *          in <code>[0.0, 1.0]</code>
	 * @param b the <span style="color:blue;">blue</span> component of the color in
	 *          <code>[0.0, 1.0]</code>
	 * @param a the transparency (alpha) component of the color in
	 *          <code>[0.0, 1.0]</code>
	 * @return the Material object for non-shiny surfaces representing the RGB value
	 *         with transparency (if applicable)
	 * 
	 * @see java.awt.Color#darker()
	 */
	public static MeshLambertMaterial Color2Material(double r, double g, double b, double a) {
		MeshLambertMaterial material = new MeshLambertMaterial();
		material.setColor(Color2Color(r, g, b));
		// manually derive darker shade of color
		material.setAmbient(Color2Color(r * FACTOR, g * FACTOR, b * FACTOR));
		if (a < 1.0) {
			material.setTransparent(true);
			material.setOpacity(a);
		}
		return material;
	}

	/**
	 * Convert a java.awt.Color to a WebGL Material object. In order to enhance 3D
	 * effects the ambient color is set to <code>color.darker()</code>.
	 *
	 * @param color the java.awt.Color
	 * @return the Material object for non-shiny surfaces with <code>color</code>
	 *         and transparency (if applicable)
	 */
	public static MeshLambertMaterial Color2Material(java.awt.Color color) {
		final double scale = 0.00390625; // 1/256
		return Color2Material(color.getRed() * scale, color.getGreen() * scale, color.getBlue() * scale,
				color.getAlpha() * scale);
	}

	/**
	 * Associates integer indices with colors.
	 */
	public static class Index extends ColorMap.Index<MeshLambertMaterial> {

		/**
		 * Construct a new Index color map.
		 * 
		 * @param colors the array of colors to assign to indices
		 */
		public Index(java.awt.Color[] colors) {
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
		public Index(java.awt.Color[] colors, int alpha) {
			super(colors, alpha, new MeshLambertMaterial[colors.length]);
		}

		@Override
		public MeshLambertMaterial color2Color(java.awt.Color color) {
			return Color2Material(color);
		}

		@Override
		public Gradient1D<MeshLambertMaterial> toGradient1D(int nIncr) {
			return new ColorMap3D.Gradient1D(srccolors, nIncr);
		}
	}

	/**
	 * Color gradient following the hue.
	 * <p>
	 * The actual data-to-color translation is performed in the super class.
	 * 
	 * @see ColorMap3D.Gradient1D
	 */
	public static class Hue extends ColorMap.Hue<MeshLambertMaterial> {

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
			super(start, end, nIncr > 0, new MeshLambertMaterial[Math.abs(nIncr)]);
		}

		@Override
		public MeshLambertMaterial color2Color(java.awt.Color color) {
			return Color2Material(color);
		}
	}

	/**
	 * One dimensional color gradient spanning two or more colors
	 */
	public static class Gradient1D extends ColorMap.Gradient1D<MeshLambertMaterial> {

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
		public Gradient1D(java.awt.Color start, java.awt.Color end, int nIncr) {
			this(new java.awt.Color[] { start, end }, nIncr);
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
		 * @param nIncr  the number of intermediate colors
		 * 
		 * @see #setRange(double, double)
		 */
		public Gradient1D(java.awt.Color[] colors, int nIncr) {
			super(colors, 0, new MeshLambertMaterial[nIncr]);
		}

		@Override
		public MeshLambertMaterial color2Color(java.awt.Color color) {
			return Color2Material(color);
		}
	}

	/**
	 * Two dimensional color gradient with one color for each dimension.
	 */
	public static class Gradient2D extends ColorMap.Gradient2D<MeshLambertMaterial> {

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
		public Gradient2D(java.awt.Color[] colors, int nIncr) {
			this(colors, -1, nIncr);
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
		 * @param dep    the index of the dependent trait
		 * @param nIncr  the number of intermediate colors per dimension
		 * 
		 * @see #setRange(double, double)
		 * @see #setRange(double[], double[])
		 */
		public Gradient2D(java.awt.Color[] colors, int dep, int nIncr) {
			super(colors, dep, new MeshLambertMaterial[nIncr][nIncr]);
		}

		@Override
		public void setColor(double[] value, java.awt.Color color) {
			gradient[(int) ((value[0] - min[0]) * map[0] + 0.5)][(int) ((value[1] - min[1]) * map[1]
					+ 0.5)] = Color2Material(color);
		}

		@Override
		public void setRange(double min, double max) {
			this.min = new double[nTraits];
			Arrays.fill(this.min, min);
			Arrays.fill(this.map, max <= min ? 0.0 : nGradient / (max - min));
		}

		@Override
		public void setRange(double[] min, double[] max) {
			setRange(min, max, -1);
		}

		@Override
		public MeshLambertMaterial color2Color(java.awt.Color color) {
			return Color2Material(color);
		}
	}

	/**
	 * <code>N</code> dimensional color gradient with one color for each dimension.
	 */
	public static class GradientND extends ColorMap.GradientND<MeshLambertMaterial> {

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
		public GradientND(java.awt.Color[] colors) {
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
		public GradientND(java.awt.Color[] colors, java.awt.Color bg) {
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
		public GradientND(java.awt.Color[] colors, int idx) {
			super(colors, idx);
		}

		/**
		 * {@inheritDoc}
		 * <p>
		 * <strong>GradientND:</strong> color gradients are generated on the fly.
		 * 
		 * @return WebGL Material representing the corresponding gradient color
		 */
		@Override
		public MeshLambertMaterial translate(double[] data) {
			for (int n = 0; n < nTraits; n++)
				weights[n] = (data[n] - min[n]) * map[n];
			return color2Color(blendColors(traits, weights));
		}

		@Override
		public MeshLambertMaterial color2Color(java.awt.Color color) {
			return Color2Material(color);
		}

		@Override
		public Gradient1D<MeshLambertMaterial> toGradient1D(int nIncr) {
			return new ColorMap3D.Gradient1D(traits, nIncr);
		}
	}
}
