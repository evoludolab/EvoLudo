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

package org.evoludo.util;

import java.text.DecimalFormat;

/**
 * Collection of convenience methods for formatting numbers, vectors and
 * matrices as Strings.
 * 
 * @author Christoph Hauert
 */
public class Formatter {

	/**
	 * The maximum number of decimal digits to format.
	 */
	private static final int MAX_DIGITS = 12;

	/**
	 * Delimiter between matrix rows.
	 *
	 * <strong>Note:</strong> keep in sync with {@link CLOParser#MATRIX_DELIMITER}
	 */
	public static final String MATRIX_DELIMITER = "; ";

	/**
	 * Delimiter between vector elements.
	 *
	 * <strong>Note:</strong> keep in sync with {@link CLOParser#VECTOR_DELIMITER}
	 */
	public static final String VECTOR_DELIMITER = ", ";

	/**
	 * Array of formatters with 0 to up to 12 non-zero decimal digits.
	 */
	private static final DecimalFormat[] myFormatters = { new DecimalFormat("0"), new DecimalFormat("0.#"),
			new DecimalFormat("0.##"), new DecimalFormat("0.###"), new DecimalFormat("0.####"),
			new DecimalFormat("0.#####"), new DecimalFormat("0.######"), new DecimalFormat("0.#######"),
			new DecimalFormat("0.########"), new DecimalFormat("0.#########"), new DecimalFormat("0.##########"),
			new DecimalFormat("0.###########"), new DecimalFormat("0.############") };

	/**
	 * Array of formatters with fixed number of digits ranging from 0 to 12.
	 */
	private static final DecimalFormat[] myFixFormatters = { new DecimalFormat("0"), new DecimalFormat("0.0"),
			new DecimalFormat("0.00"), new DecimalFormat("0.000"), new DecimalFormat("0.0000"),
			new DecimalFormat("0.00000"), new DecimalFormat("0.000000"), new DecimalFormat("0.0000000"),
			new DecimalFormat("0.00000000"), new DecimalFormat("0.000000000"), new DecimalFormat("0.0000000000"),
			new DecimalFormat("0.00000000000"), new DecimalFormat("0.000000000000") };

	/**
	 * Array of scientific formatters with fixed number of digits ranging from 0 to
	 * up to 12 non-zero decimal digits.
	 */
	private static final DecimalFormat[] mySciFormatters = { new DecimalFormat("0E0"), new DecimalFormat("0.#E0"),
			new DecimalFormat("0.##E0"), new DecimalFormat("0.###E0"), new DecimalFormat("0.####E0"),
			new DecimalFormat("0.#####E0"), new DecimalFormat("0.######E0"), new DecimalFormat("0.#######E0"),
			new DecimalFormat("0.########E0"), new DecimalFormat("0.#########E0"), new DecimalFormat("0.##########E0"),
			new DecimalFormat("0.###########E0"), new DecimalFormat("0.############E0") };

	/**
	 * Array of percentage formatters with fixed number of digits ranging from 0 to
	 * 12.
	 */
	private static final DecimalFormat[] myPercentFormatters = { new DecimalFormat("##0%"), new DecimalFormat("##0.0%"),
			new DecimalFormat("##0.00%"), new DecimalFormat("##0.000%"), new DecimalFormat("##0.0000%"),
			new DecimalFormat("##0.00000%"), new DecimalFormat("##0.000000%"), new DecimalFormat("##0.0000000%"),
			new DecimalFormat("##0.00000000%"), new DecimalFormat("##0.000000000%"),
			new DecimalFormat("##0.0000000000%"), new DecimalFormat("##0.00000000000%"),
			new DecimalFormat("##0.000000000000%") };

	/**
	 * Ensure non-instantiability with private default constructor
	 */
	private Formatter() {
	}

	/**
	 * Format the vector <code>aVector</code> of type {@code T} as String with
	 * elements separated by '{@value #VECTOR_DELIMITER}'.
	 * 
	 * @param <T>     the type of the vector
	 * @param aVector the vector to format
	 * @return the formatted String vector
	 */
	public static <T> String format(T[] aVector) {
		return format(aVector, VECTOR_DELIMITER);
	}

	/**
	 * Format the vector <code>aVector</code> of type {@code T} as a String with
	 * elements separated by {@code delimiter} string.
	 * 
	 * @param <T>       the type of the vector
	 * @param aVector   the string vector to format
	 * @param delimiter the delimiter for separating the entries in {@code aVector}
	 * @return the formatted String vector
	 */
	public static <T> String format(T[] aVector, String delimiter) {
		if (aVector == null)
			return "";
		int len = aVector.length;
		if (len == 0)
			return "";
		String vecString = aVector[0].toString();
		for (int i = 1; i < len; i++)
			vecString += delimiter + aVector[i];
		return vecString;
	}

