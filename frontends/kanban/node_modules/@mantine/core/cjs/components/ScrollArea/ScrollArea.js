'use strict';

Object.defineProperty(exports, '__esModule', { value: true });

var React = require('react');
var RadixScrollArea = require('@radix-ui/react-scroll-area');
var styles = require('@mantine/styles');
var ScrollArea_styles = require('./ScrollArea.styles.js');
var Box = require('../Box/Box.js');

function _interopDefaultLegacy (e) { return e && typeof e === 'object' && 'default' in e ? e['default'] : e; }

function _interopNamespace(e) {
  if (e && e.__esModule) return e;
  var n = Object.create(null);
  if (e) {
    Object.keys(e).forEach(function (k) {
      n[k] = e[k];
    });
  }
  n['default'] = e;
  return Object.freeze(n);
}

var React__default = /*#__PURE__*/_interopDefaultLegacy(React);
var RadixScrollArea__namespace = /*#__PURE__*/_interopNamespace(RadixScrollArea);

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
const ScrollArea = React.forwardRef((_a, ref) => {
  var _b = _a, {
    children,
    className,
    classNames,
    styles: styles$1,
    scrollbarSize = 12,
    scrollHideDelay = 1e3,
    type = "hover",
    dir,
    offsetScrollbars = false,
    viewportRef,
    onScrollPositionChange
  } = _b, others = __objRest(_b, [
    "children",
    "className",
    "classNames",
    "styles",
    "scrollbarSize",
    "scrollHideDelay",
    "type",
    "dir",
    "offsetScrollbars",
    "viewportRef",
    "onScrollPositionChange"
  ]);
  const [scrollbarHovered, setScrollbarHovered] = React.useState(false);
  const theme = styles.useMantineTheme();
  const { classes, cx } = ScrollArea_styles['default']({ scrollbarSize, offsetScrollbars, scrollbarHovered }, { name: "ScrollArea", classNames, styles: styles$1 });
  return /* @__PURE__ */ React__default.createElement(RadixScrollArea__namespace.Root, {
    type,
    scrollHideDelay,
    dir: dir || theme.dir,
    ref,
    asChild: true
  }, /* @__PURE__ */ React__default.createElement(Box.Box, __spreadValues({
    className: cx(classes.root, className)
  }, others), /* @__PURE__ */ React__default.createElement(RadixScrollArea__namespace.Viewport, {
    className: classes.viewport,
    ref: viewportRef,
    onScroll: typeof onScrollPositionChange === "function" ? ({ currentTarget }) => onScrollPositionChange({
      x: currentTarget.scrollLeft,
      y: currentTarget.scrollTop
    }) : void 0
  }, children), /* @__PURE__ */ React__default.createElement(RadixScrollArea__namespace.Scrollbar, {
    orientation: "horizontal",
    className: classes.scrollbar,
    forceMount: true,
    onMouseEnter: () => setScrollbarHovered(true),
    onMouseLeave: () => setScrollbarHovered(false)
  }, /* @__PURE__ */ React__default.createElement(RadixScrollArea__namespace.Thumb, {
    className: classes.thumb
  })), /* @__PURE__ */ React__default.createElement(RadixScrollArea__namespace.Scrollbar, {
    orientation: "vertical",
    className: classes.scrollbar,
    forceMount: true,
    onMouseEnter: () => setScrollbarHovered(true),
    onMouseLeave: () => setScrollbarHovered(false)
  }, /* @__PURE__ */ React__default.createElement(RadixScrollArea__namespace.Thumb, {
    className: classes.thumb
  })), /* @__PURE__ */ React__default.createElement(RadixScrollArea__namespace.Corner, {
    className: classes.corner
  })));
});
ScrollArea.displayName = "@mantine/core/ScrollArea";

exports.ScrollArea = ScrollArea;
//# sourceMappingURL=ScrollArea.js.map
