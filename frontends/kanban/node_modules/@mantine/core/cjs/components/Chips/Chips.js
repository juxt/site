'use strict';

Object.defineProperty(exports, '__esModule', { value: true });

var React = require('react');
var hooks = require('@mantine/hooks');
var Group = require('../Group/Group.js');
var Chip = require('./Chip/Chip.js');
var filterChildrenByType = require('../../utils/filter-children-by-type/filter-children-by-type.js');

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
  const uuid = hooks.useUuid(id);
  const [_value, setValue] = hooks.useUncontrolled({
    value,
    defaultValue,
    finalValue: multiple ? [] : null,
    onChange,
    rule: (val) => multiple ? Array.isArray(val) : typeof val === "string"
  });
  const chips = filterChildrenByType.filterChildrenByType(children, Chip.Chip).map((child, index) => React__default.cloneElement(child, {
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
  return /* @__PURE__ */ React__default.createElement(Group.Group, __spreadValues({
    spacing,
    id: uuid
  }, others), chips);
}
Chips.displayName = "@mantine/core/Chips";

exports.Chips = Chips;
//# sourceMappingURL=Chips.js.map
