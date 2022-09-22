'use strict';

Object.defineProperty(exports, '__esModule', { value: true });

var React = require('react');

function findChildByType(children, type) {
  return React.Children.toArray(children).find((item) => item.type === type);
}

exports.findChildByType = findChildByType;
//# sourceMappingURL=find-child-by-type.js.map
