'use strict';

Object.defineProperty(exports, '__esModule', { value: true });

var react = require('react');
var useWindowEvent = require('../use-window-event/use-window-event.js');

function useHash() {
  const [hash, setHashValue] = react.useState(typeof window !== "undefined" ? window.location.hash : "");
  const setHash = (value) => {
    window.location.hash = value;
    setHashValue(value);
  };
  useWindowEvent.useWindowEvent("hashchange", () => {
    setHashValue(window.location.hash);
  });
  return [hash, setHash];
}

exports.useHash = useHash;
//# sourceMappingURL=use-hash.js.map
