'use strict';

Object.defineProperty(exports, '__esModule', { value: true });

var React = require('react');
var hooks = require('@mantine/hooks');
var styles = require('@mantine/styles');
var InputWrapper = require('../InputWrapper/InputWrapper.js');
var Input = require('../Input/Input.js');
var getSelectRightSectionProps = require('../Select/SelectRightSection/get-select-right-section-props.js');

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
const NativeSelect = React.forwardRef((_a, ref) => {
  var _b = _a, {
    id,
    className,
    required,
    label,
    error,
    style,
    data,
    placeholder,
    wrapperProps,
    inputStyle,
    description,
    defaultValue,
    onChange,
    value,
    classNames,
    styles: styles$1,
    size = "sm",
    rightSection,
    rightSectionWidth,
    sx
  } = _b, others = __objRest(_b, [
    "id",
    "className",
    "required",
    "label",
    "error",
    "style",
    "data",
    "placeholder",
    "wrapperProps",
    "inputStyle",
    "description",
    "defaultValue",
    "onChange",
    "value",
    "classNames",
    "styles",
    "size",
    "rightSection",
    "rightSectionWidth",
    "sx"
  ]);
  const uuid = hooks.useUuid(id);
  const theme = styles.useMantineTheme();
  const { margins, rest } = styles.extractMargins(others);
  const formattedData = data.map((item) => typeof item === "string" ? { label: item, value: item } : item);
  const options = formattedData.map((item) => /* @__PURE__ */ React__default.createElement("option", {
    key: item.value,
    value: item.value,
    disabled: item.disabled
  }, item.label));
  if (placeholder) {
    options.unshift(/* @__PURE__ */ React__default.createElement("option", {
      key: "placeholder",
      value: "",
      disabled: true,
      hidden: true
    }, placeholder));
  }
  return /* @__PURE__ */ React__default.createElement(InputWrapper.InputWrapper, __spreadValues(__spreadValues({
    required,
    id: uuid,
    label,
    error,
    className,
    style,
    description,
    size,
    styles: styles$1,
    classNames,
    sx,
    __staticSelector: "NativeSelect"
  }, margins), wrapperProps), /* @__PURE__ */ React__default.createElement(Input.Input, __spreadValues(__spreadProps(__spreadValues({}, rest), {
    onChange,
    component: "select",
    invalid: !!error,
    style: inputStyle,
    "aria-required": required,
    ref,
    id: uuid,
    required,
    value: value === null ? "" : value,
    size,
    classNames,
    __staticSelector: "NativeSelect"
  }), getSelectRightSectionProps.getSelectRightSectionProps({
    theme,
    rightSection,
    rightSectionWidth,
    styles: styles$1,
    shouldClear: false,
    size,
    error
  })), options));
});
NativeSelect.displayName = "@mantine/core/NativeSelect";

exports.NativeSelect = NativeSelect;
//# sourceMappingURL=NativeSelect.js.map
