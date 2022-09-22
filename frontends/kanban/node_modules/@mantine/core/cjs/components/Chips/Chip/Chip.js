'use strict';

Object.defineProperty(exports, '__esModule', { value: true });

var React = require('react');
var hooks = require('@mantine/hooks');
var styles = require('@mantine/styles');
var Chip_styles = require('./Chip.styles.js');
var Box = require('../../Box/Box.js');
var CheckboxIcon = require('../../Checkbox/CheckboxIcon.js');

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
const Chip = React.forwardRef((_a, ref) => {
  var _b = _a, {
    radius = "xl",
    type = "checkbox",
    size = "sm",
    variant,
    disabled = false,
    __staticSelector = "Chip",
    id,
    color,
    children,
    className,
    classNames,
    style,
    styles: styles$1,
    checked,
    defaultChecked,
    onChange,
    sx,
    wrapperProps
  } = _b, others = __objRest(_b, [
    "radius",
    "type",
    "size",
    "variant",
    "disabled",
    "__staticSelector",
    "id",
    "color",
    "children",
    "className",
    "classNames",
    "style",
    "styles",
    "checked",
    "defaultChecked",
    "onChange",
    "sx",
    "wrapperProps"
  ]);
  const uuid = hooks.useUuid(id);
  const { margins, rest } = styles.extractMargins(others);
  const { classes, cx, theme } = Chip_styles['default']({ radius, size, color }, { classNames, styles: styles$1, name: __staticSelector });
  const [value, setValue] = hooks.useUncontrolled({
    value: checked,
    defaultValue: defaultChecked,
    finalValue: false,
    onChange,
    rule: (val) => typeof val === "boolean"
  });
  const defaultVariant = theme.colorScheme === "dark" ? "filled" : "outline";
  return /* @__PURE__ */ React__default.createElement(Box.Box, __spreadValues(__spreadValues({
    className: cx(classes.root, className),
    style,
    sx
  }, margins), wrapperProps), /* @__PURE__ */ React__default.createElement("input", __spreadValues({
    type,
    className: classes.input,
    checked: value,
    onChange: (event) => setValue(event.currentTarget.checked),
    id: uuid,
    disabled,
    ref
  }, rest)), /* @__PURE__ */ React__default.createElement("label", {
    htmlFor: uuid,
    className: cx(classes.label, { [classes.checked]: value, [classes.disabled]: disabled }, classes[variant || defaultVariant])
  }, value && /* @__PURE__ */ React__default.createElement("span", {
    className: classes.iconWrapper
  }, /* @__PURE__ */ React__default.createElement(CheckboxIcon.CheckboxIcon, {
    indeterminate: false,
    className: classes.checkIcon
  })), children));
});
Chip.displayName = "@mantine/core/Chip";

exports.Chip = Chip;
//# sourceMappingURL=Chip.js.map
