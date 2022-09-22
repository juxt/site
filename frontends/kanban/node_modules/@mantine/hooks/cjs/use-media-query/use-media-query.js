'use strict';

Object.defineProperty(exports, '__esModule', { value: true });

var react = require('react');

function attachMediaListener(query, callback) {
  try {
    query.addEventListener("change", callback);
    return () => query.removeEventListener("change", callback);
  } catch (e) {
    query.addListener(callback);
    return () => query.removeListener(callback);
  }
}
function getInitialValue(query) {
  if (typeof window !== "undefined" && "matchMedia" in window) {
    return window.matchMedia(query).matches;
  }
  return false;
}
function useMediaQuery(query) {
  const [matches, setMatches] = react.useState(getInitialValue(query));
  const queryRef = react.useRef();
  react.useEffect(() => {
    if ("matchMedia" in window) {
      queryRef.current = window.matchMedia(query);
      setMatches(queryRef.current.matches);
      return attachMediaListener(queryRef.current, (event) => setMatches(event.matches));
    }
  }, [query]);
  return matches;
}

exports.useMediaQuery = useMediaQuery;
//# sourceMappingURL=use-media-query.js.map
