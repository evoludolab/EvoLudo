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

package org.evoludo.util;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Basic parser for <code>plist</code>-files.
 * <p>
 * The primary purpose and motivation for <code>PlistParser</code> is to allow
 * <code>EvoLudo</code> to save and restore the exact states of numerical or
 * individual based models. The faithful storage of all information (including
 * the state of the random number generator) allows to resume calculations
 * without any information loss. For example, the state of some calculations is
 * saved after some time. Then the restored calculations produce time series
 * that are identical to the ones produced when continuing the original
 * calculation. Naturally, this strong criterion no longer holds after any
 * modifications of numerical schemes or the use of random numbers.
 * 
 * @author Christoph Hauert
 */
public class PlistParser {

	/**
	 * Suppresses default constructor, ensuring non-instantiability.
	 */
	private PlistParser() {
	}

// NOTE: reading an actual plist-file is not compatible with GWT. A subclass
// PlistParserJRE could provide parsers for a File object
//
//	/**
//	 *
//	 * @param xmlfile
//	 * @return
//	 */
//	public static Map<String,Object> parse(File xmlfile) {
//		Map<String,Object> plist = new HashMap<String,Object>();
//		try {
//			PlistReader reader = new PlistReader(new BufferedReader(new InputStreamReader(new FileInputStream(xmlfile), "UTF-8")));
//			while( reader.hasNext() ) {
//				PlistTag tag = reader.next();
//				if( tag.equals("plist") ) {
//					// check version of plist specifications? - see attributes of reader
//					continue;
//				}
//				if( tag.equals("dict") ) {
//					parseDict(reader, plist);
//					continue;
//				}
//				if( tag.equals("/plist") ) break;
//				// all other tags should not be encountered when parsing <plist>
//				System.err.println((new Date().toString())+" - PlistParser, line "+reader.getLine()+": unknown tag '"+tag.getTag()+"' - ignored.");
//			}
//			reader.close();
//		}
//		catch( UnsupportedEncodingException e ) {
//			System.err.println((new Date().toString())+" - PlistParser: UTF-8 encoding unsupported. "+e.getMessage());
//			e.printStackTrace(System.err);
//			return null;
//		}
//		catch( FileNotFoundException e ) {
//			System.err.println((new Date().toString())+" - PlistParser: file "+xmlfile.getName()+" not found. "+e.getMessage());
//			e.printStackTrace(System.err);
//			return null;
//		}
//		return plist;
//	}

