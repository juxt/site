import { useState, useRef } from 'react';

function useInterval(fn, interval) {
  const [active, setActive] = useState(false);
  const intervalRef = useRef();
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

export { useInterval };
//# sourceMappingURL=use-interval.js.map
