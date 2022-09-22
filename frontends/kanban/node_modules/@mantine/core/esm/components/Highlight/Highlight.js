import React, { forwardRef } from 'react';
import { Text } from '../Text/Text.js';
import { Mark } from '../Mark/Mark.js';
import { highlighter } from './highlighter/highlighter.js';

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
const Highlight = forwardRef((_a, ref) => {
  var _b = _a, {
    children,
    highlight,
    highlightColor = "yellow",
    component,
    highlightStyles
  } = _b, others = __objRest(_b, [
    "children",
    "highlight",
    "highlightColor",
    "component",
    "highlightStyles"
  ]);
  const highlightChunks = highlighter(children, highlight);
  return /* @__PURE__ */ React.createElement(Text, __spreadValues({
    component,
    ref
  }, others), highlightChunks.map(({ chunk, highlighted }, i) => highlighted ? /* @__PURE__ */ React.createElement(Mark, {
    key: i,
    color: highlightColor,
    sx: highlightStyles
  }, chunk) : /* @__PURE__ */ React.createElement("span", {
    key: i
  }, chunk)));
});
Highlight.displayName = "@mantine/core/Highlight";

export { Highlight };
//# sourceMappingURL=Highlight.js.map
