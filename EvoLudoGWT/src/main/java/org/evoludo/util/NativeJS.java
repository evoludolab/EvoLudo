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

package org.evoludo.util;

import org.evoludo.EvoLudoWeb;
import org.evoludo.graphics.AbstractGraph.MyContext2d;
import org.evoludo.ui.FullscreenChangeHandler;

import com.google.gwt.canvas.dom.client.Context2d;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NodeList;

/**
 * Collection of native javascript methods.
 */
public class NativeJS {

	/**
	 * Private default constructor to ensure non-instantiability.
	 */
	private NativeJS() {
	}

	/**
	 * JSNI method: Display javascript alert panel. Very intrusive. Use for
	 * debugging only.
	 *
	 * @param msg the message to display
	 */
	public static native void alert(String msg) /*-{
		$wnd.alert(msg);
	}-*/;

	/**
	 * JSNI method: log message into JS console.
	 *
	 * @param msg the message to log
	 */
	public static native void log(String msg) /*-{
		console.log(msg);
	}-*/;

	/**
	 * JSNI method: base-64 encoding of string.
	 *
	 * @param b string to encode
	 * @return encoded string
	 */
	public static native String b64encode(String b) /*-{
		return window.btoa(b);
	}-*/;

	/**
	 * JSNI method: decoding of base-64 encoded string.
	 *
	 * @param a string to decode
	 * @return decoded string
	 */
	public static native String b64decode(String a) /*-{
		return window.atob(a);
	}-*/;

	/**
	 * JSNI method: expose convenient javascript method to obtain a list of elements
	 * in the DOM that match the selection criterion <code>selector</code>.
	 *
	 * @param selector the criterion for selecting elements in DOM
	 * @return list of elements that match <code>selector</code>
	 */
	public static final native NodeList<Element> querySelectorAll(String selector)
	/*-{
		return $doc.querySelectorAll(selector);
	}-*/;

	/**
	 * JSNI method: check if {@code element} is active.
	 * 
	 * @param element the element to check
	 * @return {@code true} if {@code element} is active
	 */
	public static final native boolean isElementActive(Element element)
	/*-{
		return ($doc.activeElement === element);
	}-*/;

	/**
	 * JSNI method: return the currently focused element in the document.
	 * 
	 * @return the active element or {@code null} if none
	 */
	public static final native Element getActiveElement()
	/*-{
		return $doc.activeElement || null;
	}-*/;

	/**
	 * JSNI method: focus on {@code element}.
	 * 
	 * @param element the element to focus on
	 */
	public static final native void focusOn(Element element)
	/*-{
		element.focus();
	}-*/;

	/**
	 * JSNI method: Check if document has focus. This is important to avoid showing
	 * tooltips when the browser window is not in focus.
	 * 
	 * @return <code>true</code> if document has focus
	 */
	public static final native boolean hasFocus()
	/*-{
		return $doc.hasFocus();
	}-*/;

	/**
	 * JSNI method: return the pixel ratio of the current device. This is intended
	 * to prevent distortions on the <code>canvas</code> objects of the data views.
	 *
	 * @return the pixel ratio of device
	 */
	public static final native int getDevicePixelRatio()
	/*-{
		return $wnd.devicePixelRatio || 1;
	}-*/;

	/**
	 * JSNI method: Check if fullscreen mode is supported.
	 * 
	 * @return {@code true} if fullscreen supported
	 */
	public static native boolean isFullscreenSupported()
	/*-{
		return $doc.fullscreenEnabled || $doc.mozFullScreenEnabled
			|| $doc.webkitFullscreenEnabled || $doc.msFullscreenEnabled ? true
			: false;
	}-*/;

	/**
	 * JSNI method: Request fullscreen mode for the element {@code ele}.
	 * 
	 * @param ele the element to request fullscreen mode for
	 */
	public static native void requestFullscreen(Element ele)
	/*-{
		if (ele.requestFullscreen) {
			ele.requestFullscreen();
			// using promise:
			//			ele.requestFullscreen().then(console.log("request honoured! width="+ele.scrollWidth));
		} else if (ele.msRequestFullscreen) {
			ele.msRequestFullscreen();
		} else if (ele.mozRequestFullScreen) {
			ele.mozRequestFullScreen();
		} else if (ele.webkitRequestFullScreen) {
			ele.webkitRequestFullScreen();
		}
	}-*/;