	/**
	 * Format integer <code>anInteger</code> as String
	 * 
	 * @param anInteger number to format
	 * @return formatted <code>int</code> as String
	 */
	public static String format(int anInteger) {
		return (myFormatters[0]).format(anInteger);
	}

	/**
	 * Format integer array/vector <code>aVector</code> as String. Elements are
	 * separated by '{@value #VECTOR_DELIMITER}'.
	 * 
	 * @param aVector array to format
	 * @return formatted <code>int[]</code> as String
	 */
	public static String format(int[] aVector) {
		if (aVector == null)
			return "";
		int len = aVector.length;
		if (len == 0)
			return "";
		String vecString = format(aVector[0]);
		for (int i = 1; i < len; i++)
			vecString += VECTOR_DELIMITER + format(aVector[i]);
		return vecString;
	}

	/**
	 * Format array/matrix of integers <code>aMatrix</code> as String. Column
	 * elements are separated by '{@value #VECTOR_DELIMITER}' and rows of elements
	 * by '{@value #MATRIX_DELIMITER}'.
	 * 
	 * @param aMatrix array to format
	 * @return formatted <code>int[][]</code> as String
	 */
	public static String format(int[][] aMatrix) {
		if (aMatrix == null)
			return "";
		int len = aMatrix.length;
		if (len == 0)
			return "";
		String matString = format(aMatrix[0]);
		for (int i = 1; i < len; i++)
			matString += MATRIX_DELIMITER + format(aMatrix[i]);
		return matString;
	}

	/**
	 * Format double <code>aDouble</code> as String with at most <code>digits</code>
	 * decimal places (trailing zeroes are suppressed).
	 * 
	 * @param aDouble number to format
	 * @param digits  number of decimal places
	 * @return formatted <code>double</code> as String
	 */
	public static String format(double aDouble, int digits) {
		// at least zero, at most six decimal digits
		digits = Math.max(Math.min(digits, MAX_DIGITS), 0);
		return (myFormatters[digits]).format(aDouble);
	}

	/**
	 * Format array/vector of doubles <code>aVector</code> as String. Elements are
	 * separated by '{@value #VECTOR_DELIMITER}' and formatted with at most
	 * <code>digits</code> decimal places (trailing zeroes are suppressed).
	 * 
	 * @param aVector array to format
	 * @param digits  number of decimal places
	 * @return formatted <code>double[]</code> as String
	 */
	public static String format(double[] aVector, int digits) {
		if (aVector == null)
			return "";
		int len = aVector.length;
		if (len == 0)
			return "";
		digits = Math.max(Math.min(digits, MAX_DIGITS), 0);
		String vecString = (myFormatters[digits]).format(aVector[0]);
		for (int i = 1; i < len; i++)
			vecString += VECTOR_DELIMITER + (myFormatters[digits]).format(aVector[i]);
		return vecString;
	}

	/**
	 * Format array/matrix of doubles <code>aMatrix</code> as String. Column
	 * elements are separated by '{@value #VECTOR_DELIMITER}' and rows of elements
	 * by '{@value #MATRIX_DELIMITER}'. Each element is formatted with at most
	 * <code>digits</code> decimal places (trailing zeroes are suppressed).
	 * 
	 * @param aMatrix array to format
	 * @param digits  number of decimal places
	 * @return formatted <code>double[][]</code> as String
	 */
	public static String format(double[][] aMatrix, int digits) {
		if (aMatrix == null)
			return "";
		int len = aMatrix.length;
		if (len == 0)
			return "";
		String matString = format(aMatrix[0], digits);
		for (int i = 1; i < len; i++)
			matString += MATRIX_DELIMITER + format(aMatrix[i], digits);
		return matString;
	}

	/**
	 * Format array/vector of floats <code>aVector</code> as String. Elements are
	 * separated by '{@value #VECTOR_DELIMITER}' and formatted with at most
	 * <code>digits</code> decimal places (trailing zeroes are suppressed).
	 * 
	 * @param aVector array to format
	 * @param digits  number of decimal places
	 * @return formatted <code>float[]</code> as String
	 */
	public static String format(float[] aVector, int digits) {
		if (aVector == null)
			return "";
		int len = aVector.length;
		if (len == 0)
			return "";
		digits = Math.max(Math.min(digits, MAX_DIGITS), 0);
		String vecString = (myFormatters[digits]).format(aVector[0]);
		for (int i = 1; i < len; i++)
			vecString += VECTOR_DELIMITER + (myFormatters[digits]).format(aVector[i]);
		return vecString;
	}

