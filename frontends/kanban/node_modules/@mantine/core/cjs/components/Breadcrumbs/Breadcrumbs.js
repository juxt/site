'use strict';

Object.defineProperty(exports, '__esModule', { value: true });

var React = require('react');
var Breadcrumbs_styles = require('./Breadcrumbs.styles.js');
var Text = require('../Text/Text.js');
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
const Breadcrumbs = React.forwardRef((_a, ref) => {
  var _b = _a, { className, children, separator = "/", classNames, styles } = _b, others = __objRest(_b, ["className", "children", "separator", "classNames", "styles"]);
  const { classes, cx } = Breadcrumbs_styles['default'](null, { classNames, styles, name: "Breadcrumbs" });
  const items = React__default.Children.toArray(children).reduce((acc, child, index, array) => {
    acc.push(React__default.cloneElement(child, { className: classes.breadcrumb, key: index }));
    if (index !== array.length - 1) {
      acc.push(/* @__PURE__ */ React__default.createElement(Text.Text, {
        size: "sm",
        className: classes.separator,
        key: `separator-${index}`
      }, separator));
    }
    return acc;
  }, []);
  return /* @__PURE__ */ React__default.createElement(Box.Box, __spreadValues({
    className: cx(classes.root, className),
    ref
  }, others), items);
});
Breadcrumbs.displayName = "@mantine/core/Breadcrumbs";

exports.Breadcrumbs = Breadcrumbs;
//# sourceMappingURL=Breadcrumbs.js.map
