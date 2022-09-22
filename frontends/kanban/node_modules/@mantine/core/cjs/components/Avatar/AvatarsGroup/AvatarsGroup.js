'use strict';

Object.defineProperty(exports, '__esModule', { value: true });

var React = require('react');
var Avatar = require('../Avatar.js');
var AvatarsGroup_styles = require('./AvatarsGroup.styles.js');
var filterChildrenByType = require('../../../utils/filter-children-by-type/filter-children-by-type.js');
var Box = require('../../Box/Box.js');
var Center = require('../../Center/Center.js');

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
const AvatarsGroup = React.forwardRef((_a, ref) => {
  var _b = _a, {
    className,
    children,
    size = "md",
    radius = "xl",
    limit = 2,
    classNames,
    styles,
    spacing = "lg",
    total
  } = _b, others = __objRest(_b, [
    "className",
    "children",
    "size",
    "radius",
    "limit",
    "classNames",
    "styles",
    "spacing",
    "total"
  ]);
  const { classes, cx } = AvatarsGroup_styles['default']({ spacing }, { classNames, styles, name: "AvatarsGroup" });
  const avatars = filterChildrenByType.filterChildrenByType(children, Avatar.Avatar).map((child, index) => React__default.cloneElement(child, {
    size,
    radius,
    key: index,
    className: cx(classes.child, child.props.className),
    style: __spreadProps(__spreadValues({}, child.props.style), {
      zIndex: index + 1
    })
  }));
  const clampedMax = limit < 2 ? 2 : limit;
  const extraAvatars = avatars.length > clampedMax ? avatars.length - clampedMax : 0;
  const truncatedAvatars = total ? total - Math.min(avatars.length, clampedMax) : extraAvatars;
  return /* @__PURE__ */ React__default.createElement(Box.Box, __spreadValues({
    className: cx(className, classes.root),
    ref
  }, others), avatars.slice(0, avatars.length - extraAvatars), truncatedAvatars ? /* @__PURE__ */ React__default.createElement(Avatar.Avatar, {
    size,
    radius,
    className: classes.child,
    style: { zIndex: limit + 1 }
  }, /* @__PURE__ */ React__default.createElement(Center.Center, {
    className: classes.truncated
  }, "+", truncatedAvatars)) : null);
});
AvatarsGroup.displayName = "@mantine/core/AvatarsGroup";

exports.AvatarsGroup = AvatarsGroup;
//# sourceMappingURL=AvatarsGroup.js.map
