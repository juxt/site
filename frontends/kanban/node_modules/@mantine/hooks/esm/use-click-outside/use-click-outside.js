import { useRef, useEffect } from 'react';

const DEFAULT_EVENTS = ["mousedown", "touchstart"];
function useClickOutside(handler, events, nodes) {
  const ref = useRef();
  useEffect(() => {
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

export { useClickOutside };
//# sourceMappingURL=use-click-outside.js.map
