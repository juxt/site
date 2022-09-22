'use strict';

Object.defineProperty(exports, '__esModule', { value: true });

var React = require('react');
var hooks = require('@mantine/hooks');
var styles = require('@mantine/styles');
var Radio_styles = require('./Radio.styles.js');
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
const Radio = React.forwardRef((_a, ref) => {
  var _b = _a, {
    className,
    style,
    id,
    children,
    size,
    title,
    disabled,
    color,
    classNames,
    styles: styles$1,
    __staticSelector = "Radio",
    sx
  } = _b, others = __objRest(_b, [
    "className",
    "style",
    "id",
    "children",
    "size",
    "title",
    "disabled",
    "color",
    "classNames",
    "styles",
    "__staticSelector",
    "sx"
  ]);
  const { classes, cx } = Radio_styles['default']({ color, size }, { classNames, styles: styles$1, name: __staticSelector });
  const { margins, rest } = styles.extractMargins(others);
  const uuid = hooks.useUuid(id);
  return /* @__PURE__ */ React__default.createElement(Box.Box, __spreadValues({
    className: cx(classes.radioWrapper, className),
    style,
    title,
    sx
  }, margins), /* @__PURE__ */ React__default.createElement("label", {
    className: cx(classes.label, { [classes.labelDisabled]: disabled }),
    htmlFor: uuid
  }, /* @__PURE__ */ React__default.createElement("input", __spreadValues({
    ref,
    className: classes.radio,
    type: "radio",
    id: uuid,
    disabled
  }, rest)), children && /* @__PURE__ */ React__default.createElement("span", null, children)));
});
Radio.displayName = "@mantine/core/Radio";

exports.Radio = Radio;
//# sourceMappingURL=Radio.js.map
