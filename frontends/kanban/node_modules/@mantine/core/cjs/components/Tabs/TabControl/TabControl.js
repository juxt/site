'use strict';

Object.defineProperty(exports, '__esModule', { value: true });

var React = require('react');
var hooks = require('@mantine/hooks');
var TabControl_styles = require('./TabControl.styles.js');
var Box = require('../../Box/Box.js');

function _interopDefaultLegacy (e) { return e && typeof e === 'object' && 'default' in e ? e['default'] : e; }

var React__default = /*#__PURE__*/_interopDefaultLegacy(React);

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
const TabControl = React.forwardRef((_a, ref) => {
  var _b = _a, {
    className,
    active,
    color,
    variant = "default",
    classNames,
    styles,
    orientation = "horizontal",
    icon: __,
    label,
    icon,
    tabKey,
    color: overrideColor,
    elementRef
  } = _b, others = __objRest(_b, [
    "className",
    "active",
    "color",
    "variant",
    "classNames",
    "styles",
    "orientation",
    "icon",
    "label",
    "icon",
    "tabKey",
    "color",
    "elementRef"
  ]);
  const { classes, cx } = TabControl_styles['default']({ color: overrideColor || color, orientation }, { classNames, styles, name: "Tabs" });
  return /* @__PURE__ */ React__default.createElement(Box.Box, __spreadProps(__spreadValues({}, others), {
    component: "button",
    tabIndex: active ? 0 : -1,
    className: cx(classes.tabControl, classes[variant], { [classes.tabActive]: active }, className),
    type: "button",
    role: "tab",
    "aria-selected": active,
    ref: hooks.mergeRefs(ref, elementRef)
  }), /* @__PURE__ */ React__default.createElement("div", {
    className: classes.tabInner
  }, icon && /* @__PURE__ */ React__default.createElement("div", {
    className: classes.tabIcon
  }, icon), label && /* @__PURE__ */ React__default.createElement("div", {
    className: classes.tabLabel
  }, label)));
});
TabControl.displayName = "@mantine/core/TabControl";

exports.TabControl = TabControl;
//# sourceMappingURL=TabControl.js.map