	/**
	 * Format array/matrix of floats <code>aMatrix</code> as String. Column elements
	 * are separated by '{@value #VECTOR_DELIMITER}' and rows of elements by
	 * '{@value #MATRIX_DELIMITER}'. Each element is formatted with at most
	 * <code>digits</code> decimal places (trailing zeroes are suppressed).
	 * 
	 * @param aMatrix array to format
	 * @param digits  number of decimal places
	 * @return formatted <code>float[][]</code> as String
	 */
	public static String format(float[][] aMatrix, int digits) {
		if (aMatrix == null)
			return "";
		int len = aMatrix.length;
		if (len == 0)
			return "";
		String matString = format(aMatrix[0], digits);
		for (int i = 1; i < len; i++)
			matString += MATRIX_DELIMITER + format(aMatrix[i], digits);
		return matString;
	}

	/**
	 * Format double <code>aDouble</code> as String with <code>digits</code> decimal
	 * places (trailing zeroes are included).
	 * 
	 * @param aDouble number to format
	 * @param digits  number of decimal places
	 * @return formatted <code>double</code> as String
	 */
	public static String formatFix(double aDouble, int digits) {
		// at least zero, at most six decimal digits
		digits = Math.max(Math.min(digits, MAX_DIGITS), 0);
		return (myFixFormatters[digits]).format(aDouble);
	}

	/**
	 * Format array/vector of doubles <code>aVector</code> as String. Elements are
	 * separated by '{@value #VECTOR_DELIMITER}' and formatted with
	 * <code>digits</code> decimal places (trailing zeroes are included).
	 * 
	 * @param aVector array to format
	 * @param digits  number of decimal places
	 * @return formatted <code>double[]</code> as String
	 */
	public static String formatFix(double[] aVector, int digits) {
		if (aVector == null)
			return "";
		int len = aVector.length;
		if (len == 0)
			return "";
		digits = Math.max(Math.min(digits, MAX_DIGITS), 0);
		String vecString = (myFixFormatters[digits]).format(aVector[0]);
		for (int i = 1; i < len; i++)
			vecString += VECTOR_DELIMITER + (myFixFormatters[digits]).format(aVector[i]);
		return vecString;
	}

	/**
	 * Format array/matrix of doubles <code>aMatrix</code> as String. Column
	 * elements are separated by '{@value #VECTOR_DELIMITER}' and rows of elements
	 * by '{@value #MATRIX_DELIMITER}'. Each element is formatted with
	 * <code>digits</code> decimal places (trailing zeroes are included).
	 * 
	 * @param aMatrix array to format
	 * @param digits  number of decimal places
	 * @return formatted <code>double[][]</code> as String
	 */
	public static String formatFix(double[][] aMatrix, int digits) {
		if (aMatrix == null)
			return "";
		int len = aMatrix.length;
		if (len == 0)
			return "";
		String matString = formatFix(aMatrix[0], digits);
		for (int i = 1; i < len; i++)
			matString += MATRIX_DELIMITER + formatFix(aMatrix[i], digits);
		return matString;
	}

	/**
	 * Format double <code>aDouble</code> as String with up to <code>digits</code>
	 * decimal places (trailing zeroes are suppressed) forcing scientific formatting
	 * including exponent (separated by 'E').
	 * 
	 * @param aDouble number to format
	 * @param digits  number of decimal places
	 * @return formatted <code>double</code> as String
	 */
	public static String formatSci(double aDouble, int digits) {
		// at least zero, at most six decimal digits
		digits = Math.max(Math.min(digits, MAX_DIGITS), 0);
		return (mySciFormatters[digits]).format(aDouble);
	}

	/**
	 * Format array/vector of doubles <code>aVector</code> as String. Elements are
	 * separated by '{@value #VECTOR_DELIMITER}' and formatted with up to
	 * <code>digits</code> decimal places (trailing zeroes are suppressed) forcing
	 * scientific formatting including exponent (separated by 'E').
	 * 
	 * @param aVector array to format
	 * @param digits  number of decimal places
	 * @return formatted <code>double[]</code> as String
	 */
	public static String formatSci(double[] aVector, int digits) {
		if (aVector == null)
			return "";
		int len = aVector.length;
		if (len == 0)
			return "";
		digits = Math.max(Math.min(digits, MAX_DIGITS), 0);
		String vecString = (mySciFormatters[digits]).format(aVector[0]);
		for (int i = 1; i < len; i++)
			vecString += VECTOR_DELIMITER + (mySciFormatters[digits]).format(aVector[i]);
		return vecString;
	}

