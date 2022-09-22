'use strict';

Object.defineProperty(exports, '__esModule', { value: true });

var React = require('react');
var Text = require('../../Text/Text.js');
var CloseButton = require('../../ActionIcon/CloseButton/CloseButton.js');
var PopoverBody_styles = require('./PopoverBody.styles.js');

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
const PopoverBody = React.forwardRef((_a, ref) => {
  var _b = _a, {
    classNames,
    styles,
    shadow,
    spacing,
    radius,
    withCloseButton,
    title,
    titleId,
    bodyId,
    onClose,
    closeButtonLabel,
    children,
    width
  } = _b, others = __objRest(_b, [
    "classNames",
    "styles",
    "shadow",
    "spacing",
    "radius",
    "withCloseButton",
    "title",
    "titleId",
    "bodyId",
    "onClose",
    "closeButtonLabel",
    "children",
    "width"
  ]);
  const { classes } = PopoverBody_styles['default']({ shadow, radius, spacing, width }, { classNames, styles, name: "Popover" });
  return /* @__PURE__ */ React__default.createElement("div", __spreadValues({
    role: "dialog",
    tabIndex: -1,
    "aria-labelledby": titleId,
    "aria-describedby": bodyId,
    className: classes.wrapper,
    ref
  }, others), /* @__PURE__ */ React__default.createElement("div", {
    className: classes.popover
  }, /* @__PURE__ */ React__default.createElement("div", {
    className: classes.body
  }, !!title && /* @__PURE__ */ React__default.createElement("div", {
    className: classes.header
  }, /* @__PURE__ */ React__default.createElement(Text.Text, {
    size: "sm",
    id: titleId,
    className: classes.title
  }, title)), withCloseButton && /* @__PURE__ */ React__default.createElement(CloseButton.CloseButton, {
    size: "sm",
    onClick: onClose,
    "aria-label": closeButtonLabel,
    className: classes.close
  }), /* @__PURE__ */ React__default.createElement("div", {
    className: classes.inner,
    id: bodyId
  }, children))));
});
PopoverBody.displayName = "@mantine/core/PopoverBody";

exports.PopoverBody = PopoverBody;
//# sourceMappingURL=PopoverBody.js.map
