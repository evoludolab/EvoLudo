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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import org.evoludo.math.Combinatorics;

/**
 * Utility class to read and write plist-files with some customizations to store
 * the bit-patterns for doubles to allow for perfect reproducibility.
 * 
 * @see #encodeKey(String, double)
 * @see PlistParser#parse(String)
 * 
 * @author Christoph Hauert
 */
public class Plist extends HashMap<String, Object> {

	/**
	 * Required for serializable classes.
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * The list of keys to skip when comparing two plists.
	 */
	Collection<String> skip;

	/**
	 * The flag to indicate if the comparison should fail fast, i.e. after first
	 * issue encountered.
	 */
	boolean failFast = false;

	/**
	 * The number of repeated messages to report before skipping further messages.
	 */
	static final int N_REPEAT = 3;

	/**
	 * Construct a new plist.
	 */
	public Plist() {
		nRepeat = N_REPEAT;
		repeat = 0;
	}

	/**
	 * Set verbose mode to report all differences.
	 */
	public void verbose() {
		nRepeat = Integer.MAX_VALUE;
	}

	/**
	 * Set quiet mode to suppress all differences.
	 */
	public void quiet() {
		nRepeat = 0;
	}

	/**
	 * Check if in fail-fast mode.
	 * 
	 * @return <code>true</code> if fail-fast mode is set
	 */
	public boolean failfast() {
		return failFast;
	}

	/**
	 * Set the fail-fast mode to stop comparisons after the first issue encountered.
	 * 
	 * @param failfast the fail-fast-flag
	 */
	public void failfast(boolean failfast) {
		failFast = failfast;
		nRepeat = failFast ? 0 : N_REPEAT;
	}

	/**
	 * Get the number of issues found.
	 * 
	 * @return the number of issues
	 */
	public int getNIssues() {
		return nIssues;
	}

	/**
	 * Get the number of major issues found.
	 * 
	 * @return the number of issues
	 */
	public int getNMajor() {
		return nIssues - nNumerical;
	}

	/**
	 * Get the number of minor issues found (most likely numerical).
	 * 
	 * @return the number of issues
	 */
	public int getNMinor() {
		return nNumerical;
	}

	public String sha256(Collection<String> exclude) {
		HashMap<String, Object> original = new HashMap<>(this);
		for (String key : exclude) {
			if (containsKey(key)) {
				original.put(key, get(key));
				replace(key, null);
			}
		}
		String sha = "";
		try {
			sha = hashSHA256(this);
		} catch (IOException | NoSuchAlgorithmException e) {
			e.printStackTrace();
		} finally {
			// restore original state
			this.putAll(original);
		}
		return sha;
	}

