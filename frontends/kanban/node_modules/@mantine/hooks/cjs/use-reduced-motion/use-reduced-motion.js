'use strict';

Object.defineProperty(exports, '__esModule', { value: true });

var useMediaQuery = require('../use-media-query/use-media-query.js');

function useReducedMotion() {
  return useMediaQuery.useMediaQuery("(prefers-reduced-motion: reduce)");
}

exports.useReducedMotion = useReducedMotion;
//# sourceMappingURL=use-reduced-motion.js.map
