import React, { forwardRef } from 'react';
import { usePagination } from '@mantine/hooks';
import { Group } from '../Group/Group.js';
import { DefaultItem } from './DefaultItem/DefaultItem.js';
import useStyles from './Pagination.styles.js';

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
const Pagination = forwardRef((_a, ref) => {
  var _b = _a, {
    itemComponent: Item = DefaultItem,
    classNames,
    styles,
    page,
    initialPage = 1,
    color,
    total,
    siblings = 1,
    boundaries = 1,
    size = "md",
    radius = "sm",
    onChange,
    getItemAriaLabel,
    spacing,
    withEdges = false,
    withControls = true,
    sx
  } = _b, others = __objRest(_b, [
    "itemComponent",
    "classNames",
    "styles",
    "page",
    "initialPage",
    "color",
    "total",
    "siblings",
    "boundaries",
    "size",
    "radius",
    "onChange",
    "getItemAriaLabel",
    "spacing",
    "withEdges",
    "withControls",
    "sx"
  ]);
  const { classes, cx, theme } = useStyles({ color, size, radius }, { classNames, styles, name: "Pagination" });
  const { range, setPage, next, previous, active, first, last } = usePagination({
    page,
    siblings,
    total,
    onChange,
    initialPage,
    boundaries
  });
  const items = range.map((pageNumber, index) => /* @__PURE__ */ React.createElement(Item, {
    key: index,
    page: pageNumber,
    active: pageNumber === active,
    "aria-label": typeof getItemAriaLabel === "function" ? getItemAriaLabel(pageNumber) : null,
    tabIndex: pageNumber === "dots" ? -1 : 0,
    className: cx(classes.item, {
      [classes.active]: pageNumber === active,
      [classes.dots]: pageNumber === "dots"
    }),
    onClick: pageNumber !== "dots" ? () => setPage(pageNumber) : void 0
  }));
  return /* @__PURE__ */ React.createElement(Group, __spreadValues({
    spacing: spacing || theme.fn.size({ size, sizes: theme.spacing }) / 2,
    ref,
    sx
  }, others), withEdges && /* @__PURE__ */ React.createElement(Item, {
    page: "first",
    onClick: first,
    "aria-label": getItemAriaLabel ? getItemAriaLabel("first") : void 0,
    "aria-disabled": active === 1,
    className: classes.item,
    disabled: active === 1
  }), withControls && /* @__PURE__ */ React.createElement(Item, {
    page: "prev",
    onClick: previous,
    "aria-label": getItemAriaLabel ? getItemAriaLabel("prev") : void 0,
    "aria-disabled": active === 1,
    className: classes.item,
    disabled: active === 1
  }), items, withControls && /* @__PURE__ */ React.createElement(Item, {
    page: "next",
    onClick: next,
    "aria-label": getItemAriaLabel ? getItemAriaLabel("next") : void 0,
    "aria-disabled": active === total,
    className: classes.item,
    disabled: active === total
  }), withEdges && /* @__PURE__ */ React.createElement(Item, {
    page: "last",
    onClick: last,
    "aria-label": getItemAriaLabel ? getItemAriaLabel("last") : void 0,
    "aria-disabled": active === total,
    className: classes.item,
    disabled: active === total
  }));
});
Pagination.displayName = "@mantine/core/Pagination";

export { Pagination };
//# sourceMappingURL=Pagination.js.map
