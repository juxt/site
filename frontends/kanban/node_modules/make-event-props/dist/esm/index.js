// As defined on the list of supported events: https://reactjs.org/docs/events.html
export var clipboardEvents = ['onCopy', 'onCut', 'onPaste'];
export var compositionEvents = ['onCompositionEnd', 'onCompositionStart', 'onCompositionUpdate'];
export var keyboardEvents = ['onKeyDown', 'onKeyPress', 'onKeyUp'];
export var focusEvents = ['onFocus', 'onBlur'];
export var formEvents = ['onChange', 'onInput', 'onInvalid', 'onReset', 'onSubmit'];
export var genericEvents = ['onError', 'onLoad'];
export var mouseEvents = ['onClick', 'onContextMenu', 'onDoubleClick', 'onDrag', 'onDragEnd', 'onDragEnter', 'onDragExit', 'onDragLeave', 'onDragOver', 'onDragStart', 'onDrop', 'onMouseDown', 'onMouseEnter', 'onMouseLeave', 'onMouseMove', 'onMouseOut', 'onMouseOver', 'onMouseUp'];
export var pointerEvents = ['onPointerDown', 'onPointerMove', 'onPointerUp', 'onPointerCancel', 'onGotPointerCapture', 'onLostPointerCapture', 'onPointerEnter', 'onPointerLeave', 'onPointerOver', 'onPointerOut'];
export var selectionEvents = ['onSelect'];
export var touchEvents = ['onTouchCancel', 'onTouchEnd', 'onTouchMove', 'onTouchStart'];
export var uiEvents = ['onScroll'];
export var wheelEvents = ['onWheel'];
export var mediaEvents = ['onAbort', 'onCanPlay', 'onCanPlayThrough', 'onDurationChange', 'onEmptied', 'onEncrypted', 'onEnded', 'onError', 'onLoadedData', 'onLoadedMetadata', 'onLoadStart', 'onPause', 'onPlay', 'onPlaying', 'onProgress', 'onRateChange', 'onSeeked', 'onSeeking', 'onStalled', 'onSuspend', 'onTimeUpdate', 'onVolumeChange', 'onWaiting'];
export var imageEvents = ['onLoad', 'onError'];
export var animationEvents = ['onAnimationStart', 'onAnimationEnd', 'onAnimationIteration'];
export var transitionEvents = ['onTransitionEnd'];
export var otherEvents = ['onToggle'];
export var allEvents = [].concat(clipboardEvents, compositionEvents, keyboardEvents, focusEvents, formEvents, genericEvents, mouseEvents, pointerEvents, selectionEvents, touchEvents, uiEvents, wheelEvents, mediaEvents, imageEvents, animationEvents, transitionEvents, otherEvents);
/**
 * Returns an object with on-event callback props curried with provided args.
 * @param {Object} props Props passed to a component.
 * @param {Function=} getArgs A function that returns argument(s) on-event callbacks
 *   shall be curried with.
 */

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

export default makeEventProps;