	/**
	 * Parse the contents of <code>plist</code>-file supplied as a String and return
	 * a Map with key and object associations. The parses processes the following
	 * <code>plist</code> elements:
	 * <dl>
	 * <dt>&lt;key&gt;</dt>
	 * <dd>Name of tag: any valid String.</dd>
	 * <dt>&lt;dict&gt;</dt>
	 * <dd>Dictionary: Alternating <code>&lt;key&gt;</code> tags and
	 * <code>plist</code> elements (excluding <code>&lt;key&gt;</code>). Can be
	 * empty.</dd>
	 * <dt>&lt;array&gt;</dt>
	 * <dd>Array: Can contain any number of identical child <code>plist</code>
	 * elements (excluding <code>&lt;key&gt;</code>). Can be empty.</dd>
	 * <dt>&lt;string&gt;</dt>
	 * <dd>UTF-8 encoded string.</dd>
	 * <dt>&lt;real&gt;</dt>
	 * <dd>Floating point number: any string that {@link Double#parseDouble(String)}
	 * can process (includes infinity and NaN). <br>
	 * <strong>Important:</strong> Strings starting with <code>0x</code> indicate
	 * that floating point number is encoded as hexadecimal. Decoding happens in two
	 * stages: first, the hexadecimal is processed as a <code>long</code> using
	 * {@link Long#parseLong(String, int)} and then converted to <code>double</code>
	 * with {@link Double#longBitsToDouble(long)}. Hexadecimal encoding of floating
	 * point numbers is not part of the <code>plist</code> specification. However,
	 * only bitwise encoding can guarantee faithful writing and restoring of
	 * floating point numbers.</dd>
	 * <dt>&lt;integer&gt;</dt>
	 * <dd>Integer number: any string that {@link Integer#parseInt(String)} can
	 * process, i.e. limited to 32bits.</dd>
	 * <dt>&lt;true/&gt;, &lt;false/&gt;</dt>
	 * <dd>Boolean numbers: tag represents the boolean values <code>true</code> and
	 * <code>false</code>.</dd>
	 * </dl>
	 * <p>
	 * Not implemented are currently:
	 * </p>
	 * <dl>
	 * <dt>&lt;data&gt;</dt>
	 * <dd>Base64 encoded data.</dd>
	 * <dt>&lt;date&gt;</dt>
	 * <dd>ISO 8601 formatted string.</dd>
	 * </dl>
	 * <p>
	 * <em>Note:</em> Invalid or unknown tags trigger error message on standard out.
	 * </p>
	 * 
	 * @param string contents of <code>plist</code>-file
	 * @return map with key and element associations
	 */
	public static Plist parse(String string) {
		Plist plist = new Plist();
		PlistReader reader = new PlistReader(string);
		while (reader.hasNext()) {
			PlistTag tag = reader.next();
			if (tag.equals("plist")) {
				// check version of plist specifications? - see attributes of reader
				continue;
			}
			if (tag.equals("dict")) {
				parseDict(reader, plist);
				continue;
			}
			if (tag.equals("/plist"))
				break;
			// all other tags should not be encountered when parsing <plist>
			System.err.println((new Date().toString()) + " - PlistParser, line " + reader.getLine() + ": unknown tag '"
					+ tag.getTag() + "' - ignored.");
		}
//		reader.close();
		if (plist.isEmpty())
			return null;
		return plist;
	}

	/**
	 * Parses a dictionary entry, <code>&lt;dict&gt;</code>, in the
	 * <code>plist</code>-string provided by <code>reader</code> and writes
	 * <code>&lt;key&gt;</code> and <code>plist</code> element pairs to the lookup
	 * table <code>dict</code>. Note, dictionaries may contain
	 * <code>&lt;dict&gt;</code> elements, which results in recursive calls to this
	 * method.
	 * <p>
	 * <em>Note:</em> Invalid or unknown tags trigger error message on standard out.
	 * </p>
	 * 
	 * @param reader iterator over <code>plist</code> tags
	 * @param dict   map for storing all pairs of <code>&lt;key&gt;</code> and
	 *               <code>plist</code> element pairs.
	 */
	protected static void parseDict(PlistReader reader, Plist dict) {
		String key = null;
		while (reader.hasNext()) {
			PlistTag tag = reader.next();
			if (tag.equals("key")) {
				key = tag.getValue();
				continue;
			}
			if (tag.equals("string")) {
				String string = tag.getValue();
				if (key == null) {
					System.err.println((new Date().toString()) + " - PlistParser, line " + reader.getLine()
							+ ": no key found for <string> '" + string + "' - ignored.");
					continue;
				}
				dict.put(key, XMLCoder.decode(string));
				key = null;
				continue;
			}
			if (tag.equals("/dict"))
				return;
			if (tag.equals("dict")) {
				Plist subdict = new Plist();
				// catch empty dicts
				if (tag.getValue() == null)
					parseDict(reader, subdict);
				if (key == null) {
					System.err.println((new Date().toString()) + " - PlistParser, line " + reader.getLine()
							+ ": no key found for <dict> - ignored.");
					continue;
				}
				dict.put(key, subdict);
				key = null;
				continue;
			}
			if (tag.equals("array")) {
				List<Object> array = new ArrayList<Object>();
				// catch empty arrays
				if (tag.getValue() == null)
					parseArray(reader, array);
				if (key == null) {
					System.err.println((new Date().toString()) + " - PlistParser, line " + reader.getLine()
							+ ": no key found for <array> - ignored.");
					continue;
				}
				dict.put(key, array);
				key = null;
				continue;
			}
			if (tag.equals("integer")) {
				String integer = tag.getValue();
				if (key == null) {
					System.err.println((new Date().toString()) + " - PlistParser, line " + reader.getLine()
							+ ": no key found for <integer> '" + integer + "' - ignored.");
					continue;
				}
				dict.put(key, Integer.parseInt(integer));
				key = null;
				continue;
			}
			if (tag.equals("real")) {
				String real = tag.getValue();
				if (key == null) {
					System.err.println((new Date().toString()) + " - PlistParser, line " + reader.getLine()
							+ ": no key found for <real> '" + real + "' - ignored.");
					continue;
				}
				if (real.startsWith("0x"))
					dict.put(key, Double.longBitsToDouble(Long.parseLong(real.substring(2), 16)));
				else
					dict.put(key, Double.parseDouble(real));
				key = null;
				continue;
			}
			if (tag.equals("true")) {
				if (key == null) {
					System.err.println((new Date().toString()) + " - PlistParser, line " + reader.getLine()
							+ ": no key found for <true/> - ignored.");
					continue;
				}
				dict.put(key, Boolean.parseBoolean("true"));
				key = null;
				continue;
			}
			if (tag.equals("false")) {
				if (key == null) {
					System.err.println((new Date().toString()) + " - PlistParser, line " + reader.getLine()
							+ ": no key found for <false/> - ignored.");
					continue;
				}
				dict.put(key, Boolean.parseBoolean("false"));
				key = null;
				continue;
			}
			// no other tags should be encountered when parsing <dict>
			System.err.println((new Date().toString()) + " - PlistParser, line " + reader.getLine() + ": unknown tag "
					+ tag.getTag() + " - ignored.");
		}
		System.err.println(
				(new Date().toString()) + " - PlistParser, line " + reader.getLine() + ": </dict> missing - failed.");
	}

