import React, { forwardRef } from 'react';
import useStyles from './Text.styles.js';
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
const Text = forwardRef((_a, ref) => {
  var _b = _a, {
    className,
    component,
    size = "md",
    weight,
    transform,
    color,
    align,
    variant = "text",
    lineClamp,
    gradient = { from: "blue", to: "cyan", deg: 45 },
    inline = false,
    inherit = false,
    underline,
    classNames,
    styles
  } = _b, others = __objRest(_b, [
    "className",
    "component",
    "size",
    "weight",
    "transform",
    "color",
    "align",
    "variant",
    "lineClamp",
    "gradient",
    "inline",
    "inherit",
    "underline",
    "classNames",
    "styles"
  ]);
  const { classes, cx } = useStyles({
    variant,
    color,
    size,
    lineClamp,
    inline,
    inherit,
    underline,
    weight,
    transform,
    align,
    gradientFrom: gradient.from,
    gradientTo: gradient.to,
    gradientDeg: gradient.deg
  }, { classNames, styles, name: "Text" });
  return /* @__PURE__ */ React.createElement(Box, __spreadValues({
    ref,
    component: component || "div",
    className: cx(classes.root, { [classes.gradient]: variant === "gradient" }, className)
  }, others));
});
Text.displayName = "@mantine/core/Text";

export { Text };
//# sourceMappingURL=Text.js.map
