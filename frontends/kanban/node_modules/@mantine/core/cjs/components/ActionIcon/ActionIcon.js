'use strict';

Object.defineProperty(exports, '__esModule', { value: true });

var React = require('react');
var styles = require('@mantine/styles');
var ActionIcon_styles = require('./ActionIcon.styles.js');
var Loader = require('../Loader/Loader.js');
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
const ActionIcon = React.forwardRef((_a, ref) => {
  var _b = _a, {
    className,
    color = "gray",
    children,
    radius = "sm",
    size = "md",
    variant = "hover",
    disabled,
    loaderProps,
    loading = false,
    component,
    styles: styles$1,
    classNames
  } = _b, others = __objRest(_b, [
    "className",
    "color",
    "children",
    "radius",
    "size",
    "variant",
    "disabled",
    "loaderProps",
    "loading",
    "component",
    "styles",
    "classNames"
  ]);
  const theme = styles.useMantineTheme();
  const { classes, cx } = ActionIcon_styles['default']({ size, radius, color }, { name: "ActionIcon", classNames, styles: styles$1 });
  const colors = styles.getSharedColorScheme({ color, theme, variant: "light" });
  const loader = /* @__PURE__ */ React__default.createElement(Loader.Loader, __spreadValues({
    color: colors.color,
    size: theme.fn.size({ size, sizes: ActionIcon_styles.sizes }) - 12
  }, loaderProps));
  return /* @__PURE__ */ React__default.createElement(Box.Box, __spreadValues({
    component: component || "button",
    className: cx(classes[variant], classes.root, { [classes.loading]: loading }, className),
    type: "button",
    ref,
    disabled: disabled || loading
  }, others), loading ? loader : children);
});
ActionIcon.displayName = "@mantine/core/ActionIcon";

exports.ActionIcon = ActionIcon;
//# sourceMappingURL=ActionIcon.js.map
