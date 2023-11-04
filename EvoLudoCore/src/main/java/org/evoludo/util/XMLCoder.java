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
	private static final Map<String, String> XMLDecode = new HashMap<String, String>();

	/**
	 * Lookup table for encoding XML strings.
	 */
	private static final Map<Character, String> XMLEncode = new HashMap<Character, String>();

// NOTE: CURRENTLY UNUSED BUT KEPT FOR POSSIBLE FUTURE REFERENCE
//	/**
//	 * Lookup table to decode HTML strings.
//	 */
//	public static final Map<String,Character> HTMLDecode = new HashMap<>();

	/**
	 * <code>true</code> to strictly adhering to XML standard. If <code>false</code>
	 * some additional characters are encoded/decoded that are invalid in XML (or
	 * XHTML) but acceptable in ePubs.
	 */
	private static boolean strict = false;

	/**
	 * Set the encoding/decoding mode to <code>strict</code>. The encoding/decoding
	 * strictly adheres to XML standards if <code>true</code>. If the mode changed
	 * the (static) lookup tables are re-initialized.
	 * 
	 * @param strict encoding/decoding mode
	 * @see #init()
	 */
	public static void setStrict(boolean strict) {
		if (XMLCoder.strict == strict)
			return;
		XMLCoder.strict = strict;
		init();
	}

	/**
	 * @return <code>true</code> if strictly XML compliant encoding/decoding
	 */
	public static boolean isStrict() {
		return strict;
	}

	/**
	 * Suppresses default constructor, ensuring non-instantiability.
	 */
	private XMLCoder() {
	}

	/**
	 * Encode string as XML. If <code>strict==false</code> some additional
	 * characters are encoded that still validate in ePub's.
	 * <p>
	 * <strong>Note:</strong> XHTML is very fussy about characters in argument to
	 * innerHTML. It does not seem to accept any kind of special character even when
	 * properly encoded. For example, the properly encoded <code>'√'</code>
	 * (<code>&#8730;</code>) or <code>&radic;</code> both result in an error (even
	 * though the latter is perfectly fine in XHTML documents).
	 * </p>
	 * 
	 * @param string to encode in XML
	 * @return encoded string
	 */
	public static String encode(String string) {
		if (string == null)
			return null;
		int len = string.length();
		if (len < 1)
			return string;
		StringBuilder html = new StringBuilder(string);
		for (int i = 0; i < len; i++) {
			char ch = html.charAt(i);
// when injecting a string using innerHTML not even HTML encoded character are acceptable in XHTML documents
//			if( ch==0x26 ) {
//				// & found - let's see if this is an encoded entity; find following ';'
//				int j = html.indexOf(";", i);
//				if( j>i && HTMLDecode.get(html.substring(i, j+1))!=null ) {
//					// valid html code found - skip encoding
//					i = j+1;
//					continue;
//				}
//			}
			String enc = XMLEncode.get(Character.valueOf(ch));
			if (enc == null && ch > 0x7F) {
				enc = "&#" + ch + ";";
			}
			if (enc != null) {
				html.replace(i, i + 1, enc);
				int skip = enc.length() - 1;
				i += skip;
				len += skip;
			}
		}
		return html.toString();
	}

	/**
	 * Decode XML string. If <code>strict==false</code> some additional characters
	 * are decoded that still validate in ePub's.
	 * 
	 * @param string XML string to decode
	 * @return decoded string
	 */
	public static String decode(String string) {
		int start, end = 0;
		StringBuilder decoded = new StringBuilder(string);
		while ((start = decoded.indexOf("&", end)) >= 0) {
			end = decoded.indexOf(";", start) + 1;
			decoded.replace(start, end, XMLDecode.get(decoded.substring(start, end)));
		}
		return decoded.toString();
	}

	static {
		init();
	}

	/**
	 * Initialize lookup tables for encoding and decoding XML strings.
	 */
	private static void init() {
		// clear (en)coding maps
		XMLEncode.clear();
		XMLDecode.clear();

		// init XMLEncode
		XMLEncode.put('>', "&gt;");
		XMLEncode.put('<', "&lt;");
		XMLEncode.put('"', "&quot;");
		XMLEncode.put('&', "&amp;");
		XMLEncode.put('\n', "<br/>");
		// init XMLDecode
		XMLDecode.put("&gt;", ">");
		XMLDecode.put("&lt;", "<");
		XMLDecode.put("&quot;", "\"");
		XMLDecode.put("&amp;", "&");

		// additions if (en)coding is non-strict, i.e. invalid XHTML but ok e.g. in
		// ePubs
		if (strict) {
			// invalid XML characters - render them harmless
			XMLEncode.put('\u00a9', "&#169;"); // copyright
			XMLEncode.put('\u0394', "&#916;"); // upper case delta
		} else {
			// additional characters to encode
			XMLEncode.put('\u00a9', "&copy;"); // copyright
			XMLEncode.put('\u0394', "&Delta;"); // upper case delta
			// additional characters to decode
			XMLDecode.put("&copy;", "\u00a9");
			XMLDecode.put("&Delta;", "\u0394");
		}
//		// adapted from http://www.koders.com/, HTMLDecoder
//		HTMLDecode.put("&quot;", (char)34);
//		HTMLDecode.put("&amp;", (char)38);
////		HTMLDecode.put("&apos;", (char)39);		// this is not valid HTML...
//		HTMLDecode.put("&lt;", (char)60);
//		HTMLDecode.put("&gt;", (char)62);
//		HTMLDecode.put("&nbsp;", (char)160);
//		HTMLDecode.put("&iexcl;", (char)161);
//		HTMLDecode.put("&cent;", (char)162);
//		HTMLDecode.put("&pound;", (char)163);
//		HTMLDecode.put("&curren;", (char)164);
//		HTMLDecode.put("&yen;", (char)165);
//		HTMLDecode.put("&brvbar;", (char)166);
//		HTMLDecode.put("&sect;", (char)167);
//		HTMLDecode.put("&uml;", (char)168);
//		HTMLDecode.put("&copy;", (char)169);
//		HTMLDecode.put("&ordf;", (char)170);
//		HTMLDecode.put("&laquo;", (char)171);
//		HTMLDecode.put("&not;", (char)172);
//		HTMLDecode.put("&shy;", (char)173);
//		HTMLDecode.put("&reg;", (char)174);
//		HTMLDecode.put("&macr;", (char)175);
//		HTMLDecode.put("&deg;", (char)176);
//		HTMLDecode.put("&plusmn;", (char)177);
//		HTMLDecode.put("&sup2;", (char)178);
//		HTMLDecode.put("&sup3;", (char)179);
//		HTMLDecode.put("&acute;", (char)180);
//		HTMLDecode.put("&micro;", (char)181);
//		HTMLDecode.put("&para;", (char)182);
//		HTMLDecode.put("&middot;", (char)183);
//		HTMLDecode.put("&cedil;", (char)184);
//		HTMLDecode.put("&sup1;", (char)185);
//		HTMLDecode.put("&ordm;", (char)186);
//		HTMLDecode.put("&raquo;", (char)187);
//		HTMLDecode.put("&frac14;", (char)188);
//		HTMLDecode.put("&frac12;", (char)189);
//		HTMLDecode.put("&frac34;", (char)190);
//		HTMLDecode.put("&iquest;", (char)191);
//		HTMLDecode.put("&Agrave;", (char)192);
//		HTMLDecode.put("&Aacute;", (char)193);
//		HTMLDecode.put("&Acirc;", (char)194);
//		HTMLDecode.put("&Atilde;", (char)195);
//		HTMLDecode.put("&Auml;", (char)196);
//		HTMLDecode.put("&Aring;", (char)197);
//		HTMLDecode.put("&AElig;", (char)198);
//		HTMLDecode.put("&Ccedil;", (char)199);
//		HTMLDecode.put("&Egrave;", (char)200);
//		HTMLDecode.put("&Eacute;", (char)201);
//		HTMLDecode.put("&Ecirc;", (char)202);
//		HTMLDecode.put("&Euml;", (char)203);
//		HTMLDecode.put("&Igrave;", (char)204);
//		HTMLDecode.put("&Iacute;", (char)205);
//		HTMLDecode.put("&Icirc;", (char)206);
//		HTMLDecode.put("&Iuml;", (char)207);
//		HTMLDecode.put("&ETH;", (char)208);
//		HTMLDecode.put("&Ntilde;", (char)209);
//		HTMLDecode.put("&Ograve;", (char)210);
//		HTMLDecode.put("&Oacute;", (char)211);
//		HTMLDecode.put("&Ocirc;", (char)212);
//		HTMLDecode.put("&Otilde;", (char)213);
//		HTMLDecode.put("&Ouml;", (char)214);
//		HTMLDecode.put("&times;", (char)215);
//		HTMLDecode.put("&Oslash;", (char)216);
//		HTMLDecode.put("&Ugrave;", (char)217);
//		HTMLDecode.put("&Uacute;", (char)218);
//		HTMLDecode.put("&Ucirc;", (char)219);
//		HTMLDecode.put("&Uuml;", (char)220);
//		HTMLDecode.put("&Yacute;", (char)221);
//		HTMLDecode.put("&THORN;", (char)222);
//		HTMLDecode.put("&szlig;", (char)223);
//		HTMLDecode.put("&agrave;", (char)224);
//		HTMLDecode.put("&aacute;", (char)225);
//		HTMLDecode.put("&acirc;", (char)226);
//		HTMLDecode.put("&atilde;", (char)227);
//		HTMLDecode.put("&auml;", (char)228);
//		HTMLDecode.put("&aring;", (char)229);
//		HTMLDecode.put("&aelig;", (char)230);
//		HTMLDecode.put("&ccedil;", (char)231);
//		HTMLDecode.put("&egrave;", (char)232);
//		HTMLDecode.put("&eacute;", (char)233);
//		HTMLDecode.put("&ecirc;", (char)234);
//		HTMLDecode.put("&euml;", (char)235);
//		HTMLDecode.put("&igrave;", (char)236);
//		HTMLDecode.put("&iacute;", (char)237);
//		HTMLDecode.put("&icirc;", (char)238);
//		HTMLDecode.put("&iuml;", (char)239);
//		HTMLDecode.put("&eth;", (char)240);
//		HTMLDecode.put("&ntilde;", (char)241);
//		HTMLDecode.put("&ograve;", (char)242);
//		HTMLDecode.put("&oacute;", (char)243);
//		HTMLDecode.put("&ocirc;", (char)244);
//		HTMLDecode.put("&otilde;", (char)245);
//		HTMLDecode.put("&ouml;", (char)246);
//		HTMLDecode.put("&divide;", (char)247);
//		HTMLDecode.put("&oslash;", (char)248);
//		HTMLDecode.put("&ugrave;", (char)249);
//		HTMLDecode.put("&uacute;", (char)250);
//		HTMLDecode.put("&ucirc;", (char)251);
//		HTMLDecode.put("&uuml;", (char)252);
//		HTMLDecode.put("&yacute;", (char)253);
//		HTMLDecode.put("&thorn;", (char)254);
//		HTMLDecode.put("&yuml;", (char)255);
//		HTMLDecode.put("&OElig;", (char)338);
//		HTMLDecode.put("&oelig;", (char)339);
//		HTMLDecode.put("&Scaron;", (char)352);
//		HTMLDecode.put("&scaron;", (char)353);
//		HTMLDecode.put("&Yuml;", (char)376);
//		HTMLDecode.put("&fnof;", (char)402);
//		HTMLDecode.put("&circ;", (char)710);
//		HTMLDecode.put("&tilde;", (char)732);
//		HTMLDecode.put("&Alpha;", (char)913);
//		HTMLDecode.put("&Beta;", (char)914);
//		HTMLDecode.put("&Gamma;", (char)915);
//		HTMLDecode.put("&Delta;", (char)916);
//		HTMLDecode.put("&Epsilon;", (char)917);
//		HTMLDecode.put("&Zeta;", (char)918);
//		HTMLDecode.put("&Eta;", (char)919);
//		HTMLDecode.put("&Theta;", (char)920);
//		HTMLDecode.put("&Iota;", (char)921);
//		HTMLDecode.put("&Kappa;", (char)922);
//		HTMLDecode.put("&Lambda;", (char)923);
//		HTMLDecode.put("&Mu;", (char)924);
//		HTMLDecode.put("&Nu;", (char)925);
//		HTMLDecode.put("&Xi;", (char)926);
//		HTMLDecode.put("&Omicron;", (char)927);
//		HTMLDecode.put("&Pi;", (char)928);
//		HTMLDecode.put("&Rho;", (char)929);
//		HTMLDecode.put("&Sigma;", (char)931);
//		HTMLDecode.put("&Tau;", (char)932);
//		HTMLDecode.put("&Upsilon;", (char)933);
//		HTMLDecode.put("&Phi;", (char)934);
//		HTMLDecode.put("&Chi;", (char)935);
//		HTMLDecode.put("&Psi;", (char)936);
//		HTMLDecode.put("&Omega;", (char)937);
//		HTMLDecode.put("&alpha;", (char)945);
//		HTMLDecode.put("&beta;", (char)946);
//		HTMLDecode.put("&gamma;", (char)947);
//		HTMLDecode.put("&delta;", (char)948);
//		HTMLDecode.put("&epsilon;", (char)949);
//		HTMLDecode.put("&zeta;", (char)950);
//		HTMLDecode.put("&eta;", (char)951);
//		HTMLDecode.put("&theta;", (char)952);
//		HTMLDecode.put("&iota;", (char)953);
//		HTMLDecode.put("&kappa;", (char)954);
//		HTMLDecode.put("&lambda;", (char)955);
//		HTMLDecode.put("&mu;", (char)956);
//		HTMLDecode.put("&nu;", (char)957);
//		HTMLDecode.put("&xi;", (char)958);
//		HTMLDecode.put("&omicron;", (char)959);
//		HTMLDecode.put("&pi;", (char)960);
//		HTMLDecode.put("&rho;", (char)961);
//		HTMLDecode.put("&sigmaf;", (char)962);
//		HTMLDecode.put("&sigma;", (char)963);
//		HTMLDecode.put("&tau;", (char)964);
//		HTMLDecode.put("&upsilon;", (char)965);
//		HTMLDecode.put("&phi;", (char)966);
//		HTMLDecode.put("&chi;", (char)967);
//		HTMLDecode.put("&psi;", (char)968);
//		HTMLDecode.put("&omega;", (char)969);
//		HTMLDecode.put("&thetasym;", (char)977);
//		HTMLDecode.put("&upsih;", (char)978);
//		HTMLDecode.put("&piv;", (char)982);
//		HTMLDecode.put("&ensp;", (char)8194);
//		HTMLDecode.put("&emsp;", (char)8195);
//		HTMLDecode.put("&thinsp;", (char)8201);
//		HTMLDecode.put("&zwnj;", (char)8204);
//		HTMLDecode.put("&zwj;", (char)8205);
//		HTMLDecode.put("&lrm;", (char)8206);
//		HTMLDecode.put("&rlm;", (char)8207);
//		HTMLDecode.put("&ndash;", (char)8211);
//		HTMLDecode.put("&mdash;", (char)8212);
//		HTMLDecode.put("&lsquo;", (char)8216);
//		HTMLDecode.put("&rsquo;", (char)8217);
//		HTMLDecode.put("&sbquo;", (char)8218);
//		HTMLDecode.put("&ldquo;", (char)8220);
//		HTMLDecode.put("&rdquo;", (char)8221);
//		HTMLDecode.put("&bdquo;", (char)8222);
//		HTMLDecode.put("&dagger;", (char)8224);
//		HTMLDecode.put("&Dagger;", (char)8225);
//		HTMLDecode.put("&bull;", (char)8226);
//		HTMLDecode.put("&hellip;", (char)8230);
//		HTMLDecode.put("&permil;", (char)8240);
//		HTMLDecode.put("&prime;", (char)8242);
//		HTMLDecode.put("&Prime;", (char)8243);
//		HTMLDecode.put("&lsaquo;", (char)8249);
//		HTMLDecode.put("&rsaquo;", (char)8250);
//		HTMLDecode.put("&oline;", (char)8254);
//		HTMLDecode.put("&frasl;", (char)8260);
//		HTMLDecode.put("&euro;", (char)8364);
//		HTMLDecode.put("&image;", (char)8465);
//		HTMLDecode.put("&weierp;", (char)8472);
//		HTMLDecode.put("&real;", (char)8476);
//		HTMLDecode.put("&trade;", (char)8482);
//		HTMLDecode.put("&alefsym;", (char)8501);
//		HTMLDecode.put("&larr;", (char)8592);
//		HTMLDecode.put("&uarr;", (char)8593);
//		HTMLDecode.put("&rarr;", (char)8594);
//		HTMLDecode.put("&darr;", (char)8595);
//		HTMLDecode.put("&harr;", (char)8596);
//		HTMLDecode.put("&crarr;", (char)8629);
//		HTMLDecode.put("&lArr;", (char)8656);
//		HTMLDecode.put("&uArr;", (char)8657);
//		HTMLDecode.put("&rArr;", (char)8658);
//		HTMLDecode.put("&dArr;", (char)8659);
//		HTMLDecode.put("&hArr;", (char)8660);
//		HTMLDecode.put("&forall;", (char)8704);
//		HTMLDecode.put("&part;", (char)8706);
//		HTMLDecode.put("&exist;", (char)8707);
//		HTMLDecode.put("&empty;", (char)8709);
//		HTMLDecode.put("&nabla;", (char)8711);
//		HTMLDecode.put("&isin;", (char)8712);
//		HTMLDecode.put("&notin;", (char)8713);
//		HTMLDecode.put("&ni;", (char)8715);
//		HTMLDecode.put("&prod;", (char)8719);
//		HTMLDecode.put("&sum;", (char)8721);
//		HTMLDecode.put("&minus;", (char)8722);
//		HTMLDecode.put("&lowast;", (char)8727);
//		HTMLDecode.put("&radic;", (char)8730);
//		HTMLDecode.put("&prop;", (char)8733);
//		HTMLDecode.put("&infin;", (char)8734);
//		HTMLDecode.put("&ang;", (char)8736);
//		HTMLDecode.put("&and;", (char)8743);
//		HTMLDecode.put("&or;", (char)8744);
//		HTMLDecode.put("&cap;", (char)8745);
//		HTMLDecode.put("&cup;", (char)8746);
//		HTMLDecode.put("&int;", (char)8747);
//		HTMLDecode.put("&there4;", (char)8756);
//		HTMLDecode.put("&sim;", (char)8764);
//		HTMLDecode.put("&cong;", (char)8773);
//		HTMLDecode.put("&asymp;", (char)8776);
//		HTMLDecode.put("&ne;", (char)8800);
//		HTMLDecode.put("&equiv;", (char)8801);
//		HTMLDecode.put("&le;", (char)8804);
//		HTMLDecode.put("&ge;", (char)8805);
//		HTMLDecode.put("&sub;", (char)8834);
//		HTMLDecode.put("&sup;", (char)8835);
//		HTMLDecode.put("&nsub;", (char)8836);
//		HTMLDecode.put("&sube;", (char)8838);
//		HTMLDecode.put("&supe;", (char)8839);
//		HTMLDecode.put("&oplus;", (char)8853);
//		HTMLDecode.put("&otimes;", (char)8855);
//		HTMLDecode.put("&perp;", (char)8869);
//		HTMLDecode.put("&sdot;", (char)8901);
//		HTMLDecode.put("&lceil;", (char)8968);
//		HTMLDecode.put("&rceil;", (char)8969);
//		HTMLDecode.put("&lfloor;", (char)8970);
//		HTMLDecode.put("&rfloor;", (char)8971);
//		HTMLDecode.put("&lang;", (char)9001);
//		HTMLDecode.put("&rang;", (char)9002);
//		HTMLDecode.put("&loz;", (char)9674);
//		HTMLDecode.put("&spades;", (char)9824);
//		HTMLDecode.put("&clubs;", (char)9827);
//		HTMLDecode.put("&hearts;", (char)9829);
//		HTMLDecode.put("&diams;", (char)9830);
	}
}
