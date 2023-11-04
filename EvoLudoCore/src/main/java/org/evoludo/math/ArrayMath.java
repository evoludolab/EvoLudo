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

package org.evoludo.math;

import java.util.Arrays;

/**
 * Collection of convenience methods for mathematical operations, including
 * array manipulations and statistics.
 * 
 * @author Christoph Hauert
 */
public class ArrayMath {

	/**
	 * Ensure non-instantiability with private default constructor
	 */
	private ArrayMath() {
	}

	/**
	 * Append {@code element} to {@code boolean} array {@code array}. The length of
	 * the returned array is {@code array.length +1}.
	 * 
	 * @param array   the array
	 * @param element the element to append
	 * @return the new (longer) array
	 */
	public static boolean[] append(boolean[] array, boolean element) {
		int len = array.length;
		boolean[] newarray = Arrays.copyOf(array, len + 1);
		newarray[len] = element;
		return newarray;
	}

	/**
	 * Append {@code element} to {@code int} array {@code array}. The length of the
	 * returned array is {@code array.length +1}.
	 * 
	 * @param array   the array
	 * @param element the element to append
	 * @return the new (longer) array
	 */
	public static int[] append(int[] array, int element) {
		int len = array.length;
		int[] newarray = Arrays.copyOf(array, len + 1);
		newarray[len] = element;
		return newarray;
	}

	/**
	 * Append {@code element} to {@code double} array {@code array}. The length of
	 * the returned array is {@code array.length +1}.
	 * 
	 * @param array   the array
	 * @param element the element to append
	 * @return the new (longer) array
	 */
	public static double[] append(double[] array, double element) {
		int len = array.length;
		double[] newarray = Arrays.copyOf(array, len + 1);
		newarray[len] = element;
		return newarray;
	}

	/**
	 * Append {@code element} of type {@code T} to array {@code array} of the same
	 * type. The length of the returned array is {@code array.length +1}.
	 * 
	 * @param <T>     the type of the array
	 * @param array   the array
	 * @param element the element to append
	 * @return the new (longer) array
	 */
	public static <T> T[] append(T[] array, T element) {
		int len = array.length;
		T[] newarray = Arrays.copyOf(array, len + 1);
		newarray[len] = element;
		return newarray;
	}

	/**
	 * Drop the element at {@code index} from the array {@code array} of type
	 * {@code boolean[]}. The length of the returned array is
	 * {@code array.length - 1}.
	 * 
	 * @param array the array
	 * @param index the index of the element to remove
	 * @return the new (shorter) array
	 */
	public static boolean[] drop(boolean[] array, int index) {
		return drop(array, index, index + 1);
	}

	/**
	 * Drop a range of elements starting with index {@code from} to index {@code to}
	 * from the {@code boolean[]} array {@code array}. The length of the returned
	 * array is {@code array.length - (to - from)}.
	 * 
	 * @param array the array
	 * @param from  the first index to drop
	 * @param to    the last index to drop
	 * @return the new (shorter) array
	 */
	public static boolean[] drop(boolean[] array, int from, int to) {
		int nDrop = to - from;
		boolean[] newarray = Arrays.copyOf(array, array.length - nDrop);
		System.arraycopy(array, to, newarray, from, newarray.length - from);
		return newarray;
	}

	/**
	 * Drop the element at {@code index} from the array {@code array} of type
	 * {@code int[]}. The length of the returned array is {@code array.length - 1}.
	 * 
	 * @param array the array
	 * @param index the index of the element to remove
	 * @return the new (shorter) array
	 */
	public static int[] drop(int[] array, int index) {
		return drop(array, index, index + 1);
	}

	/**
	 * Drop a range of elements starting with index {@code from} to index {@code to}
	 * from the {@code int[]} array {@code array}. The length of the returned array
	 * is {@code array.length - (to - from)}.
	 * 
	 * @param array the array
	 * @param from  the first index to drop
	 * @param to    the last index to drop
	 * @return the new (shorter) array
	 */
	public static int[] drop(int[] array, int from, int to) {
		int nDrop = to - from;
		int[] newarray = Arrays.copyOf(array, array.length - nDrop);
		System.arraycopy(array, to, newarray, from, newarray.length - from);
		return newarray;
	}

	/**
	 * Drop the element at {@code index} from the array {@code array} of type
	 * {@code double[]}. The length of the returned array is
	 * {@code array.length - 1}.
	 * 
	 * @param array the array
	 * @param index the index of the element to remove
	 * @return the new (shorter) array
	 */
	public static double[] drop(double[] array, int index) {
		return drop(array, index, index + 1);
	}

	/**
	 * Drop a range of elements starting with index {@code from} to index {@code to}
	 * from the {@code double[]} array {@code array}. The length of the returned
	 * array is
	 * {@code array.length - (to - from)}.
	 * 
	 * @param array the array
	 * @param from  the first index to drop
	 * @param to    the last index to drop
	 * @return the new (shorter) array
	 */
	public static double[] drop(double[] array, int from, int to) {
		int nDrop = to - from;
		double[] newarray = Arrays.copyOf(array, array.length - nDrop);
		System.arraycopy(array, to, newarray, from, newarray.length - from);
		return newarray;
	}

	/**
	 * Drop the element at {@code index} from the array {@code array} of type
	 * {@code T}. The length of the returned array is {@code array.length - 1}.
	 * 
	 * @param <T>   the type of the array
	 * @param array the array
	 * @param index the index of the element to remove
	 * @return the new (shorter) array
	 */
	public static <T> T[] drop(T[] array, int index) {
		return drop(array, index, index + 1);
	}

	/**
	 * Drop a range of elements starting with index {@code from} to index {@code to}
	 * from the array {@code array} of type {@code T}. The length of the returned
	 * array is {@code array.length - (to - from)}.
	 * 
	 * @param <T>   the type of the array
	 * @param array the array
	 * @param from  the first index to drop
	 * @param to    the last index to drop
	 * @return the new (shorter) array
	 */
	public static <T> T[] drop(T[] array, int from, int to) {
		int nDrop = to - from;
		T[] newarray = Arrays.copyOf(array, array.length - nDrop);
		System.arraycopy(array, to, newarray, from, newarray.length - from);
		return newarray;
	}

	/**
	 * Insert element {@code element} of type {@code boolean} into array
	 * {@code array} of the same type at {@code index}. The length of the returned
	 * array is {@code array.length + 1}.
	 * 
	 * @param array   the array
	 * @param element the element to insert
	 * @param index   the insertion point
	 * @return the new (longer) array
	 */
	public static boolean[] insert(boolean[] array, boolean element, int index) {
		int len = array.length;
		if (index >= len)
			return append(array, element);
		boolean[] newarray = Arrays.copyOf(array, len + 1);
		System.arraycopy(array, index, newarray, index + 1, len - index);
		newarray[index] = element;
		return newarray;
	}

	/**
	 * Insert element {@code element} of type {@code int} into array {@code array}
	 * of the same type at {@code index}. The length of the returned array is
	 * {@code array.length + 1}.
	 * 
	 * @param array   the array
	 * @param element the element to insert
	 * @param index   the insertion point
	 * @return the new (longer) array
	 */
	public static int[] insert(int[] array, int element, int index) {
		int len = array.length;
		if (index >= len)
			return append(array, element);
		int[] newarray = Arrays.copyOf(array, len + 1);
		System.arraycopy(array, index, newarray, index + 1, len - index);
		newarray[index] = element;
		return newarray;
	}

	/**
	 * Insert element {@code element} of type {@code double} into array
	 * {@code array} of the same type at {@code index}. The length of the returned
	 * array is {@code array.length + 1}.
	 * 
	 * @param array   the array
	 * @param element the element to insert
	 * @param index   the insertion point
	 * @return the new (longer) array
	 */
	public static double[] insert(double[] array, double element, int index) {
		int len = array.length;
		if (index >= len)
			return append(array, element);
		double[] newarray = Arrays.copyOf(array, len + 1);
		System.arraycopy(array, index, newarray, index + 1, len - index);
		newarray[index] = element;
		return newarray;
	}

	/**
	 * Insert element {@code element} of type {@code T} into array {@code array} of
	 * the same type at {@code index}. The length of the returned array is
	 * {@code array.length + 1}.
	 * 
	 * @param <T>     the type of the element and the array
	 * @param array   the array
	 * @param element the element to insert
	 * @param index   the insertion point
	 * @return the new (longer) array
	 */
	public static <T> T[] insert(T[] array, T element, int index) {
		int len = array.length;
		if (index >= len)
			return append(array, element);
		T[] newarray = Arrays.copyOf(array, len + 1);
		System.arraycopy(array, index, newarray, index + 1, len - index);
		newarray[index] = element;
		return newarray;
	}

