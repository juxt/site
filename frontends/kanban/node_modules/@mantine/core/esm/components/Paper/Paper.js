import React, { forwardRef } from 'react';
import useStyles from './Paper.styles.js';
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
const Paper = forwardRef((_a, ref) => {
  var _b = _a, {
    component,
    className,
    children,
    padding = 0,
    radius = "sm",
    withBorder = false,
    shadow
  } = _b, others = __objRest(_b, [
    "component",
    "className",
    "children",
    "padding",
    "radius",
    "withBorder",
    "shadow"
  ]);
  const { classes, cx } = useStyles({ radius, shadow, padding, withBorder }, { name: "Paper" });
  return /* @__PURE__ */ React.createElement(Box, __spreadValues({
    component: component || "div",
    className: cx(classes.root, className),
    ref
  }, others), children);
});
Paper.displayName = "@mantine/core/Paper";

export { Paper };
//# sourceMappingURL=Paper.js.map
