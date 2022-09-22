// As defined on the list of supported events: https://reactjs.org/docs/events.html
export const clipboardEvents = ['onCopy', 'onCut', 'onPaste'];
export const compositionEvents = ['onCompositionEnd', 'onCompositionStart', 'onCompositionUpdate'];
export const keyboardEvents = ['onKeyDown', 'onKeyPress', 'onKeyUp'];
export const focusEvents = ['onFocus', 'onBlur'];
export const formEvents = ['onChange', 'onInput', 'onInvalid', 'onReset', 'onSubmit'];
export const genericEvents = ['onError', 'onLoad'];
export const mouseEvents = ['onClick', 'onContextMenu', 'onDoubleClick', 'onDrag', 'onDragEnd', 'onDragEnter', 'onDragExit', 'onDragLeave', 'onDragOver', 'onDragStart', 'onDrop', 'onMouseDown', 'onMouseEnter', 'onMouseLeave', 'onMouseMove', 'onMouseOut', 'onMouseOver', 'onMouseUp'];
export const pointerEvents = ['onPointerDown', 'onPointerMove', 'onPointerUp', 'onPointerCancel', 'onGotPointerCapture', 'onLostPointerCapture', 'onPointerEnter', 'onPointerLeave', 'onPointerOver', 'onPointerOut'];
export const selectionEvents = ['onSelect'];
export const touchEvents = ['onTouchCancel', 'onTouchEnd', 'onTouchMove', 'onTouchStart'];
export const uiEvents = ['onScroll'];
export const wheelEvents = ['onWheel'];
export const mediaEvents = ['onAbort', 'onCanPlay', 'onCanPlayThrough', 'onDurationChange', 'onEmptied', 'onEncrypted', 'onEnded', 'onError', 'onLoadedData', 'onLoadedMetadata', 'onLoadStart', 'onPause', 'onPlay', 'onPlaying', 'onProgress', 'onRateChange', 'onSeeked', 'onSeeking', 'onStalled', 'onSuspend', 'onTimeUpdate', 'onVolumeChange', 'onWaiting'];
export const imageEvents = ['onLoad', 'onError'];
export const animationEvents = ['onAnimationStart', 'onAnimationEnd', 'onAnimationIteration'];
export const transitionEvents = ['onTransitionEnd'];
export const otherEvents = ['onToggle'];

export const allEvents = [
  ...clipboardEvents, ...compositionEvents, ...keyboardEvents, ...focusEvents, ...formEvents,
  ...genericEvents, ...mouseEvents, ...pointerEvents, ...selectionEvents, ...touchEvents,
  ...uiEvents, ...wheelEvents, ...mediaEvents, ...imageEvents, ...animationEvents,
  ...transitionEvents, ...otherEvents,
];

/**
 * Returns an object with on-event callback props curried with provided args.
 * @param {Object} props Props passed to a component.
 * @param {Function=} getArgs A function that returns argument(s) on-event callbacks
 *   shall be curried with.
 */
const makeEventProps = (props, getArgs) => {
  const eventProps = {};

  allEvents.forEach((eventName) => {
    if (!(eventName in props)) {
      return;
    }

    if (!getArgs) {
      eventProps[eventName] = props[eventName];
      return;
    }

    eventProps[eventName] = (event) => props[eventName](event, getArgs(eventName));
  });

  return eventProps;
};

export default makeEventProps;
