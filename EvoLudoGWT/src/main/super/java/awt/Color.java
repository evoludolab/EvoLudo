/*
 * Copyright (c) 1995, 2010, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 * 
 * Christoph Hauert: Modifications for GWT for EvoLudo project
 * 160121: Adjustments for super-sourcing Color in GWT
 * 160125: System property stuff and implementation of Paint interface removed
 * 221105: GWT doesn't like float - converted to double
 * 231104: Limit argument range instead of throwing exceptions that throw GWT off
 */

package java.awt;

/**
 * The <code>Color</code> class is used to encapsulate colors in the default
 * sRGB color space or colors in arbitrary color spaces identified by a
 * {@link java.awt.color.ColorSpace}. Every color has an implicit alpha value of
 * 1.0 or an explicit one provided in the constructor. The alpha value
 * defines the transparency of a color and can be represented by
 * a double value in the range 0.0&nbsp;-&nbsp;1.0 or 0&nbsp;-&nbsp;255.
 * An alpha value of 1.0 or 255 means that the color is completely
 * opaque and an alpha value of 0 or 0.0 means that the color is
 * completely transparent. When constructing a <code>Color</code> with an
 * explicit alpha or getting the color/alpha components of a <code>Color</code>,
 * the color components are never premultiplied by the alpha component.
 * <p>
 * The default color space for the Java 2D(tm) API is sRGB, a proposed
 * standard RGB color space. For further information on sRGB,
 * see <A href="http://www.w3.org/pub/WWW/Graphics/Color/sRGB.html">
 * http://www.w3.org/pub/WWW/Graphics/Color/sRGB.html
 * </A>.
 * <p>
 * 
 * @version 10 Feb 1997
 * @author Sami Shaio
 * @author Arthur van Hoff
 * @see java.awt.color.ColorSpace
 * @see AlphaComposite
 */
public class Color {

	/**
	 * The color white. In the default sRGB space.
	 */
	public final static Color white = new Color(255, 255, 255);

	/**
	 * The color white. In the default sRGB space.
	 * 
	 * @since 1.4
	 */
	public final static Color WHITE = white;

	/**
	 * The color light gray. In the default sRGB space.
	 */
	public final static Color lightGray = new Color(192, 192, 192);

	/**
	 * The color light gray. In the default sRGB space.
	 * 
	 * @since 1.4
	 */
	public final static Color LIGHT_GRAY = lightGray;

	/**
	 * The color gray. In the default sRGB space.
	 */
	public final static Color gray = new Color(128, 128, 128);

	/**
	 * The color gray. In the default sRGB space.
	 * 
	 * @since 1.4
	 */
	public final static Color GRAY = gray;

	/**
	 * The color dark gray. In the default sRGB space.
	 */
	public final static Color darkGray = new Color(64, 64, 64);

	/**
	 * The color dark gray. In the default sRGB space.
	 * 
	 * @since 1.4
	 */
	public final static Color DARK_GRAY = darkGray;

	/**
	 * The color black. In the default sRGB space.
	 */
	public final static Color black = new Color(0, 0, 0);

	/**
	 * The color black. In the default sRGB space.
	 * 
	 * @since 1.4
	 */
	public final static Color BLACK = black;

	/**
	 * The color red. In the default sRGB space.
	 */
	public final static Color red = new Color(255, 0, 0);

	/**
	 * The color red. In the default sRGB space.
	 * 
	 * @since 1.4
	 */
	public final static Color RED = red;

	/**
	 * The color pink. In the default sRGB space.
	 */
	public final static Color pink = new Color(255, 175, 175);

	/**
	 * The color pink. In the default sRGB space.
	 * 
	 * @since 1.4
	 */
	public final static Color PINK = pink;

	/**
	 * The color orange. In the default sRGB space.
	 */
	public final static Color orange = new Color(255, 200, 0);

	/**
	 * The color orange. In the default sRGB space.
	 * 
	 * @since 1.4
	 */
	public final static Color ORANGE = orange;

