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

/**
 * Entry in <code>plist</code>-string. Every <code>plist</code> comes in
 * <code>&lt;key&gt;</code> and <code>plist</code> element pairs.
 * 
 * @author Christoph Hauert
 */
public class PlistTag {
	/**
	 * Name of <code>&lt;key&gt;</code> tag.
	 */
	String tag;

	/**
	 * Value associated with <code>tag</code>.
	 */
	String value;

	/**
	 * Attributes of <code>tag</code> (or <code>null</code>).
	 */
	String attributes;

	/**
	 * Create a <code>plist</code> entry with <code>&lt;key&gt;</code> name
	 * <code>tag</code> without attributes associated with <code>value</code>.
	 * 
	 * @param tag   name of <code>&lt;key&gt;</code>
	 * @param value value of <code>plist</code> entry
	 */
	public PlistTag(String tag, String value) {
		this(tag, null, value);
	}

	/**
	 * Create a <code>plist</code> entry with <code>&lt;key&gt;</code> name
	 * <code>tag</code> and <code>attributes</code> associated with
	 * <code>value</code>.
	 * 
	 * @param tag        name of <code>&lt;key&gt;</code>
	 * @param attributes of <code>&lt;key&gt;</code>
	 * @param value      value of <code>plist</code> entry
	 */
	public PlistTag(String tag, String attributes, String value) {
		this.tag = tag;
		this.value = value;
		this.attributes = attributes;
	}

	/**
	 * Compares the <code>&lt;key&gt;</code> names of <code>anothertag</code> with
	 * this one.
	 * 
	 * @param anothertag another <code>plist</code> entry
	 * @return <code>true</code> if <code>this</code> and <code>anothertag</code>
	 *         have the same the <code>&lt;key&gt;</code>
	 */
	public boolean equals(String anothertag) {
		return tag.equals(anothertag);
	}

	/**
	 * @return <code>&lt;key&gt;</code> name of this <code>plist</code> entry.
	 */
	public String getTag() {
		return tag;
	}

	/**
	 * @return value of this <code>plist</code> entry.
	 */
	public String getValue() {
		return value;
	}

	/**
	 * @return <code>true</code> if <code>&lt;key&gt;</code> has attributes
	 */
	public boolean hasAttributes() {
		return (attributes != null);
	}

	/**
	 * @return attributes of <code>&lt;key&gt;</code> and <code>null</code> if
	 *         <code>&lt;key&gt;</code> has no attributes.
	 */
	public String getAttributes() {
		return attributes;
	}
}
