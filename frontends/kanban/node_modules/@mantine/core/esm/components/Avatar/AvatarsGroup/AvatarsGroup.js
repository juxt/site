import React, { forwardRef } from 'react';
import { Avatar } from '../Avatar.js';
import useStyles from './AvatarsGroup.styles.js';
import { filterChildrenByType } from '../../../utils/filter-children-by-type/filter-children-by-type.js';
import { Box } from '../../Box/Box.js';
import { Center } from '../../Center/Center.js';

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
const AvatarsGroup = forwardRef((_a, ref) => {
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
  const { classes, cx } = useStyles({ spacing }, { classNames, styles, name: "AvatarsGroup" });
  const avatars = filterChildrenByType(children, Avatar).map((child, index) => React.cloneElement(child, {
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
  return /* @__PURE__ */ React.createElement(Box, __spreadValues({
    className: cx(className, classes.root),
    ref
  }, others), avatars.slice(0, avatars.length - extraAvatars), truncatedAvatars ? /* @__PURE__ */ React.createElement(Avatar, {
    size,
    radius,
    className: classes.child,
    style: { zIndex: limit + 1 }
  }, /* @__PURE__ */ React.createElement(Center, {
    className: classes.truncated
  }, "+", truncatedAvatars)) : null);
});
AvatarsGroup.displayName = "@mantine/core/AvatarsGroup";

export { AvatarsGroup };
//# sourceMappingURL=AvatarsGroup.js.map
