'use strict';

Object.defineProperty(exports, '__esModule', { value: true });

function getElementHeight(element) {
  var _a;
  const height = (_a = element == null ? void 0 : element.props) == null ? void 0 : _a.height;
  return typeof height === "number" ? `${height}px` : typeof height === "string" ? height : "0px";
}

exports.getElementHeight = getElementHeight;
//# sourceMappingURL=get-element-height.js.map