	/**
	 * JSNI method: Check if the document is in fullscreen mode.
	 * 
	 * @return {@code true} if the document is in fullscreen mode
	 */
	public static final native boolean isFullscreen()
	/*-{
		if (($doc.fullscreenElement != null)
				|| ($doc.mozFullScreenElement != null)
				|| ($doc.webkitFullscreenElement != null)
				|| ($doc.msFullscreenElement != null))
			return true;
		// NOTE: Document.fullscreen et al. are obsolete - last resort
		return $doc.fullscreen || $doc.mozFullScreen || $doc.webkitIsFullScreen ? true
				: false;
	}-*/;

	/**
	 * JSNI method: Gets fullscreen element if in fullscreen mode or
	 * <code>null</code> if not in fullscreen or fullscreen not supported by web
	 * browser.
	 *
	 * @return fullscreen element or <code>null</code>
	 */
	public static final native Element getFullscreenElement()
	/*-{
		if ($doc.fullscreenElement != null)
			return $doc.fullscreenElement;
		if ($doc.mozFullScreenElement != null)
			return $doc.mozFullScreenElement;
		if ($doc.webkitFullscreenElement != null)
			return $doc.webkitFullscreenElement;
		if ($doc.msFullscreenElement != null)
			return $doc.msFullscreenElement;
		return null;
	}-*/;

	/**
	 * JSNI method: Exit fullscreen mode.
	 */
	public static native void exitFullscreen()
	/*-{
		if ($doc.exitFullscreen) {
			$doc.exitFullscreen();
		} else if ($doc.msExitFullscreen) {
			$doc.msExitFullscreen();
		} else if ($doc.mozCancelFullScreen) {
			$doc.mozCancelFullScreen();
		} else if ($doc.webkitCancelFullScreen) {
			$doc.webkitCancelFullScreen();
		}
	}-*/;

	/**
	 * JSNI method: Determine name of the fullscreen change event in current web
	 * browser.
	 * <p>
	 * <strong>Note:</strong> Chrome implements both <code>fullscreenchange</code>
	 * and <code>webkitfullscreenchange</code> but with slightly different behaviour
	 * (neither identical to Safari). <code>fullscreenchange</code> at least works
	 * for a single graph and hence give it precedence. For Firefox scaling/resizing
	 * issues remain as well as for Chrome with multiple graphs.
	 *
	 * @return web browser specific fullscreen change event name or
	 *         <code>null</code> if Fullscreen API not implemented.
	 */
	public static native String fullscreenChangeEventName()
	/*-{
		if ($doc.onfullscreenchange !== undefined)
			return "fullscreenchange";
		if ($doc.onwebkitfullscreenchange !== undefined)
			return "webkitfullscreenchange";
		if ($doc.onmozfullscreenchange !== undefined)
			return "mozfullscreenchange";
		if ($doc.onmsfullscreenchange !== undefined)
			return "msfullscreenchange";
		return null;
	}-*/;

	/**
	 * JSNI method: Add a fullscreen change handler.
	 * 
	 * @evoludo.impl The JSNI routine works reasonably well with Safari but not with
	 *               all other browsers because aspects of the fullscreen API are
	 *               interpreted differently, see
	 *               {@link #fullscreenChangeEventName()}.
	 * 
	 * @param eventname the name of the fullscreen change event
	 * @param handler   the handler to add
	 */
	public static final native void addFullscreenChangeHandler(String eventname, FullscreenChangeHandler handler)
	/*-{
		$doc.addEventListener(
			eventname,
			function(event) {
				handler.@org.evoludo.ui.FullscreenChangeHandler::onFullscreenChange(Lorg/evoludo/ui/FullscreenChangeEvent;)(event);
			}, true);
	}-*/;