	/**
	 * Format array/matrix of doubles <code>aMatrix</code> as String. Column
	 * elements are separated by '{@value #VECTOR_DELIMITER}' and rows of elements
	 * by '{@value #MATRIX_DELIMITER}'. Each element is formatted with up to
	 * <code>digits</code> decimal places (trailing zeroes are suppressed) forcing
	 * scientific formatting including exponent (separated by 'E').
	 * 
	 * @param aMatrix array to format
	 * @param digits  number of decimal places
	 * @return formatted <code>double[][]</code> as String
	 */
	public static String formatSci(double[][] aMatrix, int digits) {
		if (aMatrix == null)
			return "";
		int len = aMatrix.length;
		if (len == 0)
			return "";
		String matString = formatSci(aMatrix[0], digits);
		for (int i = 1; i < len; i++)
			matString += MATRIX_DELIMITER + formatSci(aMatrix[i], digits);
		return matString;
	}

	/**
	 * The thresholds for 'pretty' formatting of exponents for each number of
	 * digits. For absolute values between the upper and lower bound fixed
	 * formatting is used, otherwise scientific formatting is applied.
	 */
	private static final double[][] prettyThresholds = new double[][] {
			/* 0 */ { 1.0, 1e3 },
			/* 1 */ { 1.0, 1e3 },
			/* 2 */ { 1.0, 1e3 },
			/* 3 */ { 1e-1, 1e3 },
			/* 4 */ { 1e-1, 1e4 },
			/* 5 */ { 1e-2, 1e5 },
			/* 6 */ { 1e-3, 1e6 },
			/* 7 */ { 1e-3, 1e6 },
			/* 8 */ { 1e-3, 1e6 },
			/* 9 */ { 1e-3, 1e6 },
			/* 10 */ { 1e-3, 1e6 },
			/* 11 */ { 1e-3, 1e6 },
			/* 12 */ { 1e-3, 1e6 } };

	/**
	 * Helper method to format a double <code>aDouble</code> as HTML string with
	 * <code>digits</code> decimal places (trailing zeroes are included).
	 * 
	 * @param aDouble the number to format
	 * @param digits  the number of decimal places
	 * @return the formatted <code>double</code> as HTML string
	 */
	private static String prettyFormat(double aDouble, int digits) {
		double[] thresh = prettyThresholds[digits];
		double abs = Math.abs(aDouble);
		// catch zero
		if (abs < Double.MIN_VALUE)
			return myFixFormatters[digits].format(aDouble);
		if (abs > thresh[1] || abs < thresh[0])
			return mySciFormatters[digits].format(aDouble).replace("E", "⋅10<sup>") + "</sup>";
		return myFixFormatters[digits].format(aDouble);
	}

	/**
	 * Same as {@link #formatSci(double, int)} but formatting of exponent
	 * 'prettyfied' using HTML.
	 * 
	 * @param aDouble number to format
	 * @param digits  number of decimal places
	 * @return formatted <code>double</code> as HTML string
	 */
	public static String pretty(double aDouble, int digits) {
		return prettyFormat(aDouble, Math.max(Math.min(digits, MAX_DIGITS), 0));
	}

	/**
	 * Same as {@link #formatSci(double[], int)} but formatting of exponents
	 * 'prettyfied' using HTML.
	 * 
	 * @param aVector number to format
	 * @param digits  number of decimal places
	 * @return formatted <code>double[]</code> as HTML string
	 */
	public static String pretty(double[] aVector, int digits) {
		if (aVector == null)
			return "";
		int len = aVector.length;
		if (len == 0)
			return "";
		digits = Math.max(Math.min(digits, MAX_DIGITS), 0);
		String vecString = prettyFormat(aVector[0], digits);
		for (int i = 1; i < len; i++)
			vecString += VECTOR_DELIMITER + prettyFormat(aVector[i], digits);
		return vecString;
	}

	/**
	 * Same as {@link #formatSci(double[], int)} but formatting of exponents
	 * 'prettyfied' using HTML.
	 * 
	 * @param aMatrix number to format
	 * @param digits  number of decimal places
	 * @return formatted <code>double[][]</code> as HTML string
	 */
	public static String pretty(double[][] aMatrix, int digits) {
		if (aMatrix == null)
			return "";
		int len = aMatrix.length;
		if (len == 0)
			return "";
		String matString = pretty(aMatrix[0], digits);
		for (int i = 1; i < len; i++)
			matString += MATRIX_DELIMITER + pretty(aMatrix[i], digits);
		return matString;
	}

	/**
	 * Format double <code>aDouble</code> as percent String with <code>digits</code>
	 * decimal places (trailing zeroes are included).
	 * 
	 * @param aDouble number to format
	 * @param digits  number of decimal places
	 * @return formatted <code>double</code> as String
	 */
	public static String formatPercent(double aDouble, int digits) {
		// at least zero, at most six decimal digits
		digits = Math.max(Math.min(digits, MAX_DIGITS), 0);
		return (myPercentFormatters[digits]).format(aDouble);
	}
}