	/**
	 * The color yellow. In the default sRGB space.
	 */
	public final static Color yellow = new Color(255, 255, 0);

	/**
	 * The color yellow. In the default sRGB space.
	 * 
	 * @since 1.4
	 */
	public final static Color YELLOW = yellow;

	/**
	 * The color green. In the default sRGB space.
	 */
	public final static Color green = new Color(0, 255, 0);

	/**
	 * The color green. In the default sRGB space.
	 * 
	 * @since 1.4
	 */
	public final static Color GREEN = green;

	/**
	 * The color magenta. In the default sRGB space.
	 */
	public final static Color magenta = new Color(255, 0, 255);

	/**
	 * The color magenta. In the default sRGB space.
	 * 
	 * @since 1.4
	 */
	public final static Color MAGENTA = magenta;

	/**
	 * The color cyan. In the default sRGB space.
	 */
	public final static Color cyan = new Color(0, 255, 255);

	/**
	 * The color cyan. In the default sRGB space.
	 * 
	 * @since 1.4
	 */
	public final static Color CYAN = cyan;

	/**
	 * The color blue. In the default sRGB space.
	 */
	public final static Color blue = new Color(0, 0, 255);

	/**
	 * The color blue. In the default sRGB space.
	 * 
	 * @since 1.4
	 */
	public final static Color BLUE = blue;

	/**
	 * The normalization factor.
	 */
	private final static double INV255 = 1.0 / 255.0;

	/**
	 * The color value.
	 * 
	 * @serial
	 * @see #getRGB
	 */
	int value;

	/**
	 * The color value in the default sRGB <code>ColorSpace</code> as
	 * <code>double</code> components (no alpha). If <code>null</code> after object
	 * construction, this must be an sRGB color constructed with 8-bit precision, so
	 * compute from the <code>int</code> color value.
	 * 
	 * @serial
	 * @see #getRGBColorComponents
	 * @see #getRGBComponents
	 */
	private double frgbvalue[] = null;

	/**
	 * The color value in the native <code>ColorSpace</code> as
	 * <code>double</code> components (no alpha). If <code>null</code> after object
	 * construction, this must be an sRGB color constructed with 8-bit precision, so
	 * compute from the <code>int</code> color value.
	 * 
	 * @serial
	 * @see #getRGBColorComponents
	 * @see #getRGBComponents
	 */
	private double fvalue[] = null;

	/**
	 * The alpha value as a <code>double</code> component. If <code>frgbvalue</code>
	 * is <code>null</code>, this is not valid data, so compute from the
	 * <code>int</code> color value.
	 * 
	 * @serial
	 * @see #getRGBComponents
	 * @see #getComponents
	 */
	private double falpha = 0.0;

	/**
	 * Creates an opaque sRGB color with the specified red, green, and blue values
	 * in the range (0 - 255). The actual color used in rendering depends on finding
	 * the best match given the color space available for a given output device.
	 * Alpha is defaulted to 255.
	 *
	 * @param r the red component
	 * @param g the green component
	 * @param b the blue component
	 * @see #getRed
	 * @see #getGreen
	 * @see #getBlue
	 * @see #getRGB
	 */
	public Color(int r, int g, int b) {
		this(r, g, b, 255);
	}

	/**
	 * Creates an sRGB color with the specified red, green, blue, and alpha
	 * values in the range (0 - 255).
	 *
	 * @param r the red component
	 * @param g the green component
	 * @param b the blue component
	 * @param a the alpha component
	 * @see #getRed
	 * @see #getGreen
	 * @see #getBlue
	 * @see #getAlpha
	 * @see #getRGB
	 */
	public Color(int r, int g, int b, int a) {
		// GWT: truncating instead of exceptions
		r = Math.min(Math.max(r, 0), 255);
		g = Math.min(Math.max(g, 0), 255);
		b = Math.min(Math.max(b, 0), 255);
		a = Math.min(Math.max(a, 0), 255);
		value = ((a & 0xFF) << 24) |
				((r & 0xFF) << 16) |
				((g & 0xFF) << 8) |
				((b & 0xFF) << 0);
	}

