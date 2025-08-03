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

package org.evoludo.ui;

import java.util.logging.LogRecord;

import org.evoludo.util.XMLCoder;

/**
 * Formats LogRecords into XML/XHTML compliant text
 * <p>
 * <strong>Note:</strong> The GWT implementation of
 * {@link java.util.logging.Formatter} is simply <blockquote>
 * 
 * <pre>
 * public String formatMessage(LogRecord record) {
 * 	return format(record);
 * }
 * </pre>
 * 
 * </blockquote> which seems naturally prone to causing grief through circular
 * references... (admittedly also not clear how to do things better).
 * <p>
 * Thus, instead of extending
 * {@link com.google.gwt.logging.client.ConsoleLogHandler} to implement
 * formatting acceptable to XHTML, this extends
 * {@link com.google.gwt.logging.client.TextLogFormatter} instead.
 * 
 * @author Christoph Hauert
 *
 */
public class TextLogFormatter extends com.google.gwt.logging.client.TextLogFormatter {

	/**
	 * <code>false</code> if XML/XHTML compliant encoding of log messages desired.
	 */
	private boolean isHTML = true;

	/**
	 * Construct a new formatter for log messages without XML/XHTML encoding.
	 * 
	 * @param showStackTraces <code>true</code> to show stack traces
	 */
	public TextLogFormatter(boolean showStackTraces) {
		this(showStackTraces, false);
	}

	/**
	 * Construct a new formatter for log messages with XML/XHTML encoding, provided
	 * that <code>isHTML</code> is <code>false</code>.
	 * 
	 * @param showStackTraces <code>true</code> to show stack traces
	 * @param isHTML           <code>true</code> to use HTML encoding
	 */
	public TextLogFormatter(boolean showStackTraces, boolean isHTML) {
		super(showStackTraces);
		this.isHTML = isHTML;
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * If <code>isHTML</code> is <code>false</code> the returned string is XML/XHTML
	 * compliant.
	 */
	@Override
	public String format(LogRecord record) {
		String msg = super.format(record);
		if (isHTML)
			return msg;
		return XMLCoder.encode(msg);
	}
}