	/**
	 * Compares the two specified {@code int[]} arrays. Returns zero if the arrays
	 * are identical and <code>1</code> otherwise. In contrast to
	 * <code>Integer.compare(int, int)</code> there is no obvious ranking of the two
	 * arrays.
	 *
	 * @param a1 the first {@code int[]} array to compare
	 * @param a2 the second {@code int[]} array to compare
	 * @return the value {@code 0} if {@code a1} is numerically equal to {@code a2}
	 *         and {@code 1} otherwise
	 * 
	 * @see Integer#compare(int, int)
	 */
	public static int compare(int[] a1, int[] a2) {
		if (a1 == null || a2 == null || a1.length != a2.length)
			return 1;
		if (a1 == a2)
			return 0;
		int dim = a1.length;
		for (int n = 0; n < dim; n++)
			if (a1[n] != a2[n])
				return 1;
		return 0;
	}

	/**
	 * Compares the two specified {@code int[][]} arrays. Returns zero if the arrays
	 * are identical and <code>1</code> otherwise. In contrast to
	 * <code>Integer.compare(int, int)</code> there is no obvious ranking of the two
	 * arrays.
	 *
	 * @param a1 the first {@code int[][]} array to compare
	 * @param a2 the second {@code int[][]} array to compare
	 * @return the value {@code 0} if {@code a1} is numerically equal to {@code a2}
	 *         and {@code 1} otherwise
	 * 
	 * @see Integer#compare(int, int)
	 */
	public static int compare(int[][] a1, int[][] a2) {
		if (a1 == null || a2 == null || a1.length != a2.length)
			return 1;
		if (a1 == a2)
			return 0;
		int dim = a1.length;
		for (int n = 0; n < dim; n++)
			if (compare(a1[n], a2[n]) != 0)
				return 1;
		return 0;
	}

	/**
	 * Compares the two specified {@code double[]} arrays. Returns zero if the
	 * arrays are identical and <code>1</code> otherwise. In contrast to
	 * <code>Double.compare(double, double)</code> there is no obvious ranking of
	 * the two arrays.
	 *
	 * @param a1 the first {@code double[]} array to compare
	 * @param a2 the second {@code double[]} array to compare
	 * @return the value {@code 0} if {@code d1} is numerically equal to {@code d2}
	 *         and {@code 1} otherwise
	 * 
	 * @see Double#compare(double, double)
	 */
	public static int compare(double[] a1, double[] a2) {
		if (a1 == null || a2 == null || a1.length != a2.length)
			return 1;
		if (a1 == a2)
			return 0;
		int dim = a1.length;
		for (int n = 0; n < dim; n++)
			if (Double.compare(a1[n], a2[n]) != 0)
				return 1;
		return 0;
	}

	/**
	 * Compares the two specified {@code double[][]} arrays. Returns zero if the
	 * arrays are identical and <code>1</code> otherwise. In contrast to
	 * <code>Double.compare(double, double)</code> there is no obvious ranking of
	 * the two arrays.
	 *
	 * @param a1 the first {@code double[][]} array to compare
	 * @param a2 the second {@code double[][]} array to compare
	 * @return the value {@code 0} if {@code d1} is numerically equal to {@code d2}
	 *         and {@code 1} otherwise
	 * 
	 * @see Double#compare(double, double)
	 */
	public static int compare(double[][] a1, double[][] a2) {
		if (a1 == null || a2 == null || a1.length != a2.length)
			return 1;
		if (a1 == a2)
			return 0;
		int dim = a1.length;
		for (int n = 0; n < dim; n++)
			if (compare(a1[n], a2[n]) != 0)
				return 1;
		return 0;
	}

	/**
	 * Find the first occurence of the element {@code b} in the array
	 * {@code int[] a}.
	 * 
	 * @param a the array
	 * @param b the element to look for
	 * @return the index of the first occurrence of {@code b} or {@code -1} if
	 *         {@code a} does not contain any element {@code b}
	 */
	public static int first(int[] a, int b) {
		if (a == null)
			return -1;
		for (int n = 0; n < a.length; n++)
			if (a[n] == b)
				return n;
		return -1;
	}

	/**
	 * Find the first occurence of the element {@code b} in the array
	 * {@code double[] a}.
	 * 
	 * @param a the array
	 * @param b the element to look for
	 * @return the index of the first occurrence of {@code b} or {@code -1} if
	 *         {@code a} does not contain any element {@code b}
	 */
	public static int first(double[] a, double b) {
		if (a == null)
			return -1;
		for (int n = 0; n < a.length; n++)
			if (Double.compare(a[n], b) == 0)
				return n;
		return -1;
	}

	/**
	 * Find the last occurence of the element {@code b} in the array
	 * {@code int[] a}.
	 * 
	 * @param a the array
	 * @param b the element to look for
	 * @return the index of the first occurrence of {@code b} or {@code -1} if
	 *         {@code a} does not contain any element {@code b}
	 */
	public static int last(int[] a, int b) {
		if (a == null)
			return -1;
		for (int n = a.length - 1; n >= 0; n--)
			if (a[n] == b)
				return n;
		return -1;
	}

	/**
	 * Find the last occurence of the element {@code b} in the array
	 * {@code double[] a}.
	 * 
	 * @param a the array
	 * @param b the element to look for
	 * @return the index of the first occurrence of {@code b} or {@code -1} if
	 *         {@code a} does not contain any element {@code b}
	 */
	public static int last(double[] a, int b) {
		if (a == null)
			return -1;
		for (int n = a.length - 1; n >= 0; n--)
			if (Double.compare(a[n], b) == 0)
				return n;
		return -1;
	}

	/**
	 * Find minimum element in boolean array/vector <code>a</code>.
	 * 
	 * @param a the <code>boolean[]</code> array
	 * @return <code>true</code> if all elements are <code>true</code> and
	 *         <code>false</code> if at least one element is <code>false</code>
	 */
	public static boolean min(boolean[] a) {
		int dim = a.length;
		for (int n = 0; n < dim; n++)
			if (!a[n])
				return false;
		return true;
	}

	/**
	 * Find minimum element in integer array/vector <code>a</code>.
	 * 
	 * @param a the array <code>int[]</code>
	 * @return the minimum element
	 */
	public static int min(int[] a) {
		int dim = a.length;
		int min = Integer.MAX_VALUE;
		for (int n = 0; n < dim; n++)
			min = Math.min(min, a[n]);
		return min;
	}

	/**
	 * Find minimum element in long array/vector <code>a</code>.
	 * 
	 * @param a the array <code>long[]</code>
	 * @return the minimum element
	 */
	public static long min(long[] a) {
		int dim = a.length;
		long min = Long.MAX_VALUE;
		for (int n = 0; n < dim; n++)
			min = Math.min(min, a[n]);
		return min;
	}

	/**
	 * Find minimum element in float array/vector <code>a</code>.
	 * 
	 * @param a the array <code>float[]</code>
	 * @return the minimum element
	 */
	public static float min(float[] a) {
		int dim = a.length;
		float min = Float.MAX_VALUE;
		for (int n = 0; n < dim; n++)
			min = Math.min(min, a[n]);
		return min;
	}

	/**
	 * Find minimum element in double array/vector <code>a</code>.
	 * 
	 * @param a the array <code>double[]</code>
	 * @return the minimum element
	 */
	public static double min(double[] a) {
		int dim = a.length;
		double min = Double.MAX_VALUE;
		for (int n = 0; n < dim; n++)
			min = Math.min(min, a[n]);
		return min;
	}

	/**
	 * Find minimum element in integer array/matrix <code>a</code>.
	 * 
	 * @param a the array <code>int[][]</code>
	 * @return the minimum element
	 */
	public static int min(int[][] a) {
		int dim = a.length;
		int min = Integer.MAX_VALUE;
		for (int n = 0; n < dim; n++)
			min = Math.min(min, min(a[n]));
		return min;
	}

	/**
	 * Find minimum element in long array/matrix <code>a</code>.
	 * 
	 * @param a the array <code>long[][]</code>
	 * @return the minimum element
	 */
	public static long min(long[][] a) {
		int dim = a.length;
		long min = Long.MAX_VALUE;
		for (int n = 0; n < dim; n++)
			min = Math.min(min, min(a[n]));
		return min;
	}

	/**
	 * Find minimum element in float array/matrix <code>a</code>.
	 * 
	 * @param a the array <code>float[][]</code>
	 * @return the minimum element
	 */
	public static float min(float[][] a) {
		int dim = a.length;
		float min = Float.MAX_VALUE;
		for (int n = 0; n < dim; n++)
			min = Math.min(min, min(a[n]));
		return min;
	}

	/**
	 * Find minimum element in double array/matrix <code>a</code>.
	 * 
	 * @param a the array <code>double[][]</code>
	 * @return the minimum element
	 */
	public static double min(double[][] a) {
		int dim = a.length;
		double min = Double.MAX_VALUE;
		for (int n = 0; n < dim; n++)
			min = Math.min(min, min(a[n]));
		return min;
	}

	/**
	 * Find index of minimum element in integer array.
	 * 
	 * @param a the array <code>int[]</code>
	 * @return the index of the minimum element
	 */
	public static int minIndex(int[] a) {
		int dim = a.length;
		int min = Integer.MAX_VALUE;
		int idx = -1;
		for (int n = 0; n < dim; n++) {
			if (min < a[n])
				continue;
			min = a[n];
			idx = n;
		}
		return idx;
	}

