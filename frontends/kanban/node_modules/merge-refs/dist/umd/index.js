"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports["default"] = mergeRefs;

/**
 * A function that merges React refs into one.
 * Supports both functions and ref objects created using createRef() and useRef().
 *
 * Usage:
 * ```jsx
 * <div ref={mergeRefs(ref1, ref2, ref3)} />
 * ```
 *
 * @param {...Array<Function|object>} inputRefs Array of refs
 * @returns {Function} Merged refs
 */
function mergeRefs() {
  for (var _len = arguments.length, inputRefs = new Array(_len), _key = 0; _key < _len; _key++) {
    inputRefs[_key] = arguments[_key];
  }

  var filteredInputRefs = inputRefs.filter(Boolean);

  if (filteredInputRefs.length <= 1) {
    return filteredInputRefs[0];
  }

  return function mergedRefs(ref) {
    filteredInputRefs.forEach(function (inputRef) {
      if (typeof inputRef === 'function') {
        inputRef(ref);
      } else {
        // eslint-disable-next-line no-param-reassign
        inputRef.current = ref;
      }
    });
  };
}