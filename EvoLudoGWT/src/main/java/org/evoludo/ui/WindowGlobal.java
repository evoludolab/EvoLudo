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

package org.evoludo.ui;

import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsType;

/**
 * JSInterop bridge exposing the global {@code window} object.
 */
@JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "window")
public class WindowGlobal {

	/**
	 * Utility class – prevent instantiation.
	 */
	private WindowGlobal() {
	}

	/**
	 * Register a JS event listener on the global window object.
	 * 
	 * @param type     event type
	 * @param listener callback invoked for events
	 */
	public static native void addEventListener(String type, EventListener listener);
}
