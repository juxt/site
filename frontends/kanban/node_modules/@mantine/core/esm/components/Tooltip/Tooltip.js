import React, { forwardRef, useRef, useState } from 'react';
import { getDefaultZIndex } from '@mantine/styles';
import { mergeRefs } from '@mantine/hooks';
import useStyles from './Tooltip.styles.js';
import { Box } from '../Box/Box.js';
import { Popper } from '../Popper/Popper.js';

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
const Tooltip = forwardRef((_a, ref) => {
  var _b = _a, {
    className,
    label,
    children,
    opened,
    delay = 0,
    gutter = 5,
    color = "gray",
    radius = "sm",
    disabled = false,
    withArrow = false,
    arrowSize = 2,
    position = "top",
    placement = "center",
    transition = "pop-top-left",
    transitionDuration = 100,
    zIndex = getDefaultZIndex("popover"),
    transitionTimingFunction,
    width = "auto",
    wrapLines = false,
    allowPointerEvents = false,
    positionDependencies = [],
    withinPortal = true,
    tooltipRef,
    tooltipId,
    classNames,
    styles,
    onMouseLeave,
    onMouseEnter
  } = _b, others = __objRest(_b, [
    "className",
    "label",
    "children",
    "opened",
    "delay",
    "gutter",
    "color",
    "radius",
    "disabled",
    "withArrow",
    "arrowSize",
    "position",
    "placement",
    "transition",
    "transitionDuration",
    "zIndex",
    "transitionTimingFunction",
    "width",
    "wrapLines",
    "allowPointerEvents",
    "positionDependencies",
    "withinPortal",
    "tooltipRef",
    "tooltipId",
    "classNames",
    "styles",
    "onMouseLeave",
    "onMouseEnter"
  ]);
  const { classes, cx } = useStyles({ color, radius }, { classNames, styles, name: "Tooltip" });
  const timeoutRef = useRef();
  const [_opened, setOpened] = useState(false);
  const visible = (typeof opened === "boolean" ? opened : _opened) && !disabled;
  const [referenceElement, setReferenceElement] = useState(null);
  const handleOpen = () => {
    window.clearTimeout(timeoutRef.current);
    setOpened(true);
  };
  const handleClose = () => {
    if (delay !== 0) {
      timeoutRef.current = window.setTimeout(() => {
        setOpened(false);
      }, delay);
    } else {
      setOpened(false);
    }
  };
  return /* @__PURE__ */ React.createElement(Box, __spreadValues({
    className: cx(classes.root, className),
    onMouseEnter: (event) => {
      handleOpen();
      typeof onMouseEnter === "function" && onMouseEnter(event);
    },
    onMouseLeave: (event) => {
      handleClose();
      typeof onMouseLeave === "function" && onMouseLeave(event);
    },
    onFocusCapture: handleOpen,
    onBlurCapture: handleClose,
    ref: mergeRefs(setReferenceElement, ref)
  }, others), /* @__PURE__ */ React.createElement(Popper, {
    referenceElement,
    transitionDuration,
    transition,
    mounted: visible,
    position,
    placement,
    gutter,
    withArrow,
    arrowSize,
    arrowDistance: 7,
    zIndex,
    arrowClassName: classes.arrow,
    forceUpdateDependencies: [color, radius, ...positionDependencies],
    withinPortal
  }, /* @__PURE__ */ React.createElement(Box, {
    className: classes.body,
    ref: tooltipRef,
    sx: {
      pointerEvents: allowPointerEvents ? "all" : "none",
      whiteSpace: wrapLines ? "normal" : "nowrap",
      width
    }
  }, label)), children);
});
Tooltip.displayName = "@mantine/core/Tooltip";

export { Tooltip };
//# sourceMappingURL=Tooltip.js.map
