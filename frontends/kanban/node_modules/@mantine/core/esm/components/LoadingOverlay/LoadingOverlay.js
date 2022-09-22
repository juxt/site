import React, { forwardRef } from 'react';
import { getDefaultZIndex } from '@mantine/styles';
import useStyles from './LoadingOverlay.styles.js';
import { Transition } from '../Transition/Transition.js';
import { Box } from '../Box/Box.js';
import { Loader } from '../Loader/Loader.js';
import { Overlay } from '../Overlay/Overlay.js';

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
const LoadingOverlay = forwardRef((_a, ref) => {
  var _b = _a, {
    className,
    visible,
    loaderProps = {},
    overlayOpacity = 0.75,
    overlayColor,
    transitionDuration = 200,
    zIndex = getDefaultZIndex("overlay"),
    style,
    loader,
    radius,
    classNames,
    styles
  } = _b, others = __objRest(_b, [
    "className",
    "visible",
    "loaderProps",
    "overlayOpacity",
    "overlayColor",
    "transitionDuration",
    "zIndex",
    "style",
    "loader",
    "radius",
    "classNames",
    "styles"
  ]);
  const { classes, cx, theme } = useStyles(null, { name: "LoadingOverlay", classNames, styles });
  return /* @__PURE__ */ React.createElement(Transition, {
    duration: transitionDuration,
    mounted: visible,
    transition: "fade"
  }, (transitionStyles) => /* @__PURE__ */ React.createElement(Box, __spreadValues({
    className: cx(classes.root, className),
    style: __spreadProps(__spreadValues(__spreadValues({}, transitionStyles), style), { zIndex }),
    ref
  }, others), loader ? /* @__PURE__ */ React.createElement("div", {
    style: { zIndex: zIndex + 1 }
  }, loader) : /* @__PURE__ */ React.createElement(Loader, __spreadValues({
    style: { zIndex: zIndex + 1 }
  }, loaderProps)), /* @__PURE__ */ React.createElement(Overlay, {
    opacity: overlayOpacity,
    zIndex,
    radius,
    color: overlayColor || (theme.colorScheme === "dark" ? theme.colors.dark[5] : theme.white)
  })));
});
LoadingOverlay.displayName = "@mantine/core/LoadingOverlay";

export { LoadingOverlay };
//# sourceMappingURL=LoadingOverlay.js.map
