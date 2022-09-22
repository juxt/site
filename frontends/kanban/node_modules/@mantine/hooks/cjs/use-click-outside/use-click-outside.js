'use strict';

Object.defineProperty(exports, '__esModule', { value: true });

var react = require('react');

const DEFAULT_EVENTS = ["mousedown", "touchstart"];
function useClickOutside(handler, events, nodes) {
  const ref = react.useRef();
  react.useEffect(() => {
    const listener = (event) => {
      if (Array.isArray(nodes)) {
        const shouldTrigger = nodes.every((node) => !!node && !node.contains(event.target));
        shouldTrigger && handler();
      } else if (ref.current && !ref.current.contains(event.target)) {
        handler();
      }
    };
    (events || DEFAULT_EVENTS).forEach((fn) => document.addEventListener(fn, listener));
    return () => {
      (events || DEFAULT_EVENTS).forEach((fn) => document.removeEventListener(fn, listener));
    };
  }, [ref, handler, nodes]);
  return ref;
}

exports.useClickOutside = useClickOutside;
//# sourceMappingURL=use-click-outside.js.map
