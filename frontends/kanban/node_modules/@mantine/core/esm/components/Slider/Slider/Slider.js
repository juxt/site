import React, { forwardRef, useState, useRef } from 'react';
import { useUncontrolled, clamp, useMove, useMergedRef } from '@mantine/hooks';
import { useMantineTheme } from '@mantine/styles';
import { getPosition } from '../utils/get-position/get-position.js';
import { getChangeValue } from '../utils/get-change-value/get-change-value.js';
import { Thumb } from '../Thumb/Thumb.js';
import { Track } from '../Track/Track.js';
import { SliderRoot } from '../SliderRoot/SliderRoot.js';

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
const Slider = forwardRef((_a, ref) => {
  var _b = _a, {
    classNames,
    styles,
    color,
    value,
    onChange,
    size = "md",
    radius = "xl",
    min = 0,
    max = 100,
    step = 1,
    defaultValue,
    name,
    marks = [],
    label = (f) => f,
    labelTransition = "skew-down",
    labelTransitionDuration = 0,
    labelTransitionTimingFunction,
    labelAlwaysOn = false,
    thumbLabel = "",
    showLabelOnHover = true,
    thumbChildren
  } = _b, others = __objRest(_b, [
    "classNames",
    "styles",
    "color",
    "value",
    "onChange",
    "size",
    "radius",
    "min",
    "max",
    "step",
    "defaultValue",
    "name",
    "marks",
    "label",
    "labelTransition",
    "labelTransitionDuration",
    "labelTransitionTimingFunction",
    "labelAlwaysOn",
    "thumbLabel",
    "showLabelOnHover",
    "thumbChildren"
  ]);
  const theme = useMantineTheme();
  const [hovered, setHovered] = useState(false);
  const [_value, setValue] = useUncontrolled({
    value: typeof value === "number" ? clamp({ value, min, max }) : value,
    defaultValue: typeof defaultValue === "number" ? clamp({ value: defaultValue, min, max }) : defaultValue,
    finalValue: clamp({ value: 0, min, max }),
    rule: (val) => typeof val === "number",
    onChange
  });
  const thumb = useRef();
  const position = getPosition({ value: _value, min, max });
  const _label = typeof label === "function" ? label(_value) : label;
  const handleChange = (val) => {
    const nextValue = getChangeValue({ value: val, min, max, step });
    setValue(nextValue);
  };
  const { ref: container, active } = useMove(({ x }) => handleChange(x), void 0, theme.dir);
  function handleThumbMouseDown(event) {
    if (event.cancelable) {
      event.preventDefault();
      event.stopPropagation();
    }
  }
  const handleTrackKeydownCapture = (event) => {
    switch (event.nativeEvent.code) {
      case "ArrowUp": {
        event.preventDefault();
        thumb.current.focus();
        setValue(Math.min(Math.max(_value + step, min), max));
        break;
      }
      case "ArrowRight": {
        event.preventDefault();
        thumb.current.focus();
        setValue(Math.min(Math.max(theme.dir === "rtl" ? _value - step : _value + step, min), max));
        break;
      }
      case "ArrowDown": {
        event.preventDefault();
        thumb.current.focus();
        setValue(Math.min(Math.max(_value - step, min), max));
        break;
      }
      case "ArrowLeft": {
        event.preventDefault();
        thumb.current.focus();
        setValue(Math.min(Math.max(theme.dir === "rtl" ? _value + step : _value - step, min), max));
        break;
      }
    }
  };
  return /* @__PURE__ */ React.createElement(SliderRoot, __spreadProps(__spreadValues({}, others), {
    size,
    ref: useMergedRef(container, ref),
    onKeyDownCapture: handleTrackKeydownCapture,
    onMouseDownCapture: () => {
      var _a2;
      return (_a2 = container.current) == null ? void 0 : _a2.focus();
    },
    classNames,
    styles
  }), /* @__PURE__ */ React.createElement(Track, {
    offset: 0,
    filled: position,
    marks,
    size,
    radius,
    color,
    min,
    max,
    value: _value,
    onChange: setValue,
    onMouseEnter: showLabelOnHover ? () => setHovered(true) : void 0,
    onMouseLeave: showLabelOnHover ? () => setHovered(false) : void 0,
    classNames,
    styles
  }, /* @__PURE__ */ React.createElement(Thumb, {
    max,
    min,
    value: _value,
    position,
    dragging: active,
    color,
    size,
    label: _label,
    ref: thumb,
    onMouseDown: handleThumbMouseDown,
    labelTransition,
    labelTransitionDuration,
    labelTransitionTimingFunction,
    labelAlwaysOn,
    classNames,
    styles,
    thumbLabel,
    showLabelOnHover: showLabelOnHover && hovered
  }, thumbChildren)), /* @__PURE__ */ React.createElement("input", {
    type: "hidden",
    name,
    value: _value
  }));
});
Slider.displayName = "@mantine/core/Slider";

export { Slider };
//# sourceMappingURL=Slider.js.map
