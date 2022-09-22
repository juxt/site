'use strict';

Object.defineProperty(exports, '__esModule', { value: true });

var react = require('react');
var assignRef = require('../utils/assign-ref/assign-ref.js');

function useMergedRef(...refs) {
  return react.useCallback((node) => {
    refs.forEach((ref) => assignRef.assignRef(ref, node));
  }, refs);
}
function mergeRefs(...refs) {
  return (node) => {
    refs.forEach((ref) => assignRef.assignRef(ref, node));
  };
}

exports.mergeRefs = mergeRefs;
exports.useMergedRef = useMergedRef;
//# sourceMappingURL=use-merged-ref.js.map