	/**
	 * JSNI method: Remove the fullscreen change handler.
	 * 
	 * @evoludo.impl The handler function needs to be specified again when removing
	 *               the listener... Because we don't know how to store the handler
	 *               returned by the JSNI method
	 *               {@code _addFullscreenChangeHandler(String, FullscreenChangeHandler)}
	 *               it must be exact copy of handler specification there.
	 * 
	 * @param eventname the name of the fullscreen change event
	 * @param handler   the handler to add
	 */
	public static final native void removeFullscreenChangeHandler(String eventname, FullscreenChangeHandler handler)
	/*-{
		$doc.removeEventListener(
			eventname,
			function(event) {
				handler.@org.evoludo.ui.FullscreenChangeHandler::onFullscreenChange(Lorg/evoludo/ui/FullscreenChangeEvent;)(event);
			}, true);
	}-*/;

	/**
	 * JSNI method: check whether WebGL and hence 3D graphics are supported.
	 * <p>
	 * <strong>Note:</strong> asssumes that <code>canvas</code> is supported
	 *
	 * @return <code>true</code> if WebGL is supported
	 */
	public static native boolean isWebGLSupported() /*-{
		try {
			var canvas = $doc.createElement('canvas');
			// must explicitly check for null (otherwise this may return an object
			// violating the signature of the _isWebGLSupported() method)
			return (!!$wnd.WebGLRenderingContext
					&& (canvas.getContext('webgl') || 
						canvas.getContext('experimental-webgl'))) != null;
		} catch (e) {
			return false;
		}
	}-*/;

	/**
	 * JSNI method: check whether a single plist files was dropped.
	 * <p>
	 * <strong>Note:</strong> This check needs to be done in native javascript
	 * because the DataTransfer object returned by the <code>onDrop</code> handler
	 * cannot be read using GWT routines.
	 *
	 * @param dataTransfer list of dropped file(s)
	 * @return <code>true</code> if the dropped file looks promising and ok for
	 *         further processing
	 */
	public static final native boolean isValidDnD(JavaScriptObject dataTransfer) /*-{
		var files = dataTransfer.files;
		if (files.length != 1)
			return false;
		var fname = files[0].name;
		return fname.includes(".plist");
	}-*/;

	/**
	 * JSNI method: the HTML5 File API enables reading of files. Take advantage of
	 * functionality to read contents of dropped file.
	 * <p>
	 * <strong>Note:</strong> {@link #isValidDnD(JavaScriptObject)} should be called
	 * first to ensure that only a single 'plist' file was dropped.
	 *
	 * @param dataTransfer the list of dropped file(s)
	 * @param gui          the user interface that processes dropped file
	 */
	public static final native void handleDnD(JavaScriptObject dataTransfer, EvoLudoWeb gui) /*-{
		var files = dataTransfer.files;
		if (files.length != 1)
			return;
		var file = files[0];
		var restoreState = $entry(function(name, content) {
			gui.@org.evoludo.EvoLudoWeb::restoreFromString(Ljava/lang/String;Ljava/lang/String;)(name, content);
		});
		// first try as compressed data
		JSZip.loadAsync(file)
			.then(function(zip) {
				zip.forEach(function (relativePath, zipEntry) {
					zip.file(zipEntry.name).async("string").then(function (data) {
						restoreState(zipEntry.name, data);
					});
				});
			}, function (e) {
				// file is not compressed; try as plain plist file
				var reader = new FileReader();
				reader.onload = function(e) {
					restoreState(file.name, e.target.result);
				}
				reader.readAsText(file);
			});
	}-*/;

	/**
	 * JSNI method: opens javascript file chooser and attempts to restore state from
	 * selected file
	 *
	 * @param gui the user interface that processes the contents of selected file
	 */
	public static final native void restoreFromFile(EvoLudoWeb gui) /*-{
		var input = $doc.createElement('input');
		input.setAttribute('type', 'file');
		input.onchange = function(e) {
			var files = e.target.files;
			if (files.length != 1)
				return;
			var file = files[0];
			var reader = new FileReader();
			reader.onload = function(e) {
				gui.@org.evoludo.EvoLudoWeb::restoreFromString(Ljava/lang/String;Ljava/lang/String;)(file.name, e.target.result);
			}
			reader.readAsText(file);
		}
		input.click();
	}-*/;