	/**
	 * Creates an opaque sRGB color with the specified combined RGB value
	 * consisting of the red component in bits 16-23, the green component
	 * in bits 8-15, and the blue component in bits 0-7. The actual color
	 * used in rendering depends on finding the best match given the color space
	 * available for a particular output device. Alpha is defaulted to 255.
	 *
	 * @param rgb the combined RGB components
	 * @see java.awt.image.ColorModel#getRGBdefault
	 * @see #getRed
	 * @see #getGreen
	 * @see #getBlue
	 * @see #getRGB
	 */
	public Color(int rgb) {
		value = 0xff000000 | rgb;
	}

	/**
	 * Creates an sRGB color with the specified combined RGBA value consisting
	 * of the alpha component in bits 24-31, the red component in bits 16-23,
	 * the green component in bits 8-15, and the blue component in bits 0-7.
	 * If the <code>hasalpha</code> argument is <code>false</code>, alpha
	 * is defaulted to 255.
	 *
	 * @param rgba     the combined RGBA components
	 * @param hasalpha <code>true</code> if the alpha bits are valid;
	 *                 <code>false</code> otherwise
	 * @see java.awt.image.ColorModel#getRGBdefault
	 * @see #getRed
	 * @see #getGreen
	 * @see #getBlue
	 * @see #getAlpha
	 * @see #getRGB
	 */
	public Color(int rgba, boolean hasalpha) {
		if (hasalpha) {
			value = rgba;
		} else {
			value = 0xff000000 | rgba;
		}
	}

	/**
	 * Creates an opaque sRGB color with the specified red, green, and blue values
	 * in the range (0.0 - 1.0). Alpha is defaulted to 1.0. The actual color used in
	 * rendering depends on finding the best match given the color space available
	 * for a particular output device.
	 *
	 * @param r the red component
	 * @param g the green component
	 * @param b the blue component
	 * @see #getRed
	 * @see #getGreen
	 * @see #getBlue
	 * @see #getRGB
	 */
	public Color(double r, double g, double b) {
		this((int) (r * 255 + 0.5), (int) (g * 255 + 0.5), (int) (b * 255 + 0.5));
		// GWT: truncating instead of exceptions
		frgbvalue = new double[3];
		frgbvalue[0] = Math.max(0.0, Math.min(r, 1.0));
		frgbvalue[1] = Math.max(0.0, Math.min(g, 1.0));
		frgbvalue[2] = Math.max(0.0, Math.min(b, 1.0));
		falpha = 1.0;
		fvalue = frgbvalue;
	}

	/**
	 * Creates an sRGB color with the specified red, green, blue, and alpha values
	 * in the range (0.0 - 1.0). The actual color used in rendering depends on
	 * finding the best match given the color space available for a particular
	 * output device.
	 * 
	 * @param r the red component
	 * @param g the green component
	 * @param b the blue component
	 * @param a the alpha component
	 * @see #getRed
	 * @see #getGreen
	 * @see #getBlue
	 * @see #getAlpha
	 * @see #getRGB
	 */
	public Color(double r, double g, double b, double a) {
		this((int) (r * 255 + 0.5), (int) (g * 255 + 0.5), (int) (b * 255 + 0.5), (int) (a * 255 + 0.5));
		frgbvalue = new double[3];
		frgbvalue[0] = r;
		frgbvalue[1] = g;
		frgbvalue[2] = b;
		falpha = a;
		fvalue = frgbvalue;
	}

	/**
	 * Returns the red component in the range 0-255 in the default sRGB space.
	 * 
	 * @return the red component.
	 * @see #getRGB
	 */
	public int getRed() {
		return (getRGB() >> 16) & 0xFF;
	}

	/**
	 * Returns the green component in the range 0-255 in the default sRGB space.
	 * 
	 * @return the green component.
	 * @see #getRGB
	 */
	public int getGreen() {
		return (getRGB() >> 8) & 0xFF;
	}

