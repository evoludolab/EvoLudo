//
//  TouchEventsGWT.js
//
//  The macOS patch TouchEvents.js to deal with touch events in Apple Books does 
//  not play nice with GWT. In particular, TouchEvents.wrapListener relies on a
//  test wether the listener is instanceof Function. However, during the transmission
//  from GWT's $wnd.addEventListener to the overridden Window.prototype.addEventListener
//  the listener mysteriously changes (both 'listener instanceof Function' and 
//  'listener instanceof Object' return false but 'typeof listener' is 'function' as
//  expected). The patch below addresses this particular case by explicitly wrapping
//  listener in a function before passing it on to TouchEvents.
//
//	GWT calls com.google.gwt.user.client.impl.DOMImplStandard.initEventSystem() early
//	on to setup all event handlers. To prevent the issues outline above we either need
//	to convince GWT that the listener remains an instance of 'Function' (all attempts
//	have failed so far) or make the macOS patch more lenient (approach below).
//
//  This patch should only be applied to ePubs in readers without touch support.
//

(function() {
var TouchEventsGWT = new Object();

TouchEventsGWT.patchPrototype = function(DOMType) {

	var __original__addEventListener = DOMType.prototype.addEventListener;
	DOMType.prototype.addEventListener = function(type, listener, useCapture) {
//		console.log("re-patched addEventListener for event '"+type+"'");
		if( listener instanceof Function ) {
			// nothing to do
			__original__addEventListener.call(this, type, listener, useCapture);
			return;
		}
		if( typeof listener === 'function' ) {
			// try to force listener to become an instance of Function
//			console.log("instanceof Object: "+(listener instanceof Object)+
//						", instanceof Function: "+(listener instanceof Function)+", typeof: "+(typeof listener));
			var funclistener = function (...args) { listener(...args); };
			__original__addEventListener.call(this, type, funclistener, useCapture);
			return;
		}
		// let's hope for the best...
		__original__addEventListener.call(this, type, listener, useCapture);
	}
}

// patch Node event listener methods to redirect touch events to mouse events.
TouchEventsGWT.patchPrototype(Node);
TouchEventsGWT.patchPrototype(Window);
})();
