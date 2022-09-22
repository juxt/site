import React, { forwardRef } from 'react';
import { getDefaultZIndex, useMantineTheme } from '@mantine/styles';
import useStyles from './AppShell.styles.js';
import { getNavbarBreakpoints } from './utils/get-navbar-breakpoints/get-navbar-breakpoints.js';
import { getNavbarBaseWidth } from './utils/get-navbar-base-width/get-navbar-base-width.js';
import { getElementHeight } from './utils/get-element-height/get-element-height.js';
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
const AppShell = forwardRef((_a, ref) => {
  var _b = _a, {
    children,
    navbar,
    header,
    fixed = false,
    zIndex = getDefaultZIndex("app"),
    padding = "md",
    navbarOffsetBreakpoint,
    className,
    styles,
    classNames
  } = _b, others = __objRest(_b, [
    "children",
    "navbar",
    "header",
    "fixed",
    "zIndex",
    "padding",
    "navbarOffsetBreakpoint",
    "className",
    "styles",
    "classNames"
  ]);
  const theme = useMantineTheme();
  const navbarBreakpoints = getNavbarBreakpoints(navbar, theme);
  const navbarWidth = getNavbarBaseWidth(navbar);
  const headerHeight = getElementHeight(header);
  const navbarHeight = getElementHeight(navbar);
  const { classes, cx } = useStyles({
    padding,
    fixed,
    navbarWidth,
    headerHeight,
    navbarBreakpoints,
    navbarOffsetBreakpoint
  }, { styles, classNames, name: "AppShell" });
  const _header = header ? React.cloneElement(header, { fixed, zIndex }) : null;
  const _navbar = navbar ? React.cloneElement(navbar, {
    fixed,
    zIndex,
    height: navbarHeight !== "0px" ? navbarHeight : `calc(100vh - ${headerHeight})`,
    position: { top: headerHeight, left: 0 }
  }) : null;
  return /* @__PURE__ */ React.createElement(Box, __spreadValues({
    className: cx(classes.root, className),
    ref
  }, others), _header, /* @__PURE__ */ React.createElement("div", {
    className: classes.body
  }, _navbar, /* @__PURE__ */ React.createElement("main", {
    className: classes.main
  }, children)));
});
AppShell.displayName = "@mantine/core/AppShell";

export { AppShell };
//# sourceMappingURL=AppShell.js.map
