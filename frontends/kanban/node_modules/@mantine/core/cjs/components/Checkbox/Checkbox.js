'use strict';

Object.defineProperty(exports, '__esModule', { value: true });

var React = require('react');
var styles = require('@mantine/styles');
var hooks = require('@mantine/hooks');
var CheckboxIcon = require('./CheckboxIcon.js');
var Checkbox_styles = require('./Checkbox.styles.js');
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
const Checkbox = React.forwardRef((_a, ref) => {
  var _b = _a, {
    className,
    style,
    sx,
    checked,
    color,
    label,
    indeterminate,
    id,
    size = "sm",
    radius = "sm",
    wrapperProps,
    children,
    classNames,
    styles: styles$1,
    transitionDuration = 100,
    icon: Icon = CheckboxIcon.CheckboxIcon
  } = _b, others = __objRest(_b, [
    "className",
    "style",
    "sx",
    "checked",
    "color",
    "label",
    "indeterminate",
    "id",
    "size",
    "radius",
    "wrapperProps",
    "children",
    "classNames",
    "styles",
    "transitionDuration",
    "icon"
  ]);
  const uuid = hooks.useUuid(id);
  const { margins, rest } = styles.extractMargins(others);
  const { classes, cx } = Checkbox_styles['default']({ size, radius, color, transitionDuration }, { classNames, styles: styles$1, name: "Checkbox" });
  return /* @__PURE__ */ React__default.createElement(Box.Box, __spreadValues(__spreadValues({
    className: cx(classes.root, className),
    style,
    sx
  }, margins), wrapperProps), /* @__PURE__ */ React__default.createElement("div", {
    className: classes.inner
  }, /* @__PURE__ */ React__default.createElement("input", __spreadValues({
    id: uuid,
    ref,
    type: "checkbox",
    className: classes.input,
    checked: indeterminate || checked
  }, rest)), /* @__PURE__ */ React__default.createElement(Icon, {
    indeterminate,
    className: classes.icon
  })), label && /* @__PURE__ */ React__default.createElement("label", {
    className: classes.label,
    htmlFor: uuid
  }, label));
});
Checkbox.displayName = "@mantine/core/Checkbox";

exports.Checkbox = Checkbox;
//# sourceMappingURL=Checkbox.js.map
