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

package org.evoludo.simulator;

import com.google.gwt.core.client.GWT;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.resources.client.DataResource;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.resources.client.ImageResource.ImageOptions;
import com.google.gwt.resources.client.ImageResource.RepeatStyle;
import com.google.gwt.resources.client.TextResource;
import com.google.gwt.resources.client.DataResource.MimeType;

/**
 * The interface to provide access to GWT specific resource files.
 * 
 * @author Christoph Hauert
 */
public interface Resources extends ClientBundle {

	/**
	 * 'Instantiate' the interface.
	 */
	public static final Resources INSTANCE = GWT.create(Resources.class);

	/**
	 * Get the GSS style file for the GWT EvoLudo GUI.
	 * 
	 * @return the CSS resource
	 */
	@Source("resources/EvoLudo.gss")
	@CssResource.NotStrict
	public CssResource css();

	/**
	 * Get the JavaScript file for exporting canvas elements to SVG.
	 * 
	 * @return the JavaScript file
	 */
	@Source("resources/Canvas2SVG.js")
	public TextResource canvas2SVG();

	/**
	 * Get the JavaScript file for patching the macOS patch for Apple Books (iBooks)
	 * on devices without touch.
	 * 
	 * @return the JavaScript file
	 */
	@Source("resources/TouchEventsGWT.js")
	public TextResource touchEventsGWTHandler();

	/**
	 * Get the JavaScript file for accepting compressed files for restoring states
	 * through drag'n'drop.
	 * 
	 * @return the JavaScript file
	 */
	@Source("resources/jszip.js")
	public TextResource zip();

	/**
	 * Get the EvoLudo logo.
	 * 
	 * @return the image resource
	 */
	@Source("resources/logo.small.png")
	@ImageOptions(repeatStyle = RepeatStyle.None)
	ImageResource logoSmall();

	/**
	 * Get the rotate cursor.
	 * 
	 * @return the image resource
	 */
	@Source("resources/cursorRotate.svg")
	@MimeType("image/svg+xml")
	DataResource cursorRotate();

	/**
	 * Get the horizontal border for GWT GUI elements.
	 * 
	 * @return the image resource
	 */
	@Source("resources/hborder.png")
	@ImageOptions(repeatStyle = RepeatStyle.Horizontal)
	ImageResource hBorder();

	/**
	 * Get the vertical border for GWT GUI elements.
	 * 
	 * @return the image resource
	 */
	@Source("resources/vborder.png")
	@ImageOptions(repeatStyle = RepeatStyle.Vertical)
	ImageResource vBorder();

	/**
	 * Get decorations for GWT GUI elements.
	 * 
	 * @return the image resource
	 */
	@Source("resources/circles.png")
	@ImageOptions(repeatStyle = RepeatStyle.None)
	ImageResource circles();

	/**
	 * Get decorations for GWT GUI elements.
	 * 
	 * @return the image resource
	 */
	@Source("resources/corner.png")
	@ImageOptions(repeatStyle = RepeatStyle.None)
	ImageResource corner();

	/**
	 * Get the GWT sprite for {@code VerticalSplitPanel} GUI elements.
	 * 
	 * @return the image resource
	 */
	@Source("resources/thumb_horz.png")
	@ImageOptions(repeatStyle = RepeatStyle.None)
	ImageResource thumbHorizontal();

	/**
	 * Get the GWT sprite for {@code HorizontalSplitPanel} GUI elements.
	 * 
	 * @return the image resource
	 */
	@Source("resources/thumb_vertical.png")
	@ImageOptions(repeatStyle = RepeatStyle.None)
	ImageResource thumbVertical();
}