	/**
	 * Find index of minimum element in long array.
	 * 
	 * @param a the array <code>long[]</code>
	 * @return the index of the minimum element
	 */
	public static int minIndex(long[] a) {
		int dim = a.length;
		long min = Long.MAX_VALUE;
		int idx = -1;
		for (int n = 0; n < dim; n++) {
			if (min < a[n])
				continue;
			min = a[n];
			idx = n;
		}
		return idx;
	}

	/**
	 * Find index of minimum element in float array.
	 * 
	 * @param a the array <code>float[]</code>
	 * @return the index of the minimum element
	 */
	public static int minIndex(float[] a) {
		int dim = a.length;
		float min = Float.MAX_VALUE;
		int idx = -1;
		for (int n = 0; n < dim; n++) {
			float an = a[n];
			if (min > an) {
				min = an;
				idx = n;
			}
		}
		return idx;
	}

	/**
	 * Find index of minimum element in double array.
	 * 
	 * @param a the array <code>double[]</code>
	 * @return the index of the minimum element
	 */
	public static int minIndex(double[] a) {
		int dim = a.length;
		double min = Double.MAX_VALUE;
		int idx = -1;
		for (int n = 0; n < dim; n++) {
			double an = a[n];
			if (min > an) {
				min = an;
				idx = n;
			}
		}
		return idx;
	}

	/**
	 * Find minimum element in two float arrays <code>a</code> and <code>b</code>.
	 * 
	 * @param a the first array
	 * @param b the second array
	 * @return the minimum element in <code>a</code> and <code>b</code>
	 */
	public static float[] min(float[] a, float[] b) {
		int dim = a.length;
		for (int n = 0; n < dim; n++)
			a[n] = Math.min(a[n], b[n]);
		return a;
	}

	/**
	 * Find minimum element in two double arrays <code>a</code> and <code>b</code>.
	 * 
	 * @param a the first array
	 * @param b the second array
	 * @return the minimum element in <code>a</code> and <code>b</code>
	 */
	public static double[] min(double[] a, double[] b) {
		int dim = a.length;
		for (int n = 0; n < dim; n++)
			a[n] = Math.min(a[n], b[n]);
		return a;
	}

	/**
	 * Find minimum element in double array <code>a</code> among <code>active</code>
	 * elements. More precisely, disregard elements <code>a[i]</code> with
	 * <code>active[i]==false</code>.
	 * 
	 * @param a      the array <code>double[]</code>
	 * @param active the array <code>boolean[]</code> indicating whether element
	 *               should be skipped (<code>false</code>) or considered
	 *               (<code>true</code>).
	 * @return the minimum active element
	 */
	public static double min(double[] a, boolean[] active) {
		int dim = a.length;
		double min = Double.MAX_VALUE;
		for (int n = 0; n < dim; n++) {
			if (!active[n])
				continue;
			min = Math.min(min, a[n]);
		}
		return min;
	}

	/**
	 * Find maximum element in boolean array/vector <code>a</code>.
	 * 
	 * @param a the <code>boolean[]</code> array
	 * @return <code>false</code> if all elements are <code>false</code> and
	 *         <code>true</code> if at least one element is <code>true</code>
	 */
	public static boolean max(boolean[] a) {
		int dim = a.length;
		for (int n = 0; n < dim; n++)
			if (!a[n])
				return false;
		return true;
	}

	/**
	 * Find maximum element in int array/vector <code>a</code>.
	 * 
	 * @param a the array <code>int[]</code>
	 * @return the maximum element
	 */
	public static int max(int[] a) {
		int dim = a.length;
		int max = -Integer.MAX_VALUE;
		for (int n = 0; n < dim; n++)
			max = Math.max(max, a[n]);
		return max;
	}

	/**
	 * Find maximum element in long array/vector <code>a</code>.
	 * 
	 * @param a the array <code>long[]</code>
	 * @return the maximum element
	 */
	public static long max(long[] a) {
		int dim = a.length;
		long max = -Long.MAX_VALUE;
		for (int n = 0; n < dim; n++)
			max = Math.max(max, a[n]);
		return max;
	}

	/**
	 * Find maximum element in float array/vector <code>a</code>.
	 * 
	 * @param a the array <code>float[]</code>
	 * @return the maximum element
	 */
	public static float max(float[] a) {
		int dim = a.length;
		float max = -Float.MAX_VALUE;
		for (int n = 0; n < dim; n++)
			max = Math.max(max, a[n]);
		return max;
	}

	/**
	 * Find maximum element in double array/vector <code>a</code>.
	 * 
	 * @param a the array <code>double[]</code>
	 * @return the maximum element
	 */
	public static double max(double[] a) {
		int dim = a.length;
		double max = -Double.MAX_VALUE;
		for (int n = 0; n < dim; n++)
			max = Math.max(max, a[n]);
		return max;
	}

	/**
	 * Find maximum element in int array/matrix <code>a</code>.
	 * 
	 * @param a the array <code>int[][]</code>
	 * @return the maximum element
	 */
	public static int max(int[][] a) {
		int dim = a.length;
		int max = -Integer.MAX_VALUE;
		for (int n = 0; n < dim; n++)
			max = Math.max(max, max(a[n]));
		return max;
	}

	/**
	 * Find maximum element in long array/matrix <code>a</code>.
	 * 
	 * @param a the array <code>long[][]</code>
	 * @return the maximum element
	 */
	public static long max(long[][] a) {
		int dim = a.length;
		long max = -Long.MAX_VALUE;
		for (int n = 0; n < dim; n++)
			max = Math.max(max, max(a[n]));
		return max;
	}

	/**
	 * Find maximum element in float array/matrix <code>a</code>.
	 * 
	 * @param a the array <code>float[][]</code>
	 * @return the maximum element
	 */
	public static float max(float[][] a) {
		int dim = a.length;
		float max = -Float.MAX_VALUE;
		for (int n = 0; n < dim; n++)
			max = Math.max(max, max(a[n]));
		return max;
	}

	/**
	 * Find maximum element in double array/matrix <code>a</code>.
	 * 
	 * @param a the array <code>double[][]</code>
	 * @return the maximum element
	 */
	public static double max(double[][] a) {
		int dim = a.length;
		double max = -Double.MAX_VALUE;
		for (int n = 0; n < dim; n++)
			max = Math.max(max, max(a[n]));
		return max;
	}

	/**
	 * Find index of maximum element in integer array.
	 * 
	 * @param a the array <code>int[]</code>
	 * @return the index of maximum element
	 */
	public static int maxIndex(int[] a) {
		int dim = a.length;
		int max = -Integer.MAX_VALUE;
		int idx = -1;
		for (int n = 0; n < dim; n++) {
			if (max > a[n])
				continue;
			max = a[n];
			idx = n;
		}
		return idx;
	}

	/**
	 * Find index of maximum element in long array.
	 * 
	 * @param a the array <code>long[]</code>
	 * @return the index of maximum element
	 */
	public static int maxIndex(long[] a) {
		int dim = a.length;
		long max = -Long.MAX_VALUE;
		int idx = -1;
		for (int n = 0; n < dim; n++) {
			if (max > a[n])
				continue;
			max = a[n];
			idx = n;
		}
		return idx;
	}

	/**
	 * Find index of maximum element in float array.
	 * 
	 * @param a the array <code>float[]</code>
	 * @return the index of maximum element
	 */
	public static int maxIndex(float[] a) {
		int dim = a.length;
		float max = -Float.MAX_VALUE;
		int idx = -1;
		for (int n = 0; n < dim; n++) {
			float an = a[n];
			if (max < an) {
				max = an;
				idx = n;
			}
		}
		return idx;
	}

	/**
	 * Find index of maximum element in double array.
	 * 
	 * @param a the array <code>double[]</code>
	 * @return the index of maximum element
	 */
	public static int maxIndex(double[] a) {
		int dim = a.length;
		double max = -Double.MAX_VALUE;
		int idx = -1;
		for (int n = 0; n < dim; n++) {
			double an = a[n];
			if (max < an) {
				max = an;
				idx = n;
			}
		}
		return idx;
	}

	/**
	 * Find maximum element in two float arrays <code>a</code> and <code>b</code>.
	 * 
	 * @param a the first array
	 * @param b the second array
	 * @return the maximum element in <code>a</code> and <code>b</code>
	 */
	public static float[] max(float[] a, float[] b) {
		int dim = a.length;
		for (int n = 0; n < dim; n++)
			a[n] = Math.max(a[n], b[n]);
		return a;
	}

	/**
	 * Find maximum element in two double arrays <code>a</code> and <code>b</code>.
	 * 
	 * @param a the first array
	 * @param b the second array
	 * @return the maximum element in <code>a</code> and <code>b</code>
	 */
	public static double[] max(double[] a, double[] b) {
		int dim = a.length;
		for (int n = 0; n < dim; n++)
			a[n] = Math.max(a[n], b[n]);
		return a;
	}

