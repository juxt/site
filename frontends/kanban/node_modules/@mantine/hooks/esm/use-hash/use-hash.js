import { useState } from 'react';
import { useWindowEvent } from '../use-window-event/use-window-event.js';

function useHash() {
  const [hash, setHashValue] = useState(typeof window !== "undefined" ? window.location.hash : "");
  const setHash = (value) => {
    window.location.hash = value;
    setHashValue(value);
  };
  useWindowEvent("hashchange", () => {
    setHashValue(window.location.hash);
  });
  return [hash, setHash];
}

export { useHash };
//# sourceMappingURL=use-hash.js.map
