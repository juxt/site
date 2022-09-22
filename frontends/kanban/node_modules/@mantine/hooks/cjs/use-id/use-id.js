'use strict';

Object.defineProperty(exports, '__esModule', { value: true });

var react = require('react');
var randomId = require('../utils/random-id/random-id.js');

function useId(id, generateId = randomId.randomId) {
  const generatedId = react.useRef(generateId());
  return id || generatedId.current;
}

exports.useId = useId;
//# sourceMappingURL=use-id.js.map
