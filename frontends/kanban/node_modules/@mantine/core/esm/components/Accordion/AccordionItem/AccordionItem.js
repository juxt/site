import React from 'react';
import { useReducedMotion } from '@mantine/hooks';
import { ChevronIcon } from './ChevronIcon.js';
import useStyles from './AccordionItem.styles.js';
import { Box } from '../../Box/Box.js';
import { UnstyledButton } from '../../Button/UnstyledButton/UnstyledButton.js';
import { Center } from '../../Center/Center.js';
import { Collapse } from '../../Collapse/Collapse.js';

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
function AccordionItem(_a) {
  var _b = _a, {
    opened,
    onToggle,
    label,
    children,
    className,
    classNames,
    styles,
    transitionDuration,
    icon = /* @__PURE__ */ React.createElement(ChevronIcon, null),
    disableIconRotation = false,
    offsetIcon = true,
    iconSize = 24,
    iconPosition = "left",
    order = 3,
    id,
    controlRef,
    onControlKeyDown
  } = _b, others = __objRest(_b, [
    "opened",
    "onToggle",
    "label",
    "children",
    "className",
    "classNames",
    "styles",
    "transitionDuration",
    "icon",
    "disableIconRotation",
    "offsetIcon",
    "iconSize",
    "iconPosition",
    "order",
    "id",
    "controlRef",
    "onControlKeyDown"
  ]);
  const reduceMotion = useReducedMotion();
  const duration = reduceMotion ? 0 : transitionDuration;
  const { classes, cx } = useStyles({ transitionDuration: duration, disableIconRotation, iconPosition, offsetIcon, iconSize }, { classNames, styles, name: "Accordion" });
  const cappedOrder = Math.min(6, Math.max(2, order));
  const Heading = `h${cappedOrder}`;
  return /* @__PURE__ */ React.createElement(Box, __spreadValues({
    className: cx(classes.item, { [classes.itemOpened]: opened }, className)
  }, others), /* @__PURE__ */ React.createElement(Heading, {
    className: classes.itemTitle
  }, /* @__PURE__ */ React.createElement(UnstyledButton, {
    className: classes.control,
    onClick: onToggle,
    type: "button",
    "aria-expanded": opened,
    "aria-controls": `${id}-body`,
    id,
    ref: controlRef,
    onKeyDown: onControlKeyDown
  }, /* @__PURE__ */ React.createElement(Center, {
    className: classes.icon
  }, icon), /* @__PURE__ */ React.createElement("div", {
    className: classes.label
  }, label))), /* @__PURE__ */ React.createElement(Collapse, {
    in: opened,
    transitionDuration: duration
  }, /* @__PURE__ */ React.createElement("div", {
    className: classes.content,
    role: "region",
    id: `${id}-body`,
    "aria-labelledby": id
  }, /* @__PURE__ */ React.createElement("div", {
    className: classes.contentInner
  }, children))));
}
AccordionItem.displayName = "@mantine/core/AccordionItem";

export { AccordionItem };
//# sourceMappingURL=AccordionItem.js.map
