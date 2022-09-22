'use strict';

Object.defineProperty(exports, '__esModule', { value: true });

var react = require('react');

function useInterval(fn, interval) {
  const [active, setActive] = react.useState(false);
  const intervalRef = react.useRef();
  const start = () => {
    if (!active) {
      setActive(true);
      intervalRef.current = window.setInterval(fn, interval);
    }
  };
  const stop = () => {
    setActive(false);
    window.clearInterval(intervalRef.current);
  };
  const toggle = () => {
    if (active) {
      stop();
    } else {
      start();
    }
  };
  return { start, stop, toggle, active };
}

exports.useInterval = useInterval;
//# sourceMappingURL=use-interval.js.map
