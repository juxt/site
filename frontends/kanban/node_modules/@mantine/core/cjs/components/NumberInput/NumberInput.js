'use strict';

Object.defineProperty(exports, '__esModule', { value: true });

var React = require('react');
var hooks = require('@mantine/hooks');
var TextInput = require('../TextInput/TextInput.js');
var NumberInput_styles = require('./NumberInput.styles.js');

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
const defaultFormatter = (value) => value || "";
const defaultParser = (num) => {
  const parsedNum = parseFloat(num);
  if (Number.isNaN(parsedNum)) {
    return void 0;
  }
  return num;
};
const NumberInput = React.forwardRef((_a, ref) => {
  var _b = _a, {
    disabled,
    value,
    onChange,
    decimalSeparator,
    min,
    max,
    step = 1,
    stepHoldInterval,
    stepHoldDelay,
    onBlur,
    onFocus,
    hideControls = false,
    radius = "sm",
    variant,
    precision = 0,
    defaultValue,
    noClampOnBlur = false,
    handlersRef,
    classNames,
    styles,
    size,
    rightSection,
    formatter = defaultFormatter,
    parser = defaultParser
  } = _b, others = __objRest(_b, [
    "disabled",
    "value",
    "onChange",
    "decimalSeparator",
    "min",
    "max",
    "step",
    "stepHoldInterval",
    "stepHoldDelay",
    "onBlur",
    "onFocus",
    "hideControls",
    "radius",
    "variant",
    "precision",
    "defaultValue",
    "noClampOnBlur",
    "handlersRef",
    "classNames",
    "styles",
    "size",
    "rightSection",
    "formatter",
    "parser"
  ]);
  const { classes, cx, theme } = NumberInput_styles['default']({ radius, size }, { classNames, styles, name: "NumberInput" });
  const [focused, setFocused] = React.useState(false);
  const [_value, setValue] = React.useState(typeof value === "number" ? value : typeof defaultValue === "number" ? defaultValue : void 0);
  const finalValue = typeof value === "number" ? value : _value;
  const [tempValue, setTempValue] = React.useState(typeof finalValue === "number" ? finalValue.toFixed(precision) : "");
  const inputRef = React.useRef();
  const handleValueChange = (val) => {
    typeof onChange === "function" && onChange(val);
    setValue(val);
  };
  const formatNum = (val = "") => {
    let parsedStr = typeof val === "number" ? String(val) : val;
    if (decimalSeparator) {
      parsedStr = parsedStr.replace(/\./g, decimalSeparator);
    }
    return formatter(parsedStr);
  };
  const parseNum = (val) => {
    let num = val;
    if (decimalSeparator) {
      num = num.replace(new RegExp(`\\${decimalSeparator}`, "g"), ".");
    }
    return parser(num);
  };
  const _min = typeof min === "number" ? min : -Infinity;
  const _max = typeof max === "number" ? max : Infinity;
  const incrementRef = React.useRef();
  incrementRef.current = () => {
    var _a2;
    if (_value === void 0) {
      handleValueChange(min != null ? min : 0);
      setTempValue((_a2 = min == null ? void 0 : min.toFixed(precision)) != null ? _a2 : "0");
    } else {
      const result = hooks.clamp({ value: _value + step, min: _min, max: _max }).toFixed(precision);
      handleValueChange(parseFloat(result));
      setTempValue(result);
    }
  };
  const decrementRef = React.useRef();
  decrementRef.current = () => {
    var _a2;
    if (_value === void 0) {
      handleValueChange(min != null ? min : 0);
      setTempValue((_a2 = min == null ? void 0 : min.toFixed(precision)) != null ? _a2 : "0");
    } else {
      const result = hooks.clamp({ value: _value - step, min: _min, max: _max }).toFixed(precision);
      handleValueChange(parseFloat(result));
      setTempValue(result);
    }
  };
  hooks.assignRef(handlersRef, { increment: incrementRef.current, decrement: decrementRef.current });
  React.useEffect(() => {
    if (typeof value === "number" && !focused) {
      setValue(value);
      setTempValue(value.toFixed(precision));
    }
    if (defaultValue === void 0 && value === void 0 && !focused) {
      setValue(value);
      setTempValue("");
    }
  }, [value]);
  const shouldUseStepInterval = stepHoldDelay !== void 0 && stepHoldInterval !== void 0;
  const onStepTimeoutRef = React.useRef(null);
  const stepCountRef = React.useRef(0);
  const onStepDone = () => {
    if (onStepTimeoutRef.current) {
      window.clearTimeout(onStepTimeoutRef.current);
    }
    onStepTimeoutRef.current = null;
    stepCountRef.current = 0;
  };
  const onStepHandleChange = (isIncrement) => {
    if (isIncrement) {
      incrementRef.current();
    } else {
      decrementRef.current();
    }
    stepCountRef.current += 1;
  };
  const onStepLoop = (isIncrement) => {
    onStepHandleChange(isIncrement);
    if (shouldUseStepInterval) {
      const interval = typeof stepHoldInterval === "number" ? stepHoldInterval : stepHoldInterval(stepCountRef.current);
      onStepTimeoutRef.current = window.setTimeout(() => onStepLoop(isIncrement), interval);
    }
  };
  const onStep = (event, isIncrement) => {
    event.preventDefault();
    onStepHandleChange(isIncrement);
    if (shouldUseStepInterval) {
      onStepTimeoutRef.current = window.setTimeout(() => onStepLoop(isIncrement), stepHoldDelay);
    }
    inputRef.current.focus();
  };
  React.useEffect(() => {
    onStepDone();
    return onStepDone;
  }, []);
  const controls = /* @__PURE__ */ React__default.createElement("div", {
    className: classes.rightSection
  }, /* @__PURE__ */ React__default.createElement("button", {
    type: "button",
    tabIndex: -1,
    "aria-hidden": true,
    disabled: finalValue >= max,
    className: cx(classes.control, classes.controlUp),
    onMouseDown: (event) => {
      onStep(event, true);
    },
    onMouseUp: onStepDone,
    onMouseLeave: onStepDone
  }), /* @__PURE__ */ React__default.createElement("button", {
    type: "button",
    tabIndex: -1,
    "aria-hidden": true,
    disabled: finalValue <= min,
    className: cx(classes.control, classes.controlDown),
    onMouseDown: (event) => {
      onStep(event, false);
    },
    onMouseUp: onStepDone,
    onMouseLeave: onStepDone
  }));
  const handleChange = (event) => {
    const val = event.target.value;
    const parsed = parseNum(val);
    setTempValue(parsed);
    if (val === "") {
      handleValueChange(void 0);
    } else {
      val.trim() !== "" && !Number.isNaN(parsed) && handleValueChange(parseFloat(parsed));
    }
  };
  const handleBlur = (event) => {
    var _a2;
    if (event.target.value === "") {
      setTempValue("");
      handleValueChange(void 0);
    } else {
      const parsedVal = parseNum(event.target.value);
      const val = hooks.clamp({ value: parseFloat(parsedVal), min: _min, max: _max });
      if (!Number.isNaN(val)) {
        if (!noClampOnBlur) {
          setTempValue(val.toFixed(precision));
          handleValueChange(parseFloat(val.toFixed(precision)));
        }
      } else {
        setTempValue((_a2 = finalValue == null ? void 0 : finalValue.toFixed(precision)) != null ? _a2 : "");
      }
    }
    setFocused(false);
    typeof onBlur === "function" && onBlur(event);
  };
  const handleFocus = (event) => {
    setFocused(true);
    typeof onFocus === "function" && onFocus(event);
  };
  const handleKeyDown = (event) => {
    if (event.repeat && shouldUseStepInterval) {
      event.preventDefault();
      return;
    }
    if (event.key === "ArrowUp") {
      onStep(event, true);
    } else if (event.key === "ArrowDown") {
      onStep(event, false);
    }
  };
  const handleKeyUp = (event) => {
    if (event.key === "ArrowUp" || event.key === "ArrowDown") {
      onStepDone();
    }
  };
  return /* @__PURE__ */ React__default.createElement(TextInput.TextInput, __spreadProps(__spreadValues({}, others), {
    variant,
    value: formatNum(tempValue),
    disabled,
    ref: hooks.useMergedRef(inputRef, ref),
    type: "text",
    onChange: handleChange,
    onBlur: handleBlur,
    onFocus: handleFocus,
    onKeyDown: handleKeyDown,
    onKeyUp: handleKeyUp,
    rightSection: rightSection || (disabled || hideControls || variant === "unstyled" ? null : controls),
    rightSectionWidth: theme.fn.size({ size, sizes: NumberInput_styles.CONTROL_SIZES }) + 1,
    radius,
    max,
    min,
    step,
    size,
    styles,
    classNames,
    inputMode: Number.isInteger(step) && precision === 0 ? "numeric" : "decimal",
    __staticSelector: "NumberInput"
  }));
});
NumberInput.displayName = "@mantine/core/NumberInput";

exports.NumberInput = NumberInput;
//# sourceMappingURL=NumberInput.js.map
