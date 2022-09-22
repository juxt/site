import React, { forwardRef } from 'react';
import { extractMargins } from '@mantine/styles';
import { useUuid } from '@mantine/hooks';
import { CheckboxIcon } from './CheckboxIcon.js';
import useStyles from './Checkbox.styles.js';
import { Box } from '../Box/Box.js';

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
const Checkbox = forwardRef((_a, ref) => {
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
    styles,
    transitionDuration = 100,
    icon: Icon = CheckboxIcon
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
  const uuid = useUuid(id);
  const { margins, rest } = extractMargins(others);
  const { classes, cx } = useStyles({ size, radius, color, transitionDuration }, { classNames, styles, name: "Checkbox" });
  return /* @__PURE__ */ React.createElement(Box, __spreadValues(__spreadValues({
    className: cx(classes.root, className),
    style,
    sx
  }, margins), wrapperProps), /* @__PURE__ */ React.createElement("div", {
    className: classes.inner
  }, /* @__PURE__ */ React.createElement("input", __spreadValues({
    id: uuid,
    ref,
    type: "checkbox",
    className: classes.input,
    checked: indeterminate || checked
  }, rest)), /* @__PURE__ */ React.createElement(Icon, {
    indeterminate,
    className: classes.icon
  })), label && /* @__PURE__ */ React.createElement("label", {
    className: classes.label,
    htmlFor: uuid
  }, label));
});
Checkbox.displayName = "@mantine/core/Checkbox";

export { Checkbox };
//# sourceMappingURL=Checkbox.js.map
