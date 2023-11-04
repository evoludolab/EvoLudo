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

import java.util.Iterator;

/**
 * Iterator over tags in <code>plist</code>-string.
 * 
 * @author Christoph Hauert
 */
public class PlistReader implements Iterator<PlistTag> {

	/**
	 * Number of lines read in <code>plist</code>-string.
	 */
	int read;

	/**
	 * Remainder of <code>plist</code>-string.
	 */
	String line;

	/**
	 * Name of root tag as specified in DOCTYPE declaration
	 */
	String root;

	/**
	 * <code>true</code> if done reading the <code>plist</code>-string.
	 */
	boolean done = false;

	/**
	 * Next tag.
	 */
	PlistTag tag;

	/**
	 * <code>true</code> if no processing has occurred yet. In particular this means
	 * that the header has not yet been parsed.
	 * 
	 * @see #parseHeader()
	 */
	boolean isVirgin = true;

// NOTE: reading an actual plist-file is not compatible with GWT. A subclass
// PlistReaderJRE could provide readers for a BufferedReader object.
//	
//	BufferedReader xmlbuf;
//
//	/**
//	 * Create reader for <code>plist</code>-string provided through 
//	 * <code>xmlbuf</code>.
//	 * 
//	 * @param xmlbuf buffer for reading <code>plist</code>-string
//	 * @throws NullPointerException if <code>null</code> buffer provided
//	 */
//	public PlistReader(BufferedReader xmlbuf) throws NullPointerException {
//		if( xmlbuf==null )
//			throw new NullPointerException("no plist as input stream provided.");
//		this.xmlbuf = xmlbuf;
//		read = 0;
//	}

	/**
	 * Create reader for <code>plist</code>-string.
	 * 
	 * @param string input <code>plist</code>-string
	 * @throws NullPointerException if <code>null</code> string provided
	 */
	public PlistReader(String string) throws NullPointerException {
		if (string == null)
			throw new NullPointerException("no plist as input provided.");
		line = string;
		read = 0;
	}

	/**
	 * Parse (and discard) header of <code>plist</code>-string. Performs only very
	 * rudimentary sanity checks.
	 */
	protected void parseHeader() {
		String name;
		String attributes;
		// read preamble
		int start = line.indexOf("<?xml");
		int end = line.indexOf("?>");
		if (start >= 0 && end > start) {
			// read attributes of xml tag
//			attributes = line.substring(start+"<?xml".length(), end).trim();
			// parse version
			line = line.substring(end + "?>".length()).trim();
		}
		start = line.indexOf("<!DOCTYPE");
		end = line.indexOf('>', "<!DOCTYPE".length());
		if (start >= 0 && end > start) {
			attributes = line.substring(start + "<!DOCTYPE".length(), end).trim();
			// parse doctype
			int idx = attributes.indexOf(' ');
			root = attributes.substring(0, idx);
			line = line.substring(end + 1).trim();
		}
		skipComments();
		name = line.substring(1, line.indexOf('>')).trim();
		// opening tags extend only to the first whitespace - after that there are
		// arguments
		int arg = name.indexOf(' ');
		if (arg > 0) {
			// read attributes of root tag
//			attributes = name.substring(arg);
			name = name.substring(0, arg);
		}
		if (root == null)
			root = name;
		else {
			// the root tag needs to be declared in the doctype tag
			if (!root.equals(name)) {
				// this is inconsistent... fail!
				done = true;
			}
		}
		isVirgin = false;
	}

	@Override
	public boolean hasNext() {
		String name, attributes = null, value = null;
		boolean closing = false;
		tag = null;
		if (done)
			return false;

		skipComments();

		// sanity check
		if (line.charAt(0) != '<') {
			done = true;
			return false;
		}

		// on first call read/parse header
		if (isVirgin)
			parseHeader();

		// check if it is a (self) closing tag
		int end = line.indexOf('>') + 1;
		int tagend = end - 1;
		if (line.charAt(tagend - 1) == '/') {
			closing = true;
			tagend--;
		}
		name = line.substring(1, tagend).trim();

		// is this an opening tag
		if (!closing && name.charAt(0) != '/') {
			// check attributes
			int arg = name.indexOf(' ');
			if (arg > 0) {
				attributes = name.substring(arg);
				name = name.substring(0, arg);
			}
			// check next closing tag
			int close = line.indexOf("</", end);
			String closingtag = "</" + name + ">";
			int closetag = line.indexOf(closingtag, close);
			if (close == closetag) {
				value = line.substring(end, close);
				end = close + closingtag.length();
			}
		}
		tag = new PlistTag(name, attributes, value);
		line = line.substring(end).trim();
//		if( line.indexOf("</"+root+">")>0 ) done = true;
		return true;
	}

	@Override
	public PlistTag next() {
		return tag;
	}

	/**
	 * Utility method to skip comments in <code>plist</code>-string.
	 */
	private void skipComments() {
		line = line.trim();
		while (line.startsWith("<!--")) {
			int end = line.indexOf("-->");
			line = line.substring(end + "-->".length()).trim();
		}
	}

	/**
	 * Return line number of last line read. This is mainly useful to provide some
	 * context in case an error or inconsistency is encountered.
	 * 
	 * @return last line read
	 */
	public int getLine() {
		return read;
	}

// NOTE: reading an actual plist-file is not compatible with GWT. This closes the
// buffer provided by a BufferedReader object and leaves us with a stale PlistReader 
// object that cannot easily be re-animated...
//	
//	/**
//	 * Close buffer that provided <code>plist</code>-string.
//	 */
//	public void close() {
//		try {
//			xmlbuf.close();
//		}
//		catch( IOException ioe ) {
//			// ignore exception - who cares if we can't close the stream?
//		}
//		done = true;
//		tag = null;
//		read = -1;
//	}
}