	/**
	 * Returns the blue component in the range 0-255 in the default sRGB space.
	 * 
	 * @return the blue component.
	 * @see #getRGB
	 */
	public int getBlue() {
		return (getRGB() >> 0) & 0xFF;
	}

	/**
	 * Returns the alpha component in the range 0-255.
	 * 
	 * @return the alpha component.
	 * @see #getRGB
	 */
	public int getAlpha() {
		return (getRGB() >> 24) & 0xff;
	}

	/**
	 * Returns the RGB value representing the color in the default sRGB
	 * {@link java.awt.image.ColorModel}. (Bits 24-31 are alpha, 16-23 are red, 8-15
	 * are green, 0-7 are blue).
	 * 
	 * @return the RGB value of the color in the default sRGB
	 *         <code>ColorModel</code>.
	 * @see java.awt.image.ColorModel#getRGBdefault
	 * @see #getRed
	 * @see #getGreen
	 * @see #getBlue
	 * @since JDK1.0
	 */
	public int getRGB() {
		return value;
	}

	private static final double FACTOR = 0.7;

	/**
	 * Creates a new <code>Color</code> that is a brighter version of this
	 * <code>Color</code>.
	 * <p>
	 * This method applies an arbitrary scale factor to each of the three RGB
	 * components of this <code>Color</code> to create a brighter version of this
	 * <code>Color</code>. The {@code alpha} value is preserved. Although
	 * <code>brighter</code> and <code>darker</code> are inverse operations, the
	 * results of a series of invocations of these two methods might be inconsistent
	 * because of rounding errors.
	 * 
	 * @return a new <code>Color</code> object that is
	 *         a brighter version of this <code>Color</code>
	 *         with the same {@code alpha} value.
	 * @see java.awt.Color#darker
	 * @since JDK1.0
	 */
	public Color brighter() {
		int r = getRed();
		int g = getGreen();
		int b = getBlue();
		int alpha = getAlpha();

		/*
		 * From 2D group:
		 * 1. black.brighter() should return grey
		 * 2. applying brighter to blue will always return blue, brighter
		 * 3. non pure color (non zero rgb) will eventually return white
		 */
		int i = (int) (1.0 / (1.0 - FACTOR));
		if (r == 0 && g == 0 && b == 0) {
			return new Color(i, i, i, alpha);
		}
		if (r > 0 && r < i)
			r = i;
		if (g > 0 && g < i)
			g = i;
		if (b > 0 && b < i)
			b = i;

		return new Color(Math.min((int) (r / FACTOR), 255),
				Math.min((int) (g / FACTOR), 255),
				Math.min((int) (b / FACTOR), 255),
				alpha);
	}

	/**
	 * Creates a new <code>Color</code> that is a darker version of this
	 * <code>Color</code>.
	 * <p>
	 * This method applies an arbitrary scale factor to each of the three RGB
	 * components of this <code>Color</code> to create a darker version of this
	 * <code>Color</code>. The {@code alpha} value is preserved. Although
	 * <code>brighter</code> and <code>darker</code> are inverse operations, the
	 * results of a series of invocations of these two methods might be inconsistent
	 * because of rounding errors.
	 * 
	 * @return a new <code>Color</code> object that is
	 *         a darker version of this <code>Color</code>
	 *         with the same {@code alpha} value.
	 * @see java.awt.Color#brighter
	 * @since JDK1.0
	 */
	public Color darker() {
		return new Color(Math.max((int) (getRed() * FACTOR), 0),
				Math.max((int) (getGreen() * FACTOR), 0),
				Math.max((int) (getBlue() * FACTOR), 0),
				getAlpha());
	}

	/**
	 * Computes the hash code for this <code>Color</code>.
	 * 
	 * @return a hash code value for this object.
	 * @since JDK1.0
	 */
	@Override
	public int hashCode() {
		return value;
	}

