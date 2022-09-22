import React, { forwardRef } from 'react';
import { useUuid, useUncontrolled } from '@mantine/hooks';
import { extractMargins } from '@mantine/styles';
import useStyles from './Chip.styles.js';
import { Box } from '../../Box/Box.js';
import { CheckboxIcon } from '../../Checkbox/CheckboxIcon.js';

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
const Chip = forwardRef((_a, ref) => {
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
    styles,
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
  const uuid = useUuid(id);
  const { margins, rest } = extractMargins(others);
  const { classes, cx, theme } = useStyles({ radius, size, color }, { classNames, styles, name: __staticSelector });
  const [value, setValue] = useUncontrolled({
    value: checked,
    defaultValue: defaultChecked,
    finalValue: false,
    onChange,
    rule: (val) => typeof val === "boolean"
  });
  const defaultVariant = theme.colorScheme === "dark" ? "filled" : "outline";
  return /* @__PURE__ */ React.createElement(Box, __spreadValues(__spreadValues({
    className: cx(classes.root, className),
    style,
    sx
  }, margins), wrapperProps), /* @__PURE__ */ React.createElement("input", __spreadValues({
    type,
    className: classes.input,
    checked: value,
    onChange: (event) => setValue(event.currentTarget.checked),
    id: uuid,
    disabled,
    ref
  }, rest)), /* @__PURE__ */ React.createElement("label", {
    htmlFor: uuid,
    className: cx(classes.label, { [classes.checked]: value, [classes.disabled]: disabled }, classes[variant || defaultVariant])
  }, value && /* @__PURE__ */ React.createElement("span", {
    className: classes.iconWrapper
  }, /* @__PURE__ */ React.createElement(CheckboxIcon, {
    indeterminate: false,
    className: classes.checkIcon
  })), children));
});
Chip.displayName = "@mantine/core/Chip";

export { Chip };
//# sourceMappingURL=Chip.js.map
