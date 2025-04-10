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

package org.evoludo.simulator;

import com.google.gwt.core.client.GWT;
import com.google.gwt.i18n.client.Constants;

/**
 * The interface to provide access to the {@code Git.properties} file.
 * 
 * @author Christoph Hauert
 */
public interface Git extends Constants {

	/**
	 * 'Instantiate' the interface.
	 */
	public static final Git INSTANCE = GWT.create(Git.class);

	/**
	 * Returns the time of the last git commit formatted as a string.
	 * 
	 * @return the time of the last git commit
	 */
	@Key("git.build.time")
	@DefaultStringValue("")
	String gitDate();

	/**
	 * Returns the id (short SHA hash) of the last git commit formatted as a string.
	 * If there are local changes the id is marked as 'dirty' with an asterisk '*'.
	 * 
	 * @return the id of the last git commit
	 */
	@Key("git.commit.id.describe")
	@DefaultStringValue("unknown")
	String gitVersion();
}
