'use strict';

Object.defineProperty(exports, '__esModule', { value: true });

var react = require('react');
var useWindowEvent = require('../use-window-event/use-window-event.js');

const eventListerOptions = {
  passive: true
};
const browser = typeof window !== "undefined";
function useViewportSize() {
  const [windowSize, setWindowSize] = react.useState({
    width: browser ? window.innerWidth : 0,
    height: browser ? window.innerHeight : 0
  });
  const setSize = react.useCallback(() => {
    setWindowSize({
      width: window.innerWidth || 0,
      height: window.innerHeight || 0
    });
  }, []);
  useWindowEvent.useWindowEvent("resize", setSize, eventListerOptions);
  useWindowEvent.useWindowEvent("orientationchange", setSize, eventListerOptions);
  return windowSize;
}

exports.useViewportSize = useViewportSize;
//# sourceMappingURL=use-viewport-size.js.map