	/**
	 * Determines whether another object is equal to this <code>Color</code>.
	 * <p>
	 * The result is <code>true</code> if and only if the argument is not
	 * <code>null</code> and is a <code>Color</code> object that has the same red,
	 * green, blue, and alpha values as this object.
	 * 
	 * @param obj the object to test for equality with this
	 *            <code>Color</code>
	 * @return <code>true</code> if the objects are the same;
	 *         <code>false</code> otherwise.
	 * @since JDK1.0
	 */
	@Override
	public boolean equals(Object obj) {
		return obj instanceof Color && ((Color) obj).getRGB() == this.getRGB();
	}

	/**
	 * Returns a string representation of this <code>Color</code>. This method is
	 * intended to be used only for debugging purposes. The content and format of
	 * the returned string might vary between implementations. The returned string
	 * might be empty but cannot be <code>null</code>.
	 *
	 * @return a string representation of this <code>Color</code>.
	 */
	@Override
	public String toString() {
		int alpha = getAlpha();
		if (alpha == 0xff)
			return getClass().getName() + "[r=" + getRed() + ",g=" + getGreen() + ",b=" + getBlue() + "]";
		return getClass().getName() + "[r=" + getRed() + ",g=" + getGreen() + ",b=" + getBlue() + ",a=" + alpha + "]";
	}

	/**
	 * Converts a <code>String</code> to an integer and returns the specified opaque
	 * <code>Color</code>. This method handles string formats that are used to
	 * represent octal and hexadecimal numbers.
	 * 
	 * @param nm a <code>String</code> that represents
	 *           an opaque color as a 24-bit integer
	 * @return the new <code>Color</code> object.
	 * @see java.lang.Integer#decode
	 * @since JDK1.1
	 */
	public static Color decode(String nm) {
		Integer intval = Integer.decode(nm);
		int i = intval.intValue();
		return new Color((i >> 16) & 0xFF, (i >> 8) & 0xFF, i & 0xFF);
	}

	/**
	 * Converts the components of a color, as specified by the HSB model, to an
	 * equivalent set of values for the default RGB model.
	 * <p>
	 * The <code>saturation</code> and <code>brightness</code> components should be
	 * floating-point values between zero and one (numbers in the range 0.0-1.0).
	 * The <code>hue</code> component can be any floating-point number. The floor of
	 * this number is subtracted from it to create a fraction between 0 and 1. This
	 * fractional number is then multiplied by 360 to produce the hue angle in the
	 * HSB color model.
	 * <p>
	 * The integer that is returned by <code>HSBtoRGB</code> encodes the value of a
	 * color in bits 0-23 of an integer value that is the same format used by the
	 * method {@link #getRGB() <code>getRGB</code>}. This integer can be supplied as
	 * an argument to the <code>Color</code> constructor that takes a single integer
	 * argument.
	 * 
	 * @param hue        the hue component of the color
	 * @param saturation the saturation of the color
	 * @param brightness the brightness of the color
	 * @return the RGB value of the color with the indicated hue,
	 *         saturation, and brightness.
	 * @see java.awt.Color#getRGB()
	 * @see java.awt.Color#Color(int)
	 * @see java.awt.image.ColorModel#getRGBdefault()
	 * @since JDK1.0
	 */
	public static int HSBtoRGB(double hue, double saturation, double brightness) {
		int r = 0, g = 0, b = 0;
		if (saturation == 0) {
			r = g = b = (int) (brightness * 255.0 + 0.5);
		} else {
			double h = (hue - Math.floor(hue)) * 6.0;
			double f = h - Math.floor(h);
			double p = brightness * (1.0 - saturation);
			double q = brightness * (1.0 - saturation * f);
			double t = brightness * (1.0 - (saturation * (1.0 - f)));
			switch ((int) h) {
				case 0:
					r = (int) (brightness * 255.0 + 0.5);
					g = (int) (t * 255.0 + 0.5);
					b = (int) (p * 255.0 + 0.5);
					break;
				case 1:
					r = (int) (q * 255.0 + 0.5);
					g = (int) (brightness * 255.0 + 0.5);
					b = (int) (p * 255.0 + 0.5);
					break;
				case 2:
					r = (int) (p * 255.0 + 0.5);
					g = (int) (brightness * 255.0 + 0.5);
					b = (int) (t * 255.0 + 0.5);
					break;
				case 3:
					r = (int) (p * 255.0 + 0.5);
					g = (int) (q * 255.0 + 0.5);
					b = (int) (brightness * 255.0 + 0.5);
					break;
				case 4:
					r = (int) (t * 255.0 + 0.5);
					g = (int) (p * 255.0 + 0.5);
					b = (int) (brightness * 255.0 + 0.5);
					break;
				case 5:
					r = (int) (brightness * 255.0 + 0.5);
					g = (int) (p * 255.0 + 0.5);
					b = (int) (q * 255.0 + 0.5);
					break;
			}
		}
		return 0xff000000 | (r << 16) | (g << 8) | (b << 0);
	}

