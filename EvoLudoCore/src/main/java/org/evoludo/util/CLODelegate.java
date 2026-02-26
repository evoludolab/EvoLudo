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

/**
 * Interface to process command line arguments
 */
public interface CLODelegate {

	/**
	 * Parse string <code>arg</code> and set configurable parameters that correspond
	 * to this command line option. The delegate for options with
	 * {@code Argument.NONE} must implement this method.
	 * <p>
	 * <strong>Note:</strong> returning <code>false</code> triggers a warning about
	 * which command line option failed to correctly parse. If the parser can
	 * rectify the issue on the spot this is also acceptable. In that case the
	 * method should return {@code true} and possibly log the fact that parameters
	 * have been adjusted.
	 * 
	 * @param isSet {@code true} if option was set on command line
	 * @return {@code true} if parsing successful
	 */
	public default boolean parse(boolean isSet) {
		throw new UnsupportedOperationException("parse(boolean) not implemented");
	}

	/**
	 * Parse string <code>arg</code> and set configurable parameters that correspond
	 * to this command line option. The delegate for options with
	 * {@code Argument.REQUIRED} must implement this method.
	 * <p>
	 * <strong>Note:</strong> returning <code>false</code> triggers a warning about
	 * which command line option failed to correctly parse. If the parser can
	 * rectify the issue on the spot this is also acceptable. In that case the
	 * method should return {@code true} and possibly log the fact that
	 * parameters have been adjusted.
	 * 
	 * @param arg the argument for parsing by command line option
	 * @return {@code true} if parsing successful
	 */
	public default boolean parse(String arg) {
		throw new UnsupportedOperationException("parse(String) not implemented");
	}

	/**
	 * Parse string <code>arg</code> and set configurable parameters that correspond
	 * to this command line option. The delegate for options with
	 * {@code Argument.OPTIONAL} must implement this method.
	 * <p>
	 * <strong>Note:</strong> returning <code>false</code> triggers a warning about
	 * which command line option failed to correctly parse. If the parser can
	 * rectify the issue on the spot this is also acceptable. In that case the
	 * method should return {@code true} and possibly log the fact that
	 * parameters have been adjusted.
	 * 
	 * @param arg   the argument for parsing by command line option
	 * @param isSet {@code true} if arg is set on commandline
	 * @return {@code true} if parsing successful
	 */
	public default boolean parse(String arg, boolean isSet) {
		throw new UnsupportedOperationException("parse(String, boolean) not implemented");
	}

	/**
	 * If settings for option are not known upon initialization, an up-to-date
	 * description is requested when needed (e.g. if help is requested, typically
	 * using <code>--help</code> option).
	 * <p>
	 * <strong>Note:</strong> the description string may contain any UTF-8
	 * characters as well as HTML character entities. If necessary they will be
	 * escaped and converted to UTF-8 for display in XML documents.
	 *
	 * @return description of command line option.
	 */
	public default String getDescription() {
		return null;
	}

	/**
	 * Optional: position of key in the list of arguments. Used in help display.
	 * 
	 * @return the position of the key
	 */
	public default int getKeyPos() {
		return 0;
	}
}