'use strict';

Object.defineProperty(exports, '__esModule', { value: true });

var styles = require('@mantine/styles');
var getSortedBreakpoints = require('../utils/get-sorted-breakpoints/get-sorted-breakpoints.js');

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
var useStyles = styles.createStyles((theme, { height, width, padding, fixed, position, hiddenBreakpoint, zIndex }) => {
  const breakpoints = typeof width === "object" && width !== null ? getSortedBreakpoints.getSortedBreakpoints(width, theme).reduce((acc, [breakpoint, breakpointSize]) => {
    acc[`@media (min-width: ${breakpoint + 1}px)`] = {
      width: breakpointSize,
      minWidth: breakpointSize
    };
    return acc;
  }, {}) : null;
  return {
    root: __spreadValues(__spreadProps(__spreadValues(__spreadValues({}, theme.fn.fontStyles()), position), {
      zIndex,
      height,
      width: (width == null ? void 0 : width.base) || "100%",
      position: fixed ? "fixed" : "static",
      boxSizing: "border-box",
      padding: theme.fn.size({ size: padding, sizes: theme.spacing }),
      display: "flex",
      flexDirection: "column",
      backgroundColor: theme.colorScheme === "dark" ? theme.colors.dark[7] : theme.white,
      borderRight: `1px solid ${theme.colorScheme === "dark" ? theme.colors.dark[5] : theme.colors.gray[2]}`
    }), breakpoints),
    hidden: {
      [`@media (max-width: ${theme.fn.size({
        size: hiddenBreakpoint,
        sizes: theme.breakpoints
      })}px)`]: {
        display: "none"
      }
    }
  };
});

exports.default = useStyles;
//# sourceMappingURL=Navbar.styles.js.map