	/**
	 * Converts the components of a color, as specified by the default RGB model, to
	 * an equivalent set of values for hue, saturation, and brightness that are the
	 * three components of the HSB model.
	 * <p>
	 * If the <code>hsbvals</code> argument is <code>null</code>, then a new array
	 * is allocated to return the result. Otherwise, the method returns the array
	 * <code>hsbvals</code>, with the values put into that array.
	 * 
	 * @param r       the red component of the color
	 * @param g       the green component of the color
	 * @param b       the blue component of the color
	 * @param hsbvals the array used to return the
	 *                three HSB values, or <code>null</code>
	 * @return an array of three elements containing the hue, saturation,
	 *         and brightness (in that order), of the color with
	 *         the indicated red, green, and blue components.
	 * @see java.awt.Color#getRGB()
	 * @see java.awt.Color#Color(int)
	 * @see java.awt.image.ColorModel#getRGBdefault()
	 * @since JDK1.0
	 */
	public static double[] RGBtoHSB(int r, int g, int b, double[] hsbvals) {
		double hue, saturation, brightness;
		if (hsbvals == null) {
			hsbvals = new double[3];
		}
		int cmax = (r > g) ? r : g;
		if (b > cmax)
			cmax = b;
		int cmin = (r < g) ? r : g;
		if (b < cmin)
			cmin = b;

		brightness = cmax * INV255;
		if (cmax != 0)
			saturation = ((double) (cmax - cmin)) / ((double) cmax);
		else
			saturation = 0;
		if (saturation == 0)
			hue = 0;
		else {
			double redc = ((double) (cmax - r)) / ((double) (cmax - cmin));
			double greenc = ((double) (cmax - g)) / ((double) (cmax - cmin));
			double bluec = ((double) (cmax - b)) / ((double) (cmax - cmin));
			if (r == cmax)
				hue = bluec - greenc;
			else if (g == cmax)
				hue = 2.0 + redc - bluec;
			else
				hue = 4.0 + greenc - redc;
			hue = hue / 6.0;
			if (hue < 0)
				hue = hue + 1.0;
		}
		hsbvals[0] = hue;
		hsbvals[1] = saturation;
		hsbvals[2] = brightness;
		return hsbvals;
	}

	/**
	 * Creates a <code>Color</code> object based on the specified values for the HSB
	 * color model.
	 * <p>
	 * The <code>s</code> and <code>b</code> components should be floating-point
	 * values between zero and one (numbers in the range 0.0-1.0). The
	 * <code>h</code> component can be any floating-point number. The floor of this
	 * number is subtracted from it to create a fraction between 0 and 1. This
	 * fractional number is then multiplied by 360 to produce the hue angle in the
	 * HSB color model.
	 * 
	 * @param h the hue component
	 * @param s the saturation of the color
	 * @param b the brightness of the color
	 * @return a <code>Color</code> object with the specified hue,
	 *         saturation, and brightness.
	 * @since JDK1.0
	 */
	public static Color getHSBColor(double h, double s, double b) {
		return new Color(HSBtoRGB(h, s, b));
	}

