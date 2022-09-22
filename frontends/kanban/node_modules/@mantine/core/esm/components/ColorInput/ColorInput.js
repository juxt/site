import React, { forwardRef, useState, useEffect } from 'react';
import { useUuid, useUncontrolled, useDidUpdate } from '@mantine/hooks';
import { getDefaultZIndex, extractMargins } from '@mantine/styles';
import { ColorPicker } from '../ColorPicker/ColorPicker.js';
import useStyles from './ColorInput.styles.js';
import { isColorValid, parseColor } from '../ColorPicker/converters/parsers.js';
import { convertHsvaTo } from '../ColorPicker/converters/converters.js';
import { InputWrapper } from '../InputWrapper/InputWrapper.js';
import { Input } from '../Input/Input.js';
import { ColorSwatch } from '../ColorSwatch/ColorSwatch.js';
import { Popper } from '../Popper/Popper.js';
import { Paper } from '../Paper/Paper.js';

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
const SWATCH_SIZES = {
  xs: 16,
  sm: 18,
  md: 22,
  lg: 28,
  xl: 36
};
const ARROW_OFFSET = {
  xs: 12,
  sm: 15,
  md: 17,
  lg: 21,
  xl: 25
};
const ColorInput = forwardRef((_a, ref) => {
  var _b = _a, {
    label,
    description,
    error,
    required,
    wrapperProps,
    size = "sm",
    format = "hex",
    onChange,
    onFocus,
    onBlur,
    value,
    defaultValue,
    classNames,
    styles,
    disallowInput = false,
    fixOnBlur = true,
    withPreview = true,
    swatchesPerRow = 10,
    withPicker = true,
    icon,
    transition = "pop-top-left",
    id,
    dropdownZIndex = getDefaultZIndex("popover"),
    transitionDuration = 0,
    transitionTimingFunction,
    withinPortal = true,
    className,
    style,
    swatches,
    sx
  } = _b, others = __objRest(_b, [
    "label",
    "description",
    "error",
    "required",
    "wrapperProps",
    "size",
    "format",
    "onChange",
    "onFocus",
    "onBlur",
    "value",
    "defaultValue",
    "classNames",
    "styles",
    "disallowInput",
    "fixOnBlur",
    "withPreview",
    "swatchesPerRow",
    "withPicker",
    "icon",
    "transition",
    "id",
    "dropdownZIndex",
    "transitionDuration",
    "transitionTimingFunction",
    "withinPortal",
    "className",
    "style",
    "swatches",
    "sx"
  ]);
  const { classes, cx, theme } = useStyles({ disallowInput }, { classNames, styles, name: "ColorInput" });
  const { margins, rest } = extractMargins(others);
  const uuid = useUuid(id);
  const [referenceElement, setReferenceElement] = useState(null);
  const [dropdownOpened, setDropdownOpened] = useState(false);
  const [lastValidValue, setLastValidValue] = useState("");
  const [_value, setValue] = useUncontrolled({
    value,
    defaultValue,
    finalValue: "",
    rule: (val) => !!val && val.trim().length > 0,
    onChange
  });
  const handleInputFocus = (event) => {
    typeof onFocus === "function" && onFocus(event);
    setDropdownOpened(true);
  };
  const handleInputBlur = (event) => {
    typeof onBlur === "function" && onBlur(event);
    setDropdownOpened(false);
    fixOnBlur && setValue(lastValidValue);
  };
  useEffect(() => {
    if (isColorValid(_value)) {
      setLastValidValue(_value);
    }
  }, [_value]);
  useDidUpdate(() => {
    if (isColorValid(_value)) {
      setValue(convertHsvaTo(format, parseColor(_value)));
    }
  }, [format]);
  return /* @__PURE__ */ React.createElement(InputWrapper, __spreadValues(__spreadValues({
    label,
    description,
    error,
    required,
    classNames,
    styles,
    size,
    id: uuid,
    className,
    style,
    __staticSelector: "ColorInput",
    sx
  }, margins), wrapperProps), /* @__PURE__ */ React.createElement("div", {
    ref: setReferenceElement
  }, /* @__PURE__ */ React.createElement(Input, __spreadProps(__spreadValues({}, rest), {
    ref,
    __staticSelector: "ColorInput",
    id: uuid,
    onFocus: handleInputFocus,
    onBlur: handleInputBlur,
    spellCheck: false,
    size,
    value: _value,
    onChange: (event) => setValue(event.currentTarget.value),
    invalid: !!error,
    required,
    autoComplete: "nope",
    icon: icon || (withPreview ? /* @__PURE__ */ React.createElement(ColorSwatch, {
      color: isColorValid(_value) ? _value : "#fff",
      size: theme.fn.size({ size, sizes: SWATCH_SIZES })
    }) : null),
    readOnly: disallowInput,
    classNames: __spreadProps(__spreadValues({}, classNames), { input: cx(classes.input, classNames == null ? void 0 : classNames.input) }),
    styles
  }))), /* @__PURE__ */ React.createElement(Popper, {
    referenceElement,
    transitionDuration,
    transitionTimingFunction,
    transition,
    mounted: dropdownOpened,
    position: "bottom",
    placement: "start",
    gutter: 5,
    arrowSize: 3,
    zIndex: dropdownZIndex,
    arrowStyle: { left: theme.fn.size({ size, sizes: ARROW_OFFSET }) },
    withinPortal
  }, /* @__PURE__ */ React.createElement("div", {
    style: { pointerEvents: "all" }
  }, /* @__PURE__ */ React.createElement(Paper, {
    shadow: "sm",
    padding: size,
    className: classes.dropdownBody,
    onMouseDown: (event) => event.preventDefault()
  }, /* @__PURE__ */ React.createElement(ColorPicker, {
    __staticSelector: "ColorInput",
    value: _value,
    onChange: setValue,
    format,
    swatches,
    swatchesPerRow,
    withPicker,
    size,
    focusable: false,
    styles,
    classNames
  })))));
});
ColorInput.displayName = "@mantine/core/ColorInput";

export { ColorInput };
//# sourceMappingURL=ColorInput.js.map
