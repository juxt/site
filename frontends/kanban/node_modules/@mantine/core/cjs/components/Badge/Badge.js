'use strict';

Object.defineProperty(exports, '__esModule', { value: true });

var React = require('react');
var Badge_styles = require('./Badge.styles.js');
var Box = require('../Box/Box.js');

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
const Badge = React.forwardRef((_a, ref) => {
  var _b = _a, {
    component,
    className,
    color,
    variant = "light",
    fullWidth,
    children,
    size = "md",
    leftSection,
    rightSection,
    radius = "xl",
    gradient = { from: "blue", to: "cyan", deg: 45 },
    classNames,
    styles
  } = _b, others = __objRest(_b, [
    "component",
    "className",
    "color",
    "variant",
    "fullWidth",
    "children",
    "size",
    "leftSection",
    "rightSection",
    "radius",
    "gradient",
    "classNames",
    "styles"
  ]);
  const { classes, cx } = Badge_styles['default']({
    size,
    fullWidth,
    color,
    radius,
    gradientFrom: gradient.from,
    gradientTo: gradient.to,
    gradientDeg: gradient.deg
  }, { classNames, styles, name: "Badge" });
  return /* @__PURE__ */ React__default.createElement(Box.Box, __spreadValues({
    component: component || "div",
    className: cx(classes[variant], classes.root, className),
    ref
  }, others), leftSection && /* @__PURE__ */ React__default.createElement("span", {
    className: classes.leftSection
  }, leftSection), /* @__PURE__ */ React__default.createElement("span", {
    className: classes.inner
  }, children), rightSection && /* @__PURE__ */ React__default.createElement("span", {
    className: classes.rightSection
  }, rightSection));
});
Badge.displayName = "@mantine/core/Badge";

exports.Badge = Badge;
//# sourceMappingURL=Badge.js.map