	/**
	 * Returns a <code>double</code> array containing the color and alpha components
	 * of the <code>Color</code>, as represented in the default sRGB color space. If
	 * <code>compArray</code> is <code>null</code>, an array of length 4 is created
	 * for the return value. Otherwise, <code>compArray</code> must have length 4 or
	 * greater, and it is filled in with the components and returned.
	 * 
	 * @param compArray an array that this method fills with
	 *                  color and alpha components and returns
	 * @return the RGBA components in a <code>double</code> array.
	 */
	public double[] getRGBComponents(double[] compArray) {
		double[] f;
		if (compArray == null) {
			f = new double[4];
		} else {
			f = compArray;
		}
		if (frgbvalue == null) {
			f[0] = getRed() * INV255;
			f[1] = getGreen() * INV255;
			f[2] = getBlue() * INV255;
			f[3] = getAlpha() * INV255;
		} else {
			f[0] = frgbvalue[0];
			f[1] = frgbvalue[1];
			f[2] = frgbvalue[2];
			f[3] = falpha;
		}
		return f;
	}

	/**
	 * Returns a <code>double</code> array containing only the color components of
	 * the <code>Color</code>, in the default sRGB color space. If
	 * <code>compArray</code> is <code>null</code>, an array of length 3 is created
	 * for the return value. Otherwise, <code>compArray</code> must have length 3 or
	 * greater, and it is filled in with the components and returned.
	 * 
	 * @param compArray an array that this method fills with color
	 *                  components and returns
	 * @return the RGB components in a <code>double</code> array.
	 */
	public double[] getRGBColorComponents(double[] compArray) {
		double[] f;
		if (compArray == null) {
			f = new double[3];
		} else {
			f = compArray;
		}
		if (frgbvalue == null) {
			f[0] = getRed() * INV255;
			f[1] = getGreen() * INV255;
			f[2] = getBlue() * INV255;
		} else {
			f[0] = frgbvalue[0];
			f[1] = frgbvalue[1];
			f[2] = frgbvalue[2];
		}
		return f;
	}

	/**
	 * Returns a <code>double</code> array containing the color and alpha components
	 * of the <code>Color</code>, in the <code>ColorSpace</code> of the
	 * <code>Color</code>. If <code>compArray</code> is <code>null</code>, an array
	 * with length equal to the number of components in the associated
	 * <code>ColorSpace</code> plus one is created for the return value. Otherwise,
	 * <code>compArray</code> must have at least this length and it is filled in
	 * with the components and returned.
	 * 
	 * @param compArray an array that this method fills with the color and
	 *                  alpha components of this <code>Color</code> in its
	 *                  <code>ColorSpace</code> and returns
	 * @return the color and alpha components in a <code>double</code>
	 *         array.
	 */
	public double[] getComponents(double[] compArray) {
		if (fvalue == null)
			return getRGBComponents(compArray);
		double[] f;
		int n = fvalue.length;
		if (compArray == null) {
			f = new double[n + 1];
		} else {
			f = compArray;
		}
		for (int i = 0; i < n; i++) {
			f[i] = fvalue[i];
		}
		f[n] = falpha;
		return f;
	}

	/**
	 * Returns a <code>double</code> array containing only the color components of
	 * the <code>Color</code>, in the <code>ColorSpace</code> of the
	 * <code>Color</code>. If <code>compArray</code> is <code>null</code>, an array
	 * with length equal to the number of components in the associated
	 * <code>ColorSpace</code> is created for the return value. Otherwise,
	 * <code>compArray</code> must have at least this length and it is filled in
	 * with the components and returned.
	 * 
	 * @param compArray an array that this method fills with the color
	 *                  components of this <code>Color</code> in its
	 *                  <code>ColorSpace</code> and returns
	 * @return the color components in a <code>double</code> array.
	 */
	public double[] getColorComponents(double[] compArray) {
		if (fvalue == null)
			return getRGBColorComponents(compArray);
		double[] f;
		int n = fvalue.length;
		if (compArray == null) {
			f = new double[n];
		} else {
			f = compArray;
		}
		for (int i = 0; i < n; i++) {
			f[i] = fvalue[i];
		}
		return f;
	}
}