	/**
	 * Find maximum element in double array <code>a</code> among <code>active</code>
	 * elements. More precisely, disregard elements <code>a[i]</code> with
	 * <code>active[i]==false</code>.
	 * 
	 * @param a      the array <code>double[]</code>
	 * @param active the array <code>boolean[]</code> indicating whether element
	 *               should be skipped (<code>false</code>) or considered
	 *               (<code>true</code>).
	 * @return the maximum active element
	 */
	public static double max(double[] a, boolean[] active) {
		int dim = a.length;
		double max = -Double.MAX_VALUE;
		for (int n = 0; n < dim; n++) {
			if (!active[n])
				continue;
			max = Math.max(max, a[n]);
		}
		return max;
	}

	/**
	 * Norm of integer array <code>a</code>.
	 * 
	 * @param a the array <code>int[]</code>
	 * @return the sum of all elements <code>a[i]</code>
	 */
	public static int norm(int[] a) {
		int dim = a.length;
		int norm = 0;
		for (int n = 0; n < dim; n++)
			norm += a[n];
		return norm;
	}

	/**
	 * Norm of long array <code>a</code>.
	 * 
	 * @param a the rray <code>long[]</code>
	 * @return the sum of all elements <code>a[i]</code>
	 */
	public static long norm(long[] a) {
		int dim = a.length;
		long norm = 0L;
		for (int n = 0; n < dim; n++)
			norm += a[n];
		return norm;
	}

	/**
	 * Norm of float array <code>a</code>.
	 * 
	 * @param a the array <code>float[]</code>
	 * @return the sum of all elements <code>a[i]</code>
	 */
	public static float norm(float[] a) {
		int dim = a.length;
		float norm = 0f;
		for (int n = 0; n < dim; n++)
			norm += a[n];
		return norm;
	}

	/**
	 * Norm of double array <code>a</code>.
	 * 
	 * @param a the array <code>double[]</code>
	 * @return the sum of all elements <code>a[i]</code>
	 */
	public static double norm(double[] a) {
		int dim = a.length;
		double norm = 0.0;
		for (int n = 0; n < dim; n++)
			norm += a[n];
		return norm;
	}

	/**
	 * Normalize float array <code>a</code>. Scales elements such that the sum over
	 * all elements <code>a[i]</code> adds up to <code>1</code>. Normalization
	 * usually only makes sense if the sign of all elements is the same but this is
	 * not checked. Elements of <code>a</code> are overwritten with new values.
	 * 
	 * @param a the array <code>float[]</code>
	 * @return the normalized array <code>a</code>
	 */
	public static float[] normalize(float[] a) {
		return normalize(a, 0, a.length);
	}

	/**
	 * Normalize elements ranging from index <code>from</code> to index
	 * <code>to</code> in float array <code>a</code>. Scales elements such that the
	 * sum over elements <code>a[i]</code> with <code>i=from,...,to</code> add up to
	 * <code>1</code>. Normalization usually only makes sense if the sign of all
	 * elements is the same but this is not checked. Elements of <code>a</code> are
	 * overwritten with new values.
	 * 
	 * @param a    the <code>float[]</code> array to normalize
	 * @param from the start index of the section to normalize
	 * @param to   the end index of the section to normalize
	 * @return the array <code>a</code> with normalized section
	 * 
	 * @see #normalize(double[])
	 */
	public static float[] normalize(float[] a, int from, int to) {
		float norm = 0f;
		for (int n = from; n < to; n++)
			norm += a[n];
		norm = 1f / norm;
		for (int n = from; n < to; n++)
			a[n] *= norm;
		return a;
	}

	/**
	 * Normalize double array <code>a</code>. Scales elements such that the sum over
	 * all elements <code>a[i]</code> adds up to <code>1</code>. Normalization
	 * usually only makes sense if the sign of all elements is the same but this is
	 * not checked. Elements of <code>a</code> are overwritten with new values.
	 * 
	 * @param a the <code>double[]</code> array to normalize
	 * @return the normalized array <code>a</code>
	 * 
	 * @see #normalize(double[], int, int)
	 */
	public static double[] normalize(double[] a) {
		return normalize(a, 0, a.length);
	}

	/**
	 * Normalize elements ranging from index <code>from</code> to index
	 * <code>to</code> in double array <code>a</code>. Scales elements such that the
	 * sum over elements <code>a[i]</code> with <code>i=from,...,to</code> add up to
	 * <code>1</code>. Normalization usually only makes sense if the sign of all
	 * elements is the same but this is not checked. Elements of <code>a</code> are
	 * overwritten with new values.
	 * 
	 * @param a    the <code>double[]</code> array to normalize
	 * @param from the start index of the section to normalize
	 * @param to   the end index of the section to normalize
	 * @return the array <code>a</code> with normalized section
	 * 
	 * @see #normalize(double[])
	 */
	public static double[] normalize(double[] a, int from, int to) {
		double norm = 0.0;
		for (int n = from; n < to; n++)
			norm += a[n];
		norm = 1.0 / norm;
		for (int n = from; n < to; n++)
			a[n] *= norm;
		return a;
	}

	/**
	 * Element-wise copy of integer array/vector <code>src</code> to double
	 * array/vector <code>dst</code>.
	 * 
	 * @param src the <code>int[]</code> source array
	 * @param dst the <code>double[]</code> destination array
	 * @return the array <code>dst</code>
	 */
	public static double[] copy(int[] src, double[] dst) {
		if (dst == null || dst.length < src.length)
			dst = new double[src.length];
		int dim = src.length;
		for (int n = 0; n < dim; n++)
			dst[n] += src[n];
		return dst;
	}

	/**
	 * Copy the integer array/vector <code>src</code> to <code>dst</code>.
	 * 
	 * @param src the <code>int[]</code> source vector
	 * @param dst the <code>int[]</code> destination vector
	 * @return the matrix <code>dst</code>
	 */
	public static int[] copy(int[] src, int[] dst) {
		if (dst == null || dst.length < src.length)
			dst = new int[src.length];
		System.arraycopy(src, 0, dst, 0, src.length);
		return dst;
	}

	/**
	 * Copy the double 2D array/matrix <code>src</code> to <code>dst</code>.
	 * 
	 * @param src the <code>double[]</code> source matrix
	 * @param dst the <code>double[]</code> destination matrix
	 * @return the matrix <code>dst</code>
	 */
	public static double[] copy(double[] src, double[] dst) {
		if (dst == null || dst.length < src.length)
			dst = new double[src.length];
		System.arraycopy(src, 0, dst, 0, src.length);
		return dst;
	}

	/**
	 * Copy the integer 2D array/matrix <code>src</code> to <code>dst</code>.
	 * 
	 * @param src the <code>int[][]</code> source matrix
	 * @param dst the <code>int[][]</code> destination matrix
	 * @return the matrix <code>dst</code>
	 */
	public static int[][] copy(int[][] src, int[][] dst) {
		if (dst == null || dst.length < src.length)
			dst = new int[src.length][];
		int dim = src.length;
		for (int n = 0; n < dim; n++)
			copy(src[n], dst[n]);
		return dst;
	}

	/**
	 * Copy the double 2D array/matrix <code>src</code> to <code>dst</code>.
	 * 
	 * @param src the <code>double[][]</code> source matrix
	 * @param dst the <code>double[][]</code> destination matrix
	 * @return the matrix <code>dst</code>
	 */
	public static double[][] copy(double[][] src, double[][] dst) {
		if (dst == null || dst.length < src.length)
			dst = new double[src.length][];
		int dim = src.length;
		for (int n = 0; n < dim; n++)
			copy(src[n], dst[n]);
		return dst;
	}

	/**
	 * Add <code>scalar</code> value to each element of integer array/vector
	 * <code>dst</code>. Elements of <code>dst</code> are overwritten with new
	 * values.
	 * 
	 * @param dst    the array <code>int[]</code>
	 * @param scalar the value to add to each element of <code>dst</code>
	 * @return the modified array <code>dst</code>
	 */
	public static int[] add(int[] dst, int scalar) {
		int dim = dst.length;
		for (int n = 0; n < dim; n++)
			dst[n] += scalar;
		return dst;
	}

	/**
	 * Add <code>scalar</code> value to each element of long array/vector
	 * <code>dst</code>. Elements of <code>dst</code> are overwritten with new
	 * values.
	 * 
	 * @param dst    the array <code>long[]</code>
	 * @param scalar the value to add to each element of <code>dst</code>
	 * @return the modified array <code>dst</code>
	 */
	public static long[] add(long[] dst, long scalar) {
		int dim = dst.length;
		for (int n = 0; n < dim; n++)
			dst[n] += scalar;
		return dst;
	}

	/**
	 * Add <code>scalar</code> value to each element of float array/vector
	 * <code>dst</code>. Elements of <code>dst</code> are overwritten with new
	 * values.
	 * 
	 * @param dst    the array <code>float[]</code>
	 * @param scalar the value to add to each element of <code>dst</code>
	 * @return the modified array <code>dst</code>
	 */
	public static float[] add(float[] dst, float scalar) {
		int dim = dst.length;
		for (int n = 0; n < dim; n++)
			dst[n] += scalar;
		return dst;
	}

