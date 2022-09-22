import React, { useState } from 'react';
import { useFocusTrap, useClickOutside, useFocusReturn, useUuid, useMergedRef } from '@mantine/hooks';
import { getDefaultZIndex } from '@mantine/styles';
import { PopoverBody } from './PopoverBody/PopoverBody.js';
import useStyles from './Popover.styles.js';
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
function Popover(_a) {
  var _b = _a, {
    className,
    children,
    target,
    title,
    onClose,
    opened,
    zIndex = getDefaultZIndex("popover"),
    arrowSize = 4,
    withArrow = false,
    transition = "fade",
    transitionDuration = 200,
    transitionTimingFunction,
    gutter = 10,
    position = "left",
    placement = "center",
    disabled = false,
    noClickOutside = false,
    noFocusTrap = false,
    noEscape = false,
    withCloseButton = false,
    radius = "sm",
    spacing = "md",
    shadow = "sm",
    closeButtonLabel,
    positionDependencies = [],
    withinPortal = true,
    id,
    classNames,
    styles,
    width,
    clickOutsideEvents = ["click", "touchstart"]
  } = _b, others = __objRest(_b, [
    "className",
    "children",
    "target",
    "title",
    "onClose",
    "opened",
    "zIndex",
    "arrowSize",
    "withArrow",
    "transition",
    "transitionDuration",
    "transitionTimingFunction",
    "gutter",
    "position",
    "placement",
    "disabled",
    "noClickOutside",
    "noFocusTrap",
    "noEscape",
    "withCloseButton",
    "radius",
    "spacing",
    "shadow",
    "closeButtonLabel",
    "positionDependencies",
    "withinPortal",
    "id",
    "classNames",
    "styles",
    "width",
    "clickOutsideEvents"
  ]);
  const { classes, cx } = useStyles(null, { classNames, styles, name: "Popover" });
  const handleClose = () => typeof onClose === "function" && onClose();
  const [referenceElement, setReferenceElement] = useState(null);
  const [rootElement, setRootElement] = useState(null);
  const [dropdownElement, setDropdownElement] = useState(null);
  const focusTrapRef = useFocusTrap(!noFocusTrap && opened);
  useClickOutside(() => !noClickOutside && handleClose(), clickOutsideEvents, [
    rootElement,
    dropdownElement
  ]);
  const returnFocus = useFocusReturn({
    opened: opened || noFocusTrap,
    transitionDuration,
    shouldReturnFocus: false
  });
  const handleKeydown = (event) => {
    if (!noEscape && event.nativeEvent.code === "Escape") {
      handleClose();
      window.setTimeout(returnFocus, 0);
    }
  };
  const uuid = useUuid(id);
  const titleId = `${uuid}-title`;
  const bodyId = `${uuid}-body`;
  return /* @__PURE__ */ React.createElement(Box, __spreadValues({
    className: cx(classes.root, className),
    id,
    ref: setRootElement
  }, others), /* @__PURE__ */ React.createElement(Popper, {
    referenceElement,
    transitionDuration,
    transition,
    mounted: opened && !disabled,
    position,
    placement,
    gutter,
    withArrow,
    arrowSize,
    zIndex,
    arrowClassName: classes.arrow,
    forceUpdateDependencies: [radius, shadow, spacing, ...positionDependencies],
    withinPortal
  }, /* @__PURE__ */ React.createElement(PopoverBody, {
    shadow,
    radius,
    spacing,
    withCloseButton,
    title,
    titleId,
    bodyId,
    closeButtonLabel,
    onClose: handleClose,
    ref: useMergedRef(focusTrapRef, setDropdownElement),
    onKeyDownCapture: handleKeydown,
    classNames,
    styles,
    width
  }, children)), /* @__PURE__ */ React.createElement("div", {
    className: classes.target,
    ref: setReferenceElement
  }, target));
}
Popover.displayName = "@mantine/core/Popover";

export { Popover };
//# sourceMappingURL=Popover.js.map
