'use strict';

Object.defineProperty(exports, '__esModule', { value: true });

var React = require('react');
var Paper = require('../Paper/Paper.js');
var CardSection = require('./CardSection/CardSection.js');
var Card_styles = require('./Card.styles.js');

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
const Card = React.forwardRef((_a, ref) => {
  var _b = _a, {
    component,
    className,
    padding = "md",
    radius = "sm",
    children,
    classNames,
    styles
  } = _b, others = __objRest(_b, [
    "component",
    "className",
    "padding",
    "radius",
    "children",
    "classNames",
    "styles"
  ]);
  const { classes, cx } = Card_styles['default'](null, { name: "Card", classNames, styles });
  const _children = React.Children.toArray(children);
  const content = _children.map((child, index) => {
    if (typeof child === "object" && child && "type" in child && child.type === CardSection.CardSection) {
      return React.cloneElement(child, {
        padding,
        first: index === 0,
        last: index === _children.length - 1
      });
    }
    return child;
  });
  return /* @__PURE__ */ React__default.createElement(Paper.Paper, __spreadValues({
    className: cx(classes.root, className),
    radius,
    padding,
    component,
    ref
  }, others), content);
});
Card.Section = CardSection.CardSection;
Card.displayName = "@mantine/core/Card";

exports.Card = Card;
//# sourceMappingURL=Card.js.map
