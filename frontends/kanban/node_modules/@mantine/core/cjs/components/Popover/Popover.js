'use strict';

Object.defineProperty(exports, '__esModule', { value: true });

var React = require('react');
var hooks = require('@mantine/hooks');
var styles = require('@mantine/styles');
var PopoverBody = require('./PopoverBody/PopoverBody.js');
var Popover_styles = require('./Popover.styles.js');
var Box = require('../Box/Box.js');
var Popper = require('../Popper/Popper.js');

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
function Popover(_a) {
  var _b = _a, {
    className,
    children,
    target,
    title,
    onClose,
    opened,
    zIndex = styles.getDefaultZIndex("popover"),
    arrowSize = 4,
    withArrow = false,
    transition = "fade",
    transitionDuration = 200,
    transitionTimingFunction,
    gutter = 10,
    position = "left",
    placement = "center",
    disabled = false,
    noClickOutside = false,
    noFocusTrap = false,
    noEscape = false,
    withCloseButton = false,
    radius = "sm",
    spacing = "md",
    shadow = "sm",
    closeButtonLabel,
    positionDependencies = [],
    withinPortal = true,
    id,
    classNames,
    styles: styles$1,
    width,
    clickOutsideEvents = ["click", "touchstart"]
  } = _b, others = __objRest(_b, [
    "className",
    "children",
    "target",
    "title",
    "onClose",
    "opened",
    "zIndex",
    "arrowSize",
    "withArrow",
    "transition",
    "transitionDuration",
    "transitionTimingFunction",
    "gutter",
    "position",
    "placement",
    "disabled",
    "noClickOutside",
    "noFocusTrap",
    "noEscape",
    "withCloseButton",
    "radius",
    "spacing",
    "shadow",
    "closeButtonLabel",
    "positionDependencies",
    "withinPortal",
    "id",
    "classNames",
    "styles",
    "width",
    "clickOutsideEvents"
  ]);
  const { classes, cx } = Popover_styles['default'](null, { classNames, styles: styles$1, name: "Popover" });
  const handleClose = () => typeof onClose === "function" && onClose();
  const [referenceElement, setReferenceElement] = React.useState(null);
  const [rootElement, setRootElement] = React.useState(null);
  const [dropdownElement, setDropdownElement] = React.useState(null);
  const focusTrapRef = hooks.useFocusTrap(!noFocusTrap && opened);
  hooks.useClickOutside(() => !noClickOutside && handleClose(), clickOutsideEvents, [
    rootElement,
    dropdownElement
  ]);
  const returnFocus = hooks.useFocusReturn({
    opened: opened || noFocusTrap,
    transitionDuration,
    shouldReturnFocus: false
  });
  const handleKeydown = (event) => {
    if (!noEscape && event.nativeEvent.code === "Escape") {
      handleClose();
      window.setTimeout(returnFocus, 0);
    }
  };
  const uuid = hooks.useUuid(id);
  const titleId = `${uuid}-title`;
  const bodyId = `${uuid}-body`;
  return /* @__PURE__ */ React__default.createElement(Box.Box, __spreadValues({
    className: cx(classes.root, className),
    id,
    ref: setRootElement
  }, others), /* @__PURE__ */ React__default.createElement(Popper.Popper, {
    referenceElement,
    transitionDuration,
    transition,
    mounted: opened && !disabled,
    position,
    placement,
    gutter,
    withArrow,
    arrowSize,
    zIndex,
    arrowClassName: classes.arrow,
    forceUpdateDependencies: [radius, shadow, spacing, ...positionDependencies],
    withinPortal
  }, /* @__PURE__ */ React__default.createElement(PopoverBody.PopoverBody, {
    shadow,
    radius,
    spacing,
    withCloseButton,
    title,
    titleId,
    bodyId,
    closeButtonLabel,
    onClose: handleClose,
    ref: hooks.useMergedRef(focusTrapRef, setDropdownElement),
    onKeyDownCapture: handleKeydown,
    classNames,
    styles: styles$1,
    width
  }, children)), /* @__PURE__ */ React__default.createElement("div", {
    className: classes.target,
    ref: setReferenceElement
  }, target));
}
Popover.displayName = "@mantine/core/Popover";

exports.Popover = Popover;
//# sourceMappingURL=Popover.js.map
