import React, { useEffect } from 'react';
import { useFocusTrap, useScrollLock, useFocusReturn } from '@mantine/hooks';
import { getDefaultZIndex } from '@mantine/styles';
import useStyles from './Drawer.styles.js';
import { Portal } from '../Portal/Portal.js';
import { GroupedTransition } from '../Transition/GroupedTransition.js';
import { Box } from '../Box/Box.js';
import { Paper } from '../Paper/Paper.js';
import { Text } from '../Text/Text.js';
import { CloseButton } from '../ActionIcon/CloseButton/CloseButton.js';
import { Overlay } from '../Overlay/Overlay.js';

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
const transitions = {
  top: "slide-down",
  bottom: "slide-up",
  left: "slide-right",
  right: "slide-left"
};
const rtlTransitions = {
  top: "slide-down",
  bottom: "slide-up",
  right: "slide-right",
  left: "slide-left"
};
function MantineDrawer(_a) {
  var _b = _a, {
    className,
    opened,
    onClose,
    position = "left",
    size = "md",
    noFocusTrap = false,
    noScrollLock = false,
    noCloseOnClickOutside = false,
    noCloseOnEscape = false,
    transition,
    transitionDuration = 250,
    transitionTimingFunction = "ease",
    zIndex = getDefaultZIndex("modal"),
    overlayColor,
    overlayOpacity,
    children,
    noOverlay = false,
    shadow = "md",
    padding = 0,
    title,
    hideCloseButton,
    closeButtonLabel,
    classNames,
    styles,
    target
  } = _b, others = __objRest(_b, [
    "className",
    "opened",
    "onClose",
    "position",
    "size",
    "noFocusTrap",
    "noScrollLock",
    "noCloseOnClickOutside",
    "noCloseOnEscape",
    "transition",
    "transitionDuration",
    "transitionTimingFunction",
    "zIndex",
    "overlayColor",
    "overlayOpacity",
    "children",
    "noOverlay",
    "shadow",
    "padding",
    "title",
    "hideCloseButton",
    "closeButtonLabel",
    "classNames",
    "styles",
    "target"
  ]);
  const { classes, cx, theme } = useStyles({ size, position }, { classNames, styles, name: "Drawer" });
  const focusTrapRef = useFocusTrap(!noFocusTrap && opened);
  const [, lockScroll] = useScrollLock();
  const drawerTransition = transition || (theme.dir === "rtl" ? rtlTransitions : transitions)[position];
  const _overlayOpacity = typeof overlayOpacity === "number" ? overlayOpacity : theme.colorScheme === "dark" ? 0.85 : 0.75;
  const closeOnEscape = (event) => {
    if (noFocusTrap && event.code === "Escape" && !noCloseOnEscape) {
      onClose();
    }
  };
  useEffect(() => {
    if (noFocusTrap) {
      window.addEventListener("keydown", closeOnEscape);
      return () => window.removeEventListener("keydown", closeOnEscape);
    }
  }, [noFocusTrap]);
  useFocusReturn({ opened, transitionDuration });
  return /* @__PURE__ */ React.createElement(GroupedTransition, {
    onExited: () => lockScroll(false),
    onEntered: () => lockScroll(!noScrollLock && true),
    mounted: opened,
    transitions: {
      overlay: { duration: transitionDuration / 2, transition: "fade", timingFunction: "ease" },
      drawer: {
        duration: transitionDuration,
        transition: drawerTransition,
        timingFunction: transitionTimingFunction
      }
    }
  }, (transitionStyles) => /* @__PURE__ */ React.createElement(Box, __spreadValues({
    className: cx(classes.root, { [classes.noOverlay]: noOverlay }, className),
    role: "dialog",
    "aria-modal": true,
    onMouseDown: () => !noCloseOnClickOutside && onClose()
  }, others), /* @__PURE__ */ React.createElement(Paper, {
    onMouseDown: (event) => event.stopPropagation(),
    className: cx(classes.drawer, className),
    ref: focusTrapRef,
    style: __spreadProps(__spreadValues({}, transitionStyles.drawer), { zIndex: zIndex + 2 }),
    radius: 0,
    tabIndex: -1,
    onKeyDownCapture: (event) => {
      var _a2;
      const shouldTrigger = ((_a2 = event.target) == null ? void 0 : _a2.getAttribute("data-mantine-stop-propagation")) !== "true";
      shouldTrigger && event.nativeEvent.code === "Escape" && !noCloseOnEscape && onClose();
    },
    shadow,
    padding
  }, (title || !hideCloseButton) && /* @__PURE__ */ React.createElement("div", {
    className: classes.header
  }, /* @__PURE__ */ React.createElement(Text, {
    className: classes.title
  }, title), !hideCloseButton && /* @__PURE__ */ React.createElement(CloseButton, {
    iconSize: 16,
    onClick: onClose,
    "aria-label": closeButtonLabel,
    className: classes.closeButton
  })), children), !noOverlay && /* @__PURE__ */ React.createElement("div", {
    style: transitionStyles.overlay
  }, /* @__PURE__ */ React.createElement(Overlay, {
    className: classes.overlay,
    opacity: _overlayOpacity,
    zIndex,
    color: overlayColor || (theme.colorScheme === "dark" ? theme.colors.dark[9] : theme.black)
  }))));
}
function Drawer(_c) {
  var _d = _c, {
    zIndex = getDefaultZIndex("modal"),
    target
  } = _d, props = __objRest(_d, [
    "zIndex",
    "target"
  ]);
  return /* @__PURE__ */ React.createElement(Portal, {
    zIndex,
    target
  }, /* @__PURE__ */ React.createElement(MantineDrawer, __spreadValues({}, props)));
}
Drawer.displayName = "@mantine/core/Drawer";

export { Drawer, MantineDrawer };
//# sourceMappingURL=Drawer.js.map
