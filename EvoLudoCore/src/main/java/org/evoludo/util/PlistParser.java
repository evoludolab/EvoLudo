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

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

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
	// /**
	// *
	// * @param xmlfile
	// * @return
	// */
	// public static Map<String,Object> parse(File xmlfile) {
	// Map<String,Object> plist = new HashMap<String,Object>();
	// try {
	// PlistReader reader = new PlistReader(new BufferedReader(new
	// InputStreamReader(new FileInputStream(xmlfile), "UTF-8")));
	// while( reader.hasNext() ) {
	// PlistTag tag = reader.next();
	// if( tag.equals("plist") ) {
	// // check version of plist specifications? - see attributes of reader
	// continue;
	// }
	// if( tag.equals("dict") ) {
	// parseDict(reader, plist);
	// continue;
	// }
	// if( tag.equals("/plist") ) break;
	// // all other tags should not be encountered when parsing <plist>
	// System.err.println((new Date().toString())+" - PlistParser, line
	// "+reader.getLine()+": unknown tag '"+tag.getTag()+"' - ignored.");
	// }
	// reader.close();
	// }
	// catch( UnsupportedEncodingException e ) {
	// System.err.println((new Date().toString())+" - PlistParser: UTF-8 encoding
	// unsupported. "+e.getMessage());
	// e.printStackTrace(System.err);
	// return null;
	// }
	// catch( FileNotFoundException e ) {
	// System.err.println((new Date().toString())+" - PlistParser: file
	// "+xmlfile.getName()+" not found. "+e.getMessage());
	// e.printStackTrace(System.err);
	// return null;
	// }
	// return plist;
	// }

	/**
	 * Parses the contents of a <code>plist</code> file supplied as a String and
	 * returns
	 * a {@link Plist} with key and object associations. The parser processes the
	 * following
	 * <code>plist</code> elements:
	 * <dl>
	 * <dt>{@code <key>}</dt>
	 * <dd>Name of tag: any valid String.</dd>
	 * <dt>{@code <dict>}</dt>
	 * <dd>Dictionary: Alternating {@code <key>} tags and
	 * <code>plist</code> elements (excluding {@code <key>}). Can be
	 * empty.</dd>
	 * <dt>{@code <array>}</dt>
	 * <dd>Array: Can contain any number of identical child <code>plist</code>
	 * elements (excluding {@code <key>}). Can be empty.</dd>
	 * <dt>{@code <string>}</dt>
	 * <dd>UTF-8 encoded string.</dd>
	 * <dt>{@code <real>}</dt>
	 * <dd>Floating point number: if the string ends with '{@code L}' it is assumed
	 * to be a double encoded as a long.
	 * <p>
	 * <strong>Important:</strong>
	 * <ul>
	 * <li>Using long to encode floating point numbers is not part of the
	 * <code>plist</code> specification. However, only bitwise encoding can
	 * guarantee faithful writing and restoring of floating point numbers.</li>
	 * <li>Cannot use {@link Double#valueOf(String)} because it is not implemented
	 * by GWT.</li>
	 * </ul>
	 * </dd>
	 * <dt>{@code <integer>}</dt>
	 * <dd>Integer number: any string that {@link Integer#parseInt(String)} can
	 * process, i.e., limited to 32 bits.</dd>
	 * <dt>{@code <true/>}, {@code <false/>}</dt>
	 * <dd>Boolean values: tag represents the boolean values <code>true</code> and
	 * <code>false</code>.</dd>
	 * </dl>
	 * <p>
	 * <strong>Not currently implemented:</strong>
	 * </p>
	 * <dl>
	 * <dt>{@code <data>}</dt>
	 * <dd>Base64 encoded data.</dd>
	 * <dt>{@code <date>}</dt>
	 * <dd>ISO 8601 formatted string.</dd>
	 * </dl>
	 * <p>
	 * <em>Note:</em> Invalid or unknown tags trigger an error message on standard
	 * error.
	 * </p>
	 *
	 * @param string contents of the <code>plist</code> file
	 * @return a {@link Plist} with key and element associations
	 */
	public static Plist parse(String string) {
		Plist plist = new Plist();
		PlistReader reader = new PlistReader(string);
		while (reader.hasNext()) {
			PlistTag tag = reader.next();
			String name = tag.getTag();
			if (name.equals("/" + TAG_PLIST))
				break;
			if (name.equals(TAG_PLIST)) {
				// check version of plist specifications? - see attributes of reader
			} else if (name.equals(TAG_DICT)) {
				parseDict(reader, plist);
			} else {
				// no other tags should be encountered when parsing <plist>
				logInvalidTagWarning(reader.getLine(), name, TAG_PLIST);
			}
		}
		return plist;
	}

	/**
	 * Parses a dictionary entry, {@code <dict>}, in the
	 * <code>plist</code>-string provided by <code>reader</code> and writes
	 * {@code <key>} and <code>plist</code> element pairs to the lookup
	 * table <code>dict</code>. Note, dictionaries may contain
	 * {@code <dict>} elements, which results in recursive calls to this
	 * method.
	 * <p>
	 * <em>Note:</em> Invalid or unknown tags trigger error message on standard out.
	 * </p>
	 * 
	 * @param reader iterator over <code>plist</code> tags
	 * @param dict   map for storing all pairs of {@code <key>} and
	 *               <code>plist</code> element pairs.
	 */
	protected static Plist parseDict(PlistReader reader, Plist dict) {
		String key = null;
		while (reader.hasNext()) {
			PlistTag tag = reader.next();
			String name = tag.getTag();
			switch (name) {
				case "/" + TAG_DICT:
					return dict;
				case TAG_KEY:
					key = tag.getValue();
					continue;
				case TAG_STRING:
					store(reader, dict, key, XMLCoder.decode(tag.getValue()));
					break;
				case TAG_DICT:
					if (tag.isSelfClosing()) {
						store(reader, dict, key, new Plist());
					} else {
						store(reader, dict, key, parseDict(reader, new Plist()));
					}
					break;
				case TAG_ARRAY:
					if (tag.isSelfClosing()) {
						store(reader, dict, key, new ArrayList<>());
					} else {
						store(reader, dict, key, parseArray(reader, new ArrayList<>()));
					}
					break;
				case TAG_INTEGER:
					if (tag.isSelfClosing()) {
						// ignore empty dict
						break;
					}
					store(reader, dict, key, Integer.parseInt(tag.getValue()));
					break;
				case TAG_REAL:
					store(reader, dict, key, parseReal(tag.getValue()));
					break;
				case TAG_TRUE:
					store(reader, dict, key, Boolean.TRUE);
					break;
				case TAG_FALSE:
					store(reader, dict, key, Boolean.FALSE);
					break;
				default:
					logInvalidTagWarning(reader.getLine(), name, TAG_DICT);
					break;
			}
			key = null;
		}
		logClosingTagWarning(reader.getLine(), TAG_DICT);
		return dict;
	}

	/**
	 * Parses an array entry, <code>&lt;array&gt;</code>, in the
	 * <code>plist</code>-string provided by <code>reader</code> and writes all
	 * elements to the list <code>array</code>.
	 * <p>
	 * <em>Note:</em> Invalid or unknown tags trigger log warnings and errors.
	 * </p>
	 * 
	 * @param reader iterator over <code>plist</code> tags
	 * @param array  list for storing the array of <code>plist</code> elements.
	 */
	protected static List<Object> parseArray(PlistReader reader, List<Object> array) {
		while (reader.hasNext()) {
			PlistTag tag = reader.next();
			String name = tag.getTag();
			switch (name) {
				case "/" + TAG_ARRAY:
					return array;
				case "/" + TAG_DICT:
				case "/" + TAG_PLIST:
				case TAG_KEY:
					reader.pushTag(tag);
					return array;
				case TAG_STRING:
					array.add(XMLCoder.decode(tag.getValue()));
					break;
				case TAG_DICT:
					Plist subdict = new Plist();
					if (!tag.isSelfClosing() && tag.getValue() == null)
						parseDict(reader, subdict);
					array.add(subdict);
					break;
				case TAG_ARRAY:
					List<Object> subarray = new ArrayList<>();
					if (!tag.isSelfClosing() && tag.getValue() == null)
						parseArray(reader, subarray);
					array.add(subarray);
					break;
				case TAG_INTEGER:
					array.add(Integer.parseInt(tag.getValue()));
					break;
				case TAG_REAL:
					array.add(parseReal(tag.getValue()));
					break;
				case TAG_TRUE:
					array.add(Boolean.parseBoolean(TAG_TRUE));
					break;
				case TAG_FALSE:
					array.add(Boolean.parseBoolean(TAG_FALSE));
					break;
				default:
					logInvalidTagWarning(reader.getLine(), name, TAG_ARRAY);
					break;
			}
		}
		logClosingTagWarning(reader.getLine(), TAG_ARRAY);
		return array;
	}

	private static double parseReal(String real) {
		if (real.endsWith("L"))
			return Double.longBitsToDouble(Long.valueOf(real.substring(0, real.length() - 1)));
		return Double.parseDouble(real);
	}

	private static void store(PlistReader reader, Plist dict, String key, Object value) {
		if (key == null) {
			if (LOGGER.isLoggable(java.util.logging.Level.WARNING))
				LOGGER.warning(MSG_LINE + reader.getLine() + ": key missing for '"
						+ (value == null ? "null" : value.getClass().getSimpleName())
						+ "'" + MSG_IGNORE);
			return;
		}
		dict.put(key, value);
	}

	private static final String TAG_PLIST = "plist";
	private static final String TAG_DICT = "dict";
	private static final String TAG_ARRAY = "array";
	private static final String TAG_STRING = "string";
	private static final String TAG_INTEGER = "integer";
	private static final String TAG_REAL = "real";
	private static final String TAG_TRUE = "true";
	private static final String TAG_FALSE = "false";
	private static final String TAG_KEY = "key";

	private static final String MSG_LINE = "line ";
	private static final String MSG_IGNORE = " - ignored.";

	private static final Logger LOGGER = Logger.getLogger(PlistParser.class.getName());

	private static void logClosingTagWarning(int line, String tag) {
		if (LOGGER.isLoggable(java.util.logging.Level.WARNING))
			LOGGER.warning(MSG_LINE + line + ": closing tag </" + tag + "> missing" + MSG_IGNORE);
	}

	private static void logInvalidTagWarning(int line, String tag, String context) {
		if (LOGGER.isLoggable(java.util.logging.Level.WARNING))
			LOGGER.warning(MSG_LINE + line + ": invalid tag <" + tag + "> in <" + context + ">" + MSG_IGNORE);
	}
}
