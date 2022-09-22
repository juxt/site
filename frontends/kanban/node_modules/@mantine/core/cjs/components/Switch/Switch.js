'use strict';

Object.defineProperty(exports, '__esModule', { value: true });

var React = require('react');
var hooks = require('@mantine/hooks');
var styles = require('@mantine/styles');
var Switch_styles = require('./Switch.styles.js');
var Box = require('../Box/Box.js');

function _interopDefaultLegacy (e) { return e && typeof e === 'object' && 'default' in e ? e['default'] : e; }

var React__default = /*#__PURE__*/_interopDefaultLegacy(React);

var __defProp = Object.defineProperty;
var __defProps = Object.defineProperties;
var __getOwnPropDescs = Object.getOwnPropertyDescriptors;
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
var __spreadProps = (a, b) => __defProps(a, __getOwnPropDescs(b));
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
const Switch = React.forwardRef((_a, ref) => {
  var _b = _a, {
    className,
    color,
    label,
    offLabel = "",
    onLabel = "",
    id,
    style,
    size = "sm",
    radius = "xl",
    wrapperProps,
    children,
    classNames,
    styles: styles$1,
    sx
  } = _b, others = __objRest(_b, [
    "className",
    "color",
    "label",
    "offLabel",
    "onLabel",
    "id",
    "style",
    "size",
    "radius",
    "wrapperProps",
    "children",
    "classNames",
    "styles",
    "sx"
  ]);
  const { classes, cx } = Switch_styles['default']({ size, color, radius, offLabel, onLabel }, { classNames, styles: styles$1, name: "Switch" });
  const { margins, rest } = styles.extractMargins(others);
  const uuid = hooks.useUuid(id);
  return /* @__PURE__ */ React__default.createElement(Box.Box, __spreadValues(__spreadValues({
    className: cx(classes.root, className),
    style,
    sx
  }, margins), wrapperProps), /* @__PURE__ */ React__default.createElement("input", __spreadProps(__spreadValues({}, rest), {
    id: uuid,
    ref,
    type: "checkbox",
    className: classes.input
  })), label && /* @__PURE__ */ React__default.createElement("label", {
    className: classes.label,
    htmlFor: uuid
  }, label));
});
Switch.displayName = "@mantine/core/Switch";

exports.Switch = Switch;
//# sourceMappingURL=Switch.js.map
