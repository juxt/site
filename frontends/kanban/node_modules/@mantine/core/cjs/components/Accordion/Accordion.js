'use strict';

Object.defineProperty(exports, '__esModule', { value: true });

var React = require('react');
var hooks = require('@mantine/hooks');
var AccordionItem = require('./AccordionItem/AccordionItem.js');
var useAccordionState = require('./use-accordion-state/use-accordion-state.js');
var useAccordionFocus = require('./use-accordion-focus/use-accordion-focus.js');
var filterChildrenByType = require('../../utils/filter-children-by-type/filter-children-by-type.js');
var Box = require('../Box/Box.js');

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
const Accordion = React.forwardRef((_a, ref) => {
  var _b = _a, {
    children,
    initialItem = -1,
    initialState,
    state,
    onChange,
    multiple = false,
    disableIconRotation = false,
    transitionDuration = 200,
    iconPosition = "left",
    offsetIcon = true,
    iconSize = 24,
    order = 3,
    icon,
    classNames,
    styles,
    id
  } = _b, others = __objRest(_b, [
    "children",
    "initialItem",
    "initialState",
    "state",
    "onChange",
    "multiple",
    "disableIconRotation",
    "transitionDuration",
    "iconPosition",
    "offsetIcon",
    "iconSize",
    "order",
    "icon",
    "classNames",
    "styles",
    "id"
  ]);
  const uuid = hooks.useUuid(id);
  const items = filterChildrenByType.filterChildrenByType(children, AccordionItem.AccordionItem);
  const { handleItemKeydown, assignControlRef } = useAccordionFocus.useAccordionFocus(items.length);
  const [value, handlers] = useAccordionState.useAccordionState({
    multiple,
    total: items.length,
    initialItem,
    state,
    initialState,
    onChange
  });
  const controls = items.map((item, index) => {
    var _a2, _b2, _c;
    return /* @__PURE__ */ React__default.createElement(AccordionItem.AccordionItem, __spreadProps(__spreadValues({}, item.props), {
      icon: item.props.icon || icon,
      iconPosition: item.props.iconPosition || iconPosition,
      disableIconRotation,
      key: index,
      transitionDuration,
      opened: value[index],
      onToggle: () => handlers.toggle(index),
      classNames: ((_a2 = item.props) == null ? void 0 : _a2.classNames) || classNames,
      styles: ((_b2 = item.props) == null ? void 0 : _b2.styles) || styles,
      id: `${uuid}-${index}`,
      onControlKeyDown: handleItemKeydown(index),
      controlRef: hooks.mergeRefs(assignControlRef(index), (_c = item.props) == null ? void 0 : _c.controlRef),
      offsetIcon,
      iconSize,
      order
    }));
  });
  return /* @__PURE__ */ React__default.createElement(Box.Box, __spreadValues({
    ref
  }, others), controls);
});
Accordion.Item = AccordionItem.AccordionItem;
Accordion.displayName = "@mantine/core/Accordion";

exports.Accordion = Accordion;
//# sourceMappingURL=Accordion.js.map
