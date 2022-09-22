'use strict';

Object.defineProperty(exports, '__esModule', { value: true });

var react = require('react');
var clamp = require('../utils/clamp/clamp.js');

const clampUseMovePosition = (position) => ({
  x: clamp.clamp({ min: 0, max: 1, value: position.x }),
  y: clamp.clamp({ min: 0, max: 1, value: position.y })
});
function useMove(onChange, handlers, dir = "ltr") {
  const ref = react.useRef();
  const mounted = react.useRef(false);
  const isSliding = react.useRef(false);
  const frame = react.useRef(0);
  const [active, setActive] = react.useState(false);
  react.useEffect(() => {
    mounted.current = true;
  }, []);
  react.useEffect(() => {
    const onScrub = ({ x, y }) => {
      cancelAnimationFrame(frame.current);
      frame.current = requestAnimationFrame(() => {
        if (mounted.current && ref.current) {
          ref.current.style.userSelect = "none";
          const rect = ref.current.getBoundingClientRect();
          if (rect.width && rect.height) {
            const _x = clamp.clamp({ value: (x - rect.left) / rect.width, min: 0, max: 1 });
            onChange({
              x: dir === "ltr" ? _x : 1 - _x,
              y: clamp.clamp({ value: (y - rect.top) / rect.height, min: 0, max: 1 })
            });
          }
        }
      });
    };
    const bindEvents = () => {
      document.addEventListener("mousemove", onMouseMove);
      document.addEventListener("mouseup", stopScrubbing);
      document.addEventListener("touchmove", onTouchMove);
      document.addEventListener("touchend", stopScrubbing);
    };
    const unbindEvents = () => {
      document.removeEventListener("mousemove", onMouseMove);
      document.removeEventListener("mouseup", stopScrubbing);
      document.removeEventListener("touchmove", onTouchMove);
      document.removeEventListener("touchend", stopScrubbing);
    };
    const startScrubbing = () => {
      if (!isSliding.current && mounted.current) {
        isSliding.current = true;
        typeof (handlers == null ? void 0 : handlers.onScrubStart) === "function" && handlers.onScrubStart();
        setActive(true);
        bindEvents();
      }
    };
    const stopScrubbing = () => {
      if (isSliding.current && mounted.current) {
        isSliding.current = false;
        typeof (handlers == null ? void 0 : handlers.onScrubEnd) === "function" && handlers.onScrubEnd();
        setActive(false);
        unbindEvents();
      }
    };
    const onMouseDown = (event) => {
      startScrubbing();
      onMouseMove(event);
    };
    const onMouseMove = (event) => onScrub({ x: event.clientX, y: event.clientY });
    const onTouchStart = (event) => {
      startScrubbing();
      event == null ? void 0 : event.preventDefault();
      onTouchMove(event);
    };
    const onTouchMove = (event) => {
      event == null ? void 0 : event.preventDefault();
      onScrub({ x: event.changedTouches[0].clientX, y: event.changedTouches[0].clientY });
    };
    ref.current.addEventListener("mousedown", onMouseDown);
    ref.current.addEventListener("touchstart", onTouchStart);
    return () => {
      if (ref.current) {
        ref.current.removeEventListener("mousedown", onMouseDown);
        ref.current.removeEventListener("touchstart", onTouchStart);
      }
    };
  }, [ref.current, dir]);
  return { ref, active };
}

exports.clampUseMovePosition = clampUseMovePosition;
exports.useMove = useMove;
//# sourceMappingURL=use-move.js.map
