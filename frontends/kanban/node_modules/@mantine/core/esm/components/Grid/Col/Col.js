import React from 'react';
import useStyles from './Col.styles.js';
import { Box } from '../../Box/Box.js';

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
function isValidSpan(span) {
  return typeof span === "number" && span > 0 && span % 1 === 0;
}
function Col(_a) {
  var _b = _a, {
    children,
    span,
    gutter,
    offset = 0,
    offsetXs = 0,
    offsetSm = 0,
    offsetMd = 0,
    offsetLg = 0,
    offsetXl = 0,
    grow,
    xs,
    sm,
    md,
    lg,
    xl,
    columns,
    className,
    classNames,
    styles,
    id
  } = _b, others = __objRest(_b, [
    "children",
    "span",
    "gutter",
    "offset",
    "offsetXs",
    "offsetSm",
    "offsetMd",
    "offsetLg",
    "offsetXl",
    "grow",
    "xs",
    "sm",
    "md",
    "lg",
    "xl",
    "columns",
    "className",
    "classNames",
    "styles",
    "id"
  ]);
  const { classes, cx } = useStyles({
    gutter,
    offset,
    offsetXs,
    offsetSm,
    offsetMd,
    offsetLg,
    offsetXl,
    xs,
    sm,
    md,
    lg,
    xl,
    grow,
    columns,
    span
  }, { classNames, styles, name: "Col" });
  if (!isValidSpan(span) || span > columns) {
    return null;
  }
  return /* @__PURE__ */ React.createElement(Box, __spreadValues({
    className: cx(classes.root, className)
  }, others), children);
}
Col.displayName = "@mantine/core/Col";

export { Col, isValidSpan };
//# sourceMappingURL=Col.js.map
