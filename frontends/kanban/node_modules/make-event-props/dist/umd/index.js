"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports["default"] = exports.allEvents = exports.otherEvents = exports.transitionEvents = exports.animationEvents = exports.imageEvents = exports.mediaEvents = exports.wheelEvents = exports.uiEvents = exports.touchEvents = exports.selectionEvents = exports.pointerEvents = exports.mouseEvents = exports.genericEvents = exports.formEvents = exports.focusEvents = exports.keyboardEvents = exports.compositionEvents = exports.clipboardEvents = void 0;
// As defined on the list of supported events: https://reactjs.org/docs/events.html
var clipboardEvents = ['onCopy', 'onCut', 'onPaste'];
exports.clipboardEvents = clipboardEvents;
var compositionEvents = ['onCompositionEnd', 'onCompositionStart', 'onCompositionUpdate'];
exports.compositionEvents = compositionEvents;
var keyboardEvents = ['onKeyDown', 'onKeyPress', 'onKeyUp'];
exports.keyboardEvents = keyboardEvents;
var focusEvents = ['onFocus', 'onBlur'];
exports.focusEvents = focusEvents;
var formEvents = ['onChange', 'onInput', 'onInvalid', 'onReset', 'onSubmit'];
exports.formEvents = formEvents;
var genericEvents = ['onError', 'onLoad'];
exports.genericEvents = genericEvents;
var mouseEvents = ['onClick', 'onContextMenu', 'onDoubleClick', 'onDrag', 'onDragEnd', 'onDragEnter', 'onDragExit', 'onDragLeave', 'onDragOver', 'onDragStart', 'onDrop', 'onMouseDown', 'onMouseEnter', 'onMouseLeave', 'onMouseMove', 'onMouseOut', 'onMouseOver', 'onMouseUp'];
exports.mouseEvents = mouseEvents;
var pointerEvents = ['onPointerDown', 'onPointerMove', 'onPointerUp', 'onPointerCancel', 'onGotPointerCapture', 'onLostPointerCapture', 'onPointerEnter', 'onPointerLeave', 'onPointerOver', 'onPointerOut'];
exports.pointerEvents = pointerEvents;
var selectionEvents = ['onSelect'];
exports.selectionEvents = selectionEvents;
var touchEvents = ['onTouchCancel', 'onTouchEnd', 'onTouchMove', 'onTouchStart'];
exports.touchEvents = touchEvents;
var uiEvents = ['onScroll'];
exports.uiEvents = uiEvents;
var wheelEvents = ['onWheel'];
exports.wheelEvents = wheelEvents;
var mediaEvents = ['onAbort', 'onCanPlay', 'onCanPlayThrough', 'onDurationChange', 'onEmptied', 'onEncrypted', 'onEnded', 'onError', 'onLoadedData', 'onLoadedMetadata', 'onLoadStart', 'onPause', 'onPlay', 'onPlaying', 'onProgress', 'onRateChange', 'onSeeked', 'onSeeking', 'onStalled', 'onSuspend', 'onTimeUpdate', 'onVolumeChange', 'onWaiting'];
exports.mediaEvents = mediaEvents;
var imageEvents = ['onLoad', 'onError'];
exports.imageEvents = imageEvents;
var animationEvents = ['onAnimationStart', 'onAnimationEnd', 'onAnimationIteration'];
exports.animationEvents = animationEvents;
var transitionEvents = ['onTransitionEnd'];
exports.transitionEvents = transitionEvents;
var otherEvents = ['onToggle'];
exports.otherEvents = otherEvents;
var allEvents = [].concat(clipboardEvents, compositionEvents, keyboardEvents, focusEvents, formEvents, genericEvents, mouseEvents, pointerEvents, selectionEvents, touchEvents, uiEvents, wheelEvents, mediaEvents, imageEvents, animationEvents, transitionEvents, otherEvents);
/**
 * Returns an object with on-event callback props curried with provided args.
 * @param {Object} props Props passed to a component.
 * @param {Function=} getArgs A function that returns argument(s) on-event callbacks
 *   shall be curried with.
 */

exports.allEvents = allEvents;

var makeEventProps = function makeEventProps(props, getArgs) {
  var eventProps = {};
  allEvents.forEach(function (eventName) {
    if (!(eventName in props)) {
      return;
    }

    if (!getArgs) {
      eventProps[eventName] = props[eventName];
      return;
    }

    eventProps[eventName] = function (event) {
      return props[eventName](event, getArgs(eventName));
    };
  });
  return eventProps;
};

var _default = makeEventProps;
exports["default"] = _default;