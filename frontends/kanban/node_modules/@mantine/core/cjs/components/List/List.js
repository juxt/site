'use strict';

Object.defineProperty(exports, '__esModule', { value: true });

var React = require('react');
var ListItem = require('./ListItem/ListItem.js');
var List_styles = require('./List.styles.js');
var filterChildrenByType = require('../../utils/filter-children-by-type/filter-children-by-type.js');
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
const List = React.forwardRef((_a, ref) => {
  var _b = _a, {
    children,
    type = "unordered",
    size = "md",
    listStyleType,
    withPadding = false,
    center = false,
    spacing = 0,
    icon,
    className,
    styles,
    classNames
  } = _b, others = __objRest(_b, [
    "children",
    "type",
    "size",
    "listStyleType",
    "withPadding",
    "center",
    "spacing",
    "icon",
    "className",
    "styles",
    "classNames"
  ]);
  const { classes, cx } = List_styles['default']({ withPadding, size, listStyleType }, { classNames, styles, name: "List" });
  const items = filterChildrenByType.filterChildrenByType(children, ListItem.ListItem).map((item) => {
    var _a2;
    return React__default.cloneElement(item, {
      classNames,
      styles,
      spacing,
      center,
      icon: ((_a2 = item.props) == null ? void 0 : _a2.icon) || icon
    });
  });
  return /* @__PURE__ */ React__default.createElement(Box.Box, __spreadValues({
    component: type === "unordered" ? "ul" : "ol",
    className: cx(classes.root, className),
    ref
  }, others), items);
});
List.Item = ListItem.ListItem;
List.displayName = "@mantine/core/List";

exports.List = List;
//# sourceMappingURL=List.js.map
