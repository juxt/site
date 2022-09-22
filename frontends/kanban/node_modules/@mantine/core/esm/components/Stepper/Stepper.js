import React, { forwardRef } from 'react';
import { Step } from './Step/Step.js';
import { StepCompleted } from './StepCompleted/StepCompleted.js';
import useStyles from './Stepper.styles.js';
import { filterChildrenByType } from '../../utils/filter-children-by-type/filter-children-by-type.js';
import { findChildByType } from '../../utils/find-child-by-type/find-child-by-type.js';
import { Box } from '../Box/Box.js';

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
const Stepper = forwardRef((_a, ref) => {
  var _b = _a, {
    className,
    children,
    onStepClick,
    active,
    completedIcon,
    progressIcon,
    color,
    iconSize,
    contentPadding = "md",
    size = "md",
    radius = "xl",
    orientation = "horizontal",
    breakpoint,
    iconPosition = "left",
    classNames,
    styles
  } = _b, others = __objRest(_b, [
    "className",
    "children",
    "onStepClick",
    "active",
    "completedIcon",
    "progressIcon",
    "color",
    "iconSize",
    "contentPadding",
    "size",
    "radius",
    "orientation",
    "breakpoint",
    "iconPosition",
    "classNames",
    "styles"
  ]);
  var _a2, _b2, _c;
  const { classes, cx } = useStyles({ contentPadding, color, orientation, iconPosition, size, iconSize, breakpoint }, { classNames, styles, name: "Stepper" });
  const filteredChildren = filterChildrenByType(children, Step);
  const completedStep = findChildByType(children, StepCompleted);
  const items = filteredChildren.reduce((acc, item, index, array) => {
    const shouldAllowSelect = typeof item.props.allowStepSelect === "boolean" ? item.props.allowStepSelect : typeof onStepClick === "function";
    acc.push(/* @__PURE__ */ React.createElement(Step, __spreadProps(__spreadValues({}, item.props), {
      __staticSelector: "Stepper",
      icon: item.props.icon || index + 1,
      key: index,
      state: active === index ? "stepProgress" : active > index ? "stepCompleted" : "stepInactive",
      onClick: () => shouldAllowSelect && typeof onStepClick === "function" && onStepClick(index),
      allowStepClick: shouldAllowSelect && typeof onStepClick === "function",
      completedIcon: item.props.completedIcon || completedIcon,
      progressIcon: item.props.progressIcon || progressIcon,
      color: item.props.color || color,
      iconSize,
      size,
      radius,
      classNames,
      styles,
      iconPosition: item.props.iconPosition || iconPosition
    })));
    if (index !== array.length - 1) {
      acc.push(/* @__PURE__ */ React.createElement("div", {
        className: cx(classes.separator, { [classes.separatorActive]: index < active }),
        key: `separator-${index}`
      }));
    }
    return acc;
  }, []);
  const stepContent = (_b2 = (_a2 = filteredChildren[active]) == null ? void 0 : _a2.props) == null ? void 0 : _b2.children;
  const completedContent = (_c = completedStep == null ? void 0 : completedStep.props) == null ? void 0 : _c.children;
  const content = active > filteredChildren.length - 1 ? completedContent : stepContent;
  return /* @__PURE__ */ React.createElement(Box, __spreadValues({
    className: cx(classes.root, className),
    ref
  }, others), /* @__PURE__ */ React.createElement("div", {
    className: classes.steps
  }, items), content && /* @__PURE__ */ React.createElement("div", {
    className: classes.content
  }, content));
});
Stepper.Step = Step;
Stepper.Completed = StepCompleted;
Stepper.displayName = "@mantine/core/Stepper";

export { Stepper };
//# sourceMappingURL=Stepper.js.map
