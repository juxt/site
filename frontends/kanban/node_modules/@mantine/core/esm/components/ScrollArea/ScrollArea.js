import React, { forwardRef, useState } from 'react';
import * as RadixScrollArea from '@radix-ui/react-scroll-area';
import { useMantineTheme } from '@mantine/styles';
import useStyles from './ScrollArea.styles.js';
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
const ScrollArea = forwardRef((_a, ref) => {
  var _b = _a, {
    children,
    className,
    classNames,
    styles,
    scrollbarSize = 12,
    scrollHideDelay = 1e3,
    type = "hover",
    dir,
    offsetScrollbars = false,
    viewportRef,
    onScrollPositionChange
  } = _b, others = __objRest(_b, [
    "children",
    "className",
    "classNames",
    "styles",
    "scrollbarSize",
    "scrollHideDelay",
    "type",
    "dir",
    "offsetScrollbars",
    "viewportRef",
    "onScrollPositionChange"
  ]);
  const [scrollbarHovered, setScrollbarHovered] = useState(false);
  const theme = useMantineTheme();
  const { classes, cx } = useStyles({ scrollbarSize, offsetScrollbars, scrollbarHovered }, { name: "ScrollArea", classNames, styles });
  return /* @__PURE__ */ React.createElement(RadixScrollArea.Root, {
    type,
    scrollHideDelay,
    dir: dir || theme.dir,
    ref,
    asChild: true
  }, /* @__PURE__ */ React.createElement(Box, __spreadValues({
    className: cx(classes.root, className)
  }, others), /* @__PURE__ */ React.createElement(RadixScrollArea.Viewport, {
    className: classes.viewport,
    ref: viewportRef,
    onScroll: typeof onScrollPositionChange === "function" ? ({ currentTarget }) => onScrollPositionChange({
      x: currentTarget.scrollLeft,
      y: currentTarget.scrollTop
    }) : void 0
  }, children), /* @__PURE__ */ React.createElement(RadixScrollArea.Scrollbar, {
    orientation: "horizontal",
    className: classes.scrollbar,
    forceMount: true,
    onMouseEnter: () => setScrollbarHovered(true),
    onMouseLeave: () => setScrollbarHovered(false)
  }, /* @__PURE__ */ React.createElement(RadixScrollArea.Thumb, {
    className: classes.thumb
  })), /* @__PURE__ */ React.createElement(RadixScrollArea.Scrollbar, {
    orientation: "vertical",
    className: classes.scrollbar,
    forceMount: true,
    onMouseEnter: () => setScrollbarHovered(true),
    onMouseLeave: () => setScrollbarHovered(false)
  }, /* @__PURE__ */ React.createElement(RadixScrollArea.Thumb, {
    className: classes.thumb
  })), /* @__PURE__ */ React.createElement(RadixScrollArea.Corner, {
    className: classes.corner
  })));
});
ScrollArea.displayName = "@mantine/core/ScrollArea";

export { ScrollArea };
//# sourceMappingURL=ScrollArea.js.map
