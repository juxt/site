import React, { forwardRef } from 'react';
import { Text } from '../../Text/Text.js';
import { CloseButton } from '../../ActionIcon/CloseButton/CloseButton.js';
import useStyles from './PopoverBody.styles.js';

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
const PopoverBody = forwardRef((_a, ref) => {
  var _b = _a, {
    classNames,
    styles,
    shadow,
    spacing,
    radius,
    withCloseButton,
    title,
    titleId,
    bodyId,
    onClose,
    closeButtonLabel,
    children,
    width
  } = _b, others = __objRest(_b, [
    "classNames",
    "styles",
    "shadow",
    "spacing",
    "radius",
    "withCloseButton",
    "title",
    "titleId",
    "bodyId",
    "onClose",
    "closeButtonLabel",
    "children",
    "width"
  ]);
  const { classes } = useStyles({ shadow, radius, spacing, width }, { classNames, styles, name: "Popover" });
  return /* @__PURE__ */ React.createElement("div", __spreadValues({
    role: "dialog",
    tabIndex: -1,
    "aria-labelledby": titleId,
    "aria-describedby": bodyId,
    className: classes.wrapper,
    ref
  }, others), /* @__PURE__ */ React.createElement("div", {
    className: classes.popover
  }, /* @__PURE__ */ React.createElement("div", {
    className: classes.body
  }, !!title && /* @__PURE__ */ React.createElement("div", {
    className: classes.header
  }, /* @__PURE__ */ React.createElement(Text, {
    size: "sm",
    id: titleId,
    className: classes.title
  }, title)), withCloseButton && /* @__PURE__ */ React.createElement(CloseButton, {
    size: "sm",
    onClick: onClose,
    "aria-label": closeButtonLabel,
    className: classes.close
  }), /* @__PURE__ */ React.createElement("div", {
    className: classes.inner,
    id: bodyId
  }, children))));
});
PopoverBody.displayName = "@mantine/core/PopoverBody";

export { PopoverBody };
//# sourceMappingURL=PopoverBody.js.map
