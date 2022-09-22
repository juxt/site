import React, { forwardRef, useState, useRef, cloneElement } from 'react';
import { useUuid, useUncontrolled, useWindowEvent, useClickOutside, useMergedRef } from '@mantine/hooks';
import { getDefaultZIndex } from '@mantine/styles';
import { MenuIcon } from './MenuIcon.js';
import { MenuItem } from './MenuItem/MenuItem.js';
import { MenuLabel } from './MenuLabel/MenuLabel.js';
import useStyles from './Menu.styles.js';
import { filterChildrenByType } from '../../utils/filter-children-by-type/filter-children-by-type.js';
import { Divider } from '../Divider/Divider.js';
import { Text } from '../Text/Text.js';
import { Box } from '../Box/Box.js';
import { Popper } from '../Popper/Popper.js';
import { Paper } from '../Paper/Paper.js';
import { ActionIcon } from '../ActionIcon/ActionIcon.js';

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
const defaultControl = /* @__PURE__ */ React.createElement(ActionIcon, null, /* @__PURE__ */ React.createElement(MenuIcon, null));
function getNextItem(active, items) {
  for (let i = active + 1; i < items.length; i += 1) {
    if (!items[i].props.disabled && items[i].type === MenuItem) {
      return i;
    }
  }
  return active;
}
function findInitialItem(items) {
  for (let i = 0; i < items.length; i += 1) {
    if (!items[i].props.disabled && items[i].type === MenuItem) {
      return i;
    }
  }
  return -1;
}
function getPreviousItem(active, items) {
  for (let i = active - 1; i >= 0; i -= 1) {
    if (!items[i].props.disabled && items[i].type === MenuItem) {
      return i;
    }
  }
  if (!items[active] || items[active].type !== MenuItem) {
    return findInitialItem(items);
  }
  return active;
}
const Menu = forwardRef((_a, ref) => {
  var _b = _a, {
    control = defaultControl,
    children,
    onClose,
    onOpen,
    opened,
    menuId,
    closeOnItemClick = true,
    transitionDuration = 250,
    size = "md",
    shadow = "md",
    position = "bottom",
    placement = "start",
    gutter = 5,
    withArrow = false,
    transition = "pop-top-left",
    transitionTimingFunction,
    menuButtonLabel,
    controlRefProp = "ref",
    trigger = "click",
    radius = "sm",
    delay = 100,
    zIndex = getDefaultZIndex("popover"),
    withinPortal = true,
    trapFocus = true,
    classNames,
    styles,
    closeOnScroll = false,
    onMouseLeave,
    onMouseEnter,
    onChange,
    className,
    sx,
    clickOutsideEvents = ["mouseup", "touchstart"]
  } = _b, others = __objRest(_b, [
    "control",
    "children",
    "onClose",
    "onOpen",
    "opened",
    "menuId",
    "closeOnItemClick",
    "transitionDuration",
    "size",
    "shadow",
    "position",
    "placement",
    "gutter",
    "withArrow",
    "transition",
    "transitionTimingFunction",
    "menuButtonLabel",
    "controlRefProp",
    "trigger",
    "radius",
    "delay",
    "zIndex",
    "withinPortal",
    "trapFocus",
    "classNames",
    "styles",
    "closeOnScroll",
    "onMouseLeave",
    "onMouseEnter",
    "onChange",
    "className",
    "sx",
    "clickOutsideEvents"
  ]);
  const [hovered, setHovered] = useState(-1);
  const buttonsRefs = useRef({});
  const { classes, cx, theme } = useStyles({ size }, { classNames, styles, name: "Menu" });
  const delayTimeout = useRef();
  const [referenceElement, setReferenceElement] = useState(null);
  const [wrapperElement, setWrapperElement] = useState(null);
  const [dropdownElement, setDropdownElement] = useState(null);
  const items = filterChildrenByType(children, [MenuItem, MenuLabel, Divider]);
  const uuid = useUuid(menuId);
  const focusReference = () => window.setTimeout(() => referenceElement == null ? void 0 : referenceElement.focus(), 0);
  const [_opened, setOpened] = useUncontrolled({
    value: opened,
    defaultValue: false,
    finalValue: false,
    rule: (val) => typeof val === "boolean",
    onChange: (value) => value ? typeof onOpen === "function" && onOpen() : typeof onClose === "function" && onClose()
  });
  const openedRef = useRef(_opened);
  const handleClose = () => {
    if (openedRef.current) {
      openedRef.current = false;
      setOpened(false);
    }
  };
  const handleOpen = () => {
    openedRef.current = true;
    setOpened(true);
  };
  useWindowEvent("scroll", () => closeOnScroll && handleClose());
  useClickOutside(() => _opened && handleClose(), clickOutsideEvents, [
    dropdownElement,
    wrapperElement
  ]);
  const toggleMenu = () => {
    _opened ? handleClose() : handleOpen();
  };
  const controlEventHandlers = trigger === "click" ? { onClick: toggleMenu } : { onMouseEnter: handleOpen, onClick: toggleMenu };
  const handleMouseLeave = (event) => {
    typeof onMouseLeave === "function" && onMouseLeave(event);
    if (trigger === "hover") {
      if (delay > 0) {
        delayTimeout.current = window.setTimeout(() => handleClose(), delay);
      } else {
        handleClose();
      }
    }
  };
  const handleMouseEnter = (event) => {
    typeof onMouseEnter === "function" && onMouseEnter(event);
    window.clearTimeout(delayTimeout.current);
  };
  const handleKeyDown = (event) => {
    if (_opened) {
      if (event.nativeEvent.code === "Tab" && trapFocus) {
        event.preventDefault();
      }
      if (event.nativeEvent.code === "ArrowDown") {
        event.preventDefault();
        const prevIndex = getNextItem(hovered, items);
        setHovered(prevIndex);
        buttonsRefs.current[prevIndex].focus();
      }
      if (event.nativeEvent.code === "ArrowUp") {
        event.preventDefault();
        const prevIndex = getPreviousItem(hovered, items);
        setHovered(prevIndex);
        buttonsRefs.current[prevIndex].focus();
      }
      if (event.nativeEvent.code === "Escape") {
        handleClose();
        focusReference();
      }
    }
  };
  const menuControl = cloneElement(control, __spreadProps(__spreadValues({}, controlEventHandlers), {
    onClick: (event) => {
      controlEventHandlers.onClick();
      typeof control.props.onClick === "function" && control.props.onClick(event);
    },
    role: "button",
    "aria-haspopup": "menu",
    "aria-expanded": _opened,
    "aria-controls": uuid,
    "aria-label": menuButtonLabel,
    title: menuButtonLabel,
    [controlRefProp]: useMergedRef(setReferenceElement, ref),
    onKeyDown: handleKeyDown
  }));
  const content = items.map((item, index) => {
    if (item.type === MenuItem) {
      return /* @__PURE__ */ React.createElement(MenuItem, __spreadProps(__spreadValues({}, item.props), {
        key: index,
        hovered: hovered === index,
        onHover: () => setHovered(index),
        radius,
        onMouseLeave: () => setHovered(-1),
        onKeyDown: handleKeyDown,
        styles,
        classNames,
        onClick: (event) => {
          if (closeOnItemClick) {
            handleClose();
            trigger === "click" && focusReference();
          }
          if (typeof item.props.onClick === "function") {
            item.props.onClick(event);
          }
        },
        ref: (node) => {
          buttonsRefs.current[index] = node;
        }
      }));
    }
    if (item.type === MenuLabel) {
      return /* @__PURE__ */ React.createElement(Text, __spreadValues({
        key: index,
        className: classes.label
      }, item.props));
    }
    if (item.type === Divider) {
      return /* @__PURE__ */ React.createElement(Divider, {
        variant: "solid",
        className: classes.divider,
        my: theme.spacing.xs / 2,
        key: index
      });
    }
    return null;
  });
  return /* @__PURE__ */ React.createElement(Box, __spreadValues({
    ref: setWrapperElement,
    onMouseLeave: handleMouseLeave,
    onMouseEnter: handleMouseEnter,
    className: cx(classes.root, className),
    sx
  }, others), menuControl, /* @__PURE__ */ React.createElement(Popper, {
    referenceElement,
    transitionDuration,
    transitionTimingFunction,
    transition,
    mounted: _opened,
    position,
    placement,
    gutter,
    withArrow,
    arrowSize: 3,
    zIndex,
    arrowClassName: classes.arrow,
    withinPortal
  }, /* @__PURE__ */ React.createElement(Paper, __spreadValues({
    shadow,
    className: classes.body,
    role: "menu",
    "aria-orientation": "vertical",
    radius,
    onMouseLeave: () => setHovered(-1),
    ref: setDropdownElement,
    id: uuid
  }, others), content)));
});
Menu.Item = MenuItem;
Menu.Label = MenuLabel;
Menu.displayName = "@mantine/core/Menu";

export { Menu };
//# sourceMappingURL=Menu.js.map
