import React, { forwardRef, useRef, useState, useEffect } from 'react';
import { useUncontrolled, useDidUpdate } from '@mantine/hooks';
import { ColorSwatch } from '../ColorSwatch/ColorSwatch.js';
import { HueSlider } from './HueSlider/HueSlider.js';
import { AlphaSlider } from './AlphaSlider/AlphaSlider.js';
import { Saturation } from './Saturation/Saturation.js';
import { Swatches } from './Swatches/Swatches.js';
import useStyles from './ColorPicker.styles.js';
import { isColorValid, parseColor } from './converters/parsers.js';
import { convertHsvaTo } from './converters/converters.js';
import { Box } from '../Box/Box.js';

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
const SWATCH_SIZES = {
  xs: 26,
  sm: 34,
  md: 42,
  lg: 50,
  xl: 54
};
const ColorPicker = forwardRef((_a, ref) => {
  var _b = _a, {
    value,
    defaultValue,
    onChange,
    format,
    swatches,
    swatchesPerRow = 10,
    size = "sm",
    withPicker = true,
    fullWidth = false,
    focusable = true,
    __staticSelector = "ColorPicker",
    saturationLabel,
    hueLabel,
    alphaLabel,
    className,
    styles,
    classNames
  } = _b, others = __objRest(_b, [
    "value",
    "defaultValue",
    "onChange",
    "format",
    "swatches",
    "swatchesPerRow",
    "size",
    "withPicker",
    "fullWidth",
    "focusable",
    "__staticSelector",
    "saturationLabel",
    "hueLabel",
    "alphaLabel",
    "className",
    "styles",
    "classNames"
  ]);
  const { classes, cx, theme } = useStyles({ size, fullWidth }, { classNames, styles, name: __staticSelector });
  const formatRef = useRef(format);
  const valueRef = useRef(null);
  const withAlpha = format === "rgba" || format === "hsla";
  const [shouldSkip, setShouldSkip] = useState(false);
  const [_value, setValue] = useUncontrolled({
    value,
    defaultValue,
    finalValue: "#FFFFFF",
    rule: (val) => isColorValid(val),
    onChange
  });
  const [parsed, setParsed] = useState(parseColor(_value));
  const handleChange = (color) => {
    setShouldSkip(true);
    setParsed((current) => {
      const next = __spreadValues(__spreadValues({}, current), color);
      valueRef.current = convertHsvaTo(formatRef.current, next);
      return next;
    });
    Promise.resolve().then(() => setValue(valueRef.current)).then(() => setShouldSkip(false));
  };
  useEffect(() => {
    if (isColorValid(value) && !shouldSkip) {
      setParsed(parseColor(value));
    }
  }, [value]);
  useDidUpdate(() => {
    formatRef.current = format;
    setValue(convertHsvaTo(format, parsed));
  }, [format]);
  return /* @__PURE__ */ React.createElement(Box, __spreadValues({
    className: cx(classes.wrapper, className),
    ref
  }, others), withPicker && /* @__PURE__ */ React.createElement(React.Fragment, null, /* @__PURE__ */ React.createElement(Saturation, {
    value: parsed,
    onChange: handleChange,
    color: _value,
    styles,
    classNames,
    size,
    focusable,
    saturationLabel,
    __staticSelector
  }), /* @__PURE__ */ React.createElement("div", {
    className: classes.body
  }, /* @__PURE__ */ React.createElement("div", {
    className: classes.sliders
  }, /* @__PURE__ */ React.createElement(HueSlider, {
    value: parsed.h,
    onChange: (h) => handleChange({ h }),
    size,
    styles,
    classNames,
    focusable,
    "aria-label": hueLabel,
    __staticSelector
  }), withAlpha && /* @__PURE__ */ React.createElement(AlphaSlider, {
    value: parsed.a,
    onChange: (a) => handleChange({ a }),
    size,
    color: convertHsvaTo("hex", parsed),
    style: { marginTop: 6 },
    styles,
    classNames,
    focusable,
    "aria-label": alphaLabel,
    __staticSelector
  })), withAlpha && /* @__PURE__ */ React.createElement(ColorSwatch, {
    color: _value,
    radius: "sm",
    size: theme.fn.size({ size, sizes: SWATCH_SIZES }),
    className: classes.preview
  }))), Array.isArray(swatches) && /* @__PURE__ */ React.createElement(Swatches, {
    data: swatches,
    onSelect: handleChange,
    style: { marginTop: 5 },
    swatchesPerRow,
    focusable,
    classNames,
    styles,
    __staticSelector
  }));
});
ColorPicker.displayName = "@mantine/core/ColorPicker";

export { ColorPicker };
//# sourceMappingURL=ColorPicker.js.map
