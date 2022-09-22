'use strict';

Object.defineProperty(exports, '__esModule', { value: true });

var React = require('react');
var hooks = require('@mantine/hooks');
var styles = require('@mantine/styles');
var Input = require('../Input/Input.js');
var InputWrapper = require('../InputWrapper/InputWrapper.js');

function _interopDefaultLegacy (e) { return e && typeof e === 'object' && 'default' in e ? e['default'] : e; }

var React__default = /*#__PURE__*/_interopDefaultLegacy(React);

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
const TextInput = React.forwardRef((_a, ref) => {
  var _b = _a, {
    className,
    id,
    label,
    error,
    required,
    type = "text",
    style,
    icon,
    description,
    wrapperProps,
    size = "sm",
    classNames,
    styles: styles$1,
    __staticSelector = "TextInput",
    sx
  } = _b, others = __objRest(_b, [
    "className",
    "id",
    "label",
    "error",
    "required",
    "type",
    "style",
    "icon",
    "description",
    "wrapperProps",
    "size",
    "classNames",
    "styles",
    "__staticSelector",
    "sx"
  ]);
  const uuid = hooks.useUuid(id);
  const { margins, rest } = styles.extractMargins(others);
  return /* @__PURE__ */ React__default.createElement(InputWrapper.InputWrapper, __spreadValues(__spreadValues({
    required,
    id: uuid,
    label,
    error,
    description,
    size,
    className,
    style,
    classNames,
    styles: styles$1,
    __staticSelector,
    sx
  }, margins), wrapperProps), /* @__PURE__ */ React__default.createElement(Input.Input, __spreadProps(__spreadValues({}, rest), {
    required,
    ref,
    id: uuid,
    type,
    invalid: !!error,
    icon,
    size,
    classNames,
    styles: styles$1,
    __staticSelector
  })));
});
TextInput.displayName = "@mantine/core/TextInput";

exports.TextInput = TextInput;
//# sourceMappingURL=TextInput.js.map
