'use strict';

Object.defineProperty(exports, '__esModule', { value: true });

var React = require('react');
var styles = require('@mantine/styles');
var AppShell_styles = require('./AppShell.styles.js');
var getNavbarBreakpoints = require('./utils/get-navbar-breakpoints/get-navbar-breakpoints.js');
var getNavbarBaseWidth = require('./utils/get-navbar-base-width/get-navbar-base-width.js');
var getElementHeight = require('./utils/get-element-height/get-element-height.js');
var Box = require('../Box/Box.js');

function _interopDefaultLegacy (e) { return e && typeof e === 'object' && 'default' in e ? e['default'] : e; }

var React__default = /*#__PURE__*/_interopDefaultLegacy(React);

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
const AppShell = React.forwardRef((_a, ref) => {
  var _b = _a, {
    children,
    navbar,
    header,
    fixed = false,
    zIndex = styles.getDefaultZIndex("app"),
    padding = "md",
    navbarOffsetBreakpoint,
    className,
    styles: styles$1,
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
  const theme = styles.useMantineTheme();
  const navbarBreakpoints = getNavbarBreakpoints.getNavbarBreakpoints(navbar, theme);
  const navbarWidth = getNavbarBaseWidth.getNavbarBaseWidth(navbar);
  const headerHeight = getElementHeight.getElementHeight(header);
  const navbarHeight = getElementHeight.getElementHeight(navbar);
  const { classes, cx } = AppShell_styles['default']({
    padding,
    fixed,
    navbarWidth,
    headerHeight,
    navbarBreakpoints,
    navbarOffsetBreakpoint
  }, { styles: styles$1, classNames, name: "AppShell" });
  const _header = header ? React__default.cloneElement(header, { fixed, zIndex }) : null;
  const _navbar = navbar ? React__default.cloneElement(navbar, {
    fixed,
    zIndex,
    height: navbarHeight !== "0px" ? navbarHeight : `calc(100vh - ${headerHeight})`,
    position: { top: headerHeight, left: 0 }
  }) : null;
  return /* @__PURE__ */ React__default.createElement(Box.Box, __spreadValues({
    className: cx(classes.root, className),
    ref
  }, others), _header, /* @__PURE__ */ React__default.createElement("div", {
    className: classes.body
  }, _navbar, /* @__PURE__ */ React__default.createElement("main", {
    className: classes.main
  }, children)));
});
AppShell.displayName = "@mantine/core/AppShell";

exports.AppShell = AppShell;
//# sourceMappingURL=AppShell.js.map
