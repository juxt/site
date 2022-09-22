import React, { forwardRef } from 'react';
import { getDefaultZIndex } from '@mantine/styles';
import { NavbarSection } from './NavbarSection/NavbarSection.js';
import useStyles from './Navbar.styles.js';
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
const Navbar = forwardRef((_a, ref) => {
  var _b = _a, {
    width,
    height = "100vh",
    padding = 0,
    fixed = false,
    position = { top: 0, left: 0 },
    zIndex = getDefaultZIndex("app"),
    hiddenBreakpoint = "md",
    hidden = false,
    className,
    classNames,
    styles,
    children
  } = _b, others = __objRest(_b, [
    "width",
    "height",
    "padding",
    "fixed",
    "position",
    "zIndex",
    "hiddenBreakpoint",
    "hidden",
    "className",
    "classNames",
    "styles",
    "children"
  ]);
  const { classes, cx } = useStyles({ width, height, padding, fixed, position, hiddenBreakpoint, zIndex }, { classNames, styles, name: "Navbar" });
  return /* @__PURE__ */ React.createElement(Box, __spreadValues({
    component: "nav",
    ref,
    className: cx(classes.root, { [classes.hidden]: hidden }, className)
  }, others), children);
});
Navbar.Section = NavbarSection;
Navbar.displayName = "@mantine/core/Navbar";

export { Navbar };
//# sourceMappingURL=Navbar.js.map
