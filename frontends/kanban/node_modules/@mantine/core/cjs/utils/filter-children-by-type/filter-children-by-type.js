'use strict';

Object.defineProperty(exports, '__esModule', { value: true });

var React = require('react');

function filterChildrenByType(children, type) {
  return React.Children.toArray(children).filter((item) => Array.isArray(type) ? type.some((component) => component === item.type) : item.type === type);
}

exports.filterChildrenByType = filterChildrenByType;
//# sourceMappingURL=filter-children-by-type.js.map
