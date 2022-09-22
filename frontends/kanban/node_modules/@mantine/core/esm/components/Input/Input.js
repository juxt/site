import React, { forwardRef } from 'react';
import { useMantineTheme, extractMargins } from '@mantine/styles';
import useStyles from './Input.styles.js';
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
const Input = forwardRef((_a, ref) => {
  var _b = _a, {
    component,
    className,
    invalid = false,
    required = false,
    disabled = false,
    variant,
    icon,
    style,
    rightSectionWidth = 36,
    iconWidth,
    rightSection,
    rightSectionProps = {},
    radius = "sm",
    size = "sm",
    wrapperProps,
    classNames,
    styles,
    __staticSelector = "Input",
    multiline = false,
    sx
  } = _b, others = __objRest(_b, [
    "component",
    "className",
    "invalid",
    "required",
    "disabled",
    "variant",
    "icon",
    "style",
    "rightSectionWidth",
    "iconWidth",
    "rightSection",
    "rightSectionProps",
    "radius",
    "size",
    "wrapperProps",
    "classNames",
    "styles",
    "__staticSelector",
    "multiline",
    "sx"
  ]);
  const theme = useMantineTheme();
  const _variant = variant || (theme.colorScheme === "dark" ? "filled" : "default");
  const { classes, cx } = useStyles({
    radius,
    size,
    multiline,
    variant: _variant,
    invalid,
    rightSectionWidth,
    iconWidth,
    withRightSection: !!rightSection
  }, { classNames, styles, name: __staticSelector });
  const { margins, rest } = extractMargins(others);
  const Element = component || "input";
  return /* @__PURE__ */ React.createElement(Box, __spreadValues(__spreadValues({
    className: cx(classes.wrapper, className),
    sx,
    style
  }, margins), wrapperProps), icon && /* @__PURE__ */ React.createElement("div", {
    className: classes.icon
  }, icon), /* @__PURE__ */ React.createElement(Element, __spreadProps(__spreadValues({}, rest), {
    ref,
    required,
    "aria-invalid": invalid,
    disabled,
    className: cx(classes[`${_variant}Variant`], classes.input, {
      [classes.withIcon]: icon,
      [classes.invalid]: invalid,
      [classes.disabled]: disabled
    })
  })), rightSection && /* @__PURE__ */ React.createElement("div", __spreadProps(__spreadValues({}, rightSectionProps), {
    className: classes.rightSection
  }), rightSection));
});
Input.displayName = "@mantine/core/Input";

export { Input };
//# sourceMappingURL=Input.js.map
