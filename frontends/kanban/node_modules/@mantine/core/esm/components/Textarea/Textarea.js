import React, { forwardRef } from 'react';
import TextareaAutosize from 'react-textarea-autosize';
import { useUuid } from '@mantine/hooks';
import { extractMargins } from '@mantine/styles';
import { InputWrapper } from '../InputWrapper/InputWrapper.js';
import { Input } from '../Input/Input.js';
import useStyles from './Textarea.styles.js';

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
const Textarea = forwardRef((_a, ref) => {
  var _b = _a, {
    autosize = false,
    maxRows,
    minRows,
    label,
    error,
    description,
    id,
    className,
    required,
    style,
    wrapperProps,
    classNames,
    styles,
    size = "sm",
    __staticSelector = "Textarea",
    sx
  } = _b, others = __objRest(_b, [
    "autosize",
    "maxRows",
    "minRows",
    "label",
    "error",
    "description",
    "id",
    "className",
    "required",
    "style",
    "wrapperProps",
    "classNames",
    "styles",
    "size",
    "__staticSelector",
    "sx"
  ]);
  const uuid = useUuid(id);
  const { classes, cx } = useStyles();
  const { margins, rest } = extractMargins(others);
  const sharedProps = __spreadValues({
    required,
    ref,
    invalid: !!error,
    id: uuid,
    classNames: __spreadProps(__spreadValues({}, classNames), { input: cx(classes.input, classNames == null ? void 0 : classNames.input) }),
    styles,
    __staticSelector,
    size,
    multiline: true
  }, rest);
  return /* @__PURE__ */ React.createElement(InputWrapper, __spreadValues(__spreadValues({
    label,
    error,
    id: uuid,
    description,
    required,
    style,
    className,
    classNames,
    styles,
    size,
    __staticSelector,
    sx
  }, margins), wrapperProps), autosize ? /* @__PURE__ */ React.createElement(Input, __spreadProps(__spreadValues({}, sharedProps), {
    component: TextareaAutosize,
    maxRows,
    minRows
  })) : /* @__PURE__ */ React.createElement(Input, __spreadProps(__spreadValues({}, sharedProps), {
    component: "textarea",
    rows: minRows
  })));
});
Textarea.displayName = "@mantine/core/Textarea";

export { Textarea };
//# sourceMappingURL=Textarea.js.map