	/**
	 * Parses an array entry, <code>&lt;array&gt;</code>, in the
	 * <code>plist</code>-string provided by <code>reader</code> and writes all
	 * elements to the list <code>array</code>.
	 * <p>
	 * <em>Note:</em> Invalid or unknown tags trigger error message on standard out.
	 * </p>
	 * 
	 * @param reader iterator over <code>plist</code> tags
	 * @param array  list for storing the array of <code>plist</code> elements.
	 */
	protected static void parseArray(PlistReader reader, List<Object> array) {
		while (reader.hasNext()) {
			PlistTag tag = reader.next();
			if (tag.equals("key")) {
				System.err.println((new Date().toString()) + " - PlistParser, line " + reader.getLine()
						+ ": <array> should not contain <key> - ignored.");
				continue;
			}
			if (tag.equals("string")) {
				array.add(XMLCoder.decode(tag.getValue()));
				continue;
			}
			if (tag.equals("dict")) {
				Plist dict = new Plist();
				// catch empty dicts
				if (tag.getValue() == null)
					parseDict(reader, dict);
				array.add(dict);
				continue;
			}
			if (tag.equals("/array"))
				return;
			if (tag.equals("array")) {
				List<Object> subarray = new ArrayList<Object>();
				// catch empty arrays
				if (tag.getValue() == null)
					parseArray(reader, subarray);
				array.add(subarray);
				continue;
			}
			if (tag.equals("integer")) {
				array.add(Integer.parseInt(tag.getValue()));
				continue;
			}
			if (tag.equals("real")) {
				String real = tag.getValue();
				if (real.startsWith("0x"))
					array.add(Double.longBitsToDouble(Long.parseLong(real.substring(2), 16)));
				else
					array.add(Double.parseDouble(tag.getValue()));
				continue;
			}
			if (tag.equals("true")) {
				array.add(Boolean.parseBoolean("true"));
				continue;
			}
			if (tag.equals("false")) {
				array.add(Boolean.parseBoolean("false"));
				continue;
			}
			// no other tags should be encountered when parsing <array>
			System.err.println((new Date().toString()) + " - PlistParser, line " + reader.getLine() + ": unknown tag "
					+ tag.getTag() + " - ignored.");
		}
		System.err.println(
				(new Date().toString()) + " - PlistParser, line " + reader.getLine() + ": </array> missing - failed.");
	}
}