	/**
	 * Add <code>scalar</code> value to each element of double array/vector
	 * <code>dst</code>. Elements of <code>dst</code> are overwritten with new
	 * values.
	 * 
	 * @param dst    the array <code>double[]</code>
	 * @param scalar the value to add to each element of <code>dst</code>
	 * @return the modified array <code>dst</code>
	 */
	public static double[] add(double[] dst, double scalar) {
		int dim = dst.length;
		for (int n = 0; n < dim; n++)
			dst[n] += scalar;
		return dst;
	}

	/**
	 * Add integer arrays <code>dst</code> and <code>add</code>. Place result in
	 * <code>dst</code> (array <code>add</code> remains unchanged). Arrays
	 * <code>dst</code> and <code>add</code> can be of different lengths as long as
	 * <code>dst.length&lt;add.length</code>. If <code>add</code> is longer,
	 * additional elements are ignored.
	 * 
	 * @param dst the destination array
	 * @param add the array to add to <code>dst</code>
	 * @return the modified array <code>dst</code>
	 * @throws ArrayIndexOutOfBoundsException if
	 *                                        <code>add.length&lt;dst.length</code>
	 */
	public static int[] add(int[] dst, int[] add) {
		int dim = dst.length;
		for (int n = 0; n < dim; n++)
			dst[n] += add[n];
		return dst;
	}

	/**
	 * Add long arrays <code>dst</code> and <code>add</code>. Place result in
	 * <code>dst</code> (array <code>add</code> remains unchanged). Arrays
	 * <code>dst</code> and <code>add</code> can be of different lengths as long as
	 * <code>dst.length&lt;add.length</code>. If <code>add</code> is longer,
	 * additional elements are ignored.
	 * 
	 * @param dst the destination array
	 * @param add the array to add to <code>dst</code>
	 * @return the modified array <code>dst</code>
	 * @throws ArrayIndexOutOfBoundsException if
	 *                                        <code>add.length&lt;dst.length</code>
	 */
	public static long[] add(long[] dst, long[] add) {
		int dim = dst.length;
		for (int n = 0; n < dim; n++)
			dst[n] += add[n];
		return dst;
	}

	/**
	 * Add float arrays <code>dst</code> and <code>add</code>. Place result in
	 * <code>dst</code> (array <code>add</code> remains unchanged). Arrays
	 * <code>dst</code> and <code>add</code> can be of different lengths as long as
	 * <code>dst.length&lt;add.length</code>. If <code>add</code> is longer,
	 * additional elements are ignored.
	 * 
	 * @param dst the destination array
	 * @param add the array to add to <code>dst</code>
	 * @return the modified array <code>dst</code>
	 * @throws ArrayIndexOutOfBoundsException if
	 *                                        <code>add.length&lt;dst.length</code>
	 */
	public static float[] add(float[] dst, float[] add) {
		int dim = dst.length;
		for (int n = 0; n < dim; n++)
			dst[n] += add[n];
		return dst;
	}

	/**
	 * Add double arrays <code>dst</code> and <code>add</code>. Place result in
	 * <code>dst</code> (array <code>add</code> remains unchanged). Arrays
	 * <code>dst</code> and <code>add</code> can be of different lengths as long as
	 * <code>dst.length&lt;add.length</code>. If <code>add</code> is longer,
	 * additional elements are ignored.
	 * 
	 * @param dst the destination array
	 * @param add the array to add to <code>dst</code>
	 * @return the modified array <code>dst</code>
	 * @throws ArrayIndexOutOfBoundsException if
	 *                                        <code>add.length&lt;dst.length</code>
	 */
	public static double[] add(double[] dst, double[] add) {
		int dim = dst.length;
		for (int n = 0; n < dim; n++)
			dst[n] += add[n];
		return dst;
	}

	/**
	 * Non-destructive array addition. Add int arrays <code>a</code> and
	 * <code>b</code> and place result in <code>dst</code> (arrays <code>a</code>
	 * and <code>b</code> remain unchanged). Arrays <code>a</code>, <code>b</code>
	 * and <code>dst</code> can be of different lengths as long as
	 * <code>a.length&le;b.length&le;dst.length</code>. If <code>b</code> or
	 * <code>dst</code> is longer than <code>a</code>, additional elements are
	 * ignored.
	 * 
	 * @param a   the first array
	 * @param b   the second array
	 * @param dst the result array
	 * @return the modified array <code>dst=a+b</code>
	 * @throws ArrayIndexOutOfBoundsException if <code>dst.length&lt;a.length</code>
	 *                                        or <code>b.length&lt;a.length</code>
	 */
	public static int[] add(int[] a, int[] b, int[] dst) {
		int dim = a.length;
		for (int n = 0; n < dim; n++)
			dst[n] = a[n] + b[n];
		return dst;
	}

	/**
	 * Non-destructive array addition. Add long arrays <code>a</code> and
	 * <code>b</code> and place result in <code>dst</code> (arrays <code>a</code>
	 * and <code>b</code> remain unchanged). Arrays <code>a</code>, <code>b</code>
	 * and <code>dst</code> can be of different lengths as long as
	 * <code>a.length&le;b.length&le;dst.length</code>. If <code>b</code> or
	 * <code>dst</code> is longer than <code>a</code>, additional elements are
	 * ignored.
	 * 
	 * @param a   the first array
	 * @param b   the second array
	 * @param dst the result array
	 * @return the modified array <code>dst=a+b</code>
	 * @throws ArrayIndexOutOfBoundsException if <code>dst.length&lt;a.length</code>
	 *                                        or <code>b.length&lt;a.length</code>
	 */
	public static long[] add(long[] a, long[] b, long[] dst) {
		int dim = a.length;
		for (int n = 0; n < dim; n++)
			dst[n] = a[n] + b[n];
		return dst;
	}

	/**
	 * Non-destructive array addition. Add float arrays <code>a</code> and
	 * <code>b</code> and place result in <code>dst</code> (arrays <code>a</code>
	 * and <code>b</code> remain unchanged). Arrays <code>a</code>, <code>b</code>
	 * and <code>dst</code> can be of different lengths as long as
	 * <code>a.length&le;b.length&le;dst.length</code>. If <code>b</code> or
	 * <code>dst</code> is longer than <code>a</code>, additional elements are
	 * ignored.
	 * 
	 * @param a   the first array
	 * @param b   the second array
	 * @param dst the result array
	 * @return the modified array <code>dst=a+b</code>
	 * @throws ArrayIndexOutOfBoundsException if <code>dst.length&lt;a.length</code>
	 *                                        or <code>b.length&lt;a.length</code>
	 */
	public static float[] add(float[] a, float[] b, float[] dst) {
		int dim = a.length;
		for (int n = 0; n < dim; n++)
			dst[n] = a[n] + b[n];
		return dst;
	}

	/**
	 * Non-destructive array addition. Add double arrays <code>a</code> and
	 * <code>b</code> and place result in <code>dst</code> (arrays <code>a</code>
	 * and <code>b</code> remain unchanged). Arrays <code>a</code>, <code>b</code>
	 * and <code>dst</code> can be of different lengths as long as
	 * <code>a.length&le;b.length&le;dst.length</code>. If <code>b</code> or
	 * <code>dst</code> is longer than <code>a</code>, additional elements are
	 * ignored.
	 * 
	 * @param a   the first array
	 * @param b   the second array
	 * @param dst the result array
	 * @return the modified array <code>dst=a+b</code>
	 * @throws ArrayIndexOutOfBoundsException if <code>dst.length&lt;a.length</code>
	 *                                        or <code>b.length&lt;a.length</code>
	 */
	public static double[] add(double[] a, double[] b, double[] dst) {
		int dim = a.length;
		for (int n = 0; n < dim; n++)
			dst[n] = a[n] + b[n];
		return dst;
	}

	/**
	 * Non-destructive scalar multiplication and array addition. Multiply double
	 * array <code>b</code> by scalar value <code>s</code>, add the result to array
	 * <code>a</code> and store result in array <code>dst</code> (arrays
	 * <code>a</code> and <code>b</code> remain unchanged). Arrays <code>a</code>,
	 * <code>b</code> and <code>dst</code> can be of different lengths as long as
	 * <code>a.length&le;b.length&le;dst.length</code>. If <code>b</code> or
	 * <code>dst</code> are longer than <code>a</code>, additional elements are
	 * ignored.
	 * 
	 * @param a   the first array
	 * @param b   the second array
	 * @param dst the result array
	 * @param s   the scalar multiplier
	 * @return the modified array <code>dst=a+s*b</code>
	 * @throws ArrayIndexOutOfBoundsException if <code>a.length&lt;dst.length</code>
	 *                                        or <code>b.length&lt;dst.length</code>
	 */
	public static double[] addscale(double[] a, double[] b, double s, double[] dst) {
		int dim = a.length;
		for (int n = 0; n < dim; n++)
			dst[n] = a[n] + s * b[n];
		return dst;
	}

