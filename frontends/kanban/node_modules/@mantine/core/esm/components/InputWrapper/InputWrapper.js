import React, { forwardRef, createElement } from 'react';
import useStyles from './InputWrapper.styles.js';
import { Box } from '../Box/Box.js';
import { Text } from '../Text/Text.js';

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
const InputWrapper = forwardRef((_a, ref) => {
  var _b = _a, {
    className,
    label,
    children,
    required,
    id,
    error,
    description,
    labelElement = "label",
    labelProps,
    descriptionProps,
    errorProps,
    classNames,
    styles,
    size = "sm",
    __staticSelector = "InputWrapper"
  } = _b, others = __objRest(_b, [
    "className",
    "label",
    "children",
    "required",
    "id",
    "error",
    "description",
    "labelElement",
    "labelProps",
    "descriptionProps",
    "errorProps",
    "classNames",
    "styles",
    "size",
    "__staticSelector"
  ]);
  const { classes, cx } = useStyles({ size }, { classNames, styles, name: __staticSelector });
  const _labelProps = labelElement === "label" ? { htmlFor: id } : {};
  const inputLabel = createElement(labelElement, __spreadProps(__spreadValues(__spreadValues({}, _labelProps), labelProps), {
    id: id ? `${id}-label` : void 0,
    className: classes.label
  }), /* @__PURE__ */ React.createElement(React.Fragment, null, label, required && /* @__PURE__ */ React.createElement("span", {
    className: classes.required
  }, " *")));
  return /* @__PURE__ */ React.createElement(Box, __spreadValues({
    className: cx(classes.root, className),
    ref
  }, others), label && inputLabel, description && /* @__PURE__ */ React.createElement(Text, __spreadProps(__spreadValues({}, descriptionProps), {
    color: "gray",
    className: classes.description
  }), description), children, typeof error !== "boolean" && error && /* @__PURE__ */ React.createElement(Text, __spreadProps(__spreadValues({}, errorProps), {
    size,
    className: classes.error
  }), error));
});
InputWrapper.displayName = "@mantine/core/InputWrapper";

export { InputWrapper };
//# sourceMappingURL=InputWrapper.js.map
