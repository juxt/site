import React, { forwardRef, cloneElement } from 'react';
import { useUuid, useUncontrolled } from '@mantine/hooks';
import { InputWrapper } from '../InputWrapper/InputWrapper.js';
import { Radio } from './Radio/Radio.js';
import { Group } from '../Group/Group.js';
import { filterChildrenByType } from '../../utils/filter-children-by-type/filter-children-by-type.js';

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
const RadioGroup = forwardRef((_a, ref) => {
  var _b = _a, {
    name,
    children,
    value,
    defaultValue,
    onChange,
    variant = "horizontal",
    spacing = "sm",
    color,
    size,
    classNames,
    styles,
    wrapperProps
  } = _b, others = __objRest(_b, [
    "name",
    "children",
    "value",
    "defaultValue",
    "onChange",
    "variant",
    "spacing",
    "color",
    "size",
    "classNames",
    "styles",
    "wrapperProps"
  ]);
  const uuid = useUuid(name);
  const [_value, setValue] = useUncontrolled({
    value,
    defaultValue,
    finalValue: "",
    onChange,
    rule: (val) => typeof val === "string"
  });
  const radios = filterChildrenByType(children, Radio).map((radio, index) => cloneElement(radio, {
    key: index,
    checked: _value === radio.props.value,
    name: uuid,
    color,
    size,
    classNames,
    styles,
    __staticSelector: "RadioGroup",
    onChange: (event) => setValue(event.currentTarget.value)
  }));
  return /* @__PURE__ */ React.createElement(InputWrapper, __spreadValues(__spreadValues({
    labelElement: "div",
    size,
    __staticSelector: "RadioGroup",
    classNames,
    styles,
    ref
  }, wrapperProps), others), /* @__PURE__ */ React.createElement(Group, {
    role: "radiogroup",
    spacing,
    direction: variant === "horizontal" ? "row" : "column",
    style: { paddingTop: 5 }
  }, radios));
});
RadioGroup.displayName = "@mantine/core/RadioGroup";

export { RadioGroup };
//# sourceMappingURL=RadioGroup.js.map
