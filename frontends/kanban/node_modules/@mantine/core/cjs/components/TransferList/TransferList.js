'use strict';

Object.defineProperty(exports, '__esModule', { value: true });

var React = require('react');
var RenderList = require('./RenderList/RenderList.js');
var SelectScrollArea = require('../Select/SelectScrollArea/SelectScrollArea.js');
var DefaultItem = require('./DefaultItem/DefaultItem.js');
var useSelectionState = require('./use-selection-state/use-selection-state.js');
var SimpleGrid = require('../SimpleGrid/SimpleGrid.js');

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
function defaultFilter(query, item) {
  return item.label.toLowerCase().trim().includes(query.toLowerCase().trim());
}
const TransferList = React.forwardRef((_a, ref) => {
  var _b = _a, {
    value,
    onChange,
    itemComponent = DefaultItem.DefaultItem,
    searchPlaceholder,
    filter = defaultFilter,
    nothingFound,
    titles = [null, null],
    initialSelection,
    listHeight = 150,
    listComponent = SelectScrollArea.SelectScrollArea,
    showTransferAll = true,
    breakpoint,
    classNames,
    styles,
    limit = Infinity
  } = _b, others = __objRest(_b, [
    "value",
    "onChange",
    "itemComponent",
    "searchPlaceholder",
    "filter",
    "nothingFound",
    "titles",
    "initialSelection",
    "listHeight",
    "listComponent",
    "showTransferAll",
    "breakpoint",
    "classNames",
    "styles",
    "limit"
  ]);
  const [selection, handlers] = useSelectionState.useSelectionState(initialSelection);
  const handleMoveAll = (listIndex) => {
    const items = Array(2);
    const moveToIndex = listIndex === 0 ? 1 : 0;
    items[listIndex] = [];
    items[moveToIndex] = [...value[moveToIndex], ...value[listIndex]];
    onChange(items);
    handlers.deselectAll(listIndex);
  };
  const handleMove = (listIndex) => {
    const moveToIndex = listIndex === 0 ? 1 : 0;
    const items = Array(2);
    const transferData = value[listIndex].reduce((acc, item) => {
      if (!selection[listIndex].includes(item.value)) {
        acc.filtered.push(item);
      } else {
        acc.current.push(item);
      }
      return acc;
    }, { filtered: [], current: [] });
    items[listIndex] = transferData.filtered;
    items[moveToIndex] = [...transferData.current, ...value[moveToIndex]];
    onChange(items);
    handlers.deselectAll(listIndex);
  };
  const breakpoints = breakpoint ? [{ maxWidth: breakpoint, cols: 1 }] : void 0;
  const sharedListProps = {
    itemComponent,
    listComponent,
    searchPlaceholder,
    filter,
    nothingFound,
    height: listHeight,
    showTransferAll,
    classNames,
    styles,
    limit
  };
  return /* @__PURE__ */ React__default.createElement(SimpleGrid.SimpleGrid, __spreadValues({
    cols: 2,
    spacing: "xl",
    breakpoints,
    ref
  }, others), /* @__PURE__ */ React__default.createElement(RenderList.RenderList, __spreadProps(__spreadValues({}, sharedListProps), {
    data: value[0],
    selection: selection[0],
    onSelect: (val) => handlers.select(0, val),
    onMoveAll: () => handleMoveAll(0),
    onMove: () => handleMove(0),
    title: titles[0]
  })), /* @__PURE__ */ React__default.createElement(RenderList.RenderList, __spreadProps(__spreadValues({}, sharedListProps), {
    data: value[1],
    selection: selection[1],
    onSelect: (val) => handlers.select(1, val),
    onMoveAll: () => handleMoveAll(1),
    onMove: () => handleMove(1),
    title: titles[1],
    reversed: true
  })));
});
TransferList.displayName = "@mantine/core/TransferList";

exports.TransferList = TransferList;
exports.defaultFilter = defaultFilter;
//# sourceMappingURL=TransferList.js.map