	/**
	 * Subtract integer array <code>sub</code> from array <code>dst</code>. Place
	 * result in <code>dst</code> (array <code>sub</code> remains unchanged). Arrays
	 * <code>dst</code> and <code>sub</code> can be of different lengths as long as
	 * <code>dst.length&lt;sub.length</code>. If <code>sub</code> is longer,
	 * additional elements are ignored.
	 * 
	 * @param dst the destination array
	 * @param sub the array to subtract from <code>dst</code>
	 * @return the modified array <code>dst</code>
	 * @throws ArrayIndexOutOfBoundsException if
	 *                                        <code>sub.length&lt;dst.length</code>
	 */
	public static int[] sub(int[] dst, int[] sub) {
		int dim = dst.length;
		for (int n = 0; n < dim; n++)
			dst[n] -= sub[n];
		return dst;
	}

	/**
	 * Subtract long array <code>sub</code> from array <code>dst</code>. Place
	 * result in <code>dst</code> (array <code>sub</code> remains unchanged). Arrays
	 * <code>dst</code> and <code>sub</code> can be of different lengths as long as
	 * <code>dst.length&lt;sub.length</code>. If <code>sub</code> is longer,
	 * additional elements are ignored.
	 * 
	 * @param dst the destination array
	 * @param sub the array to subtract from <code>dst</code>
	 * @return the modified array <code>dst</code>
	 * @throws ArrayIndexOutOfBoundsException if
	 *                                        <code>sub.length&lt;dst.length</code>
	 */
	public static long[] sub(long[] dst, long[] sub) {
		int dim = dst.length;
		for (int n = 0; n < dim; n++)
			dst[n] -= sub[n];
		return dst;
	}

	/**
	 * Subtract float array <code>sub</code> from array <code>dst</code>. Place
	 * result in <code>dst</code> (array <code>sub</code> remains unchanged). Arrays
	 * <code>dst</code> and <code>sub</code> can be of different lengths as long as
	 * <code>dst.length&lt;sub.length</code>. If <code>sub</code> is longer,
	 * additional elements are ignored.
	 * 
	 * @param dst the destination array
	 * @param sub the array to subtract from <code>dst</code>
	 * @return the modified array <code>dst</code>
	 * @throws ArrayIndexOutOfBoundsException if
	 *                                        <code>sub.length&lt;dst.length</code>
	 */
	public static float[] sub(float[] dst, float[] sub) {
		int dim = dst.length;
		for (int n = 0; n < dim; n++)
			dst[n] -= sub[n];
		return dst;
	}

	/**
	 * Subtract double array <code>sub</code> from array <code>dst</code>. Place
	 * result in <code>dst</code> (array <code>sub</code> remains unchanged). Arrays
	 * <code>dst</code> and <code>sub</code> can be of different lengths as long as
	 * <code>dst.length&lt;sub.length</code>. If <code>sub</code> is longer,
	 * additional elements are ignored.
	 * 
	 * @param dst the destination array
	 * @param sub the array to subtract from <code>dst</code>
	 * @return the modified array <code>dst</code>
	 * @throws ArrayIndexOutOfBoundsException if
	 *                                        <code>sub.length&lt;dst.length</code>
	 */
	public static double[] sub(double[] dst, double[] sub) {
		int dim = dst.length;
		for (int n = 0; n < dim; n++)
			dst[n] -= sub[n];
		return dst;
	}

	/**
	 * Non-destructive array subtraction. Subtract integer array <code>sub</code>
	 * from array <code>orig</code> and place result in <code>dst</code> (arrays
	 * <code>orig</code> and <code>sub</code> remain unchanged). Arrays
	 * <code>orig</code>, <code>sub</code> and <code>dst</code> can be of different
	 * lengths as long as <code>orig.length&le;sub.length&le;dst.length</code>. If
	 * <code>dst</code> or <code>sub</code> are longer, additional elements are
	 * ignored.
	 * 
	 * @param orig the first array
	 * @param sub  the second array to subtract from <code>orig</code>
	 * @param dst  the result array
	 * @return the modified array <code>dst=orig-sub</code>
	 * @throws ArrayIndexOutOfBoundsException if
	 *                                        <code>dst.length&lt;orig.length</code>
	 *                                        or
	 *                                        <code>sub.length&lt;orig.length</code>
	 */
	public static int[] sub(int[] orig, int[] sub, int[] dst) {
		int dim = orig.length;
		for (int n = 0; n < dim; n++)
			dst[n] = orig[n] - sub[n];
		return dst;
	}

	/**
	 * Non-destructive array subtraction. Subtract long array <code>sub</code> from
	 * array <code>orig</code> and place result in <code>dst</code> (arrays
	 * <code>orig</code> and <code>sub</code> remain unchanged). Arrays
	 * <code>orig</code>, <code>sub</code> and <code>dst</code> can be of different
	 * lengths as long as <code>orig.length&le;sub.length&le;dst.length</code>. If
	 * <code>dst</code> or <code>sub</code> are longer, additional elements are
	 * ignored.
	 * 
	 * @param orig the first array
	 * @param sub  the second array to subtract from <code>orig</code>
	 * @param dst  the result array
	 * @return the modified array <code>dst=orig-sub</code>
	 * @throws ArrayIndexOutOfBoundsException if
	 *                                        <code>dst.length&lt;orig.length</code>
	 *                                        or
	 *                                        <code>sub.length&lt;orig.length</code>
	 */
	public static long[] sub(long[] orig, long[] sub, long[] dst) {
		int dim = orig.length;
		for (int n = 0; n < dim; n++)
			dst[n] = orig[n] - sub[n];
		return dst;
	}

	/**
	 * Non-destructive array subtraction. Subtract float array <code>sub</code> from
	 * array <code>orig</code> and place result in <code>dst</code> (arrays
	 * <code>orig</code> and <code>sub</code> remain unchanged). Arrays
	 * <code>orig</code>, <code>sub</code> and <code>dst</code> can be of different
	 * lengths as long as <code>orig.length&le;sub.length&le;dst.length</code>. If
	 * <code>dst</code> or <code>sub</code> are longer, additional elements are
	 * ignored.
	 * 
	 * @param orig the first array
	 * @param sub  the second array to subtract from <code>orig</code>
	 * @param dst  the result array
	 * @return the modified array <code>dst=orig-sub</code>
	 * @throws ArrayIndexOutOfBoundsException if
	 *                                        <code>dst.length&lt;orig.length</code>
	 *                                        or
	 *                                        <code>sub.length&lt;orig.length</code>
	 */
	public static float[] sub(float[] orig, float[] sub, float[] dst) {
		int dim = orig.length;
		for (int n = 0; n < dim; n++)
			dst[n] = orig[n] - sub[n];
		return dst;
	}

	/**
	 * Non-destructive array subtraction. Subtract double array <code>sub</code>
	 * from array <code>orig</code> and place result in <code>dst</code> (arrays
	 * <code>orig</code> and <code>sub</code> remain unchanged). Arrays
	 * <code>orig</code>, <code>sub</code> and <code>dst</code> can be of different
	 * lengths as long as <code>orig.length&le;sub.length&le;dst.length</code>. If
	 * <code>dst</code> or <code>sub</code> are longer, additional elements are
	 * ignored.
	 * 
	 * @param orig the first array
	 * @param sub  the second array to subtract from <code>orig</code>
	 * @param dst  the result array
	 * @return the modified array <code>dst=orig-sub</code>
	 * @throws ArrayIndexOutOfBoundsException if
	 *                                        <code>dst.length&lt;orig.length</code>
	 *                                        or
	 *                                        <code>sub.length&lt;orig.length</code>
	 */
	public static double[] sub(double[] orig, double[] sub, double[] dst) {
		int dim = orig.length;
		for (int n = 0; n < dim; n++)
			dst[n] = orig[n] - sub[n];
		return dst;
	}

	/**
	 * Squared distance of two integer arrays <code>a</code> and <code>b</code>
	 * given by the sum over <code>(a[i]-b[i])^2</code>. Arrays <code>a</code> and
	 * <code>b</code> are preserved.
	 *
	 * @param a the first array
	 * @param b the second array
	 * @return the distance between <code>a</code> and <code>b</code> squared
	 */
	public static int distSq(int[] a, int[] b) {
		int dim = a.length;
		int dist2 = 0;
		for (int n = 0; n < dim; n++) {
			int d = a[n] - b[n];
			dist2 += d * d;
		}
		return dist2;
	}

	/**
	 * Squared distance of two double arrays <code>a</code> and <code>b</code> given
	 * by the sum over <code>(a[i]-b[i])^2</code>. Arrays <code>a</code> and
	 * <code>b</code> are preserved.
	 *
	 * @param a the first array
	 * @param b the second array
	 * @return the distance between <code>a</code> and <code>b</code> squared
	 */
	public static double distSq(double[] a, double[] b) {
		int dim = a.length;
		double dist2 = 0.0;
		for (int n = 0; n < dim; n++) {
			double d = a[n] - b[n];
			dist2 += d * d;
		}
		return dist2;
	}

