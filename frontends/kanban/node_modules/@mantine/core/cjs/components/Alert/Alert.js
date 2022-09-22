'use strict';

Object.defineProperty(exports, '__esModule', { value: true });

var React = require('react');
var Alert_styles = require('./Alert.styles.js');
var Box = require('../Box/Box.js');
var CloseButton = require('../ActionIcon/CloseButton/CloseButton.js');

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
const Alert = React.forwardRef((_a, ref) => {
  var _b = _a, {
    className,
    title,
    variant = "light",
    children,
    color,
    classNames,
    icon,
    styles,
    onClose,
    radius = "sm",
    withCloseButton,
    closeButtonLabel
  } = _b, others = __objRest(_b, [
    "className",
    "title",
    "variant",
    "children",
    "color",
    "classNames",
    "icon",
    "styles",
    "onClose",
    "radius",
    "withCloseButton",
    "closeButtonLabel"
  ]);
  const { classes, cx } = Alert_styles['default']({ color, radius, variant }, { classNames, styles, name: "Alert" });
  return /* @__PURE__ */ React__default.createElement(Box.Box, __spreadValues({
    className: cx(classes.root, classes[variant], className),
    ref
  }, others), /* @__PURE__ */ React__default.createElement("div", {
    className: classes.wrapper
  }, icon && /* @__PURE__ */ React__default.createElement("div", {
    className: classes.icon
  }, icon), /* @__PURE__ */ React__default.createElement("div", {
    className: classes.body
  }, title && /* @__PURE__ */ React__default.createElement("div", {
    className: classes.title
  }, /* @__PURE__ */ React__default.createElement("span", {
    className: classes.label
  }, title), withCloseButton && /* @__PURE__ */ React__default.createElement(CloseButton.CloseButton, {
    className: classes.closeButton,
    onClick: () => onClose == null ? void 0 : onClose(),
    variant: "transparent",
    size: 16,
    iconSize: 16,
    "aria-label": closeButtonLabel
  })), /* @__PURE__ */ React__default.createElement("div", {
    className: classes.message
  }, children))));
});
Alert.displayName = "@mantine/core/Alert";

exports.Alert = Alert;
//# sourceMappingURL=Alert.js.map
