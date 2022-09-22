import React, { forwardRef, useState, useRef, useEffect } from 'react';
import { useReducedMotion, useUncontrolled, useUuid, useResizeObserver, useMergedRef } from '@mantine/hooks';
import useStyles, { WRAPPER_PADDING } from './SegmentedControl.styles.js';
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
const SegmentedControl = forwardRef((_a, ref) => {
  var _b = _a, {
    className,
    disabled = false,
    data: _data,
    name,
    value,
    onChange,
    color,
    fullWidth,
    radius = "sm",
    size = "sm",
    transitionDuration = 200,
    transitionTimingFunction,
    classNames,
    styles,
    defaultValue,
    orientation
  } = _b, others = __objRest(_b, [
    "className",
    "disabled",
    "data",
    "name",
    "value",
    "onChange",
    "color",
    "fullWidth",
    "radius",
    "size",
    "transitionDuration",
    "transitionTimingFunction",
    "classNames",
    "styles",
    "defaultValue",
    "orientation"
  ]);
  const reduceMotion = useReducedMotion();
  const data = _data.map((item) => typeof item === "string" ? { label: item, value: item } : item);
  const [shouldAnimate, setShouldAnimate] = useState(false);
  const [_value, handleValueChange] = useUncontrolled({
    value,
    defaultValue,
    finalValue: Array.isArray(data) ? data[0].value : null,
    onChange,
    rule: (val) => !!val
  });
  const { classes, cx, theme } = useStyles({
    size,
    fullWidth,
    color,
    radius,
    shouldAnimate: reduceMotion || !shouldAnimate,
    transitionDuration,
    transitionTimingFunction,
    orientation
  }, { classNames, styles, name: "SegmentedControl" });
  const [activePosition, setActivePosition] = useState({
    width: 0,
    height: 0,
    translate: [0, 0]
  });
  const uuid = useUuid(name);
  const refs = useRef({});
  const [observerRef, containerRect] = useResizeObserver();
  useEffect(() => {
    if (_value in refs.current && observerRef.current) {
      const element = refs.current[_value];
      const elementRect = element.getBoundingClientRect();
      const scaledValue = element.offsetWidth / elementRect.width;
      const width = elementRect.width * scaledValue || 0;
      const height = elementRect.height * scaledValue || 0;
      const offsetRight = containerRect.width - element.parentElement.offsetLeft + WRAPPER_PADDING - width;
      const offsetLeft = element.parentElement.offsetLeft - WRAPPER_PADDING;
      setActivePosition({
        width,
        height,
        translate: [
          theme.dir === "rtl" ? offsetRight : offsetLeft,
          element.parentElement.offsetTop - WRAPPER_PADDING
        ]
      });
    }
  }, [_value, containerRect]);
  useEffect(() => {
    setShouldAnimate(true);
  }, []);
  const controls = data.map((item) => /* @__PURE__ */ React.createElement("div", {
    className: cx(classes.control, { [classes.controlActive]: _value === item.value }),
    key: item.value
  }, /* @__PURE__ */ React.createElement("input", {
    className: classes.input,
    disabled,
    type: "radio",
    name: uuid,
    value: item.value,
    id: `${uuid}-${item.value}`,
    checked: _value === item.value,
    onChange: () => handleValueChange(item.value)
  }), /* @__PURE__ */ React.createElement("label", {
    className: cx(classes.label, {
      [classes.labelActive]: _value === item.value,
      [classes.disabled]: disabled
    }),
    htmlFor: `${uuid}-${item.value}`,
    ref: (node) => {
      refs.current[item.value] = node;
    }
  }, item.label)));
  return /* @__PURE__ */ React.createElement(Box, __spreadValues({
    className: cx(classes.root, className),
    ref: useMergedRef(observerRef, ref)
  }, others), !!_value && /* @__PURE__ */ React.createElement(Box, {
    component: "span",
    className: classes.active,
    sx: {
      width: activePosition.width,
      height: activePosition.height,
      transform: `translate(${activePosition.translate[0]}px, ${activePosition.translate[1]}px )`
    }
  }), controls);
});
SegmentedControl.displayName = "@mantine/core/SegmentedControl";

export { SegmentedControl };
//# sourceMappingURL=SegmentedControl.js.map
