import React from 'react';
import { useUuid, useUncontrolled } from '@mantine/hooks';
import { Group } from '../Group/Group.js';
import { Chip } from './Chip/Chip.js';
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
function Chips(_a) {
  var _b = _a, {
    value,
    defaultValue,
    onChange,
    color,
    spacing = "xs",
    radius = "xl",
    size = "sm",
    name,
    variant,
    multiple,
    children,
    id,
    classNames,
    styles
  } = _b, others = __objRest(_b, [
    "value",
    "defaultValue",
    "onChange",
    "color",
    "spacing",
    "radius",
    "size",
    "name",
    "variant",
    "multiple",
    "children",
    "id",
    "classNames",
    "styles"
  ]);
  const uuid = useUuid(id);
  const [_value, setValue] = useUncontrolled({
    value,
    defaultValue,
    finalValue: multiple ? [] : null,
    onChange,
    rule: (val) => multiple ? Array.isArray(val) : typeof val === "string"
  });
  const chips = filterChildrenByType(children, Chip).map((child, index) => React.cloneElement(child, {
    variant,
    radius,
    color,
    __staticSelector: "Chips",
    classNames,
    styles,
    name,
    size,
    id: `${uuid}-${index}`,
    type: multiple ? "checkbox" : "radio",
    checked: Array.isArray(_value) ? _value.includes(child.props.value) : child.props.value === _value,
    onChange: () => {
      const val = child.props.value;
      if (Array.isArray(_value)) {
        setValue(_value.includes(val) ? _value.filter((v) => v !== val) : [..._value, val]);
      } else {
        setValue(val);
      }
    }
  }));
  return /* @__PURE__ */ React.createElement(Group, __spreadValues({
    spacing,
    id: uuid
  }, others), chips);
}
Chips.displayName = "@mantine/core/Chips";

export { Chips };
//# sourceMappingURL=Chips.js.map
