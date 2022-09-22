'use strict';

Object.defineProperty(exports, '__esModule', { value: true });

var react = require('react');
var shallowEqual = require('../utils/shallow-equal/shallow-equal.js');

function shallowCompare(prevValue, currValue) {
  if (!prevValue || !currValue) {
    return false;
  }
  if (prevValue === currValue) {
    return true;
  }
  if (prevValue.length !== currValue.length) {
    return false;
  }
  for (let i = 0; i < prevValue.length; i += 1) {
    if (!shallowEqual.shallowEqual(prevValue[i], currValue[i])) {
      return false;
    }
  }
  return true;
}
function useShallowCompare(dependencies) {
  const ref = react.useRef([]);
  const updateRef = react.useRef(0);
  if (!shallowCompare(ref.current, dependencies)) {
    ref.current = dependencies;
    updateRef.current += 1;
  }
  return [updateRef.current];
}
function useShallowEffect(cb, dependencies) {
  react.useEffect(cb, useShallowCompare(dependencies));
}

exports.useShallowEffect = useShallowEffect;
//# sourceMappingURL=use-shallow-effect.js.map
