import React, { forwardRef } from 'react';
import { useMantineTheme } from '@mantine/styles';
import useStyles from './Burger.styles.js';
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
const Burger = forwardRef((_a, ref) => {
  var _b = _a, { className, opened, color, size = "md", classNames, styles } = _b, others = __objRest(_b, ["className", "opened", "color", "size", "classNames", "styles"]);
  const theme = useMantineTheme();
  const _color = color || (theme.colorScheme === "dark" ? theme.white : theme.black);
  const { classes, cx } = useStyles({ color: _color, size }, { classNames, styles, name: "Burger" });
  return /* @__PURE__ */ React.createElement(Box, __spreadValues({
    component: "button",
    type: "button",
    className: cx(classes.root, className),
    ref
  }, others), /* @__PURE__ */ React.createElement("div", {
    className: cx(classes.burger, { [classes.opened]: opened })
  }));
});
Burger.displayName = "@mantine/core/Burger";

export { Burger };
//# sourceMappingURL=Burger.js.map
