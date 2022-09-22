'use strict';

Object.defineProperty(exports, '__esModule', { value: true });

var React = require('react');
var Progress_styles = require('./Progress.styles.js');
var Box = require('../Box/Box.js');
var Text = require('../Text/Text.js');

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
function getCumulativeSections(sections) {
  return sections.reduce((acc, section) => {
    acc.sections.push(__spreadProps(__spreadValues({}, section), { accumulated: acc.accumulated }));
    acc.accumulated += section.value;
    return acc;
  }, { accumulated: 0, sections: [] }).sections;
}
const Progress = React.forwardRef((_a, ref) => {
  var _b = _a, {
    className,
    value,
    color,
    size = "md",
    radius = "sm",
    striped = false,
    animate = false,
    label = "",
    "aria-label": ariaLabel,
    classNames,
    styles,
    sections
  } = _b, others = __objRest(_b, [
    "className",
    "value",
    "color",
    "size",
    "radius",
    "striped",
    "animate",
    "label",
    "aria-label",
    "classNames",
    "styles",
    "sections"
  ]);
  const { classes, cx, theme } = Progress_styles['default']({ color, size, radius, striped: striped || animate, animate }, { classNames, styles, name: "Progress" });
  const segments = Array.isArray(sections) ? getCumulativeSections(sections).map((section, index) => /* @__PURE__ */ React__default.createElement(Box.Box, {
    key: index,
    className: classes.bar,
    sx: {
      width: `${section.value}%`,
      left: `${section.accumulated}%`,
      backgroundColor: theme.fn.themeColor(section.color, 6, false)
    }
  }, section.label && /* @__PURE__ */ React__default.createElement(Text.Text, {
    className: classes.label
  }, section.label))) : null;
  return /* @__PURE__ */ React__default.createElement(Box.Box, __spreadValues({
    className: cx(classes.root, className),
    ref
  }, others), segments || /* @__PURE__ */ React__default.createElement("div", {
    role: "progressbar",
    "aria-valuemax": 100,
    "aria-valuemin": 0,
    "aria-valuenow": value,
    "aria-label": ariaLabel,
    className: classes.bar,
    style: { width: `${value}%` }
  }, label ? /* @__PURE__ */ React__default.createElement(Text.Text, {
    className: classes.label
  }, label) : ""));
});
Progress.displayName = "@mantine/core/Progress";

exports.Progress = Progress;
//# sourceMappingURL=Progress.js.map
