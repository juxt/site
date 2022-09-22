'use strict';

Object.defineProperty(exports, '__esModule', { value: true });

var React = require('react');
var hooks = require('@mantine/hooks');
var ColorSwatch = require('../ColorSwatch/ColorSwatch.js');
var HueSlider = require('./HueSlider/HueSlider.js');
var AlphaSlider = require('./AlphaSlider/AlphaSlider.js');
var Saturation = require('./Saturation/Saturation.js');
var Swatches = require('./Swatches/Swatches.js');
var ColorPicker_styles = require('./ColorPicker.styles.js');
var parsers = require('./converters/parsers.js');
var converters = require('./converters/converters.js');
var Box = require('../Box/Box.js');

function _interopDefaultLegacy (e) { return e && typeof e === 'object' && 'default' in e ? e['default'] : e; }

var React__default = /*#__PURE__*/_interopDefaultLegacy(React);

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
const ColorPicker = React.forwardRef((_a, ref) => {
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
  const { classes, cx, theme } = ColorPicker_styles['default']({ size, fullWidth }, { classNames, styles, name: __staticSelector });
  const formatRef = React.useRef(format);
  const valueRef = React.useRef(null);
  const withAlpha = format === "rgba" || format === "hsla";
  const [shouldSkip, setShouldSkip] = React.useState(false);
  const [_value, setValue] = hooks.useUncontrolled({
    value,
    defaultValue,
    finalValue: "#FFFFFF",
    rule: (val) => parsers.isColorValid(val),
    onChange
  });
  const [parsed, setParsed] = React.useState(parsers.parseColor(_value));
  const handleChange = (color) => {
    setShouldSkip(true);
    setParsed((current) => {
      const next = __spreadValues(__spreadValues({}, current), color);
      valueRef.current = converters.convertHsvaTo(formatRef.current, next);
      return next;
    });
    Promise.resolve().then(() => setValue(valueRef.current)).then(() => setShouldSkip(false));
  };
  React.useEffect(() => {
    if (parsers.isColorValid(value) && !shouldSkip) {
      setParsed(parsers.parseColor(value));
    }
  }, [value]);
  hooks.useDidUpdate(() => {
    formatRef.current = format;
    setValue(converters.convertHsvaTo(format, parsed));
  }, [format]);
  return /* @__PURE__ */ React__default.createElement(Box.Box, __spreadValues({
    className: cx(classes.wrapper, className),
    ref
  }, others), withPicker && /* @__PURE__ */ React__default.createElement(React__default.Fragment, null, /* @__PURE__ */ React__default.createElement(Saturation.Saturation, {
    value: parsed,
    onChange: handleChange,
    color: _value,
    styles,
    classNames,
    size,
    focusable,
    saturationLabel,
    __staticSelector
  }), /* @__PURE__ */ React__default.createElement("div", {
    className: classes.body
  }, /* @__PURE__ */ React__default.createElement("div", {
    className: classes.sliders
  }, /* @__PURE__ */ React__default.createElement(HueSlider.HueSlider, {
    value: parsed.h,
    onChange: (h) => handleChange({ h }),
    size,
    styles,
    classNames,
    focusable,
    "aria-label": hueLabel,
    __staticSelector
  }), withAlpha && /* @__PURE__ */ React__default.createElement(AlphaSlider.AlphaSlider, {
    value: parsed.a,
    onChange: (a) => handleChange({ a }),
    size,
    color: converters.convertHsvaTo("hex", parsed),
    style: { marginTop: 6 },
    styles,
    classNames,
    focusable,
    "aria-label": alphaLabel,
    __staticSelector
  })), withAlpha && /* @__PURE__ */ React__default.createElement(ColorSwatch.ColorSwatch, {
    color: _value,
    radius: "sm",
    size: theme.fn.size({ size, sizes: SWATCH_SIZES }),
    className: classes.preview
  }))), Array.isArray(swatches) && /* @__PURE__ */ React__default.createElement(Swatches.Swatches, {
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

exports.ColorPicker = ColorPicker;
//# sourceMappingURL=ColorPicker.js.map
