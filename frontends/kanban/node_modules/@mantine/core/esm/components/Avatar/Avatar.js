import React, { forwardRef, useState, useEffect } from 'react';
import { AvatarPlaceholderIcon } from './AvatarPlaceholderIcon.js';
import useStyles from './Avatar.styles.js';
import { Box } from '../Box/Box.js';

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
const Avatar = forwardRef((_a, ref) => {
  var _b = _a, {
    component,
    className,
    size = "md",
    src,
    alt,
    radius = "sm",
    children,
    color = "gray",
    classNames,
    styles,
    imageProps
  } = _b, others = __objRest(_b, [
    "component",
    "className",
    "size",
    "src",
    "alt",
    "radius",
    "children",
    "color",
    "classNames",
    "styles",
    "imageProps"
  ]);
  const { classes, cx } = useStyles({ color, radius, size }, { classNames, styles, name: "Avatar" });
  const [error, setError] = useState(!src);
  useEffect(() => {
    !src ? setError(true) : setError(false);
  }, [src]);
  return /* @__PURE__ */ React.createElement(Box, __spreadValues({
    component: component || "div",
    className: cx(classes.root, className),
    ref
  }, others), error ? /* @__PURE__ */ React.createElement("div", {
    className: classes.placeholder,
    title: alt
  }, children || /* @__PURE__ */ React.createElement(AvatarPlaceholderIcon, {
    className: classes.placeholderIcon
  })) : /* @__PURE__ */ React.createElement("img", __spreadProps(__spreadValues({}, imageProps), {
    className: classes.image,
    src,
    alt,
    onError: () => setError(true)
  })));
});
Avatar.displayName = "@mantine/core/Avatar";

export { Avatar };
//# sourceMappingURL=Avatar.js.map
