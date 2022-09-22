import React, { forwardRef } from 'react';
import { useUuid } from '@mantine/hooks';
import { extractMargins } from '@mantine/styles';
import useStyles from './Switch.styles.js';
import { Box } from '../Box/Box.js';

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
const Switch = forwardRef((_a, ref) => {
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
    styles,
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
  const { classes, cx } = useStyles({ size, color, radius, offLabel, onLabel }, { classNames, styles, name: "Switch" });
  const { margins, rest } = extractMargins(others);
  const uuid = useUuid(id);
  return /* @__PURE__ */ React.createElement(Box, __spreadValues(__spreadValues({
    className: cx(classes.root, className),
    style,
    sx
  }, margins), wrapperProps), /* @__PURE__ */ React.createElement("input", __spreadProps(__spreadValues({}, rest), {
    id: uuid,
    ref,
    type: "checkbox",
    className: classes.input
  })), label && /* @__PURE__ */ React.createElement("label", {
    className: classes.label,
    htmlFor: uuid
  }, label));
});
Switch.displayName = "@mantine/core/Switch";

export { Switch };
//# sourceMappingURL=Switch.js.map
