'use strict';

Object.defineProperty(exports, '__esModule', { value: true });

var React = require('react');
var styles = require('@mantine/styles');
var Header_styles = require('./Header.styles.js');
var Box = require('../../Box/Box.js');

function _interopDefaultLegacy (e) { return e && typeof e === 'object' && 'default' in e ? e['default'] : e; }

var React__default = /*#__PURE__*/_interopDefaultLegacy(React);

var __defProp = Object.defineProperty;
var __getOwnPropSymbols = Object.getOwnPropertySymbols;
var __hasOwnProp = Object.prototype.hasOwnProperty;
var __propIsEnum = Object.prototype.propertyIsEnumerable;
var __defNormalProp = (obj, key, value) => key in obj ? __defProp(obj, key, { enumerable: true, configurable: true, writable: true, value }) : obj[key] = value;
var __spreadValues = (a, b) => {
  for (var prop in b || (b = {}))
    if (__hasOwnProp.call(b, prop))
      __defNormalProp(a, prop, b[prop]);
  if (__getOwnPropSymbols)
    for (var prop of __getOwnPropSymbols(b)) {
      if (__propIsEnum.call(b, prop))
        __defNormalProp(a, prop, b[prop]);
    }
  return a;
};
var __objRest = (source, exclude) => {
  var target = {};
  for (var prop in source)
    if (__hasOwnProp.call(source, prop) && exclude.indexOf(prop) < 0)
      target[prop] = source[prop];
  if (source != null && __getOwnPropSymbols)
    for (var prop of __getOwnPropSymbols(source)) {
      if (exclude.indexOf(prop) < 0 && __propIsEnum.call(source, prop))
        target[prop] = source[prop];
    }
  return target;
};
const Header = React.forwardRef((_a, ref) => {
  var _b = _a, {
    children,
    className,
    classNames,
    styles: styles$1,
    height,
    padding = 0,
    fixed = false,
    position = { top: 0, left: 0, right: 0 },
    zIndex = styles.getDefaultZIndex("app")
  } = _b, others = __objRest(_b, [
    "children",
    "className",
    "classNames",
    "styles",
    "height",
    "padding",
    "fixed",
    "position",
    "zIndex"
  ]);
  const { classes, cx } = Header_styles['default']({ height, padding, fixed, position, zIndex }, { name: "Header", classNames, styles: styles$1 });
  return /* @__PURE__ */ React__default.createElement(Box.Box, __spreadValues({
    component: "nav",
    className: cx(classes.root, className),
    ref
  }, others), children);
});
Header.displayName = "@mantine/core/Header";

exports.Header = Header;
//# sourceMappingURL=Header.js.map
