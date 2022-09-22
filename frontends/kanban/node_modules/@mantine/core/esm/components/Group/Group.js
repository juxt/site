import React, { forwardRef } from 'react';
import useStyles from './Group.styles.js';
import { filterFalsyChildren } from '../../utils/filter-falsy-children/filter-falsy-children.js';
import { Box } from '../Box/Box.js';

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
const Group = forwardRef((_a, ref) => {
  var _b = _a, {
    className,
    position = "left",
    align,
    children,
    noWrap = false,
    grow = false,
    spacing = "md",
    direction = "row",
    classNames,
    styles
  } = _b, others = __objRest(_b, [
    "className",
    "position",
    "align",
    "children",
    "noWrap",
    "grow",
    "spacing",
    "direction",
    "classNames",
    "styles"
  ]);
  const filteredChildren = filterFalsyChildren(children);
  const { classes, cx } = useStyles({
    align,
    grow,
    noWrap,
    spacing,
    position,
    direction,
    count: filteredChildren.length
  }, { classNames, styles, name: "Group" });
  const items = filteredChildren.map((child) => React.cloneElement(child, {
    className: cx(classes.child, child.props.className)
  }));
  return /* @__PURE__ */ React.createElement(Box, __spreadValues({
    className: cx(classes.root, className),
    ref
  }, others), items);
});
Group.displayName = "@mantine/core/Group";

export { Group };
//# sourceMappingURL=Group.js.map
