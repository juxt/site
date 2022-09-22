'use strict';

Object.defineProperty(exports, '__esModule', { value: true });

var React = require('react');
var hooks = require('@mantine/hooks');
var styles = require('@mantine/styles');
var MenuIcon = require('./MenuIcon.js');
var MenuItem = require('./MenuItem/MenuItem.js');
var MenuLabel = require('./MenuLabel/MenuLabel.js');
var Menu_styles = require('./Menu.styles.js');
var filterChildrenByType = require('../../utils/filter-children-by-type/filter-children-by-type.js');
var Divider = require('../Divider/Divider.js');
var Text = require('../Text/Text.js');
var Box = require('../Box/Box.js');
var Popper = require('../Popper/Popper.js');
var Paper = require('../Paper/Paper.js');
var ActionIcon = require('../ActionIcon/ActionIcon.js');

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
const defaultControl = /* @__PURE__ */ React__default.createElement(ActionIcon.ActionIcon, null, /* @__PURE__ */ React__default.createElement(MenuIcon.MenuIcon, null));
function getNextItem(active, items) {
  for (let i = active + 1; i < items.length; i += 1) {
    if (!items[i].props.disabled && items[i].type === MenuItem.MenuItem) {
      return i;
    }
  }
  return active;
}
function findInitialItem(items) {
  for (let i = 0; i < items.length; i += 1) {
    if (!items[i].props.disabled && items[i].type === MenuItem.MenuItem) {
      return i;
    }
  }
  return -1;
}
function getPreviousItem(active, items) {
  for (let i = active - 1; i >= 0; i -= 1) {
    if (!items[i].props.disabled && items[i].type === MenuItem.MenuItem) {
      return i;
    }
  }
  if (!items[active] || items[active].type !== MenuItem.MenuItem) {
    return findInitialItem(items);
  }
  return active;
}
const Menu = React.forwardRef((_a, ref) => {
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
    zIndex = styles.getDefaultZIndex("popover"),
    withinPortal = true,
    trapFocus = true,
    classNames,
    styles: styles$1,
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
  const [hovered, setHovered] = React.useState(-1);
  const buttonsRefs = React.useRef({});
  const { classes, cx, theme } = Menu_styles['default']({ size }, { classNames, styles: styles$1, name: "Menu" });
  const delayTimeout = React.useRef();
  const [referenceElement, setReferenceElement] = React.useState(null);
  const [wrapperElement, setWrapperElement] = React.useState(null);
  const [dropdownElement, setDropdownElement] = React.useState(null);
  const items = filterChildrenByType.filterChildrenByType(children, [MenuItem.MenuItem, MenuLabel.MenuLabel, Divider.Divider]);
  const uuid = hooks.useUuid(menuId);
  const focusReference = () => window.setTimeout(() => referenceElement == null ? void 0 : referenceElement.focus(), 0);
  const [_opened, setOpened] = hooks.useUncontrolled({
    value: opened,
    defaultValue: false,
    finalValue: false,
    rule: (val) => typeof val === "boolean",
    onChange: (value) => value ? typeof onOpen === "function" && onOpen() : typeof onClose === "function" && onClose()
  });
  const openedRef = React.useRef(_opened);
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
  hooks.useWindowEvent("scroll", () => closeOnScroll && handleClose());
  hooks.useClickOutside(() => _opened && handleClose(), clickOutsideEvents, [
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
  const menuControl = React.cloneElement(control, __spreadProps(__spreadValues({}, controlEventHandlers), {
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
    [controlRefProp]: hooks.useMergedRef(setReferenceElement, ref),
    onKeyDown: handleKeyDown
  }));
  const content = items.map((item, index) => {
    if (item.type === MenuItem.MenuItem) {
      return /* @__PURE__ */ React__default.createElement(MenuItem.MenuItem, __spreadProps(__spreadValues({}, item.props), {
        key: index,
        hovered: hovered === index,
        onHover: () => setHovered(index),
        radius,
        onMouseLeave: () => setHovered(-1),
        onKeyDown: handleKeyDown,
        styles: styles$1,
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
    if (item.type === MenuLabel.MenuLabel) {
      return /* @__PURE__ */ React__default.createElement(Text.Text, __spreadValues({
        key: index,
        className: classes.label
      }, item.props));
    }
    if (item.type === Divider.Divider) {
      return /* @__PURE__ */ React__default.createElement(Divider.Divider, {
        variant: "solid",
        className: classes.divider,
        my: theme.spacing.xs / 2,
        key: index
      });
    }
    return null;
  });
  return /* @__PURE__ */ React__default.createElement(Box.Box, __spreadValues({
    ref: setWrapperElement,
    onMouseLeave: handleMouseLeave,
    onMouseEnter: handleMouseEnter,
    className: cx(classes.root, className),
    sx
  }, others), menuControl, /* @__PURE__ */ React__default.createElement(Popper.Popper, {
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
  }, /* @__PURE__ */ React__default.createElement(Paper.Paper, __spreadValues({
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
Menu.Item = MenuItem.MenuItem;
Menu.Label = MenuLabel.MenuLabel;
Menu.displayName = "@mantine/core/Menu";

exports.Menu = Menu;
//# sourceMappingURL=Menu.js.map