	/**
	 * Distance of two integer arrays <code>a</code> and <code>b</code> given by the
	 * square root of the sum over <code>(a[i]-b[i])^2</code>. Arrays <code>a</code>
	 * and <code>b</code> are preserved.
	 *
	 * @param a the first array
	 * @param b the second array
	 * @return the distance between <code>a</code> and <code>b</code>
	 */
	public static double dist(int[] a, int[] b) {
		return Math.sqrt(distSq(a, b));
	}

	/**
	 * Distance of two double arrays <code>a</code> and <code>b</code> given by the
	 * square root of the sum over <code>(a[i]-b[i])^2</code>. Arrays <code>a</code>
	 * and <code>b</code> are preserved.
	 *
	 * @param a the first array
	 * @param b the second array
	 * @return the distance between <code>a</code> and <code>b</code>
	 */
	public static double dist(double[] a, double[] b) {
		return Math.sqrt(distSq(a, b));
	}

	/**
	 * Dot product of two integer arrays <code>a</code> and <code>b</code> given by
	 * the sum over <code>a[i]*b[i]</code>. Arrays <code>a</code> and <code>b</code>
	 * are preserved.
	 * 
	 * @param a the first array
	 * @param b the second array
	 * @return the dot product <code>a.b</code>
	 */
	public static int dot(int[] a, int[] b) {
		int dim = a.length;
		int dot = 0;
		for (int n = 0; n < dim; n++)
			dot += a[n] * b[n];
		return dot;
	}

	/**
	 * Dot product of two long arrays <code>a</code> and <code>b</code> given by the
	 * sum over <code>a[i]*b[i]</code>. Arrays <code>a</code> and <code>b</code> are
	 * preserved.
	 * 
	 * @param a the first array
	 * @param b the second array
	 * @return the dot product <code>a.b</code>
	 */
	public static long dot(long[] a, long[] b) {
		int dim = a.length;
		long dot = 0;
		for (int n = 0; n < dim; n++)
			dot += a[n] * b[n];
		return dot;
	}

	/**
	 * Dot product of two float arrays <code>a</code> and <code>b</code> given by
	 * the sum over <code>a[i]*b[i]</code>. Arrays <code>a</code> and <code>b</code>
	 * are preserved.
	 * 
	 * @param a the first array
	 * @param b the second array
	 * @return the dot product <code>a.b</code>
	 */
	public static float dot(float[] a, float[] b) {
		int dim = a.length;
		float dot = 0;
		for (int n = 0; n < dim; n++)
			dot += a[n] * b[n];
		return dot;
	}

	/**
	 * Dot product of two double arrays <code>a</code> and <code>b</code> given by
	 * the sum over <code>a[i]*b[i]</code>. Arrays <code>a</code> and <code>b</code>
	 * are preserved.
	 * 
	 * @param a the first array
	 * @param b the second array
	 * @return the dot product <code>a.b</code>
	 */
	public static double dot(double[] a, double[] b) {
		int dim = a.length;
		double dot = 0;
		for (int n = 0; n < dim; n++)
			dot += a[n] * b[n];
		return dot;
	}

	/**
	 * Scalar multiplication of integer array <code>dst</code> by
	 * <code>scalar</code>.
	 * 
	 * @param dst    the array
	 * @param scalar the multiplier
	 * @return the modified array <code>dst</code>
	 */
	public static int[] multiply(int[] dst, int scalar) {
		int dim = dst.length;
		for (int n = 0; n < dim; n++)
			dst[n] *= scalar;
		return dst;
	}

	/**
	 * Scalar multiplication of long array <code>dst</code> by <code>scalar</code>.
	 * 
	 * @param dst    the array
	 * @param scalar the multiplier
	 * @return the modified array <code>dst</code>
	 */
	public static long[] multiply(long[] dst, long scalar) {
		int dim = dst.length;
		for (int n = 0; n < dim; n++)
			dst[n] *= scalar;
		return dst;
	}

	/**
	 * Scalar multiplication of float array <code>dst</code> by <code>scalar</code>.
	 * 
	 * @param dst    the array
	 * @param scalar the multiplier
	 * @return the modified array <code>dst</code>
	 */
	public static float[] multiply(float[] dst, float scalar) {
		int dim = dst.length;
		for (int n = 0; n < dim; n++)
			dst[n] *= scalar;
		return dst;
	}

	/**
	 * Scalar multiplication of double array <code>dst</code> by
	 * <code>scalar</code>.
	 * 
	 * @param dst    the array
	 * @param scalar the multiplier
	 * @return the modified array <code>dst</code>
	 */
	public static double[] multiply(double[] dst, double scalar) {
		int dim = dst.length;
		for (int n = 0; n < dim; n++)
			dst[n] *= scalar;
		return dst;
	}

	/**
	 * Scalar multiplication of integer array <code>a</code> by <code>scalar</code>
	 * with result in <code>dst</code>.
	 * 
	 * @param a      the array of <code>int</code>'s
	 * @param scalar the multiplier
	 * @param dst    the destination array
	 * @return the modified array <code>dst</code>
	 */
	public static int[] multiply(int[] a, int scalar, int[] dst) {
		int dim = a.length;
		for (int n = 0; n < dim; n++)
			dst[n] = a[n] * scalar;
		return dst;
	}

	/**
	 * Scalar multiplication of long array <code>a</code> by <code>scalar</code>
	 * with result in <code>dst</code>.
	 * 
	 * @param a      the array of <code>long</code>'s
	 * @param scalar the multiplier
	 * @param dst    the destination array
	 * @return the modified array <code>dst</code>
	 */
	public static long[] multiply(long[] a, long scalar, long[] dst) {
		int dim = a.length;
		for (int n = 0; n < dim; n++)
			dst[n] = a[n] * scalar;
		return dst;
	}

	/**
	 * Scalar multiplication of float array <code>a</code> by <code>scalar</code>
	 * with result in <code>dst</code>.
	 * 
	 * @param a      the array of <code>float</code>'s
	 * @param scalar the multiplier
	 * @param dst    the destination array
	 * @return the modified array <code>dst</code>
	 */
	public static float[] multiply(float[] a, float scalar, float[] dst) {
		int dim = a.length;
		for (int n = 0; n < dim; n++)
			dst[n] = a[n] * scalar;
		return dst;
	}

	/**
	 * Scalar multiplication of double array <code>a</code> by <code>scalar</code>
	 * with result in <code>dst</code>.
	 * 
	 * @param a      the array of <code>double</code>'s
	 * @param scalar the multiplier
	 * @param dst    the destination array
	 * @return the modified array <code>dst</code>
	 */
	public static double[] multiply(double[] a, double scalar, double[] dst) {
		int dim = a.length;
		for (int n = 0; n < dim; n++)
			dst[n] = a[n] * scalar;
		return dst;
	}

	/**
	 * Element-wise multiplication of integer arrays <code>a</code> and
	 * <code>dst</code>. The result is placed in <code>dst</code> (array
	 * <code>a</code> is preserved).
	 * 
	 * @param dst the first array
	 * @param a   the second array
	 * @return the modified array <code>dst</code>
	 */
	public static int[] multiply(int[] dst, int[] a) {
		int dim = dst.length;
		for (int n = 0; n < dim; n++)
			dst[n] *= a[n];
		return dst;
	}

	/**
	 * Element-wise multiplication of long arrays <code>a</code> and
	 * <code>dst</code>. The result is placed in <code>dst</code> (array
	 * <code>a</code> is preserved).
	 * 
	 * @param dst the first array
	 * @param a   the second array
	 * @return the modified array <code>dst</code>
	 */
	public static long[] multiply(long[] dst, long[] a) {
		int dim = dst.length;
		for (int n = 0; n < dim; n++)
			dst[n] *= a[n];
		return dst;
	}

	/**
	 * Element-wise multiplication of float arrays <code>a</code> and
	 * <code>dst</code>. The result is placed in <code>dst</code> (array
	 * <code>a</code> is preserved).
	 * 
	 * @param dst the first array
	 * @param a   the second array
	 * @return the modified array <code>dst</code>
	 */
	public static float[] multiply(float[] dst, float[] a) {
		int dim = dst.length;
		for (int n = 0; n < dim; n++)
			dst[n] *= a[n];
		return dst;
	}

	/**
	 * Element-wise multiplication of double array <code>a</code> and
	 * <code>dst</code>. The result is placed in <code>dst</code> (array
	 * <code>a</code> is preserved).
	 * 
	 * @param dst the first array
	 * @param a   the second array
	 * @return the modified array <code>dst</code>
	 */
	public static double[] multiply(double[] dst, double[] a) {
		int dim = dst.length;
		for (int n = 0; n < dim; n++)
			dst[n] *= a[n];
		return dst;
	}

	/**
	 * Non-destructive, element-wise multiplication of integer array <code>a</code>
	 * and <code>b</code>. The result is placed in <code>dst</code> (array
	 * <code>a</code> and <code>b</code> are preserved).
	 * 
	 * @param a   the first array
	 * @param b   the second array
	 * @param dst the result array
	 * @return the modified array <code>dst[i]=a[i]*b[i]</code>
	 */
	public static int[] multiply(int[] a, int[] b, int[] dst) {
		int dim = a.length;
		for (int n = 0; n < dim; n++)
			dst[n] = a[n] * b[n];
		return a;
	}

