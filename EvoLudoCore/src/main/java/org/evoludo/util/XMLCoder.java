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

package org.evoludo.util;

import java.util.HashMap;
import java.util.Map;

/**
 * Encode and decode XML strings.
 * 
 * @author Christoph Hauert
 */
public class XMLCoder {

	/**
	 * Lookup table for decoding XML strings.
	 */
	private static final Map<String, Character> XMLDecode = new HashMap<>();

	/**
	 * Lookup table for encoding XML strings.
	 */
	private static final Map<Character, String> XMLEncode = new HashMap<>();

	/**
	 * Lookup table for decoding HTML strings.
	 */
	private static final Map<String, Character> HTMLDecode = new HashMap<>();

	/**
	 * Suppresses default constructor, ensuring non-instantiability.
	 */
	private XMLCoder() {
	}

	/**
	 * Encode string as XML. Encoded HTML values are converted to UTF-8 characters
	 * except for the five XML entities {@code &amp;, &apos; &lt; &gt; &quot;}.
	 * 
	 * @param string the string to encode in XML
	 * @return the encoded string
	 */
	public static String encode(String string) {
		if (string == null || string.isEmpty()) {
			return string;
		}

		StringBuilder result = new StringBuilder();

		for (int i = 0; i < string.length(); i++) {
			char ch = string.charAt(i);

			// check if this starts an HTML entity that should be converted to UTF-8
			if (ch == '&') {
				int semicolon = string.indexOf(';', i);
				if (semicolon > i) {
					String entity = string.substring(i, semicolon + 1);
					Character utf8 = HTMLDecode.get(entity);
					if (utf8 != null) {
						// Replace HTML entity with UTF-8 character
						result.append(utf8);
						i = semicolon; // Skip past the entity
						continue;
					}
				}
			}

			// check if character needs XML encoding
			String xmlEntity = XMLEncode.get(ch);
			if (xmlEntity != null) {
				result.append(xmlEntity);
			} else if (ch > 0x7F) {
				// Encode non-ASCII characters as numeric entities
				result.append("&#").append((int) ch).append(";");
			} else {
				result.append(ch);
			}
		}

		return result.toString();
	}

	/**
	 * Decode XML string.
	 * 
	 * @param string the XML string to decode
	 * @return the decoded string
	 */
	public static String decode(String string) {
		if (string == null || string.isEmpty())
			return string;
		int start = 0;
		StringBuilder decoded = new StringBuilder(string);
		while ((start = decoded.indexOf("&", start)) >= 0) {
			int end = decoded.indexOf(";", start);
			if (end < 0) {
				// no semicolon found, no further decoding necessary
				break;
			}
			if (decoded.charAt(start + 1) == '#') {
				try {
					// numeric character reference
					String numeric = decoded.substring(start + 2, end);
					int code;
					if (numeric.startsWith("x") || numeric.startsWith("X")) {
						// hexadecimal
						code = Integer.parseInt(numeric.substring(1), 16);
					} else {
						// decimal
						code = Integer.parseInt(numeric);
					}
					decoded.replace(start, end + 1, Character.toString((char) code));
					start++;
				} catch (NumberFormatException e) {
					// invalid numeric reference, skip and pray - or replace with something
					// harmless?
					start = end + 1;
				}
			} else {
				// named character reference
				String named = decoded.substring(start, end + 1);
				Character replacement = XMLDecode.get(named);
				if (replacement == null) {
					// skip and pray
					start = end + 1;
					continue;
				}
				decoded.replace(start, end + 1, Character.toString(replacement));
			}
		}
		return decoded.toString();
	}

