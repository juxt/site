import React, { forwardRef } from 'react';
import { getDefaultZIndex, useMantineTheme } from '@mantine/styles';
import { Paper } from '../Paper/Paper.js';
import useStyles from './Dialog.styles.js';
import { Affix } from '../Affix/Affix.js';
import { Transition } from '../Transition/Transition.js';
import { CloseButton } from '../ActionIcon/CloseButton/CloseButton.js';

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
function MantineDialog(_a) {
  var _b = _a, {
    withCloseButton,
    onClose,
    position,
    shadow = "md",
    padding = "md",
    children,
    className,
    style,
    classNames,
    styles,
    opened,
    withBorder = true,
    size = "md",
    transition = "pop-top-right",
    transitionDuration = 200,
    transitionTimingFunction
  } = _b, others = __objRest(_b, [
    "withCloseButton",
    "onClose",
    "position",
    "shadow",
    "padding",
    "children",
    "className",
    "style",
    "classNames",
    "styles",
    "opened",
    "withBorder",
    "size",
    "transition",
    "transitionDuration",
    "transitionTimingFunction"
  ]);
  const { classes, cx } = useStyles({ size }, { classNames, styles, name: "Dialog" });
  return /* @__PURE__ */ React.createElement(Transition, {
    mounted: opened,
    transition,
    duration: transitionDuration,
    timingFunction: transitionTimingFunction
  }, (transitionStyles) => /* @__PURE__ */ React.createElement(Paper, __spreadValues({
    className: cx(classes.root, className),
    style: __spreadValues(__spreadValues({}, style), transitionStyles),
    shadow,
    padding,
    withBorder
  }, others), withCloseButton && /* @__PURE__ */ React.createElement(CloseButton, {
    onClick: onClose,
    className: classes.closeButton
  }), children));
}
const Dialog = forwardRef((_c, ref) => {
  var _d = _c, { zIndex = getDefaultZIndex("modal") } = _d, props = __objRest(_d, ["zIndex"]);
  const theme = useMantineTheme();
  return /* @__PURE__ */ React.createElement(Affix, {
    zIndex,
    position: props.position || { bottom: theme.spacing.xl, right: theme.spacing.xl },
    ref
  }, /* @__PURE__ */ React.createElement(MantineDialog, __spreadValues({}, props)));
});
Dialog.displayName = "@mantine/core/Dialog";

export { Dialog, MantineDialog };
//# sourceMappingURL=Dialog.js.map
