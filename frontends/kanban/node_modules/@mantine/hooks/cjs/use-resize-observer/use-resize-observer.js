'use strict';

Object.defineProperty(exports, '__esModule', { value: true });

var react = require('react');

const defaultState = {
  x: 0,
  y: 0,
  width: 0,
  height: 0,
  top: 0,
  left: 0,
  bottom: 0,
  right: 0
};
const browser = typeof window !== "undefined";
function useResizeObserver() {
  const frameID = react.useRef(0);
  const ref = react.useRef(null);
  const [rect, setRect] = react.useState(defaultState);
  const observer = react.useMemo(() => browser ? new ResizeObserver((entries) => {
    const entry = entries[0];
    if (entry) {
      cancelAnimationFrame(frameID.current);
      frameID.current = requestAnimationFrame(() => {
        if (ref.current) {
          setRect(entry.contentRect);
        }
      });
    }
  }) : null, []);
  react.useEffect(() => {
    if (ref.current) {
      observer.observe(ref.current);
    }
    return () => {
      observer.disconnect();
      if (frameID.current) {
        cancelAnimationFrame(frameID.current);
      }
    };
  }, [ref.current]);
  return [ref, rect];
}
function useElementSize() {
  const [ref, { width, height }] = useResizeObserver();
  return { ref, width, height };
}

exports.useElementSize = useElementSize;
exports.useResizeObserver = useResizeObserver;
//# sourceMappingURL=use-resize-observer.js.map