	public static String hashSHA256(Object obj) throws IOException, NoSuchAlgorithmException {
		try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
				ObjectOutputStream oos = new ObjectOutputStream(bos)) {
			oos.writeObject(obj);
			oos.flush();
			byte[] objectBytes = bos.toByteArray();

			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] shaHash = digest.digest(objectBytes);
			StringBuilder sb = new StringBuilder();
			for (byte b : shaHash) {
				sb.append(String.format("%02x", b));
			}
			return sb.toString();
		}
	}

	/**
	 * Compare this plist to {@code plist}.
	 * 
	 * @param plist the plist to compare against
	 * @return the number of differences
	 */
	public int diff(Plist plist) {
		return diff(plist, new ArrayList<String>());
	}

	/**
	 * Compare this plist to {@code plist} but ignore keys in {@code clo}.
	 * 
	 * @param plist the reference {@code Plist} to compare against
	 * @param clo   the collection of keys to skip
	 * @return the number of differences
	 */
	public int diff(Plist plist, Collection<String> clo) {
		skip = clo;
		nNumerical = 0;
		nIssues = 0;
		diffDict(plist, this);
		if (nNumerical > 0)
			reportDiff(nNumerical + " out of " + nIssues + " differences likely numerical rounding issues.");
		return nIssues;
	}

	/**
	 * Helper method to compare two plist-dictionaries.
	 * 
	 * @param reference the reference {@code Plist}
	 * @param plist     the {@code Plist} to check
	 */
	private void diffDict(Plist reference, Plist plist) {
		// step 1: check if dict reference contains all keys of plist
		for (String key : plist.keySet()) {
			if (skip.contains(key) || reference.containsKey(key))
				continue;
			processDiff("key '" + key + "' missing in reference.");
			if (failFast)
				return;
		}
		// step 2: check if dict plist contains all keys of reference
		for (String key : reference.keySet()) {
			if (skip.contains(key))
				continue;
			if (!plist.containsKey(key)) {
				if (reference == this)
					continue;
				processDiff("key '" + key + "' missing in plist.");
				if (failFast)
					return;
				continue;
			}
			// step 3: compare entries this
			Object val = reference.get(key);
			Object pval = plist.get(key);
			if (val.getClass() != pval.getClass()) {
				processDiff("key '" + key + "' values class differs\n(me: " + pval.getClass()
						+ ", ref: " + val.getClass() + ")");
				if (failFast)
					return;
				continue;
			}
			if (val instanceof Plist) {
				int before = nIssues;
				// entry is dict
				diffDict((Plist) val, (Plist) pval);
				if (before == nIssues)
					continue;
				reportDiff("key '" + key + "' dicts differ.");
				if (failFast)
					return;
				continue;
			}
			if (val instanceof List) {
				int before = nIssues;
				// entry is array
				@SuppressWarnings("unchecked")
				List<Object> lval = (List<Object>) val;
				@SuppressWarnings("unchecked")
				List<Object> lpval = (List<Object>) pval;
				diffArray(lval, lpval);
				if (before == nIssues)
					continue;
				reportDiff("key '" + key + "' arrays differ.");
				if (failFast)
					return;
				continue;
			}
			if (val instanceof String) {
				if (((String) val).equals(pval))
					continue;
				processDiff("key '" + key + "' strings differ\n(me: " + pval + ", ref: " + val + ")");
				if (failFast)
					return;
				continue;
			}
			if (val instanceof Integer) {
				if (((Integer) val).equals(pval))
					continue;
				processDiff("key '" + key + "' integers differ\n(me: " + pval + ", ref: " + val + ")");
				if (failFast)
					return;
				continue;
			}
			if (val instanceof Double) {
				if (((Double) val).equals(pval))
					continue;
				checkRounding((Double) val, (Double) pval);
				processDiff("key '" + key + "' reals differ\n(me: " + pval + ", ref: " + val + ", Δ: "
						+ ((Double) pval - (Double) val) + ")");
				if (failFast)
					return;
				continue;
			}
			if (val instanceof Boolean) {
				if (((Boolean) val).equals(pval))
					continue;
				processDiff("key '" + key + "' booleans differ\n(me: " + pval + ", ref: " + val + ")");
				if (failFast)
					return;
				continue;
			}
			processDiff("key '" + key + "' unknown value type (class: " + val.getClass() + ")");
			if (failFast)
				return;
		}
	}

	/**
	 * Helper method to compare two plist-arrays.
	 * 
	 * @param reference the reference array
	 * @param array     the array to check
	 */
	private void diffArray(List<Object> reference, List<Object> array) {
		if (reference.size() != array.size()) {
			processDiff(
					"arrays differ in size\n(me: " + array.size() + ", ref: " + reference.size() + ")");
			if (failFast)
				return;
			return;
		}
		int i = -1;
		for (Object ele : reference) {
			Object pele = array.get(++i);
			if (ele.getClass() != pele.getClass()) {
				processDiff(
						"array classes differ\n(me: " + pele.getClass() + ", ref: " + ele.getClass() + ")");
				if (failFast)
					return;
				continue;
			}
			if (ele instanceof Plist) {
				// entry is dict
				diffDict((Plist) ele, (Plist) pele);
				continue;
			}
			if (ele instanceof List) {
				// entry is array
				@SuppressWarnings("unchecked")
				List<Object> lele = (List<Object>) ele;
				@SuppressWarnings("unchecked")
				List<Object> lpele = (List<Object>) pele;
				diffArray(lele, lpele);
				continue;
			}
			if (ele instanceof String) {
				if (((String) ele).equals(pele))
					continue;
				processDiff(
						"array string[" + i + "] differs\n(me: " + pele + ", ref: " + ele + ")");
				if (failFast)
					return;
				continue;
			}
			if (ele instanceof Integer) {
				if (((Integer) ele).equals(pele))
					continue;
				processDiff(
						"array integer[" + i + "] differs\n(me: " + pele + ", ref: " + ele + ")");
				if (failFast)
					return;
				continue;
			}
			if (ele instanceof Double) {
				if (((Double) ele).equals(pele))
					continue;
				checkRounding((Double) ele, (Double) pele);
				processDiff("array real[" + i + "] differs\n(me: " + pele + ", ref: " + ele + ", Δ: "
						+ ((Double) pele - (Double) ele) + ")");
				if (failFast)
					return;
				continue;
			}
			if (ele instanceof Boolean) {
				if (((Boolean) ele).equals(pele))
					continue;
				processDiff(
						"array boolean[" + i + "] differs\n(me: " + pele + ", ref: " + ele + ")");
				if (failFast)
					return;
				continue;
			}
			processDiff("array of unknown type (class: " + ele.getClass() + ")");
			if (failFast)
				return;
		}
	}

	/**
	 * The number of significant digits. This is used to distinguish between minor
	 * numerical issues and major differences.
	 */
	private static final int PRECISION_DIGITS = 12;

	/**
	 * Check if the difference between <code>ref</code> and <code>check</code> is
	 * due to rounding errors.
	 * 
	 * @param ref   the reference value
	 * @param check the value to check
	 * 
	 * @see #PRECISION_DIGITS
	 */
	private void checkRounding(Double ref, Double check) {
		// tolerate if only last 3 digits differ
		checkRounding(ref, check, PRECISION_DIGITS);
	}

	/**
	 * Check if the difference between <code>ref</code> and <code>check</code> is
	 * due to rounding errors.
	 * 
	 * @param ref    the reference value
	 * @param check  the value to check
	 * @param digits the number of significant digits
	 */
	private void checkRounding(Double ref, Double check, Integer digits) {
		int order = (int) Math.floor(Math.log10(1.0 + ref));
		double scale = Combinatorics.pow(10.0, digits - order);
		if ((int) Math.abs(Math.floor(ref * scale) - Math.floor(check * scale)) <= 1)
			nNumerical++;
	}

	/**
	 * The number of issues found.
	 */
	private int nIssues;

	/**
	 * The number of numerical issues found.
	 */
	private int nNumerical;

	/**
	 * The number of times a particular type of issue is reported before skipping
	 * any further ones.
	 */
	private int nRepeat;

	/**
	 * The previous message.
	 */
	private String prevmsg;

	/**
	 * The number of times the previous message was repeated.
	 */
	private int repeat;

	/**
	 * Process a difference.
	 * 
	 * @param msg the message to report
	 */
	private void processDiff(String msg) {
		nIssues++;
		reportDiff(msg);
	}

	/**
	 * Report a difference. Nothing is reported if the message is a repetition of
	 * the previous message and {@code nRepeat} has been reached. If messages have
	 * been skipped a summary is reported before continuing with the next issue.
	 * 
	 * @param msg the message to report
	 */
	private void reportDiff(String msg) {
		if (prevmsg == null || !msg.startsWith(prevmsg)) {
			// arrays are most likely candidates for excessive repeated messages
			// 'array real[' is the shortest header - use first 11 characters
			prevmsg = msg.substring(0, 11);
			if (repeat > nRepeat)
				reportDiff("\nThe previous message was reported a total of " + repeat + " times.");
			repeat = 1;
		} else {
			if (repeat++ > nRepeat)
				return;
		}
		if (nRepeat > 0)
			System.err.println((new Date().toString()) + " - Plist.diff: " + msg);
	}

	/**
	 * Utility method to encode <code>boolean</code> with tag <code>key</code>.
	 * 
	 * @param key  tag name
	 * @param bool <code>boolean</code> value
	 * @return encoded String
	 */
	public static String encodeKey(String key, boolean bool) {
		return "<key>" + key + "</key>\n<" + (bool ? "true" : "false") + "/>\n";
	}

	/**
	 * Utility method to encode <code>int</code> with tag <code>key</code>.
	 * 
	 * @param key     tag name
	 * @param integer <code>int</code> value
	 * @return encoded String
	 */
	public static String encodeKey(String key, int integer) {
		return "<key>" + key + "</key>\n<integer>" + integer + "</integer>\n";
	}

	/**
	 * Utility method to encode <code>double</code> with tag <code>key</code>.
	 * <p>
	 * <strong>Notes:</strong>
	 * <ul>
	 * <li>floating point values are saved as {@code long}'s (bit strings) to avoid
	 * rounding errors when saving/restoring the state of the model.
	 * <li>cannot use {@code Double.toHexString(real)} because GWT does not
	 * implement it.
	 * </ul>
	 * 
	 * @param key  tag name
	 * @param real <code>double</code> value
	 * @return encoded String
	 */
	public static String encodeKey(String key, double real) {
		// return "<key>" + key + "</key>\n<real>" + Double.toHexString(real) +
		// "</real>\n";
		return "<key>" + key + "</key>\n<real>" + Long.toString(Double.doubleToLongBits(real)) + "L</real>\n";
	}

	/**
	 * Utility method to encode <code>String</code> with tag <code>key</code>.
	 * 
	 * @param key    tag name
	 * @param string <code>String</code> value
	 * @return encoded String
	 */
	public static String encodeKey(String key, String string) {
		return "<key>" + key + "</key>\n<string>" + XMLCoder.encode(string) + "</string>\n";
	}

	/**
	 * Utility method to encode <code>int</code> array with tag <code>key</code>.
	 * 
	 * @param key   tag name
	 * @param array <code>int[]</code> value
	 * @return encoded String
	 */
	public static String encodeKey(String key, int[] array) {
		return "<key>" + key + "</key>\n" + encodeArray(array);
	}

	/**
	 * Utility method to encode first <code>len</code> entries of <code>int</code>
	 * array with tag <code>key</code>.
	 * 
	 * @param key   tag name
	 * @param array <code>int[]</code> value
	 * @param len   number elements to encode
	 * @return encoded String
	 */
	public static String encodeKey(String key, int[] array, int len) {
		return "<key>" + key + "</key>\n" + encodeArray(array, len);
	}

	/**
	 * Utility method to encode <code>double</code> array with tag <code>key</code>.
	 * <p>
	 * <strong>Note:</strong> floating point values are saved as bit strings to
	 * avoid rounding errors when saving/restoring the state of the model.
	 * 
	 * @param key   tag name
	 * @param array <code>double[]</code> value
	 * @return encoded String
	 */
	public static String encodeKey(String key, double[] array) {
		return "<key>" + key + "</key>\n" + encodeArray(array);
	}

	/**
	 * Utility method to encode first <code>len</code> entries of
	 * <code>double</code> array with tag <code>key</code>.
	 * <p>
	 * <strong>Note:</strong> floating point values are saved as bit strings to
	 * avoid rounding errors when saving/restoring the state of the model.
	 * 
	 * @param key   tag name
	 * @param array <code>double[]</code> value
	 * @param len   number elements to encode
	 * @return encoded String
	 */
	public static String encodeKey(String key, double[] array, int len) {
		return "<key>" + key + "</key>\n" + encodeArray(array, len);
	}

	/**
	 * Utility method to encode <code>double</code> matrix with tag
	 * <code>key</code>.
	 * <p>
	 * <strong>Note:</strong> floating point values are saved as bit strings to
	 * avoid rounding errors when saving/restoring the state of the model.
	 * 
	 * @param key    tag name
	 * @param matrix <code>double[][]</code> value
	 * @return encoded String
	 */
	public static String encodeKey(String key, double[][] matrix) {
		return "<key>" + key + "</key>\n" + encodeArray(matrix);
	}

	/**
	 * Utility method to encode <code>String</code> array with tag <code>key</code>.
	 * 
	 * @param key   tag name
	 * @param array <code>String[]</code> value
	 * @return encoded String
	 */
	public static String encodeKey(String key, String[] array) {
		return "<key>" + key + "</key>\n" + encodeArray(array);
	}

	/**
	 * Utility method to encode first <code>len</code> entries of
	 * <code>String</code> array with tag <code>key</code>.
	 * 
	 * @param key   tag name
	 * @param array <code>String[]</code> value
	 * @param len   number elements to encode
	 * @return encoded String
	 */
	public static String encodeKey(String key, String[] array, int len) {
		return "<key>" + key + "</key>\n" + encodeArray(array, len);
	}

	/**
	 * Helper method to encode <code>int</code> array
	 * 
	 * @param array <code>int[]</code> value
	 * @return encoded String
	 */
	private static String encodeArray(int[] array) {
		StringBuilder plist = new StringBuilder("<array>\n");
		for (int a : array)
			plist.append("<integer>" + a + "</integer>\n");
		return plist.append("</array>\n").toString();
	}

	/**
	 * Helper method to encode first <code>len</code> elements of <code>int</code>
	 * array
	 * 
	 * @param array <code>int[]</code> value
	 * @param len   number elements to encode
	 * @return encoded String
	 */
	private static String encodeArray(int[] array, int len) {
		StringBuilder plist = new StringBuilder("<array>\n");
		for (int n = 0; n < len; n++)
			plist.append("<integer>" + array[n] + "</integer>\n");
		return plist.append("</array>\n").toString();
	}

	/**
	 * Helper method to encode <code>double</code> array
	 * <p>
	 * <strong>Note:</strong> floating point values are saved as bit strings to
	 * avoid rounding errors when saving/restoring the state of the model.
	 * 
	 * @param array <code>double[]</code> value
	 * @return encoded String
	 */
	private static String encodeArray(double[] array) {
		StringBuilder plist = new StringBuilder("<array>\n");
		for (double a : array)
			plist.append("<real>" + Long.toString(Double.doubleToLongBits(a)) + "L</real>\n");
		return plist.append("</array>\n").toString();
	}

	/**
	 * Helper method to encode first <code>len</code> elements of
	 * <code>double</code> array
	 * <p>
	 * <strong>Note:</strong> floating point values are saved as bit strings to
	 * avoid rounding errors when saving/restoring the state of the model.
	 * 
	 * @param array <code>double[]</code> value
	 * @param len   number elements to encode
	 * @return encoded String
	 */
	private static String encodeArray(double[] array, int len) {
		StringBuilder plist = new StringBuilder("<array>\n");
		for (int n = 0; n < len; n++)
			plist.append("<real>" + Long.toString(Double.doubleToLongBits(array[n])) + "L</real>\n");
		return plist.append("</array>\n").toString();
	}

	/**
	 * Helper method to encode <code>double</code> matrix
	 * 
	 * @param array <code>double[][]</code> value
	 * @return encoded String
	 */
	private static String encodeArray(double[][] array) {
		StringBuilder plist = new StringBuilder("<array>\n");
		for (double[] a : array)
			plist.append(encodeArray(a));
		return plist.append("</array>\n").toString();
	}

	/**
	 * Helper method to encode <code>String</code> array
	 * 
	 * @param array <code>String[]</code> value
	 * @return encoded String
	 */
	private static String encodeArray(String[] array) {
		StringBuilder plist = new StringBuilder("<array>\n");
		for (String a : array)
			plist.append("<string>" + XMLCoder.encode(a) + "</string>\n");
		return plist.append("</array>\n").toString();
	}

	/**
	 * Helper method to encode first <code>len</code> elements of
	 * <code>String</code> array
	 * 
	 * @param array <code>String[]</code> value
	 * @param len   number elements to encode
	 * @return encoded String
	 */
	private static String encodeArray(String[] array, int len) {
		StringBuilder plist = new StringBuilder("<array>\n");
		for (int n = 0; n < len; n++)
			plist.append("<string>" + XMLCoder.encode(array[n]) + "</string>\n");
		return plist.append("</array>\n").toString();
	}

	/**
	 * Utility method to convert a list of <code>Integer</code>'s to an array of
	 * <code>int</code>'s.
	 * 
	 * @param list {@code List<Integer>} value
	 * @return <code>int[]</code> array
	 */
	public static int[] list2int(List<Integer> list) {
		// int[] array = new int[list.size()];
		// int idx = 0;
		// for (Integer i : list)
		// array[idx++] = i;
		// return array;
		return list.stream().mapToInt(Integer::intValue).toArray();
	}

	/**
	 * Utility method to convert a list of <code>Double</code>'s to an array of
	 * <code>double</code>'s.
	 * 
	 * @param list {@code List<Double>} value
	 * @return <code>double[]</code> array
	 */
	public static double[] list2double(List<Double> list) {
		// double[] array = new double[list.size()];
		// int idx = 0;
		// for (Double d : list)
		// array[idx++] = d;
		// return array;
		return list.stream().mapToDouble(Double::doubleValue).toArray();
	}
}
