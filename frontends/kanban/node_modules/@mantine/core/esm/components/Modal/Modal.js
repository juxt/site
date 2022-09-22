import React, { useEffect } from 'react';
import { useUuid, useFocusTrap, useScrollLock, useFocusReturn } from '@mantine/hooks';
import { getDefaultZIndex } from '@mantine/styles';
import useStyles from './Modal.styles.js';
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
function MantineModal(_a) {
  var _b = _a, {
    className,
    opened,
    title,
    onClose,
    children,
    hideCloseButton = false,
    overlayOpacity,
    size = "md",
    transitionDuration = 300,
    closeButtonLabel,
    overlayColor,
    overflow = "outside",
    transition = "pop",
    padding = "lg",
    shadow = "lg",
    radius = "sm",
    id,
    classNames,
    styles,
    closeOnClickOutside = true,
    noFocusTrap = false,
    closeOnEscape = true,
    centered = false,
    target
  } = _b, others = __objRest(_b, [
    "className",
    "opened",
    "title",
    "onClose",
    "children",
    "hideCloseButton",
    "overlayOpacity",
    "size",
    "transitionDuration",
    "closeButtonLabel",
    "overlayColor",
    "overflow",
    "transition",
    "padding",
    "shadow",
    "radius",
    "id",
    "classNames",
    "styles",
    "closeOnClickOutside",
    "noFocusTrap",
    "closeOnEscape",
    "centered",
    "target"
  ]);
  const baseId = useUuid(id);
  const titleId = `${baseId}-title`;
  const bodyId = `${baseId}-body`;
  const { classes, cx, theme } = useStyles({ size, overflow, centered }, { classNames, styles, name: "Modal" });
  const focusTrapRef = useFocusTrap(!noFocusTrap && opened);
  const _overlayOpacity = typeof overlayOpacity === "number" ? overlayOpacity : theme.colorScheme === "dark" ? 0.85 : 0.75;
  const [, lockScroll] = useScrollLock();
  const closeOnEscapePress = (event) => {
    if (noFocusTrap && event.code === "Escape" && closeOnEscape) {
      onClose();
    }
  };
  useEffect(() => {
    if (noFocusTrap) {
      window.addEventListener("keydown", closeOnEscapePress);
      return () => window.removeEventListener("keydown", closeOnEscapePress);
    }
  }, [noFocusTrap]);
  useFocusReturn({ opened, transitionDuration });
  return /* @__PURE__ */ React.createElement(GroupedTransition, {
    onExited: () => lockScroll(false),
    onEntered: () => lockScroll(true),
    mounted: opened,
    transitions: {
      modal: { duration: transitionDuration, transition },
      overlay: {
        duration: transitionDuration / 2,
        transition: "fade",
        timingFunction: "ease"
      }
    }
  }, (transitionStyles) => /* @__PURE__ */ React.createElement(Box, __spreadValues({
    className: cx(classes.root, className)
  }, others), /* @__PURE__ */ React.createElement("div", {
    className: classes.inner,
    onMouseDown: () => closeOnClickOutside && onClose(),
    onKeyDownCapture: (event) => {
      var _a2;
      const shouldTrigger = ((_a2 = event.target) == null ? void 0 : _a2.getAttribute("data-mantine-stop-propagation")) !== "true";
      shouldTrigger && event.nativeEvent.code === "Escape" && closeOnEscape && onClose();
    },
    ref: focusTrapRef
  }, /* @__PURE__ */ React.createElement(Paper, {
    onMouseDown: (event) => event.stopPropagation(),
    className: classes.modal,
    shadow,
    padding,
    radius,
    role: "dialog",
    "aria-labelledby": titleId,
    "aria-describedby": bodyId,
    "aria-modal": true,
    tabIndex: -1,
    style: __spreadProps(__spreadValues({}, transitionStyles.modal), {
      marginLeft: "calc(var(--removed-scroll-width, 0px) * -1)",
      zIndex: 3
    })
  }, (title || !hideCloseButton) && /* @__PURE__ */ React.createElement("div", {
    className: classes.header
  }, /* @__PURE__ */ React.createElement(Text, {
    id: titleId,
    className: classes.title
  }, title), !hideCloseButton && /* @__PURE__ */ React.createElement(CloseButton, {
    iconSize: 16,
    onClick: onClose,
    "aria-label": closeButtonLabel,
    className: classes.close
  })), /* @__PURE__ */ React.createElement("div", {
    id: bodyId,
    className: classes.body
  }, children))), /* @__PURE__ */ React.createElement("div", {
    style: transitionStyles.overlay
  }, /* @__PURE__ */ React.createElement(Overlay, {
    className: classes.overlay,
    zIndex: 0,
    color: overlayColor || (theme.colorScheme === "dark" ? theme.colors.dark[9] : theme.black),
    opacity: _overlayOpacity
  }))));
}
function Modal(_c) {
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
  }, /* @__PURE__ */ React.createElement(MantineModal, __spreadValues({}, props)));
}
Modal.displayName = "@mantine/core/Modal";

export { MantineModal, Modal };
//# sourceMappingURL=Modal.js.map
