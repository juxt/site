import React from 'react';
import { useMantineTheme } from '@mantine/styles';
import { getCurveProps } from './get-curve-props.js';

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
function Curve({
  size,
  value,
  offset,
  sum,
  thickness,
  root,
  color,
  lineRoundCaps
}) {
  const theme = useMantineTheme();
  const stroke = theme.fn.themeColor(color || (theme.colorScheme === "dark" ? "dark" : "gray"), color ? 6 : theme.colorScheme === "dark" ? 4 : 1, false);
  return /* @__PURE__ */ React.createElement("circle", __spreadValues({
    fill: "none",
    strokeLinecap: lineRoundCaps ? "round" : "butt",
    stroke
  }, getCurveProps({ sum, size, thickness, value, offset, root })));
}
Curve.displayName = "@mantine/core/Curve";

export { Curve };
//# sourceMappingURL=Curve.js.map
