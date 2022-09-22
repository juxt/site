'use strict';

Object.defineProperty(exports, '__esModule', { value: true });

var React = require('react');
var hooks = require('@mantine/hooks');
var styles = require('@mantine/styles');
var ColorPicker = require('../ColorPicker/ColorPicker.js');
var ColorInput_styles = require('./ColorInput.styles.js');
var parsers = require('../ColorPicker/converters/parsers.js');
var converters = require('../ColorPicker/converters/converters.js');
var InputWrapper = require('../InputWrapper/InputWrapper.js');
var Input = require('../Input/Input.js');
var ColorSwatch = require('../ColorSwatch/ColorSwatch.js');
var Popper = require('../Popper/Popper.js');
var Paper = require('../Paper/Paper.js');

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
const ColorInput = React.forwardRef((_a, ref) => {
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
    styles: styles$1,
    disallowInput = false,
    fixOnBlur = true,
    withPreview = true,
    swatchesPerRow = 10,
    withPicker = true,
    icon,
    transition = "pop-top-left",
    id,
    dropdownZIndex = styles.getDefaultZIndex("popover"),
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
  const { classes, cx, theme } = ColorInput_styles['default']({ disallowInput }, { classNames, styles: styles$1, name: "ColorInput" });
  const { margins, rest } = styles.extractMargins(others);
  const uuid = hooks.useUuid(id);
  const [referenceElement, setReferenceElement] = React.useState(null);
  const [dropdownOpened, setDropdownOpened] = React.useState(false);
  const [lastValidValue, setLastValidValue] = React.useState("");
  const [_value, setValue] = hooks.useUncontrolled({
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
  React.useEffect(() => {
    if (parsers.isColorValid(_value)) {
      setLastValidValue(_value);
    }
  }, [_value]);
  hooks.useDidUpdate(() => {
    if (parsers.isColorValid(_value)) {
      setValue(converters.convertHsvaTo(format, parsers.parseColor(_value)));
    }
  }, [format]);
  return /* @__PURE__ */ React__default.createElement(InputWrapper.InputWrapper, __spreadValues(__spreadValues({
    label,
    description,
    error,
    required,
    classNames,
    styles: styles$1,
    size,
    id: uuid,
    className,
    style,
    __staticSelector: "ColorInput",
    sx
  }, margins), wrapperProps), /* @__PURE__ */ React__default.createElement("div", {
    ref: setReferenceElement
  }, /* @__PURE__ */ React__default.createElement(Input.Input, __spreadProps(__spreadValues({}, rest), {
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
    icon: icon || (withPreview ? /* @__PURE__ */ React__default.createElement(ColorSwatch.ColorSwatch, {
      color: parsers.isColorValid(_value) ? _value : "#fff",
      size: theme.fn.size({ size, sizes: SWATCH_SIZES })
    }) : null),
    readOnly: disallowInput,
    classNames: __spreadProps(__spreadValues({}, classNames), { input: cx(classes.input, classNames == null ? void 0 : classNames.input) }),
    styles: styles$1
  }))), /* @__PURE__ */ React__default.createElement(Popper.Popper, {
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
  }, /* @__PURE__ */ React__default.createElement("div", {
    style: { pointerEvents: "all" }
  }, /* @__PURE__ */ React__default.createElement(Paper.Paper, {
    shadow: "sm",
    padding: size,
    className: classes.dropdownBody,
    onMouseDown: (event) => event.preventDefault()
  }, /* @__PURE__ */ React__default.createElement(ColorPicker.ColorPicker, {
    __staticSelector: "ColorInput",
    value: _value,
    onChange: setValue,
    format,
    swatches,
    swatchesPerRow,
    withPicker,
    size,
    focusable: false,
    styles: styles$1,
    classNames
  })))));
});
ColorInput.displayName = "@mantine/core/ColorInput";

exports.ColorInput = ColorInput;
//# sourceMappingURL=ColorInput.js.map
