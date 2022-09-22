'use strict';

Object.defineProperty(exports, '__esModule', { value: true });

function size(props) {
  if (typeof props.size === "number") {
    return props.size;
  }
  return props.sizes[props.size] || props.size || props.sizes.md;
}

exports.size = size;
//# sourceMappingURL=size.js.map