	static {
		// Special characters for XML - encode
		XMLEncode.put('\u0026', "&amp;");
		XMLEncode.put('\'', "&apos;");
		XMLEncode.put('<', "&lt;");
		XMLEncode.put('>', "&gt;");
		XMLEncode.put('\u0022', "&quot;");
		// Special characters for XML - decode
		XMLDecode.put("&amp;", '&');
		XMLDecode.put("&apos;", '\'');
		XMLDecode.put("&gt;", '>');
		XMLDecode.put("&lt;", '<');
		XMLDecode.put("&quot;", '"');
		// skip decoding of special XML characters
		HTMLDecode.put("&OElig;", '\u0152');
		HTMLDecode.put("&oelig;", '\u0153');
		HTMLDecode.put("&Scaron;", '\u0160');
		HTMLDecode.put("&scaron;", '\u0161');
		HTMLDecode.put("&Yuml;", '\u0178');
		HTMLDecode.put("&circ;", '\u02C6');
		HTMLDecode.put("&tilde;", '\u02DC');
		HTMLDecode.put("&ensp;", '\u2002');
		HTMLDecode.put("&emsp;", '\u2003');
		HTMLDecode.put("&thinsp;", '\u2009');
		HTMLDecode.put("&zwnj;", '\u200C');
		HTMLDecode.put("&zwj;", '\u200D');
		HTMLDecode.put("&lrm;", '\u200E');
		HTMLDecode.put("&rlm;", '\u200F');
		HTMLDecode.put("&ndash;", '\u2013');
		HTMLDecode.put("&mdash;", '\u2014');
		HTMLDecode.put("&lsquo;", '\u2018');
		HTMLDecode.put("&rsquo;", '\u2019');
		HTMLDecode.put("&sbquo;", '\u201A');
		HTMLDecode.put("&ldquo;", '\u201C');
		HTMLDecode.put("&rdquo;", '\u201D');
		HTMLDecode.put("&bdquo;", '\u201E');
		HTMLDecode.put("&dagger;", '\u2020');
		HTMLDecode.put("&Dagger;", '\u2021');
		HTMLDecode.put("&permil;", '\u2030');
		HTMLDecode.put("&lsaquo;", '\u2039');
		HTMLDecode.put("&rsaquo;", '\u203A');
		HTMLDecode.put("&euro;", '\u20AC');
		// Character entity references for ISO 8859-1 characters
		HTMLDecode.put("&nbsp;", '\u00A0');
		HTMLDecode.put("&iexcl;", '\u00A1');
		HTMLDecode.put("&cent;", '\u00A2');
		HTMLDecode.put("&pound;", '\u00A3');
		HTMLDecode.put("&curren;", '\u00A4');
		HTMLDecode.put("&yen;", '\u00A5');
		HTMLDecode.put("&brvbar;", '\u00A6');
		HTMLDecode.put("&sect;", '\u00A7');
		HTMLDecode.put("&uml;", '\u00A8');
		HTMLDecode.put("&copy;", '\u00A9');
		HTMLDecode.put("&ordf;", '\u00AA');
		HTMLDecode.put("&laquo;", '\u00AB');
		HTMLDecode.put("&not;", '\u00AC');
		HTMLDecode.put("&shy;", '\u00AD');
		HTMLDecode.put("&reg;", '\u00AE');
		HTMLDecode.put("&macr;", '\u00AF');
		HTMLDecode.put("&deg;", '\u00B0');
		HTMLDecode.put("&plusmn;", '\u00B1');
		HTMLDecode.put("&sup2;", '\u00B2');
		HTMLDecode.put("&sup3;", '\u00B3');
		HTMLDecode.put("&acute;", '\u00B4');
		HTMLDecode.put("&micro;", '\u00B5');
		HTMLDecode.put("&para;", '\u00B6');
		HTMLDecode.put("&middot;", '\u00B7');
		HTMLDecode.put("&cedil;", '\u00B8');
		HTMLDecode.put("&sup1;", '\u00B9');
		HTMLDecode.put("&ordm;", '\u00BA');
		HTMLDecode.put("&raquo;", '\u00BB');
		HTMLDecode.put("&frac14;", '\u00BC');
		HTMLDecode.put("&frac12;", '\u00BD');
		HTMLDecode.put("&frac34;", '\u00BE');
		HTMLDecode.put("&iquest;", '\u00BF');
		HTMLDecode.put("&Agrave;", '\u00C0');
		HTMLDecode.put("&Aacute;", '\u00C1');
		HTMLDecode.put("&Acirc;", '\u00C2');
		HTMLDecode.put("&Atilde;", '\u00C3');
		HTMLDecode.put("&Auml;", '\u00C4');
		HTMLDecode.put("&Aring;", '\u00C5');
		HTMLDecode.put("&AElig;", '\u00C6');
		HTMLDecode.put("&Ccedil;", '\u00C7');
		HTMLDecode.put("&Egrave;", '\u00C8');
		HTMLDecode.put("&Eacute;", '\u00C9');
		HTMLDecode.put("&Ecirc;", '\u00CA');
		HTMLDecode.put("&Euml;", '\u00CB');
		HTMLDecode.put("&Igrave;", '\u00CC');
		HTMLDecode.put("&Iacute;", '\u00CD');
		HTMLDecode.put("&Icirc;", '\u00CE');
		HTMLDecode.put("&Iuml;", '\u00CF');
		HTMLDecode.put("&ETH;", '\u00D0');
		HTMLDecode.put("&Ntilde;", '\u00D1');
		HTMLDecode.put("&Ograve;", '\u00D2');
		HTMLDecode.put("&Oacute;", '\u00D3');
		HTMLDecode.put("&Ocirc;", '\u00D4');
		HTMLDecode.put("&Otilde;", '\u00D5');
		HTMLDecode.put("&Ouml;", '\u00D6');
		HTMLDecode.put("&times;", '\u00D7');
		HTMLDecode.put("&Oslash;", '\u00D8');
		HTMLDecode.put("&Ugrave;", '\u00D9');
		HTMLDecode.put("&Uacute;", '\u00DA');
		HTMLDecode.put("&Ucirc;", '\u00DB');
		HTMLDecode.put("&Uuml;", '\u00DC');
		HTMLDecode.put("&Yacute;", '\u00DD');
		HTMLDecode.put("&THORN;", '\u00DE');
		HTMLDecode.put("&szlig;", '\u00DF');
		HTMLDecode.put("&agrave;", '\u00E0');
		HTMLDecode.put("&aacute;", '\u00E1');
		HTMLDecode.put("&acirc;", '\u00E2');
		HTMLDecode.put("&atilde;", '\u00E3');
		HTMLDecode.put("&auml;", '\u00E4');
		HTMLDecode.put("&aring;", '\u00E5');
		HTMLDecode.put("&aelig;", '\u00E6');
		HTMLDecode.put("&ccedil;", '\u00E7');
		HTMLDecode.put("&egrave;", '\u00E8');
		HTMLDecode.put("&eacute;", '\u00E9');
		HTMLDecode.put("&ecirc;", '\u00EA');
		HTMLDecode.put("&euml;", '\u00EB');
		HTMLDecode.put("&igrave;", '\u00EC');
		HTMLDecode.put("&iacute;", '\u00ED');
		HTMLDecode.put("&icirc;", '\u00EE');
		HTMLDecode.put("&iuml;", '\u00EF');
		HTMLDecode.put("&eth;", '\u00F0');
		HTMLDecode.put("&ntilde;", '\u00F1');
		HTMLDecode.put("&ograve;", '\u00F2');
		HTMLDecode.put("&oacute;", '\u00F3');
		HTMLDecode.put("&ocirc;", '\u00F4');
		HTMLDecode.put("&otilde;", '\u00F5');
		HTMLDecode.put("&ouml;", '\u00F6');
		HTMLDecode.put("&divide;", '\u00F7');
		HTMLDecode.put("&oslash;", '\u00F8');
		HTMLDecode.put("&ugrave;", '\u00F9');
		HTMLDecode.put("&uacute;", '\u00FA');
		HTMLDecode.put("&ucirc;", '\u00FB');
		HTMLDecode.put("&uuml;", '\u00FC');
		HTMLDecode.put("&yacute;", '\u00FD');
		HTMLDecode.put("&thorn;", '\u00FE');
		HTMLDecode.put("&yuml;", '\u00FF');
		// Mathematical, Greek and Symbolic characters for HTML
		HTMLDecode.put("&fnof;", '\u0192');
		HTMLDecode.put("&Alpha;", '\u0391');
		HTMLDecode.put("&Beta;", '\u0392');
		HTMLDecode.put("&Gamma;", '\u0393');
		HTMLDecode.put("&Delta;", '\u0394');
		HTMLDecode.put("&Epsilon;", '\u0395');
		HTMLDecode.put("&Zeta;", '\u0396');
		HTMLDecode.put("&Eta;", '\u0397');
		HTMLDecode.put("&Theta;", '\u0398');
		HTMLDecode.put("&Iota;", '\u0399');
		HTMLDecode.put("&Kappa;", '\u039A');
		HTMLDecode.put("&Lambda;", '\u039B');
		HTMLDecode.put("&Mu;", '\u039C');
		HTMLDecode.put("&Nu;", '\u039D');
		HTMLDecode.put("&Xi;", '\u039E');
		HTMLDecode.put("&Omicron;", '\u039F');
		HTMLDecode.put("&Pi;", '\u03A0');
		HTMLDecode.put("&Rho;", '\u03A1');
		HTMLDecode.put("&Sigma;", '\u03A3');
		HTMLDecode.put("&Tau;", '\u03A4');
		HTMLDecode.put("&Upsilon;", '\u03A5');
		HTMLDecode.put("&Phi;", '\u03A6');
		HTMLDecode.put("&Chi;", '\u03A7');
		HTMLDecode.put("&Psi;", '\u03A8');
		HTMLDecode.put("&Omega;", '\u03A9');
		HTMLDecode.put("&alpha;", '\u03B1');
		HTMLDecode.put("&beta;", '\u03B2');
		HTMLDecode.put("&gamma;", '\u03B3');
		HTMLDecode.put("&delta;", '\u03B4');
		HTMLDecode.put("&epsilon;", '\u03B5');
		HTMLDecode.put("&zeta;", '\u03B6');
		HTMLDecode.put("&eta;", '\u03B7');
		HTMLDecode.put("&theta;", '\u03B8');
		HTMLDecode.put("&iota;", '\u03B9');
		HTMLDecode.put("&kappa;", '\u03BA');
		HTMLDecode.put("&lambda;", '\u03BB');
		HTMLDecode.put("&mu;", '\u03BC');
		HTMLDecode.put("&nu;", '\u03BD');
		HTMLDecode.put("&xi;", '\u03BE');
		HTMLDecode.put("&omicron;", '\u03BF');
		HTMLDecode.put("&pi;", '\u03C0');
		HTMLDecode.put("&rho;", '\u03C1');
		HTMLDecode.put("&sigmaf;", '\u03C2');
		HTMLDecode.put("&sigma;", '\u03C3');
		HTMLDecode.put("&tau;", '\u03C4');
		HTMLDecode.put("&upsilon;", '\u03C5');
		HTMLDecode.put("&phi;", '\u03C6');
		HTMLDecode.put("&chi;", '\u03C7');
		HTMLDecode.put("&psi;", '\u03C8');
		HTMLDecode.put("&omega;", '\u03C9');
		HTMLDecode.put("&thetasym;", '\u03D1');
		HTMLDecode.put("&upsih;", '\u03D2');
		HTMLDecode.put("&piv;", '\u03D6');
		HTMLDecode.put("&bull;", '\u2022');
		HTMLDecode.put("&hellip;", '\u2026');
		HTMLDecode.put("&prime;", '\u2032');
		HTMLDecode.put("&Prime;", '\u2033');
		HTMLDecode.put("&oline;", '\u203E');
		HTMLDecode.put("&frasl;", '\u2044');
		HTMLDecode.put("&weierp;", '\u2118');
		HTMLDecode.put("&image;", '\u2111');
		HTMLDecode.put("&real;", '\u211C');
		HTMLDecode.put("&trade;", '\u2122');
		HTMLDecode.put("&alefsym;", '\u2135');
		HTMLDecode.put("&larr;", '\u2190');
		HTMLDecode.put("&uarr;", '\u2191');
		HTMLDecode.put("&rarr;", '\u2192');
		HTMLDecode.put("&darr;", '\u2193');
		HTMLDecode.put("&harr;", '\u2194');
		HTMLDecode.put("&crarr;", '\u21B5');
		HTMLDecode.put("&lArr;", '\u21D0');
		HTMLDecode.put("&uArr;", '\u21D1');
		HTMLDecode.put("&rArr;", '\u21D2');
		HTMLDecode.put("&dArr;", '\u21D3');
		HTMLDecode.put("&hArr;", '\u21D4');
		HTMLDecode.put("&forall;", '\u2200');
		HTMLDecode.put("&part;", '\u2202');
		HTMLDecode.put("&exist;", '\u2203');
		HTMLDecode.put("&empty;", '\u2205');
		HTMLDecode.put("&nabla;", '\u2207');
		HTMLDecode.put("&isin;", '\u2208');
		HTMLDecode.put("&notin;", '\u2209');
		HTMLDecode.put("&ni;", '\u220B');
		HTMLDecode.put("&prod;", '\u220F');
		HTMLDecode.put("&sum;", '\u2211');
		HTMLDecode.put("&minus;", '\u2212');
		HTMLDecode.put("&lowast;", '\u2217');
		HTMLDecode.put("&radic;", '\u221A');
		HTMLDecode.put("&prop;", '\u221D');
		HTMLDecode.put("&infin;", '\u221E');
		HTMLDecode.put("&ang;", '\u2220');
		HTMLDecode.put("&and;", '\u2227');
		HTMLDecode.put("&or;", '\u2228');
		HTMLDecode.put("&cap;", '\u2229');
		HTMLDecode.put("&cup;", '\u222A');
		HTMLDecode.put("&int;", '\u222B');
		HTMLDecode.put("&there4;", '\u2234');
		HTMLDecode.put("&sim;", '\u223C');
		HTMLDecode.put("&cong;", '\u2245');
		HTMLDecode.put("&asymp;", '\u2248');
		HTMLDecode.put("&ne;", '\u2260');
		HTMLDecode.put("&equiv;", '\u2261');
		HTMLDecode.put("&le;", '\u2264');
		HTMLDecode.put("&ge;", '\u2265');
		HTMLDecode.put("&sub;", '\u2282');
		HTMLDecode.put("&sup;", '\u2283');
		HTMLDecode.put("&nsub;", '\u2284');
		HTMLDecode.put("&sube;", '\u2286');
		HTMLDecode.put("&supe;", '\u2287');
		HTMLDecode.put("&oplus;", '\u2295');
		HTMLDecode.put("&otimes;", '\u2297');
		HTMLDecode.put("&perp;", '\u22A5');
		HTMLDecode.put("&sdot;", '\u22C5');
		HTMLDecode.put("&lceil;", '\u2308');
		HTMLDecode.put("&rceil;", '\u2309');
		HTMLDecode.put("&lfloor;", '\u230A');
		HTMLDecode.put("&rfloor;", '\u230B');
		HTMLDecode.put("&lang;", '\u2329');
		HTMLDecode.put("&rang;", '\u232A');
		HTMLDecode.put("&loz;", '\u25CA');
		HTMLDecode.put("&spades;", '\u2660');
		HTMLDecode.put("&clubs;", '\u2663');
		HTMLDecode.put("&hearts;", '\u2665');
		HTMLDecode.put("&diams;", '\u2666');
	}
}
