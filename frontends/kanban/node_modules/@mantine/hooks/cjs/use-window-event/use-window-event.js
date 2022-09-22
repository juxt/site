'use strict';

Object.defineProperty(exports, '__esModule', { value: true });

var react = require('react');

function useWindowEvent(type, listener, options) {
  react.useEffect(() => {
    window.addEventListener(type, listener, options);
    return () => window.removeEventListener(type, listener, options);
  }, []);
}

exports.useWindowEvent = useWindowEvent;
//# sourceMappingURL=use-window-event.js.map