	/**
	 * Non-destructive, element-wise multiplication of long arrays <code>a</code>
	 * and <code>b</code>. The result is placed in <code>dst</code> (arrays
	 * <code>a</code> and <code>b</code> are preserved).
	 * 
	 * @param a   the first array
	 * @param b   the second array
	 * @param dst the result array
	 * @return the modified array <code>dst[i]=a[i]*b[i]</code>
	 */
	public static long[] multiply(long[] a, long[] b, long[] dst) {
		int dim = a.length;
		for (int n = 0; n < dim; n++)
			dst[n] = a[n] * b[n];
		return a;
	}

	/**
	 * Non-destructive, element-wise multiplication of float arrays <code>a</code>
	 * and <code>b</code>. The result is placed in <code>dst</code> (arrays
	 * <code>a</code> and <code>b</code> are preserved).
	 * 
	 * @param a   the first array
	 * @param b   the second array
	 * @param dst the result array
	 * @return the modified array <code>dst[i]=a[i]*b[i]</code>
	 */
	public static float[] multiply(float[] a, float[] b, float[] dst) {
		int dim = a.length;
		for (int n = 0; n < dim; n++)
			dst[n] = a[n] * b[n];
		return a;
	}

	/**
	 * Non-destructive, element-wise multiplication of double arrays <code>a</code>
	 * and <code>b</code>. The result is placed in <code>dst</code> (arrays
	 * <code>a</code> and <code>b</code> are preserved).
	 * 
	 * @param a   the first array
	 * @param b   the second array
	 * @param dst the result array
	 * @return the modified array <code>dst[i]=a[i]*b[i]</code>
	 */
	public static double[] multiply(double[] a, double[] b, double[] dst) {
		int dim = a.length;
		for (int n = 0; n < dim; n++)
			dst[n] = a[n] * b[n];
		return a;
	}

	/**
	 * Non-destructive integer matrix multiplication. Matrix <code>mat</code>
	 * multiplied by array <code>vec</code> and the result stored in
	 * <code>dst</code> (right multiplication). Matrix <code>mat</code> and array
	 * <code>vec</code> preserved.
	 * 
	 * @param mat the matrix
	 * @param vec the array
	 * @param dst the result array
	 * @return the modified array <code>dst=mat * vec</code>
	 */
	public static int[] multiply(int[][] mat, int[] vec, int[] dst) {
		int n = mat.length;
		for (int i = 0; i < n; i++) {
			dst[i] = 0;
			int[] row = mat[i];
			for (int j = 0; j < n; j++) {
				dst[i] += row[j] * vec[j];
			}
		}
		return dst;
	}

	/**
	 * Non-destructive long matrix multiplication. Matrix <code>mat</code>
	 * multiplied by array <code>vec</code> and the result stored in
	 * <code>dst</code> (right multiplication). Matrix <code>mat</code> and array
	 * <code>vec</code> preserved.
	 * 
	 * @param mat the matrix
	 * @param vec the array
	 * @param dst the result array
	 * @return the modified array <code>dst=mat * vec</code>
	 */
	public static long[] multiply(long[][] mat, long[] vec, long[] dst) {
		int n = mat.length;
		for (int i = 0; i < n; i++) {
			dst[i] = 0L;
			long[] row = mat[i];
			for (int j = 0; j < n; j++) {
				dst[i] += row[j] * vec[j];
			}
		}
		return dst;
	}

	/**
	 * Non-destructive float matrix multiplication. Matrix <code>mat</code>
	 * multiplied by array <code>vec</code> and the result stored in
	 * <code>dst</code> (right multiplication). Matrix <code>mat</code> and array
	 * <code>vec</code> preserved.
	 * 
	 * @param mat the matrix
	 * @param vec the array
	 * @param dst the result array
	 * @return the modified array <code>dst=mat * vec</code>
	 */
	public static float[] multiply(float[][] mat, float[] vec, float[] dst) {
		int n = mat.length;
		for (int i = 0; i < n; i++) {
			dst[i] = 0f;
			float[] row = mat[i];
			for (int j = 0; j < n; j++) {
				dst[i] += row[j] * vec[j];
			}
		}
		return dst;
	}

	/**
	 * Non-destructive double matrix multiplication. Matrix <code>mat</code>
	 * multiplied by array <code>vec</code> and the result stored in
	 * <code>dst</code> (right multiplication). Matrix <code>mat</code> and array
	 * <code>vec</code> preserved.
	 * 
	 * @param mat the matrix
	 * @param vec the array
	 * @param dst the result array
	 * @return the modified array <code>dst=mat * vec</code>
	 */
	public static double[] multiply(double[][] mat, double[] vec, double[] dst) {
		int n = mat.length;
		for (int i = 0; i < n; i++) {
			dst[i] = 0.0;
			double[] row = mat[i];
			for (int j = 0; j < n; j++) {
				dst[i] += row[j] * vec[j];
			}
		}
		return dst;
	}

	/**
	 * Element-wise absolute value of int array <code>a</code>.
	 * 
	 * @param a the array to apply absolute value to
	 * @return the modified array <code>a</code>
	 */
	public static int[] abs(int[] a) {
		int dim = a.length;
		for (int n = 0; n < dim; n++)
			a[n] = Math.abs(a[n]);
		return a;
	}

	/**
	 * Element-wise absolute value of float array <code>a</code>.
	 * 
	 * @param a the array to apply absolute value to
	 * @return the modified array <code>a</code>
	 */
	public static float[] abs(float[] a) {
		int dim = a.length;
		for (int n = 0; n < dim; n++)
			a[n] = Math.abs(a[n]);
		return a;
	}

	/**
	 * Element-wise absolute value of double array <code>a</code>.
	 * 
	 * @param a the array to apply absolute value to
	 * @return the modified array <code>a</code>
	 */
	public static double[] abs(double[] a) {
		int dim = a.length;
		for (int n = 0; n < dim; n++)
			a[n] = Math.abs(a[n]);
		return a;
	}

	/**
	 * Element-wise logarithm of float array <code>a</code> (base 10).
	 * 
	 * @param a the array to apply logarithm to
	 * @return the modified array <code>a</code>
	 */
	public static float[] log10(float[] a) {
		int dim = a.length;
		for (int n = 0; n < dim; n++)
			a[n] = (float) Math.log10(a[n]);
		return a;
	}

	/**
	 * Element-wise logarithm of double array <code>a</code> (base 10).
	 * 
	 * @param a the array to apply logarithm to
	 * @return the modified array <code>a</code>
	 */
	public static double[] log10(double[] a) {
		int dim = a.length;
		for (int n = 0; n < dim; n++)
			a[n] = Math.log10(a[n]);
		return a;
	}

	/**
	 * Element-wise square-root of float array <code>a</code>.
	 * 
	 * @param a the array to apply logarithm to
	 * @return the modified array <code>a</code>
	 */
	public static float[] sqrt(float[] a) {
		int dim = a.length;
		for (int n = 0; n < dim; n++)
			a[n] = (float) Math.sqrt(a[n]);
		return a;
	}

	/**
	 * Element-wise square-root of double array <code>a</code>.
	 * 
	 * @param a the array to apply logarithm to
	 * @return the modified array <code>a</code>
	 */
	public static double[] sqrt(double[] a) {
		int dim = a.length;
		for (int n = 0; n < dim; n++)
			a[n] = Math.sqrt(a[n]);
		return a;
	}

	/**
	 * GWT has an aversion to clone()ing - provide alternatives.
	 * 
	 * @param orig the boolean array to clone.
	 * @return the clone of boolean array <code>orig</code>.
	 */
	public static boolean[] clone(boolean[] orig) {
		int len = orig.length;
		boolean[] clone = new boolean[len];
		System.arraycopy(orig, 0, clone, 0, len);
		return clone;
	}

	/**
	 * GWT has an aversion to clone()ing - provide alternatives.
	 * 
	 * @param orig the int array to clone.
	 * @return the clone of int array <code>orig</code>.
	 */
	public static int[] clone(int[] orig) {
		int len = orig.length;
		int[] clone = new int[len];
		System.arraycopy(orig, 0, clone, 0, len);
		return clone;
	}

	/**
	 * GWT has an aversion to clone()ing - provide alternatives.
	 * 
	 * @param orig the double array to clone.
	 * @return the clone of double array <code>orig</code>.
	 */
	public static double[] clone(double[] orig) {
		int len = orig.length;
		double[] clone = new double[len];
		System.arraycopy(orig, 0, clone, 0, len);
		return clone;
	}

	/**
	 * GWT has an aversion to clone()ing - provide alternatives.
	 * 
	 * @param orig the two dimensional array (matrix) to clone.
	 * @return the clone of matrix <code>orig</code>.
	 */
	public static double[][] clone(double[][] orig) {
		int len = orig.length;
		double[][] clone = new double[len][];
		for (int i = 0; i < len; i++)
			clone[i] = clone(orig[i]);
		return clone;
	}
}
