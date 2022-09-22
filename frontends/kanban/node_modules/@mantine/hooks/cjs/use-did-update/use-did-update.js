'use strict';

Object.defineProperty(exports, '__esModule', { value: true });

var react = require('react');

function useDidUpdate(fn, dependencies) {
  const mounted = react.useRef(false);
  react.useEffect(() => {
    if (mounted.current) {
      fn();
    } else {
      mounted.current = true;
    }
  }, dependencies);
}

exports.useDidUpdate = useDidUpdate;
//# sourceMappingURL=use-did-update.js.map