	/**
	 * JSNI method: Create a SVG context for exporting the view.
	 * <p>
	 * <strong>Note:</strong> Requires that the {@code Canvas2SVG.js} script has
	 * been injected, e.g. using something like
	 * {@code ScriptInjector.fromString(Resources.INSTANCE.canvas2SVG().getText()).inject();}
	 * 
	 * @param width  the width of the context
	 * @param height the height of the context
	 * @return the SVG context
	 */
	public static native MyContext2d createSVGContext(int width, int height)
	/*-{
		return C2S(width, height);
	}-*/;

	/**
	 * JSNI method: Export the SVG context.
	 * 
	 * @param ctx the SVG context to export
	 */
	public static native void exportSVG(Context2d ctx)
	/*-{
		@org.evoludo.util.NativeJS::export(Ljava/lang/String;Ljava/lang/String;)("data:image/svg+xml;charset=utf-8,"+
			ctx.getSerializedSvg(true), "evoludo.svg");
	}-*/;

	/**
	 * JSNI method: encourage browser to download {@code dataURL} as a file named
	 * {@code filename}.
	 * <p>
	 * <strong>Note:</strong> <code>&#x23;</code> characters cause trouble in data
	 * URL. Escape them with <code>%23</code>.
	 *
	 * @param dataURL  the file content encoded as data URL
	 * @param filename the name of the downloaded file
	 */
	public static native void export(String dataURL, String filename) /*-{
		var elem = $doc.createElement('a');
		// character '#' makes trouble in data URL's - escape
		elem.href = dataURL.replace(/#/g, "%23");
		elem.download = filename;
		$doc.body.appendChild(elem);
		elem.click();
		$doc.body.removeChild(elem);
	}-*/;

	/**
	 * JSNI method: Test whether loaded from an HTML document.
	 *
	 * @return <code>true</code> if HTML document
	 */
	public static final native boolean isHTML() /*-{
		return ($doc.contentType.includes("text/html"));
	}-*/;

	/**
	 * JSNI method: Check if graph is displayed in an ePub reading system.
	 * 
	 * @return <code>true</code> if ePub reading system.
	 */
	public static final native boolean isEPub()
	/*-{
		var epub = $wnd.navigator.epubReadingSystem
				|| window.navigator.epubReadingSystem
				|| navigator.epubReadingSystem;
		return (epub != null);
	}-*/;

	/**
	 * JSNI method: Retrieve string identifying the ePub reading system:
	 * <ul>
	 * <li>web browser: null</li>
	 * <li>iBooks: iBooks, Apple Books</li>
	 * <li>Adobe Digital Editions 4.5.8 (macOS): RMSDK</li>
	 * <li>Readium: no JS (and no display equations!)</li>
	 * <li>TEA Ebook 1.5.0 (macOS): no JS (and issues with MathML)</li>
	 * <li>calibre: calibre-desktop (supports JS and MathML but appears
	 * unstable)</li>
	 * </ul>
	 * <strong>Note:</strong> nonlinear content in Apple Books on iOS does
	 * <strong>not</strong> report as an ePub reading system (at least on the iPad).
	 * Apple Books on macOS behaves as expected.
	 *
	 * @return identification string of ePub reading system or <code>null</code> if
	 *         no reading system or reading system unknown
	 */
	public static final native String getEPubReader()
	/*-{
		var epub = $wnd.navigator.epubReadingSystem
				|| window.navigator.epubReadingSystem
				|| navigator.epubReadingSystem;
		if (!epub)
			return null;
		return epub.name;
	}-*/;

