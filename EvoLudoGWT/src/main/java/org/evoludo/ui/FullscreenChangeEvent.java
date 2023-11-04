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

package org.evoludo.ui;

import com.google.gwt.event.dom.client.DomEvent;

/**
 * Represents a native fullscreenchange event.
 * 
 * @author Christoph Hauert
 * 
 * @see <a href=
 *      "http://gwt-code-reviews.appspot.com/1248801/">http://gwt-code-reviews.appspot.com/1248801/</a>
 */
public class FullscreenChangeEvent extends DomEvent<FullscreenChangeHandler> {

	/**
	 * Name of fullscreenchange event with prefix added if needed.
	 */
	public static final String FULLSCREEN = _jsPrefix() + "fullscreenchange";

	/**
	 * Event type for fullscreenchange events. Represents the meta-data associated
	 * with this event.
	 */
	private static final Type<FullscreenChangeHandler> TYPE = new Type<FullscreenChangeHandler>(FULLSCREEN,
			new FullscreenChangeEvent());

	/**
	 * Gets the event type associated with fullscreenchange events.
	 *
	 * @return the handler type
	 */
	public static Type<FullscreenChangeHandler> getType() {
		return TYPE;
	}

	/**
	 * Protected constructor, use
	 * {@link DomEvent#fireNativeEvent(com.google.gwt.dom.client.NativeEvent, com.google.gwt.event.shared.HasHandlers)}
	 * to fire fullscreenchange events.
	 */
	protected FullscreenChangeEvent() {
	}

// NOTE: apparently it is not possible to simply add custom methods here...
//	public boolean isFullscreen() {
//		return _isFullscreen();
//	}
//
//	private final static native boolean _isFullscreen() 
//	/*-{
//		if( ($doc.fullscreenElement!=null) || ($doc.mozFullScreenElement!=null) || ($doc.webkitFullscreenElement!=null) || ($doc.msFullscreenElement!=null) )
//			return true;
//		// NOTE: Document.fullscreen et al. are obsolete - last resort
//		return $doc.fullscreen || $doc.mozFullScreen || $doc.webkitIsFullScreen ? true : false;
//	}-*/;

	@Override
	public final Type<FullscreenChangeHandler> getAssociatedType() {
		return TYPE;
	}

	@Override
	protected void dispatch(FullscreenChangeHandler handler) {
		handler.onFullscreenChange(this);
	}

	/**
	 * Determine browser prefix, if any, to use the correct identifier for capturing
	 * the fullscreen change event.
	 * 
	 * @return browser specific javascript prefix
	 */
	private static native String _jsPrefix()
	/*-{
		var styles = $wnd.getComputedStyle($doc.documentElement, '');
		return (Array.prototype.slice.call(styles).join('').match(
				/-(moz|webkit|ms)-/) || (styles.OLink === '' && [ '', 'o' ]))[1];
	}-*/;
}
