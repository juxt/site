'use strict';

Object.defineProperty(exports, '__esModule', { value: true });

var react = require('react');
var useReducedMotion = require('../use-reduced-motion/use-reduced-motion.js');
var useWindowEvent = require('../use-window-event/use-window-event.js');
var easeInOutQuad = require('./utils/ease-in-out-quad.js');
var getRelativePosition = require('./utils/get-relative-position.js');
var getScrollStart = require('./utils/get-scroll-start.js');
var setScrollParam = require('./utils/set-scroll-param.js');

function useScrollIntoView({
  duration = 1250,
  axis = "y",
  onScrollFinish,
  easing = easeInOutQuad.easeInOutQuad,
  offset = 0,
  cancelable = true,
  isList = false
} = {}) {
  const frameID = react.useRef(0);
  const startTime = react.useRef(0);
  const shouldStop = react.useRef(false);
  const scrollableRef = react.useRef(null);
  const targetRef = react.useRef(null);
  const reducedMotion = useReducedMotion.useReducedMotion();
  const cancel = () => {
    if (frameID.current) {
      cancelAnimationFrame(frameID.current);
    }
  };
  const scrollIntoView = react.useCallback(({ alignment = "start" } = {}) => {
    var _a;
    shouldStop.current = false;
    if (frameID.current) {
      cancel();
    }
    const start = (_a = getScrollStart.getScrollStart({ parent: scrollableRef.current, axis })) != null ? _a : 0;
    const change = getRelativePosition.getRelativePosition({
      parent: scrollableRef.current,
      target: targetRef.current,
      axis,
      alignment,
      offset,
      isList
    }) - (scrollableRef.current ? 0 : start);
    function animateScroll() {
      if (startTime.current === 0) {
        startTime.current = performance.now();
      }
      const now = performance.now();
      const elapsed = now - startTime.current;
      const t = reducedMotion || duration === 0 ? 1 : elapsed / duration;
      const distance = start + change * easing(t);
      setScrollParam.setScrollParam({
        parent: scrollableRef.current,
        axis,
        distance
      });
      if (!shouldStop.current && t < 1) {
        frameID.current = requestAnimationFrame(animateScroll);
      } else {
        typeof onScrollFinish === "function" && onScrollFinish();
        startTime.current = 0;
        frameID.current = 0;
        cancel();
      }
    }
    animateScroll();
  }, [scrollableRef.current]);
  const handleStop = () => {
    if (cancelable) {
      shouldStop.current = true;
    }
  };
  useWindowEvent.useWindowEvent("wheel", handleStop, {
    passive: true
  });
  useWindowEvent.useWindowEvent("touchmove", handleStop, {
    passive: true
  });
  react.useEffect(() => cancel, []);
  return {
    scrollableRef,
    targetRef,
    scrollIntoView,
    cancel
  };
}

exports.useScrollIntoView = useScrollIntoView;
//# sourceMappingURL=use-scroll-into-view.js.map