	/**
	 * JSNI method: test if ePub reader supports <code>feature</code>. in ePub 3
	 * possible features are:
	 * <dl>
	 * <dt>dom-manipulation</dt>
	 * <dd>Scripts MAY make structural changes to the document’s DOM (applies to
	 * spine-level scripting only).</dd>
	 * <dt>layout-changes</dt>
	 * <dd>Scripts MAY modify attributes and CSS styles that affect content layout
	 * (applies to spine-level scripting only).</dd>
	 * <dt>touch-events</dt>
	 * <dd>The device supports touch events and the Reading System passes touch
	 * events to the content.</dd>
	 * <dt>mouse-events</dt>
	 * <dd>The device supports mouse events and the Reading System passes mouse
	 * events to the content.</dd>
	 * <dt>keyboard-events</dt>
	 * <dd>The device supports keyboard events and the Reading System passes
	 * keyboard events to the content.</dd>
	 * <dt>spine-scripting</dt>
	 * <dd>Indicates whether the Reading System supports spine-level scripting
	 * (e.g., so a container-constrained script can determine whether any actions
	 * that depend on scripting support in a Top-level Content Document have any
	 * chance of success before attempting them).</dd>
	 * </dl>
	 *
	 * @see <a href=
	 *      "https://www.w3.org/publishing/epub3/epub-contentdocs.html#app-epubReadingSystem">
	 *      https://www.w3.org/publishing/epub3/epub-contentdocs.html#app-epubReadingSystem</a>
	 *
	 * @param feature the ePub feature to test
	 * @return <code>true</code> if feature supported
	 */
	public static final native boolean ePubReaderHasFeature(String feature)
	/*-{
		var epub = $wnd.navigator.epubReadingSystem;
		if (!epub)
			return false;
		return $wnd.navigator.epubReadingSystem.hasFeature(feature);
	}-*/;

	/**
	 * JSNI method: Check if execution environment supports keyboard events.
	 * 
	 * @return <code>true</code> if keyboard events are supported
	 */
	public static final native boolean hasKeys() /*-{
		return true == ("onkeydown" in $wnd);
	}-*/;

	/**
	 * JSNI method: Check if execution environment supports mouse events.
	 * 
	 * @return <code>true</code> if mouse events are supported
	 */
	public static final native boolean hasMouse() /*-{
		return true == ("onmousedown" in $wnd);
	}-*/;

	/**
	 * JSNI method: Check if execution environment supports touch events.
	 * 
	 * @return <code>true</code> if touch events are supported
	 */
	public static final native boolean hasTouch() /*-{
		return true == ("ontouchstart" in $wnd || $wnd.DocumentTouch
				&& $doc instanceof DocumentTouch);
	}-*/;

	// /*
	// * some potentially useful (but currently unused) javascript snippets
	// */

	// /**
	// * open new window displaying data url
	// *
	// * <p>unfortunately an increasing number of browsers deem data url's unsafe
	// and ignore this call.
	// * at present (october 2019), works as expected in Safari but not Google
	// Chrome or Firefox.
	// *
	// * @param dataURL
	// */
	// protected static native void _export(String dataURL)
	// /*-{
	// $wnd.open(dataURL, '_blank');
	// }-*/;

	// public static native String getComputedStyle(Element element, String style)
	// /*-{
	// return $wnd.getComputedStyle(element, null).getPropertyValue(style);
	// }-*/;

	// public static native int getBoundingHeight(Element element)
	// /*-{
	// return element.getBoundingClientRect().height;
	// }-*/;

	// public static native JsArray<Node> getAttributes(Element elem) /*-{
	// return elem.attributes;
	// }-*/;

	// public final static native int getScreenWidth()
	// /*-{
	// return $wnd.screen.width;
	// }-*/;

	// public final static native int getScreenHeight()
	// /*-{
	// return $wnd.screen.height;
	// }-*/;

	// protected static native String dumpStyle(Element element)
	// /*-{
	// return Object.keys(element.style).map(function(k) { return k + ":" +
	// element.style[k] }).join(", ");
	// }-*/;

	// public final static native String getLocalStorage(String key)
	// /*-{
	// return localStorage.getItem(key);
	// }-*/;

	// public final static native String setLocalStorage(String key, String value)
	// /*-{
	// return localStorage.setItem(key, value);
	// }-*/;

	// /**
	// * determine browser prefix for event handlers and features
	// *
	// * @return browser specific javascript prefix
	// */
	// private static native String _jsPrefix()
	// /*-{
	// var styles = $wnd.getComputedStyle($doc.documentElement, '');
	// return (Array.prototype.slice
	// .call(styles).join('')
	// .match(/-(moz|webkit|ms)-/) || (styles.OLink === '' && ['', 'o']))[1];
	// }-*/;
}
